package com.fortitudetec.elucidation.client.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

/**
 * A representation of an observed connection within a given service.
 */
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
     * The direction that the event was observed. INBOUND or OUTBOUND
     */
    private Direction eventDirection;

    /**
     * The method of communication that was observed. REST or JMS
     */
    private CommunicationType communicationType;

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
