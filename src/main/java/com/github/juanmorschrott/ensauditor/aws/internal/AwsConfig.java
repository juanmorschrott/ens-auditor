package com.github.juanmorschrott.ensauditor.aws.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK v2 configuration exposing service clients as Spring Beans.
 */
@Configuration
public class AwsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${aws.region:}")
    private String configuredRegion;

    private Region resolveRegion() {
        // 1. Use configured region from application.yaml / env
        if (configuredRegion != null && !configuredRegion.isBlank()) {
            return Region.of(configuredRegion);
        }
        // 2. Use AWS default provider chain (env, system props, profile, instance metadata)
        try {
            return DefaultAwsRegionProviderChain.builder().build().getRegion();
        } catch (SdkClientException e) {
            log.warn("AWS region not configured. Falling back to us-east-1. Set AWS_REGION or aws.region property.");
            return Region.US_EAST_1;
        }
    }

    @Bean
    S3Client s3Client() {
        return S3Client.builder().region(resolveRegion()).build();
    }

    @Bean
    IamClient iamClient() {
        return IamClient.builder().region(resolveRegion()).build();
    }

    @Bean
    RdsClient rdsClient() {
        return RdsClient.builder().region(resolveRegion()).build();
    }
}
