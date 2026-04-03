package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.RdsInstanceDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for RDS-related ENS controls.
 * Evaluates compliance of RDS database instances against ENS security requirements.
 */
@Component
public class RdsEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public RdsEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        List<RdsInstanceDto> instances = awsResourceService.fetchRdsInstances();

        if (instances == null || instances.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No RDS instances found. Inherently compliant.");
        }

        List<RdsInstanceDto> nonCompliant = getNonCompliantInstances(control.controlId(), instances);

        if (nonCompliant == null) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for RDS evaluation.");
        }

        if (nonCompliant.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + instances.size() + " RDS instances are compliant.");
        }

        String failedIds = nonCompliant.stream()
                .map(RdsInstanceDto::identifier)
                .collect(Collectors.joining(", "));

        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliant.size() + " out of " + instances.size() + " RDS instances are NON-COMPLIANT: "
                        + failedIds);
    }

    private List<RdsInstanceDto> getNonCompliantInstances(String controlId, List<RdsInstanceDto> instances) {
        return switch (controlId) {
            // C2.1 - Encryption at rest
            case "ens.rds.encryption" -> instances.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.storageEncrypted()))
                    .toList();
            // C2.2 - IAM database authentication (proxy for transit security / access control)
            case "ens.rds.iam-auth" -> instances.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.enableIamDatabaseAuthentication()))
                    .toList();
            // Multi-AZ for high availability
            case "ens.rds.multi-az" -> instances.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.multiAz()))
                    .toList();
            // Automated backups must be enabled (retention > 0)
            case "ens.rds.backup" -> instances.stream()
                    .filter(i -> i.backupRetentionPeriods() == null || i.backupRetentionPeriods() == 0)
                    .toList();
            // CloudWatch log exports must be enabled
            case "ens.rds.logging" -> instances.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.enableCloudwatchLogsExports()))
                    .toList();
            // KMS key must be set for instances with encryption
            case "ens.rds.kms-key" -> instances.stream()
                    .filter(i -> Boolean.TRUE.equals(i.storageEncrypted()) && i.kmsKeyId() == null)
                    .toList();
            default -> null;
        };
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.RDS_INSTANCE;
    }

    @Override
    public String getName() {
        return "RdsEvaluator";
    }
}
