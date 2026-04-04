package com.github.juanmorschrott.ensauditor.compliance.internal;

import com.github.juanmorschrott.ensauditor.aws.AwsResourceService;
import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import com.github.juanmorschrott.ensauditor.compliance.ControlDefinition;
import com.github.juanmorschrott.ensauditor.compliance.ControlEvaluationResult;
import com.github.juanmorschrott.ensauditor.compliance.ControlStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDBEvaluatorTest {

    @Mock
    private AwsResourceService awsResourceService;

    @InjectMocks
    private DynamoDBEvaluator evaluator;

    private ControlDefinition control(String id) {
        return new ControlDefinition(id, "test", null, null, null, null);
    }

    @Test
    void noTables_returnsCompliant() {
        when(awsResourceService.fetchDynamoDBs()).thenReturn(List.of());
        ControlEvaluationResult result = evaluator.evaluate(control("ens.dynamodb.encryption"));
        assertEquals(ControlStatus.COMPLIANT, result.status());
    }

    @Test
    void allTablesEncryptedWithCmk_returnsCompliant() {
        when(awsResourceService.fetchDynamoDBs()).thenReturn(List.of(
                new DynamoDBDto("t1", Instant.now(), true, "ENABLED", "arn:aws:kms:region:account:key/123", Map.of())
        ));
        ControlEvaluationResult result = evaluator.evaluate(control("ens.dynamodb.encryption"));
        assertEquals(ControlStatus.COMPLIANT, result.status());
    }

    @Test
    void tableEncryptedWithAwsOwnedKey_returnsNonCompliant() {
        when(awsResourceService.fetchDynamoDBs()).thenReturn(List.of(
                new DynamoDBDto("t1", Instant.now(), true, "ENABLED", "arn:aws:kms:region:account:key/123", Map.of()),
                new DynamoDBDto("t2", Instant.now(), false, null, null, Map.of())
        ));
        ControlEvaluationResult result = evaluator.evaluate(control("ens.dynamodb.encryption"));
        assertEquals(ControlStatus.NON_COMPLIANT, result.status());
        assertEquals("1 out of 2 tables are NON-COMPLIANT: t2", result.findings());
    }
}
