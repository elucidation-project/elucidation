package com.fortitudetec.elucidation.client;

/*-
 * #%L
 * Elucidation Client
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
