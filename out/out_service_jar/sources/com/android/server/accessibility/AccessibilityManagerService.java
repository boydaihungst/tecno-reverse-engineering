package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.MagnificationConfig;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.appwidget.AppWidgetManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.IFingerprintService;
import android.media.AudioManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.safetycenter.SafetyCenterManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IInputFilter;
import android.view.IWindow;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.WindowInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityManager;
import android.view.accessibility.IAccessibilityManagerClient;
import android.view.accessibility.IWindowMagnificationConnection;
import android.view.inputmethod.EditorInfo;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.accessibility.dialog.AccessibilityButtonChooserActivity;
import com.android.internal.accessibility.dialog.AccessibilityShortcutChooserActivity;
import com.android.internal.accessibility.util.AccessibilityStatsLogUtils;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IntPair;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.AccessibilityManagerInternal;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.AccessibilitySecurityPolicy;
import com.android.server.accessibility.AccessibilityUserState;
import com.android.server.accessibility.AccessibilityWindowManager;
import com.android.server.accessibility.PolicyWarningUIController;
import com.android.server.accessibility.SystemActionPerformer;
import com.android.server.accessibility.magnification.MagnificationController;
import com.android.server.accessibility.magnification.MagnificationProcessor;
import com.android.server.accessibility.magnification.MagnificationScaleProvider;
import com.android.server.accessibility.magnification.WindowMagnificationManager;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AccessibilityManagerService extends IAccessibilityManager.Stub implements AbstractAccessibilityServiceConnection.SystemSupport, AccessibilityUserState.ServiceInfoChangeListener, AccessibilityWindowManager.AccessibilityEventSender, AccessibilitySecurityPolicy.AccessibilityUserManager, SystemActionPerformer.SystemActionsChangedListener {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE = "registerUiTestAutomationService";
    private static final String GET_WINDOW_TOKEN = "getWindowToken";
    public static final int INVALID_SERVICE_ID = -1;
    private static final String LOG_TAG = "AccessibilityManagerService";
    public static final int MAGNIFICATION_GESTURE_HANDLER_ID = 0;
    private static final int POSTPONE_WINDOW_STATE_CHANGED_EVENT_TIMEOUT_MILLIS = 500;
    private static final String SET_PIP_ACTION_REPLACEMENT = "setPictureInPictureActionReplacingConnection";
    private static final String TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED = "temporaryEnableAccessibilityStateUntilKeyguardRemoved";
    private static final int WAIT_FOR_USER_STATE_FULLY_INITIALIZED_MILLIS = 3000;
    private static final int WAIT_MOTION_INJECTOR_TIMEOUT_MILLIS = 1000;
    private final AccessibilityDisplayListener mA11yDisplayListener;
    private final AccessibilityWindowManager mA11yWindowManager;
    private final ActivityTaskManagerInternal mActivityTaskManagerService;
    private final CaptioningManagerImpl mCaptioningManagerImpl;
    private final Context mContext;
    private int mCurrentUserId;
    EditorInfo mEditorInfo;
    private AlertDialog mEnableTouchExplorationDialog;
    private FingerprintGestureDispatcher mFingerprintGestureDispatcher;
    private final RemoteCallbackList<IAccessibilityManagerClient> mGlobalClients;
    private boolean mHasInputFilter;
    private boolean mInitialized;
    private boolean mInputBound;
    private AccessibilityInputFilter mInputFilter;
    boolean mInputSessionRequested;
    private InteractionBridge mInteractionBridge;
    private boolean mIsAccessibilityButtonShown;
    private KeyEventDispatcher mKeyEventDispatcher;
    private final Object mLock;
    private final MagnificationController mMagnificationController;
    private final MagnificationProcessor mMagnificationProcessor;
    private final MainHandler mMainHandler;
    private SparseArray<MotionEventInjector> mMotionEventInjectors;
    private final PackageManager mPackageManager;
    private final PowerManager mPowerManager;
    IRemoteAccessibilityInputConnection mRemoteInputConnection;
    boolean mRestarting;
    private final AccessibilitySecurityPolicy mSecurityPolicy;
    private final List<SendWindowStateChangedEventRunnable> mSendWindowStateChangedEventRunnables;
    private final TextUtils.SimpleStringSplitter mStringColonSplitter;
    private SystemActionPerformer mSystemActionPerformer;
    private final List<AccessibilityServiceInfo> mTempAccessibilityServiceInfoList;
    private final Set<ComponentName> mTempComponentNameSet;
    private final IntArray mTempIntArray;
    private Point mTempPoint;
    private final Rect mTempRect;
    private final Rect mTempRect1;
    private final AccessibilityTraceManager mTraceManager;
    private final UiAutomationManager mUiAutomationManager;
    final SparseArray<AccessibilityUserState> mUserStates;
    private final WindowManagerInternal mWindowManagerService;
    private static final int OWN_PROCESS_ID = Process.myPid();
    private static int sIdCounter = 1;

    public AccessibilityUserState getCurrentUserStateLocked() {
        return getUserStateLocked(this.mCurrentUserId);
    }

    public void changeMagnificationMode(int displayId, int magnificationMode) {
        synchronized (this.mLock) {
            if (displayId == 0) {
                persistMagnificationModeSettingsLocked(magnificationMode);
            } else {
                AccessibilityUserState userState = getCurrentUserStateLocked();
                int currentMode = userState.getMagnificationModeLocked(displayId);
                if (magnificationMode != currentMode) {
                    userState.setMagnificationModeLocked(displayId, magnificationMode);
                    updateMagnificationModeChangeSettingsLocked(userState, displayId);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class LocalServiceImpl extends AccessibilityManagerInternal {
        private final AccessibilityManagerService mService;

        LocalServiceImpl(AccessibilityManagerService service) {
            this.mService = service;
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void setImeSessionEnabled(SparseArray<IAccessibilityInputMethodSession> sessions, boolean enabled) {
            this.mService.scheduleSetImeSessionEnabled(sessions, enabled);
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void unbindInput() {
            this.mService.scheduleUnbindInput();
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void bindInput() {
            this.mService.scheduleBindInput();
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void createImeSession(ArraySet<Integer> ignoreSet) {
            this.mService.scheduleCreateImeSession(ignoreSet);
        }

        @Override // com.android.server.AccessibilityManagerInternal
        public void startInput(IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, EditorInfo editorInfo, boolean restarting) {
            this.mService.scheduleStartInput(remoteAccessibilityInputConnection, editorInfo, restarting);
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final AccessibilityManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new AccessibilityManagerService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            LocalServices.addService(AccessibilityManagerInternal.class, new LocalServiceImpl(this.mService));
            publishBinderService("accessibility", this.mService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }
    }

    AccessibilityManagerService(Context context, PackageManager packageManager, AccessibilitySecurityPolicy securityPolicy, SystemActionPerformer systemActionPerformer, AccessibilityWindowManager a11yWindowManager, AccessibilityDisplayListener a11yDisplayListener, MagnificationController magnificationController, AccessibilityInputFilter inputFilter) {
        Object obj = new Object();
        this.mLock = obj;
        this.mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
        this.mTempRect = new Rect();
        this.mTempRect1 = new Rect();
        this.mTempComponentNameSet = new HashSet();
        this.mTempAccessibilityServiceInfoList = new ArrayList();
        this.mTempIntArray = new IntArray(0);
        this.mGlobalClients = new RemoteCallbackList<>();
        this.mUserStates = new SparseArray<>();
        this.mUiAutomationManager = new UiAutomationManager(obj);
        this.mSendWindowStateChangedEventRunnables = new ArrayList();
        this.mCurrentUserId = 0;
        this.mTempPoint = new Point();
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mWindowManagerService = windowManagerInternal;
        this.mTraceManager = AccessibilityTraceManager.getInstance(windowManagerInternal.getAccessibilityController(), this, obj);
        this.mMainHandler = new MainHandler(context.getMainLooper());
        this.mActivityTaskManagerService = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mPackageManager = packageManager;
        this.mSecurityPolicy = securityPolicy;
        this.mSystemActionPerformer = systemActionPerformer;
        this.mA11yWindowManager = a11yWindowManager;
        this.mA11yDisplayListener = a11yDisplayListener;
        this.mMagnificationController = magnificationController;
        this.mMagnificationProcessor = new MagnificationProcessor(magnificationController);
        this.mCaptioningManagerImpl = new CaptioningManagerImpl(context);
        if (inputFilter != null) {
            this.mInputFilter = inputFilter;
            this.mHasInputFilter = true;
        }
        init();
    }

    public AccessibilityManagerService(Context context) {
        Object obj = new Object();
        this.mLock = obj;
        this.mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
        this.mTempRect = new Rect();
        this.mTempRect1 = new Rect();
        this.mTempComponentNameSet = new HashSet();
        this.mTempAccessibilityServiceInfoList = new ArrayList();
        this.mTempIntArray = new IntArray(0);
        this.mGlobalClients = new RemoteCallbackList<>();
        this.mUserStates = new SparseArray<>();
        this.mUiAutomationManager = new UiAutomationManager(obj);
        this.mSendWindowStateChangedEventRunnables = new ArrayList();
        this.mCurrentUserId = 0;
        this.mTempPoint = new Point();
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mWindowManagerService = windowManagerInternal;
        AccessibilityTraceManager accessibilityTraceManager = AccessibilityTraceManager.getInstance(windowManagerInternal.getAccessibilityController(), this, obj);
        this.mTraceManager = accessibilityTraceManager;
        MainHandler mainHandler = new MainHandler(context.getMainLooper());
        this.mMainHandler = mainHandler;
        this.mActivityTaskManagerService = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mPackageManager = context.getPackageManager();
        PolicyWarningUIController policyWarningUIController = new PolicyWarningUIController(mainHandler, context, new PolicyWarningUIController.NotificationController(context));
        AccessibilitySecurityPolicy accessibilitySecurityPolicy = new AccessibilitySecurityPolicy(policyWarningUIController, context, this);
        this.mSecurityPolicy = accessibilitySecurityPolicy;
        this.mA11yWindowManager = new AccessibilityWindowManager(obj, mainHandler, windowManagerInternal, this, accessibilitySecurityPolicy, this, accessibilityTraceManager);
        this.mA11yDisplayListener = new AccessibilityDisplayListener(context, mainHandler);
        MagnificationController magnificationController = new MagnificationController(this, obj, context, new MagnificationScaleProvider(context));
        this.mMagnificationController = magnificationController;
        this.mMagnificationProcessor = new MagnificationProcessor(magnificationController);
        this.mCaptioningManagerImpl = new CaptioningManagerImpl(context);
        init();
    }

    private void init() {
        this.mSecurityPolicy.setAccessibilityWindowManager(this.mA11yWindowManager);
        registerBroadcastReceivers();
        new AccessibilityContentObserver(this.mMainHandler).register(this.mContext.getContentResolver());
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport, com.android.server.accessibility.AccessibilitySecurityPolicy.AccessibilityUserManager
    public int getCurrentUserIdLocked() {
        return this.mCurrentUserId;
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public boolean isAccessibilityButtonShown() {
        return this.mIsAccessibilityButtonShown;
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public Pair<float[], MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(int windowId) {
        WindowInfo windowInfo;
        IBinder token;
        synchronized (this.mLock) {
            windowInfo = this.mA11yWindowManager.findWindowInfoByIdLocked(windowId);
        }
        if (windowInfo != null) {
            MagnificationSpec spec = new MagnificationSpec();
            spec.setTo(windowInfo.mMagnificationSpec);
            return new Pair<>(windowInfo.mTransformMatrix, spec);
        }
        synchronized (this.mLock) {
            token = this.mA11yWindowManager.getWindowTokenForUserAndWindowIdLocked(this.mCurrentUserId, windowId);
        }
        Pair<Matrix, MagnificationSpec> pair = this.mWindowManagerService.getWindowTransformationMatrixAndMagnificationSpec(token);
        float[] outTransformationMatrix = new float[9];
        Matrix tmpMatrix = (Matrix) pair.first;
        MagnificationSpec spec2 = (MagnificationSpec) pair.second;
        if (!spec2.isNop()) {
            tmpMatrix.postScale(spec2.scale, spec2.scale);
            tmpMatrix.postTranslate(spec2.offsetX, spec2.offsetY);
        }
        tmpMatrix.getValues(outTransformationMatrix);
        return new Pair<>(outTransformationMatrix, (MagnificationSpec) pair.second);
    }

    @Override // com.android.server.accessibility.AccessibilityUserState.ServiceInfoChangeListener
    public void onServiceInfoChangedLocked(AccessibilityUserState userState) {
        this.mSecurityPolicy.onBoundServicesChangedLocked(userState.mUserId, userState.mBoundServices);
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public FingerprintGestureDispatcher getFingerprintGestureDispatcher() {
        return this.mFingerprintGestureDispatcher;
    }

    public void onBootPhase(int phase) {
        if (phase == 500 && this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
            this.mSecurityPolicy.setAppWidgetManager((AppWidgetManagerInternal) LocalServices.getService(AppWidgetManagerInternal.class));
        }
        if (phase == 600) {
            setNonA11yToolNotificationToMatchSafetyCenter();
        }
    }

    public void setNonA11yToolNotificationToMatchSafetyCenter() {
        boolean sendNotification = !((SafetyCenterManager) this.mContext.getSystemService(SafetyCenterManager.class)).isSafetyCenterEnabled();
        synchronized (this.mLock) {
            this.mSecurityPolicy.setSendingNonA11yToolNotificationLocked(sendNotification);
        }
    }

    public AccessibilityUserState getCurrentUserState() {
        AccessibilityUserState currentUserStateLocked;
        synchronized (this.mLock) {
            currentUserStateLocked = getCurrentUserStateLocked();
        }
        return currentUserStateLocked;
    }

    private AccessibilityUserState getUserState(int userId) {
        AccessibilityUserState userStateLocked;
        synchronized (this.mLock) {
            userStateLocked = getUserStateLocked(userId);
        }
        return userStateLocked;
    }

    public AccessibilityUserState getUserStateLocked(int userId) {
        AccessibilityUserState state = this.mUserStates.get(userId);
        if (state == null) {
            AccessibilityUserState state2 = new AccessibilityUserState(userId, this.mContext, this);
            this.mUserStates.put(userId, state2);
            return state2;
        }
        return state;
    }

    public boolean getBindInstantServiceAllowed(int userId) {
        boolean bindInstantServiceAllowedLocked;
        synchronized (this.mLock) {
            AccessibilityUserState userState = getUserStateLocked(userId);
            bindInstantServiceAllowedLocked = userState.getBindInstantServiceAllowedLocked();
        }
        return bindInstantServiceAllowedLocked;
    }

    public void setBindInstantServiceAllowed(int userId, boolean allowed) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIND_INSTANT_SERVICE", "setBindInstantServiceAllowed");
        synchronized (this.mLock) {
            AccessibilityUserState userState = getUserStateLocked(userId);
            if (allowed != userState.getBindInstantServiceAllowedLocked()) {
                userState.setBindInstantServiceAllowedLocked(allowed);
                onUserStateChangedLocked(userState);
            }
        }
    }

    /* renamed from: com.android.server.accessibility.AccessibilityManagerService$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends PackageMonitor {
        AnonymousClass1() {
            AccessibilityManagerService.this = this$0;
        }

        public void onSomePackagesChanged() {
            if (AccessibilityManagerService.this.mTraceManager.isA11yTracingEnabledForTypes(32768L)) {
                AccessibilityManagerService.this.mTraceManager.logTrace("AccessibilityManagerService.PM.onSomePackagesChanged", 32768L);
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                if (getChangingUserId() != AccessibilityManagerService.this.mCurrentUserId) {
                    return;
                }
                AccessibilityUserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                userState.mInstalledServices.clear();
                if (AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                }
            }
        }

        public void onPackageUpdateFinished(final String packageName, int uid) {
            boolean reboundAService;
            if (AccessibilityManagerService.this.mTraceManager.isA11yTracingEnabledForTypes(32768L)) {
                AccessibilityManagerService.this.mTraceManager.logTrace("AccessibilityManagerService.PM.onPackageUpdateFinished", 32768L, "packageName=" + packageName + ";uid=" + uid);
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                int userId = getChangingUserId();
                if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                    return;
                }
                AccessibilityUserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                if (!userState.getBindingServicesLocked().removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$1$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return AccessibilityManagerService.AnonymousClass1.lambda$onPackageUpdateFinished$0(packageName, (ComponentName) obj);
                    }
                }) && !userState.mCrashedServices.removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$1$$ExternalSyntheticLambda2
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return AccessibilityManagerService.AnonymousClass1.lambda$onPackageUpdateFinished$1(packageName, (ComponentName) obj);
                    }
                })) {
                    reboundAService = false;
                    userState.mInstalledServices.clear();
                    boolean configurationChanged = AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState);
                    if (!reboundAService || configurationChanged) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                    AccessibilityManagerService.this.migrateAccessibilityButtonSettingsIfNecessaryLocked(userState, packageName, 0);
                }
                reboundAService = true;
                userState.mInstalledServices.clear();
                boolean configurationChanged2 = AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState);
                if (!reboundAService) {
                }
                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                AccessibilityManagerService.this.migrateAccessibilityButtonSettingsIfNecessaryLocked(userState, packageName, 0);
            }
        }

        public static /* synthetic */ boolean lambda$onPackageUpdateFinished$0(String packageName, ComponentName component) {
            return component != null && component.getPackageName().equals(packageName);
        }

        public static /* synthetic */ boolean lambda$onPackageUpdateFinished$1(String packageName, ComponentName component) {
            return component != null && component.getPackageName().equals(packageName);
        }

        public void onPackageRemoved(final String packageName, int uid) {
            if (AccessibilityManagerService.this.mTraceManager.isA11yTracingEnabledForTypes(32768L)) {
                AccessibilityManagerService.this.mTraceManager.logTrace("AccessibilityManagerService.PM.onPackageRemoved", 32768L, "packageName=" + packageName + ";uid=" + uid);
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                int userId = getChangingUserId();
                if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                    return;
                }
                AccessibilityUserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                Predicate<ComponentName> filter = new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$1$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return AccessibilityManagerService.AnonymousClass1.lambda$onPackageRemoved$2(packageName, (ComponentName) obj);
                    }
                };
                userState.mBindingServices.removeIf(filter);
                userState.mCrashedServices.removeIf(filter);
                Iterator<ComponentName> it = userState.mEnabledServices.iterator();
                boolean anyServiceRemoved = false;
                while (it.hasNext()) {
                    ComponentName comp = it.next();
                    String compPkg = comp.getPackageName();
                    if (compPkg.equals(packageName)) {
                        it.remove();
                        userState.mTouchExplorationGrantedServices.remove(comp);
                        anyServiceRemoved = true;
                    }
                }
                if (anyServiceRemoved) {
                    AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userId);
                    AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", userState.mTouchExplorationGrantedServices, userId);
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                }
            }
        }

        public static /* synthetic */ boolean lambda$onPackageRemoved$2(String packageName, ComponentName component) {
            return component != null && component.getPackageName().equals(packageName);
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            String[] strArr = packages;
            if (AccessibilityManagerService.this.mTraceManager.isA11yTracingEnabledForTypes(32768L)) {
                AccessibilityManagerService.this.mTraceManager.logTrace("AccessibilityManagerService.PM.onHandleForceStop", 32768L, "intent=" + intent + ";packages=" + strArr + ";uid=" + uid + ";doit=" + doit);
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                int userId = getChangingUserId();
                int i = 0;
                if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                    return false;
                }
                AccessibilityUserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                Iterator<ComponentName> it = userState.mEnabledServices.iterator();
                while (it.hasNext()) {
                    ComponentName comp = it.next();
                    String compPkg = comp.getPackageName();
                    int length = strArr.length;
                    int i2 = i;
                    while (i2 < length) {
                        String pkg = strArr[i2];
                        if (compPkg.equals(pkg)) {
                            if (!doit) {
                                return true;
                            }
                            it.remove();
                            userState.getBindingServicesLocked().remove(comp);
                            userState.getCrashedServicesLocked().remove(comp);
                            AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userId);
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                        i2++;
                        strArr = packages;
                    }
                    strArr = packages;
                    i = 0;
                }
                return false;
            }
        }
    }

    private void registerBroadcastReceivers() {
        PackageMonitor monitor = new AnonymousClass1();
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.os.action.SETTING_RESTORED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.accessibility.AccessibilityManagerService.2
            {
                AccessibilityManagerService.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (AccessibilityManagerService.this.mTraceManager.isA11yTracingEnabledForTypes(65536L)) {
                    AccessibilityManagerService.this.mTraceManager.logTrace("AccessibilityManagerService.BR.onReceive", 65536L, "context=" + context + ";intent=" + intent);
                }
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    AccessibilityManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    AccessibilityManagerService.this.unlockUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    AccessibilityManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                    synchronized (AccessibilityManagerService.this.mLock) {
                        AccessibilityUserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                        if (AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    }
                } else if ("android.os.action.SETTING_RESTORED".equals(action)) {
                    String which = intent.getStringExtra("setting_name");
                    if ("enabled_accessibility_services".equals(which)) {
                        synchronized (AccessibilityManagerService.this.mLock) {
                            AccessibilityManagerService.this.restoreEnabledAccessibilityServicesLocked(intent.getStringExtra("previous_value"), intent.getStringExtra("new_value"), intent.getIntExtra("restored_from_sdk_int", 0));
                        }
                    } else if ("accessibility_display_magnification_navbar_enabled".equals(which)) {
                        synchronized (AccessibilityManagerService.this.mLock) {
                            AccessibilityManagerService.this.restoreLegacyDisplayMagnificationNavBarIfNeededLocked(intent.getStringExtra("new_value"), intent.getIntExtra("restored_from_sdk_int", 0));
                        }
                    } else if ("accessibility_button_targets".equals(which)) {
                        synchronized (AccessibilityManagerService.this.mLock) {
                            AccessibilityManagerService.this.restoreAccessibilityButtonTargetsLocked(intent.getStringExtra("previous_value"), intent.getStringExtra("new_value"));
                        }
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.safetycenter.action.SAFETY_CENTER_ENABLED_CHANGED");
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.accessibility.AccessibilityManagerService.3
            {
                AccessibilityManagerService.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                AccessibilityManagerService.this.setNonA11yToolNotificationToMatchSafetyCenter();
            }
        };
        this.mContext.registerReceiverAsUser(receiver, UserHandle.ALL, filter, null, this.mMainHandler, 2);
    }

    public void restoreLegacyDisplayMagnificationNavBarIfNeededLocked(String newSetting, int restoreFromSdkInt) {
        if (restoreFromSdkInt >= 30) {
            return;
        }
        try {
            boolean displayMagnificationNavBarEnabled = Integer.parseInt(newSetting) == 1;
            AccessibilityUserState userState = getUserStateLocked(0);
            ArraySet arraySet = new ArraySet();
            readColonDelimitedSettingToSet("accessibility_button_targets", userState.mUserId, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda14
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return AccessibilityManagerService.lambda$restoreLegacyDisplayMagnificationNavBarIfNeededLocked$0((String) obj);
                }
            }, arraySet);
            boolean targetsContainMagnification = arraySet.contains("com.android.server.accessibility.MagnificationController");
            if (targetsContainMagnification == displayMagnificationNavBarEnabled) {
                return;
            }
            if (displayMagnificationNavBarEnabled) {
                arraySet.add("com.android.server.accessibility.MagnificationController");
            } else {
                arraySet.remove("com.android.server.accessibility.MagnificationController");
            }
            persistColonDelimitedSetToSettingLocked("accessibility_button_targets", userState.mUserId, arraySet, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda15
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return AccessibilityManagerService.lambda$restoreLegacyDisplayMagnificationNavBarIfNeededLocked$1((String) obj);
                }
            });
            readAccessibilityButtonTargetsLocked(userState);
            onUserStateChangedLocked(userState);
        } catch (NumberFormatException e) {
            Slog.w(LOG_TAG, "number format is incorrect" + e);
        }
    }

    public static /* synthetic */ String lambda$restoreLegacyDisplayMagnificationNavBarIfNeededLocked$0(String str) {
        return str;
    }

    public static /* synthetic */ String lambda$restoreLegacyDisplayMagnificationNavBarIfNeededLocked$1(String str) {
        return str;
    }

    public long addClient(IAccessibilityManagerClient callback, int userId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.addClient", 4L, "callback=" + callback + ";userId=" + userId);
        }
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            AccessibilityUserState userState = getUserStateLocked(resolvedUserId);
            Client client = new Client(callback, Binder.getCallingUid(), userState);
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                this.mGlobalClients.register(callback, client);
                return IntPair.of(getClientStateLocked(userState), client.mLastSentRelevantEventTypes);
            }
            userState.mUserClients.register(callback, client);
            return IntPair.of(resolvedUserId == this.mCurrentUserId ? getClientStateLocked(userState) : 0, client.mLastSentRelevantEventTypes);
        }
    }

    public boolean removeClient(IAccessibilityManagerClient callback, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            AccessibilityUserState userState = getUserStateLocked(resolvedUserId);
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                boolean unregistered = this.mGlobalClients.unregister(callback);
                return unregistered;
            }
            boolean unregistered2 = userState.mUserClients.unregister(callback);
            return unregistered2;
        }
    }

    public void sendAccessibilityEvent(AccessibilityEvent event, int userId) {
        int resolvedUserId;
        AccessibilityWindowInfo pip;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.sendAccessibilityEvent", 4L, "event=" + event + ";userId=" + userId);
        }
        boolean dispatchEvent = false;
        synchronized (this.mLock) {
            if (event.getWindowId() == -3 && (pip = this.mA11yWindowManager.getPictureInPictureWindowLocked()) != null) {
                int pipId = pip.getId();
                event.setWindowId(pipId);
            }
            resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            event.setPackageName(this.mSecurityPolicy.resolveValidReportedPackageLocked(event.getPackageName(), UserHandle.getCallingAppId(), resolvedUserId, getCallingPid()));
            int i = this.mCurrentUserId;
            if (resolvedUserId == i) {
                if (this.mSecurityPolicy.canDispatchAccessibilityEventLocked(i, event)) {
                    this.mA11yWindowManager.updateActiveAndAccessibilityFocusedWindowLocked(this.mCurrentUserId, event.getWindowId(), event.getSourceNodeId(), event.getEventType(), event.getAction());
                    this.mSecurityPolicy.updateEventSourceLocked(event);
                    dispatchEvent = true;
                }
                if (this.mHasInputFilter && this.mInputFilter != null) {
                    this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda7
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((AccessibilityManagerService) obj).sendAccessibilityEventToInputFilter((AccessibilityEvent) obj2);
                        }
                    }, this, AccessibilityEvent.obtain(event)));
                }
            }
        }
        if (dispatchEvent) {
            boolean shouldComputeWindows = false;
            int displayId = -1;
            synchronized (this.mLock) {
                int windowId = event.getWindowId();
                if (windowId != -1) {
                    displayId = this.mA11yWindowManager.getDisplayIdByUserIdAndWindowIdLocked(resolvedUserId, windowId);
                    event.setDisplayId(displayId);
                }
                if (event.getEventType() == 32 && displayId != -1 && this.mA11yWindowManager.isTrackingWindowsLocked(displayId)) {
                    shouldComputeWindows = true;
                }
            }
            if (shouldComputeWindows) {
                if (this.mTraceManager.isA11yTracingEnabledForTypes(512L)) {
                    this.mTraceManager.logTrace("WindowManagerInternal.computeWindowsForAccessibility", 512L, "display=" + displayId);
                }
                WindowManagerInternal wm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                wm.computeWindowsForAccessibility(displayId);
                if (postponeWindowStateEvent(event)) {
                    return;
                }
            }
            synchronized (this.mLock) {
                dispatchAccessibilityEventLocked(event);
            }
        }
        if (OWN_PROCESS_ID != Binder.getCallingPid()) {
            event.recycle();
        }
    }

    public void dispatchAccessibilityEventLocked(AccessibilityEvent event) {
        notifyAccessibilityServicesDelayedLocked(event, false);
        notifyAccessibilityServicesDelayedLocked(event, true);
        this.mUiAutomationManager.sendAccessibilityEventLocked(event);
    }

    public void sendAccessibilityEventToInputFilter(AccessibilityEvent event) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.notifyAccessibilityEvent(event);
            }
        }
        event.recycle();
    }

    public void registerSystemAction(RemoteAction action, int actionId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.registerSystemAction", 4L, "action=" + action + ";actionId=" + actionId);
        }
        this.mSecurityPolicy.enforceCallingOrSelfPermission("android.permission.MANAGE_ACCESSIBILITY");
        getSystemActionPerformer().registerSystemAction(actionId, action);
    }

    public void unregisterSystemAction(int actionId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.unregisterSystemAction", 4L, "actionId=" + actionId);
        }
        this.mSecurityPolicy.enforceCallingOrSelfPermission("android.permission.MANAGE_ACCESSIBILITY");
        getSystemActionPerformer().unregisterSystemAction(actionId);
    }

    public SystemActionPerformer getSystemActionPerformer() {
        if (this.mSystemActionPerformer == null) {
            this.mSystemActionPerformer = new SystemActionPerformer(this.mContext, this.mWindowManagerService, null, this);
        }
        return this.mSystemActionPerformer;
    }

    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) {
        int resolvedUserId;
        List<AccessibilityServiceInfo> serviceInfos;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getInstalledAccessibilityServiceList", 4L, "userId=" + userId);
        }
        synchronized (this.mLock) {
            resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            serviceInfos = new ArrayList<>(getUserStateLocked(resolvedUserId).mInstalledServices);
        }
        if (Binder.getCallingPid() == OWN_PROCESS_ID) {
            return serviceInfos;
        }
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        for (int i = serviceInfos.size() - 1; i >= 0; i--) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
            if (pm.filterAppAccess(serviceInfo.getComponentName().getPackageName(), callingUid, resolvedUserId)) {
                serviceInfos.remove(i);
            }
        }
        return serviceInfos;
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getEnabledAccessibilityServiceList", 4L, "feedbackType=" + feedbackType + ";userId=" + userId);
        }
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            AccessibilityUserState userState = getUserStateLocked(resolvedUserId);
            if (this.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
                return Collections.emptyList();
            }
            List<AccessibilityServiceConnection> services = userState.mBoundServices;
            int serviceCount = services.size();
            List<AccessibilityServiceInfo> result = new ArrayList<>(serviceCount);
            for (int i = 0; i < serviceCount; i++) {
                AccessibilityServiceConnection service = services.get(i);
                if ((service.mFeedbackType & feedbackType) != 0 || feedbackType == -1) {
                    result.add(service.getServiceInfo());
                }
            }
            return result;
        }
    }

    public void interrupt(int userId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.interrupt", 4L, "userId=" + userId);
        }
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            if (resolvedUserId != this.mCurrentUserId) {
                return;
            }
            List<AccessibilityServiceConnection> services = getUserStateLocked(resolvedUserId).mBoundServices;
            int numServices = services.size();
            List<IAccessibilityServiceClient> interfacesToInterrupt = new ArrayList<>(numServices);
            for (int i = 0; i < numServices; i++) {
                AccessibilityServiceConnection service = services.get(i);
                IBinder a11yServiceBinder = service.mService;
                IAccessibilityServiceClient a11yServiceInterface = service.mServiceInterface;
                if (a11yServiceBinder != null && a11yServiceInterface != null) {
                    interfacesToInterrupt.add(a11yServiceInterface);
                }
            }
            int count = interfacesToInterrupt.size();
            for (int i2 = 0; i2 < count; i2++) {
                try {
                    if (this.mTraceManager.isA11yTracingEnabledForTypes(2L)) {
                        this.mTraceManager.logTrace("AccessibilityManagerService.IAccessibilityServiceClient.onInterrupt", 2L);
                    }
                    interfacesToInterrupt.get(i2).onInterrupt();
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Error sending interrupt request to " + interfacesToInterrupt.get(i2), re);
                }
            }
        }
    }

    public int addAccessibilityInteractionConnection(IWindow windowToken, IBinder leashToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.addAccessibilityInteractionConnection", 4L, "windowToken=" + windowToken + "leashToken=" + leashToken + ";connection=" + connection + "; packageName=" + packageName + ";userId=" + userId);
        }
        return this.mA11yWindowManager.addAccessibilityInteractionConnection(windowToken, leashToken, connection, packageName, userId);
    }

    public void removeAccessibilityInteractionConnection(IWindow window) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.removeAccessibilityInteractionConnection", 4L, "window=" + window);
        }
        this.mA11yWindowManager.removeAccessibilityInteractionConnection(window);
    }

    public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) throws RemoteException {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.setPictureInPictureActionReplacingConnection", 4L, "connection=" + connection);
        }
        this.mSecurityPolicy.enforceCallingPermission("android.permission.MODIFY_ACCESSIBILITY_DATA", SET_PIP_ACTION_REPLACEMENT);
        this.mA11yWindowManager.setPictureInPictureActionReplacingConnection(connection);
    }

    public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient serviceClient, AccessibilityServiceInfo accessibilityServiceInfo, int flags) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.registerUiTestAutomationService", 4L, "owner=" + owner + ";serviceClient=" + serviceClient + ";accessibilityServiceInfo=" + accessibilityServiceInfo + ";flags=" + flags);
        }
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_CONTENT", FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE);
        synchronized (this.mLock) {
            try {
                try {
                    UiAutomationManager uiAutomationManager = this.mUiAutomationManager;
                    Context context = this.mContext;
                    int i = sIdCounter;
                    sIdCounter = i + 1;
                    uiAutomationManager.registerUiTestAutomationServiceLocked(owner, serviceClient, context, accessibilityServiceInfo, i, this.mMainHandler, this.mSecurityPolicy, this, getTraceManager(), this.mWindowManagerService, getSystemActionPerformer(), this.mA11yWindowManager, flags);
                    onUserStateChangedLocked(getCurrentUserStateLocked());
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void unregisterUiTestAutomationService(IAccessibilityServiceClient serviceClient) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.unregisterUiTestAutomationService", 4L, "serviceClient=" + serviceClient);
        }
        synchronized (this.mLock) {
            this.mUiAutomationManager.unregisterUiTestAutomationServiceLocked(serviceClient);
        }
    }

    public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.temporaryEnableAccessibilityStateUntilKeyguardRemoved", 4L, "service=" + service + ";touchExplorationEnabled=" + touchExplorationEnabled);
        }
        this.mSecurityPolicy.enforceCallingPermission("android.permission.TEMPORARY_ENABLE_ACCESSIBILITY", TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED);
        if (this.mTraceManager.isA11yTracingEnabledForTypes(512L)) {
            this.mTraceManager.logTrace("WindowManagerInternal.isKeyguardLocked", 512L);
        }
        if (!this.mWindowManagerService.isKeyguardLocked()) {
            return;
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            userState.setTouchExplorationEnabledLocked(touchExplorationEnabled);
            userState.setDisplayMagnificationEnabledLocked(false);
            userState.disableShortcutMagnificationLocked();
            userState.setAutoclickEnabledLocked(false);
            userState.mEnabledServices.clear();
            userState.mEnabledServices.add(service);
            userState.getBindingServicesLocked().clear();
            userState.getCrashedServicesLocked().clear();
            userState.mTouchExplorationGrantedServices.clear();
            userState.mTouchExplorationGrantedServices.add(service);
            onUserStateChangedLocked(userState);
        }
    }

    public IBinder getWindowToken(int windowId, int userId) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getWindowToken", 4L, "windowId=" + windowId + ";userId=" + userId);
        }
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_TOKEN", GET_WINDOW_TOKEN);
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            if (resolvedUserId != this.mCurrentUserId) {
                return null;
            }
            AccessibilityWindowInfo accessibilityWindowInfo = this.mA11yWindowManager.findA11yWindowInfoByIdLocked(windowId);
            if (accessibilityWindowInfo == null) {
                return null;
            }
            return this.mA11yWindowManager.getWindowTokenForUserAndWindowIdLocked(userId, accessibilityWindowInfo.getId());
        }
    }

    public void notifyAccessibilityButtonClicked(int displayId, String targetName) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.notifyAccessibilityButtonClicked", 4L, "displayId=" + displayId + ";targetName=" + targetName);
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR_SERVICE");
        }
        if (targetName == null) {
            synchronized (this.mLock) {
                AccessibilityUserState userState = getCurrentUserStateLocked();
                targetName = userState.getTargetAssignedToAccessibilityButton();
            }
        }
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new AccessibilityManagerService$$ExternalSyntheticLambda1(), this, Integer.valueOf(displayId), 0, targetName));
    }

    public void notifyAccessibilityButtonVisibilityChanged(boolean shown) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.notifyAccessibilityButtonVisibilityChanged", 4L, "shown=" + shown);
        }
        this.mSecurityPolicy.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE");
        synchronized (this.mLock) {
            notifyAccessibilityButtonVisibilityChangedLocked(shown);
        }
    }

    public boolean onGesture(AccessibilityGestureEvent gestureEvent) {
        boolean handled;
        synchronized (this.mLock) {
            handled = notifyGestureLocked(gestureEvent, false);
            if (!handled) {
                handled = notifyGestureLocked(gestureEvent, true);
            }
        }
        return handled;
    }

    public boolean sendMotionEventToListeningServices(MotionEvent event) {
        boolean result = scheduleNotifyMotionEvent(MotionEvent.obtain(event));
        return result;
    }

    public boolean onTouchStateChanged(int displayId, int state) {
        return scheduleNotifyTouchState(displayId, state);
    }

    @Override // com.android.server.accessibility.SystemActionPerformer.SystemActionsChangedListener
    public void onSystemActionsChanged() {
        synchronized (this.mLock) {
            AccessibilityUserState state = getCurrentUserStateLocked();
            notifySystemActionsChangedLocked(state);
        }
    }

    void notifySystemActionsChangedLocked(AccessibilityUserState userState) {
        for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            service.notifySystemActionsChangedLocked();
        }
    }

    public boolean notifyKeyEvent(KeyEvent event, int policyFlags) {
        synchronized (this.mLock) {
            List<AccessibilityServiceConnection> boundServices = getCurrentUserStateLocked().mBoundServices;
            if (boundServices.isEmpty()) {
                return false;
            }
            return getKeyEventDispatcher().notifyKeyEventLocked(event, policyFlags, boundServices);
        }
    }

    public void notifyMagnificationChanged(int displayId, Region region, MagnificationConfig config) {
        synchronized (this.mLock) {
            notifyClearAccessibilityCacheLocked();
            notifyMagnificationChangedLocked(displayId, region, config);
        }
    }

    public void setMotionEventInjectors(SparseArray<MotionEventInjector> motionEventInjectors) {
        synchronized (this.mLock) {
            this.mMotionEventInjectors = motionEventInjectors;
            this.mLock.notifyAll();
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public MotionEventInjector getMotionEventInjectorForDisplayLocked(int displayId) {
        long endMillis = SystemClock.uptimeMillis() + 1000;
        while (this.mMotionEventInjectors == null && SystemClock.uptimeMillis() < endMillis) {
            try {
                this.mLock.wait(endMillis - SystemClock.uptimeMillis());
            } catch (InterruptedException e) {
            }
        }
        SparseArray<MotionEventInjector> sparseArray = this.mMotionEventInjectors;
        if (sparseArray == null) {
            Slog.e(LOG_TAG, "MotionEventInjector installation timed out");
            return null;
        }
        MotionEventInjector motionEventInjector = sparseArray.get(displayId);
        return motionEventInjector;
    }

    public boolean getAccessibilityFocusClickPointInScreen(Point outPoint) {
        return getInteractionBridge().getAccessibilityFocusClickPointInScreenNotLocked(outPoint);
    }

    public boolean performActionOnAccessibilityFocusedItem(AccessibilityNodeInfo.AccessibilityAction action) {
        return getInteractionBridge().performActionOnAccessibilityFocusedItemNotLocked(action);
    }

    public boolean accessibilityFocusOnlyInActiveWindow() {
        boolean accessibilityFocusOnlyInActiveWindowLocked;
        synchronized (this.mLock) {
            accessibilityFocusOnlyInActiveWindowLocked = this.mA11yWindowManager.accessibilityFocusOnlyInActiveWindowLocked();
        }
        return accessibilityFocusOnlyInActiveWindowLocked;
    }

    boolean getWindowBounds(int windowId, Rect outBounds) {
        IBinder token;
        synchronized (this.mLock) {
            token = getWindowToken(windowId, this.mCurrentUserId);
        }
        if (this.mTraceManager.isA11yTracingEnabledForTypes(512L)) {
            this.mTraceManager.logTrace("WindowManagerInternal.getWindowFrame", 512L, "token=" + token + ";outBounds=" + outBounds);
        }
        this.mWindowManagerService.getWindowFrame(token, outBounds);
        if (!outBounds.isEmpty()) {
            return true;
        }
        return false;
    }

    public int getActiveWindowId() {
        return this.mA11yWindowManager.getActiveWindowId(this.mCurrentUserId);
    }

    public void onTouchInteractionStart() {
        this.mA11yWindowManager.onTouchInteractionStart();
    }

    public void onTouchInteractionEnd() {
        this.mA11yWindowManager.onTouchInteractionEnd();
    }

    public void switchUser(int userId) {
        this.mMagnificationController.updateUserIdIfNeeded(userId);
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId && this.mInitialized) {
                return;
            }
            AccessibilityUserState oldUserState = getCurrentUserStateLocked();
            oldUserState.onSwitchToAnotherUserLocked();
            if (oldUserState.mUserClients.getRegisteredCallbackCount() > 0) {
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda16
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AccessibilityManagerService) obj).sendStateToClients(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                    }
                }, this, 0, Integer.valueOf(oldUserState.mUserId)));
            }
            UserManager userManager = (UserManager) this.mContext.getSystemService("user");
            boolean z = true;
            if (userManager.getUsers().size() <= 1) {
                z = false;
            }
            boolean announceNewUser = z;
            this.mCurrentUserId = userId;
            AccessibilityUserState userState = getCurrentUserStateLocked();
            readConfigurationForUserStateLocked(userState);
            this.mSecurityPolicy.onSwitchUserLocked(this.mCurrentUserId, userState.mEnabledServices);
            onUserStateChangedLocked(userState);
            migrateAccessibilityButtonSettingsIfNecessaryLocked(userState, null, 0);
            if (announceNewUser) {
                this.mMainHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda17
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((AccessibilityManagerService) obj).announceNewUserIfNeeded();
                    }
                }, this), BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
            }
        }
    }

    public void announceNewUserIfNeeded() {
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            if (userState.isHandlingAccessibilityEventsLocked()) {
                UserManager userManager = (UserManager) this.mContext.getSystemService("user");
                String message = this.mContext.getString(17041688, userManager.getUserInfo(this.mCurrentUserId).name);
                AccessibilityEvent event = AccessibilityEvent.obtain(16384);
                event.getText().add(message);
                sendAccessibilityEventLocked(event, this.mCurrentUserId);
            }
        }
    }

    public void unlockUser(int userId) {
        synchronized (this.mLock) {
            int parentUserId = this.mSecurityPolicy.resolveProfileParentLocked(userId);
            int i = this.mCurrentUserId;
            if (parentUserId == i) {
                AccessibilityUserState userState = getUserStateLocked(i);
                onUserStateChangedLocked(userState);
            }
        }
    }

    public void removeUser(int userId) {
        synchronized (this.mLock) {
            this.mUserStates.remove(userId);
        }
        getMagnificationController().onUserRemoved(userId);
    }

    void restoreEnabledAccessibilityServicesLocked(String oldSetting, String newSetting, int restoreFromSdkInt) {
        readComponentNamesFromStringLocked(oldSetting, this.mTempComponentNameSet, false);
        readComponentNamesFromStringLocked(newSetting, this.mTempComponentNameSet, true);
        AccessibilityUserState userState = getUserStateLocked(0);
        userState.mEnabledServices.clear();
        userState.mEnabledServices.addAll(this.mTempComponentNameSet);
        persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, 0);
        onUserStateChangedLocked(userState);
        migrateAccessibilityButtonSettingsIfNecessaryLocked(userState, null, restoreFromSdkInt);
    }

    void restoreAccessibilityButtonTargetsLocked(String oldSetting, String newSetting) {
        ArraySet arraySet = new ArraySet();
        readColonDelimitedStringToSet(oldSetting, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda10
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$restoreAccessibilityButtonTargetsLocked$2((String) obj);
            }
        }, arraySet, false);
        readColonDelimitedStringToSet(newSetting, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda11
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$restoreAccessibilityButtonTargetsLocked$3((String) obj);
            }
        }, arraySet, true);
        AccessibilityUserState userState = getUserStateLocked(0);
        userState.mAccessibilityButtonTargets.clear();
        userState.mAccessibilityButtonTargets.addAll((Collection<? extends String>) arraySet);
        persistColonDelimitedSetToSettingLocked("accessibility_button_targets", 0, userState.mAccessibilityButtonTargets, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$restoreAccessibilityButtonTargetsLocked$4((String) obj);
            }
        });
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
        onUserStateChangedLocked(userState);
    }

    public static /* synthetic */ String lambda$restoreAccessibilityButtonTargetsLocked$2(String str) {
        return str;
    }

    public static /* synthetic */ String lambda$restoreAccessibilityButtonTargetsLocked$3(String str) {
        return str;
    }

    public static /* synthetic */ String lambda$restoreAccessibilityButtonTargetsLocked$4(String str) {
        return str;
    }

    private int getClientStateLocked(AccessibilityUserState userState) {
        return userState.getClientStateLocked(this.mUiAutomationManager.isUiAutomationRunningLocked(), this.mTraceManager.getTraceStateForAccessibilityManagerClientState());
    }

    public InteractionBridge getInteractionBridge() {
        InteractionBridge interactionBridge;
        synchronized (this.mLock) {
            if (this.mInteractionBridge == null) {
                this.mInteractionBridge = new InteractionBridge();
            }
            interactionBridge = this.mInteractionBridge;
        }
        return interactionBridge;
    }

    private boolean notifyGestureLocked(AccessibilityGestureEvent gestureEvent, boolean isDefault) {
        AccessibilityUserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection service = state.mBoundServices.get(i);
            if (service.mRequestTouchExplorationMode && service.mIsDefault == isDefault) {
                service.notifyGesture(gestureEvent);
                return true;
            }
        }
        return false;
    }

    private boolean scheduleNotifyMotionEvent(MotionEvent event) {
        synchronized (this.mLock) {
            AccessibilityUserState state = getCurrentUserStateLocked();
            for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = state.mBoundServices.get(i);
                if (service.mRequestTouchExplorationMode) {
                    service.notifyMotionEvent(event);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean scheduleNotifyTouchState(int displayId, int touchState) {
        synchronized (this.mLock) {
            AccessibilityUserState state = getCurrentUserStateLocked();
            for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = state.mBoundServices.get(i);
                if (service.mRequestTouchExplorationMode) {
                    service.notifyTouchState(displayId, touchState);
                    return true;
                }
            }
            return false;
        }
    }

    public void notifyClearAccessibilityCacheLocked() {
        AccessibilityUserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection service = state.mBoundServices.get(i);
            service.notifyClearAccessibilityNodeInfoCache();
        }
    }

    private void notifyMagnificationChangedLocked(int displayId, Region region, MagnificationConfig config) {
        AccessibilityUserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection service = state.mBoundServices.get(i);
            service.notifyMagnificationChangedLocked(displayId, region, config);
        }
    }

    private void sendAccessibilityButtonToInputFilter(int displayId) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.notifyAccessibilityButtonClicked(displayId);
            }
        }
    }

    private void showAccessibilityTargetsSelection(int displayId, int shortcutType) {
        String chooserClassName;
        Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
        if (shortcutType == 1) {
            chooserClassName = AccessibilityShortcutChooserActivity.class.getName();
        } else {
            chooserClassName = AccessibilityButtonChooserActivity.class.getName();
        }
        intent.setClassName(PackageManagerService.PLATFORM_PACKAGE_NAME, chooserClassName);
        intent.addFlags(268468224);
        Bundle bundle = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle();
        this.mContext.startActivityAsUser(intent, bundle, UserHandle.of(this.mCurrentUserId));
    }

    private void launchShortcutTargetActivity(int displayId, ComponentName name) {
        Intent intent = new Intent();
        Bundle bundle = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle();
        intent.setComponent(name);
        intent.addFlags(268435456);
        try {
            this.mContext.startActivityAsUser(intent, bundle, UserHandle.of(this.mCurrentUserId));
        } catch (ActivityNotFoundException e) {
        }
    }

    private void notifyAccessibilityButtonVisibilityChangedLocked(boolean available) {
        AccessibilityUserState state = getCurrentUserStateLocked();
        this.mIsAccessibilityButtonShown = available;
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection clientConnection = state.mBoundServices.get(i);
            if (clientConnection.mRequestAccessibilityButton) {
                clientConnection.notifyAccessibilityButtonAvailabilityChangedLocked(clientConnection.isAccessibilityButtonAvailableLocked(state));
            }
        }
    }

    private boolean readInstalledAccessibilityServiceLocked(AccessibilityUserState userState) {
        this.mTempAccessibilityServiceInfoList.clear();
        int flags = userState.getBindInstantServiceAllowedLocked() ? 819332 | 8388608 : 819332;
        List<ResolveInfo> installedServices = this.mPackageManager.queryIntentServicesAsUser(new Intent("android.accessibilityservice.AccessibilityService"), flags, this.mCurrentUserId);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (this.mSecurityPolicy.canRegisterService(serviceInfo)) {
                try {
                    AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo(resolveInfo, this.mContext);
                    if (!accessibilityServiceInfo.isWithinParcelableSize()) {
                        Slog.e(LOG_TAG, "Skipping service " + accessibilityServiceInfo.getResolveInfo().getComponentInfo() + " because service info size is larger than safe parcelable limits.");
                    } else {
                        if (userState.mCrashedServices.contains(serviceInfo.getComponentName())) {
                            accessibilityServiceInfo.crashed = true;
                        }
                        this.mTempAccessibilityServiceInfoList.add(accessibilityServiceInfo);
                    }
                } catch (IOException | XmlPullParserException xppe) {
                    Slog.e(LOG_TAG, "Error while initializing AccessibilityServiceInfo", xppe);
                }
            }
        }
        if (!this.mTempAccessibilityServiceInfoList.equals(userState.mInstalledServices)) {
            userState.mInstalledServices.clear();
            userState.mInstalledServices.addAll(this.mTempAccessibilityServiceInfoList);
            this.mTempAccessibilityServiceInfoList.clear();
            return true;
        }
        this.mTempAccessibilityServiceInfoList.clear();
        return false;
    }

    private boolean readInstalledAccessibilityShortcutLocked(AccessibilityUserState userState) {
        List<AccessibilityShortcutInfo> shortcutInfos = AccessibilityManager.getInstance(this.mContext).getInstalledAccessibilityShortcutListAsUser(this.mContext, this.mCurrentUserId);
        if (!shortcutInfos.equals(userState.mInstalledShortcuts)) {
            userState.mInstalledShortcuts.clear();
            userState.mInstalledShortcuts.addAll(shortcutInfos);
            return true;
        }
        return false;
    }

    public boolean readEnabledAccessibilityServicesLocked(AccessibilityUserState userState) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("enabled_accessibility_services", userState.mUserId, this.mTempComponentNameSet);
        if (!this.mTempComponentNameSet.equals(userState.mEnabledServices)) {
            userState.mEnabledServices.clear();
            userState.mEnabledServices.addAll(this.mTempComponentNameSet);
            this.mTempComponentNameSet.clear();
            return true;
        }
        this.mTempComponentNameSet.clear();
        return false;
    }

    public boolean readTouchExplorationGrantedAccessibilityServicesLocked(AccessibilityUserState userState) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("touch_exploration_granted_accessibility_services", userState.mUserId, this.mTempComponentNameSet);
        if (!this.mTempComponentNameSet.equals(userState.mTouchExplorationGrantedServices)) {
            userState.mTouchExplorationGrantedServices.clear();
            userState.mTouchExplorationGrantedServices.addAll(this.mTempComponentNameSet);
            this.mTempComponentNameSet.clear();
            return true;
        }
        this.mTempComponentNameSet.clear();
        return false;
    }

    private void notifyAccessibilityServicesDelayedLocked(AccessibilityEvent event, boolean isDefault) {
        try {
            AccessibilityUserState state = getCurrentUserStateLocked();
            int count = state.mBoundServices.size();
            for (int i = 0; i < count; i++) {
                AccessibilityServiceConnection service = state.mBoundServices.get(i);
                if (service.mIsDefault == isDefault) {
                    service.notifyAccessibilityEvent(event);
                }
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void updateRelevantEventsLocked(final AccessibilityUserState userState) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(2L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.updateRelevantEventsLocked", 2L, "userState=" + userState);
        }
        this.mMainHandler.post(new Runnable() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                AccessibilityManagerService.this.m655xbb9d3f63(userState);
            }
        });
    }

    /* renamed from: lambda$updateRelevantEventsLocked$6$com-android-server-accessibility-AccessibilityManagerService */
    public /* synthetic */ void m655xbb9d3f63(final AccessibilityUserState userState) {
        broadcastToClients(userState, FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda30
            public final void acceptOrThrow(Object obj) {
                AccessibilityManagerService.this.m654x275ecfc4(userState, (AccessibilityManagerService.Client) obj);
            }
        }));
    }

    /* renamed from: lambda$updateRelevantEventsLocked$5$com-android-server-accessibility-AccessibilityManagerService */
    public /* synthetic */ void m654x275ecfc4(AccessibilityUserState userState, Client client) throws RemoteException {
        int relevantEventTypes;
        boolean changed = false;
        synchronized (this.mLock) {
            relevantEventTypes = computeRelevantEventTypesLocked(userState, client);
            if (client.mLastSentRelevantEventTypes != relevantEventTypes) {
                client.mLastSentRelevantEventTypes = relevantEventTypes;
                changed = true;
            }
        }
        if (changed) {
            client.mCallback.setRelevantEventTypes(relevantEventTypes);
        }
    }

    public int computeRelevantEventTypesLocked(AccessibilityUserState userState, Client client) {
        int relevantEventTypes = 0;
        int serviceCount = userState.mBoundServices.size();
        int i = 0;
        while (true) {
            if (i >= serviceCount) {
                break;
            }
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            if (isClientInPackageAllowlist(service.getServiceInfo(), client)) {
                r3 = service.getRelevantEventTypes();
            }
            relevantEventTypes |= r3;
            i++;
        }
        return relevantEventTypes | (isClientInPackageAllowlist(this.mUiAutomationManager.getServiceInfo(), client) ? this.mUiAutomationManager.getRelevantEventTypes() : 0);
    }

    public void updateMagnificationModeChangeSettingsLocked(AccessibilityUserState userState, int displayId) {
        if (userState.mUserId != this.mCurrentUserId || fallBackMagnificationModeSettingsLocked(userState, displayId)) {
            return;
        }
        this.mMagnificationController.transitionMagnificationModeLocked(displayId, userState.getMagnificationModeLocked(displayId), new MagnificationController.TransitionCallBack() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda13
            @Override // com.android.server.accessibility.magnification.MagnificationController.TransitionCallBack
            public final void onResult(int i, boolean z) {
                AccessibilityManagerService.this.onMagnificationTransitionEndedLocked(i, z);
            }
        });
    }

    public void onMagnificationTransitionEndedLocked(int displayId, boolean success) {
        AccessibilityUserState userState = getCurrentUserStateLocked();
        int previousMode = userState.getMagnificationModeLocked(displayId) ^ 3;
        if (!success && previousMode != 0) {
            userState.setMagnificationModeLocked(displayId, previousMode);
            if (displayId == 0) {
                persistMagnificationModeSettingsLocked(previousMode);
                return;
            }
            return;
        }
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda39
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).notifyRefreshMagnificationModeToInputFilter(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(displayId)));
    }

    public void notifyRefreshMagnificationModeToInputFilter(int displayId) {
        synchronized (this.mLock) {
            if (this.mHasInputFilter) {
                ArrayList<Display> displays = getValidDisplayList();
                for (int i = 0; i < displays.size(); i++) {
                    Display display = displays.get(i);
                    if (display != null && display.getDisplayId() == displayId) {
                        this.mInputFilter.refreshMagnificationMode(display);
                        return;
                    }
                }
            }
        }
    }

    private static boolean isClientInPackageAllowlist(AccessibilityServiceInfo serviceInfo, Client client) {
        if (serviceInfo == null) {
            return false;
        }
        String[] clientPackages = client.mPackageNames;
        boolean result = ArrayUtils.isEmpty(serviceInfo.packageNames);
        if (!result && clientPackages != null) {
            for (String packageName : clientPackages) {
                if (ArrayUtils.contains(serviceInfo.packageNames, packageName)) {
                    return true;
                }
            }
            return result;
        }
        return result;
    }

    private void broadcastToClients(AccessibilityUserState userState, Consumer<Client> clientAction) {
        this.mGlobalClients.broadcastForEachCookie(clientAction);
        userState.mUserClients.broadcastForEachCookie(clientAction);
    }

    private void readComponentNamesFromSettingLocked(String settingName, int userId, Set<ComponentName> outComponentNames) {
        readColonDelimitedSettingToSet(settingName, userId, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda40
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                ComponentName unflattenFromString;
                unflattenFromString = ComponentName.unflattenFromString((String) obj);
                return unflattenFromString;
            }
        }, outComponentNames);
    }

    private void readComponentNamesFromStringLocked(String names, Set<ComponentName> outComponentNames, boolean doMerge) {
        readColonDelimitedStringToSet(names, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda9
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                ComponentName unflattenFromString;
                unflattenFromString = ComponentName.unflattenFromString((String) obj);
                return unflattenFromString;
            }
        }, outComponentNames, doMerge);
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void persistComponentNamesToSettingLocked(String settingName, Set<ComponentName> componentNames, int userId) {
        persistColonDelimitedSetToSettingLocked(settingName, userId, componentNames, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda24
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String flattenToShortString;
                flattenToShortString = ((ComponentName) obj).flattenToShortString();
                return flattenToShortString;
            }
        });
    }

    private <T> void readColonDelimitedSettingToSet(String settingName, int userId, Function<String, T> toItem, Set<T> outSet) {
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), settingName, userId);
        readColonDelimitedStringToSet(settingValue, toItem, outSet, false);
    }

    private <T> void readColonDelimitedStringToSet(String names, Function<String, T> toItem, Set<T> outSet, boolean doMerge) {
        T item;
        if (!doMerge) {
            outSet.clear();
        }
        if (!TextUtils.isEmpty(names)) {
            TextUtils.SimpleStringSplitter splitter = this.mStringColonSplitter;
            splitter.setString(names);
            while (splitter.hasNext()) {
                String str = splitter.next();
                if (!TextUtils.isEmpty(str) && (item = toItem.apply(str)) != null) {
                    outSet.add(item);
                }
            }
        }
    }

    private <T> void persistColonDelimitedSetToSettingLocked(String settingName, int userId, Set<T> set, Function<T, String> toString) {
        String str;
        StringBuilder builder = new StringBuilder();
        Iterator<T> it = set.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            T item = it.next();
            str = item != null ? toString.apply(item) : null;
            if (!TextUtils.isEmpty(str)) {
                if (builder.length() > 0) {
                    builder.append(COMPONENT_NAME_SEPARATOR);
                }
                builder.append(str);
            }
        }
        long identity = Binder.clearCallingIdentity();
        try {
            String settingValue = builder.toString();
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (!TextUtils.isEmpty(settingValue)) {
                str = settingValue;
            }
            Settings.Secure.putStringForUser(contentResolver, settingName, str, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void updateServicesLocked(AccessibilityUserState userState) {
        int i;
        int count;
        Map<ComponentName, AccessibilityServiceConnection> componentNameToServiceMap;
        AccessibilityManagerService accessibilityManagerService;
        AccessibilityUserState accessibilityUserState;
        AccessibilityServiceConnection service;
        AccessibilityServiceConnection service2;
        AccessibilityManagerService accessibilityManagerService2 = this;
        AccessibilityUserState accessibilityUserState2 = userState;
        Map<ComponentName, AccessibilityServiceConnection> componentNameToServiceMap2 = accessibilityUserState2.mComponentNameToServiceMap;
        boolean isUnlockingOrUnlocked = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).isUserUnlockingOrUnlocked(accessibilityUserState2.mUserId);
        int count2 = accessibilityUserState2.mInstalledServices.size();
        int i2 = 0;
        while (i2 < count2) {
            AccessibilityServiceInfo installedService = accessibilityUserState2.mInstalledServices.get(i2);
            ComponentName componentName = ComponentName.unflattenFromString(installedService.getId());
            AccessibilityServiceConnection service3 = componentNameToServiceMap2.get(componentName);
            if (!isUnlockingOrUnlocked && !installedService.isDirectBootAware()) {
                Slog.d(LOG_TAG, "Ignoring non-encryption-aware service " + componentName);
                i = i2;
                count = count2;
                componentNameToServiceMap = componentNameToServiceMap2;
                accessibilityManagerService = accessibilityManagerService2;
                accessibilityUserState = accessibilityUserState2;
            } else if (userState.getBindingServicesLocked().contains(componentName)) {
                i = i2;
                count = count2;
                componentNameToServiceMap = componentNameToServiceMap2;
                accessibilityManagerService = accessibilityManagerService2;
                accessibilityUserState = accessibilityUserState2;
            } else if (userState.getCrashedServicesLocked().contains(componentName)) {
                i = i2;
                count = count2;
                componentNameToServiceMap = componentNameToServiceMap2;
                accessibilityManagerService = accessibilityManagerService2;
                accessibilityUserState = accessibilityUserState2;
            } else {
                if (!accessibilityUserState2.mEnabledServices.contains(componentName)) {
                    service = service3;
                    i = i2;
                    count = count2;
                    componentNameToServiceMap = componentNameToServiceMap2;
                    accessibilityUserState = accessibilityUserState2;
                } else if (accessibilityManagerService2.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
                    service = service3;
                    i = i2;
                    count = count2;
                    componentNameToServiceMap = componentNameToServiceMap2;
                    accessibilityUserState = accessibilityUserState2;
                } else {
                    if (service3 == null) {
                        Context context = accessibilityManagerService2.mContext;
                        int i3 = sIdCounter;
                        sIdCounter = i3 + 1;
                        MainHandler mainHandler = accessibilityManagerService2.mMainHandler;
                        Object obj = accessibilityManagerService2.mLock;
                        AccessibilitySecurityPolicy accessibilitySecurityPolicy = accessibilityManagerService2.mSecurityPolicy;
                        AccessibilityTraceManager traceManager = getTraceManager();
                        WindowManagerInternal windowManagerInternal = accessibilityManagerService2.mWindowManagerService;
                        SystemActionPerformer systemActionPerformer = getSystemActionPerformer();
                        AccessibilityWindowManager accessibilityWindowManager = accessibilityManagerService2.mA11yWindowManager;
                        ActivityTaskManagerInternal activityTaskManagerInternal = accessibilityManagerService2.mActivityTaskManagerService;
                        i = i2;
                        count = count2;
                        componentNameToServiceMap = componentNameToServiceMap2;
                        accessibilityUserState = accessibilityUserState2;
                        service2 = new AccessibilityServiceConnection(userState, context, componentName, installedService, i3, mainHandler, obj, accessibilitySecurityPolicy, this, traceManager, windowManagerInternal, systemActionPerformer, accessibilityWindowManager, activityTaskManagerInternal);
                    } else {
                        i = i2;
                        count = count2;
                        componentNameToServiceMap = componentNameToServiceMap2;
                        accessibilityUserState = accessibilityUserState2;
                        if (!accessibilityUserState.mBoundServices.contains(service3)) {
                            service2 = service3;
                        } else {
                            accessibilityManagerService = this;
                        }
                    }
                    service2.bindLocked();
                    accessibilityManagerService = this;
                }
                if (service == null) {
                    accessibilityManagerService = this;
                } else {
                    service.unbindLocked();
                    accessibilityManagerService = this;
                    accessibilityManagerService.removeShortcutTargetForUnboundServiceLocked(accessibilityUserState, service);
                }
            }
            i2 = i + 1;
            accessibilityUserState2 = accessibilityUserState;
            componentNameToServiceMap2 = componentNameToServiceMap;
            count2 = count;
            accessibilityManagerService2 = accessibilityManagerService;
        }
        AccessibilityManagerService accessibilityManagerService3 = accessibilityManagerService2;
        AccessibilityUserState accessibilityUserState3 = accessibilityUserState2;
        int count3 = accessibilityUserState3.mBoundServices.size();
        accessibilityManagerService3.mTempIntArray.clear();
        for (int i4 = 0; i4 < count3; i4++) {
            ResolveInfo resolveInfo = accessibilityUserState3.mBoundServices.get(i4).mAccessibilityServiceInfo.getResolveInfo();
            if (resolveInfo != null) {
                accessibilityManagerService3.mTempIntArray.add(resolveInfo.serviceInfo.applicationInfo.uid);
            }
        }
        AudioManagerInternal audioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (audioManager != null) {
            audioManager.setAccessibilityServiceUids(accessibilityManagerService3.mTempIntArray);
        }
        accessibilityManagerService3.mActivityTaskManagerService.setAccessibilityServiceUids(accessibilityManagerService3.mTempIntArray);
        updateAccessibilityEnabledSettingLocked(userState);
    }

    void scheduleUpdateClientsIfNeeded(AccessibilityUserState userState) {
        synchronized (this.mLock) {
            scheduleUpdateClientsIfNeededLocked(userState);
        }
    }

    public void scheduleUpdateClientsIfNeededLocked(AccessibilityUserState userState) {
        int clientState = getClientStateLocked(userState);
        if (userState.getLastSentClientStateLocked() != clientState) {
            if (this.mGlobalClients.getRegisteredCallbackCount() > 0 || userState.mUserClients.getRegisteredCallbackCount() > 0) {
                userState.setLastSentClientStateLocked(clientState);
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda53
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AccessibilityManagerService) obj).sendStateToAllClients(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                    }
                }, this, Integer.valueOf(clientState), Integer.valueOf(userState.mUserId)));
            }
        }
    }

    public void sendStateToAllClients(int clientState, int userId) {
        sendStateToClients(clientState, this.mGlobalClients);
        sendStateToClients(clientState, userId);
    }

    public void sendStateToClients(int clientState, int userId) {
        sendStateToClients(clientState, getUserState(userId).mUserClients);
    }

    private void sendStateToClients(final int clientState, RemoteCallbackList<IAccessibilityManagerClient> clients) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(8L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.sendStateToClients", 8L, "clientState=" + clientState);
        }
        clients.broadcast(FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda34
            public final void acceptOrThrow(Object obj) {
                ((IAccessibilityManagerClient) obj).setState(clientState);
            }
        }));
    }

    private void scheduleNotifyClientsOfServicesStateChangeLocked(AccessibilityUserState userState) {
        updateRecommendedUiTimeoutLocked(userState);
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda29
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).sendServicesStateChanged((RemoteCallbackList) obj2, ((Long) obj3).longValue());
            }
        }, this, userState.mUserClients, Long.valueOf(getRecommendedTimeoutMillisLocked(userState))));
    }

    public void sendServicesStateChanged(RemoteCallbackList<IAccessibilityManagerClient> userClients, long uiTimeout) {
        notifyClientsOfServicesStateChange(this.mGlobalClients, uiTimeout);
        notifyClientsOfServicesStateChange(userClients, uiTimeout);
    }

    private void notifyClientsOfServicesStateChange(RemoteCallbackList<IAccessibilityManagerClient> clients, final long uiTimeout) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(8L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.notifyClientsOfServicesStateChange", 8L, "uiTimeout=" + uiTimeout);
        }
        clients.broadcast(FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda25
            public final void acceptOrThrow(Object obj) {
                ((IAccessibilityManagerClient) obj).notifyServicesStateChanged(uiTimeout);
            }
        }));
    }

    private void scheduleUpdateInputFilter(AccessibilityUserState userState) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda35
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).updateInputFilter((AccessibilityUserState) obj2);
            }
        }, this, userState));
    }

    private void scheduleUpdateFingerprintGestureHandling(AccessibilityUserState userState) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda18
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).updateFingerprintGestureHandling((AccessibilityUserState) obj2);
            }
        }, this, userState));
    }

    public void updateInputFilter(AccessibilityUserState userState) {
        if (this.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
            return;
        }
        boolean setInputFilter = false;
        IInputFilter iInputFilter = null;
        synchronized (this.mLock) {
            int flags = 0;
            if (userState.isDisplayMagnificationEnabledLocked()) {
                flags = 0 | 1;
            }
            if (userState.isShortcutMagnificationEnabledLocked()) {
                flags |= 64;
            }
            if (userHasMagnificationServicesLocked(userState)) {
                flags |= 32;
            }
            if (userState.isHandlingAccessibilityEventsLocked() && userState.isTouchExplorationEnabledLocked()) {
                flags |= 2;
                if (userState.isServiceHandlesDoubleTapEnabledLocked()) {
                    flags |= 128;
                }
                if (userState.isMultiFingerGesturesEnabledLocked()) {
                    flags |= 256;
                }
                if (userState.isTwoFingerPassthroughEnabledLocked()) {
                    flags |= 512;
                }
            }
            if (userState.isFilterKeyEventsEnabledLocked()) {
                flags |= 4;
            }
            if (userState.isSendMotionEventsEnabled()) {
                flags |= 1024;
            }
            if (userState.isAutoclickEnabledLocked()) {
                flags |= 8;
            }
            if (userState.isPerformGesturesEnabledLocked()) {
                flags |= 16;
            }
            if (flags != 0) {
                if (!this.mHasInputFilter) {
                    this.mHasInputFilter = true;
                    if (this.mInputFilter == null) {
                        this.mInputFilter = new AccessibilityInputFilter(this.mContext, this);
                    }
                    iInputFilter = this.mInputFilter;
                    setInputFilter = true;
                }
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, flags);
            } else if (this.mHasInputFilter) {
                this.mHasInputFilter = false;
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, 0);
                iInputFilter = null;
                setInputFilter = true;
            }
        }
        if (setInputFilter) {
            if (this.mTraceManager.isA11yTracingEnabledForTypes(4608L)) {
                this.mTraceManager.logTrace("WindowManagerInternal.setInputFilter", 4608L, "inputFilter=" + iInputFilter);
            }
            this.mWindowManagerService.setInputFilter(iInputFilter);
        }
    }

    public void showEnableTouchExplorationDialog(final AccessibilityServiceConnection service) {
        synchronized (this.mLock) {
            String label = service.getServiceInfo().getResolveInfo().loadLabel(this.mContext.getPackageManager()).toString();
            final AccessibilityUserState userState = getCurrentUserStateLocked();
            if (userState.isTouchExplorationEnabledLocked()) {
                return;
            }
            AlertDialog alertDialog = this.mEnableTouchExplorationDialog;
            if (alertDialog == null || !alertDialog.isShowing()) {
                AlertDialog create = new AlertDialog.Builder(this.mContext).setIconAttribute(16843605).setPositiveButton(17039370, new DialogInterface.OnClickListener() { // from class: com.android.server.accessibility.AccessibilityManagerService.5
                    {
                        AccessibilityManagerService.this = this;
                    }

                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        userState.mTouchExplorationGrantedServices.add(service.mComponentName);
                        AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", userState.mTouchExplorationGrantedServices, userState.mUserId);
                        userState.setTouchExplorationEnabledLocked(true);
                        long identity = Binder.clearCallingIdentity();
                        try {
                            Settings.Secure.putIntForUser(AccessibilityManagerService.this.mContext.getContentResolver(), "touch_exploration_enabled", 1, userState.mUserId);
                            Binder.restoreCallingIdentity(identity);
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                }).setNegativeButton(17039360, new DialogInterface.OnClickListener() { // from class: com.android.server.accessibility.AccessibilityManagerService.4
                    {
                        AccessibilityManagerService.this = this;
                    }

                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setTitle(17040210).setMessage(this.mContext.getString(17040209, label)).create();
                this.mEnableTouchExplorationDialog = create;
                create.getWindow().setType(2003);
                this.mEnableTouchExplorationDialog.getWindow().getAttributes().privateFlags |= 16;
                this.mEnableTouchExplorationDialog.setCanceledOnTouchOutside(true);
                this.mEnableTouchExplorationDialog.show();
            }
        }
    }

    public void onUserStateChangedLocked(AccessibilityUserState userState) {
        this.mInitialized = true;
        updateLegacyCapabilitiesLocked(userState);
        updateServicesLocked(userState);
        updateWindowsForAccessibilityCallbackLocked(userState);
        updateFilterKeyEventsLocked(userState);
        updateTouchExplorationLocked(userState);
        updatePerformGesturesLocked(userState);
        updateMagnificationLocked(userState);
        scheduleUpdateFingerprintGestureHandling(userState);
        scheduleUpdateInputFilter(userState);
        updateRelevantEventsLocked(userState);
        scheduleUpdateClientsIfNeededLocked(userState);
        updateAccessibilityShortcutKeyTargetsLocked(userState);
        updateAccessibilityButtonTargetsLocked(userState);
        updateMagnificationCapabilitiesSettingsChangeLocked(userState);
        updateMagnificationModeChangeSettingsForAllDisplaysLocked(userState);
        updateFocusAppearanceDataLocked(userState);
    }

    private void updateMagnificationModeChangeSettingsForAllDisplaysLocked(AccessibilityUserState userState) {
        ArrayList<Display> displays = getValidDisplayList();
        for (int i = 0; i < displays.size(); i++) {
            int displayId = displays.get(i).getDisplayId();
            updateMagnificationModeChangeSettingsLocked(userState, displayId);
        }
    }

    public void updateWindowsForAccessibilityCallbackLocked(AccessibilityUserState userState) {
        boolean observingWindows = this.mUiAutomationManager.canRetrieveInteractiveWindowsLocked();
        List<AccessibilityServiceConnection> boundServices = userState.mBoundServices;
        int boundServiceCount = boundServices.size();
        for (int i = 0; !observingWindows && i < boundServiceCount; i++) {
            AccessibilityServiceConnection boundService = boundServices.get(i);
            if (boundService.canRetrieveInteractiveWindowsLocked()) {
                userState.setAccessibilityFocusOnlyInActiveWindow(false);
                observingWindows = true;
            }
        }
        userState.setAccessibilityFocusOnlyInActiveWindow(true);
        ArrayList<Display> displays = getValidDisplayList();
        for (int i2 = 0; i2 < displays.size(); i2++) {
            Display display = displays.get(i2);
            if (display != null) {
                if (observingWindows) {
                    this.mA11yWindowManager.startTrackingWindows(display.getDisplayId());
                } else {
                    this.mA11yWindowManager.stopTrackingWindows(display.getDisplayId());
                }
            }
        }
    }

    private void updateLegacyCapabilitiesLocked(AccessibilityUserState userState) {
        int installedServiceCount = userState.mInstalledServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            AccessibilityServiceInfo serviceInfo = userState.mInstalledServices.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if ((serviceInfo.getCapabilities() & 2) == 0 && resolveInfo.serviceInfo.applicationInfo.targetSdkVersion <= 17) {
                ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                if (userState.mTouchExplorationGrantedServices.contains(componentName)) {
                    serviceInfo.setCapabilities(serviceInfo.getCapabilities() | 2);
                }
            }
        }
    }

    private void updatePerformGesturesLocked(AccessibilityUserState userState) {
        int serviceCount = userState.mBoundServices.size();
        for (int i = 0; i < serviceCount; i++) {
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            if ((service.getCapabilities() & 32) != 0) {
                userState.setPerformGesturesEnabledLocked(true);
                return;
            }
        }
        userState.setPerformGesturesEnabledLocked(false);
    }

    private void updateFilterKeyEventsLocked(AccessibilityUserState userState) {
        int serviceCount = userState.mBoundServices.size();
        for (int i = 0; i < serviceCount; i++) {
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            if (service.mRequestFilterKeyEvents && (service.getCapabilities() & 8) != 0) {
                userState.setFilterKeyEventsEnabledLocked(true);
                return;
            }
        }
        userState.setFilterKeyEventsEnabledLocked(false);
    }

    public boolean readConfigurationForUserStateLocked(AccessibilityUserState userState) {
        boolean somethingChanged = readInstalledAccessibilityServiceLocked(userState);
        return somethingChanged | readInstalledAccessibilityShortcutLocked(userState) | readEnabledAccessibilityServicesLocked(userState) | readTouchExplorationGrantedAccessibilityServicesLocked(userState) | readTouchExplorationEnabledSettingLocked(userState) | readHighTextContrastEnabledSettingLocked(userState) | readAudioDescriptionEnabledSettingLocked(userState) | readMagnificationEnabledSettingsLocked(userState) | readAutoclickEnabledSettingLocked(userState) | readAccessibilityShortcutKeySettingLocked(userState) | readAccessibilityButtonTargetsLocked(userState) | readAccessibilityButtonTargetComponentLocked(userState) | readUserRecommendedUiTimeoutSettingsLocked(userState) | readMagnificationModeForDefaultDisplayLocked(userState) | readMagnificationCapabilitiesLocked(userState) | readMagnificationFollowTypingLocked(userState);
    }

    private void updateAccessibilityEnabledSettingLocked(AccessibilityUserState userState) {
        boolean isA11yEnabled = this.mUiAutomationManager.isUiAutomationRunningLocked() || userState.isHandlingAccessibilityEventsLocked();
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", isA11yEnabled ? 1 : 0, userState.mUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean readTouchExplorationEnabledSettingLocked(AccessibilityUserState userState) {
        boolean touchExplorationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", 0, userState.mUserId) == 1;
        if (touchExplorationEnabled != userState.isTouchExplorationEnabledLocked()) {
            userState.setTouchExplorationEnabledLocked(touchExplorationEnabled);
            return true;
        }
        return false;
    }

    public boolean readMagnificationEnabledSettingsLocked(AccessibilityUserState userState) {
        boolean displayMagnificationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, userState.mUserId) == 1;
        if (displayMagnificationEnabled != userState.isDisplayMagnificationEnabledLocked()) {
            userState.setDisplayMagnificationEnabledLocked(displayMagnificationEnabled);
            return true;
        }
        return false;
    }

    public boolean readAutoclickEnabledSettingLocked(AccessibilityUserState userState) {
        boolean autoclickEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_autoclick_enabled", 0, userState.mUserId) == 1;
        if (autoclickEnabled != userState.isAutoclickEnabledLocked()) {
            userState.setAutoclickEnabledLocked(autoclickEnabled);
            return true;
        }
        return false;
    }

    public boolean readHighTextContrastEnabledSettingLocked(AccessibilityUserState userState) {
        boolean highTextContrastEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "high_text_contrast_enabled", 0, userState.mUserId) == 1;
        if (highTextContrastEnabled != userState.isTextHighContrastEnabledLocked()) {
            userState.setTextHighContrastEnabledLocked(highTextContrastEnabled);
            return true;
        }
        return false;
    }

    public boolean readAudioDescriptionEnabledSettingLocked(AccessibilityUserState userState) {
        boolean audioDescriptionByDefaultEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "enabled_accessibility_audio_description_by_default", 0, userState.mUserId) == 1;
        if (audioDescriptionByDefaultEnabled != userState.isAudioDescriptionByDefaultEnabledLocked()) {
            userState.setAudioDescriptionByDefaultEnabledLocked(audioDescriptionByDefaultEnabled);
            return true;
        }
        return false;
    }

    private void updateTouchExplorationLocked(AccessibilityUserState userState) {
        boolean touchExplorationEnabled = this.mUiAutomationManager.isTouchExplorationEnabledLocked();
        boolean serviceHandlesDoubleTapEnabled = false;
        boolean requestMultiFingerGestures = false;
        boolean requestTwoFingerPassthrough = false;
        boolean sendMotionEvents = false;
        int serviceCount = userState.mBoundServices.size();
        int i = 0;
        while (true) {
            if (i >= serviceCount) {
                break;
            }
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            if (!canRequestAndRequestsTouchExplorationLocked(service, userState)) {
                i++;
            } else {
                touchExplorationEnabled = true;
                serviceHandlesDoubleTapEnabled = service.isServiceHandlesDoubleTapEnabled();
                requestMultiFingerGestures = service.isMultiFingerGesturesEnabled();
                requestTwoFingerPassthrough = service.isTwoFingerPassthroughEnabled();
                sendMotionEvents = service.isSendMotionEventsEnabled();
                break;
            }
        }
        if (touchExplorationEnabled != userState.isTouchExplorationEnabledLocked()) {
            userState.setTouchExplorationEnabledLocked(touchExplorationEnabled);
            long identity = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", touchExplorationEnabled ? 1 : 0, userState.mUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        userState.setServiceHandlesDoubleTapLocked(serviceHandlesDoubleTapEnabled);
        userState.setMultiFingerGesturesLocked(requestMultiFingerGestures);
        userState.setTwoFingerPassthroughLocked(requestTwoFingerPassthrough);
        userState.setSendMotionEventsEnabled(sendMotionEvents);
    }

    public boolean readAccessibilityShortcutKeySettingLocked(AccessibilityUserState userState) {
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "accessibility_shortcut_target_service", userState.mUserId);
        ArraySet arraySet = new ArraySet();
        readColonDelimitedStringToSet(settingValue, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda38
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$readAccessibilityShortcutKeySettingLocked$12((String) obj);
            }
        }, arraySet, false);
        if (settingValue == null) {
            String defaultService = this.mContext.getString(17039915);
            if (!TextUtils.isEmpty(defaultService)) {
                arraySet.add(defaultService);
            }
        }
        Set<String> currentTargets = userState.getShortcutTargetsLocked(1);
        if (arraySet.equals(currentTargets)) {
            return false;
        }
        currentTargets.clear();
        currentTargets.addAll(arraySet);
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
        return true;
    }

    public static /* synthetic */ String lambda$readAccessibilityShortcutKeySettingLocked$12(String str) {
        return str;
    }

    public boolean readAccessibilityButtonTargetsLocked(AccessibilityUserState userState) {
        ArraySet arraySet = new ArraySet();
        readColonDelimitedSettingToSet("accessibility_button_targets", userState.mUserId, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda23
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$readAccessibilityButtonTargetsLocked$13((String) obj);
            }
        }, arraySet);
        Set<String> currentTargets = userState.getShortcutTargetsLocked(0);
        if (arraySet.equals(currentTargets)) {
            return false;
        }
        currentTargets.clear();
        currentTargets.addAll(arraySet);
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
        return true;
    }

    public static /* synthetic */ String lambda$readAccessibilityButtonTargetsLocked$13(String str) {
        return str;
    }

    public boolean readAccessibilityButtonTargetComponentLocked(AccessibilityUserState userState) {
        String componentId = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "accessibility_button_target_component", userState.mUserId);
        if (TextUtils.isEmpty(componentId)) {
            if (userState.getTargetAssignedToAccessibilityButton() == null) {
                return false;
            }
            userState.setTargetAssignedToAccessibilityButton(null);
            return true;
        } else if (componentId.equals(userState.getTargetAssignedToAccessibilityButton())) {
            return false;
        } else {
            userState.setTargetAssignedToAccessibilityButton(componentId);
            return true;
        }
    }

    public boolean readUserRecommendedUiTimeoutSettingsLocked(AccessibilityUserState userState) {
        int nonInteractiveUiTimeout = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_non_interactive_ui_timeout_ms", 0, userState.mUserId);
        int interactiveUiTimeout = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_interactive_ui_timeout_ms", 0, userState.mUserId);
        if (nonInteractiveUiTimeout == userState.getUserNonInteractiveUiTimeoutLocked() && interactiveUiTimeout == userState.getUserInteractiveUiTimeoutLocked()) {
            return false;
        }
        userState.setUserNonInteractiveUiTimeoutLocked(nonInteractiveUiTimeout);
        userState.setUserInteractiveUiTimeoutLocked(interactiveUiTimeout);
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
        return true;
    }

    private void updateAccessibilityShortcutKeyTargetsLocked(final AccessibilityUserState userState) {
        ArraySet<String> shortcutTargetsLocked = userState.getShortcutTargetsLocked(1);
        int lastSize = shortcutTargetsLocked.size();
        if (lastSize == 0) {
            return;
        }
        shortcutTargetsLocked.removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda50
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AccessibilityManagerService.lambda$updateAccessibilityShortcutKeyTargetsLocked$14(AccessibilityUserState.this, (String) obj);
            }
        });
        if (lastSize == shortcutTargetsLocked.size()) {
            return;
        }
        persistColonDelimitedSetToSettingLocked("accessibility_shortcut_target_service", userState.mUserId, shortcutTargetsLocked, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda51
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$updateAccessibilityShortcutKeyTargetsLocked$15((String) obj);
            }
        });
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
    }

    public static /* synthetic */ boolean lambda$updateAccessibilityShortcutKeyTargetsLocked$14(AccessibilityUserState userState, String name) {
        return !userState.isShortcutTargetInstalledLocked(name);
    }

    public static /* synthetic */ String lambda$updateAccessibilityShortcutKeyTargetsLocked$15(String str) {
        return str;
    }

    private boolean canRequestAndRequestsTouchExplorationLocked(AccessibilityServiceConnection service, AccessibilityUserState userState) {
        if (service.canReceiveEventsLocked() && service.mRequestTouchExplorationMode) {
            if (service.getServiceInfo().getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 17) {
                if (userState.mTouchExplorationGrantedServices.contains(service.mComponentName)) {
                    return true;
                }
                AlertDialog alertDialog = this.mEnableTouchExplorationDialog;
                if (alertDialog == null || !alertDialog.isShowing()) {
                    this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda42
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((AccessibilityManagerService) obj).showEnableTouchExplorationDialog((AccessibilityServiceConnection) obj2);
                        }
                    }, this, service));
                }
            } else if ((service.getCapabilities() & 2) != 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void updateMagnificationLocked(AccessibilityUserState userState) {
        if (userState.mUserId != this.mCurrentUserId) {
            return;
        }
        if (this.mUiAutomationManager.suppressingAccessibilityServicesLocked() && this.mMagnificationController.isFullScreenMagnificationControllerInitialized()) {
            getMagnificationController().getFullScreenMagnificationController().unregisterAll();
            return;
        }
        ArrayList<Display> displays = getValidDisplayList();
        if (userState.isDisplayMagnificationEnabledLocked() || userState.isShortcutMagnificationEnabledLocked()) {
            for (int i = 0; i < displays.size(); i++) {
                Display display = displays.get(i);
                getMagnificationController().getFullScreenMagnificationController().register(display.getDisplayId());
            }
            return;
        }
        for (int i2 = 0; i2 < displays.size(); i2++) {
            Display display2 = displays.get(i2);
            int displayId = display2.getDisplayId();
            if (userHasListeningMagnificationServicesLocked(userState, displayId)) {
                getMagnificationController().getFullScreenMagnificationController().register(displayId);
            } else if (this.mMagnificationController.isFullScreenMagnificationControllerInitialized()) {
                getMagnificationController().getFullScreenMagnificationController().unregister(displayId);
            }
        }
    }

    private void updateWindowMagnificationConnectionIfNeeded(AccessibilityUserState userState) {
        if (!this.mMagnificationController.supportWindowMagnification()) {
            return;
        }
        boolean z = true;
        if (((!userState.isShortcutMagnificationEnabledLocked() && !userState.isDisplayMagnificationEnabledLocked()) || userState.getMagnificationCapabilitiesLocked() == 1) && !userHasMagnificationServicesLocked(userState)) {
            z = false;
        }
        boolean connect = z;
        getWindowMagnificationMgr().requestConnection(connect);
    }

    private boolean userHasMagnificationServicesLocked(AccessibilityUserState userState) {
        List<AccessibilityServiceConnection> services = userState.mBoundServices;
        int count = services.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceConnection service = services.get(i);
            if (this.mSecurityPolicy.canControlMagnification(service)) {
                return true;
            }
        }
        return false;
    }

    private boolean userHasListeningMagnificationServicesLocked(AccessibilityUserState userState, int displayId) {
        List<AccessibilityServiceConnection> services = userState.mBoundServices;
        int count = services.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceConnection service = services.get(i);
            if (this.mSecurityPolicy.canControlMagnification(service) && service.isMagnificationCallbackEnabled(displayId)) {
                return true;
            }
        }
        return false;
    }

    public void updateFingerprintGestureHandling(AccessibilityUserState userState) {
        List<AccessibilityServiceConnection> services;
        synchronized (this.mLock) {
            services = userState.mBoundServices;
            if (this.mFingerprintGestureDispatcher == null && this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                int numServices = services.size();
                int i = 0;
                while (true) {
                    if (i >= numServices) {
                        break;
                    }
                    if (services.get(i).isCapturingFingerprintGestures()) {
                        long identity = Binder.clearCallingIdentity();
                        IFingerprintService service = IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
                        Binder.restoreCallingIdentity(identity);
                        if (service != null) {
                            this.mFingerprintGestureDispatcher = new FingerprintGestureDispatcher(service, this.mContext.getResources(), this.mLock);
                            break;
                        }
                    }
                    i++;
                }
            }
        }
        FingerprintGestureDispatcher fingerprintGestureDispatcher = this.mFingerprintGestureDispatcher;
        if (fingerprintGestureDispatcher != null) {
            fingerprintGestureDispatcher.updateClientList(services);
        }
    }

    private void updateAccessibilityButtonTargetsLocked(final AccessibilityUserState userState) {
        for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
            AccessibilityServiceConnection service = userState.mBoundServices.get(i);
            if (service.mRequestAccessibilityButton) {
                service.notifyAccessibilityButtonAvailabilityChangedLocked(service.isAccessibilityButtonAvailableLocked(userState));
            }
        }
        ArraySet<String> shortcutTargetsLocked = userState.getShortcutTargetsLocked(0);
        int lastSize = shortcutTargetsLocked.size();
        if (lastSize == 0) {
            return;
        }
        shortcutTargetsLocked.removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AccessibilityManagerService.lambda$updateAccessibilityButtonTargetsLocked$16(AccessibilityUserState.this, (String) obj);
            }
        });
        if (lastSize == shortcutTargetsLocked.size()) {
            return;
        }
        persistColonDelimitedSetToSettingLocked("accessibility_button_targets", userState.mUserId, shortcutTargetsLocked, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda5
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$updateAccessibilityButtonTargetsLocked$17((String) obj);
            }
        });
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
    }

    public static /* synthetic */ boolean lambda$updateAccessibilityButtonTargetsLocked$16(AccessibilityUserState userState, String name) {
        return !userState.isShortcutTargetInstalledLocked(name);
    }

    public static /* synthetic */ String lambda$updateAccessibilityButtonTargetsLocked$17(String str) {
        return str;
    }

    public void migrateAccessibilityButtonSettingsIfNecessaryLocked(final AccessibilityUserState userState, final String packageName, int restoreFromSdkInt) {
        if (restoreFromSdkInt > 29) {
            return;
        }
        final ArraySet<String> shortcutTargetsLocked = userState.getShortcutTargetsLocked(0);
        int lastSize = shortcutTargetsLocked.size();
        shortcutTargetsLocked.removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda44
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AccessibilityManagerService.lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$18(packageName, userState, (String) obj);
            }
        });
        boolean changed = lastSize != shortcutTargetsLocked.size();
        int lastSize2 = shortcutTargetsLocked.size();
        final Set<String> shortcutKeyTargets = userState.getShortcutTargetsLocked(1);
        userState.mEnabledServices.forEach(new Consumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda45
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AccessibilityManagerService.lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$19(packageName, userState, shortcutTargetsLocked, shortcutKeyTargets, (ComponentName) obj);
            }
        });
        if (!((lastSize2 != shortcutTargetsLocked.size()) | changed)) {
            return;
        }
        persistColonDelimitedSetToSettingLocked("accessibility_button_targets", userState.mUserId, shortcutTargetsLocked, new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda46
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AccessibilityManagerService.lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$20((String) obj);
            }
        });
        scheduleNotifyClientsOfServicesStateChangeLocked(userState);
    }

    public static /* synthetic */ boolean lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$18(String packageName, AccessibilityUserState userState, String name) {
        ComponentName componentName;
        AccessibilityServiceInfo serviceInfo;
        if ((packageName != null && name != null && !name.contains(packageName)) || (componentName = ComponentName.unflattenFromString(name)) == null || (serviceInfo = userState.getInstalledServiceInfoLocked(componentName)) == null) {
            return false;
        }
        if (serviceInfo.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 29) {
            Slog.v(LOG_TAG, "Legacy service " + componentName + " should not in the button");
            return true;
        }
        boolean requestA11yButton = (serviceInfo.flags & 256) != 0;
        if (!requestA11yButton || userState.mEnabledServices.contains(componentName)) {
            return false;
        }
        Slog.v(LOG_TAG, "Service requesting a11y button and be assigned to the button" + componentName + " should be enabled state");
        return true;
    }

    public static /* synthetic */ void lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$19(String packageName, AccessibilityUserState userState, Set buttonTargets, Set shortcutKeyTargets, ComponentName componentName) {
        AccessibilityServiceInfo serviceInfo;
        if ((packageName != null && componentName != null && !packageName.equals(componentName.getPackageName())) || (serviceInfo = userState.getInstalledServiceInfoLocked(componentName)) == null) {
            return;
        }
        boolean requestA11yButton = (serviceInfo.flags & 256) != 0;
        if (serviceInfo.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 29 || !requestA11yButton) {
            return;
        }
        String serviceName = componentName.flattenToString();
        if (TextUtils.isEmpty(serviceName) || AccessibilityUserState.doesShortcutTargetsStringContain(buttonTargets, serviceName) || AccessibilityUserState.doesShortcutTargetsStringContain(shortcutKeyTargets, serviceName)) {
            return;
        }
        Slog.v(LOG_TAG, "A enabled service requesting a11y button " + componentName + " should be assign to the button or shortcut.");
        buttonTargets.add(serviceName);
    }

    public static /* synthetic */ String lambda$migrateAccessibilityButtonSettingsIfNecessaryLocked$20(String str) {
        return str;
    }

    private void removeShortcutTargetForUnboundServiceLocked(AccessibilityUserState userState, AccessibilityServiceConnection service) {
        if (!service.mRequestAccessibilityButton || service.getServiceInfo().getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 29) {
            return;
        }
        ComponentName serviceName = service.getComponentName();
        if (userState.removeShortcutTargetLocked(1, serviceName)) {
            persistColonDelimitedSetToSettingLocked("accessibility_shortcut_target_service", userState.mUserId, userState.getShortcutTargetsLocked(1), new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda27
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return AccessibilityManagerService.lambda$removeShortcutTargetForUnboundServiceLocked$21((String) obj);
                }
            });
        }
        if (userState.removeShortcutTargetLocked(0, serviceName)) {
            persistColonDelimitedSetToSettingLocked("accessibility_button_targets", userState.mUserId, userState.getShortcutTargetsLocked(0), new Function() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda28
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return AccessibilityManagerService.lambda$removeShortcutTargetForUnboundServiceLocked$22((String) obj);
                }
            });
        }
    }

    public static /* synthetic */ String lambda$removeShortcutTargetForUnboundServiceLocked$21(String str) {
        return str;
    }

    public static /* synthetic */ String lambda$removeShortcutTargetForUnboundServiceLocked$22(String str) {
        return str;
    }

    private void updateRecommendedUiTimeoutLocked(AccessibilityUserState userState) {
        int newNonInteractiveUiTimeout = userState.getUserNonInteractiveUiTimeoutLocked();
        int newInteractiveUiTimeout = userState.getUserInteractiveUiTimeoutLocked();
        if (newNonInteractiveUiTimeout == 0 || newInteractiveUiTimeout == 0) {
            int serviceNonInteractiveUiTimeout = 0;
            int serviceInteractiveUiTimeout = 0;
            List<AccessibilityServiceConnection> services = userState.mBoundServices;
            for (int i = 0; i < services.size(); i++) {
                int timeout = services.get(i).getServiceInfo().getInteractiveUiTimeoutMillis();
                if (serviceInteractiveUiTimeout < timeout) {
                    serviceInteractiveUiTimeout = timeout;
                }
                int timeout2 = services.get(i).getServiceInfo().getNonInteractiveUiTimeoutMillis();
                if (serviceNonInteractiveUiTimeout < timeout2) {
                    serviceNonInteractiveUiTimeout = timeout2;
                }
            }
            if (newNonInteractiveUiTimeout == 0) {
                newNonInteractiveUiTimeout = serviceNonInteractiveUiTimeout;
            }
            if (newInteractiveUiTimeout == 0) {
                newInteractiveUiTimeout = serviceInteractiveUiTimeout;
            }
        }
        userState.setNonInteractiveUiTimeoutLocked(newNonInteractiveUiTimeout);
        userState.setInteractiveUiTimeoutLocked(newInteractiveUiTimeout);
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public KeyEventDispatcher getKeyEventDispatcher() {
        if (this.mKeyEventDispatcher == null) {
            this.mKeyEventDispatcher = new KeyEventDispatcher(this.mMainHandler, 8, this.mLock, this.mPowerManager);
        }
        return this.mKeyEventDispatcher;
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public PendingIntent getPendingIntentActivity(Context context, int requestCode, Intent intent, int flags) {
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    public void performAccessibilityShortcut(String targetName) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.performAccessibilityShortcut", 4L, "targetName=" + targetName);
        }
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000 && this.mContext.checkCallingPermission("android.permission.MANAGE_ACCESSIBILITY") != 0) {
            throw new SecurityException("performAccessibilityShortcut requires the MANAGE_ACCESSIBILITY permission");
        }
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new AccessibilityManagerService$$ExternalSyntheticLambda1(), this, 0, 1, targetName));
    }

    public void performAccessibilityShortcutInternal(int displayId, int shortcutType, String targetName) {
        List<String> shortcutTargets = getAccessibilityShortcutTargetsInternal(shortcutType);
        if (shortcutTargets.isEmpty()) {
            Slog.d(LOG_TAG, "No target to perform shortcut, shortcutType=" + shortcutType);
            return;
        }
        if (targetName != null && !AccessibilityUserState.doesShortcutTargetsStringContain(shortcutTargets, targetName)) {
            Slog.v(LOG_TAG, "Perform shortcut failed, invalid target name:" + targetName);
            targetName = null;
        }
        if (targetName == null) {
            if (shortcutTargets.size() > 1) {
                showAccessibilityTargetsSelection(displayId, shortcutType);
                return;
            }
            targetName = shortcutTargets.get(0);
        }
        if (targetName.equals("com.android.server.accessibility.MagnificationController")) {
            boolean enabled = !getMagnificationController().getFullScreenMagnificationController().isMagnifying(displayId);
            AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME, shortcutType, enabled);
            sendAccessibilityButtonToInputFilter(displayId);
            return;
        }
        ComponentName targetComponentName = ComponentName.unflattenFromString(targetName);
        if (targetComponentName == null) {
            Slog.d(LOG_TAG, "Perform shortcut failed, invalid target name:" + targetName);
        } else if (performAccessibilityFrameworkFeature(targetComponentName, shortcutType)) {
        } else {
            if (performAccessibilityShortcutTargetActivity(displayId, targetComponentName)) {
                AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, targetComponentName, shortcutType);
            } else {
                performAccessibilityShortcutTargetService(displayId, shortcutType, targetComponentName);
            }
        }
    }

    private boolean performAccessibilityFrameworkFeature(ComponentName assignedTarget, int shortcutType) {
        Map<ComponentName, AccessibilityShortcutController.ToggleableFrameworkFeatureInfo> frameworkFeatureMap = AccessibilityShortcutController.getFrameworkShortcutFeaturesMap();
        if (frameworkFeatureMap.containsKey(assignedTarget)) {
            AccessibilityShortcutController.ToggleableFrameworkFeatureInfo featureInfo = frameworkFeatureMap.get(assignedTarget);
            SettingsStringUtil.SettingStringHelper setting = new SettingsStringUtil.SettingStringHelper(this.mContext.getContentResolver(), featureInfo.getSettingKey(), this.mCurrentUserId);
            if (!TextUtils.equals(featureInfo.getSettingOnValue(), setting.read())) {
                AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, assignedTarget, shortcutType, true);
                setting.write(featureInfo.getSettingOnValue());
            } else {
                AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, assignedTarget, shortcutType, false);
                setting.write(featureInfo.getSettingOffValue());
            }
            return true;
        }
        return false;
    }

    private boolean performAccessibilityShortcutTargetActivity(int displayId, ComponentName assignedTarget) {
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = 0; i < userState.mInstalledShortcuts.size(); i++) {
                AccessibilityShortcutInfo shortcutInfo = userState.mInstalledShortcuts.get(i);
                if (shortcutInfo.getComponentName().equals(assignedTarget)) {
                    launchShortcutTargetActivity(displayId, assignedTarget);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean performAccessibilityShortcutTargetService(int displayId, int shortcutType, ComponentName assignedTarget) {
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            AccessibilityServiceInfo installedServiceInfo = userState.getInstalledServiceInfoLocked(assignedTarget);
            if (installedServiceInfo == null) {
                Slog.d(LOG_TAG, "Perform shortcut failed, invalid component name:" + assignedTarget);
                return false;
            }
            AccessibilityServiceConnection serviceConnection = userState.getServiceConnectionLocked(assignedTarget);
            int targetSdk = installedServiceInfo.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion;
            boolean requestA11yButton = (installedServiceInfo.flags & 256) != 0;
            if ((targetSdk <= 29 && shortcutType == 1) || (targetSdk > 29 && !requestA11yButton)) {
                if (serviceConnection != null) {
                    AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, assignedTarget, shortcutType, false);
                    disableAccessibilityServiceLocked(assignedTarget, this.mCurrentUserId);
                } else {
                    AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, assignedTarget, shortcutType, true);
                    enableAccessibilityServiceLocked(assignedTarget, this.mCurrentUserId);
                }
                return true;
            } else if (shortcutType == 1 && targetSdk > 29 && requestA11yButton && !userState.getEnabledServicesLocked().contains(assignedTarget)) {
                enableAccessibilityServiceLocked(assignedTarget, this.mCurrentUserId);
                return true;
            } else {
                if (serviceConnection != null && userState.mBoundServices.contains(serviceConnection) && serviceConnection.mRequestAccessibilityButton) {
                    AccessibilityStatsLogUtils.logAccessibilityShortcutActivated(this.mContext, assignedTarget, shortcutType, true);
                    serviceConnection.notifyAccessibilityButtonClickedLocked(displayId);
                    return true;
                }
                Slog.d(LOG_TAG, "Perform shortcut failed, service is not ready:" + assignedTarget);
                return false;
            }
        }
    }

    public List<String> getAccessibilityShortcutTargets(int shortcutType) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getAccessibilityShortcutTargets", 4L, "shortcutType=" + shortcutType);
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_ACCESSIBILITY") != 0) {
            throw new SecurityException("getAccessibilityShortcutService requires the MANAGE_ACCESSIBILITY permission");
        }
        return getAccessibilityShortcutTargetsInternal(shortcutType);
    }

    private List<String> getAccessibilityShortcutTargetsInternal(int shortcutType) {
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            ArrayList<String> shortcutTargets = new ArrayList<>(userState.getShortcutTargetsLocked(shortcutType));
            if (shortcutType != 0) {
                return shortcutTargets;
            }
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (service.mRequestAccessibilityButton && service.getServiceInfo().getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 29) {
                    String serviceName = service.getComponentName().flattenToString();
                    if (!TextUtils.isEmpty(serviceName)) {
                        shortcutTargets.add(serviceName);
                    }
                }
            }
            return shortcutTargets;
        }
    }

    private void enableAccessibilityServiceLocked(ComponentName componentName, int userId) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("enabled_accessibility_services", userId, this.mTempComponentNameSet);
        this.mTempComponentNameSet.add(componentName);
        persistComponentNamesToSettingLocked("enabled_accessibility_services", this.mTempComponentNameSet, userId);
        AccessibilityUserState userState = getUserStateLocked(userId);
        if (userState.mEnabledServices.add(componentName)) {
            onUserStateChangedLocked(userState);
        }
    }

    private void disableAccessibilityServiceLocked(ComponentName componentName, int userId) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("enabled_accessibility_services", userId, this.mTempComponentNameSet);
        this.mTempComponentNameSet.remove(componentName);
        persistComponentNamesToSettingLocked("enabled_accessibility_services", this.mTempComponentNameSet, userId);
        AccessibilityUserState userState = getUserStateLocked(userId);
        if (userState.mEnabledServices.remove(componentName)) {
            onUserStateChangedLocked(userState);
        }
    }

    @Override // com.android.server.accessibility.AccessibilityWindowManager.AccessibilityEventSender
    public void sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent event) {
        if (event.getWindowChanges() == 1) {
            sendPendingWindowStateChangedEventsForAvailableWindowLocked(event.getWindowId());
        }
        sendAccessibilityEventLocked(event, this.mCurrentUserId);
    }

    private void sendAccessibilityEventLocked(AccessibilityEvent event, int userId) {
        event.setEventTime(SystemClock.uptimeMillis());
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda20
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).sendAccessibilityEvent((AccessibilityEvent) obj2, ((Integer) obj3).intValue());
            }
        }, this, event, Integer.valueOf(userId)));
    }

    public boolean sendFingerprintGesture(int gestureKeyCode) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(131076L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.sendFingerprintGesture", 131076L, "gestureKeyCode=" + gestureKeyCode);
        }
        synchronized (this.mLock) {
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                throw new SecurityException("Only SYSTEM can call sendFingerprintGesture");
            }
        }
        FingerprintGestureDispatcher fingerprintGestureDispatcher = this.mFingerprintGestureDispatcher;
        if (fingerprintGestureDispatcher == null) {
            return false;
        }
        return fingerprintGestureDispatcher.onFingerprintGesture(gestureKeyCode);
    }

    public int getAccessibilityWindowId(IBinder windowToken) {
        int findWindowIdLocked;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getAccessibilityWindowId", 4L, "windowToken=" + windowToken);
        }
        synchronized (this.mLock) {
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                throw new SecurityException("Only SYSTEM can call getAccessibilityWindowId");
            }
            findWindowIdLocked = this.mA11yWindowManager.findWindowIdLocked(this.mCurrentUserId, windowToken);
        }
        return findWindowIdLocked;
    }

    public long getRecommendedTimeoutMillis() {
        long recommendedTimeoutMillisLocked;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getRecommendedTimeoutMillis", 4L);
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            recommendedTimeoutMillisLocked = getRecommendedTimeoutMillisLocked(userState);
        }
        return recommendedTimeoutMillisLocked;
    }

    private long getRecommendedTimeoutMillisLocked(AccessibilityUserState userState) {
        return IntPair.of(userState.getInteractiveUiTimeoutLocked(), userState.getNonInteractiveUiTimeoutLocked());
    }

    public void setWindowMagnificationConnection(IWindowMagnificationConnection connection) throws RemoteException {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(132L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.setWindowMagnificationConnection", 132L, "connection=" + connection);
        }
        this.mSecurityPolicy.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE");
        getWindowMagnificationMgr().setConnection(connection);
    }

    public WindowMagnificationManager getWindowMagnificationMgr() {
        WindowMagnificationManager windowMagnificationMgr;
        synchronized (this.mLock) {
            windowMagnificationMgr = this.mMagnificationController.getWindowMagnificationMgr();
        }
        return windowMagnificationMgr;
    }

    public MagnificationController getMagnificationController() {
        return this.mMagnificationController;
    }

    public void associateEmbeddedHierarchy(IBinder host, IBinder embedded) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.associateEmbeddedHierarchy", 4L, "host=" + host + ";embedded=" + embedded);
        }
        synchronized (this.mLock) {
            this.mA11yWindowManager.associateEmbeddedHierarchyLocked(host, embedded);
        }
    }

    public void disassociateEmbeddedHierarchy(IBinder token) {
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.disassociateEmbeddedHierarchy", 4L, "token=" + token);
        }
        synchronized (this.mLock) {
            this.mA11yWindowManager.disassociateEmbeddedHierarchyLocked(token);
        }
    }

    public int getFocusStrokeWidth() {
        int focusStrokeWidthLocked;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getFocusStrokeWidth", 4L);
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            focusStrokeWidthLocked = userState.getFocusStrokeWidthLocked();
        }
        return focusStrokeWidthLocked;
    }

    public int getFocusColor() {
        int focusColorLocked;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.getFocusColor", 4L);
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            focusColorLocked = userState.getFocusColorLocked();
        }
        return focusColorLocked;
    }

    public boolean isAudioDescriptionByDefaultEnabled() {
        boolean isAudioDescriptionByDefaultEnabledLocked;
        if (this.mTraceManager.isA11yTracingEnabledForTypes(4L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.isAudioDescriptionByDefaultEnabled", 4L);
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            isAudioDescriptionByDefaultEnabledLocked = userState.isAudioDescriptionByDefaultEnabledLocked();
        }
        return isAudioDescriptionByDefaultEnabledLocked;
    }

    public void setSystemAudioCaptioningEnabled(boolean isEnabled, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_SYSTEM_AUDIO_CAPTION", "setSystemAudioCaptioningEnabled");
        this.mCaptioningManagerImpl.setSystemAudioCaptioningEnabled(isEnabled, userId);
    }

    public boolean isSystemAudioCaptioningUiEnabled(int userId) {
        return this.mCaptioningManagerImpl.isSystemAudioCaptioningUiEnabled(userId);
    }

    public void setSystemAudioCaptioningUiEnabled(boolean isEnabled, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_SYSTEM_AUDIO_CAPTION", "setSystemAudioCaptioningUiEnabled");
        this.mCaptioningManagerImpl.setSystemAudioCaptioningUiEnabled(isEnabled, userId);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        AccessibilityInputFilter accessibilityInputFilter;
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            synchronized (this.mLock) {
                pw.println("ACCESSIBILITY MANAGER (dumpsys accessibility)");
                pw.println();
                pw.append("currentUserId=").append((CharSequence) String.valueOf(this.mCurrentUserId));
                pw.println();
                pw.append("hasWindowMagnificationConnection=").append((CharSequence) String.valueOf(getWindowMagnificationMgr().isConnected()));
                pw.println();
                this.mMagnificationProcessor.dump(pw, getValidDisplayList());
                int userCount = this.mUserStates.size();
                for (int i = 0; i < userCount; i++) {
                    this.mUserStates.valueAt(i).dump(fd, pw, args);
                }
                if (this.mUiAutomationManager.isUiAutomationRunningLocked()) {
                    this.mUiAutomationManager.dumpUiAutomationService(fd, pw, args);
                    pw.println();
                }
                this.mA11yWindowManager.dump(fd, pw, args);
                if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                    accessibilityInputFilter.dump(fd, pw, args);
                }
                pw.println("Global client list info:{");
                this.mGlobalClients.dump(pw, "    Client list ");
                pw.println("    Registered clients:{");
                for (int i2 = 0; i2 < this.mGlobalClients.getRegisteredCallbackCount(); i2++) {
                    Client client = (Client) this.mGlobalClients.getRegisteredCallbackCookie(i2);
                    pw.append((CharSequence) Arrays.toString(client.mPackageNames));
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public final class MainHandler extends Handler {
        public static final int MSG_SEND_KEY_EVENT_TO_INPUT_FILTER = 8;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MainHandler(Looper looper) {
            super(looper);
            AccessibilityManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 8) {
                KeyEvent event = (KeyEvent) msg.obj;
                int policyFlags = msg.arg1;
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (AccessibilityManagerService.this.mHasInputFilter && AccessibilityManagerService.this.mInputFilter != null) {
                        AccessibilityManagerService.this.mInputFilter.sendInputEvent(event, policyFlags);
                    }
                }
                event.recycle();
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public MagnificationProcessor getMagnificationProcessor() {
        return this.mMagnificationProcessor;
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void onClientChangeLocked(boolean serviceInfoChanged) {
        AccessibilityUserState userState = getUserStateLocked(this.mCurrentUserId);
        onUserStateChangedLocked(userState);
        if (serviceInfoChanged) {
            scheduleNotifyClientsOfServicesStateChangeLocked(userState);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.accessibility.AccessibilityManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new AccessibilityShellCommand(this, this.mSystemActionPerformer).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    public final class InteractionBridge {
        private final ComponentName COMPONENT_NAME;
        private final AccessibilityInteractionClient mClient;
        private final int mConnectionId;
        private final Display mDefaultDisplay;

        public InteractionBridge() {
            AccessibilityUserState userState;
            AccessibilityManagerService.this = r21;
            ComponentName componentName = new ComponentName("com.android.server.accessibility", "InteractionBridge");
            this.COMPONENT_NAME = componentName;
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.setCapabilities(1);
            info.flags |= 64;
            info.flags |= 2;
            synchronized (r21.mLock) {
                try {
                    userState = r21.getCurrentUserStateLocked();
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
            Context context = r21.mContext;
            int i = AccessibilityManagerService.sIdCounter;
            AccessibilityManagerService.sIdCounter = i + 1;
            AccessibilityServiceConnection service = new AccessibilityServiceConnection(userState, context, componentName, info, i, r21.mMainHandler, r21.mLock, r21.mSecurityPolicy, r21, r21.getTraceManager(), r21.mWindowManagerService, r21.getSystemActionPerformer(), r21.mA11yWindowManager, r21.mActivityTaskManagerService) { // from class: com.android.server.accessibility.AccessibilityManagerService.InteractionBridge.1
                {
                    InteractionBridge.this = this;
                }

                @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
                public boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info2) {
                    return true;
                }
            };
            int i2 = service.mId;
            this.mConnectionId = i2;
            this.mClient = AccessibilityInteractionClient.getInstance(r21.mContext);
            AccessibilityInteractionClient.addConnection(i2, service, false);
            DisplayManager displayManager = (DisplayManager) r21.mContext.getSystemService("display");
            this.mDefaultDisplay = displayManager.getDisplay(0);
        }

        boolean getAccessibilityFocusClickPointInScreen(Point outPoint) {
            return AccessibilityManagerService.this.getInteractionBridge().getAccessibilityFocusClickPointInScreenNotLocked(outPoint);
        }

        public boolean performActionOnAccessibilityFocusedItemNotLocked(AccessibilityNodeInfo.AccessibilityAction action) {
            AccessibilityNodeInfo focus = getAccessibilityFocusNotLocked();
            if (focus == null || !focus.getActionList().contains(action)) {
                return false;
            }
            return focus.performAction(action.getId());
        }

        public boolean getAccessibilityFocusClickPointInScreenNotLocked(Point outPoint) {
            AccessibilityNodeInfo focus = getAccessibilityFocusNotLocked();
            if (focus == null) {
                return false;
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                Rect boundsInScreenBeforeMagnification = AccessibilityManagerService.this.mTempRect;
                focus.getBoundsInScreen(boundsInScreenBeforeMagnification);
                Point nodeCenter = new Point(boundsInScreenBeforeMagnification.centerX(), boundsInScreenBeforeMagnification.centerY());
                Pair<float[], MagnificationSpec> pair = AccessibilityManagerService.this.getWindowTransformationMatrixAndMagnificationSpec(focus.getWindowId());
                MagnificationSpec spec = null;
                if (pair != null && pair.second != null) {
                    spec = new MagnificationSpec();
                    spec.setTo((MagnificationSpec) pair.second);
                }
                if (spec != null && !spec.isNop()) {
                    boundsInScreenBeforeMagnification.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                    boundsInScreenBeforeMagnification.scale(1.0f / spec.scale);
                }
                Rect windowBounds = AccessibilityManagerService.this.mTempRect1;
                AccessibilityManagerService.this.getWindowBounds(focus.getWindowId(), windowBounds);
                if (!boundsInScreenBeforeMagnification.intersect(windowBounds)) {
                    return false;
                }
                Point screenSize = AccessibilityManagerService.this.mTempPoint;
                this.mDefaultDisplay.getRealSize(screenSize);
                if (!boundsInScreenBeforeMagnification.intersect(0, 0, screenSize.x, screenSize.y)) {
                    return false;
                }
                outPoint.set(nodeCenter.x, nodeCenter.y);
                return true;
            }
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked() {
            synchronized (AccessibilityManagerService.this.mLock) {
                int focusedWindowId = AccessibilityManagerService.this.mA11yWindowManager.getFocusedWindowId(2);
                if (focusedWindowId == -1) {
                    return null;
                }
                return getAccessibilityFocusNotLocked(focusedWindowId);
            }
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked(int windowId) {
            return this.mClient.findFocus(this.mConnectionId, windowId, AccessibilityNodeInfo.ROOT_NODE_ID, 2);
        }
    }

    public ArrayList<Display> getValidDisplayList() {
        return this.mA11yDisplayListener.getValidDisplayList();
    }

    /* loaded from: classes.dex */
    public class AccessibilityDisplayListener implements DisplayManager.DisplayListener {
        private final DisplayManager mDisplayManager;
        private final ArrayList<Display> mDisplaysList = new ArrayList<>();
        private int mSystemUiUid;

        AccessibilityDisplayListener(Context context, MainHandler handler) {
            AccessibilityManagerService.this = this$0;
            this.mSystemUiUid = 0;
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            this.mDisplayManager = displayManager;
            displayManager.registerDisplayListener(this, handler);
            initializeDisplayList();
            PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            if (pm != null) {
                this.mSystemUiUid = pm.getPackageUid(pm.getSystemUiServiceComponent().getPackageName(), 1048576L, this$0.mCurrentUserId);
            }
        }

        public ArrayList<Display> getValidDisplayList() {
            ArrayList<Display> arrayList;
            synchronized (AccessibilityManagerService.this.mLock) {
                arrayList = this.mDisplaysList;
            }
            return arrayList;
        }

        private void initializeDisplayList() {
            Display[] displays = this.mDisplayManager.getDisplays();
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mDisplaysList.clear();
                for (Display display : displays) {
                    if (isValidDisplay(display)) {
                        this.mDisplaysList.add(display);
                    }
                }
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            Display display = this.mDisplayManager.getDisplay(displayId);
            if (!isValidDisplay(display)) {
                return;
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mDisplaysList.add(display);
                if (AccessibilityManagerService.this.mInputFilter != null) {
                    AccessibilityManagerService.this.mInputFilter.onDisplayAdded(display);
                }
                AccessibilityUserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (displayId != 0) {
                    List<AccessibilityServiceConnection> services = userState.mBoundServices;
                    for (int i = 0; i < services.size(); i++) {
                        AccessibilityServiceConnection boundClient = services.get(i);
                        boundClient.onDisplayAdded(displayId);
                    }
                }
                AccessibilityManagerService.this.updateMagnificationLocked(userState);
                AccessibilityManagerService.this.updateWindowsForAccessibilityCallbackLocked(userState);
                AccessibilityManagerService.this.notifyClearAccessibilityCacheLocked();
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (removeDisplayFromList(displayId)) {
                    if (AccessibilityManagerService.this.mInputFilter != null) {
                        AccessibilityManagerService.this.mInputFilter.onDisplayRemoved(displayId);
                    }
                    AccessibilityUserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                    if (displayId != 0) {
                        List<AccessibilityServiceConnection> services = userState.mBoundServices;
                        for (int i = 0; i < services.size(); i++) {
                            AccessibilityServiceConnection boundClient = services.get(i);
                            boundClient.onDisplayRemoved(displayId);
                        }
                    }
                    AccessibilityManagerService.this.mMagnificationController.onDisplayRemoved(displayId);
                    AccessibilityManagerService.this.mA11yWindowManager.stopTrackingWindows(displayId);
                }
            }
        }

        private boolean removeDisplayFromList(int displayId) {
            for (int i = 0; i < this.mDisplaysList.size(); i++) {
                if (this.mDisplaysList.get(i).getDisplayId() == displayId) {
                    this.mDisplaysList.remove(i);
                    return true;
                }
            }
            return false;
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
        }

        private boolean isValidDisplay(Display display) {
            if (display == null || display.getType() == 4) {
                return false;
            }
            if (display.getType() == 5 && (display.getFlags() & 4) != 0 && display.getOwnerUid() != this.mSystemUiUid) {
                return false;
            }
            return true;
        }
    }

    /* loaded from: classes.dex */
    public class Client {
        final IAccessibilityManagerClient mCallback;
        int mLastSentRelevantEventTypes;
        final String[] mPackageNames;

        private Client(IAccessibilityManagerClient callback, int clientUid, AccessibilityUserState userState) {
            AccessibilityManagerService.this = this$0;
            this.mCallback = callback;
            this.mPackageNames = this$0.mPackageManager.getPackagesForUid(clientUid);
            synchronized (this$0.mLock) {
                this.mLastSentRelevantEventTypes = this$0.computeRelevantEventTypesLocked(userState, this);
            }
        }
    }

    /* loaded from: classes.dex */
    public final class AccessibilityContentObserver extends ContentObserver {
        private final Uri mAccessibilityButtonComponentIdUri;
        private final Uri mAccessibilityButtonTargetsUri;
        private final Uri mAccessibilityShortcutServiceIdUri;
        private final Uri mAccessibilitySoftKeyboardModeUri;
        private final Uri mAudioDescriptionByDefaultUri;
        private final Uri mAutoclickEnabledUri;
        private final Uri mDisplayMagnificationEnabledUri;
        private final Uri mEnabledAccessibilityServicesUri;
        private final Uri mHighTextContrastUri;
        private final Uri mMagnificationCapabilityUri;
        private final Uri mMagnificationFollowTypingUri;
        private final Uri mMagnificationModeUri;
        private final Uri mShowImeWithHardKeyboardUri;
        private final Uri mTouchExplorationEnabledUri;
        private final Uri mTouchExplorationGrantedAccessibilityServicesUri;
        private final Uri mUserInteractiveUiTimeoutUri;
        private final Uri mUserNonInteractiveUiTimeoutUri;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public AccessibilityContentObserver(Handler handler) {
            super(handler);
            AccessibilityManagerService.this = r1;
            this.mTouchExplorationEnabledUri = Settings.Secure.getUriFor("touch_exploration_enabled");
            this.mDisplayMagnificationEnabledUri = Settings.Secure.getUriFor("accessibility_display_magnification_enabled");
            this.mAutoclickEnabledUri = Settings.Secure.getUriFor("accessibility_autoclick_enabled");
            this.mEnabledAccessibilityServicesUri = Settings.Secure.getUriFor("enabled_accessibility_services");
            this.mTouchExplorationGrantedAccessibilityServicesUri = Settings.Secure.getUriFor("touch_exploration_granted_accessibility_services");
            this.mHighTextContrastUri = Settings.Secure.getUriFor("high_text_contrast_enabled");
            this.mAudioDescriptionByDefaultUri = Settings.Secure.getUriFor("enabled_accessibility_audio_description_by_default");
            this.mAccessibilitySoftKeyboardModeUri = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
            this.mShowImeWithHardKeyboardUri = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            this.mAccessibilityShortcutServiceIdUri = Settings.Secure.getUriFor("accessibility_shortcut_target_service");
            this.mAccessibilityButtonComponentIdUri = Settings.Secure.getUriFor("accessibility_button_target_component");
            this.mAccessibilityButtonTargetsUri = Settings.Secure.getUriFor("accessibility_button_targets");
            this.mUserNonInteractiveUiTimeoutUri = Settings.Secure.getUriFor("accessibility_non_interactive_ui_timeout_ms");
            this.mUserInteractiveUiTimeoutUri = Settings.Secure.getUriFor("accessibility_interactive_ui_timeout_ms");
            this.mMagnificationModeUri = Settings.Secure.getUriFor("accessibility_magnification_mode");
            this.mMagnificationCapabilityUri = Settings.Secure.getUriFor("accessibility_magnification_capability");
            this.mMagnificationFollowTypingUri = Settings.Secure.getUriFor("accessibility_magnification_follow_typing_enabled");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mTouchExplorationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayMagnificationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAutoclickEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mEnabledAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mTouchExplorationGrantedAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mHighTextContrastUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAudioDescriptionByDefaultUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilitySoftKeyboardModeUri, false, this, -1);
            contentResolver.registerContentObserver(this.mShowImeWithHardKeyboardUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilityShortcutServiceIdUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilityButtonComponentIdUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilityButtonTargetsUri, false, this, -1);
            contentResolver.registerContentObserver(this.mUserNonInteractiveUiTimeoutUri, false, this, -1);
            contentResolver.registerContentObserver(this.mUserInteractiveUiTimeoutUri, false, this, -1);
            contentResolver.registerContentObserver(this.mMagnificationModeUri, false, this, -1);
            contentResolver.registerContentObserver(this.mMagnificationCapabilityUri, false, this, -1);
            contentResolver.registerContentObserver(this.mMagnificationFollowTypingUri, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (AccessibilityManagerService.this.mLock) {
                AccessibilityUserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (this.mTouchExplorationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readTouchExplorationEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mDisplayMagnificationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readMagnificationEnabledSettingsLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mAutoclickEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readAutoclickEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mEnabledAccessibilityServicesUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readEnabledAccessibilityServicesLocked(userState)) {
                        AccessibilityManagerService.this.mSecurityPolicy.onEnabledServicesChangedLocked(userState.mUserId, userState.mEnabledServices);
                        userState.updateCrashedServicesIfNeededLocked();
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mTouchExplorationGrantedAccessibilityServicesUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readTouchExplorationGrantedAccessibilityServicesLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mHighTextContrastUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readHighTextContrastEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mAudioDescriptionByDefaultUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readAudioDescriptionEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else {
                    if (!this.mAccessibilitySoftKeyboardModeUri.equals(uri) && !this.mShowImeWithHardKeyboardUri.equals(uri)) {
                        if (this.mAccessibilityShortcutServiceIdUri.equals(uri)) {
                            if (AccessibilityManagerService.this.readAccessibilityShortcutKeySettingLocked(userState)) {
                                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                            }
                        } else if (this.mAccessibilityButtonComponentIdUri.equals(uri)) {
                            if (AccessibilityManagerService.this.readAccessibilityButtonTargetComponentLocked(userState)) {
                                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                            }
                        } else if (this.mAccessibilityButtonTargetsUri.equals(uri)) {
                            if (AccessibilityManagerService.this.readAccessibilityButtonTargetsLocked(userState)) {
                                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                            }
                        } else {
                            if (!this.mUserNonInteractiveUiTimeoutUri.equals(uri) && !this.mUserInteractiveUiTimeoutUri.equals(uri)) {
                                if (this.mMagnificationModeUri.equals(uri)) {
                                    if (AccessibilityManagerService.this.readMagnificationModeForDefaultDisplayLocked(userState)) {
                                        AccessibilityManagerService.this.updateMagnificationModeChangeSettingsLocked(userState, 0);
                                    }
                                } else if (this.mMagnificationCapabilityUri.equals(uri)) {
                                    if (AccessibilityManagerService.this.readMagnificationCapabilitiesLocked(userState)) {
                                        AccessibilityManagerService.this.updateMagnificationCapabilitiesSettingsChangeLocked(userState);
                                    }
                                } else if (this.mMagnificationFollowTypingUri.equals(uri)) {
                                    AccessibilityManagerService.this.readMagnificationFollowTypingLocked(userState);
                                }
                            }
                            AccessibilityManagerService.this.readUserRecommendedUiTimeoutSettingsLocked(userState);
                        }
                    }
                    userState.reconcileSoftKeyboardModeWithSettingsLocked();
                }
            }
        }
    }

    public void updateMagnificationCapabilitiesSettingsChangeLocked(AccessibilityUserState userState) {
        ArrayList<Display> displays = getValidDisplayList();
        for (int i = 0; i < displays.size(); i++) {
            int displayId = displays.get(i).getDisplayId();
            if (fallBackMagnificationModeSettingsLocked(userState, displayId)) {
                updateMagnificationModeChangeSettingsLocked(userState, displayId);
            }
        }
        updateWindowMagnificationConnectionIfNeeded(userState);
        if ((!userState.isDisplayMagnificationEnabledLocked() && !userState.isShortcutMagnificationEnabledLocked()) || userState.getMagnificationCapabilitiesLocked() != 3) {
            for (int i2 = 0; i2 < displays.size(); i2++) {
                getWindowMagnificationMgr().removeMagnificationButton(displays.get(i2).getDisplayId());
            }
        }
    }

    private boolean fallBackMagnificationModeSettingsLocked(AccessibilityUserState userState, int displayId) {
        if (userState.isValidMagnificationModeLocked(displayId)) {
            return false;
        }
        Slog.w(LOG_TAG, "displayId " + displayId + ", invalid magnification mode:" + userState.getMagnificationModeLocked(displayId));
        int capabilities = userState.getMagnificationCapabilitiesLocked();
        userState.setMagnificationModeLocked(displayId, capabilities);
        if (displayId == 0) {
            persistMagnificationModeSettingsLocked(capabilities);
            return true;
        }
        return true;
    }

    private void persistMagnificationModeSettingsLocked(final int mode) {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda33
            @Override // java.lang.Runnable
            public final void run() {
                AccessibilityManagerService.this.m652xb48c0392(mode);
            }
        });
    }

    /* renamed from: lambda$persistMagnificationModeSettingsLocked$23$com-android-server-accessibility-AccessibilityManagerService */
    public /* synthetic */ void m652xb48c0392(int mode) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_magnification_mode", mode, this.mCurrentUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getMagnificationMode(int displayId) {
        int magnificationModeLocked;
        synchronized (this.mLock) {
            magnificationModeLocked = getCurrentUserStateLocked().getMagnificationModeLocked(displayId);
        }
        return magnificationModeLocked;
    }

    public boolean readMagnificationModeForDefaultDisplayLocked(AccessibilityUserState userState) {
        int magnificationMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_magnification_mode", 1, userState.mUserId);
        if (magnificationMode == userState.getMagnificationModeLocked(0)) {
            return false;
        }
        userState.setMagnificationModeLocked(0, magnificationMode);
        return true;
    }

    public boolean readMagnificationCapabilitiesLocked(AccessibilityUserState userState) {
        int capabilities = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_magnification_capability", 1, userState.mUserId);
        if (capabilities != userState.getMagnificationCapabilitiesLocked()) {
            userState.setMagnificationCapabilitiesLocked(capabilities);
            this.mMagnificationController.setMagnificationCapabilities(capabilities);
            return true;
        }
        return false;
    }

    boolean readMagnificationFollowTypingLocked(AccessibilityUserState userState) {
        boolean followTypeEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_magnification_follow_typing_enabled", 1, userState.mUserId) == 1;
        if (followTypeEnabled != userState.isMagnificationFollowTypingEnabled()) {
            userState.setMagnificationFollowTypingEnabled(followTypeEnabled);
            this.mMagnificationController.setMagnificationFollowTypingEnabled(followTypeEnabled);
            return true;
        }
        return false;
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void setGestureDetectionPassthroughRegion(int displayId, Region region) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda3
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).setGestureDetectionPassthroughRegionInternal(((Integer) obj2).intValue(), (Region) obj3);
            }
        }, this, Integer.valueOf(displayId), region));
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void setTouchExplorationPassthroughRegion(int displayId, Region region) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda43
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).setTouchExplorationPassthroughRegionInternal(((Integer) obj2).intValue(), (Region) obj3);
            }
        }, this, Integer.valueOf(displayId), region));
    }

    public void setTouchExplorationPassthroughRegionInternal(int displayId, Region region) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.setTouchExplorationPassthroughRegion(displayId, region);
            }
        }
    }

    public void setGestureDetectionPassthroughRegionInternal(int displayId, Region region) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.setGestureDetectionPassthroughRegion(displayId, region);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void setServiceDetectsGesturesEnabled(int displayId, boolean mode) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda37
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).setServiceDetectsGesturesInternal(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue());
            }
        }, this, Integer.valueOf(displayId), Boolean.valueOf(mode)));
    }

    public void setServiceDetectsGesturesInternal(int displayId, boolean mode) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.setServiceDetectsGesturesEnabled(displayId, mode);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void requestTouchExploration(int displayId) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda32
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).requestTouchExplorationInternal(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(displayId)));
    }

    public void requestTouchExplorationInternal(int displayId) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.requestTouchExploration(displayId);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void requestDragging(int displayId, int pointerId) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda26
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).requestDraggingInternal(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
            }
        }, this, Integer.valueOf(displayId), Integer.valueOf(pointerId)));
    }

    public void requestDraggingInternal(int displayId, int pointerId) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.requestDragging(displayId, pointerId);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void requestDelegating(int displayId) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda36
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).requestDelegatingInternal(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(displayId)));
    }

    public void requestDelegatingInternal(int displayId) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.requestDelegating(displayId);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void onDoubleTap(int displayId) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda52
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).onDoubleTapInternal(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(displayId)));
    }

    public void onDoubleTapInternal(int displayId) {
        AccessibilityInputFilter accessibilityInputFilter;
        AccessibilityInputFilter inputFilter = null;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                inputFilter = accessibilityInputFilter;
            }
        }
        if (inputFilter != null) {
            inputFilter.onDoubleTap(displayId);
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void onDoubleTapAndHold(int displayId) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).onDoubleTapAndHoldInternal(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(displayId)));
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void requestImeLocked(AbstractAccessibilityServiceConnection connection) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda48
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).createSessionForConnection((AbstractAccessibilityServiceConnection) obj2);
            }
        }, this, connection));
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda49
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).bindAndStartInputForConnection((AbstractAccessibilityServiceConnection) obj2);
            }
        }, this, connection));
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection.SystemSupport
    public void unbindImeLocked(AbstractAccessibilityServiceConnection connection) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda8
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).unbindInputForConnection((AbstractAccessibilityServiceConnection) obj2);
            }
        }, this, connection));
    }

    public void createSessionForConnection(AbstractAccessibilityServiceConnection connection) {
        synchronized (this.mLock) {
            if (this.mInputSessionRequested) {
                connection.createImeSessionLocked();
            }
        }
    }

    public void bindAndStartInputForConnection(AbstractAccessibilityServiceConnection connection) {
        synchronized (this.mLock) {
            if (this.mInputBound) {
                connection.bindInputLocked();
                connection.startInputLocked(this.mRemoteInputConnection, this.mEditorInfo, this.mRestarting);
            }
        }
    }

    public void unbindInputForConnection(AbstractAccessibilityServiceConnection connection) {
        InputMethodManagerInternal.get().unbindAccessibilityFromCurrentClient(connection.mId);
        synchronized (this.mLock) {
            connection.unbindInputLocked();
        }
    }

    public void onDoubleTapAndHoldInternal(int displayId) {
        AccessibilityInputFilter accessibilityInputFilter;
        synchronized (this.mLock) {
            if (this.mHasInputFilter && (accessibilityInputFilter = this.mInputFilter) != null) {
                accessibilityInputFilter.onDoubleTapAndHold(displayId);
            }
        }
    }

    private void updateFocusAppearanceDataLocked(final AccessibilityUserState userState) {
        if (userState.mUserId != this.mCurrentUserId) {
            return;
        }
        if (this.mTraceManager.isA11yTracingEnabledForTypes(2L)) {
            this.mTraceManager.logTrace("AccessibilityManagerService.updateFocusAppearanceDataLocked", 2L, "userState=" + userState);
        }
        this.mMainHandler.post(new Runnable() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                AccessibilityManagerService.this.m653x80d31fbe(userState);
            }
        });
    }

    /* renamed from: lambda$updateFocusAppearanceDataLocked$25$com-android-server-accessibility-AccessibilityManagerService */
    public /* synthetic */ void m653x80d31fbe(final AccessibilityUserState userState) {
        broadcastToClients(userState, FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda6
            public final void acceptOrThrow(Object obj) {
                ((AccessibilityManagerService.Client) obj).mCallback.setFocusAppearance(r0.getFocusStrokeWidthLocked(), AccessibilityUserState.this.getFocusColorLocked());
            }
        }));
    }

    public AccessibilityTraceManager getTraceManager() {
        return this.mTraceManager;
    }

    public void scheduleBindInput() {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda19
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((AccessibilityManagerService) obj).bindInput();
            }
        }, this));
    }

    public void bindInput() {
        synchronized (this.mLock) {
            this.mInputBound = true;
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (service.requestImeApis()) {
                    service.bindInputLocked();
                }
            }
        }
    }

    public void scheduleUnbindInput() {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda22
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((AccessibilityManagerService) obj).unbindInput();
            }
        }, this));
    }

    public void unbindInput() {
        synchronized (this.mLock) {
            this.mInputBound = false;
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (service.requestImeApis()) {
                    service.unbindInputLocked();
                }
            }
        }
    }

    public void scheduleStartInput(IRemoteAccessibilityInputConnection connection, EditorInfo editorInfo, boolean restarting) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda41
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((AccessibilityManagerService) obj).startInput((IRemoteAccessibilityInputConnection) obj2, (EditorInfo) obj3, ((Boolean) obj4).booleanValue());
            }
        }, this, connection, editorInfo, Boolean.valueOf(restarting)));
    }

    public void startInput(IRemoteAccessibilityInputConnection connection, EditorInfo editorInfo, boolean restarting) {
        synchronized (this.mLock) {
            this.mRemoteInputConnection = connection;
            this.mEditorInfo = editorInfo;
            this.mRestarting = restarting;
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (service.requestImeApis()) {
                    service.startInputLocked(connection, editorInfo, restarting);
                }
            }
        }
    }

    public void scheduleCreateImeSession(ArraySet<Integer> ignoreSet) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).createImeSession((ArraySet) obj2);
            }
        }, this, ignoreSet));
    }

    public void createImeSession(ArraySet<Integer> ignoreSet) {
        synchronized (this.mLock) {
            this.mInputSessionRequested = true;
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (!ignoreSet.contains(Integer.valueOf(service.mId)) && service.requestImeApis()) {
                    service.createImeSessionLocked();
                }
            }
        }
    }

    public void scheduleSetImeSessionEnabled(SparseArray<IAccessibilityInputMethodSession> sessions, boolean enabled) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityManagerService$$ExternalSyntheticLambda47
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).setImeSessionEnabled((SparseArray) obj2, ((Boolean) obj3).booleanValue());
            }
        }, this, sessions, Boolean.valueOf(enabled)));
    }

    public void setImeSessionEnabled(SparseArray<IAccessibilityInputMethodSession> sessions, boolean enabled) {
        synchronized (this.mLock) {
            AccessibilityUserState userState = getCurrentUserStateLocked();
            for (int i = userState.mBoundServices.size() - 1; i >= 0; i--) {
                AccessibilityServiceConnection service = userState.mBoundServices.get(i);
                if (sessions.contains(service.mId) && service.requestImeApis()) {
                    service.setImeSessionEnabledLocked(sessions.get(service.mId), enabled);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public final class SendWindowStateChangedEventRunnable implements Runnable {
        private final AccessibilityEvent mPendingEvent;
        private final int mWindowId;

        SendWindowStateChangedEventRunnable(AccessibilityEvent event) {
            AccessibilityManagerService.this = r1;
            this.mPendingEvent = event;
            this.mWindowId = event.getWindowId();
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (AccessibilityManagerService.this.mLock) {
                Slog.w(AccessibilityManagerService.LOG_TAG, " wait for adding window timeout: " + this.mWindowId);
                sendPendingEventLocked();
            }
        }

        public void sendPendingEventLocked() {
            AccessibilityManagerService.this.mSendWindowStateChangedEventRunnables.remove(this);
            AccessibilityManagerService.this.dispatchAccessibilityEventLocked(this.mPendingEvent);
        }

        public int getWindowId() {
            return this.mWindowId;
        }
    }

    void sendPendingWindowStateChangedEventsForAvailableWindowLocked(int windowId) {
        int eventSize = this.mSendWindowStateChangedEventRunnables.size();
        for (int i = eventSize - 1; i >= 0; i--) {
            SendWindowStateChangedEventRunnable runnable = this.mSendWindowStateChangedEventRunnables.get(i);
            if (runnable.getWindowId() == windowId) {
                this.mMainHandler.removeCallbacks(runnable);
                runnable.sendPendingEventLocked();
            }
        }
    }

    private boolean postponeWindowStateEvent(AccessibilityEvent event) {
        synchronized (this.mLock) {
            int resolvedWindowId = this.mA11yWindowManager.resolveParentWindowIdLocked(event.getWindowId());
            if (this.mA11yWindowManager.findWindowInfoByIdLocked(resolvedWindowId) != null) {
                return false;
            }
            SendWindowStateChangedEventRunnable pendingRunnable = new SendWindowStateChangedEventRunnable(new AccessibilityEvent(event));
            this.mMainHandler.postDelayed(pendingRunnable, 500L);
            this.mSendWindowStateChangedEventRunnables.add(pendingRunnable);
            return true;
        }
    }
}
