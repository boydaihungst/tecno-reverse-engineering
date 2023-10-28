package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.TouchInteractionController;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AccessibilityServiceConnection extends AbstractAccessibilityServiceConnection {
    private static final String LOG_TAG = "AccessibilityServiceConnection";
    final ActivityTaskManagerInternal mActivityTaskManagerService;
    final Intent mIntent;
    private final Handler mMainHandler;
    final WeakReference<AccessibilityUserState> mUserStateWeakReference;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityServiceConnection(AccessibilityUserState userState, Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int id, Handler mainHandler, Object lock, AccessibilitySecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, AccessibilityTrace trace, WindowManagerInternal windowManagerInternal, SystemActionPerformer systemActionPerfomer, AccessibilityWindowManager awm, ActivityTaskManagerInternal activityTaskManagerService) {
        super(context, componentName, accessibilityServiceInfo, id, mainHandler, lock, securityPolicy, systemSupport, trace, windowManagerInternal, systemActionPerfomer, awm);
        this.mUserStateWeakReference = new WeakReference<>(userState);
        Intent component = new Intent().setComponent(this.mComponentName);
        this.mIntent = component;
        this.mMainHandler = mainHandler;
        component.putExtra("android.intent.extra.client_label", 17039570);
        this.mActivityTaskManagerService = activityTaskManagerService;
        long identity = Binder.clearCallingIdentity();
        try {
            component.putExtra("android.intent.extra.client_intent", this.mSystemSupport.getPendingIntentActivity(this.mContext, 0, new Intent("android.settings.ACCESSIBILITY_SETTINGS"), 67108864));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void bindLocked() {
        AccessibilityUserState userState = this.mUserStateWeakReference.get();
        if (userState == null) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = userState.getBindInstantServiceAllowedLocked() ? 34607105 | 4194304 : 34607105;
            if (this.mService == null && this.mContext.bindServiceAsUser(this.mIntent, this, flags, new UserHandle(userState.mUserId))) {
                userState.getBindingServicesLocked().add(this.mComponentName);
            }
            Binder.restoreCallingIdentity(identity);
            this.mActivityTaskManagerService.setAllowAppSwitches(this.mComponentName.flattenToString(), this.mAccessibilityServiceInfo.getResolveInfo().serviceInfo.applicationInfo.uid, userState.mUserId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void unbindLocked() {
        if (requestImeApis()) {
            this.mSystemSupport.unbindImeLocked(this);
        }
        this.mContext.unbindService(this);
        AccessibilityUserState userState = this.mUserStateWeakReference.get();
        if (userState == null) {
            return;
        }
        userState.removeServiceLocked(this);
        this.mSystemSupport.getMagnificationProcessor().resetAllIfNeeded(this.mId);
        this.mActivityTaskManagerService.setAllowAppSwitches(this.mComponentName.flattenToString(), -1, userState.mUserId);
        resetLocked();
    }

    public boolean canRetrieveInteractiveWindowsLocked() {
        return this.mSecurityPolicy.canRetrieveWindowContentLocked(this) && this.mRetrieveInteractiveWindows;
    }

    public void disableSelf() {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("disableSelf", "");
        }
        synchronized (this.mLock) {
            AccessibilityUserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            if (userState.getEnabledServicesLocked().remove(this.mComponentName)) {
                long identity = Binder.clearCallingIdentity();
                this.mSystemSupport.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.getEnabledServicesLocked(), userState.mUserId);
                Binder.restoreCallingIdentity(identity);
                this.mSystemSupport.onClientChangeLocked(false);
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        synchronized (this.mLock) {
            if (this.mService != service) {
                if (this.mService != null) {
                    this.mService.unlinkToDeath(this, 0);
                }
                this.mService = service;
                try {
                    this.mService.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed registering death link");
                    this.mService = null;
                    binderDied();
                    return;
                }
            }
            this.mServiceInterface = IAccessibilityServiceClient.Stub.asInterface(service);
            AccessibilityUserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            userState.addServiceLocked(this);
            this.mSystemSupport.onClientChangeLocked(false);
            this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.accessibility.AccessibilityServiceConnection$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AccessibilityServiceConnection) obj).initializeService();
                }
            }, this));
            if (requestImeApis()) {
                this.mSystemSupport.requestImeLocked(this);
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
    public AccessibilityServiceInfo getServiceInfo() {
        return this.mAccessibilityServiceInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initializeService() {
        IAccessibilityServiceClient serviceInterface = null;
        synchronized (this.mLock) {
            AccessibilityUserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            Set<ComponentName> bindingServices = userState.getBindingServicesLocked();
            Set<ComponentName> crashedServices = userState.getCrashedServicesLocked();
            if (bindingServices.contains(this.mComponentName) || crashedServices.contains(this.mComponentName)) {
                bindingServices.remove(this.mComponentName);
                crashedServices.remove(this.mComponentName);
                this.mAccessibilityServiceInfo.crashed = false;
                serviceInterface = this.mServiceInterface;
            }
            if (serviceInterface != null && !userState.getEnabledServicesLocked().contains(this.mComponentName)) {
                this.mSystemSupport.onClientChangeLocked(false);
            } else if (serviceInterface == null) {
                binderDied();
            } else {
                try {
                    if (svcClientTracingEnabled()) {
                        logTraceSvcClient("init", this + "," + this.mId + "," + this.mOverlayWindowTokens.get(0));
                    }
                    serviceInterface.init(this, this.mId, this.mOverlayWindowTokens.get(0));
                } catch (RemoteException re) {
                    Slog.w(LOG_TAG, "Error while setting connection for service: " + serviceInterface, re);
                    binderDied();
                }
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName componentName) {
        binderDied();
        AccessibilityUserState userState = this.mUserStateWeakReference.get();
        if (userState != null) {
            this.mActivityTaskManagerService.setAllowAppSwitches(this.mComponentName.flattenToString(), -1, userState.mUserId);
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
    protected boolean hasRightsToCurrentUserLocked() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || callingUid == 2000 || this.mSecurityPolicy.resolveProfileParentLocked(UserHandle.getUserId(callingUid)) == this.mSystemSupport.getCurrentUserIdLocked() || this.mSecurityPolicy.hasPermission("android.permission.INTERACT_ACROSS_USERS") || this.mSecurityPolicy.hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL")) {
            return true;
        }
        return false;
    }

    public boolean setSoftKeyboardShowMode(int showMode) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("setSoftKeyboardShowMode", "showMode=" + showMode);
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                AccessibilityUserState userState = this.mUserStateWeakReference.get();
                if (userState == null) {
                    return false;
                }
                return userState.setSoftKeyboardModeLocked(showMode, this.mComponentName);
            }
            return false;
        }
    }

    public int getSoftKeyboardShowMode() {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("getSoftKeyboardShowMode", "");
        }
        AccessibilityUserState userState = this.mUserStateWeakReference.get();
        if (userState != null) {
            return userState.getSoftKeyboardShowModeLocked();
        }
        return 0;
    }

    public boolean switchToInputMethod(String imeId) {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("switchToInputMethod", "imeId=" + imeId);
        }
        synchronized (this.mLock) {
            if (!hasRightsToCurrentUserLocked()) {
                return false;
            }
            int callingUserId = UserHandle.getCallingUserId();
            long identity = Binder.clearCallingIdentity();
            try {
                boolean result = InputMethodManagerInternal.get().switchToInputMethod(imeId, callingUserId);
                return result;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [355=4] */
    public int setInputMethodEnabled(String imeId, boolean enabled) throws SecurityException {
        int checkResult;
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("switchToInputMethod", "imeId=" + imeId);
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                int callingUserId = UserHandle.getCallingUserId();
                InputMethodManagerInternal inputMethodManagerInternal = InputMethodManagerInternal.get();
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mLock) {
                        checkResult = this.mSecurityPolicy.canEnableDisableInputMethod(imeId, this);
                    }
                    if (checkResult != 0) {
                        return checkResult;
                    }
                    if (inputMethodManagerInternal.setInputMethodEnabled(imeId, enabled, callingUserId)) {
                        return 0;
                    }
                    return 2;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return 2;
        }
    }

    public boolean isAccessibilityButtonAvailable() {
        if (svcConnTracingEnabled()) {
            logTraceSvcConn("isAccessibilityButtonAvailable", "");
        }
        synchronized (this.mLock) {
            boolean z = false;
            if (hasRightsToCurrentUserLocked()) {
                AccessibilityUserState userState = this.mUserStateWeakReference.get();
                if (userState != null && isAccessibilityButtonAvailableLocked(userState)) {
                    z = true;
                }
                return z;
            }
            return false;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        synchronized (this.mLock) {
            if (isConnectedLocked()) {
                if (requestImeApis()) {
                    this.mSystemSupport.unbindImeLocked(this);
                }
                this.mAccessibilityServiceInfo.crashed = true;
                AccessibilityUserState userState = this.mUserStateWeakReference.get();
                if (userState != null) {
                    userState.serviceDisconnectedLocked(this);
                }
                resetLocked();
                this.mSystemSupport.getMagnificationProcessor().resetAllIfNeeded(this.mId);
                this.mSystemSupport.onClientChangeLocked(false);
            }
        }
    }

    public boolean isAccessibilityButtonAvailableLocked(AccessibilityUserState userState) {
        return this.mRequestAccessibilityButton && this.mSystemSupport.isAccessibilityButtonShown();
    }

    @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
    public boolean isCapturingFingerprintGestures() {
        return this.mServiceInterface != null && this.mSecurityPolicy.canCaptureFingerprintGestures(this) && this.mCaptureFingerprintGestures;
    }

    @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
    public void onFingerprintGestureDetectionActiveChanged(boolean active) {
        IAccessibilityServiceClient serviceInterface;
        if (!isCapturingFingerprintGestures()) {
            return;
        }
        synchronized (this.mLock) {
            serviceInterface = this.mServiceInterface;
        }
        if (serviceInterface != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onFingerprintCapturingGesturesChanged", String.valueOf(active));
                }
                this.mServiceInterface.onFingerprintCapturingGesturesChanged(active);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // com.android.server.accessibility.FingerprintGestureDispatcher.FingerprintGestureClient
    public void onFingerprintGesture(int gesture) {
        IAccessibilityServiceClient serviceInterface;
        if (!isCapturingFingerprintGestures()) {
            return;
        }
        synchronized (this.mLock) {
            serviceInterface = this.mServiceInterface;
        }
        if (serviceInterface != null) {
            try {
                if (svcClientTracingEnabled()) {
                    logTraceSvcClient("onFingerprintGesture", String.valueOf(gesture));
                }
                this.mServiceInterface.onFingerprintGesture(gesture);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
    public void dispatchGesture(int sequence, ParceledListSlice gestureSteps, int displayId) {
        synchronized (this.mLock) {
            if (this.mServiceInterface != null && this.mSecurityPolicy.canPerformGestures(this)) {
                MotionEventInjector motionEventInjector = this.mSystemSupport.getMotionEventInjectorForDisplayLocked(displayId);
                if (wmTracingEnabled()) {
                    logTraceWM("isTouchOrFaketouchDevice", "");
                }
                if (motionEventInjector != null && this.mWindowManagerService.isTouchOrFaketouchDevice()) {
                    motionEventInjector.injectEvents(gestureSteps.getList(), this.mServiceInterface, sequence, displayId);
                } else {
                    try {
                        if (svcClientTracingEnabled()) {
                            logTraceSvcClient("onPerformGestureResult", sequence + ", false");
                        }
                        this.mServiceInterface.onPerformGestureResult(sequence, false);
                    } catch (RemoteException re) {
                        Slog.e(LOG_TAG, "Error sending motion event injection failure to " + this.mServiceInterface, re);
                    }
                }
            }
        }
    }

    @Override // com.android.server.accessibility.AbstractAccessibilityServiceConnection
    public void setFocusAppearance(int strokeWidth, int color) {
        AccessibilityUserState userState = this.mUserStateWeakReference.get();
        if (userState == null) {
            return;
        }
        synchronized (this.mLock) {
            if (hasRightsToCurrentUserLocked()) {
                if (this.mSecurityPolicy.checkAccessibilityAccess(this)) {
                    if (userState.getFocusStrokeWidthLocked() == strokeWidth && userState.getFocusColorLocked() == color) {
                        return;
                    }
                    userState.setFocusAppearanceLocked(strokeWidth, color);
                    this.mSystemSupport.onClientChangeLocked(false);
                }
            }
        }
    }

    public void notifyMotionEvent(MotionEvent event) {
        Message msg = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.AccessibilityServiceConnection$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityServiceConnection) obj).notifyMotionEventInternal((MotionEvent) obj2);
            }
        }, this, event);
        this.mMainHandler.sendMessage(msg);
    }

    public void notifyTouchState(int displayId, int state) {
        Message msg = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.AccessibilityServiceConnection$$ExternalSyntheticLambda0
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityServiceConnection) obj).notifyTouchStateInternal(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
            }
        }, this, Integer.valueOf(displayId), Integer.valueOf(state));
        this.mMainHandler.sendMessage(msg);
    }

    public boolean requestImeApis() {
        return this.mRequestImeApis;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyMotionEventInternal(MotionEvent event) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (this.mTrace.isA11yTracingEnabled()) {
                    logTraceSvcClient(".onMotionEvent ", event.toString());
                }
                listener.onMotionEvent(event);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending motion event to" + this.mService, re);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyTouchStateInternal(int displayId, int state) {
        IAccessibilityServiceClient listener = getServiceInterfaceSafely();
        if (listener != null) {
            try {
                if (this.mTrace.isA11yTracingEnabled()) {
                    logTraceSvcClient(".onTouchStateChanged ", TouchInteractionController.stateToString(state));
                }
                listener.onTouchStateChanged(displayId, state);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending motion event to" + this.mService, re);
            }
        }
    }
}
