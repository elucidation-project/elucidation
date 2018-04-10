package com.fortitudetec.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ServiceWithConnections {

    private String name;
    private List<Connection> outboundConnections;
    private List<Connection> inboundConnections;

}
