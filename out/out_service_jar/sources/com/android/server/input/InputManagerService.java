package com.android.server.input;

import android.app.ActivityManagerInternal;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.graphics.PointF;
import android.hardware.SensorPrivacyManagerInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayViewport;
import android.hardware.input.IGestureListener;
import android.hardware.input.IInputDevicesChangedListener;
import android.hardware.input.IInputManager;
import android.hardware.input.IInputSensorEventListener;
import android.hardware.input.ITabletModeChangedListener;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.hardware.input.InputSensorInfo;
import android.hardware.input.KeyboardLayout;
import android.hardware.input.TouchCalibration;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IVibratorStateListener;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.IInputFilter;
import android.view.IInputFilterHost;
import android.view.IInputMonitorHost;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputMonitor;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.SurfaceControl;
import android.view.VerifiedInputEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.am.HostingRecord;
import com.android.server.input.InputManagerService;
import com.android.server.input.NativeInputManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.transsion.hubcore.server.ITranInput;
import com.transsion.hubsdk.trancare.trancare.TranTrancareCallback;
import com.transsion.hubsdk.trancare.trancare.TranTrancareManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import libcore.io.Streams;
/* loaded from: classes.dex */
public class InputManagerService extends IInputManager.Stub implements Watchdog.Monitor {
    public static final int BTN_MOUSE = 272;
    static final boolean DEBUG = false;
    private static final String DEEP_PRESS_ENABLED = "deep_press_enabled";
    private static final int DEFAULT_VIBRATION_MAGNITUDE = 192;
    private static final String EXCLUDED_DEVICES_PATH = "etc/excluded-input-devices.xml";
    private static final int INJECTION_TIMEOUT_MILLIS = 30000;
    public static final int KEY_STATE_DOWN = 1;
    public static final int KEY_STATE_UNKNOWN = -1;
    public static final int KEY_STATE_UP = 0;
    public static final int KEY_STATE_VIRTUAL = 2;
    public static final long MONITOR_INPUT_RESPONSE_CONTROL = 933861000014L;
    public static final int MONITOR_UB_APP_RESP_TIME = 10650092;
    private static final int MSG_DELIVER_INPUT_DEVICES_CHANGED = 1;
    private static final int MSG_DELIVER_TABLET_MODE_CHANGED = 6;
    private static final int MSG_POINTER_DISPLAY_ID_CHANGED = 7;
    private static final int MSG_RELOAD_DEVICE_ALIASES = 5;
    private static final int MSG_RELOAD_KEYBOARD_LAYOUTS = 3;
    private static final int MSG_SWITCH_KEYBOARD_LAYOUT = 2;
    private static final int MSG_UPDATE_KEYBOARD_LAYOUTS = 4;
    private static final String PORT_ASSOCIATIONS_PATH = "etc/input-port-associations.xml";
    public static final int SW_CAMERA_LENS_COVER = 9;
    public static final int SW_CAMERA_LENS_COVER_BIT = 512;
    public static final int SW_HEADPHONE_INSERT = 2;
    public static final int SW_HEADPHONE_INSERT_BIT = 4;
    public static final int SW_JACK_BITS = 212;
    public static final int SW_JACK_PHYSICAL_INSERT = 7;
    public static final int SW_JACK_PHYSICAL_INSERT_BIT = 128;
    public static final int SW_KEYPAD_SLIDE = 10;
    public static final int SW_KEYPAD_SLIDE_BIT = 1024;
    public static final int SW_LID = 0;
    public static final int SW_LID_BIT = 1;
    public static final int SW_LINEOUT_INSERT = 6;
    public static final int SW_LINEOUT_INSERT_BIT = 64;
    public static final int SW_MICROPHONE_INSERT = 4;
    public static final int SW_MICROPHONE_INSERT_BIT = 16;
    public static final int SW_MUTE_DEVICE = 14;
    public static final int SW_MUTE_DEVICE_BIT = 16384;
    public static final int SW_TABLET_MODE = 1;
    public static final int SW_TABLET_MODE_BIT = 2;
    static final String TAG = "InputManager";
    private static final boolean UNTRUSTED_TOUCHES_TOAST = false;
    private int mAcknowledgedPointerDisplayId;
    private final SparseArray<AdditionalDisplayInputProperties> mAdditionalDisplayInputProperties;
    private final Object mAdditionalDisplayInputPropertiesLock;
    private HashMap<String, AppResponseTimeRecord> mAppResponseTimeRecords;
    private final Object mAssociationsLock;
    private final Context mContext;
    private final AdditionalDisplayInputProperties mCurrentDisplayProperties;
    private final PersistentDataStore mDataStore;
    private final File mDoubleTouchGestureEnableFile;
    private final InputManagerHandler mHandler;
    private PointerIcon mIcon;
    private int mIconType;
    private InputDevice[] mInputDevices;
    private final SparseArray<InputDevicesChangedListenerRecord> mInputDevicesChangedListeners;
    private boolean mInputDevicesChangedPending;
    private final Object mInputDevicesLock;
    IInputFilter mInputFilter;
    InputFilterHost mInputFilterHost;
    final Object mInputFilterLock;
    final Map<IBinder, GestureMonitorSpyWindow> mInputMonitors;
    private boolean mIsRspDumpEnabled;
    private boolean mIsRspTimeEnabled;
    private final SparseBooleanArray mIsVibrating;
    private boolean mKeyboardLayoutNotificationShown;
    private final List<InputManagerInternal.LidSwitchCallback> mLidSwitchCallbacks;
    private final Object mLidSwitchLock;
    private final Object mLightLock;
    private final ArrayMap<IBinder, LightSession> mLightSessions;
    private final NativeInputManagerService mNative;
    private int mNextVibratorTokenValue;
    private NotificationManager mNotificationManager;
    private int mOverriddenPointerDisplayId;
    private Context mPointerIconDisplayContext;
    private int mRequestedPointerDisplayId;
    private final Map<String, Integer> mRuntimeAssociations;
    private final List<SensorEventListenerRecord> mSensorAccuracyListenersToNotify;
    private final SparseArray<SensorEventListenerRecord> mSensorEventListeners;
    private final List<SensorEventListenerRecord> mSensorEventListenersToNotify;
    private final Object mSensorEventLock;
    private final Map<String, Integer> mStaticAssociations;
    private Toast mSwitchedKeyboardLayoutToast;
    private boolean mSystemReady;
    private final SparseArray<TabletModeChangedListenerRecord> mTabletModeChangedListeners;
    private final Object mTabletModeLock;
    private final ArrayList<InputDevice> mTempFullKeyboards;
    private final ArrayList<InputDevicesChangedListenerRecord> mTempInputDevicesChangedListenersToNotify;
    private final List<TabletModeChangedListenerRecord> mTempTabletModeChangedListenersToNotify;
    private final Map<String, String> mUniqueIdAssociations;
    final boolean mUseDevInputEventForAudioJack;
    private final Object mVibratorLock;
    private final SparseArray<RemoteCallbackList<IVibratorStateListener>> mVibratorStateListeners;
    private final Map<IBinder, VibratorToken> mVibratorTokens;
    private WindowManagerCallbacks mWindowManagerCallbacks;
    private WiredAccessoryCallbacks mWiredAccessoryCallbacks;
    private static final AdditionalDisplayInputProperties DEFAULT_ADDITIONAL_DISPLAY_INPUT_PROPERTIES = new AdditionalDisplayInputProperties();
    private static final String[] PACKAGE_BLOCKLIST_FOR_UNTRUSTED_TOUCHES_TOAST = {"com.snapchat.android"};

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface KeyboardLayoutVisitor {
        void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout);
    }

    /* loaded from: classes.dex */
    public interface WindowManagerCallbacks extends InputManagerInternal.LidSwitchCallback {
        SurfaceControl createSurfaceForGestureMonitor(String str, int i);

        KeyEvent dispatchUnhandledKey(IBinder iBinder, KeyEvent keyEvent, int i);

        PointF getCursorPosition();

        SurfaceControl getParentSurfaceForPointers(int i);

        int getPointerDisplayId();

        int getPointerLayer();

        long interceptKeyBeforeDispatching(IBinder iBinder, KeyEvent keyEvent, int i);

        int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i);

        int interceptMotionBeforeQueueingNonInteractive(int i, long j, int i2);

        void notifyCameraLensCoverSwitchChanged(long j, boolean z);

        void notifyConfigurationChanged();

        void notifyDropWindow(IBinder iBinder, float f, float f2);

        void notifyFocusChanged(IBinder iBinder, IBinder iBinder2);

        void notifyInputChannelBroken(IBinder iBinder);

        void notifyNoFocusedWindowAnr(InputApplicationHandle inputApplicationHandle);

        void notifyPointerDisplayIdChanged(int i, float f, float f2);

        void notifyWindowResponsive(IBinder iBinder, OptionalInt optionalInt);

        void notifyWindowUnresponsive(IBinder iBinder, OptionalInt optionalInt, String str);

        void onPointerDownOutsideFocus(IBinder iBinder);
    }

    /* loaded from: classes.dex */
    public interface WiredAccessoryCallbacks {
        void notifyWiredAccessoryChanged(long j, int i, int i2);

        void systemReady();
    }

    /* loaded from: classes.dex */
    static class Injector {
        private final Context mContext;
        private final Looper mLooper;

        Injector(Context context, Looper looper) {
            this.mContext = context;
            this.mLooper = looper;
        }

        Context getContext() {
            return this.mContext;
        }

        Looper getLooper() {
            return this.mLooper;
        }

        NativeInputManagerService getNativeService(InputManagerService service) {
            return new NativeInputManagerService.NativeImpl(service, this.mContext, this.mLooper.getQueue());
        }

        void registerLocalService(InputManagerInternal localService) {
            LocalServices.addService(InputManagerInternal.class, localService);
        }
    }

    public InputManagerService(Context context) {
        this(new Injector(context, DisplayThread.get().getLooper()));
    }

    InputManagerService(Injector injector) {
        this.mTabletModeLock = new Object();
        this.mTabletModeChangedListeners = new SparseArray<>();
        this.mTempTabletModeChangedListenersToNotify = new ArrayList();
        this.mSensorEventLock = new Object();
        this.mSensorEventListeners = new SparseArray<>();
        this.mSensorEventListenersToNotify = new ArrayList();
        this.mSensorAccuracyListenersToNotify = new ArrayList();
        this.mDataStore = new PersistentDataStore();
        this.mInputDevicesLock = new Object();
        this.mInputDevices = new InputDevice[0];
        this.mInputDevicesChangedListeners = new SparseArray<>();
        this.mTempInputDevicesChangedListenersToNotify = new ArrayList<>();
        this.mTempFullKeyboards = new ArrayList<>();
        this.mVibratorLock = new Object();
        this.mVibratorTokens = new ArrayMap();
        this.mVibratorStateListeners = new SparseArray<>();
        this.mIsVibrating = new SparseBooleanArray();
        this.mLightLock = new Object();
        this.mLightSessions = new ArrayMap<>();
        this.mLidSwitchLock = new Object();
        this.mLidSwitchCallbacks = new ArrayList();
        this.mInputFilterLock = new Object();
        this.mAssociationsLock = new Object();
        this.mRuntimeAssociations = new ArrayMap();
        this.mUniqueIdAssociations = new ArrayMap();
        this.mAdditionalDisplayInputPropertiesLock = new Object();
        this.mOverriddenPointerDisplayId = -1;
        this.mAcknowledgedPointerDisplayId = -1;
        this.mRequestedPointerDisplayId = -1;
        this.mAdditionalDisplayInputProperties = new SparseArray<>();
        this.mCurrentDisplayProperties = new AdditionalDisplayInputProperties();
        this.mIconType = 1;
        this.mInputMonitors = new HashMap();
        this.mAppResponseTimeRecords = new HashMap<>();
        this.mIsRspTimeEnabled = false;
        this.mIsRspDumpEnabled = false;
        this.mStaticAssociations = loadStaticInputPortAssociations();
        Context context = injector.getContext();
        this.mContext = context;
        this.mHandler = new InputManagerHandler(injector.getLooper());
        this.mNative = injector.getNativeService(this);
        boolean z = context.getResources().getBoolean(17891811);
        this.mUseDevInputEventForAudioJack = z;
        Slog.i(TAG, "Initializing input manager, mUseDevInputEventForAudioJack=" + z);
        String doubleTouchGestureEnablePath = context.getResources().getString(17039963);
        this.mDoubleTouchGestureEnableFile = TextUtils.isEmpty(doubleTouchGestureEnablePath) ? null : new File(doubleTouchGestureEnablePath);
        injector.registerLocalService(new LocalService());
        if (Build.TRANCARE_SUPPORT) {
            TranTrancareManager.regTranLogCallback(new TranTrancareCallback() { // from class: com.android.server.input.InputManagerService.1
                public void onTidChange() {
                    try {
                        boolean isRspTimeEnabled = TranTrancareManager.isEnabled((long) InputManagerService.MONITOR_INPUT_RESPONSE_CONTROL);
                        if (isRspTimeEnabled != InputManagerService.this.mIsRspTimeEnabled) {
                            InputManagerService.this.setInputRspTimeEnabled(isRspTimeEnabled);
                            InputManagerService.this.setInputRspDumpEnabled(isRspTimeEnabled);
                            InputManagerService.this.mIsRspTimeEnabled = isRspTimeEnabled;
                            InputManagerService.this.mIsRspDumpEnabled = isRspTimeEnabled;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void setWindowManagerCallbacks(WindowManagerCallbacks callbacks) {
        WindowManagerCallbacks windowManagerCallbacks = this.mWindowManagerCallbacks;
        if (windowManagerCallbacks != null) {
            unregisterLidSwitchCallbackInternal(windowManagerCallbacks);
        }
        this.mWindowManagerCallbacks = callbacks;
        registerLidSwitchCallbackInternal(callbacks);
    }

    public void setWiredAccessoryCallbacks(WiredAccessoryCallbacks callbacks) {
        this.mWiredAccessoryCallbacks = callbacks;
    }

    void registerLidSwitchCallbackInternal(InputManagerInternal.LidSwitchCallback callback) {
        synchronized (this.mLidSwitchLock) {
            this.mLidSwitchCallbacks.add(callback);
            if (this.mSystemReady) {
                boolean lidOpen = getSwitchState(-1, -256, 0) == 0;
                callback.notifyLidSwitchChanged(0L, lidOpen);
            }
        }
    }

    void unregisterLidSwitchCallbackInternal(InputManagerInternal.LidSwitchCallback callback) {
        synchronized (this.mLidSwitchLock) {
            this.mLidSwitchCallbacks.remove(callback);
        }
    }

    public void start() {
        Slog.i(TAG, "Starting input manager");
        this.mNative.start();
        Watchdog.getInstance().addMonitor(this);
        registerPointerSpeedSettingObserver();
        registerShowTouchesSettingObserver();
        registerAccessibilityLargePointerSettingObserver();
        registerLongPressTimeoutObserver();
        registerMaximumObscuringOpacityForTouchSettingObserver();
        registerBlockUntrustedTouchesModeSettingObserver();
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.input.InputManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.updatePointerSpeedFromSettings();
                InputManagerService.this.updateShowTouchesFromSettings();
                InputManagerService.this.updateAccessibilityLargePointerFromSettings();
                InputManagerService.this.updateDeepPressStatusFromSettings("user switched");
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
        updatePointerSpeedFromSettings();
        updateShowTouchesFromSettings();
        updateAccessibilityLargePointerFromSettings();
        updateDeepPressStatusFromSettings("just booted");
        updateMaximumObscuringOpacityForTouchFromSettings();
        updateBlockUntrustedTouchesModeFromSettings();
    }

    public void systemRunning() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        synchronized (this.mLidSwitchLock) {
            this.mSystemReady = true;
            int switchState = getSwitchState(-1, -256, 0);
            for (int i = 0; i < this.mLidSwitchCallbacks.size(); i++) {
                InputManagerInternal.LidSwitchCallback callback = this.mLidSwitchCallbacks.get(i);
                callback.notifyLidSwitchChanged(0L, switchState == 0);
            }
        }
        int micMuteState = getSwitchState(-1, -256, 14);
        if (micMuteState != -1) {
            setSensorPrivacy(1, micMuteState != 0);
        }
        int cameraMuteState = getSwitchState(-1, -256, 9);
        if (cameraMuteState != -1) {
            setSensorPrivacy(2, cameraMuteState != 0);
        }
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.input.InputManagerService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.updateKeyboardLayouts();
            }
        }, filter, null, this.mHandler);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.input.InputManagerService.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.reloadDeviceAliases();
            }
        }, new IntentFilter("android.bluetooth.device.action.ALIAS_CHANGED"), null, this.mHandler);
        this.mHandler.sendEmptyMessage(5);
        this.mHandler.sendEmptyMessage(4);
        WiredAccessoryCallbacks wiredAccessoryCallbacks = this.mWiredAccessoryCallbacks;
        if (wiredAccessoryCallbacks != null) {
            wiredAccessoryCallbacks.systemReady();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadKeyboardLayouts() {
        this.mNative.reloadKeyboardLayouts();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadDeviceAliases() {
        this.mNative.reloadDeviceAliases();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayViewportsInternal(List<DisplayViewport> viewports) {
        DisplayViewport[] vArray = new DisplayViewport[viewports.size()];
        for (int i = viewports.size() - 1; i >= 0; i--) {
            vArray[i] = viewports.get(i);
        }
        this.mNative.setDisplayViewports(vArray);
        int pointerDisplayId = this.mWindowManagerCallbacks.getPointerDisplayId();
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            if (this.mOverriddenPointerDisplayId == -1) {
                updatePointerDisplayIdLocked(pointerDisplayId);
            }
        }
    }

    public int getKeyCodeState(int deviceId, int sourceMask, int keyCode) {
        return this.mNative.getKeyCodeState(deviceId, sourceMask, keyCode);
    }

    public int getScanCodeState(int deviceId, int sourceMask, int scanCode) {
        return this.mNative.getScanCodeState(deviceId, sourceMask, scanCode);
    }

    public int getSwitchState(int deviceId, int sourceMask, int switchCode) {
        return this.mNative.getSwitchState(deviceId, sourceMask, switchCode);
    }

    public boolean hasKeys(int deviceId, int sourceMask, int[] keyCodes, boolean[] keyExists) {
        Objects.requireNonNull(keyCodes, "keyCodes must not be null");
        Objects.requireNonNull(keyExists, "keyExists must not be null");
        if (keyExists.length < keyCodes.length) {
            throw new IllegalArgumentException("keyExists must be at least as large as keyCodes");
        }
        return this.mNative.hasKeys(deviceId, sourceMask, keyCodes, keyExists);
    }

    public int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode) {
        if (locationKeyCode <= 0 || locationKeyCode > KeyEvent.getMaxKeyCode()) {
            return 0;
        }
        return this.mNative.getKeyCodeForKeyLocation(deviceId, locationKeyCode);
    }

    public boolean transferTouch(IBinder destChannelToken, int displayId) {
        Objects.requireNonNull(destChannelToken, "destChannelToken must not be null");
        return this.mNative.transferTouch(destChannelToken, displayId);
    }

    public InputChannel monitorInput(String inputChannelName, int displayId) {
        Objects.requireNonNull(inputChannelName, "inputChannelName not be null");
        if (displayId < 0) {
            throw new IllegalArgumentException("displayId must >= 0.");
        }
        return this.mNative.createInputMonitor(displayId, inputChannelName, Binder.getCallingPid());
    }

    private InputChannel createSpyWindowGestureMonitor(IBinder monitorToken, String name, int displayId, int pid, int uid) {
        SurfaceControl sc = this.mWindowManagerCallbacks.createSurfaceForGestureMonitor(name, displayId);
        if (sc == null) {
            throw new IllegalArgumentException("Could not create gesture monitor surface on display: " + displayId);
        }
        final InputChannel channel = createInputChannel(name);
        try {
            try {
                monitorToken.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda7
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        InputManagerService.this.m3938xe697d403(channel);
                    }
                }, 0);
                synchronized (this.mInputMonitors) {
                    try {
                        try {
                            this.mInputMonitors.put(channel.getToken(), new GestureMonitorSpyWindow(monitorToken, name, displayId, pid, uid, sc, channel));
                            InputChannel outInputChannel = new InputChannel();
                            channel.copyTo(outInputChannel);
                            return outInputChannel;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (RemoteException e) {
                Slog.i(TAG, "Client died before '" + name + "' could be created.");
                return null;
            }
        } catch (RemoteException e2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createSpyWindowGestureMonitor$0$com-android-server-input-InputManagerService  reason: not valid java name */
    public /* synthetic */ void m3938xe697d403(InputChannel channel) {
        removeSpyWindowGestureMonitor(channel.getToken());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSpyWindowGestureMonitor(IBinder inputChannelToken) {
        GestureMonitorSpyWindow monitor;
        synchronized (this.mInputMonitors) {
            monitor = this.mInputMonitors.remove(inputChannelToken);
        }
        removeInputChannel(inputChannelToken);
        if (monitor == null) {
            return;
        }
        monitor.remove();
    }

    public InputMonitor monitorGestureInput(IBinder monitorToken, String requestedName, int displayId) {
        if (!checkCallingPermission("android.permission.MONITOR_INPUT", "monitorGestureInput()")) {
            throw new SecurityException("Requires MONITOR_INPUT permission");
        }
        Objects.requireNonNull(requestedName, "name must not be null.");
        Objects.requireNonNull(monitorToken, "token must not be null.");
        if (displayId < 0) {
            throw new IllegalArgumentException("displayId must >= 0.");
        }
        String name = "[Gesture Monitor] " + requestedName;
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            InputChannel inputChannel = createSpyWindowGestureMonitor(monitorToken, name, displayId, pid, uid);
            return new InputMonitor(inputChannel, new InputMonitorHost(inputChannel.getToken()));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public InputChannel createInputChannel(String name) {
        return this.mNative.createInputChannel(name);
    }

    public void removeInputChannel(IBinder connectionToken) {
        Objects.requireNonNull(connectionToken, "connectionToken must not be null");
        this.mNative.removeInputChannel(connectionToken);
    }

    public void setInputFilter(IInputFilter filter) {
        synchronized (this.mInputFilterLock) {
            IInputFilter oldFilter = this.mInputFilter;
            if (oldFilter == filter) {
                return;
            }
            if (oldFilter != null) {
                this.mInputFilter = null;
                this.mInputFilterHost.disconnectLocked();
                this.mInputFilterHost = null;
                try {
                    oldFilter.uninstall();
                } catch (RemoteException e) {
                }
            }
            if (filter != null) {
                this.mInputFilter = filter;
                InputFilterHost inputFilterHost = new InputFilterHost();
                this.mInputFilterHost = inputFilterHost;
                try {
                    filter.install(inputFilterHost);
                } catch (RemoteException e2) {
                }
            }
            this.mNative.setInputFilterEnabled(filter != null);
        }
    }

    public boolean setInTouchMode(boolean inTouchMode, int pid, int uid, boolean hasPermission) {
        return this.mNative.setInTouchMode(inTouchMode, pid, uid, hasPermission);
    }

    public boolean injectInputEvent(InputEvent event, int mode) {
        return injectInputEventToTarget(event, mode, -1);
    }

    public boolean injectInputEventToTarget(InputEvent event, int mode, int targetUid) {
        if (!checkCallingPermission("android.permission.INJECT_EVENTS", "injectInputEvent()", true)) {
            throw new SecurityException("Injecting input events requires the caller (or the source of the instrumentation, if any) to have the INJECT_EVENTS permission.");
        }
        Objects.requireNonNull(event, "event must not be null");
        if (mode != 0 && mode != 2 && mode != 1) {
            throw new IllegalArgumentException("mode is invalid");
        }
        int pid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        boolean injectIntoUid = targetUid != -1;
        try {
            int result = this.mNative.injectInputEvent(event, injectIntoUid, targetUid, mode, 30000, 134217728);
            Binder.restoreCallingIdentity(ident);
            switch (result) {
                case 0:
                    return true;
                case 1:
                    if (injectIntoUid) {
                        if (checkCallingPermission("android.permission.INJECT_EVENTS", "injectInputEvent-target-mismatch-fallback")) {
                            Slog.w(TAG, "Targeted input event was not directed at a window owned by uid " + targetUid + ". Falling back to injecting into all windows.");
                            return injectInputEventToTarget(event, mode, -1);
                        }
                        throw new IllegalArgumentException("Targeted input event injection from pid " + pid + " was not directed at a window owned by uid " + targetUid + ".");
                    }
                    throw new IllegalStateException("Injection should not result in TARGET_MISMATCH when it is not targeted into to a specific uid.");
                case 2:
                default:
                    Slog.w(TAG, "Input event injection from pid " + pid + " failed.");
                    return false;
                case 3:
                    Slog.w(TAG, "Input event injection from pid " + pid + " timed out.");
                    return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public VerifiedInputEvent verifyInputEvent(InputEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return this.mNative.verifyInputEvent(event);
    }

    public InputDevice getInputDevice(int deviceId) {
        InputDevice[] inputDeviceArr;
        synchronized (this.mInputDevicesLock) {
            for (InputDevice inputDevice : this.mInputDevices) {
                if (inputDevice.getId() == deviceId) {
                    return inputDevice;
                }
            }
            return null;
        }
    }

    public boolean isInputDeviceEnabled(int deviceId) {
        return this.mNative.isInputDeviceEnabled(deviceId);
    }

    public void enableInputDevice(int deviceId) {
        if (!checkCallingPermission("android.permission.DISABLE_INPUT_DEVICE", "enableInputDevice()")) {
            throw new SecurityException("Requires DISABLE_INPUT_DEVICE permission");
        }
        this.mNative.enableInputDevice(deviceId);
    }

    public void disableInputDevice(int deviceId) {
        if (!checkCallingPermission("android.permission.DISABLE_INPUT_DEVICE", "disableInputDevice()")) {
            throw new SecurityException("Requires DISABLE_INPUT_DEVICE permission");
        }
        this.mNative.disableInputDevice(deviceId);
    }

    public int[] getInputDeviceIds() {
        int[] ids;
        synchronized (this.mInputDevicesLock) {
            int count = this.mInputDevices.length;
            ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = this.mInputDevices[i].getId();
            }
        }
        return ids;
    }

    public InputDevice[] getInputDevices() {
        InputDevice[] inputDeviceArr;
        synchronized (this.mInputDevicesLock) {
            inputDeviceArr = this.mInputDevices;
        }
        return inputDeviceArr;
    }

    public void registerInputDevicesChangedListener(IInputDevicesChangedListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        synchronized (this.mInputDevicesLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mInputDevicesChangedListeners.get(callingPid) != null) {
                throw new SecurityException("The calling process has already registered an InputDevicesChangedListener.");
            }
            InputDevicesChangedListenerRecord record = new InputDevicesChangedListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mInputDevicesChangedListeners.put(callingPid, record);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInputDevicesChangedListenerDied(int pid) {
        synchronized (this.mInputDevicesLock) {
            this.mInputDevicesChangedListeners.remove(pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverInputDevicesChanged(InputDevice[] oldInputDevices) {
        int numFullKeyboardsAdded = 0;
        this.mTempInputDevicesChangedListenersToNotify.clear();
        this.mTempFullKeyboards.clear();
        synchronized (this.mInputDevicesLock) {
            try {
                if (this.mInputDevicesChangedPending) {
                    this.mInputDevicesChangedPending = false;
                    int numListeners = this.mInputDevicesChangedListeners.size();
                    for (int i = 0; i < numListeners; i++) {
                        this.mTempInputDevicesChangedListenersToNotify.add(this.mInputDevicesChangedListeners.valueAt(i));
                    }
                    int numDevices = this.mInputDevices.length;
                    int[] deviceIdAndGeneration = new int[numDevices * 2];
                    for (int i2 = 0; i2 < numDevices; i2++) {
                        InputDevice inputDevice = this.mInputDevices[i2];
                        deviceIdAndGeneration[i2 * 2] = inputDevice.getId();
                        deviceIdAndGeneration[(i2 * 2) + 1] = inputDevice.getGeneration();
                        if (!inputDevice.isVirtual() && inputDevice.isFullKeyboard()) {
                            if (!containsInputDeviceWithDescriptor(oldInputDevices, inputDevice.getDescriptor())) {
                                int numFullKeyboardsAdded2 = numFullKeyboardsAdded + 1;
                                try {
                                    this.mTempFullKeyboards.add(numFullKeyboardsAdded, inputDevice);
                                    numFullKeyboardsAdded = numFullKeyboardsAdded2;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                this.mTempFullKeyboards.add(inputDevice);
                            }
                        }
                    }
                    for (int i3 = 0; i3 < numListeners; i3++) {
                        this.mTempInputDevicesChangedListenersToNotify.get(i3).notifyInputDevicesChanged(deviceIdAndGeneration);
                    }
                    this.mTempInputDevicesChangedListenersToNotify.clear();
                    List<InputDevice> keyboardsMissingLayout = new ArrayList<>();
                    int numFullKeyboards = this.mTempFullKeyboards.size();
                    synchronized (this.mDataStore) {
                        for (int i4 = 0; i4 < numFullKeyboards; i4++) {
                            InputDevice inputDevice2 = this.mTempFullKeyboards.get(i4);
                            String layout = getCurrentKeyboardLayoutForInputDevice(inputDevice2.getIdentifier());
                            if (layout == null && (layout = getDefaultKeyboardLayout(inputDevice2)) != null) {
                                setCurrentKeyboardLayoutForInputDevice(inputDevice2.getIdentifier(), layout);
                            }
                            if (layout == null) {
                                keyboardsMissingLayout.add(inputDevice2);
                            }
                        }
                    }
                    if (this.mNotificationManager != null) {
                        if (!keyboardsMissingLayout.isEmpty()) {
                            if (keyboardsMissingLayout.size() > 1) {
                                showMissingKeyboardLayoutNotification(null);
                            } else {
                                showMissingKeyboardLayoutNotification(keyboardsMissingLayout.get(0));
                            }
                        } else if (this.mKeyboardLayoutNotificationShown) {
                            hideMissingKeyboardLayoutNotification();
                        }
                    }
                    this.mTempFullKeyboards.clear();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private String getDefaultKeyboardLayout(final InputDevice d) {
        final Locale systemLocale = this.mContext.getResources().getConfiguration().locale;
        if (TextUtils.isEmpty(systemLocale.getLanguage())) {
            return null;
        }
        final List<KeyboardLayout> layouts = new ArrayList<>();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda4
            @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
            public final void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                InputManagerService.lambda$getDefaultKeyboardLayout$1(d, systemLocale, layouts, resources, i, keyboardLayout);
            }
        });
        if (layouts.isEmpty()) {
            return null;
        }
        Collections.sort(layouts);
        int N = layouts.size();
        for (int i = 0; i < N; i++) {
            KeyboardLayout layout = layouts.get(i);
            LocaleList locales = layout.getLocales();
            int numLocales = locales.size();
            for (int localeIndex = 0; localeIndex < numLocales; localeIndex++) {
                Locale locale = locales.get(localeIndex);
                if (locale.getCountry().equals(systemLocale.getCountry()) && locale.getVariant().equals(systemLocale.getVariant())) {
                    return layout.getDescriptor();
                }
            }
        }
        for (int i2 = 0; i2 < N; i2++) {
            KeyboardLayout layout2 = layouts.get(i2);
            LocaleList locales2 = layout2.getLocales();
            int numLocales2 = locales2.size();
            for (int localeIndex2 = 0; localeIndex2 < numLocales2; localeIndex2++) {
                if (locales2.get(localeIndex2).getCountry().equals(systemLocale.getCountry())) {
                    return layout2.getDescriptor();
                }
            }
        }
        return layouts.get(0).getDescriptor();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getDefaultKeyboardLayout$1(InputDevice d, Locale systemLocale, List layouts, Resources resources, int keyboardLayoutResId, KeyboardLayout layout) {
        if (layout.getVendorId() != d.getVendorId() || layout.getProductId() != d.getProductId()) {
            return;
        }
        LocaleList locales = layout.getLocales();
        int numLocales = locales.size();
        for (int localeIndex = 0; localeIndex < numLocales; localeIndex++) {
            if (isCompatibleLocale(systemLocale, locales.get(localeIndex))) {
                layouts.add(layout);
                return;
            }
        }
    }

    private static boolean isCompatibleLocale(Locale systemLocale, Locale keyboardLocale) {
        if (systemLocale.getLanguage().equals(keyboardLocale.getLanguage())) {
            return TextUtils.isEmpty(systemLocale.getCountry()) || TextUtils.isEmpty(keyboardLocale.getCountry()) || systemLocale.getCountry().equals(keyboardLocale.getCountry());
        }
        return false;
    }

    public TouchCalibration getTouchCalibrationForInputDevice(String inputDeviceDescriptor, int surfaceRotation) {
        TouchCalibration touchCalibration;
        Objects.requireNonNull(inputDeviceDescriptor, "inputDeviceDescriptor must not be null");
        synchronized (this.mDataStore) {
            touchCalibration = this.mDataStore.getTouchCalibration(inputDeviceDescriptor, surfaceRotation);
        }
        return touchCalibration;
    }

    public void setTouchCalibrationForInputDevice(String inputDeviceDescriptor, int surfaceRotation, TouchCalibration calibration) {
        if (!checkCallingPermission("android.permission.SET_INPUT_CALIBRATION", "setTouchCalibrationForInputDevice()")) {
            throw new SecurityException("Requires SET_INPUT_CALIBRATION permission");
        }
        Objects.requireNonNull(inputDeviceDescriptor, "inputDeviceDescriptor must not be null");
        Objects.requireNonNull(calibration, "calibration must not be null");
        if (surfaceRotation < 0 || surfaceRotation > 3) {
            throw new IllegalArgumentException("surfaceRotation value out of bounds");
        }
        synchronized (this.mDataStore) {
            if (this.mDataStore.setTouchCalibration(inputDeviceDescriptor, surfaceRotation, calibration)) {
                this.mNative.reloadCalibration();
            }
            this.mDataStore.saveIfNeeded();
        }
    }

    public int isInTabletMode() {
        if (!checkCallingPermission("android.permission.TABLET_MODE", "isInTabletMode()")) {
            throw new SecurityException("Requires TABLET_MODE permission");
        }
        return getSwitchState(-1, -256, 1);
    }

    public int isMicMuted() {
        return getSwitchState(-1, -256, 14);
    }

    public void registerTabletModeChangedListener(ITabletModeChangedListener listener) {
        if (!checkCallingPermission("android.permission.TABLET_MODE", "registerTabletModeChangedListener()")) {
            throw new SecurityException("Requires TABLET_MODE_LISTENER permission");
        }
        Objects.requireNonNull(listener, "event must not be null");
        synchronized (this.mTabletModeLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mTabletModeChangedListeners.get(callingPid) != null) {
                throw new IllegalStateException("The calling process has already registered a TabletModeChangedListener.");
            }
            TabletModeChangedListenerRecord record = new TabletModeChangedListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mTabletModeChangedListeners.put(callingPid, record);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTabletModeChangedListenerDied(int pid) {
        synchronized (this.mTabletModeLock) {
            this.mTabletModeChangedListeners.remove(pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deliverTabletModeChanged(long whenNanos, boolean inTabletMode) {
        int numListeners;
        this.mTempTabletModeChangedListenersToNotify.clear();
        synchronized (this.mTabletModeLock) {
            numListeners = this.mTabletModeChangedListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mTempTabletModeChangedListenersToNotify.add(this.mTabletModeChangedListeners.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mTempTabletModeChangedListenersToNotify.get(i2).notifyTabletModeChanged(whenNanos, inTabletMode);
        }
    }

    private void showMissingKeyboardLayoutNotification(InputDevice device) {
        if (!this.mKeyboardLayoutNotificationShown) {
            Intent intent = new Intent("android.settings.HARD_KEYBOARD_SETTINGS");
            if (device != null) {
                intent.putExtra("input_device_identifier", (Parcelable) device.getIdentifier());
            }
            intent.setFlags(337641472);
            PendingIntent keyboardLayoutIntent = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 67108864, null, UserHandle.CURRENT);
            Resources r = this.mContext.getResources();
            Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.PHYSICAL_KEYBOARD).setContentTitle(r.getString(17041470)).setContentText(r.getString(17041469)).setContentIntent(keyboardLayoutIntent).setSmallIcon(17302873).setColor(this.mContext.getColor(17170460)).build();
            this.mNotificationManager.notifyAsUser(null, 19, notification, UserHandle.ALL);
            this.mKeyboardLayoutNotificationShown = true;
        }
    }

    private void hideMissingKeyboardLayoutNotification() {
        if (this.mKeyboardLayoutNotificationShown) {
            this.mKeyboardLayoutNotificationShown = false;
            this.mNotificationManager.cancelAsUser(null, 19, UserHandle.ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateKeyboardLayouts() {
        final HashSet<String> availableKeyboardLayouts = new HashSet<>();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda5
            @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
            public final void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                availableKeyboardLayouts.add(keyboardLayout.getDescriptor());
            }
        });
        synchronized (this.mDataStore) {
            this.mDataStore.removeUninstalledKeyboardLayouts(availableKeyboardLayouts);
            this.mDataStore.saveIfNeeded();
        }
        reloadKeyboardLayouts();
    }

    private static boolean containsInputDeviceWithDescriptor(InputDevice[] inputDevices, String descriptor) {
        for (InputDevice inputDevice : inputDevices) {
            if (inputDevice.getDescriptor().equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    public KeyboardLayout[] getKeyboardLayouts() {
        final ArrayList<KeyboardLayout> list = new ArrayList<>();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda3
            @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
            public final void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                list.add(keyboardLayout);
            }
        });
        return (KeyboardLayout[]) list.toArray(new KeyboardLayout[list.size()]);
    }

    public KeyboardLayout[] getKeyboardLayoutsForInputDevice(final InputDeviceIdentifier identifier) {
        final String[] enabledLayoutDescriptors = getEnabledKeyboardLayoutsForInputDevice(identifier);
        final ArrayList<KeyboardLayout> enabledLayouts = new ArrayList<>(enabledLayoutDescriptors.length);
        final ArrayList<KeyboardLayout> potentialLayouts = new ArrayList<>();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService.5
            boolean mHasSeenDeviceSpecificLayout;

            @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
            public void visitKeyboardLayout(Resources resources, int keyboardLayoutResId, KeyboardLayout layout) {
                String[] strArr;
                for (String s : enabledLayoutDescriptors) {
                    if (s != null && s.equals(layout.getDescriptor())) {
                        enabledLayouts.add(layout);
                        return;
                    }
                }
                if (layout.getVendorId() == identifier.getVendorId() && layout.getProductId() == identifier.getProductId()) {
                    if (!this.mHasSeenDeviceSpecificLayout) {
                        this.mHasSeenDeviceSpecificLayout = true;
                        potentialLayouts.clear();
                    }
                    potentialLayouts.add(layout);
                } else if (layout.getVendorId() == -1 && layout.getProductId() == -1 && !this.mHasSeenDeviceSpecificLayout) {
                    potentialLayouts.add(layout);
                }
            }
        });
        int enabledLayoutSize = enabledLayouts.size();
        int potentialLayoutSize = potentialLayouts.size();
        KeyboardLayout[] layouts = new KeyboardLayout[enabledLayoutSize + potentialLayoutSize];
        enabledLayouts.toArray(layouts);
        for (int i = 0; i < potentialLayoutSize; i++) {
            layouts[enabledLayoutSize + i] = potentialLayouts.get(i);
        }
        return layouts;
    }

    public KeyboardLayout getKeyboardLayout(String keyboardLayoutDescriptor) {
        Objects.requireNonNull(keyboardLayoutDescriptor, "keyboardLayoutDescriptor must not be null");
        final KeyboardLayout[] result = new KeyboardLayout[1];
        visitKeyboardLayout(keyboardLayoutDescriptor, new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda6
            @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
            public final void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                InputManagerService.lambda$getKeyboardLayout$4(result, resources, i, keyboardLayout);
            }
        });
        if (result[0] == null) {
            Slog.w(TAG, "Could not get keyboard layout with descriptor '" + keyboardLayoutDescriptor + "'.");
        }
        return result[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getKeyboardLayout$4(KeyboardLayout[] result, Resources resources, int keyboardLayoutResId, KeyboardLayout layout) {
        result[0] = layout;
    }

    private void visitAllKeyboardLayouts(KeyboardLayoutVisitor visitor) {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS");
        for (ResolveInfo resolveInfo : pm.queryBroadcastReceivers(intent, 786560)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            int priority = resolveInfo.priority;
            visitKeyboardLayoutsInPackage(pm, activityInfo, null, priority, visitor);
        }
    }

    private void visitKeyboardLayout(String keyboardLayoutDescriptor, KeyboardLayoutVisitor visitor) {
        KeyboardLayoutDescriptor d = KeyboardLayoutDescriptor.parse(keyboardLayoutDescriptor);
        if (d != null) {
            PackageManager pm = this.mContext.getPackageManager();
            try {
                ActivityInfo receiver = pm.getReceiverInfo(new ComponentName(d.packageName, d.receiverName), 786560);
                visitKeyboardLayoutsInPackage(pm, receiver, d.keyboardLayoutName, 0, visitor);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1599=4] */
    private void visitKeyboardLayoutsInPackage(PackageManager pm, ActivityInfo receiver, String keyboardName, int requestedPriority, KeyboardLayoutVisitor visitor) {
        XmlResourceParser parser;
        Throwable th;
        Bundle metaData;
        int configResId;
        Resources resources;
        int i;
        TypedArray a;
        Object obj = keyboardName;
        Bundle metaData2 = receiver.metaData;
        if (metaData2 == null) {
            return;
        }
        int configResId2 = metaData2.getInt("android.hardware.input.metadata.KEYBOARD_LAYOUTS");
        if (configResId2 == 0) {
            Slog.w(TAG, "Missing meta-data 'android.hardware.input.metadata.KEYBOARD_LAYOUTS' on receiver " + receiver.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiver.name);
            return;
        }
        CharSequence receiverLabel = receiver.loadLabel(pm);
        String collection = receiverLabel != null ? receiverLabel.toString() : "";
        int i2 = 1;
        int priority = (receiver.applicationInfo.flags & 1) != 0 ? requestedPriority : 0;
        try {
            Resources resources2 = pm.getResourcesForApplication(receiver.applicationInfo);
            XmlResourceParser parser2 = resources2.getXml(configResId2);
            try {
                XmlUtils.beginDocument(parser2, "keyboard-layouts");
                while (true) {
                    XmlUtils.nextElement(parser2);
                    String element = parser2.getName();
                    if (element == null) {
                        break;
                    }
                    if (element.equals("keyboard-layout")) {
                        TypedArray a2 = resources2.obtainAttributes(parser2, R.styleable.KeyboardLayout);
                        try {
                            String name = a2.getString(i2);
                            String label = a2.getString(0);
                            int keyboardLayoutResId = a2.getResourceId(2, 0);
                            String languageTags = a2.getString(3);
                            LocaleList locales = getLocalesFromLanguageTags(languageTags);
                            metaData = metaData2;
                            try {
                                int vid = a2.getInt(5, -1);
                                configResId = configResId2;
                                a = a2;
                                try {
                                    int pid = a.getInt(4, -1);
                                    try {
                                        try {
                                            if (name == null || label == null) {
                                                parser = parser2;
                                                resources = resources2;
                                                i = 1;
                                            } else if (keyboardLayoutResId == 0) {
                                                parser = parser2;
                                                resources = resources2;
                                                i = 1;
                                            } else {
                                                String descriptor = KeyboardLayoutDescriptor.format(receiver.packageName, receiver.name, name);
                                                try {
                                                    try {
                                                        if (obj != null) {
                                                            try {
                                                                if (!name.equals(obj)) {
                                                                    parser = parser2;
                                                                    resources = resources2;
                                                                    i = 1;
                                                                    a.recycle();
                                                                }
                                                            } catch (Throwable th2) {
                                                                th = th2;
                                                                parser = parser2;
                                                                a.recycle();
                                                                throw th;
                                                            }
                                                        }
                                                        KeyboardLayout layout = new KeyboardLayout(descriptor, label, collection, priority, locales, vid, pid);
                                                        visitor.visitKeyboardLayout(resources, keyboardLayoutResId, layout);
                                                        a.recycle();
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        a.recycle();
                                                        throw th;
                                                    }
                                                    parser = parser2;
                                                    resources = resources2;
                                                    i = 1;
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    parser = parser2;
                                                }
                                            }
                                            a.recycle();
                                        } catch (Throwable th5) {
                                            th = th5;
                                            if (parser != null) {
                                                try {
                                                    parser.close();
                                                }
                                            }
                                            throw th;
                                        }
                                        Slog.w(TAG, "Missing required 'name', 'label' or 'keyboardLayout' attributes in keyboard layout resource from receiver " + receiver.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiver.name);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        a.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    parser = parser2;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                parser = parser2;
                                a = a2;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            parser = parser2;
                            a = a2;
                        }
                    } else {
                        metaData = metaData2;
                        configResId = configResId2;
                        parser = parser2;
                        resources = resources2;
                        i = i2;
                        Slog.w(TAG, "Skipping unrecognized element '" + element + "' in keyboard layout resource from receiver " + receiver.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiver.name);
                    }
                    resources2 = resources;
                    i2 = i;
                    metaData2 = metaData;
                    configResId2 = configResId;
                    parser2 = parser;
                    obj = keyboardName;
                }
                if (parser2 != null) {
                    try {
                        parser2.close();
                    } catch (Exception e) {
                        ex = e;
                        Slog.w(TAG, "Could not parse keyboard layout resource from receiver " + receiver.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiver.name, ex);
                    }
                }
            } catch (Throwable th10) {
                parser = parser2;
                th = th10;
            }
        } catch (Exception e2) {
            ex = e2;
        }
    }

    private static LocaleList getLocalesFromLanguageTags(String languageTags) {
        if (TextUtils.isEmpty(languageTags)) {
            return LocaleList.getEmptyLocaleList();
        }
        return LocaleList.forLanguageTags(languageTags.replace('|', ','));
    }

    private String getLayoutDescriptor(InputDeviceIdentifier identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(identifier.getDescriptor(), "descriptor must not be null");
        if (identifier.getVendorId() == 0 && identifier.getProductId() == 0) {
            return identifier.getDescriptor();
        }
        return "vendor:" + identifier.getVendorId() + ",product:" + identifier.getProductId();
    }

    public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier) {
        String layout;
        String key = getLayoutDescriptor(identifier);
        synchronized (this.mDataStore) {
            layout = this.mDataStore.getCurrentKeyboardLayout(key);
            if (layout == null && !key.equals(identifier.getDescriptor())) {
                layout = this.mDataStore.getCurrentKeyboardLayout(identifier.getDescriptor());
            }
        }
        return layout;
    }

    public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "setCurrentKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        Objects.requireNonNull(keyboardLayoutDescriptor, "keyboardLayoutDescriptor must not be null");
        String key = getLayoutDescriptor(identifier);
        synchronized (this.mDataStore) {
            if (this.mDataStore.setCurrentKeyboardLayout(key, keyboardLayoutDescriptor)) {
                this.mHandler.sendEmptyMessage(3);
            }
            this.mDataStore.saveIfNeeded();
        }
    }

    public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) {
        String[] layouts;
        String key = getLayoutDescriptor(identifier);
        synchronized (this.mDataStore) {
            layouts = this.mDataStore.getKeyboardLayouts(key);
            if ((layouts == null || layouts.length == 0) && !key.equals(identifier.getDescriptor())) {
                layouts = this.mDataStore.getKeyboardLayouts(identifier.getDescriptor());
            }
        }
        return layouts;
    }

    public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "addKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        Objects.requireNonNull(keyboardLayoutDescriptor, "keyboardLayoutDescriptor must not be null");
        String key = getLayoutDescriptor(identifier);
        synchronized (this.mDataStore) {
            String oldLayout = this.mDataStore.getCurrentKeyboardLayout(key);
            if (oldLayout == null && !key.equals(identifier.getDescriptor())) {
                oldLayout = this.mDataStore.getCurrentKeyboardLayout(identifier.getDescriptor());
            }
            if (this.mDataStore.addKeyboardLayout(key, keyboardLayoutDescriptor) && !Objects.equals(oldLayout, this.mDataStore.getCurrentKeyboardLayout(key))) {
                this.mHandler.sendEmptyMessage(3);
            }
            this.mDataStore.saveIfNeeded();
        }
    }

    public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "removeKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        Objects.requireNonNull(keyboardLayoutDescriptor, "keyboardLayoutDescriptor must not be null");
        String key = getLayoutDescriptor(identifier);
        synchronized (this.mDataStore) {
            String oldLayout = this.mDataStore.getCurrentKeyboardLayout(key);
            if (oldLayout == null && !key.equals(identifier.getDescriptor())) {
                oldLayout = this.mDataStore.getCurrentKeyboardLayout(identifier.getDescriptor());
            }
            boolean removed = this.mDataStore.removeKeyboardLayout(key, keyboardLayoutDescriptor);
            if (!key.equals(identifier.getDescriptor())) {
                removed |= this.mDataStore.removeKeyboardLayout(identifier.getDescriptor(), keyboardLayoutDescriptor);
            }
            if (removed && !Objects.equals(oldLayout, this.mDataStore.getCurrentKeyboardLayout(key))) {
                this.mHandler.sendEmptyMessage(3);
            }
            this.mDataStore.saveIfNeeded();
        }
    }

    public void switchKeyboardLayout(int deviceId, int direction) {
        this.mHandler.obtainMessage(2, deviceId, direction).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSwitchKeyboardLayout(int deviceId, int direction) {
        boolean changed;
        String keyboardLayoutDescriptor;
        KeyboardLayout keyboardLayout;
        InputDevice device = getInputDevice(deviceId);
        if (device != null) {
            String key = getLayoutDescriptor(device.getIdentifier());
            synchronized (this.mDataStore) {
                changed = this.mDataStore.switchKeyboardLayout(key, direction);
                keyboardLayoutDescriptor = this.mDataStore.getCurrentKeyboardLayout(key);
                this.mDataStore.saveIfNeeded();
            }
            if (changed) {
                Toast toast = this.mSwitchedKeyboardLayoutToast;
                if (toast != null) {
                    toast.cancel();
                    this.mSwitchedKeyboardLayoutToast = null;
                }
                if (keyboardLayoutDescriptor != null && (keyboardLayout = getKeyboardLayout(keyboardLayoutDescriptor)) != null) {
                    Toast makeText = Toast.makeText(this.mContext, keyboardLayout.getLabel(), 0);
                    this.mSwitchedKeyboardLayoutToast = makeText;
                    makeText.show();
                }
                reloadKeyboardLayouts();
            }
        }
    }

    public void setFocusedApplication(int displayId, InputApplicationHandle application) {
        this.mNative.setFocusedApplication(displayId, application);
    }

    public void setFocusedDisplay(int displayId) {
        this.mNative.setFocusedDisplay(displayId);
    }

    public void onDisplayRemoved(int displayId) {
        Context context = this.mPointerIconDisplayContext;
        if (context != null && context.getDisplay().getDisplayId() == displayId) {
            this.mPointerIconDisplayContext = null;
        }
        updateAdditionalDisplayInputProperties(displayId, new Consumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((InputManagerService.AdditionalDisplayInputProperties) obj).reset();
            }
        });
        this.mNative.displayRemoved(displayId);
    }

    public void requestPointerCapture(IBinder inputChannelToken, boolean enabled) {
        Objects.requireNonNull(inputChannelToken, "event must not be null");
        this.mNative.requestPointerCapture(inputChannelToken, enabled);
    }

    public void setInputDispatchMode(boolean enabled, boolean frozen) {
        this.mNative.setInputDispatchMode(enabled, frozen);
    }

    public void setSystemUiLightsOut(boolean lightsOut) {
        this.mNative.setSystemUiLightsOut(lightsOut);
    }

    public boolean transferTouchFocus(InputChannel fromChannel, InputChannel toChannel, boolean isDragDrop) {
        return this.mNative.transferTouchFocus(fromChannel.getToken(), toChannel.getToken(), isDragDrop);
    }

    public boolean transferTouchFocus(IBinder fromChannelToken, IBinder toChannelToken) {
        Objects.nonNull(fromChannelToken);
        Objects.nonNull(toChannelToken);
        return this.mNative.transferTouchFocus(fromChannelToken, toChannelToken, false);
    }

    public void tryPointerSpeed(int speed) {
        if (!checkCallingPermission("android.permission.SET_POINTER_SPEED", "tryPointerSpeed()")) {
            throw new SecurityException("Requires SET_POINTER_SPEED permission");
        }
        if (speed < -7 || speed > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        setPointerSpeedUnchecked(speed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePointerSpeedFromSettings() {
        int speed = getPointerSpeedSetting();
        setPointerSpeedUnchecked(speed);
    }

    private void setPointerSpeedUnchecked(int speed) {
        this.mNative.setPointerSpeed(Math.min(Math.max(speed, -7), 7));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPointerAcceleration(final float acceleration, int displayId) {
        updateAdditionalDisplayInputProperties(displayId, new Consumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((InputManagerService.AdditionalDisplayInputProperties) obj).pointerAcceleration = acceleration;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPointerIconVisible(final boolean visible, int displayId) {
        updateAdditionalDisplayInputProperties(displayId, new Consumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((InputManagerService.AdditionalDisplayInputProperties) obj).pointerIconVisible = visible;
            }
        });
    }

    private void registerPointerSpeedSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("pointer_speed"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updatePointerSpeedFromSettings();
            }
        }, -1);
    }

    private int getPointerSpeedSetting() {
        try {
            int speed = Settings.System.getIntForUser(this.mContext.getContentResolver(), "pointer_speed", -2);
            return speed;
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateShowTouchesFromSettings() {
        int setting = getShowTouchesSetting(0);
        this.mNative.setShowTouches(setting != 0);
    }

    private void registerShowTouchesSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("show_touches"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updateShowTouchesFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAccessibilityLargePointerFromSettings() {
        int accessibilityConfig = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_large_pointer_icon", 0, -2);
        PointerIcon.setUseLargeIcons(accessibilityConfig == 1);
        this.mNative.reloadPointerIcons();
    }

    private void registerAccessibilityLargePointerSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_large_pointer_icon"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updateAccessibilityLargePointerFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeepPressStatusFromSettings(String reason) {
        int timeout = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "long_press_timeout", 400, -2);
        boolean z = true;
        boolean featureEnabledFlag = DeviceConfig.getBoolean("input_native_boot", DEEP_PRESS_ENABLED, true);
        boolean enabled = (!featureEnabledFlag || timeout > 400) ? false : false;
        Log.i(TAG, (enabled ? "Enabling" : "Disabling") + " motion classifier because " + reason + ": feature " + (featureEnabledFlag ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED) + ", long press timeout = " + timeout);
        this.mNative.setMotionClassifierEnabled(enabled);
    }

    private void registerLongPressTimeoutObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("long_press_timeout"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updateDeepPressStatusFromSettings("timeout changed");
            }
        }, -1);
    }

    private void registerBlockUntrustedTouchesModeSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("block_untrusted_touches"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.10
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updateBlockUntrustedTouchesModeFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBlockUntrustedTouchesModeFromSettings() {
        int mode = InputManager.getInstance().getBlockUntrustedTouchesMode(this.mContext);
        this.mNative.setBlockUntrustedTouchesMode(mode);
    }

    private void registerMaximumObscuringOpacityForTouchSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("maximum_obscuring_opacity_for_touch"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerService.11
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerService.this.updateMaximumObscuringOpacityForTouchFromSettings();
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaximumObscuringOpacityForTouchFromSettings() {
        float opacity = InputManager.getInstance().getMaximumObscuringOpacityForTouch();
        if (opacity < 0.0f || opacity > 1.0f) {
            Log.e(TAG, "Invalid maximum obscuring opacity " + opacity + ", it should be >= 0 and <= 1, rejecting update.");
        } else {
            this.mNative.setMaximumObscuringOpacityForTouch(opacity);
        }
    }

    private int getShowTouchesSetting(int defaultValue) {
        try {
            int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), "show_touches", -2);
            return result;
        } catch (Settings.SettingNotFoundException e) {
            return defaultValue;
        }
    }

    private boolean updatePointerDisplayIdLocked(int pointerDisplayId) {
        if (this.mRequestedPointerDisplayId == pointerDisplayId) {
            return false;
        }
        this.mRequestedPointerDisplayId = pointerDisplayId;
        this.mNative.setPointerDisplayId(pointerDisplayId);
        applyAdditionalDisplayInputProperties();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePointerDisplayIdChanged(PointerDisplayIdChangedArgs args) {
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            this.mAcknowledgedPointerDisplayId = args.mPointerDisplayId;
            this.mAdditionalDisplayInputPropertiesLock.notifyAll();
        }
        this.mWindowManagerCallbacks.notifyPointerDisplayIdChanged(args.mPointerDisplayId, args.mXPosition, args.mYPosition);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setVirtualMousePointerDisplayIdBlocking(int overrideDisplayId) {
        int resolvedDisplayId;
        boolean z = false;
        boolean isRemovingOverride = overrideDisplayId == -1;
        if (isRemovingOverride) {
            resolvedDisplayId = this.mWindowManagerCallbacks.getPointerDisplayId();
        } else {
            resolvedDisplayId = overrideDisplayId;
        }
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            this.mOverriddenPointerDisplayId = overrideDisplayId;
            if (updatePointerDisplayIdLocked(resolvedDisplayId) || this.mAcknowledgedPointerDisplayId != resolvedDisplayId) {
                if (isRemovingOverride && this.mAcknowledgedPointerDisplayId == -1) {
                    return true;
                }
                try {
                    this.mAdditionalDisplayInputPropertiesLock.wait(5000L);
                } catch (InterruptedException e) {
                }
                if (isRemovingOverride || this.mAcknowledgedPointerDisplayId == overrideDisplayId) {
                    z = true;
                }
                return z;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getVirtualMousePointerDisplayId() {
        int i;
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            i = this.mOverriddenPointerDisplayId;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDisplayEligibilityForPointerCapture(int displayId, boolean isEligible) {
        this.mNative.setDisplayEligibilityForPointerCapture(displayId, isEligible);
    }

    /* loaded from: classes.dex */
    private static class VibrationInfo {
        private final int[] mAmplitudes;
        private final long[] mPattern;
        private final int mRepeat;

        public long[] getPattern() {
            return this.mPattern;
        }

        public int[] getAmplitudes() {
            return this.mAmplitudes;
        }

        public int getRepeatIndex() {
            return this.mRepeat;
        }

        VibrationInfo(VibrationEffect effect) {
            long[] pattern = null;
            int[] amplitudes = null;
            int patternRepeatIndex = -1;
            int amplitudeCount = -1;
            if (effect instanceof VibrationEffect.Composed) {
                VibrationEffect.Composed composed = (VibrationEffect.Composed) effect;
                int segmentCount = composed.getSegments().size();
                pattern = new long[segmentCount];
                amplitudes = new int[segmentCount];
                patternRepeatIndex = composed.getRepeatIndex();
                amplitudeCount = 0;
                int i = 0;
                while (true) {
                    if (i >= segmentCount) {
                        break;
                    }
                    StepSegment stepSegment = (VibrationEffectSegment) composed.getSegments().get(i);
                    patternRepeatIndex = composed.getRepeatIndex() == i ? amplitudeCount : patternRepeatIndex;
                    if (!(stepSegment instanceof StepSegment)) {
                        Slog.w(InputManagerService.TAG, "Input devices don't support segment " + stepSegment);
                        amplitudeCount = -1;
                        break;
                    }
                    float amplitude = stepSegment.getAmplitude();
                    if (Float.compare(amplitude, -1.0f) == 0) {
                        amplitudes[amplitudeCount] = 192;
                    } else {
                        amplitudes[amplitudeCount] = (int) (255.0f * amplitude);
                    }
                    pattern[amplitudeCount] = stepSegment.getDuration();
                    i++;
                    amplitudeCount++;
                }
            }
            if (amplitudeCount < 0) {
                Slog.w(InputManagerService.TAG, "Only oneshot and step waveforms are supported on input devices");
                this.mPattern = new long[0];
                this.mAmplitudes = new int[0];
                this.mRepeat = -1;
                return;
            }
            this.mRepeat = patternRepeatIndex;
            long[] jArr = new long[amplitudeCount];
            this.mPattern = jArr;
            int[] iArr = new int[amplitudeCount];
            this.mAmplitudes = iArr;
            System.arraycopy(pattern, 0, jArr, 0, amplitudeCount);
            System.arraycopy(amplitudes, 0, iArr, 0, amplitudeCount);
            if (patternRepeatIndex >= jArr.length) {
                throw new ArrayIndexOutOfBoundsException("Repeat index " + patternRepeatIndex + " must be within the bounds of the pattern.length " + jArr.length);
            }
        }
    }

    private VibratorToken getVibratorToken(int deviceId, IBinder token) {
        VibratorToken v;
        synchronized (this.mVibratorLock) {
            v = this.mVibratorTokens.get(token);
            if (v == null) {
                int i = this.mNextVibratorTokenValue;
                this.mNextVibratorTokenValue = i + 1;
                v = new VibratorToken(deviceId, token, i);
                try {
                    token.linkToDeath(v, 0);
                    this.mVibratorTokens.put(token, v);
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return v;
    }

    public void vibrate(int deviceId, VibrationEffect effect, IBinder token) {
        VibrationInfo info = new VibrationInfo(effect);
        VibratorToken v = getVibratorToken(deviceId, token);
        synchronized (v) {
            v.mVibrating = true;
            this.mNative.vibrate(deviceId, info.getPattern(), info.getAmplitudes(), info.getRepeatIndex(), v.mTokenValue);
        }
    }

    public int[] getVibratorIds(int deviceId) {
        return this.mNative.getVibratorIds(deviceId);
    }

    public boolean isVibrating(int deviceId) {
        return this.mNative.isVibrating(deviceId);
    }

    public void vibrateCombined(int deviceId, CombinedVibration effect, IBinder token) {
        VibratorToken v = getVibratorToken(deviceId, token);
        synchronized (v) {
            if (!(effect instanceof CombinedVibration.Mono) && !(effect instanceof CombinedVibration.Stereo)) {
                Slog.e(TAG, "Only Mono and Stereo effects are supported");
                return;
            }
            v.mVibrating = true;
            if (effect instanceof CombinedVibration.Mono) {
                CombinedVibration.Mono mono = (CombinedVibration.Mono) effect;
                VibrationInfo info = new VibrationInfo(mono.getEffect());
                this.mNative.vibrate(deviceId, info.getPattern(), info.getAmplitudes(), info.getRepeatIndex(), v.mTokenValue);
            } else if (effect instanceof CombinedVibration.Stereo) {
                CombinedVibration.Stereo stereo = (CombinedVibration.Stereo) effect;
                SparseArray<VibrationEffect> effects = stereo.getEffects();
                long[] pattern = new long[0];
                SparseArray<int[]> amplitudes = new SparseArray<>(effects.size());
                long[] pattern2 = pattern;
                int repeat = Integer.MIN_VALUE;
                for (int i = 0; i < effects.size(); i++) {
                    VibrationInfo info2 = new VibrationInfo(effects.valueAt(i));
                    if (pattern2.length == 0) {
                        pattern2 = info2.getPattern();
                    }
                    if (repeat == Integer.MIN_VALUE) {
                        repeat = info2.getRepeatIndex();
                    }
                    amplitudes.put(effects.keyAt(i), info2.getAmplitudes());
                }
                this.mNative.vibrateCombined(deviceId, pattern2, amplitudes, repeat, v.mTokenValue);
            }
        }
    }

    public void cancelVibrate(int deviceId, IBinder token) {
        synchronized (this.mVibratorLock) {
            VibratorToken v = this.mVibratorTokens.get(token);
            if (v != null && v.mDeviceId == deviceId) {
                cancelVibrateIfNeeded(v);
            }
        }
    }

    void onVibratorTokenDied(VibratorToken v) {
        synchronized (this.mVibratorLock) {
            this.mVibratorTokens.remove(v.mToken);
        }
        cancelVibrateIfNeeded(v);
    }

    private void cancelVibrateIfNeeded(VibratorToken v) {
        synchronized (v) {
            if (v.mVibrating) {
                this.mNative.cancelVibrate(v.mDeviceId, v.mTokenValue);
                v.mVibrating = false;
            }
        }
    }

    private void notifyVibratorState(int deviceId, boolean isOn) {
        synchronized (this.mVibratorLock) {
            this.mIsVibrating.put(deviceId, isOn);
            notifyVibratorStateListenersLocked(deviceId);
        }
    }

    private void notifyVibratorStateListenersLocked(int deviceId) {
        if (!this.mVibratorStateListeners.contains(deviceId)) {
            return;
        }
        RemoteCallbackList<IVibratorStateListener> listeners = this.mVibratorStateListeners.get(deviceId);
        int length = listeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                notifyVibratorStateListenerLocked(deviceId, listeners.getBroadcastItem(i));
            } finally {
                listeners.finishBroadcast();
            }
        }
    }

    private void notifyVibratorStateListenerLocked(int deviceId, IVibratorStateListener listener) {
        try {
            listener.onVibrating(this.mIsVibrating.get(deviceId));
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Vibrator state listener failed to call", e);
        }
    }

    public boolean registerVibratorStateListener(int deviceId, IVibratorStateListener listener) {
        RemoteCallbackList<IVibratorStateListener> listeners;
        Objects.requireNonNull(listener, "listener must not be null");
        synchronized (this.mVibratorLock) {
            if (!this.mVibratorStateListeners.contains(deviceId)) {
                listeners = new RemoteCallbackList<>();
                this.mVibratorStateListeners.put(deviceId, listeners);
            } else {
                listeners = this.mVibratorStateListeners.get(deviceId);
            }
            long token = Binder.clearCallingIdentity();
            if (!listeners.register(listener)) {
                Slog.e(TAG, "Could not register vibrator state listener " + listener);
                Binder.restoreCallingIdentity(token);
                return false;
            }
            notifyVibratorStateListenerLocked(deviceId, listener);
            Binder.restoreCallingIdentity(token);
            return true;
        }
    }

    public boolean unregisterVibratorStateListener(int deviceId, IVibratorStateListener listener) {
        synchronized (this.mVibratorLock) {
            long token = Binder.clearCallingIdentity();
            if (!this.mVibratorStateListeners.contains(deviceId)) {
                Slog.w(TAG, "Vibrator state listener " + deviceId + " doesn't exist");
                Binder.restoreCallingIdentity(token);
                return false;
            }
            RemoteCallbackList<IVibratorStateListener> listeners = this.mVibratorStateListeners.get(deviceId);
            boolean unregister = listeners.unregister(listener);
            Binder.restoreCallingIdentity(token);
            return unregister;
        }
    }

    public int getBatteryStatus(int deviceId) {
        return this.mNative.getBatteryStatus(deviceId);
    }

    public int getBatteryCapacity(int deviceId) {
        return this.mNative.getBatteryCapacity(deviceId);
    }

    public void setPointerIconType(int iconType) {
        if (iconType == -1) {
            throw new IllegalArgumentException("Use setCustomPointerIcon to set custom pointers");
        }
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            this.mIcon = null;
            this.mIconType = iconType;
            if (this.mCurrentDisplayProperties.pointerIconVisible) {
                this.mNative.setPointerIconType(this.mIconType);
            }
        }
    }

    public void setCustomPointerIcon(PointerIcon icon) {
        Objects.requireNonNull(icon);
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            this.mIconType = -1;
            this.mIcon = icon;
            if (this.mCurrentDisplayProperties.pointerIconVisible) {
                this.mNative.setCustomPointerIcon(this.mIcon);
            }
        }
    }

    public void addPortAssociation(String inputPort, int displayPort) {
        if (!checkCallingPermission("android.permission.ASSOCIATE_INPUT_DEVICE_TO_DISPLAY", "addPortAssociation()")) {
            throw new SecurityException("Requires ASSOCIATE_INPUT_DEVICE_TO_DISPLAY permission");
        }
        Objects.requireNonNull(inputPort);
        synchronized (this.mAssociationsLock) {
            this.mRuntimeAssociations.put(inputPort, Integer.valueOf(displayPort));
        }
        this.mNative.notifyPortAssociationsChanged();
    }

    public void removePortAssociation(String inputPort) {
        if (!checkCallingPermission("android.permission.ASSOCIATE_INPUT_DEVICE_TO_DISPLAY", "clearPortAssociations()")) {
            throw new SecurityException("Requires ASSOCIATE_INPUT_DEVICE_TO_DISPLAY permission");
        }
        Objects.requireNonNull(inputPort);
        synchronized (this.mAssociationsLock) {
            this.mRuntimeAssociations.remove(inputPort);
        }
        this.mNative.notifyPortAssociationsChanged();
    }

    public void addUniqueIdAssociation(String inputPort, String displayUniqueId) {
        if (!checkCallingPermission("android.permission.ASSOCIATE_INPUT_DEVICE_TO_DISPLAY", "addNameAssociation()")) {
            throw new SecurityException("Requires ASSOCIATE_INPUT_DEVICE_TO_DISPLAY permission");
        }
        Objects.requireNonNull(inputPort);
        Objects.requireNonNull(displayUniqueId);
        synchronized (this.mAssociationsLock) {
            this.mUniqueIdAssociations.put(inputPort, displayUniqueId);
        }
        this.mNative.changeUniqueIdAssociation();
    }

    public void removeUniqueIdAssociation(String inputPort) {
        if (!checkCallingPermission("android.permission.ASSOCIATE_INPUT_DEVICE_TO_DISPLAY", "removeUniqueIdAssociation()")) {
            throw new SecurityException("Requires ASSOCIATE_INPUT_DEVICE_TO_DISPLAY permission");
        }
        Objects.requireNonNull(inputPort);
        synchronized (this.mAssociationsLock) {
            this.mUniqueIdAssociations.remove(inputPort);
        }
        this.mNative.changeUniqueIdAssociation();
    }

    public InputSensorInfo[] getSensorList(int deviceId) {
        return this.mNative.getSensorList(deviceId);
    }

    public boolean registerSensorListener(IInputSensorEventListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        synchronized (this.mSensorEventLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mSensorEventListeners.get(callingPid) != null) {
                Slog.e(TAG, "The calling process " + callingPid + " has already registered an InputSensorEventListener.");
                return false;
            }
            SensorEventListenerRecord record = new SensorEventListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mSensorEventListeners.put(callingPid, record);
                return true;
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void unregisterSensorListener(IInputSensorEventListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        synchronized (this.mSensorEventLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mSensorEventListeners.get(callingPid) != null) {
                SensorEventListenerRecord record = this.mSensorEventListeners.get(callingPid);
                if (record.getListener().asBinder() != listener.asBinder()) {
                    throw new IllegalArgumentException("listener is not registered");
                }
                this.mSensorEventListeners.remove(callingPid);
            }
        }
    }

    public boolean flushSensor(int deviceId, int sensorType) {
        synchronized (this.mSensorEventLock) {
            int callingPid = Binder.getCallingPid();
            SensorEventListenerRecord listener = this.mSensorEventListeners.get(callingPid);
            if (listener != null) {
                return this.mNative.flushSensor(deviceId, sensorType);
            }
            return false;
        }
    }

    public boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs, int maxBatchReportLatencyUs) {
        boolean enableSensor;
        synchronized (this.mInputDevicesLock) {
            enableSensor = this.mNative.enableSensor(deviceId, sensorType, samplingPeriodUs, maxBatchReportLatencyUs);
        }
        return enableSensor;
    }

    public void disableSensor(int deviceId, int sensorType) {
        synchronized (this.mInputDevicesLock) {
            this.mNative.disableSensor(deviceId, sensorType);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LightSession implements IBinder.DeathRecipient {
        private final int mDeviceId;
        private int[] mLightIds;
        private LightState[] mLightStates;
        private final String mOpPkg;
        private final IBinder mToken;

        LightSession(int deviceId, String opPkg, IBinder token) {
            this.mDeviceId = deviceId;
            this.mOpPkg = opPkg;
            this.mToken = token;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (InputManagerService.this.mLightLock) {
                InputManagerService.this.closeLightSession(this.mDeviceId, this.mToken);
                InputManagerService.this.mLightSessions.remove(this.mToken);
            }
        }
    }

    public List<Light> getLights(int deviceId) {
        return this.mNative.getLights(deviceId);
    }

    private void setLightStateInternal(int deviceId, Light light, LightState lightState) {
        Objects.requireNonNull(light, "light does not exist");
        if (light.getType() == 10002) {
            this.mNative.setLightPlayerId(deviceId, light.getId(), lightState.getPlayerId());
        } else {
            this.mNative.setLightColor(deviceId, light.getId(), lightState.getColor());
        }
    }

    private void setLightStatesInternal(int deviceId, int[] lightIds, LightState[] lightStates) {
        List<Light> lights = this.mNative.getLights(deviceId);
        SparseArray<Light> lightArray = new SparseArray<>();
        for (int i = 0; i < lights.size(); i++) {
            lightArray.put(lights.get(i).getId(), lights.get(i));
        }
        for (int i2 = 0; i2 < lightIds.length; i2++) {
            if (lightArray.contains(lightIds[i2])) {
                setLightStateInternal(deviceId, lightArray.get(lightIds[i2]), lightStates[i2]);
            }
        }
    }

    public void setLightStates(int deviceId, int[] lightIds, LightState[] lightStates, IBinder token) {
        boolean z = true;
        Preconditions.checkArgument(lightIds.length == lightStates.length, "lights and light states are not same length");
        synchronized (this.mLightLock) {
            LightSession lightSession = this.mLightSessions.get(token);
            Preconditions.checkArgument(lightSession != null, "not registered");
            if (lightSession.mDeviceId != deviceId) {
                z = false;
            }
            Preconditions.checkState(z, "Incorrect device ID");
            lightSession.mLightIds = (int[]) lightIds.clone();
            lightSession.mLightStates = (LightState[]) lightStates.clone();
        }
        setLightStatesInternal(deviceId, lightIds, lightStates);
    }

    public LightState getLightState(int deviceId, int lightId) {
        LightState lightState;
        synchronized (this.mLightLock) {
            int color = this.mNative.getLightColor(deviceId, lightId);
            int playerId = this.mNative.getLightPlayerId(deviceId, lightId);
            lightState = new LightState(color, playerId);
        }
        return lightState;
    }

    public void openLightSession(int deviceId, String opPkg, IBinder token) {
        Objects.requireNonNull(token);
        synchronized (this.mLightLock) {
            Preconditions.checkState(this.mLightSessions.get(token) == null, "already registered");
            LightSession lightSession = new LightSession(deviceId, opPkg, token);
            try {
                token.linkToDeath(lightSession, 0);
            } catch (RemoteException ex) {
                ex.rethrowAsRuntimeException();
            }
            this.mLightSessions.put(token, lightSession);
        }
    }

    public void closeLightSession(int deviceId, IBinder token) {
        Objects.requireNonNull(token);
        synchronized (this.mLightLock) {
            LightSession lightSession = this.mLightSessions.get(token);
            Preconditions.checkState(lightSession != null, "not registered");
            Arrays.fill(lightSession.mLightStates, new LightState(0));
            setLightStatesInternal(deviceId, lightSession.mLightIds, lightSession.mLightStates);
            this.mLightSessions.remove(token);
            if (!this.mLightSessions.isEmpty()) {
                LightSession nextSession = this.mLightSessions.valueAt(0);
                setLightStatesInternal(deviceId, nextSession.mLightIds, nextSession.mLightStates);
            }
        }
    }

    public void cancelCurrentTouch() {
        if (!checkCallingPermission("android.permission.MONITOR_INPUT", "cancelCurrentTouch()")) {
            throw new SecurityException("Requires MONITOR_INPUT permission");
        }
        this.mNative.cancelCurrentTouch();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("INPUT MANAGER (dumpsys input)\n");
            String dumpStr = this.mNative.dump();
            if (dumpStr != null) {
                pw.println(dumpStr);
            }
            pw.println("Input Manager Service (Java) State:");
            dumpAssociations(pw, "  ");
            dumpSpyWindowGestureMonitors(pw, "  ");
            dumpDisplayInputPropertiesValues(pw, "  ");
        }
    }

    private void dumpAssociations(final PrintWriter pw, final String prefix) {
        if (!this.mStaticAssociations.isEmpty()) {
            pw.println(prefix + "Static Associations:");
            this.mStaticAssociations.forEach(new BiConsumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda9
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    InputManagerService.lambda$dumpAssociations$7(pw, prefix, (String) obj, (Integer) obj2);
                }
            });
        }
        synchronized (this.mAssociationsLock) {
            if (!this.mRuntimeAssociations.isEmpty()) {
                pw.println(prefix + "Runtime Associations:");
                this.mRuntimeAssociations.forEach(new BiConsumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda10
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        InputManagerService.lambda$dumpAssociations$8(pw, prefix, (String) obj, (Integer) obj2);
                    }
                });
            }
            if (!this.mUniqueIdAssociations.isEmpty()) {
                pw.println(prefix + "Unique Id Associations:");
                this.mUniqueIdAssociations.forEach(new BiConsumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda11
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        InputManagerService.lambda$dumpAssociations$9(pw, prefix, (String) obj, (String) obj2);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpAssociations$7(PrintWriter pw, String prefix, String k, Integer v) {
        pw.print(prefix + "  port: " + k);
        pw.println("  display: " + v);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpAssociations$8(PrintWriter pw, String prefix, String k, Integer v) {
        pw.print(prefix + "  port: " + k);
        pw.println("  display: " + v);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpAssociations$9(PrintWriter pw, String prefix, String k, String v) {
        pw.print(prefix + "  port: " + k);
        pw.println("  uniqueId: " + v);
    }

    private void dumpSpyWindowGestureMonitors(PrintWriter pw, String prefix) {
        synchronized (this.mInputMonitors) {
            if (this.mInputMonitors.isEmpty()) {
                return;
            }
            pw.println(prefix + "Gesture Monitors (implemented as spy windows):");
            int i = 0;
            for (GestureMonitorSpyWindow monitor : this.mInputMonitors.values()) {
                pw.append((CharSequence) (prefix + "  " + i + ": ")).println(monitor.dump());
                i++;
            }
        }
    }

    private void dumpDisplayInputPropertiesValues(PrintWriter pw, String prefix) {
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            if (this.mAdditionalDisplayInputProperties.size() != 0) {
                pw.println(prefix + "mAdditionalDisplayInputProperties:");
                for (int i = 0; i < this.mAdditionalDisplayInputProperties.size(); i++) {
                    pw.println(prefix + "  displayId: " + this.mAdditionalDisplayInputProperties.keyAt(i));
                    AdditionalDisplayInputProperties properties = this.mAdditionalDisplayInputProperties.valueAt(i);
                    pw.println(prefix + "  pointerAcceleration: " + properties.pointerAcceleration);
                    pw.println(prefix + "  pointerIconVisible: " + properties.pointerIconVisible);
                }
            }
            int i2 = this.mOverriddenPointerDisplayId;
            if (i2 != -1) {
                pw.println(prefix + "mOverriddenPointerDisplayId: " + this.mOverriddenPointerDisplayId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkCallingPermission(String permission, String func) {
        return checkCallingPermission(permission, func, false);
    }

    private boolean checkCallingPermission(String permission, String func, boolean checkInstrumentationSource) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        if (checkInstrumentationSource) {
            ActivityManagerInternal ami = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            Objects.requireNonNull(ami, "ActivityManagerInternal should not be null.");
            int instrumentationUid = ami.getInstrumentationSourceUid(Binder.getCallingUid());
            if (instrumentationUid != -1) {
                long token = Binder.clearCallingIdentity();
                try {
                    if (this.mContext.checkPermission(permission, -1, instrumentationUid) == 0) {
                        return true;
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
        Slog.w(TAG, msg);
        return false;
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mInputFilterLock) {
        }
        synchronized (this.mAssociationsLock) {
        }
        synchronized (this.mLidSwitchLock) {
        }
        synchronized (this.mInputMonitors) {
        }
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
        }
        this.mNative.monitor();
    }

    private void notifyConfigurationChanged(long whenNanos) {
        this.mWindowManagerCallbacks.notifyConfigurationChanged();
    }

    private void notifyInputDevicesChanged(InputDevice[] inputDevices) {
        synchronized (this.mInputDevicesLock) {
            if (!this.mInputDevicesChangedPending) {
                this.mInputDevicesChangedPending = true;
                this.mHandler.obtainMessage(1, this.mInputDevices).sendToTarget();
            }
            this.mInputDevices = inputDevices;
        }
    }

    private void notifySwitch(long whenNanos, int switchValues, int switchMask) {
        if ((switchMask & 1) != 0) {
            boolean lidOpen = (switchValues & 1) == 0;
            synchronized (this.mLidSwitchLock) {
                if (this.mSystemReady) {
                    for (int i = 0; i < this.mLidSwitchCallbacks.size(); i++) {
                        InputManagerInternal.LidSwitchCallback callbacks = this.mLidSwitchCallbacks.get(i);
                        callbacks.notifyLidSwitchChanged(whenNanos, lidOpen);
                    }
                }
            }
        }
        if ((switchMask & 512) != 0) {
            boolean lensCovered = (switchValues & 512) != 0;
            this.mWindowManagerCallbacks.notifyCameraLensCoverSwitchChanged(whenNanos, lensCovered);
            setSensorPrivacy(2, lensCovered);
        }
        if (this.mUseDevInputEventForAudioJack && (switchMask & 212) != 0) {
            this.mWiredAccessoryCallbacks.notifyWiredAccessoryChanged(whenNanos, switchValues, switchMask);
        }
        if ((switchMask & 2) != 0) {
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = (int) ((-1) & whenNanos);
            args.argi2 = (int) (whenNanos >> 32);
            args.arg1 = Boolean.valueOf((switchValues & 2) != 0);
            this.mHandler.obtainMessage(6, args).sendToTarget();
        }
        if ((switchMask & 16384) != 0) {
            boolean micMute = (switchValues & 16384) != 0;
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class);
            audioManager.setMicrophoneMuteFromSwitch(micMute);
            setSensorPrivacy(1, micMute);
        }
    }

    private void setSensorPrivacy(int sensor, boolean enablePrivacy) {
        SensorPrivacyManagerInternal sensorPrivacyManagerInternal = (SensorPrivacyManagerInternal) LocalServices.getService(SensorPrivacyManagerInternal.class);
        sensorPrivacyManagerInternal.setPhysicalToggleSensorPrivacy(-2, sensor, enablePrivacy);
    }

    private void notifyInputChannelBroken(IBinder token) {
        synchronized (this.mInputMonitors) {
            if (this.mInputMonitors.containsKey(token)) {
                removeSpyWindowGestureMonitor(token);
            }
        }
        this.mWindowManagerCallbacks.notifyInputChannelBroken(token);
    }

    private void notifyFocusChanged(IBinder oldToken, IBinder newToken) {
        this.mWindowManagerCallbacks.notifyFocusChanged(oldToken, newToken);
    }

    private void notifyDropWindow(IBinder token, float x, float y) {
        this.mWindowManagerCallbacks.notifyDropWindow(token, x, y);
    }

    private void notifyUntrustedTouch(String packageName) {
        Log.i(TAG, "Suppressing untrusted touch toast for " + packageName);
    }

    private /* synthetic */ void lambda$notifyUntrustedTouch$10(String packageName) {
        Toast.makeText(this.mContext, "Touch obscured by " + packageName + " will be blocked. Check go/untrusted-touches", 0).show();
    }

    private void notifyNoFocusedWindowAnr(InputApplicationHandle inputApplicationHandle) {
        this.mWindowManagerCallbacks.notifyNoFocusedWindowAnr(inputApplicationHandle);
    }

    private void notifyWindowUnresponsive(IBinder token, int pid, boolean isPidValid, String reason) {
        this.mWindowManagerCallbacks.notifyWindowUnresponsive(token, isPidValid ? OptionalInt.of(pid) : OptionalInt.empty(), reason);
    }

    private void notifyWindowResponsive(IBinder token, int pid, boolean isPidValid) {
        this.mWindowManagerCallbacks.notifyWindowResponsive(token, isPidValid ? OptionalInt.of(pid) : OptionalInt.empty());
    }

    private void notifySensorEvent(int deviceId, int sensorType, int accuracy, long timestamp, float[] values) {
        int numListeners;
        this.mSensorEventListenersToNotify.clear();
        synchronized (this.mSensorEventLock) {
            numListeners = this.mSensorEventListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mSensorEventListenersToNotify.add(this.mSensorEventListeners.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mSensorEventListenersToNotify.get(i2).notifySensorEvent(deviceId, sensorType, accuracy, timestamp, values);
        }
        this.mSensorEventListenersToNotify.clear();
    }

    private void notifySensorAccuracy(int deviceId, int sensorType, int accuracy) {
        int numListeners;
        this.mSensorAccuracyListenersToNotify.clear();
        synchronized (this.mSensorEventLock) {
            numListeners = this.mSensorEventListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mSensorAccuracyListenersToNotify.add(this.mSensorEventListeners.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mSensorAccuracyListenersToNotify.get(i2).notifySensorAccuracy(deviceId, sensorType, accuracy);
        }
        this.mSensorAccuracyListenersToNotify.clear();
    }

    private String getInstalledPackageName(String packageName, PackageManager pm) {
        try {
            try {
                InstallSourceInfo sourceInfo = pm.getInstallSourceInfo(packageName);
                if (sourceInfo == null) {
                    return UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
                }
                if (sourceInfo.getInitiatingPackageName() != null) {
                    String insPkgName = sourceInfo.getInitiatingPackageName();
                    return insPkgName;
                } else if (sourceInfo.getInstallingPackageName() != null) {
                    String insPkgName2 = sourceInfo.getInstallingPackageName();
                    return insPkgName2;
                } else if (sourceInfo.getOriginatingPackageName() == null) {
                    return UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
                } else {
                    String insPkgName3 = sourceInfo.getOriginatingPackageName();
                    return insPkgName3;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
            }
        } catch (Throwable th) {
            return UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
        }
    }

    private void notifyAppResponse(int isFinished, int isActivity, String packageName, String activityName, int[] count, long totalCount) {
        String version;
        if (Build.TRANCARE_SUPPORT && TranTrancareManager.isEnabled(10650092L)) {
            if (1 == isFinished) {
                PackageManager pm = this.mContext.getPackageManager();
                Iterator<String> it = this.mAppResponseTimeRecords.keySet().iterator();
                while (it.hasNext()) {
                    String packageNameKey = it.next();
                    AppResponseTimeRecord appResponseTimeRecord = this.mAppResponseTimeRecords.get(packageNameKey);
                    try {
                        PackageInfo info = pm.getPackageInfo(packageNameKey, 0);
                        version = info.versionName;
                    } catch (Exception e) {
                        e.printStackTrace();
                        version = "uninstalled";
                    }
                    Bundle packageBundle = new Bundle();
                    packageBundle.putString("qg", packageNameKey);
                    packageBundle.putString("ve", version);
                    packageBundle.putString("il", getInstalledPackageName(packageNameKey, pm));
                    packageBundle.putString("st", Arrays.toString(appResponseTimeRecord.mPackageLevels));
                    packageBundle.putString("pt", String.valueOf(appResponseTimeRecord.mPackageTotalTime));
                    ArrayList<Bundle> activitysBundles = new ArrayList<>();
                    for (String activityNameKey : appResponseTimeRecord.mActivityRecords.keySet()) {
                        String version2 = version;
                        ActivityResponseTimeRecord activityResponseTimeRecord = appResponseTimeRecord.mActivityRecords.get(activityNameKey);
                        ActivityResponseTimeRecord activityResponseTimeRecord2 = activityResponseTimeRecord;
                        Bundle activityBundle = new Bundle();
                        activityBundle.putString("an", activityNameKey);
                        activityBundle.putString("rt", Arrays.toString(activityResponseTimeRecord2.mActivityLevels));
                        activityBundle.putString("at", String.valueOf(activityResponseTimeRecord2.mActivityTotalTime));
                        activitysBundles.add(activityBundle);
                        it = it;
                        version = version2;
                        pm = pm;
                    }
                    TranTrancareManager.serverLog((int) MONITOR_UB_APP_RESP_TIME, "art", 3, activitysBundles, packageBundle);
                    appResponseTimeRecord.mActivityRecords.clear();
                    it = it;
                    pm = pm;
                }
                this.mAppResponseTimeRecords.clear();
            } else if (1 == isActivity) {
                ActivityResponseTimeRecord activityResponseTimeRecord3 = new ActivityResponseTimeRecord(count, totalCount);
                AppResponseTimeRecord appResponseTimeRecord2 = this.mAppResponseTimeRecords.get(packageName);
                AppResponseTimeRecord appResponseTimeRecord3 = appResponseTimeRecord2;
                if (appResponseTimeRecord3 != null) {
                    appResponseTimeRecord3.mActivityRecords.put(activityName, activityResponseTimeRecord3);
                }
            } else {
                AppResponseTimeRecord appResponseTimeRecord4 = new AppResponseTimeRecord(count, totalCount);
                this.mAppResponseTimeRecords.put(packageName, appResponseTimeRecord4);
            }
        }
    }

    private void notifySlowResponse(String packageName, String activity, long value) {
        if (Build.TRANCARE_SUPPORT) {
            try {
                Bundle bundle = new Bundle();
                bundle.putInt(DatabaseHelper.SoundModelContract.KEY_TYPE, 3);
                bundle.putString("package", packageName);
                bundle.putString(HostingRecord.HOSTING_TYPE_ACTIVITY, activity);
                bundle.putLong("value", value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    final boolean filterInputEvent(InputEvent event, int policyFlags) {
        synchronized (this.mInputFilterLock) {
            IInputFilter iInputFilter = this.mInputFilter;
            if (iInputFilter != null) {
                try {
                    iInputFilter.filterInputEvent(event, policyFlags);
                } catch (RemoteException e) {
                }
                return false;
            }
            event.recycle();
            return true;
        }
    }

    final boolean onTripleFingerInputEvent(InputEvent event, int policyFlags) {
        if (event instanceof MotionEvent) {
            MotionEvent ev = (MotionEvent) event;
            if (ITranInput.Instance().checkMotionEvent(ev)) {
                return false;
            }
        }
        event.recycle();
        return true;
    }

    final int onSafetyEdgeInputEvent(InputEvent event) {
        if (event instanceof MotionEvent) {
            MotionEvent ev = (MotionEvent) event;
            return ITranInput.Instance().checkEdgeMotionEvent(ev);
        }
        event.recycle();
        return 0;
    }

    private int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        return this.mWindowManagerCallbacks.interceptKeyBeforeQueueing(event, policyFlags);
    }

    private int interceptMotionBeforeQueueingNonInteractive(int displayId, long whenNanos, int policyFlags) {
        return this.mWindowManagerCallbacks.interceptMotionBeforeQueueingNonInteractive(displayId, whenNanos, policyFlags);
    }

    private long interceptKeyBeforeDispatching(IBinder focus, KeyEvent event, int policyFlags) {
        return this.mWindowManagerCallbacks.interceptKeyBeforeDispatching(focus, event, policyFlags);
    }

    private KeyEvent dispatchUnhandledKey(IBinder focus, KeyEvent event, int policyFlags) {
        return this.mWindowManagerCallbacks.dispatchUnhandledKey(focus, event, policyFlags);
    }

    private void onPointerDownOutsideFocus(IBinder touchedToken) {
        this.mWindowManagerCallbacks.onPointerDownOutsideFocus(touchedToken);
    }

    private int getVirtualKeyQuietTimeMillis() {
        return this.mContext.getResources().getInteger(17694972);
    }

    private static String[] getExcludedDeviceNames() {
        List<String> names = new ArrayList<>();
        File[] baseDirs = {Environment.getRootDirectory(), Environment.getVendorDirectory()};
        for (File baseDir : baseDirs) {
            File confFile = new File(baseDir, EXCLUDED_DEVICES_PATH);
            try {
                InputStream stream = new FileInputStream(confFile);
                try {
                    names.addAll(ConfigurationProcessor.processExcludedDeviceNames(stream));
                    stream.close();
                } catch (Throwable th) {
                    try {
                        stream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                    break;
                }
            } catch (FileNotFoundException e) {
            } catch (Exception e2) {
                Slog.e(TAG, "Could not parse '" + confFile.getAbsolutePath() + "'", e2);
            }
        }
        return (String[]) names.toArray(new String[0]);
    }

    private static <T> String[] flatten(Map<String, T> map) {
        final List<String> list = new ArrayList<>(map.size() * 2);
        map.forEach(new BiConsumer() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                InputManagerService.lambda$flatten$11(list, (String) obj, obj2);
            }
        });
        return (String[]) list.toArray(new String[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$flatten$11(List list, String k, Object v) {
        list.add(k);
        list.add(v.toString());
    }

    private static Map<String, Integer> loadStaticInputPortAssociations() {
        File baseDir = Environment.getVendorDirectory();
        File confFile = new File(baseDir, PORT_ASSOCIATIONS_PATH);
        try {
            InputStream stream = new FileInputStream(confFile);
            try {
                Map<String, Integer> processInputPortAssociations = ConfigurationProcessor.processInputPortAssociations(stream);
                stream.close();
                return processInputPortAssociations;
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (FileNotFoundException e) {
            return new HashMap();
        } catch (Exception e2) {
            Slog.e(TAG, "Could not parse '" + confFile.getAbsolutePath() + "'", e2);
            return new HashMap();
        }
    }

    private String[] getInputPortAssociations() {
        Map<String, Integer> associations = new HashMap<>(this.mStaticAssociations);
        synchronized (this.mAssociationsLock) {
            associations.putAll(this.mRuntimeAssociations);
        }
        return flatten(associations);
    }

    private String[] getInputUniqueIdAssociations() {
        Map<String, String> associations;
        synchronized (this.mAssociationsLock) {
            associations = new HashMap<>(this.mUniqueIdAssociations);
        }
        return flatten(associations);
    }

    public boolean canDispatchToDisplay(int deviceId, int displayId) {
        return this.mNative.canDispatchToDisplay(deviceId, displayId);
    }

    private int getKeyRepeatTimeout() {
        return ViewConfiguration.getKeyRepeatTimeout();
    }

    private int getKeyRepeatDelay() {
        return ViewConfiguration.getKeyRepeatDelay();
    }

    private int getHoverTapTimeout() {
        return ViewConfiguration.getHoverTapTimeout();
    }

    private int getHoverTapSlop() {
        return ViewConfiguration.getHoverTapSlop();
    }

    private int getDoubleTapTimeout() {
        return ViewConfiguration.getDoubleTapTimeout();
    }

    private int getLongPressTimeout() {
        return ViewConfiguration.getLongPressTimeout();
    }

    private int getPointerLayer() {
        return this.mWindowManagerCallbacks.getPointerLayer();
    }

    private PointerIcon getPointerIcon(int displayId) {
        return PointerIcon.getDefaultIcon(getContextForPointerIcon(displayId));
    }

    private long getParentSurfaceForPointers(int displayId) {
        SurfaceControl sc = this.mWindowManagerCallbacks.getParentSurfaceForPointers(displayId);
        if (sc == null) {
            return 0L;
        }
        return sc.mNativeObject;
    }

    private Context getContextForPointerIcon(int displayId) {
        Context context = this.mPointerIconDisplayContext;
        if (context != null && context.getDisplay().getDisplayId() == displayId) {
            return this.mPointerIconDisplayContext;
        }
        Context contextForDisplay = getContextForDisplay(displayId);
        this.mPointerIconDisplayContext = contextForDisplay;
        if (contextForDisplay == null) {
            this.mPointerIconDisplayContext = getContextForDisplay(0);
        }
        return this.mPointerIconDisplayContext;
    }

    private Context getContextForDisplay(int displayId) {
        if (displayId == -1) {
            return null;
        }
        if (this.mContext.getDisplay().getDisplayId() == displayId) {
            return this.mContext;
        }
        DisplayManager displayManager = (DisplayManager) Objects.requireNonNull((DisplayManager) this.mContext.getSystemService(DisplayManager.class));
        Display display = displayManager.getDisplay(displayId);
        if (display == null) {
            return null;
        }
        return this.mContext.createDisplayContext(display);
    }

    private String[] getKeyboardLayoutOverlay(InputDeviceIdentifier identifier) {
        String keyboardLayoutDescriptor;
        if (this.mSystemReady && (keyboardLayoutDescriptor = getCurrentKeyboardLayoutForInputDevice(identifier)) != null) {
            final String[] result = new String[2];
            visitKeyboardLayout(keyboardLayoutDescriptor, new KeyboardLayoutVisitor() { // from class: com.android.server.input.InputManagerService$$ExternalSyntheticLambda12
                @Override // com.android.server.input.InputManagerService.KeyboardLayoutVisitor
                public final void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                    InputManagerService.lambda$getKeyboardLayoutOverlay$12(result, resources, i, keyboardLayout);
                }
            });
            if (result[0] == null) {
                Slog.w(TAG, "Could not get keyboard layout with descriptor '" + keyboardLayoutDescriptor + "'.");
                return null;
            }
            return result;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getKeyboardLayoutOverlay$12(String[] result, Resources resources, int keyboardLayoutResId, KeyboardLayout layout) {
        try {
            InputStreamReader stream = new InputStreamReader(resources.openRawResource(keyboardLayoutResId));
            result[0] = layout.getDescriptor();
            result[1] = Streams.readFully(stream);
            stream.close();
        } catch (Resources.NotFoundException | IOException e) {
        }
    }

    private String getDeviceAlias(String uniqueId) {
        BluetoothAdapter.checkBluetoothAddress(uniqueId);
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PointerDisplayIdChangedArgs {
        final int mPointerDisplayId;
        final float mXPosition;
        final float mYPosition;

        PointerDisplayIdChangedArgs(int pointerDisplayId, float xPosition, float yPosition) {
            this.mPointerDisplayId = pointerDisplayId;
            this.mXPosition = xPosition;
            this.mYPosition = yPosition;
        }
    }

    void onPointerDisplayIdChanged(int pointerDisplayId, float xPosition, float yPosition) {
        this.mHandler.obtainMessage(7, new PointerDisplayIdChangedArgs(pointerDisplayId, xPosition, yPosition)).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class InputManagerHandler extends Handler {
        public InputManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    InputManagerService.this.deliverInputDevicesChanged((InputDevice[]) msg.obj);
                    return;
                case 2:
                    InputManagerService.this.handleSwitchKeyboardLayout(msg.arg1, msg.arg2);
                    return;
                case 3:
                    InputManagerService.this.reloadKeyboardLayouts();
                    return;
                case 4:
                    InputManagerService.this.updateKeyboardLayouts();
                    return;
                case 5:
                    InputManagerService.this.reloadDeviceAliases();
                    return;
                case 6:
                    SomeArgs args = (SomeArgs) msg.obj;
                    long whenNanos = (args.argi1 & 4294967295L) | (args.argi2 << 32);
                    boolean inTabletMode = ((Boolean) args.arg1).booleanValue();
                    InputManagerService.this.deliverTabletModeChanged(whenNanos, inTabletMode);
                    return;
                case 7:
                    InputManagerService.this.handlePointerDisplayIdChanged((PointerDisplayIdChangedArgs) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class InputFilterHost extends IInputFilterHost.Stub {
        private boolean mDisconnected;

        private InputFilterHost() {
        }

        public void disconnectLocked() {
            this.mDisconnected = true;
        }

        public void sendInputEvent(InputEvent event, int policyFlags) {
            if (!InputManagerService.this.checkCallingPermission("android.permission.INJECT_EVENTS", "sendInputEvent()")) {
                throw new SecurityException("The INJECT_EVENTS permission is required for injecting input events.");
            }
            Objects.requireNonNull(event, "event must not be null");
            synchronized (InputManagerService.this.mInputFilterLock) {
                if (!this.mDisconnected) {
                    InputManagerService.this.mNative.injectInputEvent(event, false, -1, 0, 0, policyFlags | 67108864);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class InputMonitorHost extends IInputMonitorHost.Stub {
        private final IBinder mInputChannelToken;

        InputMonitorHost(IBinder inputChannelToken) {
            this.mInputChannelToken = inputChannelToken;
        }

        public void pilferPointers() {
            InputManagerService.this.mNative.pilferPointers(this.mInputChannelToken);
        }

        public void dispose() {
            InputManagerService.this.removeSpyWindowGestureMonitor(this.mInputChannelToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class KeyboardLayoutDescriptor {
        public String keyboardLayoutName;
        public String packageName;
        public String receiverName;

        private KeyboardLayoutDescriptor() {
        }

        public static String format(String packageName, String receiverName, String keyboardName) {
            return packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiverName + SliceClientPermissions.SliceAuthority.DELIMITER + keyboardName;
        }

        public static KeyboardLayoutDescriptor parse(String descriptor) {
            int pos2;
            int pos = descriptor.indexOf(47);
            if (pos < 0 || pos + 1 == descriptor.length() || (pos2 = descriptor.indexOf(47, pos + 1)) < pos + 2 || pos2 + 1 == descriptor.length()) {
                return null;
            }
            KeyboardLayoutDescriptor result = new KeyboardLayoutDescriptor();
            result.packageName = descriptor.substring(0, pos);
            result.receiverName = descriptor.substring(pos + 1, pos2);
            result.keyboardLayoutName = descriptor.substring(pos2 + 1);
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class InputDevicesChangedListenerRecord implements IBinder.DeathRecipient {
        private final IInputDevicesChangedListener mListener;
        private final int mPid;

        public InputDevicesChangedListenerRecord(int pid, IInputDevicesChangedListener listener) {
            this.mPid = pid;
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            InputManagerService.this.onInputDevicesChangedListenerDied(this.mPid);
        }

        public void notifyInputDevicesChanged(int[] info) {
            try {
                this.mListener.onInputDevicesChanged(info);
            } catch (RemoteException ex) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that input devices changed, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TabletModeChangedListenerRecord implements IBinder.DeathRecipient {
        private final ITabletModeChangedListener mListener;
        private final int mPid;

        public TabletModeChangedListenerRecord(int pid, ITabletModeChangedListener listener) {
            this.mPid = pid;
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            InputManagerService.this.onTabletModeChangedListenerDied(this.mPid);
        }

        public void notifyTabletModeChanged(long whenNanos, boolean inTabletMode) {
            try {
                this.mListener.onTabletModeChanged(whenNanos, inTabletMode);
            } catch (RemoteException ex) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that tablet mode changed, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSensorEventListenerDied(int pid) {
        synchronized (this.mSensorEventLock) {
            this.mSensorEventListeners.remove(pid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SensorEventListenerRecord implements IBinder.DeathRecipient {
        private final IInputSensorEventListener mListener;
        private final int mPid;

        SensorEventListenerRecord(int pid, IInputSensorEventListener listener) {
            this.mPid = pid;
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            InputManagerService.this.onSensorEventListenerDied(this.mPid);
        }

        public IInputSensorEventListener getListener() {
            return this.mListener;
        }

        public void notifySensorEvent(int deviceId, int sensorType, int accuracy, long timestamp, float[] values) {
            try {
                this.mListener.onInputSensorChanged(deviceId, sensorType, accuracy, timestamp, values);
            } catch (RemoteException ex) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that sensor event notified, assuming it died.", ex);
                binderDied();
            }
        }

        public void notifySensorAccuracy(int deviceId, int sensorType, int accuracy) {
            try {
                this.mListener.onInputSensorAccuracyChanged(deviceId, sensorType, accuracy);
            } catch (RemoteException ex) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that sensor accuracy notified, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class VibratorToken implements IBinder.DeathRecipient {
        public final int mDeviceId;
        public final IBinder mToken;
        public final int mTokenValue;
        public boolean mVibrating;

        public VibratorToken(int deviceId, IBinder token, int tokenValue) {
            this.mDeviceId = deviceId;
            this.mToken = token;
            this.mTokenValue = tokenValue;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            InputManagerService.this.onVibratorTokenDied(this);
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends InputManagerInternal {
        private LocalService() {
        }

        public void setDisplayViewports(List<DisplayViewport> viewports) {
            InputManagerService.this.setDisplayViewportsInternal(viewports);
        }

        public void setInteractive(boolean interactive) {
            InputManagerService.this.mNative.setInteractive(interactive);
        }

        public void toggleCapsLock(int deviceId) {
            InputManagerService.this.mNative.toggleCapsLock(deviceId);
        }

        public void setPulseGestureEnabled(boolean enabled) {
            if (InputManagerService.this.mDoubleTouchGestureEnableFile != null) {
                FileWriter writer = null;
                try {
                    try {
                        writer = new FileWriter(InputManagerService.this.mDoubleTouchGestureEnableFile);
                        writer.write(enabled ? "1" : "0");
                    } catch (IOException e) {
                        Log.wtf(InputManagerService.TAG, "Unable to setPulseGestureEnabled", e);
                    }
                } finally {
                    IoUtils.closeQuietly(writer);
                }
            }
        }

        public boolean transferTouchFocus(IBinder fromChannelToken, IBinder toChannelToken) {
            return InputManagerService.this.transferTouchFocus(fromChannelToken, toChannelToken);
        }

        public boolean setVirtualMousePointerDisplayId(int pointerDisplayId) {
            return InputManagerService.this.setVirtualMousePointerDisplayIdBlocking(pointerDisplayId);
        }

        public int getVirtualMousePointerDisplayId() {
            return InputManagerService.this.getVirtualMousePointerDisplayId();
        }

        public PointF getCursorPosition() {
            return InputManagerService.this.mWindowManagerCallbacks.getCursorPosition();
        }

        public void setPointerAcceleration(float acceleration, int displayId) {
            InputManagerService.this.setPointerAcceleration(acceleration, displayId);
        }

        public void setDisplayEligibilityForPointerCapture(int displayId, boolean isEligible) {
            InputManagerService.this.setDisplayEligibilityForPointerCapture(displayId, isEligible);
        }

        public void setPointerIconVisible(boolean visible, int displayId) {
            InputManagerService.this.setPointerIconVisible(visible, displayId);
        }

        public void registerLidSwitchCallback(InputManagerInternal.LidSwitchCallback callbacks) {
            InputManagerService.this.registerLidSwitchCallbackInternal(callbacks);
        }

        public void unregisterLidSwitchCallback(InputManagerInternal.LidSwitchCallback callbacks) {
            InputManagerService.this.unregisterLidSwitchCallbackInternal(callbacks);
        }

        public InputChannel createInputChannel(String inputChannelName) {
            return InputManagerService.this.createInputChannel(inputChannelName);
        }

        public void pilferPointers(IBinder token) {
            InputManagerService.this.mNative.pilferPointers(token);
        }

        public void setConnectScreenActive(boolean active) {
            InputManagerService.this.mNative.setConnectScreenActive(active);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.input.InputManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new InputShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AdditionalDisplayInputProperties {
        static final float DEFAULT_POINTER_ACCELERATION = 3.0f;
        static final boolean DEFAULT_POINTER_ICON_VISIBLE = true;
        public float pointerAcceleration;
        public boolean pointerIconVisible;

        AdditionalDisplayInputProperties() {
            reset();
        }

        public boolean allDefaults() {
            return Float.compare(this.pointerAcceleration, DEFAULT_POINTER_ACCELERATION) == 0 && this.pointerIconVisible;
        }

        public void reset() {
            this.pointerAcceleration = DEFAULT_POINTER_ACCELERATION;
            this.pointerIconVisible = true;
        }
    }

    private void applyAdditionalDisplayInputProperties() {
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            AdditionalDisplayInputProperties properties = this.mAdditionalDisplayInputProperties.get(this.mRequestedPointerDisplayId);
            if (properties == null) {
                properties = DEFAULT_ADDITIONAL_DISPLAY_INPUT_PROPERTIES;
            }
            applyAdditionalDisplayInputPropertiesLocked(properties);
        }
    }

    private void applyAdditionalDisplayInputPropertiesLocked(AdditionalDisplayInputProperties properties) {
        if (properties.pointerIconVisible != this.mCurrentDisplayProperties.pointerIconVisible) {
            this.mCurrentDisplayProperties.pointerIconVisible = properties.pointerIconVisible;
            if (properties.pointerIconVisible) {
                int i = this.mIconType;
                if (i == -1) {
                    Objects.requireNonNull(this.mIcon);
                    this.mNative.setCustomPointerIcon(this.mIcon);
                } else {
                    this.mNative.setPointerIconType(i);
                }
            } else {
                this.mNative.setPointerIconType(0);
            }
        }
        if (properties.pointerAcceleration != this.mCurrentDisplayProperties.pointerAcceleration) {
            this.mCurrentDisplayProperties.pointerAcceleration = properties.pointerAcceleration;
            this.mNative.setPointerAcceleration(properties.pointerAcceleration);
        }
    }

    private void updateAdditionalDisplayInputProperties(int displayId, Consumer<AdditionalDisplayInputProperties> updater) {
        synchronized (this.mAdditionalDisplayInputPropertiesLock) {
            AdditionalDisplayInputProperties properties = this.mAdditionalDisplayInputProperties.get(displayId);
            if (properties == null) {
                properties = new AdditionalDisplayInputProperties();
                this.mAdditionalDisplayInputProperties.put(displayId, properties);
            }
            updater.accept(properties);
            if (properties.allDefaults()) {
                this.mAdditionalDisplayInputProperties.remove(displayId);
            }
            if (displayId != this.mRequestedPointerDisplayId) {
                Log.i(TAG, "Not applying additional properties for display " + displayId + " because the pointer is currently targeting display " + this.mRequestedPointerDisplayId + ".");
            } else {
                applyAdditionalDisplayInputPropertiesLocked(properties);
            }
        }
    }

    public void setInputRspTimeEnabled(boolean enabled) {
        this.mNative.setInputRspTimeEnabled(enabled);
    }

    public void setInputRspDumpEnabled(boolean enabled) {
        this.mNative.setInputRspDumpEnabled(enabled);
    }

    /* loaded from: classes.dex */
    private final class ActivityResponseTimeRecord {
        int[] mActivityLevels;
        long mActivityTotalTime;

        public ActivityResponseTimeRecord(int[] count, long activityTotalTime) {
            this.mActivityTotalTime = activityTotalTime;
            this.mActivityLevels = Arrays.copyOf(count, 9);
        }
    }

    /* loaded from: classes.dex */
    private final class AppResponseTimeRecord {
        HashMap<String, ActivityResponseTimeRecord> mActivityRecords = new HashMap<>();
        int[] mPackageLevels;
        long mPackageTotalTime;

        public AppResponseTimeRecord(int[] count, long packageTotalTime) {
            this.mPackageTotalTime = packageTotalTime;
            this.mPackageLevels = Arrays.copyOf(count, 9);
        }
    }

    public void registerGestureListener(IGestureListener listener) {
        ITranInput.Instance().registerGestureListener(listener);
    }

    public void unRegisterGestureListener(IGestureListener listener) {
        ITranInput.Instance().unRegisterGestureListener(listener);
    }

    public String getMagellanConfig() {
        return ITranInput.Instance().getMagellanConfig();
    }

    public boolean updateTrackingData(int type, Bundle trackingData) {
        return ITranInput.Instance().updateTrackingData(type, trackingData);
    }

    public boolean ignoreMultiWindowGesture() {
        return this.mNative.ignoreMultiWindowGesture();
    }
}
