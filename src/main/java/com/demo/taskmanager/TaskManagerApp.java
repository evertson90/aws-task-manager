package com.demo.taskmanager;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class TaskManagerApp {
    public static void main(final String[] args) {
        App app = new App();
        TaskManagerStack taskManager = new TaskManagerStack(app, "TaskManagerStack", StackProps.builder().build());
        new DemoProcessorStack(app, "DemoProcessorStack", StackProps.builder().build(), taskManager.getEventBus());
        app.synth();
    }
}
