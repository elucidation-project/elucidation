package com.fortitudetec.elucidation.client;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fortitudetec.elucidation.client.model.CommunicationType;
import com.fortitudetec.elucidation.client.model.ConnectionEvent;
import com.fortitudetec.elucidation.client.model.Direction;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ElucidationEventRecorderTest {

    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/")
    public static class TestElucidationServerResource {

        @POST
        public Response recordEvent(ConnectionEvent event) {

            return Response.ok().build();
        }
    }

    private static DropwizardClientExtension client = new DropwizardClientExtension(TestElucidationServerResource.class);
    private ElucidationEventRecorder recorder = new ElucidationEventRecorder(client.baseUri().toString());

    @Test
    void testRecordEvent() {
        ConnectionEvent event = ConnectionEvent.builder()
            .eventDirection(Direction.INBOUND)
            .communicationType(CommunicationType.JMS)
            .connectionIdentifier("SOME_MESSAGE")
            .observedAt(ZonedDateTime.now())
            .serviceName("my-service")
            .build();

        recorder.recordNewEvent(event);

    }
}
