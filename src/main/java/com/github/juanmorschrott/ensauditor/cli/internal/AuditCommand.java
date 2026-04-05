package com.github.juanmorschrott.ensauditor.cli.internal;

import com.github.juanmorschrott.ensauditor.compliance.*;
import com.github.juanmorschrott.ensauditor.report.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "audit", description = "Run compliance audit on AWS infrastructure", mixinStandardHelpOptions = true)
public class AuditCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(AuditCommand.class);

    private final ComplianceService complianceService;
    private final ControlRegistry controlRegistry;
    private final ReportService reportService;

    @Option(names = {"-o", "--output"}, description = "Output format: json, table, html, csv (default: ${DEFAULT-VALUE})", defaultValue = "table")
    private String output;

    @Option(names = {"-c", "--control"}, description = "Evaluate a specific control ID only")
    private String controlId;

    @Option(names = {"-s", "--severity"}, description = "Filter by minimum severity: CRITICAL, HIGH, MEDIUM, LOW")
    private String minSeverity;

    @Option(names = {"-f", "--output-file"}, description = "Write report to file instead of stdout")
    private Path outputFile;

    public AuditCommand(ComplianceService complianceService, ControlRegistry controlRegistry, ReportService reportService) {
        this.complianceService = complianceService;
        this.controlRegistry = controlRegistry;
        this.reportService = reportService;
    }

    @Override
    public Integer call() throws IOException {
        List<ControlDefinition> controls = resolveControls();

        if (controls.isEmpty()) {
            System.err.println("No controls matched the given filters. Use 'list-controls' to see available controls.");
            return 0;
        }

        log.info("Running audit of {} controls", controls.size());

        AuditResult auditResult = complianceService.evaluateControls(controls, (current, total) -> {
            int percentage = (int) (((double) current / total) * 100);
            System.err.printf("\rAudit Progress: [%-20s] %d%% (%d/%d)",
                    "=".repeat(percentage / 5), percentage, current, total);
        });
        System.err.print("\r" + " ".repeat(80) + "\r");
        System.err.println("Audit completed.\n");

        String report = formatReport(auditResult);

        if (outputFile != null) {
            Files.writeString(outputFile, report, StandardCharsets.UTF_8);
            System.err.println("Report written to: " + outputFile.toAbsolutePath());
        } else {
            System.out.println(report);
        }

        return auditResult.getOverallCompliance() == ComplianceLevel.COMPLIANT ? 0 : 1;
    }

    private String formatReport(AuditResult auditResult) {
        return switch (output.toLowerCase()) {
            case "json" -> reportService.presentAsJson(auditResult);
            case "table" -> reportService.presentAsTable(auditResult);
            case "html" -> reportService.presentAsHtml(auditResult);
            case "csv" -> reportService.presentAsCsv(auditResult);
            default -> {
                System.err.println("Invalid output format: '" + output + "'. Using table.");
                yield reportService.presentAsTable(auditResult);
            }
        };
    }

    private List<ControlDefinition> resolveControls() {
        if (controlId != null && !controlId.isBlank()) {
            return controlRegistry.getControlById(controlId)
                    .map(List::of)
                    .orElse(List.of());
        }

        List<ControlDefinition> all = controlRegistry.getAllControls();

        SeverityLevel minLevel = SeverityLevel.fromString(minSeverity);
        if (minLevel != null) {
            all = all.stream()
                    .filter(c -> c.severity() != null && c.severity().ordinal() <= minLevel.ordinal())
                    .toList();
        }

        return all;
    }
}
