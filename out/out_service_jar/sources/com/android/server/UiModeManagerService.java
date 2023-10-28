package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IApplicationThread;
import android.app.IOnProjectionStateChangedListener;
import android.app.IUiModeManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.Sandman;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.DisableCarModeActivity;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class UiModeManagerService extends SystemService {
    private static final boolean ENABLE_LAUNCH_DESK_DOCK_APP = true;
    private static final boolean LOG = false;
    private static final String SYSTEM_PROPERTY_DEVICE_THEME = "persist.sys.theme";
    private final LocalTime DEFAULT_CUSTOM_NIGHT_END_TIME;
    private final LocalTime DEFAULT_CUSTOM_NIGHT_START_TIME;
    private ActivityTaskManagerInternal mActivityTaskManager;
    private AlarmManager mAlarmManager;
    private final BroadcastReceiver mBatteryReceiver;
    private boolean mCar;
    private int mCarModeEnableFlags;
    private boolean mCarModeEnabled;
    private boolean mCarModeKeepsScreenOn;
    private Map<Integer, String> mCarModePackagePriority;
    private boolean mCharging;
    private boolean mComputedNightMode;
    private Configuration mConfiguration;
    int mCurUiMode;
    private int mCurrentUser;
    private LocalTime mCustomAutoNightModeEndMilliseconds;
    private LocalTime mCustomAutoNightModeStartMilliseconds;
    private final AlarmManager.OnAlarmListener mCustomTimeListener;
    private final ContentObserver mDarkThemeObserver;
    private int mDefaultUiModeType;
    private boolean mDeskModeKeepsScreenOn;
    private final BroadcastReceiver mDockModeReceiver;
    private int mDockState;
    private boolean mEnableCarDockLaunch;
    private final Handler mHandler;
    private boolean mHoldingConfiguration;
    private final Injector mInjector;
    private KeyguardManager mKeyguardManager;
    private boolean mLastBedtimeRequestedNightMode;
    private int mLastBroadcastState;
    private PowerManagerInternal mLocalPowerManager;
    private final LocalService mLocalService;
    private final Object mLock;
    private int mNightMode;
    private int mNightModeCustomType;
    private boolean mNightModeLocked;
    private NotificationManager mNotificationManager;
    private final BroadcastReceiver mOnScreenOffHandler;
    private final BroadcastReceiver mOnShutdown;
    private final BroadcastReceiver mOnTimeChangedHandler;
    private boolean mOverrideNightModeOff;
    private boolean mOverrideNightModeOn;
    private int mOverrideNightModeUser;
    private PowerManager mPowerManager;
    private boolean mPowerSave;
    private SparseArray<List<ProjectionHolder>> mProjectionHolders;
    private SparseArray<RemoteCallbackList<IOnProjectionStateChangedListener>> mProjectionListeners;
    private final BroadcastReceiver mResultReceiver;
    private final IUiModeManager.Stub mService;
    private int mSetUiMode;
    private final BroadcastReceiver mSettingsRestored;
    private boolean mSetupWizardComplete;
    private final ContentObserver mSetupWizardObserver;
    private boolean mStartDreamImmediatelyOnDock;
    private StatusBarManager mStatusBarManager;
    boolean mSystemReady;
    private boolean mTelevision;
    private final TwilightListener mTwilightListener;
    private TwilightManager mTwilightManager;
    private boolean mUiModeLocked;
    private boolean mVrHeadset;
    private final IVrStateCallbacks mVrStateCallbacks;
    private boolean mWaitForScreenOff;
    private PowerManager.WakeLock mWakeLock;
    private boolean mWatch;
    private WindowManagerInternal mWindowManager;
    private static final String TAG = UiModeManager.class.getSimpleName();
    public static final Set<Integer> SUPPORTED_NIGHT_MODE_CUSTOM_TYPES = new ArraySet(new Integer[]{0, 1});
    private static final boolean mIsAdbEnable = "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));

    public UiModeManagerService(Context context) {
        this(context, false, null, new Injector());
    }

    protected UiModeManagerService(Context context, boolean setupWizardComplete, TwilightManager tm, Injector injector) {
        super(context);
        this.mLock = new Object();
        this.mDockState = 0;
        this.mLastBroadcastState = 0;
        this.mNightMode = 1;
        this.mNightModeCustomType = -1;
        LocalTime of = LocalTime.of(22, 0);
        this.DEFAULT_CUSTOM_NIGHT_START_TIME = of;
        LocalTime of2 = LocalTime.of(6, 0);
        this.DEFAULT_CUSTOM_NIGHT_END_TIME = of2;
        this.mCustomAutoNightModeStartMilliseconds = of;
        this.mCustomAutoNightModeEndMilliseconds = of2;
        this.mCarModePackagePriority = new HashMap();
        this.mCarModeEnabled = false;
        this.mCharging = false;
        this.mPowerSave = false;
        this.mWaitForScreenOff = false;
        this.mLastBedtimeRequestedNightMode = false;
        this.mStartDreamImmediatelyOnDock = true;
        this.mEnableCarDockLaunch = true;
        this.mUiModeLocked = false;
        this.mNightModeLocked = false;
        this.mCurUiMode = 0;
        this.mSetUiMode = 0;
        this.mHoldingConfiguration = false;
        this.mConfiguration = new Configuration();
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mOverrideNightModeUser = 0;
        this.mLocalService = new LocalService();
        this.mResultReceiver = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (getResultCode() != -1) {
                    return;
                }
                int enableFlags = intent.getIntExtra("enableFlags", 0);
                int disableFlags = intent.getIntExtra("disableFlags", 0);
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.updateAfterBroadcastLocked(intent.getAction(), enableFlags, disableFlags);
                }
            }
        };
        this.mDockModeReceiver = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                UiModeManagerService.this.updateDockState(state);
            }
        };
        this.mBatteryReceiver = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        UiModeManagerService.this.mCharging = intent.getIntExtra("plugged", 0) != 0;
                        break;
                }
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.mSystemReady) {
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mTwilightListener = new TwilightListener() { // from class: com.android.server.UiModeManagerService.4
            @Override // com.android.server.twilight.TwilightListener
            public void onTwilightStateChanged(TwilightState state) {
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.mNightMode == 0 && UiModeManagerService.this.mSystemReady) {
                        if (UiModeManagerService.this.shouldApplyAutomaticChangesImmediately()) {
                            UiModeManagerService.this.updateLocked(0, 0);
                        } else {
                            UiModeManagerService.this.registerScreenOffEventLocked();
                        }
                    }
                }
            }
        };
        this.mOnScreenOffHandler = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.unregisterScreenOffEventLocked();
                    UiModeManagerService.this.updateLocked(0, 0);
                }
            }
        };
        this.mOnTimeChangedHandler = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.updateCustomTimeLocked();
                }
            }
        };
        this.mCustomTimeListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.UiModeManagerService$$ExternalSyntheticLambda2
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                UiModeManagerService.this.m511lambda$new$0$comandroidserverUiModeManagerService();
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() { // from class: com.android.server.UiModeManagerService.7
            public void onVrStateChanged(boolean enabled) {
                synchronized (UiModeManagerService.this.mLock) {
                    UiModeManagerService.this.mVrHeadset = enabled;
                    if (UiModeManagerService.this.mSystemReady) {
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mSetupWizardObserver = new ContentObserver(handler) { // from class: com.android.server.UiModeManagerService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.setupWizardCompleteForCurrentUser() && !selfChange) {
                        UiModeManagerService.this.mSetupWizardComplete = true;
                        UiModeManagerService.this.getContext().getContentResolver().unregisterContentObserver(UiModeManagerService.this.mSetupWizardObserver);
                        Context context2 = UiModeManagerService.this.getContext();
                        UiModeManagerService.this.updateNightModeFromSettingsLocked(context2, context2.getResources(), UserHandle.getCallingUserId());
                        UiModeManagerService.this.updateLocked(0, 0);
                    }
                }
            }
        };
        this.mDarkThemeObserver = new ContentObserver(handler) { // from class: com.android.server.UiModeManagerService.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                UiModeManagerService.this.updateSystemProperties();
            }
        };
        this.mOnShutdown = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.10
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (UiModeManagerService.this.mNightMode == 0) {
                    UiModeManagerService uiModeManagerService = UiModeManagerService.this;
                    uiModeManagerService.persistComputedNightMode(uiModeManagerService.mCurrentUser);
                }
            }
        };
        this.mSettingsRestored = new BroadcastReceiver() { // from class: com.android.server.UiModeManagerService.11
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                List<String> settings = Arrays.asList("ui_night_mode", "dark_theme_custom_start_time", "dark_theme_custom_end_time");
                if (settings.contains(intent.getExtras().getCharSequence("setting_name"))) {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.updateNightModeFromSettingsLocked(context2, context2.getResources(), UserHandle.getCallingUserId());
                        UiModeManagerService.this.updateConfigurationLocked();
                    }
                }
            }
        };
        this.mService = new AnonymousClass12();
        this.mConfiguration.setToDefaults();
        this.mSetupWizardComplete = setupWizardComplete;
        this.mTwilightManager = tm;
        this.mInjector = injector;
    }

    private static Intent buildHomeIntent(String category) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(category);
        intent.setFlags(270532608);
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-UiModeManagerService  reason: not valid java name */
    public /* synthetic */ void m511lambda$new$0$comandroidserverUiModeManagerService() {
        synchronized (this.mLock) {
            updateCustomTimeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSystemProperties() {
        int mode = Settings.Secure.getIntForUser(getContext().getContentResolver(), "ui_night_mode", this.mNightMode, 0);
        mode = (mode == 0 || mode == 3) ? 2 : 2;
        SystemProperties.set(SYSTEM_PROPERTY_DEVICE_THEME, Integer.toString(mode));
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mCurrentUser = to.getUserIdentifier();
        if (this.mNightMode == 0) {
            persistComputedNightMode(from.getUserIdentifier());
        }
        getContext().getContentResolver().unregisterContentObserver(this.mSetupWizardObserver);
        verifySetupWizardCompleted();
        synchronized (this.mLock) {
            updateNightModeFromSettingsLocked(getContext(), getContext().getResources(), to.getUserIdentifier());
            updateLocked(0, 0);
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            synchronized (this.mLock) {
                Context context = getContext();
                boolean z = true;
                this.mSystemReady = true;
                this.mKeyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
                PowerManager powerManager = (PowerManager) context.getSystemService("power");
                this.mPowerManager = powerManager;
                this.mWakeLock = powerManager.newWakeLock(26, TAG);
                this.mWindowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                this.mActivityTaskManager = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
                this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
                TwilightManager twilightManager = (TwilightManager) getLocalService(TwilightManager.class);
                if (twilightManager != null) {
                    this.mTwilightManager = twilightManager;
                }
                this.mLocalPowerManager = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                initPowerSave();
                if (this.mDockState != 2) {
                    z = false;
                }
                this.mCarModeEnabled = z;
                registerVrStateListener();
                context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("ui_night_mode"), false, this.mDarkThemeObserver, 0);
                context.registerReceiver(this.mDockModeReceiver, new IntentFilter("android.intent.action.DOCK_EVENT"));
                IntentFilter batteryFilter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
                context.registerReceiver(this.mBatteryReceiver, batteryFilter);
                context.registerReceiver(this.mSettingsRestored, new IntentFilter("android.os.action.SETTING_RESTORED"));
                context.registerReceiver(this.mOnShutdown, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
                updateConfigurationLocked();
                applyConfigurationExternallyLocked();
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        final Context context = getContext();
        verifySetupWizardCompleted();
        final Resources res = context.getResources();
        this.mStartDreamImmediatelyOnDock = res.getBoolean(17891765);
        this.mNightMode = res.getInteger(17694790);
        this.mDefaultUiModeType = res.getInteger(17694801);
        boolean z = false;
        this.mCarModeKeepsScreenOn = res.getInteger(17694768) == 1;
        this.mDeskModeKeepsScreenOn = res.getInteger(17694805) == 1;
        this.mEnableCarDockLaunch = res.getBoolean(17891629);
        this.mUiModeLocked = res.getBoolean(17891697);
        this.mNightModeLocked = res.getBoolean(17891696);
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.type.television") || pm.hasSystemFeature("android.software.leanback")) {
            z = true;
        }
        this.mTelevision = z;
        this.mCar = pm.hasSystemFeature("android.hardware.type.automotive");
        this.mWatch = pm.hasSystemFeature("android.hardware.type.watch");
        SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.UiModeManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UiModeManagerService.this.m512lambda$onStart$1$comandroidserverUiModeManagerService(context, res);
            }
        }, TAG + ".onStart");
        publishBinderService("uimode", this.mService);
        publishLocalService(UiModeManagerInternal.class, this.mLocalService);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStart$1$com-android-server-UiModeManagerService  reason: not valid java name */
    public /* synthetic */ void m512lambda$onStart$1$comandroidserverUiModeManagerService(Context context, Resources res) {
        synchronized (this.mLock) {
            TwilightManager twilightManager = (TwilightManager) getLocalService(TwilightManager.class);
            if (twilightManager != null) {
                this.mTwilightManager = twilightManager;
            }
            updateNightModeFromSettingsLocked(context, res, UserHandle.getCallingUserId());
            updateSystemProperties();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persistComputedNightMode(int userId) {
        Settings.Secure.putIntForUser(getContext().getContentResolver(), "ui_night_mode_last_computed", this.mComputedNightMode ? 1 : 0, userId);
    }

    private void initPowerSave() {
        this.mPowerSave = this.mLocalPowerManager.getLowPowerState(16).batterySaverEnabled;
        this.mLocalPowerManager.registerLowPowerModeObserver(16, new Consumer() { // from class: com.android.server.UiModeManagerService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UiModeManagerService.this.m510lambda$initPowerSave$2$comandroidserverUiModeManagerService((PowerSaveState) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initPowerSave$2$com-android-server-UiModeManagerService  reason: not valid java name */
    public /* synthetic */ void m510lambda$initPowerSave$2$comandroidserverUiModeManagerService(PowerSaveState state) {
        synchronized (this.mLock) {
            if (this.mPowerSave == state.batterySaverEnabled) {
                return;
            }
            this.mPowerSave = state.batterySaverEnabled;
            if (this.mSystemReady) {
                updateLocked(0, 0);
            }
        }
    }

    protected IUiModeManager getService() {
        return this.mService;
    }

    protected Configuration getConfiguration() {
        return this.mConfiguration;
    }

    private void verifySetupWizardCompleted() {
        Context context = getContext();
        int userId = UserHandle.getCallingUserId();
        if (!setupWizardCompleteForCurrentUser()) {
            this.mSetupWizardComplete = false;
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this.mSetupWizardObserver, userId);
            return;
        }
        this.mSetupWizardComplete = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setupWizardCompleteForCurrentUser() {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "user_setup_complete", 0, UserHandle.getCallingUserId()) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCustomTimeLocked() {
        if (this.mNightMode != 3) {
            return;
        }
        if (shouldApplyAutomaticChangesImmediately()) {
            updateLocked(0, 0);
        } else {
            registerScreenOffEventLocked();
        }
        scheduleNextCustomTimeListener();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNightModeFromSettingsLocked(Context context, Resources res, int userId) {
        if (!this.mCarModeEnabled && !this.mCar && this.mSetupWizardComplete) {
            this.mNightMode = Settings.Secure.getIntForUser(context.getContentResolver(), "ui_night_mode", res.getInteger(17694790), userId);
            this.mNightModeCustomType = Settings.Secure.getIntForUser(context.getContentResolver(), "ui_night_mode_custom_type", -1, userId);
            this.mOverrideNightModeOn = Settings.Secure.getIntForUser(context.getContentResolver(), "ui_night_mode_override_on", 0, userId) != 0;
            this.mOverrideNightModeOff = Settings.Secure.getIntForUser(context.getContentResolver(), "ui_night_mode_override_off", 0, userId) != 0;
            this.mCustomAutoNightModeStartMilliseconds = LocalTime.ofNanoOfDay(Settings.Secure.getLongForUser(context.getContentResolver(), "dark_theme_custom_start_time", this.DEFAULT_CUSTOM_NIGHT_START_TIME.toNanoOfDay() / 1000, userId) * 1000);
            this.mCustomAutoNightModeEndMilliseconds = LocalTime.ofNanoOfDay(Settings.Secure.getLongForUser(context.getContentResolver(), "dark_theme_custom_end_time", this.DEFAULT_CUSTOM_NIGHT_END_TIME.toNanoOfDay() / 1000, userId) * 1000);
            if (this.mNightMode == 0) {
                this.mComputedNightMode = Settings.Secure.getIntForUser(context.getContentResolver(), "ui_night_mode_last_computed", 0, userId) != 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long toMilliSeconds(LocalTime t) {
        return t.toNanoOfDay() / 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static LocalTime fromMilliseconds(long t) {
        return LocalTime.ofNanoOfDay(1000 * t);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerScreenOffEventLocked() {
        if (this.mPowerSave) {
            return;
        }
        this.mWaitForScreenOff = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        getContext().registerReceiver(this.mOnScreenOffHandler, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelCustomAlarm() {
        this.mAlarmManager.cancel(this.mCustomTimeListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterScreenOffEventLocked() {
        this.mWaitForScreenOff = false;
        try {
            getContext().unregisterReceiver(this.mOnScreenOffHandler);
        } catch (IllegalArgumentException e) {
        }
    }

    private void registerTimeChangeEvent() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(this.mOnTimeChangedHandler, intentFilter);
    }

    private void unregisterTimeChangeEvent() {
        try {
            getContext().unregisterReceiver(this.mOnTimeChangedHandler);
        } catch (IllegalArgumentException e) {
        }
    }

    /* renamed from: com.android.server.UiModeManagerService$12  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass12 extends IUiModeManager.Stub {
        AnonymousClass12() {
        }

        public void enableCarMode(int flags, int priority, String callingPackage) {
            if (isUiModeLocked()) {
                Slog.e(UiModeManagerService.TAG, "enableCarMode while UI mode is locked");
            } else if (priority != 0 && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.ENTER_CAR_MODE_PRIORITIZED") != 0) {
                throw new SecurityException("Enabling car mode with a priority requires permission ENTER_CAR_MODE_PRIORITIZED");
            } else {
                boolean isShellCaller = UiModeManagerService.this.mInjector.getCallingUid() == 2000;
                if (!isShellCaller) {
                    UiModeManagerService.this.assertLegit(callingPackage);
                }
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (UiModeManagerService.this.mLock) {
                        UiModeManagerService.this.setCarModeLocked(true, flags, priority, callingPackage);
                        if (UiModeManagerService.this.mSystemReady) {
                            UiModeManagerService.this.updateLocked(flags, 0);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void disableCarMode(int flags) {
            disableCarModeByCallingPackage(flags, null);
        }

        public void disableCarModeByCallingPackage(int flags, final String callingPackage) {
            if (isUiModeLocked()) {
                Slog.e(UiModeManagerService.TAG, "disableCarMode while UI mode is locked");
                return;
            }
            int callingUid = UiModeManagerService.this.mInjector.getCallingUid();
            boolean isSystemCaller = callingUid == 1000;
            boolean isShellCaller = callingUid == 2000;
            if (!isSystemCaller && !isShellCaller) {
                UiModeManagerService.this.assertLegit(callingPackage);
            }
            int carModeFlags = isSystemCaller ? flags : flags & (-3);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UiModeManagerService.this.mLock) {
                    int priority = ((Integer) UiModeManagerService.this.mCarModePackagePriority.entrySet().stream().filter(new Predicate() { // from class: com.android.server.UiModeManagerService$12$$ExternalSyntheticLambda1
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean equals;
                            equals = ((String) ((Map.Entry) obj).getValue()).equals(callingPackage);
                            return equals;
                        }
                    }).findFirst().map(new Function() { // from class: com.android.server.UiModeManagerService$12$$ExternalSyntheticLambda2
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            return (Integer) ((Map.Entry) obj).getKey();
                        }
                    }).orElse(0)).intValue();
                    UiModeManagerService.this.setCarModeLocked(false, carModeFlags, priority, callingPackage);
                    if (UiModeManagerService.this.mSystemReady) {
                        UiModeManagerService.this.updateLocked(0, flags);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getCurrentModeType() {
            int i;
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UiModeManagerService.this.mLock) {
                    i = UiModeManagerService.this.mCurUiMode & 15;
                }
                return i;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setNightMode(int mode) {
            int customModeType;
            if (mode == 3) {
                customModeType = 0;
            } else {
                customModeType = -1;
            }
            setNightModeInternal(mode, customModeType);
        }

        private void setNightModeInternal(int mode, int customModeType) {
            int i;
            if (isNightModeLocked() && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                Slog.e(UiModeManagerService.TAG, "Night mode locked, requires MODIFY_DAY_NIGHT_MODE permission");
                return;
            }
            switch (mode) {
                case 0:
                case 1:
                case 2:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown mode: " + mode);
                case 3:
                    if (!UiModeManagerService.SUPPORTED_NIGHT_MODE_CUSTOM_TYPES.contains(Integer.valueOf(customModeType))) {
                        throw new IllegalArgumentException("Can't set the custom type to " + customModeType);
                    }
                    break;
            }
            int user = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UiModeManagerService.this.mLock) {
                    if (UiModeManagerService.this.mNightMode != mode || UiModeManagerService.this.mNightModeCustomType != customModeType) {
                        if (UiModeManagerService.this.mNightMode == 0 || UiModeManagerService.this.mNightMode == 3) {
                            UiModeManagerService.this.unregisterScreenOffEventLocked();
                            UiModeManagerService.this.cancelCustomAlarm();
                        }
                        UiModeManagerService uiModeManagerService = UiModeManagerService.this;
                        if (mode == 3) {
                            i = customModeType;
                        } else {
                            i = -1;
                        }
                        uiModeManagerService.mNightModeCustomType = i;
                        UiModeManagerService.this.mNightMode = mode;
                        UiModeManagerService.this.resetNightModeOverrideLocked();
                        UiModeManagerService.this.persistNightMode(user);
                        if ((UiModeManagerService.this.mNightMode != 0 && UiModeManagerService.this.mNightMode != 3) || UiModeManagerService.this.shouldApplyAutomaticChangesImmediately()) {
                            UiModeManagerService.this.unregisterScreenOffEventLocked();
                            UiModeManagerService.this.updateLocked(0, 0);
                        } else {
                            UiModeManagerService.this.registerScreenOffEventLocked();
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getNightMode() {
            int i;
            synchronized (UiModeManagerService.this.mLock) {
                i = UiModeManagerService.this.mNightMode;
            }
            return i;
        }

        public void setNightModeCustomType(int nightModeCustomType) {
            if (UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                throw new SecurityException("setNightModeCustomType requires MODIFY_DAY_NIGHT_MODE permission");
            }
            setNightModeInternal(3, nightModeCustomType);
        }

        public int getNightModeCustomType() {
            int i;
            if (UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                throw new SecurityException("getNightModeCustomType requires MODIFY_DAY_NIGHT_MODE permission");
            }
            synchronized (UiModeManagerService.this.mLock) {
                i = UiModeManagerService.this.mNightModeCustomType;
            }
            return i;
        }

        public void setApplicationNightMode(int mode) {
            int configNightMode;
            switch (mode) {
                case 0:
                case 1:
                case 2:
                case 3:
                    switch (mode) {
                        case 1:
                            configNightMode = 16;
                            break;
                        case 2:
                            configNightMode = 32;
                            break;
                        default:
                            configNightMode = 0;
                            break;
                    }
                    ActivityTaskManagerInternal.PackageConfigurationUpdater updater = UiModeManagerService.this.mActivityTaskManager.createPackageConfigurationUpdater();
                    updater.setNightMode(configNightMode);
                    updater.commit();
                    return;
                default:
                    throw new IllegalArgumentException("Unknown mode: " + mode);
            }
        }

        public boolean isUiModeLocked() {
            boolean z;
            synchronized (UiModeManagerService.this.mLock) {
                z = UiModeManagerService.this.mUiModeLocked;
            }
            return z;
        }

        public boolean isNightModeLocked() {
            boolean z;
            synchronized (UiModeManagerService.this.mLock) {
                z = UiModeManagerService.this.mNightModeLocked;
            }
            return z;
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell(UiModeManagerService.this.mService).exec(UiModeManagerService.this.mService, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(UiModeManagerService.this.getContext(), UiModeManagerService.TAG, pw)) {
                UiModeManagerService.this.dumpImpl(pw);
            }
        }

        public boolean setNightModeActivatedForCustomMode(int modeNightCustomType, boolean active) {
            return setNightModeActivatedForModeInternal(modeNightCustomType, active);
        }

        public boolean setNightModeActivated(boolean active) {
            return setNightModeActivatedForModeInternal(UiModeManagerService.this.mNightModeCustomType, active);
        }

        private boolean setNightModeActivatedForModeInternal(int modeCustomType, boolean active) {
            if (UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                Slog.e(UiModeManagerService.TAG, "Night mode locked, requires MODIFY_DAY_NIGHT_MODE permission");
                return false;
            }
            int user = Binder.getCallingUserHandle().getIdentifier();
            if (user != UiModeManagerService.this.mCurrentUser && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
                Slog.e(UiModeManagerService.TAG, "Target user is not current user, INTERACT_ACROSS_USERS permission is required");
                return false;
            }
            if (modeCustomType == 1) {
                UiModeManagerService.this.mLastBedtimeRequestedNightMode = active;
            }
            if (modeCustomType != UiModeManagerService.this.mNightModeCustomType) {
                return false;
            }
            synchronized (UiModeManagerService.this.mLock) {
                long ident = Binder.clearCallingIdentity();
                if (UiModeManagerService.this.mNightMode != 0 && UiModeManagerService.this.mNightMode != 3) {
                    if (UiModeManagerService.this.mNightMode == 1 && active) {
                        UiModeManagerService.this.mNightMode = 2;
                    } else if (UiModeManagerService.this.mNightMode == 2 && !active) {
                        UiModeManagerService.this.mNightMode = 1;
                    }
                    UiModeManagerService.this.updateConfigurationLocked();
                    UiModeManagerService.this.applyConfigurationExternallyLocked();
                    UiModeManagerService uiModeManagerService = UiModeManagerService.this;
                    uiModeManagerService.persistNightMode(uiModeManagerService.mCurrentUser);
                    Binder.restoreCallingIdentity(ident);
                }
                UiModeManagerService.this.unregisterScreenOffEventLocked();
                UiModeManagerService.this.mOverrideNightModeOff = active ? false : true;
                UiModeManagerService.this.mOverrideNightModeOn = active;
                UiModeManagerService uiModeManagerService2 = UiModeManagerService.this;
                uiModeManagerService2.mOverrideNightModeUser = uiModeManagerService2.mCurrentUser;
                UiModeManagerService uiModeManagerService3 = UiModeManagerService.this;
                uiModeManagerService3.persistNightModeOverrides(uiModeManagerService3.mCurrentUser);
                UiModeManagerService.this.updateConfigurationLocked();
                UiModeManagerService.this.applyConfigurationExternallyLocked();
                UiModeManagerService uiModeManagerService4 = UiModeManagerService.this;
                uiModeManagerService4.persistNightMode(uiModeManagerService4.mCurrentUser);
                Binder.restoreCallingIdentity(ident);
            }
            return true;
        }

        public long getCustomNightModeStart() {
            return UiModeManagerService.this.mCustomAutoNightModeStartMilliseconds.toNanoOfDay() / 1000;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [964=4] */
        public void setCustomNightModeStart(long time) {
            LocalTime newTime;
            if (isNightModeLocked() && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                Slog.e(UiModeManagerService.TAG, "Set custom time start, requires MODIFY_DAY_NIGHT_MODE permission");
                return;
            }
            int user = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    newTime = LocalTime.ofNanoOfDay(1000 * time);
                } catch (DateTimeException e) {
                    UiModeManagerService.this.unregisterScreenOffEventLocked();
                }
                if (newTime == null) {
                    return;
                }
                UiModeManagerService.this.mCustomAutoNightModeStartMilliseconds = newTime;
                UiModeManagerService.this.persistNightMode(user);
                UiModeManagerService.this.onCustomTimeUpdated(user);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public long getCustomNightModeEnd() {
            return UiModeManagerService.this.mCustomAutoNightModeEndMilliseconds.toNanoOfDay() / 1000;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [991=4] */
        public void setCustomNightModeEnd(long time) {
            LocalTime newTime;
            if (isNightModeLocked() && UiModeManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
                Slog.e(UiModeManagerService.TAG, "Set custom time end, requires MODIFY_DAY_NIGHT_MODE permission");
                return;
            }
            int user = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    newTime = LocalTime.ofNanoOfDay(1000 * time);
                } catch (DateTimeException e) {
                    UiModeManagerService.this.unregisterScreenOffEventLocked();
                }
                if (newTime == null) {
                    return;
                }
                UiModeManagerService.this.mCustomAutoNightModeEndMilliseconds = newTime;
                UiModeManagerService.this.onCustomTimeUpdated(user);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean requestProjection(IBinder binder, int projectionType, String callingPackage) {
            UiModeManagerService.this.assertLegit(callingPackage);
            UiModeManagerService.assertSingleProjectionType(projectionType);
            UiModeManagerService.this.enforceProjectionTypePermissions(projectionType);
            synchronized (UiModeManagerService.this.mLock) {
                if (UiModeManagerService.this.mProjectionHolders == null) {
                    UiModeManagerService.this.mProjectionHolders = new SparseArray(1);
                }
                if (!UiModeManagerService.this.mProjectionHolders.contains(projectionType)) {
                    UiModeManagerService.this.mProjectionHolders.put(projectionType, new ArrayList(1));
                }
                List<ProjectionHolder> currentHolders = (List) UiModeManagerService.this.mProjectionHolders.get(projectionType);
                for (int i = 0; i < currentHolders.size(); i++) {
                    if (callingPackage.equals(currentHolders.get(i).mPackageName)) {
                        return true;
                    }
                }
                if (projectionType == 1 && !currentHolders.isEmpty()) {
                    return false;
                }
                final UiModeManagerService uiModeManagerService = UiModeManagerService.this;
                ProjectionHolder projectionHolder = new ProjectionHolder(callingPackage, projectionType, binder, new ProjectionHolder.ProjectionReleaser() { // from class: com.android.server.UiModeManagerService$12$$ExternalSyntheticLambda0
                    @Override // com.android.server.UiModeManagerService.ProjectionHolder.ProjectionReleaser
                    public final boolean release(int i2, String str) {
                        boolean releaseProjectionUnchecked;
                        releaseProjectionUnchecked = UiModeManagerService.this.releaseProjectionUnchecked(i2, str);
                        return releaseProjectionUnchecked;
                    }
                });
                if (!projectionHolder.linkToDeath()) {
                    return false;
                }
                currentHolders.add(projectionHolder);
                Slog.d(UiModeManagerService.TAG, "Package " + callingPackage + " set projection type " + projectionType + ".");
                UiModeManagerService.this.onProjectionStateChangedLocked(projectionType);
                return true;
            }
        }

        public boolean releaseProjection(int projectionType, String callingPackage) {
            UiModeManagerService.this.assertLegit(callingPackage);
            UiModeManagerService.assertSingleProjectionType(projectionType);
            UiModeManagerService.this.enforceProjectionTypePermissions(projectionType);
            return UiModeManagerService.this.releaseProjectionUnchecked(projectionType, callingPackage);
        }

        public int getActiveProjectionTypes() {
            UiModeManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.READ_PROJECTION_STATE", "getActiveProjectionTypes");
            int projectionTypeFlag = 0;
            synchronized (UiModeManagerService.this.mLock) {
                if (UiModeManagerService.this.mProjectionHolders != null) {
                    for (int i = 0; i < UiModeManagerService.this.mProjectionHolders.size(); i++) {
                        if (!((List) UiModeManagerService.this.mProjectionHolders.valueAt(i)).isEmpty()) {
                            projectionTypeFlag |= UiModeManagerService.this.mProjectionHolders.keyAt(i);
                        }
                    }
                }
            }
            return projectionTypeFlag;
        }

        public List<String> getProjectingPackages(int projectionType) {
            List<String> packageNames;
            UiModeManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.READ_PROJECTION_STATE", "getProjectionState");
            synchronized (UiModeManagerService.this.mLock) {
                packageNames = new ArrayList<>();
                UiModeManagerService.this.populateWithRelevantActivePackageNames(projectionType, packageNames);
            }
            return packageNames;
        }

        public void addOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener, int projectionType) {
            UiModeManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.READ_PROJECTION_STATE", "addOnProjectionStateChangedListener");
            if (projectionType == 0) {
                return;
            }
            synchronized (UiModeManagerService.this.mLock) {
                if (UiModeManagerService.this.mProjectionListeners == null) {
                    UiModeManagerService.this.mProjectionListeners = new SparseArray(1);
                }
                if (!UiModeManagerService.this.mProjectionListeners.contains(projectionType)) {
                    UiModeManagerService.this.mProjectionListeners.put(projectionType, new RemoteCallbackList());
                }
                if (((RemoteCallbackList) UiModeManagerService.this.mProjectionListeners.get(projectionType)).register(listener)) {
                    List<String> packageNames = new ArrayList<>();
                    int activeProjectionTypes = UiModeManagerService.this.populateWithRelevantActivePackageNames(projectionType, packageNames);
                    if (!packageNames.isEmpty()) {
                        try {
                            listener.onProjectionStateChanged(activeProjectionTypes, packageNames);
                        } catch (RemoteException e) {
                            Slog.w(UiModeManagerService.TAG, "Failed a call to onProjectionStateChanged() during listener registration.");
                        }
                    }
                }
            }
        }

        public void removeOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener) {
            UiModeManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.READ_PROJECTION_STATE", "removeOnProjectionStateChangedListener");
            synchronized (UiModeManagerService.this.mLock) {
                if (UiModeManagerService.this.mProjectionListeners != null) {
                    for (int i = 0; i < UiModeManagerService.this.mProjectionListeners.size(); i++) {
                        ((RemoteCallbackList) UiModeManagerService.this.mProjectionListeners.valueAt(i)).unregister(listener);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceProjectionTypePermissions(int p) {
        if ((p & 1) != 0) {
            getContext().enforceCallingPermission("android.permission.TOGGLE_AUTOMOTIVE_PROJECTION", "toggleProjection");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void assertSingleProjectionType(int p) {
        boolean projectionTypeIsPowerOfTwoOrZero = ((p + (-1)) & p) == 0;
        if (p == 0 || !projectionTypeIsPowerOfTwoOrZero) {
            throw new IllegalArgumentException("Must specify exactly one projection type.");
        }
    }

    private static List<String> toPackageNameList(Collection<ProjectionHolder> c) {
        List<String> packageNames = new ArrayList<>();
        for (ProjectionHolder p : c) {
            packageNames.add(p.mPackageName);
        }
        return packageNames;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int populateWithRelevantActivePackageNames(int projectionType, List<String> packageNames) {
        packageNames.clear();
        int projectionTypeFlag = 0;
        if (this.mProjectionHolders != null) {
            for (int i = 0; i < this.mProjectionHolders.size(); i++) {
                int key = this.mProjectionHolders.keyAt(i);
                List<ProjectionHolder> holders = this.mProjectionHolders.valueAt(i);
                if ((projectionType & key) != 0 && packageNames.addAll(toPackageNameList(holders))) {
                    projectionTypeFlag |= key;
                }
            }
        }
        return projectionTypeFlag;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean releaseProjectionUnchecked(int projectionType, String pkg) {
        boolean removed;
        List<ProjectionHolder> holders;
        synchronized (this.mLock) {
            removed = false;
            SparseArray<List<ProjectionHolder>> sparseArray = this.mProjectionHolders;
            if (sparseArray != null && (holders = sparseArray.get(projectionType)) != null) {
                for (int i = holders.size() - 1; i >= 0; i--) {
                    ProjectionHolder holder = holders.get(i);
                    if (pkg.equals(holder.mPackageName)) {
                        holder.unlinkToDeath();
                        Slog.d(TAG, "Projection type " + projectionType + " released by " + pkg + ".");
                        holders.remove(i);
                        removed = true;
                    }
                }
            }
            if (removed) {
                onProjectionStateChangedLocked(projectionType);
            } else {
                Slog.w(TAG, pkg + " tried to release projection type " + projectionType + " but was not set by that package.");
            }
        }
        return removed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ProjectionHolder implements IBinder.DeathRecipient {
        private final IBinder mBinder;
        private final String mPackageName;
        private final ProjectionReleaser mProjectionReleaser;
        private final int mProjectionType;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public interface ProjectionReleaser {
            boolean release(int i, String str);
        }

        private ProjectionHolder(String packageName, int projectionType, IBinder binder, ProjectionReleaser projectionReleaser) {
            this.mPackageName = packageName;
            this.mProjectionType = projectionType;
            this.mBinder = binder;
            this.mProjectionReleaser = projectionReleaser;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean linkToDeath() {
            try {
                this.mBinder.linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Slog.e(UiModeManagerService.TAG, "linkToDeath failed for projection requester: " + this.mPackageName + ".", e);
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unlinkToDeath() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.w(UiModeManagerService.TAG, "Projection holder " + this.mPackageName + " died. Releasing projection type " + this.mProjectionType + ".");
            this.mProjectionReleaser.release(this.mProjectionType, this.mPackageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void assertLegit(String packageName) {
        if (!doesPackageHaveCallingUid(packageName)) {
            throw new SecurityException("Caller claimed bogus packageName: " + packageName + ".");
        }
    }

    private boolean doesPackageHaveCallingUid(String packageName) {
        int callingUid = this.mInjector.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        long ident = Binder.clearCallingIdentity();
        try {
            return getContext().getPackageManager().getPackageUidAsUser(packageName, callingUserId) == callingUid;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProjectionStateChangedLocked(int changedProjectionType) {
        if (this.mProjectionListeners == null) {
            return;
        }
        for (int i = 0; i < this.mProjectionListeners.size(); i++) {
            int listenerProjectionType = this.mProjectionListeners.keyAt(i);
            if ((changedProjectionType & listenerProjectionType) != 0) {
                RemoteCallbackList<IOnProjectionStateChangedListener> listeners = this.mProjectionListeners.valueAt(i);
                List<String> packageNames = new ArrayList<>();
                int activeProjectionTypes = populateWithRelevantActivePackageNames(listenerProjectionType, packageNames);
                int listenerCount = listeners.beginBroadcast();
                for (int j = 0; j < listenerCount; j++) {
                    try {
                        listeners.getBroadcastItem(j).onProjectionStateChanged(activeProjectionTypes, packageNames);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failed a call to onProjectionStateChanged().");
                    }
                }
                listeners.finishBroadcast();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCustomTimeUpdated(int user) {
        persistNightMode(user);
        if (this.mNightMode != 3) {
            return;
        }
        if (shouldApplyAutomaticChangesImmediately()) {
            unregisterScreenOffEventLocked();
            updateLocked(0, 0);
            return;
        }
        registerScreenOffEventLocked();
    }

    void dumpImpl(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Current UI Mode Service state:");
            pw.print("  mDockState=");
            pw.print(this.mDockState);
            pw.print(" mLastBroadcastState=");
            pw.println(this.mLastBroadcastState);
            pw.print(" mStartDreamImmediatelyOnDock=");
            pw.print(this.mStartDreamImmediatelyOnDock);
            pw.print("  mNightMode=");
            pw.print(this.mNightMode);
            pw.print(" (");
            pw.print(Shell.nightModeToStr(this.mNightMode, this.mNightModeCustomType));
            pw.print(") ");
            pw.print(" mOverrideOn/Off=");
            pw.print(this.mOverrideNightModeOn);
            pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
            pw.print(this.mOverrideNightModeOff);
            pw.print(" mNightModeLocked=");
            pw.println(this.mNightModeLocked);
            pw.print("  mCarModeEnabled=");
            pw.print(this.mCarModeEnabled);
            pw.print(" (carModeApps=");
            for (Map.Entry<Integer, String> entry : this.mCarModePackagePriority.entrySet()) {
                pw.print(entry.getKey());
                pw.print(":");
                pw.print(entry.getValue());
                pw.print(" ");
            }
            pw.println("");
            pw.print(" waitScreenOff=");
            pw.print(this.mWaitForScreenOff);
            pw.print(" mComputedNightMode=");
            pw.print(this.mComputedNightMode);
            pw.print(" customStart=");
            pw.print(this.mCustomAutoNightModeStartMilliseconds);
            pw.print(" customEnd");
            pw.print(this.mCustomAutoNightModeEndMilliseconds);
            pw.print(" mCarModeEnableFlags=");
            pw.print(this.mCarModeEnableFlags);
            pw.print(" mEnableCarDockLaunch=");
            pw.println(this.mEnableCarDockLaunch);
            pw.print("  mCurUiMode=0x");
            pw.print(Integer.toHexString(this.mCurUiMode));
            pw.print(" mUiModeLocked=");
            pw.print(this.mUiModeLocked);
            pw.print(" mSetUiMode=0x");
            pw.println(Integer.toHexString(this.mSetUiMode));
            pw.print("  mHoldingConfiguration=");
            pw.print(this.mHoldingConfiguration);
            pw.print(" mSystemReady=");
            pw.println(this.mSystemReady);
            if (this.mTwilightManager != null) {
                pw.print("  mTwilightService.getLastTwilightState()=");
                pw.println(this.mTwilightManager.getLastTwilightState());
            }
        }
    }

    void setCarModeLocked(boolean enabled, int flags, int priority, String packageName) {
        if (enabled) {
            enableCarMode(priority, packageName);
        } else {
            disableCarMode(flags, priority, packageName);
        }
        boolean isCarModeNowEnabled = isCarModeEnabled();
        if (this.mCarModeEnabled != isCarModeNowEnabled) {
            this.mCarModeEnabled = isCarModeNowEnabled;
            if (!isCarModeNowEnabled) {
                Context context = getContext();
                updateNightModeFromSettingsLocked(context, context.getResources(), UserHandle.getCallingUserId());
            }
        }
        this.mCarModeEnableFlags = flags;
    }

    private void disableCarMode(int flags, int priority, String packageName) {
        boolean isChangeAllowed = true;
        boolean isDisableAll = (flags & 2) != 0;
        boolean isPriorityTracked = this.mCarModePackagePriority.keySet().contains(Integer.valueOf(priority));
        boolean isDefaultPriority = priority == 0;
        if (!isDefaultPriority && ((!isPriorityTracked || !this.mCarModePackagePriority.get(Integer.valueOf(priority)).equals(packageName)) && !isDisableAll)) {
            isChangeAllowed = false;
        }
        if (isChangeAllowed) {
            Slog.d(TAG, "disableCarMode: disabling, priority=" + priority + ", packageName=" + packageName);
            if (isDisableAll) {
                Set<Map.Entry<Integer, String>> entries = new ArraySet<>(this.mCarModePackagePriority.entrySet());
                this.mCarModePackagePriority.clear();
                for (Map.Entry<Integer, String> entry : entries) {
                    notifyCarModeDisabled(entry.getKey().intValue(), entry.getValue());
                }
                return;
            }
            this.mCarModePackagePriority.remove(Integer.valueOf(priority));
            notifyCarModeDisabled(priority, packageName);
        }
    }

    private void enableCarMode(int priority, String packageName) {
        boolean isPriorityTracked = this.mCarModePackagePriority.containsKey(Integer.valueOf(priority));
        boolean isPackagePresent = this.mCarModePackagePriority.containsValue(packageName);
        if (!isPriorityTracked && !isPackagePresent) {
            Slog.d(TAG, "enableCarMode: enabled at priority=" + priority + ", packageName=" + packageName);
            this.mCarModePackagePriority.put(Integer.valueOf(priority), packageName);
            notifyCarModeEnabled(priority, packageName);
            return;
        }
        Slog.d(TAG, "enableCarMode: car mode at priority " + priority + " already enabled.");
    }

    private void notifyCarModeEnabled(int priority, String packageName) {
        Intent intent = new Intent("android.app.action.ENTER_CAR_MODE_PRIORITIZED");
        intent.putExtra("android.app.extra.CALLING_PACKAGE", packageName);
        intent.putExtra("android.app.extra.PRIORITY", priority);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.HANDLE_CAR_MODE_CHANGES");
    }

    private void notifyCarModeDisabled(int priority, String packageName) {
        Intent intent = new Intent("android.app.action.EXIT_CAR_MODE_PRIORITIZED");
        intent.putExtra("android.app.extra.CALLING_PACKAGE", packageName);
        intent.putExtra("android.app.extra.PRIORITY", priority);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.HANDLE_CAR_MODE_CHANGES");
    }

    private boolean isCarModeEnabled() {
        return this.mCarModePackagePriority.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDockState(int newState) {
        synchronized (this.mLock) {
            if (newState != this.mDockState) {
                this.mDockState = newState;
                setCarModeLocked(newState == 2, 0, 0, "");
                if (this.mSystemReady) {
                    updateLocked(1, 0);
                }
            }
        }
    }

    private static boolean isDeskDockState(int state) {
        switch (state) {
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persistNightMode(int user) {
        if (this.mCarModeEnabled || this.mCar) {
            return;
        }
        Settings.Secure.putIntForUser(getContext().getContentResolver(), "ui_night_mode", this.mNightMode, user);
        Settings.Secure.putLongForUser(getContext().getContentResolver(), "ui_night_mode_custom_type", this.mNightModeCustomType, user);
        Settings.Secure.putLongForUser(getContext().getContentResolver(), "dark_theme_custom_start_time", this.mCustomAutoNightModeStartMilliseconds.toNanoOfDay() / 1000, user);
        Settings.Secure.putLongForUser(getContext().getContentResolver(), "dark_theme_custom_end_time", this.mCustomAutoNightModeEndMilliseconds.toNanoOfDay() / 1000, user);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persistNightModeOverrides(int user) {
        if (this.mCarModeEnabled || this.mCar) {
            return;
        }
        Settings.Secure.putIntForUser(getContext().getContentResolver(), "ui_night_mode_override_on", this.mOverrideNightModeOn ? 1 : 0, user);
        Settings.Secure.putIntForUser(getContext().getContentResolver(), "ui_night_mode_override_off", this.mOverrideNightModeOff ? 1 : 0, user);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConfigurationLocked() {
        int uiMode;
        int uiMode2 = this.mDefaultUiModeType;
        if (!this.mUiModeLocked) {
            if (this.mTelevision) {
                uiMode2 = 4;
            } else if (this.mWatch) {
                uiMode2 = 6;
            } else if (this.mCarModeEnabled) {
                uiMode2 = 3;
            } else if (isDeskDockState(this.mDockState)) {
                uiMode2 = 2;
            } else if (this.mVrHeadset) {
                uiMode2 = 7;
            }
        }
        int i = this.mNightMode;
        if (i == 2 || i == 1) {
            updateComputedNightModeLocked(i == 2);
        }
        if (this.mNightMode == 0) {
            boolean activateNightMode = this.mComputedNightMode;
            TwilightManager twilightManager = this.mTwilightManager;
            if (twilightManager != null) {
                twilightManager.registerListener(this.mTwilightListener, this.mHandler);
                TwilightState lastState = this.mTwilightManager.getLastTwilightState();
                activateNightMode = lastState == null ? this.mComputedNightMode : lastState.isNight();
            }
            updateComputedNightModeLocked(activateNightMode);
        } else {
            TwilightManager twilightManager2 = this.mTwilightManager;
            if (twilightManager2 != null) {
                twilightManager2.unregisterListener(this.mTwilightListener);
            }
        }
        if (this.mNightMode == 3) {
            if (this.mNightModeCustomType == 1) {
                updateComputedNightModeLocked(this.mLastBedtimeRequestedNightMode);
            } else {
                registerTimeChangeEvent();
                boolean activate = computeCustomNightMode();
                updateComputedNightModeLocked(activate);
                scheduleNextCustomTimeListener();
            }
        } else {
            unregisterTimeChangeEvent();
        }
        if (this.mPowerSave && !this.mCarModeEnabled && !this.mCar) {
            uiMode = (uiMode2 & (-17)) | 32;
        } else {
            uiMode = getComputedUiModeConfiguration(uiMode2);
        }
        this.mCurUiMode = uiMode;
        if (this.mHoldingConfiguration) {
            return;
        }
        if (!this.mWaitForScreenOff || this.mPowerSave) {
            this.mConfiguration.uiMode = uiMode;
        }
    }

    private int getComputedUiModeConfiguration(int uiMode) {
        boolean z = this.mComputedNightMode;
        return (uiMode | (z ? 32 : 16)) & (z ? -17 : -33);
    }

    private boolean computeCustomNightMode() {
        return TimeUtils.isTimeBetween(LocalTime.now(), this.mCustomAutoNightModeStartMilliseconds, this.mCustomAutoNightModeEndMilliseconds);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyConfigurationExternallyLocked() {
        if (this.mSetUiMode != this.mConfiguration.uiMode) {
            this.mSetUiMode = this.mConfiguration.uiMode;
            this.mWindowManager.clearSnapshotCache();
            try {
                if (mIsAdbEnable) {
                    Log.d(TAG, "applyConfigurationExternallyLocked", new Throwable());
                }
                ActivityTaskManager.getService().updateConfiguration(this.mConfiguration);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failure communicating with activity manager", e);
            } catch (SecurityException e2) {
                Slog.e(TAG, "Activity does not have the ", e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldApplyAutomaticChangesImmediately() {
        return this.mCar || !this.mPowerManager.isInteractive() || this.mNightModeCustomType == 1;
    }

    private void scheduleNextCustomTimeListener() {
        LocalDateTime next;
        cancelCustomAlarm();
        LocalDateTime now = LocalDateTime.now();
        boolean active = computeCustomNightMode();
        if (active) {
            next = getDateTimeAfter(this.mCustomAutoNightModeEndMilliseconds, now);
        } else {
            next = getDateTimeAfter(this.mCustomAutoNightModeStartMilliseconds, now);
        }
        long millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.mAlarmManager.setExact(1, millis, TAG, this.mCustomTimeListener, null);
    }

    private LocalDateTime getDateTimeAfter(LocalTime localTime, LocalDateTime compareTime) {
        LocalDateTime ldt = LocalDateTime.of(compareTime.toLocalDate(), localTime);
        return ldt.isBefore(compareTime) ? ldt.plusDays(1L) : ldt;
    }

    void updateLocked(int enableFlags, int disableFlags) {
        String action = null;
        String oldAction = null;
        int i = this.mLastBroadcastState;
        if (i == 2) {
            adjustStatusBarCarModeLocked();
            oldAction = UiModeManager.ACTION_EXIT_CAR_MODE;
        } else if (isDeskDockState(i)) {
            oldAction = UiModeManager.ACTION_EXIT_DESK_MODE;
        }
        boolean z = false;
        if (this.mCarModeEnabled) {
            if (this.mLastBroadcastState != 2) {
                adjustStatusBarCarModeLocked();
                if (oldAction != null) {
                    sendForegroundBroadcastToAllUsers(oldAction);
                }
                this.mLastBroadcastState = 2;
                action = UiModeManager.ACTION_ENTER_CAR_MODE;
            }
        } else if (isDeskDockState(this.mDockState)) {
            if (!isDeskDockState(this.mLastBroadcastState)) {
                if (oldAction != null) {
                    sendForegroundBroadcastToAllUsers(oldAction);
                }
                this.mLastBroadcastState = this.mDockState;
                action = UiModeManager.ACTION_ENTER_DESK_MODE;
            }
        } else {
            this.mLastBroadcastState = 0;
            action = oldAction;
        }
        if (action != null) {
            Intent intent = new Intent(action);
            intent.putExtra("enableFlags", enableFlags);
            intent.putExtra("disableFlags", disableFlags);
            intent.addFlags(268435456);
            getContext().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mResultReceiver, null, -1, null, null);
            this.mHoldingConfiguration = true;
            updateConfigurationLocked();
        } else {
            String category = null;
            if (this.mCarModeEnabled) {
                if (this.mEnableCarDockLaunch && (enableFlags & 1) != 0) {
                    category = "android.intent.category.CAR_DOCK";
                }
            } else if (isDeskDockState(this.mDockState)) {
                if ((enableFlags & 1) != 0) {
                    category = "android.intent.category.DESK_DOCK";
                }
            } else if ((disableFlags & 1) != 0) {
                category = "android.intent.category.HOME";
            }
            sendConfigurationAndStartDreamOrDockAppLocked(category);
        }
        if (this.mCharging && ((this.mCarModeEnabled && this.mCarModeKeepsScreenOn && (this.mCarModeEnableFlags & 2) == 0) || (this.mCurUiMode == 2 && this.mDeskModeKeepsScreenOn))) {
            z = true;
        }
        boolean keepScreenOn = z;
        if (keepScreenOn != this.mWakeLock.isHeld()) {
            if (keepScreenOn) {
                this.mWakeLock.acquire();
            } else {
                this.mWakeLock.release();
            }
        }
    }

    private void sendForegroundBroadcastToAllUsers(String action) {
        getContext().sendBroadcastAsUser(new Intent(action).addFlags(268435456), UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAfterBroadcastLocked(String action, int enableFlags, int disableFlags) {
        String category = null;
        if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(action)) {
            if (this.mEnableCarDockLaunch && (enableFlags & 1) != 0) {
                category = "android.intent.category.CAR_DOCK";
            }
        } else if (UiModeManager.ACTION_ENTER_DESK_MODE.equals(action)) {
            if ((enableFlags & 1) != 0) {
                category = "android.intent.category.DESK_DOCK";
            }
        } else if ((disableFlags & 1) != 0) {
            category = "android.intent.category.HOME";
        }
        sendConfigurationAndStartDreamOrDockAppLocked(category);
    }

    private void sendConfigurationAndStartDreamOrDockAppLocked(String category) {
        Intent homeIntent;
        this.mHoldingConfiguration = false;
        updateConfigurationLocked();
        boolean dockAppStarted = false;
        if (category != null) {
            Intent homeIntent2 = buildHomeIntent(category);
            if (Sandman.shouldStartDockApp(getContext(), homeIntent2)) {
                try {
                    try {
                        int result = ActivityTaskManager.getService().startActivityWithConfig((IApplicationThread) null, getContext().getBasePackageName(), getContext().getAttributionTag(), homeIntent2, (String) null, (IBinder) null, (String) null, 0, 0, this.mConfiguration, (Bundle) null, -2);
                        if (ActivityManager.isStartResultSuccessful(result)) {
                            dockAppStarted = true;
                        } else if (result != -91) {
                            homeIntent = homeIntent2;
                            try {
                                Slog.e(TAG, "Could not start dock app: " + homeIntent + ", startActivityWithConfig result " + result);
                            } catch (RemoteException e) {
                                ex = e;
                                Slog.e(TAG, "Could not start dock app: " + homeIntent, ex);
                                applyConfigurationExternallyLocked();
                                if (category == null) {
                                }
                                return;
                            }
                        }
                    } catch (RemoteException e2) {
                        ex = e2;
                        homeIntent = homeIntent2;
                    }
                } catch (RemoteException e3) {
                    ex = e3;
                    homeIntent = homeIntent2;
                }
            }
        }
        applyConfigurationExternallyLocked();
        if (category == null && !dockAppStarted) {
            if (this.mStartDreamImmediatelyOnDock || this.mKeyguardManager.isKeyguardLocked()) {
                Sandman.startDreamWhenDockedIfAppropriate(getContext());
            }
        }
    }

    private void adjustStatusBarCarModeLocked() {
        int i;
        Context context = getContext();
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        }
        StatusBarManager statusBarManager = this.mStatusBarManager;
        if (statusBarManager != null) {
            if (this.mCarModeEnabled) {
                i = 524288;
            } else {
                i = 0;
            }
            statusBarManager.disable(i);
        }
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        }
        NotificationManager notificationManager = this.mNotificationManager;
        if (notificationManager != null) {
            if (this.mCarModeEnabled) {
                Intent carModeOffIntent = new Intent(context, DisableCarModeActivity.class);
                Notification.Builder n = new Notification.Builder(context, SystemNotificationChannels.CAR_MODE).setSmallIcon(17303603).setDefaults(4).setOngoing(true).setWhen(0L).setColor(context.getColor(17170460)).setContentTitle(context.getString(17039853)).setContentText(context.getString(17039852)).setContentIntent(PendingIntent.getActivityAsUser(context, 0, carModeOffIntent, 33554432, null, UserHandle.CURRENT));
                this.mNotificationManager.notifyAsUser(null, 10, n.build(), UserHandle.ALL);
                return;
            }
            notificationManager.cancelAsUser(null, 10, UserHandle.ALL);
        }
    }

    private void updateComputedNightModeLocked(boolean activate) {
        this.mComputedNightMode = activate;
        int i = this.mNightMode;
        if (i == 2 || i == 1) {
            return;
        }
        if (this.mOverrideNightModeOn && !activate) {
            this.mComputedNightMode = true;
        } else if (this.mOverrideNightModeOff && activate) {
            this.mComputedNightMode = false;
        } else {
            resetNightModeOverrideLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean resetNightModeOverrideLocked() {
        if (this.mOverrideNightModeOff || this.mOverrideNightModeOn) {
            this.mOverrideNightModeOff = false;
            this.mOverrideNightModeOn = false;
            persistNightModeOverrides(this.mOverrideNightModeUser);
            this.mOverrideNightModeUser = 0;
            return true;
        }
        return false;
    }

    private void registerVrStateListener() {
        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (vrManager != null) {
            try {
                vrManager.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to register VR mode state listener: " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Shell extends ShellCommand {
        public static final String NIGHT_MODE_STR_AUTO = "auto";
        public static final String NIGHT_MODE_STR_CUSTOM_BEDTIME = "custom_bedtime";
        public static final String NIGHT_MODE_STR_CUSTOM_SCHEDULE = "custom_schedule";
        public static final String NIGHT_MODE_STR_NO = "no";
        public static final String NIGHT_MODE_STR_UNKNOWN = "unknown";
        public static final String NIGHT_MODE_STR_YES = "yes";
        private final IUiModeManager mInterface;

        Shell(IUiModeManager iface) {
            this.mInterface = iface;
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("UiModeManager service (uimode) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  night [yes|no|auto|custom_schedule|custom_bedtime]");
            pw.println("    Set or read night mode.");
            pw.println("  car [yes|no]");
            pw.println("    Set or read car mode.");
            pw.println("  time [start|end] <ISO time>");
            pw.println("    Set custom start/end schedule time (night mode must be set to custom to apply).");
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                switch (cmd.hashCode()) {
                    case 98260:
                        if (cmd.equals("car")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3560141:
                        if (cmd.equals("time")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 104817688:
                        if (cmd.equals("night")) {
                            c = 0;
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
                        return handleNightMode();
                    case 1:
                        return handleCarMode();
                    case 2:
                        return handleCustomTime();
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (RemoteException e) {
                PrintWriter err = getErrPrintWriter();
                err.println("Remote exception: " + e);
                return -1;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private int handleCustomTime() throws RemoteException {
            boolean z;
            String modeStr = getNextArg();
            if (modeStr == null) {
                printCustomTime();
                return 0;
            }
            switch (modeStr.hashCode()) {
                case 100571:
                    if (modeStr.equals("end")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 109757538:
                    if (modeStr.equals("start")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    String start = getNextArg();
                    this.mInterface.setCustomNightModeStart(UiModeManagerService.toMilliSeconds(LocalTime.parse(start)));
                    return 0;
                case true:
                    String end = getNextArg();
                    this.mInterface.setCustomNightModeEnd(UiModeManagerService.toMilliSeconds(LocalTime.parse(end)));
                    return 0;
                default:
                    getErrPrintWriter().println("command must be in [start|end]");
                    return -1;
            }
        }

        private void printCustomTime() throws RemoteException {
            getOutPrintWriter().println("start " + UiModeManagerService.fromMilliseconds(this.mInterface.getCustomNightModeStart()).toString());
            getOutPrintWriter().println("end " + UiModeManagerService.fromMilliseconds(this.mInterface.getCustomNightModeEnd()).toString());
        }

        private int handleNightMode() throws RemoteException {
            PrintWriter err = getErrPrintWriter();
            String modeStr = getNextArg();
            if (modeStr == null) {
                printCurrentNightMode();
                return 0;
            }
            int mode = strToNightMode(modeStr);
            int customType = strToNightModeCustomType(modeStr);
            if (mode >= 0) {
                this.mInterface.setNightMode(mode);
                if (mode == 3) {
                    this.mInterface.setNightModeCustomType(customType);
                }
                printCurrentNightMode();
                return 0;
            }
            err.println("Error: mode must be 'yes', 'no', or 'auto', or 'custom_schedule', or 'custom_bedtime'");
            return -1;
        }

        private void printCurrentNightMode() throws RemoteException {
            PrintWriter pw = getOutPrintWriter();
            int currMode = this.mInterface.getNightMode();
            int customType = this.mInterface.getNightModeCustomType();
            String currModeStr = nightModeToStr(currMode, customType);
            pw.println("Night mode: " + currModeStr);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String nightModeToStr(int mode, int customType) {
            switch (mode) {
                case 0:
                    return NIGHT_MODE_STR_AUTO;
                case 1:
                    return NIGHT_MODE_STR_NO;
                case 2:
                    return NIGHT_MODE_STR_YES;
                case 3:
                    if (customType == 0) {
                        return NIGHT_MODE_STR_CUSTOM_SCHEDULE;
                    }
                    if (customType == 1) {
                        return NIGHT_MODE_STR_CUSTOM_BEDTIME;
                    }
                    return NIGHT_MODE_STR_UNKNOWN;
                default:
                    return NIGHT_MODE_STR_UNKNOWN;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private static int strToNightMode(String modeStr) {
            char c;
            switch (modeStr.hashCode()) {
                case -757868544:
                    if (modeStr.equals(NIGHT_MODE_STR_CUSTOM_BEDTIME)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 3521:
                    if (modeStr.equals(NIGHT_MODE_STR_NO)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 119527:
                    if (modeStr.equals(NIGHT_MODE_STR_YES)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 3005871:
                    if (modeStr.equals(NIGHT_MODE_STR_AUTO)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 164399013:
                    if (modeStr.equals(NIGHT_MODE_STR_CUSTOM_SCHEDULE)) {
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
                    return 2;
                case 1:
                    return 1;
                case 2:
                    return 0;
                case 3:
                case 4:
                    return 3;
                default:
                    return -1;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private static int strToNightModeCustomType(String customTypeStr) {
            boolean z;
            switch (customTypeStr.hashCode()) {
                case -757868544:
                    if (customTypeStr.equals(NIGHT_MODE_STR_CUSTOM_BEDTIME)) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 164399013:
                    if (customTypeStr.equals(NIGHT_MODE_STR_CUSTOM_SCHEDULE)) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return 1;
                case true:
                    return 0;
                default:
                    return -1;
            }
        }

        private int handleCarMode() throws RemoteException {
            PrintWriter err = getErrPrintWriter();
            String modeStr = getNextArg();
            if (modeStr == null) {
                printCurrentCarMode();
                return 0;
            } else if (modeStr.equals(NIGHT_MODE_STR_YES)) {
                this.mInterface.enableCarMode(0, 0, "");
                printCurrentCarMode();
                return 0;
            } else if (modeStr.equals(NIGHT_MODE_STR_NO)) {
                this.mInterface.disableCarMode(0);
                printCurrentCarMode();
                return 0;
            } else {
                err.println("Error: mode must be 'yes', or 'no'");
                return -1;
            }
        }

        private void printCurrentCarMode() throws RemoteException {
            PrintWriter pw = getOutPrintWriter();
            int currMode = this.mInterface.getCurrentModeType();
            pw.println("Car mode: " + (currMode == 3 ? NIGHT_MODE_STR_YES : NIGHT_MODE_STR_NO));
        }
    }

    /* loaded from: classes.dex */
    public final class LocalService extends UiModeManagerInternal {
        public LocalService() {
        }

        @Override // com.android.server.UiModeManagerInternal
        public boolean isNightMode() {
            boolean isIt;
            synchronized (UiModeManagerService.this.mLock) {
                isIt = (UiModeManagerService.this.mConfiguration.uiMode & 32) != 0;
            }
            return isIt;
        }
    }

    /* loaded from: classes.dex */
    public static class Injector {
        public int getCallingUid() {
            return Binder.getCallingUid();
        }
    }
}
