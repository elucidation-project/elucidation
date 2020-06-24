package com.fortitudetec.elucidation.server.db;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@Slf4j
public class DBLoader {

    public static void loadDb(Jdbi jdbi) throws IOException {
        var mapper = new CsvMapper();
        var schema = CsvSchema.emptySchema().withHeader();

        loadEvents(jdbi, mapper, schema);
        loadTrackedIdentifiers(jdbi, mapper, schema);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void loadEvents(Jdbi jdbi, CsvMapper mapper, CsvSchema schema) throws IOException {
        var url = Resources.getResource("elucidation-events.csv");
        MappingIterator<ConnectionEvent> iterator = mapper.readerFor(ConnectionEvent.class).with(schema).readValues(url);

        LOG.info("Starting to load events");
        var eventInsertCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .mapToInt(event ->
                    jdbi.withHandle(handle -> handle.execute("insert into connection_events " +
                            "(id, service_name, event_direction, communication_type, connection_identifier, observed_at) " +
                            "values (?, ?, ?, ?, ?, ?)",
                    event.getId(), event.getServiceName(), event.getEventDirection(), event.getCommunicationType(),
                    event.getConnectionIdentifier(), event.getObservedAt()))).sum();
        LOG.info("Events loaded {}", eventInsertCount);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void loadTrackedIdentifiers(Jdbi jdbi, CsvMapper mapper, CsvSchema schema) throws IOException {
        var url = Resources.getResource("elucidation-tracked-identifiers.csv");
        MappingIterator<TrackedConnectionIdentifier> iterator = mapper.readerFor(TrackedConnectionIdentifier.class).with(schema).readValues(url);

        LOG.info("Starting to load tracked identifiers");
        var trackedInsertCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .mapToInt(trackedIdentifier ->
                        jdbi.withHandle(handle -> handle.execute("insert into tracked_connection_identifiers " +
                                "(id, service_name, communication_type, connection_identifier) " +
                                "values (?, ?, ?, ?)",
                        trackedIdentifier.getId(), trackedIdentifier.getServiceName(), trackedIdentifier.getCommunicationType(),
                        trackedIdentifier.getConnectionIdentifier()))).sum();
        LOG.info("Tracked Identifiers loaded {}", trackedInsertCount);
    }
}
