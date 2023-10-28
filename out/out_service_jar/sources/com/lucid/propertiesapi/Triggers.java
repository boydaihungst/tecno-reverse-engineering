package com.lucid.propertiesapi;
/* loaded from: classes2.dex */
public class Triggers {
    public int high;
    public int normal;
    public int ultra;

    public Triggers(int tNormal, int tHigh, int tUltra) {
        this.normal = tNormal;
        this.high = tHigh;
        this.ultra = tUltra;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Triggers)) {
            return false;
        }
        Triggers that = (Triggers) other;
        boolean result = this.normal == that.normal && this.high == that.high && this.ultra == that.ultra;
        return result;
    }
}
