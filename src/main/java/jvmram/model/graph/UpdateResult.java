package jvmram.model.graph;

import jvmram.metrics.GraphPoint;

import java.util.List;

public sealed interface UpdateResult {
    enum Signal implements UpdateResult {
        REDUNDANT_UPDATE,
        UPDATE_WITHIN_BONDS
    }

    record Exceed(List<GraphPoint> points) implements UpdateResult {}
}
