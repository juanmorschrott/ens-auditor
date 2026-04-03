package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
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
class S3EvaluatorTest {

    @Mock
    private AwsResourceService awsResourceService;

    @InjectMocks
    private S3Evaluator evaluator;

    private ControlDefinition control(String id) {
        return new ControlDefinition(id, "test", null, null, null, null);
    }

    // --- No buckets → compliant ---

    @Test
    void noBuckets_returnsCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of());

        ControlEvaluationResult result = evaluator.evaluate(control("ens.s3.encryption"));

        assertEquals(ControlStatus.COMPLIANT, result.status());
    }

    // --- Encryption ---

    @Test
    void allBucketsEncrypted_returnsCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("b1", true, true, true, true),
                bucket("b2", true, true, true, true)));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.s3.encryption"));

        assertEquals(ControlStatus.COMPLIANT, result.status());
    }

    @Test
    void unencryptedBucket_returnsNonCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("secure", true, true, true, true),
                bucket("open", false, true, true, true)));

        ControlEvaluationResult result = evaluator.evaluate(control("ens.s3.encryption"));

        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
        assertEquals("1 out of 2 buckets are NON-COMPLIANT: open", result.findings());
    }

    // --- Versioning ---

    @Test
    void allBucketsVersioned_returnsCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("b1", true, true, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.s3.versioning")).status());
    }

    @Test
    void unversionedBucket_returnsNonCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("no-ver", true, false, true, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.s3.versioning")).status());
    }

    // --- Public access ---

    @Test
    void publicAccessBlocked_returnsCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("b1", true, true, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.s3.public-access")).status());
    }

    @Test
    void publicAccessOpen_returnsNonCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("public", true, true, true, false)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.s3.public-access")).status());
    }

    // --- Logging ---

    @Test
    void loggingEnabled_returnsCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("b1", true, true, true, true)));

        assertEquals(ControlStatus.COMPLIANT,
                evaluator.evaluate(control("ens.s3.logging")).status());
    }

    @Test
    void loggingDisabled_returnsNonCompliant() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("no-log", true, true, false, true)));

        assertEquals(ControlStatus.NON_COMPLIANT,
                evaluator.evaluate(control("ens.s3.logging")).status());
    }

    // --- Unknown control ---

    @Test
    void unknownControl_returnsNotEvaluated() {
        when(awsResourceService.fetchS3Buckets()).thenReturn(List.of(
                bucket("b1", true, true, true, true)));

        assertEquals(ControlStatus.NOT_EVALUATED,
                evaluator.evaluate(control("ens.s3.unknown")).status());
    }

    private S3BucketDto bucket(String name, boolean encrypted, boolean versioned,
                               boolean logging, boolean publicAccessBlock) {
        return new S3BucketDto(name, "us-east-1", Instant.now(),
                versioned, encrypted, "AES256", logging, null, publicAccessBlock, Map.of());
    }
}
