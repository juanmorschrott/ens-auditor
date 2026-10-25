package com.github.juanmorschrott.ensauditor.aws;

public record CloudTrailDto(
        String name,
        String arn,
        boolean multiRegion,
        boolean logging,
        boolean logFileValidationEnabled,
        String kmsKeyId,
        String cloudWatchLogsLogGroupArn
) {}
