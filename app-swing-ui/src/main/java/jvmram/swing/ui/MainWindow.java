package jvmram.swing.ui;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private final GraphPanel graphPanel = new GraphPanel();
    private final ControlsPanel controlsPanel = new ControlsPanel();
    
    public MainWindow() {
        setTitle("Jvm RAM Cost Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        createUI();
    }
    
    private void createUI() {
        setLayout(new BorderLayout());
        
        var mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Панель процессов (слева)
        var processPanel = new ProcessPanel();
        mainPanel.add(processPanel, BorderLayout.WEST);
        
        // Вертикальный контейнер для графика и панели управления
        var rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Панель графиков
        rightPanel.add(graphPanel, BorderLayout.CENTER);
        
        // Панель управления
        rightPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            System.exit(0);
        }
        super.processWindowEvent(e);
    }
}
