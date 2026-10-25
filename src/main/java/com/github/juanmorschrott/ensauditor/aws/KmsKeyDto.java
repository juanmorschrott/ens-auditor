package com.github.juanmorschrott.ensauditor.aws;

/**
 * Customer-managed KMS key configuration.
 *
 * @param rotationApplicable whether automatic rotation can be enabled for this key
 *                           (symmetric keys with AWS_KMS origin)
 */
public record KmsKeyDto(
        String keyId,
        String arn,
        String state,
        boolean rotationApplicable,
        boolean rotationEnabled
) {}
