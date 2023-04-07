package org.kiwiproject.elucidation.server.db.mapper;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
