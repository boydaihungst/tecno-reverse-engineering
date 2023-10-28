package android.animation.dynamicanimation;

import android.animation.dynamicanimation.AnimationHandler;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.view.Choreographer;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class AnimationHandler {
    private static final long FRAME_DELAY_MS = 10;
    private static final ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();
    private FrameCallbackScheduler mScheduler;
    private final ArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime = new ArrayMap<>();
    final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final AnimationCallbackDispatcher mCallbackDispatcher = new AnimationCallbackDispatcher();
    private final Runnable mRunnable = new Runnable() { // from class: android.animation.dynamicanimation.AnimationHandler$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            AnimationHandler.this.m106lambda$new$0$androidanimationdynamicanimationAnimationHandler();
        }
    };
    long mCurrentFrameTime = 0;
    private boolean mListDirty = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface AnimationFrameCallback {
        boolean doAnimationFrame(long j);
    }

    /* loaded from: classes.dex */
    public interface FrameCallbackScheduler {
        boolean isCurrentThread();

        void postFrameCallback(Runnable runnable);
    }

    /* loaded from: classes.dex */
    private class AnimationCallbackDispatcher {
        private AnimationCallbackDispatcher() {
        }

        void dispatchAnimationFrame() {
            AnimationHandler.this.mCurrentFrameTime = SystemClock.uptimeMillis();
            AnimationHandler animationHandler = AnimationHandler.this;
            animationHandler.doAnimationFrame(animationHandler.mCurrentFrameTime);
            if (AnimationHandler.this.mAnimationCallbacks.size() > 0) {
                AnimationHandler.this.mScheduler.postFrameCallback(AnimationHandler.this.mRunnable);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-animation-dynamicanimation-AnimationHandler  reason: not valid java name */
    public /* synthetic */ void m106lambda$new$0$androidanimationdynamicanimationAnimationHandler() {
        this.mCallbackDispatcher.dispatchAnimationFrame();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AnimationHandler getInstance() {
        FrameCallbackScheduler frameCallbackScheduler14;
        ThreadLocal<AnimationHandler> threadLocal = sAnimatorHandler;
        if (threadLocal.get() == null) {
            if (Build.VERSION.SDK_INT >= 16) {
                frameCallbackScheduler14 = new FrameCallbackScheduler16();
            } else {
                frameCallbackScheduler14 = new FrameCallbackScheduler14();
            }
            AnimationHandler handler = new AnimationHandler(frameCallbackScheduler14);
            threadLocal.set(handler);
        }
        return threadLocal.get();
    }

    public AnimationHandler(FrameCallbackScheduler scheduler) {
        this.mScheduler = scheduler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAnimationFrameCallback(AnimationFrameCallback callback, long delay) {
        if (this.mAnimationCallbacks.size() == 0) {
            this.mScheduler.postFrameCallback(this.mRunnable);
        }
        if (!this.mAnimationCallbacks.contains(callback)) {
            this.mAnimationCallbacks.add(callback);
        }
        if (delay > 0) {
            this.mDelayedCallbackStartTime.put(callback, Long.valueOf(SystemClock.uptimeMillis() + delay));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCallback(AnimationFrameCallback callback) {
        this.mDelayedCallbackStartTime.remove(callback);
        int id = this.mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            this.mAnimationCallbacks.set(id, null);
            this.mListDirty = true;
        }
    }

    void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        for (int i = 0; i < this.mAnimationCallbacks.size(); i++) {
            AnimationFrameCallback callback = this.mAnimationCallbacks.get(i);
            if (callback != null && isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
        cleanUpList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentThread() {
        return this.mScheduler.isCurrentThread();
    }

    private boolean isCallbackDue(AnimationFrameCallback callback, long currentTime) {
        Long startTime = this.mDelayedCallbackStartTime.get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime.longValue() < currentTime) {
            this.mDelayedCallbackStartTime.remove(callback);
            return true;
        }
        return false;
    }

    private void cleanUpList() {
        if (this.mListDirty) {
            for (int i = this.mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (this.mAnimationCallbacks.get(i) == null) {
                    this.mAnimationCallbacks.remove(i);
                }
            }
            this.mListDirty = false;
        }
    }

    public void setScheduler(FrameCallbackScheduler scheduler) {
        this.mScheduler = scheduler;
    }

    public FrameCallbackScheduler getScheduler() {
        return this.mScheduler;
    }

    /* loaded from: classes.dex */
    static final class FrameCallbackScheduler16 implements FrameCallbackScheduler {
        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Looper mLooper = Looper.myLooper();

        FrameCallbackScheduler16() {
        }

        @Override // android.animation.dynamicanimation.AnimationHandler.FrameCallbackScheduler
        public void postFrameCallback(final Runnable frameCallback) {
            this.mChoreographer.postFrameCallback(new Choreographer.FrameCallback() { // from class: android.animation.dynamicanimation.AnimationHandler$FrameCallbackScheduler16$$ExternalSyntheticLambda0
                @Override // android.view.Choreographer.FrameCallback
                public final void doFrame(long j) {
                    frameCallback.run();
                }
            });
        }

        @Override // android.animation.dynamicanimation.AnimationHandler.FrameCallbackScheduler
        public boolean isCurrentThread() {
            return Thread.currentThread() == this.mLooper.getThread();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class FrameCallbackScheduler14 implements FrameCallbackScheduler {
        private final Handler mHandler = new Handler(Looper.myLooper());
        private long mLastFrameTime;

        FrameCallbackScheduler14() {
        }

        @Override // android.animation.dynamicanimation.AnimationHandler.FrameCallbackScheduler
        public void postFrameCallback(final Runnable frameCallback) {
            long delay = 10 - (SystemClock.uptimeMillis() - this.mLastFrameTime);
            this.mHandler.postDelayed(new Runnable() { // from class: android.animation.dynamicanimation.AnimationHandler$FrameCallbackScheduler14$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AnimationHandler.FrameCallbackScheduler14.this.m107x4ce99e8c(frameCallback);
                }
            }, Math.max(delay, 0L));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postFrameCallback$0$android-animation-dynamicanimation-AnimationHandler$FrameCallbackScheduler14  reason: not valid java name */
        public /* synthetic */ void m107x4ce99e8c(Runnable frameCallback) {
            this.mLastFrameTime = SystemClock.uptimeMillis();
            frameCallback.run();
        }

        @Override // android.animation.dynamicanimation.AnimationHandler.FrameCallbackScheduler
        public boolean isCurrentThread() {
            return Thread.currentThread() == this.mHandler.getLooper().getThread();
        }
    }
}
