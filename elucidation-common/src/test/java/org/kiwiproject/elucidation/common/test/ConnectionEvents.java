package org.kiwiproject.elucidation.common.test;

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

import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ConnectionEvents {

    public static ConnectionEvent newConnectionEvent(String serviceName, Direction direction, String identifier) {
        return newConnectionEvent(ThreadLocalRandom.current().nextLong(), serviceName, direction, identifier);
    }

    public static ConnectionEvent newConnectionEvent(String serviceName, Direction direction, String identifier, long observedAt) {
        return newConnectionEvent(ThreadLocalRandom.current().nextLong(), serviceName, direction, identifier, observedAt);
    }

    public static ConnectionEvent newConnectionEvent(Long id, String serviceName, Direction direction, String identifier) {
        return newConnectionEvent(id, serviceName, direction, identifier, System.currentTimeMillis());
    }

    public static ConnectionEvent newConnectionEvent(Long id, String serviceName, Direction direction, String identifier, long observedAt) {
        return ConnectionEvent.builder()
                .serviceName(serviceName)
                .communicationType("JMS")
                .eventDirection(direction)
                .connectionIdentifier(identifier)
                .observedAt(observedAt)
                .id(id)
                .build();
    }

}