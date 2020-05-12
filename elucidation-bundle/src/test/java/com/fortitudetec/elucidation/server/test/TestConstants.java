package com.fortitudetec.elucidation.server.test;

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

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestConstants {

    public static final String COMMUNICATION_TYPE_FIELD = "communicationType";
    public static final String CONNECTION_IDENTIFIER_FIELD = "connectionIdentifier";
    public static final String DEPENDENCIES_FIELD = "dependencies";
    public static final String EVENT_DIRECTION_FIELD = "eventDirection";
    public static final String HAS_INBOUND_FIELD = "hasInbound";
    public static final String HAS_OUTBOUND_FIELD = "hasOutbound";
    public static final String SERVICE_NAME_FIELD = "serviceName";

    public static final String A_SERVICE_NAME = "test-service";
    public static final String ANOTHER_SERVICE_NAME = "another-service-1";
    public static final String YET_ANOTHER_SERVICE_NAME = "another-service-2";

    public static final String MSG_FROM_ANOTHER_SERVICE = "MSG_FROM_ANOTHER_SERVICE";
    public static final String MSG_TO_ANOTHER_SERVICE = "MSG_TO_ANOTHER_SERVICE";
    public static final String IGNORED_MSG = "MSG_NO_ONE_LISTENS_TO";

}
