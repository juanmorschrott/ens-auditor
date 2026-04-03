package com.github.juanmorschrott.ensauditor.compliance;

import java.util.List;

/**
 * Public API for the compliance module.
 * Coordinates evaluation of ENS controls across different AWS resource types.
 */
public interface ComplianceService {
    /**
     * Evaluates multiple controls in batch.
     * @param controls list of control definitions to evaluate
     * @return list of evaluation results
     */
    List<ControlEvaluationResult> evaluateControls(List<ControlDefinition> controls);
}
