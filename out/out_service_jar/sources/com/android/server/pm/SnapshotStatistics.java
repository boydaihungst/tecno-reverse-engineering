package com.android.server.pm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import com.android.server.EventLogTags;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
/* loaded from: classes2.dex */
public class SnapshotStatistics {
    public static final int SNAPSHOT_BIG_BUILD_TIME_US = 10000;
    public static final int SNAPSHOT_BUILD_REPORT_LIMIT = 10;
    public static final int SNAPSHOT_LONG_TICKS = 10080;
    public static final int SNAPSHOT_REPORTABLE_BUILD_TIME_US = 30000;
    public static final int SNAPSHOT_SHORT_LIFETIME = 5;
    public static final int SNAPSHOT_TICK_INTERVAL_MS = 60000;
    private static final int US_IN_MS = 1000;
    private Handler mHandler;
    private Stats[] mLong;
    private Stats[] mShort;
    private final Object mLock = new Object();
    private int mEventsReported = 0;
    private int mTicks = 0;
    private long mLastBuildTime = 0;
    private final BinMap mTimeBins = new BinMap(new int[]{1, 2, 5, 10, 20, 50, 100});
    private final BinMap mUseBins = new BinMap(new int[]{1, 2, 5, 10, 20, 50, 100});

    private int usToMs(int us) {
        return us / 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class BinMap {
        private int[] mBinMap;
        private int mCount;
        private int mMaxBin;
        private int[] mUserKey;

        BinMap(int[] userKey) {
            int[] copyOf = Arrays.copyOf(userKey, userKey.length);
            this.mUserKey = copyOf;
            this.mCount = copyOf.length + 1;
            int i = copyOf[copyOf.length - 1] + 1;
            this.mMaxBin = i;
            this.mBinMap = new int[i + 1];
            int j = 0;
            int i2 = 0;
            while (true) {
                int[] iArr = this.mUserKey;
                if (i2 < iArr.length) {
                    while (j <= this.mUserKey[i2]) {
                        this.mBinMap[j] = i2;
                        j++;
                    }
                    i2++;
                } else {
                    this.mBinMap[this.mMaxBin] = iArr.length;
                    return;
                }
            }
        }

        public int getBin(int x) {
            if (x >= 0 && x < this.mMaxBin) {
                return this.mBinMap[x];
            }
            int i = this.mMaxBin;
            if (x >= i) {
                return this.mBinMap[i];
            }
            return 0;
        }

        public int count() {
            return this.mCount;
        }

        public int[] userKeys() {
            return this.mUserKey;
        }
    }

    /* loaded from: classes2.dex */
    public class Stats {
        public int mBigBuilds;
        public int mMaxBuildTimeUs;
        public int mShortLived;
        public long mStartTimeUs;
        public long mStopTimeUs;
        public int[] mTimes;
        public int mTotalBuilds;
        public int mTotalCorked;
        public long mTotalTimeUs;
        public int mTotalUsed;
        public int[] mUsed;

        /* JADX INFO: Access modifiers changed from: private */
        public void rebuild(int duration, int used, int buildBin, int useBin, boolean big, boolean quick) {
            this.mTotalBuilds++;
            int[] iArr = this.mTimes;
            iArr[buildBin] = iArr[buildBin] + 1;
            if (used >= 0) {
                this.mTotalUsed += used;
                int[] iArr2 = this.mUsed;
                iArr2[useBin] = iArr2[useBin] + 1;
            }
            this.mTotalTimeUs += duration;
            if (big) {
                this.mBigBuilds++;
            }
            if (quick) {
                this.mShortLived++;
            }
            if (this.mMaxBuildTimeUs < duration) {
                this.mMaxBuildTimeUs = duration;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void corked() {
            this.mTotalCorked++;
        }

        private Stats(long now) {
            this.mStartTimeUs = 0L;
            this.mStopTimeUs = 0L;
            this.mTotalBuilds = 0;
            this.mTotalUsed = 0;
            this.mTotalCorked = 0;
            this.mBigBuilds = 0;
            this.mShortLived = 0;
            this.mTotalTimeUs = 0L;
            this.mMaxBuildTimeUs = 0;
            this.mStartTimeUs = now;
            this.mTimes = new int[SnapshotStatistics.this.mTimeBins.count()];
            this.mUsed = new int[SnapshotStatistics.this.mUseBins.count()];
        }

        private Stats(Stats orig) {
            this.mStartTimeUs = 0L;
            this.mStopTimeUs = 0L;
            this.mTotalBuilds = 0;
            this.mTotalUsed = 0;
            this.mTotalCorked = 0;
            this.mBigBuilds = 0;
            this.mShortLived = 0;
            this.mTotalTimeUs = 0L;
            this.mMaxBuildTimeUs = 0;
            this.mStartTimeUs = orig.mStartTimeUs;
            this.mStopTimeUs = orig.mStopTimeUs;
            int[] iArr = orig.mTimes;
            this.mTimes = Arrays.copyOf(iArr, iArr.length);
            int[] iArr2 = orig.mUsed;
            this.mUsed = Arrays.copyOf(iArr2, iArr2.length);
            this.mTotalBuilds = orig.mTotalBuilds;
            this.mTotalUsed = orig.mTotalUsed;
            this.mTotalCorked = orig.mTotalCorked;
            this.mBigBuilds = orig.mBigBuilds;
            this.mShortLived = orig.mShortLived;
            this.mTotalTimeUs = orig.mTotalTimeUs;
            this.mMaxBuildTimeUs = orig.mMaxBuildTimeUs;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void complete(long stop) {
            this.mStopTimeUs = stop;
        }

        private String durationToString(long us) {
            int s = (int) (us / 1000000);
            int m = s / 60;
            int s2 = s % 60;
            int h = m / 60;
            int m2 = m % 60;
            int d = h / 24;
            int h2 = h % 24;
            return d != 0 ? TextUtils.formatSimple("%2d:%02d:%02d:%02d", new Object[]{Integer.valueOf(d), Integer.valueOf(h2), Integer.valueOf(m2), Integer.valueOf(s2)}) : h2 != 0 ? TextUtils.formatSimple("%2s %02d:%02d:%02d", new Object[]{"", Integer.valueOf(h2), Integer.valueOf(m2), Integer.valueOf(s2)}) : TextUtils.formatSimple("%2s %2s %2d:%02d", new Object[]{"", "", Integer.valueOf(m2), Integer.valueOf(s2)});
        }

        private void dumpPrefix(PrintWriter pw, String indent, long now, boolean header, String title) {
            pw.print(indent + " ");
            if (header) {
                pw.format(Locale.US, "%-23s", title);
                return;
            }
            pw.format(Locale.US, "%11s", durationToString(now - this.mStartTimeUs));
            if (this.mStopTimeUs != 0) {
                pw.format(Locale.US, " %11s", durationToString(now - this.mStopTimeUs));
            } else {
                pw.format(Locale.US, " %11s", "now");
            }
        }

        private void dumpStats(PrintWriter pw, String indent, long now, boolean header) {
            dumpPrefix(pw, indent, now, header, "Summary stats");
            if (header) {
                pw.format(Locale.US, "  %10s  %10s  %10s  %10s  %10s  %10s  %10s", "TotBlds", "TotUsed", "TotCork", "BigBlds", "ShortLvd", "TotTime", "MaxTime");
            } else {
                pw.format(Locale.US, "  %10d  %10d  %10d  %10d  %10d  %10d  %10d", Integer.valueOf(this.mTotalBuilds), Integer.valueOf(this.mTotalUsed), Integer.valueOf(this.mTotalCorked), Integer.valueOf(this.mBigBuilds), Integer.valueOf(this.mShortLived), Long.valueOf(this.mTotalTimeUs / 1000), Integer.valueOf(this.mMaxBuildTimeUs / 1000));
            }
            pw.println();
        }

        private void dumpTimes(PrintWriter pw, String indent, long now, boolean header) {
            dumpPrefix(pw, indent, now, header, "Build times");
            if (header) {
                int[] keys = SnapshotStatistics.this.mTimeBins.userKeys();
                for (int i = 0; i < keys.length; i++) {
                    pw.format(Locale.US, "  %10s", TextUtils.formatSimple("<= %dms", new Object[]{Integer.valueOf(keys[i])}));
                }
                pw.format(Locale.US, "  %10s", TextUtils.formatSimple("> %dms", new Object[]{Integer.valueOf(keys[keys.length - 1])}));
            } else {
                for (int i2 = 0; i2 < this.mTimes.length; i2++) {
                    pw.format(Locale.US, "  %10d", Integer.valueOf(this.mTimes[i2]));
                }
            }
            pw.println();
        }

        private void dumpUsage(PrintWriter pw, String indent, long now, boolean header) {
            dumpPrefix(pw, indent, now, header, "Use counters");
            if (header) {
                int[] keys = SnapshotStatistics.this.mUseBins.userKeys();
                for (int i = 0; i < keys.length; i++) {
                    pw.format(Locale.US, "  %10s", TextUtils.formatSimple("<= %d", new Object[]{Integer.valueOf(keys[i])}));
                }
                pw.format(Locale.US, "  %10s", TextUtils.formatSimple("> %d", new Object[]{Integer.valueOf(keys[keys.length - 1])}));
            } else {
                for (int i2 = 0; i2 < this.mUsed.length; i2++) {
                    pw.format(Locale.US, "  %10d", Integer.valueOf(this.mUsed[i2]));
                }
            }
            pw.println();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, String indent, long now, boolean header, String what) {
            if (what.equals("stats")) {
                dumpStats(pw, indent, now, header);
            } else if (what.equals("times")) {
                dumpTimes(pw, indent, now, header);
            } else if (what.equals("usage")) {
                dumpUsage(pw, indent, now, header);
            } else {
                throw new IllegalArgumentException("unrecognized choice: " + what);
            }
        }

        private void report() {
            EventLogTags.writePmSnapshotStats(this.mTotalBuilds, this.mTotalUsed, this.mBigBuilds, this.mShortLived, this.mMaxBuildTimeUs / 1000, this.mTotalTimeUs / 1000);
        }
    }

    public SnapshotStatistics() {
        this.mHandler = null;
        long now = SystemClock.currentTimeMicro();
        Stats[] statsArr = new Stats[2];
        this.mLong = statsArr;
        statsArr[0] = new Stats(now);
        Stats[] statsArr2 = new Stats[10];
        this.mShort = statsArr2;
        statsArr2[0] = new Stats(now);
        this.mHandler = new Handler(Looper.getMainLooper()) { // from class: com.android.server.pm.SnapshotStatistics.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                SnapshotStatistics.this.handleMessage(msg);
            }
        };
        scheduleTick();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMessage(Message msg) {
        tick();
        scheduleTick();
    }

    private void scheduleTick() {
        this.mHandler.sendEmptyMessageDelayed(0, 60000L);
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:27:0x006a */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r4v0, types: [long] */
    /* JADX WARN: Type inference failed for: r4v1 */
    /* JADX WARN: Type inference failed for: r4v2 */
    public final void rebuild(long now, long done, int hits) {
        ?? r4 = done - now;
        int duration = (int) r4;
        boolean reportEvent = false;
        Object obj = this.mLock;
        synchronized (obj) {
            try {
                try {
                    this.mLastBuildTime = now;
                    int timeBin = this.mTimeBins.getBin(duration / 1000);
                    int useBin = this.mUseBins.getBin(hits);
                    boolean big = duration >= 10000;
                    boolean quick = hits <= 5;
                    this.mShort[0].rebuild(duration, hits, timeBin, useBin, big, quick);
                    this.mLong[0].rebuild(duration, hits, timeBin, useBin, big, quick);
                    if (duration >= 30000) {
                        int i = this.mEventsReported;
                        this.mEventsReported = i + 1;
                        if (i < 10) {
                            reportEvent = true;
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    r4 = obj;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (reportEvent) {
            EventLogTags.writePmSnapshotRebuild(duration / 1000, hits);
        }
    }

    public final void corked() {
        synchronized (this.mLock) {
            this.mShort[0].corked();
            this.mLong[0].corked();
        }
    }

    private void shift(Stats[] s, long now) {
        s[0].complete(now);
        for (int i = s.length - 1; i > 0; i--) {
            s[i] = s[i - 1];
        }
        s[0] = new Stats(now);
    }

    private void tick() {
        synchronized (this.mLock) {
            long now = SystemClock.currentTimeMicro();
            int i = this.mTicks + 1;
            this.mTicks = i;
            if (i % 10080 == 0) {
                shift(this.mLong, now);
            }
            shift(this.mShort, now);
            this.mEventsReported = 0;
        }
    }

    private void dump(PrintWriter pw, String indent, long now, Stats[] l, Stats[] s, String what) {
        l[0].dump(pw, indent, now, true, what);
        for (int i = 0; i < s.length; i++) {
            if (s[i] != null) {
                s[i].dump(pw, indent, now, false, what);
            }
        }
        for (int i2 = 0; i2 < l.length; i2++) {
            if (l[i2] != null) {
                l[i2].dump(pw, indent, now, false, what);
            }
        }
    }

    public void dump(PrintWriter pw, String indent, long now, int unrecorded, int corkLevel, boolean brief) {
        Stats[] l;
        Stats[] s;
        synchronized (this.mLock) {
            try {
                Stats[] statsArr = this.mLong;
                l = (Stats[]) Arrays.copyOf(statsArr, statsArr.length);
                l[0] = new Stats(l[0]);
                Stats[] statsArr2 = this.mShort;
                s = (Stats[]) Arrays.copyOf(statsArr2, statsArr2.length);
                s[0] = new Stats(s[0]);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        pw.format(Locale.US, "%s Unrecorded-hits: %d  Cork-level: %d", indent, Integer.valueOf(unrecorded), Integer.valueOf(corkLevel));
        pw.println();
        dump(pw, indent, now, l, s, "stats");
        if (brief) {
            return;
        }
        pw.println();
        dump(pw, indent, now, l, s, "times");
        pw.println();
        dump(pw, indent, now, l, s, "usage");
    }
}
