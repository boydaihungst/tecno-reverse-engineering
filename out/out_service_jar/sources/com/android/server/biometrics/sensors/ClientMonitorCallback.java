package com.android.server.biometrics.sensors;
/* loaded from: classes.dex */
public interface ClientMonitorCallback {
    default void onClientStarted(BaseClientMonitor clientMonitor) {
    }

    default void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
    }
}
