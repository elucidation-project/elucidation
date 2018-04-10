package com.fortitudetec.elucidation.server.service;

import static guru.nidi.graphviz.model.Factory.graph;

import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.ServiceWithConnections;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RelationshipService {

    private ConnectionEventDao dao;

    public RelationshipService(ConnectionEventDao dao) {
        this.dao = dao;
    }

    public byte[] generateRelationshipGraphForService(String serviceName) throws IOException {

        Graph g = graph(serviceName).directed();




        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Graphviz.fromGraph(g).width(200).render(Format.PNG).toOutputStream(byteStream);

        return byteStream.toByteArray();
    }

//    private Map<String, Node> generateNodes(Graph g, List<S>)

    public Map<String, Object> generateRelationshipDataForService(String serviceName) {

        throw new NotImplementedException();
    }

    private ServiceWithConnections flattenEvents(List<ConnectionEvent> events) {
        events.stream().filter(event -> event.getConnectionType() == )
    }
}
