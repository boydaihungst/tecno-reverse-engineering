package com.mediatek.boostfwk.scenario.refreshrate;

import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class EventScenario extends BasicScenario {
    private boolean mIsMarked = false;
    private boolean mIsVariableRefreshRateEnabled = false;
    private int mScenarioAction;

    public EventScenario() {
        this.mScenario = 7;
    }

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public EventScenario setScenarioAction(int scenarioAction) {
        this.mScenarioAction = scenarioAction;
        return this;
    }

    public boolean getIsMarked() {
        return this.mIsMarked;
    }

    public EventScenario setIsMarked(boolean isMarked) {
        this.mIsMarked = isMarked;
        return this;
    }

    public EventScenario setVariableRefreshRateEnabled(boolean isVariableRefreshRateEnabled) {
        this.mIsVariableRefreshRateEnabled = isVariableRefreshRateEnabled;
        return this;
    }

    public boolean getVariableRefreshRateEnabled() {
        return this.mIsVariableRefreshRateEnabled;
    }
}
