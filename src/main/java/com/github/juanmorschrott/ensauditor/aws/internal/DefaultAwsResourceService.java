package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.IamPrincipalDto;
import com.github.juanmorschrott.ensauditor.aws.RdsInstanceDto;
import com.github.juanmorschrott.ensauditor.aws.S3BucketDto;
import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import com.github.juanmorschrott.ensauditor.aws.CloudTrailDto;
import com.github.juanmorschrott.ensauditor.aws.EbsVolumeDto;
import com.github.juanmorschrott.ensauditor.aws.KmsKeyDto;
import com.github.juanmorschrott.ensauditor.aws.SecurityGroupDto;
import com.github.juanmorschrott.ensauditor.aws.VpcDto;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Default implementation of {@link AwsResourceService} using AWS SDK v2.
 */
@Service
class DefaultAwsResourceService implements AwsResourceService {

    private final S3ResourceFetcher s3Fetcher;
    private final RdsResourceFetcher rdsFetcher;
    private final IamResourceFetcher iamFetcher;
    private final DynamoDBResourceFetcher dynamoDbFetcher;
    private final KmsResourceFetcher kmsFetcher;
    private final CloudTrailResourceFetcher cloudTrailFetcher;
    private final Ec2ResourceFetcher ec2Fetcher;

    public DefaultAwsResourceService(
            S3ResourceFetcher s3Fetcher,
            RdsResourceFetcher rdsFetcher,
            IamResourceFetcher iamFetcher,
            DynamoDBResourceFetcher dynamoDbFetcher,
            KmsResourceFetcher kmsFetcher,
            CloudTrailResourceFetcher cloudTrailFetcher,
            Ec2ResourceFetcher ec2Fetcher) {
        this.s3Fetcher = s3Fetcher;
        this.rdsFetcher = rdsFetcher;
        this.iamFetcher = iamFetcher;
        this.dynamoDbFetcher = dynamoDbFetcher;
        this.kmsFetcher = kmsFetcher;
        this.cloudTrailFetcher = cloudTrailFetcher;
        this.ec2Fetcher = ec2Fetcher;
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

    @Override
    public List<KmsKeyDto> fetchKmsKeys() {
        return kmsFetcher.fetchKeys();
    }

    @Override
    public List<CloudTrailDto> fetchCloudTrails() {
        return cloudTrailFetcher.fetchTrails();
    }

    @Override
    public List<EbsVolumeDto> fetchEbsVolumes() {
        return ec2Fetcher.fetchVolumes();
    }

    @Override
    public List<SecurityGroupDto> fetchSecurityGroups() {
        return ec2Fetcher.fetchSecurityGroups();
    }

    @Override
    public List<VpcDto> fetchVpcs() {
        return ec2Fetcher.fetchVpcs();
    }
}
