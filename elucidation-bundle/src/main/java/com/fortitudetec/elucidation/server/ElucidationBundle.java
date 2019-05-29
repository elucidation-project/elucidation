package com.fortitudetec.elucidation.server;

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


import com.fortitudetec.elucidation.server.config.ElucidationConfiguration;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.jobs.ArchiveEventsJob;
import com.fortitudetec.elucidation.server.resources.RelationshipResource;
import com.fortitudetec.elucidation.server.service.RelationshipService;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ElucidationBundle<T extends Configuration> implements ConfiguredBundle<T>, DatabaseConfiguration<T>, ElucidationConfiguration<T> {

    private final JdbiFactory jdbiFactory;

    protected ElucidationBundle() {
        jdbiFactory = new JdbiFactory();
    }

    protected ElucidationBundle(JdbiFactory jdbiFactory) {
        this.jdbiFactory = jdbiFactory;
    }

    // TODO: When we move to Dropwizard 2.0 after it is released, remove this (It is a default method in DW2)
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // For this we don't have anything to setup here
    }

    @Override
    public void run(T configuration, Environment environment) {
        Jdbi jdbi = setupJdbi(configuration, environment);

        ConnectionEventDao dao = jdbi.onDemand(ConnectionEventDao.class);

        RelationshipService relationshipService = new RelationshipService(dao);

        environment.jersey().register(new RelationshipResource(relationshipService));

        ScheduledExecutorService executorService = environment.lifecycle()
                .scheduledExecutorService("Event-Archive-Job", true).build();

        ArchiveEventsJob job = new ArchiveEventsJob(dao, getTimeToLive(configuration));
        executorService.scheduleWithFixedDelay(job, 1, 60, TimeUnit.MINUTES);
    }

    private Jdbi setupJdbi(T configuration, Environment environment) {
        Jdbi jdbi = jdbiFactory.build(environment, getDataSourceFactory(configuration), "Elucidation-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }


}
