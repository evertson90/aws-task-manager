package com.demo.taskmanager;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class DemoProcessorStack extends Stack {

    public DemoProcessorStack(final Construct scope, final String id, final StackProps props, final EventBus eventBus) {
        super(scope, id, props);

        // DemoProcessor Lambda
        Function demoProcessor = Function.Builder.create(this, "DemoProcessorFunction")
                .runtime(Runtime.JAVA_21)
                .memorySize(512)
                .timeout(Duration.seconds(60))
                .handler("com.demo.taskmanager.lambda.DemoProcessorHandler")
                .code(Code.fromAsset("lambdas/demo-processor/target/demo-processor-1.0.0.jar"))
                .environment(Map.of("EVENT_BUS_NAME", eventBus.getEventBusName()))
                .build();

        System.out.println("test");

        eventBus.grantPutEventsTo(demoProcessor);

        // EventBridge Rule
        Rule taskCreatedRule = Rule.Builder.create(this, "TaskCreatedRule")
                .eventBus(eventBus)
                .eventPattern(EventPattern.builder()
                        .source(List.of("com.demo.taskmanager"))
                        .detailType(List.of("TaskCreatedEvent"))
                        .detail(Map.of("type", List.of("EXPORT")))
                        .build())
                .build();
        taskCreatedRule.addTarget(new LambdaFunction(demoProcessor));
    }
}
