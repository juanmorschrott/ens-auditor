package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.CloudTrailDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for CloudTrail-related ENS controls.
 * Unlike per-resource evaluators, the absence of trails is a finding, not compliance.
 */
@Component
public class CloudTrailEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public CloudTrailEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        List<CloudTrailDto> trails = awsResourceService.fetchCloudTrails();

        if (trails == null || trails.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                    "No CloudTrail trails found. Account activity is not being recorded.");
        }

        if ("ens.cloudtrail.enabled".equals(control.controlId())) {
            boolean ok = trails.stream().anyMatch(t -> t.logging() && t.multiRegion());
            return ok
                    ? new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                            "At least one multi-region trail is actively logging.")
                    : new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                            "No multi-region trail is actively logging.");
        }

        List<CloudTrailDto> nonCompliant = getNonCompliantTrails(control.controlId(), trails);

        if (nonCompliant == null) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for CloudTrail evaluation.");
        }

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + trails.size() + " trails are compliant.");
        }

        String failedNames = nonCompliant.stream()
                .map(CloudTrailDto::name)
                .collect(Collectors.joining(", "));

        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + trails.size() + " trails are NON-COMPLIANT: " + failedNames);
    }

    private List<CloudTrailDto> getNonCompliantTrails(String controlId, List<CloudTrailDto> trails) {
        return switch (controlId) {
            case "ens.cloudtrail.log-validation" -> trails.stream()
                    .filter(t -> !t.logFileValidationEnabled())
                    .toList();
            case "ens.cloudtrail.encryption" -> trails.stream()
                    .filter(t -> t.kmsKeyId() == null || t.kmsKeyId().isBlank())
                    .toList();
            default -> null;
        };
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.CLOUDTRAIL_TRAIL;
    }

    @Override
    public String getName() {
        return "CloudTrailEvaluator";
    }
}
