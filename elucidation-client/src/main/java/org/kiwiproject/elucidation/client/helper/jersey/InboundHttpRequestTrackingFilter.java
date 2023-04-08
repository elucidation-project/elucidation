package org.kiwiproject.elucidation.client.helper.jersey;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.kiwiproject.elucidation.client.ElucidationClient;
import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.common.definition.CommunicationDefinition;
import org.kiwiproject.elucidation.common.definition.HttpCommunicationDefinition;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import java.util.Optional;

/**
 * Helper to aid in recording INBOUND connection events for HTTP requests. If this filter is registered, then there is
 * no need to record events inside of the individual resource endpoints.
 *
 * <p>
 * This filter can be registered in your jersey app, like so (note: this example is using Dropwizard):
 * <p>
 * <code>
 *     environment.jersey().register(new InboundHttpRequestTrackingFilter("dummy-service", client, new HttpCommunicationDefinition()));
 * </code>
 *
 * <p>
 * By setting the {@code originatingServiceHeaderName}, you don't need to (and should not) record an OUTBOUND event at the
 * originating service, which can significantly cut down on the amount of code needed to record HTTP events.
 */
public class InboundHttpRequestTrackingFilter implements ContainerRequestFilter {

    /**
     * A suggested name for the header representing the originating service.
     * Using this can ensure consistency across your services, and make it less
     * likely there is a typo or mistake specifying the header name.
     */
    @SuppressWarnings("unused") // This is a library and is provided for users of the library
    public static final String ELUCIDATION_ORIGINATING_SERVICE_HEADER = "Elucidation-Originating-Service";

    private static final String IDENTIFIER_FORMAT = "%s %s";

    @Context
    private ResourceInfo resourceInfo;

    private final ElucidationClient<ConnectionEvent> elucidationClient;
    private final CommunicationDefinition communicationDefinition;
    private final String serviceName;
    private final String originatingServiceHeaderName;

    /**
     * Constructs a new {@link ContainerRequestFilter}. This constructor will NOT enable OUTBOUND event tracking. This constructor
     * will default the communicationDefinition to the {@link HttpCommunicationDefinition}.
     *
     * @param serviceName                   The service name that will be used for recording events
     * @param recorder                      A preconfigured {@link ElucidationRecorder} used to send the events to elucidation
     */
    public InboundHttpRequestTrackingFilter(String serviceName, ElucidationRecorder recorder) {
        this(serviceName, recorder, new HttpCommunicationDefinition());
    }

    /**
     * Constructs a new {@link ContainerRequestFilter}. This constructor will NOT enable OUTBOUND event tracking.
     *
     * @param serviceName                   The service name that will be used for recording events
     * @param recorder                      A preconfigured {@link ElucidationRecorder} used to send the events to elucidation
     * @param communicationDefinition       A {@link CommunicationDefinition} instance that will be used to add the {@code communicationType} to the events
     */
    public InboundHttpRequestTrackingFilter(String serviceName, ElucidationRecorder recorder, CommunicationDefinition communicationDefinition) {
        this(serviceName, recorder, communicationDefinition, null);
    }

    /**
     * Constructs a new {@link ContainerRequestFilter}, optionally setting up the ability to record accompanying Outbound events. This constructor
     * will default the communicationDefinition to the {@link HttpCommunicationDefinition}.
     *
     * @param serviceName                   The service name that will be used for recording events
     * @param recorder                      A preconfigured {@link ElucidationRecorder} used to send the events to elucidation
     * @param originatingServiceHeaderName  An optional header key name that if set will trigger OUTBOUND events to be recorded also
     */
    public InboundHttpRequestTrackingFilter(String serviceName, ElucidationRecorder recorder, String originatingServiceHeaderName) {
        this(serviceName, recorder, new HttpCommunicationDefinition(), originatingServiceHeaderName);
    }

    /**
     * Constructs a new {@link ContainerRequestFilter}, optionally setting up the ability to record accompanying Outbound events
     *
     * @param serviceName                   The service name that will be used for recording events
     * @param recorder                      A preconfigured {@link ElucidationRecorder} used to send the events to elucidation
     * @param communicationDefinition       A {@link CommunicationDefinition} instance that will be used to add the {@code communicationType} to the events
     * @param originatingServiceHeaderName  An optional header key name that if set will trigger OUTBOUND events to be recorded also
     */
    public InboundHttpRequestTrackingFilter(String serviceName, ElucidationRecorder recorder, CommunicationDefinition communicationDefinition, String originatingServiceHeaderName) {
        this.serviceName = serviceName;
        this.communicationDefinition = communicationDefinition;
        this.originatingServiceHeaderName = originatingServiceHeaderName;
        this.elucidationClient = ElucidationClient.of(recorder, Optional::of);
    }

    @Override
    public void filter(ContainerRequestContext context) {
        var method = context.getMethod();
        var classBasePath = resourceInfo.getResourceClass().getAnnotation(Path.class).value();
        var methodPath = optionalMethodPath().orElse("");

        var fullPath = java.nio.file.Path.of(classBasePath, methodPath);

        var identifier = format(IDENTIFIER_FORMAT, method, fullPath);

        recordOutboundEventIfNecessary(identifier, context);

        elucidationClient.recordNewEvent(ConnectionEvent.builder()
                .serviceName(serviceName)
                .communicationType(communicationDefinition.getCommunicationType())
                .eventDirection(Direction.INBOUND)
                .connectionIdentifier(identifier)
                .build());
    }

    private Optional<String> optionalMethodPath() {
        return Optional.ofNullable(resourceInfo.getResourceMethod().getAnnotation(Path.class)).map(Path::value);
    }

    private void recordOutboundEventIfNecessary(String identifier, ContainerRequestContext context) {
        if (isNotBlank(originatingServiceHeaderName)) {
            var originatingServiceName = context.getHeaderString(originatingServiceHeaderName);

            if (isNotBlank(originatingServiceName)) {
                elucidationClient.recordNewEvent(ConnectionEvent.builder()
                        .serviceName(originatingServiceName)
                        .communicationType(communicationDefinition.getCommunicationType())
                        .eventDirection(Direction.OUTBOUND)
                        .connectionIdentifier(identifier)
                        .build());
            }
        }
    }
}
