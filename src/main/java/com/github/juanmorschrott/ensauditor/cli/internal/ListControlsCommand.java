package com.github.juanmorschrott.ensauditor.cli.internal;

import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlRegistry;
import com.github.juanmorschrott.ensauditor.compliance.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "list-controls", description = "List available ENS controls", mixinStandardHelpOptions = true)
public class ListControlsCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ListControlsCommand.class);

    private final ControlRegistry controlRegistry;

    @Option(names = {"-s", "--severity"}, description = "Filter by minimum severity: CRITICAL, HIGH, MEDIUM, LOW")
    private String minSeverity;

    public ListControlsCommand(ControlRegistry controlRegistry) {
        this.controlRegistry = controlRegistry;
    }

    @Override
    public Integer call() {
        List<ControlDefinition> controls = controlRegistry.getAllControls();

        if (minSeverity != null) {
            SeverityLevel minLevel = parseSeverity(minSeverity);
            if (minLevel != null) {
                controls = controls.stream()
                        .filter(c -> c.severity() != null && c.severity().ordinal() <= minLevel.ordinal())
                        .toList();
            }
        }

        if (controls.isEmpty()) {
            System.out.println("No controls found.");
            return 0;
        }

        System.out.printf("%-25s %-10s %-45s%n", "ID", "Severity", "Name");
        System.out.println("-".repeat(80));
        for (ControlDefinition c : controls) {
            System.out.printf("%-25s %-10s %-45s%n",
                    c.controlId(),
                    c.severity() != null ? c.severity() : "",
                    c.name());
        }
        System.out.printf("%nTotal: %d control(s)%n", controls.size());
        return 0;
    }

    private SeverityLevel parseSeverity(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return SeverityLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown severity level '{}', ignoring filter.", value);
            return null;
        }
    }
}
