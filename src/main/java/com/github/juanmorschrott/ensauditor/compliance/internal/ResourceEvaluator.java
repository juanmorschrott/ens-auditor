package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;

/**
 * Base interface for resource-specific evaluators.
 * Each evaluator is responsible for evaluating controls for a specific resource type.
 */
public interface ResourceEvaluator {
    /**
     * Evaluates a control for this resource type.
     * @param control the control definition to evaluate
     * @return evaluation result
     */
    ControlEvaluationResult evaluate(ControlDefinition control);

    /**
     * Returns the resource type this evaluator handles.
     * @return the resource type
     */
    ResourceType getResourceType();

    /**
     * Returns a name for this evaluator (for logging/debugging).
     * @return evaluator name
     */
    String getName();
}
