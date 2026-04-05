package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamEvaluatorTest {

    @Mock
    private AwsResourceService awsResourceService;

    @InjectMocks
    private IamEvaluator evaluator;

    private ControlDefinition control(String id) {
        return new ControlDefinition(id, "op.acc", "test", null, null, null);
    }

    // --- MFA ---

    @Test
    void noUsers_mfa_returnsCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of());

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.mfa")).status());
    }

    @Test
    void allUsersHaveMfa_returnsCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of(
                user("alice", true, false, null),
                user("bob", true, false, null)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.mfa")).status());
    }

    @Test
    void userWithoutMfa_returnsNonCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of(
                user("alice", true, false, null),
                user("bob", false, false, null)));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.iam.mfa"));
        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
        assertEquals("1 out of 2 IAM users lack MFA: bob", result.findings());
    }

    // --- Least privilege ---

    @Test
    void noRoles_leastPrivilege_returnsCompliant() {
        when(awsResourceService.fetchIamRoles()).thenReturn(List.of());

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.least-privilege")).status());
    }

    @Test
    void rolesWithNarrowPolicies_returnsCompliant() {
        when(awsResourceService.fetchIamRoles()).thenReturn(List.of(
                role("app-role", List.of("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"))));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.least-privilege")).status());
    }

    @Test
    void roleWithAdminAccess_returnsNonCompliant() {
        when(awsResourceService.fetchIamRoles()).thenReturn(List.of(
                role("admin-role", List.of("arn:aws:iam::aws:policy/AdministratorAccess")),
                role("app-role", List.of("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"))));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.iam.least-privilege"));
        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
        assertEquals("1 out of 2 IAM roles attach overly broad policies: admin-role",
                result.findings());
    }

    @Test
    void roleWithPowerUserAccess_returnsNonCompliant() {
        when(awsResourceService.fetchIamRoles()).thenReturn(List.of(
                role("power-role", List.of("arn:aws:iam::aws:policy/PowerUserAccess"))));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.iam.least-privilege")).status());
    }

    // --- Unused credentials ---

    @Test
    void noUsers_unusedCredentials_returnsCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of());

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.unused-credentials")).status());
    }

    @Test
    void recentlyUsedKeys_returnsCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of(
                user("alice", true, true, 10)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.unused-credentials")).status());
    }

    @Test
    void staleKeys_returnsNonCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of(
                user("alice", true, true, 91)));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.iam.unused-credentials"));
        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
    }

    @Test
    void noActiveKeys_returnsCompliant() {
        when(awsResourceService.fetchIamUsers()).thenReturn(List.of(
                user("alice", true, false, null)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.iam.unused-credentials")).status());
    }

    // --- Unknown control ---

    @Test
    void unknownControl_returnsNotEvaluated() {
        assertEquals(ControlStatus.NOT_EVALUATED,
                evaluator.evaluate(control("ens.iam.unknown")).status());
    }

    private IamPrincipalDto user(String name, boolean mfa, boolean hasActiveKeys, Integer lastUsedDays) {
        return new IamPrincipalDto(name, "AIDA" + name, "arn:aws:iam::123456789012:user/" + name,
                Instant.now(), null, mfa, null, "/", List.of(), List.of(),
                hasActiveKeys, lastUsedDays, Map.of());
    }

    private IamPrincipalDto role(String name, List<String> attachedPolicies) {
        return new IamPrincipalDto(name, "AROA" + name, "arn:aws:iam::123456789012:role/" + name,
                Instant.now(), null, false, 3600, "/", attachedPolicies, List.of(),
                false, null, Map.of());
    }
}
