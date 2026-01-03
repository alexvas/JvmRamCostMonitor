package jvmram.suppliers.data;

/**
 * Данные потребления памяти из разных источников
 * на уровне ОС или JDK-фреймворка.
 */
public sealed interface HardwareData permits JmxData, MemInfoData, WinData, PwsData, SmapsData {
}
