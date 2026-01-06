package jvmram.swing;

import jvmram.swing.client.impl.JvmRamBackendClientImpl;
import jvmram.swing.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.invoke.MethodHandles;

public class JvmRamCostMonitorSwingMain {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main() {
        Thread.setDefaultUncaughtExceptionHandler((_, e) -> LOG.error("Unexpected exception: ", e));

        var client = new JvmRamBackendClientImpl();

        SwingUtilities.invokeLater(() -> {
            try {
                // Установка системного Look and Feel
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    LOG.info("Failed to install system look 'n feel, using default one: {}", e.getMessage());
                }
                
                var window = new MainWindow(client);
                window.setVisible(true);
            } catch (Exception e) {
                LOG.error("Error starting the application", e);
                System.exit(1);
            }
        });
    }
}
