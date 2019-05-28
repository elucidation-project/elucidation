package com.fortitudetec.elucidation.server.config;

import io.dropwizard.util.Duration;

public interface ElucidationConfiguration<T> {
    default Duration getTimeToLive(T configuration) {
        return Duration.days(7);
    }
}
