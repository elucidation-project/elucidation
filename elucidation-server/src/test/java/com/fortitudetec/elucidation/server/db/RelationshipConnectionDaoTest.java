package com.fortitudetec.elucidation.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.fortitudetec.elucidation.server.core.ConnectionType;
import com.fortitudetec.elucidation.server.core.RelationshipConnection;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;

public class RelationshipConnectionDaoTest {

    @ClassRule
    public static final H2JDBIRule RULE = new H2JDBIRule();

    private RelationshipConnectionDao dao;
    private Jdbi jdbi;

    @Before
    public void setUp() {
        jdbi = RULE.getJdbi();
        jdbi.installPlugin(new SqlObjectPlugin());
        dao = jdbi.onDemand(RelationshipConnectionDao.class);
    }

    @Test
    public void testInsertConnection() {
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
