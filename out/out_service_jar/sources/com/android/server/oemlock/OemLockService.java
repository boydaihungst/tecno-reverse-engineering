package com.android.server.oemlock;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.oemlock.V1_0.IOemLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.oemlock.IOemLockService;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserRestrictionsUtils;
/* loaded from: classes2.dex */
public class OemLockService extends SystemService {
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final String TAG = "OemLock";
    private Context mContext;
    private OemLock mOemLock;
    private final IBinder mService;
    private final UserManagerInternal.UserRestrictionsListener mUserRestrictionsListener;

    public static boolean isHalPresent() {
        return VendorLock.getOemLockHalService() != null;
    }

    private static OemLock getOemLock(Context context) {
        IOemLock oemLockHal = VendorLock.getOemLockHalService();
        if (oemLockHal != null) {
            Slog.i(TAG, "Using vendor lock via the HAL");
            return new VendorLock(context, oemLockHal);
        }
        Slog.i(TAG, "Using persistent data block based lock");
        return new PersistentDataBlockLock(context);
    }

    public OemLockService(Context context) {
        this(context, getOemLock(context));
    }

    OemLockService(Context context, OemLock oemLock) {
        super(context);
        UserManagerInternal.UserRestrictionsListener userRestrictionsListener = new UserManagerInternal.UserRestrictionsListener() { // from class: com.android.server.oemlock.OemLockService.1
            @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
            public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
                if (UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_factory_reset")) {
                    boolean unlockAllowedByAdmin = !newRestrictions.getBoolean("no_factory_reset");
                    if (!unlockAllowedByAdmin) {
                        OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(false);
                        OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(false);
                    }
                }
            }
        };
        this.mUserRestrictionsListener = userRestrictionsListener;
        this.mService = new IOemLockService.Stub() { // from class: com.android.server.oemlock.OemLockService.2
            public String getLockName() {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    return OemLockService.this.mOemLock.getLockName();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public void setOemUnlockAllowedByCarrier(boolean allowed, byte[] signature) {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                OemLockService.this.enforceUserIsAdmin();
                long token = Binder.clearCallingIdentity();
                try {
                    OemLockService.this.mOemLock.setOemUnlockAllowedByCarrier(allowed, signature);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isOemUnlockAllowedByCarrier() {
                OemLockService.this.enforceManageCarrierOemUnlockPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    return OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public void setOemUnlockAllowedByUser(boolean allowedByUser) {
                if (ActivityManager.isUserAMonkey()) {
                    return;
                }
                OemLockService.this.enforceManageUserOemUnlockPermission();
                OemLockService.this.enforceUserIsAdmin();
                long token = Binder.clearCallingIdentity();
                try {
                    if (!OemLockService.this.isOemUnlockAllowedByAdmin()) {
                        throw new SecurityException("Admin does not allow OEM unlock");
                    }
                    if (!OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier()) {
                        throw new SecurityException("Carrier does not allow OEM unlock");
                    }
                    OemLockService.this.mOemLock.setOemUnlockAllowedByDevice(allowedByUser);
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(allowedByUser);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isOemUnlockAllowedByUser() {
                OemLockService.this.enforceManageUserOemUnlockPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    return OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isOemUnlockAllowed() {
                OemLockService.this.enforceOemUnlockReadPermission();
                long token = Binder.clearCallingIdentity();
                try {
                    boolean allowed = OemLockService.this.mOemLock.isOemUnlockAllowedByCarrier() && OemLockService.this.mOemLock.isOemUnlockAllowedByDevice();
                    OemLockService.this.setPersistentDataBlockOemUnlockAllowedBit(allowed);
                    return allowed;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean isDeviceOemUnlocked() {
                char c;
                OemLockService.this.enforceOemUnlockReadPermission();
                String locked = SystemProperties.get(OemLockService.FLASH_LOCK_PROP);
                switch (locked.hashCode()) {
                    case 48:
                        if (locked.equals(OemLockService.FLASH_LOCK_UNLOCKED)) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        return true;
                    default:
                        return false;
                }
            }
        };
        this.mContext = context;
        this.mOemLock = oemLock;
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(userRestrictionsListener);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("oem_lock", this.mService);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPersistentDataBlockOemUnlockAllowedBit(boolean allowed) {
        PersistentDataBlockManagerInternal pdbmi = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        if (pdbmi != null && !(this.mOemLock instanceof PersistentDataBlockLock)) {
            Slog.i(TAG, "Update OEM Unlock bit in pst partition to " + allowed);
            pdbmi.forceOemUnlockEnabled(allowed);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isOemUnlockAllowedByAdmin() {
        return !UserManager.get(this.mContext).hasUserRestriction("no_factory_reset", UserHandle.SYSTEM);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceManageCarrierOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CARRIER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by carrier");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceManageUserOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USER_OEM_UNLOCK_STATE", "Can't manage OEM unlock allowed by user");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceOemUnlockReadPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") == -1) {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceUserIsAdmin() {
        int userId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            if (!UserManager.get(this.mContext).isUserAdmin(userId)) {
                throw new SecurityException("Must be an admin user");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
