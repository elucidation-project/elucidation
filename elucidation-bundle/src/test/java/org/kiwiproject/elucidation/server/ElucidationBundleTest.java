package org.kiwiproject.elucidation.server;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.server.config.PollingConfig;
import org.kiwiproject.elucidation.server.jobs.ArchiveEventsJob;
import org.kiwiproject.elucidation.server.jobs.PollForEventsJob;
import org.kiwiproject.elucidation.server.resources.RelationshipResource;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.jersey.LoggingJdbiExceptionMapper;
import io.dropwizard.jdbi3.jersey.LoggingSQLExceptionMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.core.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterRegistration;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ElucidationBundleTest {

    private Configuration configuration;
    private DataSourceFactory dataSourceFactory;
    private Environment environment;
    private JdbiFactory jdbiFactory;
    private JerseyEnvironment jerseyEnvironment;
    private ScheduledExecutorService executor;
    private ElucidationBundle<Configuration> bundle;
    private Client client;
    private LifecycleEnvironment lifecycle;
    private ScheduledExecutorServiceBuilder builder;
    private FilterRegistration.Dynamic cors;

    @BeforeEach
    void setUp() {
        dataSourceFactory = new DataSourceFactory();

        // Mocks
        var jdbi = mock(Jdbi.class);
        lifecycle = mock(LifecycleEnvironment.class);
        builder = mock(ScheduledExecutorServiceBuilder.class);

        configuration = mock(Configuration.class);
        environment = mock(Environment.class);
        jdbiFactory = mock(JdbiFactory.class);
        jerseyEnvironment = mock(JerseyEnvironment.class);
        executor = mock(ScheduledExecutorService.class);

        var servlets = mock(ServletEnvironment.class);
        cors = mock(FilterRegistration.Dynamic.class);

        // Expectations
        when(jdbiFactory.build(environment,
                dataSourceFactory,
                "Elucidation-Data-Source")).thenReturn(jdbi);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycle);
        when(environment.servlets()).thenReturn(servlets);
        when(servlets.addFilter("CORS", CrossOriginFilter.class)).thenReturn(cors);
        when(lifecycle.scheduledExecutorService("Event-Archive-Job", true)).thenReturn(builder);
        when(builder.build()).thenReturn(executor);

        client = ClientBuilder.newClient();

        // Create bundle
        bundle = new ElucidationBundle<>(jdbiFactory, client) {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                return dataSourceFactory;
            }
        };
    }

    @Nested
    class Run {

        @Test
        void shouldSetupJdbi() {
            bundle.run(configuration, environment);

            verify(jdbiFactory).build(environment,
                    dataSourceFactory,
                    "Elucidation-Data-Source");
        }

        @Nested
        class JdbiExceptionMappers {

            @Test
            void shouldBeAddedByDefault() {
                bundle.run(configuration, environment);

                verify(jerseyEnvironment).register(isA(LoggingSQLExceptionMapper.class));
                verify(jerseyEnvironment).register(isA(LoggingJdbiExceptionMapper.class));
            }

            @Test
            void shouldAllowOptOutOfJdbiExceptionMapperRegistration() {
                var customBundle = new ElucidationBundle<TestJdbiMapperOverrideAppConfig>(jdbiFactory, client) {

                    @Override
                    public PooledDataSourceFactory getDataSourceFactory(TestJdbiMapperOverrideAppConfig configuration) {
                        return dataSourceFactory;
                    }

                    @Override
                    public boolean isRegisterJdbiExceptionMappers(TestJdbiMapperOverrideAppConfig configuration) {
                        return configuration.isRegisterJdbiExceptionMappers();
                    }
                };

                var customConfig = new TestJdbiMapperOverrideAppConfig();

                customBundle.run(customConfig, environment);

                verify(jerseyEnvironment, never()).register(isA(LoggingSQLExceptionMapper.class));
                verify(jerseyEnvironment, never()).register(isA(LoggingJdbiExceptionMapper.class));
            }
        }

        @Nested
        class SetupScheduledJobs {
            @Test
            void shouldSetupArchiveEventsOnlyByDefault() {
                bundle.run(configuration, environment);
                verify(executor).scheduleWithFixedDelay(isA(ArchiveEventsJob.class), eq(1L), eq(60L), eq(TimeUnit.MINUTES));
                verifyNoMoreInteractions(executor);
            }

            @Test
            void shouldSetupPollForEventsJobWhenConfigured() {
                var pollingConfig = PollingConfig.builder()
                        .pollingEndpoint("http://localhost:8080")
                        .build();

                var bundleWithPolling = new ElucidationBundle<>(jdbiFactory, client) {
                    @Override
                    public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                        return dataSourceFactory;
                    }

                    @Override
                    public Optional<PollingConfig> getPollingConfig(Configuration configuration) {
                        return Optional.of(pollingConfig);
                    }
                };

                when(lifecycle.scheduledExecutorService("Event-Polling-Job", true)).thenReturn(builder);

                bundleWithPolling.run(configuration, environment);
                verify(executor).scheduleWithFixedDelay(isA(ArchiveEventsJob.class), eq(1L), eq(60L), eq(TimeUnit.MINUTES));
                verify(executor).scheduleWithFixedDelay(isA(PollForEventsJob.class), eq(1L), eq(1L), eq(TimeUnit.MINUTES));
            }
        }

        @Test
        void shouldSetupResources() {
            bundle.run(configuration, environment);
            verify(jerseyEnvironment).register(isA(RelationshipResource.class));
        }
    }
}
