package jvmram.ui;

import jvmram.Config;
import jvmram.controller.JmxService;
import jvmram.model.graph.MetricVisibility;
import jvmram.model.metrics.MetricType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Arrays;

import static jvmram.ui.Utils.parsePidFromProcessDisplayName;

public class ControlsPanel extends JPanel {
    private static final long NO_PID_SELECTED = -1;

    private final MetricVisibility metricVisibility = MetricVisibility.getInstance();
    private final JmxService jmxService = JmxService.getInstance();

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> followingProcessesList = new JList<>(listModel);
    private volatile long currentPid = NO_PID_SELECTED;

    public ControlsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Группа действий
        JPanel actionsGroup = createGroup("Действия");
        JPanel actionsLayout = new JPanel();
        actionsLayout.setLayout(new BoxLayout(actionsLayout, BoxLayout.Y_AXIS));

        followingProcessesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        followingProcessesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onFollowingProcessSelected();
            }
        });

        JButton gcButton = new JButton("GC");
        gcButton.addActionListener(_ -> onTriggerGc());
        actionsLayout.add(gcButton);

        JButton heapDumpButton = new JButton("Heap Dump");
        heapDumpButton.addActionListener(_ -> onHeapDumpClicked());
        actionsLayout.add(heapDumpButton);
        
        JButton saveButton = new JButton("Сохранить график");
        saveButton.addActionListener(_ -> onSaveClicked());
        actionsLayout.add(saveButton);
        
        actionsGroup.add(actionsLayout);
        add(actionsGroup);
        
        // Группа настроек метрик
        JPanel metricsGroup = createGroup("Отображаемые метрики");
        JPanel metricsLayout = new JPanel();
        metricsLayout.setLayout(new BoxLayout(metricsLayout, BoxLayout.Y_AXIS));

        Arrays.stream(MetricType.values())
                .filter(mt -> mt.isApplicable(Config.os))
                .forEach(mt -> addMetricCheck(metricsLayout, mt));

        metricsGroup.add(metricsLayout);
        add(metricsGroup);
    }

    private void onTriggerGc() {
        long pid = currentPid;
        if (pid == NO_PID_SELECTED) {
            return;
        }
        jmxService.gc(pid);
    }

    private void onFollowingProcessSelected() {
        int selectedIndex = followingProcessesList.getSelectedIndex();
        if (selectedIndex < 0) {
            currentPid = NO_PID_SELECTED;
            return;
        }

        var selectedItem = listModel.get(selectedIndex);
        currentPid = parsePidFromProcessDisplayName(selectedItem);
    }

    private JPanel createGroup(String title) {
        JPanel group = new JPanel();
        group.setBorder(BorderFactory.createTitledBorder(title));
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        return group;
    }
    
    private void addMetricCheck(JPanel layout, MetricType mt) {
        var checkBox = new JCheckBox("%s %s".formatted(mt.name(), mt.getDisplayName()));
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

    private void onHeapDumpClicked() {
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить Heap Dump");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Heap Dump (*.hprof)", "hprof"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var filepath = fileChooser.getSelectedFile().getAbsolutePath();
            jmxService.createHeapDump(filepath);
        }
    }
    
    private void onSaveClicked() {
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить график");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "PNG (*.png)", "png"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            // todo: delegate to GraphPanel and save screenshot with a filepath there.
        }
    }
}
