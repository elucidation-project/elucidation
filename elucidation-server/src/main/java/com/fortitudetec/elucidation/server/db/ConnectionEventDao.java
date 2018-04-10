package com.fortitudetec.elucidation.server.db;

import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterRowMapper(value = ConnectionEventMapper.class)
public interface ConnectionEventDao {

    @SqlUpdate("insert into connection_events " +
        "(service_name, connection_type, connection_identifier, rest_method, observed_at, originating_service_name) " +
        "values (:serviceName, :connectionType, :connectionIdentifier, :restMethod, :observedAt, :originatingServiceName)")
    @GetGeneratedKeys("id")
    Long insertConnection(@BindBean ConnectionEvent connection);

    @SqlQuery("select * from connection_events where service_name = :serviceName or originating_service_name = :serviceName")
    List<ConnectionEvent> findEventsByServiceName(@Bind("serviceName") String serviceName);

}
