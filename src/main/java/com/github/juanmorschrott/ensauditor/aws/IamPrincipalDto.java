package com.github.juanmorschrott.ensauditor.aws;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for AWS IAM principal (user or role).
 * Representation of IAM principal details retrieved from AWS.
 */
public record IamPrincipalDto(
        String name,
        String id,
        String arn,
        Instant createdAt,
        String assumeRolePolicyDocument,
        Boolean mfaRequired,
        Integer maxSessionDurationSeconds,
        String path,
        List<String> attachedPolicies,
        List<String> inlinePolicies,
        Boolean hasActiveAccessKeys,
        Integer lastUsedDaysAgo,
        Map<String, String> tags) {

    /**
     * Convenience constructor for building an IAM role DTO with minimal fields.
     * Useful when detailed information is not available.
     */
    public IamPrincipalDto(String name, String id, String arn, Instant createdAt) {
        this(name, id, arn, createdAt, null, false, 3600, "/",
                List.of(), List.of(), false, null, Map.of());
    }
}
