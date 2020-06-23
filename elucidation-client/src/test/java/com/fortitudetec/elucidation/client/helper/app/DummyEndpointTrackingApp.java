package com.fortitudetec.elucidation.client.helper.app;

import static org.mockito.Mockito.mock;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.Getter;

public class DummyEndpointTrackingApp extends Application<DummyConfig> {

    @Getter
    private ElucidationClient<String> client;

    @SuppressWarnings("unchecked")
    public DummyEndpointTrackingApp() {
        client = mock(ElucidationClient.class);
    }

    @Override
    public void run(DummyConfig configuration, Environment environment) {
        environment.jersey().register(new DummyResource());
        environment.jersey().register(new EndpointTrackingListener<>(environment.jersey().getResourceConfig(), "dummy-service", client));
    }

}
