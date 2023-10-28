package com.android.server.statusbar;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.ITransientNotificationCallback;
import android.app.Notification;
import android.app.compat.CompatChanges;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.hardware.biometrics.IBiometricContextListener;
import android.hardware.biometrics.IBiometricSysuiReceiver;
import android.hardware.biometrics.PromptInfo;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.IUdfpsHbmListener;
import android.media.INearbyMediaDevicesProvider;
import android.media.MediaRoute2Info;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InsetsVisibilities;
import com.android.internal.logging.InstanceId;
import com.android.internal.os.TransferPipe;
import com.android.internal.statusbar.IAddTileResultCallback;
import com.android.internal.statusbar.ISessionListener;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IUndoMediaTransferCallback;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.RegisterStatusBarResult;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.GcUtils;
import com.android.internal.view.AppearanceRegion;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.notification.NotificationDelegate;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.power.ShutdownCheckPoints;
import com.android.server.power.ShutdownThread;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class StatusBarManagerService extends IStatusBarService.Stub implements DisplayManager.DisplayListener {
    private static final long LOCK_DOWN_COLLAPSE_STATUS_BAR = 173031413;
    static final long REQUEST_LISTENING_MUST_MATCH_PACKAGE = 172251878;
    private static final boolean SPEW = false;
    private static final String TAG = "StatusBarManagerService";
    private boolean debug_on;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final ActivityTaskManagerInternal mActivityTaskManager;
    private volatile IStatusBar mBar;
    private IBiometricContextListener mBiometricContextListener;
    private final Context mContext;
    private final ArrayMap<String, Long> mCurrentRequestAddTilePackages;
    private int mCurrentUserId;
    private final DeathRecipient mDeathRecipient;
    private final ArrayList<DisableRecord> mDisableRecords;
    private final SparseArray<UiState> mDisplayUiState;
    private GlobalActionsProvider.GlobalActionsListener mGlobalActionListener;
    private final GlobalActionsProvider mGlobalActionsProvider;
    private final Handler mHandler;
    private final ArrayMap<String, StatusBarIcon> mIcons;
    private final StatusBarManagerInternal mInternalService;
    private final Object mLock;
    private NotificationDelegate mNotificationDelegate;
    private IOverlayManager mOverlayManager;
    private final PackageManagerInternal mPackageManagerInternal;
    private final SessionMonitor mSessionMonitor;
    private final IBinder mSysUiVisToken;
    private final TileRequestTracker mTileRequestTracker;
    private boolean mTracingEnabled;
    private IUdfpsHbmListener mUdfpsHbmListener;
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.statusbar.log", false);
    private static final long REQUEST_TIME_OUT = TimeUnit.MINUTES.toNanos(5);

    /* loaded from: classes2.dex */
    private class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            StatusBarManagerService.this.mBar.asBinder().unlinkToDeath(this, 0);
            StatusBarManagerService.this.mBar = null;
            StatusBarManagerService.this.notifyBarAttachChanged();
        }

        public void linkToDeath() {
            try {
                StatusBarManagerService.this.mBar.asBinder().linkToDeath(StatusBarManagerService.this.mDeathRecipient, 0);
            } catch (RemoteException e) {
                Slog.e(StatusBarManagerService.TAG, "Unable to register Death Recipient for status bar", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisableRecord implements IBinder.DeathRecipient {
        String pkg;
        IBinder token;
        int userId;
        int what1;
        int what2;

        public DisableRecord(int userId, IBinder token) {
            this.userId = userId;
            this.token = token;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.i(StatusBarManagerService.TAG, "binder died for pkg=" + this.pkg);
            StatusBarManagerService.this.disableForUser(0, this.token, this.pkg, this.userId);
            StatusBarManagerService.this.disable2ForUser(0, this.token, this.pkg, this.userId);
            this.token.unlinkToDeath(this, 0);
        }

        public void setFlags(int what, int which, String pkg) {
            switch (which) {
                case 1:
                    this.what1 = what;
                    break;
                case 2:
                    this.what2 = what;
                    break;
                default:
                    Slog.w(StatusBarManagerService.TAG, "Can't set unsupported disable flag " + which + ": 0x" + Integer.toHexString(what));
                    break;
            }
            this.pkg = pkg;
        }

        public int getFlags(int which) {
            switch (which) {
                case 1:
                    return this.what1;
                case 2:
                    return this.what2;
                default:
                    Slog.w(StatusBarManagerService.TAG, "Can't get unsupported disable flag " + which);
                    return 0;
            }
        }

        public boolean isEmpty() {
            return this.what1 == 0 && this.what2 == 0;
        }

        public String toString() {
            return String.format("userId=%d what1=0x%08X what2=0x%08X pkg=%s token=%s", Integer.valueOf(this.userId), Integer.valueOf(this.what1), Integer.valueOf(this.what2), this.pkg, this.token);
        }
    }

    public StatusBarManagerService(Context context) {
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mIcons = new ArrayMap<>();
        this.mDisableRecords = new ArrayList<>();
        this.mSysUiVisToken = new Binder();
        this.mLock = new Object();
        this.mDeathRecipient = new DeathRecipient();
        SparseArray<UiState> sparseArray = new SparseArray<>();
        this.mDisplayUiState = sparseArray;
        this.debug_on = "1".equals(SystemProperties.get("persist.sys.adb.support", "0")) || "1".equals(SystemProperties.get("persist.sys.fans.support", "0"));
        this.mCurrentRequestAddTilePackages = new ArrayMap<>();
        StatusBarManagerInternal statusBarManagerInternal = new StatusBarManagerInternal() { // from class: com.android.server.statusbar.StatusBarManagerService.1
            private boolean mNotificationLightOn;

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setNotificationDelegate(NotificationDelegate delegate) {
                StatusBarManagerService.this.mNotificationDelegate = delegate;
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showScreenPinningRequest(int taskId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showScreenPinningRequest(taskId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showAssistDisclosure() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showAssistDisclosure();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void startAssist(Bundle args) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.startAssist(args);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onCameraLaunchGestureDetected(int source) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onCameraLaunchGestureDetected(source);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onEmergencyActionLaunchGestureDetected() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onEmergencyActionLaunchGestureDetected();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setDisableFlags(int displayId, int flags, String cause) {
                StatusBarManagerService.this.setDisableFlags(displayId, flags, cause);
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void toggleSplitScreen() {
                StatusBarManagerService.this.enforceStatusBarService();
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.toggleSplitScreen();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void appTransitionFinished(int displayId) {
                StatusBarManagerService.this.enforceStatusBarService();
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.appTransitionFinished(displayId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void toggleRecentApps() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.toggleRecentApps();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setCurrentUser(int newUserId) {
                StatusBarManagerService.this.mCurrentUserId = newUserId;
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void preloadRecentApps() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.preloadRecentApps();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void cancelPreloadRecentApps() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.cancelPreloadRecentApps();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showRecentApps(boolean triggeredFromAltTab) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showRecentApps(triggeredFromAltTab);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.hideRecentApps(triggeredFromAltTab, triggeredFromHomeKey);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void collapsePanels() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.animateCollapsePanels();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void dismissKeyboardShortcutsMenu() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.dismissKeyboardShortcutsMenu();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void toggleKeyboardShortcutsMenu(int deviceId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.toggleKeyboardShortcutsMenu(deviceId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showChargingAnimation(int batteryLevel) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showWirelessChargingAnimation(batteryLevel);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showPictureInPictureMenu() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showPictureInPictureMenu();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setWindowState(int displayId, int window, int state) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        if (StatusBarManagerService.DEBUG) {
                            Log.d(StatusBarManagerService.TAG, Log.getStackTraceString(new Throwable()));
                            int pid = Binder.getCallingPid();
                            int uid = Binder.getCallingUid();
                            String callingApp = StatusBarManagerService.this.mContext.getPackageManager().getNameForUid(uid);
                            Log.d(StatusBarManagerService.TAG, "callingApp = " + callingApp + ", window = " + window + ", state = " + state + ", pid = " + pid);
                        }
                        StatusBarManagerService.this.mBar.setWindowState(displayId, window, state);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void appTransitionPending(int displayId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.appTransitionPending(displayId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void appTransitionCancelled(int displayId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.appTransitionCancelled(displayId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void appTransitionStarting(int displayId, long statusBarAnimationsStartTime, long statusBarAnimationsDuration) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.appTransitionStarting(displayId, statusBarAnimationsStartTime, statusBarAnimationsDuration);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setTopAppHidesStatusBar(boolean hidesStatusBar) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.setTopAppHidesStatusBar(hidesStatusBar);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public boolean showShutdownUi(boolean isReboot, String reason) {
                if (StatusBarManagerService.this.mContext.getResources().getBoolean(17891751) && StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showShutdownUi(isReboot, reason);
                        return true;
                    } catch (RemoteException e) {
                    }
                }
                return false;
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onProposedRotationChanged(int rotation, boolean isValid) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onProposedRotationChanged(rotation, isValid);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onDisplayReady(int displayId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onDisplayReady(displayId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onRecentsAnimationStateChanged(boolean running) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onRecentsAnimationStateChanged(running);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void onSystemBarAttributesChanged(int displayId, int appearance, AppearanceRegion[] appearanceRegions, boolean navbarColorManagedByIme, int behavior, InsetsVisibilities requestedVisibilities, String packageName) {
                StatusBarManagerService.this.getUiState(displayId).setBarAttributes(appearance, appearanceRegions, navbarColorManagedByIme, behavior, requestedVisibilities, packageName);
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.onSystemBarAttributesChanged(displayId, appearance, appearanceRegions, navbarColorManagedByIme, behavior, requestedVisibilities, packageName);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showTransient(int displayId, int[] types, boolean isGestureOnSystemBar) {
                StatusBarManagerService.this.getUiState(displayId).showTransient(types);
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showTransient(displayId, types, isGestureOnSystemBar);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void abortTransient(int displayId, int[] types) {
                StatusBarManagerService.this.getUiState(displayId).clearTransient(types);
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.abortTransient(displayId, types);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void showToast(int uid, String packageName, IBinder token, CharSequence text, IBinder windowToken, int duration, ITransientNotificationCallback callback, int displayId) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showToast(uid, packageName, token, text, windowToken, duration, callback, displayId);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void hideToast(String packageName, IBinder token) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.hideToast(packageName, token);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public boolean requestWindowMagnificationConnection(boolean request) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.requestWindowMagnificationConnection(request);
                        return true;
                    } catch (RemoteException e) {
                        return false;
                    }
                }
                return false;
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void handleWindowManagerLoggingCommand(String[] args, ParcelFileDescriptor outFd) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.handleWindowManagerLoggingCommand(args, outFd);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setNavigationBarLumaSamplingEnabled(int displayId, boolean enable) {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.setNavigationBarLumaSamplingEnabled(displayId, enable);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setUdfpsHbmListener(IUdfpsHbmListener listener) {
                synchronized (StatusBarManagerService.this.mLock) {
                    StatusBarManagerService.this.mUdfpsHbmListener = listener;
                }
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.setUdfpsHbmListener(listener);
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // com.android.server.statusbar.StatusBarManagerInternal
            public void setNotificationRanking(Map config) {
            }
        };
        this.mInternalService = statusBarManagerInternal;
        GlobalActionsProvider globalActionsProvider = new GlobalActionsProvider() { // from class: com.android.server.statusbar.StatusBarManagerService.2
            @Override // com.android.server.policy.GlobalActionsProvider
            public boolean isGlobalActionsDisabled() {
                int disabled2 = ((UiState) StatusBarManagerService.this.mDisplayUiState.get(0)).getDisabled2();
                return (disabled2 & 8) != 0;
            }

            @Override // com.android.server.policy.GlobalActionsProvider
            public void setGlobalActionsListener(GlobalActionsProvider.GlobalActionsListener listener) {
                StatusBarManagerService.this.mGlobalActionListener = listener;
                StatusBarManagerService.this.mGlobalActionListener.onGlobalActionsAvailableChanged(StatusBarManagerService.this.mBar != null);
            }

            @Override // com.android.server.policy.GlobalActionsProvider
            public void showGlobalActions() {
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.showGlobalActionsMenu();
                    } catch (RemoteException e) {
                    }
                }
            }
        };
        this.mGlobalActionsProvider = globalActionsProvider;
        this.mContext = context;
        LocalServices.addService(StatusBarManagerInternal.class, statusBarManagerInternal);
        LocalServices.addService(GlobalActionsProvider.class, globalActionsProvider);
        UiState state = new UiState();
        sparseArray.put(0, state);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        displayManager.registerDisplayListener(this, handler);
        this.mActivityTaskManager = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mTileRequestTracker = new TileRequestTracker(context);
        this.mSessionMonitor = new SessionMonitor(context);
    }

    private IOverlayManager getOverlayManager() {
        if (this.mOverlayManager == null) {
            IOverlayManager asInterface = IOverlayManager.Stub.asInterface(ServiceManager.getService(ParsingPackageUtils.TAG_OVERLAY));
            this.mOverlayManager = asInterface;
            if (asInterface == null) {
                Slog.w("StatusBarManager", "warning: no OVERLAY_SERVICE");
            }
        }
        return this.mOverlayManager;
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayAdded(int displayId) {
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayRemoved(int displayId) {
        synchronized (this.mLock) {
            this.mDisplayUiState.remove(displayId);
        }
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayChanged(int displayId) {
    }

    private boolean isDisable2FlagSet(int target2) {
        int disabled2 = this.mDisplayUiState.get(0).getDisabled2();
        return (disabled2 & target2) == target2;
    }

    public void expandNotificationsPanel() {
        enforceExpandStatusBar();
        if (!isDisable2FlagSet(4) && this.mBar != null) {
            try {
                this.mBar.animateExpandNotificationsPanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void collapsePanels() {
        if (checkCanCollapseStatusBar("collapsePanels") && this.mBar != null) {
            try {
                this.mBar.animateCollapsePanels();
            } catch (RemoteException e) {
            }
        }
    }

    public void togglePanel() {
        if (checkCanCollapseStatusBar("togglePanel") && !isDisable2FlagSet(4) && this.mBar != null) {
            try {
                this.mBar.togglePanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void expandSettingsPanel(String subPanel) {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandSettingsPanel(subPanel);
            } catch (RemoteException e) {
            }
        }
    }

    public void addTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.addQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void remTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.remQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void clickTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.clickQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void handleSystemKey(int key) throws RemoteException {
        if (checkCanCollapseStatusBar("handleSystemKey") && this.mBar != null) {
            try {
                this.mBar.handleSystemKey(key);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEnterExitToast(boolean entering) throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEnterExitToast(entering);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEscapeToast() throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEscapeToast();
            } catch (RemoteException e) {
            }
        }
    }

    public void showAuthenticationDialog(PromptInfo promptInfo, IBiometricSysuiReceiver receiver, int[] sensorIds, boolean credentialAllowed, boolean requireConfirmation, int userId, long operationId, String opPackageName, long requestId, int multiSensorConfig) {
        enforceBiometricDialog();
        if (this.mBar != null) {
            try {
                this.mBar.showAuthenticationDialog(promptInfo, receiver, sensorIds, credentialAllowed, requireConfirmation, userId, operationId, opPackageName, requestId, multiSensorConfig);
            } catch (RemoteException e) {
            }
        }
    }

    public void onBiometricAuthenticated(int modality) {
        enforceBiometricDialog();
        if (this.mBar != null) {
            try {
                this.mBar.onBiometricAuthenticated(modality);
            } catch (RemoteException e) {
            }
        }
    }

    public void onBiometricHelp(int modality, String message) {
        enforceBiometricDialog();
        if (this.mBar != null) {
            try {
                this.mBar.onBiometricHelp(modality, message);
            } catch (RemoteException e) {
            }
        }
    }

    public void onBiometricError(int modality, int error, int vendorCode) {
        enforceBiometricDialog();
        if (this.mBar != null) {
            try {
                this.mBar.onBiometricError(modality, error, vendorCode);
            } catch (RemoteException e) {
            }
        }
    }

    public void hideAuthenticationDialog(long requestId) {
        enforceBiometricDialog();
        if (this.mBar != null) {
            try {
                this.mBar.hideAuthenticationDialog(requestId);
            } catch (RemoteException e) {
            }
        }
    }

    public void setBiometicContextListener(IBiometricContextListener listener) {
        enforceStatusBarService();
        synchronized (this.mLock) {
            this.mBiometricContextListener = listener;
        }
        if (this.mBar != null) {
            try {
                this.mBar.setBiometicContextListener(listener);
            } catch (RemoteException e) {
            }
        }
    }

    public void setUdfpsHbmListener(IUdfpsHbmListener listener) {
        enforceStatusBarService();
        if (this.mBar != null) {
            try {
                this.mBar.setUdfpsHbmListener(listener);
            } catch (RemoteException e) {
            }
        }
    }

    public void startTracing() {
        if (this.mBar != null) {
            try {
                this.mBar.startTracing();
                this.mTracingEnabled = true;
            } catch (RemoteException e) {
            }
        }
    }

    public void stopTracing() {
        if (this.mBar != null) {
            try {
                this.mTracingEnabled = false;
                this.mBar.stopTracing();
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isTracing() {
        return this.mTracingEnabled;
    }

    public void disable(int what, IBinder token, String pkg) {
        disableForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disableForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(0, userId, what, token, pkg, 1);
        }
    }

    public void disable2(int what, IBinder token, String pkg) {
        disable2ForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disable2ForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(0, userId, what, token, pkg, 2);
        }
    }

    private void disableLocked(int displayId, int userId, int what, IBinder token, String pkg, int whichFlag) {
        manageDisableListLocked(userId, what, token, pkg, whichFlag);
        final int net1 = gatherDisableActionsLocked(this.mCurrentUserId, 1);
        int net2 = gatherDisableActionsLocked(this.mCurrentUserId, 2);
        UiState state = getUiState(displayId);
        if (!state.disableEquals(net1, net2)) {
            state.setDisabled(net1, net2);
            this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarManagerService.this.m6673x609bf840(net1);
                }
            });
            if (this.mBar != null) {
                try {
                    if (this.debug_on) {
                        Log.d("TAG", "net1 = " + net1 + " , net2 = " + net2 + ", mCurrentUserId = " + this.mCurrentUserId);
                    }
                    this.mBar.disable(displayId, net1, net2);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disableLocked$0$com-android-server-statusbar-StatusBarManagerService  reason: not valid java name */
    public /* synthetic */ void m6673x609bf840(int net1) {
        this.mNotificationDelegate.onSetDisabled(net1);
    }

    public int[] getDisableFlags(IBinder token, int userId) {
        enforceStatusBar();
        int disable1 = 0;
        int disable2 = 0;
        synchronized (this.mLock) {
            DisableRecord record = (DisableRecord) findMatchingRecordLocked(token, userId).second;
            if (record != null) {
                disable1 = record.what1;
                disable2 = record.what2;
            }
        }
        return new int[]{disable1, disable2};
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runGcForTest() {
        if (!Build.IS_DEBUGGABLE) {
            throw new SecurityException("runGcForTest requires a debuggable build");
        }
        GcUtils.runGcAndFinalizersSync();
        if (this.mBar != null) {
            try {
                this.mBar.runGcForTest();
            } catch (RemoteException e) {
            }
        }
    }

    public void setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = new StatusBarIcon(iconPackage, UserHandle.SYSTEM, iconId, iconLevel, 0, contentDescription);
            this.mIcons.put(slot, icon);
            if (this.mBar != null) {
                try {
                    this.mBar.setIcon(slot, icon);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIconVisibility(String slot, boolean visibility) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = this.mIcons.get(slot);
            if (icon == null) {
                return;
            }
            if (icon.visible != visibility) {
                icon.visible = visibility;
                if (this.mBar != null) {
                    try {
                        this.mBar.setIcon(slot, icon);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void removeIcon(String slot) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            this.mIcons.remove(slot);
            if (this.mBar != null) {
                try {
                    this.mBar.removeIcon(slot);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setImeWindowStatus(final int displayId, final IBinder token, final int vis, final int backDisposition, final boolean showImeSwitcher) {
        enforceStatusBar();
        synchronized (this.mLock) {
            getUiState(displayId).setImeWindowState(vis, backDisposition, showImeSwitcher, token);
            this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarManagerService.this.m6677xa26bf516(displayId, token, vis, backDisposition, showImeSwitcher);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setImeWindowStatus$1$com-android-server-statusbar-StatusBarManagerService  reason: not valid java name */
    public /* synthetic */ void m6677xa26bf516(int displayId, IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.setImeWindowStatus(displayId, token, vis, backDisposition, showImeSwitcher);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisableFlags(int displayId, int flags, String cause) {
        enforceStatusBarService();
        int unknownFlags = (-134152193) & flags;
        if (unknownFlags != 0) {
            Slog.e(TAG, "Unknown disable flags: 0x" + Integer.toHexString(unknownFlags), new RuntimeException());
        }
        synchronized (this.mLock) {
            disableLocked(displayId, this.mCurrentUserId, flags, this.mSysUiVisToken, cause, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UiState getUiState(int displayId) {
        UiState state = this.mDisplayUiState.get(displayId);
        if (state == null) {
            UiState state2 = new UiState();
            this.mDisplayUiState.put(displayId, state2);
            return state2;
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class UiState {
        private int mAppearance;
        private AppearanceRegion[] mAppearanceRegions;
        private int mBehavior;
        private int mDisabled1;
        private int mDisabled2;
        private int mImeBackDisposition;
        private IBinder mImeToken;
        private int mImeWindowVis;
        private boolean mNavbarColorManagedByIme;
        private String mPackageName;
        private InsetsVisibilities mRequestedVisibilities;
        private boolean mShowImeSwitcher;
        private final ArraySet<Integer> mTransientBarTypes;

        private UiState() {
            this.mAppearance = 0;
            this.mAppearanceRegions = new AppearanceRegion[0];
            this.mTransientBarTypes = new ArraySet<>();
            this.mNavbarColorManagedByIme = false;
            this.mRequestedVisibilities = new InsetsVisibilities();
            this.mPackageName = "none";
            this.mDisabled1 = 0;
            this.mDisabled2 = 0;
            this.mImeWindowVis = 0;
            this.mImeBackDisposition = 0;
            this.mShowImeSwitcher = false;
            this.mImeToken = null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setBarAttributes(int appearance, AppearanceRegion[] appearanceRegions, boolean navbarColorManagedByIme, int behavior, InsetsVisibilities requestedVisibilities, String packageName) {
            this.mAppearance = appearance;
            this.mAppearanceRegions = appearanceRegions;
            this.mNavbarColorManagedByIme = navbarColorManagedByIme;
            this.mBehavior = behavior;
            this.mRequestedVisibilities = requestedVisibilities;
            this.mPackageName = packageName;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void showTransient(int[] types) {
            for (int type : types) {
                this.mTransientBarTypes.add(Integer.valueOf(type));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clearTransient(int[] types) {
            for (int type : types) {
                this.mTransientBarTypes.remove(Integer.valueOf(type));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getDisabled1() {
            return this.mDisabled1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getDisabled2() {
            return this.mDisabled2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setDisabled(int disabled1, int disabled2) {
            this.mDisabled1 = disabled1;
            this.mDisabled2 = disabled2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean disableEquals(int disabled1, int disabled2) {
            return this.mDisabled1 == disabled1 && this.mDisabled2 == disabled2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setImeWindowState(int vis, int backDisposition, boolean showImeSwitcher, IBinder token) {
            this.mImeWindowVis = vis;
            this.mImeBackDisposition = backDisposition;
            this.mShowImeSwitcher = showImeSwitcher;
            this.mImeToken = token;
        }
    }

    private void enforceStatusBarOrShell() {
        if (Binder.getCallingUid() == 2000) {
            return;
        }
        enforceStatusBar();
    }

    private void enforceStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", TAG);
    }

    private void enforceExpandStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.EXPAND_STATUS_BAR", TAG);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceStatusBarService() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", TAG);
    }

    private void enforceBiometricDialog() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIOMETRIC_DIALOG", TAG);
    }

    private void enforceMediaContentControl() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MEDIA_CONTENT_CONTROL", TAG);
    }

    private boolean checkCanCollapseStatusBar(String method) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingUid();
        if (CompatChanges.isChangeEnabled((long) LOCK_DOWN_COLLAPSE_STATUS_BAR, uid)) {
            enforceStatusBar();
            return true;
        } else if (this.mContext.checkPermission("android.permission.STATUS_BAR", pid, uid) != 0) {
            enforceExpandStatusBar();
            if (!this.mActivityTaskManager.canCloseSystemDialogs(pid, uid)) {
                Slog.e(TAG, "Permission Denial: Method " + method + "() requires permission android.permission.STATUS_BAR, ignoring call.");
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    public RegisterStatusBarResult registerStatusBar(IStatusBar bar) {
        ArrayMap<String, StatusBarIcon> icons;
        RegisterStatusBarResult registerStatusBarResult;
        enforceStatusBarService();
        Slog.i(TAG, "registerStatusBar bar=" + bar);
        this.mBar = bar;
        this.mDeathRecipient.linkToDeath();
        notifyBarAttachChanged();
        synchronized (this.mIcons) {
            icons = new ArrayMap<>(this.mIcons);
        }
        synchronized (this.mLock) {
            UiState state = this.mDisplayUiState.get(0);
            int[] transientBarTypes = new int[state.mTransientBarTypes.size()];
            for (int i = 0; i < transientBarTypes.length; i++) {
                transientBarTypes[i] = ((Integer) state.mTransientBarTypes.valueAt(i)).intValue();
            }
            registerStatusBarResult = new RegisterStatusBarResult(icons, gatherDisableActionsLocked(this.mCurrentUserId, 1), state.mAppearance, state.mAppearanceRegions, state.mImeWindowVis, state.mImeBackDisposition, state.mShowImeSwitcher, gatherDisableActionsLocked(this.mCurrentUserId, 2), state.mImeToken, state.mNavbarColorManagedByIme, state.mBehavior, state.mRequestedVisibilities, state.mPackageName, transientBarTypes);
        }
        return registerStatusBarResult;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyBarAttachChanged() {
        UiThread.getHandler().post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                StatusBarManagerService.this.m6674x457bba91();
            }
        });
        this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                StatusBarManagerService.this.m6675x5f973930();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyBarAttachChanged$2$com-android-server-statusbar-StatusBarManagerService  reason: not valid java name */
    public /* synthetic */ void m6674x457bba91() {
        GlobalActionsProvider.GlobalActionsListener globalActionsListener = this.mGlobalActionListener;
        if (globalActionsListener == null) {
            return;
        }
        globalActionsListener.onGlobalActionsAvailableChanged(this.mBar != null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyBarAttachChanged$3$com-android-server-statusbar-StatusBarManagerService  reason: not valid java name */
    public /* synthetic */ void m6675x5f973930() {
        synchronized (this.mLock) {
            setUdfpsHbmListener(this.mUdfpsHbmListener);
            setBiometicContextListener(this.mBiometricContextListener);
        }
    }

    void registerOverlayManager(IOverlayManager overlayManager) {
        this.mOverlayManager = overlayManager;
    }

    public void onPanelRevealed(boolean clearNotificationEffects, int numItems) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelRevealed(clearNotificationEffects, numItems);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearNotificationEffects() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.clearEffects();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onPanelHidden() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelHidden();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void shutdown() {
        if (ActivityManager.isUserAMonkey()) {
            Slog.d(TAG, "isUserAMonkey true,don't shutdown");
            return;
        }
        enforceStatusBarService();
        ShutdownCheckPoints.recordCheckPoint(Binder.getCallingPid(), "userrequested");
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.prepareForPossibleShutdown();
            this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    ShutdownThread.shutdown(StatusBarManagerService.getUiContext(), r1, false);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void reboot(final boolean safeMode) {
        final String reason;
        if (ActivityManager.isUserAMonkey()) {
            Slog.d(TAG, "isUserAMonkey true,don't reboot");
            return;
        }
        enforceStatusBarService();
        if (safeMode) {
            reason = "safemode";
        } else {
            reason = "userrequested";
        }
        ShutdownCheckPoints.recordCheckPoint(Binder.getCallingPid(), reason);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.prepareForPossibleShutdown();
            this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarManagerService.lambda$reboot$5(safeMode, reason);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reboot$5(boolean safeMode, String reason) {
        if (safeMode) {
            ShutdownThread.rebootSafeMode(getUiContext(), true);
        } else {
            ShutdownThread.reboot(getUiContext(), reason, false);
        }
    }

    public void restart() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mHandler.post(new Runnable() { // from class: com.android.server.statusbar.StatusBarManagerService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarManagerService.this.m6676x1ac9f1d();
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restart$6$com-android-server-statusbar-StatusBarManagerService  reason: not valid java name */
    public /* synthetic */ void m6676x1ac9f1d() {
        this.mActivityManagerInternal.restart();
    }

    public void onGlobalActionsShown() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            GlobalActionsProvider.GlobalActionsListener globalActionsListener = this.mGlobalActionListener;
            if (globalActionsListener == null) {
                return;
            }
            globalActionsListener.onGlobalActionsShown();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onGlobalActionsHidden() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            GlobalActionsProvider.GlobalActionsListener globalActionsListener = this.mGlobalActionListener;
            if (globalActionsListener == null) {
                return;
            }
            globalActionsListener.onGlobalActionsDismissed();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClick(String key, NotificationVisibility nv) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClick(callingUid, callingPid, key, nv);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationActionClick(String key, int actionIndex, Notification.Action action, NotificationVisibility nv, boolean generatedByAssistant) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationActionClick(callingUid, callingPid, key, actionIndex, action, nv, generatedByAssistant);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationError(String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationError(callingUid, callingPid, pkg, tag, id, uid, initialPid, message, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClear(String pkg, int userId, String key, int dismissalSurface, int dismissalSentiment, NotificationVisibility nv) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClear(callingUid, callingPid, pkg, userId, key, dismissalSurface, dismissalSentiment, nv);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationVisibilityChanged(newlyVisibleKeys, noLongerVisibleKeys);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded, int location) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationExpansionChanged(key, userAction, expanded, location);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationDirectReplied(String key) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationDirectReplied(key);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSmartSuggestionsAdded(String key, int smartReplyCount, int smartActionCount, boolean generatedByAssistant, boolean editBeforeSending) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartSuggestionsAdded(key, smartReplyCount, smartActionCount, generatedByAssistant, editBeforeSending);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSmartReplySent(String key, int replyIndex, CharSequence reply, int notificationLocation, boolean modifiedBeforeSending) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartReplySent(key, replyIndex, reply, notificationLocation, modifiedBeforeSending);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSettingsViewed(String key) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSettingsViewed(key);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onClearAllNotifications(int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onClearAll(callingUid, callingPid, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationBubbleChanged(String key, boolean isBubble, int flags) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationBubbleChanged(key, isBubble, flags);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onBubbleMetadataFlagChanged(String key, int flags) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onBubbleMetadataFlagChanged(key, flags);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void hideCurrentInputMethodForBubbles() {
        enforceStatusBarService();
        long token = Binder.clearCallingIdentity();
        try {
            InputMethodManagerInternal.get().hideCurrentInputMethod(19);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void grantInlineReplyUriPermission(String key, Uri uri, UserHandle user, String packageName) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.grantInlineReplyUriPermission(key, uri, user, packageName, callingUid);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearInlineReplyUriPermissions(String key) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.clearInlineReplyUriPermissions(key, callingUid);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationFeedbackReceived(String key, Bundle feedback) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationFeedbackReceived(key, feedback);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.statusbar.StatusBarManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new StatusBarShellCommand(this, this.mContext).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public void showInattentiveSleepWarning() {
        enforceStatusBarService();
        if (this.mBar != null) {
            try {
                this.mBar.showInattentiveSleepWarning();
            } catch (RemoteException e) {
            }
        }
    }

    public void dismissInattentiveSleepWarning(boolean animated) {
        enforceStatusBarService();
        if (this.mBar != null) {
            try {
                this.mBar.dismissInattentiveSleepWarning(animated);
            } catch (RemoteException e) {
            }
        }
    }

    public void suppressAmbientDisplay(boolean suppress) {
        enforceStatusBarService();
        if (this.mBar != null) {
            try {
                this.mBar.suppressAmbientDisplay(suppress);
            } catch (RemoteException e) {
            }
        }
    }

    private void checkCallingUidPackage(String packageName, int callingUid, int userId) {
        int packageUid = this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId);
        if (UserHandle.getAppId(callingUid) != UserHandle.getAppId(packageUid)) {
            throw new SecurityException("Package " + packageName + " does not belong to the calling uid " + callingUid);
        }
    }

    private ResolveInfo isComponentValidTileService(ComponentName componentName, int userId) {
        Intent intent = new Intent("android.service.quicksettings.action.QS_TILE");
        intent.setComponent(componentName);
        ResolveInfo r = this.mPackageManagerInternal.resolveService(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0L, userId, Process.myUid());
        int enabled = this.mPackageManagerInternal.getComponentEnabledSetting(componentName, Process.myUid(), userId);
        if (r != null && r.serviceInfo != null && resolveEnabledComponent(r.serviceInfo.enabled, enabled) && "android.permission.BIND_QUICK_SETTINGS_TILE".equals(r.serviceInfo.permission)) {
            return r;
        }
        return null;
    }

    private boolean resolveEnabledComponent(boolean defaultValue, int pmResult) {
        if (pmResult == 1) {
            return true;
        }
        if (pmResult == 0) {
            return defaultValue;
        }
        return false;
    }

    public void requestTileServiceListeningState(ComponentName componentName, int userId) {
        int callingUid = Binder.getCallingUid();
        String packageName = componentName.getPackageName();
        boolean mustPerformChecks = CompatChanges.isChangeEnabled((long) REQUEST_LISTENING_MUST_MATCH_PACKAGE, callingUid);
        if (mustPerformChecks) {
            int userId2 = this.mActivityManagerInternal.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, 0, "requestTileServiceListeningState", packageName);
            checkCallingUidPackage(packageName, callingUid, userId2);
            int currentUser = this.mActivityManagerInternal.getCurrentUserId();
            if (userId2 != currentUser) {
                throw new IllegalArgumentException("User " + userId2 + " is not the current user.");
            }
        }
        if (this.mBar != null) {
            try {
                this.mBar.requestTileServiceListeningState(componentName);
            } catch (RemoteException e) {
                Slog.e(TAG, "requestTileServiceListeningState", e);
            }
        }
    }

    public void requestAddTile(final ComponentName componentName, CharSequence label, Icon icon, final int userId, final IAddTileResultCallback callback) {
        IAddTileResultCallback iAddTileResultCallback;
        int callingUid = Binder.getCallingUid();
        final String packageName = componentName.getPackageName();
        this.mActivityManagerInternal.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, 0, "requestAddTile", packageName);
        checkCallingUidPackage(packageName, callingUid, userId);
        int currentUser = this.mActivityManagerInternal.getCurrentUserId();
        if (userId != currentUser) {
            try {
                callback.onTileRequest(1003);
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "requestAddTile", e);
                return;
            }
        }
        ResolveInfo r = isComponentValidTileService(componentName, userId);
        if (r == null) {
            iAddTileResultCallback = callback;
        } else if (r.serviceInfo.exported) {
            int procState = this.mActivityManagerInternal.getUidProcessState(callingUid);
            if (ActivityManager.RunningAppProcessInfo.procStateToImportance(procState) != 100) {
                try {
                    callback.onTileRequest(1004);
                    return;
                } catch (RemoteException e2) {
                    Slog.e(TAG, "requestAddTile", e2);
                    return;
                }
            }
            synchronized (this.mCurrentRequestAddTilePackages) {
                try {
                    Long lastTime = this.mCurrentRequestAddTilePackages.get(packageName);
                    long currentTime = System.nanoTime();
                    if (lastTime != null) {
                        try {
                            if (currentTime - lastTime.longValue() < REQUEST_TIME_OUT) {
                                try {
                                    callback.onTileRequest(1001);
                                } catch (RemoteException e3) {
                                    Slog.e(TAG, "requestAddTile", e3);
                                }
                                return;
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
                    if (lastTime != null) {
                        cancelRequestAddTileInternal(packageName);
                    }
                    this.mCurrentRequestAddTilePackages.put(packageName, Long.valueOf(currentTime));
                    if (this.mTileRequestTracker.shouldBeDenied(userId, componentName)) {
                        if (clearTileAddRequest(packageName)) {
                            try {
                                callback.onTileRequest(0);
                                return;
                            } catch (RemoteException e4) {
                                Slog.e(TAG, "requestAddTile - callback", e4);
                                return;
                            }
                        }
                        return;
                    }
                    IAddTileResultCallback.Stub stub = new IAddTileResultCallback.Stub() { // from class: com.android.server.statusbar.StatusBarManagerService.3
                        public void onTileRequest(int i) {
                            if (i == 3) {
                                i = 0;
                            } else if (i == 0) {
                                StatusBarManagerService.this.mTileRequestTracker.addDenial(userId, componentName);
                            } else if (i == 2) {
                                StatusBarManagerService.this.mTileRequestTracker.resetRequests(userId, componentName);
                            }
                            if (StatusBarManagerService.this.clearTileAddRequest(packageName)) {
                                try {
                                    callback.onTileRequest(i);
                                } catch (RemoteException e5) {
                                    Slog.e(StatusBarManagerService.TAG, "requestAddTile - callback", e5);
                                }
                            }
                        }
                    };
                    CharSequence appName = r.serviceInfo.applicationInfo.loadLabel(this.mContext.getPackageManager());
                    if (this.mBar != null) {
                        try {
                            this.mBar.requestAddTile(componentName, appName, label, icon, stub);
                            return;
                        } catch (RemoteException e5) {
                            Slog.e(TAG, "requestAddTile", e5);
                        }
                    }
                    clearTileAddRequest(packageName);
                    try {
                        callback.onTileRequest(1005);
                        return;
                    } catch (RemoteException e6) {
                        Slog.e(TAG, "requestAddTile", e6);
                        return;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        } else {
            iAddTileResultCallback = callback;
        }
        try {
            iAddTileResultCallback.onTileRequest(1002);
        } catch (RemoteException e7) {
            Slog.e(TAG, "requestAddTile", e7);
        }
    }

    public void cancelRequestAddTile(String packageName) {
        enforceStatusBar();
        cancelRequestAddTileInternal(packageName);
    }

    private void cancelRequestAddTileInternal(String packageName) {
        clearTileAddRequest(packageName);
        if (this.mBar != null) {
            try {
                this.mBar.cancelRequestAddTile(packageName);
            } catch (RemoteException e) {
                Slog.e(TAG, "requestAddTile", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean clearTileAddRequest(String packageName) {
        boolean z;
        synchronized (this.mCurrentRequestAddTilePackages) {
            z = this.mCurrentRequestAddTilePackages.remove(packageName) != null;
        }
        return z;
    }

    public void onSessionStarted(int sessionType, InstanceId instance) {
        this.mSessionMonitor.onSessionStarted(sessionType, instance);
    }

    public void onSessionEnded(int sessionType, InstanceId instance) {
        this.mSessionMonitor.onSessionEnded(sessionType, instance);
    }

    public void registerSessionListener(int sessionFlags, ISessionListener listener) {
        this.mSessionMonitor.registerSessionListener(sessionFlags, listener);
    }

    public void unregisterSessionListener(int sessionFlags, ISessionListener listener) {
        this.mSessionMonitor.unregisterSessionListener(sessionFlags, listener);
    }

    public String[] getStatusBarIcons() {
        return this.mContext.getResources().getStringArray(17236134);
    }

    public void setNavBarMode(int navBarMode) {
        enforceStatusBar();
        if (navBarMode != 0 && navBarMode != 1) {
            throw new IllegalArgumentException("Supplied navBarMode not supported: " + navBarMode);
        }
        int userId = this.mCurrentUserId;
        long userIdentity = Binder.clearCallingIdentity();
        try {
            try {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "nav_bar_kids_mode", navBarMode, userId);
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "nav_bar_force_visible", navBarMode, userId);
                IOverlayManager overlayManager = getOverlayManager();
                if (overlayManager != null && navBarMode == 1 && isPackageSupported("com.android.internal.systemui.navbar.threebutton")) {
                    overlayManager.setEnabledExclusiveInCategory("com.android.internal.systemui.navbar.threebutton", userId);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } finally {
            Binder.restoreCallingIdentity(userIdentity);
        }
    }

    public int getNavBarMode() {
        enforceStatusBar();
        int userId = this.mCurrentUserId;
        long userIdentity = Binder.clearCallingIdentity();
        try {
            int navBarKidsMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "nav_bar_kids_mode", userId);
            return navBarKidsMode;
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        } finally {
            Binder.restoreCallingIdentity(userIdentity);
        }
    }

    private boolean isPackageSupported(String packageName) {
        if (packageName == null) {
            return false;
        }
        try {
            return this.mContext.getPackageManager().getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void updateMediaTapToTransferSenderDisplay(int displayState, MediaRoute2Info routeInfo, IUndoMediaTransferCallback undoCallback) {
        enforceMediaContentControl();
        if (this.mBar != null) {
            try {
                this.mBar.updateMediaTapToTransferSenderDisplay(displayState, routeInfo, undoCallback);
            } catch (RemoteException e) {
                Slog.e(TAG, "updateMediaTapToTransferSenderDisplay", e);
            }
        }
    }

    public void updateMediaTapToTransferReceiverDisplay(int displayState, MediaRoute2Info routeInfo, Icon appIcon, CharSequence appName) {
        enforceMediaContentControl();
        if (this.mBar != null) {
            try {
                this.mBar.updateMediaTapToTransferReceiverDisplay(displayState, routeInfo, appIcon, appName);
            } catch (RemoteException e) {
                Slog.e(TAG, "updateMediaTapToTransferReceiverDisplay", e);
            }
        }
    }

    public void registerNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) {
        enforceMediaContentControl();
        if (this.mBar != null) {
            try {
                this.mBar.registerNearbyMediaDevicesProvider(provider);
            } catch (RemoteException e) {
                Slog.e(TAG, "registerNearbyMediaDevicesProvider", e);
            }
        }
    }

    public void unregisterNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) {
        enforceMediaContentControl();
        if (this.mBar != null) {
            try {
                this.mBar.unregisterNearbyMediaDevicesProvider(provider);
            } catch (RemoteException e) {
                Slog.e(TAG, "unregisterNearbyMediaDevicesProvider", e);
            }
        }
    }

    public void passThroughShellCommand(String[] args, FileDescriptor fd) {
        enforceStatusBarOrShell();
        if (this.mBar == null) {
            return;
        }
        try {
            TransferPipe tp = new TransferPipe();
            tp.setBufferPrefix("  ");
            this.mBar.passThroughShellCommand(args, tp.getWriteFd());
            tp.go(fd);
            tp.close();
        } catch (Throwable t) {
            Slog.e(TAG, "Error sending command to IStatusBar", t);
        }
    }

    void manageDisableListLocked(int userId, int what, IBinder token, String pkg, int which) {
        Pair<Integer, DisableRecord> match = findMatchingRecordLocked(token, userId);
        int i = ((Integer) match.first).intValue();
        DisableRecord record = (DisableRecord) match.second;
        if (!token.isBinderAlive()) {
            if (record != null) {
                this.mDisableRecords.remove(i);
                record.token.unlinkToDeath(record, 0);
            }
        } else if (record != null) {
            record.setFlags(what, which, pkg);
            if (record.isEmpty()) {
                this.mDisableRecords.remove(i);
                record.token.unlinkToDeath(record, 0);
            }
        } else {
            DisableRecord record2 = new DisableRecord(userId, token);
            record2.setFlags(what, which, pkg);
            this.mDisableRecords.add(record2);
        }
    }

    private Pair<Integer, DisableRecord> findMatchingRecordLocked(IBinder token, int userId) {
        int numRecords = this.mDisableRecords.size();
        DisableRecord record = null;
        int i = 0;
        while (true) {
            if (i >= numRecords) {
                break;
            }
            DisableRecord r = this.mDisableRecords.get(i);
            if (r.token != token || r.userId != userId) {
                i++;
            } else {
                record = r;
                break;
            }
        }
        return new Pair<>(Integer.valueOf(i), record);
    }

    int gatherDisableActionsLocked(int userId, int which) {
        int N = this.mDisableRecords.size();
        int net = 0;
        for (int i = 0; i < N; i++) {
            DisableRecord rec = this.mDisableRecords.get(i);
            if (rec.userId == userId) {
                net |= rec.getFlags(which);
            }
        }
        return net;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ArrayList<String> requests;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                for (int i = 0; i < this.mDisplayUiState.size(); i++) {
                    int key = this.mDisplayUiState.keyAt(i);
                    UiState state = this.mDisplayUiState.get(key);
                    pw.println("  displayId=" + key);
                    pw.println("    mDisabled1=0x" + Integer.toHexString(state.getDisabled1()));
                    pw.println("    mDisabled2=0x" + Integer.toHexString(state.getDisabled2()));
                }
                int N = this.mDisableRecords.size();
                pw.println("  mDisableRecords.size=" + N);
                for (int i2 = 0; i2 < N; i2++) {
                    DisableRecord tok = this.mDisableRecords.get(i2);
                    pw.println("    [" + i2 + "] " + tok);
                }
                pw.println("  mCurrentUserId=" + this.mCurrentUserId);
                pw.println("  mIcons=");
                for (String slot : this.mIcons.keySet()) {
                    pw.println("    ");
                    pw.print(slot);
                    pw.print(" -> ");
                    StatusBarIcon icon = this.mIcons.get(slot);
                    pw.print(icon);
                    if (!TextUtils.isEmpty(icon.contentDescription)) {
                        pw.print(" \"");
                        pw.print(icon.contentDescription);
                        pw.print("\"");
                    }
                    pw.println();
                }
                synchronized (this.mCurrentRequestAddTilePackages) {
                    requests = new ArrayList<>(this.mCurrentRequestAddTilePackages.keySet());
                }
                pw.println("  mCurrentRequestAddTilePackages=[");
                int reqN = requests.size();
                for (int i3 = 0; i3 < reqN; i3++) {
                    pw.println("    " + requests.get(i3) + ",");
                }
                pw.println("  ]");
                IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                this.mTileRequestTracker.dump(fd, ipw.increaseIndent(), args);
            }
        }
    }

    private static final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public void showInCallUIStatuBar(String callState, long baseTime) {
        if (this.mBar != null) {
            try {
                this.mBar.showInCallUIStatuBar(callState, baseTime);
            } catch (RemoteException ex) {
                Slog.e(TAG, "unable to showInCallUIStatuBar: Exception:", ex);
            }
        }
    }

    public void hideInCallUIStatuBar() {
        if (this.mBar != null) {
            try {
                this.mBar.hideInCallUIStatuBar();
            } catch (RemoteException ex) {
                Slog.e(TAG, "unable to hideCallUIStatuBar: Exception:", ex);
            }
        }
    }
}
