package com.github.juanmorschrott.ensauditor.aws;

import java.time.Instant;
import java.util.Map;

/**
 * Data transfer object for AWS RDS instance information.
 * Representation of RDS instance details retrieved from AWS.
 */
public record RdsInstanceDto(
        String identifier,
        String engine,
        String engineVersion,
        String instanceClass,
        String status,
        Instant createdAt,
        Boolean storageEncrypted,
        String kmsKeyId,
        Boolean multiAz,
        Integer backupRetentionPeriod,
        String preferredBackupWindow,
        Boolean enableCloudwatchLogsExports,
        Boolean enableIamDatabaseAuthentication,
        String endpoint,
        Integer port,
        Map<String, String> tags) {

    /**
     * Convenience constructor for building an RDS instance DTO with minimal fields.
     * Useful when detailed information is not available.
     */
    public RdsInstanceDto(String identifier, String engine, String instanceClass, String status) {
        this(identifier, engine, null, instanceClass, status, null, false, null, false,
                0, null, false, false, null, null, Map.of());
    }
}
