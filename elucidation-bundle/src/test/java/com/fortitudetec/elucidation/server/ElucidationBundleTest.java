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
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ElucidationBundleTest {

    private final Configuration configuration = mock(Configuration.class);
    private final DataSourceFactory dataSourceFactory = new DataSourceFactory();
    private final Environment environment = mock(Environment.class);
    private final JdbiFactory jdbiFactory = mock(JdbiFactory.class);
    private final Jdbi jdbi = mock(Jdbi.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final LifecycleEnvironment lifecycle = mock(LifecycleEnvironment.class);
    private final ScheduledExecutorServiceBuilder builder = mock(ScheduledExecutorServiceBuilder.class);
    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    private final ElucidationBundle<Configuration> bundle = new ElucidationBundle<>(jdbiFactory) {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
            return dataSourceFactory;
        }
    };

    @BeforeEach
    void setUp() {
        when(jdbiFactory.build(eq(environment),
                eq(dataSourceFactory),
                eq("Elucidation-Data-Source"))).thenReturn(jdbi);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycle);
        when(lifecycle.scheduledExecutorService(eq("Event-Archive-Job"), eq(true))).thenReturn(builder);
        when(builder.build()).thenReturn(executor);
    }

    @Test
    void testJdbiSetup() {
        bundle.run(configuration, environment);

        verify(jdbiFactory).build(eq(environment),
                eq(dataSourceFactory),
                eq("Elucidation-Data-Source"));

    }

    @Test
    void testResourcesSetup() {
        bundle.run(configuration, environment);

        verify(jerseyEnvironment).register(isA(RelationshipResource.class));
    }

    @Test
    void testScheduledJobsSetup() {
        bundle.run(configuration, environment);

        verify(executor).scheduleWithFixedDelay(isA(ArchiveEventsJob.class), eq(1L), eq(60L), eq(TimeUnit.MINUTES));
    }
}
