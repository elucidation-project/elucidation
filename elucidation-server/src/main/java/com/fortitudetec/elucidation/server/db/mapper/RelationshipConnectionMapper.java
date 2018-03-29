package com.fortitudetec.elucidation.server.db.mapper;

import com.fortitudetec.elucidation.server.core.ConnectionType;
import com.fortitudetec.elucidation.server.core.RelationshipConnection;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZonedDateTime;

public class RelationshipConnectionMapper implements RowMapper<RelationshipConnection> {
    @Override
    public RelationshipConnection map(ResultSet rs, StatementContext ctx) throws SQLException {

        return RelationshipConnection.builder()
            .id(rs.getLong("id"))
            .serviceName(rs.getString("service_name"))
            .connectionType(ConnectionType.valueOf(rs.getString("connection_type")))
            .connectionIdentifier(rs.getString("connection_identifier"))
            .restMethod(rs.getString("rest_method"))
            .observedAt(ZonedDateTime.from(Instant.ofEpochMilli(rs.getLong("observed_at"))))
            .originatingServiceName(rs.getString("originating_service_name"))
            .build();

    }
}
