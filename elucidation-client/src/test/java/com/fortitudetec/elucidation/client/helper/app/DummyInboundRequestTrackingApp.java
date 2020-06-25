package com.fortitudetec.elucidation.client.helper.app;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mockito.Mockito.mock;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.Getter;

public class DummyInboundRequestTrackingApp extends Application<DummyConfig> {

    @Getter
    private final ElucidationRecorder recorder;

    public DummyInboundRequestTrackingApp() {
        recorder = mock(ElucidationRecorder.class);
    }

    @Override
    public void run(DummyConfig configuration, Environment environment) {
        environment.jersey().register(new DummyResource());

        if (isBlank(configuration.getOriginatingHeaderName())) {
            environment.jersey().register(new InboundHttpRequestTrackingFilter("dummy-service", recorder));
        } else {
            environment.jersey().register(new InboundHttpRequestTrackingFilter("dummy-service", recorder, new HttpCommunicationDefinition(), configuration.getOriginatingHeaderName()));
        }
    }

}
