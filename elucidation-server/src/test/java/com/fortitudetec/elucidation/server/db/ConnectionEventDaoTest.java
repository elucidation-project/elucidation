package com.fortitudetec.elucidation.server.db;

import static com.fortitudetec.elucidation.server.core.ConnectionType.INBOUND_REST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.ConnectionType;
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
            .connectionType(ConnectionType.OUTBOUND_REST)
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
        setupConnectionEvent("test-service-1", "test-service-2");
        setupConnectionEvent("test-service-3", "test-service-1");
        setupConnectionEvent("test-service-2", "test-service-3");

        List<ConnectionEvent> eventsByServiceName = dao.findEventsByServiceName("test-service-1");

        assertThat(eventsByServiceName).hasSize(2)
            .extracting("serviceName", "originatingServiceName")
            .containsExactly(
                tuple("test-service-1", "test-service-2"),
                tuple("test-service-3", "test-service-1")
            );
    }

    private void setupConnectionEvent(String serviceName, String originatingServiceName) {
        jdbi.withHandle(handle -> handle
            .execute("insert into connection_events " +
                "(service_name, connection_type, connection_identifier, rest_method, observed_at, originating_service_name) " +
                "values (?, ?, ?, ?, ?, ?)",
                serviceName, INBOUND_REST, "/test/path", "GET", Timestamp.from(Instant.now()), originatingServiceName));
    }

}
