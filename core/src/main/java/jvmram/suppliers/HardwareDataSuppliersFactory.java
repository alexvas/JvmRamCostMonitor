package jvmram.suppliers;

import jvmram.model.metrics.MetricType;
import jvmram.suppliers.data.HardwareData;

public interface HardwareDataSuppliersFactory {
    <T extends HardwareData> HardwareDataSupplier<T> getOrCreateSupplier(long pid, MetricType metricType);

    static HardwareDataSuppliersFactory getInstance() {
        return HardwareDataSuppliersFactoryImpl.INSTANCE;
    }
}
