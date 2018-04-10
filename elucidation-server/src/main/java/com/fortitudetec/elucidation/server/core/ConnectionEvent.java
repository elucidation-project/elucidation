package com.fortitudetec.elucidation.server.core;

import static com.fortitudetec.elucidation.server.core.ConnectionType.CONSUMED_JMS;
import static com.fortitudetec.elucidation.server.core.ConnectionType.INBOUND_REST;
import static com.fortitudetec.elucidation.server.core.ConnectionType.OUTBOUND_REST;
import static com.fortitudetec.elucidation.server.core.ConnectionType.PRODUCED_JMS;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
public class ConnectionEvent {

    private Long id;

    /**
     * A name of the service where the connection was observed
     */
    private String serviceName;

    /**
     * The type and direction of the connection that was observed
     */
    private ConnectionType connectionType;

    /**
     * A unique identifier for the connection (i.e. REST endpoint path or JMS Message Type)
     */
    private String connectionIdentifier;

    /**
     * For REST connections, the method used (i.e. GET, POST, PUT, DELETE, etc)
     */
    private String restMethod;

    /**
     * The date the connection was observed
     */
    private ZonedDateTime observedAt;

    /**
     * The name of the service that originated the connection (can be optionally used for inbound and consuming connections)
     */
    private String originatingServiceName;

    public boolean isInbound() {
        return INBOUND_REST == connectionType || CONSUMED_JMS == connectionType;
    }

    public boolean isOutbound() {
        return OUTBOUND_REST == connectionType || PRODUCED_JMS == connectionType;
    }

}
