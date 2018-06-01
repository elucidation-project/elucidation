package com.fortitudetec.elucidation.server.jobs;

import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
public class ArchiveEventsJob implements Runnable {

    private ConnectionEventDao dao;
    private Duration timeToLive;

    public ArchiveEventsJob(ConnectionEventDao dao, Duration timeToLive) {
        this.dao = dao;
        this.timeToLive = timeToLive;
    }

    @Override
    public void run() {
        log.debug("Cleaning up expired events");

        int numDeleted = dao.deleteExpiredEvents(ZonedDateTime.now().minusMinutes(timeToLive.toMinutes()));

        log.info("Deleted {} events", numDeleted);
    }
}
