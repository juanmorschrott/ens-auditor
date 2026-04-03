package com.github.juanmorschrott.ensauditor.compliance;

/**
 * Represents the severity level of a control's non-compliance.
 */
public enum SeverityLevel {
    /**
     * Critical severity - immediate action required.
     */
    CRITICAL("CRITICAL"),
    
    /**
     * High severity - significant risk.
     */
    HIGH("HIGH"),
    
    /**
     * Medium severity - moderate risk.
     */
    MEDIUM("MEDIUM"),
    
    /**
     * Low severity - minor risk.
     */
    LOW("LOW");

    private final String displayName;

    SeverityLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
