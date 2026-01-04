package jvmram.metrics.impl;

import jvmram.metrics.GraphPoint;
import jvmram.metrics.RamMetric;
import jvmram.suppliers.HardwareDataSupplier;
import jvmram.suppliers.data.HardwareData;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

class BaseMetric<T extends HardwareData> implements RamMetric {
    private final HardwareDataSupplier<T> supplier;
    private final Function<T, Long> converter;
    private volatile Duration pollInterval;
    private volatile Instant metricsLastPoll;

    BaseMetric(HardwareDataSupplier<T> supplier, Duration pollInterval, Function<T, Long> converter) {
        this.supplier = supplier;
        this.pollInterval = pollInterval;
        this.converter = converter;
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
        var supplierLastPoll = supplier.lastPollInstant();

        if (supplierLastPoll != null) {
            var nextPoll = supplierLastPoll.plus(pollInterval);
            if (nextPoll.isAfter(Instant.now())) {
                return Objects.equals(metricsLastPoll, supplierLastPoll)
                        ? SAME_DATA
                        : convertStoredSupplierData();
            }
        }

        supplier.measureAndStore();
        return convertStoredSupplierData();
    }

    private GraphPoint convertStoredSupplierData() {
        var data = supplier.getStoredData();
        if (data == null) {
            return NO_DATA;
        }
        metricsLastPoll = supplier.lastPollInstant();
        Long bytes = converter.apply(data);
        if (bytes == null) {
            throw new IllegalStateException("Null bytes conversion not allowed");
        }
        return new GraphPoint(metricsLastPoll, bytes);
    }

    @Override
    public void updatePollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

}
