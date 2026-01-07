package jvmram.swing.client.impl;

import com.google.common.base.Function;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import jvmram.model.metrics.MetricType;
import jvmram.proto.*;
import jvmram.proto.AppBackendGrpc.AppBackendBlockingStub;
import jvmram.swing.client.GraphPointQueuesMiniMax;
import jvmram.swing.client.JvmRamClient;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static jvmram.swing.client.impl.Converter.convert2Grpc;
import static jvmram.swing.client.impl.Converter.fromGrpc;

public class JvmRamClientImpl implements JvmRamClient {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final java.time.Duration RPC_DEADLINE = java.time.Duration.ofSeconds(1);

    private static final Empty EMPTY = Empty.newBuilder().build();

    private final ObserverAdapter<JvmProcessListResponse, Collection<ProcInfo>> procObserver = ObserverAdapter.create(JvmProcessListResponse::getInfosList);

    private final ObserverAdapter<GraphQueues, GraphPointQueuesMiniMax> graphObserver = ObserverAdapter.create(this::convertAndCacheQueue);

    private final AppBackendBlockingStub blockingStub;

    public JvmRamClientImpl(Channel channel) {
        blockingStub = AppBackendGrpc.newBlockingStub(channel);

        var stub = AppBackendGrpc.newStub(channel);
        stub.listenJvmProcessList(EMPTY, procObserver);
        stub.listenGraphQueues(EMPTY, graphObserver);
    }

    @Override
    public void gc(long pid) {
        var request = Pid.newBuilder().setPid(pid).build();
        exec(stub -> stub.triggerGc(request));
    }

    @Override
    public void createHeapDump(String filepath) {
        var request = File.newBuilder().setFileName(filepath).build();
        exec(stub -> stub.dumpHeap(request));
    }

    @Override
    public List<MetricType> getApplicableMetrics() {
        var response = exec(stub -> stub.getApplicableMetrics(EMPTY));
        return response == null
                ? List.of()
                : response.getTypesList()
                .stream()
                .map(Converter::fromGrpc)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void setVisible(MetricType input) {
        var output = convert2Grpc(input);
        var request = SetVisibleRequest.newBuilder().setMetricType(output).build();
        exec(stub -> stub.setVisible(request));
    }

    @Override
    public void setInvisible(MetricType input) {
        var output = convert2Grpc(input);
        var request = SetInvisibleRequest.newBuilder().setMetricType(output).build();
        exec(stub -> stub.setInvisible(request));
    }

    @Override
    public void refreshAvailableJvmProcesses() {
        exec(stub -> stub.refreshAvailableJvmProcesses(EMPTY));
    }

    @Override
    public void addAvailableJvmProcessesListener(Consumer<Collection<ProcInfo>> input) {
        procObserver.addListener(input);
    }

    @Override
    public List<Long> getExplicitlyFollowingPids() {
        var response = exec(stub -> stub.getExplicitlyFollowingPids(EMPTY));
        return convert2Grpc(response);
    }

    @Override
    public void setFollowingPids(Collection<Long> input) {
        var output = convert2Grpc(input);
        exec(stub -> stub.setFollowingPids(output));
    }

    @Override
    public boolean areChildrenProcessesIncluded() {
        var response = exec(stub -> stub.areChildrenProcessesIncluded(EMPTY));
        return response != null && response.getAreIncluded();
    }

    @Override
    public void includeChildrenProcesses() {
        exec(stub -> stub.includeChildrenProcesses(EMPTY));
    }

    @Override
    public void excludeChildrenProcesses() {
        exec(stub -> stub.excludeChildrenProcesses(EMPTY));
    }


    @Override
    public void addGraphChangesListener(Consumer<GraphPointQueuesMiniMax> listener) {
        graphObserver.addListener(listener);
    }

    private <T> T exec(Function<@NonNull AppBackendBlockingStub, T> function) {
        try {
            var stub = blockingStub.withDeadlineAfter(RPC_DEADLINE);
            return function.apply(stub);
        } catch (StatusRuntimeException e) {
            LOG.warn("RPC call {} failed with status: {}", e.getMessage(), e.getStatus());
            return null;
        }
    }

    private final GraphStore graphStore = new GraphStore();

    private GraphPointQueuesMiniMax convertAndCacheQueue(GraphQueues input) {
        long pid = input.getPid();
        input.getQueuesList().forEach(queue ->
        {
            var mt = fromGrpc(queue.getMetricType());
            var points = queue.getPointsList().stream().map(Converter::fromGrpc).toList();
            graphStore.put(pid, mt, points);
        });
        return graphStore;
    }
}
