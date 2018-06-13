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

import com.fortitudetec.elucidation.client.ElucidationEventRecorder.RecordingType;
import com.fortitudetec.elucidation.client.model.CommunicationType;
import com.fortitudetec.elucidation.client.model.ConnectionEvent;
import com.fortitudetec.elucidation.client.model.Direction;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ElucidationEventRecorderTest {

    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/event")
    @Slf4j
    public static class TestElucidationServerResource {

        static final AtomicReference<Response.Status> STATUS =
                new AtomicReference<>(Response.Status.OK);

        @POST
        public Response recordEvent(ConnectionEvent event) {
            LOG.info("Recording event: {}", event);

            return Response.status(STATUS.get()).build();
        }

    }

    private static DropwizardClientExtension client = new DropwizardClientExtension(TestElucidationServerResource.class);

    private ElucidationEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ElucidationEventRecorder(client.baseUri().toString() + "/event");

        TestElucidationServerResource.STATUS.set(Response.Status.OK);
    }

    // TODO Fix problem with ZonedDateTime binding....see newEvent() method for commented out ZDT value

    @Test
    void testRecordEvent() throws InterruptedException, ExecutionException, TimeoutException {
        ConnectionEvent event = newEvent();

        Future<RecorderResult> resultFuture = recorder.recordNewEvent(event);
        RecorderResult result = resultFuture.get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.RECORDED_OK);
        assertThat(result.hasErrorMessage()).isFalse();
        assertThat(result.hasException()).isFalse();
    }

    @Test
    void testRecordEvent_WhenErrorResponse() throws InterruptedException, ExecutionException, TimeoutException {
        ConnectionEvent event = newEvent();

        TestElucidationServerResource.STATUS.set(Response.Status.INTERNAL_SERVER_ERROR);

        Future<RecorderResult> resultFuture = recorder.recordNewEvent(event);
        RecorderResult result = resultFuture.get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.ERROR_RECORDING);
        assertThat(result.hasErrorMessage()).isTrue();
        assertThat(result.getErrorMessage().orElse("")).contains("Status: 500");
        assertThat(result.hasException()).isFalse();
    }

    @Test
    void testRecordEvent_WhenException() throws InterruptedException, ExecutionException, TimeoutException {
        ConnectionEvent event = newEvent();

        recorder = new ElucidationEventRecorder("/not-an-absolute-uri");

        Future<RecorderResult> resultFuture = recorder.recordNewEvent(event);
        RecorderResult result = resultFuture.get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.ERROR_RECORDING);
        assertThat(result.hasErrorMessage()).isFalse();
        assertThat(result.hasException()).isTrue();
        assertThat(result.getException()).containsInstanceOf(ProcessingException.class);
    }

    @Test
    void testRecordEventSync() {
        ConnectionEvent event = newEvent();

        RecorderResult result = recorder.recordNewEventSync(event);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.RECORDED_OK);
        assertThat(result.hasErrorMessage()).isFalse();
        assertThat(result.hasException()).isFalse();
    }

    @Test
    void testRecordEvent_UsingRecordingTypeEnum() throws InterruptedException, ExecutionException, TimeoutException {
        ConnectionEvent event = newEvent();

        Future<RecorderResult> resultFuture = recorder.recordNewEvent(event, RecordingType.ASYNC);
        RecorderResult result = resultFuture.get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.RECORDED_OK);
        assertThat(result.hasErrorMessage()).isFalse();
        assertThat(result.hasException()).isFalse();
    }

    private ConnectionEvent newEvent() {
        return ConnectionEvent.builder()
                .eventDirection(Direction.INBOUND)
                .communicationType(CommunicationType.JMS)
                .connectionIdentifier("SOME_MESSAGE")
//                .observedAt(ZonedDateTime.now())
                .serviceName("my-service")
                .build();
    }

}
