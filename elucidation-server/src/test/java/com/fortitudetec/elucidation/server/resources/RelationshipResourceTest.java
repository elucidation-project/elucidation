package com.fortitudetec.elucidation.server.resources;

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

import static com.fortitudetec.elucidation.server.core.CommunicationType.JMS;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.server.core.CommunicationType;
import com.fortitudetec.elucidation.server.core.ConnectionEvent;
import com.fortitudetec.elucidation.server.core.ConnectionSummary;
import com.fortitudetec.elucidation.server.core.Direction;
import com.fortitudetec.elucidation.server.core.RelationshipDetails;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import com.fortitudetec.elucidation.server.core.ServiceDependencies;
import com.fortitudetec.elucidation.server.service.RelationshipService;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

@ExtendWith(DropwizardExtensionsSupport.class)
class RelationshipResourceTest {

    private RelationshipService service = mock(RelationshipService.class);

    private final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new RelationshipResource(service))
            .build();

    @Test
    @DisplayName("given a valid event should attempt to save the event")
    void testRecordEvent() {
        ConnectionEvent event = buildEvent("test-service", Direction.OUTBOUND, "some-identifier");

        Response response = resources.target("/").request().post(Entity.json(event));

        assertThat(response.getStatus()).isEqualTo(202);
    }

    @Test
    @DisplayName("should return a list of ConnectionEvents for a given service")
    void testViewEventsForService() {
        when(service.listEventsForService("test-service")).thenReturn(newArrayList(
                buildEvent("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
                buildEvent("test-service", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
                buildEvent("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
        ));

        Response response = resources.target("/test-service").request().get();

        assertThat(response.getStatus()).isEqualTo(200);

        List<ConnectionEvent> events = response.readEntity(new GenericType<List<ConnectionEvent>>() {
        });

        assertThat(events).hasSize(3)
                .extracting("serviceName", "eventDirection", "connectionIdentifier")
                .contains(
                        tuple("test-service", Direction.INBOUND, "MSG_FROM_ANOTHER_SERVICE"),
                        tuple("test-service", Direction.OUTBOUND, "MSG_TO_ANOTHER_SERVICE"),
                        tuple("test-service", Direction.OUTBOUND, "MSG_NO_ONE_LISTENS_TO")
                );
    }

    @Test
    @DisplayName("should trigger the build of the relationship data for a given service")
    void testCalculateRelationships() {
        ServiceConnections connections = ServiceConnections.builder()
                .serviceName("test-service")
                .children(newHashSet(ConnectionSummary.builder().serviceName("other-service").hasInbound(true).hasOutbound(true).build()))
                .build();

        when(service.buildRelationships("test-service")).thenReturn(connections);

        Response response = resources.target("/test-service/relationships").request().get();

        assertThat(response.getStatus()).isEqualTo(200);

        ServiceConnections connectionsResponse = response.readEntity(ServiceConnections.class);

        assertThat(connectionsResponse.getServiceName()).isEqualTo("test-service");
        assertThat(connectionsResponse.getChildren()).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should return the details of the relationships between two given services")
    void testRelationshipDetails() {
        List<RelationshipDetails> details = newArrayList(RelationshipDetails.builder().communicationType(CommunicationType.JMS).connectionIdentifier("ACTIVITY_TEST").eventDirection(Direction.OUTBOUND).build());

        when(service.findRelationshipDetails("test-service", "other-service")).thenReturn(details);

        Response response = resources.target("/test-service/relationship/details/other-service").request().get();

        assertThat(response.getStatus()).isEqualTo(200);

        List<RelationshipDetails> responseList = response.readEntity(List.class);
        assertThat(responseList).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should return the list of dependent services for each service recorded")
    void testGetAllDependencies() {
        when(service.buildAllDependencies()).thenReturn(newArrayList(ServiceDependencies.builder().serviceName("test-service").dependencies(newHashSet("another-service")).build()));

        Response response = resources.target("/relationships").request().get();

        assertThat(response.getStatus()).isEqualTo(200);

        List<ServiceDependencies> responseList = response.readEntity(List.class);
        assertThat(responseList).hasSize(1);
    }

    // TODO: Extract this duplicate code into a Test Utils (duplicate code is in the client module... so would need a common module)
    private ConnectionEvent buildEvent(String serviceName, Direction direction, String identifier) {
        return ConnectionEvent.builder()
                .serviceName(serviceName)
                .communicationType(JMS)
                .eventDirection(direction)
                .connectionIdentifier(identifier)
                .observedAt(System.currentTimeMillis())
                .id(ThreadLocalRandom.current().nextLong())
                .build();
    }
}
