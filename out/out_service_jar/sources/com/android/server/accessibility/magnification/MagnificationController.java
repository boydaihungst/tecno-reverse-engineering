package com.android.server.accessibility.magnification;

import android.accessibilityservice.MagnificationConfig;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.view.accessibility.MagnificationAnimationCallback;
import com.android.internal.accessibility.util.AccessibilityStatsLogUtils;
import com.android.server.LocalServices;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.magnification.FullScreenMagnificationController;
import com.android.server.accessibility.magnification.MagnificationGestureHandler;
import com.android.server.accessibility.magnification.WindowMagnificationManager;
import com.android.server.wm.WindowManagerInternal;
/* loaded from: classes.dex */
public class MagnificationController implements WindowMagnificationManager.Callback, MagnificationGestureHandler.Callback, FullScreenMagnificationController.MagnificationInfoChangedCallback, WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks {
    private static final boolean DEBUG = false;
    private static final String TAG = "MagnificationController";
    private final SparseArray<WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks> mAccessibilityCallbacksDelegateArray;
    private final AccessibilityManagerService mAms;
    private final Context mContext;
    private final SparseIntArray mCurrentMagnificationModeArray;
    private FullScreenMagnificationController mFullScreenMagnificationController;
    private final SparseLongArray mFullScreenModeEnabledTimeArray;
    private final SparseBooleanArray mIsImeVisibleArray;
    private final SparseIntArray mLastMagnificationActivatedModeArray;
    private final Object mLock;
    private int mMagnificationCapabilities;
    private final SparseArray<DisableMagnificationCallback> mMagnificationEndRunnableSparseArray;
    private final MagnificationScaleProvider mScaleProvider;
    private final boolean mSupportWindowMagnification;
    private final PointF mTempPoint;
    private final SparseArray<Integer> mTransitionModes;
    private int mUserId;
    private WindowMagnificationManager mWindowMagnificationMgr;
    private final SparseLongArray mWindowModeEnabledTimeArray;

    /* loaded from: classes.dex */
    public interface TransitionCallBack {
        void onResult(int i, boolean z);
    }

