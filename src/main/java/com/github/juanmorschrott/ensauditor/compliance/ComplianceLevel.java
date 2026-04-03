package com.github.juanmorschrott.ensauditor.compliance;

/**
 * Represents the overall compliance level of an ENS module or pillar.
 */
public enum ComplianceLevel {
    /**
     * 0-25% of controls are compliant.
     */
    CRITICAL("CRITICAL"),
    
    /**
     * 25-50% of controls are compliant.
     */
    HIGH("HIGH"),
    
    /**
     * 50-75% of controls are compliant.
     */
    MEDIUM("MEDIUM"),
    
    /**
     * 75-100% of controls are compliant.
     */
    LOW("LOW"),
    
    /**
     * All controls are compliant.
     */
    COMPLIANT("COMPLIANT");

    private final String displayName;

    ComplianceLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ComplianceLevel fromCompliancePercentage(double percentage) {
        if (percentage >= 0.95) return COMPLIANT;
        if (percentage >= 0.75) return LOW;
        if (percentage >= 0.50) return MEDIUM;
        if (percentage >= 0.25) return HIGH;
        return CRITICAL;
    }
}
