package com.android.server.sensors;

import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public abstract class SensorManagerInternal {

    /* loaded from: classes2.dex */
    public interface ProximityActiveListener {
        void onProximityActive(boolean z);
    }

    public abstract void addProximityActiveListener(Executor executor, ProximityActiveListener proximityActiveListener);

    public abstract void removeProximityActiveListener(ProximityActiveListener proximityActiveListener);
}
