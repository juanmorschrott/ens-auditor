package com.github.juanmorschrott.ensauditor.compliance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ComplianceLevel enum.
 */
class ComplianceLevelTest {

    @Test
    void testComplianceLevelCalculation() {
        assertEquals(ComplianceLevel.COMPLIANT, ComplianceLevel.fromCompliancePercentage(0.95));
        assertEquals(ComplianceLevel.LOW, ComplianceLevel.fromCompliancePercentage(0.80));
        assertEquals(ComplianceLevel.MEDIUM, ComplianceLevel.fromCompliancePercentage(0.60));
        assertEquals(ComplianceLevel.HIGH, ComplianceLevel.fromCompliancePercentage(0.40));
        assertEquals(ComplianceLevel.CRITICAL, ComplianceLevel.fromCompliancePercentage(0.20));
    }

    @Test
    void testEdgeCases() {
        assertEquals(ComplianceLevel.CRITICAL, ComplianceLevel.fromCompliancePercentage(0.0));
        assertEquals(ComplianceLevel.COMPLIANT, ComplianceLevel.fromCompliancePercentage(1.0));
        assertEquals(ComplianceLevel.LOW, ComplianceLevel.fromCompliancePercentage(0.75));
    }
}
