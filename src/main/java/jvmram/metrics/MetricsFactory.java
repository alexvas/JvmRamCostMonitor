package jvmram.metrics;

import jvmram.model.metrics.MetricType;

import java.util.Map;

public interface MetricsFactory {
    Map<MetricType, RamMetric> getOrCreateMetrics(long pid, RamMetric.Os os);

    static MetricsFactory getInstance() {
        return MetricsFactoryImpl.INSTANCE;
    }
}
