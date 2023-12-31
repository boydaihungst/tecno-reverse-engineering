package com.android.server.os;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IDeviceIdentifiersPolicyService;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
/* loaded from: classes2.dex */
public final class DeviceIdentifiersPolicyService extends SystemService {
    public DeviceIdentifiersPolicyService(Context context) {
        super(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("device_identifiers", new DeviceIdentifiersPolicy(getContext()));
    }

    /* loaded from: classes2.dex */
    private static final class DeviceIdentifiersPolicy extends IDeviceIdentifiersPolicyService.Stub {
        private final Context mContext;

        public DeviceIdentifiersPolicy(Context context) {
            this.mContext = context;
        }

        public String getSerial() throws RemoteException {
            return !TelephonyPermissions.checkCallingOrSelfReadDeviceIdentifiers(this.mContext, (String) null, (String) null, "getSerial") ? UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN : SystemProperties.get("ro.serialno", UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
        }

        public String getSerialForPackage(String callingPackage, String callingFeatureId) throws RemoteException {
            if (checkPackageBelongsToCaller(callingPackage)) {
                return !TelephonyPermissions.checkCallingOrSelfReadDeviceIdentifiers(this.mContext, callingPackage, callingFeatureId, "getSerial") ? UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN : SystemProperties.get("ro.serialno", UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            }
            throw new IllegalArgumentException("Invalid callingPackage or callingPackage does not belong to caller's uid:" + Binder.getCallingUid());
        }

        private boolean checkPackageBelongsToCaller(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            try {
                int callingPackageUid = this.mContext.getPackageManager().getPackageUidAsUser(callingPackage, callingUserId);
                return callingPackageUid == callingUid;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }
}