    public MagnificationController(AccessibilityManagerService ams, Object lock, Context context, MagnificationScaleProvider scaleProvider) {
        this.mTempPoint = new PointF();
        this.mMagnificationEndRunnableSparseArray = new SparseArray<>();
        this.mMagnificationCapabilities = 1;
        this.mCurrentMagnificationModeArray = new SparseIntArray();
        this.mLastMagnificationActivatedModeArray = new SparseIntArray();
        this.mUserId = 0;
        this.mIsImeVisibleArray = new SparseBooleanArray();
        this.mWindowModeEnabledTimeArray = new SparseLongArray();
        this.mFullScreenModeEnabledTimeArray = new SparseLongArray();
        this.mTransitionModes = new SparseArray<>();
        this.mAccessibilityCallbacksDelegateArray = new SparseArray<>();
        this.mAms = ams;
        this.mLock = lock;
        this.mContext = context;
        this.mScaleProvider = scaleProvider;
        ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class)).getAccessibilityController().setUiChangesForAccessibilityCallbacks(this);
        this.mSupportWindowMagnification = context.getPackageManager().hasSystemFeature("android.software.window_magnification");
    }

    public MagnificationController(AccessibilityManagerService ams, Object lock, Context context, FullScreenMagnificationController fullScreenMagnificationController, WindowMagnificationManager windowMagnificationManager, MagnificationScaleProvider scaleProvider) {
        this(ams, lock, context, scaleProvider);
        this.mFullScreenMagnificationController = fullScreenMagnificationController;
        this.mWindowMagnificationMgr = windowMagnificationManager;
    }

    @Override // com.android.server.accessibility.magnification.WindowMagnificationManager.Callback
    public void onPerformScaleAction(int displayId, float scale) {
        getWindowMagnificationMgr().setScale(displayId, scale);
        getWindowMagnificationMgr().persistScale(displayId);
    }

    @Override // com.android.server.accessibility.magnification.WindowMagnificationManager.Callback
    public void onAccessibilityActionPerformed(int displayId) {
        updateMagnificationButton(displayId, 2);
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler.Callback
    public void onTouchInteractionStart(int displayId, int mode) {
        handleUserInteractionChanged(displayId, mode);
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler.Callback
    public void onTouchInteractionEnd(int displayId, int mode) {
        handleUserInteractionChanged(displayId, mode);
    }

    private void handleUserInteractionChanged(int displayId, int mode) {
        if (this.mMagnificationCapabilities == 3 && isActivated(displayId, mode)) {
            getWindowMagnificationMgr().showMagnificationButton(displayId, mode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMagnificationButton(int displayId, int mode) {
        boolean showButton;
        boolean isActivated = isActivated(displayId, mode);
        synchronized (this.mLock) {
            if (isActivated) {
                try {
                    showButton = this.mMagnificationCapabilities == 3 ? true : true;
                } catch (Throwable th) {
                    throw th;
                }
            }
            showButton = false;
        }
        if (showButton) {
            getWindowMagnificationMgr().showMagnificationButton(displayId, mode);
        } else {
            getWindowMagnificationMgr().removeMagnificationButton(displayId);
        }
    }

    public boolean supportWindowMagnification() {
        return this.mSupportWindowMagnification;
    }

    public void transitionMagnificationModeLocked(int displayId, int targetMode, TransitionCallBack transitionCallBack) {
        PointF currentCenter = getCurrentMagnificationCenterLocked(displayId, targetMode);
        DisableMagnificationCallback animationCallback = getDisableMagnificationEndRunnableLocked(displayId);
        if (currentCenter == null && animationCallback == null) {
            transitionCallBack.onResult(displayId, true);
        } else if (animationCallback != null) {
            if (animationCallback.mCurrentMode != targetMode) {
                Slog.w(TAG, "discard duplicate request");
            } else {
                animationCallback.restoreToCurrentMagnificationMode();
            }
        } else if (currentCenter != null) {
            setTransitionState(Integer.valueOf(displayId), Integer.valueOf(targetMode));
            FullScreenMagnificationController screenMagnificationController = getFullScreenMagnificationController();
            WindowMagnificationManager windowMagnificationMgr = getWindowMagnificationMgr();
            float scale = getTargetModeScaleFromCurrentMagnification(displayId, targetMode);
            DisableMagnificationCallback animationEndCallback = new DisableMagnificationCallback(transitionCallBack, displayId, targetMode, scale, currentCenter, true);
            if (targetMode == 2) {
                screenMagnificationController.reset(displayId, animationEndCallback);
            } else {
                windowMagnificationMgr.disableWindowMagnification(displayId, false, animationEndCallback);
            }
            setDisableMagnificationCallbackLocked(displayId, animationEndCallback);
        } else {
            Slog.w(TAG, "Invalid center, ignore it");
            transitionCallBack.onResult(displayId, true);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [328=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:51:0x00ec */
    /* JADX DEBUG: Multi-variable search result rejected for r15v2, resolved type: java.lang.Integer */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r15v0, types: [android.graphics.PointF] */
    /* JADX WARN: Type inference failed for: r15v1 */
    /* JADX WARN: Type inference failed for: r15v3 */
    /* JADX WARN: Type inference failed for: r15v4 */
    /* JADX WARN: Type inference failed for: r15v7 */
    public void transitionMagnificationConfigMode(int displayId, MagnificationConfig config, boolean animate, int id) {
        MagnificationAnimationCallback magnificationAnimationCallback;
        Integer num;
        synchronized (this.mLock) {
            int targetMode = config.getMode();
            Integer currentMagnificationCenterLocked = getCurrentMagnificationCenterLocked(displayId, targetMode);
            PointF magnificationCenter = new PointF(config.getCenterX(), config.getCenterY());
            if (currentMagnificationCenterLocked != 0) {
                float centerX = Float.isNaN(config.getCenterX()) ? ((PointF) currentMagnificationCenterLocked).x : config.getCenterX();
                float centerY = Float.isNaN(config.getCenterY()) ? ((PointF) currentMagnificationCenterLocked).y : config.getCenterY();
                magnificationCenter.set(centerX, centerY);
            }
            DisableMagnificationCallback animationCallback = getDisableMagnificationEndRunnableLocked(displayId);
            if (animationCallback != null) {
                Slog.w(TAG, "Discard previous animation request");
                animationCallback.setExpiredAndRemoveFromListLocked();
            }
            FullScreenMagnificationController screenMagnificationController = getFullScreenMagnificationController();
            WindowMagnificationManager windowMagnificationMgr = getWindowMagnificationMgr();
            float targetScale = Float.isNaN(config.getScale()) ? getTargetModeScaleFromCurrentMagnification(displayId, targetMode) : config.getScale();
            try {
                setTransitionState(Integer.valueOf(displayId), Integer.valueOf(targetMode));
                try {
                    if (targetMode == 2) {
                        try {
                            screenMagnificationController.reset(displayId, false);
                            float f = magnificationCenter.x;
                            float f2 = magnificationCenter.y;
                            if (animate) {
                                try {
                                    magnificationAnimationCallback = MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK;
                                } catch (Throwable th) {
                                    th = th;
                                    currentMagnificationCenterLocked = 0;
                                    setTransitionState(Integer.valueOf(displayId), currentMagnificationCenterLocked);
                                    throw th;
                                }
                            } else {
                                magnificationAnimationCallback = null;
                            }
                            num = null;
                            windowMagnificationMgr.enableWindowMagnification(displayId, targetScale, f, f2, magnificationAnimationCallback, id);
                        } catch (Throwable th2) {
                            th = th2;
                            currentMagnificationCenterLocked = 0;
                        }
                    } else {
                        currentMagnificationCenterLocked = 0;
                        if (targetMode == 1) {
                            try {
                                windowMagnificationMgr.disableWindowMagnification(displayId, false, null);
                                if (!screenMagnificationController.isRegistered(displayId)) {
                                    screenMagnificationController.register(displayId);
                                }
                                try {
                                    screenMagnificationController.setScaleAndCenter(displayId, targetScale, magnificationCenter.x, magnificationCenter.y, animate, id);
                                    num = currentMagnificationCenterLocked;
                                } catch (Throwable th3) {
                                    th = th3;
                                    setTransitionState(Integer.valueOf(displayId), currentMagnificationCenterLocked);
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        } else {
                            num = currentMagnificationCenterLocked;
                        }
                    }
                    setTransitionState(Integer.valueOf(displayId), num);
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
                currentMagnificationCenterLocked = 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTransitionState(Integer displayId, Integer targetMode) {
        synchronized (this.mLock) {
            if (targetMode == null && displayId == null) {
                this.mTransitionModes.clear();
            } else {
                this.mTransitionModes.put(displayId.intValue(), targetMode);
            }
        }
    }

    private float getTargetModeScaleFromCurrentMagnification(int displayId, int targetMode) {
        if (targetMode == 2) {
            return getFullScreenMagnificationController().getScale(displayId);
        }
        return getWindowMagnificationMgr().getScale(displayId);
    }

    public boolean hasDisableMagnificationCallback(int displayId) {
        synchronized (this.mLock) {
            DisableMagnificationCallback animationCallback = getDisableMagnificationEndRunnableLocked(displayId);
            if (animationCallback != null) {
                return true;
            }
            return false;
        }
    }

    private void setCurrentMagnificationModeAndSwitchDelegate(int displayId, int mode) {
        this.mCurrentMagnificationModeArray.put(displayId, mode);
        assignMagnificationWindowManagerDelegateByMode(displayId, mode);
    }

    private void assignMagnificationWindowManagerDelegateByMode(int displayId, int mode) {
        if (mode == 1) {
            this.mAccessibilityCallbacksDelegateArray.put(displayId, getFullScreenMagnificationController());
        } else if (mode == 2) {
            this.mAccessibilityCallbacksDelegateArray.put(displayId, getWindowMagnificationMgr());
        } else {
            this.mAccessibilityCallbacksDelegateArray.delete(displayId);
        }
    }

    @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks
    public void onRectangleOnScreenRequested(int displayId, int left, int top, int right, int bottom) {
        WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks delegate;
        synchronized (this.mLock) {
            delegate = this.mAccessibilityCallbacksDelegateArray.get(displayId);
        }
        if (delegate != null) {
            delegate.onRectangleOnScreenRequested(displayId, left, top, right, bottom);
        }
    }

    @Override // com.android.server.accessibility.magnification.FullScreenMagnificationController.MagnificationInfoChangedCallback
    public void onRequestMagnificationSpec(int displayId, int serviceId) {
        synchronized (this.mLock) {
            if (serviceId == 0) {
                return;
            }
            updateMagnificationButton(displayId, 1);
            WindowMagnificationManager windowMagnificationManager = this.mWindowMagnificationMgr;
            if (windowMagnificationManager != null) {
                windowMagnificationManager.disableWindowMagnification(displayId, false);
            }
        }
    }

    @Override // com.android.server.accessibility.magnification.WindowMagnificationManager.Callback
    public void onWindowMagnificationActivationState(int displayId, boolean activated) {
        long duration;
        if (activated) {
            synchronized (this.mLock) {
                this.mWindowModeEnabledTimeArray.put(displayId, SystemClock.uptimeMillis());
                setCurrentMagnificationModeAndSwitchDelegate(displayId, 2);
                this.mLastMagnificationActivatedModeArray.put(displayId, 2);
            }
            logMagnificationModeWithImeOnIfNeeded(displayId);
            disableFullScreenMagnificationIfNeeded(displayId);
        } else {
            synchronized (this.mLock) {
                setCurrentMagnificationModeAndSwitchDelegate(displayId, 0);
                duration = SystemClock.uptimeMillis() - this.mWindowModeEnabledTimeArray.get(displayId);
            }
            logMagnificationUsageState(2, duration);
        }
        updateMagnificationButton(displayId, 2);
    }

    @Override // com.android.server.accessibility.magnification.WindowMagnificationManager.Callback
    public void onChangeMagnificationMode(int displayId, int magnificationMode) {
        this.mAms.changeMagnificationMode(displayId, magnificationMode);
    }

    @Override // com.android.server.accessibility.magnification.WindowMagnificationManager.Callback
    public void onSourceBoundsChanged(int displayId, Rect bounds) {
        if (shouldNotifyMagnificationChange(displayId, 2)) {
            MagnificationConfig config = new MagnificationConfig.Builder().setMode(2).setScale(getWindowMagnificationMgr().getScale(displayId)).setCenterX(bounds.exactCenterX()).setCenterY(bounds.exactCenterY()).build();
            this.mAms.notifyMagnificationChanged(displayId, new Region(bounds), config);
        }
    }

    @Override // com.android.server.accessibility.magnification.FullScreenMagnificationController.MagnificationInfoChangedCallback
    public void onFullScreenMagnificationChanged(int displayId, Region region, MagnificationConfig config) {
        if (shouldNotifyMagnificationChange(displayId, 1)) {
            this.mAms.notifyMagnificationChanged(displayId, region, config);
        }
    }

    private boolean shouldNotifyMagnificationChange(int displayId, int changeMode) {
        synchronized (this.mLock) {
            FullScreenMagnificationController fullScreenMagnificationController = this.mFullScreenMagnificationController;
            boolean fullScreenMagnifying = fullScreenMagnificationController != null && fullScreenMagnificationController.isMagnifying(displayId);
            WindowMagnificationManager windowMagnificationManager = this.mWindowMagnificationMgr;
            boolean windowEnabled = windowMagnificationManager != null && windowMagnificationManager.isWindowMagnifierEnabled(displayId);
            Integer transitionMode = this.mTransitionModes.get(displayId);
            if (((changeMode == 1 && fullScreenMagnifying) || (changeMode == 2 && windowEnabled)) && transitionMode == null) {
                return true;
            }
            if (fullScreenMagnifying || windowEnabled || transitionMode != null) {
                return transitionMode != null && changeMode == transitionMode.intValue();
            }
            return true;
        }
    }

    private void disableFullScreenMagnificationIfNeeded(int displayId) {
        FullScreenMagnificationController fullScreenMagnificationController = getFullScreenMagnificationController();
        boolean isMagnifyByExternalRequest = fullScreenMagnificationController.getIdOfLastServiceToMagnify(displayId) > 0;
        if (isMagnifyByExternalRequest || isActivated(displayId, 1)) {
            fullScreenMagnificationController.reset(displayId, false);
        }
    }

    @Override // com.android.server.accessibility.magnification.FullScreenMagnificationController.MagnificationInfoChangedCallback
    public void onFullScreenMagnificationActivationState(int displayId, boolean activated) {
        long duration;
        if (activated) {
            synchronized (this.mLock) {
                this.mFullScreenModeEnabledTimeArray.put(displayId, SystemClock.uptimeMillis());
                setCurrentMagnificationModeAndSwitchDelegate(displayId, 1);
                this.mLastMagnificationActivatedModeArray.put(displayId, 1);
            }
            logMagnificationModeWithImeOnIfNeeded(displayId);
            disableWindowMagnificationIfNeeded(displayId);
        } else {
            synchronized (this.mLock) {
                setCurrentMagnificationModeAndSwitchDelegate(displayId, 0);
                duration = SystemClock.uptimeMillis() - this.mFullScreenModeEnabledTimeArray.get(displayId);
            }
            logMagnificationUsageState(1, duration);
        }
        updateMagnificationButton(displayId, 1);
    }

    private void disableWindowMagnificationIfNeeded(int displayId) {
        WindowMagnificationManager windowMagnificationManager = getWindowMagnificationMgr();
        if (isActivated(displayId, 2)) {
            windowMagnificationManager.disableWindowMagnification(displayId, false);
        }
    }

    @Override // com.android.server.accessibility.magnification.FullScreenMagnificationController.MagnificationInfoChangedCallback
    public void onImeWindowVisibilityChanged(int displayId, boolean shown) {
        synchronized (this.mLock) {
            this.mIsImeVisibleArray.put(displayId, shown);
        }
        getWindowMagnificationMgr().onImeWindowVisibilityChanged(displayId, shown);
        logMagnificationModeWithImeOnIfNeeded(displayId);
    }

    public int getLastMagnificationActivatedMode(int displayId) {
        int i;
        synchronized (this.mLock) {
            i = this.mLastMagnificationActivatedModeArray.get(displayId, 1);
        }
        return i;
    }

    public void logMagnificationUsageState(int mode, long duration) {
        AccessibilityStatsLogUtils.logMagnificationUsageState(mode, duration);
    }

    public void logMagnificationModeWithIme(int mode) {
        AccessibilityStatsLogUtils.logMagnificationModeWithImeOn(mode);
    }

    public void updateUserIdIfNeeded(int userId) {
        FullScreenMagnificationController fullMagnificationController;
        WindowMagnificationManager windowMagnificationManager;
        if (this.mUserId == userId) {
            return;
        }
        this.mUserId = userId;
        synchronized (this.mLock) {
            fullMagnificationController = this.mFullScreenMagnificationController;
            windowMagnificationManager = this.mWindowMagnificationMgr;
            this.mAccessibilityCallbacksDelegateArray.clear();
            this.mCurrentMagnificationModeArray.clear();
            this.mLastMagnificationActivatedModeArray.clear();
            this.mIsImeVisibleArray.clear();
        }
        this.mScaleProvider.onUserChanged(userId);
        if (fullMagnificationController != null) {
            fullMagnificationController.resetAllIfNeeded(false);
        }
        if (windowMagnificationManager != null) {
            windowMagnificationManager.disableAllWindowMagnifiers();
        }
    }

    public void onDisplayRemoved(int displayId) {
        synchronized (this.mLock) {
            FullScreenMagnificationController fullScreenMagnificationController = this.mFullScreenMagnificationController;
            if (fullScreenMagnificationController != null) {
                fullScreenMagnificationController.onDisplayRemoved(displayId);
            }
            WindowMagnificationManager windowMagnificationManager = this.mWindowMagnificationMgr;
            if (windowMagnificationManager != null) {
                windowMagnificationManager.onDisplayRemoved(displayId);
            }
            this.mAccessibilityCallbacksDelegateArray.delete(displayId);
            this.mCurrentMagnificationModeArray.delete(displayId);
            this.mLastMagnificationActivatedModeArray.delete(displayId);
            this.mIsImeVisibleArray.delete(displayId);
        }
        this.mScaleProvider.onDisplayRemoved(displayId);
    }

    public void onUserRemoved(int userId) {
        this.mScaleProvider.onUserRemoved(userId);
    }

    public void setMagnificationCapabilities(int capabilities) {
        this.mMagnificationCapabilities = capabilities;
    }

    public void setMagnificationFollowTypingEnabled(boolean enabled) {
        getWindowMagnificationMgr().setMagnificationFollowTypingEnabled(enabled);
        getFullScreenMagnificationController().setMagnificationFollowTypingEnabled(enabled);
    }

    private DisableMagnificationCallback getDisableMagnificationEndRunnableLocked(int displayId) {
        return this.mMagnificationEndRunnableSparseArray.get(displayId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisableMagnificationCallbackLocked(int displayId, DisableMagnificationCallback callback) {
        this.mMagnificationEndRunnableSparseArray.put(displayId, callback);
    }

    private void logMagnificationModeWithImeOnIfNeeded(int displayId) {
        synchronized (this.mLock) {
            int currentActivateMode = this.mCurrentMagnificationModeArray.get(displayId, 0);
            if (this.mIsImeVisibleArray.get(displayId, false) && currentActivateMode != 0) {
                logMagnificationModeWithIme(currentActivateMode);
            }
        }
    }

    public FullScreenMagnificationController getFullScreenMagnificationController() {
        synchronized (this.mLock) {
            if (this.mFullScreenMagnificationController == null) {
                this.mFullScreenMagnificationController = new FullScreenMagnificationController(this.mContext, this.mAms.getTraceManager(), this.mLock, this, this.mScaleProvider);
            }
        }
        return this.mFullScreenMagnificationController;
    }

    public boolean isFullScreenMagnificationControllerInitialized() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mFullScreenMagnificationController != null;
        }
        return z;
    }

    public WindowMagnificationManager getWindowMagnificationMgr() {
        WindowMagnificationManager windowMagnificationManager;
        synchronized (this.mLock) {
            if (this.mWindowMagnificationMgr == null) {
                this.mWindowMagnificationMgr = new WindowMagnificationManager(this.mContext, this.mLock, this, this.mAms.getTraceManager(), this.mScaleProvider);
            }
            windowMagnificationManager = this.mWindowMagnificationMgr;
        }
        return windowMagnificationManager;
    }

    private PointF getCurrentMagnificationCenterLocked(int displayId, int targetMode) {
        if (targetMode == 1) {
            WindowMagnificationManager windowMagnificationManager = this.mWindowMagnificationMgr;
            if (windowMagnificationManager == null || !windowMagnificationManager.isWindowMagnifierEnabled(displayId)) {
                return null;
            }
            this.mTempPoint.set(this.mWindowMagnificationMgr.getCenterX(displayId), this.mWindowMagnificationMgr.getCenterY(displayId));
        } else {
            FullScreenMagnificationController fullScreenMagnificationController = this.mFullScreenMagnificationController;
            if (fullScreenMagnificationController == null || !fullScreenMagnificationController.isMagnifying(displayId)) {
                return null;
            }
            this.mTempPoint.set(this.mFullScreenMagnificationController.getCenterX(displayId), this.mFullScreenMagnificationController.getCenterY(displayId));
        }
        return this.mTempPoint;
    }

    public boolean isActivated(int displayId, int mode) {
        boolean isActivated = false;
        boolean z = true;
        if (mode == 1) {
            synchronized (this.mLock) {
                FullScreenMagnificationController fullScreenMagnificationController = this.mFullScreenMagnificationController;
                if (fullScreenMagnificationController == null) {
                    return false;
                }
                if (!fullScreenMagnificationController.isMagnifying(displayId) && !this.mFullScreenMagnificationController.isForceShowMagnifiableBounds(displayId)) {
                    z = false;
                }
                isActivated = z;
            }
        } else if (mode == 2) {
            synchronized (this.mLock) {
                WindowMagnificationManager windowMagnificationManager = this.mWindowMagnificationMgr;
                if (windowMagnificationManager == null) {
                    return false;
                }
                isActivated = windowMagnificationManager.isWindowMagnifierEnabled(displayId);
            }
        }
        return isActivated;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisableMagnificationCallback implements MagnificationAnimationCallback {
        private final boolean mAnimate;
        private final PointF mCurrentCenter;
        private final int mCurrentMode;
        private final float mCurrentScale;
        private final int mDisplayId;
        private boolean mExpired = false;
        private final int mTargetMode;
        private final TransitionCallBack mTransitionCallBack;

        DisableMagnificationCallback(TransitionCallBack transitionCallBack, int displayId, int targetMode, float scale, PointF currentCenter, boolean animate) {
            PointF pointF = new PointF();
            this.mCurrentCenter = pointF;
            this.mTransitionCallBack = transitionCallBack;
            this.mDisplayId = displayId;
            this.mTargetMode = targetMode;
            this.mCurrentMode = targetMode ^ 3;
            this.mCurrentScale = scale;
            pointF.set(currentCenter);
            this.mAnimate = animate;
        }

        public void onResult(boolean success) {
            synchronized (MagnificationController.this.mLock) {
                if (this.mExpired) {
                    return;
                }
                setExpiredAndRemoveFromListLocked();
                MagnificationController.this.setTransitionState(Integer.valueOf(this.mDisplayId), null);
                if (success) {
                    adjustCurrentCenterIfNeededLocked();
                    applyMagnificationModeLocked(this.mTargetMode);
                } else {
                    FullScreenMagnificationController screenMagnificationController = MagnificationController.this.getFullScreenMagnificationController();
                    if (this.mCurrentMode == 1 && !screenMagnificationController.isMagnifying(this.mDisplayId)) {
                        MagnificationConfig.Builder configBuilder = new MagnificationConfig.Builder();
                        Region region = new Region();
                        configBuilder.setMode(1).setScale(screenMagnificationController.getScale(this.mDisplayId)).setCenterX(screenMagnificationController.getCenterX(this.mDisplayId)).setCenterY(screenMagnificationController.getCenterY(this.mDisplayId));
                        screenMagnificationController.getMagnificationRegion(this.mDisplayId, region);
                        MagnificationController.this.mAms.notifyMagnificationChanged(this.mDisplayId, region, configBuilder.build());
                    }
                }
                MagnificationController.this.updateMagnificationButton(this.mDisplayId, this.mTargetMode);
                TransitionCallBack transitionCallBack = this.mTransitionCallBack;
                if (transitionCallBack != null) {
                    transitionCallBack.onResult(this.mDisplayId, success);
                }
            }
        }

        private void adjustCurrentCenterIfNeededLocked() {
            if (this.mTargetMode == 2) {
                return;
            }
            Region outRegion = new Region();
            MagnificationController.this.getFullScreenMagnificationController().getMagnificationRegion(this.mDisplayId, outRegion);
            if (outRegion.contains((int) this.mCurrentCenter.x, (int) this.mCurrentCenter.y)) {
                return;
            }
            Rect bounds = outRegion.getBounds();
            this.mCurrentCenter.set(bounds.exactCenterX(), bounds.exactCenterY());
        }

        void restoreToCurrentMagnificationMode() {
            synchronized (MagnificationController.this.mLock) {
                if (this.mExpired) {
                    return;
                }
                setExpiredAndRemoveFromListLocked();
                MagnificationController.this.setTransitionState(Integer.valueOf(this.mDisplayId), null);
                applyMagnificationModeLocked(this.mCurrentMode);
                MagnificationController.this.updateMagnificationButton(this.mDisplayId, this.mCurrentMode);
                TransitionCallBack transitionCallBack = this.mTransitionCallBack;
                if (transitionCallBack != null) {
                    transitionCallBack.onResult(this.mDisplayId, true);
                }
            }
        }

        void setExpiredAndRemoveFromListLocked() {
            this.mExpired = true;
            MagnificationController.this.setDisableMagnificationCallbackLocked(this.mDisplayId, null);
        }

        private void applyMagnificationModeLocked(int mode) {
            if (mode == 1) {
                FullScreenMagnificationController fullScreenMagnificationController = MagnificationController.this.getFullScreenMagnificationController();
                if (!fullScreenMagnificationController.isRegistered(this.mDisplayId)) {
                    fullScreenMagnificationController.register(this.mDisplayId);
                }
                fullScreenMagnificationController.setScaleAndCenter(this.mDisplayId, this.mCurrentScale, this.mCurrentCenter.x, this.mCurrentCenter.y, this.mAnimate, 0);
                return;
            }
            MagnificationController.this.getWindowMagnificationMgr().enableWindowMagnification(this.mDisplayId, this.mCurrentScale, this.mCurrentCenter.x, this.mCurrentCenter.y, this.mAnimate ? STUB_ANIMATION_CALLBACK : null, 0);
        }
    }
}
