package com.github.juanmorschrott.ensauditor.aws;

import com.github.juanmorschrott.ensauditor.shared.exception.AuditException;

/**
 * Exception thrown when AWS resources cannot be fetched.
 */
public class ResourceFetchException extends AuditException {
    public ResourceFetchException(String message) {
        super(message);
    }

    public ResourceFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
