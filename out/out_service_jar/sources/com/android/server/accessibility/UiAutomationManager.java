package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.UiAutomationManager;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class UiAutomationManager {
    private static final ComponentName COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "UiAutomation");
    private static final String LOG_TAG = "UiAutomationManager";
    private final Object mLock;
    private AbstractAccessibilityServiceConnection.SystemSupport mSystemSupport;
    private AccessibilityTrace mTrace;
    private int mUiAutomationFlags;
    private UiAutomationService mUiAutomationService;
    private AccessibilityServiceInfo mUiAutomationServiceInfo;
    private IBinder mUiAutomationServiceOwner;
    private final IBinder.DeathRecipient mUiAutomationServiceOwnerDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.accessibility.UiAutomationManager.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            UiAutomationManager.this.mUiAutomationServiceOwner.unlinkToDeath(this, 0);
            UiAutomationManager.this.mUiAutomationServiceOwner = null;
            UiAutomationManager.this.destroyUiAutomationService();
            Slog.v(UiAutomationManager.LOG_TAG, "UiAutomation service owner died");
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public UiAutomationManager(Object lock) {
        this.mLock = lock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerUiTestAutomationServiceLocked(IBinder owner, IAccessibilityServiceClient serviceClient, Context context, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, AccessibilitySecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, AccessibilityTrace trace, WindowManagerInternal windowManagerInternal, SystemActionPerformer systemActionPerformer, AccessibilityWindowManager awm, int flags) {
        Object obj;
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    accessibilityServiceInfo.setComponentName(COMPONENT_NAME);
                    if (this.mUiAutomationService != null) {
                        throw new IllegalStateException("UiAutomationService " + this.mUiAutomationService.mServiceInterface + "already registered!");
                    }
                    try {
                        owner.linkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
                        this.mUiAutomationFlags = flags;
                        this.mSystemSupport = systemSupport;
                        this.mTrace = trace;
                        if (useAccessibility()) {
                            obj = obj2;
                            try {
                                UiAutomationService uiAutomationService = new UiAutomationService(context, accessibilityServiceInfo, id, mainHandler, this.mLock, securityPolicy, systemSupport, trace, windowManagerInternal, systemActionPerformer, awm);
                                this.mUiAutomationService = uiAutomationService;
                                this.mUiAutomationServiceOwner = owner;
                                try {
                                    this.mUiAutomationServiceInfo = accessibilityServiceInfo;
                                    uiAutomationService.mServiceInterface = serviceClient;
                                    try {
                                        this.mUiAutomationService.mServiceInterface.asBinder().linkToDeath(this.mUiAutomationService, 0);
                                        this.mUiAutomationService.onAdded();
                                        this.mUiAutomationService.connectServiceUnknownThread();
                                    } catch (RemoteException re) {
                                        Slog.e(LOG_TAG, "Failed registering death link: " + re);
                                        destroyUiAutomationService();
                                    }
                                } catch (Throwable th) {
                                    re = th;
                                    throw re;
                                }
                            } catch (Throwable th2) {
                                re = th2;
                            }
                        }
                    } catch (RemoteException re2) {
                        Slog.e(LOG_TAG, "Couldn't register for the death of a UiTestAutomationService!", re2);
                    }
                } catch (Throwable th3) {
                    re = th3;
                    obj = obj2;
                }
            } catch (Throwable th4) {
                re = th4;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterUiTestAutomationServiceLocked(IAccessibilityServiceClient serviceClient) {
        UiAutomationService uiAutomationService;
        synchronized (this.mLock) {
            if (useAccessibility() && ((uiAutomationService = this.mUiAutomationService) == null || serviceClient == null || uiAutomationService.mServiceInterface == null || serviceClient.asBinder() != this.mUiAutomationService.mServiceInterface.asBinder())) {
                throw new IllegalStateException("UiAutomationService " + serviceClient + " not registered!");
            }
            destroyUiAutomationService();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendAccessibilityEventLocked(AccessibilityEvent event) {
        UiAutomationService uiAutomationService = this.mUiAutomationService;
        if (uiAutomationService != null) {
            uiAutomationService.notifyAccessibilityEvent(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUiAutomationRunningLocked() {
        return (this.mUiAutomationService == null && useAccessibility()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean suppressingAccessibilityServicesLocked() {
        return !(this.mUiAutomationService == null && useAccessibility()) && (this.mUiAutomationFlags & 1) == 0;
    }

    boolean useAccessibility() {
        return (this.mUiAutomationFlags & 2) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTouchExplorationEnabledLocked() {
        UiAutomationService uiAutomationService = this.mUiAutomationService;
        return uiAutomationService != null && uiAutomationService.mRequestTouchExplorationMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canRetrieveInteractiveWindowsLocked() {
        UiAutomationService uiAutomationService = this.mUiAutomationService;
        return uiAutomationService != null && uiAutomationService.mRetrieveInteractiveWindows;
    }

    int getRequestedEventMaskLocked() {
        UiAutomationService uiAutomationService = this.mUiAutomationService;
        if (uiAutomationService == null) {
            return 0;
        }
        return uiAutomationService.mEventTypes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRelevantEventTypes() {
        UiAutomationService uiAutomationService;
        synchronized (this.mLock) {
            uiAutomationService = this.mUiAutomationService;
        }
        if (uiAutomationService == null) {
            return 0;
        }
        return uiAutomationService.getRelevantEventTypes();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityServiceInfo getServiceInfo() {
        UiAutomationService uiAutomationService;
        synchronized (this.mLock) {
            uiAutomationService = this.mUiAutomationService;
        }
        if (uiAutomationService == null) {
            return null;
        }
        return uiAutomationService.getServiceInfo();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpUiAutomationService(FileDescriptor fd, PrintWriter pw, String[] args) {
        UiAutomationService uiAutomationService;
        synchronized (this.mLock) {
            uiAutomationService = this.mUiAutomationService;
        }
        if (uiAutomationService != null) {
            uiAutomationService.dump(fd, pw, args);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyUiAutomationService() {
        synchronized (this.mLock) {
            UiAutomationService uiAutomationService = this.mUiAutomationService;
            if (uiAutomationService != null) {
                uiAutomationService.mServiceInterface.asBinder().unlinkToDeath(this.mUiAutomationService, 0);
                this.mUiAutomationService.onRemoved();
                this.mUiAutomationService.resetLocked();
                this.mUiAutomationService = null;
                IBinder iBinder = this.mUiAutomationServiceOwner;
                if (iBinder != null) {
                    iBinder.unlinkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
                    this.mUiAutomationServiceOwner = null;
                }
            }
            this.mUiAutomationFlags = 0;
            this.mSystemSupport.onClientChangeLocked(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UiAutomationService extends AbstractAccessibilityServiceConnection {
        private final Handler mMainHandler;

        UiAutomationService(Context context, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, AccessibilitySecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, AccessibilityTrace trace, WindowManagerInternal windowManagerInternal, SystemActionPerformer systemActionPerformer, AccessibilityWindowManager awm) {
            super(context, UiAutomationManager.COMPONENT_NAME, accessibilityServiceInfo, id, mainHandler, lock, securityPolicy, systemSupport, trace, windowManagerInternal, systemActionPerformer, awm);
            this.mMainHandler = mainHandler;
        }

        void connectServiceUnknownThread() {
            this.mMainHandler.post(new Runnable() { // from class: com.android.server.accessibility.UiAutomationManager$UiAutomationService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UiAutomationManager.UiAutomationService.this.m689xc0e62175();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$connectServiceUnknownThread$0$com-android-server-accessibility-UiAutomationManager$UiAutomationService  reason: not valid java name */
        public /* synthetic */ void m689xc0e62175() {
            IAccessibilityServiceClient serviceInterface;
            try {
                synchronized (this.mLock) {
                    serviceInterface = this.mServiceInterface;
                    if (serviceInterface == null) {
                        this.mService = null;
                    } else {
                        this.mService = this.mServiceInterface.asBinder();
                        this.mService.linkToDeath(this, 0);
                    }
                }
                if (serviceInterface != null) {
                    if (this.mTrace.isA11yTracingEnabledForTypes(2L)) {
                        this.mTrace.logTrace("UiAutomationService.connectServiceUnknownThread", 2L, "serviceConnection=" + this + ";connectionId=" + this.mId + "windowToken=" + this.mOverlayWindowTokens.get(0));
                    }
                    serviceInterface.init(this, this.mId, this.mOverlayWindowTokens.get(0));
                }
            } catch (RemoteException re) {
                Slog.w(UiAutomationManager.LOG_TAG, "Error initialized connection", re);
                UiAutomationManager.this.destroyUiAutomationService();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            UiAutomationManager.this.destroyUiAutomationService();
        }

        @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
        protected boolean hasRightsToCurrentUserLocked() {
            return true;
        }

        @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
        protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo info) {
            return true;
        }

        @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(this.mContext, UiAutomationManager.LOG_TAG, pw)) {
                synchronized (this.mLock) {
                    pw.append((CharSequence) ("Ui Automation[eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                    pw.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
                    pw.append("]");
                }
            }
        }

        public boolean setSoftKeyboardShowMode(int mode) {
            return false;
        }

        public int getSoftKeyboardShowMode() {
            return 0;
        }

        public boolean switchToInputMethod(String imeId) {
            return false;
        }

        public int setInputMethodEnabled(String imeId, boolean enabled) {
            return 2;
        }

        public boolean isAccessibilityButtonAvailable() {
            return false;
        }

        public void disableSelf() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
        }

        @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
        public boolean isCapturingFingerprintGestures() {
            return false;
        }

        @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
        public void onFingerprintGestureDetectionActiveChanged(boolean active) {
        }

        @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
        public void onFingerprintGesture(int gesture) {
        }

        @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
        public void takeScreenshot(int displayId, RemoteCallback callback) {
        }
    }
}
