package org.kiwiproject.elucidation.server.db;

import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.kiwiproject.elucidation.server.db.mapper.TrackedConnectionIdentifierMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterRowMapper(value = TrackedConnectionIdentifierMapper.class)
public interface TrackedConnectionIdentifierDao {

    @SqlUpdate("insert into tracked_connection_identifiers " +
            "(service_name, communication_type, connection_identifier) " +
            "values (:serviceName, :communicationType, :connectionIdentifier)")
    @GetGeneratedKeys("id")
    long insertIdentifier(@BindBean TrackedConnectionIdentifier identifier);

    @SqlUpdate("delete from tracked_connection_identifiers where service_name = :serviceName and communication_type = :communicationType")
    int clearIdentifiersFor(@Bind("serviceName") String serviceName, @Bind("communicationType") String communicationType);

    @SqlQuery("select * from tracked_connection_identifiers")
    List<TrackedConnectionIdentifier> findIdentifiers();

    @SqlQuery("select distinct(service_name) from tracked_connection_identifiers")
    List<String> findAllServiceNames();

    @SqlQuery("select * from tracked_connection_identifiers where service_name = :serviceName")
    List<TrackedConnectionIdentifier> findByServiceName(@Bind("serviceName") String serviceName);
}
