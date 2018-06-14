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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fortitudetec.elucidation.client.model.ConnectionEvent;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

/**
 * Client for creating new events using an {@link ElucidationEventRecorder} and the supplied transformer from some
 * type {@code T} to {@link ConnectionEvent}.
 */
@Slf4j
public class ElucidationClient<T> {

    private final ElucidationEventRecorder eventRecorder;
    private final Function<T, Optional<ConnectionEvent>> eventFactory;
    private final boolean enabled;

    private ElucidationClient(ElucidationEventRecorder recorder, Function<T, Optional<ConnectionEvent>> eventFactory) {
        this.eventRecorder = recorder;
        this.eventFactory = eventFactory;
        this.enabled = nonNull(recorder) && nonNull(eventFactory);

        if (!enabled) {
            String recorderNull = isNull(recorder) ? "recorder: null" : "recorder: OK";
            String eventFactoryNull = isNull(eventFactory) ? "eventFactory: null" : "eventFactory: OK";
            LOG.warn("This ElucidationClient is not enabled so no events will be recorded ({}, {})", recorderNull, eventFactoryNull);
        }
    }

    /**
     * Create a new instance for the given recorder and event factory.
     */
    public static <T> ElucidationClient<T> of(ElucidationEventRecorder recorder, Function<T, Optional<ConnectionEvent>> eventFactory) {
        return new ElucidationClient(recorder, eventFactory);
    }

    /**
     * Create a no-op instance that does nothing.
     */
    public static <T> ElucidationClient<T> noop() {
        return new ElucidationClient(null, null);
    }    

    /**
     * Asynchronously records a new event for the given input.
     */
    public ListenableFuture<RecorderResult> recordNewEvent(T input) {
        if (!enabled) {
            RecorderResult result = RecorderResult.fromSkipMessage("Recorder not enabled");
            return Futures.immediateFuture(result);
        }

        if (isNull(input)) {
            RecorderResult result = RecorderResult.fromErrorMessage("input is null; cannot create event");
            return Futures.immediateFuture(result);
        }

        ConnectionEvent event = null;
        try {
            Optional<ConnectionEvent> optionalEvent = eventFactory.apply(input);

            if (optionalEvent.isPresent()) {
                event = optionalEvent.get();
                return eventRecorder.recordNewEvent(event);
            }

            RecorderResult result = RecorderResult.fromErrorMessage("event is missing; cannot record");
            return Futures.immediateFuture(result);
        } catch (Exception ex) {
            LOG.warn("Error recording Elucidation event: {}", event, ex);
            RecorderResult result = RecorderResult.fromException(ex);
            return Futures.immediateFuture(result);
        }
    }

}