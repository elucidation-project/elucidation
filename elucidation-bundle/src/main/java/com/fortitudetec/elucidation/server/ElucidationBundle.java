package com.fortitudetec.elucidation.server;

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


import com.fortitudetec.elucidation.common.definition.CommunicationDefinition;
import com.fortitudetec.elucidation.server.config.ElucidationConfiguration;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.db.TrackedConnectionIdentifierDao;
import com.fortitudetec.elucidation.server.jobs.ArchiveEventsJob;
import com.fortitudetec.elucidation.server.jobs.PollForEventsJob;
import com.fortitudetec.elucidation.server.resources.RelationshipResource;
import com.fortitudetec.elucidation.server.resources.TrackedConnectionIdentifierResource;
import com.fortitudetec.elucidation.server.service.RelationshipService;
import com.fortitudetec.elucidation.server.service.TrackedConnectionIdentifierService;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.jersey.LoggingJdbiExceptionMapper;
import io.dropwizard.jdbi3.jersey.LoggingSQLExceptionMapper;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * The elucidation bundle that implements {@link ConfiguredBundle} and {@link DatabaseConfiguration} from Dropwizard,
 * and also {@link ElucidationConfiguration} to define elucidation-specific configuration.
 *
 * @param <T> type of configuration
 */
public abstract class ElucidationBundle<T extends Configuration>
        implements ConfiguredBundle<T>, DatabaseConfiguration<T>, ElucidationConfiguration<T> {

    private final JdbiFactory jdbiFactory;
    private final Client client;

    protected ElucidationBundle() {
        this(new JdbiFactory(), ClientBuilder.newClient());
    }

    protected ElucidationBundle(JdbiFactory jdbiFactory, Client client) {
        this.jdbiFactory = jdbiFactory;
        this.client = client;
    }

    @Override
    public void run(T configuration, Environment environment) {
        var jdbi = setupJdbi(configuration, environment);

        var connectionEventDao = jdbi.onDemand(ConnectionEventDao.class);
        var trackedConnectionIdentifierDao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

        var communicationDefinitions = getCommunicationDefinitions(configuration);
        var relationshipService = new RelationshipService(connectionEventDao, CommunicationDefinition.toMap(communicationDefinitions));

        var trackedConnectionIdentifierService = new TrackedConnectionIdentifierService(trackedConnectionIdentifierDao, connectionEventDao);

        environment.jersey().register(new RelationshipResource(relationshipService));
        environment.jersey().register(new TrackedConnectionIdentifierResource(trackedConnectionIdentifierService));

        setupArchiveJob(configuration, environment, connectionEventDao);
        setupPollingIfNecessary(configuration, environment, relationshipService);
        setupCorsIfNecessary(configuration, environment);
    }

    private void setupPollingIfNecessary(T configuration, Environment environment, RelationshipService relationshipService) {
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

    private void setupArchiveJob(T configuration, Environment environment, ConnectionEventDao connectionEventDao) {
        var archiveExecutorService = environment.lifecycle()
                .scheduledExecutorService("Event-Archive-Job", true).build();

        var archiveJob = new ArchiveEventsJob(connectionEventDao, getTimeToLive(configuration));
        archiveExecutorService.scheduleWithFixedDelay(archiveJob, 1, 60, TimeUnit.MINUTES);
    }

    /**
     * @implNote Because we want to allow opting out of the automatic JDBI exception mapper registration, we
     * cannot add the {@link io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle} in the {@code initialize} method
     * since we do not have access to the {@link Configuration} object there. As a result, we are instead manually
     * registering the {@link LoggingSQLExceptionMapper} and {@link LoggingJdbiExceptionMapper} here when
     * {@link ElucidationConfiguration#isRegisterJdbiExceptionMappers(Configuration)} returns {@code true}. If the
     * {@link io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle} ever changes, then we will need to update the logic
     * here. This is non-ideal, but I don't see a better alternative right now.
     */
    private Jdbi setupJdbi(T configuration, Environment environment) {
        var jdbi = jdbiFactory.build(environment, getDataSourceFactory(configuration), "Elucidation-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());

        if (isRegisterJdbiExceptionMappers(configuration)) {
            environment.jersey().register(new LoggingSQLExceptionMapper());
            environment.jersey().register(new LoggingJdbiExceptionMapper());
        }

        return jdbi;
    }

    private void setupCorsIfNecessary(T configuration, Environment environment) {
        if (!isCorsEnabled(configuration)) {
            return;
        }

        var cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, corsPath(configuration));
    }
}
