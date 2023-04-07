package org.kiwiproject.elucidation.server.config;

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

import static java.util.stream.Collectors.toList;

import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.HttpCommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.JmsCommunicationDefinition;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Defines Elucidation configuration properties.
 *
 * @param <T> the type of configuration; a subclass of {@link io.dropwizard.Configuration}
 */
public interface ElucidationConfiguration<T extends Configuration> {

    /**
     * How long should recorded events live before they are automatically deleted? (default: 7 days)
     *
     * @param configuration the Configuration, which can optionally be used to obtain a custom TTL
     * @return the TTL duration
     */
    @NotNull
    default Duration getTimeToLive(T configuration) {
        return Duration.days(7);
    }

    /**
     * The list of communication definitions that define different types of communications (HTTP, JMS, Kafka, etc.)
     * <p>
     * <strong>NOTE:</strong>
     * These should have a unique {@link CommunicationDefinition#getCommunicationType()}, otherwise the resulting
     * behavior will be non-deterministic when they are converted to a map whose keys are the communication type.
     *
     * @param configuration the Configuration, which can optionally be used to obtain custom {@link CommunicationDefinition}s
     * @return a list of {@link CommunicationDefinition}s
     * @see #defaultCommunicationDefinitions()
     */
    @NotEmpty
    default List<CommunicationDefinition> getCommunicationDefinitions(T configuration) {
        return defaultCommunicationDefinitions();
    }

    /**
     * Return an immutable list containing the default {@link CommunicationDefinition}s.
     *
     * @return an immutable list of default communication definitions
     */
    static List<CommunicationDefinition> defaultCommunicationDefinitions() {
        return List.of(new HttpCommunicationDefinition(), new JmsCommunicationDefinition());
    }

    /**
     * Return an immutable list containing the default and additional {@link CommunicationDefinition}s.
     * <p>
     * This method will be useful if you want to define a custom configuration in which the defaults are included
     * plus some custom definitions.
     *
     * @param additionalDefinitions definitions to include in addition to the default ones
     * @return immutable list
     */
    static List<CommunicationDefinition> defaultDefinitionsAnd(CommunicationDefinition... additionalDefinitions) {
        List<CommunicationDefinition> combinedDefs = new ArrayList<>();

        combinedDefs.addAll(defaultCommunicationDefinitions());
        combinedDefs.addAll(Arrays.stream(additionalDefinitions).collect(toList()));

        return List.copyOf(combinedDefs);
    }

    /**
     * Returns the polling config object from the main configuration.  If empty, polling will be disabled.
     *
     * @param configuration the Configuration, which can optionally be used to obtain custom {@link CommunicationDefinition}s
     * @return An optional containing the polling config
     */
    default Optional<PollingConfig> getPollingConfig(T configuration) {
        return Optional.empty();
    }

    /**
     * Determines whether or not the polling should execute.  By default this is determined by the existence of
     * a {@link PollingConfig} in the Optional returned by {@link #getPollingConfig(Configuration)}.
     *
     * @param configuration the Configuration, which can optionally be used to obtain custom {@link CommunicationDefinition}s
     * @return true if polling is configured, false if polling should be turned off
     * @see #getPollingConfig(Configuration)
     */
    default boolean shouldPoll(T configuration) {
        return getPollingConfig(configuration).isPresent();
    }

    /**
     * Gets a supplier to return the endpoint for the elucidation service to poll for events.  By default this will used the
     * static endpoint config value.
     *
     * @param configuration the Configuration, which can optionally be used to obtain custom {@link CommunicationDefinition}s
     * @return A supplier that returns the endpoint to use for polling
     */
    default Supplier<String> getPollEndpointSupplier(T configuration) {
        return () -> getPollingConfig(configuration).orElseThrow().getPollingEndpoint();
    }

    /**
     * Whether to register the JDBI exception mappers or not. Default is true.
     *
     * @return true to register the JDBI exception mappers; false otherwise
     */
    default boolean isRegisterJdbiExceptionMappers(T configuration) {
        return true;
    }

    /**
     * Whether CORS should be enabled for the elucidation endpoints. Default is true.
     * @return true to enable CORS; false otherwise
     */
    default boolean isCorsEnabled(T configuration) {
        return true;
    }

    /**
     * The path to use for the CORS configuration. Defaults to /elucidate/* so that implementors don't have to expose
     * all of their endpoints to CORS.
     * @return The path to lockdown CORS access to.
     */
    default String corsPath(T configuration) {
        return "/elucidate/*";
    }
}
