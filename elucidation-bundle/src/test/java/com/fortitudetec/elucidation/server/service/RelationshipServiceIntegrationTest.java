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

import static com.fortitudetec.elucidation.common.model.Direction.OUTBOUND;
import static com.fortitudetec.elucidation.common.test.ConnectionEvents.newConnectionEvent;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.kiwiproject.collect.KiwiLists.first;

import com.fortitudetec.elucidation.common.definition.CommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.server.config.ElucidationConfiguration;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.db.DBLoader;
import com.fortitudetec.elucidation.server.db.H2JDBIExtension;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Instant;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("RelationshipServiceIntegration")
class RelationshipServiceIntegrationTest {

    private static final String NON_EXISTENT_SERVICE_NAME = "foo-service";

    private RelationshipService service;

    @BeforeEach
    void setUp(Jdbi jdbi) throws IOException {
        var dao = jdbi.onDemand(ConnectionEventDao.class);

        DBLoader.loadDb(jdbi);

        var communicationDefinitions =
                CommunicationDefinition.toMap(ElucidationConfiguration.defaultCommunicationDefinitions());

        service = new RelationshipService(dao, communicationDefinitions);
    }

    @Nested
    class CreateEvent {

        @Test
        void shouldCreateAnEvent_WhenEventIsNew(Jdbi jdbi) {
            var countBeforeCreate = countExistingEvents(jdbi);

            var event = newConnectionEvent(null, NON_EXISTENT_SERVICE_NAME, OUTBOUND, "some-identifier");
            service.createEvent(event);

            assertThat(countExistingEvents(jdbi)).isEqualTo(countBeforeCreate + 1);
        }

        @Test
        void shouldUpdateAnEvent_WhenEventAlreadyExists(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var existingEvent = jdbi.withHandle(handle -> handle.createQuery("select * from connection_events order by id limit 1")
                    .registerRowMapper(new ConnectionEventMapper())
                    .mapTo(ConnectionEvent.class)
                    .first());

            var existingId = existingEvent.getId();
            var existingObservedAt = existingEvent.getObservedAt();

            service.createEvent(ConnectionEvent.builder()
                    .serviceName(existingEvent.getServiceName())
                    .eventDirection(existingEvent.getEventDirection())
                    .connectionIdentifier(existingEvent.getConnectionIdentifier())
                    .communicationType(existingEvent.getCommunicationType())
                    .build());

            var updatedObservedAt = jdbi.withHandle(handle -> handle.createQuery("select observed_at from connection_events where id = :id")
                    .bind("id", existingId)
                    .mapTo(Long.class)
                    .first());

            assertThat(updatedObservedAt).isGreaterThan(existingObservedAt);
        }
    }

    @Nested
    class ListEventsSince {

        @Test
        void shouldReturnAListOfAllEventsOccurringAfterSinceParam(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var countOfExistingEvents = countExistingEvents(jdbi);
            var earliestObservedAt = jdbi.withHandle(handle -> handle.createQuery("select observed_at from connection_events order by observed_at")
                    .mapTo(Long.class)
                    .first());

            var events = service.listEventsSince(earliestObservedAt - 1);

            assertThat(events).hasSize(countOfExistingEvents);
        }

        @Test
        void shouldReturnAnEmptyListWhenNoEventsFoundAfterSinceParam(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var events = service.listEventsSince(Instant.now().toEpochMilli());
            assertThat(events).isEmpty();
        }
    }

    @Nested
    class ListEventForService {

        @Test
        void shouldReturnEventsForGivenService(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var existingServices = jdbi.withHandle(handle -> handle.createQuery("select distinct(service_name) from connection_events")
                    .mapTo(String.class)
                    .list());

            existingServices.forEach(serviceName -> {
                var eventCount = jdbi.withHandle(handle -> handle.createQuery("select count(*) from connection_events where service_name = ?")
                        .bind(0, serviceName)
                        .mapTo(Integer.class)
                        .first()
                );

                var events = service.listEventsForService(serviceName);
                assertThat(events).hasSize(eventCount);
            });
        }
    }

    @Nested
    class FindAllEventsByConnectionIdentifier {

        @Test
        void shouldReturnEventsForGivenConnectionIdentifier(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var existingConnectionIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select distinct(connection_identifier) from connection_events")
                            .mapTo(String.class)
                            .list());

            existingConnectionIdentifiers.forEach(connectionIdentifier -> {
                var eventCount = jdbi.withHandle(handle ->
                        handle.createQuery("select count(*) from connection_events where connection_identifier = ?")
                                .bind(0, connectionIdentifier)
                                .mapTo(Integer.class)
                                .first()
                );

                var events = service.findAllEventsByConnectionIdentifier(connectionIdentifier);
                assertThat(events).hasSize(eventCount);
            });
        }
    }

    @Nested
    class BuildRelationships {
        @Test
        void shouldReturnAServiceConnectionWithoutEvents_WhenServiceDoesNotHaveEvents(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

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
        void shouldReturnAnEmptyList_WhenServiceDoesNotHaveRelationships(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var existingServiceName = jdbi.withHandle(handle -> handle.createQuery("select service_name from connection_events limit 1")
                    .mapTo(String.class)
                    .first());

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
        void shouldReturnListOfServicesInSystem(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var serviceNames = jdbi.withHandle(handle ->
                    handle.createQuery("select distinct(service_name) from connection_events")
                            .mapTo(String.class)
                            .list());

            assertThat(service.currentServiceNames()).hasSameElementsAs(serviceNames);
        }
    }

    @Nested
    class CurrentServiceDetails {

        @Test
        void shouldReturnDetailsForServicesInSystem(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var serviceDetails = jdbi.withHandle(handle ->
                    handle.createQuery("select a.service_name, a.communication_type, a.typeCount, b.inboundCount, c.outboundCount from " +
                            "(select service_name, communication_type, count(communication_type) as typeCount from connection_events group by service_name, communication_type) a left join " +
                            "(select service_name, count(service_name) as inboundCount from connection_events where event_direction = 'INBOUND' group by service_name) b on a.service_name = b.service_name left join " +
                            "(select service_name, count(service_name) as outboundCount from connection_events where event_direction = 'OUTBOUND' group by service_name) c on a.service_name = c.service_name")

                    .mapToMap()
                    .list());

            var details = service.currentServiceDetails();

            details.forEach(detail -> {
                var rows = serviceDetails.stream()
                        .filter(row -> row.get("service_name").equals(detail.getServiceName()))
                        .collect(toList());

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
        void shouldReturnServiceDependenciesForAllServices(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var serviceNames = jdbi.withHandle(handle ->
                    handle.createQuery("select distinct(service_name) from connection_events")
                        .mapTo(String.class)
                        .list());

            var dependencies = service.buildAllDependencies();

            assertThat(dependencies).hasSize(serviceNames.size());
        }

    }

    @Nested
    class BuildAllDependenciesWithDetails {

        @Test
        void shouldReturnServiceDependenciesForAllServicesWithDetails(Jdbi jdbi) {
            assertDataIsLoaded(jdbi);

            var serviceNames = jdbi.withHandle(handle ->
                    handle.createQuery("select distinct(service_name) from connection_events")
                            .mapTo(String.class)
                            .list());

            var dependencies = service.buildAllDependenciesWithDetails();

            assertThat(dependencies).hasSize(serviceNames.size());
        }
    }

    private int countExistingEvents(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from connection_events")
                .mapTo(Integer.class)
                .first());
    }

    void assertDataIsLoaded(Jdbi jdbi) {
        assertThat(countExistingEvents(jdbi)).isPositive();
    }
}
