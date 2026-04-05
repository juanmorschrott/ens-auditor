package com.github.juanmorschrott.ensauditor.shared.config;

import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.AuditResult;
import com.github.juanmorschrott.ensauditor.compliance.ComplianceLevel;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import com.github.juanmorschrott.ensauditor.compliance.SeverityLevel;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Centralized GraalVM native-image runtime hints.
 * Registers resources and reflection metadata required for native compilation.
 */
@Configuration
@ImportRuntimeHints(NativeHintsConfig.Registrar.class)
class NativeHintsConfig {

    static class Registrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Resource files
            hints.resources().registerPattern("ens-controls.yaml");
            hints.resources().registerPattern("logback.xml");
            hints.resources().registerPattern("ch/qos/logback/classic/logback-classic-version.properties");
            hints.resources().registerPattern("ch/qos/logback/core/logback-core-version.properties");

            // JSON serialization targets (used by ReportService)
            hints.reflection().registerType(AuditResult.class, MemberCategory.values());
            hints.reflection().registerType(ControlEvaluationResult.class, MemberCategory.values());
            hints.reflection().registerType(ControlStatus.class, MemberCategory.values());
            hints.reflection().registerType(SeverityLevel.class, MemberCategory.values());
            hints.reflection().registerType(ComplianceLevel.class, MemberCategory.values());
            hints.reflection().registerType(ResourceType.class, MemberCategory.values());
        }
    }
}
