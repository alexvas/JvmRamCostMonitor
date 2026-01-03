package jvmram.metrics;

import java.time.Instant;
import java.util.Comparator;

public record GraphPoint(Instant moment, long bytes) {

    public boolean isRedundant() {
        return bytes == RamMetric.NO_DATA || bytes == RamMetric.SAME_DATA;
    }
}
