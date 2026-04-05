package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.juanmorschrott.ensauditor.compliance.ControlRegistry;
import com.github.juanmorschrott.ensauditor.shared.exception.InvalidConfigurationException;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.SeverityLevel;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of control registry.
 * Loads ENS control definitions from YAML configuration at startup.
 */
@Service
@ImportRuntimeHints(YamlControlRegistry.NativeHints.class)
class YamlControlRegistry implements ControlRegistry {

    private final List<ControlDefinition> controls;

    YamlControlRegistry(
            @Value("${ens-auditor.controls-path}") String controlsPath,
            ResourceLoader resourceLoader) {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        yaml.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            Resource resource = resourceLoader.getResource(controlsPath);
            EnsControlsConfig config = yaml.readValue(resource.getInputStream(), EnsControlsConfig.class);
            this.controls = config.modules().stream()
                    .flatMap(m -> m.controls().stream()
                            .map(c -> toControlDefinition(c, m.name())))
                    .toList();
        } catch (IOException e) {
            throw new InvalidConfigurationException("Failed to load ENS controls from: " + controlsPath, e);
        }
    }

    @Override
    public List<ControlDefinition> getAllControls() {
        return controls;
    }

    @Override
    public Optional<ControlDefinition> getControlById(String controlId) {
        return controls.stream()
                .filter(c -> c.controlId().equals(controlId))
                .findFirst();
    }

    private static ControlDefinition toControlDefinition(EnsControlsConfig.ControlConfig c, String moduleName) {
        return new ControlDefinition(
                c.id(),
                moduleName,
                c.name(),
                c.description(),
                SeverityLevel.valueOf(c.severity()),
                c.affectedResources() != null ? c.affectedResources() : List.of()
        );
    }

    static class NativeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("ens-controls.yaml");
            hints.resources().registerPattern("logback.xml");
            hints.resources().registerPattern("ch/qos/logback/classic/logback-classic-version.properties");
            hints.resources().registerPattern("ch/qos/logback/core/logback-core-version.properties");
            hints.reflection().registerType(EnsControlsConfig.class, MemberCategory.values());
            hints.reflection().registerType(EnsControlsConfig.ModuleConfig.class, MemberCategory.values());
            hints.reflection().registerType(EnsControlsConfig.ControlConfig.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.compliance.AuditResult.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.compliance.ControlStatus.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.compliance.SeverityLevel.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.compliance.ComplianceLevel.class, MemberCategory.values());
            hints.reflection().registerType(com.github.juanmorschrott.ensauditor.aws.ResourceType.class, MemberCategory.values());
        }
    }
}
