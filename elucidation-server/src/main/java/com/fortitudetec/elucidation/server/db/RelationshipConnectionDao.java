package com.fortitudetec.elucidation.server.db;

import com.fortitudetec.elucidation.server.core.RelationshipConnection;
import com.fortitudetec.elucidation.server.db.mapper.RelationshipConnectionMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(value = RelationshipConnectionMapper.class)
public interface RelationshipConnectionDao {

    @SqlUpdate("insert into relationship_connections " +
        "(service_name, connection_type, connection_identifier, rest_method, observed_at, originating_service_name) " +
        "values (:serviceName, :connectionType, :connectionIdentifier, :restMethod, :observedAt, :originatingServiceName)")
    @GetGeneratedKeys("id")
    Long insertConnection(@BindBean RelationshipConnection connection);

}
