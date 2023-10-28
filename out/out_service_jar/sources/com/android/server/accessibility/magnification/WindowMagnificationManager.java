package com.android.server.accessibility.magnification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.accessibility.IWindowMagnificationConnection;
import android.view.accessibility.IWindowMagnificationConnectionCallback;
import android.view.accessibility.MagnificationAnimationCallback;
import com.android.server.LocalServices;
import com.android.server.accessibility.AccessibilityTraceManager;
import com.android.server.accessibility.magnification.PanningScalingHandler;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public class WindowMagnificationManager implements PanningScalingHandler.MagnificationDelegate, WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks {
    private static final int CONNECTED = 1;
    private static final int CONNECTING = 0;
    private static final boolean DBG = false;
    private static final int DISCONNECTED = 3;
    private static final int DISCONNECTING = 2;
    private static final String TAG = "WindowMagnificationMgr";
    private static final int WAIT_CONNECTION_TIMEOUT_MILLIS = 100;
    public static final int WINDOW_POSITION_AT_CENTER = 0;
    public static final int WINDOW_POSITION_AT_TOP_LEFT = 1;
    private final Callback mCallback;
    private ConnectionCallback mConnectionCallback;
    WindowMagnificationConnectionWrapper mConnectionWrapper;
    private final Context mContext;
    private final Object mLock;
    private final MagnificationScaleProvider mScaleProvider;
    private final AccessibilityTraceManager mTrace;
    private int mConnectionState = 3;
    private SparseArray<WindowMagnifier> mWindowMagnifiers = new SparseArray<>();
    private boolean mMagnificationFollowTypingEnabled = true;
    private final SparseBooleanArray mIsImeVisibleArray = new SparseBooleanArray();
    private boolean mReceiverRegistered = false;
    protected final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() { // from class: com.android.server.accessibility.magnification.WindowMagnificationManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int displayId = context.getDisplayId();
            WindowMagnificationManager.this.removeMagnificationButton(displayId);
            WindowMagnificationManager.this.disableWindowMagnification(displayId, false, null);
        }
    };

    /* loaded from: classes.dex */
    public interface Callback {
        void onAccessibilityActionPerformed(int i);

        void onChangeMagnificationMode(int i, int i2);

        void onPerformScaleAction(int i, float f);

        void onSourceBoundsChanged(int i, Rect rect);

        void onWindowMagnificationActivationState(int i, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface ConnectionState {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface WindowPosition {
    }

    private static String connectionStateToString(int state) {
        switch (state) {
            case 0:
                return "CONNECTING";
            case 1:
                return "CONNECTED";
            case 2:
                return "DISCONNECTING";
            case 3:
                return "DISCONNECTED";
            default:
                return "UNKNOWN:" + state;
        }
    }

    public WindowMagnificationManager(Context context, Object lock, Callback callback, AccessibilityTraceManager trace, MagnificationScaleProvider scaleProvider) {
        this.mContext = context;
        this.mLock = lock;
        this.mCallback = callback;
        this.mTrace = trace;
        this.mScaleProvider = scaleProvider;
    }

    public void setConnection(IWindowMagnificationConnection connection) {
        Object obj;
        synchronized (this.mLock) {
            WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
            if (windowMagnificationConnectionWrapper != null) {
                windowMagnificationConnectionWrapper.setConnectionCallback(null);
                ConnectionCallback connectionCallback = this.mConnectionCallback;
                if (connectionCallback != null) {
                    connectionCallback.mExpiredDeathRecipient = true;
                }
                this.mConnectionWrapper.unlinkToDeath(this.mConnectionCallback);
                this.mConnectionWrapper = null;
                if (this.mConnectionState != 0) {
                    setConnectionState(3);
                }
            }
            if (connection != null) {
                this.mConnectionWrapper = new WindowMagnificationConnectionWrapper(connection, this.mTrace);
            }
            if (this.mConnectionWrapper != null) {
                try {
                    ConnectionCallback connectionCallback2 = new ConnectionCallback();
                    this.mConnectionCallback = connectionCallback2;
                    this.mConnectionWrapper.linkToDeath(connectionCallback2);
                    this.mConnectionWrapper.setConnectionCallback(this.mConnectionCallback);
                    setConnectionState(1);
                    obj = this.mLock;
                } catch (RemoteException e) {
                    Slog.e(TAG, "setConnection failed", e);
                    this.mConnectionWrapper = null;
                    setConnectionState(3);
                    obj = this.mLock;
                }
                obj.notify();
            }
        }
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mConnectionWrapper != null;
        }
        return z;
    }

    public boolean requestConnection(boolean connect) {
        int i;
        if (this.mTrace.isA11yTracingEnabledForTypes(128L)) {
            this.mTrace.logTrace("WindowMagnificationMgr.requestWindowMagnificationConnection", 128L, "connect=" + connect);
        }
        synchronized (this.mLock) {
            if (connect) {
                try {
                    int i2 = this.mConnectionState;
                    if (i2 != 1 && i2 != 0) {
                    }
                    Slog.w(TAG, "requestConnection duplicated request: connect=" + connect + ", mConnectionState=" + connectionStateToString(this.mConnectionState));
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (connect || ((i = this.mConnectionState) != 3 && i != 2)) {
                if (connect) {
                    IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
                    if (!this.mReceiverRegistered) {
                        this.mContext.registerReceiver(this.mScreenStateReceiver, intentFilter);
                        this.mReceiverRegistered = true;
                    }
                } else {
                    disableAllWindowMagnifiers();
                    if (this.mReceiverRegistered) {
                        this.mContext.unregisterReceiver(this.mScreenStateReceiver);
                        this.mReceiverRegistered = false;
                    }
                }
                if (requestConnectionInternal(connect)) {
                    setConnectionState(connect ? 0 : 2);
                    return true;
                }
                setConnectionState(3);
                return false;
            }
            Slog.w(TAG, "requestConnection duplicated request: connect=" + connect + ", mConnectionState=" + connectionStateToString(this.mConnectionState));
            return false;
        }
    }

    private boolean requestConnectionInternal(boolean connect) {
        long identity = Binder.clearCallingIdentity();
        try {
            StatusBarManagerInternal service = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (service != null) {
                return service.requestWindowMagnificationConnection(connect);
            }
            Binder.restoreCallingIdentity(identity);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public String getConnectionState() {
        return connectionStateToString(this.mConnectionState);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setConnectionState(int state) {
        this.mConnectionState = state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disableAllWindowMagnifiers() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mWindowMagnifiers.size(); i++) {
                WindowMagnifier magnifier = this.mWindowMagnifiers.valueAt(i);
                magnifier.disableWindowMagnificationInternal(null);
            }
            this.mWindowMagnifiers.clear();
        }
    }

    public void resetAllIfNeeded(int connectionId) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mWindowMagnifiers.size(); i++) {
                WindowMagnifier magnifier = this.mWindowMagnifiers.valueAt(i);
                if (magnifier != null && magnifier.mEnabled && connectionId == magnifier.getIdOfLastServiceToControl()) {
                    magnifier.disableWindowMagnificationInternal(null);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetWindowMagnifiers() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mWindowMagnifiers.size(); i++) {
                WindowMagnifier magnifier = this.mWindowMagnifiers.valueAt(i);
                magnifier.reset();
            }
        }
    }

    @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks
    public void onRectangleOnScreenRequested(int displayId, int left, int top, int right, int bottom) {
        if (!this.mMagnificationFollowTypingEnabled) {
            return;
        }
        float toCenterX = (left + right) / 2.0f;
        float toCenterY = (top + bottom) / 2.0f;
        synchronized (this.mLock) {
            if (this.mIsImeVisibleArray.get(displayId, false) && !isPositionInSourceBounds(displayId, toCenterX, toCenterY) && isTrackingTypingFocusEnabled(displayId)) {
                moveWindowMagnifierToPositionInternal(displayId, toCenterX, toCenterY, MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMagnificationFollowTypingEnabled(boolean enabled) {
        this.mMagnificationFollowTypingEnabled = enabled;
    }

    boolean isMagnificationFollowTypingEnabled() {
        return this.mMagnificationFollowTypingEnabled;
    }

    public int getIdOfLastServiceToMagnify(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier != null) {
                return magnifier.mIdOfLastServiceToControl;
            }
            return -1;
        }
    }

    void setTrackingTypingFocusEnabled(int displayId, boolean trackingTypingFocusEnabled) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return;
            }
            magnifier.setTrackingTypingFocusEnabled(trackingTypingFocusEnabled);
        }
    }

    private void enableAllTrackingTypingFocus() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mWindowMagnifiers.size(); i++) {
                WindowMagnifier magnifier = this.mWindowMagnifiers.valueAt(i);
                magnifier.setTrackingTypingFocusEnabled(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onImeWindowVisibilityChanged(int displayId, boolean shown) {
        this.mIsImeVisibleArray.put(displayId, shown);
        if (shown) {
            enableAllTrackingTypingFocus();
        }
    }

    @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
    public boolean processScroll(int displayId, float distanceX, float distanceY) {
        moveWindowMagnification(displayId, -distanceX, -distanceY);
        setTrackingTypingFocusEnabled(displayId, false);
        return true;
    }

    @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
    public void setScale(int displayId, float scale) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return;
            }
            magnifier.setScale(scale);
        }
    }

    public boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY) {
        return enableWindowMagnification(displayId, scale, centerX, centerY, MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK, 0);
    }

    public boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY, MagnificationAnimationCallback animationCallback, int id) {
        return enableWindowMagnification(displayId, scale, centerX, centerY, animationCallback, 0, id);
    }

    public boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY, int windowPosition) {
        return enableWindowMagnification(displayId, scale, centerX, centerY, MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK, windowPosition, 0);
    }

    public boolean enableWindowMagnification(int displayId, float scale, float centerX, float centerY, MagnificationAnimationCallback animationCallback, int windowPosition, int id) {
        boolean previousEnabled;
        boolean enabled;
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                magnifier = createWindowMagnifier(displayId);
            }
            previousEnabled = magnifier.mEnabled;
            enabled = magnifier.enableWindowMagnificationInternal(scale, centerX, centerY, animationCallback, windowPosition, id);
        }
        if (enabled) {
            setTrackingTypingFocusEnabled(displayId, true);
            if (!previousEnabled) {
                this.mCallback.onWindowMagnificationActivationState(displayId, true);
            }
        }
        return enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableWindowMagnification(int displayId, boolean clear) {
        return disableWindowMagnification(displayId, clear, MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK);
    }

    public boolean disableWindowMagnification(int displayId, boolean clear, MagnificationAnimationCallback animationCallback) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return false;
            }
            boolean disabled = magnifier.disableWindowMagnificationInternal(animationCallback);
            if (clear) {
                this.mWindowMagnifiers.delete(displayId);
            }
            if (disabled) {
                this.mCallback.onWindowMagnificationActivationState(displayId, false);
            }
            return disabled;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int pointersInWindow(int displayId, MotionEvent motionEvent) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return 0;
            }
            return magnifier.pointersInWindow(motionEvent);
        }
    }

    boolean isPositionInSourceBounds(int displayId, float x, float y) {
        WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
        if (magnifier == null) {
            return false;
        }
        return magnifier.isPositionInSourceBounds(x, y);
    }

    public boolean isWindowMagnifierEnabled(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return false;
            }
            return magnifier.isEnabled();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getPersistedScale(int displayId) {
        return this.mScaleProvider.getScale(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistScale(int displayId) {
        float scale = getScale(displayId);
        if (scale != 1.0f) {
            this.mScaleProvider.putScale(scale, displayId);
        }
    }

    @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
    public float getScale(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier != null && magnifier.mEnabled) {
                return magnifier.getScale();
            }
            return 1.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveWindowMagnification(int displayId, float offsetX, float offsetY) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return;
            }
            magnifier.move(offsetX, offsetY);
        }
    }

    public boolean showMagnificationButton(int displayId, int magnificationMode) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        return windowMagnificationConnectionWrapper != null && windowMagnificationConnectionWrapper.showMagnificationButton(displayId, magnificationMode);
    }

    public boolean removeMagnificationButton(int displayId) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        return windowMagnificationConnectionWrapper != null && windowMagnificationConnectionWrapper.removeMagnificationButton(displayId);
    }

    public float getCenterX(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier != null && magnifier.mEnabled) {
                return magnifier.getCenterX();
            }
            return Float.NaN;
        }
    }

    public float getCenterY(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier != null && magnifier.mEnabled) {
                return magnifier.getCenterY();
            }
            return Float.NaN;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTrackingTypingFocusEnabled(int displayId) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier == null) {
                return false;
            }
            return magnifier.isTrackingTypingFocusEnabled();
        }
    }

    public void getMagnificationSourceBounds(int displayId, Region outRegion) {
        synchronized (this.mLock) {
            WindowMagnifier magnifier = this.mWindowMagnifiers.get(displayId);
            if (magnifier != null && magnifier.mEnabled) {
                outRegion.set(magnifier.mSourceBounds);
            }
            outRegion.setEmpty();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WindowMagnifier createWindowMagnifier(int displayId) {
        WindowMagnifier magnifier = new WindowMagnifier(displayId, this);
        this.mWindowMagnifiers.put(displayId, magnifier);
        return magnifier;
    }

    public void onDisplayRemoved(int displayId) {
        disableWindowMagnification(displayId, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ConnectionCallback extends IWindowMagnificationConnectionCallback.Stub implements IBinder.DeathRecipient {
        private boolean mExpiredDeathRecipient;

        private ConnectionCallback() {
            this.mExpiredDeathRecipient = false;
        }

        public void onWindowMagnifierBoundsChanged(int displayId, Rect bounds) {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onWindowMagnifierBoundsChanged", 256L, "displayId=" + displayId + ";bounds=" + bounds);
            }
            synchronized (WindowMagnificationManager.this.mLock) {
                WindowMagnifier magnifier = (WindowMagnifier) WindowMagnificationManager.this.mWindowMagnifiers.get(displayId);
                if (magnifier == null) {
                    magnifier = WindowMagnificationManager.this.createWindowMagnifier(displayId);
                }
                magnifier.setMagnifierLocation(bounds);
            }
        }

        public void onChangeMagnificationMode(int displayId, int magnificationMode) throws RemoteException {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onChangeMagnificationMode", 256L, "displayId=" + displayId + ";mode=" + magnificationMode);
            }
            WindowMagnificationManager.this.mCallback.onChangeMagnificationMode(displayId, magnificationMode);
        }

        public void onSourceBoundsChanged(int displayId, Rect sourceBounds) {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onSourceBoundsChanged", 256L, "displayId=" + displayId + ";source=" + sourceBounds);
            }
            synchronized (WindowMagnificationManager.this.mLock) {
                WindowMagnifier magnifier = (WindowMagnifier) WindowMagnificationManager.this.mWindowMagnifiers.get(displayId);
                if (magnifier == null) {
                    magnifier = WindowMagnificationManager.this.createWindowMagnifier(displayId);
                }
                magnifier.onSourceBoundsChanged(sourceBounds);
            }
            WindowMagnificationManager.this.mCallback.onSourceBoundsChanged(displayId, sourceBounds);
        }

        public void onPerformScaleAction(int displayId, float scale) {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onPerformScaleAction", 256L, "displayId=" + displayId + ";scale=" + scale);
            }
            WindowMagnificationManager.this.mCallback.onPerformScaleAction(displayId, scale);
        }

        public void onAccessibilityActionPerformed(int displayId) {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onAccessibilityActionPerformed", 256L, "displayId=" + displayId);
            }
            WindowMagnificationManager.this.mCallback.onAccessibilityActionPerformed(displayId);
        }

        public void onMove(int displayId) {
            if (WindowMagnificationManager.this.mTrace.isA11yTracingEnabledForTypes(256L)) {
                WindowMagnificationManager.this.mTrace.logTrace("WindowMagnificationMgrConnectionCallback.onMove", 256L, "displayId=" + displayId);
            }
            WindowMagnificationManager.this.setTrackingTypingFocusEnabled(displayId, false);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (WindowMagnificationManager.this.mLock) {
                Slog.w(WindowMagnificationManager.TAG, "binderDied DeathRecipient :" + this.mExpiredDeathRecipient);
                if (this.mExpiredDeathRecipient) {
                    return;
                }
                WindowMagnificationManager.this.mConnectionWrapper.unlinkToDeath(this);
                WindowMagnificationManager.this.mConnectionWrapper = null;
                WindowMagnificationManager.this.mConnectionCallback = null;
                WindowMagnificationManager.this.setConnectionState(3);
                WindowMagnificationManager.this.resetWindowMagnifiers();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class WindowMagnifier {
        private final int mDisplayId;
        private boolean mEnabled;
        private final WindowMagnificationManager mWindowMagnificationManager;
        private float mScale = 1.0f;
        private final Rect mBounds = new Rect();
        private final Rect mSourceBounds = new Rect();
        private int mIdOfLastServiceToControl = -1;
        private final PointF mMagnificationFrameOffsetRatio = new PointF(0.0f, 0.0f);
        private boolean mTrackingTypingFocusEnabled = true;

        WindowMagnifier(int displayId, WindowMagnificationManager windowMagnificationManager) {
            this.mDisplayId = displayId;
            this.mWindowMagnificationManager = windowMagnificationManager;
        }

        boolean enableWindowMagnificationInternal(float scale, float centerX, float centerY, MagnificationAnimationCallback animationCallback, int windowPosition, int id) {
            if (Float.isNaN(scale)) {
                scale = getScale();
            }
            float normScale = MagnificationScaleProvider.constrainScale(scale);
            setMagnificationFrameOffsetRatioByWindowPosition(windowPosition);
            if (this.mWindowMagnificationManager.enableWindowMagnificationInternal(this.mDisplayId, normScale, centerX, centerY, this.mMagnificationFrameOffsetRatio.x, this.mMagnificationFrameOffsetRatio.y, animationCallback)) {
                this.mScale = normScale;
                this.mEnabled = true;
                this.mIdOfLastServiceToControl = id;
                return true;
            }
            return false;
        }

        void setMagnificationFrameOffsetRatioByWindowPosition(int windowPosition) {
            switch (windowPosition) {
                case 0:
                    this.mMagnificationFrameOffsetRatio.set(0.0f, 0.0f);
                    return;
                case 1:
                    this.mMagnificationFrameOffsetRatio.set(-1.0f, -1.0f);
                    return;
                default:
                    return;
            }
        }

        boolean disableWindowMagnificationInternal(MagnificationAnimationCallback animationResultCallback) {
            if (this.mEnabled && this.mWindowMagnificationManager.disableWindowMagnificationInternal(this.mDisplayId, animationResultCallback)) {
                this.mEnabled = false;
                this.mIdOfLastServiceToControl = -1;
                this.mTrackingTypingFocusEnabled = false;
                return true;
            }
            return false;
        }

        void setScale(float scale) {
            if (!this.mEnabled) {
                return;
            }
            float normScale = MagnificationScaleProvider.constrainScale(scale);
            if (Float.compare(this.mScale, normScale) != 0 && this.mWindowMagnificationManager.setScaleInternal(this.mDisplayId, scale)) {
                this.mScale = normScale;
            }
        }

        float getScale() {
            return this.mScale;
        }

        void setMagnifierLocation(Rect rect) {
            this.mBounds.set(rect);
        }

        int getIdOfLastServiceToControl() {
            return this.mIdOfLastServiceToControl;
        }

        int pointersInWindow(MotionEvent motionEvent) {
            int count = 0;
            int pointerCount = motionEvent.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                float x = motionEvent.getX(i);
                float y = motionEvent.getY(i);
                if (this.mBounds.contains((int) x, (int) y)) {
                    count++;
                }
            }
            return count;
        }

        boolean isPositionInSourceBounds(float x, float y) {
            return this.mSourceBounds.contains((int) x, (int) y);
        }

        void setTrackingTypingFocusEnabled(boolean trackingTypingFocusEnabled) {
            this.mTrackingTypingFocusEnabled = trackingTypingFocusEnabled;
        }

        boolean isTrackingTypingFocusEnabled() {
            return this.mTrackingTypingFocusEnabled;
        }

        boolean isEnabled() {
            return this.mEnabled;
        }

        void move(float offsetX, float offsetY) {
            this.mWindowMagnificationManager.moveWindowMagnifierInternal(this.mDisplayId, offsetX, offsetY);
        }

        void reset() {
            this.mEnabled = false;
            this.mIdOfLastServiceToControl = -1;
            this.mSourceBounds.setEmpty();
        }

        public void onSourceBoundsChanged(Rect sourceBounds) {
            this.mSourceBounds.set(sourceBounds);
        }

        float getCenterX() {
            return this.mSourceBounds.exactCenterX();
        }

        float getCenterY() {
            return this.mSourceBounds.exactCenterY();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean enableWindowMagnificationInternal(int displayId, float scale, float centerX, float centerY, float magnificationFrameOffsetRatioX, float magnificationFrameOffsetRatioY, MagnificationAnimationCallback animationCallback) {
        long endMillis = SystemClock.uptimeMillis() + 100;
        while (this.mConnectionState == 0 && SystemClock.uptimeMillis() < endMillis) {
            try {
                this.mLock.wait(endMillis - SystemClock.uptimeMillis());
            } catch (InterruptedException e) {
            }
        }
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        if (windowMagnificationConnectionWrapper == null) {
            Slog.w(TAG, "enableWindowMagnificationInternal mConnectionWrapper is null. mConnectionState=" + connectionStateToString(this.mConnectionState));
            return false;
        }
        return windowMagnificationConnectionWrapper.enableWindowMagnification(displayId, scale, centerX, centerY, magnificationFrameOffsetRatioX, magnificationFrameOffsetRatioY, animationCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setScaleInternal(int displayId, float scale) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        return windowMagnificationConnectionWrapper != null && windowMagnificationConnectionWrapper.setScale(displayId, scale);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean disableWindowMagnificationInternal(int displayId, MagnificationAnimationCallback animationCallback) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        if (windowMagnificationConnectionWrapper == null) {
            Slog.w(TAG, "mConnectionWrapper is null");
            return false;
        }
        return windowMagnificationConnectionWrapper.disableWindowMagnification(displayId, animationCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean moveWindowMagnifierInternal(int displayId, float offsetX, float offsetY) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        return windowMagnificationConnectionWrapper != null && windowMagnificationConnectionWrapper.moveWindowMagnifier(displayId, offsetX, offsetY);
    }

    private boolean moveWindowMagnifierToPositionInternal(int displayId, float positionX, float positionY, MagnificationAnimationCallback animationCallback) {
        WindowMagnificationConnectionWrapper windowMagnificationConnectionWrapper = this.mConnectionWrapper;
        return windowMagnificationConnectionWrapper != null && windowMagnificationConnectionWrapper.moveWindowMagnifierToPosition(displayId, positionX, positionY, animationCallback);
    }
}
