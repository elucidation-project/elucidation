package com.fortitudetec.elucidation.server.core;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class RelationshipConnection {

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
}
