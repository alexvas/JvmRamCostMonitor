
package jvmram.controller.impl;

import jvmram.conf.Config;
import jvmram.controller.GraphController;
import jvmram.controller.GraphRenderer;
import jvmram.controller.ProcessController;
import jvmram.metrics.GraphPoint;
import jvmram.metrics.MetricsFactory;
import jvmram.model.graph.GraphPointQueues;
import jvmram.model.graph.MetricVisibility;
import jvmram.model.metrics.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.synchronizedList;

public class GraphControllerImpl implements GraphController {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MetricVisibility metricVisibility = MetricVisibility.getInstance();
    private final MetricsFactory metricsFactory = MetricsFactory.getInstance();
    private final ProcessController processController = ProcessController.getInstance();
    private final GraphPointQueues graphPointQueues = GraphPointQueues.getInstance();

    private final List<GraphRenderer> renderers = synchronizedList(new ArrayList<>());

    @Override
    public void update() {
        LOG.trace("general update");
        var followingPids = processController.getPidsWithDescendants();
        followingPids.forEach(this::update);
    }

    private void update(long pid) {
        LOG.trace("updating pid {}", pid);
        var metrics = metricsFactory.getOrCreateMetrics(pid, Config.os);

        var exceeds = new ArrayList<GraphPoint>();
        boolean relevantUpdate = false;
        var effectiveMetrics = Arrays.stream(MetricType.values())
                .filter(it -> it.isApplicable(Config.os) && metricVisibility.isVisible(it))
                .toList();
        LOG.trace("effective metrics: {}", effectiveMetrics);
        for (var mt : effectiveMetrics) {
            var ramMetric = metrics.get(mt);
            var point = ramMetric.getGraphPoint();

            if (point.isRedundant()) {
                continue;
            }

            relevantUpdate = true;


            var exceed = graphPointQueues.add(pid, mt, point);
            exceeds.addAll(exceed);
        }

        if (!exceeds.isEmpty()) {
            graphPointQueues.handleExceed(exceeds);
        }

        if (relevantUpdate) {
            LOG.trace("Repainting after the relevant update of pid {}", pid);
            renderers.forEach(GraphRenderer::repaintAsync);
        }
    }

    @Override
    public void addRenderer(GraphRenderer renderer) {
        this.renderers.add(renderer);
    }

    private GraphControllerImpl() {
    }

    public static final GraphControllerImpl INSTANCE = new GraphControllerImpl();
}
