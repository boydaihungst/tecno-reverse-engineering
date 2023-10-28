package android.os;

import android.os.IWakeLockCallback;
import java.util.List;
/* loaded from: classes2.dex */
public interface IPowerManager extends IInterface {
    public static final int GO_TO_SLEEP_FLAG_NO_DOZE = 1;
    public static final int GO_TO_SLEEP_REASON_ACCESSIBILITY = 7;
    public static final int GO_TO_SLEEP_REASON_APPLICATION = 0;
    public static final int GO_TO_SLEEP_REASON_FORCE_SUSPEND = 8;
    public static final int GO_TO_SLEEP_REASON_HDMI = 5;
    public static final int GO_TO_SLEEP_REASON_INATTENTIVE = 9;
    public static final int GO_TO_SLEEP_REASON_LID_SWITCH = 3;
    public static final int GO_TO_SLEEP_REASON_MAX = 10;
    public static final int GO_TO_SLEEP_REASON_MIN = 0;
    public static final int GO_TO_SLEEP_REASON_POWER_BUTTON = 4;
    public static final int GO_TO_SLEEP_REASON_QUIESCENT = 10;
    public static final int GO_TO_SLEEP_REASON_SLEEP_BUTTON = 6;
    public static final int GO_TO_SLEEP_REASON_TIMEOUT = 2;
    public static final int LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF = 2;
    public static final int LOCATION_MODE_FOREGROUND_ONLY = 3;
    public static final int LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF = 1;
    public static final int LOCATION_MODE_NO_CHANGE = 0;
    public static final int LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF = 4;
    public static final int MAX_LOCATION_MODE = 4;
    public static final int MIN_LOCATION_MODE = 0;

    void acquireWakeLock(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3, int i2, IWakeLockCallback iWakeLockCallback) throws RemoteException;

    void acquireWakeLockAsync(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3) throws RemoteException;

    void acquireWakeLockWithUid(IBinder iBinder, int i, String str, String str2, int i2, int i3, IWakeLockCallback iWakeLockCallback) throws RemoteException;

    void boostScreenBrightness(long j) throws RemoteException;

    void crash(String str) throws RemoteException;

    void forceLowPowerStandbyActive(boolean z) throws RemoteException;

    boolean forceSuspend() throws RemoteException;

    List<String> getAcquireableWakeLockApp() throws RemoteException;

    ParcelDuration getBatteryDischargePrediction() throws RemoteException;

    float getBrightnessConstraint(int i) throws RemoteException;

    BatterySaverPolicyConfig getFullPowerSavePolicy() throws RemoteException;

    int getLastShutdownReason() throws RemoteException;

    int getLastSleepReason() throws RemoteException;

    int getPowerSaveModeTrigger() throws RemoteException;

    PowerSaveState getPowerSaveState(int i) throws RemoteException;

    boolean getScreenOnManagerEnable() throws RemoteException;

    List<String> getUnacquireableWakeLockApp() throws RemoteException;

    List<String> getWakeLockPkgs() throws RemoteException;

    void goToSleep(long j, int i, int i2) throws RemoteException;

    boolean isAmbientDisplayAvailable() throws RemoteException;

    boolean isAmbientDisplaySuppressed() throws RemoteException;

    boolean isAmbientDisplaySuppressedForToken(String str) throws RemoteException;

    boolean isAmbientDisplaySuppressedForTokenByApp(String str, int i) throws RemoteException;

    boolean isBatteryDischargePredictionPersonalized() throws RemoteException;

    boolean isDeviceIdleMode() throws RemoteException;

    boolean isInteractive() throws RemoteException;

    boolean isLightDeviceIdleMode() throws RemoteException;

    boolean isLowPowerStandbyEnabled() throws RemoteException;

    boolean isLowPowerStandbySupported() throws RemoteException;

    void isNotifyScreenOn(boolean z, long j) throws RemoteException;

    boolean isPowerSaveMode() throws RemoteException;

    boolean isScreenBrightnessBoosted() throws RemoteException;

    boolean isWakeLockLevelSupported(int i) throws RemoteException;

    void nap(long j) throws RemoteException;

    void notifyChangeConnectState(boolean z) throws RemoteException;

    void reboot(boolean z, String str, boolean z2) throws RemoteException;

    void rebootSafeMode(boolean z, boolean z2) throws RemoteException;

    void releaseWakeLock(IBinder iBinder, int i) throws RemoteException;

