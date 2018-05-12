package com.fortitudetec.elucidation.server.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

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
        ConnectionEvent event = buildEvent("test-service", Direction.OUTBOUND, "some-identifier");
        event.setId(null);

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

        when(dao.findAssociatedEvents(Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE", CommunicationType.JMS)).thenReturn(
            newArrayList(buildEvent("another-service-1", Direction.OUTBOUND, "MSG_FROM_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE", CommunicationType.JMS)).thenReturn(
            newArrayList(buildEvent("another-service-2", Direction.INBOUND, "MSG_TO_ANOTHER_SERVICE"))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, "MSG_NO_ONE_LISTENS_TO", CommunicationType.JMS)).thenReturn(newArrayList());

        ServiceConnections serviceConnections = service.buildRelationships("test-service");

        assertThat(serviceConnections.getServiceName()).isEqualTo("test-service");
        assertThat(serviceConnections.getInboundConnections()).hasSize(1)
            .extracting("serviceName", "identifier")
            .contains(tuple("another-service-1", "MSG_FROM_ANOTHER_SERVICE"));
        assertThat(serviceConnections.getOutboundConnections()).hasSize(1)
            .extracting("serviceName", "identifier")
            .contains(tuple("another-service-2", "MSG_TO_ANOTHER_SERVICE"));
    }

    private ConnectionEvent buildEvent(String serviceName, Direction direction, String identifier) {
        return ConnectionEvent.builder()
            .serviceName(serviceName)
            .communicationType(CommunicationType.JMS)
            .eventDirection(direction)
            .connectionIdentifier(identifier)
            .observedAt(ZonedDateTime.now())
            .id(new Random().nextLong())
            .build();
    }
}
