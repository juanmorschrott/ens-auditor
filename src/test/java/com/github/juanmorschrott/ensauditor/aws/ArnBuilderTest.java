package com.github.juanmorschrott.ensauditor.aws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for ArnBuilder utility.
 */
class ArnBuilderTest {

    @Test
    void testBuildValidArn() {
        String arn = new ArnBuilder()
                .withService("s3")
                .withResource("my-bucket")
                .build();
        
        assertEquals("arn:aws:s3:::my-bucket", arn);
    }

    @Test
    void testBuildArnWithRegionAndAccount() {
        String arn = new ArnBuilder()
                .withService("ec2")
                .withRegion("us-east-1")
                .withAccountId("123456789012")
                .withResource("instance/i-0123456789abcdef0")
                .build();
        
        assertEquals("arn:aws:ec2:us-east-1:123456789012:instance/i-0123456789abcdef0", arn);
    }

    @Test
    void testParseValidArn() {
        String arn = "arn:aws:s3:us-east-1:123456789012:bucket/my-bucket";
        ArnBuilder.ArnComponents components = ArnBuilder.parseArn(arn);
        
        assertEquals("aws", components.partition());
        assertEquals("s3", components.service());
        assertEquals("us-east-1", components.region());
        assertEquals("123456789012", components.accountId());
        assertEquals("bucket/my-bucket", components.resource());
    }

    @Test
    void testParseInvalidArn() {
        assertThrows(IllegalArgumentException.class, () -> 
                ArnBuilder.parseArn("not-an-arn")
        );
    }
}
