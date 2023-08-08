package org.kiwiproject.elucidation.server.service;

import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.server.config.ElucidationConfiguration;
import org.kiwiproject.elucidation.server.db.ConnectionEventDao;
import org.kiwiproject.elucidation.server.db.DBLoader;
import org.kiwiproject.elucidation.server.db.mapper.ConnectionEventMapper;
import org.kiwiproject.test.junit.jupiter.Jdbi3DaoExtension;
import org.kiwiproject.test.junit.jupiter.PostgresLiquibaseTestExtension;

import java.io.IOException;
import java.time.Instant;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.elucidation.common.model.Direction.OUTBOUND;
import static org.kiwiproject.elucidation.common.test.ConnectionEvents.newConnectionEvent;

@DisplayName("RelationshipServiceIntegration")
class RelationshipServiceIntegrationTest {

    @RegisterExtension
    static final PostgresLiquibaseTestExtension POSTGRES = new PostgresLiquibaseTestExtension("elucidation-migrations.xml");

    @RegisterExtension
    final Jdbi3DaoExtension<ConnectionEventDao> daoExtension = Jdbi3DaoExtension.<ConnectionEventDao>builder()
            .daoType(ConnectionEventDao.class)
            .dataSource(POSTGRES.getTestDataSource())
            .build();

    private static final String NON_EXISTENT_SERVICE_NAME = "foo-service";

    private RelationshipService service;

    private Handle handle;

    @BeforeEach
    void setUp() throws IOException {
        var dao = daoExtension.getDao();
        handle = daoExtension.getHandle();
        DBLoader.loadDb(daoExtension.getJdbi());

        var communicationDefinitions =
                CommunicationDefinition.toMap(ElucidationConfiguration.defaultCommunicationDefinitions());

        service = new RelationshipService(dao, communicationDefinitions);
    }

    @Nested
    class CreateEvent {

        @Test
        void shouldCreateAnEvent_WhenEventIsNew() {
            var countBeforeCreate = countExistingEvents();

            var event = newConnectionEvent(null, NON_EXISTENT_SERVICE_NAME, OUTBOUND, "some-identifier");
            service.createEvent(event);

            assertThat(countExistingEvents()).isEqualTo(countBeforeCreate + 1);
        }

        @Test
        void shouldUpdateAnEvent_WhenEventAlreadyExists() {
            assertDataIsLoaded();

            var existingEvent = handle.createQuery("select * from connection_events order by id limit 1")
                    .registerRowMapper(new ConnectionEventMapper())
                    .mapTo(ConnectionEvent.class)
                    .first();

            var existingId = existingEvent.getId();
            var existingObservedAt = existingEvent.getObservedAt();

            service.createEvent(ConnectionEvent.builder()
                    .serviceName(existingEvent.getServiceName())
                    .eventDirection(existingEvent.getEventDirection())
                    .connectionIdentifier(existingEvent.getConnectionIdentifier())
                    .communicationType(existingEvent.getCommunicationType())
                    .build());

            var updatedObservedAt = handle.createQuery("select observed_at from connection_events where id = :id")
                    .bind("id", existingId)
                    .mapTo(Long.class)
                    .first();

            assertThat(updatedObservedAt).isGreaterThan(existingObservedAt);
        }
    }

    @Nested
    class ListEventsSince {

        @Test
        void shouldReturnAListOfAllEventsOccurringAfterSinceParam() {
            assertDataIsLoaded();

            var countOfExistingEvents = countExistingEvents();
            var earliestObservedAt = handle.createQuery("select observed_at from connection_events order by observed_at")
                    .mapTo(Long.class)
                    .first();

            var events = service.listEventsSince(earliestObservedAt - 1);

            assertThat(events).hasSize(countOfExistingEvents);
        }

        @Test
        void shouldReturnAnEmptyListWhenNoEventsFoundAfterSinceParam() {
            assertDataIsLoaded();

            var events = service.listEventsSince(Instant.now().toEpochMilli());
            assertThat(events).isEmpty();
        }
    }

    @Nested
    class ListEventForService {

        @Test
        void shouldReturnEventsForGivenService() {
            assertDataIsLoaded();

            var existingServices = handle.createQuery("select distinct(service_name) from connection_events")
                    .mapTo(String.class)
                    .list();

            existingServices.forEach(serviceName -> {
                var eventCount = handle.createQuery("select count(*) from connection_events where service_name = ?")
                        .bind(0, serviceName)
                        .mapTo(Integer.class)
                        .first();

                var events = service.listEventsForService(serviceName);
                assertThat(events).hasSize(eventCount);
            });
        }
    }

    @Nested
    class FindAllEventsByConnectionIdentifier {

        @Test
        void shouldReturnEventsForGivenConnectionIdentifier() {
            assertDataIsLoaded();

            var existingConnectionIdentifiers = handle.createQuery("select distinct(connection_identifier) from connection_events")
                            .mapTo(String.class)
                            .list();

            existingConnectionIdentifiers.forEach(connectionIdentifier -> {
                var eventCount = handle.createQuery("select count(*) from connection_events where connection_identifier = ?")
                                .bind(0, connectionIdentifier)
                                .mapTo(Integer.class)
                                .first();

                var events = service.findAllEventsByConnectionIdentifier(connectionIdentifier);
                assertThat(events).hasSize(eventCount);
            });
        }
    }

