package com.demo.taskmanager;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class TaskManagerStack extends Stack {

    private final Table taskStatusTable;
    private final EventBus eventBus;

    public TaskManagerStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // DynamoDB Table
        taskStatusTable = Table.Builder.create(this, "TaskStatusTable")
                .tableName("TaskStatus")
                .partitionKey(Attribute.builder().name("tenantId").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("taskId").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .stream(StreamViewType.NEW_AND_OLD_IMAGES)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Custom EventBus
        eventBus = EventBus.Builder.create(this, "TaskManagerBus")
                .eventBusName("TaskManagerBus")
                .build();

        // TaskCreator Lambda
        Function taskCreator = Function.Builder.create(this, "TaskCreatorFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .handler("com.demo.taskmanager.lambda.TaskCreatorHandler")
                .code(Code.fromAsset("lambdas/task-creator/target/task-creator-1.0.0.jar"))
                .environment(Map.of("TABLE_NAME", taskStatusTable.getTableName()))
                .build();
        taskStatusTable.grantReadWriteData(taskCreator);

        // TaskGetter Lambda
        Function taskGetter = Function.Builder.create(this, "TaskGetterFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .handler("com.demo.taskmanager.lambda.TaskGetterHandler")
                .code(Code.fromAsset("lambdas/task-getter/target/task-getter-1.0.0.jar"))
                .environment(Map.of("TABLE_NAME", taskStatusTable.getTableName()))
                .build();
        taskStatusTable.grantReadData(taskGetter);

        // TaskStatusPublisher Lambda
        Function taskStatusPublisher = Function.Builder.create(this, "TaskStatusPublisherFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(60))
                .handler("com.demo.taskmanager.lambda.TaskStatusPublisherHandler")
                .code(Code.fromAsset("lambdas/task-status-publisher/target/task-status-publisher-1.0.0.jar"))
                .environment(Map.of("EVENT_BUS_NAME", eventBus.getEventBusName()))
                .build();
        eventBus.grantPutEventsTo(taskStatusPublisher);
        taskStatusPublisher.addEventSource(DynamoEventSource.Builder.create(taskStatusTable)
                .startingPosition(StartingPosition.TRIM_HORIZON)
                .batchSize(10)
                .retryAttempts(3)
                .build());

        // SQS Queue
        Queue taskUpdateQueue = Queue.Builder.create(this, "TaskUpdateQueue")
                .queueName("TaskUpdateQueue")
                .visibilityTimeout(Duration.seconds(300))
                .build();

        // EventBridge Rule
        Rule taskUpdatedRule = Rule.Builder.create(this, "TaskUpdatedRule")
                .eventBus(eventBus)
                .eventPattern(EventPattern.builder()
                        .source(List.of("com.demo.taskmanager"))
                        .detailType(List.of("TaskUpdatedEvent"))
                        .build())
                .build();
        taskUpdatedRule.addTarget(new SqsQueue(taskUpdateQueue));

        // TaskStatusUpdater Lambda
        Function taskStatusUpdater = Function.Builder.create(this, "TaskStatusUpdaterFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(60))
                .handler("com.demo.taskmanager.lambda.TaskStatusUpdaterHandler")
                .code(Code.fromAsset("lambdas/task-status-updater/target/task-status-updater-1.0.0.jar"))
                .environment(Map.of("TABLE_NAME", taskStatusTable.getTableName()))
                .build();
        taskStatusTable.grantReadWriteData(taskStatusUpdater);
        taskStatusUpdater.addEventSource(new SqsEventSource(taskUpdateQueue));

        // API Gateway
        RestApi api = RestApi.Builder.create(this, "TaskManagerApi")
                .restApiName("TaskManager API")
                .build();

        var tasks = api.getRoot().addResource("tasks");
        tasks.addMethod("POST", new LambdaIntegration(taskCreator));

        var tenantId = tasks.addResource("{tenantId}");
        var taskId = tenantId.addResource("{taskId}");
        taskId.addMethod("GET", new LambdaIntegration(taskGetter));
    }

    public Table getTaskStatusTable() {
        return taskStatusTable;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
