package com.ayushgarg.docqa.exception;

/**
 * Raised when an upstream dependency fails (the AI chat API or the
 * database/vector store). Mapped to HTTP 502 by the global error handler.
 */
public class UpstreamException extends RuntimeException {

    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
