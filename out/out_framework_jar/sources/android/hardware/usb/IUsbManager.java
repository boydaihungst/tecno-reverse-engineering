package android.hardware.usb;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.hardware.usb.IUsbOperationInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import java.util.List;
/* loaded from: classes2.dex */
public interface IUsbManager extends IInterface {
    void addAccessoryPackagesToPreferenceDenied(UsbAccessory usbAccessory, String[] strArr, UserHandle userHandle) throws RemoteException;

    void addDevicePackagesToPreferenceDenied(UsbDevice usbDevice, String[] strArr, UserHandle userHandle) throws RemoteException;

    void clearDefaults(String str, int i) throws RemoteException;

    void enableContaminantDetection(String str, boolean z) throws RemoteException;

    void enableLimitPowerTransfer(String str, boolean z, int i, IUsbOperationInternal iUsbOperationInternal) throws RemoteException;

    boolean enableUsbData(String str, boolean z, int i, IUsbOperationInternal iUsbOperationInternal) throws RemoteException;

    void enableUsbDataWhileDocked(String str, int i, IUsbOperationInternal iUsbOperationInternal) throws RemoteException;

    ParcelFileDescriptor getControlFd(long j) throws RemoteException;

    UsbAccessory getCurrentAccessory() throws RemoteException;

    long getCurrentFunctions() throws RemoteException;

    int getCurrentUsbSpeed() throws RemoteException;

    void getDeviceList(Bundle bundle) throws RemoteException;

    int getGadgetHalVersion() throws RemoteException;

    UsbPortStatus getPortStatus(String str) throws RemoteException;

    List<ParcelableUsbPort> getPorts() throws RemoteException;

    long getScreenUnlockedFunctions() throws RemoteException;

    int getUsbHalVersion() throws RemoteException;

    void grantAccessoryPermission(UsbAccessory usbAccessory, int i) throws RemoteException;

    void grantDevicePermission(UsbDevice usbDevice, int i) throws RemoteException;

    boolean hasAccessoryPermission(UsbAccessory usbAccessory) throws RemoteException;

    boolean hasDefaults(String str, int i) throws RemoteException;

    boolean hasDevicePermission(UsbDevice usbDevice, String str) throws RemoteException;

    boolean isFunctionEnabled(String str) throws RemoteException;

    ParcelFileDescriptor openAccessory(UsbAccessory usbAccessory) throws RemoteException;

    ParcelFileDescriptor openDevice(String str, String str2) throws RemoteException;

    void removeAccessoryPackagesFromPreferenceDenied(UsbAccessory usbAccessory, String[] strArr, UserHandle userHandle) throws RemoteException;

    void removeDevicePackagesFromPreferenceDenied(UsbDevice usbDevice, String[] strArr, UserHandle userHandle) throws RemoteException;

    void requestAccessoryPermission(UsbAccessory usbAccessory, String str, PendingIntent pendingIntent) throws RemoteException;

    void requestDevicePermission(UsbDevice usbDevice, String str, PendingIntent pendingIntent) throws RemoteException;

    void resetUsbGadget() throws RemoteException;

    void resetUsbPort(String str, int i, IUsbOperationInternal iUsbOperationInternal) throws RemoteException;

    void setAccessoryPackage(UsbAccessory usbAccessory, String str, int i) throws RemoteException;

    void setAccessoryPersistentPermission(UsbAccessory usbAccessory, int i, UserHandle userHandle, boolean z) throws RemoteException;

    void setCurrentFunction(String str, boolean z) throws RemoteException;

    void setCurrentFunctions(long j) throws RemoteException;

    void setDevicePackage(UsbDevice usbDevice, String str, int i) throws RemoteException;

    void setDevicePersistentPermission(UsbDevice usbDevice, int i, UserHandle userHandle, boolean z) throws RemoteException;

    void setPortRoles(String str, int i, int i2) throws RemoteException;

    void setScreenUnlockedFunctions(long j) throws RemoteException;

