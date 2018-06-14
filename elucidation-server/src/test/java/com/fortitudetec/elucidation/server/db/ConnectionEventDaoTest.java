package com.fortitudetec.elucidation.server.db;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
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

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@ExtendWith(H2JDBIExtension.class)
class ConnectionEventDaoTest {

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
        ZonedDateTime now = ZonedDateTime.now();

        ConnectionEvent preSaved = ConnectionEvent.builder()
            .serviceName("test-service")
            .eventDirection(Direction.OUTBOUND)
            .communicationType(CommunicationType.REST)
            .connectionIdentifier("/doSomething")
            .restMethod("GET")
            .observedAt(now)
            .build();

        Long newId = dao.insertConnection(preSaved);

        assertThat(newId).isNotNull();

        List<String> serviceNames = jdbi.withHandle(handle ->
            handle.createQuery("select service_name from connection_events where id = ?")
                .bind(0, newId)
                .mapTo(String.class)
                .list());

        assertThat(serviceNames).hasSize(1).containsExactly("test-service");


    }

    @Test
    @DisplayName("should only return events for the given service")
    void testFindByService() {
        setupConnectionEvent("test-service-1", Direction.INBOUND, CommunicationType.REST);
        setupConnectionEvent("test-service-3", Direction.INBOUND, CommunicationType.REST);
        setupConnectionEvent("test-service-2", Direction.INBOUND, CommunicationType.REST);

        List<ConnectionEvent> eventsByServiceName = dao.findEventsByServiceName("test-service-1");

        assertThat(eventsByServiceName).hasSize(1);
        assertThat(eventsByServiceName.get(0).getServiceName()).isEqualTo("test-service-1");
    }

    @Test
    @DisplayName("should return events that match the given identifier and opposite direction")
    void testFindAssociatedEvents() {
        setupConnectionEvent("test-associated-service-1", Direction.OUTBOUND, CommunicationType.REST);
        setupConnectionEvent("test-other-service-1", Direction.INBOUND, CommunicationType.REST);

        List<ConnectionEvent> associatedEvents = dao.findAssociatedEvents(Direction.OUTBOUND, "/test/path", CommunicationType.REST);

        assertThat(associatedEvents).hasSize(1);
        assertThat(associatedEvents.get(0).getServiceName()).isEqualTo("test-associated-service-1");
    }

    @Test
    @DisplayName("should return just the list of available service names")
    void testFindAllServiceNames() {
        setupConnectionEvent("test-associated-service-1", Direction.OUTBOUND, CommunicationType.REST);
        setupConnectionEvent("test-other-service-1", Direction.INBOUND, CommunicationType.REST);

        List<String> serviceNames = dao.findAllServiceNames();

        assertThat(serviceNames).hasSize(2).containsOnly("test-associated-service-1", "test-other-service-1");
    }

    private void setupConnectionEvent(String serviceName, Direction direction, CommunicationType type) {
        jdbi.withHandle(handle -> handle
            .execute("insert into connection_events " +
                "(service_name, event_direction, communication_type, connection_identifier, rest_method, observed_at) " +
                "values (?, ?, ?, ?, ?, ?)",
                serviceName, direction.name(), type.name(), "/test/path", "GET", Timestamp.from(Instant.now())));
    }

}
