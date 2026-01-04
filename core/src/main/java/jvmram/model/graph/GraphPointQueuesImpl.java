package jvmram.model.graph;

import jvmram.metrics.GraphPoint;
import jvmram.model.metrics.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

class GraphPointQueuesImpl implements GraphPointQueues {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int SIZE_LIMIT = 1_000;

    private final Map<GraphKey, Deque<GraphPoint>> data = new ConcurrentHashMap<>();

    private GraphPointQueuesImpl() {
    }

    private volatile Instant minMoment = Instant.MAX;
    private volatile Instant maxMoment = Instant.MIN;
    private volatile long maxBytes = -1;

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    @Override
    public List<GraphPoint> add(long pid, MetricType metricType, GraphPoint graphPoint) {

        var bytes = graphPoint.bytes();
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes in GraphPoint must be positive: %s".formatted(graphPoint));
        }

        minMoment = Utils.min(minMoment, graphPoint.moment());
        maxMoment = Utils.max(maxMoment, graphPoint.moment());
        maxBytes = Math.max(maxBytes, graphPoint.bytes());

        var key = new GraphKey(metricType, pid);
        var deque = data.computeIfAbsent(key, _ -> {
            LOG.debug("creating entry for a {}", key);
            return new LinkedBlockingDeque<>();
        });
        var exceed = new ArrayList<GraphPoint>();
        while (deque.size() >= SIZE_LIMIT) {
            exceed.add(deque.pollFirst());
        }
        deque.offer(graphPoint);
        return exceed.isEmpty()
                ? List.of()
                : exceed;
    }

    @Override
    public void handleExceed(Collection<GraphPoint> exceeds) {
        if (exceeds.isEmpty()) {
            return;
        }
        var maxExceedInstant = exceeds.stream()
                .map(GraphPoint::moment)
                .max(Instant::compareTo)
                .orElse(Instant.MIN);
        data.values().forEach(deque -> trim(deque, maxExceedInstant));
        minMoment = findMinTime();

        long maxExceedBytes = exceeds.stream()
                .mapToLong(GraphPoint::bytes)
                .max()
                .orElse(-1);

        if (maxExceedBytes == maxBytes) {
            maxBytes = findMaxBytes();
        }
    }

    private static void trim(Deque<GraphPoint> deque, Instant maxExceed) {
        var point = deque.peekFirst();
        if (point != null && point.moment().isBefore(maxExceed)) {
            deque.pollFirst();
        }
    }

    @Override
    public Collection<GraphKey> keys() {
        return data.keySet();
    }

    @Override
    public Collection<GraphPoint> getPoints(GraphKey key) {
        return data.get(key);
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty() || data.values().stream().allMatch(Collection::isEmpty);
    }

    private Instant findMinTime() {
        return data.values()
                .stream()
                .map(Deque::peekFirst)
                .filter(Objects::nonNull)
                .map(GraphPoint::moment)
                .min(Instant::compareTo)
                .orElseThrow();
    }

    private long findMaxBytes() {
        return byteStream().max(Long::compare).orElseThrow();
    }

    private Stream<Long> byteStream() {
        return data.values().stream().flatMap(Collection::stream).map(GraphPoint::bytes);
    }

    @Override
    public Instant minMoment() {
        return minMoment;
    }

    @Override
    public Instant maxMoment() {
        return maxMoment;
    }

    @Override
    public long maxBytes() {
        return maxBytes;
    }

    static final GraphPointQueuesImpl INSTANCE = new GraphPointQueuesImpl();
}
