package com.github.juanmorschrott.ensauditor.cli;

import com.github.juanmorschrott.ensauditor.cli.internal.AuditCommand;
import com.github.juanmorschrott.ensauditor.cli.internal.ListControlsCommand;
import com.github.juanmorschrott.ensauditor.cli.internal.StatusCommand;
import com.github.juanmorschrott.ensauditor.cli.internal.GenerateCompletionCommand;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(
        name = "ens-auditor",
        description = "AWS compliance auditor for Spanish ENS (Esquema Nacional de Seguridad)",
        mixinStandardHelpOptions = true,
        version = "ens-auditor 0.2.0",
        subcommands = {
                AuditCommand.class,
                ListControlsCommand.class,
                StatusCommand.class,
                GenerateCompletionCommand.class
        }
)
public class EnsAuditorCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Use --help to see available commands.");
    }
}
