package org.kiwiproject.elucidation.server.config;

import io.dropwizard.util.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Setter
@Getter
public class PollingConfig {

    /**
     * The duration between polling executions.
     */
    @NonNull
    @Builder.Default
    private Duration pollingInterval = Duration.minutes(1);

    /**
     * The duration to wait after startup that the polling will start.
     */
    @NonNull
    @Builder.Default
    private Duration pollingDelay = Duration.minutes(1);

    /**
     * The static endpoint to use for polling.  For dynamic endpoint resolution see ElucidationConfiguration#getPollEndpointSupplier
     */
    private String pollingEndpoint;

}
