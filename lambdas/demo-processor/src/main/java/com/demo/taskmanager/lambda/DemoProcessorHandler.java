package com.demo.taskmanager.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Map;

public class DemoProcessorHandler implements RequestHandler<Map<String, Object>, Void> {

    private final EventBridgeClient eventBridge = EventBridgeClient.create();
    private final Gson gson = new Gson();
    private static final String SOURCE = "com.demo.taskmanager";

    @Override
    @SuppressWarnings("unchecked")
    public Void handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> detail = (Map<String, Object>) event.get("detail");

        String taskId = (String) detail.get("taskId");
        String tenantId = (String) detail.get("tenantId");

        context.getLogger().log("Processing task " + taskId + " for tenant " + tenantId);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishUpdate(taskId, tenantId, "FAILED", null);
            return null;
        }

        String result = "Export completed at " + java.time.Instant.now();
        publishUpdate(taskId, tenantId, "SUCCESSFUL", result);

        context.getLogger().log("Task " + taskId + " completed successfully");
        return null;
    }

    private void publishUpdate(String taskId, String tenantId, String status, String result) {
        JsonObject detail = new JsonObject();
        detail.addProperty("taskId", taskId);
        detail.addProperty("tenantId", tenantId);
        detail.addProperty("status", status);
        if (result != null) {
            detail.addProperty("result", result);
        }

        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .source(SOURCE)
                .detailType("TaskUpdatedEvent")
                .detail(gson.toJson(detail))
                .build();

        eventBridge.putEvents(PutEventsRequest.builder()
                .entries(entry)
                .build());
    }
}
