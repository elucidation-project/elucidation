package org.kiwiproject.elucidation.server.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.server.db.mapper.ConnectionEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.junit.jupiter.Jdbi3DaoExtension;
import org.kiwiproject.test.junit.jupiter.PostgresLiquibaseTestExtension;

import java.util.List;
import java.util.stream.IntStream;

@DisplayName("ConnectionEventDao")
class ConnectionEventDaoTest {

    @RegisterExtension
    static final PostgresLiquibaseTestExtension POSTGRES = new PostgresLiquibaseTestExtension("elucidation-migrations.xml");

    @RegisterExtension
    final Jdbi3DaoExtension<ConnectionEventDao> daoExtension = Jdbi3DaoExtension.<ConnectionEventDao>builder()
            .daoType(ConnectionEventDao.class)
            .dataSource(POSTGRES.getTestDataSource())
            .build();

    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_CONNECTION_PATH = "GET /test/path";
    private static final String SERVICE_NAME_PROPERTY = "serviceName";

    private ConnectionEventDao dao;
    private Handle handle;

    @BeforeEach
    void setUp() {
        dao = daoExtension.getDao();
        handle = daoExtension.getHandle();
    }

    @Nested
    class InsertConnection {
        @Test
        void shouldSuccessfullyInsertANewConnectionEvent() {
            var preSaved = ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .observedAt(System.currentTimeMillis())
                    .build();

            var newId = dao.insertConnection(preSaved);

            assertThat(newId).isNotNull();

            var serviceNames = handle.createQuery("select service_name from connection_events where id = ?")
                            .bind(0, newId)
                            .mapTo(String.class)
                            .list();

            assertThat(serviceNames).hasSize(1).containsExactly(TEST_SERVICE_NAME);
        }
    }

    @Nested
    class FindEventsSince {
        @Test
        void shouldReturnAllEventsStoredInTheDatabaseSinceAGivenTime() {
            var initialTime = System.currentTimeMillis();

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(TEST_SERVICE_NAME + idx, Direction.INBOUND, (initialTime * idx)));

            var allEvents = dao.findEventsSince(initialTime);

            assertThat(allEvents).hasSize(2)
                    .extracting(SERVICE_NAME_PROPERTY)
                    .containsExactly(TEST_SERVICE_NAME + 3, TEST_SERVICE_NAME + 2);
        }
    }

    @Nested
    class FindEventsByServiceName {
        @Test
        void shouldOnlyReturnEventsForTheGivenService() {
            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(TEST_SERVICE_NAME + idx, Direction.INBOUND));

            var eventsByServiceName = dao.findEventsByServiceName(TEST_SERVICE_NAME + 1);

            assertThat(eventsByServiceName).hasSize(1);
            assertThat(eventsByServiceName.get(0).getServiceName()).isEqualTo(TEST_SERVICE_NAME + 1);
        }
    }

    @Nested
    class FindAssociatedEvents {
        @Test
        void shouldReturnEventsThatMatchTheGivenIdentifierAndOppositeDirection() {
            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupConnectionEvent(associateServiceName, Direction.OUTBOUND);
            setupConnectionEvent(otherServiceName, Direction.INBOUND);

            var associatedEvents = dao.findAssociatedEvents(Direction.OUTBOUND, TEST_CONNECTION_PATH, "HTTP");

            assertThat(associatedEvents).hasSize(1);
            assertThat(associatedEvents.get(0).getServiceName()).isEqualTo(associateServiceName);
        }
    }

    @Nested
    class FindAllServiceNames {
        @Test
        void shouldReturnJustTheListOfExistingServiceNames() {
            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupConnectionEvent(associateServiceName, Direction.OUTBOUND);
            setupConnectionEvent(otherServiceName, Direction.INBOUND);

            var serviceNames = dao.findAllServiceNames();

            assertThat(serviceNames).hasSize(2).containsOnly(associateServiceName, otherServiceName);
        }
    }

    @Nested
    class FindEventsByConnectionIdentifier {
        @Test
        void shouldOnlyReturnEventsThatMatchTheGivenConnectionIdentifier() {
            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupConnectionEvent(TEST_SERVICE_NAME + idx, Direction.INBOUND));

            var eventsByConnectionIdentifier = dao.findEventsByConnectionIdentifier(TEST_CONNECTION_PATH);

            assertThat(eventsByConnectionIdentifier)
                    .hasSize(3)
                    .extracting(ConnectionEvent::getServiceName)
                    .containsExactlyInAnyOrder(TEST_SERVICE_NAME + 1, TEST_SERVICE_NAME + 2, TEST_SERVICE_NAME + 3);
        }
    }

    @Nested
    class CreateOrUpdate {

        @Test
        void shouldCreateANewRecord_WhenOneDoesNotExist() {
            var preSaved = ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .observedAt(System.currentTimeMillis())
                    .build();

            var servicesPreInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPreInsert).isEmpty();

            dao.createOrUpdate(preSaved);

            var servicesPostInsert = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(servicesPostInsert).hasSize(1);
        }

        @Test
        void shouldUpdateAnExistingRecord_WhenTheRecordExists() {
            setupConnectionEvent(TEST_SERVICE_NAME, Direction.OUTBOUND);

            var initialEvents = dao.findEventsByServiceName(TEST_SERVICE_NAME);

            assertThat(initialEvents).hasSize(1);

            dao.createOrUpdate(ConnectionEvent.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .eventDirection(Direction.OUTBOUND)
                    .communicationType("HTTP")
                    .connectionIdentifier(TEST_CONNECTION_PATH)
                    .build());

            var eventsAfterFirstUpdate = eventsForService();

            assertThat(eventsAfterFirstUpdate).hasSize(1);

            var existingEvent = eventsAfterFirstUpdate.get(0);
            dao.createOrUpdate(existingEvent);
            var eventsAfterSecondUpdate = eventsForService();

            assertThat(eventsAfterSecondUpdate).extracting(ConnectionEvent::getId).containsOnly(existingEvent.getId());

            var updatedEvent = eventsAfterSecondUpdate.get(0);

            assertThat(updatedEvent.getObservedAt()).isGreaterThan(existingEvent.getObservedAt());
        }

        private List<ConnectionEvent> eventsForService() {
            return handle.createQuery("select * from connection_events where service_name = :serviceName")
                            .bind(SERVICE_NAME_PROPERTY, ConnectionEventDaoTest.TEST_SERVICE_NAME)
                            .map(new ConnectionEventMapper())
                            .list();
        }

    }

    private void setupConnectionEvent(String serviceName, Direction direction) {
        setupConnectionEvent(serviceName, direction, System.currentTimeMillis());
    }

    private void setupConnectionEvent(String serviceName, Direction direction, Long observedAt) {
        handle.execute("insert into connection_events " +
                                "(service_name, event_direction, communication_type, connection_identifier, observed_at) " +
                                "values (?, ?, ?, ?, ?)",
                        serviceName, direction.name(), "HTTP", TEST_CONNECTION_PATH, observedAt);
    }

}
