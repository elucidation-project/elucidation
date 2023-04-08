package org.kiwiproject.elucidation.server.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ConnectionSummary {

    private String serviceName;
    private boolean hasInbound;
    private boolean hasOutbound;

}
