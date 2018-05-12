package com.fortitudetec.elucidation.server.api;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class Connection {

    private CommunicationType protocol;
    private String identifier;
    private String serviceName;

    public static Connection fromEvent(ConnectionEvent event) {
        return Connection.builder()
            .protocol(event.getCommunicationType())
            .identifier(event.getConnectionIdentifier())
            .serviceName(event.getServiceName())
            .build();
    }
}
