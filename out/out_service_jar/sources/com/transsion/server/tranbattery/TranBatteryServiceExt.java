package com.transsion.server.tranbattery;

import android.content.Context;
/* loaded from: classes2.dex */
public class TranBatteryServiceExt {
    private static TranBatteryServiceExt sInstance;

    private TranBatteryServiceExt(Context context) {
    }

    public TranBatteryServiceExt() {
    }

    public static TranBatteryServiceExt getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TranBatteryServiceExt();
        }
        return sInstance;
    }

    public void init() {
    }

    public void otgStateOperation() {
    }
}
