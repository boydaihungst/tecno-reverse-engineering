package com.transsion.hubcore.utils;

import android.text.format.DateFormat;
import java.util.Map;
/* loaded from: classes4.dex */
public class TranFeatureToggle {
    private Map<String, String> mFeatureList;
    private String mFeatureName;
    private boolean mStatus;

    public String getFeatureName() {
        return this.mFeatureName;
    }

    public void setFeatureName(String featureName) {
        this.mFeatureName = featureName;
    }

    public Map<String, String> getFeatureList() {
        return this.mFeatureList;
    }

    public void setFeatureList(Map<String, String> featureList) {
        this.mFeatureList = featureList;
    }

    public boolean getStatus() {
        return this.mStatus;
    }

    public void setStatus(boolean status) {
        this.mStatus = status;
    }

    public String toString() {
        return "TranFeatureToggle{mFeatureName='" + this.mFeatureName + DateFormat.QUOTE + ", mFeatureList=" + this.mFeatureList + ", mStatus=" + this.mStatus + '}';
    }
}
