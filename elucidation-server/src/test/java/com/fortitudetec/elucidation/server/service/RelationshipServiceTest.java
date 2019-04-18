package com.fortitudetec.elucidation.server.service;

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

import static com.fortitudetec.elucidation.common.model.CommunicationType.JMS;
import static com.fortitudetec.elucidation.common.test.ConnectionEvents.newConnectionEvent;
import static com.fortitudetec.elucidation.server.test.TestConstants.ANOTHER_SERVICE_NAME;
import static com.fortitudetec.elucidation.server.test.TestConstants.A_SERVICE_NAME;
import static com.fortitudetec.elucidation.server.test.TestConstants.COMMUNICATION_TYPE_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.CONNECTION_IDENTIFIER_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.DEPENDENCIES_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.EVENT_DIRECTION_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.HAS_INBOUND_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.HAS_OUTBOUND_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.IGNORED_MSG;
import static com.fortitudetec.elucidation.server.test.TestConstants.MSG_FROM_ANOTHER_SERVICE;
import static com.fortitudetec.elucidation.server.test.TestConstants.MSG_TO_ANOTHER_SERVICE;
import static com.fortitudetec.elucidation.server.test.TestConstants.SERVICE_NAME_FIELD;
import static com.fortitudetec.elucidation.server.test.TestConstants.YET_ANOTHER_SERVICE_NAME;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.common.model.RelationshipDetails;
import com.fortitudetec.elucidation.server.core.ServiceConnections;
import com.fortitudetec.elucidation.server.core.ServiceDependencies;
import com.fortitudetec.elucidation.server.db.ConnectionEventDao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class RelationshipServiceTest {

    private ConnectionEventDao dao;
    private RelationshipService service;

    @BeforeEach
    void setUp() {
        dao = mock(ConnectionEventDao.class);
        service = new RelationshipService(dao);
    }

    @Test
    @DisplayName("should pass a ConnectionEvent onto the dao to be created")
    void testCreateEvent() {
        ConnectionEvent event = newConnectionEvent(null, A_SERVICE_NAME, Direction.OUTBOUND, "some-identifier");

        service.createEvent(event);

        verify(dao).createOrUpdate(event);
    }

    @Test
    @DisplayName("should return a list of ConnectionEvents that have been recorded for a given service")
    void testListEventsForService() {
        when(dao.findEventsByServiceName(A_SERVICE_NAME)).thenReturn(newArrayList(
                newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
        ));

        List<ConnectionEvent> events = service.listEventsForService(A_SERVICE_NAME);

        assertThat(events).hasSize(3)
                .extracting(SERVICE_NAME_FIELD, EVENT_DIRECTION_FIELD, CONNECTION_IDENTIFIER_FIELD)
                .contains(
                        tuple(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                        tuple(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                        tuple(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                );
    }

    @Test
    @DisplayName("should return a ServiceConnection without any events")
    void testBuildRelationships_NoEvents() {
        when(dao.findEventsByServiceName(A_SERVICE_NAME)).thenReturn(newArrayList());

        ServiceConnections serviceConnections = service.buildRelationships(A_SERVICE_NAME);

        assertThat(serviceConnections.getServiceName()).isEqualTo(A_SERVICE_NAME);
        assertThat(serviceConnections.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("should return a Service Connection with the proper events")
    void testBuildRelationships_WithEvents() {
        when(dao.findEventsByServiceName(A_SERVICE_NAME)).thenReturn(
                newArrayList(
                        newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                ));

        when(dao.findAssociatedEvents(Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE, JMS)).thenReturn(
                newArrayList(newConnectionEvent(ANOTHER_SERVICE_NAME, Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, MSG_TO_ANOTHER_SERVICE, JMS)).thenReturn(
                newArrayList(newConnectionEvent(YET_ANOTHER_SERVICE_NAME, Direction.INBOUND, MSG_TO_ANOTHER_SERVICE))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, IGNORED_MSG, JMS)).thenReturn(newArrayList());

        ServiceConnections serviceConnections = service.buildRelationships(A_SERVICE_NAME);

        assertThat(serviceConnections.getServiceName()).isEqualTo(A_SERVICE_NAME);
        assertThat(serviceConnections.getChildren()).hasSize(3)
                .extracting(SERVICE_NAME_FIELD, HAS_INBOUND_FIELD, HAS_OUTBOUND_FIELD)
                .contains(
                        tuple(ANOTHER_SERVICE_NAME, true, false),
                        tuple(YET_ANOTHER_SERVICE_NAME, false, true),
                        tuple("unknown-service", false, true)
                );
    }

    @Test
    @DisplayName("should return a list of details for the relationships between 2 services")
    void testFindRelationshipDetails() {
        when(dao.findEventsByServiceName(A_SERVICE_NAME)).thenReturn(
                newArrayList(
                        newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                )
        );

        when(dao.findAssociatedEvents(Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE, JMS)).thenReturn(
                newArrayList(newConnectionEvent(ANOTHER_SERVICE_NAME, Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, MSG_TO_ANOTHER_SERVICE, JMS)).thenReturn(
                newArrayList(newConnectionEvent(YET_ANOTHER_SERVICE_NAME, Direction.INBOUND, MSG_TO_ANOTHER_SERVICE))
        );

        when(dao.findAssociatedEvents(Direction.INBOUND, IGNORED_MSG, JMS)).thenReturn(newArrayList());

        List<RelationshipDetails> relationshipDetails = service.findRelationshipDetails(A_SERVICE_NAME, ANOTHER_SERVICE_NAME);

        assertThat(relationshipDetails).hasSize(1)
                .extracting(COMMUNICATION_TYPE_FIELD, CONNECTION_IDENTIFIER_FIELD, EVENT_DIRECTION_FIELD)
                .contains(tuple(JMS, MSG_FROM_ANOTHER_SERVICE, Direction.INBOUND));
    }

    @Test
    @DisplayName("should return a list of service dependencies for a given service")
    void testBuildAllDependencies() {
        when(dao.findAllServiceNames()).thenReturn(newArrayList(A_SERVICE_NAME));
        when(dao.findEventsByServiceName(A_SERVICE_NAME)).thenReturn(
                newArrayList(
                        newConnectionEvent(A_SERVICE_NAME, Direction.INBOUND, MSG_FROM_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, MSG_TO_ANOTHER_SERVICE),
                        newConnectionEvent(A_SERVICE_NAME, Direction.OUTBOUND, IGNORED_MSG)
                )
        );

        when(dao.findAssociatedEvents(Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE, JMS)).thenReturn(
                newArrayList(newConnectionEvent(ANOTHER_SERVICE_NAME, Direction.OUTBOUND, MSG_FROM_ANOTHER_SERVICE))
        );

        List<ServiceDependencies> serviceDependencies = service.buildAllDependencies();

        assertThat(serviceDependencies).hasSize(1)
                .extracting(SERVICE_NAME_FIELD, DEPENDENCIES_FIELD)
                .contains(tuple(A_SERVICE_NAME, newHashSet(ANOTHER_SERVICE_NAME)));
    }

}
