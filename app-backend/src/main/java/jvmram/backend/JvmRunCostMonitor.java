package jvmram.backend;

import jvmram.controller.AppScheduler;
import jvmram.controller.GraphController;
import jvmram.controller.JmxService;
import jvmram.controller.ProcessController;
import jvmram.model.graph.GraphPointQueues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class JvmRunCostMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static void main() {
        var main = new JvmRunCostMonitor();
        main.setup(53333);
        main.blockUntilShutdown();
    }

    private volatile JvmRamBackendManager backendManager;

    public void setup(int port) {
        Thread.setDefaultUncaughtExceptionHandler((_, e) -> LOG.error("Unexpected exception: ", e));

        var processController = ProcessController.getInstance();
        var graphController = GraphController.getInstance();
        var graphPointQueues = GraphPointQueues.getInstance();
        var jmxService = JmxService.getInstance();

        backendManager = new JvmRamBackendManager();
        var backend = new JvmRamBackendImpl(processController, graphController, graphPointQueues, jmxService);
        backendManager.start(port, backend);

        var appScheduler = AppScheduler.getInstance();
        appScheduler.start();
    }

    public void blockUntilShutdown() {
        backendManager.blockUntilShutdown();
    }
}
