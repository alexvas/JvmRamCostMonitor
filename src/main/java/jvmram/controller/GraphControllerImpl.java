
package jvmram.controller;

import jvmram.Config;
import jvmram.metrics.MetricsFactory;
import jvmram.model.graph.GraphPointQueues;
import jvmram.model.graph.MetricVisibility;
import jvmram.model.graph.UpdateResult;
import jvmram.model.metrics.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.synchronizedList;

class GraphControllerImpl implements GraphController {

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

        var exceeds = new ArrayList<UpdateResult.Exceed>();
        boolean relevantUpdate = false;
        for (MetricType mt : MetricType.values()) {
            if (!mt.isApplicable(Config.os) || !metricVisibility.isVisible(mt)) {
                continue;
            }
            var ramMetric = metrics.get(mt);
            var point = ramMetric.getGraphPoint();
            var updateResult = graphPointQueues.add(pid, mt, point);
            @SuppressWarnings("unused") // избыточный результат для полноты перебора в switch expression
            Void exhaustedResult = switch (updateResult) {
                case UpdateResult.Exceed ex -> {
                    relevantUpdate = true;
                    exceeds.add(ex);
                    yield null;
                }
                case UpdateResult.Signal signal -> switch (signal) {
                    case REDUNDANT_UPDATE -> null;
                    case UPDATE_WITHIN_BONDS -> {
                        relevantUpdate = true;
                        yield null;
                    }
                };
            };
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

    static GraphControllerImpl INSTANCE = new GraphControllerImpl();
}
