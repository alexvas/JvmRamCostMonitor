package jvmram.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

class AppSchedulerImpl implements AppScheduler {

    private AppSchedulerImpl() {
    }

    @Override
    public void start() {
        ThreadFactory tf = runnable -> new Thread(runnable, "graph controller");
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor(tf);
        var graphController = GraphController.getInstance();
        executor.scheduleAtFixedRate(graphController::update, 0, 100, MILLISECONDS);
        var processController = ProcessController.getInstance();
        executor.scheduleAtFixedRate(processController::refreshAvailableJvmProcesses, 0, 1, SECONDS);
    }

    static final AppSchedulerImpl INSTANCE = new AppSchedulerImpl();
}
