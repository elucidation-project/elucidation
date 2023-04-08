package org.kiwiproject.elucidation.common.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * A representation of an observed connection within a given service.
 */
@Builder
@Value
public class ConnectionEvent {

    public static final String UNKNOWN_SERVICE = "unknown-service";

    @With
    Long id;

    /**
     * A name of the service where the connection was observed
     */
    @NotBlank
    String serviceName;

    /**
     * The direction that the event was observed. INBOUND or OUTBOUND
     */
    @NotNull
    Direction eventDirection;

    /**
     * The method of communication that was observed. For example, "HTTP" or "JMS".
     *
     * @see CommunicationDefinition
     */
    @NotBlank
    String communicationType;

    /**
     * A unique identifier for the connection (i.e. REST endpoint path or JMS Message Type)
     */
    @NotBlank
    String connectionIdentifier;

    /**
     * The date/time the connection was observed (in milliseconds since EPOCH)
     */
    @Builder.Default
    long observedAt = System.currentTimeMillis();

}
