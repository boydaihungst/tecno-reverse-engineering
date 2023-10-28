package com.android.server.powerstats;

import android.content.Context;
import android.os.Handler;
/* loaded from: classes2.dex */
public final class TimerTrigger extends PowerStatsLogTrigger {
    private static final boolean DEBUG = false;
    private static final long LOG_PERIOD_MS_HIGH_FREQUENCY = 120000;
    private static final long LOG_PERIOD_MS_LOW_FREQUENCY = 3600000;
    private static final String TAG = TimerTrigger.class.getSimpleName();
    private final Handler mHandler;
    private Runnable mLogDataHighFrequency;
    private Runnable mLogDataLowFrequency;

    public TimerTrigger(Context context, PowerStatsLogger powerStatsLogger, boolean triggerEnabled) {
        super(context, powerStatsLogger);
        this.mLogDataLowFrequency = new Runnable() { // from class: com.android.server.powerstats.TimerTrigger.1
            @Override // java.lang.Runnable
            public void run() {
                TimerTrigger.this.mHandler.postDelayed(TimerTrigger.this.mLogDataLowFrequency, 3600000L);
                TimerTrigger.this.logPowerStatsData(1);
            }
        };
        this.mLogDataHighFrequency = new Runnable() { // from class: com.android.server.powerstats.TimerTrigger.2
            @Override // java.lang.Runnable
            public void run() {
                TimerTrigger.this.mHandler.postDelayed(TimerTrigger.this.mLogDataHighFrequency, 120000L);
                TimerTrigger.this.logPowerStatsData(2);
            }
        };
        this.mHandler = this.mContext.getMainThreadHandler();
        if (triggerEnabled) {
            this.mLogDataLowFrequency.run();
            this.mLogDataHighFrequency.run();
        }
    }
}
