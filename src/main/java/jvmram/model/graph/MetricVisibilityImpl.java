package jvmram.model.graph;

import jvmram.model.metrics.MetricType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

class MetricVisibilityImpl implements MetricVisibility {

    private final Set<MetricType> invisibles = Collections.synchronizedSet(EnumSet.noneOf(MetricType.class));

    private MetricVisibilityImpl() {
    }

    @Override
    public boolean isVisible(MetricType type) {
        return !invisibles.contains(type);
    }

    @Override
    public void setInvisible(MetricType type) {
        invisibles.add(type);
    }

    @Override
    public void setVisible(MetricType type) {
        invisibles.remove(type);
    }

    static MetricVisibilityImpl INSTANCE = new MetricVisibilityImpl();
}
