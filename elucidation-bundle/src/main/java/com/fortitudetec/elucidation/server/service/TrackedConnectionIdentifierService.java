package com.fortitudetec.elucidation.server.service;

import static java.util.stream.Collectors.toList;

import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.fortitudetec.elucidation.server.db.TrackedConnectionIdentifierDao;

import java.util.List;

public class TrackedConnectionIdentifierService {

    private final TrackedConnectionIdentifierDao dao;

    public TrackedConnectionIdentifierService(TrackedConnectionIdentifierDao dao) {
        this.dao = dao;
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
                .collect(toList());

        dao.clearIdentifiersFor(serviceName, communicationType);

        trackedConnectionIdentifiers.forEach(dao::insertIdentifier);
        return trackedConnectionIdentifiers.size();
    }
}
