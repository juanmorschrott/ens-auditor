package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.CloudTrailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.Trail;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CloudTrail-specific resource fetcher.
 */
@Component
class CloudTrailResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(CloudTrailResourceFetcher.class);

    private final CloudTrailClient cloudTrailClient;

    CloudTrailResourceFetcher(CloudTrailClient cloudTrailClient) {
        this.cloudTrailClient = cloudTrailClient;
    }

    public List<CloudTrailDto> fetchTrails() {
        // Shadow trails (multi-region trails homed elsewhere) are included by default; dedupe by ARN
        Map<String, Trail> trails = cloudTrailClient.describeTrails().trailList().stream()
                .collect(Collectors.toMap(Trail::trailARN, Function.identity(), (a, b) -> a));

        return trails.values().stream()
                .sorted(Comparator.comparing(Trail::name))
                .map(this::toDto)
                .toList();
    }

    private CloudTrailDto toDto(Trail trail) {
        boolean logging = false;
        try {
            logging = Boolean.TRUE.equals(
                    cloudTrailClient.getTrailStatus(b -> b.name(trail.trailARN())).isLogging());
        } catch (SdkException e) {
            log.warn("Could not fetch status of trail {}: {}", trail.name(), e.getMessage());
        }

        return new CloudTrailDto(
                trail.name(),
                trail.trailARN(),
                Boolean.TRUE.equals(trail.isMultiRegionTrail()),
                logging,
                Boolean.TRUE.equals(trail.logFileValidationEnabled()),
                trail.kmsKeyId(),
                trail.cloudWatchLogsLogGroupArn()
        );
    }
}
