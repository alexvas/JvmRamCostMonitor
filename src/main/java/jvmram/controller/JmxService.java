package jvmram.controller;

public interface JmxService {

    /**
     * Провести Garbage Collection для JDK-процесса с определённым PID
     *
     * @param pid кому провести GC
     */
    void gc(long pid);

    void createHeapDump(String filepath);

    static JmxService getInstance() {
        return JmxServiceImpl.INSTANCE;
    }
}
