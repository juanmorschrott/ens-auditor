package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.KmsKeyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyManagerType;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.OriginType;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * KMS-specific resource fetcher. Only customer-managed keys are returned,
 * since AWS-managed keys are rotated by AWS.
 */
@Component
class KmsResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(KmsResourceFetcher.class);

    private final KmsClient kmsClient;
    private final ExecutorService executorService;

    KmsResourceFetcher(KmsClient kmsClient, ExecutorService executorService) {
        this.kmsClient = kmsClient;
        this.executorService = executorService;
    }

    public List<KmsKeyDto> fetchKeys() {
        List<KeyListEntry> entries = kmsClient.listKeysPaginator().keys().stream().toList();

        List<CompletableFuture<KmsKeyDto>> futures = entries.stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> fetchKey(entry.keyId()), executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    KmsKeyDto fetchKey(String keyId) {
        try {
            KeyMetadata metadata = kmsClient.describeKey(b -> b.keyId(keyId)).keyMetadata();
            if (metadata.keyManager() != KeyManagerType.CUSTOMER) {
                return null;
            }

            String state = metadata.keyStateAsString();
            boolean rotationApplicable = metadata.keySpec() == KeySpec.SYMMETRIC_DEFAULT
                    && metadata.origin() == OriginType.AWS_KMS
                    && "Enabled".equals(state);

            boolean rotationEnabled = false;
            if (rotationApplicable) {
                rotationEnabled = kmsClient.getKeyRotationStatus(b -> b.keyId(keyId)).keyRotationEnabled();
            }

            return new KmsKeyDto(metadata.keyId(), metadata.arn(), state, rotationApplicable, rotationEnabled);
        } catch (SdkException e) {
            log.warn("Could not fetch KMS key {}: {}", keyId, e.getMessage());
            return null;
        }
    }
}
