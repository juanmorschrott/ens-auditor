package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.EbsVolumeDto;
import com.github.juanmorschrott.ensauditor.aws.SecurityGroupDto;
import com.github.juanmorschrott.ensauditor.aws.VpcDto;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.FlowLog;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EC2-specific resource fetcher (EBS volumes, security groups and VPCs).
 */
@Component
class Ec2ResourceFetcher {

    private static final List<Integer> ADMIN_PORTS = List.of(22, 3389);

    private final Ec2Client ec2Client;

    Ec2ResourceFetcher(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    public List<EbsVolumeDto> fetchVolumes() {
        return ec2Client.describeVolumesPaginator().volumes().stream()
                .map(v -> new EbsVolumeDto(v.volumeId(), Boolean.TRUE.equals(v.encrypted()), v.kmsKeyId()))
                .toList();
    }

    public List<SecurityGroupDto> fetchSecurityGroups() {
        return ec2Client.describeSecurityGroupsPaginator().securityGroups().stream()
                .map(sg -> new SecurityGroupDto(sg.groupId(), sg.groupName(), sg.vpcId(), exposesAdminPorts(sg)))
                .toList();
    }

    public List<VpcDto> fetchVpcs() {
        Set<String> vpcsWithFlowLogs = ec2Client.describeFlowLogsPaginator().flowLogs().stream()
                .filter(f -> "ACTIVE".equals(f.flowLogStatus()))
                .map(FlowLog::resourceId)
                .collect(Collectors.toSet());

        return ec2Client.describeVpcsPaginator().vpcs().stream()
                .map(v -> new VpcDto(v.vpcId(), vpcsWithFlowLogs.contains(v.vpcId())))
                .toList();
    }

    static boolean exposesAdminPorts(SecurityGroup sg) {
        return sg.ipPermissions().stream()
                .filter(Ec2ResourceFetcher::isOpenToWorld)
                .anyMatch(Ec2ResourceFetcher::coversAdminPort);
    }

    private static boolean isOpenToWorld(IpPermission p) {
        return p.ipRanges().stream().anyMatch(r -> "0.0.0.0/0".equals(r.cidrIp()))
                || p.ipv6Ranges().stream().anyMatch(r -> "::/0".equals(r.cidrIpv6()));
    }

    private static boolean coversAdminPort(IpPermission p) {
        // Protocol "-1" (all traffic) has no port range
        if ("-1".equals(p.ipProtocol())) {
            return true;
        }
        if (!"tcp".equals(p.ipProtocol()) || p.fromPort() == null || p.toPort() == null) {
            return false;
        }
        return ADMIN_PORTS.stream().anyMatch(port -> p.fromPort() <= port && port <= p.toPort());
    }
}
