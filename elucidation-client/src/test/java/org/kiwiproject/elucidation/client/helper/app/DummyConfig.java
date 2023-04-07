package org.kiwiproject.elucidation.client.helper.app;

import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DummyConfig extends Configuration {

    private String originatingHeaderName;

    private boolean loadDummyResource;
}
