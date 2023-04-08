package org.kiwiproject.elucidation.client;

/**
 * The possible status values for request attempts to the elucidation server.
 */
public enum Status {

    /**
     * The request was successful.
     */
    SUCCESS,

    /**
     * There was some error making the request to the elucidation server.
     */
    ERROR,

    /**
     * Request was skipped (this is mostly like to due to elucidation being disabled).
     */
    SKIPPED

}
