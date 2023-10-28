package com.android.server.location.injector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.PowerManager;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemDeviceIdleHelper extends DeviceIdleHelper {
    private final Context mContext;
    private PowerManager mPowerManager;
    private BroadcastReceiver mReceiver;
    private boolean mRegistrationRequired;
    private boolean mSystemReady;

    public SystemDeviceIdleHelper(Context context) {
        this.mContext = context;
    }

    public synchronized void onSystemReady() {
        this.mSystemReady = true;
        this.mPowerManager = (PowerManager) Objects.requireNonNull((PowerManager) this.mContext.getSystemService(PowerManager.class));
        onRegistrationStateChanged();
    }

    @Override // com.android.server.location.injector.DeviceIdleHelper
    protected synchronized void registerInternal() {
        this.mRegistrationRequired = true;
        onRegistrationStateChanged();
    }

    @Override // com.android.server.location.injector.DeviceIdleHelper
    protected synchronized void unregisterInternal() {
        this.mRegistrationRequired = false;
        onRegistrationStateChanged();
    }

    private void onRegistrationStateChanged() {
        BroadcastReceiver receiver;
        if (!this.mSystemReady) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            boolean z = this.mRegistrationRequired;
            if (z && this.mReceiver == null) {
                BroadcastReceiver receiver2 = new BroadcastReceiver() { // from class: com.android.server.location.injector.SystemDeviceIdleHelper.1
                    @Override // android.content.BroadcastReceiver
                    public void onReceive(Context context, Intent intent) {
                        SystemDeviceIdleHelper.this.notifyDeviceIdleChanged();
                    }
                };
                this.mContext.registerReceiver(receiver2, new IntentFilter("android.os.action.DEVICE_IDLE_MODE_CHANGED"), null, FgThread.getHandler());
                this.mReceiver = receiver2;
            } else if (!z && (receiver = this.mReceiver) != null) {
                this.mReceiver = null;
                this.mContext.unregisterReceiver(receiver);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.DeviceIdleHelper
    public boolean isDeviceIdle() {
        Preconditions.checkState(this.mPowerManager != null);
        return this.mPowerManager.isDeviceIdleMode();
    }
}
