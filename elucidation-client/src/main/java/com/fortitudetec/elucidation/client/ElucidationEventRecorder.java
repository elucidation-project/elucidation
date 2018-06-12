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

import com.fortitudetec.elucidation.client.exception.ElucidationEventRecorderException;
import com.fortitudetec.elucidation.client.model.ConnectionEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.function.Supplier;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.json;

/**
 * Abstraction that allows service relationship events to be recorded in the elucidation server.
 */
public class ElucidationEventRecorder {

    private final Client client;
    private final Supplier<String> serverBaseUriSupplier;

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
     * @param client A pre-built and configured {@link javax.ws.rs.client.Client} to be used
     * @param elucidationServerBaseUri  The base uri for the elucidation server
     */
    public ElucidationEventRecorder(Client client, String elucidationServerBaseUri) {
        this(client, () -> elucidationServerBaseUri);
    }

    /**
     * Creates a new instance of the recorder given a pre-built {@link javax.ws.rs.client.Client} and a supplier to get
     * the base uri for the elucidation server.
     *
     * @param client A pre-built and configured {@link javax.ws.rs.client.Client} to be used
     * @param serverBaseUriSupplier  The base uri for the elucidation server
     */
    public ElucidationEventRecorder(Client client, Supplier<String> serverBaseUriSupplier) {
        this.client = client;
        this.serverBaseUriSupplier = serverBaseUriSupplier;
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     * <br/><br/>
     * The actual call to the server will be done asynchronously.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     */
    public void recordNewEvent(ConnectionEvent event) {
        recordNewEvent(event, RecordingType.ASYNC);
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     * <br/><br/>
     * The actual call to the server will be done synchronously.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     */
    public void recordNewEventSync(ConnectionEvent event) {
        recordNewEvent(event, RecordingType.SYNC);
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     * @param recordingType determines if the call should be made asynchronously or not
     */
    public void recordNewEvent(ConnectionEvent event, RecordingType recordingType) {
        Runnable task = () -> sendEvent(event);

        if (recordingType == RecordingType.ASYNC) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }

    private void sendEvent(ConnectionEvent event) {
        Response response = client.target(serverBaseUriSupplier.get())
            .request()
            .post(json(event));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new ElucidationEventRecorderException(
                format("Unable to record connection event due to a problem talking to the elucidation server. Status: %s, Body: %s",
                    response.getStatus(), response.readEntity(String.class)));
        }
    }

}
