package org.kiwiproject.elucidation.server.service;

import static java.util.stream.Collectors.toSet;

import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.kiwiproject.elucidation.server.core.UnusedIdentifier;
import org.kiwiproject.elucidation.server.core.UnusedServiceIdentifiers;
import org.kiwiproject.elucidation.server.db.ConnectionEventDao;
import org.kiwiproject.elucidation.server.db.TrackedConnectionIdentifierDao;

import java.util.List;
import java.util.stream.Stream;

public class TrackedConnectionIdentifierService {

    private final TrackedConnectionIdentifierDao trackedConnectionIdentifierDao;
    private final ConnectionEventDao connectionEventDao;

    public TrackedConnectionIdentifierService(TrackedConnectionIdentifierDao trackedConnectionIdentifierDao, ConnectionEventDao connectionEventDao) {
        this.trackedConnectionIdentifierDao = trackedConnectionIdentifierDao;
        this.connectionEventDao = connectionEventDao;
    }

    /**
     * Loads new tracked identifiers for use in determining unused identifiers that can't be determined by the data
     *
     * @implNote If any tracked identifiers for the given service and communication type exist, they will be removed prior
     * to the load.
     */
    public int loadNewIdentifiers(String serviceName, String communicationType, List<String> connectionIdentifiers) {
        var trackedConnectionIdentifiers = connectionIdentifiers.stream()
                .map(identifier -> TrackedConnectionIdentifier.builder()
                        .serviceName(serviceName)
                        .communicationType(communicationType)
                        .connectionIdentifier(identifier)
                        .build())
                .toList();

        trackedConnectionIdentifierDao.clearIdentifiersFor(serviceName, communicationType);

        trackedConnectionIdentifiers.forEach(trackedConnectionIdentifierDao::insertIdentifier);
        return trackedConnectionIdentifiers.size();
    }

    public List<UnusedServiceIdentifiers> findUnusedIdentifiers() {
        var serviceNamesFromEvents = connectionEventDao.findAllServiceNames();
        var serviceNamesFromTracked = trackedConnectionIdentifierDao.findAllServiceNames();

        var serviceNames = Stream.concat(serviceNamesFromEvents.stream(), serviceNamesFromTracked.stream())
                .collect(toSet());

        return serviceNames.stream()
                .map(this::createUnusedServiceIdentifierFor)
                .filter(unused -> !unused.getIdentifiers().isEmpty())
                .toList();
    }

    private UnusedServiceIdentifiers createUnusedServiceIdentifierFor(String serviceName) {
        var unusedFromEvents = findUnusedFromEvents(serviceName);
        var unusedFromTracked = findUnusedFromTracked(serviceName);

        return UnusedServiceIdentifiers.builder()
                .serviceName(serviceName)
                .identifiers(Stream.concat(unusedFromEvents.stream(), unusedFromTracked.stream()).toList())
                .build();
    }

    private List<UnusedIdentifier> findUnusedFromEvents(String serviceName) {
        return connectionEventDao.findEventsByServiceName(serviceName)
                .stream()
                .filter(event -> event.getEventDirection() == Direction.OUTBOUND)
                .filter(event -> connectionEventDao.findAssociatedEvents(Direction.INBOUND, event.getConnectionIdentifier(), event.getCommunicationType()).isEmpty())
                .map(event -> UnusedIdentifier.builder()
                        .communicationType(event.getCommunicationType())
                        .connectionIdentifier(event.getConnectionIdentifier())
                        .build())
                .toList();
    }

    private List<UnusedIdentifier> findUnusedFromTracked(String serviceName) {
        return trackedConnectionIdentifierDao.findByServiceName(serviceName)
                .stream()
                .filter(tracked -> connectionEventDao.findAssociatedEvents(Direction.INBOUND, tracked.getConnectionIdentifier(), tracked.getCommunicationType()).isEmpty())
                .map(tracked -> UnusedIdentifier.builder()
                        .communicationType(tracked.getCommunicationType())
                        .connectionIdentifier(tracked.getConnectionIdentifier())
                        .build())
                .toList();
    }

    public List<TrackedConnectionIdentifier> allTrackedConnectionIdentifiers() {
        return trackedConnectionIdentifierDao.findIdentifiers();
    }

    public UnusedServiceIdentifiers findUnusedIdentifiersForService(String serviceName) {
        return createUnusedServiceIdentifierFor(serviceName);
    }
}
