package com.android.server.display;

import android.app.AppOpsManager;
import android.app.compat.CompatChanges;
import android.companion.virtual.IVirtualDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.ColorSpace;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.devicestate.DeviceStateManagerInternal;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.Curve;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayViewport;
import android.hardware.display.DisplayedContentSample;
import android.hardware.display.DisplayedContentSamplingAttributes;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplayConfig;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.graphics.common.DisplayDecorationSupport;
import android.hardware.input.InputManagerInternal;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.sysprop.DisplayProperties;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Spline;
import android.view.Display;
import android.view.DisplayEventReceiver;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.window.DisplayWindowPolicyController;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.camera.CameraServiceProxy;
import com.android.server.companion.virtual.VirtualDeviceManagerInternal;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.LogicalDisplayMapper;
import com.android.server.display.utils.SensorUtils;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.SurfaceAnimationThread;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.display.ITranDisplayManagerService;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class DisplayManagerService extends SystemService {
    private static final int CALLING_PID_NO_CALLING = -1;
    private static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.dms", false);
    static final int DEVICE_STATE_CLOSED = 0;
    static final int DEVICE_STATE_HALF_OPENED = 1;
    static final int DEVICE_STATE_NOT_SET = -1;
    static final int DEVICE_STATE_OPENED = 2;
    static final long DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE = 170503758;
    private static final int FORCE_MODE_EXTERNAL_SCREEN = 2;
    private static final int FORCE_MODE_EXTERNAL_SCREEN_ONCE = 3;
    private static final int FORCE_MODE_FOLLOW_SYSTEM = 0;
    private static final int FORCE_MODE_INTERNAL_SCREEN = 1;
    private static final String FORCE_WIFI_DISPLAY_ENABLE = "persist.debug.wfd.enable";
    private static final int MSG_DELIVER_DISPLAY_EVENT = 3;
    private static final int MSG_DELIVER_DISPLAY_EVENT_FRAME_RATE_OVERRIDE = 7;
    private static final int MSG_DELIVER_DISPLAY_GROUP_EVENT = 8;
    private static final int MSG_DELIVER_DUAL_DISPLAY_EVENT = 111;
    private static final int MSG_LOAD_BRIGHTNESS_CONFIGURATIONS = 6;
    private static final int MSG_REGISTER_ADDITIONAL_DISPLAY_ADAPTERS = 2;
    private static final int MSG_REGISTER_DEFAULT_DISPLAY_ADAPTERS = 1;
    private static final int MSG_REQUEST_TRAVERSAL = 4;
    private static final int MSG_UPDATE_VIEWPORT = 5;
    private static final String PROP_DEFAULT_DISPLAY_TOP_INSET = "persist.sys.displayinset.top";
    private static final String TAG = "DisplayManagerService";
    private static final float THRESHOLD_FOR_REFRESH_RATES_DIVISORS = 9.0E-4f;
    private static final long WAIT_FOR_DEFAULT_DISPLAY_TIMEOUT = 10000;
    public static boolean mDualBootAnimSupport;
    public static boolean mDualDisplayFlipSupport;
    public static final boolean mDualDisplaySupport;
    public static boolean mMultipleDisplayFlipSupport;
    private final boolean mAllowNonNativeRefreshRateOverride;
    private boolean mAreUserDisabledHdrTypesAllowed;
    private final BrightnessSynchronizer mBrightnessSynchronizer;
    private BrightnessTracker mBrightnessTracker;
    public final SparseArray<CallbackRecord> mCallbacks;
    private int mCallingPidForSetMode;
    private final Context mContext;
    private int mCurrentForcedMode;
    private int mCurrentUserId;
    private final int mDefaultDisplayDefaultColorMode;
    private int mDefaultDisplayTopInset;
    private int mDeviceStateFromFold;
    private DeviceStateManagerInternal mDeviceStateManager;
    private int mDeviceStateRealUsed;
    private final SparseArray<IntArray> mDisplayAccessUIDs;
    private final ArrayList<DisplayAdapter> mDisplayAdapters;
    private final DisplayBlanker mDisplayBlanker;
    private final SparseArray<BrightnessPair> mDisplayBrightnesses;
    private final DisplayDeviceRepository mDisplayDeviceRepo;
    private final CopyOnWriteArrayList<DisplayManagerInternal.DisplayGroupListener> mDisplayGroupListeners;
    private final DisplayModeDirector mDisplayModeDirector;
    private DisplayManagerInternal.DisplayPowerCallbacks mDisplayPowerCallbacks;
    private final SparseArray<DisplayPowerController> mDisplayPowerControllers;
    private final SparseIntArray mDisplayStates;
    private final CopyOnWriteArrayList<DisplayManagerInternal.DisplayTransactionListener> mDisplayTransactionListeners;
    final SparseArray<Pair<IVirtualDevice, DisplayWindowPolicyController>> mDisplayWindowPolicyControllers;
    public final SparseArray<DualCallbackRecord> mDualCallbacks;
    private final HashSet<String> mDualDisplayComponent;
    private final Object mDualDisplayLock;
    private final DisplayManagerHandler mHandler;
    private final BroadcastReceiver mIdleModeReceiver;
    private final Injector mInjector;
    private InputManagerInternal mInputManagerInternal;
    private boolean mIsBootCompleted;
    private boolean mIsDocked;
    private boolean mIsDreaming;
    private final LogicalDisplayMapper mLogicalDisplayMapper;
    private boolean mMinimalPostProcessingAllowed;
    private final Curve mMinimumBrightnessCurve;
    private final Spline mMinimumBrightnessSpline;
    public boolean mOnlyCore;
    private boolean mPendingTraversal;
    private final PersistentDataStore mPersistentDataStore;
    private Handler mPowerHandler;
    private IMediaProjectionManager mProjectionService;
    private boolean mRequestOpenDual;
    public boolean mSafeMode;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private Point mStableDisplaySize;
    private final SyncRoot mSyncRoot;
    private boolean mSystemReady;
    private final ArrayList<CallbackRecord> mTempCallbacks;
    private ArrayList<DualCallbackRecord> mTempDualCallbacks;
    private final ArrayList<DisplayViewport> mTempViewports;
    private final Handler mUiHandler;
    private int[] mUserDisabledHdrTypes;
    private Display.Mode mUserPreferredMode;
    private final ArrayList<DisplayViewport> mViewports;
    private VirtualDisplayAdapter mVirtualDisplayAdapter;
    private final ColorSpace mWideColorSpace;
    private WifiDisplayAdapter mWifiDisplayAdapter;
    private int mWifiDisplayScanRequestCount;
    private WindowManagerInternal mWindowManagerInternal;

    /* loaded from: classes.dex */
    public interface Clock {
        long uptimeMillis();
    }

    /* loaded from: classes.dex */
    public static final class SyncRoot {
    }

    static {
        mDualBootAnimSupport = SystemProperties.getInt("ro.product.dual_bootanim.support", 0) == 1;
        mMultipleDisplayFlipSupport = SystemProperties.getInt("ro.product.multiple_display_flip.support", 0) == 1;
        mDualDisplayFlipSupport = SystemProperties.getInt("ro.product.dual_display_flip.support", 0) == 1;
        mDualDisplaySupport = SystemProperties.getInt("ro.product.dualdisplay.support", 0) == 1;
    }

    public DisplayManagerService(Context context) {
        this(context, new Injector());
    }

    DisplayManagerService(Context context, Injector injector) {
        super(context);
        this.mUserDisabledHdrTypes = new int[0];
        this.mAreUserDisabledHdrTypesAllowed = true;
        this.mDeviceStateFromFold = mDualDisplaySupport ? 99 : -1;
        this.mDeviceStateRealUsed = -1;
        this.mCurrentForcedMode = 0;
        this.mCallingPidForSetMode = -1;
        this.mIsBootCompleted = false;
        SyncRoot syncRoot = new SyncRoot();
        this.mSyncRoot = syncRoot;
        this.mCallbacks = new SparseArray<>();
        this.mDisplayWindowPolicyControllers = new SparseArray<>();
        this.mDisplayAdapters = new ArrayList<>();
        this.mDisplayTransactionListeners = new CopyOnWriteArrayList<>();
        this.mDisplayGroupListeners = new CopyOnWriteArrayList<>();
        this.mDisplayPowerControllers = new SparseArray<>();
        this.mDisplayBlanker = new DisplayBlanker() { // from class: com.android.server.display.DisplayManagerService.1
            @Override // com.android.server.display.DisplayBlanker
            public synchronized void requestDisplayState(int displayId, int state, float brightness, float sdrBrightness) {
                boolean stateChanged;
                boolean allInactive = true;
                boolean allOff = true;
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    int index = DisplayManagerService.this.mDisplayStates.indexOfKey(displayId);
                    if (index > -1) {
                        int currentState = DisplayManagerService.this.mDisplayStates.valueAt(index);
                        stateChanged = state != currentState;
                        if (stateChanged) {
                            int size = DisplayManagerService.this.mDisplayStates.size();
                            int i = 0;
                            while (i < size) {
                                int displayState = i == index ? state : DisplayManagerService.this.mDisplayStates.valueAt(i);
                                if (displayState != 1) {
                                    allOff = false;
                                }
                                if (Display.isActiveState(displayState)) {
                                    allInactive = false;
                                }
                                if (!allOff && !allInactive) {
                                    break;
                                }
                                i++;
                            }
                        }
                    } else {
                        stateChanged = false;
                    }
                }
                if (state == 1) {
                    DisplayManagerService.this.requestDisplayStateInternal(displayId, state, brightness, sdrBrightness);
                }
                if (stateChanged) {
                    DisplayManagerService.this.mDisplayPowerCallbacks.onDisplayStateChange(allInactive, allOff);
                }
                if (state != 1) {
                    DisplayManagerService.this.requestDisplayStateInternal(displayId, state, brightness, sdrBrightness);
                }
            }
        };
        this.mDisplayStates = new SparseIntArray();
        this.mDisplayBrightnesses = new SparseArray<>();
        this.mStableDisplaySize = new Point();
        this.mViewports = new ArrayList<>();
        PersistentDataStore persistentDataStore = new PersistentDataStore();
        this.mPersistentDataStore = persistentDataStore;
        this.mTempCallbacks = new ArrayList<>();
        this.mTempViewports = new ArrayList<>();
        this.mDisplayAccessUIDs = new SparseArray<>();
        this.mIdleModeReceiver = new BroadcastReceiver() { // from class: com.android.server.display.DisplayManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
                boolean z = true;
                if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                    int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                    DisplayManagerService.this.mIsDocked = dockState == 1 || dockState == 3 || dockState == 4;
                }
                if ("android.intent.action.DREAMING_STARTED".equals(intent.getAction())) {
                    DisplayManagerService.this.mIsDreaming = true;
                } else if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction())) {
                    DisplayManagerService.this.mIsDreaming = false;
                }
                DisplayManagerService displayManagerService = DisplayManagerService.this;
                if (!displayManagerService.mIsDocked || !DisplayManagerService.this.mIsDreaming) {
                    z = false;
                }
                displayManagerService.setDockedAndIdleEnabled(z, 0);
            }
        };
        this.mDualDisplayLock = new Object();
        this.mDualDisplayComponent = new HashSet<>();
        this.mRequestOpenDual = false;
        this.mTempDualCallbacks = new ArrayList<>();
        this.mDualCallbacks = new SparseArray<>();
        this.mInjector = injector;
        this.mContext = context;
        DisplayManagerHandler displayManagerHandler = new DisplayManagerHandler(DisplayThread.get().getLooper());
        this.mHandler = displayManagerHandler;
        this.mUiHandler = UiThread.getHandler();
        DisplayDeviceRepository displayDeviceRepository = new DisplayDeviceRepository(syncRoot, persistentDataStore);
        this.mDisplayDeviceRepo = displayDeviceRepository;
        this.mLogicalDisplayMapper = new LogicalDisplayMapper(context, displayDeviceRepository, new LogicalDisplayListener(), syncRoot, displayManagerHandler, new DeviceStateToLayoutMap());
        this.mDisplayModeDirector = new DisplayModeDirector(context, displayManagerHandler);
        this.mBrightnessSynchronizer = new BrightnessSynchronizer(context);
        Resources resources = context.getResources();
        this.mDefaultDisplayDefaultColorMode = context.getResources().getInteger(17694784);
        this.mDefaultDisplayTopInset = SystemProperties.getInt(PROP_DEFAULT_DISPLAY_TOP_INSET, -1);
        float[] lux = getFloatArray(resources.obtainTypedArray(17236091));
        float[] nits = getFloatArray(resources.obtainTypedArray(17236092));
        this.mMinimumBrightnessCurve = new Curve(lux, nits);
        this.mMinimumBrightnessSpline = Spline.createSpline(lux, nits);
        this.mCurrentUserId = 0;
        ColorSpace[] colorSpaces = SurfaceControl.getCompositionColorSpaces();
        this.mWideColorSpace = colorSpaces[1];
        this.mAllowNonNativeRefreshRateOverride = injector.getAllowNonNativeRefreshRateOverride();
        this.mSystemReady = false;
    }

    public void setupSchedulerPolicies() {
        Process.setThreadGroupAndCpuset(DisplayThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(AnimationThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(SurfaceAnimationThread.get().getThreadId(), 5);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        synchronized (this.mSyncRoot) {
            this.mPersistentDataStore.loadIfNeeded();
            loadStableDisplayValuesLocked();
        }
        this.mHandler.sendEmptyMessage(1);
        DisplayManagerGlobal.invalidateLocalDisplayInfoCaches();
        publishBinderService("display", new BinderService(), true);
        publishLocalService(DisplayManagerInternal.class, new LocalService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 100) {
            synchronized (this.mSyncRoot) {
                long timeout = SystemClock.uptimeMillis() + this.mInjector.getDefaultDisplayDelayTimeout();
                while (true) {
                    if (this.mLogicalDisplayMapper.getDisplayLocked(0) != null && this.mVirtualDisplayAdapter != null) {
                    }
                    long delay = timeout - SystemClock.uptimeMillis();
                    if (delay <= 0) {
                        throw new RuntimeException("Timeout waiting for default display to be initialized. DefaultDisplay=" + this.mLogicalDisplayMapper.getDisplayLocked(0) + ", mVirtualDisplayAdapter=" + this.mVirtualDisplayAdapter);
                    }
                    if (DEBUG) {
                        Slog.d(TAG, "waitForDefaultDisplay: waiting, timeout=" + delay);
                    }
                    try {
                        this.mSyncRoot.wait(delay);
                    } catch (InterruptedException e) {
                    }
                }
            }
        } else if (phase == 1000) {
            this.mDisplayModeDirector.onBootCompleted();
            this.mLogicalDisplayMapper.onBootCompleted();
            onBootCompleted();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        final int newUserId = to.getUserIdentifier();
        final int userSerial = getUserManager().getUserSerialNumber(newUserId);
        synchronized (this.mSyncRoot) {
            final boolean userSwitching = this.mCurrentUserId != newUserId;
            if (userSwitching) {
                this.mCurrentUserId = newUserId;
            }
            this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayManagerService.this.m3331x439a2cb0(userSwitching, userSerial, newUserId, (LogicalDisplay) obj);
                }
            });
            handleSettingsChange();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserSwitching$0$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3331x439a2cb0(boolean userSwitching, int userSerial, int newUserId, LogicalDisplay logicalDisplay) {
        DisplayPowerController dpc;
        if (logicalDisplay.getDisplayInfoLocked().type != 1 || (dpc = this.mDisplayPowerControllers.get(logicalDisplay.getDisplayIdLocked())) == null) {
            return;
        }
        if (userSwitching) {
            BrightnessConfiguration config = getBrightnessConfigForDisplayWithPdsFallbackLocked(logicalDisplay.getPrimaryDisplayDeviceLocked().getUniqueId(), userSerial);
            dpc.setBrightnessConfiguration(config);
        }
        dpc.onSwitchUser(newUserId);
    }

    public void windowManagerAndInputReady() {
        synchronized (this.mSyncRoot) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            this.mDeviceStateManager = (DeviceStateManagerInternal) LocalServices.getService(DeviceStateManagerInternal.class);
            ((DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class)).registerCallback(new HandlerExecutor(this.mHandler), new DeviceStateListener());
            scheduleTraversalLocked(false);
        }
    }

    public void systemReady(boolean safeMode, boolean onlyCore) {
        synchronized (this.mSyncRoot) {
            this.mSafeMode = safeMode;
            this.mOnlyCore = onlyCore;
            this.mSystemReady = true;
            recordTopInsetLocked(this.mLogicalDisplayMapper.getDisplayLocked(0));
            updateSettingsLocked();
            updateUserDisabledHdrTypesFromSettingsLocked();
            updateUserPreferredDisplayModeSettingsLocked();
        }
        this.mDisplayModeDirector.setDesiredDisplayModeSpecsListener(new DesiredDisplayModeSpecsObserver());
        this.mDisplayModeDirector.start(this.mSensorManager);
        this.mHandler.sendEmptyMessage(2);
        this.mSettingsObserver = new SettingsObserver();
        this.mBrightnessSynchronizer.startSynchronizing();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        filter.addAction("android.intent.action.DOCK_EVENT");
        this.mContext.registerReceiver(this.mIdleModeReceiver, filter);
    }

    Handler getDisplayHandler() {
        return this.mHandler;
    }

    DisplayDeviceRepository getDisplayDeviceRepository() {
        return this.mDisplayDeviceRepo;
    }

    private void loadStableDisplayValuesLocked() {
        Point size = this.mPersistentDataStore.getStableDisplaySize();
        if (size.x > 0 && size.y > 0) {
            this.mStableDisplaySize.set(size.x, size.y);
            return;
        }
        Resources res = this.mContext.getResources();
        int width = res.getInteger(17694954);
        int height = res.getInteger(17694953);
        if (width > 0 && height > 0) {
            setStableDisplaySizeLocked(width, height);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Point getStableDisplaySizeInternal() {
        Point r = new Point();
        synchronized (this.mSyncRoot) {
            if (this.mStableDisplaySize.x > 0 && this.mStableDisplaySize.y > 0) {
                r.set(this.mStableDisplaySize.x, this.mStableDisplaySize.y);
            }
        }
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDisplayTransactionListenerInternal(DisplayManagerInternal.DisplayTransactionListener listener) {
        this.mDisplayTransactionListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterDisplayTransactionListenerInternal(DisplayManagerInternal.DisplayTransactionListener listener) {
        this.mDisplayTransactionListeners.remove(listener);
    }

    void setDisplayInfoOverrideFromWindowManagerInternal(int displayId, DisplayInfo info) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null && display.setDisplayInfoOverrideFromWindowManagerLocked(info)) {
                handleLogicalDisplayChangedLocked(display);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getNonOverrideDisplayInfoInternal(int displayId, DisplayInfo outInfo) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null) {
                display.getNonOverrideDisplayInfoLocked(outInfo);
            }
        }
    }

    void performTraversalInternal(SurfaceControl.Transaction t) {
        synchronized (this.mSyncRoot) {
            Pair<Boolean, Boolean> resultLice = ITranWindowManagerService.Instance().performTraversalInternal(this.mPendingTraversal);
            if (resultLice != null) {
                if (((Boolean) resultLice.first).booleanValue()) {
                    return;
                }
                if (!((Boolean) resultLice.second).booleanValue()) {
                    this.mPendingTraversal = false;
                }
            }
            performTraversalLocked(t);
            Iterator<DisplayManagerInternal.DisplayTransactionListener> it = this.mDisplayTransactionListeners.iterator();
            while (it.hasNext()) {
                DisplayManagerInternal.DisplayTransactionListener listener = it.next();
                listener.onDisplayTransaction(t);
            }
        }
    }

    private float clampBrightness(int displayState, float brightnessState) {
        if (displayState == 1) {
            return -1.0f;
        }
        if (brightnessState != -1.0f && brightnessState < 0.0f) {
            return Float.NaN;
        }
        if (brightnessState > 1.0f) {
            return 1.0f;
        }
        return brightnessState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestDisplayStateInternal(int displayId, int state, float brightnessState, float sdrBrightnessState) {
        if (state == 0) {
            state = 2;
        }
        float brightnessState2 = clampBrightness(state, brightnessState);
        float sdrBrightnessState2 = clampBrightness(state, sdrBrightnessState);
        synchronized (this.mSyncRoot) {
            int index = this.mDisplayStates.indexOfKey(displayId);
            BrightnessPair brightnessPair = index < 0 ? null : this.mDisplayBrightnesses.valueAt(index);
            if (index >= 0 && (this.mDisplayStates.valueAt(index) != state || brightnessPair.brightness != brightnessState2 || brightnessPair.sdrBrightness != sdrBrightnessState2)) {
                String traceMessage = "requestDisplayStateInternal(" + displayId + ", " + Display.stateToString(state) + ", brightness=" + brightnessState2 + ", sdrBrightness=" + sdrBrightnessState2 + ")";
                Trace.asyncTraceBegin(131072L, traceMessage, displayId);
                this.mDisplayStates.setValueAt(index, state);
                brightnessPair.brightness = brightnessState2;
                brightnessPair.sdrBrightness = sdrBrightnessState2;
                Runnable runnable = updateDisplayStateLocked(this.mLogicalDisplayMapper.getDisplayLocked(displayId).getPrimaryDisplayDeviceLocked());
                if (runnable != null) {
                    runnable.run();
                }
                Trace.asyncTraceEnd(131072L, traceMessage, displayId);
            }
        }
    }

    /* loaded from: classes.dex */
    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(DisplayManagerService.this.mHandler);
            DisplayManagerService.this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("minimal_post_processing_allowed"), false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            DisplayManagerService.this.handleSettingsChange();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChange() {
        synchronized (this.mSyncRoot) {
            updateSettingsLocked();
            scheduleTraversalLocked(false);
        }
    }

    private void updateSettingsLocked() {
        this.mMinimalPostProcessingAllowed = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "minimal_post_processing_allowed", 1, -2) != 0;
    }

    private void updateUserDisabledHdrTypesFromSettingsLocked() {
        this.mAreUserDisabledHdrTypesAllowed = Settings.Global.getInt(this.mContext.getContentResolver(), "are_user_disabled_hdr_formats_allowed", 1) != 0;
        String userDisabledHdrTypes = Settings.Global.getString(this.mContext.getContentResolver(), "user_disabled_hdr_formats");
        if (userDisabledHdrTypes != null) {
            try {
                String[] userDisabledHdrTypeStrings = TextUtils.split(userDisabledHdrTypes, ",");
                this.mUserDisabledHdrTypes = new int[userDisabledHdrTypeStrings.length];
                for (int i = 0; i < userDisabledHdrTypeStrings.length; i++) {
                    this.mUserDisabledHdrTypes[i] = Integer.parseInt(userDisabledHdrTypeStrings[i]);
                }
                return;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Failed to parse USER_DISABLED_HDR_FORMATS. Clearing the setting.", e);
                clearUserDisabledHdrTypesLocked();
                return;
            }
        }
        clearUserDisabledHdrTypesLocked();
    }

    private void clearUserDisabledHdrTypesLocked() {
        this.mUserDisabledHdrTypes = new int[0];
        synchronized (this.mSyncRoot) {
            Settings.Global.putString(this.mContext.getContentResolver(), "user_disabled_hdr_formats", "");
        }
    }

    private void updateUserPreferredDisplayModeSettingsLocked() {
        float refreshRate = Settings.Global.getFloat(this.mContext.getContentResolver(), "user_preferred_refresh_rate", 0.0f);
        int height = Settings.Global.getInt(this.mContext.getContentResolver(), "user_preferred_resolution_height", -1);
        int width = Settings.Global.getInt(this.mContext.getContentResolver(), "user_preferred_resolution_width", -1);
        Display.Mode mode = new Display.Mode(width, height, refreshRate);
        this.mUserPreferredMode = isResolutionAndRefreshRateValid(mode) ? mode : null;
    }

    private DisplayInfo getDisplayInfoForFrameRateOverride(DisplayEventReceiver.FrameRateOverride[] frameRateOverrides, DisplayInfo info, int callingUid) {
        Display.Mode[] modeArr;
        float frameRateHz = 0.0f;
        int length = frameRateOverrides.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            DisplayEventReceiver.FrameRateOverride frameRateOverride = frameRateOverrides[i];
            if (frameRateOverride.uid != callingUid) {
                i++;
            } else {
                frameRateHz = frameRateOverride.frameRateHz;
                break;
            }
        }
        if (frameRateHz == 0.0f) {
            return info;
        }
        Display.Mode currentMode = info.getMode();
        float numPeriods = currentMode.getRefreshRate() / frameRateHz;
        float numPeriodsRound = Math.round(numPeriods);
        if (Math.abs(numPeriods - numPeriodsRound) > THRESHOLD_FOR_REFRESH_RATES_DIVISORS) {
            return info;
        }
        float frameRateHz2 = currentMode.getRefreshRate() / numPeriodsRound;
        DisplayInfo overriddenInfo = new DisplayInfo();
        overriddenInfo.copyFrom(info);
        for (Display.Mode mode : info.supportedModes) {
            if (mode.equalsExceptRefreshRate(currentMode) && mode.getRefreshRate() >= frameRateHz2 - THRESHOLD_FOR_REFRESH_RATES_DIVISORS && mode.getRefreshRate() <= frameRateHz2 + THRESHOLD_FOR_REFRESH_RATES_DIVISORS) {
                if (DEBUG) {
                    Slog.d(TAG, "found matching modeId " + mode.getModeId());
                }
                overriddenInfo.refreshRateOverride = mode.getRefreshRate();
                if (!CompatChanges.isChangeEnabled((long) DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE, callingUid)) {
                    overriddenInfo.modeId = mode.getModeId();
                }
                return overriddenInfo;
            }
        }
        if (this.mAllowNonNativeRefreshRateOverride) {
            overriddenInfo.refreshRateOverride = frameRateHz2;
            if (!CompatChanges.isChangeEnabled((long) DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE, callingUid)) {
                overriddenInfo.supportedModes = (Display.Mode[]) Arrays.copyOf(info.supportedModes, info.supportedModes.length + 1);
                overriddenInfo.supportedModes[overriddenInfo.supportedModes.length - 1] = new Display.Mode(255, currentMode.getPhysicalWidth(), currentMode.getPhysicalHeight(), overriddenInfo.refreshRateOverride);
                overriddenInfo.modeId = overriddenInfo.supportedModes[overriddenInfo.supportedModes.length - 1].getModeId();
            }
            return overriddenInfo;
        }
        return info;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayInfo getDisplayInfoInternal(int displayId, int callingUid) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId, true);
            if (display != null) {
                DisplayInfo info = getDisplayInfoForFrameRateOverride(display.getFrameRateOverrides(), display.getDisplayInfoLocked(), callingUid);
                if (info.hasAccess(callingUid) || isUidPresentOnDisplayInternal(callingUid, displayId)) {
                    return info;
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCallbackInternal(IDisplayManagerCallback callback, int callingPid, int callingUid, long eventsMask) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = this.mCallbacks.get(callingPid);
            if (record != null) {
                record.updateEventsMask(eventsMask);
                return;
            }
            CallbackRecord record2 = new CallbackRecord(callingPid, callingUid, callback, eventsMask);
            try {
                IBinder binder = callback.asBinder();
                binder.linkToDeath(record2, 0);
                this.mCallbacks.put(callingPid, record2);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCallbackDied(CallbackRecord record) {
        synchronized (this.mSyncRoot) {
            if (this.mCallingPidForSetMode == record.mPid) {
                resetDisplayToFollowSystemLocked();
            }
            this.mCallbacks.remove(record.mPid);
            stopWifiDisplayScanLocked(record);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startWifiDisplayScanInternal(int callingPid) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = this.mCallbacks.get(callingPid);
            if (record == null) {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
            startWifiDisplayScanLocked(record);
        }
    }

    private void startWifiDisplayScanLocked(CallbackRecord record) {
        WifiDisplayAdapter wifiDisplayAdapter;
        if (!record.mWifiDisplayScanRequested) {
            record.mWifiDisplayScanRequested = true;
            int i = this.mWifiDisplayScanRequestCount;
            this.mWifiDisplayScanRequestCount = i + 1;
            if (i == 0 && (wifiDisplayAdapter = this.mWifiDisplayAdapter) != null) {
                wifiDisplayAdapter.requestStartScanLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopWifiDisplayScanInternal(int callingPid) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = this.mCallbacks.get(callingPid);
            if (record == null) {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
            stopWifiDisplayScanLocked(record);
        }
    }

    private void stopWifiDisplayScanLocked(CallbackRecord record) {
        if (record.mWifiDisplayScanRequested) {
            record.mWifiDisplayScanRequested = false;
            int i = this.mWifiDisplayScanRequestCount - 1;
            this.mWifiDisplayScanRequestCount = i;
            if (i == 0) {
                WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
                if (wifiDisplayAdapter != null) {
                    wifiDisplayAdapter.requestStopScanLocked();
                }
            } else if (i < 0) {
                Slog.wtf(TAG, "mWifiDisplayScanRequestCount became negative: " + this.mWifiDisplayScanRequestCount);
                this.mWifiDisplayScanRequestCount = 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connectWifiDisplayInternal(String address) {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestConnectLocked(address);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pauseWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestPauseLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resumeWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestResumeLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disconnectWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestDisconnectLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void renameWifiDisplayInternal(String address, String alias) {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestRenameLocked(address, alias);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forgetWifiDisplayInternal(String address) {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                wifiDisplayAdapter.requestForgetLocked(address);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WifiDisplayStatus getWifiDisplayStatusInternal() {
        synchronized (this.mSyncRoot) {
            WifiDisplayAdapter wifiDisplayAdapter = this.mWifiDisplayAdapter;
            if (wifiDisplayAdapter != null) {
                return wifiDisplayAdapter.getWifiDisplayStatusLocked();
            }
            return new WifiDisplayStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUserDisabledHdrTypesInternal(final int[] userDisabledHdrTypes) {
        synchronized (this.mSyncRoot) {
            if (userDisabledHdrTypes == null) {
                Slog.e(TAG, "Null is not an expected argument to setUserDisabledHdrTypesInternal");
            } else if (!isSubsetOf(Display.HdrCapabilities.HDR_TYPES, userDisabledHdrTypes)) {
                Slog.e(TAG, "userDisabledHdrTypes contains unexpected types");
            } else {
                Arrays.sort(userDisabledHdrTypes);
                if (Arrays.equals(this.mUserDisabledHdrTypes, userDisabledHdrTypes)) {
                    return;
                }
                String userDisabledFormatsString = "";
                if (userDisabledHdrTypes.length != 0) {
                    userDisabledFormatsString = TextUtils.join(",", Arrays.stream(userDisabledHdrTypes).boxed().toArray());
                }
                Settings.Global.putString(this.mContext.getContentResolver(), "user_disabled_hdr_formats", userDisabledFormatsString);
                this.mUserDisabledHdrTypes = userDisabledHdrTypes;
                if (!this.mAreUserDisabledHdrTypesAllowed) {
                    this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda9
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            DisplayManagerService.this.m3335xeddc67aa(userDisabledHdrTypes, (LogicalDisplay) obj);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setUserDisabledHdrTypesInternal$1$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3335xeddc67aa(int[] userDisabledHdrTypes, LogicalDisplay display) {
        display.setUserDisabledHdrTypes(userDisabledHdrTypes);
        handleLogicalDisplayChangedLocked(display);
    }

    private boolean isSubsetOf(int[] sortedSuperset, int[] subset) {
        for (int i : subset) {
            if (Arrays.binarySearch(sortedSuperset, i) < 0) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAreUserDisabledHdrTypesAllowedInternal(boolean areUserDisabledHdrTypesAllowed) {
        synchronized (this.mSyncRoot) {
            if (this.mAreUserDisabledHdrTypesAllowed == areUserDisabledHdrTypesAllowed) {
                return;
            }
            this.mAreUserDisabledHdrTypesAllowed = areUserDisabledHdrTypesAllowed;
            if (this.mUserDisabledHdrTypes.length == 0) {
                return;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "are_user_disabled_hdr_formats_allowed", areUserDisabledHdrTypesAllowed ? 1 : 0);
            int[] userDisabledHdrTypes = new int[0];
            if (!this.mAreUserDisabledHdrTypesAllowed) {
                userDisabledHdrTypes = this.mUserDisabledHdrTypes;
            }
            final int[] finalUserDisabledHdrTypes = userDisabledHdrTypes;
            this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayManagerService.this.m3334x4cc79809(finalUserDisabledHdrTypes, (LogicalDisplay) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAreUserDisabledHdrTypesAllowedInternal$2$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3334x4cc79809(int[] finalUserDisabledHdrTypes, LogicalDisplay display) {
        display.setUserDisabledHdrTypes(finalUserDisabledHdrTypes);
        handleLogicalDisplayChangedLocked(display);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestColorModeInternal(int displayId, int colorMode) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null && display.getRequestedColorModeLocked() != colorMode) {
                display.setRequestedColorModeLocked(colorMode);
                scheduleTraversalLocked(false);
            }
        }
    }

    private boolean validatePackageName(int uid, String packageName) {
        String[] packageNames;
        if (packageName != null && (packageNames = this.mContext.getPackageManager().getPackagesForUid(uid)) != null) {
            for (String n : packageNames) {
                if (n.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canProjectVideo(IMediaProjection projection) {
        if (projection != null) {
            try {
                if (projection.canProjectVideo()) {
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to query projection service for permissions", e);
            }
        }
        if (checkCallingPermission("android.permission.CAPTURE_VIDEO_OUTPUT", "canProjectVideo()")) {
            return true;
        }
        return canProjectSecureVideo(projection);
    }

    private boolean canProjectSecureVideo(IMediaProjection projection) {
        if (projection != null) {
            try {
                if (projection.canProjectSecureVideo()) {
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to query projection service for permissions", e);
            }
        }
        return checkCallingPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT", "canProjectSecureVideo()");
    }

    private boolean checkCallingPermission(String permission, String func) {
        if (this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
        Slog.w(TAG, msg);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerStateInternalForLucid() {
        synchronized (this.mSyncRoot) {
            DisplayPowerController displayPowerController = this.mDisplayPowerControllers.get(0);
            if (displayPowerController != null) {
                displayPowerController.updatePowerStateForLucid();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int createVirtualDisplayInternal(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IMediaProjection projection, IVirtualDevice virtualDevice, DisplayWindowPolicyController dwpc, String packageName) {
        int flags;
        int flags2;
        int displayId;
        int callingUid = Binder.getCallingUid();
        if (!validatePackageName(callingUid, packageName)) {
            throw new SecurityException("packageName must match the calling uid");
        }
        if (callback == null) {
            throw new IllegalArgumentException("appToken must not be null");
        }
        if (virtualDisplayConfig == null) {
            throw new IllegalArgumentException("virtualDisplayConfig must not be null");
        }
        Surface surface = virtualDisplayConfig.getSurface();
        int flags3 = virtualDisplayConfig.getFlags();
        if (virtualDevice != null) {
            VirtualDeviceManagerInternal vdm = (VirtualDeviceManagerInternal) getLocalService(VirtualDeviceManagerInternal.class);
            if (!vdm.isValidVirtualDevice(virtualDevice)) {
                throw new SecurityException("Invalid virtual device");
            }
            flags3 |= vdm.getBaseVirtualDisplayFlags(virtualDevice);
        }
        if (surface != null && surface.isSingleBuffered()) {
            throw new IllegalArgumentException("Surface can't be single-buffered");
        }
        if ((flags3 & 1) != 0) {
            flags3 |= 16;
            if ((flags3 & 32) != 0) {
                throw new IllegalArgumentException("Public display must not be marked as SHOW_WHEN_LOCKED_INSECURE");
            }
        }
        if ((flags3 & 8) != 0) {
            flags3 &= -17;
        }
        if ((flags3 & 16) == 0) {
            flags = flags3;
        } else {
            flags = flags3 & (-2049);
        }
        if (projection != null) {
            try {
                if (!getProjectionService().isValidMediaProjection(projection)) {
                    throw new SecurityException("Invalid media projection");
                }
                flags = projection.applyVirtualDisplayFlags(flags);
            } catch (RemoteException e) {
                throw new SecurityException("unable to validate media projection or flags");
            }
        }
        if (callingUid != 1000 && (flags & 16) != 0 && !canProjectVideo(projection)) {
            throw new SecurityException("Requires CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission, or an appropriate MediaProjection token in order to create a screen sharing virtual display.");
        }
        if (callingUid != 1000 && (flags & 4) != 0 && !canProjectSecureVideo(projection)) {
            throw new SecurityException("Requires CAPTURE_SECURE_VIDEO_OUTPUT or an appropriate MediaProjection token to create a secure virtual display.");
        }
        if (callingUid != 1000 && (flags & 1024) != 0 && !checkCallingPermission("android.permission.ADD_TRUSTED_DISPLAY", "createVirtualDisplay()")) {
            EventLog.writeEvent(1397638484, "162627132", Integer.valueOf(callingUid), "Attempt to create a trusted display without holding permission!");
            throw new SecurityException("Requires ADD_TRUSTED_DISPLAY permission to create a trusted virtual display.");
        } else if (callingUid != 1000 && (flags & 2048) != 0 && virtualDevice == null && !checkCallingPermission("android.permission.ADD_TRUSTED_DISPLAY", "createVirtualDisplay()")) {
            throw new SecurityException("Requires ADD_TRUSTED_DISPLAY permission to create a virtual display which is not in the default DisplayGroup.");
        } else {
            if ((flags & 4096) != 0 && callingUid != 1000 && !checkCallingPermission("android.permission.ADD_ALWAYS_UNLOCKED_DISPLAY", "createVirtualDisplay()")) {
                throw new SecurityException("Requires ADD_ALWAYS_UNLOCKED_DISPLAY permission to create an always unlocked virtual display.");
            }
            if ((flags & 1024) != 0) {
                flags2 = flags;
            } else {
                flags2 = flags & (-513);
            }
            if ((flags2 & 1536) == 512 && !checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "createVirtualDisplay()")) {
                throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mSyncRoot) {
                    displayId = createVirtualDisplayLocked(callback, projection, callingUid, packageName, surface, flags2, virtualDisplayConfig);
                    if (displayId != -1 && virtualDevice != null && dwpc != null) {
                        this.mDisplayWindowPolicyControllers.put(displayId, Pair.create(virtualDevice, dwpc));
                    }
                }
                return displayId;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private int createVirtualDisplayLocked(IVirtualDisplayCallback callback, IMediaProjection projection, int callingUid, String packageName, Surface surface, int flags, VirtualDisplayConfig virtualDisplayConfig) {
        VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
        if (virtualDisplayAdapter == null) {
            Slog.w(TAG, "Rejecting request to create private virtual display because the virtual display adapter is not available.");
            return -1;
        }
        DisplayDevice device = virtualDisplayAdapter.createVirtualDisplayLocked(callback, projection, callingUid, packageName, surface, flags, virtualDisplayConfig);
        if (device == null) {
            return -1;
        }
        this.mDisplayDeviceRepo.onDisplayDeviceEvent(device, 1);
        LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(device);
        if (display != null) {
            return display.getDisplayIdLocked();
        }
        Slog.w(TAG, "Rejecting request to create virtual display because the logical display was not created.");
        this.mVirtualDisplayAdapter.releaseVirtualDisplayLocked(callback.asBinder());
        this.mDisplayDeviceRepo.onDisplayDeviceEvent(device, 3);
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resizeVirtualDisplayInternal(IBinder appToken, int width, int height, int densityDpi) {
        synchronized (this.mSyncRoot) {
            VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
            if (virtualDisplayAdapter == null) {
                return;
            }
            virtualDisplayAdapter.resizeVirtualDisplayLocked(appToken, width, height, densityDpi);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setVirtualDisplaySurfaceInternal(IBinder appToken, Surface surface) {
        synchronized (this.mSyncRoot) {
            VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
            if (virtualDisplayAdapter == null) {
                return;
            }
            virtualDisplayAdapter.setVirtualDisplaySurfaceLocked(appToken, surface);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseVirtualDisplayInternal(IBinder appToken) {
        synchronized (this.mSyncRoot) {
            VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
            if (virtualDisplayAdapter == null) {
                return;
            }
            DisplayDevice device = virtualDisplayAdapter.releaseVirtualDisplayLocked(appToken);
            if (device != null) {
                this.mDisplayDeviceRepo.onDisplayDeviceEvent(device, 3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setVirtualDisplayStateInternal(IBinder appToken, boolean isOn) {
        synchronized (this.mSyncRoot) {
            VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
            if (virtualDisplayAdapter == null) {
                return;
            }
            virtualDisplayAdapter.setVirtualDisplayStateLocked(appToken, isOn);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDefaultDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            registerDisplayAdapterLocked(new LocalDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayDeviceRepo));
            VirtualDisplayAdapter virtualDisplayAdapter = this.mInjector.getVirtualDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayDeviceRepo);
            this.mVirtualDisplayAdapter = virtualDisplayAdapter;
            if (virtualDisplayAdapter != null) {
                registerDisplayAdapterLocked(virtualDisplayAdapter);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerAdditionalDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            if (shouldRegisterNonEssentialDisplayAdaptersLocked()) {
                registerOverlayDisplayAdapterLocked();
                registerWifiDisplayAdapterLocked();
            }
        }
    }

    private void registerOverlayDisplayAdapterLocked() {
        registerDisplayAdapterLocked(new OverlayDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayDeviceRepo, this.mUiHandler));
    }

    private void registerWifiDisplayAdapterLocked() {
        if (this.mContext.getResources().getBoolean(17891653) || SystemProperties.getInt(FORCE_WIFI_DISPLAY_ENABLE, -1) == 1 || SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            WifiDisplayAdapter wifiDisplayAdapter = new WifiDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayDeviceRepo, this.mPersistentDataStore);
            this.mWifiDisplayAdapter = wifiDisplayAdapter;
            registerDisplayAdapterLocked(wifiDisplayAdapter);
        }
    }

    private boolean shouldRegisterNonEssentialDisplayAdaptersLocked() {
        return (this.mSafeMode || this.mOnlyCore) ? false : true;
    }

    private void registerDisplayAdapterLocked(DisplayAdapter adapter) {
        this.mDisplayAdapters.add(adapter);
        adapter.registerLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplayAddedLocked(LogicalDisplay display) {
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        int displayId = display.getDisplayIdLocked();
        boolean isDefault = displayId == 0;
        configureColorModeLocked(display, device);
        if (!this.mAreUserDisabledHdrTypesAllowed) {
            display.setUserDisabledHdrTypes(this.mUserDisabledHdrTypes);
        }
        if (isDefault) {
            recordStableDisplayStatsIfNeededLocked(display);
            recordTopInsetLocked(display);
        }
        Display.Mode mode = this.mUserPreferredMode;
        if (mode != null) {
            device.setUserPreferredDisplayModeLocked(mode);
        } else {
            configurePreferredDisplayModeLocked(display);
        }
        addDisplayPowerControllerLocked(display);
        this.mDisplayStates.append(displayId, 0);
        float brightnessDefault = display.getDisplayInfoLocked().brightnessDefault;
        this.mDisplayBrightnesses.append(displayId, new BrightnessPair(brightnessDefault, brightnessDefault));
        DisplayManagerGlobal.invalidateLocalDisplayInfoCaches();
        if (isDefault) {
            this.mSyncRoot.notifyAll();
        }
        StringBuilder append = new StringBuilder().append("EVENT_DISPLAY_ADDED DualDisplaySupport:");
        boolean z = mDualDisplaySupport;
        Slog.d(TAG, append.append(z).append(" isDualDisplayEnabled: ").append(this.mLogicalDisplayMapper.isDualDisplayEnabled()).append(" displayId:").append(displayId).toString());
        if (z && this.mLogicalDisplayMapper.isDualDisplayEnabled() && !isDefault && device.getUniqueId().startsWith("local:")) {
            sendDualDisplayEventLocked(displayId, 1);
        }
        sendDisplayEventLocked(displayId, 1);
        Runnable work = updateDisplayStateLocked(device);
        if (work != null) {
            work.run();
        }
        scheduleTraversalLocked(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplayChangedLocked(LogicalDisplay display) {
        updateViewportPowerStateLocked(display);
        int displayId = display.getDisplayIdLocked();
        if (displayId == 0) {
            recordTopInsetLocked(display);
        }
        sendDisplayEventLocked(displayId, 2);
        scheduleTraversalLocked(false);
        this.mPersistentDataStore.saveIfNeeded();
        DisplayPowerController dpc = this.mDisplayPowerControllers.get(displayId);
        if (dpc != null) {
            dpc.onDisplayChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplayFrameRateOverridesChangedLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        sendDisplayEventFrameRateOverrideLocked(displayId);
        scheduleTraversalLocked(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplayRemovedLocked(LogicalDisplay display) {
        final IVirtualDevice virtualDevice;
        final int displayId = display.getDisplayIdLocked();
        DisplayPowerController dpc = (DisplayPowerController) this.mDisplayPowerControllers.removeReturnOld(displayId);
        IDisplayManagerServiceLice.Instance().onHandleLogicalDisplayRemovedLocked(display, dpc);
        if (dpc != null) {
            dpc.stop();
        }
        this.mDisplayStates.delete(displayId);
        this.mDisplayBrightnesses.delete(displayId);
        DisplayManagerGlobal.invalidateLocalDisplayInfoCaches();
        StringBuilder append = new StringBuilder().append("EVENT_DISPLAY_REMOVED DualDisplaySupport:");
        boolean z = mDualDisplaySupport;
        Slog.d(TAG, append.append(z).append(" isDualDisplayEnabled: ").append(this.mLogicalDisplayMapper.isDualDisplayEnabled()).append(" displayId:").append(displayId).toString());
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        if (z && !this.mLogicalDisplayMapper.isDualDisplayEnabled() && displayId != 0 && (device == null || !device.getUniqueId().startsWith("local:"))) {
            sendDualDisplayEventLocked(displayId, 3);
        }
        sendDisplayEventLocked(displayId, 3);
        scheduleTraversalLocked(false);
        if (this.mDisplayWindowPolicyControllers.contains(displayId) && (virtualDevice = (IVirtualDevice) ((Pair) this.mDisplayWindowPolicyControllers.removeReturnOld(displayId)).first) != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayManagerService.this.m3329x60c0e268(virtualDevice, displayId);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleLogicalDisplayRemovedLocked$3$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3329x60c0e268(IVirtualDevice virtualDevice, int displayId) {
        ((VirtualDeviceManagerInternal) getLocalService(VirtualDeviceManagerInternal.class)).onVirtualDisplayRemoved(virtualDevice, displayId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplaySwappedLocked(LogicalDisplay display) {
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        Runnable work = updateDisplayStateLocked(device);
        if (work != null) {
            this.mHandler.post(work);
        }
        int displayId = display.getDisplayIdLocked();
        DisplayPowerController dpc = this.mDisplayPowerControllers.get(displayId);
        if (dpc != null) {
            dpc.onDisplayChanged();
        }
        this.mPersistentDataStore.saveIfNeeded();
        this.mHandler.sendEmptyMessage(6);
        sendDisplayEventLocked(displayId, 5);
        handleLogicalDisplayChangedLocked(display);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLogicalDisplayDeviceStateTransitionLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        DisplayPowerController dpc = this.mDisplayPowerControllers.get(displayId);
        if (dpc != null) {
            dpc.onDeviceStateTransition();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hanldeLogicalDisplayUpdatePowerStateLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        DisplayPowerController dpc = this.mDisplayPowerControllers.get(displayId);
        if (dpc != null) {
            dpc.onDeviceStateTransition();
        }
        scheduleTraversalLocked(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hanldeLogicalDisplayDualDisplayOpenedLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        sendDualDisplayEventLocked(displayId, 1);
        hanldeLogicalDisplayUpdatePowerStateLocked(display);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hanldeLogicalDisplayDualDisplayClosedLocked(LogicalDisplay display) {
        int displayId = display.getDisplayIdLocked();
        sendDualDisplayEventLocked(displayId, 3);
        hanldeLogicalDisplayUpdatePowerStateLocked(display);
    }

    private Runnable updateDisplayStateLocked(DisplayDevice device) {
        LogicalDisplay display;
        int displayId;
        int state;
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        if ((info.flags & 32) != 0 || (display = this.mLogicalDisplayMapper.getDisplayLocked(device)) == null || (state = this.mDisplayStates.get((displayId = display.getDisplayIdLocked()))) == 0) {
            return null;
        }
        BrightnessPair brightnessPair = this.mDisplayBrightnesses.get(mDualDisplaySupport ? 0 : displayId);
        ScreenStateController.getInstance().hookSecondaryScreenState(display.isDualDisplay(), state);
        return device.requestDisplayStateLocked(state, brightnessPair.brightness, brightnessPair.sdrBrightness);
    }

    private void configureColorModeLocked(LogicalDisplay display, DisplayDevice device) {
        if (display.getPrimaryDisplayDeviceLocked() == device) {
            int colorMode = this.mPersistentDataStore.getColorMode(device);
            if (colorMode == -1) {
                if (display.getDisplayIdLocked() == 0) {
                    colorMode = this.mDefaultDisplayDefaultColorMode;
                } else {
                    colorMode = 0;
                }
            }
            display.setRequestedColorModeLocked(colorMode);
        }
    }

    private void configurePreferredDisplayModeLocked(LogicalDisplay display) {
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        Point userPreferredResolution = this.mPersistentDataStore.getUserPreferredResolution(device);
        float refreshRate = this.mPersistentDataStore.getUserPreferredRefreshRate(device);
        if (userPreferredResolution == null && Float.isNaN(refreshRate)) {
            return;
        }
        Display.Mode.Builder modeBuilder = new Display.Mode.Builder();
        if (userPreferredResolution != null) {
            modeBuilder.setResolution(userPreferredResolution.x, userPreferredResolution.y);
        }
        if (!Float.isNaN(refreshRate)) {
            modeBuilder.setRefreshRate(refreshRate);
        }
        device.setUserPreferredDisplayModeLocked(modeBuilder.build());
    }

    private void recordStableDisplayStatsIfNeededLocked(LogicalDisplay d) {
        if (this.mStableDisplaySize.x <= 0 && this.mStableDisplaySize.y <= 0) {
            DisplayInfo info = d.getDisplayInfoLocked();
            setStableDisplaySizeLocked(info.getNaturalWidth(), info.getNaturalHeight());
        }
    }

    private void recordTopInsetLocked(LogicalDisplay d) {
        int topInset;
        if (!this.mSystemReady || d == null || (topInset = d.getInsets().top) == this.mDefaultDisplayTopInset) {
            return;
        }
        this.mDefaultDisplayTopInset = topInset;
        SystemProperties.set(PROP_DEFAULT_DISPLAY_TOP_INSET, Integer.toString(topInset));
    }

    private void setStableDisplaySizeLocked(int width, int height) {
        Point point = new Point(width, height);
        this.mStableDisplaySize = point;
        try {
            this.mPersistentDataStore.setStableDisplaySize(point);
        } finally {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    Curve getMinimumBrightnessCurveInternal() {
        return this.mMinimumBrightnessCurve;
    }

    int getPreferredWideGamutColorSpaceIdInternal() {
        return this.mWideColorSpace.getId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x001c, code lost:
        r2 = -1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setUserPreferredDisplayModeInternal(int displayId, Display.Mode mode) {
        int resolutionWidth;
        synchronized (this.mSyncRoot) {
            if (mode != null) {
                try {
                    if (!isResolutionAndRefreshRateValid(mode) && displayId == -1) {
                        throw new IllegalArgumentException("width, height and refresh rate of mode should be greater than 0 when setting the global user preferred display mode.");
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            int resolutionHeight = mode.getPhysicalHeight();
            if (mode == null) {
                resolutionWidth = -1;
            } else {
                resolutionWidth = mode.getPhysicalWidth();
            }
            float refreshRate = mode == null ? 0.0f : mode.getRefreshRate();
            storeModeInPersistentDataStoreLocked(displayId, resolutionWidth, resolutionHeight, refreshRate);
            if (displayId != -1) {
                setUserPreferredModeForDisplayLocked(displayId, mode);
            } else {
                this.mUserPreferredMode = mode;
                storeModeInGlobalSettingsLocked(resolutionWidth, resolutionHeight, refreshRate, mode);
            }
        }
    }

    private void storeModeInPersistentDataStoreLocked(int displayId, int resolutionWidth, int resolutionHeight, float refreshRate) {
        DisplayDevice displayDevice = getDeviceForDisplayLocked(displayId);
        if (displayDevice == null) {
            return;
        }
        this.mPersistentDataStore.setUserPreferredResolution(displayDevice, resolutionWidth, resolutionHeight);
        this.mPersistentDataStore.setUserPreferredRefreshRate(displayDevice, refreshRate);
    }

    private void setUserPreferredModeForDisplayLocked(int displayId, Display.Mode mode) {
        DisplayDevice displayDevice = getDeviceForDisplayLocked(displayId);
        if (displayDevice == null) {
            return;
        }
        displayDevice.setUserPreferredDisplayModeLocked(mode);
    }

    private void storeModeInGlobalSettingsLocked(int resolutionWidth, int resolutionHeight, float refreshRate, final Display.Mode mode) {
        Settings.Global.putFloat(this.mContext.getContentResolver(), "user_preferred_refresh_rate", refreshRate);
        Settings.Global.putInt(this.mContext.getContentResolver(), "user_preferred_resolution_height", resolutionHeight);
        Settings.Global.putInt(this.mContext.getContentResolver(), "user_preferred_resolution_width", resolutionWidth);
        this.mDisplayDeviceRepo.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayManagerService.this.m3336x78152677(mode, (DisplayDevice) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$storeModeInGlobalSettingsLocked$4$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3336x78152677(Display.Mode mode, DisplayDevice device) {
        Point deviceUserPreferredResolution = this.mPersistentDataStore.getUserPreferredResolution(device);
        float deviceRefreshRate = this.mPersistentDataStore.getUserPreferredRefreshRate(device);
        if (!isValidResolution(deviceUserPreferredResolution) && !isValidRefreshRate(deviceRefreshRate)) {
            device.setUserPreferredDisplayModeLocked(mode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Display.Mode getUserPreferredDisplayModeInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            if (displayId == -1) {
                return this.mUserPreferredMode;
            }
            DisplayDevice displayDevice = getDeviceForDisplayLocked(displayId);
            if (displayDevice == null) {
                return null;
            }
            return displayDevice.getUserPreferredDisplayModeLocked();
        }
    }

    Display.Mode getSystemPreferredDisplayModeInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            DisplayDevice device = getDeviceForDisplayLocked(displayId);
            if (device == null) {
                return null;
            }
            return device.getSystemPreferredDisplayModeLocked();
        }
    }

    void setShouldAlwaysRespectAppRequestedModeInternal(boolean enabled) {
        this.mDisplayModeDirector.setShouldAlwaysRespectAppRequestedMode(enabled);
    }

    private void clearCallingPidForSetMode() {
        this.mCallingPidForSetMode = -1;
    }

    private void changeCallingPidForSetMode(int newPid) {
        this.mCallingPidForSetMode = newPid;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCallingPidForSetMode(int newPid, int forcedMode) {
        Slog.i("SwapDisplay", "dms--updateCallingPidForSetMode-newPid:" + newPid + "   /forcedMode:" + forcedMode);
        if (isFixedDisplayMode(forcedMode)) {
            changeCallingPidForSetMode(newPid);
        } else {
            clearCallingPidForSetMode();
        }
    }

    private boolean isFixedDisplayMode(int forcedMode) {
        return forcedMode == 1 || forcedMode == 2;
    }

    private void resetDisplayToFollowSystemLocked() {
        clearCallingPidForSetMode();
        setForcedUsingDisplayModeInternalLocked(0);
    }

    private int convertToDeviceStateFromForcedMode(int forcedMode) {
        switch (forcedMode) {
            case 1:
                return 2;
            case 2:
            case 3:
                return 0;
            default:
                return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setForcedUsingDisplayModeInternalLocked(int forcedMode) {
        updateForceMode(forcedMode);
        Slog.i("SwapDisplay", "dms-setForcedUsingDisplayModeLockedInternal:" + forcedMode);
        updateDeviceStateLocked(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceMode(int forcedMode) {
        this.mCurrentForcedMode = forcedMode;
        CameraServiceProxy serviceProxy = (CameraServiceProxy) LocalServices.getService(CameraServiceProxy.class);
        if (serviceProxy != null) {
            serviceProxy.onForceModeChanged(forcedMode);
        }
    }

    private void onBootCompleted() {
        synchronized (this.mSyncRoot) {
            this.mIsBootCompleted = true;
            if (this.mDeviceStateFromFold != -1) {
                updateDeviceStateLocked(false);
            }
        }
    }

    boolean shouldAlwaysRespectAppRequestedModeInternal() {
        return this.mDisplayModeDirector.shouldAlwaysRespectAppRequestedMode();
    }

    void setRefreshRateSwitchingTypeInternal(int newValue) {
        this.mDisplayModeDirector.setModeSwitchingType(newValue);
    }

    int getRefreshRateSwitchingTypeInternal() {
        return this.mDisplayModeDirector.getModeSwitchingType();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayDecorationSupport getDisplayDecorationSupportInternal(int displayId) {
        IBinder displayToken = getDisplayToken(displayId);
        if (displayToken == null) {
            return null;
        }
        return SurfaceControl.getDisplayDecorationSupport(displayToken);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBrightnessConfigurationForDisplayInternal(BrightnessConfiguration c, String uniqueId, int userId, String packageName) {
        validateBrightnessConfiguration(c);
        int userSerial = getUserManager().getUserSerialNumber(userId);
        synchronized (this.mSyncRoot) {
            DisplayDevice displayDevice = this.mDisplayDeviceRepo.getByUniqueIdLocked(uniqueId);
            if (displayDevice != null) {
                this.mPersistentDataStore.setBrightnessConfigurationForDisplayLocked(c, displayDevice, userSerial, packageName);
                this.mPersistentDataStore.saveIfNeeded();
                if (userId != this.mCurrentUserId) {
                    return;
                }
                DisplayPowerController dpc = getDpcFromUniqueIdLocked(uniqueId);
                if (dpc != null) {
                    dpc.setBrightnessConfiguration(c);
                }
                return;
            }
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayPowerController getDpcFromUniqueIdLocked(String uniqueId) {
        DisplayDevice displayDevice = this.mDisplayDeviceRepo.getByUniqueIdLocked(uniqueId);
        LogicalDisplay logicalDisplay = this.mLogicalDisplayMapper.getDisplayLocked(displayDevice);
        if (logicalDisplay != null) {
            int displayId = logicalDisplay.getDisplayIdLocked();
            return this.mDisplayPowerControllers.get(displayId);
        }
        return null;
    }

    void validateBrightnessConfiguration(BrightnessConfiguration config) {
        if (config != null && isBrightnessConfigurationTooDark(config)) {
            throw new IllegalArgumentException("brightness curve is too dark");
        }
    }

    private boolean isBrightnessConfigurationTooDark(BrightnessConfiguration config) {
        Pair<float[], float[]> curve = config.getCurve();
        float[] lux = (float[]) curve.first;
        float[] nits = (float[]) curve.second;
        for (int i = 0; i < lux.length; i++) {
            if (nits[i] < this.mMinimumBrightnessSpline.interpolate(lux[i])) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadBrightnessConfigurations() {
        final int userSerial = getUserManager().getUserSerialNumber(this.mContext.getUserId());
        synchronized (this.mSyncRoot) {
            this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayManagerService.this.m3330x9659fd81(userSerial, (LogicalDisplay) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$loadBrightnessConfigurations$5$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3330x9659fd81(int userSerial, LogicalDisplay logicalDisplay) {
        DisplayPowerController dpc;
        String uniqueId = logicalDisplay.getPrimaryDisplayDeviceLocked().getUniqueId();
        BrightnessConfiguration config = getBrightnessConfigForDisplayWithPdsFallbackLocked(uniqueId, userSerial);
        if (config != null && (dpc = this.mDisplayPowerControllers.get(logicalDisplay.getDisplayIdLocked())) != null) {
            dpc.setBrightnessConfiguration(config);
        }
    }

    private void performTraversalLocked(final SurfaceControl.Transaction t) {
        clearViewportsLocked();
        this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayManagerService.this.m3332x1535ab2d(t, (LogicalDisplay) obj);
            }
        });
        if (this.mInputManagerInternal != null) {
            this.mHandler.sendEmptyMessage(5);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performTraversalLocked$6$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3332x1535ab2d(SurfaceControl.Transaction t, LogicalDisplay display) {
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        if (device != null) {
            configureDisplayLocked(t, device);
            device.performTraversalLocked(t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayPropertiesInternal(int displayId, boolean hasContent, float requestedRefreshRate, int requestedModeId, float requestedMinRefreshRate, float requestedMaxRefreshRate, boolean preferMinimalPostProcessing, boolean inTraversal) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display == null) {
                return;
            }
            boolean shouldScheduleTraversal = false;
            if (display.hasContentLocked() != hasContent) {
                if (DEBUG) {
                    Slog.d(TAG, "Display " + displayId + " hasContent flag changed: hasContent=" + hasContent + ", inTraversal=" + inTraversal);
                }
                display.setHasContentLocked(hasContent);
                shouldScheduleTraversal = true;
            }
            if (requestedModeId == 0 && requestedRefreshRate != 0.0f) {
                Display.Mode mode = display.getDisplayInfoLocked().findDefaultModeByRefreshRate(requestedRefreshRate);
                if (mode != null) {
                    requestedModeId = mode.getModeId();
                } else {
                    Slog.e(TAG, "Couldn't find a mode for the requestedRefreshRate: " + requestedRefreshRate + " on Display: " + displayId);
                }
            }
            this.mDisplayModeDirector.getAppRequestObserver().setAppRequest(displayId, requestedModeId, requestedMinRefreshRate, requestedMaxRefreshRate);
            boolean mppRequest = this.mMinimalPostProcessingAllowed && preferMinimalPostProcessing;
            if (display.getRequestedMinimalPostProcessingLocked() != mppRequest) {
                display.setRequestedMinimalPostProcessingLocked(mppRequest);
                shouldScheduleTraversal = true;
            }
            if (shouldScheduleTraversal) {
                scheduleTraversalLocked(inTraversal);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayOffsetsInternal(int displayId, int x, int y) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display == null) {
                return;
            }
            if (display.getDisplayOffsetXLocked() != x || display.getDisplayOffsetYLocked() != y) {
                if (DEBUG) {
                    Slog.d(TAG, "Display " + displayId + " burn-in offset set to (" + x + ", " + y + ")");
                }
                display.setDisplayOffsetsLocked(x, y);
                scheduleTraversalLocked(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayScalingDisabledInternal(int displayId, boolean disable) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display == null) {
                return;
            }
            if (display.isDisplayScalingDisabled() != disable) {
                if (DEBUG) {
                    Slog.d(TAG, "Display " + displayId + " content scaling disabled = " + disable);
                }
                display.setDisplayScalingDisabledLocked(disable);
                scheduleTraversalLocked(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayAccessUIDsInternal(SparseArray<IntArray> newDisplayAccessUIDs) {
        synchronized (this.mSyncRoot) {
            this.mDisplayAccessUIDs.clear();
            for (int i = newDisplayAccessUIDs.size() - 1; i >= 0; i--) {
                this.mDisplayAccessUIDs.append(newDisplayAccessUIDs.keyAt(i), newDisplayAccessUIDs.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUidPresentOnDisplayInternal(int uid, int displayId) {
        boolean z;
        synchronized (this.mSyncRoot) {
            IntArray displayUIDs = this.mDisplayAccessUIDs.get(displayId);
            z = (displayUIDs == null || displayUIDs.indexOf(uid) == -1) ? false : true;
        }
        return z;
    }

    private IBinder getDisplayToken(int displayId) {
        DisplayDevice device;
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null && (device = display.getPrimaryDisplayDeviceLocked()) != null) {
                return device.getDisplayTokenLocked();
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SurfaceControl.ScreenshotHardwareBuffer systemScreenshotInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            IBinder token = getDisplayToken(displayId);
            if (token == null) {
                return null;
            }
            LogicalDisplay logicalDisplay = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (logicalDisplay == null) {
                return null;
            }
            DisplayInfo displayInfo = logicalDisplay.getDisplayInfoLocked();
            SurfaceControl.DisplayCaptureArgs captureArgs = new SurfaceControl.DisplayCaptureArgs.Builder(token).setSize(displayInfo.getNaturalWidth(), displayInfo.getNaturalHeight()).setUseIdentityTransform(true).setCaptureSecureLayers(true).setAllowProtected(true).build();
            return SurfaceControl.captureDisplay(captureArgs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SurfaceControl.ScreenshotHardwareBuffer userScreenshotInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            IBinder token = getDisplayToken(displayId);
            if (token == null) {
                return null;
            }
            SurfaceControl.DisplayCaptureArgs captureArgs = new SurfaceControl.DisplayCaptureArgs.Builder(token).build();
            return SurfaceControl.captureDisplay(captureArgs);
        }
    }

    DisplayedContentSamplingAttributes getDisplayedContentSamplingAttributesInternal(int displayId) {
        IBinder token = getDisplayToken(displayId);
        if (token == null) {
            return null;
        }
        return SurfaceControl.getDisplayedContentSamplingAttributes(token);
    }

    boolean setDisplayedContentSamplingEnabledInternal(int displayId, boolean enable, int componentMask, int maxFrames) {
        IBinder token = getDisplayToken(displayId);
        if (token == null) {
            return false;
        }
        return SurfaceControl.setDisplayedContentSamplingEnabled(token, enable, componentMask, maxFrames);
    }

    DisplayedContentSample getDisplayedContentSampleInternal(int displayId, long maxFrames, long timestamp) {
        IBinder token = getDisplayToken(displayId);
        if (token == null) {
            return null;
        }
        return SurfaceControl.getDisplayedContentSample(token, maxFrames, timestamp);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetBrightnessConfigurations() {
        synchronized (this.mSyncRoot) {
            this.mPersistentDataStore.setBrightnessConfigurationForUser(null, this.mContext.getUserId(), this.mContext.getPackageName());
            this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda10
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayManagerService.this.m3333xe08dafd0((LogicalDisplay) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetBrightnessConfigurations$7$com-android-server-display-DisplayManagerService  reason: not valid java name */
    public /* synthetic */ void m3333xe08dafd0(LogicalDisplay logicalDisplay) {
        if (logicalDisplay.getDisplayInfoLocked().type != 1) {
            return;
        }
        String uniqueId = logicalDisplay.getPrimaryDisplayDeviceLocked().getUniqueId();
        setBrightnessConfigurationForDisplayInternal(null, uniqueId, this.mContext.getUserId(), this.mContext.getPackageName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAutoBrightnessLoggingEnabled(boolean enabled) {
        synchronized (this.mSyncRoot) {
            DisplayPowerController displayPowerController = this.mDisplayPowerControllers.get(0);
            if (displayPowerController != null) {
                displayPowerController.setAutoBrightnessLoggingEnabled(enabled);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayWhiteBalanceLoggingEnabled(boolean enabled) {
        synchronized (this.mSyncRoot) {
            DisplayPowerController displayPowerController = this.mDisplayPowerControllers.get(0);
            if (displayPowerController != null) {
                displayPowerController.setDisplayWhiteBalanceLoggingEnabled(enabled);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayModeDirectorLoggingEnabled(boolean enabled) {
        synchronized (this.mSyncRoot) {
            DisplayModeDirector displayModeDirector = this.mDisplayModeDirector;
            if (displayModeDirector != null) {
                displayModeDirector.setLoggingEnabled(enabled);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Display.Mode getActiveDisplayModeAtStart(int displayId) {
        synchronized (this.mSyncRoot) {
            DisplayDevice device = getDeviceForDisplayLocked(displayId);
            if (device == null) {
                return null;
            }
            return device.getActiveDisplayModeAtStartLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAmbientColorTemperatureOverride(float cct) {
        synchronized (this.mSyncRoot) {
            DisplayPowerController displayPowerController = this.mDisplayPowerControllers.get(0);
            if (displayPowerController != null) {
                displayPowerController.setAmbientColorTemperatureOverride(cct);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDockedAndIdleEnabled(boolean enabled, int displayId) {
        synchronized (this.mSyncRoot) {
            DisplayPowerController displayPowerController = this.mDisplayPowerControllers.get(displayId);
            if (displayPowerController != null) {
                displayPowerController.setAutomaticScreenBrightnessMode(enabled);
            }
        }
    }

    private void clearViewportsLocked() {
        this.mViewports.clear();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Optional<Integer> getViewportType(DisplayDeviceInfo info) {
        switch (info.touch) {
            case 1:
                return Optional.of(1);
            case 2:
                return Optional.of(2);
            case 3:
                if (!TextUtils.isEmpty(info.uniqueId)) {
                    return Optional.of(3);
                }
                break;
        }
        Slog.w(TAG, "Display " + info + " does not support input device matching.");
        return Optional.empty();
    }

    private void configureDisplayLocked(SurfaceControl.Transaction t, DisplayDevice device) {
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        boolean z = false;
        boolean ownContent = (info.flags & 128) != 0;
        LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(device);
        if (!ownContent && !device.isWindowManagerMirroringLocked()) {
            if (display != null && !display.hasContentLocked() && !display.isDualDisplay() && (!mMultipleDisplayFlipSupport || display.getDisplayIdLocked() != 1)) {
                display = this.mLogicalDisplayMapper.getDisplayLocked(device.getDisplayIdToMirrorLocked());
            }
            if (display == null) {
                display = this.mLogicalDisplayMapper.getDisplayLocked(0);
            }
        }
        if (display == null) {
            Slog.w(TAG, "Missing logical display to use for physical display device: " + device.getDisplayDeviceInfoLocked());
            return;
        }
        boolean z2 = info.state == 1;
        if (!mDualBootAnimSupport || this.mIsBootCompleted) {
            z = true;
        }
        display.configureDisplayLocked(t, device, z2, z);
        Optional<Integer> viewportType = getViewportType(info);
        if (viewportType.isPresent()) {
            populateViewportLocked(viewportType.get().intValue(), display.getDisplayIdLocked(), device, info);
        }
    }

    private DisplayViewport getViewportLocked(int viewportType, String uniqueId) {
        if (viewportType != 1 && viewportType != 2 && viewportType != 3) {
            Slog.wtf(TAG, "Cannot call getViewportByTypeLocked for type " + DisplayViewport.typeToString(viewportType));
            return null;
        }
        int count = this.mViewports.size();
        for (int i = 0; i < count; i++) {
            DisplayViewport viewport = this.mViewports.get(i);
            if (viewport.type == viewportType && uniqueId.equals(viewport.uniqueId)) {
                return viewport;
            }
        }
        DisplayViewport viewport2 = new DisplayViewport();
        viewport2.type = viewportType;
        viewport2.uniqueId = uniqueId;
        this.mViewports.add(viewport2);
        return viewport2;
    }

    private void populateViewportLocked(int viewportType, int displayId, DisplayDevice device, DisplayDeviceInfo info) {
        DisplayViewport viewport = getViewportLocked(viewportType, info.uniqueId);
        device.populateViewportLocked(viewport);
        viewport.valid = true;
        viewport.displayId = displayId;
        viewport.isActive = Display.isActiveState(info.state);
    }

    private void updateViewportPowerStateLocked(LogicalDisplay display) {
        DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        Optional<Integer> viewportType = getViewportType(info);
        if (viewportType.isPresent()) {
            Iterator<DisplayViewport> it = this.mViewports.iterator();
            while (it.hasNext()) {
                DisplayViewport d = it.next();
                if (d.type == viewportType.get().intValue() && info.uniqueId.equals(d.uniqueId)) {
                    d.isActive = Display.isActiveState(info.state);
                }
            }
            if (this.mInputManagerInternal != null) {
                this.mHandler.sendEmptyMessage(5);
            }
        }
    }

    private void sendDisplayEventLocked(int displayId, int event) {
        Message msg = this.mHandler.obtainMessage(3, displayId, event);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendDisplayGroupEvent(int groupId, int event) {
        Message msg = this.mHandler.obtainMessage(8, groupId, event);
        this.mHandler.sendMessage(msg);
    }

    private void sendDisplayEventFrameRateOverrideLocked(int displayId) {
        Message msg = this.mHandler.obtainMessage(7, displayId, 2);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleTraversalLocked(boolean inTraversal) {
        if (!this.mPendingTraversal && this.mWindowManagerInternal != null) {
            this.mPendingTraversal = true;
            if (!inTraversal) {
                this.mHandler.sendEmptyMessage(4);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverDisplayEvent(int displayId, ArraySet<Integer> uids, int event) {
        if (DEBUG) {
            Slog.d(TAG, "Delivering display event: displayId=" + displayId + ", event=" + event);
        }
        synchronized (this.mSyncRoot) {
            int count = this.mCallbacks.size();
            this.mTempCallbacks.clear();
            for (int i = 0; i < count; i++) {
                if (uids == null || uids.contains(Integer.valueOf(this.mCallbacks.valueAt(i).mUid))) {
                    this.mTempCallbacks.add(this.mCallbacks.valueAt(i));
                }
            }
        }
        for (int i2 = 0; i2 < this.mTempCallbacks.size(); i2++) {
            this.mTempCallbacks.get(i2).notifyDisplayEventAsync(displayId, event);
        }
        this.mTempCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverDisplayGroupEvent(int groupId, int event) {
        if (DEBUG) {
            Slog.d(TAG, "Delivering display group event: groupId=" + groupId + ", event=" + event);
        }
        switch (event) {
            case 1:
                Iterator<DisplayManagerInternal.DisplayGroupListener> it = this.mDisplayGroupListeners.iterator();
                while (it.hasNext()) {
                    DisplayManagerInternal.DisplayGroupListener listener = it.next();
                    listener.onDisplayGroupAdded(groupId);
                }
                return;
            case 2:
                Iterator<DisplayManagerInternal.DisplayGroupListener> it2 = this.mDisplayGroupListeners.iterator();
                while (it2.hasNext()) {
                    DisplayManagerInternal.DisplayGroupListener listener2 = it2.next();
                    listener2.onDisplayGroupChanged(groupId);
                }
                return;
            case 3:
                Iterator<DisplayManagerInternal.DisplayGroupListener> it3 = this.mDisplayGroupListeners.iterator();
                while (it3.hasNext()) {
                    DisplayManagerInternal.DisplayGroupListener listener3 = it3.next();
                    listener3.onDisplayGroupRemoved(groupId);
                }
                return;
            default:
                return;
        }
    }

    private IMediaProjectionManager getProjectionService() {
        if (this.mProjectionService == null) {
            IBinder b = ServiceManager.getService("media_projection");
            this.mProjectionService = IMediaProjectionManager.Stub.asInterface(b);
        }
        return this.mProjectionService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserManager getUserManager() {
        return (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(final PrintWriter pw) {
        int[] iArr;
        pw.println("DISPLAY MANAGER (dumpsys display)");
        synchronized (this.mSyncRoot) {
            pw.println("  mOnlyCode=" + this.mOnlyCore);
            pw.println("  mSafeMode=" + this.mSafeMode);
            pw.println("  mPendingTraversal=" + this.mPendingTraversal);
            pw.println("  mViewports=" + this.mViewports);
            pw.println("  mDefaultDisplayDefaultColorMode=" + this.mDefaultDisplayDefaultColorMode);
            pw.println("  mWifiDisplayScanRequestCount=" + this.mWifiDisplayScanRequestCount);
            pw.println("  mStableDisplaySize=" + this.mStableDisplaySize);
            pw.println("  mMinimumBrightnessCurve=" + this.mMinimumBrightnessCurve);
            if (this.mUserPreferredMode != null) {
                pw.println(" mUserPreferredMode=" + this.mUserPreferredMode);
            }
            pw.println();
            if (!this.mAreUserDisabledHdrTypesAllowed) {
                pw.println("  mUserDisabledHdrTypes: size=" + this.mUserDisabledHdrTypes.length);
                for (int type : this.mUserDisabledHdrTypes) {
                    pw.println("  " + type);
                }
            }
            pw.println();
            int displayStateCount = this.mDisplayStates.size();
            pw.println("Display States: size=" + displayStateCount);
            for (int i = 0; i < displayStateCount; i++) {
                int displayId = this.mDisplayStates.keyAt(i);
                int displayState = this.mDisplayStates.valueAt(i);
                BrightnessPair brightnessPair = this.mDisplayBrightnesses.valueAt(i);
                pw.println("  Display Id=" + displayId);
                pw.println("  Display State=" + Display.stateToString(displayState));
                pw.println("  Display Brightness=" + brightnessPair.brightness);
                pw.println("  Display SdrBrightness=" + brightnessPair.sdrBrightness);
            }
            final PrintWriter indentingPrintWriter = new IndentingPrintWriter(pw, "    ");
            indentingPrintWriter.increaseIndent();
            pw.println();
            pw.println("Display Adapters: size=" + this.mDisplayAdapters.size());
            Iterator<DisplayAdapter> it = this.mDisplayAdapters.iterator();
            while (it.hasNext()) {
                DisplayAdapter adapter = it.next();
                pw.println("  " + adapter.getName());
                adapter.dumpLocked(indentingPrintWriter);
            }
            pw.println();
            pw.println("Display Devices: size=" + this.mDisplayDeviceRepo.sizeLocked());
            this.mDisplayDeviceRepo.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayManagerService.lambda$dumpInternal$8(pw, indentingPrintWriter, (DisplayDevice) obj);
                }
            });
            pw.println();
            this.mLogicalDisplayMapper.dumpLocked(pw);
            int callbackCount = this.mCallbacks.size();
            pw.println();
            pw.println("Callbacks: size=" + callbackCount);
            for (int i2 = 0; i2 < callbackCount; i2++) {
                CallbackRecord callback = this.mCallbacks.valueAt(i2);
                pw.println("  " + i2 + ": mPid=" + callback.mPid + ", mWifiDisplayScanRequested=" + callback.mWifiDisplayScanRequested);
            }
            int callbackCount2 = this.mDualCallbacks.size();
            pw.println();
            pw.println("DualCallbacks: size=" + callbackCount2);
            for (int i3 = 0; i3 < callbackCount2; i3++) {
                DualCallbackRecord callback2 = this.mDualCallbacks.valueAt(i3);
                pw.println("  " + i3 + ": mPid=" + callback2.mPid + ", mReallyOpenDualDisplay=" + callback2.mReallyOpenDualDisplay);
            }
            int displayPowerControllerCount = this.mDisplayPowerControllers.size();
            pw.println();
            pw.println("Display Power Controllers: size=" + displayPowerControllerCount);
            for (int i4 = 0; i4 < displayPowerControllerCount; i4++) {
                this.mDisplayPowerControllers.valueAt(i4).dump(pw);
            }
            if (this.mBrightnessTracker != null) {
                pw.println();
                this.mBrightnessTracker.dump(pw);
            }
            pw.println();
            this.mPersistentDataStore.dump(pw);
            int displayWindowPolicyControllerCount = this.mDisplayWindowPolicyControllers.size();
            pw.println();
            pw.println("Display Window Policy Controllers: size=" + displayWindowPolicyControllerCount);
            for (int i5 = 0; i5 < displayWindowPolicyControllerCount; i5++) {
                pw.print("Display " + this.mDisplayWindowPolicyControllers.keyAt(i5) + ":");
                ((DisplayWindowPolicyController) this.mDisplayWindowPolicyControllers.valueAt(i5).second).dump("  ", pw);
            }
        }
        pw.println();
        this.mDisplayModeDirector.dump(pw);
        this.mBrightnessSynchronizer.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpInternal$8(PrintWriter pw, IndentingPrintWriter ipw, DisplayDevice device) {
        pw.println("  " + device.getDisplayDeviceInfoLocked());
        device.dumpLocked(ipw);
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }

    private static boolean isResolutionAndRefreshRateValid(Display.Mode mode) {
        return mode.getPhysicalWidth() > 0 && mode.getPhysicalHeight() > 0 && mode.getRefreshRate() > 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        VirtualDisplayAdapter getVirtualDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener displayAdapterListener) {
            return new VirtualDisplayAdapter(syncRoot, context, handler, displayAdapterListener);
        }

        long getDefaultDisplayDelayTimeout() {
            return 10000L;
        }

        boolean getAllowNonNativeRefreshRateOverride() {
            return ((Boolean) DisplayProperties.debug_allow_non_native_refresh_rate_override().orElse(true)).booleanValue();
        }
    }

    DisplayDeviceInfo getDisplayDeviceInfoInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null) {
                DisplayDevice displayDevice = display.getPrimaryDisplayDeviceLocked();
                return displayDevice.getDisplayDeviceInfoLocked();
            }
            return null;
        }
    }

    int getDisplayIdToMirrorInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display != null) {
                DisplayDevice displayDevice = display.getPrimaryDisplayDeviceLocked();
                return displayDevice.getDisplayIdToMirrorLocked();
            }
            return -1;
        }
    }

    Surface getVirtualDisplaySurfaceInternal(IBinder appToken) {
        synchronized (this.mSyncRoot) {
            VirtualDisplayAdapter virtualDisplayAdapter = this.mVirtualDisplayAdapter;
            if (virtualDisplayAdapter == null) {
                return null;
            }
            return virtualDisplayAdapter.getVirtualDisplaySurfaceLocked(appToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initializeDisplayPowerControllersLocked() {
        this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayManagerService.this.addDisplayPowerControllerLocked((LogicalDisplay) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addDisplayPowerControllerLocked(final LogicalDisplay display) {
        if (this.mPowerHandler == null) {
            return;
        }
        if (this.mBrightnessTracker == null) {
            this.mBrightnessTracker = new BrightnessTracker(this.mContext, null);
        }
        BrightnessSetting brightnessSetting = new BrightnessSetting(this.mPersistentDataStore, display, this.mSyncRoot);
        DisplayPowerController displayPowerController = new DisplayPowerController(this.mContext, this.mDisplayPowerCallbacks, this.mPowerHandler, this.mSensorManager, this.mDisplayBlanker, display, this.mBrightnessTracker, brightnessSetting, new Runnable() { // from class: com.android.server.display.DisplayManagerService$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                DisplayManagerService.this.m3328x1f080281(display);
            }
        });
        this.mDisplayPowerControllers.append(display.getDisplayIdLocked(), displayPowerController);
        IDisplayManagerServiceLice.Instance().onAddDisplayPowerControllerLocked(display, displayPowerController);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleBrightnessChange */
    public void m3328x1f080281(LogicalDisplay display) {
        sendDisplayEventLocked(display.getDisplayIdLocked(), 4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayDevice getDeviceForDisplayLocked(int displayId) {
        LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
        if (display == null) {
            return null;
        }
        return display.getPrimaryDisplayDeviceLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public BrightnessConfiguration getBrightnessConfigForDisplayWithPdsFallbackLocked(String uniqueId, int userSerial) {
        BrightnessConfiguration config = this.mPersistentDataStore.getBrightnessConfigurationForDisplayLocked(uniqueId, userSerial);
        if (config == null) {
            return this.mPersistentDataStore.getBrightnessConfiguration(userSerial);
        }
        return config;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayManagerHandler extends Handler {
        public DisplayManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean changed;
            switch (msg.what) {
                case 1:
                    DisplayManagerService.this.registerDefaultDisplayAdapters();
                    return;
                case 2:
                    DisplayManagerService.this.registerAdditionalDisplayAdapters();
                    return;
                case 3:
                    DisplayManagerService.this.deliverDisplayEvent(msg.arg1, null, msg.arg2);
                    return;
                case 4:
                    DisplayManagerService.this.mWindowManagerInternal.requestTraversalFromDisplayManager();
                    return;
                case 5:
                    synchronized (DisplayManagerService.this.mSyncRoot) {
                        changed = !DisplayManagerService.this.mTempViewports.equals(DisplayManagerService.this.mViewports);
                        if (changed) {
                            DisplayManagerService.this.mTempViewports.clear();
                            Iterator it = DisplayManagerService.this.mViewports.iterator();
                            while (it.hasNext()) {
                                DisplayViewport d = (DisplayViewport) it.next();
                                DisplayManagerService.this.mTempViewports.add(d.makeCopy());
                            }
                        }
                    }
                    if (changed) {
                        DisplayManagerService.this.mInputManagerInternal.setDisplayViewports(DisplayManagerService.this.mTempViewports);
                        return;
                    }
                    return;
                case 6:
                    DisplayManagerService.this.loadBrightnessConfigurations();
                    return;
                case 7:
                    synchronized (DisplayManagerService.this.mSyncRoot) {
                        int displayId = msg.arg1;
                        LogicalDisplay display = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(displayId);
                        if (display != null) {
                            ArraySet<Integer> uids = display.getPendingFrameRateOverrideUids();
                            display.clearPendingFrameRateOverrideUids();
                            DisplayManagerService.this.deliverDisplayEvent(msg.arg1, uids, msg.arg2);
                        }
                    }
                    return;
                case 8:
                    DisplayManagerService.this.deliverDisplayGroupEvent(msg.arg1, msg.arg2);
                    return;
                case 111:
                    DisplayManagerService.this.deliverDualDisplayEvent(msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class LogicalDisplayListener implements LogicalDisplayMapper.Listener {
        private LogicalDisplayListener() {
        }

        @Override // com.android.server.display.LogicalDisplayMapper.Listener
        public void onLogicalDisplayEventLocked(LogicalDisplay display, int event) {
            switch (event) {
                case 1:
                    DisplayManagerService.this.handleLogicalDisplayAddedLocked(display);
                    return;
                case 2:
                    DisplayManagerService.this.handleLogicalDisplayChangedLocked(display);
                    return;
                case 3:
                    DisplayManagerService.this.handleLogicalDisplayRemovedLocked(display);
                    return;
                case 4:
                    DisplayManagerService.this.handleLogicalDisplaySwappedLocked(display);
                    return;
                case 5:
                    DisplayManagerService.this.handleLogicalDisplayFrameRateOverridesChangedLocked(display);
                    return;
                case 6:
                    DisplayManagerService.this.handleLogicalDisplayDeviceStateTransitionLocked(display);
                    return;
                case 60:
                    DisplayManagerService.this.hanldeLogicalDisplayUpdatePowerStateLocked(display);
                    return;
                case 61:
                    DisplayManagerService.this.hanldeLogicalDisplayDualDisplayOpenedLocked(display);
                    return;
                case 62:
                    DisplayManagerService.this.hanldeLogicalDisplayDualDisplayClosedLocked(display);
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.display.LogicalDisplayMapper.Listener
        public void onDisplayGroupEventLocked(int groupId, int event) {
            DisplayManagerService.this.sendDisplayGroupEvent(groupId, event);
        }

        @Override // com.android.server.display.LogicalDisplayMapper.Listener
        public void onTraversalRequested() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.scheduleTraversalLocked(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CallbackRecord implements IBinder.DeathRecipient {
        private final IDisplayManagerCallback mCallback;
        private AtomicLong mEventsMask;
        public final int mPid;
        public final int mUid;
        public boolean mWifiDisplayScanRequested;

        CallbackRecord(int pid, int uid, IDisplayManagerCallback callback, long eventsMask) {
            this.mPid = pid;
            this.mUid = uid;
            this.mCallback = callback;
            this.mEventsMask = new AtomicLong(eventsMask);
        }

        public void updateEventsMask(long eventsMask) {
            this.mEventsMask.set(eventsMask);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (DisplayManagerService.DEBUG) {
                Slog.d(DisplayManagerService.TAG, "Display listener for pid " + this.mPid + " died.");
            }
            DisplayManagerService.this.onCallbackDied(this);
        }

        public void notifyDisplayEventAsync(int displayId, int event) {
            if (!shouldSendEvent(event)) {
                return;
            }
            try {
                this.mCallback.onDisplayEvent(displayId, event);
            } catch (RemoteException ex) {
                Slog.w(DisplayManagerService.TAG, "Failed to notify process " + this.mPid + " that displays changed, assuming it died.", ex);
                binderDied();
            }
        }

        private boolean shouldSendEvent(int event) {
            long mask = this.mEventsMask.get();
            switch (event) {
                case 1:
                    return (1 & mask) != 0;
                case 2:
                    return (4 & mask) != 0;
                case 3:
                    return (2 & mask) != 0;
                case 4:
                    return (8 & mask) != 0;
                default:
                    Slog.e(DisplayManagerService.TAG, "Unknown display event " + event);
                    return true;
            }
        }
    }

    private void sendDualDisplayEventLocked(int displayId, int event) {
        Slog.d(TAG, "sendDualDisplayEventLocked displayId: " + displayId + " event:" + event);
        Message msg = this.mHandler.obtainMessage(111, displayId, event);
        this.mHandler.sendMessage(msg);
    }

    private int getDualDeviceStateLocked() {
        return this.mLogicalDisplayMapper.getDualDeviceStateLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeviceStateLocked(boolean isOverrideActive) {
        synchronized (this.mSyncRoot) {
            int pendingState = this.mDeviceStateFromFold;
            if (!this.mIsBootCompleted) {
                Slog.d(TAG, "Still use dual display before bootcomplete");
                return;
            }
            int i = this.mCurrentForcedMode;
            if (i != 0) {
                pendingState = convertToDeviceStateFromForcedMode(i);
            }
            if (this.mRequestOpenDual) {
                if (pendingState != 0) {
                    pendingState = 99;
                } else {
                    this.mRequestOpenDual = false;
                }
            }
            Slog.i(TAG, "updateDeviceStateLocked pendingState: " + pendingState + " mDeviceStateRealUsed: " + this.mDeviceStateRealUsed);
            if (pendingState != -1 && pendingState != this.mDeviceStateRealUsed) {
                this.mDeviceStateRealUsed = pendingState;
                Slog.i(TAG, Log.getStackTraceString(new Throwable()));
                if (this.mDeviceStateRealUsed == 0) {
                    SystemProperties.set("persist.sys.display.status", "1");
                } else {
                    SystemProperties.set("persist.sys.display.status", "0");
                }
                this.mLogicalDisplayMapper.setDeviceStateLocked(this.mDeviceStateRealUsed, isOverrideActive);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverDualDisplayEvent(int displayId, int event) {
        if (DEBUG) {
            Slog.d(TAG, "Delivering display event: displayId=" + displayId + ", event=" + event);
        }
        synchronized (this.mSyncRoot) {
            this.mTempDualCallbacks.clear();
            for (int i = 0; i < this.mDualCallbacks.size(); i++) {
                this.mTempDualCallbacks.add(this.mDualCallbacks.valueAt(i));
            }
        }
        Slog.d(TAG, "mTempDualCallbacks size:" + this.mTempDualCallbacks.size());
        for (int i2 = 0; i2 < this.mTempDualCallbacks.size(); i2++) {
            this.mTempDualCallbacks.get(i2).notifyDisplayEventAsync(displayId, event);
        }
        this.mTempDualCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openDualDisplayInternal(int callingPid) {
        synchronized (this.mSyncRoot) {
            if (mDualDisplaySupport || mDualDisplayFlipSupport) {
                if (this.mDualCallbacks.get(callingPid) == null) {
                    Slog.d(TAG, "pid: " + callingPid + " must register before open dualdisplay");
                } else if (!this.mLogicalDisplayMapper.isDualDisplayEnabled()) {
                    int dualDeviceState = getDualDeviceStateLocked();
                    Slog.d(TAG, "openDualDisplay: DualDeviceState " + dualDeviceState);
                    if (dualDeviceState < 0) {
                        Slog.d(TAG, "openDualDisplay failed, because dualdisplay has been opened or closing!");
                        return;
                    }
                    this.mRequestOpenDual = true;
                    updateDeviceStateLocked(false);
                    this.mDualCallbacks.get(callingPid).mReallyOpenDualDisplay = true;
                } else {
                    Slog.d(TAG, "openDualDisplay: DualDisplay has been opened, stop this request!");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeDualDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (mDualDisplaySupport || mDualDisplayFlipSupport) {
                if (this.mLogicalDisplayMapper.isDualDisplayEnabled()) {
                    this.mRequestOpenDual = false;
                    updateDeviceStateLocked(false);
                } else {
                    Slog.d(TAG, "closeDualDisplay: DualDisplay has been closed, stop this request!");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDualCallbackInternal(IDisplayManagerCallback callback, int callingPid, int callingUid, long eventsMask) {
        synchronized (this.mSyncRoot) {
            Slog.d(TAG, "registerDualCallbackInternal from " + callingPid);
            DualCallbackRecord record = this.mDualCallbacks.get(callingPid);
            if (record != null) {
                record.updateEventsMask(eventsMask);
                return;
            }
            DualCallbackRecord record2 = new DualCallbackRecord(callingPid, callingUid, callback, eventsMask);
            try {
                IBinder binder = callback.asBinder();
                binder.linkToDeath(record2, 0);
                this.mDualCallbacks.put(callingPid, record2);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DualCallbackRecord implements IBinder.DeathRecipient {
        private final IDisplayManagerCallback mCallback;
        private AtomicLong mEventsMask;
        public final int mPid;
        public boolean mReallyOpenDualDisplay = false;
        public final int mUid;

        DualCallbackRecord(int pid, int uid, IDisplayManagerCallback callback, long eventsMask) {
            this.mPid = pid;
            this.mUid = uid;
            this.mCallback = callback;
            this.mEventsMask = new AtomicLong(eventsMask);
        }

        public void updateEventsMask(long eventsMask) {
            this.mEventsMask.set(eventsMask);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                Slog.d(DisplayManagerService.TAG, "Dual Display listener for pid " + this.mPid + " died. It open DualDisplay:" + this.mReallyOpenDualDisplay);
                DisplayManagerService.this.mDualCallbacks.remove(this.mPid);
                if (this.mReallyOpenDualDisplay) {
                    DisplayManagerService.this.closeDualDisplayInternal();
                }
            }
        }

        public void notifyDisplayEventAsync(int displayId, int event) {
            if (!shouldSendEvent(event)) {
                return;
            }
            try {
                this.mCallback.onDisplayEvent(displayId, event);
            } catch (RemoteException ex) {
                Slog.w(DisplayManagerService.TAG, "Failed to notify process " + this.mPid + " that displays changed, assuming it died.", ex);
                binderDied();
            }
        }

        private boolean shouldSendEvent(int event) {
            long mask = this.mEventsMask.get();
            switch (event) {
                case 1:
                    return (1 & mask) != 0;
                case 2:
                default:
                    Slog.e(DisplayManagerService.TAG, "Unknown display event " + event);
                    return true;
                case 3:
                    return (2 & mask) != 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class BinderService extends IDisplayManager.Stub {
        BinderService() {
        }

        public DisplayInfo getDisplayInfo(int displayId) {
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getDisplayInfoInternal(displayId, callingUid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void updateRefreshRateForScene(Bundle b) {
            long token = Binder.clearCallingIdentity();
            try {
                ITranDisplayManagerService.instance().updateRefreshRateForScene(DisplayManagerService.this.mWindowManagerInternal, b);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void updateRefreshRateForVideoScene(int videoState, int videoFps, int videoSessionId) {
            long token = Binder.clearCallingIdentity();
            try {
                ITranDisplayManagerService.instance().updateRefreshRateForVideoScene(DisplayManagerService.this.mWindowManagerInternal, videoState, videoFps, videoSessionId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int[] getDisplayIds(boolean includeDisabledDisplays) {
            int[] displayIdsLocked;
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    displayIdsLocked = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayIdsLocked(callingUid, includeDisabledDisplays);
                }
                return displayIdsLocked;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isUidPresentOnDisplay(int uid, int displayId) {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.isUidPresentOnDisplayInternal(uid, displayId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Point getStableDisplaySize() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getStableDisplaySizeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerCallback(IDisplayManagerCallback callback) {
            registerCallbackWithEventMask(callback, 23L);
        }

        public void registerCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) {
            if (callback == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.registerCallbackInternal(callback, callingPid, callingUid, eventsMask);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerDualCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) {
            if (callback == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.registerDualCallbackInternal(callback, callingPid, callingUid, eventsMask);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void openDualDisplay() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                int callingPid = Binder.getCallingPid();
                long token = Binder.clearCallingIdentity();
                DisplayManagerService.this.openDualDisplayInternal(callingPid);
                Binder.restoreCallingIdentity(token);
            }
        }

        public void closeDualDisplay() {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.closeDualDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addDualDisplayCompotent(String pkg, String title) {
            Slog.d(DisplayManagerService.TAG, "addDualDisplayCompotent: " + pkg + SliceClientPermissions.SliceAuthority.DELIMITER + title);
            synchronized (DisplayManagerService.this.mDualDisplayLock) {
                DisplayManagerService.this.mDualDisplayComponent.add(pkg + SliceClientPermissions.SliceAuthority.DELIMITER + title);
            }
        }

        public boolean isDualDisplayComponent(String pkg, String title) {
            boolean contains;
            synchronized (DisplayManagerService.this.mDualDisplayLock) {
                contains = DisplayManagerService.this.mDualDisplayComponent.contains(pkg + SliceClientPermissions.SliceAuthority.DELIMITER + title);
            }
            return contains;
        }

        public void startWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to start wifi display scans");
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.startWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void stopWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to stop wifi display scans");
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.stopWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void connectWifiDisplay(String address) {
            if (address == null) {
                throw new IllegalArgumentException("address must not be null");
            }
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to connect to a wifi display");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.connectWifiDisplayInternal(address);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void disconnectWifiDisplay() {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.disconnectWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void renameWifiDisplay(String address, String alias) {
            if (address == null) {
                throw new IllegalArgumentException("address must not be null");
            }
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to rename to a wifi display");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.renameWifiDisplayInternal(address, alias);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void forgetWifiDisplay(String address) {
            if (address == null) {
                throw new IllegalArgumentException("address must not be null");
            }
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to forget to a wifi display");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.forgetWifiDisplayInternal(address);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void pauseWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to pause a wifi display session");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.pauseWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void resumeWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to resume a wifi display session");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resumeWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public WifiDisplayStatus getWifiDisplayStatus() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getWifiDisplayStatusInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setUserDisabledHdrTypes(int[] userDisabledFormats) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", "Permission required to write the user settings.");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setUserDisabledHdrTypesInternal(userDisabledFormats);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setAreUserDisabledHdrTypesAllowed(boolean areUserDisabledHdrTypesAllowed) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", "Permission required to write the user settings.");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setAreUserDisabledHdrTypesAllowedInternal(areUserDisabledHdrTypesAllowed);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean areUserDisabledHdrTypesAllowed() {
            boolean z;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                z = DisplayManagerService.this.mAreUserDisabledHdrTypesAllowed;
            }
            return z;
        }

        public int[] getUserDisabledHdrTypes() {
            return DisplayManagerService.this.mUserDisabledHdrTypes;
        }

        public void requestColorMode(int displayId, int colorMode) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_COLOR_MODE", "Permission required to change the display color mode");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.requestColorModeInternal(displayId, colorMode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IMediaProjection projection, String packageName) {
            return DisplayManagerService.this.createVirtualDisplayInternal(virtualDisplayConfig, callback, projection, null, null, packageName);
        }

        public void resizeVirtualDisplay(IVirtualDisplayCallback callback, int width, int height, int densityDpi) {
            if (width <= 0 || height <= 0 || densityDpi <= 0) {
                throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
            }
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resizeVirtualDisplayInternal(callback.asBinder(), width, height, densityDpi);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setVirtualDisplaySurface(IVirtualDisplayCallback callback, Surface surface) {
            if (surface != null && surface.isSingleBuffered()) {
                throw new IllegalArgumentException("Surface can't be single-buffered");
            }
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setVirtualDisplaySurfaceInternal(callback.asBinder(), surface);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void releaseVirtualDisplay(IVirtualDisplayCallback callback) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.releaseVirtualDisplayInternal(callback.asBinder());
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setVirtualDisplayState(IVirtualDisplayCallback callback, boolean isOn) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setVirtualDisplayStateInternal(callback.asBinder(), isOn);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DisplayManagerService.this.mContext, DisplayManagerService.TAG, pw)) {
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.dumpInternal(pw);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(String callingPackage) {
            ParceledListSlice<BrightnessChangeEvent> brightnessEvents;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BRIGHTNESS_SLIDER_USAGE", "Permission to read brightness events.");
            int callingUid = Binder.getCallingUid();
            AppOpsManager appOpsManager = (AppOpsManager) DisplayManagerService.this.mContext.getSystemService(AppOpsManager.class);
            int mode = appOpsManager.noteOp(43, callingUid, callingPackage);
            boolean hasUsageStats = true;
            if (mode == 3) {
                if (DisplayManagerService.this.mContext.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") != 0) {
                    hasUsageStats = false;
                }
            } else if (mode != 0) {
                hasUsageStats = false;
            }
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    brightnessEvents = ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).getBrightnessEvents(userId, hasUsageStats);
                }
                return brightnessEvents;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
            ParceledListSlice<AmbientBrightnessDayStats> ambientBrightnessStats;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_LIGHT_STATS", "Permission required to to access ambient light stats.");
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    ambientBrightnessStats = ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).getAmbientBrightnessStats(userId);
                }
                return ambientBrightnessStats;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setBrightnessConfigurationForUser(final BrightnessConfiguration c, final int userId, final String packageName) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to change the display's brightness configuration");
            if (userId != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to change the display brightness configuration of another user");
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayManagerService.this.mLogicalDisplayMapper.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$BinderService$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            DisplayManagerService.BinderService.this.m3337x7b64e4aa(c, userId, packageName, (LogicalDisplay) obj);
                        }
                    });
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setBrightnessConfigurationForUser$0$com-android-server-display-DisplayManagerService$BinderService  reason: not valid java name */
        public /* synthetic */ void m3337x7b64e4aa(BrightnessConfiguration c, int userId, String packageName, LogicalDisplay logicalDisplay) {
            if (logicalDisplay.getDisplayInfoLocked().type != 1) {
                return;
            }
            DisplayDevice displayDevice = logicalDisplay.getPrimaryDisplayDeviceLocked();
            DisplayManagerService.this.setBrightnessConfigurationForDisplayInternal(c, displayDevice.getUniqueId(), userId, packageName);
        }

        public void setBrightnessConfigurationForDisplay(BrightnessConfiguration c, String uniqueId, int userId, String packageName) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to change the display's brightness configuration");
            if (userId != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to change the display brightness configuration of another user");
            }
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setBrightnessConfigurationForDisplayInternal(c, uniqueId, userId, packageName);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public BrightnessConfiguration getBrightnessConfigurationForDisplay(String uniqueId, int userId) {
            BrightnessConfiguration config;
            DisplayPowerController dpc;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's brightness configuration");
            if (userId != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to read the display brightness configuration of another user");
            }
            long token = Binder.clearCallingIdentity();
            int userSerial = DisplayManagerService.this.getUserManager().getUserSerialNumber(userId);
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    config = DisplayManagerService.this.getBrightnessConfigForDisplayWithPdsFallbackLocked(uniqueId, userSerial);
                    if (config == null && (dpc = DisplayManagerService.this.getDpcFromUniqueIdLocked(uniqueId)) != null) {
                        config = dpc.getDefaultBrightnessConfiguration();
                    }
                }
                return config;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public BrightnessConfiguration getBrightnessConfigurationForUser(int userId) {
            String uniqueId;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayDevice displayDevice = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(0).getPrimaryDisplayDeviceLocked();
                uniqueId = displayDevice.getUniqueId();
            }
            return getBrightnessConfigurationForDisplay(uniqueId, userId);
        }

        public BrightnessConfiguration getDefaultBrightnessConfiguration() {
            BrightnessConfiguration defaultBrightnessConfiguration;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's default brightness configuration");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    defaultBrightnessConfiguration = ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).getDefaultBrightnessConfiguration();
                }
                return defaultBrightnessConfiguration;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public BrightnessInfo getBrightnessInfo(int displayId) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to read the display's brightness info.");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayPowerController dpc = (DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(displayId);
                    if (dpc != null) {
                        return dpc.getBrightnessInfo();
                    }
                    Binder.restoreCallingIdentity(token);
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isMinimalPostProcessingRequested(int displayId) {
            boolean requestedMinimalPostProcessingLocked;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                requestedMinimalPostProcessingLocked = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(displayId).getRequestedMinimalPostProcessingLocked();
            }
            return requestedMinimalPostProcessingLocked;
        }

        public void setTemporaryBrightness(int displayId, float brightness) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's brightness");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(displayId)).setTemporaryBrightness(brightness);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setBrightness(int displayId, float brightness) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's brightness");
            if (!DisplayManagerService.isValidBrightness(brightness)) {
                Slog.w(DisplayManagerService.TAG, "Attempted to set invalid brightness" + brightness);
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayPowerController dpc = (DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(displayId);
                    if (dpc != null) {
                        dpc.setBrightness(brightness);
                    }
                    DisplayManagerService.this.mPersistentDataStore.saveIfNeeded();
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public float getBrightness(int displayId) {
            float brightness = Float.NaN;
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's brightness");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayPowerController dpc = (DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(displayId);
                    if (dpc != null) {
                        brightness = dpc.getScreenBrightnessSetting();
                    }
                }
                return brightness;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setTemporaryAutoBrightnessAdjustment(float adjustment) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's auto brightness adjustment");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).setTemporaryAutoBrightnessAdjustment(adjustment);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.display.DisplayManagerService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new DisplayManagerShellCommand(DisplayManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public Curve getMinimumBrightnessCurve() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getMinimumBrightnessCurveInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getPreferredWideGamutColorSpaceId() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getPreferredWideGamutColorSpaceIdInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setUserPreferredDisplayMode(int displayId, Display.Mode mode) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_USER_PREFERRED_DISPLAY_MODE", "Permission required to set the user preferred display mode.");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setUserPreferredDisplayModeInternal(displayId, mode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Display.Mode getUserPreferredDisplayMode(int displayId) {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getUserPreferredDisplayModeInternal(displayId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Display.Mode getSystemPreferredDisplayMode(int displayId) {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getSystemPreferredDisplayModeInternal(displayId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setShouldAlwaysRespectAppRequestedMode(boolean enabled) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.OVERRIDE_DISPLAY_MODE_REQUESTS", "Permission required to override display mode requests.");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setShouldAlwaysRespectAppRequestedModeInternal(enabled);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setForcedUsingDisplayMode(int forcedMode) {
            if (DisplayManagerService.this.mLogicalDisplayMapper == null) {
                Slog.i("SwapDisplay", "dms-setForcedUsingDisplayMode return due to mLogicalDisplayMapper is null");
                return;
            }
            LogicalDisplay display = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(0);
            if (display == null) {
                Slog.i("SwapDisplay", "dms-setForcedUsingDisplayMode return due to default-display is null");
            } else if (display.getPhase() == 0) {
                Slog.i("SwapDisplay", "dms-setForcedUsingDisplayMode return due to phase is transition");
            } else {
                int callingPid = Binder.getCallingPid();
                DisplayManagerService.this.updateCallingPidForSetMode(callingPid, forcedMode);
                long token = Binder.clearCallingIdentity();
                try {
                    synchronized (DisplayManagerService.this.mSyncRoot) {
                        DisplayManagerService.this.setForcedUsingDisplayModeInternalLocked(forcedMode);
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        public int getForcedUsingDisplayMode() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.mCurrentForcedMode;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean shouldAlwaysRespectAppRequestedMode() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.OVERRIDE_DISPLAY_MODE_REQUESTS", "Permission required to override display mode requests.");
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.shouldAlwaysRespectAppRequestedModeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setRefreshRateSwitchingType(int newValue) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_REFRESH_RATE_SWITCHING_TYPE", "Permission required to modify refresh rate switching type.");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setRefreshRateSwitchingTypeInternal(newValue);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getRefreshRateSwitchingType() {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getRefreshRateSwitchingTypeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public DisplayDecorationSupport getDisplayDecorationSupport(int displayId) {
            long token = Binder.clearCallingIdentity();
            try {
                return DisplayManagerService.this.getDisplayDecorationSupportInternal(displayId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidBrightness(float brightness) {
        return !Float.isNaN(brightness) && brightness >= 0.0f && brightness <= 1.0f;
    }

    private static boolean isValidResolution(Point resolution) {
        return resolution != null && resolution.x > 0 && resolution.y > 0;
    }

    private static boolean isValidRefreshRate(float refreshRate) {
        return !Float.isNaN(refreshRate) && refreshRate > 0.0f;
    }

    /* loaded from: classes.dex */
    final class LocalService extends DisplayManagerInternal {
        LocalService() {
        }

        public void initPowerManagement(DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.mDisplayPowerCallbacks = callbacks;
                DisplayManagerService.this.mSensorManager = sensorManager;
                DisplayManagerService.this.mPowerHandler = handler;
                DisplayManagerService.this.initializeDisplayPowerControllersLocked();
            }
            DisplayManagerService.this.mHandler.sendEmptyMessage(6);
        }

        public int createVirtualDisplay(VirtualDisplayConfig config, IVirtualDisplayCallback callback, IVirtualDevice virtualDevice, DisplayWindowPolicyController dwpc, String packageName) {
            return DisplayManagerService.this.createVirtualDisplayInternal(config, callback, null, virtualDevice, dwpc, packageName);
        }

        public boolean requestPowerState(int groupId, DisplayManagerInternal.DisplayPowerRequest request, boolean waitForNegativeProximity) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayGroup displayGroup = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayGroupLocked(groupId);
                if (displayGroup == null) {
                    return true;
                }
                if (request.policy == 0 && DisplayManagerService.this.mRequestOpenDual && DisplayManagerService.this.mLogicalDisplayMapper.isDualDisplayEnabled()) {
                    DisplayManagerService.this.closeDualDisplayInternal();
                }
                int size = displayGroup.getSizeLocked();
                boolean ready = true;
                for (int i = 0; i < size; i++) {
                    int id = displayGroup.getIdLocked(i);
                    DisplayDevice displayDevice = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(id).getPrimaryDisplayDeviceLocked();
                    int flags = displayDevice.getDisplayDeviceInfoLocked().flags;
                    if ((flags & 32) == 0) {
                        DisplayPowerController displayPowerController = (DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(id);
                        ready &= displayPowerController.requestPowerState(request, waitForNegativeProximity);
                    }
                }
                return ready;
            }
        }

        public boolean shouldAlwaysRespectAppRequestedMode() {
            return DisplayManagerService.this.mDisplayModeDirector.shouldAlwaysRespectAppRequestedMode();
        }

        public void preResumeLcm() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).preResumeLcm();
            }
        }

        public boolean isProximitySensorAvailable() {
            boolean isProximitySensorAvailable;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                isProximitySensorAvailable = ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).isProximitySensorAvailable();
            }
            return isProximitySensorAvailable;
        }

        public void registerDisplayGroupListener(DisplayManagerInternal.DisplayGroupListener listener) {
            DisplayManagerService.this.mDisplayGroupListeners.add(listener);
        }

        public void unregisterDisplayGroupListener(DisplayManagerInternal.DisplayGroupListener listener) {
            DisplayManagerService.this.mDisplayGroupListeners.remove(listener);
        }

        public SurfaceControl.ScreenshotHardwareBuffer systemScreenshot(int displayId) {
            return DisplayManagerService.this.systemScreenshotInternal(displayId);
        }

        public SurfaceControl.ScreenshotHardwareBuffer userScreenshot(int displayId) {
            return DisplayManagerService.this.userScreenshotInternal(displayId);
        }

        public DisplayInfo getDisplayInfo(int displayId) {
            return DisplayManagerService.this.getDisplayInfoInternal(displayId, Process.myUid());
        }

        public Set<DisplayInfo> getPossibleDisplayInfo(int displayId) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                int displayGroupId = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayGroupIdFromDisplayIdLocked(displayId);
                if (displayGroupId == -1) {
                    Slog.w(DisplayManagerService.TAG, "Can't get possible display info since display group for " + displayId + " does not exist");
                    return new ArraySet();
                }
                Set<DisplayInfo> possibleInfo = new ArraySet<>();
                DisplayGroup group = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayGroupLocked(displayGroupId);
                for (int i = 0; i < group.getSizeLocked(); i++) {
                    int id = group.getIdLocked(i);
                    LogicalDisplay logical = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(id);
                    if (logical == null) {
                        Slog.w(DisplayManagerService.TAG, "Can't get possible display info since logical display for display id " + id + " does not exist, as part of group " + displayGroupId);
                    } else {
                        possibleInfo.add(logical.getDisplayInfoLocked());
                    }
                }
                if (DisplayManagerService.this.mDeviceStateManager == null) {
                    Slog.w(DisplayManagerService.TAG, "Can't get supported states since DeviceStateManager not ready");
                } else {
                    int[] supportedStates = DisplayManagerService.this.mDeviceStateManager.getSupportedStateIdentifiers();
                    for (int state : supportedStates) {
                        possibleInfo.addAll(DisplayManagerService.this.mLogicalDisplayMapper.getDisplayInfoForStateLocked(state, displayId, displayGroupId));
                    }
                }
                return possibleInfo;
            }
        }

        public Point getDisplayPosition(int displayId) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                LogicalDisplay display = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(displayId);
                if (display != null) {
                    return display.getDisplayPosition();
                }
                return null;
            }
        }

        public void registerDisplayTransactionListener(DisplayManagerInternal.DisplayTransactionListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            DisplayManagerService.this.registerDisplayTransactionListenerInternal(listener);
        }

        public void unregisterDisplayTransactionListener(DisplayManagerInternal.DisplayTransactionListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            DisplayManagerService.this.unregisterDisplayTransactionListenerInternal(listener);
        }

        public void setDisplayInfoOverrideFromWindowManager(int displayId, DisplayInfo info) {
            DisplayManagerService.this.setDisplayInfoOverrideFromWindowManagerInternal(displayId, info);
        }

        public void getNonOverrideDisplayInfo(int displayId, DisplayInfo outInfo) {
            DisplayManagerService.this.getNonOverrideDisplayInfoInternal(displayId, outInfo);
        }

        public void performTraversal(SurfaceControl.Transaction t) {
            DisplayManagerService.this.performTraversalInternal(t);
        }

        public void setDisplayProperties(int displayId, boolean hasContent, float requestedRefreshRate, int requestedMode, float requestedMinRefreshRate, float requestedMaxRefreshRate, boolean requestedMinimalPostProcessing, boolean inTraversal) {
            DisplayManagerService.this.setDisplayPropertiesInternal(displayId, hasContent, requestedRefreshRate, requestedMode, requestedMinRefreshRate, requestedMaxRefreshRate, requestedMinimalPostProcessing, inTraversal);
        }

        public void setMsyncFps(int displayId, float requestedMinRefreshRate, float requestedMaxRefreshRate) {
            DisplayManagerService.this.mDisplayModeDirector.getAppRequestObserver().setAppRequest(displayId, 0, requestedMinRefreshRate, requestedMaxRefreshRate);
        }

        public void setDisplayOffsets(int displayId, int x, int y) {
            DisplayManagerService.this.setDisplayOffsetsInternal(displayId, x, y);
        }

        public void setDisplayScalingDisabled(int displayId, boolean disableScaling) {
            DisplayManagerService.this.setDisplayScalingDisabledInternal(displayId, disableScaling);
        }

        public void setDisplayAccessUIDs(SparseArray<IntArray> newDisplayAccessUIDs) {
            DisplayManagerService.this.setDisplayAccessUIDsInternal(newDisplayAccessUIDs);
        }

        public void persistBrightnessTrackerState() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).persistBrightnessTrackerState();
            }
        }

        public void onOverlayChanged() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.mDisplayDeviceRepo.forEachLocked(new Consumer() { // from class: com.android.server.display.DisplayManagerService$LocalService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayDevice) obj).onOverlayChangedLocked();
                    }
                });
            }
        }

        public DisplayedContentSamplingAttributes getDisplayedContentSamplingAttributes(int displayId) {
            return DisplayManagerService.this.getDisplayedContentSamplingAttributesInternal(displayId);
        }

        public boolean setDisplayedContentSamplingEnabled(int displayId, boolean enable, int componentMask, int maxFrames) {
            return DisplayManagerService.this.setDisplayedContentSamplingEnabledInternal(displayId, enable, componentMask, maxFrames);
        }

        public DisplayedContentSample getDisplayedContentSample(int displayId, long maxFrames, long timestamp) {
            return DisplayManagerService.this.getDisplayedContentSampleInternal(displayId, maxFrames, timestamp);
        }

        public void ignoreProximitySensorUntilChanged() {
            ((DisplayPowerController) DisplayManagerService.this.mDisplayPowerControllers.get(0)).ignoreProximitySensorUntilChanged();
        }

        public int getRefreshRateSwitchingType() {
            return DisplayManagerService.this.getRefreshRateSwitchingTypeInternal();
        }

        public DisplayManagerInternal.RefreshRateRange getRefreshRateForDisplayAndSensor(int displayId, String sensorName, String sensorType) {
            SensorManager sensorManager;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                sensorManager = DisplayManagerService.this.mSensorManager;
            }
            if (sensorManager == null) {
                return null;
            }
            Sensor sensor = SensorUtils.findSensor(sensorManager, sensorType, sensorName, 0);
            if (sensor == null) {
                return null;
            }
            synchronized (DisplayManagerService.this.mSyncRoot) {
                LogicalDisplay display = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(displayId);
                if (display == null) {
                    return null;
                }
                DisplayDevice device = display.getPrimaryDisplayDeviceLocked();
                if (device == null) {
                    return null;
                }
                DisplayDeviceConfig config = device.getDisplayDeviceConfig();
                DisplayDeviceConfig.SensorData sensorData = config.getProximitySensor();
                if (!sensorData.matches(sensorName, sensorType)) {
                    return null;
                }
                return new DisplayManagerInternal.RefreshRateRange(sensorData.minRefreshRate, sensorData.maxRefreshRate);
            }
        }

        public List<DisplayManagerInternal.RefreshRateLimitation> getRefreshRateLimitations(int displayId) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayDevice device = DisplayManagerService.this.getDeviceForDisplayLocked(displayId);
                if (device == null) {
                    return null;
                }
                DisplayDeviceConfig config = device.getDisplayDeviceConfig();
                return config.getRefreshRateLimitations();
            }
        }

        public float getDefaultDisplayBrightness() {
            LogicalDisplay display = DisplayManagerService.this.mLogicalDisplayMapper.getDisplayLocked(0);
            if (display == null) {
                return -1.0f;
            }
            float brightness = DisplayManagerService.this.mPersistentDataStore.getBrightness(display.getPrimaryDisplayDeviceLocked());
            if (Float.isNaN(brightness)) {
                return -1.0f;
            }
            return brightness;
        }

        public void setWindowManagerMirroring(int displayId, boolean isMirroring) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayDevice device = DisplayManagerService.this.getDeviceForDisplayLocked(displayId);
                if (device != null) {
                    device.setWindowManagerMirroringLocked(isMirroring);
                }
            }
        }

        public Point getDisplaySurfaceDefaultSize(int displayId) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayDevice device = DisplayManagerService.this.getDeviceForDisplayLocked(displayId);
                if (device == null) {
                    return null;
                }
                return device.getDisplaySurfaceDefaultSizeLocked();
            }
        }

        public void onEarlyInteractivityChange(boolean interactive) {
            DisplayManagerService.this.mLogicalDisplayMapper.onEarlyInteractivityChange(interactive);
        }

        public void updatePowerStateForLucid() {
            DisplayManagerService.this.updatePowerStateInternalForLucid();
        }

        public DisplayWindowPolicyController getDisplayWindowPolicyController(int displayId) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                if (DisplayManagerService.this.mDisplayWindowPolicyControllers.contains(displayId)) {
                    return (DisplayWindowPolicyController) DisplayManagerService.this.mDisplayWindowPolicyControllers.get(displayId).second;
                }
                return null;
            }
        }

        public void onWakeDisplayCalled() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                if (DisplayManagerService.this.mCurrentForcedMode == 3) {
                    DisplayManagerService.this.setForcedUsingDisplayModeInternalLocked(0);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DesiredDisplayModeSpecsObserver implements DisplayModeDirector.DesiredDisplayModeSpecsListener {
        private final Consumer<LogicalDisplay> mSpecsChangedConsumer = new Consumer() { // from class: com.android.server.display.DisplayManagerService$DesiredDisplayModeSpecsObserver$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayManagerService.DesiredDisplayModeSpecsObserver.this.m3338xd4728b93((LogicalDisplay) obj);
            }
        };
        private boolean mChanged = false;

        DesiredDisplayModeSpecsObserver() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-display-DisplayManagerService$DesiredDisplayModeSpecsObserver  reason: not valid java name */
        public /* synthetic */ void m3338xd4728b93(LogicalDisplay display) {
            int displayId = display.getDisplayIdLocked();
            DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs = DisplayManagerService.this.mDisplayModeDirector.getDesiredDisplayModeSpecs(displayId);
            DisplayModeDirector.DesiredDisplayModeSpecs existingDesiredDisplayModeSpecs = display.getDesiredDisplayModeSpecsLocked();
            if (DisplayManagerService.DEBUG) {
                Slog.i(DisplayManagerService.TAG, "Comparing display specs: " + desiredDisplayModeSpecs + ", existing: " + existingDesiredDisplayModeSpecs);
            }
            if (!desiredDisplayModeSpecs.equals(existingDesiredDisplayModeSpecs)) {
                display.setDesiredDisplayModeSpecsLocked(desiredDisplayModeSpecs);
                this.mChanged = true;
            }
        }

        @Override // com.android.server.display.DisplayModeDirector.DesiredDisplayModeSpecsListener
        public void onDesiredDisplayModeSpecsChanged() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                this.mChanged = false;
                DisplayManagerService.this.mLogicalDisplayMapper.forEachLocked(this.mSpecsChangedConsumer);
                if (this.mChanged) {
                    DisplayManagerService.this.scheduleTraversalLocked(false);
                    this.mChanged = false;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class DeviceStateListener implements DeviceStateManager.DeviceStateCallback {
        private int mBaseState = -1;

        DeviceStateListener() {
        }

        public void onStateChanged(int deviceState) {
            boolean isFromUnfoldToFold = true;
            boolean isDeviceStateOverrideActive = deviceState != this.mBaseState;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                if (DisplayManagerService.this.mDeviceStateFromFold == 0 || deviceState != 0) {
                    isFromUnfoldToFold = false;
                }
                DisplayManagerService.this.mDeviceStateFromFold = deviceState;
                Slog.i("SwapDisplay", "dms-setDeviceStateLockedFromFold:" + DisplayManagerService.this.mDeviceStateFromFold + "    /mCurrentForcedMode:" + DisplayManagerService.this.mCurrentForcedMode);
                if (isFromUnfoldToFold && DisplayManagerService.this.mCurrentForcedMode == 3) {
                    DisplayManagerService.this.updateForceMode(0);
                }
                DisplayManagerService.this.updateDeviceStateLocked(isDeviceStateOverrideActive);
            }
        }

        public void onBaseStateChanged(int state) {
            this.mBaseState = state;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BrightnessPair {
        public float brightness;
        public float sdrBrightness;

        BrightnessPair(float brightness, float sdrBrightness) {
            this.brightness = brightness;
            this.sdrBrightness = sdrBrightness;
        }
    }
}
