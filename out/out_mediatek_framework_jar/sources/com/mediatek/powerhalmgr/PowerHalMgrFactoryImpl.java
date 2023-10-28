package com.mediatek.powerhalmgr;
/* loaded from: classes.dex */
public class PowerHalMgrFactoryImpl extends PowerHalMgrFactory {
    public PowerHalMgr makePowerHalMgr() {
        return PowerHalMgrImpl.getInstance();
    }
}
