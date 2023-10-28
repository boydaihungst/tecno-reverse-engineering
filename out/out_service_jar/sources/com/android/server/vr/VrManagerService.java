package com.android.server.vr;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.Vr2dDisplayProperties;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.PackageTagsList;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IVrListener;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.utils.ManagedApplicationService;
import com.android.server.vr.EnabledComponentsObserver;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public class VrManagerService extends SystemService implements EnabledComponentsObserver.EnabledComponentChangeListener, ActivityTaskManagerInternal.ScreenObserver {
    static final boolean DBG = false;
    private static final int EVENT_LOG_SIZE = 64;
    private static final int FLAG_ALL = 7;
    private static final int FLAG_AWAKE = 1;
    private static final int FLAG_KEYGUARD_UNLOCKED = 4;
    private static final int FLAG_NONE = 0;
    private static final int FLAG_SCREEN_ON = 2;
    private static final int INVALID_APPOPS_MODE = -1;
    private static final int MSG_PENDING_VR_STATE_CHANGE = 1;
    private static final int MSG_PERSISTENT_VR_MODE_STATE_CHANGE = 2;
    private static final int MSG_VR_STATE_CHANGE = 0;
    private static final int PENDING_STATE_DELAY_MS = 300;
    public static final String TAG = "VrManagerService";
    private static final ManagedApplicationService.BinderChecker sBinderChecker = new ManagedApplicationService.BinderChecker() { // from class: com.android.server.vr.VrManagerService.3
        @Override // com.android.server.utils.ManagedApplicationService.BinderChecker
        public IInterface asInterface(IBinder binder) {
            return IVrListener.Stub.asInterface(binder);
        }

        @Override // com.android.server.utils.ManagedApplicationService.BinderChecker
        public boolean checkType(IInterface service) {
            return service instanceof IVrListener;
        }
    };
    private boolean mBootsToVr;
    private EnabledComponentsObserver mComponentObserver;
    private Context mContext;
    private ManagedApplicationService mCurrentVrCompositorService;
    private ComponentName mCurrentVrModeComponent;
    private int mCurrentVrModeUser;
    private ManagedApplicationService mCurrentVrService;
    private ComponentName mDefaultVrService;
    private final ManagedApplicationService.EventCallback mEventCallback;
    private boolean mGuard;
    private final Handler mHandler;
    private final Object mLock;
    private boolean mLogLimitHit;
    private final ArrayDeque<ManagedApplicationService.LogFormattable> mLoggingDeque;
    private final NotificationAccessManager mNotifAccessManager;
    private INotificationManager mNotificationManager;
    private final IBinder mOverlayToken;
    private VrState mPendingState;
    private boolean mPersistentVrModeEnabled;
    private final RemoteCallbackList<IPersistentVrStateCallbacks> mPersistentVrStateRemoteCallbacks;
    private int mPreviousCoarseLocationMode;
    private int mPreviousManageOverlayMode;
    private boolean mRunning2dInVr;
    private boolean mStandby;
    private int mSystemSleepFlags;
    private boolean mUseStandbyToExitVrMode;
    private boolean mUserUnlocked;
    private Vr2dDisplay mVr2dDisplay;
    private int mVrAppProcessId;
    private final IVrManager mVrManager;
    private boolean mVrModeAllowed;
    private boolean mVrModeEnabled;
    private final RemoteCallbackList<IVrStateCallbacks> mVrStateRemoteCallbacks;
    private boolean mWasDefaultGranted;

    private static native void initializeNative();

    private static native void setVrModeNative(boolean z);

    private void updateVrModeAllowedLocked() {
        VrState vrState;
        ManagedApplicationService managedApplicationService;
        boolean z = this.mBootsToVr;
        boolean ignoreSleepFlags = z && this.mUseStandbyToExitVrMode;
        boolean disallowedByStandby = this.mStandby && this.mUseStandbyToExitVrMode;
        boolean allowed = (this.mSystemSleepFlags == 7 || ignoreSleepFlags) && this.mUserUnlocked && !disallowedByStandby;
        if (this.mVrModeAllowed != allowed) {
            this.mVrModeAllowed = allowed;
            if (allowed) {
                if (z) {
                    setPersistentVrModeEnabled(true);
                }
                if (this.mBootsToVr && !this.mVrModeEnabled) {
                    setVrMode(true, this.mDefaultVrService, 0, -1, null);
                    return;
                }
                return;
            }
            setPersistentModeAndNotifyListenersLocked(false);
            boolean z2 = this.mVrModeEnabled;
            if (z2 && (managedApplicationService = this.mCurrentVrService) != null) {
                vrState = new VrState(z2, this.mRunning2dInVr, managedApplicationService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            } else {
                vrState = null;
            }
            this.mPendingState = vrState;
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenOn(boolean isScreenOn) {
        setSystemState(2, isScreenOn);
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
    public void onAwakeStateChanged(boolean isAwake) {
        setSystemState(1, isAwake);
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
    public void onKeyguardStateChanged(boolean isShowing) {
        setSystemState(4, !isShowing);
    }

    private void setSystemState(int flags, boolean isOn) {
        synchronized (this.mLock) {
            int oldState = this.mSystemSleepFlags;
            if (isOn) {
                this.mSystemSleepFlags = oldState | flags;
            } else {
                this.mSystemSleepFlags = oldState & (~flags);
            }
            if (oldState != this.mSystemSleepFlags) {
                updateVrModeAllowedLocked();
            }
        }
    }

    private String getStateAsString() {
        return ((this.mSystemSleepFlags & 1) != 0 ? "awake, " : "") + ((this.mSystemSleepFlags & 2) != 0 ? "screen_on, " : "") + ((this.mSystemSleepFlags & 4) != 0 ? "keyguard_off" : "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUserUnlocked() {
        synchronized (this.mLock) {
            this.mUserUnlocked = true;
            updateVrModeAllowedLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStandbyEnabled(boolean standby) {
        synchronized (this.mLock) {
            if (!this.mBootsToVr) {
                Slog.e(TAG, "Attempting to set standby mode on a non-standalone device");
                return;
            }
            this.mStandby = standby;
            updateVrModeAllowedLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SettingEvent implements ManagedApplicationService.LogFormattable {
        public final long timestamp = System.currentTimeMillis();
        public final String what;

        SettingEvent(String what) {
            this.what = what;
        }

        @Override // com.android.server.utils.ManagedApplicationService.LogFormattable
        public String toLogString(SimpleDateFormat dateFormat) {
            return dateFormat.format(new Date(this.timestamp)) + "   " + this.what;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class VrState implements ManagedApplicationService.LogFormattable {
        final ComponentName callingPackage;
        final boolean defaultPermissionsGranted;
        final boolean enabled;
        final int processId;
        final boolean running2dInVr;
        final ComponentName targetPackageName;
        final long timestamp;
        final int userId;

        VrState(boolean enabled, boolean running2dInVr, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage) {
            this.enabled = enabled;
            this.running2dInVr = running2dInVr;
            this.userId = userId;
            this.processId = processId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = false;
            this.timestamp = System.currentTimeMillis();
        }

        VrState(boolean enabled, boolean running2dInVr, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage, boolean defaultPermissionsGranted) {
            this.enabled = enabled;
            this.running2dInVr = running2dInVr;
            this.userId = userId;
            this.processId = processId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = defaultPermissionsGranted;
            this.timestamp = System.currentTimeMillis();
        }

        @Override // com.android.server.utils.ManagedApplicationService.LogFormattable
        public String toLogString(SimpleDateFormat dateFormat) {
            StringBuilder sb = new StringBuilder(dateFormat.format(new Date(this.timestamp)));
            sb.append("  ");
            sb.append("State changed to:");
            sb.append("  ");
            sb.append(this.enabled ? "ENABLED" : "DISABLED");
            sb.append("\n");
            if (this.enabled) {
                sb.append("  ");
                sb.append("User=");
                sb.append(this.userId);
                sb.append("\n");
                sb.append("  ");
                sb.append("Current VR Activity=");
                ComponentName componentName = this.callingPackage;
                sb.append(componentName == null ? "None" : componentName.flattenToString());
                sb.append("\n");
                sb.append("  ");
                sb.append("Bound VrListenerService=");
                ComponentName componentName2 = this.targetPackageName;
                sb.append(componentName2 != null ? componentName2.flattenToString() : "None");
                sb.append("\n");
                if (this.defaultPermissionsGranted) {
                    sb.append("  ");
                    sb.append("Default permissions granted to the bound VrListenerService.");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    /* loaded from: classes2.dex */
    private final class NotificationAccessManager {
        private final SparseArray<ArraySet<String>> mAllowedPackages;
        private final ArrayMap<String, Integer> mNotificationAccessPackageToUserId;

        private NotificationAccessManager() {
            this.mAllowedPackages = new SparseArray<>();
            this.mNotificationAccessPackageToUserId = new ArrayMap<>();
        }

        public void update(Collection<String> packageNames) {
            int currentUserId = ActivityManager.getCurrentUser();
            ArraySet<String> allowed = this.mAllowedPackages.get(currentUserId);
            if (allowed == null) {
                allowed = new ArraySet<>();
            }
            int listenerCount = this.mNotificationAccessPackageToUserId.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                int grantUserId = this.mNotificationAccessPackageToUserId.valueAt(i).intValue();
                if (grantUserId != currentUserId) {
                    String packageName = this.mNotificationAccessPackageToUserId.keyAt(i);
                    VrManagerService.this.revokeNotificationListenerAccess(packageName, grantUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(packageName);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(packageName, grantUserId);
                    this.mNotificationAccessPackageToUserId.removeAt(i);
                }
            }
            Iterator<String> it = allowed.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                if (!packageNames.contains(pkg)) {
                    VrManagerService.this.revokeNotificationListenerAccess(pkg, currentUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(pkg);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(pkg, currentUserId);
                    this.mNotificationAccessPackageToUserId.remove(pkg);
                }
            }
            for (String pkg2 : packageNames) {
                if (!allowed.contains(pkg2)) {
                    VrManagerService.this.grantNotificationPolicyAccess(pkg2);
                    VrManagerService.this.grantNotificationListenerAccess(pkg2, currentUserId);
                    VrManagerService.this.grantCoarseLocationPermissionIfNeeded(pkg2, currentUserId);
                    this.mNotificationAccessPackageToUserId.put(pkg2, Integer.valueOf(currentUserId));
                }
            }
            allowed.clear();
            allowed.addAll(packageNames);
            this.mAllowedPackages.put(currentUserId, allowed);
        }
    }

    @Override // com.android.server.vr.EnabledComponentsObserver.EnabledComponentChangeListener
    public void onEnabledComponentChanged() {
        synchronized (this.mLock) {
            int currentUser = ActivityManager.getCurrentUser();
            ArraySet<ComponentName> enabledListeners = this.mComponentObserver.getEnabled(currentUser);
            ArraySet<String> enabledPackages = new ArraySet<>();
            Iterator<ComponentName> it = enabledListeners.iterator();
            while (it.hasNext()) {
                ComponentName n = it.next();
                String pkg = n.getPackageName();
                if (isDefaultAllowed(pkg)) {
                    enabledPackages.add(n.getPackageName());
                }
            }
            this.mNotifAccessManager.update(enabledPackages);
            if (this.mVrModeAllowed) {
                consumeAndApplyPendingStateLocked(false);
                ManagedApplicationService managedApplicationService = this.mCurrentVrService;
                if (managedApplicationService == null) {
                    return;
                }
                updateCurrentVrServiceLocked(this.mVrModeEnabled, this.mRunning2dInVr, managedApplicationService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceCallerPermissionAnyOf(String... permissions) {
        for (String permission : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(permission) == 0) {
                return;
            }
        }
        throw new SecurityException("Caller does not hold at least one of the permissions: " + Arrays.toString(permissions));
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends VrManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.vr.VrManagerInternal
        public void setVrMode(boolean enabled, ComponentName packageName, int userId, int processId, ComponentName callingPackage) {
            VrManagerService.this.setVrMode(enabled, packageName, userId, processId, callingPackage);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public void onScreenStateChanged(boolean isScreenOn) {
            VrManagerService.this.setScreenOn(isScreenOn);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public boolean isCurrentVrListener(String packageName, int userId) {
            return VrManagerService.this.isCurrentVrListener(packageName, userId);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public int hasVrPackage(ComponentName packageName, int userId) {
            return VrManagerService.this.hasVrPackage(packageName, userId);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public void setPersistentVrModeEnabled(boolean enabled) {
            VrManagerService.this.setPersistentVrModeEnabled(enabled);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public void setVr2dDisplayProperties(Vr2dDisplayProperties compatDisplayProp) {
            VrManagerService.this.setVr2dDisplayProperties(compatDisplayProp);
        }

        @Override // com.android.server.vr.VrManagerInternal
        public int getVr2dDisplayId() {
            return VrManagerService.this.getVr2dDisplayId();
        }

        @Override // com.android.server.vr.VrManagerInternal
        public void addPersistentVrModeStateListener(IPersistentVrStateCallbacks listener) {
            VrManagerService.this.addPersistentStateCallback(listener);
        }
    }

    public VrManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mOverlayToken = new Binder();
        this.mVrStateRemoteCallbacks = new RemoteCallbackList<>();
        this.mPersistentVrStateRemoteCallbacks = new RemoteCallbackList<>();
        this.mPreviousCoarseLocationMode = -1;
        this.mPreviousManageOverlayMode = -1;
        this.mLoggingDeque = new ArrayDeque<>(64);
        this.mNotifAccessManager = new NotificationAccessManager();
        this.mSystemSleepFlags = 5;
        this.mEventCallback = new ManagedApplicationService.EventCallback() { // from class: com.android.server.vr.VrManagerService.1
            @Override // com.android.server.utils.ManagedApplicationService.EventCallback
            public void onServiceEvent(ManagedApplicationService.LogEvent event) {
                ComponentName component;
                VrManagerService.this.logEvent(event);
                synchronized (VrManagerService.this.mLock) {
                    component = VrManagerService.this.mCurrentVrService == null ? null : VrManagerService.this.mCurrentVrService.getComponent();
                    if (component != null && component.equals(event.component) && (event.event == 2 || event.event == 3)) {
                        VrManagerService.this.callFocusedActivityChangedLocked();
                    }
                }
                if (!VrManagerService.this.mBootsToVr && event.event == 4) {
                    if (component == null || component.equals(event.component)) {
                        Slog.e(VrManagerService.TAG, "VrListenerSevice has died permanently, leaving system VR mode.");
                        VrManagerService.this.setPersistentVrModeEnabled(false);
                    }
                }
            }
        };
        this.mHandler = new Handler() { // from class: com.android.server.vr.VrManagerService.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        boolean state = msg.arg1 == 1;
                        int i = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                        while (i > 0) {
                            i--;
                            try {
                                VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(i).onVrStateChanged(state);
                            } catch (RemoteException e) {
                            }
                        }
                        VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                        return;
                    case 1:
                        synchronized (VrManagerService.this.mLock) {
                            if (VrManagerService.this.mVrModeAllowed) {
                                VrManagerService.this.consumeAndApplyPendingStateLocked();
                            }
                        }
                        return;
                    case 2:
                        boolean state2 = msg.arg1 == 1;
                        int i2 = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                        while (i2 > 0) {
                            i2--;
                            try {
                                VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(i2).onPersistentVrStateChanged(state2);
                            } catch (RemoteException e2) {
                            }
                        }
                        VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                        return;
                    default:
                        throw new IllegalStateException("Unknown message type: " + msg.what);
                }
            }
        };
        this.mVrManager = new IVrManager.Stub() { // from class: com.android.server.vr.VrManagerService.4
            public void registerListener(IVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.addStateCallback(cb);
            }

            public void unregisterListener(IVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.removeStateCallback(cb);
            }

            public void registerPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.addPersistentStateCallback(cb);
            }

            public void unregisterPersistentVrStateListener(IPersistentVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.removePersistentStateCallback(cb);
            }

            public boolean getVrModeState() {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                return VrManagerService.this.getVrMode();
            }

            public boolean getPersistentVrModeEnabled() {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                return VrManagerService.this.getPersistentVrMode();
            }

            public void setPersistentVrModeEnabled(boolean enabled) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setPersistentVrModeEnabled(enabled);
            }

            public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProp) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setVr2dDisplayProperties(vr2dDisplayProp);
            }

            public int getVr2dDisplayId() {
                return VrManagerService.this.getVr2dDisplayId();
            }

            public void setAndBindCompositor(String componentName) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setAndBindCompositor(componentName == null ? null : ComponentName.unflattenFromString(componentName));
            }

            public void setStandbyEnabled(boolean standby) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER");
                VrManagerService.this.setStandbyEnabled(standby);
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (DumpUtils.checkDumpPermission(VrManagerService.this.mContext, VrManagerService.TAG, pw)) {
                    pw.println("********* Dump of VrManagerService *********");
                    pw.println("VR mode is currently: " + (VrManagerService.this.mVrModeAllowed ? "allowed" : "disallowed"));
                    pw.println("Persistent VR mode is currently: " + (VrManagerService.this.mPersistentVrModeEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED));
                    pw.println("Currently bound VR listener service: " + (VrManagerService.this.mCurrentVrService == null ? "None" : VrManagerService.this.mCurrentVrService.getComponent().flattenToString()));
                    pw.println("Currently bound VR compositor service: " + (VrManagerService.this.mCurrentVrCompositorService == null ? "None" : VrManagerService.this.mCurrentVrCompositorService.getComponent().flattenToString()));
                    pw.println("Previous state transitions:\n");
                    VrManagerService.this.dumpStateTransitions(pw);
                    pw.println("\n\nRemote Callbacks:");
                    int i = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                    while (true) {
                        int i2 = i - 1;
                        if (i <= 0) {
                            break;
                        }
                        pw.print("  ");
                        pw.print(VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(i2));
                        if (i2 > 0) {
                            pw.println(",");
                        }
                        i = i2;
                    }
                    VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                    pw.println("\n\nPersistent Vr State Remote Callbacks:");
                    int i3 = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                    while (true) {
                        int i4 = i3 - 1;
                        if (i3 <= 0) {
                            break;
                        }
                        pw.print("  ");
                        pw.print(VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(i4));
                        if (i4 > 0) {
                            pw.println(",");
                        }
                        i3 = i4;
                    }
                    VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                    pw.println("\n");
                    pw.println("Installed VrListenerService components:");
                    int userId = VrManagerService.this.mCurrentVrModeUser;
                    ArraySet<ComponentName> installed = VrManagerService.this.mComponentObserver.getInstalled(userId);
                    if (installed == null || installed.size() == 0) {
                        pw.println("None");
                    } else {
                        Iterator<ComponentName> it = installed.iterator();
                        while (it.hasNext()) {
                            ComponentName n = it.next();
                            pw.print("  ");
                            pw.println(n.flattenToString());
                        }
                    }
                    pw.println("Enabled VrListenerService components:");
                    ArraySet<ComponentName> enabled = VrManagerService.this.mComponentObserver.getEnabled(userId);
                    if (enabled == null || enabled.size() == 0) {
                        pw.println("None");
                    } else {
                        Iterator<ComponentName> it2 = enabled.iterator();
                        while (it2.hasNext()) {
                            ComponentName n2 = it2.next();
                            pw.print("  ");
                            pw.println(n2.flattenToString());
                        }
                    }
                    pw.println("\n");
                    pw.println("********* End of VrManagerService Dump *********");
                }
            }
        };
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        synchronized (this.mLock) {
            initializeNative();
            this.mContext = getContext();
        }
        boolean z = false;
        boolean z2 = SystemProperties.getBoolean("ro.boot.vr", false);
        this.mBootsToVr = z2;
        if (z2 && SystemProperties.getBoolean("persist.vr.use_standby_to_exit_vr_mode", true)) {
            z = true;
        }
        this.mUseStandbyToExitVrMode = z;
        publishLocalService(VrManagerInternal.class, new LocalService());
        publishBinderService("vrmanager", this.mVrManager.asBinder());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).registerScreenObserver(this);
            this.mNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            synchronized (this.mLock) {
                Looper looper = Looper.getMainLooper();
                Handler handler = new Handler(looper);
                ArrayList<EnabledComponentsObserver.EnabledComponentChangeListener> listeners = new ArrayList<>();
                listeners.add(this);
                EnabledComponentsObserver build = EnabledComponentsObserver.build(this.mContext, handler, "enabled_vr_listeners", looper, "android.permission.BIND_VR_LISTENER_SERVICE", "android.service.vr.VrListenerService", this.mLock, listeners);
                this.mComponentObserver = build;
                build.rebuildAll();
            }
            ArraySet<ComponentName> defaultVrComponents = SystemConfig.getInstance().getDefaultVrComponents();
            if (defaultVrComponents.size() > 0) {
                this.mDefaultVrService = defaultVrComponents.valueAt(0);
            } else {
                Slog.i(TAG, "No default vr listener service found.");
            }
            DisplayManager dm = (DisplayManager) getContext().getSystemService("display");
            Vr2dDisplay vr2dDisplay = new Vr2dDisplay(dm, (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class), (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class), this.mVrManager);
            this.mVr2dDisplay = vr2dDisplay;
            vr2dDisplay.init(getContext(), this.mBootsToVr);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_UNLOCKED");
            getContext().registerReceiver(new BroadcastReceiver() { // from class: com.android.server.vr.VrManagerService.5
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                        VrManagerService.this.setUserUnlocked();
                    }
                }
            }, intentFilter);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.vr.VrManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VrManagerService.this.m7660lambda$onUserSwitching$0$comandroidservervrVrManagerService();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserSwitching$0$com-android-server-vr-VrManagerService  reason: not valid java name */
    public /* synthetic */ void m7660lambda$onUserSwitching$0$comandroidservervrVrManagerService() {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopped(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    private void updateOverlayStateLocked(String exemptedPackage, int newUserId, int oldUserId) {
        PackageTagsList exemptions;
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        if (oldUserId != newUserId) {
            appOpsManager.setUserRestrictionForUser(24, false, this.mOverlayToken, null, oldUserId);
        }
        if (exemptedPackage == null) {
            exemptions = null;
        } else {
            PackageTagsList exemptions2 = new PackageTagsList.Builder(1).add(exemptedPackage).build();
            exemptions = exemptions2;
        }
        appOpsManager.setUserRestrictionForUser(24, this.mVrModeEnabled, this.mOverlayToken, exemptions, newUserId);
    }

    private void updateDependentAppOpsLocked(String newVrServicePackage, int newUserId, String oldVrServicePackage, int oldUserId) {
        if (Objects.equals(newVrServicePackage, oldVrServicePackage)) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            updateOverlayStateLocked(newVrServicePackage, newUserId, oldUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [994=5] */
    /* JADX WARN: Removed duplicated region for block: B:54:0x010d A[Catch: all -> 0x0136, TryCatch #1 {all -> 0x0136, blocks: (B:52:0x0107, B:54:0x010d, B:55:0x0110, B:57:0x0114, B:59:0x011e, B:62:0x0129, B:64:0x012e), top: B:77:0x0107 }] */
    /* JADX WARN: Removed duplicated region for block: B:57:0x0114 A[Catch: all -> 0x0136, TryCatch #1 {all -> 0x0136, blocks: (B:52:0x0107, B:54:0x010d, B:55:0x0110, B:57:0x0114, B:59:0x011e, B:62:0x0129, B:64:0x012e), top: B:77:0x0107 }] */
    /* JADX WARN: Removed duplicated region for block: B:64:0x012e A[Catch: all -> 0x0136, TRY_LEAVE, TryCatch #1 {all -> 0x0136, blocks: (B:52:0x0107, B:54:0x010d, B:55:0x0110, B:57:0x0114, B:59:0x011e, B:62:0x0129, B:64:0x012e), top: B:77:0x0107 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean updateCurrentVrServiceLocked(boolean enabled, boolean running2dInVr, ComponentName component, int userId, int processId, ComponentName calling) {
        String oldVrServicePackage;
        String newVrServicePackage;
        boolean sendUpdatedCaller;
        ManagedApplicationService managedApplicationService;
        boolean sendUpdatedCaller2 = false;
        long identity = Binder.clearCallingIdentity();
        try {
            boolean goingIntoVrMode = true;
            boolean validUserComponent = this.mComponentObserver.isValid(component, userId) == 0;
            if (!validUserComponent || !enabled) {
                goingIntoVrMode = false;
            }
            if (!this.mVrModeEnabled && !goingIntoVrMode) {
                Binder.restoreCallingIdentity(identity);
                return validUserComponent;
            }
            ManagedApplicationService managedApplicationService2 = this.mCurrentVrService;
            if (managedApplicationService2 != null) {
                try {
                    oldVrServicePackage = managedApplicationService2.getComponent().getPackageName();
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            } else {
                oldVrServicePackage = null;
            }
            int oldUserId = this.mCurrentVrModeUser;
            changeVrModeLocked(goingIntoVrMode);
            boolean nothingChanged = false;
            try {
                try {
                    if (goingIntoVrMode) {
                        sendUpdatedCaller = false;
                        ManagedApplicationService managedApplicationService3 = this.mCurrentVrService;
                        if (managedApplicationService3 == null) {
                            newVrServicePackage = null;
                            createAndConnectService(component, userId);
                            sendUpdatedCaller2 = true;
                        } else if (managedApplicationService3.disconnectIfNotMatching(component, userId)) {
                            Slog.i(TAG, "VR mode component changed to " + component + ", disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                            newVrServicePackage = null;
                            updateCompositorServiceLocked(-10000, null);
                            createAndConnectService(component, userId);
                            sendUpdatedCaller2 = true;
                        } else {
                            newVrServicePackage = null;
                            nothingChanged = true;
                            sendUpdatedCaller2 = false;
                        }
                    } else {
                        try {
                            if (this.mCurrentVrService == null) {
                                nothingChanged = true;
                                newVrServicePackage = null;
                                sendUpdatedCaller2 = (((calling == null || this.mPersistentVrModeEnabled) && !Objects.equals(calling, this.mCurrentVrModeComponent)) || this.mRunning2dInVr != running2dInVr) ? true : true;
                                this.mCurrentVrModeComponent = calling;
                                this.mRunning2dInVr = running2dInVr;
                                this.mVrAppProcessId = processId;
                                if (this.mCurrentVrModeUser != userId) {
                                    this.mCurrentVrModeUser = userId;
                                    sendUpdatedCaller2 = true;
                                }
                                managedApplicationService = this.mCurrentVrService;
                                if (managedApplicationService != null) {
                                    newVrServicePackage = managedApplicationService.getComponent().getPackageName();
                                }
                                int newUserId = this.mCurrentVrModeUser;
                                updateDependentAppOpsLocked(newVrServicePackage, newUserId, oldVrServicePackage, oldUserId);
                                if (this.mCurrentVrService != null && sendUpdatedCaller2) {
                                    callFocusedActivityChangedLocked();
                                }
                                if (!nothingChanged) {
                                    logStateLocked();
                                }
                                Binder.restoreCallingIdentity(identity);
                                return validUserComponent;
                            }
                            sendUpdatedCaller = false;
                            Slog.i(TAG, "Leaving VR mode, disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                            this.mCurrentVrService.disconnect();
                            updateCompositorServiceLocked(-10000, null);
                            this.mCurrentVrService = null;
                            sendUpdatedCaller2 = false;
                            newVrServicePackage = null;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    this.mVrAppProcessId = processId;
                    if (this.mCurrentVrModeUser != userId) {
                    }
                    managedApplicationService = this.mCurrentVrService;
                    if (managedApplicationService != null) {
                    }
                    int newUserId2 = this.mCurrentVrModeUser;
                    updateDependentAppOpsLocked(newVrServicePackage, newUserId2, oldVrServicePackage, oldUserId);
                    if (this.mCurrentVrService != null) {
                        callFocusedActivityChangedLocked();
                    }
                    if (!nothingChanged) {
                    }
                    Binder.restoreCallingIdentity(identity);
                    return validUserComponent;
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
                if (calling == null) {
                }
                this.mCurrentVrModeComponent = calling;
                this.mRunning2dInVr = running2dInVr;
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Throwable th5) {
            th = th5;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void callFocusedActivityChangedLocked() {
        final ComponentName c = this.mCurrentVrModeComponent;
        final boolean b = this.mRunning2dInVr;
        final int pid = this.mVrAppProcessId;
        this.mCurrentVrService.sendEvent(new ManagedApplicationService.PendingEvent() { // from class: com.android.server.vr.VrManagerService.6
            @Override // com.android.server.utils.ManagedApplicationService.PendingEvent
            public void runEvent(IInterface service) throws RemoteException {
                IVrListener l = (IVrListener) service;
                l.focusedActivityChanged(c, b, pid);
            }
        });
    }

    private boolean isDefaultAllowed(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfo(packageName, 128);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (info != null) {
            if (!info.isSystemApp() && !info.isUpdatedSystemApp()) {
                return false;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantNotificationPolicyAccess(String pkg) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        nm.setNotificationPolicyAccessGranted(pkg, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void revokeNotificationPolicyAccess(String pkg) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        nm.removeAutomaticZenRules(pkg);
        nm.setNotificationPolicyAccessGranted(pkg, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantNotificationListenerAccess(String pkg, int userId) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        PackageManager pm = this.mContext.getPackageManager();
        ArraySet<ComponentName> possibleServices = EnabledComponentsObserver.loadComponentNames(pm, userId, "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE");
        Iterator<ComponentName> it = possibleServices.iterator();
        while (it.hasNext()) {
            ComponentName c = it.next();
            if (Objects.equals(c.getPackageName(), pkg)) {
                nm.setNotificationListenerAccessGrantedForUser(c, userId, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void revokeNotificationListenerAccess(String pkg, int userId) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        List<ComponentName> current = nm.getEnabledNotificationListeners(userId);
        for (ComponentName component : current) {
            if (component != null && component.getPackageName().equals(pkg)) {
                nm.setNotificationListenerAccessGrantedForUser(component, userId, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantCoarseLocationPermissionIfNeeded(String pkg, int userId) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", pkg, userId)) {
            try {
                this.mContext.getPackageManager().grantRuntimePermission(pkg, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(userId));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Could not grant coarse location permission, package " + pkg + " was removed.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void revokeCoarseLocationPermissionIfNeeded(String pkg, int userId) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", pkg, userId)) {
            try {
                this.mContext.getPackageManager().revokeRuntimePermission(pkg, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(userId));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Could not revoke coarse location permission, package " + pkg + " was removed.");
            }
        }
    }

    private boolean isPermissionUserUpdated(String permission, String pkg, int userId) {
        int flags = this.mContext.getPackageManager().getPermissionFlags(permission, pkg, new UserHandle(userId));
        return (flags & 3) != 0;
    }

    private ArraySet<String> getNotificationListeners(ContentResolver resolver, int userId) {
        String flat = Settings.Secure.getStringForUser(resolver, "enabled_notification_listeners", userId);
        ArraySet<String> current = new ArraySet<>();
        if (flat != null) {
            String[] allowed = flat.split(":");
            for (String s : allowed) {
                if (!TextUtils.isEmpty(s)) {
                    current.add(s);
                }
            }
        }
        return current;
    }

    private static String formatSettings(Collection<String> c) {
        if (c == null || c.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean start = true;
        for (String s : c) {
            if (!"".equals(s)) {
                if (!start) {
                    b.append(':');
                }
                b.append(s);
                start = false;
            }
        }
        return b.toString();
    }

    private void createAndConnectService(ComponentName component, int userId) {
        ManagedApplicationService createVrListenerService = createVrListenerService(component, userId);
        this.mCurrentVrService = createVrListenerService;
        createVrListenerService.connect();
        Slog.i(TAG, "Connecting " + component + " for user " + userId);
    }

    private void changeVrModeLocked(boolean enabled) {
        if (this.mVrModeEnabled != enabled) {
            this.mVrModeEnabled = enabled;
            Slog.i(TAG, "VR mode " + (this.mVrModeEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED));
            setVrModeNative(this.mVrModeEnabled);
            onVrModeChangedLocked();
        }
    }

    private void onVrModeChangedLocked() {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(0, this.mVrModeEnabled ? 1 : 0, 0));
    }

    private ManagedApplicationService createVrListenerService(ComponentName component, int userId) {
        int retryType = this.mBootsToVr ? 1 : 2;
        return ManagedApplicationService.build(this.mContext, component, userId, 17041720, "android.settings.VR_LISTENER_SETTINGS", sBinderChecker, true, retryType, this.mHandler, this.mEventCallback);
    }

    private ManagedApplicationService createVrCompositorService(ComponentName component, int userId) {
        int retryType = this.mBootsToVr ? 1 : 3;
        return ManagedApplicationService.build(this.mContext, component, userId, 0, null, null, true, retryType, this.mHandler, this.mEventCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void consumeAndApplyPendingStateLocked() {
        consumeAndApplyPendingStateLocked(true);
    }

    private void consumeAndApplyPendingStateLocked(boolean disconnectIfNoPendingState) {
        VrState vrState = this.mPendingState;
        if (vrState != null) {
            updateCurrentVrServiceLocked(vrState.enabled, this.mPendingState.running2dInVr, this.mPendingState.targetPackageName, this.mPendingState.userId, this.mPendingState.processId, this.mPendingState.callingPackage);
            this.mPendingState = null;
        } else if (disconnectIfNoPendingState) {
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void logStateLocked() {
        ManagedApplicationService managedApplicationService = this.mCurrentVrService;
        ComponentName currentBoundService = managedApplicationService == null ? null : managedApplicationService.getComponent();
        logEvent(new VrState(this.mVrModeEnabled, this.mRunning2dInVr, currentBoundService, this.mCurrentVrModeUser, this.mVrAppProcessId, this.mCurrentVrModeComponent, this.mWasDefaultGranted));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logEvent(ManagedApplicationService.LogFormattable event) {
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 64) {
                this.mLoggingDeque.removeFirst();
                this.mLogLimitHit = true;
            }
            this.mLoggingDeque.add(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpStateTransitions(PrintWriter pw) {
        SimpleDateFormat d = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 0) {
                pw.print("  ");
                pw.println("None");
            }
            if (this.mLogLimitHit) {
                pw.println("...");
            }
            Iterator<ManagedApplicationService.LogFormattable> it = this.mLoggingDeque.iterator();
            while (it.hasNext()) {
                ManagedApplicationService.LogFormattable event = it.next();
                pw.println(event.toLogString(d));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:19:0x001f A[Catch: all -> 0x0010, TryCatch #0 {all -> 0x0010, blocks: (B:6:0x0009, B:15:0x0018, B:19:0x001f, B:21:0x0028, B:23:0x003e, B:24:0x0040, B:27:0x0044, B:29:0x0048, B:31:0x004c, B:32:0x0053, B:33:0x0055, B:35:0x0057, B:36:0x0070), top: B:40:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0024  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x003e A[Catch: all -> 0x0010, TryCatch #0 {all -> 0x0010, blocks: (B:6:0x0009, B:15:0x0018, B:19:0x001f, B:21:0x0028, B:23:0x003e, B:24:0x0040, B:27:0x0044, B:29:0x0048, B:31:0x004c, B:32:0x0053, B:33:0x0055, B:35:0x0057, B:36:0x0070), top: B:40:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0042  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setVrMode(boolean enabled, ComponentName targetPackageName, int userId, int processId, ComponentName callingPackage) {
        boolean z;
        ComponentName targetListener;
        synchronized (this.mLock) {
            boolean running2dInVr = false;
            if (!enabled) {
                try {
                    if (!this.mPersistentVrModeEnabled) {
                        z = false;
                        boolean targetEnabledState = z;
                        if (!enabled && this.mPersistentVrModeEnabled) {
                            running2dInVr = true;
                        }
                        if (!running2dInVr) {
                            targetListener = this.mDefaultVrService;
                        } else {
                            targetListener = targetPackageName;
                        }
                        VrState pending = new VrState(targetEnabledState, running2dInVr, targetListener, userId, processId, callingPackage);
                        if (this.mVrModeAllowed) {
                            this.mPendingState = pending;
                            return;
                        } else if (!targetEnabledState && this.mCurrentVrService != null) {
                            if (this.mPendingState == null) {
                                this.mHandler.sendEmptyMessageDelayed(1, 300L);
                            }
                            this.mPendingState = pending;
                            return;
                        } else {
                            this.mHandler.removeMessages(1);
                            this.mPendingState = null;
                            updateCurrentVrServiceLocked(targetEnabledState, running2dInVr, targetListener, userId, processId, callingPackage);
                            return;
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            z = true;
            boolean targetEnabledState2 = z;
            if (!enabled) {
                running2dInVr = true;
            }
            if (!running2dInVr) {
            }
            VrState pending2 = new VrState(targetEnabledState2, running2dInVr, targetListener, userId, processId, callingPackage);
            if (this.mVrModeAllowed) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPersistentVrModeEnabled(boolean enabled) {
        synchronized (this.mLock) {
            setPersistentModeAndNotifyListenersLocked(enabled);
            if (!enabled) {
                setVrMode(false, null, 0, -1, null);
            }
        }
    }

    public void setVr2dDisplayProperties(Vr2dDisplayProperties compatDisplayProp) {
        long token = Binder.clearCallingIdentity();
        try {
            Vr2dDisplay vr2dDisplay = this.mVr2dDisplay;
            if (vr2dDisplay != null) {
                vr2dDisplay.setVirtualDisplayProperties(compatDisplayProp);
                return;
            }
            Binder.restoreCallingIdentity(token);
            Slog.w(TAG, "Vr2dDisplay is null!");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getVr2dDisplayId() {
        Vr2dDisplay vr2dDisplay = this.mVr2dDisplay;
        if (vr2dDisplay != null) {
            return vr2dDisplay.getVirtualDisplayId();
        }
        Slog.w(TAG, "Vr2dDisplay is null!");
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAndBindCompositor(ComponentName componentName) {
        int userId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                updateCompositorServiceLocked(userId, componentName);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void updateCompositorServiceLocked(int userId, ComponentName componentName) {
        ManagedApplicationService managedApplicationService = this.mCurrentVrCompositorService;
        if (managedApplicationService != null && managedApplicationService.disconnectIfNotMatching(componentName, userId)) {
            Slog.i(TAG, "Disconnecting compositor service: " + this.mCurrentVrCompositorService.getComponent());
            this.mCurrentVrCompositorService = null;
        }
        if (componentName != null && this.mCurrentVrCompositorService == null) {
            Slog.i(TAG, "Connecting compositor service: " + componentName);
            ManagedApplicationService createVrCompositorService = createVrCompositorService(componentName, userId);
            this.mCurrentVrCompositorService = createVrCompositorService;
            createVrCompositorService.connect();
        }
    }

    private void setPersistentModeAndNotifyListenersLocked(boolean enabled) {
        if (this.mPersistentVrModeEnabled == enabled) {
            return;
        }
        String eventName = "Persistent VR mode " + (enabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        Slog.i(TAG, eventName);
        logEvent(new SettingEvent(eventName));
        this.mPersistentVrModeEnabled = enabled;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(2, enabled ? 1 : 0, 0));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int hasVrPackage(ComponentName targetPackageName, int userId) {
        int isValid;
        synchronized (this.mLock) {
            isValid = this.mComponentObserver.isValid(targetPackageName, userId);
        }
        return isValid;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCurrentVrListener(String packageName, int userId) {
        synchronized (this.mLock) {
            ManagedApplicationService managedApplicationService = this.mCurrentVrService;
            boolean z = false;
            if (managedApplicationService == null) {
                return false;
            }
            if (managedApplicationService.getComponent().getPackageName().equals(packageName) && userId == this.mCurrentVrService.getUserId()) {
                z = true;
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addStateCallback(IVrStateCallbacks cb) {
        this.mVrStateRemoteCallbacks.register(cb);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeStateCallback(IVrStateCallbacks cb) {
        this.mVrStateRemoteCallbacks.unregister(cb);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addPersistentStateCallback(IPersistentVrStateCallbacks cb) {
        this.mPersistentVrStateRemoteCallbacks.register(cb);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePersistentStateCallback(IPersistentVrStateCallbacks cb) {
        this.mPersistentVrStateRemoteCallbacks.unregister(cb);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mVrModeEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getPersistentVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPersistentVrModeEnabled;
        }
        return z;
    }
}
