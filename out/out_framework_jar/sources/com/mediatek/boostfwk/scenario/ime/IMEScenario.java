package com.mediatek.boostfwk.scenario.ime;

import android.view.Window;
import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class IMEScenario extends BasicScenario {
    protected int mScenarioAction;
    private Window mWindow;

    public IMEScenario() {
        this.mScenario = 5;
    }

    public IMEScenario setAction(int action) {
        this.mScenarioAction = action;
        return this;
    }

    public IMEScenario setWindow(Window window) {
        this.mWindow = window;
        return this;
    }

    public int getScenarioAction() {
        return this.mScenarioAction;
    }

    public Window getWindowAndClear() {
        Window window = this.mWindow;
        this.mWindow = null;
        return window;
    }
}
