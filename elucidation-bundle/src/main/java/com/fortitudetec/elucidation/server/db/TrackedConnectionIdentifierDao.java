package com.fortitudetec.elucidation.server.db;

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

import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.fortitudetec.elucidation.server.db.mapper.TrackedConnectionIdentifierMapper;
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
