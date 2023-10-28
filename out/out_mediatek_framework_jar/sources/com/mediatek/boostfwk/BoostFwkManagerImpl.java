package com.mediatek.boostfwk;

import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.utils.Config;
/* loaded from: classes.dex */
public final class BoostFwkManagerImpl extends BoostFwkManager {
    private static BoostFwkManagerImpl sInstance = null;
    private static BoostModuleDispatcher sBoostDispatcher = BoostModuleDispatcher.getInstance();
    private static Object sLock = new Object();

    public static BoostFwkManagerImpl getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new BoostFwkManagerImpl();
                }
            }
        }
        return sInstance;
    }

    public void perfHint(BasicScenario scenario) {
        if (Config.disableSBE()) {
            return;
        }
        sBoostDispatcher.scenarioActionDispatcher(scenario);
    }

    public void perfHint(BasicScenario... scenarios) {
        if (Config.disableSBE()) {
            return;
        }
        for (BasicScenario basicScenario : scenarios) {
            if (basicScenario != null) {
                sBoostDispatcher.scenarioActionDispatcher(basicScenario);
            }
        }
    }
}
