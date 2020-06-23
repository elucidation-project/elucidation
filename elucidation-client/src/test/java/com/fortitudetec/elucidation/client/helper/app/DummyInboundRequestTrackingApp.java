package com.fortitudetec.elucidation.client.helper.app;

import static org.mockito.Mockito.mock;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.Getter;

public class DummyInboundRequestTrackingApp extends Application<DummyConfig> {

    @Getter
    private ElucidationClient<ConnectionEvent> client;

    @SuppressWarnings("unchecked")
    public DummyInboundRequestTrackingApp() {
        client = mock(ElucidationClient.class);
    }

    @Override
    public void run(DummyConfig configuration, Environment environment) {
        environment.jersey().register(new DummyResource());
        environment.jersey().register(new InboundHttpRequestTrackingFilter("dummy-service", client, new HttpCommunicationDefinition(), configuration.getOriginatingHeaderName()));
    }

}
