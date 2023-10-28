package com.android.server.biometrics.sensors;

import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class ClientMonitorCompositeCallback implements ClientMonitorCallback {
    private final List<ClientMonitorCallback> mCallbacks = new ArrayList();

    public ClientMonitorCompositeCallback(ClientMonitorCallback... callbacks) {
        for (ClientMonitorCallback callback : callbacks) {
            if (callback != null) {
                this.mCallbacks.add(callback);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public final void onClientStarted(BaseClientMonitor clientMonitor) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onClientStarted(clientMonitor);
        }
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public final void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            this.mCallbacks.get(i).onClientFinished(clientMonitor, success);
        }
    }
}
