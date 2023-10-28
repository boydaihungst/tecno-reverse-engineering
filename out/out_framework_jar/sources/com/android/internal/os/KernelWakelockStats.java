package com.android.internal.os;

import java.util.HashMap;
/* loaded from: classes4.dex */
public class KernelWakelockStats extends HashMap<String, Entry> {
    int kernelWakelockVersion;

    /* loaded from: classes4.dex */
    public static class Entry {
        public int mCount;
        public long mTotalTime;
        public int mVersion;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Entry(int count, long totalTime, int version) {
            this.mCount = count;
            this.mTotalTime = totalTime;
            this.mVersion = version;
        }
    }
}
