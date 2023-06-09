package org.kiwiproject.elucidation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@DisplayName("ElucidationClient")
class ElucidationClientTest {

    private ElucidationClient<String> elucidation;

    @Nested
    class RecordNewEvent {

        @Nested
        class SkipsRecording {
            @Test
            void whenNoOp() throws InterruptedException, ExecutionException, TimeoutException {
                elucidation = ElucidationClient.noop();
                assertSkipped(elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS));
            }

            @Test
            void WhenNotEnabled() throws InterruptedException, ExecutionException, TimeoutException {
                elucidation = ElucidationClient.of(null, null);
                assertSkipped(elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS));

                elucidation = ElucidationClient.of(mock(ElucidationRecorder.class), null);
                assertSkipped(elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS));

                elucidation = ElucidationClient.of(null, value -> Optional.of(ConnectionEvent.builder().build()));
                assertSkipped(elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS));
            }
        }

        @Nested
        class ReturnsError {
            @Test
            void whenNoEvent() throws InterruptedException, ExecutionException, TimeoutException {
                var recorder = mock(ElucidationRecorder.class);

                elucidation = ElucidationClient.of(recorder, value -> Optional.empty());

                var result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);

                assertThat(result.getStatus()).isEqualTo(Status.ERROR);
                assertThat(result.hasException()).isFalse();
                assertThat(result.hasErrorMessage()).isTrue();
                assertThat(result.getErrorMessage()).contains("event is missing; cannot record");

                verifyNoInteractions(recorder);
            }

            @Test
            void whenEnabled_ButMessageIsNull() throws InterruptedException, ExecutionException, TimeoutException {
                var recorder = mock(ElucidationRecorder.class);
                elucidation = ElucidationClient.of(recorder, value -> Optional.of(ConnectionEvent.builder().build()));

                var result = elucidation.recordNewEvent(null).get(1, TimeUnit.SECONDS);

                assertThat(result.getStatus()).isEqualTo(Status.ERROR);
                assertThat(result.hasErrorMessage()).isTrue();
                assertThat(result.getErrorMessage()).contains("input is null; cannot create event");

                verifyNoInteractions(recorder);
            }

            @Test
            void WhenExceptionThrownRecording() throws InterruptedException, ExecutionException, TimeoutException {
                var recorder = mock(ElucidationRecorder.class);

                var event = ConnectionEvent.builder().build();

                elucidation = ElucidationClient.of(recorder, value -> Optional.of(event));

                doThrow(new RuntimeException("oops")).when(recorder).recordNewEvent(any());

                var result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);

                assertThat(result.getStatus()).isEqualTo(Status.ERROR);
                assertThat(result.hasException()).isTrue();
                assertThat(result.getException().orElseThrow(IllegalStateException::new))
                        .isExactlyInstanceOf(RuntimeException.class)
                        .hasMessage("oops");

                verify(recorder).recordNewEvent(same(event));
            }
        }

        @Test
        void shouldReturnSuccessResult_WhenProcessedOk() throws InterruptedException, ExecutionException, TimeoutException {
            var recorder = mock(ElucidationRecorder.class);
            when(recorder.recordNewEvent(any())).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));

            var event = ConnectionEvent.builder().build();

            elucidation = ElucidationClient.of(recorder, value -> Optional.of(event));

            var result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(result.hasSkipMessage()).isFalse();
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isFalse();

            verify(recorder).recordNewEvent(same(event));
        }
    }

    @Nested
    class TrackIdentifiers {

        @Nested
        class SkipsSending {
            @Test
            void whenNoOp() throws InterruptedException, ExecutionException, TimeoutException {
                elucidation = ElucidationClient.noop();
                assertSkipped(elucidation.trackIdentifiers("foo", "HTTP", List.of()).get(1, TimeUnit.SECONDS));
            }

            @Test
            void WhenNotEnabled() throws InterruptedException, ExecutionException, TimeoutException {
                elucidation = ElucidationClient.of(null, null);
                assertSkipped(elucidation.trackIdentifiers("foo", "HTTP", List.of()).get(1, TimeUnit.SECONDS));

                elucidation = ElucidationClient.of(mock(ElucidationRecorder.class), null);
                assertSkipped(elucidation.trackIdentifiers("foo", "HTTP", List.of()).get(1, TimeUnit.SECONDS));

                elucidation = ElucidationClient.of(null, value -> Optional.of(ConnectionEvent.builder().build()));
                assertSkipped(elucidation.trackIdentifiers("foo", "HTTP", List.of()).get(1, TimeUnit.SECONDS));
            }
        }

        @Nested
        class ReturnsError {
            @Test
            void WhenExceptionThrownRecording() throws InterruptedException, ExecutionException, TimeoutException {
                var recorder = mock(ElucidationRecorder.class);

                var service = "foo";
                var type = "HTTP";
                var identifiers = List.<String>of();

                elucidation = ElucidationClient.of(recorder, value -> Optional.of(ConnectionEvent.builder().build()));

                doThrow(new RuntimeException("oops")).when(recorder).track(any(), any(), any());

                var result = elucidation.trackIdentifiers(service, type, identifiers).get(1, TimeUnit.SECONDS);

                assertThat(result.getStatus()).isEqualTo(Status.ERROR);
                assertThat(result.hasException()).isTrue();
                assertThat(result.getException().orElseThrow(IllegalStateException::new))
                        .isExactlyInstanceOf(RuntimeException.class)
                        .hasMessage("oops");

                verify(recorder).track(same(service), same(type), same(identifiers));
            }
        }

        @Test
        void shouldReturnSuccessResult_WhenProcessedOk() throws InterruptedException, ExecutionException, TimeoutException {
            var recorder = mock(ElucidationRecorder.class);
            when(recorder.track(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));

            var service = "foo";
            var type = "HTTP";
            var identifiers = List.<String>of();

            elucidation = ElucidationClient.of(recorder, value -> Optional.of(ConnectionEvent.builder().build()));

            var result = elucidation.trackIdentifiers(service, type, identifiers).get(1, TimeUnit.SECONDS);

            assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(result.hasSkipMessage()).isFalse();
            assertThat(result.hasErrorMessage()).isFalse();
            assertThat(result.hasException()).isFalse();

            verify(recorder).track(same(service), same(type), same(identifiers));
        }
    }

    private void assertSkipped(ElucidationResult result) {
        assertThat(result.getStatus()).isEqualTo(Status.SKIPPED);
        assertThat(result.hasSkipMessage()).isTrue();
        assertThat(result.getSkipMessage()).contains("Recorder not enabled");
    }

}