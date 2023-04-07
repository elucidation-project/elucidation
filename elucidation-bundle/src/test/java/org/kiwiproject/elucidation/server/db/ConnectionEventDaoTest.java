package org.kiwiproject.elucidation.server.db;

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

import static org.assertj.core.api.Assertions.assertThat;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.server.db.mapper.ConnectionEventMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("ConnectionEventDao")
class ConnectionEventDaoTest {

    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_CONNECTION_PATH = "GET /test/path";
    private static final String SERVICE_NAME_PROPERTY = "serviceName";

    @Nested
    class InsertConnection {
        @Test
        void shouldSuccessfullyInsertANewConnectionEvent(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            var preSaved = ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .observedAt(System.currentTimeMillis())
                    .build();

            var newId = dao.insertConnection(preSaved);

            assertThat(newId).isNotNull();

            var serviceNames = jdbi.withHandle(handle ->
                    handle.createQuery("select service_name from connection_events where id = ?")
                            .bind(0, newId)
                            .mapTo(String.class)
                            .list());

            assertThat(serviceNames).hasSize(1).containsExactly(TEST_SERVICE_NAME);
        }
    }

    @Nested
    class FindEventsSince {
        @Test
        void shouldReturnAllEventsStoredInTheDatabaseSinceAGivenTime(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            var initialTime = System.currentTimeMillis();

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND, (initialTime * idx)));

            var allEvents = dao.findEventsSince(initialTime);

            assertThat(allEvents).hasSize(2)
                    .extracting(SERVICE_NAME_PROPERTY)
                    .containsExactly(TEST_SERVICE_NAME + 3, TEST_SERVICE_NAME + 2);
        }
    }

    @Nested
    class FindEventsByServiceName {
        @Test
        void shouldOnlyReturnEventsForTheGivenService(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND));

            var eventsByServiceName = dao.findEventsByServiceName(TEST_SERVICE_NAME + 1);

            assertThat(eventsByServiceName).hasSize(1);
            assertThat(eventsByServiceName.get(0).getServiceName()).isEqualTo(TEST_SERVICE_NAME + 1);
        }
    }

    @Nested
    class FindAssociatedEvents {
        @Test
        void shouldReturnEventsThatMatchTheGivenIdentifierAndOppositeDirection(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupConnectionEvent(jdbi, associateServiceName, Direction.OUTBOUND);
            setupConnectionEvent(jdbi, otherServiceName, Direction.INBOUND);

            var associatedEvents = dao.findAssociatedEvents(Direction.OUTBOUND, TEST_CONNECTION_PATH, "HTTP");

            assertThat(associatedEvents).hasSize(1);
            assertThat(associatedEvents.get(0).getServiceName()).isEqualTo(associateServiceName);
        }
    }

    @Nested
    class FindAllServiceNames {
        @Test
        void shouldReturnJustTheListOfExistingServiceNames(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupConnectionEvent(jdbi, associateServiceName, Direction.OUTBOUND);
            setupConnectionEvent(jdbi, otherServiceName, Direction.INBOUND);

            var serviceNames = dao.findAllServiceNames();

            assertThat(serviceNames).hasSize(2).containsOnly(associateServiceName, otherServiceName);
        }
    }

    @Nested
    class FindEventsByConnectionIdentifier {
        @Test
        void shouldOnlyReturnEventsThatMatchTheGivenConnectionIdentifier(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND));

            var eventsByConnectionIdentifier = dao.findEventsByConnectionIdentifier(TEST_CONNECTION_PATH);

            assertThat(eventsByConnectionIdentifier)
                    .hasSize(3)
                    .extracting(ConnectionEvent::getServiceName)
                    .containsExactlyInAnyOrder(TEST_SERVICE_NAME + 1, TEST_SERVICE_NAME + 2, TEST_SERVICE_NAME + 3);
        }
    }

    @Nested
    class CreateOrUpdate {

        @Test
        void shouldCreateANewRecord_WhenOneDoesNotExist(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            var preSaved = ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .observedAt(System.currentTimeMillis())
                    .build();

            var servicesPreInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPreInsert).isEmpty();

            dao.createOrUpdate(preSaved);

            var servicesPostInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPostInsert).hasSize(1);
        }

        @Test
        void shouldUpdateAnExistingRecord_WhenTheRecordExists(Jdbi jdbi) {
            var dao = jdbi.onDemand(ConnectionEventDao.class);

            setupConnectionEvent(jdbi, TEST_SERVICE_NAME, Direction.OUTBOUND);

            var initialEvents = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(initialEvents).hasSize(1);

            dao.createOrUpdate(ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier(TEST_CONNECTION_PATH)
                    .build());

            var eventsAfterFirstUpdate = eventsForService(jdbi);

            assertThat(eventsAfterFirstUpdate).hasSize(1);

            var existingEvent = eventsAfterFirstUpdate.get(0);
            dao.createOrUpdate(existingEvent);
            var eventsAfterSecondUpdate = eventsForService(jdbi);

            assertThat(eventsAfterSecondUpdate).extracting(ConnectionEvent::getId).containsOnly(existingEvent.getId());

            var updatedEvent = eventsAfterSecondUpdate.get(0);

            assertThat(updatedEvent.getObservedAt()).isGreaterThan(existingEvent.getObservedAt());
        }

        private List<ConnectionEvent> eventsForService(Jdbi jdbi) {
            return jdbi.withHandle(handle ->
                    handle.createQuery("select * from connection_events where service_name = :serviceName")
                            .bind(SERVICE_NAME_PROPERTY, ConnectionEventDaoTest.TEST_SERVICE_NAME)
                            .map(new ConnectionEventMapper())
                            .list());
        }

    }

    private static void setupConnectionEvent(Jdbi jdbi, String serviceName, Direction direction) {
        setupConnectionEvent(jdbi, serviceName, direction, System.currentTimeMillis());
    }

    private static void setupConnectionEvent(Jdbi jdbi, String serviceName, Direction direction, Long observedAt) {
        jdbi.withHandle(handle -> handle
                .execute("insert into connection_events " +
                                "(service_name, event_direction, communication_type, connection_identifier, observed_at) " +
                                "values (?, ?, ?, ?, ?)",
                        serviceName, direction.name(), "HTTP", TEST_CONNECTION_PATH, observedAt));
    }

}
