package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for IAM role-related ENS controls.
 * Evaluates least-privilege enforcement on IAM roles.
 */
@Component
public class IamRoleEvaluator implements ResourceEvaluator {

    private static final List<String> OVERLY_BROAD_POLICIES = List.of(
            "arn:aws:iam::aws:policy/AdministratorAccess",
            "arn:aws:iam::aws:policy/PowerUserAccess"
    );

    private final AwsResourceService awsResourceService;

    public IamRoleEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        if (!"ens.iam.least-privilege".equals(control.controlId())) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for IAM role evaluation.");
        }

        List<IamPrincipalDto> roles = awsResourceService.fetchIamRoles();

        if (roles == null || roles.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM roles found. Inherently compliant.");
        }

        List<IamPrincipalDto> violating = roles.stream()
                .filter(r -> r.attachedPolicies() != null &&
                        r.attachedPolicies().stream().anyMatch(OVERLY_BROAD_POLICIES::contains))
                .toList();

        if (violating.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + roles.size() + " IAM roles follow least-privilege.");
        }

        String failedRoles = violating.stream().map(IamPrincipalDto::name).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                violating.size() + " out of " + roles.size()
                        + " IAM roles attach overly broad policies: " + failedRoles);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.IAM_ROLE;
    }

    @Override
    public String getName() {
        return "IamRoleEvaluator";
    }
}
