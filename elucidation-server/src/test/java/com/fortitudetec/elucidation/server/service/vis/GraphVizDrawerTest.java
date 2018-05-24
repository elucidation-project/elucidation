package com.fortitudetec.elucidation.server.service.vis;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.Connection;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import org.junit.jupiter.api.Test;

class GraphVizDrawerTest {

    @Test
    void testBuildGraph() {
        ServiceConnections connections = ServiceConnections.builder()
            .serviceName("very-cool-service")
            .inboundConnections(newHashSet(
                Connection.builder().serviceName("im-talking-to-you").protocol(CommunicationType.REST).identifier("/some_call").build(),
                Connection.builder().serviceName("im-messaging-you").protocol(CommunicationType.JMS).identifier("HELLO_MSG").build()
            ))
            .outboundConnections(newHashSet(
                Connection.builder().serviceName("who-are-you").protocol(CommunicationType.REST).identifier("/do_it").build(),
                Connection.builder().serviceName("message-received").protocol(CommunicationType.JMS).identifier("WHATS_UP_MSG").build()
            ))
            .build();

        byte[] bytes = GraphVizDrawer.buildGraphFrom(connections);

        assertThat(bytes).hasSize(21508);
    }

}
