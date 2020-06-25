package com.fortitudetec.elucidation.client.helper.dropwizard;

import static org.mockito.Mockito.verifyNoInteractions;

import com.fortitudetec.elucidation.client.helper.app.DummyConfig;
import com.fortitudetec.elucidation.client.helper.app.DummyEndpointTrackingApp;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("EmptyEndpointTrackingListener")
@ExtendWith(DropwizardExtensionsSupport.class)
class EmptyEndpointTrackingListenerTest {

    public static DropwizardAppExtension<DummyConfig> APP = new DropwizardAppExtension<>(DummyEndpointTrackingApp.class);

    @Test
    void shouldNotRegisterEndpointPathsWithElucidation() {
        var elucidationClient = APP.<DummyEndpointTrackingApp>getApplication().getClient();
        verifyNoInteractions(elucidationClient);
    }
}
