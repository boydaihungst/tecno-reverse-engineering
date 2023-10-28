package com.android.server.telecom;

import android.os.Binder;
import android.os.Process;
import com.android.internal.telecom.IDeviceIdleControllerAdapter;
import com.android.internal.telecom.IInternalServiceRetriever;
import com.android.server.DeviceIdleInternal;
/* loaded from: classes2.dex */
public class InternalServiceRepository extends IInternalServiceRetriever.Stub {
    private final DeviceIdleInternal mDeviceIdleController;
    private final IDeviceIdleControllerAdapter.Stub mDeviceIdleControllerAdapter = new IDeviceIdleControllerAdapter.Stub() { // from class: com.android.server.telecom.InternalServiceRepository.1
        public void exemptAppTemporarilyForEvent(String packageName, long duration, int userHandle, String reason) {
            InternalServiceRepository.this.mDeviceIdleController.addPowerSaveTempWhitelistApp(Process.myUid(), packageName, duration, userHandle, true, 0, reason);
        }
    };

    public InternalServiceRepository(DeviceIdleInternal deviceIdleController) {
        this.mDeviceIdleController = deviceIdleController;
    }

    public IDeviceIdleControllerAdapter getDeviceIdleController() {
        ensureSystemProcess();
        return this.mDeviceIdleControllerAdapter;
    }

    private void ensureSystemProcess() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("SYSTEM ONLY API.");
        }
    }
}
