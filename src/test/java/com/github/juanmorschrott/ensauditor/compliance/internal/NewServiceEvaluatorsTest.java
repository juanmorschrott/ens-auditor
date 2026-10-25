package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.CloudTrailDto;
import com.github.juanmorschrott.ensauditor.aws.EbsVolumeDto;
import com.github.juanmorschrott.ensauditor.aws.KmsKeyDto;
import com.github.juanmorschrott.ensauditor.aws.SecurityGroupDto;
import com.github.juanmorschrott.ensauditor.aws.VpcDto;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewServiceEvaluatorsTest {

    @Mock
    private AwsResourceService aws;

    private ControlDefinition control(String id) {
        return new ControlDefinition(id, "mod", "test", null, null, null);
    }

    @Test
    void kmsRotation_flagsOnlyApplicableKeysWithoutRotation() {
        when(aws.fetchKmsKeys()).thenReturn(List.of(
                new KmsKeyDto("k1", "arn1", "Enabled", true, true),
                new KmsKeyDto("k2", "arn2", "Enabled", true, false),
                new KmsKeyDto("k3", "arn3", "Enabled", false, false)));
        ControlEvaluationResult r = new KmsEvaluator(aws).evaluate(control("ens.kms.rotation"));
        assertEquals(ControlStatus.NON_COMPLIANT, r.status());
        assertEquals("1 out of 3 KMS keys are NON-COMPLIANT: k2", r.findings());
    }

    @Test
    void kmsPendingDeletion_flagsKeys() {
        when(aws.fetchKmsKeys()).thenReturn(List.of(new KmsKeyDto("k1", "arn1", "PendingDeletion", false, false)));
        assertEquals(ControlStatus.NON_COMPLIANT,
                new KmsEvaluator(aws).evaluate(control("ens.kms.pending-deletion")).status());
    }

    @Test
    void cloudTrail_noTrails_isNonCompliant() {
        when(aws.fetchCloudTrails()).thenReturn(List.of());
        assertEquals(ControlStatus.NON_COMPLIANT,
                new CloudTrailEvaluator(aws).evaluate(control("ens.cloudtrail.enabled")).status());
    }

    @Test
    void cloudTrail_enabledRequiresLoggingMultiRegionTrail() {
        when(aws.fetchCloudTrails()).thenReturn(List.of(
                new CloudTrailDto("t", "arn", true, true, true, "kms", null)));
        assertEquals(ControlStatus.COMPLIANT,
                new CloudTrailEvaluator(aws).evaluate(control("ens.cloudtrail.enabled")).status());
    }

    @Test
    void cloudTrail_singleRegionNotLogging_isNonCompliant() {
        when(aws.fetchCloudTrails()).thenReturn(List.of(
                new CloudTrailDto("t", "arn", false, true, true, "kms", null)));
        assertEquals(ControlStatus.NON_COMPLIANT,
                new CloudTrailEvaluator(aws).evaluate(control("ens.cloudtrail.enabled")).status());
    }

    @Test
    void cloudTrail_encryptionAndValidation() {
        when(aws.fetchCloudTrails()).thenReturn(List.of(
                new CloudTrailDto("t1", "a1", true, true, true, "kms", null),
                new CloudTrailDto("t2", "a2", true, true, false, null, null)));
        CloudTrailEvaluator evaluator = new CloudTrailEvaluator(aws);
        assertEquals("1 out of 2 trails are NON-COMPLIANT: t2",
                evaluator.evaluate(control("ens.cloudtrail.encryption")).findings());
        assertEquals("1 out of 2 trails are NON-COMPLIANT: t2",
                evaluator.evaluate(control("ens.cloudtrail.log-validation")).findings());
    }

    @Test
    void ebsEncryption_flagsUnencryptedVolumes() {
        when(aws.fetchEbsVolumes()).thenReturn(List.of(
                new EbsVolumeDto("vol-1", true, "k"), new EbsVolumeDto("vol-2", false, null)));
        ControlEvaluationResult r = new EbsEvaluator(aws).evaluate(control("ens.ebs.encryption"));
        assertEquals(ControlStatus.NON_COMPLIANT, r.status());
        assertEquals("1 out of 2 EBS volumes are NON-COMPLIANT: vol-2", r.findings());
    }

    @Test
    void securityGroups_flagsOpenAdminPorts() {
        when(aws.fetchSecurityGroups()).thenReturn(List.of(
                new SecurityGroupDto("sg-1", "a", "vpc", false), new SecurityGroupDto("sg-2", "b", "vpc", true)));
        ControlEvaluationResult r = new SecurityGroupEvaluator(aws).evaluate(control("ens.ec2.sg-admin-ports"));
        assertEquals(ControlStatus.NON_COMPLIANT, r.status());
    }

    @Test
    void vpcFlowLogs_flagsVpcsWithoutFlowLogs() {
        when(aws.fetchVpcs()).thenReturn(List.of(new VpcDto("vpc-1", true), new VpcDto("vpc-2", false)));
        ControlEvaluationResult r = new VpcEvaluator(aws).evaluate(control("ens.vpc.flow-logs"));
        assertEquals("1 out of 2 VPCs are NON-COMPLIANT: vpc-2", r.findings());
    }

    @Test
    void unknownControl_isNotEvaluated() {
        assertEquals(ControlStatus.NOT_EVALUATED, new EbsEvaluator(aws).evaluate(control("x")).status());
    }
}
