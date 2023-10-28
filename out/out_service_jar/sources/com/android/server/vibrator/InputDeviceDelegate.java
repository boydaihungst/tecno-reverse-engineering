package com.android.server.vibrator;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.VibrationAttributes;
import android.os.VibratorManager;
import android.util.SparseArray;
import android.view.InputDevice;
/* loaded from: classes2.dex */
final class InputDeviceDelegate implements InputManager.InputDeviceListener {
    private static final String TAG = "InputDeviceDelegate";
    private final Context mContext;
    private final Handler mHandler;
    private InputManager mInputManager;
    private boolean mShouldVibrateInputDevices;
    private final Object mLock = new Object();
    private final SparseArray<VibratorManager> mInputDeviceVibrators = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputDeviceDelegate(Context context, Handler handler) {
        this.mHandler = handler;
        this.mContext = context;
    }

    public void onSystemReady() {
        synchronized (this.mLock) {
            this.mInputManager = (InputManager) this.mContext.getSystemService(InputManager.class);
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int deviceId) {
        updateInputDevice(deviceId);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int deviceId) {
        updateInputDevice(deviceId);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int deviceId) {
        synchronized (this.mLock) {
            this.mInputDeviceVibrators.remove(deviceId);
        }
    }

    public boolean isAvailable() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mInputDeviceVibrators.size() > 0;
        }
        return z;
    }

    public boolean vibrateIfAvailable(int uid, String opPkg, CombinedVibration effect, String reason, VibrationAttributes attrs) {
        boolean z;
        synchronized (this.mLock) {
            for (int i = 0; i < this.mInputDeviceVibrators.size(); i++) {
                this.mInputDeviceVibrators.valueAt(i).vibrate(uid, opPkg, effect, reason, attrs);
            }
            z = this.mInputDeviceVibrators.size() > 0;
        }
        return z;
    }

    public boolean cancelVibrateIfAvailable() {
        boolean z;
        synchronized (this.mLock) {
            for (int i = 0; i < this.mInputDeviceVibrators.size(); i++) {
                this.mInputDeviceVibrators.valueAt(i).cancel();
            }
            z = this.mInputDeviceVibrators.size() > 0;
        }
        return z;
    }

    public boolean updateInputDeviceVibrators(boolean vibrateInputDevices) {
        int[] inputDeviceIds;
        synchronized (this.mLock) {
            if (this.mInputManager == null) {
                return false;
            }
            if (vibrateInputDevices == this.mShouldVibrateInputDevices) {
                return false;
            }
            this.mShouldVibrateInputDevices = vibrateInputDevices;
            this.mInputDeviceVibrators.clear();
            if (vibrateInputDevices) {
                this.mInputManager.registerInputDeviceListener(this, this.mHandler);
                for (int deviceId : this.mInputManager.getInputDeviceIds()) {
                    InputDevice device = this.mInputManager.getInputDevice(deviceId);
                    if (device != null) {
                        VibratorManager vibratorManager = device.getVibratorManager();
                        if (vibratorManager.getVibratorIds().length > 0) {
                            this.mInputDeviceVibrators.put(device.getId(), vibratorManager);
                        }
                    }
                }
            } else {
                this.mInputManager.unregisterInputDeviceListener(this);
            }
            return true;
        }
    }

    private void updateInputDevice(int deviceId) {
        synchronized (this.mLock) {
            InputManager inputManager = this.mInputManager;
            if (inputManager == null) {
                return;
            }
            if (this.mShouldVibrateInputDevices) {
                InputDevice device = inputManager.getInputDevice(deviceId);
                if (device == null) {
                    this.mInputDeviceVibrators.remove(deviceId);
                    return;
                }
                VibratorManager vibratorManager = device.getVibratorManager();
                if (vibratorManager.getVibratorIds().length > 0) {
                    this.mInputDeviceVibrators.put(device.getId(), vibratorManager);
                } else {
                    this.mInputDeviceVibrators.remove(deviceId);
                }
            }
        }
    }
}
