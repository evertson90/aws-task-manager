package com.demo.taskmanager.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

public class TaskStatusUpdaterHandler implements RequestHandler<SQSEvent, Void> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final Gson gson = new Gson();
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            JsonObject envelope = gson.fromJson(message.getBody(), JsonObject.class);
            JsonObject detail = envelope.getAsJsonObject("detail");

            String taskId = detail.get("taskId").getAsString();
            String tenantId = detail.get("tenantId").getAsString();
            String status = detail.get("status").getAsString();

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":status", AttributeValue.builder().s(status).build());

            String updateExpression = "SET #s = :status";
            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#s", "status");

            if (detail.has("result") && !detail.get("result").isJsonNull()) {
                updateExpression += ", #r = :result";
                expressionNames.put("#r", "result");
                expressionValues.put(":result", AttributeValue.builder().s(detail.get("result").getAsString()).build());
            }

            dynamoDb.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "tenantId", AttributeValue.builder().s(tenantId).build(),
                            "taskId", AttributeValue.builder().s(taskId).build()
                    ))
                    .updateExpression(updateExpression)
                    .expressionAttributeNames(expressionNames)
                    .expressionAttributeValues(expressionValues)
                    .build());

            context.getLogger().log("Updated task " + taskId + " to status " + status);
        }
        return null;
    }
}
