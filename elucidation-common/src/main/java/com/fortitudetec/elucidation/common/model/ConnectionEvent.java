package com.fortitudetec.elucidation.common.model;

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

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

/**
 * A representation of an observed connection within a given service.
 */
@Builder
@Value
public class ConnectionEvent {

    public static final String UNKNOWN_SERVICE = "unknown-service";

    @Wither
    private Long id;

    /**
     * A name of the service where the connection was observed
     */
    @NotBlank
    private String serviceName;

    /**
     * The direction that the event was observed. INBOUND or OUTBOUND
     */
    @NotNull
    private Direction eventDirection;

    /**
     * The method of communication that was observed. For example, "HTTP" or "JMS".
     *
     * @see com.fortitudetec.elucidation.common.definition.CommunicationDefinition
     */
    @NotBlank
    private String communicationType;

    /**
     * A unique identifier for the connection (i.e. REST endpoint path or JMS Message Type)
     */
    @NotBlank
    private String connectionIdentifier;

    /**
     * The date/time the connection was observed (in milliseconds since EPOCH)
     */
    @Builder.Default
    private long observedAt = System.currentTimeMillis();

}
