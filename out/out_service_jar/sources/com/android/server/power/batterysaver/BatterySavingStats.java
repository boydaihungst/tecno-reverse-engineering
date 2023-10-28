package com.android.server.power.batterysaver;

import android.os.BatteryManagerInternal;
import android.os.SystemClock;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes2.dex */
public class BatterySavingStats {
    private static final boolean DEBUG = false;
    private static final int STATE_NOT_INITIALIZED = -1;
    private static final String TAG = "BatterySavingStats";
    private final Object mLock;
    private int mCurrentState = -1;
    final SparseArray<Stat> mStats = new SparseArray<>();
    private int mBatterySaverEnabledCount = 0;
    private long mLastBatterySaverEnabledTime = 0;
    private long mLastBatterySaverDisabledTime = 0;
    private int mAdaptiveBatterySaverEnabledCount = 0;
    private long mLastAdaptiveBatterySaverEnabledTime = 0;
    private long mLastAdaptiveBatterySaverDisabledTime = 0;
    private BatteryManagerInternal mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface BatterySaverState {
        public static final int ADAPTIVE = 2;
        public static final int BITS = 2;
        public static final int MASK = 3;
        public static final int OFF = 0;
        public static final int ON = 1;
        public static final int SHIFT = 0;

        static int fromIndex(int index) {
            return (index >> 0) & 3;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface InteractiveState {
        public static final int BITS = 1;
        public static final int INTERACTIVE = 1;
        public static final int MASK = 1;
        public static final int NON_INTERACTIVE = 0;
        public static final int SHIFT = 2;

        static int fromIndex(int index) {
            return (index >> 2) & 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface DozeState {
        public static final int BITS = 2;
        public static final int DEEP = 2;
        public static final int LIGHT = 1;
        public static final int MASK = 3;
        public static final int NOT_DOZING = 0;
        public static final int SHIFT = 3;

        static int fromIndex(int index) {
            return (index >> 3) & 3;
        }
    }

    /* loaded from: classes2.dex */
    interface PlugState {
        public static final int BITS = 1;
        public static final int MASK = 1;
        public static final int PLUGGED = 1;
        public static final int SHIFT = 5;
        public static final int UNPLUGGED = 0;

        static int fromIndex(int index) {
            return (index >> 5) & 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Stat {
        public int endBatteryLevel;
        public int endBatteryPercent;
        public long endTime;
        public int startBatteryLevel;
        public int startBatteryPercent;
        public long startTime;
        public int totalBatteryDrain;
        public int totalBatteryDrainPercent;
        public long totalTimeMillis;

        Stat() {
        }

        public long totalMinutes() {
            return this.totalTimeMillis / 60000;
        }

        public double drainPerHour() {
            long j = this.totalTimeMillis;
            if (j == 0) {
                return 0.0d;
            }
            return this.totalBatteryDrain / (j / 3600000.0d);
        }

        public double drainPercentPerHour() {
            long j = this.totalTimeMillis;
            if (j == 0) {
                return 0.0d;
            }
            return this.totalBatteryDrainPercent / (j / 3600000.0d);
        }

        String toStringForTest() {
            return "{" + totalMinutes() + "m," + this.totalBatteryDrain + "," + String.format("%.2f", Double.valueOf(drainPerHour())) + "uA/H," + String.format("%.2f", Double.valueOf(drainPercentPerHour())) + "%}";
        }
    }

    public BatterySavingStats(Object lock) {
        this.mLock = lock;
    }

    private BatteryManagerInternal getBatteryManagerInternal() {
        if (this.mBatteryManagerInternal == null) {
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryManagerInternal = batteryManagerInternal;
            if (batteryManagerInternal == null) {
                Slog.wtf(TAG, "BatteryManagerInternal not initialized");
            }
        }
        return this.mBatteryManagerInternal;
    }

    static int statesToIndex(int batterySaverState, int interactiveState, int dozeState, int plugState) {
        int ret = batterySaverState & 3;
        return ret | ((interactiveState & 1) << 2) | ((dozeState & 3) << 3) | ((plugState & 1) << 5);
    }

    static String stateToString(int state) {
        switch (state) {
            case -1:
                return "NotInitialized";
            default:
                return "BS=" + BatterySaverState.fromIndex(state) + ",I=" + InteractiveState.fromIndex(state) + ",D=" + DozeState.fromIndex(state) + ",P=" + PlugState.fromIndex(state);
        }
    }

    Stat getStat(int stateIndex) {
        Stat stat;
        synchronized (this.mLock) {
            stat = this.mStats.get(stateIndex);
            if (stat == null) {
                stat = new Stat();
                this.mStats.put(stateIndex, stat);
            }
        }
        return stat;
    }

    private Stat getStat(int batterySaverState, int interactiveState, int dozeState, int plugState) {
        return getStat(statesToIndex(batterySaverState, interactiveState, dozeState, plugState));
    }

    long injectCurrentTime() {
        return SystemClock.elapsedRealtime();
    }

    int injectBatteryLevel() {
        BatteryManagerInternal bmi = getBatteryManagerInternal();
        if (bmi == null) {
            return 0;
        }
        return bmi.getBatteryChargeCounter();
    }

    int injectBatteryPercent() {
        BatteryManagerInternal bmi = getBatteryManagerInternal();
        if (bmi == null) {
            return 0;
        }
        return bmi.getBatteryLevel();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transitionState(int batterySaverState, int interactiveState, int dozeState, int plugState) {
        synchronized (this.mLock) {
            int newState = statesToIndex(batterySaverState, interactiveState, dozeState, plugState);
            transitionStateLocked(newState);
        }
    }

    private void transitionStateLocked(int newState) {
        if (this.mCurrentState == newState) {
            return;
        }
        long now = injectCurrentTime();
        int batteryLevel = injectBatteryLevel();
        int batteryPercent = injectBatteryPercent();
        int i = this.mCurrentState;
        int oldBatterySaverState = i < 0 ? 0 : BatterySaverState.fromIndex(i);
        int newBatterySaverState = newState >= 0 ? BatterySaverState.fromIndex(newState) : 0;
        if (oldBatterySaverState != newBatterySaverState) {
            switch (newBatterySaverState) {
                case 0:
                    if (oldBatterySaverState == 1) {
                        this.mLastBatterySaverDisabledTime = now;
                        break;
                    } else {
                        this.mLastAdaptiveBatterySaverDisabledTime = now;
                        break;
                    }
                case 1:
                    this.mBatterySaverEnabledCount++;
                    this.mLastBatterySaverEnabledTime = now;
                    if (oldBatterySaverState == 2) {
                        this.mLastAdaptiveBatterySaverDisabledTime = now;
                        break;
                    }
                    break;
                case 2:
                    this.mAdaptiveBatterySaverEnabledCount++;
                    this.mLastAdaptiveBatterySaverEnabledTime = now;
                    if (oldBatterySaverState == 1) {
                        this.mLastBatterySaverDisabledTime = now;
                        break;
                    }
                    break;
            }
        }
        endLastStateLocked(now, batteryLevel, batteryPercent);
        startNewStateLocked(newState, now, batteryLevel, batteryPercent);
    }

    private void endLastStateLocked(long now, int batteryLevel, int batteryPercent) {
        int i = this.mCurrentState;
        if (i < 0) {
            return;
        }
        Stat stat = getStat(i);
        stat.endBatteryLevel = batteryLevel;
        stat.endBatteryPercent = batteryPercent;
        stat.endTime = now;
        long deltaTime = stat.endTime - stat.startTime;
        int deltaDrain = stat.startBatteryLevel - stat.endBatteryLevel;
        int deltaPercent = stat.startBatteryPercent - stat.endBatteryPercent;
        stat.totalTimeMillis += deltaTime;
        stat.totalBatteryDrain += deltaDrain;
        stat.totalBatteryDrainPercent += deltaPercent;
        EventLogTags.writeBatterySavingStats(BatterySaverState.fromIndex(this.mCurrentState), InteractiveState.fromIndex(this.mCurrentState), DozeState.fromIndex(this.mCurrentState), deltaTime, deltaDrain, deltaPercent, stat.totalTimeMillis, stat.totalBatteryDrain, stat.totalBatteryDrainPercent);
    }

    private void startNewStateLocked(int newState, long now, int batteryLevel, int batteryPercent) {
        this.mCurrentState = newState;
        if (newState < 0) {
            return;
        }
        Stat stat = getStat(newState);
        stat.startBatteryLevel = batteryLevel;
        stat.startBatteryPercent = batteryPercent;
        stat.startTime = now;
        stat.endTime = 0L;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Battery saving stats:");
        pw.increaseIndent();
        synchronized (this.mLock) {
            long now = System.currentTimeMillis();
            long nowElapsed = injectCurrentTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            pw.print("Battery Saver is currently: ");
            switch (BatterySaverState.fromIndex(this.mCurrentState)) {
                case 0:
                    pw.println("OFF");
                    break;
                case 1:
                    pw.println("ON");
                    break;
                case 2:
                    pw.println("ADAPTIVE");
                    break;
            }
            pw.increaseIndent();
            if (this.mLastBatterySaverEnabledTime > 0) {
                pw.print("Last ON time: ");
                pw.print(sdf.format(new Date((now - nowElapsed) + this.mLastBatterySaverEnabledTime)));
                pw.print(" ");
                TimeUtils.formatDuration(this.mLastBatterySaverEnabledTime, nowElapsed, pw);
                pw.println();
            }
            if (this.mLastBatterySaverDisabledTime > 0) {
                pw.print("Last OFF time: ");
                pw.print(sdf.format(new Date((now - nowElapsed) + this.mLastBatterySaverDisabledTime)));
                pw.print(" ");
                TimeUtils.formatDuration(this.mLastBatterySaverDisabledTime, nowElapsed, pw);
                pw.println();
            }
            pw.print("Times full enabled: ");
            pw.println(this.mBatterySaverEnabledCount);
            if (this.mLastAdaptiveBatterySaverEnabledTime > 0) {
                pw.print("Last ADAPTIVE ON time: ");
                pw.print(sdf.format(new Date((now - nowElapsed) + this.mLastAdaptiveBatterySaverEnabledTime)));
                pw.print(" ");
                TimeUtils.formatDuration(this.mLastAdaptiveBatterySaverEnabledTime, nowElapsed, pw);
                pw.println();
            }
            if (this.mLastAdaptiveBatterySaverDisabledTime > 0) {
                pw.print("Last ADAPTIVE OFF time: ");
                pw.print(sdf.format(new Date((now - nowElapsed) + this.mLastAdaptiveBatterySaverDisabledTime)));
                pw.print(" ");
                TimeUtils.formatDuration(this.mLastAdaptiveBatterySaverDisabledTime, nowElapsed, pw);
                pw.println();
            }
            pw.print("Times adaptive enabled: ");
            pw.println(this.mAdaptiveBatterySaverEnabledCount);
            pw.decreaseIndent();
            pw.println();
            pw.println("Drain stats:");
            pw.println("                   Battery saver OFF                          ON");
            dumpLineLocked(pw, 0, "NonIntr", 0, "NonDoze");
            dumpLineLocked(pw, 1, "   Intr", 0, "       ");
            dumpLineLocked(pw, 0, "NonIntr", 2, "Deep   ");
            dumpLineLocked(pw, 1, "   Intr", 2, "       ");
            dumpLineLocked(pw, 0, "NonIntr", 1, "Light  ");
            dumpLineLocked(pw, 1, "   Intr", 1, "       ");
        }
        pw.decreaseIndent();
    }

    private void dumpLineLocked(IndentingPrintWriter pw, int interactiveState, String interactiveLabel, int dozeState, String dozeLabel) {
        pw.print(dozeLabel);
        pw.print(" ");
        pw.print(interactiveLabel);
        pw.print(": ");
        Stat offStat = getStat(0, interactiveState, dozeState, 0);
        Stat onStat = getStat(1, interactiveState, dozeState, 0);
        pw.println(String.format("%6dm %6dmAh(%3d%%) %8.1fmAh/h     %6dm %6dmAh(%3d%%) %8.1fmAh/h", Long.valueOf(offStat.totalMinutes()), Integer.valueOf(offStat.totalBatteryDrain / 1000), Integer.valueOf(offStat.totalBatteryDrainPercent), Double.valueOf(offStat.drainPerHour() / 1000.0d), Long.valueOf(onStat.totalMinutes()), Integer.valueOf(onStat.totalBatteryDrain / 1000), Integer.valueOf(onStat.totalBatteryDrainPercent), Double.valueOf(onStat.drainPerHour() / 1000.0d)));
    }
}
