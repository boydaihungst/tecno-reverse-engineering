package com.android.server.am;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.BatteryConsumer;
import android.os.BatteryStatsInternal;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.MessageQueue;
import android.os.PowerExemptionManager;
import android.os.SystemClock;
import android.os.UidBatteryConsumer;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.AppRestrictionController;
import com.android.server.am.BaseAppStateTracker;
import com.android.server.pm.UserManagerInternal;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppBatteryTracker extends BaseAppStateTracker<AppBatteryPolicy> implements AppRestrictionController.UidBatteryUsageProvider {
    static final ImmutableBatteryUsage BATTERY_USAGE_NONE = new ImmutableBatteryUsage();
    static final long BATTERY_USAGE_STATS_POLLING_INTERVAL_MS_DEBUG = 2000;
    static final long BATTERY_USAGE_STATS_POLLING_INTERVAL_MS_LONG = 1800000;
    static final long BATTERY_USAGE_STATS_POLLING_MIN_INTERVAL_MS_DEBUG = 2000;
    static final long BATTERY_USAGE_STATS_POLLING_MIN_INTERVAL_MS_LONG = 300000;
    static final boolean DEBUG_BACKGROUND_BATTERY_TRACKER = false;
    static final boolean DEBUG_BACKGROUND_BATTERY_TRACKER_VERBOSE = false;
    static final String TAG = "ActivityManager";
    private final SparseBooleanArray mActiveUserIdStates;
    private final long mBatteryUsageStatsPollingIntervalMs;
    private final long mBatteryUsageStatsPollingMinIntervalMs;
    private boolean mBatteryUsageStatsUpdatePending;
    private final Runnable mBgBatteryUsageStatsCheck;
    private final Runnable mBgBatteryUsageStatsPolling;
    private final SparseArray<ImmutableBatteryUsage> mDebugUidPercentages;
    private long mLastBatteryUsageSamplingTs;
    private long mLastReportTime;
    private final SparseArray<ImmutableBatteryUsage> mLastUidBatteryUsage;
    private long mLastUidBatteryUsageStartTs;
    private final SparseArray<BatteryUsage> mTmpUidBatteryUsage;
    private final SparseArray<ImmutableBatteryUsage> mTmpUidBatteryUsage2;
    private final SparseArray<ImmutableBatteryUsage> mTmpUidBatteryUsageInWindow;
    private final ArraySet<UserHandle> mTmpUserIds;
    private final SparseArray<BatteryUsage> mUidBatteryUsage;
    private final SparseArray<ImmutableBatteryUsage> mUidBatteryUsageInWindow;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBatteryTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppBatteryTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppBatteryPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mBgBatteryUsageStatsPolling = new Runnable() { // from class: com.android.server.am.AppBatteryTracker$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppBatteryTracker.this.updateBatteryUsageStatsAndCheck();
            }
        };
        this.mBgBatteryUsageStatsCheck = new Runnable() { // from class: com.android.server.am.AppBatteryTracker$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AppBatteryTracker.this.checkBatteryUsageStats();
            }
        };
        this.mActiveUserIdStates = new SparseBooleanArray();
        this.mUidBatteryUsage = new SparseArray<>();
        this.mUidBatteryUsageInWindow = new SparseArray<>();
        this.mLastUidBatteryUsage = new SparseArray<>();
        this.mTmpUidBatteryUsage = new SparseArray<>();
        this.mTmpUidBatteryUsage2 = new SparseArray<>();
        this.mTmpUidBatteryUsageInWindow = new SparseArray<>();
        this.mTmpUserIds = new ArraySet<>();
        this.mLastReportTime = 0L;
        this.mDebugUidPercentages = new SparseArray<>();
        if (injector == null) {
            this.mBatteryUsageStatsPollingIntervalMs = 1800000L;
            this.mBatteryUsageStatsPollingMinIntervalMs = 300000L;
        } else {
            this.mBatteryUsageStatsPollingIntervalMs = 2000L;
            this.mBatteryUsageStatsPollingMinIntervalMs = 2000L;
        }
        this.mInjector.setPolicy(new AppBatteryPolicy(this.mInjector, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onSystemReady() {
        super.onSystemReady();
        UserManagerInternal um = this.mInjector.getUserManagerInternal();
        int[] userIds = um.getUserIds();
        for (int userId : userIds) {
            if (um.isUserRunning(userId)) {
                synchronized (this.mLock) {
                    this.mActiveUserIdStates.put(userId, true);
                }
            }
        }
        scheduleBatteryUsageStatsUpdateIfNecessary(this.mBatteryUsageStatsPollingIntervalMs);
    }

    private void scheduleBatteryUsageStatsUpdateIfNecessary(long delay) {
        if (((AppBatteryPolicy) this.mInjector.getPolicy()).isEnabled()) {
            synchronized (this.mLock) {
                if (!this.mBgHandler.hasCallbacks(this.mBgBatteryUsageStatsPolling)) {
                    this.mBgHandler.postDelayed(this.mBgBatteryUsageStatsPolling, delay);
                }
            }
            logAppBatteryTrackerIfNeeded();
        }
    }

    private void logAppBatteryTrackerIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            AppBatteryPolicy bgPolicy = (AppBatteryPolicy) this.mInjector.getPolicy();
            if (now - this.mLastReportTime < bgPolicy.mBgCurrentDrainWindowMs) {
                return;
            }
            this.mLastReportTime = now;
            updateBatteryUsageStatsIfNecessary(this.mInjector.currentTimeMillis(), true);
            synchronized (this.mLock) {
                int size = this.mUidBatteryUsageInWindow.size();
                for (int i = 0; i < size; i++) {
                    int uid = this.mUidBatteryUsageInWindow.keyAt(i);
                    if ((UserHandle.isCore(uid) || UserHandle.isApp(uid)) && !BATTERY_USAGE_NONE.equals(this.mUidBatteryUsageInWindow.valueAt(i))) {
                        FrameworkStatsLog.write((int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO, uid, 0, 0, 0, (byte[]) null, getTrackerInfoForStatsd(uid), (byte[]) null, (byte[]) null, 0, 0, 0, ActivityManager.isLowRamDeviceStatic(), 0);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public byte[] getTrackerInfoForStatsd(int uid) {
        ImmutableBatteryUsage temp;
        synchronized (this.mLock) {
            temp = this.mUidBatteryUsageInWindow.get(uid);
        }
        if (temp == null) {
            return null;
        }
        BatteryUsage bgUsage = temp.calcPercentage(uid, (AppBatteryPolicy) this.mInjector.getPolicy());
        double allUsage = bgUsage.mPercentage[0] + bgUsage.mPercentage[1] + bgUsage.mPercentage[2] + bgUsage.mPercentage[3] + bgUsage.mPercentage[4];
        double usageBackground = bgUsage.mPercentage[2];
        double usageFgs = bgUsage.mPercentage[3];
        double usageForeground = bgUsage.mPercentage[1];
        double usageCached = bgUsage.mPercentage[4];
        ProtoOutputStream proto = new ProtoOutputStream();
        proto.write(CompanionMessage.MESSAGE_ID, allUsage * 10000.0d);
        proto.write(1120986464258L, usageBackground * 10000.0d);
        proto.write(1120986464259L, usageFgs * 10000.0d);
        proto.write(1120986464260L, usageForeground * 10000.0d);
        proto.write(1120986464261L, usageCached * 10000.0d);
        proto.flush();
        return proto.getBytes();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserStarted(int userId) {
        synchronized (this.mLock) {
            this.mActiveUserIdStates.put(userId, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserStopped(int userId) {
        synchronized (this.mLock) {
            this.mActiveUserIdStates.put(userId, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            this.mActiveUserIdStates.delete(userId);
            for (int i = this.mUidBatteryUsage.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mUidBatteryUsage.keyAt(i)) == userId) {
                    this.mUidBatteryUsage.removeAt(i);
                }
            }
            for (int i2 = this.mUidBatteryUsageInWindow.size() - 1; i2 >= 0; i2--) {
                if (UserHandle.getUserId(this.mUidBatteryUsageInWindow.keyAt(i2)) == userId) {
                    this.mUidBatteryUsageInWindow.removeAt(i2);
                }
            }
            ((AppBatteryPolicy) this.mInjector.getPolicy()).onUserRemovedLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUidRemoved(int uid) {
        synchronized (this.mLock) {
            this.mUidBatteryUsage.delete(uid);
            this.mUidBatteryUsageInWindow.delete(uid);
            ((AppBatteryPolicy) this.mInjector.getPolicy()).onUidRemovedLocked(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserInteractionStarted(String packageName, int uid) {
        ((AppBatteryPolicy) this.mInjector.getPolicy()).onUserInteractionStarted(packageName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onBackgroundRestrictionChanged(int uid, String pkgName, boolean restricted) {
        ((AppBatteryPolicy) this.mInjector.getPolicy()).onBackgroundRestrictionChanged(uid, pkgName, restricted);
    }

    @Override // com.android.server.am.AppRestrictionController.UidBatteryUsageProvider
    public ImmutableBatteryUsage getUidBatteryUsage(int uid) {
        ImmutableBatteryUsage immutableBatteryUsage;
        long now = this.mInjector.currentTimeMillis();
        boolean updated = updateBatteryUsageStatsIfNecessary(now, false);
        synchronized (this.mLock) {
            if (updated) {
                this.mBgHandler.removeCallbacks(this.mBgBatteryUsageStatsPolling);
                scheduleBgBatteryUsageStatsCheck();
            }
            BatteryUsage usage = this.mUidBatteryUsage.get(uid);
            immutableBatteryUsage = usage != null ? new ImmutableBatteryUsage(usage) : BATTERY_USAGE_NONE;
        }
        return immutableBatteryUsage;
    }

    private void scheduleBgBatteryUsageStatsCheck() {
        if (!this.mBgHandler.hasCallbacks(this.mBgBatteryUsageStatsCheck)) {
            this.mBgHandler.post(this.mBgBatteryUsageStatsCheck);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBatteryUsageStatsAndCheck() {
        long now = this.mInjector.currentTimeMillis();
        if (updateBatteryUsageStatsIfNecessary(now, false)) {
            checkBatteryUsageStats();
            return;
        }
        synchronized (this.mLock) {
            scheduleBatteryUsageStatsUpdateIfNecessary((this.mLastBatteryUsageSamplingTs + this.mBatteryUsageStatsPollingMinIntervalMs) - now);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkBatteryUsageStats() {
        long now = SystemClock.elapsedRealtime();
        AppBatteryPolicy bgPolicy = (AppBatteryPolicy) this.mInjector.getPolicy();
        try {
            SparseArray<ImmutableBatteryUsage> uidConsumers = this.mTmpUidBatteryUsageInWindow;
            synchronized (this.mLock) {
                copyUidBatteryUsage(this.mUidBatteryUsageInWindow, uidConsumers);
            }
            long since = Math.max(0L, now - bgPolicy.mBgCurrentDrainWindowMs);
            int size = uidConsumers.size();
            for (int i = 0; i < size; i++) {
                int uid = uidConsumers.keyAt(i);
                ImmutableBatteryUsage actualUsage = uidConsumers.valueAt(i);
                ImmutableBatteryUsage exemptedUsage = this.mAppRestrictionController.getUidBatteryExemptedUsageSince(uid, since, now, bgPolicy.mBgCurrentDrainExemptedTypes);
                ImmutableBatteryUsage bgUsage = actualUsage.mutate().subtract(exemptedUsage).calcPercentage(uid, bgPolicy).unmutate();
                bgPolicy.handleUidBatteryUsage(uid, bgUsage);
            }
            int size2 = this.mDebugUidPercentages.size();
            for (int i2 = 0; i2 < size2; i2++) {
                bgPolicy.handleUidBatteryUsage(this.mDebugUidPercentages.keyAt(i2), this.mDebugUidPercentages.valueAt(i2));
            }
        } finally {
            scheduleBatteryUsageStatsUpdateIfNecessary(this.mBatteryUsageStatsPollingIntervalMs);
        }
    }

    private boolean updateBatteryUsageStatsIfNecessary(long now, boolean forceUpdate) {
        boolean needUpdate = false;
        synchronized (this.mLock) {
            if (this.mLastBatteryUsageSamplingTs + this.mBatteryUsageStatsPollingMinIntervalMs >= now && !forceUpdate) {
                return false;
            }
            if (this.mBatteryUsageStatsUpdatePending) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                }
            } else {
                this.mBatteryUsageStatsUpdatePending = true;
                needUpdate = true;
            }
            if (needUpdate) {
                updateBatteryUsageStatsOnce(now);
                synchronized (this.mLock) {
                    this.mLastBatteryUsageSamplingTs = now;
                    this.mBatteryUsageStatsUpdatePending = false;
                    this.mLock.notifyAll();
                }
            }
            return true;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, MOVE, MOVE, MOVE]}, finally: {[MOVE, MOVE, MOVE] complete} */
    /* JADX DEBUG: Different variable names in phi insn: [builder, curStart], use first */
    /* JADX DEBUG: Different variable names in phi insn: [curDuration, windowSize], use first */
    /* JADX DEBUG: Different variable names in phi insn: [needUpdateUidBatteryUsageInWindow, windowSize], use first */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [621=4] */
    private void updateBatteryUsageStatsOnce(long now) {
        ArraySet<UserHandle> userIds;
        long curDuration;
        boolean needUpdateUidBatteryUsageInWindow;
        long lastUidBatteryUsageStartTs;
        BatteryUsageStatsQuery.Builder builder;
        long curDuration2;
        BatteryStatsInternal batteryStatsInternal;
        long needUpdateUidBatteryUsageInWindow2;
        long lastUidBatteryUsageStartTs2;
        boolean needUpdateUidBatteryUsageInWindow3;
        BatteryUsageStatsQuery.Builder builder2;
        int size;
        AppBatteryPolicy bgPolicy = (AppBatteryPolicy) this.mInjector.getPolicy();
        ArraySet<UserHandle> userIds2 = this.mTmpUserIds;
        SparseArray<BatteryUsage> buf = this.mTmpUidBatteryUsage;
        BatteryStatsInternal batteryStatsInternal2 = this.mInjector.getBatteryStatsInternal();
        long windowSize = bgPolicy.mBgCurrentDrainWindowMs;
        buf.clear();
        userIds2.clear();
        synchronized (this.mLock) {
            try {
                for (int i = this.mActiveUserIdStates.size() - 1; i >= 0; i--) {
                    try {
                        userIds2.add(UserHandle.of(this.mActiveUserIdStates.keyAt(i)));
                        if (!this.mActiveUserIdStates.valueAt(i)) {
                            this.mActiveUserIdStates.removeAt(i);
                        }
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
                BatteryUsageStatsQuery.Builder builder3 = new BatteryUsageStatsQuery.Builder().includeProcessStateData().setMaxStatsAgeMs(0L);
                BatteryUsageStats stats = updateBatteryUsageStatsOnceInternal(0L, buf, builder3, userIds2, batteryStatsInternal2);
                long builder4 = stats != null ? stats.getStatsStartTimestamp() : 0L;
                long curEnd = stats != null ? stats.getStatsEndTimestamp() : now;
                long curDuration3 = curEnd - builder4;
                if (curDuration3 >= windowSize) {
                    synchronized (this.mLock) {
                        try {
                            try {
                                userIds = userIds2;
                                curDuration = windowSize;
                                copyUidBatteryUsage(buf, this.mUidBatteryUsageInWindow, (windowSize * 1.0d) / curDuration3);
                                needUpdateUidBatteryUsageInWindow = false;
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            throw th;
                        }
                    }
                } else {
                    userIds = userIds2;
                    curDuration = windowSize;
                    needUpdateUidBatteryUsageInWindow = true;
                }
                this.mTmpUidBatteryUsage2.clear();
                copyUidBatteryUsage(buf, this.mTmpUidBatteryUsage2);
                synchronized (this.mLock) {
                    try {
                        lastUidBatteryUsageStartTs = this.mLastUidBatteryUsageStartTs;
                        this.mLastUidBatteryUsageStartTs = builder4;
                    } finally {
                        th = th;
                        long j = builder4;
                        boolean z = needUpdateUidBatteryUsageInWindow;
                        long j2 = curDuration;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                    }
                }
                if (builder4 <= lastUidBatteryUsageStartTs || lastUidBatteryUsageStartTs <= 0) {
                    builder = builder3;
                    curDuration2 = curDuration3;
                } else {
                    BatteryUsageStatsQuery.Builder builder5 = new BatteryUsageStatsQuery.Builder().includeProcessStateData().aggregateSnapshots(lastUidBatteryUsageStartTs, builder4);
                    long curStart = builder4;
                    updateBatteryUsageStatsOnceInternal(0L, buf, builder5, userIds, batteryStatsInternal2);
                    curDuration2 = curDuration3 + (curStart - lastUidBatteryUsageStartTs);
                    builder = builder5;
                }
                if (!needUpdateUidBatteryUsageInWindow || curDuration < curDuration) {
                    batteryStatsInternal = batteryStatsInternal2;
                    needUpdateUidBatteryUsageInWindow2 = curDuration;
                    lastUidBatteryUsageStartTs2 = lastUidBatteryUsageStartTs;
                    needUpdateUidBatteryUsageInWindow3 = needUpdateUidBatteryUsageInWindow;
                } else {
                    synchronized (this.mLock) {
                        try {
                            try {
                                needUpdateUidBatteryUsageInWindow2 = curDuration;
                                batteryStatsInternal = batteryStatsInternal2;
                                lastUidBatteryUsageStartTs2 = lastUidBatteryUsageStartTs;
                                copyUidBatteryUsage(buf, this.mUidBatteryUsageInWindow, (needUpdateUidBatteryUsageInWindow2 * 1.0d) / curDuration);
                                needUpdateUidBatteryUsageInWindow3 = false;
                            } catch (Throwable th6) {
                                th = th6;
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            throw th;
                        }
                    }
                }
                synchronized (this.mLock) {
                    int i2 = 0;
                    try {
                        int size2 = buf.size();
                        while (i2 < size2) {
                            try {
                                int uid = buf.keyAt(i2);
                                int index = this.mUidBatteryUsage.indexOfKey(uid);
                                BatteryUsage lastUsage = this.mLastUidBatteryUsage.get(uid, BATTERY_USAGE_NONE);
                                BatteryUsage curUsage = buf.valueAt(i2);
                                if (index >= 0) {
                                    builder2 = builder;
                                    try {
                                        BatteryUsage totalUsage = this.mUidBatteryUsage.valueAt(index);
                                        size = size2;
                                        totalUsage.subtract(lastUsage).add(curUsage);
                                    } catch (Throwable th8) {
                                        th = th8;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th9) {
                                                th = th9;
                                            }
                                        }
                                        throw th;
                                    }
                                } else {
                                    builder2 = builder;
                                    size = size2;
                                    this.mUidBatteryUsage.put(uid, curUsage);
                                }
                                i2++;
                                size2 = size;
                                builder = builder2;
                            } catch (Throwable th10) {
                                th = th10;
                            }
                        }
                        try {
                            copyUidBatteryUsage(this.mTmpUidBatteryUsage2, this.mLastUidBatteryUsage);
                            this.mTmpUidBatteryUsage2.clear();
                            if (needUpdateUidBatteryUsageInWindow3) {
                                long start = now - needUpdateUidBatteryUsageInWindow;
                                long end = lastUidBatteryUsageStartTs2 - 1;
                                updateBatteryUsageStatsOnceInternal(end - start, buf, new BatteryUsageStatsQuery.Builder().includeProcessStateData().aggregateSnapshots(start, end), userIds, batteryStatsInternal);
                                synchronized (this.mLock) {
                                    copyUidBatteryUsage(buf, this.mUidBatteryUsageInWindow);
                                }
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                    }
                }
            } catch (Throwable th13) {
                th = th13;
            }
        }
    }

    private BatteryUsageStats updateBatteryUsageStatsOnceInternal(long expectedDuration, SparseArray<BatteryUsage> buf, BatteryUsageStatsQuery.Builder builder, ArraySet<UserHandle> userIds, BatteryStatsInternal batteryStatsInternal) {
        int uid;
        int size = userIds.size();
        for (int i = 0; i < size; i++) {
            builder.addUser(userIds.valueAt(i));
        }
        List<BatteryUsageStats> statsList = batteryStatsInternal.getBatteryUsageStats(Arrays.asList(builder.build()));
        if (ArrayUtils.isEmpty(statsList)) {
            return null;
        }
        BatteryUsageStats stats = statsList.get(0);
        List<UidBatteryConsumer> uidConsumers = stats.getUidBatteryConsumers();
        if (uidConsumers != null) {
            long start = stats.getStatsStartTimestamp();
            long end = stats.getStatsEndTimestamp();
            double d = 1.0d;
            if (expectedDuration > 0) {
                d = Math.min((expectedDuration * 1.0d) / (end - start), 1.0d);
            }
            double scale = d;
            AppBatteryPolicy bgPolicy = (AppBatteryPolicy) this.mInjector.getPolicy();
            for (UidBatteryConsumer uidConsumer : uidConsumers) {
                int rawUid = uidConsumer.getUid();
                if (!UserHandle.isIsolated(rawUid)) {
                    int sharedAppId = UserHandle.getAppIdFromSharedAppGid(rawUid);
                    if (sharedAppId <= 0) {
                        uid = rawUid;
                    } else {
                        int uid2 = UserHandle.getUid(0, sharedAppId);
                        uid = uid2;
                    }
                    BatteryUsage bgUsage = new BatteryUsage(uidConsumer, bgPolicy).scale(scale);
                    double scale2 = scale;
                    int index = buf.indexOfKey(uid);
                    if (index < 0) {
                        buf.put(uid, bgUsage);
                    } else {
                        BatteryUsage before = buf.valueAt(index);
                        before.add(bgUsage);
                    }
                    scale = scale2;
                }
            }
        }
        return stats;
    }

    private static void copyUidBatteryUsage(SparseArray<? extends BatteryUsage> source, SparseArray<ImmutableBatteryUsage> dest) {
        dest.clear();
        for (int i = source.size() - 1; i >= 0; i--) {
            dest.put(source.keyAt(i), new ImmutableBatteryUsage(source.valueAt(i)));
        }
    }

    private static void copyUidBatteryUsage(SparseArray<? extends BatteryUsage> source, SparseArray<ImmutableBatteryUsage> dest, double scale) {
        dest.clear();
        for (int i = source.size() - 1; i >= 0; i--) {
            dest.put(source.keyAt(i), new ImmutableBatteryUsage(source.valueAt(i), scale));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCurrentDrainMonitorEnabled(boolean enabled) {
        if (enabled) {
            if (!this.mBgHandler.hasCallbacks(this.mBgBatteryUsageStatsPolling)) {
                this.mBgHandler.postDelayed(this.mBgBatteryUsageStatsPolling, this.mBatteryUsageStatsPollingIntervalMs);
                return;
            }
            return;
        }
        this.mBgHandler.removeCallbacks(this.mBgBatteryUsageStatsPolling);
        synchronized (this.mLock) {
            if (this.mBatteryUsageStatsUpdatePending) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                }
            }
            this.mUidBatteryUsage.clear();
            this.mUidBatteryUsageInWindow.clear();
            this.mLastUidBatteryUsage.clear();
            this.mLastBatteryUsageSamplingTs = 0L;
            this.mLastUidBatteryUsageStartTs = 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugUidPercentage(int[] uids, double[][] percentages) {
        this.mDebugUidPercentages.clear();
        for (int i = 0; i < uids.length; i++) {
            this.mDebugUidPercentages.put(uids[i], new BatteryUsage().setPercentage(percentages[i]).unmutate());
        }
        scheduleBgBatteryUsageStatsCheck();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearDebugUidPercentage() {
        this.mDebugUidPercentages.clear();
        scheduleBgBatteryUsageStatsCheck();
    }

    void reset() {
        synchronized (this.mLock) {
            this.mUidBatteryUsage.clear();
            this.mUidBatteryUsageInWindow.clear();
            this.mLastUidBatteryUsage.clear();
            this.mLastBatteryUsageSamplingTs = 0L;
            this.mLastUidBatteryUsageStartTs = 0L;
        }
        this.mBgHandler.removeCallbacks(this.mBgBatteryUsageStatsPolling);
        updateBatteryUsageStatsAndCheck();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP BATTERY STATE TRACKER:");
        updateBatteryUsageStatsIfNecessary(this.mInjector.currentTimeMillis(), true);
        scheduleBgBatteryUsageStatsCheck();
        final CountDownLatch latch = new CountDownLatch(1);
        this.mBgHandler.getLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() { // from class: com.android.server.am.AppBatteryTracker$$ExternalSyntheticLambda0
            @Override // android.os.MessageQueue.IdleHandler
            public final boolean queueIdle() {
                return AppBatteryTracker.lambda$dump$0(latch);
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
        synchronized (this.mLock) {
            SparseArray<ImmutableBatteryUsage> uidConsumers = this.mUidBatteryUsageInWindow;
            pw.print("  " + prefix);
            pw.print("  Last battery usage start=");
            TimeUtils.dumpTime(pw, this.mLastUidBatteryUsageStartTs);
            pw.println();
            pw.print("  " + prefix);
            pw.print("Battery usage over last ");
            String newPrefix = "    " + prefix;
            AppBatteryPolicy bgPolicy = (AppBatteryPolicy) this.mInjector.getPolicy();
            long now = SystemClock.elapsedRealtime();
            long since = Math.max(0L, now - bgPolicy.mBgCurrentDrainWindowMs);
            pw.println(TimeUtils.formatDuration(now - since));
            if (uidConsumers.size() == 0) {
                pw.print(newPrefix);
                pw.println("(none)");
            } else {
                int i = 0;
                for (int size = uidConsumers.size(); i < size; size = size) {
                    int uid = uidConsumers.keyAt(i);
                    BatteryUsage bgUsage = uidConsumers.valueAt(i).calcPercentage(uid, bgPolicy);
                    BatteryUsage exemptedUsage = this.mAppRestrictionController.getUidBatteryExemptedUsageSince(uid, since, now, bgPolicy.mBgCurrentDrainExemptedTypes).calcPercentage(uid, bgPolicy);
                    BatteryUsage reportedUsage = new BatteryUsage(bgUsage).subtract(exemptedUsage).calcPercentage(uid, bgPolicy);
                    pw.format("%s%s: [%s] %s (%s) | %s (%s) | %s (%s) | %s\n", newPrefix, UserHandle.formatUid(uid), PowerExemptionManager.reasonCodeToString(bgPolicy.shouldExemptUid(uid)), bgUsage.toString(), bgUsage.percentageToString(), exemptedUsage.toString(), exemptedUsage.percentageToString(), reportedUsage.toString(), reportedUsage.percentageToString(), this.mUidBatteryUsage.get(uid, BATTERY_USAGE_NONE).toString());
                    i++;
                    uidConsumers = uidConsumers;
                }
            }
        }
        super.dump(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$dump$0(CountDownLatch latch) {
        latch.countDown();
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void dumpAsProto(ProtoOutputStream proto, int uid) {
        updateBatteryUsageStatsIfNecessary(this.mInjector.currentTimeMillis(), true);
        synchronized (this.mLock) {
            SparseArray<ImmutableBatteryUsage> uidConsumers = this.mUidBatteryUsageInWindow;
            if (uid != -1) {
                BatteryUsage usage = uidConsumers.get(uid);
                if (usage != null) {
                    dumpUidStats(proto, uid, usage);
                }
            } else {
                int size = uidConsumers.size();
                for (int i = 0; i < size; i++) {
                    int aUid = uidConsumers.keyAt(i);
                    dumpUidStats(proto, aUid, uidConsumers.valueAt(i));
                }
            }
        }
    }

    private void dumpUidStats(ProtoOutputStream proto, int uid, BatteryUsage usage) {
        if (usage.mUsage == null) {
            return;
        }
        double foregroundUsage = usage.getUsagePowerMah(1);
        double backgroundUsage = usage.getUsagePowerMah(2);
        double fgsUsage = usage.getUsagePowerMah(3);
        double cachedUsage = usage.getUsagePowerMah(4);
        if (foregroundUsage == 0.0d && backgroundUsage == 0.0d && fgsUsage == 0.0d) {
            return;
        }
        long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        proto.write(CompanionMessage.MESSAGE_ID, uid);
        dumpProcessStateStats(proto, 1, foregroundUsage);
        dumpProcessStateStats(proto, 2, backgroundUsage);
        dumpProcessStateStats(proto, 3, fgsUsage);
        dumpProcessStateStats(proto, 4, cachedUsage);
        proto.end(token);
    }

    private void dumpProcessStateStats(ProtoOutputStream proto, int processState, double powerMah) {
        if (powerMah == 0.0d) {
            return;
        }
        long token = proto.start(2246267895810L);
        proto.write(1159641169921L, processState);
        proto.write(1103806595075L, powerMah);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BatteryUsage {
        static final int BATTERY_USAGE_COUNT = 5;
        static final int BATTERY_USAGE_INDEX_BACKGROUND = 2;
        static final int BATTERY_USAGE_INDEX_CACHED = 4;
        static final int BATTERY_USAGE_INDEX_FOREGROUND = 1;
        static final int BATTERY_USAGE_INDEX_FOREGROUND_SERVICE = 3;
        static final int BATTERY_USAGE_INDEX_UNSPECIFIED = 0;
        static final BatteryConsumer.Dimensions[] BATT_DIMENS = {new BatteryConsumer.Dimensions(-1, 0), new BatteryConsumer.Dimensions(-1, 1), new BatteryConsumer.Dimensions(-1, 2), new BatteryConsumer.Dimensions(-1, 3), new BatteryConsumer.Dimensions(-1, 4)};
        double[] mPercentage;
        double[] mUsage;

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage() {
            this(0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
        }

        BatteryUsage(double unspecifiedUsage, double fgUsage, double bgUsage, double fgsUsage, double cachedUsage) {
            this.mUsage = new double[]{unspecifiedUsage, fgUsage, bgUsage, fgsUsage, cachedUsage};
        }

        BatteryUsage(double[] usage) {
            this.mUsage = usage;
        }

        BatteryUsage(BatteryUsage other, double scale) {
            this(other);
            scaleInternal(scale);
        }

        BatteryUsage(BatteryUsage other) {
            this.mUsage = new double[other.mUsage.length];
            setToInternal(other);
        }

        BatteryUsage(UidBatteryConsumer consumer, AppBatteryPolicy policy) {
            BatteryConsumer.Dimensions[] dims = policy.mBatteryDimensions;
            this.mUsage = new double[]{getConsumedPowerNoThrow(consumer, dims[0]), getConsumedPowerNoThrow(consumer, dims[1]), getConsumedPowerNoThrow(consumer, dims[2]), getConsumedPowerNoThrow(consumer, dims[3]), getConsumedPowerNoThrow(consumer, dims[4])};
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage setTo(BatteryUsage other) {
            return setToInternal(other);
        }

        private BatteryUsage setToInternal(BatteryUsage other) {
            double[] dArr = other.mUsage;
            System.arraycopy(dArr, 0, this.mUsage, 0, dArr.length);
            double[] dArr2 = other.mPercentage;
            if (dArr2 != null) {
                double[] dArr3 = new double[dArr2.length];
                this.mPercentage = dArr3;
                double[] dArr4 = other.mPercentage;
                System.arraycopy(dArr4, 0, dArr3, 0, dArr4.length);
            } else {
                this.mPercentage = null;
            }
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage add(BatteryUsage other) {
            int i = 0;
            while (true) {
                double[] dArr = other.mUsage;
                if (i < dArr.length) {
                    double[] dArr2 = this.mUsage;
                    dArr2[i] = dArr2[i] + dArr[i];
                    i++;
                } else {
                    return this;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage subtract(BatteryUsage other) {
            int i = 0;
            while (true) {
                double[] dArr = other.mUsage;
                if (i < dArr.length) {
                    double[] dArr2 = this.mUsage;
                    dArr2[i] = Math.max(0.0d, dArr2[i] - dArr[i]);
                    i++;
                } else {
                    return this;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage scale(double scale) {
            return scaleInternal(scale);
        }

        private BatteryUsage scaleInternal(double scale) {
            int i = 0;
            while (true) {
                double[] dArr = this.mUsage;
                if (i < dArr.length) {
                    dArr[i] = dArr[i] * scale;
                    i++;
                } else {
                    return this;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ImmutableBatteryUsage unmutate() {
            return new ImmutableBatteryUsage(this);
        }

        BatteryUsage calcPercentage(int uid, AppBatteryPolicy policy) {
            double[] dArr = this.mPercentage;
            if (dArr == null || dArr.length != this.mUsage.length) {
                this.mPercentage = new double[this.mUsage.length];
            }
            policy.calcPercentage(uid, this.mUsage, this.mPercentage);
            return this;
        }

        BatteryUsage setPercentage(double[] percentage) {
            this.mPercentage = percentage;
            return this;
        }

        double[] getPercentage() {
            return this.mPercentage;
        }

        String percentageToString() {
            return formatBatteryUsagePercentage(this.mPercentage);
        }

        public String toString() {
            return formatBatteryUsage(this.mUsage);
        }

        double getUsagePowerMah(int processState) {
            switch (processState) {
                case 1:
                    return this.mUsage[1];
                case 2:
                    return this.mUsage[2];
                case 3:
                    return this.mUsage[3];
                case 4:
                    return this.mUsage[4];
                default:
                    return 0.0d;
            }
        }

        boolean isValid() {
            int i = 0;
            while (true) {
                double[] dArr = this.mUsage;
                if (i < dArr.length) {
                    if (dArr[i] >= 0.0d) {
                        i++;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isEmpty() {
            int i = 0;
            while (true) {
                double[] dArr = this.mUsage;
                if (i < dArr.length) {
                    if (dArr[i] <= 0.0d) {
                        i++;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            BatteryUsage otherUsage = (BatteryUsage) other;
            int i = 0;
            while (true) {
                double[] dArr = this.mUsage;
                if (i < dArr.length) {
                    if (Double.compare(dArr[i], otherUsage.mUsage[i]) != 0) {
                        return false;
                    }
                    i++;
                } else {
                    return true;
                }
            }
        }

        public int hashCode() {
            int hashCode = 0;
            int i = 0;
            while (true) {
                double[] dArr = this.mUsage;
                if (i < dArr.length) {
                    hashCode = Double.hashCode(dArr[i]) + (hashCode * 31);
                    i++;
                } else {
                    return hashCode;
                }
            }
        }

        private static String formatBatteryUsage(double[] usage) {
            return String.format("%.3f %.3f %.3f %.3f %.3f mAh", Double.valueOf(usage[0]), Double.valueOf(usage[1]), Double.valueOf(usage[2]), Double.valueOf(usage[3]), Double.valueOf(usage[4]));
        }

        static String formatBatteryUsagePercentage(double[] percentage) {
            return String.format("%4.2f%% %4.2f%% %4.2f%% %4.2f%% %4.2f%%", Double.valueOf(percentage[0]), Double.valueOf(percentage[1]), Double.valueOf(percentage[2]), Double.valueOf(percentage[3]), Double.valueOf(percentage[4]));
        }

        private static double getConsumedPowerNoThrow(UidBatteryConsumer uidConsumer, BatteryConsumer.Dimensions dimens) {
            try {
                return uidConsumer.getConsumedPower(dimens);
            } catch (IllegalArgumentException e) {
                return 0.0d;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ImmutableBatteryUsage extends BatteryUsage {
        ImmutableBatteryUsage() {
        }

        ImmutableBatteryUsage(double unspecifiedUsage, double fgUsage, double bgUsage, double fgsUsage, double cachedUsage) {
            super(unspecifiedUsage, fgUsage, bgUsage, fgsUsage, cachedUsage);
        }

        ImmutableBatteryUsage(double[] usage) {
            super(usage);
        }

        ImmutableBatteryUsage(BatteryUsage other, double scale) {
            super(other, scale);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ImmutableBatteryUsage(BatteryUsage other) {
            super(other);
        }

        ImmutableBatteryUsage(UidBatteryConsumer consumer, AppBatteryPolicy policy) {
            super(consumer, policy);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.AppBatteryTracker.BatteryUsage
        public BatteryUsage setTo(BatteryUsage other) {
            throw new RuntimeException("Readonly");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.AppBatteryTracker.BatteryUsage
        public BatteryUsage add(BatteryUsage other) {
            throw new RuntimeException("Readonly");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.AppBatteryTracker.BatteryUsage
        public BatteryUsage subtract(BatteryUsage other) {
            throw new RuntimeException("Readonly");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.AppBatteryTracker.BatteryUsage
        public BatteryUsage scale(double scale) {
            throw new RuntimeException("Readonly");
        }

        @Override // com.android.server.am.AppBatteryTracker.BatteryUsage
        BatteryUsage setPercentage(double[] percentage) {
            throw new RuntimeException("Readonly");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryUsage mutate() {
            return new BatteryUsage(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppBatteryPolicy extends BaseAppStatePolicy<AppBatteryTracker> {
        static final int BATTERY_USAGE_TYPE_BACKGROUND = 4;
        static final int BATTERY_USAGE_TYPE_CACHED = 16;
        static final int BATTERY_USAGE_TYPE_FOREGROUND = 2;
        static final int BATTERY_USAGE_TYPE_FOREGROUND_SERVICE = 8;
        static final int BATTERY_USAGE_TYPE_UNSPECIFIED = 1;
        static final boolean DEFAULT_BG_CURRENT_DRAIN_DECOUPLE_THRESHOLD = true;
        static final int DEFAULT_BG_CURRENT_DRAIN_POWER_COMPONENTS = -1;
        static final int INDEX_HIGH_CURRENT_DRAIN_THRESHOLD = 1;
        static final int INDEX_REGULAR_CURRENT_DRAIN_THRESHOLD = 0;
        static final String KEY_BG_CURRENT_DRAIN_AUTO_RESTRICT_ABUSIVE_APPS_ENABLED = "bg_current_drain_auto_restrict_abusive_apps_enabled";
        static final String KEY_BG_CURRENT_DRAIN_DECOUPLE_THRESHOLDS = "bg_current_drain_decouple_thresholds";
        static final String KEY_BG_CURRENT_DRAIN_EVENT_DURATION_BASED_THRESHOLD_ENABLED = "bg_current_drain_event_duration_based_threshold_enabled";
        static final String KEY_BG_CURRENT_DRAIN_EXEMPTED_TYPES = "bg_current_drain_exempted_types";
        static final String KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_BY_BG_LOCATION = "bg_current_drain_high_threshold_by_bg_location";
        static final String KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_BG_RESTRICTED = "bg_current_drain_high_threshold_to_bg_restricted";
        static final String KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_RESTRICTED_BUCKET = "bg_current_drain_high_threshold_to_restricted_bucket";
        static final String KEY_BG_CURRENT_DRAIN_INTERACTION_GRACE_PERIOD = "bg_current_drain_interaction_grace_period";
        static final String KEY_BG_CURRENT_DRAIN_LOCATION_MIN_DURATION = "bg_current_drain_location_min_duration";
        static final String KEY_BG_CURRENT_DRAIN_MEDIA_PLAYBACK_MIN_DURATION = "bg_current_drain_media_playback_min_duration";
        static final String KEY_BG_CURRENT_DRAIN_MONITOR_ENABLED = "bg_current_drain_monitor_enabled";
        static final String KEY_BG_CURRENT_DRAIN_POWER_COMPONENTS = "bg_current_drain_power_components";
        static final String KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_BG_RESTRICTED = "bg_current_drain_threshold_to_bg_restricted";
        static final String KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_RESTRICTED_BUCKET = "bg_current_drain_threshold_to_restricted_bucket";
        static final String KEY_BG_CURRENT_DRAIN_TYPES_TO_BG_RESTRICTED = "bg_current_drain_types_to_bg_restricted";
        static final String KEY_BG_CURRENT_DRAIN_TYPES_TO_RESTRICTED_BUCKET = "bg_current_drain_types_to_restricted_bucket";
        static final String KEY_BG_CURRENT_DRAIN_WINDOW = "bg_current_drain_window";
        private static final int TIME_STAMP_INDEX_BG_RESTRICTED = 1;
        private static final int TIME_STAMP_INDEX_LAST = 2;
        private static final int TIME_STAMP_INDEX_RESTRICTED_BUCKET = 0;
        volatile BatteryConsumer.Dimensions[] mBatteryDimensions;
        private int mBatteryFullChargeMah;
        volatile boolean mBgCurrentDrainAutoRestrictAbusiveAppsEnabled;
        volatile float[] mBgCurrentDrainBgRestrictedThreshold;
        volatile int mBgCurrentDrainBgRestrictedTypes;
        volatile boolean mBgCurrentDrainDecoupleThresholds;
        volatile boolean mBgCurrentDrainEventDurationBasedThresholdEnabled;
        volatile int mBgCurrentDrainExemptedTypes;
        volatile boolean mBgCurrentDrainHighThresholdByBgLocation;
        volatile long mBgCurrentDrainInteractionGracePeriodMs;
        volatile long mBgCurrentDrainLocationMinDuration;
        volatile long mBgCurrentDrainMediaPlaybackMinDuration;
        volatile int mBgCurrentDrainPowerComponents;
        volatile float[] mBgCurrentDrainRestrictedBucketThreshold;
        volatile int mBgCurrentDrainRestrictedBucketTypes;
        volatile long mBgCurrentDrainWindowMs;
        final boolean mDefaultBgCurrentDrainAutoRestrictAbusiveAppsEnabled;
        final float mDefaultBgCurrentDrainBgRestrictedHighThreshold;
        final float mDefaultBgCurrentDrainBgRestrictedThreshold;
        final boolean mDefaultBgCurrentDrainEventDurationBasedThresholdEnabled;
        final int mDefaultBgCurrentDrainExemptedTypes;
        final boolean mDefaultBgCurrentDrainHighThresholdByBgLocation;
        final long mDefaultBgCurrentDrainInteractionGracePeriodMs;
        final long mDefaultBgCurrentDrainLocationMinDuration;
        final long mDefaultBgCurrentDrainMediaPlaybackMinDuration;
        final int mDefaultBgCurrentDrainPowerComponent;
        final float mDefaultBgCurrentDrainRestrictedBucket;
        final float mDefaultBgCurrentDrainRestrictedBucketHighThreshold;
        final int mDefaultBgCurrentDrainTypesToBgRestricted;
        final long mDefaultBgCurrentDrainWindowMs;
        final int mDefaultCurrentDrainTypesToRestrictedBucket;
        private final SparseArray<Pair<long[], ImmutableBatteryUsage[]>> mHighBgBatteryPackages;
        private final SparseLongArray mLastInteractionTime;
        private final Object mLock;

        AppBatteryPolicy(BaseAppStateTracker.Injector injector, AppBatteryTracker tracker) {
            super(injector, tracker, KEY_BG_CURRENT_DRAIN_MONITOR_ENABLED, tracker.mContext.getResources().getBoolean(17891389));
            this.mBgCurrentDrainRestrictedBucketThreshold = new float[2];
            this.mBgCurrentDrainBgRestrictedThreshold = new float[2];
            this.mHighBgBatteryPackages = new SparseArray<>();
            this.mLastInteractionTime = new SparseLongArray();
            this.mLock = tracker.mLock;
            Resources resources = tracker.mContext.getResources();
            float[] val = getFloatArray(resources.obtainTypedArray(17236003));
            float f = ActivityManager.isLowRamDeviceStatic() ? val[1] : val[0];
            this.mDefaultBgCurrentDrainRestrictedBucket = f;
            float[] val2 = getFloatArray(resources.obtainTypedArray(17236002));
            float f2 = ActivityManager.isLowRamDeviceStatic() ? val2[1] : val2[0];
            this.mDefaultBgCurrentDrainBgRestrictedThreshold = f2;
            long integer = resources.getInteger(17694752) * 1000;
            this.mDefaultBgCurrentDrainWindowMs = integer;
            this.mDefaultBgCurrentDrainInteractionGracePeriodMs = integer;
            float[] val3 = getFloatArray(resources.obtainTypedArray(17236001));
            float f3 = ActivityManager.isLowRamDeviceStatic() ? val3[1] : val3[0];
            this.mDefaultBgCurrentDrainRestrictedBucketHighThreshold = f3;
            float[] val4 = getFloatArray(resources.obtainTypedArray(17236000));
            float f4 = ActivityManager.isLowRamDeviceStatic() ? val4[1] : val4[0];
            this.mDefaultBgCurrentDrainBgRestrictedHighThreshold = f4;
            long integer2 = resources.getInteger(17694748) * 1000;
            this.mDefaultBgCurrentDrainMediaPlaybackMinDuration = integer2;
            long integer3 = resources.getInteger(17694747) * 1000;
            this.mDefaultBgCurrentDrainLocationMinDuration = integer3;
            this.mDefaultBgCurrentDrainEventDurationBasedThresholdEnabled = resources.getBoolean(17891387);
            this.mDefaultBgCurrentDrainAutoRestrictAbusiveAppsEnabled = resources.getBoolean(17891386);
            this.mDefaultCurrentDrainTypesToRestrictedBucket = resources.getInteger(17694751);
            this.mDefaultBgCurrentDrainTypesToBgRestricted = resources.getInteger(17694750);
            this.mDefaultBgCurrentDrainPowerComponent = resources.getInteger(17694749);
            this.mDefaultBgCurrentDrainExemptedTypes = resources.getInteger(17694746);
            this.mDefaultBgCurrentDrainHighThresholdByBgLocation = resources.getBoolean(17891388);
            this.mBgCurrentDrainRestrictedBucketThreshold[0] = f;
            this.mBgCurrentDrainRestrictedBucketThreshold[1] = f3;
            this.mBgCurrentDrainBgRestrictedThreshold[0] = f2;
            this.mBgCurrentDrainBgRestrictedThreshold[1] = f4;
            this.mBgCurrentDrainWindowMs = integer;
            this.mBgCurrentDrainInteractionGracePeriodMs = integer;
            this.mBgCurrentDrainMediaPlaybackMinDuration = integer2;
            this.mBgCurrentDrainLocationMinDuration = integer3;
        }

        static float[] getFloatArray(TypedArray array) {
            int length = array.length();
            float[] floatArray = new float[length];
            for (int i = 0; i < length; i++) {
                floatArray[i] = array.getFloat(i, Float.NaN);
            }
            array.recycle();
            return floatArray;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.am.BaseAppStatePolicy
        public void onPropertiesChanged(String name) {
            char c;
            switch (name.hashCode()) {
                case -1969771998:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_EVENT_DURATION_BASED_THRESHOLD_ENABLED)) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case -1881058465:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_DECOUPLE_THRESHOLDS)) {
                        c = 15;
                        break;
                    }
                    c = 65535;
                    break;
                case -531697693:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_MEDIA_PLAYBACK_MIN_DURATION)) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -523630921:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_POWER_COMPONENTS)) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -494951532:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_RESTRICTED_BUCKET)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 50590052:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_LOCATION_MIN_DURATION)) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case 101017819:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_BG_RESTRICTED)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 129921652:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_AUTO_RESTRICT_ABUSIVE_APPS_ENABLED)) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 399258641:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_BY_BG_LOCATION)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 517972572:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_INTERACTION_GRACE_PERIOD)) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case 655159543:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_TYPES_TO_RESTRICTED_BUCKET)) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 718752671:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_EXEMPTED_TYPES)) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case 1136582590:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_TYPES_TO_BG_RESTRICTED)) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1362995852:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_BG_RESTRICTED)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1869456581:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_RESTRICTED_BUCKET)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1961864407:
                    if (name.equals(KEY_BG_CURRENT_DRAIN_WINDOW)) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    updateCurrentDrainThreshold();
                    return;
                case '\b':
                    updateBgCurrentDrainAutoRestrictAbusiveAppsEnabled();
                    return;
                case '\t':
                    updateCurrentDrainWindow();
                    return;
                case '\n':
                    updateCurrentDrainInteractionGracePeriod();
                    return;
                case 11:
                    updateCurrentDrainMediaPlaybackMinDuration();
                    return;
                case '\f':
                    updateCurrentDrainLocationMinDuration();
                    return;
                case '\r':
                    updateCurrentDrainEventDurationBasedThresholdEnabled();
                    return;
                case 14:
                    updateCurrentDrainExemptedTypes();
                    return;
                case 15:
                    updateCurrentDrainDecoupleThresholds();
                    return;
                default:
                    super.onPropertiesChanged(name);
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStatePolicy
        public void updateTrackerEnabled() {
            if (this.mBatteryFullChargeMah > 0) {
                super.updateTrackerEnabled();
                return;
            }
            this.mTrackerEnabled = false;
            onTrackerEnabled(false);
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((AppBatteryTracker) this.mTracker).onCurrentDrainMonitorEnabled(enabled);
        }

        private void updateCurrentDrainThreshold() {
            this.mBgCurrentDrainRestrictedBucketThreshold[0] = DeviceConfig.getFloat("activity_manager", KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_RESTRICTED_BUCKET, this.mDefaultBgCurrentDrainRestrictedBucket);
            this.mBgCurrentDrainRestrictedBucketThreshold[1] = DeviceConfig.getFloat("activity_manager", KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_RESTRICTED_BUCKET, this.mDefaultBgCurrentDrainRestrictedBucketHighThreshold);
            this.mBgCurrentDrainBgRestrictedThreshold[0] = DeviceConfig.getFloat("activity_manager", KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_BG_RESTRICTED, this.mDefaultBgCurrentDrainBgRestrictedThreshold);
            this.mBgCurrentDrainBgRestrictedThreshold[1] = DeviceConfig.getFloat("activity_manager", KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_BG_RESTRICTED, this.mDefaultBgCurrentDrainBgRestrictedHighThreshold);
            this.mBgCurrentDrainRestrictedBucketTypes = DeviceConfig.getInt("activity_manager", KEY_BG_CURRENT_DRAIN_TYPES_TO_RESTRICTED_BUCKET, this.mDefaultCurrentDrainTypesToRestrictedBucket);
            this.mBgCurrentDrainBgRestrictedTypes = DeviceConfig.getInt("activity_manager", KEY_BG_CURRENT_DRAIN_TYPES_TO_BG_RESTRICTED, this.mDefaultBgCurrentDrainTypesToBgRestricted);
            this.mBgCurrentDrainPowerComponents = DeviceConfig.getInt("activity_manager", KEY_BG_CURRENT_DRAIN_POWER_COMPONENTS, this.mDefaultBgCurrentDrainPowerComponent);
            if (this.mBgCurrentDrainPowerComponents == -1) {
                this.mBatteryDimensions = BatteryUsage.BATT_DIMENS;
            } else {
                this.mBatteryDimensions = new BatteryConsumer.Dimensions[5];
                for (int i = 0; i < 5; i++) {
                    this.mBatteryDimensions[i] = new BatteryConsumer.Dimensions(this.mBgCurrentDrainPowerComponents, i);
                }
            }
            this.mBgCurrentDrainHighThresholdByBgLocation = DeviceConfig.getBoolean("activity_manager", KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_BY_BG_LOCATION, this.mDefaultBgCurrentDrainHighThresholdByBgLocation);
        }

        private void updateCurrentDrainWindow() {
            this.mBgCurrentDrainWindowMs = DeviceConfig.getLong("activity_manager", KEY_BG_CURRENT_DRAIN_WINDOW, this.mDefaultBgCurrentDrainWindowMs);
        }

        private void updateCurrentDrainInteractionGracePeriod() {
            this.mBgCurrentDrainInteractionGracePeriodMs = DeviceConfig.getLong("activity_manager", KEY_BG_CURRENT_DRAIN_INTERACTION_GRACE_PERIOD, this.mDefaultBgCurrentDrainInteractionGracePeriodMs);
        }

        private void updateCurrentDrainMediaPlaybackMinDuration() {
            this.mBgCurrentDrainMediaPlaybackMinDuration = DeviceConfig.getLong("activity_manager", KEY_BG_CURRENT_DRAIN_MEDIA_PLAYBACK_MIN_DURATION, this.mDefaultBgCurrentDrainMediaPlaybackMinDuration);
        }

        private void updateCurrentDrainLocationMinDuration() {
            this.mBgCurrentDrainLocationMinDuration = DeviceConfig.getLong("activity_manager", KEY_BG_CURRENT_DRAIN_LOCATION_MIN_DURATION, this.mDefaultBgCurrentDrainLocationMinDuration);
        }

        private void updateCurrentDrainEventDurationBasedThresholdEnabled() {
            this.mBgCurrentDrainEventDurationBasedThresholdEnabled = DeviceConfig.getBoolean("activity_manager", KEY_BG_CURRENT_DRAIN_EVENT_DURATION_BASED_THRESHOLD_ENABLED, this.mDefaultBgCurrentDrainEventDurationBasedThresholdEnabled);
        }

        private void updateCurrentDrainExemptedTypes() {
            this.mBgCurrentDrainExemptedTypes = DeviceConfig.getInt("activity_manager", KEY_BG_CURRENT_DRAIN_EXEMPTED_TYPES, this.mDefaultBgCurrentDrainExemptedTypes);
        }

        private void updateCurrentDrainDecoupleThresholds() {
            this.mBgCurrentDrainDecoupleThresholds = DeviceConfig.getBoolean("activity_manager", KEY_BG_CURRENT_DRAIN_DECOUPLE_THRESHOLDS, true);
        }

        private void updateBgCurrentDrainAutoRestrictAbusiveAppsEnabled() {
            this.mBgCurrentDrainAutoRestrictAbusiveAppsEnabled = DeviceConfig.getBoolean("activity_manager", KEY_BG_CURRENT_DRAIN_AUTO_RESTRICT_ABUSIVE_APPS_ENABLED, this.mDefaultBgCurrentDrainAutoRestrictAbusiveAppsEnabled);
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onSystemReady() {
            this.mBatteryFullChargeMah = this.mInjector.getBatteryManagerInternal().getBatteryFullCharge() / 1000;
            super.onSystemReady();
            updateCurrentDrainThreshold();
            updateCurrentDrainWindow();
            updateCurrentDrainInteractionGracePeriod();
            updateCurrentDrainMediaPlaybackMinDuration();
            updateCurrentDrainLocationMinDuration();
            updateCurrentDrainEventDurationBasedThresholdEnabled();
            updateCurrentDrainExemptedTypes();
            updateCurrentDrainDecoupleThresholds();
            updateBgCurrentDrainAutoRestrictAbusiveAppsEnabled();
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public int getProposedRestrictionLevel(String packageName, int uid, int maxLevel) {
            int restrictedLevel;
            boolean canRestrict = false;
            if (maxLevel <= 30) {
                return 0;
            }
            synchronized (this.mLock) {
                Pair<long[], ImmutableBatteryUsage[]> pair = this.mHighBgBatteryPackages.get(uid);
                if (pair != null) {
                    long lastInteractionTime = this.mLastInteractionTime.get(uid, 0L);
                    long[] ts = (long[]) pair.first;
                    boolean noInteractionRecently = ts[0] > this.mBgCurrentDrainInteractionGracePeriodMs + lastInteractionTime;
                    if (((AppBatteryTracker) this.mTracker).mAppRestrictionController.isAutoRestrictAbusiveAppEnabled() && this.mBgCurrentDrainAutoRestrictAbusiveAppsEnabled) {
                        canRestrict = true;
                    }
                    if (noInteractionRecently && canRestrict) {
                        restrictedLevel = 40;
                    } else {
                        restrictedLevel = 30;
                    }
                    int i = 50;
                    if (maxLevel > 50) {
                        if (ts[1] <= 0) {
                            i = restrictedLevel;
                        }
                        return i;
                    } else if (maxLevel == 50) {
                        return restrictedLevel;
                    }
                }
                return 30;
            }
        }

        double[] calcPercentage(int uid, double[] usage, double[] percentage) {
            BatteryUsage debugUsage = uid > 0 ? (BatteryUsage) ((AppBatteryTracker) this.mTracker).mDebugUidPercentages.get(uid) : null;
            double[] forced = debugUsage != null ? debugUsage.getPercentage() : null;
            for (int i = 0; i < usage.length; i++) {
                percentage[i] = forced != null ? forced[i] : (usage[i] / this.mBatteryFullChargeMah) * 100.0d;
            }
            return percentage;
        }

        private double sumPercentageOfTypes(double[] percentage, int types) {
            double result = 0.0d;
            int type = Integer.highestOneBit(types);
            while (type != 0) {
                int index = Integer.numberOfTrailingZeros(type);
                result += percentage[index];
                types &= ~type;
                type = Integer.highestOneBit(types);
            }
            return result;
        }

        private static String batteryUsageTypesToString(int types) {
            StringBuilder sb = new StringBuilder("[");
            boolean needDelimiter = false;
            int type = Integer.highestOneBit(types);
            while (type != 0) {
                if (needDelimiter) {
                    sb.append('|');
                }
                needDelimiter = true;
                switch (type) {
                    case 1:
                        sb.append("UNSPECIFIED");
                        break;
                    case 2:
                        sb.append("FOREGROUND");
                        break;
                    case 4:
                        sb.append("BACKGROUND");
                        break;
                    case 8:
                        sb.append("FOREGROUND_SERVICE");
                        break;
                    case 16:
                        sb.append("CACHED");
                        break;
                    default:
                        return "[UNKNOWN(" + Integer.toHexString(types) + ")]";
                }
                types &= ~type;
                type = Integer.highestOneBit(types);
            }
            sb.append("]");
            return sb.toString();
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1938=7] */
        /* JADX WARN: Not initialized variable reg: 28, insn: 0x0173: MOVE  (r10 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r28 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('notifyController' boolean)]), block:B:87:0x0173 */
        void handleUidBatteryUsage(int uid, ImmutableBatteryUsage usage) {
            Object obj;
            boolean notifyController;
            boolean decoupleThresholds;
            boolean notifyController2;
            int reason = shouldExemptUid(uid);
            if (reason != -1) {
                return;
            }
            boolean notifyController3 = false;
            boolean excessive = false;
            double rbPercentage = sumPercentageOfTypes(usage.getPercentage(), this.mBgCurrentDrainRestrictedBucketTypes);
            double brPercentage = sumPercentageOfTypes(usage.getPercentage(), this.mBgCurrentDrainBgRestrictedTypes);
            Object obj2 = this.mLock;
            synchronized (obj2) {
                try {
                    try {
                        int curLevel = ((AppBatteryTracker) this.mTracker).mAppRestrictionController.getRestrictionLevel(uid);
                        if (curLevel >= 50) {
                            try {
                            } catch (Throwable th) {
                                th = th;
                                obj = obj2;
                            }
                        } else {
                            long lastInteractionTime = this.mLastInteractionTime.get(uid, 0L);
                            long now = SystemClock.elapsedRealtime();
                            obj = obj2;
                            try {
                                int thresholdIndex = getCurrentDrainThresholdIndex(uid, now, this.mBgCurrentDrainWindowMs);
                                int index = this.mHighBgBatteryPackages.indexOfKey(uid);
                                boolean decoupleThresholds2 = this.mBgCurrentDrainDecoupleThresholds;
                                double rbThreshold = this.mBgCurrentDrainRestrictedBucketThreshold[thresholdIndex];
                                double brThreshold = this.mBgCurrentDrainBgRestrictedThreshold[thresholdIndex];
                                boolean z = false;
                                try {
                                    if (index < 0) {
                                        long[] ts = null;
                                        ImmutableBatteryUsage[] usages = null;
                                        if (rbPercentage >= rbThreshold) {
                                            decoupleThresholds = decoupleThresholds2;
                                            if (now > lastInteractionTime + this.mBgCurrentDrainInteractionGracePeriodMs) {
                                                long[] ts2 = {now};
                                                try {
                                                    ImmutableBatteryUsage[] usages2 = new ImmutableBatteryUsage[2];
                                                    usages2[0] = usage;
                                                    this.mHighBgBatteryPackages.put(uid, Pair.create(ts2, usages2));
                                                    usages = usages2;
                                                    ts = ts2;
                                                    notifyController3 = true;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                }
                                            }
                                            excessive = true;
                                        } else {
                                            decoupleThresholds = decoupleThresholds2;
                                        }
                                        if (!decoupleThresholds || brPercentage < brThreshold) {
                                            notifyController = notifyController3;
                                        } else {
                                            if (ts == null) {
                                                try {
                                                    long[] ts3 = new long[2];
                                                    ImmutableBatteryUsage[] usages3 = new ImmutableBatteryUsage[2];
                                                    notifyController2 = notifyController3;
                                                    try {
                                                        this.mHighBgBatteryPackages.put(uid, Pair.create(ts3, usages3));
                                                        usages = usages3;
                                                        ts = ts3;
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                    }
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                }
                                            } else {
                                                notifyController2 = notifyController3;
                                            }
                                            ts[1] = now;
                                            usages[1] = usage;
                                            excessive = true;
                                            notifyController = true;
                                        }
                                    } else {
                                        boolean notifyController4 = false;
                                        Pair<long[], ImmutableBatteryUsage[]> pair = this.mHighBgBatteryPackages.valueAt(index);
                                        long[] ts4 = (long[]) pair.first;
                                        long lastRestrictBucketTs = ts4[0];
                                        if (rbPercentage >= rbThreshold) {
                                            if (now > lastInteractionTime + this.mBgCurrentDrainInteractionGracePeriodMs) {
                                                if (lastRestrictBucketTs == 0) {
                                                    ts4[0] = now;
                                                    ((ImmutableBatteryUsage[]) pair.second)[0] = usage;
                                                }
                                                notifyController4 = true;
                                            }
                                            excessive = true;
                                        }
                                        if (brPercentage >= brThreshold) {
                                            if (decoupleThresholds2 || (curLevel == 40 && now > this.mBgCurrentDrainWindowMs + lastRestrictBucketTs)) {
                                                z = true;
                                            }
                                            boolean notifyController5 = z;
                                            if (notifyController5) {
                                                try {
                                                    ts4[1] = now;
                                                    ((ImmutableBatteryUsage[]) pair.second)[1] = usage;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                }
                                            }
                                            notifyController = notifyController5;
                                            excessive = true;
                                        } else {
                                            ts4[1] = 0;
                                            ((ImmutableBatteryUsage[]) pair.second)[1] = null;
                                            notifyController = notifyController4;
                                        }
                                    }
                                    if (excessive && notifyController) {
                                        ((AppBatteryTracker) this.mTracker).mAppRestrictionController.refreshAppRestrictionLevelForUid(uid, 1536, 2, true);
                                        return;
                                    }
                                    return;
                                } catch (Throwable th6) {
                                    th = th6;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                throw th;
                            }
                        }
                    } catch (Throwable th8) {
                        th = th8;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    obj = obj2;
                }
                throw th;
            }
        }

        private int getCurrentDrainThresholdIndex(int uid, long now, long window) {
            if (hasMediaPlayback(uid, now, window) || hasLocation(uid, now, window)) {
                return 1;
            }
            return 0;
        }

        private boolean hasMediaPlayback(int uid, long now, long window) {
            return this.mBgCurrentDrainEventDurationBasedThresholdEnabled && ((AppBatteryTracker) this.mTracker).mAppRestrictionController.getCompositeMediaPlaybackDurations(uid, now, window) >= this.mBgCurrentDrainMediaPlaybackMinDuration;
        }

        private boolean hasLocation(int uid, long now, long window) {
            if (this.mBgCurrentDrainHighThresholdByBgLocation) {
                AppRestrictionController controller = ((AppBatteryTracker) this.mTracker).mAppRestrictionController;
                if (this.mInjector.getPermissionManagerServiceInternal().checkUidPermission(uid, "android.permission.ACCESS_BACKGROUND_LOCATION") == 0) {
                    return true;
                }
                if (this.mBgCurrentDrainEventDurationBasedThresholdEnabled) {
                    long since = Math.max(0L, now - window);
                    long locationDuration = controller.getForegroundServiceTotalDurationsSince(uid, since, now, 8);
                    return locationDuration >= this.mBgCurrentDrainLocationMinDuration;
                }
                return false;
            }
            return false;
        }

        void onUserInteractionStarted(String packageName, int uid) {
            int index;
            boolean changed = false;
            synchronized (this.mLock) {
                this.mLastInteractionTime.put(uid, SystemClock.elapsedRealtime());
                int curLevel = ((AppBatteryTracker) this.mTracker).mAppRestrictionController.getRestrictionLevel(uid, packageName);
                if (curLevel != 50 && (index = this.mHighBgBatteryPackages.indexOfKey(uid)) >= 0) {
                    this.mHighBgBatteryPackages.removeAt(index);
                    changed = true;
                }
            }
            if (changed) {
                ((AppBatteryTracker) this.mTracker).mAppRestrictionController.refreshAppRestrictionLevelForUid(uid, 768, 3, true);
            }
        }

        void onBackgroundRestrictionChanged(int uid, String pkgName, boolean restricted) {
            if (restricted) {
                return;
            }
            synchronized (this.mLock) {
                Pair<long[], ImmutableBatteryUsage[]> pair = this.mHighBgBatteryPackages.get(uid);
                if (pair != null) {
                    ((long[]) pair.first)[1] = 0;
                    ((ImmutableBatteryUsage[]) pair.second)[1] = null;
                }
            }
        }

        void reset() {
            this.mHighBgBatteryPackages.clear();
            this.mLastInteractionTime.clear();
            ((AppBatteryTracker) this.mTracker).reset();
        }

        void onUserRemovedLocked(int userId) {
            for (int i = this.mHighBgBatteryPackages.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mHighBgBatteryPackages.keyAt(i)) == userId) {
                    this.mHighBgBatteryPackages.removeAt(i);
                }
            }
            for (int i2 = this.mLastInteractionTime.size() - 1; i2 >= 0; i2--) {
                if (UserHandle.getUserId(this.mLastInteractionTime.keyAt(i2)) == userId) {
                    this.mLastInteractionTime.removeAt(i2);
                }
            }
        }

        void onUidRemovedLocked(int uid) {
            this.mHighBgBatteryPackages.remove(uid);
            this.mLastInteractionTime.delete(uid);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStatePolicy
        public void dump(PrintWriter pw, String prefix) {
            Object obj;
            Object obj2;
            pw.print(prefix);
            pw.println("APP BATTERY TRACKER POLICY SETTINGS:");
            String prefix2 = "  " + prefix;
            super.dump(pw, prefix2);
            if (isEnabled()) {
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_RESTRICTED_BUCKET);
                pw.print('=');
                char c = 0;
                pw.println(this.mBgCurrentDrainRestrictedBucketThreshold[0]);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_RESTRICTED_BUCKET);
                pw.print('=');
                char c2 = 1;
                pw.println(this.mBgCurrentDrainRestrictedBucketThreshold[1]);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_THRESHOLD_TO_BG_RESTRICTED);
                pw.print('=');
                pw.println(this.mBgCurrentDrainBgRestrictedThreshold[0]);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_TO_BG_RESTRICTED);
                pw.print('=');
                pw.println(this.mBgCurrentDrainBgRestrictedThreshold[1]);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_WINDOW);
                pw.print('=');
                pw.println(this.mBgCurrentDrainWindowMs);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_INTERACTION_GRACE_PERIOD);
                pw.print('=');
                pw.println(this.mBgCurrentDrainInteractionGracePeriodMs);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_MEDIA_PLAYBACK_MIN_DURATION);
                pw.print('=');
                pw.println(this.mBgCurrentDrainMediaPlaybackMinDuration);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_LOCATION_MIN_DURATION);
                pw.print('=');
                pw.println(this.mBgCurrentDrainLocationMinDuration);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_EVENT_DURATION_BASED_THRESHOLD_ENABLED);
                pw.print('=');
                pw.println(this.mBgCurrentDrainEventDurationBasedThresholdEnabled);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_AUTO_RESTRICT_ABUSIVE_APPS_ENABLED);
                pw.print('=');
                pw.println(this.mBgCurrentDrainAutoRestrictAbusiveAppsEnabled);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_TYPES_TO_RESTRICTED_BUCKET);
                pw.print('=');
                pw.println(batteryUsageTypesToString(this.mBgCurrentDrainRestrictedBucketTypes));
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_TYPES_TO_BG_RESTRICTED);
                pw.print('=');
                pw.println(batteryUsageTypesToString(this.mBgCurrentDrainBgRestrictedTypes));
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_POWER_COMPONENTS);
                pw.print('=');
                pw.println(this.mBgCurrentDrainPowerComponents);
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_EXEMPTED_TYPES);
                pw.print('=');
                pw.println(BaseAppStateTracker.stateTypesToString(this.mBgCurrentDrainExemptedTypes));
                pw.print(prefix2);
                pw.print(KEY_BG_CURRENT_DRAIN_HIGH_THRESHOLD_BY_BG_LOCATION);
                pw.print('=');
                pw.println(this.mBgCurrentDrainHighThresholdByBgLocation);
                pw.print(prefix2);
                pw.print("Full charge capacity=");
                pw.print(this.mBatteryFullChargeMah);
                pw.println(" mAh");
                pw.print(prefix2);
                pw.println("Excessive current drain detected:");
                Object obj3 = this.mLock;
                synchronized (obj3) {
                    try {
                        try {
                            int size = this.mHighBgBatteryPackages.size();
                            String prefix3 = "  " + prefix2;
                            if (size > 0) {
                                try {
                                    long now = SystemClock.elapsedRealtime();
                                    int i = 0;
                                    while (i < size) {
                                        int uid = this.mHighBgBatteryPackages.keyAt(i);
                                        Pair<long[], ImmutableBatteryUsage[]> pair = this.mHighBgBatteryPackages.valueAt(i);
                                        long[] ts = (long[]) pair.first;
                                        ImmutableBatteryUsage[] usages = (ImmutableBatteryUsage[]) pair.second;
                                        int thresholdIndex = getCurrentDrainThresholdIndex(uid, now, this.mBgCurrentDrainWindowMs);
                                        Object[] objArr = new Object[6];
                                        objArr[c] = prefix3;
                                        objArr[c2] = UserHandle.formatUid(uid);
                                        objArr[2] = Float.valueOf(this.mBgCurrentDrainRestrictedBucketThreshold[thresholdIndex]);
                                        objArr[3] = Float.valueOf(this.mBgCurrentDrainBgRestrictedThreshold[thresholdIndex]);
                                        String prefix4 = prefix3;
                                        int i2 = i;
                                        obj = obj3;
                                        try {
                                            objArr[4] = formatHighBgBatteryRecord(ts[0], now, usages[0]);
                                            objArr[5] = formatHighBgBatteryRecord(ts[1], now, usages[1]);
                                            pw.format("%s%s: (threshold=%4.2f%%/%4.2f%%) %s / %s\n", objArr);
                                            i = i2 + 1;
                                            obj3 = obj;
                                            c2 = 1;
                                            prefix3 = prefix4;
                                            c = 0;
                                        } catch (Throwable th) {
                                            th = th;
                                            throw th;
                                        }
                                    }
                                    obj2 = obj3;
                                } catch (Throwable th2) {
                                    th = th2;
                                    obj = obj3;
                                }
                            } else {
                                obj2 = obj3;
                                pw.print(prefix3);
                                pw.println("(none)");
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            obj = obj3;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            }
        }

        private String formatHighBgBatteryRecord(long ts, long now, ImmutableBatteryUsage usage) {
            if (ts > 0 && usage != null) {
                return String.format("%s %s (%s)", TimeUtils.formatTime(ts, now), usage.toString(), usage.percentageToString());
            }
            return "0";
        }
    }
}
