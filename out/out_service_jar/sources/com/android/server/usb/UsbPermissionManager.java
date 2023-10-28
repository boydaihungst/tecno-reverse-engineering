package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UsbPermissionManager {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = UsbPermissionManager.class.getSimpleName();
    private final Context mContext;
    private final SparseArray<UsbUserPermissionManager> mPermissionsByUser = new SparseArray<>();
    final UsbService mUsbService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbPermissionManager(Context context, UsbService usbService) {
        this.mContext = context;
        this.mUsbService = usbService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserPermissionManager getPermissionsForUser(int userId) {
        UsbUserPermissionManager permissions;
        synchronized (this.mPermissionsByUser) {
            permissions = this.mPermissionsByUser.get(userId);
            if (permissions == null) {
                permissions = new UsbUserPermissionManager(this.mContext.createContextAsUser(UserHandle.of(userId), 0), this.mUsbService.getSettingsForUser(userId));
                this.mPermissionsByUser.put(userId, permissions);
            }
        }
        return permissions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserPermissionManager getPermissionsForUser(UserHandle user) {
        return getPermissionsForUser(user.getIdentifier());
    }

    void remove(UserHandle userToRemove) {
        synchronized (this.mPermissionsByUser) {
            this.mPermissionsByUser.remove(userToRemove.getIdentifier());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void usbDeviceRemoved(UsbDevice device) {
        synchronized (this.mPermissionsByUser) {
            for (int i = 0; i < this.mPermissionsByUser.size(); i++) {
                this.mPermissionsByUser.valueAt(i).removeDevicePermissions(device);
            }
        }
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intent.addFlags(16777216);
        intent.putExtra("device", device);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void usbAccessoryRemoved(UsbAccessory accessory) {
        synchronized (this.mPermissionsByUser) {
            for (int i = 0; i < this.mPermissionsByUser.size(); i++) {
                this.mPermissionsByUser.valueAt(i).removeAccessoryPermissions(accessory);
            }
        }
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_DETACHED");
        intent.addFlags(16777216);
        intent.putExtra("accessory", accessory);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        synchronized (this.mPermissionsByUser) {
            List<UserInfo> users = userManager.getUsers();
            int numUsers = users.size();
            for (int i = 0; i < numUsers; i++) {
                getPermissionsForUser(users.get(i).id).dump(dump, "user_permissions", CompanionAppsPermissions.APP_PERMISSIONS);
            }
        }
        dump.end(token);
    }
}
