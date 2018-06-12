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

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.Connection;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.fortitudetec.elucidation.server.core.CommunicationType.JMS;
import static com.fortitudetec.elucidation.server.core.CommunicationType.REST;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.union;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Link.to;

@UtilityClass
@Slf4j
public class GraphVizDrawer {

    public static byte[] buildGraphFrom(ServiceConnections serviceConnections) {
        String rootService = serviceConnections.getServiceName();

        Graph graph = graph(rootService).directed();

        Map<String, Node> nodes = newHashMap();
        nodes.put(rootService, node(rootService));

        Set<Connection> inboundConnections = serviceConnections.getInboundConnections();
        Set<Connection> outboundConnections = serviceConnections.getOutboundConnections();

        generateNodes(nodes, union(inboundConnections, outboundConnections));

        linkInboundConnections(nodes, rootService, inboundConnections, REST);
        linkInboundConnections(nodes, rootService, inboundConnections, JMS);
        linkOutboundConnections(nodes, rootService, outboundConnections, REST);
        linkOutboundConnections(nodes, rootService, outboundConnections, JMS);

        graph = graph.with(nodes.values().toArray(new Node[0]));

        return readBytes(graph);
    }

    private static void generateNodes(Map<String, Node> nodes, Set<Connection> connections) {
        connections.forEach(connection ->
            nodes.put(connection.getServiceName(), node(connection.getServiceName())));
    }

    private static void linkInboundConnections(Map<String, Node> nodes, String rootNodeName,
                                               Set<Connection> connections, CommunicationType commType) {

        Node mainNode = nodes.get(rootNodeName);

        connections.stream()
            .filter(connection -> connection.getProtocol() == commType)
            .forEach(connection -> nodes.put(connection.getServiceName(),
                nodes.get(connection.getServiceName()).link(to(mainNode).with(styleFor(commType), Label.of(connection.getIdentifier())))));
    }

    private static void linkOutboundConnections(Map<String, Node> nodes, String rootNodeName,
                                                Set<Connection> connections, CommunicationType commType) {

        connections.stream()
            .filter(connection -> connection.getProtocol() == commType)
            .forEach(connection -> nodes.put(rootNodeName,
                nodes.get(rootNodeName)
                    .link(to(nodes.get(connection.getServiceName())).with(styleFor(commType), Label.of(connection.getIdentifier())))));
    }

    private Style styleFor(CommunicationType commType) {
        if (commType == CommunicationType.JMS) {
            return Style.DASHED;
        } else {
            return Style.SOLID;
        }
    }

    private static byte[] readBytes(Graph graph) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Graphviz.fromGraph(graph).render(Format.PNG).toOutputStream(baos);
            baos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            LOG.warn("There was a problem reading the bytes for the graph", e);
        }

        return new byte[0];
    }
}
