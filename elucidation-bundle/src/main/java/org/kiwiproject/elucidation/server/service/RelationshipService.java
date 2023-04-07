package org.kiwiproject.elucidation.server.service;

/*-
 * #%L
 * Elucidation Server
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;

import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.common.model.RelationshipDetails;
import org.kiwiproject.elucidation.server.core.ConnectionSummary;
import org.kiwiproject.elucidation.server.core.DependencyRelationshipDetails;
import org.kiwiproject.elucidation.server.core.ServiceConnections;
import org.kiwiproject.elucidation.server.core.ServiceDependencies;
import org.kiwiproject.elucidation.server.core.ServiceDependencyDetails;
import org.kiwiproject.elucidation.server.core.ServiceDetails;
import org.kiwiproject.elucidation.server.db.ConnectionEventDao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RelationshipService {

    private final ConnectionEventDao dao;
    private final Map<String, CommunicationDefinition> communicationDefinitions;

    public RelationshipService(ConnectionEventDao dao, Map<String, CommunicationDefinition> communicationDefinitions) {
        this.dao = dao;
        this.communicationDefinitions = communicationDefinitions;
    }

    public void createEvent(ConnectionEvent event) {
        dao.createOrUpdate(event);
    }

    public List<ConnectionEvent> listEventsSince(long sinceInMillis) {
        return dao.findEventsSince(sinceInMillis);
    }

    public List<ConnectionEvent> listEventsForService(String serviceName) {
        return dao.findEventsByServiceName(serviceName);
    }

    public List<String> currentServiceNames() {
        return dao.findAllServiceNames();
    }

    public List<ServiceDetails> currentServiceDetails() {
        var serviceNames = currentServiceNames();

        return serviceNames.stream()
                .map(this::buildDetails)
                .collect(toList());
    }

    private ServiceDetails buildDetails(String serviceName) {
        var events = dao.findEventsByServiceName(serviceName);
        var eventsByDirectionCounts = events.stream().collect(groupingBy(ConnectionEvent::getEventDirection));
        var eventsByCommunicationType = events.stream().collect(groupingBy(ConnectionEvent::getCommunicationType));

        var eventsByCommunicationTypeCounts = eventsByCommunicationType.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry-> entry.getValue().size()
                ));

        return ServiceDetails.builder()
                .serviceName(serviceName)
                .inboundEvents(eventsByDirectionCounts.getOrDefault(Direction.INBOUND, newArrayList()).size())
                .outboundEvents(eventsByDirectionCounts.getOrDefault(Direction.OUTBOUND, newArrayList()).size())
                .communicationTypes(eventsByCommunicationTypeCounts)
                .build();
    }

    public List<ServiceDependencies> buildAllDependencies() {
        return dao.findAllServiceNames().stream()
                .distinct()
                .map(this::findDependencies)
                .collect(toList());
    }

    public List<ServiceDependencyDetails> buildAllDependenciesWithDetails() {
        var servicesWithDependencies = buildAllDependencies();

        return servicesWithDependencies.stream()
                .map(this::expandDetails)
                .collect(toList());
    }

    private ServiceDependencyDetails expandDetails(ServiceDependencies service) {
        var serviceName = service.getServiceName();
        var depsWithDetails = service.getDependencies().stream()
                .map(dep -> detailsForDependency(serviceName, dep))
                .collect(toList());

        return ServiceDependencyDetails.builder()
                .serviceName(serviceName)
                .dependencies(depsWithDetails)
                .build();
    }

    private DependencyRelationshipDetails detailsForDependency(String fromService, String toService) {
        return DependencyRelationshipDetails.builder()
                .serviceName(toService)
                .details(findRelationshipDetails(fromService, toService))
                .build();
    }

    private ServiceDependencies findDependencies(String serviceName) {
        List<ConnectionEvent> dependentEvents = dao.findEventsByServiceName(serviceName).stream()
                .filter(this::isDependentEvent)
                .collect(toList());

        return ServiceDependencies.builder()
                .serviceName(serviceName)
                .dependencies(populateOppositeConnections(dependentEvents))
                .build();
    }

    private boolean isDependentEvent(ConnectionEvent event) {
        var communicationType = event.getCommunicationType();
        var communicationDefinition = communicationDefinitions.get(communicationType);

        return communicationDefinition.isDependentEvent(event);
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

        Map<String, ConnectionSummary> outboundSummaries = outboundConnections.stream()
                .map(connectedServiceName -> ConnectionSummary.builder()
                        .serviceName(connectedServiceName)
                        .hasOutbound(true)
                        .build())
                .collect(toMap(ConnectionSummary::getServiceName, Function.identity()));

        Map<String, ConnectionSummary> summaries = new HashMap<>(inboundSummaries);
        outboundSummaries.forEach(
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
                        .lastObserved(event.getObservedAt())
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
            return List.of(syntheticEvent);
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

    public List<ConnectionEvent> findAllEventsByConnectionIdentifier(String connectionIdentifier) {
        return dao.findEventsByConnectionIdentifier(connectionIdentifier);
    }

}
