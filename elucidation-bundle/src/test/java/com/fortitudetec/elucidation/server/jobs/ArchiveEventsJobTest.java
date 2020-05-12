package com.fortitudetec.elucidation.server.jobs;

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
