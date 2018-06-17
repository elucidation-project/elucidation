package com.fortitudetec.elucidation.client;

/*-
 * #%L
 * Elucidation Client
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
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

import com.fortitudetec.elucidation.client.model.ConnectionEvent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.json;

/**
 * Abstraction that allows service relationship events to be recorded in the elucidation server.
 */
@Slf4j
public class ElucidationEventRecorder {

    // TODO Make configurable and/or allow user to supply an ExecutorService?
    private static final int NUM_THREADS = 5;

    private static final String UNSUCCESSFUL_RESPONSE_ERROR_TEMPLATE =
            "Unable to record connection event due to a problem communicating with the elucidation server. Status: %s, Body: %s";

    private final Client client;
    private final Supplier<String> serverBaseUriSupplier;
    private final ExecutorService executorService;

    public enum RecordingType {
        ASYNC, SYNC
    }

    /**
     * Creates a new instance of the recorder specifying a given base uri for the elucidation server.
     * <br/><br/>
     * This constructor will create a new {@link javax.ws.rs.client.Client} to be used.
     *
     * @param elucidationServerBaseUri The base uri for the elucidation server
     */
    public ElucidationEventRecorder(String elucidationServerBaseUri) {
        this(ClientBuilder.newClient(), elucidationServerBaseUri);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link javax.ws.rs.client.Client} and a base uri
     * for the elucidation server.
     *
     * @param client                   A pre-built and configured {@link javax.ws.rs.client.Client} to be used
     * @param elucidationServerBaseUri The base uri for the elucidation server
     */
    public ElucidationEventRecorder(Client client, String elucidationServerBaseUri) {
        this(client, () -> elucidationServerBaseUri);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link javax.ws.rs.client.Client} and a supplier to get
     * the base uri for the elucidation server.
     *
     * @param client                A pre-built and configured {@link javax.ws.rs.client.Client} to be used
     * @param serverBaseUriSupplier The base uri for the elucidation server
     */
    public ElucidationEventRecorder(Client client, Supplier<String> serverBaseUriSupplier) {
        this.client = client;
        this.serverBaseUriSupplier = serverBaseUriSupplier;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("elucidation-recorder-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((thread, exception) ->
                        LOG.error("Thread {} threw an exception that was not handled", thread.getName(), exception))
                .build();
        this.executorService = Executors.newFixedThreadPool(NUM_THREADS, threadFactory);
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     * <br/><br/>
     * The actual call to the server will be done asynchronously.
     *
     * @param event The {@link ConnectionEvent} that is being sent
     */
    public CompletableFuture<RecorderResult> recordNewEvent(ConnectionEvent event) {
        return recordNewEvent(event, RecordingType.ASYNC);
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     * <br/><br/>
     * The actual call to the server will be done synchronously.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     */
    public RecorderResult recordNewEventSync(ConnectionEvent event) {
        try {
            return recordNewEvent(event, RecordingType.SYNC).get();
        } catch (InterruptedException | ExecutionException e) {
            return RecorderResult.fromException(e);
        }
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     *
     * @param event         The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     * @param recordingType determines if the call should be made asynchronously or not
     */
    public CompletableFuture<RecorderResult> recordNewEvent(ConnectionEvent event, RecordingType recordingType) {
        if (recordingType == RecordingType.ASYNC) {
            Supplier<RecorderResult> task = () -> sendEvent(event);
            return CompletableFuture.supplyAsync(task, executorService);
        } else {
            return CompletableFuture.completedFuture(sendEvent(event));
        }
    }

    private RecorderResult sendEvent(ConnectionEvent event) {
        try {
            Response response = client.target(serverBaseUriSupplier.get())
                    .request()
                    .post(json(event));

            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                response.close();
                return RecorderResult.ok();
            }
            
            String errorEntity = response.readEntity(String.class);
            String errorMessage =
                    format(UNSUCCESSFUL_RESPONSE_ERROR_TEMPLATE, response.getStatus(), errorEntity);
            return RecorderResult.fromErrorMessage(errorMessage);            
        } catch (Exception e) {
            return RecorderResult.fromException(e);
        }
    }

}
