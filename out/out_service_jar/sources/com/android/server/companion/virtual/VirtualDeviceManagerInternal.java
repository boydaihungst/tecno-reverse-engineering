package com.android.server.companion.virtual;

import android.companion.virtual.IVirtualDevice;
/* loaded from: classes.dex */
public abstract class VirtualDeviceManagerInternal {
    public abstract int getBaseVirtualDisplayFlags(IVirtualDevice iVirtualDevice);

    public abstract boolean isAppOwnerOfAnyVirtualDevice(int i);

    public abstract boolean isAppRunningOnAnyVirtualDevice(int i);

    public abstract boolean isDisplayOwnedByAnyVirtualDevice(int i);

    public abstract boolean isValidVirtualDevice(IVirtualDevice iVirtualDevice);

    public abstract void onVirtualDisplayRemoved(IVirtualDevice iVirtualDevice, int i);
}
