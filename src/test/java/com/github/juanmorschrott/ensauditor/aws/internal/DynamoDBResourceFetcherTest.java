package com.github.juanmorschrott.ensauditor.aws.internal;

import com.github.juanmorschrott.ensauditor.aws.DynamoDBDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.SSEDescription;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDBResourceFetcherTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDBResourceFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new DynamoDBResourceFetcher(dynamoDbClient, Executors.newSingleThreadExecutor());
    }

    @Test
    void fetchTables_returnsDtoList() {
        when(dynamoDbClient.listTables()).thenReturn(ListTablesResponse.builder()
                .tableNames("table1", "table2")
                .build());

        when(dynamoDbClient.describeTable(any(java.util.function.Consumer.class)))
                .thenAnswer(invocation -> {
                    return DescribeTableResponse.builder()
                            .table(TableDescription.builder()
                                    .tableName("mockedTable")
                                    .creationDateTime(Instant.now())
                                    .sseDescription(SSEDescription.builder()
                                            .status("ENABLED")
                                            .kmsMasterKeyArn("arn:aws:kms:region:account:key/123")
                                            .build())
                                    .build())
                            .build();
                });

        List<DynamoDBDto> tables = fetcher.fetchTables();

        assertEquals(2, tables.size());
        assertTrue(tables.get(0).sseEnabled());
        assertEquals("arn:aws:kms:region:account:key/123", tables.get(0).sseDescriptionKmsMasterKeyArn());
    }

    @Test
    void fetchTable_withNoSse_returnsUnencrypted() {
        when(dynamoDbClient.describeTable(any(java.util.function.Consumer.class)))
                .thenAnswer(invocation -> DescribeTableResponse.builder()
                        .table(TableDescription.builder()
                                .tableName("table1")
                                .creationDateTime(Instant.now())
                                // No SSEDescription
                                .build())
                        .build());

        DynamoDBDto table = fetcher.fetchTable("table1");

        assertFalse(table.sseEnabled());
        assertEquals("table1", table.name());
    }
}
