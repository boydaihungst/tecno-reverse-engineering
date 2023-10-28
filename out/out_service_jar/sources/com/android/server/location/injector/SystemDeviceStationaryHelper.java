package com.android.server.location.injector;

import android.os.Binder;
import com.android.internal.util.Preconditions;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemDeviceStationaryHelper extends DeviceStationaryHelper {
    private DeviceIdleInternal mDeviceIdle;

    public void onSystemReady() {
        this.mDeviceIdle = (DeviceIdleInternal) Objects.requireNonNull((DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class));
    }

    @Override // com.android.server.location.injector.DeviceStationaryHelper
    public void addListener(DeviceIdleInternal.StationaryListener listener) {
        Preconditions.checkState(this.mDeviceIdle != null);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mDeviceIdle.registerStationaryListener(listener);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.DeviceStationaryHelper
    public void removeListener(DeviceIdleInternal.StationaryListener listener) {
        Preconditions.checkState(this.mDeviceIdle != null);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mDeviceIdle.unregisterStationaryListener(listener);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
