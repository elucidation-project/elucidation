package com.fortitudetec.elucidation.server;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.server.jobs.ArchiveEventsJob;
import com.fortitudetec.elucidation.server.resources.RelationshipResource;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void setUp() {
        dataSourceFactory = new DataSourceFactory();

        // Mocks
        Jdbi jdbi = mock(Jdbi.class);
        LifecycleEnvironment lifecycle = mock(LifecycleEnvironment.class);
        ScheduledExecutorServiceBuilder builder = mock(ScheduledExecutorServiceBuilder.class);

        configuration = mock(Configuration.class);
        environment = mock(Environment.class);
        jdbiFactory = mock(JdbiFactory.class);
        jerseyEnvironment = mock(JerseyEnvironment.class);
        executor = mock(ScheduledExecutorService.class);

        // Expectations
        when(jdbiFactory.build(eq(environment),
                eq(dataSourceFactory),
                eq("Elucidation-Data-Source"))).thenReturn(jdbi);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycle);
        when(lifecycle.scheduledExecutorService(eq("Event-Archive-Job"), eq(true))).thenReturn(builder);
        when(builder.build()).thenReturn(executor);

        // Create bundle
        bundle = new ElucidationBundle<>(jdbiFactory) {
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

            verify(jdbiFactory).build(eq(environment),
                    eq(dataSourceFactory),
                    eq("Elucidation-Data-Source"));
        }

        @Test
        void shouldSetupScheduledJobs() {
            bundle.run(configuration, environment);
            verify(executor).scheduleWithFixedDelay(isA(ArchiveEventsJob.class), eq(1L), eq(60L), eq(TimeUnit.MINUTES));
        }

        @Test
        void shouldSetupResources() {
            bundle.run(configuration, environment);
            verify(jerseyEnvironment).register(isA(RelationshipResource.class));
        }
    }
}