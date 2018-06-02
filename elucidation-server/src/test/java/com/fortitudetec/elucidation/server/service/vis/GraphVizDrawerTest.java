package com.fortitudetec.elucidation.server.service.vis;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
