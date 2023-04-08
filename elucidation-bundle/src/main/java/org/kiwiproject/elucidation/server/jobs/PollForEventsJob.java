package org.kiwiproject.elucidation.server.jobs;

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
