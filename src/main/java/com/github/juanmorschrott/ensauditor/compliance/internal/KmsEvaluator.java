package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.KmsKeyDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for KMS-related ENS controls (customer-managed keys only).
 */
@Component
public class KmsEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public KmsEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        List<KmsKeyDto> keys = awsResourceService.fetchKmsKeys();

        if (keys == null || keys.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No customer-managed KMS keys found. Inherently compliant.");
        }

        List<KmsKeyDto> nonCompliant = getNonCompliantKeys(control.controlId(), keys);

        if (nonCompliant == null) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for KMS evaluation.");
        }

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + keys.size() + " KMS keys are compliant.");
        }

        String failedIds = nonCompliant.stream()
                .map(KmsKeyDto::keyId)
                .collect(Collectors.joining(", "));

        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + keys.size() + " KMS keys are NON-COMPLIANT: " + failedIds);
    }

    private List<KmsKeyDto> getNonCompliantKeys(String controlId, List<KmsKeyDto> keys) {
        return switch (controlId) {
            case "ens.kms.rotation" -> keys.stream()
                    .filter(k -> k.rotationApplicable() && !k.rotationEnabled())
                    .toList();
            case "ens.kms.pending-deletion" -> keys.stream()
                    .filter(k -> "PendingDeletion".equals(k.state()) || "PendingReplicaDeletion".equals(k.state()))
                    .toList();
            default -> null;
        };
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.KMS_KEY;
    }

    @Override
    public String getName() {
        return "KmsEvaluator";
    }
}
