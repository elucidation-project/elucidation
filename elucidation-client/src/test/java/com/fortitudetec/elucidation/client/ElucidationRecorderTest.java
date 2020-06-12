package com.fortitudetec.elucidation.client;

/*-
 * #%L
 * Elucidation Client
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
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
import static org.assertj.core.api.Assertions.assertThat;

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("ElucidationEventRecorder")
public class ElucidationRecorderTest {

    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/elucidate")
    @Slf4j
    public static class TestElucidationServerResource {

        static final AtomicReference<Response.Status> STATUS =
                new AtomicReference<>(Response.Status.OK);

        @Path("/event")
        @POST
        public Response recordEvent(ConnectionEvent event) {
            LOG.info("Recording event: {}", event);

            return Response.status(STATUS.get()).build();
        }

        @Path("/trackedIdentifier/{serviceName}/{communicationType}")
        @POST
        public Response track(@PathParam("serviceName") String serviceName,
                              @PathParam("communicationType") String communicationType,
                              List<String> identifiers) {
            LOG.info("Loading identifiers for service {}, communication type: {}.  Identifiers: {}", serviceName, communicationType, identifiers);

            return Response.status(STATUS.get()).build();
        }

    }

    private static final DropwizardClientExtension CLIENT = new DropwizardClientExtension(TestElucidationServerResource.class);

    private ElucidationRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ElucidationRecorder(CLIENT.baseUri().toString());
        TestElucidationServerResource.STATUS.set(Response.Status.OK);
    }

    @Nested
    class RecordEvent {

        @Test
        void shouldReceiveASuccessfulResult_WhenRecordingSucceeds() throws InterruptedException, ExecutionException, TimeoutException {
            var event = newEvent();

            var resultFuture = recorder.recordNewEvent(event);
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isFalse();
        }

        @Test
        void shouldReceiveAnErrorResult_WhenErrorMessageReturnedFromService() throws InterruptedException, ExecutionException, TimeoutException {
            var event = newEvent();

            TestElucidationServerResource.STATUS.set(Response.Status.INTERNAL_SERVER_ERROR);

            var resultFuture = recorder.recordNewEvent(event);
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.hasErrorMessage()).isTrue();
            assertThat(result.getErrorMessage().orElse("")).contains("Status: 500");
            assertThat(result.hasException()).isFalse();
        }

        @Test
        void shouldReceiveAnErrorResult_WhenExceptionIsThrownOnRequest() throws InterruptedException, ExecutionException, TimeoutException {
            var event = newEvent();

            recorder = new ElucidationRecorder("/not-an-absolute-uri");

            var resultFuture = recorder.recordNewEvent(event);
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isTrue();
            assertThat(result.getException()).containsInstanceOf(ProcessingException.class);
        }

        private ConnectionEvent newEvent() {
            return ConnectionEvent.builder()
                    .eventDirection(Direction.INBOUND)
                    .communicationType("JMS")
                    .connectionIdentifier("SOME_MESSAGE")
                    .observedAt(System.currentTimeMillis())
                    .serviceName("my-service")
                    .build();
        }
    }

    @Nested
    class Track {
        @Test
        void shouldReceiveASuccessfulResult_WhenRecordingSucceeds() throws InterruptedException, ExecutionException, TimeoutException {
            var resultFuture = recorder.track("a-service", "HTTP", List.of("/some/path"));
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isFalse();
        }

        @Test
        void shouldReceiveAnErrorResult_WhenErrorMessageReturnedFromService() throws InterruptedException, ExecutionException, TimeoutException {
            TestElucidationServerResource.STATUS.set(Response.Status.INTERNAL_SERVER_ERROR);

            var resultFuture = recorder.track("b-service", "HTTP", List.of("/some/other/path"));
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.hasErrorMessage()).isTrue();
            assertThat(result.getErrorMessage().orElse("")).contains("Status: 500");
            assertThat(result.hasException()).isFalse();
        }

        @Test
        void shouldReceiveAnErrorResult_WhenExceptionIsThrownOnRequest() throws InterruptedException, ExecutionException, TimeoutException {
            recorder = new ElucidationRecorder("/not-an-absolute-uri");

            var resultFuture = recorder.track("c-service", "HTTP", List.of("/never/gonna/get/it"));
            var result = resultFuture.get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isTrue();
            assertThat(result.getException()).containsInstanceOf(ProcessingException.class);
        }
    }

}
