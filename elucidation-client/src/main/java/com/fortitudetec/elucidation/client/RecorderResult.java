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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * The result of recording a new event, which contains a status and optionally may contain
 * an error message or an exception (but not both).
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecorderResult {

    private RecordingStatus status;
    private String skipMessage;
    private String errorMessage;
    private Exception exception;

    /**
     * Create a new result for a successful recording.
     */
    public static RecorderResult ok() {
        return new RecorderResult(RecordingStatus.RECORDED_OK, null, null, null);
    }

    /**
     * Create a skipped recording result with the given message.
     */
    public static RecorderResult fromSkipMessage(String skipMessage) {
        return new RecorderResult(RecordingStatus.SKIPPED_RECORDING,
                requireNonNull(skipMessage, "skipMessage"),
                null,
                null);
    }

    /**
     * Create an unsuccessful recording result with the given error message.
     */
    public static RecorderResult fromErrorMessage(String errorMessage) {
        return new RecorderResult(RecordingStatus.ERROR_RECORDING,
                null,
                requireNonNull(errorMessage, "errorMessage"),
                null);
    }

    /**
     * Create an unsuccessful recording result with the given cause.
     */
    public static RecorderResult fromException(Exception cause) {
        return new RecorderResult(RecordingStatus.ERROR_RECORDING,
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
