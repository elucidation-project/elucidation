package com.fortitudetec.elucidation.server.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import io.dropwizard.util.Duration;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;

class ArchiveEventsJobTest {

    private ArchiveEventsJob job;
    private ConnectionEventDao dao;

    @BeforeEach
    void setUp() {
        dao = mock(ConnectionEventDao.class);
        job = new ArchiveEventsJob(dao, Duration.days(7));
    }

    @Test
    void testArchiveEvents() {
        long expectedTime = ZonedDateTime.now().minusMinutes(Duration.days(7).toMinutes()).toInstant().toEpochMilli();

        job.run();

        var longArgumentCaptor = ArgumentCaptor.forClass(Long.class);

        verify(dao).deleteExpiredEvents(longArgumentCaptor.capture());

        assertThat(longArgumentCaptor.getValue()).isCloseTo(expectedTime, Offset.offset(500L));
    }
}
