package android.view;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.res.CompatibilityInfo;
import android.graphics.Insets;
import android.graphics.Rect;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioSystem;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.InsetsController;
import android.view.SyncRtSurfaceTransactionApplier;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowInsetsController;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.internal.inputmethod.ImeTracing;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
/* loaded from: classes3.dex */
public class InsetsController implements WindowInsetsController, InsetsAnimationControlCallbacks {
    private static final int ANIMATION_DELAY_DIM_MS = 500;
    private static final int ANIMATION_DURATION_FADE_IN_MS = 500;
    private static final int ANIMATION_DURATION_FADE_OUT_MS = 1500;
    private static final int ANIMATION_DURATION_MOVE_IN_MS = 275;
    private static final int ANIMATION_DURATION_MOVE_OUT_MS = 340;
    public static final int ANIMATION_DURATION_RESIZE = 300;
    private static final int ANIMATION_DURATION_SYNC_IME_MS = 285;
    private static final int ANIMATION_DURATION_UNSYNC_IME_MS = 200;
    public static final int ANIMATION_TYPE_HIDE = 1;
    public static final int ANIMATION_TYPE_NONE = -1;
    public static final int ANIMATION_TYPE_RESIZE = 3;
    public static final int ANIMATION_TYPE_SHOW = 0;
    public static final int ANIMATION_TYPE_USER = 2;
    static final boolean DEBUG = false;
    static final boolean DEBUG_NAV = false;
    private static final int FLOATING_IME_BOTTOM_INSET_DP = -80;
    public static final int LAYOUT_INSETS_DURING_ANIMATION_HIDDEN = 1;
    public static final int LAYOUT_INSETS_DURING_ANIMATION_SHOWN = 0;
    private static final int PENDING_CONTROL_TIMEOUT_MS = 2000;
    private static final String TAG = "InsetsController";
    private static final int TRAN_ANIMATION_DURATION_SYNC_IME_MS = 250;
    private static final int TRAN_ANIMATION_DURATION_UNSYNC_IME_MS = 200;
    static final boolean WARN = false;
    private final Runnable mAnimCallback;
    private boolean mAnimCallbackScheduled;
    private boolean mAnimationsDisabled;
    private int mCaptionInsetsHeight;
    private final BiFunction<InsetsController, Integer, InsetsSourceConsumer> mConsumerCreator;
    private final ArrayList<WindowInsetsController.OnControllableInsetsChangedListener> mControllableInsetsChangedListeners;
    private int mDisabledUserAnimationInsetsTypes;
    private final Rect mFrame;
    private final Handler mHandler;
    private final Host mHost;
    private final Runnable mInvokeControllableInsetsChangedListeners;
    private final InsetsState mLastDispatchedState;
    private WindowInsets mLastInsets;
    private int mLastLegacySoftInputMode;
    private int mLastLegacySystemUiFlags;
    private int mLastLegacyWindowFlags;
    private int mLastStartedAnimTypes;
    private int mLastWindowingMode;
    private final Runnable mPendingControlTimeout;
    private PendingControlRequest mPendingImeControlRequest;
    private final InsetsVisibilities mRequestedVisibilities;
    private final ArraySet<InsetsSourceConsumer> mRequestedVisibilityChanged;
    private final ArrayList<RunningAnimation> mRunningAnimations;
    private final SparseArray<InsetsSourceConsumer> mSourceConsumers;
    private boolean mStartingAnimation;
    private final InsetsState mState;
    private final SparseArray<InsetsSourceControl> mTmpControlArray;
    private int mTypesBeingCancelled;
    private int mWindowType;
    private static final Interpolator SYSTEM_BARS_INSETS_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    private static final Interpolator SYSTEM_BARS_ALPHA_INTERPOLATOR = new PathInterpolator(0.3f, 0.0f, 1.0f, 1.0f);
    private static final Interpolator SYSTEM_BARS_DIM_INTERPOLATOR = new Interpolator() { // from class: android.view.InsetsController$$ExternalSyntheticLambda0
        @Override // android.animation.TimeInterpolator
        public final float getInterpolation(float f) {
            return InsetsController.lambda$static$0(f);
        }
    };
    private static final Interpolator SYNC_IME_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    private static final Interpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator RESIZE_INTERPOLATOR = new LinearInterpolator();
    private static final boolean mTranImeOpt = "1".equals(SystemProperties.get("ro.os_imeoptimize_support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
    private static final Interpolator TRAN_INSETS_LINEAR_IN_INTERPOLATOR = new PathInterpolator(0.25f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator TRAN_INSETS_LINEAR_OUT_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    private static TypeEvaluator<Insets> sEvaluator = new TypeEvaluator() { // from class: android.view.InsetsController$$ExternalSyntheticLambda1
        @Override // android.animation.TypeEvaluator
        public final Object evaluate(float f, Object obj, Object obj2) {
            Insets of;
            Insets insets = (Insets) obj;
            Insets insets2 = (Insets) obj2;
            of = Insets.of((int) (insets.left + ((insets2.left - insets.left) * f)), (int) (insets.top + ((insets2.top - insets.top) * f)), (int) (insets.right + ((insets2.right - insets.right) * f)), (int) (insets.bottom + ((insets2.bottom - insets.bottom) * f)));
            return of;
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    @interface AnimationType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    @interface LayoutInsetsDuringAnimation {
    }

    /* loaded from: classes3.dex */
    public interface Host {
        void addOnPreDrawRunnable(Runnable runnable);

        void applySurfaceParams(SyncRtSurfaceTransactionApplier.SurfaceParams... surfaceParamsArr);

        int dipToPx(int i);

        void dispatchWindowInsetsAnimationEnd(WindowInsetsAnimation windowInsetsAnimation);

        void dispatchWindowInsetsAnimationPrepare(WindowInsetsAnimation windowInsetsAnimation);

        WindowInsets dispatchWindowInsetsAnimationProgress(WindowInsets windowInsets, List<WindowInsetsAnimation> list);

        WindowInsetsAnimation.Bounds dispatchWindowInsetsAnimationStart(WindowInsetsAnimation windowInsetsAnimation, WindowInsetsAnimation.Bounds bounds);

        Handler getHandler();

        InputMethodManager getInputMethodManager();

        String getRootViewTitle();

        int getSystemBarsAppearance();

        int getSystemBarsBehavior();

        IBinder getWindowToken();

        boolean hasAnimationCallbacks();

        void notifyInsetsChanged();

        void postInsetsAnimationCallback(Runnable runnable);

        void releaseSurfaceControlFromRt(SurfaceControl surfaceControl);

        void setSystemBarsAppearance(int i, int i2);

        void setSystemBarsBehavior(int i);

        void updateCompatSysUiVisibility(int i, boolean z, boolean z2);

        void updateRequestedVisibilities(InsetsVisibilities insetsVisibilities);

        default boolean isSystemBarsAppearanceControlled() {
            return false;
        }

        default boolean isSystemBarsBehaviorControlled() {
            return false;
        }

        default CompatibilityInfo.Translator getTranslator() {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ float lambda$static$0(float alphaFraction) {
        float fraction = 1.0f - alphaFraction;
        if (fraction <= 0.33333334f) {
            return 1.0f;
        }
        float innerFraction = (fraction - 0.33333334f) / 0.6666666f;
        return 1.0f - SYSTEM_BARS_ALPHA_INTERPOLATOR.getInterpolation(innerFraction);
    }

    /* loaded from: classes3.dex */
    public static class InternalAnimationControlListener implements WindowInsetsAnimationControlListener {
        private ValueAnimator mAnimator;
        private final int mBehavior;
        private WindowInsetsAnimationController mController;
        private final boolean mDisable;
        private final int mFloatingImeBottomInset;
        private final boolean mHasAnimationCallbacks;
        private final int mRequestedTypes;
        private final boolean mShow;
        private final ThreadLocal<AnimationHandler> mSfAnimationHandlerThreadLocal = new ThreadLocal<AnimationHandler>() { // from class: android.view.InsetsController.InternalAnimationControlListener.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: protected */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // java.lang.ThreadLocal
            public AnimationHandler initialValue() {
                AnimationHandler handler = new AnimationHandler();
                handler.setProvider(new SfVsyncFrameCallbackProvider());
                return handler;
            }
        };
        private final long mDurationMs = calculateDurationMs();

        public InternalAnimationControlListener(boolean show, boolean hasAnimationCallbacks, int requestedTypes, int behavior, boolean disable, int floatingImeBottomInset) {
            this.mShow = show;
            this.mHasAnimationCallbacks = hasAnimationCallbacks;
            this.mRequestedTypes = requestedTypes;
            this.mBehavior = behavior;
            this.mDisable = disable;
            this.mFloatingImeBottomInset = floatingImeBottomInset;
        }

        @Override // android.view.WindowInsetsAnimationControlListener
        public void onReady(final WindowInsetsAnimationController controller, int types) {
            Insets insets;
            final Insets start;
            final Insets end;
            this.mController = controller;
            if (this.mDisable) {
                onAnimationFinish();
                return;
            }
            ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.mAnimator = ofFloat;
            ofFloat.setDuration(this.mDurationMs);
            this.mAnimator.setInterpolator(new LinearInterpolator());
            Insets hiddenInsets = controller.getHiddenStateInsets();
            if (controller.hasZeroInsetsIme()) {
                insets = Insets.of(hiddenInsets.left, hiddenInsets.top, hiddenInsets.right, this.mFloatingImeBottomInset);
            } else {
                insets = hiddenInsets;
            }
            Insets hiddenInsets2 = insets;
            if (this.mShow) {
                start = hiddenInsets2;
            } else {
                start = controller.getShownStateInsets();
            }
            if (this.mShow) {
                end = controller.getShownStateInsets();
            } else {
                end = hiddenInsets2;
            }
            final Interpolator insetsInterpolator = getInsetsInterpolator();
            final Interpolator alphaInterpolator = getAlphaInterpolator();
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: android.view.InsetsController$InternalAnimationControlListener$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    InsetsController.InternalAnimationControlListener.this.m4817x674928e3(insetsInterpolator, controller, start, end, alphaInterpolator, valueAnimator);
                }
            });
            this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: android.view.InsetsController.InternalAnimationControlListener.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    InternalAnimationControlListener.this.onAnimationFinish();
                }
            });
            if (!this.mHasAnimationCallbacks) {
                this.mAnimator.setAnimationHandler(this.mSfAnimationHandlerThreadLocal.get());
            }
            this.mAnimator.start();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReady$0$android-view-InsetsController$InternalAnimationControlListener  reason: not valid java name */
        public /* synthetic */ void m4817x674928e3(Interpolator insetsInterpolator, WindowInsetsAnimationController controller, Insets start, Insets end, Interpolator alphaInterpolator, ValueAnimator animation) {
            float alphaFraction;
            float rawFraction = animation.getAnimatedFraction();
            float f = 1.0f;
            if (this.mShow) {
                alphaFraction = rawFraction;
            } else {
                alphaFraction = 1.0f - rawFraction;
            }
            float insetsFraction = insetsInterpolator.getInterpolation(rawFraction);
            Insets insets = (Insets) InsetsController.sEvaluator.evaluate(insetsFraction, start, end);
            if ((this.mRequestedTypes & WindowInsets.Type.ime()) == 0 || !InsetsController.mTranImeOpt) {
                f = alphaInterpolator.getInterpolation(alphaFraction);
            }
            controller.setInsetsAndAlpha(insets, f, rawFraction);
        }

        @Override // android.view.WindowInsetsAnimationControlListener
        public void onFinished(WindowInsetsAnimationController controller) {
        }

        @Override // android.view.WindowInsetsAnimationControlListener
        public void onCancelled(WindowInsetsAnimationController controller) {
            ValueAnimator valueAnimator = this.mAnimator;
            if (valueAnimator != null) {
                valueAnimator.removeAllUpdateListeners();
                this.mAnimator.cancel();
            }
        }

        protected Interpolator getInsetsInterpolator() {
            if ((this.mRequestedTypes & WindowInsets.Type.ime()) != 0) {
                if (this.mHasAnimationCallbacks) {
                    return InsetsController.SYNC_IME_INTERPOLATOR;
                }
                return this.mShow ? InsetsController.mTranImeOpt ? InsetsController.TRAN_INSETS_LINEAR_IN_INTERPOLATOR : InsetsController.LINEAR_OUT_SLOW_IN_INTERPOLATOR : InsetsController.mTranImeOpt ? InsetsController.TRAN_INSETS_LINEAR_OUT_INTERPOLATOR : InsetsController.FAST_OUT_LINEAR_IN_INTERPOLATOR;
            } else if (this.mBehavior == 2) {
                return InsetsController.SYSTEM_BARS_INSETS_INTERPOLATOR;
            } else {
                return new Interpolator() { // from class: android.view.InsetsController$InternalAnimationControlListener$$ExternalSyntheticLambda1
                    @Override // android.animation.TimeInterpolator
                    public final float getInterpolation(float f) {
                        return InsetsController.InternalAnimationControlListener.this.m4816x553b5e13(f);
                    }
                };
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getInsetsInterpolator$1$android-view-InsetsController$InternalAnimationControlListener  reason: not valid java name */
        public /* synthetic */ float m4816x553b5e13(float input) {
            return this.mShow ? 1.0f : 0.0f;
        }

        Interpolator getAlphaInterpolator() {
            if ((this.mRequestedTypes & WindowInsets.Type.ime()) != 0) {
                if (this.mHasAnimationCallbacks) {
                    return new Interpolator() { // from class: android.view.InsetsController$InternalAnimationControlListener$$ExternalSyntheticLambda2
                        @Override // android.animation.TimeInterpolator
                        public final float getInterpolation(float f) {
                            return InsetsController.InternalAnimationControlListener.lambda$getAlphaInterpolator$2(f);
                        }
                    };
                }
                if (this.mShow) {
                    return new Interpolator() { // from class: android.view.InsetsController$InternalAnimationControlListener$$ExternalSyntheticLambda3
                        @Override // android.animation.TimeInterpolator
                        public final float getInterpolation(float f) {
                            float min;
                            min = Math.min(1.0f, 2.0f * f);
                            return min;
                        }
                    };
                }
                return InsetsController.FAST_OUT_LINEAR_IN_INTERPOLATOR;
            } else if (this.mBehavior == 2) {
                return new Interpolator() { // from class: android.view.InsetsController$InternalAnimationControlListener$$ExternalSyntheticLambda4
                    @Override // android.animation.TimeInterpolator
                    public final float getInterpolation(float f) {
                        return InsetsController.InternalAnimationControlListener.lambda$getAlphaInterpolator$4(f);
                    }
                };
            } else {
                if (this.mShow) {
                    return InsetsController.SYSTEM_BARS_ALPHA_INTERPOLATOR;
                }
                return InsetsController.SYSTEM_BARS_DIM_INTERPOLATOR;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ float lambda$getAlphaInterpolator$2(float input) {
            return 1.0f;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ float lambda$getAlphaInterpolator$4(float input) {
            return 1.0f;
        }

        protected void onAnimationFinish() {
            this.mController.finish(this.mShow);
        }

        public long getDurationMs() {
            return this.mDurationMs;
        }

        private long calculateDurationMs() {
            if ((this.mRequestedTypes & WindowInsets.Type.ime()) == 0) {
                return this.mBehavior == 2 ? this.mShow ? 275L : 340L : this.mShow ? 500L : 1500L;
            } else if (this.mHasAnimationCallbacks) {
                return 285L;
            } else {
                if (this.mShow) {
                    return InsetsController.mTranImeOpt ? 250L : 200L;
                }
                boolean unused = InsetsController.mTranImeOpt;
                return 200L;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class RunningAnimation {
        final InsetsAnimationControlRunner runner;
        boolean startDispatched;
        final int type;

        RunningAnimation(InsetsAnimationControlRunner runner, int type) {
            this.runner = runner;
            this.type = type;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class PendingControlRequest {
        final int animationType;
        final CancellationSignal cancellationSignal;
        final long durationMs;
        final Interpolator interpolator;
        final int layoutInsetsDuringAnimation;
        final WindowInsetsAnimationControlListener listener;
        final int types;
        final boolean useInsetsAnimationThread;

        PendingControlRequest(int types, WindowInsetsAnimationControlListener listener, long durationMs, Interpolator interpolator, int animationType, int layoutInsetsDuringAnimation, CancellationSignal cancellationSignal, boolean useInsetsAnimationThread) {
            this.types = types;
            this.listener = listener;
            this.durationMs = durationMs;
            this.interpolator = interpolator;
            this.animationType = animationType;
            this.layoutInsetsDuringAnimation = layoutInsetsDuringAnimation;
            this.cancellationSignal = cancellationSignal;
            this.useInsetsAnimationThread = useInsetsAnimationThread;
        }
    }

    public InsetsController(Host host) {
        this(host, new BiFunction() { // from class: android.view.InsetsController$$ExternalSyntheticLambda5
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return InsetsController.lambda$new$2((InsetsController) obj, (Integer) obj2);
            }
        }, host.getHandler());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ InsetsSourceConsumer lambda$new$2(InsetsController controller, Integer type) {
        if (type.intValue() == 19) {
            return new ImeInsetsSourceConsumer(controller.mState, new InsetsController$$ExternalSyntheticLambda4(), controller);
        }
        return new InsetsSourceConsumer(type.intValue(), controller.mState, new InsetsController$$ExternalSyntheticLambda4(), controller);
    }

    public InsetsController(Host host, BiFunction<InsetsController, Integer, InsetsSourceConsumer> consumerCreator, Handler handler) {
        this.mState = new InsetsState();
        this.mLastDispatchedState = new InsetsState();
        this.mRequestedVisibilities = new InsetsVisibilities();
        this.mFrame = new Rect();
        this.mSourceConsumers = new SparseArray<>();
        this.mTmpControlArray = new SparseArray<>();
        this.mRunningAnimations = new ArrayList<>();
        this.mRequestedVisibilityChanged = new ArraySet<>();
        this.mCaptionInsetsHeight = 0;
        this.mPendingControlTimeout = new Runnable() { // from class: android.view.InsetsController$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                InsetsController.this.abortPendingImeControlRequest();
            }
        };
        this.mControllableInsetsChangedListeners = new ArrayList<>();
        this.mInvokeControllableInsetsChangedListeners = new Runnable() { // from class: android.view.InsetsController$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                InsetsController.this.invokeControllableInsetsChangedListeners();
            }
        };
        this.mHost = host;
        this.mConsumerCreator = consumerCreator;
        this.mHandler = handler;
        this.mAnimCallback = new Runnable() { // from class: android.view.InsetsController$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                InsetsController.this.m4813lambda$new$3$androidviewInsetsController();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$3$android-view-InsetsController  reason: not valid java name */
    public /* synthetic */ void m4813lambda$new$3$androidviewInsetsController() {
        this.mAnimCallbackScheduled = false;
        if (this.mRunningAnimations.isEmpty()) {
            return;
        }
        List<WindowInsetsAnimation> runningAnimations = new ArrayList<>();
        List<WindowInsetsAnimation> finishedAnimations = new ArrayList<>();
        InsetsState state = new InsetsState(this.mState, true);
        for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
            RunningAnimation runningAnimation = this.mRunningAnimations.get(i);
            InsetsAnimationControlRunner runner = runningAnimation.runner;
            if (runner instanceof WindowInsetsAnimationController) {
                if (runningAnimation.startDispatched) {
                    runningAnimations.add(runner.getAnimation());
                }
                if (((InternalInsetsAnimationController) runner).applyChangeInsets(state)) {
                    finishedAnimations.add(runner.getAnimation());
                }
            }
        }
        WindowInsets insets = state.calculateInsets(this.mFrame, this.mState, this.mLastInsets.isRound(), this.mLastInsets.shouldAlwaysConsumeSystemBars(), this.mLastLegacySoftInputMode, this.mLastLegacyWindowFlags, this.mLastLegacySystemUiFlags, this.mWindowType, this.mLastWindowingMode, null);
        this.mHost.dispatchWindowInsetsAnimationProgress(insets, Collections.unmodifiableList(runningAnimations));
        for (int i2 = finishedAnimations.size() - 1; i2 >= 0; i2--) {
            dispatchAnimationEnd(finishedAnimations.get(i2));
        }
    }

    public void onFrameChanged(Rect frame) {
        if (this.mFrame.equals(frame)) {
            return;
        }
        this.mHost.notifyInsetsChanged();
        this.mFrame.set(frame);
    }

    @Override // android.view.WindowInsetsController
    public InsetsState getState() {
        return this.mState;
    }

    @Override // android.view.WindowInsetsController
    public boolean isRequestedVisible(int type) {
        return getSourceConsumer(type).isRequestedVisible();
    }

    public InsetsState getLastDispatchedState() {
        return this.mLastDispatchedState;
    }

    public boolean onStateChanged(InsetsState state) {
        boolean stateChanged;
        if (!ViewRootImpl.CAPTION_ON_SHELL) {
            stateChanged = !this.mState.equals(state, true, false) || captionInsetsUnchanged();
        } else {
            stateChanged = !this.mState.equals(state, false, false);
        }
        if (stateChanged || !this.mLastDispatchedState.equals(state)) {
            this.mLastDispatchedState.set(state, true);
            InsetsState lastState = new InsetsState(this.mState, true);
            updateState(state);
            applyLocalVisibilityOverride();
            if (!this.mState.equals(lastState, false, true)) {
                this.mHost.notifyInsetsChanged();
                startResizingAnimationIfNeeded(lastState);
            }
            return true;
        }
        return false;
    }

    private void updateState(InsetsState newState) {
        this.mState.set(newState, 0);
        int disabledUserAnimationTypes = 0;
        final int[] cancelledUserAnimationTypes = {0};
        for (int type = 0; type < 24; type++) {
            InsetsSource source = newState.peekSource(type);
            if (source != null) {
                int animationType = getAnimationType(type);
                if (!source.isUserControllable()) {
                    int insetsType = InsetsState.toPublicType(type);
                    disabledUserAnimationTypes |= insetsType;
                    if (animationType == 2) {
                        animationType = -1;
                        cancelledUserAnimationTypes[0] = cancelledUserAnimationTypes[0] | insetsType;
                    }
                }
                getSourceConsumer(type).updateSource(source, animationType);
            }
        }
        for (int type2 = 0; type2 < 24; type2++) {
            if (type2 != 2 && this.mState.peekSource(type2) != null && newState.peekSource(type2) == null) {
                this.mState.removeSource(type2);
            }
        }
        updateDisabledUserAnimationTypes(disabledUserAnimationTypes);
        if (cancelledUserAnimationTypes[0] != 0) {
            this.mHandler.post(new Runnable() { // from class: android.view.InsetsController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    InsetsController.this.m4815lambda$updateState$4$androidviewInsetsController(cancelledUserAnimationTypes);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateState$4$android-view-InsetsController  reason: not valid java name */
    public /* synthetic */ void m4815lambda$updateState$4$androidviewInsetsController(int[] cancelledUserAnimationTypes) {
        show(cancelledUserAnimationTypes[0]);
    }

    private void updateDisabledUserAnimationTypes(int disabledUserAnimationTypes) {
        int diff = this.mDisabledUserAnimationInsetsTypes ^ disabledUserAnimationTypes;
        if (diff != 0) {
            int i = this.mSourceConsumers.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                InsetsSourceConsumer consumer = this.mSourceConsumers.valueAt(i);
                if (consumer.getControl() == null || (InsetsState.toPublicType(consumer.getType()) & diff) == 0) {
                    i--;
                } else {
                    this.mHandler.removeCallbacks(this.mInvokeControllableInsetsChangedListeners);
                    this.mHandler.post(this.mInvokeControllableInsetsChangedListeners);
                    break;
                }
            }
            this.mDisabledUserAnimationInsetsTypes = disabledUserAnimationTypes;
        }
    }

    private boolean captionInsetsUnchanged() {
        if (ViewRootImpl.CAPTION_ON_SHELL) {
            return false;
        }
        if (this.mState.peekSource(2) == null && this.mCaptionInsetsHeight == 0) {
            return false;
        }
        return this.mState.peekSource(2) == null || this.mCaptionInsetsHeight != this.mState.peekSource(2).getFrame().height();
    }

    private void startResizingAnimationIfNeeded(InsetsState fromState) {
        if (!fromState.getDisplayFrame().equals(this.mState.getDisplayFrame())) {
            return;
        }
        int types = 0;
        InsetsState toState = null;
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(WindowInsets.Type.systemBars());
        for (int i = internalTypes.size() - 1; i >= 0; i--) {
            int type = internalTypes.valueAt(i).intValue();
            InsetsSource fromSource = fromState.peekSource(type);
            InsetsSource toSource = this.mState.peekSource(type);
            if (fromSource != null && toSource != null && fromSource.isVisible() && toSource.isVisible() && !fromSource.getFrame().equals(toSource.getFrame()) && (Rect.intersects(this.mFrame, fromSource.getFrame()) || Rect.intersects(this.mFrame, toSource.getFrame()))) {
                types |= InsetsState.toPublicType(toSource.getType());
                if (toState == null) {
                    toState = new InsetsState();
                }
                toState.addSource(new InsetsSource(toSource));
            }
        }
        if (types == 0) {
            return;
        }
        cancelExistingControllers(types);
        InsetsAnimationControlRunner runner = new InsetsResizeAnimationRunner(this.mFrame, fromState, toState, RESIZE_INTERPOLATOR, 300L, types, this);
        this.mRunningAnimations.add(new RunningAnimation(runner, runner.getAnimationType()));
    }

    public WindowInsets calculateInsets(boolean isScreenRound, boolean alwaysConsumeSystemBars, int windowType, int windowingMode, int legacySoftInputMode, int legacyWindowFlags, int legacySystemUiFlags) {
        this.mWindowType = windowType;
        this.mLastWindowingMode = windowingMode;
        this.mLastLegacySoftInputMode = legacySoftInputMode;
        this.mLastLegacyWindowFlags = legacyWindowFlags;
        this.mLastLegacySystemUiFlags = legacySystemUiFlags;
        WindowInsets calculateInsets = this.mState.calculateInsets(this.mFrame, null, isScreenRound, alwaysConsumeSystemBars, legacySoftInputMode, legacyWindowFlags, legacySystemUiFlags, windowType, windowingMode, null);
        this.mLastInsets = calculateInsets;
        return calculateInsets;
    }

    public Insets calculateVisibleInsets(int windowType, int windowingMode, int softInputMode, int windowFlags) {
        return this.mState.calculateVisibleInsets(this.mFrame, windowType, windowingMode, softInputMode, windowFlags);
    }

    public void onControlsChanged(InsetsSourceControl[] activeControls) {
        boolean requestedVisibilityChanged;
        boolean imeRequestedVisible;
        boolean z;
        if (activeControls != null) {
            for (InsetsSourceControl activeControl : activeControls) {
                if (activeControl != null) {
                    this.mTmpControlArray.put(activeControl.getType(), activeControl);
                }
            }
        }
        boolean requestedVisibilityStale = false;
        int[] showTypes = new int[1];
        int[] hideTypes = new int[1];
        for (int i = this.mSourceConsumers.size() - 1; i >= 0; i--) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.valueAt(i);
            consumer.setControl(this.mTmpControlArray.get(consumer.getType()), showTypes, hideTypes);
        }
        for (int i2 = this.mTmpControlArray.size() - 1; i2 >= 0; i2--) {
            InsetsSourceControl control = this.mTmpControlArray.valueAt(i2);
            int type = control.getType();
            InsetsSourceConsumer consumer2 = getSourceConsumer(type);
            consumer2.setControl(control, showTypes, hideTypes);
            if (!requestedVisibilityStale) {
                boolean requestedVisible = consumer2.isRequestedVisible();
                if (requestedVisible == this.mRequestedVisibilities.getVisibility(type)) {
                    requestedVisibilityChanged = false;
                } else {
                    requestedVisibilityChanged = true;
                }
                if (type != 19 || !requestedVisible) {
                    imeRequestedVisible = false;
                } else {
                    imeRequestedVisible = true;
                }
                if (!requestedVisibilityChanged && !imeRequestedVisible) {
                    z = false;
                } else {
                    z = true;
                }
                requestedVisibilityStale = z;
            }
        }
        if (this.mTmpControlArray.size() > 0) {
            for (int i3 = this.mRunningAnimations.size() - 1; i3 >= 0; i3--) {
                this.mRunningAnimations.get(i3).runner.updateSurfacePosition(this.mTmpControlArray);
            }
        }
        this.mTmpControlArray.clear();
        int animatingTypes = invokeControllableInsetsChangedListeners();
        showTypes[0] = showTypes[0] & (~animatingTypes);
        hideTypes[0] = hideTypes[0] & (~animatingTypes);
        if (showTypes[0] != 0) {
            applyAnimation(showTypes[0], true, false);
        }
        if (hideTypes[0] != 0) {
            applyAnimation(hideTypes[0], false, false);
        }
        updateRequestedVisibilities();
    }

    @Override // android.view.WindowInsetsController
    public void show(int types) {
        show(types, false);
    }

    public void show(int types, boolean fromIme) {
        if ((types & WindowInsets.Type.ime()) != 0) {
            Log.d(TAG, "show(ime(), fromIme=" + fromIme + NavigationBarInflaterView.KEY_CODE_END);
        }
        if (fromIme) {
            ImeTracing.getInstance().triggerClientDump("InsetsController#show", this.mHost.getInputMethodManager(), null);
            Trace.asyncTraceEnd(8L, "IC.showRequestFromApiToImeReady", 0);
            Trace.asyncTraceBegin(8L, "IC.showRequestFromIme", 0);
        } else {
            Trace.asyncTraceBegin(8L, "IC.showRequestFromApi", 0);
            Trace.asyncTraceBegin(8L, "IC.showRequestFromApiToImeReady", 0);
        }
        if (fromIme && this.mPendingImeControlRequest != null) {
            PendingControlRequest pendingRequest = this.mPendingImeControlRequest;
            this.mPendingImeControlRequest = null;
            this.mHandler.removeCallbacks(this.mPendingControlTimeout);
            controlAnimationUnchecked(pendingRequest.types, pendingRequest.cancellationSignal, pendingRequest.listener, null, true, pendingRequest.durationMs, pendingRequest.interpolator, pendingRequest.animationType, pendingRequest.layoutInsetsDuringAnimation, pendingRequest.useInsetsAnimationThread);
            return;
        }
        int typesReady = 0;
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        for (int i = internalTypes.size() - 1; i >= 0; i--) {
            int internalType = internalTypes.valueAt(i).intValue();
            int animationType = getAnimationType(internalType);
            InsetsSourceConsumer consumer = getSourceConsumer(internalType);
            if ((!consumer.isRequestedVisible() || animationType != -1) && animationType != 0 && (!fromIme || animationType != 2)) {
                typesReady |= InsetsState.toPublicType(consumer.getType());
            }
        }
        applyAnimation(typesReady, true, fromIme);
    }

    @Override // android.view.WindowInsetsController
    public void hide(int types) {
        hide(types, false);
    }

    public void hide(int types, boolean fromIme) {
        if (!fromIme) {
            Trace.asyncTraceBegin(8L, "IC.hideRequestFromApi", 0);
        } else {
            ImeTracing.getInstance().triggerClientDump("InsetsController#hide", this.mHost.getInputMethodManager(), null);
            Trace.asyncTraceBegin(8L, "IC.hideRequestFromIme", 0);
        }
        int typesReady = 0;
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        for (int i = internalTypes.size() - 1; i >= 0; i--) {
            int internalType = internalTypes.valueAt(i).intValue();
            int animationType = getAnimationType(internalType);
            InsetsSourceConsumer consumer = getSourceConsumer(internalType);
            if ((consumer.isRequestedVisible() || animationType != -1) && animationType != 1) {
                typesReady |= InsetsState.toPublicType(consumer.getType());
            }
        }
        applyAnimation(typesReady, false, fromIme);
    }

    @Override // android.view.WindowInsetsController
    public void controlWindowInsetsAnimation(int types, long durationMillis, Interpolator interpolator, CancellationSignal cancellationSignal, WindowInsetsAnimationControlListener listener) {
        controlWindowInsetsAnimation(types, cancellationSignal, listener, false, durationMillis, interpolator, 2);
    }

    private void controlWindowInsetsAnimation(int types, CancellationSignal cancellationSignal, WindowInsetsAnimationControlListener listener, boolean fromIme, long durationMs, Interpolator interpolator, int animationType) {
        if ((this.mState.calculateUncontrollableInsetsFromFrame(this.mFrame) & types) != 0) {
            listener.onCancelled(null);
            return;
        }
        if (fromIme) {
            ImeTracing.getInstance().triggerClientDump("InsetsController#controlWindowInsetsAnimation", this.mHost.getInputMethodManager(), null);
        }
        controlAnimationUnchecked(types, cancellationSignal, listener, this.mFrame, fromIme, durationMs, interpolator, animationType, getLayoutInsetsDuringAnimationMode(types), false);
    }

    private void controlAnimationUnchecked(int types, CancellationSignal cancellationSignal, WindowInsetsAnimationControlListener listener, Rect frame, boolean fromIme, long durationMs, Interpolator interpolator, int animationType, int layoutInsetsDuringAnimation, boolean useInsetsAnimationThread) {
        int types2;
        int types3;
        byte[] bArr;
        InsetsAnimationControlRunner insetsAnimationControlImpl;
        boolean z;
        if ((types & this.mTypesBeingCancelled) != 0) {
            throw new IllegalStateException("Cannot start a new insets animation of " + WindowInsets.Type.toString(types) + " while an existing " + WindowInsets.Type.toString(this.mTypesBeingCancelled) + " is being cancelled.");
        }
        if (animationType != 2) {
            types2 = types;
        } else {
            int i = this.mDisabledUserAnimationInsetsTypes;
            int disabledTypes = types & i;
            int types4 = types & (~i);
            if (fromIme && (WindowInsets.Type.ime() & disabledTypes) != 0 && !this.mState.getSource(19).isVisible()) {
                getSourceConsumer(19).hide(true, animationType);
            }
            types2 = types4;
        }
        if (types2 == 0) {
            listener.onCancelled(null);
            updateRequestedVisibilities();
            return;
        }
        cancelExistingControllers(types2);
        this.mLastStartedAnimTypes |= types2;
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types2);
        SparseArray<InsetsSourceControl> controls = new SparseArray<>();
        Pair<Integer, Boolean> typesReadyPair = collectSourceControls(fromIme, internalTypes, controls, animationType);
        int typesReady = typesReadyPair.first.intValue();
        boolean imeReady = typesReadyPair.second.booleanValue();
        if (!imeReady) {
            abortPendingImeControlRequest();
            final PendingControlRequest request = new PendingControlRequest(types2, listener, durationMs, interpolator, animationType, layoutInsetsDuringAnimation, cancellationSignal, useInsetsAnimationThread);
            this.mPendingImeControlRequest = request;
            this.mHandler.postDelayed(this.mPendingControlTimeout, 2000L);
            if (cancellationSignal != null) {
                cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: android.view.InsetsController$$ExternalSyntheticLambda6
                    @Override // android.os.CancellationSignal.OnCancelListener
                    public final void onCancel() {
                        InsetsController.this.m4811lambda$controlAnimationUnchecked$5$androidviewInsetsController(request);
                    }
                });
            }
            updateRequestedVisibilities();
            Trace.asyncTraceEnd(8L, "IC.showRequestFromApi", 0);
            return;
        }
        int types5 = types2;
        if (typesReady == 0) {
            listener.onCancelled(null);
            updateRequestedVisibilities();
            return;
        }
        if (useInsetsAnimationThread) {
            bArr = null;
            types3 = types5;
            insetsAnimationControlImpl = new InsetsAnimationThreadControlRunner(controls, frame, this.mState, listener, typesReady, this, durationMs, interpolator, animationType, layoutInsetsDuringAnimation, this.mHost.getTranslator(), this.mHost.getHandler());
        } else {
            types3 = types5;
            bArr = null;
            insetsAnimationControlImpl = new InsetsAnimationControlImpl(controls, frame, this.mState, listener, typesReady, this, durationMs, interpolator, animationType, layoutInsetsDuringAnimation, this.mHost.getTranslator());
        }
        final InsetsAnimationControlRunner runner = insetsAnimationControlImpl;
        if ((typesReady & WindowInsets.Type.ime()) != 0) {
            ImeTracing.getInstance().triggerClientDump("InsetsAnimationControlImpl", this.mHost.getInputMethodManager(), bArr);
        }
        this.mRunningAnimations.add(new RunningAnimation(runner, animationType));
        if (cancellationSignal != null) {
            cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: android.view.InsetsController$$ExternalSyntheticLambda7
                @Override // android.os.CancellationSignal.OnCancelListener
                public final void onCancel() {
                    InsetsController.this.m4812lambda$controlAnimationUnchecked$6$androidviewInsetsController(runner);
                }
            });
            z = false;
        } else {
            z = false;
            Trace.asyncTraceBegin(8L, "IC.pendingAnim", 0);
        }
        if (layoutInsetsDuringAnimation == 0) {
            showDirectly(types3, fromIme);
        } else {
            hideDirectly(types3, z, animationType, fromIme);
        }
        updateRequestedVisibilities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$controlAnimationUnchecked$5$android-view-InsetsController  reason: not valid java name */
    public /* synthetic */ void m4811lambda$controlAnimationUnchecked$5$androidviewInsetsController(PendingControlRequest request) {
        if (this.mPendingImeControlRequest == request) {
            abortPendingImeControlRequest();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$controlAnimationUnchecked$6$android-view-InsetsController  reason: not valid java name */
    public /* synthetic */ void m4812lambda$controlAnimationUnchecked$6$androidviewInsetsController(InsetsAnimationControlRunner runner) {
        cancelAnimation(runner, true);
    }

    private Pair<Integer, Boolean> collectSourceControls(boolean fromIme, ArraySet<Integer> internalTypes, SparseArray<InsetsSourceControl> controls, int animationType) {
        int typesReady = 0;
        boolean imeReady = true;
        boolean z = true;
        int i = internalTypes.size() - 1;
        while (i >= 0) {
            InsetsSourceConsumer consumer = getSourceConsumer(internalTypes.valueAt(i).intValue());
            boolean show = (animationType == 0 || animationType == 2) ? z : false;
            boolean canRun = false;
            if (show) {
                if (fromIme) {
                    ImeTracing.getInstance().triggerClientDump("ImeInsetsSourceConsumer#requestShow", this.mHost.getInputMethodManager(), null);
                }
                switch (consumer.requestShow(fromIme)) {
                    case 0:
                        canRun = true;
                        break;
                    case 1:
                        imeReady = false;
                        break;
                }
            } else {
                if (!fromIme) {
                    consumer.notifyHidden();
                }
                canRun = true;
            }
            if (canRun) {
                InsetsSourceControl control = consumer.getControl();
                if (control != null && control.getLeash() != null) {
                    controls.put(consumer.getType(), new InsetsSourceControl(control));
                    typesReady |= InsetsState.toPublicType(consumer.getType());
                } else if (animationType == 0) {
                    if (fromIme) {
                        ImeTracing.getInstance().triggerClientDump("InsetsSourceConsumer#show", this.mHost.getInputMethodManager(), null);
                    }
                    consumer.show(fromIme);
                } else if (animationType == 1) {
                    if (fromIme) {
                        ImeTracing.getInstance().triggerClientDump("InsetsSourceConsumer#hide", this.mHost.getInputMethodManager(), null);
                    }
                    consumer.hide();
                }
            }
            i--;
            z = true;
        }
        return new Pair<>(Integer.valueOf(typesReady), Boolean.valueOf(imeReady));
    }

    private int getLayoutInsetsDuringAnimationMode(int types) {
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        for (int i = internalTypes.size() - 1; i >= 0; i--) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.get(internalTypes.valueAt(i).intValue());
            if (consumer != null && !consumer.isRequestedVisible()) {
                return 0;
            }
        }
        return 1;
    }

    private void cancelExistingControllers(int types) {
        int originalmTypesBeingCancelled = this.mTypesBeingCancelled;
        this.mTypesBeingCancelled |= types;
        try {
            for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
                InsetsAnimationControlRunner control = this.mRunningAnimations.get(i).runner;
                if ((control.getTypes() & types) != 0) {
                    cancelAnimation(control, true);
                }
            }
            int i2 = WindowInsets.Type.ime();
            if ((i2 & types) != 0) {
                abortPendingImeControlRequest();
            }
        } finally {
            this.mTypesBeingCancelled = originalmTypesBeingCancelled;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void abortPendingImeControlRequest() {
        PendingControlRequest pendingControlRequest = this.mPendingImeControlRequest;
        if (pendingControlRequest != null) {
            pendingControlRequest.listener.onCancelled(null);
            this.mPendingImeControlRequest = null;
            this.mHandler.removeCallbacks(this.mPendingControlTimeout);
        }
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public void notifyFinished(InsetsAnimationControlRunner runner, boolean shown) {
        cancelAnimation(runner, false);
        if (runner.getAnimationType() == 3) {
            return;
        }
        if (shown) {
            showDirectly(runner.getTypes(), true);
        } else {
            hideDirectly(runner.getTypes(), true, runner.getAnimationType(), true);
        }
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public void applySurfaceParams(SyncRtSurfaceTransactionApplier.SurfaceParams... params) {
        this.mHost.applySurfaceParams(params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyControlRevoked(InsetsSourceConsumer consumer) {
        int types = InsetsState.toPublicType(consumer.getType());
        for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
            InsetsAnimationControlRunner control = this.mRunningAnimations.get(i).runner;
            control.notifyControlRevoked(types);
            if (control.getControllingTypes() == 0) {
                cancelAnimation(control, true);
            }
        }
        int i2 = consumer.getType();
        if (i2 == 19) {
            abortPendingImeControlRequest();
        }
    }

    private void cancelAnimation(InsetsAnimationControlRunner control, boolean invokeCallback) {
        if (invokeCallback) {
            control.cancel();
        }
        boolean stateChanged = false;
        int i = this.mRunningAnimations.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            RunningAnimation runningAnimation = this.mRunningAnimations.get(i);
            if (runningAnimation.runner != control) {
                i--;
            } else {
                this.mRunningAnimations.remove(i);
                ArraySet<Integer> types = InsetsState.toInternalType(control.getTypes());
                for (int j = types.size() - 1; j >= 0; j--) {
                    if (types.valueAt(j).intValue() == 19) {
                        ImeTracing.getInstance().triggerClientDump("InsetsSourceConsumer#notifyAnimationFinished", this.mHost.getInputMethodManager(), null);
                    }
                    stateChanged |= getSourceConsumer(types.valueAt(j).intValue()).notifyAnimationFinished();
                }
                if (invokeCallback) {
                    dispatchAnimationEnd(runningAnimation.runner.getAnimation());
                }
            }
        }
        if (stateChanged) {
            this.mHost.notifyInsetsChanged();
        }
    }

    private void applyLocalVisibilityOverride() {
        for (int i = this.mSourceConsumers.size() - 1; i >= 0; i--) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.valueAt(i);
            consumer.applyLocalVisibilityOverride();
        }
    }

    public InsetsSourceConsumer getSourceConsumer(int type) {
        InsetsSourceConsumer controller = this.mSourceConsumers.get(type);
        if (controller != null) {
            return controller;
        }
        InsetsSourceConsumer controller2 = this.mConsumerCreator.apply(this, Integer.valueOf(type));
        this.mSourceConsumers.put(type, controller2);
        return controller2;
    }

    public void notifyVisibilityChanged() {
        this.mHost.notifyInsetsChanged();
    }

    public void updateCompatSysUiVisibility(int type, boolean visible, boolean hasControl) {
        this.mHost.updateCompatSysUiVisibility(type, visible, hasControl);
    }

    public void onWindowFocusGained(boolean hasViewFocused) {
        getSourceConsumer(19).onWindowFocusGained(hasViewFocused);
    }

    public void onWindowFocusLost() {
        getSourceConsumer(19).onWindowFocusLost();
    }

    public int getAnimationType(int type) {
        for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
            InsetsAnimationControlRunner control = this.mRunningAnimations.get(i).runner;
            if (control.controlsInternalType(type)) {
                return this.mRunningAnimations.get(i).type;
            }
        }
        return -1;
    }

    public void onRequestedVisibilityChanged(InsetsSourceConsumer consumer) {
        this.mRequestedVisibilityChanged.add(consumer);
    }

    private void updateRequestedVisibilities() {
        boolean requestedVisible;
        boolean changed = false;
        for (int i = this.mRequestedVisibilityChanged.size() - 1; i >= 0; i--) {
            InsetsSourceConsumer consumer = this.mRequestedVisibilityChanged.valueAt(i);
            int type = consumer.getType();
            if (type != 2 && this.mRequestedVisibilities.getVisibility(type) != (requestedVisible = consumer.isRequestedVisible())) {
                this.mRequestedVisibilities.setVisibility(type, requestedVisible);
                changed = true;
            }
        }
        this.mRequestedVisibilityChanged.clear();
        if (!changed) {
            return;
        }
        this.mHost.updateRequestedVisibilities(this.mRequestedVisibilities);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsVisibilities getRequestedVisibilities() {
        return this.mRequestedVisibilities;
    }

    public void applyAnimation(int types, boolean show, boolean fromIme) {
        boolean skipAnim = false;
        if ((WindowInsets.Type.ime() & types) != 0) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.get(19);
            InsetsSourceControl imeControl = consumer != null ? consumer.getControl() : null;
            if (imeControl != null) {
                skipAnim = imeControl.getAndClearSkipAnimationOnce() && show && consumer.hasViewFocusWhenWindowFocusGain();
            }
        }
        applyAnimation(types, show, fromIme, skipAnim);
    }

    public void applyAnimation(int types, boolean show, boolean fromIme, boolean skipAnim) {
        if (types == 0) {
            return;
        }
        boolean hasAnimationCallbacks = this.mHost.hasAnimationCallbacks();
        InternalAnimationControlListener listener = new InternalAnimationControlListener(show, hasAnimationCallbacks, types, this.mHost.getSystemBarsBehavior(), skipAnim || this.mAnimationsDisabled, this.mHost.dipToPx(-80));
        controlAnimationUnchecked(types, null, listener, null, fromIme, listener.getDurationMs(), listener.getInsetsInterpolator(), !show ? 1 : 0, !show ? 1 : 0, !hasAnimationCallbacks);
    }

    private void hideDirectly(int types, boolean animationFinished, int animationType, boolean fromIme) {
        if ((WindowInsets.Type.ime() & types) != 0) {
            ImeTracing.getInstance().triggerClientDump("InsetsController#hideDirectly", this.mHost.getInputMethodManager(), null);
        }
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        for (int i = internalTypes.size() - 1; i >= 0; i--) {
            getSourceConsumer(internalTypes.valueAt(i).intValue()).hide(animationFinished, animationType);
        }
        updateRequestedVisibilities();
        if (fromIme) {
            Trace.asyncTraceEnd(8L, "IC.hideRequestFromIme", 0);
        }
    }

    private void showDirectly(int types, boolean fromIme) {
        if ((WindowInsets.Type.ime() & types) != 0) {
            ImeTracing.getInstance().triggerClientDump("InsetsController#showDirectly", this.mHost.getInputMethodManager(), null);
        }
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        int i = internalTypes.size();
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            getSourceConsumer(internalTypes.valueAt(i).intValue()).show(false);
        }
        updateRequestedVisibilities();
        if (fromIme) {
            Trace.asyncTraceEnd(8L, "IC.showRequestFromIme", 0);
        }
    }

    public void cancelExistingAnimations() {
        cancelExistingControllers(WindowInsets.Type.all());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.println("InsetsController:");
        this.mState.dump(prefix + "  ", pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mState.dumpDebug(proto, 1146756268033L);
        for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
            InsetsAnimationControlRunner runner = this.mRunningAnimations.get(i).runner;
            runner.dumpDebug(proto, 2246267895810L);
        }
        proto.end(token);
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public <T extends InsetsAnimationControlRunner & InternalInsetsAnimationController> void startAnimation(final T runner, final WindowInsetsAnimationControlListener listener, final int types, final WindowInsetsAnimation animation, final WindowInsetsAnimation.Bounds bounds) {
        this.mHost.dispatchWindowInsetsAnimationPrepare(animation);
        this.mHost.addOnPreDrawRunnable(new Runnable() { // from class: android.view.InsetsController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                InsetsController.this.m4814lambda$startAnimation$7$androidviewInsetsController(runner, types, animation, bounds, listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startAnimation$7$android-view-InsetsController  reason: not valid java name */
    public /* synthetic */ void m4814lambda$startAnimation$7$androidviewInsetsController(InsetsAnimationControlRunner runner, int types, WindowInsetsAnimation animation, WindowInsetsAnimation.Bounds bounds, WindowInsetsAnimationControlListener listener) {
        if (((WindowInsetsAnimationController) runner).isCancelled()) {
            return;
        }
        Trace.asyncTraceBegin(8L, "InsetsAnimation: " + WindowInsets.Type.toString(types), types);
        for (int i = this.mRunningAnimations.size() - 1; i >= 0; i--) {
            RunningAnimation runningAnimation = this.mRunningAnimations.get(i);
            if (runningAnimation.runner == runner) {
                runningAnimation.startDispatched = true;
            }
        }
        Trace.asyncTraceEnd(8L, "IC.pendingAnim", 0);
        this.mHost.dispatchWindowInsetsAnimationStart(animation, bounds);
        this.mStartingAnimation = true;
        ((InternalInsetsAnimationController) runner).setReadyDispatched(true);
        listener.onReady((WindowInsetsAnimationController) runner, types);
        this.mStartingAnimation = false;
    }

    public void dispatchAnimationEnd(WindowInsetsAnimation animation) {
        Trace.asyncTraceEnd(8L, "InsetsAnimation: " + WindowInsets.Type.toString(animation.getTypeMask()), animation.getTypeMask());
        this.mHost.dispatchWindowInsetsAnimationEnd(animation);
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public void scheduleApplyChangeInsets(InsetsAnimationControlRunner runner) {
        if (this.mStartingAnimation || runner.getAnimationType() == 2) {
            this.mAnimCallback.run();
            this.mAnimCallbackScheduled = false;
        } else if (!this.mAnimCallbackScheduled) {
            this.mHost.postInsetsAnimationCallback(this.mAnimCallback);
            this.mAnimCallbackScheduled = true;
        }
    }

    @Override // android.view.WindowInsetsController
    public void setSystemBarsAppearance(int appearance, int mask) {
        this.mHost.setSystemBarsAppearance(appearance, mask);
    }

    @Override // android.view.WindowInsetsController
    public int getSystemBarsAppearance() {
        if (!this.mHost.isSystemBarsAppearanceControlled()) {
            return 0;
        }
        return this.mHost.getSystemBarsAppearance();
    }

    @Override // android.view.WindowInsetsController
    public void setCaptionInsetsHeight(int height) {
        if (!ViewRootImpl.CAPTION_ON_SHELL && this.mCaptionInsetsHeight != height) {
            this.mCaptionInsetsHeight = height;
            if (height != 0) {
                this.mState.getSource(2).setFrame(this.mFrame.left, this.mFrame.top, this.mFrame.right, this.mFrame.top + this.mCaptionInsetsHeight);
            } else {
                this.mState.removeSource(2);
            }
            this.mHost.notifyInsetsChanged();
        }
    }

    @Override // android.view.WindowInsetsController
    public void setSystemBarsBehavior(int behavior) {
        this.mHost.setSystemBarsBehavior(behavior);
    }

    @Override // android.view.WindowInsetsController
    public int getSystemBarsBehavior() {
        if (!this.mHost.isSystemBarsBehaviorControlled()) {
            return 0;
        }
        return this.mHost.getSystemBarsBehavior();
    }

    @Override // android.view.WindowInsetsController
    public void setAnimationsDisabled(boolean disable) {
        this.mAnimationsDisabled = disable;
    }

    private int calculateControllableTypes() {
        int result = 0;
        for (int i = this.mSourceConsumers.size() - 1; i >= 0; i--) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.valueAt(i);
            InsetsSource source = this.mState.peekSource(consumer.mType);
            if (consumer.getControl() != null && source != null && source.isUserControllable()) {
                result |= InsetsState.toPublicType(consumer.mType);
            }
        }
        return (~this.mState.calculateUncontrollableInsetsFromFrame(this.mFrame)) & result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int invokeControllableInsetsChangedListeners() {
        this.mHandler.removeCallbacks(this.mInvokeControllableInsetsChangedListeners);
        this.mLastStartedAnimTypes = 0;
        int types = calculateControllableTypes();
        int size = this.mControllableInsetsChangedListeners.size();
        for (int i = 0; i < size; i++) {
            this.mControllableInsetsChangedListeners.get(i).onControllableInsetsChanged(this, types);
        }
        int i2 = this.mLastStartedAnimTypes;
        return i2;
    }

    @Override // android.view.WindowInsetsController
    public void addOnControllableInsetsChangedListener(WindowInsetsController.OnControllableInsetsChangedListener listener) {
        Objects.requireNonNull(listener);
        this.mControllableInsetsChangedListeners.add(listener);
        listener.onControllableInsetsChanged(this, calculateControllableTypes());
    }

    @Override // android.view.WindowInsetsController
    public void removeOnControllableInsetsChangedListener(WindowInsetsController.OnControllableInsetsChangedListener listener) {
        Objects.requireNonNull(listener);
        this.mControllableInsetsChangedListeners.remove(listener);
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public void releaseSurfaceControlFromRt(SurfaceControl sc) {
        this.mHost.releaseSurfaceControlFromRt(sc);
    }

    @Override // android.view.InsetsAnimationControlCallbacks
    public void reportPerceptible(int types, boolean perceptible) {
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(types);
        int size = this.mSourceConsumers.size();
        for (int i = 0; i < size; i++) {
            InsetsSourceConsumer consumer = this.mSourceConsumers.valueAt(i);
            if (internalTypes.contains(Integer.valueOf(consumer.getType()))) {
                consumer.onPerceptible(perceptible);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Host getHost() {
        return this.mHost;
    }
}
