package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * IAM-specific resource fetcher.
 */
@Component
class IamResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(IamResourceFetcher.class);

    private final IamClient iamClient;
    private final ExecutorService executorService;
    private final Semaphore semaphore = new Semaphore(10); // Limit to 10 concurrent IAM calls

    public IamResourceFetcher(IamClient iamClient, ExecutorService executorService) {
        this.iamClient = iamClient;
        this.executorService = executorService;
    }

    /**
     * Fetches all IAM users.
     *
     * @return list of IAM user DTOs
     */
    public List<IamPrincipalDto> fetchUsers() {
        List<User> users = iamClient.listUsersPaginator().users().stream().toList();

        List<CompletableFuture<IamPrincipalDto>> futures = users.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return fetchUser(user.userName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        semaphore.release();
                    }
                }, executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Fetches all IAM roles.
     *
     * @return list of IAM role DTOs
     */
    public List<IamPrincipalDto> fetchRoles() {
        List<Role> roles = iamClient.listRolesPaginator().roles().stream().toList();

        List<CompletableFuture<IamPrincipalDto>> futures = roles.stream()
                .map(role -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return fetchRole(role.roleName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        semaphore.release();
                    }
                }, executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Fetches IAM user configuration.
     *
     * @param userName the IAM user name
     * @return user DTO
     */
    public IamPrincipalDto fetchUser(String userName) {
        User user = iamClient.getUser(b -> b.userName(userName)).user();

        boolean mfaRequired = hasMfaDevice(userName);
        AccessKeyUsage accessKeyUsage = fetchAccessKeyUsage(userName);

        return new IamPrincipalDto(
                user.userName(),
                user.userId(),
                user.arn(),
                user.createDate(),
                null,
                mfaRequired,
                null,
                user.path(),
                fetchAttachedUserPolicies(userName),
                fetchInlineUserPolicies(userName),
                accessKeyUsage.hasActiveAccessKeys(),
                accessKeyUsage.lastUsedDaysAgo(),
                fetchUserTags(userName)
        );
    }

    private boolean hasMfaDevice(String userName) {
        try {
            return !iamClient.listMFADevices(b -> b.userName(userName)).mfaDevices().isEmpty();
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
            return false;
        }
    }

    private AccessKeyUsage fetchAccessKeyUsage(String userName) {
        boolean hasActiveAccessKeys = false;
        Integer lastUsedDaysAgo = null;
        try {
            List<AccessKeyMetadata> accessKeys = iamClient.listAccessKeys(b -> b.userName(userName)).accessKeyMetadata();
            for (AccessKeyMetadata key : accessKeys) {
                if (StatusType.ACTIVE == key.status()) {
                    hasActiveAccessKeys = true;
                    try {
                        Instant lastUsed = iamClient.getAccessKeyLastUsed(b -> b.accessKeyId(key.accessKeyId()))
                                .accessKeyLastUsed().lastUsedDate();
                        lastUsedDaysAgo = latestUsageDays(lastUsedDaysAgo, lastUsed);
                    } catch (SdkException e) {
                        log.debug("IAM API call failed: {}", e.getMessage());
                    }
                }
            }
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }
        return new AccessKeyUsage(hasActiveAccessKeys, lastUsedDaysAgo);
    }

    private static Integer latestUsageDays(Integer current, Instant lastUsed) {
        if (lastUsed == null) {
            return current;
        }
        int days = (int) ChronoUnit.DAYS.between(lastUsed, Instant.now());
        return current == null ? days : Math.min(current, days);
    }

    private List<String> fetchAttachedUserPolicies(String userName) {
        List<String> attachedPolicies = new ArrayList<>();
        try {
            iamClient.listAttachedUserPoliciesPaginator(b -> b.userName(userName))
                    .attachedPolicies()
                    .forEach(p -> attachedPolicies.add(p.policyArn()));
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }
        return attachedPolicies;
    }

    private List<String> fetchInlineUserPolicies(String userName) {
        List<String> inlinePolicies = new ArrayList<>();
        try {
            iamClient.listUserPoliciesPaginator(b -> b.userName(userName))
                    .policyNames()
                    .forEach(inlinePolicies::add);
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }
        return inlinePolicies;
    }

    private Map<String, String> fetchUserTags(String userName) {
        Map<String, String> tags = Map.of();
        try {
            tags = iamClient.listUserTags(b -> b.userName(userName)).tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value, (existing, replace) -> replace));
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }
        return tags;
    }

    /**
     * Fetches IAM role configuration.
     *
     * @param roleName the IAM role name
     * @return role DTO
     */
    public IamPrincipalDto fetchRole(String roleName) {
        Role role = iamClient.getRole(b -> b.roleName(roleName)).role();

        // Attached managed policies
        List<String> attachedPolicies = new ArrayList<>();
        try {
            iamClient.listAttachedRolePoliciesPaginator(b -> b.roleName(roleName))
                    .attachedPolicies()
                    .forEach(p -> attachedPolicies.add(p.policyArn()));
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }

        // Inline policy names
        List<String> inlinePolicies = new ArrayList<>();
        try {
            iamClient.listRolePoliciesPaginator(b -> b.roleName(roleName))
                    .policyNames()
                    .forEach(inlinePolicies::add);
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }

        // Tags
        Map<String, String> tags = Map.of();
        try {
            tags = iamClient.listRoleTags(b -> b.roleName(roleName)).tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value, (existing, replace) -> replace));
        } catch (SdkException e) {
            log.debug("IAM API call failed: {}", e.getMessage());
        }

        return new IamPrincipalDto(
                role.roleName(),
                role.roleId(),
                role.arn(),
                role.createDate(),
                role.assumeRolePolicyDocument(),
                null,
                role.maxSessionDuration(),
                role.path(),
                attachedPolicies,
                inlinePolicies,
                false,
                null,
                tags
        );
    }

    private record AccessKeyUsage(boolean hasActiveAccessKeys, Integer lastUsedDaysAgo) {}

}
