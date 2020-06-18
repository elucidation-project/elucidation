package com.fortitudetec.elucidation.client.helper.dropwizard;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.fortitudetec.elucidation.client.ElucidationClient;
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
import java.util.Set;

@Slf4j
public class EndpointTrackingListener<T> implements ApplicationEventListener {

    private static final String IDENTIFIER_FORMAT = "%s %s";

    private final DropwizardResourceConfig resourceConfig;
    private final ElucidationClient<T> client;
    private final String serviceName;

    public EndpointTrackingListener(DropwizardResourceConfig resourceConfig, String serviceName, ElucidationClient<T> client) {
        this.resourceConfig = resourceConfig;
        this.client = client;
        this.serviceName = serviceName;
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

        client.trackIdentifiers(serviceName, "HTTP", endpointIdentifiers);
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
