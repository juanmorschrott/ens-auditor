package com.github.juanmorschrott.ensauditor.report;

import com.github.juanmorschrott.ensauditor.compliance.AuditResult;

/**
 * Public API for the report module.
 * Formats audit results for various output formats.
 */
public interface ReportService {
    /**
     * Presents an audit result in JSON format.
     * @param result the audit result to present
     * @return JSON string representation
     */
    String presentAsJson(AuditResult result);

    /**
     * Presents an audit result in human-readable table format.
     * @param result the audit result to present
     * @return table formatted string
     */
    String presentAsTable(AuditResult result);

    /**
     * Presents an audit result in HTML format.
     * @param result the audit result to present
     * @return HTML string
     */
    String presentAsHtml(AuditResult result);

    /**
     * Presents an audit result in CSV format.
     * @param result the audit result to present
     * @return CSV string
     */
    String presentAsCsv(AuditResult result);
}
