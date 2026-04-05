package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.SSEDescription;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
class DynamoDBResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBResourceFetcher.class);

    private final DynamoDbClient dynamoDbClient;
    private final ExecutorService executorService;

    public DynamoDBResourceFetcher(DynamoDbClient dynamoDbClient, ExecutorService executorService) {
        this.dynamoDbClient = dynamoDbClient;
        this.executorService = executorService;
    }

    public List<DynamoDBDto> fetchTables() {
        ListTablesResponse listResponse = dynamoDbClient.listTables();
        List<String> tableNames = listResponse.tableNames();

        List<CompletableFuture<DynamoDBDto>> futures = tableNames.stream()
                .map(tableName -> CompletableFuture.supplyAsync(() -> fetchTable(tableName), executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public DynamoDBDto fetchTable(String tableName) {
        try {
            DescribeTableResponse response = dynamoDbClient.describeTable(b -> b.tableName(tableName));
            TableDescription table = response.table();
            
            boolean sseEnabled = false;
            String sseStatus = null;
            String kmsKeyArn = null;

            SSEDescription sseDescription = table.sseDescription();
            if (sseDescription != null) {
                sseStatus = sseDescription.statusAsString();
                sseEnabled = "ENABLING".equals(sseStatus) || "ENABLED".equals(sseStatus) || "UPDATING".equals(sseStatus);
                kmsKeyArn = sseDescription.kmsMasterKeyArn();
            }

            return new DynamoDBDto(
                    table.tableName(),
                    table.creationDateTime(),
                    sseEnabled,
                    sseStatus,
                    kmsKeyArn,
                    Map.of()
            );
        } catch (DynamoDbException e) {
            log.error("Failed to describe table {}: {}", tableName, e.getMessage());
            return new DynamoDBDto(tableName, Instant.now(), false, null, null, Map.of());
        }
    }

}
