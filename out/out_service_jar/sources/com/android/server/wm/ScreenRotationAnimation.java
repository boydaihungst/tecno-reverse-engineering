package com.android.server.wm;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Trace;
import android.util.RotationUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.SimpleSurfaceAnimatable;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.utils.RotationAnimationUtils;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class ScreenRotationAnimation {
    private static final String TAG = "WindowManager";
    private boolean mAnimRunning;
    private SurfaceControl mBackColorSurface;
    private final Context mContext;
    private int mCurRotation;
    private final DisplayContent mDisplayContent;
    private float mEndLuma;
    private SurfaceControl mEnterBlackFrameLayer;
    private BlackFrame mEnteringBlackFrame;
    private boolean mFinishAnimReady;
    private long mFinishAnimStartTime;
    private final int mOriginalHeight;
    private final int mOriginalRotation;
    private final int mOriginalWidth;
    private Animation mRotateAlphaAnimation;
    private Animation mRotateEnterAnimation;
    private Animation mRotateExitAnimation;
    private SurfaceControl mScreenshotLayer;
    private final WindowManagerService mService;
    private float mStartLuma;
    private boolean mStarted;
    private SurfaceRotationAnimationController mSurfaceRotationAnimationController;
    private final float[] mTmpFloats = new float[9];
    private final Transformation mRotateExitTransformation = new Transformation();
    private final Transformation mRotateEnterTransformation = new Transformation();
    private final Matrix mSnapshotInitialMatrix = new Matrix();

    /* JADX WARN: Removed duplicated region for block: B:127:0x01cb  */
    /* JADX WARN: Removed duplicated region for block: B:128:0x01cd  */
    /* JADX WARN: Removed duplicated region for block: B:131:0x01d2  */
    /* JADX WARN: Removed duplicated region for block: B:132:0x01d4  */
    /* JADX WARN: Removed duplicated region for block: B:142:0x0206  */
    /* JADX WARN: Removed duplicated region for block: B:144:0x021d  */
    /* JADX WARN: Removed duplicated region for block: B:145:0x0221  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ScreenRotationAnimation(DisplayContent displayContent, int originalRotation) {
        int logicalWidth;
        int logicalHeight;
        boolean z;
        int i;
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer;
        WindowManagerService windowManagerService = displayContent.mWmService;
        this.mService = windowManagerService;
        this.mContext = windowManagerService.mContext;
        this.mDisplayContent = displayContent;
        Rect currentBounds = displayContent.getBounds();
        int width = currentBounds.width();
        int height = currentBounds.height();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int realOriginalRotation = displayInfo.rotation;
        this.mOriginalRotation = originalRotation;
        int delta = RotationUtils.deltaRotation(originalRotation, realOriginalRotation);
        boolean flipped = delta == 1 || delta == 3;
        this.mOriginalWidth = flipped ? height : width;
        this.mOriginalHeight = flipped ? width : height;
        this.mSurfaceRotationAnimationController = new SurfaceRotationAnimationController();
        windowManagerService.mPowerHalManager.setRotationBoost(true);
        boolean isSecure = displayContent.hasSecureWindowOnScreen();
        int displayId = displayContent.getDisplayId();
        SurfaceControl.Transaction t = windowManagerService.mTransactionFactory.get();
        try {
            try {
                try {
                    SurfaceControl.LayerCaptureArgs args = new SurfaceControl.LayerCaptureArgs.Builder(displayContent.getSurfaceControl()).setCaptureSecureLayers(true).setAllowProtected(true).setSourceCrop(new Rect(0, 0, width, height)).setExcludeLayers(displayContent.findRoundedCornerOverlays()).build();
                    screenshotBuffer = SurfaceControl.captureLayers(args);
                } catch (Surface.OutOfResourcesException e) {
                    e = e;
                }
            } catch (Surface.OutOfResourcesException e2) {
                e = e2;
            }
        } catch (Surface.OutOfResourcesException e3) {
            e = e3;
        }
        if (screenshotBuffer == null) {
            try {
                Slog.w("WindowManager", "Unable to take screenshot of display " + displayId);
                return;
            } catch (Surface.OutOfResourcesException e4) {
                e = e4;
            }
        } else {
            isSecure = screenshotBuffer.containsSecureLayers() ? true : isSecure;
            this.mBackColorSurface = displayContent.makeChildSurface(null).setName("BackColorSurface").setColorLayer().setCallsite("ScreenRotationAnimation").build();
            SurfaceControl build = displayContent.makeOverlay().setName("RotationLayer").setOpaque(true).setSecure(isSecure).setCallsite("ScreenRotationAnimation").setBLASTLayer().build();
            this.mScreenshotLayer = build;
            InputMonitor.setTrustedOverlayInputInfo(build, t, displayId, "RotationLayer");
            this.mEnterBlackFrameLayer = displayContent.makeOverlay().setName("EnterBlackFrameLayer").setContainerLayer().setCallsite("ScreenRotationAnimation").build();
            HardwareBuffer hardwareBuffer = screenshotBuffer.getHardwareBuffer();
            try {
                Trace.traceBegin(32L, "ScreenRotationAnimation#getMedianBorderLuma");
                this.mStartLuma = RotationAnimationUtils.getMedianBorderLuma(hardwareBuffer, screenshotBuffer.getColorSpace());
                Trace.traceEnd(32L);
                GraphicBuffer buffer = GraphicBuffer.createFromHardwareBuffer(screenshotBuffer.getHardwareBuffer());
                t.setLayer(this.mScreenshotLayer, 2010000);
                t.reparent(this.mBackColorSurface, displayContent.getSurfaceControl());
                t.setDimmingEnabled(this.mScreenshotLayer, !screenshotBuffer.containsHdrLayers());
                t.setLayer(this.mBackColorSurface, -1);
                SurfaceControl surfaceControl = this.mBackColorSurface;
                float f = this.mStartLuma;
                t.setColor(surfaceControl, new float[]{f, f, f});
                t.setAlpha(this.mBackColorSurface, 1.0f);
                t.setBuffer(this.mScreenshotLayer, buffer);
                t.setColorSpace(this.mScreenshotLayer, screenshotBuffer.getColorSpace());
                t.show(this.mScreenshotLayer);
            } catch (Surface.OutOfResourcesException e5) {
                e = e5;
            }
            logicalWidth = displayInfo.logicalWidth;
            logicalHeight = displayInfo.logicalHeight;
            int i2 = this.mOriginalWidth;
            z = logicalWidth <= i2;
            i = this.mOriginalHeight;
            if (z == (logicalHeight <= i) && (logicalWidth != i2 || logicalHeight != i)) {
                displayContent.getPendingTransaction().setGeometry(this.mScreenshotLayer, new Rect(0, 0, this.mOriginalWidth, this.mOriginalHeight), new Rect(0, 0, logicalWidth, logicalHeight), 0);
            }
            if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                String protoLogParam0 = String.valueOf(this.mScreenshotLayer);
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 10608884, 0, (String) null, new Object[]{protoLogParam0});
            }
            if (originalRotation != realOriginalRotation) {
                setRotation(t, realOriginalRotation);
            } else {
                this.mCurRotation = realOriginalRotation;
                this.mSnapshotInitialMatrix.reset();
                setRotationTransform(t, this.mSnapshotInitialMatrix);
            }
            t.apply();
        }
        Slog.w("WindowManager", "Unable to allocate freeze surface", e);
        logicalWidth = displayInfo.logicalWidth;
        logicalHeight = displayInfo.logicalHeight;
        int i22 = this.mOriginalWidth;
        if (logicalWidth <= i22) {
        }
        i = this.mOriginalHeight;
        if (z == (logicalHeight <= i)) {
            displayContent.getPendingTransaction().setGeometry(this.mScreenshotLayer, new Rect(0, 0, this.mOriginalWidth, this.mOriginalHeight), new Rect(0, 0, logicalWidth, logicalHeight), 0);
        }
        if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
        }
        if (originalRotation != realOriginalRotation) {
        }
        t.apply();
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mStarted);
        proto.write(1133871366146L, this.mAnimRunning);
        proto.end(token);
    }

    public boolean hasScreenshot() {
        return this.mScreenshotLayer != null;
    }

    private void setRotationTransform(SurfaceControl.Transaction t, Matrix matrix) {
        if (this.mScreenshotLayer == null) {
            return;
        }
        matrix.getValues(this.mTmpFloats);
        float[] fArr = this.mTmpFloats;
        float x = fArr[2];
        float y = fArr[5];
        t.setPosition(this.mScreenshotLayer, x, y);
        SurfaceControl surfaceControl = this.mScreenshotLayer;
        float[] fArr2 = this.mTmpFloats;
        t.setMatrix(surfaceControl, fArr2[0], fArr2[3], fArr2[1], fArr2[4]);
        t.setAlpha(this.mScreenshotLayer, 1.0f);
        t.show(this.mScreenshotLayer);
    }

    public void printTo(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mSurface=");
        pw.print(this.mScreenshotLayer);
        pw.print(prefix);
        pw.print("mEnteringBlackFrame=");
        pw.println(this.mEnteringBlackFrame);
        BlackFrame blackFrame = this.mEnteringBlackFrame;
        if (blackFrame != null) {
            blackFrame.printTo(prefix + "  ", pw);
        }
        pw.print(prefix);
        pw.print("mCurRotation=");
        pw.print(this.mCurRotation);
        pw.print(" mOriginalRotation=");
        pw.println(this.mOriginalRotation);
        pw.print(prefix);
        pw.print("mOriginalWidth=");
        pw.print(this.mOriginalWidth);
        pw.print(" mOriginalHeight=");
        pw.println(this.mOriginalHeight);
        pw.print(prefix);
        pw.print("mStarted=");
        pw.print(this.mStarted);
        pw.print(" mAnimRunning=");
        pw.print(this.mAnimRunning);
        pw.print(" mFinishAnimReady=");
        pw.print(this.mFinishAnimReady);
        pw.print(" mFinishAnimStartTime=");
        pw.println(this.mFinishAnimStartTime);
        pw.print(prefix);
        pw.print("mRotateExitAnimation=");
        pw.print(this.mRotateExitAnimation);
        pw.print(" ");
        this.mRotateExitTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mRotateEnterAnimation=");
        pw.print(this.mRotateEnterAnimation);
        pw.print(" ");
        this.mRotateEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mSnapshotInitialMatrix=");
        this.mSnapshotInitialMatrix.dump(pw);
        pw.println();
    }

    public void setRotation(SurfaceControl.Transaction t, int rotation) {
        this.mCurRotation = rotation;
        int delta = RotationUtils.deltaRotation(rotation, this.mOriginalRotation);
        RotationAnimationUtils.createRotationMatrix(delta, this.mOriginalWidth, this.mOriginalHeight, this.mSnapshotInitialMatrix);
        setRotationTransform(t, this.mSnapshotInitialMatrix);
    }

    private void adjustScaleAnimation(AnimationSet animationSet, boolean exit) {
        List<Animation> animations = animationSet.getAnimations();
        if (animations != null) {
            for (int i = 0; i < animations.size(); i++) {
                Animation animation = animations.get(i);
                if (animation instanceof ScaleAnimation) {
                    ScaleAnimation scaleAnim = (ScaleAnimation) animation;
                    if (exit) {
                        scaleAnim.setToX(this.mOriginalWidth / this.mOriginalHeight);
                        scaleAnim.setToY(this.mOriginalHeight / this.mOriginalWidth);
                    } else {
                        scaleAnim.setFromX(this.mOriginalWidth / this.mOriginalHeight);
                        scaleAnim.setFromY(this.mOriginalHeight / this.mOriginalWidth);
                    }
                }
            }
        }
    }

    private boolean startAnimation(SurfaceControl.Transaction t, long maxAnimationDuration, float animationScale, int finalWidth, int finalHeight, int exitAnim, int enterAnim) {
        boolean customAnim;
        if (this.mScreenshotLayer == null) {
            return false;
        }
        if (this.mStarted) {
            return true;
        }
        this.mStarted = true;
        int delta = RotationUtils.deltaRotation(this.mCurRotation, this.mOriginalRotation);
        if (exitAnim != 0 && enterAnim != 0) {
            customAnim = true;
            this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, exitAnim);
            this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, enterAnim);
            this.mRotateAlphaAnimation = AnimationUtils.loadAnimation(this.mContext, 17432727);
        } else {
            customAnim = false;
            if (WindowManagerService.mTranScreenRotation) {
                switch (delta) {
                    case 0:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432721);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432717);
                        break;
                    case 1:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432738);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432736);
                        break;
                    case 2:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432725);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432723);
                        break;
                    case 3:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432734);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432732);
                        break;
                }
                if (delta == 1 || delta == 3) {
                    adjustScaleAnimation((AnimationSet) this.mRotateExitAnimation, true);
                    adjustScaleAnimation((AnimationSet) this.mRotateEnterAnimation, false);
                }
            } else {
                switch (delta) {
                    case 0:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432721);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432717);
                        break;
                    case 1:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432737);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432735);
                        break;
                    case 2:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432724);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432722);
                        break;
                    case 3:
                        this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, 17432733);
                        this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, 17432731);
                        break;
                }
            }
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam0 = String.valueOf(customAnim);
            String protoLogParam1 = String.valueOf(Surface.rotationToString(this.mCurRotation));
            String protoLogParam2 = String.valueOf(Surface.rotationToString(this.mOriginalRotation));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, -177040661, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
        this.mRotateExitAnimation.initialize(finalWidth, finalHeight, this.mOriginalWidth, this.mOriginalHeight);
        this.mRotateExitAnimation.restrictDuration(maxAnimationDuration);
        this.mRotateExitAnimation.scaleCurrentDuration(animationScale);
        this.mRotateEnterAnimation.initialize(finalWidth, finalHeight, this.mOriginalWidth, this.mOriginalHeight);
        this.mRotateEnterAnimation.restrictDuration(maxAnimationDuration);
        this.mRotateEnterAnimation.scaleCurrentDuration(animationScale);
        this.mAnimRunning = false;
        this.mFinishAnimReady = false;
        this.mFinishAnimStartTime = -1L;
        if (customAnim) {
            this.mRotateAlphaAnimation.restrictDuration(maxAnimationDuration);
            this.mRotateAlphaAnimation.scaleCurrentDuration(animationScale);
        }
        if (customAnim && this.mEnteringBlackFrame == null) {
            try {
                Rect outer = new Rect(-finalWidth, -finalHeight, finalWidth * 2, finalHeight * 2);
                Rect inner = new Rect(0, 0, finalWidth, finalHeight);
                this.mEnteringBlackFrame = new BlackFrame(this.mService.mTransactionFactory, t, outer, inner, 2010000, this.mDisplayContent, false, this.mEnterBlackFrameLayer);
            } catch (Surface.OutOfResourcesException e) {
                Slog.w("WindowManager", "Unable to allocate black surface", e);
            }
        }
        if (customAnim) {
            this.mSurfaceRotationAnimationController.startCustomAnimation();
            return true;
        }
        this.mSurfaceRotationAnimationController.startScreenRotationAnimation();
        return true;
    }

    public boolean dismiss(SurfaceControl.Transaction t, long maxAnimationDuration, float animationScale, int finalWidth, int finalHeight, int exitAnim, int enterAnim) {
        if (this.mScreenshotLayer == null) {
            return false;
        }
        if (!this.mStarted) {
            this.mEndLuma = RotationAnimationUtils.getLumaOfSurfaceControl(this.mDisplayContent.getDisplay(), this.mDisplayContent.getWindowingLayer());
            startAnimation(t, maxAnimationDuration, animationScale, finalWidth, finalHeight, exitAnim, enterAnim);
        }
        if (this.mStarted) {
            this.mFinishAnimReady = true;
            return true;
        }
        return false;
    }

    public void kill() {
        SurfaceRotationAnimationController surfaceRotationAnimationController = this.mSurfaceRotationAnimationController;
        if (surfaceRotationAnimationController != null) {
            surfaceRotationAnimationController.cancel();
            this.mSurfaceRotationAnimationController = null;
        }
        if (this.mScreenshotLayer != null) {
            if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                String protoLogParam0 = String.valueOf(this.mScreenshotLayer);
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 1089714158, 0, (String) null, new Object[]{protoLogParam0});
            }
            SurfaceControl.Transaction t = this.mService.mTransactionFactory.get();
            if (this.mScreenshotLayer.isValid()) {
                t.remove(this.mScreenshotLayer);
            } else {
                Slog.w("WindowManager", "this = " + this + "  mScreenihotLayer = " + this.mScreenshotLayer + " not valid !");
            }
            this.mScreenshotLayer = null;
            SurfaceControl surfaceControl = this.mEnterBlackFrameLayer;
            if (surfaceControl != null) {
                if (surfaceControl.isValid()) {
                    t.remove(this.mEnterBlackFrameLayer);
                }
                this.mEnterBlackFrameLayer = null;
            }
            SurfaceControl surfaceControl2 = this.mBackColorSurface;
            if (surfaceControl2 != null) {
                if (surfaceControl2.isValid()) {
                    t.remove(this.mBackColorSurface);
                }
                this.mBackColorSurface = null;
            }
            t.apply();
        }
        BlackFrame blackFrame = this.mEnteringBlackFrame;
        if (blackFrame != null) {
            blackFrame.kill();
            this.mEnteringBlackFrame = null;
        }
        Animation animation = this.mRotateExitAnimation;
        if (animation != null) {
            animation.cancel();
            this.mRotateExitAnimation = null;
        }
        Animation animation2 = this.mRotateEnterAnimation;
        if (animation2 != null) {
            animation2.cancel();
            this.mRotateEnterAnimation = null;
        }
        Animation animation3 = this.mRotateAlphaAnimation;
        if (animation3 != null) {
            animation3.cancel();
            this.mRotateAlphaAnimation = null;
        }
        this.mService.mPowerHalManager.setRotationBoost(false);
    }

    public boolean isAnimating() {
        SurfaceRotationAnimationController surfaceRotationAnimationController = this.mSurfaceRotationAnimationController;
        return surfaceRotationAnimationController != null && surfaceRotationAnimationController.isAnimating();
    }

    public boolean isRotating() {
        return this.mCurRotation != this.mOriginalRotation;
    }

    /* loaded from: classes2.dex */
    public class SurfaceRotationAnimationController {
        private SurfaceAnimator mDisplayAnimator;
        private SurfaceAnimator mEnterBlackFrameAnimator;
        private SurfaceAnimator mRotateScreenAnimator;
        private SurfaceAnimator mScreenshotRotationAnimator;

        SurfaceRotationAnimationController() {
            ScreenRotationAnimation.this = this$0;
        }

        void startCustomAnimation() {
            try {
                ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner.deferStartingAnimations();
                this.mRotateScreenAnimator = startScreenshotAlphaAnimation();
                this.mDisplayAnimator = startDisplayRotation();
                if (ScreenRotationAnimation.this.mEnteringBlackFrame != null) {
                    this.mEnterBlackFrameAnimator = startEnterBlackFrameAnimation();
                }
            } finally {
                ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
            }
        }

        void startScreenRotationAnimation() {
            try {
                ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner.deferStartingAnimations();
                this.mDisplayAnimator = startDisplayRotation();
                this.mScreenshotRotationAnimator = startScreenshotRotationAnimation();
                startColorAnimation();
            } finally {
                ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
            }
        }

        private SimpleSurfaceAnimatable.Builder initializeBuilder() {
            SimpleSurfaceAnimatable.Builder builder = new SimpleSurfaceAnimatable.Builder();
            final DisplayContent displayContent = ScreenRotationAnimation.this.mDisplayContent;
            Objects.requireNonNull(displayContent);
            SimpleSurfaceAnimatable.Builder syncTransactionSupplier = builder.setSyncTransactionSupplier(new Supplier() { // from class: com.android.server.wm.ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return DisplayContent.this.getSyncTransaction();
                }
            });
            final DisplayContent displayContent2 = ScreenRotationAnimation.this.mDisplayContent;
            Objects.requireNonNull(displayContent2);
            SimpleSurfaceAnimatable.Builder pendingTransactionSupplier = syncTransactionSupplier.setPendingTransactionSupplier(new Supplier() { // from class: com.android.server.wm.ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    return DisplayContent.this.getPendingTransaction();
                }
            });
            final DisplayContent displayContent3 = ScreenRotationAnimation.this.mDisplayContent;
            Objects.requireNonNull(displayContent3);
            SimpleSurfaceAnimatable.Builder commitTransactionRunnable = pendingTransactionSupplier.setCommitTransactionRunnable(new Runnable() { // from class: com.android.server.wm.ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayContent.this.commitPendingTransaction();
                }
            });
            final DisplayContent displayContent4 = ScreenRotationAnimation.this.mDisplayContent;
            Objects.requireNonNull(displayContent4);
            return commitTransactionRunnable.setAnimationLeashSupplier(new Supplier() { // from class: com.android.server.wm.ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda4
                @Override // java.util.function.Supplier
                public final Object get() {
                    return DisplayContent.this.makeOverlay();
                }
            });
        }

        private SurfaceAnimator startDisplayRotation() {
            return startAnimation(initializeBuilder().setAnimationLeashParent(ScreenRotationAnimation.this.mDisplayContent.getSurfaceControl()).setSurfaceControl(ScreenRotationAnimation.this.mDisplayContent.getWindowingLayer()).setParentSurfaceControl(ScreenRotationAnimation.this.mDisplayContent.getSurfaceControl()).setWidth(ScreenRotationAnimation.this.mDisplayContent.getSurfaceWidth()).setHeight(ScreenRotationAnimation.this.mDisplayContent.getSurfaceHeight()).build(), createWindowAnimationSpec(ScreenRotationAnimation.this.mRotateEnterAnimation), new ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0(this));
        }

        private SurfaceAnimator startScreenshotAlphaAnimation() {
            return startAnimation(initializeBuilder().setSurfaceControl(ScreenRotationAnimation.this.mScreenshotLayer).setAnimationLeashParent(ScreenRotationAnimation.this.mDisplayContent.getOverlayLayer()).setWidth(ScreenRotationAnimation.this.mDisplayContent.getSurfaceWidth()).setHeight(ScreenRotationAnimation.this.mDisplayContent.getSurfaceHeight()).build(), createWindowAnimationSpec(ScreenRotationAnimation.this.mRotateAlphaAnimation), new ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0(this));
        }

        private SurfaceAnimator startEnterBlackFrameAnimation() {
            return startAnimation(initializeBuilder().setSurfaceControl(ScreenRotationAnimation.this.mEnterBlackFrameLayer).setAnimationLeashParent(ScreenRotationAnimation.this.mDisplayContent.getOverlayLayer()).build(), createWindowAnimationSpec(ScreenRotationAnimation.this.mRotateEnterAnimation), new ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0(this));
        }

        private SurfaceAnimator startScreenshotRotationAnimation() {
            return startAnimation(initializeBuilder().setSurfaceControl(ScreenRotationAnimation.this.mScreenshotLayer).setAnimationLeashParent(ScreenRotationAnimation.this.mDisplayContent.getOverlayLayer()).build(), createWindowAnimationSpec(ScreenRotationAnimation.this.mRotateExitAnimation), new ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0(this));
        }

        private void startColorAnimation() {
            final int colorTransitionMs = ScreenRotationAnimation.this.mContext.getResources().getInteger(17694939);
            SurfaceAnimationRunner runner = ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner;
            final float[] rgbTmpFloat = new float[3];
            final int startColor = Color.rgb(ScreenRotationAnimation.this.mStartLuma, ScreenRotationAnimation.this.mStartLuma, ScreenRotationAnimation.this.mStartLuma);
            final int endColor = Color.rgb(ScreenRotationAnimation.this.mEndLuma, ScreenRotationAnimation.this.mEndLuma, ScreenRotationAnimation.this.mEndLuma);
            final long duration = colorTransitionMs * ScreenRotationAnimation.this.mService.getCurrentAnimatorScale();
            final ArgbEvaluator va = ArgbEvaluator.getInstance();
            runner.startAnimation(new LocalAnimationAdapter.AnimationSpec() { // from class: com.android.server.wm.ScreenRotationAnimation.SurfaceRotationAnimationController.1
                {
                    SurfaceRotationAnimationController.this = this;
                }

                @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
                public long getDuration() {
                    return duration;
                }

                @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
                public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
                    float fraction = getFraction((float) currentPlayTime);
                    int color = ((Integer) va.evaluate(fraction, Integer.valueOf(startColor), Integer.valueOf(endColor))).intValue();
                    Color middleColor = Color.valueOf(color);
                    rgbTmpFloat[0] = middleColor.red();
                    rgbTmpFloat[1] = middleColor.green();
                    rgbTmpFloat[2] = middleColor.blue();
                    if (leash.isValid()) {
                        t.setColor(leash, rgbTmpFloat);
                    }
                }

                @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
                public void dump(PrintWriter pw, String prefix) {
                    pw.println(prefix + "startLuma=" + ScreenRotationAnimation.this.mStartLuma + " endLuma=" + ScreenRotationAnimation.this.mEndLuma + " durationMs=" + colorTransitionMs);
                }

                @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
                public void dumpDebugInner(ProtoOutputStream proto) {
                    long token = proto.start(1146756268036L);
                    proto.write(1108101562369L, ScreenRotationAnimation.this.mStartLuma);
                    proto.write(1108101562370L, ScreenRotationAnimation.this.mEndLuma);
                    proto.write(1112396529667L, colorTransitionMs);
                    proto.end(token);
                }
            }, ScreenRotationAnimation.this.mBackColorSurface, ScreenRotationAnimation.this.mDisplayContent.getPendingTransaction(), null);
        }

        private WindowAnimationSpec createWindowAnimationSpec(Animation mAnimation) {
            WindowManagerService unused = ScreenRotationAnimation.this.mService;
            if (WindowManagerService.mTranScreenRotation) {
                return new WindowAnimationSpec(mAnimation, new Point(0, 0), false, ScreenRotationAnimation.this.mDisplayContent.getWindowCornerRadius());
            }
            return new WindowAnimationSpec(mAnimation, new Point(0, 0), false, 0.0f);
        }

        private SurfaceAnimator startAnimation(SurfaceAnimator.Animatable animatable, LocalAnimationAdapter.AnimationSpec animationSpec, SurfaceAnimator.OnAnimationFinishedCallback animationFinishedCallback) {
            SurfaceAnimator animator = new SurfaceAnimator(animatable, animationFinishedCallback, ScreenRotationAnimation.this.mService);
            LocalAnimationAdapter localAnimationAdapter = new LocalAnimationAdapter(animationSpec, ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner);
            animator.startAnimation(ScreenRotationAnimation.this.mDisplayContent.getPendingTransaction(), localAnimationAdapter, false, 2);
            return animator;
        }

        public void onAnimationEnd(int type, AnimationAdapter anim) {
            synchronized (ScreenRotationAnimation.this.mService.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (isAnimating()) {
                            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                                long protoLogParam0 = type;
                                SurfaceAnimator surfaceAnimator = this.mDisplayAnimator;
                                String protoLogParam1 = String.valueOf(surfaceAnimator != null ? Boolean.valueOf(surfaceAnimator.isAnimating()) : null);
                                SurfaceAnimator surfaceAnimator2 = this.mEnterBlackFrameAnimator;
                                String protoLogParam2 = String.valueOf(surfaceAnimator2 != null ? Boolean.valueOf(surfaceAnimator2.isAnimating()) : null);
                                SurfaceAnimator surfaceAnimator3 = this.mRotateScreenAnimator;
                                String protoLogParam3 = String.valueOf(surfaceAnimator3 != null ? Boolean.valueOf(surfaceAnimator3.isAnimating()) : null);
                                SurfaceAnimator surfaceAnimator4 = this.mScreenshotRotationAnimator;
                                String protoLogParam4 = String.valueOf(surfaceAnimator4 != null ? Boolean.valueOf(surfaceAnimator4.isAnimating()) : null);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1346895820, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1, protoLogParam2, protoLogParam3, protoLogParam4});
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                            Object[] objArr = null;
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 200829729, 0, (String) null, (Object[]) null);
                        }
                        this.mEnterBlackFrameAnimator = null;
                        this.mScreenshotRotationAnimator = null;
                        this.mRotateScreenAnimator = null;
                        WindowAnimator windowAnimator = ScreenRotationAnimation.this.mService.mAnimator;
                        windowAnimator.mBulkUpdateParams = 1 | windowAnimator.mBulkUpdateParams;
                        ScreenRotationAnimation rotationAnimation = ScreenRotationAnimation.this.mDisplayContent.getRotationAnimation();
                        ScreenRotationAnimation screenRotationAnimation = ScreenRotationAnimation.this;
                        if (rotationAnimation == screenRotationAnimation) {
                            screenRotationAnimation.mDisplayContent.setRotationAnimation(null);
                        } else {
                            screenRotationAnimation.kill();
                        }
                        ScreenRotationAnimation.this.mService.updateRotation(false, false);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public void cancel() {
            SurfaceAnimator surfaceAnimator = this.mEnterBlackFrameAnimator;
            if (surfaceAnimator != null) {
                surfaceAnimator.cancelAnimation();
            }
            SurfaceAnimator surfaceAnimator2 = this.mScreenshotRotationAnimator;
            if (surfaceAnimator2 != null) {
                surfaceAnimator2.cancelAnimation();
            }
            SurfaceAnimator surfaceAnimator3 = this.mRotateScreenAnimator;
            if (surfaceAnimator3 != null) {
                surfaceAnimator3.cancelAnimation();
            }
            SurfaceAnimator surfaceAnimator4 = this.mDisplayAnimator;
            if (surfaceAnimator4 != null) {
                surfaceAnimator4.cancelAnimation();
            }
            if (ScreenRotationAnimation.this.mBackColorSurface != null) {
                ScreenRotationAnimation.this.mService.mSurfaceAnimationRunner.onAnimationCancelled(ScreenRotationAnimation.this.mBackColorSurface);
            }
        }

        public boolean isAnimating() {
            SurfaceAnimator surfaceAnimator;
            SurfaceAnimator surfaceAnimator2;
            SurfaceAnimator surfaceAnimator3;
            SurfaceAnimator surfaceAnimator4 = this.mDisplayAnimator;
            return (surfaceAnimator4 != null && surfaceAnimator4.isAnimating()) || ((surfaceAnimator = this.mEnterBlackFrameAnimator) != null && surfaceAnimator.isAnimating()) || (((surfaceAnimator2 = this.mRotateScreenAnimator) != null && surfaceAnimator2.isAnimating()) || ((surfaceAnimator3 = this.mScreenshotRotationAnimator) != null && surfaceAnimator3.isAnimating()));
        }
    }
}
