package jvmram.backend;

import jvmram.controller.AppScheduler;
import jvmram.controller.GraphController;
import jvmram.controller.JmxService;
import jvmram.controller.ProcessController;
import jvmram.model.graph.GraphPointQueuesWritable;
import jvmram.visibility.MetricVisibility;

public class JvmRunCost {
    private volatile JvmRamBackendManager backendManager;

    public void setup(int port) {

        var processController = ProcessController.getInstance();
        var graphController = GraphController.getInstance();
        var graphPointQueues = GraphPointQueuesWritable.getInstance();
        var jmxService = JmxService.getInstance();
        var metricsVisibility = MetricVisibility.getInstance();

        backendManager = new JvmRamBackendManager();
        var backend = new JvmRamBackendImpl(processController, graphController, graphPointQueues, jmxService, metricsVisibility);
        backendManager.start(port, backend);

        var appScheduler = AppScheduler.getInstance();
        appScheduler.start();
    }

    public void blockUntilShutdown() {
        backendManager.blockUntilShutdown();
    }
}
