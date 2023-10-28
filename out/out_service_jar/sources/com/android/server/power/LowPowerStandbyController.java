package com.android.server.power;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.net.NetworkPolicyManagerInternal;
import java.io.PrintWriter;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class LowPowerStandbyController {
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT_ACTIVE_DURING_MAINTENANCE = false;
    private static final int MSG_NOTIFY_ACTIVE_CHANGED = 1;
    private static final int MSG_NOTIFY_ALLOWLIST_CHANGED = 2;
    private static final int MSG_STANDBY_TIMEOUT = 0;
    private static final String TAG = "LowPowerStandbyController";
    private boolean mActiveDuringMaintenance;
    private AlarmManager mAlarmManager;
    private final Clock mClock;
    private final Context mContext;
    private boolean mEnabledByDefaultConfig;
    private boolean mForceActive;
    private final Handler mHandler;
    private boolean mIdleSinceNonInteractive;
    private boolean mIsActive;
    private boolean mIsDeviceIdle;
    private boolean mIsEnabled;
    private boolean mIsInteractive;
    private long mLastInteractiveTimeElapsed;
    private PowerManager mPowerManager;
    private final SettingsObserver mSettingsObserver;
    private int mStandbyTimeoutConfig;
    private boolean mSupportedConfig;
    private final Object mLock = new Object();
    private final AlarmManager.OnAlarmListener mOnStandbyTimeoutExpired = new AlarmManager.OnAlarmListener() { // from class: com.android.server.power.LowPowerStandbyController$$ExternalSyntheticLambda0
        @Override // android.app.AlarmManager.OnAlarmListener
        public final void onAlarm() {
            LowPowerStandbyController.this.onStandbyTimeoutExpired();
        }
    };
    private final LowPowerStandbyControllerInternal mLocalService = new LocalService();
    private final SparseBooleanArray mAllowlistUids = new SparseBooleanArray();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.power.LowPowerStandbyController.1
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
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
                case 870701415:
                    if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                        c = 2;
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
                    LowPowerStandbyController.this.onNonInteractive();
                    return;
                case 1:
                    LowPowerStandbyController.this.onInteractive();
                    return;
                case 2:
                    LowPowerStandbyController.this.onDeviceIdleModeChanged();
                    return;
                default:
                    return;
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Clock {
        long elapsedRealtime();
    }

    public LowPowerStandbyController(Context context, Looper looper, Clock clock) {
        this.mContext = context;
        LowPowerStandbyHandler lowPowerStandbyHandler = new LowPowerStandbyHandler(looper);
        this.mHandler = lowPowerStandbyHandler;
        this.mClock = clock;
        this.mSettingsObserver = new SettingsObserver(lowPowerStandbyHandler);
    }

    public void systemReady() {
        Resources resources = this.mContext.getResources();
        synchronized (this.mLock) {
            boolean z = resources.getBoolean(17891700);
            this.mSupportedConfig = z;
            if (z) {
                this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
                this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
                this.mStandbyTimeoutConfig = resources.getInteger(17694866);
                this.mEnabledByDefaultConfig = resources.getBoolean(17891699);
                this.mIsInteractive = this.mPowerManager.isInteractive();
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power_standby_enabled"), false, this.mSettingsObserver, -1);
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power_standby_active_during_maintenance"), false, this.mSettingsObserver, -1);
                updateSettingsLocked();
                if (this.mIsEnabled) {
                    registerBroadcastReceiver();
                }
                LocalServices.addService(LowPowerStandbyControllerInternal.class, this.mLocalService);
            }
        }
    }

    private void updateSettingsLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mIsEnabled = this.mSupportedConfig && Settings.Global.getInt(resolver, "low_power_standby_enabled", this.mEnabledByDefaultConfig ? 1 : 0) != 0;
        this.mActiveDuringMaintenance = Settings.Global.getInt(resolver, "low_power_standby_active_during_maintenance", 0) != 0;
        updateActiveLocked();
    }

    private void updateActiveLocked() {
        long now = this.mClock.elapsedRealtime();
        boolean newActive = true;
        boolean standbyTimeoutExpired = now - this.mLastInteractiveTimeElapsed >= ((long) this.mStandbyTimeoutConfig);
        boolean maintenanceMode = this.mIdleSinceNonInteractive && !this.mIsDeviceIdle;
        if (!this.mForceActive && (!this.mIsEnabled || this.mIsInteractive || !standbyTimeoutExpired || (maintenanceMode && !this.mActiveDuringMaintenance))) {
            newActive = false;
        }
        if (this.mIsActive != newActive) {
            this.mIsActive = newActive;
            enqueueNotifyActiveChangedLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNonInteractive() {
        long now = this.mClock.elapsedRealtime();
        synchronized (this.mLock) {
            this.mIsInteractive = false;
            this.mIsDeviceIdle = false;
            this.mLastInteractiveTimeElapsed = now;
            if (this.mStandbyTimeoutConfig > 0) {
                scheduleStandbyTimeoutAlarmLocked();
            }
            updateActiveLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInteractive() {
        synchronized (this.mLock) {
            cancelStandbyTimeoutAlarmLocked();
            this.mIsInteractive = true;
            this.mIsDeviceIdle = false;
            this.mIdleSinceNonInteractive = false;
            updateActiveLocked();
        }
    }

    private void scheduleStandbyTimeoutAlarmLocked() {
        long nextAlarmTime = SystemClock.elapsedRealtime() + this.mStandbyTimeoutConfig;
        this.mAlarmManager.setExact(2, nextAlarmTime, "LowPowerStandbyController.StandbyTimeout", this.mOnStandbyTimeoutExpired, this.mHandler);
    }

    private void cancelStandbyTimeoutAlarmLocked() {
        this.mAlarmManager.cancel(this.mOnStandbyTimeoutExpired);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDeviceIdleModeChanged() {
        boolean z;
        synchronized (this.mLock) {
            boolean isDeviceIdleMode = this.mPowerManager.isDeviceIdleMode();
            this.mIsDeviceIdle = isDeviceIdleMode;
            if (!this.mIdleSinceNonInteractive && !isDeviceIdleMode) {
                z = false;
                this.mIdleSinceNonInteractive = z;
                updateActiveLocked();
            }
            z = true;
            this.mIdleSinceNonInteractive = z;
            updateActiveLocked();
        }
    }

    private void onEnabledLocked() {
        if (this.mPowerManager.isInteractive()) {
            onInteractive();
        } else {
            onNonInteractive();
        }
        registerBroadcastReceiver();
    }

    private void onDisabledLocked() {
        cancelStandbyTimeoutAlarmLocked();
        unregisterBroadcastReceiver();
        updateActiveLocked();
    }

    void onSettingsChanged() {
        synchronized (this.mLock) {
            boolean oldEnabled = this.mIsEnabled;
            updateSettingsLocked();
            boolean z = this.mIsEnabled;
            if (z != oldEnabled) {
                if (z) {
                    onEnabledLocked();
                } else {
                    onDisabledLocked();
                }
                notifyEnabledChangedLocked();
            }
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    private void notifyEnabledChangedLocked() {
        Intent intent = new Intent("android.os.action.LOW_POWER_STANDBY_ENABLED_CHANGED");
        intent.addFlags(1342177280);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStandbyTimeoutExpired() {
        synchronized (this.mLock) {
            updateActiveLocked();
        }
    }

    private void enqueueNotifyActiveChangedLocked() {
        long now = this.mClock.elapsedRealtime();
        Message msg = this.mHandler.obtainMessage(1, Boolean.valueOf(this.mIsActive));
        this.mHandler.sendMessageAtTime(msg, now);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyActiveChanged(boolean active) {
        PowerManagerInternal pmi = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        NetworkPolicyManagerInternal npmi = (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        pmi.setLowPowerStandbyActive(active);
        npmi.setLowPowerStandbyActive(active);
    }

    boolean isActive() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsActive;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSupported() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSupportedConfig;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSupportedConfig && this.mIsEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnabled(boolean enabled) {
        synchronized (this.mLock) {
            if (!this.mSupportedConfig) {
                Slog.w(TAG, "Low Power Standby cannot be enabled because it is not supported on this device");
                return;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "low_power_standby_enabled", enabled ? 1 : 0);
            onSettingsChanged();
        }
    }

    public void setActiveDuringMaintenance(boolean activeDuringMaintenance) {
        synchronized (this.mLock) {
            if (!this.mSupportedConfig) {
                Slog.w(TAG, "Low Power Standby settings cannot be changed because it is not supported on this device");
                return;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "low_power_standby_active_during_maintenance", activeDuringMaintenance ? 1 : 0);
            onSettingsChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceActive(boolean active) {
        synchronized (this.mLock) {
            this.mForceActive = active;
            updateActiveLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println();
        ipw.println("Low Power Standby Controller:");
        ipw.increaseIndent();
        synchronized (this.mLock) {
            ipw.print("mIsActive=");
            ipw.println(this.mIsActive);
            ipw.print("mIsEnabled=");
            ipw.println(this.mIsEnabled);
            ipw.print("mSupportedConfig=");
            ipw.println(this.mSupportedConfig);
            ipw.print("mEnabledByDefaultConfig=");
            ipw.println(this.mEnabledByDefaultConfig);
            ipw.print("mStandbyTimeoutConfig=");
            ipw.println(this.mStandbyTimeoutConfig);
            if (this.mIsActive || this.mIsEnabled) {
                ipw.print("mIsInteractive=");
                ipw.println(this.mIsInteractive);
                ipw.print("mLastInteractiveTime=");
                ipw.println(this.mLastInteractiveTimeElapsed);
                ipw.print("mIdleSinceNonInteractive=");
                ipw.println(this.mIdleSinceNonInteractive);
                ipw.print("mIsDeviceIdle=");
                ipw.println(this.mIsDeviceIdle);
            }
            int[] allowlistUids = getAllowlistUidsLocked();
            ipw.print("mAllowlistUids=");
            ipw.println(Arrays.toString(allowlistUids));
        }
        ipw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProto(ProtoOutputStream proto, long tag) {
        synchronized (this.mLock) {
            long token = proto.start(tag);
            proto.write(1133871366145L, this.mIsActive);
            proto.write(1133871366146L, this.mIsEnabled);
            proto.write(1133871366147L, this.mSupportedConfig);
            proto.write(1133871366148L, this.mEnabledByDefaultConfig);
            proto.write(1133871366149L, this.mIsInteractive);
            proto.write(1112396529670L, this.mLastInteractiveTimeElapsed);
            proto.write(1120986464263L, this.mStandbyTimeoutConfig);
            proto.write(1133871366152L, this.mIdleSinceNonInteractive);
            proto.write(1133871366153L, this.mIsDeviceIdle);
            int[] allowlistUids = getAllowlistUidsLocked();
            for (int appId : allowlistUids) {
                proto.write(2220498092042L, appId);
            }
            proto.end(token);
        }
    }

    /* loaded from: classes2.dex */
    private class LowPowerStandbyHandler extends Handler {
        LowPowerStandbyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    LowPowerStandbyController.this.onStandbyTimeoutExpired();
                    return;
                case 1:
                    boolean active = ((Boolean) msg.obj).booleanValue();
                    LowPowerStandbyController.this.notifyActiveChanged(active);
                    return;
                case 2:
                    int[] allowlistUids = (int[]) msg.obj;
                    LowPowerStandbyController.this.notifyAllowlistChanged(allowlistUids);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addToAllowlistInternal(int uid) {
        synchronized (this.mLock) {
            if (this.mSupportedConfig && !this.mAllowlistUids.get(uid)) {
                this.mAllowlistUids.append(uid, true);
                enqueueNotifyAllowlistChangedLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeFromAllowlistInternal(int uid) {
        synchronized (this.mLock) {
            if (this.mSupportedConfig && this.mAllowlistUids.get(uid)) {
                this.mAllowlistUids.delete(uid);
                enqueueNotifyAllowlistChangedLocked();
            }
        }
    }

    private int[] getAllowlistUidsLocked() {
        int[] uids = new int[this.mAllowlistUids.size()];
        for (int i = 0; i < this.mAllowlistUids.size(); i++) {
            uids[i] = this.mAllowlistUids.keyAt(i);
        }
        return uids;
    }

    private void enqueueNotifyAllowlistChangedLocked() {
        long now = this.mClock.elapsedRealtime();
        int[] allowlistUids = getAllowlistUidsLocked();
        Message msg = this.mHandler.obtainMessage(2, allowlistUids);
        this.mHandler.sendMessageAtTime(msg, now);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAllowlistChanged(int[] allowlistUids) {
        PowerManagerInternal pmi = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        NetworkPolicyManagerInternal npmi = (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        pmi.setLowPowerStandbyAllowlist(allowlistUids);
        npmi.setLowPowerStandbyAllowlist(allowlistUids);
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends LowPowerStandbyControllerInternal {
        private LocalService() {
        }

        @Override // com.android.server.power.LowPowerStandbyControllerInternal
        public void addToAllowlist(int uid) {
            LowPowerStandbyController.this.addToAllowlistInternal(uid);
        }

        @Override // com.android.server.power.LowPowerStandbyControllerInternal
        public void removeFromAllowlist(int uid) {
            LowPowerStandbyController.this.removeFromAllowlistInternal(uid);
        }
    }

    /* loaded from: classes2.dex */
    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            LowPowerStandbyController.this.onSettingsChanged();
        }
    }
}
