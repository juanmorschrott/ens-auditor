package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import com.github.juanmorschrott.ensauditor.aws.RdsInstanceDto;
import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Default implementation of resource fetcher using AWS SDK v2.
 */
@Service
class AwsResourceFetcher implements AwsResourceService {

    private final S3ResourceFetcher s3Fetcher;
    private final RdsResourceFetcher rdsFetcher;
    private final IamResourceFetcher iamFetcher;
    private final DynamoDBResourceFetcher dynamoDbFetcher;

    public AwsResourceFetcher(
            S3ResourceFetcher s3Fetcher,
            RdsResourceFetcher rdsFetcher,
            IamResourceFetcher iamFetcher,
            DynamoDBResourceFetcher dynamoDbFetcher) {
        this.s3Fetcher = s3Fetcher;
        this.rdsFetcher = rdsFetcher;
        this.iamFetcher = iamFetcher;
        this.dynamoDbFetcher = dynamoDbFetcher;
    }

    @Override
    public List<S3BucketDto> fetchS3Buckets() {
        return s3Fetcher.fetchBuckets();
    }

    @Override
    public List<RdsInstanceDto> fetchRdsInstances() {
        return rdsFetcher.fetchInstances();
    }

    @Override
    public List<IamPrincipalDto> fetchIamUsers() {
        return iamFetcher.fetchUsers();
    }

    @Override
    public List<IamPrincipalDto> fetchIamRoles() {
        return iamFetcher.fetchRoles();
    }

    @Override
    public List<DynamoDBDto> fetchDynamoDBs() {
        return dynamoDbFetcher.fetchTables();
    }
}
