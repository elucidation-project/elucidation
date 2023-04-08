package org.kiwiproject.elucidation.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.HttpCommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.JmsCommunicationDefinition;
import org.kiwiproject.elucidation.common.model.Direction;
import io.dropwizard.util.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

class ElucidationConfigurationTest {

    private TestAppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new TestAppConfig();
    }

    @Nested
    class DefaultConfiguration {

        private ElucidationConfiguration<TestAppConfig> defaultElucidationConfig;

        @BeforeEach
        void setUp() {
            defaultElucidationConfig = new DefaultElucidationConfiguration();
        }

        @Test
        void shouldHaveTimeToLive_OfSevenDays() {
            assertThat(defaultElucidationConfig.getTimeToLive(appConfig)).isEqualTo(Duration.days(7));
        }

        @Test
        void shouldHaveDefaultCommunicationDefinitions() {
            var definitions = defaultElucidationConfig.getCommunicationDefinitions(appConfig);

            assertDefaultCommunicationDefinitions(definitions);
        }
    }

    @Nested
    class CustomConfiguration {

        private CustomElucidationConfiguration customElucidationConfig;

        @BeforeEach
        void setUp() {
            customElucidationConfig = new CustomElucidationConfiguration();
        }

        @Test
        void shouldHaveCustomTimeToLive() {
            assertThat(customElucidationConfig.getTimeToLive(appConfig)).isEqualTo(Duration.days(14));
        }

        @Test
        void shouldHaveCustomCommunicationDefinitions() {
            var definitions = customElucidationConfig.getCommunicationDefinitions(appConfig);

            assertContainsOnlyCommunicationTypes(definitions,
                    "REST",
                    "RabbitMQ",
                    new JmsCommunicationDefinition().getCommunicationType()
            );
        }
    }

    @Nested
    class DefaultCommunicationDefinitions {

        @Test
        void shouldReturnOnlyTheDefaultDefinitions() {
            var definitions = ElucidationConfiguration.defaultCommunicationDefinitions();

            assertDefaultCommunicationDefinitions(definitions);
        }
    }

    private void assertDefaultCommunicationDefinitions(List<CommunicationDefinition> definitions) {
        assertThat(definitions)
                .extracting(def -> def.getClass().getName())
                .containsOnly(
                        HttpCommunicationDefinition.class.getName(),
                        JmsCommunicationDefinition.class.getName()
                );
    }

    @Nested
    class DefaultCommunicationDefinitionsAndAdditional {

        @Test
        void shouldReturnDefaultAndAdditionalDefinitions() {
            var kafka = CommunicationDefinition.forDependentDirection("Kafka", Direction.INBOUND);
            var gRPC = CommunicationDefinition.forDependentDirection("gRPC", Direction.OUTBOUND);

            var definitions = ElucidationConfiguration.defaultDefinitionsAnd(kafka, gRPC);

            assertContainsOnlyCommunicationTypes(definitions,
                    new HttpCommunicationDefinition().getCommunicationType(),
                    new JmsCommunicationDefinition().getCommunicationType(),
                    "Kafka",
                    "gRPC"
            );
        }
    }

    private static void assertContainsOnlyCommunicationTypes(List<CommunicationDefinition> definitions, String... expectedTypes) {
        assertThat(definitions)
                .extracting(CommunicationDefinition::getCommunicationType)
                .containsOnly(expectedTypes);
    }

    @Nested
    class IsRegisterJdbiExceptionMappers {

        @Test
        void shouldBeTrueByDefault() {
            var elucidationConfig = new DefaultElucidationConfiguration();
            assertThat(elucidationConfig.isRegisterJdbiExceptionMappers(appConfig)).isTrue();
        }

        @Test
        void shouldBeOverridable() {
            var elucidationConfig = new CustomElucidationConfiguration() {
                @Override
                public boolean isRegisterJdbiExceptionMappers(TestAppConfig config) {
                    return false;
                }
            };

            assertThat(elucidationConfig.isRegisterJdbiExceptionMappers(appConfig)).isFalse();
        }
    }
}
