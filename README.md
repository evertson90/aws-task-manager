# TaskManager CDK Demo

Demonstrates the TaskManager pattern for processing long-running events on AWS API Gateway. Based on the blog post "Processing long running events on AWS API Gateway."

## Architecture

API Gateway has a 29-second timeout. This pattern decouples task submission from processing:

1. Client POSTs a task -> gets back a taskId immediately
2. DynamoDB Stream triggers EventBridge events
3. Processor picks up the event, does work, reports back via EventBridge
4. Client polls GET endpoint for status updates

### AWS Services

- API Gateway (REST) - task submission and polling
- DynamoDB - task state with streams
- EventBridge - event routing
- SQS - buffering processor updates
- Lambda (Java 21) - all compute

## Prerequisites

- Java 21 (install via `sdk install java 21.0.10-amzn`)
- Maven (install via `sdk install maven`)
- AWS CDK CLI (`npm install -g aws-cdk`)
- AWS credentials configured

## Build

```bash
# Build all Lambda JARs
mvn package -pl lambdas/task-creator,lambdas/task-getter,lambdas/task-status-publisher,lambdas/task-status-updater,lambdas/demo-processor

# Synthesize CloudFormation
cdk synth
```

## Deploy

```bash
cdk deploy --all
```

## Test

```bash
# Create a task
curl -X POST https://<api-id>.execute-api.<region>.amazonaws.com/prod/tasks \
  -H "Content-Type: application/json" \
  -d '{"tenantId": "tenant-1", "type": "EXPORT", "requestPayload": {"query": "SELECT *"}}'

# Poll for status (use taskId from response above)
curl https://<api-id>.execute-api.<region>.amazonaws.com/prod/tasks/tenant-1/<taskId>
```

## Cleanup

```bash
cdk destroy --all
```
