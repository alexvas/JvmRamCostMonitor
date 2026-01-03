package jvmram.suppliers;

import jvmram.Config;
import jvmram.suppliers.data.PbData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static jvmram.metrics.RamMetric.Os.WINDOWS;

class PbSupplier extends AbstractDataSupplier<PbData> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    PbSupplier(long pid) {
        super(pid);
        if (Config.os != WINDOWS) {
            LOG.error("The supplier is intended for use in Windows OS only");
        } else {
            setInitialized();
        }
    }
    
    @Override
    @Nullable PbData doGetData() {
        return null;
    }
}
