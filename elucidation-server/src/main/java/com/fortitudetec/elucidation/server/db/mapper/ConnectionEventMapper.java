package com.fortitudetec.elucidation.server.db.mapper;

import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.ConnectionType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ConnectionEventMapper implements RowMapper<ConnectionEvent> {
    @Override
    public ConnectionEvent map(ResultSet rs, StatementContext ctx) throws SQLException {

        return ConnectionEvent.builder()
            .id(rs.getLong("id"))
            .serviceName(rs.getString("service_name"))
            .connectionType(ConnectionType.valueOf(rs.getString("connection_type")))
            .connectionIdentifier(rs.getString("connection_identifier"))
            .restMethod(rs.getString("rest_method"))
            .observedAt(ZonedDateTime.ofInstant(rs.getTimestamp("observed_at").toInstant(), ZoneOffset.UTC))
            .originatingServiceName(rs.getString("originating_service_name"))
            .build();

    }
}
