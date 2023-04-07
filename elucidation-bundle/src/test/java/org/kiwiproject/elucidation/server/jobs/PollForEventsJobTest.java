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

import static org.kiwiproject.elucidation.common.test.ConnectionEvents.newConnectionEvent;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.server.resources.RelationshipResource;
import org.kiwiproject.elucidation.server.service.RelationshipService;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.elucidation.server.test.TestConstants;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.ClientBuilder;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
class PollForEventsJobTest {

    private static final RelationshipService SERVICE = mock(RelationshipService.class);
    private static final DropwizardClientExtension RESOURCES = new DropwizardClientExtension(new RelationshipResource(SERVICE));

    private PollForEventsJob job;

    @BeforeEach
    void setUp() {
        job = new PollForEventsJob(()-> RESOURCES.baseUri().toString(), ClientBuilder.newClient(), SERVICE);
        reset(SERVICE);
    }

    @Test
    void firstPoll() {
        var sinceCaptor = ArgumentCaptor.forClass(Long.class);

        when(SERVICE.listEventsSince(anyLong())).thenReturn(newArrayList(
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.INBOUND, TestConstants.MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.IGNORED_MSG)
        ));

        job.run();

        verify(SERVICE, times(3)).createEvent(isA(ConnectionEvent.class));
        verify(SERVICE).listEventsSince(sinceCaptor.capture());

        var expected = ZonedDateTime.now().minus(7, ChronoUnit.DAYS).toInstant().toEpochMilli();
        assertThat(sinceCaptor.getValue()).isCloseTo(expected, Offset.offset(1500L));
    }

    @Test
    void futurePoll() {
        var firstConnectionEvents = List.of(
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.INBOUND, TestConstants.MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.IGNORED_MSG)
        );

        var secondConnectionEvents = List.of(
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.INBOUND, TestConstants.MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(TestConstants.A_SERVICE_NAME, Direction.OUTBOUND, TestConstants.IGNORED_MSG)
        );

        when(SERVICE.listEventsSince(anyLong())).thenReturn(firstConnectionEvents);
        when(SERVICE.listEventsSince(firstConnectionEvents.get(firstConnectionEvents.size()-1).getObservedAt()))
                .thenReturn(secondConnectionEvents);

        // Run first time
        job.run();

        // Run second time
        job.run();

        verify(SERVICE, times(6)).createEvent(isA(ConnectionEvent.class));
    }
}
