package android.inputmethodservice;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.AbstractInputMethodService;
import android.inputmethodservice.InkWindow;
import android.inputmethodservice.InputMethodService;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.MovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.window.CompatOnBackInvokedCallback;
import android.window.ImeOnBackInvokedDispatcher;
import android.window.WindowMetricsHelper;
import com.android.internal.R;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.inputmethod.ImeTracing;
import com.android.internal.inputmethod.InputMethodPrivilegedOperations;
import com.android.internal.inputmethod.InputMethodPrivilegedOperationsRegistry;
import com.android.internal.util.RingBuffer;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInputContext;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.scenario.ime.IMEScenario;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class InputMethodService extends AbstractInputMethodService {
    public static final int BACK_DISPOSITION_ADJUST_NOTHING = 3;
    public static final int BACK_DISPOSITION_DEFAULT = 0;
    private static final int BACK_DISPOSITION_MAX = 3;
    private static final int BACK_DISPOSITION_MIN = 0;
    @Deprecated
    public static final int BACK_DISPOSITION_WILL_DISMISS = 2;
    @Deprecated
    public static final int BACK_DISPOSITION_WILL_NOT_DISMISS = 1;
    static final boolean DEBUG;
    public static final long FINISH_INPUT_NO_FALLBACK_CONNECTION = 156215187;
    public static final int IME_ACTIVE = 1;
    public static final int IME_INVISIBLE = 4;
    public static final int IME_VISIBLE = 2;
    public static final int IME_VISIBLE_IMPERCEPTIBLE = 8;
    private static final int MAX_EVENTS_BUFFER = 500;
    static final int MOVEMENT_DOWN = -1;
    static final int MOVEMENT_UP = -2;
    private static final String PROP_CAN_RENDER_GESTURAL_NAV_BUTTONS = "persist.sys.ime.can_render_gestural_nav_buttons";
    static final String TAG = "InputMethodService";
    private static final long TIMEOUT_SURFACE_REMOVAL_MILLIS = 500;
    int mBackDisposition;
    FrameLayout mCandidatesFrame;
    boolean mCandidatesViewStarted;
    int mCandidatesVisibility;
    CompletionInfo[] mCurCompletions;
    private IBinder mCurHideInputToken;
    private IBinder mCurShowInputToken;
    boolean mDecorViewVisible;
    boolean mDecorViewWasVisible;
    private boolean mDestroyed;
    ViewGroup mExtractAccessories;
    View mExtractAction;
    ExtractEditText mExtractEditText;
    FrameLayout mExtractFrame;
    View mExtractView;
    boolean mExtractViewHidden;
    ExtractedText mExtractedText;
    int mExtractedToken;
    boolean mFullscreenApplied;
    ViewGroup mFullscreenArea;
    private Handler mHandler;
    private InputEventReceiver mHandwritingEventReceiver;
    private boolean mHideNavBarForKeyboard;
    private ImeOnBackInvokedDispatcher mImeDispatcher;
    private boolean mImeSurfaceScheduledForRemoval;
    InputMethodManager mImm;
    boolean mInShowWindow;
    LayoutInflater mInflater;
    boolean mInitialized;
    private InkWindow mInkWindow;
    private InlineSuggestionSessionController mInlineSuggestionSessionController;
    InputBinding mInputBinding;
    InputConnection mInputConnection;
    EditorInfo mInputEditorInfo;
    FrameLayout mInputFrame;
    boolean mInputStarted;
    View mInputView;
    boolean mInputViewStarted;
    private boolean mIsAutomotive;
    boolean mIsFullscreen;
    boolean mIsInputViewShown;
    boolean mLastShowInputRequested;
    private boolean mLastWasInFullscreenMode;
    private boolean mNotifyUserActionSent;
    private boolean mOnPreparedStylusHwCalled;
    private RingBuffer<MotionEvent> mPendingEvents;
    View mRootView;
    private SettingsObserver mSettingsObserver;
    int mShowInputFlags;
    boolean mShowInputRequested;
    InputConnection mStartedInputConnection;
    int mStatusIcon;
    TypedArray mThemeAttrs;
    IBinder mToken;
    boolean mViewsCreated;
    SoftInputWindow mWindow;
    boolean mWindowVisible;
    private Boolean mBackCallbackRegistered = false;
    private final CompatOnBackInvokedCallback mCompatBackCallback = new CompatOnBackInvokedCallback() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda2
        @Override // android.window.CompatOnBackInvokedCallback, android.window.OnBackInvokedCallback
        public final void onBackInvoked() {
            InputMethodService.this.compatHandleBack();
        }
    };
    private InputMethodPrivilegedOperations mPrivOps = new InputMethodPrivilegedOperations();
    private final NavigationBarController mNavigationBarController = new NavigationBarController(this);
    int mTheme = 0;
    private Object mLock = new Object();
    final Insets mTmpInsets = new Insets();
    final int[] mTmpLocation = new int[2];
    private OptionalInt mHandwritingRequestId = OptionalInt.empty();
    private ImsConfigurationTracker mConfigTracker = new ImsConfigurationTracker();
    private final IMEScenario mImeScenario = new IMEScenario();
    final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda3
        @Override // android.view.ViewTreeObserver.OnComputeInternalInsetsListener
        public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
            InputMethodService.this.m1708lambda$new$0$androidinputmethodserviceInputMethodService(internalInsetsInfo);
        }
    };
    final View.OnClickListener mActionClickListener = new View.OnClickListener() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda4
        @Override // android.view.View.OnClickListener
        public final void onClick(View view) {
            InputMethodService.this.m1709lambda$new$1$androidinputmethodserviceInputMethodService(view);
        }
    };
    private final ImeTracing.ServiceDumper mDumper = new ImeTracing.ServiceDumper() { // from class: android.inputmethodservice.InputMethodService.2
        @Override // com.android.internal.inputmethod.ImeTracing.ServiceDumper
        public void dumpToProto(ProtoOutputStream proto, byte[] icProto) {
            long token = proto.start(1146756268035L);
            InputMethodService.this.mWindow.dumpDebug(proto, 1146756268033L);
            proto.write(1133871366146L, InputMethodService.this.mViewsCreated);
            proto.write(1133871366147L, InputMethodService.this.mDecorViewVisible);
            proto.write(1133871366148L, InputMethodService.this.mDecorViewWasVisible);
            proto.write(1133871366149L, InputMethodService.this.mWindowVisible);
            proto.write(1133871366150L, InputMethodService.this.mInShowWindow);
            proto.write(1138166333447L, InputMethodService.this.getResources().getConfiguration().toString());
            proto.write(1138166333448L, Objects.toString(InputMethodService.this.mToken));
            proto.write(1138166333449L, Objects.toString(InputMethodService.this.mInputBinding));
            proto.write(1133871366154L, InputMethodService.this.mInputStarted);
            proto.write(1133871366155L, InputMethodService.this.mInputViewStarted);
            proto.write(1133871366156L, InputMethodService.this.mCandidatesViewStarted);
            if (InputMethodService.this.mInputEditorInfo != null) {
                InputMethodService.this.mInputEditorInfo.dumpDebug(proto, 1146756268045L);
            }
            proto.write(1133871366158L, InputMethodService.this.mShowInputRequested);
            proto.write(1133871366159L, InputMethodService.this.mLastShowInputRequested);
            proto.write(1120986464274L, InputMethodService.this.mShowInputFlags);
            proto.write(1120986464275L, InputMethodService.this.mCandidatesVisibility);
            proto.write(1133871366164L, InputMethodService.this.mFullscreenApplied);
            proto.write(1133871366165L, InputMethodService.this.mIsFullscreen);
            proto.write(1133871366166L, InputMethodService.this.mExtractViewHidden);
            proto.write(1120986464279L, InputMethodService.this.mExtractedToken);
            proto.write(1133871366168L, InputMethodService.this.mIsInputViewShown);
            proto.write(1120986464281L, InputMethodService.this.mStatusIcon);
            InputMethodService.this.mTmpInsets.dumpDebug(proto, 1146756268058L);
            proto.write(1138166333467L, Objects.toString(InputMethodService.this.mSettingsObserver));
            if (icProto != null) {
                proto.write(1146756268060L, icProto);
            }
            proto.end(token);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface BackDispositionMode {
    }

    static {
        DEBUG = "1".equals(SystemProperties.get("persist.sys.adb.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS)) || "1".equals(SystemProperties.get("persist.sys.fans.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
    }

    public static boolean canImeRenderGesturalNavButtons() {
        return SystemProperties.getBoolean(PROP_CAN_RENDER_GESTURAL_NAV_BUTTONS, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-inputmethodservice-InputMethodService  reason: not valid java name */
    public /* synthetic */ void m1708lambda$new$0$androidinputmethodserviceInputMethodService(ViewTreeObserver.InternalInsetsInfo info) {
        onComputeInsets(this.mTmpInsets);
        if (!this.mViewsCreated) {
            this.mTmpInsets.visibleTopInsets = 0;
        }
        if (isExtractViewShown()) {
            View decor = getWindow().getWindow().getDecorView();
            Rect rect = info.contentInsets;
            Rect rect2 = info.visibleInsets;
            int height = decor.getHeight();
            rect2.top = height;
            rect.top = height;
            info.touchableRegion.setEmpty();
            info.setTouchableInsets(0);
        } else {
            info.contentInsets.top = this.mTmpInsets.contentTopInsets;
            info.visibleInsets.top = this.mTmpInsets.visibleTopInsets;
            info.touchableRegion.set(this.mTmpInsets.touchableRegion);
            info.setTouchableInsets(this.mTmpInsets.touchableInsets);
        }
        this.mNavigationBarController.updateTouchableInsets(this.mTmpInsets, info);
        if (this.mInputFrame != null) {
            setImeExclusionRect(this.mTmpInsets.visibleTopInsets);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$android-inputmethodservice-InputMethodService  reason: not valid java name */
    public /* synthetic */ void m1709lambda$new$1$androidinputmethodserviceInputMethodService(View v) {
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei != null && ic != null) {
            if (ei.actionId != 0) {
                ic.performEditorAction(ei.actionId);
            } else if ((ei.imeOptions & 255) != 1) {
                ic.performEditorAction(ei.imeOptions & 255);
            }
        }
    }

    /* loaded from: classes2.dex */
    public class InputMethodImpl extends AbstractInputMethodService.AbstractInputMethodImpl {
        private boolean mSystemCallingHideSoftInput;
        private boolean mSystemCallingShowSoftInput;

        public InputMethodImpl() {
            super();
        }

        @Override // android.view.inputmethod.InputMethod
        public final void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privilegedOperations, int configChanges, boolean stylusHwSupported, int navButtonFlags) {
            if (InputMethodService.this.mDestroyed) {
                Log.i(InputMethod.TAG, "The InputMethodService has already onDestroyed().Ignore the initialization.");
                return;
            }
            Trace.traceBegin(32L, "IMS.initializeInternal");
            InputMethodService.this.mConfigTracker.onInitialize(configChanges);
            InputMethodService.this.mPrivOps.set(privilegedOperations);
            InputMethodPrivilegedOperationsRegistry.put(token, InputMethodService.this.mPrivOps);
            if (stylusHwSupported) {
                InputMethodService.this.mInkWindow = new InkWindow(InputMethodService.this.mWindow.getContext());
            }
            InputMethodService.this.mNavigationBarController.onNavButtonFlagsChanged(navButtonFlags);
            attachToken(token);
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
            if (InputMethodService.DEBUG) {
                Log.d(InputMethod.TAG, "InputMethodService received onCreateInlineSuggestionsRequest()");
            }
            InputMethodService.this.mInlineSuggestionSessionController.onMakeInlineSuggestionsRequest(requestInfo, cb);
        }

        @Override // android.view.inputmethod.InputMethod
        public void attachToken(IBinder token) {
            if (InputMethodService.this.mToken != null) {
                throw new IllegalStateException("attachToken() must be called at most once. token=" + token);
            }
            InputMethodService.this.attachToWindowToken(token);
            InputMethodService.this.mToken = token;
            InputMethodService.this.mWindow.setToken(token);
            if (InputMethodService.this.mInkWindow != null) {
                InputMethodService.this.mInkWindow.setToken(token);
            }
        }

        @Override // android.view.inputmethod.InputMethod
        public void bindInput(InputBinding binding) {
            Trace.traceBegin(32L, "IMS.bindInput");
            InputMethodService.this.mInputBinding = binding;
            InputMethodService.this.mInputConnection = binding.getConnection();
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "bindInput(): binding=" + binding + " ic=" + InputMethodService.this.mInputConnection);
            }
            InputMethodService.this.reportFullscreenMode();
            InputMethodService.this.initialize();
            InputMethodService.this.onBindInput();
            InputMethodService.this.mConfigTracker.onBindInput(InputMethodService.this.getResources());
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public void unbindInput() {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "unbindInput(): binding=" + InputMethodService.this.mInputBinding + " ic=" + InputMethodService.this.mInputConnection);
            }
            InputMethodService.this.onUnbindInput();
            InputMethodService.this.mInputBinding = null;
            InputMethodService.this.mInputConnection = null;
            if (InputMethodService.this.mInkWindow != null) {
                InputMethodService.this.mInkWindow.hide(true);
            }
        }

        @Override // android.view.inputmethod.InputMethod
        public void startInput(InputConnection ic, EditorInfo attribute) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "startInput(): editor=" + attribute);
            }
            Trace.traceBegin(32L, "IMS.startInput");
            InputMethodService.this.doStartInput(ic, attribute, false);
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public void restartInput(InputConnection ic, EditorInfo attribute) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "restartInput(): editor=" + attribute);
            }
            Trace.traceBegin(32L, "IMS.restartInput");
            InputMethodService.this.doStartInput(ic, attribute, true);
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public final void dispatchStartInputWithToken(InputConnection inputConnection, EditorInfo editorInfo, boolean restarting, IBinder startInputToken, int navButtonFlags, ImeOnBackInvokedDispatcher imeDispatcher) {
            InputMethodService.this.mPrivOps.reportStartInputAsync(startInputToken);
            InputMethodService.this.mNavigationBarController.onNavButtonFlagsChanged(navButtonFlags);
            if (restarting) {
                restartInput(inputConnection, editorInfo);
            } else {
                startInput(inputConnection, editorInfo);
            }
            InputMethodService.this.mImeDispatcher = imeDispatcher;
            if (InputMethodService.this.mWindow != null) {
                InputMethodService.this.mWindow.getOnBackInvokedDispatcher().setImeOnBackInvokedDispatcher(imeDispatcher);
            }
        }

        @Override // android.view.inputmethod.InputMethod
        public void onNavButtonFlagsChanged(int navButtonFlags) {
            InputMethodService.this.mNavigationBarController.onNavButtonFlagsChanged(navButtonFlags);
        }

        @Override // android.view.inputmethod.InputMethod
        public void hideSoftInputWithToken(int flags, ResultReceiver resultReceiver, IBinder hideInputToken) {
            this.mSystemCallingHideSoftInput = true;
            InputMethodService.this.mCurHideInputToken = hideInputToken;
            hideSoftInput(flags, resultReceiver);
            InputMethodService.this.mCurHideInputToken = null;
            this.mSystemCallingHideSoftInput = false;
        }

        @Override // android.view.inputmethod.InputMethod
        public void hideSoftInput(int flags, ResultReceiver resultReceiver) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "hideSoftInput()");
            }
            if (InputMethodService.this.getApplicationInfo().targetSdkVersion >= 30 && !this.mSystemCallingHideSoftInput) {
                Log.e(InputMethod.TAG, "IME shouldn't call hideSoftInput on itself. Use requestHideSelf(int) itself");
                return;
            }
            ImeTracing.getInstance().triggerServiceDump("InputMethodService.InputMethodImpl#hideSoftInput", InputMethodService.this.mDumper, null);
            boolean wasVisible = InputMethodService.this.isInputViewShown();
            Trace.traceBegin(32L, "IMS.hideSoftInput");
            int i = 0;
            InputMethodService.this.mShowInputFlags = 0;
            InputMethodService.this.mShowInputRequested = false;
            InputMethodService.this.hideWindow();
            boolean isVisible = InputMethodService.this.isInputViewShown();
            boolean visibilityChanged = isVisible != wasVisible;
            if (resultReceiver != null) {
                if (visibilityChanged) {
                    i = 3;
                } else if (!wasVisible) {
                    i = 1;
                }
                resultReceiver.send(i, null);
            }
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public void showSoftInputWithToken(int flags, ResultReceiver resultReceiver, IBinder showInputToken) {
            this.mSystemCallingShowSoftInput = true;
            InputMethodService.this.mCurShowInputToken = showInputToken;
            showSoftInput(flags, resultReceiver);
            InputMethodService.this.mCurShowInputToken = null;
            this.mSystemCallingShowSoftInput = false;
        }

        @Override // android.view.inputmethod.InputMethod
        public void showSoftInput(int flags, ResultReceiver resultReceiver) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "showSoftInput()");
            }
            if (InputMethodService.this.getApplicationInfo().targetSdkVersion >= 30 && !this.mSystemCallingShowSoftInput) {
                Log.e(InputMethod.TAG, " IME shouldn't call showSoftInput on itself. Use requestShowSelf(int) itself");
                return;
            }
            Trace.traceBegin(32L, "IMS.showSoftInput");
            ImeTracing.getInstance().triggerServiceDump("InputMethodService.InputMethodImpl#showSoftInput", InputMethodService.this.mDumper, null);
            boolean wasVisible = InputMethodService.this.isInputViewShown();
            int i = 0;
            if (InputMethodService.this.dispatchOnShowInputRequested(flags, false)) {
                InputMethodService.this.showWindow(true);
            }
            InputMethodService inputMethodService = InputMethodService.this;
            inputMethodService.setImeWindowStatus(inputMethodService.mapToImeWindowStatus(), InputMethodService.this.mBackDisposition);
            boolean isVisible = InputMethodService.this.isInputViewShown();
            boolean visibilityChanged = isVisible != wasVisible;
            if (resultReceiver != null) {
                if (visibilityChanged) {
                    i = 2;
                } else if (!wasVisible) {
                    i = 1;
                }
                resultReceiver.send(i, null);
            }
            Trace.traceEnd(32L);
        }

        @Override // android.view.inputmethod.InputMethod
        public void canStartStylusHandwriting(int requestId) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "canStartStylusHandwriting()");
            }
            if (InputMethodService.this.mHandwritingRequestId.isPresent()) {
                Log.d(InputMethod.TAG, "There is an ongoing Handwriting session. ignoring.");
            } else if (!InputMethodService.this.mInputStarted) {
                Log.d(InputMethod.TAG, "Input should have started before starting Stylus handwriting.");
            } else {
                if (!InputMethodService.this.mOnPreparedStylusHwCalled) {
                    InputMethodService.this.onPrepareStylusHandwriting();
                    InputMethodService.this.mOnPreparedStylusHwCalled = true;
                }
                if (InputMethodService.this.onStartStylusHandwriting()) {
                    InputMethodService.this.mPrivOps.onStylusHandwritingReady(requestId, Process.myPid());
                } else {
                    Log.i(InputMethod.TAG, "IME is not ready. Can't start Stylus Handwriting");
                }
            }
        }

        @Override // android.view.inputmethod.InputMethod
        public void startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> stylusEvents) {
            if (InputMethodService.DEBUG) {
                Log.v(InputMethod.TAG, "startStylusHandwriting()");
            }
            Objects.requireNonNull(channel);
            Objects.requireNonNull(stylusEvents);
            if (InputMethodService.this.mHandwritingRequestId.isPresent()) {
                return;
            }
            InputMethodService.this.mHandwritingRequestId = OptionalInt.of(requestId);
            InputMethodService.this.mShowInputRequested = false;
            InputMethodService.this.mInkWindow.show();
            final InputMethodService inputMethodService = InputMethodService.this;
            stylusEvents.forEach(new Consumer() { // from class: android.inputmethodservice.InputMethodService$InputMethodImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    InputMethodService.this.onStylusHandwritingMotionEvent((MotionEvent) obj);
                }
            });
            InputMethodService.this.mHandwritingEventReceiver = new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver(channel, Looper.getMainLooper(), Choreographer.getInstance(), new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver.InputEventListener() { // from class: android.inputmethodservice.InputMethodService$InputMethodImpl$$ExternalSyntheticLambda1
                @Override // android.view.BatchedInputEventReceiver.SimpleBatchedInputEventReceiver.InputEventListener
                public final boolean onInputEvent(InputEvent inputEvent) {
                    return InputMethodService.InputMethodImpl.this.m1712xe8915f74(inputEvent);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startStylusHandwriting$0$android-inputmethodservice-InputMethodService$InputMethodImpl  reason: not valid java name */
        public /* synthetic */ boolean m1712xe8915f74(InputEvent event) {
            if (!(event instanceof MotionEvent)) {
                return false;
            }
            InputMethodService.this.onStylusHandwritingMotionEvent((MotionEvent) event);
            return true;
        }

        @Override // android.view.inputmethod.InputMethod
        public void initInkWindow() {
            InputMethodService.this.mInkWindow.initOnly();
            InputMethodService.this.onPrepareStylusHandwriting();
            InputMethodService.this.mOnPreparedStylusHwCalled = true;
        }

        @Override // android.view.inputmethod.InputMethod
        public void finishStylusHandwriting() {
            InputMethodService.this.finishStylusHandwriting();
        }

        @Override // android.view.inputmethod.InputMethod
        public void changeInputMethodSubtype(InputMethodSubtype subtype) {
            InputMethodService.this.dispatchOnCurrentInputMethodSubtypeChanged(subtype);
        }
    }

    public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(Bundle uiExtras) {
        return null;
    }

    public boolean onInlineSuggestionsResponse(InlineSuggestionsResponse response) {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IBinder getHostInputToken() {
        ViewRootImpl viewRoot = null;
        View view = this.mRootView;
        if (view != null) {
            viewRoot = view.getViewRootImpl();
        }
        if (viewRoot == null) {
            return null;
        }
        return viewRoot.getInputToken();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleImeSurfaceRemoval() {
        if (this.mShowInputRequested || this.mWindowVisible || this.mWindow == null || this.mImeSurfaceScheduledForRemoval) {
            return;
        }
        if (this.mHandler == null) {
            this.mHandler = new Handler(getMainLooper());
        }
        if (this.mLastWasInFullscreenMode) {
            m1711x3cd4fc45();
            return;
        }
        this.mImeSurfaceScheduledForRemoval = true;
        this.mHandler.postDelayed(new Runnable() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InputMethodService.this.m1711x3cd4fc45();
            }
        }, TIMEOUT_SURFACE_REMOVAL_MILLIS);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: removeImeSurface */
    public void m1711x3cd4fc45() {
        SoftInputWindow softInputWindow = this.mWindow;
        if (softInputWindow != null) {
            softInputWindow.hide();
        }
        this.mImeSurfaceScheduledForRemoval = false;
    }

    private void cancelImeSurfaceRemoval() {
        Handler handler = this.mHandler;
        if (handler != null && this.mImeSurfaceScheduledForRemoval) {
            handler.removeCallbacksAndMessages(null);
            this.mImeSurfaceScheduledForRemoval = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setImeWindowStatus(int visibilityFlags, int backDisposition) {
        this.mPrivOps.setImeWindowStatusAsync(visibilityFlags, backDisposition);
    }

    private void setImeExclusionRect(int visibleTopInsets) {
        View rootView = this.mInputFrame.getRootView();
        if (rootView.getResources() != null) {
            DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
            float minHeight = TypedValue.applyDimension(1, 200.0f, dm);
            if (rootView.getHeight() > minHeight) {
                return;
            }
        }
        android.graphics.Insets systemGesture = rootView.getRootWindowInsets().getInsets(WindowInsets.Type.systemGestures());
        ArrayList<Rect> exclusionRects = new ArrayList<>();
        exclusionRects.add(new Rect(0, visibleTopInsets, systemGesture.left, rootView.getHeight()));
        exclusionRects.add(new Rect(rootView.getWidth() - systemGesture.right, visibleTopInsets, rootView.getWidth(), rootView.getHeight()));
        rootView.setSystemGestureExclusionRects(exclusionRects);
    }

    /* loaded from: classes2.dex */
    public class InputMethodSessionImpl extends AbstractInputMethodService.AbstractInputMethodSessionImpl {
        public InputMethodSessionImpl() {
            super();
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void finishInput() {
            if (!isEnabled()) {
                return;
            }
            if (InputMethodService.DEBUG) {
                Log.v(InputMethodService.TAG, "finishInput() in " + this);
            }
            InputMethodService.this.doFinishInput();
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void displayCompletions(CompletionInfo[] completions) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.mCurCompletions = completions;
            InputMethodService.this.onDisplayCompletions(completions);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void updateExtractedText(int token, ExtractedText text) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateExtractedText(token, text);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void viewClicked(boolean focusChanged) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onViewClicked(focusChanged);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void updateCursor(Rect newCursor) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateCursor(newCursor);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void appPrivateCommand(String action, Bundle data) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onAppPrivateCommand(action, data);
        }

        @Override // android.view.inputmethod.InputMethodSession
        @Deprecated
        public void toggleSoftInput(int showFlags, int hideFlags) {
            InputMethodService.this.onToggleSoftInput(showFlags, hideFlags);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public void updateCursorAnchorInfo(CursorAnchorInfo info) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateCursorAnchorInfo(info);
        }

        @Override // android.view.inputmethod.InputMethodSession
        public final void removeImeSurface() {
            InputMethodService.this.scheduleImeSurfaceRemoval();
        }

        @Override // android.view.inputmethod.InputMethodSession
        public final void invalidateInputInternal(EditorInfo editorInfo, IInputContext inputContext, int sessionId) {
            if (InputMethodService.this.mStartedInputConnection instanceof RemoteInputConnection) {
                RemoteInputConnection ric = (RemoteInputConnection) InputMethodService.this.mStartedInputConnection;
                if (!ric.isSameConnection(inputContext)) {
                    if (InputMethodService.DEBUG) {
                        Log.d(InputMethodService.TAG, "ignoring invalidateInput() due to context mismatch.");
                        return;
                    }
                    return;
                }
                editorInfo.makeCompatible(InputMethodService.this.getApplicationInfo().targetSdkVersion);
                InputMethodService.this.getInputMethodInternal().restartInput(new RemoteInputConnection(ric, sessionId), editorInfo);
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class Insets {
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_REGION = 3;
        public static final int TOUCHABLE_INSETS_VISIBLE = 2;
        public int contentTopInsets;
        public int touchableInsets;
        public final Region touchableRegion = new Region();
        public int visibleTopInsets;

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1120986464257L, this.contentTopInsets);
            proto.write(1120986464258L, this.visibleTopInsets);
            proto.write(1120986464259L, this.touchableInsets);
            proto.write(1138166333444L, this.touchableRegion.toString());
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class SettingsObserver extends ContentObserver {
        private final InputMethodService mService;
        private int mShowImeWithHardKeyboard;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        private @interface ShowImeWithHardKeyboardType {
            public static final int FALSE = 1;
            public static final int TRUE = 2;
            public static final int UNKNOWN = 0;
        }

        private SettingsObserver(InputMethodService service) {
            super(new Handler(service.getMainLooper()));
            this.mShowImeWithHardKeyboard = 0;
            this.mService = service;
        }

        public static SettingsObserver createAndRegister(InputMethodService service) {
            SettingsObserver observer = new SettingsObserver(service);
            service.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD), false, observer);
            return observer;
        }

        void unregister() {
            this.mService.getContentResolver().unregisterContentObserver(this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean shouldShowImeWithHardKeyboard() {
            if (this.mShowImeWithHardKeyboard == 0) {
                this.mShowImeWithHardKeyboard = Settings.Secure.getInt(this.mService.getContentResolver(), Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 0) != 0 ? 2 : 1;
            }
            switch (this.mShowImeWithHardKeyboard) {
                case 1:
                    return false;
                case 2:
                    return true;
                default:
                    Log.e(InputMethodService.TAG, "Unexpected mShowImeWithHardKeyboard=" + this.mShowImeWithHardKeyboard);
                    return false;
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Uri showImeWithHardKeyboardUri = Settings.Secure.getUriFor(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD);
            if (showImeWithHardKeyboardUri.equals(uri)) {
                this.mShowImeWithHardKeyboard = Settings.Secure.getInt(this.mService.getContentResolver(), Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 0) != 0 ? 2 : 1;
                this.mService.resetStateForNewConfiguration();
            }
        }

        public String toString() {
            return "SettingsObserver{mShowImeWithHardKeyboard=" + this.mShowImeWithHardKeyboard + "}";
        }
    }

    @Override // android.content.ContextWrapper, android.content.Context
    public void setTheme(int theme) {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        this.mTheme = theme;
    }

    @Deprecated
    public boolean enableHardwareAcceleration() {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        return ActivityManager.isHighEndGfx();
    }

    @Override // android.app.Service
    public void onCreate() {
        Trace.traceBegin(32L, "IMS.onCreate");
        int selectSystemTheme = Resources.selectSystemTheme(this.mTheme, getApplicationInfo().targetSdkVersion, 16973908, 16973951, 16974142, 16974142);
        this.mTheme = selectSystemTheme;
        super.setTheme(selectSystemTheme);
        super.onCreate();
        this.mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        SettingsObserver createAndRegister = SettingsObserver.createAndRegister(this);
        this.mSettingsObserver = createAndRegister;
        createAndRegister.shouldShowImeWithHardKeyboard();
        this.mHideNavBarForKeyboard = getApplicationContext().getResources().getBoolean(R.bool.config_hideNavBarForKeyboard);
        this.mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Trace.traceBegin(32L, "IMS.initSoftInputWindow");
        SoftInputWindow softInputWindow = new SoftInputWindow(this, this.mTheme, this.mDispatcherState);
        this.mWindow = softInputWindow;
        if (this.mImeDispatcher != null) {
            softInputWindow.getOnBackInvokedDispatcher().setImeOnBackInvokedDispatcher(this.mImeDispatcher);
        }
        this.mNavigationBarController.onSoftInputWindowCreated(this.mWindow);
        Window window = this.mWindow.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.setTitle(InputMethod.TAG);
        lp.type = 2011;
        lp.width = -1;
        lp.height = -2;
        lp.gravity = 80;
        lp.setFitInsetsTypes(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        lp.setFitInsetsSides(WindowInsets.Side.all() & (-9));
        lp.receiveInsetsIgnoringZOrder = true;
        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mImeScenario.setAction(3).setWindow(window));
        window.setAttributes(lp);
        window.setFlags(-2147483384, -2147483382);
        if (this.mHideNavBarForKeyboard) {
            window.setDecorFitsSystemWindows(false);
        }
        initViews();
        Trace.traceEnd(32L);
        this.mInlineSuggestionSessionController = new InlineSuggestionSessionController(new Function() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda6
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return InputMethodService.this.onCreateInlineSuggestionsRequest((Bundle) obj);
            }
        }, new Supplier() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                IBinder hostInputToken;
                hostInputToken = InputMethodService.this.getHostInputToken();
                return hostInputToken;
            }
        }, new Consumer() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                InputMethodService.this.onInlineSuggestionsResponse((InlineSuggestionsResponse) obj);
            }
        });
        Trace.traceEnd(32L);
    }

    public void onInitializeInterface() {
    }

    void initialize() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            onInitializeInterface();
        }
    }

    void initViews() {
        Trace.traceBegin(32L, "IMS.initViews");
        this.mInitialized = false;
        this.mViewsCreated = false;
        this.mShowInputRequested = false;
        this.mShowInputFlags = 0;
        this.mThemeAttrs = obtainStyledAttributes(android.R.styleable.InputMethodService);
        View inflate = this.mInflater.inflate(R.layout.input_method, (ViewGroup) null);
        this.mRootView = inflate;
        this.mWindow.setContentView(inflate);
        this.mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsComputer);
        this.mFullscreenArea = (ViewGroup) this.mRootView.findViewById(R.id.fullscreenArea);
        this.mExtractViewHidden = false;
        this.mExtractFrame = (FrameLayout) this.mRootView.findViewById(16908316);
        this.mExtractView = null;
        this.mExtractEditText = null;
        this.mExtractAccessories = null;
        this.mExtractAction = null;
        this.mFullscreenApplied = false;
        this.mCandidatesFrame = (FrameLayout) this.mRootView.findViewById(16908317);
        this.mInputFrame = (FrameLayout) this.mRootView.findViewById(16908318);
        this.mInputView = null;
        this.mIsInputViewShown = false;
        this.mExtractFrame.setVisibility(8);
        int candidatesHiddenVisibility = getCandidatesHiddenVisibility();
        this.mCandidatesVisibility = candidatesHiddenVisibility;
        this.mCandidatesFrame.setVisibility(candidatesHiddenVisibility);
        this.mInputFrame.setVisibility(8);
        this.mNavigationBarController.onViewInitialized();
        Trace.traceEnd(32L);
    }

    @Override // android.window.WindowProviderService, android.app.Service
    public void onDestroy() {
        this.mDestroyed = true;
        super.onDestroy();
        this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
        doFinishInput();
        this.mNavigationBarController.onDestroy();
        this.mWindow.dismissForDestroyIfNecessary();
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver != null) {
            settingsObserver.unregister();
            this.mSettingsObserver = null;
        }
        IBinder iBinder = this.mToken;
        if (iBinder != null) {
            InputMethodPrivilegedOperationsRegistry.remove(iBinder);
        }
        this.mImeDispatcher = null;
    }

    @Override // android.app.Service, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mConfigTracker.onConfigurationChanged(newConfig, new Runnable() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InputMethodService.this.resetStateForNewConfiguration();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStateForNewConfiguration() {
        Trace.traceBegin(32L, "IMS.resetStateForNewConfiguration");
        boolean visible = this.mDecorViewVisible;
        int showFlags = this.mShowInputFlags;
        boolean showingInput = this.mShowInputRequested;
        CompletionInfo[] completions = this.mCurCompletions;
        this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
        initViews();
        this.mInputViewStarted = false;
        this.mCandidatesViewStarted = false;
        if (this.mInputStarted) {
            doStartInput(getCurrentInputConnection(), getCurrentInputEditorInfo(), true);
        }
        if (visible) {
            if (showingInput) {
                if (dispatchOnShowInputRequested(showFlags, true)) {
                    showWindow(true);
                    if (completions != null) {
                        this.mCurCompletions = completions;
                        onDisplayCompletions(completions);
                    }
                } else {
                    hideWindow();
                }
            } else if (this.mCandidatesVisibility == 0) {
                showWindow(false);
            } else {
                hideWindow();
            }
            boolean showing = onEvaluateInputViewShown();
            setImeWindowStatus((showing ? 2 : 0) | 1, this.mBackDisposition);
        }
        Trace.traceEnd(32L);
    }

    @Override // android.inputmethodservice.AbstractInputMethodService
    public AbstractInputMethodService.AbstractInputMethodImpl onCreateInputMethodInterface() {
        return new InputMethodImpl();
    }

    @Override // android.inputmethodservice.AbstractInputMethodService
    public AbstractInputMethodService.AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface() {
        return new InputMethodSessionImpl();
    }

    public LayoutInflater getLayoutInflater() {
        return this.mInflater;
    }

    public Dialog getWindow() {
        return this.mWindow;
    }

    public void setBackDisposition(int disposition) {
        if (disposition == this.mBackDisposition) {
            return;
        }
        if (disposition > 3 || disposition < 0) {
            Log.e(TAG, "Invalid back disposition value (" + disposition + ") specified.");
            return;
        }
        this.mBackDisposition = disposition;
        setImeWindowStatus(mapToImeWindowStatus(), this.mBackDisposition);
    }

    public int getBackDisposition() {
        return this.mBackDisposition;
    }

    public int getMaxWidth() {
        WindowManager windowManager = (WindowManager) getSystemService(WindowManager.class);
        return WindowMetricsHelper.getBoundsExcludingNavigationBarAndCutout(windowManager.getCurrentWindowMetrics()).width();
    }

    public InputBinding getCurrentInputBinding() {
        return this.mInputBinding;
    }

    public InputConnection getCurrentInputConnection() {
        InputConnection ic = this.mStartedInputConnection;
        if (ic != null) {
            return ic;
        }
        return this.mInputConnection;
    }

    public final boolean switchToPreviousInputMethod() {
        return this.mPrivOps.switchToPreviousInputMethod();
    }

    public final boolean switchToNextInputMethod(boolean onlyCurrentIme) {
        return this.mPrivOps.switchToNextInputMethod(onlyCurrentIme);
    }

    public final boolean shouldOfferSwitchingToNextInputMethod() {
        return this.mPrivOps.shouldOfferSwitchingToNextInputMethod();
    }

    public boolean getCurrentInputStarted() {
        return this.mInputStarted;
    }

    public EditorInfo getCurrentInputEditorInfo() {
        return this.mInputEditorInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFullscreenMode() {
        this.mPrivOps.reportFullscreenModeAsync(this.mIsFullscreen);
    }

    public void updateFullscreenMode() {
        View v;
        Trace.traceBegin(32L, "IMS.updateFullscreenMode");
        boolean isFullscreen = this.mShowInputRequested && onEvaluateFullscreenMode();
        boolean changed = this.mLastShowInputRequested != this.mShowInputRequested;
        if (this.mIsFullscreen != isFullscreen || !this.mFullscreenApplied) {
            changed = true;
            this.mIsFullscreen = isFullscreen;
            reportFullscreenMode();
            this.mFullscreenApplied = true;
            initialize();
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.mFullscreenArea.getLayoutParams();
            if (isFullscreen) {
                this.mFullscreenArea.setBackgroundDrawable(this.mThemeAttrs.getDrawable(0));
                lp.height = 0;
                lp.weight = 1.0f;
            } else {
                this.mFullscreenArea.setBackgroundDrawable(null);
                lp.height = -2;
                lp.weight = 0.0f;
            }
            ((ViewGroup) this.mFullscreenArea.getParent()).updateViewLayout(this.mFullscreenArea, lp);
            if (isFullscreen) {
                if (this.mExtractView == null && (v = onCreateExtractTextView()) != null) {
                    setExtractView(v);
                }
                startExtractingText(false);
            }
            updateExtractFrameVisibility();
        }
        if (changed) {
            onConfigureWindow(this.mWindow.getWindow(), isFullscreen, true ^ this.mShowInputRequested);
            this.mLastShowInputRequested = this.mShowInputRequested;
        }
        Trace.traceEnd(32L);
    }

    public void onConfigureWindow(Window win, boolean isFullscreen, boolean isCandidatesOnly) {
        int currentHeight = this.mWindow.getWindow().getAttributes().height;
        int newHeight = isFullscreen ? -1 : -2;
        if (this.mIsInputViewShown && currentHeight != newHeight && DEBUG) {
            Log.w(TAG, "Window size has been changed. This may cause jankiness of resizing window: " + currentHeight + " -> " + newHeight);
        }
        this.mWindow.getWindow().setLayout(-1, newHeight);
    }

    public boolean isFullscreenMode() {
        return this.mIsFullscreen;
    }

    public boolean onEvaluateFullscreenMode() {
        Configuration config = getResources().getConfiguration();
        if (config.orientation != 2) {
            return false;
        }
        EditorInfo editorInfo = this.mInputEditorInfo;
        return editorInfo == null || ((editorInfo.imeOptions & 33554432) == 0 && (this.mInputEditorInfo.internalImeOptions & 1) == 0);
    }

    public void setExtractViewShown(boolean shown) {
        if (this.mExtractViewHidden == shown) {
            this.mExtractViewHidden = !shown;
            updateExtractFrameVisibility();
        }
    }

    public boolean isExtractViewShown() {
        return this.mIsFullscreen && !this.mExtractViewHidden;
    }

    void updateExtractFrameVisibility() {
        int vis;
        updateCandidatesVisibility(this.mCandidatesVisibility == 0);
        if (isFullscreenMode()) {
            vis = this.mExtractViewHidden ? 4 : 0;
            this.mExtractFrame.setVisibility(vis);
        } else {
            vis = this.mCandidatesVisibility;
            this.mExtractFrame.setVisibility(8);
        }
        if (this.mDecorViewWasVisible && this.mFullscreenArea.getVisibility() != vis) {
            int animRes = this.mThemeAttrs.getResourceId(vis != 0 ? 2 : 1, 0);
            if (animRes != 0) {
                this.mFullscreenArea.startAnimation(AnimationUtils.loadAnimation(this, animRes));
            }
        }
        this.mFullscreenArea.setVisibility(vis);
    }

    public void onComputeInsets(Insets outInsets) {
        Trace.traceBegin(32L, "IMS.onComputeInsets");
        int[] loc = this.mTmpLocation;
        if (this.mInputFrame.getVisibility() == 0) {
            this.mInputFrame.getLocationInWindow(loc);
        } else {
            View decor = getWindow().getWindow().getDecorView();
            loc[1] = decor.getHeight();
        }
        if (isFullscreenMode()) {
            View decor2 = getWindow().getWindow().getDecorView();
            outInsets.contentTopInsets = decor2.getHeight();
        } else {
            outInsets.contentTopInsets = loc[1];
        }
        if (this.mCandidatesFrame.getVisibility() == 0) {
            this.mCandidatesFrame.getLocationInWindow(loc);
        }
        outInsets.visibleTopInsets = loc[1];
        outInsets.touchableInsets = 2;
        outInsets.touchableRegion.setEmpty();
        Trace.traceEnd(32L);
    }

    public void updateInputViewShown() {
        boolean isShown = this.mShowInputRequested && onEvaluateInputViewShown();
        if (this.mIsInputViewShown != isShown && this.mDecorViewVisible) {
            this.mIsInputViewShown = isShown;
            this.mInputFrame.setVisibility(isShown ? 0 : 8);
            if (this.mInputView == null) {
                initialize();
                View v = onCreateInputView();
                if (v != null) {
                    setInputView(v);
                }
            }
        }
    }

    public boolean isShowInputRequested() {
        return this.mShowInputRequested;
    }

    public boolean isInputViewShown() {
        return this.mDecorViewVisible;
    }

    public boolean onEvaluateInputViewShown() {
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver == null) {
            Log.w(TAG, "onEvaluateInputViewShown: mSettingsObserver must not be null here.");
            return false;
        } else if (settingsObserver.shouldShowImeWithHardKeyboard()) {
            return true;
        } else {
            Configuration config = getResources().getConfiguration();
            return config.keyboard == 1 || config.hardKeyboardHidden == 2;
        }
    }

    public void setCandidatesViewShown(boolean shown) {
        updateCandidatesVisibility(shown);
        if (!this.mShowInputRequested && this.mDecorViewVisible != shown) {
            if (shown) {
                showWindow(false);
            } else {
                hideWindow();
            }
        }
    }

    void updateCandidatesVisibility(boolean shown) {
        int vis = shown ? 0 : getCandidatesHiddenVisibility();
        if (this.mCandidatesVisibility != vis) {
            this.mCandidatesFrame.setVisibility(vis);
            this.mCandidatesVisibility = vis;
        }
    }

    public int getCandidatesHiddenVisibility() {
        return isExtractViewShown() ? 8 : 4;
    }

    public void showStatusIcon(int iconResId) {
        this.mStatusIcon = iconResId;
        this.mPrivOps.updateStatusIconAsync(getPackageName(), iconResId);
    }

    public void hideStatusIcon() {
        this.mStatusIcon = 0;
        this.mPrivOps.updateStatusIconAsync(null, 0);
    }

    public void switchInputMethod(String id) {
        this.mPrivOps.setInputMethod(id);
    }

    public final void switchInputMethod(String id, InputMethodSubtype subtype) {
        this.mPrivOps.setInputMethodAndSubtype(id, subtype);
    }

    public void setExtractView(View view) {
        this.mExtractFrame.removeAllViews();
        this.mExtractFrame.addView(view, new FrameLayout.LayoutParams(-1, -1));
        this.mExtractView = view;
        if (view != null) {
            ExtractEditText extractEditText = (ExtractEditText) view.findViewById(16908325);
            this.mExtractEditText = extractEditText;
            extractEditText.setIME(this);
            View findViewById = view.findViewById(16908377);
            this.mExtractAction = findViewById;
            if (findViewById != null) {
                this.mExtractAccessories = (ViewGroup) view.findViewById(16908378);
            }
            startExtractingText(false);
            return;
        }
        this.mExtractEditText = null;
        this.mExtractAccessories = null;
        this.mExtractAction = null;
    }

    public void setCandidatesView(View view) {
        this.mCandidatesFrame.removeAllViews();
        this.mCandidatesFrame.addView(view, new FrameLayout.LayoutParams(-1, -2));
    }

    public void setInputView(View view) {
        this.mInputFrame.removeAllViews();
        this.mInputFrame.addView(view, new FrameLayout.LayoutParams(-1, -2));
        this.mInputView = view;
    }

    public View onCreateExtractTextView() {
        return this.mInflater.inflate(R.layout.input_method_extract_view, (ViewGroup) null);
    }

    public View onCreateCandidatesView() {
        return null;
    }

    public View onCreateInputView() {
        return null;
    }

    public void onStartInputView(EditorInfo info, boolean restarting) {
    }

    public void onFinishInputView(boolean finishingInput) {
        InputConnection ic;
        if (!finishingInput && (ic = getCurrentInputConnection()) != null) {
            ic.finishComposingText();
        }
    }

    public void onStartCandidatesView(EditorInfo info, boolean restarting) {
    }

    public void onFinishCandidatesView(boolean finishingInput) {
        InputConnection ic;
        if (!finishingInput && (ic = getCurrentInputConnection()) != null) {
            ic.finishComposingText();
        }
    }

    public void onPrepareStylusHandwriting() {
    }

    public boolean onStartStylusHandwriting() {
        return false;
    }

    public void onStylusHandwritingMotionEvent(MotionEvent motionEvent) {
        if (this.mInkWindow.isInkViewVisible()) {
            this.mInkWindow.getDecorView().dispatchTouchEvent(motionEvent);
            return;
        }
        if (this.mPendingEvents == null) {
            this.mPendingEvents = new RingBuffer<>(MotionEvent.class, 500);
        }
        this.mPendingEvents.append(motionEvent);
        this.mInkWindow.setInkViewVisibilityListener(new InkWindow.InkVisibilityListener() { // from class: android.inputmethodservice.InputMethodService$$ExternalSyntheticLambda5
            @Override // android.inputmethodservice.InkWindow.InkVisibilityListener
            public final void onInkViewVisible() {
                InputMethodService.this.m1710x7759dc7f();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStylusHandwritingMotionEvent$3$android-inputmethodservice-InputMethodService  reason: not valid java name */
    public /* synthetic */ void m1710x7759dc7f() {
        MotionEvent[] array;
        RingBuffer<MotionEvent> ringBuffer = this.mPendingEvents;
        if (ringBuffer != null && !ringBuffer.isEmpty()) {
            for (MotionEvent event : this.mPendingEvents.toArray()) {
                this.mInkWindow.getDecorView().dispatchTouchEvent(event);
            }
            this.mPendingEvents.clear();
        }
    }

    public void onFinishStylusHandwriting() {
    }

    public final Window getStylusHandwritingWindow() {
        return this.mInkWindow;
    }

    public final void finishStylusHandwriting() {
        if (DEBUG) {
            Log.v(TAG, "finishStylusHandwriting()");
        }
        if (this.mInkWindow == null || !this.mHandwritingRequestId.isPresent()) {
            return;
        }
        int requestId = this.mHandwritingRequestId.getAsInt();
        this.mHandwritingRequestId = OptionalInt.empty();
        this.mHandwritingEventReceiver.dispose();
        this.mHandwritingEventReceiver = null;
        this.mInkWindow.hide(false);
        this.mPrivOps.resetStylusHandwriting(requestId);
        this.mOnPreparedStylusHwCalled = false;
        onFinishStylusHandwriting();
    }

    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (onEvaluateInputViewShown()) {
            if ((flags & 1) == 0) {
                if (!configChange && onEvaluateFullscreenMode()) {
                    return false;
                }
                if (!this.mSettingsObserver.shouldShowImeWithHardKeyboard() && getResources().getConfiguration().keyboard != 1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean dispatchOnShowInputRequested(int flags, boolean configChange) {
        boolean result = onShowInputRequested(flags, configChange);
        this.mInlineSuggestionSessionController.notifyOnShowInputRequested(result);
        if (result) {
            this.mShowInputFlags = flags;
        } else {
            this.mShowInputFlags = 0;
        }
        return result;
    }

    public void showWindow(boolean showInput) {
        int i;
        boolean z = DEBUG;
        if (z) {
            Log.v(TAG, "Showing window: showInput=" + showInput + " mShowInputRequested=" + this.mShowInputRequested + " mViewsCreated=" + this.mViewsCreated + " mDecorViewVisible=" + this.mDecorViewVisible + " mWindowVisible=" + this.mWindowVisible + " mInputStarted=" + this.mInputStarted + " mShowInputFlags=" + this.mShowInputFlags);
        }
        if (this.mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#showWindow", this.mDumper, null);
        Trace.traceBegin(32L, "IMS.showWindow");
        boolean z2 = this.mDecorViewVisible;
        this.mDecorViewWasVisible = z2;
        this.mInShowWindow = true;
        if (isInputViewShown()) {
            i = !this.mWindowVisible ? 4 : 2;
        } else {
            i = 0;
        }
        int previousImeWindowStatus = (z2 ? 1 : 0) | i;
        startViews(prepareWindow(showInput));
        int nextImeWindowStatus = mapToImeWindowStatus();
        if (previousImeWindowStatus != nextImeWindowStatus) {
            setImeWindowStatus(nextImeWindowStatus, this.mBackDisposition);
        }
        this.mNavigationBarController.onWindowShown();
        onWindowShown();
        this.mWindowVisible = true;
        if (z) {
            Log.v(TAG, "showWindow: draw decorView!");
        }
        this.mWindow.show();
        this.mDecorViewWasVisible = true;
        applyVisibilityInInsetsConsumerIfNecessary(true);
        cancelImeSurfaceRemoval();
        this.mInShowWindow = false;
        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mImeScenario.setAction(1));
        Trace.traceEnd(32L);
        registerCompatOnBackInvokedCallback();
    }

    private void registerCompatOnBackInvokedCallback() {
        SoftInputWindow softInputWindow;
        if (!this.mBackCallbackRegistered.booleanValue() && (softInputWindow = this.mWindow) != null) {
            softInputWindow.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this.mCompatBackCallback);
            this.mBackCallbackRegistered = true;
        }
    }

    private void unregisterCompatOnBackInvokedCallback() {
        SoftInputWindow softInputWindow;
        if (this.mBackCallbackRegistered.booleanValue() && (softInputWindow = this.mWindow) != null) {
            softInputWindow.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(this.mCompatBackCallback);
            this.mBackCallbackRegistered = false;
        }
    }

    private KeyEvent createBackKeyEvent(int action, boolean isTracking) {
        long when = SystemClock.uptimeMillis();
        return new KeyEvent(when, when, action, 4, 0, 0, -1, 0, (isTracking ? 512 : 0) | 72, 257);
    }

    private boolean prepareWindow(boolean showInput) {
        boolean doShowInput = false;
        this.mDecorViewVisible = true;
        if (!this.mShowInputRequested && this.mInputStarted && showInput) {
            doShowInput = true;
            this.mShowInputRequested = true;
        }
        boolean z = DEBUG;
        if (z) {
            Log.v(TAG, "showWindow: updating UI");
        }
        initialize();
        updateFullscreenMode();
        updateInputViewShown();
        if (!this.mViewsCreated) {
            this.mViewsCreated = true;
            initialize();
            if (z) {
                Log.v(TAG, "CALL: onCreateCandidatesView");
            }
            View v = onCreateCandidatesView();
            if (z) {
                Log.v(TAG, "showWindow: candidates=" + v);
            }
            if (v != null) {
                setCandidatesView(v);
            }
        }
        return doShowInput;
    }

    private void startViews(boolean doShowInput) {
        if (this.mShowInputRequested) {
            if (!this.mInputViewStarted) {
                if (DEBUG) {
                    Log.v(TAG, "CALL: onStartInputView");
                }
                this.mInputViewStarted = true;
                this.mInlineSuggestionSessionController.notifyOnStartInputView();
                onStartInputView(this.mInputEditorInfo, false);
            }
        } else if (!this.mCandidatesViewStarted) {
            if (DEBUG) {
                Log.v(TAG, "CALL: onStartCandidatesView");
            }
            this.mCandidatesViewStarted = true;
            onStartCandidatesView(this.mInputEditorInfo, false);
        }
        if (doShowInput) {
            startExtractingText(false);
        }
    }

    private void applyVisibilityInInsetsConsumerIfNecessary(boolean setVisible) {
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#applyVisibilityInInsetsConsumerIfNecessary", this.mDumper, null);
        this.mPrivOps.applyImeVisibilityAsync(setVisible ? this.mCurShowInputToken : this.mCurHideInputToken, setVisible);
    }

    private void finishViews(boolean finishingInput) {
        if (this.mInputViewStarted) {
            if (DEBUG) {
                Log.v(TAG, "CALL: onFinishInputView");
            }
            this.mInlineSuggestionSessionController.notifyOnFinishInputView();
            onFinishInputView(finishingInput);
        } else if (this.mCandidatesViewStarted) {
            if (DEBUG) {
                Log.v(TAG, "CALL: onFinishCandidatesView");
            }
            onFinishCandidatesView(finishingInput);
        }
        this.mInputViewStarted = false;
        this.mCandidatesViewStarted = false;
    }

    public void hideWindow() {
        if (DEBUG) {
            Log.v(TAG, "CALL: hideWindow");
        }
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#hideWindow", this.mDumper, null);
        setImeWindowStatus(0, this.mBackDisposition);
        applyVisibilityInInsetsConsumerIfNecessary(false);
        this.mWindowVisible = false;
        finishViews(false);
        if (this.mDecorViewVisible) {
            View view = this.mInputView;
            if (view != null) {
                view.dispatchWindowVisibilityChanged(8);
            }
            this.mDecorViewVisible = false;
            onWindowHidden();
            this.mDecorViewWasVisible = false;
            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mImeScenario.setAction(2));
        }
        this.mLastWasInFullscreenMode = this.mIsFullscreen;
        updateFullscreenMode();
        unregisterCompatOnBackInvokedCallback();
    }

    public void onWindowShown() {
    }

    public void onWindowHidden() {
    }

    public void onBindInput() {
    }

    public void onUnbindInput() {
    }

    public void onStartInput(EditorInfo attribute, boolean restarting) {
    }

    void doFinishInput() {
        boolean z = DEBUG;
        if (z) {
            Log.v(TAG, "CALL: doFinishInput");
        }
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#doFinishInput", this.mDumper, null);
        finishViews(true);
        if (this.mInputStarted) {
            this.mInlineSuggestionSessionController.notifyOnFinishInput();
            if (z) {
                Log.v(TAG, "CALL: onFinishInput");
            }
            onFinishInput();
        }
        this.mInputStarted = false;
        this.mStartedInputConnection = null;
        this.mCurCompletions = null;
        if (this.mInkWindow != null) {
            finishStylusHandwriting();
        }
        unregisterCompatOnBackInvokedCallback();
    }

    void doStartInput(InputConnection ic, EditorInfo attribute, boolean restarting) {
        if (!restarting && this.mInputStarted) {
            doFinishInput();
        }
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#doStartInput", this.mDumper, null);
        this.mInputStarted = true;
        this.mStartedInputConnection = ic;
        this.mInputEditorInfo = attribute;
        initialize();
        this.mInlineSuggestionSessionController.notifyOnStartInput(attribute == null ? null : attribute.packageName, attribute != null ? attribute.autofillId : null);
        boolean z = DEBUG;
        if (z) {
            Log.v(TAG, "CALL: onStartInput");
        }
        onStartInput(attribute, restarting);
        if (this.mDecorViewVisible) {
            if (this.mShowInputRequested) {
                if (z) {
                    Log.v(TAG, "CALL: onStartInputView");
                }
                this.mInputViewStarted = true;
                this.mInlineSuggestionSessionController.notifyOnStartInputView();
                onStartInputView(this.mInputEditorInfo, restarting);
                startExtractingText(true);
            } else if (this.mCandidatesVisibility == 0) {
                if (z) {
                    Log.v(TAG, "CALL: onStartCandidatesView");
                }
                this.mCandidatesViewStarted = true;
                onStartCandidatesView(this.mInputEditorInfo, restarting);
            }
        }
    }

    public void onFinishInput() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.finishComposingText();
        }
    }

    public void onDisplayCompletions(CompletionInfo[] completions) {
    }

    public void onUpdateExtractedText(int token, ExtractedText text) {
        ExtractEditText extractEditText;
        if (this.mExtractedToken == token && text != null && (extractEditText = this.mExtractEditText) != null) {
            this.mExtractedText = text;
            extractEditText.setExtractedText(text);
        }
    }

    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        ExtractedText extractedText;
        ExtractEditText eet = this.mExtractEditText;
        if (eet != null && isFullscreenMode() && (extractedText = this.mExtractedText) != null) {
            int off = extractedText.startOffset;
            eet.startInternalChanges();
            int newSelStart2 = newSelStart - off;
            int newSelEnd2 = newSelEnd - off;
            int len = eet.getText().length();
            if (newSelStart2 < 0) {
                newSelStart2 = 0;
            } else if (newSelStart2 > len) {
                newSelStart2 = len;
            }
            if (newSelEnd2 < 0) {
                newSelEnd2 = 0;
            } else if (newSelEnd2 > len) {
                newSelEnd2 = len;
            }
            eet.setSelection(newSelStart2, newSelEnd2);
            eet.finishInternalChanges();
        }
    }

    @Deprecated
    public void onViewClicked(boolean focusChanged) {
    }

    @Deprecated
    public void onUpdateCursor(Rect newCursor) {
    }

    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
    }

    public void requestHideSelf(int flags) {
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#requestHideSelf", this.mDumper, null);
        this.mPrivOps.hideMySoftInput(flags);
    }

    public final void requestShowSelf(int flags) {
        ImeTracing.getInstance().triggerServiceDump("InputMethodService#requestShowSelf", this.mDumper, null);
        this.mPrivOps.showMySoftInput(flags);
    }

    private boolean handleBack(boolean doIt) {
        if (this.mShowInputRequested) {
            if (doIt) {
                requestHideSelf(0);
            }
            return true;
        } else if (this.mDecorViewVisible) {
            if (this.mCandidatesVisibility == 0) {
                if (doIt) {
                    setCandidatesViewShown(false);
                }
            } else if (doIt) {
                hideWindow();
            }
            return true;
        } else {
            return false;
        }
    }

    private ExtractEditText getExtractEditTextIfVisible() {
        if (!isExtractViewShown() || !isInputViewShown()) {
            return null;
        }
        return this.mExtractEditText;
    }

    @Override // android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == 4) {
            ExtractEditText eet = getExtractEditTextIfVisible();
            if (eet != null && eet.handleBackInTextActionModeIfNeeded(event)) {
                return true;
            }
            if (!handleBack(false)) {
                return false;
            }
            event.startTracking();
            return true;
        }
        return doMovementKey(keyCode, event, -1);
    }

    @Override // android.view.KeyEvent.Callback
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // android.view.KeyEvent.Callback
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return doMovementKey(keyCode, event, count);
    }

    @Override // android.view.KeyEvent.Callback
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == 4) {
            ExtractEditText eet = getExtractEditTextIfVisible();
            if (eet != null && eet.handleBackInTextActionModeIfNeeded(event)) {
                return true;
            }
            if (event.isTracking() && !event.isCanceled()) {
                return handleBack(true);
            }
        }
        return doMovementKey(keyCode, event, -2);
    }

    @Override // android.inputmethodservice.AbstractInputMethodService
    public boolean onTrackballEvent(MotionEvent event) {
        if (DEBUG) {
            Log.v(TAG, "onTrackballEvent: " + event);
            return false;
        }
        return false;
    }

    @Override // android.inputmethodservice.AbstractInputMethodService
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (DEBUG) {
            Log.v(TAG, "onGenericMotionEvent(): event " + event);
            return false;
        }
        return false;
    }

    public void onAppPrivateCommand(String action, Bundle data) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onToggleSoftInput(int showFlags, int hideFlags) {
        if (DEBUG) {
            Log.v(TAG, "toggleSoftInput()");
        }
        if (isInputViewShown()) {
            requestHideSelf(hideFlags);
        } else {
            requestShowSelf(showFlags);
        }
    }

    void reportExtractedMovement(int keyCode, int count) {
        int dx = 0;
        int dy = 0;
        switch (keyCode) {
            case 19:
                dy = -count;
                break;
            case 20:
                dy = count;
                break;
            case 21:
                dx = -count;
                break;
            case 22:
                dx = count;
                break;
        }
        onExtractedCursorMovement(dx, dy);
    }

    boolean doMovementKey(int keyCode, KeyEvent event, int count) {
        ExtractEditText eet = getExtractEditTextIfVisible();
        if (eet != null) {
            MovementMethod movement = eet.getMovementMethod();
            Layout layout = eet.getLayout();
            if (movement != null && layout != null) {
                if (count == -1) {
                    if (movement.onKeyDown(eet, eet.getText(), keyCode, event)) {
                        reportExtractedMovement(keyCode, 1);
                        return true;
                    }
                } else if (count == -2) {
                    if (movement.onKeyUp(eet, eet.getText(), keyCode, event)) {
                        return true;
                    }
                } else if (movement.onKeyOther(eet, eet.getText(), event)) {
                    reportExtractedMovement(keyCode, count);
                } else {
                    KeyEvent down = KeyEvent.changeAction(event, 0);
                    if (movement.onKeyDown(eet, eet.getText(), keyCode, down)) {
                        KeyEvent up = KeyEvent.changeAction(event, 1);
                        movement.onKeyUp(eet, eet.getText(), keyCode, up);
                        while (true) {
                            count--;
                            if (count <= 0) {
                                break;
                            }
                            movement.onKeyDown(eet, eet.getText(), keyCode, down);
                            movement.onKeyUp(eet, eet.getText(), keyCode, up);
                        }
                        reportExtractedMovement(keyCode, count);
                    }
                }
            }
            switch (keyCode) {
                case 19:
                case 20:
                case 21:
                case 22:
                    return true;
            }
        }
        return false;
    }

    public void sendDownUpKeyEvents(int keyEventCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        long eventTime = SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, 0, keyEventCode, 0, 0, -1, 0, 6));
        ic.sendKeyEvent(new KeyEvent(eventTime, SystemClock.uptimeMillis(), 1, keyEventCode, 0, 0, -1, 0, 6));
    }

    public boolean sendDefaultEditorAction(boolean fromEnterKey) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) {
            if ((!fromEnterKey || (ei.imeOptions & 1073741824) == 0) && (ei.imeOptions & 255) != 1) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.performEditorAction(ei.imeOptions & 255);
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void sendKeyChar(char charCode) {
        switch (charCode) {
            case '\n':
                if (!sendDefaultEditorAction(true)) {
                    sendDownUpKeyEvents(66);
                    return;
                }
                return;
            default:
                if (charCode >= '0' && charCode <= '9') {
                    sendDownUpKeyEvents((charCode - '0') + 7);
                    return;
                }
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(String.valueOf(charCode), 1);
                    return;
                }
                return;
        }
    }

    public void onExtractedSelectionChanged(int start, int end) {
        InputConnection conn = getCurrentInputConnection();
        if (conn != null) {
            conn.setSelection(start, end);
        }
    }

    public void onExtractedDeleteText(int start, int end) {
        InputConnection conn = getCurrentInputConnection();
        if (conn != null) {
            conn.finishComposingText();
            conn.setSelection(start, start);
            conn.deleteSurroundingText(0, end - start);
        }
    }

    public void onExtractedReplaceText(int start, int end, CharSequence text) {
        InputConnection conn = getCurrentInputConnection();
        if (conn != null) {
            conn.setComposingRegion(start, end);
            conn.commitText(text, 1);
        }
    }

    public void onExtractedSetSpan(Object span, int start, int end, int flags) {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null || !conn.setSelection(start, end)) {
            return;
        }
        CharSequence text = conn.getSelectedText(1);
        if (text instanceof Spannable) {
            ((Spannable) text).setSpan(span, 0, text.length(), flags);
            conn.setComposingRegion(start, end);
            conn.commitText(text, 1);
        }
    }

    public void onExtractedTextClicked() {
        ExtractEditText extractEditText = this.mExtractEditText;
        if (extractEditText != null && extractEditText.hasVerticalScrollBar()) {
            setCandidatesViewShown(false);
        }
    }

    public void onExtractedCursorMovement(int dx, int dy) {
        ExtractEditText extractEditText = this.mExtractEditText;
        if (extractEditText != null && dy != 0 && extractEditText.hasVerticalScrollBar()) {
            setCandidatesViewShown(false);
        }
    }

    public boolean onExtractTextContextMenuItem(int id) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.performContextMenuAction(id);
            return true;
        }
        return true;
    }

    public CharSequence getTextForImeAction(int imeOptions) {
        switch (imeOptions & 255) {
            case 1:
                return null;
            case 2:
                return getText(R.string.ime_action_go);
            case 3:
                return getText(R.string.ime_action_search);
            case 4:
                return getText(R.string.ime_action_send);
            case 5:
                return getText(R.string.ime_action_next);
            case 6:
                return getText(R.string.ime_action_done);
            case 7:
                return getText(R.string.ime_action_previous);
            default:
                return getText(R.string.ime_action_default);
        }
    }

    private int getIconForImeAction(int imeOptions) {
        switch (imeOptions & 255) {
            case 2:
                return R.drawable.ic_input_extract_action_go;
            case 3:
                return R.drawable.ic_input_extract_action_search;
            case 4:
                return R.drawable.ic_input_extract_action_send;
            case 5:
                return R.drawable.ic_input_extract_action_next;
            case 6:
                return R.drawable.ic_input_extract_action_done;
            case 7:
                return R.drawable.ic_input_extract_action_previous;
            default:
                return R.drawable.ic_input_extract_action_return;
        }
    }

    public void onUpdateExtractingVisibility(EditorInfo ei) {
        if (ei.inputType == 0 || (ei.imeOptions & 268435456) != 0) {
            setExtractViewShown(false);
        } else {
            setExtractViewShown(true);
        }
    }

    public void onUpdateExtractingViews(EditorInfo ei) {
        if (!isExtractViewShown() || this.mExtractAccessories == null) {
            return;
        }
        boolean z = true;
        if (ei.actionLabel == null && ((ei.imeOptions & 255) == 1 || (ei.imeOptions & 536870912) != 0 || ei.inputType == 0)) {
            z = false;
        }
        boolean hasAction = z;
        if (hasAction) {
            this.mExtractAccessories.setVisibility(0);
            View view = this.mExtractAction;
            if (view != null) {
                if (view instanceof ImageButton) {
                    ((ImageButton) view).setImageResource(getIconForImeAction(ei.imeOptions));
                    if (ei.actionLabel != null) {
                        this.mExtractAction.setContentDescription(ei.actionLabel);
                    } else {
                        this.mExtractAction.setContentDescription(getTextForImeAction(ei.imeOptions));
                    }
                } else if (ei.actionLabel != null) {
                    ((TextView) this.mExtractAction).setText(ei.actionLabel);
                } else {
                    ((TextView) this.mExtractAction).setText(getTextForImeAction(ei.imeOptions));
                }
                this.mExtractAction.setOnClickListener(this.mActionClickListener);
                return;
            }
            return;
        }
        this.mExtractAccessories.setVisibility(8);
        View view2 = this.mExtractAction;
        if (view2 != null) {
            view2.setOnClickListener(null);
        }
    }

    public void onExtractingInputChanged(EditorInfo ei) {
        if (ei.inputType == 0) {
            requestHideSelf(2);
        }
    }

    void startExtractingText(boolean inputChanged) {
        ExtractEditText eet = this.mExtractEditText;
        if (eet != null && getCurrentInputStarted() && isFullscreenMode()) {
            this.mExtractedToken++;
            ExtractedTextRequest req = new ExtractedTextRequest();
            req.token = this.mExtractedToken;
            req.flags = 1;
            req.hintMaxLines = 10;
            req.hintMaxChars = 10000;
            InputConnection ic = getCurrentInputConnection();
            ExtractedText extractedText = ic == null ? null : ic.getExtractedText(req, 1);
            this.mExtractedText = extractedText;
            if (extractedText == null || ic == null) {
                Log.e(TAG, "Unexpected null in startExtractingText : mExtractedText = " + this.mExtractedText + ", input connection = " + ic);
            }
            EditorInfo ei = getCurrentInputEditorInfo();
            try {
                eet.startInternalChanges();
                onUpdateExtractingVisibility(ei);
                onUpdateExtractingViews(ei);
                int inputType = ei.inputType;
                if ((inputType & 15) == 1 && (262144 & inputType) != 0) {
                    inputType |= 131072;
                }
                eet.setInputType(inputType);
                eet.setHint(ei.hintText);
                if (this.mExtractedText != null) {
                    eet.setEnabled(true);
                    eet.setExtractedText(this.mExtractedText);
                } else {
                    eet.setEnabled(false);
                    eet.setText("");
                }
                if (inputChanged) {
                    onExtractingInputChanged(ei);
                }
            } finally {
                eet.finishInternalChanges();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchOnCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        synchronized (this.mLock) {
            this.mNotifyUserActionSent = false;
        }
        onCurrentInputMethodSubtypeChanged(newSubtype);
    }

    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        if (DEBUG) {
            int nameResId = newSubtype.getNameResId();
            String mode = newSubtype.getMode();
            String output = "changeInputMethodSubtype:" + (nameResId == 0 ? "<none>" : getString(nameResId)) + "," + mode + "," + newSubtype.getLocale() + "," + newSubtype.getExtraValue();
            Log.v(TAG, "--- " + output);
        }
    }

    @Deprecated
    public int getInputMethodWindowRecommendedHeight() {
        Log.w(TAG, "getInputMethodWindowRecommendedHeight() is deprecated and now always returns 0. Do not use this method.");
        return 0;
    }

    @Override // android.inputmethodservice.AbstractInputMethodService
    final InputMethodServiceInternal createInputMethodServiceInternal() {
        return new InputMethodServiceInternal() { // from class: android.inputmethodservice.InputMethodService.1
            @Override // android.inputmethodservice.InputMethodServiceInternal
            public Context getContext() {
                return InputMethodService.this;
            }

            @Override // android.inputmethodservice.InputMethodServiceInternal
            public void exposeContent(InputContentInfo inputContentInfo, InputConnection inputConnection) {
                if (inputConnection == null || InputMethodService.this.getCurrentInputConnection() != inputConnection) {
                    return;
                }
                exposeContentInternal(inputContentInfo, InputMethodService.this.getCurrentInputEditorInfo());
            }

            @Override // android.inputmethodservice.InputMethodServiceInternal
            public void notifyUserActionIfNecessary() {
                synchronized (InputMethodService.this.mLock) {
                    if (InputMethodService.this.mNotifyUserActionSent) {
                        return;
                    }
                    InputMethodService.this.mPrivOps.notifyUserActionAsync();
                    InputMethodService.this.mNotifyUserActionSent = true;
                }
            }

            private void exposeContentInternal(InputContentInfo inputContentInfo, EditorInfo editorInfo) {
                Uri contentUri = inputContentInfo.getContentUri();
                IInputContentUriToken uriToken = InputMethodService.this.mPrivOps.createInputContentUriToken(contentUri, editorInfo.packageName);
                if (uriToken == null) {
                    Log.e(InputMethodService.TAG, "createInputContentAccessToken failed. contentUri=" + contentUri.toString() + " packageName=" + editorInfo.packageName);
                } else {
                    inputContentInfo.setUriToken(uriToken);
                }
            }

            @Override // android.inputmethodservice.InputMethodServiceInternal
            public void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
                InputMethodService.this.dump(fd, fout, args);
            }

            @Override // android.inputmethodservice.InputMethodServiceInternal
            public void triggerServiceDump(String where, byte[] icProto) {
                ImeTracing.getInstance().triggerServiceDump(where, InputMethodService.this.mDumper, icProto);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int mapToImeWindowStatus() {
        return (isInputViewShown() ? 2 : 0) | 1;
    }

    private boolean isAutomotive() {
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.inputmethodservice.AbstractInputMethodService, android.app.Service
    public void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        Printer p = new PrintWriterPrinter(fout);
        p.println("Input method service state for " + this + ":");
        p.println("  mViewsCreated=" + this.mViewsCreated);
        p.println("  mDecorViewVisible=" + this.mDecorViewVisible + " mDecorViewWasVisible=" + this.mDecorViewWasVisible + " mWindowVisible=" + this.mWindowVisible + " mInShowWindow=" + this.mInShowWindow);
        p.println("  Configuration=" + getResources().getConfiguration());
        p.println("  mToken=" + this.mToken);
        p.println("  mInputBinding=" + this.mInputBinding);
        p.println("  mInputConnection=" + this.mInputConnection);
        p.println("  mStartedInputConnection=" + this.mStartedInputConnection);
        p.println("  mInputStarted=" + this.mInputStarted + " mInputViewStarted=" + this.mInputViewStarted + " mCandidatesViewStarted=" + this.mCandidatesViewStarted);
        if (this.mInputEditorInfo != null) {
            p.println("  mInputEditorInfo:");
            this.mInputEditorInfo.dump(p, "    ");
        } else {
            p.println("  mInputEditorInfo: null");
        }
        p.println("  mShowInputRequested=" + this.mShowInputRequested + " mLastShowInputRequested=" + this.mLastShowInputRequested + " mShowInputFlags=0x" + Integer.toHexString(this.mShowInputFlags));
        p.println("  mCandidatesVisibility=" + this.mCandidatesVisibility + " mFullscreenApplied=" + this.mFullscreenApplied + " mIsFullscreen=" + this.mIsFullscreen + " mExtractViewHidden=" + this.mExtractViewHidden);
        if (this.mExtractedText != null) {
            p.println("  mExtractedText:");
            p.println("    text=" + this.mExtractedText.text.length() + " chars startOffset=" + this.mExtractedText.startOffset);
            p.println("    selectionStart=" + this.mExtractedText.selectionStart + " selectionEnd=" + this.mExtractedText.selectionEnd + " flags=0x" + Integer.toHexString(this.mExtractedText.flags));
        } else {
            p.println("  mExtractedText: null");
        }
        p.println("  mExtractedToken=" + this.mExtractedToken);
        p.println("  mIsInputViewShown=" + this.mIsInputViewShown + " mStatusIcon=" + this.mStatusIcon);
        p.println("Last computed insets:");
        p.println("  contentTopInsets=" + this.mTmpInsets.contentTopInsets + " visibleTopInsets=" + this.mTmpInsets.visibleTopInsets + " touchableInsets=" + this.mTmpInsets.touchableInsets + " touchableRegion=" + this.mTmpInsets.touchableRegion);
        p.println(" mSettingsObserver=" + this.mSettingsObserver);
        p.println(" mNavigationBarController=" + this.mNavigationBarController.toDebugString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void compatHandleBack() {
        if (!this.mDecorViewVisible) {
            Log.e(TAG, "Back callback invoked on a hidden IME. Removing the callback...");
            unregisterCompatOnBackInvokedCallback();
            return;
        }
        KeyEvent downEvent = createBackKeyEvent(0, false);
        onKeyDown(4, downEvent);
        boolean hasStartedTracking = (downEvent.getFlags() & 1073741824) != 0;
        KeyEvent upEvent = createBackKeyEvent(1, hasStartedTracking);
        onKeyUp(4, upEvent);
    }
}
