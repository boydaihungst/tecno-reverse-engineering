package com.android.server.dreams;

import android.app.ActivityManager;
import android.app.TaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.hardware.input.InputManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.util.Slog;
import android.view.Display;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.dreams.DreamController;
import com.android.server.dreams.DreamUiEventLogger;
import com.android.server.wm.ActivityInterceptorCallback;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.transsion.hubcore.server.power.ITranPowerManagerService;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public final class DreamManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final boolean FOLDABLE_SUPPORT = "1".equals(SystemProperties.get("ro.os_foldable_screen_support", ""));
    private static final String TAG = "DreamManagerService";
    private final ActivityInterceptorCallback mActivityInterceptorCallback;
    private final ComponentName mAmbientDisplayComponent;
    private final ContentObserver mAodEnabledObserver;
    private final ActivityTaskManagerInternal mAtmInternal;
    private final Context mContext;
    private final DreamController mController;
    private final DreamController.Listener mControllerListener;
    private boolean mCurrentDreamCanDoze;
    private int mCurrentDreamDozeScreenBrightness;
    private int mCurrentDreamDozeScreenState;
    private boolean mCurrentDreamIsDozing;
    private boolean mCurrentDreamIsPreview;
    private boolean mCurrentDreamIsWaking;
    private ComponentName mCurrentDreamName;
    private Binder mCurrentDreamToken;
    private int mCurrentDreamUserId;
    private final boolean mDismissDreamOnActivityStart;
    private AmbientDisplayConfiguration mDozeConfig;
    private final ContentObserver mDozeEnabledObserver;
    private final PowerManager.WakeLock mDozeWakeLock;
    private ComponentName mDreamOverlayServiceName;
    private final DreamUiEventLogger mDreamUiEventLogger;
    private boolean mDreamsOnlyEnabledForSystemUser;
    private boolean mForceAmbientDisplayEnabled;
    private final DreamHandler mHandler;
    private final Object mLock;
    private final PowerManager mPowerManager;
    private final PowerManagerInternal mPowerManagerInternal;
    private final Runnable mSystemPropertiesChanged;
    private final UiEventLogger mUiEventLogger;

    public DreamManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mCurrentDreamDozeScreenState = 0;
        this.mCurrentDreamDozeScreenBrightness = -1;
        this.mActivityInterceptorCallback = new ActivityInterceptorCallback() { // from class: com.android.server.dreams.DreamManagerService.1
            @Override // com.android.server.wm.ActivityInterceptorCallback
            public ActivityInterceptorCallback.ActivityInterceptResult intercept(ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                return null;
            }

            @Override // com.android.server.wm.ActivityInterceptorCallback
            public void onActivityLaunched(TaskInfo taskInfo, ActivityInfo activityInfo, ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                int activityType = taskInfo.getActivityType();
                boolean activityAllowed = activityType == 2 || activityType == 5 || activityType == 4;
                if (DreamManagerService.this.mCurrentDreamToken != null && !DreamManagerService.this.mCurrentDreamIsWaking && !DreamManagerService.this.mCurrentDreamIsDozing && !activityAllowed) {
                    DreamManagerService.this.requestAwakenInternal("stopping dream due to activity start: " + activityInfo.name);
                }
            }
        };
        DreamController.Listener listener = new DreamController.Listener() { // from class: com.android.server.dreams.DreamManagerService.6
            @Override // com.android.server.dreams.DreamController.Listener
            public void onDreamStopped(Binder token) {
                synchronized (DreamManagerService.this.mLock) {
                    if (DreamManagerService.this.mCurrentDreamToken == token) {
                        DreamManagerService.this.cleanupDreamLocked();
                    }
                }
            }
        };
        this.mControllerListener = listener;
        this.mDozeEnabledObserver = new ContentObserver(null) { // from class: com.android.server.dreams.DreamManagerService.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                DreamManagerService.this.writePulseGestureEnabled();
            }
        };
        this.mAodEnabledObserver = new ContentObserver(null) { // from class: com.android.server.dreams.DreamManagerService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean dozeEnable = Settings.Secure.getIntForUser(DreamManagerService.this.mContext.getContentResolver(), "doze_enabled", 0, ActivityManager.getCurrentUser()) == 1;
                Slog.d(DreamManagerService.TAG, "updateAODKeyFile dozeEnable = " + dozeEnable);
                DreamManagerService.this.updateAODKeyFile(dozeEnable);
                if (DreamManagerService.FOLDABLE_SUPPORT) {
                    DreamManagerService.this.updateAODKey2File(dozeEnable);
                }
            }
        };
        this.mSystemPropertiesChanged = new Runnable() { // from class: com.android.server.dreams.DreamManagerService.9
            @Override // java.lang.Runnable
            public void run() {
                synchronized (DreamManagerService.this.mLock) {
                    if (DreamManagerService.this.mCurrentDreamName != null && DreamManagerService.this.mCurrentDreamCanDoze && !DreamManagerService.this.mCurrentDreamName.equals(DreamManagerService.this.getDozeComponent())) {
                        DreamManagerService.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.dreams:SYSPROP");
                    }
                }
            }
        };
        this.mContext = context;
        DreamHandler dreamHandler = new DreamHandler(FgThread.get().getLooper());
        this.mHandler = dreamHandler;
        this.mController = new DreamController(context, dreamHandler, listener);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mPowerManagerInternal = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
        this.mAtmInternal = (ActivityTaskManagerInternal) getLocalService(ActivityTaskManagerInternal.class);
        this.mDozeWakeLock = powerManager.newWakeLock(64, TAG);
        this.mDozeConfig = new AmbientDisplayConfiguration(context);
        this.mUiEventLogger = new UiEventLoggerImpl();
        this.mDreamUiEventLogger = new DreamUiEventLoggerImpl(context.getResources().getString(17039992));
        AmbientDisplayConfiguration adc = new AmbientDisplayConfiguration(context);
        this.mAmbientDisplayComponent = ComponentName.unflattenFromString(adc.ambientDisplayComponent());
        this.mDreamsOnlyEnabledForSystemUser = context.getResources().getBoolean(17891619);
        this.mDismissDreamOnActivityStart = context.getResources().getBoolean(17891601);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("dreams", new BinderService());
        publishLocalService(DreamManagerInternal.class, new LocalService());
        boolean aodSupport = "1".equals(SystemProperties.get("ro.vendor.mtk_aod_support", ""));
        if (aodSupport) {
            final boolean dozeEnable = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "doze_enabled", 0, ActivityManager.getCurrentUser()) == 1;
            this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.DreamManagerService.2
                @Override // java.lang.Runnable
                public void run() {
                    DreamManagerService.this.updateAODKeyFile(dozeEnable);
                    if (DreamManagerService.FOLDABLE_SUPPORT) {
                        DreamManagerService.this.updateAODKey2File(dozeEnable);
                    }
                }
            });
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 600) {
            if (Build.IS_DEBUGGABLE) {
                SystemProperties.addChangeCallback(this.mSystemPropertiesChanged);
            }
            this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.dreams.DreamManagerService.3
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    DreamManagerService.this.writePulseGestureEnabled();
                    synchronized (DreamManagerService.this.mLock) {
                        DreamManagerService.this.stopDreamLocked(false, "user switched");
                    }
                }
            }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("doze_pulse_on_double_tap"), false, this.mDozeEnabledObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("doze_enabled"), false, this.mAodEnabledObserver, -1);
            writePulseGestureEnabled();
            if (this.mDismissDreamOnActivityStart) {
                this.mAtmInternal.registerActivityStartInterceptor(4, this.mActivityInterceptorCallback);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("DREAM MANAGER (dumpsys dreams)");
        pw.println();
        pw.println("mCurrentDreamToken=" + this.mCurrentDreamToken);
        pw.println("mCurrentDreamName=" + this.mCurrentDreamName);
        pw.println("mCurrentDreamUserId=" + this.mCurrentDreamUserId);
        pw.println("mCurrentDreamIsPreview=" + this.mCurrentDreamIsPreview);
        pw.println("mCurrentDreamCanDoze=" + this.mCurrentDreamCanDoze);
        pw.println("mCurrentDreamIsDozing=" + this.mCurrentDreamIsDozing);
        pw.println("mCurrentDreamIsWaking=" + this.mCurrentDreamIsWaking);
        pw.println("mForceAmbientDisplayEnabled=" + this.mForceAmbientDisplayEnabled);
        pw.println("mDreamsOnlyEnabledForSystemUser=" + this.mDreamsOnlyEnabledForSystemUser);
        pw.println("mCurrentDreamDozeScreenState=" + Display.stateToString(this.mCurrentDreamDozeScreenState));
        pw.println("mCurrentDreamDozeScreenBrightness=" + this.mCurrentDreamDozeScreenBrightness);
        pw.println("getDozeComponent()=" + getDozeComponent());
        pw.println();
        DumpUtils.dumpAsync(this.mHandler, new DumpUtils.Dump() { // from class: com.android.server.dreams.DreamManagerService.4
            public void dump(PrintWriter pw2, String prefix) {
                DreamManagerService.this.mController.dump(pw2);
            }
        }, pw, "", 200L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDreamingInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mCurrentDreamToken == null || this.mCurrentDreamIsPreview || this.mCurrentDreamIsWaking) ? false : true;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestDreamInternal() {
        long time = SystemClock.uptimeMillis();
        this.mPowerManager.userActivity(time, true);
        this.mPowerManager.nap(time);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestAwakenInternal(String reason) {
        long time = SystemClock.uptimeMillis();
        this.mPowerManager.userActivity(time, false);
        stopDreamInternal(false, reason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishSelfInternal(IBinder token, boolean immediate) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == token) {
                stopDreamLocked(immediate, "finished self");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void testDreamInternal(ComponentName dream, int userId) {
        synchronized (this.mLock) {
            startDreamLocked(dream, true, false, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startDreamInternal(boolean doze) {
        int userId = ActivityManager.getCurrentUser();
        ComponentName dream = chooseDreamForUser(doze, userId);
        if (dream != null) {
            synchronized (this.mLock) {
                startDreamLocked(dream, false, doze, userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopDreamInternal(boolean immediate, String reason) {
        synchronized (this.mLock) {
            ITranPowerManagerService.Instance().hookScreenStateFromDozeToOn();
            stopDreamLocked(immediate, reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startDozingInternal(IBinder token, int screenState, int screenBrightness) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == token && this.mCurrentDreamCanDoze) {
                this.mCurrentDreamDozeScreenState = screenState;
                this.mCurrentDreamDozeScreenBrightness = screenBrightness;
                this.mPowerManagerInternal.setDozeOverrideFromDreamManager(screenState, screenBrightness);
                if (!this.mCurrentDreamIsDozing) {
                    this.mCurrentDreamIsDozing = true;
                    this.mDozeWakeLock.acquire();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopDozingInternal(IBinder token) {
        synchronized (this.mLock) {
            if (this.mCurrentDreamToken == token && this.mCurrentDreamIsDozing) {
                this.mCurrentDreamIsDozing = false;
                this.mDozeWakeLock.release();
                this.mPowerManagerInternal.setDozeOverrideFromDreamManager(0, -1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forceAmbientDisplayEnabledInternal(boolean enabled) {
        synchronized (this.mLock) {
            this.mForceAmbientDisplayEnabled = enabled;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ComponentName getActiveDreamComponentInternal(boolean doze) {
        return chooseDreamForUser(doze, ActivityManager.getCurrentUser());
    }

    private ComponentName chooseDreamForUser(boolean doze, int userId) {
        if (doze) {
            ComponentName dozeComponent = getDozeComponent(userId);
            if (validateDream(dozeComponent)) {
                return dozeComponent;
            }
            return null;
        }
        ComponentName[] dreams = getDreamComponentsForUser(userId);
        if (dreams == null || dreams.length == 0) {
            return null;
        }
        return dreams[0];
    }

    private boolean validateDream(ComponentName component) {
        if (component == null) {
            return false;
        }
        ServiceInfo serviceInfo = getServiceInfo(component);
        if (serviceInfo == null) {
            Slog.w(TAG, "Dream " + component + " does not exist");
            return false;
        } else if (serviceInfo.applicationInfo.targetSdkVersion >= 21 && !"android.permission.BIND_DREAM_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, "Dream " + component + " is not available because its manifest is missing the android.permission.BIND_DREAM_SERVICE permission on the dream service declaration.");
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ComponentName[] getDreamComponentsForUser(int userId) {
        ComponentName defaultDream;
        if (!dreamsEnabledForUser(userId)) {
            return null;
        }
        String names = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "screensaver_components", userId);
        ComponentName[] components = componentsFromString(names);
        List<ComponentName> validComponents = new ArrayList<>();
        if (components != null) {
            for (ComponentName component : components) {
                if (validateDream(component)) {
                    validComponents.add(component);
                }
            }
        }
        if (validComponents.isEmpty() && (defaultDream = getDefaultDreamComponentForUser(userId)) != null) {
            Slog.w(TAG, "Falling back to default dream " + defaultDream);
            validComponents.add(defaultDream);
        }
        return (ComponentName[]) validComponents.toArray(new ComponentName[validComponents.size()]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDreamComponentsForUser(int userId, ComponentName[] componentNames) {
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "screensaver_components", componentsToString(componentNames), userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ComponentName getDefaultDreamComponentForUser(int userId) {
        String name = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "screensaver_default_component", userId);
        if (name == null) {
            return null;
        }
        return ComponentName.unflattenFromString(name);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ComponentName getDozeComponent() {
        return getDozeComponent(ActivityManager.getCurrentUser());
    }

    private ComponentName getDozeComponent(int userId) {
        if (this.mForceAmbientDisplayEnabled || this.mDozeConfig.enabled(userId)) {
            return ComponentName.unflattenFromString(this.mDozeConfig.ambientDisplayComponent());
        }
        return null;
    }

    private boolean dreamsEnabledForUser(int userId) {
        return !this.mDreamsOnlyEnabledForSystemUser || userId == 0;
    }

    private ServiceInfo getServiceInfo(ComponentName name) {
        if (name != null) {
            try {
                return this.mContext.getPackageManager().getServiceInfo(name, 268435456);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private void startDreamLocked(final ComponentName name, final boolean isPreviewMode, final boolean canDoze, final int userId) {
        if (this.mCurrentDreamIsWaking || !Objects.equals(this.mCurrentDreamName, name) || this.mCurrentDreamIsPreview != isPreviewMode || this.mCurrentDreamCanDoze != canDoze || this.mCurrentDreamUserId != userId) {
            stopDreamLocked(true, "starting new dream");
            Slog.i(TAG, "Entering dreamland.");
            final Binder newToken = new Binder();
            this.mCurrentDreamToken = newToken;
            this.mCurrentDreamName = name;
            this.mCurrentDreamIsPreview = isPreviewMode;
            this.mCurrentDreamCanDoze = canDoze;
            this.mCurrentDreamUserId = userId;
            if (!name.equals(this.mAmbientDisplayComponent)) {
                this.mUiEventLogger.log(DreamUiEventLogger.DreamUiEventEnum.DREAM_START);
                this.mDreamUiEventLogger.log(DreamUiEventLogger.DreamUiEventEnum.DREAM_START, this.mCurrentDreamName.flattenToString());
            }
            final PowerManager.WakeLock wakeLock = this.mPowerManager.newWakeLock(1, "startDream");
            this.mHandler.post(wakeLock.wrap(new Runnable() { // from class: com.android.server.dreams.DreamManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DreamManagerService.this.m3685x5ae3487e(newToken, name, isPreviewMode, canDoze, userId, wakeLock);
                }
            }));
            return;
        }
        Slog.i(TAG, "Already in target dream.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startDreamLocked$0$com-android-server-dreams-DreamManagerService  reason: not valid java name */
    public /* synthetic */ void m3685x5ae3487e(Binder newToken, ComponentName name, boolean isPreviewMode, boolean canDoze, int userId, PowerManager.WakeLock wakeLock) {
        this.mAtmInternal.notifyDreamStateChanged(true);
        this.mController.startDream(newToken, name, isPreviewMode, canDoze, userId, wakeLock, this.mDreamOverlayServiceName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopDreamLocked(final boolean immediate, final String reason) {
        if (this.mCurrentDreamToken != null) {
            if (immediate) {
                Slog.i(TAG, "Leaving dreamland.");
                cleanupDreamLocked();
            } else if (!this.mCurrentDreamIsWaking) {
                Slog.i(TAG, "Gently waking up from dream.");
                this.mCurrentDreamIsWaking = true;
            } else {
                return;
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.DreamManagerService.5
                @Override // java.lang.Runnable
                public void run() {
                    Slog.i(DreamManagerService.TAG, "Performing gentle wake from dream.");
                    DreamManagerService.this.mController.stopDream(immediate, reason);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupDreamLocked() {
        if (!this.mCurrentDreamName.equals(this.mAmbientDisplayComponent)) {
            this.mUiEventLogger.log(DreamUiEventLogger.DreamUiEventEnum.DREAM_STOP);
            this.mDreamUiEventLogger.log(DreamUiEventLogger.DreamUiEventEnum.DREAM_STOP, this.mCurrentDreamName.flattenToString());
        }
        this.mCurrentDreamToken = null;
        this.mCurrentDreamName = null;
        this.mCurrentDreamIsPreview = false;
        this.mCurrentDreamCanDoze = false;
        this.mCurrentDreamUserId = 0;
        this.mCurrentDreamIsWaking = false;
        if (this.mCurrentDreamIsDozing) {
            this.mCurrentDreamIsDozing = false;
            this.mDozeWakeLock.release();
        }
        this.mCurrentDreamDozeScreenState = 0;
        this.mCurrentDreamDozeScreenBrightness = -1;
        this.mHandler.post(new Runnable() { // from class: com.android.server.dreams.DreamManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DreamManagerService.this.m3684x8ed3affd();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cleanupDreamLocked$1$com-android-server-dreams-DreamManagerService  reason: not valid java name */
    public /* synthetic */ void m3684x8ed3affd() {
        this.mAtmInternal.notifyDreamStateChanged(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + permission);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writePulseGestureEnabled() {
        ComponentName name = getDozeComponent();
        boolean dozeEnabled = validateDream(name);
        ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).setPulseGestureEnabled(dozeEnabled);
    }

    private static String componentsToString(ComponentName[] componentNames) {
        if (componentNames == null) {
            return null;
        }
        StringBuilder names = new StringBuilder();
        for (ComponentName componentName : componentNames) {
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(componentName.flattenToString());
        }
        return names.toString();
    }

    private static ComponentName[] componentsFromString(String names) {
        if (names == null) {
            return null;
        }
        String[] namesArray = names.split(",");
        ComponentName[] componentNames = new ComponentName[namesArray.length];
        for (int i = 0; i < namesArray.length; i++) {
            componentNames[i] = ComponentName.unflattenFromString(namesArray[i]);
        }
        return componentNames;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DreamHandler extends Handler {
        public DreamHandler(Looper looper) {
            super(looper, null, true);
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IDreamManager.Stub {
        private BinderService() {
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DreamManagerService.this.mContext, DreamManagerService.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    DreamManagerService.this.dumpInternal(pw);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public ComponentName[] getDreamComponents() {
            return getDreamComponentsForUser(UserHandle.getCallingUserId());
        }

        public ComponentName[] getDreamComponentsForUser(int userId) {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getDreamComponents", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.getDreamComponentsForUser(userId2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setDreamComponents(ComponentName[] componentNames) {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            int userId = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                setDreamComponentsForUser(userId, componentNames);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setDreamComponentsForUser(int userId, ComponentName[] componentNames) {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setDreamComponents", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.setDreamComponentsForUser(userId2, componentNames);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void registerDreamOverlayService(ComponentName overlayComponent) {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            DreamManagerService.this.mDreamOverlayServiceName = overlayComponent;
        }

        public ComponentName getDefaultDreamComponentForUser(int userId) {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getDefaultDreamComponent", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.getDefaultDreamComponentForUser(userId2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isDreaming() {
            DreamManagerService.this.checkPermission("android.permission.READ_DREAM_STATE");
            long ident = Binder.clearCallingIdentity();
            try {
                return DreamManagerService.this.isDreamingInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void dream() {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.requestDreamInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void testDream(int userId, ComponentName dream) {
            if (dream == null) {
                throw new IllegalArgumentException("dream must not be null");
            }
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "testDream", null);
            int currentUserId = ActivityManager.getCurrentUser();
            if (userId2 != currentUserId) {
                Slog.w(DreamManagerService.TAG, "Aborted attempt to start a test dream while a different  user is active: userId=" + userId2 + ", currentUserId=" + currentUserId);
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.testDreamInternal(dream, userId2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void awaken() {
            DreamManagerService.this.checkPermission("android.permission.WRITE_DREAM_STATE");
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.requestAwakenInternal("request awaken");
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void finishSelf(IBinder token, boolean immediate) {
            if (token == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.finishSelfInternal(token, immediate);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void startDozing(IBinder token, int screenState, int screenBrightness) {
            if (token == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.startDozingInternal(token, screenState, screenBrightness);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void stopDozing(IBinder token) {
            if (token == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.stopDozingInternal(token);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void forceAmbientDisplayEnabled(boolean enabled) {
            DreamManagerService.this.checkPermission("android.permission.DEVICE_POWER");
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.forceAmbientDisplayEnabledInternal(enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void notifyAodAction(int state) {
            long ident = Binder.clearCallingIdentity();
            try {
                DreamManagerService.this.notifyAodActionInternal(state);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void notifyAodActionInternal(int state) {
        this.mController.notifyAodAction(state);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [852=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public void updateAODKeyFile(boolean isOpen) {
        FileOutputStream fops = null;
        Slog.d(TAG, "updateAODKeyFile isOpen = " + isOpen);
        try {
            try {
                try {
                    try {
                        fops = new FileOutputStream("/proc/gesture_function");
                        fops.write((isOpen ? "fp1" : "fp2").getBytes());
                        fops.flush();
                        fops.close();
                    } catch (Throwable th) {
                        if (fops != null) {
                            try {
                                fops.close();
                            } catch (IOException e) {
                                Slog.d(TAG, "updateAODKeyFile IOException");
                            }
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    Slog.d(TAG, "updateAODKeyFile IOException e = " + e2);
                    if (fops == null) {
                        return;
                    }
                    fops.close();
                }
            } catch (Exception e3) {
                Slog.d(TAG, "updateAODKeyFile Exception");
                if (fops == null) {
                    return;
                }
                fops.close();
            }
        } catch (IOException e4) {
            Slog.d(TAG, "updateAODKeyFile IOException");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [875=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public void updateAODKey2File(boolean isOpen) {
        FileOutputStream fops = null;
        Slog.d(TAG, "updateAODKey2File isOpen = " + isOpen);
        try {
            try {
                try {
                    try {
                        fops = new FileOutputStream("/proc/main_gesture_function");
                        fops.write((isOpen ? "fp1" : "fp2").getBytes());
                        fops.flush();
                        fops.close();
                    } catch (Throwable th) {
                        if (fops != null) {
                            try {
                                fops.close();
                            } catch (IOException e) {
                                Slog.d(TAG, "updateAODKey2File IOException");
                            }
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    Slog.d(TAG, "updateAODKey2File IOException e = " + e2);
                    if (fops == null) {
                        return;
                    }
                    fops.close();
                }
            } catch (Exception e3) {
                Slog.d(TAG, "updateAODKey2File Exception");
                if (fops == null) {
                    return;
                }
                fops.close();
            }
        } catch (IOException e4) {
            Slog.d(TAG, "updateAODKey2File IOException");
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends DreamManagerInternal {
        private LocalService() {
        }

        public void startDream(boolean doze) {
            DreamManagerService.this.startDreamInternal(doze);
        }

        public void stopDream(boolean immediate) {
            DreamManagerService.this.stopDreamInternal(immediate, "requested stopDream");
        }

        public boolean isDreaming() {
            return DreamManagerService.this.isDreamingInternal();
        }

        public ComponentName getActiveDreamComponent(boolean doze) {
            return DreamManagerService.this.getActiveDreamComponentInternal(doze);
        }

        public void notifyAodAction(int state) {
            Slog.d(DreamManagerService.TAG, "notifyAodAction state = " + state);
            DreamManagerService.this.notifyAodActionInternal(state);
        }
    }
}
