package com.github.juanmorschrott.ensauditor.cli.internal;

import com.github.juanmorschrott.ensauditor.compliance.ControlRegistry;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Component
@Command(name = "status", description = "Show audit status and compliance summary", mixinStandardHelpOptions = true)
public class StatusCommand implements Callable<Integer> {

    private final ControlRegistry controlRegistry;

    public StatusCommand(ControlRegistry controlRegistry) {
        this.controlRegistry = controlRegistry;
    }

    @Override
    public Integer call() {
        int controlCount = controlRegistry.getAllControls().size();
        System.out.println("""
                ╔════════════════════════════════════════════╗
                ║       ENS Auditor - Compliance Status      ║
                ╚════════════════════════════════════════════╝
                
                Registry loaded: %d control(s) available.
                
                Run 'ens-auditor audit' to start a compliance evaluation.
                Run 'ens-auditor list-controls' to see all available controls.
                """.formatted(controlCount));
        return 0;
    }
}
