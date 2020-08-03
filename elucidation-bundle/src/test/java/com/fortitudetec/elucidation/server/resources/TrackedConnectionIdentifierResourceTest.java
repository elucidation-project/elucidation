package com.fortitudetec.elucidation.server.resources;

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

import static com.fortitudetec.elucidation.server.test.TestConstants.A_SERVICE_NAME;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import com.fortitudetec.elucidation.server.core.UnusedIdentifier;
import com.fortitudetec.elucidation.server.core.UnusedServiceIdentifiers;
import com.fortitudetec.elucidation.server.service.TrackedConnectionIdentifierService;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("TrackedConnectionIdentifierResource")
class TrackedConnectionIdentifierResourceTest {

    private static final TrackedConnectionIdentifierService SERVICE = mock(TrackedConnectionIdentifierService.class);

    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new TrackedConnectionIdentifierResource(SERVICE))
            .build();

    @BeforeEach
    void setUp() {
        reset(SERVICE);
    }

    @Nested
    class LoadTrackedIdentifiers {

        @Test
        void shouldReturnAccepted_WhenInputsAreValid() {
            var identifiers = List.of("identifier-a", "identifier-b");

            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(identifiers));

            assertThat(response.getStatus()).isEqualTo(202);

            verify(SERVICE).loadNewIdentifiers(A_SERVICE_NAME, "HTTP", identifiers);
        }

        @Test
        void shouldReturn422_WhenBodyIsMissing() {
            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(""));

            assertThat(response.getStatus()).isEqualTo(422);
            verifyNoInteractions(SERVICE);
        }

        @Test
        void shouldReturn422_WhenBodyIsEmptyList() {
            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(List.of()));

            assertThat(response.getStatus()).isEqualTo(422);
            verifyNoInteractions(SERVICE);
        }

        @Test
        void shouldReturn422_WhenBodyHasBlankValues() {
            var identifiers = List.of("identifier-a", "");

            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(identifiers));

            assertThat(response.getStatus()).isEqualTo(422);
            verifyNoInteractions(SERVICE);
        }

        @Test
        void shouldReturn422_WhenBodyHasNullValues() {
            var identifiers = new ArrayList<String>();
            identifiers.add("identifier-a");
            identifiers.add(null);

            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(identifiers));

            assertThat(response.getStatus()).isEqualTo(422);
            verifyNoInteractions(SERVICE);
        }
    }

    @Nested
    class FindUnusedIdentifiers {
        @Test
        void shouldReturnAnyUnusedIdentifiers() {
            var unused = List.of(
                    UnusedServiceIdentifiers.builder()
                            .serviceName(A_SERVICE_NAME)
                            .identifiers(List.of(
                                    UnusedIdentifier.builder().communicationType("JMS").connectionIdentifier("SOME_MSG_TYPE").build(),
                                    UnusedIdentifier.builder().communicationType("HTTP").connectionIdentifier("GET /some/unused/path").build()
                            ))
                            .build()
            );

            when(SERVICE.findUnusedIdentifiers()).thenReturn(unused);

            var response = RESOURCES.target("/elucidate/connectionIdentifier/unused")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var unusedFromResponse = response.readEntity(new GenericType<List<UnusedServiceIdentifiers>>(){});
            assertThat(unusedFromResponse).usingElementComparatorOnFields("serviceName").containsAll(unused);
        }
    }

    @Nested
    class FindUnusedIdentifiersForService {
        @Test
        void shouldReturnAnyUnusedIdentifiersForTheGivenService() {
            var unused = UnusedServiceIdentifiers.builder()
                            .serviceName(A_SERVICE_NAME)
                            .identifiers(List.of(
                                    UnusedIdentifier.builder().communicationType("JMS").connectionIdentifier("SOME_MSG_TYPE").build(),
                                    UnusedIdentifier.builder().communicationType("HTTP").connectionIdentifier("GET /some/unused/path").build()
                            ))
                            .build();

            when(SERVICE.findUnusedIdentifiersForService(A_SERVICE_NAME)).thenReturn(unused);

            var response = RESOURCES.target("/elucidate/connectionIdentifier/{serviceName}/unused")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var unusedFromResponse = response.readEntity(UnusedServiceIdentifiers.class);
            assertThat(unusedFromResponse.getServiceName()).isEqualTo(A_SERVICE_NAME);
            assertThat(unusedFromResponse.getIdentifiers())
                    .extracting("communicationType", "connectionIdentifier")
                    .containsOnly(
                            tuple("JMS", "SOME_MSG_TYPE"),
                            tuple("HTTP", "GET /some/unused/path")
                    );
        }
    }

    @Nested
    class AllTrackedIdentifiers {
        @Test
        void shouldReturnAnyUnusedIdentifiers() {
            var trackedIdentifiers = List.of(TrackedConnectionIdentifier.builder()
                    .serviceName(A_SERVICE_NAME)
                    .communicationType("HTTP")
                    .connectionIdentifier("/path/unused")
                    .build());

            when(SERVICE.allTrackedConnectionIdentifiers()).thenReturn(trackedIdentifiers);

            var response = RESOURCES.target("/elucidate/trackedIdentifiers")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var trackedFromResponse = response.readEntity(new GenericType<List<TrackedConnectionIdentifier>>(){});
            assertThat(trackedFromResponse).usingElementComparatorOnFields("serviceName", "communicationType", "connectionIdentifier").containsAll(trackedIdentifiers);
        }
    }
}
