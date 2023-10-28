package com.android.server.biometrics.sensors;
/* loaded from: classes.dex */
public interface Interruptable {
    void cancel();

    void cancelWithoutStarting(ClientMonitorCallback clientMonitorCallback);
}
