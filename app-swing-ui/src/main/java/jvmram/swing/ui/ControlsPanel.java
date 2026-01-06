package jvmram.swing.ui;

import jvmram.swing.client.JvmRamBackendClient;

import javax.swing.*;

public class ControlsPanel extends JPanel {

    public ControlsPanel(JvmRamBackendClient client) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Группа действий над JVM
        var jvmActionsPanel = new JvmActionsAndSaveImagePanel(client);
        add(jvmActionsPanel);

        // Группа управления отображением графиков
        var graphVisualizationControlsPanel = new GraphVisualizationControlsPanel(client);
        add(graphVisualizationControlsPanel);
    }
}
