package com.fortitudetec.elucidation.server.db;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;

@Slf4j
public class DBLoader {

    @SuppressWarnings("UnstableApiUsage")
    public static int loadDb(Jdbi jdbi, String csvFile) throws IOException {
        var mapper = new CsvMapper();
        var schema = CsvSchema.emptySchema().withHeader();

        var url = Resources.getResource(csvFile);
        MappingIterator<ConnectionEvent> iterator = mapper.readerFor(ConnectionEvent.class).with(schema).readValues(url);

        LOG.info("Starting to load events");
        int eventInsertCount = 0;
        while (iterator.hasNext()) {
            var event = iterator.nextValue();

            eventInsertCount += jdbi.withHandle(handle -> handle.execute("insert into connection_events " +
                            "(id, service_name, event_direction, communication_type, connection_identifier, observed_at) " +
                            "values (?, ?, ?, ?, ?, ?)",
                    event.getId(), event.getServiceName(), event.getEventDirection(), event.getCommunicationType(),
                    event.getConnectionIdentifier(), event.getObservedAt()));

        }
        LOG.info("Events loaded {}", eventInsertCount);

        return eventInsertCount;
    }
}
