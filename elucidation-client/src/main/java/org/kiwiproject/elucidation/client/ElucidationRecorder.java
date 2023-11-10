package org.kiwiproject.elucidation.client;

import static jakarta.ws.rs.client.Entity.json;
import static java.lang.String.format;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response.Status.Family;
import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Abstraction that allows service relationship events to be recorded in the elucidation server.
 */
@SuppressWarnings("WeakerAccess") // it's a library
@Slf4j
public class ElucidationRecorder {

    private static final int DEFAULT_NUM_THREADS = 5;

    private static final String UNSUCCESSFUL_EVENT_RECORDING_RESPONSE_ERROR_TEMPLATE =
            "Unable to record connection event due to a problem communicating with the elucidation server. Status: %s, Body: %s";

    private static final String UNSUCCESSFUL_IDENTIFIER_LOADING_RESPONSE_ERROR_TEMPLATE =
            "Unable to load tracked identifiers due to a problem communicating with the elucidation server. Status: %s, Body: %s";

    private final Client client;
    private final Supplier<String> serverBaseUriSupplier;
    private final ExecutorService executorService;

    /**
     * Creates a new instance of the recorder specifying a given base uri for the elucidation server.
     * <p></p>
     * This constructor will create a new {@link Client} to be used.
     *
     * @param elucidationServerBaseUri The base uri for the elucidation server
     */
    public ElucidationRecorder(String elucidationServerBaseUri) {
        this(ClientBuilder.newClient(), elucidationServerBaseUri);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link Client} and a base uri
     * for the elucidation server.
     *
     * @param client                   A pre-built and configured {@link Client} to be used
     * @param elucidationServerBaseUri The base uri for the elucidation server
     */
    public ElucidationRecorder(Client client, String elucidationServerBaseUri) {
        this(client, () -> elucidationServerBaseUri);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link Client} and a supplier to get
     * the base uri for the elucidation server.
     *
     * @param client                A pre-built and configured {@link Client} to be used
     * @param serverBaseUriSupplier The base uri for the elucidation server
     */
    public ElucidationRecorder(Client client, Supplier<String> serverBaseUriSupplier) {
        this(client, DEFAULT_NUM_THREADS, serverBaseUriSupplier);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link Client} and a supplier to get
     * the base uri for the elucidation server. This will create a new fixed pool {@link ExecutorService} with the
     * given number of threads.
     *
     * @param client                A pre-built and configured {@link Client} to be used
     * @param numThreads            The number of threads to use in the fixed pool {@link ExecutorService}
     * @param serverBaseUriSupplier The base uri for the elucidation server
     */
    public ElucidationRecorder(Client client, int numThreads, Supplier<String> serverBaseUriSupplier) {
        this.client = client;
        this.serverBaseUriSupplier = serverBaseUriSupplier;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("elucidation-recorder-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((thread, exception) ->
                        LOG.error("Thread {} threw an exception that was not handled", thread.getName(), exception))
                .build();
        this.executorService = Executors.newFixedThreadPool(numThreads, threadFactory);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link Client} and a supplier to get
     * the base uri for the elucidation server, and a pre-build {@link ExecutorService}.
     *
     * @param client                A pre-built and configured {@link Client} to be used
     * @param executorService       A pre-build and configured {@link ExecutorService} to be used
     * @param serverBaseUriSupplier The base uri for the elucidation server
     */
    public ElucidationRecorder(Client client, ExecutorService executorService, Supplier<String> serverBaseUriSupplier) {
        this.client = client;
        this.serverBaseUriSupplier = serverBaseUriSupplier;
        this.executorService = executorService;
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     *
     * @param event         The {@link ConnectionEvent} that is being sent
     * @return a future that will return the result of recording a new event
     */
    public CompletableFuture<ElucidationResult> recordNewEvent(ConnectionEvent event) {
        Supplier<ElucidationResult> task = () -> sendEvent(event);
        return CompletableFuture.supplyAsync(task, executorService);
    }

    private ElucidationResult sendEvent(ConnectionEvent event) {
        try {
            var response = client.target(serverBaseUriSupplier.get())
                    .path("/elucidate/event")
                    .request()
                    .post(json(event));

            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                response.close();
                return ElucidationResult.ok();
            }

            var errorEntity = response.readEntity(String.class);
            var errorMessage =
                    format(UNSUCCESSFUL_EVENT_RECORDING_RESPONSE_ERROR_TEMPLATE, response.getStatus(), errorEntity);
            return ElucidationResult.fromErrorMessage(errorMessage);
        } catch (Exception e) {
            return ElucidationResult.fromException(e);
        }
    }

    /**
     * Attempts to send the given identifiers to be tracked for the given service name and given communication type.
     *
     * @param serviceName       The name of the service tied to the identifiers
     * @param communicationType The communication type that are tied to the identifiers (e.g. HTTP or JMS)
     * @param identifiers       The list of identifiers that are to be tracked for usage
     * @return a future that will return the result of loading the identifiers
     */
    public CompletableFuture<ElucidationResult> track(String serviceName, String communicationType, List<String> identifiers) {
        Supplier<ElucidationResult> task = () -> sendIdentifiersToTrack(serviceName, communicationType, identifiers);
        return CompletableFuture.supplyAsync(task, executorService);
    }

    private ElucidationResult sendIdentifiersToTrack(String serviceName, String communicationType, List<String> identifiers) {
        try {
            var response = client.target(serverBaseUriSupplier.get())
                    .path("/elucidate/trackedIdentifier/{serviceName}/{communicationType}")
                    .resolveTemplate("serviceName", serviceName)
                    .resolveTemplate("communicationType", communicationType)
                    .request()
                    .post(json(identifiers));

            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                response.close();
                return ElucidationResult.ok();
            }

            var errorEntity = response.readEntity(String.class);
            var errorMessage =
                    format(UNSUCCESSFUL_IDENTIFIER_LOADING_RESPONSE_ERROR_TEMPLATE, response.getStatus(), errorEntity);
            return ElucidationResult.fromErrorMessage(errorMessage);
        } catch (Exception e) {
            return ElucidationResult.fromException(e);
        }
    }
}
