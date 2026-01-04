package jvmram.process;

import org.jspecify.annotations.NonNull;

public record JvmProcessInfo(long pid, String displayName) implements Comparable<JvmProcessInfo> {
    @Override
    public int compareTo(@NonNull JvmProcessInfo other) {
        return Long.compare(pid, other.pid);
    }
}

