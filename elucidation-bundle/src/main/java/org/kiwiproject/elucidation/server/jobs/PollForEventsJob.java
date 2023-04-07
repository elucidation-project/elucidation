package org.kiwiproject.elucidation.server.jobs;

/*-
 * #%L
 * Elucidation Bundle
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

import static org.apache.commons.lang3.math.NumberUtils.max;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.server.service.RelationshipService;

import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

/**
 * @implNote This should only be run in a single background thread, e.g. via an
 * {@link java.util.concurrent.ExecutorService}.
 */
@NotThreadSafe
public class PollForEventsJob implements Runnable {

    private static final GenericType<List<ConnectionEvent>> GENERIC_LIST_OF_CONNECTION_EVENTS = new GenericType<>() {
    };

    private final Supplier<String> elucidationEndpointSupplier;
    private final Client client;
    private final RelationshipService relationshipService;
    private long lastEventTimestamp = ZonedDateTime.now().minus(7, ChronoUnit.DAYS).toInstant().toEpochMilli();

    public PollForEventsJob(Supplier<String> elucidationEndpointSupplier,
                            Client client,
                            RelationshipService relationshipService) {

        this.elucidationEndpointSupplier = elucidationEndpointSupplier;
        this.client = client;
        this.relationshipService = relationshipService;
    }

    @Override
    public void run() {
        String endpoint = elucidationEndpointSupplier.get();

        var response = client.target(endpoint)
                .path("/elucidate/events")
                .queryParam("since", lastEventTimestamp)
                .request()
                .get();

        var connectionEvents = response.readEntity(GENERIC_LIST_OF_CONNECTION_EVENTS);

        connectionEvents.forEach(event -> {
            lastEventTimestamp = max(lastEventTimestamp, event.getObservedAt());
            relationshipService.createEvent(event.withId(null));
        });
    }
}
