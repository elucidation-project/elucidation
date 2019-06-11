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


import com.fortitudetec.elucidation.common.definition.CommunicationDefinition;
import com.fortitudetec.elucidation.server.config.ElucidationConfiguration;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.jobs.ArchiveEventsJob;
import com.fortitudetec.elucidation.server.jobs.PollForEventsJob;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

/**
 * The elucidation bundle that implements {@link ConfiguredBundle} and {@link DatabaseConfiguration} from Dropwizard,
 * and also {@link ElucidationConfiguration} to define elucidation-specific configuration.
 *
 * @param <T> type of configuration
 */
public abstract class ElucidationBundle<T extends Configuration> implements ConfiguredBundle<T>, DatabaseConfiguration<T>, ElucidationConfiguration<T> {

    private final JdbiFactory jdbiFactory;
    private final Client client;

    protected ElucidationBundle() {
        this(new JdbiFactory(), ClientBuilder.newClient());
    }

    protected ElucidationBundle(JdbiFactory jdbiFactory, Client client) {
        this.jdbiFactory = jdbiFactory;
        this.client = client;
    }

    // TODO: When we move to Dropwizard 2.0 after it is released, remove this (It is a default method in DW2)
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // For this we don't have anything to setup here
    }

    @Override
    public void run(T configuration, Environment environment) {
        var jdbi = setupJdbi(configuration, environment);

        var dao = jdbi.onDemand(ConnectionEventDao.class);

        var communicationDefinitions = getCommunicationDefinitions(configuration);
        var relationshipService = new RelationshipService(dao, CommunicationDefinition.toMap(communicationDefinitions));

        environment.jersey().register(new RelationshipResource(relationshipService));

        var archiveExecutorService = environment.lifecycle()
                .scheduledExecutorService("Event-Archive-Job", true).build();

        var archiveJob = new ArchiveEventsJob(dao, getTimeToLive(configuration));
        archiveExecutorService.scheduleWithFixedDelay(archiveJob, 1, 60, TimeUnit.MINUTES);

        if (shouldPoll(configuration)) {
            var pollingExecutorService = environment.lifecycle()
                    .scheduledExecutorService("Event-Polling-Job", true).build();

            var pollingJob = new PollForEventsJob(
                    getPollEndpointSupplier(configuration),
                    client,
                    relationshipService
            );

            var pollingConfig = getPollingConfig(configuration);
            pollingExecutorService.scheduleWithFixedDelay(
                    pollingJob,
                    pollingConfig.orElseThrow().getPollingDelay().toMinutes(),
                    pollingConfig.orElseThrow().getPollingInterval().toMinutes(),
                    TimeUnit.MINUTES);
        }
    }

    private Jdbi setupJdbi(T configuration, Environment environment) {
        var jdbi = jdbiFactory.build(environment, getDataSourceFactory(configuration), "Elucidation-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }


}