    @Nested
    class BuildRelationships {
        @Test
        void shouldReturnAServiceConnectionWithoutEvents_WhenServiceDoesNotHaveEvents() {
            assertDataIsLoaded();

            var serviceConnections = service.buildRelationships(NON_EXISTENT_SERVICE_NAME);

            assertThat(serviceConnections.getServiceName()).isEqualTo(NON_EXISTENT_SERVICE_NAME);
            assertThat(serviceConnections.getChildren()).isEmpty();
        }

        /**
         * This test is very specific to the generated test data in /src/test/resources.  I would like this test to be more
         * generic but I'm afraid I will have to rebuild all the queries and logic that already exists in the method
         * just to verify the data returned.  I will try to think on this more and update later.
         */
        @Test
        void shouldReturnAServiceConnectionWithEvents_WhenServiceDoesHaveEvents() {

            var serviceConnections = service.buildRelationships("home-service");
            assertThat(serviceConnections.getServiceName()).isEqualTo("home-service");
            assertThat(serviceConnections.getChildren()).hasSize(6)
                    .extracting("serviceName", "hasInbound", "hasOutbound")
                    .contains(
                            tuple("unknown-service", false, true),
                            tuple("appliance-service", false, true),
                            tuple("light-service", false, true),
                            tuple("doorbell-service", true, false),
                            tuple("thermostat-service", false, true),
                            tuple("canary-service", true, false)
                    );
        }
    }

    @Nested
    class FindRelationshipDetails {

        @Test
        void shouldReturnAnEmptyList_WhenServiceDoesNotHaveRelationships() {
            assertDataIsLoaded();

            var existingServiceName = handle.createQuery("select service_name from connection_events limit 1")
                    .mapTo(String.class)
                    .first();

            var relationshipDetails = service.findRelationshipDetails(NON_EXISTENT_SERVICE_NAME, existingServiceName);
            assertThat(relationshipDetails).isEmpty();
        }

        /**
         * This test is very specific to the generated test data in /src/test/resources.  I would like this test to be more
         * generic but I'm afraid I will have to rebuild all the queries and logic that already exists in the method
         * just to verify the data returned.  I will try to think on this more and update later.
         */
        @Test
        void shouldReturnListOfRelationshipDetails_WhenRelationshipsExist() {
            var relationshipDetails = service.findRelationshipDetails("home-service", "thermostat-service");

            assertThat(relationshipDetails).hasSize(1)
                    .extracting("communicationType", "connectionIdentifier")
                    .contains(tuple("JMS", "temp"));
        }
    }

    @Nested
    class CurrentServiceNames {

        @Test
        void shouldReturnListOfServicesInSystem() {
            assertDataIsLoaded();

            var serviceNames = handle.createQuery("select distinct(service_name) from connection_events")
                            .mapTo(String.class)
                            .list();

            assertThat(service.currentServiceNames()).hasSameElementsAs(serviceNames);
        }
    }

    @Nested
    class CurrentServiceDetails {

        @Test
        void shouldReturnDetailsForServicesInSystem() {
            assertDataIsLoaded();

            var serviceDetails = handle.createQuery("select a.service_name, a.communication_type, a.typeCount, b.inboundCount, c.outboundCount from " +
                            "(select service_name, communication_type, count(communication_type) as typeCount from connection_events group by service_name, communication_type) a left join " +
                            "(select service_name, count(service_name) as inboundCount from connection_events where event_direction = 'INBOUND' group by service_name) b on a.service_name = b.service_name left join " +
                            "(select service_name, count(service_name) as outboundCount from connection_events where event_direction = 'OUTBOUND' group by service_name) c on a.service_name = c.service_name")

                    .mapToMap()
                    .list();

            var details = service.currentServiceDetails();

            details.forEach(detail -> {
                var rows = serviceDetails.stream()
                        .filter(row -> row.get("service_name").equals(detail.getServiceName()))
                        .toList();

                assertThat(rows).hasSizeGreaterThanOrEqualTo(1);

                var firstRow = first(rows);

                if (nonNull(firstRow.get("inboundcount"))) {
                    assertThat(first(rows)).containsEntry("inboundcount", (long) detail.getInboundEvents());
                }

                if (nonNull(firstRow.get("outboundcount"))) {
                    assertThat(first(rows)).containsEntry("outboundcount", (long) detail.getOutboundEvents());
                }

                rows.forEach(row -> {
                    var count = (Long) row.get("typecount");
                    assertThat(detail.getCommunicationTypes()).containsEntry((String) row.get("communication_type"), count.intValue());
                });

            });

        }
    }

    @Nested
    class BuildAllDependencies {

        @Test
        void shouldReturnServiceDependenciesForAllServices() {
            assertDataIsLoaded();

            var serviceNames = handle.createQuery("select distinct(service_name) from connection_events")
                        .mapTo(String.class)
                        .list();

            var dependencies = service.buildAllDependencies();

            assertThat(dependencies).hasSize(serviceNames.size());
        }

    }

    @Nested
    class BuildAllDependenciesWithDetails {

        @Test
        void shouldReturnServiceDependenciesForAllServicesWithDetails() {
            assertDataIsLoaded();

            var serviceNames = handle.createQuery("select distinct(service_name) from connection_events")
                            .mapTo(String.class)
                            .list();

            var dependencies = service.buildAllDependenciesWithDetails();

            assertThat(dependencies).hasSize(serviceNames.size());
        }
    }

    private int countExistingEvents() {
        return handle.createQuery("select count(*) from connection_events")
                .mapTo(Integer.class)
                .first();
    }

    void assertDataIsLoaded() {
        assertThat(countExistingEvents()).isPositive();
    }
}
