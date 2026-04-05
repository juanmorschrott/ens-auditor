package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Orchestrates evaluation of controls using specialized evaluators.
 * Routes control evaluation requests to appropriate resource evaluators.
 */
@Service
class ComplianceOrchestrator implements ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceOrchestrator.class);

    private final Map<ResourceType, ResourceEvaluator> evaluatorsByType;

    public ComplianceOrchestrator(List<ResourceEvaluator> evaluators) {
        this.evaluatorsByType = evaluators.stream()
                .collect(Collectors.toMap(
                        ResourceEvaluator::getResourceType,
                        evaluator -> evaluator,
                        (existing, replacement) -> existing
                ));
        log.info("Initialized {} resource evaluators", evaluatorsByType.size());
    }

    @Override
    public AuditResult evaluateControls(List<ControlDefinition> controls, BiConsumer<Integer, Integer> onProgress) {
        int total = controls.size();
        AuditResult auditResult = new AuditResult();

        for (int i = 0; i < total; i++) {
            ControlDefinition control = controls.get(i);
            auditResult.addControlResult(this.evaluateControl(control));
            onProgress.accept(i + 1, total);
        }

        auditResult.calculateComplianceLevels();
        return auditResult;
    }

    private ControlEvaluationResult evaluateControl(ControlDefinition control) {
        try {
            if (control.affectedResourceTypes() == null || control.affectedResourceTypes().isEmpty()) {
                return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED);
            }

            // Find first applicable evaluator by resolving resource type identifiers
            for (String resourceTypeId : control.affectedResourceTypes()) {
                ResourceType type = ResourceType.fromIdentifier(resourceTypeId);
                if (type == null) {
                    log.debug("Unknown resource type identifier: {}", resourceTypeId);
                    continue;
                }

                ResourceEvaluator evaluator = evaluatorsByType.get(type);
                if (evaluator != null) {
                    log.debug("Evaluating control {} with {}", control.controlId(), evaluator.getName());
                    ControlEvaluationResult raw = evaluator.evaluate(control);
                    return new ControlEvaluationResult(
                            control.controlId(),
                            control.name(),
                            control.module(),
                            raw.status(),
                            control.severity(),
                            raw.resourceId(),
                            type,
                            raw.findings(),
                            raw.evaluatedAt()
                    );
                }
            }

            return new ControlEvaluationResult(
                    control.controlId(),
                    control.name(),
                    control.module(),
                    ControlStatus.NOT_EVALUATED,
                    control.severity(),
                    null,
                    null,
                    null,
                    null
            );

        } catch (Exception e) {
            log.warn("Error evaluating control {}: {}", control.controlId(), e.getMessage());
            throw new ControlEvaluationException("Failed to evaluate control: " + control.controlId(), e);
        }
    }
}
