package com.android.server.tare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.IndentingPrintWriter;
import android.util.Log;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DeviceIdleModifier extends Modifier {
    private static final boolean DEBUG;
    private static final String TAG;
    private final DeviceIdleTracker mDeviceIdleTracker = new DeviceIdleTracker();
    private final InternalResourceService mIrs;
    private final PowerManager mPowerManager;

    static {
        String str = "TARE-" + DeviceIdleModifier.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeviceIdleModifier(InternalResourceService irs) {
        this.mIrs = irs;
        this.mPowerManager = (PowerManager) irs.getContext().getSystemService(PowerManager.class);
    }

    @Override // com.android.server.tare.Modifier
    public void setup() {
        this.mDeviceIdleTracker.startTracking(this.mIrs.getContext());
    }

    @Override // com.android.server.tare.Modifier
    public void tearDown() {
        this.mDeviceIdleTracker.stopTracking(this.mIrs.getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public long getModifiedCostToProduce(long ctp) {
        if (this.mDeviceIdleTracker.mDeviceIdle) {
            return (long) (ctp * 1.2d);
        }
        if (this.mDeviceIdleTracker.mDeviceLightIdle) {
            return (long) (ctp * 1.1d);
        }
        return ctp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void dump(IndentingPrintWriter pw) {
        pw.print("idle=");
        pw.println(this.mDeviceIdleTracker.mDeviceIdle);
        pw.print("lightIdle=");
        pw.println(this.mDeviceIdleTracker.mDeviceLightIdle);
    }

    /* loaded from: classes2.dex */
    private final class DeviceIdleTracker extends BroadcastReceiver {
        private volatile boolean mDeviceIdle;
        private volatile boolean mDeviceLightIdle;
        private boolean mIsSetup = false;

        DeviceIdleTracker() {
        }

        void startTracking(Context context) {
            if (this.mIsSetup) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            filter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
            context.registerReceiver(this, filter);
            this.mDeviceIdle = DeviceIdleModifier.this.mPowerManager.isDeviceIdleMode();
            this.mDeviceLightIdle = DeviceIdleModifier.this.mPowerManager.isLightDeviceIdleMode();
            this.mIsSetup = true;
        }

        void stopTracking(Context context) {
            if (!this.mIsSetup) {
                return;
            }
            context.unregisterReceiver(this);
            this.mIsSetup = false;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                if (this.mDeviceIdle != DeviceIdleModifier.this.mPowerManager.isDeviceIdleMode()) {
                    this.mDeviceIdle = DeviceIdleModifier.this.mPowerManager.isDeviceIdleMode();
                    DeviceIdleModifier.this.mIrs.onDeviceStateChanged();
                }
            } else if ("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED".equals(action) && this.mDeviceLightIdle != DeviceIdleModifier.this.mPowerManager.isLightDeviceIdleMode()) {
                this.mDeviceLightIdle = DeviceIdleModifier.this.mPowerManager.isLightDeviceIdleMode();
                DeviceIdleModifier.this.mIrs.onDeviceStateChanged();
            }
        }
    }
}
