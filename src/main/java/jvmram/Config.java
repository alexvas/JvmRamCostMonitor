package jvmram;

import jvmram.metrics.RamMetric;
import jvmram.model.metrics.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Config {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Интервалы опроса (в секундах)
    public static final Map<MetricType, Duration> PRODUCTION_POLL_INTERVALS = new EnumMap<>(MetricType.class);

    // Настройки отображения метрик по умолчанию
    public static final Map<MetricType, Boolean> DEFAULT_METRIC_VISIBILITY = new EnumMap<>(MetricType.class);

    // Определение платформы
    public static final RamMetric.Os os;

    static {
        // Инициализация интервалов опроса
        Arrays.stream(MetricType.values()).forEach(type -> {
            int productionDurationInSeconds = switch (type) {
                case RSS, WS, HEAP_USED, HEAP_COMMITTED, NMT_USED, NMT_COMMITTED -> 5;
                case PSS, USS -> 30;
                case PWS, PB -> 10;
            };
            var productionDuration = Duration.ofSeconds(productionDurationInSeconds);

            PRODUCTION_POLL_INTERVALS.put(type, productionDuration);
        });


        // Инициализация видимости метрик по умолчанию
        DEFAULT_METRIC_VISIBILITY.put(MetricType.RSS, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.PSS, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.USS, false);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.WS, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.PWS, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.PB, false);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.HEAP_USED, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.HEAP_COMMITTED, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.NMT_USED, true);
        DEFAULT_METRIC_VISIBILITY.put(MetricType.NMT_COMMITTED, true);

        // Определение платформы
        var osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("linux")) {
            os = RamMetric.Os.LINUX;
        } else if (osName.contains("windows")) {
            os = RamMetric.Os.WINDOWS;
        } else {
            LOG.error("Unsupported OS {}", osName);
            System.exit(2);
            // never reach the next line
            os = null;
        }
    }

    private Config() {
        // Утилитный класс, не должен быть инстанциирован
    }
}

