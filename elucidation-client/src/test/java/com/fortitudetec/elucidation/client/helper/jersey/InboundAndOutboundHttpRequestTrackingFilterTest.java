package com.fortitudetec.elucidation.client.helper.jersey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fortitudetec.elucidation.client.helper.app.DummyConfig;
import com.fortitudetec.elucidation.client.helper.app.DummyInboundRequestTrackingApp;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("InboundHttpRequestTrackingFilter")
class InboundAndOutboundHttpRequestTrackingFilterTest {

    public static DropwizardAppExtension<DummyConfig> APP = new DropwizardAppExtension<>(
            DummyInboundRequestTrackingApp.class,
            ResourceHelpers.resourceFilePath("config.yml"));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void clearMock() {
        reset(APP.<DummyInboundRequestTrackingApp>getApplication().getClient());
    }

    @Test
    void shouldRecordInboundAndOutboundGetRequestWithElucidation() {
        var elucidationClient = APP.<DummyInboundRequestTrackingApp>getApplication().getClient();

        APP.client()
                .target("http://localhost:" + APP.getLocalPort())
                .path("/dummy")
                .request()
                .header("ORIGINATING_SERVICE_NAME", "some-other-service")
                .get();

        var captor = ArgumentCaptor.forClass(ConnectionEvent.class);

        verify(elucidationClient, times(2)).recordNewEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting("serviceName", "eventDirection", "communicationType", "connectionIdentifier")
                .containsExactly(
                        tuple("some-other-service", Direction.OUTBOUND, "HTTP", "GET /dummy"),
                        tuple("dummy-service", Direction.INBOUND, "HTTP", "GET /dummy")
                );
    }

    @Test
    void shouldRecordInboundOnlyWhenHeaderIsMissingGetRequestWithElucidation() {
        var elucidationClient = APP.<DummyInboundRequestTrackingApp>getApplication().getClient();

        APP.client()
                .target("http://localhost:" + APP.getLocalPort())
                .path("/dummy")
                .request()
                .get();

        var captor = ArgumentCaptor.forClass(ConnectionEvent.class);

        verify(elucidationClient).recordNewEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting("serviceName", "eventDirection", "communicationType", "connectionIdentifier")
                .containsExactly(
                        tuple("dummy-service", Direction.INBOUND, "HTTP", "GET /dummy")
                );
    }
}
