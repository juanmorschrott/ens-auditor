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
 * Evaluator for IAM user-related ENS controls.
 * Evaluates MFA enforcement and unused credentials.
 */
@Component
public class IamUserEvaluator implements ResourceEvaluator {

    private static final int UNUSED_KEY_THRESHOLD_DAYS = 90;

    private final AwsResourceService awsResourceService;

    public IamUserEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        return switch (control.controlId()) {
            case "ens.iam.mfa" -> evaluateMfa(control);
            case "ens.iam.unused-credentials" -> evaluateUnusedCredentials(control);
            default -> new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for IAM user evaluation.");
        };
    }

    private ControlEvaluationResult evaluateMfa(ControlDefinition control) {
        List<IamPrincipalDto> users = awsResourceService.fetchIamUsers();

        if (users == null || users.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM users found. Inherently compliant.");
        }

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

    private ControlEvaluationResult evaluateUnusedCredentials(ControlDefinition control) {
        List<IamPrincipalDto> users = awsResourceService.fetchIamUsers();

        if (users == null || users.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No IAM users found. Inherently compliant.");
        }

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
        return ResourceType.IAM_USER;
    }

    @Override
    public String getName() {
        return "IamUserEvaluator";
    }
}
