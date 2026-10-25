package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.aws.VpcDto;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for VPC ENS controls.
 */
@Component
public class VpcEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public VpcEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        if (!"ens.vpc.flow-logs".equals(control.controlId())) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for VPC evaluation.");
        }

        List<VpcDto> vpcs = awsResourceService.fetchVpcs();

        if (vpcs == null || vpcs.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No VPCs found. Inherently compliant.");
        }

        List<VpcDto> nonCompliant = vpcs.stream().filter(v -> !v.flowLogsEnabled()).toList();

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + vpcs.size() + " VPCs have active flow logs.");
        }

        String failedIds = nonCompliant.stream().map(VpcDto::vpcId).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + vpcs.size() + " VPCs are NON-COMPLIANT: " + failedIds);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.VPC;
    }

    @Override
    public String getName() {
        return "VpcEvaluator";
    }
}
