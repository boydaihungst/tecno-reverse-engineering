package com.android.server.usb.hal.port;

import android.hardware.usb.IUsb;
import android.hardware.usb.IUsbCallback;
import android.hardware.usb.IUsbOperationInternal;
import android.hardware.usb.PortRole;
import android.hardware.usb.PortStatus;
import android.hardware.usb.UsbPort;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.LongSparseArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.usb.UsbPortManager;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
/* loaded from: classes2.dex */
public final class UsbPortAidl implements UsbPortHal {
    private static final String USB_AIDL_SERVICE = "android.hardware.usb.IUsb/default";
    public static final int USB_DATA_STATUS_DISABLED_CONTAMINANT = 3;
    public static final int USB_DATA_STATUS_DISABLED_DEBUG = 6;
    public static final int USB_DATA_STATUS_DISABLED_DOCK = 4;
    public static final int USB_DATA_STATUS_DISABLED_FORCE = 5;
    public static final int USB_DATA_STATUS_DISABLED_OVERHEAT = 2;
    public static final int USB_DATA_STATUS_ENABLED = 1;
    public static final int USB_DATA_STATUS_UNKNOWN = 0;
    private IBinder mBinder;
    private HALCallback mHALCallback;
    private final Object mLock = new Object();
    private UsbPortManager mPortManager;
    private IUsb mProxy;
    public IndentingPrintWriter mPw;
    private boolean mSystemReady;
    private long mTransactionId;
    private static final String TAG = UsbPortAidl.class.getSimpleName();
    private static final LongSparseArray<IUsbOperationInternal> sCallbacks = new LongSparseArray<>();

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public int getUsbHalVersion() throws RemoteException {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                throw new RemoteException("IUsb not initialized yet");
            }
        }
        UsbPortManager.logAndPrint(4, null, "USB HAL AIDL version: USB_HAL_V2_0");
        return 20;
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void systemReady() {
        this.mSystemReady = true;
    }

    public void serviceDied() {
        UsbPortManager.logAndPrint(6, this.mPw, "Usb AIDL hal service died");
        synchronized (this.mLock) {
            this.mProxy = null;
        }
        connectToProxy(null);
    }

    private void connectToProxy(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                return;
            }
            try {
                IBinder waitForService = ServiceManager.waitForService(USB_AIDL_SERVICE);
                this.mBinder = waitForService;
                this.mProxy = IUsb.Stub.asInterface(waitForService);
                this.mBinder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.usb.hal.port.UsbPortAidl$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        UsbPortAidl.this.serviceDied();
                    }
                }, 0);
                this.mProxy.setCallback(this.mHALCallback);
                IUsb iUsb = this.mProxy;
                long j = this.mTransactionId + 1;
                this.mTransactionId = j;
                iUsb.queryPortStatus(j);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(pw, "connectToProxy: usb hal service not responding", e);
            } catch (NoSuchElementException e2) {
                UsbPortManager.logAndPrintException(pw, "connectToProxy: usb hal service not found. Did the service fail to start?", e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isServicePresent(IndentingPrintWriter pw) {
        try {
            return ServiceManager.isDeclared(USB_AIDL_SERVICE);
        } catch (NoSuchElementException e) {
            UsbPortManager.logAndPrintException(pw, "connectToProxy: usb Aidl hal service not found.", e);
            return false;
        }
    }

    public UsbPortAidl(UsbPortManager portManager, IndentingPrintWriter pw) {
        this.mPortManager = (UsbPortManager) Objects.requireNonNull(portManager);
        this.mPw = pw;
        this.mHALCallback = new HALCallback(null, this.mPortManager, this);
        connectToProxy(this.mPw);
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableContaminantPresenceDetection(String portName, boolean enable, long operationID) {
        synchronized (this.mLock) {
            IUsb iUsb = this.mProxy;
            if (iUsb == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry ! opID: " + operationID);
                return;
            }
            try {
                iUsb.enableContaminantPresenceDetection(portName, enable, operationID);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set contaminant detection. opID:" + operationID, e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void queryPortStatus(long operationID) {
        synchronized (this.mLock) {
            IUsb iUsb = this.mProxy;
            if (iUsb == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry ! opID:" + operationID);
                return;
            }
            try {
                iUsb.queryPortStatus(operationID);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(null, "ServiceStart: Failed to query port status. opID:" + operationID, e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchMode(String portId, int newMode, long operationID) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry ! opID:" + operationID);
                return;
            }
            PortRole newRole = new PortRole();
            newRole.setMode((byte) newMode);
            try {
                this.mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB port mode: portId=" + portId + ", newMode=" + UsbPort.modeToString(newMode) + "opID:" + operationID, e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchPowerRole(String portId, int newPowerRole, long operationID) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry ! opID:" + operationID);
                return;
            }
            PortRole newRole = new PortRole();
            newRole.setPowerRole((byte) newPowerRole);
            try {
                this.mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB power role: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + "opID:" + operationID, e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchDataRole(String portId, int newDataRole, long operationID) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry ! opID:" + operationID);
                return;
            }
            PortRole newRole = new PortRole();
            newRole.setDataRole((byte) newDataRole);
            try {
                this.mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB data role: portId=" + portId + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole) + "opID:" + operationID, e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void resetUsbPort(String portName, long operationID, IUsbOperationInternal callback) {
        LongSparseArray<IUsbOperationInternal> longSparseArray;
        Objects.requireNonNull(portName);
        Objects.requireNonNull(callback);
        long key = operationID;
        synchronized (this.mLock) {
            try {
                if (this.mProxy == null) {
                    UsbPortManager.logAndPrint(6, this.mPw, "resetUsbPort: Proxy is null. Retry !opID:" + operationID);
                    callback.onOperationComplete(1);
                }
                while (true) {
                    longSparseArray = sCallbacks;
                    if (longSparseArray.get(key) == null) {
                        break;
                    }
                    key = ThreadLocalRandom.current().nextInt();
                }
                if (key != operationID) {
                    UsbPortManager.logAndPrint(4, this.mPw, "resetUsbPort: operationID exists ! opID:" + operationID + " key:" + key);
                }
                try {
                    longSparseArray.put(key, callback);
                    this.mProxy.resetUsbPort(portName, key);
                } catch (RemoteException e) {
                    UsbPortManager.logAndPrintException(this.mPw, "resetUsbPort: Failed to resetUsbPort: portID=" + portName + "opId:" + operationID, e);
                    callback.onOperationComplete(1);
                    sCallbacks.remove(key);
                }
            } catch (RemoteException e2) {
                UsbPortManager.logAndPrintException(this.mPw, "resetUsbPort: Failed to call onOperationComplete portID=" + portName + "opID:" + operationID, e2);
                sCallbacks.remove(key);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public boolean enableUsbData(String portName, boolean enable, long operationID, IUsbOperationInternal callback) {
        LongSparseArray<IUsbOperationInternal> longSparseArray;
        Objects.requireNonNull(portName);
        Objects.requireNonNull(callback);
        long key = operationID;
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mProxy == null) {
                        UsbPortManager.logAndPrint(6, this.mPw, "enableUsbData: Proxy is null. Retry !opID:" + operationID);
                        callback.onOperationComplete(1);
                        return false;
                    }
                    while (true) {
                        longSparseArray = sCallbacks;
                        if (longSparseArray.get(key) == null) {
                            break;
                        }
                        key = ThreadLocalRandom.current().nextInt();
                    }
                    if (key != operationID) {
                        UsbPortManager.logAndPrint(4, this.mPw, "enableUsbData: operationID exists ! opID:" + operationID + " key:" + key);
                    }
                    try {
                        longSparseArray.put(key, callback);
                        this.mProxy.enableUsbData(portName, enable, key);
                        return true;
                    } catch (RemoteException e) {
                        UsbPortManager.logAndPrintException(this.mPw, "enableUsbData: Failed to invoke enableUsbData: portID=" + portName + "opID:" + operationID, e);
                        callback.onOperationComplete(1);
                        sCallbacks.remove(key);
                        return false;
                    }
                } catch (RemoteException e2) {
                    UsbPortManager.logAndPrintException(this.mPw, "enableUsbData: Failed to call onOperationComplete portID=" + portName + "opID:" + operationID, e2);
                    sCallbacks.remove(key);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableLimitPowerTransfer(String portName, boolean limit, long operationID, IUsbOperationInternal callback) {
        LongSparseArray<IUsbOperationInternal> longSparseArray;
        Objects.requireNonNull(portName);
        long key = operationID;
        synchronized (this.mLock) {
            try {
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "enableLimitPowerTransfer: Failed to call onOperationComplete portID=" + portName + " opID:" + operationID, e);
            }
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "enableLimitPowerTransfer: Proxy is null. Retry !opID:" + operationID);
                callback.onOperationComplete(1);
                return;
            }
            while (true) {
                longSparseArray = sCallbacks;
                if (longSparseArray.get(key) == null) {
                    break;
                }
                key = ThreadLocalRandom.current().nextInt();
            }
            if (key != operationID) {
                UsbPortManager.logAndPrint(4, this.mPw, "enableUsbData: operationID exists ! opID:" + operationID + " key:" + key);
            }
            try {
                longSparseArray.put(key, callback);
                this.mProxy.limitPowerTransfer(portName, limit, key);
            } catch (RemoteException e2) {
                UsbPortManager.logAndPrintException(this.mPw, "enableLimitPowerTransfer: Failed while invoking AIDL HAL portID=" + portName + " opID:" + operationID, e2);
                if (callback != null) {
                    callback.onOperationComplete(1);
                }
                sCallbacks.remove(key);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableUsbDataWhileDocked(String portName, long operationID, IUsbOperationInternal callback) {
        LongSparseArray<IUsbOperationInternal> longSparseArray;
        Objects.requireNonNull(portName);
        long key = operationID;
        synchronized (this.mLock) {
            try {
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "enableUsbDataWhileDocked: Failed to call onOperationComplete portID=" + portName + " opID:" + operationID, e);
            }
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "enableUsbDataWhileDocked: Proxy is null. Retry !opID:" + operationID);
                callback.onOperationComplete(1);
                return;
            }
            while (true) {
                longSparseArray = sCallbacks;
                if (longSparseArray.get(key) == null) {
                    break;
                }
                key = ThreadLocalRandom.current().nextInt();
            }
            if (key != operationID) {
                UsbPortManager.logAndPrint(4, this.mPw, "enableUsbDataWhileDocked: operationID exists ! opID:" + operationID + " key:" + key);
            }
            try {
                longSparseArray.put(key, callback);
                this.mProxy.enableUsbDataWhileDocked(portName, key);
            } catch (RemoteException e2) {
                UsbPortManager.logAndPrintException(this.mPw, "enableUsbDataWhileDocked: error while invoking halportID=" + portName + " opID:" + operationID, e2);
                if (callback != null) {
                    callback.onOperationComplete(1);
                }
                sCallbacks.remove(key);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class HALCallback extends IUsbCallback.Stub {
        public UsbPortManager mPortManager;
        public IndentingPrintWriter mPw;
        public UsbPortAidl mUsbPortAidl;

        HALCallback(IndentingPrintWriter pw, UsbPortManager portManager, UsbPortAidl usbPortAidl) {
            this.mPw = pw;
            this.mPortManager = portManager;
            this.mUsbPortAidl = usbPortAidl;
        }

        private int toPortMode(byte aidlPortMode) {
            switch (aidlPortMode) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 8;
                default:
                    UsbPortManager.logAndPrint(6, this.mPw, "Unrecognized aidlPortMode:" + ((int) aidlPortMode));
                    return 0;
            }
        }

        private int toSupportedModes(byte[] aidlPortModes) {
            int supportedModes = 0;
            for (byte aidlPortMode : aidlPortModes) {
                supportedModes |= toPortMode(aidlPortMode);
            }
            return supportedModes;
        }

        private int toContaminantProtectionStatus(byte aidlContaminantProtection) {
            switch (aidlContaminantProtection) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 4;
                case 4:
                    return 8;
                default:
                    UsbPortManager.logAndPrint(6, this.mPw, "Unrecognized aidlContaminantProtection:" + ((int) aidlContaminantProtection));
                    return 0;
            }
        }

        private int toSupportedContaminantProtectionModes(byte[] aidlModes) {
            int supportedContaminantProtectionModes = 0;
            for (byte aidlMode : aidlModes) {
                supportedContaminantProtectionModes |= toContaminantProtectionStatus(aidlMode);
            }
            return supportedContaminantProtectionModes;
        }

        private int toUsbDataStatusInt(byte[] usbDataStatusHal) {
            int usbDataStatus = 0;
            for (byte b : usbDataStatusHal) {
                switch (b) {
                    case 1:
                        usbDataStatus |= 1;
                        break;
                    case 2:
                        usbDataStatus |= 2;
                        break;
                    case 3:
                        usbDataStatus |= 4;
                        break;
                    case 4:
                        usbDataStatus |= 8;
                        break;
                    case 5:
                        usbDataStatus |= 16;
                        break;
                    case 6:
                        usbDataStatus |= 32;
                        break;
                    default:
                        usbDataStatus |= 0;
                        break;
                }
            }
            UsbPortManager.logAndPrint(4, this.mPw, "AIDL UsbDataStatus:" + usbDataStatus);
            return usbDataStatus;
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyPortStatusChange(PortStatus[] currentPortStatus, int retval) {
            PortStatus[] portStatusArr = currentPortStatus;
            if (!this.mUsbPortAidl.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.mPw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList<>();
            int numStatus = portStatusArr.length;
            int i = 0;
            while (i < numStatus) {
                PortStatus current = portStatusArr[i];
                RawPortInfo temp = new RawPortInfo(current.portName, toSupportedModes(current.supportedModes), toSupportedContaminantProtectionModes(current.supportedContaminantProtectionModes), toPortMode(current.currentMode), current.canChangeMode, current.currentPowerRole, current.canChangePowerRole, current.currentDataRole, current.canChangeDataRole, current.supportsEnableContaminantPresenceProtection, toContaminantProtectionStatus(current.contaminantProtectionStatus), current.supportsEnableContaminantPresenceDetection, current.contaminantDetectionStatus, toUsbDataStatusInt(current.usbDataStatus), current.powerTransferLimited, current.powerBrickStatus);
                newPortInfo.add(temp);
                UsbPortManager.logAndPrint(4, this.mPw, "ClientCallback AIDL V1: " + current.portName);
                i++;
                portStatusArr = currentPortStatus;
            }
            this.mPortManager.updatePorts(newPortInfo);
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyRoleSwitchStatus(String portName, PortRole role, int retval, long operationID) {
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, portName + " role switch successful. opID:" + operationID);
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + " role switch failed. err:" + retval + "opID:" + operationID);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyQueryPortStatus(String portName, int retval, long operationID) {
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, portName + ": opID:" + operationID + " successful");
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + ": opID:" + operationID + " failed. err:" + retval);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyEnableUsbDataStatus(String portName, boolean enable, int retval, long operationID) {
            int i;
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, "notifyEnableUsbDataStatus:" + portName + ": opID:" + operationID + " enable:" + enable);
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + "notifyEnableUsbDataStatus: opID:" + operationID + " failed. err:" + retval);
            }
            try {
                IUsbOperationInternal iUsbOperationInternal = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                if (retval == 0) {
                    i = 0;
                } else {
                    i = 1;
                }
                iUsbOperationInternal.onOperationComplete(i);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "notifyEnableUsbDataStatus: Failed to call onOperationComplete", e);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyContaminantEnabledStatus(String portName, boolean enable, int retval, long operationID) {
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, "notifyContaminantEnabledStatus:" + portName + ": opID:" + operationID + " enable:" + enable);
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + "notifyContaminantEnabledStatus: opID:" + operationID + " failed. err:" + retval);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyLimitPowerTransferStatus(String portName, boolean limit, int retval, long operationID) {
            int i;
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, portName + ": opID:" + operationID + " successful");
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + "notifyLimitPowerTransferStatus: opID:" + operationID + " failed. err:" + retval);
            }
            try {
                IUsbOperationInternal callback = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                if (callback != null) {
                    IUsbOperationInternal iUsbOperationInternal = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                    if (retval == 0) {
                        i = 0;
                    } else {
                        i = 1;
                    }
                    iUsbOperationInternal.onOperationComplete(i);
                }
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "enableLimitPowerTransfer: Failed to call onOperationComplete", e);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyEnableUsbDataWhileDockedStatus(String portName, int retval, long operationID) {
            int i;
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, portName + ": opID:" + operationID + " successful");
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + "notifyEnableUsbDataWhileDockedStatus: opID:" + operationID + " failed. err:" + retval);
            }
            try {
                IUsbOperationInternal callback = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                if (callback != null) {
                    IUsbOperationInternal iUsbOperationInternal = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                    if (retval == 0) {
                        i = 0;
                    } else {
                        i = 1;
                    }
                    iUsbOperationInternal.onOperationComplete(i);
                }
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "notifyEnableUsbDataWhileDockedStatus: Failed to call onOperationComplete", e);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyResetUsbPortStatus(String portName, int retval, long operationID) {
            int i;
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, "notifyResetUsbPortStatus:" + portName + ": opID:" + operationID);
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + "notifyEnableUsbDataStatus: opID:" + operationID + " failed. err:" + retval);
            }
            try {
                IUsbOperationInternal iUsbOperationInternal = (IUsbOperationInternal) UsbPortAidl.sCallbacks.get(operationID);
                if (retval == 0) {
                    i = 0;
                } else {
                    i = 1;
                }
                iUsbOperationInternal.onOperationComplete(i);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "notifyResetUsbPortStatus: Failed to call onOperationComplete", e);
            }
        }

        @Override // android.hardware.usb.IUsbCallback
        public String getInterfaceHash() {
            return "9762531142d72e03bb4228209846c135f276d40e";
        }

        @Override // android.hardware.usb.IUsbCallback
        public int getInterfaceVersion() {
            return 1;
        }
    }
}
