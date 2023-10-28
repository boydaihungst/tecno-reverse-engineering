package com.android.server.inputmethod;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.compat.IPlatformCompat;
/* loaded from: classes.dex */
final class ImePlatformCompatUtils {
    private final IPlatformCompat mPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));

    public boolean shouldFinishInputWithReportToIme(int imeUid) {
        return isChangeEnabledByUid(156215187L, imeUid);
    }

    public boolean shouldClearShowForcedFlag(int clientUid) {
        return isChangeEnabledByUid(214016041L, clientUid);
    }

    private boolean isChangeEnabledByUid(long changeFlag, int uid) {
        try {
            boolean result = this.mPlatformCompat.isChangeEnabledByUid(changeFlag, uid);
            return result;
        } catch (RemoteException e) {
            return false;
        }
    }
}
