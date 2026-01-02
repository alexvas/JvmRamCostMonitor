package jvmram.metrics;

import jvmram.model.metrics.MetricType;

import java.time.Duration;

public interface RamMetric {

    /**
     * Нет данных в связи со сбоем в работе или логике.
     */
    long NO_DATA = -1;

    /**
     * Данные прежние. Следует использовать данные с предыдущего опроса,
     * поскольку таймаут опроса ещё не истёк.
     */
    long SAME_DATA = -2;

    /**
     * Получить значение метрики в байтах
     *
     * @return либо неотрицательное значение в байтах, либо специальные константы (выше).
     */
    GraphPoint getGraphPoint();

    /**
     * Обновить таймаут опроса.
     *
     * @param pollInterval - новый таймаут опроса.
     */
    void updatePollInterval(Duration pollInterval);

    /**
     * Отдаёт тип метрики
     * @return тип метрики
     */
    MetricType getMetricType();

    /**
     * Отдаёт pid процесса, за которым следит метрика.
     *
     * @return pid процесса.
     */
    long getPid();

    enum Os {
        LINUX,
        WINDOWS
    }
}
