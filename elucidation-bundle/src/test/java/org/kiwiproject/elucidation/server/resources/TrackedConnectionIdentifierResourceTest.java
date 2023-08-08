package org.kiwiproject.elucidation.server.resources;

import static org.kiwiproject.elucidation.server.test.TestConstants.A_SERVICE_NAME;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertAcceptedResponse;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertOkResponse;
import static org.kiwiproject.test.jaxrs.JaxrsTestHelper.assertUnprocessableEntity;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.common.model.TrackedConnectionIdentifier;
import org.kiwiproject.elucidation.server.core.UnusedIdentifier;
import org.kiwiproject.elucidation.server.core.UnusedServiceIdentifiers;
import org.kiwiproject.elucidation.server.service.TrackedConnectionIdentifierService;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
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

            assertAcceptedResponse(response);

            verify(SERVICE).loadNewIdentifiers(A_SERVICE_NAME, "HTTP", identifiers);
        }

        @Test
        void shouldReturn422_WhenBodyIsMissing() {
            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(""));

            assertUnprocessableEntity(response);
            verifyNoInteractions(SERVICE);
        }

        @Test
        void shouldReturn422_WhenBodyIsEmptyList() {
            var response = RESOURCES.target("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", A_SERVICE_NAME)
                    .resolveTemplate("communicationType", "HTTP")
                    .request()
                    .post(json(List.of()));

            assertUnprocessableEntity(response);
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

            assertUnprocessableEntity(response);
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

            assertUnprocessableEntity(response);
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

            assertOkResponse(response);

            var unusedFromResponse = response.readEntity(new GenericType<List<UnusedServiceIdentifiers>>() {
            });
            assertThat(unusedFromResponse)
                    .usingRecursiveFieldByFieldElementComparatorOnFields("serviceName")
                    .containsAll(unused);
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

            assertOkResponse(response);

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

            assertOkResponse(response);

            var trackedFromResponse = response.readEntity(new GenericType<List<TrackedConnectionIdentifier>>() {
            });
            assertThat(trackedFromResponse)
                    .usingRecursiveFieldByFieldElementComparatorOnFields("serviceName", "communicationType", "connectionIdentifier")
                    .containsAll(trackedIdentifiers);
        }
    }
}
