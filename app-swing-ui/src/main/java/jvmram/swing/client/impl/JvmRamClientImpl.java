package jvmram.swing.client.impl;

import io.grpc.Channel;
import jvmram.model.graph.GraphPointQueues;
import jvmram.model.metrics.MetricType;
import jvmram.proto.AppBackendGrpc;
import jvmram.proto.ProcInfo;
import jvmram.swing.client.JvmRamClient;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class JvmRamClientImpl implements JvmRamClient {

    private final AppBackendGrpc.AppBackendBlockingStub blockingStub;

    public JvmRamClientImpl(Channel channel) {
        blockingStub = AppBackendGrpc.newBlockingStub(channel);
    }

    @Override
    public void gc(long pid) {

    }

    @Override
    public void createHeapDump(String filepath) {

    }

    @Override
    public List<MetricType> getApplicableMetrics() {
        return List.of();
    }

    @Override
    public void setVisible(MetricType metricType) {

    }

    @Override
    public void setInvisible(MetricType metricType) {

    }

    @Override
    public void refreshAvailableJvmProcesses() {

    }

    @Override
    public void addAvailableJvmProcessesListener(Consumer<Collection<ProcInfo>> listener) {

    }

    @Override
    public List<Long> getExplicitlyFollowingPids() {
        return List.of();
    }

    @Override
    public void setCurrentlySelectedPids(Collection<Long> pids) {

    }

    @Override
    public void includeChildrenProcesses() {

    }

    @Override
    public void excludeChildrenProcesses() {

    }

    @Override
    public void addGraphChangesListener(Consumer<GraphPointQueues> listener) {

    }
}
