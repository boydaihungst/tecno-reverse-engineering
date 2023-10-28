package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.health.HealthInfo;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.BatteryProperty;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBatteryPropertiesRegistrar;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.sysprop.PowerProperties;
import android.util.EventLog;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.server.am.BatteryStatsService;
import com.android.server.health.HealthInfoCallback;
import com.android.server.health.HealthServiceWrapper;
import com.android.server.health.Utils;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.tare.JobSchedulerEconomicPolicy;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.transsion.hubcore.server.eventtrack.ITranAIChargingEventTrackExt;
import com.transsion.hubcore.server.tranhbm.ITranHBMManager;
import com.transsion.hubcore.server.tranled.ITranLedLightExt;
import com.transsion.server.TranSystemServiceFactory;
import com.transsion.server.tranbattery.TranBatteryServiceExt;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.NoSuchElementException;
/* loaded from: classes.dex */
public final class BatteryService extends SystemService {
    private static final long BATTERY_LEVEL_CHANGE_THROTTLE_MS = 60000;
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final int BATTERY_SCALE = 100;
    private static final boolean DEBUG = false;
    private static final String DUMPSYS_DATA_PATH = "/data/system/";
    private static final long HEALTH_HAL_WAIT_MS = 1000;
    private static final int MAX_BATTERY_LEVELS_QUEUE_SIZE = 100;
    static final int OPTION_FORCE_UPDATE = 1;
    private static ITranLedLightExt mTranLedLightExt;
    private ContentObserver batteryLowShutDownObserver;
    private ActivityManagerInternal mActivityManagerInternal;
    private boolean mActivityManagerReady;
    private boolean mBatteryInputSuspended;
    private boolean mBatteryLevelCritical;
    private boolean mBatteryLevelLow;
    private ArrayDeque<Bundle> mBatteryLevelsEventQueue;
    private BatteryPropertiesRegistrar mBatteryPropertiesRegistrar;
    private final IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private int mChargeStartLevel;
    private long mChargeStartTime;
    private final Context mContext;
    private int mCriticalBatteryLevel;
    private int mDischargeStartLevel;
    private long mDischargeStartTime;
    private boolean mEmergencyContactFinished;
    private boolean mEmergencyContactMode;
    private final Handler mHandler;
    private HealthInfo mHealthInfo;
    private HealthServiceWrapper mHealthServiceWrapper;
    private int mInvalidCharger;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private long mLastBatteryLevelChangedSentMs;
    private boolean mLastBatteryLevelCritical;
    private boolean mLastBatteryPresent;
    private int mLastBatteryStatus;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastChargeCounter;
    private final HealthInfo mLastHealthInfo;
    private int mLastInvalidCharger;
    private int mLastLowBatteryWarningLevel;
    private int mLastMaxChargingCurrent;
    private int mLastMaxChargingVoltage;
    private int mLastPlugType;
    private Led mLed;
    private final Object mLock;
    private int mLowBatteryCloseWarningLevel;
    private boolean mLowBatteryNotifyEnable;
    private int mLowBatteryWarningLevel;
    private MetricsLogger mMetricsLogger;
    private int mPlugType;
    private boolean mSentLowBatteryBroadcast;
    private int mSequence;
    private int mShutdownBatteryTemperature;
    private boolean mUpdatesStopped;
    private int mliquidCheckValue;
    private BroadcastReceiver screenOnReceiver;
    private static final String TAG = BatteryService.class.getSimpleName();
    private static final String[] DUMPSYS_ARGS = {"--checkin", "--unplugged"};
    private static final boolean TRAN_SHUTDOWN_EMERGENCY_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.tran.shutdown.emergency.support"));
    private static TranBatteryServiceExt sTranBatteryServiceExt = null;

