package jvmram.suppliers;

import jvmram.model.metrics.MetricType;
import jvmram.suppliers.data.HardwareData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class HardwareDataSuppliersFactoryImpl implements HardwareDataSuppliersFactory {

    private HardwareDataSuppliersFactoryImpl() {
    }

    private final Map<Long, Map<Class<? extends AbstractDataSupplier<?>>, AbstractDataSupplier<?>>> suppliers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends HardwareData> HardwareDataSupplier<T> getOrCreateSupplier(long pid, MetricType metricType) {
        return (AbstractDataSupplier<T>) suppliers.computeIfAbsent(
                pid,
                ignored -> new HashMap<>()
        ).computeIfAbsent(
                supplierClass(metricType),
                ignored2 -> doCreateSupplier(pid, metricType)
        );
    }

    private Class<? extends AbstractDataSupplier<?>> supplierClass(MetricType type) {
        return switch (type) {
            case RSS -> MemInfoSupplier.class;
            case PSS, USS -> SmapsSupplier.class;
            case PB -> PbSupplier.class;
            case WS -> WsSupplier.class;
            case PWS -> PwsSupplier.class;
            case HEAP_COMMITTED, HEAP_USED, NMT_USED, NMT_COMMITTED -> JmxSupplier.class;
        };
    }

    private AbstractDataSupplier<?> doCreateSupplier(long pid, MetricType type) {
        return switch (type) {
            case RSS -> new MemInfoSupplier(pid);
            case PSS, USS -> new SmapsSupplier(pid);
            case PB -> new PbSupplier(pid);
            case WS -> new WsSupplier(pid);
            case PWS -> new PwsSupplier(pid);
            case HEAP_COMMITTED, HEAP_USED, NMT_USED, NMT_COMMITTED -> new JmxSupplier(pid);
        };
    }

    static final HardwareDataSuppliersFactoryImpl INSTANCE = new HardwareDataSuppliersFactoryImpl();
}
