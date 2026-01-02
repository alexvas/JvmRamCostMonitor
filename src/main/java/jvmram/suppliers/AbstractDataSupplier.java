package jvmram.suppliers;

import jvmram.suppliers.data.HardwareData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;

abstract class AbstractDataSupplier<T extends HardwareData> implements HardwareDataSupplier<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    final long pid;

    private boolean initialized;
    private Instant lastPollInstant;

    AbstractDataSupplier(long pid) {
        this.pid = pid;
    }
    
    @Override
    public @Nullable T getData() {
        if (!initialized) {
            return null;
        }

        lastPollInstant = Instant.now();

        try {
            return doGetData();
        } catch (Exception e) {
            LOG.error("Error getting data for pid {}", pid);
            return null;
        }
    }

    @Override
    public @Nullable Instant lastPollInstant() {
        return lastPollInstant;
    }

    /**
     * Метод для использования (и переопределения) в потомках,
     * который помечает Поставщик как рабочий.
     */
    void setInitialized() {
        this.initialized = true;
    }

    abstract T doGetData();

    @Override
    public long getPid() {
        return pid;
    }
}
