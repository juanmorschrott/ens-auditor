package com.github.juanmorschrott.ensauditor.compliance;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete ENS compliance audit result.
 * Mutable class for building audit results progressively.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditResult {
    private final String auditId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<ControlEvaluationResult> controlResults;
    private final Map<String, ComplianceLevel> moduleLevels;
    private ComplianceLevel overallCompliance;
    private String awsAccount;
    private String awsRegion;

    public AuditResult() {
        this.controlResults = new ArrayList<>();
        this.moduleLevels = new HashMap<>();
        this.startTime = LocalDateTime.now();
        this.auditId = UUID.randomUUID().toString();
    }

    public void addControlResult(ControlEvaluationResult result) {
        this.controlResults.add(result);
    }

    public void calculateComplianceLevels() {
        // Group results by module (extracted from controlId format: MODULE.CONTROL)
        Map<String, List<ControlEvaluationResult>> resultsByModule = controlResults.stream()
                .collect(Collectors.groupingBy(this::extractModule));

        // Calculate compliance level per module
        resultsByModule.forEach((module, results) -> {
            long compliant = results.stream()
                    .filter(r -> r.status() == ControlStatus.COMPLIANT)
                    .count();
            double percentage = (double) compliant / results.size();
            moduleLevels.put(module, ComplianceLevel.fromCompliancePercentage(percentage));
        });

        // Calculate overall compliance
        long totalCompliant = controlResults.stream()
                .filter(r -> r.status() == ControlStatus.COMPLIANT)
                .count();
        double overallPercentage = (double) totalCompliant / controlResults.size();
        this.overallCompliance = ComplianceLevel.fromCompliancePercentage(overallPercentage);

        this.endTime = LocalDateTime.now();
    }

    private String extractModule(ControlEvaluationResult result) {
        return result.module() != null ? result.module() : "UNKNOWN";
    }

    public String getAuditId() {
        return auditId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<ControlEvaluationResult> getControlResults() {
        return controlResults;
    }

    public Map<String, ComplianceLevel> getModuleLevels() {
        return moduleLevels;
    }

    public ComplianceLevel getOverallCompliance() {
        return overallCompliance;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public void setAwsAccount(String awsAccount) {
        this.awsAccount = awsAccount;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @Override
    public String toString() {
        return "AuditResult{" +
                "auditId='" + auditId + '\'' +
                ", overallCompliance=" + overallCompliance +
                ", controlResults=" + controlResults.size() +
                '}';
    }
}
