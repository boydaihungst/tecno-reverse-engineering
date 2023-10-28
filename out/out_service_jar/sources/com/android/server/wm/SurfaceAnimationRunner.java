package com.android.server.wm;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Handler;
import android.os.PowerManagerInternal;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.server.AnimationThread;
import com.android.server.wm.LocalAnimationAdapter;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class SurfaceAnimationRunner {
    private static final int MTKPOWER_HINT_LAUNCHER_ANIMA = 48;
    private static PowerHalMgr mPowerHalService;
    private final AnimationHandler mAnimationHandler;
    private boolean mAnimationStartDeferred;
    private final Handler mAnimationThreadHandler;
    private final AnimatorFactory mAnimatorFactory;
    private boolean mApplyScheduled;
    private final Runnable mApplyTransactionRunnable;
    private final Object mCancelLock;
    Choreographer mChoreographer;
    private final ExecutorService mEdgeExtensionExecutor;
    private final Object mEdgeExtensionLock;
    private final ArrayMap<SurfaceControl, ArrayList<SurfaceControl>> mEdgeExtensions;
    private final SurfaceControl.Transaction mFrameTransaction;
    private final Object mLock;
    final ArrayMap<SurfaceControl, RunningAnimation> mPendingAnimations;
    private final PowerManagerInternal mPowerManagerInternal;
    final ArrayMap<SurfaceControl, RunningAnimation> mPreProcessingAnimations;
    final ArrayMap<SurfaceControl, RunningAnimation> mRunningAnimations;
    private final Handler mSurfaceAnimationHandler;
    private long maxDuration;

    /* loaded from: classes2.dex */
    public interface AnimatorFactory {
        ValueAnimator makeAnimator();
    }

    public SurfaceAnimationRunner(Supplier<SurfaceControl.Transaction> transactionFactory, PowerManagerInternal powerManagerInternal) {
        this(null, null, transactionFactory.get(), powerManagerInternal);
    }

    SurfaceAnimationRunner(AnimationHandler.AnimationFrameCallbackProvider callbackProvider, AnimatorFactory animatorFactory, SurfaceControl.Transaction frameTransaction, PowerManagerInternal powerManagerInternal) {
        AnimationHandler.AnimationFrameCallbackProvider sfVsyncFrameCallbackProvider;
        AnimatorFactory animatorFactory2;
        this.mLock = new Object();
        this.mCancelLock = new Object();
        this.mEdgeExtensionLock = new Object();
        this.mAnimationThreadHandler = AnimationThread.getHandler();
        Handler handler = SurfaceAnimationThread.getHandler();
        this.mSurfaceAnimationHandler = handler;
        this.mApplyTransactionRunnable = new Runnable() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceAnimationRunner.this.applyTransaction();
            }
        };
        this.mEdgeExtensionExecutor = Executors.newFixedThreadPool(2);
        this.mPendingAnimations = new ArrayMap<>();
        this.mPreProcessingAnimations = new ArrayMap<>();
        this.mRunningAnimations = new ArrayMap<>();
        this.mEdgeExtensions = new ArrayMap<>();
        handler.runWithScissors(new Runnable() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceAnimationRunner.this.m8250lambda$new$0$comandroidserverwmSurfaceAnimationRunner();
            }
        }, 0L);
        this.mFrameTransaction = frameTransaction;
        AnimationHandler animationHandler = new AnimationHandler();
        this.mAnimationHandler = animationHandler;
        if (callbackProvider != null) {
            sfVsyncFrameCallbackProvider = callbackProvider;
        } else {
            sfVsyncFrameCallbackProvider = new SfVsyncFrameCallbackProvider(this.mChoreographer);
        }
        animationHandler.setProvider(sfVsyncFrameCallbackProvider);
        if (animatorFactory != null) {
            animatorFactory2 = animatorFactory;
        } else {
            animatorFactory2 = new AnimatorFactory() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda6
                @Override // com.android.server.wm.SurfaceAnimationRunner.AnimatorFactory
                public final ValueAnimator makeAnimator() {
                    return SurfaceAnimationRunner.this.m8251lambda$new$1$comandroidserverwmSurfaceAnimationRunner();
                }
            };
        }
        this.mAnimatorFactory = animatorFactory2;
        this.mPowerManagerInternal = powerManagerInternal;
    }

    /* renamed from: lambda$new$0$com-android-server-wm-SurfaceAnimationRunner */
    public /* synthetic */ void m8250lambda$new$0$comandroidserverwmSurfaceAnimationRunner() {
        this.mChoreographer = Choreographer.getSfInstance();
    }

    /* renamed from: lambda$new$1$com-android-server-wm-SurfaceAnimationRunner */
    public /* synthetic */ ValueAnimator m8251lambda$new$1$comandroidserverwmSurfaceAnimationRunner() {
        return new SfValueAnimator();
    }

    public void deferStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = true;
        }
    }

    public void continueStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = false;
            if (!this.mPendingAnimations.isEmpty() && this.mPreProcessingAnimations.isEmpty()) {
                this.mChoreographer.postFrameCallback(new SurfaceAnimationRunner$$ExternalSyntheticLambda1(this));
            }
        }
    }

    public void startAnimation(final LocalAnimationAdapter.AnimationSpec a, final SurfaceControl animationLeash, SurfaceControl.Transaction t, Runnable finishCallback) {
        synchronized (this.mLock) {
            final RunningAnimation runningAnim = new RunningAnimation(a, animationLeash, finishCallback);
            boolean requiresEdgeExtension = requiresEdgeExtension(a);
            if (requiresEdgeExtension) {
                ArrayList<SurfaceControl> extensionSurfaces = new ArrayList<>();
                synchronized (this.mEdgeExtensionLock) {
                    this.mEdgeExtensions.put(animationLeash, extensionSurfaces);
                }
                this.mPreProcessingAnimations.put(animationLeash, runningAnim);
                t.addTransactionCommittedListener(this.mEdgeExtensionExecutor, new SurfaceControl.TransactionCommittedListener() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda2
                    public final void onTransactionCommitted() {
                        SurfaceAnimationRunner.this.m8253xa2ce7b75(a, animationLeash, runningAnim);
                    }
                });
            }
            if (!requiresEdgeExtension) {
                this.mPendingAnimations.put(animationLeash, runningAnim);
                if (!this.mAnimationStartDeferred && this.mPreProcessingAnimations.isEmpty()) {
                    this.mChoreographer.postFrameCallback(new SurfaceAnimationRunner$$ExternalSyntheticLambda1(this));
                }
                applyTransformation(runningAnim, t, 0L);
            }
        }
    }

    /* renamed from: lambda$startAnimation$2$com-android-server-wm-SurfaceAnimationRunner */
    public /* synthetic */ void m8253xa2ce7b75(LocalAnimationAdapter.AnimationSpec a, SurfaceControl animationLeash, RunningAnimation runningAnim) {
        WindowAnimationSpec animationSpec = a.asWindowAnimationSpec();
        SurfaceControl.Transaction edgeExtensionCreationTransaction = new SurfaceControl.Transaction();
        edgeExtendWindow(animationLeash, animationSpec.getRootTaskBounds(), animationSpec.getAnimation(), edgeExtensionCreationTransaction);
        synchronized (this.mLock) {
            if (this.mPreProcessingAnimations.get(animationLeash) == runningAnim) {
                synchronized (this.mEdgeExtensionLock) {
                    if (!this.mEdgeExtensions.isEmpty()) {
                        edgeExtensionCreationTransaction.apply();
                    }
                }
                this.mPreProcessingAnimations.remove(animationLeash);
                this.mPendingAnimations.put(animationLeash, runningAnim);
                if (!this.mAnimationStartDeferred && this.mPreProcessingAnimations.isEmpty()) {
                    this.mChoreographer.postFrameCallback(new SurfaceAnimationRunner$$ExternalSyntheticLambda1(this));
                }
            }
        }
    }

    private boolean requiresEdgeExtension(LocalAnimationAdapter.AnimationSpec a) {
        return a.asWindowAnimationSpec() != null && a.asWindowAnimationSpec().hasExtension();
    }

    public void onAnimationCancelled(SurfaceControl leash) {
        synchronized (this.mLock) {
            if (this.mPendingAnimations.containsKey(leash)) {
                this.mPendingAnimations.remove(leash);
            } else if (this.mPreProcessingAnimations.containsKey(leash)) {
                this.mPreProcessingAnimations.remove(leash);
            } else {
                final RunningAnimation anim = this.mRunningAnimations.get(leash);
                if (anim != null) {
                    this.mRunningAnimations.remove(leash);
                    synchronized (this.mCancelLock) {
                        anim.mCancelled = true;
                    }
                    this.mSurfaceAnimationHandler.post(new Runnable() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            SurfaceAnimationRunner.this.m8252x3f0bf46a(anim);
                        }
                    });
                }
            }
        }
    }

    /* renamed from: lambda$onAnimationCancelled$3$com-android-server-wm-SurfaceAnimationRunner */
    public /* synthetic */ void m8252x3f0bf46a(RunningAnimation anim) {
        anim.mAnim.cancel();
        applyTransaction();
    }

    private void startPendingAnimationsLocked() {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            startAnimationLocked(this.mPendingAnimations.valueAt(i));
        }
        this.mPendingAnimations.clear();
    }

    private void startAnimationLocked(final RunningAnimation a) {
        final ValueAnimator anim = this.mAnimatorFactory.makeAnimator();
        anim.overrideDurationScale(1.0f);
        anim.setDuration(a.mAnimSpec.getDuration());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.SurfaceAnimationRunner$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                SurfaceAnimationRunner.this.m8254x71001989(a, anim, valueAnimator);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.SurfaceAnimationRunner.1
            {
                SurfaceAnimationRunner.this = this;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                    if (!a.mCancelled) {
                        SurfaceAnimationRunner.this.mFrameTransaction.setAlpha(a.mLeash, 1.0f);
                    }
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                synchronized (SurfaceAnimationRunner.this.mLock) {
                    SurfaceAnimationRunner.this.mRunningAnimations.remove(a.mLeash);
                    synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                        if (!a.mCancelled) {
                            SurfaceAnimationRunner.this.mAnimationThreadHandler.post(a.mFinishCallback);
                        }
                    }
                }
            }
        });
        a.mAnim = anim;
        this.mRunningAnimations.put(a.mLeash, a);
        anim.start();
        if (a.mAnimSpec.canSkipFirstFrame()) {
            anim.setCurrentPlayTime(this.mChoreographer.getFrameIntervalNanos() / 1000000);
        }
        anim.doAnimationFrame(this.mChoreographer.getFrameTime());
    }

    /* renamed from: lambda$startAnimationLocked$4$com-android-server-wm-SurfaceAnimationRunner */
    public /* synthetic */ void m8254x71001989(RunningAnimation a, ValueAnimator anim, ValueAnimator animation) {
        synchronized (this.mCancelLock) {
            if (!a.mCancelled) {
                long duration = anim.getDuration();
                long currentPlayTime = anim.getCurrentPlayTime();
                if (currentPlayTime > duration) {
                    currentPlayTime = duration;
                }
                applyTransformation(a, this.mFrameTransaction, currentPlayTime);
            }
        }
        scheduleApplyTransaction();
    }

    private void applyTransformation(RunningAnimation a, SurfaceControl.Transaction t, long currentPlayTime) {
        a.mAnimSpec.apply(t, a.mLeash, currentPlayTime);
    }

    public void startAnimations(long frameTimeNanos) {
        this.maxDuration = 0L;
        synchronized (this.mLock) {
            if (this.mPreProcessingAnimations.isEmpty()) {
                startPendingAnimationsLocked();
                for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
                    RunningAnimation anim = this.mRunningAnimations.valueAt(i);
                    if (anim != null && this.maxDuration < anim.mAnimSpec.getDuration()) {
                        this.maxDuration = anim.mAnimSpec.getDuration();
                    }
                }
                long j = this.maxDuration;
                if (j > 0) {
                    startAnimationBoost(j);
                }
            }
        }
    }

    private void scheduleApplyTransaction() {
        if (!this.mApplyScheduled) {
            this.mChoreographer.postCallback(3, this.mApplyTransactionRunnable, null);
            this.mApplyScheduled = true;
        }
    }

    public void applyTransaction() {
        this.mFrameTransaction.setAnimationTransaction();
        this.mFrameTransaction.setFrameTimelineVsync(this.mChoreographer.getVsyncId());
        this.mFrameTransaction.apply();
        this.mApplyScheduled = false;
    }

    private void edgeExtendWindow(SurfaceControl leash, Rect bounds, Animation a, SurfaceControl.Transaction transaction) {
        Transformation transformationAtStart = new Transformation();
        a.getTransformationAt(0.0f, transformationAtStart);
        Transformation transformationAtEnd = new Transformation();
        a.getTransformationAt(1.0f, transformationAtEnd);
        Insets maxExtensionInsets = Insets.min(transformationAtStart.getInsets(), transformationAtEnd.getInsets());
        int targetSurfaceHeight = bounds.height();
        int targetSurfaceWidth = bounds.width();
        if (maxExtensionInsets.left < 0) {
            Rect edgeBounds = new Rect(0, 0, 1, targetSurfaceHeight);
            Rect extensionRect = new Rect(0, 0, -maxExtensionInsets.left, targetSurfaceHeight);
            int xPos = maxExtensionInsets.left;
            createExtensionSurface(leash, edgeBounds, extensionRect, xPos, 0, "Left Edge Extension", transaction);
        }
        int xPos2 = maxExtensionInsets.top;
        if (xPos2 < 0) {
            Rect edgeBounds2 = new Rect(0, 0, targetSurfaceWidth, 1);
            Rect extensionRect2 = new Rect(0, 0, targetSurfaceWidth, -maxExtensionInsets.top);
            int yPos = maxExtensionInsets.top;
            createExtensionSurface(leash, edgeBounds2, extensionRect2, 0, yPos, "Top Edge Extension", transaction);
        }
        int xPos3 = maxExtensionInsets.right;
        if (xPos3 < 0) {
            Rect edgeBounds3 = new Rect(targetSurfaceWidth - 1, 0, targetSurfaceWidth, targetSurfaceHeight);
            Rect extensionRect3 = new Rect(0, 0, -maxExtensionInsets.right, targetSurfaceHeight);
            createExtensionSurface(leash, edgeBounds3, extensionRect3, targetSurfaceWidth, 0, "Right Edge Extension", transaction);
        }
        if (maxExtensionInsets.bottom < 0) {
            Rect edgeBounds4 = new Rect(0, targetSurfaceHeight - 1, targetSurfaceWidth, targetSurfaceHeight);
            Rect extensionRect4 = new Rect(0, 0, targetSurfaceWidth, -maxExtensionInsets.bottom);
            int xPos4 = maxExtensionInsets.left;
            createExtensionSurface(leash, edgeBounds4, extensionRect4, xPos4, targetSurfaceHeight, "Bottom Edge Extension", transaction);
        }
    }

    private void createExtensionSurface(SurfaceControl leash, Rect edgeBounds, Rect extensionRect, int xPos, int yPos, String layerName, SurfaceControl.Transaction startTransaction) {
        Trace.traceBegin(32L, "createExtensionSurface");
        doCreateExtensionSurface(leash, edgeBounds, extensionRect, xPos, yPos, layerName, startTransaction);
        Trace.traceEnd(32L);
    }

    private void doCreateExtensionSurface(SurfaceControl leash, Rect edgeBounds, Rect extensionRect, int xPos, int yPos, String layerName, SurfaceControl.Transaction startTransaction) {
        SurfaceControl.LayerCaptureArgs captureArgs = new SurfaceControl.LayerCaptureArgs.Builder(leash).setSourceCrop(edgeBounds).setFrameScale(1.0f).setPixelFormat(1).setChildrenOnly(true).setAllowProtected(true).build();
        SurfaceControl.ScreenshotHardwareBuffer edgeBuffer = SurfaceControl.captureLayers(captureArgs);
        if (edgeBuffer == null) {
            Log.e("SurfaceAnimationRunner", "Failed to create edge extension - edge buffer is null");
            return;
        }
        SurfaceControl edgeExtensionLayer = new SurfaceControl.Builder().setName(layerName).setHidden(true).setCallsite("DefaultTransitionHandler#startAnimation").setOpaque(true).setBufferSize(edgeBounds.width(), edgeBounds.height()).build();
        Surface surface = new Surface(edgeExtensionLayer);
        surface.attachAndQueueBufferWithColorSpace(edgeBuffer.getHardwareBuffer(), edgeBuffer.getColorSpace());
        surface.release();
        float scaleX = getScaleXForExtensionSurface(edgeBounds, extensionRect);
        float scaleY = getScaleYForExtensionSurface(edgeBounds, extensionRect);
        synchronized (this.mEdgeExtensionLock) {
            try {
                try {
                    if (!this.mEdgeExtensions.containsKey(leash)) {
                        startTransaction.remove(edgeExtensionLayer);
                        return;
                    }
                    startTransaction.setScale(edgeExtensionLayer, scaleX, scaleY);
                    startTransaction.reparent(edgeExtensionLayer, leash);
                    startTransaction.setLayer(edgeExtensionLayer, Integer.MIN_VALUE);
                    startTransaction.setPosition(edgeExtensionLayer, xPos, yPos);
                    startTransaction.setVisibility(edgeExtensionLayer, true);
                    this.mEdgeExtensions.get(leash).add(edgeExtensionLayer);
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

    private float getScaleXForExtensionSurface(Rect edgeBounds, Rect extensionRect) {
        if (edgeBounds.width() == extensionRect.width()) {
            return 1.0f;
        }
        if (edgeBounds.width() == 1) {
            return extensionRect.width();
        }
        throw new RuntimeException("Unexpected edgeBounds and extensionRect widths");
    }

    private float getScaleYForExtensionSurface(Rect edgeBounds, Rect extensionRect) {
        if (edgeBounds.height() == extensionRect.height()) {
            return 1.0f;
        }
        if (edgeBounds.height() == 1) {
            return extensionRect.height();
        }
        throw new RuntimeException("Unexpected edgeBounds and extensionRect heights");
    }

    /* loaded from: classes2.dex */
    public static final class RunningAnimation {
        ValueAnimator mAnim;
        final LocalAnimationAdapter.AnimationSpec mAnimSpec;
        private boolean mCancelled;
        final Runnable mFinishCallback;
        final SurfaceControl mLeash;

        RunningAnimation(LocalAnimationAdapter.AnimationSpec animSpec, SurfaceControl leash, Runnable finishCallback) {
            this.mAnimSpec = animSpec;
            this.mLeash = leash;
            this.mFinishCallback = finishCallback;
        }
    }

    public void onAnimationLeashLost(SurfaceControl animationLeash, SurfaceControl.Transaction t) {
        synchronized (this.mEdgeExtensionLock) {
            if (this.mEdgeExtensions.containsKey(animationLeash)) {
                ArrayList<SurfaceControl> edgeExtensions = this.mEdgeExtensions.get(animationLeash);
                for (int i = 0; i < edgeExtensions.size(); i++) {
                    SurfaceControl extension = edgeExtensions.get(i);
                    t.remove(extension);
                }
                this.mEdgeExtensions.remove(animationLeash);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class SfValueAnimator extends ValueAnimator {
        SfValueAnimator() {
            SurfaceAnimationRunner.this = r1;
            setFloatValues(0.0f, 1.0f);
        }

        public AnimationHandler getAnimationHandler() {
            return SurfaceAnimationRunner.this.mAnimationHandler;
        }
    }

    private void startAnimationBoost(long maxDuration) {
        if (mPowerHalService == null) {
            mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
        }
        PowerHalMgr powerHalMgr = mPowerHalService;
        if (powerHalMgr != null) {
            powerHalMgr.perfCusLockHint(48, (int) maxDuration);
        }
    }
}
