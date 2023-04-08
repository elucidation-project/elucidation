package org.kiwiproject.elucidation.common.definition;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;

/**
 * For asynchronous communications, an event is dependent if we depend on another service/system/etc. to produce
 * the event for our consumption.
 */
public interface AsyncMessagingCommunicationDefinition extends CommunicationDefinition {

    @Override
    default boolean isDependentEvent(ConnectionEvent event) {
        return event.getEventDirection() == Direction.INBOUND;
    }
}
