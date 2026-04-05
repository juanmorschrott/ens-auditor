package com.github.juanmorschrott.ensauditor.compliance;

import java.util.List;

/**
 * Public API for the compliance module.
 * Coordinates evaluation of ENS controls across different AWS resource types.
 */
public interface ComplianceService {
    /**
     * Evaluates multiple controls and calculates compliance levels.
     * @param controls list of control definitions to evaluate
     * @return complete audit result with evaluation results and compliance levels
     */
    AuditResult evaluateControls(List<ControlDefinition> controls);
}
