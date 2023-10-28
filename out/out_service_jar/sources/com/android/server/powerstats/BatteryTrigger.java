package com.android.server.powerstats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
/* loaded from: classes2.dex */
public final class BatteryTrigger extends PowerStatsLogTrigger {
    private static final boolean DEBUG = false;
    private static final String TAG = BatteryTrigger.class.getSimpleName();
    private int mBatteryLevel;
    private final BroadcastReceiver mBatteryLevelReceiver;

    public BatteryTrigger(Context context, PowerStatsLogger powerStatsLogger, boolean triggerEnabled) {
        super(context, powerStatsLogger);
        this.mBatteryLevel = 0;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.powerstats.BatteryTrigger.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        int newBatteryLevel = intent.getIntExtra("level", 0);
                        if (newBatteryLevel < BatteryTrigger.this.mBatteryLevel) {
                            BatteryTrigger.this.logPowerStatsData(0);
                        }
                        BatteryTrigger.this.mBatteryLevel = newBatteryLevel;
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBatteryLevelReceiver = broadcastReceiver;
        if (triggerEnabled) {
            IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
            Intent batteryStatus = this.mContext.registerReceiver(broadcastReceiver, filter);
            this.mBatteryLevel = batteryStatus.getIntExtra("level", 0);
        }
    }
}
