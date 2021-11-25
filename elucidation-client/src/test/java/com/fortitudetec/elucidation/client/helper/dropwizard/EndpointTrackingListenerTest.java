package com.fortitudetec.elucidation.client.helper.dropwizard;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fortitudetec.elucidation.client.helper.app.DummyConfig;
import com.fortitudetec.elucidation.client.helper.app.DummyEndpointTrackingApp;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@DisplayName("EndpointTrackingListener")
@ExtendWith(DropwizardExtensionsSupport.class)
class EndpointTrackingListenerTest {

    private static final DropwizardAppExtension<DummyConfig> APP = new DropwizardAppExtension<>(
            DummyEndpointTrackingApp.class,
            ResourceHelpers.resourceFilePath("config.yml"));

    @Test
    void shouldRegisterEndpointPathsWithElucidation() {
        var elucidationRecorder = APP.<DummyEndpointTrackingApp>getApplication().getRecorder();
        verify(elucidationRecorder).track(eq("dummy-service"),
                eq("HTTP"),
                argThat(list -> list.containsAll(List.of("GET /dummy", "POST /dummy/post", "PUT /dummy/{id}"))));
    }

}
