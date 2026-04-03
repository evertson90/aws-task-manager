package com.demo.taskmanager.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;

public class TaskGetterHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final Gson gson = new Gson();
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> pathParams = request.getPathParameters();
        String tenantId = pathParams.get("tenantId");
        String taskId = pathParams.get("taskId");

        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "tenantId", AttributeValue.builder().s(tenantId).build(),
                        "taskId", AttributeValue.builder().s(taskId).build()
                ))
                .build());

        if (!response.hasItem()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\": \"Task not found\"}");
        }

        Map<String, AttributeValue> item = response.item();
        JsonObject result = new JsonObject();
        result.addProperty("taskId", item.get("taskId").s());
        result.addProperty("tenantId", item.get("tenantId").s());
        result.addProperty("status", item.get("status").s());
        result.addProperty("type", item.get("type").s());
        if (item.containsKey("result")) {
            result.addProperty("result", item.get("result").s());
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(gson.toJson(result));
    }
}
