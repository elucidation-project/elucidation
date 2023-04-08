package org.kiwiproject.elucidation.server.db;

import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.kiwiproject.test.junit.jupiter.Jdbi3DaoExtension;
import org.kiwiproject.test.junit.jupiter.PostgresLiquibaseTestExtension;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.collect.KiwiLists.first;

@DisplayName("TrackedConnectionIdentifierDao")
class TrackedConnectionIdentifierDaoTest {

    @RegisterExtension
    static final PostgresLiquibaseTestExtension POSTGRES = new PostgresLiquibaseTestExtension("elucidation-migrations.xml");

    @RegisterExtension
    final Jdbi3DaoExtension<TrackedConnectionIdentifierDao> daoExtension = Jdbi3DaoExtension.<TrackedConnectionIdentifierDao>builder()
            .daoType(TrackedConnectionIdentifierDao.class)
            .dataSource(POSTGRES.getTestDataSource())
            .build();

    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_CONNECTION_PATH = "GET /test/path";
    private static final String SERVICE_NAME_PROPERTY = "serviceName";

    private TrackedConnectionIdentifierDao dao;
    private Handle handle;

    @BeforeEach
    void setUp() {
        dao = daoExtension.getDao();
        handle = daoExtension.getHandle();
    }

    @Nested
    class InsertIdentifier {
        @Test
        void shouldSuccessfullyInsertANewTrackedConnectionIdentifier() {
            var preSaved = TrackedConnectionIdentifier.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .build();

            var newId = dao.insertIdentifier(preSaved);

            assertThat(newId).isPositive();

            var serviceNames = handle.createQuery("select service_name from tracked_connection_identifiers where id = ?")
                            .bind(0, newId)
                            .mapTo(String.class)
                            .list();

            assertThat(serviceNames).hasSize(1).containsExactly(TEST_SERVICE_NAME);
        }
    }

    @Nested
    class FindIdentifiers {
        @Test
        void shouldReturnAllTrackedConnectionIdentifiersStoredInTheDatabase() {
            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(TEST_SERVICE_NAME + idx));

            var allEvents = dao.findIdentifiers();

            assertThat(allEvents).hasSize(3)
                    .extracting(SERVICE_NAME_PROPERTY)
                    .containsOnly(TEST_SERVICE_NAME + 3, TEST_SERVICE_NAME + 2, TEST_SERVICE_NAME + 1);
        }
    }

    @Nested
    class ClearIdentifiersFor {

        @Test
        void shouldDeleteAllTrackedConnectionIdentifiersForTheGivenServiceAndCommunicationType() {
            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(TEST_SERVICE_NAME + idx));

            var deletedCount = dao.clearIdentifiersFor(TEST_SERVICE_NAME + 1, "HTTP");

            assertThat(deletedCount).isEqualTo(1);

            var countFromDb = handle.createQuery("select count(*) from tracked_connection_identifiers")
                        .mapTo(Integer.class)
                        .first();

            assertThat(countFromDb).isEqualTo(2);
        }
    }

    @Nested
    class FindAllServiceNames {
        @Test
        void shouldReturnJustTheListOfExistingServiceNames() {
            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupIdentifier(associateServiceName);
            setupIdentifier(otherServiceName);

            var serviceNames = dao.findAllServiceNames();

            assertThat(serviceNames).containsExactlyInAnyOrder(associateServiceName, otherServiceName);
        }
    }

    @Nested
    class FindByServiceName {
        @Test
        void shouldOnlyReturnIdentifiersForTheGivenService() {
            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(TEST_SERVICE_NAME + idx));

            var eventsByServiceName = dao.findByServiceName(TEST_SERVICE_NAME + 1);

            assertThat(eventsByServiceName).hasSize(1);
            assertThat(first(eventsByServiceName).getServiceName()).isEqualTo(TEST_SERVICE_NAME + 1);
        }
    }

    private void setupIdentifier(String serviceName) {
        handle.execute("insert into tracked_connection_identifiers " +
                                "(service_name, communication_type, connection_identifier) " +
                                "values (?, ?, ?)",
                        serviceName, "HTTP", TEST_CONNECTION_PATH);
    }

}
