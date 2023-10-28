package com.android.server.usb.hal.port;

import android.hardware.usb.IUsbOperationInternal;
import android.os.RemoteException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public interface UsbPortHal {
    public static final int HAL_DATA_ROLE_DEVICE = 2;
    public static final int HAL_DATA_ROLE_HOST = 1;
    public static final int HAL_MODE_DFP = 1;
    public static final int HAL_MODE_UFP = 2;
    public static final int HAL_POWER_ROLE_SINK = 2;
    public static final int HAL_POWER_ROLE_SOURCE = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface HalUsbDataRole {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface HalUsbPortMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface HalUsbPowerRole {
    }

    void enableContaminantPresenceDetection(String str, boolean z, long j);

    void enableLimitPowerTransfer(String str, boolean z, long j, IUsbOperationInternal iUsbOperationInternal);

    boolean enableUsbData(String str, boolean z, long j, IUsbOperationInternal iUsbOperationInternal);

    void enableUsbDataWhileDocked(String str, long j, IUsbOperationInternal iUsbOperationInternal);

    int getUsbHalVersion() throws RemoteException;

    void queryPortStatus(long j);

    void resetUsbPort(String str, long j, IUsbOperationInternal iUsbOperationInternal);

    void switchDataRole(String str, int i, long j);

    void switchMode(String str, int i, long j);

    void switchPowerRole(String str, int i, long j);

    void systemReady();
}
