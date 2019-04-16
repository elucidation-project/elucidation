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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.ConnectionSummary;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.core.RelationshipDetails;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import com.fortitudetec.elucidation.server.core.ServiceDependencies;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.google.common.collect.ImmutableList;

import org.apache.commons.collections4.map.HashedMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RelationshipService {

    private ConnectionEventDao dao;

    public RelationshipService(ConnectionEventDao dao) {
        this.dao = dao;
    }

    public void createEvent(ConnectionEvent event) {
        dao.createOrUpdate(event);
    }

    public List<ConnectionEvent> listEventsForService(String serviceName) {
        return dao.findEventsByServiceName(serviceName);
    }

    public List<ServiceDependencies> buildAllDependencies() {
        return dao.findAllServiceNames().stream()
                .distinct()
                .map(this::findDependencies)
                .collect(toList());
    }

    private ServiceDependencies findDependencies(String serviceName) {
        List<ConnectionEvent> dependentEvents = dao.findEventsByServiceName(serviceName).stream()
                .filter(RelationshipService::isDependentEvent)
                .collect(toList());

        return ServiceDependencies.builder()
                .serviceName(serviceName)
                .dependencies(populateOppositeConnections(dependentEvents))
                .build();
    }

    private static boolean isDependentEvent(ConnectionEvent event) {
        return (event.getCommunicationType() == CommunicationType.JMS && event.getEventDirection() == Direction.INBOUND) ||
                (event.getCommunicationType() == CommunicationType.REST && event.getEventDirection() == Direction.OUTBOUND);
    }

    public ServiceConnections buildRelationships(String serviceName) {
        List<ConnectionEvent> events = dao.findEventsByServiceName(serviceName);

        Map<Direction, List<ConnectionEvent>> eventsByDirection = events.stream()
                .collect(groupingBy(ConnectionEvent::getEventDirection));

        Set<String> inboundConnections = populateOppositeConnections(eventsByDirection.get(Direction.INBOUND));
        Set<String> outboundConnections = populateOppositeConnections(eventsByDirection.get(Direction.OUTBOUND));

        Map<String, ConnectionSummary> inboundSummaries = inboundConnections.stream()
                .map(connectedServiceName -> ConnectionSummary.builder()
                        .serviceName(connectedServiceName)
                        .hasInbound(true)
                        .build())
                .collect(toMap(ConnectionSummary::getServiceName, Function.identity()));

        Map<String, ConnectionSummary> ouboundSummaries = outboundConnections.stream()
                .map(connectedServiceName -> ConnectionSummary.builder()
                        .serviceName(connectedServiceName)
                        .hasOutbound(true)
                        .build())
                .collect(toMap(ConnectionSummary::getServiceName, Function.identity()));

        Map<String, ConnectionSummary> summaries = new HashedMap<>(inboundSummaries);
        ouboundSummaries.forEach(
                (key, value) -> summaries.merge(key, value,
                        (v1, v2) -> ConnectionSummary.builder().serviceName(v1.getServiceName()).hasOutbound(true).hasInbound(true).build()));

        return ServiceConnections.builder()
                .serviceName(serviceName)
                .children(newHashSet(summaries.values()))
                .build();
    }

    public List<RelationshipDetails> findRelationshipDetails(String fromService, String toService) {
        List<ConnectionEvent> events = dao.findEventsByServiceName(fromService);

        return events.stream()
                .map(this::findAssociatedEventsOrUnknown)
                .flatMap(List::stream)
                .filter(event -> event.getServiceName().equalsIgnoreCase(toService))
                .map(event -> RelationshipDetails.builder()
                        .communicationType(event.getCommunicationType())
                        .connectionIdentifier(event.getConnectionIdentifier())
                        .eventDirection(event.getEventDirection().opposite())
                        .build())
                .collect(toList());
    }

    private Set<String> populateOppositeConnections(List<ConnectionEvent> events) {
        if (isNull(events)) {
            return newHashSet();
        }

        return events.stream()
                .map(this::findAssociatedEventsOrUnknown)
                .flatMap(List::stream)
                .map(ConnectionEvent::getServiceName)
                .collect(toSet());
    }

    private List<ConnectionEvent> findAssociatedEventsOrUnknown(ConnectionEvent event) {
        List<ConnectionEvent> associatedEvents = dao.findAssociatedEvents(
                event.getEventDirection().opposite(),
                event.getConnectionIdentifier(),
                event.getCommunicationType());

        return associatedEventsOrUnknown(event, associatedEvents);
    }

    private static List<ConnectionEvent> associatedEventsOrUnknown(ConnectionEvent event,
                                                                   List<ConnectionEvent> associatedEvents) {
        if (associatedEvents.isEmpty()) {
            ConnectionEvent syntheticEvent = syntheticConnectionEvent(event);
            return ImmutableList.of(syntheticEvent);
        }

        return associatedEvents;
    }

    private static ConnectionEvent syntheticConnectionEvent(ConnectionEvent event) {
        return ConnectionEvent.builder()
                .serviceName(ConnectionEvent.UNKNOWN_SERVICE)
                .eventDirection(event.getEventDirection().opposite())
                .communicationType(event.getCommunicationType())
                .connectionIdentifier(event.getConnectionIdentifier())
                .observedAt(event.getObservedAt())
                .build();
    }
}
