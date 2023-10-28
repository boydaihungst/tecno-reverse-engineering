package com.android.server.accessibility.magnification;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.accessibility.IRemoteMagnificationAnimationCallback;
import android.view.accessibility.IWindowMagnificationConnection;
import android.view.accessibility.IWindowMagnificationConnectionCallback;
import android.view.accessibility.MagnificationAnimationCallback;
import com.android.server.accessibility.AccessibilityTraceManager;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class WindowMagnificationConnectionWrapper {
    private static final boolean DBG = false;
    private static final String TAG = "WindowMagnificationConnectionWrapper";
    private final IWindowMagnificationConnection mConnection;
    private final AccessibilityTraceManager mTrace;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowMagnificationConnectionWrapper(IWindowMagnificationConnection connection, AccessibilityTraceManager trace) {
        this.mConnection = connection;
        this.mTrace = trace;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unlinkToDeath(IBinder.DeathRecipient deathRecipient) {
        this.mConnection.asBinder().unlinkToDeath(deathRecipient, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void linkToDeath(IBinder.DeathRecipient deathRecipient) throws RemoteException {
        this.mConnection.asBinder().linkToDeath(deathRecipient, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY, float magnificationFrameOffsetRatioX, float magnificationFrameOffsetRatioY, MagnificationAnimationCallback callback) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.enableWindowMagnification", 128L, "displayId=" + displayId + ";scale=" + scale + ";centerX=" + centerX + ";centerY=" + centerY + ";magnificationFrameOffsetRatioX=" + magnificationFrameOffsetRatioX + ";magnificationFrameOffsetRatioY=" + magnificationFrameOffsetRatioY + ";callback=" + callback);
        }
        try {
            this.mConnection.enableWindowMagnification(displayId, scale, centerX, centerY, magnificationFrameOffsetRatioX, magnificationFrameOffsetRatioY, transformToRemoteCallback(callback, this.mTrace));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setScale(int displayId, float scale) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.setScale", 128L, "displayId=" + displayId + ";scale=" + scale);
        }
        try {
            this.mConnection.setScale(displayId, scale);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableWindowMagnification(int displayId, MagnificationAnimationCallback callback) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.disableWindowMagnification", 128L, "displayId=" + displayId + ";callback=" + callback);
        }
        try {
            this.mConnection.disableWindowMagnification(displayId, transformToRemoteCallback(callback, this.mTrace));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean moveWindowMagnifier(int displayId, float offsetX, float offsetY) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.moveWindowMagnifier", 128L, "displayId=" + displayId + ";offsetX=" + offsetX + ";offsetY=" + offsetY);
        }
        try {
            this.mConnection.moveWindowMagnifier(displayId, offsetX, offsetY);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean moveWindowMagnifierToPosition(int displayId, float positionX, float positionY, MagnificationAnimationCallback callback) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.moveWindowMagnifierToPosition", 128L, "displayId=" + displayId + ";positionX=" + positionX + ";positionY=" + positionY);
        }
        try {
            this.mConnection.moveWindowMagnifierToPosition(displayId, positionX, positionY, transformToRemoteCallback(callback, this.mTrace));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showMagnificationButton(int displayId, int magnificationMode) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.showMagnificationButton", 128L, "displayId=" + displayId + ";mode=" + magnificationMode);
        }
        try {
            this.mConnection.showMagnificationButton(displayId, magnificationMode);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeMagnificationButton(int displayId) {
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.removeMagnificationButton", 128L, "displayId=" + displayId);
        }
        try {
            this.mConnection.removeMagnificationButton(displayId);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setConnectionCallback(IWindowMagnificationConnectionCallback connectionCallback) {
        if (this.mTrace.isA11yTracingEnabledForTypes(384L)) {
            this.mTrace.logTrace("WindowMagnificationConnectionWrapper.setConnectionCallback", 384L, "callback=" + connectionCallback);
        }
        try {
            this.mConnection.setConnectionCallback(connectionCallback);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private static IRemoteMagnificationAnimationCallback transformToRemoteCallback(MagnificationAnimationCallback callback, AccessibilityTraceManager trace) {
        if (callback == null) {
            return null;
        }
        return new RemoteAnimationCallback(callback, trace);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RemoteAnimationCallback extends IRemoteMagnificationAnimationCallback.Stub {
        private final MagnificationAnimationCallback mCallback;
        private final AccessibilityTraceManager mTrace;

        RemoteAnimationCallback(MagnificationAnimationCallback callback, AccessibilityTraceManager trace) {
            this.mCallback = callback;
            this.mTrace = trace;
            if (trace.isA11yTracingEnabledForTypes(64L)) {
                trace.logTrace("RemoteAnimationCallback.constructor", 64L, "callback=" + callback);
            }
        }

        public void onResult(boolean success) throws RemoteException {
            this.mCallback.onResult(success);
            if (this.mTrace.isA11yTracingEnabledForTypes(64L)) {
                this.mTrace.logTrace("RemoteAnimationCallback.onResult", 64L, "success=" + success);
            }
        }
    }
}
