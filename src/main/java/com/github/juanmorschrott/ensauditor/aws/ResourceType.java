package com.github.juanmorschrott.ensauditor.aws;

/**
 * Represents the types of AWS resources that can be audited.
 */
public enum ResourceType {
    /**
     * AWS S3 bucket.
     */
    S3_BUCKET("s3:bucket"),
    
    /**
     * AWS RDS database instance.
     */
    RDS_INSTANCE("rds:db"),
    
    /**
     * AWS IAM user.
     */
    IAM_USER("iam:user"),
    
    /**
     * AWS IAM role.
     */
    IAM_ROLE("iam:role"),
    
    /**
     * AWS IAM policy.
     */
    IAM_POLICY("iam:policy"),
    
    /**
     * AWS KMS key.
     */
    KMS_KEY("kms:key"),
    
    /**
     * AWS VPC.
     */
    VPC("ec2:vpc");

    private final String identifier;

    ResourceType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Resolves a ResourceType from its identifier string (e.g. "s3:bucket").
     * @param identifier the resource type identifier
     * @return the matching ResourceType, or null if not found
     */
    public static ResourceType fromIdentifier(String identifier) {
        if (identifier == null) return null;
        for (ResourceType type : values()) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }
        return null;
    }
}
