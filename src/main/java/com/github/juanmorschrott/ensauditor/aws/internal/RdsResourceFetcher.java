package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.RdsInstanceDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.Tag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * RDS-specific resource fetcher.
 */
@Component
public class RdsResourceFetcher implements InternalResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(RdsResourceFetcher.class);

    private final RdsClient rdsClient;

    public RdsResourceFetcher(RdsClient rdsClient) {
        this.rdsClient = rdsClient;
    }

    /**
     * Fetches all RDS instances.
     *
     * @return list of RDS instance DTOs
     */
    public List<RdsInstanceDto> fetchInstances() {
        List<DBInstance> instances = rdsClient.describeDBInstancesPaginator().dbInstances().stream().toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<RdsInstanceDto>> futures = instances.stream()
                    .map(instance -> CompletableFuture.supplyAsync(
                            () -> fetchInstance(instance.dbInstanceIdentifier()), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Fetches RDS instance configuration.
     *
     * @param instanceId the instance identifier
     * @return instance DTO
     */
    public RdsInstanceDto fetchInstance(String instanceId) {
        DBInstance db = rdsClient.describeDBInstances(b -> b.dbInstanceIdentifier(instanceId))
                .dbInstances().getFirst();

        // Tags
        Map<String, String> tags = Map.of();
        try {
            if (db.dbInstanceArn() != null) {
                tags = rdsClient.listTagsForResource(b -> b.resourceName(db.dbInstanceArn()))
                        .tagList().stream()
                        .collect(Collectors.toMap(Tag::key, Tag::value, (existing, replace) -> replace));
            }
        } catch (SdkException e) {
            log.debug("Could not fetch tags for RDS instance {}: {}", db.dbInstanceArn(), e.getMessage());
        }

        boolean cloudwatchLogsEnabled = db.enabledCloudwatchLogsExports() != null
                && !db.enabledCloudwatchLogsExports().isEmpty();

        String endpoint = null;
        Integer port = null;
        if (db.endpoint() != null) {
            endpoint = db.endpoint().address();
            port = db.endpoint().port();
        }

        return new RdsInstanceDto(
                db.dbInstanceIdentifier(),
                db.engine(),
                db.engineVersion(),
                db.dbInstanceClass(),
                db.dbInstanceStatus(),
                db.instanceCreateTime(),
                db.storageEncrypted(),
                db.kmsKeyId(),
                db.multiAZ(),
                db.backupRetentionPeriod(),
                db.preferredBackupWindow(),
                cloudwatchLogsEnabled,
                db.iamDatabaseAuthenticationEnabled(),
                endpoint,
                port,
                tags
        );
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.RDS_INSTANCE;
    }

    @Override
    public String getName() {
        return "RdsResourceFetcher";
    }
}
