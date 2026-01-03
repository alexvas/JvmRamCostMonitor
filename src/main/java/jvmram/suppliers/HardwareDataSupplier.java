package jvmram.suppliers;

import jvmram.suppliers.data.HardwareData;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Измеряет и хранит последнее измерение данных о потреблении RAM в том или ином аспекте от ОС.
 * Отдаёт сохранённое значение по запросу.
 *
 * @param <T> какие именно данные.
 */
public interface HardwareDataSupplier<T extends HardwareData> {

    /**
     * Получить запомненное значение.
     *
     * @return возвращает ненулевые данные в случае штатной работы, либо null, если что-то пошло не так.
     */
    @Nullable T getStoredData();

    /**
     * Измерить и запомнить данные о потреблении RAM.
     */
    void measureAndStore();

    /**
     * Получить момент последнего запроса данных от ОС.
     *
     * @return момент последнего запроса данных от ОС или null, если такого запроса пока не было.
     */
    @Nullable Instant lastPollInstant();

    /**
     * Идентификатор процесса, к которому привязан Поставщик.
     *
     * @return идентификатор процесса.
     */
    long getPid();
}
