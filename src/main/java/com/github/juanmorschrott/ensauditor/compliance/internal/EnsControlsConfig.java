package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Internal model for deserializing ens-controls.yaml.
 */
record EnsControlsConfig(List<ModuleConfig> modules) {

    record ModuleConfig(
            String name,
            @JsonProperty("display_name") String displayName,
            List<ControlConfig> controls) {}

    record ControlConfig(
            String id,
            String name,
            String description,
            String severity,
            boolean automatable,
            @JsonProperty("affected_resources") List<String> affectedResources,
            List<String> evaluators) {}
}
