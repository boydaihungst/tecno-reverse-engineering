package com.transsion.server.tranled;

import android.content.Context;
import com.android.server.lights.LightsManager;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class TranLedLightExt {
    private static volatile TranLedLightExt singleton;

    public TranLedLightExt() {
    }

    public TranLedLightExt(LightsManager lightsManager, Context context) {
    }

    public static TranLedLightExt getInstance(LightsManager lightsManager, Context context) {
        if (singleton == null) {
            synchronized (TranLedLightExt.class) {
                if (singleton == null) {
                    singleton = new TranLedLightExt(lightsManager, context);
                }
            }
        }
        return singleton;
    }

    public void registerLedObserver() {
    }

    public void upNotificationList(ArrayList<String> keyList) {
    }

    public boolean isLedWork() {
        return false;
    }

    public boolean isBatteryOnlyWork() {
        return false;
    }

    public void updateBattery(int level, int status) {
    }
}
