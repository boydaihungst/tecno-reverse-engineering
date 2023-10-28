package com.mediatek.boostfwk.scenario.scroll;

import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class ViewScenario extends BasicScenario {
    protected String mPkgName;
    protected int mScenarioAction;

    public ViewScenario() {
    }

    public ViewScenario(int scenario, int action, String pkgName) {
        this.mScenario = scenario;
        this.mScenarioAction = action;
        this.mPkgName = pkgName;
    }

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public String getPackageName() {
        return this.mPkgName;
    }
}
