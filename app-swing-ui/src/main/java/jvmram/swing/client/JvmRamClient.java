package jvmram.swing.client;

import jvmram.model.graph.GraphPointQueues;
import jvmram.model.metrics.MetricType;
import jvmram.proto.ProcInfo;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface JvmRamClient {
    void gc(long pid);

    void createHeapDump(String filepath);

    List<MetricType> getApplicableMetrics();

    void setVisible(MetricType metricType);

    void setInvisible(MetricType metricType);

    void refreshAvailableJvmProcesses();

    void addAvailableJvmProcessesListener(Consumer<Collection<ProcInfo>> listener);

    List<Long> getExplicitlyFollowingPids();

    void setFollowingPids(Collection<Long> pids);

    boolean areChildrenProcessesIncluded();

    void includeChildrenProcesses();

    void excludeChildrenProcesses();

    void addGraphChangesListener(Consumer<GraphPointQueuesMiniMax> listener);
}
