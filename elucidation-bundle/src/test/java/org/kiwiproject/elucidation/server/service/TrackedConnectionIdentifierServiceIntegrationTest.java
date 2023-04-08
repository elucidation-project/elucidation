package org.kiwiproject.elucidation.server.service;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.kiwiproject.elucidation.server.db.ConnectionEventDao;
import org.kiwiproject.elucidation.server.db.DBLoader;
import org.kiwiproject.elucidation.server.db.H2JDBIExtension;
import org.kiwiproject.elucidation.server.db.TrackedConnectionIdentifierDao;
import org.kiwiproject.elucidation.server.db.mapper.ConnectionEventMapper;
import org.kiwiproject.elucidation.server.db.mapper.TrackedConnectionIdentifierMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("TrackedConnectionIdentifierServiceIntegration")
class TrackedConnectionIdentifierServiceIntegrationTest {

    private static final String TEST_ONLY_SERVICE = "foo-service";

    private TrackedConnectionIdentifierService service;

    @BeforeEach
    void setUp(Jdbi jdbi) throws IOException {
        var trackedConnectionIdentifierDao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);
        var connectionEventDao = jdbi.onDemand(ConnectionEventDao.class);

        DBLoader.loadDb(jdbi);

        service = new TrackedConnectionIdentifierService(trackedConnectionIdentifierDao, connectionEventDao);
    }

    @Nested
    class LoadNewIdentifiers {
        @Test
        void shouldCreateNewTrackedConnectionIdentifiers(Jdbi jdbi) {
            var identifiers = List.of("identifier-1", "identifier-2");
            var identifiersAdded = service.loadNewIdentifiers(TEST_ONLY_SERVICE, "HTTP", identifiers);

            assertThat(identifiersAdded).isEqualTo(identifiers.size());

            var savedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers where service_name = ? and communication_type = ?")
                            .bind(0, TEST_ONLY_SERVICE)
                            .bind(1, "HTTP")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            assertThat(savedIdentifiers)
                    .hasSize(2)
                    .extracting("connectionIdentifier")
                    .containsAll(identifiers);
        }

        @Test
        void shouldRemoveExistingTrackedConnectionIdentifiersBeforeInsert_WhenSomeExist(Jdbi jdbi) {
            jdbi.withHandle(handle ->
                    handle.execute(
                            "insert into tracked_connection_identifiers (service_name, communication_type, connection_identifier) values (?, ?, ?)",
                            TEST_ONLY_SERVICE, "HTTP", "some-whack-identifier"));

            var identifiers = List.of("identifier-1", "identifier-2");
            var identifiersAdded = service.loadNewIdentifiers(TEST_ONLY_SERVICE, "HTTP", identifiers);

            assertThat(identifiersAdded).isEqualTo(identifiers.size());

            var savedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers where service_name = ? and communication_type = ?")
                            .bind(0, TEST_ONLY_SERVICE)
                            .bind(1, "HTTP")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            assertThat(savedIdentifiers)
                    .hasSize(2)
                    .extracting("connectionIdentifier")
                    .containsOnly(identifiers.toArray());
        }
    }

    @Nested
    class FindUnusedIdentifiers {

        @Test
        void shouldReturnUnusedIdentifiers_BasedOnEventsAndTracked(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var unusedEventServices = expectedUnusedEvents(jdbi);
            var unusedTrackedServices = expectedUnusedTracked(jdbi);
            var totalUnusedServices = Stream.concat(unusedEventServices.stream(), unusedTrackedServices.stream()).collect(toSet());

            var unusedIdentifiers = service.findUnusedIdentifiers();
            assertThat(unusedIdentifiers).hasSize(totalUnusedServices.size());
        }

        private Set<String> expectedUnusedEvents(Jdbi jdbi) {
            var outboundEvents = jdbi.withHandle(handle ->
                    handle.createQuery("select * from connection_events where event_direction = 'OUTBOUND'")
                            .registerRowMapper(new ConnectionEventMapper())
                            .mapTo(ConnectionEvent.class)
                            .list());

            return outboundEvents.stream()
                    .filter(event -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(distinct(service_name)) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, event.getCommunicationType())
                                    .bind(1, event.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)
                    .map(ConnectionEvent::getServiceName)
                    .collect(toSet());
        }

        private Set<String> expectedUnusedTracked(Jdbi jdbi) {
            var trackedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            return trackedIdentifiers.stream()
                    .filter(tracked -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(distinct(service_name)) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, tracked.getCommunicationType())
                                    .bind(1, tracked.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)

                    .map(TrackedConnectionIdentifier::getServiceName)
                    .collect(toSet());
        }
    }

    @Nested
    class FindUnusedIdentifiersForService {

        @Test
        void shouldReturnUnusedIdentifiers_BasedOnEventsAndTracked(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var unusedEventServices = expectedUnusedEvents(jdbi);
            var unusedTrackedServices = expectedUnusedTracked(jdbi);
            var serviceWithBoth = unusedEventServices.stream()
                    .filter(unusedTrackedServices::contains)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Test data needs at least one service with unused identifiers in both cases."));

            var unusedIdentifier = service.findUnusedIdentifiersForService(serviceWithBoth);
            assertThat(unusedIdentifier.getServiceName()).isEqualTo(serviceWithBoth);
            assertThat(unusedIdentifier.getIdentifiers()).hasSizeGreaterThanOrEqualTo(2);
        }

        private Set<String> expectedUnusedEvents(Jdbi jdbi) {
            var outboundEvents = jdbi.withHandle(handle ->
                    handle.createQuery("select * from connection_events where event_direction = 'OUTBOUND'")
                            .registerRowMapper(new ConnectionEventMapper())
                            .mapTo(ConnectionEvent.class)
                            .list());

            return outboundEvents.stream()
                    .filter(event -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(distinct(service_name)) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, event.getCommunicationType())
                                    .bind(1, event.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)
                    .map(ConnectionEvent::getServiceName)
                    .collect(toSet());
        }

        private Set<String> expectedUnusedTracked(Jdbi jdbi) {
            var trackedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            return trackedIdentifiers.stream()
                    .filter(tracked -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(distinct(service_name)) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, tracked.getCommunicationType())
                                    .bind(1, tracked.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)

                    .map(TrackedConnectionIdentifier::getServiceName)
                    .collect(toSet());
        }
    }

    @Nested
    class AllTrackedConnectionIdentifiers {
        @Test
        void shouldReturnAllTrackedIdentifiers(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var foundIdentifiers = service.allTrackedConnectionIdentifiers();

            assertThat(foundIdentifiers).hasSize(countExistingTrackedIdentifiers(jdbi));
        }
    }

    private int countExistingEvents(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from connection_events")
                .mapTo(Integer.class)
                .first());
    }

    private int countExistingTrackedIdentifiers(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from tracked_connection_identifiers")
                .mapTo(Integer.class)
                .first());
    }

    void assertDataIsLoaded(Jdbi jdbi) {
        assertThat(countExistingEvents(jdbi)).isPositive();
    }
}
