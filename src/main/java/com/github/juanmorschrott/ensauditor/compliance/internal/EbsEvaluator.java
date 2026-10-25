package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.EbsVolumeDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for EBS volume ENS controls.
 */
@Component
public class EbsEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public EbsEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        if (!"ens.ebs.encryption".equals(control.controlId())) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for EBS evaluation.");
        }

        List<EbsVolumeDto> volumes = awsResourceService.fetchEbsVolumes();

        if (volumes == null || volumes.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No EBS volumes found. Inherently compliant.");
        }

        List<EbsVolumeDto> nonCompliant = volumes.stream().filter(v -> !v.encrypted()).toList();

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + volumes.size() + " EBS volumes are encrypted.");
        }

        String failedIds = nonCompliant.stream().map(EbsVolumeDto::volumeId).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + volumes.size() + " EBS volumes are NON-COMPLIANT: " + failedIds);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.EBS_VOLUME;
    }

    @Override
    public String getName() {
        return "EbsEvaluator";
    }
}
