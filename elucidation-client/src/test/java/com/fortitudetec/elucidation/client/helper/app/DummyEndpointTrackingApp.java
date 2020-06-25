package com.fortitudetec.elucidation.client.helper.app;

import static org.mockito.Mockito.mock;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
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