    void releaseWakeLockAsync(IBinder iBinder, int i) throws RemoteException;

    boolean setAdaptivePowerSaveEnabled(boolean z) throws RemoteException;

    boolean setAdaptivePowerSavePolicy(BatterySaverPolicyConfig batterySaverPolicyConfig) throws RemoteException;

    void setAttentionLight(boolean z, int i) throws RemoteException;

    void setBatteryDischargePrediction(ParcelDuration parcelDuration, boolean z) throws RemoteException;

    void setDozeAfterScreenOff(boolean z) throws RemoteException;

    boolean setDynamicPowerSaveHint(boolean z, int i) throws RemoteException;

    boolean setFullPowerSavePolicy(BatterySaverPolicyConfig batterySaverPolicyConfig) throws RemoteException;

    void setLowPowerStandbyActiveDuringMaintenance(boolean z) throws RemoteException;

    void setLowPowerStandbyEnabled(boolean z) throws RemoteException;

    void setPowerBoost(int i, int i2) throws RemoteException;

    void setPowerMode(int i, boolean z) throws RemoteException;

    boolean setPowerModeChecked(int i, boolean z) throws RemoteException;

    boolean setPowerSaveModeEnabled(boolean z) throws RemoteException;

    void setScreenOnManagerEnable(boolean z) throws RemoteException;

    void setStayOnSetting(int i) throws RemoteException;

    void setTorch(boolean z) throws RemoteException;

    void setWakeLockAppMap(String str, boolean z) throws RemoteException;

    void shutdown(boolean z, String str, boolean z2) throws RemoteException;

    void suppressAmbientDisplay(String str, boolean z) throws RemoteException;

    void updateWakeLockCallback(IBinder iBinder, IWakeLockCallback iWakeLockCallback) throws RemoteException;

    void updateWakeLockUids(IBinder iBinder, int[] iArr) throws RemoteException;

    void updateWakeLockUidsAsync(IBinder iBinder, int[] iArr) throws RemoteException;

    void updateWakeLockWorkSource(IBinder iBinder, WorkSource workSource, String str) throws RemoteException;

    void userActivity(int i, long j, int i2, int i3) throws RemoteException;

