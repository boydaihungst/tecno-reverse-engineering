package com.android.server.wm;

import android.app.AppOpsManager;
import android.app.ThunderbackConfig;
import android.app.admin.DevicePolicyCache;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.GraphicsProtos;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.audio.common.V2_0.AudioChannelMask;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.IWindow;
import android.view.IWindowFocusObserver;
import android.view.IWindowId;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputWindowHandle;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewRootImpl;
import android.view.WindowInfo;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.window.ClientWindowFrames;
import android.window.OnBackInvokedCallbackInfo;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.am.ActivityManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.LocalAnimationAdapter;
import com.transsion.hubcore.griffin.lib.app.TranWindowInfo;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.server.wm.ITranWindowState;
import dalvik.annotation.optimization.NeverCompile;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class WindowState extends WindowContainer<WindowState> implements WindowManagerPolicy.WindowState, InsetsControlTarget, InputTarget {
    static final int BLAST_TIMEOUT_DURATION = 5000;
    private static final float DEFAULT_DIM_AMOUNT_DEAD_WINDOW = 0.5f;
    private static final int DREAM_HIDE = 2;
    private static final int DREAM_UNSET = 1;
    static final int EXCLUSION_LEFT = 0;
    static final int EXCLUSION_RIGHT = 1;
    static final int EXIT_ANIMATING_TYPES = 25;
    static final int LEGACY_POLICY_VISIBILITY = 1;
    static final int MINIMUM_VISIBLE_HEIGHT_IN_DP = 32;
    static final int MINIMUM_VISIBLE_WIDTH_IN_DP = 48;
    private static final String MSYNCTAG = "MSyncForWMS";
    private static final int POLICY_VISIBILITY_ALL = 3;
    static final int RESIZE_HANDLE_WIDTH_IN_DP = 30;
    static final String TAG = "WindowManager";
    private static final int VISIBLE_FOR_USER = 2;
    boolean hasHiddedPreWakeupQ;
    final InsetsState mAboveInsetsState;
    ActivityRecord mActivityRecord;
    private boolean mAnimateReplacingWindow;
    boolean mAnimatingExit;
    boolean mAppDied;
    boolean mAppFreezing;
    final int mAppOp;
    private boolean mAppOpVisibility;
    float mAppPreferredFrameRate;
    final WindowManager.LayoutParams mAttrs;
    final int mBaseLayer;
    private boolean mCaptionViewShow;
    final IWindow mClient;
    boolean mClientWasDrawingForSync;
    private final ClientWindowFrames mClientWindowFrames;
    final Context mContext;
    private DeadWindowEventReceiver mDeadWindowEventReceiver;
    final DeathRecipient mDeathRecipient;
    boolean mDestroying;
    int mDisableFlags;
    private boolean mDragResizing;
    private boolean mDragResizingChangeReported;
    private final List<DrawHandler> mDrawHandlers;
    private PowerManager.WakeLock mDrawLock;
    private boolean mDrawnStateEvaluated;
    private final List<Rect> mExclusionRects;
    private RemoteCallbackList<IWindowFocusObserver> mFocusCallbacks;
    private boolean mForceHideNonSystemOverlayWindow;
    final boolean mForceSeamlesslyRotate;
    int mFrameRateSelectionPriority;
    private InsetsState mFrozenInsetsState;
    final Rect mGivenContentInsets;
    boolean mGivenInsetsPending;
    final Region mGivenTouchableRegion;
    final Rect mGivenVisibleInsets;
    float mGlobalScale;
    float mHScale;
    public float mHWScale;
    boolean mHasSurface;
    boolean mHaveFrame;
    boolean mHidden;
    private boolean mHiddenWhileSuspended;
    private int mHideByDreamAnimationState;
    boolean mInRelayout;
    InputChannel mInputChannel;
    IBinder mInputChannelToken;
    final InputWindowHandleWrapper mInputWindowHandle;
    float mInvGlobalScale;
    private boolean mIsChildWindow;
    private boolean mIsDimming;
    boolean mIsDreamAnimationWindow;
    private final boolean mIsFloatingLayer;
    final boolean mIsImWindow;
    final boolean mIsWallpaper;
    private final List<Rect> mKeepClearAreas;
    private KeyInterceptionInfo mKeyInterceptionInfo;
    private boolean mLastConfigReportedToClient;
    private final long[] mLastExclusionLogUptimeMillis;
    int mLastFreezeDuration;
    private final int[] mLastGrantedExclusionHeight;
    float mLastHScale;
    int mLastMultiWindowMode;
    private final MergedConfiguration mLastReportedConfiguration;
    private final int[] mLastRequestedExclusionHeight;
    private int mLastRequestedHeight;
    private int mLastRequestedWidth;
    int mLastSeqIdSentToRelayout;
    private boolean mLastShownChangedReported;
    final Rect mLastSurfaceInsets;
    private CharSequence mLastTitle;
    float mLastVScale;
    int mLastVisibleLayoutRotation;
    int mLayer;
    final boolean mLayoutAttached;
    boolean mLayoutNeeded;
    int mLayoutSeq;
    boolean mLegacyPolicyVisibilityAfterAnim;
    public float mMaxFPS;
    SparseArray<InsetsSource> mMergedLocalInsetsSources;
    private boolean mMovedByResize;
    public boolean mNeedHWResizer;
    boolean mObscured;
    private OnBackInvokedCallbackInfo mOnBackInvokedCallbackInfo;
    private long mOrientationChangeRedrawRequestTime;
    private boolean mOrientationChangeTimedOut;
    private boolean mOrientationChanging;
    final float mOverrideScale;
    final boolean mOwnerCanAddInternalSystemWindow;
    final int mOwnerUid;
    SeamlessRotator mPendingSeamlessRotate;
    private boolean mPerformShowLog;
    boolean mPermanentlyHidden;
    final WindowManagerPolicy mPolicy;
    public int mPolicyVisibility;
    private PowerManagerWrapper mPowerManagerWrapper;
    private boolean mRedrawForSyncReported;
    boolean mRelayoutCalled;
    boolean mRemoveOnExit;
    boolean mRemoved;
    private WindowState mReplacementWindow;
    private boolean mReplacingRemoveRequested;
    boolean mReportOrientationChanged;
    int mRequestedHeight;
    private final InsetsVisibilities mRequestedVisibilities;
    int mRequestedWidth;
    private int mResizeMode;
    boolean mResizedWhileGone;
    private final Consumer<SurfaceControl.Transaction> mSeamlessRotationFinishedConsumer;
    boolean mSeamlesslyRotated;
    final Session mSession;
    private final Consumer<SurfaceControl.Transaction> mSetSurfacePositionConsumer;
    boolean mShouldScaleWallpaper;
    final int mShowUserId;
    boolean mSkipEnterAnimationForSeamlessReplacement;
    StartingData mStartingData;
    private String mStringNameCache;
    final int mSubLayer;
    boolean mSurfacePlacementNeeded;
    private final Point mSurfacePosition;
    private int mSurfaceTranslationY;
    int mSyncSeqId;
    private final Region mTapExcludeRegion;
    private final Configuration mTempConfiguration;
    final Matrix mTmpMatrix;
    final float[] mTmpMatrixArray;
    private final Point mTmpPoint;
    private final Rect mTmpRect;
    private final Region mTmpRegion;
    private final SurfaceControl.Transaction mTmpTransaction;
    WindowToken mToken;
    int mTouchableInsets;
    private final List<Rect> mUnrestrictedKeepClearAreas;
    float mVScale;
    int mViewVisibility;
    int mWallpaperDisplayOffsetX;
    int mWallpaperDisplayOffsetY;
    float mWallpaperScale;
    float mWallpaperX;
    float mWallpaperXStep;
    float mWallpaperY;
    float mWallpaperYStep;
    float mWallpaperZoomOut;
    private boolean mWasExiting;
    private SurfaceControl mWeltLeash;
    boolean mWillReplaceWindow;
    final WindowStateAnimator mWinAnimator;
    private TranWindowInfo mWinProxy;
    private final WindowFrames mWindowFrames;
    final WindowId mWindowId;
    boolean mWindowRemovalAllowed;
    private final WindowProcessController mWpcForDisplayAreaConfigChanges;
    int mXOffset;
    int mYOffset;
    private static final boolean ENABLE_DOUBLE_WALLPAPER_SUPPROT = "1".equals(SystemProperties.get("ro.os.default_local_wallpaper_support"));
    static final boolean DEBUG_ADB = "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));
    private static final StringBuilder sTmpSB = new StringBuilder();
    private static final Comparator<WindowState> sWindowSubLayerComparator = new Comparator<WindowState>() { // from class: com.android.server.wm.WindowState.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(WindowState w1, WindowState w2) {
            int layer1 = w1.mSubLayer;
            int layer2 = w2.mSubLayer;
            if (layer1 >= layer2) {
                if (layer1 == layer2 && layer2 < 0) {
                    return -1;
                }
                return 1;
            }
            return -1;
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface PowerManagerWrapper {
        boolean isInteractive();

        void wakeUp(long j, int i, String str);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeash() {
        return super.getAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ SurfaceControl getFreezeSnapshotTarget() {
        return super.getFreezeSnapshotTarget();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getParentSurfaceControl() {
        return super.getParentSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getPendingTransaction() {
        return super.getPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ SparseArray getProvidedInsetsSources() {
        return super.getProvidedInsetsSources();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getSurfaceControl() {
        return super.getSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceHeight() {
        return super.getSurfaceHeight();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceWidth() {
        return super.getSurfaceWidth();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getSyncTransaction() {
        return super.getSyncTransaction();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ boolean isOSFullDialog() {
        return super.isOSFullDialog();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void onAnimationLeashLost(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashLost(transaction);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ void onRequestedOverrideConfigurationChanged(Configuration configuration) {
        super.onRequestedOverrideConfigurationChanged(configuration);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ void onUnfrozen() {
        super.onUnfrozen();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class DrawHandler {
        Consumer<SurfaceControl.Transaction> mConsumer;
        int mSeqId;

        DrawHandler(int seqId, Consumer<SurfaceControl.Transaction> consumer) {
            this.mSeqId = seqId;
            this.mConsumer = consumer;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WindowState  reason: not valid java name */
    public /* synthetic */ void m8537lambda$new$0$comandroidserverwmWindowState(SurfaceControl.Transaction t) {
        finishSeamlessRotation(t);
        updateSurfacePosition(t);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-WindowState  reason: not valid java name */
    public /* synthetic */ void m8538lambda$new$1$comandroidserverwmWindowState(SurfaceControl.Transaction t) {
        if (this.mSurfaceControl != null && this.mSurfaceControl.isValid()) {
            t.setPosition(this.mSurfaceControl, this.mSurfacePosition.x, this.mSurfacePosition.y);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    WindowState asWindowState() {
        return this;
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public boolean getRequestedVisibility(int type) {
        return this.mRequestedVisibilities.getVisibility(type);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsVisibilities getRequestedVisibilities() {
        return this.mRequestedVisibilities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequestedVisibilities(InsetsVisibilities visibilities) {
        this.mRequestedVisibilities.set(visibilities);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void freezeInsetsState() {
        if (this.mFrozenInsetsState == null) {
            this.mFrozenInsetsState = new InsetsState(getInsetsState(), true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearFrozenInsetsState() {
        this.mFrozenInsetsState = null;
    }

    InsetsState getFrozenInsetsState() {
        return this.mFrozenInsetsState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReadyToDispatchInsetsState() {
        boolean visible = shouldCheckTokenVisibleRequested() ? isVisibleRequested() : isVisible();
        return visible && this.mFrozenInsetsState == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void seamlesslyRotateIfAllowed(SurfaceControl.Transaction transaction, int oldRotation, int rotation, boolean requested) {
        if (!isVisibleNow() || this.mIsWallpaper || ITranWindowState.Instance().isThunderbackWindow(getConfiguration()) || this.mToken.hasFixedRotationTransform()) {
            return;
        }
        Task task = getTask();
        if (task != null && task.inPinnedWindowingMode()) {
            return;
        }
        SeamlessRotator seamlessRotator = this.mPendingSeamlessRotate;
        if (seamlessRotator != null) {
            oldRotation = seamlessRotator.getOldRotation();
        }
        if (this.mControllableInsetProvider != null && this.mControllableInsetProvider.getSource().getType() == 19) {
            return;
        }
        if (this.mForceSeamlesslyRotate || requested) {
            if (this.mControllableInsetProvider != null) {
                this.mControllableInsetProvider.startSeamlessRotation();
            }
            this.mPendingSeamlessRotate = new SeamlessRotator(oldRotation, rotation, getDisplayInfo(), false);
            this.mLastSurfacePosition.set(this.mSurfacePosition.x, this.mSurfacePosition.y);
            this.mPendingSeamlessRotate.unrotate(transaction, this);
            getDisplayContent().getDisplayRotation().markForSeamlessRotation(this, true);
            applyWithNextDraw(this.mSeamlessRotationFinishedConsumer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelSeamlessRotation() {
        finishSeamlessRotation(getPendingTransaction());
    }

    void finishSeamlessRotation(SurfaceControl.Transaction t) {
        SeamlessRotator seamlessRotator = this.mPendingSeamlessRotate;
        if (seamlessRotator == null) {
            return;
        }
        seamlessRotator.finish(t, this);
        this.mPendingSeamlessRotate = null;
        getDisplayContent().getDisplayRotation().markForSeamlessRotation(this, false);
        if (this.mControllableInsetProvider != null) {
            this.mControllableInsetProvider.finishSeamlessRotation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Rect> getSystemGestureExclusion() {
        return this.mExclusionRects;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setSystemGestureExclusion(List<Rect> exclusionRects) {
        if (this.mExclusionRects.equals(exclusionRects)) {
            return false;
        }
        this.mExclusionRects.clear();
        this.mExclusionRects.addAll(exclusionRects);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isImplicitlyExcludingAllSystemGestures() {
        ActivityRecord activityRecord;
        boolean stickyHideNav = this.mAttrs.insetsFlags.behavior == 2 && !getRequestedVisibility(1);
        return stickyHideNav && this.mWmService.mConstants.mSystemGestureExcludedByPreQStickyImmersive && (activityRecord = this.mActivityRecord) != null && activityRecord.mTargetSdk < 29;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastExclusionHeights(int side, int requested, int granted) {
        boolean changed = (this.mLastGrantedExclusionHeight[side] == granted && this.mLastRequestedExclusionHeight[side] == requested) ? false : true;
        if (changed) {
            if (this.mLastShownChangedReported) {
                logExclusionRestrictions(side);
            }
            this.mLastGrantedExclusionHeight[side] = granted;
            this.mLastRequestedExclusionHeight[side] = requested;
        }
    }

    void getKeepClearAreas(Collection<Rect> outRestricted, Collection<Rect> outUnrestricted) {
        Matrix tmpMatrix = new Matrix();
        float[] tmpFloat9 = new float[9];
        getKeepClearAreas(outRestricted, outUnrestricted, tmpMatrix, tmpFloat9);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getKeepClearAreas(Collection<Rect> outRestricted, Collection<Rect> outUnrestricted, Matrix tmpMatrix, float[] float9) {
        outRestricted.addAll(getRectsInScreenSpace(this.mKeepClearAreas, tmpMatrix, float9));
        outUnrestricted.addAll(getRectsInScreenSpace(this.mUnrestrictedKeepClearAreas, tmpMatrix, float9));
    }

    List<Rect> getRectsInScreenSpace(List<Rect> rects, Matrix tmpMatrix, float[] float9) {
        getTransformationMatrix(float9, tmpMatrix);
        List<Rect> transformedRects = new ArrayList<>();
        RectF tmpRect = new RectF();
        for (Rect r : rects) {
            tmpRect.set(r);
            tmpMatrix.mapRect(tmpRect);
            Rect curr = new Rect();
            tmpRect.roundOut(curr);
            transformedRects.add(curr);
        }
        return transformedRects;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setKeepClearAreas(List<Rect> restricted, List<Rect> unrestricted) {
        boolean newRestrictedAreas = !this.mKeepClearAreas.equals(restricted);
        boolean newUnrestrictedAreas = !this.mUnrestrictedKeepClearAreas.equals(unrestricted);
        if (!newRestrictedAreas && !newUnrestrictedAreas) {
            return false;
        }
        if (newRestrictedAreas) {
            this.mKeepClearAreas.clear();
            this.mKeepClearAreas.addAll(restricted);
        }
        if (newUnrestrictedAreas) {
            this.mUnrestrictedKeepClearAreas.clear();
            this.mUnrestrictedKeepClearAreas.addAll(unrestricted);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOnBackInvokedCallbackInfo(OnBackInvokedCallbackInfo callbackInfo) {
        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(callbackInfo);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -228813488, 0, "%s: Setting back callback %s", new Object[]{protoLogParam0, protoLogParam1});
        }
        this.mOnBackInvokedCallbackInfo = callbackInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OnBackInvokedCallbackInfo getOnBackInvokedCallbackInfo() {
        return this.mOnBackInvokedCallbackInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState(final WindowManagerService service, Session s, IWindow c, WindowToken token, WindowState parentWindow, int appOp, WindowManager.LayoutParams a, int viewVisibility, int ownerId, int showUserId, boolean ownerCanAddInternalSystemWindow) {
        this(service, s, c, token, parentWindow, appOp, a, viewVisibility, ownerId, showUserId, ownerCanAddInternalSystemWindow, new PowerManagerWrapper() { // from class: com.android.server.wm.WindowState.2
            @Override // com.android.server.wm.WindowState.PowerManagerWrapper
            public void wakeUp(long time, int reason, String details) {
                WindowManagerService.this.mPowerManager.wakeUp(time, reason, details);
            }

            @Override // com.android.server.wm.WindowState.PowerManagerWrapper
            public boolean isInteractive() {
                return WindowManagerService.this.mPowerManager.isInteractive();
            }
        });
    }

    WindowState(WindowManagerService service, Session s, IWindow c, WindowToken token, WindowState parentWindow, int appOp, WindowManager.LayoutParams a, int viewVisibility, int ownerId, int showUserId, boolean ownerCanAddInternalSystemWindow, PowerManagerWrapper powerManagerWrapper) {
        super(service);
        WindowProcessController windowProcessController;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        this.mAttrs = layoutParams;
        this.mMaxFPS = 0.0f;
        this.mHideByDreamAnimationState = 1;
        this.mPolicyVisibility = 3;
        this.mLegacyPolicyVisibilityAfterAnim = true;
        this.mAppOpVisibility = true;
        this.mHidden = true;
        this.mDragResizingChangeReported = true;
        this.mSyncSeqId = 0;
        this.mLastSeqIdSentToRelayout = 0;
        this.mClientWasDrawingForSync = false;
        this.mLayoutSeq = -1;
        this.hasHiddedPreWakeupQ = false;
        this.mIsDreamAnimationWindow = false;
        this.mLastReportedConfiguration = new MergedConfiguration();
        this.mTempConfiguration = new Configuration();
        this.mGivenContentInsets = new Rect();
        this.mGivenVisibleInsets = new Rect();
        this.mGivenTouchableRegion = new Region();
        this.mTouchableInsets = 0;
        this.mGlobalScale = 1.0f;
        this.mInvGlobalScale = 1.0f;
        this.mHScale = 1.0f;
        this.mVScale = 1.0f;
        this.mLastHScale = 1.0f;
        this.mLastVScale = 1.0f;
        this.mXOffset = 0;
        this.mYOffset = 0;
        this.mWallpaperScale = 1.0f;
        this.mTmpMatrix = new Matrix();
        this.mTmpMatrixArray = new float[9];
        this.mWindowFrames = new WindowFrames();
        this.mClientWindowFrames = new ClientWindowFrames();
        this.mExclusionRects = new ArrayList();
        this.mKeepClearAreas = new ArrayList();
        this.mUnrestrictedKeepClearAreas = new ArrayList();
        this.mLastRequestedExclusionHeight = new int[]{0, 0};
        this.mLastGrantedExclusionHeight = new int[]{0, 0};
        this.mLastExclusionLogUptimeMillis = new long[]{0, 0};
        this.mWallpaperX = -1.0f;
        this.mWallpaperY = -1.0f;
        this.mWallpaperZoomOut = -1.0f;
        this.mWallpaperXStep = -1.0f;
        this.mWallpaperYStep = -1.0f;
        this.mWallpaperDisplayOffsetX = Integer.MIN_VALUE;
        this.mWallpaperDisplayOffsetY = Integer.MIN_VALUE;
        this.mLastVisibleLayoutRotation = -1;
        this.mHasSurface = false;
        this.mWillReplaceWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacementWindow = null;
        this.mSkipEnterAnimationForSeamlessReplacement = false;
        this.mPerformShowLog = false;
        this.mTmpRect = new Rect();
        this.mTmpPoint = new Point();
        this.mTmpRegion = new Region();
        this.mResizedWhileGone = false;
        this.mSeamlesslyRotated = false;
        this.mAboveInsetsState = new InsetsState();
        this.mMergedLocalInsetsSources = null;
        Rect rect = new Rect();
        this.mLastSurfaceInsets = rect;
        this.mSurfacePosition = new Point();
        this.mNeedHWResizer = false;
        this.mHWScale = this.mGlobalScale;
        this.mTapExcludeRegion = new Region();
        this.mIsDimming = false;
        this.mRequestedVisibilities = new InsetsVisibilities();
        this.mFrameRateSelectionPriority = -1;
        this.mAppPreferredFrameRate = 0.0f;
        this.mDrawHandlers = new ArrayList();
        this.mSeamlessRotationFinishedConsumer = new Consumer() { // from class: com.android.server.wm.WindowState$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowState.this.m8537lambda$new$0$comandroidserverwmWindowState((SurfaceControl.Transaction) obj);
            }
        };
        this.mSetSurfacePositionConsumer = new Consumer() { // from class: com.android.server.wm.WindowState$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowState.this.m8538lambda$new$1$comandroidserverwmWindowState((SurfaceControl.Transaction) obj);
            }
        };
        this.mWeltLeash = null;
        this.mWinProxy = new TranWindowInfo() { // from class: com.android.server.wm.WindowState.3
            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public int owningUid() {
                return WindowState.this.mOwnerUid;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public String owningPackage() {
                return WindowState.this.mAttrs.packageName;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public int baseType() {
                return WindowState.this.getTopParentWindow().mAttrs.type;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public int baseLayer() {
                return WindowState.this.mBaseLayer;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public boolean isImWindow() {
                return WindowState.this.mIsImWindow;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public boolean isWallpaper() {
                return WindowState.this.mIsWallpaper;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public boolean isFloatingLayer() {
                return WindowState.this.mIsFloatingLayer;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public IBinder client() {
                return WindowState.this.mClient.asBinder();
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public DisplayInfo displayInfo() {
                return WindowState.this.getDisplayInfo();
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public int displayId() {
                return WindowState.this.getDisplayId();
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public int viewVisibility() {
                return WindowState.this.mViewVisibility;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public boolean isOnScreen() {
                if (WindowState.this.mHasSurface && !WindowState.this.mDestroying && WindowState.this.isVisibleByPolicy()) {
                    ActivityRecord atoken = WindowState.this.mActivityRecord;
                    return atoken != null ? (!WindowState.this.isParentWindowHidden() && atoken.mVisibleRequested) || WindowState.this.mWinAnimator.mWin.isAnimating() : !WindowState.this.isParentWindowHidden() || WindowState.this.isAnimating(3);
                }
                return false;
            }

            @Override // com.transsion.hubcore.griffin.lib.app.TranWindowInfo
            public boolean isVisible() {
                return WindowState.this.isVisibleLw();
            }
        };
        this.mTmpTransaction = service.mTransactionFactory.get();
        this.mSession = s;
        this.mClient = c;
        this.mAppOp = appOp;
        this.mToken = token;
        this.mActivityRecord = token.asActivityRecord();
        this.mOwnerUid = ownerId;
        this.mShowUserId = showUserId;
        this.mOwnerCanAddInternalSystemWindow = ownerCanAddInternalSystemWindow;
        this.mWindowId = new WindowId();
        layoutParams.copyFrom(a);
        rect.set(layoutParams.surfaceInsets);
        this.mViewVisibility = viewVisibility;
        WindowManagerPolicy windowManagerPolicy = this.mWmService.mPolicy;
        this.mPolicy = windowManagerPolicy;
        this.mContext = this.mWmService.mContext;
        DeathRecipient deathRecipient = new DeathRecipient();
        this.mPowerManagerWrapper = powerManagerWrapper;
        if (!"com.android.systemui".equals(layoutParams.packageName) || !"screen-record".equals(layoutParams.getTitle())) {
            this.mForceSeamlesslyRotate = token.mRoundedCornerOverlay;
        } else {
            Slog.d("WindowManager", "No need force seamless rotate, this is " + this);
            this.mForceSeamlesslyRotate = false;
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        InputWindowHandleWrapper inputWindowHandleWrapper = new InputWindowHandleWrapper(new InputWindowHandle(activityRecord != null ? activityRecord.getInputApplicationHandle(false) : null, getDisplayId()));
        this.mInputWindowHandle = inputWindowHandleWrapper;
        inputWindowHandleWrapper.setFocusable(false);
        inputWindowHandleWrapper.setOwnerPid(s.mPid);
        inputWindowHandleWrapper.setOwnerUid(s.mUid);
        inputWindowHandleWrapper.setName(getName());
        inputWindowHandleWrapper.setPackageName(layoutParams.packageName);
        inputWindowHandleWrapper.setLayoutParamsType(layoutParams.type);
        inputWindowHandleWrapper.setTrustedOverlay(((layoutParams.privateFlags & 536870912) != 0 && ownerCanAddInternalSystemWindow) || InputMonitor.isTrustedOverlay(layoutParams.type));
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "Window " + this + " client=" + c.asBinder() + " token=" + token + " (" + layoutParams.token + ") params=" + a);
        }
        try {
            c.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipient = deathRecipient;
            if (layoutParams.type < 1000 || layoutParams.type > 1999) {
                this.mBaseLayer = (windowManagerPolicy.getWindowLayerLw(this) * 10000) + 1000;
                this.mSubLayer = 0;
                this.mIsChildWindow = false;
                this.mLayoutAttached = false;
                this.mIsImWindow = layoutParams.type == 2011 || layoutParams.type == 2012;
                this.mIsWallpaper = layoutParams.type == 2013;
            } else {
                this.mBaseLayer = (windowManagerPolicy.getWindowLayerLw(parentWindow) * 10000) + 1000;
                this.mSubLayer = windowManagerPolicy.getSubWindowLayerFromTypeLw(a.type);
                this.mIsChildWindow = true;
                this.mLayoutAttached = layoutParams.type != 1003;
                this.mIsImWindow = parentWindow.mAttrs.type == 2011 || parentWindow.mAttrs.type == 2012;
                this.mIsWallpaper = parentWindow.mAttrs.type == 2013;
            }
            this.mIsFloatingLayer = this.mIsImWindow || this.mIsWallpaper;
            ActivityRecord activityRecord2 = this.mActivityRecord;
            if (activityRecord2 != null && activityRecord2.mShowForAllUsers) {
                layoutParams.flags |= 524288;
            }
            WindowStateAnimator windowStateAnimator = new WindowStateAnimator(this);
            this.mWinAnimator = windowStateAnimator;
            windowStateAnimator.mAlpha = a.alpha;
            this.mRequestedWidth = -1;
            this.mRequestedHeight = -1;
            this.mLastRequestedWidth = -1;
            this.mLastRequestedHeight = -1;
            this.mLayer = 0;
            this.mOverrideScale = this.mWmService.mAtmService.mCompatModePackages.getCompatScale(layoutParams.packageName, s.mUid);
            updateGlobalScale();
            if (this.mIsChildWindow) {
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    String protoLogParam1 = String.valueOf(parentWindow);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -916108501, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                parentWindow.addChild(this, sWindowSubLayerComparator);
            }
            if (s.mPid == ActivityManagerService.MY_PID || s.mPid < 0) {
                windowProcessController = null;
            } else {
                windowProcessController = service.mAtmService.getProcessController(s.mPid, s.mUid);
            }
            this.mWpcForDisplayAreaConfigChanges = windowProcessController;
            ITranWindowState.Instance().onConstruct("WindowManager");
            if (toString().contains("windowfordreamanimation")) {
                this.mIsDreamAnimationWindow = true;
            }
        } catch (RemoteException e) {
            this.mDeathRecipient = null;
            this.mIsChildWindow = false;
            this.mLayoutAttached = false;
            this.mIsImWindow = false;
            this.mIsWallpaper = false;
            this.mIsFloatingLayer = false;
            this.mBaseLayer = 0;
            this.mSubLayer = 0;
            this.mWinAnimator = null;
            this.mWpcForDisplayAreaConfigChanges = null;
            this.mOverrideScale = 1.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTouchOcclusionMode() {
        return (WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) || isAnimating(3, -1) || inTransition()) ? 1 : 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attach() {
        ActivityRecord activityRecord;
        if (this.mWmService.isMsyncOn && !this.mIsImWindow && (activityRecord = this.mActivityRecord) != null) {
            String mActivityName = activityRecord.getName();
            this.mMaxFPS = this.mWmService.getWmsExt().getMaxRefreshRate(0, 0, getOwningPackage(), mActivityName);
            if (this.mWmService.isMsyncLogOn) {
                Slog.d("MSyncForWMS", mActivityName + " setMaxFPS: " + this.mAppPreferredFrameRate);
            }
        }
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "Attaching " + this + " token=" + this.mToken);
        }
        this.mSession.windowAddedLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateGlobalScale() {
        float f;
        if (layoutForART()) {
            return true;
        }
        if (this.mRequestedWidth > 65536 || this.mRequestedHeight > 65536) {
            Slog.v("WindowManager", "this  = " + this + " ,and hasCompatScale()" + hasCompatScale() + " ,and the mOverrideScale = " + this.mOverrideScale + " ,and the mToken.getSizeCompatScale() = " + this.mToken.getSizeCompatScale());
        }
        if (hasCompatScale()) {
            if (this.mOverrideScale != 1.0f) {
                if (this.mToken.hasSizeCompatBounds()) {
                    f = this.mToken.getSizeCompatScale() * this.mOverrideScale;
                } else {
                    f = this.mOverrideScale;
                }
                this.mGlobalScale = f;
            } else {
                this.mGlobalScale = this.mToken.getSizeCompatScale();
            }
            this.mInvGlobalScale = 1.0f / this.mGlobalScale;
            return true;
        }
        this.mInvGlobalScale = 1.0f;
        this.mGlobalScale = 1.0f;
        return false;
    }

    boolean hasCompatScale() {
        return hasCompatScale(this.mAttrs, this.mActivityRecord, this.mOverrideScale);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean hasCompatScale(WindowManager.LayoutParams attrs, WindowToken token, float overrideScale) {
        if ((attrs.privateFlags & 128) != 0) {
            return true;
        }
        if (attrs.type == 3) {
            return false;
        }
        return (token != null && token.hasSizeCompatBounds()) || overrideScale != 1.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getDrawnStateEvaluated() {
        return this.mDrawnStateEvaluated;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDrawnStateEvaluated(boolean evaluated) {
        this.mDrawnStateEvaluated = evaluated;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        super.onParentChanged(newParent, oldParent);
        setDrawnStateEvaluated(false);
        getDisplayContent().reapplyMagnificationSpec();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getOwningUid() {
        return this.mOwnerUid;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public String getOwningPackage() {
        return this.mAttrs.packageName;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public boolean canAddInternalSystemWindow() {
        return this.mOwnerCanAddInternalSystemWindow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean skipLayout() {
        if (!this.mWillReplaceWindow || (!this.mAnimatingExit && this.mReplacingRemoveRequested)) {
            ActivityRecord activityRecord = this.mActivityRecord;
            return activityRecord != null && activityRecord.mWaitForEnteringPinnedMode;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFrames(ClientWindowFrames clientWindowFrames, int requestedWidth, int requestedHeight) {
        WindowFrames windowFrames = this.mWindowFrames;
        this.mTmpRect.set(windowFrames.mParentFrame);
        if (ViewRootImpl.LOCAL_LAYOUT) {
            windowFrames.mCompatFrame.set(clientWindowFrames.frame);
            windowFrames.mFrame.set(clientWindowFrames.frame);
            windowFrames.mDisplayFrame.set(clientWindowFrames.displayFrame);
            windowFrames.mParentFrame.set(clientWindowFrames.parentFrame);
            if (hasCompatScale()) {
                windowFrames.mFrame.scale(this.mGlobalScale);
                windowFrames.mDisplayFrame.scale(this.mGlobalScale);
                windowFrames.mParentFrame.scale(this.mGlobalScale);
            }
        } else {
            windowFrames.mDisplayFrame.set(clientWindowFrames.displayFrame);
            windowFrames.mParentFrame.set(clientWindowFrames.parentFrame);
            windowFrames.mFrame.set(clientWindowFrames.frame);
            windowFrames.mCompatFrame.set(windowFrames.mFrame);
            if (hasCompatScale() || this.mNeedHWResizer) {
                windowFrames.mCompatFrame.scale(this.mInvGlobalScale);
            }
        }
        windowFrames.setParentFrameWasClippedByDisplayCutout(clientWindowFrames.isParentFrameClippedByDisplayCutout);
        windowFrames.mRelFrame.set(windowFrames.mFrame);
        WindowContainer<?> parent = getParent();
        int parentLeft = 0;
        int parentTop = 0;
        if (this.mIsChildWindow) {
            parentLeft = ((WindowState) parent).mWindowFrames.mFrame.left;
            parentTop = ((WindowState) parent).mWindowFrames.mFrame.top;
        } else if (parent != null) {
            Rect parentBounds = parent.getBounds();
            parentLeft = parentBounds.left;
            parentTop = parentBounds.top;
        }
        windowFrames.mRelFrame.offsetTo(windowFrames.mFrame.left - parentLeft, windowFrames.mFrame.top - parentTop);
        if (requestedWidth != this.mLastRequestedWidth || requestedHeight != this.mLastRequestedHeight || !this.mTmpRect.equals(windowFrames.mParentFrame)) {
            this.mLastRequestedWidth = requestedWidth;
            this.mLastRequestedHeight = requestedHeight;
            windowFrames.setContentChanged(true);
        }
        if (this.mAttrs.type == 2034 && !windowFrames.mFrame.equals(windowFrames.mLastFrame)) {
            this.mMovedByResize = true;
        }
        if (this.mIsWallpaper) {
            Rect lastFrame = windowFrames.mLastFrame;
            Rect frame = windowFrames.mFrame;
            if (lastFrame.width() != frame.width() || lastFrame.height() != frame.height()) {
                this.mDisplayContent.mWallpaperController.updateWallpaperOffset(this, false);
            }
        }
        updateSourceFrame(windowFrames.mFrame);
        if (ViewRootImpl.LOCAL_LAYOUT && !this.mHaveFrame) {
            updateLastFrames();
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && !this.mIsChildWindow) {
            activityRecord.layoutLetterbox(this);
        }
        this.mSurfacePlacementNeeded = true;
        this.mHaveFrame = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSourceFrame(Rect winFrame) {
        if (this.mGivenInsetsPending) {
            return;
        }
        SparseArray<InsetsSource> providedSources = getProvidedInsetsSources();
        InsetsStateController controller = getDisplayContent().getInsetsStateController();
        for (int i = providedSources.size() - 1; i >= 0; i--) {
            controller.getSourceProvider(providedSources.keyAt(i)).updateSourceFrame(winFrame);
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public Rect getBounds() {
        return this.mToken.hasSizeCompatBounds() ? this.mToken.getBounds() : super.getBounds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getFrame() {
        return this.mWindowFrames.mFrame;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getRelativeFrame() {
        return this.mWindowFrames.mRelFrame;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getDisplayFrame() {
        return this.mWindowFrames.mDisplayFrame;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getParentFrame() {
        return this.mWindowFrames.mParentFrame;
    }

    Rect getCompatFrame() {
        return this.mWindowFrames.mCompatFrame;
    }

    public WindowManager.LayoutParams getAttrs() {
        return this.mAttrs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowManager.LayoutParams getLayoutingAttrs(int rotation) {
        WindowManager.LayoutParams[] paramsForRotation = this.mAttrs.paramsForRotation;
        if (paramsForRotation == null || paramsForRotation.length != 4 || paramsForRotation[rotation] == null) {
            return this.mAttrs;
        }
        return paramsForRotation[rotation];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisableFlags() {
        return this.mDisableFlags;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public int getBaseType() {
        return getTopParentWindow().mAttrs.type;
    }

    boolean setReportResizeHints() {
        return this.mWindowFrames.setReportResizeHints();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateResizingWindowIfNeeded() {
        boolean insetsChanged = this.mWindowFrames.hasInsetsChanged();
        if ((!this.mHasSurface || getDisplayContent().mLayoutSeq != this.mLayoutSeq || isGoneForLayout()) && !insetsChanged) {
            return;
        }
        WindowStateAnimator winAnimator = this.mWinAnimator;
        boolean didFrameInsetsChange = setReportResizeHints();
        boolean configChanged = (this.mInRelayout || isLastConfigReportedToClient()) ? false : true;
        if (WindowManagerDebugConfig.DEBUG_CONFIGURATION && configChanged) {
            Slog.v("WindowManager", "Win " + this + " config changed: " + getConfiguration());
        }
        boolean dragResizingChanged = isDragResizeChanged() && !isDragResizingChangeReported();
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "Resizing " + this + ": configChanged=" + configChanged + " dragResizingChanged=" + dragResizingChanged + " last=" + this.mWindowFrames.mLastFrame + " frame=" + this.mWindowFrames.mFrame);
        }
        updateLastFrames();
        if (didFrameInsetsChange || configChanged || insetsChanged || dragResizingChanged || this.mReportOrientationChanged || shouldSendRedrawForSync()) {
            if (ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
                String protoLogParam0 = String.valueOf(this);
                String protoLogParam1 = String.valueOf(this.mWindowFrames.getInsetsChangedInfo());
                boolean protoLogParam2 = configChanged;
                boolean protoLogParam3 = dragResizingChanged;
                boolean protoLogParam4 = this.mReportOrientationChanged;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_RESIZE, 625447638, 1008, (String) null, new Object[]{protoLogParam0, protoLogParam1, Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4)});
            }
            if (insetsChanged) {
                this.mWindowFrames.setInsetsChanged(false);
                this.mWmService.mWindowsInsetsChanged--;
                if (this.mWmService.mWindowsInsetsChanged == 0) {
                    this.mWmService.mH.removeMessages(66);
                }
            }
            ActivityRecord activityRecord = this.mActivityRecord;
            if (activityRecord != null && this.mAppDied) {
                activityRecord.removeDeadWindows();
                return;
            }
            onResizeHandled();
            this.mWmService.makeWindowFreezingScreenIfNeededLocked(this);
            if (getOrientationChanging() || dragResizingChanged || isWindowModeChanged()) {
                if (dragResizingChanged && ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
                    String protoLogParam02 = String.valueOf(this);
                    String protoLogParam12 = String.valueOf(winAnimator.mSurfaceController);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_RESIZE, -1270148832, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
                }
                if (!getOrientationChanging() && !dragResizingChanged && isWindowModeChanged()) {
                    Slog.v("WindowManager", "OS ADD: WindowMode start waiting for draw, mDrawState=DRAW_PENDING in " + this + ", surfaceController " + winAnimator.mSurfaceController);
                }
                winAnimator.mDrawState = 1;
                ActivityRecord activityRecord2 = this.mActivityRecord;
                if (activityRecord2 != null) {
                    activityRecord2.clearAllDrawn();
                }
            }
            if (!this.mWmService.mResizingWindows.contains(this)) {
                if (ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
                    String protoLogParam03 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_RESIZE, 685047360, 0, (String) null, new Object[]{protoLogParam03});
                }
                this.mWmService.mResizingWindows.add(this);
                return;
            }
            return;
        }
        if (getOrientationChanging() && isDrawn()) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam04 = String.valueOf(this);
                String protoLogParam13 = String.valueOf(winAnimator.mSurfaceController);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1329340614, 0, (String) null, new Object[]{protoLogParam04, protoLogParam13});
            }
            setOrientationChanging(false);
            this.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mWmService.mDisplayFreezeTime);
        }
    }

    boolean isWindowModeChanged() {
        return (getConfiguration().windowConfiguration == null || getLastReportedConfiguration().windowConfiguration == null || getConfiguration().windowConfiguration.getWindowingMode() == getLastReportedConfiguration().windowConfiguration.getWindowingMode()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getOrientationChanging() {
        return ((!this.mOrientationChanging && (!isVisible() || getConfiguration().orientation == getLastReportedConfiguration().orientation)) || this.mSeamlesslyRotated || this.mOrientationChangeTimedOut) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOrientationChanging(boolean changing) {
        this.mOrientationChangeTimedOut = false;
        if (this.mOrientationChanging == changing) {
            return;
        }
        this.mOrientationChanging = changing;
        if (changing) {
            this.mLastFreezeDuration = 0;
            if (this.mWmService.mRoot.mOrientationChangeComplete && this.mDisplayContent.shouldSyncRotationChange(this)) {
                this.mWmService.mRoot.mOrientationChangeComplete = false;
                return;
            }
            return;
        }
        this.mDisplayContent.finishAsyncRotation(this.mToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void orientationChangeTimedOut() {
        this.mOrientationChangeTimedOut = true;
    }

    @Override // com.android.server.wm.WindowContainer
    public DisplayContent getDisplayContent() {
        return this.mToken.getDisplayContent();
    }

    @Override // com.android.server.wm.WindowContainer
    void onDisplayChanged(DisplayContent dc) {
        if (dc != null && this.mDisplayContent != null && dc != this.mDisplayContent && this.mDisplayContent.getImeInputTarget() == this) {
            dc.updateImeInputAndControlTarget(getImeInputTarget());
            this.mDisplayContent.setImeInputTarget(null);
        }
        super.onDisplayChanged(dc);
        if (dc != null && this.mInputWindowHandle.getDisplayId() != dc.getDisplayId()) {
            this.mLayoutSeq = dc.mLayoutSeq - 1;
            this.mInputWindowHandle.setDisplayId(dc.getDisplayId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayFrames getDisplayFrames(DisplayFrames originalFrames) {
        DisplayFrames displayFrames = this.mToken.getFixedRotationTransformDisplayFrames();
        if (displayFrames != null) {
            return displayFrames;
        }
        return originalFrames;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayInfo getDisplayInfo() {
        DisplayInfo displayInfo = this.mToken.getFixedRotationTransformDisplayInfo();
        if (displayInfo != null) {
            return displayInfo;
        }
        return getDisplayContent().getDisplayInfo();
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public Rect getMaxBounds() {
        Rect maxBounds = this.mToken.getFixedRotationTransformMaxBounds();
        if (maxBounds != null) {
            return maxBounds;
        }
        return super.getMaxBounds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getInsetsState() {
        return getInsetsState(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getInsetsState(boolean includeTransient) {
        InsetsState rotatedState = this.mToken.getFixedRotationTransformInsetsState();
        InsetsPolicy insetsPolicy = getDisplayContent().getInsetsPolicy();
        if (rotatedState != null) {
            return insetsPolicy.adjustInsetsForWindow(this, rotatedState);
        }
        InsetsSourceProvider provider = getControllableInsetProvider();
        int insetTypeProvidedByWindow = provider != null ? provider.getSource().getType() : -1;
        InsetsState rawInsetsState = this.mFrozenInsetsState;
        if (rawInsetsState == null) {
            rawInsetsState = getMergedInsetsState();
        }
        InsetsState insetsStateForWindow = insetsPolicy.enforceInsetsPolicyForTarget(insetTypeProvidedByWindow, getWindowingMode(), isAlwaysOnTop(), rawInsetsState);
        InsetsState state = insetsPolicy.adjustInsetsForWindow(this, insetsStateForWindow, includeTransient);
        return ITranWindowState.Instance().getInsetsState(this, state);
    }

    private InsetsState getMergedInsetsState() {
        InsetsState globalInsetsState;
        if (this.mAttrs.receiveInsetsIgnoringZOrder) {
            globalInsetsState = getDisplayContent().getInsetsStateController().getRawInsetsState();
        } else {
            globalInsetsState = this.mAboveInsetsState;
        }
        if (this.mMergedLocalInsetsSources == null) {
            return globalInsetsState;
        }
        InsetsState mergedInsetsState = new InsetsState(globalInsetsState);
        for (int i = 0; i < this.mMergedLocalInsetsSources.size(); i++) {
            mergedInsetsState.addSource(this.mMergedLocalInsetsSources.valueAt(i));
        }
        return mergedInsetsState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getCompatInsetsState() {
        InsetsState state = getInsetsState();
        if (hasCompatScale()) {
            InsetsState state2 = new InsetsState(state, true);
            state2.scale(this.mInvGlobalScale);
            return state2;
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getInsetsStateWithVisibilityOverride() {
        InsetsState state = new InsetsState(getInsetsState());
        for (int type = 0; type < 24; type++) {
            boolean requestedVisible = getRequestedVisibility(type);
            InsetsSource source = state.peekSource(type);
            if (source != null && source.isVisible() != requestedVisible) {
                InsetsSource source2 = new InsetsSource(source);
                source2.setVisible(requestedVisible);
                state.addSource(source2);
            }
        }
        return state;
    }

    @Override // com.android.server.wm.InputTarget
    public int getDisplayId() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return -1;
        }
        return displayContent.getDisplayId();
    }

    @Override // com.android.server.wm.InputTarget
    public WindowState getWindowState() {
        return this;
    }

    @Override // com.android.server.wm.InputTarget
    public IWindow getIWindow() {
        return this.mClient;
    }

    @Override // com.android.server.wm.InputTarget
    public int getPid() {
        return this.mSession.mPid;
    }

    @Override // com.android.server.wm.InputTarget
    public int getUid() {
        return this.mSession.mUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            return activityRecord.getTask();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getTaskFragment() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            return activityRecord.getTaskFragment();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask() {
        Task task = getTask();
        if (task != null) {
            return task.getRootTask();
        }
        DisplayContent dc = getDisplayContent();
        if (this.mAttrs.type < 2000 || dc == null) {
            return null;
        }
        return dc.getDefaultTaskDisplayArea().getRootHomeTask();
    }

    private void cutRect(Rect rect, Rect toRemove) {
        if (toRemove.isEmpty()) {
            return;
        }
        if (toRemove.top < rect.bottom && toRemove.bottom > rect.top) {
            if (toRemove.right >= rect.right && toRemove.left >= rect.left) {
                rect.right = toRemove.left;
            } else if (toRemove.left <= rect.left && toRemove.right <= rect.right) {
                rect.left = toRemove.right;
            }
        }
        if (toRemove.left < rect.right && toRemove.right > rect.left) {
            if (toRemove.bottom >= rect.bottom && toRemove.top >= rect.top) {
                rect.bottom = toRemove.top;
            } else if (toRemove.top <= rect.top && toRemove.bottom <= rect.bottom) {
                rect.top = toRemove.bottom;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getVisibleBounds(Rect bounds) {
        Task task = getTask();
        boolean intersectWithRootTaskBounds = task != null && task.cropWindowsToRootTaskBounds();
        bounds.setEmpty();
        this.mTmpRect.setEmpty();
        if (intersectWithRootTaskBounds) {
            Task rootTask = task.getRootTask();
            if (rootTask != null) {
                rootTask.getDimBounds(this.mTmpRect);
            } else {
                intersectWithRootTaskBounds = false;
            }
        }
        bounds.set(this.mWindowFrames.mFrame);
        bounds.inset(getInsetsStateWithVisibilityOverride().calculateVisibleInsets(bounds, this.mAttrs.type, getWindowingMode(), this.mAttrs.softInputMode, this.mAttrs.flags));
        if (intersectWithRootTaskBounds) {
            bounds.intersect(this.mTmpRect);
        }
    }

    public long getInputDispatchingTimeoutMillis() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            return activityRecord.mInputDispatchingTimeoutMillis;
        }
        return InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAppShownWindows() {
        ActivityRecord activityRecord = this.mActivityRecord;
        return activityRecord != null && (activityRecord.firstWindowDrawn || this.mActivityRecord.isStartingWindowDisplayed());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean layoutForART() {
        if (this.mNeedHWResizer) {
            float f = this.mHWScale;
            this.mGlobalScale = f;
            this.mInvGlobalScale = 1.0f / f;
            Slog.v("AppResolutionTuner", "windowstate layoutForART() Need HWResizer, mGlobalScale = " + this.mGlobalScale + " , this = " + this);
        }
        return this.mNeedHWResizer;
    }

    void prelayout() {
        float f;
        if (this.mNeedHWResizer) {
            float f2 = this.mHWScale;
            this.mGlobalScale = f2;
            this.mInvGlobalScale = 1.0f / f2;
            Slog.v("AppResolutionTuner", "windowstate prelayout() Need HWResizer, mGlobalScale = " + this.mGlobalScale + " , this = " + this);
        } else if (hasCompatScale()) {
            if (this.mOverrideScale != 1.0f) {
                if (this.mToken.hasSizeCompatBounds()) {
                    f = this.mToken.getSizeCompatScale() * this.mOverrideScale;
                } else {
                    f = this.mOverrideScale;
                }
                this.mGlobalScale = f;
            } else {
                this.mGlobalScale = this.mToken.getSizeCompatScale();
            }
            this.mInvGlobalScale = 1.0f / this.mGlobalScale;
        } else {
            this.mInvGlobalScale = 1.0f;
            this.mGlobalScale = 1.0f;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean hasContentToDisplay() {
        if (!this.mAppFreezing && isDrawn()) {
            if (this.mViewVisibility != 0) {
                if (isAnimating(3) && !getDisplayContent().mAppTransition.isTransitionSet()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return super.hasContentToDisplay();
    }

    private boolean isVisibleByPolicyOrInsets() {
        return isVisibleByPolicy() && (this.mControllableInsetProvider == null || this.mControllableInsetProvider.isClientVisible());
    }

    @Override // com.android.server.wm.WindowContainer
    public boolean isVisible() {
        return wouldBeVisibleIfPolicyIgnored() && isVisibleByPolicyOrInsets();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isVisibleRequested() {
        boolean localVisibleRequested = wouldBeVisibleRequestedIfPolicyIgnored() && isVisibleByPolicyOrInsets();
        if (localVisibleRequested && shouldCheckTokenVisibleRequested()) {
            return this.mToken.isVisibleRequested();
        }
        return localVisibleRequested;
    }

    boolean shouldCheckTokenVisibleRequested() {
        return (this.mActivityRecord == null && this.mToken.asWallpaperToken() == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleByPolicy() {
        return (this.mPolicyVisibility & 3) == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearPolicyVisibilityFlag(int policyVisibilityFlag) {
        this.mPolicyVisibility &= ~policyVisibilityFlag;
        this.mWmService.scheduleAnimationLocked();
    }

    void setPolicyVisibilityFlag(int policyVisibilityFlag) {
        this.mPolicyVisibility |= policyVisibilityFlag;
        this.mWmService.scheduleAnimationLocked();
    }

    private boolean isLegacyPolicyVisibility() {
        return (this.mPolicyVisibility & 1) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean wouldBeVisibleIfPolicyIgnored() {
        if (!this.mHasSurface || isParentWindowHidden() || this.mAnimatingExit || this.mDestroying) {
            return false;
        }
        boolean isWallpaper = this.mToken.asWallpaperToken() != null;
        return !isWallpaper || this.mToken.isVisible();
    }

    private boolean wouldBeVisibleRequestedIfPolicyIgnored() {
        WindowState parent = getParentWindow();
        boolean isParentHiddenRequested = (parent == null || parent.isVisibleRequested()) ? false : true;
        if (isParentHiddenRequested || this.mAnimatingExit || this.mDestroying) {
            return false;
        }
        boolean isWallpaper = this.mToken.asWallpaperToken() != null;
        return !isWallpaper || this.mToken.isVisibleRequested();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWinVisibleLw() {
        ActivityRecord activityRecord = this.mActivityRecord;
        return (activityRecord == null || activityRecord.mVisibleRequested || this.mActivityRecord.isAnimating(3)) && isVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleNow() {
        return (this.mToken.isVisible() || this.mAttrs.type == 3) && isVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPotentialDragTarget(boolean targetInterceptsGlobalDrag) {
        return ((!targetInterceptsGlobalDrag && !isVisibleNow()) || this.mRemoved || this.mInputChannel == null || this.mInputWindowHandle == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleRequestedOrAdding() {
        ActivityRecord atoken = this.mActivityRecord;
        return (this.mHasSurface || (!this.mRelayoutCalled && this.mViewVisibility == 0)) && isVisibleByPolicy() && !isParentWindowHidden() && !((atoken != null && !atoken.mVisibleRequested) || this.mAnimatingExit || this.mDestroying);
    }

    public boolean isOnScreen() {
        if (this.mHasSurface && !this.mDestroying && isVisibleByPolicy()) {
            ActivityRecord atoken = this.mActivityRecord;
            if (atoken != null) {
                return (!isParentWindowHidden() && atoken.isVisible()) || isAnimating(3);
            }
            WallpaperWindowToken wtoken = this.mToken.asWallpaperToken();
            return wtoken != null ? !isParentWindowHidden() && wtoken.isVisible() : !isParentWindowHidden() || isAnimating(3);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDreamWindow() {
        WindowManager.LayoutParams layoutParams;
        ActivityRecord activityRecord = this.mActivityRecord;
        return (activityRecord != null && activityRecord.getActivityType() == 5) || ((layoutParams = this.mAttrs) != null && layoutParams.type == 2044);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSecureLocked() {
        if ((this.mAttrs.flags & 8192) != 0) {
            return true;
        }
        return !DevicePolicyCache.getInstance().isScreenCaptureAllowed(this.mShowUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean mightAffectAllDrawn() {
        boolean isAppType = this.mWinAnimator.mAttrType == 1 || this.mWinAnimator.mAttrType == 4;
        return ((!isOnScreen() && !isAppType) || this.mAnimatingExit || this.mDestroying) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInteresting() {
        ActivityRecord activityRecord = this.mActivityRecord;
        return (activityRecord == null || this.mAppDied || (activityRecord.isFreezingScreen() && this.mAppFreezing) || this.mViewVisibility != 0) ? false : true;
    }

    boolean isReadyForDisplay() {
        if (this.mToken.waitingToShow && getDisplayContent().mAppTransition.isTransitionSet()) {
            return false;
        }
        boolean parentAndClientVisible = !isParentWindowHidden() && this.mViewVisibility == 0 && this.mToken.isVisible();
        if (this.mHasSurface && isVisibleByPolicy() && !this.mDestroying) {
            return parentAndClientVisible || isAnimating(3);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullyTransparent() {
        return this.mAttrs.alpha == 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canAffectSystemUiFlags() {
        if (isFullyTransparent()) {
            return false;
        }
        if (this.mActivityRecord == null) {
            boolean shown = this.mWinAnimator.getShown();
            boolean exiting = this.mAnimatingExit || this.mDestroying;
            return shown && !exiting;
        }
        boolean shown2 = ThunderbackConfig.isVersion4();
        if (shown2 && this.mActivityRecord.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            return false;
        }
        Task task = getTask();
        boolean canFromTask = task != null && task.canAffectSystemUiFlags();
        return canFromTask && this.mActivityRecord.isVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisplayed() {
        ActivityRecord atoken = this.mActivityRecord;
        return isDrawn() && isVisibleByPolicy() && ((!isParentWindowHidden() && (atoken == null || atoken.mVisibleRequested)) || isAnimating(3));
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public boolean isAnimatingLw() {
        return isAnimating(3);
    }

    public boolean isGoneForLayout() {
        ActivityRecord atoken = this.mActivityRecord;
        return this.mViewVisibility == 8 || !this.mRelayoutCalled || (atoken == null && !(wouldBeVisibleIfPolicyIgnored() && isVisibleByPolicy())) || (!(atoken == null || atoken.mVisibleRequested) || isParentWindowGoneForLayout() || ((this.mAnimatingExit && !isAnimatingLw()) || this.mDestroying));
    }

    public boolean isDrawFinishedLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 2 || this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    public boolean isDrawn() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    private boolean isOpaqueDrawn() {
        boolean isWallpaper = this.mToken.asWallpaperToken() != null;
        return ((!isWallpaper && this.mAttrs.format == -1) || (isWallpaper && this.mToken.isVisible())) && isDrawn() && !isAnimating(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestDrawIfNeeded(List<WindowState> outWaitingForDrawn) {
        if (!isVisible()) {
            return;
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            if (activityRecord.allDrawn) {
                return;
            }
            if (this.mAttrs.type == 3) {
                if (isDrawn()) {
                    return;
                }
            } else if (this.mActivityRecord.mStartingWindow != null) {
                return;
            }
        } else if (!this.mPolicy.isKeyguardHostWindow(this.mAttrs)) {
            return;
        }
        if (!ITranWindowManagerService.Instance().isInPreWakeupInProgress(this, outWaitingForDrawn)) {
            this.mWinAnimator.mDrawState = 1;
            forceReportingResized();
            outWaitingForDrawn.add(this);
            return;
        }
        boolean keyguard = this.mPolicy.isKeyguardHostWindow(this.mAttrs);
        if (!hasDrawn() && !keyguard) {
            Slog.i("WindowManager", " waitForAllWindowsDrawn Add win=" + this + " in prewake.");
            outWaitingForDrawn.add(this);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void onMovedByResize() {
        if (ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RESIZE, 1635462459, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mMovedByResize = true;
        super.onMovedByResize();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppVisibilityChanged(boolean visible, boolean runningAppAnimation) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).onAppVisibilityChanged(visible, runningAppAnimation);
        }
        boolean isVisibleNow = isVisibleNow();
        if (this.mAttrs.type == 3) {
            if (!visible && isVisibleNow && this.mActivityRecord.isAnimating(3)) {
                if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, -1471518109, 0, (String) null, new Object[]{protoLogParam0});
                }
                this.mAnimatingExit = true;
                this.mRemoveOnExit = true;
                this.mWindowRemovalAllowed = true;
            }
        } else if (visible != isVisibleNow) {
            if (!runningAppAnimation && isVisibleNow) {
                AccessibilityController accessibilityController = this.mWmService.mAccessibilityController;
                this.mWinAnimator.applyAnimationLocked(2, false);
                if (accessibilityController.hasCallbacks()) {
                    accessibilityController.onWindowTransition(this, 2);
                }
            }
            setDisplayLayoutNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onSetAppExiting(boolean animateExit) {
        DisplayContent displayContent = getDisplayContent();
        boolean changed = false;
        if (!animateExit) {
            this.mPermanentlyHidden = true;
            hide(false, false);
        }
        if (isVisibleNow() && animateExit) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (this.mWmService.mAccessibilityController.hasCallbacks()) {
                this.mWmService.mAccessibilityController.onWindowTransition(this, 2);
            }
            changed = true;
            if (displayContent != null) {
                displayContent.setLayoutNeeded();
            }
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            changed |= c.onSetAppExiting(animateExit);
        }
        return changed;
    }

    @Override // com.android.server.wm.WindowContainer
    void onResize() {
        ArrayList<WindowState> resizingWindows = this.mWmService.mResizingWindows;
        if (this.mHasSurface && !isGoneForLayout() && !resizingWindows.contains(this)) {
            if (ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RESIZE, 417311568, 0, (String) null, new Object[]{protoLogParam0});
            }
            resizingWindows.add(this);
        }
        if (isGoneForLayout()) {
            this.mResizedWhileGone = true;
        }
        super.onResize();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleWindowMovedIfNeeded() {
        if (!hasMoved()) {
            return;
        }
        int left = this.mWindowFrames.mFrame.left;
        int top = this.mWindowFrames.mFrame.top;
        if (canPlayMoveAnimation()) {
            startMoveAnimation(left, top);
        }
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
            this.mWmService.mAccessibilityController.onSomeWindowResizedOrMoved(getDisplayId());
        }
        try {
            this.mClient.moved(left, top);
        } catch (RemoteException e) {
        }
        this.mMovedByResize = false;
    }

    private boolean canPlayMoveAnimation() {
        boolean hasMovementAnimation;
        if (getTask() == null) {
            hasMovementAnimation = getWindowConfiguration().hasMovementAnimations();
        } else {
            hasMovementAnimation = getTask().getWindowConfiguration().hasMovementAnimations();
        }
        return this.mToken.okToAnimate() && (this.mAttrs.privateFlags & 64) == 0 && !isDragResizing() && hasMovementAnimation && !this.mWinAnimator.mLastHidden && !this.mSeamlesslyRotated;
    }

    private boolean hasMoved() {
        return this.mHasSurface && !((!this.mWindowFrames.hasContentChanged() && !this.mMovedByResize) || this.mAnimatingExit || ((this.mWindowFrames.mRelFrame.top == this.mWindowFrames.mLastRelFrame.top && this.mWindowFrames.mRelFrame.left == this.mWindowFrames.mLastRelFrame.left) || ((this.mIsChildWindow && getParentWindow().hasMoved()) || this.mTransitionController.isCollecting())));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isObscuringDisplay() {
        Task task = getTask();
        return (task == null || task.getRootTask() == null || task.getRootTask().fillsParent()) && isOpaqueDrawn() && fillsDisplay();
    }

    boolean fillsDisplay() {
        DisplayInfo displayInfo = getDisplayInfo();
        return this.mWindowFrames.mFrame.left <= 0 && this.mWindowFrames.mFrame.top <= 0 && this.mWindowFrames.mFrame.right >= displayInfo.appWidth && this.mWindowFrames.mFrame.bottom >= displayInfo.appHeight;
    }

    public boolean matchesDisplayAreaBounds() {
        Rect rotatedDisplayBounds = this.mToken.getFixedRotationTransformDisplayBounds();
        if (rotatedDisplayBounds != null) {
            return rotatedDisplayBounds.equals(getBounds());
        }
        DisplayArea displayArea = getDisplayArea();
        if (displayArea == null) {
            return getDisplayContent().getBounds().equals(getBounds());
        }
        return displayArea.getBounds().equals(getBounds());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLastConfigReportedToClient() {
        return this.mLastConfigReportedToClient;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        if (ITranWindowState.Instance().isThunderbackWindow(newParentConfig)) {
            this.mGlobalScale = 1.0f;
        }
        TranFoldWMCustody.instance().onConfigurationChanged(this, newParentConfig);
        boolean updateImeForcely = ITranWindowState.Instance().isUpdateImeForcely(newParentConfig, getConfiguration());
        if (getDisplayContent().getImeInputTarget() != this && !isImeLayeringTarget()) {
            super.onConfigurationChanged(newParentConfig);
            return;
        }
        this.mTempConfiguration.setTo(getConfiguration());
        super.onConfigurationChanged(newParentConfig);
        boolean windowConfigChanged = this.mTempConfiguration.windowConfiguration.diff(newParentConfig.windowConfiguration, false) != 0;
        if (windowConfigChanged) {
            getDisplayContent().updateImeControlTarget(updateImeForcely);
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    void onMergedOverrideConfigurationChanged() {
        super.onMergedOverrideConfigurationChanged();
        this.mLastConfigReportedToClient = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowReplacementTimeout() {
        if (this.mWillReplaceWindow) {
            removeImmediately();
            return;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.onWindowReplacementTimeout();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeImmediately() {
        ActivityRecord activityRecord;
        if (this.mWeltLeash != null) {
            SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
            ITranWindowState.Instance().removeWeltLeash(this, t);
        }
        if (this.mRemoved) {
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 2018454757, 0, (String) null, new Object[]{protoLogParam0});
                return;
            }
            return;
        }
        this.mRemoved = true;
        this.mWinAnimator.destroySurfaceLocked(getSyncTransaction());
        super.removeImmediately();
        this.mWillReplaceWindow = false;
        WindowState windowState = this.mReplacementWindow;
        if (windowState != null) {
            windowState.mSkipEnterAnimationForSeamlessReplacement = false;
        }
        DisplayContent dc = getDisplayContent();
        if (isImeLayeringTarget()) {
            dc.removeImeScreenshotIfPossible();
            dc.setImeLayeringTarget(null);
            dc.computeImeTarget(true);
        }
        if (dc.getImeInputTarget() == this && ((activityRecord = this.mActivityRecord) == null || !activityRecord.isRelaunching())) {
            dc.updateImeInputAndControlTarget(null);
        }
        int type = this.mAttrs.type;
        if (WindowManagerService.excludeWindowTypeFromTapOutTask(type)) {
            dc.mTapExcludedWindows.remove(this);
        }
        dc.mTapExcludeProvidingWindows.remove(this);
        dc.getDisplayPolicy().removeWindowLw(this);
        disposeInputChannel();
        this.mOnBackInvokedCallbackInfo = null;
        this.mSession.windowRemovedLocked();
        try {
            this.mClient.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        } catch (RuntimeException e) {
        }
        this.mWmService.postWindowRemoveCleanupLocked(this);
        TranFoldWMCustody.instance().removeImmediately(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeIfPossible() {
        super.removeIfPossible();
        removeIfPossible(false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2896=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public void removeIfPossible(boolean keepVisibleDeadWindow) {
        int i;
        int i2;
        int i3;
        int i4;
        ActivityRecord activityRecord;
        this.mWindowRemovalAllowed = true;
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1504168072, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        boolean startingWindow = this.mAttrs.type == 3;
        if (startingWindow) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -986746907, 0, (String) null, new Object[]{protoLogParam02});
            }
            ActivityRecord activityRecord2 = this.mActivityRecord;
            if (activityRecord2 != null) {
                activityRecord2.forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.WindowState$$ExternalSyntheticLambda0
                    public final boolean apply(Object obj) {
                        return WindowState.lambda$removeIfPossible$2((WindowState) obj);
                    }
                }, true);
            }
        } else if (this.mAttrs.type == 1 && isSelfAnimating(0, 128)) {
            cancelAnimation();
        }
        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
            long protoLogParam03 = System.identityHashCode(this.mClient.asBinder());
            String protoLogParam12 = String.valueOf(this.mWinAnimator.mSurfaceController);
            String protoLogParam2 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS, -1047945589, 1, (String) null, new Object[]{Long.valueOf(protoLogParam03), protoLogParam12, protoLogParam2});
        }
        long origId = Binder.clearCallingIdentity();
        try {
            disposeInputChannel();
            this.mOnBackInvokedCallbackInfo = null;
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam04 = String.valueOf(this);
                String protoLogParam13 = String.valueOf(this.mWinAnimator.mSurfaceController);
                boolean protoLogParam22 = this.mAnimatingExit;
                boolean protoLogParam3 = this.mRemoveOnExit;
                boolean protoLogParam4 = this.mHasSurface;
                boolean protoLogParam5 = this.mWinAnimator.getShown();
                boolean protoLogParam6 = isAnimating(3);
                ActivityRecord activityRecord3 = this.mActivityRecord;
                boolean protoLogParam7 = activityRecord3 != null && activityRecord3.isAnimating(3);
                boolean protoLogParam8 = this.mWillReplaceWindow;
                boolean protoLogParam9 = this.mWmService.mDisplayFrozen;
                String protoLogParam10 = String.valueOf(Debug.getCallers(6));
                i = 2;
                i3 = 4;
                i2 = 5;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 594260654, 1048560, (String) null, new Object[]{protoLogParam04, protoLogParam13, Boolean.valueOf(protoLogParam22), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4), Boolean.valueOf(protoLogParam5), Boolean.valueOf(protoLogParam6), Boolean.valueOf(protoLogParam7), Boolean.valueOf(protoLogParam8), Boolean.valueOf(protoLogParam9), protoLogParam10});
            } else {
                i = 2;
                i2 = 5;
                i3 = 4;
            }
            boolean wasVisible = false;
            if (!this.mHasSurface || !this.mToken.okToAnimate()) {
                i4 = 0;
            } else if (this.mWillReplaceWindow) {
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam05 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1921821199, 0, (String) null, new Object[]{protoLogParam05});
                }
                if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                    String protoLogParam06 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, -799003045, 0, (String) null, new Object[]{protoLogParam06});
                }
                this.mAnimatingExit = true;
                this.mReplacingRemoveRequested = true;
                return;
            } else {
                wasVisible = isWinVisibleLw();
                if (keepVisibleDeadWindow) {
                    if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                        String protoLogParam07 = String.valueOf(this);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 2114149926, 0, (String) null, new Object[]{protoLogParam07});
                    }
                    this.mAppDied = true;
                    setDisplayLayoutNeeded();
                    this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
                    openInputChannel(null);
                    getDisplayContent().getInputMonitor().updateInputWindowsLw(true);
                    return;
                }
                boolean allowExitAnimation = !getDisplayContent().inTransition();
                if (wasVisible) {
                    if (!startingWindow) {
                        i2 = i;
                    }
                    int transit = i2;
                    int animationType = ITranWindowState.Instance().getAnimationTypeNormal();
                    if (this.mAttrs.type == 2044) {
                        animationType = ITranWindowState.Instance().getAnimationTypeDreamAod();
                    }
                    if (allowExitAnimation && this.mWinAnimator.applyAnimationLocked(transit, false, animationType)) {
                        if (animationType == ITranWindowState.Instance().getAnimationTypeDreamAod()) {
                            int dreamAnimation = ITranWindowState.Instance().getDreamAnimation();
                            Slog.i("WindowManager", "  dreamAnimation=" + dreamAnimation);
                            if (dreamAnimation == ITranWindowState.Instance().getAnimationFromAodToKeyguard()) {
                                getDisplayContent().getDisplayPolicy().startKeyguardWindowAnimation(1, true, ITranWindowState.Instance().getAnimationTypeDreamKeyguard());
                            }
                            ITranWindowState.Instance().hookAodWindowRemove();
                        }
                        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                            String protoLogParam08 = String.valueOf(this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, -91393839, 0, (String) null, new Object[]{protoLogParam08});
                        }
                        this.mAnimatingExit = true;
                        setDisplayLayoutNeeded();
                        this.mWmService.requestTraversal();
                    }
                    if (this.mWmService.mAccessibilityController.hasCallbacks()) {
                        this.mWmService.mAccessibilityController.onWindowTransition(this, transit);
                    }
                }
                boolean isAnimating = allowExitAnimation && (this.mAnimatingExit || isExitAnimationRunningSelfOrParent());
                boolean lastWindowIsStartingWindow = startingWindow && (activityRecord = this.mActivityRecord) != null && activityRecord.isLastWindow(this);
                if (this.mWinAnimator.getShown() && !lastWindowIsStartingWindow && isAnimating) {
                    this.mAnimatingExit = true;
                    if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                        String protoLogParam09 = String.valueOf(this);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1103716954, 0, (String) null, new Object[]{protoLogParam09});
                    }
                    if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                        String protoLogParam010 = String.valueOf(this);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 975275467, 0, (String) null, new Object[]{protoLogParam010});
                    }
                    setupWindowForRemoveOnExit();
                    ActivityRecord activityRecord4 = this.mActivityRecord;
                    if (activityRecord4 != null) {
                        activityRecord4.updateReportedVisibilityLocked();
                    }
                    return;
                }
                i4 = 0;
            }
            removeImmediately();
            if (wasVisible) {
                DisplayContent displayContent = getDisplayContent();
                if (displayContent.updateOrientation()) {
                    displayContent.sendNewConfiguration();
                }
            }
            this.mWmService.updateFocusedWindowLocked(isFocused() ? i3 : i4, true);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeIfPossible$2(WindowState w) {
        if (!w.isSelfAnimating(0, 128)) {
            return false;
        }
        w.cancelAnimation();
        return true;
    }

    private void setupWindowForRemoveOnExit() {
        this.mRemoveOnExit = true;
        setDisplayLayoutNeeded();
        getDisplayContent().getDisplayPolicy().removeWindowLw(this);
        boolean focusChanged = this.mWmService.updateFocusedWindowLocked(3, false);
        this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
        if (focusChanged) {
            getDisplayContent().getInputMonitor().updateInputWindowsLw(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasSurface(boolean hasSurface) {
        this.mHasSurface = hasSurface;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeImeTarget() {
        ActivityRecord activityRecord;
        ActivityRecord activityRecord2;
        int fl;
        if (this.mIsImWindow || inPinnedWindowingMode() || this.mAttrs.type == 2036) {
            return false;
        }
        ActivityRecord activityRecord3 = this.mActivityRecord;
        boolean windowsAreFocusable = activityRecord3 == null || activityRecord3.windowsAreFocusable();
        if (windowsAreFocusable) {
            Task rootTask = getRootTask();
            if (rootTask == null || rootTask.isFocusable()) {
                if (this.mAttrs.type == 3 || (fl = this.mAttrs.flags & 131080) == 0 || fl == 131080) {
                    if (rootTask == null || this.mActivityRecord == null || !this.mTransitionController.isTransientLaunch(this.mActivityRecord)) {
                        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                            Slog.i("WindowManager", "isVisibleRequestedOrAdding " + this + ": " + isVisibleRequestedOrAdding() + " isVisible: " + (isVisible() && (activityRecord2 = this.mActivityRecord) != null && activityRecord2.isVisible()));
                            if (!isVisibleRequestedOrAdding()) {
                                Slog.i("WindowManager", "  mSurfaceController=" + this.mWinAnimator.mSurfaceController + " relayoutCalled=" + this.mRelayoutCalled + " viewVis=" + this.mViewVisibility + " policyVis=" + isVisibleByPolicy() + " policyVisAfterAnim=" + this.mLegacyPolicyVisibilityAfterAnim + " parentHidden=" + isParentWindowHidden() + " exiting=" + this.mAnimatingExit + " destroying=" + this.mDestroying);
                                if (this.mActivityRecord != null) {
                                    Slog.i("WindowManager", "  mActivityRecord.visibleRequested=" + this.mActivityRecord.mVisibleRequested);
                                }
                            }
                        }
                        return isVisibleRequestedOrAdding() || (isVisible() && (activityRecord = this.mActivityRecord) != null && activityRecord.isVisible());
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DeadWindowEventReceiver extends InputEventReceiver {
        DeadWindowEventReceiver(InputChannel inputChannel) {
            super(inputChannel, WindowState.this.mWmService.mH.getLooper());
        }

        public void onInputEvent(InputEvent event) {
            finishInputEvent(event, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void openInputChannel(InputChannel outInputChannel) {
        if (this.mInputChannel != null) {
            throw new IllegalStateException("Window already has an input channel.");
        }
        String name = getName();
        InputChannel createInputChannel = this.mWmService.mInputManager.createInputChannel(name);
        this.mInputChannel = createInputChannel;
        IBinder token = createInputChannel.getToken();
        this.mInputChannelToken = token;
        this.mInputWindowHandle.setToken(token);
        this.mWmService.mInputToWindowMap.put(this.mInputChannelToken, this);
        if (outInputChannel != null) {
            this.mInputChannel.copyTo(outInputChannel);
        } else {
            this.mDeadWindowEventReceiver = new DeadWindowEventReceiver(this.mInputChannel);
        }
    }

    public boolean transferTouch() {
        return this.mWmService.mInputManager.transferTouch(this.mInputChannelToken, getDisplayId());
    }

    void disposeInputChannel() {
        DeadWindowEventReceiver deadWindowEventReceiver = this.mDeadWindowEventReceiver;
        if (deadWindowEventReceiver != null) {
            deadWindowEventReceiver.dispose();
            this.mDeadWindowEventReceiver = null;
        }
        if (this.mInputChannelToken != null) {
            this.mWmService.mInputManager.removeInputChannel(this.mInputChannelToken);
            this.mWmService.mKeyInterceptionInfoForToken.remove(this.mInputChannelToken);
            this.mWmService.mInputToWindowMap.remove(this.mInputChannelToken);
            this.mInputChannelToken = null;
        }
        InputChannel inputChannel = this.mInputChannel;
        if (inputChannel != null) {
            inputChannel.dispose();
            this.mInputChannel = null;
        }
        this.mInputWindowHandle.setToken(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeReplacedWindowIfNeeded(WindowState replacement) {
        if (this.mWillReplaceWindow && this.mReplacementWindow == replacement && replacement.hasDrawn()) {
            replacement.mSkipEnterAnimationForSeamlessReplacement = false;
            removeReplacedWindow();
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            if (c.removeReplacedWindowIfNeeded(replacement)) {
                return true;
            }
        }
        return false;
    }

    private void removeReplacedWindow() {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -320419645, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mWillReplaceWindow = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mReplacementWindow = null;
        if (!this.mAnimatingExit) {
        }
        removeImmediately();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setReplacementWindowIfNeeded(WindowState replacementCandidate) {
        boolean replacementSet = false;
        if (this.mWillReplaceWindow && this.mReplacementWindow == null && getWindowTag().toString().equals(replacementCandidate.getWindowTag().toString())) {
            this.mReplacementWindow = replacementCandidate;
            replacementCandidate.mSkipEnterAnimationForSeamlessReplacement = !this.mAnimateReplacingWindow;
            replacementSet = true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            replacementSet |= c.setReplacementWindowIfNeeded(replacementCandidate);
        }
        return replacementSet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayLayoutNeeded() {
        DisplayContent dc = getDisplayContent();
        if (dc != null) {
            dc.setLayoutNeeded();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void switchUser(int userId) {
        super.switchUser(userId);
        if (showToCurrentUser()) {
            setPolicyVisibilityFlag(2);
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.w("WindowManager", "user changing, hiding " + this + ", attrs=" + this.mAttrs.type + ", belonging to " + this.mOwnerUid);
        }
        clearPolicyVisibilityFlag(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getSurfaceTouchableRegion(Region region, WindowManager.LayoutParams attrs) {
        boolean modal = attrs.isModal();
        if (modal) {
            if (this.mActivityRecord != null) {
                updateRegionForModalActivityWindow(region);
            } else {
                getDisplayContent().getBounds(this.mTmpRect);
                int dw = this.mTmpRect.width();
                int dh = this.mTmpRect.height();
                region.set(-dw, -dh, dw + dw, dh + dh);
            }
            subtractTouchExcludeRegionIfNeeded(region);
        } else {
            getTouchableRegion(region);
        }
        Rect frame = this.mWindowFrames.mFrame;
        if (frame.left != 0 || frame.top != 0) {
            region.translate(-frame.left, -frame.top);
        }
        if (modal && this.mTouchableInsets == 3) {
            this.mTmpRegion.set(0, 0, frame.right, frame.bottom);
            this.mTmpRegion.op(this.mGivenTouchableRegion, Region.Op.DIFFERENCE);
            region.op(this.mTmpRegion, Region.Op.DIFFERENCE);
        }
        float f = this.mInvGlobalScale;
        if (f != 1.0f) {
            region.scale(f);
        }
    }

    private void adjustRegionInFreefromWindowMode(Rect inOutRect) {
        if (ThunderbackConfig.isVersion4()) {
            if (!inFreeformWindowingMode() && !isInMultiWindow()) {
                return;
            }
        } else if (!inFreeformWindowingMode()) {
            return;
        }
        DisplayMetrics displayMetrics = getDisplayContent().getDisplayMetrics();
        int delta = WindowManagerService.dipToPixel(30, displayMetrics);
        inOutRect.inset(-delta, -delta);
    }

    private void updateRegionForModalActivityWindow(Region outRegion) {
        this.mActivityRecord.getLetterboxInnerBounds(this.mTmpRect);
        if (this.mTmpRect.isEmpty()) {
            Rect transformedBounds = this.mActivityRecord.getFixedRotationTransformDisplayBounds();
            if (transformedBounds != null) {
                this.mTmpRect.set(transformedBounds);
            } else {
                TaskFragment taskFragment = getTaskFragment();
                if (taskFragment != null && !isOSFullDialog() && (getParent() == null || !getParent().isOSFullDialog())) {
                    Task task = taskFragment.asTask();
                    if (task != null) {
                        task.getDimBounds(this.mTmpRect);
                    } else {
                        this.mTmpRect.set(taskFragment.getBounds());
                    }
                } else if (getRootTask() != null) {
                    getRootTask().getDimBounds(this.mTmpRect);
                }
            }
        }
        adjustRegionInFreefromWindowMode(this.mTmpRect);
        outRegion.set(this.mTmpRect);
        cropRegionToRootTaskBoundsIfNeeded(outRegion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkPolicyVisibilityChange() {
        if (isLegacyPolicyVisibility() != this.mLegacyPolicyVisibilityAfterAnim) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Policy visibility changing after anim in " + this.mWinAnimator + ": " + this.mLegacyPolicyVisibilityAfterAnim);
            }
            if (this.mLegacyPolicyVisibilityAfterAnim) {
                setPolicyVisibilityFlag(1);
            } else {
                clearPolicyVisibilityFlag(1);
            }
            if (!isVisibleByPolicy()) {
                this.mWinAnimator.hide(SurfaceControl.getGlobalTransaction(), "checkPolicyVisibilityChange");
                if (isFocused()) {
                    if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                        ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 693423992, 0, (String) null, (Object[]) null);
                    }
                    this.mWmService.mFocusMayChange = true;
                }
                setDisplayLayoutNeeded();
                this.mWmService.enableScreenIfNeededLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequestedSize(int requestedWidth, int requestedHeight) {
        int i;
        int i2 = this.mRequestedWidth;
        if (i2 != requestedWidth || (i = this.mRequestedHeight) != requestedHeight || (ENABLE_DOUBLE_WALLPAPER_SUPPROT && this.mIsWallpaper && i2 == -1 && i == -1)) {
            this.mLayoutNeeded = true;
            this.mRequestedWidth = requestedWidth;
            this.mRequestedHeight = requestedHeight;
        }
    }

    void prepareWindowToDisplayDuringRelayout(boolean wasVisible) {
        ActivityRecord activityRecord;
        boolean hasTurnScreenOnFlag = (this.mAttrs.flags & 2097152) != 0 || ((activityRecord = this.mActivityRecord) != null && activityRecord.canTurnScreenOn());
        if (hasTurnScreenOnFlag) {
            boolean allowTheaterMode = this.mWmService.mAllowTheaterModeWakeFromLayout || Settings.Global.getInt(this.mWmService.mContext.getContentResolver(), "theater_mode_on", 0) == 0;
            ActivityRecord activityRecord2 = this.mActivityRecord;
            boolean canTurnScreenOn = activityRecord2 == null || activityRecord2.currentLaunchCanTurnScreenOn();
            if (allowTheaterMode && canTurnScreenOn && (this.mWmService.mAtmService.isDreaming() || !this.mPowerManagerWrapper.isInteractive())) {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_POWER) {
                    Slog.v("WindowManager", "Relayout window turning screen on: " + this);
                }
                this.mPowerManagerWrapper.wakeUp(SystemClock.uptimeMillis(), 2, "android.server.wm:SCREEN_ON_FLAG");
            }
            ActivityRecord activityRecord3 = this.mActivityRecord;
            if (activityRecord3 != null) {
                activityRecord3.setCurrentLaunchCanTurnScreenOn(false);
            }
        }
        if (wasVisible) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Already visible and does not turn on screen, skip preparing: " + this);
                return;
            }
            return;
        }
        if ((this.mAttrs.softInputMode & FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED) == 16) {
            this.mLayoutNeeded = true;
        }
        if (isDrawn() && this.mToken.okToAnimate()) {
            this.mWinAnimator.applyEnterAnimationLocked();
        }
    }

    private Configuration getProcessGlobalConfiguration() {
        WindowState parentWindow = getParentWindow();
        int pid = (parentWindow != null ? parentWindow.mSession : this.mSession).mPid;
        Configuration processConfig = this.mWmService.mAtmService.getGlobalConfigurationForPid(pid);
        return processConfig;
    }

    private Configuration getLastReportedConfiguration() {
        return this.mLastReportedConfiguration.getMergedConfiguration();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getLastReportedBounds() {
        Rect bounds = getLastReportedConfiguration().windowConfiguration.getBounds();
        return !bounds.isEmpty() ? bounds : getBounds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustStartingWindowFlags() {
        ActivityRecord activityRecord;
        if (this.mAttrs.type == 1 && (activityRecord = this.mActivityRecord) != null && activityRecord.mStartingWindow != null) {
            WindowManager.LayoutParams sa = this.mActivityRecord.mStartingWindow.mAttrs;
            sa.flags = (sa.flags & (-4718594)) | (this.mAttrs.flags & 4718593);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowScale(int requestedWidth, int requestedHeight) {
        float f;
        boolean scaledWindow = (this.mAttrs.flags & 16384) != 0;
        float f2 = 1.0f;
        if (scaledWindow) {
            if (this.mAttrs.width == requestedWidth) {
                f = 1.0f;
            } else {
                f = this.mAttrs.width / requestedWidth;
            }
            this.mHScale = f;
            if (this.mAttrs.height != requestedHeight) {
                f2 = this.mAttrs.height / requestedHeight;
            }
            this.mVScale = f2;
            return;
        }
        this.mVScale = 1.0f;
        this.mHScale = 1.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                synchronized (WindowState.this.mWmService.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState win = WindowState.this.mWmService.windowForClientLocked(WindowState.this.mSession, WindowState.this.mClient, false);
                    Slog.i("WindowManager", "WIN DEATH: " + win);
                    if (win != null) {
                        WindowState.this.getDisplayContent();
                        if (win.mActivityRecord != null && win.mActivityRecord.findMainWindow() == win) {
                            WindowState.this.mWmService.mTaskSnapshotController.onAppDied(win.mActivityRecord);
                        }
                        win.removeIfPossible(WindowState.this.shouldKeepVisibleDeadAppWindow());
                    } else if (WindowState.this.mHasSurface) {
                        Slog.e("WindowManager", "!!! LEAK !!! Window removed but surface still valid.");
                        WindowState.this.removeIfPossible();
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldKeepVisibleDeadAppWindow() {
        ActivityRecord activityRecord;
        if (isWinVisibleLw() && (activityRecord = this.mActivityRecord) != null && activityRecord.isClientVisible() && this.mAttrs.token == this.mClient.asBinder() && this.mAttrs.type != 3) {
            return getWindowConfiguration().keepVisibleDeadAppWindowOnScreen();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canReceiveKeys() {
        return canReceiveKeys(false);
    }

    public String canReceiveKeysReason(boolean fromUserTouch) {
        StringBuilder append = new StringBuilder().append("fromTouch= ").append(fromUserTouch).append(" isVisibleRequestedOrAdding=").append(isVisibleRequestedOrAdding()).append(" mViewVisibility=").append(this.mViewVisibility).append(" mRemoveOnExit=").append(this.mRemoveOnExit).append(" flags=").append(this.mAttrs.flags).append(" appWindowsAreFocusable=");
        ActivityRecord activityRecord = this.mActivityRecord;
        return append.append(activityRecord == null || activityRecord.windowsAreFocusable(fromUserTouch)).append(" canReceiveTouchInput=").append(canReceiveTouchInput()).append(" displayIsOnTop=").append(getDisplayContent().isOnTop()).append(" displayIsTrusted=").append(getDisplayContent().isTrusted()).toString();
    }

    public boolean canReceiveKeys(boolean fromUserTouch) {
        ActivityRecord activityRecord;
        boolean canReceiveKeys = isVisibleRequestedOrAdding() && this.mViewVisibility == 0 && !this.mRemoveOnExit && (this.mAttrs.flags & 8) == 0 && ((activityRecord = this.mActivityRecord) == null || activityRecord.windowsAreFocusable(fromUserTouch)) && canReceiveTouchInput();
        if (canReceiveKeys) {
            return fromUserTouch || getDisplayContent().isOnTop() || getDisplayContent().isTrusted();
        }
        return false;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public boolean canShowWhenLocked() {
        ActivityRecord activityRecord = this.mActivityRecord;
        boolean showBecauseOfActivity = activityRecord != null && activityRecord.canShowWhenLocked();
        boolean showBecauseOfWindow = (getAttrs().flags & 524288) != 0;
        return showBecauseOfActivity || showBecauseOfWindow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canReceiveTouchInput() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord == null || activityRecord.getTask() == null) {
            return true;
        }
        return !this.mActivityRecord.getTask().getRootTask().shouldIgnoreInput() && this.mActivityRecord.mVisibleRequested;
    }

    @Deprecated
    public boolean hasDrawn() {
        return this.mWinAnimator.mDrawState == 4;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public boolean showLw(boolean doAnimation) {
        return show(doAnimation, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean show(boolean doAnimation, boolean requestAnim) {
        DisplayContent displayContent;
        if ((isLegacyPolicyVisibility() && this.mLegacyPolicyVisibilityAfterAnim) || !showToCurrentUser() || !this.mAppOpVisibility || this.mPermanentlyHidden || this.mHiddenWhileSuspended || this.mForceHideNonSystemOverlayWindow) {
            return false;
        }
        if (this.mAttrs.type == 2040 && (this.mAttrs.flags & 1048576) != 0 && ITranWindowState.Instance().isAodWallpaperFeatureEnabled() && (displayContent = getDisplayContent()) != null) {
            displayContent.pendingLayoutChanges |= 4;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Policy visibility true: " + this);
        }
        if (doAnimation) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "doAnimation: mPolicyVisibility=" + isLegacyPolicyVisibility() + " animating=" + isAnimating(3));
            }
            if (!this.mToken.okToAnimate()) {
                doAnimation = false;
            } else if (isLegacyPolicyVisibility() && !isAnimating(3)) {
                doAnimation = false;
            }
        }
        setPolicyVisibilityFlag(1);
        this.mLegacyPolicyVisibilityAfterAnim = true;
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(1, true);
        }
        if (requestAnim) {
            this.mWmService.scheduleAnimationLocked();
        }
        if ((this.mAttrs.flags & 8) == 0) {
            this.mWmService.updateFocusedWindowLocked(0, false);
        }
        return true;
    }

    public boolean dreamAnimationHideLw() {
        return hideWithDreamAnimation();
    }

    private boolean hideWithDreamAnimation() {
        if (this.mToken.okToAnimate()) {
            boolean current = this.mLegacyPolicyVisibilityAfterAnim;
            if (current) {
                this.mHideByDreamAnimationState = 2;
                int animationType = ITranWindowState.Instance().getAnimationTypeNormal();
                if (this.mAttrs.type == 2040) {
                    animationType = ITranWindowState.Instance().getAnimationTypeDreamKeyguard();
                } else if (this.mAttrs.type == 2044) {
                    animationType = ITranWindowState.Instance().getAnimationTypeDreamAod();
                }
                this.mWinAnimator.applyAnimationLocked(2, false, animationType);
                this.mWmService.scheduleAnimationLocked();
                Slog.i("WindowManager", "  hideWithDreamAnimation on win=" + this);
                return true;
            }
            return false;
        }
        return false;
    }

    public void resetWithDreamAnimation() {
        if (this.mHideByDreamAnimationState == 2) {
            this.mWinAnimator.resetLayerLocked();
            this.mHideByDreamAnimationState = 1;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowState
    public boolean hideLw(boolean doAnimation) {
        return hide(doAnimation, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hide(boolean doAnimation, boolean requestAnim) {
        DisplayContent displayContent;
        if (doAnimation && !this.mToken.okToAnimate()) {
            doAnimation = false;
        }
        boolean current = doAnimation ? this.mLegacyPolicyVisibilityAfterAnim : isLegacyPolicyVisibility();
        if (!current) {
            return false;
        }
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (!isAnimating(3)) {
                doAnimation = false;
            }
        }
        this.mLegacyPolicyVisibilityAfterAnim = false;
        boolean isFocused = isFocused();
        if (!doAnimation) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Policy visibility false: " + this);
            }
            clearPolicyVisibilityFlag(1);
            this.mWmService.enableScreenIfNeededLocked();
            if (isFocused) {
                if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 1288731814, 0, (String) null, (Object[]) null);
                }
                this.mWmService.mFocusMayChange = true;
            }
        }
        if (requestAnim) {
            this.mWmService.scheduleAnimationLocked();
        }
        if (isFocused) {
            this.mWmService.updateFocusedWindowLocked(0, false);
        }
        if ((this.mAttrs.type == 2040 || this.mIsDreamAnimationWindow) && (this.mAttrs.flags & 1048576) != 0 && ITranWindowState.Instance().isAodWallpaperFeatureEnabled() && (displayContent = getDisplayContent()) != null) {
            displayContent.pendingLayoutChanges |= 4;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceHideNonSystemOverlayWindowIfNeeded(boolean forceHide) {
        if (!this.mSession.mCanAddInternalSystemWindow) {
            if (!WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) && this.mAttrs.type != 2005) {
                return;
            }
            if (TextUtils.equals(getOwningPackage(), "com.sh.smart.caller") && TextUtils.equals(this.mAttrs.getTitle(), "InCall Heads Up")) {
                return;
            }
            if ((this.mAttrs.type == 2038 && this.mAttrs.isSystemApplicationOverlay() && this.mSession.mCanCreateSystemApplicationOverlay) || this.mForceHideNonSystemOverlayWindow == forceHide) {
                return;
            }
            this.mForceHideNonSystemOverlayWindow = forceHide;
            if (forceHide) {
                hide(true, true);
            } else {
                show(true, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHiddenWhileSuspended(boolean hide) {
        if (!this.mOwnerCanAddInternalSystemWindow) {
            if ((!WindowManager.LayoutParams.isSystemAlertWindowType(this.mAttrs.type) && this.mAttrs.type != 2005) || this.mHiddenWhileSuspended == hide) {
                return;
            }
            this.mHiddenWhileSuspended = hide;
            if (hide) {
                hide(true, true);
            } else {
                show(true, true);
            }
        }
    }

    private void setAppOpVisibilityLw(boolean state) {
        if (this.mAppOpVisibility != state) {
            this.mAppOpVisibility = state;
            if (state) {
                show(true, true);
            } else {
                hide(true, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initAppOpsState() {
        int mode;
        if (this.mAppOp != -1 && this.mAppOpVisibility && (mode = this.mWmService.mAppOps.startOpNoThrow(this.mAppOp, getOwningUid(), getOwningPackage(), true, null, "init-default-visibility")) != 0 && mode != 3) {
            setAppOpVisibilityLw(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetAppOpsState() {
        if (this.mAppOp != -1 && this.mAppOpVisibility) {
            this.mWmService.mAppOps.finishOp(this.mAppOp, getOwningUid(), getOwningPackage(), (String) null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAppOpsState() {
        if (this.mAppOp == -1) {
            return;
        }
        int uid = getOwningUid();
        String packageName = getOwningPackage();
        if (this.mAppOpVisibility) {
            int mode = this.mWmService.mAppOps.checkOpNoThrow(this.mAppOp, uid, packageName);
            if (mode != 0 && mode != 3) {
                this.mWmService.mAppOps.finishOp(this.mAppOp, uid, packageName, (String) null);
                setAppOpVisibilityLw(false);
                return;
            }
            return;
        }
        int mode2 = this.mWmService.mAppOps.startOpNoThrow(this.mAppOp, uid, packageName, true, null, "attempt-to-be-visible");
        if (mode2 == 0 || mode2 == 3) {
            setAppOpVisibilityLw(true);
        }
    }

    public void hidePermanentlyLw() {
        if (!this.mPermanentlyHidden) {
            this.mPermanentlyHidden = true;
            hide(true, true);
        }
    }

    public void pokeDrawLockLw(long timeout) {
        if (isVisibleRequestedOrAdding()) {
            if (this.mDrawLock == null) {
                CharSequence tag = getWindowTag();
                PowerManager.WakeLock newWakeLock = this.mWmService.mPowerManager.newWakeLock(128, "Window:" + ((Object) tag));
                this.mDrawLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
                this.mDrawLock.setWorkSource(new WorkSource(this.mOwnerUid, this.mAttrs.packageName));
            }
            if (WindowManagerDebugConfig.DEBUG_POWER) {
                Slog.d("WindowManager", "pokeDrawLock: poking draw lock on behalf of visible window owned by " + this.mAttrs.packageName);
            }
            this.mDrawLock.acquire(timeout);
        } else if (WindowManagerDebugConfig.DEBUG_POWER) {
            Slog.d("WindowManager", "pokeDrawLock: suppressed draw lock request for invisible window owned by " + this.mAttrs.packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAlive() {
        return this.mClient.asBinder().isBinderAlive();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClosing() {
        ActivityRecord activityRecord;
        return this.mAnimatingExit || ((activityRecord = this.mActivityRecord) != null && activityRecord.isClosingOrEnteringPip());
    }

    @Override // com.android.server.wm.WindowContainer
    void sendAppVisibilityToClients() {
        super.sendAppVisibilityToClients();
        boolean clientVisible = this.mToken.isClientVisible();
        if (this.mAttrs.type == 3 && !clientVisible) {
            return;
        }
        try {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v("WindowManager", "Setting visibility of " + this + ": " + clientVisible);
            }
            this.mClient.dispatchAppVisibility(clientVisible);
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Exception thrown during dispatchAppVisibility " + this, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartFreezingScreen() {
        this.mAppFreezing = true;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.onStartFreezingScreen();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onStopFreezingScreen() {
        boolean unfrozeWindows = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            unfrozeWindows |= c.onStopFreezingScreen();
        }
        if (!this.mAppFreezing) {
            return unfrozeWindows;
        }
        this.mAppFreezing = false;
        if (this.mHasSurface && !getOrientationChanging() && this.mWmService.mWindowsFreezingScreen != 2) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1747461042, 0, (String) null, new Object[]{protoLogParam0});
            }
            setOrientationChanging(true);
        }
        this.mLastFreezeDuration = 0;
        setDisplayLayoutNeeded();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean destroySurface(boolean cleanupOnResume, boolean appStopped) {
        boolean destroyedSomething = false;
        ArrayList<WindowState> childWindows = new ArrayList<>(this.mChildren);
        for (int i = childWindows.size() - 1; i >= 0; i--) {
            WindowState c = childWindows.get(i);
            destroyedSomething |= c.destroySurface(cleanupOnResume, appStopped);
        }
        if (!appStopped && !this.mWindowRemovalAllowed && !cleanupOnResume) {
            return destroyedSomething;
        }
        if (this.mDestroying) {
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam0 = String.valueOf(this);
                boolean protoLogParam2 = this.mWindowRemovalAllowed;
                boolean protoLogParam3 = this.mRemoveOnExit;
                ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1577579529, (int) AudioChannelMask.IN_6, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(appStopped), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3)});
            }
            if (!cleanupOnResume || this.mRemoveOnExit) {
                destroySurfaceUnchecked();
            }
            if (this.mRemoveOnExit) {
                removeImmediately();
            }
            if (cleanupOnResume) {
                requestUpdateWallpaperIfNeeded();
            }
            this.mDestroying = false;
            destroyedSomething = true;
            if (getDisplayContent().mAppTransition.isTransitionSet() && getDisplayContent().mOpeningApps.contains(this.mActivityRecord)) {
                this.mWmService.mWindowPlacerLocked.requestTraversal();
            }
        }
        return destroyedSomething;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySurfaceUnchecked() {
        this.mWinAnimator.destroySurfaceLocked(this.mTmpTransaction);
        this.mTmpTransaction.apply();
        this.mAnimatingExit = false;
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, -2052051397, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mReportOrientationChanged = false;
        if (useBLASTSync()) {
            immediatelyNotifyBlastSync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSurfaceShownChanged(boolean shown) {
        if (this.mLastShownChangedReported == shown) {
            return;
        }
        this.mLastShownChangedReported = shown;
        if (shown) {
            initExclusionRestrictions();
        } else {
            logExclusionRestrictions(0);
            logExclusionRestrictions(1);
        }
        if (this.mAttrs.type >= 2000 && this.mAttrs.type != 2005 && this.mAttrs.type != 2030) {
            this.mWmService.mAtmService.mActiveUids.onNonAppSurfaceVisibilityChanged(this.mOwnerUid, shown);
        }
    }

    private void logExclusionRestrictions(int side) {
        if (!DisplayContent.logsGestureExclusionRestrictions(this) || SystemClock.uptimeMillis() < this.mLastExclusionLogUptimeMillis[side] + this.mWmService.mConstants.mSystemGestureExclusionLogDebounceTimeoutMillis) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long[] jArr = this.mLastExclusionLogUptimeMillis;
        long duration = now - jArr[side];
        jArr[side] = now;
        int requested = this.mLastRequestedExclusionHeight[side];
        int granted = this.mLastGrantedExclusionHeight[side];
        FrameworkStatsLog.write((int) FrameworkStatsLog.EXCLUSION_RECT_STATE_CHANGED, this.mAttrs.packageName, requested, requested - granted, side + 1, getConfiguration().orientation == 2, false, (int) duration);
    }

    private void initExclusionRestrictions() {
        long now = SystemClock.uptimeMillis();
        long[] jArr = this.mLastExclusionLogUptimeMillis;
        jArr[0] = now;
        jArr[1] = now;
    }

    boolean showForAllUsers() {
        switch (this.mAttrs.type) {
            case 3:
            case 2000:
            case 2001:
            case 2002:
            case 2007:
            case 2008:
            case 2009:
            case 2017:
            case 2018:
            case 2019:
            case NotificationShellCmd.NOTIFICATION_ID /* 2020 */:
            case 2021:
            case 2022:
            case 2024:
            case 2026:
            case 2027:
            case 2030:
            case 2034:
            case 2037:
            case 2039:
            case 2040:
            case 2041:
                break;
            default:
                if ((this.mAttrs.privateFlags & 16) == 0) {
                    return false;
                }
                break;
        }
        return this.mOwnerCanAddInternalSystemWindow;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean showToCurrentUser() {
        ActivityRecord activityRecord;
        WindowState win = getTopParentWindow();
        return (win.mAttrs.type < 2000 && (activityRecord = win.mActivityRecord) != null && activityRecord.mShowForAllUsers && win.getFrame().left <= win.getDisplayFrame().left && win.getFrame().top <= win.getDisplayFrame().top && win.getFrame().right >= win.getDisplayFrame().right && win.getFrame().bottom >= win.getDisplayFrame().bottom) || win.showForAllUsers() || this.mWmService.isCurrentProfile(win.mShowUserId);
    }

    private static void applyInsets(Region outRegion, Rect frame, Rect inset) {
        outRegion.set(frame.left + inset.left, frame.top + inset.top, frame.right - inset.right, frame.bottom - inset.bottom);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getTouchableRegion(Region outRegion) {
        Rect frame = this.mWindowFrames.mFrame;
        switch (this.mTouchableInsets) {
            case 1:
                applyInsets(outRegion, frame, this.mGivenContentInsets);
                break;
            case 2:
                applyInsets(outRegion, frame, this.mGivenVisibleInsets);
                break;
            case 3:
                outRegion.set(this.mGivenTouchableRegion);
                if (frame.left != 0 || frame.top != 0) {
                    outRegion.translate(frame.left, frame.top);
                    break;
                }
                break;
            default:
                outRegion.set(frame);
                break;
        }
        cropRegionToRootTaskBoundsIfNeeded(outRegion);
        subtractTouchExcludeRegionIfNeeded(outRegion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getEffectiveTouchableRegion(Region outRegion) {
        DisplayContent dc = getDisplayContent();
        if (this.mAttrs.isModal() && dc != null) {
            outRegion.set(dc.getBounds());
            cropRegionToRootTaskBoundsIfNeeded(outRegion);
            subtractTouchExcludeRegionIfNeeded(outRegion);
            return;
        }
        getTouchableRegion(outRegion);
    }

    private void cropRegionToRootTaskBoundsIfNeeded(Region region) {
        Task rootTask;
        Task task = getTask();
        if (task == null || !task.cropWindowsToRootTaskBounds() || (rootTask = task.getRootTask()) == null || rootTask.mCreatedByOrganizer) {
            return;
        }
        rootTask.getDimBounds(this.mTmpRect);
        adjustRegionInFreefromWindowMode(this.mTmpRect);
        region.op(this.mTmpRect, Region.Op.INTERSECT);
    }

    private void subtractTouchExcludeRegionIfNeeded(Region touchableRegion) {
        if (this.mTapExcludeRegion.isEmpty()) {
            return;
        }
        Region touchExcludeRegion = Region.obtain();
        getTapExcludeRegion(touchExcludeRegion);
        if (!touchExcludeRegion.isEmpty()) {
            touchableRegion.op(touchExcludeRegion, Region.Op.DIFFERENCE);
        }
        touchExcludeRegion.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportFocusChangedSerialized(boolean focused) {
        RemoteCallbackList<IWindowFocusObserver> remoteCallbackList = this.mFocusCallbacks;
        if (remoteCallbackList != null) {
            int N = remoteCallbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IWindowFocusObserver obs = this.mFocusCallbacks.getBroadcastItem(i);
                if (focused) {
                    try {
                        obs.focusGained(this.mWindowId.asBinder());
                    } catch (RemoteException e) {
                    }
                } else {
                    obs.focusLost(this.mWindowId.asBinder());
                }
            }
            this.mFocusCallbacks.finishBroadcast();
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public Configuration getConfiguration() {
        if (!registeredForDisplayAreaConfigChanges()) {
            return super.getConfiguration();
        }
        this.mTempConfiguration.setTo(getProcessGlobalConfiguration());
        this.mTempConfiguration.updateFrom(getMergedOverrideConfiguration());
        return this.mTempConfiguration;
    }

    private boolean registeredForDisplayAreaConfigChanges() {
        WindowProcessController wpc;
        WindowState parentWindow = getParentWindow();
        if (parentWindow != null) {
            wpc = parentWindow.mWpcForDisplayAreaConfigChanges;
        } else {
            wpc = this.mWpcForDisplayAreaConfigChanges;
        }
        return wpc != null && wpc.registeredForDisplayAreaConfigChanges();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fillClientWindowFramesAndConfiguration(ClientWindowFrames outFrames, MergedConfiguration outMergedConfiguration, boolean useLatestConfig, boolean relayoutVisible) {
        outFrames.frame.set(this.mWindowFrames.mCompatFrame);
        outFrames.displayFrame.set(this.mWindowFrames.mDisplayFrame);
        if (this.mInvGlobalScale != 1.0f && hasCompatScale()) {
            outFrames.displayFrame.scale(this.mInvGlobalScale);
        }
        if (useLatestConfig || (relayoutVisible && (shouldCheckTokenVisibleRequested() || this.mToken.isVisibleRequested()))) {
            Configuration globalConfig = getProcessGlobalConfiguration();
            Configuration overrideConfig = getMergedOverrideConfiguration();
            if (!this.mLastReportedConfiguration.getGlobalConfiguration().equals(globalConfig) || !this.mLastReportedConfiguration.getOverrideConfiguration().equals(overrideConfig)) {
                TranFoldWMCustody.instance().updateLastReportedConfiguration(this, outFrames, this.mLastReportedConfiguration, globalConfig, overrideConfig, useLatestConfig, relayoutVisible, shouldSendRedrawForSync());
            }
            outMergedConfiguration.setConfiguration(globalConfig, overrideConfig);
            MergedConfiguration mergedConfiguration = this.mLastReportedConfiguration;
            if (outMergedConfiguration != mergedConfiguration) {
                mergedConfiguration.setTo(outMergedConfiguration);
            }
        } else {
            outMergedConfiguration.setTo(this.mLastReportedConfiguration);
        }
        this.mLastConfigReportedToClient = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't wrap try/catch for region: R(33:12|(1:14)|15|(1:17)|18|(1:20)(1:89)|21|(1:25)|26|(1:28)(1:88)|29|(1:87)(1:32)|33|(1:86)(1:37)|38|(1:85)(1:42)|43|(2:47|(1:51))|(1:53)|54|(13:56|57|59|60|61|62|63|64|(2:69|(1:71))|72|(1:74)|76|77)|84|59|60|61|62|63|64|(3:67|69|(0))|72|(0)|76|77) */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01d0, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01d2, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x01d3, code lost:
        r9 = "WindowManager";
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x01d9, code lost:
        setOrientationChanging(false);
        r31.mLastFreezeDuration = (int) (android.os.SystemClock.elapsedRealtime() - r31.mWmService.mDisplayFreezeTime);
        android.util.Slog.w(r9, "Failed to report 'resized' to " + r31 + " due to " + r0);
     */
    /* JADX WARN: Removed duplicated region for block: B:79:0x01aa A[Catch: RemoteException -> 0x01d0, TryCatch #0 {RemoteException -> 0x01d0, blocks: (B:72:0x0195, B:75:0x019c, B:77:0x01a0, B:79:0x01aa, B:80:0x01ba, B:82:0x01c4), top: B:91:0x0195 }] */
    /* JADX WARN: Removed duplicated region for block: B:82:0x01c4 A[Catch: RemoteException -> 0x01d0, TRY_LEAVE, TryCatch #0 {RemoteException -> 0x01d0, blocks: (B:72:0x0195, B:75:0x019c, B:77:0x01a0, B:79:0x01aa, B:80:0x01ba, B:82:0x01c4), top: B:91:0x0195 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void reportResized() {
        int lastReportedWidth;
        int lastReportedHeight;
        boolean alwaysConsumeSystemBars;
        int resizeMode;
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.isRelaunching()) {
            return;
        }
        if (shouldCheckTokenVisibleRequested() && !this.mToken.isVisibleRequested()) {
            return;
        }
        if (Trace.isTagEnabled(32L)) {
            Trace.traceBegin(32L, "wm.reportResized_" + ((Object) getWindowTag()));
        }
        if (ProtoLogCache.WM_DEBUG_RESIZE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(this.mWindowFrames.mCompatFrame);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_RESIZE, -1824578273, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        boolean drawPending = this.mWinAnimator.mDrawState == 1;
        if (drawPending && ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam02 = String.valueOf(this);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1130868271, 0, (String) null, new Object[]{protoLogParam02});
        }
        boolean reportOrientation = this.mReportOrientationChanged;
        this.mReportOrientationChanged = false;
        this.mDragResizingChangeReported = true;
        this.mWindowFrames.clearReportResizeHints();
        if (!this.mIsWallpaper) {
            lastReportedWidth = 0;
            lastReportedHeight = 0;
        } else {
            Rect lastReportedBounds = getLastReportedBounds();
            int lastReportedWidth2 = lastReportedBounds.width();
            int lastReportedHeight2 = lastReportedBounds.height();
            lastReportedWidth = lastReportedWidth2;
            lastReportedHeight = lastReportedHeight2;
        }
        fillClientWindowFramesAndConfiguration(this.mClientWindowFrames, this.mLastReportedConfiguration, true, false);
        boolean syncRedraw = shouldSendRedrawForSync();
        boolean reportDraw = syncRedraw || drawPending;
        boolean isDragResizeChanged = isDragResizeChanged();
        boolean forceRelayout = syncRedraw || reportOrientation || isDragResizeChanged;
        DisplayContent displayContent = getDisplayContent();
        boolean alwaysConsumeSystemBars2 = displayContent.getDisplayPolicy().areSystemBarsForcedShownLw();
        if (alwaysConsumeSystemBars2 && isNoNeedSetAlwaysCustomSystemBars(this, this.mActivityRecord, displayContent)) {
            alwaysConsumeSystemBars = false;
        } else {
            alwaysConsumeSystemBars = alwaysConsumeSystemBars2;
        }
        int displayId = displayContent.getDisplayId();
        if (this.mIsWallpaper && (displayContent.pendingLayoutChanges & 4) == 0) {
            Rect lastReportedBounds2 = getLastReportedBounds();
            if (lastReportedWidth != lastReportedBounds2.width() || lastReportedHeight != lastReportedBounds2.height()) {
                Slog.d("WindowManager", String.format("need update wallpaper when config bounds changed from (%d, %d) to (%d, %d) in %s.", Integer.valueOf(lastReportedWidth), Integer.valueOf(lastReportedHeight), Integer.valueOf(lastReportedBounds2.width()), Integer.valueOf(lastReportedBounds2.height()), getName()));
                displayContent.pendingLayoutChanges |= 4;
            }
        }
        if (isDragResizeChanged) {
            setDragResizing();
        }
        if (isDragResizing()) {
            switch (getResizeMode()) {
                case 0:
                    resizeMode = 0;
                    break;
                case 1:
                    resizeMode = 1;
                    break;
            }
            markRedrawForSyncReported();
            this.mClient.resized(this.mClientWindowFrames, reportDraw, this.mLastReportedConfiguration, getCompatInsetsState(), forceRelayout, alwaysConsumeSystemBars, displayId, this.mSyncSeqId, resizeMode);
            String str = "WindowManager";
            TranFoldWMCustody.instance().resized(this, reportDraw, forceRelayout, displayContent, this.mClientWindowFrames, this.mSyncSeqId);
            if (drawPending && reportOrientation && this.mOrientationChanging) {
                this.mOrientationChangeRedrawRequestTime = SystemClock.elapsedRealtime();
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam03 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -567946587, 0, (String) null, new Object[]{protoLogParam03});
                }
            }
            if (this.mWmService.mAccessibilityController.hasCallbacks()) {
                this.mWmService.mAccessibilityController.onSomeWindowResizedOrMoved(displayId);
            }
            Trace.traceEnd(32L);
        }
        resizeMode = -1;
        markRedrawForSyncReported();
        this.mClient.resized(this.mClientWindowFrames, reportDraw, this.mLastReportedConfiguration, getCompatInsetsState(), forceRelayout, alwaysConsumeSystemBars, displayId, this.mSyncSeqId, resizeMode);
        String str2 = "WindowManager";
        TranFoldWMCustody.instance().resized(this, reportDraw, forceRelayout, displayContent, this.mClientWindowFrames, this.mSyncSeqId);
        if (drawPending) {
            this.mOrientationChangeRedrawRequestTime = SystemClock.elapsedRealtime();
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            }
        }
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
        }
        Trace.traceEnd(32L);
    }

    boolean isClientLocal() {
        return this.mClient instanceof IWindow.Stub;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInsetsChanged() {
        if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, 1047505501, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mWindowFrames.setInsetsChanged(true);
        this.mWmService.mWindowsInsetsChanged++;
        this.mWmService.mH.removeMessages(66);
        this.mWmService.mH.sendEmptyMessage(66);
        WindowContainer p = getParent();
        if (p != null) {
            p.updateOverlayInsetsState(this);
        }
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public void notifyInsetsControlChanged() {
        if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, 1030898920, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (this.mAppDied || this.mRemoved) {
            return;
        }
        InsetsStateController stateController = getDisplayContent().getInsetsStateController();
        try {
            this.mClient.insetsControlChanged(getCompatInsetsState(), stateController.getControlsForDispatch(this));
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Failed to deliver inset state change to w=" + this, e);
        }
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public WindowState getWindow() {
        return this;
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public void showInsets(int types, boolean fromIme) {
        try {
            this.mClient.showInsets(types, fromIme);
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Failed to deliver showInsets", e);
        }
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public void hideInsets(int types, boolean fromIme) {
        try {
            this.mClient.hideInsets(types, fromIme);
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Failed to deliver showInsets", e);
        }
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public boolean canShowTransient() {
        return (this.mAttrs.insetsFlags.behavior & 2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeHiddenByKeyguard() {
        if (this.mActivityRecord != null) {
            return false;
        }
        switch (this.mAttrs.type) {
            case 2000:
            case 2013:
            case 2019:
            case 2040:
                return false;
            default:
                return this.mPolicy.getWindowLayerLw(this) < this.mPolicy.getWindowLayerFromTypeLw(2040);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeHiddenByAOD() {
        if (this.mActivityRecord != null) {
            return false;
        }
        switch (this.mAttrs.type) {
            case 2013:
            case 2044:
                return false;
            default:
                return this.mPolicy.getWindowLayerLw(this) < this.mPolicy.getWindowLayerFromTypeLw(2044);
        }
    }

    private int getRootTaskId() {
        Task rootTask = getRootTask();
        if (rootTask == null) {
            return -1;
        }
        return rootTask.mTaskId;
    }

    public void registerFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFocusCallbacks == null) {
                    this.mFocusCallbacks = new RemoteCallbackList<>();
                }
                this.mFocusCallbacks.register(observer);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void unregisterFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                RemoteCallbackList<IWindowFocusObserver> remoteCallbackList = this.mFocusCallbacks;
                if (remoteCallbackList != null) {
                    remoteCallbackList.unregister(observer);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocused() {
        return getDisplayContent().mCurrentFocus == this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areAppWindowBoundsLetterboxed() {
        ActivityRecord activityRecord = this.mActivityRecord;
        return activityRecord != null && (activityRecord.areBoundsLetterboxed() || isLetterboxedForDisplayCutout());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLetterboxedForDisplayCutout() {
        if (this.mActivityRecord != null && this.mWindowFrames.parentFrameWasClippedByDisplayCutout() && this.mAttrs.layoutInDisplayCutoutMode != 3 && this.mAttrs.isFullscreen()) {
            return !frameCoversEntireAppTokenBounds();
        }
        return false;
    }

    private boolean frameCoversEntireAppTokenBounds() {
        this.mTmpRect.set(this.mActivityRecord.getBounds());
        this.mTmpRect.intersectUnchecked(this.mWindowFrames.mFrame);
        return this.mActivityRecord.getBounds().equals(this.mTmpRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullyTransparentBarAllowed(Rect frame) {
        ActivityRecord activityRecord = this.mActivityRecord;
        return activityRecord == null || activityRecord.isFullyTransparentBarAllowed(frame);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDragResizeChanged() {
        return this.mDragResizing != computeDragResizing();
    }

    @Override // com.android.server.wm.WindowContainer
    void setWaitingForDrawnIfResizingChanged() {
        if (isDragResizeChanged()) {
            this.mWmService.mRoot.mWaitingForDrawn.add(this);
        }
        super.setWaitingForDrawnIfResizingChanged();
    }

    private boolean isDragResizingChangeReported() {
        return this.mDragResizingChangeReported;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void resetDragResizingChangeReported() {
        this.mDragResizingChangeReported = false;
        super.resetDragResizingChangeReported();
    }

    int getResizeMode() {
        return this.mResizeMode;
    }

    private boolean computeDragResizing() {
        Task task = getTask();
        if (task == null) {
            return false;
        }
        if ((!inFreeformWindowingMode() && !task.getRootTask().mCreatedByOrganizer) || task.getActivityType() == 2 || this.mAttrs.width != -1 || this.mAttrs.height != -1) {
            return false;
        }
        if (task.isDragResizing()) {
            return true;
        }
        return (!getDisplayContent().mDividerControllerLocked.isResizing() || task.inFreeformWindowingMode() || isGoneForLayout()) ? false : true;
    }

    void setDragResizing() {
        int i;
        boolean resizing = computeDragResizing();
        if (resizing == this.mDragResizing) {
            return;
        }
        this.mDragResizing = resizing;
        Task task = getTask();
        if (task != null && task.isDragResizing()) {
            this.mResizeMode = task.getDragResizeMode();
            return;
        }
        if (this.mDragResizing && getDisplayContent().mDividerControllerLocked.isResizing()) {
            i = 1;
        } else {
            i = 0;
        }
        this.mResizeMode = i;
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    boolean isDockedResizing() {
        if (this.mDragResizing && getResizeMode() == 1) {
            return true;
        }
        return isChildWindow() && getParentWindow().isDockedResizing();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        boolean isVisible = isVisible();
        if (logLevel == 2 && !isVisible) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        proto.write(1120986464259L, getDisplayId());
        proto.write(1120986464260L, getRootTaskId());
        this.mAttrs.dumpDebug(proto, 1146756268037L);
        this.mGivenContentInsets.dumpDebug(proto, 1146756268038L);
        this.mWindowFrames.dumpDebug(proto, 1146756268073L);
        this.mAttrs.surfaceInsets.dumpDebug(proto, 1146756268044L);
        GraphicsProtos.dumpPointProto(this.mSurfacePosition, proto, 1146756268048L);
        this.mWinAnimator.dumpDebug(proto, 1146756268045L);
        proto.write(1133871366158L, this.mAnimatingExit);
        proto.write(1120986464274L, this.mRequestedWidth);
        proto.write(1120986464275L, this.mRequestedHeight);
        proto.write(1120986464276L, this.mViewVisibility);
        proto.write(1133871366166L, this.mHasSurface);
        proto.write(1133871366167L, isReadyForDisplay());
        proto.write(1133871366178L, this.mRemoveOnExit);
        proto.write(1133871366179L, this.mDestroying);
        proto.write(1133871366180L, this.mRemoved);
        proto.write(1133871366181L, isOnScreen());
        proto.write(1133871366182L, isVisible);
        proto.write(1133871366183L, this.mPendingSeamlessRotate != null);
        proto.write(1133871366186L, this.mForceSeamlesslyRotate);
        proto.write(1133871366187L, hasCompatScale());
        proto.write(1108101562412L, this.mGlobalScale);
        for (Rect r : this.mKeepClearAreas) {
            r.dumpDebug(proto, 2246267895853L);
        }
        for (Rect r2 : this.mUnrestrictedKeepClearAreas) {
            r2.dumpDebug(proto, 2246267895854L);
        }
        proto.end(token);
    }

    @Override // com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268040L;
    }

    @Override // com.android.server.wm.WindowContainer
    public void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
        proto.write(1120986464258L, this.mShowUserId);
        CharSequence title = getWindowTag();
        if (title != null) {
            proto.write(1138166333443L, title.toString());
        }
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    @NeverCompile
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        pw.print(prefix + "mDisplayId=" + getDisplayId());
        if (getRootTask() != null) {
            pw.print(" rootTaskId=" + getRootTaskId());
        }
        pw.println(" mSession=" + this.mSession + " mClient=" + this.mClient.asBinder());
        pw.println(prefix + "mOwnerUid=" + this.mOwnerUid + " showForAllUsers=" + showForAllUsers() + " package=" + this.mAttrs.packageName + " appop=" + AppOpsManager.opToName(this.mAppOp));
        pw.println(prefix + "mAttrs=" + this.mAttrs.toString(prefix));
        pw.println(prefix + "Requested w=" + this.mRequestedWidth + " h=" + this.mRequestedHeight + " mLayoutSeq=" + this.mLayoutSeq);
        if (this.mRequestedWidth != this.mLastRequestedWidth || this.mRequestedHeight != this.mLastRequestedHeight) {
            pw.println(prefix + "LastRequested w=" + this.mLastRequestedWidth + " h=" + this.mLastRequestedHeight);
        }
        if (this.mIsChildWindow || this.mLayoutAttached) {
            pw.println(prefix + "mParentWindow=" + getParentWindow() + " mLayoutAttached=" + this.mLayoutAttached);
        }
        if (this.mIsImWindow || this.mIsWallpaper || this.mIsFloatingLayer) {
            pw.println(prefix + "mIsImWindow=" + this.mIsImWindow + " mIsWallpaper=" + this.mIsWallpaper + " mIsFloatingLayer=" + this.mIsFloatingLayer);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mBaseLayer=");
            pw.print(this.mBaseLayer);
            pw.print(" mSubLayer=");
            pw.print(this.mSubLayer);
        }
        if (dumpAll) {
            pw.println(prefix + "mToken=" + this.mToken);
            if (this.mActivityRecord != null) {
                pw.println(prefix + "mActivityRecord=" + this.mActivityRecord);
                pw.print(prefix + "mAppDied=" + this.mAppDied);
                pw.print(prefix + "drawnStateEvaluated=" + getDrawnStateEvaluated());
                pw.println(prefix + "mightAffectAllDrawn=" + mightAffectAllDrawn());
            }
            pw.println(prefix + "mViewVisibility=0x" + Integer.toHexString(this.mViewVisibility) + " mHaveFrame=" + this.mHaveFrame + " mObscured=" + this.mObscured);
            if (this.mDisableFlags != 0) {
                pw.println(prefix + "mDisableFlags=" + ViewDebug.flagsToString(View.class, "mSystemUiVisibility", this.mDisableFlags));
            }
        }
        if (!isVisibleByPolicy() || !this.mLegacyPolicyVisibilityAfterAnim || !this.mAppOpVisibility || isParentWindowHidden() || this.mPermanentlyHidden || this.mForceHideNonSystemOverlayWindow || this.mHiddenWhileSuspended) {
            pw.println(prefix + "mPolicyVisibility=" + isVisibleByPolicy() + " mLegacyPolicyVisibilityAfterAnim=" + this.mLegacyPolicyVisibilityAfterAnim + " mAppOpVisibility=" + this.mAppOpVisibility + " parentHidden=" + isParentWindowHidden() + " mPermanentlyHidden=" + this.mPermanentlyHidden + " mHiddenWhileSuspended=" + this.mHiddenWhileSuspended + " mForceHideNonSystemOverlayWindow=" + this.mForceHideNonSystemOverlayWindow);
        }
        if (!this.mRelayoutCalled || this.mLayoutNeeded) {
            pw.println(prefix + "mRelayoutCalled=" + this.mRelayoutCalled + " mLayoutNeeded=" + this.mLayoutNeeded);
        }
        if (dumpAll) {
            StringBuilder append = new StringBuilder().append(prefix).append("mGivenContentInsets=");
            Rect rect = this.mGivenContentInsets;
            StringBuilder sb = sTmpSB;
            pw.println(append.append(rect.toShortString(sb)).append(" mGivenVisibleInsets=").append(this.mGivenVisibleInsets.toShortString(sb)).toString());
            if (this.mTouchableInsets != 0 || this.mGivenInsetsPending) {
                pw.println(prefix + "mTouchableInsets=" + this.mTouchableInsets + " mGivenInsetsPending=" + this.mGivenInsetsPending);
                Region region = new Region();
                getTouchableRegion(region);
                pw.println(prefix + "touchable region=" + region);
            }
            pw.println(prefix + "mFullConfiguration=" + getConfiguration());
            pw.println(prefix + "mLastReportedConfiguration=" + getLastReportedConfiguration());
        }
        pw.println(prefix + "mHasSurface=" + this.mHasSurface + " isReadyForDisplay()=" + isReadyForDisplay() + " mWindowRemovalAllowed=" + this.mWindowRemovalAllowed);
        if (hasCompatScale()) {
            pw.println(prefix + "mCompatFrame=" + this.mWindowFrames.mCompatFrame.toShortString(sTmpSB));
        }
        if (dumpAll) {
            this.mWindowFrames.dump(pw, prefix);
            pw.println(prefix + " surface=" + this.mAttrs.surfaceInsets.toShortString(sTmpSB));
        }
        super.dump(pw, prefix, dumpAll);
        pw.println(prefix + this.mWinAnimator + ":");
        this.mWinAnimator.dump(pw, prefix + "  ", dumpAll);
        if (this.mAnimatingExit || this.mRemoveOnExit || this.mDestroying || this.mRemoved) {
            pw.println(prefix + "mAnimatingExit=" + this.mAnimatingExit + " mRemoveOnExit=" + this.mRemoveOnExit + " mDestroying=" + this.mDestroying + " mRemoved=" + this.mRemoved);
        }
        if (getOrientationChanging() || this.mAppFreezing || this.mReportOrientationChanged) {
            pw.println(prefix + "mOrientationChanging=" + this.mOrientationChanging + " configOrientationChanging=" + (getLastReportedConfiguration().orientation != getConfiguration().orientation) + " mAppFreezing=" + this.mAppFreezing + " mReportOrientationChanged=" + this.mReportOrientationChanged);
        }
        if (this.mLastFreezeDuration != 0) {
            pw.print(prefix + "mLastFreezeDuration=");
            TimeUtils.formatDuration(this.mLastFreezeDuration, pw);
            pw.println();
        }
        pw.print(prefix + "mForceSeamlesslyRotate=" + this.mForceSeamlesslyRotate + " seamlesslyRotate: pending=");
        SeamlessRotator seamlessRotator = this.mPendingSeamlessRotate;
        if (seamlessRotator != null) {
            seamlessRotator.dump(pw);
        } else {
            pw.print("null");
        }
        if (this.mHScale != 1.0f || this.mVScale != 1.0f) {
            pw.println(prefix + "mHScale=" + this.mHScale + " mVScale=" + this.mVScale);
        }
        if (this.mWallpaperX != -1.0f || this.mWallpaperY != -1.0f) {
            pw.println(prefix + "mWallpaperX=" + this.mWallpaperX + " mWallpaperY=" + this.mWallpaperY);
        }
        if (this.mWallpaperXStep != -1.0f || this.mWallpaperYStep != -1.0f) {
            pw.println(prefix + "mWallpaperXStep=" + this.mWallpaperXStep + " mWallpaperYStep=" + this.mWallpaperYStep);
        }
        if (this.mWallpaperZoomOut != -1.0f) {
            pw.println(prefix + "mWallpaperZoomOut=" + this.mWallpaperZoomOut);
        }
        if (this.mWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            pw.println(prefix + "mWallpaperDisplayOffsetX=" + this.mWallpaperDisplayOffsetX + " mWallpaperDisplayOffsetY=" + this.mWallpaperDisplayOffsetY);
        }
        if (this.mDrawLock != null) {
            pw.println(prefix + "mDrawLock=" + this.mDrawLock);
        }
        if (isDragResizing()) {
            pw.println(prefix + "isDragResizing=" + isDragResizing());
        }
        if (computeDragResizing()) {
            pw.println(prefix + "computeDragResizing=" + computeDragResizing());
        }
        pw.println(prefix + "isOnScreen=" + isOnScreen());
        pw.println(prefix + "isVisible=" + isVisible());
        pw.println(prefix + "keepClearAreas: restricted=" + this.mKeepClearAreas + ", unrestricted=" + this.mUnrestrictedKeepClearAreas);
        if (dumpAll) {
            String visibilityString = this.mRequestedVisibilities.toString();
            if (!visibilityString.isEmpty()) {
                pw.println(prefix + "Requested visibilities: " + visibilityString);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return Integer.toHexString(System.identityHashCode(this)) + " " + ((Object) getWindowTag());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CharSequence getWindowTag() {
        CharSequence tag = this.mAttrs.getTitle();
        if (tag == null || tag.length() <= 0) {
            return this.mAttrs.packageName;
        }
        return tag;
    }

    public String toString() {
        CharSequence title = getWindowTag();
        if (this.mStringNameCache == null || this.mLastTitle != title || this.mWasExiting != this.mAnimatingExit) {
            this.mLastTitle = title;
            this.mWasExiting = this.mAnimatingExit;
            this.mStringNameCache = "Window{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.mShowUserId + " " + ((Object) this.mLastTitle) + (this.mAnimatingExit ? " EXITING}" : "}");
        }
        return this.mStringNameCache;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isChildWindow() {
        return this.mIsChildWindow;
    }

    public float getLastHScale() {
        return this.mLastHScale;
    }

    public float getLastVScale() {
        return this.mLastVScale;
    }

    public Point getSurfacePositon() {
        return this.mSurfacePosition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hideNonSystemOverlayWindowsWhenVisible() {
        return (this.mAttrs.privateFlags & 524288) != 0 && this.mSession.mCanHideNonSystemOverlayWindows;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getParentWindow() {
        if (this.mIsChildWindow) {
            return (WindowState) super.getParent();
        }
        return null;
    }

    WindowState getTopParentWindow() {
        WindowState current = this;
        WindowState topParent = current;
        while (current != null && current.mIsChildWindow) {
            current = current.getParentWindow();
            if (current != null) {
                topParent = current;
            }
        }
        return topParent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isParentWindowHidden() {
        WindowState parent = getParentWindow();
        return parent != null && parent.mHidden;
    }

    private boolean isParentWindowGoneForLayout() {
        WindowState parent = getParentWindow();
        return parent != null && parent.isGoneForLayout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceWindow(boolean animate) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.setWillReplaceWindow(animate);
        }
        if ((this.mAttrs.privateFlags & 32768) != 0 || this.mAttrs.type == 3) {
            return;
        }
        this.mWillReplaceWindow = true;
        this.mReplacementWindow = null;
        this.mAnimateReplacingWindow = animate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearWillReplaceWindow() {
        this.mWillReplaceWindow = false;
        this.mReplacementWindow = null;
        this.mAnimateReplacingWindow = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.clearWillReplaceWindow();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean waitingForReplacement() {
        if (this.mWillReplaceWindow) {
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            if (c.waitingForReplacement()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestUpdateWallpaperIfNeeded() {
        DisplayContent dc = getDisplayContent();
        if (dc != null && hasWallpaper()) {
            dc.pendingLayoutChanges |= 4;
            dc.setLayoutNeeded();
            this.mWmService.mWindowPlacerLocked.requestTraversal();
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.requestUpdateWallpaperIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float translateToWindowX(float x) {
        float winX = x - this.mWindowFrames.mFrame.left;
        if (hasCompatScale()) {
            return winX * this.mGlobalScale;
        }
        return winX;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float translateToWindowY(float y) {
        float winY = y - this.mWindowFrames.mFrame.top;
        if (hasCompatScale()) {
            return winY * this.mGlobalScale;
        }
        return winY;
    }

    boolean shouldBeReplacedWithChildren() {
        return this.mIsChildWindow || this.mAttrs.type == 2 || this.mAttrs.type == 4;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceChildWindows() {
        if (shouldBeReplacedWithChildren()) {
            setWillReplaceWindow(false);
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.setWillReplaceChildWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getReplacingWindow() {
        if (this.mAnimatingExit && this.mWillReplaceWindow && this.mAnimateReplacingWindow) {
            return this;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            WindowState replacing = c.getReplacingWindow();
            if (replacing != null) {
                return replacing;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRotationAnimationHint() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            return activityRecord.mRotationAnimationHint;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPerformShowLogFlag(boolean flag) {
        this.mPerformShowLog = flag;
    }

    private void logPerformShowForAnr(String prefix) {
        boolean z = true;
        StringBuilder append = new StringBuilder().append(prefix).append(this).append(": mDrawState=").append(this.mWinAnimator.drawStateToString()).append(" starting=").append(this.mAttrs.type == 3).append(" during animation: policyVis=").append(isVisibleByPolicy()).append(" mToken.waitingToShow = ").append(this.mToken.waitingToShow).append(" appTransitionState = ").append(getDisplayContent().mAppTransition.getAppTransitionState()).append(" mAppTransition isTransitionSet = ").append(getDisplayContent().mAppTransition.isTransitionSet()).append(" parentHidden=").append(isParentWindowHidden()).append(" isVisible = ").append(this.mToken.isVisible()).append(" mViewVisibility = ").append(this.mViewVisibility).append(" tok.visibleRequested = ");
        ActivityRecord activityRecord = this.mActivityRecord;
        StringBuilder append2 = append.append(activityRecord != null && activityRecord.mVisibleRequested).append(" tok.visible= ");
        ActivityRecord activityRecord2 = this.mActivityRecord;
        StringBuilder append3 = append2.append(activityRecord2 != null && activityRecord2.isVisible()).append(" animating= ").append(isAnimating(3)).append(" mHasSurface= ").append(this.mHasSurface).append(" mDestroying= ").append(this.mDestroying).append(" tok animating= ");
        ActivityRecord activityRecord3 = this.mActivityRecord;
        if (activityRecord3 == null || !activityRecord3.isAnimating(3)) {
            z = false;
        }
        String logMessage = append3.append(z).toString();
        Slog.d("WindowManager", logMessage);
        Trace.traceBegin(32L, logMessage);
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean performShowLocked() {
        if (!showToCurrentUser()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w("WindowManager", "hiding " + this + ", belonging to " + this.mOwnerUid);
            }
            clearPolicyVisibilityFlag(2);
            return false;
        }
        logPerformShow("performShow on ");
        int drawState = this.mWinAnimator.mDrawState;
        if ((drawState == 4 || drawState == 3) && this.mActivityRecord != null) {
            if (this.mAttrs.type != 3) {
                this.mActivityRecord.onFirstWindowDrawn(this);
            } else {
                this.mActivityRecord.onStartingWindowDrawn();
            }
        }
        if (this.mWinAnimator.mDrawState != 3 || !isReadyForDisplay()) {
            if (DEBUG_ADB && this.mPerformShowLog) {
                try {
                    logPerformShowForAnr("Showing ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        logPerformShow("Showing ");
        this.mWmService.enableScreenIfNeededLocked();
        if (this.mAttrs.type != 2044) {
            this.mWinAnimator.applyEnterAnimationLocked();
        } else {
            this.mWinAnimator.applyEnterAnimationLocked(ITranWindowState.Instance().getAnimationTypeDreamAod());
            int dreamAnimation = ITranWindowState.Instance().getDreamAnimation();
            Slog.i("WindowManager", "  dreamAnimation=" + dreamAnimation);
            if (dreamAnimation == ITranWindowState.Instance().getAnimationFromKeyguardToAod()) {
                getDisplayContent().getDisplayPolicy().startKeyguardWindowAnimation(2, false, ITranWindowState.Instance().getAnimationTypeDreamKeyguard());
            }
            ITranWindowState.Instance().hookAodWindowFinishDraw();
        }
        if (this.mHideByDreamAnimationState == 2) {
            this.mWinAnimator.resetLayerLocked();
            this.mHideByDreamAnimationState = 1;
        }
        this.mWinAnimator.mLastAlpha = -1.0f;
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, -1288007399, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.v("WindowManager", "performShowLocked: mDrawState=HAS_DRAWN in " + this);
        }
        this.mWinAnimator.mDrawState = 4;
        this.mWmService.scheduleAnimationLocked();
        if (this.mHidden) {
            this.mHidden = false;
            DisplayContent displayContent = getDisplayContent();
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowState c = (WindowState) this.mChildren.get(i);
                if (c.mWinAnimator.mSurfaceController != null) {
                    c.performShowLocked();
                    if (displayContent != null) {
                        displayContent.setLayoutNeeded();
                    }
                }
            }
        }
        return true;
    }

    private void logPerformShow(String prefix) {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mAttrs.type == 3)) {
            boolean z = true;
            StringBuilder append = new StringBuilder().append(prefix).append(this).append(": mDrawState=").append(this.mWinAnimator.drawStateToString()).append(" readyForDisplay=").append(isReadyForDisplay()).append(" starting=").append(this.mAttrs.type == 3).append(" during animation: policyVis=").append(isVisibleByPolicy()).append(" parentHidden=").append(isParentWindowHidden()).append(" tok.visibleRequested=");
            ActivityRecord activityRecord = this.mActivityRecord;
            StringBuilder append2 = append.append(activityRecord != null && activityRecord.mVisibleRequested).append(" tok.visible=");
            ActivityRecord activityRecord2 = this.mActivityRecord;
            StringBuilder append3 = append2.append(activityRecord2 != null && activityRecord2.isVisible()).append(" animating=").append(isAnimating(3)).append(" tok animating=");
            ActivityRecord activityRecord3 = this.mActivityRecord;
            if (activityRecord3 == null || !activityRecord3.isAnimating(3)) {
                z = false;
            }
            Slog.v("WindowManager", append3.append(z).append(" Callers=").append(Debug.getCallers(4)).toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowInfo getWindowInfo() {
        WindowInfo windowInfo = WindowInfo.obtain();
        windowInfo.displayId = getDisplayId();
        windowInfo.type = this.mAttrs.type;
        windowInfo.layer = this.mLayer;
        windowInfo.token = this.mClient.asBinder();
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            windowInfo.activityToken = activityRecord.token;
        }
        windowInfo.title = this.mAttrs.accessibilityTitle;
        boolean isPanelWindow = this.mAttrs.type >= 1000 && this.mAttrs.type <= 1999;
        boolean isAccessibilityOverlay = windowInfo.type == 2032;
        if (TextUtils.isEmpty(windowInfo.title) && (isPanelWindow || isAccessibilityOverlay)) {
            CharSequence title = this.mAttrs.getTitle();
            windowInfo.title = TextUtils.isEmpty(title) ? null : title;
        }
        windowInfo.accessibilityIdOfAnchor = this.mAttrs.accessibilityIdOfAnchor;
        windowInfo.focused = isFocused();
        Task task = getTask();
        windowInfo.inPictureInPicture = task != null && task.inPinnedWindowingMode();
        windowInfo.taskId = task == null ? -1 : task.mTaskId;
        windowInfo.hasFlagWatchOutsideTouch = (this.mAttrs.flags & 262144) != 0;
        if (this.mIsChildWindow) {
            windowInfo.parentToken = getParentWindow().mClient.asBinder();
        }
        int childCount = this.mChildren.size();
        if (childCount > 0) {
            if (windowInfo.childTokens == null) {
                windowInfo.childTokens = new ArrayList(childCount);
            }
            for (int j = 0; j < childCount; j++) {
                WindowState child = (WindowState) this.mChildren.get(j);
                windowInfo.childTokens.add(child.mClient.asBinder());
            }
        }
        return windowInfo;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (this.mChildren.isEmpty()) {
            return applyInOrderWithImeWindows(callback, traverseTopToBottom);
        }
        if (traverseTopToBottom) {
            return forAllWindowTopToBottom(callback);
        }
        return forAllWindowBottomToTop(callback);
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x0031, code lost:
        if (applyInOrderWithImeWindows(r7, false) == false) goto L16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0033, code lost:
        return true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0034, code lost:
        if (r0 >= r1) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x003a, code lost:
        if (r2.applyInOrderWithImeWindows(r7, false) == false) goto L19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x003c, code lost:
        return true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x003d, code lost:
        r0 = r0 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x003f, code lost:
        if (r0 < r1) goto L21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0042, code lost:
        r2 = (com.android.server.wm.WindowState) r6.mChildren.get(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x004c, code lost:
        return false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean forAllWindowBottomToTop(ToBooleanFunction<WindowState> callback) {
        int i = 0;
        int count = this.mChildren.size();
        Object obj = this.mChildren.get(0);
        while (true) {
            WindowState child = (WindowState) obj;
            if (i >= count || child.mSubLayer >= 0) {
                break;
            } else if (child.applyInOrderWithImeWindows(callback, false)) {
                return true;
            } else {
                i++;
                if (i >= count) {
                    break;
                }
                obj = this.mChildren.get(i);
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void updateAboveInsetsState(final InsetsState aboveInsetsState, SparseArray<InsetsSourceProvider> localInsetsSourceProvidersFromParent, final ArraySet<WindowState> insetsChangedWindows) {
        SparseArray<InsetsSourceProvider> mergedLocalInsetsSourceProviders = localInsetsSourceProvidersFromParent;
        if (this.mLocalInsetsSourceProviders != null && this.mLocalInsetsSourceProviders.size() != 0) {
            mergedLocalInsetsSourceProviders = createShallowCopy(mergedLocalInsetsSourceProviders);
            for (int i = 0; i < this.mLocalInsetsSourceProviders.size(); i++) {
                mergedLocalInsetsSourceProviders.put(this.mLocalInsetsSourceProviders.keyAt(i), this.mLocalInsetsSourceProviders.valueAt(i));
            }
        }
        final SparseArray<InsetsSource> mergedLocalInsetsSourcesFromParent = toInsetsSources(mergedLocalInsetsSourceProviders);
        forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowState$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowState.lambda$updateAboveInsetsState$3(aboveInsetsState, insetsChangedWindows, mergedLocalInsetsSourcesFromParent, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateAboveInsetsState$3(InsetsState aboveInsetsState, ArraySet insetsChangedWindows, SparseArray mergedLocalInsetsSourcesFromParent, WindowState w) {
        if (!w.mAboveInsetsState.equals(aboveInsetsState)) {
            w.mAboveInsetsState.set(aboveInsetsState);
            insetsChangedWindows.add(w);
        }
        if (!mergedLocalInsetsSourcesFromParent.contentEquals(w.mMergedLocalInsetsSources)) {
            w.mMergedLocalInsetsSources = createShallowCopy(mergedLocalInsetsSourcesFromParent);
            insetsChangedWindows.add(w);
        }
        SparseArray<InsetsSource> providedSources = w.mProvidedInsetsSources;
        if (providedSources != null) {
            for (int i = providedSources.size() - 1; i >= 0; i--) {
                aboveInsetsState.addSource(providedSources.valueAt(i));
            }
        }
    }

    private static SparseArray<InsetsSource> toInsetsSources(SparseArray<InsetsSourceProvider> insetsSourceProviders) {
        SparseArray<InsetsSource> insetsSources = new SparseArray<>(insetsSourceProviders.size());
        for (int i = 0; i < insetsSourceProviders.size(); i++) {
            insetsSources.append(insetsSourceProviders.keyAt(i), insetsSourceProviders.valueAt(i).getSource());
        }
        return insetsSources;
    }

    private boolean forAllWindowTopToBottom(ToBooleanFunction<WindowState> callback) {
        int i = this.mChildren.size() - 1;
        WindowState child = (WindowState) this.mChildren.get(i);
        while (i >= 0 && child.mSubLayer >= 0) {
            if (child.applyInOrderWithImeWindows(callback, true)) {
                return true;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        if (applyInOrderWithImeWindows(callback, true)) {
            return true;
        }
        while (i >= 0) {
            if (child.applyInOrderWithImeWindows(callback, true)) {
                return true;
            }
            i--;
            if (i >= 0) {
                child = (WindowState) this.mChildren.get(i);
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean applyImeWindowsIfNeeded(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (isImeLayeringTarget()) {
            WindowState imeInputTarget = getImeInputTarget();
            if (imeInputTarget == null || imeInputTarget.isDrawn() || imeInputTarget.isVisible()) {
                return this.mDisplayContent.forAllImeWindows(callback, traverseTopToBottom);
            }
            return false;
        }
        return false;
    }

    private boolean applyInOrderWithImeWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            if (applyImeWindowsIfNeeded(callback, traverseTopToBottom) || callback.apply(this)) {
                return true;
            }
            return false;
        } else if (callback.apply(this) || applyImeWindowsIfNeeded(callback, traverseTopToBottom)) {
            return true;
        } else {
            return false;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    WindowState getWindow(Predicate<WindowState> callback) {
        if (this.mChildren.isEmpty()) {
            if (callback.test(this)) {
                return this;
            }
            return null;
        }
        int i = this.mChildren.size() - 1;
        WindowState child = (WindowState) this.mChildren.get(i);
        while (i >= 0 && child.mSubLayer >= 0) {
            if (callback.test(child)) {
                return child;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        if (callback.test(this)) {
            return this;
        }
        while (i >= 0) {
            if (callback.test(child)) {
                return child;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        return null;
    }

    boolean isSelfOrAncestorWindowAnimatingExit() {
        WindowState window = this;
        while (!window.mAnimatingExit) {
            window = window.getParentWindow();
            if (window == null) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExitAnimationRunningSelfOrParent() {
        return inTransitionSelfOrParent() || isAnimating(0, 16);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isExitAnimationRunningSelfOrChild() {
        return isAnimating(4, 16);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTransitionSelfOrParent() {
        if (!this.mTransitionController.isShellTransitionsEnabled()) {
            return isAnimating(3, 9);
        }
        return this.mTransitionController.inTransition(this);
    }

    private boolean shouldFinishAnimatingExit() {
        if (inTransition()) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -1145384901, 0, (String) null, new Object[]{protoLogParam0});
            }
            return false;
        } else if (this.mDisplayContent.okToAnimate()) {
            if (isExitAnimationRunningSelfOrParent()) {
                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                    String protoLogParam02 = String.valueOf(this);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -743856570, 0, (String) null, new Object[]{protoLogParam02});
                }
                return false;
            } else if (this.mDisplayContent.mWallpaperController.isWallpaperTarget(this)) {
                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                    String protoLogParam03 = String.valueOf(this);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -208825711, 0, (String) null, new Object[]{protoLogParam03});
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupAnimatingExitWindow() {
        if (this.mAnimatingExit && shouldFinishAnimatingExit()) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1087494661, 0, (String) null, new Object[]{protoLogParam0});
            }
            onExitAnimationDone();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onExitAnimationDone() {
        if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_ANIM)) {
            AnimationAdapter animationAdapter = this.mSurfaceAnimator.getAnimation();
            StringWriter sw = new StringWriter();
            if (animationAdapter != null) {
                PrintWriter pw = new PrintWriter(sw);
                animationAdapter.dump(pw, "");
            }
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(this);
                boolean protoLogParam1 = this.mAnimatingExit;
                boolean protoLogParam2 = this.mRemoveOnExit;
                boolean protoLogParam3 = isAnimating();
                String protoLogParam4 = String.valueOf(sw);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 1164325516, (int) AudioChannelMask.IN_6, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), protoLogParam4});
            }
        }
        if (!this.mChildren.isEmpty()) {
            ArrayList<WindowState> childWindows = new ArrayList<>(this.mChildren);
            for (int i = childWindows.size() - 1; i >= 0; i--) {
                childWindows.get(i).onExitAnimationDone();
            }
        }
        if (this.mWinAnimator.mEnteringAnimation) {
            this.mWinAnimator.mEnteringAnimation = false;
            this.mWmService.requestTraversal();
            if (this.mActivityRecord == null) {
                try {
                    this.mClient.dispatchWindowShown();
                } catch (RemoteException e) {
                }
            }
        }
        if (isAnimating() || !isSelfOrAncestorWindowAnimatingExit()) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam02 = String.valueOf(this);
            boolean protoLogParam12 = this.mRemoveOnExit;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1051545910, 12, (String) null, new Object[]{protoLogParam02, Boolean.valueOf(protoLogParam12)});
        }
        this.mDestroying = true;
        boolean hasSurface = this.mWinAnimator.hasSurface();
        this.mWinAnimator.hide(getPendingTransaction(), "onExitAnimationDone");
        if (this.mActivityRecord != null) {
            if (this.mAttrs.type != 1) {
                destroySurface(false, this.mActivityRecord.mAppStopped);
            } else {
                this.mActivityRecord.destroySurfaces();
            }
        } else if (hasSurface) {
            this.mWmService.mDestroySurface.add(this);
        }
        this.mAnimatingExit = false;
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam03 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, 283489582, 0, (String) null, new Object[]{protoLogParam03});
        }
        getDisplayContent().mWallpaperController.hideWallpapers(this);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean handleCompleteDeferredRemoval() {
        if (this.mRemoveOnExit && !isSelfAnimating(0, 16)) {
            this.mRemoveOnExit = false;
            removeImmediately();
        }
        return super.handleCompleteDeferredRemoval();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearAnimatingFlags() {
        boolean didSomething = false;
        if (!this.mWillReplaceWindow && !this.mRemoveOnExit) {
            if (this.mAnimatingExit) {
                this.mAnimatingExit = false;
                if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, -1209252064, 0, (String) null, new Object[]{protoLogParam0});
                }
                didSomething = true;
            }
            if (this.mDestroying) {
                this.mDestroying = false;
                this.mWmService.mDestroySurface.remove(this);
                didSomething = true;
            }
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            didSomething |= ((WindowState) this.mChildren.get(i)).clearAnimatingFlags();
        }
        return didSomething;
    }

    public boolean isRtl() {
        return getConfiguration().getLayoutDirection() == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateReportedVisibility(UpdateReportedVisibilityResults results) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState c = (WindowState) this.mChildren.get(i);
            c.updateReportedVisibility(results);
        }
        if (this.mAppFreezing || this.mViewVisibility != 0 || this.mAttrs.type == 3 || this.mDestroying) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Win " + this + ": isDrawn=" + isDrawn() + ", animating=" + isAnimating(3));
            if (!isDrawn()) {
                StringBuilder append = new StringBuilder().append("Not displayed: s=").append(this.mWinAnimator.mSurfaceController).append(" pv=").append(isVisibleByPolicy()).append(" mDrawState=").append(this.mWinAnimator.mDrawState).append(" ph=").append(isParentWindowHidden()).append(" th=");
                ActivityRecord activityRecord = this.mActivityRecord;
                Slog.v("WindowManager", append.append(activityRecord != null && activityRecord.mVisibleRequested).append(" a=").append(isAnimating(3)).toString());
            }
        }
        results.numInteresting++;
        if (isDrawn()) {
            results.numDrawn++;
            if (!isAnimating(3)) {
                results.numVisible++;
            }
            results.nowGone = false;
        } else if (isAnimating(3)) {
            results.nowGone = false;
        }
    }

    boolean surfaceInsetsChanging() {
        return !this.mLastSurfaceInsets.equals(this.mAttrs.surfaceInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int relayoutVisibleWindow(int result) {
        boolean wasVisible = isVisible();
        int result2 = result | ((wasVisible && isDrawn()) ? 0 : 1);
        if (this.mAnimatingExit) {
            Slog.d("WindowManager", "relayoutVisibleWindow: " + this + " mAnimatingExit=true, mRemoveOnExit=" + this.mRemoveOnExit + ", mDestroying=" + this.mDestroying);
            if (isAnimating()) {
                cancelAnimation();
            }
            this.mAnimatingExit = false;
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, -1933723759, 0, (String) null, new Object[]{protoLogParam0});
            }
        }
        if (this.mDestroying) {
            this.mDestroying = false;
            this.mWmService.mDestroySurface.remove(this);
        }
        if (!wasVisible) {
            this.mWinAnimator.mEnterAnimationPending = true;
        }
        this.mLastVisibleLayoutRotation = getDisplayContent().getRotation();
        this.mWinAnimator.mEnteringAnimation = true;
        Trace.traceBegin(32L, "prepareToDisplay");
        try {
            prepareWindowToDisplayDuringRelayout(wasVisible);
            return result2;
        } finally {
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLaidOut() {
        return this.mLayoutSeq != -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLastFrames() {
        this.mWindowFrames.mLastFrame.set(this.mWindowFrames.mFrame);
        this.mWindowFrames.mLastRelFrame.set(this.mWindowFrames.mRelFrame);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onResizeHandled() {
        this.mWindowFrames.onResizeHandled();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.WindowContainer
    public boolean isSelfAnimating(int flags, int typesToCheck) {
        if (this.mControllableInsetProvider != null) {
            return false;
        }
        return super.isSelfAnimating(flags, typesToCheck);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(Animation anim) {
        if (this.mControllableInsetProvider != null) {
            return;
        }
        DisplayInfo displayInfo = getDisplayInfo();
        anim.initialize(this.mWindowFrames.mFrame.width(), this.mWindowFrames.mFrame.height(), displayInfo.appWidth, displayInfo.appHeight);
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        anim.scaleCurrentDuration(this.mWmService.getWindowAnimationScaleLocked());
        AnimationAdapter adapter = new LocalAnimationAdapter(new WindowAnimationSpec(anim, this.mSurfacePosition, false, 0.0f), this.mWmService.mSurfaceAnimationRunner);
        startAnimation(getPendingTransaction(), adapter);
        commitPendingTransaction();
    }

    private void startMoveAnimation(int left, int top) {
        if (this.mControllableInsetProvider != null) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, -347866078, 0, (String) null, new Object[]{protoLogParam0});
        }
        Point oldPosition = new Point();
        Point newPosition = new Point();
        transformFrameToSurfacePosition(this.mWindowFrames.mLastFrame.left, this.mWindowFrames.mLastFrame.top, oldPosition);
        transformFrameToSurfacePosition(left, top, newPosition);
        if (TranFoldWMCustody.instance().startMoveAnimation(this, oldPosition, newPosition)) {
            return;
        }
        AnimationAdapter adapter = new LocalAnimationAdapter(new MoveAnimationSpec(oldPosition.x, oldPosition.y, newPosition.x, newPosition.y), this.mWmService.mSurfaceAnimationRunner);
        startAnimation(getPendingTransaction(), adapter);
    }

    private void startAnimation(SurfaceControl.Transaction t, AnimationAdapter adapter) {
        startAnimation(t, adapter, this.mWinAnimator.mLastHidden, 16);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.WindowContainer
    public void onAnimationFinished(int type, AnimationAdapter anim) {
        super.onAnimationFinished(type, anim);
        this.mWinAnimator.onAnimationFinished();
        if (this.mHideByDreamAnimationState == 2) {
            this.mWinAnimator.hideLayerLocked();
        }
        Slog.i("WindowManager", "  onAnimationFinished on win=" + this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getTransformationMatrix(float[] float9, Matrix outMatrix) {
        float f = this.mGlobalScale;
        float9[0] = f;
        float9[3] = 0.0f;
        float9[1] = 0.0f;
        float9[4] = f;
        transformSurfaceInsetsPosition(this.mTmpPoint, this.mAttrs.surfaceInsets);
        int x = this.mSurfacePosition.x + this.mTmpPoint.x;
        int y = this.mSurfacePosition.y + this.mTmpPoint.y;
        WindowContainer parent = getParent();
        if (isChildWindow()) {
            WindowState parentWindow = getParentWindow();
            x += parentWindow.mWindowFrames.mFrame.left - parentWindow.mAttrs.surfaceInsets.left;
            y += parentWindow.mWindowFrames.mFrame.top - parentWindow.mAttrs.surfaceInsets.top;
        } else if (parent != null) {
            Rect parentBounds = parent.getBounds();
            x += parentBounds.left;
            y += parentBounds.top;
        }
        float9[2] = x;
        float9[5] = y;
        float9[6] = 0.0f;
        float9[7] = 0.0f;
        float9[8] = 1.0f;
        outMatrix.setValues(float9);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class UpdateReportedVisibilityResults {
        boolean nowGone = true;
        int numDrawn;
        int numInteresting;
        int numVisible;

        /* JADX INFO: Access modifiers changed from: package-private */
        public void reset() {
            this.numInteresting = 0;
            this.numVisible = 0;
            this.numDrawn = 0;
            this.nowGone = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WindowId extends IWindowId.Stub {
        private final WeakReference<WindowState> mOuter;

        private WindowId(WindowState outer) {
            this.mOuter = new WeakReference<>(outer);
        }

        public void registerFocusObserver(IWindowFocusObserver observer) {
            WindowState outer = this.mOuter.get();
            if (outer != null) {
                outer.registerFocusObserver(observer);
            }
        }

        public void unregisterFocusObserver(IWindowFocusObserver observer) {
            WindowState outer = this.mOuter.get();
            if (outer != null) {
                outer.unregisterFocusObserver(observer);
            }
        }

        public boolean isFocused() {
            boolean isFocused;
            WindowState outer = this.mOuter.get();
            if (outer != null) {
                synchronized (outer.mWmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        isFocused = outer.isFocused();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return isFocused;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean shouldMagnify() {
        return (this.mAttrs.type == 2039 || this.mAttrs.type == 2011 || this.mAttrs.type == 2012 || this.mAttrs.type == 2027 || this.mAttrs.type == 2019 || this.mAttrs.type == 2024 || (this.mAttrs.privateFlags & 4194304) != 0) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer
    SurfaceSession getSession() {
        if (this.mSession.mSurfaceSession != null) {
            return this.mSession.mSurfaceSession;
        }
        return getParent().getSession();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean needsZBoost() {
        ActivityRecord activity;
        InsetsControlTarget target = getDisplayContent().getImeTarget(0);
        if (this.mIsImWindow && target != null && (activity = target.getWindow().mActivityRecord) != null) {
            return activity.needsZBoost();
        }
        return this.mWillReplaceWindow;
    }

    private boolean isStartingWindowAssociatedToTask() {
        StartingData startingData = this.mStartingData;
        return (startingData == null || startingData.mAssociatedTask == null) ? false : true;
    }

    private void applyDims() {
        if (!this.mAnimatingExit && this.mAppDied) {
            this.mIsDimming = true;
            getDimmer().dimAbove(getSyncTransaction(), this, 0.5f);
        } else if (((this.mAttrs.flags & 2) != 0 || shouldDrawBlurBehind()) && isVisibleNow() && !this.mHidden) {
            if (-2.0f != this.mAttrs.getBlurBehindSaturation()) {
                getDimmer().setSaturation(this.mAttrs.getBlurBehindSaturation());
            }
            this.mIsDimming = true;
            float dimAmount = (this.mAttrs.flags & 2) != 0 ? this.mAttrs.dimAmount : 0.0f;
            int blurRadius = shouldDrawBlurBehind() ? this.mAttrs.getBlurBehindRadius() : 0;
            getDimmer().dimBelow(getSyncTransaction(), this, dimAmount, blurRadius);
        }
    }

    private boolean shouldDrawBlurBehind() {
        return (this.mAttrs.flags & 4) != 0 && this.mWmService.mBlurController.getBlurEnabled();
    }

    void updateFrameRateSelectionPriorityIfNeeded() {
        RefreshRatePolicy refreshRatePolicy = getDisplayContent().getDisplayPolicy().getRefreshRatePolicy();
        int priority = refreshRatePolicy.calculatePriority(this);
        if (this.mFrameRateSelectionPriority != priority) {
            this.mFrameRateSelectionPriority = priority;
            getPendingTransaction().setFrameRateSelectionPriority(this.mSurfaceControl, this.mFrameRateSelectionPriority);
        }
        if (this.mWmService.mDisplayManagerInternal.getRefreshRateSwitchingType() != 0) {
            if (this.mWmService.isMsyncOn) {
                ActivityRecord mActivityRecord = getActivityRecord();
                if (!this.mIsImWindow && (mActivityRecord == null || !mActivityRecord.isVisible())) {
                    return;
                }
                if (this.mIsImWindow && this.mAttrs.mMSyncScenarioType == 0) {
                    return;
                }
            }
            float refreshRate = refreshRatePolicy.getPreferredRefreshRate(this);
            if (this.mAppPreferredFrameRate != refreshRate) {
                this.mAppPreferredFrameRate = refreshRate;
                if (this.mWmService.isMsyncOn) {
                    Slog.d("MSyncForWMS", "setFrameRate: " + this.mAppPreferredFrameRate);
                }
                if (ITranWindowState.Instance().needHookWindowState()) {
                    ITranWindowState.Instance().updateFrameRateSelectionPriorityIfNeeded(this);
                } else {
                    getPendingTransaction().setFrameRate(this.mSurfaceControl, this.mAppPreferredFrameRate, 100, 1);
                }
            }
        }
    }

    private void updateScaleIfNeeded() {
        if (this.mIsChildWindow) {
            return;
        }
        if (!isVisibleRequested() && (!this.mIsWallpaper || !this.mToken.isVisible())) {
            return;
        }
        if (ITranWindowState.Instance().isThunderbackWindow(getConfiguration())) {
            this.mGlobalScale = 1.0f;
        }
        float f = this.mHScale;
        float f2 = this.mGlobalScale;
        float f3 = this.mWallpaperScale;
        float newHScale = f * f2 * f3;
        float newVScale = this.mVScale * f2 * f3;
        if (this.mLastHScale != newHScale || this.mLastVScale != newVScale) {
            getSyncTransaction().setMatrix(this.mSurfaceControl, newHScale, 0.0f, 0.0f, newVScale);
            this.mLastHScale = newHScale;
            this.mLastVScale = newVScale;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void prepareSurfaces() {
        this.mIsDimming = false;
        applyDims();
        updateSurfacePositionNonOrganized();
        updateFrameRateSelectionPriorityIfNeeded();
        updateScaleIfNeeded();
        this.mWinAnimator.prepareSurfaceLocked(getSyncTransaction());
        super.prepareSurfaces();
    }

    @Override // com.android.server.wm.WindowContainer
    void updateSurfacePosition(SurfaceControl.Transaction t) {
        if (this.mSurfaceControl == null) {
            return;
        }
        if ((this.mWmService.mWindowPlacerLocked.isLayoutDeferred() || isGoneForLayout()) && !this.mSurfacePlacementNeeded) {
            return;
        }
        boolean surfaceSizeChanged = false;
        this.mSurfacePlacementNeeded = false;
        transformFrameToSurfacePosition(this.mWindowFrames.mFrame.left, this.mWindowFrames.mFrame.top, this.mSurfacePosition);
        if (this.mWallpaperScale != 1.0f) {
            Rect bounds = getLastReportedBounds();
            Matrix matrix = this.mTmpMatrix;
            matrix.setTranslate(this.mXOffset, this.mYOffset);
            float f = this.mWallpaperScale;
            matrix.postScale(f, f, bounds.exactCenterX(), bounds.exactCenterY());
            matrix.getValues(this.mTmpMatrixArray);
            this.mSurfacePosition.offset(Math.round(this.mTmpMatrixArray[2]), Math.round(this.mTmpMatrixArray[5]));
        } else {
            this.mSurfacePosition.offset(this.mXOffset, this.mYOffset);
        }
        if (!this.mSurfaceAnimator.hasLeash() && this.mPendingSeamlessRotate == null && !this.mLastSurfacePosition.equals(this.mSurfacePosition)) {
            boolean frameSizeChanged = this.mWindowFrames.isFrameSizeChangeReported();
            boolean surfaceInsetsChanged = surfaceInsetsChanging();
            surfaceSizeChanged = (frameSizeChanged || surfaceInsetsChanged) ? true : true;
            this.mLastSurfacePosition.set(this.mSurfacePosition.x, this.mSurfacePosition.y);
            if (surfaceInsetsChanged) {
                this.mLastSurfaceInsets.set(this.mAttrs.surfaceInsets);
            }
            if (surfaceSizeChanged && this.mWinAnimator.getShown() && !canPlayMoveAnimation() && okToDisplay()) {
                applyWithNextDraw(this.mSetSurfacePositionConsumer);
            } else {
                this.mSetSurfacePositionConsumer.accept(t);
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void transformFrameToSurfacePosition(int left, int top, Point outPoint) {
        Rect parentBounds;
        outPoint.set(left, top);
        WindowContainer parentWindowContainer = getParent();
        if (isChildWindow()) {
            WindowState parent = getParentWindow();
            outPoint.offset(-parent.mWindowFrames.mFrame.left, -parent.mWindowFrames.mFrame.top);
            if (this.mInvGlobalScale != 1.0f) {
                outPoint.x = (int) ((outPoint.x * this.mInvGlobalScale) + 0.5f);
                outPoint.y = (int) ((outPoint.y * this.mInvGlobalScale) + 0.5f);
            }
            transformSurfaceInsetsPosition(this.mTmpPoint, parent.mAttrs.surfaceInsets);
            outPoint.offset(this.mTmpPoint.x, this.mTmpPoint.y);
        } else if (parentWindowContainer != null) {
            if (isStartingWindowAssociatedToTask()) {
                parentBounds = this.mStartingData.mAssociatedTask.getBounds();
            } else {
                parentBounds = parentWindowContainer.getBounds();
            }
            outPoint.offset(-parentBounds.left, -parentBounds.top);
        }
        transformSurfaceInsetsPosition(this.mTmpPoint, this.mAttrs.surfaceInsets);
        outPoint.offset(-this.mTmpPoint.x, -this.mTmpPoint.y);
        outPoint.y += this.mSurfaceTranslationY;
    }

    private void transformSurfaceInsetsPosition(Point outPos, Rect surfaceInsets) {
        if (this.mGlobalScale == 1.0f || this.mIsChildWindow) {
            outPos.x = surfaceInsets.left;
            outPos.y = surfaceInsets.top;
            return;
        }
        outPos.x = (int) ((surfaceInsets.left * this.mGlobalScale) + 0.5f);
        outPos.y = (int) ((surfaceInsets.top * this.mGlobalScale) + 0.5f);
    }

    boolean needsRelativeLayeringToIme() {
        WindowState imeTarget;
        if (!this.mDisplayContent.shouldImeAttachedToApp() && getDisplayContent().getImeContainer().isVisible()) {
            if (!isChildWindow()) {
                return (this.mActivityRecord == null || (imeTarget = getImeLayeringTarget()) == null || imeTarget == this || imeTarget.mToken != this.mToken || this.mAttrs.type == 3 || getParent() == null || imeTarget.compareTo((WindowContainer) this) > 0) ? false : true;
            } else if (getParentWindow().isImeLayeringTarget()) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.server.wm.InputTarget
    public InsetsControlTarget getImeControlTarget() {
        return getDisplayContent().getImeHostOrFallback(this);
    }

    @Override // com.android.server.wm.WindowContainer
    void assignLayer(final SurfaceControl.Transaction t, int layer) {
        Task task;
        if (this.mStartingData != null) {
            t.setLayer(this.mSurfaceControl, Integer.MAX_VALUE);
        } else if (needsRelativeLayeringToIme()) {
            getDisplayContent().assignRelativeLayerForImeTargetChild(t, this);
        } else {
            if (isOSFullDialog() && (task = getTask()) != null) {
                boolean assigned = task.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.WindowState$$ExternalSyntheticLambda3
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return WindowState.this.m8536lambda$assignLayer$4$comandroidserverwmWindowState(t, (TaskFragment) obj);
                    }
                });
                if (assigned) {
                    return;
                }
            }
            super.assignLayer(t, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$assignLayer$4$com-android-server-wm-WindowState  reason: not valid java name */
    public /* synthetic */ boolean m8536lambda$assignLayer$4$comandroidserverwmWindowState(SurfaceControl.Transaction t, TaskFragment tf) {
        if (tf != null && tf.getSurfaceControl() != null && tf.getTopNonFinishingActivity() != null) {
            assignRelativeLayer(t, tf.getSurfaceControl(), Integer.MAX_VALUE);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDimming() {
        return this.mIsDimming;
    }

    @Override // com.android.server.wm.WindowContainer
    protected void reparentSurfaceControl(SurfaceControl.Transaction t, SurfaceControl newParent) {
        if (isStartingWindowAssociatedToTask()) {
            return;
        }
        super.reparentSurfaceControl(t, newParent);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getAnimationLeashParent() {
        if (isStartingWindowAssociatedToTask()) {
            return this.mStartingData.mAssociatedTask.mSurfaceControl;
        }
        return super.getAnimationLeashParent();
    }

    @Override // com.android.server.wm.WindowContainer
    public void assignChildLayers(SurfaceControl.Transaction t) {
        int layer = 2;
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowState w = (WindowState) this.mChildren.get(i);
            if (w.mAttrs.type == 1001) {
                if (this.mWinAnimator.hasSurface()) {
                    w.assignRelativeLayer(t, this.mWinAnimator.mSurfaceController.mSurfaceControl, -2);
                } else {
                    w.assignLayer(t, -2);
                }
            } else if (w.mAttrs.type == 1004) {
                if (this.mWinAnimator.hasSurface()) {
                    w.assignRelativeLayer(t, this.mWinAnimator.mSurfaceController.mSurfaceControl, -1);
                } else {
                    w.assignLayer(t, -1);
                }
            } else {
                w.assignLayer(t, layer);
            }
            w.assignChildLayers(t);
            layer++;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTapExcludeRegion(Region region) {
        DisplayContent currentDisplay = getDisplayContent();
        if (currentDisplay == null) {
            throw new IllegalStateException("Trying to update window not attached to any display.");
        }
        if (region == null || region.isEmpty()) {
            this.mTapExcludeRegion.setEmpty();
            currentDisplay.mTapExcludeProvidingWindows.remove(this);
        } else {
            this.mTapExcludeRegion.set(region);
            currentDisplay.mTapExcludeProvidingWindows.add(this);
        }
        currentDisplay.updateTouchExcludeRegion();
        currentDisplay.getInputMonitor().updateInputWindowsLw(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getTapExcludeRegion(Region outRegion) {
        this.mTmpRect.set(this.mWindowFrames.mFrame);
        this.mTmpRect.offsetTo(0, 0);
        outRegion.set(this.mTapExcludeRegion);
        outRegion.op(this.mTmpRect, Region.Op.INTERSECT);
        outRegion.translate(this.mWindowFrames.mFrame.left, this.mWindowFrames.mFrame.top);
    }

    boolean hasTapExcludeRegion() {
        return !this.mTapExcludeRegion.isEmpty();
    }

    boolean isImeLayeringTarget() {
        return getDisplayContent().getImeTarget(0) == this;
    }

    WindowState getImeLayeringTarget() {
        InsetsControlTarget target = getDisplayContent().getImeTarget(0);
        if (target != null) {
            return target.getWindow();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getImeInputTarget() {
        InputTarget target = this.mDisplayContent.getImeInputTarget();
        if (target != null) {
            return target.getWindowState();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceReportingResized() {
        this.mWindowFrames.forceReportingResized();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowFrames getWindowFrames() {
        return this.mWindowFrames;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetContentChanged() {
        this.mWindowFrames.setContentChanged(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class MoveAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
        private final long mDuration;
        private Point mFrom;
        private Interpolator mInterpolator;
        private Point mTo;

        private MoveAnimationSpec(int fromX, int fromY, int toX, int toY) {
            this.mFrom = new Point();
            this.mTo = new Point();
            Animation anim = AnimationUtils.loadAnimation(WindowState.this.mContext, 17432795);
            this.mDuration = ((float) anim.computeDurationHint()) * WindowState.this.mWmService.getWindowAnimationScaleLocked();
            this.mInterpolator = anim.getInterpolator();
            this.mFrom.set(fromX, fromY);
            this.mTo.set(toX, toY);
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public long getDuration() {
            return this.mDuration;
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
            float fraction = getFraction((float) currentPlayTime);
            float v = this.mInterpolator.getInterpolation(fraction);
            t.setPosition(leash, this.mFrom.x + ((this.mTo.x - this.mFrom.x) * v), this.mFrom.y + ((this.mTo.y - this.mFrom.y) * v));
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "from=" + this.mFrom + " to=" + this.mTo + " duration=" + this.mDuration);
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void dumpDebugInner(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            GraphicsProtos.dumpPointProto(this.mFrom, proto, 1146756268033L);
            GraphicsProtos.dumpPointProto(this.mTo, proto, 1146756268034L);
            proto.write(1112396529667L, this.mDuration);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public KeyInterceptionInfo getKeyInterceptionInfo() {
        KeyInterceptionInfo keyInterceptionInfo = this.mKeyInterceptionInfo;
        if (keyInterceptionInfo == null || keyInterceptionInfo.layoutParamsPrivateFlags != getAttrs().privateFlags || this.mKeyInterceptionInfo.layoutParamsType != getAttrs().type || this.mKeyInterceptionInfo.windowTitle != getWindowTag()) {
            this.mKeyInterceptionInfo = new KeyInterceptionInfo(getAttrs().type, getAttrs().privateFlags, getWindowTag().toString());
        }
        return this.mKeyInterceptionInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void getAnimationFrames(Rect outFrame, Rect outInsets, Rect outStableInsets, Rect outSurfaceInsets) {
        if (inFreeformWindowingMode()) {
            outFrame.set(getFrame());
        } else if (areAppWindowBoundsLetterboxed() || this.mToken.isFixedRotationTransforming()) {
            outFrame.set(getTask().getBounds());
        } else if (isDockedResizing()) {
            outFrame.set(getTask().getParent().getBounds());
        } else {
            outFrame.set(getParentFrame());
        }
        outSurfaceInsets.set(getAttrs().surfaceInsets);
        InsetsState state = getInsetsStateWithVisibilityOverride();
        outInsets.set(state.calculateInsets(outFrame, WindowInsets.Type.systemBars(), false).toRect());
        outStableInsets.set(state.calculateInsets(outFrame, WindowInsets.Type.systemBars(), true).toRect());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setViewVisibility(int viewVisibility) {
        this.mViewVisibility = viewVisibility;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getClientViewRootSurface() {
        return this.mWinAnimator.getSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean prepareSync() {
        if (!super.prepareSync()) {
            return false;
        }
        this.mSyncState = 1;
        this.mSyncSeqId++;
        requestRedrawForSync();
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isSyncFinished() {
        if (this.mSyncState == 1 && this.mViewVisibility == 8 && !isVisibleRequested()) {
            return true;
        }
        return super.isSyncFinished();
    }

    @Override // com.android.server.wm.WindowContainer
    void finishSync(SurfaceControl.Transaction outMergedTransaction, boolean cancel) {
        if (this.mSyncState == 1 && this.mRedrawForSyncReported) {
            this.mClientWasDrawingForSync = true;
        }
        super.finishSync(outMergedTransaction, cancel);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean finishDrawing(SurfaceControl.Transaction postDrawTransaction, int syncSeqId) {
        TranFoldWMCustody.instance().finishDrawing(this, postDrawTransaction, syncSeqId);
        if (this.mOrientationChangeRedrawRequestTime > 0) {
            long duration = SystemClock.elapsedRealtime() - this.mOrientationChangeRedrawRequestTime;
            Slog.i("WindowManager", "finishDrawing of orientation change: " + this + " " + duration + "ms");
            this.mOrientationChangeRedrawRequestTime = 0L;
        } else {
            ActivityRecord activityRecord = this.mActivityRecord;
            if (activityRecord != null && activityRecord.mRelaunchStartTime != 0 && this.mActivityRecord.findMainWindow() == this) {
                long duration2 = SystemClock.elapsedRealtime() - this.mActivityRecord.mRelaunchStartTime;
                Slog.i("WindowManager", "finishDrawing of relaunch: " + this + " " + duration2 + "ms");
                this.mActivityRecord.mRelaunchStartTime = 0L;
            }
        }
        if (this.mActivityRecord != null && this.mAttrs.type == 3) {
            this.mWmService.mAtmService.mTaskSupervisor.getActivityMetricsLogger().notifyStartingWindowDrawn(this.mActivityRecord);
        }
        boolean hasSyncHandlers = executeDrawHandlers(postDrawTransaction, syncSeqId);
        boolean skipLayout = false;
        AsyncRotationController asyncRotationController = this.mDisplayContent.getAsyncRotationController();
        if (asyncRotationController != null && asyncRotationController.handleFinishDrawing(this, postDrawTransaction)) {
            postDrawTransaction = null;
            skipLayout = true;
            clearSyncState();
        } else if (onSyncFinishedDrawing() && postDrawTransaction != null) {
            this.mSyncTransaction.merge(postDrawTransaction);
            postDrawTransaction = null;
        }
        boolean layoutNeeded = this.mWinAnimator.finishDrawingLocked(postDrawTransaction, this.mClientWasDrawingForSync);
        this.mClientWasDrawingForSync = false;
        if (skipLayout) {
            return false;
        }
        return hasSyncHandlers || layoutNeeded;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void immediatelyNotifyBlastSync() {
        finishDrawing(null, Integer.MAX_VALUE);
        this.mWmService.mH.removeMessages(64, this);
        useBLASTSync();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean fillsParent() {
        return this.mAttrs.type == 3;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean showWallpaper() {
        if (!isVisibleRequested() || inMultiWindowMode()) {
            return false;
        }
        return hasWallpaper();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWallpaper() {
        return (this.mAttrs.flags & 1048576) != 0 || hasWallpaperForLetterboxBackground();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWallpaperForLetterboxBackground() {
        ActivityRecord activityRecord = this.mActivityRecord;
        return activityRecord != null && activityRecord.hasWallpaperBackgroudForLetterbox();
    }

    private boolean shouldSendRedrawForSync() {
        if (this.mRedrawForSyncReported) {
            return false;
        }
        return useBLASTSync();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestRedrawForSync() {
        this.mRedrawForSyncReported = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean useBLASTSync() {
        return super.useBLASTSync() || this.mDrawHandlers.size() != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyWithNextDraw(Consumer<SurfaceControl.Transaction> consumer) {
        int i = this.mSyncSeqId + 1;
        this.mSyncSeqId = i;
        this.mDrawHandlers.add(new DrawHandler(i, consumer));
        requestRedrawForSync();
        this.mWmService.mH.sendNewMessageDelayed(64, this, 5000L);
    }

    boolean executeDrawHandlers(SurfaceControl.Transaction t, int seqId) {
        boolean hadHandlers = false;
        boolean applyHere = false;
        if (t == null) {
            t = this.mTmpTransaction;
            applyHere = true;
        }
        List<DrawHandler> handlersToRemove = new ArrayList<>();
        for (int i = 0; i < this.mDrawHandlers.size(); i++) {
            DrawHandler h = this.mDrawHandlers.get(i);
            if (h.mSeqId <= seqId) {
                h.mConsumer.accept(t);
                handlersToRemove.add(h);
                hadHandlers = true;
            }
        }
        for (int i2 = 0; i2 < handlersToRemove.size(); i2++) {
            this.mDrawHandlers.remove(handlersToRemove.get(i2));
        }
        if (hadHandlers) {
            this.mWmService.mH.removeMessages(64, this);
        }
        if (applyHere) {
            t.apply();
        }
        return hadHandlers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSurfaceTranslationY(int translationY) {
        this.mSurfaceTranslationY = translationY;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public int getWindowType() {
        return this.mAttrs.type;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void markRedrawForSyncReported() {
        this.mRedrawForSyncReported = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setWallpaperOffset(int dx, int dy, float scale) {
        if (this.mXOffset == dx && this.mYOffset == dy && Float.compare(this.mWallpaperScale, scale) == 0) {
            return false;
        }
        this.mXOffset = dx;
        this.mYOffset = dy;
        this.mWallpaperScale = scale;
        scheduleAnimation();
        return true;
    }

    boolean isTrustedOverlay() {
        return this.mInputWindowHandle.isTrustedOverlay();
    }

    @Override // com.android.server.wm.InputTarget
    public boolean receiveFocusFromTapOutside() {
        return canReceiveKeys(true);
    }

    @Override // com.android.server.wm.InputTarget
    public void handleTapOutsideFocusOutsideSelf() {
    }

    @Override // com.android.server.wm.InputTarget
    public void handleTapOutsideFocusInsideSelf() {
        DisplayContent displayContent = getDisplayContent();
        if (!displayContent.isOnTop()) {
            displayContent.getParent().positionChildAt(Integer.MAX_VALUE, displayContent, true);
        }
        this.mWmService.handleTaskFocusChange(getTask(), this.mActivityRecord);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearClientTouchableRegion() {
        this.mTouchableInsets = 0;
        this.mGivenTouchableRegion.setEmpty();
    }

    @Override // com.android.server.wm.InputTarget
    public boolean shouldControlIme() {
        return !inMultiWindowMode();
    }

    @Override // com.android.server.wm.InputTarget
    public boolean canScreenshotIme() {
        return !isSecureLocked();
    }

    @Override // com.android.server.wm.InputTarget
    public ActivityRecord getActivityRecord() {
        return this.mActivityRecord;
    }

    @Override // com.android.server.wm.InputTarget
    public void unfreezeInsetsAfterStartInput() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null) {
            activityRecord.mImeInsetsFrozenUntilStartInput = false;
        }
    }

    @Override // com.android.server.wm.InputTarget
    public boolean isInputMethodClientFocus(int uid, int pid) {
        return getDisplayContent().isInputMethodClientFocus(uid, pid);
    }

    @Override // com.android.server.wm.InputTarget
    public void dumpProto(ProtoOutputStream proto, long fieldId, int logLevel) {
        dumpDebug(proto, fieldId, logLevel);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl createWeltAnimationLeash(SurfaceControl.Transaction t, int width, int height, int x, int y, boolean hidden) {
        return ITranWindowState.Instance().createWeltAnimationLeash(this, this.mSurfaceFreezer.hasLeash(), this.mSurfaceAnimator.hasLeash(), t, width, height, x, y, hidden);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl createDragAndZoomBgLeash(SurfaceControl.Transaction t, int width, int height, int x, int y, boolean hidden) {
        return ITranWindowState.Instance().createDragAndZoomBgLeash(this, this.mSurfaceFreezer.hasLeash(), this.mSurfaceAnimator.hasLeash(), t, width, height, x, y, hidden);
    }

    public int getRequestedWidth() {
        return this.mRequestedWidth;
    }

    public int getRequestedHeight() {
        return this.mRequestedHeight;
    }

    @Override // com.android.server.wm.InsetsControlTarget
    public boolean canShowStatusBar() {
        return !getConfiguration().windowConfiguration.isSplitScreenWindow();
    }

    public void updateCaptionViewStatusIfNeeded() {
        ITranWindowState.Instance().updateCaptionViewStatusIfNeeded(this, this.mClient, this.mActivityRecord);
    }

    public final boolean isInMultiWindow() {
        return ITranWindowState.Instance().isInMultiWindow(getConfiguration());
    }

    public SurfaceControl getWeltLeash() {
        return this.mWeltLeash;
    }

    public void setWeltLeash(SurfaceControl weltLeash) {
        this.mWeltLeash = weltLeash;
    }

    public void setCaptionViewShow(boolean captionViewShow) {
        this.mCaptionViewShow = captionViewShow;
    }

    public boolean isCaptionViewShow() {
        return this.mCaptionViewShow;
    }

    public void setLastMultiWindowMode(int lastMultiWindowMode) {
        this.mLastMultiWindowMode = lastMultiWindowMode;
    }

    public int getLastMultiWindowMode() {
        return this.mLastMultiWindowMode;
    }

    public TranWindowInfo getWinProxy() {
        return this.mWinProxy;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isVisibleLw() {
        return isVisible();
    }

    /* loaded from: classes2.dex */
    public static class TranWindowStateProxy {
        private WindowState mWindowState;

        public TranWindowStateProxy(WindowState state) {
            this.mWindowState = state;
        }

        public boolean isFullscreen() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mAttrs.isFullscreen();
            }
            return false;
        }

        public String getLastTitle() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mLastTitle.toString();
            }
            return "";
        }

        public int getRequestedWidth() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mRequestedWidth;
            }
            return 0;
        }

        public int getRequestedHeight() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mRequestedHeight;
            }
            return 0;
        }

        public SurfaceControl getSurfaceControl() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mSurfaceControl;
            }
            return new SurfaceControl();
        }

        public float getAppPreferredFrameRate() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mAppPreferredFrameRate;
            }
            return 0.0f;
        }

        public int getPreferredModeId() {
            DisplayContent displayContent;
            DisplayPolicy displayPolicy;
            RefreshRatePolicy refreshRatePolicy;
            WindowState windowState = this.mWindowState;
            if (windowState != null && (displayContent = windowState.getDisplayContent()) != null && (displayPolicy = displayContent.getDisplayPolicy()) != null && (refreshRatePolicy = displayPolicy.getRefreshRatePolicy()) != null) {
                return refreshRatePolicy.getPreferredModeId(this.mWindowState);
            }
            return 0;
        }

        public DisplayInfo getDisplayInfo() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.getDisplayInfo();
            }
            return null;
        }

        public float getPreferredRefreshRate() {
            WindowState windowState = this.mWindowState;
            if (windowState != null) {
                return windowState.mAttrs.preferredRefreshRate;
            }
            return 0.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isNoNeedSetAlwaysCustomSystemBars(WindowState win, ActivityRecord activity, DisplayContent displayContent) {
        if (displayContent != null && activity != null && activity.getWindowingMode() != 6 && activity.getDisplayArea() == displayContent.getDefaultTaskDisplayArea()) {
            return true;
        }
        return false;
    }
}
