package com.github.juanmorschrott.ensauditor.aws;

import java.time.Instant;
import java.util.Map;

/**
 * Data transfer object for AWS S3 bucket information.
 * Representation of S3 bucket details retrieved from AWS.
 */
public record S3BucketDto(
        String name,
        String region,
        Instant createdAt,
        Boolean versioningEnabled,
        Boolean encryptionEnabled,
        String encryptionAlgorithm,
        Boolean loggingEnabled,
        String loggingTargetBucket,
        Boolean publicAccessBlockEnabled,
        Map<String, String> tags) {

    /**
     * Convenience constructor for building an S3 bucket DTO with minimal fields.
     * Useful when detailed information is not available.
     */
    public S3BucketDto(String name, String region, Instant createdAt) {
        this(name, region, createdAt, false, false, null,
                false, null, false, Map.of());
    }
}
