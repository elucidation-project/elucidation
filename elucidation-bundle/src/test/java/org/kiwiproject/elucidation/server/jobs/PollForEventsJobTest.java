package org.kiwiproject.elucidation.server.jobs;

import static org.kiwiproject.elucidation.common.test.ConnectionEvents.newConnectionEvent;
import static org.kiwiproject.elucidation.server.test.TestConstants.A_SERVICE_NAME;
import static org.kiwiproject.elucidation.server.test.TestConstants.IGNORED_MSG;
import static org.kiwiproject.elucidation.server.test.TestConstants.MSG_FROM_ANOTHER_SERVICE;
import static org.kiwiproject.elucidation.server.test.TestConstants.MSG_TO_ANOTHER_SERVICE;
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
import org.mockito.ArgumentCaptor;

import jakarta.ws.rs.client.ClientBuilder;
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
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
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
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
        );

        var secondConnectionEvents = List.of(
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
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
