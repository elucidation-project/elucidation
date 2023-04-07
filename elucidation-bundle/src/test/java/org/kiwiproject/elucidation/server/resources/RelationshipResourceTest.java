package org.kiwiproject.elucidation.server.resources;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
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

import static org.kiwiproject.elucidation.common.test.ConnectionEvents.newConnectionEvent;
import static org.kiwiproject.elucidation.server.test.TestConstants.ANOTHER_SERVICE_NAME;
import static org.kiwiproject.elucidation.server.test.TestConstants.A_SERVICE_NAME;
import static org.kiwiproject.elucidation.server.test.TestConstants.CONNECTION_IDENTIFIER_FIELD;
import static org.kiwiproject.elucidation.server.test.TestConstants.EVENT_DIRECTION_FIELD;
import static org.kiwiproject.elucidation.server.test.TestConstants.IGNORED_MSG;
import static org.kiwiproject.elucidation.server.test.TestConstants.MSG_FROM_ANOTHER_SERVICE;
import static org.kiwiproject.elucidation.server.test.TestConstants.MSG_TO_ANOTHER_SERVICE;
import static org.kiwiproject.elucidation.server.test.TestConstants.SERVICE_NAME_FIELD;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.kiwiproject.collect.KiwiMaps.newHashMap;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertAcceptedResponse;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertBadRequest;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertOkResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.common.model.RelationshipDetails;
import org.kiwiproject.elucidation.server.core.ConnectionSummary;
import org.kiwiproject.elucidation.server.core.ServiceConnections;
import org.kiwiproject.elucidation.server.core.ServiceDependencies;
import org.kiwiproject.elucidation.server.core.ServiceDetails;
import org.kiwiproject.elucidation.server.service.RelationshipService;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
class RelationshipResourceTest {

