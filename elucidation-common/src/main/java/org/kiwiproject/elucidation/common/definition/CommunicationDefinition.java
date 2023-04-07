package org.kiwiproject.elucidation.common.definition;

/*-
 * #%L
 * Elucidation Common
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

import static java.util.stream.Collectors.toUnmodifiableMap;

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;

import java.util.List;
import java.util.Map;

/**
 * Defines a type of communication with a Dropwizard application/service, and whether that communication is "dependent"
 * for a given {@link ConnectionEvent}.
 */
public interface CommunicationDefinition {

    /**
     * A name for this type of communication, which should generally not change once defined. For example, HTTP.
     *
     * @return the type of communication recorded
     */
    String getCommunicationType();

    /**
     * Return {@code true} if the given {@code event} is "dependent".
     * <p>
     * We define an event as "dependent" differently for different types of communication. For example, an event
     * that represents an HTTP request we are making to some other resource on the internet is considered dependent,
     * because we require that remote service to exist in order for the call to succeed. Another example of a dependent
     * event is an incoming message from some asynchronous message source (e.g. RabbitMQ or JMS/ActiveMQ or Kafka),
     * since we need the other service to produce the message in order for us to consume it.
     * <p>
     * An example of an events that is <strong>not</strong> dependent include an outgoing asynchronous message
     * that we publish for others to consume. In this case, we don't know or care if anyone actually consumes the
     * message and thus don't depend on any other service. Another example of an event that is not dependent is
     * an incoming HTTP request that we are handling and returning a response to a remote client. In this situation,
     * while that other service is dependent on us, we are not dependent on it; thus the event is not dependent.
     *
     * @param event The {@link ConnectionEvent} to in question as to whether it is a dependent event or not
     * @return {@code true} if the given event is a dependent event, {@code false} otherwise
     */
    boolean isDependentEvent(ConnectionEvent event);

    /**
     * Convert a list of {@link CommunicationDefinition}s to an immutable map whose keys are the {@code communicationType}
     * and values are the {@link CommunicationDefinition} objects themselves.
     * <p>
     * We store the {@code communicationType} in the elucidation data store, so it is very important that each
     * definition have a unique communication type.
     *
     * @param definitions the definitions to convert
     * @return an immutable map
     */
    static Map<String, CommunicationDefinition> toMap(List<CommunicationDefinition> definitions) {
        return definitions
                .stream()
                .collect(toUnmodifiableMap(CommunicationDefinition::getCommunicationType, def -> def));
    }

    /**
     * A factory method to easily create {@link CommunicationDefinition}s when the event {@link Direction} is the
     * sole characteristic that defines whether a {@link ConnectionEvent} is dependent or not.
     * <p>
     * The returned {@link CommunicationDefinition} will consider {@link ConnectionEvent}s as dependent if their
     * direction matches the given {@code dependentEventDirection}.
     *
     * @param communicationType       the name of the type of communication
     * @param dependentEventDirection the direction for which events will be considered as dependent
     * @return a new {@link CommunicationDefinition}
     */
    static CommunicationDefinition forDependentDirection(String communicationType, Direction dependentEventDirection) {
        return new CommunicationDefinition() {
            @Override
            public String getCommunicationType() {
                return communicationType;
            }

            @Override
            public boolean isDependentEvent(ConnectionEvent event) {
                return dependentEventDirection == event.getEventDirection();
            }
        };
    }

}
