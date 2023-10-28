package com.transsion.hubcore.server.pnp;
/* loaded from: classes2.dex */
public class TranPNPNative {
    public static native void nativeActivitySetProcessGroup(int i, int i2, int i3, String str);

    public static native void nativePnPMgrModeCtl(String str);

    public static native void nativePnPMgrUpdateAppUsage(String str);

    public static native void nativeSetAffinity(int i, int i2);

    public static native void nativeUpdateActivity(String str);
}
