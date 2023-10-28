package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.PowerManager;
import android.util.Slog;
import com.android.internal.os.CachedDeviceState;
/* loaded from: classes.dex */
public class CachedDeviceStateService extends SystemService {
    private static final String TAG = "CachedDeviceStateService";
    private final BroadcastReceiver mBroadcastReceiver;
    private final CachedDeviceState mDeviceState;

    public CachedDeviceStateService(Context context) {
        super(context);
        this.mDeviceState = new CachedDeviceState();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.CachedDeviceStateService.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        CachedDeviceStateService.this.mDeviceState.setCharging(intent.getIntExtra("plugged", 0) != 0);
                        return;
                    case 1:
                        CachedDeviceStateService.this.mDeviceState.setScreenInteractive(true);
                        return;
                    case 2:
                        CachedDeviceStateService.this.mDeviceState.setScreenInteractive(false);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishLocalService(CachedDeviceState.Readonly.class, this.mDeviceState.getReadonlyClient());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (500 == phase) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.setPriority(1000);
            getContext().registerReceiver(this.mBroadcastReceiver, filter);
            this.mDeviceState.setCharging(queryIsCharging());
            this.mDeviceState.setScreenInteractive(queryScreenInteractive(getContext()));
        }
    }

    private boolean queryIsCharging() {
        BatteryManagerInternal batteryManager = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        if (batteryManager != null) {
            return batteryManager.getPlugType() != 0;
        }
        Slog.wtf(TAG, "BatteryManager null while starting CachedDeviceStateService");
        return true;
    }

    private boolean queryScreenInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        if (powerManager == null) {
            Slog.wtf(TAG, "PowerManager null while starting CachedDeviceStateService");
            return false;
        }
        return powerManager.isInteractive();
    }
}
