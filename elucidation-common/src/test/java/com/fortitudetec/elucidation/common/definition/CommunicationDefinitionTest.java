package com.fortitudetec.elucidation.common.definition;

/*-
 * #%L
 * Elucidation Common
 * %%
 * Copyright (C) 2018 - 2019 Fortitude Technologies, LLC
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

import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
