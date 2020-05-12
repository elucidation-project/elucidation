package com.fortitudetec.elucidation.server;

/*-
 * #%L
 * Elucidation Bundle
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.server.config.PollingConfig;
import com.fortitudetec.elucidation.server.jobs.ArchiveEventsJob;
import com.fortitudetec.elucidation.server.jobs.PollForEventsJob;
import com.fortitudetec.elucidation.server.resources.RelationshipResource;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jdbi3.jersey.LoggingJdbiExceptionMapper;
import io.dropwizard.jdbi3.jersey.LoggingSQLExceptionMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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

    @BeforeEach
    void setUp() {
        dataSourceFactory = new DataSourceFactory();

        // Mocks
        Jdbi jdbi = mock(Jdbi.class);
        lifecycle = mock(LifecycleEnvironment.class);
        builder = mock(ScheduledExecutorServiceBuilder.class);

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

            verify(jdbiFactory).build(eq(environment),
                    eq(dataSourceFactory),
                    eq("Elucidation-Data-Source"));
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

                when(lifecycle.scheduledExecutorService(eq("Event-Polling-Job"), eq(true))).thenReturn(builder);

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
