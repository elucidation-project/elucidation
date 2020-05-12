package com.fortitudetec.elucidation.server.db;

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

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.IntStream;

@ExtendWith(H2JDBIExtension.class)
class ConnectionEventDaoTest {

    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_CONNECTION_PATH = "GET /test/path";
    private static final String SERVICE_NAME_PROPERTY = "serviceName";
    private ConnectionEventDao dao;
    private Jdbi jdbi;

    @SuppressWarnings("WeakerAccess")
    public void setUp(Jdbi jdbi) {
        this.jdbi = jdbi;
        dao = jdbi.onDemand(ConnectionEventDao.class);
    }

    @Test
    @DisplayName("should successfully insert a new record into the database")
    void testInsertConnection() {
        ConnectionEvent preSaved = ConnectionEvent.builder()
                .serviceName(TEST_SERVICE_NAME)
                .eventDirection(Direction.OUTBOUND)
                .communicationType("HTTP")
                .connectionIdentifier("GET /doSomething")
                .observedAt(System.currentTimeMillis())
                .build();

        Long newId = dao.insertConnection(preSaved);

        assertThat(newId).isNotNull();

        List<String> serviceNames = jdbi.withHandle(handle ->
                handle.createQuery("select service_name from connection_events where id = ?")
                        .bind(0, newId)
                        .mapTo(String.class)
                        .list());

        assertThat(serviceNames).hasSize(1).containsExactly(TEST_SERVICE_NAME);
    }

    @Test
    @DisplayName("should return all events stored in the database since a given time")
    void testGetAllEventsSince() {
        Long initialTime = System.currentTimeMillis();

        IntStream.rangeClosed(1,3)
                .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND, (initialTime * idx)));

        var allEvents = dao.findEventsSince(initialTime);

        assertThat(allEvents).hasSize(2)
                .extracting(SERVICE_NAME_PROPERTY)
                .containsExactly(TEST_SERVICE_NAME + 3, TEST_SERVICE_NAME + 2);
    }

    @Test
    @DisplayName("should only return events for the given service")
    void testFindByService() {
        IntStream.rangeClosed(1,3)
                .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND));

        List<ConnectionEvent> eventsByServiceName = dao.findEventsByServiceName(TEST_SERVICE_NAME + 1);

        assertThat(eventsByServiceName).hasSize(1);
        assertThat(eventsByServiceName.get(0).getServiceName()).isEqualTo(TEST_SERVICE_NAME + 1);
    }

    @Test
    @DisplayName("should return events that match the given identifier and opposite direction")
    void testFindAssociatedEvents() {
        String associateServiceName = "test-associated-service";
        String otherServiceName = "test-other-service";

        setupConnectionEvent(jdbi, associateServiceName, Direction.OUTBOUND);
        setupConnectionEvent(jdbi, otherServiceName, Direction.INBOUND);

        List<ConnectionEvent> associatedEvents = dao.findAssociatedEvents(Direction.OUTBOUND, TEST_CONNECTION_PATH, "HTTP");

        assertThat(associatedEvents).hasSize(1);
        assertThat(associatedEvents.get(0).getServiceName()).isEqualTo(associateServiceName);
    }

    @Test
    @DisplayName("should return just the list of available service names")
    void testFindAllServiceNames() {
        String associateServiceName = "test-associated-service";
        String otherServiceName = "test-other-service";

        setupConnectionEvent(jdbi, associateServiceName, Direction.OUTBOUND);
        setupConnectionEvent(jdbi, otherServiceName, Direction.INBOUND);

        List<String> serviceNames = dao.findAllServiceNames();

        assertThat(serviceNames).hasSize(2).containsOnly(associateServiceName, otherServiceName);
    }

    @Test
    @DisplayName("should only return events for the given connection identifier")
    void testFindByConnectionIdentifier() {
        IntStream.rangeClosed(1,3)
                .forEach(idx -> setupConnectionEvent(jdbi, TEST_SERVICE_NAME + idx, Direction.INBOUND));

        var eventsByConnectionIdentifier = dao.findEventsByConnectionIdentifier(TEST_CONNECTION_PATH);

        assertThat(eventsByConnectionIdentifier)
                .hasSize(3)
                .extracting(ConnectionEvent::getServiceName)
                .containsExactlyInAnyOrder(TEST_SERVICE_NAME + 1, TEST_SERVICE_NAME + 2, TEST_SERVICE_NAME + 3);
    }

    @Nested
    @ExtendWith(H2JDBIExtension.class)
    class CreateOrUpdate {

        private ConnectionEventDao dao;
        private Jdbi jdbi;

        @SuppressWarnings("WeakerAccess")
        public void setUp(Jdbi jdbi) {
            this.jdbi = jdbi;
            this.dao = jdbi.onDemand(ConnectionEventDao.class);
        }

        @Test
        @DisplayName("when event doesn't exist, it should be created")
        void testCreateNew() {
            ConnectionEvent preSaved = ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .observedAt(System.currentTimeMillis())
                    .build();

            List<ConnectionEvent> servicesPreInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPreInsert).isEmpty();

            dao.createOrUpdate(preSaved);

            List<ConnectionEvent> servicesPostInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPostInsert).hasSize(1);
        }

        @Test
        @DisplayName("when event does exist, it should update the observed at")
        void testUpdateEvent() {
            setupConnectionEvent(jdbi, TEST_SERVICE_NAME, Direction.OUTBOUND);

            List<ConnectionEvent> initialEvents = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(initialEvents).hasSize(1);

            dao.createOrUpdate(ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier(TEST_CONNECTION_PATH)
                    .build());

            List<ConnectionEvent> eventsAfterFirstUpdate = eventsForService(TEST_SERVICE_NAME);

            assertThat(eventsAfterFirstUpdate).hasSize(1);

            ConnectionEvent existingEvent = eventsAfterFirstUpdate.get(0);
            dao.createOrUpdate(existingEvent);
            List<ConnectionEvent> eventsAfterSecondUpdate = eventsForService(TEST_SERVICE_NAME);

            assertThat(eventsAfterSecondUpdate).extracting(ConnectionEvent::getId).containsOnly(existingEvent.getId());

            ConnectionEvent updatedEvent = eventsAfterSecondUpdate.get(0);

            assertThat(updatedEvent.getObservedAt()).isGreaterThan(existingEvent.getObservedAt());
        }

        @SuppressWarnings("SameParameterValue")
        private List<ConnectionEvent> eventsForService(String serviceName) {
            return jdbi.withHandle(handle ->
                    handle.createQuery("select * from connection_events where service_name = :serviceName")
                            .bind(SERVICE_NAME_PROPERTY, serviceName)
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
