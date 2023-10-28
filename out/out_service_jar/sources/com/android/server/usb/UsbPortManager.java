package com.android.server.usb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.IUsbOperationInternal;
import android.hardware.usb.ParcelableUsbPort;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.usb.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import com.android.server.usb.hal.port.RawPortInfo;
import com.android.server.usb.hal.port.UsbPortHal;
import com.android.server.usb.hal.port.UsbPortHalInstance;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
/* loaded from: classes2.dex */
public class UsbPortManager {
    private static final int MSG_SYSTEM_READY = 2;
    private static final int MSG_UPDATE_PORTS = 1;
    private static final String PORT_INFO = "port_info";
    private static final String TAG = "UsbPortManager";
    private final Context mContext;
    private int mIsPortContaminatedNotificationId;
    private NotificationManager mNotificationManager;
    private boolean mSystemReady;
    private long mTransactionId;
    private static final int COMBO_SOURCE_HOST = UsbPort.combineRolesAsBit(1, 1);
    private static final int COMBO_SOURCE_DEVICE = UsbPort.combineRolesAsBit(1, 2);
    private static final int COMBO_SINK_HOST = UsbPort.combineRolesAsBit(2, 1);
    private static final int COMBO_SINK_DEVICE = UsbPort.combineRolesAsBit(2, 2);
    private final Object mLock = new Object();
    private final ArrayMap<String, PortInfo> mPorts = new ArrayMap<>();
    private final ArrayMap<String, RawPortInfo> mSimulatedPorts = new ArrayMap<>();
    private final ArrayMap<String, Boolean> mConnected = new ArrayMap<>();
    private final ArrayMap<String, Integer> mContaminantStatus = new ArrayMap<>();
    private final Handler mHandler = new Handler(FgThread.get().getLooper()) { // from class: com.android.server.usb.UsbPortManager.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Bundle b = msg.getData();
                    ArrayList<RawPortInfo> PortInfo2 = b.getParcelableArrayList(UsbPortManager.PORT_INFO);
                    synchronized (UsbPortManager.this.mLock) {
                        UsbPortManager.this.updatePortsLocked(null, PortInfo2);
                    }
                    return;
                case 2:
                    UsbPortManager usbPortManager = UsbPortManager.this;
                    usbPortManager.mNotificationManager = (NotificationManager) usbPortManager.mContext.getSystemService("notification");
                    return;
                default:
                    return;
            }
        }
    };
    private UsbPortHal mUsbPortHal = UsbPortHalInstance.getInstance(this, null);

    public UsbPortManager(Context context) {
        this.mContext = context;
        logAndPrint(3, null, "getInstance done");
    }

    public void systemReady() {
        this.mSystemReady = true;
        UsbPortHal usbPortHal = this.mUsbPortHal;
        if (usbPortHal != null) {
            usbPortHal.systemReady();
            try {
                UsbPortHal usbPortHal2 = this.mUsbPortHal;
                long j = this.mTransactionId + 1;
                this.mTransactionId = j;
                usbPortHal2.queryPortStatus(j);
            } catch (Exception e) {
                logAndPrintException(null, "ServiceStart: Failed to query port status", e);
            }
        }
        this.mHandler.sendEmptyMessage(2);
    }

    private void updateContaminantNotification() {
        int i;
        int i2;
        PortInfo currentPortInfo = null;
        Resources r = this.mContext.getResources();
        int contaminantStatus = 2;
        for (PortInfo portInfo : this.mPorts.values()) {
            contaminantStatus = portInfo.mUsbPortStatus.getContaminantDetectionStatus();
            if (contaminantStatus != 3) {
                if (contaminantStatus == 1) {
                }
            }
            currentPortInfo = portInfo;
        }
        if (contaminantStatus == 3 && (i2 = this.mIsPortContaminatedNotificationId) != 52) {
            if (i2 == 53) {
                this.mNotificationManager.cancelAsUser(null, i2, UserHandle.ALL);
            }
            this.mIsPortContaminatedNotificationId = 52;
            CharSequence title = r.getText(17041665);
            String channel = SystemNotificationChannels.ALERTS;
            CharSequence message = r.getText(17041664);
            Intent intent = new Intent();
            intent.addFlags(268435456);
            intent.setComponent(ComponentName.unflattenFromString(r.getString(17040053)));
            intent.putExtra("port", (Parcelable) ParcelableUsbPort.of(currentPortInfo.mUsbPort));
            intent.putExtra("portStatus", (Parcelable) currentPortInfo.mUsbPortStatus);
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 67108864, null, UserHandle.CURRENT);
            Notification.Builder builder = new Notification.Builder(this.mContext, channel).setOngoing(true).setTicker(title).setColor(this.mContext.getColor(17170460)).setContentIntent(pi).setContentTitle(title).setContentText(message).setVisibility(1).setSmallIcon(17301642).setStyle(new Notification.BigTextStyle().bigText(message));
            Notification notification = builder.build();
            this.mNotificationManager.notifyAsUser(null, this.mIsPortContaminatedNotificationId, notification, UserHandle.ALL);
        } else if (contaminantStatus != 3 && (i = this.mIsPortContaminatedNotificationId) == 52) {
            this.mNotificationManager.cancelAsUser(null, i, UserHandle.ALL);
            this.mIsPortContaminatedNotificationId = 0;
            if (contaminantStatus == 2) {
                this.mIsPortContaminatedNotificationId = 53;
                CharSequence title2 = r.getText(17041667);
                String channel2 = SystemNotificationChannels.ALERTS;
                CharSequence message2 = r.getText(17041666);
                Notification.Builder builder2 = new Notification.Builder(this.mContext, channel2).setSmallIcon(17302907).setTicker(title2).setColor(this.mContext.getColor(17170460)).setContentTitle(title2).setContentText(message2).setVisibility(1).setStyle(new Notification.BigTextStyle().bigText(message2));
                Notification notification2 = builder2.build();
                this.mNotificationManager.notifyAsUser(null, this.mIsPortContaminatedNotificationId, notification2, UserHandle.ALL);
            }
        }
    }

    public UsbPort[] getPorts() {
        UsbPort[] result;
        synchronized (this.mLock) {
            int count = this.mPorts.size();
            result = new UsbPort[count];
            for (int i = 0; i < count; i++) {
                result[i] = this.mPorts.valueAt(i).mUsbPort;
            }
        }
        return result;
    }

    public UsbPortStatus getPortStatus(String portId) {
        UsbPortStatus usbPortStatus;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(portId);
            usbPortStatus = portInfo != null ? portInfo.mUsbPortStatus : null;
        }
        return usbPortStatus;
    }

    public void enableContaminantDetection(String portId, boolean enable, IndentingPrintWriter pw) {
        PortInfo portInfo = this.mPorts.get(portId);
        if (portInfo == null) {
            if (pw != null) {
                pw.println("No such USB port: " + portId);
            }
        } else if (!portInfo.mUsbPort.supportsEnableContaminantPresenceDetection()) {
        } else {
            if (!enable || portInfo.mUsbPortStatus.getContaminantDetectionStatus() == 1) {
                if ((!enable && portInfo.mUsbPortStatus.getContaminantDetectionStatus() == 1) || portInfo.mUsbPortStatus.getContaminantDetectionStatus() == 0) {
                    return;
                }
                try {
                    UsbPortHal usbPortHal = this.mUsbPortHal;
                    long j = this.mTransactionId + 1;
                    this.mTransactionId = j;
                    usbPortHal.enableContaminantPresenceDetection(portId, enable, j);
                } catch (Exception e) {
                    logAndPrintException(pw, "Failed to set contaminant detection", e);
                }
            }
        }
    }

    public void enableLimitPowerTransfer(String portId, boolean limit, long transactionId, IUsbOperationInternal callback, IndentingPrintWriter pw) {
        Objects.requireNonNull(portId);
        PortInfo portInfo = this.mPorts.get(portId);
        if (portInfo == null) {
            logAndPrint(6, pw, "enableLimitPowerTransfer: No such port: " + portId + " opId:" + transactionId);
            if (callback != null) {
                try {
                    callback.onOperationComplete(3);
                    return;
                } catch (RemoteException e) {
                    logAndPrintException(pw, "enableLimitPowerTransfer: Failed to call OperationComplete. opId:" + transactionId, e);
                    return;
                }
            }
            return;
        }
        try {
            this.mUsbPortHal.enableLimitPowerTransfer(portId, limit, transactionId, callback);
        } catch (Exception e2) {
            try {
                logAndPrintException(pw, "enableLimitPowerTransfer: Failed to limit power transfer. opId:" + transactionId, e2);
                if (callback != null) {
                    callback.onOperationComplete(1);
                }
            } catch (RemoteException e3) {
                logAndPrintException(pw, "enableLimitPowerTransfer:Failed to call onOperationComplete. opId:" + transactionId, e3);
            }
        }
    }

    public void enableUsbDataWhileDocked(String portId, long transactionId, IUsbOperationInternal callback, IndentingPrintWriter pw) {
        Objects.requireNonNull(portId);
        PortInfo portInfo = this.mPorts.get(portId);
        if (portInfo == null) {
            logAndPrint(6, pw, "enableUsbDataWhileDocked: No such port: " + portId + " opId:" + transactionId);
            if (callback != null) {
                try {
                    callback.onOperationComplete(3);
                    return;
                } catch (RemoteException e) {
                    logAndPrintException(pw, "enableUsbDataWhileDocked: Failed to call OperationComplete. opId:" + transactionId, e);
                    return;
                }
            }
            return;
        }
        try {
            this.mUsbPortHal.enableUsbDataWhileDocked(portId, transactionId, callback);
        } catch (Exception e2) {
            try {
                logAndPrintException(pw, "enableUsbDataWhileDocked: Failed to limit power transfer. opId:" + transactionId, e2);
                if (callback != null) {
                    callback.onOperationComplete(1);
                }
            } catch (RemoteException e3) {
                logAndPrintException(pw, "enableUsbDataWhileDocked:Failed to call onOperationComplete. opId:" + transactionId, e3);
            }
        }
    }

    public boolean enableUsbData(String portId, boolean enable, int transactionId, IUsbOperationInternal callback, IndentingPrintWriter pw) {
        Objects.requireNonNull(callback);
        Objects.requireNonNull(portId);
        PortInfo portInfo = this.mPorts.get(portId);
        if (portInfo == null) {
            logAndPrint(6, pw, "enableUsbData: No such port: " + portId + " opId:" + transactionId);
            try {
                callback.onOperationComplete(3);
            } catch (RemoteException e) {
                logAndPrintException(pw, "enableUsbData: Failed to call OperationComplete. opId:" + transactionId, e);
            }
            return false;
        }
        try {
            return this.mUsbPortHal.enableUsbData(portId, enable, transactionId, callback);
        } catch (Exception e2) {
            try {
                logAndPrintException(pw, "enableUsbData: Failed to invoke enableUsbData. opId:" + transactionId, e2);
                callback.onOperationComplete(1);
            } catch (RemoteException e3) {
                logAndPrintException(pw, "enableUsbData: Failed to call onOperationComplete. opId:" + transactionId, e3);
            }
            return false;
        }
    }

    public int getUsbHalVersion() {
        UsbPortHal usbPortHal = this.mUsbPortHal;
        if (usbPortHal != null) {
            try {
                return usbPortHal.getUsbHalVersion();
            } catch (RemoteException e) {
                return -2;
            }
        }
        return -2;
    }

    private int toHalUsbDataRole(int usbDataRole) {
        if (usbDataRole == 2) {
            return 2;
        }
        return 1;
    }

    private int toHalUsbPowerRole(int usbPowerRole) {
        if (usbPowerRole == 2) {
            return 2;
        }
        return 1;
    }

    private int toHalUsbMode(int usbMode) {
        if (usbMode != 1) {
            return 1;
        }
        return 2;
    }

    public void resetUsbPort(String portId, int transactionId, IUsbOperationInternal callback, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            Objects.requireNonNull(callback);
            Objects.requireNonNull(portId);
            PortInfo portInfo = this.mPorts.get(portId);
            if (portInfo == null) {
                logAndPrint(6, pw, "resetUsbPort: No such port: " + portId + " opId:" + transactionId);
                try {
                    callback.onOperationComplete(3);
                } catch (RemoteException e) {
                    logAndPrintException(pw, "resetUsbPort: Failed to call OperationComplete. opId:" + transactionId, e);
                }
            }
            try {
                this.mUsbPortHal.resetUsbPort(portId, transactionId, callback);
            } catch (Exception e2) {
                try {
                    logAndPrintException(pw, "reseetUsbPort: Failed to resetUsbPort. opId:" + transactionId, e2);
                    callback.onOperationComplete(1);
                } catch (RemoteException e3) {
                    logAndPrintException(pw, "resetUsbPort: Failed to call onOperationComplete. opId:" + transactionId, e3);
                }
            }
        }
    }

    public void setPortRoles(String portId, int newPowerRole, int newDataRole, IndentingPrintWriter pw) {
        int newMode;
        UsbPortHal usbPortHal;
        int halUsbMode;
        synchronized (this.mLock) {
            PortInfo portInfo = this.mPorts.get(portId);
            if (portInfo == null) {
                if (pw != null) {
                    pw.println("No such USB port: " + portId);
                }
            } else if (!portInfo.mUsbPortStatus.isRoleCombinationSupported(newPowerRole, newDataRole)) {
                logAndPrint(6, pw, "Attempted to set USB port into unsupported role combination: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
            } else {
                int currentDataRole = portInfo.mUsbPortStatus.getCurrentDataRole();
                int currentPowerRole = portInfo.mUsbPortStatus.getCurrentPowerRole();
                if (currentDataRole == newDataRole && currentPowerRole == newPowerRole) {
                    if (pw != null) {
                        pw.println("No change.");
                    }
                    return;
                }
                boolean canChangeMode = portInfo.mCanChangeMode;
                boolean canChangePowerRole = portInfo.mCanChangePowerRole;
                boolean canChangeDataRole = portInfo.mCanChangeDataRole;
                int currentMode = portInfo.mUsbPortStatus.getCurrentMode();
                if ((!canChangePowerRole && currentPowerRole != newPowerRole) || (!canChangeDataRole && currentDataRole != newDataRole)) {
                    if (canChangeMode && newPowerRole == 1 && newDataRole == 1) {
                        newMode = 2;
                    } else if (canChangeMode && newPowerRole == 2 && newDataRole == 2) {
                        newMode = 1;
                    } else {
                        logAndPrint(6, pw, "Found mismatch in supported USB role combinations while attempting to change role: " + portInfo + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
                        return;
                    }
                } else {
                    newMode = currentMode;
                }
                logAndPrint(4, pw, "Setting USB port mode and role: portId=" + portId + ", currentMode=" + UsbPort.modeToString(currentMode) + ", currentPowerRole=" + UsbPort.powerRoleToString(currentPowerRole) + ", currentDataRole=" + UsbPort.dataRoleToString(currentDataRole) + ", newMode=" + UsbPort.modeToString(newMode) + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole) + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole));
                RawPortInfo sim = this.mSimulatedPorts.get(portId);
                if (sim != null) {
                    sim.currentMode = newMode;
                    sim.currentPowerRole = newPowerRole;
                    sim.currentDataRole = newDataRole;
                    updatePortsLocked(pw, null);
                } else {
                    UsbPortHal usbPortHal2 = this.mUsbPortHal;
                    if (usbPortHal2 != null) {
                        if (currentMode != newMode) {
                            logAndPrint(6, pw, "Trying to set the USB port mode: portId=" + portId + ", newMode=" + UsbPort.modeToString(newMode));
                            try {
                                usbPortHal = this.mUsbPortHal;
                                halUsbMode = toHalUsbMode(newMode);
                            } catch (Exception e) {
                                e = e;
                            }
                            try {
                                long j = this.mTransactionId + 1;
                                this.mTransactionId = j;
                                usbPortHal.switchMode(portId, halUsbMode, j);
                            } catch (Exception e2) {
                                e = e2;
                                logAndPrintException(pw, "Failed to set the USB port mode: portId=" + portId + ", newMode=" + UsbPort.modeToString(newMode), e);
                            }
                        } else {
                            if (currentPowerRole != newPowerRole) {
                                try {
                                    int halUsbPowerRole = toHalUsbPowerRole(newPowerRole);
                                    long j2 = this.mTransactionId + 1;
                                    this.mTransactionId = j2;
                                    usbPortHal2.switchPowerRole(portId, halUsbPowerRole, j2);
                                } catch (Exception e3) {
                                    logAndPrintException(pw, "Failed to set the USB port power role: portId=" + portId + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole), e3);
                                    return;
                                }
                            }
                            if (currentDataRole != newDataRole) {
                                try {
                                    UsbPortHal usbPortHal3 = this.mUsbPortHal;
                                    int halUsbDataRole = toHalUsbDataRole(newDataRole);
                                    long j3 = this.mTransactionId + 1;
                                    this.mTransactionId = j3;
                                    usbPortHal3.switchDataRole(portId, halUsbDataRole, j3);
                                } catch (Exception e4) {
                                    logAndPrintException(pw, "Failed to set the USB port data role: portId=" + portId + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole), e4);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void updatePorts(ArrayList<RawPortInfo> newPortInfo) {
        Message message = this.mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(PORT_INFO, newPortInfo);
        message.what = 1;
        message.setData(bundle);
        this.mHandler.sendMessage(message);
    }

    public void addSimulatedPort(String portId, int supportedModes, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mSimulatedPorts.containsKey(portId)) {
                pw.println("Port with same name already exists.  Please remove it first.");
                return;
            }
            pw.println("Adding simulated port: portId=" + portId + ", supportedModes=" + UsbPort.modeToString(supportedModes));
            this.mSimulatedPorts.put(portId, new RawPortInfo(portId, supportedModes));
            updatePortsLocked(pw, null);
        }
    }

    public void connectSimulatedPort(String portId, int mode, boolean canChangeMode, int powerRole, boolean canChangePowerRole, int dataRole, boolean canChangeDataRole, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            RawPortInfo portInfo = this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Cannot connect simulated port which does not exist.");
                return;
            }
            if (mode != 0 && powerRole != 0 && dataRole != 0) {
                if ((portInfo.supportedModes & mode) == 0) {
                    pw.println("Simulated port does not support mode: " + UsbPort.modeToString(mode));
                    return;
                }
                pw.println("Connecting simulated port: portId=" + portId + ", mode=" + UsbPort.modeToString(mode) + ", canChangeMode=" + canChangeMode + ", powerRole=" + UsbPort.powerRoleToString(powerRole) + ", canChangePowerRole=" + canChangePowerRole + ", dataRole=" + UsbPort.dataRoleToString(dataRole) + ", canChangeDataRole=" + canChangeDataRole);
                portInfo.currentMode = mode;
                portInfo.canChangeMode = canChangeMode;
                portInfo.currentPowerRole = powerRole;
                portInfo.canChangePowerRole = canChangePowerRole;
                portInfo.currentDataRole = dataRole;
                portInfo.canChangeDataRole = canChangeDataRole;
                updatePortsLocked(pw, null);
                return;
            }
            pw.println("Cannot connect simulated port in null mode, power role, or data role.");
        }
    }

    public void simulateContaminantStatus(String portId, boolean detected, IndentingPrintWriter pw) {
        int i;
        synchronized (this.mLock) {
            RawPortInfo portInfo = this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Simulated port not found.");
                return;
            }
            pw.println("Simulating wet port: portId=" + portId + ", wet=" + detected);
            if (detected) {
                i = 3;
            } else {
                i = 2;
            }
            portInfo.contaminantDetectionStatus = i;
            updatePortsLocked(pw, null);
        }
    }

    public void disconnectSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            RawPortInfo portInfo = this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Cannot disconnect simulated port which does not exist.");
                return;
            }
            pw.println("Disconnecting simulated port: portId=" + portId);
            portInfo.currentMode = 0;
            portInfo.canChangeMode = false;
            portInfo.currentPowerRole = 0;
            portInfo.canChangePowerRole = false;
            portInfo.currentDataRole = 0;
            portInfo.canChangeDataRole = false;
            updatePortsLocked(pw, null);
        }
    }

    public void removeSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            int index = this.mSimulatedPorts.indexOfKey(portId);
            if (index < 0) {
                pw.println("Cannot remove simulated port which does not exist.");
                return;
            }
            pw.println("Disconnecting simulated port: portId=" + portId);
            this.mSimulatedPorts.removeAt(index);
            updatePortsLocked(pw, null);
        }
    }

    public void resetSimulation(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Removing all simulated ports and ending simulation.");
            if (!this.mSimulatedPorts.isEmpty()) {
                this.mSimulatedPorts.clear();
                updatePortsLocked(pw, null);
            }
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        synchronized (this.mLock) {
            dump.write("is_simulation_active", 1133871366145L, !this.mSimulatedPorts.isEmpty());
            for (PortInfo portInfo : this.mPorts.values()) {
                portInfo.dump(dump, "usb_ports", 2246267895810L);
            }
            dump.write("usb_hal_version", 1159641169924L, getUsbHalVersion());
        }
        dump.end(token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePortsLocked(IndentingPrintWriter pw, ArrayList<RawPortInfo> newPortInfo) {
        UsbPortManager usbPortManager = this;
        int i = usbPortManager.mPorts.size();
        while (true) {
            int i2 = i - 1;
            if (i <= 0) {
                break;
            }
            usbPortManager.mPorts.valueAt(i2).mDisposition = 3;
            i = i2;
        }
        if (!usbPortManager.mSimulatedPorts.isEmpty()) {
            int i3 = 0;
            for (int count = usbPortManager.mSimulatedPorts.size(); i3 < count; count = count) {
                RawPortInfo portInfo = usbPortManager.mSimulatedPorts.valueAt(i3);
                usbPortManager = this;
                usbPortManager.addOrUpdatePortLocked(portInfo.portId, portInfo.supportedModes, portInfo.supportedContaminantProtectionModes, portInfo.currentMode, portInfo.canChangeMode, portInfo.currentPowerRole, portInfo.canChangePowerRole, portInfo.currentDataRole, portInfo.canChangeDataRole, portInfo.supportsEnableContaminantPresenceProtection, portInfo.contaminantProtectionStatus, portInfo.supportsEnableContaminantPresenceDetection, portInfo.contaminantDetectionStatus, portInfo.usbDataStatus, portInfo.powerTransferLimited, portInfo.powerBrickConnectionStatus, pw);
                i3++;
            }
        } else {
            Iterator<RawPortInfo> it = newPortInfo.iterator();
            while (it.hasNext()) {
                RawPortInfo currentPortInfo = it.next();
                addOrUpdatePortLocked(currentPortInfo.portId, currentPortInfo.supportedModes, currentPortInfo.supportedContaminantProtectionModes, currentPortInfo.currentMode, currentPortInfo.canChangeMode, currentPortInfo.currentPowerRole, currentPortInfo.canChangePowerRole, currentPortInfo.currentDataRole, currentPortInfo.canChangeDataRole, currentPortInfo.supportsEnableContaminantPresenceProtection, currentPortInfo.contaminantProtectionStatus, currentPortInfo.supportsEnableContaminantPresenceDetection, currentPortInfo.contaminantDetectionStatus, currentPortInfo.usbDataStatus, currentPortInfo.powerTransferLimited, currentPortInfo.powerBrickConnectionStatus, pw);
            }
        }
        int i4 = this.mPorts.size();
        while (true) {
            int i5 = i4 - 1;
            if (i4 > 0) {
                PortInfo portInfo2 = this.mPorts.valueAt(i5);
                switch (portInfo2.mDisposition) {
                    case 0:
                        handlePortAddedLocked(portInfo2, pw);
                        portInfo2.mDisposition = 2;
                        break;
                    case 1:
                        handlePortChangedLocked(portInfo2, pw);
                        portInfo2.mDisposition = 2;
                        break;
                    case 3:
                        this.mPorts.removeAt(i5);
                        portInfo2.mUsbPortStatus = null;
                        handlePortRemovedLocked(portInfo2, pw);
                        break;
                }
                i4 = i5;
            } else {
                return;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x00a9  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00eb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void addOrUpdatePortLocked(String portId, int supportedModes, int supportedContaminantProtectionModes, int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, boolean supportsEnableContaminantPresenceProtection, int contaminantProtectionStatus, boolean supportsEnableContaminantPresenceDetection, int contaminantDetectionStatus, int usbDataStatus, boolean powerTransferLimited, int powerBrickConnectionStatus, IndentingPrintWriter pw) {
        boolean canChangeMode2;
        int currentMode2;
        int supportedRoleCombinations;
        PortInfo portInfo;
        if ((supportedModes & 3) == 3) {
            canChangeMode2 = canChangeMode;
            currentMode2 = currentMode;
        } else if (currentMode != 0 && currentMode != supportedModes) {
            logAndPrint(5, pw, "Ignoring inconsistent current mode from USB port driver: supportedModes=" + UsbPort.modeToString(supportedModes) + ", currentMode=" + UsbPort.modeToString(currentMode));
            currentMode2 = 0;
            canChangeMode2 = false;
        } else {
            currentMode2 = currentMode;
            canChangeMode2 = false;
        }
        int supportedRoleCombinations2 = UsbPort.combineRolesAsBit(currentPowerRole, currentDataRole);
        if (currentMode2 != 0 && currentPowerRole != 0 && currentDataRole != 0) {
            if (canChangePowerRole && canChangeDataRole) {
                supportedRoleCombinations = supportedRoleCombinations2 | COMBO_SOURCE_HOST | COMBO_SOURCE_DEVICE | COMBO_SINK_HOST | COMBO_SINK_DEVICE;
            } else if (canChangePowerRole) {
                supportedRoleCombinations = supportedRoleCombinations2 | UsbPort.combineRolesAsBit(1, currentDataRole) | UsbPort.combineRolesAsBit(2, currentDataRole);
            } else if (canChangeDataRole) {
                supportedRoleCombinations = supportedRoleCombinations2 | UsbPort.combineRolesAsBit(currentPowerRole, 1) | UsbPort.combineRolesAsBit(currentPowerRole, 2);
            } else if (canChangeMode2) {
                supportedRoleCombinations = supportedRoleCombinations2 | COMBO_SOURCE_HOST | COMBO_SINK_DEVICE;
            }
            portInfo = this.mPorts.get(portId);
            if (portInfo != null) {
                PortInfo portInfo2 = new PortInfo((UsbManager) this.mContext.getSystemService(UsbManager.class), portId, supportedModes, supportedContaminantProtectionModes, supportsEnableContaminantPresenceProtection, supportsEnableContaminantPresenceDetection);
                portInfo2.setStatus(currentMode2, canChangeMode2, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, supportedRoleCombinations, contaminantProtectionStatus, contaminantDetectionStatus, usbDataStatus, powerTransferLimited, powerBrickConnectionStatus);
                this.mPorts.put(portId, portInfo2);
                return;
            }
            if (supportedModes != portInfo.mUsbPort.getSupportedModes()) {
                logAndPrint(5, pw, "Ignoring inconsistent list of supported modes from USB port driver (should be immutable): previous=" + UsbPort.modeToString(portInfo.mUsbPort.getSupportedModes()) + ", current=" + UsbPort.modeToString(supportedModes));
            }
            if (supportsEnableContaminantPresenceProtection != portInfo.mUsbPort.supportsEnableContaminantPresenceProtection()) {
                logAndPrint(5, pw, "Ignoring inconsistent supportsEnableContaminantPresenceProtectionUSB port driver (should be immutable): previous=" + portInfo.mUsbPort.supportsEnableContaminantPresenceProtection() + ", current=" + supportsEnableContaminantPresenceProtection);
            }
            if (supportsEnableContaminantPresenceDetection != portInfo.mUsbPort.supportsEnableContaminantPresenceDetection()) {
                logAndPrint(5, pw, "Ignoring inconsistent supportsEnableContaminantPresenceDetection USB port driver (should be immutable): previous=" + portInfo.mUsbPort.supportsEnableContaminantPresenceDetection() + ", current=" + supportsEnableContaminantPresenceDetection);
            }
            if (portInfo.setStatus(currentMode2, canChangeMode2, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, supportedRoleCombinations, contaminantProtectionStatus, contaminantDetectionStatus, usbDataStatus, powerTransferLimited, powerBrickConnectionStatus)) {
                portInfo.mDisposition = 1;
            } else {
                portInfo.mDisposition = 2;
            }
            return;
        }
        supportedRoleCombinations = supportedRoleCombinations2;
        portInfo = this.mPorts.get(portId);
        if (portInfo != null) {
        }
    }

    private void handlePortLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        sendPortChangedBroadcastLocked(portInfo);
        logToStatsd(portInfo, pw);
        updateContaminantNotification();
    }

    private void handlePortAddedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port added: " + portInfo);
        handlePortLocked(portInfo, pw);
    }

    private void handlePortChangedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port changed: " + portInfo);
        enableContaminantDetectionIfNeeded(portInfo, pw);
        disableLimitPowerTransferIfNeeded(portInfo, pw);
        handlePortLocked(portInfo, pw);
    }

    private void handlePortRemovedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        logAndPrint(4, pw, "USB port removed: " + portInfo);
        handlePortLocked(portInfo, pw);
    }

    private static int convertContaminantDetectionStatusToProto(int contaminantDetectionStatus) {
        switch (contaminantDetectionStatus) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return 0;
        }
    }

    private void sendPortChangedBroadcastLocked(PortInfo portInfo) {
        final Intent intent = new Intent("android.hardware.usb.action.USB_PORT_CHANGED");
        intent.addFlags(AudioFormat.EVRCB);
        intent.putExtra("port", (Parcelable) ParcelableUsbPort.of(portInfo.mUsbPort));
        intent.putExtra("portStatus", (Parcelable) portInfo.mUsbPortStatus);
        this.mHandler.post(new Runnable() { // from class: com.android.server.usb.UsbPortManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UsbPortManager.this.m7387xa05031b6(intent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPortChangedBroadcastLocked$0$com-android-server-usb-UsbPortManager  reason: not valid java name */
    public /* synthetic */ void m7387xa05031b6(Intent intent) {
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.MANAGE_USB");
    }

    private void enableContaminantDetectionIfNeeded(PortInfo portInfo, IndentingPrintWriter pw) {
        if (this.mConnected.containsKey(portInfo.mUsbPort.getId()) && this.mConnected.get(portInfo.mUsbPort.getId()).booleanValue() && !portInfo.mUsbPortStatus.isConnected() && portInfo.mUsbPortStatus.getContaminantDetectionStatus() == 1) {
            enableContaminantDetection(portInfo.mUsbPort.getId(), true, pw);
        }
    }

    private void disableLimitPowerTransferIfNeeded(PortInfo portInfo, IndentingPrintWriter pw) {
        if (this.mConnected.containsKey(portInfo.mUsbPort.getId()) && this.mConnected.get(portInfo.mUsbPort.getId()).booleanValue() && !portInfo.mUsbPortStatus.isConnected() && portInfo.mUsbPortStatus.isPowerTransferLimited()) {
            String id = portInfo.mUsbPort.getId();
            long j = 1 + this.mTransactionId;
            this.mTransactionId = j;
            enableLimitPowerTransfer(id, false, j, null, pw);
        }
    }

    private void logToStatsd(PortInfo portInfo, IndentingPrintWriter pw) {
        if (portInfo.mUsbPortStatus == null) {
            if (this.mConnected.containsKey(portInfo.mUsbPort.getId())) {
                if (this.mConnected.get(portInfo.mUsbPort.getId()).booleanValue()) {
                    FrameworkStatsLog.write(70, 0, portInfo.mUsbPort.getId(), portInfo.mLastConnectDurationMillis);
                }
                this.mConnected.remove(portInfo.mUsbPort.getId());
            }
            if (this.mContaminantStatus.containsKey(portInfo.mUsbPort.getId())) {
                if (this.mContaminantStatus.get(portInfo.mUsbPort.getId()).intValue() == 3) {
                    FrameworkStatsLog.write(146, portInfo.mUsbPort.getId(), convertContaminantDetectionStatusToProto(2));
                }
                this.mContaminantStatus.remove(portInfo.mUsbPort.getId());
                return;
            }
            return;
        }
        if (!this.mConnected.containsKey(portInfo.mUsbPort.getId()) || this.mConnected.get(portInfo.mUsbPort.getId()).booleanValue() != portInfo.mUsbPortStatus.isConnected()) {
            this.mConnected.put(portInfo.mUsbPort.getId(), Boolean.valueOf(portInfo.mUsbPortStatus.isConnected()));
            FrameworkStatsLog.write(70, portInfo.mUsbPortStatus.isConnected() ? 1 : 0, portInfo.mUsbPort.getId(), portInfo.mLastConnectDurationMillis);
        }
        if (!this.mContaminantStatus.containsKey(portInfo.mUsbPort.getId()) || this.mContaminantStatus.get(portInfo.mUsbPort.getId()).intValue() != portInfo.mUsbPortStatus.getContaminantDetectionStatus()) {
            this.mContaminantStatus.put(portInfo.mUsbPort.getId(), Integer.valueOf(portInfo.mUsbPortStatus.getContaminantDetectionStatus()));
            FrameworkStatsLog.write(146, portInfo.mUsbPort.getId(), convertContaminantDetectionStatusToProto(portInfo.mUsbPortStatus.getContaminantDetectionStatus()));
        }
    }

    public static void logAndPrint(int priority, IndentingPrintWriter pw, String msg) {
        Slog.println(priority, TAG, msg);
        if (pw != null) {
            pw.println(msg);
        }
    }

    public static void logAndPrintException(IndentingPrintWriter pw, String msg, Exception e) {
        Slog.e(TAG, msg, e);
        if (pw != null) {
            pw.println(msg + e);
        }
    }

    /* loaded from: classes2.dex */
    public static final class PortInfo {
        public static final int DISPOSITION_ADDED = 0;
        public static final int DISPOSITION_CHANGED = 1;
        public static final int DISPOSITION_READY = 2;
        public static final int DISPOSITION_REMOVED = 3;
        public boolean mCanChangeDataRole;
        public boolean mCanChangeMode;
        public boolean mCanChangePowerRole;
        public long mConnectedAtMillis;
        public int mDisposition;
        public long mLastConnectDurationMillis;
        public final UsbPort mUsbPort;
        public UsbPortStatus mUsbPortStatus;

        PortInfo(UsbManager usbManager, String portId, int supportedModes, int supportedContaminantProtectionModes, boolean supportsEnableContaminantPresenceDetection, boolean supportsEnableContaminantPresenceProtection) {
            this.mUsbPort = new UsbPort(usbManager, portId, supportedModes, supportedContaminantProtectionModes, supportsEnableContaminantPresenceDetection, supportsEnableContaminantPresenceProtection);
        }

        /* JADX WARN: Code restructure failed: missing block: B:11:0x0037, code lost:
            if (r20.mUsbPortStatus.getSupportedRoleCombinations() == r27) goto L11;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean setStatus(int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, int supportedRoleCombinations) {
            boolean dispositionChanged = false;
            this.mCanChangeMode = canChangeMode;
            this.mCanChangePowerRole = canChangePowerRole;
            this.mCanChangeDataRole = canChangeDataRole;
            UsbPortStatus usbPortStatus = this.mUsbPortStatus;
            if (usbPortStatus != null) {
                if (usbPortStatus.getCurrentMode() == currentMode) {
                    if (this.mUsbPortStatus.getCurrentPowerRole() == currentPowerRole) {
                        if (this.mUsbPortStatus.getCurrentDataRole() != currentDataRole) {
                        }
                    }
                }
            }
            this.mUsbPortStatus = new UsbPortStatus(currentMode, currentPowerRole, currentDataRole, supportedRoleCombinations, 0, 0, 0, false, 0);
            dispositionChanged = true;
            if (this.mUsbPortStatus.isConnected() && this.mConnectedAtMillis == 0) {
                this.mConnectedAtMillis = SystemClock.elapsedRealtime();
                this.mLastConnectDurationMillis = 0L;
            } else if (!this.mUsbPortStatus.isConnected() && this.mConnectedAtMillis != 0) {
                this.mLastConnectDurationMillis = SystemClock.elapsedRealtime() - this.mConnectedAtMillis;
                this.mConnectedAtMillis = 0L;
            }
            return dispositionChanged;
        }

        /* JADX WARN: Code restructure failed: missing block: B:21:0x0069, code lost:
            if (r16.mUsbPortStatus.getPowerBrickConnectionStatus() == r28) goto L21;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean setStatus(int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, int supportedRoleCombinations, int contaminantProtectionStatus, int contaminantDetectionStatus, int usbDataStatus, boolean powerTransferLimited, int powerBrickConnectionStatus) {
            boolean dispositionChanged = false;
            this.mCanChangeMode = canChangeMode;
            this.mCanChangePowerRole = canChangePowerRole;
            this.mCanChangeDataRole = canChangeDataRole;
            UsbPortStatus usbPortStatus = this.mUsbPortStatus;
            if (usbPortStatus != null) {
                if (usbPortStatus.getCurrentMode() == currentMode) {
                    if (this.mUsbPortStatus.getCurrentPowerRole() == currentPowerRole) {
                        if (this.mUsbPortStatus.getCurrentDataRole() == currentDataRole) {
                            if (this.mUsbPortStatus.getSupportedRoleCombinations() == supportedRoleCombinations) {
                                if (this.mUsbPortStatus.getContaminantProtectionStatus() == contaminantProtectionStatus) {
                                    if (this.mUsbPortStatus.getContaminantDetectionStatus() == contaminantDetectionStatus) {
                                        if (this.mUsbPortStatus.getUsbDataStatus() == usbDataStatus) {
                                            if (this.mUsbPortStatus.isPowerTransferLimited() != powerTransferLimited) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            this.mUsbPortStatus = new UsbPortStatus(currentMode, currentPowerRole, currentDataRole, supportedRoleCombinations, contaminantProtectionStatus, contaminantDetectionStatus, usbDataStatus, powerTransferLimited, powerBrickConnectionStatus);
            dispositionChanged = true;
            if (this.mUsbPortStatus.isConnected() && this.mConnectedAtMillis == 0) {
                this.mConnectedAtMillis = SystemClock.elapsedRealtime();
                this.mLastConnectDurationMillis = 0L;
            } else if (!this.mUsbPortStatus.isConnected() && this.mConnectedAtMillis != 0) {
                this.mLastConnectDurationMillis = SystemClock.elapsedRealtime() - this.mConnectedAtMillis;
                this.mConnectedAtMillis = 0L;
            }
            return dispositionChanged;
        }

        void dump(DualDumpOutputStream dump, String idName, long id) {
            long token = dump.start(idName, id);
            DumpUtils.writePort(dump, "port", 1146756268033L, this.mUsbPort);
            DumpUtils.writePortStatus(dump, "status", 1146756268034L, this.mUsbPortStatus);
            dump.write("can_change_mode", 1133871366147L, this.mCanChangeMode);
            dump.write("can_change_power_role", 1133871366148L, this.mCanChangePowerRole);
            dump.write("can_change_data_role", 1133871366149L, this.mCanChangeDataRole);
            dump.write("connected_at_millis", 1112396529670L, this.mConnectedAtMillis);
            dump.write("last_connect_duration_millis", 1112396529671L, this.mLastConnectDurationMillis);
            dump.end(token);
        }

        public String toString() {
            return "port=" + this.mUsbPort + ", status=" + this.mUsbPortStatus + ", canChangeMode=" + this.mCanChangeMode + ", canChangePowerRole=" + this.mCanChangePowerRole + ", canChangeDataRole=" + this.mCanChangeDataRole + ", connectedAtMillis=" + this.mConnectedAtMillis + ", lastConnectDurationMillis=" + this.mLastConnectDurationMillis;
        }
    }
}
