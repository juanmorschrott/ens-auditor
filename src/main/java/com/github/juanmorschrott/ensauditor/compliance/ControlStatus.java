package com.github.juanmorschrott.ensauditor.compliance;

/**
 * Represents the evaluation result of a control.
 */
public enum ControlStatus {
    /**
     * Control is fully compliant.
     */
    COMPLIANT("COMPLIANT"),
    
    /**
     * Control is not compliant or configuration issue found.
     */
    NON_COMPLIANT("NON_COMPLIANT"),
    
    /**
     * Manual verification required - cannot be fully automated.
     */
    MANUAL_VERIFICATION_REQUIRED("MANUAL_VERIFICATION_REQUIRED"),
    
    /**
     * Control cannot be evaluated (resource not found, permission denied, etc).
     */
    NOT_EVALUATED("NOT_EVALUATED");

    private final String displayName;

    ControlStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
