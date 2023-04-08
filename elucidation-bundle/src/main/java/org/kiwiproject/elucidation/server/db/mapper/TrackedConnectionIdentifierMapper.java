package org.kiwiproject.elucidation.server.db.mapper;

import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TrackedConnectionIdentifierMapper implements RowMapper<TrackedConnectionIdentifier> {
    @Override
    public TrackedConnectionIdentifier map(ResultSet rs, StatementContext ctx) throws SQLException {

        return TrackedConnectionIdentifier.builder()
                .id(rs.getLong("id"))
                .serviceName(rs.getString("service_name"))
                .communicationType(rs.getString("communication_type"))
                .connectionIdentifier(rs.getString("connection_identifier"))
                .build();

    }
}
