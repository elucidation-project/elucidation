package com.fortitudetec.elucidation.client.helper.jersey;

import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.fortitudetec.elucidation.client.helper.app.DummyConfig;
import com.fortitudetec.elucidation.client.helper.app.DummyInboundRequestTrackingApp;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("InboundHttpRequestTrackingFilter")
class InboundHttpRequestTrackingFilterTest {

    public static DropwizardAppExtension<DummyConfig> APP = new DropwizardAppExtension<>(DummyInboundRequestTrackingApp.class);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void clearMock() {
        reset(APP.<DummyInboundRequestTrackingApp>getApplication().getClient());
    }

    @Test
    void shouldRecordInboundGetRequestWithElucidation() {
        var elucidationClient = APP.<DummyInboundRequestTrackingApp>getApplication().getClient();

        APP.client().target("http://localhost:" + APP.getLocalPort()).path("/dummy").request().get();

        var captor = ArgumentCaptor.forClass(ConnectionEvent.class);

        verify(elucidationClient).recordNewEvent(captor.capture());

        var arg = captor.getValue();
        assertThat(arg.getServiceName()).isEqualTo("dummy-service");
        assertThat(arg.getEventDirection()).isEqualTo(Direction.INBOUND);
        assertThat(arg.getCommunicationType()).isEqualTo("HTTP");
        assertThat(arg.getConnectionIdentifier()).isEqualTo("GET /dummy");
    }

    @Test
    void shouldRecordInboundPostRequestWithElucidation() {
        var elucidationClient = APP.<DummyInboundRequestTrackingApp>getApplication().getClient();

        APP.client().target("http://localhost:" + APP.getLocalPort()).path("/dummy/post").request().post(json(""));

        var captor = ArgumentCaptor.forClass(ConnectionEvent.class);

        verify(elucidationClient).recordNewEvent(captor.capture());

        var arg = captor.getValue();
        assertThat(arg.getServiceName()).isEqualTo("dummy-service");
        assertThat(arg.getEventDirection()).isEqualTo(Direction.INBOUND);
        assertThat(arg.getCommunicationType()).isEqualTo("HTTP");
        assertThat(arg.getConnectionIdentifier()).isEqualTo("POST /dummy/post");
    }

    @Test
    void shouldRecordInboundPutRequestWithElucidation() {
        var elucidationClient = APP.<DummyInboundRequestTrackingApp>getApplication().getClient();

        APP.client().target("http://localhost:" + APP.getLocalPort()).path("/dummy/1").request().put(json(""));

        var captor = ArgumentCaptor.forClass(ConnectionEvent.class);

        verify(elucidationClient).recordNewEvent(captor.capture());

        var arg = captor.getValue();
        assertThat(arg.getServiceName()).isEqualTo("dummy-service");
        assertThat(arg.getEventDirection()).isEqualTo(Direction.INBOUND);
        assertThat(arg.getCommunicationType()).isEqualTo("HTTP");
        assertThat(arg.getConnectionIdentifier()).isEqualTo("PUT /dummy/{id}");
    }
}
