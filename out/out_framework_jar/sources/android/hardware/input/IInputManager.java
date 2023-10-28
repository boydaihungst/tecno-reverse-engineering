package android.hardware.input;

import android.hardware.input.IGestureListener;
import android.hardware.input.IInputDevicesChangedListener;
import android.hardware.input.IInputSensorEventListener;
import android.hardware.input.ITabletModeChangedListener;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.os.Binder;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IVibratorStateListener;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.VibrationEffect;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputMonitor;
import android.view.PointerIcon;
import android.view.VerifiedInputEvent;
import java.util.List;
/* loaded from: classes2.dex */
public interface IInputManager extends IInterface {
    void addKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) throws RemoteException;

    void addPortAssociation(String str, int i) throws RemoteException;

    void addUniqueIdAssociation(String str, String str2) throws RemoteException;

    void cancelCurrentTouch() throws RemoteException;

    void cancelVibrate(int i, IBinder iBinder) throws RemoteException;

    void closeLightSession(int i, IBinder iBinder) throws RemoteException;

    void disableInputDevice(int i) throws RemoteException;

    void disableSensor(int i, int i2) throws RemoteException;

    void enableInputDevice(int i) throws RemoteException;

    boolean enableSensor(int i, int i2, int i3, int i4) throws RemoteException;

    boolean flushSensor(int i, int i2) throws RemoteException;

    int getBatteryCapacity(int i) throws RemoteException;

    int getBatteryStatus(int i) throws RemoteException;

    String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) throws RemoteException;

    String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) throws RemoteException;

    InputDevice getInputDevice(int i) throws RemoteException;

    int[] getInputDeviceIds() throws RemoteException;

    int getKeyCodeForKeyLocation(int i, int i2) throws RemoteException;

    KeyboardLayout getKeyboardLayout(String str) throws RemoteException;

    KeyboardLayout[] getKeyboardLayouts() throws RemoteException;

    KeyboardLayout[] getKeyboardLayoutsForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) throws RemoteException;

    LightState getLightState(int i, int i2) throws RemoteException;

    List<Light> getLights(int i) throws RemoteException;

    String getMagellanConfig() throws RemoteException;

    InputSensorInfo[] getSensorList(int i) throws RemoteException;

    TouchCalibration getTouchCalibrationForInputDevice(String str, int i) throws RemoteException;

    int[] getVibratorIds(int i) throws RemoteException;

    boolean hasKeys(int i, int i2, int[] iArr, boolean[] zArr) throws RemoteException;

    boolean ignoreMultiWindowGesture() throws RemoteException;

    boolean injectInputEvent(InputEvent inputEvent, int i) throws RemoteException;

    boolean injectInputEventToTarget(InputEvent inputEvent, int i, int i2) throws RemoteException;

    int isInTabletMode() throws RemoteException;

    boolean isInputDeviceEnabled(int i) throws RemoteException;

    int isMicMuted() throws RemoteException;

    boolean isVibrating(int i) throws RemoteException;

    InputMonitor monitorGestureInput(IBinder iBinder, String str, int i) throws RemoteException;

    void openLightSession(int i, String str, IBinder iBinder) throws RemoteException;

    void registerGestureListener(IGestureListener iGestureListener) throws RemoteException;

    void registerInputDevicesChangedListener(IInputDevicesChangedListener iInputDevicesChangedListener) throws RemoteException;

    boolean registerSensorListener(IInputSensorEventListener iInputSensorEventListener) throws RemoteException;

    void registerTabletModeChangedListener(ITabletModeChangedListener iTabletModeChangedListener) throws RemoteException;

    boolean registerVibratorStateListener(int i, IVibratorStateListener iVibratorStateListener) throws RemoteException;

    void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) throws RemoteException;

    void removePortAssociation(String str) throws RemoteException;

    void removeUniqueIdAssociation(String str) throws RemoteException;

    void requestPointerCapture(IBinder iBinder, boolean z) throws RemoteException;

    void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) throws RemoteException;

    void setCustomPointerIcon(PointerIcon pointerIcon) throws RemoteException;

    void setLightStates(int i, int[] iArr, LightState[] lightStateArr, IBinder iBinder) throws RemoteException;

    void setPointerIconType(int i) throws RemoteException;

    void setTouchCalibrationForInputDevice(String str, int i, TouchCalibration touchCalibration) throws RemoteException;

    void tryPointerSpeed(int i) throws RemoteException;

    void unRegisterGestureListener(IGestureListener iGestureListener) throws RemoteException;

    void unregisterSensorListener(IInputSensorEventListener iInputSensorEventListener) throws RemoteException;

    boolean unregisterVibratorStateListener(int i, IVibratorStateListener iVibratorStateListener) throws RemoteException;

    boolean updateTrackingData(int i, Bundle bundle) throws RemoteException;

    VerifiedInputEvent verifyInputEvent(InputEvent inputEvent) throws RemoteException;

    void vibrate(int i, VibrationEffect vibrationEffect, IBinder iBinder) throws RemoteException;

    void vibrateCombined(int i, CombinedVibration combinedVibration, IBinder iBinder) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IInputManager {
        @Override // android.hardware.input.IInputManager
        public InputDevice getInputDevice(int deviceId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public int[] getInputDeviceIds() throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public boolean isInputDeviceEnabled(int deviceId) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public void enableInputDevice(int deviceId) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void disableInputDevice(int deviceId) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public boolean hasKeys(int deviceId, int sourceMask, int[] keyCodes, boolean[] keyExists) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.input.IInputManager
        public void tryPointerSpeed(int speed) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public boolean injectInputEvent(InputEvent ev, int mode) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public boolean injectInputEventToTarget(InputEvent ev, int mode, int targetUid) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public VerifiedInputEvent verifyInputEvent(InputEvent ev) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public TouchCalibration getTouchCalibrationForInputDevice(String inputDeviceDescriptor, int rotation) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public void setTouchCalibrationForInputDevice(String inputDeviceDescriptor, int rotation, TouchCalibration calibration) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public KeyboardLayout[] getKeyboardLayouts() throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public KeyboardLayout[] getKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public KeyboardLayout getKeyboardLayout(String keyboardLayoutDescriptor) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void registerInputDevicesChangedListener(IInputDevicesChangedListener listener) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public int isInTabletMode() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.input.IInputManager
        public void registerTabletModeChangedListener(ITabletModeChangedListener listener) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public int isMicMuted() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.input.IInputManager
        public void vibrate(int deviceId, VibrationEffect effect, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void vibrateCombined(int deviceId, CombinedVibration vibration, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void cancelVibrate(int deviceId, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public int[] getVibratorIds(int deviceId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public boolean isVibrating(int deviceId) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public boolean registerVibratorStateListener(int deviceId, IVibratorStateListener listener) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public boolean unregisterVibratorStateListener(int deviceId, IVibratorStateListener listener) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public int getBatteryStatus(int deviceId) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.input.IInputManager
        public int getBatteryCapacity(int deviceId) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.input.IInputManager
        public void setPointerIconType(int typeId) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void setCustomPointerIcon(PointerIcon icon) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void requestPointerCapture(IBinder inputChannelToken, boolean enabled) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public InputMonitor monitorGestureInput(IBinder token, String name, int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public void addPortAssociation(String inputPort, int displayPort) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void removePortAssociation(String inputPort) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void addUniqueIdAssociation(String inputPort, String displayUniqueId) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void removeUniqueIdAssociation(String inputPort) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public InputSensorInfo[] getSensorList(int deviceId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public boolean registerSensorListener(IInputSensorEventListener listener) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public void unregisterSensorListener(IInputSensorEventListener listener) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs, int maxBatchReportLatencyUs) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public void disableSensor(int deviceId, int sensorType) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public boolean flushSensor(int deviceId, int sensorType) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public List<Light> getLights(int deviceId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public LightState getLightState(int deviceId, int lightId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public void setLightStates(int deviceId, int[] lightIds, LightState[] states, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void openLightSession(int deviceId, String opPkg, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void closeLightSession(int deviceId, IBinder token) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void cancelCurrentTouch() throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void registerGestureListener(IGestureListener listener) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public void unRegisterGestureListener(IGestureListener listener) throws RemoteException {
        }

        @Override // android.hardware.input.IInputManager
        public String getMagellanConfig() throws RemoteException {
            return null;
        }

        @Override // android.hardware.input.IInputManager
        public boolean updateTrackingData(int type, Bundle trackingData) throws RemoteException {
            return false;
        }

        @Override // android.hardware.input.IInputManager
        public boolean ignoreMultiWindowGesture() throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IInputManager {
        public static final String DESCRIPTOR = "android.hardware.input.IInputManager";
        static final int TRANSACTION_addKeyboardLayoutForInputDevice = 20;
        static final int TRANSACTION_addPortAssociation = 39;
        static final int TRANSACTION_addUniqueIdAssociation = 41;
        static final int TRANSACTION_cancelCurrentTouch = 54;
        static final int TRANSACTION_cancelVibrate = 28;
        static final int TRANSACTION_closeLightSession = 53;
        static final int TRANSACTION_disableInputDevice = 5;
        static final int TRANSACTION_disableSensor = 47;
        static final int TRANSACTION_enableInputDevice = 4;
        static final int TRANSACTION_enableSensor = 46;
        static final int TRANSACTION_flushSensor = 48;
        static final int TRANSACTION_getBatteryCapacity = 34;
        static final int TRANSACTION_getBatteryStatus = 33;
        static final int TRANSACTION_getCurrentKeyboardLayoutForInputDevice = 17;
        static final int TRANSACTION_getEnabledKeyboardLayoutsForInputDevice = 19;
        static final int TRANSACTION_getInputDevice = 1;
        static final int TRANSACTION_getInputDeviceIds = 2;
        static final int TRANSACTION_getKeyCodeForKeyLocation = 7;
        static final int TRANSACTION_getKeyboardLayout = 16;
        static final int TRANSACTION_getKeyboardLayouts = 14;
        static final int TRANSACTION_getKeyboardLayoutsForInputDevice = 15;
        static final int TRANSACTION_getLightState = 50;
        static final int TRANSACTION_getLights = 49;
        static final int TRANSACTION_getMagellanConfig = 57;
        static final int TRANSACTION_getSensorList = 43;
        static final int TRANSACTION_getTouchCalibrationForInputDevice = 12;
        static final int TRANSACTION_getVibratorIds = 29;
        static final int TRANSACTION_hasKeys = 6;
        static final int TRANSACTION_ignoreMultiWindowGesture = 59;
        static final int TRANSACTION_injectInputEvent = 9;
        static final int TRANSACTION_injectInputEventToTarget = 10;
        static final int TRANSACTION_isInTabletMode = 23;
        static final int TRANSACTION_isInputDeviceEnabled = 3;
        static final int TRANSACTION_isMicMuted = 25;
        static final int TRANSACTION_isVibrating = 30;
        static final int TRANSACTION_monitorGestureInput = 38;
        static final int TRANSACTION_openLightSession = 52;
        static final int TRANSACTION_registerGestureListener = 55;
        static final int TRANSACTION_registerInputDevicesChangedListener = 22;
        static final int TRANSACTION_registerSensorListener = 44;
        static final int TRANSACTION_registerTabletModeChangedListener = 24;
        static final int TRANSACTION_registerVibratorStateListener = 31;
        static final int TRANSACTION_removeKeyboardLayoutForInputDevice = 21;
        static final int TRANSACTION_removePortAssociation = 40;
        static final int TRANSACTION_removeUniqueIdAssociation = 42;
        static final int TRANSACTION_requestPointerCapture = 37;
        static final int TRANSACTION_setCurrentKeyboardLayoutForInputDevice = 18;
        static final int TRANSACTION_setCustomPointerIcon = 36;
        static final int TRANSACTION_setLightStates = 51;
        static final int TRANSACTION_setPointerIconType = 35;
        static final int TRANSACTION_setTouchCalibrationForInputDevice = 13;
        static final int TRANSACTION_tryPointerSpeed = 8;
        static final int TRANSACTION_unRegisterGestureListener = 56;
        static final int TRANSACTION_unregisterSensorListener = 45;
        static final int TRANSACTION_unregisterVibratorStateListener = 32;
        static final int TRANSACTION_updateTrackingData = 58;
        static final int TRANSACTION_verifyInputEvent = 11;
        static final int TRANSACTION_vibrate = 26;
        static final int TRANSACTION_vibrateCombined = 27;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInputManager)) {
                return (IInputManager) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "getInputDevice";
                case 2:
                    return "getInputDeviceIds";
                case 3:
                    return "isInputDeviceEnabled";
                case 4:
                    return "enableInputDevice";
                case 5:
                    return "disableInputDevice";
                case 6:
                    return "hasKeys";
                case 7:
                    return "getKeyCodeForKeyLocation";
                case 8:
                    return "tryPointerSpeed";
                case 9:
                    return "injectInputEvent";
                case 10:
                    return "injectInputEventToTarget";
                case 11:
                    return "verifyInputEvent";
                case 12:
                    return "getTouchCalibrationForInputDevice";
                case 13:
                    return "setTouchCalibrationForInputDevice";
                case 14:
                    return "getKeyboardLayouts";
                case 15:
                    return "getKeyboardLayoutsForInputDevice";
                case 16:
                    return "getKeyboardLayout";
                case 17:
                    return "getCurrentKeyboardLayoutForInputDevice";
                case 18:
                    return "setCurrentKeyboardLayoutForInputDevice";
                case 19:
                    return "getEnabledKeyboardLayoutsForInputDevice";
                case 20:
                    return "addKeyboardLayoutForInputDevice";
                case 21:
                    return "removeKeyboardLayoutForInputDevice";
                case 22:
                    return "registerInputDevicesChangedListener";
                case 23:
                    return "isInTabletMode";
                case 24:
                    return "registerTabletModeChangedListener";
                case 25:
                    return "isMicMuted";
                case 26:
                    return "vibrate";
                case 27:
                    return "vibrateCombined";
                case 28:
                    return "cancelVibrate";
                case 29:
                    return "getVibratorIds";
                case 30:
                    return "isVibrating";
                case 31:
                    return "registerVibratorStateListener";
                case 32:
                    return "unregisterVibratorStateListener";
                case 33:
                    return "getBatteryStatus";
                case 34:
                    return "getBatteryCapacity";
                case 35:
                    return "setPointerIconType";
                case 36:
                    return "setCustomPointerIcon";
                case 37:
                    return "requestPointerCapture";
                case 38:
                    return "monitorGestureInput";
                case 39:
                    return "addPortAssociation";
                case 40:
                    return "removePortAssociation";
                case 41:
                    return "addUniqueIdAssociation";
                case 42:
                    return "removeUniqueIdAssociation";
                case 43:
                    return "getSensorList";
                case 44:
                    return "registerSensorListener";
                case 45:
                    return "unregisterSensorListener";
                case 46:
                    return "enableSensor";
                case 47:
                    return "disableSensor";
                case 48:
                    return "flushSensor";
                case 49:
                    return "getLights";
                case 50:
                    return "getLightState";
                case 51:
                    return "setLightStates";
                case 52:
                    return "openLightSession";
                case 53:
                    return "closeLightSession";
                case 54:
                    return "cancelCurrentTouch";
                case 55:
                    return "registerGestureListener";
                case 56:
                    return "unRegisterGestureListener";
                case 57:
                    return "getMagellanConfig";
                case 58:
                    return "updateTrackingData";
                case 59:
                    return "ignoreMultiWindowGesture";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            boolean[] _arg3;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            InputDevice _result = getInputDevice(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            int[] _result2 = getInputDeviceIds();
                            reply.writeNoException();
                            reply.writeIntArray(_result2);
                            break;
                        case 3:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result3 = isInputDeviceEnabled(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 4:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            enableInputDevice(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            disableInputDevice(_arg04);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _arg05 = data.readInt();
                            int _arg1 = data.readInt();
                            int[] _arg2 = data.createIntArray();
                            int _arg3_length = data.readInt();
                            if (_arg3_length < 0) {
                                _arg3 = null;
                            } else {
                                _arg3 = new boolean[_arg3_length];
                            }
                            data.enforceNoDataAvail();
                            boolean _result4 = hasKeys(_arg05, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            reply.writeBooleanArray(_arg3);
                            break;
                        case 7:
                            int _arg06 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result5 = getKeyCodeForKeyLocation(_arg06, _arg12);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 8:
                            int _arg07 = data.readInt();
                            data.enforceNoDataAvail();
                            tryPointerSpeed(_arg07);
                            reply.writeNoException();
                            break;
                        case 9:
                            InputEvent _arg08 = (InputEvent) data.readTypedObject(InputEvent.CREATOR);
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = injectInputEvent(_arg08, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 10:
                            InputEvent _arg09 = (InputEvent) data.readTypedObject(InputEvent.CREATOR);
                            int _arg14 = data.readInt();
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result7 = injectInputEventToTarget(_arg09, _arg14, _arg22);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 11:
                            InputEvent _arg010 = (InputEvent) data.readTypedObject(InputEvent.CREATOR);
                            data.enforceNoDataAvail();
                            VerifiedInputEvent _result8 = verifyInputEvent(_arg010);
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            break;
                        case 12:
                            String _arg011 = data.readString();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            TouchCalibration _result9 = getTouchCalibrationForInputDevice(_arg011, _arg15);
                            reply.writeNoException();
                            reply.writeTypedObject(_result9, 1);
                            break;
                        case 13:
                            String _arg012 = data.readString();
                            int _arg16 = data.readInt();
                            TouchCalibration _arg23 = (TouchCalibration) data.readTypedObject(TouchCalibration.CREATOR);
                            data.enforceNoDataAvail();
                            setTouchCalibrationForInputDevice(_arg012, _arg16, _arg23);
                            reply.writeNoException();
                            break;
                        case 14:
                            KeyboardLayout[] _result10 = getKeyboardLayouts();
                            reply.writeNoException();
                            reply.writeTypedArray(_result10, 1);
                            break;
                        case 15:
                            InputDeviceIdentifier _arg013 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            data.enforceNoDataAvail();
                            KeyboardLayout[] _result11 = getKeyboardLayoutsForInputDevice(_arg013);
                            reply.writeNoException();
                            reply.writeTypedArray(_result11, 1);
                            break;
                        case 16:
                            String _arg014 = data.readString();
                            data.enforceNoDataAvail();
                            KeyboardLayout _result12 = getKeyboardLayout(_arg014);
                            reply.writeNoException();
                            reply.writeTypedObject(_result12, 1);
                            break;
                        case 17:
                            InputDeviceIdentifier _arg015 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            data.enforceNoDataAvail();
                            String _result13 = getCurrentKeyboardLayoutForInputDevice(_arg015);
                            reply.writeNoException();
                            reply.writeString(_result13);
                            break;
                        case 18:
                            InputDeviceIdentifier _arg016 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            setCurrentKeyboardLayoutForInputDevice(_arg016, _arg17);
                            reply.writeNoException();
                            break;
                        case 19:
                            InputDeviceIdentifier _arg017 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            data.enforceNoDataAvail();
                            String[] _result14 = getEnabledKeyboardLayoutsForInputDevice(_arg017);
                            reply.writeNoException();
                            reply.writeStringArray(_result14);
                            break;
                        case 20:
                            InputDeviceIdentifier _arg018 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            String _arg18 = data.readString();
                            data.enforceNoDataAvail();
                            addKeyboardLayoutForInputDevice(_arg018, _arg18);
                            reply.writeNoException();
                            break;
                        case 21:
                            InputDeviceIdentifier _arg019 = (InputDeviceIdentifier) data.readTypedObject(InputDeviceIdentifier.CREATOR);
                            String _arg19 = data.readString();
                            data.enforceNoDataAvail();
                            removeKeyboardLayoutForInputDevice(_arg019, _arg19);
                            reply.writeNoException();
                            break;
                        case 22:
                            IInputDevicesChangedListener _arg020 = IInputDevicesChangedListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerInputDevicesChangedListener(_arg020);
                            reply.writeNoException();
                            break;
                        case 23:
                            int _result15 = isInTabletMode();
                            reply.writeNoException();
                            reply.writeInt(_result15);
                            break;
                        case 24:
                            ITabletModeChangedListener _arg021 = ITabletModeChangedListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerTabletModeChangedListener(_arg021);
                            reply.writeNoException();
                            break;
                        case 25:
                            int _result16 = isMicMuted();
                            reply.writeNoException();
                            reply.writeInt(_result16);
                            break;
                        case 26:
                            int _arg022 = data.readInt();
                            VibrationEffect _arg110 = (VibrationEffect) data.readTypedObject(VibrationEffect.CREATOR);
                            IBinder _arg24 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            vibrate(_arg022, _arg110, _arg24);
                            reply.writeNoException();
                            break;
                        case 27:
                            int _arg023 = data.readInt();
                            CombinedVibration _arg111 = (CombinedVibration) data.readTypedObject(CombinedVibration.CREATOR);
                            IBinder _arg25 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            vibrateCombined(_arg023, _arg111, _arg25);
                            reply.writeNoException();
                            break;
                        case 28:
                            int _arg024 = data.readInt();
                            IBinder _arg112 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            cancelVibrate(_arg024, _arg112);
                            reply.writeNoException();
                            break;
                        case 29:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            int[] _result17 = getVibratorIds(_arg025);
                            reply.writeNoException();
                            reply.writeIntArray(_result17);
                            break;
                        case 30:
                            int _arg026 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result18 = isVibrating(_arg026);
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            break;
                        case 31:
                            int _arg027 = data.readInt();
                            IVibratorStateListener _arg113 = IVibratorStateListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result19 = registerVibratorStateListener(_arg027, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            break;
                        case 32:
                            int _arg028 = data.readInt();
                            IVibratorStateListener _arg114 = IVibratorStateListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result20 = unregisterVibratorStateListener(_arg028, _arg114);
                            reply.writeNoException();
                            reply.writeBoolean(_result20);
                            break;
                        case 33:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result21 = getBatteryStatus(_arg029);
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            break;
                        case 34:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result22 = getBatteryCapacity(_arg030);
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            break;
                        case 35:
                            int _arg031 = data.readInt();
                            data.enforceNoDataAvail();
                            setPointerIconType(_arg031);
                            reply.writeNoException();
                            break;
                        case 36:
                            PointerIcon _arg032 = (PointerIcon) data.readTypedObject(PointerIcon.CREATOR);
                            data.enforceNoDataAvail();
                            setCustomPointerIcon(_arg032);
                            reply.writeNoException();
                            break;
                        case 37:
                            IBinder _arg033 = data.readStrongBinder();
                            boolean _arg115 = data.readBoolean();
                            data.enforceNoDataAvail();
                            requestPointerCapture(_arg033, _arg115);
                            break;
                        case 38:
                            IBinder _arg034 = data.readStrongBinder();
                            String _arg116 = data.readString();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            InputMonitor _result23 = monitorGestureInput(_arg034, _arg116, _arg26);
                            reply.writeNoException();
                            reply.writeTypedObject(_result23, 1);
                            break;
                        case 39:
                            String _arg035 = data.readString();
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            addPortAssociation(_arg035, _arg117);
                            reply.writeNoException();
                            break;
                        case 40:
                            String _arg036 = data.readString();
                            data.enforceNoDataAvail();
                            removePortAssociation(_arg036);
                            reply.writeNoException();
                            break;
                        case 41:
                            String _arg037 = data.readString();
                            String _arg118 = data.readString();
                            data.enforceNoDataAvail();
                            addUniqueIdAssociation(_arg037, _arg118);
                            reply.writeNoException();
                            break;
                        case 42:
                            String _arg038 = data.readString();
                            data.enforceNoDataAvail();
                            removeUniqueIdAssociation(_arg038);
                            reply.writeNoException();
                            break;
                        case 43:
                            int _arg039 = data.readInt();
                            data.enforceNoDataAvail();
                            InputSensorInfo[] _result24 = getSensorList(_arg039);
                            reply.writeNoException();
                            reply.writeTypedArray(_result24, 1);
                            break;
                        case 44:
                            IInputSensorEventListener _arg040 = IInputSensorEventListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result25 = registerSensorListener(_arg040);
                            reply.writeNoException();
                            reply.writeBoolean(_result25);
                            break;
                        case 45:
                            IInputSensorEventListener _arg041 = IInputSensorEventListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterSensorListener(_arg041);
                            reply.writeNoException();
                            break;
                        case 46:
                            int _arg042 = data.readInt();
                            int _arg119 = data.readInt();
                            int _arg27 = data.readInt();
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result26 = enableSensor(_arg042, _arg119, _arg27, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result26);
                            break;
                        case 47:
                            int _arg043 = data.readInt();
                            int _arg120 = data.readInt();
                            data.enforceNoDataAvail();
                            disableSensor(_arg043, _arg120);
                            reply.writeNoException();
                            break;
                        case 48:
                            int _arg044 = data.readInt();
                            int _arg121 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result27 = flushSensor(_arg044, _arg121);
                            reply.writeNoException();
                            reply.writeBoolean(_result27);
                            break;
                        case 49:
                            int _arg045 = data.readInt();
                            data.enforceNoDataAvail();
                            List<Light> _result28 = getLights(_arg045);
                            reply.writeNoException();
                            reply.writeTypedList(_result28);
                            break;
                        case 50:
                            int _arg046 = data.readInt();
                            int _arg122 = data.readInt();
                            data.enforceNoDataAvail();
                            LightState _result29 = getLightState(_arg046, _arg122);
                            reply.writeNoException();
                            reply.writeTypedObject(_result29, 1);
                            break;
                        case 51:
                            int _arg047 = data.readInt();
                            int[] _arg123 = data.createIntArray();
                            LightState[] _arg28 = (LightState[]) data.createTypedArray(LightState.CREATOR);
                            IBinder _arg33 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            setLightStates(_arg047, _arg123, _arg28, _arg33);
                            reply.writeNoException();
                            break;
                        case 52:
                            int _arg048 = data.readInt();
                            String _arg124 = data.readString();
                            IBinder _arg29 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            openLightSession(_arg048, _arg124, _arg29);
                            reply.writeNoException();
                            break;
                        case 53:
                            int _arg049 = data.readInt();
                            IBinder _arg125 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            closeLightSession(_arg049, _arg125);
                            reply.writeNoException();
                            break;
                        case 54:
                            cancelCurrentTouch();
                            reply.writeNoException();
                            break;
                        case 55:
                            IGestureListener _arg050 = IGestureListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerGestureListener(_arg050);
                            reply.writeNoException();
                            break;
                        case 56:
                            IGestureListener _arg051 = IGestureListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unRegisterGestureListener(_arg051);
                            reply.writeNoException();
                            break;
                        case 57:
                            String _result30 = getMagellanConfig();
                            reply.writeNoException();
                            reply.writeString(_result30);
                            break;
                        case 58:
                            int _arg052 = data.readInt();
                            Bundle _arg126 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result31 = updateTrackingData(_arg052, _arg126);
                            reply.writeNoException();
                            reply.writeBoolean(_result31);
                            break;
                        case 59:
                            boolean _result32 = ignoreMultiWindowGesture();
                            reply.writeNoException();
                            reply.writeBoolean(_result32);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class Proxy implements IInputManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.hardware.input.IInputManager
            public InputDevice getInputDevice(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    InputDevice _result = (InputDevice) _reply.readTypedObject(InputDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int[] getInputDeviceIds() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean isInputDeviceEnabled(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void enableInputDevice(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void disableInputDevice(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean hasKeys(int deviceId, int sourceMask, int[] keyCodes, boolean[] keyExists) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(sourceMask);
                    _data.writeIntArray(keyCodes);
                    if (keyExists == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(keyExists.length);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    _reply.readBooleanArray(keyExists);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(locationKeyCode);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void tryPointerSpeed(int speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(speed);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean injectInputEvent(InputEvent ev, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(ev, 0);
                    _data.writeInt(mode);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean injectInputEventToTarget(InputEvent ev, int mode, int targetUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(ev, 0);
                    _data.writeInt(mode);
                    _data.writeInt(targetUid);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public VerifiedInputEvent verifyInputEvent(InputEvent ev) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(ev, 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    VerifiedInputEvent _result = (VerifiedInputEvent) _reply.readTypedObject(VerifiedInputEvent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public TouchCalibration getTouchCalibrationForInputDevice(String inputDeviceDescriptor, int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputDeviceDescriptor);
                    _data.writeInt(rotation);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    TouchCalibration _result = (TouchCalibration) _reply.readTypedObject(TouchCalibration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void setTouchCalibrationForInputDevice(String inputDeviceDescriptor, int rotation, TouchCalibration calibration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputDeviceDescriptor);
                    _data.writeInt(rotation);
                    _data.writeTypedObject(calibration, 0);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public KeyboardLayout[] getKeyboardLayouts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    KeyboardLayout[] _result = (KeyboardLayout[]) _reply.createTypedArray(KeyboardLayout.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public KeyboardLayout[] getKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    KeyboardLayout[] _result = (KeyboardLayout[]) _reply.createTypedArray(KeyboardLayout.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public KeyboardLayout getKeyboardLayout(String keyboardLayoutDescriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(keyboardLayoutDescriptor);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    KeyboardLayout _result = (KeyboardLayout) _reply.readTypedObject(KeyboardLayout.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    _data.writeString(keyboardLayoutDescriptor);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier identifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    _data.writeString(keyboardLayoutDescriptor);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier identifier, String keyboardLayoutDescriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(identifier, 0);
                    _data.writeString(keyboardLayoutDescriptor);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void registerInputDevicesChangedListener(IInputDevicesChangedListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int isInTabletMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void registerTabletModeChangedListener(ITabletModeChangedListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int isMicMuted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void vibrate(int deviceId, VibrationEffect effect, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeTypedObject(effect, 0);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void vibrateCombined(int deviceId, CombinedVibration vibration, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeTypedObject(vibration, 0);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void cancelVibrate(int deviceId, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int[] getVibratorIds(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean isVibrating(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean registerVibratorStateListener(int deviceId, IVibratorStateListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean unregisterVibratorStateListener(int deviceId, IVibratorStateListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int getBatteryStatus(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public int getBatteryCapacity(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void setPointerIconType(int typeId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(typeId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void setCustomPointerIcon(PointerIcon icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(icon, 0);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void requestPointerCapture(IBinder inputChannelToken, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(inputChannelToken);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public InputMonitor monitorGestureInput(IBinder token, String name, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(name);
                    _data.writeInt(displayId);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    InputMonitor _result = (InputMonitor) _reply.readTypedObject(InputMonitor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void addPortAssociation(String inputPort, int displayPort) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputPort);
                    _data.writeInt(displayPort);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void removePortAssociation(String inputPort) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputPort);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void addUniqueIdAssociation(String inputPort, String displayUniqueId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputPort);
                    _data.writeString(displayUniqueId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void removeUniqueIdAssociation(String inputPort) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputPort);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public InputSensorInfo[] getSensorList(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    InputSensorInfo[] _result = (InputSensorInfo[]) _reply.createTypedArray(InputSensorInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean registerSensorListener(IInputSensorEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void unregisterSensorListener(IInputSensorEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs, int maxBatchReportLatencyUs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(sensorType);
                    _data.writeInt(samplingPeriodUs);
                    _data.writeInt(maxBatchReportLatencyUs);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void disableSensor(int deviceId, int sensorType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(sensorType);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean flushSensor(int deviceId, int sensorType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(sensorType);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public List<Light> getLights(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    List<Light> _result = _reply.createTypedArrayList(Light.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public LightState getLightState(int deviceId, int lightId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeInt(lightId);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    LightState _result = (LightState) _reply.readTypedObject(LightState.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void setLightStates(int deviceId, int[] lightIds, LightState[] states, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeIntArray(lightIds);
                    _data.writeTypedArray(states, 0);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void openLightSession(int deviceId, String opPkg, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeString(opPkg);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void closeLightSession(int deviceId, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void cancelCurrentTouch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void registerGestureListener(IGestureListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public void unRegisterGestureListener(IGestureListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public String getMagellanConfig() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean updateTrackingData(int type, Bundle trackingData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeTypedObject(trackingData, 0);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.input.IInputManager
            public boolean ignoreMultiWindowGesture() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 58;
        }
    }
}
