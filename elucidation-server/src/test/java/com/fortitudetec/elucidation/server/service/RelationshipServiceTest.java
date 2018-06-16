package com.fortitudetec.elucidation.server.service;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
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

import static com.fortitudetec.elucidation.server.core.CommunicationType.JMS;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.Connection;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class RelationshipServiceTest {

    private ConnectionEventDao dao;
    private RelationshipService service;

    @BeforeEach
    void setUp() {
        dao = mock(ConnectionEventDao.class);
        service = new RelationshipService(dao);
    }

    @Test
    @DisplayName("should pass a ConnectionEvent onto the dao to be created")
    void testCreateEvent() {
        ConnectionEvent event = buildEvent(null, "test-service", Direction.OUTBOUND, "some-identifier");

        when(dao.insertConnection(event)).thenReturn(1L);

        service.createEvent(event);

        verify(dao).insertConnection(event);
    }

    @Test
    @DisplayName("should return a list of ConnectionEvents that have been recorded for a given service")
    void testListEventsForService() {
        when(dao.findEventsByServiceName("test-service")).thenReturn(newArrayList(
            buildEvent("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
            buildEvent("test-service", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
            buildEvent("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
        ));

        List<ConnectionEvent> events = service.listEventsForService("test-service");

        assertThat(events).hasSize(3)
            .extracting("serviceName", "eventDirection", "connectionIdentifier")
            .contains(
                tuple("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
                tuple("test-service", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
                tuple("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
            );
    }

    @Test
    @DisplayName("should return a ServiceConnection without any events")
    void testBuildRelationships_NoEvents() {
        when(dao.findEventsByServiceName("test-service")).thenReturn(newArrayList());

        ServiceConnections serviceConnections = service.buildRelationships("test-service");

        assertThat(serviceConnections.getServiceName()).isEqualTo("test-service");
        assertThat(serviceConnections.getInboundConnections()).isEmpty();
        assertThat(serviceConnections.getOutboundConnections()).isEmpty();
    }

    @Test
    @DisplayName("should return a Service Connection with the proper events")
    void testBuildRelationships_WithEvents() {
        when(dao.findEventsByServiceName("test-service")).thenReturn(
            newArrayList(
                buildEvent("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
                buildEvent("test-serivce", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
                buildEvent("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
        ));

        when(dao.findAssociatedEvents(Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("another-service-1", Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("another-service-2", Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_NO_ONE_LISTENS_TO", JMS)).thenReturn(newArrayList());

        ServiceConnections serviceConnections = service.buildRelationships("test-service");

        assertThat(serviceConnections.getServiceName()).isEqualTo("test-service");
        assertThat(serviceConnections.getInboundConnections()).hasSize(1)
            .extracting("serviceName", "identifier")
            .contains(tuple("another-service-1", "MSG_FROM_ANOTHER_SERVICE"));
        assertThat(serviceConnections.getOutboundConnections()).hasSize(2)
            .extracting("serviceName", "identifier")
            .contains(
                tuple("another-service-2", "MSG_TO_ANOTHER_SERVICE"),
                tuple("unknown-service", "MSG_NO_ONE_LISTENS_TO"));
    }

    @Test
    @DisplayName("should return ALL services and their relationships")
    void testBuildAllRelationships() {
        when(dao.findAllServiceNames()).thenReturn(newArrayList("test-service", "another-service-1", "another-service-2"));
        when(dao.findEventsByServiceName("test-service")).thenReturn(
            newArrayList(
                buildEvent("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
                buildEvent("test-serivce", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
                buildEvent("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
            )
        );
        when(dao.findEventsByServiceName("another-service-1")).thenReturn(
            newArrayList(
                buildEvent("another-service-1", Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE")
            )
        );
        when(dao.findEventsByServiceName("another-service-2")).thenReturn(
            newArrayList(
                buildEvent("another-service-2", Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE")
            )
        );

        when(dao.findAssociatedEvents(Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("another-service-1", Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE"))
        );
        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("another-service-2", Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE"))
        );
        when(dao.findAssociatedEvents(Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE", JMS)).thenReturn(
            newArrayList(buildEvent("test-service", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_NO_ONE_LISTENS_TO", JMS)).thenReturn(newArrayList());

        List<ServiceConnections> serviceConnectionsList = service.buildAllRelationships();

        assertThat(serviceConnectionsList).hasSize(3)
            .extracting("serviceName", "inboundConnections", "outboundConnections")
            .contains(
                tuple("test-service",
                    newHashSet(buildConnection("another-service-1", "MSG_FROM_ANOTHER_SERVICE", JMS)),
                    newHashSet(
                        buildConnection("another-service-2", "MSG_TO_ANOTHER_SERVICE", JMS),
                        buildConnection("unknown-service", "MSG_NO_ONE_LISTENS_TO", JMS)
                    )
                ),
                tuple("another-service-1",
                    newHashSet(),
                    newHashSet(
                        buildConnection("test-service", "MSG_FROM_ANOTHER_SERVICE", JMS)
                    )
                ),
                tuple("another-service-2",
                    newHashSet(buildConnection("test-service", "MSG_TO_ANOTHER_SERVICE", JMS)),
                    newHashSet()
                )
            );
    }

    private static Connection buildConnection(String serviceName, String identifier, CommunicationType type) {
        return Connection.builder()
            .serviceName(serviceName)
            .identifier(identifier)
            .protocol(type)
            .build();
    }

    private static ConnectionEvent buildEvent(String serviceName, Direction direction, String identifier) {
        return buildEvent(ThreadLocalRandom.current().nextLong(), serviceName, direction, identifier);
    }

    private static ConnectionEvent buildEvent(Long id, String serviceName, Direction direction, String identifier) {
        return ConnectionEvent.builder()
            .serviceName(serviceName)
            .communicationType(JMS)
            .eventDirection(direction)
            .connectionIdentifier(identifier)
            .observedAt(System.currentTimeMillis())
            .id(id)
            .build();
    }
}
