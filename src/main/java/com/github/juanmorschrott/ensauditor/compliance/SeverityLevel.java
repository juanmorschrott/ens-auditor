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

    /**
     * Parses a severity level from a string, case-insensitive.
     * @param value the string to parse
     * @return the matching SeverityLevel, or null if blank or unknown
     */
    public static SeverityLevel fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getDisplayName() {
        return displayName;
    }
}
