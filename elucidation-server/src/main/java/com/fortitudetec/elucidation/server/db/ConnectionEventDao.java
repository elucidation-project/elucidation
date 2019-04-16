package com.fortitudetec.elucidation.server.db;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
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

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventForAssociatedServiceMapper;
import com.fortitudetec.elucidation.server.db.mapper.ConnectionEventForServiceMapper;
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
            "(service_name, event_direction, communication_type, connection_identifier, observed_at) " +
            "values (:serviceName, :eventDirection, :communicationType, :connectionIdentifier, :observedAt)")
    @GetGeneratedKeys("id")
    Long insertConnection(@BindBean ConnectionEvent connection);

    @SqlQuery("select service_name, event_direction, communication_type, connection_identifier from connection_events " +
            "where service_name = :serviceName " +
            "group by service_name, event_direction, communication_type, connection_identifier")
    @RegisterRowMapper(value = ConnectionEventForServiceMapper.class)
    List<ConnectionEvent> findEventsByServiceName(@Bind("serviceName") String serviceName);

    @SqlQuery("select service_name, communication_type, connection_identifier, event_direction from connection_events " +
            "where event_direction = :eventDirection and connection_identifier = :connectionIdentifier and communication_type = :communicationType " +
            "group by service_name, communication_type, connection_identifier, event_direction")
    @RegisterRowMapper(value = ConnectionEventForAssociatedServiceMapper.class)
    List<ConnectionEvent> findAssociatedEvents(@Bind("eventDirection") Direction eventDirection,
                                               @Bind("connectionIdentifier") String connectionIdentifier,
                                               @Bind("communicationType") CommunicationType communicationType);

    @SqlQuery("select distinct(service_name) from connection_events")
    List<String> findAllServiceNames();

    @SqlUpdate("delete from connection_events where observed_at < :expiresAt")
    int deleteExpiredEvents(@Bind("expiresAt") long expiresAt);

    @SqlQuery("select * from connection_events " +
            "where service_name = :serviceName and event_direction = :eventDirection and " +
            "communication_type = :communicationType and connection_identifier = :connectionIdentifier")
    List<ConnectionEvent> findEventsByExample(@BindBean ConnectionEvent connection);

    @SqlUpdate("update connection_events set observed_at = :newTimestamp " +
            "where service_name = :serviceName and event_direction = :eventDirection and " +
            "communication_type = :communicationType and connection_identifier = :connectionIdentifier")
    void updateObservedAt(@BindBean ConnectionEvent connection, @Bind("newTimestamp") Long newTimestamp);

    /**
     * Utility to create or update a connection event.  Using this method over native insert or update (mysql) or
     * update on conflict (postgres) to keep the system DB agnostic.
     *
     * @param event The connection event to insert or update
     */
    default void createOrUpdate(final ConnectionEvent event) {
        if (findEventsByExample(event).isEmpty()) {
            insertConnection(event);
        } else {
            updateObservedAt(event, System.currentTimeMillis());
        }
    }

}
