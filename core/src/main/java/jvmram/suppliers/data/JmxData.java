package jvmram.suppliers.data;

public record JmxData(long heapUsed, long heapCommitted, long nmtUsed, long nmtCommitted) implements HardwareData {
}

