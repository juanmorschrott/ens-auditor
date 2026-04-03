package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for S3-related ENS controls.
 * Evaluates compliance of S3 buckets against ENS security requirements.
 */
@Component
public class S3Evaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public S3Evaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    /**
     * Evaluates S3 bucket compliance.
     * 
     * @param control the control to evaluate
     * @return evaluation result
     */
    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        
        List<S3BucketDto> buckets = awsResourceService.fetchS3Buckets();

        if (buckets == null || buckets.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No S3 buckets found. Inherently compliant.");
        }

        List<S3BucketDto> nonCompliantBuckets = getNonCompliantBuckets(control.controlId(), buckets);

        if (nonCompliantBuckets == null) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for S3 evaluation.");
        }

        if (nonCompliantBuckets.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + buckets.size() + " buckets are compliant.");
        }

        String failedNames = nonCompliantBuckets.stream()
                .map(S3BucketDto::name)
                .collect(Collectors.joining(", "));

        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliantBuckets.size() + " out of " + buckets.size() + " buckets are NON-COMPLIANT: "
                        + failedNames);
    }

    private List<S3BucketDto> getNonCompliantBuckets(String controlId, List<S3BucketDto> buckets) {
        return switch (controlId) {
            case "ens.s3.encryption" -> buckets.stream()
                    .filter(b -> Boolean.FALSE.equals(b.encryptionEnabled()))
                    .toList();
            case "ens.s3.versioning" -> buckets.stream()
                    .filter(b -> Boolean.FALSE.equals(b.versioningEnabled()))
                    .toList();
            case "ens.s3.public-access" -> buckets.stream()
                    .filter(b -> Boolean.FALSE.equals(b.publicAccessBlockEnabled()))
                    .toList();
            case "ens.s3.logging" -> buckets.stream()
                    .filter(b -> Boolean.FALSE.equals(b.loggingEnabled()))
                    .toList();
            default -> null; // Unknown control
        };
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.S3_BUCKET;
    }

    @Override
    public String getName() {
        return "S3Evaluator";
    }
}
