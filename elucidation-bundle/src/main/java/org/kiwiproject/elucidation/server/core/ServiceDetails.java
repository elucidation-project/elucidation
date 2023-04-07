package org.kiwiproject.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class ServiceDetails {

    private String serviceName;
    private int inboundEvents;
    private int outboundEvents;
    private Map<String, Integer> communicationTypes;

}
