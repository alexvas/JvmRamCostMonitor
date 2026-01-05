package jvmram.backend;

import jvmram.controller.AppScheduler;
import jvmram.controller.GraphController;
import jvmram.controller.JmxService;
import jvmram.controller.ProcessController;
import jvmram.model.graph.GraphPointQueues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class JvmRunCostMonitorMain {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static void main() {
        Thread.setDefaultUncaughtExceptionHandler((_, e) -> LOG.error("Unexpected exception: ", e));

        var processController = ProcessController.getInstance();
        var graphController = GraphController.getInstance();
        var graphPointQueues = GraphPointQueues.getInstance();
        var jmxService = JmxService.getInstance();

        var backendManager = new JvmRamBackendManager();
        var backend = new JvmRamBackendImpl(processController, graphController, graphPointQueues, jmxService);
        backendManager.start(50553, backend);
        backendManager.blockUntilShutdown();

        var appScheduler = AppScheduler.getInstance();
        appScheduler.start();

    }
}
