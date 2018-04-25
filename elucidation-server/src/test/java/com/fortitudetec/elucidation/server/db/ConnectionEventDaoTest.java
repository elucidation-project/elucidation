package com.fortitudetec.elucidation.server.db;

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
        setupConnectionEvent("test-service-1");
        setupConnectionEvent("test-service-3");
        setupConnectionEvent("test-service-2");

        List<ConnectionEvent> eventsByServiceName = dao.findEventsByServiceName("test-service-1");

        assertThat(eventsByServiceName).hasSize(1);
        assertThat(eventsByServiceName.get(0).getServiceName()).isEqualTo("test-service-1");
    }

    private void setupConnectionEvent(String serviceName) {
        jdbi.withHandle(handle -> handle
            .execute("insert into connection_events " +
                "(service_name, event_direction, communication_type, connection_identifier, rest_method, observed_at) " +
                "values (?, ?, ?, ?, ?, ?)",
                serviceName, "INBOUND", "REST", "/test/path", "GET", Timestamp.from(Instant.now())));
    }

}
