package org.kiwiproject.elucidation.server.test;

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
