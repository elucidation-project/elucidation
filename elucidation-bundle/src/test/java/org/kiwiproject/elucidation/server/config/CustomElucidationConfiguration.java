package org.kiwiproject.elucidation.server.config;

import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;

import java.util.List;

/**
 * Test class to support {@link ElucidationConfigurationTest}
 */
class CustomElucidationConfiguration
        extends Configuration
        implements ElucidationConfiguration<TestAppConfig> {

    @Override
    public Duration getTimeToLive(TestAppConfig configuration) {
        return configuration.getCustomTtl();
    }

    @Override
    public List<CommunicationDefinition> getCommunicationDefinitions(TestAppConfig configuration) {
        return configuration.getCommunicationDefinitions();
    }
}
