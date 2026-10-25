package com.github.juanmorschrott.ensauditor.aws;

/**
 * @param adminPortsOpenToWorld true when SSH (22) or RDP (3389) is reachable from 0.0.0.0/0 or ::/0
 */
public record SecurityGroupDto(String groupId, String name, String vpcId, boolean adminPortsOpenToWorld) {}
