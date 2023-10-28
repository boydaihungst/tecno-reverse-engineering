package com.android.server.am;

import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.Parcelable;
import android.os.Process;
import android.os.SynchronousResultReceiver;
import android.os.SystemClock;
import android.os.ThreadLocalWorkSource;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.power.PowerStatsInternal;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.am.MeasuredEnergySnapshot;
import com.android.server.job.controllers.JobStatus;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import libcore.util.EmptyArray;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BatteryExternalStatsWorker implements BatteryStatsImpl.ExternalStatsSync {
    private static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final long MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS = 750;
    private static final String TAG = "BatteryExternalStatsWorker";
    public static final int UID_REMOVAL_AFTER_USER_REMOVAL_DELAY_MILLIS = 10000;
    private Future<?> mBatteryLevelSync;
    private Future<?> mCurrentFuture;
    private String mCurrentReason;
    private SparseArray<int[]> mEnergyConsumerTypeToIdMap;
    private final ScheduledExecutorService mExecutorService;
    final Injector mInjector;
    private long mLastCollectionTimeStamp;
    private WifiActivityEnergyInfo mLastWifiInfo;
    private MeasuredEnergySnapshot mMeasuredEnergySnapshot;
    private boolean mOnBattery;
    private boolean mOnBatteryScreenOff;
    private int[] mPerDisplayScreenStates;
    private PowerStatsInternal mPowerStatsInternal;
    private Future<?> mProcessStateSync;
    private int mScreenState;
    private final BatteryStatsImpl mStats;
    private final Runnable mSyncTask;
    private TelephonyManager mTelephony;
    private final IntArray mUidsToRemove;
    private int mUpdateFlags;
    private boolean mUseLatestStates;
    private Future<?> mWakelockChangesUpdate;
    private WifiManager mWifiManager;
    private final Object mWorkerLock;
    private final Runnable mWriteTask;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Thread lambda$new$1(final Runnable r) {
        Thread t = new Thread(new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BatteryExternalStatsWorker.lambda$new$0(r);
            }
        }, "batterystats-worker");
        t.setPriority(5);
        return t;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$new$0(Runnable r) {
        ThreadLocalWorkSource.setUid(Process.myUid());
        r.run();
    }

    /* loaded from: classes.dex */
    public static class Injector {
        private final Context mContext;

        Injector(Context context) {
            this.mContext = context;
        }

        public <T> T getSystemService(Class<T> serviceClass) {
            return (T) this.mContext.getSystemService(serviceClass);
        }

        public <T> T getLocalService(Class<T> serviceClass) {
            return (T) LocalServices.getService(serviceClass);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BatteryExternalStatsWorker(Context context, BatteryStatsImpl stats) {
        this(new Injector(context), stats);
    }

    BatteryExternalStatsWorker(Injector injector, BatteryStatsImpl stats) {
        this.mExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda7
            @Override // java.util.concurrent.ThreadFactory
            public final Thread newThread(Runnable runnable) {
                return BatteryExternalStatsWorker.lambda$new$1(runnable);
            }
        });
        this.mUpdateFlags = 0;
        this.mCurrentFuture = null;
        this.mCurrentReason = null;
        this.mPerDisplayScreenStates = null;
        this.mUseLatestStates = true;
        this.mUidsToRemove = new IntArray();
        this.mWorkerLock = new Object();
        this.mWifiManager = null;
        this.mTelephony = null;
        this.mPowerStatsInternal = null;
        this.mLastWifiInfo = new WifiActivityEnergyInfo(0L, 0, 0L, 0L, 0L, 0L);
        this.mEnergyConsumerTypeToIdMap = null;
        this.mMeasuredEnergySnapshot = null;
        this.mSyncTask = new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker.1
            @Override // java.lang.Runnable
            public void run() {
                int updateFlags;
                String reason;
                int[] uidsToRemove;
                boolean onBattery;
                boolean onBatteryScreenOff;
                int screenState;
                int[] displayScreenStates;
                boolean useLatestStates;
                synchronized (BatteryExternalStatsWorker.this) {
                    updateFlags = BatteryExternalStatsWorker.this.mUpdateFlags;
                    reason = BatteryExternalStatsWorker.this.mCurrentReason;
                    uidsToRemove = BatteryExternalStatsWorker.this.mUidsToRemove.size() > 0 ? BatteryExternalStatsWorker.this.mUidsToRemove.toArray() : EmptyArray.INT;
                    onBattery = BatteryExternalStatsWorker.this.mOnBattery;
                    onBatteryScreenOff = BatteryExternalStatsWorker.this.mOnBatteryScreenOff;
                    screenState = BatteryExternalStatsWorker.this.mScreenState;
                    displayScreenStates = BatteryExternalStatsWorker.this.mPerDisplayScreenStates;
                    useLatestStates = BatteryExternalStatsWorker.this.mUseLatestStates;
                    BatteryExternalStatsWorker.this.mUpdateFlags = 0;
                    BatteryExternalStatsWorker.this.mCurrentReason = null;
                    BatteryExternalStatsWorker.this.mUidsToRemove.clear();
                    BatteryExternalStatsWorker.this.mCurrentFuture = null;
                    BatteryExternalStatsWorker.this.mUseLatestStates = true;
                    if ((updateFlags & 63) == 63) {
                        BatteryExternalStatsWorker.this.cancelSyncDueToBatteryLevelChangeLocked();
                    }
                    if ((updateFlags & 1) != 0) {
                        BatteryExternalStatsWorker.this.cancelCpuSyncDueToWakelockChange();
                    }
                    if ((updateFlags & 14) == 14) {
                        BatteryExternalStatsWorker.this.cancelSyncDueToProcessStateChange();
                    }
                }
                try {
                    synchronized (BatteryExternalStatsWorker.this.mWorkerLock) {
                        BatteryExternalStatsWorker.this.updateExternalStatsLocked(reason, updateFlags, onBattery, onBatteryScreenOff, screenState, displayScreenStates, useLatestStates);
                    }
                    if ((updateFlags & 1) != 0) {
                        BatteryExternalStatsWorker.this.mStats.updateCpuTimesForAllUids();
                    }
                    synchronized (BatteryExternalStatsWorker.this.mStats) {
                        for (int uid : uidsToRemove) {
                            FrameworkStatsLog.write(43, -1, uid, 0);
                            BatteryExternalStatsWorker.this.mStats.maybeRemoveIsolatedUidLocked(uid, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
                        }
                        BatteryExternalStatsWorker.this.mStats.clearPendingRemovedUidsLocked();
                    }
                } catch (Exception e) {
                    Slog.wtf(BatteryExternalStatsWorker.TAG, "Error updating external stats: ", e);
                }
                if ((updateFlags & 64) != 0) {
                    synchronized (BatteryExternalStatsWorker.this) {
                        BatteryExternalStatsWorker.this.mLastCollectionTimeStamp = 0L;
                    }
                } else if ((updateFlags & 63) == 63) {
                    synchronized (BatteryExternalStatsWorker.this) {
                        BatteryExternalStatsWorker.this.mLastCollectionTimeStamp = SystemClock.elapsedRealtime();
                    }
                }
            }
        };
        this.mWriteTask = new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (BatteryExternalStatsWorker.this.mStats) {
                    BatteryExternalStatsWorker.this.mStats.writeAsyncLocked();
                }
            }
        };
        this.mInjector = injector;
        this.mStats = stats;
    }

    public void systemServicesReady() {
        int voltageMv;
        SparseArray<EnergyConsumer> idToConsumer;
        WifiManager wm = (WifiManager) this.mInjector.getSystemService(WifiManager.class);
        TelephonyManager tm = (TelephonyManager) this.mInjector.getSystemService(TelephonyManager.class);
        PowerStatsInternal psi = (PowerStatsInternal) this.mInjector.getLocalService(PowerStatsInternal.class);
        synchronized (this.mStats) {
            voltageMv = this.mStats.getBatteryVoltageMvLocked();
        }
        synchronized (this.mWorkerLock) {
            this.mWifiManager = wm;
            this.mTelephony = tm;
            this.mPowerStatsInternal = psi;
            boolean[] supportedStdBuckets = null;
            String[] customBucketNames = null;
            if (psi != null && (idToConsumer = populateEnergyConsumerSubsystemMapsLocked()) != null) {
                this.mMeasuredEnergySnapshot = new MeasuredEnergySnapshot(idToConsumer);
                try {
                    EnergyConsumerResult[] initialEcrs = getEnergyConsumptionData().get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    this.mMeasuredEnergySnapshot.updateAndGetDelta(initialEcrs, voltageMv);
                } catch (InterruptedException | TimeoutException e) {
                    Slog.w(TAG, "timeout or interrupt reading initial getEnergyConsumedAsync: " + e);
                } catch (ExecutionException e2) {
                    Slog.wtf(TAG, "exception reading initial getEnergyConsumedAsync: " + e2.getCause());
                }
                customBucketNames = this.mMeasuredEnergySnapshot.getOtherOrdinalNames();
                supportedStdBuckets = getSupportedEnergyBuckets(idToConsumer);
            }
            synchronized (this.mStats) {
                this.mStats.initMeasuredEnergyStatsLocked(supportedStdBuckets, customBucketNames);
            }
        }
    }

    public synchronized Future<?> scheduleSync(String reason, int flags) {
        return scheduleSyncLocked(reason, flags);
    }

    public synchronized Future<?> scheduleCpuSyncDueToRemovedUid(int uid) {
        this.mUidsToRemove.add(uid);
        return scheduleSyncLocked("remove-uid", 1);
    }

    public synchronized Future<?> scheduleCpuSyncDueToSettingChange() {
        return scheduleSyncLocked("setting-change", 1);
    }

    public Future<?> scheduleSyncDueToScreenStateChange(int flags, boolean onBattery, boolean onBatteryScreenOff, int screenState, int[] perDisplayScreenStates) {
        Future<?> scheduleSyncLocked;
        synchronized (this) {
            if (this.mCurrentFuture == null || (this.mUpdateFlags & 1) == 0) {
                this.mOnBattery = onBattery;
                this.mOnBatteryScreenOff = onBatteryScreenOff;
                this.mUseLatestStates = false;
            }
            this.mScreenState = screenState;
            this.mPerDisplayScreenStates = perDisplayScreenStates;
            scheduleSyncLocked = scheduleSyncLocked("screen-state", flags);
        }
        return scheduleSyncLocked;
    }

    public Future<?> scheduleCpuSyncDueToWakelockChange(long delayMillis) {
        Future<?> scheduleDelayedSyncLocked;
        synchronized (this) {
            scheduleDelayedSyncLocked = scheduleDelayedSyncLocked(this.mWakelockChangesUpdate, new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.m1221x9410955c();
                }
            }, delayMillis);
            this.mWakelockChangesUpdate = scheduleDelayedSyncLocked;
        }
        return scheduleDelayedSyncLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleCpuSyncDueToWakelockChange$3$com-android-server-am-BatteryExternalStatsWorker  reason: not valid java name */
    public /* synthetic */ void m1221x9410955c() {
        scheduleSync("wakelock-change", 1);
        scheduleRunnable(new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                BatteryExternalStatsWorker.this.m1220xa266ef3d();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleCpuSyncDueToWakelockChange$2$com-android-server-am-BatteryExternalStatsWorker  reason: not valid java name */
    public /* synthetic */ void m1220xa266ef3d() {
        this.mStats.postBatteryNeedsCpuUpdateMsg();
    }

    public void cancelCpuSyncDueToWakelockChange() {
        synchronized (this) {
            Future<?> future = this.mWakelockChangesUpdate;
            if (future != null) {
                future.cancel(false);
                this.mWakelockChangesUpdate = null;
            }
        }
    }

    public Future<?> scheduleSyncDueToBatteryLevelChange(long delayMillis) {
        Future<?> scheduleDelayedSyncLocked;
        synchronized (this) {
            scheduleDelayedSyncLocked = scheduleDelayedSyncLocked(this.mBatteryLevelSync, new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.m1222x59cf2473();
                }
            }, delayMillis);
            this.mBatteryLevelSync = scheduleDelayedSyncLocked;
        }
        return scheduleDelayedSyncLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSyncDueToBatteryLevelChange$4$com-android-server-am-BatteryExternalStatsWorker  reason: not valid java name */
    public /* synthetic */ void m1222x59cf2473() {
        scheduleSync("battery-level", 63);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelSyncDueToBatteryLevelChangeLocked() {
        Future<?> future = this.mBatteryLevelSync;
        if (future != null) {
            future.cancel(false);
            this.mBatteryLevelSync = null;
        }
    }

    public void scheduleSyncDueToProcessStateChange(final int flags, long delayMillis) {
        synchronized (this) {
            this.mProcessStateSync = scheduleDelayedSyncLocked(this.mProcessStateSync, new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.m1223xeec5ff07(flags);
                }
            }, delayMillis);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSyncDueToProcessStateChange$5$com-android-server-am-BatteryExternalStatsWorker  reason: not valid java name */
    public /* synthetic */ void m1223xeec5ff07(int flags) {
        scheduleSync("procstate-change", flags);
    }

    public void cancelSyncDueToProcessStateChange() {
        synchronized (this) {
            Future<?> future = this.mProcessStateSync;
            if (future != null) {
                future.cancel(false);
                this.mProcessStateSync = null;
            }
        }
    }

    public Future<?> scheduleCleanupDueToRemovedUser(final int userId) {
        ScheduledFuture<?> schedule;
        synchronized (this) {
            schedule = this.mExecutorService.schedule(new Runnable() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryExternalStatsWorker.this.m1219xd44d1b6c(userId);
                }
            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, TimeUnit.MILLISECONDS);
        }
        return schedule;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleCleanupDueToRemovedUser$6$com-android-server-am-BatteryExternalStatsWorker  reason: not valid java name */
    public /* synthetic */ void m1219xd44d1b6c(int userId) {
        synchronized (this.mStats) {
            this.mStats.clearRemovedUserUidsLocked(userId);
        }
    }

    private Future<?> scheduleDelayedSyncLocked(Future<?> lastScheduledSync, Runnable syncRunnable, long delayMillis) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (lastScheduledSync != null) {
            if (delayMillis == 0) {
                lastScheduledSync.cancel(false);
            } else {
                return lastScheduledSync;
            }
        }
        return this.mExecutorService.schedule(syncRunnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized Future<?> scheduleWrite() {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        scheduleSyncLocked("write", 63);
        return this.mExecutorService.submit(this.mWriteTask);
    }

    public synchronized void scheduleRunnable(Runnable runnable) {
        if (!this.mExecutorService.isShutdown()) {
            this.mExecutorService.submit(runnable);
        }
    }

    public void shutdown() {
        this.mExecutorService.shutdownNow();
    }

    private Future<?> scheduleSyncLocked(String reason, int flags) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (this.mCurrentFuture == null) {
            this.mUpdateFlags = flags;
            this.mCurrentReason = reason;
            this.mCurrentFuture = this.mExecutorService.submit(this.mSyncTask);
        }
        this.mUpdateFlags |= flags;
        return this.mCurrentFuture;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCollectionTimeStamp() {
        long j;
        synchronized (this) {
            j = this.mLastCollectionTimeStamp;
        }
        return j;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [733=8] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:194:0x016c A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:51:0x00f7 A[ADDED_TO_REGION] */
    /* JADX WARN: Type inference failed for: r14v3, types: [com.android.internal.os.BatteryStatsImpl] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateExternalStatsLocked(String reason, int updateFlags, boolean onBattery, boolean onBatteryScreenOff, int screenState, int[] displayScreenStates, boolean useLatestStates) {
        final SynchronousResultReceiver tempWifiReceiver;
        ModemActivityInfo modemInfo;
        MeasuredEnergySnapshot.MeasuredEnergyDeltaData measuredEnergyDeltas;
        ?? r14;
        int[] iArr;
        boolean onBattery2;
        long elapsedRealtime;
        MeasuredEnergySnapshot.MeasuredEnergyDeltaData measuredEnergyDeltas2;
        int i;
        boolean onBattery3;
        boolean onBatteryScreenOff2;
        long[] cpuClusterChargeUC;
        int voltageMv;
        EnergyConsumerResult[] ecrs;
        BluetoothAdapter adapter;
        SynchronousResultReceiver bluetoothReceiver = null;
        CompletableFuture<ModemActivityInfo> modemFuture = CompletableFuture.completedFuture(null);
        boolean railUpdated = false;
        CompletableFuture<EnergyConsumerResult[]> futureECRs = getMeasuredEnergyLocked(updateFlags);
        if ((updateFlags & 2) != 0) {
            WifiManager wifiManager = this.mWifiManager;
            if (wifiManager == null || !wifiManager.isEnhancedPowerReportingSupported()) {
                tempWifiReceiver = null;
            } else {
                tempWifiReceiver = new SynchronousResultReceiver("wifi");
                this.mWifiManager.getWifiActivityEnergyInfoAsync(new Executor() { // from class: com.android.server.am.BatteryExternalStatsWorker.3
                    @Override // java.util.concurrent.Executor
                    public void execute(Runnable runnable) {
                        runnable.run();
                    }
                }, new WifiManager.OnWifiActivityEnergyInfoListener() { // from class: com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda3
                    public final void onWifiActivityEnergyInfo(WifiActivityEnergyInfo wifiActivityEnergyInfo) {
                        BatteryExternalStatsWorker.lambda$updateExternalStatsLocked$7(tempWifiReceiver, wifiActivityEnergyInfo);
                    }
                });
            }
            synchronized (this.mStats) {
                this.mStats.updateRailStatsLocked();
            }
            railUpdated = true;
        } else {
            tempWifiReceiver = null;
        }
        if ((updateFlags & 8) != 0 && (adapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            final SynchronousResultReceiver resultReceiver = new SynchronousResultReceiver("bluetooth");
            adapter.requestControllerActivityEnergyInfo(new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), new BluetoothAdapter.OnBluetoothActivityEnergyInfoCallback() { // from class: com.android.server.am.BatteryExternalStatsWorker.4
                public void onBluetoothActivityEnergyInfoAvailable(BluetoothActivityEnergyInfo info) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("controller_activity", info);
                    resultReceiver.send(0, bundle);
                }

                public void onBluetoothActivityEnergyInfoError(int errorCode) {
                    Slog.w(BatteryExternalStatsWorker.TAG, "error reading Bluetooth stats: " + errorCode);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("controller_activity", null);
                    resultReceiver.send(0, bundle);
                }
            });
            bluetoothReceiver = resultReceiver;
        }
        if ((updateFlags & 4) != 0) {
            if (this.mTelephony != null) {
                final CompletableFuture<ModemActivityInfo> temp = new CompletableFuture<>();
                this.mTelephony.requestModemActivityInfo(new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), new OutcomeReceiver<ModemActivityInfo, TelephonyManager.ModemActivityInfoException>() { // from class: com.android.server.am.BatteryExternalStatsWorker.5
                    /* JADX DEBUG: Method merged with bridge method */
                    @Override // android.os.OutcomeReceiver
                    public void onResult(ModemActivityInfo result) {
                        temp.complete(result);
                    }

                    /* JADX DEBUG: Method merged with bridge method */
                    @Override // android.os.OutcomeReceiver
                    public void onError(TelephonyManager.ModemActivityInfoException e) {
                        Slog.w(BatteryExternalStatsWorker.TAG, "error reading modem stats:" + e);
                        temp.complete(null);
                    }
                });
                modemFuture = temp;
            }
            if (!railUpdated) {
                synchronized (this.mStats) {
                    this.mStats.updateRailStatsLocked();
                }
            }
        }
        if ((updateFlags & 16) != 0) {
            this.mStats.fillLowPowerStats();
        }
        WifiActivityEnergyInfo wifiInfo = (WifiActivityEnergyInfo) awaitControllerInfo(tempWifiReceiver);
        BluetoothActivityEnergyInfo bluetoothInfo = awaitControllerInfo(bluetoothReceiver);
        try {
            ModemActivityInfo modemInfo2 = modemFuture.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            modemInfo = modemInfo2;
        } catch (InterruptedException | TimeoutException e) {
            Slog.w(TAG, "timeout or interrupt reading modem stats: " + e);
            modemInfo = null;
            if (this.mMeasuredEnergySnapshot != null) {
            }
            measuredEnergyDeltas = null;
            long elapsedRealtime2 = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            long elapsedRealtimeUs = elapsedRealtime2 * 1000;
            long j = uptime * 1000;
            r14 = this.mStats;
            synchronized (r14) {
            }
        } catch (ExecutionException e2) {
            Slog.w(TAG, "exception reading modem stats: " + e2.getCause());
            modemInfo = null;
            if (this.mMeasuredEnergySnapshot != null) {
            }
            measuredEnergyDeltas = null;
            long elapsedRealtime22 = SystemClock.elapsedRealtime();
            long uptime2 = SystemClock.uptimeMillis();
            long elapsedRealtimeUs2 = elapsedRealtime22 * 1000;
            long j2 = uptime2 * 1000;
            r14 = this.mStats;
            synchronized (r14) {
            }
        }
        if (this.mMeasuredEnergySnapshot != null || futureECRs == null) {
            measuredEnergyDeltas = null;
        } else {
            synchronized (this.mStats) {
                voltageMv = this.mStats.getBatteryVoltageMvLocked();
            }
            try {
                ecrs = futureECRs.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException e3) {
                Slog.w(TAG, "timeout or interrupt reading getEnergyConsumedAsync: " + e3);
                ecrs = null;
            } catch (ExecutionException e4) {
                Slog.wtf(TAG, "exception reading getEnergyConsumedAsync: " + e4.getCause());
                ecrs = null;
            }
            measuredEnergyDeltas = this.mMeasuredEnergySnapshot.updateAndGetDelta(ecrs, voltageMv);
        }
        long elapsedRealtime222 = SystemClock.elapsedRealtime();
        long uptime22 = SystemClock.uptimeMillis();
        long elapsedRealtimeUs22 = elapsedRealtime222 * 1000;
        long j22 = uptime22 * 1000;
        r14 = this.mStats;
        synchronized (r14) {
            try {
                this.mStats.addHistoryEventLocked(elapsedRealtime222, uptime22, 14, reason, 0);
                if ((updateFlags & 1) != 0) {
                    if (useLatestStates) {
                        try {
                            onBattery3 = this.mStats.isOnBatteryLocked();
                        } catch (Throwable th) {
                            th = th;
                            iArr = r14;
                        }
                        try {
                            onBatteryScreenOff2 = this.mStats.isOnBatteryScreenOffLocked();
                        } catch (Throwable th2) {
                            th = th2;
                            iArr = r14;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    } else {
                        onBattery3 = onBattery;
                        onBatteryScreenOff2 = onBatteryScreenOff;
                    }
                    if (measuredEnergyDeltas == null) {
                        cpuClusterChargeUC = null;
                    } else {
                        try {
                            cpuClusterChargeUC = measuredEnergyDeltas.cpuClusterChargeUC;
                        } catch (Throwable th4) {
                            th = th4;
                            iArr = r14;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    }
                    try {
                        this.mStats.updateCpuTimeLocked(onBattery3, onBatteryScreenOff2, cpuClusterChargeUC);
                        onBattery2 = onBattery3;
                    } catch (Throwable th5) {
                        th = th5;
                        iArr = r14;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } else {
                    onBattery2 = onBattery;
                }
                if ((updateFlags & 63) == 63) {
                    try {
                        this.mStats.updateKernelWakelocksLocked(elapsedRealtimeUs22);
                        this.mStats.updateKernelMemoryBandwidthLocked(elapsedRealtimeUs22);
                    } catch (Throwable th6) {
                        th = th6;
                        iArr = r14;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                if ((updateFlags & 16) != 0) {
                    this.mStats.updateRpmStatsLocked(elapsedRealtimeUs22);
                }
                if (measuredEnergyDeltas != null) {
                    try {
                        long[] displayChargeUC = measuredEnergyDeltas.displayChargeUC;
                        if (displayChargeUC == null || displayChargeUC.length <= 0) {
                            iArr = displayScreenStates;
                        } else {
                            iArr = displayScreenStates;
                            try {
                                this.mStats.updateDisplayMeasuredEnergyStatsLocked(displayChargeUC, iArr, elapsedRealtime222);
                            } catch (Throwable th7) {
                                th = th7;
                                iArr = r14;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                        long gnssChargeUC = measuredEnergyDeltas.gnssChargeUC;
                        if (gnssChargeUC != -1) {
                            this.mStats.updateGnssMeasuredEnergyStatsLocked(gnssChargeUC, elapsedRealtime222);
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        iArr = r14;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } else {
                    iArr = displayScreenStates;
                }
                if (measuredEnergyDeltas != null && measuredEnergyDeltas.otherTotalChargeUC != null) {
                    int ord = 0;
                    while (ord < measuredEnergyDeltas.otherTotalChargeUC.length) {
                        long totalEnergy = measuredEnergyDeltas.otherTotalChargeUC[ord];
                        SparseLongArray uidEnergies = measuredEnergyDeltas.otherUidChargesUC[ord];
                        this.mStats.updateCustomMeasuredEnergyStatsLocked(ord, totalEnergy, uidEnergies);
                        ord++;
                        iArr = displayScreenStates;
                    }
                }
                try {
                    if (bluetoothInfo != null) {
                        try {
                            if (bluetoothInfo.isValid()) {
                                long btChargeUC = measuredEnergyDeltas != null ? measuredEnergyDeltas.bluetoothChargeUC : -1L;
                                i = 63;
                                elapsedRealtime = elapsedRealtime222;
                                measuredEnergyDeltas2 = measuredEnergyDeltas;
                                iArr = r14;
                                onBattery = onBattery2;
                                this.mStats.updateBluetoothStateLocked(bluetoothInfo, btChargeUC, elapsedRealtime, uptime22);
                            } else {
                                onBattery = onBattery2;
                                elapsedRealtime = elapsedRealtime222;
                                measuredEnergyDeltas2 = measuredEnergyDeltas;
                                iArr = r14;
                                i = 63;
                                Slog.w(TAG, "bluetooth info is invalid: " + bluetoothInfo);
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            iArr = r14;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    } else {
                        onBattery = onBattery2;
                        elapsedRealtime = elapsedRealtime222;
                        measuredEnergyDeltas2 = measuredEnergyDeltas;
                        iArr = r14;
                        i = 63;
                    }
                    if (wifiInfo != null) {
                        if (wifiInfo.isValid()) {
                            long wifiChargeUC = measuredEnergyDeltas2 != null ? measuredEnergyDeltas2.wifiChargeUC : -1L;
                            NetworkStatsManager networkStatsManager = (NetworkStatsManager) this.mInjector.getSystemService(NetworkStatsManager.class);
                            this.mStats.updateWifiState(extractDeltaLocked(wifiInfo), wifiChargeUC, elapsedRealtime, uptime22, networkStatsManager);
                        } else {
                            Slog.w(TAG, "wifi info is invalid: " + wifiInfo);
                        }
                    }
                    if (modemInfo != null) {
                        long mobileRadioChargeUC = measuredEnergyDeltas2 != null ? measuredEnergyDeltas2.mobileRadioChargeUC : -1L;
                        NetworkStatsManager networkStatsManager2 = (NetworkStatsManager) this.mInjector.getSystemService(NetworkStatsManager.class);
                        this.mStats.noteModemControllerActivity(modemInfo, mobileRadioChargeUC, elapsedRealtime, uptime22, networkStatsManager2);
                    }
                    if ((updateFlags & 63) == i) {
                        this.mStats.informThatAllExternalStatsAreFlushed();
                    }
                } catch (Throwable th10) {
                    th = th10;
                }
            } catch (Throwable th11) {
                th = th11;
                iArr = r14;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateExternalStatsLocked$7(SynchronousResultReceiver tempWifiReceiver, WifiActivityEnergyInfo info) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("controller_activity", info);
        tempWifiReceiver.send(0, bundle);
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver receiver) {
        if (receiver == null) {
            return null;
        }
        try {
            SynchronousResultReceiver.Result result = receiver.awaitResult((long) EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (result.bundle != null) {
                result.bundle.setDefusable(true);
                T data = (T) result.bundle.getParcelable("controller_activity");
                if (data != null) {
                    return data;
                }
            }
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + receiver.getName() + " stats");
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x00f6  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private WifiActivityEnergyInfo extractDeltaLocked(WifiActivityEnergyInfo latest) {
        long deltaControllerIdleDurationMillis;
        long deltaControllerScanDurationMillis;
        long deltaControllerRxDurationMillis;
        long deltaControllerTxDurationMillis;
        boolean wasReset;
        long deltaControllerEnergyUsedMicroJoules;
        long deltaControllerEnergyUsedMicroJoules2;
        long deltaControllerIdleDurationMillis2;
        long deltaControllerIdleDurationMillis3;
        long deltaControllerRxDurationMillis2;
        long deltaControllerTxDurationMillis2;
        long timePeriodMs = latest.getTimeSinceBootMillis() - this.mLastWifiInfo.getTimeSinceBootMillis();
        long lastScanMs = this.mLastWifiInfo.getControllerScanDurationMillis();
        long lastIdleMs = this.mLastWifiInfo.getControllerIdleDurationMillis();
        long lastTxMs = this.mLastWifiInfo.getControllerTxDurationMillis();
        long lastRxMs = this.mLastWifiInfo.getControllerRxDurationMillis();
        long lastEnergy = this.mLastWifiInfo.getControllerEnergyUsedMicroJoules();
        long deltaTimeSinceBootMillis = latest.getTimeSinceBootMillis();
        int deltaStackState = latest.getStackState();
        long txTimeMs = latest.getControllerTxDurationMillis() - lastTxMs;
        long rxTimeMs = latest.getControllerRxDurationMillis() - lastRxMs;
        long idleTimeMs = latest.getControllerIdleDurationMillis() - lastIdleMs;
        long scanTimeMs = latest.getControllerScanDurationMillis() - lastScanMs;
        if (txTimeMs >= 0 && rxTimeMs >= 0 && scanTimeMs >= 0) {
            if (idleTimeMs >= 0) {
                long lastScanMs2 = latest.getControllerEnergyUsedMicroJoules() - lastEnergy;
                deltaControllerEnergyUsedMicroJoules = Math.max(0L, lastScanMs2);
                wasReset = false;
                deltaControllerTxDurationMillis = txTimeMs;
                deltaControllerRxDurationMillis = rxTimeMs;
                deltaControllerScanDurationMillis = scanTimeMs;
                deltaControllerIdleDurationMillis = idleTimeMs;
                this.mLastWifiInfo = latest;
                WifiActivityEnergyInfo delta = new WifiActivityEnergyInfo(deltaTimeSinceBootMillis, deltaStackState, deltaControllerTxDurationMillis, deltaControllerRxDurationMillis, deltaControllerScanDurationMillis, deltaControllerIdleDurationMillis, deltaControllerEnergyUsedMicroJoules);
                if (wasReset) {
                    Slog.v(TAG, "WiFi energy data was reset, new WiFi energy data is " + delta);
                }
                return delta;
            }
        }
        long lastScanMs3 = latest.getControllerTxDurationMillis();
        long totalOnTimeMs = lastScanMs3 + latest.getControllerRxDurationMillis() + latest.getControllerIdleDurationMillis();
        if (totalOnTimeMs <= MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS + timePeriodMs) {
            deltaControllerEnergyUsedMicroJoules2 = latest.getControllerEnergyUsedMicroJoules();
            long deltaControllerRxDurationMillis3 = latest.getControllerRxDurationMillis();
            long deltaControllerTxDurationMillis3 = latest.getControllerTxDurationMillis();
            long deltaControllerIdleDurationMillis4 = latest.getControllerIdleDurationMillis();
            deltaControllerIdleDurationMillis2 = latest.getControllerScanDurationMillis();
            deltaControllerIdleDurationMillis3 = deltaControllerIdleDurationMillis4;
            deltaControllerRxDurationMillis2 = deltaControllerTxDurationMillis3;
            deltaControllerTxDurationMillis2 = deltaControllerRxDurationMillis3;
        } else {
            deltaControllerEnergyUsedMicroJoules2 = 0;
            deltaControllerIdleDurationMillis2 = 0;
            deltaControllerIdleDurationMillis3 = 0;
            deltaControllerRxDurationMillis2 = 0;
            deltaControllerTxDurationMillis2 = 0;
        }
        wasReset = true;
        deltaControllerEnergyUsedMicroJoules = deltaControllerEnergyUsedMicroJoules2;
        deltaControllerTxDurationMillis = deltaControllerRxDurationMillis2;
        deltaControllerRxDurationMillis = deltaControllerTxDurationMillis2;
        deltaControllerScanDurationMillis = deltaControllerIdleDurationMillis2;
        deltaControllerIdleDurationMillis = deltaControllerIdleDurationMillis3;
        this.mLastWifiInfo = latest;
        WifiActivityEnergyInfo delta2 = new WifiActivityEnergyInfo(deltaTimeSinceBootMillis, deltaStackState, deltaControllerTxDurationMillis, deltaControllerRxDurationMillis, deltaControllerScanDurationMillis, deltaControllerIdleDurationMillis, deltaControllerEnergyUsedMicroJoules);
        if (wasReset) {
        }
        return delta2;
    }

    private static boolean[] getSupportedEnergyBuckets(SparseArray<EnergyConsumer> idToConsumer) {
        if (idToConsumer == null) {
            return null;
        }
        boolean[] buckets = new boolean[8];
        int size = idToConsumer.size();
        for (int idx = 0; idx < size; idx++) {
            EnergyConsumer consumer = idToConsumer.valueAt(idx);
            switch (consumer.type) {
                case 1:
                    buckets[5] = true;
                    break;
                case 2:
                    buckets[3] = true;
                    break;
                case 3:
                    buckets[0] = true;
                    buckets[1] = true;
                    buckets[2] = true;
                    break;
                case 4:
                    buckets[6] = true;
                    break;
                case 5:
                    buckets[7] = true;
                    break;
                case 6:
                    buckets[4] = true;
                    break;
            }
        }
        return buckets;
    }

    private CompletableFuture<EnergyConsumerResult[]> getEnergyConsumptionData() {
        return getEnergyConsumptionData(new int[0]);
    }

    private CompletableFuture<EnergyConsumerResult[]> getEnergyConsumptionData(int[] consumerIds) {
        return this.mPowerStatsInternal.getEnergyConsumedAsync(consumerIds);
    }

    public CompletableFuture<EnergyConsumerResult[]> getMeasuredEnergyLocked(int flags) {
        if (this.mMeasuredEnergySnapshot == null || this.mPowerStatsInternal == null) {
            return null;
        }
        if (flags == 63) {
            return getEnergyConsumptionData();
        }
        IntArray energyConsumerIds = new IntArray();
        if ((flags & 8) != 0) {
            addEnergyConsumerIdLocked(energyConsumerIds, 1);
        }
        if ((flags & 1) != 0) {
            addEnergyConsumerIdLocked(energyConsumerIds, 2);
        }
        if ((flags & 32) != 0) {
            addEnergyConsumerIdLocked(energyConsumerIds, 3);
        }
        if ((flags & 4) != 0) {
            addEnergyConsumerIdLocked(energyConsumerIds, 5);
        }
        if ((flags & 2) != 0) {
            addEnergyConsumerIdLocked(energyConsumerIds, 6);
        }
        if (energyConsumerIds.size() == 0) {
            return null;
        }
        return getEnergyConsumptionData(energyConsumerIds.toArray());
    }

    private void addEnergyConsumerIdLocked(IntArray energyConsumerIds, int type) {
        int[] consumerIds = this.mEnergyConsumerTypeToIdMap.get(type);
        if (consumerIds == null) {
            return;
        }
        energyConsumerIds.addAll(consumerIds);
    }

    private SparseArray<EnergyConsumer> populateEnergyConsumerSubsystemMapsLocked() {
        EnergyConsumer[] energyConsumers;
        PowerStatsInternal powerStatsInternal = this.mPowerStatsInternal;
        if (powerStatsInternal == null || (energyConsumers = powerStatsInternal.getEnergyConsumerInfo()) == null || energyConsumers.length == 0) {
            return null;
        }
        SparseArray<EnergyConsumer> idToConsumer = new SparseArray<>(energyConsumers.length);
        SparseArray<IntArray> tempTypeToId = new SparseArray<>();
        for (EnergyConsumer consumer : energyConsumers) {
            if (consumer.ordinal != 0) {
                switch (consumer.type) {
                    case 0:
                    case 2:
                    case 3:
                        break;
                    case 1:
                    default:
                        Slog.w(TAG, "EnergyConsumer '" + consumer.name + "' has unexpected ordinal " + consumer.ordinal + " for type " + ((int) consumer.type));
                        continue;
                }
            }
            idToConsumer.put(consumer.id, consumer);
            IntArray ids = tempTypeToId.get(consumer.type);
            if (ids == null) {
                ids = new IntArray();
                tempTypeToId.put(consumer.type, ids);
            }
            ids.add(consumer.id);
        }
        this.mEnergyConsumerTypeToIdMap = new SparseArray<>(tempTypeToId.size());
        int size = tempTypeToId.size();
        for (int i = 0; i < size; i++) {
            int consumerType = tempTypeToId.keyAt(i);
            int[] consumerIds = tempTypeToId.valueAt(i).toArray();
            this.mEnergyConsumerTypeToIdMap.put(consumerType, consumerIds);
        }
        return idToConsumer;
    }
}
