package jvmram.ui;

import jvmram.conf.Config;
import jvmram.model.graph.MetricVisibility;
import jvmram.model.metrics.MetricType;

import javax.swing.*;
import java.util.Arrays;

public class GraphVisualizationControlsPanel extends JPanel {
    private final MetricVisibility metricVisibility = MetricVisibility.getInstance();

    public GraphVisualizationControlsPanel() {
        setBorder(BorderFactory.createTitledBorder("Отображаемые метрики"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel metricsLayout = new JPanel();
        metricsLayout.setLayout(new BoxLayout(metricsLayout, BoxLayout.Y_AXIS));

        Arrays.stream(MetricType.values())
                .filter(mt -> mt.isApplicable(Config.os))
                .forEach(mt -> addMetricCheck(metricsLayout, mt));

        add(metricsLayout);
    }

    private void addMetricCheck(JPanel layout, MetricType mt) {
        var title = "%s %s".formatted(mt.name(), mt.getDisplayName());
        var checkBox = new JCheckBox(title);
        checkBox.setIcon(new CustomIcon(mt));

        Boolean defaultVisible = Config.DEFAULT_METRIC_VISIBILITY.get(mt);
        boolean visible = defaultVisible != null && defaultVisible;
        checkBox.setSelected(visible);
        setVisible(mt, visible);
        layout.add(checkBox);
        checkBox.addChangeListener(_ -> setVisible(mt, checkBox.isSelected()));
    }

    private void setVisible(MetricType metricType, boolean selected) {
        if (selected) {
            metricVisibility.setVisible(metricType);
        } else {
            metricVisibility.setInvisible(metricType);
        }
    }

}
