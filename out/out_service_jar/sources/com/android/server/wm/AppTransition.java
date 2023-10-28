package com.android.server.wm;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.HardwareBuffer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.RemoteAnimationAdapter;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.internal.R;
import com.android.internal.policy.TransitionAnimation;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.transsion.hubcore.server.wm.ITranAppTransition;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class AppTransition implements DumpUtils.Dump {
    private static final int APP_STATE_IDLE = 0;
    private static final int APP_STATE_READY = 1;
    private static final int APP_STATE_RUNNING = 2;
    private static final int APP_STATE_TIMEOUT = 3;
    private static final long APP_TRANSITION_TIMEOUT_MS = 5000;
    static final int DEFAULT_APP_TRANSITION_DURATION = 336;
    static final int MAX_APP_TRANSITION_DURATION = 3000;
    private static final int NEXT_TRANSIT_TYPE_CLIP_REVEAL = 8;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM = 1;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE = 7;
    private static final int NEXT_TRANSIT_TYPE_NONE = 0;
    private static final int NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS = 9;
    private static final int NEXT_TRANSIT_TYPE_REMOTE = 10;
    private static final int NEXT_TRANSIT_TYPE_SCALE_UP = 2;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN = 6;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP = 5;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN = 4;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP = 3;
    private static final String TAG = "WindowManager";
    private static final ArrayList<Pair<Integer, String>> sFlagToString;
    private IRemoteCallback mAnimationFinishedCallback;
    private final Context mContext;
    private AppTransitionAnimationSpec mDefaultNextAppTransitionAnimationSpec;
    private final int mDefaultWindowAnimationStyleResId;
    private final DisplayContent mDisplayContent;
    final Handler mHandler;
    private WindowManagerInternal.KeyguardExitAnimationStartListener mKeyguardExitAnimationStartListener;
    private String mLastChangingApp;
    private String mLastClosingApp;
    private String mLastOpeningApp;
    private IAppTransitionAnimationSpecsFuture mNextAppTransitionAnimationsSpecsFuture;
    private boolean mNextAppTransitionAnimationsSpecsPending;
    private int mNextAppTransitionBackgroundColor;
    private IRemoteCallback mNextAppTransitionCallback;
    private int mNextAppTransitionEnter;
    private int mNextAppTransitionExit;
    private IRemoteCallback mNextAppTransitionFutureCallback;
    private int mNextAppTransitionInPlace;
    private boolean mNextAppTransitionIsSync;
    private boolean mNextAppTransitionOverrideRequested;
    private String mNextAppTransitionPackage;
    private boolean mNextAppTransitionScaleUp;
    private boolean mOverrideTaskTransition;
    private RemoteAnimationController mRemoteAnimationController;
    private final WindowManagerService mService;
    private RemoteAnimationController mThunderbackRemoteAnimationController;
    private final TransitionAnimation mTransitionAnimation;
    private int mNextAppTransitionFlags = 0;
    private boolean mSkipAppTransitionAnim = false;
    private final ArrayList<Integer> mNextAppTransitionRequests = new ArrayList<>();
    private int mLastUsedAppTransition = -1;
    private int mNextAppTransitionType = 0;
    private final SparseArray<AppTransitionAnimationSpec> mNextAppTransitionAnimationsSpecs = new SparseArray<>();
    private final Rect mTmpRect = new Rect();
    private int mAppTransitionState = 0;
    private final ArrayList<WindowManagerInternal.AppTransitionListener> mListeners = new ArrayList<>();
    private final ExecutorService mDefaultExecutor = Executors.newSingleThreadExecutor();
    final Runnable mHandleAppTransitionTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.AppTransition$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            AppTransition.this.m7855lambda$new$0$comandroidserverwmAppTransition();
        }
    };
    private final boolean mGridLayoutRecentsEnabled = SystemProperties.getBoolean("ro.recents.grid", false);

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppTransition(Context context, WindowManagerService service, DisplayContent displayContent) {
        this.mContext = context;
        this.mService = service;
        this.mHandler = new Handler(service.mH.getLooper());
        this.mDisplayContent = displayContent;
        this.mTransitionAnimation = new TransitionAnimation(context, ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_ANIM), "WindowManager");
        TypedArray windowStyle = context.getTheme().obtainStyledAttributes(R.styleable.Window);
        this.mDefaultWindowAnimationStyleResId = windowStyle.getResourceId(8, 0);
        windowStyle.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransitionSet() {
        return !this.mNextAppTransitionRequests.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUnoccluding() {
        return this.mNextAppTransitionRequests.contains(9);
    }

    boolean isTransitOldThunder() {
        return this.mNextAppTransitionRequests.contains(32) || this.mNextAppTransitionRequests.contains(33);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean transferFrom(AppTransition other) {
        this.mNextAppTransitionRequests.addAll(other.mNextAppTransitionRequests);
        return prepare();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastAppTransition(int transit, ActivityRecord openingApp, ActivityRecord closingApp, ActivityRecord changingApp) {
        this.mLastUsedAppTransition = transit;
        this.mLastOpeningApp = "" + openingApp;
        this.mLastClosingApp = "" + closingApp;
        this.mLastChangingApp = "" + changingApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReady() {
        int i = this.mAppTransitionState;
        return i == 1 || i == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAppTransitionState() {
        return this.mAppTransitionState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady() {
        setAppTransitionState(1);
        fetchAppTransitionSpecsFromFuture();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abort() {
        RemoteAnimationController remoteAnimationController = this.mRemoteAnimationController;
        if (remoteAnimationController != null) {
            remoteAnimationController.cancelAnimation("aborted");
        }
        clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRunning() {
        return this.mAppTransitionState == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIdle() {
        setAppTransitionState(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIdle() {
        return this.mAppTransitionState == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTimeout() {
        return this.mAppTransitionState == 3;
    }

    void setTimeout() {
        setAppTransitionState(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HardwareBuffer getAppTransitionThumbnailHeader(WindowContainer container) {
        AppTransitionAnimationSpec spec = this.mNextAppTransitionAnimationsSpecs.get(container.hashCode());
        if (spec == null) {
            spec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (spec != null) {
            return spec.buffer;
        }
        return null;
    }

    boolean isNextThumbnailTransitionAspectScaled() {
        int i = this.mNextAppTransitionType;
        return i == 5 || i == 6;
    }

    boolean isNextThumbnailTransitionScaleUp() {
        return this.mNextAppTransitionScaleUp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNextAppTransitionThumbnailUp() {
        int i = this.mNextAppTransitionType;
        return i == 3 || i == 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNextAppTransitionThumbnailDown() {
        int i = this.mNextAppTransitionType;
        return i == 4 || i == 6;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNextAppTransitionOpenCrossProfileApps() {
        return this.mNextAppTransitionType == 9;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFetchingAppTransitionsSpecs() {
        return this.mNextAppTransitionAnimationsSpecsPending;
    }

    private boolean prepare() {
        if (isRunning()) {
            return false;
        }
        setAppTransitionState(0);
        notifyAppTransitionPendingLocked();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int goodToGo(int transit, ActivityRecord topOpeningApp) {
        long uptimeMillis;
        this.mNextAppTransitionFlags = 0;
        this.mSkipAppTransitionAnim = false;
        this.mNextAppTransitionRequests.clear();
        setAppTransitionState(2);
        WindowContainer wc = topOpeningApp != null ? topOpeningApp.getAnimatingContainer() : null;
        AnimationAdapter topOpeningAnim = wc != null ? wc.getAnimation() : null;
        boolean isKeyguardGoingAwayTransitOld = isKeyguardGoingAwayTransitOld(transit);
        boolean isKeyguardOccludeTransitOld = isKeyguardOccludeTransitOld(transit);
        long durationHint = topOpeningAnim != null ? topOpeningAnim.getDurationHint() : 0L;
        if (topOpeningAnim != null) {
            uptimeMillis = topOpeningAnim.getStatusBarTransitionsStartTime();
        } else {
            uptimeMillis = SystemClock.uptimeMillis();
        }
        int redoLayout = notifyAppTransitionStartingLocked(isKeyguardGoingAwayTransitOld, isKeyguardOccludeTransitOld, durationHint, uptimeMillis, 120L);
        RemoteAnimationController remoteAnimationController = this.mRemoteAnimationController;
        if (remoteAnimationController != null) {
            remoteAnimationController.goodToGo(transit);
        } else if ((isTaskOpenTransitOld(transit) || transit == 12) && topOpeningAnim != null && this.mDisplayContent.getDisplayPolicy().shouldAttachNavBarToAppDuringTransition() && this.mService.getRecentsAnimationController() == null) {
            NavBarFadeAnimationController controller = new NavBarFadeAnimationController(this.mDisplayContent);
            controller.fadeOutAndInSequentially(topOpeningAnim.getDurationHint(), null, topOpeningApp.getSurfaceControl());
        }
        return redoLayout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mNextAppTransitionType = 0;
        this.mNextAppTransitionOverrideRequested = false;
        this.mNextAppTransitionPackage = null;
        this.mNextAppTransitionAnimationsSpecs.clear();
        this.mRemoteAnimationController = null;
        this.mNextAppTransitionAnimationsSpecsFuture = null;
        this.mDefaultNextAppTransitionAnimationSpec = null;
        this.mAnimationFinishedCallback = null;
        this.mOverrideTaskTransition = false;
        this.mNextAppTransitionIsSync = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void freeze() {
        boolean keyguardGoingAway = this.mNextAppTransitionRequests.contains(7);
        RemoteAnimationController remoteAnimationController = this.mRemoteAnimationController;
        if (remoteAnimationController != null) {
            remoteAnimationController.cancelAnimation("freeze");
        }
        this.mNextAppTransitionRequests.clear();
        clear();
        setReady();
        notifyAppTransitionCancelledLocked(keyguardGoingAway);
    }

    private void setAppTransitionState(int state) {
        this.mAppTransitionState = state;
        updateBooster();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setAppTransitionRunning(needsBoosting());
    }

    private boolean needsBoosting() {
        int i;
        boolean recentsAnimRunning = this.mService.getRecentsAnimationController() != null;
        return !this.mNextAppTransitionRequests.isEmpty() || (i = this.mAppTransitionState) == 1 || i == 2 || recentsAnimRunning;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerListenerLocked(WindowManagerInternal.AppTransitionListener listener) {
        this.mListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterListener(WindowManagerInternal.AppTransitionListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerKeygaurdExitAnimationStartListener(WindowManagerInternal.KeyguardExitAnimationStartListener listener) {
        this.mKeyguardExitAnimationStartListener = listener;
    }

    public void notifyAppTransitionFinishedLocked(IBinder token) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionFinishedLocked(token);
        }
    }

    private void notifyAppTransitionPendingLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionPendingLocked();
        }
    }

    private void notifyAppTransitionCancelledLocked(boolean keyguardGoingAway) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionCancelledLocked(keyguardGoingAway);
        }
    }

    private void notifyAppTransitionTimeoutLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionTimeoutLocked();
        }
    }

    private int notifyAppTransitionStartingLocked(boolean keyguardGoingAway, boolean keyguardOcclude, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
        int redoLayout = 0;
        for (int i = 0; i < this.mListeners.size(); i++) {
            redoLayout |= this.mListeners.get(i).onAppTransitionStartingLocked(keyguardGoingAway, keyguardOcclude, duration, statusBarAnimationStartTime, statusBarAnimationDuration);
        }
        return redoLayout;
    }

    int getDefaultWindowAnimationStyleResId() {
        return this.mDefaultWindowAnimationStyleResId;
    }

    int getAnimationStyleResId(WindowManager.LayoutParams lp) {
        return this.mTransitionAnimation.getAnimationStyleResId(lp);
    }

    Animation loadAnimationSafely(Context context, int resId) {
        return TransitionAnimation.loadAnimationSafely(context, resId, "WindowManager");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Animation loadAnimationAttr(WindowManager.LayoutParams lp, int animAttr, int transit) {
        return this.mTransitionAnimation.loadAnimationAttr(lp, animAttr, transit);
    }

    private void getDefaultNextAppTransitionStartRect(Rect rect) {
        AppTransitionAnimationSpec appTransitionAnimationSpec = this.mDefaultNextAppTransitionAnimationSpec;
        if (appTransitionAnimationSpec == null || appTransitionAnimationSpec.rect == null) {
            Slog.e("WindowManager", "Starting rect for app requested, but none available", new Throwable());
            rect.setEmpty();
            return;
        }
        rect.set(this.mDefaultNextAppTransitionAnimationSpec.rect);
    }

    private void putDefaultNextAppTransitionCoordinates(int left, int top, int width, int height, HardwareBuffer buffer) {
        this.mDefaultNextAppTransitionAnimationSpec = new AppTransitionAnimationSpec(-1, buffer, new Rect(left, top, left + width, top + height));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HardwareBuffer createCrossProfileAppsThumbnail(Drawable thumbnailDrawable, Rect frame) {
        return this.mTransitionAnimation.createCrossProfileAppsThumbnail(thumbnailDrawable, frame);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Animation createCrossProfileAppsThumbnailAnimationLocked(Rect appRect) {
        return this.mTransitionAnimation.createCrossProfileAppsThumbnailAnimationLocked(appRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Animation createThumbnailAspectScaleAnimationLocked(Rect appRect, Rect contentInsets, HardwareBuffer thumbnailHeader, WindowContainer container, int orientation) {
        AppTransitionAnimationSpec spec = this.mNextAppTransitionAnimationsSpecs.get(container.hashCode());
        TransitionAnimation transitionAnimation = this.mTransitionAnimation;
        Rect rect = null;
        Rect rect2 = spec != null ? spec.rect : null;
        AppTransitionAnimationSpec appTransitionAnimationSpec = this.mDefaultNextAppTransitionAnimationSpec;
        if (appTransitionAnimationSpec != null) {
            rect = appTransitionAnimationSpec.rect;
        }
        return transitionAnimation.createThumbnailAspectScaleAnimationLocked(appRect, contentInsets, thumbnailHeader, orientation, rect2, rect, this.mNextAppTransitionScaleUp);
    }

    private AnimationSet createAspectScaledThumbnailFreeformAnimationLocked(Rect sourceFrame, Rect destFrame, Rect surfaceInsets, boolean enter) {
        float sourceWidth = sourceFrame.width();
        float sourceHeight = sourceFrame.height();
        float destWidth = destFrame.width();
        float destHeight = destFrame.height();
        float scaleH = enter ? sourceWidth / destWidth : destWidth / sourceWidth;
        float scaleV = enter ? sourceHeight / destHeight : destHeight / sourceHeight;
        AnimationSet set = new AnimationSet(true);
        int surfaceInsetsH = surfaceInsets == null ? 0 : surfaceInsets.left + surfaceInsets.right;
        int surfaceInsetsV = surfaceInsets != null ? surfaceInsets.top + surfaceInsets.bottom : 0;
        float scaleHCenter = ((enter ? destWidth : sourceWidth) + surfaceInsetsH) / 2.0f;
        float scaleVCenter = ((enter ? destHeight : sourceHeight) + surfaceInsetsV) / 2.0f;
        ScaleAnimation scale = enter ? new ScaleAnimation(scaleH, 1.0f, scaleV, 1.0f, scaleHCenter, scaleVCenter) : new ScaleAnimation(1.0f, scaleH, 1.0f, scaleV, scaleHCenter, scaleVCenter);
        int sourceHCenter = sourceFrame.left + (sourceFrame.width() / 2);
        int sourceVCenter = sourceFrame.top + (sourceFrame.height() / 2);
        int destHCenter = destFrame.left + (destFrame.width() / 2);
        int destVCenter = destFrame.top + (destFrame.height() / 2);
        int fromX = enter ? sourceHCenter - destHCenter : destHCenter - sourceHCenter;
        int fromY = enter ? sourceVCenter - destVCenter : destVCenter - sourceVCenter;
        TranslateAnimation translation = enter ? new TranslateAnimation(fromX, 0.0f, fromY, 0.0f) : new TranslateAnimation(0.0f, fromX, 0.0f, fromY);
        set.addAnimation(scale);
        set.addAnimation(translation);
        setAppTransitionFinishedCallbackIfNeeded(set);
        return set;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canSkipFirstFrame() {
        int i = this.mNextAppTransitionType;
        return (i == 1 || this.mNextAppTransitionOverrideRequested || i == 7 || i == 8 || this.mNextAppTransitionRequests.contains(7)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationController getRemoteAnimationController() {
        return this.mRemoteAnimationController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r18v2 */
    /* JADX WARN: Type inference failed for: r18v3 */
    /* JADX WARN: Type inference failed for: r18v6 */
    /* JADX WARN: Type inference failed for: r19v0 */
    /* JADX WARN: Type inference failed for: r19v1 */
    /* JADX WARN: Type inference failed for: r19v10 */
    /* JADX WARN: Type inference failed for: r19v11 */
    /* JADX WARN: Type inference failed for: r21v0 */
    /* JADX WARN: Type inference failed for: r21v1 */
    /* JADX WARN: Type inference failed for: r21v2 */
    /* JADX WARN: Type inference failed for: r21v7 */
    /* JADX WARN: Type inference failed for: r21v8 */
    /* JADX WARN: Type inference failed for: r7v3 */
    /* JADX WARN: Type inference failed for: r7v4, types: [android.graphics.Rect] */
    public Animation loadAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, int uiMode, int orientation, Rect frame, Rect displayFrame, Rect insets, Rect surfaceInsets, Rect stableInsets, boolean isVoiceInteraction, boolean freeform, WindowContainer container) {
        AppTransition appTransition;
        int i;
        Animation a;
        int i2;
        ?? r21;
        int i3;
        String str;
        char c;
        ?? r19;
        int i4;
        int i5;
        ?? r18;
        Rect rect;
        char c2;
        boolean z;
        Animation a2;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        int i15;
        Animation loadKeyguardExitAnimation;
        if (this.mNextAppTransitionOverrideRequested && (container.canCustomizeAppTransition() || this.mOverrideTaskTransition)) {
            this.mNextAppTransitionType = 1;
        }
        Pair<Boolean, Animation> liceRet = IWindowManagerServiceLice.Instance().loadAnimation(this.mContext, UserHandle.getCallingUserId(), lp, transit, enter, uiMode, orientation, frame, displayFrame, insets, surfaceInsets, stableInsets, isVoiceInteraction, freeform);
        if (((Boolean) liceRet.first).booleanValue()) {
            return (Animation) liceRet.second;
        }
        if (isKeyguardGoingAwayTransitOld(transit) && enter) {
            int animAttr = ITranAppTransition.Instance().getAnimAttr(transit);
            if (animAttr != 0) {
                appTransition = this;
                loadKeyguardExitAnimation = appTransition.loadAnimationAttr(lp, animAttr, transit);
            } else {
                appTransition = this;
                loadKeyguardExitAnimation = appTransition.mTransitionAnimation.loadKeyguardExitAnimation(appTransition.mNextAppTransitionFlags, transit == 21);
            }
            a = loadKeyguardExitAnimation;
        } else {
            appTransition = this;
            if (transit == 22) {
                a = null;
            } else if (transit == 23 && !enter) {
                a = appTransition.mTransitionAnimation.loadKeyguardUnoccludeAnimation();
            } else if (transit == 26) {
                a = null;
            } else {
                int i16 = 10;
                if (isVoiceInteraction) {
                    if (transit == 6 || transit == 8 || transit == 10) {
                        a = appTransition.mTransitionAnimation.loadVoiceActivityOpenAnimation(enter);
                        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                            String protoLogParam0 = String.valueOf(a);
                            String protoLogParam1 = String.valueOf(appTransitionOldToString(transit));
                            String protoLogParam3 = String.valueOf(Debug.getCallers(3));
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 508887531, 48, (String) null, new Object[]{protoLogParam0, protoLogParam1, Boolean.valueOf(enter), protoLogParam3});
                        }
                    } else {
                        i = 1;
                    }
                } else {
                    i = 1;
                }
                if (isVoiceInteraction && (transit == 7 || transit == 9 || transit == 11)) {
                    a = appTransition.mTransitionAnimation.loadVoiceActivityExitAnimation(enter);
                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                        String protoLogParam02 = String.valueOf(a);
                        String protoLogParam12 = String.valueOf(appTransitionOldToString(transit));
                        String protoLogParam32 = String.valueOf(Debug.getCallers(3));
                        ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                        Object[] objArr = new Object[4];
                        objArr[0] = protoLogParam02;
                        objArr[i] = protoLogParam12;
                        objArr[2] = Boolean.valueOf(enter);
                        objArr[3] = protoLogParam32;
                        ProtoLogImpl.v(protoLogGroup, 508887531, 48, (String) null, objArr);
                    }
                } else {
                    int i17 = 18;
                    if (transit == 18) {
                        TransitionAnimation transitionAnimation = appTransition.mTransitionAnimation;
                        AppTransitionAnimationSpec appTransitionAnimationSpec = appTransition.mDefaultNextAppTransitionAnimationSpec;
                        a = transitionAnimation.createRelaunchAnimation(frame, insets, appTransitionAnimationSpec != null ? appTransitionAnimationSpec.rect : null);
                        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                            String protoLogParam03 = String.valueOf(a);
                            String protoLogParam13 = String.valueOf(appTransitionOldToString(transit));
                            String protoLogParam2 = String.valueOf(Debug.getCallers(3));
                            ProtoLogGroup protoLogGroup2 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                            Object[] objArr2 = new Object[3];
                            objArr2[0] = protoLogParam03;
                            objArr2[i] = protoLogParam13;
                            objArr2[2] = protoLogParam2;
                            ProtoLogImpl.v(protoLogGroup2, -1800899273, 0, (String) null, objArr2);
                        }
                    } else {
                        int i18 = appTransition.mNextAppTransitionType;
                        if (i18 == i) {
                            a = appTransition.mTransitionAnimation.loadAppTransitionAnimation(appTransition.mNextAppTransitionPackage, enter ? appTransition.mNextAppTransitionEnter : appTransition.mNextAppTransitionExit);
                            int i19 = appTransition.mNextAppTransitionBackgroundColor;
                            if (i19 != 0) {
                                a.setBackdropColor(i19);
                            }
                            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                String protoLogParam04 = String.valueOf(a);
                                String protoLogParam14 = String.valueOf(appTransitionOldToString(transit));
                                String protoLogParam33 = String.valueOf(Debug.getCallers(3));
                                ProtoLogGroup protoLogGroup3 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                Object[] objArr3 = new Object[4];
                                objArr3[0] = protoLogParam04;
                                objArr3[i] = protoLogParam14;
                                objArr3[2] = Boolean.valueOf(enter);
                                objArr3[3] = protoLogParam33;
                                ProtoLogImpl.v(protoLogGroup3, -519504830, 48, (String) null, objArr3);
                            }
                        } else if (i18 == 7) {
                            a = appTransition.mTransitionAnimation.loadAppTransitionAnimation(appTransition.mNextAppTransitionPackage, appTransition.mNextAppTransitionInPlace);
                            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                String protoLogParam05 = String.valueOf(a);
                                String protoLogParam15 = String.valueOf(appTransitionOldToString(transit));
                                String protoLogParam22 = String.valueOf(Debug.getCallers(3));
                                ProtoLogGroup protoLogGroup4 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                Object[] objArr4 = new Object[3];
                                objArr4[0] = protoLogParam05;
                                objArr4[i] = protoLogParam15;
                                objArr4[2] = protoLogParam22;
                                ProtoLogImpl.v(protoLogGroup4, 1457990604, 0, (String) null, objArr4);
                            }
                        } else if (i18 == 8) {
                            TransitionAnimation transitionAnimation2 = appTransition.mTransitionAnimation;
                            AppTransitionAnimationSpec appTransitionAnimationSpec2 = appTransition.mDefaultNextAppTransitionAnimationSpec;
                            a = transitionAnimation2.createClipRevealAnimationLockedCompat(transit, enter, frame, displayFrame, appTransitionAnimationSpec2 != null ? appTransitionAnimationSpec2.rect : null);
                            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                String protoLogParam06 = String.valueOf(a);
                                String protoLogParam16 = String.valueOf(appTransitionOldToString(transit));
                                String protoLogParam23 = String.valueOf(Debug.getCallers(3));
                                ProtoLogGroup protoLogGroup5 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                Object[] objArr5 = new Object[3];
                                objArr5[0] = protoLogParam06;
                                objArr5[i] = protoLogParam16;
                                objArr5[2] = protoLogParam23;
                                ProtoLogImpl.v(protoLogGroup5, 274773837, 0, (String) null, objArr5);
                            }
                        } else if (i18 == 2) {
                            TransitionAnimation transitionAnimation3 = appTransition.mTransitionAnimation;
                            AppTransitionAnimationSpec appTransitionAnimationSpec3 = appTransition.mDefaultNextAppTransitionAnimationSpec;
                            a = transitionAnimation3.createScaleUpAnimationLockedCompat(transit, enter, frame, appTransitionAnimationSpec3 != null ? appTransitionAnimationSpec3.rect : null);
                            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                String protoLogParam07 = String.valueOf(a);
                                String protoLogParam17 = String.valueOf(appTransitionOldToString(transit));
                                String protoLogParam24 = String.valueOf(enter);
                                String protoLogParam34 = String.valueOf(Debug.getCallers(3));
                                ProtoLogGroup protoLogGroup6 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                Object[] objArr6 = new Object[4];
                                objArr6[0] = protoLogParam07;
                                objArr6[i] = protoLogParam17;
                                objArr6[2] = protoLogParam24;
                                objArr6[3] = protoLogParam34;
                                ProtoLogImpl.v(protoLogGroup6, 2028163120, 0, (String) null, objArr6);
                            }
                        } else {
                            if (i18 == 3) {
                                i2 = -1872288685;
                                r21 = 4;
                                i3 = 192;
                                str = null;
                                c = 2;
                                r19 = 1;
                                i4 = 3;
                            } else if (i18 == 4) {
                                i2 = -1872288685;
                                r21 = 4;
                                i3 = 192;
                                str = null;
                                c = 2;
                                r19 = 1;
                                i4 = 3;
                            } else {
                                if (i18 == 5) {
                                    i5 = 3;
                                    r18 = true;
                                    rect = null;
                                    c2 = 2;
                                    z = true;
                                } else if (i18 == 6) {
                                    i5 = 3;
                                    r18 = true;
                                    rect = null;
                                    c2 = 2;
                                    z = true;
                                } else if (i18 == 9 && enter) {
                                    a = appTransition.mTransitionAnimation.loadCrossProfileAppEnterAnimation();
                                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                        String protoLogParam08 = String.valueOf(a);
                                        String protoLogParam18 = String.valueOf(appTransitionOldToString(transit));
                                        String protoLogParam25 = String.valueOf(Debug.getCallers(3));
                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 1589610525, 0, (String) null, new Object[]{protoLogParam08, protoLogParam18, protoLogParam25});
                                    }
                                } else if (isChangeTransitOld(transit)) {
                                    a = new AlphaAnimation(1.0f, 1.0f);
                                    a.setDuration(336L);
                                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                        String protoLogParam09 = String.valueOf(a);
                                        String protoLogParam19 = String.valueOf(appTransitionOldToString(transit));
                                        String protoLogParam35 = String.valueOf(Debug.getCallers(3));
                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, -1862269827, 48, (String) null, new Object[]{protoLogParam09, protoLogParam19, Boolean.valueOf(enter), protoLogParam35});
                                    }
                                } else {
                                    int animAttr2 = 0;
                                    switch (transit) {
                                        case 6:
                                        case 24:
                                            if (enter) {
                                                i6 = 4;
                                            } else {
                                                i6 = 5;
                                            }
                                            animAttr2 = i6;
                                            break;
                                        case 7:
                                        case 25:
                                            if (enter) {
                                                i7 = 6;
                                            } else {
                                                i7 = 7;
                                            }
                                            animAttr2 = i7;
                                            break;
                                        case 8:
                                            if (TranFpUnlockStateController.getInstance().canHideByFingerprint()) {
                                                animAttr2 = 0;
                                                break;
                                            } else {
                                                animAttr2 = enter ? 8 : 9;
                                                break;
                                            }
                                        case 9:
                                            if (!enter) {
                                                i16 = 11;
                                            }
                                            animAttr2 = i16;
                                            break;
                                        case 10:
                                            if (enter) {
                                                i8 = 12;
                                            } else {
                                                i8 = 13;
                                            }
                                            animAttr2 = i8;
                                            break;
                                        case 11:
                                            if (enter) {
                                                i9 = 14;
                                            } else {
                                                i9 = 15;
                                            }
                                            animAttr2 = i9;
                                            break;
                                        case 12:
                                            if (!enter) {
                                                i17 = 19;
                                            }
                                            animAttr2 = i17;
                                            break;
                                        case 13:
                                            if (enter) {
                                                i10 = 16;
                                            } else {
                                                i10 = 17;
                                            }
                                            animAttr2 = i10;
                                            break;
                                        case 14:
                                            if (enter) {
                                                i11 = 20;
                                            } else {
                                                i11 = 21;
                                            }
                                            animAttr2 = i11;
                                            break;
                                        case 15:
                                            if (enter) {
                                                i12 = 22;
                                            } else {
                                                i12 = 23;
                                            }
                                            animAttr2 = i12;
                                            break;
                                        case 16:
                                            if (enter) {
                                                i13 = 25;
                                            } else {
                                                i13 = 24;
                                            }
                                            animAttr2 = i13;
                                            break;
                                        case 28:
                                            if (enter) {
                                                i14 = 4;
                                            } else {
                                                i14 = 5;
                                            }
                                            animAttr2 = i14;
                                            break;
                                        case 29:
                                            if (enter) {
                                                i15 = 6;
                                            } else {
                                                i15 = 7;
                                            }
                                            animAttr2 = i15;
                                            break;
                                        case 56:
                                            animAttr2 = 0;
                                            break;
                                        case 57:
                                            animAttr2 = ITranAppTransition.Instance().getWallpaperUnlockAnimAttr(0, enter);
                                            break;
                                    }
                                    Animation a3 = animAttr2 != 0 ? appTransition.loadAnimationAttr(lp, animAttr2, transit) : null;
                                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                        String protoLogParam010 = String.valueOf(a3);
                                        long protoLogParam110 = animAttr2;
                                        String protoLogParam26 = String.valueOf(appTransitionOldToString(transit));
                                        String protoLogParam4 = String.valueOf(Debug.getCallers(3));
                                        a2 = a3;
                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 2137411379, 196, (String) null, new Object[]{protoLogParam010, Long.valueOf(protoLogParam110), protoLogParam26, Boolean.valueOf(enter), protoLogParam4});
                                    } else {
                                        a2 = a3;
                                    }
                                    a = a2;
                                }
                                appTransition.mNextAppTransitionScaleUp = i18 == 5 ? z : false;
                                AppTransitionAnimationSpec spec = appTransition.mNextAppTransitionAnimationsSpecs.get(container.hashCode());
                                TransitionAnimation transitionAnimation4 = appTransition.mTransitionAnimation;
                                boolean z2 = appTransition.mNextAppTransitionScaleUp;
                                Rect rect2 = spec != null ? spec.rect : rect;
                                AppTransitionAnimationSpec appTransitionAnimationSpec4 = appTransition.mDefaultNextAppTransitionAnimationSpec;
                                int i20 = i5;
                                boolean z3 = z;
                                ?? r212 = r18;
                                a = transitionAnimation4.createAspectScaledThumbnailEnterExitAnimationLocked(enter, z2, orientation, transit, frame, insets, surfaceInsets, stableInsets, freeform, rect2, appTransitionAnimationSpec4 != null ? appTransitionAnimationSpec4.rect : rect);
                                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                    String protoLogParam011 = String.valueOf(a);
                                    String protoLogParam111 = String.valueOf(appTransition.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_ASPECT_SCALE_UP" : "ANIM_THUMBNAIL_ASPECT_SCALE_DOWN");
                                    String protoLogParam27 = String.valueOf(appTransitionOldToString(transit));
                                    String protoLogParam42 = String.valueOf(Debug.getCallers(i20));
                                    ProtoLogGroup protoLogGroup7 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                    Object[] objArr7 = new Object[5];
                                    objArr7[0] = protoLogParam011;
                                    objArr7[z3 ? 1 : 0] = protoLogParam111;
                                    objArr7[c2] = protoLogParam27;
                                    objArr7[i20] = Boolean.valueOf(enter);
                                    objArr7[r212 == true ? 1 : 0] = protoLogParam42;
                                    ProtoLogImpl.v(protoLogGroup7, -1872288685, 192, (String) null, objArr7);
                                }
                            }
                            appTransition.mNextAppTransitionScaleUp = i18 == i4 ? r19 : false;
                            HardwareBuffer thumbnailHeader = appTransition.getAppTransitionThumbnailHeader(container);
                            TransitionAnimation transitionAnimation5 = appTransition.mTransitionAnimation;
                            boolean z4 = appTransition.mNextAppTransitionScaleUp;
                            AppTransitionAnimationSpec appTransitionAnimationSpec5 = appTransition.mDefaultNextAppTransitionAnimationSpec;
                            a = transitionAnimation5.createThumbnailEnterExitAnimationLockedCompat(enter, z4, frame, transit, thumbnailHeader, (Rect) (appTransitionAnimationSpec5 != null ? appTransitionAnimationSpec5.rect : str));
                            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                                String protoLogParam012 = String.valueOf(a);
                                String protoLogParam112 = String.valueOf(appTransition.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_SCALE_UP" : "ANIM_THUMBNAIL_SCALE_DOWN");
                                String protoLogParam28 = String.valueOf(appTransitionOldToString(transit));
                                String protoLogParam43 = String.valueOf(Debug.getCallers(i4));
                                ProtoLogGroup protoLogGroup8 = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM;
                                Object[] objArr8 = new Object[5];
                                objArr8[0] = protoLogParam012;
                                objArr8[r19] = protoLogParam112;
                                objArr8[c] = protoLogParam28;
                                objArr8[i4] = Boolean.valueOf(enter);
                                objArr8[r21] = protoLogParam43;
                                ProtoLogImpl.v(protoLogGroup8, i2, i3, str, objArr8);
                            }
                        }
                    }
                }
            }
        }
        appTransition.setAppTransitionFinishedCallbackIfNeeded(a);
        return a;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAppRootTaskClipMode() {
        if (this.mNextAppTransitionRequests.contains(5) || this.mNextAppTransitionRequests.contains(7) || this.mNextAppTransitionType == 8) {
            return 1;
        }
        return 0;
    }

    public int getTransitFlags() {
        return this.mNextAppTransitionFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postAnimationCallback() {
        if (this.mNextAppTransitionCallback != null) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.AppTransition$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppTransition.doAnimationCallback((IRemoteCallback) obj);
                }
            }, this.mNextAppTransitionCallback));
            this.mNextAppTransitionCallback = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, int backgroundColor, IRemoteCallback startedCallback, IRemoteCallback endedCallback, boolean overrideTaskTransaction) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionOverrideRequested = true;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionEnter = enterAnim;
            this.mNextAppTransitionExit = exitAnim;
            this.mNextAppTransitionBackgroundColor = backgroundColor;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            this.mAnimationFinishedCallback = endedCallback;
            this.mOverrideTaskTransition = overrideTaskTransaction;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 2;
            putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, null);
            postAnimationCallback();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 8;
            putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, null);
            postAnimationCallback();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionThumb(HardwareBuffer srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 3 : 4;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, 0, 0, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionAspectScaledThumb(HardwareBuffer srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, targetWidth, targetHeight, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback onAnimationStartedCallback, IRemoteCallback onAnimationFinishedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionScaleUp = scaleUp;
            if (specs != null) {
                for (int i = 0; i < specs.length; i++) {
                    AppTransitionAnimationSpec spec = specs[i];
                    if (spec != null) {
                        Predicate<Task> obtainPredicate = PooledLambda.obtainPredicate(new AppTransition$$ExternalSyntheticLambda2(), PooledLambda.__(Task.class), Integer.valueOf(spec.taskId));
                        WindowContainer container = this.mDisplayContent.getTask(obtainPredicate);
                        obtainPredicate.recycle();
                        if (container != null) {
                            this.mNextAppTransitionAnimationsSpecs.put(container.hashCode(), spec);
                            if (i == 0) {
                                Rect rect = spec.rect;
                                putDefaultNextAppTransitionCoordinates(rect.left, rect.top, rect.width(), rect.height(), spec.buffer);
                            }
                        }
                    }
                }
            }
            postAnimationCallback();
            this.mNextAppTransitionCallback = onAnimationStartedCallback;
            this.mAnimationFinishedCallback = onAnimationFinishedCallback;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionAnimationsSpecsFuture = specsFuture;
            this.mNextAppTransitionScaleUp = scaleUp;
            this.mNextAppTransitionFutureCallback = callback;
            if (isReady()) {
                fetchAppTransitionSpecsFromFuture();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) {
        overridePendingAppTransitionRemote(remoteAnimationAdapter, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter, boolean sync) {
        overridePendingAppTransitionRemoteInner(remoteAnimationAdapter, sync, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionRemoteInner(RemoteAnimationAdapter remoteAnimationAdapter, boolean sync, RemoteAnimationAdapter tbAdapater) {
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            boolean protoLogParam0 = isTransitionSet();
            String protoLogParam1 = String.valueOf(remoteAnimationAdapter);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1448683958, 3, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), protoLogParam1});
        }
        boolean protoLogParam02 = isTransitionSet();
        if (protoLogParam02) {
            if (!this.mNextAppTransitionIsSync || (isTransitOldThunder() && getKeyguardTransition() != 0)) {
                clear();
                this.mNextAppTransitionType = 10;
                RemoteAnimationController remoteAnimationController = new RemoteAnimationController(this.mService, this.mDisplayContent, remoteAnimationAdapter, this.mHandler);
                this.mRemoteAnimationController = remoteAnimationController;
                this.mNextAppTransitionIsSync = sync;
                if (remoteAnimationAdapter == tbAdapater && tbAdapater != null) {
                    this.mThunderbackRemoteAnimationController = remoteAnimationController;
                    remoteAnimationController.setIsForThunderback();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isThunderbackRemoteAnimationRunning(RemoteAnimationAdapter adapter) {
        RemoteAnimationController remoteAnimationController = this.mThunderbackRemoteAnimationController;
        if (remoteAnimationController != null) {
            return remoteAnimationController.isRunning(adapter);
        }
        return false;
    }

    void overrideInPlaceAppTransition(String packageName, int anim) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 7;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionInPlace = anim;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePendingAppTransitionStartCrossProfileApps() {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 9;
            postAnimationCallback();
        }
    }

    private boolean canOverridePendingAppTransition() {
        return isTransitionSet() && this.mNextAppTransitionType != 10;
    }

    private void fetchAppTransitionSpecsFromFuture() {
        if (this.mNextAppTransitionAnimationsSpecsFuture != null) {
            this.mNextAppTransitionAnimationsSpecsPending = true;
            final IAppTransitionAnimationSpecsFuture future = this.mNextAppTransitionAnimationsSpecsFuture;
            this.mNextAppTransitionAnimationsSpecsFuture = null;
            this.mDefaultExecutor.execute(new Runnable() { // from class: com.android.server.wm.AppTransition$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AppTransition.this.m7854x8ad64ad4(future);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$fetchAppTransitionSpecsFromFuture$1$com-android-server-wm-AppTransition  reason: not valid java name */
    public /* synthetic */ void m7854x8ad64ad4(IAppTransitionAnimationSpecsFuture future) {
        AppTransitionAnimationSpec[] specs = null;
        try {
            Binder.allowBlocking(future.asBinder());
            specs = future.get();
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Failed to fetch app transition specs: " + e);
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mNextAppTransitionAnimationsSpecsPending = false;
                overridePendingAppTransitionMultiThumb(specs, this.mNextAppTransitionFutureCallback, null, this.mNextAppTransitionScaleUp);
                this.mNextAppTransitionFutureCallback = null;
                this.mService.requestTraversal();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mNextAppTransitionRequests=[");
        boolean separator = false;
        Iterator<Integer> it = this.mNextAppTransitionRequests.iterator();
        while (it.hasNext()) {
            Integer transit = it.next();
            if (separator) {
                sb.append(", ");
            }
            sb.append(appTransitionToString(transit.intValue()));
            separator = true;
        }
        sb.append("]");
        sb.append(", mNextAppTransitionFlags=" + appTransitionFlagsToString(this.mNextAppTransitionFlags) + ", mSkipAppTransitionAnim=" + this.mSkipAppTransitionAnim);
        return sb.toString();
    }

    public static String appTransitionOldToString(int transition) {
        switch (transition) {
            case -1:
                return "TRANSIT_OLD_UNSET";
            case 0:
                return "TRANSIT_OLD_NONE";
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 17:
            case 19:
            case 27:
            default:
                return "<UNKNOWN: " + transition + ">";
            case 6:
                return "TRANSIT_OLD_ACTIVITY_OPEN";
            case 7:
                return "TRANSIT_OLD_ACTIVITY_CLOSE";
            case 8:
                return "TRANSIT_OLD_TASK_OPEN";
            case 9:
                return "TRANSIT_OLD_TASK_CLOSE";
            case 10:
                return "TRANSIT_OLD_TASK_TO_FRONT";
            case 11:
                return "TRANSIT_OLD_TASK_TO_BACK";
            case 12:
                return "TRANSIT_OLD_WALLPAPER_CLOSE";
            case 13:
                return "TRANSIT_OLD_WALLPAPER_OPEN";
            case 14:
                return "TRANSIT_OLD_WALLPAPER_INTRA_OPEN";
            case 15:
                return "TRANSIT_OLD_WALLPAPER_INTRA_CLOSE";
            case 16:
                return "TRANSIT_OLD_TASK_OPEN_BEHIND";
            case 18:
                return "TRANSIT_OLD_ACTIVITY_RELAUNCH";
            case 20:
                return "TRANSIT_OLD_KEYGUARD_GOING_AWAY";
            case 21:
                return "TRANSIT_OLD_KEYGUARD_GOING_AWAY_ON_WALLPAPER";
            case 22:
                return "TRANSIT_OLD_KEYGUARD_OCCLUDE";
            case 23:
                return "TRANSIT_OLD_KEYGUARD_UNOCCLUDE";
            case 24:
                return "TRANSIT_OLD_TRANSLUCENT_ACTIVITY_OPEN";
            case 25:
                return "TRANSIT_OLD_TRANSLUCENT_ACTIVITY_CLOSE";
            case 26:
                return "TRANSIT_OLD_CRASHING_ACTIVITY_CLOSE";
            case 28:
                return "TRANSIT_OLD_TASK_FRAGMENT_OPEN";
            case 29:
                return "TRANSIT_OLD_TASK_FRAGMENT_CLOSE";
            case 30:
                return "TRANSIT_OLD_TASK_FRAGMENT_CHANGE";
        }
    }

    public static String appTransitionToString(int transition) {
        switch (transition) {
            case 0:
                return "TRANSIT_NONE";
            case 1:
                return "TRANSIT_OPEN";
            case 2:
                return "TRANSIT_CLOSE";
            case 3:
                return "TRANSIT_TO_FRONT";
            case 4:
                return "TRANSIT_TO_BACK";
            case 5:
                return "TRANSIT_RELAUNCH";
            case 6:
                return "TRANSIT_CHANGE";
            case 7:
                return "TRANSIT_KEYGUARD_GOING_AWAY";
            case 8:
                return "TRANSIT_KEYGUARD_OCCLUDE";
            case 9:
                return "TRANSIT_KEYGUARD_UNOCCLUDE";
            case 40:
                return "TRANSIT_FINGERPIRNT_UNLOCK_KEYGUARD";
            case 56:
                return "TRANSIT_DREAM_ANIMATION_UNLOCK";
            case 57:
                return "TRANSIT_DREAM_ANIMATION_WALLPAPER_UNLOCK";
            default:
                return "<UNKNOWN: " + transition + ">";
        }
    }

    private String appStateToString() {
        switch (this.mAppTransitionState) {
            case 0:
                return "APP_STATE_IDLE";
            case 1:
                return "APP_STATE_READY";
            case 2:
                return "APP_STATE_RUNNING";
            case 3:
                return "APP_STATE_TIMEOUT";
            default:
                return "unknown state=" + this.mAppTransitionState;
        }
    }

    private String transitTypeToString() {
        switch (this.mNextAppTransitionType) {
            case 0:
                return "NEXT_TRANSIT_TYPE_NONE";
            case 1:
                return "NEXT_TRANSIT_TYPE_CUSTOM";
            case 2:
                return "NEXT_TRANSIT_TYPE_SCALE_UP";
            case 3:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP";
            case 4:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN";
            case 5:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP";
            case 6:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN";
            case 7:
                return "NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE";
            case 8:
            default:
                return "unknown type=" + this.mNextAppTransitionType;
            case 9:
                return "NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS";
        }
    }

    static {
        ArrayList<Pair<Integer, String>> arrayList = new ArrayList<>();
        sFlagToString = arrayList;
        arrayList.add(new Pair<>(1, "TRANSIT_FLAG_KEYGUARD_GOING_AWAY_TO_SHADE"));
        arrayList.add(new Pair<>(2, "TRANSIT_FLAG_KEYGUARD_GOING_AWAY_NO_ANIMATION"));
        arrayList.add(new Pair<>(4, "TRANSIT_FLAG_KEYGUARD_GOING_AWAY_WITH_WALLPAPER"));
        arrayList.add(new Pair<>(8, "TRANSIT_FLAG_KEYGUARD_GOING_AWAY_SUBTLE_ANIMATION"));
        arrayList.add(new Pair<>(22, "TRANSIT_FLAG_KEYGUARD_GOING_AWAY_TO_LAUNCHER_WITH_IN_WINDOW_ANIMATIONS"));
        arrayList.add(new Pair<>(16, "TRANSIT_FLAG_APP_CRASHED"));
        arrayList.add(new Pair<>(32, "TRANSIT_FLAG_OPEN_BEHIND"));
    }

    public static String appTransitionFlagsToString(int flags) {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        Iterator<Pair<Integer, String>> it = sFlagToString.iterator();
        while (it.hasNext()) {
            Pair<Integer, String> pair = it.next();
            if ((((Integer) pair.first).intValue() & flags) != 0) {
                sb.append(sep);
                sb.append((String) pair.second);
                sep = " | ";
            }
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1159641169921L, this.mAppTransitionState);
        proto.write(CompanionMessage.TYPE, this.mLastUsedAppTransition);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(this);
        pw.print(prefix);
        pw.print("mAppTransitionState=");
        pw.println(appStateToString());
        if (this.mNextAppTransitionType != 0) {
            pw.print(prefix);
            pw.print("mNextAppTransitionType=");
            pw.println(transitTypeToString());
        }
        if (this.mNextAppTransitionOverrideRequested || this.mNextAppTransitionType == 1) {
            pw.print(prefix);
            pw.print("mNextAppTransitionPackage=");
            pw.println(this.mNextAppTransitionPackage);
            pw.print(prefix);
            pw.print("mNextAppTransitionEnter=0x");
            pw.print(Integer.toHexString(this.mNextAppTransitionEnter));
            pw.print(" mNextAppTransitionExit=0x");
            pw.println(Integer.toHexString(this.mNextAppTransitionExit));
            pw.print(" mNextAppTransitionBackgroundColor=0x");
            pw.println(Integer.toHexString(this.mNextAppTransitionBackgroundColor));
        }
        switch (this.mNextAppTransitionType) {
            case 2:
                getDefaultNextAppTransitionStartRect(this.mTmpRect);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartX=");
                pw.print(this.mTmpRect.left);
                pw.print(" mNextAppTransitionStartY=");
                pw.println(this.mTmpRect.top);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartWidth=");
                pw.print(this.mTmpRect.width());
                pw.print(" mNextAppTransitionStartHeight=");
                pw.println(this.mTmpRect.height());
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                pw.print(prefix);
                pw.print("mDefaultNextAppTransitionAnimationSpec=");
                pw.println(this.mDefaultNextAppTransitionAnimationSpec);
                pw.print(prefix);
                pw.print("mNextAppTransitionAnimationsSpecs=");
                pw.println(this.mNextAppTransitionAnimationsSpecs);
                pw.print(prefix);
                pw.print("mNextAppTransitionScaleUp=");
                pw.println(this.mNextAppTransitionScaleUp);
                break;
            case 7:
                pw.print(prefix);
                pw.print("mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print(prefix);
                pw.print("mNextAppTransitionInPlace=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionInPlace));
                break;
        }
        if (this.mNextAppTransitionCallback != null) {
            pw.print(prefix);
            pw.print("mNextAppTransitionCallback=");
            pw.println(this.mNextAppTransitionCallback);
        }
        if (this.mLastUsedAppTransition != 0) {
            pw.print(prefix);
            pw.print("mLastUsedAppTransition=");
            pw.println(appTransitionOldToString(this.mLastUsedAppTransition));
            pw.print(prefix);
            pw.print("mLastOpeningApp=");
            pw.println(this.mLastOpeningApp);
            pw.print(prefix);
            pw.print("mLastClosingApp=");
            pw.println(this.mLastClosingApp);
            pw.print(prefix);
            pw.print("mLastChangingApp=");
            pw.println(this.mLastChangingApp);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean prepareAppTransition(int transit, int flags) {
        if (this.mDisplayContent.mTransitionController.isShellTransitionsEnabled()) {
            return false;
        }
        this.mNextAppTransitionRequests.add(Integer.valueOf(transit));
        this.mNextAppTransitionFlags |= flags;
        updateBooster();
        removeAppTransitionTimeoutCallbacks();
        this.mHandler.postDelayed(this.mHandleAppTransitionTimeoutRunnable, APP_TRANSITION_TIMEOUT_MS);
        return prepare();
    }

    public static boolean isKeyguardGoingAwayTransitOld(int transit) {
        return transit == 20 || transit == 21;
    }

    public static boolean isFingerprintUnlockGoingAwayTransit(int transit) {
        return transit == 40;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFingerprintUnlockTransition() {
        for (int i = 0; i < this.mNextAppTransitionRequests.size(); i++) {
            int transit = this.mNextAppTransitionRequests.get(i).intValue();
            if (isFingerprintUnlockGoingAwayTransit(transit)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isKeyguardOccludeTransitOld(int transit) {
        return transit == 22 || transit == 23;
    }

    static boolean isKeyguardTransitOld(int transit) {
        return isKeyguardGoingAwayTransitOld(transit) || isKeyguardOccludeTransitOld(transit);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTaskTransitOld(int transit) {
        return isTaskOpenTransitOld(transit) || isTaskCloseTransitOld(transit);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTaskCloseTransitOld(int transit) {
        return transit == 9 || transit == 11;
    }

    private static boolean isTaskOpenTransitOld(int transit) {
        return transit == 8 || transit == 16 || transit == 10;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isActivityTransitOld(int transit) {
        return transit == 6 || transit == 7 || transit == 18;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isChangeTransitOld(int transit) {
        return transit == 27 || transit == 30;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isClosingTransitOld(int transit) {
        return transit == 7 || transit == 9 || transit == 12 || transit == 15 || transit == 25 || transit == 26;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isNormalTransit(int transit) {
        return transit == 1 || transit == 2 || transit == 3 || transit == 4;
    }

    static boolean isKeyguardTransit(int transit) {
        return transit == 7 || transit == 8 || transit == 40 || transit == 9;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyguardTransition() {
        if (this.mNextAppTransitionRequests.indexOf(7) != -1) {
            return 7;
        }
        if (this.mNextAppTransitionRequests.indexOf(40) != -1) {
            return 40;
        }
        int unoccludeIndex = this.mNextAppTransitionRequests.lastIndexOf(9);
        int occludeIndex = this.mNextAppTransitionRequests.lastIndexOf(8);
        if (unoccludeIndex == -1 && occludeIndex == -1) {
            return 0;
        }
        return (unoccludeIndex == -1 || occludeIndex == -1) ? unoccludeIndex != -1 ? 9 : 8 : unoccludeIndex > occludeIndex ? 9 : 8;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFirstAppTransition() {
        for (int i = 0; i < this.mNextAppTransitionRequests.size(); i++) {
            int transit = this.mNextAppTransitionRequests.get(i).intValue();
            if (transit != 0 && !isKeyguardTransit(transit)) {
                return transit;
            }
        }
        return 0;
    }

    public boolean containsTransitRequest(int transit) {
        return this.mNextAppTransitionRequests.contains(Integer.valueOf(transit));
    }

    private boolean shouldScaleDownThumbnailTransition(int uiMode, int orientation) {
        return this.mGridLayoutRecentsEnabled || orientation == 1;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleAppTransitionTimeout */
    public void m7855lambda$new$0$comandroidserverwmAppTransition() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mDisplayContent;
                if (dc == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                notifyAppTransitionTimeoutLocked();
                if (isTransitionSet() || !dc.mOpeningApps.isEmpty() || !dc.mClosingApps.isEmpty() || !dc.mChangingContainers.isEmpty()) {
                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                        long protoLogParam0 = dc.getDisplayId();
                        boolean protoLogParam1 = dc.mAppTransition.isTransitionSet();
                        long protoLogParam2 = dc.mOpeningApps.size();
                        long protoLogParam3 = dc.mClosingApps.size();
                        long protoLogParam4 = dc.mChangingContainers.size();
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 344795667, 349, (String) null, new Object[]{Long.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), Long.valueOf(protoLogParam3), Long.valueOf(protoLogParam4)});
                    }
                    setTimeout();
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void doAnimationCallback(IRemoteCallback callback) {
        try {
            callback.sendResult((Bundle) null);
        } catch (RemoteException e) {
        }
    }

    private void setAppTransitionFinishedCallbackIfNeeded(Animation anim) {
        IRemoteCallback callback = this.mAnimationFinishedCallback;
        if (callback != null && anim != null) {
            anim.setAnimationListener(new AnonymousClass1(callback));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.AppTransition$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements Animation.AnimationListener {
        final /* synthetic */ IRemoteCallback val$callback;

        AnonymousClass1(IRemoteCallback iRemoteCallback) {
            this.val$callback = iRemoteCallback;
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            AppTransition.this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.AppTransition$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppTransition.doAnimationCallback((IRemoteCallback) obj);
                }
            }, this.val$callback));
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAppTransitionTimeoutCallbacks() {
        this.mHandler.removeCallbacks(this.mHandleAppTransitionTimeoutRunnable);
    }

    public ArrayList<Integer> getNextAppTransitionRequests() {
        return this.mNextAppTransitionRequests;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSkipAppTransitionAnim(boolean skip) {
        this.mSkipAppTransitionAnim = skip;
    }

    public boolean getSkipAppTransitionAnim() {
        return this.mSkipAppTransitionAnim;
    }
}
