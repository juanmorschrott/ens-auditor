package com.github.juanmorschrott.ensauditor.compliance;

import java.util.List;
import java.util.function.BiConsumer;

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
    default AuditResult evaluateControls(List<ControlDefinition> controls) {
        return evaluateControls(controls, (current, total) -> {});
    }

    /**
     * Evaluates multiple controls, reporting progress via callback.
     * @param controls list of control definitions to evaluate
     * @param onProgress called after each control with (completed, total)
     * @return complete audit result with evaluation results and compliance levels
     */
    AuditResult evaluateControls(List<ControlDefinition> controls, BiConsumer<Integer, Integer> onProgress);
}
