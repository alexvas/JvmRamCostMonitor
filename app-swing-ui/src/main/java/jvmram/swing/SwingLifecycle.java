package jvmram.swing;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import jvmram.swing.client.JvmRamClient;
import jvmram.swing.client.impl.JvmRamClientImpl;
import jvmram.swing.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SwingLifecycle implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private volatile ManagedChannel channel;

    public void setup(int port) {
        var client = createGrpcClient(port);
        createSwingUi(client);
    }

    private JvmRamClient createGrpcClient(int port) {
        String target = "localhost:%d".formatted(port);

        // Создаём канал коммуникации с сервером. Каналы потокобезопасны и переиспользуемы.
        // Обычно каналы создают в начале работы приложения и переиспользуют их, пока
        // работа не завершится.
        //
        // Например, здесь используются небезопасные верительные данные открытого текста, чтобы
        // избежать нужды в TLS сертификате. Чтобы задействовать TLS, используйте вместо них
        // TlsChannelCredentials
        channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .build();

        return new JvmRamClientImpl(channel);
    }

    private static void createSwingUi(JvmRamClient client) {
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

    private static final Duration CHANNEL_TERMINATION_TIMEOUT = Duration.ofSeconds(5);

    public void close() {
        // Канал ManagedChannel использует ресурсы, такие как потоки или TCP-соединения.
        // Чтобы предотвратить утечку этих ресурсов, канал нужно выключить, когда он более не нужен.
        // Если же его можно переиспользовать, оставьте его включённым.
        try {
            channel.shutdownNow().awaitTermination(
                    CHANNEL_TERMINATION_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException e) {
            // Игнорируем исключение, поскольку в нашем случае
            // канал закрывается при останове приложения.
        }
    }
}
