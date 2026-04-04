package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import com.github.juanmorschrott.ensauditor.aws.ResourceType;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluator for DynamoDB-related ENS controls.
 */
@Component
public class DynamoDBEvaluator implements ResourceEvaluator {

    private final AwsResourceService awsResourceService;

    public DynamoDBEvaluator(AwsResourceService awsResourceService) {
        this.awsResourceService = awsResourceService;
    }

    @Override
    public ControlEvaluationResult evaluate(ControlDefinition control) {
        
        List<DynamoDBDto> tables = awsResourceService.fetchDynamoDBs();

        if (tables == null || tables.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "No DynamoDB tables found. Inherently compliant.");
        }

        List<DynamoDBDto> nonCompliantTables = getNonCompliantTables(control.controlId(), tables);

        if (nonCompliantTables == null) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.NOT_EVALUATED,
                    "Control ID not mapped for DynamoDB evaluation.");
        }

        if (nonCompliantTables.isEmpty()) {
            return new ControlEvaluationResult(control.controlId(), ControlStatus.COMPLIANT,
                    "All " + tables.size() + " tables are compliant.");
        }

        String failedNames = nonCompliantTables.stream()
                .map(DynamoDBDto::name)
                .collect(Collectors.joining(", "));

        return new ControlEvaluationResult(control.controlId(), ControlStatus.NON_COMPLIANT,
                nonCompliantTables.size() + " out of " + tables.size() + " tables are NON-COMPLIANT: "
                        + failedNames);
    }

    private List<DynamoDBDto> getNonCompliantTables(String controlId, List<DynamoDBDto> tables) {
        return switch (controlId) {
            case "ens.dynamodb.encryption" -> tables.stream()
                    .filter(t -> !Boolean.TRUE.equals(t.sseEnabled()) || t.sseDescriptionKmsMasterKeyArn() == null)
                    .toList();
            default -> null;
        };
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.DYNAMODB_TABLE;
    }

    @Override
    public String getName() {
        return "DynamoDBEvaluator";
    }
}
