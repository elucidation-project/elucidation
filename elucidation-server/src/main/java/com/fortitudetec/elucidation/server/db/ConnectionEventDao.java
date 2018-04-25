package com.fortitudetec.elucidation.server.db;

import com.fortitudetec.elucidation.server.core.CommunicationType;
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
        "(service_name, event_direction, communication_type, connection_identifier, rest_method, observed_at) " +
        "values (:serviceName, :eventDirection, :communicationType, :connectionIdentifier, :restMethod, :observedAt)")
    @GetGeneratedKeys("id")
    Long insertConnection(@BindBean ConnectionEvent connection);

    @SqlQuery("select * from connection_events where service_name = :serviceName")
    List<ConnectionEvent> findEventsByServiceName(@Bind("serviceName") String serviceName);

    @SqlQuery("select * from connection_events where event_direction = :eventDirection " +
        "and connection_identifier = :connectionIdentifier and communication_type = :communicationType")
    List<ConnectionEvent> findAssociatedEvents(@Bind("eventDirection") String eventDirection,
                                               @Bind("connectionIdentifier") String connectionIdentifier,
                                               @Bind("communicationType") String communicationType);

    @SqlQuery("select * from connection_events where service_name = :serviceName and connection_type = :connectionType")
    List<ConnectionEvent> findInboundEventsByServiceNameAndConnectionType(@Bind("serviceName") String serviceName,
                                                                   @Bind("connectionType") CommunicationType communicationType);

}
