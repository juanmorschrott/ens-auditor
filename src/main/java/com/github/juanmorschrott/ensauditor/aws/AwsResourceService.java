package com.github.juanmorschrott.ensauditor.aws;

import java.util.List;

/**
 * Public API for the AWS module.
 * Retrieves AWS resources and their configurations.
 */
public interface AwsResourceService {

    /**
     * Fetches all S3 buckets from AWS.
     * @return list of S3 bucket configurations
     */
    List<S3BucketDto> fetchS3Buckets();

    /**
     * Fetches all RDS instances from AWS.
     * @return list of RDS instance configurations
     */
    List<RdsInstanceDto> fetchRdsInstances();

    /**
     * Fetches all IAM users from AWS.
     * @return list of IAM user configurations
     */
    List<IamPrincipalDto> fetchIamUsers();

    /**
     * Fetches all IAM roles from AWS.
     * @return list of IAM role configurations
     */
    List<IamPrincipalDto> fetchIamRoles();
}
