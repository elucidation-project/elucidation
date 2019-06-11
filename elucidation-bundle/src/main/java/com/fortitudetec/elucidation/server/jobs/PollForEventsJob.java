package com.fortitudetec.elucidation.server.jobs;

import static org.apache.commons.lang3.math.NumberUtils.max;

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.server.service.RelationshipService;

import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.function.Supplier;

@NotThreadSafe
public class PollForEventsJob implements Runnable {

    private static final GenericType<List<ConnectionEvent>> GENERIC_LIST_OF_CONNECTION_EVENTS = new GenericType<>() {
    };

    private Supplier<String> elucidationEndpointSupplier;
    private Client client;
    private RelationshipService relationshipService;
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