    public BatteryService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mLastHealthInfo = new HealthInfo();
        this.mSequence = 1;
        this.mLastPlugType = -1;
        this.mSentLowBatteryBroadcast = false;
        this.mLowBatteryNotifyEnable = true;
        this.mEmergencyContactMode = false;
        this.mEmergencyContactFinished = false;
        this.mActivityManagerReady = false;
        this.batteryLowShutDownObserver = new ContentObserver(new Handler()) { // from class: com.android.server.BatteryService.10
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                synchronized (BatteryService.this.mLock) {
                    BatteryService.this.updateBatteryLowSettingLocked();
                }
            }
        };
        this.screenOnReceiver = new BroadcastReceiver() { // from class: com.android.server.BatteryService.11
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    synchronized (BatteryService.this.mLock) {
                        BatteryService.this.shutdownBatteryLowLocked();
                    }
                }
            }
        };
        this.mContext = context;
        this.mHandler = new Handler(true);
        this.mLed = new Led(context, (LightsManager) getLocalService(LightsManager.class));
        this.mBatteryStats = BatteryStatsService.getService();
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mCriticalBatteryLevel = context.getResources().getInteger(17694772);
        int integer = context.getResources().getInteger(17694863);
        this.mLowBatteryWarningLevel = integer;
        this.mLowBatteryCloseWarningLevel = integer + context.getResources().getInteger(17694862);
        this.mShutdownBatteryTemperature = context.getResources().getInteger(17694949);
        this.mBatteryLevelsEventQueue = new ArrayDeque<>();
        this.mMetricsLogger = new MetricsLogger();
        if (new File("/sys/devices/virtual/switch/invalid_charger/state").exists()) {
            UEventObserver invalidChargerObserver = new UEventObserver() { // from class: com.android.server.BatteryService.1
                public void onUEvent(UEventObserver.UEvent event) {
                    boolean equals = "1".equals(event.get("SWITCH_STATE"));
                    synchronized (BatteryService.this.mLock) {
                        if (BatteryService.this.mInvalidCharger != equals) {
                            BatteryService batteryService = BatteryService.this;
                            int invalidCharger = equals ? 1 : 0;
                            batteryService.mInvalidCharger = invalidCharger;
                        }
                    }
                }
            };
            invalidChargerObserver.startObserving("DEVPATH=/devices/virtual/switch/invalid_charger");
        }
        this.mBatteryInputSuspended = ((Boolean) PowerProperties.battery_input_suspended().orElse(false)).booleanValue();
        sTranBatteryServiceExt = TranSystemServiceFactory.getInstance().makeTranBatteryServiceExt(context);
        ITranLedLightExt Instance = ITranLedLightExt.Instance();
        mTranLedLightExt = Instance;
        Instance.init((LightsManager) getLocalService(LightsManager.class), context);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.BatteryService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.BatteryService$BatteryPropertiesRegistrar, android.os.IBinder] */
    @Override // com.android.server.SystemService
    public void onStart() {
        registerHealthCallback();
        BinderService binderService = new BinderService();
        this.mBinderService = binderService;
        publishBinderService("battery", binderService);
        ?? batteryPropertiesRegistrar = new BatteryPropertiesRegistrar();
        this.mBatteryPropertiesRegistrar = batteryPropertiesRegistrar;
        publishBinderService("batteryproperties", batteryPropertiesRegistrar);
        publishLocalService(BatteryManagerInternal.class, new LocalService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            synchronized (this.mLock) {
                ContentObserver obs = new ContentObserver(this.mHandler) { // from class: com.android.server.BatteryService.2
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        synchronized (BatteryService.this.mLock) {
                            BatteryService.this.updateBatteryWarningLevelLocked();
                        }
                    }
                };
                ContentResolver resolver = this.mContext.getContentResolver();
                sTranBatteryServiceExt.init();
                resolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, obs, -1);
                updateBatteryWarningLevelLocked();
                mTranLedLightExt.registerLedObserver();
                if (TRAN_SHUTDOWN_EMERGENCY_SUPPORT) {
                    reigsterBatteryLowShutDownObserver();
                    this.mActivityManagerReady = true;
                }
                initialLiuqidCheck();
            }
        }
        if (phase == 1000) {
            sTranBatteryServiceExt.otgStateOperation();
        }
    }

    private void initialLiuqidCheck() {
        UEventObserver liquidCheckObserver = new UEventObserver() { // from class: com.android.server.BatteryService.3
            public void onUEvent(UEventObserver.UEvent event) {
                boolean equals = "1".equals(event.get("WATER_STATE"));
                synchronized (BatteryService.this.mLock) {
                    if (equals != BatteryService.this.mliquidCheckValue) {
                        final Intent liquidCheckIntent = new Intent("mediatek.intent.action.LIQUID_CHECK_WARNING");
                        liquidCheckIntent.setPackage("com.mediatek.batterywarning");
                        liquidCheckIntent.setFlags(67108864);
                        int liquidCheckValue = equals ? 1 : 0;
                        liquidCheckIntent.putExtra(DatabaseHelper.SoundModelContract.KEY_TYPE, liquidCheckValue);
                        BatteryService.this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.3.1
                            @Override // java.lang.Runnable
                            public void run() {
                                BatteryService.this.mContext.sendBroadcastAsUser(liquidCheckIntent, UserHandle.ALL);
                                Slog.d(BatteryService.TAG, "liquidCheckObserver send LIQUID_CHECK_WARNING");
                            }
                        });
                    }
                }
                BatteryService batteryService = BatteryService.this;
                int liquidCheckValue2 = equals ? 1 : 0;
                batteryService.mliquidCheckValue = liquidCheckValue2;
            }
        };
        liquidCheckObserver.startObserving("DEVPATH=/devices/platform/odm/odm:water_detect");
    }

    private void registerHealthCallback() {
        traceBegin("HealthInitWrapper");
        try {
            try {
                this.mHealthServiceWrapper = HealthServiceWrapper.create(new HealthInfoCallback() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda4
                    @Override // com.android.server.health.HealthInfoCallback
                    public final void update(HealthInfo healthInfo) {
                        BatteryService.this.update(healthInfo);
                    }
                });
                traceEnd();
                traceBegin("HealthInitWaitUpdate");
                long beforeWait = SystemClock.uptimeMillis();
                synchronized (this.mLock) {
                    while (this.mHealthInfo == null) {
                        Slog.i(TAG, "health: Waited " + (SystemClock.uptimeMillis() - beforeWait) + "ms for callbacks. Waiting another 1000 ms...");
                        try {
                            this.mLock.wait(1000L);
                        } catch (InterruptedException e) {
                            Slog.i(TAG, "health: InterruptedException when waiting for update.  Continuing...");
                        }
                    }
                }
                Slog.i(TAG, "health: Waited " + (SystemClock.uptimeMillis() - beforeWait) + "ms and received the update.");
            } finally {
                traceEnd();
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "health: cannot register callback. (RemoteException)");
            throw ex.rethrowFromSystemServer();
        } catch (NoSuchElementException ex2) {
            Slog.e(TAG, "health: cannot register callback. (no supported health HAL service)");
            throw ex2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBatteryWarningLevelLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        int defWarnLevel = this.mContext.getResources().getInteger(17694863);
        this.mLastLowBatteryWarningLevel = this.mLowBatteryWarningLevel;
        int i = Settings.Global.getInt(resolver, "low_power_trigger_level", defWarnLevel);
        this.mLowBatteryWarningLevel = i;
        if (i == 0) {
            this.mLowBatteryWarningLevel = defWarnLevel;
        }
        int i2 = this.mLowBatteryWarningLevel;
        int i3 = this.mCriticalBatteryLevel;
        if (i2 < i3) {
            this.mLowBatteryWarningLevel = i3;
        }
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(17694862);
        m90lambda$setChargerAcOnline$1$comandroidserverBatteryService(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPoweredLocked(int plugTypeSet) {
        if (this.mHealthInfo.batteryStatus == 1) {
            return true;
        }
        if ((plugTypeSet & 1) == 0 || !this.mHealthInfo.chargerAcOnline) {
            if ((plugTypeSet & 2) == 0 || !this.mHealthInfo.chargerUsbOnline) {
                if ((plugTypeSet & 4) == 0 || !this.mHealthInfo.chargerWirelessOnline) {
                    return (plugTypeSet & 8) != 0 && this.mHealthInfo.chargerDockOnline;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    private boolean shouldSendBatteryLowLocked() {
        boolean plugged = this.mPlugType != 0;
        boolean oldPlugged = this.mLastPlugType != 0;
        if (plugged || this.mHealthInfo.batteryStatus == 1) {
            return false;
        }
        int i = this.mHealthInfo.batteryLevel;
        int i2 = this.mLowBatteryWarningLevel;
        if (i <= i2) {
            return oldPlugged || this.mLastBatteryLevel > i2 || this.mHealthInfo.batteryLevel > this.mLastLowBatteryWarningLevel;
        }
        return false;
    }

    private boolean shouldShutdownLocked() {
        return this.mHealthInfo.batteryCapacityLevel != -1 ? this.mHealthInfo.batteryCapacityLevel == 1 : this.mHealthInfo.batteryLevel <= 0 && this.mHealthInfo.batteryPresent && this.mHealthInfo.batteryStatus != 2;
    }

    private void shutdownIfNoPowerLocked() {
        if (shouldShutdownLocked()) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.4
                @Override // java.lang.Runnable
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void shutdownIfOverTempLocked() {
        if (this.mHealthInfo.batteryTemperatureTenthsCelsius > this.mShutdownBatteryTemperature) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.5
                @Override // java.lang.Runnable
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "thermal,battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void update(HealthInfo info) {
        traceBegin("HealthInfoUpdate");
        Trace.traceCounter(131072L, "BatteryChargeCounter", info.batteryChargeCounterUah);
        Trace.traceCounter(131072L, "BatteryCurrent", info.batteryCurrentMicroamps);
        Trace.traceCounter(131072L, "PlugType", plugType(info));
        Trace.traceCounter(131072L, "BatteryStatus", info.batteryStatus);
        synchronized (this.mLock) {
            if (!this.mUpdatesStopped) {
                this.mHealthInfo = info;
                m90lambda$setChargerAcOnline$1$comandroidserverBatteryService(false);
                this.mLock.notifyAll();
                if (this.mHealthInfo != null) {
                    Slog.d("BatteryService", "HealthInfoUpdate: " + this.mHealthInfo.toString());
                }
            } else {
                Utils.copyV1Battery(this.mLastHealthInfo, info);
            }
        }
        traceEnd();
    }

    private static int plugType(HealthInfo healthInfo) {
        if (healthInfo.chargerAcOnline) {
            return 1;
        }
        if (healthInfo.chargerUsbOnline) {
            return 2;
        }
        if (healthInfo.chargerWirelessOnline) {
            return 4;
        }
        if (healthInfo.chargerDockOnline) {
            return 8;
        }
        return 0;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: processValuesLocked */
    public void m90lambda$setChargerAcOnline$1$comandroidserverBatteryService(boolean force) {
        boolean logOutlier = false;
        long dischargeDuration = 0;
        this.mBatteryLevelCritical = this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mCriticalBatteryLevel;
        this.mPlugType = plugType(this.mHealthInfo);
        if (Build.IS_DEBUG_ENABLE && this.mLastPlugType != this.mPlugType) {
            Slog.d(TAG, "plugType change mLastPlugType :" + this.mLastPlugType + ", mPlugType :" + this.mPlugType);
        }
        try {
            this.mBatteryStats.setBatteryState(this.mHealthInfo.batteryStatus, this.mHealthInfo.batteryHealth, this.mPlugType, this.mHealthInfo.batteryLevel, this.mHealthInfo.batteryTemperatureTenthsCelsius, this.mHealthInfo.batteryVoltageMillivolts, this.mHealthInfo.batteryChargeCounterUah, this.mHealthInfo.batteryFullChargeUah, this.mHealthInfo.batteryChargeTimeToFullNowSeconds);
        } catch (RemoteException e) {
        }
        shutdownIfNoPowerLocked();
        shutdownIfOverTempLocked();
        if (TRAN_SHUTDOWN_EMERGENCY_SUPPORT && this.mActivityManagerReady) {
            shutdownBatteryLowLocked();
        }
        if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
            ITranHBMManager.Instance().hookBatteryTemperatureTenthsCelsius(this.mHealthInfo.batteryTemperatureTenthsCelsius);
        }
        if (force || this.mHealthInfo.batteryStatus != this.mLastBatteryStatus || this.mHealthInfo.batteryHealth != this.mLastBatteryHealth || this.mHealthInfo.batteryPresent != this.mLastBatteryPresent || this.mHealthInfo.batteryLevel != this.mLastBatteryLevel || this.mPlugType != this.mLastPlugType || this.mHealthInfo.batteryVoltageMillivolts != this.mLastBatteryVoltage || this.mHealthInfo.batteryTemperatureTenthsCelsius != this.mLastBatteryTemperature || this.mHealthInfo.maxChargingCurrentMicroamps != this.mLastMaxChargingCurrent || this.mHealthInfo.maxChargingVoltageMicrovolts != this.mLastMaxChargingVoltage || this.mHealthInfo.batteryChargeCounterUah != this.mLastChargeCounter || this.mInvalidCharger != this.mLastInvalidCharger) {
            int i = this.mPlugType;
            int i2 = this.mLastPlugType;
            if (i != i2) {
                if (i2 == 0) {
                    this.mChargeStartLevel = this.mHealthInfo.batteryLevel;
                    this.mChargeStartTime = SystemClock.elapsedRealtime();
                    LogMaker builder = new LogMaker(1417);
                    builder.setType(4);
                    builder.addTaggedData(1421, Integer.valueOf(this.mPlugType));
                    builder.addTaggedData(1418, Integer.valueOf(this.mHealthInfo.batteryLevel));
                    this.mMetricsLogger.write(builder);
                    if (this.mDischargeStartTime != 0 && this.mDischargeStartLevel != this.mHealthInfo.batteryLevel) {
                        dischargeDuration = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
                        logOutlier = true;
                        EventLog.writeEvent((int) EventLogTags.BATTERY_DISCHARGE, Long.valueOf(dischargeDuration), Integer.valueOf(this.mDischargeStartLevel), Integer.valueOf(this.mHealthInfo.batteryLevel));
                        this.mDischargeStartTime = 0L;
                    }
                } else if (i == 0) {
                    this.mDischargeStartTime = SystemClock.elapsedRealtime();
                    this.mDischargeStartLevel = this.mHealthInfo.batteryLevel;
                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    long j = this.mChargeStartTime;
                    long chargeDuration = elapsedRealtime - j;
                    if (j != 0 && chargeDuration != 0) {
                        LogMaker builder2 = new LogMaker(1417);
                        builder2.setType(5);
                        builder2.addTaggedData(1421, Integer.valueOf(this.mLastPlugType));
                        builder2.addTaggedData(1420, Long.valueOf(chargeDuration));
                        builder2.addTaggedData(1418, Integer.valueOf(this.mChargeStartLevel));
                        builder2.addTaggedData(1419, Integer.valueOf(this.mHealthInfo.batteryLevel));
                        this.mMetricsLogger.write(builder2);
                    }
                    this.mChargeStartTime = 0L;
                }
            }
            if (this.mHealthInfo.batteryStatus != this.mLastBatteryStatus || this.mHealthInfo.batteryHealth != this.mLastBatteryHealth || this.mHealthInfo.batteryPresent != this.mLastBatteryPresent || this.mPlugType != this.mLastPlugType) {
                EventLog.writeEvent((int) EventLogTags.BATTERY_STATUS, Integer.valueOf(this.mHealthInfo.batteryStatus), Integer.valueOf(this.mHealthInfo.batteryHealth), Integer.valueOf(this.mHealthInfo.batteryPresent ? 1 : 0), Integer.valueOf(this.mPlugType), this.mHealthInfo.batteryTechnology);
            }
            if (this.mHealthInfo.batteryLevel != this.mLastBatteryLevel) {
                EventLog.writeEvent((int) EventLogTags.BATTERY_LEVEL, Integer.valueOf(this.mHealthInfo.batteryLevel), Integer.valueOf(this.mHealthInfo.batteryVoltageMillivolts), Integer.valueOf(this.mHealthInfo.batteryTemperatureTenthsCelsius));
            }
            if (this.mBatteryLevelCritical && !this.mLastBatteryLevelCritical && this.mPlugType == 0) {
                logOutlier = true;
                dischargeDuration = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
            }
            if (!this.mBatteryLevelLow) {
                if (this.mPlugType == 0 && this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mLowBatteryWarningLevel) {
                    this.mBatteryLevelLow = true;
                }
            } else if (this.mPlugType != 0) {
                this.mBatteryLevelLow = false;
            } else if (this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mBatteryLevelLow = false;
            } else if (force && this.mHealthInfo.batteryLevel >= this.mLowBatteryWarningLevel) {
                this.mBatteryLevelLow = false;
            }
            this.mSequence++;
            int i3 = this.mPlugType;
            if (i3 != 0 && this.mLastPlugType == 0) {
                final Intent statusIntent = new Intent("android.intent.action.ACTION_POWER_CONNECTED");
                statusIntent.setFlags(67108864);
                statusIntent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.6
                    @Override // java.lang.Runnable
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
            } else if (i3 == 0 && this.mLastPlugType != 0) {
                final Intent statusIntent2 = new Intent("android.intent.action.ACTION_POWER_DISCONNECTED");
                statusIntent2.setFlags(67108864);
                statusIntent2.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.7
                    @Override // java.lang.Runnable
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent2, UserHandle.ALL);
                    }
                });
            }
            if (shouldSendBatteryLowLocked()) {
                this.mSentLowBatteryBroadcast = true;
                final Intent statusIntent3 = new Intent("android.intent.action.BATTERY_LOW");
                statusIntent3.setFlags(67108864);
                statusIntent3.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.8
                    @Override // java.lang.Runnable
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent3, UserHandle.ALL);
                    }
                });
            } else if (this.mSentLowBatteryBroadcast && this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mSentLowBatteryBroadcast = false;
                final Intent statusIntent4 = new Intent("android.intent.action.BATTERY_OKAY");
                statusIntent4.setFlags(67108864);
                statusIntent4.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.9
                    @Override // java.lang.Runnable
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent4, UserHandle.ALL);
                    }
                });
            }
            ITranAIChargingEventTrackExt.Instance().postBatteryLevel(this.mHealthInfo.batteryLevel, this.mPlugType);
            sendBatteryChangedIntentLocked();
            if (this.mLastBatteryLevel != this.mHealthInfo.batteryLevel || this.mLastPlugType != this.mPlugType) {
                sendBatteryLevelChangedIntentLocked();
            }
            this.mLed.updateLightsLocked();
            if (logOutlier && dischargeDuration != 0) {
                logOutlierLocked(dischargeDuration);
            }
            this.mLastBatteryStatus = this.mHealthInfo.batteryStatus;
            this.mLastBatteryHealth = this.mHealthInfo.batteryHealth;
            this.mLastBatteryPresent = this.mHealthInfo.batteryPresent;
            this.mLastBatteryLevel = this.mHealthInfo.batteryLevel;
            this.mLastPlugType = this.mPlugType;
            this.mLastBatteryVoltage = this.mHealthInfo.batteryVoltageMillivolts;
            this.mLastBatteryTemperature = this.mHealthInfo.batteryTemperatureTenthsCelsius;
            this.mLastMaxChargingCurrent = this.mHealthInfo.maxChargingCurrentMicroamps;
            this.mLastMaxChargingVoltage = this.mHealthInfo.maxChargingVoltageMicrovolts;
            this.mLastChargeCounter = this.mHealthInfo.batteryChargeCounterUah;
            this.mLastBatteryLevelCritical = this.mBatteryLevelCritical;
            this.mLastInvalidCharger = this.mInvalidCharger;
        }
    }

    private void sendBatteryChangedIntentLocked() {
        final Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.addFlags(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_START);
        int icon = getIconLocked(this.mHealthInfo.batteryLevel);
        intent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        intent.putExtra("status", this.mHealthInfo.batteryStatus);
        intent.putExtra("health", this.mHealthInfo.batteryHealth);
        intent.putExtra("present", this.mHealthInfo.batteryPresent);
        intent.putExtra("level", this.mHealthInfo.batteryLevel);
        intent.putExtra("battery_low", this.mSentLowBatteryBroadcast);
        intent.putExtra("scale", 100);
        intent.putExtra("icon-small", icon);
        intent.putExtra("plugged", this.mPlugType);
        intent.putExtra("voltage", this.mHealthInfo.batteryVoltageMillivolts);
        intent.putExtra("temperature", this.mHealthInfo.batteryTemperatureTenthsCelsius);
        intent.putExtra("technology", this.mHealthInfo.batteryTechnology);
        intent.putExtra("invalid_charger", this.mInvalidCharger);
        intent.putExtra("max_charging_current", this.mHealthInfo.maxChargingCurrentMicroamps);
        intent.putExtra("max_charging_voltage", this.mHealthInfo.maxChargingVoltageMicrovolts);
        intent.putExtra("charge_counter", this.mHealthInfo.batteryChargeCounterUah);
        this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ActivityManager.broadcastStickyIntent(intent, -1);
            }
        });
    }

    private void sendBatteryLevelChangedIntentLocked() {
        Bundle event = new Bundle();
        long now = SystemClock.elapsedRealtime();
        event.putInt(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        event.putInt("status", this.mHealthInfo.batteryStatus);
        event.putInt("health", this.mHealthInfo.batteryHealth);
        event.putBoolean("present", this.mHealthInfo.batteryPresent);
        event.putInt("level", this.mHealthInfo.batteryLevel);
        event.putBoolean("battery_low", this.mSentLowBatteryBroadcast);
        event.putInt("scale", 100);
        event.putInt("plugged", this.mPlugType);
        event.putInt("voltage", this.mHealthInfo.batteryVoltageMillivolts);
        event.putInt("temperature", this.mHealthInfo.batteryTemperatureTenthsCelsius);
        event.putInt("charge_counter", this.mHealthInfo.batteryChargeCounterUah);
        event.putLong("android.os.extra.EVENT_TIMESTAMP", now);
        boolean queueWasEmpty = this.mBatteryLevelsEventQueue.isEmpty();
        this.mBatteryLevelsEventQueue.add(event);
        if (this.mBatteryLevelsEventQueue.size() > 100) {
            this.mBatteryLevelsEventQueue.removeFirst();
        }
        if (queueWasEmpty) {
            long j = this.mLastBatteryLevelChangedSentMs;
            long delay = now - j > 60000 ? 0L : (j + 60000) - now;
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    BatteryService.this.sendEnqueuedBatteryLevelChangedEvents();
                }
            }, delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEnqueuedBatteryLevelChangedEvents() {
        ArrayList<Bundle> events;
        synchronized (this.mLock) {
            events = new ArrayList<>(this.mBatteryLevelsEventQueue);
            this.mBatteryLevelsEventQueue.clear();
        }
        Intent intent = new Intent("android.intent.action.BATTERY_LEVEL_CHANGED");
        intent.addFlags(16777216);
        intent.putParcelableArrayListExtra("android.os.extra.EVENTS", events);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.BATTERY_STATS");
        this.mLastBatteryLevelChangedSentMs = SystemClock.elapsedRealtime();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [932=4, 933=4, 934=4, 935=4, 937=4, 938=6] */
    private void logBatteryStatsLocked() {
        DropBoxManager db;
        String str;
        StringBuilder sb;
        IBinder batteryInfoService = ServiceManager.getService("batterystats");
        if (batteryInfoService == null || (db = (DropBoxManager) this.mContext.getSystemService("dropbox")) == null || !db.isTagEnabled("BATTERY_DISCHARGE_INFO")) {
            return;
        }
        File dumpFile = null;
        FileOutputStream dumpStream = null;
        try {
            try {
                dumpFile = new File("/data/system/batterystats.dump");
                dumpStream = new FileOutputStream(dumpFile);
                batteryInfoService.dump(dumpStream.getFD(), DUMPSYS_ARGS);
                FileUtils.sync(dumpStream);
                db.addFile("BATTERY_DISCHARGE_INFO", dumpFile, 2);
                try {
                    dumpStream.close();
                } catch (IOException e) {
                    Slog.e(TAG, "failed to close dumpsys output stream");
                }
            } catch (RemoteException e2) {
                Slog.e(TAG, "failed to dump battery service", e2);
                if (dumpStream != null) {
                    try {
                        dumpStream.close();
                    } catch (IOException e3) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                }
                if (dumpFile == null || dumpFile.delete()) {
                    return;
                }
                str = TAG;
                sb = new StringBuilder();
            } catch (IOException e4) {
                Slog.e(TAG, "failed to write dumpsys file", e4);
                if (dumpStream != null) {
                    try {
                        dumpStream.close();
                    } catch (IOException e5) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                }
                if (dumpFile == null || dumpFile.delete()) {
                    return;
                }
                str = TAG;
                sb = new StringBuilder();
            }
            if (dumpFile.delete()) {
                return;
            }
            str = TAG;
            sb = new StringBuilder();
            Slog.e(str, sb.append("failed to delete temporary dumpsys file: ").append(dumpFile.getAbsolutePath()).toString());
        } catch (Throwable th) {
            if (dumpStream != null) {
                try {
                    dumpStream.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "failed to close dumpsys output stream");
                }
            }
            if (dumpFile != null && !dumpFile.delete()) {
                Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile.getAbsolutePath());
            }
            throw th;
        }
    }

    private void logOutlierLocked(long duration) {
        ContentResolver cr = this.mContext.getContentResolver();
        String dischargeThresholdString = Settings.Global.getString(cr, "battery_discharge_threshold");
        String durationThresholdString = Settings.Global.getString(cr, "battery_discharge_duration_threshold");
        if (dischargeThresholdString != null && durationThresholdString != null) {
            try {
                long durationThreshold = Long.parseLong(durationThresholdString);
                int dischargeThreshold = Integer.parseInt(dischargeThresholdString);
                if (duration <= durationThreshold && this.mDischargeStartLevel - this.mHealthInfo.batteryLevel >= dischargeThreshold) {
                    logBatteryStatsLocked();
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Invalid DischargeThresholds GService string: " + durationThresholdString + " or " + dischargeThresholdString);
            }
        }
    }

    private int getIconLocked(int level) {
        if (this.mHealthInfo.batteryStatus == 2) {
            return 17303629;
        }
        if (this.mHealthInfo.batteryStatus == 3) {
            return 17303615;
        }
        if (this.mHealthInfo.batteryStatus == 4 || this.mHealthInfo.batteryStatus == 5) {
            return (!isPoweredLocked(15) || this.mHealthInfo.batteryLevel < 100) ? 17303615 : 17303629;
        }
        return 17303643;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String cmd) {
            return BatteryService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            BatteryService.dumpHelp(pw);
        }
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Battery service (battery) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  get [-f] [ac|usb|wireless|status|level|temp|present|counter|invalid]");
        pw.println("  set [-f] [ac|usb|wireless|status|level|temp|present|counter|invalid] <value>");
        pw.println("    Force a battery property value, freezing battery state.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        pw.println("  unplug [-f]");
        pw.println("    Force battery unplugged, freezing battery state.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze battery state, returning to current hardware values.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        if (Build.IS_DEBUGGABLE) {
            pw.println("  suspend_input");
            pw.println("    Suspend charging even if plugged in. ");
        }
    }

    int parseOptions(Shell shell) {
        int opts = 0;
        while (true) {
            String opt = shell.getNextOption();
            if (opt != null) {
                if ("-f".equals(opt)) {
                    opts |= 1;
                }
            } else {
                return opts;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    int onShellCommand(Shell shell, String cmd) {
        char c;
        char c2;
        char c3;
        boolean update;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        switch (cmd.hashCode()) {
            case -840325209:
                if (cmd.equals("unplug")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -541966841:
                if (cmd.equals("suspend_input")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 102230:
                if (cmd.equals("get")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 113762:
                if (cmd.equals("set")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 108404047:
                if (cmd.equals("reset")) {
                    c = 3;
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
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                unplugBattery((opts & 1) != 0, pw);
                return 0;
            case 1:
                String key = shell.getNextArg();
                if (key == null) {
                    pw.println("No property specified");
                    return -1;
                }
                switch (key.hashCode()) {
                    case -1000044642:
                        if (key.equals("wireless")) {
                            c2 = 3;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case -892481550:
                        if (key.equals("status")) {
                            c2 = 4;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case -318277445:
                        if (key.equals("present")) {
                            c2 = 0;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 3106:
                        if (key.equals("ac")) {
                            c2 = 1;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 116100:
                        if (key.equals("usb")) {
                            c2 = 2;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 3556308:
                        if (key.equals("temp")) {
                            c2 = 7;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 102865796:
                        if (key.equals("level")) {
                            c2 = 5;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 957830652:
                        if (key.equals("counter")) {
                            c2 = 6;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 1959784951:
                        if (key.equals("invalid")) {
                            c2 = '\b';
                            break;
                        }
                        c2 = 65535;
                        break;
                    default:
                        c2 = 65535;
                        break;
                }
                switch (c2) {
                    case 0:
                        pw.println(this.mHealthInfo.batteryPresent);
                        return 0;
                    case 1:
                        pw.println(this.mHealthInfo.chargerAcOnline);
                        return 0;
                    case 2:
                        pw.println(this.mHealthInfo.chargerUsbOnline);
                        return 0;
                    case 3:
                        pw.println(this.mHealthInfo.chargerWirelessOnline);
                        return 0;
                    case 4:
                        pw.println(this.mHealthInfo.batteryStatus);
                        return 0;
                    case 5:
                        pw.println(this.mHealthInfo.batteryLevel);
                        return 0;
                    case 6:
                        pw.println(this.mHealthInfo.batteryChargeCounterUah);
                        return 0;
                    case 7:
                        pw.println(this.mHealthInfo.batteryTemperatureTenthsCelsius);
                        return 0;
                    case '\b':
                        pw.println(this.mInvalidCharger);
                        return 0;
                    default:
                        pw.println("Unknown get option: " + key);
                        return 0;
                }
            case 2:
                int opts2 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                String key2 = shell.getNextArg();
                if (key2 == null) {
                    pw.println("No property specified");
                    return -1;
                }
                String value = shell.getNextArg();
                if (value == null) {
                    pw.println("No value specified");
                    return -1;
                }
                try {
                    if (!this.mUpdatesStopped) {
                        Utils.copyV1Battery(this.mLastHealthInfo, this.mHealthInfo);
                    }
                    switch (key2.hashCode()) {
                        case -1000044642:
                            if (key2.equals("wireless")) {
                                c3 = 3;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case -892481550:
                            if (key2.equals("status")) {
                                c3 = 4;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case -318277445:
                            if (key2.equals("present")) {
                                c3 = 0;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 3106:
                            if (key2.equals("ac")) {
                                c3 = 1;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 116100:
                            if (key2.equals("usb")) {
                                c3 = 2;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 3556308:
                            if (key2.equals("temp")) {
                                c3 = 7;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 102865796:
                            if (key2.equals("level")) {
                                c3 = 5;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 957830652:
                            if (key2.equals("counter")) {
                                c3 = 6;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 1959784951:
                            if (key2.equals("invalid")) {
                                c3 = '\b';
                                break;
                            }
                            c3 = 65535;
                            break;
                        default:
                            c3 = 65535;
                            break;
                    }
                    switch (c3) {
                        case 0:
                            this.mHealthInfo.batteryPresent = Integer.parseInt(value) != 0;
                            update = true;
                            break;
                        case 1:
                            this.mHealthInfo.chargerAcOnline = Integer.parseInt(value) != 0;
                            update = true;
                            break;
                        case 2:
                            this.mHealthInfo.chargerUsbOnline = Integer.parseInt(value) != 0;
                            update = true;
                            break;
                        case 3:
                            this.mHealthInfo.chargerWirelessOnline = Integer.parseInt(value) != 0;
                            update = true;
                            break;
                        case 4:
                            this.mHealthInfo.batteryStatus = Integer.parseInt(value);
                            update = true;
                            break;
                        case 5:
                            this.mHealthInfo.batteryLevel = Integer.parseInt(value);
                            update = true;
                            break;
                        case 6:
                            this.mHealthInfo.batteryChargeCounterUah = Integer.parseInt(value);
                            update = true;
                            break;
                        case 7:
                            this.mHealthInfo.batteryTemperatureTenthsCelsius = Integer.parseInt(value);
                            update = true;
                            break;
                        case '\b':
                            this.mInvalidCharger = Integer.parseInt(value);
                            update = true;
                            break;
                        default:
                            pw.println("Unknown set option: " + key2);
                            update = false;
                            break;
                    }
                    if (update) {
                        long ident = Binder.clearCallingIdentity();
                        boolean z = true;
                        this.mUpdatesStopped = true;
                        if ((opts2 & 1) == 0) {
                            z = false;
                        }
                        m91lambda$unplugBattery$3$comandroidserverBatteryService(z, pw);
                        Binder.restoreCallingIdentity(ident);
                        return 0;
                    }
                    return 0;
                } catch (NumberFormatException e) {
                    pw.println("Bad value: " + value);
                    return -1;
                }
            case 3:
                int opts3 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                resetBattery((opts3 & 1) != 0, pw);
                return 0;
            case 4:
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                suspendBatteryInput();
                return 0;
            default:
                return shell.handleDefaultCommands(cmd);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setChargerAcOnline(boolean online, final boolean forceUpdate) {
        if (!this.mUpdatesStopped) {
            Utils.copyV1Battery(this.mLastHealthInfo, this.mHealthInfo);
        }
        this.mHealthInfo.chargerAcOnline = online;
        this.mUpdatesStopped = true;
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda3
            public final void runOrThrow() {
                BatteryService.this.m90lambda$setChargerAcOnline$1$comandroidserverBatteryService(forceUpdate);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBatteryLevel(int level, final boolean forceUpdate) {
        if (!this.mUpdatesStopped) {
            Utils.copyV1Battery(this.mLastHealthInfo, this.mHealthInfo);
        }
        this.mHealthInfo.batteryLevel = level;
        this.mUpdatesStopped = true;
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda1
            public final void runOrThrow() {
                BatteryService.this.m89lambda$setBatteryLevel$2$comandroidserverBatteryService(forceUpdate);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unplugBattery(final boolean forceUpdate, final PrintWriter pw) {
        if (!this.mUpdatesStopped) {
            Utils.copyV1Battery(this.mLastHealthInfo, this.mHealthInfo);
        }
        this.mHealthInfo.chargerAcOnline = false;
        this.mHealthInfo.chargerUsbOnline = false;
        this.mHealthInfo.chargerWirelessOnline = false;
        this.mUpdatesStopped = true;
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda5
            public final void runOrThrow() {
                BatteryService.this.m91lambda$unplugBattery$3$comandroidserverBatteryService(forceUpdate, pw);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetBattery(final boolean forceUpdate, final PrintWriter pw) {
        if (this.mUpdatesStopped) {
            this.mUpdatesStopped = false;
            Utils.copyV1Battery(this.mHealthInfo, this.mLastHealthInfo);
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.BatteryService$$ExternalSyntheticLambda2
                public final void runOrThrow() {
                    BatteryService.this.m88lambda$resetBattery$4$comandroidserverBatteryService(forceUpdate, pw);
                }
            });
        }
        if (this.mBatteryInputSuspended) {
            PowerProperties.battery_input_suspended(false);
            this.mBatteryInputSuspended = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void suspendBatteryInput() {
        if (!Build.IS_DEBUGGABLE) {
            throw new SecurityException("battery suspend_input is only supported on debuggable builds");
        }
        PowerProperties.battery_input_suspended(true);
        this.mBatteryInputSuspended = true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: processValuesLocked */
    public void m91lambda$unplugBattery$3$comandroidserverBatteryService(boolean forceUpdate, PrintWriter pw) {
        m90lambda$setChargerAcOnline$1$comandroidserverBatteryService(forceUpdate);
        if (pw != null && forceUpdate) {
            pw.println(this.mSequence);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            if (args != null) {
                if (args.length != 0 && !"-a".equals(args[0])) {
                    Shell shell = new Shell();
                    shell.exec(this.mBinderService, null, fd, null, args, null, new ResultReceiver(null));
                }
            }
            pw.println("Current Battery Service state:");
            if (this.mUpdatesStopped) {
                pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
            }
            pw.println("  AC powered: " + this.mHealthInfo.chargerAcOnline);
            pw.println("  USB powered: " + this.mHealthInfo.chargerUsbOnline);
            pw.println("  Wireless powered: " + this.mHealthInfo.chargerWirelessOnline);
            pw.println("  Max charging current: " + this.mHealthInfo.maxChargingCurrentMicroamps);
            pw.println("  Max charging voltage: " + this.mHealthInfo.maxChargingVoltageMicrovolts);
            pw.println("  Charge counter: " + this.mHealthInfo.batteryChargeCounterUah);
            pw.println("  status: " + this.mHealthInfo.batteryStatus);
            pw.println("  health: " + this.mHealthInfo.batteryHealth);
            pw.println("  present: " + this.mHealthInfo.batteryPresent);
            pw.println("  level: " + this.mHealthInfo.batteryLevel);
            pw.println("  scale: 100");
            pw.println("  voltage: " + this.mHealthInfo.batteryVoltageMillivolts);
            pw.println("  temperature: " + this.mHealthInfo.batteryTemperatureTenthsCelsius);
            pw.println("  technology: " + this.mHealthInfo.batteryTechnology);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpProto(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            proto.write(1133871366145L, this.mUpdatesStopped);
            int batteryPluggedValue = 0;
            if (this.mHealthInfo.chargerAcOnline) {
                batteryPluggedValue = 1;
            } else if (this.mHealthInfo.chargerUsbOnline) {
                batteryPluggedValue = 2;
            } else if (this.mHealthInfo.chargerWirelessOnline) {
                batteryPluggedValue = 4;
            } else if (this.mHealthInfo.chargerDockOnline) {
                batteryPluggedValue = 8;
            }
            proto.write(CompanionMessage.TYPE, batteryPluggedValue);
            proto.write(1120986464259L, this.mHealthInfo.maxChargingCurrentMicroamps);
            proto.write(1120986464260L, this.mHealthInfo.maxChargingVoltageMicrovolts);
            proto.write(1120986464261L, this.mHealthInfo.batteryChargeCounterUah);
            proto.write(1159641169926L, this.mHealthInfo.batteryStatus);
            proto.write(1159641169927L, this.mHealthInfo.batteryHealth);
            proto.write(1133871366152L, this.mHealthInfo.batteryPresent);
            proto.write(1120986464265L, this.mHealthInfo.batteryLevel);
            proto.write(1120986464266L, 100);
            proto.write(1120986464267L, this.mHealthInfo.batteryVoltageMillivolts);
            proto.write(1120986464268L, this.mHealthInfo.batteryTemperatureTenthsCelsius);
            proto.write(1138166333453L, this.mHealthInfo.batteryTechnology);
        }
        proto.flush();
    }

    private static void traceBegin(String name) {
        Trace.traceBegin(524288L, name);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Led {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final LogicalLight mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public Led(Context context, LightsManager lights) {
            this.mBatteryLight = lights.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(17694902);
            this.mBatteryMediumARGB = context.getResources().getInteger(17694903);
            this.mBatteryFullARGB = context.getResources().getInteger(17694899);
            this.mBatteryLedOn = context.getResources().getInteger(17694901);
            this.mBatteryLedOff = context.getResources().getInteger(17694900);
        }

        public void updateLightsLocked() {
            if (this.mBatteryLight == null) {
                return;
            }
            int level = BatteryService.this.mHealthInfo.batteryLevel;
            int status = BatteryService.this.mHealthInfo.batteryStatus;
            if (BatteryService.mTranLedLightExt.isLedWork() || BatteryService.mTranLedLightExt.isBatteryOnlyWork()) {
                BatteryService.mTranLedLightExt.updateBattery(level, status);
            } else if (level < BatteryService.this.mLowBatteryWarningLevel) {
                if (status == 2) {
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                } else {
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                }
            } else if (status == 2 || status == 5) {
                if (status == 5 || level >= 90) {
                    this.mBatteryLight.setColor(this.mBatteryFullARGB);
                } else {
                    this.mBatteryLight.setColor(this.mBatteryMediumARGB);
                }
            } else {
                this.mBatteryLight.turnOff();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(BatteryService.this.mContext, BatteryService.TAG, pw)) {
                if (args.length > 0 && "--proto".equals(args[0])) {
                    BatteryService.this.dumpProto(fd);
                } else {
                    BatteryService.this.dumpInternal(fd, pw, args);
                }
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private final class BatteryPropertiesRegistrar extends IBatteryPropertiesRegistrar.Stub {
        private BatteryPropertiesRegistrar() {
        }

        public int getProperty(int id, BatteryProperty prop) throws RemoteException {
            return BatteryService.this.mHealthServiceWrapper.getProperty(id, prop);
        }

        public void scheduleUpdate() throws RemoteException {
            BatteryService.this.mHealthServiceWrapper.scheduleUpdate();
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends BatteryManagerInternal {
        private LocalService() {
        }

        public boolean isPowered(int plugTypeSet) {
            boolean isPoweredLocked;
            synchronized (BatteryService.this.mLock) {
                isPoweredLocked = BatteryService.this.isPoweredLocked(plugTypeSet);
            }
            return isPoweredLocked;
        }

        public int getPlugType() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mPlugType;
            }
            return i;
        }

        public int getBatteryLevel() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryLevel;
            }
            return i;
        }

        public int getBatteryChargeCounter() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryChargeCounterUah;
            }
            return i;
        }

        public int getBatteryFullCharge() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryFullChargeUah;
            }
            return i;
        }

        public boolean getBatteryLevelLow() {
            boolean z;
            synchronized (BatteryService.this.mLock) {
                z = BatteryService.this.mBatteryLevelLow;
            }
            return z;
        }

        public int getInvalidCharger() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mInvalidCharger;
            }
            return i;
        }

        public void setChargerAcOnline(boolean online, boolean forceUpdate) {
            BatteryService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            BatteryService.this.setChargerAcOnline(online, forceUpdate);
        }

        public void setBatteryLevel(int level, boolean forceUpdate) {
            BatteryService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            BatteryService.this.setBatteryLevel(level, forceUpdate);
        }

        public void unplugBattery(boolean forceUpdate) {
            BatteryService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            BatteryService.this.unplugBattery(forceUpdate, null);
        }

        public void resetBattery(boolean forceUpdate) {
            BatteryService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            BatteryService.this.resetBattery(forceUpdate, null);
        }

        public void suspendBatteryInput() {
            BatteryService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            BatteryService.this.suspendBatteryInput();
        }
    }

    private void reigsterBatteryLowShutDownObserver() {
        int userIdentifier = -2;
        try {
            userIdentifier = ActivityManager.getService().getCurrentUser().getUserHandle().getIdentifier();
        } catch (RemoteException e) {
            Slog.d(TAG, "reigsterBatteryLowShutDownObserver ERROR=" + e);
        }
        ContentResolver cr = this.mContext.getContentResolver();
        Uri LOWBATTERY_NOTIFY_ENABLE = Settings.Global.getUriFor("tran_low_battery_notify_enable");
        Uri EMERGENCY_CONTACT_MODE = Settings.Global.getUriFor("tran_low_battery_mode");
        Uri EMERGENCY_CONTACT_FINISHED = Settings.System.getUriFor("tran_emergency_contact_finished");
        cr.registerContentObserver(EMERGENCY_CONTACT_MODE, false, this.batteryLowShutDownObserver, userIdentifier);
        cr.registerContentObserver(LOWBATTERY_NOTIFY_ENABLE, false, this.batteryLowShutDownObserver, userIdentifier);
        cr.registerContentObserver(EMERGENCY_CONTACT_FINISHED, false, this.batteryLowShutDownObserver, userIdentifier);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.screenOnReceiver, filter);
        updateBatteryLowSettingLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBatteryLowSettingLocked() {
        int userIdentifier = -2;
        try {
            userIdentifier = ActivityManager.getService().getCurrentUser().getUserHandle().getIdentifier();
        } catch (RemoteException e) {
            Slog.d(TAG, "updateBatteryLowSetting ERROR=" + e);
        }
        ContentResolver cr = this.mContext.getContentResolver();
        this.mEmergencyContactMode = Settings.Global.getInt(cr, "tran_low_battery_mode", 0) == 1;
        this.mEmergencyContactFinished = Settings.System.getIntForUser(cr, "tran_emergency_contact_finished", 0, userIdentifier) == 1;
        this.mLowBatteryNotifyEnable = Settings.Global.getInt(cr, "tran_low_battery_notify_enable", 1) == 1;
        if (this.mHealthInfo.batteryStatus == 2 || getLowBatteryNotifySetting() == 0) {
            putLowBatteryNotifySetting(1);
        }
        if (this.mEmergencyContactFinished) {
            shutdownBatteryLowLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shutdownBatteryLowLocked() {
        boolean z = this.mEmergencyContactMode;
        boolean emergenceyStatus = !z || (z && this.mEmergencyContactFinished);
        if (this.mHealthInfo.batteryStatus == 2 && getLowBatteryNotifySetting() == 0) {
            putLowBatteryNotifySetting(1);
        }
        if (this.mLowBatteryNotifyEnable && this.mHealthInfo.batteryLevel <= 1) {
            boolean needNotify = getLowBatteryNotifySetting() != 0;
            if (this.mHealthInfo.batteryStatus != 2 && emergenceyStatus && needNotify) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.BatteryService.12
                    @Override // java.lang.Runnable
                    public void run() {
                        if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                            Intent intent = new Intent("com.transsion.batterylow.notify");
                            intent.setPackage("com.mediatek.batterywarning");
                            intent.setFlags(268435456);
                            BatteryService.this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                            Slog.d(BatteryService.TAG, "shutdownBatteryLowLocked start BatteryLowService");
                        }
                    }
                });
            }
        }
    }

    private int getLowBatteryNotifySetting() {
        int val = Settings.Global.getInt(this.mContext.getContentResolver(), "tran_low_battery_notify", 1);
        return val;
    }

    private void putLowBatteryNotifySetting(int val) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "tran_low_battery_notify", val);
    }
}
