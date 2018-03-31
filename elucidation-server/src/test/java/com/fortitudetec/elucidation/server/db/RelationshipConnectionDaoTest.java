package com.fortitudetec.elucidation.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.fortitudetec.elucidation.server.core.ConnectionType;
import com.fortitudetec.elucidation.server.core.RelationshipConnection;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.util.List;

@ExtendWith(H2JDBIExtension.class)
class RelationshipConnectionDaoTest {

    private RelationshipConnectionDao dao;
    private Jdbi jdbi;

    @SuppressWarnings("WeakerAccess")
    public void setUp(Jdbi jdbi) {
        this.jdbi = jdbi;
        dao = jdbi.onDemand(RelationshipConnectionDao.class);
    }

    @Test
    @DisplayName("should successfully insert a new record into the database")
    void testInsertConnection() {
        ZonedDateTime now = ZonedDateTime.now();

        RelationshipConnection preSaved = RelationshipConnection.builder()
            .serviceName("test-service")
            .connectionType(ConnectionType.OUTBOUND_REST)
            .connectionIdentifier("/doSomething")
            .restMethod("GET")
            .observedAt(now)
            .build();

        Long newId = dao.insertConnection(preSaved);

        assertThat(newId).isNotNull();

        List<String> serviceNames = jdbi.withHandle(handle ->
            handle.createQuery("select service_name from relationship_connections where id = ?")
                .bind(0, newId)
                .mapTo(String.class)
                .list());

        assertThat(serviceNames).hasSize(1).containsExactly("test-service");


    }

}
