package com.fortitudetec.elucidation.server.service;

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

import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.fortitudetec.elucidation.server.db.H2JDBIExtension;
import com.fortitudetec.elucidation.server.db.TrackedConnectionIdentifierDao;
import com.fortitudetec.elucidation.server.db.mapper.TrackedConnectionIdentifierMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(H2JDBIExtension.class)
@DisplayName("TrackedConnectionIdentifierServiceIntegeration")
class TrackedConnectionIdentifierServiceIntegrationTest {

    private static final String TEST_ONLY_SERVICE = "foo-service";

    private TrackedConnectionIdentifierService service;

    @BeforeEach
    void setUp(Jdbi jdbi) {
        var dao = jdbi.onDemand(TrackedConnectionIdentifierDao.class);
        service = new TrackedConnectionIdentifierService(dao);
    }

    @Nested
    class LoadNewIdentifiers {
        @Test
        void shouldCreateNewTrackedConnectionIdentifiers(Jdbi jdbi) {
            var identifiers = List.of("identifier-1", "identifier-2");
            var identifiersAdded = service.loadNewIdentifiers(TEST_ONLY_SERVICE, "HTTP", identifiers);

            assertThat(identifiersAdded).isEqualTo(identifiers.size());

            var savedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers where service_name = ? and communication_type = ?")
                            .bind(0, TEST_ONLY_SERVICE)
                            .bind(1, "HTTP")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            assertThat(savedIdentifiers)
                    .hasSize(2)
                    .extracting("connectionIdentifier")
                    .containsAll(identifiers);
        }

        @Test
        void shouldRemoveExistingTrackedConnectionIdentifiersBeforeInsert_WhenSomeExist(Jdbi jdbi) {
            jdbi.withHandle(handle ->
                    handle.execute(
                            "insert into tracked_connection_identifiers (service_name, communication_type, connection_identifier) values (?, ?, ?)",
                            TEST_ONLY_SERVICE, "HTTP", "some-whack-identifier"));

            var identifiers = List.of("identifier-1", "identifier-2");
            var identifiersAdded = service.loadNewIdentifiers(TEST_ONLY_SERVICE, "HTTP", identifiers);

            assertThat(identifiersAdded).isEqualTo(identifiers.size());

            var savedIdentifiers = jdbi.withHandle(handle ->
                    handle.createQuery("select * from tracked_connection_identifiers where service_name = ? and communication_type = ?")
                            .bind(0, TEST_ONLY_SERVICE)
                            .bind(1, "HTTP")
                            .registerRowMapper(new TrackedConnectionIdentifierMapper())
                            .mapTo(TrackedConnectionIdentifier.class)
                            .list());

            assertThat(savedIdentifiers)
                    .hasSize(2)
                    .extracting("connectionIdentifier")
                    .containsOnly(identifiers.toArray());
        }
    }

    private int countExistingEvents(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("select count(*) from tracked_connection_identifier")
                .mapTo(Integer.class)
                .first());
    }

    void assertDataIsLoaded(Jdbi jdbi) {
        assertThat(countExistingEvents(jdbi)).isGreaterThan(0);
    }
}
