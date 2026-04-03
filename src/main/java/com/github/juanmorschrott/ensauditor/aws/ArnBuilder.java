package com.github.juanmorschrott.ensauditor.aws;

/**
 * Utility for building and parsing AWS ARNs (Amazon Resource Names).
 */
public class ArnBuilder {
    private String partition = "aws";
    private String service;
    private String region;
    private String accountId;
    private String resource;

    public ArnBuilder withService(String service) {
        this.service = service;
        return this;
    }

    public ArnBuilder withRegion(String region) {
        this.region = region;
        return this;
    }

    public ArnBuilder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public ArnBuilder withResource(String resource) {
        this.resource = resource;
        return this;
    }

    public String build() {
        StringBuilder arn = new StringBuilder("arn:");
        arn.append(partition).append(":");
        arn.append(service != null ? service : "").append(":");
        arn.append(region != null ? region : "").append(":");
        arn.append(accountId != null ? accountId : "").append(":");
        arn.append(resource != null ? resource : "");
        return arn.toString();
    }

    /**
     * Parses an ARN string and returns its components.
     * @param arn the ARN string to parse
     * @return ArnComponents containing parsed parts
     */
    public static ArnComponents parseArn(String arn) {
        if (arn == null || !arn.startsWith("arn:")) {
            throw new IllegalArgumentException("Invalid ARN format: " + arn);
        }

        String[] parts = arn.split(":", 6);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid ARN format: " + arn);
        }

        return new ArnComponents(
            parts[1], // partition
            parts[2], // service
            parts[3], // region
            parts[4], // accountId
            parts[5]  // resource
        );
    }

    /**
     * Container for parsed ARN components.
     */
    public record ArnComponents(String partition, String service, String region,
                                String accountId, String resource) {}
}
