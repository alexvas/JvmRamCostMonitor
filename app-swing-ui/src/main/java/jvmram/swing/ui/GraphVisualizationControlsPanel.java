package jvmram.swing.ui;

import jvmram.model.metrics.MetricType;
import jvmram.swing.client.JvmRamClient;

import javax.swing.*;
import java.util.List;

public class GraphVisualizationControlsPanel extends JPanel {

    private final JvmRamClient client;

    public GraphVisualizationControlsPanel(JvmRamClient client) {
        this.client = client;
        setBorder(BorderFactory.createTitledBorder("Отображаемые метрики"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel metricsLayout = new JPanel();
        metricsLayout.setLayout(new BoxLayout(metricsLayout, BoxLayout.Y_AXIS));

        List<MetricType> applicableMetrics = client.getApplicableMetrics();

        applicableMetrics
                .forEach(mt -> addMetricCheck(metricsLayout, mt));

        add(metricsLayout);
    }

    private void addMetricCheck(JPanel layout, MetricType mt) {
        var title = "%s %s".formatted(mt.name(), mt.getDisplayName());
        var checkBox = new JCheckBox(title);
        checkBox.setIcon(new CustomIcon(mt));

        // todo: config default visibility
        Boolean defaultVisible = Boolean.TRUE;
        boolean visible = defaultVisible != null && defaultVisible;
        checkBox.setSelected(visible);
        setVisible(mt, visible);
        layout.add(checkBox);
        checkBox.addChangeListener(_ -> setVisible(mt, checkBox.isSelected()));
    }

    private void setVisible(MetricType metricType, boolean selected) {
        if (selected) {
            client.setVisible(metricType);
        } else {
            client.setInvisible(metricType);
        }
    }

}
