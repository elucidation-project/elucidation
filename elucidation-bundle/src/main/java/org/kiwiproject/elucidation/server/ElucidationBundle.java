package org.kiwiproject.elucidation.server;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.jersey.LoggingJdbiExceptionMapper;
import io.dropwizard.jdbi3.jersey.LoggingSQLExceptionMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.server.config.ElucidationConfiguration;
import org.kiwiproject.elucidation.server.db.ConnectionEventDao;
import org.kiwiproject.elucidation.server.db.TrackedConnectionIdentifierDao;
import org.kiwiproject.elucidation.server.jobs.ArchiveEventsJob;
import org.kiwiproject.elucidation.server.jobs.PollForEventsJob;
import org.kiwiproject.elucidation.server.resources.RelationshipResource;
import org.kiwiproject.elucidation.server.resources.TrackedConnectionIdentifierResource;
import org.kiwiproject.elucidation.server.service.RelationshipService;
import org.kiwiproject.elucidation.server.service.TrackedConnectionIdentifierService;

import java.util.concurrent.TimeUnit;

/**
 * The elucidation bundle that implements {@link ConfiguredBundle} and {@link DatabaseConfiguration} from Dropwizard,
 * and also {@link ElucidationConfiguration} to define elucidation-specific configuration.
 *
 * @param <T> type of configuration
 */
@Slf4j
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
            LOG.warn("CORS is not enabled. Skipping registration of CrossOriginHandler.");
            return;
        }

        var corsHandler = newCorsHandler();
        corsHandler.setAllowedOriginPatterns(corsAllowedOriginPatterns(configuration));
        corsHandler.setAllowedHeaders(corsAllowedHeaders(configuration));
        corsHandler.setAllowedMethods(corsAllowedMethods(configuration));
        corsHandler.setAllowCredentials(corsAllowCredentials(configuration));
        corsHandler.setExposedHeaders(corsExposedHeaders(configuration));

        environment.getApplicationContext().insertHandler(corsHandler);
    }

    @VisibleForTesting
    CrossOriginHandler newCorsHandler() {
        return new CrossOriginHandler();
    }
}
