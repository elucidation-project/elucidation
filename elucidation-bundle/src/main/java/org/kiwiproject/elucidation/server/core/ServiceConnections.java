package org.kiwiproject.elucidation.server.core;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class ServiceConnections {

    String serviceName;
    Set<ConnectionSummary> children;

}
