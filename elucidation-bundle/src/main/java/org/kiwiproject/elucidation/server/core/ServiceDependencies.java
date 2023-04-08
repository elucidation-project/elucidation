package org.kiwiproject.elucidation.server.core;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Builder
@Value
public class ServiceDependencies {

    String serviceName;
    Set<String> dependencies;

}
