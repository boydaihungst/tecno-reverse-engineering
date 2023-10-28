package com.android.server.biometrics;
/* loaded from: classes.dex */
public class SensorConfig {
    public final int id;
    final int modality;
    public final int strength;

    public SensorConfig(String config) {
        String[] elems = config.split(":");
        this.id = Integer.parseInt(elems[0]);
        this.modality = Integer.parseInt(elems[1]);
        this.strength = Integer.parseInt(elems[2]);
    }
}
