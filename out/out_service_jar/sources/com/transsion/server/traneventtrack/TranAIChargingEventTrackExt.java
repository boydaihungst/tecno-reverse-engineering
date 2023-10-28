package com.transsion.server.traneventtrack;

import android.content.Context;
/* loaded from: classes2.dex */
public class TranAIChargingEventTrackExt {
    private static TranAIChargingEventTrackExt sTranAIChargingEventTrackExt;

    public static TranAIChargingEventTrackExt getInstance() {
        if (sTranAIChargingEventTrackExt == null) {
            sTranAIChargingEventTrackExt = new TranAIChargingEventTrackExt();
        }
        return sTranAIChargingEventTrackExt;
    }

    public void recordWakeUp(int reason, String details) {
    }

    public void recordGoToSleep(int reason) {
    }

    public void init(Context context) {
    }

    public void destroy() {
    }
}
