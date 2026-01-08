package jvmram.controller.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static <L> void callActionOrGetRidOfListener(Collection<L> listeners, Consumer<L> action) {
        Collection<L> failedListeners = new ArrayList<>();
        for (L listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOG.warn("Failed to deliver an action to listener", e);
                failedListeners.add(listener);
            }
        }
        listeners.removeAll(failedListeners);
    }

    private Utils() {
    }
}
