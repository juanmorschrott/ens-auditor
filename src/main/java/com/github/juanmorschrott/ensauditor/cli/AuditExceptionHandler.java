package com.github.juanmorschrott.ensauditor.cli;

import com.github.juanmorschrott.ensauditor.aws.ResourceFetchException;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationException;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@Component
public class AuditExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        if (ex instanceof ResourceFetchException) {
            commandLine.getErr().println("Error fetching resources: " + ex.getMessage());
            return 10;
        } else if (ex instanceof ControlEvaluationException) {
            commandLine.getErr().println("Error evaluating controls: " + ex.getMessage());
            return 11;
        } else if (ex.getClass().getName().contains("software.amazon.awssdk")) {
            commandLine.getErr().println("AWS Cloud Error: " + ex.getMessage());
            return 12;
        }

        commandLine.getErr().println("Unexpected error: " + ex.getMessage());
        return 1;
    }
}
