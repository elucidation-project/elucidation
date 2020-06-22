package com.fortitudetec.elucidation.server.service;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.db.DBLoader;
import com.fortitudetec.elucidation.server.db.H2JDBIExtension;
import com.fortitudetec.elucidation.server.db.TrackedConnectionIdentifierDao;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventMapper;
import com.fortitudetec.elucidation.server.db.mapper.TrackedConnectionIdentifierMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("TrackedConnectionIdentifierServiceIntegeration")
class TrackedConnectionIdentifierServiceIntegrationTest {

    private static final String TEST_ONLY_SERVICE = "foo-service";

    private TrackedConnectionIdentifierService service;

    @BeforeEach
    void setUp(Jdbi jdbi) throws IOException {
        var trackedConnectionIdentifierDao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);
        var connectionEventDao = jdbi.onDemand(ConnectionEventDao.class);

        DBLoader.loadDb(jdbi, "elucidation-events.csv");
        // TODO: Build and load tracked test data

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

            var unusedEvents = expectedUnusedEvents(jdbi);
            var unusedTracked = expectedUnusedTracked(jdbi);

            var unusedIdentifiers = service.findUnusedIdentifiers();
            assertThat(unusedIdentifiers).hasSize(unusedEvents.size() + unusedTracked.size());
        }

        private List<ConnectionEvent> expectedUnusedEvents(Jdbi jdbi) {
            var outboundEvents = jdbi.withHandle(handle ->
                    handle.createQuery("select * from connection_events where event_direction = 'OUTBOUND'")
                            .registerRowMapper(new ConnectionEventMapper())
                            .mapTo(ConnectionEvent.class)
                            .list());

            return outboundEvents.stream()
                    .filter(event -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(*) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, event.getCommunicationType())
                                    .bind(1, event.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)
                    .collect(toList());
        }

        private List<TrackedConnectionIdentifier> expectedUnusedTracked(Jdbi jdbi) {
            // TODO: Once we have generated data for tracked identifiers, the creation part here can go away
            var trackedIdentifiers = List.of(TrackedConnectionIdentifier.builder()
                    .serviceName(TEST_ONLY_SERVICE)
                    .communicationType("HTTP")
                    .connectionIdentifier("/path/unused")
                    .build());

            trackedIdentifiers.forEach(tracked ->
                    jdbi.withHandle(handle ->
                            handle.execute("insert into tracked_connection_identifiers " +
                                            "(service_name, communication_type, connection_identifier) " +
                                            "values (?, ?, ?)",
                                    tracked.getServiceName(), tracked.getCommunicationType(), tracked.getConnectionIdentifier())));

            return trackedIdentifiers.stream()
                    .filter(tracked -> jdbi.withHandle(handle ->
                            handle.createQuery("select count(*) from connection_events " +
                                    "where communication_type = ? and connection_identifier = ? and event_direction = 'INBOUND'")
                                    .bind(0, tracked.getCommunicationType())
                                    .bind(1, tracked.getConnectionIdentifier())
                                    .mapTo(Integer.class)
                                    .first()) == 0)
                    .collect(toList());
        }
    }

    @Nested
    class AllTrackedConnectionIdentifiers {
        @Test
        void shouldReturnAllTrackedIdentifiers(Jdbi jdbi) {
            // TODO: Once we have generated data for tracked identifiers, the creation part here can go away
            var trackedIdentifiers = List.of(TrackedConnectionIdentifier.builder()
                    .serviceName(TEST_ONLY_SERVICE)
                    .communicationType("HTTP")
                    .connectionIdentifier("/path/unused")
                    .build());

            trackedIdentifiers.forEach(tracked ->
                    jdbi.withHandle(handle ->
                            handle.execute("insert into tracked_connection_identifiers " +
                                            "(service_name, communication_type, connection_identifier) " +
                                            "values (?, ?, ?)",
                                    tracked.getServiceName(), tracked.getCommunicationType(), tracked.getConnectionIdentifier())));

            var foundIdentifiers = service.allTrackedConnectionIdentifiers();

            var identifier = trackedIdentifiers.get(0);
            assertThat(foundIdentifiers)
                    .extracting("serviceName", "communicationType", "connectionIdentifier")
                    .containsExactly(tuple(identifier.getServiceName(), identifier.getCommunicationType(), identifier.getConnectionIdentifier()));
        }
    }

    private int countExistingEvents(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from connection_events")
                .mapTo(Integer.class)
                .first());
    }

    void assertDataIsLoaded(Jdbi jdbi) {
        assertThat(countExistingEvents(jdbi)).isGreaterThan(0);
    }
}
