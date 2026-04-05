package com.github.juanmorschrott.ensauditor.report.internal;

import com.github.juanmorschrott.ensauditor.report.ReportService;
import com.github.juanmorschrott.ensauditor.compliance.AuditResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import com.github.juanmorschrott.ensauditor.shared.util.JsonUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of report service.
 * Formats audit results for various output formats.
 */
@Service
class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String presentAsJson(AuditResult result) {
        return JsonUtils.toJson(result);
    }

    @Override
    public String presentAsTable(AuditResult result) {

        List<ControlEvaluationResult> controls = result.getControlResults();

        // Column widths
        int idW       = 25;
        int statusW   = 18;
        int severityW = 10;
        int findingsW = 65;

        String line = "+" + "-".repeat(idW + 2)
                + "+" + "-".repeat(statusW + 2)
                + "+" + "-".repeat(severityW + 2)
                + "+" + "-".repeat(findingsW + 2) + "+";

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("""
                ╔════════════════════════════════════════════╗
                ║     ENS Auditor - Compliance Report        ║
                ╚════════════════════════════════════════════╝
                """);

        if (result.getStartTime() != null) {
            sb.append("  Date       : ").append(result.getStartTime().format(TIMESTAMP_FMT)).append('\n');
        }
        if (result.getAwsAccount() != null) {
            sb.append("  AWS Account: ").append(result.getAwsAccount()).append('\n');
        }
        if (result.getAwsRegion() != null) {
            sb.append("  AWS Region : ").append(result.getAwsRegion()).append('\n');
        }
        if (result.getOverallCompliance() != null) {
            sb.append("  Overall    : ").append(result.getOverallCompliance()).append('\n');
        }
        sb.append('\n');

        // Table
        sb.append(line).append('\n');
        sb.append(row("Control ID", "Status", "Severity", "Findings", idW, statusW, severityW, findingsW)).append('\n');
        sb.append(line).append('\n');

        if (controls == null || controls.isEmpty()) {
            sb.append("  No control results available.\n");
        } else {
            for (ControlEvaluationResult r : controls) {
                sb.append(row(
                        nvl(r.controlId()),
                        nvl(r.status()),
                        nvl(r.severity()),
                        nvl(r.findings()),
                        idW, statusW, severityW, findingsW
                )).append('\n');
            }
        }
        sb.append(line).append('\n');

        // Module summary
        if (result.getModuleLevels() != null && !result.getModuleLevels().isEmpty()) {
            sb.append("\n  Module compliance:\n");
            result.getModuleLevels().forEach((module, level) ->
                    sb.append("    ").append(module).append(": ").append(level).append('\n'));
        }

        // Counts
        if (controls != null) {
            long compliant = controls.stream().filter(r -> r.status() == ControlStatus.COMPLIANT).count();
            long nonCompliant = controls.stream().filter(r -> r.status() == ControlStatus.NON_COMPLIANT).count();
            long notEvaluated = controls.stream().filter(r -> r.status() == ControlStatus.NOT_EVALUATED).count();
            sb.append("\n  COMPLIANT: ").append(compliant)
                    .append("  |  NON-COMPLIANT: ").append(nonCompliant)
                    .append("  |  NOT EVALUATED: ").append(notEvaluated)
                    .append('\n');
        }

        return sb.toString();
    }

    private static String row(Object id, Object status, Object severity, Object findings,
                              int idW, int statusW, int severityW, int findingsW) {
        return "| " + pad(id, idW)
                + " | " + pad(status, statusW)
                + " | " + pad(severity, severityW)
                + " | " + pad(findings, findingsW) + " |";
    }

    private static String pad(Object value, int width) {
        String s = value == null ? "" : value.toString();
        if (s.length() > width) s = s.substring(0, width - 1) + "…";
        return String.format("%-" + width + "s", s);
    }

    @Override
    public String presentAsHtml(AuditResult result) {
        List<ControlEvaluationResult> controls = result.getControlResults();

        String rows = controls == null ? "" : controls.stream()
                                              .map(r -> {
                                                  String css = switch (r.status()) {
                                                      case COMPLIANT     -> "compliant";
                                                      case NON_COMPLIANT -> "noncompliant";
                                                      default            -> "unevaluated";
                                                  };
                                                  return "<tr class=\"" + css + "\">"
                                                         + "<td>" + esc(r.controlId()) + "</td>"
                                                         + "<td>" + esc(r.controlName()) + "</td>"
                                                         + "<td>" + esc(r.status()) + "</td>"
                                                         + "<td>" + esc(r.severity()) + "</td>"
                                                         + "<td>" + esc(r.findings()) + "</td>"
                                                         + "</tr>";
                                              })
                                              .collect(Collectors.joining("\n"));

        String moduleRows = result.getModuleLevels() == null
                ? ""
                : result.getModuleLevels().entrySet().stream()
                  .map(e -> "<tr><td>" + esc(e.getKey()) + "</td><td>" + esc(e.getValue()) + "</td></tr>")
                  .collect(Collectors.joining("\n"));

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>ENS Audit Report</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; color: #333; }
                    h1   { color: #1a3a5c; }
                    table { border-collapse: collapse; width: 100%%; margin-bottom: 2rem; }
                    th, td { border: 1px solid #ccc; padding: 8px 12px; text-align: left; }
                    th { background: #1a3a5c; color: #fff; }
                    tr.compliant    td { background: #e6f4ea; }
                    tr.noncompliant td { background: #fce8e8; }
                    tr.unevaluated  td { background: #fdf6e3; }
                    .summary p { margin: 0.3rem 0; }
                    .badge { display: inline-block; padding: 2px 8px; border-radius: 4px;
                             font-weight: bold; font-size: 0.85em; }
                    .badge.COMPLIANT    { background: #34a853; color: #fff; }
                    .badge.CRITICAL     { background: #ea4335; color: #fff; }
                    .badge.HIGH         { background: #ff6d00; color: #fff; }
                    .badge.MEDIUM       { background: #fbbc05; color: #333; }
                    .badge.LOW          { background: #4285f4; color: #fff; }
                  </style>
                </head>
                <body>
                  <h1>ENS Auditor &mdash; Compliance Report</h1>
                  <div class="summary">
                    %s%s%s%s
                  </div>
                  <h2>Control Results</h2>
                  <table>
                    <thead>
                      <tr><th>Control ID</th><th>Name</th><th>Status</th><th>Severity</th><th>Findings</th></tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <h2>Module Summary</h2>
                  <table>
                    <thead><tr><th>Module</th><th>Compliance Level</th></tr></thead>
                    <tbody>%s</tbody>
                  </table>
                </body>
                </html>
                """.formatted(
                result.getStartTime() != null
                        ? "<p><strong>Date:</strong> " + result.getStartTime().format(TIMESTAMP_FMT) + "</p>" : "",
                result.getAwsAccount() != null
                        ? "<p><strong>AWS Account:</strong> " + esc(result.getAwsAccount()) + "</p>" : "",
                result.getAwsRegion() != null
                        ? "<p><strong>AWS Region:</strong> " + esc(result.getAwsRegion()) + "</p>" : "",
                result.getOverallCompliance() != null
                        ? "<p><strong>Overall:</strong> <span class=\"badge " + result.getOverallCompliance() + "\">"
                          + result.getOverallCompliance() + "</span></p>" : "",
                rows,
                moduleRows
        );
    }

    @Override
    public String presentAsCsv(AuditResult result) {
        List<ControlEvaluationResult> controls = result.getControlResults();

        StringBuilder sb = new StringBuilder();
        sb.append("controlId,controlName,status,severity,resourceId,resourceType,findings,evaluatedAt\n");

        if (controls != null) {
            for (ControlEvaluationResult r : controls) {
                sb.append(csvField(r.controlId())).append(',')
                        .append(csvField(r.controlName())).append(',')
                        .append(csvField(r.status())).append(',')
                        .append(csvField(r.severity())).append(',')
                        .append(csvField(r.resourceId())).append(',')
                        .append(csvField(r.resourceType())).append(',')
                        .append(csvField(r.findings())).append(',')
                        .append(csvField(r.evaluatedAt())).append('\n');
            }
        }

        return sb.toString();
    }

    private static String nvl(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String esc(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String csvField(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
