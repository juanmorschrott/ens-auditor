package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * S3-specific resource fetcher.
 */
@Component
class S3ResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(S3ResourceFetcher.class);

    private final S3Client s3Client;
    private final ExecutorService executorService;

    public S3ResourceFetcher(S3Client s3Client, ExecutorService executorService) {
        this.s3Client = s3Client;
        this.executorService = executorService;
    }

    /**
     * Fetches all S3 buckets.
     *
     * @return list of S3 bucket DTOs
     */
    public List<S3BucketDto> fetchBuckets() {
        List<Bucket> buckets = s3Client.listBuckets().buckets();

        List<CompletableFuture<S3BucketDto>> futures = buckets.stream()
                .map(bucket -> CompletableFuture.supplyAsync(() -> fetchBucket(bucket.name()), executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Fetches S3 bucket configuration.
     *
     * @param bucketName the bucket name
     * @return bucket DTO
     */
    public S3BucketDto fetchBucket(String bucketName) {
        boolean encryptionEnabled = false;
        String encryptionAlgorithm = null;
        boolean versioningEnabled = false;
        boolean publicAccessBlockEnabled = false;
        boolean loggingEnabled = false;
        String loggingTargetBucket = null;

        try {
            GetBucketEncryptionResponse encryptionResp = s3Client.getBucketEncryption(b -> b.bucket(bucketName));
            if (encryptionResp.serverSideEncryptionConfiguration() != null && !encryptionResp.serverSideEncryptionConfiguration().rules().isEmpty()) {
                encryptionEnabled = true;
                encryptionAlgorithm = encryptionResp.serverSideEncryptionConfiguration().rules().getFirst()
                        .applyServerSideEncryptionByDefault().sseAlgorithmAsString();
            }
        } catch (S3Exception e) {
            log.debug("No encryption config for bucket {}: {}", bucketName, e.awsErrorDetails().errorCode());
        }

        try {
            GetBucketVersioningResponse versioningResp = s3Client.getBucketVersioning(b -> b.bucket(bucketName));
            versioningEnabled = BucketVersioningStatus.ENABLED == versioningResp.status();
        } catch (S3Exception e) {
            log.debug("Could not fetch versioning for bucket {}: {}", bucketName, e.getMessage());
        }

        try {
            GetPublicAccessBlockResponse pabResp = s3Client.getPublicAccessBlock(b -> b.bucket(bucketName));
            PublicAccessBlockConfiguration pabConfig = pabResp.publicAccessBlockConfiguration();
            if (pabConfig != null) {
                publicAccessBlockEnabled = pabConfig.blockPublicAcls() && pabConfig.ignorePublicAcls() &&
                        pabConfig.blockPublicPolicy() && pabConfig.restrictPublicBuckets();
            }
        } catch (S3Exception e) {
            log.debug("Could not fetch public access block for bucket {}: {}", bucketName, e.getMessage());
        }

        try {
            GetBucketLoggingResponse loggingResp = s3Client.getBucketLogging(b -> b.bucket(bucketName));
            if (loggingResp.loggingEnabled() != null) {
                loggingEnabled = true;
                loggingTargetBucket = loggingResp.loggingEnabled().targetBucket();
            }
        } catch (S3Exception e) {
            log.debug("Could not fetch logging config for bucket {}: {}", bucketName, e.getMessage());
        }

        return new S3BucketDto(
                bucketName,
                null,
                Instant.now(),
                versioningEnabled,
                encryptionEnabled,
                encryptionAlgorithm,
                loggingEnabled,
                loggingTargetBucket,
                publicAccessBlockEnabled,
                Map.of()
        );
    }

}
