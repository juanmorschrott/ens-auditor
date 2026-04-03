package com.github.juanmorschrott.ensauditor.aws.internal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Test configuration that provides AWS clients pointing to a LocalStack container.
 * The container is shared across all tests using this configuration for performance.
 */
@TestConfiguration(proxyBeanMethods = false)
public class LocalStackTestConfiguration {

    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.0"))
            .withServices(Service.S3, Service.IAM, Service.STS);

    static {
        LOCALSTACK.start();
    }

    @Bean
    @Primary
    public S3Client testS3Client() {
        return S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(Service.S3))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @Primary
    public IamClient testIamClient() {
        return IamClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(Service.IAM))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
    }

    @Bean
    @Primary
    public RdsClient testRdsClient() {
        return RdsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
    }
}
