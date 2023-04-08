package org.kiwiproject.elucidation.common.test;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ConnectionEvents {

    public static ConnectionEvent newConnectionEvent(String serviceName, Direction direction, String identifier) {
        return newConnectionEvent(ThreadLocalRandom.current().nextLong(), serviceName, direction, identifier);
    }

    public static ConnectionEvent newConnectionEvent(String serviceName, Direction direction, String identifier, long observedAt) {
        return newConnectionEvent(ThreadLocalRandom.current().nextLong(), serviceName, direction, identifier, observedAt);
    }

    public static ConnectionEvent newConnectionEvent(Long id, String serviceName, Direction direction, String identifier) {
        return newConnectionEvent(id, serviceName, direction, identifier, System.currentTimeMillis());
    }

    public static ConnectionEvent newConnectionEvent(Long id, String serviceName, Direction direction, String identifier, long observedAt) {
        return ConnectionEvent.builder()
                .serviceName(serviceName)
                .communicationType("JMS")
                .eventDirection(direction)
                .connectionIdentifier(identifier)
                .observedAt(observedAt)
                .id(id)
                .build();
    }

}
