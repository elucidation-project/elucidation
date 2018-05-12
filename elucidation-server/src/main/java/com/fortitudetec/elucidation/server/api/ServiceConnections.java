package com.fortitudetec.elucidation.server.api;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class ServiceConnections {

    private String serviceName;
    private Set<Connection> outboundConnections;
    private Set<Connection> inboundConnections;
}
