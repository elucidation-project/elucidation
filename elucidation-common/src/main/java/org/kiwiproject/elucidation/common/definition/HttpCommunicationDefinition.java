package org.kiwiproject.elucidation.common.definition;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;

/**
 * For HTTP communications, an event is dependent if we depend on another service/system/etc. to exist
 * so that we can make HTTP requests to it.
 */
public class HttpCommunicationDefinition implements CommunicationDefinition {

    @Override
    public String getCommunicationType() {
        return "HTTP";
    }

    @Override
    public boolean isDependentEvent(ConnectionEvent event) {
        return event.getEventDirection() == Direction.OUTBOUND;
    }
}
