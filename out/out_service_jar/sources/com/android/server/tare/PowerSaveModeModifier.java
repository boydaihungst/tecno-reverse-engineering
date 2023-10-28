package com.android.server.tare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PowerSaveModeModifier extends Modifier {
    private static final boolean DEBUG;
    private static final String TAG;
    private final InternalResourceService mIrs;
    private final PowerSaveModeTracker mPowerSaveModeTracker = new PowerSaveModeTracker();

    static {
        String str = "TARE-" + PowerSaveModeModifier.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerSaveModeModifier(InternalResourceService irs) {
        this.mIrs = irs;
    }

    @Override // com.android.server.tare.Modifier
    public void setup() {
        this.mPowerSaveModeTracker.startTracking(this.mIrs.getContext());
    }

    @Override // com.android.server.tare.Modifier
    public void tearDown() {
        this.mPowerSaveModeTracker.stopTracking(this.mIrs.getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public long getModifiedCostToProduce(long ctp) {
        if (this.mPowerSaveModeTracker.mPowerSaveModeEnabled) {
            return (long) (ctp * 1.5d);
        }
        if (this.mPowerSaveModeTracker.mPowerSaveModeEnabled) {
            return (long) (ctp * 1.25d);
        }
        return ctp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void dump(IndentingPrintWriter pw) {
        pw.print("power save=");
        pw.println(this.mPowerSaveModeTracker.mPowerSaveModeEnabled);
    }

    /* loaded from: classes2.dex */
    private final class PowerSaveModeTracker extends BroadcastReceiver {
        private boolean mIsSetup;
        private final PowerManager mPowerManager;
        private volatile boolean mPowerSaveModeEnabled;

        private PowerSaveModeTracker() {
            this.mIsSetup = false;
            this.mPowerManager = (PowerManager) PowerSaveModeModifier.this.mIrs.getContext().getSystemService(PowerManager.class);
        }

        public void startTracking(Context context) {
            if (this.mIsSetup) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            context.registerReceiver(this, filter);
            this.mPowerSaveModeEnabled = this.mPowerManager.isPowerSaveMode();
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
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action)) {
                boolean enabled = this.mPowerManager.isPowerSaveMode();
                if (PowerSaveModeModifier.DEBUG) {
                    Slog.d(PowerSaveModeModifier.TAG, "Power save mode changed to " + enabled + ", fired @ " + SystemClock.elapsedRealtime());
                }
                if (this.mPowerSaveModeEnabled != enabled) {
                    this.mPowerSaveModeEnabled = enabled;
                    PowerSaveModeModifier.this.mIrs.onDeviceStateChanged();
                }
            }
        }
    }
}
