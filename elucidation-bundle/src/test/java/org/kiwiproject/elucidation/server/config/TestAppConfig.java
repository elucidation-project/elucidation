package org.kiwiproject.elucidation.server.config;

import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.JmsCommunicationDefinition;
import org.kiwiproject.elucidation.common.model.Direction;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
class TestAppConfig extends Configuration {

    private Duration customTtl = Duration.days(14);

    private List<CommunicationDefinition> communicationDefinitions = customCommunicationDefinitions();

    private static List<CommunicationDefinition> customCommunicationDefinitions() {
        var rest = CommunicationDefinition.forDependentDirection("REST", Direction.OUTBOUND);
        var rabbitMq = CommunicationDefinition.forDependentDirection("RabbitMQ", Direction.INBOUND);
        var jms = new JmsCommunicationDefinition();

        return List.of(rest, rabbitMq, jms);
    }

}
