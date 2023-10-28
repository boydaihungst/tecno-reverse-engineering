package com.mediatek.boostfwk.identify;

import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes.dex */
public class BaseIdentify {
    private static final String TAG = "BaseIdentify";

    public boolean dispatchScenario(BasicScenario scenario) {
        return true;
    }

    public boolean isMainThreadOnly() {
        return true;
    }
}
