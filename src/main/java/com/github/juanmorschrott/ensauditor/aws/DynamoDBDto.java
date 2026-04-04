package com.github.juanmorschrott.ensauditor.aws;

import java.time.Instant;
import java.util.Map;

public record DynamoDBDto(
        String name,
        Instant creationDateTime,
        boolean sseEnabled,
        String sseDescriptionStatus,
        String sseDescriptionKmsMasterKeyArn,
        Map<String, String> tags
) {}
