package com.android.server.accessibility.magnification;

import android.accessibilityservice.MagnificationConfig;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.DisplayInfo;
import android.view.MagnificationSpec;
import android.view.accessibility.MagnificationAnimationCallback;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.accessibility.AccessibilityTraceManager;
import com.android.server.accessibility.magnification.FullScreenMagnificationController;
import com.android.server.wm.WindowManagerInternal;
import java.util.Locale;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class FullScreenMagnificationController implements WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SET_MAGNIFICATION_SPEC = false;
    private static final String LOG_TAG = "FullScreenMagnificationController";
    private final ControllerContext mControllerCtx;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private final SparseArray<DisplayMagnification> mDisplays;
    private final Object mLock;
    private boolean mMagnificationFollowTypingEnabled;
    private final MagnificationInfoChangedCallback mMagnificationInfoChangedCallback;
    private final long mMainThreadId;
    private final MagnificationScaleProvider mScaleProvider;
    private final ScreenStateObserver mScreenStateObserver;
    private final Rect mTempRect;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface MagnificationInfoChangedCallback {
        void onFullScreenMagnificationActivationState(int i, boolean z);

        void onFullScreenMagnificationChanged(int i, Region region, MagnificationConfig magnificationConfig);

        void onImeWindowVisibilityChanged(int i, boolean z);

        void onRequestMagnificationSpec(int i, int i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayMagnification implements WindowManagerInternal.MagnificationCallbacks {
        private boolean mDeleteAfterUnregister;
        private final int mDisplayId;
        private boolean mForceShowMagnifiableBounds;
        private boolean mRegistered;
        private final SpecAnimationBridge mSpecAnimationBridge;
        private boolean mUnregisterPending;
        private final MagnificationSpec mCurrentMagnificationSpec = new MagnificationSpec();
        private final Region mMagnificationRegion = Region.obtain();
        private final Rect mMagnificationBounds = new Rect();
        private final Rect mTempRect = new Rect();
        private final Rect mTempRect1 = new Rect();
        private int mIdOfLastServiceToMagnify = -1;
        private boolean mMagnificationActivated = false;

        DisplayMagnification(int displayId) {
            this.mDisplayId = displayId;
            this.mSpecAnimationBridge = new SpecAnimationBridge(FullScreenMagnificationController.this.mControllerCtx, FullScreenMagnificationController.this.mLock, displayId);
        }

        boolean register() {
            if (FullScreenMagnificationController.this.traceEnabled()) {
                FullScreenMagnificationController.this.logTrace("setMagnificationCallbacks", "displayID=" + this.mDisplayId + ";callback=" + this);
            }
            boolean magnificationCallbacks = FullScreenMagnificationController.this.mControllerCtx.getWindowManager().setMagnificationCallbacks(this.mDisplayId, this);
            this.mRegistered = magnificationCallbacks;
            if (!magnificationCallbacks) {
                Slog.w(FullScreenMagnificationController.LOG_TAG, "set magnification callbacks fail, displayId:" + this.mDisplayId);
                return false;
            }
            this.mSpecAnimationBridge.setEnabled(true);
            if (FullScreenMagnificationController.this.traceEnabled()) {
                FullScreenMagnificationController.this.logTrace("getMagnificationRegion", "displayID=" + this.mDisplayId + ";region=" + this.mMagnificationRegion);
            }
            FullScreenMagnificationController.this.mControllerCtx.getWindowManager().getMagnificationRegion(this.mDisplayId, this.mMagnificationRegion);
            this.mMagnificationRegion.getBounds(this.mMagnificationBounds);
            return true;
        }

        void unregister(boolean delete) {
            if (this.mRegistered) {
                this.mSpecAnimationBridge.setEnabled(false);
                if (FullScreenMagnificationController.this.traceEnabled()) {
                    FullScreenMagnificationController.this.logTrace("setMagnificationCallbacks", "displayID=" + this.mDisplayId + ";callback=null");
                }
                FullScreenMagnificationController.this.mControllerCtx.getWindowManager().setMagnificationCallbacks(this.mDisplayId, null);
                this.mMagnificationRegion.setEmpty();
                this.mRegistered = false;
                FullScreenMagnificationController.this.unregisterCallbackLocked(this.mDisplayId, delete);
            }
            this.mUnregisterPending = false;
        }

        void unregisterPending(boolean delete) {
            this.mDeleteAfterUnregister = delete;
            this.mUnregisterPending = true;
            reset(true);
        }

        boolean isRegistered() {
            return this.mRegistered;
        }

        boolean isMagnifying() {
            return this.mCurrentMagnificationSpec.scale > 1.0f;
        }

        float getScale() {
            return this.mCurrentMagnificationSpec.scale;
        }

        float getOffsetX() {
            return this.mCurrentMagnificationSpec.offsetX;
        }

        float getOffsetY() {
            return this.mCurrentMagnificationSpec.offsetY;
        }

        float getCenterX() {
            return (((this.mMagnificationBounds.width() / 2.0f) + this.mMagnificationBounds.left) - getOffsetX()) / getScale();
        }

        float getCenterY() {
            return (((this.mMagnificationBounds.height() / 2.0f) + this.mMagnificationBounds.top) - getOffsetY()) / getScale();
        }

        float getSentScale() {
            return this.mSpecAnimationBridge.mSentMagnificationSpec.scale;
        }

        float getSentOffsetX() {
            return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetX;
        }

        float getSentOffsetY() {
            return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetY;
        }

        @Override // com.android.server.wm.WindowManagerInternal.MagnificationCallbacks
        public void onMagnificationRegionChanged(Region magnificationRegion) {
            Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.magnification.FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda4
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((FullScreenMagnificationController.DisplayMagnification) obj).updateMagnificationRegion((Region) obj2);
                }
            }, this, Region.obtain(magnificationRegion));
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        @Override // com.android.server.wm.WindowManagerInternal.MagnificationCallbacks
        public void onRectangleOnScreenRequested(int left, int top, int right, int bottom) {
            Message m = PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.accessibility.magnification.FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda1
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((FullScreenMagnificationController.DisplayMagnification) obj).requestRectangleOnScreen(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), ((Integer) obj5).intValue());
                }
            }, this, Integer.valueOf(left), Integer.valueOf(top), Integer.valueOf(right), Integer.valueOf(bottom));
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        @Override // com.android.server.wm.WindowManagerInternal.MagnificationCallbacks
        public void onDisplaySizeChanged() {
            Message m = PooledLambda.obtainMessage(new FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda0(), FullScreenMagnificationController.this, Integer.valueOf(this.mDisplayId), true);
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        @Override // com.android.server.wm.WindowManagerInternal.MagnificationCallbacks
        public void onUserContextChanged() {
            Message m = PooledLambda.obtainMessage(new FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda0(), FullScreenMagnificationController.this, Integer.valueOf(this.mDisplayId), true);
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        @Override // com.android.server.wm.WindowManagerInternal.MagnificationCallbacks
        public void onImeWindowVisibilityChanged(boolean shown) {
            Message m = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.magnification.FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda3
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((FullScreenMagnificationController) obj).notifyImeWindowVisibilityChanged(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue());
                }
            }, FullScreenMagnificationController.this, Integer.valueOf(this.mDisplayId), Boolean.valueOf(shown));
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void updateMagnificationRegion(Region magnified) {
            synchronized (FullScreenMagnificationController.this.mLock) {
                if (this.mRegistered) {
                    if (!this.mMagnificationRegion.equals(magnified)) {
                        this.mMagnificationRegion.set(magnified);
                        this.mMagnificationRegion.getBounds(this.mMagnificationBounds);
                        if (updateCurrentSpecWithOffsetsLocked(this.mCurrentMagnificationSpec.offsetX, this.mCurrentMagnificationSpec.offsetY)) {
                            sendSpecToAnimation(this.mCurrentMagnificationSpec, null);
                        }
                        onMagnificationChangedLocked();
                    }
                    magnified.recycle();
                }
            }
        }

        void sendSpecToAnimation(MagnificationSpec spec, MagnificationAnimationCallback animationCallback) {
            if (Thread.currentThread().getId() == FullScreenMagnificationController.this.mMainThreadId) {
                this.mSpecAnimationBridge.updateSentSpecMainThread(spec, animationCallback);
                return;
            }
            Message m = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.accessibility.magnification.FullScreenMagnificationController$DisplayMagnification$$ExternalSyntheticLambda2
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((FullScreenMagnificationController.SpecAnimationBridge) obj).updateSentSpecMainThread((MagnificationSpec) obj2, (MagnificationAnimationCallback) obj3);
                }
            }, this.mSpecAnimationBridge, spec, animationCallback);
            FullScreenMagnificationController.this.mControllerCtx.getHandler().sendMessage(m);
        }

        int getIdOfLastServiceToMagnify() {
            return this.mIdOfLastServiceToMagnify;
        }

        void onMagnificationChangedLocked() {
            float scale = getScale();
            boolean lastMagnificationActivated = this.mMagnificationActivated;
            boolean z = scale > 1.0f;
            this.mMagnificationActivated = z;
            if (z != lastMagnificationActivated) {
                FullScreenMagnificationController.this.mMagnificationInfoChangedCallback.onFullScreenMagnificationActivationState(this.mDisplayId, this.mMagnificationActivated);
            }
            MagnificationConfig config = new MagnificationConfig.Builder().setMode(1).setScale(scale).setCenterX(getCenterX()).setCenterY(getCenterY()).build();
            FullScreenMagnificationController.this.mMagnificationInfoChangedCallback.onFullScreenMagnificationChanged(this.mDisplayId, this.mMagnificationRegion, config);
            if (this.mUnregisterPending && !isMagnifying()) {
                unregister(this.mDeleteAfterUnregister);
            }
        }

        boolean magnificationRegionContains(float x, float y) {
            return this.mMagnificationRegion.contains((int) x, (int) y);
        }

        void getMagnificationBounds(Rect outBounds) {
            outBounds.set(this.mMagnificationBounds);
        }

        void getMagnificationRegion(Region outRegion) {
            outRegion.set(this.mMagnificationRegion);
        }

        private DisplayMetrics getDisplayMetricsForId() {
            DisplayMetrics outMetrics = new DisplayMetrics();
            DisplayInfo displayInfo = FullScreenMagnificationController.this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
            if (displayInfo != null) {
                displayInfo.getLogicalMetrics(outMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, (Configuration) null);
            } else {
                outMetrics.setToDefaults();
            }
            return outMetrics;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void requestRectangleOnScreen(int left, int top, int right, int bottom) {
            float scrollX;
            float scrollY;
            synchronized (FullScreenMagnificationController.this.mLock) {
                Rect magnifiedFrame = this.mTempRect;
                getMagnificationBounds(magnifiedFrame);
                if (magnifiedFrame.intersects(left, top, right, bottom)) {
                    Rect magnifFrameInScreenCoords = this.mTempRect1;
                    getMagnifiedFrameInContentCoordsLocked(magnifFrameInScreenCoords);
                    DisplayMetrics metrics = getDisplayMetricsForId();
                    float offsetViewportX = magnifFrameInScreenCoords.width() / 4.0f;
                    float offsetViewportY = TypedValue.applyDimension(1, 10.0f, metrics);
                    if (right - left > magnifFrameInScreenCoords.width()) {
                        int direction = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
                        if (direction == 0) {
                            scrollX = left - magnifFrameInScreenCoords.left;
                        } else {
                            scrollX = right - magnifFrameInScreenCoords.right;
                        }
                    } else if (left < magnifFrameInScreenCoords.left) {
                        scrollX = (left - magnifFrameInScreenCoords.left) - offsetViewportX;
                    } else if (right > magnifFrameInScreenCoords.right) {
                        scrollX = (right - magnifFrameInScreenCoords.right) + offsetViewportX;
                    } else {
                        scrollX = 0.0f;
                    }
                    if (bottom - top > magnifFrameInScreenCoords.height()) {
                        scrollY = top - magnifFrameInScreenCoords.top;
                    } else if (top < magnifFrameInScreenCoords.top) {
                        scrollY = (top - magnifFrameInScreenCoords.top) - offsetViewportY;
                    } else if (bottom > magnifFrameInScreenCoords.bottom) {
                        scrollY = (bottom - magnifFrameInScreenCoords.bottom) + offsetViewportY;
                    } else {
                        scrollY = 0.0f;
                    }
                    float scale = getScale();
                    offsetMagnifiedRegion(scrollX * scale, scrollY * scale, -1);
                }
            }
        }

        void getMagnifiedFrameInContentCoordsLocked(Rect outFrame) {
            float scale = getSentScale();
            float offsetX = getSentOffsetX();
            float offsetY = getSentOffsetY();
            getMagnificationBounds(outFrame);
            outFrame.offset((int) (-offsetX), (int) (-offsetY));
            outFrame.scale(1.0f / scale);
        }

        void setForceShowMagnifiableBounds(boolean show) {
            if (this.mRegistered) {
                this.mForceShowMagnifiableBounds = show;
                if (FullScreenMagnificationController.this.traceEnabled()) {
                    FullScreenMagnificationController.this.logTrace("setForceShowMagnifiableBounds", "displayID=" + this.mDisplayId + ";show=" + show);
                }
                FullScreenMagnificationController.this.mControllerCtx.getWindowManager().setForceShowMagnifiableBounds(this.mDisplayId, show);
            }
        }

        boolean isForceShowMagnifiableBounds() {
            return this.mRegistered && this.mForceShowMagnifiableBounds;
        }

        boolean reset(boolean animate) {
            return reset(FullScreenMagnificationController.transformToStubCallback(animate));
        }

        boolean reset(MagnificationAnimationCallback animationCallback) {
            if (this.mRegistered) {
                MagnificationSpec spec = this.mCurrentMagnificationSpec;
                boolean changed = !spec.isNop();
                if (changed) {
                    spec.clear();
                    onMagnificationChangedLocked();
                }
                this.mIdOfLastServiceToMagnify = -1;
                this.mForceShowMagnifiableBounds = false;
                sendSpecToAnimation(spec, animationCallback);
                return changed;
            }
            return false;
        }

        boolean setScale(float scale, float pivotX, float pivotY, boolean animate, int id) {
            if (!this.mRegistered) {
                return false;
            }
            float scale2 = MagnificationScaleProvider.constrainScale(scale);
            Rect viewport = this.mTempRect;
            this.mMagnificationRegion.getBounds(viewport);
            MagnificationSpec spec = this.mCurrentMagnificationSpec;
            float oldScale = spec.scale;
            float oldCenterX = (((viewport.width() / 2.0f) - spec.offsetX) + viewport.left) / oldScale;
            float oldCenterY = (((viewport.height() / 2.0f) - spec.offsetY) + viewport.top) / oldScale;
            float normPivotX = (pivotX - spec.offsetX) / oldScale;
            float normPivotY = (pivotY - spec.offsetY) / oldScale;
            float offsetX = (oldCenterX - normPivotX) * (oldScale / scale2);
            float offsetY = (oldCenterY - normPivotY) * (oldScale / scale2);
            float centerX = normPivotX + offsetX;
            float centerY = normPivotY + offsetY;
            this.mIdOfLastServiceToMagnify = id;
            return setScaleAndCenter(scale2, centerX, centerY, FullScreenMagnificationController.transformToStubCallback(animate), id);
        }

        boolean setScaleAndCenter(float scale, float centerX, float centerY, MagnificationAnimationCallback animationCallback, int id) {
            if (!this.mRegistered) {
                return false;
            }
            boolean changed = updateMagnificationSpecLocked(scale, centerX, centerY);
            sendSpecToAnimation(this.mCurrentMagnificationSpec, animationCallback);
            if (isMagnifying() && id != -1) {
                this.mIdOfLastServiceToMagnify = id;
                FullScreenMagnificationController.this.mMagnificationInfoChangedCallback.onRequestMagnificationSpec(this.mDisplayId, this.mIdOfLastServiceToMagnify);
            }
            return changed;
        }

        boolean updateMagnificationSpecLocked(float scale, float centerX, float centerY) {
            if (Float.isNaN(centerX)) {
                centerX = getCenterX();
            }
            if (Float.isNaN(centerY)) {
                centerY = getCenterY();
            }
            if (Float.isNaN(scale)) {
                scale = getScale();
            }
            boolean changed = false;
            float normScale = MagnificationScaleProvider.constrainScale(scale);
            if (Float.compare(this.mCurrentMagnificationSpec.scale, normScale) != 0) {
                this.mCurrentMagnificationSpec.scale = normScale;
                changed = true;
            }
            float nonNormOffsetX = ((this.mMagnificationBounds.width() / 2.0f) + this.mMagnificationBounds.left) - (centerX * normScale);
            float nonNormOffsetY = ((this.mMagnificationBounds.height() / 2.0f) + this.mMagnificationBounds.top) - (centerY * normScale);
            boolean changed2 = changed | updateCurrentSpecWithOffsetsLocked(nonNormOffsetX, nonNormOffsetY);
            if (changed2) {
                onMagnificationChangedLocked();
            }
            return changed2;
        }

        void offsetMagnifiedRegion(float offsetX, float offsetY, int id) {
            if (!this.mRegistered) {
                return;
            }
            float nonNormOffsetX = this.mCurrentMagnificationSpec.offsetX - offsetX;
            float nonNormOffsetY = this.mCurrentMagnificationSpec.offsetY - offsetY;
            if (updateCurrentSpecWithOffsetsLocked(nonNormOffsetX, nonNormOffsetY)) {
                onMagnificationChangedLocked();
            }
            if (id != -1) {
                this.mIdOfLastServiceToMagnify = id;
            }
            sendSpecToAnimation(this.mCurrentMagnificationSpec, null);
        }

        boolean updateCurrentSpecWithOffsetsLocked(float nonNormOffsetX, float nonNormOffsetY) {
            boolean changed = false;
            float offsetX = MathUtils.constrain(nonNormOffsetX, getMinOffsetXLocked(), getMaxOffsetXLocked());
            if (Float.compare(this.mCurrentMagnificationSpec.offsetX, offsetX) != 0) {
                this.mCurrentMagnificationSpec.offsetX = offsetX;
                changed = true;
            }
            float offsetY = MathUtils.constrain(nonNormOffsetY, getMinOffsetYLocked(), getMaxOffsetYLocked());
            if (Float.compare(this.mCurrentMagnificationSpec.offsetY, offsetY) != 0) {
                this.mCurrentMagnificationSpec.offsetY = offsetY;
                return true;
            }
            return changed;
        }

        float getMinOffsetXLocked() {
            float viewportWidth = this.mMagnificationBounds.width();
            float viewportLeft = this.mMagnificationBounds.left;
            return (viewportLeft + viewportWidth) - ((viewportLeft + viewportWidth) * this.mCurrentMagnificationSpec.scale);
        }

        float getMaxOffsetXLocked() {
            return this.mMagnificationBounds.left - (this.mMagnificationBounds.left * this.mCurrentMagnificationSpec.scale);
        }

        float getMinOffsetYLocked() {
            float viewportHeight = this.mMagnificationBounds.height();
            float viewportTop = this.mMagnificationBounds.top;
            return (viewportTop + viewportHeight) - ((viewportTop + viewportHeight) * this.mCurrentMagnificationSpec.scale);
        }

        float getMaxOffsetYLocked() {
            return this.mMagnificationBounds.top - (this.mMagnificationBounds.top * this.mCurrentMagnificationSpec.scale);
        }

        public String toString() {
            return "DisplayMagnification[mCurrentMagnificationSpec=" + this.mCurrentMagnificationSpec + ", mMagnificationRegion=" + this.mMagnificationRegion + ", mMagnificationBounds=" + this.mMagnificationBounds + ", mDisplayId=" + this.mDisplayId + ", mIdOfLastServiceToMagnify=" + this.mIdOfLastServiceToMagnify + ", mRegistered=" + this.mRegistered + ", mUnregisterPending=" + this.mUnregisterPending + ']';
        }
    }

    public FullScreenMagnificationController(Context context, AccessibilityTraceManager traceManager, Object lock, MagnificationInfoChangedCallback magnificationInfoChangedCallback, MagnificationScaleProvider scaleProvider) {
        this(new ControllerContext(context, traceManager, (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class), new Handler(context.getMainLooper()), context.getResources().getInteger(17694722)), lock, magnificationInfoChangedCallback, scaleProvider);
    }

    public FullScreenMagnificationController(ControllerContext ctx, Object lock, MagnificationInfoChangedCallback magnificationInfoChangedCallback, MagnificationScaleProvider scaleProvider) {
        this.mDisplays = new SparseArray<>(0);
        this.mTempRect = new Rect();
        this.mMagnificationFollowTypingEnabled = true;
        this.mControllerCtx = ctx;
        this.mLock = lock;
        this.mMainThreadId = ctx.getContext().getMainLooper().getThread().getId();
        this.mScreenStateObserver = new ScreenStateObserver(ctx.getContext(), this);
        this.mMagnificationInfoChangedCallback = magnificationInfoChangedCallback;
        this.mScaleProvider = scaleProvider;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    }

    public void register(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                display = new DisplayMagnification(displayId);
            }
            if (display.isRegistered()) {
                return;
            }
            if (display.register()) {
                this.mDisplays.put(displayId, display);
                this.mScreenStateObserver.registerIfNecessary();
            }
        }
    }

    public void unregister(int displayId) {
        synchronized (this.mLock) {
            unregisterLocked(displayId, false);
        }
    }

    public void unregisterAll() {
        synchronized (this.mLock) {
            SparseArray<DisplayMagnification> displays = this.mDisplays.clone();
            for (int i = 0; i < displays.size(); i++) {
                unregisterLocked(displays.keyAt(i), false);
            }
        }
    }

    @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks
    public void onRectangleOnScreenRequested(int displayId, int left, int top, int right, int bottom) {
        synchronized (this.mLock) {
            if (this.mMagnificationFollowTypingEnabled) {
                DisplayMagnification display = this.mDisplays.get(displayId);
                if (display == null) {
                    return;
                }
                if (display.isMagnifying()) {
                    Rect magnifiedRegionBounds = this.mTempRect;
                    display.getMagnifiedFrameInContentCoordsLocked(magnifiedRegionBounds);
                    if (magnifiedRegionBounds.contains(left, top, right, bottom)) {
                        return;
                    }
                    display.onRectangleOnScreenRequested(left, top, right, bottom);
                }
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

    public void onDisplayRemoved(int displayId) {
        synchronized (this.mLock) {
            unregisterLocked(displayId, true);
        }
    }

    public boolean isRegistered(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.isRegistered();
        }
    }

    public boolean isMagnifying(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.isMagnifying();
        }
    }

    public boolean magnificationRegionContains(int displayId, float x, float y) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.magnificationRegionContains(x, y);
        }
    }

    public void getMagnificationBounds(int displayId, Rect outBounds) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return;
            }
            display.getMagnificationBounds(outBounds);
        }
    }

    public void getMagnificationRegion(int displayId, Region outRegion) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return;
            }
            display.getMagnificationRegion(outRegion);
        }
    }

    public float getScale(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return 1.0f;
            }
            return display.getScale();
        }
    }

    public float getOffsetX(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return 0.0f;
            }
            return display.getOffsetX();
        }
    }

    public float getCenterX(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return 0.0f;
            }
            return display.getCenterX();
        }
    }

    public float getOffsetY(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return 0.0f;
            }
            return display.getOffsetY();
        }
    }

    public float getCenterY(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return 0.0f;
            }
            return display.getCenterY();
        }
    }

    public boolean reset(int displayId, boolean animate) {
        return reset(displayId, animate ? MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK : null);
    }

    public boolean reset(int displayId, MagnificationAnimationCallback animationCallback) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.reset(animationCallback);
        }
    }

    public boolean setScale(int displayId, float scale, float pivotX, float pivotY, boolean animate, int id) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.setScale(scale, pivotX, pivotY, animate, id);
        }
    }

    public boolean setCenter(int displayId, float centerX, float centerY, boolean animate, int id) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.setScaleAndCenter(Float.NaN, centerX, centerY, animate ? MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK : null, id);
        }
    }

    public boolean setScaleAndCenter(int displayId, float scale, float centerX, float centerY, boolean animate, int id) {
        return setScaleAndCenter(displayId, scale, centerX, centerY, transformToStubCallback(animate), id);
    }

    public boolean setScaleAndCenter(int displayId, float scale, float centerX, float centerY, MagnificationAnimationCallback animationCallback, int id) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.setScaleAndCenter(scale, centerX, centerY, animationCallback, id);
        }
    }

    public void offsetMagnifiedRegion(int displayId, float offsetX, float offsetY, int id) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return;
            }
            display.offsetMagnifiedRegion(offsetX, offsetY, id);
        }
    }

    public int getIdOfLastServiceToMagnify(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return -1;
            }
            return display.getIdOfLastServiceToMagnify();
        }
    }

    public void persistScale(int displayId) {
        float scale = getScale(0);
        this.mScaleProvider.putScale(scale, displayId);
    }

    public float getPersistedScale(int displayId) {
        return this.mScaleProvider.getScale(displayId);
    }

    public void resetAllIfNeeded(int connectionId) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mDisplays.size(); i++) {
                resetIfNeeded(this.mDisplays.keyAt(i), connectionId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resetIfNeeded(int displayId, boolean animate) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display != null && display.isMagnifying()) {
                display.reset(animate);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resetIfNeeded(int displayId, int connectionId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display != null && display.isMagnifying() && connectionId == display.getIdOfLastServiceToMagnify()) {
                display.reset(true);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceShowMagnifiableBounds(int displayId, boolean show) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return;
            }
            display.setForceShowMagnifiableBounds(show);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyImeWindowVisibilityChanged(int displayId, boolean shown) {
        this.mMagnificationInfoChangedCallback.onImeWindowVisibilityChanged(displayId, shown);
    }

    public boolean isForceShowMagnifiableBounds(int displayId) {
        synchronized (this.mLock) {
            DisplayMagnification display = this.mDisplays.get(displayId);
            if (display == null) {
                return false;
            }
            return display.isForceShowMagnifiableBounds();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onScreenTurnedOff() {
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.magnification.FullScreenMagnificationController$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((FullScreenMagnificationController) obj).resetAllIfNeeded(((Boolean) obj2).booleanValue());
            }
        }, this, false);
        this.mControllerCtx.getHandler().sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetAllIfNeeded(boolean animate) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mDisplays.size(); i++) {
                resetIfNeeded(this.mDisplays.keyAt(i), animate);
            }
        }
    }

    private void unregisterLocked(int displayId, boolean delete) {
        DisplayMagnification display = this.mDisplays.get(displayId);
        if (display == null) {
            return;
        }
        if (!display.isRegistered()) {
            if (delete) {
                this.mDisplays.remove(displayId);
            }
        } else if (!display.isMagnifying()) {
            display.unregister(delete);
        } else {
            display.unregisterPending(delete);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterCallbackLocked(int displayId, boolean delete) {
        if (delete) {
            this.mDisplays.remove(displayId);
        }
        boolean hasRegister = false;
        for (int i = 0; i < this.mDisplays.size(); i++) {
            DisplayMagnification display = this.mDisplays.valueAt(i);
            hasRegister = display.isRegistered();
            if (hasRegister) {
                break;
            }
        }
        if (!hasRegister) {
            this.mScreenStateObserver.unregister();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean traceEnabled() {
        return this.mControllerCtx.getTraceManager().isA11yTracingEnabledForTypes(512L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logTrace(String methodName, String params) {
        this.mControllerCtx.getTraceManager().logTrace("WindowManagerInternal." + methodName, 512L, params);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MagnificationController[");
        builder.append(", mDisplays=").append(this.mDisplays);
        builder.append(", mScaleProvider=").append(this.mScaleProvider);
        builder.append("]");
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SpecAnimationBridge implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private MagnificationAnimationCallback mAnimationCallback;
        private final ControllerContext mControllerCtx;
        private final int mDisplayId;
        private boolean mEnabled;
        private final MagnificationSpec mEndMagnificationSpec;
        private final Object mLock;
        private final MagnificationSpec mSentMagnificationSpec;
        private final MagnificationSpec mStartMagnificationSpec;
        private final ValueAnimator mValueAnimator;

        private SpecAnimationBridge(ControllerContext ctx, Object lock, int displayId) {
            this.mSentMagnificationSpec = new MagnificationSpec();
            this.mStartMagnificationSpec = new MagnificationSpec();
            this.mEndMagnificationSpec = new MagnificationSpec();
            this.mEnabled = false;
            this.mControllerCtx = ctx;
            this.mLock = lock;
            this.mDisplayId = displayId;
            long animationDuration = ctx.getAnimationDuration();
            ValueAnimator newValueAnimator = ctx.newValueAnimator();
            this.mValueAnimator = newValueAnimator;
            newValueAnimator.setDuration(animationDuration);
            newValueAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
            newValueAnimator.setFloatValues(0.0f, 1.0f);
            newValueAnimator.addUpdateListener(this);
            newValueAnimator.addListener(this);
        }

        public void setEnabled(boolean enabled) {
            synchronized (this.mLock) {
                if (enabled != this.mEnabled) {
                    this.mEnabled = enabled;
                    if (!enabled) {
                        this.mSentMagnificationSpec.clear();
                        if (this.mControllerCtx.getTraceManager().isA11yTracingEnabledForTypes(512L)) {
                            this.mControllerCtx.getTraceManager().logTrace("WindowManagerInternal.setMagnificationSpec", 512L, "displayID=" + this.mDisplayId + ";spec=" + this.mSentMagnificationSpec);
                        }
                        this.mControllerCtx.getWindowManager().setMagnificationSpec(this.mDisplayId, this.mSentMagnificationSpec);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void updateSentSpecMainThread(MagnificationSpec spec, MagnificationAnimationCallback animationCallback) {
            if (this.mValueAnimator.isRunning()) {
                this.mValueAnimator.cancel();
            }
            this.mAnimationCallback = animationCallback;
            synchronized (this.mLock) {
                boolean changed = !this.mSentMagnificationSpec.equals(spec);
                if (changed) {
                    if (this.mAnimationCallback != null) {
                        animateMagnificationSpecLocked(spec);
                    } else {
                        setMagnificationSpecLocked(spec);
                    }
                } else {
                    sendEndCallbackMainThread(true);
                }
            }
        }

        private void sendEndCallbackMainThread(boolean success) {
            MagnificationAnimationCallback magnificationAnimationCallback = this.mAnimationCallback;
            if (magnificationAnimationCallback != null) {
                magnificationAnimationCallback.onResult(success);
                this.mAnimationCallback = null;
            }
        }

        private void setMagnificationSpecLocked(MagnificationSpec spec) {
            if (this.mEnabled) {
                this.mSentMagnificationSpec.setTo(spec);
                if (this.mControllerCtx.getTraceManager().isA11yTracingEnabledForTypes(512L)) {
                    this.mControllerCtx.getTraceManager().logTrace("WindowManagerInternal.setMagnificationSpec", 512L, "displayID=" + this.mDisplayId + ";spec=" + this.mSentMagnificationSpec);
                }
                this.mControllerCtx.getWindowManager().setMagnificationSpec(this.mDisplayId, this.mSentMagnificationSpec);
            }
        }

        private void animateMagnificationSpecLocked(MagnificationSpec toSpec) {
            this.mEndMagnificationSpec.setTo(toSpec);
            this.mStartMagnificationSpec.setTo(this.mSentMagnificationSpec);
            this.mValueAnimator.start();
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            synchronized (this.mLock) {
                if (this.mEnabled) {
                    float fract = animation.getAnimatedFraction();
                    MagnificationSpec magnificationSpec = new MagnificationSpec();
                    magnificationSpec.scale = this.mStartMagnificationSpec.scale + ((this.mEndMagnificationSpec.scale - this.mStartMagnificationSpec.scale) * fract);
                    magnificationSpec.offsetX = this.mStartMagnificationSpec.offsetX + ((this.mEndMagnificationSpec.offsetX - this.mStartMagnificationSpec.offsetX) * fract);
                    magnificationSpec.offsetY = this.mStartMagnificationSpec.offsetY + ((this.mEndMagnificationSpec.offsetY - this.mStartMagnificationSpec.offsetY) * fract);
                    setMagnificationSpecLocked(magnificationSpec);
                }
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            sendEndCallbackMainThread(true);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            sendEndCallbackMainThread(false);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ScreenStateObserver extends BroadcastReceiver {
        private final Context mContext;
        private final FullScreenMagnificationController mController;
        private boolean mRegistered = false;

        ScreenStateObserver(Context context, FullScreenMagnificationController controller) {
            this.mContext = context;
            this.mController = controller;
        }

        public void registerIfNecessary() {
            if (!this.mRegistered) {
                this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
                this.mRegistered = true;
            }
        }

        public void unregister() {
            if (this.mRegistered) {
                this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            this.mController.onScreenTurnedOff();
        }
    }

    /* loaded from: classes.dex */
    public static class ControllerContext {
        private final Long mAnimationDuration;
        private final Context mContext;
        private final Handler mHandler;
        private final AccessibilityTraceManager mTrace;
        private final WindowManagerInternal mWindowManager;

        public ControllerContext(Context context, AccessibilityTraceManager traceManager, WindowManagerInternal windowManager, Handler handler, long animationDuration) {
            this.mContext = context;
            this.mTrace = traceManager;
            this.mWindowManager = windowManager;
            this.mHandler = handler;
            this.mAnimationDuration = Long.valueOf(animationDuration);
        }

        public Context getContext() {
            return this.mContext;
        }

        public AccessibilityTraceManager getTraceManager() {
            return this.mTrace;
        }

        public WindowManagerInternal getWindowManager() {
            return this.mWindowManager;
        }

        public Handler getHandler() {
            return this.mHandler;
        }

        public ValueAnimator newValueAnimator() {
            return new ValueAnimator();
        }

        public long getAnimationDuration() {
            return this.mAnimationDuration.longValue();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static MagnificationAnimationCallback transformToStubCallback(boolean animate) {
        if (animate) {
            return MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK;
        }
        return null;
    }
}
