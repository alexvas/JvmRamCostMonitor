package jvmram.process;

import com.sun.tools.attach.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

class ProcessManagerImpl implements ProcessManager {

    private ProcessManagerImpl() {
    }

    @Override
    public List<JvmProcessInfo> getJvmProcesses() {
        return VirtualMachine.list().stream().map(vmd ->
                new JvmProcessInfo(
                        Long.parseLong(vmd.id()),
                        vmd.displayName().isEmpty()
                                ? vmd.id()
                                : vmd.displayName()
                )).toList();
    }

    @Override
    public List<Long> getProcessDescendantIds(long pid) {
        List<Long> output = new ArrayList<>();
        var processHandle = ProcessHandle.of(pid);
        processHandle.ifPresent(ph ->
                ph.descendants()
                        .map(ProcessHandle::pid)
                        .forEach(output::add)
        );
        return output;
    }

    static final ProcessManager INSTANCE = new ProcessManagerImpl();
}