    void wakeUp(long j, int i, String str, String str2) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IPowerManager {
        @Override // android.os.IPowerManager
        public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int displayId, IWakeLockCallback callback) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uidtoblame, int displayId, IWakeLockCallback callback) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void releaseWakeLock(IBinder lock, int flags) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void updateWakeLockUids(IBinder lock, int[] uids) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setPowerBoost(int boost, int durationMs) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setPowerMode(int mode, boolean enabled) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean setPowerModeChecked(int mode, boolean enabled) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void updateWakeLockCallback(IBinder lock, IWakeLockCallback callback) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean isWakeLockLevelSupported(int level) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void userActivity(int displayId, long time, int event, int flags) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void wakeUp(long time, int reason, String details, String opPackageName) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void goToSleep(long time, int reason, int flags) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void nap(long time) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public float getBrightnessConstraint(int constraint) throws RemoteException {
            return 0.0f;
        }

        @Override // android.os.IPowerManager
        public boolean isInteractive() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isPowerSaveMode() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public PowerSaveState getPowerSaveState(int serviceType) throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public boolean setPowerSaveModeEnabled(boolean mode) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public BatterySaverPolicyConfig getFullPowerSavePolicy() throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public boolean setFullPowerSavePolicy(BatterySaverPolicyConfig config) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean setDynamicPowerSaveHint(boolean powerSaveHint, int disableThreshold) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean setAdaptivePowerSavePolicy(BatterySaverPolicyConfig config) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean setAdaptivePowerSaveEnabled(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public int getPowerSaveModeTrigger() throws RemoteException {
            return 0;
        }

        @Override // android.os.IPowerManager
        public void setBatteryDischargePrediction(ParcelDuration timeRemaining, boolean isCustomized) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public ParcelDuration getBatteryDischargePrediction() throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public boolean isBatteryDischargePredictionPersonalized() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isDeviceIdleMode() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isLightDeviceIdleMode() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isLowPowerStandbySupported() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isLowPowerStandbyEnabled() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void setLowPowerStandbyEnabled(boolean enabled) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setLowPowerStandbyActiveDuringMaintenance(boolean activeDuringMaintenance) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void forceLowPowerStandbyActive(boolean active) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void rebootSafeMode(boolean confirm, boolean wait) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void shutdown(boolean confirm, String reason, boolean wait) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void crash(String message) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public int getLastShutdownReason() throws RemoteException {
            return 0;
        }

        @Override // android.os.IPowerManager
        public int getLastSleepReason() throws RemoteException {
            return 0;
        }

        @Override // android.os.IPowerManager
        public void setStayOnSetting(int val) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void boostScreenBrightness(long time) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void acquireWakeLockAsync(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void releaseWakeLockAsync(IBinder lock, int flags) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void updateWakeLockUidsAsync(IBinder lock, int[] uids) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean isScreenBrightnessBoosted() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void setAttentionLight(boolean on, int color) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setDozeAfterScreenOff(boolean on) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean isAmbientDisplayAvailable() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void suppressAmbientDisplay(String token, boolean suppress) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean isAmbientDisplaySuppressedForToken(String token) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isAmbientDisplaySuppressed() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean isAmbientDisplaySuppressedForTokenByApp(String token, int appUid) throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public boolean forceSuspend() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public List<String> getWakeLockPkgs() throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public void isNotifyScreenOn(boolean notify, long dimDuration) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setWakeLockAppMap(String packageName, boolean on) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public void setScreenOnManagerEnable(boolean enable) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public boolean getScreenOnManagerEnable() throws RemoteException {
            return false;
        }

        @Override // android.os.IPowerManager
        public void setTorch(boolean on) throws RemoteException {
        }

        @Override // android.os.IPowerManager
        public List<String> getAcquireableWakeLockApp() throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public List<String> getUnacquireableWakeLockApp() throws RemoteException {
            return null;
        }

        @Override // android.os.IPowerManager
        public void notifyChangeConnectState(boolean connect) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IPowerManager {
        public static final String DESCRIPTOR = "android.os.IPowerManager";
        static final int TRANSACTION_acquireWakeLock = 1;
        static final int TRANSACTION_acquireWakeLockAsync = 44;
        static final int TRANSACTION_acquireWakeLockWithUid = 2;
        static final int TRANSACTION_boostScreenBrightness = 43;
        static final int TRANSACTION_crash = 39;
        static final int TRANSACTION_forceLowPowerStandbyActive = 35;
        static final int TRANSACTION_forceSuspend = 55;
        static final int TRANSACTION_getAcquireableWakeLockApp = 62;
        static final int TRANSACTION_getBatteryDischargePrediction = 27;
        static final int TRANSACTION_getBrightnessConstraint = 15;
        static final int TRANSACTION_getFullPowerSavePolicy = 20;
        static final int TRANSACTION_getLastShutdownReason = 40;
        static final int TRANSACTION_getLastSleepReason = 41;
        static final int TRANSACTION_getPowerSaveModeTrigger = 25;
        static final int TRANSACTION_getPowerSaveState = 18;
        static final int TRANSACTION_getScreenOnManagerEnable = 60;
        static final int TRANSACTION_getUnacquireableWakeLockApp = 63;
        static final int TRANSACTION_getWakeLockPkgs = 56;
        static final int TRANSACTION_goToSleep = 13;
        static final int TRANSACTION_isAmbientDisplayAvailable = 50;
        static final int TRANSACTION_isAmbientDisplaySuppressed = 53;
        static final int TRANSACTION_isAmbientDisplaySuppressedForToken = 52;
        static final int TRANSACTION_isAmbientDisplaySuppressedForTokenByApp = 54;
        static final int TRANSACTION_isBatteryDischargePredictionPersonalized = 28;
        static final int TRANSACTION_isDeviceIdleMode = 29;
        static final int TRANSACTION_isInteractive = 16;
        static final int TRANSACTION_isLightDeviceIdleMode = 30;
        static final int TRANSACTION_isLowPowerStandbyEnabled = 32;
        static final int TRANSACTION_isLowPowerStandbySupported = 31;
        static final int TRANSACTION_isNotifyScreenOn = 57;
        static final int TRANSACTION_isPowerSaveMode = 17;
        static final int TRANSACTION_isScreenBrightnessBoosted = 47;
        static final int TRANSACTION_isWakeLockLevelSupported = 10;
        static final int TRANSACTION_nap = 14;
        static final int TRANSACTION_notifyChangeConnectState = 64;
        static final int TRANSACTION_reboot = 36;
        static final int TRANSACTION_rebootSafeMode = 37;
        static final int TRANSACTION_releaseWakeLock = 3;
        static final int TRANSACTION_releaseWakeLockAsync = 45;
        static final int TRANSACTION_setAdaptivePowerSaveEnabled = 24;
        static final int TRANSACTION_setAdaptivePowerSavePolicy = 23;
        static final int TRANSACTION_setAttentionLight = 48;
        static final int TRANSACTION_setBatteryDischargePrediction = 26;
        static final int TRANSACTION_setDozeAfterScreenOff = 49;
        static final int TRANSACTION_setDynamicPowerSaveHint = 22;
        static final int TRANSACTION_setFullPowerSavePolicy = 21;
        static final int TRANSACTION_setLowPowerStandbyActiveDuringMaintenance = 34;
        static final int TRANSACTION_setLowPowerStandbyEnabled = 33;
        static final int TRANSACTION_setPowerBoost = 5;
        static final int TRANSACTION_setPowerMode = 6;
        static final int TRANSACTION_setPowerModeChecked = 7;
        static final int TRANSACTION_setPowerSaveModeEnabled = 19;
        static final int TRANSACTION_setScreenOnManagerEnable = 59;
        static final int TRANSACTION_setStayOnSetting = 42;
        static final int TRANSACTION_setTorch = 61;
        static final int TRANSACTION_setWakeLockAppMap = 58;
        static final int TRANSACTION_shutdown = 38;
        static final int TRANSACTION_suppressAmbientDisplay = 51;
        static final int TRANSACTION_updateWakeLockCallback = 9;
        static final int TRANSACTION_updateWakeLockUids = 4;
        static final int TRANSACTION_updateWakeLockUidsAsync = 46;
        static final int TRANSACTION_updateWakeLockWorkSource = 8;
        static final int TRANSACTION_userActivity = 11;
        static final int TRANSACTION_wakeUp = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPowerManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPowerManager)) {
                return (IPowerManager) iin;
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
                    return "acquireWakeLock";
                case 2:
                    return "acquireWakeLockWithUid";
                case 3:
                    return "releaseWakeLock";
                case 4:
                    return "updateWakeLockUids";
                case 5:
                    return "setPowerBoost";
                case 6:
                    return "setPowerMode";
                case 7:
                    return "setPowerModeChecked";
                case 8:
                    return "updateWakeLockWorkSource";
                case 9:
                    return "updateWakeLockCallback";
                case 10:
                    return "isWakeLockLevelSupported";
                case 11:
                    return "userActivity";
                case 12:
                    return "wakeUp";
                case 13:
                    return "goToSleep";
                case 14:
                    return "nap";
                case 15:
                    return "getBrightnessConstraint";
                case 16:
                    return "isInteractive";
                case 17:
                    return "isPowerSaveMode";
                case 18:
                    return "getPowerSaveState";
                case 19:
                    return "setPowerSaveModeEnabled";
                case 20:
                    return "getFullPowerSavePolicy";
                case 21:
                    return "setFullPowerSavePolicy";
                case 22:
                    return "setDynamicPowerSaveHint";
                case 23:
                    return "setAdaptivePowerSavePolicy";
                case 24:
                    return "setAdaptivePowerSaveEnabled";
                case 25:
                    return "getPowerSaveModeTrigger";
                case 26:
                    return "setBatteryDischargePrediction";
                case 27:
                    return "getBatteryDischargePrediction";
                case 28:
                    return "isBatteryDischargePredictionPersonalized";
                case 29:
                    return "isDeviceIdleMode";
                case 30:
                    return "isLightDeviceIdleMode";
                case 31:
                    return "isLowPowerStandbySupported";
                case 32:
                    return "isLowPowerStandbyEnabled";
                case 33:
                    return "setLowPowerStandbyEnabled";
                case 34:
                    return "setLowPowerStandbyActiveDuringMaintenance";
                case 35:
                    return "forceLowPowerStandbyActive";
                case 36:
                    return "reboot";
                case 37:
                    return "rebootSafeMode";
                case 38:
                    return "shutdown";
                case 39:
                    return "crash";
                case 40:
                    return "getLastShutdownReason";
                case 41:
                    return "getLastSleepReason";
                case 42:
                    return "setStayOnSetting";
                case 43:
                    return "boostScreenBrightness";
                case 44:
                    return "acquireWakeLockAsync";
                case 45:
                    return "releaseWakeLockAsync";
                case 46:
                    return "updateWakeLockUidsAsync";
                case 47:
                    return "isScreenBrightnessBoosted";
                case 48:
                    return "setAttentionLight";
                case 49:
                    return "setDozeAfterScreenOff";
                case 50:
                    return "isAmbientDisplayAvailable";
                case 51:
                    return "suppressAmbientDisplay";
                case 52:
                    return "isAmbientDisplaySuppressedForToken";
                case 53:
                    return "isAmbientDisplaySuppressed";
                case 54:
                    return "isAmbientDisplaySuppressedForTokenByApp";
                case 55:
                    return "forceSuspend";
                case 56:
                    return "getWakeLockPkgs";
                case 57:
                    return "isNotifyScreenOn";
                case 58:
                    return "setWakeLockAppMap";
                case 59:
                    return "setScreenOnManagerEnable";
                case 60:
                    return "getScreenOnManagerEnable";
                case 61:
                    return "setTorch";
                case 62:
                    return "getAcquireableWakeLockApp";
                case 63:
                    return "getUnacquireableWakeLockApp";
                case 64:
                    return "notifyChangeConnectState";
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
                            IBinder _arg0 = data.readStrongBinder();
                            int _arg1 = data.readInt();
                            String _arg2 = data.readString();
                            String _arg3 = data.readString();
                            WorkSource _arg4 = (WorkSource) data.readTypedObject(WorkSource.CREATOR);
                            String _arg5 = data.readString();
                            int _arg6 = data.readInt();
                            IWakeLockCallback _arg7 = IWakeLockCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            acquireWakeLock(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
                            reply.writeNoException();
                            break;
                        case 2:
                            IBinder _arg02 = data.readStrongBinder();
                            int _arg12 = data.readInt();
                            String _arg22 = data.readString();
                            String _arg32 = data.readString();
                            int _arg42 = data.readInt();
                            int _arg52 = data.readInt();
                            IWakeLockCallback _arg62 = IWakeLockCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            acquireWakeLockWithUid(_arg02, _arg12, _arg22, _arg32, _arg42, _arg52, _arg62);
                            reply.writeNoException();
                            break;
                        case 3:
                            IBinder _arg03 = data.readStrongBinder();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            releaseWakeLock(_arg03, _arg13);
                            reply.writeNoException();
                            break;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            int[] _arg14 = data.createIntArray();
                            data.enforceNoDataAvail();
                            updateWakeLockUids(_arg04, _arg14);
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            setPowerBoost(_arg05, _arg15);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            boolean _arg16 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPowerMode(_arg06, _arg16);
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            boolean _arg17 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result = setPowerModeChecked(_arg07, _arg17);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 8:
                            IBinder _arg08 = data.readStrongBinder();
                            WorkSource _arg18 = (WorkSource) data.readTypedObject(WorkSource.CREATOR);
                            String _arg23 = data.readString();
                            data.enforceNoDataAvail();
                            updateWakeLockWorkSource(_arg08, _arg18, _arg23);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            IWakeLockCallback _arg19 = IWakeLockCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            updateWakeLockCallback(_arg09, _arg19);
                            reply.writeNoException();
                            break;
                        case 10:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result2 = isWakeLockLevelSupported(_arg010);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            long _arg110 = data.readLong();
                            int _arg24 = data.readInt();
                            int _arg33 = data.readInt();
                            data.enforceNoDataAvail();
                            userActivity(_arg011, _arg110, _arg24, _arg33);
                            reply.writeNoException();
                            break;
                        case 12:
                            long _arg012 = data.readLong();
                            int _arg111 = data.readInt();
                            String _arg25 = data.readString();
                            String _arg34 = data.readString();
                            data.enforceNoDataAvail();
                            wakeUp(_arg012, _arg111, _arg25, _arg34);
                            reply.writeNoException();
                            break;
                        case 13:
                            long _arg013 = data.readLong();
                            int _arg112 = data.readInt();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            goToSleep(_arg013, _arg112, _arg26);
                            reply.writeNoException();
                            break;
                        case 14:
                            long _arg014 = data.readLong();
                            data.enforceNoDataAvail();
                            nap(_arg014);
                            reply.writeNoException();
                            break;
                        case 15:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            float _result3 = getBrightnessConstraint(_arg015);
                            reply.writeNoException();
                            reply.writeFloat(_result3);
                            break;
                        case 16:
                            boolean _result4 = isInteractive();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 17:
                            boolean _result5 = isPowerSaveMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 18:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            PowerSaveState _result6 = getPowerSaveState(_arg016);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 19:
                            boolean _arg017 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result7 = setPowerSaveModeEnabled(_arg017);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 20:
                            BatterySaverPolicyConfig _result8 = getFullPowerSavePolicy();
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            break;
                        case 21:
                            BatterySaverPolicyConfig _arg018 = (BatterySaverPolicyConfig) data.readTypedObject(BatterySaverPolicyConfig.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result9 = setFullPowerSavePolicy(_arg018);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 22:
                            boolean _arg019 = data.readBoolean();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result10 = setDynamicPowerSaveHint(_arg019, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 23:
                            BatterySaverPolicyConfig _arg020 = (BatterySaverPolicyConfig) data.readTypedObject(BatterySaverPolicyConfig.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result11 = setAdaptivePowerSavePolicy(_arg020);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 24:
                            boolean _arg021 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result12 = setAdaptivePowerSaveEnabled(_arg021);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case 25:
                            int _result13 = getPowerSaveModeTrigger();
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            break;
                        case 26:
                            ParcelDuration _arg022 = (ParcelDuration) data.readTypedObject(ParcelDuration.CREATOR);
                            boolean _arg114 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setBatteryDischargePrediction(_arg022, _arg114);
                            reply.writeNoException();
                            break;
                        case 27:
                            ParcelDuration _result14 = getBatteryDischargePrediction();
                            reply.writeNoException();
                            reply.writeTypedObject(_result14, 1);
                            break;
                        case 28:
                            boolean _result15 = isBatteryDischargePredictionPersonalized();
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 29:
                            boolean _result16 = isDeviceIdleMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 30:
                            boolean _result17 = isLightDeviceIdleMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            break;
                        case 31:
                            boolean _result18 = isLowPowerStandbySupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            break;
                        case 32:
                            boolean _result19 = isLowPowerStandbyEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            break;
                        case 33:
                            boolean _arg023 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setLowPowerStandbyEnabled(_arg023);
                            reply.writeNoException();
                            break;
                        case 34:
                            boolean _arg024 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setLowPowerStandbyActiveDuringMaintenance(_arg024);
                            reply.writeNoException();
                            break;
                        case 35:
                            boolean _arg025 = data.readBoolean();
                            data.enforceNoDataAvail();
                            forceLowPowerStandbyActive(_arg025);
                            reply.writeNoException();
                            break;
                        case 36:
                            boolean _arg026 = data.readBoolean();
                            String _arg115 = data.readString();
                            boolean _arg27 = data.readBoolean();
                            data.enforceNoDataAvail();
                            reboot(_arg026, _arg115, _arg27);
                            reply.writeNoException();
                            break;
                        case 37:
                            boolean _arg027 = data.readBoolean();
                            boolean _arg116 = data.readBoolean();
                            data.enforceNoDataAvail();
                            rebootSafeMode(_arg027, _arg116);
                            reply.writeNoException();
                            break;
                        case 38:
                            boolean _arg028 = data.readBoolean();
                            String _arg117 = data.readString();
                            boolean _arg28 = data.readBoolean();
                            data.enforceNoDataAvail();
                            shutdown(_arg028, _arg117, _arg28);
                            reply.writeNoException();
                            break;
                        case 39:
                            String _arg029 = data.readString();
                            data.enforceNoDataAvail();
                            crash(_arg029);
                            reply.writeNoException();
                            break;
                        case 40:
                            int _result20 = getLastShutdownReason();
                            reply.writeNoException();
                            reply.writeInt(_result20);
                            break;
                        case 41:
                            int _result21 = getLastSleepReason();
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            break;
                        case 42:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            setStayOnSetting(_arg030);
                            reply.writeNoException();
                            break;
                        case 43:
                            long _arg031 = data.readLong();
                            data.enforceNoDataAvail();
                            boostScreenBrightness(_arg031);
                            reply.writeNoException();
                            break;
                        case 44:
                            IBinder _arg032 = data.readStrongBinder();
                            int _arg118 = data.readInt();
                            String _arg29 = data.readString();
                            String _arg35 = data.readString();
                            WorkSource _arg43 = (WorkSource) data.readTypedObject(WorkSource.CREATOR);
                            String _arg53 = data.readString();
                            data.enforceNoDataAvail();
                            acquireWakeLockAsync(_arg032, _arg118, _arg29, _arg35, _arg43, _arg53);
                            break;
                        case 45:
                            IBinder _arg033 = data.readStrongBinder();
                            int _arg119 = data.readInt();
                            data.enforceNoDataAvail();
                            releaseWakeLockAsync(_arg033, _arg119);
                            break;
                        case 46:
                            IBinder _arg034 = data.readStrongBinder();
                            int[] _arg120 = data.createIntArray();
                            data.enforceNoDataAvail();
                            updateWakeLockUidsAsync(_arg034, _arg120);
                            break;
                        case 47:
                            boolean _result22 = isScreenBrightnessBoosted();
                            reply.writeNoException();
                            reply.writeBoolean(_result22);
                            break;
                        case 48:
                            boolean _arg035 = data.readBoolean();
                            int _arg121 = data.readInt();
                            data.enforceNoDataAvail();
                            setAttentionLight(_arg035, _arg121);
                            reply.writeNoException();
                            break;
                        case 49:
                            boolean _arg036 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDozeAfterScreenOff(_arg036);
                            reply.writeNoException();
                            break;
                        case 50:
                            boolean _result23 = isAmbientDisplayAvailable();
                            reply.writeNoException();
                            reply.writeBoolean(_result23);
                            break;
                        case 51:
                            String _arg037 = data.readString();
                            boolean _arg122 = data.readBoolean();
                            data.enforceNoDataAvail();
                            suppressAmbientDisplay(_arg037, _arg122);
                            reply.writeNoException();
                            break;
                        case 52:
                            String _arg038 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result24 = isAmbientDisplaySuppressedForToken(_arg038);
                            reply.writeNoException();
                            reply.writeBoolean(_result24);
                            break;
                        case 53:
                            boolean _result25 = isAmbientDisplaySuppressed();
                            reply.writeNoException();
                            reply.writeBoolean(_result25);
                            break;
                        case 54:
                            String _arg039 = data.readString();
                            int _arg123 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result26 = isAmbientDisplaySuppressedForTokenByApp(_arg039, _arg123);
                            reply.writeNoException();
                            reply.writeBoolean(_result26);
                            break;
                        case 55:
                            boolean _result27 = forceSuspend();
                            reply.writeNoException();
                            reply.writeBoolean(_result27);
                            break;
                        case 56:
                            List<String> _result28 = getWakeLockPkgs();
                            reply.writeNoException();
                            reply.writeStringList(_result28);
                            break;
                        case 57:
                            boolean _arg040 = data.readBoolean();
                            long _arg124 = data.readLong();
                            data.enforceNoDataAvail();
                            isNotifyScreenOn(_arg040, _arg124);
                            reply.writeNoException();
                            break;
                        case 58:
                            String _arg041 = data.readString();
                            boolean _arg125 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setWakeLockAppMap(_arg041, _arg125);
                            reply.writeNoException();
                            break;
                        case 59:
                            boolean _arg042 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setScreenOnManagerEnable(_arg042);
                            reply.writeNoException();
                            break;
                        case 60:
                            boolean _result29 = getScreenOnManagerEnable();
                            reply.writeNoException();
                            reply.writeBoolean(_result29);
                            break;
                        case 61:
                            boolean _arg043 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setTorch(_arg043);
                            reply.writeNoException();
                            break;
                        case 62:
                            List<String> _result30 = getAcquireableWakeLockApp();
                            reply.writeNoException();
                            reply.writeStringList(_result30);
                            break;
                        case 63:
                            List<String> _result31 = getUnacquireableWakeLockApp();
                            reply.writeNoException();
                            reply.writeStringList(_result31);
                            break;
                        case 64:
                            boolean _arg044 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyChangeConnectState(_arg044);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IPowerManager {
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

            @Override // android.os.IPowerManager
            public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int displayId, IWakeLockCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    _data.writeString(tag);
                    _data.writeString(packageName);
                    _data.writeTypedObject(ws, 0);
                    _data.writeString(historyTag);
                    _data.writeInt(displayId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uidtoblame, int displayId, IWakeLockCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    _data.writeString(tag);
                    _data.writeString(packageName);
                    _data.writeInt(uidtoblame);
                    _data.writeInt(displayId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void releaseWakeLock(IBinder lock, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void updateWakeLockUids(IBinder lock, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setPowerBoost(int boost, int durationMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(boost);
                    _data.writeInt(durationMs);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setPowerMode(int mode, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setPowerModeChecked(int mode, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeTypedObject(ws, 0);
                    _data.writeString(historyTag);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void updateWakeLockCallback(IBinder lock, IWakeLockCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isWakeLockLevelSupported(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void userActivity(int displayId, long time, int event, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeLong(time);
                    _data.writeInt(event);
                    _data.writeInt(flags);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void wakeUp(long time, int reason, String details, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    _data.writeInt(reason);
                    _data.writeString(details);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void goToSleep(long time, int reason, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    _data.writeInt(reason);
                    _data.writeInt(flags);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void nap(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public float getBrightnessConstraint(int constraint) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(constraint);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isInteractive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isPowerSaveMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public PowerSaveState getPowerSaveState(int serviceType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serviceType);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    PowerSaveState _result = (PowerSaveState) _reply.readTypedObject(PowerSaveState.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setPowerSaveModeEnabled(boolean mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(mode);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public BatterySaverPolicyConfig getFullPowerSavePolicy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    BatterySaverPolicyConfig _result = (BatterySaverPolicyConfig) _reply.readTypedObject(BatterySaverPolicyConfig.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setFullPowerSavePolicy(BatterySaverPolicyConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setDynamicPowerSaveHint(boolean powerSaveHint, int disableThreshold) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(powerSaveHint);
                    _data.writeInt(disableThreshold);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setAdaptivePowerSavePolicy(BatterySaverPolicyConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean setAdaptivePowerSaveEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public int getPowerSaveModeTrigger() throws RemoteException {
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

            @Override // android.os.IPowerManager
            public void setBatteryDischargePrediction(ParcelDuration timeRemaining, boolean isCustomized) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(timeRemaining, 0);
                    _data.writeBoolean(isCustomized);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public ParcelDuration getBatteryDischargePrediction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    ParcelDuration _result = (ParcelDuration) _reply.readTypedObject(ParcelDuration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isBatteryDischargePredictionPersonalized() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isDeviceIdleMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isLightDeviceIdleMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isLowPowerStandbySupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isLowPowerStandbyEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setLowPowerStandbyEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setLowPowerStandbyActiveDuringMaintenance(boolean activeDuringMaintenance) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(activeDuringMaintenance);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void forceLowPowerStandbyActive(boolean active) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(active);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(confirm);
                    _data.writeString(reason);
                    _data.writeBoolean(wait);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void rebootSafeMode(boolean confirm, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(confirm);
                    _data.writeBoolean(wait);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void shutdown(boolean confirm, String reason, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(confirm);
                    _data.writeString(reason);
                    _data.writeBoolean(wait);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void crash(String message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(message);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public int getLastShutdownReason() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public int getLastSleepReason() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setStayOnSetting(int val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(val);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void boostScreenBrightness(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void acquireWakeLockAsync(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    _data.writeString(tag);
                    _data.writeString(packageName);
                    _data.writeTypedObject(ws, 0);
                    _data.writeString(historyTag);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void releaseWakeLockAsync(IBinder lock, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    this.mRemote.transact(45, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void updateWakeLockUidsAsync(IBinder lock, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isScreenBrightnessBoosted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setAttentionLight(boolean on, int color) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(on);
                    _data.writeInt(color);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setDozeAfterScreenOff(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(on);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isAmbientDisplayAvailable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void suppressAmbientDisplay(String token, boolean suppress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(token);
                    _data.writeBoolean(suppress);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isAmbientDisplaySuppressedForToken(String token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(token);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isAmbientDisplaySuppressed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean isAmbientDisplaySuppressedForTokenByApp(String token, int appUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(token);
                    _data.writeInt(appUid);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean forceSuspend() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public List<String> getWakeLockPkgs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void isNotifyScreenOn(boolean notify, long dimDuration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(notify);
                    _data.writeLong(dimDuration);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setWakeLockAppMap(String packageName, boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeBoolean(on);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setScreenOnManagerEnable(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public boolean getScreenOnManagerEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void setTorch(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(on);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public List<String> getAcquireableWakeLockApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public List<String> getUnacquireableWakeLockApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IPowerManager
            public void notifyChangeConnectState(boolean connect) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(connect);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 63;
        }
    }
}
