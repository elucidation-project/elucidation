package org.kiwiproject.elucidation.server.db;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.server.db.mapper.ConnectionEventMapper;
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

    @SqlQuery("select * from connection_events where observed_at > :since order by observed_at desc limit 100")
    List<ConnectionEvent> findEventsSince(@Bind("since") Long since);

    @SqlQuery("select * from connection_events where service_name = :serviceName")
    List<ConnectionEvent> findEventsByServiceName(@Bind("serviceName") String serviceName);

    @SqlQuery("select * from connection_events " +
            "where event_direction = :eventDirection and connection_identifier = :connectionIdentifier and communication_type = :communicationType")
    List<ConnectionEvent> findAssociatedEvents(@Bind("eventDirection") Direction eventDirection,
                                               @Bind("connectionIdentifier") String connectionIdentifier,
                                               @Bind("communicationType") String communicationType);

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

    @SqlQuery("select * from connection_events where connection_identifier = :connectionIdentifier")
    List<ConnectionEvent> findEventsByConnectionIdentifier(@Bind("connectionIdentifier") String connectionIdentifier);
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
