package jvmram.ui;

import javax.swing.*;

public class ControlsPanel extends JPanel {

    public ControlsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Группа действий над JVM
        var jvmActionsPanel = new JvmActionsAndSaveImagePanel();
        add(jvmActionsPanel);

        // Группа управления отображением графиков
        var graphVisualizationControlsPanel = new GraphVisualizationControlsPanel();
        add(graphVisualizationControlsPanel);
    }
}
