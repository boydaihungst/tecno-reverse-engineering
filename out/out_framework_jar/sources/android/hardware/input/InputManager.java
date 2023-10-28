package android.hardware.input;

import android.app.ActivityThread;
import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.input.IInputDevicesChangedListener;
import android.hardware.input.IInputManager;
import android.hardware.input.ITabletModeChangedListener;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.IBinder;
import android.os.IVibratorStateListener;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputMonitor;
import android.view.PointerIcon;
import android.view.VerifiedInputEvent;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public final class InputManager {
    public static final String ACTION_QUERY_KEYBOARD_LAYOUTS = "android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS";
    public static final long BLOCK_UNTRUSTED_TOUCHES = 158002302;
    public static final int[] BLOCK_UNTRUSTED_TOUCHES_MODES = {0, 1, 2};
    private static final boolean DEBUG = false;
    public static final int DEFAULT_BLOCK_UNTRUSTED_TOUCHES_MODE = 2;
    public static final float DEFAULT_MAXIMUM_OBSCURING_OPACITY_FOR_TOUCH = 0.8f;
    public static final int DEFAULT_POINTER_SPEED = 0;
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int MAX_POINTER_SPEED = 7;
    public static final String META_DATA_KEYBOARD_LAYOUTS = "android.hardware.input.metadata.KEYBOARD_LAYOUTS";
    public static final int MIN_POINTER_SPEED = -7;
    private static final int MSG_DEVICE_ADDED = 1;
    private static final int MSG_DEVICE_CHANGED = 3;
    private static final int MSG_DEVICE_REMOVED = 2;
    public static final int SWITCH_STATE_OFF = 0;
    public static final int SWITCH_STATE_ON = 1;
    public static final int SWITCH_STATE_UNKNOWN = -1;
    private static final String TAG = "InputManager";
    private static InputManager sInstance;
    private final IInputManager mIm;
    private InputDeviceSensorManager mInputDeviceSensorManager;
    private SparseArray<InputDevice> mInputDevices;
    private InputDevicesChangedListener mInputDevicesChangedListener;
    private List<OnTabletModeChangedListenerDelegate> mOnTabletModeChangedListeners;
    private TabletModeChangedListener mTabletModeChangedListener;
    private final Object mInputDevicesLock = new Object();
    private final ArrayList<InputDeviceListenerDelegate> mInputDeviceListeners = new ArrayList<>();
    private final Object mTabletModeLock = new Object();

    /* loaded from: classes2.dex */
    public interface InputDeviceListener {
        void onInputDeviceAdded(int i);

        void onInputDeviceChanged(int i);

        void onInputDeviceRemoved(int i);
    }

    /* loaded from: classes2.dex */
    public interface OnTabletModeChangedListener {
        void onTabletModeChanged(long j, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SwitchState {
    }

    private InputManager(IInputManager im) {
        this.mIm = im;
    }

    public static InputManager resetInstance(IInputManager inputManagerService) {
        InputManager inputManager;
        synchronized (InputManager.class) {
            inputManager = new InputManager(inputManagerService);
            sInstance = inputManager;
        }
        return inputManager;
    }

    public static void clearInstance() {
        synchronized (InputManager.class) {
            sInstance = null;
        }
    }

    public static InputManager getInstance() {
        InputManager inputManager;
        synchronized (InputManager.class) {
            if (sInstance == null) {
                try {
                    sInstance = new InputManager(IInputManager.Stub.asInterface(ServiceManager.getServiceOrThrow("input")));
                } catch (ServiceManager.ServiceNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
            inputManager = sInstance;
        }
        return inputManager;
    }

    public InputDevice getInputDevice(int id) {
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int index = this.mInputDevices.indexOfKey(id);
            if (index < 0) {
                return null;
            }
            InputDevice inputDevice = this.mInputDevices.valueAt(index);
            if (inputDevice == null) {
                try {
                    inputDevice = this.mIm.getInputDevice(id);
                    if (inputDevice != null) {
                        this.mInputDevices.setValueAt(index, inputDevice);
                    }
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
            return inputDevice;
        }
    }

    public InputDevice getInputDeviceByDescriptor(String descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null.");
        }
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int numDevices = this.mInputDevices.size();
            for (int i = 0; i < numDevices; i++) {
                InputDevice inputDevice = this.mInputDevices.valueAt(i);
                if (inputDevice == null) {
                    int id = this.mInputDevices.keyAt(i);
                    try {
                        inputDevice = this.mIm.getInputDevice(id);
                        if (inputDevice == null) {
                            continue;
                        } else {
                            this.mInputDevices.setValueAt(i, inputDevice);
                        }
                    } catch (RemoteException ex) {
                        throw ex.rethrowFromSystemServer();
                    }
                }
                if (descriptor.equals(inputDevice.getDescriptor())) {
                    return inputDevice;
                }
            }
            return null;
        }
    }

    public int[] getInputDeviceIds() {
        int[] ids;
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int count = this.mInputDevices.size();
            ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = this.mInputDevices.keyAt(i);
            }
        }
        return ids;
    }

    public boolean isInputDeviceEnabled(int id) {
        try {
            return this.mIm.isInputDeviceEnabled(id);
        } catch (RemoteException ex) {
            Log.w(TAG, "Could not check enabled status of input device with id = " + id);
            throw ex.rethrowFromSystemServer();
        }
    }

    public void enableInputDevice(int id) {
        try {
            this.mIm.enableInputDevice(id);
        } catch (RemoteException ex) {
            Log.w(TAG, "Could not enable input device with id = " + id);
            throw ex.rethrowFromSystemServer();
        }
    }

    public void disableInputDevice(int id) {
        try {
            this.mIm.disableInputDevice(id);
        } catch (RemoteException ex) {
            Log.w(TAG, "Could not disable input device with id = " + id);
            throw ex.rethrowFromSystemServer();
        }
    }

    public void registerInputDeviceListener(InputDeviceListener listener, Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int index = findInputDeviceListenerLocked(listener);
            if (index < 0) {
                this.mInputDeviceListeners.add(new InputDeviceListenerDelegate(listener, handler));
            }
        }
    }

    public void unregisterInputDeviceListener(InputDeviceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mInputDevicesLock) {
            int index = findInputDeviceListenerLocked(listener);
            if (index >= 0) {
                InputDeviceListenerDelegate d = this.mInputDeviceListeners.get(index);
                d.removeCallbacksAndMessages(null);
                this.mInputDeviceListeners.remove(index);
            }
        }
    }

    private int findInputDeviceListenerLocked(InputDeviceListener listener) {
        int numListeners = this.mInputDeviceListeners.size();
        for (int i = 0; i < numListeners; i++) {
            if (this.mInputDeviceListeners.get(i).mListener == listener) {
                return i;
            }
        }
        return -1;
    }

    public int isInTabletMode() {
        try {
            return this.mIm.isInTabletMode();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void registerOnTabletModeChangedListener(OnTabletModeChangedListener listener, Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTabletModeLock) {
            if (this.mOnTabletModeChangedListeners == null) {
                initializeTabletModeListenerLocked();
            }
            int idx = findOnTabletModeChangedListenerLocked(listener);
            if (idx < 0) {
                OnTabletModeChangedListenerDelegate d = new OnTabletModeChangedListenerDelegate(listener, handler);
                this.mOnTabletModeChangedListeners.add(d);
            }
        }
    }

    public void unregisterOnTabletModeChangedListener(OnTabletModeChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTabletModeLock) {
            int idx = findOnTabletModeChangedListenerLocked(listener);
            if (idx >= 0) {
                OnTabletModeChangedListenerDelegate d = this.mOnTabletModeChangedListeners.remove(idx);
                d.removeCallbacksAndMessages(null);
            }
        }
    }

    private void initializeTabletModeListenerLocked() {
        TabletModeChangedListener listener = new TabletModeChangedListener();
        try {
            this.mIm.registerTabletModeChangedListener(listener);
            this.mTabletModeChangedListener = listener;
            this.mOnTabletModeChangedListeners = new ArrayList();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private int findOnTabletModeChangedListenerLocked(OnTabletModeChangedListener listener) {
        int N = this.mOnTabletModeChangedListeners.size();
        for (int i = 0; i < N; i++) {
            if (this.mOnTabletModeChangedListeners.get(i).mListener == listener) {
                return i;
            }
        }
        return -1;
    }

    public int isMicMuted() {
        try {
            return this.mIm.isMicMuted();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public KeyboardLayout[] getKeyboardLayouts() {
        try {
            return this.mIm.getKeyboardLayouts();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public List<String> getKeyboardLayoutDescriptorsForInputDevice(InputDevice device) {
        KeyboardLayout[] layouts = getKeyboardLayoutsForInputDevice(device.getIdentifier());
        List<String> res = new ArrayList<>();
        for (KeyboardLayout kl : layouts) {
            res.add(kl.getDescriptor());
        }
        return res;
    }

    public KeyboardLayout[] getKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) {
        try {
            return this.mIm.getKeyboardLayoutsForInputDevice(identifier);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public KeyboardLayout getKeyboardLayout(String keyboardLayoutDescriptor) {
        if (keyboardLayoutDescriptor == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            return this.mIm.getKeyboardLayout(keyboardLayoutDescriptor);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier) {
        try {
            return this.mIm.getCurrentKeyboardLayoutForInputDevice(identifier);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        if (keyboardLayoutDescriptor == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.setCurrentKeyboardLayoutForInputDevice(identifier, keyboardLayoutDescriptor);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        try {
            return this.mIm.getEnabledKeyboardLayoutsForInputDevice(identifier);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (identifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        if (keyboardLayoutDescriptor == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.addKeyboardLayoutForInputDevice(identifier, keyboardLayoutDescriptor);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) {
        if (identifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        if (keyboardLayoutDescriptor == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.removeKeyboardLayoutForInputDevice(identifier, keyboardLayoutDescriptor);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public TouchCalibration getTouchCalibration(String inputDeviceDescriptor, int surfaceRotation) {
        try {
            return this.mIm.getTouchCalibrationForInputDevice(inputDeviceDescriptor, surfaceRotation);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setTouchCalibration(String inputDeviceDescriptor, int surfaceRotation, TouchCalibration calibration) {
        try {
            this.mIm.setTouchCalibrationForInputDevice(inputDeviceDescriptor, surfaceRotation, calibration);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void registerGestureListener(IGestureListener listener) {
        try {
            this.mIm.registerGestureListener(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void unRegisterGestureListener(IGestureListener listener) {
        try {
            this.mIm.unRegisterGestureListener(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public String getMagellanConfig() {
        try {
            String config = this.mIm.getMagellanConfig();
            return config;
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean updateTrackingData(int type, Bundle trackingData) {
        try {
            boolean bUpdateSuc = this.mIm.updateTrackingData(type, trackingData);
            return bUpdateSuc;
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public int getPointerSpeed(Context context) {
        try {
            int speed = Settings.System.getInt(context.getContentResolver(), Settings.System.POINTER_SPEED);
            return speed;
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    public void setPointerSpeed(Context context, int speed) {
        if (speed < -7 || speed > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        Settings.System.putInt(context.getContentResolver(), Settings.System.POINTER_SPEED, speed);
    }

    public void tryPointerSpeed(int speed) {
        if (speed < -7 || speed > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        try {
            this.mIm.tryPointerSpeed(speed);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public float getMaximumObscuringOpacityForTouch() {
        Context context = ActivityThread.currentApplication();
        return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.MAXIMUM_OBSCURING_OPACITY_FOR_TOUCH, 0.8f);
    }

    public void setMaximumObscuringOpacityForTouch(float opacity) {
        if (opacity < 0.0f || opacity > 1.0f) {
            throw new IllegalArgumentException("Maximum obscuring opacity for touch should be >= 0 and <= 1");
        }
        Context context = ActivityThread.currentApplication();
        Settings.Global.putFloat(context.getContentResolver(), Settings.Global.MAXIMUM_OBSCURING_OPACITY_FOR_TOUCH, opacity);
    }

    public int getBlockUntrustedTouchesMode(Context context) {
        int mode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BLOCK_UNTRUSTED_TOUCHES_MODE, 2);
        if (!ArrayUtils.contains(BLOCK_UNTRUSTED_TOUCHES_MODES, mode)) {
            Log.w(TAG, "Unknown block untrusted touches feature mode " + mode + ", using default 2");
            return 2;
        }
        return mode;
    }

    public void setBlockUntrustedTouchesMode(Context context, int mode) {
        if (!ArrayUtils.contains(BLOCK_UNTRUSTED_TOUCHES_MODES, mode)) {
            throw new IllegalArgumentException("Invalid feature mode " + mode);
        }
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.BLOCK_UNTRUSTED_TOUCHES_MODE, mode);
    }

    public boolean[] deviceHasKeys(int[] keyCodes) {
        return deviceHasKeys(-1, keyCodes);
    }

    public boolean[] deviceHasKeys(int id, int[] keyCodes) {
        boolean[] ret = new boolean[keyCodes.length];
        try {
            this.mIm.hasKeys(id, -256, keyCodes, ret);
            return ret;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode) {
        try {
            return this.mIm.getKeyCodeForKeyLocation(deviceId, locationKeyCode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean injectInputEvent(InputEvent event, int mode, int targetUid) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (mode != 0 && mode != 2 && mode != 1) {
            throw new IllegalArgumentException("mode is invalid");
        }
        try {
            return this.mIm.injectInputEventToTarget(event, mode, targetUid);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean injectInputEvent(InputEvent event, int mode) {
        return injectInputEvent(event, mode, -1);
    }

    public VerifiedInputEvent verifyInputEvent(InputEvent event) {
        try {
            return this.mIm.verifyInputEvent(event);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setPointerIconType(int iconId) {
        try {
            this.mIm.setPointerIconType(iconId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setCustomPointerIcon(PointerIcon icon) {
        try {
            this.mIm.setCustomPointerIcon(icon);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void requestPointerCapture(IBinder windowToken, boolean enable) {
        try {
            this.mIm.requestPointerCapture(windowToken, enable);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public InputMonitor monitorGestureInput(String name, int displayId) {
        try {
            return this.mIm.monitorGestureInput(new Binder(), name, displayId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public InputSensorInfo[] getSensorList(int deviceId) {
        try {
            return this.mIm.getSensorList(deviceId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs, int maxBatchReportLatencyUs) {
        try {
            return this.mIm.enableSensor(deviceId, sensorType, samplingPeriodUs, maxBatchReportLatencyUs);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void disableSensor(int deviceId, int sensorType) {
        try {
            this.mIm.disableSensor(deviceId, sensorType);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean flushSensor(int deviceId, int sensorType) {
        try {
            return this.mIm.flushSensor(deviceId, sensorType);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean registerSensorListener(IInputSensorEventListener listener) {
        try {
            return this.mIm.registerSensorListener(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void unregisterSensorListener(IInputSensorEventListener listener) {
        try {
            this.mIm.unregisterSensorListener(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public int getBatteryStatus(int deviceId) {
        try {
            return this.mIm.getBatteryStatus(deviceId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public int getBatteryCapacity(int deviceId) {
        try {
            return this.mIm.getBatteryCapacity(deviceId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void addPortAssociation(String inputPort, int displayPort) {
        try {
            this.mIm.addPortAssociation(inputPort, displayPort);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void removePortAssociation(String inputPort) {
        try {
            this.mIm.removePortAssociation(inputPort);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void addUniqueIdAssociation(String inputPort, String displayUniqueId) {
        try {
            this.mIm.addUniqueIdAssociation(inputPort, displayUniqueId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUniqueIdAssociation(String inputPort) {
        try {
            this.mIm.removeUniqueIdAssociation(inputPort);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void populateInputDevicesLocked() {
        if (this.mInputDevicesChangedListener == null) {
            InputDevicesChangedListener listener = new InputDevicesChangedListener();
            try {
                this.mIm.registerInputDevicesChangedListener(listener);
                this.mInputDevicesChangedListener = listener;
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        if (this.mInputDevices == null) {
            try {
                int[] ids = this.mIm.getInputDeviceIds();
                this.mInputDevices = new SparseArray<>();
                if (ids != null) {
                    for (int i : ids) {
                        this.mInputDevices.put(i, null);
                    }
                }
            } catch (RemoteException ex2) {
                throw ex2.rethrowFromSystemServer();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInputDevicesChanged(int[] deviceIdAndGeneration) {
        synchronized (this.mInputDevicesLock) {
            int i = this.mInputDevices.size();
            while (true) {
                i--;
                if (i <= 0) {
                    break;
                }
                int deviceId = this.mInputDevices.keyAt(i);
                if (!containsDeviceId(deviceIdAndGeneration, deviceId)) {
                    this.mInputDevices.removeAt(i);
                    sendMessageToInputDeviceListenersLocked(2, deviceId);
                }
            }
            for (int i2 = 0; i2 < deviceIdAndGeneration.length; i2 += 2) {
                int deviceId2 = deviceIdAndGeneration[i2];
                int index = this.mInputDevices.indexOfKey(deviceId2);
                if (index < 0) {
                    this.mInputDevices.put(deviceId2, null);
                    sendMessageToInputDeviceListenersLocked(1, deviceId2);
                } else {
                    InputDevice device = this.mInputDevices.valueAt(index);
                    if (device != null) {
                        int generation = deviceIdAndGeneration[i2 + 1];
                        if (device.getGeneration() != generation) {
                            this.mInputDevices.setValueAt(index, null);
                            sendMessageToInputDeviceListenersLocked(3, deviceId2);
                        }
                    }
                }
            }
        }
    }

    private void sendMessageToInputDeviceListenersLocked(int what, int deviceId) {
        int numListeners = this.mInputDeviceListeners.size();
        for (int i = 0; i < numListeners; i++) {
            InputDeviceListenerDelegate listener = this.mInputDeviceListeners.get(i);
            listener.sendMessage(listener.obtainMessage(what, deviceId, 0));
        }
    }

    private static boolean containsDeviceId(int[] deviceIdAndGeneration, int deviceId) {
        for (int i = 0; i < deviceIdAndGeneration.length; i += 2) {
            if (deviceIdAndGeneration[i] == deviceId) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTabletModeChanged(long whenNanos, boolean inTabletMode) {
        synchronized (this.mTabletModeLock) {
            int N = this.mOnTabletModeChangedListeners.size();
            for (int i = 0; i < N; i++) {
                OnTabletModeChangedListenerDelegate listener = this.mOnTabletModeChangedListeners.get(i);
                listener.sendTabletModeChanged(whenNanos, inTabletMode);
            }
        }
    }

    public Vibrator getInputDeviceVibrator(int deviceId, int vibratorId) {
        return new InputDeviceVibrator(this, deviceId, vibratorId);
    }

    public VibratorManager getInputDeviceVibratorManager(int deviceId) {
        return new InputDeviceVibratorManager(this, deviceId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getVibratorIds(int deviceId) {
        try {
            return this.mIm.getVibratorIds(deviceId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void vibrate(int deviceId, VibrationEffect effect, IBinder token) {
        try {
            this.mIm.vibrate(deviceId, effect, token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void vibrate(int deviceId, CombinedVibration effect, IBinder token) {
        try {
            this.mIm.vibrateCombined(deviceId, effect, token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelVibrate(int deviceId, IBinder token) {
        try {
            this.mIm.cancelVibrate(deviceId, token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVibrating(int deviceId) {
        try {
            return this.mIm.isVibrating(deviceId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean registerVibratorStateListener(int deviceId, IVibratorStateListener listener) {
        try {
            return this.mIm.registerVibratorStateListener(deviceId, listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean unregisterVibratorStateListener(int deviceId, IVibratorStateListener listener) {
        try {
            return this.mIm.unregisterVibratorStateListener(deviceId, listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public SensorManager getInputDeviceSensorManager(int deviceId) {
        if (this.mInputDeviceSensorManager == null) {
            this.mInputDeviceSensorManager = new InputDeviceSensorManager(this);
        }
        return this.mInputDeviceSensorManager.getSensorManager(deviceId);
    }

    public InputDeviceBatteryState getInputDeviceBatteryState(int deviceId, boolean hasBattery) {
        return new InputDeviceBatteryState(this, deviceId, hasBattery);
    }

    public LightsManager getInputDeviceLightsManager(int deviceId) {
        return new InputDeviceLightsManager(this, deviceId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Light> getLights(int deviceId) {
        try {
            return this.mIm.getLights(deviceId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LightState getLightState(int deviceId, Light light) {
        try {
            return this.mIm.getLightState(deviceId, light.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestLights(int deviceId, LightsRequest request, IBinder token) {
        try {
            List<Integer> lightIdList = request.getLights();
            int[] lightIds = new int[lightIdList.size()];
            for (int i = 0; i < lightIds.length; i++) {
                lightIds[i] = lightIdList.get(i).intValue();
            }
            List<LightState> lightStateList = request.getLightStates();
            this.mIm.setLightStates(deviceId, lightIds, (LightState[]) lightStateList.toArray(new LightState[lightStateList.size()]), token);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void openLightSession(int deviceId, String opPkg, IBinder token) {
        try {
            this.mIm.openLightSession(deviceId, opPkg, token);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeLightSession(int deviceId, IBinder token) {
        try {
            this.mIm.closeLightSession(deviceId, token);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void cancelCurrentTouch() {
        try {
            this.mIm.cancelCurrentTouch();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class InputDevicesChangedListener extends IInputDevicesChangedListener.Stub {
        private InputDevicesChangedListener() {
        }

        @Override // android.hardware.input.IInputDevicesChangedListener
        public void onInputDevicesChanged(int[] deviceIdAndGeneration) throws RemoteException {
            InputManager.this.onInputDevicesChanged(deviceIdAndGeneration);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class InputDeviceListenerDelegate extends Handler {
        public final InputDeviceListener mListener;

        public InputDeviceListenerDelegate(InputDeviceListener listener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper());
            this.mListener = listener;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    this.mListener.onInputDeviceAdded(msg.arg1);
                    return;
                case 2:
                    this.mListener.onInputDeviceRemoved(msg.arg1);
                    return;
                case 3:
                    this.mListener.onInputDeviceChanged(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class TabletModeChangedListener extends ITabletModeChangedListener.Stub {
        private TabletModeChangedListener() {
        }

        @Override // android.hardware.input.ITabletModeChangedListener
        public void onTabletModeChanged(long whenNanos, boolean inTabletMode) {
            InputManager.this.onTabletModeChanged(whenNanos, inTabletMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class OnTabletModeChangedListenerDelegate extends Handler {
        private static final int MSG_TABLET_MODE_CHANGED = 0;
        public final OnTabletModeChangedListener mListener;

        public OnTabletModeChangedListenerDelegate(OnTabletModeChangedListener listener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper());
            this.mListener = listener;
        }

        public void sendTabletModeChanged(long whenNanos, boolean inTabletMode) {
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = (int) ((-1) & whenNanos);
            args.argi2 = (int) (whenNanos >> 32);
            args.arg1 = Boolean.valueOf(inTabletMode);
            obtainMessage(0, args).sendToTarget();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    SomeArgs args = (SomeArgs) msg.obj;
                    long whenNanos = (args.argi1 & 4294967295L) | (args.argi2 << 32);
                    boolean inTabletMode = ((Boolean) args.arg1).booleanValue();
                    this.mListener.onTabletModeChanged(whenNanos, inTabletMode);
                    return;
                default:
                    return;
            }
        }
    }

    public boolean ignoreMultiWindowGesture() {
        try {
            return this.mIm.ignoreMultiWindowGesture();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
}
