package jvmram.metrics;

import java.time.Instant;
import java.util.Comparator;

public record GraphPoint(Instant moment, long bytes) {

    public static final Comparator<GraphPoint> TIME_COMPARATOR = (left, right) -> left.moment.compareTo(right.moment);

    public static final Comparator<GraphPoint> BYTE_COMPARATOR = (left, right) -> Long.compare(left.bytes, right.bytes);
}
