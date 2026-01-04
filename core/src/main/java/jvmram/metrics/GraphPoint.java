package jvmram.metrics;

import java.time.Instant;

public record GraphPoint(Instant moment, long bytes) {

    public boolean isRedundant() {
        return bytes == RamMetric.NO_DATA || bytes == RamMetric.SAME_DATA;
    }
}
