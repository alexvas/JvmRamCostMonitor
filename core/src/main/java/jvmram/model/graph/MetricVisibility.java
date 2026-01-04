package jvmram.model.graph;

import jvmram.model.graph.impl.MetricVisibilityImpl;
import jvmram.model.metrics.MetricType;


public interface MetricVisibility {

    boolean isVisible(MetricType type);

    void setInvisible(MetricType type);

    void setVisible(MetricType type);

    static MetricVisibility getInstance() {
        return MetricVisibilityImpl.INSTANCE;
    }
}
