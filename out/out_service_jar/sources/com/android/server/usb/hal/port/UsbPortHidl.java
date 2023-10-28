package com.android.server.usb.hal.port;

import android.hardware.usb.IUsbOperationInternal;
import android.hardware.usb.UsbPort;
import android.hardware.usb.V1_0.IUsb;
import android.hardware.usb.V1_0.PortRole;
import android.hardware.usb.V1_0.PortStatus;
import android.hardware.usb.V1_1.PortStatus_1_1;
import android.hardware.usb.V1_2.IUsbCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.usb.UsbPortManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class UsbPortHidl implements UsbPortHal {
    private static final int USB_HAL_DEATH_COOKIE = 1000;
    private HALCallback mHALCallback;
    private final Object mLock = new Object();
    private UsbPortManager mPortManager;
    private IUsb mProxy;
    public IndentingPrintWriter mPw;
    private boolean mSystemReady;
    private static final String TAG = UsbPortHidl.class.getSimpleName();
    private static int sUsbDataStatus = 0;

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public int getUsbHalVersion() throws RemoteException {
        int version;
        synchronized (this.mLock) {
            IUsb iUsb = this.mProxy;
            if (iUsb == null) {
                throw new RemoteException("IUsb not initialized yet");
            }
            if (android.hardware.usb.V1_3.IUsb.castFrom((IHwInterface) iUsb) != null) {
                version = 13;
            } else if (android.hardware.usb.V1_2.IUsb.castFrom((IHwInterface) this.mProxy) != null) {
                version = 12;
            } else if (android.hardware.usb.V1_1.IUsb.castFrom((IHwInterface) this.mProxy) != null) {
                version = 11;
            } else {
                version = 10;
            }
            UsbPortManager.logAndPrint(4, null, "USB HAL HIDL version: " + version);
        }
        return version;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class DeathRecipient implements IHwBinder.DeathRecipient {
        public IndentingPrintWriter pw;

        DeathRecipient(IndentingPrintWriter pw) {
            this.pw = pw;
        }

        public void serviceDied(long cookie) {
            if (cookie == 1000) {
                UsbPortManager.logAndPrint(6, this.pw, "Usb hal service died cookie: " + cookie);
                synchronized (UsbPortHidl.this.mLock) {
                    UsbPortHidl.this.mProxy = null;
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    final class ServiceNotification extends IServiceNotification.Stub {
        ServiceNotification() {
        }

        @Override // android.hidl.manager.V1_0.IServiceNotification
        public void onRegistration(String fqName, String name, boolean preexisting) {
            UsbPortManager.logAndPrint(4, null, "Usb hal service started " + fqName + " " + name);
            UsbPortHidl.this.connectToProxy(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connectToProxy(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                return;
            }
            try {
                IUsb service = IUsb.getService();
                this.mProxy = service;
                service.linkToDeath(new DeathRecipient(pw), 1000L);
                this.mProxy.setCallback(this.mHALCallback);
                this.mProxy.queryPortStatus();
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(pw, "connectToProxy: usb hal service not responding", e);
            } catch (NoSuchElementException e2) {
                UsbPortManager.logAndPrintException(pw, "connectToProxy: usb hal service not found. Did the service fail to start?", e2);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void systemReady() {
        this.mSystemReady = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isServicePresent(IndentingPrintWriter pw) {
        try {
            IUsb.getService(true);
        } catch (RemoteException e) {
            UsbPortManager.logAndPrintException(pw, "IUSB hal service present but failed to get service", e);
        } catch (NoSuchElementException e2) {
            UsbPortManager.logAndPrintException(pw, "connectToProxy: usb hidl hal service not found.", e2);
            return false;
        }
        return true;
    }

    public UsbPortHidl(UsbPortManager portManager, IndentingPrintWriter pw) {
        this.mPortManager = (UsbPortManager) Objects.requireNonNull(portManager);
        this.mPw = pw;
        this.mHALCallback = new HALCallback(null, this.mPortManager, this);
        try {
            ServiceNotification serviceNotification = new ServiceNotification();
            boolean ret = IServiceManager.getService().registerForNotifications(IUsb.kInterfaceName, "", serviceNotification);
            if (!ret) {
                UsbPortManager.logAndPrint(6, null, "Failed to register service start notification");
            }
            connectToProxy(this.mPw);
        } catch (RemoteException e) {
            UsbPortManager.logAndPrintException(null, "Failed to register service start notification", e);
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableContaminantPresenceDetection(String portName, boolean enable, long transactionId) {
        synchronized (this.mLock) {
            IUsb iUsb = this.mProxy;
            if (iUsb == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry !");
                return;
            }
            try {
                android.hardware.usb.V1_2.IUsb proxy = android.hardware.usb.V1_2.IUsb.castFrom((IHwInterface) iUsb);
                proxy.enableContaminantPresenceDetection(portName, enable);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set contaminant detection", e);
            } catch (ClassCastException e2) {
                UsbPortManager.logAndPrintException(this.mPw, "Method only applicable to V1.2 or above implementation", e2);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void queryPortStatus(long transactionId) {
        synchronized (this.mLock) {
            IUsb iUsb = this.mProxy;
            if (iUsb == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry !");
                return;
            }
            try {
                iUsb.queryPortStatus();
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(null, "ServiceStart: Failed to query port status", e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchMode(String portId, int newMode, long transactionId) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry !");
                return;
            }
            PortRole newRole = new PortRole();
            newRole.type = 2;
            newRole.role = newMode;
            try {
                this.mProxy.switchRole(portId, newRole);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB port mode: portId=" + portId + ", newMode=" + UsbPort.modeToString(newRole.role), e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchPowerRole(String portId, int newPowerRole, long transactionId) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry !");
                return;
            }
            PortRole newRole = new PortRole();
            newRole.type = 1;
            newRole.role = newPowerRole;
            try {
                this.mProxy.switchRole(portId, newRole);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB power role: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newRole.role), e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableLimitPowerTransfer(String portName, boolean limit, long transactionId, IUsbOperationInternal callback) {
        try {
            callback.onOperationComplete(2);
        } catch (RemoteException e) {
            UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete", e);
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void enableUsbDataWhileDocked(String portName, long transactionId, IUsbOperationInternal callback) {
        try {
            callback.onOperationComplete(2);
        } catch (RemoteException e) {
            UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete", e);
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void switchDataRole(String portId, int newDataRole, long transactionId) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                UsbPortManager.logAndPrint(6, this.mPw, "Proxy is null. Retry !");
                return;
            }
            PortRole newRole = new PortRole();
            newRole.type = 0;
            newRole.role = newDataRole;
            try {
                this.mProxy.switchRole(portId, newRole);
            } catch (RemoteException e) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to set the USB data role: portId=" + portId + ", newDataRole=" + UsbPort.dataRoleToString(newRole.role), e);
            }
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public void resetUsbPort(String portName, long transactionId, IUsbOperationInternal callback) {
        try {
            callback.onOperationComplete(2);
        } catch (RemoteException e) {
            UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete. opID:" + transactionId + " portId:" + portName, e);
        }
    }

    @Override // com.android.server.usb.hal.port.UsbPortHal
    public boolean enableUsbData(String portName, boolean enable, long transactionId, IUsbOperationInternal callback) {
        int i;
        boolean success;
        try {
            int halVersion = getUsbHalVersion();
            if (halVersion != 13) {
                try {
                    callback.onOperationComplete(2);
                } catch (RemoteException e) {
                    UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete. opID:" + transactionId + " portId:" + portName, e);
                }
                return false;
            }
            synchronized (this.mLock) {
                i = 1;
                try {
                    android.hardware.usb.V1_3.IUsb proxy = android.hardware.usb.V1_3.IUsb.castFrom((IHwInterface) this.mProxy);
                    success = proxy.enableUsbDataSignal(enable);
                } catch (RemoteException e2) {
                    UsbPortManager.logAndPrintException(this.mPw, "Failed enableUsbData: opId:" + transactionId + " portId=" + portName, e2);
                    try {
                        callback.onOperationComplete(1);
                    } catch (RemoteException r) {
                        UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete. opID:" + transactionId + " portId:" + portName, r);
                    }
                    return false;
                }
            }
            if (success) {
                sUsbDataStatus = enable ? 0 : 16;
            }
            if (success) {
                i = 0;
            }
            try {
                callback.onOperationComplete(i);
            } catch (RemoteException r2) {
                UsbPortManager.logAndPrintException(this.mPw, "Failed to call onOperationComplete. opID:" + transactionId + " portId:" + portName, r2);
            }
            return false;
        } catch (RemoteException e3) {
            UsbPortManager.logAndPrintException(this.mPw, "Failed to query USB HAL version. opID:" + transactionId + " portId:" + portName, e3);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class HALCallback extends IUsbCallback.Stub {
        public UsbPortManager mPortManager;
        public IndentingPrintWriter mPw;
        public UsbPortHidl mUsbPortHidl;

        HALCallback(IndentingPrintWriter pw, UsbPortManager portManager, UsbPortHidl usbPortHidl) {
            this.mPw = pw;
            this.mPortManager = portManager;
            this.mUsbPortHidl = usbPortHidl;
        }

        @Override // android.hardware.usb.V1_0.IUsbCallback
        public void notifyPortStatusChange(ArrayList<PortStatus> currentPortStatus, int retval) {
            if (!this.mUsbPortHidl.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.mPw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList<>();
            Iterator<PortStatus> it = currentPortStatus.iterator();
            while (it.hasNext()) {
                PortStatus current = it.next();
                RawPortInfo temp = new RawPortInfo(current.portName, current.supportedModes, 0, current.currentMode, current.canChangeMode, current.currentPowerRole, current.canChangePowerRole, current.currentDataRole, current.canChangeDataRole, false, 0, false, 0, UsbPortHidl.sUsbDataStatus, false, 0);
                newPortInfo.add(temp);
                UsbPortManager.logAndPrint(4, this.mPw, "ClientCallback V1_0: " + current.portName);
            }
            this.mPortManager.updatePorts(newPortInfo);
        }

        @Override // android.hardware.usb.V1_1.IUsbCallback
        public void notifyPortStatusChange_1_1(ArrayList<PortStatus_1_1> currentPortStatus, int retval) {
            if (!this.mUsbPortHidl.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.mPw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList<>();
            int numStatus = currentPortStatus.size();
            for (int i = 0; i < numStatus; i++) {
                PortStatus_1_1 current = currentPortStatus.get(i);
                RawPortInfo temp = new RawPortInfo(current.status.portName, current.supportedModes, 0, current.currentMode, current.status.canChangeMode, current.status.currentPowerRole, current.status.canChangePowerRole, current.status.currentDataRole, current.status.canChangeDataRole, false, 0, false, 0, UsbPortHidl.sUsbDataStatus, false, 0);
                newPortInfo.add(temp);
                UsbPortManager.logAndPrint(4, this.mPw, "ClientCallback V1_1: " + current.status.portName);
            }
            this.mPortManager.updatePorts(newPortInfo);
        }

        @Override // android.hardware.usb.V1_2.IUsbCallback
        public void notifyPortStatusChange_1_2(ArrayList<android.hardware.usb.V1_2.PortStatus> currentPortStatus, int retval) {
            if (!this.mUsbPortHidl.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.mPw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList<>();
            int i = 0;
            for (int numStatus = currentPortStatus.size(); i < numStatus; numStatus = numStatus) {
                android.hardware.usb.V1_2.PortStatus current = currentPortStatus.get(i);
                RawPortInfo temp = new RawPortInfo(current.status_1_1.status.portName, current.status_1_1.supportedModes, current.supportedContaminantProtectionModes, current.status_1_1.currentMode, current.status_1_1.status.canChangeMode, current.status_1_1.status.currentPowerRole, current.status_1_1.status.canChangePowerRole, current.status_1_1.status.currentDataRole, current.status_1_1.status.canChangeDataRole, current.supportsEnableContaminantPresenceProtection, current.contaminantProtectionStatus, current.supportsEnableContaminantPresenceDetection, current.contaminantDetectionStatus, UsbPortHidl.sUsbDataStatus, false, 0);
                newPortInfo.add(temp);
                UsbPortManager.logAndPrint(4, this.mPw, "ClientCallback V1_2: " + current.status_1_1.status.portName);
                i++;
            }
            this.mPortManager.updatePorts(newPortInfo);
        }

        @Override // android.hardware.usb.V1_0.IUsbCallback
        public void notifyRoleSwitchStatus(String portName, PortRole role, int retval) {
            if (retval == 0) {
                UsbPortManager.logAndPrint(4, this.mPw, portName + " role switch successful");
            } else {
                UsbPortManager.logAndPrint(6, this.mPw, portName + " role switch failed");
            }
        }
    }
}
