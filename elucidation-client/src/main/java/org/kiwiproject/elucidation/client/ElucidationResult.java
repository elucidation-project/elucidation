package org.kiwiproject.elucidation.client;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Optional;

/**
 * The result of recording a new event, which contains a status and optionally may contain
 * an error message or an exception (but not both).
 */
@SuppressWarnings("WeakerAccess") // it's a library
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ElucidationResult {

    Status status;
    String skipMessage;
    String errorMessage;
    Exception exception;

    /**
     * Create a new result for a successful request to the elucidation server.
     *
     * @return a result indicating that the request completed successfully
     */
    public static ElucidationResult ok() {
        return new ElucidationResult(Status.SUCCESS, null, null, null);
    }

    /**
     * Create a skipped recording result with the given message.
     *
     * @param skipMessage A message indicating why an event was skipped
     * @return a result indicating that the event was skipped and includes the given message
     */
    public static ElucidationResult fromSkipMessage(String skipMessage) {
        return new ElucidationResult(Status.SKIPPED,
                requireNonNull(skipMessage, "skipMessage"),
                null,
                null);
    }

    /**
     * Create an unsuccessful recording result with the given error message.
     *
     * @param errorMessage A message indicating why an event errored during recording
     * @return a result indicating that the event errored during recording and includes the given message
     */
    public static ElucidationResult fromErrorMessage(String errorMessage) {
        return new ElucidationResult(Status.ERROR,
                null,
                requireNonNull(errorMessage, "errorMessage"),
                null);
    }

    /**
     * Create an unsuccessful recording result with the given cause.
     *
     * @param cause An {@link Exception} indicating why an event errored during recording
     * @return a result indicating that the event errored during recording and includes the given cause
     */
    public static ElucidationResult fromException(Exception cause) {
        return new ElucidationResult(Status.ERROR,
                null,
                null,
                requireNonNull(cause, "cause"));
    }

    public boolean hasSkipMessage() {
        return nonNull(skipMessage);
    }

    public Optional<String> getSkipMessage() {
        return Optional.ofNullable(skipMessage);
    }

    public boolean hasErrorMessage() {
        return nonNull(errorMessage);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public boolean hasException() {
        return nonNull(exception);
    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

}
