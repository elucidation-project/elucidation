package com.fortitudetec.elucidation.client;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.exception.ElucidationEventRecorderExeception;
import com.fortitudetec.elucidation.client.model.ConnectionEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

/**
 * Abstraction that allows service relationship events to be recorded in the elucidation server.
 */
public class ElucidationEventRecorder {

    private final Client client;
    private final String elucidationServerBaseUri;

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
        this.client = client;
        this.elucidationServerBaseUri = elucidationServerBaseUri;
    }


    /**
     * Attempts to send the given connection event to the elucidation server.
     * <br/><br/>
     * The actual call to the server will be done asynchronously.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     */
    public void recordNewEvent(ConnectionEvent event) {
        recordNewEvent(event, true);
    }

    /**
     * Attempts to send the given connection event to the elucidation server.
     *
     * @param event The {@link com.fortitudetec.elucidation.client.model.ConnectionEvent} that is being sent
     * @param useAsync determines if the call should be made asynchronously or not
     */
    public void recordNewEvent(ConnectionEvent event, boolean useAsync) {
        Runnable task = () -> sendEvent(event);

        if (useAsync) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }

    private void sendEvent(ConnectionEvent event) {
        Response response = client.target(elucidationServerBaseUri)
            .request()
            .post(json(event));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new ElucidationEventRecorderExeception(
                format("Unable to record connection event due to a problem talking to the elucidation server. Status: %s, Body: %s",
                    response.getStatus(), response.readEntity(String.class)));
        }
    }

}
