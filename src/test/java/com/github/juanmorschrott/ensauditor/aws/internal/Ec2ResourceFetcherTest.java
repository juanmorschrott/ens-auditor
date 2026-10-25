package com.github.juanmorschrott.ensauditor.aws.internal;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.Ipv6Range;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ec2ResourceFetcherTest {

    private SecurityGroup sg(IpPermission permission) {
        return SecurityGroup.builder().ipPermissions(permission).build();
    }

    private IpPermission.Builder tcp(int from, int to) {
        return IpPermission.builder().ipProtocol("tcp").fromPort(from).toPort(to);
    }

    private static IpRange world() {
        return IpRange.builder().cidrIp("0.0.0.0/0").build();
    }

    @Test
    void sshOpenToWorld_isFlagged() {
        assertTrue(Ec2ResourceFetcher.exposesAdminPorts(sg(tcp(22, 22).ipRanges(world()).build())));
    }

    @Test
    void portRangeCoveringRdp_isFlagged() {
        assertTrue(Ec2ResourceFetcher.exposesAdminPorts(sg(tcp(3000, 4000).ipRanges(world()).build())));
    }

    @Test
    void allTrafficOpenToWorld_isFlagged() {
        IpPermission all = IpPermission.builder().ipProtocol("-1").ipRanges(world()).build();
        assertTrue(Ec2ResourceFetcher.exposesAdminPorts(sg(all)));
    }

    @Test
    void ipv6OpenToWorld_isFlagged() {
        IpPermission p = tcp(22, 22).ipv6Ranges(Ipv6Range.builder().cidrIpv6("::/0").build()).build();
        assertTrue(Ec2ResourceFetcher.exposesAdminPorts(sg(p)));
    }

    @Test
    void httpsOpenToWorld_isNotFlagged() {
        assertFalse(Ec2ResourceFetcher.exposesAdminPorts(sg(tcp(443, 443).ipRanges(world()).build())));
    }

    @Test
    void sshFromPrivateRange_isNotFlagged() {
        IpRange internal = IpRange.builder().cidrIp("10.0.0.0/8").build();
        assertFalse(Ec2ResourceFetcher.exposesAdminPorts(sg(tcp(22, 22).ipRanges(internal).build())));
    }
}
