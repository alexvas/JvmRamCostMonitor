package jvmram.controller;

import jvmram.controller.impl.ProcessControllerImpl;
import jvmram.process.JvmProcessInfo;

import java.util.Collection;
import java.util.function.Consumer;

public interface ProcessController {

    /**
     * Следим не только за памятью указанного процесса,
     * но и за памятью всех его процессов-потомков.
     */
    void includeChildrenProcesses();

    /**
     * Следим только за памятью указанного процесса.
     * За памятью процессов-потомков не следим.
     */
    void excludeChildrenProcesses();

    /**
     * Получить список процессов, за которыми явно поручено следить.
     *
     * @return список отслеживаемых процессов
     */
    Collection<Long> getExplicitlyFollowingPids();

    /**
     * Получить список процессов, за которыми поручено следить
     * вместе с их процессами-потомками.
     *
     * @return список отслеживаемых процессов
     */
    Collection<Long> getPidsWithDescendants();

    void refreshAvailableJvmProcesses();

    void addAvailableJvmProcessesListener(Consumer<Collection<JvmProcessInfo>> onProcessInfoChanged);

    void setCurrentlySelectedPids(Collection<Long> pids);

    static ProcessController getInstance() {
        return ProcessControllerImpl.INSTANCE;
    }
}
