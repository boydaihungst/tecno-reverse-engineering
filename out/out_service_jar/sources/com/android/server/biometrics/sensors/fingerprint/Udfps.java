package com.android.server.biometrics.sensors.fingerprint;
/* loaded from: classes.dex */
public interface Udfps {
    boolean isPointerDown();

    void onPointerDown(int i, int i2, float f, float f2);

    void onPointerUp();

    void onUiReady();
}
