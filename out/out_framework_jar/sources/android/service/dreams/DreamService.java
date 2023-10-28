package android.service.dreams;

import android.app.Activity;
import android.app.ActivityTaskManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.audio.Enums;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.service.dreams.IDreamOverlay;
import android.service.dreams.IDreamOverlayCallback;
import android.service.dreams.IDreamService;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Xml;
import android.view.ActionMode;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.R;
import com.android.internal.util.DumpUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes3.dex */
public class DreamService extends Service implements Window.Callback {
    private static final boolean DEBUG;
    public static final boolean DEFAULT_SHOW_COMPLICATIONS = false;
    public static final String DREAM_META_DATA = "android.service.dream";
    private static final String DREAM_META_DATA_ROOT_TAG = "dream";
    public static final String DREAM_SERVICE = "dreams";
    public static final String EXTRA_DREAM_OVERLAY_COMPONENT = "android.service.dream.DreamService.dream_overlay_component";
    public static final String EXTRA_SHOW_COMPLICATIONS = "android.service.dreams.SHOW_COMPLICATIONS";
    public static final String SERVICE_INTERFACE = "android.service.dreams.DreamService";
    private static final String TAG;
    private Activity mActivity;
    private boolean mCanDoze;
    private Runnable mDispatchAfterOnAttachedToWindow;
    private boolean mDozing;
    private DreamServiceWrapper mDreamServiceWrapper;
    private IBinder mDreamToken;
    private boolean mFinished;
    private boolean mFullscreen;
    private boolean mInteractive;
    private boolean mStarted;
    private boolean mWaking;
    private Window mWindow;
    private boolean mWindowless;
    private final String mTag = TAG + NavigationBarInflaterView.SIZE_MOD_START + getClass().getSimpleName() + NavigationBarInflaterView.SIZE_MOD_END;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mScreenBright = true;
    private int mDozeScreenState = 0;
    private int mDozeScreenBrightness = -1;
    private boolean mDebug = false;
    private final IDreamOverlayCallback mOverlayCallback = new IDreamOverlayCallback.Stub() { // from class: android.service.dreams.DreamService.1
        @Override // android.service.dreams.IDreamOverlayCallback
        public void onExitRequested() {
            DreamService.this.finish();
        }
    };
    private final IDreamManager mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService(DREAM_SERVICE));
    private final OverlayConnection mOverlayConnection = new OverlayConnection();

    static {
        String simpleName = DreamService.class.getSimpleName();
        TAG = simpleName;
        DEBUG = Log.isLoggable(simpleName, 3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class OverlayConnection implements ServiceConnection {
        private boolean mBound;
        private IDreamOverlay mOverlay;
        private final ArrayDeque<Consumer<IDreamOverlay>> mRequests = new ArrayDeque<>();

        OverlayConnection() {
        }

        public void bind(Context context, ComponentName overlayService, ComponentName dreamService) {
            if (overlayService == null) {
                return;
            }
            ServiceInfo serviceInfo = DreamService.fetchServiceInfo(context, dreamService);
            Intent overlayIntent = new Intent();
            overlayIntent.setComponent(overlayService);
            overlayIntent.putExtra(DreamService.EXTRA_SHOW_COMPLICATIONS, DreamService.fetchShouldShowComplications(context, serviceInfo));
            context.bindService(overlayIntent, this, Enums.AUDIO_FORMAT_AAC_MAIN);
            this.mBound = true;
        }

        public void unbind(Context context) {
            if (!this.mBound) {
                return;
            }
            context.unbindService(this);
            this.mBound = false;
        }

        public void request(Consumer<IDreamOverlay> request) {
            this.mRequests.push(request);
            evaluate();
        }

        private void evaluate() {
            if (this.mOverlay == null) {
                return;
            }
            while (!this.mRequests.isEmpty()) {
                Consumer<IDreamOverlay> request = this.mRequests.pop();
                request.accept(this.mOverlay);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mOverlay = IDreamOverlay.Stub.asInterface(service);
            evaluate();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            this.mOverlay = null;
        }
    }

    public void setDebug(boolean dbg) {
        this.mDebug = dbg;
    }

    @Override // android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!this.mInteractive) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on keyEvent");
            }
            wakeUp();
            return true;
        } else if (event.getKeyCode() == 4) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on back key");
            }
            wakeUp();
            return true;
        } else {
            return this.mWindow.superDispatchKeyEvent(event);
        }
    }

    @Override // android.view.Window.Callback
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        if (!this.mInteractive) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on keyShortcutEvent");
            }
            wakeUp();
            return true;
        }
        return this.mWindow.superDispatchKeyShortcutEvent(event);
    }

    @Override // android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!this.mInteractive && event.getActionMasked() == 1) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on touchEvent");
            }
            wakeUp();
            return true;
        }
        return this.mWindow.superDispatchTouchEvent(event);
    }

    @Override // android.view.Window.Callback
    public boolean dispatchTrackballEvent(MotionEvent event) {
        if (!this.mInteractive) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on trackballEvent");
            }
            wakeUp();
            return true;
        }
        return this.mWindow.superDispatchTrackballEvent(event);
    }

    @Override // android.view.Window.Callback
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (!this.mInteractive) {
            if (this.mDebug) {
                Slog.v(this.mTag, "Waking up on genericMotionEvent");
            }
            wakeUp();
            return true;
        }
        return this.mWindow.superDispatchGenericMotionEvent(event);
    }

    @Override // android.view.Window.Callback
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    @Override // android.view.Window.Callback
    public View onCreatePanelView(int featureId) {
        return null;
    }

    @Override // android.view.Window.Callback
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return false;
    }

    @Override // android.view.Window.Callback
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return false;
    }

    @Override // android.view.Window.Callback
    public boolean onMenuOpened(int featureId, Menu menu) {
        return false;
    }

    @Override // android.view.Window.Callback
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    @Override // android.view.Window.Callback
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
    }

    @Override // android.view.Window.Callback
    public void onContentChanged() {
    }

    @Override // android.view.Window.Callback
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override // android.view.Window.Callback
    public void onAttachedToWindow() {
    }

    @Override // android.view.Window.Callback
    public void onDetachedFromWindow() {
    }

    @Override // android.view.Window.Callback
    public void onPanelClosed(int featureId, Menu menu) {
    }

    @Override // android.view.Window.Callback
    public boolean onSearchRequested(SearchEvent event) {
        return onSearchRequested();
    }

    @Override // android.view.Window.Callback
    public boolean onSearchRequested() {
        return false;
    }

    @Override // android.view.Window.Callback
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return null;
    }

    @Override // android.view.Window.Callback
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
        return null;
    }

    @Override // android.view.Window.Callback
    public void onActionModeStarted(ActionMode mode) {
    }

    @Override // android.view.Window.Callback
    public void onActionModeFinished(ActionMode mode) {
    }

    public WindowManager getWindowManager() {
        Window window = this.mWindow;
        if (window != null) {
            return window.getWindowManager();
        }
        return null;
    }

    public Window getWindow() {
        return this.mWindow;
    }

    public void setContentView(int layoutResID) {
        getWindow().setContentView(layoutResID);
    }

    public void setContentView(View view) {
        getWindow().setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getWindow().setContentView(view, params);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getWindow().addContentView(view, params);
    }

    public <T extends View> T findViewById(int id) {
        return (T) getWindow().findViewById(id);
    }

    public final <T extends View> T requireViewById(int id) {
        T view = (T) findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this DreamService");
        }
        return view;
    }

    public void setInteractive(boolean interactive) {
        this.mInteractive = interactive;
    }

    public boolean isInteractive() {
        return this.mInteractive;
    }

    public void setFullscreen(boolean fullscreen) {
        if (this.mFullscreen != fullscreen) {
            this.mFullscreen = fullscreen;
            applyWindowFlags(fullscreen ? 1024 : 0, 1024);
        }
    }

    public boolean isFullscreen() {
        return this.mFullscreen;
    }

    public void setScreenBright(boolean screenBright) {
        if (this.mScreenBright != screenBright) {
            this.mScreenBright = screenBright;
            applyWindowFlags(screenBright ? 128 : 0, 128);
        }
    }

    public boolean isScreenBright() {
        return getWindowFlagValue(128, this.mScreenBright);
    }

    public void setWindowless(boolean windowless) {
        this.mWindowless = windowless;
    }

    public boolean isWindowless() {
        return this.mWindowless;
    }

    public boolean canDoze() {
        return this.mCanDoze;
    }

    public void startDozing() {
        if (this.mCanDoze && !this.mDozing) {
            this.mDozing = true;
            updateDoze();
        }
    }

    private void updateDoze() {
        IBinder iBinder = this.mDreamToken;
        if (iBinder == null) {
            Slog.w(this.mTag, "Updating doze without a dream token.");
        } else if (this.mDozing) {
            try {
                this.mDreamManager.startDozing(iBinder, this.mDozeScreenState, this.mDozeScreenBrightness);
            } catch (RemoteException e) {
            }
        }
    }

    public void stopDozing() {
        if (this.mDozing) {
            this.mDozing = false;
            try {
                this.mDreamManager.stopDozing(this.mDreamToken);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public int getDozeScreenState() {
        return this.mDozeScreenState;
    }

    public void setDozeScreenState(int state) {
        if (this.mDozeScreenState != state) {
            this.mDozeScreenState = state;
            updateDoze();
        }
    }

    public int getDozeScreenBrightness() {
        return this.mDozeScreenBrightness;
    }

    public void setDozeScreenBrightness(int brightness) {
        if (brightness != -1) {
            brightness = clampAbsoluteBrightness(brightness);
        }
        if (this.mDozeScreenBrightness != brightness) {
            this.mDozeScreenBrightness = brightness;
            updateDoze();
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        if (this.mDebug) {
            Slog.v(this.mTag, "onCreate()");
        }
        super.onCreate();
    }

    public void onDreamingStarted() {
        if (this.mDebug) {
            Slog.v(this.mTag, "onDreamingStarted()");
        }
    }

    public void onDreamingStopped() {
        if (this.mDebug) {
            Slog.v(this.mTag, "onDreamingStopped()");
        }
    }

    public void onWakeUp() {
        finish();
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        if (this.mDebug) {
            Slog.v(this.mTag, "onBind() intent = " + intent);
        }
        this.mDreamServiceWrapper = new DreamServiceWrapper();
        if (!this.mWindowless) {
            this.mOverlayConnection.bind(this, (ComponentName) intent.getParcelableExtra(EXTRA_DREAM_OVERLAY_COMPONENT), new ComponentName(this, getClass()));
        }
        return this.mDreamServiceWrapper;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        this.mOverlayConnection.unbind(this);
        return super.onUnbind(intent);
    }

    public final void finish() {
        if (this.mDebug) {
            Slog.v(this.mTag, "finish(): mFinished=" + this.mFinished);
        }
        Activity activity = this.mActivity;
        if (activity != null) {
            if (!activity.isFinishing()) {
                activity.finishAndRemoveTask();
            }
        } else if (this.mFinished) {
        } else {
            this.mFinished = true;
            this.mOverlayConnection.unbind(this);
            IBinder iBinder = this.mDreamToken;
            if (iBinder != null) {
                try {
                    this.mDreamManager.finishSelf(iBinder, true);
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            Slog.w(this.mTag, "Finish was called before the dream was attached.");
            stopSelf();
        }
    }

    public final void wakeUp() {
        wakeUp(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wakeUp(boolean fromSystem) {
        if (this.mDebug) {
            Slog.v(this.mTag, "wakeUp(): fromSystem=" + fromSystem + ", mWaking=" + this.mWaking + ", mFinished=" + this.mFinished);
        }
        if (!this.mWaking && !this.mFinished) {
            this.mWaking = true;
            Activity activity = this.mActivity;
            if (activity != null) {
                activity.convertToTranslucent(null, null);
            }
            onWakeUp();
            if (!fromSystem && !this.mFinished) {
                if (this.mActivity == null) {
                    Slog.w(this.mTag, "WakeUp was called before the dream was attached.");
                    return;
                }
                try {
                    this.mDreamManager.finishSelf(this.mDreamToken, false);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        if (this.mDebug) {
            Slog.v(this.mTag, "onDestroy()");
        }
        detach();
        super.onDestroy();
    }

    public static DreamMetadata getDreamMetadata(Context context, ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        TypedArray rawMetadata = readMetadata(pm, serviceInfo);
        if (rawMetadata != null) {
            try {
                DreamMetadata dreamMetadata = new DreamMetadata(convertToComponentName(rawMetadata.getString(0), serviceInfo), rawMetadata.getDrawable(1), rawMetadata.getBoolean(2, false));
                if (rawMetadata != null) {
                    rawMetadata.close();
                }
                return dreamMetadata;
            } catch (Throwable th) {
                if (rawMetadata != null) {
                    try {
                        rawMetadata.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (rawMetadata != null) {
            rawMetadata.close();
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1180=4] */
    private static TypedArray readMetadata(PackageManager pm, ServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.metaData == null) {
            return null;
        }
        try {
            XmlResourceParser parser = serviceInfo.loadXmlMetaData(pm, DREAM_META_DATA);
            if (parser == null) {
                if (DEBUG) {
                    Log.w(TAG, "No android.service.dream metadata");
                }
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            if (parser.getName().equals("dream")) {
                TypedArray obtainAttributes = pm.getResourcesForApplication(serviceInfo.applicationInfo).obtainAttributes(attrs, R.styleable.Dream);
                if (parser != null) {
                    parser.close();
                }
                return obtainAttributes;
            }
            if (DEBUG) {
                Log.w(TAG, "Metadata does not start with dream tag");
            }
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            if (DEBUG) {
                Log.e(TAG, "Error parsing: " + serviceInfo.packageName, e);
            }
            return null;
        }
    }

    private static ComponentName convertToComponentName(String flattenedString, ServiceInfo serviceInfo) {
        if (flattenedString == null) {
            return null;
        }
        if (!flattenedString.contains("/")) {
            return new ComponentName(serviceInfo.packageName, flattenedString);
        }
        return ComponentName.unflattenFromString(flattenedString);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void detach() {
        if (this.mStarted) {
            if (this.mDebug) {
                Slog.v(this.mTag, "detach(): Calling onDreamingStopped()");
            }
            this.mStarted = false;
            onDreamingStopped();
        }
        Activity activity = this.mActivity;
        if (activity != null && !activity.isFinishing()) {
            this.mActivity.finishAndRemoveTask();
        } else {
            finish();
        }
        this.mDreamToken = null;
        this.mCanDoze = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void attach(IBinder dreamToken, boolean canDoze, final IRemoteCallback started) {
        if (this.mDreamToken != null) {
            Slog.e(this.mTag, "attach() called when dream with token=" + this.mDreamToken + " already attached");
        } else if (this.mFinished || this.mWaking) {
            Slog.w(this.mTag, "attach() called after dream already finished");
            try {
                this.mDreamManager.finishSelf(dreamToken, true);
            } catch (RemoteException e) {
            }
        } else {
            this.mDreamToken = dreamToken;
            this.mCanDoze = canDoze;
            if (this.mWindowless && !canDoze) {
                throw new IllegalStateException("Only doze dreams can be windowless");
            }
            Runnable runnable = new Runnable() { // from class: android.service.dreams.DreamService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DreamService.this.m3525lambda$attach$0$androidservicedreamsDreamService(started);
                }
            };
            this.mDispatchAfterOnAttachedToWindow = runnable;
            if (!this.mWindowless) {
                Intent i = new Intent(this, DreamActivity.class);
                i.setPackage(getApplicationContext().getPackageName());
                i.setFlags(268435456);
                i.putExtra("binder", new DreamActivityCallback(this.mDreamToken));
                ServiceInfo serviceInfo = fetchServiceInfo(this, new ComponentName(this, getClass()));
                i.putExtra("title", fetchDreamLabel(this, serviceInfo));
                try {
                    if (!ActivityTaskManager.getService().startDreamActivity(i)) {
                        detach();
                        return;
                    }
                    return;
                } catch (RemoteException e2) {
                    Log.w(this.mTag, "Could not connect to activity task manager to start dream activity");
                    e2.rethrowFromSystemServer();
                    return;
                }
            }
            runnable.run();
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$attach$0$android-service-dreams-DreamService  reason: not valid java name */
    public /* synthetic */ void m3525lambda$attach$0$androidservicedreamsDreamService(IRemoteCallback started) {
        if (this.mWindow != null || this.mWindowless) {
            this.mStarted = true;
            try {
                onDreamingStarted();
                try {
                    started.sendResult(null);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (Throwable th) {
                try {
                    started.sendResult(null);
                    throw th;
                } catch (RemoteException e2) {
                    throw e2.rethrowFromSystemServer();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onWindowCreated(Window w) {
        this.mWindow = w;
        w.setCallback(this);
        this.mWindow.requestFeature(1);
        WindowManager.LayoutParams lp = this.mWindow.getAttributes();
        lp.flags |= (this.mFullscreen ? 1024 : 0) | 21561601 | (this.mScreenBright ? 128 : 0);
        lp.layoutInDisplayCutoutMode = 3;
        this.mWindow.setAttributes(lp);
        this.mWindow.clearFlags(Integer.MIN_VALUE);
        this.mWindow.getDecorView().getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        this.mWindow.setDecorFitsSystemWindows(false);
        this.mWindow.getDecorView().addOnAttachStateChangeListener(new AnonymousClass2());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.service.dreams.DreamService$2  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass2 implements View.OnAttachStateChangeListener {
        AnonymousClass2() {
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View v) {
            DreamService.this.mDispatchAfterOnAttachedToWindow.run();
            DreamService.this.mOverlayConnection.request(new Consumer() { // from class: android.service.dreams.DreamService$2$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DreamService.AnonymousClass2.this.m3527x18e60bf((IDreamOverlay) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onViewAttachedToWindow$0$android-service-dreams-DreamService$2  reason: not valid java name */
        public /* synthetic */ void m3527x18e60bf(IDreamOverlay overlay) {
            try {
                overlay.startDream(DreamService.this.mWindow.getAttributes(), DreamService.this.mOverlayCallback);
            } catch (RemoteException e) {
                Log.e(DreamService.this.mTag, "could not send window attributes:" + e);
            }
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View v) {
            if (DreamService.this.mActivity == null || !DreamService.this.mActivity.isChangingConfigurations()) {
                DreamService.this.mWindow = null;
                DreamService.this.mActivity = null;
                DreamService.this.finish();
            }
        }
    }

    private boolean getWindowFlagValue(int flag, boolean defaultValue) {
        Window window = this.mWindow;
        return window == null ? defaultValue : (window.getAttributes().flags & flag) != 0;
    }

    private void applyWindowFlags(int flags, int mask) {
        Window window = this.mWindow;
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.flags = applyFlags(lp.flags, flags, mask);
            this.mWindow.setAttributes(lp);
            this.mWindow.getWindowManager().updateViewLayout(this.mWindow.getDecorView(), lp);
        }
    }

    private int applyFlags(int oldFlags, int flags, int mask) {
        return ((~mask) & oldFlags) | (flags & mask);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean fetchShouldShowComplications(Context context, ServiceInfo serviceInfo) {
        DreamMetadata metadata = getDreamMetadata(context, serviceInfo);
        if (metadata != null) {
            return metadata.showComplications;
        }
        return false;
    }

    private static CharSequence fetchDreamLabel(Context context, ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        return serviceInfo.loadLabel(pm);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ServiceInfo fetchServiceInfo(Context context, ComponentName componentName) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getServiceInfo(componentName, PackageManager.ComponentInfoFlags.of(128L));
        } catch (PackageManager.NameNotFoundException e) {
            if (DEBUG) {
                Log.w(TAG, "cannot find component " + componentName.flattenToShortString());
                return null;
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Service
    public void dump(final FileDescriptor fd, PrintWriter pw, final String[] args) {
        DumpUtils.dumpAsync(this.mHandler, new DumpUtils.Dump() { // from class: android.service.dreams.DreamService$$ExternalSyntheticLambda1
            @Override // com.android.internal.util.DumpUtils.Dump
            public final void dump(PrintWriter printWriter, String str) {
                DreamService.this.m3526lambda$dump$1$androidservicedreamsDreamService(fd, args, printWriter, str);
            }
        }, pw, "", 1000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$1$android-service-dreams-DreamService  reason: not valid java name */
    public /* synthetic */ void m3526lambda$dump$1$androidservicedreamsDreamService(FileDescriptor fd, String[] args, PrintWriter pw1, String prefix) {
        dumpOnHandler(fd, pw1, args);
    }

    protected void dumpOnHandler(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print(this.mTag + ": ");
        if (this.mFinished) {
            pw.println("stopped");
        } else {
            pw.println("running (dreamToken=" + this.mDreamToken + NavigationBarInflaterView.KEY_CODE_END);
        }
        pw.println("  window: " + this.mWindow);
        pw.print("  flags:");
        if (isInteractive()) {
            pw.print(" interactive");
        }
        if (isFullscreen()) {
            pw.print(" fullscreen");
        }
        if (isScreenBright()) {
            pw.print(" bright");
        }
        if (isWindowless()) {
            pw.print(" windowless");
        }
        if (isDozing()) {
            pw.print(" dozing");
        } else if (canDoze()) {
            pw.print(" candoze");
        }
        pw.println();
        if (canDoze()) {
            pw.println("  doze screen state: " + Display.stateToString(this.mDozeScreenState));
            pw.println("  doze screen brightness: " + this.mDozeScreenBrightness);
        }
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class DreamServiceWrapper extends IDreamService.Stub {
        DreamServiceWrapper() {
        }

        @Override // android.service.dreams.IDreamService
        public void attach(final IBinder dreamToken, final boolean canDoze, final IRemoteCallback started) {
            DreamService.this.mHandler.post(new Runnable() { // from class: android.service.dreams.DreamService$DreamServiceWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DreamService.DreamServiceWrapper.this.m3528x1dde7aa0(dreamToken, canDoze, started);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$attach$0$android-service-dreams-DreamService$DreamServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3528x1dde7aa0(IBinder dreamToken, boolean canDoze, IRemoteCallback started) {
            DreamService.this.attach(dreamToken, canDoze, started);
        }

        @Override // android.service.dreams.IDreamService
        public void detach() {
            Handler handler = DreamService.this.mHandler;
            final DreamService dreamService = DreamService.this;
            handler.post(new Runnable() { // from class: android.service.dreams.DreamService$DreamServiceWrapper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    DreamService.this.detach();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$wakeUp$2$android-service-dreams-DreamService$DreamServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3529x16d3ed3c() {
            DreamService.this.wakeUp(true);
        }

        @Override // android.service.dreams.IDreamService
        public void wakeUp() {
            DreamService.this.mHandler.post(new Runnable() { // from class: android.service.dreams.DreamService$DreamServiceWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DreamService.DreamServiceWrapper.this.m3529x16d3ed3c();
                }
            });
        }

        @Override // android.service.dreams.IDreamService
        public void notifyAodAction(int state) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public final class DreamActivityCallback extends Binder {
        private final IBinder mActivityDreamToken;

        DreamActivityCallback(IBinder token) {
            this.mActivityDreamToken = token;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void onActivityCreated(DreamActivity activity) {
            if (this.mActivityDreamToken != DreamService.this.mDreamToken || DreamService.this.mFinished) {
                Slog.d(DreamService.TAG, "DreamActivity was created after the dream was finished or a new dream started, finishing DreamActivity");
                if (!activity.isFinishing()) {
                    activity.finishAndRemoveTask();
                }
            } else if (DreamService.this.mActivity != null) {
                Slog.w(DreamService.TAG, "A DreamActivity has already been started, finishing latest DreamActivity");
                if (!activity.isFinishing()) {
                    activity.finishAndRemoveTask();
                }
            } else {
                DreamService.this.mActivity = activity;
                DreamService.this.onWindowCreated(activity.getWindow());
            }
        }
    }

    /* loaded from: classes3.dex */
    public static final class DreamMetadata {
        public final Drawable previewImage;
        public final ComponentName settingsActivity;
        public final boolean showComplications;

        DreamMetadata(ComponentName settingsActivity, Drawable previewImage, boolean showComplications) {
            this.settingsActivity = settingsActivity;
            this.previewImage = previewImage;
            this.showComplications = showComplications;
        }
    }
}
