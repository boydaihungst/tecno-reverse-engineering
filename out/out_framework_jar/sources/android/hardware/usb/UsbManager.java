package android.hardware.usb;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes2.dex */
public class UsbManager {
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED";
    public static final String ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";
    @SystemApi
    public static final String ACTION_USB_ACCESSORY_HANDSHAKE = "android.hardware.usb.action.USB_ACCESSORY_HANDSHAKE";
    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    @SystemApi
    public static final String ACTION_USB_PORT_CHANGED = "android.hardware.usb.action.USB_PORT_CHANGED";
    @SystemApi
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String EXTRA_ACCESSORY = "accessory";
    @SystemApi
    public static final String EXTRA_ACCESSORY_HANDSHAKE_END = "android.hardware.usb.extra.ACCESSORY_HANDSHAKE_END";
    @SystemApi
    public static final String EXTRA_ACCESSORY_START = "android.hardware.usb.extra.ACCESSORY_START";
    @SystemApi
    public static final String EXTRA_ACCESSORY_STRING_COUNT = "android.hardware.usb.extra.ACCESSORY_STRING_COUNT";
    @SystemApi
    public static final String EXTRA_ACCESSORY_UEVENT_TIME = "android.hardware.usb.extra.ACCESSORY_UEVENT_TIME";
    public static final String EXTRA_CAN_BE_DEFAULT = "android.hardware.usb.extra.CAN_BE_DEFAULT";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_PACKAGE = "android.hardware.usb.extra.PACKAGE";
    public static final String EXTRA_PERMISSION_GRANTED = "permission";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_PORT_STATUS = "portStatus";
    @SystemApi
    public static final long FUNCTION_ACCESSORY = 2;
    @SystemApi
    public static final long FUNCTION_ADB = 1;
    @SystemApi
    public static final long FUNCTION_AUDIO_SOURCE = 64;
    public static final long FUNCTION_BYPASS = 32768;
    @SystemApi
    public static final long FUNCTION_MIDI = 8;
    @SystemApi
    public static final long FUNCTION_MTP = 4;
    private static final Map<String, Long> FUNCTION_NAME_TO_CODE;
    @SystemApi
    public static final long FUNCTION_NCM = 1024;
    @SystemApi
    public static final long FUNCTION_NONE = 0;
    @SystemApi
    public static final long FUNCTION_PTP = 16;
    @SystemApi
    public static final long FUNCTION_RNDIS = 32;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int GADGET_HAL_NOT_SUPPORTED = -1;
    public static final String GADGET_HAL_UNKNOWN = "unknown";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int GADGET_HAL_V1_0 = 10;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int GADGET_HAL_V1_1 = 11;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int GADGET_HAL_V1_2 = 12;
    public static final String GADGET_HAL_VERSION_1_0 = "V1_0";
    public static final String GADGET_HAL_VERSION_1_1 = "V1_1";
    public static final String GADGET_HAL_VERSION_1_2 = "V1_2";
    private static final long SETTABLE_FUNCTIONS = 33852;
    private static final String TAG = "UsbManager";
    @SystemApi
    public static final String USB_CONFIGURED = "configured";
    @SystemApi
    public static final String USB_CONNECTED = "connected";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_10G = 10240;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_20G = 20480;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_40G = 40960;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_5G = 5120;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_FULL_SPEED = 12;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_HIGH_SPEED = 480;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_LOW_SPEED = 2;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_DATA_TRANSFER_RATE_UNKNOWN = -1;
    public static final String USB_DATA_UNLOCKED = "unlocked";
    public static final String USB_FUNCTION_ACCESSORY = "accessory";
    public static final String USB_FUNCTION_ADB = "adb";
    public static final String USB_FUNCTION_AUDIO_SOURCE = "audio_source";
    public static final String USB_FUNCTION_BYPASS = "via_bypass";
    public static final String USB_FUNCTION_MIDI = "midi";
    public static final String USB_FUNCTION_MTP = "mtp";
    @SystemApi
    public static final String USB_FUNCTION_NCM = "ncm";
    public static final String USB_FUNCTION_NONE = "none";
    public static final String USB_FUNCTION_PTP = "ptp";
    @SystemApi
    public static final String USB_FUNCTION_RNDIS = "rndis";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_NOT_SUPPORTED = -1;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_RETRY = -2;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_V1_0 = 10;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_V1_1 = 11;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_V1_2 = 12;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_V1_3 = 13;
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int USB_HAL_V2_0 = 20;
    public static final String USB_HOST_CONNECTED = "host_connected";
    private final Context mContext;
    private final IUsbManager mService;

