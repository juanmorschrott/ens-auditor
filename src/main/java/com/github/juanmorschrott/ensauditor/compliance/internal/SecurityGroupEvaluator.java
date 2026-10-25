package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.aws.SecurityGroupDto;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for EC2 security group ENS controls.
 */
@Component
public class SecurityGroupEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public SecurityGroupEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        if (!"ens.ec2.sg-admin-ports".equals(control.controlId())) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for security group evaluation.");
        }

        List<SecurityGroupDto> groups = awsResourceService.fetchSecurityGroups();

        if (groups == null || groups.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No security groups found. Inherently compliant.");
        }

        List<SecurityGroupDto> nonCompliant = groups.stream()
                .filter(SecurityGroupDto::adminPortsOpenToWorld)
                .toList();

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No security group exposes SSH/RDP to the internet (" + groups.size() + " checked).");
        }

        String failedIds = nonCompliant.stream().map(SecurityGroupDto::groupId).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + groups.size()
                        + " security groups expose SSH/RDP to 0.0.0.0/0 or ::/0: " + failedIds);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SECURITY_GROUP;
    }

    @Override
    public String getName() {
        return "SecurityGroupEvaluator";
    }
}
