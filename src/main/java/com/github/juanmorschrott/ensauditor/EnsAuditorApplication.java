package com.github.juanmorschrott.ensauditor;

import com.github.juanmorschrott.ensauditor.cli.AuditExceptionHandler;
import com.github.juanmorschrott.ensauditor.cli.EnsAuditorCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@Modulith
@SpringBootApplication
public class EnsAuditorApplication implements CommandLineRunner, ExitCodeGenerator {

    static {
        System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");
    }

    private final EnsAuditorCommand ensAuditorCommand;
    private final AuditExceptionHandler exceptionHandler;
    private final IFactory factory;
    private int exitCode;

    public EnsAuditorApplication(EnsAuditorCommand ensAuditorCommand, AuditExceptionHandler exceptionHandler, IFactory factory) {
        this.ensAuditorCommand = ensAuditorCommand;
        this.exceptionHandler = exceptionHandler;
        this.factory = factory;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(EnsAuditorApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(ensAuditorCommand, factory)
                .setExecutionExceptionHandler(exceptionHandler)
                .execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
