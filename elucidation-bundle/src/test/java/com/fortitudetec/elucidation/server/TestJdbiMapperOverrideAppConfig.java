package com.fortitudetec.elucidation.server;

import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * Test class to support {@link ElucidationBundleTest}
 */
@Getter
@Setter
class TestJdbiMapperOverrideAppConfig extends Configuration {

    private boolean registerJdbiExceptionMappers;

}