    private static final RelationshipService SERVICE = mock(RelationshipService.class);

    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new RelationshipResource(SERVICE))
            .build();

    @AfterEach
    void tearDown() {
        reset(SERVICE);
    }

    @Test
    @DisplayName("given a valid event should attempt to save the event")
    void testRecordEvent() {
        var event = newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, "some-identifier");

        var response = RESOURCES.target("/elucidate/event").request().post(Entity.json(event));

        assertAcceptedResponse(response);

        verify(SERVICE).createEvent(event);
    }

    @Nested
    class ViewEventsSince {
        @Test
        @DisplayName("should return a 400 when 'since' param is null")
        void testSinceParamIsNull() {
            var response = RESOURCES.target("/elucidate/events").request().get();

            assertBadRequest(response);
        }

        @Test
        @DisplayName("should return a 400 when 'since' param is not a decimal value")
        void testSinceParamIsANonDecimal() {
            var response = RESOURCES.target("/elucidate/events").queryParam("since", "abc").request().get();

            assertBadRequest(response);
        }

        @Test
        @DisplayName("should return all events that exist when 'since' param is given")
        void testSinceParamIsPresentAndValid() {
            long time = System.currentTimeMillis();
            when(SERVICE.listEventsSince(time)).thenReturn(newArrayList(
                    newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                    newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                    newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
            ));

            var response = RESOURCES.target("/elucidate/events").queryParam("since", time).request().get();

            assertOkResponse(response);

            // DO NOT REMOVE THE GENERIC TYPE DEFINITION!! Doing so will cause a NPE in Java compiler with a nearly
            // incomprehensible message of: "compiler message file broken: key=compiler.misc.msg.bug arguments=<JDK version>"
            //
            // This is a known open bug: https://bugs.openjdk.java.net/browse/JDK-8203195
            @SuppressWarnings("Convert2Diamond")
            List<ConnectionEvent> events = response.readEntity(new GenericType<List<ConnectionEvent>>() {
            });

            assertThat(events).hasSize(3)
                    .extracting(SERVICE_NAME_FIELD, EVENT_DIRECTION_FIELD, CONNECTION_IDENTIFIER_FIELD)
                    .contains(
                            tuple(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                            tuple(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                            tuple(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                    );
        }
    }

    @Test
    @DisplayName("should return a list of ConnectionEvents for a given service")
    void testViewEventsForService() {
        when(SERVICE.listEventsForService(A_SERVICE_NAME)).thenReturn(newArrayList(
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
        ));

        var response = RESOURCES.target("/elucidate/service/test-service/events").request().get();

        assertOkResponse(response);

        // DO NOT REMOVE THE GENERIC TYPE DEFINITION!! Doing so will cause a NPE in Java compiler with a nearly
        // incomprehensible message of: "compiler message file broken: key=compiler.misc.msg.bug arguments=<JDK version>"
        //
        // This is a known open bug: https://bugs.openjdk.java.net/browse/JDK-8203195
        @SuppressWarnings("Convert2Diamond")
        List<ConnectionEvent> events = response.readEntity(new GenericType<List<ConnectionEvent>>() {
        });

        assertThat(events).hasSize(3)
                .extracting(SERVICE_NAME_FIELD, EVENT_DIRECTION_FIELD, CONNECTION_IDENTIFIER_FIELD)
                .contains(
                        tuple(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                        tuple(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                        tuple(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                );
    }

    @Test
    @DisplayName("should return a list of ConnectionEvents for a given connection identifier")
    void testViewEventsForConnectionIdentifier() {
        when(SERVICE.findAllEventsByConnectionIdentifier(MSG_FROM_ANOTHER_SERVICE)).thenReturn(List.of(
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE)
        ));

        var response = RESOURCES
                .target("/elucidate/connectionIdentifier/{connectionIdentifier}/events")
                .resolveTemplate("connectionIdentifier", MSG_FROM_ANOTHER_SERVICE)
                .request()
                .get();

        assertOkResponse(response);

        // DO NOT REMOVE THE GENERIC TYPE DEFINITION!! Doing so will cause a NPE in Java compiler with a nearly
        // incomprehensible message of: "compiler message file broken: key=compiler.misc.msg.bug arguments=<JDK version>"
        //
        // This is a known open bug: https://bugs.openjdk.java.net/browse/JDK-8203195
        var events = response.readEntity(new GenericType<List<ConnectionEvent>>() {
        });

        assertThat(events).hasSize(1)
                .extracting(SERVICE_NAME_FIELD, EVENT_DIRECTION_FIELD, CONNECTION_IDENTIFIER_FIELD)
                .contains(
                        tuple(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE)
                );
    }

    @Test
    @DisplayName("should trigger the build of the relationship data for a given service")
    void testCalculateRelationships() {
        ServiceConnections connections = ServiceConnections.builder()
                .serviceName(A_SERVICE_NAME)
                .children(newHashSet(
                        ConnectionSummary.builder()
                                .serviceName(ANOTHER_SERVICE_NAME)
                                .hasInbound(true)
                                .hasOutbound(true)
                                .build()))
                .build();

        when(SERVICE.buildRelationships(A_SERVICE_NAME)).thenReturn(connections);

        var response = RESOURCES.target("/elucidate/service/test-service/relationships").request().get();

        assertOkResponse(response);

        ServiceConnections connectionsResponse = response.readEntity(ServiceConnections.class);

        assertThat(connectionsResponse.getServiceName()).isEqualTo(A_SERVICE_NAME);
        assertThat(connectionsResponse.getChildren()).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should return the details of the relationships between two given services")
    void testRelationshipDetails() {
        List<RelationshipDetails> details = newArrayList(
                RelationshipDetails.builder()
                        .communicationType("JMS")
                        .connectionIdentifier("ACTIVITY_TEST")
                        .eventDirection(Direction.OUTBOUND)
                        .build());

        when(SERVICE.findRelationshipDetails(A_SERVICE_NAME, "other-service")).thenReturn(details);

        var response = RESOURCES.target("/elucidate/service/test-service/relationship/other-service").request().get();

        assertOkResponse(response);

        List<RelationshipDetails> responseList = response.readEntity(List.class);
        assertThat(responseList).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("should return the list of dependent services for each service recorded")
    void testGetAllDependencies() {
        when(SERVICE.buildAllDependencies()).thenReturn(newArrayList(
                ServiceDependencies.builder()
                        .serviceName(A_SERVICE_NAME)
                        .dependencies(newHashSet(ANOTHER_SERVICE_NAME))
                        .build()));

        var response = RESOURCES.target("/elucidate/dependencies").request().get();

        assertOkResponse(response);

        List<ServiceDependencies> responseList = response.readEntity(List.class);
        assertThat(responseList).hasSize(1);
    }

    @Nested
    class CurrentServiceNames {

        @Test
        void shouldReturnAllServiceNames() {
            when(SERVICE.currentServiceNames()).thenReturn(newArrayList(A_SERVICE_NAME, ANOTHER_SERVICE_NAME));

            var response = RESOURCES.target("/elucidate/services").request().get();
            assertOkResponse(response);

            var services = response.readEntity(new GenericType<List<String>>() {
            });
            assertThat(services).containsOnly(A_SERVICE_NAME, ANOTHER_SERVICE_NAME);
        }
    }

    @Nested
    class CurrentServiceDetails {

        @Test
        void shouldReturnAllServiceDetails() {
            var details = ServiceDetails.builder()
                    .serviceName(A_SERVICE_NAME)
                    .inboundEvents(1)
                    .communicationTypes(newHashMap("HTTP", 1))
                    .build();

            when(SERVICE.currentServiceDetails()).thenReturn(newArrayList(details));

            var response = RESOURCES.target("/elucidate/services/details").request().get();
            assertOkResponse(response);

            var services = response.readEntity(new GenericType<List<ServiceDetails>>() {
            });
            assertThat(services).usingRecursiveFieldByFieldElementComparator().contains(details);
        }
    }

}
