package org.kiwiproject.elucidation.common.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

class CommunicationDefinitionTest {

    @Nested
    class ToMap {

        @Test
        void shouldReturnEmptyMap_WhenDefinitionsAreEmpty() {
            assertThat(CommunicationDefinition.toMap(List.of())).isEmpty();
        }

        @Test
        void shouldReturnMap_WhenDefinitionsAreNotEmpty() {
            var http = new HttpCommunicationDefinition();
            var jms = new JmsCommunicationDefinition();
            var rabbitMq = new AsyncMessagingCommunicationDefinition() {
                @Override
                public String getCommunicationType() {
                    return "RabbitMQ";
                }
            };
            var defs = List.of(http, jms, rabbitMq);

            var defsMap = CommunicationDefinition.toMap(defs);

            assertThat(defsMap).containsOnly(
                    entry(http.getCommunicationType(), http),
                    entry(jms.getCommunicationType(), jms),
                    entry(rabbitMq.getCommunicationType(), rabbitMq)
            );
        }
    }

    @Nested
    class ForDependentDirection {

        private CommunicationDefinition definition;
        private String communicationType;
        private Direction dependentEvenDirection;

        @BeforeEach
        void setUp() {
            communicationType = "Kafka";
            dependentEvenDirection = Direction.INBOUND;
            definition = CommunicationDefinition.forDependentDirection(communicationType, dependentEvenDirection);
        }

        @Test
        void shouldReturnCommunicationType() {
            assertThat(definition.getCommunicationType()).isEqualTo(communicationType);
        }

        @ParameterizedTest
        @EnumSource(Direction.class)
        void shouldCorrectlyDetermineDependentEvents(Direction direction) {
            var event = ConnectionEvent.builder()
                    .eventDirection(direction)
                    .build();

            var shouldBeDependent = (direction == dependentEvenDirection);

            assertThat(definition.isDependentEvent(event))
                    .describedAs("#isDependentEvent should have returned %b for direction %s",
                            shouldBeDependent, direction)
                    .isEqualTo(shouldBeDependent);
        }
    }

}
