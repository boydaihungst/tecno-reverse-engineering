package com.android.server.tare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ChargingModifier extends Modifier {
    private static final boolean DEBUG;
    private static final String TAG;
    private final ChargingTracker mChargingTracker = new ChargingTracker();
    private final InternalResourceService mIrs;

    static {
        String str = "TARE-" + ChargingModifier.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ChargingModifier(InternalResourceService irs) {
        this.mIrs = irs;
    }

    @Override // com.android.server.tare.Modifier
    public void setup() {
        this.mChargingTracker.startTracking(this.mIrs.getContext());
    }

    @Override // com.android.server.tare.Modifier
    public void tearDown() {
        this.mChargingTracker.stopTracking(this.mIrs.getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public long getModifiedCostToProduce(long ctp) {
        return modifyValue(ctp);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public long getModifiedPrice(long price) {
        return modifyValue(price);
    }

    private long modifyValue(long val) {
        if (this.mChargingTracker.mCharging) {
            return 0L;
        }
        return val;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void dump(IndentingPrintWriter pw) {
        pw.print("charging=");
        pw.println(this.mChargingTracker.mCharging);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ChargingTracker extends BroadcastReceiver {
        private volatile boolean mCharging;
        private boolean mIsSetup;

        private ChargingTracker() {
            this.mIsSetup = false;
        }

        public void startTracking(Context context) {
            if (this.mIsSetup) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.os.action.CHARGING");
            filter.addAction("android.os.action.DISCHARGING");
            context.registerReceiver(this, filter);
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(BatteryManager.class);
            this.mCharging = batteryManager.isCharging();
            this.mIsSetup = true;
        }

        public void stopTracking(Context context) {
            if (!this.mIsSetup) {
                return;
            }
            context.unregisterReceiver(this);
            this.mIsSetup = false;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.CHARGING".equals(action)) {
                if (ChargingModifier.DEBUG) {
                    Slog.d(ChargingModifier.TAG, "Received charging intent, fired @ " + SystemClock.elapsedRealtime());
                }
                if (!this.mCharging) {
                    this.mCharging = true;
                    ChargingModifier.this.mIrs.onDeviceStateChanged();
                }
            } else if ("android.os.action.DISCHARGING".equals(action)) {
                if (ChargingModifier.DEBUG) {
                    Slog.d(ChargingModifier.TAG, "Disconnected from power.");
                }
                if (this.mCharging) {
                    this.mCharging = false;
                    ChargingModifier.this.mIrs.onDeviceStateChanged();
                }
            }
        }
    }
}
