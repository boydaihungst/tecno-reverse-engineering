package com.android.server.usb.hal.port;

import com.android.internal.util.IndentingPrintWriter;
import com.android.server.usb.UsbPortManager;
/* loaded from: classes2.dex */
public final class UsbPortHalInstance {
    public static UsbPortHal getInstance(UsbPortManager portManager, IndentingPrintWriter pw) {
        UsbPortManager.logAndPrint(3, null, "Querying USB HAL version");
        if (UsbPortHidl.isServicePresent(null)) {
            UsbPortManager.logAndPrint(4, null, "USB HAL HIDL present");
            return new UsbPortHidl(portManager, pw);
        } else if (UsbPortAidl.isServicePresent(null)) {
            UsbPortManager.logAndPrint(4, null, "USB HAL AIDL present");
            return new UsbPortAidl(portManager, pw);
        } else {
            return null;
        }
    }
}
