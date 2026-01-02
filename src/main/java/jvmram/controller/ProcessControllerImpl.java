package jvmram.controller;

import jvmram.model.graph.GraphPointQueues;
import jvmram.model.graph.MetricVisibility;
import jvmram.process.JvmProcessInfo;
import jvmram.process.ProcessManager;
import jvmram.util.RwGuarded;

import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;

class ProcessControllerImpl implements ProcessController {

    private final RwGuarded guarded = RwGuarded.create();

    private boolean includeChildrenProcesses = false;

    private final Collection<Long> explicitlyFollowingPids = new TreeSet<>();
    private final Map<Long, Collection<Long>> descendantPids = new HashMap<>();

    private final ProcessManager processManager = ProcessManager.getInstance();
    private final MetricVisibility metricVisibility = MetricVisibility.getInstance();
    private final GraphPointQueues graphPointQueues = GraphPointQueues.getInstance();

    private final List<Consumer<Collection<JvmProcessInfo>>> onProcessInfoChangedListeners = new ArrayList<>();

    @Override
    public void includeChildrenProcesses() {
        guarded.write(() ->
                includeChildrenProcesses = true
        );
    }

    @Override
    public void excludeChildrenProcesses() {
        guarded.write(() ->
                includeChildrenProcesses = false
        );
    }

    @Override
    public Collection<Long> getExplicitlyFollowingPids() {
        return guarded.read(() -> explicitlyFollowingPids);
    }

    @Override
    public Collection<Long> getPidsWithDescendants() {
        return guarded.read(() -> {
                    List<Long> output = new ArrayList<>();
                    for (long pid : explicitlyFollowingPids) {
                        output.add(pid);
                        output.addAll(descendantPids.get(pid));
                    }
                    return output;
                }
        );
    }

    @Override
    public void setCurrentlySelectedPids(Collection<Long> pids) {
        guarded.write(() -> {
            var pidsGone = new HashSet<>(explicitlyFollowingPids);
            pidsGone.removeAll(pids);
            pidsGone.forEach(this::doUnfollowPid);
            pids.forEach(this::doFollowPid);
        });
    }

    private void doUnfollowPid(long pid) {
        explicitlyFollowingPids.remove(pid);
        descendantPids.remove(pid);
    }

    private void doFollowPid(long pid) {
        explicitlyFollowingPids.add(pid);
        if (!includeChildrenProcesses) {
            return;
        }
        descendantPids.put(pid, processManager.getProcessDescendantIds(pid));
    }

    @Override
    public void refreshAvailableJvmProcesses() {
        var jvmProcesses = processManager.getJvmProcesses();
        var actualPids = jvmProcesses.stream().map(JvmProcessInfo::pid).collect(toSet());
        guarded.write(() -> {
                    var pidsGone = new HashSet<>(explicitlyFollowingPids);
                    pidsGone.removeAll(actualPids);
                    pidsGone.forEach(this::doUnfollowPid);
                }
        );
        guarded.read(() -> {
            onProcessInfoChangedListeners.forEach(listener -> listener.accept(jvmProcesses));
        });
    }

    @Override
    public void addAvailableJvmProcessesListener(Consumer<Collection<JvmProcessInfo>> onProcessInfoChanged) {
        guarded.write(() -> onProcessInfoChangedListeners.add(onProcessInfoChanged));
    }

    private ProcessControllerImpl() {
    }

    static ProcessControllerImpl INSTANCE = new ProcessControllerImpl();
}
