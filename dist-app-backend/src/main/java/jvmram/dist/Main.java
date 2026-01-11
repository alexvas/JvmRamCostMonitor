package jvmram.dist;

import jvmram.backend.JvmRunCost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_PORT = 53535;

    static void main() {
        Thread.setDefaultUncaughtExceptionHandler((_, e) -> LOG.error("Unexpected exception: ", e));

        var main = new JvmRunCost();
        main.setup(DEFAULT_PORT);
        main.blockUntilShutdown();
    }
}
