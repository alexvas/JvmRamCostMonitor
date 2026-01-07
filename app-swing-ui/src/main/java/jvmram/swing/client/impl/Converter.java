package jvmram.swing.client.impl;

import com.google.protobuf.Timestamp;
import jvmram.model.graph.GraphPoint;
import jvmram.model.metrics.MetricType;
import jvmram.proto.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static jvmram.proto.MetricType.*;
import static jvmram.proto.MetricType.HEAP_COMMITTED;
import static jvmram.proto.MetricType.HEAP_USED;
import static jvmram.proto.MetricType.NMT_COMMITTED;
import static jvmram.proto.MetricType.NMT_USED;
import static jvmram.proto.MetricType.PB;
import static jvmram.proto.MetricType.WS;

public class Converter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static PidList convert2Grpc(Collection<Long> input) {
        var output = input.stream().map(it -> Pid.newBuilder().setPid(it).build()).toList();
        return PidList.newBuilder().addAllPids(output).build();
    }

    static jvmram.proto.MetricType convert2Grpc(MetricType input) {
        return switch (input) {
            case RSS -> RSS;
            case PSS -> PSS;
            case USS -> USS;
            case WS -> WS;
            case PB -> PB;
            case HEAP_USED -> HEAP_USED;
            case HEAP_COMMITTED -> HEAP_COMMITTED;
            case NMT_USED -> NMT_USED;
            case NMT_COMMITTED -> NMT_COMMITTED;
        };
    }

    static List<Long> convert2Grpc(@Nullable PidList input) {
        return input == null
                ? List.of()
                : input.getPidsList()
                .stream()
                .map(Pid::getPid)
                .toList();
    }

    static @Nullable MetricType fromGrpc(jvmram.proto.MetricType input) {
        return switch (input) {
            case RSS -> MetricType.RSS;
            case PSS -> MetricType.PSS;
            case USS -> MetricType.USS;
            case WS -> MetricType.WS;
            case PB -> MetricType.PB;
            case HEAP_USED -> MetricType.HEAP_USED;
            case HEAP_COMMITTED -> MetricType.HEAP_COMMITTED;
            case NMT_USED -> MetricType.NMT_USED;
            case NMT_COMMITTED -> MetricType.NMT_COMMITTED;
            case UNRECOGNIZED -> {
                LOG.warn("Unrecognized input metrics");
                yield null;
            }
        };
    }

    public static GraphPoint fromGrpc(GraphQueue.GraphPoint input) {
        return new GraphPoint(
                fromGrpc(input.getMoment()),
                input.getBytes()
        );
    }

    private static Instant fromGrpc(Timestamp moment) {
        return Instant.ofEpochSecond(moment.getSeconds(), moment.getNanos());
    }

    private Converter() {
    }
}
