package org.kiwiproject.elucidation.client.helper.app;

import static org.mockito.Mockito.mock;

import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;

public class DummyEndpointTrackingApp extends Application<DummyConfig> {

    @Getter
    private final ElucidationRecorder recorder;

    public DummyEndpointTrackingApp() {
        recorder = mock(ElucidationRecorder.class);
    }

    @Override
    public void run(DummyConfig configuration, Environment environment) {
        if (configuration.isLoadDummyResource()) {
            environment.jersey().register(new DummyResource());
        }

        environment.jersey().register(new EndpointTrackingListener(environment.jersey().getResourceConfig(), "dummy-service", recorder));
    }

}