    void setUsbDeviceConnectionHandler(ComponentName componentName) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IUsbManager {
        @Override // android.hardware.usb.IUsbManager
        public void getDeviceList(Bundle devices) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public ParcelFileDescriptor openDevice(String deviceName, String packageName) throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public UsbAccessory getCurrentAccessory() throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public ParcelFileDescriptor openAccessory(UsbAccessory accessory) throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public void setDevicePackage(UsbDevice device, String packageName, int userId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void setAccessoryPackage(UsbAccessory accessory, String packageName, int userId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void addDevicePackagesToPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void addAccessoryPackagesToPreferenceDenied(UsbAccessory accessory, String[] packageNames, UserHandle user) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void removeDevicePackagesFromPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void removeAccessoryPackagesFromPreferenceDenied(UsbAccessory device, String[] packageNames, UserHandle user) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void setDevicePersistentPermission(UsbDevice device, int uid, UserHandle user, boolean shouldBeGranted) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void setAccessoryPersistentPermission(UsbAccessory accessory, int uid, UserHandle user, boolean shouldBeGranted) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public boolean hasDevicePermission(UsbDevice device, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.hardware.usb.IUsbManager
        public boolean hasAccessoryPermission(UsbAccessory accessory) throws RemoteException {
            return false;
        }

        @Override // android.hardware.usb.IUsbManager
        public void requestDevicePermission(UsbDevice device, String packageName, PendingIntent pi) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void requestAccessoryPermission(UsbAccessory accessory, String packageName, PendingIntent pi) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void grantDevicePermission(UsbDevice device, int uid) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void grantAccessoryPermission(UsbAccessory accessory, int uid) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public boolean hasDefaults(String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // android.hardware.usb.IUsbManager
        public void clearDefaults(String packageName, int userId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public boolean isFunctionEnabled(String function) throws RemoteException {
            return false;
        }

        @Override // android.hardware.usb.IUsbManager
        public void setCurrentFunctions(long functions) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void setCurrentFunction(String function, boolean usbDataUnlocked) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public long getCurrentFunctions() throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.usb.IUsbManager
        public int getCurrentUsbSpeed() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.usb.IUsbManager
        public int getGadgetHalVersion() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.usb.IUsbManager
        public void setScreenUnlockedFunctions(long functions) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public long getScreenUnlockedFunctions() throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.usb.IUsbManager
        public void resetUsbGadget() throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void resetUsbPort(String portId, int operationId, IUsbOperationInternal callback) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public boolean enableUsbData(String portId, boolean enable, int operationId, IUsbOperationInternal callback) throws RemoteException {
            return false;
        }

        @Override // android.hardware.usb.IUsbManager
        public void enableUsbDataWhileDocked(String portId, int operationId, IUsbOperationInternal callback) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public int getUsbHalVersion() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.usb.IUsbManager
        public ParcelFileDescriptor getControlFd(long function) throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public List<ParcelableUsbPort> getPorts() throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public UsbPortStatus getPortStatus(String portId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.usb.IUsbManager
        public void setPortRoles(String portId, int powerRole, int dataRole) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void enableLimitPowerTransfer(String portId, boolean limit, int operationId, IUsbOperationInternal callback) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void enableContaminantDetection(String portId, boolean enable) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbManager
        public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IUsbManager {
        public static final String DESCRIPTOR = "android.hardware.usb.IUsbManager";
        static final int TRANSACTION_addAccessoryPackagesToPreferenceDenied = 8;
        static final int TRANSACTION_addDevicePackagesToPreferenceDenied = 7;
        static final int TRANSACTION_clearDefaults = 20;
        static final int TRANSACTION_enableContaminantDetection = 39;
        static final int TRANSACTION_enableLimitPowerTransfer = 38;
        static final int TRANSACTION_enableUsbData = 31;
        static final int TRANSACTION_enableUsbDataWhileDocked = 32;
        static final int TRANSACTION_getControlFd = 34;
        static final int TRANSACTION_getCurrentAccessory = 3;
        static final int TRANSACTION_getCurrentFunctions = 24;
        static final int TRANSACTION_getCurrentUsbSpeed = 25;
        static final int TRANSACTION_getDeviceList = 1;
        static final int TRANSACTION_getGadgetHalVersion = 26;
        static final int TRANSACTION_getPortStatus = 36;
        static final int TRANSACTION_getPorts = 35;
        static final int TRANSACTION_getScreenUnlockedFunctions = 28;
        static final int TRANSACTION_getUsbHalVersion = 33;
        static final int TRANSACTION_grantAccessoryPermission = 18;
        static final int TRANSACTION_grantDevicePermission = 17;
        static final int TRANSACTION_hasAccessoryPermission = 14;
        static final int TRANSACTION_hasDefaults = 19;
        static final int TRANSACTION_hasDevicePermission = 13;
        static final int TRANSACTION_isFunctionEnabled = 21;
        static final int TRANSACTION_openAccessory = 4;
        static final int TRANSACTION_openDevice = 2;
        static final int TRANSACTION_removeAccessoryPackagesFromPreferenceDenied = 10;
        static final int TRANSACTION_removeDevicePackagesFromPreferenceDenied = 9;
        static final int TRANSACTION_requestAccessoryPermission = 16;
        static final int TRANSACTION_requestDevicePermission = 15;
        static final int TRANSACTION_resetUsbGadget = 29;
        static final int TRANSACTION_resetUsbPort = 30;
        static final int TRANSACTION_setAccessoryPackage = 6;
        static final int TRANSACTION_setAccessoryPersistentPermission = 12;
        static final int TRANSACTION_setCurrentFunction = 23;
        static final int TRANSACTION_setCurrentFunctions = 22;
        static final int TRANSACTION_setDevicePackage = 5;
        static final int TRANSACTION_setDevicePersistentPermission = 11;
        static final int TRANSACTION_setPortRoles = 37;
        static final int TRANSACTION_setScreenUnlockedFunctions = 27;
        static final int TRANSACTION_setUsbDeviceConnectionHandler = 40;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IUsbManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUsbManager)) {
                return (IUsbManager) iin;
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
                    return "getDeviceList";
                case 2:
                    return "openDevice";
                case 3:
                    return "getCurrentAccessory";
                case 4:
                    return "openAccessory";
                case 5:
                    return "setDevicePackage";
                case 6:
                    return "setAccessoryPackage";
                case 7:
                    return "addDevicePackagesToPreferenceDenied";
                case 8:
                    return "addAccessoryPackagesToPreferenceDenied";
                case 9:
                    return "removeDevicePackagesFromPreferenceDenied";
                case 10:
                    return "removeAccessoryPackagesFromPreferenceDenied";
                case 11:
                    return "setDevicePersistentPermission";
                case 12:
                    return "setAccessoryPersistentPermission";
                case 13:
                    return "hasDevicePermission";
                case 14:
                    return "hasAccessoryPermission";
                case 15:
                    return "requestDevicePermission";
                case 16:
                    return "requestAccessoryPermission";
                case 17:
                    return "grantDevicePermission";
                case 18:
                    return "grantAccessoryPermission";
                case 19:
                    return "hasDefaults";
                case 20:
                    return "clearDefaults";
                case 21:
                    return "isFunctionEnabled";
                case 22:
                    return "setCurrentFunctions";
                case 23:
                    return "setCurrentFunction";
                case 24:
                    return "getCurrentFunctions";
                case 25:
                    return "getCurrentUsbSpeed";
                case 26:
                    return "getGadgetHalVersion";
                case 27:
                    return "setScreenUnlockedFunctions";
                case 28:
                    return "getScreenUnlockedFunctions";
                case 29:
                    return "resetUsbGadget";
                case 30:
                    return "resetUsbPort";
                case 31:
                    return "enableUsbData";
                case 32:
                    return "enableUsbDataWhileDocked";
                case 33:
                    return "getUsbHalVersion";
                case 34:
                    return "getControlFd";
                case 35:
                    return "getPorts";
                case 36:
                    return "getPortStatus";
                case 37:
                    return "setPortRoles";
                case 38:
                    return "enableLimitPowerTransfer";
                case 39:
                    return "enableContaminantDetection";
                case 40:
                    return "setUsbDeviceConnectionHandler";
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
                            Bundle _arg0 = new Bundle();
                            data.enforceNoDataAvail();
                            getDeviceList(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_arg0, 1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            ParcelFileDescriptor _result = openDevice(_arg02, _arg1);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 3:
                            UsbAccessory _result2 = getCurrentAccessory();
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 4:
                            data.enforceNoDataAvail();
                            ParcelFileDescriptor _result3 = openAccessory((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR));
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 5:
                            String _arg12 = data.readString();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            setDevicePackage((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg12, _arg2);
                            reply.writeNoException();
                            break;
                        case 6:
                            String _arg13 = data.readString();
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            setAccessoryPackage((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg13, _arg22);
                            reply.writeNoException();
                            break;
                        case 7:
                            String[] _arg14 = data.createStringArray();
                            UserHandle _arg23 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            addDevicePackagesToPreferenceDenied((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg14, _arg23);
                            reply.writeNoException();
                            break;
                        case 8:
                            String[] _arg15 = data.createStringArray();
                            UserHandle _arg24 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            addAccessoryPackagesToPreferenceDenied((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg15, _arg24);
                            reply.writeNoException();
                            break;
                        case 9:
                            String[] _arg16 = data.createStringArray();
                            UserHandle _arg25 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            removeDevicePackagesFromPreferenceDenied((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg16, _arg25);
                            reply.writeNoException();
                            break;
                        case 10:
                            String[] _arg17 = data.createStringArray();
                            UserHandle _arg26 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            removeAccessoryPackagesFromPreferenceDenied((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg17, _arg26);
                            reply.writeNoException();
                            break;
                        case 11:
                            int _arg18 = data.readInt();
                            UserHandle _arg27 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            boolean _arg3 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDevicePersistentPermission((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg18, _arg27, _arg3);
                            reply.writeNoException();
                            break;
                        case 12:
                            int _arg19 = data.readInt();
                            UserHandle _arg28 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            boolean _arg32 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAccessoryPersistentPermission((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg19, _arg28, _arg32);
                            reply.writeNoException();
                            break;
                        case 13:
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result4 = hasDevicePermission((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg110);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 14:
                            data.enforceNoDataAvail();
                            boolean _result5 = hasAccessoryPermission((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR));
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 15:
                            String _arg111 = data.readString();
                            PendingIntent _arg29 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            data.enforceNoDataAvail();
                            requestDevicePermission((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg111, _arg29);
                            reply.writeNoException();
                            break;
                        case 16:
                            String _arg112 = data.readString();
                            PendingIntent _arg210 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            data.enforceNoDataAvail();
                            requestAccessoryPermission((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg112, _arg210);
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            grantDevicePermission((UsbDevice) data.readTypedObject(UsbDevice.CREATOR), _arg113);
                            reply.writeNoException();
                            break;
                        case 18:
                            int _arg114 = data.readInt();
                            data.enforceNoDataAvail();
                            grantAccessoryPermission((UsbAccessory) data.readTypedObject(UsbAccessory.CREATOR), _arg114);
                            reply.writeNoException();
                            break;
                        case 19:
                            String _arg03 = data.readString();
                            int _arg115 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = hasDefaults(_arg03, _arg115);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 20:
                            String _arg04 = data.readString();
                            int _arg116 = data.readInt();
                            data.enforceNoDataAvail();
                            clearDefaults(_arg04, _arg116);
                            reply.writeNoException();
                            break;
                        case 21:
                            String _arg05 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result7 = isFunctionEnabled(_arg05);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 22:
                            long _arg06 = data.readLong();
                            data.enforceNoDataAvail();
                            setCurrentFunctions(_arg06);
                            reply.writeNoException();
                            break;
                        case 23:
                            String _arg07 = data.readString();
                            boolean _arg117 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCurrentFunction(_arg07, _arg117);
                            reply.writeNoException();
                            break;
                        case 24:
                            long _result8 = getCurrentFunctions();
                            reply.writeNoException();
                            reply.writeLong(_result8);
                            break;
                        case 25:
                            int _result9 = getCurrentUsbSpeed();
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            break;
                        case 26:
                            int _result10 = getGadgetHalVersion();
                            reply.writeNoException();
                            reply.writeInt(_result10);
                            break;
                        case 27:
                            long _arg08 = data.readLong();
                            data.enforceNoDataAvail();
                            setScreenUnlockedFunctions(_arg08);
                            reply.writeNoException();
                            break;
                        case 28:
                            long _result11 = getScreenUnlockedFunctions();
                            reply.writeNoException();
                            reply.writeLong(_result11);
                            break;
                        case 29:
                            resetUsbGadget();
                            reply.writeNoException();
                            break;
                        case 30:
                            String _arg09 = data.readString();
                            int _arg118 = data.readInt();
                            IUsbOperationInternal _arg211 = IUsbOperationInternal.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            resetUsbPort(_arg09, _arg118, _arg211);
                            reply.writeNoException();
                            break;
                        case 31:
                            String _arg010 = data.readString();
                            boolean _arg119 = data.readBoolean();
                            int _arg212 = data.readInt();
                            IUsbOperationInternal _arg33 = IUsbOperationInternal.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result12 = enableUsbData(_arg010, _arg119, _arg212, _arg33);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case 32:
                            String _arg011 = data.readString();
                            int _arg120 = data.readInt();
                            IUsbOperationInternal _arg213 = IUsbOperationInternal.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            enableUsbDataWhileDocked(_arg011, _arg120, _arg213);
                            reply.writeNoException();
                            break;
                        case 33:
                            int _result13 = getUsbHalVersion();
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            break;
                        case 34:
                            long _arg012 = data.readLong();
                            data.enforceNoDataAvail();
                            ParcelFileDescriptor _result14 = getControlFd(_arg012);
                            reply.writeNoException();
                            reply.writeTypedObject(_result14, 1);
                            break;
                        case 35:
                            List<ParcelableUsbPort> _result15 = getPorts();
                            reply.writeNoException();
                            reply.writeTypedList(_result15);
                            break;
                        case 36:
                            String _arg013 = data.readString();
                            data.enforceNoDataAvail();
                            UsbPortStatus _result16 = getPortStatus(_arg013);
                            reply.writeNoException();
                            reply.writeTypedObject(_result16, 1);
                            break;
                        case 37:
                            String _arg014 = data.readString();
                            int _arg121 = data.readInt();
                            int _arg214 = data.readInt();
                            data.enforceNoDataAvail();
                            setPortRoles(_arg014, _arg121, _arg214);
                            reply.writeNoException();
                            break;
                        case 38:
                            String _arg015 = data.readString();
                            boolean _arg122 = data.readBoolean();
                            int _arg215 = data.readInt();
                            IUsbOperationInternal _arg34 = IUsbOperationInternal.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            enableLimitPowerTransfer(_arg015, _arg122, _arg215, _arg34);
                            reply.writeNoException();
                            break;
                        case 39:
                            String _arg016 = data.readString();
                            boolean _arg123 = data.readBoolean();
                            data.enforceNoDataAvail();
                            enableContaminantDetection(_arg016, _arg123);
                            reply.writeNoException();
                            break;
                        case 40:
                            data.enforceNoDataAvail();
                            setUsbDeviceConnectionHandler((ComponentName) data.readTypedObject(ComponentName.CREATOR));
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IUsbManager {
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

            @Override // android.hardware.usb.IUsbManager
            public void getDeviceList(Bundle devices) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        devices.readFromParcel(_reply);
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public ParcelFileDescriptor openDevice(String deviceName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    ParcelFileDescriptor _result = (ParcelFileDescriptor) _reply.readTypedObject(ParcelFileDescriptor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public UsbAccessory getCurrentAccessory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    UsbAccessory _result = (UsbAccessory) _reply.readTypedObject(UsbAccessory.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public ParcelFileDescriptor openAccessory(UsbAccessory accessory) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    ParcelFileDescriptor _result = (ParcelFileDescriptor) _reply.readTypedObject(ParcelFileDescriptor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setDevicePackage(UsbDevice device, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setAccessoryPackage(UsbAccessory accessory, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void addDevicePackagesToPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeStringArray(packageNames);
                    _data.writeTypedObject(user, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void addAccessoryPackagesToPreferenceDenied(UsbAccessory accessory, String[] packageNames, UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    _data.writeStringArray(packageNames);
                    _data.writeTypedObject(user, 0);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void removeDevicePackagesFromPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeStringArray(packageNames);
                    _data.writeTypedObject(user, 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void removeAccessoryPackagesFromPreferenceDenied(UsbAccessory device, String[] packageNames, UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeStringArray(packageNames);
                    _data.writeTypedObject(user, 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setDevicePersistentPermission(UsbDevice device, int uid, UserHandle user, boolean shouldBeGranted) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeInt(uid);
                    _data.writeTypedObject(user, 0);
                    _data.writeBoolean(shouldBeGranted);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setAccessoryPersistentPermission(UsbAccessory accessory, int uid, UserHandle user, boolean shouldBeGranted) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    _data.writeInt(uid);
                    _data.writeTypedObject(user, 0);
                    _data.writeBoolean(shouldBeGranted);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public boolean hasDevicePermission(UsbDevice device, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public boolean hasAccessoryPermission(UsbAccessory accessory) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void requestDevicePermission(UsbDevice device, String packageName, PendingIntent pi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeString(packageName);
                    _data.writeTypedObject(pi, 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void requestAccessoryPermission(UsbAccessory accessory, String packageName, PendingIntent pi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    _data.writeString(packageName);
                    _data.writeTypedObject(pi, 0);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void grantDevicePermission(UsbDevice device, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(device, 0);
                    _data.writeInt(uid);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void grantAccessoryPermission(UsbAccessory accessory, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accessory, 0);
                    _data.writeInt(uid);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public boolean hasDefaults(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void clearDefaults(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public boolean isFunctionEnabled(String function) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(function);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setCurrentFunctions(long functions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(functions);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setCurrentFunction(String function, boolean usbDataUnlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(function);
                    _data.writeBoolean(usbDataUnlocked);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public long getCurrentFunctions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public int getCurrentUsbSpeed() throws RemoteException {
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

            @Override // android.hardware.usb.IUsbManager
            public int getGadgetHalVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setScreenUnlockedFunctions(long functions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(functions);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public long getScreenUnlockedFunctions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void resetUsbGadget() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void resetUsbPort(String portId, int operationId, IUsbOperationInternal callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeInt(operationId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public boolean enableUsbData(String portId, boolean enable, int operationId, IUsbOperationInternal callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeBoolean(enable);
                    _data.writeInt(operationId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void enableUsbDataWhileDocked(String portId, int operationId, IUsbOperationInternal callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeInt(operationId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public int getUsbHalVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public ParcelFileDescriptor getControlFd(long function) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(function);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    ParcelFileDescriptor _result = (ParcelFileDescriptor) _reply.readTypedObject(ParcelFileDescriptor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public List<ParcelableUsbPort> getPorts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    List<ParcelableUsbPort> _result = _reply.createTypedArrayList(ParcelableUsbPort.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public UsbPortStatus getPortStatus(String portId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    UsbPortStatus _result = (UsbPortStatus) _reply.readTypedObject(UsbPortStatus.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setPortRoles(String portId, int powerRole, int dataRole) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeInt(powerRole);
                    _data.writeInt(dataRole);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void enableLimitPowerTransfer(String portId, boolean limit, int operationId, IUsbOperationInternal callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeBoolean(limit);
                    _data.writeInt(operationId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void enableContaminantDetection(String portId, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(portId);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbManager
            public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(usbDeviceConnectionHandler, 0);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 39;
        }
    }
}
