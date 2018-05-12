package com.fortitudetec.elucidation.server.db.mapper;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
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
            .eventDirection(Direction.valueOf(rs.getString("event_direction")))
            .communicationType(CommunicationType.valueOf(rs.getString("communication_type")))
            .connectionIdentifier(rs.getString("connection_identifier"))
            .restMethod(rs.getString("rest_method"))
            .observedAt(ZonedDateTime.ofInstant(rs.getTimestamp("observed_at").toInstant(), ZoneOffset.UTC))
            .build();

    }
}
