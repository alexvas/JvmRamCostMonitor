package jvmram.backend;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import jvmram.controller.GraphController;
import jvmram.controller.JmxService;
import jvmram.controller.ProcessController;
import jvmram.model.graph.GraphPoint;
import jvmram.model.graph.GraphKey;
import jvmram.model.graph.GraphPointQueues;
import jvmram.process.JvmProcessInfo;
import jvmram.proto.*;

import java.util.Collection;

class JvmRamBackendImpl extends AppBackendGrpc.AppBackendImplBase {
    private static final Empty EMPTY = Empty.newBuilder().build();

    private final ProcessController processController;
    private final GraphController graphController;
    private final GraphPointQueues queues;
    private final JmxService jmxService;

    public JvmRamBackendImpl(
            ProcessController processController,
            GraphController graphController,
            GraphPointQueues queues,
            JmxService jmxService
    ) {
        this.processController = processController;
        this.graphController = graphController;
        this.queues = queues;
        this.jmxService = jmxService;
    }

    @Override
    public void listenJvmProcessList(Empty request, StreamObserver<JvmProcessListResponse> responseObserver) {
        processController.addAvailableJvmProcessesListener(procInfos -> {
                    var resp = JvmProcessListResponse.newBuilder()
                            .addAllInfos(procInfos.stream().map(this::convert2Grpc).toList())
                            .build();
                    responseObserver.onNext(resp);
                }
        );
    }

    @Override
    public void listenGraphQueues(Empty request, StreamObserver<GraphQueues> responseObserver) {
        graphController.addRenderer(() -> {
            var keys = queues.keys();
            GraphQueues resp = GraphQueues.newBuilder().addAllQueues(
                    keys.stream()
                            .map(k -> convert2Grpc(k, queues.getPoints(k)))
                            .toList()
            ).build();
            responseObserver.onNext(resp);
        });
    }

    @Override
    public void setFollowingPids(JvmFollowPidRequest request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        processController.setCurrentlySelectedPids(
                request.getPidsList().stream().map(this::convertFromGrpc).toList()
        );
    }

    @Override
    public void triggerGc(Pid request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        jmxService.gc(request.getPid());
    }

    @Override
    public void dumpHeap(File request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        jmxService.createHeapDump(request.getFileName());
    }

    private ProcInfo convert2Grpc(JvmProcessInfo input) {
        return ProcInfo.newBuilder()
                .setPid(input.pid())
                .setDisplayName(input.displayName())
                .build();
    }

    private GraphQueue convert2Grpc(GraphKey k, Collection<GraphPoint> points) {
        return GraphQueue.newBuilder()
                .setType(convert2Grpc(k))
                .addAllPoints(
                        points.stream()
                                .map(this::convert2Grpc)
                                .toList()
                )
                .build();
    }

    private GraphQueue.MetricType convert2Grpc(GraphKey input) {
        return switch (input.type()) {
            case RSS -> GraphQueue.MetricType.RSS;
            case PSS -> GraphQueue.MetricType.PSS;
            case USS -> GraphQueue.MetricType.USS;
            case WS -> GraphQueue.MetricType.WS;
            case PB -> GraphQueue.MetricType.PB;
            case HEAP_USED -> GraphQueue.MetricType.HEAP_USED;
            case HEAP_COMMITTED -> GraphQueue.MetricType.HEAP_COMMITTED;
            case NMT_USED -> GraphQueue.MetricType.NMT_USED;
            case NMT_COMMITTED -> GraphQueue.MetricType.NMT_COMMITTED;
        };
    }

    private GraphQueue.GraphPoint convert2Grpc(GraphPoint input) {
        var moment = input.moment();
        return GraphQueue.GraphPoint.newBuilder()
                .setBytes(input.bytes())
                .setMoment(
                        Timestamp.newBuilder()
                                .setSeconds(moment.getEpochSecond())
                                .setNanos(moment.getNano())
                                .build()
                )
                .build();
    }

    private Long convertFromGrpc(Pid pid) {
        return pid.getPid();
    }
}
