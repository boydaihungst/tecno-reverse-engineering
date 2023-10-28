package com.lucid.propertiesapi;
/* loaded from: classes2.dex */
public class PSEngineConfigParams {
    public int mPSMotionHigh;
    public int mPSMotionNormal;
    public int mPSMotionUltra;
    public boolean mTouchEnabledHigh;
    public boolean mTouchEnabledNormal;
    public boolean mTouchEnabledUltra;
    public int mValHigh;
    public int mValNormal;
    public int mValUltra;

    public PSEngineConfigParams(int powerLevel, boolean touchEnabled, int psMotion) {
        this.mValNormal = powerLevel;
        this.mValHigh = powerLevel;
        this.mValUltra = powerLevel;
        this.mTouchEnabledNormal = touchEnabled;
        this.mTouchEnabledHigh = touchEnabled;
        this.mTouchEnabledUltra = touchEnabled;
        this.mPSMotionNormal = psMotion;
        this.mPSMotionHigh = psMotion;
        this.mPSMotionUltra = psMotion;
    }

    public PSEngineConfigParams(int normal, int high, int ultra, boolean touchEnabledNormal, boolean touchEnabledHigh, boolean touchEnabledUltra, int psMotionNormal, int psMotionHigh, int psMotionUltra) {
        this.mValNormal = normal;
        this.mValHigh = high;
        this.mValUltra = ultra;
        this.mTouchEnabledNormal = touchEnabledNormal;
        this.mTouchEnabledHigh = touchEnabledHigh;
        this.mTouchEnabledUltra = touchEnabledUltra;
        this.mPSMotionNormal = psMotionNormal;
        this.mPSMotionHigh = psMotionHigh;
        this.mPSMotionUltra = psMotionUltra;
    }

    public String PsToString() {
        String content = Integer.toString(this.mValNormal) + "," + Integer.toString(this.mValHigh) + "," + Integer.toString(this.mValUltra);
        return content;
    }

    public String TouchEnableToString() {
        String content = "" + (this.mTouchEnabledNormal ? "1," : "0,");
        return (content + (this.mTouchEnabledHigh ? "1," : "0,")) + (this.mTouchEnabledUltra ? "1" : "0");
    }

    public String TouchParamToString() {
        String content = Integer.toString(this.mPSMotionNormal) + "," + Integer.toString(this.mPSMotionHigh) + "," + Integer.toString(this.mPSMotionUltra);
        return content;
    }

    public boolean Validate() {
        if (validateParam(this.mValNormal) && validateParam(this.mValHigh) && validateParam(this.mValUltra) && validateParam(this.mPSMotionNormal) && validateParam(this.mPSMotionHigh) && validateParam(this.mPSMotionUltra)) {
            return true;
        }
        return false;
    }

    public boolean validateParam(int param) {
        if (param != 0 && param != 2 && param != 3 && param != 4) {
            return false;
        }
        return true;
    }
}