    /* loaded from: classes2.dex */
    public @interface UsbFunctionMode {
    }

    /* loaded from: classes2.dex */
    public @interface UsbGadgetHalVersion {
    }

    /* loaded from: classes2.dex */
    public @interface UsbHalVersion {
    }

    static {
        HashMap hashMap = new HashMap();
        FUNCTION_NAME_TO_CODE = hashMap;
        hashMap.put(USB_FUNCTION_MTP, 4L);
        hashMap.put(USB_FUNCTION_PTP, 16L);
        hashMap.put(USB_FUNCTION_RNDIS, 32L);
        hashMap.put("midi", 8L);
        hashMap.put("accessory", 2L);
        hashMap.put(USB_FUNCTION_AUDIO_SOURCE, 64L);
        hashMap.put("adb", 1L);
        hashMap.put(USB_FUNCTION_NCM, 1024L);
        hashMap.put(USB_FUNCTION_BYPASS, 32768L);
    }

    public UsbManager(Context context, IUsbManager service) {
        this.mContext = context;
        this.mService = service;
    }

    public HashMap<String, UsbDevice> getDeviceList() {
        HashMap<String, UsbDevice> result = new HashMap<>();
        if (this.mService == null) {
            return result;
        }
        Bundle bundle = new Bundle();
        try {
            this.mService.getDeviceList(bundle);
            for (String name : bundle.keySet()) {
                result.put(name, (UsbDevice) bundle.get(name));
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UsbDeviceConnection openDevice(UsbDevice device) {
        try {
            String deviceName = device.getDeviceName();
            ParcelFileDescriptor pfd = this.mService.openDevice(deviceName, this.mContext.getPackageName());
            if (pfd != null) {
                UsbDeviceConnection connection = new UsbDeviceConnection(device);
                boolean result = connection.open(deviceName, pfd, this.mContext);
                pfd.close();
                if (result) {
                    return connection;
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "exception in UsbManager.openDevice", e);
            return null;
        }
    }

    public UsbAccessory[] getAccessoryList() {
        IUsbManager iUsbManager = this.mService;
        if (iUsbManager == null) {
            return null;
        }
        try {
            UsbAccessory accessory = iUsbManager.getCurrentAccessory();
            if (accessory == null) {
                return null;
            }
            return new UsbAccessory[]{accessory};
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        try {
            return this.mService.openAccessory(accessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ParcelFileDescriptor getControlFd(long function) {
        try {
            return this.mService.getControlFd(function);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasPermission(UsbDevice device) {
        IUsbManager iUsbManager = this.mService;
        if (iUsbManager == null) {
            return false;
        }
        try {
            return iUsbManager.hasDevicePermission(device, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasPermission(UsbAccessory accessory) {
        IUsbManager iUsbManager = this.mService;
        if (iUsbManager == null) {
            return false;
        }
        try {
            return iUsbManager.hasAccessoryPermission(accessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestPermission(UsbDevice device, PendingIntent pi) {
        try {
            this.mService.requestDevicePermission(device, this.mContext.getPackageName(), pi);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestPermission(UsbAccessory accessory, PendingIntent pi) {
        try {
            this.mService.requestAccessoryPermission(accessory, this.mContext.getPackageName(), pi);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void grantPermission(UsbDevice device) {
        grantPermission(device, Process.myUid());
    }

    public void grantPermission(UsbDevice device, int uid) {
        try {
            this.mService.grantDevicePermission(device, uid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void grantPermission(UsbDevice device, String packageName) {
        try {
            int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, this.mContext.getUserId());
            grantPermission(device, uid);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package " + packageName + " not found.", e);
        }
    }

    @Deprecated
    public boolean isFunctionEnabled(String function) {
        try {
            return this.mService.isFunctionEnabled(function);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setCurrentFunctions(long functions) {
        try {
            this.mService.setCurrentFunctions(functions);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setCurrentFunction(String functions, boolean usbDataUnlocked) {
        try {
            this.mService.setCurrentFunction(functions, usbDataUnlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public long getCurrentFunctions() {
        try {
            return this.mService.getCurrentFunctions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setScreenUnlockedFunctions(long functions) {
        try {
            this.mService.setScreenUnlockedFunctions(functions);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getScreenUnlockedFunctions() {
        try {
            return this.mService.getScreenUnlockedFunctions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public int getUsbBandwidthMbps() {
        try {
            int usbSpeed = this.mService.getCurrentUsbSpeed();
            return usbSpeedToBandwidth(usbSpeed);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public int getGadgetHalVersion() {
        try {
            return this.mService.getGadgetHalVersion();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public int getUsbHalVersion() {
        try {
            return this.mService.getUsbHalVersion();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void resetUsbGadget() {
        try {
            this.mService.resetUsbGadget();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean enableUsbDataSignal(boolean enable) {
        return setUsbDataSignal(getPorts(), !enable, true);
    }

    private boolean setUsbDataSignal(List<UsbPort> usbPorts, boolean disable, boolean revertOnFailure) {
        List<UsbPort> changedPorts = new ArrayList<>();
        for (int i = 0; i < usbPorts.size(); i++) {
            UsbPort port = usbPorts.get(i);
            if (isPortDisabled(port) != disable) {
                changedPorts.add(port);
                if (port.enableUsbData(!disable) != 0 && revertOnFailure) {
                    Log.e(TAG, "Failed to set usb data signal for portID(" + port.getId() + NavigationBarInflaterView.KEY_CODE_END);
                    setUsbDataSignal(changedPorts, !disable, false);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPortDisabled(UsbPort usbPort) {
        return (getPortStatus(usbPort).getUsbDataStatus() & 16) == 16;
    }

    @SystemApi
    public List<UsbPort> getPorts() {
        IUsbManager iUsbManager = this.mService;
        if (iUsbManager == null) {
            return Collections.emptyList();
        }
        try {
            List<ParcelableUsbPort> parcelablePorts = iUsbManager.getPorts();
            if (parcelablePorts == null) {
                return Collections.emptyList();
            }
            int numPorts = parcelablePorts.size();
            ArrayList<UsbPort> ports = new ArrayList<>(numPorts);
            for (int i = 0; i < numPorts; i++) {
                ports.add(parcelablePorts.get(i).getUsbPort(this));
            }
            return ports;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbPortStatus getPortStatus(UsbPort port) {
        try {
            return this.mService.getPortStatus(port.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPortRoles(UsbPort port, int powerRole, int dataRole) {
        Log.d(TAG, "setPortRoles Package:" + this.mContext.getPackageName());
        try {
            this.mService.setPortRoles(port.getId(), powerRole, dataRole);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableContaminantDetection(UsbPort port, boolean enable) {
        try {
            this.mService.enableContaminantDetection(port.getId(), enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableLimitPowerTransfer(UsbPort port, boolean limit, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(port, "enableLimitPowerTransfer:port must not be null. opId:" + operationId);
        try {
            this.mService.enableLimitPowerTransfer(port.getId(), limit, operationId, callback);
        } catch (RemoteException e) {
            Log.e(TAG, "enableLimitPowerTransfer failed. opId:" + operationId, e);
            try {
                callback.onOperationComplete(1);
            } catch (RemoteException r) {
                Log.e(TAG, "enableLimitPowerTransfer failed to call onOperationComplete. opId:" + operationId, r);
            }
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetUsbPort(UsbPort port, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(port, "resetUsbPort: port must not be null. opId:" + operationId);
        try {
            this.mService.resetUsbPort(port.getId(), operationId, callback);
        } catch (RemoteException e) {
            Log.e(TAG, "resetUsbPort: failed. ", e);
            try {
                callback.onOperationComplete(1);
            } catch (RemoteException r) {
                Log.e(TAG, "resetUsbPort: failed to call onOperationComplete. opId:" + operationId, r);
            }
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableUsbData(UsbPort port, boolean enable, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(port, "enableUsbData: port must not be null. opId:" + operationId);
        try {
            return this.mService.enableUsbData(port.getId(), enable, operationId, callback);
        } catch (RemoteException e) {
            Log.e(TAG, "enableUsbData: failed. opId:" + operationId, e);
            try {
                callback.onOperationComplete(1);
            } catch (RemoteException r) {
                Log.e(TAG, "enableUsbData: failed to call onOperationComplete. opId:" + operationId, r);
            }
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableUsbDataWhileDocked(UsbPort port, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(port, "enableUsbDataWhileDocked: port must not be null. opId:" + operationId);
        try {
            this.mService.enableUsbDataWhileDocked(port.getId(), operationId, callback);
        } catch (RemoteException e) {
            Log.e(TAG, "enableUsbDataWhileDocked: failed. opId:" + operationId, e);
            try {
                callback.onOperationComplete(1);
            } catch (RemoteException r) {
                Log.e(TAG, "enableUsbDataWhileDocked: failed to call onOperationComplete. opId:" + operationId, r);
            }
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) {
        try {
            this.mService.setUsbDeviceConnectionHandler(usbDeviceConnectionHandler);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean areSettableFunctions(long functions) {
        if (functions != 0) {
            return ((-33853) & functions) == 0 && (Long.bitCount(functions) == 1 || functions == 1056);
        }
        return true;
    }

    public static String usbFunctionsToString(long functions) {
        StringJoiner joiner = new StringJoiner(",");
        if ((4 & functions) != 0) {
            joiner.add(USB_FUNCTION_MTP);
        }
        if ((16 & functions) != 0) {
            joiner.add(USB_FUNCTION_PTP);
        }
        if ((32 & functions) != 0) {
            joiner.add(USB_FUNCTION_RNDIS);
        }
        if ((8 & functions) != 0) {
            joiner.add("midi");
        }
        if ((2 & functions) != 0) {
            joiner.add("accessory");
        }
        if ((64 & functions) != 0) {
            joiner.add(USB_FUNCTION_AUDIO_SOURCE);
        }
        if ((1024 & functions) != 0) {
            joiner.add(USB_FUNCTION_NCM);
        }
        if ((1 & functions) != 0) {
            joiner.add("adb");
        }
        if ((32768 & functions) != 0) {
            joiner.add(USB_FUNCTION_BYPASS);
        }
        return joiner.toString();
    }

    public static long usbFunctionsFromString(String functions) {
        String[] split;
        if (functions == null || functions.equals("none")) {
            return 0L;
        }
        long ret = 0;
        for (String function : functions.split(",")) {
            Map<String, Long> map = FUNCTION_NAME_TO_CODE;
            if (map.containsKey(function)) {
                ret |= map.get(function).longValue();
            } else if (function.length() > 0) {
                throw new IllegalArgumentException("Invalid usb function " + functions);
            }
        }
        return ret;
    }

    public static int usbSpeedToBandwidth(int speed) {
        switch (speed) {
            case 0:
                return 2;
            case 1:
                return 12;
            case 2:
                return 480;
            case 3:
                return 5120;
            case 4:
                return 10240;
            case 5:
                return 20480;
            case 6:
                return 10240;
            case 7:
                return 20480;
            case 8:
                return 20480;
            case 9:
                return USB_DATA_TRANSFER_RATE_40G;
            default:
                return -1;
        }
    }

    public static String usbGadgetHalVersionToString(int version) {
        if (version == 12) {
            return GADGET_HAL_VERSION_1_2;
        }
        if (version == 11) {
            return GADGET_HAL_VERSION_1_1;
        }
        if (version == 10) {
            return GADGET_HAL_VERSION_1_0;
        }
        return "unknown";
    }
}
