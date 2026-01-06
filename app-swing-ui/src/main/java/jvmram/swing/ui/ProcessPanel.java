package jvmram.swing.ui;

import jvmram.proto.ProcInfo;
import jvmram.swing.client.JvmRamBackendClient;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jvmram.swing.ui.Utils.*;

public class ProcessPanel extends JPanel {

    private final JPanel contentWidget = new JPanel();
    private final JButton showButton;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JvmRamBackendClient client;

    /**
     * Это костыль, сынок.
     * Забудь о реактивности, ибо это Свинг.
     * Свинг релизнули, когда творцов реактивного UI ещё не было даже в планах.
     * Отсюда костыли.
     */
    private volatile boolean preventSelectionEventPropagation = false;

    private final JList<String> processList;
    private final JCheckBox childrenCheck;

    public ProcessPanel(JvmRamBackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());

        // Контейнер для содержимого панели
        contentWidget.setLayout(new BoxLayout(contentWidget, BoxLayout.Y_AXIS));
        contentWidget.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Кнопка скрытия панели
        var hideButton = new JButton("Скрыть панель");
        hideButton.addActionListener(_ -> hidePanel());
        contentWidget.add(hideButton);

        // Группа выбора процесса
        var processGroup = createGroup("JVM процессы");
        var processLayout = new JPanel();
        processLayout.setLayout(new BoxLayout(processLayout, BoxLayout.Y_AXIS));

        processList = new JList<>(listModel);
        processList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        processList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onProcessSelected();
            }
        });
        var scrollPane = new JScrollPane(processList);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        processLayout.add(scrollPane);

        var refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(_ -> client.refreshAvailableJvmProcesses());
        processLayout.add(refreshButton);

        processGroup.add(processLayout);
        contentWidget.add(processGroup);

        // Группа настроек
        var settingsGroup = createGroup("Настройки");
        var settingsLayout = new JPanel();
        settingsLayout.setLayout(new BoxLayout(settingsLayout, BoxLayout.Y_AXIS));

        childrenCheck = new JCheckBox("Включать потомки");
        childrenCheck.addActionListener(_ -> onChildrenToggled());
        settingsLayout.add(childrenCheck);

        settingsGroup.add(settingsLayout);
        contentWidget.add(settingsGroup);

        contentWidget.add(Box.createVerticalGlue());

        add(contentWidget, BorderLayout.CENTER);

        // Узкая кнопка для показа панели
        showButton = new JButton("▶");
        showButton.setPreferredSize(new Dimension(30, 30));
        showButton.setToolTipText("Показать панель");
        showButton.addActionListener(_ -> showPanel());
        showButton.setVisible(false);
        add(showButton, BorderLayout.WEST);

        client.addAvailableJvmProcessesListener(this::handleAvailableJvmProcessesUpdate);
    }

    private void showPanel() {
        showButton.setVisible(false);
        contentWidget.setVisible(true);
        revalidate();
        repaint();
    }

    private void hidePanel() {
        contentWidget.setVisible(false);
        showButton.setVisible(true);
        revalidate();
        repaint();
    }

    private void handleAvailableJvmProcessesUpdate(Collection<ProcInfo> jvmProcessInfos) {
        SwingUtilities.invokeLater(() -> doHandleAvailableJvmProcessesUpdate(jvmProcessInfos));
    }

    private void doHandleAvailableJvmProcessesUpdate(Collection<ProcInfo> jvmProcessInfos) {
        var toBeSelected = client.getExplicitlyFollowingPids();

        // Очистка списка
        preventSelectionEventPropagation = true;
        listModel.clear();
        preventSelectionEventPropagation = false;

        if (jvmProcessInfos.isEmpty()) {
            return;
        }

        // Заполнение списка
        int i = 0;
        int maxPidDigitCount = jvmProcessInfos.stream()
                .map(ProcessPanel::pidDigitCount)
                .max(Integer::compare)
                .orElseThrow();
        List<Integer> selectedIndices = new ArrayList<>();
        for (var procInfo : jvmProcessInfos.stream().sorted().toList()) {
            var formattedPid = ("%" + maxPidDigitCount + "d").formatted(procInfo.getPid());
            var p = formattedPid.replace(" ", " ");
            var entry = "%s %s".formatted(p, procInfo.getDisplayName());
            listModel.addElement(entry);
            if (toBeSelected.contains(procInfo.getPid())) {
                selectedIndices.add(i);
            }
            ++i;
        }
        processList.setSelectedIndices(toArray(selectedIndices));
    }

    private static int pidDigitCount(ProcInfo procInfo) {
        return Long.valueOf(procInfo.getPid()).toString().length();
    }

    private void onProcessSelected() {
        if (preventSelectionEventPropagation) {
            return;
        }

        var pids = new ArrayList<Long>();

        for (int i = 0; i < listModel.size(); ++i) {
            if (!processList.isSelectedIndex(i)) {
                continue;
            }
            var selectedItem = listModel.get(i);
            var pid = parsePidFromProcessDisplayName(selectedItem);
            pids.add(pid);
        }

        client.setCurrentlySelectedPids(pids);
    }

    private void onChildrenToggled() {
        boolean includeChildren = childrenCheck.isSelected();
        if (includeChildren) {
            client.includeChildrenProcesses();
        } else {
            client.excludeChildrenProcesses();
        }
    }
}
