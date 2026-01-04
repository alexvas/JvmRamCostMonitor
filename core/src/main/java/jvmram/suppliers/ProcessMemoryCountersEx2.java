package jvmram.suppliers;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;

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
public class ProcessMemoryCountersEx2 extends Structure {
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
