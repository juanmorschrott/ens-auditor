package com.github.juanmorschrott.ensauditor.shared.exception;

/**
 * Base exception for ENS Auditor operations.
 */
public class AuditException extends RuntimeException {

    public AuditException(String message) {
        super(message);
    }

    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
