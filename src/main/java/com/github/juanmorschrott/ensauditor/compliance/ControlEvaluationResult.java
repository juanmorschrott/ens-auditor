package com.github.juanmorschrott.ensauditor.compliance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the evaluation result of a single control.
 * Immutable record optimized for GraalVM native compilation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ControlEvaluationResult(
    String controlId,
    String controlName,
    ControlStatus status,
    SeverityLevel severity,
    String resourceId,
    ResourceType resourceType,
    String findings,
    LocalDateTime evaluatedAt,
    Map<String, Object> metadata
) {
    /**
     * Compact constructor with defaults.
     */
    public ControlEvaluationResult {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * Convenience constructor with minimal fields.
     */
    public ControlEvaluationResult(String controlId, ControlStatus status) {
        this(controlId, null, status, null, null, null, null, null, null);
    }

    /**
     * Convenience constructor for evaluation with resource.
     */
    public ControlEvaluationResult(String controlId, String resourceId, ControlStatus status) {
        this(controlId, null, status, null, resourceId, null, null, null, null);
    }

    /**
     * Convenience constructor for evaluation with findings.
     */
    public ControlEvaluationResult(String controlId, ControlStatus status, String findings) {
        this(controlId, null, status, null, null, null, findings, null, null);
    }
}
