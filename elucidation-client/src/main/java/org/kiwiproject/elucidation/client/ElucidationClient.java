package org.kiwiproject.elucidation.client;

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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Client for creating new events using an {@link ElucidationRecorder} and the supplied transformer from some
 * type {@code T} to {@link ConnectionEvent}.
 */
@SuppressWarnings("WeakerAccess") // it's a library
@Slf4j
public class ElucidationClient<T> {

    private final ElucidationRecorder eventRecorder;
    private final Function<T, Optional<ConnectionEvent>> eventFactory;
    private final boolean enabled;

    private ElucidationClient(ElucidationRecorder recorder, Function<T, Optional<ConnectionEvent>> eventFactory) {
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
     *
     * @param recorder responsible for recording a given event
     * @param eventFactory factory for creating a new {@link ConnectionEvent}
     * @param <T> the type that will be transformed into an {@link ConnectionEvent}
     * @return a new instance of {@link ElucidationClient}
     */
    public static <T> ElucidationClient<T> of(ElucidationRecorder recorder, Function<T, Optional<ConnectionEvent>> eventFactory) {
        return new ElucidationClient<>(recorder, eventFactory);
    }

    /**
     * Create a no-op instance that does nothing.
     * @param <T> the type that will be transformed into an {@link ConnectionEvent}
     * @return a new instance of {@link ElucidationClient}
     */
    public static <T> ElucidationClient<T> noop() {
        return new ElucidationClient<>(null, null);
    }

    /**
     * Asynchronously records a new event for the given input.
     * @param input the custom input to be recorded. Using the original factory, this will be transformed into a {@link ConnectionEvent}
     * @return a future that will contain the result of recording the event
     */
    public CompletableFuture<ElucidationResult> recordNewEvent(T input) {
        if (!enabled) {
            var result = ElucidationResult.fromSkipMessage("Recorder not enabled");
            return CompletableFuture.completedFuture(result);
        }

        if (isNull(input)) {
            var result = ElucidationResult.fromErrorMessage("input is null; cannot create event");
            return CompletableFuture.completedFuture(result);
        }

        ConnectionEvent event = null;
        try {
            var optionalEvent = eventFactory.apply(input);

            if (optionalEvent.isPresent()) {
                event = optionalEvent.get();
                return eventRecorder.recordNewEvent(event);
            }

            var result = ElucidationResult.fromErrorMessage("event is missing; cannot record");
            return CompletableFuture.completedFuture(result);
        } catch (Exception ex) {
            LOG.warn("Error recording Elucidation event: {}", event, ex);
            var result = ElucidationResult.fromException(ex);
            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Asynchronously requests to track the given identifiers of the given type for the given service.
     *
     * @param serviceName       The name of the service tied to the identifiers
     * @param communicationType The communication type that are tied to the identifiers (e.g. HTTP or JMS)
     * @param identifiers       The list of identifiers that are to be tracked for usage
     * @return a future that will contain the result of the request to the elucidation server
     */
    public CompletableFuture<ElucidationResult> trackIdentifiers(String serviceName, String communicationType, List<String> identifiers) {
        if (!enabled) {
            var result = ElucidationResult.fromSkipMessage("Recorder not enabled");
            return CompletableFuture.completedFuture(result);
        }

        try {
            return eventRecorder.track(serviceName, communicationType, identifiers);
        } catch (Exception ex) {
            LOG.warn("Error sending identifiers to elucidation for service {}: {}", serviceName, identifiers, ex);
            var result = ElucidationResult.fromException(ex);
            return CompletableFuture.completedFuture(result);
        }
    }

}