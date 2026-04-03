package com.github.juanmorschrott.ensauditor.compliance;

import com.github.juanmorschrott.ensauditor.shared.exception.AuditException;

/**
 * Exception thrown when evaluation of a control fails.
 */
public class ControlEvaluationException extends AuditException {
    public ControlEvaluationException(String message) {
        super(message);
    }

    public ControlEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
