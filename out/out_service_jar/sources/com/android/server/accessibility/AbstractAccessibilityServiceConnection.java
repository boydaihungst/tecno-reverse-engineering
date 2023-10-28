package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.accessibilityservice.MagnificationConfig;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.ParcelableColorSpace;
import android.graphics.Region;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import android.view.inputmethod.EditorInfo;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IAccessibilityInputMethodSessionCallback;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.accessibility.AccessibilityWindowManager;
import com.android.server.accessibility.FingerprintGestureDispatcher;
import com.android.server.accessibility.KeyEventDispatcher;
import com.android.server.accessibility.magnification.MagnificationProcessor;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class AbstractAccessibilityServiceConnection extends IAccessibilityServiceConnection.Stub implements ServiceConnection, IBinder.DeathRecipient, KeyEventDispatcher.KeyEventFilter, FingerprintGestureDispatcher.FingerprintGestureClient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AbstractAccessibilityServiceConnection";
    protected static final String TAKE_SCREENSHOT = "takeScreenshot";
    private static final String TRACE_SVC_CLIENT = "AbstractAccessibilityServiceConnection.IAccessibilityServiceClient";
    private static final String TRACE_SVC_CONN = "AbstractAccessibilityServiceConnection.IAccessibilityServiceConnection";
    private static final String TRACE_WM = "WindowManagerInternal";
    private static final int WAIT_WINDOWS_TIMEOUT_MILLIS = 5000;
    private final AccessibilityWindowManager mA11yWindowManager;
    protected final AccessibilityServiceInfo mAccessibilityServiceInfo;
    protected String mAttributionTag;
    boolean mCaptureFingerprintGestures;
    final ComponentName mComponentName;
    protected final Context mContext;
    private final DisplayManager mDisplayManager;
    public Handler mEventDispatchHandler;
    int mEventTypes;
    int mFeedbackType;
    int mFetchFlags;
    final int mId;
    public final InvocationHandler mInvocationHandler;
    boolean mIsDefault;
    boolean mLastAccessibilityButtonCallbackState;
    protected final Object mLock;
    private final Handler mMainHandler;
    long mNotificationTimeout;
    private final PowerManager mPowerManager;
    boolean mReceivedAccessibilityButtonCallbackSinceBind;
    boolean mRequestAccessibilityButton;
    boolean mRequestFilterKeyEvents;
    boolean mRequestImeApis;
    private boolean mRequestMultiFingerGestures;
    private long mRequestTakeScreenshotTimestampMs;
    boolean mRequestTouchExplorationMode;
    private boolean mRequestTwoFingerPassthrough;
    boolean mRetrieveInteractiveWindows;
    protected final AccessibilitySecurityPolicy mSecurityPolicy;
    private boolean mSendMotionEvents;
    IBinder mService;
    private boolean mServiceHandlesDoubleTap;
    IAccessibilityServiceClient mServiceInterface;
    private final SystemActionPerformer mSystemActionPerformer;
    protected final SystemSupport mSystemSupport;
    protected final AccessibilityTrace mTrace;
    protected final WindowManagerInternal mWindowManagerService;
    Set<String> mPackageNames = new HashSet();
    final SparseArray<AccessibilityEvent> mPendingEvents = new SparseArray<>();
    boolean mUsesAccessibilityCache = false;
    final SparseArray<IBinder> mOverlayWindowTokens = new SparseArray<>();
    private final IPlatformCompat mIPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));

    /* loaded from: classes.dex */
    public interface SystemSupport {
        int getCurrentUserIdLocked();

        FingerprintGestureDispatcher getFingerprintGestureDispatcher();

        KeyEventDispatcher getKeyEventDispatcher();

        MagnificationProcessor getMagnificationProcessor();

        MotionEventInjector getMotionEventInjectorForDisplayLocked(int i);

        PendingIntent getPendingIntentActivity(Context context, int i, Intent intent, int i2);

        Pair<float[], MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(int i);

        boolean isAccessibilityButtonShown();

        void onClientChangeLocked(boolean z);

        void onDoubleTap(int i);

        void onDoubleTapAndHold(int i);

        void persistComponentNamesToSettingLocked(String str, Set<ComponentName> set, int i);

        void requestDelegating(int i);

        void requestDragging(int i, int i2);

        void requestImeLocked(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection);

        void requestTouchExploration(int i);

        void setGestureDetectionPassthroughRegion(int i, Region region);

        void setServiceDetectsGesturesEnabled(int i, boolean z);

        void setTouchExplorationPassthroughRegion(int i, Region region);

        void unbindImeLocked(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection);
    }

    protected abstract boolean hasRightsToCurrentUserLocked();

    public AbstractAccessibilityServiceConnection(Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, AccessibilitySecurityPolicy securityPolicy, SystemSupport systemSupport, AccessibilityTrace trace, WindowManagerInternal windowManagerInternal, SystemActionPerformer systemActionPerfomer, AccessibilityWindowManager a11yWindowManager) {
        this.mContext = context;
        this.mWindowManagerService = windowManagerInternal;
        this.mId = id;
        this.mComponentName = componentName;
        this.mAccessibilityServiceInfo = accessibilityServiceInfo;
        this.mLock = lock;
        this.mSecurityPolicy = securityPolicy;
        this.mSystemActionPerformer = systemActionPerfomer;
        this.mSystemSupport = systemSupport;
        this.mTrace = trace;
        this.mMainHandler = mainHandler;
        this.mInvocationHandler = new InvocationHandler(mainHandler.getLooper());
        this.mA11yWindowManager = a11yWindowManager;
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mEventDispatchHandler = new Handler(mainHandler.getLooper()) { // from class: com.android.server.accessibility.AbstractAccessibilityServiceConnection.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                int eventType = message.what;
                AccessibilityEvent event = (AccessibilityEvent) message.obj;
                boolean serviceWantsEvent = message.arg1 != 0;
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityEventInternal(eventType, event, serviceWantsEvent);
            }
        };
        setDynamicallyConfigurableProperties(accessibilityServiceInfo);
    }

    @Override // com.android.server.accessibility.KeyEventDispatcher.KeyEventFilter
    public boolean onKeyEvent(KeyEvent keyEvent, int sequenceNumber) {
        if (!this.mRequestFilterKeyEvents || this.mServiceInterface == null || (this.mAccessibilityServiceInfo.getCapabilities() & 8) == 0 || !this.mSecurityPolicy.checkAccessibilityAccess(this)) {
            return false;
        }
        try {
            if (svcClientTracingEnabled()) {
                logTraceSvcClient("onKeyEvent", keyEvent + ", " + sequenceNumber);
            }
            this.mServiceInterface.onKeyEvent(keyEvent, sequenceNumber);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setDynamicallyConfigurableProperties(AccessibilityServiceInfo info) {
        this.mEventTypes = info.eventTypes;
        this.mFeedbackType = info.feedbackType;
        String[] packageNames = info.packageNames;
        this.mPackageNames.clear();
        if (packageNames != null) {
            this.mPackageNames.addAll(Arrays.asList(packageNames));
        }
        this.mNotificationTimeout = info.notificationTimeout;
        this.mIsDefault = (info.flags & 1) != 0;
        if (supportsFlagForNotImportantViews(info)) {
            if ((info.flags & 2) != 0) {
                this.mFetchFlags |= 128;
            } else {
                this.mFetchFlags &= -129;
            }
        }
        if ((info.flags & 16) != 0) {
            this.mFetchFlags |= 256;
        } else {
            this.mFetchFlags &= -257;
        }
        this.mRequestTouchExplorationMode = (info.flags & 4) != 0;
        this.mServiceHandlesDoubleTap = (info.flags & 2048) != 0;
        this.mRequestMultiFingerGestures = (info.flags & 4096) != 0;
        this.mRequestTwoFingerPassthrough = (info.flags & 8192) != 0;
        this.mSendMotionEvents = (info.flags & 16384) != 0;
        this.mRequestFilterKeyEvents = (info.flags & 32) != 0;
        this.mRetrieveInteractiveWindows = (info.flags & 64) != 0;
        this.mCaptureFingerprintGestures = (info.flags & 512) != 0;
        this.mRequestAccessibilityButton = (info.flags & 256) != 0;
        this.mRequestImeApis = (info.flags & 32768) != 0;
    }

    protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
        return info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion >= 16;
    }

    public boolean canReceiveEventsLocked() {
        return (this.mEventTypes == 0 || this.mService == null) ? false : true;
    }

    public void setOnKeyEventResult(boolean handled, int sequence) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setOnKeyEventResult", "handled=" + handled + ";sequence=" + sequence);
        }
        this.mSystemSupport.getKeyEventDispatcher().setOnKeyEventResult(this, handled, sequence);
    }

    public AccessibilityServiceInfo getServiceInfo() {
        AccessibilityServiceInfo accessibilityServiceInfo;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getServiceInfo", "");
        }
        synchronized (this.mLock) {
            accessibilityServiceInfo = this.mAccessibilityServiceInfo;
        }
        return accessibilityServiceInfo;
    }

    public int getCapabilities() {
        return this.mAccessibilityServiceInfo.getCapabilities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRelevantEventTypes() {
        return (this.mUsesAccessibilityCache ? 4307005 : 32) | this.mEventTypes;
    }

    public void setServiceInfo(AccessibilityServiceInfo info) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setServiceInfo", "info=" + info);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                boolean oldRequestIme = this.mRequestImeApis;
                AccessibilityServiceInfo oldInfo = this.mAccessibilityServiceInfo;
                if (oldInfo != null) {
                    oldInfo.updateDynamicallyConfigurableProperties(this.mIPlatformCompat, info);
                    setDynamicallyConfigurableProperties(oldInfo);
                } else {
                    setDynamicallyConfigurableProperties(info);
                }
                this.mSystemSupport.onClientChangeLocked(true);
                if (!oldRequestIme && this.mRequestImeApis) {
                    this.mSystemSupport.requestImeLocked(this);
                } else if (oldRequestIme && !this.mRequestImeApis) {
                    this.mSystemSupport.unbindImeLocked(this);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setAttributionTag(String attributionTag) {
        this.mAttributionTag = attributionTag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getAttributionTag() {
        return this.mAttributionTag;
    }

    public AccessibilityWindowInfo.WindowListSparseArray getWindows() {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getWindows", "");
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                boolean permissionGranted = this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                if (permissionGranted) {
                    if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                        AccessibilityWindowInfo.WindowListSparseArray allWindows = new AccessibilityWindowInfo.WindowListSparseArray();
                        ArrayList<Integer> displayList = this.mA11yWindowManager.getDisplayListLocked();
                        int displayListCounts = displayList.size();
                        if (displayListCounts > 0) {
                            for (int i = 0; i < displayListCounts; i++) {
                                int displayId = displayList.get(i).intValue();
                                ensureWindowsAvailableTimedLocked(displayId);
                                List<AccessibilityWindowInfo> windowList = getWindowsByDisplayLocked(displayId);
                                if (windowList != null) {
                                    allWindows.put(displayId, windowList);
                                }
                            }
                        }
                        return allWindows;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }
    }

    public AccessibilityWindowInfo getWindow(int windowId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getWindow", "windowId=" + windowId);
        }
        synchronized (this.mLock) {
            int displayId = -1;
            if (windowId != -1) {
                displayId = this.mA11yWindowManager.getDisplayIdByUserIdAndWindowIdLocked(this.mSystemSupport.getCurrentUserIdLocked(), windowId);
            }
            ensureWindowsAvailableTimedLocked(displayId);
            if (hasRightsToCurrentUserLocked()) {
                boolean permissionGranted = this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                if (permissionGranted) {
                    if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                        AccessibilityWindowInfo window = this.mA11yWindowManager.findA11yWindowInfoByIdLocked(windowId);
                        if (window != null) {
                            AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
                            windowClone.setConnectionId(this.mId);
                            return windowClone;
                        }
                        return null;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [609=4] */
    public String[] findAccessibilityNodeInfosByViewId(int accessibilityWindowId, long accessibilityNodeId, String viewIdResName, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion;
        MagnificationSpec spec;
        int interrogatingPid;
        Region partialInteractiveRegion2;
        MagnificationSpec spec2;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("findAccessibilityNodeInfosByViewId", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";viewIdResName=" + viewIdResName + ";interactionId=" + interactionId + ";callback=" + callback + ";interrogatingTid=" + interrogatingTid);
        }
        Region partialInteractiveRegion3 = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (hasRightsToCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    boolean permissionGranted = this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId);
                    if (permissionGranted) {
                        AccessibilityWindowManager.RemoteAccessibilityConnection connection = this.mA11yWindowManager.getConnectionLocked(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (this.mA11yWindowManager.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion3)) {
                            partialInteractiveRegion = partialInteractiveRegion3;
                        } else {
                            partialInteractiveRegion3.recycle();
                            partialInteractiveRegion = null;
                        }
                        try {
                            Pair<float[], MagnificationSpec> transformMatrixAndSpec = getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
                            float[] transformMatrix = (float[]) transformMatrixAndSpec.first;
                            MagnificationSpec spec3 = (MagnificationSpec) transformMatrixAndSpec.second;
                            if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                                int interrogatingPid2 = Binder.getCallingPid();
                                Region partialInteractiveRegion4 = partialInteractiveRegion;
                                IAccessibilityInteractionConnectionCallback callback2 = replaceCallbackIfNeeded(callback, resolvedWindowId, interactionId, interrogatingPid2, interrogatingTid);
                                long identityToken = Binder.clearCallingIdentity();
                                if (intConnTracingEnabled()) {
                                    interrogatingPid = interrogatingPid2;
                                    spec = spec3;
                                    logTraceIntConn("findAccessibilityNodeInfosByViewId", accessibilityNodeId + ";" + viewIdResName + ";" + partialInteractiveRegion4 + ";" + interactionId + ";" + callback2 + ";" + this.mFetchFlags + ";" + interrogatingPid + ";" + interrogatingTid + ";" + spec + ";" + Arrays.toString(transformMatrix));
                                } else {
                                    spec = spec3;
                                    interrogatingPid = interrogatingPid2;
                                }
                                try {
                                    spec2 = spec;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (RemoteException e) {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (Throwable th) {
                                    th = th;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                }
                                try {
                                    connection.getRemote().findAccessibilityNodeInfosByViewId(accessibilityNodeId, viewIdResName, partialInteractiveRegion2, interactionId, callback2, this.mFetchFlags, interrogatingPid, interrogatingTid, spec2, transformMatrix);
                                    String[] computeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(connection.getPackageName(), connection.getUid());
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return computeValidReportedPackages;
                                } catch (RemoteException e2) {
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    throw th;
                                }
                            }
                            return null;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    return null;
                }
                return null;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [684=4] */
    public String[] findAccessibilityNodeInfosByText(int accessibilityWindowId, long accessibilityNodeId, String text, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion;
        MagnificationSpec spec;
        int interrogatingPid;
        Region partialInteractiveRegion2;
        MagnificationSpec spec2;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("findAccessibilityNodeInfosByText", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";text=" + text + ";interactionId=" + interactionId + ";callback=" + callback + ";interrogatingTid=" + interrogatingTid);
        }
        Region partialInteractiveRegion3 = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (hasRightsToCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    boolean permissionGranted = this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId);
                    if (permissionGranted) {
                        AccessibilityWindowManager.RemoteAccessibilityConnection connection = this.mA11yWindowManager.getConnectionLocked(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (this.mA11yWindowManager.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion3)) {
                            partialInteractiveRegion = partialInteractiveRegion3;
                        } else {
                            partialInteractiveRegion3.recycle();
                            partialInteractiveRegion = null;
                        }
                        try {
                            Pair<float[], MagnificationSpec> transformMatrixAndSpec = getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
                            float[] transformMatrix = (float[]) transformMatrixAndSpec.first;
                            MagnificationSpec spec3 = (MagnificationSpec) transformMatrixAndSpec.second;
                            if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                                int interrogatingPid2 = Binder.getCallingPid();
                                Region partialInteractiveRegion4 = partialInteractiveRegion;
                                IAccessibilityInteractionConnectionCallback callback2 = replaceCallbackIfNeeded(callback, resolvedWindowId, interactionId, interrogatingPid2, interrogatingTid);
                                long identityToken = Binder.clearCallingIdentity();
                                if (intConnTracingEnabled()) {
                                    interrogatingPid = interrogatingPid2;
                                    spec = spec3;
                                    logTraceIntConn("findAccessibilityNodeInfosByText", accessibilityNodeId + ";" + text + ";" + partialInteractiveRegion4 + ";" + interactionId + ";" + callback2 + ";" + this.mFetchFlags + ";" + interrogatingPid + ";" + interrogatingTid + ";" + spec + ";" + Arrays.toString(transformMatrix));
                                } else {
                                    spec = spec3;
                                    interrogatingPid = interrogatingPid2;
                                }
                                try {
                                    spec2 = spec;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (RemoteException e) {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (Throwable th) {
                                    th = th;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                }
                                try {
                                    connection.getRemote().findAccessibilityNodeInfosByText(accessibilityNodeId, text, partialInteractiveRegion2, interactionId, callback2, this.mFetchFlags, interrogatingPid, interrogatingTid, spec2, transformMatrix);
                                    String[] computeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(connection.getPackageName(), connection.getUid());
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return computeValidReportedPackages;
                                } catch (RemoteException e2) {
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    throw th;
                                }
                            }
                            return null;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    return null;
                }
                return null;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [762=4] */
    public String[] findAccessibilityNodeInfoByAccessibilityId(int accessibilityWindowId, long accessibilityNodeId, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, long interrogatingTid, Bundle arguments) throws RemoteException {
        Region partialInteractiveRegion;
        MagnificationSpec spec;
        int interrogatingPid;
        Region partialInteractiveRegion2;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("findAccessibilityNodeInfoByAccessibilityId", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";interactionId=" + interactionId + ";callback=" + callback + ";flags=" + flags + ";interrogatingTid=" + interrogatingTid + ";arguments=" + arguments);
        }
        Region partialInteractiveRegion3 = Region.obtain();
        synchronized (this.mLock) {
            try {
                this.mUsesAccessibilityCache = true;
                if (hasRightsToCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                    boolean permissionGranted = this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId);
                    if (permissionGranted) {
                        AccessibilityWindowManager.RemoteAccessibilityConnection connection = this.mA11yWindowManager.getConnectionLocked(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (this.mA11yWindowManager.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion3)) {
                            partialInteractiveRegion = partialInteractiveRegion3;
                        } else {
                            partialInteractiveRegion3.recycle();
                            partialInteractiveRegion = null;
                        }
                        try {
                            Pair<float[], MagnificationSpec> transformMatrixAndSpec = getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
                            float[] transformMatrix = (float[]) transformMatrixAndSpec.first;
                            MagnificationSpec spec2 = (MagnificationSpec) transformMatrixAndSpec.second;
                            if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                                int interrogatingPid2 = Binder.getCallingPid();
                                Region partialInteractiveRegion4 = partialInteractiveRegion;
                                IAccessibilityInteractionConnectionCallback callback2 = replaceCallbackIfNeeded(callback, resolvedWindowId, interactionId, interrogatingPid2, interrogatingTid);
                                long identityToken = Binder.clearCallingIdentity();
                                if (intConnTracingEnabled()) {
                                    interrogatingPid = interrogatingPid2;
                                    spec = spec2;
                                    logTraceIntConn("findAccessibilityNodeInfoByAccessibilityId", accessibilityNodeId + ";" + partialInteractiveRegion4 + ";" + interactionId + ";" + callback2 + ";" + (this.mFetchFlags | flags) + ";" + interrogatingPid + ";" + interrogatingTid + ";" + spec2 + ";" + Arrays.toString(transformMatrix) + ";" + arguments);
                                } else {
                                    spec = spec2;
                                    interrogatingPid = interrogatingPid2;
                                }
                                try {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (RemoteException e) {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (Throwable th) {
                                    th = th;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                }
                                try {
                                    connection.getRemote().findAccessibilityNodeInfoByAccessibilityId(accessibilityNodeId, partialInteractiveRegion2, interactionId, callback2, this.mFetchFlags | flags, interrogatingPid, interrogatingTid, spec, transformMatrix, arguments);
                                    String[] computeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(connection.getPackageName(), connection.getUid());
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return computeValidReportedPackages;
                                } catch (RemoteException e2) {
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    throw th;
                                }
                            }
                            return null;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    return null;
                }
                return null;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [839=4] */
    public String[] findFocus(int accessibilityWindowId, long accessibilityNodeId, int focusType, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion;
        int interrogatingPid;
        MagnificationSpec spec;
        Region partialInteractiveRegion2;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("findFocus", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";focusType=" + focusType + ";interactionId=" + interactionId + ";callback=" + callback + ";interrogatingTid=" + interrogatingTid);
        }
        Region partialInteractiveRegion3 = Region.obtain();
        synchronized (this.mLock) {
            try {
                if (hasRightsToCurrentUserLocked()) {
                    int resolvedWindowId = resolveAccessibilityWindowIdForFindFocusLocked(accessibilityWindowId, focusType);
                    boolean permissionGranted = this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId);
                    if (permissionGranted) {
                        AccessibilityWindowManager.RemoteAccessibilityConnection connection = this.mA11yWindowManager.getConnectionLocked(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId);
                        if (connection == null) {
                            return null;
                        }
                        if (this.mA11yWindowManager.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion3)) {
                            partialInteractiveRegion = partialInteractiveRegion3;
                        } else {
                            partialInteractiveRegion3.recycle();
                            partialInteractiveRegion = null;
                        }
                        try {
                            Pair<float[], MagnificationSpec> transformMatrixAndSpec = getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
                            float[] transformMatrix = (float[]) transformMatrixAndSpec.first;
                            MagnificationSpec spec2 = (MagnificationSpec) transformMatrixAndSpec.second;
                            if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                                int interrogatingPid2 = Binder.getCallingPid();
                                Region partialInteractiveRegion4 = partialInteractiveRegion;
                                IAccessibilityInteractionConnectionCallback callback2 = replaceCallbackIfNeeded(callback, resolvedWindowId, interactionId, interrogatingPid2, interrogatingTid);
                                long identityToken = Binder.clearCallingIdentity();
                                if (intConnTracingEnabled()) {
                                    interrogatingPid = interrogatingPid2;
                                    spec = spec2;
                                    logTraceIntConn("findFocus", accessibilityNodeId + ";" + focusType + ";" + partialInteractiveRegion4 + ";" + interactionId + ";" + callback2 + ";" + this.mFetchFlags + ";" + interrogatingPid + ";" + interrogatingTid + ";" + spec + ";" + Arrays.toString(transformMatrix));
                                } else {
                                    interrogatingPid = interrogatingPid2;
                                    spec = spec2;
                                }
                                try {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (RemoteException e) {
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                } catch (Throwable th) {
                                    th = th;
                                    partialInteractiveRegion2 = partialInteractiveRegion4;
                                }
                                try {
                                    connection.getRemote().findFocus(accessibilityNodeId, focusType, partialInteractiveRegion2, interactionId, callback2, this.mFetchFlags, interrogatingPid, interrogatingTid, spec, transformMatrix);
                                    String[] computeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(connection.getPackageName(), connection.getUid());
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return computeValidReportedPackages;
                                } catch (RemoteException e2) {
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    return null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identityToken);
                                    if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                        partialInteractiveRegion2.recycle();
                                    }
                                    throw th;
                                }
                            }
                            return null;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    return null;
                }
                return null;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [915=4] */
    public String[] focusSearch(int accessibilityWindowId, long accessibilityNodeId, int direction, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        Region partialInteractiveRegion;
        MagnificationSpec spec;
        int interrogatingPid;
        Region partialInteractiveRegion2;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("focusSearch", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";direction=" + direction + ";interactionId=" + interactionId + ";callback=" + callback + ";interrogatingTid=" + interrogatingTid);
        }
        Region partialInteractiveRegion3 = Region.obtain();
        synchronized (this.mLock) {
            try {
                if (!hasRightsToCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                AccessibilityWindowManager.RemoteAccessibilityConnection connection = this.mA11yWindowManager.getConnectionLocked(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (this.mA11yWindowManager.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion3)) {
                    partialInteractiveRegion = partialInteractiveRegion3;
                } else {
                    partialInteractiveRegion3.recycle();
                    partialInteractiveRegion = null;
                }
                try {
                    Pair<float[], MagnificationSpec> transformMatrixAndSpec = getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
                    float[] transformMatrix = (float[]) transformMatrixAndSpec.first;
                    MagnificationSpec spec2 = (MagnificationSpec) transformMatrixAndSpec.second;
                    if (!this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                        return null;
                    }
                    int interrogatingPid2 = Binder.getCallingPid();
                    Region partialInteractiveRegion4 = partialInteractiveRegion;
                    IAccessibilityInteractionConnectionCallback callback2 = replaceCallbackIfNeeded(callback, resolvedWindowId, interactionId, interrogatingPid2, interrogatingTid);
                    long identityToken = Binder.clearCallingIdentity();
                    if (intConnTracingEnabled()) {
                        interrogatingPid = interrogatingPid2;
                        spec = spec2;
                        logTraceIntConn("focusSearch", accessibilityNodeId + ";" + direction + ";" + partialInteractiveRegion4 + ";" + interactionId + ";" + callback2 + ";" + this.mFetchFlags + ";" + interrogatingPid + ";" + interrogatingTid + ";" + spec + ";" + Arrays.toString(transformMatrix));
                    } else {
                        spec = spec2;
                        interrogatingPid = interrogatingPid2;
                    }
                    try {
                        MagnificationSpec spec3 = spec;
                        partialInteractiveRegion2 = partialInteractiveRegion4;
                        try {
                            connection.getRemote().focusSearch(accessibilityNodeId, direction, partialInteractiveRegion2, interactionId, callback2, this.mFetchFlags, interrogatingPid, interrogatingTid, spec3, transformMatrix);
                            String[] computeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(connection.getPackageName(), connection.getUid());
                            Binder.restoreCallingIdentity(identityToken);
                            if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                partialInteractiveRegion2.recycle();
                            }
                            return computeValidReportedPackages;
                        } catch (RemoteException e) {
                            Binder.restoreCallingIdentity(identityToken);
                            if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                partialInteractiveRegion2.recycle();
                            }
                            return null;
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identityToken);
                            if (partialInteractiveRegion2 != null && Binder.isProxy(connection.getRemote())) {
                                partialInteractiveRegion2.recycle();
                            }
                            throw th;
                        }
                    } catch (RemoteException e2) {
                        partialInteractiveRegion2 = partialInteractiveRegion4;
                    } catch (Throwable th2) {
                        th = th2;
                        partialInteractiveRegion2 = partialInteractiveRegion4;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public void sendGesture(int sequence, ParceledListSlice gestureSteps) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("sendGesture", "sequence=" + sequence + ";gestureSteps=" + gestureSteps);
        }
    }

    public void dispatchGesture(int sequence, ParceledListSlice gestureSteps, int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("dispatchGesture", "sequence=" + sequence + ";gestureSteps=" + gestureSteps + ";displayId=" + displayId);
        }
    }

    public boolean performAccessibilityAction(int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("performAccessibilityAction", "accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";action=" + action + ";arguments=" + arguments + ";interactionId=" + interactionId + ";callback=" + callback + ";interrogatingTid=" + interrogatingTid);
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this.mSystemSupport.getCurrentUserIdLocked(), this, resolvedWindowId)) {
                    if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                        return performAccessibilityActionInternal(this.mSystemSupport.getCurrentUserIdLocked(), resolvedWindowId, accessibilityNodeId, action, arguments, interactionId, callback, this.mFetchFlags, interrogatingTid);
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
    }

    public boolean performGlobalAction(int action) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("performGlobalAction", "action=" + action);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return false;
            }
            return this.mSystemActionPerformer.performSystemAction(action);
        }
    }

    public List<AccessibilityNodeInfo.AccessibilityAction> getSystemActions() {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getSystemActions", "");
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return Collections.emptyList();
            }
            return this.mSystemActionPerformer.getSystemActions();
        }
    }

    public boolean isFingerprintGestureDetectionAvailable() {
        FingerprintGestureDispatcher dispatcher;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("isFingerprintGestureDetectionAvailable", "");
        }
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint") && isCapturingFingerprintGestures() && (dispatcher = this.mSystemSupport.getFingerprintGestureDispatcher()) != null && dispatcher.isFingerprintGestureDetectionAvailable();
    }

    public MagnificationConfig getMagnificationConfig(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getMagnificationConfig", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mSystemSupport.getMagnificationProcessor().getMagnificationConfig(displayId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public float getMagnificationScale(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getMagnificationScale", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return 1.0f;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mSystemSupport.getMagnificationProcessor().getScale(displayId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public Region getMagnificationRegion(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getMagnificationRegion", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            Region region = Region.obtain();
            if (hasRightsToCurrentUserLocked()) {
                MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
                long identity = Binder.clearCallingIdentity();
                magnificationProcessor.getFullscreenMagnificationRegion(displayId, region, this.mSecurityPolicy.canControlMagnification(this));
                Binder.restoreCallingIdentity(identity);
                return region;
            }
            return region;
        }
    }

    public Region getCurrentMagnificationRegion(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getCurrentMagnificationRegion", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            Region region = Region.obtain();
            if (hasRightsToCurrentUserLocked()) {
                MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
                long identity = Binder.clearCallingIdentity();
                magnificationProcessor.getCurrentMagnificationRegion(displayId, region, this.mSecurityPolicy.canControlMagnification(this));
                Binder.restoreCallingIdentity(identity);
                return region;
            }
            return region;
        }
    }

    public float getMagnificationCenterX(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getMagnificationCenterX", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
            long identity = Binder.clearCallingIdentity();
            float centerX = magnificationProcessor.getCenterX(displayId, this.mSecurityPolicy.canControlMagnification(this));
            Binder.restoreCallingIdentity(identity);
            return centerX;
        }
    }

    public float getMagnificationCenterY(int displayId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getMagnificationCenterY", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
            long identity = Binder.clearCallingIdentity();
            float centerY = magnificationProcessor.getCenterY(displayId, this.mSecurityPolicy.canControlMagnification(this));
            Binder.restoreCallingIdentity(identity);
            return centerY;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x0054, code lost:
        if (r3.isMagnifying(r6) == false) goto L26;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean resetMagnification(int displayId, boolean animate) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("resetMagnification", "displayId=" + displayId + ";animate=" + animate);
        }
        synchronized (this.mLock) {
            boolean z = false;
            if (hasRightsToCurrentUserLocked()) {
                if (this.mSecurityPolicy.canControlMagnification(this)) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
                        if (!magnificationProcessor.resetFullscreenMagnification(displayId, animate)) {
                        }
                        z = true;
                        return z;
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
                return false;
            }
            return false;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x0054, code lost:
        if (r3.isMagnifying(r6) == false) goto L26;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean resetCurrentMagnification(int displayId, boolean animate) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("resetCurrentMagnification", "displayId=" + displayId + ";animate=" + animate);
        }
        synchronized (this.mLock) {
            boolean z = false;
            if (hasRightsToCurrentUserLocked()) {
                if (this.mSecurityPolicy.canControlMagnification(this)) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
                        if (!magnificationProcessor.resetCurrentMagnification(displayId, animate)) {
                        }
                        z = true;
                        return z;
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
                return false;
            }
            return false;
        }
    }

    public boolean setMagnificationConfig(int displayId, MagnificationConfig config, boolean animate) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setMagnificationSpec", "displayId=" + displayId + ", config=" + config.toString());
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                if (this.mSecurityPolicy.canControlMagnification(this)) {
                    long identity = Binder.clearCallingIdentity();
                    MagnificationProcessor magnificationProcessor = this.mSystemSupport.getMagnificationProcessor();
                    boolean magnificationConfig = magnificationProcessor.setMagnificationConfig(displayId, config, animate, this.mId);
                    Binder.restoreCallingIdentity(identity);
                    return magnificationConfig;
                }
                return false;
            }
            return false;
        }
    }

    public void setMagnificationCallbackEnabled(int displayId, boolean enabled) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setMagnificationCallbackEnabled", "displayId=" + displayId + ";enabled=" + enabled);
        }
        this.mInvocationHandler.setMagnificationCallbackEnabled(displayId, enabled);
    }

    public boolean isMagnificationCallbackEnabled(int displayId) {
        return this.mInvocationHandler.isMagnificationCallbackEnabled(displayId);
    }

    public void setSoftKeyboardCallbackEnabled(boolean enabled) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setSoftKeyboardCallbackEnabled", "enabled=" + enabled);
        }
        this.mInvocationHandler.setSoftKeyboardCallbackEnabled(enabled);
    }

    public void takeScreenshot(final int displayId, final RemoteCallback callback) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn(TAKE_SCREENSHOT, "displayId=" + displayId + ";callback=" + callback);
        }
        long currentTimestamp = SystemClock.uptimeMillis();
        long j = this.mRequestTakeScreenshotTimestampMs;
        if (j != 0 && currentTimestamp - j <= 333) {
            sendScreenshotFailure(3, callback);
            return;
        }
        this.mRequestTakeScreenshotTimestampMs = currentTimestamp;
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                sendScreenshotFailure(1, callback);
            } else if (!this.mSecurityPolicy.canTakeScreenshotLocked(this)) {
                throw new SecurityException("Services don't have the capability of taking the screenshot.");
            } else {
                if (!this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                    sendScreenshotFailure(2, callback);
                    return;
                }
                DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
                Display display = displayManager.getDisplay(displayId);
                if (display == null || (display.getType() == 5 && (display.getFlags() & 4) != 0)) {
                    sendScreenshotFailure(4, callback);
                    return;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    this.mMainHandler.post(PooledLambda.obtainRunnable(new Consumer() { // from class: com.android.server.accessibility.AbstractAccessibilityServiceConnection$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            AbstractAccessibilityServiceConnection.this.m591xe24718d8(displayId, callback, obj);
                        }
                    }, (Object) null).recycleOnUse());
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$takeScreenshot$0$com-android-server-accessibility-AbstractAccessibilityServiceConnection  reason: not valid java name */
    public /* synthetic */ void m591xe24718d8(int displayId, RemoteCallback callback, Object nonArg) {
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).userScreenshot(displayId);
        if (screenshotBuffer != null) {
            sendScreenshotSuccess(screenshotBuffer, callback);
        } else {
            sendScreenshotFailure(4, callback);
        }
    }

    private void sendScreenshotSuccess(SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer, RemoteCallback callback) {
        HardwareBuffer hardwareBuffer = screenshotBuffer.getHardwareBuffer();
        ParcelableColorSpace colorSpace = new ParcelableColorSpace(screenshotBuffer.getColorSpace());
        Bundle payload = new Bundle();
        payload.putInt("screenshot_status", 0);
        payload.putParcelable("screenshot_hardwareBuffer", hardwareBuffer);
        payload.putParcelable("screenshot_colorSpace", colorSpace);
        payload.putLong("screenshot_timestamp", SystemClock.uptimeMillis());
        callback.sendResult(payload);
        hardwareBuffer.close();
    }

    private void sendScreenshotFailure(final int errorCode, final RemoteCallback callback) {
        this.mMainHandler.post(PooledLambda.obtainRunnable(new Consumer() { // from class: com.android.server.accessibility.AbstractAccessibilityServiceConnection$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AbstractAccessibilityServiceConnection.lambda$sendScreenshotFailure$1(errorCode, callback, obj);
            }
        }, (Object) null).recycleOnUse());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$sendScreenshotFailure$1(int errorCode, RemoteCallback callback, Object nonArg) {
        Bundle payload = new Bundle();
        payload.putInt("screenshot_status", errorCode);
        callback.sendResult(payload);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            synchronized (this.mLock) {
                pw.append((CharSequence) ("Service[label=" + ((Object) this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(this.mContext.getPackageManager()))));
                pw.append((CharSequence) (", feedbackType" + AccessibilityServiceInfo.feedbackTypeToString(this.mFeedbackType)));
                pw.append((CharSequence) (", capabilities=" + this.mAccessibilityServiceInfo.getCapabilities()));
                pw.append((CharSequence) (", eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                pw.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
                pw.append((CharSequence) (", requestA11yBtn=" + this.mRequestAccessibilityButton));
                pw.append("]");
            }
        }
    }

    public void onAdded() {
        Display[] displays = this.mDisplayManager.getDisplays();
        for (Display display : displays) {
            int displayId = display.getDisplayId();
            onDisplayAdded(displayId);
        }
    }

    public void onDisplayAdded(int displayId) {
        long identity = Binder.clearCallingIdentity();
        try {
            IBinder overlayWindowToken = new Binder();
            if (wmTracingEnabled()) {
                logTraceWM("addWindowToken", overlayWindowToken + ";TYPE_ACCESSIBILITY_OVERLAY;" + displayId + ";null");
            }
            this.mWindowManagerService.addWindowToken(overlayWindowToken, 2032, displayId, null);
            synchronized (this.mLock) {
                this.mOverlayWindowTokens.put(displayId, overlayWindowToken);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onRemoved() {
        Display[] displays = this.mDisplayManager.getDisplays();
        for (Display display : displays) {
            int displayId = display.getDisplayId();
            onDisplayRemoved(displayId);
        }
    }

    public void onDisplayRemoved(int displayId) {
        long identity = Binder.clearCallingIdentity();
        if (wmTracingEnabled()) {
            logTraceWM("addWindowToken", this.mOverlayWindowTokens.get(displayId) + ";true;" + displayId);
        }
        try {
            this.mWindowManagerService.removeWindowToken(this.mOverlayWindowTokens.get(displayId), true, displayId);
            synchronized (this.mLock) {
                this.mOverlayWindowTokens.remove(displayId);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public IBinder getOverlayWindowToken(int displayId) {
        IBinder iBinder;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getOverlayWindowToken", "displayId=" + displayId);
        }
        synchronized (this.mLock) {
            iBinder = this.mOverlayWindowTokens.get(displayId);
        }
        return iBinder;
    }

    public int getWindowIdForLeashToken(IBinder token) {
        int windowIdLocked;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getWindowIdForLeashToken", "token=" + token);
        }
        synchronized (this.mLock) {
            windowIdLocked = this.mA11yWindowManager.getWindowIdLocked(token);
        }
        return windowIdLocked;
    }

    public void resetLocked() {
        this.mSystemSupport.getKeyEventDispatcher().flush(this);
        try {
            if (this.mServiceInterface != null) {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("init", "null, " + this.mId + ", null");
                }
                this.mServiceInterface.init((IAccessibilityServiceConnection) null, this.mId, (IBinder) null);
            }
        } catch (RemoteException e) {
        }
        IBinder iBinder = this.mService;
        if (iBinder != null) {
            try {
                iBinder.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e2) {
                Slog.e(LOG_TAG, "Failed unregistering death link");
            }
            this.mService = null;
        }
        this.mServiceInterface = null;
        this.mReceivedAccessibilityButtonCallbackSinceBind = false;
    }

    public boolean isConnectedLocked() {
        return this.mService != null;
    }

    public void notifyAccessibilityEvent(AccessibilityEvent event) {
        Message message;
        synchronized (this.mLock) {
            int eventType = event.getEventType();
            boolean serviceWantsEvent = wantsEventLocked(event);
            int i = 1;
            boolean requiredForCacheConsistency = this.mUsesAccessibilityCache && (4307005 & eventType) != 0;
            if (serviceWantsEvent || requiredForCacheConsistency) {
                if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                    AccessibilityEvent newEvent = AccessibilityEvent.obtain(event);
                    if (this.mNotificationTimeout > 0 && eventType != 2048) {
                        AccessibilityEvent oldEvent = this.mPendingEvents.get(eventType);
                        this.mPendingEvents.put(eventType, newEvent);
                        if (oldEvent != null) {
                            this.mEventDispatchHandler.removeMessages(eventType);
                            oldEvent.recycle();
                        }
                        message = this.mEventDispatchHandler.obtainMessage(eventType);
                    } else {
                        message = this.mEventDispatchHandler.obtainMessage(eventType, newEvent);
                    }
                    if (!serviceWantsEvent) {
                        i = 0;
                    }
                    message.arg1 = i;
                    this.mEventDispatchHandler.sendMessageDelayed(message, this.mNotificationTimeout);
                }
            }
        }
    }

    private boolean wantsEventLocked(AccessibilityEvent event) {
        if (canReceiveEventsLocked()) {
            if (event.getWindowId() == -1 || event.isImportantForAccessibility() || (this.mFetchFlags & 128) != 0) {
                int eventType = event.getEventType();
                if ((this.mEventTypes & eventType) != eventType) {
                    return false;
                }
                Set<String> packageNames = this.mPackageNames;
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
                return packageNames.isEmpty() || packageNames.contains(packageName);
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAccessibilityEventInternal(int eventType, AccessibilityEvent event, boolean serviceWantsEvent) {
        synchronized (this.mLock) {
            IAccessibilityServiceClient listener = this.mServiceInterface;
            if (listener == null) {
                return;
            }
            if (event == null) {
                event = this.mPendingEvents.get(eventType);
                if (event == null) {
                    return;
                }
                this.mPendingEvents.remove(eventType);
            }
            if (this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                event.setConnectionId(this.mId);
            } else {
                View view = null;
                event.setSource(null);
            }
            event.setSealed(true);
            try {
                try {
                    if (svcClientTracingEnabled()) {
                        logTraceSvcClient("onAccessibilityEvent", event + ";" + serviceWantsEvent);
                    }
                    listener.onAccessibilityEvent(event, serviceWantsEvent);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Error during sending " + event + " to " + listener, re);
                }
            } finally {
                event.recycle();
            }
        }
    }

    public void notifyGesture(AccessibilityGestureEvent gestureEvent) {
        this.mInvocationHandler.obtainMessage(1, gestureEvent).sendToTarget();
    }

    public void notifySystemActionsChangedLocked() {
        this.mInvocationHandler.sendEmptyMessage(9);
    }

    public void notifyClearAccessibilityNodeInfoCache() {
        this.mInvocationHandler.sendEmptyMessage(2);
    }

    public void notifyMagnificationChangedLocked(int displayId, Region region, MagnificationConfig config) {
        this.mInvocationHandler.notifyMagnificationChangedLocked(displayId, region, config);
    }

    public void notifySoftKeyboardShowModeChangedLocked(int showState) {
        this.mInvocationHandler.notifySoftKeyboardShowModeChangedLocked(showState);
    }

    public void notifyAccessibilityButtonClickedLocked(int displayId) {
        this.mInvocationHandler.notifyAccessibilityButtonClickedLocked(displayId);
    }

    public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean available) {
        this.mInvocationHandler.notifyAccessibilityButtonAvailabilityChangedLocked(available);
    }

    public void createImeSessionLocked() {
        this.mInvocationHandler.createImeSessionLocked();
    }

    public void setImeSessionEnabledLocked(IAccessibilityInputMethodSession session, boolean enabled) {
        this.mInvocationHandler.setImeSessionEnabledLocked(session, enabled);
    }

    public void bindInputLocked() {
        this.mInvocationHandler.bindInputLocked();
    }

    public void unbindInputLocked() {
        this.mInvocationHandler.unbindInputLocked();
    }

    public void startInputLocked(IRemoteAccessibilityInputConnection connection, EditorInfo editorInfo, boolean restarting) {
        this.mInvocationHandler.startInputLocked(connection, editorInfo, restarting);
    }

    private Pair<float[], MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(int resolvedWindowId) {
        return this.mSystemSupport.getWindowTransformationMatrixAndMagnificationSpec(resolvedWindowId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyMagnificationChangedInternal(int displayId, Region region, MagnificationConfig config) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onMagnificationChanged", displayId + ", " + region + ", " + config.toString());
                }
                listener.onMagnificationChanged(displayId, region, config);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending magnification changes to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySoftKeyboardShowModeChangedInternal(int showState) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onSoftKeyboardShowModeChanged", String.valueOf(showState));
                }
                listener.onSoftKeyboardShowModeChanged(showState);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending soft keyboard show mode changes to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAccessibilityButtonClickedInternal(int displayId) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onAccessibilityButtonClicked", String.valueOf(displayId));
                }
                listener.onAccessibilityButtonClicked(displayId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending accessibility button click to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAccessibilityButtonAvailabilityChangedInternal(boolean available) {
        if (this.mReceivedAccessibilityButtonCallbackSinceBind && this.mLastAccessibilityButtonCallbackState == available) {
            return;
        }
        this.mReceivedAccessibilityButtonCallbackSinceBind = true;
        this.mLastAccessibilityButtonCallbackState = available;
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onAccessibilityButtonAvailabilityChanged", String.valueOf(available));
                }
                listener.onAccessibilityButtonAvailabilityChanged(available);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending accessibility button availability change to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyGestureInternal(AccessibilityGestureEvent gestureInfo) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onGesture", gestureInfo.toString());
                }
                listener.onGesture(gestureInfo);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error during sending gesture " + gestureInfo + " to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySystemActionsChangedInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onSystemActionsChanged", "");
                }
                listener.onSystemActionsChanged();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending system actions change to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyClearAccessibilityCacheInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("clearAccessibilityCache", "");
                }
                listener.clearAccessibilityCache();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error during requesting accessibility info cache to be cleared.", re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createImeSessionInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("createImeSession", "");
                }
                AccessibilityCallback callback = new AccessibilityCallback();
                listener.createImeSession(callback);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error requesting IME session from " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setImeSessionEnabledInternal(IAccessibilityInputMethodSession session, boolean enabled) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null && session != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("createImeSession", "");
                }
                listener.setImeSessionEnabled(session, enabled);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error requesting IME session from " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindInputInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("bindInput", "");
                }
                listener.bindInput();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error binding input to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindInputInternal() {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("unbindInput", "");
                }
                listener.unbindInput();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error unbinding input to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startInputInternal(IRemoteAccessibilityInputConnection connection, EditorInfo editorInfo, boolean restarting) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("startInput", "editorInfo=" + editorInfo + " restarting=" + restarting);
                }
                listener.startInput(connection, editorInfo, restarting);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error starting input to " + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public IAccessibilityServiceClient getServiceInterfaceSafely() {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        synchronized (this.mLock) {
            iAccessibilityServiceClient = this.mServiceInterface;
        }
        return iAccessibilityServiceClient;
    }

    private int resolveAccessibilityWindowIdLocked(int accessibilityWindowId) {
        if (accessibilityWindowId == Integer.MAX_VALUE) {
            return this.mA11yWindowManager.getActiveWindowId(this.mSystemSupport.getCurrentUserIdLocked());
        }
        return accessibilityWindowId;
    }

    private int resolveAccessibilityWindowIdForFindFocusLocked(int windowId, int focusType) {
        if (windowId == Integer.MAX_VALUE) {
            return this.mA11yWindowManager.getActiveWindowId(this.mSystemSupport.getCurrentUserIdLocked());
        }
        if (windowId == -2) {
            return this.mA11yWindowManager.getFocusedWindowId(focusType);
        }
        return windowId;
    }

    private void ensureWindowsAvailableTimedLocked(int displayId) {
        if (displayId == -1 || this.mA11yWindowManager.getWindowListLocked(displayId) != null) {
            return;
        }
        if (!this.mA11yWindowManager.isTrackingWindowsLocked(displayId)) {
            this.mSystemSupport.onClientChangeLocked(false);
        }
        if (!this.mA11yWindowManager.isTrackingWindowsLocked(displayId)) {
            return;
        }
        long startMillis = SystemClock.uptimeMillis();
        while (this.mA11yWindowManager.getWindowListLocked(displayId) == null) {
            long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
            long remainMillis = 5000 - elapsedMillis;
            if (remainMillis <= 0) {
                return;
            }
            try {
                this.mLock.wait(remainMillis);
            } catch (InterruptedException e) {
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1995=8, 2001=10] */
    /* JADX WARN: Can't wrap try/catch for region: R(20:9|(5:(21:14|(1:118)(2:18|19)|20|21|(3:103|104|(16:109|110|24|25|26|27|(1:97)|(2:95|96)|32|(16:49|50|51|52|53|54|55|56|57|58|59|60|61|62|63|64)(1:34)|35|36|37|38|39|40))|23|24|25|26|27|(1:29)|97|(0)|32|(0)(0)|35|36|37|38|39|40)|37|38|39|40)|119|(1:16)|118|20|21|(0)|23|24|25|26|27|(0)|97|(0)|32|(0)(0)|35|36) */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x016a, code lost:
        r22 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x0160, code lost:
        r0 = th;
     */
    /* JADX WARN: Removed duplicated region for block: B:114:0x003e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:125:0x00a2 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:127:0x0080 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x0075  */
    /* JADX WARN: Removed duplicated region for block: B:89:0x0132  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean performAccessibilityActionInternal(int userId, int resolvedWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int fetchFlags, long interrogatingTid) {
        boolean isA11yFocusAction;
        IBinder activityToken;
        AccessibilityWindowInfo a11yWindowInfo;
        AccessibilityWindowManager.RemoteAccessibilityConnection connection;
        long identityToken;
        boolean z;
        WindowInfo windowInfo;
        synchronized (this.mLock) {
            try {
                AccessibilityWindowManager.RemoteAccessibilityConnection connection2 = this.mA11yWindowManager.getConnectionLocked(userId, resolvedWindowId);
                if (connection2 == null) {
                    return false;
                }
                try {
                    try {
                        if (action != 64 && action != 128) {
                            isA11yFocusAction = false;
                            if (!isA11yFocusAction || (windowInfo = this.mA11yWindowManager.findWindowInfoByIdLocked(resolvedWindowId)) == null) {
                                activityToken = null;
                            } else {
                                IBinder activityToken2 = windowInfo.activityToken;
                                activityToken = activityToken2;
                            }
                            a11yWindowInfo = this.mA11yWindowManager.findA11yWindowInfoByIdLocked(resolvedWindowId);
                            if (a11yWindowInfo != null) {
                                try {
                                    if (a11yWindowInfo.isInPictureInPictureMode() && this.mA11yWindowManager.getPictureInPictureActionReplacingConnection() != null && !isA11yFocusAction) {
                                        connection = this.mA11yWindowManager.getPictureInPictureActionReplacingConnection();
                                        int interrogatingPid = Binder.getCallingPid();
                                        identityToken = Binder.clearCallingIdentity();
                                        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                                        if (action != 16 || action == 32) {
                                            this.mA11yWindowManager.notifyOutsideTouch(userId, resolvedWindowId);
                                        }
                                        if (activityToken != null) {
                                            try {
                                                ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).setFocusedActivity(activityToken);
                                            } catch (RemoteException e) {
                                                z = false;
                                                Binder.restoreCallingIdentity(identityToken);
                                                return z;
                                            } catch (Throwable th) {
                                                th = th;
                                                Binder.restoreCallingIdentity(identityToken);
                                                throw th;
                                            }
                                        }
                                        if (intConnTracingEnabled()) {
                                            try {
                                            } catch (RemoteException e2) {
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                            try {
                                                try {
                                                    try {
                                                        try {
                                                            logTraceIntConn("performAccessibilityAction", accessibilityNodeId + ";" + action + ";" + arguments + ";" + interactionId + ";" + callback + ";" + this.mFetchFlags + ";" + interrogatingPid + ";" + interrogatingTid);
                                                        } catch (RemoteException e3) {
                                                            z = false;
                                                            Binder.restoreCallingIdentity(identityToken);
                                                            return z;
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                            Binder.restoreCallingIdentity(identityToken);
                                                            throw th;
                                                        }
                                                    } catch (RemoteException e4) {
                                                        z = false;
                                                        Binder.restoreCallingIdentity(identityToken);
                                                        return z;
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        Binder.restoreCallingIdentity(identityToken);
                                                        throw th;
                                                    }
                                                } catch (RemoteException e5) {
                                                    z = false;
                                                    Binder.restoreCallingIdentity(identityToken);
                                                    return z;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    Binder.restoreCallingIdentity(identityToken);
                                                    throw th;
                                                }
                                            } catch (RemoteException e6) {
                                                z = false;
                                                Binder.restoreCallingIdentity(identityToken);
                                                return z;
                                            } catch (Throwable th6) {
                                                th = th6;
                                                Binder.restoreCallingIdentity(identityToken);
                                                throw th;
                                            }
                                        }
                                        z = false;
                                        connection.getRemote().performAccessibilityAction(accessibilityNodeId, action, arguments, interactionId, callback, fetchFlags, interrogatingPid, interrogatingTid);
                                        Binder.restoreCallingIdentity(identityToken);
                                        return true;
                                    }
                                } catch (Throwable th7) {
                                    re = th7;
                                    throw re;
                                }
                            }
                            connection = connection2;
                            int interrogatingPid2 = Binder.getCallingPid();
                            identityToken = Binder.clearCallingIdentity();
                            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                            if (action != 16) {
                            }
                            this.mA11yWindowManager.notifyOutsideTouch(userId, resolvedWindowId);
                            if (activityToken != null) {
                            }
                            if (intConnTracingEnabled()) {
                            }
                            z = false;
                            connection.getRemote().performAccessibilityAction(accessibilityNodeId, action, arguments, interactionId, callback, fetchFlags, interrogatingPid2, interrogatingTid);
                            Binder.restoreCallingIdentity(identityToken);
                            return true;
                        }
                        connection.getRemote().performAccessibilityAction(accessibilityNodeId, action, arguments, interactionId, callback, fetchFlags, interrogatingPid2, interrogatingTid);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e7) {
                        Binder.restoreCallingIdentity(identityToken);
                        return z;
                    } catch (Throwable th8) {
                        th = th8;
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                    a11yWindowInfo = this.mA11yWindowManager.findA11yWindowInfoByIdLocked(resolvedWindowId);
                    if (a11yWindowInfo != null) {
                    }
                    connection = connection2;
                    int interrogatingPid22 = Binder.getCallingPid();
                    identityToken = Binder.clearCallingIdentity();
                    this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                    if (action != 16) {
                    }
                    this.mA11yWindowManager.notifyOutsideTouch(userId, resolvedWindowId);
                    if (activityToken != null) {
                    }
                    if (intConnTracingEnabled()) {
                    }
                    z = false;
                } catch (Throwable th9) {
                    re = th9;
                }
                isA11yFocusAction = true;
                if (isA11yFocusAction) {
                }
                activityToken = null;
            } catch (Throwable th10) {
                re = th10;
            }
        }
    }

    private IAccessibilityInteractionConnectionCallback replaceCallbackIfNeeded(IAccessibilityInteractionConnectionCallback originalCallback, int resolvedWindowId, int interactionId, int interrogatingPid, long interrogatingTid) {
        AccessibilityWindowManager.RemoteAccessibilityConnection pipActionReplacingConnection = this.mA11yWindowManager.getPictureInPictureActionReplacingConnection();
        synchronized (this.mLock) {
            try {
                try {
                    AccessibilityWindowInfo windowInfo = this.mA11yWindowManager.findA11yWindowInfoByIdLocked(resolvedWindowId);
                    if (windowInfo != null && windowInfo.isInPictureInPictureMode() && pipActionReplacingConnection != null) {
                        return new ActionReplacingCallback(originalCallback, pipActionReplacingConnection.getRemote(), interactionId, interrogatingPid, interrogatingTid);
                    }
                    return originalCallback;
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

    private List<AccessibilityWindowInfo> getWindowsByDisplayLocked(int displayId) {
        List<AccessibilityWindowInfo> internalWindowList = this.mA11yWindowManager.getWindowListLocked(displayId);
        if (internalWindowList == null) {
            return null;
        }
        List<AccessibilityWindowInfo> returnedWindowList = new ArrayList<>();
        int windowCount = internalWindowList.size();
        for (int i = 0; i < windowCount; i++) {
            AccessibilityWindowInfo window = internalWindowList.get(i);
            AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
            windowClone.setConnectionId(this.mId);
            returnedWindowList.add(windowClone);
        }
        return returnedWindowList;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class InvocationHandler extends Handler {
        private static final int MSG_BIND_INPUT = 12;
        public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 2;
        private static final int MSG_CREATE_IME_SESSION = 10;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_AVAILABILITY_CHANGED = 8;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_CLICKED = 7;
        public static final int MSG_ON_GESTURE = 1;
        private static final int MSG_ON_MAGNIFICATION_CHANGED = 5;
        private static final int MSG_ON_SOFT_KEYBOARD_STATE_CHANGED = 6;
        private static final int MSG_ON_SYSTEM_ACTIONS_CHANGED = 9;
        private static final int MSG_SET_IME_SESSION_ENABLED = 11;
        private static final int MSG_START_INPUT = 14;
        private static final int MSG_UNBIND_INPUT = 13;
        private boolean mIsSoftKeyboardCallbackEnabled;
        private final SparseArray<Boolean> mMagnificationCallbackState;

        public InvocationHandler(Looper looper) {
            super(looper, null, true);
            this.mMagnificationCallbackState = new SparseArray<>(0);
            this.mIsSoftKeyboardCallbackEnabled = false;
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            boolean restarting;
            int type = message.what;
            switch (type) {
                case 1:
                    AbstractAccessibilityServiceConnection.this.notifyGestureInternal((AccessibilityGestureEvent) message.obj);
                    return;
                case 2:
                    AbstractAccessibilityServiceConnection.this.notifyClearAccessibilityCacheInternal();
                    return;
                case 3:
                case 4:
                default:
                    throw new IllegalArgumentException("Unknown message: " + type);
                case 5:
                    SomeArgs args = (SomeArgs) message.obj;
                    Region region = (Region) args.arg1;
                    MagnificationConfig config = (MagnificationConfig) args.arg2;
                    int displayId = args.argi1;
                    AbstractAccessibilityServiceConnection.this.notifyMagnificationChangedInternal(displayId, region, config);
                    args.recycle();
                    return;
                case 6:
                    int showState = message.arg1;
                    AbstractAccessibilityServiceConnection.this.notifySoftKeyboardShowModeChangedInternal(showState);
                    return;
                case 7:
                    int displayId2 = message.arg1;
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonClickedInternal(displayId2);
                    return;
                case 8:
                    restarting = message.arg1 != 0;
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonAvailabilityChangedInternal(restarting);
                    return;
                case 9:
                    AbstractAccessibilityServiceConnection.this.notifySystemActionsChangedInternal();
                    return;
                case 10:
                    AbstractAccessibilityServiceConnection.this.createImeSessionInternal();
                    return;
                case 11:
                    restarting = message.arg1 != 0;
                    IAccessibilityInputMethodSession session = (IAccessibilityInputMethodSession) message.obj;
                    AbstractAccessibilityServiceConnection.this.setImeSessionEnabledInternal(session, restarting);
                    return;
                case 12:
                    AbstractAccessibilityServiceConnection.this.bindInputInternal();
                    return;
                case 13:
                    AbstractAccessibilityServiceConnection.this.unbindInputInternal();
                    return;
                case 14:
                    restarting = message.arg1 != 0;
                    SomeArgs args2 = (SomeArgs) message.obj;
                    IRemoteAccessibilityInputConnection connection = (IRemoteAccessibilityInputConnection) args2.arg1;
                    EditorInfo editorInfo = (EditorInfo) args2.arg2;
                    AbstractAccessibilityServiceConnection.this.startInputInternal(connection, editorInfo, restarting);
                    args2.recycle();
                    return;
            }
        }

        public void notifyMagnificationChangedLocked(int displayId, Region region, MagnificationConfig config) {
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                if (this.mMagnificationCallbackState.get(displayId) == null) {
                    return;
                }
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = region;
                args.arg2 = config;
                args.argi1 = displayId;
                Message msg = obtainMessage(5, args);
                msg.sendToTarget();
            }
        }

        public void setMagnificationCallbackEnabled(int displayId, boolean enabled) {
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                if (enabled) {
                    this.mMagnificationCallbackState.put(displayId, true);
                } else {
                    this.mMagnificationCallbackState.remove(displayId);
                }
            }
        }

        public boolean isMagnificationCallbackEnabled(int displayId) {
            boolean z;
            synchronized (AbstractAccessibilityServiceConnection.this.mLock) {
                z = this.mMagnificationCallbackState.get(displayId) != null;
            }
            return z;
        }

        public void notifySoftKeyboardShowModeChangedLocked(int showState) {
            if (!this.mIsSoftKeyboardCallbackEnabled) {
                return;
            }
            Message msg = obtainMessage(6, showState, 0);
            msg.sendToTarget();
        }

        public void setSoftKeyboardCallbackEnabled(boolean enabled) {
            this.mIsSoftKeyboardCallbackEnabled = enabled;
        }

        public void notifyAccessibilityButtonClickedLocked(int displayId) {
            Message msg = obtainMessage(7, displayId, 0);
            msg.sendToTarget();
        }

        public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean available) {
            Message msg = obtainMessage(8, available ? 1 : 0, 0);
            msg.sendToTarget();
        }

        public void createImeSessionLocked() {
            Message msg = obtainMessage(10);
            msg.sendToTarget();
        }

        public void setImeSessionEnabledLocked(IAccessibilityInputMethodSession session, boolean enabled) {
            Message msg = obtainMessage(11, enabled ? 1 : 0, 0, session);
            msg.sendToTarget();
        }

        public void bindInputLocked() {
            Message msg = obtainMessage(12);
            msg.sendToTarget();
        }

        public void unbindInputLocked() {
            Message msg = obtainMessage(13);
            msg.sendToTarget();
        }

        public void startInputLocked(IRemoteAccessibilityInputConnection connection, EditorInfo editorInfo, boolean restarting) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = connection;
            args.arg2 = editorInfo;
            Message msg = obtainMessage(14, restarting ? 1 : 0, 0, args);
            msg.sendToTarget();
        }
    }

    public boolean isServiceHandlesDoubleTapEnabled() {
        return this.mServiceHandlesDoubleTap;
    }

    public boolean isMultiFingerGesturesEnabled() {
        return this.mRequestMultiFingerGestures;
    }

    public boolean isTwoFingerPassthroughEnabled() {
        return this.mRequestTwoFingerPassthrough;
    }

    public boolean isSendMotionEventsEnabled() {
        return this.mSendMotionEvents;
    }

    public void setGestureDetectionPassthroughRegion(int displayId, Region region) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setGestureDetectionPassthroughRegion", "displayId=" + displayId + ";region=" + region);
        }
        this.mSystemSupport.setGestureDetectionPassthroughRegion(displayId, region);
    }

    public void setTouchExplorationPassthroughRegion(int displayId, Region region) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setTouchExplorationPassthroughRegion", "displayId=" + displayId + ";region=" + region);
        }
        this.mSystemSupport.setTouchExplorationPassthroughRegion(displayId, region);
    }

    public void setFocusAppearance(int strokeWidth, int color) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setFocusAppearance", "strokeWidth=" + strokeWidth + ";color=" + color);
        }
    }

    public void setCacheEnabled(boolean enabled) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setCacheEnabled", "enabled=" + enabled);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                this.mUsesAccessibilityCache = enabled;
                this.mSystemSupport.onClientChangeLocked(true);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void logTrace(long timestamp, String where, long loggingTypes, String callingParams, int processId, long threadId, int callingUid, Bundle callingStack) {
        if (this.mTrace.isA11yTracingEnabledForTypes(loggingTypes)) {
            ArrayList<StackTraceElement> list = (ArrayList) callingStack.getSerializable("call_stack");
            HashSet<String> ignoreList = (HashSet) callingStack.getSerializable("ignore_call_stack");
            this.mTrace.logTrace(timestamp, where, loggingTypes, callingParams, processId, threadId, callingUid, (StackTraceElement[]) list.toArray(new StackTraceElement[list.size()]), ignoreList);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean svcClientTracingEnabled() {
        return this.mTrace.isA11yTracingEnabledForTypes(2L);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void logTraceSvcClient(String methodName, String params) {
        this.mTrace.logTrace("AbstractAccessibilityServiceConnection.IAccessibilityServiceClient." + methodName, 2L, params);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean svcConnTracingEnabled() {
        return this.mTrace.isA11yTracingEnabledForTypes(1L);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void logTraceSvcConn(String methodName, String params) {
        this.mTrace.logTrace("AbstractAccessibilityServiceConnection.IAccessibilityServiceConnection." + methodName, 1L, params);
    }

    protected boolean intConnTracingEnabled() {
        return this.mTrace.isA11yTracingEnabledForTypes(16L);
    }

    protected void logTraceIntConn(String methodName, String params) {
        this.mTrace.logTrace("AbstractAccessibilityServiceConnection." + methodName, 16L, params);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean wmTracingEnabled() {
        return this.mTrace.isA11yTracingEnabledForTypes(512L);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void logTraceWM(String methodName, String params) {
        this.mTrace.logTrace("WindowManagerInternal." + methodName, 512L, params);
    }

    public void setServiceDetectsGesturesEnabled(int displayId, boolean mode) {
        this.mSystemSupport.setServiceDetectsGesturesEnabled(displayId, mode);
    }

    public void requestTouchExploration(int displayId) {
        this.mSystemSupport.requestTouchExploration(displayId);
    }

    public void requestDragging(int displayId, int pointerId) {
        this.mSystemSupport.requestDragging(displayId, pointerId);
    }

    public void requestDelegating(int displayId) {
        this.mSystemSupport.requestDelegating(displayId);
    }

    public void onDoubleTap(int displayId) {
        this.mSystemSupport.onDoubleTap(displayId);
    }

    public void onDoubleTapAndHold(int displayId) {
        this.mSystemSupport.onDoubleTapAndHold(displayId);
    }

    public void setAnimationScale(float scale) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Global.putFloat(this.mContext.getContentResolver(), "window_animation_scale", scale);
            Settings.Global.putFloat(this.mContext.getContentResolver(), "transition_animation_scale", scale);
            Settings.Global.putFloat(this.mContext.getContentResolver(), "animator_duration_scale", scale);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AccessibilityCallback extends IAccessibilityInputMethodSessionCallback.Stub {
        private AccessibilityCallback() {
        }

        public void sessionCreated(IAccessibilityInputMethodSession session, int id) {
            Trace.traceBegin(32L, "AACS.sessionCreated");
            long ident = Binder.clearCallingIdentity();
            try {
                InputMethodManagerInternal.get().onSessionForAccessibilityCreated(id, session);
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }
}
