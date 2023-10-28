package com.android.server.biometrics.sensors.face.aidl;

import android.hardware.biometrics.face.ISession;
import com.android.server.biometrics.sensors.face.aidl.Sensor;
/* loaded from: classes.dex */
public class AidlSession {
    private final int mHalInterfaceVersion;
    private final Sensor.HalSessionCallback mHalSessionCallback;
    private final ISession mSession;
    private final int mUserId;

    public AidlSession(int halInterfaceVersion, ISession session, int userId, Sensor.HalSessionCallback halSessionCallback) {
        this.mHalInterfaceVersion = halInterfaceVersion;
        this.mSession = session;
        this.mUserId = userId;
        this.mHalSessionCallback = halSessionCallback;
    }

    public ISession getSession() {
        return this.mSession;
    }

    public int getUserId() {
        return this.mUserId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Sensor.HalSessionCallback getHalSessionCallback() {
        return this.mHalSessionCallback;
    }

    public boolean hasContextMethods() {
        return this.mHalInterfaceVersion >= 2;
    }
}
