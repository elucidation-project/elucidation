package org.kiwiproject.elucidation.client.helper.dropwizard;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.kiwiproject.elucidation.client.ElucidationClient;
import org.kiwiproject.elucidation.client.ElucidationRecorder;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * An {@link ApplicationEventListener} that can be registered in a Dropwizard application that upon startup will
 * determine all of the registered endpoints and send a request to elucidation to track the endpoints. This will
 * allow elucidation to calculate unused endpoints.
 */
@Slf4j
public class EndpointTrackingListener implements ApplicationEventListener {

    private static final String IDENTIFIER_FORMAT = "%s %s";

    private final DropwizardResourceConfig resourceConfig;
    private final ElucidationClient<String> client;
    private final String serviceName;

    /**
     * Creates a new {@link ApplicationEventListener} to send resource endpoints to elucidation on startup.
     *
     * @param resourceConfig The resource config that will allow for discovering all of the endpoints in the system
     * @param serviceName    The service name of the service attached to the endpoints
     * @param recorder       A preconfigured {@link ElucidationRecorder} needed to send the endpoints to elucidation, the client
     *                       will be constructed from the recorder with a noop factory.
     */
    public EndpointTrackingListener(DropwizardResourceConfig resourceConfig, String serviceName, ElucidationRecorder recorder) {
        this.resourceConfig = resourceConfig;
        this.serviceName = serviceName;
        this.client = ElucidationClient.of(recorder, noop -> Optional.empty());
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() != ApplicationEvent.Type.INITIALIZATION_FINISHED) {
            LOG.info("Return early for event type: {}", event.getType());
            return;
        }

        var path = mergePaths(normalizedContextPath(), urlPattern());

        var endpointIdentifiers = event.getResourceModel().getResources().stream()
                .map(resource -> buildIdentifierFor(resource, path))
                .flatMap(Set::stream)
                .distinct()
                .collect(toList());

        if (!endpointIdentifiers.isEmpty()) {
            client.trackIdentifiers(serviceName, "HTTP", endpointIdentifiers);
        }
    }

    private String normalizedContextPath() {
        var contextPath = resourceConfig.getContextPath();

        if (contextPath.isBlank() || contextPath.equals("/")) {
            return "";
        }

        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    private String urlPattern() {
        var pattern = resourceConfig.getUrlPattern();

        if (pattern.endsWith("/*")) {
            pattern =  pattern.substring(0, pattern.length() - 1);
        }

        return pattern;
    }

    private Set<String> buildIdentifierFor(Resource resource, String contextPath) {
        var identifiers = new HashSet<String>();

        for (var child : resource.getChildResources()) {
            identifiers.addAll(buildIdentifierFor(child, mergePaths(contextPath, resource.getPath())));
        }

        for (var method : resource.getAllMethods()) {
            if ("OPTIONS".equalsIgnoreCase(method.getHttpMethod())) {
                continue;
            }

            var path = mergePaths(contextPath, resource.getPath());
            identifiers.add(format(IDENTIFIER_FORMAT, method.getHttpMethod(), path));
        }

        return identifiers;
    }

    private static String mergePaths(@NotNull String context, String... pathSegments) {
        return Path.of(context, pathSegments).toString();
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        // Nothing to do here but we must implement, the caller handles null
        return null;
    }
}
