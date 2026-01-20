package org.kiwiproject.elucidation.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.jersey.LoggingJdbiExceptionMapper;
import io.dropwizard.jdbi3.jersey.LoggingSQLExceptionMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.elucidation.server.config.PollingConfig;
import org.kiwiproject.elucidation.server.jobs.ArchiveEventsJob;
import org.kiwiproject.elucidation.server.jobs.PollForEventsJob;
import org.kiwiproject.elucidation.server.resources.RelationshipResource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@DisplayName("ElucidationBundle")
class ElucidationBundleTest {

    static class TestAppConfig extends Configuration {
    }

    private TestAppConfig configuration;
    private DataSourceFactory dataSourceFactory;
    private Environment environment;
    private JdbiFactory jdbiFactory;
    private JerseyEnvironment jerseyEnvironment;
    private ScheduledExecutorService executor;
    private ElucidationBundle<TestAppConfig> bundle;
    private Client client;
    private LifecycleEnvironment lifecycle;
    private ScheduledExecutorServiceBuilder scheduledExecutorServiceBuilder;
    private MutableServletContextHandler appContext;

    @BeforeEach
    void setUp() {
        dataSourceFactory = new DataSourceFactory();

        // Mocks
        var jdbi = mock(Jdbi.class);
        lifecycle = mock(LifecycleEnvironment.class);
        scheduledExecutorServiceBuilder = mock(ScheduledExecutorServiceBuilder.class);

        configuration = mock(TestAppConfig.class);
        environment = mock(Environment.class);
        jdbiFactory = mock(JdbiFactory.class);
        jerseyEnvironment = mock(JerseyEnvironment.class);
        executor = mock(ScheduledExecutorService.class);

        appContext = mock(MutableServletContextHandler.class);

        // Expectations
        when(jdbiFactory.build(environment,
                dataSourceFactory,
                "Elucidation-Data-Source")).thenReturn(jdbi);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycle);
        when(environment.getApplicationContext()).thenReturn(appContext);
        when(lifecycle.scheduledExecutorService("Event-Archive-Job", true))
                .thenReturn(scheduledExecutorServiceBuilder);
        when(scheduledExecutorServiceBuilder.build()).thenReturn(executor);

        client = ClientBuilder.newClient();

        // Create a bundle
        bundle = new ElucidationBundle<>(jdbiFactory, client) {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(TestAppConfig configuration) {
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

                when(lifecycle.scheduledExecutorService("Event-Polling-Job", true))
                        .thenReturn(scheduledExecutorServiceBuilder);

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

        @Nested
        class CrossOrigin {

            @Test
            void shouldSetupCorsHandlerByDefault() {
                var corsBundle = new ElucidationBundle<>(jdbiFactory, client) {
                    @Override
                    public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                        return dataSourceFactory;
                    }
                };

                corsBundle.run(configuration, environment);

                verify(appContext).insertHandler(any(CrossOriginHandler.class));
            }

            @Test
            void shouldSetupCorsWithCustomConfiguration() {
                @Getter
                @Setter
                class CustomCorsConfig extends Configuration {
                    Set<String> corsAllowedOriginPatterns = Set.of("http://localhost:63342");
                    Set<String> corsAllowedMethods = Set.of("GET", "POST");
                    Set<String> corsAllowedHeaders = Set.of("Content-Type", "Accept");
                    Boolean corsAllowCredentials = false;
                    Set<String> corsExposedHeaders = Set.of("Access-Control-Allow-Origin");
                }

                var mockCorsHandler = mock(CrossOriginHandler.class);

                var corsBundle = new ElucidationBundle<CustomCorsConfig>(jdbiFactory, client) {
                    @Override
                    CrossOriginHandler newCorsHandler() {
                        return mockCorsHandler;
                    }

                    @Override
                    public Set<String> corsAllowedOriginPatterns(CustomCorsConfig configuration) {
                        return configuration.getCorsAllowedOriginPatterns();
                    }

                    @Override
                    public Set<String> corsAllowedMethods(CustomCorsConfig configuration) {
                        return configuration.getCorsAllowedMethods();
                    }

                    @Override
                    public Set<String> corsAllowedHeaders(CustomCorsConfig configuration) {
                        return configuration.getCorsAllowedHeaders();
                    }

                    @Override
                    public boolean corsAllowCredentials(CustomCorsConfig configuration) {
                        return configuration.getCorsAllowCredentials();
                    }

                    @Override
                    public Set<String> corsExposedHeaders(CustomCorsConfig configuration) {
                        return configuration.getCorsExposedHeaders();
                    }

                    @Override
                    public PooledDataSourceFactory getDataSourceFactory(CustomCorsConfig configuration) {
                        return dataSourceFactory;
                    }
                };

                var config = new CustomCorsConfig();
                corsBundle.run(config, environment);

                var corsHandlerCaptor = ArgumentCaptor.forClass(CrossOriginHandler.class);
                verify(appContext).insertHandler(corsHandlerCaptor.capture());

                var corsHandler = corsHandlerCaptor.getValue();

                verify(corsHandler).setAllowedOriginPatterns(config.getCorsAllowedOriginPatterns());
                verify(corsHandler).setAllowedMethods(config.getCorsAllowedMethods());
                verify(corsHandler).setAllowedHeaders(config.getCorsAllowedHeaders());
                verify(corsHandler).setAllowCredentials(config.getCorsAllowCredentials());
                verify(corsHandler).setExposedHeaders(config.getCorsExposedHeaders());
            }

            @Test
            void shouldSkipCorsHandlerRegistration_WhenNotConfigured() {
                var noCorsBundle = new ElucidationBundle<TestAppConfig>(jdbiFactory, client) {
                    @Override
                    public boolean isCorsEnabled(TestAppConfig configuration) {
                        return false;
                    }

                    @Override
                    public PooledDataSourceFactory getDataSourceFactory(TestAppConfig configuration) {
                        return dataSourceFactory;
                    }
                };

                noCorsBundle.run(configuration, environment);

                verifyNoInteractions(appContext);
            }
        }

    }
}
