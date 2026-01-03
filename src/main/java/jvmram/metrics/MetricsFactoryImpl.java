package jvmram.metrics;

import jvmram.model.metrics.MetricType;
import jvmram.suppliers.HardwareDataSupplier;
import jvmram.suppliers.HardwareDataSuppliersFactory;
import jvmram.suppliers.data.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static jvmram.Config.DEV_POLL_INTERVALS;
import static jvmram.model.metrics.MetricType.*;

class MetricsFactoryImpl implements MetricsFactory {

    private final Map<Long, Map<MetricType, RamMetric>> metrics = new ConcurrentHashMap<>();

    private final HardwareDataSuppliersFactory suppliersFactory = HardwareDataSuppliersFactory.getInstance();

    private MetricsFactoryImpl() {
    }

    @Override
    public Map<MetricType, RamMetric> getOrCreateMetrics(long pid, RamMetric.Os os) {
        return metrics.computeIfAbsent(pid, ignored -> createMetricMap(pid, os));
    }

    private Map<MetricType, RamMetric> createMetricMap(long pid, RamMetric.Os os) {
        var osSpecific = switch (os) {
            case LINUX -> Map.of(
                    RSS, createMetrics(pid, RSS, MemInfoData::rss),
                    PSS, createMetrics(pid, PSS, SmapsData::pss),
                    USS, createMetrics(pid, USS, SmapsData::uss)
            );
            case WINDOWS -> Map.of(
                    PB, createMetrics(pid, PB, PbData::pb),
                    PWS, createMetrics(pid, PWS, PwsData::pws),
                    WS, createMetrics(pid, WS, WsData::ws)
            );
        };
        var common = Map.of(
                HEAP_USED, createMetrics(pid, HEAP_USED, JmxData::heapUsed),
                HEAP_COMMITTED, createMetrics(pid, HEAP_COMMITTED, JmxData::heapCommitted),
                NMT_USED, createMetrics(pid, NMT_USED, JmxData::nmtUsed),
                NMT_COMMITTED, createMetrics(pid, NMT_COMMITTED, JmxData::nmtCommitted)
        );
        var output = new HashMap<>(osSpecific);
        output.putAll(common);
        return output;
    }

    private <T extends HardwareData> RamMetric createMetrics(long pid, MetricType type, Function<T, Long> converter) {
        HardwareDataSupplier<T> supplier = suppliersFactory.getOrCreateSupplier(pid, type);
        return new BaseMetric<>(type, supplier, DEV_POLL_INTERVALS.get(type), converter);
    }

    static MetricsFactoryImpl INSTANCE = new MetricsFactoryImpl();
}
