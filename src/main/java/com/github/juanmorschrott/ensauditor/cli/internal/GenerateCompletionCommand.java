package com.github.juanmorschrott.ensauditor.cli.internal;

import org.springframework.stereotype.Component;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Component
@Command(name = "generate-completion", description = "Generate shell completion script", mixinStandardHelpOptions = true)
public class GenerateCompletionCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        String script = AutoComplete.bash(
                spec.root().name(),
                spec.root().commandLine());
        System.out.println(script);
        return 0;
    }
}
