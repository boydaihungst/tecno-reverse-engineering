package com.android.server.powerstats;

import android.content.Context;
import android.os.Message;
/* loaded from: classes2.dex */
public abstract class PowerStatsLogTrigger {
    private static final String TAG = PowerStatsLogTrigger.class.getSimpleName();
    protected Context mContext;
    private PowerStatsLogger mPowerStatsLogger;

    /* JADX INFO: Access modifiers changed from: protected */
    public void logPowerStatsData(int msgType) {
        Message.obtain(this.mPowerStatsLogger, msgType).sendToTarget();
    }

    public PowerStatsLogTrigger(Context context, PowerStatsLogger powerStatsLogger) {
        this.mContext = context;
        this.mPowerStatsLogger = powerStatsLogger;
    }
}
