package android.hardware.input;

import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.CombinedVibration;
import android.os.NullVibrator;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.SparseArray;
import android.view.InputDevice;
/* loaded from: classes2.dex */
public class InputDeviceVibratorManager extends VibratorManager implements InputManager.InputDeviceListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "InputDeviceVibratorManager";
    private final int mDeviceId;
    private final InputManager mInputManager;
    private final SparseArray<Vibrator> mVibrators = new SparseArray<>();
    private final Binder mToken = new Binder();

    public InputDeviceVibratorManager(InputManager inputManager, int deviceId) {
        this.mInputManager = inputManager;
        this.mDeviceId = deviceId;
        initializeVibrators();
    }

    private void initializeVibrators() {
        synchronized (this.mVibrators) {
            this.mVibrators.clear();
            InputDevice.getDevice(this.mDeviceId);
            int[] vibratorIds = this.mInputManager.getVibratorIds(this.mDeviceId);
            for (int i = 0; i < vibratorIds.length; i++) {
                this.mVibrators.put(vibratorIds[i], new InputDeviceVibrator(this.mInputManager, this.mDeviceId, vibratorIds[i]));
            }
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int deviceId) {
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int deviceId) {
        synchronized (this.mVibrators) {
            if (deviceId == this.mDeviceId) {
                this.mVibrators.clear();
            }
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int deviceId) {
        if (deviceId == this.mDeviceId) {
            initializeVibrators();
        }
    }

    @Override // android.os.VibratorManager
    public int[] getVibratorIds() {
        int[] vibratorIds;
        synchronized (this.mVibrators) {
            vibratorIds = new int[this.mVibrators.size()];
            int idx = 0;
            while (idx < this.mVibrators.size()) {
                int idx2 = idx + 1;
                vibratorIds[idx] = this.mVibrators.keyAt(idx2);
                idx = idx2 + 1;
            }
        }
        return vibratorIds;
    }

    @Override // android.os.VibratorManager
    public Vibrator getVibrator(int vibratorId) {
        synchronized (this.mVibrators) {
            if (this.mVibrators.contains(vibratorId)) {
                return this.mVibrators.get(vibratorId);
            }
            return NullVibrator.getInstance();
        }
    }

    @Override // android.os.VibratorManager
    public Vibrator getDefaultVibrator() {
        synchronized (this.mVibrators) {
            if (this.mVibrators.size() > 0) {
                return this.mVibrators.valueAt(0);
            }
            return NullVibrator.getInstance();
        }
    }

    @Override // android.os.VibratorManager
    public void vibrate(int uid, String opPkg, CombinedVibration effect, String reason, VibrationAttributes attributes) {
        this.mInputManager.vibrate(this.mDeviceId, effect, this.mToken);
    }

    @Override // android.os.VibratorManager
    public void cancel() {
        this.mInputManager.cancelVibrate(this.mDeviceId, this.mToken);
    }

    @Override // android.os.VibratorManager
    public void cancel(int usageFilter) {
        cancel();
    }
}
