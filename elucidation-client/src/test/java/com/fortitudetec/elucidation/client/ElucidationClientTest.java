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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.client.model.ConnectionEvent;
import com.google.common.util.concurrent.Futures;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

class ElucidationClientTest {

    private ElucidationClient<String> elucidation;

    @Test
    void testRecordNewEvent_ForNoOp_SkipsRecording() throws InterruptedException, ExecutionException, TimeoutException {
        elucidation = ElucidationClient.noop();
        assertSkipped(elucidation);
    }

    @Test
    void testRecordNewEvent_WhenNotEnabled_SkipsRecording() throws InterruptedException, ExecutionException, TimeoutException {
        assertSkipped(null, null);
        assertSkipped(mock(ElucidationEventRecorder.class), null);
        assertSkipped(null, value -> Optional.of(ConnectionEvent.builder().build()));
    }

    @Test
    void testRecordNewEvent_WhenEnabled_ButMessageIsNull() throws InterruptedException, ExecutionException, TimeoutException {
        ElucidationEventRecorder recorder = mock(ElucidationEventRecorder.class);
        elucidation = ElucidationClient.of(recorder, value -> Optional.of(ConnectionEvent.builder().build()));

        RecorderResult result = elucidation.recordNewEvent(null).get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.ERROR_RECORDING);
        assertThat(result.hasErrorMessage()).isTrue();
        assertThat(result.getErrorMessage()).contains("input is null; cannot create event");

        verifyZeroInteractions(recorder);
    }

    @Test
    void testRecordNewEvent_WhenNoEvent_SkipsRecording() throws InterruptedException, ExecutionException, TimeoutException {
        ElucidationEventRecorder recorder = mock(ElucidationEventRecorder.class);

        elucidation = ElucidationClient.of(recorder, value -> Optional.empty());

        RecorderResult result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.ERROR_RECORDING);
        assertThat(result.hasException()).isFalse();
        assertThat(result.hasErrorMessage()).isTrue();
        assertThat(result.getErrorMessage()).contains("event is missing; cannot record");

        verifyZeroInteractions(recorder);
    }

    @Test
    void testRecordNewEvent_WhenExceptionThrownRecording() throws InterruptedException, ExecutionException, TimeoutException {
        ElucidationEventRecorder recorder = mock(ElucidationEventRecorder.class);

        ConnectionEvent event = ConnectionEvent.builder().build();

        elucidation = ElucidationClient.of(recorder, value -> Optional.of(event));

        doThrow(new RuntimeException("oops")).when(recorder).recordNewEvent(any());

        RecorderResult result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.ERROR_RECORDING);
        assertThat(result.hasException()).isTrue();
        assertThat(result.getException().orElseThrow(IllegalStateException::new))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("oops");
        
        verify(recorder).recordNewEvent(same(event));
    }

    @Test
    void testRecordNewEvent_WhenProcessedOk() throws InterruptedException, ExecutionException, TimeoutException {
        ElucidationEventRecorder recorder = mock(ElucidationEventRecorder.class);
        when(recorder.recordNewEvent(any())).thenReturn(Futures.immediateFuture(RecorderResult.ok()));

        ConnectionEvent event = ConnectionEvent.builder().build();

        elucidation = ElucidationClient.of(recorder, value -> Optional.of(event));

        RecorderResult result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS); 
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.RECORDED_OK);
        assertThat(result.hasSkipMessage()).isFalse();
        assertThat(result.hasErrorMessage()).isFalse();
        assertThat(result.hasException()).isFalse();

        verify(recorder).recordNewEvent(same(event));
    }

    private void assertSkipped(ElucidationEventRecorder recorder,
                               Function<String, Optional<ConnectionEvent>> eventFactory) 
            throws InterruptedException, ExecutionException, TimeoutException {

        elucidation = ElucidationClient.of(recorder, eventFactory);
        assertSkipped(elucidation);
    }

    private void assertSkipped(ElucidationClient<String> elucidation)
            throws InterruptedException, ExecutionException, TimeoutException {
        
        RecorderResult result = elucidation.recordNewEvent("{}").get(1, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(RecordingStatus.SKIPPED_RECORDING);
        assertThat(result.hasSkipMessage()).isTrue();
        assertThat(result.getSkipMessage()).contains("Recorder not enabled");
    }

}