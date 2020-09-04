package com.fortitudetec.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ServiceDependencyDetails {
    private String serviceName;
    private List<DependencyRelationshipDetails> dependencies;
}
