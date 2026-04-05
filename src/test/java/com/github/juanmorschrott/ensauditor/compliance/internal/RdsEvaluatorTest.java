package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.RdsInstanceDto;
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
class RdsEvaluatorTest {

    @Mock
    private AwsResourceService awsResourceService;

    @InjectMocks
    private RdsEvaluator evaluator;

    private ControlDefinition control(String id) {
        return new ControlDefinition(id, "mp.si", "test", null, null, null);
    }

    // --- No instances → compliant ---

    @Test
    void noInstances_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of());

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.encryption")).status());
    }

    // --- Encryption ---

    @Test
    void allEncrypted_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.encryption")).status());
    }

    @Test
    void unencrypted_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", false, null, true, 7, true, true)));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.rds.encryption"));
        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
        assertEquals("1 out of 1 RDS instances are NON-COMPLIANT: db-1", result.findings());
    }

    // --- IAM auth ---

    @Test
    void iamAuthEnabled_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.iam-auth")).status());
    }

    @Test
    void iamAuthDisabled_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, false)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.rds.iam-auth")).status());
    }

    // --- Multi-AZ ---

    @Test
    void multiAzEnabled_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.multi-az")).status());
    }

    @Test
    void multiAzDisabled_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", false, 7, true, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.rds.multi-az")).status());
    }

    // --- Backup ---

    @Test
    void backupEnabled_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.backup")).status());
    }

    @Test
    void backupDisabled_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 0, true, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.rds.backup")).status());
    }

    // --- Logging ---

    @Test
    void loggingEnabled_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.logging")).status());
    }

    @Test
    void loggingDisabled_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key-1", true, 7, false, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.rds.logging")).status());
    }

    // --- KMS key ---

    @Test
    void encryptedWithKms_returnsCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "arn:aws:kms:key", true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.kms-key")).status());
    }

    @Test
    void encryptedWithoutKms_returnsNonCompliant() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, null, true, 7, true, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.rds.kms-key")).status());
    }

    @Test
    void unencryptedWithoutKms_isCompliant() {
        // If not encrypted, kms-key check doesn't flag it (encryption check catches that)
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", false, null, true, 7, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.rds.kms-key")).status());
    }

    // --- Unknown control ---

    @Test
    void unknownControl_returnsNotEvaluated() {
        when(awsResourceService.fetchRdsInstances()).thenReturn(List.of(
                rds("db-1", true, "key", true, 7, true, true)));

        assertEquals(ControlStatus.NOT_EVALUATED,
                evaluator.evaluate(control("ens.rds.unknown")).status());
    }

    private RdsInstanceDto rds(String id, boolean encrypted, String kmsKeyId,
                               boolean multiAz, int backupRetention,
                               boolean logging, boolean iamAuth) {
        return new RdsInstanceDto(id, "mysql", "8.0", "db.t3.micro", "available",
                Instant.now(), encrypted, kmsKeyId, multiAz, backupRetention,
                "03:00-04:00", logging, iamAuth, "endpoint.rds.amazonaws.com", 3306, Map.of());
    }
}
