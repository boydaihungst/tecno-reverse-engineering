package com.android.server.biometrics.log;

import com.android.server.biometrics.log.Probe;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
/* loaded from: classes.dex */
public class CallbackWithProbe<T extends Probe> implements ClientMonitorCallback {
    private final T mProbe;
    private final boolean mStartWithClient;

    public CallbackWithProbe(T probe, boolean startWithClient) {
        this.mProbe = probe;
        this.mStartWithClient = startWithClient;
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public void onClientStarted(BaseClientMonitor clientMonitor) {
        if (this.mStartWithClient) {
            this.mProbe.enable();
        }
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
        this.mProbe.destroy();
    }

    public T getProbe() {
        return this.mProbe;
    }
}
