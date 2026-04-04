package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.shell.interactive.enabled=false"
        })
@Import(LocalStackTestConfiguration.class)
class S3ResourceFetcherIT {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3ResourceFetcher s3ResourceFetcher;

    @BeforeEach
    void cleanUp() {
        // Delete all existing buckets for test isolation
        s3Client.listBuckets().buckets().forEach(bucket -> {
            try {
                // Delete all objects first
                s3Client.listObjectsV2(b -> b.bucket(bucket.name())).contents()
                        .forEach(obj -> s3Client.deleteObject(d -> d.bucket(bucket.name()).key(obj.key())));
                s3Client.deleteBucket(b -> b.bucket(bucket.name()));
            } catch (S3Exception ignored) {
            }
        });
    }

    @Test
    void fetchBuckets_returnsEmptyList_whenNoBucketsExist() {
        List<S3BucketDto> buckets = s3ResourceFetcher.fetchBuckets();
        assertTrue(buckets.isEmpty());
    }

    @Test
    void fetchBuckets_returnsBucketWithCorrectName() {
        s3Client.createBucket(b -> b.bucket("test-bucket"));

        List<S3BucketDto> buckets = s3ResourceFetcher.fetchBuckets();

        assertEquals(1, buckets.size());
        assertEquals("test-bucket", buckets.getFirst().name());
    }

    @Test
    void fetchBucket_detectsEncryptionEnabled() {
        s3Client.createBucket(b -> b.bucket("encrypted-bucket"));
        s3Client.putBucketEncryption(b -> b
                .bucket("encrypted-bucket")
                .serverSideEncryptionConfiguration(c -> c
                        .rules(r -> r.applyServerSideEncryptionByDefault(
                                d -> d.sseAlgorithm(ServerSideEncryption.AES256)))));

        S3BucketDto dto = s3ResourceFetcher.fetchBucket("encrypted-bucket");

        assertTrue(dto.encryptionEnabled());
        assertEquals("AES256", dto.encryptionAlgorithm());
    }

    @Test
    void fetchBucket_detectsVersioningEnabled() {
        s3Client.createBucket(b -> b.bucket("versioned-bucket"));
        s3Client.putBucketVersioning(b -> b
                .bucket("versioned-bucket")
                .versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));

        S3BucketDto dto = s3ResourceFetcher.fetchBucket("versioned-bucket");

        assertTrue(dto.versioningEnabled());
    }

    @Test
    void fetchBucket_detectsVersioningDisabled() {
        s3Client.createBucket(b -> b.bucket("unversioned-bucket"));

        S3BucketDto dto = s3ResourceFetcher.fetchBucket("unversioned-bucket");

        assertFalse(dto.versioningEnabled());
    }

    @Test
    void fetchBucket_detectsPublicAccessBlockEnabled() {
        s3Client.createBucket(b -> b.bucket("private-bucket"));
        s3Client.putPublicAccessBlock(b -> b
                .bucket("private-bucket")
                .publicAccessBlockConfiguration(c -> c
                        .blockPublicAcls(true)
                        .ignorePublicAcls(true)
                        .blockPublicPolicy(true)
                        .restrictPublicBuckets(true)));

        S3BucketDto dto = s3ResourceFetcher.fetchBucket("private-bucket");

        assertTrue(dto.publicAccessBlockEnabled());
    }

    @Test
    void fetchBucket_detectsLoggingEnabled() {
        s3Client.createBucket(b -> b.bucket("logging-target"));
        s3Client.createBucket(b -> b.bucket("logged-bucket"));
        s3Client.putBucketLogging(b -> b
                .bucket("logged-bucket")
                .bucketLoggingStatus(l -> l
                        .loggingEnabled(le -> le
                                .targetBucket("logging-target")
                                .targetPrefix("logs/"))));

        S3BucketDto dto = s3ResourceFetcher.fetchBucket("logged-bucket");

        assertTrue(dto.loggingEnabled());
        assertEquals("logging-target", dto.loggingTargetBucket());
    }

    @Test
    void fetchBuckets_handlesMultipleBuckets() {
        s3Client.createBucket(b -> b.bucket("bucket-one"));
        s3Client.createBucket(b -> b.bucket("bucket-two"));
        s3Client.createBucket(b -> b.bucket("bucket-three"));

        List<S3BucketDto> buckets = s3ResourceFetcher.fetchBuckets();

        assertEquals(3, buckets.size());
        List<String> names = buckets.stream().map(S3BucketDto::name).sorted().toList();
        assertEquals(List.of("bucket-one", "bucket-three", "bucket-two"), names);
    }
}
