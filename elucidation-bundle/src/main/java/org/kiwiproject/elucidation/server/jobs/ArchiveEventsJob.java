package org.kiwiproject.elucidation.server.jobs;

import org.kiwiproject.elucidation.server.db.ConnectionEventDao;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
public class ArchiveEventsJob implements Runnable {

    private final ConnectionEventDao dao;
    private final Duration timeToLive;

    public ArchiveEventsJob(ConnectionEventDao dao, Duration timeToLive) {
        this.dao = dao;
        this.timeToLive = timeToLive;
    }

    @Override
    public void run() {
        LOG.debug("Cleaning up expired events");

        try {
            int numDeleted = dao.deleteExpiredEvents(ZonedDateTime.now().minusMinutes(timeToLive.toMinutes()).toInstant().toEpochMilli());
            LOG.info("Deleted {} events", numDeleted);
        } catch (Exception e) {
            LOG.error("Error when attempting to clean up events", e);
        }
    }

}
