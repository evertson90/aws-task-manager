package com.demo.taskmanager.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskCreatorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final Gson gson = new Gson();
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        JsonObject body = gson.fromJson(request.getBody(), JsonObject.class);

        String tenantId = body.get("tenantId").getAsString();
        String type = body.get("type").getAsString();
        String requestPayload = body.has("requestPayload") ? body.get("requestPayload").toString() : "{}";
        String taskId = UUID.randomUUID().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("tenantId", AttributeValue.builder().s(tenantId).build());
        item.put("taskId", AttributeValue.builder().s(taskId).build());
        item.put("status", AttributeValue.builder().s("PENDING").build());
        item.put("type", AttributeValue.builder().s(type).build());
        item.put("requestPayload", AttributeValue.builder().s(requestPayload).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        JsonObject response = new JsonObject();
        response.addProperty("taskId", taskId);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(gson.toJson(response));
    }
}
