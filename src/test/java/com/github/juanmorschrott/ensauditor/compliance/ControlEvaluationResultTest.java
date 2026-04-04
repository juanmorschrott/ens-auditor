package com.github.juanmorschrott.ensauditor.compliance;

import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ControlEvaluationResult record.
 */
class ControlEvaluationResultTest {

    @Test
    void testRecordConstruction() {
        ControlEvaluationResult result = new ControlEvaluationResult(
                "C1.1", 
                "Test Control", 
                "mp.si", 
                ControlStatus.COMPLIANT, 
                SeverityLevel.HIGH, 
                "my-bucket", 
                ResourceType.S3_BUCKET, 
                "Configuration is compliant", 
                null, 
                null
        );

        assertEquals("C1.1", result.controlId());
        assertEquals("Test Control", result.controlName());
        assertEquals(ControlStatus.COMPLIANT, result.status());
        assertEquals(SeverityLevel.HIGH, result.severity());
        assertEquals("my-bucket", result.resourceId());
        assertEquals(ResourceType.S3_BUCKET, result.resourceType());
    }

    @Test
    void testConvenienceConstructor() {
        ControlEvaluationResult result = new ControlEvaluationResult("C1.1", ControlStatus.COMPLIANT);

        assertEquals("C1.1", result.controlId());
        assertEquals(ControlStatus.COMPLIANT, result.status());
        assertNotNull(result.evaluatedAt());
        assertNotNull(result.metadata());
    }

    @Test
    void testConvenienceConstructorWithResource() {
        ControlEvaluationResult result = new ControlEvaluationResult("C1.1", "my-bucket", ControlStatus.COMPLIANT);

        assertEquals("C1.1", result.controlId());
        assertEquals("my-bucket", result.resourceId());
        assertEquals(ControlStatus.COMPLIANT, result.status());
    }

    @Test
    void testRecordImmutability() {
        ControlEvaluationResult result = new ControlEvaluationResult("C1.1", ControlStatus.COMPLIANT);

        // Records are immutable - these would fail at compile time if attempting direct assignment
        assertEquals("C1.1", result.controlId());
        assertEquals(ControlStatus.COMPLIANT, result.status());
    }
}
