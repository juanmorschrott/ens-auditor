package com.github.juanmorschrott.ensauditor.aws.internal.iam;

import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import com.github.juanmorschrott.ensauditor.aws.internal.IamResourceFetcher;
import com.github.juanmorschrott.ensauditor.aws.internal.LocalStackTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.IamException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.shell.interactive.enabled=false"
        })
@Import(LocalStackTestConfiguration.class)
class IamResourceFetcherIT {

    @Autowired
    private IamClient iamClient;

    @Autowired
    private IamResourceFetcher iamResourceFetcher;

    @BeforeEach
    void cleanUp() {
        // Clean up users
        iamClient.listUsers().users().forEach(user -> {
            try {
                // Detach managed policies
                iamClient.listAttachedUserPolicies(b -> b.userName(user.userName()))
                        .attachedPolicies()
                        .forEach(p -> iamClient.detachUserPolicy(d -> d
                                .userName(user.userName())
                                .policyArn(p.policyArn())));
                // Delete inline policies
                iamClient.listUserPolicies(b -> b.userName(user.userName()))
                        .policyNames()
                        .forEach(policyName -> iamClient.deleteUserPolicy(d -> d
                                .userName(user.userName())
                                .policyName(policyName)));
                // Delete access keys
                iamClient.listAccessKeys(b -> b.userName(user.userName()))
                        .accessKeyMetadata()
                        .forEach(key -> iamClient.deleteAccessKey(d -> d
                                .userName(user.userName())
                                .accessKeyId(key.accessKeyId())));
                // Delete MFA devices
                iamClient.listMFADevices(b -> b.userName(user.userName()))
                        .mfaDevices()
                        .forEach(mfa -> {
                            iamClient.deactivateMFADevice(d -> d
                                    .userName(user.userName())
                                    .serialNumber(mfa.serialNumber()));
                            iamClient.deleteVirtualMFADevice(d -> d
                                    .serialNumber(mfa.serialNumber()));
                        });
                iamClient.deleteUser(b -> b.userName(user.userName()));
            } catch (IamException ignored) {
            }
        });

        // Clean up roles (only non-AWS managed ones)
        iamClient.listRoles().roles().stream()
                .filter(role -> !role.path().startsWith("/aws-service-role/"))
                .forEach(role -> {
                    try {
                        iamClient.listAttachedRolePolicies(b -> b.roleName(role.roleName()))
                                .attachedPolicies()
                                .forEach(p -> iamClient.detachRolePolicy(d -> d
                                        .roleName(role.roleName())
                                        .policyArn(p.policyArn())));
                        iamClient.listRolePolicies(b -> b.roleName(role.roleName()))
                                .policyNames()
                                .forEach(policyName -> iamClient.deleteRolePolicy(d -> d
                                        .roleName(role.roleName())
                                        .policyName(policyName)));
                        iamClient.deleteRole(b -> b.roleName(role.roleName()));
                    } catch (IamException ignored) {
                    }
                });
    }

    @Test
    void fetchUsers_returnsEmptyList_whenNoUsersExist() {
        List<IamPrincipalDto> users = iamResourceFetcher.fetchUsers();
        assertTrue(users.isEmpty());
    }

    @Test
    void fetchUsers_returnsUserWithCorrectName() {
        iamClient.createUser(b -> b.userName("test-user"));

        List<IamPrincipalDto> users = iamResourceFetcher.fetchUsers();

        assertEquals(1, users.size());
        assertEquals("test-user", users.getFirst().name());
        assertNotNull(users.getFirst().arn());
    }

    @Test
    void fetchUser_detectsMfaDisabled() {
        iamClient.createUser(b -> b.userName("no-mfa-user"));

        IamPrincipalDto dto = iamResourceFetcher.fetchUser("no-mfa-user");

        assertFalse(dto.mfaRequired());
    }

    @Test
    void fetchUser_detectsAccessKeys() {
        iamClient.createUser(b -> b.userName("key-user"));
        iamClient.createAccessKey(b -> b.userName("key-user"));

        IamPrincipalDto dto = iamResourceFetcher.fetchUser("key-user");

        assertTrue(dto.hasActiveAccessKeys());
    }

    @Test
    void fetchUser_detectsNoAccessKeys() {
        iamClient.createUser(b -> b.userName("no-key-user"));

        IamPrincipalDto dto = iamResourceFetcher.fetchUser("no-key-user");

        assertFalse(dto.hasActiveAccessKeys());
    }

    @Test
    void fetchUser_detectsAttachedPolicies() {
        iamClient.createUser(b -> b.userName("policy-user"));

        // Create a managed policy
        String policyArn = iamClient.createPolicy(b -> b
                .policyName("test-policy")
                .policyDocument("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [{
                                "Effect": "Allow",
                                "Action": "s3:GetObject",
                                "Resource": "*"
                            }]
                        }
                        """)).policy().arn();

        iamClient.attachUserPolicy(b -> b
                .userName("policy-user")
                .policyArn(policyArn));

        IamPrincipalDto dto = iamResourceFetcher.fetchUser("policy-user");

        assertFalse(dto.attachedPolicies().isEmpty());
        assertTrue(dto.attachedPolicies().stream().anyMatch(p -> p.contains("test-policy")));

        // Clean up
        iamClient.detachUserPolicy(b -> b.userName("policy-user").policyArn(policyArn));
        iamClient.deletePolicy(b -> b.policyArn(policyArn));
    }

    @Test
    void fetchRoles_returnsRoleWithCorrectName() {
        String trustPolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"Service": "ec2.amazonaws.com"},
                        "Action": "sts:AssumeRole"
                    }]
                }
                """;

        iamClient.createRole(b -> b
                .roleName("test-role")
                .assumeRolePolicyDocument(trustPolicy));

        List<IamPrincipalDto> roles = iamResourceFetcher.fetchRoles();

        assertTrue(roles.stream().anyMatch(r -> "test-role".equals(r.name())));
    }

    @Test
    void fetchRole_detectsAttachedPolicies() {
        String trustPolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"Service": "lambda.amazonaws.com"},
                        "Action": "sts:AssumeRole"
                    }]
                }
                """;

        iamClient.createRole(b -> b
                .roleName("role-with-policy")
                .assumeRolePolicyDocument(trustPolicy));

        String policyArn = iamClient.createPolicy(b -> b
                .policyName("role-test-policy")
                .policyDocument("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [{
                                "Effect": "Allow",
                                "Action": "logs:*",
                                "Resource": "*"
                            }]
                        }
                        """)).policy().arn();

        iamClient.attachRolePolicy(b -> b
                .roleName("role-with-policy")
                .policyArn(policyArn));

        IamPrincipalDto dto = iamResourceFetcher.fetchRole("role-with-policy");

        assertFalse(dto.attachedPolicies().isEmpty());
        assertTrue(dto.attachedPolicies().stream().anyMatch(p -> p.contains("role-test-policy")));

        // Clean up
        iamClient.detachRolePolicy(b -> b.roleName("role-with-policy").policyArn(policyArn));
        iamClient.deletePolicy(b -> b.policyArn(policyArn));
    }

    @Test
    void fetchUsers_handlesMultipleUsers() {
        iamClient.createUser(b -> b.userName("user-alpha"));
        iamClient.createUser(b -> b.userName("user-beta"));

        List<IamPrincipalDto> users = iamResourceFetcher.fetchUsers();

        assertEquals(2, users.size());
        List<String> names = users.stream().map(IamPrincipalDto::name).sorted().toList();
        assertEquals(List.of("user-alpha", "user-beta"), names);
    }
}
