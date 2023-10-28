package com.android.internal.os;

import android.os.Process;
import com.android.internal.os.KernelSingleProcessCpuThreadReader;
import java.io.IOException;
/* loaded from: classes4.dex */
public class SystemServerCpuThreadReader {
    private final SystemServiceCpuThreadTimes mDeltaCpuThreadTimes;
    private final KernelSingleProcessCpuThreadReader mKernelCpuThreadReader;
    private long[] mLastBinderThreadCpuTimesUs;
    private long[] mLastThreadCpuTimesUs;

    /* loaded from: classes4.dex */
    public static class SystemServiceCpuThreadTimes {
        public long[] binderThreadCpuTimesUs;
        public long[] threadCpuTimesUs;
    }

    public static SystemServerCpuThreadReader create() {
        return new SystemServerCpuThreadReader(KernelSingleProcessCpuThreadReader.create(Process.myPid()));
    }

    public SystemServerCpuThreadReader(int pid, KernelSingleProcessCpuThreadReader.CpuTimeInStateReader cpuTimeInStateReader) throws IOException {
        this(new KernelSingleProcessCpuThreadReader(pid, cpuTimeInStateReader));
    }

    public SystemServerCpuThreadReader(KernelSingleProcessCpuThreadReader kernelCpuThreadReader) {
        this.mDeltaCpuThreadTimes = new SystemServiceCpuThreadTimes();
        this.mKernelCpuThreadReader = kernelCpuThreadReader;
    }

    public void startTrackingThreadCpuTime() {
        this.mKernelCpuThreadReader.startTrackingThreadCpuTimes();
    }

    public void setBinderThreadNativeTids(int[] nativeTids) {
        this.mKernelCpuThreadReader.setSelectedThreadIds(nativeTids);
    }

    public SystemServiceCpuThreadTimes readDelta() {
        int numCpuFrequencies = this.mKernelCpuThreadReader.getCpuFrequencyCount();
        if (this.mLastThreadCpuTimesUs == null) {
            this.mLastThreadCpuTimesUs = new long[numCpuFrequencies];
            this.mLastBinderThreadCpuTimesUs = new long[numCpuFrequencies];
            this.mDeltaCpuThreadTimes.threadCpuTimesUs = new long[numCpuFrequencies];
            this.mDeltaCpuThreadTimes.binderThreadCpuTimesUs = new long[numCpuFrequencies];
        }
        KernelSingleProcessCpuThreadReader.ProcessCpuUsage processCpuUsage = this.mKernelCpuThreadReader.getProcessCpuUsage();
        if (processCpuUsage == null) {
            return null;
        }
        for (int i = numCpuFrequencies - 1; i >= 0; i--) {
            long threadCpuTimesUs = processCpuUsage.threadCpuTimesMillis[i] * 1000;
            long binderThreadCpuTimesUs = processCpuUsage.selectedThreadCpuTimesMillis[i] * 1000;
            this.mDeltaCpuThreadTimes.threadCpuTimesUs[i] = Math.max(0L, threadCpuTimesUs - this.mLastThreadCpuTimesUs[i]);
            this.mDeltaCpuThreadTimes.binderThreadCpuTimesUs[i] = Math.max(0L, binderThreadCpuTimesUs - this.mLastBinderThreadCpuTimesUs[i]);
            this.mLastThreadCpuTimesUs[i] = threadCpuTimesUs;
            this.mLastBinderThreadCpuTimesUs[i] = binderThreadCpuTimesUs;
        }
        return this.mDeltaCpuThreadTimes;
    }

    public SystemServiceCpuThreadTimes readAbsolute() {
        int numCpuFrequencies = this.mKernelCpuThreadReader.getCpuFrequencyCount();
        KernelSingleProcessCpuThreadReader.ProcessCpuUsage processCpuUsage = this.mKernelCpuThreadReader.getProcessCpuUsage();
        if (processCpuUsage == null) {
            return null;
        }
        SystemServiceCpuThreadTimes result = new SystemServiceCpuThreadTimes();
        result.threadCpuTimesUs = new long[numCpuFrequencies];
        result.binderThreadCpuTimesUs = new long[numCpuFrequencies];
        for (int i = 0; i < numCpuFrequencies; i++) {
            result.threadCpuTimesUs[i] = processCpuUsage.threadCpuTimesMillis[i] * 1000;
            result.binderThreadCpuTimesUs[i] = processCpuUsage.selectedThreadCpuTimesMillis[i] * 1000;
        }
        return result;
    }
}
