package jvmram.metrics;

import jvmram.model.metrics.MetricType;
import jvmram.suppliers.HardwareDataSupplier;
import jvmram.suppliers.data.HardwareData;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

class BaseMetric<T extends HardwareData> implements RamMetric {
    private final MetricType metricType;
    private final HardwareDataSupplier<T> supplier;
    private volatile Duration pollInterval;
    private final Function<T, Long> converter;

    BaseMetric(MetricType metricType, HardwareDataSupplier<T> supplier, Duration pollInterval, Function<T, Long> converter) {
        this.metricType = metricType;
        this.supplier = supplier;
        this.pollInterval = pollInterval;
        this.converter = converter;
    }

    @Override
    public MetricType getMetricType() {
        return metricType;
    }

    @Override
    public long getPid() {
        return supplier.getPid();
    }

    private static final GraphPoint SAME_DATA = new GraphPoint(
            Instant.now(),
            RamMetric.SAME_DATA
    );

    private static final GraphPoint NO_DATA = new GraphPoint(
            Instant.now(),
            RamMetric.NO_DATA
    );

    @Override
    public GraphPoint getGraphPoint() {
        var nextPoll = nextPollInstant();
        if (nextPoll.isAfter(Instant.now())) {
            return SAME_DATA;
        }
        var data = supplier.getData();
        if (data == null) {
            return NO_DATA;
        }
        var moment = supplier.lastPollInstant();
        Long bytes = converter.apply(data);
        if (bytes == null) {
            throw new IllegalStateException("Null bytes conversion not allowed");
        }
        return new GraphPoint(moment, bytes);
    }

    @Override
    public void updatePollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    private Instant nextPollInstant() {
        var lastPoll = supplier.lastPollInstant();
        return lastPoll == null
                ? Instant.now()
                : lastPoll.plus(pollInterval);
    }
}
