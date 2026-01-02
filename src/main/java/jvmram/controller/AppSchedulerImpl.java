package jvmram.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

class AppSchedulerImpl implements AppScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ScheduledExecutorService  guiUpdateExecutor = Executors.newSingleThreadScheduledExecutor(
            runnable -> new Thread(runnable, "gui update")
    );

    private final ScheduledExecutorService procInfoExecutor = Executors.newSingleThreadScheduledExecutor(
            runnable -> new Thread(runnable, "proc info update")
    );

    private AppSchedulerImpl() {
    }

    @Override
    public void start() {
        guiUpdateExecutor.scheduleWithFixedDelay(this::update, 0, 100, MILLISECONDS);
        procInfoExecutor.scheduleWithFixedDelay(this::refresh, 0, 1, SECONDS);
    }

    private void update() {
        var graphController = GraphController.getInstance();
        try {
            graphController.update();
        } catch (Throwable t) {
            LOG.error("Failed to update GUI", t);
        }
    }

    private void refresh() {
        var processController = ProcessController.getInstance();
        try {
            processController.refreshAvailableJvmProcesses();
        } catch (Throwable t) {
            LOG.error("Failed to refresh proc infos", t);
        }
    }

    static final AppSchedulerImpl INSTANCE = new AppSchedulerImpl();
}
