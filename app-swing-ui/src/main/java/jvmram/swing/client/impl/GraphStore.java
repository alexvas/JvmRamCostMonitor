package jvmram.swing.client.impl;

import jvmram.model.graph.GraphKey;
import jvmram.model.graph.GraphPoint;
import jvmram.model.graph.Utils;
import jvmram.model.metrics.MetricType;
import jvmram.model.util.RwGuarded;
import jvmram.swing.client.GraphPointQueuesMiniMax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GraphStore implements GraphPointQueuesMiniMax {
    private static final int SIZE_LIMIT = 1_000;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RwGuarded guarded = RwGuarded.create();
    private final Map<Long, Map<MetricType, TreeMap<Instant, Long>>> data = new HashMap<>();

    private volatile Instant minMoment = Instant.MAX;
    private volatile Instant maxMoment = Instant.MIN;
    private volatile long maxBytes = -1;

    @Override
    public Collection<GraphKey> keys() {
        return data.entrySet().stream().flatMap(entry -> {
                    long pid = entry.getKey();
                    return entry.getValue().keySet().stream().map(mt -> new GraphKey(mt, pid));
                }
        ).toList();
    }

    @Override
    public Collection<GraphPoint> getPoints(GraphKey key) {
        return guarded.read(() ->
                data.get(key.pid())
                        .get(key.type())
                        .entrySet()
                        .stream()
                        .map(it -> new GraphPoint(it.getKey(), it.getValue()))
                        .toList()
        );
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty() || data.values()
                .stream()
                .allMatch(it ->
                        it.isEmpty() || it.values()
                                .stream()
                                .allMatch(Map::isEmpty)
                );
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


    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private List<GraphPoint> add(long pid, MetricType metricType, GraphPoint input) {

        var bytes = input.bytes();
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes in GraphPoint must be positive: %s".formatted(input));
        }

        var metrics2etc = data.computeIfAbsent(pid, _ -> {
            LOG.debug("creating entry for a pid {}", pid);
            return new ConcurrentHashMap<>();
        });
        var points = metrics2etc.computeIfAbsent(metricType, _ -> {
            LOG.debug("creating entry for a pid {}, type {}", pid, metricType);
            return new TreeMap<>();
        });
        var prev = points.putIfAbsent(input.moment(), input.bytes());
        if (prev != null) {
            // Измерение в этот момент уже известно и содержится в хранилище.
            return List.of();
        }

        minMoment = Utils.min(minMoment, input.moment());
        maxMoment = Utils.max(maxMoment, input.moment());
        maxBytes = Math.max(maxBytes, input.bytes());

        var exceed = new ArrayList<GraphPoint>();
        while (points.size() >= SIZE_LIMIT) {
            var first = points.pollFirstEntry();
            exceed.add(new GraphPoint(first.getKey(), first.getValue()));
        }

        return exceed.isEmpty()
                ? List.of()
                : exceed;
    }

    private void handleExceed(Collection<GraphPoint> exceeds) {
        if (exceeds.isEmpty()) {
            return;
        }
        var maxExceedInstant = exceeds.stream()
                .map(GraphPoint::moment)
                .max(Instant::compareTo)
                .orElse(Instant.MIN);
        data.values().stream().flatMap(it -> it.values().stream())
                .forEach(map -> trim(map, maxExceedInstant));
        minMoment = findMinTime();

        long maxExceedBytes = exceeds.stream()
                .mapToLong(GraphPoint::bytes)
                .max()
                .orElse(-1);

        if (maxExceedBytes == maxBytes) {
            maxBytes = findMaxBytes();
        }
    }

    private Instant findMinTime() {
        return data.values()
                .stream()
                .flatMap(it -> it.values().stream())
                .map(TreeMap::firstKey)
                .min(Instant::compareTo)
                .orElseThrow();
    }

    private long findMaxBytes() {
        return byteStream().max(Long::compare).orElseThrow();
    }

    private Stream<Long> byteStream() {
        return data.values().stream().flatMap(it -> it.values().stream())
                .flatMap(it -> it.values().stream());
    }

    private static void trim(TreeMap<Instant, Long> map, Instant maxExceed) {
        var leftInstant = map.firstKey();
        if (leftInstant != null && leftInstant.isBefore(maxExceed)) {
            map.pollFirstEntry();
        }
    }

    public void put(long pid, MetricType mt, List<GraphPoint> points) {
        guarded.write(() -> {
                    var exceeds = new ArrayList<GraphPoint>();
                    for (var point : points) {
                        var ex = add(pid, mt, point);
                        exceeds.addAll(ex);
                    }
                    handleExceed(exceeds);
                }
        );
    }
}
