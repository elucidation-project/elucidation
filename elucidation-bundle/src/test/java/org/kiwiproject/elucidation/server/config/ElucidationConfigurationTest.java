package org.kiwiproject.elucidation.server.config;

/*-
 * #%L
 * Elucidation Bundle
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
