package android.view;

import android.Manifest;
import android.animation.AnimationHandler;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.ICompatCameraControlCallback;
import android.app.ResourcesManager;
import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.FrameInfo;
import android.graphics.HardwareRenderer;
import android.graphics.HardwareRendererObserver;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RenderNode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.gnss.GnssSignalType;
import android.hardware.input.InputManager;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaMetrics;
import android.media.TtmlUtils;
import android.media.audio.Enums;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.sysprop.DisplayProperties;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.LongArray;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.ActionMode;
import android.view.AttachedSurfaceControl;
import android.view.Choreographer;
import android.view.IWindow;
import android.view.InputDevice;
import android.view.InputQueue;
import android.view.KeyCharacterMap;
import android.view.ScrollCaptureResponse;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeIdManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.IAccessibilityEmbeddedConnection;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.contentcapture.ContentCaptureManager;
import android.view.contentcapture.ContentCaptureSession;
import android.view.contentcapture.MainContentCaptureSession;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;
import android.window.ClientWindowFrames;
import android.window.CompatOnBackInvokedCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.window.SurfaceSyncer;
import android.window.WindowOnBackInvokedDispatcher;
import com.android.internal.R;
import com.android.internal.graphics.drawable.BackgroundBlurDrawable;
import com.android.internal.inputmethod.ImeTracing;
import com.android.internal.inputmethod.InputMethodDebug;
import com.android.internal.os.IResultReceiver;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.DecorView;
import com.android.internal.policy.PhoneFallbackEventHandler;
import com.android.internal.util.Preconditions;
import com.android.internal.view.BaseSurfaceHolder;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.SurfaceCallbackHelper;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.scenario.frame.FrameScenario;
import com.mediatek.boostfwk.scenario.scroll.ScrollScenario;
import com.mediatek.view.SurfaceExt;
import com.mediatek.view.SurfaceFactory;
import com.mediatek.view.ViewDebugManager;
import com.transsion.hubcore.view.ITranView;
import com.transsion.message.bank.IMessageBankLice;
import com.transsion.powerhub.TranPowerhubManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes3.dex */
public final class ViewRootImpl implements ViewParent, View.AttachInfo.Callbacks, ThreadedRenderer.DrawCallbacks, AttachedSurfaceControl {
    private static final int CONFIG_SCREEN_CHANGES = 536874240;
    private static final int CONTENT_CAPTURE_ENABLED_FALSE = 2;
    private static final int CONTENT_CAPTURE_ENABLED_NOT_CHECKED = 0;
    private static final int CONTENT_CAPTURE_ENABLED_TRUE = 1;
    private static final long DELAY_TIME = 50;
    private static final boolean ENABLE_INPUT_LATENCY_TRACKING = true;
    private static final String GRIFFIN_CLOUD_APP_RT_BLACKLIST_SETTINGS = "tran_rt_mode_blacklist_";
    private static final String GRIFFIN_CLOUD_APP_RT_SWITCH_SETTINGS = "tran_rt_mode_switch_";
    private static final int KEEP_CLEAR_AREA_REPORT_RATE_MILLIS = 100;
    private static final int MAX_QUEUED_INPUT_EVENT_POOL_SIZE = 10;
    static final int MAX_TRACKBALL_DELAY = 250;
    private static final int MSG_CAPTION_VIEW = 100;
    private static final int MSG_CHECK_FOCUS = 13;
    private static final int MSG_CLEAR_ACCESSIBILITY_FOCUS_HOST = 21;
    private static final int MSG_CLOSE_SYSTEM_DIALOGS = 14;
    private static final int MSG_DIE = 3;
    private static final int MSG_DISPATCH_APP_VISIBILITY = 8;
    private static final int MSG_DISPATCH_DRAG_EVENT = 15;
    private static final int MSG_DISPATCH_DRAG_LOCATION_EVENT = 16;
    private static final int MSG_DISPATCH_GET_NEW_SURFACE = 9;
    private static final int MSG_DISPATCH_INPUT_EVENT = 7;
    private static final int MSG_DISPATCH_KEY_FROM_AUTOFILL = 12;
    private static final int MSG_DISPATCH_KEY_FROM_IME = 11;
    private static final int MSG_DISPATCH_SYSTEM_UI_VISIBILITY = 17;
    private static final int MSG_DISPATCH_WINDOW_SHOWN = 25;
    private static final int MSG_HIDE_INSETS = 32;
    private static final int MSG_INSETS_CONTROL_CHANGED = 29;
    private static final int MSG_INVALIDATE = 1;
    private static final int MSG_INVALIDATE_RECT = 2;
    private static final int MSG_INVALIDATE_WORLD = 22;
    private static final int MSG_KEEP_CLEAR_RECTS_CHANGED = 35;
    private static final int MSG_POINTER_CAPTURE_CHANGED = 28;
    private static final int MSG_PROCESS_INPUT_EVENTS = 19;
    private static final int MSG_REPORT_KEEP_CLEAR_RECTS = 36;
    private static final int MSG_REQUEST_KEYBOARD_SHORTCUTS = 26;
    private static final int MSG_REQUEST_SCROLL_CAPTURE = 33;
    private static final int MSG_RESIZED = 4;
    private static final int MSG_RESIZED_REPORT = 5;
    private static final int MSG_SHOW_INSETS = 31;
    private static final int MSG_SYNTHESIZE_INPUT_EVENT = 24;
    private static final int MSG_SYSTEM_GESTURE_EXCLUSION_CHANGED = 30;
    private static final int MSG_TRAN_INVALIDATE = 37;
    private static final int MSG_UPDATE_CONFIGURATION = 18;
    private static final int MSG_UPDATE_POINTER_ICON = 27;
    private static final int MSG_WINDOW_FOCUS_CHANGED = 6;
    private static final int MSG_WINDOW_MOVED = 23;
    private static final int MSG_WINDOW_TOUCH_MODE_CHANGED = 34;
    private static final boolean MT_RENDERER_AVAILABLE = true;
    private static final String PROPERTY_PROFILE_RENDERING = "viewroot.profile_rendering";
    private static final int SCROLL_CAPTURE_REQUEST_TIMEOUT_MILLIS = 2500;
    private static final String TAG = "ViewRootImpl";
    private static final String TAG_GESTURE = "TranGesture";
    private static final int UNSET_SYNC_ID = -1;
    private static boolean sAlwaysAssignFocus;
    private final boolean DEBUG_GESTURE;
    private boolean debug_on;
    private IAccessibilityEmbeddedConnection mAccessibilityEmbeddedConnection;
    View mAccessibilityFocusedHost;
    AccessibilityNodeInfo mAccessibilityFocusedVirtualView;
    final AccessibilityInteractionConnectionManager mAccessibilityInteractionConnectionManager;
    AccessibilityInteractionController mAccessibilityInteractionController;
    final AccessibilityManager mAccessibilityManager;
    private ActivityConfigCallback mActivityConfigCallback;
    private boolean mActivityRelaunched;
    boolean mAdded;
    boolean mAddedTouchMode;
    private boolean mAppVisibilityChanged;
    boolean mAppVisible;
    boolean mApplyInsetsRequested;
    final View.AttachInfo mAttachInfo;
    AudioManager mAudioManager;
    final String mBasePackageName;
    private BLASTBufferQueue mBlastBufferQueue;
    private final BackgroundBlurDrawable.Aggregator mBlurRegionAggregator;
    private SurfaceControl mBoundsLayer;
    private int mCanvasOffsetX;
    private int mCanvasOffsetY;
    final Choreographer mChoreographer;
    int mClientWindowLayoutFlags;
    private CompatOnBackInvokedCallback mCompatOnBackInvokedCallback;
    final SystemUiVisibilityInfo mCompatibleVisibilityInfo;
    final ConsumeBatchedInputImmediatelyRunnable mConsumeBatchedInputImmediatelyRunnable;
    boolean mConsumeBatchedInputImmediatelyScheduled;
    boolean mConsumeBatchedInputScheduled;
    final ConsumeBatchedInputRunnable mConsumedBatchedInputRunnable;
    int mContentCaptureEnabled;
    public final Context mContext;
    int mCurScrollY;
    View mCurrentDragView;
    private PointerIcon mCustomPointerIcon;
    private boolean mDelayInvalidate;
    private int mDelayTime;
    private final int mDensity;
    private Rect mDirty;
    int mDispatchedSystemBarAppearance;
    int mDispatchedSystemUiVisibility;
    Display mDisplay;
    boolean mDisplayDecorationCached;
    private int mDisplayInstallOrientation;
    private final DisplayManager.DisplayListener mDisplayListener;
    final DisplayManager mDisplayManager;
    ClipDescription mDragDescription;
    final PointF mDragPoint;
    private boolean mDragResizing;
    boolean mDrawingAllowed;
    FallbackEventHandler mFallbackEventHandler;
    private boolean mFastScrollSoundEffectsEnabled;
    boolean mFirst;
    InputStage mFirstInputStage;
    InputStage mFirstPostImeInputStage;
    private boolean mForceDecorViewVisibility;
    private boolean mForceDisableBLAST;
    private boolean mForceNextConfigUpdate;
    boolean mForceNextWindowRelayout;
    private int mFpsNumFrames;
    private long mFpsPrevTime;
    private long mFpsStartTime;
    public int mFrame;
    private FrameScenario mFrameScenario;
    boolean mFullRedrawNeeded;
    private final ViewRootRectTracker mGestureExclusionTracker;
    boolean mHadWindowFocus;
    final ViewRootHandler mHandler;
    boolean mHandlingLayoutInLayoutRequest;
    private final HandwritingInitiator mHandwritingInitiator;
    HardwareRendererObserver mHardwareRendererObserver;
    int mHardwareXOffset;
    int mHardwareYOffset;
    private boolean mHasPendingKeepClearAreaChange;
    boolean mHasPendingTransactions;
    int mHeight;
    final HighContrastTextManager mHighContrastTextManager;
    public long mIdent;
    private final ImeFocusController mImeFocusController;
    private boolean mInLayout;
    private final InputEventCompatProcessor mInputCompatProcessor;
    private final InputEventAssigner mInputEventAssigner;
    protected final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    private WindowInputEventReceiver mInputEventReceiver;
    InputQueue mInputQueue;
    InputQueue.Callback mInputQueueCallback;
    private final InsetsController mInsetsController;
    final InvalidateOnAnimationRunnable mInvalidateOnAnimationRunnable;
    private boolean mInvalidateRootRequested;
    boolean mIsAmbientMode;
    public boolean mIsAnimating;
    boolean mIsCreating;
    boolean mIsDrawing;
    boolean mIsInTraversal;
    private boolean mIsRtEnable;
    private boolean mIsSurfaceOpaque;
    private boolean mIsSurfaceViewCreated;
    private final ViewRootRectTracker mKeepClearRectsTracker;
    private final Configuration mLastConfigurationFromResources;
    private long mLastDelayTimestamp;
    final ViewTreeObserver.InternalInsetsInfo mLastGivenInsets;
    boolean mLastInCompatMode;
    private final MergedConfiguration mLastReportedMergedConfiguration;
    WeakReference<View> mLastScrolledFocus;
    private final Point mLastSurfaceSize;
    int mLastSyncSeqId;
    int mLastSystemUiVisibility;
    final PointF mLastTouchPoint;
    int mLastTouchSource;
    private int mLastTransformHint;
    private WindowInsets mLastWindowInsets;
    boolean mLayoutRequested;
    ArrayList<View> mLayoutRequesters;
    final IBinder mLeashToken;
    volatile Object mLocalDragState;
    final WindowLeaked mLocation;
    boolean mLostWindowFocus;
    private boolean mNeedUpdateConfig;
    private boolean mNeedsRendererSetup;
    boolean mNewSurfaceNeeded;
    private long mNextInvalidateTime;
    private final int mNoncompatDensity;
    private int mNumSyncsInProgress;
    private final WindowOnBackInvokedDispatcher mOnBackInvokedDispatcher;
    int mOrigWindowType;
    Rect mOverrideInsetsFrame;
    private ViewRootImpl mParentViewRoot;
    boolean mPausedForTransition;
    boolean mPendingAlwaysConsumeSystemBars;
    final Rect mPendingBackDropFrame;
    private boolean mPendingDragResizing;
    int mPendingInputEventCount;
    QueuedInputEvent mPendingInputEventHead;
    String mPendingInputEventQueueLengthCounterName;
    QueuedInputEvent mPendingInputEventTail;
    private final MergedConfiguration mPendingMergedConfiguration;
    private ArrayList<LayoutTransition> mPendingTransitions;
    boolean mPerformContentCapture;
    boolean mPointerCapture;
    private int mPointerIconType;
    Region mPreviousTouchableRegion;
    private int mPreviousTransformHint;
    final Region mPreviousTransparentRegion;
    boolean mProcessInputEventsScheduled;
    private boolean mProfile;
    private boolean mProfileRendering;
    private QueuedInputEvent mQueuedInputEventPool;
    private int mQueuedInputEventPoolSize;
    private Bundle mRelayoutBundle;
    private boolean mRelayoutRequested;
    private boolean mRemoved;
    private Choreographer.FrameCallback mRenderProfiler;
    private boolean mRenderProfilingEnabled;
    boolean mReportNextDraw;
    private int mResizeMode;
    private HashSet<ScrollCaptureCallback> mRootScrollCaptureCallbacks;
    private long mScrollCaptureRequestTimeout;
    boolean mScrollMayChange;
    private ScrollScenario mScrollScenario;
    int mScrollY;
    Scroller mScroller;
    SendWindowContentChangedAccessibilityEvent mSendWindowContentChangedAccessibilityEvent;
    private boolean mShowCaptionView;
    int mSoftInputMode;
    View mStartedDragViewForA11y;
    boolean mStopped;
    public final Surface mSurface;
    private final ArrayList<SurfaceChangedCallback> mSurfaceChangedCallbacks;
    private final SurfaceControl.Transaction mSurfaceChangedTransaction;
    private final SurfaceControl mSurfaceControl;
    private SurfaceExt mSurfaceExt;
    BaseSurfaceHolder mSurfaceHolder;
    SurfaceHolder.Callback2 mSurfaceHolderCallback;
    private int mSurfaceSequenceId;
    private final SurfaceSession mSurfaceSession;
    private final Point mSurfaceSize;
    private final SurfaceSyncer mSurfaceSyncer;
    private boolean mSyncBuffer;
    private SurfaceSyncer.SyncBufferCallback mSyncBufferCallback;
    private int mSyncId;
    int mSyncSeqId;
    public final SurfaceSyncer.SyncTarget mSyncTarget;
    InputStage mSyntheticInputStage;
    private String mTag;
    final int mTargetSdkVersion;
    private final InsetsSourceControl[] mTempControls;
    HashSet<View> mTempHashSet;
    private final InsetsState mTempInsets;
    private final Rect mTempRect;
    final Thread mThread;
    private final ClientWindowFrames mTmpFrames;
    final int[] mTmpLocation;
    final TypedValue mTmpValue;
    Region mTouchableRegion;
    TranFoldInnerCustody mTranFoldInnerCustody;
    private TranPowerhubManager mTranPowerhubManager;
    private final SurfaceControl.Transaction mTransaction;
    private ArrayList<AttachedSurfaceControl.OnBufferTransformHintChangedListener> mTransformHintListeners;
    CompatibilityInfo.Translator mTranslator;
    final Region mTransparentRegion;
    int mTraversalBarrier;
    final TraversalRunnable mTraversalRunnable;
    public boolean mTraversalScheduled;
    private int mTypesHiddenByFlags;
    boolean mUnbufferedInputDispatch;
    int mUnbufferedInputSource;
    private final UnhandledKeyManager mUnhandledKeyManager;
    private final ViewRootRectTracker mUnrestrictedKeepClearRectsTracker;
    boolean mUpcomingInTouchMode;
    boolean mUpcomingWindowFocus;
    private boolean mUseBLASTAdapter;
    private boolean mUseMTRenderer;
    View mView;
    final ViewConfiguration mViewConfiguration;
    protected final ViewFrameInfo mViewFrameInfo;
    private int mViewLayoutDirectionInitial;
    int mViewVisibility;
    private final Rect mVisRect;
    int mWidth;
    boolean mWillDrawSoon;
    final Rect mWinFrame;
    final W mWindow;
    public final WindowManager.LayoutParams mWindowAttributes;
    boolean mWindowAttributesChanged;
    final ArrayList<WindowCallbacks> mWindowCallbacks;
    CountDownLatch mWindowDrawCountDown;
    boolean mWindowFocusChanged;
    private final WindowLayout mWindowLayout;
    final IWindowSession mWindowSession;
    public static boolean DBG = false;
    public static boolean LOCAL_LOGV = false;
    public static boolean DEBUG_DRAW = false;
    public static boolean DEBUG_LAYOUT = false;
    public static boolean DEBUG_DIALOG = false;
    public static boolean DEBUG_INPUT_RESIZE = false;
    public static boolean DEBUG_ORIENTATION = false;
    public static boolean DEBUG_TRACKBALL = false;
    public static boolean DEBUG_IMF = false;
    public static boolean DEBUG_CONFIGURATION = false;
    public static boolean DEBUG_FPS = false;
    public static boolean DEBUG_INPUT_STAGES = false;
    public static boolean DEBUG_KEEP_SCREEN_ON = false;
    private static final boolean DEBUG_CONTENT_CAPTURE = false;
    private static final boolean DEBUG_SCROLL_CAPTURE = false;
    private static final boolean DEBUG_BLAST = false;
    public static final boolean CAPTION_ON_SHELL = SystemProperties.getBoolean("persist.debug.caption_on_shell", false);
    public static final boolean LOCAL_LAYOUT = SystemProperties.getBoolean("persist.debug.local_layout", false);
    private static final boolean TRAN_LAYERCONTROL_SUPPORT = SystemProperties.get("persist.tran.layercontrol.support").equals("1");
    private static final int TGPA_RESOLUTION_TUNING = SystemProperties.getInt("ro.vendor.tgpa_resolution_tuner", 0);
    static final ThreadLocal<HandlerActionQueue> sRunQueues = new ThreadLocal<>();
    static final ArrayList<Runnable> sFirstDrawHandlers = new ArrayList<>();
    static boolean sFirstDrawComplete = false;
    private static final ArrayList<ConfigChangedCallback> sConfigCallbacks = new ArrayList<>();
    private static boolean sCompatibilityDone = false;
    public static final int GESTURE_NAV_HEIGHT = SystemProperties.getInt("debug.gesture_nav_height", 2);
    static final Interpolator mResizeInterpolator = new AccelerateDecelerateInterpolator();
    private static volatile boolean sAnrReported = false;
    static BLASTBufferQueue.TransactionHangCallback sTransactionHangCallback = new BLASTBufferQueue.TransactionHangCallback() { // from class: android.view.ViewRootImpl.1
        @Override // android.graphics.BLASTBufferQueue.TransactionHangCallback
        public void onTransactionHang(boolean isGPUHang) {
            if (isGPUHang && !ViewRootImpl.sAnrReported) {
                ViewRootImpl.sAnrReported = true;
                try {
                    ActivityManager.getService().appNotResponding("Buffer processing hung up due to stuck fence. Indicates GPU hang");
                } catch (RemoteException e) {
                }
            }
        }
    };
    private static boolean mInRTBlacklist = false;
    private static boolean mIsRTCloudEnable = true;
    private static boolean mRTCloudInited = false;
    private static Object mLock = new Object();

    /* loaded from: classes3.dex */
    public interface ActivityConfigCallback {
        void onConfigurationChanged(Configuration configuration, int i);

        void requestCompatCameraControl(boolean z, boolean z2, ICompatCameraControlCallback iCompatCameraControlCallback);
    }

    /* loaded from: classes3.dex */
    public interface ConfigChangedCallback {
        void onConfigurationChanged(Configuration configuration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSurfaceViewCreated(boolean created) {
        this.mIsSurfaceViewCreated = created;
        if (created && this.mSurfaceExt == null) {
            this.mSurfaceExt = SurfaceFactory.getInstance().getSurfaceExt();
        }
        Log.d(TAG, "setSurfaceViewCreated, created:" + created);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public FrameInfo getUpdatedFrameInfo() {
        FrameInfo frameInfo = this.mChoreographer.mFrameInfo;
        this.mViewFrameInfo.populateFrameInfo(frameInfo);
        this.mViewFrameInfo.reset();
        this.mInputEventAssigner.notifyFrameProcessed();
        return frameInfo;
    }

    public ImeFocusController getImeFocusController() {
        return this.mImeFocusController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static final class SystemUiVisibilityInfo {
        int globalVisibility;
        int localChanges;
        int localValue;

        SystemUiVisibilityInfo() {
        }
    }

    public HandwritingInitiator getHandwritingInitiator() {
        return this.mHandwritingInitiator;
    }

    public ViewRootImpl(Context context, Display display) {
        this(context, display, WindowManagerGlobal.getWindowSession(), false);
    }

    public ViewRootImpl(Context context, Display display, IWindowSession session) {
        this(context, display, session, false);
    }

    public ViewRootImpl(Context context, Display display, IWindowSession session, boolean useSfChoreographer) {
        InputEventConsistencyVerifier inputEventConsistencyVerifier;
        this.debug_on = "1".equals(SystemProperties.get("persist.sys.adb.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS)) || "1".equals(SystemProperties.get("persist.sys.fans.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        this.mTranPowerhubManager = null;
        this.mDelayInvalidate = false;
        this.mNextInvalidateTime = 0L;
        this.mDelayTime = 0;
        this.mTransformHintListeners = new ArrayList<>();
        this.mPreviousTransformHint = 0;
        this.mSurfaceExt = null;
        this.mIsSurfaceViewCreated = false;
        this.mWindowCallbacks = new ArrayList<>();
        this.mTmpLocation = new int[2];
        this.mTmpValue = new TypedValue();
        this.mWindowAttributes = new WindowManager.LayoutParams();
        this.mAppVisible = true;
        this.mForceDecorViewVisibility = false;
        this.mOrigWindowType = -1;
        this.mStopped = false;
        this.mIsAmbientMode = false;
        this.mPausedForTransition = false;
        this.mLastInCompatMode = false;
        this.mResizeMode = -1;
        this.mViewFrameInfo = new ViewFrameInfo();
        this.mInputEventAssigner = new InputEventAssigner();
        this.mDisplayDecorationCached = false;
        this.mSurfaceSize = new Point();
        this.mLastSurfaceSize = new Point();
        this.mVisRect = new Rect();
        this.mTempRect = new Rect();
        this.mWindowLayout = new WindowLayout();
        this.mContentCaptureEnabled = 0;
        this.mSyncBuffer = false;
        this.mSyncSeqId = 0;
        this.mLastSyncSeqId = 0;
        this.mUnbufferedInputSource = 0;
        this.mPendingInputEventQueueLengthCounterName = "pq";
        this.mUnhandledKeyManager = new UnhandledKeyManager();
        this.mWindowAttributesChanged = false;
        this.mSurface = new Surface();
        this.mSurfaceControl = new SurfaceControl();
        this.mSurfaceChangedTransaction = new SurfaceControl.Transaction();
        this.mSurfaceSession = new SurfaceSession();
        this.mTransaction = new SurfaceControl.Transaction();
        this.mTmpFrames = new ClientWindowFrames();
        this.mPendingBackDropFrame = new Rect();
        this.mTempInsets = new InsetsState();
        this.mTempControls = new InsetsSourceControl[24];
        this.mLastGivenInsets = new ViewTreeObserver.InternalInsetsInfo();
        this.mTypesHiddenByFlags = 0;
        this.mLastConfigurationFromResources = new Configuration();
        this.mLastReportedMergedConfiguration = new MergedConfiguration();
        this.mPendingMergedConfiguration = new MergedConfiguration();
        this.mDragPoint = new PointF();
        this.mLastTouchPoint = new PointF();
        this.mFpsStartTime = -1L;
        this.mFpsPrevTime = -1L;
        this.mPointerIconType = 1;
        this.mCustomPointerIcon = null;
        this.mAccessibilityInteractionConnectionManager = new AccessibilityInteractionConnectionManager();
        this.mInLayout = false;
        this.mLayoutRequesters = new ArrayList<>();
        this.mHandlingLayoutInLayoutRequest = false;
        if (!InputEventConsistencyVerifier.isInstrumentationEnabled()) {
            inputEventConsistencyVerifier = null;
        } else {
            inputEventConsistencyVerifier = new InputEventConsistencyVerifier(this, 0);
        }
        this.mInputEventConsistencyVerifier = inputEventConsistencyVerifier;
        this.mBlurRegionAggregator = new BackgroundBlurDrawable.Aggregator(this);
        this.mGestureExclusionTracker = new ViewRootRectTracker(new Function() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda10
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                List systemGestureExclusionRects;
                systemGestureExclusionRects = ((View) obj).getSystemGestureExclusionRects();
                return systemGestureExclusionRects;
            }
        });
        this.mKeepClearRectsTracker = new ViewRootRectTracker(new Function() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda11
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                List collectPreferKeepClearRects;
                collectPreferKeepClearRects = ((View) obj).collectPreferKeepClearRects();
                return collectPreferKeepClearRects;
            }
        });
        this.mUnrestrictedKeepClearRectsTracker = new ViewRootRectTracker(new Function() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                List collectUnrestrictedPreferKeepClearRects;
                collectUnrestrictedPreferKeepClearRects = ((View) obj).collectUnrestrictedPreferKeepClearRects();
                return collectUnrestrictedPreferKeepClearRects;
            }
        });
        this.mSurfaceSyncer = new SurfaceSyncer();
        this.mSyncId = -1;
        this.mNumSyncsInProgress = 0;
        this.mScrollCaptureRequestTimeout = 2500L;
        this.mSurfaceSequenceId = 0;
        this.mLastTransformHint = Integer.MIN_VALUE;
        this.mRelayoutBundle = new Bundle();
        this.mTag = TAG;
        this.mFrameScenario = new FrameScenario();
        this.mScrollScenario = new ScrollScenario();
        this.mProfile = false;
        this.mDisplayListener = new DisplayManager.DisplayListener() { // from class: android.view.ViewRootImpl.3
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                int oldDisplayState;
                int newDisplayState;
                if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mDisplay.getDisplayId() == displayId && (oldDisplayState = ViewRootImpl.this.mAttachInfo.mDisplayState) != (newDisplayState = ViewRootImpl.this.mDisplay.getState())) {
                    ViewRootImpl.this.mAttachInfo.mDisplayState = newDisplayState;
                    ViewRootImpl.this.pokeDrawLockIfNeeded();
                    if (oldDisplayState != 0) {
                        int oldScreenState = toViewScreenState(oldDisplayState);
                        int newScreenState = toViewScreenState(newDisplayState);
                        if (oldScreenState != newScreenState) {
                            ViewRootImpl.this.mView.dispatchScreenStateChanged(newScreenState);
                        }
                        if (oldDisplayState == 1) {
                            ViewRootImpl.this.mFullRedrawNeeded = true;
                            ViewRootImpl.this.scheduleTraversals();
                        }
                    }
                }
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            private int toViewScreenState(int displayState) {
                if (displayState != 1) {
                    return 1;
                }
                return 0;
            }
        };
        this.mSurfaceChangedCallbacks = new ArrayList<>();
        ViewRootHandler viewRootHandler = new ViewRootHandler();
        this.mHandler = viewRootHandler;
        this.mTraversalRunnable = new TraversalRunnable();
        this.mConsumedBatchedInputRunnable = new ConsumeBatchedInputRunnable();
        this.mConsumeBatchedInputImmediatelyRunnable = new ConsumeBatchedInputImmediatelyRunnable();
        this.mInvalidateOnAnimationRunnable = new InvalidateOnAnimationRunnable();
        this.mFrame = 0;
        this.mSyncTarget = new AnonymousClass9();
        this.mIsRtEnable = "1".equals(SystemProperties.get("ro.vendor.app_resolution_tuner"));
        this.mShowCaptionView = false;
        this.mNeedUpdateConfig = false;
        this.mTranFoldInnerCustody = new TranFoldInnerCustody();
        this.DEBUG_GESTURE = false;
        this.mLastDelayTimestamp = -1L;
        this.mContext = context;
        this.mWindowSession = session;
        this.mDisplay = display;
        this.mBasePackageName = context.getBasePackageName();
        this.mThread = Thread.currentThread();
        WindowLeaked windowLeaked = new WindowLeaked(null);
        this.mLocation = windowLeaked;
        windowLeaked.fillInStackTrace();
        this.mWidth = -1;
        this.mHeight = -1;
        this.mDirty = new Rect();
        this.mWinFrame = new Rect();
        W w = new W(this);
        this.mWindow = w;
        this.mLeashToken = new Binder();
        this.mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        this.mViewVisibility = 8;
        this.mTransparentRegion = new Region();
        this.mPreviousTransparentRegion = new Region();
        this.mFirst = true;
        this.mPerformContentCapture = true;
        this.mAdded = false;
        this.mAttachInfo = new View.AttachInfo(session, w, display, this, viewRootHandler, this, context);
        this.mCompatibleVisibilityInfo = new SystemUiVisibilityInfo();
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mHighContrastTextManager = new HighContrastTextManager();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mViewConfiguration = viewConfiguration;
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
        this.mNoncompatDensity = context.getResources().getDisplayMetrics().noncompatDensityDpi;
        this.mFallbackEventHandler = new PhoneFallbackEventHandler(context);
        this.mChoreographer = useSfChoreographer ? Choreographer.getSfInstance() : Choreographer.getInstance();
        this.mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        this.mInsetsController = new InsetsController(new ViewRootInsetsControllerHost(this));
        this.mHandwritingInitiator = new HandwritingInitiator(viewConfiguration, (InputMethodManager) context.getSystemService(InputMethodManager.class));
        String processorOverrideName = context.getResources().getString(R.string.config_inputEventCompatProcessorOverrideClassName);
        if (processorOverrideName.isEmpty()) {
            this.mInputCompatProcessor = new InputEventCompatProcessor(context);
        } else {
            InputEventCompatProcessor compatProcessor = null;
            try {
                compatProcessor = (InputEventCompatProcessor) Class.forName(processorOverrideName).getConstructor(Context.class).newInstance(context);
            } catch (Exception e) {
                Log.e(TAG, "Unable to create the InputEventCompatProcessor. ", e);
            } finally {
                this.mInputCompatProcessor = compatProcessor;
            }
        }
        if (!sCompatibilityDone) {
            sAlwaysAssignFocus = this.mTargetSdkVersion < 28;
            sCompatibilityDone = true;
        }
        loadSystemProperties();
        this.mImeFocusController = new ImeFocusController(this);
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
        this.mFastScrollSoundEffectsEnabled = audioManager.areNavigationRepeatSoundEffectsEnabled();
        this.mScrollCaptureRequestTimeout = 2500L;
        ViewDebugManager.getInstance().debugViewRootConstruct(this.mTag, context, this.mThread, this.mChoreographer, this.mTraversalRunnable, this);
        if (TRAN_LAYERCONTROL_SUPPORT) {
            TranPowerhubManager tranPowerhubManager = (TranPowerhubManager) this.mContext.getSystemService(Context.TRAN_POWERHUB_SERVICE);
            this.mTranPowerhubManager = tranPowerhubManager;
            if (tranPowerhubManager != null && tranPowerhubManager.initSkipInfo(this.mBasePackageName)) {
                this.mTranPowerhubManager.setDispSize(this.mDisplay);
            } else {
                this.mTranPowerhubManager = null;
            }
        }
        synchronized (mLock) {
            if (!mRTCloudInited) {
                mIsRTCloudEnable = getRTCloudSwitch();
                mInRTBlacklist = inRTCloudBlacklist(this.mBasePackageName);
                mRTCloudInited = true;
            }
        }
        this.mOnBackInvokedDispatcher = new WindowOnBackInvokedDispatcher(context.getApplicationInfo().isOnBackInvokedCallbackEnabled());
    }

    public static void addFirstDrawHandler(Runnable callback) {
        ArrayList<Runnable> arrayList = sFirstDrawHandlers;
        synchronized (arrayList) {
            if (!sFirstDrawComplete) {
                arrayList.add(callback);
            }
        }
    }

    public static void addConfigCallback(ConfigChangedCallback callback) {
        ArrayList<ConfigChangedCallback> arrayList = sConfigCallbacks;
        synchronized (arrayList) {
            arrayList.add(callback);
        }
    }

    public static void removeConfigCallback(ConfigChangedCallback callback) {
        ArrayList<ConfigChangedCallback> arrayList = sConfigCallbacks;
        synchronized (arrayList) {
            arrayList.remove(callback);
        }
    }

    public void setActivityConfigCallback(ActivityConfigCallback callback) {
        this.mActivityConfigCallback = callback;
    }

    public void setOnContentApplyWindowInsetsListener(Window.OnContentApplyWindowInsetsListener listener) {
        this.mAttachInfo.mContentOnApplyWindowInsetsListener = listener;
        if (!this.mFirst) {
            requestFitSystemWindows();
        }
    }

    public void addWindowCallbacks(WindowCallbacks callback) {
        this.mWindowCallbacks.add(callback);
    }

    public void removeWindowCallbacks(WindowCallbacks callback) {
        this.mWindowCallbacks.remove(callback);
    }

    public void reportDrawFinish() {
        CountDownLatch countDownLatch = this.mWindowDrawCountDown;
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public void profile() {
        this.mProfile = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isInTouchMode() {
        IWindowSession windowSession = WindowManagerGlobal.peekWindowSession();
        if (windowSession != null) {
            try {
                return windowSession.getInTouchMode();
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public void notifyChildRebuilt() {
        if (this.mView instanceof RootViewSurfaceTaker) {
            SurfaceHolder.Callback2 callback2 = this.mSurfaceHolderCallback;
            if (callback2 != null) {
                this.mSurfaceHolder.removeCallback(callback2);
            }
            SurfaceHolder.Callback2 willYouTakeTheSurface = ((RootViewSurfaceTaker) this.mView).willYouTakeTheSurface();
            this.mSurfaceHolderCallback = willYouTakeTheSurface;
            if (willYouTakeTheSurface != null) {
                TakenSurfaceHolder takenSurfaceHolder = new TakenSurfaceHolder();
                this.mSurfaceHolder = takenSurfaceHolder;
                takenSurfaceHolder.setFormat(0);
                this.mSurfaceHolder.addCallback(this.mSurfaceHolderCallback);
            } else {
                this.mSurfaceHolder = null;
            }
            InputQueue.Callback willYouTakeTheInputQueue = ((RootViewSurfaceTaker) this.mView).willYouTakeTheInputQueue();
            this.mInputQueueCallback = willYouTakeTheInputQueue;
            if (willYouTakeTheInputQueue != null) {
                willYouTakeTheInputQueue.onInputQueueCreated(this.mInputQueue);
            }
        }
    }

    private Configuration getConfiguration() {
        return this.mContext.getResources().getConfiguration();
    }

    public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
        setView(view, attrs, panelParentView, UserHandle.myUserId());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1347=5] */
    /* JADX WARN: Removed duplicated region for block: B:172:0x055c A[Catch: all -> 0x056c, TRY_ENTER, TryCatch #7 {all -> 0x056c, blocks: (B:13:0x003f, B:15:0x0046, B:17:0x004c, B:19:0x0052, B:20:0x005a, B:22:0x0067, B:24:0x0072, B:25:0x0083, B:27:0x0088, B:28:0x008b, B:30:0x00a0, B:34:0x00ac, B:36:0x00b0, B:37:0x00b5, B:39:0x00ba, B:41:0x00cb, B:43:0x00cf, B:46:0x010b, B:48:0x0111, B:49:0x0119, B:53:0x012c, B:57:0x0139, B:59:0x013d, B:60:0x014b, B:62:0x0158, B:64:0x0161, B:68:0x016c, B:70:0x0174, B:72:0x017c, B:84:0x01de, B:85:0x01e1, B:89:0x01ea, B:91:0x024e, B:92:0x0251, B:94:0x0255, B:95:0x026f, B:97:0x0273, B:99:0x028f, B:100:0x02a0, B:101:0x02a3, B:122:0x03f6, B:123:0x040c, B:102:0x02a7, B:103:0x02c7, B:104:0x02c8, B:105:0x02e8, B:106:0x02e9, B:107:0x0309, B:108:0x030a, B:109:0x032a, B:110:0x032b, B:112:0x032d, B:113:0x035b, B:114:0x035c, B:115:0x0384, B:116:0x0385, B:117:0x03a5, B:118:0x03a6, B:119:0x03d4, B:120:0x03d5, B:121:0x03f5, B:124:0x040d, B:126:0x041e, B:127:0x0421, B:129:0x0425, B:132:0x0432, B:134:0x0436, B:135:0x0442, B:137:0x0453, B:138:0x046d, B:142:0x0477, B:146:0x0480, B:148:0x048a, B:149:0x048f, B:151:0x0495, B:152:0x0499, B:154:0x051f, B:156:0x0526, B:157:0x0529, B:178:0x0565, B:172:0x055c, B:174:0x0560, B:56:0x0137, B:45:0x00d3, B:182:0x056a), top: B:192:0x0005 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView, int userId) {
        boolean restore;
        InputChannel inputChannel;
        PendingInsetsController pendingInsetsController;
        synchronized (this) {
            try {
                try {
                    if (this.mView == null) {
                        this.mView = view;
                        this.mDisplayInstallOrientation = this.mDisplay.getInstallOrientation();
                        this.mViewLayoutDirectionInitial = this.mView.getRawLayoutDirection();
                        this.mFallbackEventHandler.setView(view);
                        try {
                            this.mWindowAttributes.copyFrom(attrs);
                            if (this.mWindowAttributes.packageName == null) {
                                this.mWindowAttributes.packageName = this.mBasePackageName;
                            }
                            this.mWindowAttributes.privateFlags |= 33554432;
                            WindowManager.LayoutParams attrs2 = this.mWindowAttributes;
                            setTag();
                            if (DEBUG_KEEP_SCREEN_ON && (this.mClientWindowLayoutFlags & 128) != 0 && (attrs2.flags & 128) == 0) {
                                Slog.d(this.mTag, "setView: FLAG_KEEP_SCREEN_ON changed from true to false!");
                            }
                            this.mClientWindowLayoutFlags = attrs2.flags;
                            setAccessibilityFocus(null, null);
                            if (view instanceof RootViewSurfaceTaker) {
                                SurfaceHolder.Callback2 willYouTakeTheSurface = ((RootViewSurfaceTaker) view).willYouTakeTheSurface();
                                this.mSurfaceHolderCallback = willYouTakeTheSurface;
                                if (willYouTakeTheSurface != null) {
                                    TakenSurfaceHolder takenSurfaceHolder = new TakenSurfaceHolder();
                                    this.mSurfaceHolder = takenSurfaceHolder;
                                    takenSurfaceHolder.setFormat(0);
                                    this.mSurfaceHolder.addCallback(this.mSurfaceHolderCallback);
                                }
                            }
                            if (!attrs2.hasManualSurfaceInsets) {
                                attrs2.setSurfaceInsets(view, false, true);
                            }
                            CompatibilityInfo compatibilityInfo = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
                            this.mTranslator = compatibilityInfo.getTranslator();
                            if (this.mSurfaceHolder == null) {
                                enableHardwareAcceleration(attrs2);
                                boolean useMTRenderer = this.mAttachInfo.mThreadedRenderer != null;
                                if (this.mUseMTRenderer != useMTRenderer) {
                                    endDragResizing();
                                    this.mUseMTRenderer = useMTRenderer;
                                }
                            }
                            CompatibilityInfo.Translator translator = this.mTranslator;
                            if (translator != null) {
                                this.mSurface.setCompatibilityTranslator(translator);
                                attrs2.backup();
                                this.mTranslator.translateWindowLayout(attrs2);
                                restore = true;
                            } else {
                                restore = false;
                            }
                            boolean restore2 = DEBUG_LAYOUT;
                            if (restore2 || ViewDebugManager.DEBUG_LIFECYCLE) {
                                Log.d(this.mTag, "WindowLayout in setView:" + attrs2 + ",mView = " + this.mView + ",compatibilityInfo = " + compatibilityInfo + ", this = " + this);
                            }
                            if (!compatibilityInfo.supportsScreen()) {
                                attrs2.privateFlags |= 128;
                                this.mLastInCompatMode = true;
                            }
                            this.mSoftInputMode = attrs2.softInputMode;
                            this.mWindowAttributesChanged = true;
                            this.mAttachInfo.mRootView = view;
                            this.mAttachInfo.mScalingRequired = this.mTranslator != null;
                            View.AttachInfo attachInfo = this.mAttachInfo;
                            CompatibilityInfo.Translator translator2 = this.mTranslator;
                            attachInfo.mApplicationScale = translator2 == null ? 1.0f : translator2.applicationScale;
                            if (panelParentView != null) {
                                this.mAttachInfo.mPanelParentWindowToken = panelParentView.getApplicationWindowToken();
                                this.mParentViewRoot = panelParentView.getViewRootImpl();
                            }
                            this.mAdded = true;
                            requestLayout();
                            if ((this.mWindowAttributes.inputFeatures & 1) == 0) {
                                InputChannel inputChannel2 = new InputChannel();
                                inputChannel = inputChannel2;
                            } else {
                                inputChannel = null;
                            }
                            this.mForceDecorViewVisibility = (this.mWindowAttributes.privateFlags & 16384) != 0;
                            View view2 = this.mView;
                            if ((view2 instanceof RootViewSurfaceTaker) && (pendingInsetsController = ((RootViewSurfaceTaker) view2).providePendingInsetsController()) != null) {
                                pendingInsetsController.replayAndAttach(this.mInsetsController);
                            }
                            try {
                                this.mOrigWindowType = this.mWindowAttributes.type;
                                this.mAttachInfo.mRecomputeGlobalAttributes = true;
                                collectViewAttributes();
                                adjustLayoutParamsForCompatibility(this.mWindowAttributes);
                                controlInsetsForCompatibility(this.mWindowAttributes);
                                InputChannel inputChannel3 = inputChannel;
                                try {
                                    int res = this.mWindowSession.addToDisplayAsUser(this.mWindow, this.mWindowAttributes, getHostVisibility(), this.mDisplay.getDisplayId(), userId, this.mInsetsController.getRequestedVisibilities(), inputChannel3, this.mTempInsets, this.mTempControls);
                                    CompatibilityInfo.Translator translator3 = this.mTranslator;
                                    if (translator3 != null) {
                                        try {
                                            translator3.translateInsetsStateInScreenToAppWindow(this.mTempInsets);
                                            this.mTranslator.translateSourceControlsInScreenToAppWindow(this.mTempControls);
                                        } catch (RemoteException e) {
                                            e = e;
                                            try {
                                                this.mAdded = false;
                                                this.mView = null;
                                                this.mAttachInfo.mRootView = null;
                                                this.mFallbackEventHandler.setView(null);
                                                unscheduleTraversals();
                                                setAccessibilityFocus(null, null);
                                                throw new RuntimeException("Adding window failed", e);
                                            } catch (Throwable th) {
                                                e = th;
                                                if (restore) {
                                                    attrs2.restore();
                                                }
                                                throw e;
                                            }
                                        } catch (Throwable th2) {
                                            e = th2;
                                            if (restore) {
                                            }
                                            throw e;
                                        }
                                    }
                                    if (restore) {
                                        attrs2.restore();
                                    }
                                    this.mAttachInfo.mAlwaysConsumeSystemBars = (res & 4) != 0;
                                    this.mPendingAlwaysConsumeSystemBars = this.mAttachInfo.mAlwaysConsumeSystemBars;
                                    this.mInsetsController.onStateChanged(this.mTempInsets);
                                    this.mInsetsController.onControlsChanged(this.mTempControls);
                                    InsetsState state = this.mInsetsController.getState();
                                    Rect displayCutoutSafe = this.mTempRect;
                                    state.getDisplayCutoutSafe(displayCutoutSafe);
                                    WindowConfiguration winConfig = getConfiguration().windowConfiguration;
                                    this.mWindowLayout.computeFrames(this.mWindowAttributes, state, displayCutoutSafe, winConfig.getBounds(), winConfig.getWindowingMode(), -1, -1, this.mInsetsController.getRequestedVisibilities(), getAttachedWindowFrame(), 1.0f, this.mTmpFrames);
                                    setFrame(this.mTmpFrames.frame);
                                    registerBackCallbackOnWindow();
                                    if (!WindowOnBackInvokedDispatcher.isOnBackInvokedCallbackEnabled(this.mContext)) {
                                        registerCompatOnBackInvokedCallback();
                                    }
                                    if (DEBUG_LAYOUT) {
                                        Log.v(this.mTag, "Added window " + this.mWindow);
                                    }
                                    if (DEBUG_LAYOUT) {
                                        Log.v(this.mTag, "Added window " + this.mWindow);
                                    }
                                    if (res < 0) {
                                        this.mAttachInfo.mRootView = null;
                                        this.mAdded = false;
                                        this.mFallbackEventHandler.setView(null);
                                        unscheduleTraversals();
                                        setAccessibilityFocus(null, null);
                                        switch (res) {
                                            case -11:
                                                throw new WindowManager.BadTokenException("Unable to add Window " + this.mWindow + " -- requested userId is not valid");
                                            case -10:
                                                throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified window type " + this.mWindowAttributes.type + " is not valid");
                                            case -9:
                                                throw new WindowManager.InvalidDisplayException("Unable to add window " + this.mWindow + " -- the specified display can not be found");
                                            case -8:
                                                throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- permission denied for window type " + this.mWindowAttributes.type);
                                            case -7:
                                                throw new WindowManager.BadTokenException("Unable to add window " + this.mWindow + " -- another window of type " + this.mWindowAttributes.type + " already exists");
                                            case -6:
                                                return;
                                            case -5:
                                                throw new WindowManager.BadTokenException("Unable to add window -- window " + this.mWindow + " has already been added");
                                            case -4:
                                                throw new WindowManager.BadTokenException("Unable to add window -- app for token " + attrs2.token + " is exiting");
                                            case -3:
                                                throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs2.token + " is not for an application");
                                            case -2:
                                            case -1:
                                                throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs2.token + " is not valid; is your activity running?");
                                            default:
                                                throw new RuntimeException("Unable to add window -- unknown error code " + res);
                                        }
                                    }
                                    registerListeners();
                                    this.mAttachInfo.mDisplayState = this.mDisplay.getState();
                                    if ((res & 8) != 0) {
                                        this.mUseBLASTAdapter = true;
                                    }
                                    if (view instanceof RootViewSurfaceTaker) {
                                        this.mInputQueueCallback = ((RootViewSurfaceTaker) view).willYouTakeTheInputQueue();
                                    }
                                    if (inputChannel3 != null) {
                                        if (this.mInputQueueCallback != null) {
                                            InputQueue inputQueue = new InputQueue();
                                            this.mInputQueue = inputQueue;
                                            this.mInputQueueCallback.onInputQueueCreated(inputQueue);
                                        }
                                        this.mInputEventReceiver = new WindowInputEventReceiver(inputChannel3, Looper.myLooper());
                                        if (this.mAttachInfo.mThreadedRenderer != null) {
                                            InputMetricsListener listener = new InputMetricsListener();
                                            this.mHardwareRendererObserver = new HardwareRendererObserver(listener, listener.data, this.mHandler, true);
                                            this.mAttachInfo.mThreadedRenderer.addObserver(this.mHardwareRendererObserver);
                                        }
                                    }
                                    view.assignParent(this);
                                    this.mAddedTouchMode = (res & 1) != 0;
                                    this.mAppVisible = (res & 2) != 0;
                                    if (this.mAccessibilityManager.isEnabled()) {
                                        this.mAccessibilityInteractionConnectionManager.ensureConnection();
                                    }
                                    if (view.getImportantForAccessibility() == 0) {
                                        view.setImportantForAccessibility(1);
                                    }
                                    CharSequence counterSuffix = attrs2.getTitle();
                                    SyntheticInputStage syntheticInputStage = new SyntheticInputStage();
                                    this.mSyntheticInputStage = syntheticInputStage;
                                    InputStage viewPostImeStage = new ViewPostImeInputStage(syntheticInputStage);
                                    InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage, "aq:native-post-ime:" + ((Object) counterSuffix));
                                    InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
                                    InputStage imeStage = new ImeInputStage(earlyPostImeStage, "aq:ime:" + ((Object) counterSuffix));
                                    InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);
                                    InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage, "aq:native-pre-ime:" + ((Object) counterSuffix));
                                    this.mFirstInputStage = nativePreImeStage;
                                    this.mFirstPostImeInputStage = earlyPostImeStage;
                                    this.mPendingInputEventQueueLengthCounterName = "aq:pending:" + ((Object) counterSuffix);
                                    if (this.mTranPowerhubManager != null && 1 != this.mWindowAttributes.type) {
                                        this.mTranPowerhubManager = null;
                                    }
                                    AnimationHandler.requestAnimatorsEnabled(this.mAppVisible, this);
                                } catch (RemoteException e2) {
                                    e = e2;
                                } catch (Throwable th3) {
                                    e = th3;
                                }
                            } catch (RemoteException e3) {
                                e = e3;
                            } catch (Throwable th4) {
                                e = th4;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            throw th;
                        }
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    private Rect getAttachedWindowFrame() {
        int type = this.mWindowAttributes.type;
        ViewRootImpl viewRootImpl = this.mParentViewRoot;
        boolean layoutAttached = viewRootImpl != null && type >= 1000 && type <= 1999 && type != 1003;
        if (layoutAttached) {
            return viewRootImpl.mWinFrame;
        }
        return null;
    }

    private void registerListeners() {
        this.mAccessibilityManager.addAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager, this.mHandler);
        this.mAccessibilityManager.addHighTextContrastStateChangeListener(this.mHighContrastTextManager, this.mHandler);
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
    }

    private void unregisterListeners() {
        this.mAccessibilityManager.removeAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager);
        this.mAccessibilityManager.removeHighTextContrastStateChangeListener(this.mHighContrastTextManager);
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
    }

    private void setTag() {
        String[] split = this.mWindowAttributes.getTitle().toString().split("\\.");
        if (split.length > 0) {
            this.mTag = "VRI[" + split[split.length - 1] + NavigationBarInflaterView.SIZE_MOD_END;
        }
    }

    public int getWindowFlags() {
        return this.mWindowAttributes.flags;
    }

    public int getDisplayId() {
        return this.mDisplay.getDisplayId();
    }

    public CharSequence getTitle() {
        return this.mWindowAttributes.getTitle();
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public boolean isRtEnable() {
        return (this.mIsRtEnable || TGPA_RESOLUTION_TUNING == 1) && mIsRTCloudEnable && !mInRTBlacklist;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyHardwareResources() {
        ThreadedRenderer renderer = this.mAttachInfo.mThreadedRenderer;
        if (renderer != null) {
            if (Looper.myLooper() != this.mAttachInfo.mHandler.getLooper()) {
                this.mAttachInfo.mHandler.postAtFrontOfQueue(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda14
                    @Override // java.lang.Runnable
                    public final void run() {
                        ViewRootImpl.this.destroyHardwareResources();
                    }
                });
                return;
            }
            renderer.destroyHardwareResources(this.mView);
            renderer.destroy();
        }
    }

    public void detachFunctor(long functor) {
    }

    public static void invokeFunctor(long functor, boolean waitForCompletion) {
    }

    public void registerAnimatingRenderNode(RenderNode animator) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.registerAnimatingRenderNode(animator);
            return;
        }
        if (this.mAttachInfo.mPendingAnimatingRenderNodes == null) {
            this.mAttachInfo.mPendingAnimatingRenderNodes = new ArrayList();
        }
        this.mAttachInfo.mPendingAnimatingRenderNodes.add(animator);
    }

    public void registerVectorDrawableAnimator(NativeVectorDrawableAnimator animator) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.registerVectorDrawableAnimator(animator);
        }
    }

    public void registerRtFrameCallback(final HardwareRenderer.FrameDrawingCallback callback) {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: android.view.ViewRootImpl.2
                @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
                public void onFrameDraw(long frame) {
                }

                @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
                public HardwareRenderer.FrameCommitCallback onFrameDraw(int syncResult, long frame) {
                    try {
                        return callback.onFrameDraw(syncResult, frame);
                    } catch (Exception e) {
                        Log.e(ViewRootImpl.TAG, "Exception while executing onFrameDraw", e);
                        return null;
                    }
                }
            });
        }
    }

    private void enableHardwareAcceleration(WindowManager.LayoutParams attrs) {
        boolean translucent = false;
        this.mAttachInfo.mHardwareAccelerated = false;
        this.mAttachInfo.mHardwareAccelerationRequested = false;
        if (this.mTranslator != null) {
            return;
        }
        boolean hardwareAccelerated = (attrs.flags & 16777216) != 0;
        if (ViewDebugManager.getInstance().debugForceHWDraw(hardwareAccelerated)) {
            boolean forceHwAccelerated = (attrs.privateFlags & 2) != 0;
            if (ThreadedRenderer.sRendererEnabled || forceHwAccelerated) {
                if (this.mAttachInfo.mThreadedRenderer != null) {
                    this.mAttachInfo.mThreadedRenderer.destroy();
                }
                Rect insets = attrs.surfaceInsets;
                boolean hasSurfaceInsets = (insets.left == 0 && insets.right == 0 && insets.top == 0 && insets.bottom == 0) ? false : true;
                if (attrs.format != -1 || hasSurfaceInsets) {
                    translucent = true;
                }
                this.mAttachInfo.mThreadedRenderer = ThreadedRenderer.create(this.mContext, translucent, attrs.getTitle().toString());
                updateColorModeIfNeeded(attrs.getColorMode());
                updateForceDarkMode();
                if (this.mAttachInfo.mThreadedRenderer != null) {
                    View.AttachInfo attachInfo = this.mAttachInfo;
                    attachInfo.mHardwareAccelerationRequested = true;
                    attachInfo.mHardwareAccelerated = true;
                    if (this.mHardwareRendererObserver != null) {
                        this.mAttachInfo.mThreadedRenderer.addObserver(this.mHardwareRendererObserver);
                    }
                    this.mAttachInfo.mThreadedRenderer.setSurfaceControl(this.mSurfaceControl);
                    this.mAttachInfo.mThreadedRenderer.setBlastBufferQueue(this.mBlastBufferQueue);
                }
            }
            boolean translucent2 = ViewDebugManager.DEBUG_USER;
            if (translucent2) {
                Log.d(this.mTag, "hardware acceleration = " + this.mAttachInfo.mHardwareAccelerated + ", forceHwAccelerated = " + forceHwAccelerated);
            }
        }
    }

    private int getNightMode() {
        return getConfiguration().uiMode & 48;
    }

    private void updateForceDarkMode() {
        if (this.mAttachInfo.mThreadedRenderer == null) {
            return;
        }
        boolean z = true;
        boolean useAutoDark = getNightMode() == 32;
        if (useAutoDark) {
            boolean forceDarkAllowedDefault = SystemProperties.getBoolean(ThreadedRenderer.DEBUG_FORCE_DARK, false);
            TypedArray a = this.mContext.obtainStyledAttributes(R.styleable.Theme);
            if (!a.getBoolean(279, true) || !a.getBoolean(278, forceDarkAllowedDefault)) {
                z = false;
            }
            useAutoDark = z;
            a.recycle();
        }
        if (this.mAttachInfo.mThreadedRenderer.setForceDark(useAutoDark)) {
            invalidateWorld(this.mView);
        }
    }

    public View getView() {
        return this.mView;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final WindowLeaked getLocation() {
        return this.mLocation;
    }

    public void setLayoutParams(WindowManager.LayoutParams attrs, boolean newView) {
        int oldInsetLeft;
        synchronized (this) {
            int oldInsetLeft2 = this.mWindowAttributes.surfaceInsets.left;
            int oldInsetTop = this.mWindowAttributes.surfaceInsets.top;
            int oldInsetRight = this.mWindowAttributes.surfaceInsets.right;
            int oldInsetBottom = this.mWindowAttributes.surfaceInsets.bottom;
            int oldSoftInputMode = this.mWindowAttributes.softInputMode;
            boolean oldHasManualSurfaceInsets = this.mWindowAttributes.hasManualSurfaceInsets;
            if (DEBUG_KEEP_SCREEN_ON && (this.mClientWindowLayoutFlags & 128) != 0 && (attrs.flags & 128) == 0) {
                Slog.d(this.mTag, "setLayoutParams: FLAG_KEEP_SCREEN_ON from true to false!");
            }
            this.mClientWindowLayoutFlags = attrs.flags;
            int compatibleWindowFlag = this.mWindowAttributes.privateFlags & 128;
            int systemUiVisibility = this.mWindowAttributes.systemUiVisibility;
            int subtreeSystemUiVisibility = this.mWindowAttributes.subtreeSystemUiVisibility;
            int appearance = this.mWindowAttributes.insetsFlags.appearance;
            int behavior = this.mWindowAttributes.insetsFlags.behavior;
            int appearanceAndBehaviorPrivateFlags = this.mWindowAttributes.privateFlags & Enums.AUDIO_FORMAT_DTS_HD;
            int changes = this.mWindowAttributes.copyFrom(attrs);
            if ((524288 & changes) != 0) {
                this.mAttachInfo.mRecomputeGlobalAttributes = true;
            }
            if ((changes & 1) != 0) {
                this.mAttachInfo.mNeedsUpdateLightCenter = true;
            }
            if (this.mWindowAttributes.packageName == null) {
                this.mWindowAttributes.packageName = this.mBasePackageName;
            }
            this.mWindowAttributes.systemUiVisibility = systemUiVisibility;
            this.mWindowAttributes.subtreeSystemUiVisibility = subtreeSystemUiVisibility;
            this.mWindowAttributes.insetsFlags.appearance = appearance;
            this.mWindowAttributes.insetsFlags.behavior = behavior;
            this.mWindowAttributes.privateFlags |= compatibleWindowFlag | appearanceAndBehaviorPrivateFlags | 33554432;
            if (this.mWindowAttributes.preservePreviousSurfaceInsets) {
                this.mWindowAttributes.surfaceInsets.set(oldInsetLeft2, oldInsetTop, oldInsetRight, oldInsetBottom);
                this.mWindowAttributes.hasManualSurfaceInsets = oldHasManualSurfaceInsets;
            } else if (this.mWindowAttributes.surfaceInsets.left != oldInsetLeft2 || this.mWindowAttributes.surfaceInsets.top != oldInsetTop || this.mWindowAttributes.surfaceInsets.right != oldInsetRight || this.mWindowAttributes.surfaceInsets.bottom != oldInsetBottom) {
                this.mNeedsRendererSetup = true;
            }
            applyKeepScreenOnFlag(this.mWindowAttributes);
            if (newView) {
                this.mSoftInputMode = attrs.softInputMode;
                requestLayout();
            }
            if ((attrs.softInputMode & 240) == 0) {
                WindowManager.LayoutParams layoutParams = this.mWindowAttributes;
                oldInsetLeft = oldSoftInputMode;
                layoutParams.softInputMode = (oldInsetLeft & 240) | (layoutParams.softInputMode & (-241));
            } else {
                oldInsetLeft = oldSoftInputMode;
            }
            if (this.mWindowAttributes.softInputMode != oldInsetLeft) {
                requestFitSystemWindows();
            }
            this.mWindowAttributesChanged = true;
            scheduleTraversals();
        }
        if (DEBUG_IMF) {
            Log.d(this.mTag, "setLayoutParams: attrs = " + attrs + ", mSoftInputMode = " + this.mSoftInputMode + ", mWindowAttributes = " + this.mWindowAttributes + ", this = " + this);
        }
    }

    void handleAppVisibility(boolean visible) {
        if (DEBUG_LAYOUT) {
            Log.d(this.mTag, "handleAppVisibility: visible=" + visible + ", mAppVisible=" + this.mAppVisible + ", this = " + this);
        }
        if (this.mAppVisible != visible) {
            boolean previousVisible = getHostVisibility() == 0;
            this.mAppVisible = visible;
            boolean currentVisible = getHostVisibility() == 0;
            if (previousVisible != currentVisible) {
                this.mAppVisibilityChanged = true;
                scheduleTraversals();
            }
            if (!this.mAppVisible) {
                WindowManagerGlobal.trimForeground();
            }
            AnimationHandler.requestAnimatorsEnabled(this.mAppVisible, this);
        }
    }

    void handleGetNewSurface() {
        this.mNewSurfaceNeeded = true;
        this.mFullRedrawNeeded = true;
        scheduleTraversals();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleResized(int msg, SomeArgs args) {
        boolean z;
        if (!this.mAdded) {
            return;
        }
        ClientWindowFrames frames = (ClientWindowFrames) args.arg1;
        MergedConfiguration mergedConfiguration = (MergedConfiguration) args.arg2;
        boolean forceNextWindowRelayout = args.argi1 != 0;
        int displayId = args.argi3;
        int resizeMode = args.argi5;
        Rect frame = frames.frame;
        Rect displayFrame = frames.displayFrame;
        CompatibilityInfo.Translator translator = this.mTranslator;
        if (translator != null) {
            translator.translateRectInScreenToAppWindow(frame);
            this.mTranslator.translateRectInScreenToAppWindow(displayFrame);
        }
        boolean frameChanged = !this.mWinFrame.equals(frame);
        boolean configChanged = !this.mLastReportedMergedConfiguration.equals(mergedConfiguration);
        boolean displayChanged = this.mDisplay.getDisplayId() != displayId;
        boolean resizeModeChanged = this.mResizeMode != resizeMode;
        if (msg != 4 || frameChanged || configChanged || displayChanged || resizeModeChanged || forceNextWindowRelayout) {
            this.mPendingDragResizing = resizeMode != -1;
            this.mResizeMode = resizeMode;
            if (configChanged) {
                this.mTranFoldInnerCustody.handleResizedConfigurationChanged(mergedConfiguration);
                performConfigurationChange(mergedConfiguration, false, displayChanged ? displayId : -1);
            } else if (displayChanged) {
                onMovedToDisplay(displayId, this.mLastConfigurationFromResources);
            }
            setFrame(frame);
            this.mTmpFrames.displayFrame.set(displayFrame);
            if (this.mDragResizing && this.mUseMTRenderer) {
                boolean fullscreen = frame.equals(this.mPendingBackDropFrame);
                z = true;
                int i = this.mWindowCallbacks.size() - 1;
                while (i >= 0) {
                    this.mWindowCallbacks.get(i).onWindowSizeIsChanging(this.mPendingBackDropFrame, fullscreen, this.mAttachInfo.mVisibleInsets, this.mAttachInfo.mStableInsets);
                    i--;
                    frames = frames;
                    mergedConfiguration = mergedConfiguration;
                    displayId = displayId;
                    resizeMode = resizeMode;
                }
            } else {
                z = true;
            }
            this.mForceNextWindowRelayout = forceNextWindowRelayout;
            this.mPendingAlwaysConsumeSystemBars = args.argi2 != 0 ? z : false;
            int i2 = args.argi4;
            int i3 = this.mSyncSeqId;
            if (i2 > i3) {
                i3 = args.argi4;
            }
            this.mSyncSeqId = i3;
            if (msg == 5) {
                reportNextDraw();
            }
            View view = this.mView;
            if (view != null && (frameChanged || configChanged)) {
                forceLayout(view);
            }
            requestLayout();
        }
    }

    public void onMovedToDisplay(int displayId, Configuration config) {
        if (this.mDisplay.getDisplayId() == displayId) {
            return;
        }
        updateInternalDisplay(displayId, this.mView.getResources());
        this.mImeFocusController.onMovedToDisplay();
        this.mAttachInfo.mDisplayState = this.mDisplay.getState();
        this.mDisplayInstallOrientation = this.mDisplay.getInstallOrientation();
        this.mView.dispatchMovedToDisplay(this.mDisplay, config);
    }

    private void updateInternalDisplay(int displayId, Resources resources) {
        Display preferredDisplay = ResourcesManager.getInstance().getAdjustedDisplay(displayId, resources);
        if (preferredDisplay == null) {
            Slog.w(TAG, "Cannot get desired display with Id: " + displayId);
            this.mDisplay = ResourcesManager.getInstance().getAdjustedDisplay(0, resources);
        } else {
            this.mDisplay = preferredDisplay;
        }
        this.mContext.updateDisplay(this.mDisplay.getDisplayId());
    }

    void pokeDrawLockIfNeeded() {
        if (Display.isDozeState(this.mAttachInfo.mDisplayState) && this.mWindowAttributes.type == 1 && this.mAdded && this.mTraversalScheduled && this.mAttachInfo.mHasWindowFocus) {
            try {
                this.mWindowSession.pokeDrawLock(this.mWindow);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // android.view.ViewParent
    public void requestFitSystemWindows() {
        checkThread();
        this.mApplyInsetsRequested = true;
        scheduleTraversals();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInsetsChanged() {
        this.mApplyInsetsRequested = true;
        requestLayout();
        if (View.sForceLayoutWhenInsetsChanged && this.mView != null && (this.mWindowAttributes.softInputMode & 240) == 16) {
            forceLayout(this.mView);
        }
        if (!this.mIsInTraversal) {
            scheduleTraversals();
        }
    }

    @Override // android.view.ViewParent
    public void requestLayout() {
        if (!this.mHandlingLayoutInLayoutRequest) {
            if (ViewDebugManager.DEBUG_REQUESTLAYOUT) {
                Log.d(this.mTag, "requestLayout: mView = " + this.mView + ", this = " + this, new Throwable("requestLayout"));
            }
            checkThread();
            this.mLayoutRequested = true;
            scheduleTraversals();
        }
    }

    @Override // android.view.ViewParent
    public boolean isLayoutRequested() {
        return this.mLayoutRequested;
    }

    @Override // android.view.ViewParent
    public void onDescendantInvalidated(View child, View descendant) {
        TranPowerhubManager tranPowerhubManager;
        if ((descendant.mPrivateFlags & 64) != 0) {
            this.mIsAnimating = true;
        }
        boolean mIsViewSkipDisabledDebug = SystemProperties.get("debug.product.viewskip.disabled").equals("1");
        boolean mIsViewSkipDisabled = SystemProperties.get("persist.sys.tran.viewskip.disabled").equals("1");
        if (!mIsViewSkipDisabledDebug && !mIsViewSkipDisabled && TRAN_LAYERCONTROL_SUPPORT && (tranPowerhubManager = this.mTranPowerhubManager) != null) {
            int delayTime = tranPowerhubManager.shouldSkip(this.mWindowAttributes.packageName, this.mWindowAttributes.getTitle().toString(), descendant);
            if (delayTime >= 0) {
                if (this.mDelayInvalidate) {
                    this.mHandler.removeMessages(37);
                    this.mDelayInvalidate = false;
                }
                if (delayTime != 0) {
                    this.mDelayTime = delayTime;
                    Message msg = this.mHandler.obtainMessage(37);
                    this.mHandler.sendMessageDelayed(msg, this.mDelayTime);
                    this.mDelayInvalidate = true;
                }
            } else {
                return;
            }
        }
        invalidate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidate() {
        this.mDirty.set(0, 0, this.mWidth, this.mHeight);
        if (!this.mWillDrawSoon) {
            scheduleTraversals();
        }
    }

    void invalidateWorld(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                invalidateWorld(parent.getChildAt(i));
            }
        }
    }

    @Override // android.view.ViewParent
    public void invalidateChild(View child, Rect dirty) {
        invalidateChildInParent(null, dirty);
    }

    @Override // android.view.ViewParent
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        checkThread();
        if (DEBUG_DRAW) {
            Log.v(this.mTag, "Invalidate child: " + dirty);
        }
        if (dirty == null) {
            invalidate();
            return null;
        } else if (dirty.isEmpty() && !this.mIsAnimating) {
            return null;
        } else {
            if (this.mCurScrollY != 0 || this.mTranslator != null) {
                this.mTempRect.set(dirty);
                dirty = this.mTempRect;
                int i = this.mCurScrollY;
                if (i != 0) {
                    dirty.offset(0, -i);
                }
                CompatibilityInfo.Translator translator = this.mTranslator;
                if (translator != null) {
                    translator.translateRectInAppWindowToScreen(dirty);
                }
                if (this.mAttachInfo.mScalingRequired) {
                    dirty.inset(-1, -1);
                }
            }
            invalidateRectOnScreen(dirty);
            return null;
        }
    }

    private void invalidateRectOnScreen(Rect dirty) {
        Rect localDirty = this.mDirty;
        localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
        float appScale = this.mAttachInfo.mApplicationScale;
        boolean intersected = localDirty.intersect(0, 0, (int) ((this.mWidth * appScale) + 0.5f), (int) ((this.mHeight * appScale) + 0.5f));
        if (!intersected) {
            localDirty.setEmpty();
        }
        if (!this.mWillDrawSoon && (intersected || this.mIsAnimating)) {
            scheduleTraversals();
        } else if (DEBUG_DRAW) {
            Log.v(this.mTag, "Invalidate child: Do not scheduleTraversals, mWillDrawSoon =" + this.mWillDrawSoon + ", intersected =" + intersected + ", mIsAnimating =" + this.mIsAnimating);
        }
    }

    public void setIsAmbientMode(boolean ambient) {
        this.mIsAmbientMode = ambient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowStopped(boolean stopped) {
        TranPowerhubManager tranPowerhubManager;
        checkThread();
        if (this.mStopped != stopped) {
            this.mStopped = stopped;
            ThreadedRenderer renderer = this.mAttachInfo.mThreadedRenderer;
            if (renderer != null) {
                if (DEBUG_DRAW) {
                    Log.d(this.mTag, "WindowStopped on " + ((Object) getTitle()) + " set to " + this.mStopped);
                }
                renderer.setStopped(this.mStopped);
            }
            if (!this.mStopped) {
                this.mAppVisibilityChanged = true;
                scheduleTraversals();
            } else {
                if (renderer != null) {
                    renderer.destroyHardwareResources(this.mView);
                }
                if (this.mSurface.isValid()) {
                    if (this.mSurfaceHolder != null) {
                        notifyHolderSurfaceDestroyed();
                    }
                    notifySurfaceDestroyed();
                }
                destroySurface();
            }
            if (TRAN_LAYERCONTROL_SUPPORT && (tranPowerhubManager = this.mTranPowerhubManager) != null) {
                tranPowerhubManager.updateCollectData(this.mStopped);
            }
        }
    }

    /* loaded from: classes3.dex */
    public interface SurfaceChangedCallback {
        void surfaceCreated(SurfaceControl.Transaction transaction);

        void surfaceDestroyed();

        void surfaceReplaced(SurfaceControl.Transaction transaction);

        default void surfaceSyncStarted() {
        }
    }

    public void addSurfaceChangedCallback(SurfaceChangedCallback c) {
        this.mSurfaceChangedCallbacks.add(c);
    }

    public void removeSurfaceChangedCallback(SurfaceChangedCallback c) {
        this.mSurfaceChangedCallbacks.remove(c);
    }

    private void notifySurfaceCreated() {
        for (int i = 0; i < this.mSurfaceChangedCallbacks.size(); i++) {
            this.mSurfaceChangedCallbacks.get(i).surfaceCreated(this.mSurfaceChangedTransaction);
        }
    }

    private void notifySurfaceReplaced() {
        for (int i = 0; i < this.mSurfaceChangedCallbacks.size(); i++) {
            this.mSurfaceChangedCallbacks.get(i).surfaceReplaced(this.mSurfaceChangedTransaction);
        }
    }

    private void notifySurfaceDestroyed() {
        for (int i = 0; i < this.mSurfaceChangedCallbacks.size(); i++) {
            this.mSurfaceChangedCallbacks.get(i).surfaceDestroyed();
        }
    }

    private void notifySurfaceSyncStarted() {
        for (int i = 0; i < this.mSurfaceChangedCallbacks.size(); i++) {
            this.mSurfaceChangedCallbacks.get(i).surfaceSyncStarted();
        }
    }

    public SurfaceControl getBoundsLayer() {
        if (this.mBoundsLayer == null) {
            this.mBoundsLayer = new SurfaceControl.Builder(this.mSurfaceSession).setContainerLayer().setName("Bounds for - " + getTitle().toString()).setParent(getSurfaceControl()).setCallsite("ViewRootImpl.getBoundsLayer").build();
            setBoundsLayerCrop(this.mTransaction);
            this.mTransaction.show(this.mBoundsLayer).apply();
        }
        return this.mBoundsLayer;
    }

    void updateBlastSurfaceIfNeeded() {
        if (!this.mSurfaceControl.isValid()) {
            return;
        }
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null && bLASTBufferQueue.isSameSurfaceControl(this.mSurfaceControl)) {
            this.mBlastBufferQueue.update(this.mSurfaceControl, this.mSurfaceSize.x, this.mSurfaceSize.y, this.mWindowAttributes.format);
            return;
        }
        BLASTBufferQueue bLASTBufferQueue2 = this.mBlastBufferQueue;
        if (bLASTBufferQueue2 != null) {
            bLASTBufferQueue2.destroy();
        }
        BLASTBufferQueue bLASTBufferQueue3 = new BLASTBufferQueue(this.mTag, this.mSurfaceControl, this.mSurfaceSize.x, this.mSurfaceSize.y, this.mWindowAttributes.format);
        this.mBlastBufferQueue = bLASTBufferQueue3;
        bLASTBufferQueue3.setTransactionHangCallback(sTransactionHangCallback);
        Surface blastSurface = this.mBlastBufferQueue.createSurface();
        this.mSurface.transferFrom(blastSurface);
    }

    private void setBoundsLayerCrop(SurfaceControl.Transaction t) {
        this.mTempRect.set(0, 0, this.mSurfaceSize.x, this.mSurfaceSize.y);
        this.mTempRect.inset(this.mWindowAttributes.surfaceInsets.left, this.mWindowAttributes.surfaceInsets.top, this.mWindowAttributes.surfaceInsets.right, this.mWindowAttributes.surfaceInsets.bottom);
        t.setWindowCrop(this.mBoundsLayer, this.mTempRect);
    }

    private boolean updateBoundsLayer(SurfaceControl.Transaction t) {
        if (this.mBoundsLayer != null) {
            setBoundsLayerCrop(t);
            return true;
        }
        return false;
    }

    private void prepareSurfaces() {
        SurfaceControl.Transaction t = this.mTransaction;
        SurfaceControl sc = getSurfaceControl();
        if (sc.isValid() && updateBoundsLayer(t)) {
            m5128lambda$applyTransactionOnDraw$11$androidviewViewRootImpl(t, this.mSurface.getNextFrameNumber());
        }
    }

    private void destroySurface() {
        SurfaceControl surfaceControl = this.mBoundsLayer;
        if (surfaceControl != null) {
            surfaceControl.release();
            this.mBoundsLayer = null;
        }
        this.mSurface.release();
        this.mSurfaceControl.release();
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null) {
            bLASTBufferQueue.destroy();
            this.mBlastBufferQueue = null;
        }
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.setSurfaceControl(null);
            this.mAttachInfo.mThreadedRenderer.setBlastBufferQueue(null);
        }
    }

    public void setPausedForTransition(boolean paused) {
        this.mPausedForTransition = paused;
    }

    @Override // android.view.ViewParent
    public ViewParent getParent() {
        return null;
    }

    @Override // android.view.ViewParent
    public boolean getChildVisibleRect(View child, Rect r, Point offset) {
        if (child != this.mView) {
            throw new RuntimeException("child is not mine, honest!");
        }
        return r.intersect(0, 0, this.mWidth, this.mHeight);
    }

    @Override // android.view.ViewParent
    public void bringChildToFront(View child) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getHostVisibility() {
        View view = this.mView;
        if (view == null || !(this.mAppVisible || this.mForceDecorViewVisibility)) {
            return 8;
        }
        return view.getVisibility();
    }

    public void requestTransitionStart(LayoutTransition transition) {
        ArrayList<LayoutTransition> arrayList = this.mPendingTransitions;
        if (arrayList == null || !arrayList.contains(transition)) {
            if (this.mPendingTransitions == null) {
                this.mPendingTransitions = new ArrayList<>();
            }
            this.mPendingTransitions.add(transition);
        }
    }

    void notifyRendererOfFramePending() {
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.notifyFramePending();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleTraversals() {
        if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
            Trace.traceBegin(8L, "scheduleTraversals In");
        }
        if (!this.mTraversalScheduled) {
            this.mTraversalScheduled = true;
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceBegin(8L, "scheduleTraversals occurred");
            }
            this.mTraversalBarrier = this.mHandler.getLooper().getQueue().postSyncBarrier();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Log.v(this.mTag, "scheduleTraversals: mTraversalBarrier = " + this.mTraversalBarrier + ",this = " + this, new Throwable("scheduleTraversals"));
            }
            this.mChoreographer.postCallback(3, this.mTraversalRunnable, null);
            notifyRendererOfFramePending();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceEnd(8L);
            }
            pokeDrawLockIfNeeded();
        }
        if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
            Trace.traceEnd(8L);
        }
    }

    void unscheduleTraversals() {
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().getQueue().removeSyncBarrier(this.mTraversalBarrier);
            this.mChoreographer.removeCallbacks(3, this.mTraversalRunnable, null);
        }
    }

    void doTraversal() {
        if (ViewDebugManager.DEBUG_LIFECYCLE || ViewDebugManager.DEBUG_ENG) {
            Log.v(this.mTag, "doTraversal: mTraversalScheduled = " + this.mTraversalScheduled + " mFisrt = " + this.mFirst + ",mTraversalBarrier = " + this.mTraversalBarrier + ",this = " + this);
        }
        if (this.debug_on && this.mThread != Thread.currentThread()) {
            Slog.d(TAG, "Non UI threads are operating UI data and need to fix.", new RuntimeException().fillInStackTrace());
        }
        if (this.mTraversalScheduled) {
            this.mTraversalScheduled = false;
            this.mHandler.getLooper().getQueue().removeSyncBarrier(this.mTraversalBarrier);
            if (this.mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceBegin(8L, "doTraversal");
            }
            performTraversals();
            if (ViewDebugManager.DEBUG_SCHEDULETRAVERSALS) {
                Trace.traceEnd(8L);
            }
            if (this.mProfile) {
                Debug.stopMethodTracing();
                this.mProfile = false;
            }
        }
    }

    private void applyKeepScreenOnFlag(WindowManager.LayoutParams params) {
        if (this.mAttachInfo.mKeepScreenOn) {
            params.flags |= 128;
        } else {
            params.flags = (params.flags & (-129)) | (this.mClientWindowLayoutFlags & 128);
        }
    }

    private boolean collectViewAttributes() {
        if (this.mAttachInfo.mRecomputeGlobalAttributes) {
            this.mAttachInfo.mRecomputeGlobalAttributes = false;
            boolean oldScreenOn = this.mAttachInfo.mKeepScreenOn;
            this.mAttachInfo.mKeepScreenOn = false;
            this.mAttachInfo.mSystemUiVisibility = 0;
            this.mAttachInfo.mHasSystemUiListeners = false;
            this.mView.dispatchCollectViewAttributes(this.mAttachInfo, 0);
            this.mAttachInfo.mSystemUiVisibility &= ~this.mAttachInfo.mDisabledSystemUiVisibility;
            WindowManager.LayoutParams params = this.mWindowAttributes;
            this.mAttachInfo.mSystemUiVisibility |= getImpliedSystemUiVisibility(params);
            SystemUiVisibilityInfo systemUiVisibilityInfo = this.mCompatibleVisibilityInfo;
            systemUiVisibilityInfo.globalVisibility = (systemUiVisibilityInfo.globalVisibility & (-2)) | (this.mAttachInfo.mSystemUiVisibility & 1);
            dispatchDispatchSystemUiVisibilityChanged(this.mCompatibleVisibilityInfo);
            if (this.mAttachInfo.mKeepScreenOn != oldScreenOn || this.mAttachInfo.mSystemUiVisibility != params.subtreeSystemUiVisibility || this.mAttachInfo.mHasSystemUiListeners != params.hasSystemUiListeners) {
                applyKeepScreenOnFlag(params);
                params.subtreeSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                params.hasSystemUiListeners = this.mAttachInfo.mHasSystemUiListeners;
                this.mView.dispatchWindowSystemUiVisiblityChanged(this.mAttachInfo.mSystemUiVisibility);
                return true;
            }
        }
        return false;
    }

    private int getImpliedSystemUiVisibility(WindowManager.LayoutParams params) {
        int vis = 0;
        if ((params.flags & 67108864) != 0) {
            vis = 0 | 1280;
        }
        if ((params.flags & 134217728) != 0) {
            return vis | 768;
        }
        return vis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCompatSysUiVisibility(int type, boolean visible, boolean hasControl) {
        int systemUiFlag;
        int publicType = InsetsState.toPublicType(type);
        if (publicType != WindowInsets.Type.statusBars() && publicType != WindowInsets.Type.navigationBars()) {
            return;
        }
        SystemUiVisibilityInfo info = this.mCompatibleVisibilityInfo;
        if (publicType == WindowInsets.Type.statusBars()) {
            systemUiFlag = 4;
        } else {
            systemUiFlag = 2;
        }
        if (visible) {
            info.globalVisibility &= ~systemUiFlag;
            if (hasControl && (this.mAttachInfo.mSystemUiVisibility & systemUiFlag) != 0) {
                info.localChanges |= systemUiFlag;
            }
        } else {
            info.globalVisibility |= systemUiFlag;
            info.localChanges &= ~systemUiFlag;
        }
        dispatchDispatchSystemUiVisibilityChanged(info);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearLowProfileModeIfNeeded(int showTypes, boolean fromIme) {
        SystemUiVisibilityInfo info = this.mCompatibleVisibilityInfo;
        if ((WindowInsets.Type.systemBars() & showTypes) != 0 && !fromIme && (info.globalVisibility & 1) != 0) {
            info.globalVisibility &= -2;
            info.localChanges |= 1;
            dispatchDispatchSystemUiVisibilityChanged(info);
        }
    }

    private void dispatchDispatchSystemUiVisibilityChanged(SystemUiVisibilityInfo args) {
        if (this.mDispatchedSystemUiVisibility != args.globalVisibility) {
            this.mHandler.removeMessages(17);
            ViewRootHandler viewRootHandler = this.mHandler;
            viewRootHandler.sendMessage(viewRootHandler.obtainMessage(17, args));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDispatchSystemUiVisibilityChanged(SystemUiVisibilityInfo args) {
        if (this.mView == null) {
            return;
        }
        if (args.localChanges != 0) {
            this.mView.updateLocalSystemUiVisibility(args.localValue, args.localChanges);
            args.localChanges = 0;
        }
        int visibility = args.globalVisibility & 7;
        if (this.mDispatchedSystemUiVisibility != visibility) {
            this.mDispatchedSystemUiVisibility = visibility;
            this.mView.dispatchSystemUiVisibilityChanged(visibility);
        }
    }

    public static void adjustLayoutParamsForCompatibility(WindowManager.LayoutParams inOutParams) {
        int sysUiVis = inOutParams.systemUiVisibility | inOutParams.subtreeSystemUiVisibility;
        int flags = inOutParams.flags;
        int type = inOutParams.type;
        int adjust = inOutParams.softInputMode & 240;
        if ((inOutParams.privateFlags & 67108864) == 0) {
            inOutParams.insetsFlags.appearance = 0;
            if ((sysUiVis & 1) != 0) {
                inOutParams.insetsFlags.appearance |= 4;
            }
            if ((sysUiVis & 8192) != 0) {
                inOutParams.insetsFlags.appearance |= 8;
            }
            if ((sysUiVis & 16) != 0) {
                inOutParams.insetsFlags.appearance |= 16;
            }
        }
        if ((inOutParams.privateFlags & 134217728) == 0) {
            if ((sysUiVis & 4096) != 0 || (flags & 1024) != 0) {
                inOutParams.insetsFlags.behavior = 2;
            } else {
                inOutParams.insetsFlags.behavior = 1;
            }
        }
        inOutParams.privateFlags &= -1073741825;
        if ((inOutParams.privateFlags & 268435456) != 0) {
            return;
        }
        int types = inOutParams.getFitInsetsTypes();
        boolean ignoreVis = inOutParams.isFitInsetsIgnoringVisibility();
        if ((sysUiVis & 1024) != 0 || (flags & 256) != 0 || (67108864 & flags) != 0) {
            types &= ~WindowInsets.Type.statusBars();
        }
        if ((sysUiVis & 512) != 0 || (flags & 134217728) != 0) {
            types &= ~WindowInsets.Type.systemBars();
        }
        if (type == 2005 || type == 2003) {
            ignoreVis = true;
        } else if ((WindowInsets.Type.systemBars() & types) == WindowInsets.Type.systemBars()) {
            if (adjust == 16) {
                types |= WindowInsets.Type.ime();
            } else {
                inOutParams.privateFlags |= 1073741824;
            }
        }
        inOutParams.setFitInsetsTypes(types);
        inOutParams.setFitInsetsIgnoringVisibility(ignoreVis);
        inOutParams.privateFlags &= -268435457;
    }

    private void controlInsetsForCompatibility(WindowManager.LayoutParams params) {
        int sysUiVis = params.systemUiVisibility | params.subtreeSystemUiVisibility;
        int flags = params.flags;
        boolean matchParent = params.width == -1 && params.height == -1;
        boolean nonAttachedAppWindow = params.type >= 1 && params.type <= 99;
        boolean statusWasHiddenByFlags = (this.mTypesHiddenByFlags & WindowInsets.Type.statusBars()) != 0;
        boolean statusIsHiddenByFlags = (sysUiVis & 4) != 0 || ((flags & 1024) != 0 && matchParent && nonAttachedAppWindow);
        boolean navWasHiddenByFlags = (this.mTypesHiddenByFlags & WindowInsets.Type.navigationBars()) != 0;
        boolean navIsHiddenByFlags = (sysUiVis & 2) != 0;
        int typesToHide = 0;
        int typesToShow = 0;
        if (statusIsHiddenByFlags && !statusWasHiddenByFlags) {
            typesToHide = 0 | WindowInsets.Type.statusBars();
        } else if (!statusIsHiddenByFlags && statusWasHiddenByFlags) {
            typesToShow = 0 | WindowInsets.Type.statusBars();
        }
        if (navIsHiddenByFlags && !navWasHiddenByFlags) {
            typesToHide |= WindowInsets.Type.navigationBars();
        } else if (!navIsHiddenByFlags && navWasHiddenByFlags) {
            typesToShow |= WindowInsets.Type.navigationBars();
        }
        if (typesToHide != 0) {
            getInsetsController().hide(typesToHide);
        }
        if (typesToShow != 0) {
            getInsetsController().show(typesToShow);
        }
        int i = this.mTypesHiddenByFlags | typesToHide;
        this.mTypesHiddenByFlags = i;
        this.mTypesHiddenByFlags = i & (~typesToShow);
    }

    /* JADX WARN: Removed duplicated region for block: B:42:0x01b9  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x01e5  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x01fb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean measureHierarchy(View host, WindowManager.LayoutParams lp, Resources res, int desiredWindowWidth, int desiredWindowHeight) {
        boolean windowSizeMayChange;
        boolean goodMeasure;
        boolean goodMeasure2;
        boolean windowSizeMayChange2;
        if (DEBUG_ORIENTATION || DEBUG_LAYOUT) {
            Log.v(this.mTag, "Measuring " + host + " in display " + desiredWindowWidth + "x" + desiredWindowHeight + Session.TRUNCATE_STRING);
        }
        if (lp.width != -2) {
            windowSizeMayChange = false;
            goodMeasure = false;
        } else {
            DisplayMetrics packageMetrics = res.getDisplayMetrics();
            res.getValue(R.dimen.config_prefDialogWidth, this.mTmpValue, true);
            int baseSize = 0;
            if (this.mTmpValue.type == 5) {
                baseSize = (int) this.mTmpValue.getDimension(packageMetrics);
            }
            if (DEBUG_DIALOG) {
                Log.v(this.mTag, "Window " + this.mView + ": baseSize=" + baseSize + ", desiredWindowWidth=" + desiredWindowWidth);
            }
            if (baseSize == 0 || desiredWindowWidth <= baseSize) {
                windowSizeMayChange = false;
                goodMeasure = false;
            } else {
                int childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width, lp.privateFlags);
                int childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height, lp.privateFlags);
                performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                windowSizeMayChange = false;
                if (DEBUG_DIALOG) {
                    goodMeasure = false;
                    Log.v(this.mTag, "Window " + this.mView + ": measured (" + host.getMeasuredWidth() + "," + host.getMeasuredHeight() + ") from width spec: " + View.MeasureSpec.toString(childWidthMeasureSpec) + " and height spec: " + View.MeasureSpec.toString(childHeightMeasureSpec));
                } else {
                    goodMeasure = false;
                }
                if ((host.getMeasuredWidthAndState() & 16777216) == 0) {
                    goodMeasure2 = true;
                } else {
                    int baseSize2 = (baseSize + desiredWindowWidth) / 2;
                    if (DEBUG_DIALOG) {
                        Log.v(this.mTag, "Window " + this.mView + ": next baseSize=" + baseSize2);
                    }
                    performMeasure(getRootMeasureSpec(baseSize2, lp.width, lp.privateFlags), childHeightMeasureSpec);
                    if (DEBUG_DIALOG) {
                        Log.v(this.mTag, "Window " + this.mView + ": measured (" + host.getMeasuredWidth() + "," + host.getMeasuredHeight() + NavigationBarInflaterView.KEY_CODE_END);
                    }
                    if ((host.getMeasuredWidthAndState() & 16777216) == 0) {
                        if (DEBUG_DIALOG) {
                            Log.v(this.mTag, "Good!");
                        }
                        goodMeasure2 = true;
                    }
                }
                if (!goodMeasure2) {
                    performMeasure(getRootMeasureSpec(desiredWindowWidth, lp.width, lp.privateFlags), getRootMeasureSpec(desiredWindowHeight, lp.height, lp.privateFlags));
                    if (this.mWidth != host.getMeasuredWidth() || this.mHeight != host.getMeasuredHeight()) {
                        windowSizeMayChange2 = true;
                        if (DBG) {
                            System.out.println("======================================");
                            System.out.println("performTraversals -- after measure");
                            host.debug();
                        }
                        if (!DEBUG_ORIENTATION || ViewDebugManager.DEBUG_LAYOUT) {
                            Log.v(this.mTag, "ViewRoot measure-: host measured size = (" + host.getMeasuredWidth() + "x" + host.getMeasuredHeight() + "), windowSizeMayChange = " + windowSizeMayChange2 + ", this = " + this);
                        }
                        return windowSizeMayChange2;
                    }
                }
                windowSizeMayChange2 = windowSizeMayChange;
                if (DBG) {
                }
                if (!DEBUG_ORIENTATION) {
                }
                Log.v(this.mTag, "ViewRoot measure-: host measured size = (" + host.getMeasuredWidth() + "x" + host.getMeasuredHeight() + "), windowSizeMayChange = " + windowSizeMayChange2 + ", this = " + this);
                return windowSizeMayChange2;
            }
        }
        goodMeasure2 = goodMeasure;
        if (!goodMeasure2) {
        }
        windowSizeMayChange2 = windowSizeMayChange;
        if (DBG) {
        }
        if (!DEBUG_ORIENTATION) {
        }
        Log.v(this.mTag, "ViewRoot measure-: host measured size = (" + host.getMeasuredWidth() + "x" + host.getMeasuredHeight() + "), windowSizeMayChange = " + windowSizeMayChange2 + ", this = " + this);
        return windowSizeMayChange2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transformMatrixToGlobal(Matrix m) {
        m.preTranslate(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transformMatrixToLocal(Matrix m) {
        m.postTranslate(-this.mAttachInfo.mWindowLeft, -this.mAttachInfo.mWindowTop);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowInsets getWindowInsets(boolean forceConstruct) {
        int i;
        if (this.mLastWindowInsets == null || forceConstruct) {
            Configuration config = getConfiguration();
            InsetsController insetsController = this.mInsetsController;
            boolean isScreenRound = config.isScreenRound();
            boolean z = this.mAttachInfo.mAlwaysConsumeSystemBars;
            int i2 = this.mWindowAttributes.type;
            int windowingMode = config.windowConfiguration.getWindowingMode();
            int i3 = this.mWindowAttributes.softInputMode;
            int i4 = this.mWindowAttributes.flags;
            int i5 = this.mWindowAttributes.systemUiVisibility | this.mWindowAttributes.subtreeSystemUiVisibility;
            if (config.windowConfiguration.getWindowingMode() != 6) {
                i = 0;
            } else {
                i = 256;
            }
            this.mLastWindowInsets = insetsController.calculateInsets(isScreenRound, z, i2, windowingMode, i3, i4, i5 | i);
            this.mAttachInfo.mContentInsets.set(this.mLastWindowInsets.getSystemWindowInsets().toRect());
            this.mAttachInfo.mStableInsets.set(this.mLastWindowInsets.getStableInsets().toRect());
            this.mAttachInfo.mVisibleInsets.set(this.mInsetsController.calculateVisibleInsets(this.mWindowAttributes.type, config.windowConfiguration.getWindowingMode(), this.mWindowAttributes.softInputMode, this.mWindowAttributes.flags).toRect());
        }
        return this.mLastWindowInsets;
    }

    public void dispatchApplyInsets(View host) {
        Trace.traceBegin(8L, "dispatchApplyInsets");
        this.mApplyInsetsRequested = false;
        WindowInsets insets = getWindowInsets(true);
        if (!shouldDispatchCutout()) {
            insets = insets.consumeDisplayCutout();
        }
        host.dispatchApplyWindowInsets(insets);
        this.mAttachInfo.delayNotifyContentCaptureInsetsEvent(insets.getInsets(WindowInsets.Type.all()));
        Trace.traceEnd(8L);
    }

    private boolean updateCaptionInsets() {
        if (CAPTION_ON_SHELL) {
            return false;
        }
        View view = this.mView;
        if (view instanceof DecorView) {
            int captionInsetsHeight = ((DecorView) view).getCaptionInsetsHeight();
            Rect captionFrame = new Rect();
            if (captionInsetsHeight != 0) {
                captionFrame.set(this.mWinFrame.left, this.mWinFrame.top, this.mWinFrame.right, this.mWinFrame.top + captionInsetsHeight);
            }
            if (this.mAttachInfo.mCaptionInsets.equals(captionFrame)) {
                return false;
            }
            this.mAttachInfo.mCaptionInsets.set(captionFrame);
            return true;
        }
        return false;
    }

    private boolean shouldDispatchCutout() {
        return this.mWindowAttributes.layoutInDisplayCutoutMode == 3 || this.mWindowAttributes.layoutInDisplayCutoutMode == 1;
    }

    public InsetsController getInsetsController() {
        return this.mInsetsController;
    }

    private static boolean shouldUseDisplaySize(WindowManager.LayoutParams lp) {
        return lp.type == 2041 || lp.type == 2011 || lp.type == 2020;
    }

    private Rect getWindowBoundsInsetSystemBars() {
        Rect bounds = new Rect(this.mContext.getResources().getConfiguration().windowConfiguration.getBounds());
        bounds.inset(this.mInsetsController.getState().calculateInsets(bounds, WindowInsets.Type.systemBars(), false));
        return bounds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int dipToPx(int dip) {
        DisplayMetrics displayMetrics = this.mContext.getResources().getDisplayMetrics();
        return (int) ((displayMetrics.density * dip) + 0.5f);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3399=11, 3402=16, 3403=5] */
    /* JADX WARN: Can't wrap try/catch for region: R(179:7|(1:793)(1:15)|16|(4:18|(1:20)(1:791)|(1:22)(1:790)|(172:24|25|(3:27|(1:29)(1:788)|30)(1:789)|31|(5:33|(1:35)(2:774|(1:779)(1:778))|36|(1:38)|39)(2:780|(3:784|(1:786)|787))|(3:41|(2:(1:44)(1:46)|45)|(1:50))|51|(1:53)|54|(1:56)|57|(1:773)(1:63)|64|(4:66|(2:68|(3:767|(1:769)(1:770)|73))(1:771)|72|73)(1:772)|74|(1:76)|77|(1:79)|80|(2:751|(6:753|(3:755|(2:757|758)(1:760)|759)|761|(1:763)|764|(1:766)))|84|(2:86|(149:88|(1:90)|(1:747)(127:93|(1:746)(4:97|(2:99|(1:103))(1:745)|737|(2:739|(1:741))(1:744))|104|105|(1:736)(1:109)|110|(1:735)(1:114)|115|(1:117)(1:734)|118|(1:120)(1:733)|(6:122|(1:126)|127|(1:129)|130|(1:132))(1:732)|133|(1:731)(2:138|(1:140)(40:730|(1:463)|(1:462)(1:257)|258|(1:461)(1:262)|263|(1:265)|(4:267|(4:269|(1:271)|272|(2:274|(1:276)))(1:459)|277|(1:279))(1:460)|(1:281)(1:(1:456)(1:(1:458)))|(1:283)|284|(3:286|(3:449|(1:451)(1:453)|452)(1:290)|291)(1:454)|292|(1:448)(1:296)|297|(7:299|(4:301|(1:303)|304|(1:306))(1:436)|(1:308)(1:435)|(1:310)|(1:312)(1:(1:434))|313|314)(2:437|(3:441|442|443))|315|(1:426)(6:317|(2:419|(1:423))|322|(1:324)|325|(2:327|(2:329|(1:331))(2:332|(1:334))))|(15:418|339|(1:416)(1:342)|(1:415)(1:346)|(1:348)(2:409|(1:414)(1:413))|(4:402|(1:404)(1:408)|405|(1:407))(1:351)|352|(1:354)|355|(1:357)|(4:359|(1:383)(3:363|(2:364|(1:366)(1:367))|368)|369|(1:371))(2:384|(1:386)(4:387|(4:391|(2:394|392)|395|396)|397|(1:401)))|372|(1:374)|375|(2:377|(2:379|380)(1:381))(1:382))|338|339|(0)|416|(1:344)|415|(0)(0)|(0)|402|(0)(0)|405|(0)|352|(0)|355|(0)|(0)(0)|372|(0)|375|(0)(0)))|141|(1:143)(1:729)|144|(3:146|(1:148)(1:150)|149)|151|(1:153)|154|155|156|157|(8:706|707|708|709|710|711|712|713)(1:159)|160|161|162|(1:164)|165|(1:701)|168|(3:170|(1:172)|173)|174|(1:176)(1:700)|177|178|(2:180|(1:184))|513|(8:677|678|679|680|681|682|683|684)(1:515)|516|517|518|(1:520)|521|(1:672)(6:523|(1:525)|526|(1:528)(1:671)|529|530)|531|532|(2:534|535)|536|(1:538)(1:670)|539|(1:669)(1:543)|544|(1:668)(1:548)|549|550|(1:667)(1:553)|554|(1:556)|(2:558|559)|560|(1:562)|(2:663|664)|(3:569|570|(5:605|606|607|(3:610|611|(1:613))|609))(1:(8:631|(1:633)|634|(1:636)|637|(1:639)|640|(1:642))(1:(3:652|653|654)))|572|573|(1:(7:576|(1:598)(1:580)|581|(1:583)(1:597)|584|585|586)(1:599))(1:600)|587|(1:(1:590)(1:591))|592|(1:594)|595|194|(1:503)|198|(1:502)|202|(6:204|(1:206)|207|(2:209|(3:211|(1:213)|214))|(2:489|(3:491|(4:493|(1:495)|496|497)(1:499)|498)(1:500))(1:219)|(4:221|222|223|224))(1:501)|230|(1:241)|242|(8:470|(1:472)|473|(1:475)(1:488)|476|(1:478)(1:487)|(1:486)(3:480|(1:482)(1:485)|483)|484)(1:246)|247|(0)|463|(2:253|255)|462|258|(1:260)|461|263|(0)|(0)(0)|(0)(0)|(0)|284|(0)(0)|292|(1:294)|448|297|(0)(0)|315|(0)(0)|(0)|338|339|(0)|416|(0)|415|(0)(0)|(0)|402|(0)(0)|405|(0)|352|(0)|355|(0)|(0)(0)|372|(0)|375|(0)(0))|743|105|(1:107)|736|110|(1:112)|735|115|(0)(0)|118|(0)(0)|(0)(0)|133|(0)|731|141|(0)(0)|144|(0)|151|(0)|154|155|156|157|(0)(0)|160|161|162|(0)|165|(0)|701|168|(0)|174|(0)(0)|177|178|(0)|513|(0)(0)|516|517|518|(0)|521|(0)(0)|531|532|(0)|536|(0)(0)|539|(1:541)|669|544|(1:546)|668|549|550|(0)|665|667|554|(0)|(0)|560|(0)|(1:564)|663|664|(0)(0)|572|573|(0)(0)|587|(0)|592|(0)|595|194|(1:196)|503|198|(1:200)|502|202|(0)(0)|230|(2:232|241)|242|(1:244)|464|470|(0)|473|(0)(0)|476|(0)(0)|(0)(0)|484|247|(0)|463|(0)|462|258|(0)|461|263|(0)|(0)(0)|(0)(0)|(0)|284|(0)(0)|292|(0)|448|297|(0)(0)|315|(0)(0)|(0)|338|339|(0)|416|(0)|415|(0)(0)|(0)|402|(0)(0)|405|(0)|352|(0)|355|(0)|(0)(0)|372|(0)|375|(0)(0))(1:748))(1:750)|749|(0)|(0)|747|743|105|(0)|736|110|(0)|735|115|(0)(0)|118|(0)(0)|(0)(0)|133|(0)|731|141|(0)(0)|144|(0)|151|(0)|154|155|156|157|(0)(0)|160|161|162|(0)|165|(0)|701|168|(0)|174|(0)(0)|177|178|(0)|513|(0)(0)|516|517|518|(0)|521|(0)(0)|531|532|(0)|536|(0)(0)|539|(0)|669|544|(0)|668|549|550|(0)|665|667|554|(0)|(0)|560|(0)|(0)|663|664|(0)(0)|572|573|(0)(0)|587|(0)|592|(0)|595|194|(0)|503|198|(0)|502|202|(0)(0)|230|(0)|242|(0)|464|470|(0)|473|(0)(0)|476|(0)(0)|(0)(0)|484|247|(0)|463|(0)|462|258|(0)|461|263|(0)|(0)(0)|(0)(0)|(0)|284|(0)(0)|292|(0)|448|297|(0)(0)|315|(0)(0)|(0)|338|339|(0)|416|(0)|415|(0)(0)|(0)|402|(0)(0)|405|(0)|352|(0)|355|(0)|(0)(0)|372|(0)|375|(0)(0)))|792|25|(0)(0)|31|(0)(0)|(0)|51|(0)|54|(0)|57|(2:59|61)|773|64|(0)(0)|74|(0)|77|(0)|80|(1:82)|751|(0)|84|(0)(0)|749|(0)|(0)|747|743|105|(0)|736|110|(0)|735|115|(0)(0)|118|(0)(0)|(0)(0)|133|(0)|731|141|(0)(0)|144|(0)|151|(0)|154|155|156|157|(0)(0)|160|161|162|(0)|165|(0)|701|168|(0)|174|(0)(0)|177|178|(0)|513|(0)(0)|516|517|518|(0)|521|(0)(0)|531|532|(0)|536|(0)(0)|539|(0)|669|544|(0)|668|549|550|(0)|665|667|554|(0)|(0)|560|(0)|(0)|663|664|(0)(0)|572|573|(0)(0)|587|(0)|592|(0)|595|194|(0)|503|198|(0)|502|202|(0)(0)|230|(0)|242|(0)|464|470|(0)|473|(0)(0)|476|(0)(0)|(0)(0)|484|247|(0)|463|(0)|462|258|(0)|461|263|(0)|(0)(0)|(0)(0)|(0)|284|(0)(0)|292|(0)|448|297|(0)(0)|315|(0)(0)|(0)|338|339|(0)|416|(0)|415|(0)(0)|(0)|402|(0)(0)|405|(0)|352|(0)|355|(0)|(0)(0)|372|(0)|375|(0)(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:157:0x02c4, code lost:
        if (r19.height() != r49.mHeight) goto L104;
     */
    /* JADX WARN: Code restructure failed: missing block: B:439:0x086f, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:441:0x087a, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:442:0x087b, code lost:
        r1 = r30;
        r2 = r31;
        r6 = r40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:443:0x0883, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:445:0x0893, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:446:0x0894, code lost:
        r9 = r2;
        r1 = r30;
        r2 = r31;
        r6 = r40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:447:0x08a2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:449:0x08b6, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:450:0x08b7, code lost:
        r41 = r18;
        r9 = r2;
        r1 = r30;
        r2 = r31;
        r6 = r40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:451:0x08c8, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:453:0x08e0, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:454:0x08e1, code lost:
        r38 = r3;
        r41 = r18;
        r9 = r2;
        r1 = r30;
        r2 = r31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:685:0x0e36, code lost:
        if (r49.mFirst == false) goto L338;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:103:0x01ed  */
    /* JADX WARN: Removed duplicated region for block: B:106:0x01f8  */
    /* JADX WARN: Removed duplicated region for block: B:109:0x0200  */
    /* JADX WARN: Removed duplicated region for block: B:116:0x021a  */
    /* JADX WARN: Removed duplicated region for block: B:129:0x0251  */
    /* JADX WARN: Removed duplicated region for block: B:133:0x027a  */
    /* JADX WARN: Removed duplicated region for block: B:136:0x0282  */
    /* JADX WARN: Removed duplicated region for block: B:138:0x0286 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:166:0x02d9  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x02ef  */
    /* JADX WARN: Removed duplicated region for block: B:179:0x0306  */
    /* JADX WARN: Removed duplicated region for block: B:180:0x0308  */
    /* JADX WARN: Removed duplicated region for block: B:183:0x0319  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x0323  */
    /* JADX WARN: Removed duplicated region for block: B:186:0x0329  */
    /* JADX WARN: Removed duplicated region for block: B:197:0x0363  */
    /* JADX WARN: Removed duplicated region for block: B:200:0x036b A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:210:0x039b  */
    /* JADX WARN: Removed duplicated region for block: B:211:0x03ee  */
    /* JADX WARN: Removed duplicated region for block: B:214:0x03fd  */
    /* JADX WARN: Removed duplicated region for block: B:221:0x0443  */
    /* JADX WARN: Removed duplicated region for block: B:240:0x04fd  */
    /* JADX WARN: Removed duplicated region for block: B:244:0x0509 A[Catch: all -> 0x0510, RemoteException -> 0x0524, TRY_ENTER, TRY_LEAVE, TryCatch #23 {RemoteException -> 0x0524, all -> 0x0510, blocks: (B:230:0x0468, B:244:0x0509, B:256:0x0556, B:258:0x055c, B:259:0x0563, B:267:0x057a, B:269:0x0581, B:271:0x0585), top: B:795:0x0468 }] */
    /* JADX WARN: Removed duplicated region for block: B:252:0x053b A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:256:0x0556 A[Catch: all -> 0x0510, RemoteException -> 0x0524, TRY_ENTER, TryCatch #23 {RemoteException -> 0x0524, all -> 0x0510, blocks: (B:230:0x0468, B:244:0x0509, B:256:0x0556, B:258:0x055c, B:259:0x0563, B:267:0x057a, B:269:0x0581, B:271:0x0585), top: B:795:0x0468 }] */
    /* JADX WARN: Removed duplicated region for block: B:262:0x056e  */
    /* JADX WARN: Removed duplicated region for block: B:263:0x0570  */
    /* JADX WARN: Removed duplicated region for block: B:267:0x057a A[Catch: all -> 0x0510, RemoteException -> 0x0524, TRY_ENTER, TryCatch #23 {RemoteException -> 0x0524, all -> 0x0510, blocks: (B:230:0x0468, B:244:0x0509, B:256:0x0556, B:258:0x055c, B:259:0x0563, B:267:0x057a, B:269:0x0581, B:271:0x0585), top: B:795:0x0468 }] */
    /* JADX WARN: Removed duplicated region for block: B:288:0x0602  */
    /* JADX WARN: Removed duplicated region for block: B:292:0x060e A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_ENTER, TRY_LEAVE, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:300:0x063b A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_ENTER, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:309:0x0673  */
    /* JADX WARN: Removed duplicated region for block: B:313:0x0680  */
    /* JADX WARN: Removed duplicated region for block: B:317:0x0697  */
    /* JADX WARN: Removed duplicated region for block: B:318:0x0699  */
    /* JADX WARN: Removed duplicated region for block: B:321:0x06a4 A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_ENTER, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:327:0x06b3 A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_LEAVE, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:334:0x06c8 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:33:0x006c  */
    /* JADX WARN: Removed duplicated region for block: B:341:0x06d9 A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_ENTER, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:343:0x06e1 A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_LEAVE, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:347:0x06f0  */
    /* JADX WARN: Removed duplicated region for block: B:349:0x06f5 A[Catch: all -> 0x0612, RemoteException -> 0x0622, TRY_ENTER, TryCatch #27 {RemoteException -> 0x0622, all -> 0x0612, blocks: (B:278:0x0597, B:292:0x060e, B:300:0x063b, B:302:0x063f, B:303:0x065d, B:307:0x066b, B:314:0x0682, B:321:0x06a4, B:327:0x06b3, B:341:0x06d9, B:343:0x06e1, B:349:0x06f5, B:351:0x06fd, B:357:0x0710, B:359:0x071d, B:417:0x07e5, B:381:0x076f, B:383:0x0773, B:384:0x0776, B:386:0x0781, B:387:0x0787, B:389:0x078b, B:390:0x078e, B:392:0x0794, B:399:0x07a6, B:401:0x07ac, B:403:0x07b4, B:404:0x07b7, B:407:0x07c2), top: B:789:0x0597 }] */
    /* JADX WARN: Removed duplicated region for block: B:356:0x070f  */
    /* JADX WARN: Removed duplicated region for block: B:380:0x076d  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0087  */
    /* JADX WARN: Removed duplicated region for block: B:414:0x07d5  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0091  */
    /* JADX WARN: Removed duplicated region for block: B:429:0x0838  */
    /* JADX WARN: Removed duplicated region for block: B:432:0x0848  */
    /* JADX WARN: Removed duplicated region for block: B:437:0x0864  */
    /* JADX WARN: Removed duplicated region for block: B:458:0x0915  */
    /* JADX WARN: Removed duplicated region for block: B:462:0x0920  */
    /* JADX WARN: Removed duplicated region for block: B:467:0x095c  */
    /* JADX WARN: Removed duplicated region for block: B:472:0x0974  */
    /* JADX WARN: Removed duplicated region for block: B:506:0x0a24  */
    /* JADX WARN: Removed duplicated region for block: B:509:0x0a2c  */
    /* JADX WARN: Removed duplicated region for block: B:521:0x0a5c  */
    /* JADX WARN: Removed duplicated region for block: B:533:0x0a91  */
    /* JADX WARN: Removed duplicated region for block: B:536:0x0af5  */
    /* JADX WARN: Removed duplicated region for block: B:537:0x0b06  */
    /* JADX WARN: Removed duplicated region for block: B:540:0x0b0e  */
    /* JADX WARN: Removed duplicated region for block: B:541:0x0b21  */
    /* JADX WARN: Removed duplicated region for block: B:543:0x0b25  */
    /* JADX WARN: Removed duplicated region for block: B:548:0x0b54  */
    /* JADX WARN: Removed duplicated region for block: B:552:0x0b63 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:557:0x0b6e  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x00ff  */
    /* JADX WARN: Removed duplicated region for block: B:565:0x0b7c  */
    /* JADX WARN: Removed duplicated region for block: B:572:0x0b8b  */
    /* JADX WARN: Removed duplicated region for block: B:574:0x0be1  */
    /* JADX WARN: Removed duplicated region for block: B:588:0x0c6c  */
    /* JADX WARN: Removed duplicated region for block: B:590:0x0c74  */
    /* JADX WARN: Removed duplicated region for block: B:591:0x0c78  */
    /* JADX WARN: Removed duplicated region for block: B:596:0x0c85  */
    /* JADX WARN: Removed duplicated region for block: B:599:0x0c98  */
    /* JADX WARN: Removed duplicated region for block: B:611:0x0cf4  */
    /* JADX WARN: Removed duplicated region for block: B:614:0x0d01  */
    /* JADX WARN: Removed duplicated region for block: B:620:0x0d0b  */
    /* JADX WARN: Removed duplicated region for block: B:645:0x0d6d  */
    /* JADX WARN: Removed duplicated region for block: B:64:0x013c  */
    /* JADX WARN: Removed duplicated region for block: B:657:0x0d8a  */
    /* JADX WARN: Removed duplicated region for block: B:682:0x0e30  */
    /* JADX WARN: Removed duplicated region for block: B:687:0x0e3a  */
    /* JADX WARN: Removed duplicated region for block: B:691:0x0e43 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:695:0x0e4a  */
    /* JADX WARN: Removed duplicated region for block: B:700:0x0e53  */
    /* JADX WARN: Removed duplicated region for block: B:701:0x0e5a  */
    /* JADX WARN: Removed duplicated region for block: B:708:0x0e69 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:713:0x0e79  */
    /* JADX WARN: Removed duplicated region for block: B:714:0x0e7c  */
    /* JADX WARN: Removed duplicated region for block: B:717:0x0e82  */
    /* JADX WARN: Removed duplicated region for block: B:720:0x0e9f  */
    /* JADX WARN: Removed duplicated region for block: B:723:0x0eac  */
    /* JADX WARN: Removed duplicated region for block: B:725:0x0eb1  */
    /* JADX WARN: Removed duplicated region for block: B:738:0x0ee7  */
    /* JADX WARN: Removed duplicated region for block: B:757:0x0f29  */
    /* JADX WARN: Removed duplicated region for block: B:760:0x0f33  */
    /* JADX WARN: Removed duplicated region for block: B:768:0x0f56  */
    /* JADX WARN: Removed duplicated region for block: B:76:0x015f  */
    /* JADX WARN: Removed duplicated region for block: B:791:0x045d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:798:0x058c A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0171  */
    /* JADX WARN: Removed duplicated region for block: B:813:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0190  */
    /* JADX WARN: Type inference failed for: r0v198 */
    /* JADX WARN: Type inference failed for: r0v199 */
    /* JADX WARN: Type inference failed for: r0v277 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void performTraversals() {
        boolean z;
        boolean supportsScreen;
        boolean z2;
        WindowManager.LayoutParams params;
        int desiredWindowWidth;
        int desiredWindowHeight;
        boolean layoutRequested;
        Rect frame;
        int i;
        int desiredWindowHeight2;
        int desiredWindowWidth2;
        int resizeMode;
        int desiredWindowHeight3;
        int desiredWindowWidth3;
        boolean windowSizeMayChange;
        int desiredWindowHeight4;
        int desiredWindowWidth4;
        boolean windowShouldResize;
        boolean computesInternalInsets;
        boolean surfaceSizeChanged;
        boolean surfaceCreated;
        boolean surfaceReplaced;
        boolean windowAttributesChanged;
        int relayoutResult;
        WindowManager.LayoutParams params2;
        boolean insetsPending;
        boolean updatedConfiguration;
        boolean insetsPending2;
        Rect frame2;
        boolean isViewVisible;
        boolean windowAttributesChanged2;
        String str;
        Rect frame3;
        boolean isViewVisible2;
        boolean updatedConfiguration2;
        int relayoutResult2;
        boolean didLayout;
        boolean triggerGlobalLayoutListener;
        Rect contentInsets;
        boolean needsSetInsets;
        boolean changedVisibility;
        boolean regainedFocus;
        boolean isToast;
        boolean cancelAndRedraw;
        SurfaceSyncer.SyncBufferCallback syncBufferCallback;
        int i2;
        Region region;
        Rect contentInsets2;
        BaseSurfaceHolder baseSurfaceHolder;
        boolean dispatchApplyInsets;
        boolean insetsPending3;
        int relayoutResult3;
        boolean updatedConfiguration3;
        boolean hwInitialized;
        boolean dispatchApplyInsets2;
        int relayoutResult4;
        ThreadedRenderer threadedRenderer;
        boolean measureAgain;
        int childWidthMeasureSpec;
        boolean updatedConfiguration4;
        boolean dragResizing;
        int i3;
        boolean dragResizing2;
        boolean hwInitialized2;
        int i4;
        int desiredWindowWidth5;
        int desiredWindowHeight5;
        View host = this.mView;
        if (DBG) {
            System.out.println("======================================");
            System.out.println("performTraversals");
            host.debug();
        }
        if (host == null || !this.mAdded) {
            return;
        }
        this.mIsInTraversal = true;
        this.mWillDrawSoon = true;
        boolean windowSizeMayChange2 = false;
        WindowManager.LayoutParams lp = this.mWindowAttributes;
        int viewVisibility = getHostVisibility();
        boolean z3 = this.mFirst;
        boolean viewVisibilityChanged = !z3 && (this.mViewVisibility != viewVisibility || this.mNewSurfaceNeeded || this.mAppVisibilityChanged);
        this.mAppVisibilityChanged = false;
        if (!z3) {
            if ((this.mViewVisibility == 0) != (viewVisibility == 0)) {
                z = true;
                boolean viewUserVisibilityChanged = z;
                CompatibilityInfo compatibilityInfo = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
                supportsScreen = compatibilityInfo.supportsScreen();
                z2 = this.mLastInCompatMode;
                if (supportsScreen != z2) {
                    this.mFullRedrawNeeded = true;
                    this.mLayoutRequested = true;
                    if (z2) {
                        lp.privateFlags &= -129;
                        this.mLastInCompatMode = false;
                    } else {
                        lp.privateFlags |= 128;
                        this.mLastInCompatMode = true;
                    }
                    params = lp;
                } else {
                    params = null;
                }
                Rect frame4 = this.mWinFrame;
                if (this.mFirst) {
                    desiredWindowWidth = frame4.width();
                    desiredWindowHeight = frame4.height();
                    if (desiredWindowWidth != this.mWidth || desiredWindowHeight != this.mHeight) {
                        if (DEBUG_ORIENTATION) {
                            Log.v(this.mTag, "View " + host + " resized to: " + frame4);
                        }
                        this.mFullRedrawNeeded = true;
                        this.mLayoutRequested = true;
                        windowSizeMayChange2 = true;
                    }
                } else {
                    this.mFullRedrawNeeded = true;
                    this.mLayoutRequested = true;
                    Configuration config = getConfiguration();
                    if (shouldUseDisplaySize(lp)) {
                        Point size = new Point();
                        this.mDisplay.getRealSize(size);
                        desiredWindowWidth = size.x;
                        desiredWindowHeight = size.y;
                    } else if (lp.width == -2 || lp.height == -2) {
                        Rect bounds = getWindowBoundsInsetSystemBars();
                        desiredWindowWidth = bounds.width();
                        desiredWindowHeight = bounds.height();
                    } else {
                        desiredWindowWidth = frame4.width();
                        desiredWindowHeight = frame4.height();
                    }
                    this.mAttachInfo.mUse32BitDrawingCache = true;
                    this.mAttachInfo.mWindowVisibility = viewVisibility;
                    this.mAttachInfo.mRecomputeGlobalAttributes = false;
                    this.mLastConfigurationFromResources.setTo(config);
                    this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                    if (this.mViewLayoutDirectionInitial == 2) {
                        host.setLayoutDirection(config.getLayoutDirection());
                    }
                    host.dispatchAttachedToWindow(this.mAttachInfo, 0);
                    this.mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
                    dispatchApplyInsets(host);
                }
                if (viewVisibilityChanged) {
                    this.mAttachInfo.mWindowVisibility = viewVisibility;
                    host.dispatchWindowVisibilityChanged(viewVisibility);
                    if (viewUserVisibilityChanged) {
                        host.dispatchVisibilityAggregated(viewVisibility == 0);
                    }
                    if (viewVisibility != 0 || this.mNewSurfaceNeeded) {
                        endDragResizing();
                        destroyHardwareResources();
                    }
                }
                if (this.mAttachInfo.mWindowVisibility != 0) {
                    host.clearAccessibilityFocus();
                }
                getRunQueue().executeActions(this.mAttachInfo.mHandler);
                if (this.mFirst) {
                    this.mAttachInfo.mInTouchMode = !this.mAddedTouchMode;
                    ensureTouchModeLocally(this.mAddedTouchMode);
                }
                layoutRequested = !this.mLayoutRequested && (!this.mStopped || this.mReportNextDraw);
                if (layoutRequested) {
                    frame = frame4;
                    i = -2;
                    desiredWindowHeight2 = desiredWindowHeight;
                    desiredWindowWidth2 = desiredWindowWidth;
                } else {
                    if (this.mFirst) {
                        i4 = -2;
                    } else {
                        i4 = -2;
                        if (lp.width == -2 || lp.height == -2) {
                            windowSizeMayChange2 = true;
                            if (shouldUseDisplaySize(lp)) {
                                Point size2 = new Point();
                                this.mDisplay.getRealSize(size2);
                                int desiredWindowWidth6 = size2.x;
                                desiredWindowHeight5 = size2.y;
                                desiredWindowWidth5 = desiredWindowWidth6;
                            } else {
                                Rect bounds2 = getWindowBoundsInsetSystemBars();
                                int desiredWindowWidth7 = bounds2.width();
                                desiredWindowHeight5 = bounds2.height();
                                desiredWindowWidth5 = desiredWindowWidth7;
                            }
                            i = i4;
                            frame = frame4;
                            windowSizeMayChange2 |= measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth5, desiredWindowHeight5);
                            desiredWindowHeight2 = desiredWindowHeight5;
                            desiredWindowWidth2 = desiredWindowWidth5;
                        }
                    }
                    desiredWindowHeight5 = desiredWindowHeight;
                    desiredWindowWidth5 = desiredWindowWidth;
                    i = i4;
                    frame = frame4;
                    windowSizeMayChange2 |= measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth5, desiredWindowHeight5);
                    desiredWindowHeight2 = desiredWindowHeight5;
                    desiredWindowWidth2 = desiredWindowWidth5;
                }
                if (collectViewAttributes()) {
                    params = lp;
                }
                if (this.mAttachInfo.mForceReportNewAttributes) {
                    this.mAttachInfo.mForceReportNewAttributes = false;
                    params = lp;
                }
                if (!this.mFirst || this.mAttachInfo.mViewVisibilityChanged) {
                    this.mAttachInfo.mViewVisibilityChanged = false;
                    resizeMode = this.mSoftInputMode & 240;
                    if (resizeMode == 0) {
                        int N = this.mAttachInfo.mScrollContainers.size();
                        for (int i5 = 0; i5 < N; i5++) {
                            if (this.mAttachInfo.mScrollContainers.get(i5).isShown()) {
                                resizeMode = 16;
                            }
                        }
                        if (resizeMode == 0) {
                            resizeMode = 32;
                        }
                        if ((lp.softInputMode & 240) != resizeMode) {
                            lp.softInputMode = (lp.softInputMode & (-241)) | resizeMode;
                            params = lp;
                        }
                    }
                }
                if (this.mApplyInsetsRequested) {
                    desiredWindowHeight3 = desiredWindowHeight2;
                    desiredWindowWidth3 = desiredWindowWidth2;
                } else {
                    dispatchApplyInsets(host);
                    if (this.mLayoutRequested) {
                        desiredWindowHeight3 = desiredWindowHeight2;
                        desiredWindowWidth3 = desiredWindowWidth2;
                        windowSizeMayChange = windowSizeMayChange2 | measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth2, desiredWindowHeight3);
                        if (layoutRequested) {
                            this.mLayoutRequested = false;
                        }
                        if (layoutRequested || !windowSizeMayChange) {
                            desiredWindowHeight4 = desiredWindowHeight3;
                            desiredWindowWidth4 = desiredWindowWidth3;
                        } else {
                            if (this.mWidth == host.getMeasuredWidth() && this.mHeight == host.getMeasuredHeight()) {
                                if (lp.width == i) {
                                    desiredWindowWidth4 = desiredWindowWidth3;
                                    if (frame.width() < desiredWindowWidth4 && frame.width() != this.mWidth) {
                                        desiredWindowHeight4 = desiredWindowHeight3;
                                    }
                                } else {
                                    desiredWindowWidth4 = desiredWindowWidth3;
                                }
                                if (lp.height == i) {
                                    desiredWindowHeight4 = desiredWindowHeight3;
                                    if (frame.height() < desiredWindowHeight4) {
                                    }
                                } else {
                                    desiredWindowHeight4 = desiredWindowHeight3;
                                }
                            } else {
                                desiredWindowHeight4 = desiredWindowHeight3;
                                desiredWindowWidth4 = desiredWindowWidth3;
                            }
                            windowShouldResize = true;
                            boolean windowShouldResize2 = windowShouldResize | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
                            computesInternalInsets = !this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
                            int surfaceGenerationId = this.mSurface.getGenerationId();
                            boolean isViewVisible3 = viewVisibility != 0;
                            boolean windowRelayoutWasForced = this.mForceNextWindowRelayout;
                            surfaceSizeChanged = false;
                            surfaceCreated = false;
                            boolean surfaceDestroyed = false;
                            surfaceReplaced = false;
                            windowAttributesChanged = this.mWindowAttributesChanged;
                            if (windowAttributesChanged) {
                                relayoutResult = 0;
                                params2 = params;
                            } else {
                                relayoutResult = 0;
                                this.mWindowAttributesChanged = false;
                                params2 = lp;
                            }
                            if (params2 == null) {
                                insetsPending = false;
                                if ((host.mPrivateFlags & 512) != 0 && !PixelFormat.formatHasAlpha(params2.format)) {
                                    params2.format = -3;
                                }
                                adjustLayoutParamsForCompatibility(params2);
                                View view = this.mView;
                                updatedConfiguration = false;
                                boolean updatedConfiguration5 = view instanceof DecorView;
                                if (updatedConfiguration5) {
                                    ((DecorView) view).adjustLayoutParamsForWhiteNavigationBar(params2);
                                }
                                controlInsetsForCompatibility(params2);
                                if (this.mDispatchedSystemBarAppearance != params2.insetsFlags.appearance) {
                                    int i6 = params2.insetsFlags.appearance;
                                    this.mDispatchedSystemBarAppearance = i6;
                                    this.mView.onSystemBarAppearanceChanged(i6);
                                }
                            } else {
                                insetsPending = false;
                                updatedConfiguration = false;
                            }
                            insetsPending2 = this.mFirst;
                            if (!insetsPending2 || windowShouldResize2 || viewVisibilityChanged || params2 != null) {
                                frame2 = frame;
                            } else if (this.mForceNextWindowRelayout) {
                                frame2 = frame;
                            } else {
                                Rect frame5 = frame;
                                maybeHandleWindowMove(frame5);
                                isViewVisible = isViewVisible3;
                                windowAttributesChanged2 = windowAttributesChanged;
                                isViewVisible2 = layoutRequested;
                                relayoutResult2 = relayoutResult;
                                str = "======================================";
                                frame3 = frame5;
                                updatedConfiguration2 = updatedConfiguration;
                                if (!surfaceSizeChanged || surfaceReplaced || surfaceCreated || windowAttributesChanged2) {
                                    prepareSurfaces();
                                }
                                didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                                triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                                if (DEBUG_LAYOUT) {
                                    Log.v(this.mTag, "ViewRoot layout+ : " + host + ", layoutRequested = " + isViewVisible2 + ", frame = " + frame3 + ", mStopped = " + this.mStopped + ", host.getMeasuredWidth() = " + host.getMeasuredWidth() + ", host.getMeasuredHeight() = " + host.getMeasuredHeight());
                                }
                                if (!didLayout) {
                                    performLayout(lp, this.mWidth, this.mHeight);
                                    if ((host.mPrivateFlags & 512) != 0) {
                                        host.getLocationInWindow(this.mTmpLocation);
                                        Region region2 = this.mTransparentRegion;
                                        int[] iArr = this.mTmpLocation;
                                        int i7 = iArr[0];
                                        region2.set(i7, iArr[1], (host.mRight + i7) - host.mLeft, (this.mTmpLocation[1] + host.mBottom) - host.mTop);
                                        host.gatherTransparentRegion(this.mTransparentRegion);
                                        CompatibilityInfo.Translator translator = this.mTranslator;
                                        if (translator != null) {
                                            translator.translateRegionInWindowToScreen(this.mTransparentRegion);
                                        }
                                        if (!this.mTransparentRegion.equals(this.mPreviousTransparentRegion)) {
                                            this.mPreviousTransparentRegion.set(this.mTransparentRegion);
                                            this.mFullRedrawNeeded = true;
                                            SurfaceControl sc = getSurfaceControl();
                                            if (sc.isValid()) {
                                                this.mTransaction.setTransparentRegionHint(sc, this.mTransparentRegion).apply();
                                            }
                                        }
                                    }
                                    if (DBG) {
                                        System.out.println(str);
                                        System.out.println("performTraversals -- after setFrame");
                                        host.debug();
                                    }
                                }
                                if (!surfaceCreated) {
                                    notifySurfaceCreated();
                                } else if (surfaceReplaced) {
                                    notifySurfaceReplaced();
                                } else if (surfaceDestroyed) {
                                    notifySurfaceDestroyed();
                                }
                                if (triggerGlobalLayoutListener) {
                                    this.mAttachInfo.mRecomputeGlobalAttributes = false;
                                    this.mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
                                }
                                Rect visibleInsets = null;
                                Region touchableRegion = null;
                                int touchableInsetMode = 3;
                                boolean computedInternalInsets = false;
                                if (computesInternalInsets) {
                                    contentInsets = null;
                                } else {
                                    ViewTreeObserver.InternalInsetsInfo insets = this.mAttachInfo.mGivenInternalInsets;
                                    insets.reset();
                                    this.mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
                                    this.mAttachInfo.mHasNonEmptyGivenInternalInsets = !insets.isEmpty();
                                    if (insetsPending || !this.mLastGivenInsets.equals(insets)) {
                                        this.mLastGivenInsets.set(insets);
                                        CompatibilityInfo.Translator translator2 = this.mTranslator;
                                        if (translator2 != null) {
                                            Rect contentInsets3 = translator2.getTranslatedContentInsets(insets.contentInsets);
                                            CompatibilityInfo.Translator translator3 = this.mTranslator;
                                            Rect contentInsets4 = insets.visibleInsets;
                                            Rect visibleInsets2 = translator3.getTranslatedVisibleInsets(contentInsets4);
                                            touchableRegion = this.mTranslator.getTranslatedTouchableArea(insets.touchableRegion);
                                            visibleInsets = visibleInsets2;
                                            contentInsets2 = contentInsets3;
                                        } else {
                                            contentInsets2 = insets.contentInsets;
                                            visibleInsets = insets.visibleInsets;
                                            touchableRegion = insets.touchableRegion;
                                        }
                                        computedInternalInsets = true;
                                    } else {
                                        contentInsets2 = null;
                                    }
                                    touchableInsetMode = insets.mTouchableInsets;
                                    contentInsets = contentInsets2;
                                }
                                needsSetInsets = computedInternalInsets;
                                if (!(Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) && !needsSetInsets) {
                                    if (this.mTouchableRegion != null) {
                                        if (this.mPreviousTouchableRegion == null) {
                                            this.mPreviousTouchableRegion = new Region();
                                        }
                                        this.mPreviousTouchableRegion.set(this.mTouchableRegion);
                                        if (touchableInsetMode != 3) {
                                            Log.e(this.mTag, "Setting touchableInsetMode to non TOUCHABLE_INSETS_REGION from OnComputeInternalInsets, while also using setTouchableRegion causes setTouchableRegion to be ignored");
                                        }
                                    } else {
                                        this.mPreviousTouchableRegion = null;
                                    }
                                    if (contentInsets == null) {
                                        i2 = 0;
                                        contentInsets = new Rect(0, 0, 0, 0);
                                    } else {
                                        i2 = 0;
                                    }
                                    if (visibleInsets == null) {
                                        visibleInsets = new Rect(i2, i2, i2, i2);
                                    }
                                    if (touchableRegion == null) {
                                        touchableRegion = this.mTouchableRegion;
                                    } else if (touchableRegion != null && (region = this.mTouchableRegion) != null) {
                                        touchableRegion.op(touchableRegion, region, Region.Op.UNION);
                                    }
                                    try {
                                        this.mWindowSession.setInsets(this.mWindow, touchableInsetMode, contentInsets, visibleInsets, touchableRegion);
                                    } catch (RemoteException e) {
                                        throw e.rethrowFromSystemServer();
                                    }
                                } else if (this.mTouchableRegion == null && this.mPreviousTouchableRegion != null) {
                                    this.mPreviousTouchableRegion = null;
                                    try {
                                        this.mWindowSession.clearTouchableRegion(this.mWindow);
                                    } catch (RemoteException e2) {
                                        throw e2.rethrowFromSystemServer();
                                    }
                                }
                                if (this.mFirst) {
                                    if (!sAlwaysAssignFocus && isInTouchMode()) {
                                        View focused = this.mView.findFocus();
                                        if ((focused instanceof ViewGroup) && ((ViewGroup) focused).getDescendantFocusability() == 262144) {
                                            focused.restoreDefaultFocus();
                                        }
                                    }
                                    if (DEBUG_INPUT_RESIZE) {
                                        Log.v(this.mTag, "First: mView.hasFocus()=" + this.mView.hasFocus());
                                    }
                                    View view2 = this.mView;
                                    if (view2 != null) {
                                        if (!view2.hasFocus()) {
                                            this.mView.restoreDefaultFocus();
                                            if (DEBUG_INPUT_RESIZE) {
                                                Log.v(this.mTag, "First: requested focused view=" + this.mView.findFocus());
                                            }
                                        } else if (DEBUG_INPUT_RESIZE) {
                                            Log.v(this.mTag, "First: existing focused view=" + this.mView.findFocus());
                                        }
                                    }
                                }
                                if (isViewVisible) {
                                    changedVisibility = true;
                                    boolean hasWindowFocus = !this.mAttachInfo.mHasWindowFocus && isViewVisible;
                                    regainedFocus = !hasWindowFocus && this.mLostWindowFocus;
                                    if (regainedFocus) {
                                        this.mLostWindowFocus = false;
                                    } else if (!hasWindowFocus && this.mHadWindowFocus) {
                                        this.mLostWindowFocus = true;
                                    }
                                    if (!changedVisibility || regainedFocus) {
                                        isToast = this.mWindowAttributes.type == 2005;
                                        if (!isToast) {
                                            host.sendAccessibilityEvent(32);
                                        }
                                    }
                                    this.mFirst = false;
                                    this.mWillDrawSoon = false;
                                    this.mNewSurfaceNeeded = false;
                                    this.mActivityRelaunched = false;
                                    this.mViewVisibility = viewVisibility;
                                    this.mHadWindowFocus = hasWindowFocus;
                                    this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                    if ((relayoutResult2 & 1) != 0) {
                                        reportNextDraw();
                                    }
                                    cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                    if (!cancelAndRedraw) {
                                        createSyncIfNeeded();
                                    }
                                    if (!isViewVisible) {
                                        ArrayList<LayoutTransition> arrayList = this.mPendingTransitions;
                                        if (arrayList != null && arrayList.size() > 0) {
                                            int i8 = 0;
                                            while (true) {
                                                int relayoutResult5 = relayoutResult2;
                                                if (i8 >= this.mPendingTransitions.size()) {
                                                    break;
                                                }
                                                this.mPendingTransitions.get(i8).endChangingAnimations();
                                                i8++;
                                                relayoutResult2 = relayoutResult5;
                                            }
                                            this.mPendingTransitions.clear();
                                        }
                                        SurfaceSyncer.SyncBufferCallback syncBufferCallback2 = this.mSyncBufferCallback;
                                        if (syncBufferCallback2 != null) {
                                            syncBufferCallback2.onBufferReady(null);
                                        }
                                    } else if (cancelAndRedraw) {
                                        scheduleTraversals();
                                    } else {
                                        ArrayList<LayoutTransition> arrayList2 = this.mPendingTransitions;
                                        if (arrayList2 != null && arrayList2.size() > 0) {
                                            for (int i9 = 0; i9 < this.mPendingTransitions.size(); i9++) {
                                                this.mPendingTransitions.get(i9).startChangingAnimations();
                                            }
                                            this.mPendingTransitions.clear();
                                        }
                                        if (!performDraw() && (syncBufferCallback = this.mSyncBufferCallback) != null) {
                                            syncBufferCallback.onBufferReady(null);
                                        }
                                    }
                                    if (this.mAttachInfo.mContentCaptureEvents != null) {
                                        notifyContentCatpureEvents();
                                    }
                                    this.mIsInTraversal = false;
                                    this.mRelayoutRequested = false;
                                    if (cancelAndRedraw) {
                                        return;
                                    }
                                    this.mReportNextDraw = false;
                                    this.mSyncBufferCallback = null;
                                    this.mSyncBuffer = false;
                                    if (isInLocalSync()) {
                                        this.mSurfaceSyncer.markSyncReady(this.mSyncId);
                                        this.mSyncId = -1;
                                        return;
                                    }
                                    return;
                                }
                                changedVisibility = false;
                                if (this.mAttachInfo.mHasWindowFocus) {
                                }
                                if (hasWindowFocus) {
                                }
                                if (regainedFocus) {
                                }
                                if (!changedVisibility) {
                                }
                                isToast = this.mWindowAttributes.type == 2005;
                                if (!isToast) {
                                }
                                this.mFirst = false;
                                this.mWillDrawSoon = false;
                                this.mNewSurfaceNeeded = false;
                                this.mActivityRelaunched = false;
                                this.mViewVisibility = viewVisibility;
                                this.mHadWindowFocus = hasWindowFocus;
                                this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                if ((relayoutResult2 & 1) != 0) {
                                }
                                cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                if (!cancelAndRedraw) {
                                }
                                if (!isViewVisible) {
                                }
                                if (this.mAttachInfo.mContentCaptureEvents != null) {
                                }
                                this.mIsInTraversal = false;
                                this.mRelayoutRequested = false;
                                if (cancelAndRedraw) {
                                }
                            }
                            if (!DEBUG_LAYOUT) {
                                Log.v(this.mTag, "ViewRoot adjustSize+ : mFirst = " + this.mFirst + ", windowShouldResize = " + windowShouldResize2 + ", viewVisibilityChanged = " + viewVisibilityChanged + ", params = " + params2 + ", mForceNextWindowRelayout = " + this.mForceNextWindowRelayout + ", isViewVisible = " + isViewVisible3);
                            }
                            str = "======================================";
                            windowAttributesChanged2 = windowAttributesChanged;
                            if (Trace.isTagEnabled(8L)) {
                                Object[] objArr = new Object[5];
                                objArr[0] = Boolean.valueOf(this.mFirst);
                                objArr[1] = Boolean.valueOf(windowShouldResize2);
                                objArr[2] = Boolean.valueOf(viewVisibilityChanged);
                                objArr[3] = Boolean.valueOf(params2 != null);
                                objArr[4] = Boolean.valueOf(this.mForceNextWindowRelayout);
                                Trace.traceBegin(8L, TextUtils.formatSimple("relayoutWindow#first=%b/resize=%b/vis=%b/params=%b/force=%b", objArr));
                            }
                            this.mForceNextWindowRelayout = false;
                            baseSurfaceHolder = this.mSurfaceHolder;
                            if (baseSurfaceHolder != null) {
                                baseSurfaceHolder.mSurfaceLock.lock();
                                this.mDrawingAllowed = true;
                            }
                            boolean hwInitialized3 = false;
                            dispatchApplyInsets = false;
                            boolean hadSurface = this.mSurface.isValid();
                            isViewVisible = isViewVisible3;
                            if (DEBUG_LAYOUT) {
                                hwInitialized = false;
                            } else {
                                try {
                                } catch (RemoteException e3) {
                                    e = e3;
                                    insetsPending3 = computesInternalInsets;
                                    relayoutResult3 = relayoutResult;
                                    frame3 = frame2;
                                    updatedConfiguration3 = updatedConfiguration;
                                } catch (Throwable th) {
                                    e = th;
                                }
                                try {
                                    hwInitialized = false;
                                } catch (RemoteException e4) {
                                    e = e4;
                                    insetsPending3 = computesInternalInsets;
                                    relayoutResult3 = relayoutResult;
                                    frame3 = frame2;
                                    updatedConfiguration3 = updatedConfiguration;
                                    try {
                                        Log.e(this.mTag, "RemoteException happens in " + this, e);
                                        if (Trace.isTagEnabled(8L)) {
                                        }
                                        hwInitialized = hwInitialized3;
                                        dispatchApplyInsets2 = dispatchApplyInsets;
                                        if (!DEBUG_ORIENTATION) {
                                        }
                                        Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                                        this.mAttachInfo.mWindowLeft = frame3.left;
                                        this.mAttachInfo.mWindowTop = frame3.top;
                                        if (this.mWidth == frame3.width()) {
                                        }
                                        this.mWidth = frame3.width();
                                        this.mHeight = frame3.height();
                                        if (this.mSurfaceHolder != null) {
                                        }
                                        threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                        if (threadedRenderer != null) {
                                        }
                                        if (this.mStopped) {
                                        }
                                        int childWidthMeasureSpec2 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                                        int childHeightMeasureSpec = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                                        if (DEBUG_LAYOUT) {
                                        }
                                        performMeasure(childWidthMeasureSpec2, childHeightMeasureSpec);
                                        int width = host.getMeasuredWidth();
                                        int height = host.getMeasuredHeight();
                                        measureAgain = false;
                                        if (lp.horizontalWeight > 0.0f) {
                                        }
                                        if (lp.verticalWeight > 0.0f) {
                                        }
                                        if (measureAgain) {
                                        }
                                        layoutRequested = true;
                                        isViewVisible2 = layoutRequested;
                                        relayoutResult2 = relayoutResult4;
                                        updatedConfiguration2 = updatedConfiguration4;
                                        insetsPending = insetsPending3;
                                        if (!surfaceSizeChanged) {
                                        }
                                        prepareSurfaces();
                                        didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                                        triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                                        if (DEBUG_LAYOUT) {
                                        }
                                        if (!didLayout) {
                                        }
                                        if (!surfaceCreated) {
                                        }
                                        if (triggerGlobalLayoutListener) {
                                        }
                                        Rect visibleInsets3 = null;
                                        Region touchableRegion2 = null;
                                        int touchableInsetMode2 = 3;
                                        boolean computedInternalInsets2 = false;
                                        if (computesInternalInsets) {
                                        }
                                        needsSetInsets = computedInternalInsets2;
                                        if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                                        }
                                        if (this.mFirst) {
                                        }
                                        if (isViewVisible) {
                                        }
                                        changedVisibility = false;
                                        if (this.mAttachInfo.mHasWindowFocus) {
                                        }
                                        if (hasWindowFocus) {
                                        }
                                        if (regainedFocus) {
                                        }
                                        if (!changedVisibility) {
                                        }
                                        isToast = this.mWindowAttributes.type == 2005;
                                        if (!isToast) {
                                        }
                                        this.mFirst = false;
                                        this.mWillDrawSoon = false;
                                        this.mNewSurfaceNeeded = false;
                                        this.mActivityRelaunched = false;
                                        this.mViewVisibility = viewVisibility;
                                        this.mHadWindowFocus = hasWindowFocus;
                                        this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                        if ((relayoutResult2 & 1) != 0) {
                                        }
                                        cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                        if (!cancelAndRedraw) {
                                        }
                                        if (!isViewVisible) {
                                        }
                                        if (this.mAttachInfo.mContentCaptureEvents != null) {
                                        }
                                        this.mIsInTraversal = false;
                                        this.mRelayoutRequested = false;
                                        if (cancelAndRedraw) {
                                        }
                                    } catch (Throwable th2) {
                                        e = th2;
                                        if (Trace.isTagEnabled(8L)) {
                                            Trace.traceEnd(8L);
                                        }
                                        throw e;
                                    }
                                } catch (Throwable th3) {
                                    e = th3;
                                    if (Trace.isTagEnabled(8L)) {
                                    }
                                    throw e;
                                }
                                try {
                                    Log.i(this.mTag, "host=w:" + host.getMeasuredWidth() + ", h:" + host.getMeasuredHeight() + ", params=" + params2 + " surface=" + this.mSurface + ",hadSurface = " + hadSurface);
                                } catch (RemoteException e5) {
                                    e = e5;
                                    insetsPending3 = computesInternalInsets;
                                    relayoutResult3 = relayoutResult;
                                    hwInitialized3 = hwInitialized;
                                    frame3 = frame2;
                                    updatedConfiguration3 = updatedConfiguration;
                                    Log.e(this.mTag, "RemoteException happens in " + this, e);
                                    if (Trace.isTagEnabled(8L)) {
                                    }
                                    hwInitialized = hwInitialized3;
                                    dispatchApplyInsets2 = dispatchApplyInsets;
                                    if (!DEBUG_ORIENTATION) {
                                    }
                                    Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                                    this.mAttachInfo.mWindowLeft = frame3.left;
                                    this.mAttachInfo.mWindowTop = frame3.top;
                                    if (this.mWidth == frame3.width()) {
                                    }
                                    this.mWidth = frame3.width();
                                    this.mHeight = frame3.height();
                                    if (this.mSurfaceHolder != null) {
                                    }
                                    threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                    if (threadedRenderer != null) {
                                    }
                                    if (this.mStopped) {
                                    }
                                    int childWidthMeasureSpec22 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                                    int childHeightMeasureSpec2 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                                    if (DEBUG_LAYOUT) {
                                    }
                                    performMeasure(childWidthMeasureSpec22, childHeightMeasureSpec2);
                                    int width2 = host.getMeasuredWidth();
                                    int height2 = host.getMeasuredHeight();
                                    measureAgain = false;
                                    if (lp.horizontalWeight > 0.0f) {
                                    }
                                    if (lp.verticalWeight > 0.0f) {
                                    }
                                    if (measureAgain) {
                                    }
                                    layoutRequested = true;
                                    isViewVisible2 = layoutRequested;
                                    relayoutResult2 = relayoutResult4;
                                    updatedConfiguration2 = updatedConfiguration4;
                                    insetsPending = insetsPending3;
                                    if (!surfaceSizeChanged) {
                                    }
                                    prepareSurfaces();
                                    didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                                    triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                                    if (DEBUG_LAYOUT) {
                                    }
                                    if (!didLayout) {
                                    }
                                    if (!surfaceCreated) {
                                    }
                                    if (triggerGlobalLayoutListener) {
                                    }
                                    Rect visibleInsets32 = null;
                                    Region touchableRegion22 = null;
                                    int touchableInsetMode22 = 3;
                                    boolean computedInternalInsets22 = false;
                                    if (computesInternalInsets) {
                                    }
                                    needsSetInsets = computedInternalInsets22;
                                    if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                                    }
                                    if (this.mFirst) {
                                    }
                                    if (isViewVisible) {
                                    }
                                    changedVisibility = false;
                                    if (this.mAttachInfo.mHasWindowFocus) {
                                    }
                                    if (hasWindowFocus) {
                                    }
                                    if (regainedFocus) {
                                    }
                                    if (!changedVisibility) {
                                    }
                                    isToast = this.mWindowAttributes.type == 2005;
                                    if (!isToast) {
                                    }
                                    this.mFirst = false;
                                    this.mWillDrawSoon = false;
                                    this.mNewSurfaceNeeded = false;
                                    this.mActivityRelaunched = false;
                                    this.mViewVisibility = viewVisibility;
                                    this.mHadWindowFocus = hasWindowFocus;
                                    this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                    if ((relayoutResult2 & 1) != 0) {
                                    }
                                    cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                    if (!cancelAndRedraw) {
                                    }
                                    if (!isViewVisible) {
                                    }
                                    if (this.mAttachInfo.mContentCaptureEvents != null) {
                                    }
                                    this.mIsInTraversal = false;
                                    this.mRelayoutRequested = false;
                                    if (cancelAndRedraw) {
                                    }
                                } catch (Throwable th4) {
                                    e = th4;
                                    if (Trace.isTagEnabled(8L)) {
                                    }
                                    throw e;
                                }
                            }
                            if (Trace.isTagEnabled(8L)) {
                                Trace.traceBegin(8L, "relayoutWindow");
                            }
                            if (!this.mFirst || viewVisibilityChanged) {
                                this.mViewFrameInfo.flags |= 1;
                            }
                            relayoutResult = relayoutWindow(params2, viewVisibility, computesInternalInsets);
                            dragResizing = this.mPendingDragResizing;
                            i3 = this.mSyncSeqId;
                            if (i3 > this.mLastSyncSeqId) {
                                this.mLastSyncSeqId = i3;
                                if (DEBUG_BLAST) {
                                    Log.d(this.mTag, "Relayout called with blastSync");
                                }
                                reportNextDraw();
                                this.mSyncBuffer = true;
                            }
                            boolean surfaceControlChanged = (relayoutResult & 2) != 2;
                            if (this.mSurfaceControl.isValid()) {
                                updateOpacity(this.mWindowAttributes, dragResizing, surfaceControlChanged);
                                if (surfaceControlChanged && this.mDisplayDecorationCached) {
                                    updateDisplayDecoration();
                                }
                            }
                            if (DEBUG_LAYOUT) {
                                insetsPending3 = computesInternalInsets;
                            } else {
                                try {
                                    try {
                                        insetsPending3 = computesInternalInsets;
                                        try {
                                            Log.v(this.mTag, "relayout: frame=" + frame2.toShortString() + " surface=" + this.mSurface);
                                        } catch (RemoteException e6) {
                                            e = e6;
                                            frame3 = frame2;
                                            relayoutResult3 = relayoutResult;
                                            updatedConfiguration3 = updatedConfiguration;
                                            hwInitialized3 = hwInitialized;
                                            Log.e(this.mTag, "RemoteException happens in " + this, e);
                                            if (Trace.isTagEnabled(8L)) {
                                                Trace.traceEnd(8L);
                                            }
                                            hwInitialized = hwInitialized3;
                                            dispatchApplyInsets2 = dispatchApplyInsets;
                                            if (!DEBUG_ORIENTATION) {
                                            }
                                            Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                                            this.mAttachInfo.mWindowLeft = frame3.left;
                                            this.mAttachInfo.mWindowTop = frame3.top;
                                            if (this.mWidth == frame3.width()) {
                                            }
                                            this.mWidth = frame3.width();
                                            this.mHeight = frame3.height();
                                            if (this.mSurfaceHolder != null) {
                                            }
                                            threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                            if (threadedRenderer != null) {
                                            }
                                            if (this.mStopped) {
                                            }
                                            int childWidthMeasureSpec222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                                            int childHeightMeasureSpec22 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                                            if (DEBUG_LAYOUT) {
                                            }
                                            performMeasure(childWidthMeasureSpec222, childHeightMeasureSpec22);
                                            int width22 = host.getMeasuredWidth();
                                            int height22 = host.getMeasuredHeight();
                                            measureAgain = false;
                                            if (lp.horizontalWeight > 0.0f) {
                                            }
                                            if (lp.verticalWeight > 0.0f) {
                                            }
                                            if (measureAgain) {
                                            }
                                            layoutRequested = true;
                                            isViewVisible2 = layoutRequested;
                                            relayoutResult2 = relayoutResult4;
                                            updatedConfiguration2 = updatedConfiguration4;
                                            insetsPending = insetsPending3;
                                            if (!surfaceSizeChanged) {
                                            }
                                            prepareSurfaces();
                                            didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                                            triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                                            if (DEBUG_LAYOUT) {
                                            }
                                            if (!didLayout) {
                                            }
                                            if (!surfaceCreated) {
                                            }
                                            if (triggerGlobalLayoutListener) {
                                            }
                                            Rect visibleInsets322 = null;
                                            Region touchableRegion222 = null;
                                            int touchableInsetMode222 = 3;
                                            boolean computedInternalInsets222 = false;
                                            if (computesInternalInsets) {
                                            }
                                            needsSetInsets = computedInternalInsets222;
                                            if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                                            }
                                            if (this.mFirst) {
                                            }
                                            if (isViewVisible) {
                                            }
                                            changedVisibility = false;
                                            if (this.mAttachInfo.mHasWindowFocus) {
                                            }
                                            if (hasWindowFocus) {
                                            }
                                            if (regainedFocus) {
                                            }
                                            if (!changedVisibility) {
                                            }
                                            isToast = this.mWindowAttributes.type == 2005;
                                            if (!isToast) {
                                            }
                                            this.mFirst = false;
                                            this.mWillDrawSoon = false;
                                            this.mNewSurfaceNeeded = false;
                                            this.mActivityRelaunched = false;
                                            this.mViewVisibility = viewVisibility;
                                            this.mHadWindowFocus = hasWindowFocus;
                                            this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                            if ((relayoutResult2 & 1) != 0) {
                                            }
                                            cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                            if (!cancelAndRedraw) {
                                            }
                                            if (!isViewVisible) {
                                            }
                                            if (this.mAttachInfo.mContentCaptureEvents != null) {
                                            }
                                            this.mIsInTraversal = false;
                                            this.mRelayoutRequested = false;
                                            if (cancelAndRedraw) {
                                            }
                                        } catch (Throwable th5) {
                                            e = th5;
                                            if (Trace.isTagEnabled(8L)) {
                                            }
                                            throw e;
                                        }
                                    } catch (RemoteException e7) {
                                        e = e7;
                                        insetsPending3 = computesInternalInsets;
                                        frame3 = frame2;
                                        relayoutResult3 = relayoutResult;
                                        updatedConfiguration3 = updatedConfiguration;
                                        hwInitialized3 = hwInitialized;
                                    } catch (Throwable th6) {
                                        e = th6;
                                    }
                                } catch (RemoteException e8) {
                                    e = e8;
                                    insetsPending3 = computesInternalInsets;
                                    frame3 = frame2;
                                    relayoutResult3 = relayoutResult;
                                    updatedConfiguration3 = updatedConfiguration;
                                    hwInitialized3 = hwInitialized;
                                } catch (Throwable th7) {
                                    e = th7;
                                }
                            }
                            if (Trace.isTagEnabled(8L)) {
                                Trace.traceEnd(8L);
                            }
                            if (!this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
                                if (DEBUG_CONFIGURATION) {
                                    Log.v(this.mTag, "Visible with new config: " + this.mPendingMergedConfiguration.getMergedConfiguration());
                                }
                                performConfigurationChange(new MergedConfiguration(this.mPendingMergedConfiguration), !this.mFirst, -1);
                                updatedConfiguration = true;
                            }
                            surfaceSizeChanged = false;
                            if (!this.mLastSurfaceSize.equals(this.mSurfaceSize)) {
                                surfaceSizeChanged = true;
                                this.mLastSurfaceSize.set(this.mSurfaceSize.x, this.mSurfaceSize.y);
                            }
                            frame3 = this.mPendingAlwaysConsumeSystemBars == this.mAttachInfo.mAlwaysConsumeSystemBars ? 1 : 0;
                            updateColorModeIfNeeded(lp.getColorMode());
                            surfaceCreated = hadSurface && this.mSurface.isValid();
                            surfaceDestroyed = (hadSurface || this.mSurface.isValid()) ? false : true;
                            surfaceReplaced = (surfaceGenerationId == this.mSurface.getGenerationId() || surfaceControlChanged) && this.mSurface.isValid();
                            if (surfaceReplaced) {
                                this.mSurfaceSequenceId++;
                            }
                            if (frame3 != null) {
                                this.mAttachInfo.mAlwaysConsumeSystemBars = this.mPendingAlwaysConsumeSystemBars;
                                dispatchApplyInsets = true;
                            }
                            if (updateCaptionInsets()) {
                                dispatchApplyInsets = true;
                            }
                            if (!dispatchApplyInsets || this.mLastSystemUiVisibility != this.mAttachInfo.mSystemUiVisibility || this.mApplyInsetsRequested) {
                                this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                                dispatchApplyInsets(host);
                                dispatchApplyInsets = true;
                            }
                            if (!surfaceCreated) {
                                this.mFullRedrawNeeded = true;
                                this.mPreviousTransparentRegion.setEmpty();
                                if (this.mAttachInfo.mThreadedRenderer != null) {
                                    try {
                                        hwInitialized2 = this.mAttachInfo.mThreadedRenderer.initialize(this.mSurface);
                                        if (hwInitialized2) {
                                            try {
                                                try {
                                                    if ((host.mPrivateFlags & 512) == 0) {
                                                        this.mAttachInfo.mThreadedRenderer.allocateBuffers();
                                                    }
                                                } catch (Surface.OutOfResourcesException e9) {
                                                    e = e9;
                                                    handleOutOfResourcesException(e);
                                                    if (Trace.isTagEnabled(8L)) {
                                                        Trace.traceEnd(8L);
                                                        return;
                                                    }
                                                    return;
                                                }
                                            } catch (RemoteException e10) {
                                                e = e10;
                                                hwInitialized3 = hwInitialized2;
                                                frame3 = frame2;
                                                relayoutResult3 = relayoutResult;
                                                updatedConfiguration3 = updatedConfiguration;
                                                Log.e(this.mTag, "RemoteException happens in " + this, e);
                                                if (Trace.isTagEnabled(8L)) {
                                                }
                                                hwInitialized = hwInitialized3;
                                                dispatchApplyInsets2 = dispatchApplyInsets;
                                                if (!DEBUG_ORIENTATION) {
                                                }
                                                Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                                                this.mAttachInfo.mWindowLeft = frame3.left;
                                                this.mAttachInfo.mWindowTop = frame3.top;
                                                if (this.mWidth == frame3.width()) {
                                                }
                                                this.mWidth = frame3.width();
                                                this.mHeight = frame3.height();
                                                if (this.mSurfaceHolder != null) {
                                                }
                                                threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                                                if (threadedRenderer != null) {
                                                }
                                                if (this.mStopped) {
                                                }
                                                int childWidthMeasureSpec2222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                                                int childHeightMeasureSpec222 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                                                if (DEBUG_LAYOUT) {
                                                }
                                                performMeasure(childWidthMeasureSpec2222, childHeightMeasureSpec222);
                                                int width222 = host.getMeasuredWidth();
                                                int height222 = host.getMeasuredHeight();
                                                measureAgain = false;
                                                if (lp.horizontalWeight > 0.0f) {
                                                }
                                                if (lp.verticalWeight > 0.0f) {
                                                }
                                                if (measureAgain) {
                                                }
                                                layoutRequested = true;
                                                isViewVisible2 = layoutRequested;
                                                relayoutResult2 = relayoutResult4;
                                                updatedConfiguration2 = updatedConfiguration4;
                                                insetsPending = insetsPending3;
                                                if (!surfaceSizeChanged) {
                                                }
                                                prepareSurfaces();
                                                didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                                                triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                                                if (DEBUG_LAYOUT) {
                                                }
                                                if (!didLayout) {
                                                }
                                                if (!surfaceCreated) {
                                                }
                                                if (triggerGlobalLayoutListener) {
                                                }
                                                Rect visibleInsets3222 = null;
                                                Region touchableRegion2222 = null;
                                                int touchableInsetMode2222 = 3;
                                                boolean computedInternalInsets2222 = false;
                                                if (computesInternalInsets) {
                                                }
                                                needsSetInsets = computedInternalInsets2222;
                                                if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                                                }
                                                if (this.mFirst) {
                                                }
                                                if (isViewVisible) {
                                                }
                                                changedVisibility = false;
                                                if (this.mAttachInfo.mHasWindowFocus) {
                                                }
                                                if (hasWindowFocus) {
                                                }
                                                if (regainedFocus) {
                                                }
                                                if (!changedVisibility) {
                                                }
                                                isToast = this.mWindowAttributes.type == 2005;
                                                if (!isToast) {
                                                }
                                                this.mFirst = false;
                                                this.mWillDrawSoon = false;
                                                this.mNewSurfaceNeeded = false;
                                                this.mActivityRelaunched = false;
                                                this.mViewVisibility = viewVisibility;
                                                this.mHadWindowFocus = hasWindowFocus;
                                                this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                                                if ((relayoutResult2 & 1) != 0) {
                                                }
                                                cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                                                if (!cancelAndRedraw) {
                                                }
                                                if (!isViewVisible) {
                                                }
                                                if (this.mAttachInfo.mContentCaptureEvents != null) {
                                                }
                                                this.mIsInTraversal = false;
                                                this.mRelayoutRequested = false;
                                                if (cancelAndRedraw) {
                                                }
                                            } catch (Throwable th8) {
                                                e = th8;
                                                if (Trace.isTagEnabled(8L)) {
                                                }
                                                throw e;
                                            }
                                        }
                                        hwInitialized = hwInitialized2;
                                    } catch (Surface.OutOfResourcesException e11) {
                                        e = e11;
                                        hwInitialized2 = hwInitialized;
                                    }
                                }
                            } else if (surfaceDestroyed) {
                                WeakReference<View> weakReference = this.mLastScrolledFocus;
                                if (weakReference != null) {
                                    weakReference.clear();
                                }
                                this.mCurScrollY = 0;
                                this.mScrollY = 0;
                                View view3 = this.mView;
                                if (view3 instanceof RootViewSurfaceTaker) {
                                    ((RootViewSurfaceTaker) view3).onRootViewScrollYChanged(0);
                                }
                                Scroller scroller = this.mScroller;
                                if (scroller != null) {
                                    scroller.abortAnimation();
                                }
                                if (isHardwareEnabled()) {
                                    this.mAttachInfo.mThreadedRenderer.destroy();
                                }
                            } else if ((surfaceReplaced || surfaceSizeChanged || windowRelayoutWasForced) && this.mSurfaceHolder == null && this.mAttachInfo.mThreadedRenderer != null && this.mSurface.isValid()) {
                                this.mFullRedrawNeeded = true;
                                try {
                                    this.mAttachInfo.mThreadedRenderer.updateSurface(this.mSurface);
                                } catch (Surface.OutOfResourcesException e12) {
                                    handleOutOfResourcesException(e12);
                                    if (Trace.isTagEnabled(8L)) {
                                        Trace.traceEnd(8L);
                                        return;
                                    }
                                    return;
                                }
                            }
                            if (this.mDragResizing != dragResizing) {
                                dragResizing2 = dragResizing;
                                frame3 = frame2;
                            } else if (dragResizing) {
                                boolean backdropSizeMatchesFrame = this.mWinFrame.width() == this.mPendingBackDropFrame.width() && this.mWinFrame.height() == this.mPendingBackDropFrame.height();
                                dragResizing2 = dragResizing;
                                frame3 = frame2;
                                startDragResizing(this.mPendingBackDropFrame, !backdropSizeMatchesFrame, this.mAttachInfo.mContentInsets, this.mAttachInfo.mStableInsets, this.mResizeMode);
                            } else {
                                dragResizing2 = dragResizing;
                                frame3 = frame2;
                                endDragResizing();
                            }
                            if (!this.mUseMTRenderer) {
                                if (dragResizing2) {
                                    this.mCanvasOffsetX = this.mWinFrame.left;
                                    this.mCanvasOffsetY = this.mWinFrame.top;
                                } else {
                                    this.mCanvasOffsetY = 0;
                                    this.mCanvasOffsetX = 0;
                                }
                            }
                            if (Trace.isTagEnabled(8L)) {
                                Trace.traceEnd(8L);
                            }
                            dispatchApplyInsets2 = dispatchApplyInsets;
                            relayoutResult3 = relayoutResult;
                            updatedConfiguration3 = updatedConfiguration;
                            if (!DEBUG_ORIENTATION || DEBUG_LAYOUT) {
                                Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                            }
                            this.mAttachInfo.mWindowLeft = frame3.left;
                            this.mAttachInfo.mWindowTop = frame3.top;
                            if (this.mWidth == frame3.width() || this.mHeight != frame3.height()) {
                                this.mWidth = frame3.width();
                                this.mHeight = frame3.height();
                            }
                            if (this.mSurfaceHolder != null) {
                                if (this.mSurface.isValid()) {
                                    this.mSurfaceHolder.mSurface = this.mSurface;
                                }
                                this.mSurfaceHolder.setSurfaceFrameSize(this.mWidth, this.mHeight);
                                if (surfaceCreated) {
                                    this.mSurfaceHolder.ungetCallbacks();
                                    this.mIsCreating = true;
                                    SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                                    if (callbacks != null) {
                                        for (SurfaceHolder.Callback c : callbacks) {
                                            c.surfaceCreated(this.mSurfaceHolder);
                                        }
                                    }
                                }
                                if (!surfaceCreated && !surfaceReplaced && !surfaceSizeChanged && !windowAttributesChanged2) {
                                    relayoutResult4 = relayoutResult3;
                                } else if (this.mSurface.isValid()) {
                                    SurfaceHolder.Callback[] callbacks2 = this.mSurfaceHolder.getCallbacks();
                                    if (callbacks2 != null) {
                                        int i10 = 0;
                                        for (int length = callbacks2.length; i10 < length; length = length) {
                                            SurfaceHolder.Callback c2 = callbacks2[i10];
                                            BaseSurfaceHolder baseSurfaceHolder2 = this.mSurfaceHolder;
                                            SurfaceHolder.Callback[] callbacks3 = callbacks2;
                                            int i11 = lp.format;
                                            int relayoutResult6 = relayoutResult3;
                                            int relayoutResult7 = this.mWidth;
                                            c2.surfaceChanged(baseSurfaceHolder2, i11, relayoutResult7, this.mHeight);
                                            i10++;
                                            callbacks2 = callbacks3;
                                            relayoutResult3 = relayoutResult6;
                                        }
                                        relayoutResult4 = relayoutResult3;
                                    } else {
                                        relayoutResult4 = relayoutResult3;
                                    }
                                    this.mIsCreating = false;
                                } else {
                                    relayoutResult4 = relayoutResult3;
                                }
                                if (surfaceDestroyed) {
                                    notifyHolderSurfaceDestroyed();
                                    this.mSurfaceHolder.mSurfaceLock.lock();
                                    try {
                                        this.mSurfaceHolder.mSurface = new Surface();
                                    } finally {
                                        this.mSurfaceHolder.mSurfaceLock.unlock();
                                    }
                                }
                            } else {
                                relayoutResult4 = relayoutResult3;
                            }
                            threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                            if (threadedRenderer != null && threadedRenderer.isEnabled() && (hwInitialized || this.mWidth != threadedRenderer.getWidth() || this.mHeight != threadedRenderer.getHeight() || this.mNeedsRendererSetup)) {
                                threadedRenderer.setup(this.mWidth, this.mHeight, this.mAttachInfo, this.mWindowAttributes.surfaceInsets);
                                this.mNeedsRendererSetup = false;
                            }
                            if ((this.mStopped || this.mReportNextDraw) && (this.mWidth != host.getMeasuredWidth() || this.mHeight != host.getMeasuredHeight() || dispatchApplyInsets2 || updatedConfiguration3)) {
                                int childWidthMeasureSpec22222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                                int childHeightMeasureSpec2222 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                                if (DEBUG_LAYOUT) {
                                    Log.v(this.mTag, "Ooops, something changed!  mWidth=" + this.mWidth + " measuredWidth=" + host.getMeasuredWidth() + " mHeight=" + this.mHeight + " measuredHeight=" + host.getMeasuredHeight() + " dispatchApplyInsets=" + dispatchApplyInsets2);
                                }
                                performMeasure(childWidthMeasureSpec22222, childHeightMeasureSpec2222);
                                int width2222 = host.getMeasuredWidth();
                                int height2222 = host.getMeasuredHeight();
                                measureAgain = false;
                                if (lp.horizontalWeight > 0.0f) {
                                    width2222 += (int) ((this.mWidth - width2222) * lp.horizontalWeight);
                                    childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width2222, 1073741824);
                                    measureAgain = true;
                                } else {
                                    childWidthMeasureSpec = childWidthMeasureSpec22222;
                                }
                                if (lp.verticalWeight > 0.0f) {
                                    updatedConfiguration4 = updatedConfiguration3;
                                    height2222 += (int) ((this.mHeight - height2222) * lp.verticalWeight);
                                    childHeightMeasureSpec2222 = View.MeasureSpec.makeMeasureSpec(height2222, 1073741824);
                                    measureAgain = true;
                                } else {
                                    updatedConfiguration4 = updatedConfiguration3;
                                }
                                if (measureAgain) {
                                    if (DEBUG_LAYOUT) {
                                        Log.v(this.mTag, "And hey let's measure once more: width=" + width2222 + " height=" + height2222);
                                    }
                                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec2222);
                                }
                                layoutRequested = true;
                            } else {
                                updatedConfiguration4 = updatedConfiguration3;
                            }
                            isViewVisible2 = layoutRequested;
                            relayoutResult2 = relayoutResult4;
                            updatedConfiguration2 = updatedConfiguration4;
                            insetsPending = insetsPending3;
                            if (!surfaceSizeChanged) {
                            }
                            prepareSurfaces();
                            didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                            triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                            if (DEBUG_LAYOUT) {
                            }
                            if (!didLayout) {
                            }
                            if (!surfaceCreated) {
                            }
                            if (triggerGlobalLayoutListener) {
                            }
                            Rect visibleInsets32222 = null;
                            Region touchableRegion22222 = null;
                            int touchableInsetMode22222 = 3;
                            boolean computedInternalInsets22222 = false;
                            if (computesInternalInsets) {
                            }
                            needsSetInsets = computedInternalInsets22222;
                            if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                            }
                            if (this.mFirst) {
                            }
                            if (isViewVisible) {
                            }
                            changedVisibility = false;
                            if (this.mAttachInfo.mHasWindowFocus) {
                            }
                            if (hasWindowFocus) {
                            }
                            if (regainedFocus) {
                            }
                            if (!changedVisibility) {
                            }
                            isToast = this.mWindowAttributes.type == 2005;
                            if (!isToast) {
                            }
                            this.mFirst = false;
                            this.mWillDrawSoon = false;
                            this.mNewSurfaceNeeded = false;
                            this.mActivityRelaunched = false;
                            this.mViewVisibility = viewVisibility;
                            this.mHadWindowFocus = hasWindowFocus;
                            this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                            if ((relayoutResult2 & 1) != 0) {
                            }
                            cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                            if (!cancelAndRedraw) {
                            }
                            if (!isViewVisible) {
                            }
                            if (this.mAttachInfo.mContentCaptureEvents != null) {
                            }
                            this.mIsInTraversal = false;
                            this.mRelayoutRequested = false;
                            if (cancelAndRedraw) {
                            }
                        }
                        windowShouldResize = false;
                        boolean windowShouldResize22 = windowShouldResize | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
                        computesInternalInsets = !this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
                        int surfaceGenerationId2 = this.mSurface.getGenerationId();
                        if (viewVisibility != 0) {
                        }
                        boolean windowRelayoutWasForced2 = this.mForceNextWindowRelayout;
                        surfaceSizeChanged = false;
                        surfaceCreated = false;
                        boolean surfaceDestroyed2 = false;
                        surfaceReplaced = false;
                        windowAttributesChanged = this.mWindowAttributesChanged;
                        if (windowAttributesChanged) {
                        }
                        if (params2 == null) {
                        }
                        insetsPending2 = this.mFirst;
                        if (insetsPending2) {
                        }
                        frame2 = frame;
                        if (!DEBUG_LAYOUT) {
                        }
                        str = "======================================";
                        windowAttributesChanged2 = windowAttributesChanged;
                        if (Trace.isTagEnabled(8L)) {
                        }
                        this.mForceNextWindowRelayout = false;
                        baseSurfaceHolder = this.mSurfaceHolder;
                        if (baseSurfaceHolder != null) {
                        }
                        boolean hwInitialized32 = false;
                        dispatchApplyInsets = false;
                        boolean hadSurface2 = this.mSurface.isValid();
                        isViewVisible = isViewVisible3;
                        if (DEBUG_LAYOUT) {
                        }
                        if (Trace.isTagEnabled(8L)) {
                        }
                        if (!this.mFirst) {
                        }
                        this.mViewFrameInfo.flags |= 1;
                        relayoutResult = relayoutWindow(params2, viewVisibility, computesInternalInsets);
                        dragResizing = this.mPendingDragResizing;
                        i3 = this.mSyncSeqId;
                        if (i3 > this.mLastSyncSeqId) {
                        }
                        boolean surfaceControlChanged2 = (relayoutResult & 2) != 2;
                        if (this.mSurfaceControl.isValid()) {
                        }
                        if (DEBUG_LAYOUT) {
                        }
                        if (Trace.isTagEnabled(8L)) {
                        }
                        if (!this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
                        }
                        surfaceSizeChanged = false;
                        if (!this.mLastSurfaceSize.equals(this.mSurfaceSize)) {
                        }
                        frame3 = this.mPendingAlwaysConsumeSystemBars == this.mAttachInfo.mAlwaysConsumeSystemBars ? 1 : 0;
                        updateColorModeIfNeeded(lp.getColorMode());
                        surfaceCreated = hadSurface2 && this.mSurface.isValid();
                        surfaceDestroyed2 = (hadSurface2 || this.mSurface.isValid()) ? false : true;
                        surfaceReplaced = (surfaceGenerationId2 == this.mSurface.getGenerationId() || surfaceControlChanged2) && this.mSurface.isValid();
                        if (surfaceReplaced) {
                        }
                        if (frame3 != null) {
                        }
                        if (updateCaptionInsets()) {
                        }
                        if (!dispatchApplyInsets) {
                        }
                        this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                        dispatchApplyInsets(host);
                        dispatchApplyInsets = true;
                        if (!surfaceCreated) {
                        }
                        if (this.mDragResizing != dragResizing) {
                        }
                        if (!this.mUseMTRenderer) {
                        }
                        if (Trace.isTagEnabled(8L)) {
                        }
                        dispatchApplyInsets2 = dispatchApplyInsets;
                        relayoutResult3 = relayoutResult;
                        updatedConfiguration3 = updatedConfiguration;
                        if (!DEBUG_ORIENTATION) {
                        }
                        Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                        this.mAttachInfo.mWindowLeft = frame3.left;
                        this.mAttachInfo.mWindowTop = frame3.top;
                        if (this.mWidth == frame3.width()) {
                        }
                        this.mWidth = frame3.width();
                        this.mHeight = frame3.height();
                        if (this.mSurfaceHolder != null) {
                        }
                        threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                        if (threadedRenderer != null) {
                            threadedRenderer.setup(this.mWidth, this.mHeight, this.mAttachInfo, this.mWindowAttributes.surfaceInsets);
                            this.mNeedsRendererSetup = false;
                        }
                        if (this.mStopped) {
                        }
                        int childWidthMeasureSpec222222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                        int childHeightMeasureSpec22222 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                        if (DEBUG_LAYOUT) {
                        }
                        performMeasure(childWidthMeasureSpec222222, childHeightMeasureSpec22222);
                        int width22222 = host.getMeasuredWidth();
                        int height22222 = host.getMeasuredHeight();
                        measureAgain = false;
                        if (lp.horizontalWeight > 0.0f) {
                        }
                        if (lp.verticalWeight > 0.0f) {
                        }
                        if (measureAgain) {
                        }
                        layoutRequested = true;
                        isViewVisible2 = layoutRequested;
                        relayoutResult2 = relayoutResult4;
                        updatedConfiguration2 = updatedConfiguration4;
                        insetsPending = insetsPending3;
                        if (!surfaceSizeChanged) {
                        }
                        prepareSurfaces();
                        didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                        triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                        if (DEBUG_LAYOUT) {
                        }
                        if (!didLayout) {
                        }
                        if (!surfaceCreated) {
                        }
                        if (triggerGlobalLayoutListener) {
                        }
                        Rect visibleInsets322222 = null;
                        Region touchableRegion222222 = null;
                        int touchableInsetMode222222 = 3;
                        boolean computedInternalInsets222222 = false;
                        if (computesInternalInsets) {
                        }
                        needsSetInsets = computedInternalInsets222222;
                        if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                        }
                        if (this.mFirst) {
                        }
                        if (isViewVisible) {
                        }
                        changedVisibility = false;
                        if (this.mAttachInfo.mHasWindowFocus) {
                        }
                        if (hasWindowFocus) {
                        }
                        if (regainedFocus) {
                        }
                        if (!changedVisibility) {
                        }
                        isToast = this.mWindowAttributes.type == 2005;
                        if (!isToast) {
                        }
                        this.mFirst = false;
                        this.mWillDrawSoon = false;
                        this.mNewSurfaceNeeded = false;
                        this.mActivityRelaunched = false;
                        this.mViewVisibility = viewVisibility;
                        this.mHadWindowFocus = hasWindowFocus;
                        this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                        if ((relayoutResult2 & 1) != 0) {
                        }
                        cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                        if (!cancelAndRedraw) {
                        }
                        if (!isViewVisible) {
                        }
                        if (this.mAttachInfo.mContentCaptureEvents != null) {
                        }
                        this.mIsInTraversal = false;
                        this.mRelayoutRequested = false;
                        if (cancelAndRedraw) {
                        }
                    } else {
                        desiredWindowHeight3 = desiredWindowHeight2;
                        desiredWindowWidth3 = desiredWindowWidth2;
                    }
                }
                windowSizeMayChange = windowSizeMayChange2;
                if (layoutRequested) {
                }
                if (layoutRequested) {
                }
                desiredWindowHeight4 = desiredWindowHeight3;
                desiredWindowWidth4 = desiredWindowWidth3;
                windowShouldResize = false;
                boolean windowShouldResize222 = windowShouldResize | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
                computesInternalInsets = !this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
                int surfaceGenerationId22 = this.mSurface.getGenerationId();
                if (viewVisibility != 0) {
                }
                boolean windowRelayoutWasForced22 = this.mForceNextWindowRelayout;
                surfaceSizeChanged = false;
                surfaceCreated = false;
                boolean surfaceDestroyed22 = false;
                surfaceReplaced = false;
                windowAttributesChanged = this.mWindowAttributesChanged;
                if (windowAttributesChanged) {
                }
                if (params2 == null) {
                }
                insetsPending2 = this.mFirst;
                if (insetsPending2) {
                }
                frame2 = frame;
                if (!DEBUG_LAYOUT) {
                }
                str = "======================================";
                windowAttributesChanged2 = windowAttributesChanged;
                if (Trace.isTagEnabled(8L)) {
                }
                this.mForceNextWindowRelayout = false;
                baseSurfaceHolder = this.mSurfaceHolder;
                if (baseSurfaceHolder != null) {
                }
                boolean hwInitialized322 = false;
                dispatchApplyInsets = false;
                boolean hadSurface22 = this.mSurface.isValid();
                isViewVisible = isViewVisible3;
                if (DEBUG_LAYOUT) {
                }
                if (Trace.isTagEnabled(8L)) {
                }
                if (!this.mFirst) {
                }
                this.mViewFrameInfo.flags |= 1;
                relayoutResult = relayoutWindow(params2, viewVisibility, computesInternalInsets);
                dragResizing = this.mPendingDragResizing;
                i3 = this.mSyncSeqId;
                if (i3 > this.mLastSyncSeqId) {
                }
                boolean surfaceControlChanged22 = (relayoutResult & 2) != 2;
                if (this.mSurfaceControl.isValid()) {
                }
                if (DEBUG_LAYOUT) {
                }
                if (Trace.isTagEnabled(8L)) {
                }
                if (!this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
                }
                surfaceSizeChanged = false;
                if (!this.mLastSurfaceSize.equals(this.mSurfaceSize)) {
                }
                frame3 = this.mPendingAlwaysConsumeSystemBars == this.mAttachInfo.mAlwaysConsumeSystemBars ? 1 : 0;
                updateColorModeIfNeeded(lp.getColorMode());
                surfaceCreated = hadSurface22 && this.mSurface.isValid();
                surfaceDestroyed22 = (hadSurface22 || this.mSurface.isValid()) ? false : true;
                surfaceReplaced = (surfaceGenerationId22 == this.mSurface.getGenerationId() || surfaceControlChanged22) && this.mSurface.isValid();
                if (surfaceReplaced) {
                }
                if (frame3 != null) {
                }
                if (updateCaptionInsets()) {
                }
                if (!dispatchApplyInsets) {
                }
                this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
                dispatchApplyInsets(host);
                dispatchApplyInsets = true;
                if (!surfaceCreated) {
                }
                if (this.mDragResizing != dragResizing) {
                }
                if (!this.mUseMTRenderer) {
                }
                if (Trace.isTagEnabled(8L)) {
                }
                dispatchApplyInsets2 = dispatchApplyInsets;
                relayoutResult3 = relayoutResult;
                updatedConfiguration3 = updatedConfiguration;
                if (!DEBUG_ORIENTATION) {
                }
                Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
                this.mAttachInfo.mWindowLeft = frame3.left;
                this.mAttachInfo.mWindowTop = frame3.top;
                if (this.mWidth == frame3.width()) {
                }
                this.mWidth = frame3.width();
                this.mHeight = frame3.height();
                if (this.mSurfaceHolder != null) {
                }
                threadedRenderer = this.mAttachInfo.mThreadedRenderer;
                if (threadedRenderer != null) {
                }
                if (this.mStopped) {
                }
                int childWidthMeasureSpec2222222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
                int childHeightMeasureSpec222222 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
                if (DEBUG_LAYOUT) {
                }
                performMeasure(childWidthMeasureSpec2222222, childHeightMeasureSpec222222);
                int width222222 = host.getMeasuredWidth();
                int height222222 = host.getMeasuredHeight();
                measureAgain = false;
                if (lp.horizontalWeight > 0.0f) {
                }
                if (lp.verticalWeight > 0.0f) {
                }
                if (measureAgain) {
                }
                layoutRequested = true;
                isViewVisible2 = layoutRequested;
                relayoutResult2 = relayoutResult4;
                updatedConfiguration2 = updatedConfiguration4;
                insetsPending = insetsPending3;
                if (!surfaceSizeChanged) {
                }
                prepareSurfaces();
                didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
                triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
                if (DEBUG_LAYOUT) {
                }
                if (!didLayout) {
                }
                if (!surfaceCreated) {
                }
                if (triggerGlobalLayoutListener) {
                }
                Rect visibleInsets3222222 = null;
                Region touchableRegion2222222 = null;
                int touchableInsetMode2222222 = 3;
                boolean computedInternalInsets2222222 = false;
                if (computesInternalInsets) {
                }
                needsSetInsets = computedInternalInsets2222222;
                if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
                }
                if (this.mFirst) {
                }
                if (isViewVisible) {
                }
                changedVisibility = false;
                if (this.mAttachInfo.mHasWindowFocus) {
                }
                if (hasWindowFocus) {
                }
                if (regainedFocus) {
                }
                if (!changedVisibility) {
                }
                isToast = this.mWindowAttributes.type == 2005;
                if (!isToast) {
                }
                this.mFirst = false;
                this.mWillDrawSoon = false;
                this.mNewSurfaceNeeded = false;
                this.mActivityRelaunched = false;
                this.mViewVisibility = viewVisibility;
                this.mHadWindowFocus = hasWindowFocus;
                this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
                if ((relayoutResult2 & 1) != 0) {
                }
                cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
                if (!cancelAndRedraw) {
                }
                if (!isViewVisible) {
                }
                if (this.mAttachInfo.mContentCaptureEvents != null) {
                }
                this.mIsInTraversal = false;
                this.mRelayoutRequested = false;
                if (cancelAndRedraw) {
                }
            }
        }
        z = false;
        boolean viewUserVisibilityChanged2 = z;
        CompatibilityInfo compatibilityInfo2 = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        supportsScreen = compatibilityInfo2.supportsScreen();
        z2 = this.mLastInCompatMode;
        if (supportsScreen != z2) {
        }
        Rect frame42 = this.mWinFrame;
        if (this.mFirst) {
        }
        if (viewVisibilityChanged) {
        }
        if (this.mAttachInfo.mWindowVisibility != 0) {
        }
        getRunQueue().executeActions(this.mAttachInfo.mHandler);
        if (this.mFirst) {
        }
        layoutRequested = !this.mLayoutRequested && (!this.mStopped || this.mReportNextDraw);
        if (layoutRequested) {
        }
        if (collectViewAttributes()) {
        }
        if (this.mAttachInfo.mForceReportNewAttributes) {
        }
        if (!this.mFirst) {
        }
        this.mAttachInfo.mViewVisibilityChanged = false;
        resizeMode = this.mSoftInputMode & 240;
        if (resizeMode == 0) {
        }
        if (this.mApplyInsetsRequested) {
        }
        windowSizeMayChange = windowSizeMayChange2;
        if (layoutRequested) {
        }
        if (layoutRequested) {
        }
        desiredWindowHeight4 = desiredWindowHeight3;
        desiredWindowWidth4 = desiredWindowWidth3;
        windowShouldResize = false;
        boolean windowShouldResize2222 = windowShouldResize | (!this.mDragResizing && this.mResizeMode == 0) | this.mActivityRelaunched;
        computesInternalInsets = !this.mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || this.mAttachInfo.mHasNonEmptyGivenInternalInsets;
        int surfaceGenerationId222 = this.mSurface.getGenerationId();
        if (viewVisibility != 0) {
        }
        boolean windowRelayoutWasForced222 = this.mForceNextWindowRelayout;
        surfaceSizeChanged = false;
        surfaceCreated = false;
        boolean surfaceDestroyed222 = false;
        surfaceReplaced = false;
        windowAttributesChanged = this.mWindowAttributesChanged;
        if (windowAttributesChanged) {
        }
        if (params2 == null) {
        }
        insetsPending2 = this.mFirst;
        if (insetsPending2) {
        }
        frame2 = frame;
        if (!DEBUG_LAYOUT) {
        }
        str = "======================================";
        windowAttributesChanged2 = windowAttributesChanged;
        if (Trace.isTagEnabled(8L)) {
        }
        this.mForceNextWindowRelayout = false;
        baseSurfaceHolder = this.mSurfaceHolder;
        if (baseSurfaceHolder != null) {
        }
        boolean hwInitialized3222 = false;
        dispatchApplyInsets = false;
        boolean hadSurface222 = this.mSurface.isValid();
        isViewVisible = isViewVisible3;
        if (DEBUG_LAYOUT) {
        }
        if (Trace.isTagEnabled(8L)) {
        }
        if (!this.mFirst) {
        }
        this.mViewFrameInfo.flags |= 1;
        relayoutResult = relayoutWindow(params2, viewVisibility, computesInternalInsets);
        dragResizing = this.mPendingDragResizing;
        i3 = this.mSyncSeqId;
        if (i3 > this.mLastSyncSeqId) {
        }
        boolean surfaceControlChanged222 = (relayoutResult & 2) != 2;
        if (this.mSurfaceControl.isValid()) {
        }
        if (DEBUG_LAYOUT) {
        }
        if (Trace.isTagEnabled(8L)) {
        }
        if (!this.mPendingMergedConfiguration.equals(this.mLastReportedMergedConfiguration)) {
        }
        surfaceSizeChanged = false;
        if (!this.mLastSurfaceSize.equals(this.mSurfaceSize)) {
        }
        frame3 = this.mPendingAlwaysConsumeSystemBars == this.mAttachInfo.mAlwaysConsumeSystemBars ? 1 : 0;
        updateColorModeIfNeeded(lp.getColorMode());
        surfaceCreated = hadSurface222 && this.mSurface.isValid();
        surfaceDestroyed222 = (hadSurface222 || this.mSurface.isValid()) ? false : true;
        surfaceReplaced = (surfaceGenerationId222 == this.mSurface.getGenerationId() || surfaceControlChanged222) && this.mSurface.isValid();
        if (surfaceReplaced) {
        }
        if (frame3 != null) {
        }
        if (updateCaptionInsets()) {
        }
        if (!dispatchApplyInsets) {
        }
        this.mLastSystemUiVisibility = this.mAttachInfo.mSystemUiVisibility;
        dispatchApplyInsets(host);
        dispatchApplyInsets = true;
        if (!surfaceCreated) {
        }
        if (this.mDragResizing != dragResizing) {
        }
        if (!this.mUseMTRenderer) {
        }
        if (Trace.isTagEnabled(8L)) {
        }
        dispatchApplyInsets2 = dispatchApplyInsets;
        relayoutResult3 = relayoutResult;
        updatedConfiguration3 = updatedConfiguration;
        if (!DEBUG_ORIENTATION) {
        }
        Log.v(this.mTag, "Relayout returned: frame=" + frame3 + ", surface=" + this.mSurface);
        this.mAttachInfo.mWindowLeft = frame3.left;
        this.mAttachInfo.mWindowTop = frame3.top;
        if (this.mWidth == frame3.width()) {
        }
        this.mWidth = frame3.width();
        this.mHeight = frame3.height();
        if (this.mSurfaceHolder != null) {
        }
        threadedRenderer = this.mAttachInfo.mThreadedRenderer;
        if (threadedRenderer != null) {
        }
        if (this.mStopped) {
        }
        int childWidthMeasureSpec22222222 = getRootMeasureSpec(this.mWidth, lp.width, lp.privateFlags);
        int childHeightMeasureSpec2222222 = getRootMeasureSpec(this.mHeight, lp.height, lp.privateFlags);
        if (DEBUG_LAYOUT) {
        }
        performMeasure(childWidthMeasureSpec22222222, childHeightMeasureSpec2222222);
        int width2222222 = host.getMeasuredWidth();
        int height2222222 = host.getMeasuredHeight();
        measureAgain = false;
        if (lp.horizontalWeight > 0.0f) {
        }
        if (lp.verticalWeight > 0.0f) {
        }
        if (measureAgain) {
        }
        layoutRequested = true;
        isViewVisible2 = layoutRequested;
        relayoutResult2 = relayoutResult4;
        updatedConfiguration2 = updatedConfiguration4;
        insetsPending = insetsPending3;
        if (!surfaceSizeChanged) {
        }
        prepareSurfaces();
        didLayout = !isViewVisible2 && (!this.mStopped || this.mReportNextDraw);
        triggerGlobalLayoutListener = !didLayout || this.mAttachInfo.mRecomputeGlobalAttributes;
        if (DEBUG_LAYOUT) {
        }
        if (!didLayout) {
        }
        if (!surfaceCreated) {
        }
        if (triggerGlobalLayoutListener) {
        }
        Rect visibleInsets32222222 = null;
        Region touchableRegion22222222 = null;
        int touchableInsetMode22222222 = 3;
        boolean computedInternalInsets22222222 = false;
        if (computesInternalInsets) {
        }
        needsSetInsets = computedInternalInsets22222222;
        if (!((Objects.equals(this.mPreviousTouchableRegion, this.mTouchableRegion) && this.mTouchableRegion != null) | needsSetInsets)) {
        }
        if (this.mFirst) {
        }
        if (isViewVisible) {
        }
        changedVisibility = false;
        if (this.mAttachInfo.mHasWindowFocus) {
        }
        if (hasWindowFocus) {
        }
        if (regainedFocus) {
        }
        if (!changedVisibility) {
        }
        isToast = this.mWindowAttributes.type == 2005;
        if (!isToast) {
        }
        this.mFirst = false;
        this.mWillDrawSoon = false;
        this.mNewSurfaceNeeded = false;
        this.mActivityRelaunched = false;
        this.mViewVisibility = viewVisibility;
        this.mHadWindowFocus = hasWindowFocus;
        this.mImeFocusController.onTraversal(hasWindowFocus, this.mWindowAttributes);
        if ((relayoutResult2 & 1) != 0) {
        }
        cancelAndRedraw = this.mAttachInfo.mTreeObserver.dispatchOnPreDraw();
        if (!cancelAndRedraw) {
        }
        if (!isViewVisible) {
        }
        if (this.mAttachInfo.mContentCaptureEvents != null) {
        }
        this.mIsInTraversal = false;
        this.mRelayoutRequested = false;
        if (cancelAndRedraw) {
        }
    }

    private void createSyncIfNeeded() {
        if (isInLocalSync() || !this.mReportNextDraw) {
            return;
        }
        final int seqId = this.mSyncSeqId;
        this.mSyncId = this.mSurfaceSyncer.setupSync(new Consumer() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ViewRootImpl.this.m5130lambda$createSyncIfNeeded$4$androidviewViewRootImpl(seqId, (SurfaceControl.Transaction) obj);
            }
        });
        if (DEBUG_BLAST) {
            Log.d(this.mTag, "Setup new sync id=" + this.mSyncId);
        }
        this.mSurfaceSyncer.addToSync(this.mSyncId, this.mSyncTarget);
        notifySurfaceSyncStarted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createSyncIfNeeded$4$android-view-ViewRootImpl  reason: not valid java name */
    public /* synthetic */ void m5130lambda$createSyncIfNeeded$4$androidviewViewRootImpl(final int seqId, final SurfaceControl.Transaction transaction) {
        this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ViewRootImpl.this.m5129lambda$createSyncIfNeeded$3$androidviewViewRootImpl(transaction, seqId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createSyncIfNeeded$3$android-view-ViewRootImpl  reason: not valid java name */
    public /* synthetic */ void m5129lambda$createSyncIfNeeded$3$androidviewViewRootImpl(SurfaceControl.Transaction transaction, int seqId) {
        this.mSurfaceChangedTransaction.merge(transaction);
        reportDrawFinished(seqId);
    }

    private void notifyContentCatpureEvents() {
        Trace.traceBegin(8L, "notifyContentCaptureEvents");
        try {
            MainContentCaptureSession mainSession = this.mAttachInfo.mContentCaptureManager.getMainContentCaptureSession();
            for (int i = 0; i < this.mAttachInfo.mContentCaptureEvents.size(); i++) {
                int sessionId = this.mAttachInfo.mContentCaptureEvents.keyAt(i);
                mainSession.notifyViewTreeEvent(sessionId, true);
                ArrayList<Object> events = this.mAttachInfo.mContentCaptureEvents.valueAt(i);
                for (int j = 0; j < events.size(); j++) {
                    Object event = events.get(j);
                    if (event instanceof AutofillId) {
                        mainSession.notifyViewDisappeared(sessionId, (AutofillId) event);
                    } else if (event instanceof View) {
                        View view = (View) event;
                        ContentCaptureSession session = view.getContentCaptureSession();
                        if (session == null) {
                            Log.w(this.mTag, "no content capture session on view: " + view);
                        } else {
                            int actualId = session.getId();
                            if (actualId != sessionId) {
                                Log.w(this.mTag, "content capture session mismatch for view (" + view + "): was " + sessionId + " before, it's " + actualId + " now");
                            } else {
                                ViewStructure structure = session.newViewStructure(view);
                                view.onProvideContentCaptureStructure(structure, 0);
                                session.notifyViewAppeared(structure);
                            }
                        }
                    } else if (event instanceof Insets) {
                        mainSession.notifyViewInsetsChanged(sessionId, (Insets) event);
                    } else {
                        Log.w(this.mTag, "invalid content capture event: " + event);
                    }
                }
                mainSession.notifyViewTreeEvent(sessionId, false);
            }
            this.mAttachInfo.mContentCaptureEvents = null;
            Trace.traceEnd(8L);
            this.mIsInTraversal = false;
        } catch (Throwable th) {
            Trace.traceEnd(8L);
            throw th;
        }
    }

    private void notifyHolderSurfaceDestroyed() {
        this.mSurfaceHolder.ungetCallbacks();
        SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
        if (callbacks != null) {
            for (SurfaceHolder.Callback c : callbacks) {
                c.surfaceDestroyed(this.mSurfaceHolder);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeHandleWindowMove(Rect frame) {
        boolean windowMoved = (this.mAttachInfo.mWindowLeft == frame.left && this.mAttachInfo.mWindowTop == frame.top) ? false : true;
        if (windowMoved) {
            this.mAttachInfo.mWindowLeft = frame.left;
            this.mAttachInfo.mWindowTop = frame.top;
        }
        if (windowMoved || this.mAttachInfo.mNeedsUpdateLightCenter) {
            if (this.mAttachInfo.mThreadedRenderer != null) {
                this.mAttachInfo.mThreadedRenderer.setLightCenter(this.mAttachInfo);
            }
            this.mAttachInfo.mNeedsUpdateLightCenter = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWindowFocusChanged() {
        boolean z;
        synchronized (this) {
            if (this.mWindowFocusChanged) {
                this.mWindowFocusChanged = false;
                boolean hasWindowFocus = this.mUpcomingWindowFocus;
                if (hasWindowFocus) {
                    InsetsController insetsController = this.mInsetsController;
                    if (getFocusedViewOrNull() == null) {
                        z = false;
                    } else {
                        z = true;
                    }
                    insetsController.onWindowFocusGained(z);
                } else {
                    this.mInsetsController.onWindowFocusLost();
                }
                if (this.mAdded) {
                    profileRendering(hasWindowFocus);
                    if (hasWindowFocus && this.mAttachInfo.mThreadedRenderer != null && this.mSurface.isValid()) {
                        this.mFullRedrawNeeded = true;
                        try {
                            Rect surfaceInsets = this.mWindowAttributes.surfaceInsets;
                            this.mAttachInfo.mThreadedRenderer.initializeIfNeeded(this.mWidth, this.mHeight, this.mAttachInfo, this.mSurface, surfaceInsets);
                        } catch (Surface.OutOfResourcesException e) {
                            Log.e(this.mTag, "OutOfResourcesException locking surface", e);
                            try {
                                if (!this.mWindowSession.outOfMemory(this.mWindow)) {
                                    Slog.w(this.mTag, "No processes killed for memory; killing self");
                                    Process.killProcess(Process.myPid());
                                }
                            } catch (RemoteException e2) {
                            }
                            ViewRootHandler viewRootHandler = this.mHandler;
                            viewRootHandler.sendMessageDelayed(viewRootHandler.obtainMessage(6), 500L);
                            return;
                        }
                    }
                    this.mAttachInfo.mHasWindowFocus = hasWindowFocus;
                    this.mImeFocusController.updateImeFocusable(this.mWindowAttributes, true);
                    this.mImeFocusController.onPreWindowFocus(hasWindowFocus, this.mWindowAttributes);
                    if (this.mView != null) {
                        this.mAttachInfo.mKeyDispatchState.reset();
                        this.mView.dispatchWindowFocusChanged(hasWindowFocus);
                        this.mAttachInfo.mTreeObserver.dispatchOnWindowFocusChange(hasWindowFocus);
                        if (this.mAttachInfo.mTooltipHost != null) {
                            this.mAttachInfo.mTooltipHost.hideTooltip();
                        }
                    }
                    this.mImeFocusController.onPostWindowFocus(getFocusedViewOrNull(), hasWindowFocus, this.mWindowAttributes);
                    if (hasWindowFocus) {
                        this.mWindowAttributes.softInputMode &= -257;
                        ((WindowManager.LayoutParams) this.mView.getLayoutParams()).softInputMode &= -257;
                        fireAccessibilityFocusEventIfHasFocusedNode();
                    } else if (this.mPointerCapture) {
                        handlePointerCaptureChanged(false);
                    }
                }
                this.mFirstInputStage.onWindowFocusChanged(hasWindowFocus);
                if (hasWindowFocus) {
                    handleContentCaptureFlush();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWindowTouchModeChanged() {
        boolean inTouchMode;
        synchronized (this) {
            inTouchMode = this.mUpcomingInTouchMode;
        }
        ensureTouchModeLocally(inTouchMode);
    }

    private void fireAccessibilityFocusEventIfHasFocusedNode() {
        View focusedView;
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled() || (focusedView = this.mView.findFocus()) == null) {
            return;
        }
        AccessibilityNodeProvider provider = focusedView.getAccessibilityNodeProvider();
        if (provider == null) {
            focusedView.sendAccessibilityEvent(8);
            return;
        }
        AccessibilityNodeInfo focusedNode = findFocusedVirtualNode(provider);
        if (focusedNode != null) {
            int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(focusedNode.getSourceNodeId());
            AccessibilityEvent event = AccessibilityEvent.obtain(8);
            event.setSource(focusedView, virtualId);
            event.setPackageName(focusedNode.getPackageName());
            event.setChecked(focusedNode.isChecked());
            event.setContentDescription(focusedNode.getContentDescription());
            event.setPassword(focusedNode.isPassword());
            event.getText().add(focusedNode.getText());
            event.setEnabled(focusedNode.isEnabled());
            focusedView.getParent().requestSendAccessibilityEvent(focusedView, event);
            focusedNode.recycle();
        }
    }

    private AccessibilityNodeInfo findFocusedVirtualNode(AccessibilityNodeProvider provider) {
        AccessibilityNodeInfo focusedNode = provider.findFocus(1);
        if (focusedNode != null) {
            return focusedNode;
        }
        if (this.mContext.isAutofillCompatibilityEnabled()) {
            AccessibilityNodeInfo current = provider.createAccessibilityNodeInfo(-1);
            if (current.isFocused()) {
                return current;
            }
            Queue<AccessibilityNodeInfo> fringe = new ArrayDeque<>();
            fringe.offer(current);
            while (!fringe.isEmpty()) {
                AccessibilityNodeInfo current2 = fringe.poll();
                LongArray childNodeIds = current2.getChildNodeIds();
                if (childNodeIds != null && childNodeIds.size() > 0) {
                    int childCount = childNodeIds.size();
                    for (int i = 0; i < childCount; i++) {
                        int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(childNodeIds.get(i));
                        AccessibilityNodeInfo child = provider.createAccessibilityNodeInfo(virtualId);
                        if (child != null) {
                            if (child.isFocused()) {
                                return child;
                            }
                            fringe.offer(child);
                        }
                    }
                    current2.recycle();
                }
            }
            return null;
        }
        return null;
    }

    private void handleOutOfResourcesException(Surface.OutOfResourcesException e) {
        Log.e(this.mTag, "OutOfResourcesException initializing HW surface", e);
        try {
            if (!this.mWindowSession.outOfMemory(this.mWindow) && Process.myUid() != 1000) {
                Slog.w(this.mTag, "No processes killed for memory; killing self");
                Process.killProcess(Process.myPid());
            }
        } catch (RemoteException e2) {
        }
        this.mLayoutRequested = true;
    }

    private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        if (this.mView == null) {
            return;
        }
        Trace.traceBegin(8L, "measure");
        try {
            this.mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } finally {
            Trace.traceEnd(8L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInLayout() {
        return this.mInLayout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean requestLayoutDuringLayout(View view) {
        if (view.mParent == null || view.mAttachInfo == null) {
            return true;
        }
        if (!this.mLayoutRequesters.contains(view)) {
            this.mLayoutRequesters.add(view);
        }
        return !this.mHandlingLayoutInLayoutRequest;
    }

    private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth, int desiredWindowHeight) {
        ArrayList<View> validLayoutRequesters;
        this.mScrollMayChange = true;
        this.mInLayout = true;
        View host = this.mView;
        if (host == null) {
            return;
        }
        if (DEBUG_ORIENTATION || DEBUG_LAYOUT) {
            Log.v(this.mTag, "Laying out " + host + " in " + this + " to (" + host.getMeasuredWidth() + ", " + host.getMeasuredHeight() + NavigationBarInflaterView.KEY_CODE_END);
        }
        Trace.traceBegin(8L, TtmlUtils.TAG_LAYOUT);
        try {
            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
            this.mInLayout = false;
            int numViewsRequestingLayout = this.mLayoutRequesters.size();
            if (numViewsRequestingLayout > 0 && (validLayoutRequesters = getValidLayoutRequesters(this.mLayoutRequesters, false)) != null) {
                this.mHandlingLayoutInLayoutRequest = true;
                int numValidRequests = validLayoutRequesters.size();
                for (int i = 0; i < numValidRequests; i++) {
                    View view = validLayoutRequesters.get(i);
                    Log.w("View", "requestLayout() improperly called by " + view + " during layout: running second layout pass");
                    view.requestLayout();
                }
                measureHierarchy(host, lp, this.mView.getContext().getResources(), desiredWindowWidth, desiredWindowHeight);
                this.mInLayout = true;
                host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
                this.mHandlingLayoutInLayoutRequest = false;
                final ArrayList<View> validLayoutRequesters2 = getValidLayoutRequesters(this.mLayoutRequesters, true);
                if (validLayoutRequesters2 != null) {
                    getRunQueue().post(new Runnable() { // from class: android.view.ViewRootImpl.4
                        @Override // java.lang.Runnable
                        public void run() {
                            int numValidRequests2 = validLayoutRequesters2.size();
                            for (int i2 = 0; i2 < numValidRequests2; i2++) {
                                View view2 = (View) validLayoutRequesters2.get(i2);
                                Log.w("View", "requestLayout() improperly called by " + view2 + " during second layout pass: posting in next frame");
                                view2.requestLayout();
                            }
                        }
                    });
                }
            }
            Trace.traceEnd(8L);
            this.mInLayout = false;
        } catch (Throwable th) {
            Trace.traceEnd(8L);
            throw th;
        }
    }

    private ArrayList<View> getValidLayoutRequesters(ArrayList<View> layoutRequesters, boolean secondLayoutRequests) {
        int numViewsRequestingLayout = layoutRequesters.size();
        ArrayList<View> validLayoutRequesters = null;
        for (int i = 0; i < numViewsRequestingLayout; i++) {
            View view = layoutRequesters.get(i);
            if (view != null && view.mAttachInfo != null && view.mParent != null && (secondLayoutRequests || (view.mPrivateFlags & 4096) == 4096)) {
                boolean gone = false;
                View parent = view;
                while (true) {
                    if (parent == null) {
                        break;
                    } else if ((parent.mViewFlags & 12) == 8) {
                        gone = true;
                        break;
                    } else if (parent.mParent instanceof View) {
                        parent = (View) parent.mParent;
                    } else {
                        parent = null;
                    }
                }
                if (!gone) {
                    if (validLayoutRequesters == null) {
                        validLayoutRequesters = new ArrayList<>();
                    }
                    validLayoutRequesters.add(view);
                }
            }
        }
        if (!secondLayoutRequests) {
            for (int i2 = 0; i2 < numViewsRequestingLayout; i2++) {
                View view2 = layoutRequesters.get(i2);
                while (view2 != null && (view2.mPrivateFlags & 4096) != 0) {
                    view2.mPrivateFlags &= -4097;
                    if (view2.mParent instanceof View) {
                        view2 = (View) view2.mParent;
                    } else {
                        view2 = null;
                    }
                }
            }
        }
        layoutRequesters.clear();
        return validLayoutRequesters;
    }

    @Override // android.view.ViewParent
    public void requestTransparentRegion(View child) {
        checkThread();
        View view = this.mView;
        if (view != child) {
            return;
        }
        if ((view.mPrivateFlags & 512) == 0) {
            this.mView.mPrivateFlags |= 512;
            this.mWindowAttributesChanged = true;
        }
        requestLayout();
    }

    private static int getRootMeasureSpec(int windowSize, int measurement, int privateFlags) {
        int rootDimension = (privateFlags & 8192) != 0 ? -1 : measurement;
        switch (rootDimension) {
            case -2:
                int measureSpec = View.MeasureSpec.makeMeasureSpec(windowSize, Integer.MIN_VALUE);
                return measureSpec;
            case -1:
                int measureSpec2 = View.MeasureSpec.makeMeasureSpec(windowSize, 1073741824);
                return measureSpec2;
            default:
                int measureSpec3 = View.MeasureSpec.makeMeasureSpec(rootDimension, 1073741824);
                return measureSpec3;
        }
    }

    @Override // android.view.ThreadedRenderer.DrawCallbacks
    public void onPreDraw(RecordingCanvas canvas) {
        if (this.mCurScrollY != 0 && this.mHardwareYOffset != 0 && this.mAttachInfo.mThreadedRenderer.isOpaque()) {
            canvas.drawColor(-16777216);
        }
        canvas.translate(-this.mHardwareXOffset, -this.mHardwareYOffset);
    }

    @Override // android.view.ThreadedRenderer.DrawCallbacks
    public void onPostDraw(RecordingCanvas canvas) {
        drawAccessibilityFocusedDrawableIfNeeded(canvas);
        if (this.mUseMTRenderer) {
            for (int i = this.mWindowCallbacks.size() - 1; i >= 0; i--) {
                this.mWindowCallbacks.get(i).onPostDraw(canvas);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void outputDisplayList(View view) {
        view.mRenderNode.output();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void profileRendering(boolean enabled) {
        if (this.mProfileRendering) {
            this.mRenderProfilingEnabled = enabled;
            Choreographer.FrameCallback frameCallback = this.mRenderProfiler;
            if (frameCallback != null) {
                this.mChoreographer.removeFrameCallback(frameCallback);
            }
            if (this.mRenderProfilingEnabled) {
                if (this.mRenderProfiler == null) {
                    this.mRenderProfiler = new Choreographer.FrameCallback() { // from class: android.view.ViewRootImpl.5
                        @Override // android.view.Choreographer.FrameCallback
                        public void doFrame(long frameTimeNanos) {
                            ViewRootImpl.this.mDirty.set(0, 0, ViewRootImpl.this.mWidth, ViewRootImpl.this.mHeight);
                            ViewRootImpl.this.scheduleTraversals();
                            if (ViewRootImpl.this.mRenderProfilingEnabled) {
                                ViewRootImpl.this.mChoreographer.postFrameCallback(ViewRootImpl.this.mRenderProfiler);
                            }
                        }
                    };
                }
                this.mChoreographer.postFrameCallback(this.mRenderProfiler);
                return;
            }
            this.mRenderProfiler = null;
        }
    }

    private void trackFPS() {
        long nowTime = System.currentTimeMillis();
        if (this.mFpsStartTime < 0) {
            this.mFpsPrevTime = nowTime;
            this.mFpsStartTime = nowTime;
            this.mFpsNumFrames = 0;
            return;
        }
        this.mFpsNumFrames++;
        String thisHash = Integer.toHexString(System.identityHashCode(this));
        long frameTime = nowTime - this.mFpsPrevTime;
        long totalTime = nowTime - this.mFpsStartTime;
        Log.v(this.mTag, "0x" + thisHash + "\tFrame time:\t" + frameTime);
        this.mFpsPrevTime = nowTime;
        if (totalTime > 1000) {
            float fps = (this.mFpsNumFrames * 1000.0f) / ((float) totalTime);
            Log.v(this.mTag, "0x" + thisHash + "\tFPS:\t" + fps);
            this.mFpsStartTime = nowTime;
            this.mFpsNumFrames = 0;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4468=4] */
    private void reportDrawFinished(int seqId) {
        String str;
        StringBuilder sb;
        if (DEBUG_BLAST) {
            Log.d(this.mTag, "reportDrawFinished " + Debug.getCallers(5));
        }
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "finish draw");
        }
        this.mTranFoldInnerCustody.notifySFPhysicalId();
        try {
            try {
                this.mWindowSession.finishDrawing(this.mWindow, this.mSurfaceChangedTransaction, seqId);
                if (this.mFrameScenario.canInitRenderThreadId()) {
                    if (isHardwareEnabled()) {
                        this.mFrameScenario.setThreadedRenderer(this.mAttachInfo.mThreadedRenderer);
                    }
                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario);
                }
                this.mSurfaceChangedTransaction.clear();
            } catch (RemoteException e) {
                Log.e(this.mTag, "Unable to report draw finished", e);
                this.mSurfaceChangedTransaction.apply();
                this.mSurfaceChangedTransaction.clear();
                if (ViewDebugManager.DEBUG_DRAW) {
                    str = this.mTag;
                    sb = new StringBuilder();
                }
            }
            if (ViewDebugManager.DEBUG_DRAW) {
                str = this.mTag;
                sb = new StringBuilder();
                Log.d(str, sb.append("Exception when finish draw window ").append(this.mWindow).toString());
            }
            if (Trace.isTagEnabled(8L)) {
                Trace.traceEnd(8L);
            }
        } catch (Throwable th) {
            this.mSurfaceChangedTransaction.clear();
            if (ViewDebugManager.DEBUG_DRAW) {
                Log.d(this.mTag, "Exception when finish draw window " + this.mWindow);
            }
            throw th;
        }
    }

    public boolean isHardwareEnabled() {
        return this.mAttachInfo.mThreadedRenderer != null && this.mAttachInfo.mThreadedRenderer.isEnabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addToSync(SurfaceSyncer.SyncTarget syncable) {
        if (!isInLocalSync()) {
            return;
        }
        this.mSurfaceSyncer.addToSync(this.mSyncId, syncable);
    }

    public boolean isInLocalSync() {
        return this.mSyncId != -1;
    }

    private void addFrameCommitCallbackIfNeeded() {
        if (!isHardwareEnabled()) {
            return;
        }
        final ArrayList<Runnable> commitCallbacks = this.mAttachInfo.mTreeObserver.captureFrameCommitCallbacks();
        boolean needFrameCommitCallback = commitCallbacks != null && commitCallbacks.size() > 0;
        if (!needFrameCommitCallback) {
            return;
        }
        if (DEBUG_DRAW) {
            Log.d(this.mTag, "Creating frameCommitCallback commitCallbacks size=" + commitCallbacks.size());
        }
        this.mAttachInfo.mThreadedRenderer.setFrameCommitCallback(new HardwareRenderer.FrameCommitCallback() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda7
            @Override // android.graphics.HardwareRenderer.FrameCommitCallback
            public final void onFrameCommit(boolean z) {
                ViewRootImpl.this.m5127xcb6bff16(commitCallbacks, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addFrameCommitCallbackIfNeeded$6$android-view-ViewRootImpl  reason: not valid java name */
    public /* synthetic */ void m5127xcb6bff16(final ArrayList commitCallbacks, boolean didProduceBuffer) {
        if (DEBUG_DRAW) {
            Log.d(this.mTag, "Received frameCommitCallback didProduceBuffer=" + didProduceBuffer);
        }
        this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ViewRootImpl.lambda$addFrameCommitCallbackIfNeeded$5(commitCallbacks);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addFrameCommitCallbackIfNeeded$5(ArrayList commitCallbacks) {
        for (int i = 0; i < commitCallbacks.size(); i++) {
            ((Runnable) commitCallbacks.get(i)).run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.view.ViewRootImpl$6  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass6 implements HardwareRenderer.FrameDrawingCallback {
        AnonymousClass6() {
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public HardwareRenderer.FrameCommitCallback onFrameDraw(int syncResult, final long frame) {
            if ((syncResult & 6) != 0) {
                ViewRootImpl.this.mBlastBufferQueue.applyPendingTransactions(frame);
                return null;
            }
            return new HardwareRenderer.FrameCommitCallback() { // from class: android.view.ViewRootImpl$6$$ExternalSyntheticLambda0
                @Override // android.graphics.HardwareRenderer.FrameCommitCallback
                public final void onFrameCommit(boolean z) {
                    ViewRootImpl.AnonymousClass6.this.m5135lambda$onFrameDraw$0$androidviewViewRootImpl$6(frame, z);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFrameDraw$0$android-view-ViewRootImpl$6  reason: not valid java name */
        public /* synthetic */ void m5135lambda$onFrameDraw$0$androidviewViewRootImpl$6(long frame, boolean didProduceBuffer) {
            if (!didProduceBuffer) {
                ViewRootImpl.this.mBlastBufferQueue.applyPendingTransactions(frame);
            }
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public void onFrameDraw(long frame) {
        }
    }

    private void registerCallbackForPendingTransactions() {
        registerRtFrameCallback(new AnonymousClass6());
    }

    /* JADX WARN: Removed duplicated region for block: B:43:0x0075  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x009b  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0125  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean performDraw() {
        boolean z;
        SurfaceSyncer.SyncBufferCallback syncBufferCallback;
        if ((this.mAttachInfo.mDisplayState != 1 || this.mReportNextDraw) && this.mView != null) {
            boolean fullRedrawNeeded = this.mFullRedrawNeeded || this.mSyncBufferCallback != null;
            this.mFullRedrawNeeded = false;
            this.mIsDrawing = true;
            Trace.traceBegin(8L, "draw");
            addFrameCommitCallbackIfNeeded();
            boolean usingAsyncReport = isHardwareEnabled() && this.mSyncBufferCallback != null;
            if (usingAsyncReport) {
                registerCallbacksForSync(this.mSyncBuffer, this.mSyncBufferCallback);
            } else if (this.mHasPendingTransactions) {
                registerCallbackForPendingTransactions();
            }
            this.mHasPendingTransactions = false;
            if (usingAsyncReport) {
                try {
                    if (this.mSyncBuffer) {
                        z = true;
                        boolean canUseAsync = draw(fullRedrawNeeded, z);
                        if (usingAsyncReport && !canUseAsync) {
                            this.mAttachInfo.mThreadedRenderer.setFrameCallback(null);
                            usingAsyncReport = false;
                        }
                        this.mIsDrawing = false;
                        Trace.traceEnd(8L);
                        if (this.mAttachInfo.mPendingAnimatingRenderNodes != null) {
                            int count = this.mAttachInfo.mPendingAnimatingRenderNodes.size();
                            for (int i = 0; i < count; i++) {
                                this.mAttachInfo.mPendingAnimatingRenderNodes.get(i).endAllAnimators();
                            }
                            this.mAttachInfo.mPendingAnimatingRenderNodes.clear();
                        }
                        if (this.mReportNextDraw) {
                            CountDownLatch countDownLatch = this.mWindowDrawCountDown;
                            if (countDownLatch != null) {
                                try {
                                    countDownLatch.await();
                                } catch (InterruptedException e) {
                                    Log.e(this.mTag, "Window redraw count down interrupted!");
                                }
                                this.mWindowDrawCountDown = null;
                            }
                            if (this.mAttachInfo.mThreadedRenderer != null) {
                                this.mAttachInfo.mThreadedRenderer.setStopped(this.mStopped);
                            }
                            if (LOCAL_LOGV || DEBUG_DRAW) {
                                Log.v(this.mTag, "FINISHED DRAWING: " + ((Object) this.mWindowAttributes.getTitle()));
                            }
                            if (this.mSurfaceHolder != null && this.mSurface.isValid()) {
                                final SurfaceSyncer.SyncBufferCallback syncBufferCallback2 = this.mSyncBufferCallback;
                                SurfaceCallbackHelper sch = new SurfaceCallbackHelper(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda13
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        ViewRootImpl.this.m5133lambda$performDraw$8$androidviewViewRootImpl(syncBufferCallback2);
                                    }
                                });
                                this.mSyncBufferCallback = null;
                                SurfaceHolder.Callback[] callbacks = this.mSurfaceHolder.getCallbacks();
                                sch.dispatchSurfaceRedrawNeededAsync(this.mSurfaceHolder, callbacks);
                            } else if (!usingAsyncReport && this.mAttachInfo.mThreadedRenderer != null) {
                                this.mAttachInfo.mThreadedRenderer.fence();
                            }
                        }
                        syncBufferCallback = this.mSyncBufferCallback;
                        if (syncBufferCallback != null && !usingAsyncReport) {
                            syncBufferCallback.onBufferReady(null);
                        }
                        if (this.mPerformContentCapture) {
                            performContentCaptureInitialReport();
                        }
                        return true;
                    }
                } catch (Throwable th) {
                    this.mIsDrawing = false;
                    Trace.traceEnd(8L);
                    throw th;
                }
            }
            z = false;
            boolean canUseAsync2 = draw(fullRedrawNeeded, z);
            if (usingAsyncReport) {
                this.mAttachInfo.mThreadedRenderer.setFrameCallback(null);
                usingAsyncReport = false;
            }
            this.mIsDrawing = false;
            Trace.traceEnd(8L);
            if (this.mAttachInfo.mPendingAnimatingRenderNodes != null) {
            }
            if (this.mReportNextDraw) {
            }
            syncBufferCallback = this.mSyncBufferCallback;
            if (syncBufferCallback != null) {
                syncBufferCallback.onBufferReady(null);
            }
            if (this.mPerformContentCapture) {
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performDraw$8$android-view-ViewRootImpl  reason: not valid java name */
    public /* synthetic */ void m5133lambda$performDraw$8$androidviewViewRootImpl(final SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        this.mHandler.post(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceSyncer.SyncBufferCallback.this.onBufferReady(null);
            }
        });
    }

    private boolean isContentCaptureEnabled() {
        switch (this.mContentCaptureEnabled) {
            case 0:
                boolean reallyEnabled = isContentCaptureReallyEnabled();
                this.mContentCaptureEnabled = reallyEnabled ? 1 : 2;
                return reallyEnabled;
            case 1:
                return true;
            case 2:
                return false;
            default:
                Log.w(TAG, "isContentCaptureEnabled(): invalid state " + this.mContentCaptureEnabled);
                return false;
        }
    }

    private boolean isContentCaptureReallyEnabled() {
        ContentCaptureManager ccm;
        return (this.mContext.getContentCaptureOptions() == null || (ccm = this.mAttachInfo.getContentCaptureManager(this.mContext)) == null || !ccm.isContentCaptureEnabled()) ? false : true;
    }

    private void performContentCaptureInitialReport() {
        this.mPerformContentCapture = false;
        View rootView = this.mView;
        if (DEBUG_CONTENT_CAPTURE) {
            Log.v(this.mTag, "performContentCaptureInitialReport() on " + rootView);
        }
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "dispatchContentCapture() for " + getClass().getSimpleName());
        }
        try {
            if (!isContentCaptureEnabled()) {
                return;
            }
            if (this.mAttachInfo.mContentCaptureManager != null) {
                MainContentCaptureSession session = this.mAttachInfo.mContentCaptureManager.getMainContentCaptureSession();
                session.notifyWindowBoundsChanged(session.getId(), getConfiguration().windowConfiguration.getBounds());
            }
            rootView.dispatchInitialProvideContentCaptureStructure();
        } finally {
            Trace.traceEnd(8L);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4729=4] */
    private void handleContentCaptureFlush() {
        if (DEBUG_CONTENT_CAPTURE) {
            Log.v(this.mTag, "handleContentCaptureFlush()");
        }
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "flushContentCapture for " + getClass().getSimpleName());
        }
        try {
            if (isContentCaptureEnabled()) {
                ContentCaptureManager ccm = this.mAttachInfo.mContentCaptureManager;
                if (ccm == null) {
                    Log.w(TAG, "No ContentCapture on AttachInfo");
                } else {
                    ccm.flush(2);
                }
            }
        } finally {
            Trace.traceEnd(8L);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:103:0x0281  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x0326  */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0214  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean draw(boolean fullRedrawNeeded, boolean forceDraw) {
        int curScrollY;
        boolean fullRedrawNeeded2;
        int xOffset;
        boolean accessibilityFocusDirty;
        int xOffset2;
        Drawable drawable;
        Scroller scroller;
        Surface surface = this.mSurface;
        if (surface.isValid()) {
            if (DEBUG_FPS) {
                trackFPS();
            }
            if (!sFirstDrawComplete) {
                ArrayList<Runnable> arrayList = sFirstDrawHandlers;
                synchronized (arrayList) {
                    sFirstDrawComplete = true;
                    int count = arrayList.size();
                    for (int i = 0; i < count; i++) {
                        this.mHandler.post(sFirstDrawHandlers.get(i));
                    }
                }
            }
            scrollToRectOrFocus(null, false);
            if (this.mAttachInfo.mViewScrollChanged) {
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(new ScrollScenario(1, 1, 0, this.mContext));
                this.mAttachInfo.mViewScrollChanged = false;
                this.mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
            }
            Scroller scroller2 = this.mScroller;
            boolean animating = scroller2 != null && scroller2.computeScrollOffset();
            if (animating) {
                curScrollY = this.mScroller.getCurrY();
            } else {
                int curScrollY2 = this.mScrollY;
                curScrollY = curScrollY2;
            }
            if (this.mCurScrollY == curScrollY) {
                fullRedrawNeeded2 = fullRedrawNeeded;
            } else {
                this.mCurScrollY = curScrollY;
                View view = this.mView;
                if (view instanceof RootViewSurfaceTaker) {
                    ((RootViewSurfaceTaker) view).onRootViewScrollYChanged(curScrollY);
                }
                fullRedrawNeeded2 = true;
            }
            float appScale = this.mAttachInfo.mApplicationScale;
            boolean scalingRequired = this.mAttachInfo.mScalingRequired;
            Rect dirty = this.mDirty;
            if (this.mSurfaceHolder != null) {
                dirty.setEmpty();
                if (animating && (scroller = this.mScroller) != null) {
                    scroller.abortAnimation();
                }
                return false;
            }
            if (fullRedrawNeeded2) {
                dirty.set(0, 0, (int) ((this.mWidth * appScale) + 0.5f), (int) ((this.mHeight * appScale) + 0.5f));
            }
            if (DEBUG_ORIENTATION || DEBUG_DRAW) {
                Log.v(this.mTag, "Draw " + this.mView + "/" + ((Object) this.mWindowAttributes.getTitle()) + ": dirty={" + dirty.left + "," + dirty.top + "," + dirty.right + "," + dirty.bottom + "} surface=" + surface + " surface.isValid()=" + surface.isValid() + ", appScale = " + appScale + ", width=" + this.mWidth + ", height=" + this.mHeight + ", mScrollY = " + this.mScrollY + ", mCurScrollY = " + this.mCurScrollY + ", animating = " + animating + ", mIsAnimating = " + this.mIsAnimating + ", this = " + this);
            }
            this.mAttachInfo.mTreeObserver.dispatchOnDraw();
            int xOffset3 = -this.mCanvasOffsetX;
            int yOffset = (-this.mCanvasOffsetY) + curScrollY;
            WindowManager.LayoutParams params = this.mWindowAttributes;
            Rect surfaceInsets = params != null ? params.surfaceInsets : null;
            if (surfaceInsets == null) {
                xOffset = xOffset3;
            } else {
                int xOffset4 = xOffset3 - surfaceInsets.left;
                yOffset -= surfaceInsets.top;
                dirty.offset(surfaceInsets.left, surfaceInsets.top);
                xOffset = xOffset4;
            }
            Drawable drawable2 = this.mAttachInfo.mAccessibilityFocusDrawable;
            if (drawable2 != null) {
                Rect bounds = this.mAttachInfo.mTmpInvalRect;
                boolean hasFocus = getAccessibilityFocusedRect(bounds);
                if (!hasFocus) {
                    bounds.setEmpty();
                }
                if (!bounds.equals(drawable2.getBounds())) {
                    accessibilityFocusDirty = true;
                    this.mAttachInfo.mDrawingTime = this.mChoreographer.getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
                    boolean useAsyncReport = false;
                    if (dirty.isEmpty() || this.mIsAnimating || accessibilityFocusDirty) {
                        if (isHardwareEnabled()) {
                            int yOffset2 = yOffset;
                            if (this.mAttachInfo.mThreadedRenderer == null) {
                                xOffset2 = xOffset;
                            } else if (this.mAttachInfo.mThreadedRenderer.isEnabled()) {
                                xOffset2 = xOffset;
                            } else if (!this.mAttachInfo.mThreadedRenderer.isRequested()) {
                                xOffset2 = xOffset;
                            } else if (this.mSurface.isValid()) {
                                try {
                                } catch (Surface.OutOfResourcesException e) {
                                    e = e;
                                }
                                try {
                                    this.mAttachInfo.mThreadedRenderer.initializeIfNeeded(this.mWidth, this.mHeight, this.mAttachInfo, this.mSurface, surfaceInsets);
                                    this.mFullRedrawNeeded = true;
                                    scheduleTraversals();
                                    return false;
                                } catch (Surface.OutOfResourcesException e2) {
                                    e = e2;
                                    handleOutOfResourcesException(e);
                                    return false;
                                }
                            } else {
                                xOffset2 = xOffset;
                            }
                            if (!drawSoftware(surface, this.mAttachInfo, xOffset2, yOffset2, scalingRequired, dirty, surfaceInsets)) {
                                if (DEBUG_DRAW) {
                                    Log.v(this.mTag, "drawSoftware return: this = " + this);
                                    return false;
                                }
                                return false;
                            }
                        } else {
                            boolean invalidateRoot = accessibilityFocusDirty || this.mInvalidateRootRequested;
                            this.mInvalidateRootRequested = false;
                            this.mIsAnimating = false;
                            if (this.mHardwareYOffset != yOffset || this.mHardwareXOffset != xOffset) {
                                this.mHardwareYOffset = yOffset;
                                this.mHardwareXOffset = xOffset;
                                invalidateRoot = true;
                            }
                            if (invalidateRoot) {
                                this.mAttachInfo.mThreadedRenderer.invalidateRoot();
                            }
                            dirty.setEmpty();
                            boolean updated = updateContentDrawBounds();
                            boolean invalidateRoot2 = this.mReportNextDraw;
                            if (!invalidateRoot2) {
                                drawable = drawable2;
                            } else {
                                drawable = drawable2;
                                this.mAttachInfo.mThreadedRenderer.setStopped(false);
                            }
                            if (updated) {
                                requestDrawWindow();
                            }
                            useAsyncReport = true;
                            if (forceDraw) {
                                this.mAttachInfo.mThreadedRenderer.forceDrawNextFrame();
                            }
                            this.mAttachInfo.mThreadedRenderer.draw(this.mView, this.mAttachInfo, this);
                        }
                    }
                    if (animating) {
                        this.mFullRedrawNeeded = true;
                        scheduleTraversals();
                    }
                    return useAsyncReport;
                }
            }
            accessibilityFocusDirty = false;
            this.mAttachInfo.mDrawingTime = this.mChoreographer.getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
            boolean useAsyncReport2 = false;
            if (dirty.isEmpty()) {
            }
            if (isHardwareEnabled()) {
            }
            if (animating) {
            }
            return useAsyncReport2;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4976=5] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x00c8 A[Catch: all -> 0x01ac, TryCatch #4 {all -> 0x01ac, blocks: (B:16:0x006a, B:18:0x006e, B:21:0x00a4, B:28:0x00b7, B:30:0x00c8, B:31:0x010e, B:33:0x0119, B:35:0x011e, B:37:0x0122, B:39:0x0131, B:27:0x00b1, B:20:0x0072), top: B:78:0x006a }] */
    /* JADX WARN: Removed duplicated region for block: B:33:0x0119 A[Catch: all -> 0x01ac, TryCatch #4 {all -> 0x01ac, blocks: (B:16:0x006a, B:18:0x006e, B:21:0x00a4, B:28:0x00b7, B:30:0x00c8, B:31:0x010e, B:33:0x0119, B:35:0x011e, B:37:0x0122, B:39:0x0131, B:27:0x00b1, B:20:0x0072), top: B:78:0x006a }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x011e A[Catch: all -> 0x01ac, TryCatch #4 {all -> 0x01ac, blocks: (B:16:0x006a, B:18:0x006e, B:21:0x00a4, B:28:0x00b7, B:30:0x00c8, B:31:0x010e, B:33:0x0119, B:35:0x011e, B:37:0x0122, B:39:0x0131, B:27:0x00b1, B:20:0x0072), top: B:78:0x006a }] */
    /* JADX WARN: Removed duplicated region for block: B:36:0x0121  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0131 A[Catch: all -> 0x01ac, TRY_LEAVE, TryCatch #4 {all -> 0x01ac, blocks: (B:16:0x006a, B:18:0x006e, B:21:0x00a4, B:28:0x00b7, B:30:0x00c8, B:31:0x010e, B:33:0x0119, B:35:0x011e, B:37:0x0122, B:39:0x0131, B:27:0x00b1, B:20:0x0072), top: B:78:0x006a }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean drawSoftware(Surface surface, View.AttachInfo attachInfo, int xoff, int yoff, boolean scalingRequired, Rect dirty, Rect surfaceInsets) {
        int dirtyYOffset;
        int dirtyYOffset2;
        String str;
        String str2;
        String str3;
        IllegalArgumentException e;
        String str4;
        StringBuilder append;
        boolean z;
        CompatibilityInfo.Translator translator;
        if (surfaceInsets != null) {
            int dirtyXOffset = xoff + surfaceInsets.left;
            int dirtyYOffset3 = yoff + surfaceInsets.top;
            dirtyYOffset = dirtyYOffset3;
            dirtyYOffset2 = dirtyXOffset;
        } else {
            dirtyYOffset = yoff;
            dirtyYOffset2 = xoff;
        }
        int dirtyXOffset2 = -dirtyYOffset2;
        try {
            try {
                dirty.offset(dirtyXOffset2, -dirtyYOffset);
                int i = dirty.left;
                int i2 = dirty.top;
                int i3 = dirty.right;
                int left = dirty.bottom;
                if (Trace.isTagEnabled(8L)) {
                    str = ", canvas = ";
                    str2 = "Could not unlock surface, surface = ";
                    Trace.traceBegin(8L, "drawSoftware lockCanvas");
                } else {
                    str = ", canvas = ";
                    str2 = "Could not unlock surface, surface = ";
                }
                Canvas canvas = this.mSurface.lockCanvas(dirty);
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceEnd(8L);
                }
                canvas.setDensity(this.mDensity);
                try {
                    if (DEBUG_ORIENTATION || DEBUG_DRAW) {
                        Log.v(this.mTag, "Surface " + surface + " drawing to bitmap w=" + canvas.getWidth() + ", h=" + canvas.getHeight());
                    }
                    try {
                        if (canvas.isOpaque() && yoff == 0 && xoff == 0) {
                            z = false;
                            dirty.setEmpty();
                            this.mIsAnimating = z;
                            this.mView.mPrivateFlags |= 32;
                            if (DEBUG_DRAW) {
                                Context cxt = this.mView.getContext();
                                Log.i(this.mTag, "Drawing: package:" + cxt.getPackageName() + ", metrics=" + cxt.getResources().getDisplayMetrics() + ", compatibilityInfo=" + cxt.getResources().getCompatibilityInfo());
                            }
                            canvas.translate(-xoff, -yoff);
                            translator = this.mTranslator;
                            if (translator != null) {
                                translator.translateCanvas(canvas);
                            }
                            canvas.setScreenDensity(!scalingRequired ? this.mNoncompatDensity : 0);
                            this.mView.draw(canvas);
                            drawAccessibilityFocusedDrawableIfNeeded(canvas);
                            if (DEBUG_DRAW) {
                                Log.v(this.mTag, "Drawing view end- : mView = " + this.mView + ", this = " + this);
                            }
                            surface.unlockCanvasAndPost(canvas);
                            if (!LOCAL_LOGV || DEBUG_DRAW) {
                                Log.v(this.mTag, "Surface " + surface + " unlockCanvasAndPost");
                                return true;
                            }
                            return true;
                        }
                        surface.unlockCanvasAndPost(canvas);
                        if (LOCAL_LOGV) {
                        }
                        Log.v(this.mTag, "Surface " + surface + " unlockCanvasAndPost");
                        return true;
                    } catch (IllegalArgumentException e2) {
                        e = e2;
                        str4 = this.mTag;
                        append = new StringBuilder().append(str2).append(surface);
                        str3 = str;
                        Log.e(str4, append.append(str3).append(canvas).append(", this = ").append(this).toString(), e);
                        this.mLayoutRequested = true;
                        return false;
                    }
                    z = false;
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    dirty.setEmpty();
                    this.mIsAnimating = z;
                    this.mView.mPrivateFlags |= 32;
                    if (DEBUG_DRAW) {
                    }
                    canvas.translate(-xoff, -yoff);
                    translator = this.mTranslator;
                    if (translator != null) {
                    }
                    canvas.setScreenDensity(!scalingRequired ? this.mNoncompatDensity : 0);
                    this.mView.draw(canvas);
                    drawAccessibilityFocusedDrawableIfNeeded(canvas);
                    if (DEBUG_DRAW) {
                    }
                } catch (Throwable e3) {
                    str3 = str;
                    String str5 = str2;
                    try {
                        surface.unlockCanvasAndPost(canvas);
                        if (LOCAL_LOGV || DEBUG_DRAW) {
                            Log.v(this.mTag, "Surface " + surface + " unlockCanvasAndPost");
                        }
                        throw e3;
                    } catch (IllegalArgumentException e4) {
                        e = e4;
                        str4 = this.mTag;
                        append = new StringBuilder().append(str5).append(surface);
                        Log.e(str4, append.append(str3).append(canvas).append(", this = ").append(this).toString(), e);
                        this.mLayoutRequested = true;
                        return false;
                    }
                }
            } finally {
                dirty.offset(dirtyYOffset2, dirtyYOffset);
            }
        } catch (Surface.OutOfResourcesException e5) {
            handleOutOfResourcesException(e5);
            dirty.offset(dirtyYOffset2, dirtyYOffset);
            return false;
        } catch (IllegalArgumentException e6) {
            Log.e(this.mTag, "Could not lock surface", e6);
            this.mLayoutRequested = true;
            dirty.offset(dirtyYOffset2, dirtyYOffset);
            return false;
        }
    }

    private void drawAccessibilityFocusedDrawableIfNeeded(Canvas canvas) {
        Rect bounds = this.mAttachInfo.mTmpInvalRect;
        if (getAccessibilityFocusedRect(bounds)) {
            Drawable drawable = getAccessibilityFocusedDrawable();
            if (drawable != null) {
                drawable.setBounds(bounds);
                drawable.draw(canvas);
            }
        } else if (this.mAttachInfo.mAccessibilityFocusDrawable != null) {
            this.mAttachInfo.mAccessibilityFocusDrawable.setBounds(0, 0, 0, 0);
        }
    }

    private boolean getAccessibilityFocusedRect(Rect bounds) {
        View host;
        AccessibilityManager manager = AccessibilityManager.getInstance(this.mView.mContext);
        if (!manager.isEnabled() || !manager.isTouchExplorationEnabled() || (host = this.mAccessibilityFocusedHost) == null || host.mAttachInfo == null) {
            return false;
        }
        AccessibilityNodeProvider provider = host.getAccessibilityNodeProvider();
        if (provider == null) {
            host.getBoundsOnScreen(bounds, true);
        } else {
            AccessibilityNodeInfo accessibilityNodeInfo = this.mAccessibilityFocusedVirtualView;
            if (accessibilityNodeInfo == null) {
                return false;
            }
            accessibilityNodeInfo.getBoundsInScreen(bounds);
        }
        View.AttachInfo attachInfo = this.mAttachInfo;
        bounds.offset(0, attachInfo.mViewRootImpl.mScrollY);
        bounds.offset(-attachInfo.mWindowLeft, -attachInfo.mWindowTop);
        if (!bounds.intersect(0, 0, attachInfo.mViewRootImpl.mWidth, attachInfo.mViewRootImpl.mHeight)) {
            bounds.setEmpty();
        }
        return !bounds.isEmpty();
    }

    private Drawable getAccessibilityFocusedDrawable() {
        if (this.mAttachInfo.mAccessibilityFocusDrawable == null) {
            TypedValue value = new TypedValue();
            boolean resolved = this.mView.mContext.getTheme().resolveAttribute(R.attr.accessibilityFocusedDrawable, value, true);
            if (resolved) {
                this.mAttachInfo.mAccessibilityFocusDrawable = this.mView.mContext.getDrawable(value.resourceId);
            }
        }
        if (this.mAttachInfo.mAccessibilityFocusDrawable instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) this.mAttachInfo.mAccessibilityFocusDrawable;
            drawable.setStroke(this.mAccessibilityManager.getAccessibilityFocusStrokeWidth(), this.mAccessibilityManager.getAccessibilityFocusColor());
        }
        return this.mAttachInfo.mAccessibilityFocusDrawable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSystemGestureExclusionRectsForView(View view) {
        this.mGestureExclusionTracker.updateRectsForView(view);
        this.mHandler.sendEmptyMessage(30);
    }

    void systemGestureExclusionChanged() {
        List<Rect> rectsForWindowManager = this.mGestureExclusionTracker.computeChangedRects();
        if (rectsForWindowManager != null && this.mView != null) {
            try {
                this.mWindowSession.reportSystemGestureExclusionChanged(this.mWindow, rectsForWindowManager);
                this.mAttachInfo.mTreeObserver.dispatchOnSystemGestureExclusionRectsChanged(rectsForWindowManager);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setRootSystemGestureExclusionRects(List<Rect> rects) {
        this.mGestureExclusionTracker.setRootRects(rects);
        this.mHandler.sendEmptyMessage(30);
    }

    public List<Rect> getRootSystemGestureExclusionRects() {
        return this.mGestureExclusionTracker.getRootRects();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateKeepClearRectsForView(View view) {
        this.mKeepClearRectsTracker.updateRectsForView(view);
        this.mUnrestrictedKeepClearRectsTracker.updateRectsForView(view);
        this.mHandler.sendEmptyMessage(35);
    }

    void keepClearRectsChanged() {
        boolean restrictedKeepClearRectsChanged = this.mKeepClearRectsTracker.computeChanges();
        boolean unrestrictedKeepClearRectsChanged = this.mUnrestrictedKeepClearRectsTracker.computeChanges();
        if ((restrictedKeepClearRectsChanged || unrestrictedKeepClearRectsChanged) && this.mView != null) {
            this.mHasPendingKeepClearAreaChange = true;
            if (!this.mHandler.hasMessages(36)) {
                this.mHandler.sendEmptyMessageDelayed(36, 100L);
                reportKeepClearAreasChanged();
            }
        }
    }

    void reportKeepClearAreasChanged() {
        if (!this.mHasPendingKeepClearAreaChange) {
            return;
        }
        this.mHasPendingKeepClearAreaChange = false;
        List<Rect> restrictedKeepClearRects = this.mKeepClearRectsTracker.getLastComputedRects();
        List<Rect> unrestrictedKeepClearRects = this.mUnrestrictedKeepClearRectsTracker.getLastComputedRects();
        try {
            this.mWindowSession.reportKeepClearAreasChanged(this.mWindow, restrictedKeepClearRects, unrestrictedKeepClearRects);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestInvalidateRootRenderNode() {
        this.mInvalidateRootRequested = true;
    }

    boolean scrollToRectOrFocus(Rect rectangle, boolean immediate) {
        int scrollY;
        Rect ci = this.mAttachInfo.mContentInsets;
        Rect vi = this.mAttachInfo.mVisibleInsets;
        int scrollY2 = 0;
        boolean handled = false;
        if (vi.left > ci.left || vi.top > ci.top || vi.right > ci.right || vi.bottom > ci.bottom) {
            scrollY2 = this.mScrollY;
            View focus = this.mView.findFocus();
            if (focus == null) {
                return false;
            }
            WeakReference<View> weakReference = this.mLastScrolledFocus;
            View lastScrolledFocus = weakReference != null ? weakReference.get() : null;
            if (focus != lastScrolledFocus) {
                rectangle = null;
            }
            if (DEBUG_INPUT_RESIZE) {
                Log.v(this.mTag, "Eval scroll: focus=" + focus + " rectangle=" + rectangle + " ci=" + ci + " vi=" + vi);
            }
            if (focus == lastScrolledFocus && !this.mScrollMayChange && rectangle == null) {
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(this.mTag, "Keeping scroll y=" + this.mScrollY + " vi=" + vi.toShortString());
                }
            } else {
                this.mLastScrolledFocus = new WeakReference<>(focus);
                this.mScrollMayChange = false;
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(this.mTag, "Need to scroll?");
                }
                if (focus.getGlobalVisibleRect(this.mVisRect, null)) {
                    if (DEBUG_INPUT_RESIZE) {
                        Log.v(this.mTag, "Root w=" + this.mView.getWidth() + " h=" + this.mView.getHeight() + " ci=" + ci.toShortString() + " vi=" + vi.toShortString());
                    }
                    if (rectangle == null) {
                        focus.getFocusedRect(this.mTempRect);
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus " + focus + ": focusRect=" + this.mTempRect.toShortString());
                        }
                        View view = this.mView;
                        if (view instanceof ViewGroup) {
                            ((ViewGroup) view).offsetDescendantRectToMyCoords(focus, this.mTempRect);
                        }
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus in window: focusRect=" + this.mTempRect.toShortString() + " visRect=" + this.mVisRect.toShortString());
                        }
                    } else {
                        this.mTempRect.set(rectangle);
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Request scroll to rect: " + this.mTempRect.toShortString() + " visRect=" + this.mVisRect.toShortString());
                        }
                    }
                    if (this.mTempRect.intersect(this.mVisRect)) {
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(this.mTag, "Focus window visible rect: " + this.mTempRect.toShortString());
                        }
                        if (this.mTempRect.height() > (this.mView.getHeight() - vi.top) - vi.bottom) {
                            if (DEBUG_INPUT_RESIZE) {
                                Log.v(this.mTag, "Too tall; leaving scrollY=" + scrollY2);
                            }
                        } else {
                            if (this.mTempRect.top < vi.top) {
                                scrollY = this.mTempRect.top - vi.top;
                                if (DEBUG_INPUT_RESIZE) {
                                    Log.v(this.mTag, "Top covered; scrollY=" + scrollY);
                                }
                            } else if (this.mTempRect.bottom > this.mView.getHeight() - vi.bottom) {
                                scrollY = this.mTempRect.bottom - (this.mView.getHeight() - vi.bottom);
                                if (DEBUG_INPUT_RESIZE) {
                                    Log.v(this.mTag, "Bottom covered; scrollY=" + scrollY);
                                }
                            } else {
                                scrollY2 = 0;
                            }
                            scrollY2 = scrollY;
                        }
                        handled = true;
                    }
                }
            }
        }
        if (scrollY2 != this.mScrollY) {
            if (DEBUG_INPUT_RESIZE) {
                Log.v(this.mTag, "Pan scroll changed: old=" + this.mScrollY + " , new=" + scrollY2);
            }
            if (!immediate) {
                if (this.mScroller == null) {
                    this.mScroller = new Scroller(this.mView.getContext());
                }
                Scroller scroller = this.mScroller;
                int i = this.mScrollY;
                scroller.startScroll(0, i, 0, scrollY2 - i);
            } else {
                Scroller scroller2 = this.mScroller;
                if (scroller2 != null) {
                    scroller2.abortAnimation();
                }
            }
            this.mScrollY = scrollY2;
        }
        return handled;
    }

    public View getAccessibilityFocusedHost() {
        return this.mAccessibilityFocusedHost;
    }

    public AccessibilityNodeInfo getAccessibilityFocusedVirtualView() {
        return this.mAccessibilityFocusedVirtualView;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAccessibilityFocus(View view, AccessibilityNodeInfo node) {
        if (this.mAccessibilityFocusedVirtualView != null) {
            AccessibilityNodeInfo focusNode = this.mAccessibilityFocusedVirtualView;
            View focusHost = this.mAccessibilityFocusedHost;
            this.mAccessibilityFocusedHost = null;
            this.mAccessibilityFocusedVirtualView = null;
            focusHost.clearAccessibilityFocusNoCallbacks(64);
            AccessibilityNodeProvider provider = focusHost.getAccessibilityNodeProvider();
            if (provider != null) {
                focusNode.getBoundsInParent(this.mTempRect);
                focusHost.invalidate(this.mTempRect);
                int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(focusNode.getSourceNodeId());
                provider.performAction(virtualNodeId, 128, null);
            }
            focusNode.recycle();
        }
        View view2 = this.mAccessibilityFocusedHost;
        if (view2 != null && view2 != view) {
            view2.clearAccessibilityFocusNoCallbacks(64);
        }
        this.mAccessibilityFocusedHost = view;
        this.mAccessibilityFocusedVirtualView = node;
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.invalidateRoot();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPointerCapture() {
        return this.mPointerCapture;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestPointerCapture(boolean enabled) {
        IBinder inputToken = getInputToken();
        if (inputToken == null) {
            Log.e(this.mTag, "No input channel to request Pointer Capture.");
        } else {
            InputManager.getInstance().requestPointerCapture(inputToken, enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePointerCaptureChanged(boolean hasCapture) {
        if (this.mPointerCapture == hasCapture) {
            return;
        }
        this.mPointerCapture = hasCapture;
        View view = this.mView;
        if (view != null) {
            view.dispatchPointerCaptureChanged(hasCapture);
        }
    }

    private void updateColorModeIfNeeded(int colorMode) {
        if (this.mAttachInfo.mThreadedRenderer == null) {
            return;
        }
        if (colorMode != 4 && !getConfiguration().isScreenWideColorGamut()) {
            colorMode = 0;
        }
        this.mAttachInfo.mThreadedRenderer.setColorMode(colorMode);
    }

    @Override // android.view.ViewParent
    public void requestChildFocus(View child, View focused) {
        if (DEBUG_INPUT_RESIZE) {
            Log.v(this.mTag, "Request child " + child + " focus: focus now " + focused + " in " + this);
        }
        checkThread();
        scheduleTraversals();
    }

    @Override // android.view.ViewParent
    public void clearChildFocus(View child) {
        if (DEBUG_INPUT_RESIZE) {
            Log.v(this.mTag, "Clearing child focus");
        }
        checkThread();
        scheduleTraversals();
    }

    @Override // android.view.ViewParent
    public ViewParent getParentForAccessibility() {
        return null;
    }

    @Override // android.view.ViewParent
    public void focusableViewAvailable(View v) {
        checkThread();
        View view = this.mView;
        if (view != null) {
            if (!view.hasFocus()) {
                if (sAlwaysAssignFocus || !this.mAttachInfo.mInTouchMode) {
                    v.requestFocus();
                    return;
                }
                return;
            }
            View focused = this.mView.findFocus();
            if (focused instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) focused;
                if (group.getDescendantFocusability() == 262144 && isViewDescendantOf(v, focused)) {
                    v.requestFocus();
                }
            }
        }
    }

    @Override // android.view.ViewParent
    public void recomputeViewAttributes(View child) {
        checkThread();
        if (this.mView == child) {
            this.mAttachInfo.mRecomputeGlobalAttributes = true;
            if (!this.mWillDrawSoon) {
                scheduleTraversals();
            }
        }
    }

    void dispatchDetachedFromWindow() {
        InputQueue inputQueue;
        this.mInsetsController.onWindowFocusLost();
        this.mFirstInputStage.onDetachedFromWindow();
        View view = this.mView;
        if (view != null && view.mAttachInfo != null) {
            this.mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(false);
            this.mView.dispatchDetachedFromWindow();
        }
        this.mAccessibilityInteractionConnectionManager.ensureNoConnection();
        removeSendWindowContentChangedCallback();
        destroyHardwareRenderer();
        setAccessibilityFocus(null, null);
        this.mInsetsController.cancelExistingAnimations();
        this.mView.assignParent(null);
        this.mView = null;
        this.mAttachInfo.mRootView = null;
        destroySurface();
        InputQueue.Callback callback = this.mInputQueueCallback;
        if (callback != null && (inputQueue = this.mInputQueue) != null) {
            callback.onInputQueueDestroyed(inputQueue);
            this.mInputQueue.dispose();
            this.mInputQueueCallback = null;
            this.mInputQueue = null;
        }
        try {
            this.mWindowSession.remove(this.mWindow);
        } catch (RemoteException e) {
            Log.e(this.mTag, "RemoteException remove window " + this.mWindow + " in " + this, e);
        }
        WindowInputEventReceiver windowInputEventReceiver = this.mInputEventReceiver;
        if (windowInputEventReceiver != null) {
            windowInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        unregisterListeners();
        unscheduleTraversals();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performConfigurationChange(MergedConfiguration mergedConfiguration, boolean force, int newDisplayId) {
        if (mergedConfiguration == null) {
            throw new IllegalArgumentException("No merged config provided.");
        }
        Configuration globalConfig = mergedConfiguration.getGlobalConfiguration();
        Configuration overrideConfig = mergedConfiguration.getOverrideConfiguration();
        if (DEBUG_CONFIGURATION) {
            Log.v(this.mTag, "Applying new config to window " + ((Object) this.mWindowAttributes.getTitle()) + ", globalConfig: " + globalConfig + ", overrideConfig: " + overrideConfig + ", force = " + force + ", this = " + this);
        }
        CompatibilityInfo ci = this.mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        if (!ci.equals(CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO)) {
            globalConfig = new Configuration(globalConfig);
            ci.applyToConfiguration(this.mNoncompatDensity, globalConfig);
        }
        ArrayList<ConfigChangedCallback> arrayList = sConfigCallbacks;
        synchronized (arrayList) {
            for (int i = arrayList.size() - 1; i >= 0; i--) {
                ArrayList<ConfigChangedCallback> arrayList2 = sConfigCallbacks;
                if (!arrayList2.get(i).getClass().getName().contains("OSSurfaceView")) {
                    arrayList2.get(i).onConfigurationChanged(globalConfig);
                }
            }
        }
        this.mLastReportedMergedConfiguration.setConfiguration(globalConfig, overrideConfig);
        View view = this.mView;
        if (view != null && (view instanceof DecorView)) {
            DecorView decorView = (DecorView) view;
            decorView.performMultiConfigurationChange(this.mLastReportedMergedConfiguration.getOverrideConfiguration());
        }
        this.mForceNextConfigUpdate = force;
        ActivityConfigCallback activityConfigCallback = this.mActivityConfigCallback;
        if (activityConfigCallback != null) {
            activityConfigCallback.onConfigurationChanged(overrideConfig, newDisplayId);
        } else {
            Configuration currentConfig = this.mView.getResources().getConfiguration();
            if (this.mForceNextConfigUpdate && currentConfig != null && currentConfig.diff(this.mLastConfigurationFromResources) == 0 && currentConfig.diff(overrideConfig) == CONFIG_SCREEN_CHANGES && currentConfig.diff(globalConfig) == CONFIG_SCREEN_CHANGES) {
                Log.i(TAG, "resource is old, need update to new resource");
                this.mView.getResources().setImpl(this.mContext.getApplicationContext().getResources().getImpl());
            }
            updateConfiguration(newDisplayId);
        }
        this.mForceNextConfigUpdate = false;
    }

    public void updateConfiguration(int newDisplayId) {
        View view = this.mView;
        if (view == null) {
            return;
        }
        Resources localResources = view.getResources();
        Configuration config = localResources.getConfiguration();
        if (newDisplayId != -1) {
            onMovedToDisplay(newDisplayId, config);
            WindowConfiguration winConfig = getConfiguration().windowConfiguration;
            float appScale = this.mAttachInfo.mApplicationScale;
            int requestedWidth = (int) ((this.mView.getMeasuredWidth() * appScale) + 0.5f);
            if (requestedWidth != winConfig.getAppBounds().width() && this.mLastConfigurationFromResources.diff(config) != 0 && this.mLastConfigurationFromResources.windowConfiguration.getAppBounds().width() == this.mSurfaceSize.x && this.mLastConfigurationFromResources.windowConfiguration.getAppBounds().height() == this.mSurfaceSize.y) {
                ArrayList<ConfigChangedCallback> arrayList = sConfigCallbacks;
                synchronized (arrayList) {
                    for (int i = arrayList.size() - 1; i >= 0; i--) {
                        StringBuilder append = new StringBuilder().append("sConfigCallbacks.get(").append(i).append(")=");
                        ArrayList<ConfigChangedCallback> arrayList2 = sConfigCallbacks;
                        Log.d("config_debug", append.append(arrayList2.get(i)).append(", winConfig=").append(winConfig).append(", mSurfaceSize=").append(this.mSurfaceSize).append(") getConfiguration()=").append(getConfiguration()).toString());
                        if (arrayList2.get(i).getClass().getName().contains("OSSurfaceView")) {
                            arrayList2.get(i).onConfigurationChanged(getConfiguration());
                        }
                    }
                }
            }
        }
        if (this.mForceNextConfigUpdate || this.mLastConfigurationFromResources.diff(config) != 0) {
            updateInternalDisplay(this.mDisplay.getDisplayId(), localResources);
            int lastLayoutDirection = this.mLastConfigurationFromResources.getLayoutDirection();
            int currentLayoutDirection = config.getLayoutDirection();
            this.mLastConfigurationFromResources.setTo(config);
            if (lastLayoutDirection != currentLayoutDirection && this.mViewLayoutDirectionInitial == 2) {
                this.mView.setLayoutDirection(currentLayoutDirection);
            }
            this.mView.dispatchConfigurationChanged(config);
            this.mForceNextWindowRelayout = true;
            requestLayout();
        }
        updateForceDarkMode();
    }

    public static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }
        ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    private static void forceLayout(View view) {
        view.forceLayout();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                forceLayout(group.getChildAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ViewRootHandler extends Handler {
        ViewRootHandler() {
        }

        @Override // android.os.Handler
        public String getMessageName(Message message) {
            switch (message.what) {
                case 1:
                    return "MSG_INVALIDATE";
                case 2:
                    return "MSG_INVALIDATE_RECT";
                case 3:
                    return "MSG_DIE";
                case 4:
                    return "MSG_RESIZED";
                case 5:
                    return "MSG_RESIZED_REPORT";
                case 6:
                    return "MSG_WINDOW_FOCUS_CHANGED";
                case 7:
                    return "MSG_DISPATCH_INPUT_EVENT";
                case 8:
                    return "MSG_DISPATCH_APP_VISIBILITY";
                case 9:
                    return "MSG_DISPATCH_GET_NEW_SURFACE";
                case 10:
                case 20:
                case 22:
                case 26:
                case 33:
                case 36:
                default:
                    return super.getMessageName(message);
                case 11:
                    return "MSG_DISPATCH_KEY_FROM_IME";
                case 12:
                    return "MSG_DISPATCH_KEY_FROM_AUTOFILL";
                case 13:
                    return "MSG_CHECK_FOCUS";
                case 14:
                    return "MSG_CLOSE_SYSTEM_DIALOGS";
                case 15:
                    return "MSG_DISPATCH_DRAG_EVENT";
                case 16:
                    return "MSG_DISPATCH_DRAG_LOCATION_EVENT";
                case 17:
                    return "MSG_DISPATCH_SYSTEM_UI_VISIBILITY";
                case 18:
                    return "MSG_UPDATE_CONFIGURATION";
                case 19:
                    return "MSG_PROCESS_INPUT_EVENTS";
                case 21:
                    return "MSG_CLEAR_ACCESSIBILITY_FOCUS_HOST";
                case 23:
                    return "MSG_WINDOW_MOVED";
                case 24:
                    return "MSG_SYNTHESIZE_INPUT_EVENT";
                case 25:
                    return "MSG_DISPATCH_WINDOW_SHOWN";
                case 27:
                    return "MSG_UPDATE_POINTER_ICON";
                case 28:
                    return "MSG_POINTER_CAPTURE_CHANGED";
                case 29:
                    return "MSG_INSETS_CONTROL_CHANGED";
                case 30:
                    return "MSG_SYSTEM_GESTURE_EXCLUSION_CHANGED";
                case 31:
                    return "MSG_SHOW_INSETS";
                case 32:
                    return "MSG_HIDE_INSETS";
                case 34:
                    return "MSG_WINDOW_TOUCH_MODE_CHANGED";
                case 35:
                    return "MSG_KEEP_CLEAR_RECTS_CHANGED";
                case 37:
                    return "MSG_TRAN_INVALIDATE";
            }
        }

        @Override // android.os.Handler
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            if (msg.what == 26 && msg.obj == null) {
                throw new NullPointerException("Attempted to call MSG_REQUEST_KEYBOARD_SHORTCUTS with null receiver:");
            }
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (Trace.isTagEnabled(8L)) {
                Trace.traceBegin(8L, getMessageName(msg));
            }
            try {
                handleMessageImpl(msg);
            } finally {
                Trace.traceEnd(8L);
            }
        }

        private void handleMessageImpl(Message msg) {
            switch (msg.what) {
                case 1:
                    ((View) msg.obj).invalidate();
                    return;
                case 2:
                    View.AttachInfo.InvalidateInfo info = (View.AttachInfo.InvalidateInfo) msg.obj;
                    info.target.invalidate(info.left, info.top, info.right, info.bottom);
                    info.recycle();
                    return;
                case 3:
                    ViewRootImpl.this.doDie();
                    return;
                case 4:
                case 5:
                    SomeArgs args = (SomeArgs) msg.obj;
                    if (ViewRootImpl.DEBUG_LAYOUT) {
                        Log.d(ViewRootImpl.this.mTag, "Handle RESIZE: message = " + msg.what + " ,this = " + ViewRootImpl.this);
                    }
                    ViewRootImpl.this.mInsetsController.onStateChanged((InsetsState) args.arg3);
                    ViewRootImpl.this.handleResized(msg.what, args);
                    args.recycle();
                    return;
                case 6:
                    ViewRootImpl.this.handleWindowFocusChanged();
                    return;
                case 7:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    InputEventReceiver receiver = (InputEventReceiver) args2.arg2;
                    ViewRootImpl.this.enqueueInputEvent((InputEvent) args2.arg1, receiver, 0, true);
                    args2.recycle();
                    return;
                case 8:
                    ViewRootImpl.this.handleAppVisibility(msg.arg1 != 0);
                    return;
                case 9:
                    ViewRootImpl.this.handleGetNewSurface();
                    return;
                case 11:
                    if (ViewRootImpl.LOCAL_LOGV || ViewDebugManager.DEBUG_KEY) {
                        Log.v(ViewRootImpl.this.mTag, "Dispatching key " + msg.obj + " from IME to " + ViewRootImpl.this.mView + " in " + this);
                    }
                    KeyEvent event = (KeyEvent) msg.obj;
                    if ((event.getFlags() & 8) != 0) {
                        event = KeyEvent.changeFlags(event, event.getFlags() & (-9));
                    }
                    ViewRootImpl.this.enqueueInputEvent(event, null, 1, true);
                    return;
                case 12:
                    if (ViewRootImpl.LOCAL_LOGV) {
                        Log.v(ViewRootImpl.TAG, "Dispatching key " + msg.obj + " from Autofill to " + ViewRootImpl.this.mView);
                    }
                    ViewRootImpl.this.enqueueInputEvent((KeyEvent) msg.obj, null, 0, true);
                    return;
                case 13:
                    ViewRootImpl.this.getImeFocusController().checkFocus(false, true);
                    return;
                case 14:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl.this.mView.onCloseSystemDialogs((String) msg.obj);
                        return;
                    }
                    return;
                case 15:
                case 16:
                    DragEvent event2 = (DragEvent) msg.obj;
                    event2.mLocalState = ViewRootImpl.this.mLocalDragState;
                    ViewRootImpl.this.handleDragEvent(event2);
                    return;
                case 17:
                    ViewRootImpl.this.handleDispatchSystemUiVisibilityChanged((SystemUiVisibilityInfo) msg.obj);
                    return;
                case 18:
                    Configuration config = (Configuration) msg.obj;
                    if (config.isOtherSeqNewer(ViewRootImpl.this.mLastReportedMergedConfiguration.getMergedConfiguration())) {
                        config = ViewRootImpl.this.mLastReportedMergedConfiguration.getGlobalConfiguration();
                    }
                    ViewRootImpl.this.mPendingMergedConfiguration.setConfiguration(config, ViewRootImpl.this.mLastReportedMergedConfiguration.getOverrideConfiguration());
                    ViewRootImpl.this.performConfigurationChange(new MergedConfiguration(ViewRootImpl.this.mPendingMergedConfiguration), false, -1);
                    return;
                case 19:
                    ViewRootImpl.this.mProcessInputEventsScheduled = false;
                    ViewRootImpl.this.doProcessInputEvents();
                    return;
                case 21:
                    ViewRootImpl.this.setAccessibilityFocus(null, null);
                    return;
                case 22:
                    if (ViewRootImpl.this.mView != null) {
                        ViewRootImpl viewRootImpl = ViewRootImpl.this;
                        viewRootImpl.invalidateWorld(viewRootImpl.mView);
                        return;
                    }
                    return;
                case 23:
                    if (ViewRootImpl.this.mAdded) {
                        int w = ViewRootImpl.this.mWinFrame.width();
                        int h = ViewRootImpl.this.mWinFrame.height();
                        int l = msg.arg1;
                        int t = msg.arg2;
                        ViewRootImpl.this.mTmpFrames.frame.left = l;
                        ViewRootImpl.this.mTmpFrames.frame.right = l + w;
                        ViewRootImpl.this.mTmpFrames.frame.top = t;
                        ViewRootImpl.this.mTmpFrames.frame.bottom = t + h;
                        ViewRootImpl viewRootImpl2 = ViewRootImpl.this;
                        viewRootImpl2.setFrame(viewRootImpl2.mTmpFrames.frame);
                        ViewRootImpl viewRootImpl3 = ViewRootImpl.this;
                        viewRootImpl3.maybeHandleWindowMove(viewRootImpl3.mWinFrame);
                        return;
                    }
                    return;
                case 24:
                    ViewRootImpl.this.enqueueInputEvent((InputEvent) msg.obj, null, 32, true);
                    return;
                case 25:
                    ViewRootImpl.this.handleDispatchWindowShown();
                    return;
                case 26:
                    IResultReceiver receiver2 = (IResultReceiver) msg.obj;
                    int deviceId = msg.arg1;
                    ViewRootImpl.this.handleRequestKeyboardShortcuts(receiver2, deviceId);
                    return;
                case 27:
                    ViewRootImpl.this.resetPointerIcon((MotionEvent) msg.obj);
                    return;
                case 28:
                    boolean hasCapture = msg.arg1 != 0;
                    ViewRootImpl.this.handlePointerCaptureChanged(hasCapture);
                    return;
                case 29:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    ViewRootImpl.this.mInsetsController.onStateChanged((InsetsState) args3.arg1);
                    InsetsSourceControl[] controls = (InsetsSourceControl[]) args3.arg2;
                    if (ViewRootImpl.this.mAdded) {
                        ViewRootImpl.this.mInsetsController.onControlsChanged(controls);
                    } else if (controls != null) {
                        for (InsetsSourceControl control : controls) {
                            if (control != null) {
                                control.release(new InsetsAnimationThreadControlRunner$$ExternalSyntheticLambda0());
                            }
                        }
                    }
                    args3.recycle();
                    return;
                case 30:
                    ViewRootImpl.this.systemGestureExclusionChanged();
                    return;
                case 31:
                    if (ViewRootImpl.this.mView == null) {
                        Object[] objArr = new Object[2];
                        objArr[0] = Integer.valueOf(msg.arg1);
                        objArr[1] = Boolean.valueOf(msg.arg2 == 1);
                        Log.e(ViewRootImpl.TAG, String.format("Calling showInsets(%d,%b) on window that no longer has views.", objArr));
                    }
                    ViewRootImpl.this.clearLowProfileModeIfNeeded(msg.arg1, msg.arg2 == 1);
                    ViewRootImpl.this.mInsetsController.show(msg.arg1, msg.arg2 == 1);
                    return;
                case 32:
                    ViewRootImpl.this.mInsetsController.hide(msg.arg1, msg.arg2 == 1);
                    return;
                case 33:
                    ViewRootImpl.this.handleScrollCaptureRequest((IScrollCaptureResponseListener) msg.obj);
                    return;
                case 34:
                    ViewRootImpl.this.handleWindowTouchModeChanged();
                    return;
                case 35:
                    ViewRootImpl.this.keepClearRectsChanged();
                    return;
                case 36:
                    ViewRootImpl.this.reportKeepClearAreasChanged();
                    return;
                case 37:
                    ViewRootImpl.this.invalidate();
                    Message msgDelay = ViewRootImpl.this.mHandler.obtainMessage(37);
                    ViewRootImpl.this.mHandler.sendMessageDelayed(msgDelay, ViewRootImpl.this.mDelayTime);
                    return;
                case 100:
                    boolean show = msg.arg1 == 1;
                    ViewRootImpl.this.handleCaptionViewStatus(show, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ensureTouchMode(boolean inTouchMode) {
        if (DBG) {
            Log.d("touchmode", "ensureTouchMode(" + inTouchMode + "), current touch mode is " + this.mAttachInfo.mInTouchMode);
        }
        if (this.mAttachInfo.mInTouchMode == inTouchMode) {
            return false;
        }
        try {
            this.mWindowSession.setInTouchMode(inTouchMode);
            return ensureTouchModeLocally(inTouchMode);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean ensureTouchModeLocally(boolean inTouchMode) {
        if (DBG) {
            Log.d("touchmode", "ensureTouchModeLocally(" + inTouchMode + "), current touch mode is " + this.mAttachInfo.mInTouchMode);
        }
        if (this.mAttachInfo.mInTouchMode == inTouchMode) {
            return false;
        }
        this.mAttachInfo.mInTouchMode = inTouchMode;
        this.mAttachInfo.mTreeObserver.dispatchOnTouchModeChanged(inTouchMode);
        return inTouchMode ? enterTouchMode() : leaveTouchMode();
    }

    private boolean enterTouchMode() {
        View focused;
        View view = this.mView;
        if (view == null || !view.hasFocus() || (focused = this.mView.findFocus()) == null || focused.isFocusableInTouchMode()) {
            return false;
        }
        ViewGroup ancestorToTakeFocus = findAncestorToTakeFocusInTouchMode(focused);
        if (ancestorToTakeFocus != null) {
            return ancestorToTakeFocus.requestFocus();
        }
        focused.clearFocusInternal(null, true, false);
        return true;
    }

    private static ViewGroup findAncestorToTakeFocusInTouchMode(View focused) {
        ViewParent parent = focused.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup vgParent = (ViewGroup) parent;
            if (vgParent.getDescendantFocusability() == 262144 && vgParent.isFocusableInTouchMode()) {
                return vgParent;
            }
            if (vgParent.isRootNamespace()) {
                return null;
            }
            parent = vgParent.getParent();
        }
        return null;
    }

    private boolean leaveTouchMode() {
        View view = this.mView;
        if (view != null) {
            if (view.hasFocus()) {
                View focusedView = this.mView.findFocus();
                if (!(focusedView instanceof ViewGroup) || ((ViewGroup) focusedView).getDescendantFocusability() != 262144) {
                    return false;
                }
            }
            return this.mView.restoreDefaultFocus();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public abstract class InputStage {
        protected static final int FINISH_HANDLED = 1;
        protected static final int FINISH_NOT_HANDLED = 2;
        protected static final int FORWARD = 0;
        private final InputStage mNext;
        private String mTracePrefix;

        public InputStage(InputStage next) {
            this.mNext = next;
        }

        public final void deliver(QueuedInputEvent q) {
            ViewDebugManager.getInstance().debugInputStageDeliverd(this, System.currentTimeMillis());
            if ((q.mFlags & 4) != 0) {
                forward(q);
            } else if (shouldDropInputEvent(q)) {
                finish(q, false);
            } else {
                traceEvent(q, 8L);
                try {
                    int result = onProcess(q);
                    Trace.traceEnd(8L);
                    apply(q, result);
                } catch (Throwable th) {
                    Trace.traceEnd(8L);
                    throw th;
                }
            }
        }

        protected void finish(QueuedInputEvent q, boolean handled) {
            q.mFlags |= 4;
            if (handled) {
                q.mFlags |= 8;
            }
            forward(q);
        }

        protected void forward(QueuedInputEvent q) {
            onDeliverToNext(q);
        }

        protected void apply(QueuedInputEvent q, int result) {
            if (result == 0) {
                forward(q);
            } else if (result == 1) {
                finish(q, true);
            } else if (result == 2) {
                finish(q, false);
            } else {
                throw new IllegalArgumentException("Invalid result: " + result);
            }
        }

        protected int onProcess(QueuedInputEvent q) {
            return 0;
        }

        protected void onDeliverToNext(QueuedInputEvent q) {
            if (ViewRootImpl.DEBUG_INPUT_STAGES) {
                Log.v(ViewRootImpl.this.mTag, "Done with " + getClass().getSimpleName() + ". " + q);
            }
            InputStage inputStage = this.mNext;
            if (inputStage != null) {
                inputStage.deliver(q);
            } else {
                ViewRootImpl.this.finishInputEvent(q);
            }
        }

        protected void onWindowFocusChanged(boolean hasWindowFocus) {
            InputStage inputStage = this.mNext;
            if (inputStage != null) {
                inputStage.onWindowFocusChanged(hasWindowFocus);
            }
        }

        protected void onDetachedFromWindow() {
            InputStage inputStage = this.mNext;
            if (inputStage != null) {
                inputStage.onDetachedFromWindow();
            }
        }

        protected boolean shouldDropInputEvent(QueuedInputEvent q) {
            String reason;
            if (ViewRootImpl.this.mView == null || !ViewRootImpl.this.mAdded) {
                Slog.w(ViewRootImpl.this.mTag, "Dropping event due to root view being removed: " + q.mEvent);
                return true;
            }
            if (!ViewRootImpl.this.mAttachInfo.mHasWindowFocus && !q.mEvent.isFromSource(2) && !ViewRootImpl.this.isAutofillUiShowing()) {
                reason = "no window focus";
            } else if (ViewRootImpl.this.mStopped) {
                reason = "window is stopped";
            } else if (ViewRootImpl.this.mIsAmbientMode && !q.mEvent.isFromSource(1)) {
                reason = "non-button event in ambient mode";
            } else if (!ViewRootImpl.this.mPausedForTransition || isBack(q.mEvent)) {
                return false;
            } else {
                reason = "paused for transition";
            }
            if (ViewRootImpl.isTerminalInputEvent(q.mEvent)) {
                q.mEvent.cancel();
                Slog.w(ViewRootImpl.this.mTag, "Cancelling event (" + reason + "):" + q.mEvent);
                return false;
            }
            Slog.w(ViewRootImpl.this.mTag, "Dropping event (" + reason + "):" + q.mEvent);
            return true;
        }

        void dump(String prefix, PrintWriter writer) {
            InputStage inputStage = this.mNext;
            if (inputStage != null) {
                inputStage.dump(prefix, writer);
            }
        }

        boolean isBack(InputEvent event) {
            return (event instanceof KeyEvent) && ((KeyEvent) event).getKeyCode() == 4;
        }

        private void traceEvent(QueuedInputEvent q, long traceTag) {
            if (!Trace.isTagEnabled(traceTag)) {
                return;
            }
            if (this.mTracePrefix == null) {
                this.mTracePrefix = getClass().getSimpleName();
            }
            Trace.traceBegin(traceTag, this.mTracePrefix + " id=0x" + Integer.toHexString(q.mEvent.getId()));
        }
    }

    /* loaded from: classes3.dex */
    abstract class AsyncInputStage extends InputStage {
        protected static final int DEFER = 3;
        private QueuedInputEvent mQueueHead;
        private int mQueueLength;
        private QueuedInputEvent mQueueTail;
        private final String mTraceCounter;

        public AsyncInputStage(InputStage next, String traceCounter) {
            super(next);
            this.mTraceCounter = traceCounter;
        }

        protected void defer(QueuedInputEvent q) {
            q.mFlags |= 2;
            enqueue(q);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void forward(QueuedInputEvent q) {
            q.mFlags &= -3;
            QueuedInputEvent curr = this.mQueueHead;
            if (curr == null) {
                super.forward(q);
                return;
            }
            int deviceId = q.mEvent.getDeviceId();
            QueuedInputEvent prev = null;
            boolean blocked = false;
            while (curr != null && curr != q) {
                if (!blocked && deviceId == curr.mEvent.getDeviceId()) {
                    blocked = true;
                }
                prev = curr;
                curr = curr.mNext;
            }
            if (blocked) {
                if (curr == null) {
                    enqueue(q);
                    return;
                }
                return;
            }
            if (curr != null) {
                curr = curr.mNext;
                dequeue(q, prev);
            }
            super.forward(q);
            while (curr != null) {
                if (deviceId == curr.mEvent.getDeviceId()) {
                    if ((curr.mFlags & 2) == 0) {
                        QueuedInputEvent next = curr.mNext;
                        dequeue(curr, prev);
                        super.forward(curr);
                        curr = next;
                    } else {
                        return;
                    }
                } else {
                    prev = curr;
                    curr = curr.mNext;
                }
            }
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void apply(QueuedInputEvent q, int result) {
            if (result == 3) {
                defer(q);
            } else {
                super.apply(q, result);
            }
        }

        private void enqueue(QueuedInputEvent q) {
            QueuedInputEvent queuedInputEvent = this.mQueueTail;
            if (queuedInputEvent == null) {
                this.mQueueHead = q;
                this.mQueueTail = q;
            } else {
                queuedInputEvent.mNext = q;
                this.mQueueTail = q;
            }
            int i = this.mQueueLength + 1;
            this.mQueueLength = i;
            Trace.traceCounter(4L, this.mTraceCounter, i);
        }

        private void dequeue(QueuedInputEvent q, QueuedInputEvent prev) {
            if (prev == null) {
                this.mQueueHead = q.mNext;
            } else {
                prev.mNext = q.mNext;
            }
            if (this.mQueueTail == q) {
                this.mQueueTail = prev;
            }
            q.mNext = null;
            int i = this.mQueueLength - 1;
            this.mQueueLength = i;
            Trace.traceCounter(4L, this.mTraceCounter, i);
        }

        @Override // android.view.ViewRootImpl.InputStage
        void dump(String prefix, PrintWriter writer) {
            writer.print(prefix);
            writer.print(getClass().getName());
            writer.print(": mQueueLength=");
            writer.println(this.mQueueLength);
            super.dump(prefix, writer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class NativePreImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePreImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                KeyEvent event = (KeyEvent) q.mEvent;
                if (isBack(event) && ViewRootImpl.this.mContext != null && WindowOnBackInvokedDispatcher.isOnBackInvokedCallbackEnabled(ViewRootImpl.this.mContext)) {
                    OnBackInvokedCallback topCallback = ViewRootImpl.this.getOnBackInvokedDispatcher().getTopCallback();
                    if (event.getAction() == 1) {
                        if (topCallback != null) {
                            topCallback.onBackInvoked();
                            return 1;
                        }
                    } else {
                        return 2;
                    }
                }
            }
            if (ViewRootImpl.this.mInputQueue != null && (q.mEvent instanceof KeyEvent)) {
                ViewRootImpl.this.mInputQueue.sendInputEvent(q.mEvent, q, true, this);
                return 3;
            }
            return 0;
        }

        @Override // android.view.InputQueue.FinishedInputEventCallback
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ViewPreImeInputStage extends InputStage {
        public ViewPreImeInputStage(InputStage next) {
            super(next);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            if (ViewRootImpl.this.mView.dispatchKeyEventPreIme(event)) {
                return 1;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ImeInputStage extends AsyncInputStage implements InputMethodManager.FinishedInputEventCallback {
        public ImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            int result = ViewRootImpl.this.mImeFocusController.onProcessImeInputStage(q, q.mEvent, ViewRootImpl.this.mWindowAttributes, this);
            switch (result) {
                case -1:
                    return 3;
                case 0:
                    return 0;
                case 1:
                    return 1;
                default:
                    throw new IllegalStateException("Unexpected result=" + result);
            }
        }

        @Override // android.view.inputmethod.InputMethodManager.FinishedInputEventCallback
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (ViewRootImpl.DEBUG_IMF || ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY) {
                Log.d(ViewRootImpl.this.mTag, "IME finishedEvent: handled = " + handled + ", event = " + q + ", viewAncestor = " + this);
            }
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class EarlyPostImeInputStage extends InputStage {
        public EarlyPostImeInputStage(InputStage next) {
            super(next);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            if (q.mEvent instanceof MotionEvent) {
                return processMotionEvent(q);
            }
            return 0;
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            if (ViewRootImpl.this.mAttachInfo.mTooltipHost != null) {
                ViewRootImpl.this.mAttachInfo.mTooltipHost.handleTooltipKey(event);
            }
            if (ViewRootImpl.this.checkForLeavingTouchModeAndConsume(event)) {
                return 1;
            }
            ViewRootImpl.this.mFallbackEventHandler.preDispatchKeyEvent(event);
            return 0;
        }

        private int processMotionEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            if (event.isFromSource(2)) {
                return processPointerEvent(q);
            }
            int action = event.getActionMasked();
            if ((action == 0 || action == 8) && event.isFromSource(8)) {
                ViewRootImpl.this.ensureTouchMode(false);
            }
            return 0;
        }

        private int processPointerEvent(QueuedInputEvent q) {
            AutofillManager afm;
            MotionEvent event = (MotionEvent) q.mEvent;
            if (ViewRootImpl.this.mTranslator != null) {
                ViewRootImpl.this.mTranslator.translateEventInScreenToAppWindow(event);
            }
            int action = event.getAction();
            if (action == 0 || action == 8) {
                ViewRootImpl.this.ensureTouchMode(true);
            }
            if (action == 0 && (afm = ViewRootImpl.this.getAutofillManager()) != null) {
                afm.requestHideFillUi();
            }
            if (action == 0 && ViewRootImpl.this.mAttachInfo.mTooltipHost != null) {
                ViewRootImpl.this.mAttachInfo.mTooltipHost.hideTooltip();
            }
            if (ViewRootImpl.this.mCurScrollY != 0) {
                event.offsetLocation(0.0f, ViewRootImpl.this.mCurScrollY);
            }
            if (event.isTouchEvent()) {
                ViewRootImpl.this.mLastTouchPoint.x = event.getRawX();
                ViewRootImpl.this.mLastTouchPoint.y = event.getRawY();
                ViewRootImpl.this.mLastTouchSource = event.getSource();
                return 0;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class NativePostImeInputStage extends AsyncInputStage implements InputQueue.FinishedInputEventCallback {
        public NativePostImeInputStage(InputStage next, String traceCounter) {
            super(next, traceCounter);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            if (ViewRootImpl.this.mInputQueue != null) {
                ViewRootImpl.this.mInputQueue.sendInputEvent(q.mEvent, q, false, this);
                return 3;
            }
            return 0;
        }

        @Override // android.view.InputQueue.FinishedInputEventCallback
        public void onFinishedInputEvent(Object token, boolean handled) {
            QueuedInputEvent q = (QueuedInputEvent) token;
            if (handled) {
                finish(q, true);
            } else {
                forward(q);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ViewPostImeInputStage extends InputStage {
        public ViewPostImeInputStage(InputStage next) {
            super(next);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            if (q.mEvent instanceof KeyEvent) {
                return processKeyEvent(q);
            }
            int source = q.mEvent.getSource();
            if ((source & 2) != 0) {
                return processPointerEvent(q);
            }
            if ((source & 4) != 0) {
                return processTrackballEvent(q);
            }
            return processGenericMotionEvent(q);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void onDeliverToNext(QueuedInputEvent q) {
            if (ViewRootImpl.this.mUnbufferedInputDispatch && (q.mEvent instanceof MotionEvent) && ((MotionEvent) q.mEvent).isTouchEvent() && ViewRootImpl.isTerminalInputEvent(q.mEvent)) {
                ViewRootImpl.this.mUnbufferedInputDispatch = false;
                ViewRootImpl.this.scheduleConsumeBatchedInput();
            }
            super.onDeliverToNext(q);
        }

        private boolean performFocusNavigation(KeyEvent event) {
            int direction = 0;
            switch (event.getKeyCode()) {
                case 19:
                    if (event.hasNoModifiers()) {
                        direction = 33;
                        break;
                    }
                    break;
                case 20:
                    if (event.hasNoModifiers()) {
                        direction = 130;
                        break;
                    }
                    break;
                case 21:
                    if (event.hasNoModifiers()) {
                        direction = 17;
                        break;
                    }
                    break;
                case 22:
                    if (event.hasNoModifiers()) {
                        direction = 66;
                        break;
                    }
                    break;
                case 61:
                    if (event.hasNoModifiers()) {
                        direction = 2;
                        break;
                    } else if (event.hasModifiers(1)) {
                        direction = 1;
                        break;
                    }
                    break;
            }
            if (direction != 0) {
                View focused = ViewRootImpl.this.mView.findFocus();
                if (focused != null) {
                    View v = focused.focusSearch(direction);
                    if (v != null && v != focused) {
                        focused.getFocusedRect(ViewRootImpl.this.mTempRect);
                        if (ViewRootImpl.this.mView instanceof ViewGroup) {
                            ((ViewGroup) ViewRootImpl.this.mView).offsetDescendantRectToMyCoords(focused, ViewRootImpl.this.mTempRect);
                            ((ViewGroup) ViewRootImpl.this.mView).offsetRectIntoDescendantCoords(v, ViewRootImpl.this.mTempRect);
                        }
                        if (v.requestFocus(direction, ViewRootImpl.this.mTempRect)) {
                            boolean isFastScrolling = event.getRepeatCount() > 0;
                            ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getConstantForFocusDirection(direction, isFastScrolling));
                            return true;
                        }
                    }
                    if (ViewRootImpl.this.mView.dispatchUnhandledMove(focused, direction)) {
                        return true;
                    }
                } else if (ViewRootImpl.this.mView.restoreDefaultFocus()) {
                    return true;
                }
            }
            return false;
        }

        private boolean performKeyboardGroupNavigation(int direction) {
            View cluster;
            View focused = ViewRootImpl.this.mView.findFocus();
            if (focused == null && ViewRootImpl.this.mView.restoreDefaultFocus()) {
                return true;
            }
            if (focused == null) {
                cluster = ViewRootImpl.this.keyboardNavigationClusterSearch(null, direction);
            } else {
                cluster = focused.keyboardNavigationClusterSearch(null, direction);
            }
            int realDirection = direction;
            realDirection = (direction == 2 || direction == 1) ? 130 : 130;
            if (cluster != null && cluster.isRootNamespace()) {
                if (!cluster.restoreFocusNotInCluster()) {
                    cluster = ViewRootImpl.this.keyboardNavigationClusterSearch(null, direction);
                } else {
                    ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
                    return true;
                }
            }
            if (cluster != null && cluster.restoreFocusInCluster(realDirection)) {
                ViewRootImpl.this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
                return true;
            }
            return false;
        }

        private int processKeyEvent(QueuedInputEvent q) {
            KeyEvent event = (KeyEvent) q.mEvent;
            if (ViewRootImpl.this.mUnhandledKeyManager.preViewDispatch(event)) {
                if (ViewDebugManager.DEBUG_ENG) {
                    Log.v(ViewRootImpl.this.mTag, "App handle dispatchUnique event = " + event + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
                }
                return 1;
            } else if (ViewRootImpl.this.mView.dispatchKeyEvent(event)) {
                if (ViewDebugManager.DEBUG_ENG) {
                    Log.v(ViewRootImpl.this.mTag, "App handle key event: event = " + event + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
                }
                return 1;
            } else if (shouldDropInputEvent(q)) {
                return 2;
            } else {
                if (ViewRootImpl.this.mUnhandledKeyManager.dispatch(ViewRootImpl.this.mView, event)) {
                    return 1;
                }
                int groupNavigationDirection = 0;
                if (event.getAction() == 0 && event.getKeyCode() == 61) {
                    if (KeyEvent.metaStateHasModifiers(event.getMetaState(), 65536)) {
                        groupNavigationDirection = 2;
                    } else if (KeyEvent.metaStateHasModifiers(event.getMetaState(), 65537)) {
                        groupNavigationDirection = 1;
                    }
                }
                if (event.getAction() == 0 && !KeyEvent.metaStateHasNoModifiers(event.getMetaState()) && event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(event.getKeyCode()) && groupNavigationDirection == 0) {
                    if (ViewRootImpl.this.mView.dispatchKeyShortcutEvent(event)) {
                        return 1;
                    }
                    if (shouldDropInputEvent(q)) {
                        return 2;
                    }
                }
                if (ViewRootImpl.this.mFallbackEventHandler.dispatchKeyEvent(event)) {
                    return 1;
                }
                if (shouldDropInputEvent(q)) {
                    return 2;
                }
                if (event.getAction() == 0) {
                    return groupNavigationDirection != 0 ? performKeyboardGroupNavigation(groupNavigationDirection) ? 1 : 0 : performFocusNavigation(event) ? 1 : 0;
                }
                return 0;
            }
        }

        private int processPointerEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            ViewRootImpl.this.mHandwritingInitiator.onTouchEvent(event);
            ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested = false;
            ViewRootImpl.this.mAttachInfo.mHandlingPointerEvent = true;
            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(ViewRootImpl.this.mScrollScenario.setAction(0).setBoostStatus(0).setMotionEvent(event).setContext(ViewRootImpl.this.mContext));
            boolean handled = ViewRootImpl.this.mView.dispatchPointerEvent(event);
            try {
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(ViewRootImpl.this.mScrollScenario.setBoostStatus(1).setMotionEvent(event));
            } catch (RuntimeException e) {
                Log.e(ViewRootImpl.this.mTag, "FATAL EXCEPTION when perfHint: " + e);
                e.printStackTrace();
            }
            if (handled && ViewDebugManager.DEBUG_ENG) {
                Log.v(ViewRootImpl.this.mTag, "App handle pointer event: event = " + event + ", mView = " + ViewRootImpl.this.mView + ", this = " + this);
            }
            maybeUpdatePointerIcon(event);
            ViewRootImpl.this.maybeUpdateTooltip(event);
            ViewRootImpl.this.mAttachInfo.mHandlingPointerEvent = false;
            if (ViewRootImpl.this.mAttachInfo.mUnbufferedDispatchRequested && !ViewRootImpl.this.mUnbufferedInputDispatch) {
                ViewRootImpl.this.mUnbufferedInputDispatch = true;
                if (ViewRootImpl.this.mConsumeBatchedInputScheduled) {
                    ViewRootImpl.this.scheduleConsumeBatchedInputImmediately();
                }
            }
            return handled ? 1 : 0;
        }

        private void maybeUpdatePointerIcon(MotionEvent event) {
            if (event.getPointerCount() == 1 && event.isFromSource(8194)) {
                if (event.getActionMasked() == 9 || event.getActionMasked() == 10) {
                    ViewRootImpl.this.mPointerIconType = 1;
                }
                if (event.getActionMasked() != 10 && !ViewRootImpl.this.updatePointerIcon(event) && event.getActionMasked() == 7) {
                    ViewRootImpl.this.mPointerIconType = 1;
                }
            }
        }

        private int processTrackballEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            return ((!event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE) || (ViewRootImpl.this.hasPointerCapture() && !ViewRootImpl.this.mView.dispatchCapturedPointerEvent(event))) && !ViewRootImpl.this.mView.dispatchTrackballEvent(event)) ? 0 : 1;
        }

        private int processGenericMotionEvent(QueuedInputEvent q) {
            MotionEvent event = (MotionEvent) q.mEvent;
            return ((event.isFromSource(InputDevice.SOURCE_TOUCHPAD) && ViewRootImpl.this.hasPointerCapture() && ViewRootImpl.this.mView.dispatchCapturedPointerEvent(event)) || ViewRootImpl.this.mView.dispatchGenericMotionEvent(event)) ? 1 : 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetPointerIcon(MotionEvent event) {
        this.mPointerIconType = 1;
        updatePointerIcon(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updatePointerIcon(MotionEvent event) {
        float x = event.getX(0);
        float y = event.getY(0);
        View view = this.mView;
        if (view == null) {
            Slog.d(this.mTag, "updatePointerIcon called after view was removed");
            return false;
        } else if (x >= 0.0f && x < view.getWidth() && y >= 0.0f && y < this.mView.getHeight()) {
            PointerIcon pointerIcon = this.mView.onResolvePointerIcon(event, 0);
            int pointerType = pointerIcon != null ? pointerIcon.getType() : 1000;
            if (this.mPointerIconType != pointerType) {
                this.mPointerIconType = pointerType;
                this.mCustomPointerIcon = null;
                if (pointerType != -1) {
                    InputManager.getInstance().setPointerIconType(pointerType);
                    return true;
                }
            }
            if (this.mPointerIconType == -1 && !pointerIcon.equals(this.mCustomPointerIcon)) {
                this.mCustomPointerIcon = pointerIcon;
                InputManager.getInstance().setCustomPointerIcon(this.mCustomPointerIcon);
            }
            return true;
        } else {
            Slog.d(this.mTag, "updatePointerIcon called with position out of bounds");
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeUpdateTooltip(MotionEvent event) {
        if (event.getPointerCount() != 1) {
            return;
        }
        int action = event.getActionMasked();
        if (action != 9 && action != 7 && action != 10) {
            return;
        }
        AccessibilityManager manager = AccessibilityManager.getInstance(this.mContext);
        if (manager.isEnabled() && manager.isTouchExplorationEnabled()) {
            return;
        }
        View view = this.mView;
        if (view == null) {
            Slog.d(this.mTag, "maybeUpdateTooltip called after view was removed");
        } else {
            view.dispatchTooltipHoverEvent(event);
        }
    }

    private View getFocusedViewOrNull() {
        View view = this.mView;
        if (view != null) {
            return view.findFocus();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class SyntheticInputStage extends InputStage {
        private final SyntheticJoystickHandler mJoystick;
        private final SyntheticKeyboardHandler mKeyboard;
        private final SyntheticTouchNavigationHandler mTouchNavigation;
        private final SyntheticTrackballHandler mTrackball;

        public SyntheticInputStage() {
            super(null);
            this.mTrackball = new SyntheticTrackballHandler();
            this.mJoystick = new SyntheticJoystickHandler();
            this.mTouchNavigation = new SyntheticTouchNavigationHandler();
            this.mKeyboard = new SyntheticKeyboardHandler();
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected int onProcess(QueuedInputEvent q) {
            q.mFlags |= 16;
            if (q.mEvent instanceof MotionEvent) {
                MotionEvent event = (MotionEvent) q.mEvent;
                int source = event.getSource();
                if ((source & 4) != 0) {
                    this.mTrackball.process(event);
                    return 1;
                } else if ((source & 16) != 0) {
                    this.mJoystick.process(event);
                    return 1;
                } else if ((source & 2097152) == 2097152) {
                    this.mTouchNavigation.process(event);
                    return 1;
                } else {
                    return 0;
                }
            } else if ((q.mFlags & 32) != 0) {
                this.mKeyboard.process((KeyEvent) q.mEvent);
                return 1;
            } else {
                return 0;
            }
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void onDeliverToNext(QueuedInputEvent q) {
            if ((q.mFlags & 16) == 0 && (q.mEvent instanceof MotionEvent)) {
                MotionEvent event = (MotionEvent) q.mEvent;
                int source = event.getSource();
                if ((source & 4) != 0) {
                    this.mTrackball.cancel();
                } else if ((source & 16) != 0) {
                    this.mJoystick.cancel();
                } else if ((source & 2097152) == 2097152) {
                    this.mTouchNavigation.cancel(event);
                }
            }
            super.onDeliverToNext(q);
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void onWindowFocusChanged(boolean hasWindowFocus) {
            if (!hasWindowFocus) {
                this.mJoystick.cancel();
            }
        }

        @Override // android.view.ViewRootImpl.InputStage
        protected void onDetachedFromWindow() {
            this.mJoystick.cancel();
        }
    }

    /* loaded from: classes3.dex */
    final class SyntheticTrackballHandler {
        private long mLastTime;
        private final TrackballAxis mX = new TrackballAxis();
        private final TrackballAxis mY = new TrackballAxis();

        SyntheticTrackballHandler() {
        }

        public void process(MotionEvent event) {
            long curTime;
            int keycode;
            float accel;
            String str;
            int keycode2;
            long curTime2;
            String str2;
            long curTime3 = SystemClock.uptimeMillis();
            if (this.mLastTime + 250 < curTime3) {
                this.mX.reset(0);
                this.mY.reset(0);
                this.mLastTime = curTime3;
            }
            int action = event.getAction();
            int metaState = event.getMetaState();
            switch (action) {
                case 0:
                    curTime = curTime3;
                    this.mX.reset(2);
                    this.mY.reset(2);
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 0, 23, 0, metaState, -1, 0, 1024, 257));
                    break;
                case 1:
                    this.mX.reset(2);
                    this.mY.reset(2);
                    curTime = curTime3;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime3, curTime3, 1, 23, 0, metaState, -1, 0, 1024, 257));
                    break;
                default:
                    curTime = curTime3;
                    break;
            }
            if (ViewRootImpl.DEBUG_TRACKBALL) {
                Log.v(ViewRootImpl.this.mTag, "TB X=" + this.mX.position + " step=" + this.mX.step + " dir=" + this.mX.dir + " acc=" + this.mX.acceleration + " move=" + event.getX() + " / Y=" + this.mY.position + " step=" + this.mY.step + " dir=" + this.mY.dir + " acc=" + this.mY.acceleration + " move=" + event.getY());
            }
            float xOff = this.mX.collect(event.getX(), event.getEventTime(), GnssSignalType.CODE_TYPE_X);
            float yOff = this.mY.collect(event.getY(), event.getEventTime(), GnssSignalType.CODE_TYPE_Y);
            int movement = 0;
            if (xOff > yOff) {
                movement = this.mX.generate();
                if (movement == 0) {
                    keycode = 0;
                    accel = 1.0f;
                } else {
                    int keycode3 = movement > 0 ? 22 : 21;
                    float accel2 = this.mX.acceleration;
                    this.mY.reset(2);
                    keycode = keycode3;
                    accel = accel2;
                }
            } else if (yOff <= 0.0f) {
                keycode = 0;
                accel = 1.0f;
            } else {
                movement = this.mY.generate();
                if (movement == 0) {
                    keycode = 0;
                    accel = 1.0f;
                } else {
                    int keycode4 = movement > 0 ? 20 : 19;
                    float accel3 = this.mY.acceleration;
                    this.mX.reset(2);
                    keycode = keycode4;
                    accel = accel3;
                }
            }
            if (keycode != 0) {
                if (movement < 0) {
                    movement = -movement;
                }
                int accelMovement = (int) (movement * accel);
                if (ViewRootImpl.DEBUG_TRACKBALL) {
                    Log.v(ViewRootImpl.this.mTag, "Move: movement=" + movement + " accelMovement=" + accelMovement + " accel=" + accel);
                }
                if (accelMovement <= movement) {
                    str = "Delivering fake DPAD: ";
                    keycode2 = keycode;
                    curTime2 = curTime;
                } else {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.this.mTag, "Delivering fake DPAD: " + keycode);
                    }
                    int movement2 = movement - 1;
                    int repeatCount = accelMovement - movement2;
                    str = "Delivering fake DPAD: ";
                    keycode2 = keycode;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime, curTime, 2, keycode, repeatCount, metaState, -1, 0, 1024, 257));
                    curTime2 = curTime;
                    movement = movement2;
                }
                while (movement > 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        str2 = str;
                        Log.v(ViewRootImpl.this.mTag, str2 + keycode2);
                    } else {
                        str2 = str;
                    }
                    long curTime4 = SystemClock.uptimeMillis();
                    int i = keycode2;
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime4, curTime4, 0, i, 0, metaState, -1, 0, 1024, 257));
                    ViewRootImpl.this.enqueueInputEvent(new KeyEvent(curTime4, curTime4, 1, i, 0, metaState, -1, 0, 1024, 257));
                    movement--;
                    curTime2 = curTime4;
                    str = str2;
                    keycode2 = keycode2;
                }
                this.mLastTime = curTime2;
            }
        }

        public void cancel() {
            this.mLastTime = -2147483648L;
            if (ViewRootImpl.this.mView != null && ViewRootImpl.this.mAdded) {
                ViewRootImpl.this.ensureTouchMode(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static final class TrackballAxis {
        static final float ACCEL_MOVE_SCALING_FACTOR = 0.025f;
        static final long FAST_MOVE_TIME = 150;
        static final float FIRST_MOVEMENT_THRESHOLD = 0.5f;
        static final float MAX_ACCELERATION = 20.0f;
        static final float SECOND_CUMULATIVE_MOVEMENT_THRESHOLD = 2.0f;
        static final float SUBSEQUENT_INCREMENTAL_MOVEMENT_THRESHOLD = 1.0f;
        int dir;
        int nonAccelMovement;
        float position;
        int step;
        float acceleration = 1.0f;
        long lastMoveTime = 0;

        TrackballAxis() {
        }

        void reset(int _step) {
            this.position = 0.0f;
            this.acceleration = 1.0f;
            this.lastMoveTime = 0L;
            this.step = _step;
            this.dir = 0;
        }

        float collect(float off, long time, String axis) {
            long normTime;
            if (off > 0.0f) {
                normTime = off * 150.0f;
                if (this.dir < 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, axis + " reversed to positive!");
                    }
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = 1;
            } else if (off < 0.0f) {
                normTime = (-off) * 150.0f;
                if (this.dir > 0) {
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, axis + " reversed to negative!");
                    }
                    this.position = 0.0f;
                    this.step = 0;
                    this.acceleration = 1.0f;
                    this.lastMoveTime = 0L;
                }
                this.dir = -1;
            } else {
                normTime = 0;
            }
            if (normTime > 0) {
                long delta = time - this.lastMoveTime;
                this.lastMoveTime = time;
                float acc = this.acceleration;
                if (delta < normTime) {
                    float scale = ((float) (normTime - delta)) * ACCEL_MOVE_SCALING_FACTOR;
                    if (scale > 1.0f) {
                        acc *= scale;
                    }
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, axis + " accelerate: off=" + off + " normTime=" + normTime + " delta=" + delta + " scale=" + scale + " acc=" + acc);
                    }
                    float f = MAX_ACCELERATION;
                    if (acc < MAX_ACCELERATION) {
                        f = acc;
                    }
                    this.acceleration = f;
                } else {
                    float scale2 = ((float) (delta - normTime)) * ACCEL_MOVE_SCALING_FACTOR;
                    if (scale2 > 1.0f) {
                        acc /= scale2;
                    }
                    if (ViewRootImpl.DEBUG_TRACKBALL) {
                        Log.v(ViewRootImpl.TAG, axis + " deccelerate: off=" + off + " normTime=" + normTime + " delta=" + delta + " scale=" + scale2 + " acc=" + acc);
                    }
                    this.acceleration = acc > 1.0f ? acc : 1.0f;
                }
            }
            float f2 = this.position + off;
            this.position = f2;
            return Math.abs(f2);
        }

        int generate() {
            int movement = 0;
            this.nonAccelMovement = 0;
            while (true) {
                float f = this.position;
                int dir = f >= 0.0f ? 1 : -1;
                switch (this.step) {
                    case 0:
                        if (Math.abs(f) < 0.5f) {
                            return movement;
                        }
                        movement += dir;
                        this.nonAccelMovement += dir;
                        this.step = 1;
                        break;
                    case 1:
                        if (Math.abs(f) < SECOND_CUMULATIVE_MOVEMENT_THRESHOLD) {
                            return movement;
                        }
                        movement += dir;
                        this.nonAccelMovement += dir;
                        this.position -= dir * SECOND_CUMULATIVE_MOVEMENT_THRESHOLD;
                        this.step = 2;
                        break;
                    default:
                        if (Math.abs(f) < 1.0f) {
                            return movement;
                        }
                        movement += dir;
                        this.position -= dir * 1.0f;
                        float acc = this.acceleration * 1.1f;
                        this.acceleration = acc < MAX_ACCELERATION ? acc : this.acceleration;
                        break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class SyntheticJoystickHandler extends Handler {
        private static final int MSG_ENQUEUE_X_AXIS_KEY_REPEAT = 1;
        private static final int MSG_ENQUEUE_Y_AXIS_KEY_REPEAT = 2;
        private final SparseArray<KeyEvent> mDeviceKeyEvents;
        private final JoystickAxesState mJoystickAxesState;

        public SyntheticJoystickHandler() {
            super(true);
            this.mJoystickAxesState = new JoystickAxesState();
            this.mDeviceKeyEvents = new SparseArray<>();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                    if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus) {
                        KeyEvent oldEvent = (KeyEvent) msg.obj;
                        KeyEvent e = KeyEvent.changeTimeRepeat(oldEvent, SystemClock.uptimeMillis(), oldEvent.getRepeatCount() + 1);
                        ViewRootImpl.this.enqueueInputEvent(e);
                        Message m = obtainMessage(msg.what, e);
                        m.setAsynchronous(true);
                        sendMessageDelayed(m, ViewConfiguration.getKeyRepeatDelay());
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public void process(MotionEvent event) {
            switch (event.getActionMasked()) {
                case 2:
                    update(event);
                    return;
                case 3:
                    cancel();
                    return;
                default:
                    Log.w(ViewRootImpl.this.mTag, "Unexpected action: " + event.getActionMasked());
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void cancel() {
            removeMessages(1);
            removeMessages(2);
            for (int i = 0; i < this.mDeviceKeyEvents.size(); i++) {
                KeyEvent keyEvent = this.mDeviceKeyEvents.valueAt(i);
                if (keyEvent != null) {
                    ViewRootImpl.this.enqueueInputEvent(KeyEvent.changeTimeRepeat(keyEvent, SystemClock.uptimeMillis(), 0));
                }
            }
            this.mDeviceKeyEvents.clear();
            this.mJoystickAxesState.resetState();
        }

        private void update(MotionEvent event) {
            int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                long time = event.getHistoricalEventTime(h);
                this.mJoystickAxesState.updateStateForAxis(event, time, 0, event.getHistoricalAxisValue(0, 0, h));
                this.mJoystickAxesState.updateStateForAxis(event, time, 1, event.getHistoricalAxisValue(1, 0, h));
                this.mJoystickAxesState.updateStateForAxis(event, time, 15, event.getHistoricalAxisValue(15, 0, h));
                this.mJoystickAxesState.updateStateForAxis(event, time, 16, event.getHistoricalAxisValue(16, 0, h));
            }
            long time2 = event.getEventTime();
            this.mJoystickAxesState.updateStateForAxis(event, time2, 0, event.getAxisValue(0));
            this.mJoystickAxesState.updateStateForAxis(event, time2, 1, event.getAxisValue(1));
            this.mJoystickAxesState.updateStateForAxis(event, time2, 15, event.getAxisValue(15));
            this.mJoystickAxesState.updateStateForAxis(event, time2, 16, event.getAxisValue(16));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes3.dex */
        public final class JoystickAxesState {
            private static final int STATE_DOWN_OR_RIGHT = 1;
            private static final int STATE_NEUTRAL = 0;
            private static final int STATE_UP_OR_LEFT = -1;
            final int[] mAxisStatesHat = {0, 0};
            final int[] mAxisStatesStick = {0, 0};

            JoystickAxesState() {
            }

            void resetState() {
                int[] iArr = this.mAxisStatesHat;
                iArr[0] = 0;
                iArr[1] = 0;
                int[] iArr2 = this.mAxisStatesStick;
                iArr2[0] = 0;
                iArr2[1] = 0;
            }

            void updateStateForAxis(MotionEvent event, long time, int axis, float value) {
                int axisStateIndex;
                int repeatMessage;
                int currentState;
                int keyCode;
                if (isXAxis(axis)) {
                    axisStateIndex = 0;
                    repeatMessage = 1;
                } else if (!isYAxis(axis)) {
                    Log.e(ViewRootImpl.this.mTag, "Unexpected axis " + axis + " in updateStateForAxis!");
                    return;
                } else {
                    axisStateIndex = 1;
                    repeatMessage = 2;
                }
                int newState = joystickAxisValueToState(value);
                if (axis == 0 || axis == 1) {
                    currentState = this.mAxisStatesStick[axisStateIndex];
                } else {
                    currentState = this.mAxisStatesHat[axisStateIndex];
                }
                if (currentState == newState) {
                    return;
                }
                int metaState = event.getMetaState();
                int deviceId = event.getDeviceId();
                int source = event.getSource();
                if (currentState == 1 || currentState == -1) {
                    int keyCode2 = joystickAxisAndStateToKeycode(axis, currentState);
                    if (keyCode2 != 0) {
                        ViewRootImpl.this.enqueueInputEvent(new KeyEvent(time, time, 1, keyCode2, 0, metaState, deviceId, 0, 1024, source));
                        deviceId = deviceId;
                        SyntheticJoystickHandler.this.mDeviceKeyEvents.put(deviceId, null);
                    }
                    SyntheticJoystickHandler.this.removeMessages(repeatMessage);
                }
                if ((newState == 1 || newState == -1) && (keyCode = joystickAxisAndStateToKeycode(axis, newState)) != 0) {
                    int deviceId2 = deviceId;
                    KeyEvent keyEvent = new KeyEvent(time, time, 0, keyCode, 0, metaState, deviceId2, 0, 1024, source);
                    ViewRootImpl.this.enqueueInputEvent(keyEvent);
                    Message m = SyntheticJoystickHandler.this.obtainMessage(repeatMessage, keyEvent);
                    m.setAsynchronous(true);
                    SyntheticJoystickHandler.this.sendMessageDelayed(m, ViewConfiguration.getKeyRepeatTimeout());
                    SyntheticJoystickHandler.this.mDeviceKeyEvents.put(deviceId2, new KeyEvent(time, time, 1, keyCode, 0, metaState, deviceId2, 0, 1056, source));
                }
                if (axis == 0 || axis == 1) {
                    this.mAxisStatesStick[axisStateIndex] = newState;
                } else {
                    this.mAxisStatesHat[axisStateIndex] = newState;
                }
            }

            private boolean isXAxis(int axis) {
                return axis == 0 || axis == 15;
            }

            private boolean isYAxis(int axis) {
                return axis == 1 || axis == 16;
            }

            private int joystickAxisAndStateToKeycode(int axis, int state) {
                if (isXAxis(axis) && state == -1) {
                    return 21;
                }
                if (isXAxis(axis) && state == 1) {
                    return 22;
                }
                if (isYAxis(axis) && state == -1) {
                    return 19;
                }
                if (isYAxis(axis) && state == 1) {
                    return 20;
                }
                Log.e(ViewRootImpl.this.mTag, "Unknown axis " + axis + " or direction " + state);
                return 0;
            }

            private int joystickAxisValueToState(float value) {
                if (value >= 0.5f) {
                    return 1;
                }
                if (value <= -0.5f) {
                    return -1;
                }
                return 0;
            }
        }
    }

    /* loaded from: classes3.dex */
    final class SyntheticTouchNavigationHandler extends Handler {
        private static final float DEFAULT_HEIGHT_MILLIMETERS = 48.0f;
        private static final float DEFAULT_WIDTH_MILLIMETERS = 48.0f;
        private static final float FLING_TICK_DECAY = 0.8f;
        private static final boolean LOCAL_DEBUG = false;
        private static final String LOCAL_TAG = "SyntheticTouchNavigationHandler";
        private static final float MAX_FLING_VELOCITY_TICKS_PER_SECOND = 20.0f;
        private static final float MIN_FLING_VELOCITY_TICKS_PER_SECOND = 6.0f;
        private static final int TICK_DISTANCE_MILLIMETERS = 12;
        private float mAccumulatedX;
        private float mAccumulatedY;
        private int mActivePointerId;
        private float mConfigMaxFlingVelocity;
        private float mConfigMinFlingVelocity;
        private float mConfigTickDistance;
        private boolean mConsumedMovement;
        private int mCurrentDeviceId;
        private boolean mCurrentDeviceSupported;
        private int mCurrentSource;
        private final Runnable mFlingRunnable;
        private float mFlingVelocity;
        private boolean mFlinging;
        private float mLastX;
        private float mLastY;
        private int mPendingKeyCode;
        private long mPendingKeyDownTime;
        private int mPendingKeyMetaState;
        private int mPendingKeyRepeatCount;
        private float mStartX;
        private float mStartY;
        private VelocityTracker mVelocityTracker;

        public SyntheticTouchNavigationHandler() {
            super(true);
            this.mCurrentDeviceId = -1;
            this.mActivePointerId = -1;
            this.mPendingKeyCode = 0;
            this.mFlingRunnable = new Runnable() { // from class: android.view.ViewRootImpl.SyntheticTouchNavigationHandler.1
                @Override // java.lang.Runnable
                public void run() {
                    long time = SystemClock.uptimeMillis();
                    SyntheticTouchNavigationHandler syntheticTouchNavigationHandler = SyntheticTouchNavigationHandler.this;
                    syntheticTouchNavigationHandler.sendKeyDownOrRepeat(time, syntheticTouchNavigationHandler.mPendingKeyCode, SyntheticTouchNavigationHandler.this.mPendingKeyMetaState);
                    SyntheticTouchNavigationHandler.this.mFlingVelocity *= 0.8f;
                    if (!SyntheticTouchNavigationHandler.this.postFling(time)) {
                        SyntheticTouchNavigationHandler.this.mFlinging = false;
                        SyntheticTouchNavigationHandler.this.finishKeys(time);
                    }
                }
            };
        }

        public void process(MotionEvent event) {
            long time = event.getEventTime();
            int deviceId = event.getDeviceId();
            int source = event.getSource();
            if (this.mCurrentDeviceId != deviceId || this.mCurrentSource != source) {
                finishKeys(time);
                finishTracking(time);
                this.mCurrentDeviceId = deviceId;
                this.mCurrentSource = source;
                this.mCurrentDeviceSupported = false;
                InputDevice device = event.getDevice();
                if (device != null) {
                    InputDevice.MotionRange xRange = device.getMotionRange(0);
                    InputDevice.MotionRange yRange = device.getMotionRange(1);
                    if (xRange != null && yRange != null) {
                        this.mCurrentDeviceSupported = true;
                        float xRes = xRange.getResolution();
                        if (xRes <= 0.0f) {
                            xRes = xRange.getRange() / 48.0f;
                        }
                        float yRes = yRange.getResolution();
                        if (yRes <= 0.0f) {
                            yRes = yRange.getRange() / 48.0f;
                        }
                        float nominalRes = (xRes + yRes) * 0.5f;
                        float f = 12.0f * nominalRes;
                        this.mConfigTickDistance = f;
                        this.mConfigMinFlingVelocity = f * MIN_FLING_VELOCITY_TICKS_PER_SECOND;
                        this.mConfigMaxFlingVelocity = f * MAX_FLING_VELOCITY_TICKS_PER_SECOND;
                    }
                }
            }
            if (!this.mCurrentDeviceSupported) {
                return;
            }
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    boolean caughtFling = this.mFlinging;
                    finishKeys(time);
                    finishTracking(time);
                    this.mActivePointerId = event.getPointerId(0);
                    VelocityTracker obtain = VelocityTracker.obtain();
                    this.mVelocityTracker = obtain;
                    obtain.addMovement(event);
                    this.mStartX = event.getX();
                    float y = event.getY();
                    this.mStartY = y;
                    this.mLastX = this.mStartX;
                    this.mLastY = y;
                    this.mAccumulatedX = 0.0f;
                    this.mAccumulatedY = 0.0f;
                    this.mConsumedMovement = caughtFling;
                    return;
                case 1:
                case 2:
                    int i = this.mActivePointerId;
                    if (i >= 0) {
                        int index = event.findPointerIndex(i);
                        if (index < 0) {
                            finishKeys(time);
                            finishTracking(time);
                            return;
                        }
                        this.mVelocityTracker.addMovement(event);
                        float x = event.getX(index);
                        float y2 = event.getY(index);
                        this.mAccumulatedX += x - this.mLastX;
                        this.mAccumulatedY += y2 - this.mLastY;
                        this.mLastX = x;
                        this.mLastY = y2;
                        int metaState = event.getMetaState();
                        consumeAccumulatedMovement(time, metaState);
                        if (action == 1) {
                            if (this.mConsumedMovement && this.mPendingKeyCode != 0) {
                                this.mVelocityTracker.computeCurrentVelocity(1000, this.mConfigMaxFlingVelocity);
                                float vx = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                                float vy = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                                if (!startFling(time, vx, vy)) {
                                    finishKeys(time);
                                }
                            }
                            finishTracking(time);
                            return;
                        }
                        return;
                    }
                    return;
                case 3:
                    finishKeys(time);
                    finishTracking(time);
                    return;
                default:
                    return;
            }
        }

        public void cancel(MotionEvent event) {
            if (this.mCurrentDeviceId == event.getDeviceId() && this.mCurrentSource == event.getSource()) {
                long time = event.getEventTime();
                finishKeys(time);
                finishTracking(time);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void finishKeys(long time) {
            cancelFling();
            sendKeyUp(time);
        }

        private void finishTracking(long time) {
            if (this.mActivePointerId >= 0) {
                this.mActivePointerId = -1;
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
        }

        private void consumeAccumulatedMovement(long time, int metaState) {
            float absX = Math.abs(this.mAccumulatedX);
            float absY = Math.abs(this.mAccumulatedY);
            if (absX >= absY) {
                if (absX >= this.mConfigTickDistance) {
                    this.mAccumulatedX = consumeAccumulatedMovement(time, metaState, this.mAccumulatedX, 21, 22);
                    this.mAccumulatedY = 0.0f;
                    this.mConsumedMovement = true;
                }
            } else if (absY >= this.mConfigTickDistance) {
                this.mAccumulatedY = consumeAccumulatedMovement(time, metaState, this.mAccumulatedY, 19, 20);
                this.mAccumulatedX = 0.0f;
                this.mConsumedMovement = true;
            }
        }

        private float consumeAccumulatedMovement(long time, int metaState, float accumulator, int negativeKeyCode, int positiveKeyCode) {
            while (accumulator <= (-this.mConfigTickDistance)) {
                sendKeyDownOrRepeat(time, negativeKeyCode, metaState);
                accumulator += this.mConfigTickDistance;
            }
            while (accumulator >= this.mConfigTickDistance) {
                sendKeyDownOrRepeat(time, positiveKeyCode, metaState);
                accumulator -= this.mConfigTickDistance;
            }
            return accumulator;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void sendKeyDownOrRepeat(long time, int keyCode, int metaState) {
            if (this.mPendingKeyCode == keyCode) {
                this.mPendingKeyRepeatCount++;
            } else {
                sendKeyUp(time);
                this.mPendingKeyDownTime = time;
                this.mPendingKeyCode = keyCode;
                this.mPendingKeyRepeatCount = 0;
            }
            this.mPendingKeyMetaState = metaState;
            ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, time, 0, this.mPendingKeyCode, this.mPendingKeyRepeatCount, this.mPendingKeyMetaState, this.mCurrentDeviceId, 1024, this.mCurrentSource));
        }

        private void sendKeyUp(long time) {
            if (this.mPendingKeyCode != 0) {
                ViewRootImpl.this.enqueueInputEvent(new KeyEvent(this.mPendingKeyDownTime, time, 1, this.mPendingKeyCode, 0, this.mPendingKeyMetaState, this.mCurrentDeviceId, 0, 1024, this.mCurrentSource));
                this.mPendingKeyCode = 0;
            }
        }

        private boolean startFling(long time, float vx, float vy) {
            switch (this.mPendingKeyCode) {
                case 19:
                    if ((-vy) >= this.mConfigMinFlingVelocity && Math.abs(vx) < this.mConfigMinFlingVelocity) {
                        this.mFlingVelocity = -vy;
                        break;
                    } else {
                        return false;
                    }
                case 20:
                    if (vy >= this.mConfigMinFlingVelocity && Math.abs(vx) < this.mConfigMinFlingVelocity) {
                        this.mFlingVelocity = vy;
                        break;
                    } else {
                        return false;
                    }
                case 21:
                    if ((-vx) >= this.mConfigMinFlingVelocity && Math.abs(vy) < this.mConfigMinFlingVelocity) {
                        this.mFlingVelocity = -vx;
                        break;
                    } else {
                        return false;
                    }
                    break;
                case 22:
                    if (vx >= this.mConfigMinFlingVelocity && Math.abs(vy) < this.mConfigMinFlingVelocity) {
                        this.mFlingVelocity = vx;
                        break;
                    } else {
                        return false;
                    }
            }
            boolean postFling = postFling(time);
            this.mFlinging = postFling;
            return postFling;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean postFling(long time) {
            float f = this.mFlingVelocity;
            if (f >= this.mConfigMinFlingVelocity) {
                long delay = (this.mConfigTickDistance / f) * 1000.0f;
                postAtTime(this.mFlingRunnable, time + delay);
                return true;
            }
            return false;
        }

        private void cancelFling() {
            if (this.mFlinging) {
                removeCallbacks(this.mFlingRunnable);
                this.mFlinging = false;
            }
        }
    }

    /* loaded from: classes3.dex */
    final class SyntheticKeyboardHandler {
        SyntheticKeyboardHandler() {
        }

        public void process(KeyEvent event) {
            if ((event.getFlags() & 1024) != 0) {
                return;
            }
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            int keyCode = event.getKeyCode();
            int metaState = event.getMetaState();
            KeyCharacterMap.FallbackAction fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            if (fallbackAction != null) {
                int flags = event.getFlags() | 1024;
                KeyEvent fallbackEvent = KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), fallbackAction.keyCode, event.getRepeatCount(), fallbackAction.metaState, event.getDeviceId(), event.getScanCode(), flags, event.getSource(), null);
                fallbackAction.recycle();
                ViewRootImpl.this.enqueueInputEvent(fallbackEvent);
            }
        }
    }

    private static boolean isNavigationKey(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 61:
            case 62:
            case 66:
            case 92:
            case 93:
            case 122:
            case 123:
                return true;
            default:
                return false;
        }
    }

    private static boolean isTypingKey(KeyEvent keyEvent) {
        return keyEvent.getUnicodeChar() > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkForLeavingTouchModeAndConsume(KeyEvent event) {
        if (this.mAttachInfo.mInTouchMode) {
            int action = event.getAction();
            if ((action == 0 || action == 2) && (event.getFlags() & 4) == 0) {
                if (event.hasNoModifiers() && isNavigationKey(event)) {
                    return ensureTouchMode(false);
                }
                if (isTypingKey(event)) {
                    ensureTouchMode(false);
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLocalDragState(Object obj) {
        this.mLocalDragState = obj;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDragEvent(DragEvent event) {
        if (this.mView != null && this.mAdded) {
            int what = event.mAction;
            if (what == 1) {
                this.mCurrentDragView = null;
                this.mDragDescription = event.mClipDescription;
                View view = this.mStartedDragViewForA11y;
                if (view != null) {
                    view.sendWindowContentChangedAccessibilityEvent(128);
                }
            } else {
                if (what == 4) {
                    this.mDragDescription = null;
                }
                event.mClipDescription = this.mDragDescription;
            }
            if (what == 6) {
                if (View.sCascadedDragDrop) {
                    this.mView.dispatchDragEnterExitInPreN(event);
                }
                setDragFocus(null, event);
            } else {
                if (what == 2 || what == 3) {
                    this.mDragPoint.set(event.mX, event.mY);
                    CompatibilityInfo.Translator translator = this.mTranslator;
                    if (translator != null) {
                        translator.translatePointInScreenToAppWindow(this.mDragPoint);
                    }
                    int i = this.mCurScrollY;
                    if (i != 0) {
                        this.mDragPoint.offset(0.0f, i);
                    }
                    event.mX = this.mDragPoint.x;
                    event.mY = this.mDragPoint.y;
                }
                View prevDragView = this.mCurrentDragView;
                if (what == 3 && event.mClipData != null) {
                    event.mClipData.prepareToEnterProcess(this.mView.getContext().getAttributionSource());
                }
                boolean result = this.mView.dispatchDragEvent(event);
                if (what == 2 && !event.mEventHandlerWasCalled) {
                    setDragFocus(null, event);
                }
                if (prevDragView != this.mCurrentDragView) {
                    if (prevDragView != null) {
                        try {
                            this.mWindowSession.dragRecipientExited(this.mWindow);
                        } catch (RemoteException e) {
                            Slog.e(this.mTag, "Unable to note drag target change");
                        }
                    }
                    if (this.mCurrentDragView != null) {
                        this.mWindowSession.dragRecipientEntered(this.mWindow);
                    }
                }
                if (what == 3) {
                    try {
                        Log.i(this.mTag, "Reporting drop result: " + result);
                        this.mWindowSession.reportDropResult(this.mWindow, result);
                    } catch (RemoteException e2) {
                        Log.e(this.mTag, "Unable to report drop result");
                    }
                }
                if (what == 4) {
                    if (this.mStartedDragViewForA11y != null) {
                        if (!event.getResult()) {
                            this.mStartedDragViewForA11y.sendWindowContentChangedAccessibilityEvent(512);
                        }
                        this.mStartedDragViewForA11y.setAccessibilityDragStarted(false);
                    }
                    this.mStartedDragViewForA11y = null;
                    this.mCurrentDragView = null;
                    setLocalDragState(null);
                    this.mAttachInfo.mDragToken = null;
                    if (this.mAttachInfo.mDragSurface != null) {
                        this.mAttachInfo.mDragSurface.release();
                        this.mAttachInfo.mDragSurface = null;
                    }
                }
            }
        } else if (event != null && event.getAction() == 3 && this.mWindowSession != null) {
            try {
                Log.i(this.mTag, "View is null also report drop result false, packageName=" + this.mBasePackageName);
                this.mWindowSession.reportDropResult(this.mWindow, false);
            } catch (RemoteException e3) {
                Log.e(this.mTag, "Unable to report drop result");
            }
        }
        event.recycle();
    }

    public void onWindowTitleChanged() {
        this.mAttachInfo.mForceReportNewAttributes = true;
    }

    public void handleDispatchWindowShown() {
        this.mAttachInfo.mTreeObserver.dispatchOnWindowShown();
    }

    public void handleRequestKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        Bundle data = new Bundle();
        ArrayList<KeyboardShortcutGroup> list = new ArrayList<>();
        View view = this.mView;
        if (view != null) {
            view.requestKeyboardShortcuts(list, deviceId);
        }
        data.putParcelableArrayList(WindowManager.PARCEL_KEY_SHORTCUTS_ARRAY, list);
        try {
            receiver.send(0, data);
        } catch (RemoteException e) {
        }
    }

    public void getLastTouchPoint(Point outLocation) {
        outLocation.x = (int) this.mLastTouchPoint.x;
        outLocation.y = (int) this.mLastTouchPoint.y;
    }

    public int getLastTouchSource() {
        return this.mLastTouchSource;
    }

    public void setDragFocus(View newDragTarget, DragEvent event) {
        if (this.mCurrentDragView != newDragTarget && !View.sCascadedDragDrop) {
            float tx = event.mX;
            float ty = event.mY;
            int action = event.mAction;
            ClipData td = event.mClipData;
            event.mX = 0.0f;
            event.mY = 0.0f;
            event.mClipData = null;
            if (this.mCurrentDragView != null) {
                event.mAction = 6;
                this.mCurrentDragView.callDragEventHandler(event);
            }
            if (newDragTarget != null) {
                event.mAction = 5;
                newDragTarget.callDragEventHandler(event);
            }
            event.mAction = action;
            event.mX = tx;
            event.mY = ty;
            event.mClipData = td;
        }
        this.mCurrentDragView = newDragTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDragStartedViewForAccessibility(View view) {
        if (this.mStartedDragViewForA11y == null) {
            this.mStartedDragViewForA11y = view;
        }
    }

    private AudioManager getAudioManager() {
        View view = this.mView;
        if (view == null) {
            throw new IllegalStateException("getAudioManager called when there is no mView");
        }
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) view.getContext().getSystemService("audio");
        }
        return this.mAudioManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public AutofillManager getAutofillManager() {
        View view = this.mView;
        if (view instanceof ViewGroup) {
            ViewGroup decorView = (ViewGroup) view;
            if (decorView.getChildCount() > 0) {
                return (AutofillManager) decorView.getChildAt(0).getContext().getSystemService(AutofillManager.class);
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAutofillUiShowing() {
        AutofillManager afm = getAutofillManager();
        if (afm == null) {
            return false;
        }
        return afm.isAutofillUiShowing();
    }

    public AccessibilityInteractionController getAccessibilityInteractionController() {
        if (this.mView == null) {
            throw new IllegalStateException("getAccessibilityInteractionController called when there is no mView");
        }
        if (this.mAccessibilityInteractionController == null) {
            this.mAccessibilityInteractionController = new AccessibilityInteractionController(this);
        }
        return this.mAccessibilityInteractionController;
    }

    private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, boolean insetsPending) throws RemoteException {
        boolean restore;
        int requestedHeight;
        int requestedWidth;
        String str;
        String str2;
        boolean z;
        String str3;
        int relayoutResult;
        WindowConfiguration winConfig;
        boolean z2;
        WindowConfiguration winConfig2;
        WindowConfiguration winConfig3;
        this.mRelayoutRequested = true;
        float appScale = this.mAttachInfo.mApplicationScale;
        if (params != null && this.mTranslator != null) {
            params.backup();
            this.mTranslator.translateWindowLayout(params);
            restore = true;
        } else {
            restore = false;
        }
        if (params != null) {
            if (DBG) {
                Log.d(this.mTag, "WindowLayout in layoutWindow:" + params);
            }
            if (DEBUG_LAYOUT) {
                Log.d(this.mTag, ">>>>>> CALLING relayoutW+ " + this.mWindow + ", params = " + params + ",viewVisibility = " + viewVisibility + ", insetsPending = " + insetsPending + ", appScale = " + appScale + ", mWinFrame = " + this.mWinFrame + ", mSurface = " + this.mSurface + ",valid = " + this.mSurface.isValid() + ", mOrigWindowType = " + this.mOrigWindowType + ",this = " + this);
            }
            if (this.mOrigWindowType != params.type && this.mTargetSdkVersion < 14) {
                Slog.w(this.mTag, "Window type can not be changed after the window is added; ignoring change of " + this.mView);
                params.type = this.mOrigWindowType;
            }
        }
        int requestedWidth2 = (int) ((this.mView.getMeasuredWidth() * appScale) + 0.5f);
        int requestedHeight2 = (int) ((this.mView.getMeasuredHeight() * appScale) + 0.5f);
        WindowConfiguration winConfig4 = getConfiguration().windowConfiguration;
        if (LOCAL_LAYOUT) {
            if (this.mFirst || viewVisibility != this.mViewVisibility) {
                winConfig2 = winConfig4;
                requestedHeight = requestedHeight2;
                requestedWidth = requestedWidth2;
                int relayoutResult2 = this.mWindowSession.updateVisibility(this.mWindow, params, viewVisibility, this.mPendingMergedConfiguration, this.mSurfaceControl, this.mTempInsets, this.mTempControls);
                CompatibilityInfo.Translator translator = this.mTranslator;
                if (translator != null) {
                    translator.translateInsetsStateInScreenToAppWindow(this.mTempInsets);
                    this.mTranslator.translateSourceControlsInScreenToAppWindow(this.mTempControls);
                }
                this.mInsetsController.onStateChanged(this.mTempInsets);
                this.mInsetsController.onControlsChanged(this.mTempControls);
                this.mPendingAlwaysConsumeSystemBars = (relayoutResult2 & 8) != 0;
                relayoutResult = relayoutResult2;
            } else {
                winConfig2 = winConfig4;
                requestedHeight = requestedHeight2;
                requestedWidth = requestedWidth2;
                relayoutResult = 0;
            }
            InsetsState state = this.mInsetsController.getState();
            Rect displayCutoutSafe = this.mTempRect;
            state.getDisplayCutoutSafe(displayCutoutSafe);
            if (this.mWindowAttributes.type != 3) {
                winConfig3 = winConfig2;
            } else {
                winConfig3 = this.mPendingMergedConfiguration.getMergedConfiguration().windowConfiguration;
            }
            this.mWindowLayout.computeFrames(this.mWindowAttributes, state, displayCutoutSafe, winConfig3.getBounds(), winConfig3.getWindowingMode(), requestedWidth, requestedHeight, this.mInsetsController.getRequestedVisibilities(), getAttachedWindowFrame(), 1.0f, this.mTmpFrames);
            this.mWindowSession.updateLayout(this.mWindow, params, insetsPending ? 1 : 0, this.mTmpFrames, requestedWidth, requestedHeight);
            str3 = ", mWinFrame = ";
            str2 = ", mSurface = ";
            str = ",valid = ";
            winConfig = winConfig3;
            z = true;
        } else {
            requestedHeight = requestedHeight2;
            requestedWidth = requestedWidth2;
            long lastDisplayPhysicalId = this.mPendingMergedConfiguration.getMergedConfiguration().windowConfiguration.getDisplayPhysicalId();
            str = ",valid = ";
            str2 = ", mSurface = ";
            z = true;
            str3 = ", mWinFrame = ";
            relayoutResult = this.mWindowSession.relayout(this.mWindow, params, requestedWidth, requestedHeight, viewVisibility, insetsPending ? 1 : 0, this.mTmpFrames, this.mPendingMergedConfiguration, this.mSurfaceControl, this.mTempInsets, this.mTempControls, this.mRelayoutBundle);
            this.mTranFoldInnerCustody.handleRelayout(lastDisplayPhysicalId);
            int maybeSyncSeqId = this.mRelayoutBundle.getInt("seqid");
            if (maybeSyncSeqId > 0) {
                this.mSyncSeqId = maybeSyncSeqId;
            }
            CompatibilityInfo.Translator translator2 = this.mTranslator;
            if (translator2 != null) {
                translator2.translateRectInScreenToAppWindow(this.mTmpFrames.frame);
                this.mTranslator.translateRectInScreenToAppWindow(this.mTmpFrames.displayFrame);
                this.mTranslator.translateInsetsStateInScreenToAppWindow(this.mTempInsets);
                this.mTranslator.translateSourceControlsInScreenToAppWindow(this.mTempControls);
            }
            this.mInsetsController.onStateChanged(this.mTempInsets);
            this.mInsetsController.onControlsChanged(this.mTempControls);
            this.mPendingAlwaysConsumeSystemBars = (relayoutResult & 8) != 0;
            winConfig = winConfig4;
        }
        int transformHint = SurfaceControl.rotationToBufferTransform((this.mDisplayInstallOrientation + this.mDisplay.getRotation()) % 4);
        WindowLayout.computeSurfaceSize(this.mWindowAttributes, winConfig.getMaxBounds(), requestedWidth, requestedHeight, this.mTmpFrames.frame, this.mPendingDragResizing, this.mSurfaceSize);
        boolean transformHintChanged = transformHint != this.mLastTransformHint ? z : false;
        boolean sizeChanged = !this.mLastSurfaceSize.equals(this.mSurfaceSize);
        boolean surfaceControlChanged = (relayoutResult & 2) == 2 ? z : false;
        if (this.mAttachInfo.mThreadedRenderer != null && ((transformHintChanged || sizeChanged || surfaceControlChanged) && this.mAttachInfo.mThreadedRenderer.pause())) {
            this.mDirty.set(0, 0, this.mWidth, this.mHeight);
        }
        this.mLastTransformHint = transformHint;
        this.mSurfaceControl.setTransformHint(transformHint);
        if (this.mAttachInfo.mContentCaptureManager != null) {
            MainContentCaptureSession mainSession = this.mAttachInfo.mContentCaptureManager.getMainContentCaptureSession();
            mainSession.notifyWindowBoundsChanged(mainSession.getId(), getConfiguration().windowConfiguration.getBounds());
        }
        if (this.mSurfaceControl.isValid()) {
            if (!useBLAST()) {
                this.mSurface.copyFrom(this.mSurfaceControl);
            } else {
                updateBlastSurfaceIfNeeded();
            }
            if (this.mAttachInfo.mThreadedRenderer != null) {
                this.mAttachInfo.mThreadedRenderer.setSurfaceControl(this.mSurfaceControl);
                this.mAttachInfo.mThreadedRenderer.setBlastBufferQueue(this.mBlastBufferQueue);
            }
            if (this.mPreviousTransformHint == transformHint) {
                z2 = false;
            } else {
                this.mPreviousTransformHint = transformHint;
                dispatchTransformHintChanged(transformHint);
                z2 = false;
            }
        } else {
            if (this.mAttachInfo.mThreadedRenderer != null && this.mAttachInfo.mThreadedRenderer.pause()) {
                z2 = false;
                this.mDirty.set(0, 0, this.mWidth, this.mHeight);
            } else {
                z2 = false;
            }
            destroySurface();
        }
        this.mPendingAlwaysConsumeSystemBars = (relayoutResult & 8) != 0 ? z : z2;
        if (DEBUG_LAYOUT) {
            Log.d(this.mTag, "<<<<<< BACK FROM relayoutW- : res = " + relayoutResult + str3 + this.mWinFrame + str2 + this.mSurface + str + this.mSurface.isValid() + ",params = " + params + ", this = " + this);
        }
        if (restore) {
            params.restore();
        }
        setFrame(this.mTmpFrames.frame);
        return relayoutResult;
    }

    private void updateOpacity(WindowManager.LayoutParams params, boolean dragResizing, boolean forceUpdate) {
        boolean opaque = false;
        if (!PixelFormat.formatHasAlpha(params.format) && params.surfaceInsets.left == 0 && params.surfaceInsets.top == 0 && params.surfaceInsets.right == 0 && params.surfaceInsets.bottom == 0 && !dragResizing) {
            opaque = true;
        }
        if (!forceUpdate && this.mIsSurfaceOpaque == opaque) {
            return;
        }
        ThreadedRenderer renderer = this.mAttachInfo.mThreadedRenderer;
        if (renderer != null && renderer.rendererOwnsSurfaceControlOpacity()) {
            opaque = renderer.setSurfaceControlOpaque(opaque);
        } else {
            this.mTransaction.setOpaque(this.mSurfaceControl, opaque).apply();
        }
        this.mIsSurfaceOpaque = opaque;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setFrame(Rect frame) {
        Rect rect;
        this.mWinFrame.set(frame);
        WindowConfiguration winConfig = getConfiguration().windowConfiguration;
        Rect rect2 = this.mPendingBackDropFrame;
        if (this.mPendingDragResizing && !winConfig.useWindowFrameForBackdrop()) {
            rect = winConfig.getMaxBounds();
        } else {
            rect = frame;
        }
        rect2.set(rect);
        this.mPendingBackDropFrame.offsetTo(0, 0);
        InsetsController insetsController = this.mInsetsController;
        Rect rect3 = this.mOverrideInsetsFrame;
        if (rect3 == null) {
            rect3 = frame;
        }
        insetsController.onFrameChanged(rect3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOverrideInsetsFrame(Rect frame) {
        Rect rect = new Rect(frame);
        this.mOverrideInsetsFrame = rect;
        this.mInsetsController.onFrameChanged(rect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getDisplayFrame(Rect outFrame) {
        outFrame.set(this.mTmpFrames.displayFrame);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getWindowVisibleDisplayFrame(Rect outFrame) {
        outFrame.set(this.mTmpFrames.displayFrame);
        Rect insets = this.mAttachInfo.mVisibleInsets;
        outFrame.left += insets.left;
        outFrame.top += insets.top;
        outFrame.right -= insets.right;
        outFrame.bottom -= insets.bottom;
    }

    @Override // android.view.View.AttachInfo.Callbacks
    public void playSoundEffect(int effectId) {
        if ((this.mDisplay.getFlags() & 1024) != 0) {
            return;
        }
        checkThread();
        try {
            AudioManager audioManager = getAudioManager();
            if (this.mFastScrollSoundEffectsEnabled && SoundEffectConstants.isNavigationRepeat(effectId)) {
                audioManager.playSoundEffect(SoundEffectConstants.nextNavigationRepeatSoundEffectId());
                return;
            }
            switch (effectId) {
                case 0:
                    audioManager.playSoundEffect(0);
                    return;
                case 1:
                case 5:
                    audioManager.playSoundEffect(3);
                    return;
                case 2:
                case 6:
                    audioManager.playSoundEffect(1);
                    return;
                case 3:
                case 7:
                    audioManager.playSoundEffect(4);
                    return;
                case 4:
                case 8:
                    audioManager.playSoundEffect(2);
                    return;
                default:
                    throw new IllegalArgumentException("unknown effect id " + effectId + " not defined in " + SoundEffectConstants.class.getCanonicalName());
            }
        } catch (IllegalStateException e) {
            Log.e(this.mTag, "FATAL EXCEPTION when attempting to play sound effect: " + e);
            e.printStackTrace();
        }
    }

    @Override // android.view.View.AttachInfo.Callbacks
    public boolean performHapticFeedback(int effectId, boolean always) {
        if ((this.mDisplay.getFlags() & 1024) != 0) {
            return false;
        }
        try {
            return this.mWindowSession.performHapticFeedback(effectId, always);
        } catch (RemoteException e) {
            Log.e(this.mTag, "performHapticFeedback RemoteException happens in " + this, e);
            return false;
        }
    }

    @Override // android.view.ViewParent
    public View focusSearch(View focused, int direction) {
        checkThread();
        if (!(this.mView instanceof ViewGroup)) {
            return null;
        }
        return FocusFinder.getInstance().findNextFocus((ViewGroup) this.mView, focused, direction);
    }

    @Override // android.view.ViewParent
    public View keyboardNavigationClusterSearch(View currentCluster, int direction) {
        checkThread();
        return FocusFinder.getInstance().findNextKeyboardNavigationCluster(this.mView, currentCluster, direction);
    }

    public void debug() {
        this.mView.debug();
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, Objects.toString(this.mView));
        proto.write(1120986464258L, this.mDisplay.getDisplayId());
        proto.write(1133871366147L, this.mAppVisible);
        proto.write(1120986464261L, this.mHeight);
        proto.write(1120986464260L, this.mWidth);
        proto.write(1133871366150L, this.mIsAnimating);
        this.mVisRect.dumpDebug(proto, 1146756268039L);
        proto.write(1133871366152L, this.mIsDrawing);
        proto.write(1133871366153L, this.mAdded);
        this.mWinFrame.dumpDebug(proto, 1146756268042L);
        proto.write(1138166333452L, Objects.toString(this.mLastWindowInsets));
        proto.write(1138166333453L, InputMethodDebug.softInputModeToString(this.mSoftInputMode));
        proto.write(1120986464270L, this.mScrollY);
        proto.write(1120986464271L, this.mCurScrollY);
        proto.write(1133871366160L, this.mRemoved);
        this.mWindowAttributes.dumpDebug(proto, 1146756268049L);
        proto.end(token);
        this.mInsetsController.dumpDebug(proto, 1146756268036L);
        this.mImeFocusController.dumpDebug(proto, 1146756268039L);
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.println(prefix + "ViewRoot:");
        writer.println(innerPrefix + "mAdded=" + this.mAdded);
        writer.println(innerPrefix + "mRemoved=" + this.mRemoved);
        writer.println(innerPrefix + "mStopped=" + this.mStopped);
        writer.println(innerPrefix + "mPausedForTransition=" + this.mPausedForTransition);
        writer.println(innerPrefix + "mConsumeBatchedInputScheduled=" + this.mConsumeBatchedInputScheduled);
        writer.println(innerPrefix + "mConsumeBatchedInputImmediatelyScheduled=" + this.mConsumeBatchedInputImmediatelyScheduled);
        writer.println(innerPrefix + "mPendingInputEventCount=" + this.mPendingInputEventCount);
        writer.println(innerPrefix + "mProcessInputEventsScheduled=" + this.mProcessInputEventsScheduled);
        writer.println(innerPrefix + "mTraversalScheduled=" + this.mTraversalScheduled);
        if (this.mTraversalScheduled) {
            writer.println(innerPrefix + " (barrier=" + this.mTraversalBarrier + NavigationBarInflaterView.KEY_CODE_END);
        }
        writer.println(innerPrefix + "mIsAmbientMode=" + this.mIsAmbientMode);
        writer.println(innerPrefix + "mUnbufferedInputSource=" + Integer.toHexString(this.mUnbufferedInputSource));
        if (this.mAttachInfo != null) {
            writer.print(innerPrefix + "mAttachInfo= ");
            this.mAttachInfo.dump(innerPrefix, writer);
        } else {
            writer.println(innerPrefix + "mAttachInfo=<null>");
        }
        this.mFirstInputStage.dump(innerPrefix, writer);
        WindowInputEventReceiver windowInputEventReceiver = this.mInputEventReceiver;
        if (windowInputEventReceiver != null) {
            windowInputEventReceiver.dump(innerPrefix, writer);
        }
        this.mChoreographer.dump(prefix, writer);
        this.mInsetsController.dump(prefix, writer);
        writer.println(prefix + "View Hierarchy:");
        dumpViewHierarchy(innerPrefix, writer, this.mView);
    }

    private void dumpViewHierarchy(String prefix, PrintWriter writer, View view) {
        ViewGroup grp;
        int N;
        writer.print(prefix);
        if (view == null) {
            writer.println("null");
            return;
        }
        writer.println(view.toString());
        if (!(view instanceof ViewGroup) || (N = (grp = (ViewGroup) view).getChildCount()) <= 0) {
            return;
        }
        String prefix2 = prefix + "  ";
        for (int i = 0; i < N; i++) {
            dumpViewHierarchy(prefix2, writer, grp.getChildAt(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static final class GfxInfo {
        public long renderNodeMemoryAllocated;
        public long renderNodeMemoryUsage;
        public int viewCount;

        /* JADX INFO: Access modifiers changed from: package-private */
        public void add(GfxInfo other) {
            this.viewCount += other.viewCount;
            this.renderNodeMemoryUsage += other.renderNodeMemoryUsage;
            this.renderNodeMemoryAllocated += other.renderNodeMemoryAllocated;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GfxInfo getGfxInfo() {
        GfxInfo info = new GfxInfo();
        View view = this.mView;
        if (view != null) {
            appendGfxInfo(view, info);
        }
        return info;
    }

    private static void computeRenderNodeUsage(RenderNode node, GfxInfo info) {
        if (node == null) {
            return;
        }
        info.renderNodeMemoryUsage += node.computeApproximateMemoryUsage();
        info.renderNodeMemoryAllocated += node.computeApproximateMemoryAllocated();
    }

    private static void appendGfxInfo(View view, GfxInfo info) {
        info.viewCount++;
        computeRenderNodeUsage(view.mRenderNode, info);
        computeRenderNodeUsage(view.mBackgroundRenderNode, info);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                appendGfxInfo(group.getChildAt(i), info);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean die(boolean immediate) {
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.v(this.mTag, "die: immediate = " + immediate + ", mIsInTraversal = " + this.mIsInTraversal + ",mIsDrawing = " + this.mIsDrawing + ",this = " + this, new Throwable());
        }
        if (immediate && !this.mIsInTraversal) {
            doDie();
            return false;
        }
        if (!this.mIsDrawing) {
            destroyHardwareRenderer();
        } else {
            Log.e(this.mTag, "Attempting to destroy the window while drawing!\n  window=" + this + ", title=" + ((Object) this.mWindowAttributes.getTitle()));
        }
        this.mHandler.sendEmptyMessage(3);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doDie() {
        checkThread();
        if (LOCAL_LOGV) {
            Log.v(this.mTag, "DIE in " + this + " of " + this.mSurface);
        }
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.v(this.mTag, "DIE in " + this + " of " + this.mSurface + ", mAdded = " + this.mAdded + ", mFirst = " + this.mFirst);
        }
        synchronized (this) {
            if (this.mRemoved) {
                return;
            }
            this.mRemoved = true;
            this.mOnBackInvokedDispatcher.detachFromWindow();
            if (this.mAdded) {
                dispatchDetachedFromWindow();
            } else {
                this.mAccessibilityInteractionConnectionManager.ensureNoConnection();
                this.mAccessibilityManager.removeAccessibilityStateChangeListener(this.mAccessibilityInteractionConnectionManager);
                this.mAccessibilityManager.removeHighTextContrastStateChangeListener(this.mHighContrastTextManager);
                this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "doDie:removeAccessibilityStateChangeListener......");
                }
            }
            if (this.mAdded && !this.mFirst) {
                destroyHardwareRenderer();
                View view = this.mView;
                if (view != null) {
                    int viewVisibility = view.getVisibility();
                    boolean viewVisibilityChanged = this.mViewVisibility != viewVisibility;
                    if (this.mWindowAttributesChanged || viewVisibilityChanged) {
                        try {
                            if ((1 & relayoutWindow(this.mWindowAttributes, viewVisibility, false)) != 0) {
                                this.mWindowSession.finishDrawing(this.mWindow, null, Integer.MAX_VALUE);
                            }
                        } catch (RemoteException e) {
                            Log.e(this.mTag, "RemoteException when finish draw window " + this.mWindow + " in " + this, e);
                        }
                    }
                    destroySurface();
                }
            }
            this.mInsetsController.onControlsChanged(null);
            this.mAdded = false;
            AnimationHandler.removeRequestor(this);
            WindowManagerGlobal.getInstance().doRemoveView(this);
        }
    }

    public void requestUpdateConfiguration(Configuration config) {
        Message msg = this.mHandler.obtainMessage(18, config);
        this.mHandler.sendMessage(msg);
    }

    public void loadSystemProperties() {
        this.mHandler.post(new Runnable() { // from class: android.view.ViewRootImpl.7
            @Override // java.lang.Runnable
            public void run() {
                ViewRootImpl.this.mProfileRendering = SystemProperties.getBoolean(ViewRootImpl.PROPERTY_PROFILE_RENDERING, false);
                ViewRootImpl viewRootImpl = ViewRootImpl.this;
                viewRootImpl.profileRendering(viewRootImpl.mAttachInfo.mHasWindowFocus);
                if (ViewRootImpl.this.mAttachInfo.mThreadedRenderer != null && ViewRootImpl.this.mAttachInfo.mThreadedRenderer.loadSystemProperties()) {
                    ViewRootImpl.this.invalidate();
                }
                boolean layout = DisplayProperties.debug_layout().orElse(false).booleanValue();
                if (layout != ViewRootImpl.this.mAttachInfo.mDebugLayout) {
                    ViewRootImpl.this.mAttachInfo.mDebugLayout = layout;
                    if (!ViewRootImpl.this.mHandler.hasMessages(22)) {
                        ViewRootImpl.this.mHandler.sendEmptyMessageDelayed(22, 200L);
                    }
                }
            }
        });
    }

    private void destroyHardwareRenderer() {
        ThreadedRenderer hardwareRenderer = this.mAttachInfo.mThreadedRenderer;
        if (hardwareRenderer != null) {
            HardwareRendererObserver hardwareRendererObserver = this.mHardwareRendererObserver;
            if (hardwareRendererObserver != null) {
                hardwareRenderer.removeObserver(hardwareRendererObserver);
            }
            View view = this.mView;
            if (view != null) {
                hardwareRenderer.destroyHardwareResources(view);
            }
            hardwareRenderer.destroy();
            hardwareRenderer.setRequested(false);
            this.mAttachInfo.mThreadedRenderer = null;
            this.mAttachInfo.mHardwareAccelerated = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchResized(ClientWindowFrames frames, boolean reportDraw, MergedConfiguration mergedConfiguration, InsetsState insetsState, boolean forceLayout, boolean alwaysConsumeSystemBars, int displayId, int syncSeqId, int resizeMode) {
        InsetsState insetsState2;
        Message msg = this.mHandler.obtainMessage(reportDraw ? 5 : 4);
        SomeArgs args = SomeArgs.obtain();
        boolean sameProcessCall = Binder.getCallingPid() == Process.myPid();
        if (!sameProcessCall) {
            insetsState2 = insetsState;
        } else {
            insetsState2 = new InsetsState(insetsState, true);
        }
        CompatibilityInfo.Translator translator = this.mTranslator;
        if (translator != null) {
            translator.translateInsetsStateInScreenToAppWindow(insetsState2);
        }
        if (insetsState2.getSourceOrDefaultVisibility(19)) {
            ImeTracing.getInstance().triggerClientDump("ViewRootImpl#dispatchResized", getInsetsController().getHost().getInputMethodManager(), null);
        }
        args.arg1 = sameProcessCall ? new ClientWindowFrames(frames) : frames;
        args.arg2 = (!sameProcessCall || mergedConfiguration == null) ? mergedConfiguration : new MergedConfiguration(mergedConfiguration);
        args.arg3 = insetsState2;
        args.argi1 = forceLayout ? 1 : 0;
        args.argi2 = alwaysConsumeSystemBars ? 1 : 0;
        args.argi3 = displayId;
        args.argi4 = syncSeqId;
        args.argi5 = resizeMode;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchInsetsControlChanged(InsetsState insetsState, InsetsSourceControl[] activeControls) {
        if (Binder.getCallingPid() == Process.myPid()) {
            insetsState = new InsetsState(insetsState, true);
            if (activeControls != null) {
                for (int i = activeControls.length - 1; i >= 0; i--) {
                    activeControls[i] = new InsetsSourceControl(activeControls[i]);
                }
            }
        }
        CompatibilityInfo.Translator translator = this.mTranslator;
        if (translator != null) {
            translator.translateInsetsStateInScreenToAppWindow(insetsState);
            this.mTranslator.translateSourceControlsInScreenToAppWindow(activeControls);
        }
        if (insetsState != null && insetsState.getSourceOrDefaultVisibility(19)) {
            ImeTracing.getInstance().triggerClientDump("ViewRootImpl#dispatchInsetsControlChanged", getInsetsController().getHost().getInputMethodManager(), null);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = insetsState;
        args.arg2 = activeControls;
        this.mHandler.obtainMessage(29, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showInsets(int types, boolean fromIme) {
        this.mHandler.obtainMessage(31, types, fromIme ? 1 : 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideInsets(int types, boolean fromIme) {
        this.mHandler.obtainMessage(32, types, fromIme ? 1 : 0).sendToTarget();
    }

    public void dispatchMoved(int newX, int newY) {
        if (DEBUG_LAYOUT) {
            Log.v(this.mTag, "Window moved " + this + ": newX=" + newX + " newY=" + newY);
        }
        if (this.mTranslator != null) {
            PointF point = new PointF(newX, newY);
            this.mTranslator.translatePointInScreenToAppWindow(point);
            newX = (int) (point.x + 0.5d);
            newY = (int) (point.y + 0.5d);
        }
        Message msg = this.mHandler.obtainMessage(23, newX, newY);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class QueuedInputEvent {
        public static final int FLAG_DEFERRED = 2;
        public static final int FLAG_DELIVER_POST_IME = 1;
        public static final int FLAG_FINISHED = 4;
        public static final int FLAG_FINISHED_HANDLED = 8;
        public static final int FLAG_MODIFIED_FOR_COMPATIBILITY = 64;
        public static final int FLAG_RESYNTHESIZED = 16;
        public static final int FLAG_UNHANDLED = 32;
        public InputEvent mEvent;
        public int mFlags;
        public QueuedInputEvent mNext;
        public InputEventReceiver mReceiver;

        private QueuedInputEvent() {
        }

        public boolean shouldSkipIme() {
            if ((this.mFlags & 1) != 0) {
                return true;
            }
            InputEvent inputEvent = this.mEvent;
            return (inputEvent instanceof MotionEvent) && (inputEvent.isFromSource(2) || this.mEvent.isFromSource(4194304));
        }

        public boolean shouldSendToSynthesizer() {
            if ((this.mFlags & 32) != 0) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("QueuedInputEvent{flags=");
            boolean hasPrevious = flagToString("DELIVER_POST_IME", 1, false, sb);
            if (!flagToString("UNHANDLED", 32, flagToString("RESYNTHESIZED", 16, flagToString("FINISHED_HANDLED", 8, flagToString("FINISHED", 4, flagToString("DEFERRED", 2, hasPrevious, sb), sb), sb), sb), sb)) {
                sb.append(AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
            }
            sb.append(", hasNextQueuedEvent=" + (this.mEvent != null ? "true" : "false"));
            sb.append(", hasInputEventReceiver=" + (this.mReceiver == null ? "false" : "true"));
            sb.append(", mEvent=" + this.mEvent + "}");
            return sb.toString();
        }

        private boolean flagToString(String name, int flag, boolean hasPrevious, StringBuilder sb) {
            if ((this.mFlags & flag) != 0) {
                if (hasPrevious) {
                    sb.append("|");
                }
                sb.append(name);
                return true;
            }
            return hasPrevious;
        }
    }

    private QueuedInputEvent obtainQueuedInputEvent(InputEvent event, InputEventReceiver receiver, int flags) {
        QueuedInputEvent q = this.mQueuedInputEventPool;
        if (q != null) {
            this.mQueuedInputEventPoolSize--;
            this.mQueuedInputEventPool = q.mNext;
            q.mNext = null;
        } else {
            q = new QueuedInputEvent();
        }
        q.mEvent = event;
        q.mReceiver = receiver;
        q.mFlags = flags;
        return q;
    }

    private void recycleQueuedInputEvent(QueuedInputEvent q) {
        q.mEvent = null;
        q.mReceiver = null;
        int i = this.mQueuedInputEventPoolSize;
        if (i < 10) {
            this.mQueuedInputEventPoolSize = i + 1;
            q.mNext = this.mQueuedInputEventPool;
            this.mQueuedInputEventPool = q;
        }
    }

    void enqueueInputEvent(InputEvent event) {
        enqueueInputEvent(event, null, 0, false);
    }

    void enqueueInputEvent(InputEvent event, InputEventReceiver receiver, int flags, boolean processImmediately) {
        QueuedInputEvent q = obtainQueuedInputEvent(event, receiver, flags);
        if (event instanceof MotionEvent) {
            MotionEvent me = (MotionEvent) event;
            if (me.getAction() == 3) {
                EventLog.writeEvent((int) EventLogTags.VIEW_ENQUEUE_INPUT_EVENT, "Motion - Cancel", getTitle().toString());
            }
        } else if (event instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) event;
            if (ke.isCanceled()) {
                EventLog.writeEvent((int) EventLogTags.VIEW_ENQUEUE_INPUT_EVENT, "Key - Cancel", getTitle().toString());
            }
        }
        QueuedInputEvent last = this.mPendingInputEventTail;
        if (last == null) {
            this.mPendingInputEventHead = q;
            this.mPendingInputEventTail = q;
        } else {
            last.mNext = q;
            this.mPendingInputEventTail = q;
        }
        int i = this.mPendingInputEventCount + 1;
        this.mPendingInputEventCount = i;
        Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, i);
        if (ViewDebugManager.DEBUG_MET_TRACE && (event instanceof MotionEvent)) {
            MotionEvent ev = (MotionEvent) event;
            Trace.traceBegin(4L, "MET_enqueueInputEvent_name: " + ((Object) this.mWindowAttributes.getTitle()));
            Trace.traceEnd(4L);
            Trace.traceBegin(4L, "MET_enqueueInputEvent: " + ev.getEventTime() + "," + ev.getAction() + "," + ev.getX(0) + "," + ev.getY(0));
            Trace.traceEnd(4L);
        }
        if (ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY || ViewDebugManager.DEBUG_MOTION || ViewDebugManager.DEBUG_ENG) {
            Log.v(this.mTag, "enqueueInputEvent: event = " + event + ",processImmediately = " + processImmediately + ",mProcessInputEventsScheduled = " + this.mProcessInputEventsScheduled + ", this = " + this);
        }
        if (!ViewDebugManager.DEBUG_INPUT && !ViewDebugManager.DEBUG_KEY && !ViewDebugManager.DEBUG_MOTION && !ViewDebugManager.DEBUG_ENG && (this.debug_on || ViewDebugManager.DEBUG_USERDEBUG)) {
            Log.d(this.mTag, "enqueueInputEvent: event = " + event + ",processImmediately = " + processImmediately + ",mProcessInputEventsScheduled = " + this.mProcessInputEventsScheduled + ", this = " + this);
        }
        if (processImmediately) {
            doProcessInputEvents();
        } else {
            m5131lambda$doProcessInputEvents$9$androidviewViewRootImpl();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: scheduleProcessInputEvents */
    public void m5131lambda$doProcessInputEvents$9$androidviewViewRootImpl() {
        if (!this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = true;
            Message msg = this.mHandler.obtainMessage(19);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    void doProcessInputEvents() {
        if (delayProcessMotionEvent()) {
            if (this.mLastDelayTimestamp == -1) {
                this.mLastDelayTimestamp = SystemClock.uptimeMillis();
            }
            this.mHandler.postDelayed(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    ViewRootImpl.this.m5131lambda$doProcessInputEvents$9$androidviewViewRootImpl();
                }
            }, 25L);
            Log.d(TAG_GESTURE, "delay process motion event, mPendingInputEventCount = " + this.mPendingInputEventCount);
            return;
        }
        this.mLastDelayTimestamp = -1L;
        while (this.mPendingInputEventHead != null) {
            QueuedInputEvent q = this.mPendingInputEventHead;
            QueuedInputEvent queuedInputEvent = q.mNext;
            this.mPendingInputEventHead = queuedInputEvent;
            if (queuedInputEvent == null) {
                this.mPendingInputEventTail = null;
            }
            q.mNext = null;
            int i = this.mPendingInputEventCount - 1;
            this.mPendingInputEventCount = i;
            Trace.traceCounter(4L, this.mPendingInputEventQueueLengthCounterName, i);
            if (q.mEvent instanceof MotionEvent) {
                MotionEvent me = (MotionEvent) q.mEvent;
                if (this.mIsSurfaceViewCreated && this.mSurfaceExt.isResolutionTuningPackage() && !this.mSurfaceExt.isScaledByGameMode() && isRtEnable()) {
                    Matrix scale = new Matrix();
                    scale.setScale(1.0f / this.mSurfaceExt.getXScale(), 1.0f / this.mSurfaceExt.getYScale());
                    me.transform(scale);
                }
            }
            this.mViewFrameInfo.setInputEvent(this.mInputEventAssigner.processEvent(q.mEvent));
            if (ViewDebugManager.DEBUG_INPUT || ViewDebugManager.DEBUG_KEY || ViewDebugManager.DEBUG_MOTION) {
                Log.v(this.mTag, "doProcessInputEvents: mCurrentInputEvent = " + q + ", this = " + this);
            }
            deliverInputEvent(q);
        }
        if (this.mProcessInputEventsScheduled) {
            this.mProcessInputEventsScheduled = false;
            this.mHandler.removeMessages(19);
        }
    }

    private void deliverInputEvent(QueuedInputEvent q) {
        InputStage stage;
        Trace.asyncTraceBegin(8L, "deliverInputEvent", q.mEvent.getId());
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "deliverInputEvent src=0x" + Integer.toHexString(q.mEvent.getSource()) + " eventTimeNano=" + q.mEvent.getEventTimeNano() + " id=0x" + Integer.toHexString(q.mEvent.getId()));
        }
        try {
            if (this.mInputEventConsistencyVerifier != null) {
                Trace.traceBegin(8L, "verifyEventConsistency");
                this.mInputEventConsistencyVerifier.onInputEvent(q.mEvent, 0);
                Trace.traceEnd(8L);
            }
            if (q.shouldSendToSynthesizer()) {
                stage = this.mSyntheticInputStage;
            } else {
                stage = q.shouldSkipIme() ? this.mFirstPostImeInputStage : this.mFirstInputStage;
            }
            if (q.mEvent instanceof KeyEvent) {
                Trace.traceBegin(8L, "preDispatchToUnhandledKeyManager");
                this.mUnhandledKeyManager.preDispatch((KeyEvent) q.mEvent);
                Trace.traceEnd(8L);
            }
            if (stage != null) {
                handleWindowFocusChanged();
                stage.deliver(q);
            } else {
                finishInputEvent(q);
            }
        } finally {
            Trace.traceEnd(8L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishInputEvent(QueuedInputEvent q) {
        Trace.asyncTraceEnd(8L, "deliverInputEvent", q.mEvent.getId());
        boolean handled = (q.mFlags & 8) != 0;
        ViewDebugManager.getInstance().debugInputEventFinished(this.mTag, handled, q.mEvent, this);
        if (q.mReceiver != null) {
            boolean modified = (q.mFlags & 64) != 0;
            if (modified) {
                Trace.traceBegin(8L, "processInputEventBeforeFinish");
                try {
                    InputEvent processedEvent = this.mInputCompatProcessor.processInputEventBeforeFinish(q.mEvent);
                    if (processedEvent != null) {
                        q.mReceiver.finishInputEvent(processedEvent, handled);
                    }
                } finally {
                    Trace.traceEnd(8L);
                }
            } else {
                q.mReceiver.finishInputEvent(q.mEvent, handled);
            }
        } else {
            q.mEvent.recycleIfNeededAfterDispatch();
        }
        recycleQueuedInputEvent(q);
    }

    static boolean isTerminalInputEvent(InputEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            return keyEvent.getAction() == 1;
        }
        MotionEvent motionEvent = (MotionEvent) event;
        int action = motionEvent.getAction();
        return action == 1 || action == 3 || action == 10;
    }

    void scheduleConsumeBatchedInput() {
        if (!this.mConsumeBatchedInputScheduled && !this.mConsumeBatchedInputImmediatelyScheduled) {
            this.mConsumeBatchedInputScheduled = true;
            this.mChoreographer.postCallback(0, this.mConsumedBatchedInputRunnable, null);
        }
    }

    void unscheduleConsumeBatchedInput() {
        if (this.mConsumeBatchedInputScheduled) {
            this.mConsumeBatchedInputScheduled = false;
            this.mChoreographer.removeCallbacks(0, this.mConsumedBatchedInputRunnable, null);
        }
    }

    void scheduleConsumeBatchedInputImmediately() {
        if (!this.mConsumeBatchedInputImmediatelyScheduled) {
            unscheduleConsumeBatchedInput();
            this.mConsumeBatchedInputImmediatelyScheduled = true;
            this.mHandler.post(this.mConsumeBatchedInputImmediatelyRunnable);
        }
    }

    boolean doConsumeBatchedInput(long frameTimeNanos) {
        boolean consumedBatches;
        WindowInputEventReceiver windowInputEventReceiver = this.mInputEventReceiver;
        if (windowInputEventReceiver != null) {
            consumedBatches = windowInputEventReceiver.consumeBatchedInputEvents(frameTimeNanos);
        } else {
            consumedBatches = false;
        }
        doProcessInputEvents();
        return consumedBatches;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class TraversalRunnable implements Runnable {
        TraversalRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            ViewRootImpl.this.doTraversal();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class WindowInputEventReceiver extends InputEventReceiver {
        public WindowInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override // android.view.InputEventReceiver
        public void onInputEvent(InputEvent event) {
            ViewDebugManager.getInstance().debugInputEventStart(event);
            Trace.traceBegin(8L, "processInputEventForCompatibility");
            try {
                List<InputEvent> processedEvents = ViewRootImpl.this.mInputCompatProcessor.processInputEventForCompatibility(event);
                Trace.traceEnd(8L);
                if (processedEvents == null) {
                    ViewRootImpl.this.enqueueInputEvent(event, this, 0, true);
                } else if (processedEvents.isEmpty()) {
                    finishInputEvent(event, true);
                } else {
                    for (int i = 0; i < processedEvents.size(); i++) {
                        ViewRootImpl.this.enqueueInputEvent(processedEvents.get(i), this, 64, true);
                    }
                }
            } catch (Throwable th) {
                Trace.traceEnd(8L);
                throw th;
            }
        }

        @Override // android.view.InputEventReceiver
        public void onBatchedInputEventPending(int source) {
            boolean unbuffered = ViewRootImpl.this.mUnbufferedInputDispatch || (ViewRootImpl.this.mUnbufferedInputSource & source) != 0;
            if (unbuffered) {
                if (ViewRootImpl.this.mConsumeBatchedInputScheduled) {
                    ViewRootImpl.this.unscheduleConsumeBatchedInput();
                }
                consumeBatchedInputEvents(-1L);
                return;
            }
            ViewRootImpl.this.scheduleConsumeBatchedInput();
        }

        @Override // android.view.InputEventReceiver
        public void onFocusEvent(boolean hasFocus) {
            ViewRootImpl.this.windowFocusChanged(hasFocus);
            if (ViewRootImpl.this.mView != null) {
                ViewRootImpl.this.mView.sendAccessibilityEvent(32);
            }
        }

        @Override // android.view.InputEventReceiver
        public void onTouchModeChanged(boolean inTouchMode) {
            ViewRootImpl.this.touchModeChanged(inTouchMode);
        }

        @Override // android.view.InputEventReceiver
        public void onPointerCaptureEvent(boolean pointerCaptureEnabled) {
            ViewRootImpl.this.dispatchPointerCaptureChanged(pointerCaptureEnabled);
        }

        @Override // android.view.InputEventReceiver
        public void onDragEvent(boolean isExiting, float x, float y) {
            DragEvent event = DragEvent.obtain(isExiting ? 6 : 2, x, y, 0.0f, 0.0f, null, null, null, null, null, false);
            ViewRootImpl.this.dispatchDragEvent(event);
        }

        @Override // android.view.InputEventReceiver
        public void dispose() {
            ViewRootImpl.this.unscheduleConsumeBatchedInput();
            super.dispose();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class InputMetricsListener implements HardwareRendererObserver.OnFrameMetricsAvailableListener {
        public long[] data = new long[23];

        InputMetricsListener() {
        }

        @Override // android.graphics.HardwareRendererObserver.OnFrameMetricsAvailableListener
        public void onFrameMetricsAvailable(int dropCountSinceLastInvocation) {
            long[] jArr = this.data;
            int inputEventId = (int) jArr[4];
            if (inputEventId == 0) {
                return;
            }
            long presentTime = jArr[21];
            if (presentTime <= 0) {
                return;
            }
            long gpuCompletedTime = jArr[19];
            if (ViewRootImpl.this.mInputEventReceiver == null) {
                return;
            }
            if (gpuCompletedTime >= presentTime) {
                double discrepancyMs = (gpuCompletedTime - presentTime) * 1.0E-6d;
                long vsyncId = this.data[1];
                Log.w(ViewRootImpl.TAG, "Not reporting timeline because gpuCompletedTime is " + discrepancyMs + "ms ahead of presentTime. FRAME_TIMELINE_VSYNC_ID=" + vsyncId + ", INPUT_EVENT_ID=" + inputEventId);
                return;
            }
            ViewRootImpl.this.mInputEventReceiver.reportTimeline(inputEventId, gpuCompletedTime, presentTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ConsumeBatchedInputRunnable implements Runnable {
        ConsumeBatchedInputRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            ViewRootImpl.this.mConsumeBatchedInputScheduled = false;
            ViewRootImpl viewRootImpl = ViewRootImpl.this;
            if (viewRootImpl.doConsumeBatchedInput(viewRootImpl.mChoreographer.getFrameTimeNanos())) {
                ViewRootImpl.this.scheduleConsumeBatchedInput();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class ConsumeBatchedInputImmediatelyRunnable implements Runnable {
        ConsumeBatchedInputImmediatelyRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            ViewRootImpl.this.mConsumeBatchedInputImmediatelyScheduled = false;
            ViewRootImpl.this.doConsumeBatchedInput(-1L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class InvalidateOnAnimationRunnable implements Runnable {
        private boolean mPosted;
        private View.AttachInfo.InvalidateInfo[] mTempViewRects;
        private View[] mTempViews;
        private final ArrayList<View> mViews = new ArrayList<>();
        private final ArrayList<View.AttachInfo.InvalidateInfo> mViewRects = new ArrayList<>();

        InvalidateOnAnimationRunnable() {
        }

        public void addView(View view) {
            synchronized (this) {
                this.mViews.add(view);
                postIfNeededLocked();
            }
        }

        public void addViewRect(View.AttachInfo.InvalidateInfo info) {
            synchronized (this) {
                this.mViewRects.add(info);
                postIfNeededLocked();
            }
        }

        public void removeView(View view) {
            synchronized (this) {
                this.mViews.remove(view);
                int i = this.mViewRects.size();
                while (true) {
                    int i2 = i - 1;
                    if (i <= 0) {
                        break;
                    }
                    View.AttachInfo.InvalidateInfo info = this.mViewRects.get(i2);
                    if (info.target == view) {
                        this.mViewRects.remove(i2);
                        info.recycle();
                    }
                    i = i2;
                }
                if (this.mPosted && this.mViews.isEmpty() && this.mViewRects.isEmpty()) {
                    ViewRootImpl.this.mChoreographer.removeCallbacks(1, this, null);
                    this.mPosted = false;
                }
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            int viewCount;
            int viewRectCount;
            synchronized (this) {
                this.mPosted = false;
                viewCount = this.mViews.size();
                if (viewCount != 0) {
                    ArrayList<View> arrayList = this.mViews;
                    View[] viewArr = this.mTempViews;
                    if (viewArr == null) {
                        viewArr = new View[viewCount];
                    }
                    this.mTempViews = (View[]) arrayList.toArray(viewArr);
                    this.mViews.clear();
                }
                viewRectCount = this.mViewRects.size();
                if (viewRectCount != 0) {
                    ArrayList<View.AttachInfo.InvalidateInfo> arrayList2 = this.mViewRects;
                    View.AttachInfo.InvalidateInfo[] invalidateInfoArr = this.mTempViewRects;
                    if (invalidateInfoArr == null) {
                        invalidateInfoArr = new View.AttachInfo.InvalidateInfo[viewRectCount];
                    }
                    this.mTempViewRects = (View.AttachInfo.InvalidateInfo[]) arrayList2.toArray(invalidateInfoArr);
                    this.mViewRects.clear();
                }
            }
            for (int i = 0; i < viewCount; i++) {
                this.mTempViews[i].invalidate();
                this.mTempViews[i] = null;
            }
            for (int i2 = 0; i2 < viewRectCount; i2++) {
                View.AttachInfo.InvalidateInfo info = this.mTempViewRects[i2];
                info.target.invalidate(info.left, info.top, info.right, info.bottom);
                info.recycle();
            }
        }

        private void postIfNeededLocked() {
            if (!this.mPosted) {
                ViewRootImpl.this.mChoreographer.postCallback(1, this, null);
                this.mPosted = true;
            }
        }
    }

    public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
        Message msg = this.mHandler.obtainMessage(1, view);
        this.mHandler.sendMessageDelayed(msg, delayMilliseconds);
    }

    public void dispatchInvalidateRectDelayed(View.AttachInfo.InvalidateInfo info, long delayMilliseconds) {
        Message msg = this.mHandler.obtainMessage(2, info);
        this.mHandler.sendMessageDelayed(msg, delayMilliseconds);
    }

    public void dispatchInvalidateOnAnimation(View view) {
        this.mInvalidateOnAnimationRunnable.addView(view);
    }

    public void dispatchInvalidateRectOnAnimation(View.AttachInfo.InvalidateInfo info) {
        this.mInvalidateOnAnimationRunnable.addViewRect(info);
    }

    public void cancelInvalidate(View view) {
        this.mHandler.removeMessages(1, view);
        this.mHandler.removeMessages(2, view);
        this.mInvalidateOnAnimationRunnable.removeView(view);
    }

    public void dispatchInputEvent(InputEvent event) {
        dispatchInputEvent(event, null);
    }

    public void dispatchInputEvent(InputEvent event, InputEventReceiver receiver) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = event;
        args.arg2 = receiver;
        Message msg = this.mHandler.obtainMessage(7, args);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void synthesizeInputEvent(InputEvent event) {
        Message msg = this.mHandler.obtainMessage(24, event);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchKeyFromIme(KeyEvent event) {
        Message msg = this.mHandler.obtainMessage(11, event);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchKeyFromAutofill(KeyEvent event) {
        Message msg = this.mHandler.obtainMessage(12, event);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchUnhandledInputEvent(InputEvent event) {
        if (event instanceof MotionEvent) {
            event = MotionEvent.obtain((MotionEvent) event);
        }
        synthesizeInputEvent(event);
    }

    public void dispatchAppVisibility(boolean visible) {
        Message msg = this.mHandler.obtainMessage(8);
        msg.arg1 = visible ? 1 : 0;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchGetNewSurface() {
        Message msg = this.mHandler.obtainMessage(9);
        this.mHandler.sendMessage(msg);
    }

    public void windowFocusChanged(boolean hasFocus) {
        synchronized (this) {
            this.mWindowFocusChanged = true;
            this.mUpcomingWindowFocus = hasFocus;
        }
        Message msg = Message.obtain();
        msg.what = 6;
        this.mHandler.sendMessage(msg);
    }

    public void touchModeChanged(boolean inTouchMode) {
        synchronized (this) {
            this.mUpcomingInTouchMode = inTouchMode;
        }
        Message msg = Message.obtain();
        msg.what = 34;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchWindowShown() {
        this.mHandler.sendEmptyMessage(25);
    }

    public void dispatchCloseSystemDialogs(String reason) {
        Message msg = Message.obtain();
        msg.what = 14;
        msg.obj = reason;
        this.mHandler.sendMessage(msg);
    }

    public void dispatchDragEvent(DragEvent event) {
        int what;
        if (event.getAction() == 2) {
            what = 16;
            this.mHandler.removeMessages(16);
        } else {
            what = 15;
        }
        Message msg = this.mHandler.obtainMessage(what, event);
        this.mHandler.sendMessage(msg);
    }

    public void updatePointerIcon(float x, float y) {
        this.mHandler.removeMessages(27);
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(0L, now, 7, x, y, 0);
        Message msg = this.mHandler.obtainMessage(27, event);
        this.mHandler.sendMessage(msg);
    }

    public void dispatchCheckFocus() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessage(13);
        }
    }

    public void dispatchRequestKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        this.mHandler.obtainMessage(26, deviceId, 0, receiver).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchPointerCaptureChanged(boolean on) {
        this.mHandler.removeMessages(28);
        Message msg = this.mHandler.obtainMessage(28);
        msg.arg1 = on ? 1 : 0;
        this.mHandler.sendMessage(msg);
    }

    private void postSendWindowContentChangedCallback(View source, int changeType) {
        if (this.mSendWindowContentChangedAccessibilityEvent == null) {
            this.mSendWindowContentChangedAccessibilityEvent = new SendWindowContentChangedAccessibilityEvent();
        }
        this.mSendWindowContentChangedAccessibilityEvent.runOrPost(source, changeType);
    }

    private void removeSendWindowContentChangedCallback() {
        SendWindowContentChangedAccessibilityEvent sendWindowContentChangedAccessibilityEvent = this.mSendWindowContentChangedAccessibilityEvent;
        if (sendWindowContentChangedAccessibilityEvent != null) {
            this.mHandler.removeCallbacks(sendWindowContentChangedAccessibilityEvent);
        }
    }

    @Override // android.view.ViewParent
    public boolean showContextMenuForChild(View originalView) {
        return false;
    }

    @Override // android.view.ViewParent
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        return false;
    }

    @Override // android.view.ViewParent
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        return null;
    }

    @Override // android.view.ViewParent
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
        return null;
    }

    @Override // android.view.ViewParent
    public void createContextMenu(ContextMenu menu) {
    }

    @Override // android.view.ViewParent
    public void childDrawableStateChanged(View child) {
    }

    @Override // android.view.ViewParent
    public boolean requestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        AccessibilityNodeProvider provider;
        SendWindowContentChangedAccessibilityEvent sendWindowContentChangedAccessibilityEvent;
        if (this.mView == null || this.mStopped || this.mPausedForTransition) {
            return false;
        }
        if (event.getEventType() != 2048 && (sendWindowContentChangedAccessibilityEvent = this.mSendWindowContentChangedAccessibilityEvent) != null && sendWindowContentChangedAccessibilityEvent.mSource != null) {
            this.mSendWindowContentChangedAccessibilityEvent.removeCallbacksAndRun();
        }
        int eventType = event.getEventType();
        View source = getSourceForAccessibilityEvent(event);
        switch (eventType) {
            case 2048:
                handleWindowContentChangedEvent(event);
                break;
            case 32768:
                if (source != null && (provider = source.getAccessibilityNodeProvider()) != null) {
                    int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(event.getSourceNodeId());
                    AccessibilityNodeInfo node = provider.createAccessibilityNodeInfo(virtualNodeId);
                    setAccessibilityFocus(source, node);
                    break;
                }
                break;
            case 65536:
                if (source != null && source.getAccessibilityNodeProvider() != null) {
                    setAccessibilityFocus(null, null);
                    break;
                }
                break;
        }
        this.mAccessibilityManager.sendAccessibilityEvent(event);
        return true;
    }

    private View getSourceForAccessibilityEvent(AccessibilityEvent event) {
        long sourceNodeId = event.getSourceNodeId();
        int accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(sourceNodeId);
        return AccessibilityNodeIdManager.getInstance().findView(accessibilityViewId);
    }

    private void handleWindowContentChangedEvent(AccessibilityEvent event) {
        View focusedHost = this.mAccessibilityFocusedHost;
        if (focusedHost == null || this.mAccessibilityFocusedVirtualView == null) {
            return;
        }
        AccessibilityNodeProvider provider = focusedHost.getAccessibilityNodeProvider();
        if (provider == null) {
            this.mAccessibilityFocusedHost = null;
            this.mAccessibilityFocusedVirtualView = null;
            focusedHost.clearAccessibilityFocusNoCallbacks(0);
            return;
        }
        int changes = event.getContentChangeTypes();
        if ((changes & 1) == 0 && changes != 0) {
            return;
        }
        long eventSourceNodeId = event.getSourceNodeId();
        int changedViewId = AccessibilityNodeInfo.getAccessibilityViewId(eventSourceNodeId);
        boolean hostInSubtree = false;
        View root = this.mAccessibilityFocusedHost;
        while (root != null && !hostInSubtree) {
            if (changedViewId == root.getAccessibilityViewId()) {
                hostInSubtree = true;
            } else {
                ViewParent parent = root.getParent();
                if (parent instanceof View) {
                    root = (View) parent;
                } else {
                    root = null;
                }
            }
        }
        if (!hostInSubtree) {
            return;
        }
        long focusedSourceNodeId = this.mAccessibilityFocusedVirtualView.getSourceNodeId();
        int focusedChildId = AccessibilityNodeInfo.getVirtualDescendantId(focusedSourceNodeId);
        Rect oldBounds = this.mTempRect;
        this.mAccessibilityFocusedVirtualView.getBoundsInScreen(oldBounds);
        AccessibilityNodeInfo createAccessibilityNodeInfo = provider.createAccessibilityNodeInfo(focusedChildId);
        this.mAccessibilityFocusedVirtualView = createAccessibilityNodeInfo;
        if (createAccessibilityNodeInfo == null) {
            this.mAccessibilityFocusedHost = null;
            focusedHost.clearAccessibilityFocusNoCallbacks(0);
            provider.performAction(focusedChildId, AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS.getId(), null);
            invalidateRectOnScreen(oldBounds);
            return;
        }
        Rect newBounds = createAccessibilityNodeInfo.getBoundsInScreen();
        if (!oldBounds.equals(newBounds)) {
            oldBounds.union(newBounds);
            invalidateRectOnScreen(oldBounds);
        }
    }

    @Override // android.view.ViewParent
    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType) {
        postSendWindowContentChangedCallback((View) Preconditions.checkNotNull(source), changeType);
    }

    @Override // android.view.ViewParent
    public boolean canResolveLayoutDirection() {
        return true;
    }

    @Override // android.view.ViewParent
    public boolean isLayoutDirectionResolved() {
        return true;
    }

    @Override // android.view.ViewParent
    public int getLayoutDirection() {
        return 0;
    }

    @Override // android.view.ViewParent
    public boolean canResolveTextDirection() {
        return true;
    }

    @Override // android.view.ViewParent
    public boolean isTextDirectionResolved() {
        return true;
    }

    @Override // android.view.ViewParent
    public int getTextDirection() {
        return 1;
    }

    @Override // android.view.ViewParent
    public boolean canResolveTextAlignment() {
        return true;
    }

    @Override // android.view.ViewParent
    public boolean isTextAlignmentResolved() {
        return true;
    }

    @Override // android.view.ViewParent
    public int getTextAlignment() {
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public View getCommonPredecessor(View first, View second) {
        if (this.mTempHashSet == null) {
            this.mTempHashSet = new HashSet<>();
        }
        HashSet<View> seen = this.mTempHashSet;
        seen.clear();
        View firstCurrent = first;
        while (firstCurrent != null) {
            seen.add(firstCurrent);
            ViewParent firstCurrentParent = firstCurrent.mParent;
            if (firstCurrentParent instanceof View) {
                firstCurrent = (View) firstCurrentParent;
            } else {
                firstCurrent = null;
            }
        }
        View secondCurrent = second;
        while (secondCurrent != null) {
            if (seen.contains(secondCurrent)) {
                seen.clear();
                return secondCurrent;
            }
            ViewParent secondCurrentParent = secondCurrent.mParent;
            if (secondCurrentParent instanceof View) {
                secondCurrent = (View) secondCurrentParent;
            } else {
                secondCurrent = null;
            }
        }
        seen.clear();
        return null;
    }

    void checkThread() {
        if (this.mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException("Only the original thread that created a view hierarchy can touch its views.");
        }
    }

    @Override // android.view.ViewParent
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override // android.view.ViewParent
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        if (rectangle == null) {
            return scrollToRectOrFocus(null, immediate);
        }
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
        boolean scrolled = scrollToRectOrFocus(rectangle, immediate);
        this.mTempRect.set(rectangle);
        this.mTempRect.offset(0, -this.mCurScrollY);
        this.mTempRect.offset(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
        try {
            this.mWindowSession.onRectangleOnScreenRequested(this.mWindow, this.mTempRect);
        } catch (RemoteException e) {
        }
        return scrolled;
    }

    @Override // android.view.ViewParent
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    @Override // android.view.ViewParent
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return false;
    }

    @Override // android.view.ViewParent
    public void onStopNestedScroll(View target) {
    }

    @Override // android.view.ViewParent
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override // android.view.ViewParent
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    }

    @Override // android.view.ViewParent
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    }

    @Override // android.view.ViewParent
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override // android.view.ViewParent
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override // android.view.ViewParent
    public boolean onNestedPrePerformAccessibilityAction(View target, int action, Bundle args) {
        return false;
    }

    public void addScrollCaptureCallback(ScrollCaptureCallback callback) {
        if (this.mRootScrollCaptureCallbacks == null) {
            this.mRootScrollCaptureCallbacks = new HashSet<>();
        }
        this.mRootScrollCaptureCallbacks.add(callback);
    }

    public void removeScrollCaptureCallback(ScrollCaptureCallback callback) {
        HashSet<ScrollCaptureCallback> hashSet = this.mRootScrollCaptureCallbacks;
        if (hashSet != null) {
            hashSet.remove(callback);
            if (this.mRootScrollCaptureCallbacks.isEmpty()) {
                this.mRootScrollCaptureCallbacks = null;
            }
        }
    }

    public void dispatchScrollCaptureRequest(IScrollCaptureResponseListener listener) {
        this.mHandler.obtainMessage(33, listener).sendToTarget();
    }

    public void dispatchCaptionViewStatus(boolean show, int multiwindowMode) {
        Message msg = this.mHandler.obtainMessage(100);
        msg.arg1 = show ? 1 : 0;
        msg.arg2 = multiwindowMode;
        this.mHandler.sendMessage(msg);
    }

    private void collectRootScrollCaptureTargets(ScrollCaptureSearchResults results) {
        HashSet<ScrollCaptureCallback> hashSet = this.mRootScrollCaptureCallbacks;
        if (hashSet == null) {
            return;
        }
        Iterator<ScrollCaptureCallback> it = hashSet.iterator();
        while (it.hasNext()) {
            ScrollCaptureCallback cb = it.next();
            Point offset = new Point(this.mView.getLeft(), this.mView.getTop());
            Rect rect = new Rect(0, 0, this.mView.getWidth(), this.mView.getHeight());
            results.addTarget(new ScrollCaptureTarget(this.mView, rect, offset, cb));
        }
    }

    public void setScrollCaptureRequestTimeout(int timeMillis) {
        this.mScrollCaptureRequestTimeout = timeMillis;
    }

    public long getScrollCaptureRequestTimeout() {
        return this.mScrollCaptureRequestTimeout;
    }

    public void handleScrollCaptureRequest(final IScrollCaptureResponseListener listener) {
        if (ITranView.Instance().handleScrollCaptureRequest(this.mContext, getView(), listener)) {
            return;
        }
        final ScrollCaptureSearchResults results = new ScrollCaptureSearchResults(this.mContext.getMainExecutor());
        collectRootScrollCaptureTargets(results);
        View rootView = getView();
        if (rootView != null) {
            Point point = new Point();
            Rect rect = new Rect(0, 0, rootView.getWidth(), rootView.getHeight());
            getChildVisibleRect(rootView, rect, point);
            Objects.requireNonNull(results);
            rootView.dispatchScrollCaptureSearch(rect, point, new Consumer() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ScrollCaptureSearchResults.this.addTarget((ScrollCaptureTarget) obj);
                }
            });
        }
        Runnable onComplete = new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ViewRootImpl.this.m5132lambda$handleScrollCaptureRequest$10$androidviewViewRootImpl(listener, results);
            }
        };
        results.setOnCompleteListener(onComplete);
        if (!results.isComplete()) {
            ViewRootHandler viewRootHandler = this.mHandler;
            Objects.requireNonNull(results);
            viewRootHandler.postDelayed(new Runnable() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    ScrollCaptureSearchResults.this.finish();
                }
            }, getScrollCaptureRequestTimeout());
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dispatchScrollCaptureSearchResponse */
    public void m5132lambda$handleScrollCaptureRequest$10$androidviewViewRootImpl(IScrollCaptureResponseListener listener, ScrollCaptureSearchResults results) {
        ScrollCaptureTarget selectedTarget = results.getTopResult();
        ScrollCaptureResponse.Builder response = new ScrollCaptureResponse.Builder();
        response.setWindowTitle(getTitle().toString());
        response.setPackageName(this.mContext.getPackageName());
        StringWriter writer = new StringWriter();
        IndentingPrintWriter pw = new IndentingPrintWriter(writer);
        results.dump(pw);
        pw.flush();
        response.addMessage(writer.toString());
        if (selectedTarget == null) {
            response.setDescription("No scrollable targets found in window");
            try {
                listener.onScrollCaptureResponse(response.build());
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send scroll capture search result", e);
                return;
            }
        }
        response.setDescription("Connected");
        Rect boundsInWindow = new Rect();
        View containingView = selectedTarget.getContainingView();
        containingView.getLocationInWindow(this.mAttachInfo.mTmpLocation);
        boundsInWindow.set(selectedTarget.getScrollBounds());
        boundsInWindow.offset(this.mAttachInfo.mTmpLocation[0], this.mAttachInfo.mTmpLocation[1]);
        response.setBoundsInWindow(boundsInWindow);
        boolean CUSTOM_SCREENSHOT_SUPPORT = "1".equals(SystemProperties.get("ro.os_supershot_apk", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        if (CUSTOM_SCREENSHOT_SUPPORT && isViewTreeHasAppBarLayout((ViewGroup) containingView)) {
            Point pos = selectedTarget.getPositionInWindow();
            Rect localVisibleRect = selectedTarget.getLocalVisibleRect();
            localVisibleRect.offsetTo(pos.x, pos.y);
            response.setBoundsInWindow(localVisibleRect);
        }
        if (this.mView == null) {
            Log.d(TAG, "mView is null ,return...");
            return;
        }
        Rect boundsOnScreen = new Rect();
        this.mView.getLocationOnScreen(this.mAttachInfo.mTmpLocation);
        boundsOnScreen.set(0, 0, this.mView.getWidth(), this.mView.getHeight());
        boundsOnScreen.offset(this.mAttachInfo.mTmpLocation[0], this.mAttachInfo.mTmpLocation[1]);
        response.setWindowBounds(boundsOnScreen);
        ScrollCaptureConnection connection = new ScrollCaptureConnection(this.mView.getContext().getMainExecutor(), selectedTarget);
        response.setConnection(connection);
        try {
            listener.onScrollCaptureResponse(response.build());
        } catch (RemoteException e2) {
            if (DEBUG_SCROLL_CAPTURE) {
                Log.w(TAG, "Failed to send scroll capture search response.", e2);
            }
            connection.close();
        }
    }

    private boolean isViewTreeHasAppBarLayout(ViewGroup view) {
        if (view != null) {
            View rootView = view.getRootView();
            for (ViewGroup parent = (ViewGroup) view.getParent(); parent != rootView && parent != null; parent = (ViewGroup) parent.getParent()) {
                if (parent.getClass().getName().contains("CoordinatorLayout")) {
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View child = parent.getChildAt(i);
                        if (child.getClass().getName().toLowerCase().contains("appbarlayout")) {
                            return true;
                        }
                    }
                    continue;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportNextDraw() {
        if (DEBUG_BLAST) {
            Log.d(this.mTag, "reportNextDraw " + Debug.getCallers(5));
        }
        this.mReportNextDraw = true;
    }

    public void setReportNextDraw(boolean syncBuffer) {
        this.mSyncBuffer = syncBuffer;
        reportNextDraw();
        invalidate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void changeCanvasOpacity(boolean opaque) {
        Log.d(this.mTag, "changeCanvasOpacity: opaque=" + opaque);
        boolean opaque2 = opaque & ((this.mView.mPrivateFlags & 512) == 0);
        if (this.mAttachInfo.mThreadedRenderer != null) {
            this.mAttachInfo.mThreadedRenderer.setOpaque(opaque2);
        }
    }

    public boolean dispatchUnhandledKeyEvent(KeyEvent event) {
        return this.mUnhandledKeyManager.dispatch(this.mView, event);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public class TakenSurfaceHolder extends BaseSurfaceHolder {
        TakenSurfaceHolder() {
        }

        @Override // com.android.internal.view.BaseSurfaceHolder
        public boolean onAllowLockCanvas() {
            return ViewRootImpl.this.mDrawingAllowed;
        }

        @Override // com.android.internal.view.BaseSurfaceHolder
        public void onRelayoutContainer() {
        }

        @Override // com.android.internal.view.BaseSurfaceHolder, android.view.SurfaceHolder
        public void setFormat(int format) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceFormat(format);
        }

        @Override // com.android.internal.view.BaseSurfaceHolder, android.view.SurfaceHolder
        public void setType(int type) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceType(type);
        }

        @Override // com.android.internal.view.BaseSurfaceHolder
        public void onUpdateSurface() {
            throw new IllegalStateException("Shouldn't be here");
        }

        @Override // android.view.SurfaceHolder
        public boolean isCreating() {
            return ViewRootImpl.this.mIsCreating;
        }

        @Override // com.android.internal.view.BaseSurfaceHolder, android.view.SurfaceHolder
        public void setFixedSize(int width, int height) {
            throw new UnsupportedOperationException("Currently only support sizing from layout");
        }

        @Override // android.view.SurfaceHolder
        public void setKeepScreenOn(boolean screenOn) {
            ((RootViewSurfaceTaker) ViewRootImpl.this.mView).setSurfaceKeepScreenOn(screenOn);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static class W extends IWindow.Stub {
        private final WeakReference<ViewRootImpl> mViewAncestor;
        private final IWindowSession mWindowSession;

        W(ViewRootImpl viewAncestor) {
            this.mViewAncestor = new WeakReference<>(viewAncestor);
            this.mWindowSession = viewAncestor.mWindowSession;
        }

        @Override // android.view.IWindow
        public void resized(ClientWindowFrames frames, boolean reportDraw, MergedConfiguration mergedConfiguration, InsetsState insetsState, boolean forceLayout, boolean alwaysConsumeSystemBars, int displayId, int syncSeqId, int resizeMode) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchResized(frames, reportDraw, mergedConfiguration, insetsState, forceLayout, alwaysConsumeSystemBars, displayId, syncSeqId, resizeMode);
            }
        }

        @Override // android.view.IWindow
        public void insetsControlChanged(InsetsState insetsState, InsetsSourceControl[] activeControls) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchInsetsControlChanged(insetsState, activeControls);
            }
        }

        @Override // android.view.IWindow
        public void showInsets(int types, boolean fromIme) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (fromIme) {
                ImeTracing.getInstance().triggerClientDump("ViewRootImpl.W#showInsets", viewAncestor.getInsetsController().getHost().getInputMethodManager(), null);
            }
            if (viewAncestor != null) {
                viewAncestor.showInsets(types, fromIme);
            }
        }

        @Override // android.view.IWindow
        public void hideInsets(int types, boolean fromIme) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (fromIme) {
                ImeTracing.getInstance().triggerClientDump("ViewRootImpl.W#hideInsets", viewAncestor.getInsetsController().getHost().getInputMethodManager(), null);
            }
            if (viewAncestor != null) {
                viewAncestor.hideInsets(types, fromIme);
            }
        }

        @Override // android.view.IWindow
        public void moved(int newX, int newY) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchMoved(newX, newY);
            }
        }

        @Override // android.view.IWindow
        public void dispatchAppVisibility(boolean visible) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (ViewDebugManager.DEBUG_LIFECYCLE) {
                Log.v(ViewRootImpl.TAG, "dispatchAppVisibility: visible = " + visible + ", viewAncestor = " + viewAncestor);
            }
            if (viewAncestor != null) {
                viewAncestor.dispatchAppVisibility(visible);
            }
        }

        @Override // android.view.IWindow
        public void dispatchGetNewSurface() {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchGetNewSurface();
            }
        }

        private static int checkCallingPermission(String permission) {
            try {
                return ActivityManager.getService().checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid());
            } catch (RemoteException e) {
                return -1;
            }
        }

        @Override // android.view.IWindow
        public void executeCommand(String command, String parameters, ParcelFileDescriptor out) {
            View view;
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null && (view = viewAncestor.mView) != null) {
                if (checkCallingPermission(Manifest.permission.DUMP) != 0) {
                    throw new SecurityException("Insufficient permissions to invoke executeCommand() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                }
                OutputStream clientStream = null;
                try {
                    try {
                        try {
                            clientStream = new ParcelFileDescriptor.AutoCloseOutputStream(out);
                            ViewDebug.dispatchCommand(view, command, parameters, clientStream);
                            clientStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (clientStream != null) {
                                clientStream.close();
                            }
                        }
                    } catch (Throwable th) {
                        if (clientStream != null) {
                            try {
                                clientStream.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        }

        @Override // android.view.IWindow
        public void closeSystemDialogs(String reason) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (ViewRootImpl.LOCAL_LOGV) {
                Log.v(ViewRootImpl.TAG, "Close system dialogs in " + viewAncestor + " for " + reason);
            }
            if (viewAncestor != null) {
                viewAncestor.dispatchCloseSystemDialogs(reason);
            }
        }

        @Override // android.view.IWindow
        public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, float zoom, boolean sync) {
            if (sync) {
                try {
                    this.mWindowSession.wallpaperOffsetsComplete(asBinder());
                } catch (RemoteException e) {
                    Log.e(ViewRootImpl.TAG, "RemoteException happens when dispatchWallpaperOffsets.", e);
                }
            }
        }

        @Override // android.view.IWindow
        public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
            if (sync) {
                try {
                    this.mWindowSession.wallpaperCommandComplete(asBinder(), null);
                } catch (RemoteException e) {
                    Log.e(ViewRootImpl.TAG, "RemoteException happens when dispatchWallpaperCommand.", e);
                }
            }
        }

        @Override // android.view.IWindow
        public void dispatchDragEvent(DragEvent event) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (ViewRootImpl.LOCAL_LOGV || ViewDebugManager.DEBUG_INPUT) {
                Log.v(ViewRootImpl.TAG, "Dispatch drag event " + event + " in " + viewAncestor);
            }
            if (viewAncestor != null) {
                viewAncestor.dispatchDragEvent(event);
            }
        }

        @Override // android.view.IWindow
        public void updatePointerIcon(float x, float y) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.updatePointerIcon(x, y);
            }
        }

        @Override // android.view.IWindow
        public void dispatchWindowShown() {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (ViewRootImpl.DEBUG_DRAW) {
                Log.v(ViewRootImpl.TAG, "doneAnimating: viewAncestor" + viewAncestor);
            }
            if (viewAncestor != null) {
                viewAncestor.dispatchWindowShown();
            }
        }

        @Override // android.view.IWindow
        public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchRequestKeyboardShortcuts(receiver, deviceId);
            }
        }

        @Override // android.view.IWindow
        public void requestScrollCapture(IScrollCaptureResponseListener listener) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchScrollCaptureRequest(listener);
            }
        }

        @Override // android.view.IWindow
        public void dispatchCaptionViewStatus(boolean show, int multiwindowMode) {
            ViewRootImpl viewAncestor = this.mViewAncestor.get();
            if (viewAncestor != null) {
                viewAncestor.dispatchCaptionViewStatus(show, multiwindowMode);
            }
        }
    }

    /* loaded from: classes3.dex */
    public static final class CalledFromWrongThreadException extends AndroidRuntimeException {
        public CalledFromWrongThreadException(String msg) {
            super(msg);
        }
    }

    static HandlerActionQueue getRunQueue() {
        ThreadLocal<HandlerActionQueue> threadLocal = sRunQueues;
        HandlerActionQueue rq = threadLocal.get();
        if (rq != null) {
            return rq;
        }
        HandlerActionQueue rq2 = new HandlerActionQueue();
        threadLocal.set(rq2);
        return rq2;
    }

    private void startDragResizing(Rect initialBounds, boolean fullscreen, Rect systemInsets, Rect stableInsets, int resizeMode) {
        if (!this.mDragResizing) {
            this.mDragResizing = true;
            if (this.mUseMTRenderer) {
                for (int i = this.mWindowCallbacks.size() - 1; i >= 0; i--) {
                    this.mWindowCallbacks.get(i).onWindowDragResizeStart(initialBounds, fullscreen, systemInsets, stableInsets, resizeMode);
                }
            }
            this.mFullRedrawNeeded = true;
        }
    }

    private void endDragResizing() {
        if (this.mDragResizing) {
            this.mDragResizing = false;
            if (this.mUseMTRenderer) {
                for (int i = this.mWindowCallbacks.size() - 1; i >= 0; i--) {
                    this.mWindowCallbacks.get(i).onWindowDragResizeEnd();
                }
            }
            this.mFullRedrawNeeded = true;
        }
    }

    private boolean updateContentDrawBounds() {
        boolean updated = false;
        boolean z = true;
        if (this.mUseMTRenderer) {
            for (int i = this.mWindowCallbacks.size() - 1; i >= 0; i--) {
                updated |= this.mWindowCallbacks.get(i).onContentDrawn(this.mWindowAttributes.surfaceInsets.left, this.mWindowAttributes.surfaceInsets.top, this.mWidth, this.mHeight);
            }
        }
        return updated | ((this.mDragResizing && this.mReportNextDraw) ? false : false);
    }

    private void requestDrawWindow() {
        if (!this.mUseMTRenderer) {
            return;
        }
        if (this.mReportNextDraw) {
            this.mWindowDrawCountDown = new CountDownLatch(this.mWindowCallbacks.size());
        }
        for (int i = this.mWindowCallbacks.size() - 1; i >= 0; i--) {
            this.mWindowCallbacks.get(i).onRequestDraw(this.mReportNextDraw);
        }
    }

    public void reportActivityRelaunched() {
        this.mActivityRelaunched = true;
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public IBinder getInputToken() {
        WindowInputEventReceiver windowInputEventReceiver = this.mInputEventReceiver;
        if (windowInputEventReceiver == null) {
            return null;
        }
        return windowInputEventReceiver.getToken();
    }

    public IBinder getWindowToken() {
        return this.mAttachInfo.mWindowToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class AccessibilityInteractionConnectionManager implements AccessibilityManager.AccessibilityStateChangeListener {
        AccessibilityInteractionConnectionManager() {
        }

        @Override // android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
        public void onAccessibilityStateChanged(boolean enabled) {
            if (enabled) {
                ensureConnection();
                if (ViewRootImpl.this.mAttachInfo.mHasWindowFocus && ViewRootImpl.this.mView != null) {
                    ViewRootImpl.this.mView.sendAccessibilityEvent(32);
                    View focusedView = ViewRootImpl.this.mView.findFocus();
                    if (focusedView != null && focusedView != ViewRootImpl.this.mView) {
                        focusedView.sendAccessibilityEvent(8);
                    }
                }
                if (ViewRootImpl.this.mAttachInfo.mLeashedParentToken != null) {
                    ViewRootImpl.this.mAccessibilityManager.associateEmbeddedHierarchy(ViewRootImpl.this.mAttachInfo.mLeashedParentToken, ViewRootImpl.this.mLeashToken);
                    return;
                }
                return;
            }
            ensureNoConnection();
            ViewRootImpl.this.mHandler.obtainMessage(21).sendToTarget();
        }

        public void ensureConnection() {
            boolean registered = ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != -1;
            if (!registered) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = ViewRootImpl.this.mAccessibilityManager.addAccessibilityInteractionConnection(ViewRootImpl.this.mWindow, ViewRootImpl.this.mLeashToken, ViewRootImpl.this.mContext.getPackageName(), new AccessibilityInteractionConnection(ViewRootImpl.this));
            }
        }

        public void ensureNoConnection() {
            boolean registered = ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId != -1;
            if (registered) {
                ViewRootImpl.this.mAttachInfo.mAccessibilityWindowId = -1;
                ViewRootImpl.this.mAccessibilityManager.removeAccessibilityInteractionConnection(ViewRootImpl.this.mWindow);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class HighContrastTextManager implements AccessibilityManager.HighTextContrastChangeListener {
        HighContrastTextManager() {
            ThreadedRenderer.setHighContrastText(ViewRootImpl.this.mAccessibilityManager.isHighTextContrastEnabled());
        }

        @Override // android.view.accessibility.AccessibilityManager.HighTextContrastChangeListener
        public void onHighTextContrastStateChanged(boolean enabled) {
            ThreadedRenderer.setHighContrastText(enabled);
            ViewRootImpl.this.destroyHardwareResources();
            ViewRootImpl.this.invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static final class AccessibilityInteractionConnection extends IAccessibilityInteractionConnection.Stub {
        private final WeakReference<ViewRootImpl> mViewRootImpl;

        AccessibilityInteractionConnection(ViewRootImpl viewRootImpl) {
            this.mViewRootImpl = new WeakReference<>(viewRootImpl);
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void findAccessibilityNodeInfoByAccessibilityId(long accessibilityNodeId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrix, Bundle args) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfoByAccessibilityIdClientThread(accessibilityNodeId, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec, matrix, args);
                return;
            }
            try {
                callback.setFindAccessibilityNodeInfosResult(null, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void performAccessibilityAction(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().performAccessibilityActionClientThread(accessibilityNodeId, action, arguments, interactionId, callback, flags, interrogatingPid, interrogatingTid);
                return;
            }
            try {
                callback.setPerformAccessibilityActionResult(false, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void findAccessibilityNodeInfosByViewId(long accessibilityNodeId, String viewId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrix) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByViewIdClientThread(accessibilityNodeId, viewId, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec, matrix);
                return;
            }
            try {
                callback.setFindAccessibilityNodeInfoResult(null, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void findAccessibilityNodeInfosByText(long accessibilityNodeId, String text, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrix) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findAccessibilityNodeInfosByTextClientThread(accessibilityNodeId, text, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec, matrix);
                return;
            }
            try {
                callback.setFindAccessibilityNodeInfosResult(null, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void findFocus(long accessibilityNodeId, int focusType, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrix) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().findFocusClientThread(accessibilityNodeId, focusType, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec, matrix);
                return;
            }
            try {
                callback.setFindAccessibilityNodeInfoResult(null, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void focusSearch(long accessibilityNodeId, int direction, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrix) {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().focusSearchClientThread(accessibilityNodeId, direction, interactiveRegion, interactionId, callback, flags, interrogatingPid, interrogatingTid, spec, matrix);
                return;
            }
            try {
                callback.setFindAccessibilityNodeInfoResult(null, interactionId);
            } catch (RemoteException e) {
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void clearAccessibilityFocus() {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().clearAccessibilityFocusClientThread();
            }
        }

        @Override // android.view.accessibility.IAccessibilityInteractionConnection
        public void notifyOutsideTouch() {
            ViewRootImpl viewRootImpl = this.mViewRootImpl.get();
            if (viewRootImpl != null && viewRootImpl.mView != null) {
                viewRootImpl.getAccessibilityInteractionController().notifyOutsideTouchClientThread();
            }
        }
    }

    public IAccessibilityEmbeddedConnection getAccessibilityEmbeddedConnection() {
        if (this.mAccessibilityEmbeddedConnection == null) {
            this.mAccessibilityEmbeddedConnection = new AccessibilityEmbeddedConnection(this);
        }
        return this.mAccessibilityEmbeddedConnection;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class SendWindowContentChangedAccessibilityEvent implements Runnable {
        private int mChangeTypes;
        public long mLastEventTimeMillis;
        public StackTraceElement[] mOrigin;
        public View mSource;

        private SendWindowContentChangedAccessibilityEvent() {
            this.mChangeTypes = 0;
        }

        @Override // java.lang.Runnable
        public void run() {
            View source = this.mSource;
            this.mSource = null;
            if (source == null) {
                Log.e(ViewRootImpl.TAG, "Accessibility content change has no source");
                return;
            }
            if (AccessibilityManager.getInstance(ViewRootImpl.this.mContext).isEnabled()) {
                this.mLastEventTimeMillis = SystemClock.uptimeMillis();
                AccessibilityEvent event = AccessibilityEvent.obtain();
                event.setEventType(2048);
                event.setContentChangeTypes(this.mChangeTypes);
                source.sendAccessibilityEventUnchecked(event);
            } else {
                this.mLastEventTimeMillis = 0L;
            }
            source.resetSubtreeAccessibilityStateChanged();
            this.mChangeTypes = 0;
        }

        public void runOrPost(View source, int changeType) {
            if (ViewRootImpl.this.mHandler.getLooper() != Looper.myLooper()) {
                CalledFromWrongThreadException e = new CalledFromWrongThreadException("Only the original thread that created a view hierarchy can touch its views.");
                Log.e(ViewRootImpl.TAG, "Accessibility content change on non-UI thread. Future Android versions will throw an exception.", e);
                ViewRootImpl.this.mHandler.removeCallbacks(this);
                if (this.mSource != null) {
                    run();
                }
            }
            View view = this.mSource;
            if (view != null) {
                View predecessor = ViewRootImpl.this.getCommonPredecessor(view, source);
                if (predecessor != null) {
                    predecessor = predecessor.getSelfOrParentImportantForA11y();
                }
                this.mSource = predecessor != null ? predecessor : source;
                this.mChangeTypes |= changeType;
                return;
            }
            this.mSource = source;
            this.mChangeTypes = changeType;
            long timeSinceLastMillis = SystemClock.uptimeMillis() - this.mLastEventTimeMillis;
            long minEventIntevalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();
            if (timeSinceLastMillis >= minEventIntevalMillis) {
                removeCallbacksAndRun();
            } else {
                ViewRootImpl.this.mHandler.postDelayed(this, minEventIntevalMillis - timeSinceLastMillis);
            }
        }

        public void removeCallbacksAndRun() {
            ViewRootImpl.this.mHandler.removeCallbacks(this);
            run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class UnhandledKeyManager {
        private final SparseArray<WeakReference<View>> mCapturedKeys;
        private WeakReference<View> mCurrentReceiver;
        private boolean mDispatched;

        private UnhandledKeyManager() {
            this.mDispatched = true;
            this.mCapturedKeys = new SparseArray<>();
            this.mCurrentReceiver = null;
        }

        boolean dispatch(View root, KeyEvent event) {
            if (this.mDispatched) {
                return false;
            }
            try {
                Trace.traceBegin(8L, "UnhandledKeyEvent dispatch");
                this.mDispatched = true;
                View consumer = root.dispatchUnhandledKeyEvent(event);
                if (event.getAction() == 0) {
                    int keycode = event.getKeyCode();
                    if (consumer != null && !KeyEvent.isModifierKey(keycode)) {
                        this.mCapturedKeys.put(keycode, new WeakReference<>(consumer));
                    }
                }
                return consumer != null;
            } finally {
                Trace.traceEnd(8L);
            }
        }

        void preDispatch(KeyEvent event) {
            int idx;
            this.mCurrentReceiver = null;
            if (event.getAction() == 1 && (idx = this.mCapturedKeys.indexOfKey(event.getKeyCode())) >= 0) {
                this.mCurrentReceiver = this.mCapturedKeys.valueAt(idx);
                this.mCapturedKeys.removeAt(idx);
            }
        }

        boolean preViewDispatch(KeyEvent event) {
            this.mDispatched = false;
            if (this.mCurrentReceiver == null) {
                this.mCurrentReceiver = this.mCapturedKeys.get(event.getKeyCode());
            }
            WeakReference<View> weakReference = this.mCurrentReceiver;
            if (weakReference == null) {
                return false;
            }
            View target = weakReference.get();
            if (event.getAction() == 1) {
                this.mCurrentReceiver = null;
            }
            if (target != null && target.isAttachedToWindow()) {
                target.onUnhandledKeyEvent(event);
            }
            return true;
        }
    }

    public void setDisplayDecoration(boolean displayDecoration) {
        if (displayDecoration == this.mDisplayDecorationCached) {
            return;
        }
        this.mDisplayDecorationCached = displayDecoration;
        if (this.mSurfaceControl.isValid()) {
            updateDisplayDecoration();
        }
    }

    private void updateDisplayDecoration() {
        this.mTransaction.setDisplayDecoration(this.mSurfaceControl, this.mDisplayDecorationCached).apply();
    }

    public void dispatchBlurRegions(float[][] regionCopy, long frameNumber) {
        BLASTBufferQueue bLASTBufferQueue;
        SurfaceControl surfaceControl = getSurfaceControl();
        if (!surfaceControl.isValid()) {
            return;
        }
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setBlurRegions(surfaceControl, regionCopy);
        if (useBLAST() && (bLASTBufferQueue = this.mBlastBufferQueue) != null) {
            bLASTBufferQueue.mergeWithNextTransaction(transaction, frameNumber);
        }
    }

    public BackgroundBlurDrawable createBackgroundBlurDrawable() {
        return this.mBlurRegionAggregator.createBackgroundBlurDrawable(this.mContext);
    }

    @Override // android.view.ViewParent
    public void onDescendantUnbufferedRequested() {
        this.mUnbufferedInputSource = this.mView.mUnbufferedInputSource;
    }

    void forceDisableBLAST() {
        this.mForceDisableBLAST = true;
    }

    boolean useBLAST() {
        return this.mUseBLASTAdapter && !this.mForceDisableBLAST;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSurfaceSequenceId() {
        return this.mSurfaceSequenceId;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: mergeWithNextTransaction */
    public void m5128lambda$applyTransactionOnDraw$11$androidviewViewRootImpl(SurfaceControl.Transaction t, long frameNumber) {
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null) {
            bLASTBufferQueue.mergeWithNextTransaction(t, frameNumber);
        } else {
            t.apply();
        }
    }

    @Override // android.view.AttachedSurfaceControl
    public SurfaceControl.Transaction buildReparentTransaction(SurfaceControl child) {
        if (this.mSurfaceControl.isValid()) {
            return new SurfaceControl.Transaction().reparent(child, this.mSurfaceControl);
        }
        return null;
    }

    @Override // android.view.AttachedSurfaceControl
    public boolean applyTransactionOnDraw(final SurfaceControl.Transaction t) {
        if (this.mRemoved || !isHardwareEnabled()) {
            t.apply();
        } else {
            this.mHasPendingTransactions = true;
            registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda1
                @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
                public final void onFrameDraw(long j) {
                    ViewRootImpl.this.m5128lambda$applyTransactionOnDraw$11$androidviewViewRootImpl(t, j);
                }
            });
        }
        return true;
    }

    @Override // android.view.AttachedSurfaceControl
    public int getBufferTransformHint() {
        return this.mSurfaceControl.getTransformHint();
    }

    @Override // android.view.AttachedSurfaceControl
    public void addOnBufferTransformHintChangedListener(AttachedSurfaceControl.OnBufferTransformHintChangedListener listener) {
        Objects.requireNonNull(listener);
        if (this.mTransformHintListeners.contains(listener)) {
            throw new IllegalArgumentException("attempt to call addOnBufferTransformHintChangedListener() with a previously registered listener");
        }
        this.mTransformHintListeners.add(listener);
    }

    @Override // android.view.AttachedSurfaceControl
    public void removeOnBufferTransformHintChangedListener(AttachedSurfaceControl.OnBufferTransformHintChangedListener listener) {
        Objects.requireNonNull(listener);
        this.mTransformHintListeners.remove(listener);
    }

    private void dispatchTransformHintChanged(int hint) {
        if (this.mTransformHintListeners.isEmpty()) {
            return;
        }
        ArrayList<AttachedSurfaceControl.OnBufferTransformHintChangedListener> listeners = (ArrayList) this.mTransformHintListeners.clone();
        for (int i = 0; i < listeners.size(); i++) {
            AttachedSurfaceControl.OnBufferTransformHintChangedListener listener = listeners.get(i);
            listener.onBufferTransformHintChanged(hint);
        }
    }

    public void requestCompatCameraControl(boolean showControl, boolean transformationApplied, ICompatCameraControlCallback callback) {
        this.mActivityConfigCallback.requestCompatCameraControl(showControl, transformationApplied, callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean wasRelayoutRequested() {
        return this.mRelayoutRequested;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceWmRelayout() {
        this.mForceNextWindowRelayout = true;
        scheduleTraversals();
    }

    public WindowOnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return this.mOnBackInvokedDispatcher;
    }

    @Override // android.view.ViewParent
    public OnBackInvokedDispatcher findOnBackInvokedDispatcherForChild(View child, View requester) {
        return getOnBackInvokedDispatcher();
    }

    private void registerBackCallbackOnWindow() {
        this.mOnBackInvokedDispatcher.attachToWindow(this.mWindowSession, this.mWindow);
    }

    private void sendBackKeyEvent(int action) {
        long when = SystemClock.uptimeMillis();
        KeyEvent ev = new KeyEvent(when, when, action, 4, 0, 0, -1, 0, 72, 257);
        enqueueInputEvent(ev);
    }

    private void registerCompatOnBackInvokedCallback() {
        CompatOnBackInvokedCallback compatOnBackInvokedCallback = new CompatOnBackInvokedCallback() { // from class: android.view.ViewRootImpl$$ExternalSyntheticLambda15
            @Override // android.window.CompatOnBackInvokedCallback, android.window.OnBackInvokedCallback
            public final void onBackInvoked() {
                ViewRootImpl.this.m5134x15397ea9();
            }
        };
        this.mCompatOnBackInvokedCallback = compatOnBackInvokedCallback;
        this.mOnBackInvokedDispatcher.registerOnBackInvokedCallback(0, compatOnBackInvokedCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerCompatOnBackInvokedCallback$12$android-view-ViewRootImpl  reason: not valid java name */
    public /* synthetic */ void m5134x15397ea9() {
        sendBackKeyEvent(0);
        sendBackKeyEvent(1);
    }

    @Override // android.view.AttachedSurfaceControl
    public void setTouchableRegion(Region r) {
        if (r != null) {
            this.mTouchableRegion = new Region(r);
        } else {
            this.mTouchableRegion = null;
        }
        this.mLastGivenInsets.reset();
        requestLayout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IWindowSession getWindowSession() {
        return this.mWindowSession;
    }

    private void registerCallbacksForSync(boolean syncBuffer, SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        if (!isHardwareEnabled()) {
            return;
        }
        if (DEBUG_BLAST) {
            Log.d(this.mTag, "registerCallbacksForSync syncBuffer=" + syncBuffer);
        }
        this.mAttachInfo.mThreadedRenderer.registerRtFrameCallback(new AnonymousClass8(syncBufferCallback, syncBuffer));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.view.ViewRootImpl$8  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass8 implements HardwareRenderer.FrameDrawingCallback {
        final /* synthetic */ boolean val$syncBuffer;
        final /* synthetic */ SurfaceSyncer.SyncBufferCallback val$syncBufferCallback;

        AnonymousClass8(SurfaceSyncer.SyncBufferCallback syncBufferCallback, boolean z) {
            this.val$syncBufferCallback = syncBufferCallback;
            this.val$syncBuffer = z;
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public void onFrameDraw(long frame) {
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public HardwareRenderer.FrameCommitCallback onFrameDraw(int syncResult, final long frame) {
            if (ViewRootImpl.DEBUG_BLAST) {
                Log.d(ViewRootImpl.this.mTag, "Received frameDrawingCallback syncResult=" + syncResult + " frameNum=" + frame + MediaMetrics.SEPARATOR);
            }
            if ((syncResult & 6) != 0) {
                this.val$syncBufferCallback.onBufferReady(ViewRootImpl.this.mBlastBufferQueue.gatherPendingTransactions(frame));
                return null;
            }
            if (ViewRootImpl.DEBUG_BLAST) {
                Log.d(ViewRootImpl.this.mTag, "Setting up sync and frameCommitCallback");
            }
            if (this.val$syncBuffer) {
                BLASTBufferQueue bLASTBufferQueue = ViewRootImpl.this.mBlastBufferQueue;
                SurfaceSyncer.SyncBufferCallback syncBufferCallback = this.val$syncBufferCallback;
                Objects.requireNonNull(syncBufferCallback);
                bLASTBufferQueue.syncNextTransaction(new ViewRootImpl$8$$ExternalSyntheticLambda0(syncBufferCallback));
            }
            final SurfaceSyncer.SyncBufferCallback syncBufferCallback2 = this.val$syncBufferCallback;
            final boolean z = this.val$syncBuffer;
            return new HardwareRenderer.FrameCommitCallback() { // from class: android.view.ViewRootImpl$8$$ExternalSyntheticLambda1
                @Override // android.graphics.HardwareRenderer.FrameCommitCallback
                public final void onFrameCommit(boolean z2) {
                    ViewRootImpl.AnonymousClass8.this.m5136lambda$onFrameDraw$0$androidviewViewRootImpl$8(frame, syncBufferCallback2, z, z2);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFrameDraw$0$android-view-ViewRootImpl$8  reason: not valid java name */
        public /* synthetic */ void m5136lambda$onFrameDraw$0$androidviewViewRootImpl$8(long frame, SurfaceSyncer.SyncBufferCallback syncBufferCallback, boolean syncBuffer, boolean didProduceBuffer) {
            if (ViewRootImpl.DEBUG_BLAST) {
                Log.d(ViewRootImpl.this.mTag, "Received frameCommittedCallback lastAttemptedDrawFrameNum=" + frame + " didProduceBuffer=" + didProduceBuffer);
            }
            ViewRootImpl.this.mFrameScenario.setAction(3).setRenderThreadId(Process.myTid());
            if (!didProduceBuffer) {
                ViewRootImpl.this.mBlastBufferQueue.syncNextTransaction(null);
                syncBufferCallback.onBufferReady(ViewRootImpl.this.mBlastBufferQueue.gatherPendingTransactions(frame));
            } else if (!syncBuffer) {
                syncBufferCallback.onBufferReady(null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.view.ViewRootImpl$9  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass9 implements SurfaceSyncer.SyncTarget {
        AnonymousClass9() {
        }

        @Override // android.window.SurfaceSyncer.SyncTarget
        public void onReadyToSync(SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
            ViewRootImpl.this.readyToSync(syncBufferCallback);
        }

        @Override // android.window.SurfaceSyncer.SyncTarget
        public void onSyncComplete() {
            ViewRootImpl.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: android.view.ViewRootImpl$9$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ViewRootImpl.AnonymousClass9.this.m5137lambda$onSyncComplete$0$androidviewViewRootImpl$9();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSyncComplete$0$android-view-ViewRootImpl$9  reason: not valid java name */
        public /* synthetic */ void m5137lambda$onSyncComplete$0$androidviewViewRootImpl$9() {
            ViewRootImpl viewRootImpl = ViewRootImpl.this;
            int i = viewRootImpl.mNumSyncsInProgress - 1;
            viewRootImpl.mNumSyncsInProgress = i;
            if (i == 0 && ViewRootImpl.this.mAttachInfo.mThreadedRenderer != null) {
                HardwareRenderer.setRtAnimationsEnabled(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readyToSync(SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        this.mNumSyncsInProgress++;
        if (!isInLocalSync()) {
            this.mSyncBuffer = true;
        }
        if (this.mAttachInfo.mThreadedRenderer != null) {
            HardwareRenderer.setRtAnimationsEnabled(false);
        }
        if (this.mSyncBufferCallback != null) {
            Log.d(this.mTag, "Already set sync for the next draw.");
            this.mSyncBufferCallback.onBufferReady(null);
        }
        if (DEBUG_BLAST) {
            Log.d(this.mTag, "Setting syncFrameCallback");
        }
        this.mSyncBufferCallback = syncBufferCallback;
        if (!this.mIsInTraversal && !this.mTraversalScheduled) {
            scheduleTraversals();
        }
    }

    private boolean inRTCloudBlacklist(String packageName) {
        String rtVersion = SystemProperties.get("ro.vendor.scaler_version", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
        String blacklistSettings = Settings.Global.getString(this.mContext.getContentResolver(), GRIFFIN_CLOUD_APP_RT_BLACKLIST_SETTINGS + rtVersion);
        if (blacklistSettings != null) {
            Set<String> blacklist = getBlacklistFromCloud(blacklistSettings);
            if (blacklist.size() != 0) {
                return blacklist.contains(packageName);
            }
            return false;
        }
        return false;
    }

    private boolean getRTCloudSwitch() {
        String rtVersion = SystemProperties.get("ro.vendor.scaler_version", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
        int rtSwitch = Settings.Global.getInt(this.mContext.getContentResolver(), GRIFFIN_CLOUD_APP_RT_SWITCH_SETTINGS + rtVersion, 1);
        return rtSwitch == 1;
    }

    private Set<String> getBlacklistFromCloud(String config) {
        if (config != null && config.trim().length() != 0) {
            String[] packages = config.split(NavigationBarInflaterView.GRAVITY_SEPARATOR);
            Set<String> blacklist = new ArraySet<>();
            for (String pkg : packages) {
                if (pkg.trim().length() != 0) {
                    blacklist.add(pkg);
                }
            }
            return blacklist;
        }
        return new ArraySet();
    }

    public boolean shouldShowCaptionView() {
        return this.mShowCaptionView;
    }

    public void handleCaptionViewStatus(boolean show, int multiwindowMode) {
        if (ThunderbackConfig.isVersion3()) {
            this.mShowCaptionView = show;
            View view = this.mView;
            if (view != null && (view instanceof DecorView)) {
                ((DecorView) view).setDecorViewInThunderBack(show);
            }
            if (this.mView != null) {
                Log.d(TAG, "handleCaptionViewStatus multiwindowMode: " + multiwindowMode);
                this.mLastReportedMergedConfiguration.getMergedConfiguration().windowConfiguration.setMultiWindowMode(multiwindowMode);
                this.mLastConfigurationFromResources.windowConfiguration.setMultiWindowMode(multiwindowMode);
                getConfiguration().windowConfiguration.setMultiWindowMode(multiwindowMode);
                this.mView.dispatchConfigurationChanged(this.mLastReportedMergedConfiguration.getMergedConfiguration());
                return;
            }
            return;
        }
        ThunderbackConfig.isVersion4();
    }

    public WindowConfiguration getLastResourceConfiguration() {
        return this.mLastConfigurationFromResources.windowConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void mergeSync(int syncId, SurfaceSyncer otherSyncer) {
        if (!isInLocalSync()) {
            return;
        }
        this.mSurfaceSyncer.merge(this.mSyncId, syncId, otherSyncer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class TranFoldInnerCustody {
        private Long mReporteSFPhysicalId;

        private TranFoldInnerCustody() {
        }

        public void handleResizedConfigurationChanged(MergedConfiguration mergedConfiguration) {
            if (disable()) {
                return;
            }
            long lastDisplayPhysicalId = ViewRootImpl.this.mLastReportedMergedConfiguration.getMergedConfiguration().windowConfiguration.getDisplayPhysicalId();
            long currentDisplayPhysicalId = mergedConfiguration.getMergedConfiguration().windowConfiguration.getDisplayPhysicalId();
            if (lastDisplayPhysicalId != currentDisplayPhysicalId) {
                Slog.d(tag(), ViewRootImpl.this + " handleResized reportNextDraw when  DisplayPhysicalId modified from " + lastDisplayPhysicalId + " to " + currentDisplayPhysicalId);
                ViewRootImpl.this.reportNextDraw();
            }
        }

        public void handleRelayout(long lastDisplayPhysicalId) {
            if (disable()) {
                return;
            }
            long currentDisplayPhysicalId = ViewRootImpl.this.mPendingMergedConfiguration.getMergedConfiguration().windowConfiguration.getDisplayPhysicalId();
            if (lastDisplayPhysicalId != currentDisplayPhysicalId) {
                if (debug()) {
                    Slog.d(tag(), ViewRootImpl.this + " relayoutWindow reportNextDraw when DisplayPhysicalId modified from " + lastDisplayPhysicalId + " to " + currentDisplayPhysicalId);
                }
                ViewRootImpl.this.reportNextDraw();
                if (currentDisplayPhysicalId != 0) {
                    this.mReporteSFPhysicalId = Long.valueOf(currentDisplayPhysicalId);
                }
                notifySFPhysicalId();
            }
        }

        public void notifySFPhysicalId() {
            if (this.mReporteSFPhysicalId == null) {
                return;
            }
            if (!ViewRootImpl.this.mSurfaceControl.isValid()) {
                Slog.e(tag(), ViewRootImpl.this + " notifySFPhysicalId mSurfaceControl invalid");
                return;
            }
            Parcel parcel = Parcel.obtain();
            parcel.writeLong(this.mReporteSFPhysicalId.longValue());
            try {
                ViewRootImpl.this.mSurfaceChangedTransaction.setMetadata(ViewRootImpl.this.mSurfaceControl, TranFoldViewCustody.TRANSACTION_META_KEY_DISPLAY_PHYSICAL_ID, parcel);
                parcel.recycle();
                if (debug()) {
                    Slog.d(tag(), ViewRootImpl.this + " notifySFPhysicalId " + this.mReporteSFPhysicalId);
                }
                this.mReporteSFPhysicalId = null;
            } catch (Throwable th) {
                parcel.recycle();
                throw th;
            }
        }

        private boolean disable() {
            return TranFoldViewCustody.disable();
        }

        private boolean debug() {
            return true;
        }

        private String tag() {
            return TranFoldViewCustody.TAG;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11920=7, 11921=7, 11922=7] */
    boolean delayProcessMotionEvent() {
        long now = SystemClock.uptimeMillis();
        try {
            long j = this.mLastDelayTimestamp;
            if (j != -1 && now - j > DELAY_TIME) {
                long coast = SystemClock.uptimeMillis() - now;
                if (coast > 100) {
                    Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast + "ms");
                }
                return false;
            }
            QueuedInputEvent q = this.mPendingInputEventHead;
            if (q == null) {
                long coast2 = SystemClock.uptimeMillis() - now;
                if (coast2 > 100) {
                    Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast2 + "ms");
                }
                return false;
            } else if (!(q.mEvent instanceof MotionEvent)) {
                long coast3 = SystemClock.uptimeMillis() - now;
                if (coast3 > 100) {
                    Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast3 + "ms");
                }
                return false;
            } else {
                MotionEvent motionEvent = (MotionEvent) q.mEvent;
                if (motionEvent.getAction() != 0) {
                    long coast4 = SystemClock.uptimeMillis() - now;
                    if (coast4 > 100) {
                        Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast4 + "ms");
                    }
                    return false;
                } else if (!IMessageBankLice.Instance().hasSelectedText(this.mContext)) {
                    long coast5 = SystemClock.uptimeMillis() - now;
                    if (coast5 > 100) {
                        Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast5 + "ms");
                    }
                    return false;
                } else {
                    Log.d(TAG_GESTURE, "delayProcessMotionEvent: There was a selected text, may delay dispatch motion event");
                    boolean delay = true;
                    long targetCancelEventDowntime = -1;
                    while (true) {
                        QueuedInputEvent queuedInputEvent = q.mNext;
                        q = queuedInputEvent;
                        if (queuedInputEvent == null) {
                            break;
                        } else if (q.mEvent instanceof MotionEvent) {
                            MotionEvent me = (MotionEvent) q.mEvent;
                            if (me.getAction() == 1) {
                                delay = false;
                                break;
                            } else if (me.getAction() == 3) {
                                targetCancelEventDowntime = me.getDownTime();
                                break;
                            }
                        }
                    }
                    if (targetCancelEventDowntime != -1) {
                        Log.d(TAG_GESTURE, "try remove pre motion event");
                        QueuedInputEvent pre = this.mPendingInputEventHead;
                        QueuedInputEvent q2 = pre;
                        while (q2 != null) {
                            boolean remveHead = false;
                            if (q2.mEvent instanceof MotionEvent) {
                                if (((MotionEvent) q2.mEvent).getDownTime() == targetCancelEventDowntime) {
                                    if (this.mPendingInputEventHead == q2) {
                                        QueuedInputEvent queuedInputEvent2 = q2.mNext;
                                        this.mPendingInputEventHead = queuedInputEvent2;
                                        if (queuedInputEvent2 == null) {
                                            this.mPendingInputEventTail = null;
                                        }
                                        pre = queuedInputEvent2;
                                        remveHead = true;
                                    } else {
                                        pre.mNext = q2.mNext;
                                    }
                                    this.mPendingInputEventCount--;
                                    q2.mFlags |= 8;
                                    finishInputEvent(q2);
                                }
                            } else if (pre != q2) {
                                pre = pre.mNext;
                            }
                            q2 = !remveHead ? pre.mNext : pre;
                        }
                    }
                    return delay;
                }
            }
        } finally {
            long coast6 = SystemClock.uptimeMillis() - now;
            if (coast6 > 100) {
                Log.d(TAG_GESTURE, "delayProcessMotionEvent coast " + coast6 + "ms");
            }
        }
    }
}
