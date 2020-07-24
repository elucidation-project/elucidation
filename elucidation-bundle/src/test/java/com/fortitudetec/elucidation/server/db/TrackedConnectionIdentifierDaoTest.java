package com.fortitudetec.elucidation.server.db;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.collect.KiwiLists.first;

import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.IntStream;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("TrackedConnectionIdentifierDao")
class TrackedConnectionIdentifierDaoTest {

    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_CONNECTION_PATH = "GET /test/path";
    private static final String SERVICE_NAME_PROPERTY = "serviceName";

    @Nested
    class InsertIdentifier {
        @Test
        void shouldSuccessfullyInsertANewTrackedConnectionIdentifier(Jdbi jdbi) {
            var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

            var preSaved = TrackedConnectionIdentifier.builder()
                    .serviceName(TEST_SERVICE_NAME)
                    .communicationType("HTTP")
                    .connectionIdentifier("GET /doSomething")
                    .build();

            var newId = dao.insertIdentifier(preSaved);

            assertThat(newId).isPositive();

            var serviceNames = jdbi.withHandle(handle ->
                    handle.createQuery("select service_name from tracked_connection_identifiers where id = ?")
                            .bind(0, newId)
                            .mapTo(String.class)
                            .list());

            assertThat(serviceNames).hasSize(1).containsExactly(TEST_SERVICE_NAME);
        }
    }

    @Nested
    class FindIdentifiers {
        @Test
        void shouldReturnAllTrackedConnectionIdentifiersStoredInTheDatabase(Jdbi jdbi) {
            var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(jdbi, TEST_SERVICE_NAME + idx));

            var allEvents = dao.findIdentifiers();

            assertThat(allEvents).hasSize(3)
                    .extracting(SERVICE_NAME_PROPERTY)
                    .containsOnly(TEST_SERVICE_NAME + 3, TEST_SERVICE_NAME + 2, TEST_SERVICE_NAME + 1);
        }
    }

    @Nested
    class ClearIdentifiersFor {

        @Test
        void shouldDeleteAllTrackedConnectionIdentifiersForTheGivenServiceAndCommunicationType(Jdbi jdbi) {
            var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(jdbi, TEST_SERVICE_NAME + idx));

            var deletedCount = dao.clearIdentifiersFor(TEST_SERVICE_NAME + 1, "HTTP");

            assertThat(deletedCount).isEqualTo(1);

            var countFromDb = jdbi.withHandle(handle ->
                    handle.createQuery("select count(*) from tracked_connection_identifiers")
                        .mapTo(Integer.class)
                        .first());
            assertThat(countFromDb).isEqualTo(2);
        }
    }

    @Nested
    class FindAllServiceNames {
        @Test
        void shouldReturnJustTheListOfExistingServiceNames(Jdbi jdbi) {
            var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

            var associateServiceName = "test-associated-service";
            var otherServiceName = "test-other-service";

            setupIdentifier(jdbi, associateServiceName);
            setupIdentifier(jdbi, otherServiceName);

            var serviceNames = dao.findAllServiceNames();

            assertThat(serviceNames).containsExactlyInAnyOrder(associateServiceName, otherServiceName);
        }
    }

    @Nested
    class FindByServiceName {
        @Test
        void shouldOnlyReturnIdentifiersForTheGivenService(Jdbi jdbi) {
            var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);

            IntStream.rangeClosed(1,3)
                    .forEach(idx -> setupIdentifier(jdbi, TEST_SERVICE_NAME + idx));

            var eventsByServiceName = dao.findByServiceName(TEST_SERVICE_NAME + 1);

            assertThat(eventsByServiceName).hasSize(1);
            assertThat(first(eventsByServiceName).getServiceName()).isEqualTo(TEST_SERVICE_NAME + 1);
        }
    }

    private static void setupIdentifier(Jdbi jdbi, String serviceName) {
        jdbi.withHandle(handle -> handle
                .execute("insert into tracked_connection_identifiers " +
                                "(service_name, communication_type, connection_identifier) " +
                                "values (?, ?, ?)",
                        serviceName, "HTTP", TEST_CONNECTION_PATH));
    }

}
