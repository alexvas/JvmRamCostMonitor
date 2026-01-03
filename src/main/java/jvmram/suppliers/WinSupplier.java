package jvmram.suppliers;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import jvmram.Config;
import jvmram.suppliers.data.WinData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static jvmram.metrics.RamMetric.Os.WINDOWS;

class WinSupplier extends AbstractDataSupplier<WinData> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private interface Psapi extends com.sun.jna.Library {
        Psapi INSTANCE = Native.load("psapi", Psapi.class);

        boolean GetProcessMemoryInfo(WinNT.HANDLE hProcess, PROCESS_MEMORY_COUNTERS_EX2 ppsmemCounters, int cb);
    }

    @Structure.FieldOrder({
            "cb",
            "PageFaultCount",
            "PeakWorkingSetSize",
            "WorkingSetSize",
            "QuotaPeakPagedPoolUsage",
            "QuotaPagedPoolUsage",
            "QuotaPeakNonPagedPoolUsage",
            "QuotaNonPagedPoolUsage",
            "PagefileUsage",
            "PeakPagefileUsage",
            "PrivateUsage",
            "PrivateWorkingSetSize",
            "SharedCommitUsage"
    })
    private static class PROCESS_MEMORY_COUNTERS_EX2 extends Structure {
        public WinDef.DWORD cb;
        public WinDef.DWORD PageFaultCount;
        public BaseTSD.SIZE_T PeakWorkingSetSize;
        public BaseTSD.SIZE_T WorkingSetSize;
        public BaseTSD.SIZE_T QuotaPeakPagedPoolUsage;
        public BaseTSD.SIZE_T QuotaPagedPoolUsage;
        public BaseTSD.SIZE_T QuotaPeakNonPagedPoolUsage;
        public BaseTSD.SIZE_T QuotaNonPagedPoolUsage;
        public BaseTSD.SIZE_T PagefileUsage;
        public BaseTSD.SIZE_T PeakPagefileUsage;
        public BaseTSD.SIZE_T PrivateUsage;
        public BaseTSD.SIZE_T PrivateWorkingSetSize;
        public WinDef.ULONGLONG SharedCommitUsage;
    }

    WinSupplier(long pid) {
        super(pid);
        if (Config.os != WINDOWS) {
            LOG.error("The supplier is intended for use in Windows OS only");
        } else {
            setInitialized();
        }
    }
    
    @Override
    @Nullable WinData doGetData() {
        var hProcess = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false,
                (int) pid
        );
        if (hProcess == null) {
            LOG.warn("Failed to open process handle for pid {}", pid);
            return null;
        }
        try {
            var pmc = new PROCESS_MEMORY_COUNTERS_EX2();
            int size = pmc.size();
            pmc.cb = new WinDef.DWORD(size);
            boolean success = Psapi.INSTANCE.GetProcessMemoryInfo(hProcess, pmc, size);
            if (success) {
                return new WinData(
                        pmc.WorkingSetSize.longValue(),
                        pmc.PrivateUsage.longValue()
                );
            } else {
                LOG.warn("Failed to get process memory info for pid {}", pid);
                return null;
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(hProcess);
        }
    }
}
