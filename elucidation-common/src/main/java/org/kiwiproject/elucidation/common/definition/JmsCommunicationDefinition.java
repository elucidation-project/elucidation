package org.kiwiproject.elucidation.common.definition;

public class JmsCommunicationDefinition implements AsyncMessagingCommunicationDefinition {

    @Override
    public String getCommunicationType() {
        return "JMS";
    }
}
