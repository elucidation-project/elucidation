package org.kiwiproject.elucidation.client.exception;

@SuppressWarnings("unused")
public class ElucidationEventRecorderException extends RuntimeException {
    public ElucidationEventRecorderException() {
    }

    public ElucidationEventRecorderException(String message) {
        super(message);
    }

    public ElucidationEventRecorderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ElucidationEventRecorderException(Throwable cause) {
        super(cause);
    }
}
