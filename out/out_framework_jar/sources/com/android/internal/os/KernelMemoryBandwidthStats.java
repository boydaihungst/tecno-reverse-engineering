package com.android.internal.os;

import android.os.StrictMode;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.LongSparseLongArray;
import android.util.Slog;
import android.util.TimeUtils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
/* loaded from: classes4.dex */
public class KernelMemoryBandwidthStats {
    private static final boolean DEBUG = false;
    private static final String TAG = "KernelMemoryBandwidthStats";
    private static final String mSysfsFile = "/sys/kernel/memory_state_time/show_stat";
    protected final LongSparseLongArray mBandwidthEntries = new LongSparseLongArray();
    private boolean mStatsDoNotExist = false;

    public void updateStats() {
        if (this.mStatsDoNotExist) {
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(mSysfsFile));
                try {
                    parseStats(reader);
                    reader.close();
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "No kernel memory bandwidth stats available");
                this.mBandwidthEntries.clear();
                this.mStatsDoNotExist = true;
            } catch (IOException e2) {
                Slog.e(TAG, "Failed to read memory bandwidth: " + e2.getMessage());
                this.mBandwidthEntries.clear();
            }
            StrictMode.setThreadPolicy(policy);
            long readTime = SystemClock.uptimeMillis() - startTime;
            if (readTime > 100) {
                Slog.w(TAG, "Reading memory bandwidth file took " + readTime + "ms");
            }
        } catch (Throwable th3) {
            StrictMode.setThreadPolicy(policy);
            throw th3;
        }
    }

    public void parseStats(BufferedReader reader) throws IOException {
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(' ');
        this.mBandwidthEntries.clear();
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                splitter.setString(line);
                splitter.next();
                int bandwidth = 0;
                do {
                    int index = this.mBandwidthEntries.indexOfKey(bandwidth);
                    if (index >= 0) {
                        LongSparseLongArray longSparseLongArray = this.mBandwidthEntries;
                        longSparseLongArray.put(bandwidth, longSparseLongArray.valueAt(index) + (Long.parseLong(splitter.next()) / TimeUtils.NANOS_PER_MS));
                    } else {
                        this.mBandwidthEntries.put(bandwidth, Long.parseLong(splitter.next()) / TimeUtils.NANOS_PER_MS);
                    }
                    bandwidth++;
                } while (splitter.hasNext());
            } else {
                return;
            }
        }
    }

    public LongSparseLongArray getBandwidthEntries() {
        return this.mBandwidthEntries;
    }
}
