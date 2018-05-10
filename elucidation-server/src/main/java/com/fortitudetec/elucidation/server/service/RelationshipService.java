package com.fortitudetec.elucidation.server.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import com.fortitudetec.elucidation.server.api.Connection;
import com.fortitudetec.elucidation.server.api.ServiceConnections;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RelationshipService {

    private ConnectionEventDao dao;

    public RelationshipService(ConnectionEventDao dao) {
        this.dao = dao;
    }

    public ServiceConnections buildRelationships(String serviceName) {
        List<ConnectionEvent> events = dao.findEventsByServiceName(serviceName);

        Map<Direction, List<ConnectionEvent>> eventsByDirection = events.stream()
            .collect(groupingBy(ConnectionEvent::getEventDirection));

        return ServiceConnections.builder()
            .serviceName(serviceName)
            .inboundConnections(populateInboundConnections(eventsByDirection.get(Direction.INBOUND)))
            .outboundConnections(populateOutboundConnections(eventsByDirection.get(Direction.OUTBOUND)))
            .build();

    }

    private Set<Connection> populateInboundConnections(List<ConnectionEvent> events) {
        if (isNull(events)) {
            return newHashSet();
        }

        return events.stream()
            .map(event -> dao.findAssociatedEvents(Direction.OUTBOUND,
                event.getConnectionIdentifier(), event.getCommunicationType()))
            .flatMap(List::stream)
            .map(Connection::fromEvent)
            .collect(toSet());
    }

    private Set<Connection> populateOutboundConnections(List<ConnectionEvent> events) {
        if (isNull(events)) {
            return newHashSet();
        }

        return events.stream()
            .map(event -> dao.findAssociatedEvents(Direction.INBOUND,
                event.getConnectionIdentifier(), event.getCommunicationType()))
            .flatMap(List::stream)
            .map(Connection::fromEvent)
            .collect(toSet());
    }

}
