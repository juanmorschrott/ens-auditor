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
 * Evaluator for IAM-related ENS controls.
 * Evaluates compliance of IAM users, roles, and policies against ENS security requirements.
 */
@Component
public class IamEvaluator implements ResourceEvaluator {

    // Policies that grant unrestricted access and violate least-privilege
    private static final List<String> OVERLY_BROAD_POLICIES = List.of(
            "arn:aws:iam::aws:policy/AdministratorAccess",
            "arn:aws:iam::aws:policy/PowerUserAccess"
    );

    // Number of days after which an unused access key is considered stale
    private static final int UNUSED_KEY_THRESHOLD_DAYS = 90;

    private final AwsResourceService awsResourceService;

    public IamEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        return switch (control.controlId()) {
            // C3.1 — MFA: evaluated against users
            case "ens.iam.mfa" -> evaluateUsers(control);
            // C3.2 — Least privilege: evaluated against roles
            case "ens.iam.least-privilege" -> evaluateRoles(control);
            // Unused credentials: evaluated against users
            case "ens.iam.unused-credentials" -> evaluateUnusedCredentials(control);
            default -> new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for IAM evaluation.");
        };
    }

    private ControlEvaluationResult evaluateUsers(ControlDefinition control) {
        List<IamPrincipalDto> users = awsResourceService.fetchIamUsers();

        if (users == null || users.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM users found. Inherently compliant.");
        }

        // C3.1 - MFA must be enabled for all users
        List<IamPrincipalDto> noMfa = users.stream()
                .filter(u -> !Boolean.TRUE.equals(u.mfaRequired()))
                .toList();

        if (noMfa.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + users.size() + " IAM users have MFA enabled.");
        }

        String failedUsers = noMfa.stream().map(IamPrincipalDto::name).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                noMfa.size() + " out of " + users.size() + " IAM users lack MFA: " + failedUsers);
    }

    private ControlEvaluationResult evaluateRoles(ControlDefinition control) {
        List<IamPrincipalDto> roles = awsResourceService.fetchIamRoles();

        if (roles == null || roles.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM roles found. Inherently compliant.");
        }

        // C3.2 - Roles must not attach overly broad managed policies
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

    private ControlEvaluationResult evaluateUnusedCredentials(ControlDefinition control) {
        List<IamPrincipalDto> users = awsResourceService.fetchIamUsers();

        if (users == null || users.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM users found. Inherently compliant.");
        }

        // Flag users with active keys unused for more than the threshold
        List<IamPrincipalDto> stale = users.stream()
                .filter(u -> Boolean.TRUE.equals(u.hasActiveAccessKeys())
                        && u.lastUsedDaysAgo() != null
                        && u.lastUsedDaysAgo() > UNUSED_KEY_THRESHOLD_DAYS)
                .toList();

        if (stale.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No stale access keys found among " + users.size() + " IAM users.");
        }

        String failedUsers = stale.stream().map(IamPrincipalDto::name).collect(Collectors.joining(", "));
        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                stale.size() + " out of " + users.size()
                        + " IAM users have access keys unused for >" + UNUSED_KEY_THRESHOLD_DAYS + " days: "
                        + failedUsers);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.IAM_ROLE;
    }

    @Override
    public String getName() {
        return "IamEvaluator";
    }
}
