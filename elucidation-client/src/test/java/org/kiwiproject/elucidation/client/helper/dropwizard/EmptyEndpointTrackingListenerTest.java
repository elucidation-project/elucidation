package org.kiwiproject.elucidation.client.helper.dropwizard;

import org.kiwiproject.elucidation.client.helper.app.DummyConfig;
import org.kiwiproject.elucidation.client.helper.app.DummyEndpointTrackingApp;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@DisplayName("EmptyEndpointTrackingListener")
@ExtendWith(DropwizardExtensionsSupport.class)
class EmptyEndpointTrackingListenerTest {

    private static final DropwizardAppExtension<DummyConfig> APP = new DropwizardAppExtension<>(DummyEndpointTrackingApp.class);

    @Test
    void shouldNotRegisterEndpointPathsWithElucidation() {
        var elucidationRecorder = APP.<DummyEndpointTrackingApp>getApplication().getRecorder();
        Mockito.verifyNoInteractions(elucidationRecorder);
    }
}
