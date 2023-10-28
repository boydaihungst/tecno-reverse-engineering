package com.android.server.location.injector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.FgThread;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemEmergencyHelper extends EmergencyHelper {
    private static final String LOG_TAG = "SystemEmergencyHelper";
    private final Context mContext;
    boolean mIsInEmergencyCall;
    TelephonyManager mTelephonyManager;
    private final EmergencyCallTelephonyCallback mEmergencyCallTelephonyCallback = new EmergencyCallTelephonyCallback();
    long mEmergencyCallEndRealtimeMs = Long.MIN_VALUE;

    public SystemEmergencyHelper(Context context) {
        this.mContext = context;
    }

    public synchronized void onSystemReady() {
        if (this.mTelephonyManager != null) {
            return;
        }
        TelephonyManager telephonyManager = (TelephonyManager) Objects.requireNonNull((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class));
        this.mTelephonyManager = telephonyManager;
        telephonyManager.registerTelephonyCallback(FgThread.getExecutor(), this.mEmergencyCallTelephonyCallback);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.location.injector.SystemEmergencyHelper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (!"android.intent.action.NEW_OUTGOING_CALL".equals(intent.getAction())) {
                    return;
                }
                synchronized (SystemEmergencyHelper.this) {
                    try {
                        SystemEmergencyHelper systemEmergencyHelper = SystemEmergencyHelper.this;
                        systemEmergencyHelper.mIsInEmergencyCall = systemEmergencyHelper.mTelephonyManager == null ? false : SystemEmergencyHelper.this.mTelephonyManager.isEmergencyNumber(intent.getStringExtra("android.intent.extra.PHONE_NUMBER"));
                    } catch (IllegalStateException e) {
                        SystemEmergencyHelper.this.mIsInEmergencyCall = false;
                        Log.e(SystemEmergencyHelper.LOG_TAG, "telephony service is null");
                    }
                }
            }
        }, new IntentFilter("android.intent.action.NEW_OUTGOING_CALL"));
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x0033, code lost:
        if (r7.mTelephonyManager.isInEmergencySmsMode() != false) goto L24;
     */
    @Override // com.android.server.location.injector.EmergencyHelper
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized boolean isInEmergency(long extensionTimeMs) {
        boolean z = false;
        if (this.mTelephonyManager == null) {
            return false;
        }
        boolean isInExtensionTime = this.mEmergencyCallEndRealtimeMs != Long.MIN_VALUE && SystemClock.elapsedRealtime() - this.mEmergencyCallEndRealtimeMs < extensionTimeMs;
        if (!this.mIsInEmergencyCall && !isInExtensionTime && !this.mTelephonyManager.getEmergencyCallbackMode()) {
        }
        z = true;
        return z;
    }

    /* loaded from: classes.dex */
    private class EmergencyCallTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        EmergencyCallTelephonyCallback() {
        }

        @Override // android.telephony.TelephonyCallback.CallStateListener
        public void onCallStateChanged(int state) {
            if (state == 0) {
                synchronized (SystemEmergencyHelper.this) {
                    if (SystemEmergencyHelper.this.mIsInEmergencyCall) {
                        SystemEmergencyHelper.this.mEmergencyCallEndRealtimeMs = SystemClock.elapsedRealtime();
                        SystemEmergencyHelper.this.mIsInEmergencyCall = false;
                    }
                }
            }
        }
    }
}
