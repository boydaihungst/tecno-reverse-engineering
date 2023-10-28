package com.android.server.usb;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbSerialReader;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.internal.util.ArrayUtils;
/* loaded from: classes2.dex */
class UsbSerialReader extends IUsbSerialReader.Stub {
    private final Context mContext;
    private Object mDevice;
    private final UsbPermissionManager mPermissionManager;
    private final String mSerialNumber;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbSerialReader(Context context, UsbPermissionManager permissionManager, String serialNumber) {
        this.mContext = context;
        this.mPermissionManager = permissionManager;
        this.mSerialNumber = serialNumber;
    }

    public void setDevice(Object device) {
        this.mDevice = device;
    }

    public String getSerial(String packageName) throws RemoteException {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
            enforcePackageBelongsToUid(uid, packageName);
            UserHandle user = Binder.getCallingUserHandle();
            long token = Binder.clearCallingIdentity();
            try {
                try {
                    PackageInfo pkg = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, user.getIdentifier());
                    int packageTargetSdkVersion = pkg.applicationInfo.targetSdkVersion;
                    if (packageTargetSdkVersion >= 29 && this.mContext.checkPermission("android.permission.MANAGE_USB", pid, uid) == -1) {
                        int userId = UserHandle.getUserId(uid);
                        if (this.mDevice instanceof UsbDevice) {
                            this.mPermissionManager.getPermissionsForUser(userId).checkPermission((UsbDevice) this.mDevice, packageName, pid, uid);
                        } else {
                            this.mPermissionManager.getPermissionsForUser(userId).checkPermission((UsbAccessory) this.mDevice, pid, uid);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RemoteException("package " + packageName + " cannot be found");
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return this.mSerialNumber;
    }

    private void enforcePackageBelongsToUid(int uid, String packageName) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (!ArrayUtils.contains(packages, packageName)) {
            throw new IllegalArgumentException(packageName + " does to belong to the " + uid);
        }
    }
}
