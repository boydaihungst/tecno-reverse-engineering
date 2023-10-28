package com.android.net.module.util;

import android.net.INetdUnsolicitedEventListener;
/* loaded from: classes.dex */
public class BaseNetdUnsolicitedEventListener extends INetdUnsolicitedEventListener.Stub {
    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs, int uid) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onQuotaLimitReached(String alertName, String ifName) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAdded(String ifName) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceRemoved(String ifName) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceChanged(String ifName, boolean up) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceLinkStateChanged(String ifName, boolean up) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onRouteChanged(boolean updated, String route, String gateway, String ifName) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onStrictCleartextDetected(int uid, String hex) {
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public int getInterfaceVersion() {
        return 10;
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public String getInterfaceHash() {
        return "3943383e838f39851675e3640fcdf27b42f8c9fc";
    }
}
