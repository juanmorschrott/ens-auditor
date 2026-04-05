package com.github.juanmorschrott.ensauditor.shared.exception;

/**
 * Exception thrown when ENS control mapping configuration is invalid.
 */
public class InvalidConfigurationException extends AuditException {

    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
