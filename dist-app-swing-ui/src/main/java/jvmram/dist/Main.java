package jvmram.dist;

import jvmram.backend.JvmRunCostMonitor;
import jvmram.swing.SwingLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_PORT = 53333;

    void main() {
        Thread.setDefaultUncaughtExceptionHandler((_, e) -> LOG.error("Unexpected exception: ", e));

        var backend = new JvmRunCostMonitor();
        backend.setup(DEFAULT_PORT);

        @SuppressWarnings("resource")
        var swing = new SwingLifecycle();
        swing.setup(DEFAULT_PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(swing::close));
    }
}
