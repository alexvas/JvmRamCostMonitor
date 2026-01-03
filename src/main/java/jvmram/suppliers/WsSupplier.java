package jvmram.suppliers;

import jvmram.Config;
import jvmram.suppliers.data.WsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static jvmram.metrics.RamMetric.Os.WINDOWS;

class WsSupplier extends AbstractDataSupplier<WsData> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    WsSupplier(long pid) {
        super(pid);
        if (Config.os != WINDOWS) {
            LOG.error("The supplier is intended for use in Windows OS only");
        } else {
            setInitialized();
        }
    }
    
    @Override
    WsData doGetData() {
        return null;
    }
}
