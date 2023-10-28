package com.android.server.am;

import android.app.StatsManager;
import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.State;
import android.hardware.power.stats.StateResidency;
import android.hardware.power.stats.StateResidencyResult;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManagerInternal;
import android.os.BatteryStats;
import android.os.BatteryStatsInternal;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Binder;
import android.os.BluetoothBatteryStats;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFormatException;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WakeLockStats;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.os.connectivity.WifiBatteryStats;
import android.os.health.HealthStatsParceler;
import android.os.health.HealthStatsWriter;
import android.os.health.UidHealthStats;
import android.power.PowerStatsInternal;
import android.provider.Settings;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.StatsEvent;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BatteryUsageStatsProvider;
import com.android.internal.os.BatteryUsageStatsStore;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.RailStats;
import com.android.internal.os.RpmStats;
import com.android.internal.os.SystemServerCpuThreadReader;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.ParseUtils;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.net.module.util.NetworkCapabilitiesUtils;
import com.android.net.module.util.PermissionUtils;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import com.transsion.hubcore.fuelgauge.ITranFuelgauge;
import com.transsion.hubcore.resmonitor.ITranResmonitor;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public final class BatteryStatsService extends IBatteryStats.Stub implements PowerManagerInternal.LowPowerModeListener, BatteryStatsImpl.PlatformIdleStateCallback, BatteryStatsImpl.MeasuredEnergyRetriever, Watchdog.Monitor {
    private static final boolean BATTERY_USAGE_STORE_ENABLED = true;
    static final boolean DBG = false;
    private static final String EMPTY = "Empty";
    private static final int MAX_LOW_POWER_STATS_SIZE = 16384;
    private static final int POWER_STATS_QUERY_TIMEOUT_MILLIS = 2000;
    static final String TAG = "BatteryStatsService";
    private static IBatteryStats sService;
    private BatteryManagerInternal mBatteryManagerInternal;
    private final BatteryUsageStatsProvider mBatteryUsageStatsProvider;
    private final BatteryUsageStatsStore mBatteryUsageStatsStore;
    private final Context mContext;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private ITranResmonitor mITranResmonitor;
    private final PowerProfile mPowerProfile;
    final BatteryStatsImpl mStats;
    private final BatteryStatsImpl.UserInfoProvider mUserManagerUserInfoProvider;
    private final BatteryExternalStatsWorker mWorker;
    private CharsetDecoder mDecoderStat = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
    private final Object mLock = new Object();
    private final Object mPowerStatsLock = new Object();
    private PowerStatsInternal mPowerStatsInternal = null;
    private Map<Integer, String> mEntityNames = new HashMap();
    private Map<Integer, Map<Integer, String>> mStateNames = new HashMap();
    private int mLastPowerStateFromRadio = 1;
    private int mLastPowerStateFromWifi = 1;
    private final INetworkManagementEventObserver mActivityChangeObserver = new BaseNetworkObserver() { // from class: com.android.server.am.BatteryStatsService.1
        public void interfaceClassDataActivityChanged(int transportType, boolean active, long tsNanos, int uid) {
            int powerState;
            long timestampNanos;
            if (active) {
                powerState = 3;
            } else {
                powerState = 1;
            }
            if (tsNanos <= 0) {
                timestampNanos = SystemClock.elapsedRealtimeNanos();
            } else {
                timestampNanos = tsNanos;
            }
            switch (transportType) {
                case 0:
                    BatteryStatsService.this.noteMobileRadioPowerState(powerState, timestampNanos, uid);
                    return;
                case 1:
                    BatteryStatsService.this.noteWifiRadioPowerState(powerState, timestampNanos, uid);
                    return;
                default:
                    Slog.d(BatteryStatsService.TAG, "Received unexpected transport in interfaceClassDataActivityChanged unexpected type: " + transportType);
                    return;
            }
        }
    };
    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.am.BatteryStatsService.2
        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            String state = networkCapabilities.hasCapability(21) ? "CONNECTED" : "SUSPENDED";
            BatteryStatsService.this.noteConnectivityChanged(NetworkCapabilitiesUtils.getDisplayTransport(networkCapabilities.getTransportTypes()), state);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            BatteryStatsService.this.noteConnectivityChanged(-1, "DISCONNECTED");
        }
    };

    private native void getRailEnergyPowerStats(RailStats railStats);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeWaitWakeup(ByteBuffer byteBuffer);

    private void populatePowerEntityMaps() {
        PowerEntity[] entities = this.mPowerStatsInternal.getPowerEntityInfo();
        if (entities == null) {
            return;
        }
        for (PowerEntity entity : entities) {
            Map<Integer, String> states = new HashMap<>();
            for (int j = 0; j < entity.states.length; j++) {
                State state = entity.states[j];
                states.put(Integer.valueOf(state.id), state.name);
            }
            this.mEntityNames.put(Integer.valueOf(entity.id), entity.name);
            this.mStateNames.put(Integer.valueOf(entity.id), states);
        }
    }

    public void fillLowPowerStats(RpmStats rpmStats) {
        synchronized (this.mPowerStatsLock) {
            if (this.mPowerStatsInternal != null && !this.mEntityNames.isEmpty() && !this.mStateNames.isEmpty()) {
                try {
                    StateResidencyResult[] results = this.mPowerStatsInternal.getStateResidencyAsync(new int[0]).get(2000L, TimeUnit.MILLISECONDS);
                    if (results == null) {
                        return;
                    }
                    for (StateResidencyResult result : results) {
                        RpmStats.PowerStateSubsystem subsystem = rpmStats.getSubsystem(this.mEntityNames.get(Integer.valueOf(result.id)));
                        for (int j = 0; j < result.stateResidencyData.length; j++) {
                            StateResidency stateResidency = result.stateResidencyData[j];
                            subsystem.putState(this.mStateNames.get(Integer.valueOf(result.id)).get(Integer.valueOf(stateResidency.id)), stateResidency.totalTimeInStateMs, (int) stateResidency.totalStateEntryCount);
                        }
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to getStateResidencyAsync", e);
                }
            }
        }
    }

    public void fillRailDataStats(RailStats railStats) {
        getRailEnergyPowerStats(railStats);
    }

    public String getSubsystemLowPowerStats() {
        synchronized (this.mPowerStatsLock) {
            if (this.mPowerStatsInternal != null && !this.mEntityNames.isEmpty() && !this.mStateNames.isEmpty()) {
                try {
                    StateResidencyResult[] results = this.mPowerStatsInternal.getStateResidencyAsync(new int[0]).get(2000L, TimeUnit.MILLISECONDS);
                    if (results == null || results.length == 0) {
                        return EMPTY;
                    }
                    int charsLeft = 16384;
                    StringBuilder builder = new StringBuilder("SubsystemPowerState");
                    int i = 0;
                    while (true) {
                        if (i >= results.length) {
                            break;
                        }
                        StateResidencyResult result = results[i];
                        StringBuilder subsystemBuilder = new StringBuilder();
                        subsystemBuilder.append(" subsystem_" + i);
                        subsystemBuilder.append(" name=" + this.mEntityNames.get(Integer.valueOf(result.id)));
                        for (int j = 0; j < result.stateResidencyData.length; j++) {
                            StateResidency stateResidency = result.stateResidencyData[j];
                            subsystemBuilder.append(" state_" + j);
                            subsystemBuilder.append(" name=" + this.mStateNames.get(Integer.valueOf(result.id)).get(Integer.valueOf(stateResidency.id)));
                            subsystemBuilder.append(" time=" + stateResidency.totalTimeInStateMs);
                            subsystemBuilder.append(" count=" + stateResidency.totalStateEntryCount);
                            subsystemBuilder.append(" last entry=" + stateResidency.lastEntryTimestampMs);
                        }
                        int j2 = subsystemBuilder.length();
                        if (j2 <= charsLeft) {
                            charsLeft -= subsystemBuilder.length();
                            builder.append((CharSequence) subsystemBuilder);
                            i++;
                        } else {
                            Slog.e(TAG, "getSubsystemLowPowerStats: buffer not enough");
                            break;
                        }
                    }
                    return builder.toString();
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to getStateResidencyAsync", e);
                    return EMPTY;
                }
            }
            return EMPTY;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BatteryStatsService(Context context, File systemDir, Handler handler) {
        this.mContext = context;
        BatteryStatsImpl.UserInfoProvider userInfoProvider = new BatteryStatsImpl.UserInfoProvider() { // from class: com.android.server.am.BatteryStatsService.3
            private UserManagerInternal umi;

            public int[] getUserIds() {
                if (this.umi == null) {
                    this.umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
                }
                UserManagerInternal userManagerInternal = this.umi;
                if (userManagerInternal != null) {
                    return userManagerInternal.getUserIds();
                }
                return null;
            }
        };
        this.mUserManagerUserInfoProvider = userInfoProvider;
        HandlerThread handlerThread = new HandlerThread("batterystats-handler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Handler handler2 = new Handler(handlerThread.getLooper());
        this.mHandler = handler2;
        PowerProfile powerProfile = new PowerProfile(context);
        this.mPowerProfile = powerProfile;
        BatteryStatsImpl batteryStatsImpl = new BatteryStatsImpl(systemDir, handler, this, this, userInfoProvider);
        this.mStats = batteryStatsImpl;
        if (Build.TRAN_RM2_SUPPORT) {
            ITranResmonitor Instance = ITranResmonitor.Instance();
            this.mITranResmonitor = Instance;
            Instance.setSystemDir(systemDir);
        }
        BatteryExternalStatsWorker batteryExternalStatsWorker = new BatteryExternalStatsWorker(context, batteryStatsImpl);
        this.mWorker = batteryExternalStatsWorker;
        batteryStatsImpl.setExternalStatsSyncLocked(batteryExternalStatsWorker);
        batteryStatsImpl.setRadioScanningTimeoutLocked(context.getResources().getInteger(17694921) * 1000);
        batteryStatsImpl.setPowerProfileLocked(powerProfile);
        batteryStatsImpl.startTrackingSystemServerCpuTime();
        BatteryUsageStatsStore batteryUsageStatsStore = new BatteryUsageStatsStore(context, batteryStatsImpl, systemDir, handler2);
        this.mBatteryUsageStatsStore = batteryUsageStatsStore;
        this.mBatteryUsageStatsProvider = new BatteryUsageStatsProvider(context, batteryStatsImpl, batteryUsageStatsStore);
    }

    public void publish() {
        LocalServices.addService(BatteryStatsInternal.class, new LocalService());
        ServiceManager.addService("batterystats", asBinder());
    }

    public void systemServicesReady() {
        this.mStats.systemServicesReady(this.mContext);
        this.mWorker.systemServicesReady();
        INetworkManagementService nms = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        try {
            nms.registerObserver(this.mActivityChangeObserver);
            cm.registerDefaultNetworkCallback(this.mNetworkCallback);
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not register INetworkManagement event observer " + e);
        }
        synchronized (this.mPowerStatsLock) {
            PowerStatsInternal powerStatsInternal = (PowerStatsInternal) LocalServices.getService(PowerStatsInternal.class);
            this.mPowerStatsInternal = powerStatsInternal;
            if (powerStatsInternal != null) {
                populatePowerEntityMaps();
            } else {
                Slog.e(TAG, "Could not register PowerStatsInternal");
            }
        }
        this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        Watchdog.getInstance().addMonitor(this);
        DataConnectionStats dataConnectionStats = new DataConnectionStats(this.mContext, this.mHandler);
        dataConnectionStats.startMonitoring();
        registerStatsCallbacks();
    }

    public void onSystemReady() {
        this.mStats.onSystemReady();
        this.mBatteryUsageStatsStore.onSystemReady();
        ITranFuelgauge.Instance().init(this.mStats);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends BatteryStatsInternal {
        private LocalService() {
        }

        @Override // android.os.BatteryStatsInternal
        public String[] getWifiIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getWifiIfaces().clone();
        }

        @Override // android.os.BatteryStatsInternal
        public String[] getMobileIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getMobileIfaces().clone();
        }

        @Override // android.os.BatteryStatsInternal
        public SystemServerCpuThreadReader.SystemServiceCpuThreadTimes getSystemServiceCpuThreadTimes() {
            return BatteryStatsService.this.mStats.getSystemServiceCpuThreadTimes();
        }

        @Override // android.os.BatteryStatsInternal
        public List<BatteryUsageStats> getBatteryUsageStats(List<BatteryUsageStatsQuery> queries) {
            return BatteryStatsService.this.getBatteryUsageStats(queries);
        }

        @Override // android.os.BatteryStatsInternal
        public void noteJobsDeferred(int uid, int numDeferred, long sinceLast) {
            BatteryStatsService.this.noteJobsDeferred(uid, numDeferred, sinceLast);
        }

        @Override // android.os.BatteryStatsInternal
        public void noteBinderCallStats(int workSourceUid, long incrementatCallCount, Collection<BinderCallsStats.CallStat> callStats) {
            synchronized (BatteryStatsService.this.mLock) {
                Handler handler = BatteryStatsService.this.mHandler;
                final BatteryStatsImpl batteryStatsImpl = BatteryStatsService.this.mStats;
                Objects.requireNonNull(batteryStatsImpl);
                handler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.am.BatteryStatsService$LocalService$$ExternalSyntheticLambda0
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                        batteryStatsImpl.noteBinderCallStats(((Integer) obj).intValue(), ((Long) obj2).longValue(), (Collection) obj3, ((Long) obj4).longValue(), ((Long) obj5).longValue());
                    }
                }, Integer.valueOf(workSourceUid), Long.valueOf(incrementatCallCount), callStats, Long.valueOf(SystemClock.elapsedRealtime()), Long.valueOf(SystemClock.uptimeMillis())));
            }
        }

        @Override // android.os.BatteryStatsInternal
        public void noteBinderThreadNativeIds(int[] binderThreadNativeTids) {
            synchronized (BatteryStatsService.this.mLock) {
                BatteryStatsService.this.mStats.noteBinderThreadNativeIds(binderThreadNativeTids);
            }
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mLock) {
        }
        synchronized (this.mStats) {
        }
    }

    private static void awaitUninterruptibly(Future<?> future) {
        while (true) {
            try {
                future.get();
                return;
            } catch (InterruptedException e) {
            } catch (ExecutionException e2) {
                return;
            }
        }
    }

    private void syncStats(String reason, int flags) {
        awaitUninterruptibly(this.mWorker.scheduleSync(reason, flags));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void awaitCompletion() {
        final CountDownLatch latch = new CountDownLatch(1);
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
    }

    public void initPowerManagement() {
        PowerManagerInternal powerMgr = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        powerMgr.registerLowPowerModeObserver(this);
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLockedInit(powerMgr.getLowPowerState(9).batterySaverEnabled, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        }
        new WakeupReasonThread().start();
    }

    public void shutdown() {
        Slog.w("BatteryStats", "Writing battery stats before shutdown...");
        awaitCompletion();
        syncStats("shutdown", 63);
        synchronized (this.mStats) {
            this.mStats.shutdownLocked();
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.shutdown();
            }
        }
        this.mWorker.shutdown();
    }

    public static IBatteryStats getService() {
        IBatteryStats iBatteryStats = sService;
        if (iBatteryStats != null) {
            return iBatteryStats;
        }
        IBinder b = ServiceManager.getService("batterystats");
        IBatteryStats asInterface = asInterface(b);
        sService = asInterface;
        return asInterface;
    }

    public int getServiceType() {
        return 9;
    }

    public void onLowPowerModeChanged(final PowerSaveState result) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda87
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1323xeb9afe0f(result, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onLowPowerModeChanged$1$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1323xeb9afe0f(PowerSaveState result, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(result.batterySaverEnabled, elapsedRealtime, uptime);
        }
    }

    public BatteryStatsImpl getActiveStatistics() {
        return this.mStats;
    }

    public void scheduleWriteToDisk() {
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1328xd668bf0f();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleWriteToDisk$2$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1328xd668bf0f() {
        this.mWorker.scheduleWrite();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUid(final int uid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda38
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1326lambda$removeUid$3$comandroidserveramBatteryStatsService(uid, elapsedRealtime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeUid$3$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1326lambda$removeUid$3$comandroidserveramBatteryStatsService(int uid, long elapsedRealtime) {
        synchronized (this.mStats) {
            this.mStats.removeUidStatsLocked(uid, elapsedRealtime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupUser(final int userId) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda72
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1322lambda$onCleanupUser$4$comandroidserveramBatteryStatsService(userId, elapsedRealtime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCleanupUser$4$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1322lambda$onCleanupUser$4$comandroidserveramBatteryStatsService(int userId, long elapsedRealtime) {
        synchronized (this.mStats) {
            this.mStats.onCleanupUserLocked(userId, elapsedRealtime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(final int userId) {
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda99
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1324lambda$onUserRemoved$5$comandroidserveramBatteryStatsService(userId);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserRemoved$5$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1324lambda$onUserRemoved$5$comandroidserveramBatteryStatsService(int userId) {
        synchronized (this.mStats) {
            this.mStats.onUserRemovedLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addIsolatedUid(final int isolatedUid, final int appUid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1229x8e971a3(isolatedUid, appUid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addIsolatedUid$6$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1229x8e971a3(int isolatedUid, int appUid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.addIsolatedUidLocked(isolatedUid, appUid, elapsedRealtime, uptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeIsolatedUid(final int isolatedUid, final int appUid) {
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda75
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1325xefd8e207(isolatedUid, appUid);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeIsolatedUid$7$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1325xefd8e207(int isolatedUid, int appUid) {
        synchronized (this.mStats) {
            this.mStats.scheduleRemoveIsolatedUidLocked(isolatedUid, appUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcessStart(final String name, final int uid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda56
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1273x5cc094(name, uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(28, uid, name, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteProcessStart$8$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1273x5cc094(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteProcessStartLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcessCrash(final String name, final int uid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1270xf35dbb1a(name, uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(28, uid, name, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteProcessCrash$9$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1270xf35dbb1a(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteProcessCrashLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    public void noteProcessAnr(final String name, final int uid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda84
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1269xd7a2711c(name, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteProcessAnr$10$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1269xd7a2711c(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteProcessAnrLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcessFinish(final String name, final int uid) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda35
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1272x7ce4322f(name, uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(28, uid, name, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteProcessFinish$11$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1272x7ce4322f(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteProcessFinishLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteUidProcessState(final int uid, final int state) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda52
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1298xdc964340(uid, state, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteUidProcessState$12$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1298xdc964340(int uid, int state, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteUidProcessStateLocked(uid, state, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteUidProcessState(uid, state);
            }
        }
    }

    public List<BatteryUsageStats> getBatteryUsageStats(List<BatteryUsageStatsQuery> queries) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        awaitCompletion();
        if (this.mBatteryUsageStatsProvider.shouldUpdateStats(queries, this.mWorker.getLastCollectionTimeStamp())) {
            syncStats("get-stats", 63);
        }
        return this.mBatteryUsageStatsProvider.getBatteryUsageStats(queries);
    }

    public byte[] getStatistics() {
        this.mContext.enforceCallingPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        awaitCompletion();
        syncStats("get-stats", 63);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        return data;
    }

    public ParcelFileDescriptor getStatisticsStream(boolean forceUpdate) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        if (forceUpdate) {
            awaitCompletion();
            syncStats("get-stats", 63);
        }
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        try {
            return ParcelFileDescriptor.fromData(data, "battery-stats");
        } catch (IOException e) {
            Slog.w(TAG, "Unable to create shared memory", e);
            return null;
        }
    }

    private void registerStatsCallbacks() {
        StatsManager statsManager = (StatsManager) this.mContext.getSystemService(StatsManager.class);
        StatsPullAtomCallbackImpl pullAtomCallback = new StatsPullAtomCallbackImpl();
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), pullAtomCallback);
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET_USING_POWER_PROFILE_MODEL, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), pullAtomCallback);
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_USAGE_STATS_BEFORE_RESET, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), pullAtomCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            BatteryUsageStats bus;
            switch (atomTag) {
                case FrameworkStatsLog.BATTERY_USAGE_STATS_BEFORE_RESET /* 10111 */:
                    long sessionStart = BatteryStatsService.this.mBatteryUsageStatsStore.getLastBatteryUsageStatsBeforeResetAtomPullTimestamp();
                    long sessionEnd = BatteryStatsService.this.mStats.getStartClockTime();
                    BatteryUsageStatsQuery queryBeforeReset = new BatteryUsageStatsQuery.Builder().includeProcessStateData().includeVirtualUids().aggregateSnapshots(sessionStart, sessionEnd).build();
                    BatteryUsageStats bus2 = BatteryStatsService.this.getBatteryUsageStats(List.of(queryBeforeReset)).get(0);
                    BatteryStatsService.this.mBatteryUsageStatsStore.setLastBatteryUsageStatsBeforeResetAtomPullTimestamp(sessionEnd);
                    bus = bus2;
                    break;
                case FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET /* 10112 */:
                    BatteryUsageStatsQuery querySinceReset = new BatteryUsageStatsQuery.Builder().includeProcessStateData().includeVirtualUids().build();
                    bus = BatteryStatsService.this.getBatteryUsageStats(List.of(querySinceReset)).get(0);
                    break;
                case FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET_USING_POWER_PROFILE_MODEL /* 10113 */:
                    BatteryUsageStatsQuery queryPowerProfile = new BatteryUsageStatsQuery.Builder().includeProcessStateData().includeVirtualUids().powerProfileModeledOnly().build();
                    bus = BatteryStatsService.this.getBatteryUsageStats(List.of(queryPowerProfile)).get(0);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown tagId=" + atomTag);
            }
            byte[] statsProto = bus.getStatsProto();
            data.add(FrameworkStatsLog.buildStatsEvent(atomTag, statsProto));
            return 0;
        }
    }

    public boolean isCharging() {
        boolean isCharging;
        synchronized (this.mStats) {
            isCharging = this.mStats.isCharging();
        }
        return isCharging;
    }

    public long computeBatteryTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public long computeChargeTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeChargeTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public void noteEvent(final int code, final String name, final int uid) {
        enforceCallingPermission();
        if (name == null) {
            Slog.wtfStack(TAG, "noteEvent called with null name. code = " + code);
            return;
        }
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda103
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1241lambda$noteEvent$13$comandroidserveramBatteryStatsService(code, name, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteEvent$13$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1241lambda$noteEvent$13$comandroidserveramBatteryStatsService(int code, String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteEventLocked(code, name, uid, elapsedRealtime, uptime);
        }
    }

    public void noteSyncStart(final String name, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda40
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1297x69aa0b6d(name, uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(7, uid, (String) null, name, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteSyncStart$14$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1297x69aa0b6d(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteSyncStartLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    public void noteSyncFinish(final String name, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda50
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1296x6fa3c7c9(name, uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(7, uid, (String) null, name, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteSyncFinish$15$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1296x6fa3c7c9(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteSyncFinishLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    public void noteJobStart(final String name, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1252lambda$noteJobStart$16$comandroidserveramBatteryStatsService(name, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteJobStart$16$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1252lambda$noteJobStart$16$comandroidserveramBatteryStatsService(String name, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteJobStartLocked(name, uid, elapsedRealtime, uptime);
        }
    }

    public void noteJobFinish(final String name, final int uid, final int stopReason) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1251x36d19627(name, uid, stopReason, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteJobFinish$17$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1251x36d19627(String name, int uid, int stopReason, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteJobFinishLocked(name, uid, stopReason, elapsedRealtime, uptime);
        }
    }

    void noteJobsDeferred(final int uid, final int numDeferred, final long sinceLast) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1253x3374be5(uid, numDeferred, sinceLast, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteJobsDeferred$18$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1253x3374be5(int uid, int numDeferred, long sinceLast, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteJobsDeferredLocked(uid, numDeferred, sinceLast, elapsedRealtime, uptime);
        }
    }

    public void noteWakupAlarm(final String name, final int uid, WorkSource workSource, final String tag) {
        enforceCallingPermission();
        final WorkSource localWs = workSource != null ? new WorkSource(workSource) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda63
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1303x77b606c6(name, uid, localWs, tag, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWakupAlarm$19$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1303x77b606c6(String name, int uid, WorkSource localWs, String tag, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWakupAlarmLocked(name, uid, localWs, tag, elapsedRealtime, uptime);
        }
    }

    public void noteAlarmStart(final String name, WorkSource workSource, final int uid) {
        enforceCallingPermission();
        final WorkSource localWs = workSource != null ? new WorkSource(workSource) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda68
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1231xf14deec0(name, localWs, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteAlarmStart$20$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1231xf14deec0(String name, WorkSource localWs, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteAlarmStartLocked(name, localWs, uid, elapsedRealtime, uptime);
        }
    }

    public void noteAlarmFinish(final String name, WorkSource workSource, final int uid) {
        enforceCallingPermission();
        final WorkSource localWs = workSource != null ? new WorkSource(workSource) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda66
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1230x7b73e02c(name, localWs, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteAlarmFinish$21$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1230x7b73e02c(String name, WorkSource localWs, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteAlarmFinishLocked(name, localWs, uid, elapsedRealtime, uptime);
        }
    }

    public void noteStartWakelock(final int uid, final int pid, final String name, final String historyName, final int type, final boolean unimportantForLogging) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1288x7ad58f40(uid, pid, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartWakelock$22$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1288x7ad58f40(int uid, int pid, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStartWakeLocked(uid, pid, (WorkSource.WorkChain) null, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
        }
    }

    public void noteStopWakelock(final int uid, final int pid, final String name, final String historyName, final int type) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1294x17dbc63(uid, pid, name, historyName, type, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopWakelock$23$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1294x17dbc63(int uid, int pid, String name, String historyName, int type, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStopWakeLocked(uid, pid, (WorkSource.WorkChain) null, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    public void noteStartWakelockFromSource(WorkSource ws, final int pid, final String name, final String historyName, final int type, final boolean unimportantForLogging) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda60
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1289x9725c2dd(localWs, pid, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartWakelockFromSource$24$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1289x9725c2dd(WorkSource localWs, int pid, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStartWakeFromSourceLocked(localWs, pid, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
        }
    }

    public void noteChangeWakelockFromSource(WorkSource ws, final int pid, final String name, final String historyName, final int type, WorkSource newWs, final int newPid, final String newName, final String newHistoryName, final int newType, final boolean newUnimportantForLogging) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        final WorkSource localNewWs = newWs != null ? new WorkSource(newWs) : null;
        synchronized (this.mLock) {
            try {
                try {
                    final long elapsedRealtime = SystemClock.elapsedRealtime();
                    final long uptime = SystemClock.uptimeMillis();
                    this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda61
                        @Override // java.lang.Runnable
                        public final void run() {
                            BatteryStatsService.this.m1237xd482a3d2(localWs, pid, name, historyName, type, localNewWs, newPid, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
                        }
                    });
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteChangeWakelockFromSource$25$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1237xd482a3d2(WorkSource localWs, int pid, String name, String historyName, int type, WorkSource localNewWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteChangeWakelockFromSourceLocked(localWs, pid, name, historyName, type, localNewWs, newPid, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
        }
    }

    public void noteStopWakelockFromSource(WorkSource ws, final int pid, final String name, final String historyName, final int type) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda83
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1295xb7c64c41(localWs, pid, name, historyName, type, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopWakelockFromSource$26$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1295xb7c64c41(WorkSource localWs, int pid, String name, String historyName, int type, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStopWakeFromSourceLocked(localWs, pid, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    public void noteLongPartialWakelockStart(final String name, final String historyName, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda41
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1256xbadfde8a(name, historyName, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteLongPartialWakelockStart$27$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1256xbadfde8a(String name, String historyName, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStart(name, historyName, uid, elapsedRealtime, uptime);
        }
    }

    public void noteLongPartialWakelockStartFromSource(final String name, final String historyName, WorkSource workSource) {
        enforceCallingPermission();
        final WorkSource localWs = workSource != null ? new WorkSource(workSource) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda48
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1257x42d54126(name, historyName, localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteLongPartialWakelockStartFromSource$28$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1257x42d54126(String name, String historyName, WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStartFromSource(name, historyName, localWs, elapsedRealtime, uptime);
        }
    }

    public void noteLongPartialWakelockFinish(final String name, final String historyName, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1254xb9b47e11(name, historyName, uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteLongPartialWakelockFinish$29$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1254xb9b47e11(String name, String historyName, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinish(name, historyName, uid, elapsedRealtime, uptime);
        }
    }

    public void noteLongPartialWakelockFinishFromSource(final String name, final String historyName, WorkSource workSource) {
        enforceCallingPermission();
        final WorkSource localWs = workSource != null ? new WorkSource(workSource) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda34
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1255xd6b4f442(name, historyName, localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteLongPartialWakelockFinishFromSource$30$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1255xd6b4f442(String name, String historyName, WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinishFromSource(name, historyName, localWs, elapsedRealtime, uptime);
        }
    }

    public void noteStartSensor(final int uid, final int sensor) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda51
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1286x31d82c13(uid, sensor, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(5, uid, (String) null, sensor, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartSensor$31$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1286x31d82c13(int uid, int sensor, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStartSensorLocked(uid, sensor, elapsedRealtime, uptime);
        }
    }

    public void noteStopSensor(final int uid, final int sensor) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda47
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1292xf68089b6(uid, sensor, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(5, uid, (String) null, sensor, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopSensor$32$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1292xf68089b6(int uid, int sensor, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteStopSensorLocked(uid, sensor, elapsedRealtime, uptime);
        }
    }

    public void noteVibratorOn(final int uid, final long durationMillis) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1301x736f0e7b(uid, durationMillis, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteVibratorOn$33$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1301x736f0e7b(int uid, long durationMillis, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteVibratorOnLocked(uid, durationMillis, elapsedRealtime, uptime);
        }
    }

    public void noteVibratorOff(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda81
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1300x1f8ccbc(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteVibratorOff$34$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1300x1f8ccbc(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteVibratorOffLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteGpsChanged(final WorkSource oldWs, final WorkSource newWs) {
        enforceCallingPermission();
        final WorkSource localOldWs = oldWs != null ? new WorkSource(oldWs) : null;
        final WorkSource localNewWs = newWs != null ? new WorkSource(newWs) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda57
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1248xc6ae846b(localOldWs, localNewWs, elapsedRealtime, uptime, oldWs, newWs);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteGpsChanged$35$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1248xc6ae846b(WorkSource localOldWs, WorkSource localNewWs, long elapsedRealtime, long uptime, WorkSource oldWs, WorkSource newWs) {
        synchronized (this.mStats) {
            try {
                try {
                    this.mStats.noteGpsChangedLocked(localOldWs, localNewWs, elapsedRealtime, uptime);
                    ITranResmonitor iTranResmonitor = this.mITranResmonitor;
                    if (iTranResmonitor != null) {
                        iTranResmonitor.noteGpsChanged(oldWs, newWs);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void noteGpsSignalQuality(final int signalLevel) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda88
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1249x25eb149(signalLevel, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteGpsSignalQuality$36$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1249x25eb149(int signalLevel, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteGpsSignalQualityLocked(signalLevel, elapsedRealtime, uptime);
        }
    }

    public void noteScreenState(final int state) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            final long currentTime = System.currentTimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda104
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1279xa1f7b270(state, elapsedRealtime, uptime, currentTime);
                }
            });
        }
        FrameworkStatsLog.write(29, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteScreenState$37$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1279xa1f7b270(int state, long elapsedRealtime, long uptime, long currentTime) {
        synchronized (this.mStats) {
            try {
                try {
                    this.mStats.noteScreenStateLocked(0, state, elapsedRealtime, uptime, currentTime);
                    ITranResmonitor iTranResmonitor = this.mITranResmonitor;
                    if (iTranResmonitor != null) {
                        iTranResmonitor.noteScreenStateLocked(state);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void noteScreenBrightness(final int brightness) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1278x3bbfab9b(brightness, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(9, brightness);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteScreenBrightness$38$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1278x3bbfab9b(int brightness, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteScreenBrightnessLocked(0, brightness, elapsedRealtime, uptime);
        }
    }

    public void noteUserActivity(final int uid, final int event) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda45
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1299xf6473fdf(uid, event, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteUserActivity$39$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1299xf6473fdf(int uid, int event, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteUserActivityLocked(uid, event, elapsedRealtime, uptime);
        }
    }

    public void noteWakeUp(final String reason, final int reasonUid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1302lambda$noteWakeUp$40$comandroidserveramBatteryStatsService(reason, reasonUid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWakeUp$40$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1302lambda$noteWakeUp$40$comandroidserveramBatteryStatsService(String reason, int reasonUid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWakeUpLocked(reason, reasonUid, elapsedRealtime, uptime);
        }
    }

    public void noteInteractive(final boolean interactive) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1250xade5ff0c(interactive, elapsedRealtime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteInteractive$41$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1250xade5ff0c(boolean interactive, long elapsedRealtime) {
        synchronized (this.mStats) {
            this.mStats.noteInteractiveLocked(interactive, elapsedRealtime);
        }
    }

    public void noteConnectivityChanged(final int type, final String extra) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1238xafcb2892(type, extra, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteConnectivityChanged$42$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1238xafcb2892(int type, String extra, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteConnectivityChangedLocked(type, extra, elapsedRealtime, uptime);
        }
    }

    public void noteMobileRadioPowerState(final int powerState, final long timestampNs, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            try {
                final long elapsedRealtime = SystemClock.elapsedRealtime();
                final long uptime = SystemClock.uptimeMillis();
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda26
                    @Override // java.lang.Runnable
                    public final void run() {
                        BatteryStatsService.this.m1258x118715ab(powerState, timestampNs, uid, elapsedRealtime, uptime);
                    }
                });
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
        FrameworkStatsLog.write_non_chained(12, uid, null, powerState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteMobileRadioPowerState$43$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1258x118715ab(int powerState, long timestampNs, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            if (this.mLastPowerStateFromRadio == powerState) {
                return;
            }
            this.mLastPowerStateFromRadio = powerState;
            boolean update = this.mStats.noteMobileRadioPowerStateLocked(powerState, timestampNs, uid, elapsedRealtime, uptime);
            if (update) {
                this.mWorker.scheduleSync("modem-data", 4);
            }
        }
    }

    public void notePhoneOn() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1266lambda$notePhoneOn$44$comandroidserveramBatteryStatsService(elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePhoneOn$44$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1266lambda$notePhoneOn$44$comandroidserveramBatteryStatsService(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePhoneOnLocked(elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.notePhoneStateLocked(true);
            }
        }
    }

    public void notePhoneOff() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda65
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1265lambda$notePhoneOff$45$comandroidserveramBatteryStatsService(elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePhoneOff$45$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1265lambda$notePhoneOff$45$comandroidserveramBatteryStatsService(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePhoneOffLocked(elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.notePhoneStateLocked(false);
            }
        }
    }

    public void notePhoneSignalStrength(final SignalStrength signalStrength) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda43
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1267x13053ebc(signalStrength, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePhoneSignalStrength$46$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1267x13053ebc(SignalStrength signalStrength, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePhoneSignalStrengthLocked(signalStrength, elapsedRealtime, uptime);
        }
    }

    public void notePhoneDataConnectionState(final int dataType, final boolean hasData, final int serviceType, final int nrFrequency) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1264xc657b8db(dataType, hasData, serviceType, nrFrequency, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePhoneDataConnectionState$47$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1264xc657b8db(int dataType, boolean hasData, int serviceType, int nrFrequency, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePhoneDataConnectionStateLocked(dataType, hasData, serviceType, nrFrequency, elapsedRealtime, uptime);
        }
    }

    public void notePhoneState(final int state) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda74
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1268x97229994(state, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePhoneState$48$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1268x97229994(int state, long elapsedRealtime, long uptime) {
        int simState = ((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class)).getSimState();
        synchronized (this.mStats) {
            this.mStats.notePhoneStateLocked(state, simState, elapsedRealtime, uptime);
        }
    }

    public void noteWifiOn() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda46
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1310lambda$noteWifiOn$49$comandroidserveramBatteryStatsService(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(113, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiOn$49$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1310lambda$noteWifiOn$49$comandroidserveramBatteryStatsService(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiOnLocked(elapsedRealtime, uptime);
        }
    }

    public void noteWifiOff() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda95
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1309lambda$noteWifiOff$50$comandroidserveramBatteryStatsService(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(113, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiOff$50$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1309lambda$noteWifiOff$50$comandroidserveramBatteryStatsService(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiOffLocked(elapsedRealtime, uptime);
        }
    }

    public void noteStartAudio(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda29
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1284x9f47127b(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(23, uid, null, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartAudio$51$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1284x9f47127b(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteAudioOnLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteAudioStateLocked(true);
            }
        }
    }

    public void noteStopAudio(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda78
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1290x72f72e1a(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(23, uid, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopAudio$52$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1290x72f72e1a(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteAudioOffLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteAudioStateLocked(false);
            }
        }
    }

    public void noteStartVideo(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda79
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1287x6c58bf78(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(24, uid, null, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartVideo$53$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1287x6c58bf78(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteVideoOnLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteVideoStateLocked(true);
            }
        }
    }

    public void noteStopVideo(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda31
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1293x4008db17(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(24, uid, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopVideo$54$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1293x4008db17(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteVideoOffLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteVideoStateLocked(false);
            }
        }
    }

    public void noteResetAudio() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1274x1d7cb12c(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(23, -1, null, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteResetAudio$55$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1274x1d7cb12c(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteResetAudioLocked(elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteAudioStateLocked(false);
            }
        }
    }

    public void noteResetVideo() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda97
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1277x54cef68(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(24, -1, null, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteResetVideo$56$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1277x54cef68(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteResetVideoLocked(elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteVideoStateLocked(false);
            }
        }
    }

    public void noteFlashlightOn(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda86
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1243xcfe36350(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(26, uid, null, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFlashlightOn$57$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1243xcfe36350(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOnLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteFlashlightOff(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda70
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1242xdfa75cb(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(26, uid, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFlashlightOff$58$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1242xdfa75cb(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOffLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteStartCamera(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda91
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1285xc943974e(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(25, uid, null, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStartCamera$59$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1285xc943974e(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteCameraOnLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteCameraStateLocked(true);
            }
        }
    }

    public void noteStopCamera(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda89
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1291x5c4a0ac6(uid, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(25, uid, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteStopCamera$60$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1291x5c4a0ac6(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteCameraOffLocked(uid, elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteCameraStateLocked(false);
            }
        }
    }

    public void noteResetCamera() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda101
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1275x9ef11c98(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(25, -1, null, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteResetCamera$61$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1275x9ef11c98(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteResetCameraLocked(elapsedRealtime, uptime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteCameraStateLocked(false);
            }
        }
    }

    public void noteResetFlashlight() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda80
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1276x360720f8(elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write_non_chained(26, -1, null, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteResetFlashlight$62$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1276x360720f8(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteResetFlashlightLocked(elapsedRealtime, uptime);
        }
    }

    public void noteWifiRadioPowerState(final int powerState, final long tsNanos, final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            try {
                final long elapsedRealtime = SystemClock.elapsedRealtime();
                final long uptime = SystemClock.uptimeMillis();
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda71
                    @Override // java.lang.Runnable
                    public final void run() {
                        BatteryStatsService.this.m1311xaea6981c(powerState, tsNanos, uid, elapsedRealtime, uptime);
                    }
                });
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
        FrameworkStatsLog.write_non_chained(13, uid, null, powerState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiRadioPowerState$63$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1311xaea6981c(int powerState, long tsNanos, int uid, long elapsedRealtime, long uptime) {
        String type;
        synchronized (this.mStats) {
            if (this.mLastPowerStateFromWifi == powerState) {
                return;
            }
            this.mLastPowerStateFromWifi = powerState;
            if (this.mStats.isOnBattery()) {
                if (powerState != 3 && powerState != 2) {
                    type = "inactive";
                    this.mWorker.scheduleSync("wifi-data: " + type, 2);
                }
                type = DomainVerificationPersistence.TAG_ACTIVE;
                this.mWorker.scheduleSync("wifi-data: " + type, 2);
            }
            this.mStats.noteWifiRadioPowerState(powerState, tsNanos, uid, elapsedRealtime, uptime);
        }
    }

    public void noteWifiRunning(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda44
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1313xa5394d05(localWs, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(114, ws, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiRunning$64$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1313xa5394d05(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiRunningChanged(WorkSource oldWs, WorkSource newWs) {
        enforceCallingPermission();
        final WorkSource localOldWs = oldWs != null ? new WorkSource(oldWs) : null;
        final WorkSource localNewWs = newWs != null ? new WorkSource(newWs) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1314x36c57bc8(localOldWs, localNewWs, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(114, newWs, 1);
        FrameworkStatsLog.write(114, oldWs, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiRunningChanged$65$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1314x36c57bc8(WorkSource localOldWs, WorkSource localNewWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningChangedLocked(localOldWs, localNewWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiStopped(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : ws;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda105
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1320x919eed39(localWs, elapsedRealtime, uptime);
                }
            });
        }
        FrameworkStatsLog.write(114, ws, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiStopped$66$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1320x919eed39(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiStoppedLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiState(final int wifiState, final String accessPoint) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda98
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1319xb04028b6(wifiState, accessPoint, elapsedRealtime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiState$67$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1319xb04028b6(int wifiState, String accessPoint, long elapsedRealtime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiStateLocked(wifiState, accessPoint, elapsedRealtime);
            ITranResmonitor iTranResmonitor = this.mITranResmonitor;
            if (iTranResmonitor != null) {
                iTranResmonitor.noteWifiState(wifiState);
            }
        }
    }

    public void noteWifiSupplicantStateChanged(final int supplState, final boolean failedAuth) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1321x66715f1a(supplState, failedAuth, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiSupplicantStateChanged$68$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1321x66715f1a(int supplState, boolean failedAuth, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiSupplicantStateChangedLocked(supplState, failedAuth, elapsedRealtime, uptime);
        }
    }

    public void noteWifiRssiChanged(final int newRssi) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda62
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1312x9014e8ec(newRssi, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiRssiChanged$69$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1312x9014e8ec(int newRssi, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiRssiChangedLocked(newRssi, elapsedRealtime, uptime);
        }
    }

    public void noteFullWifiLockAcquired(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1244xe9f7736f(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFullWifiLockAcquired$70$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1244xe9f7736f(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteFullWifiLockReleased(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda82
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1246x5cfbbba1(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFullWifiLockReleased$71$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1246x5cfbbba1(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteWifiScanStarted(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda90
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1315x5bde857d(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiScanStarted$72$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1315x5bde857d(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteWifiScanStopped(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda54
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1317x92294e72(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiScanStopped$73$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1317x92294e72(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteWifiMulticastEnabled(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1308x2d6dfbb2(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiMulticastEnabled$74$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1308x2d6dfbb2(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastEnabledLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteWifiMulticastDisabled(final int uid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1307xee8a8970(uid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiMulticastDisabled$75$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1307xee8a8970(int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastDisabledLocked(uid, elapsedRealtime, uptime);
        }
    }

    public void noteFullWifiLockAcquiredFromSource(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda25
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1245x87444e90(localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFullWifiLockAcquiredFromSource$76$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1245x87444e90(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredFromSourceLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteFullWifiLockReleasedFromSource(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda102
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1247xc4403d82(localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteFullWifiLockReleasedFromSource$77$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1247xc4403d82(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedFromSourceLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiScanStartedFromSource(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda85
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1316xed8c219e(localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiScanStartedFromSource$78$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1316xed8c219e(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedFromSourceLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda49
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1318xd1e2b993(localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiScanStoppedFromSource$79$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1318xd1e2b993(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedFromSourceLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteWifiBatchedScanStartedFromSource(WorkSource ws, final int csph) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda55
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1304xca126ca6(localWs, csph, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiBatchedScanStartedFromSource$80$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1304xca126ca6(WorkSource localWs, int csph, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStartedFromSourceLocked(localWs, csph, elapsedRealtime, uptime);
        }
    }

    public void noteWifiBatchedScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda93
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1305xae69049b(localWs, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiBatchedScanStoppedFromSource$81$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1305xae69049b(WorkSource localWs, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStoppedFromSourceLocked(localWs, elapsedRealtime, uptime);
        }
    }

    public void noteNetworkInterfaceForTransports(final String iface, final int[] transportTypes) {
        PermissionUtils.enforceNetworkStackPermission(this.mContext);
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda96
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1260x9eeedf03(iface, transportTypes);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteNetworkInterfaceForTransports$82$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1260x9eeedf03(String iface, int[] transportTypes) {
        this.mStats.noteNetworkInterfaceForTransports(iface, transportTypes);
    }

    public void noteNetworkStatsEnabled() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda92
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1261x15ac39bc();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteNetworkStatsEnabled$83$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1261x15ac39bc() {
        this.mWorker.scheduleSync("network-stats-enabled", 6);
    }

    public void noteDeviceIdleMode(final int mode, final String activeReason, final int activeUid) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1240xd01e7f82(mode, activeReason, activeUid, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteDeviceIdleMode$84$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1240xd01e7f82(int mode, String activeReason, int activeUid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteDeviceIdleModeLocked(mode, activeReason, activeUid, elapsedRealtime, uptime);
        }
    }

    public void notePackageInstalled(final String pkgName, final long versionCode) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda53
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1262xb96f139c(pkgName, versionCode, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePackageInstalled$85$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1262xb96f139c(String pkgName, long versionCode, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePackageInstalledLocked(pkgName, versionCode, elapsedRealtime, uptime);
        }
    }

    public void notePackageUninstalled(final String pkgName) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1263x168f0276(pkgName, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notePackageUninstalled$86$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1263x168f0276(String pkgName, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.notePackageUninstalledLocked(pkgName, elapsedRealtime, uptime);
        }
    }

    public void noteBluetoothOn(int uid, int reason, String packageName) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.BLUETOOTH_CONNECT", Binder.getCallingPid(), uid, null);
        }
        FrameworkStatsLog.write_non_chained(67, uid, (String) null, 1, reason, packageName);
    }

    public void noteBluetoothOff(int uid, int reason, String packageName) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.BLUETOOTH_CONNECT", Binder.getCallingPid(), uid, null);
        }
        FrameworkStatsLog.write_non_chained(67, uid, (String) null, 2, reason, packageName);
    }

    public void noteBleScanStarted(WorkSource ws, final boolean isUnoptimized) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1234xb6abe269(localWs, isUnoptimized, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteBleScanStarted$87$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1234xb6abe269(WorkSource localWs, boolean isUnoptimized, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStartedFromSourceLocked(localWs, isUnoptimized, elapsedRealtime, uptime);
        }
    }

    public void noteBleScanStopped(WorkSource ws, final boolean isUnoptimized) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda67
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1235xecf6ab5e(localWs, isUnoptimized, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteBleScanStopped$88$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1235xecf6ab5e(WorkSource localWs, boolean isUnoptimized, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStoppedFromSourceLocked(localWs, isUnoptimized, elapsedRealtime, uptime);
        }
    }

    public void noteBleScanReset() {
        enforceCallingPermission();
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda76
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1232xabfae01d(elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteBleScanReset$89$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1232xabfae01d(long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteResetBluetoothScanLocked(elapsedRealtime, uptime);
        }
    }

    public void noteBleScanResults(WorkSource ws, final int numNewResults) {
        enforceCallingPermission();
        final WorkSource localWs = ws != null ? new WorkSource(ws) : null;
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda73
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1233x2de32c(localWs, numNewResults, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteBleScanResults$90$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1233x2de32c(WorkSource localWs, int numNewResults, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanResultsFromSourceLocked(localWs, numNewResults, elapsedRealtime, uptime);
        }
    }

    public void noteWifiControllerActivity(final WifiActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            Slog.e(TAG, "invalid wifi data given: " + info);
            return;
        }
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            final NetworkStatsManager networkStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1306xb910a3cb(info, elapsedRealtime, uptime, networkStatsManager);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteWifiControllerActivity$91$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1306xb910a3cb(WifiActivityEnergyInfo info, long elapsedRealtime, long uptime, NetworkStatsManager networkStatsManager) {
        this.mStats.updateWifiState(info, -1L, elapsedRealtime, uptime, networkStatsManager);
    }

    public void noteBluetoothControllerActivity(final BluetoothActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            Slog.e(TAG, "invalid bluetooth data given: " + info);
            return;
        }
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda69
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1236x6334ccd1(info, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteBluetoothControllerActivity$92$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1236x6334ccd1(BluetoothActivityEnergyInfo info, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.updateBluetoothStateLocked(info, -1L, elapsedRealtime, uptime);
        }
    }

    public void noteModemControllerActivity(final ModemActivityInfo info) {
        enforceCallingPermission();
        if (info == null) {
            Slog.e(TAG, "invalid modem data given: " + info);
            return;
        }
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            final NetworkStatsManager networkStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda77
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1259x60b02316(info, elapsedRealtime, uptime, networkStatsManager);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteModemControllerActivity$93$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1259x60b02316(ModemActivityInfo info, long elapsedRealtime, long uptime, NetworkStatsManager networkStatsManager) {
        this.mStats.noteModemControllerActivity(info, -1L, elapsedRealtime, uptime, networkStatsManager);
    }

    public boolean isOnBattery() {
        return this.mStats.isOnBattery();
    }

    public void setBatteryState(final int status, final int health, final int plugType, final int level, final int temp, final int volt, final int chargeUAh, final int chargeFullUAh, final long chargeTimeToFullSeconds) {
        enforceCallingPermission();
        synchronized (this.mLock) {
            try {
                try {
                    final long elapsedRealtime = SystemClock.elapsedRealtime();
                    final long uptime = SystemClock.uptimeMillis();
                    final long currentTime = System.currentTimeMillis();
                    this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            BatteryStatsService.this.m1331xe0bfc6f6(plugType, status, health, level, temp, volt, chargeUAh, chargeFullUAh, chargeTimeToFullSeconds, elapsedRealtime, uptime, currentTime);
                        }
                    });
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setBatteryState$96$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1331xe0bfc6f6(final int plugType, final int status, final int health, final int level, final int temp, final int volt, final int chargeUAh, final int chargeFullUAh, final long chargeTimeToFullSeconds, final long elapsedRealtime, final long uptime, final long currentTime) {
        this.mWorker.scheduleRunnable(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda100
            @Override // java.lang.Runnable
            public final void run() {
                BatteryStatsService.this.m1330xfb7e5835(plugType, status, health, level, temp, volt, chargeUAh, chargeFullUAh, chargeTimeToFullSeconds, elapsedRealtime, uptime, currentTime);
            }
        });
        ITranResmonitor iTranResmonitor = this.mITranResmonitor;
        if (iTranResmonitor != null) {
            iTranResmonitor.noteBatteryState(plugType, level, temp, volt, chargeUAh);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setBatteryState$95$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1330xfb7e5835(final int plugType, final int status, final int health, final int level, final int temp, final int volt, final int chargeUAh, final int chargeFullUAh, final long chargeTimeToFullSeconds, final long elapsedRealtime, final long uptime, final long currentTime) {
        synchronized (this.mStats) {
            boolean onBattery = BatteryStatsImpl.isOnBattery(plugType, status);
            if (this.mStats.isOnBattery() == onBattery) {
                this.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh, chargeTimeToFullSeconds, elapsedRealtime, uptime, currentTime);
                return;
            }
            this.mWorker.scheduleSync("battery-state", 63);
            this.mWorker.scheduleRunnable(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1329x163ce974(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh, chargeTimeToFullSeconds, elapsedRealtime, uptime, currentTime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setBatteryState$94$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1329x163ce974(int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh, long chargeTimeToFullSeconds, long elapsedRealtime, long uptime, long currentTime) {
        synchronized (this.mStats) {
            this.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh, chargeTimeToFullSeconds, elapsedRealtime, uptime, currentTime);
        }
    }

    public long getAwakeTimeBattery() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimeBattery();
    }

    public long getAwakeTimePlugged() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimePlugged();
    }

    public void enforceCallingPermission() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class WakeupReasonThread extends Thread {
        private static final int MAX_REASON_SIZE = 512;
        private CharsetDecoder mDecoder;
        private CharBuffer mUtf16Buffer;
        private ByteBuffer mUtf8Buffer;

        WakeupReasonThread() {
            super("BatteryStats_wakeupReason");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Process.setThreadPriority(-2);
            this.mDecoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
            this.mUtf8Buffer = ByteBuffer.allocateDirect(512);
            this.mUtf16Buffer = CharBuffer.allocate(512);
            while (true) {
                try {
                    String reason = waitWakeup();
                    if (reason != null) {
                        BatteryStatsService.this.awaitCompletion();
                        synchronized (BatteryStatsService.this.mStats) {
                            BatteryStatsService.this.mStats.noteWakeupReasonLocked(reason, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
                        }
                    } else {
                        return;
                    }
                } catch (RuntimeException e) {
                    Slog.e(BatteryStatsService.TAG, "Failure reading wakeup reasons", e);
                    return;
                }
            }
        }

        private String waitWakeup() {
            this.mUtf8Buffer.clear();
            this.mUtf16Buffer.clear();
            this.mDecoder.reset();
            int bytesWritten = BatteryStatsService.nativeWaitWakeup(this.mUtf8Buffer);
            if (bytesWritten < 0) {
                return null;
            }
            if (bytesWritten == 0) {
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            this.mUtf8Buffer.limit(bytesWritten);
            this.mDecoder.decode(this.mUtf8Buffer, this.mUtf16Buffer, true);
            this.mUtf16Buffer.flip();
            return this.mUtf16Buffer.toString();
        }
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("Battery stats (batterystats) dump options:");
        pw.println("  [--checkin] [--proto] [--history] [--history-start] [--charged] [-c]");
        pw.println("  [--daily] [--reset] [--reset-all] [--write] [--new-daily] [--read-daily]");
        pw.println("  [-h] [<package.name>]");
        pw.println("  --checkin: generate output for a checkin report; will write (and clear) the");
        pw.println("             last old completed stats when they had been reset.");
        pw.println("  -c: write the current stats in checkin format.");
        pw.println("  --proto: write the current aggregate stats (without history) in proto format.");
        pw.println("  --history: show only history data.");
        pw.println("  --history-start <num>: show only history data starting at given time offset.");
        pw.println("  --history-create-events <num>: create <num> of battery history events.");
        pw.println("  --charged: only output data since last charged.");
        pw.println("  --daily: only output full daily data.");
        pw.println("  --reset: reset the stats, clearing all current data.");
        pw.println("  --reset-all: reset the stats, clearing all current and past data.");
        pw.println("  --write: force write current collected stats to disk.");
        pw.println("  --new-daily: immediately create and write new daily stats record.");
        pw.println("  --read-daily: read-load last written daily stats.");
        pw.println("  --settings: dump the settings key/values related to batterystats");
        pw.println("  --cpu: dump cpu stats for debugging purpose");
        pw.println("  --power-profile: dump the power profile constants");
        pw.println("  <package.name>: optional name of package to filter output by.");
        pw.println("  -h: print this help text.");
        pw.println("Battery stats (batterystats) commands:");
        pw.println("  enable|disable <option>");
        pw.println("    Enable or disable a running option.  Option state is not saved across boots.");
        pw.println("    Options are:");
        pw.println("      full-history: include additional detailed events in battery history:");
        pw.println("          wake_lock_in, alarms and proc events");
        pw.println("      no-auto-reset: don't automatically reset stats when unplugged");
        pw.println("      pretend-screen-off: pretend the screen is off, even if screen state changes");
    }

    private void dumpSettings(PrintWriter pw) {
        awaitCompletion();
        synchronized (this.mStats) {
            this.mStats.dumpConstantsLocked(pw);
        }
    }

    private void dumpCpuStats(PrintWriter pw) {
        awaitCompletion();
        synchronized (this.mStats) {
            this.mStats.dumpCpuStatsLocked(pw);
        }
    }

    private void dumpMeasuredEnergyStats(PrintWriter pw) {
        awaitCompletion();
        syncStats("dump", 63);
        synchronized (this.mStats) {
            this.mStats.dumpMeasuredEnergyStatsLocked(pw);
        }
    }

    private void dumpPowerProfile(PrintWriter pw) {
        synchronized (this.mStats) {
            this.mStats.dumpPowerProfileLocked(pw);
        }
    }

    private int doEnableOrDisable(PrintWriter pw, int i, String[] args, boolean enable) {
        int i2 = i + 1;
        if (i2 >= args.length) {
            pw.println("Missing option argument for " + (enable ? "--enable" : "--disable"));
            dumpHelp(pw);
            return -1;
        }
        if ("full-wake-history".equals(args[i2]) || "full-history".equals(args[i2])) {
            awaitCompletion();
            synchronized (this.mStats) {
                this.mStats.setRecordAllHistoryLocked(enable);
            }
        } else if ("no-auto-reset".equals(args[i2])) {
            awaitCompletion();
            synchronized (this.mStats) {
                this.mStats.setNoAutoReset(enable);
            }
        } else if ("pretend-screen-off".equals(args[i2])) {
            awaitCompletion();
            synchronized (this.mStats) {
                this.mStats.setPretendScreenOff(enable);
            }
        } else {
            pw.println("Unknown enable/disable option: " + args[i2]);
            dumpHelp(pw);
            return -1;
        }
        return i2;
    }

    /* JADX WARN: Incorrect condition in loop: B:9:0x0038 */
    /* JADX WARN: Removed duplicated region for block: B:274:0x046e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        boolean useCheckinFormat;
        boolean toProto;
        boolean noOutput;
        boolean writeData;
        long historyStart;
        int reqUid;
        int flags;
        boolean reqUid2;
        int flags2;
        BatteryStatsImpl batteryStatsImpl;
        BatteryStatsImpl batteryStatsImpl2;
        AtomicFile atomicFile;
        int i;
        int i2;
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            return;
        }
        int reqUid3 = -1;
        if (args != null) {
            flags = 0;
            boolean useCheckinFormat2 = false;
            boolean isRealCheckin = false;
            boolean isRealCheckin2 = false;
            boolean writeData2 = false;
            boolean writeData3 = false;
            int i3 = 0;
            historyStart = -1;
            while (i3 < flags) {
                String arg = args[i3];
                if ("--checkin".equals(arg)) {
                    useCheckinFormat2 = true;
                    isRealCheckin2 = true;
                } else if ("--history".equals(arg)) {
                    flags |= 8;
                } else if ("--history-start".equals(arg)) {
                    flags |= 8;
                    i3++;
                    if (i3 >= args.length) {
                        pw.println("Missing time argument for --history-since");
                        dumpHelp(pw);
                        return;
                    }
                    historyStart = ParseUtils.parseLong(args[i3], 0L);
                    writeData3 = true;
                } else if ("--history-create-events".equals(arg)) {
                    int i4 = i3 + 1;
                    if (i4 >= args.length) {
                        pw.println("Missing events argument for --history-create-events");
                        dumpHelp(pw);
                        return;
                    }
                    boolean useCheckinFormat3 = useCheckinFormat2;
                    boolean toProto2 = isRealCheckin;
                    long events = ParseUtils.parseLong(args[i4], 0L);
                    awaitCompletion();
                    synchronized (this.mStats) {
                        this.mStats.createFakeHistoryEvents(events);
                        pw.println("Battery history create events started.");
                        writeData2 = true;
                    }
                    i3 = i4;
                    isRealCheckin = toProto2;
                    useCheckinFormat2 = useCheckinFormat3;
                } else {
                    boolean useCheckinFormat4 = useCheckinFormat2;
                    boolean toProto3 = isRealCheckin;
                    if ("-c".equals(arg)) {
                        useCheckinFormat2 = true;
                        flags |= 16;
                        isRealCheckin = toProto3;
                    } else if ("--proto".equals(arg)) {
                        isRealCheckin = true;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--charged".equals(arg)) {
                        flags |= 2;
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--daily".equals(arg)) {
                        flags |= 4;
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--reset-all".equals(arg)) {
                        awaitCompletion();
                        synchronized (this.mStats) {
                            this.mStats.resetAllStatsCmdLocked();
                            this.mBatteryUsageStatsStore.removeAllSnapshots();
                            pw.println("Battery stats and history reset.");
                            writeData2 = true;
                        }
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--reset".equals(arg)) {
                        awaitCompletion();
                        synchronized (this.mStats) {
                            this.mStats.resetAllStatsCmdLocked();
                            pw.println("Battery stats reset.");
                            writeData2 = true;
                        }
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--write".equals(arg)) {
                        awaitCompletion();
                        syncStats("dump", 63);
                        synchronized (this.mStats) {
                            this.mStats.writeSyncLocked();
                            pw.println("Battery stats written.");
                            writeData2 = true;
                        }
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--new-daily".equals(arg)) {
                        awaitCompletion();
                        synchronized (this.mStats) {
                            this.mStats.recordDailyStatsLocked();
                            pw.println("New daily stats written.");
                            writeData2 = true;
                        }
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--read-daily".equals(arg)) {
                        awaitCompletion();
                        synchronized (this.mStats) {
                            this.mStats.readDailyStatsLocked();
                            pw.println("Last daily stats read.");
                            writeData2 = true;
                        }
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if ("--enable".equals(arg) || "enable".equals(arg)) {
                        if (doEnableOrDisable(pw, i3, args, true) < 0) {
                            return;
                        }
                        pw.println("Enabled: " + args[i]);
                        return;
                    } else if ("--disable".equals(arg) || "disable".equals(arg)) {
                        if (doEnableOrDisable(pw, i3, args, false) < 0) {
                            return;
                        }
                        pw.println("Disabled: " + args[i2]);
                        return;
                    } else if ("-h".equals(arg)) {
                        dumpHelp(pw);
                        return;
                    } else if ("--settings".equals(arg)) {
                        dumpSettings(pw);
                        return;
                    } else if ("--cpu".equals(arg)) {
                        dumpCpuStats(pw);
                        return;
                    } else if ("--measured-energy".equals(arg)) {
                        dumpMeasuredEnergyStats(pw);
                        return;
                    } else if ("--power-profile".equals(arg)) {
                        dumpPowerProfile(pw);
                        return;
                    } else if ("-a".equals(arg)) {
                        flags |= 32;
                        isRealCheckin = toProto3;
                        useCheckinFormat2 = useCheckinFormat4;
                    } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                        pw.println("Unknown option: " + arg);
                        dumpHelp(pw);
                        return;
                    } else {
                        try {
                            reqUid3 = this.mContext.getPackageManager().getPackageUidAsUser(arg, UserHandle.getCallingUserId());
                            isRealCheckin = toProto3;
                            useCheckinFormat2 = useCheckinFormat4;
                        } catch (PackageManager.NameNotFoundException e) {
                            pw.println("Unknown package: " + arg);
                            dumpHelp(pw);
                            return;
                        }
                    }
                }
                i3++;
            }
            useCheckinFormat = useCheckinFormat2;
            toProto = isRealCheckin;
            noOutput = writeData2;
            writeData = writeData3;
            reqUid = reqUid3;
            reqUid2 = isRealCheckin2;
        } else {
            useCheckinFormat = false;
            toProto = false;
            noOutput = false;
            writeData = false;
            historyStart = -1;
            reqUid = -1;
            flags = 0;
            reqUid2 = false;
        }
        if (noOutput) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (BatteryStats.checkWifiOnly(this.mContext)) {
                flags |= 64;
            }
            awaitCompletion();
            syncStats("dump", 63);
            if (reqUid >= 0 && (flags & 10) == 0) {
                flags2 = (flags | 2) & (-17);
            } else {
                flags2 = flags;
            }
            if (toProto) {
                List<ApplicationInfo> apps = this.mContext.getPackageManager().getInstalledApplications(4325376);
                if (reqUid2) {
                    synchronized (this.mStats.mCheckinFile) {
                        if (this.mStats.mCheckinFile.exists()) {
                            try {
                                byte[] raw = this.mStats.mCheckinFile.readFully();
                                if (raw != null) {
                                    Parcel in = Parcel.obtain();
                                    in.unmarshall(raw, 0, raw.length);
                                    in.setDataPosition(0);
                                    BatteryStatsImpl checkinStats = new BatteryStatsImpl((File) null, this.mStats.mHandler, (BatteryStatsImpl.PlatformIdleStateCallback) null, (BatteryStatsImpl.MeasuredEnergyRetriever) null, this.mUserManagerUserInfoProvider);
                                    checkinStats.setPowerProfileLocked(this.mPowerProfile);
                                    checkinStats.readSummaryFromParcel(in);
                                    in.recycle();
                                    checkinStats.dumpProtoLocked(this.mContext, fd, apps, flags2, historyStart);
                                    this.mStats.mCheckinFile.delete();
                                    return;
                                }
                            } catch (ParcelFormatException | IOException e2) {
                                Slog.w(TAG, "Failure reading checkin file " + this.mStats.mCheckinFile.getBaseFile(), e2);
                            }
                        }
                    }
                }
                awaitCompletion();
                synchronized (this.mStats) {
                    this.mStats.dumpProtoLocked(this.mContext, fd, apps, flags2, historyStart);
                    if (writeData) {
                        this.mStats.writeAsyncLocked();
                    }
                }
            } else if (useCheckinFormat) {
                List<ApplicationInfo> apps2 = this.mContext.getPackageManager().getInstalledApplications(4325376);
                if (reqUid2) {
                    AtomicFile atomicFile2 = this.mStats.mCheckinFile;
                    synchronized (atomicFile2) {
                        try {
                            try {
                                if (!this.mStats.mCheckinFile.exists()) {
                                    atomicFile = atomicFile2;
                                } else {
                                    try {
                                        byte[] raw2 = this.mStats.mCheckinFile.readFully();
                                        if (raw2 == null) {
                                            atomicFile = atomicFile2;
                                        } else {
                                            Parcel in2 = Parcel.obtain();
                                            in2.unmarshall(raw2, 0, raw2.length);
                                            in2.setDataPosition(0);
                                            BatteryStatsImpl checkinStats2 = new BatteryStatsImpl((File) null, this.mStats.mHandler, (BatteryStatsImpl.PlatformIdleStateCallback) null, (BatteryStatsImpl.MeasuredEnergyRetriever) null, this.mUserManagerUserInfoProvider);
                                            checkinStats2.setPowerProfileLocked(this.mPowerProfile);
                                            checkinStats2.readSummaryFromParcel(in2);
                                            in2.recycle();
                                            batteryStatsImpl2 = checkinStats2;
                                            atomicFile = atomicFile2;
                                            try {
                                                checkinStats2.dumpCheckinLocked(this.mContext, pw, apps2, flags2, historyStart);
                                                this.mStats.mCheckinFile.delete();
                                                return;
                                            } catch (ParcelFormatException | IOException e3) {
                                                e = e3;
                                                Slog.w(TAG, "Failure reading checkin file " + this.mStats.mCheckinFile.getBaseFile(), e);
                                                awaitCompletion();
                                                batteryStatsImpl = this.mStats;
                                                synchronized (batteryStatsImpl) {
                                                }
                                            }
                                        }
                                    } catch (ParcelFormatException | IOException e4) {
                                        e = e4;
                                        atomicFile = atomicFile2;
                                    }
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
                awaitCompletion();
                batteryStatsImpl = this.mStats;
                synchronized (batteryStatsImpl) {
                    try {
                        try {
                            this.mStats.dumpCheckinLocked(this.mContext, pw, apps2, flags2, historyStart);
                            if (writeData) {
                                this.mStats.writeAsyncLocked();
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            batteryStatsImpl2 = batteryStatsImpl;
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } else {
                awaitCompletion();
                synchronized (this.mStats) {
                    try {
                        try {
                            this.mStats.dumpLocked(this.mContext, pw, flags2, reqUid, historyStart);
                            if (writeData) {
                                this.mStats.writeAsyncLocked();
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public CellularBatteryStats getCellularBatteryStats() {
        CellularBatteryStats cellularBatteryStats;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS") == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        awaitCompletion();
        synchronized (this.mStats) {
            cellularBatteryStats = this.mStats.getCellularBatteryStats();
        }
        return cellularBatteryStats;
    }

    public WifiBatteryStats getWifiBatteryStats() {
        WifiBatteryStats wifiBatteryStats;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS") == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        awaitCompletion();
        synchronized (this.mStats) {
            wifiBatteryStats = this.mStats.getWifiBatteryStats();
        }
        return wifiBatteryStats;
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats gpsBatteryStats;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        awaitCompletion();
        synchronized (this.mStats) {
            gpsBatteryStats = this.mStats.getGpsBatteryStats();
        }
        return gpsBatteryStats;
    }

    public WakeLockStats getWakeLockStats() {
        WakeLockStats wakeLockStats;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        awaitCompletion();
        synchronized (this.mStats) {
            wakeLockStats = this.mStats.getWakeLockStats();
        }
        return wakeLockStats;
    }

    public BluetoothBatteryStats getBluetoothBatteryStats() {
        BluetoothBatteryStats bluetoothBatteryStats;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        awaitCompletion();
        synchronized (this.mStats) {
            bluetoothBatteryStats = this.mStats.getBluetoothBatteryStats();
        }
        return bluetoothBatteryStats;
    }

    public HealthStatsParceler takeUidSnapshot(int requestUid) {
        HealthStatsParceler healthStatsForUidLocked;
        if (requestUid != Binder.getCallingUid()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                awaitCompletion();
                if (shouldCollectExternalStats()) {
                    syncStats("get-health-stats-for-uids", 63);
                }
                synchronized (this.mStats) {
                    healthStatsForUidLocked = getHealthStatsForUidLocked(requestUid);
                }
                return healthStatsForUidLocked;
            } catch (Exception ex) {
                Slog.w(TAG, "Crashed while writing for takeUidSnapshot(" + requestUid + ")", ex);
                throw ex;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public HealthStatsParceler[] takeUidSnapshots(int[] requestUids) {
        HealthStatsParceler[] results;
        if (!onlyCaller(requestUids)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                awaitCompletion();
                if (shouldCollectExternalStats()) {
                    syncStats("get-health-stats-for-uids", 63);
                }
                synchronized (this.mStats) {
                    int N = requestUids.length;
                    results = new HealthStatsParceler[N];
                    for (int i = 0; i < N; i++) {
                        results[i] = getHealthStatsForUidLocked(requestUids[i]);
                    }
                }
                return results;
            } catch (Exception ex) {
                throw ex;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean shouldCollectExternalStats() {
        return SystemClock.elapsedRealtime() - this.mWorker.getLastCollectionTimeStamp() > this.mStats.getExternalStatsCollectionRateLimitMs();
    }

    private static boolean onlyCaller(int[] requestUids) {
        int caller = Binder.getCallingUid();
        for (int i : requestUids) {
            if (i != caller) {
                return false;
            }
        }
        return true;
    }

    HealthStatsParceler getHealthStatsForUidLocked(int requestUid) {
        HealthStatsBatteryStatsWriter writer = new HealthStatsBatteryStatsWriter();
        HealthStatsWriter uidWriter = new HealthStatsWriter(UidHealthStats.CONSTANTS);
        BatteryStats.Uid uid = (BatteryStats.Uid) this.mStats.getUidStats().get(requestUid);
        if (uid != null) {
            writer.writeUid(uidWriter, this.mStats, uid);
        }
        return new HealthStatsParceler(uidWriter);
    }

    public boolean setChargingStateUpdateDelayMillis(int delayMillis) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.POWER_SAVER", null);
        long ident = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            return Settings.Global.putLong(contentResolver, "battery_charging_state_update_delay", delayMillis);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateForegroundTimeIfOnBattery(final String packageName, final int uid, final long cpuTimeDiff) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda58
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1333x30ed9005(uid, packageName, elapsedRealtime, uptime, cpuTimeDiff);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateForegroundTimeIfOnBattery$97$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1333x30ed9005(int uid, String packageName, long elapsedRealtime, long uptime, long cpuTimeDiff) {
        if (!isOnBattery()) {
            return;
        }
        synchronized (this.mStats) {
            try {
                try {
                    BatteryStatsImpl.Uid.Proc ps = this.mStats.getProcessStatsLocked(uid, packageName, elapsedRealtime, uptime);
                    if (ps != null) {
                        ps.addForegroundTimeLocked(cpuTimeDiff);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteCurrentTimeChanged() {
        synchronized (this.mLock) {
            final long currentTime = System.currentTimeMillis();
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda32
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1239xff111124(currentTime, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteCurrentTimeChanged$98$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1239xff111124(long currentTime, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            this.mStats.noteCurrentTimeChangedLocked(currentTime, elapsedRealtime, uptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBatteryStatsOnActivityUsage(String packageName, String className, final int uid, int userId, final boolean resumed) {
        int i;
        synchronized (this.mLock) {
            try {
                final long elapsedRealtime = SystemClock.elapsedRealtime();
                final long uptime = SystemClock.uptimeMillis();
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda28
                    @Override // java.lang.Runnable
                    public final void run() {
                        BatteryStatsService.this.m1332xe8e64687(resumed, uid, elapsedRealtime, uptime);
                    }
                });
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
        if (resumed) {
            i = 1;
        } else {
            i = 0;
        }
        FrameworkStatsLog.write(42, uid, packageName, className, i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateBatteryStatsOnActivityUsage$99$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1332xe8e64687(boolean resumed, int uid, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            if (resumed) {
                this.mStats.noteActivityResumedLocked(uid, elapsedRealtime, uptime);
            } else {
                this.mStats.noteActivityPausedLocked(uid, elapsedRealtime, uptime);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcessDied(final int uid, final int pid) {
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda42
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1271x8c32214f(uid, pid);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteProcessDied$100$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1271x8c32214f(int uid, int pid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessDiedLocked(uid, pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportExcessiveCpu(final int uid, final String processName, final long uptimeSince, final long cputimeUsed) {
        synchronized (this.mLock) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1327x31f7ca82(uid, processName, uptimeSince, cputimeUsed);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportExcessiveCpu$101$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1327x31f7ca82(int uid, String processName, long uptimeSince, long cputimeUsed) {
        synchronized (this.mStats) {
            this.mStats.reportExcessiveCpuLocked(uid, processName, uptimeSince, cputimeUsed);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteServiceStartRunning(final int uid, final String pkg, final String name) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1281x908fcaf0(uid, pkg, name, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteServiceStartRunning$102$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1281x908fcaf0(int uid, String pkg, String name, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            try {
                try {
                    BatteryStatsImpl.Uid.Pkg.Serv stats = this.mStats.getServiceStatsLocked(uid, pkg, name, elapsedRealtime, uptime);
                    stats.startRunningLocked(uptime);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteServiceStopRunning(final int uid, final String pkg, final String name) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda59
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1283x825aa205(uid, pkg, name, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteServiceStopRunning$103$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1283x825aa205(int uid, String pkg, String name, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            try {
                try {
                    BatteryStatsImpl.Uid.Pkg.Serv stats = this.mStats.getServiceStatsLocked(uid, pkg, name, elapsedRealtime, uptime);
                    stats.stopRunningLocked(uptime);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteServiceStartLaunch(final int uid, final String pkg, final String name) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda94
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1280xe8597a1e(uid, pkg, name, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteServiceStartLaunch$104$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1280xe8597a1e(int uid, String pkg, String name, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            try {
                try {
                    BatteryStatsImpl.Uid.Pkg.Serv stats = this.mStats.getServiceStatsLocked(uid, pkg, name, elapsedRealtime, uptime);
                    stats.startLaunchedLocked(uptime);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteServiceStopLaunch(final int uid, final String pkg, final String name) {
        synchronized (this.mLock) {
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            final long uptime = SystemClock.uptimeMillis();
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BatteryStatsService$$ExternalSyntheticLambda64
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryStatsService.this.m1282x28d9260b(uid, pkg, name, elapsedRealtime, uptime);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$noteServiceStopLaunch$105$com-android-server-am-BatteryStatsService  reason: not valid java name */
    public /* synthetic */ void m1282x28d9260b(int uid, String pkg, String name, long elapsedRealtime, long uptime) {
        synchronized (this.mStats) {
            try {
                try {
                    BatteryStatsImpl.Uid.Pkg.Serv stats = this.mStats.getServiceStatsLocked(uid, pkg, name, elapsedRealtime, uptime);
                    stats.stopLaunchedLocked(uptime);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void setChargerAcOnline(boolean online, boolean forceUpdate) {
        this.mBatteryManagerInternal.setChargerAcOnline(online, forceUpdate);
    }

    public void setBatteryLevel(int level, boolean forceUpdate) {
        this.mBatteryManagerInternal.setBatteryLevel(level, forceUpdate);
    }

    public void unplugBattery(boolean forceUpdate) {
        this.mBatteryManagerInternal.unplugBattery(forceUpdate);
    }

    public void resetBattery(boolean forceUpdate) {
        this.mBatteryManagerInternal.resetBattery(forceUpdate);
    }

    public void suspendBatteryInput() {
        this.mBatteryManagerInternal.suspendBatteryInput();
    }
}
