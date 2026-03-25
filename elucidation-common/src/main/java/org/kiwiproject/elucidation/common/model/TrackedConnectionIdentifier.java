package org.kiwiproject.elucidation.common.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;

/**
 * A representation of a connection identifier that exists in the system.
 * The purpose of this model is to be able to track if a connection identifier is never used.
 * <p>
 * <p>
 * Note that it makes more sense to use this for communication types like HTTP.
 * This is because for JMS types we would most likely receive the Produce and the Consume events.
 * So, it makes it very easy to detect anything that is being produced but never consumed. However,
 * for HTTP, we would never know if an endpoint exists but is never called, because calling it
 * would trigger the event.
 */
@Builder
@Value
public class TrackedConnectionIdentifier {

    @With
    Long id;

    /**
     * A name of the service where the connection identifier exists
     */
    @NotBlank
    String serviceName;

    /**
     * The method of communication for this identifier. For example, "HTTP" or "JMS".
     *
     * @see CommunicationDefinition
     */
    @NotBlank
    String communicationType;

    /**
     * A unique identifier for the connection (i.e., REST endpoint path or JMS Message Type)
     */
    @NotBlank
    String connectionIdentifier;
}
