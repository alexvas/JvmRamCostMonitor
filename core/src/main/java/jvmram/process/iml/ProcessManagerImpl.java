package jvmram.process.iml;

import com.sun.tools.attach.VirtualMachine;
import jvmram.process.JvmProcessInfo;
import jvmram.process.ProcessManager;

import java.util.ArrayList;
import java.util.List;

public class ProcessManagerImpl implements ProcessManager {

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

    public static final ProcessManager INSTANCE = new ProcessManagerImpl();
}
