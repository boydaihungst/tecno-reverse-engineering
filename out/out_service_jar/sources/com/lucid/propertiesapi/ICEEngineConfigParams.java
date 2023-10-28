package com.lucid.propertiesapi;
/* loaded from: classes2.dex */
public class ICEEngineConfigParams {
    public int mValHigh;
    public int mValNormal;
    public int mValUltra;

    public ICEEngineConfigParams(int param) {
        this.mValNormal = param;
        this.mValHigh = param;
        this.mValUltra = param;
    }

    public ICEEngineConfigParams(int normal, int high, int ultra) {
        this.mValNormal = normal;
        this.mValHigh = high;
        this.mValUltra = ultra;
    }

    public String ToString() {
        String content = Integer.toString(this.mValNormal) + "," + Integer.toString(this.mValHigh) + "," + Integer.toString(this.mValUltra);
        return content;
    }

    public boolean Validate() {
        if (validateFactor(this.mValNormal) && validateFactor(this.mValHigh) && validateFactor(this.mValUltra)) {
            return true;
        }
        return false;
    }

    public boolean validateFactor(int factor) {
        if (factor < 0 || factor > 99) {
            return false;
        }
        return true;
    }
}
