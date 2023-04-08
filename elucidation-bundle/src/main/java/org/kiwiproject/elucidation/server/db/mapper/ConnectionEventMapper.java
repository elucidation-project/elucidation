package org.kiwiproject.elucidation.server.db.mapper;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionEventMapper implements RowMapper<ConnectionEvent> {
    @Override
    public ConnectionEvent map(ResultSet rs, StatementContext ctx) throws SQLException {

        return ConnectionEvent.builder()
                .id(rs.getLong("id"))
                .serviceName(rs.getString("service_name"))
                .eventDirection(Direction.valueOf(rs.getString("event_direction")))
                .communicationType(rs.getString("communication_type"))
                .connectionIdentifier(rs.getString("connection_identifier"))
                .observedAt(rs.getLong("observed_at"))
                .build();

    }
}
