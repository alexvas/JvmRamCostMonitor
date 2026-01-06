package jvmram.swing.ui;

import jvmram.swing.client.JvmRamClient;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import static jvmram.swing.ui.Utils.parsePidFromProcessDisplayName;

/**
 * Послать сигнал управления JVM:
 * сделать GC или Heap Dump,
 * а также сохранить графики в файле.
 */
public class JvmActionsAndSaveImagePanel extends JPanel {
    private static final long NO_PID_SELECTED = -1;

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> followingProcessesList = new JList<>(listModel);
    private final JvmRamClient jvmRamClient;

    private volatile long currentPid = NO_PID_SELECTED;

    public JvmActionsAndSaveImagePanel(JvmRamClient jvmRamClient) {
        this.jvmRamClient = jvmRamClient;
        setBorder(BorderFactory.createTitledBorder("Действия"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Группа действий
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
        
        add(actionsLayout);
    }

    private void onTriggerGc() {
        long pid = currentPid;
        if (pid == NO_PID_SELECTED) {
            return;
        }
        jvmRamClient.gc(pid);
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


    private void onHeapDumpClicked() {
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить Heap Dump");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Heap Dump (*.hprof)", "hprof"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var filepath = fileChooser.getSelectedFile().getAbsolutePath();
            jvmRamClient.createHeapDump(filepath);
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
