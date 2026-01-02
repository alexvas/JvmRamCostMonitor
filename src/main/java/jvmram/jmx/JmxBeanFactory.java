package jvmram.jmx;

import org.jspecify.annotations.Nullable;

import java.lang.management.MemoryMXBean;

public interface JmxBeanFactory {

    /**
     * Возвращаем JMX handle для получения данных о памяти JVM-процесса.
     *
     * @param pid процесса
     * @return данные о памяти или null в случае ошибки получения данных
     */
    @Nullable
    MemoryMXBean getMemoryMxBean(long pid);

    /**
     * Разрываем JMX-соединение с процессом
     *
     * @param pid процесса
     */
    void disconnect(long pid);

    static JmxBeanFactory getInstance() {
        return JmxBeanFactoryImpl.INSTANCE;
    }
}
