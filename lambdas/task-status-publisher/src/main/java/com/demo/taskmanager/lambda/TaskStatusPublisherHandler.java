package com.demo.taskmanager.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Map;

public class TaskStatusPublisherHandler implements RequestHandler<DynamodbEvent, Void> {

    private final EventBridgeClient eventBridge = EventBridgeClient.create();
    private final Gson gson = new Gson();
    private static final String SOURCE = "com.demo.taskmanager";
    private static final String EVENT_BUS_NAME = System.getenv("EVENT_BUS_NAME");

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            String eventName = record.getEventName();
            Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();

            if (newImage == null) continue;

            String detailType;
            if ("INSERT".equals(eventName)) {
                detailType = "TaskCreatedEvent";
            } else if ("MODIFY".equals(eventName)) {
                String status = newImage.get("status").getS();
                detailType = switch (status) {
                    case "RUNNING" -> "TaskRunningEvent";
                    case "SUCCESSFUL" -> "TaskSuccessfulEvent";
                    case "FAILED" -> "TaskFailedEvent";
                    default -> null;
                };
                if (detailType == null) continue;
            } else {
                continue;
            }

            JsonObject detail = new JsonObject();
            detail.addProperty("taskId", newImage.get("taskId").getS());
            detail.addProperty("tenantId", newImage.get("tenantId").getS());
            detail.addProperty("type", newImage.get("type").getS());
            detail.addProperty("status", newImage.get("status").getS());
            if (newImage.containsKey("requestPayload")) {
                detail.addProperty("requestPayload", newImage.get("requestPayload").getS());
            }
            if (newImage.containsKey("result")) {
                detail.addProperty("result", newImage.get("result").getS());
            }

            String detailJson = gson.toJson(detail);
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(EVENT_BUS_NAME)
                    .source(SOURCE)
                    .detailType(detailType)
                    .detail(detailJson)
                    .build();

            eventBridge.putEvents(PutEventsRequest.builder()
                    .entries(entry)
                    .build());

            context.getLogger().log("Published " + detailType + " for task " + newImage.get("taskId").getS());
        }
        return null;
    }
}
