package jvmram.swing.client;

import jvmram.model.graph.GraphPointQueues;
import jvmram.model.metrics.MetricType;
import jvmram.proto.ProcInfo;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface JvmRamBackendClient {
    void gc(long pid);

    void createHeapDump(String filepath);

    List<MetricType> getApplicableMetrics();

    void setVisible(MetricType metricType);

    void setInvisible(MetricType metricType);

    void refreshAvailableJvmProcesses();

    void addAvailableJvmProcessesListener(Consumer<Collection<ProcInfo>> listener);

    List<Long> getExplicitlyFollowingPids();

    void setCurrentlySelectedPids(Collection<Long> pids);

    void includeChildrenProcesses();

    void excludeChildrenProcesses();

    void addGraphChangesListener(Consumer<GraphPointQueues> listener);
}
