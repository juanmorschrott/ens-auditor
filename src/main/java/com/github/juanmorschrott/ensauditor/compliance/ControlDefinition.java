package com.github.juanmorschrott.ensauditor.compliance;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Represents an ENS control definition from the control registry.
 * Immutable record for representing control specifications.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ControlDefinition(
        String controlId,
        String module,
        String name,
        String description,
        SeverityLevel severity,
        List<String> affectedResourceTypes) {

    public ControlDefinition {
        if (affectedResourceTypes == null) {
            affectedResourceTypes = List.of();
        }
    }
}
