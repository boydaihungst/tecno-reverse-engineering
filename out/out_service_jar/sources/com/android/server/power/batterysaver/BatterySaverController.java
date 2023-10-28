package com.android.server.power.batterysaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.BatterySaverPolicyConfig;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.power.batterysaver.BatterySaverPolicy;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
/* loaded from: classes2.dex */
public class BatterySaverController implements BatterySaverPolicy.BatterySaverPolicyListener {
    static final boolean DEBUG = false;
    public static final int REASON_ADAPTIVE_DYNAMIC_POWER_SAVINGS_CHANGED = 11;
    public static final int REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_OFF = 10;
    public static final int REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_ON = 9;
    public static final int REASON_FULL_POWER_SAVINGS_CHANGED = 13;
    public static final int REASON_INTERACTIVE_CHANGED = 5;
    public static final int REASON_MANUAL_OFF = 3;
    public static final int REASON_MANUAL_ON = 2;
    public static final int REASON_PERCENTAGE_AUTOMATIC_OFF = 1;
    public static final int REASON_PERCENTAGE_AUTOMATIC_ON = 0;
    public static final int REASON_PLUGGED_IN = 7;
    public static final int REASON_POLICY_CHANGED = 6;
    public static final int REASON_SETTING_CHANGED = 8;
    public static final int REASON_STICKY_RESTORE = 4;
    public static final int REASON_TIMEOUT = 12;
    static final String TAG = "BatterySaverController";
    private boolean mAdaptiveEnabledRaw;
    private boolean mAdaptivePreviouslyEnabled;
    private final BatterySaverPolicy mBatterySaverPolicy;
    private final BatterySavingStats mBatterySavingStats;
    private final Context mContext;
    private boolean mFullEnabledRaw;
    private boolean mFullPreviouslyEnabled;
    private final MyHandler mHandler;
    private boolean mIsInteractive;
    private boolean mIsPluggedIn;
    private final Object mLock;
    private PowerManager mPowerManager;
    private Optional<String> mPowerSaveModeChangedListenerPackage;
    private final ArrayList<PowerManagerInternal.LowPowerModeListener> mListeners = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.power.batterysaver.BatterySaverController.1
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            boolean z = true;
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1538406691:
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 498807504:
                    if (action.equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 870701415:
                    if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                        c = 3;
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
                case 1:
                    if (!BatterySaverController.this.isPolicyEnabled()) {
                        BatterySaverController.this.updateBatterySavingStats();
                        return;
                    } else {
                        BatterySaverController.this.mHandler.postStateChanged(false, 5);
                        return;
                    }
                case 2:
                    synchronized (BatterySaverController.this.mLock) {
                        BatterySaverController batterySaverController = BatterySaverController.this;
                        if (intent.getIntExtra("plugged", 0) == 0) {
                            z = false;
                        }
                        batterySaverController.mIsPluggedIn = z;
                        break;
                    }
                case 3:
                case 4:
                    break;
                default:
                    return;
            }
            BatterySaverController.this.updateBatterySavingStats();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String reasonToString(int reason) {
        switch (reason) {
            case 0:
                return "Percentage Auto ON";
            case 1:
                return "Percentage Auto OFF";
            case 2:
                return "Manual ON";
            case 3:
                return "Manual OFF";
            case 4:
                return "Sticky restore";
            case 5:
                return "Interactivity changed";
            case 6:
                return "Policy changed";
            case 7:
                return "Plugged in";
            case 8:
                return "Setting changed";
            case 9:
                return "Dynamic Warning Auto ON";
            case 10:
                return "Dynamic Warning Auto OFF";
            case 11:
                return "Adaptive Power Savings changed";
            case 12:
                return "timeout";
            case 13:
                return "Full Power Savings changed";
            default:
                return "Unknown reason: " + reason;
        }
    }

    public BatterySaverController(Object lock, Context context, Looper looper, BatterySaverPolicy policy, BatterySavingStats batterySavingStats) {
        this.mLock = lock;
        this.mContext = context;
        this.mHandler = new MyHandler(looper);
        this.mBatterySaverPolicy = policy;
        policy.addListener(this);
        this.mBatterySavingStats = batterySavingStats;
        PowerManager.invalidatePowerSaveModeCaches();
    }

    public void addListener(PowerManagerInternal.LowPowerModeListener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    public void systemReady() {
        IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
        this.mHandler.postSystemReady();
    }

    private PowerManager getPowerManager() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) Objects.requireNonNull((PowerManager) this.mContext.getSystemService(PowerManager.class));
        }
        return this.mPowerManager;
    }

    @Override // com.android.server.power.batterysaver.BatterySaverPolicy.BatterySaverPolicyListener
    public void onBatterySaverPolicyChanged(BatterySaverPolicy policy) {
        if (!isPolicyEnabled()) {
            return;
        }
        this.mHandler.postStateChanged(true, 6);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class MyHandler extends Handler {
        private static final int ARG_DONT_SEND_BROADCAST = 0;
        private static final int ARG_SEND_BROADCAST = 1;
        private static final int MSG_STATE_CHANGED = 1;
        private static final int MSG_SYSTEM_READY = 2;

        public MyHandler(Looper looper) {
            super(looper);
        }

        void postStateChanged(boolean sendBroadcast, int reason) {
            obtainMessage(1, sendBroadcast ? 1 : 0, reason).sendToTarget();
        }

        public void postSystemReady() {
            obtainMessage(2, 0, 0).sendToTarget();
        }

        @Override // android.os.Handler
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BatterySaverController.this.handleBatterySaverStateChanged(msg.arg1 == 1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    public void enableBatterySaver(boolean enable, int reason) {
        synchronized (this.mLock) {
            if (getFullEnabledLocked() == enable) {
                return;
            }
            setFullEnabledLocked(enable);
            if (updatePolicyLevelLocked()) {
                this.mHandler.postStateChanged(true, reason);
            }
        }
    }

    private boolean updatePolicyLevelLocked() {
        if (getFullEnabledLocked()) {
            return this.mBatterySaverPolicy.setPolicyLevel(2);
        }
        if (getAdaptiveEnabledLocked()) {
            return this.mBatterySaverPolicy.setPolicyLevel(1);
        }
        return this.mBatterySaverPolicy.setPolicyLevel(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BatterySaverPolicyConfig getPolicyLocked(int policyLevel) {
        return this.mBatterySaverPolicy.getPolicyLocked(policyLevel).toConfig();
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = getFullEnabledLocked() || (getAdaptiveEnabledLocked() && this.mBatterySaverPolicy.shouldAdvertiseIsEnabled());
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPolicyEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = getFullEnabledLocked() || getAdaptiveEnabledLocked();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullEnabled() {
        boolean fullEnabledLocked;
        synchronized (this.mLock) {
            fullEnabledLocked = getFullEnabledLocked();
        }
        return fullEnabledLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setFullPolicyLocked(BatterySaverPolicyConfig config, int reason) {
        return setFullPolicyLocked(BatterySaverPolicy.Policy.fromConfig(config), reason);
    }

    boolean setFullPolicyLocked(BatterySaverPolicy.Policy policy, int reason) {
        if (this.mBatterySaverPolicy.setFullPolicyLocked(policy)) {
            this.mHandler.postStateChanged(true, reason);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAdaptiveEnabled() {
        boolean adaptiveEnabledLocked;
        synchronized (this.mLock) {
            adaptiveEnabledLocked = getAdaptiveEnabledLocked();
        }
        return adaptiveEnabledLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setAdaptivePolicyLocked(BatterySaverPolicyConfig config, int reason) {
        return setAdaptivePolicyLocked(BatterySaverPolicy.Policy.fromConfig(config), reason);
    }

    boolean setAdaptivePolicyLocked(BatterySaverPolicy.Policy policy, int reason) {
        if (this.mBatterySaverPolicy.setAdaptivePolicyLocked(policy)) {
            this.mHandler.postStateChanged(true, reason);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resetAdaptivePolicyLocked(int reason) {
        if (this.mBatterySaverPolicy.resetAdaptivePolicyLocked()) {
            this.mHandler.postStateChanged(true, reason);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setAdaptivePolicyEnabledLocked(boolean enabled, int reason) {
        if (getAdaptiveEnabledLocked() == enabled) {
            return false;
        }
        setAdaptiveEnabledLocked(enabled);
        if (updatePolicyLevelLocked()) {
            this.mHandler.postStateChanged(true, reason);
            return true;
        }
        return false;
    }

    public boolean isInteractive() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsInteractive;
        }
        return z;
    }

    public BatterySaverPolicy getBatterySaverPolicy() {
        return this.mBatterySaverPolicy;
    }

    public boolean isLaunchBoostDisabled() {
        return isPolicyEnabled() && this.mBatterySaverPolicy.isLaunchBoostDisabled();
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0022  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0024  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0029  */
    /* JADX WARN: Removed duplicated region for block: B:18:0x002b  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0032  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0034  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x003c A[Catch: all -> 0x00e3, TryCatch #0 {, blocks: (B:4:0x000b, B:6:0x0013, B:11:0x001d, B:15:0x0025, B:19:0x002c, B:23:0x0035, B:25:0x003c, B:27:0x0045, B:28:0x0063), top: B:44:0x000b }] */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0043  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void handleBatterySaverStateChanged(boolean sendBroadcast, int reason) {
        int i;
        boolean z;
        PowerManagerInternal.LowPowerModeListener[] listeners;
        boolean isInteractive = getPowerManager().isInteractive();
        synchronized (this.mLock) {
            if (!getFullEnabledLocked() && !getAdaptiveEnabledLocked()) {
                z = false;
                boolean enabled = z;
                EventLogTags.writeBatterySaverMode(!this.mFullPreviouslyEnabled ? 1 : 0, !this.mAdaptivePreviouslyEnabled ? 1 : 0, !getFullEnabledLocked() ? 1 : 0, getAdaptiveEnabledLocked() ? 1 : 0, isInteractive ? 1 : 0, !enabled ? this.mBatterySaverPolicy.toEventLogString() : "", reason);
                this.mFullPreviouslyEnabled = getFullEnabledLocked();
                this.mAdaptivePreviouslyEnabled = getAdaptiveEnabledLocked();
                listeners = (PowerManagerInternal.LowPowerModeListener[]) this.mListeners.toArray(new PowerManagerInternal.LowPowerModeListener[0]);
                this.mIsInteractive = isInteractive;
            }
            z = true;
            boolean enabled2 = z;
            if (!this.mFullPreviouslyEnabled) {
            }
            if (!this.mAdaptivePreviouslyEnabled) {
            }
            if (!getFullEnabledLocked()) {
            }
            if (!enabled2) {
            }
            EventLogTags.writeBatterySaverMode(!this.mFullPreviouslyEnabled ? 1 : 0, !this.mAdaptivePreviouslyEnabled ? 1 : 0, !getFullEnabledLocked() ? 1 : 0, getAdaptiveEnabledLocked() ? 1 : 0, isInteractive ? 1 : 0, !enabled2 ? this.mBatterySaverPolicy.toEventLogString() : "", reason);
            this.mFullPreviouslyEnabled = getFullEnabledLocked();
            this.mAdaptivePreviouslyEnabled = getAdaptiveEnabledLocked();
            listeners = (PowerManagerInternal.LowPowerModeListener[]) this.mListeners.toArray(new PowerManagerInternal.LowPowerModeListener[0]);
            this.mIsInteractive = isInteractive;
        }
        PowerManagerInternal pmi = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (pmi != null) {
            pmi.setPowerMode(1, isEnabled());
        }
        updateBatterySavingStats();
        if (sendBroadcast) {
            Intent intent = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED");
            intent.addFlags(1073741824);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            if (getPowerSaveModeChangedListenerPackage().isPresent()) {
                this.mContext.sendBroadcastAsUser(new Intent("android.os.action.POWER_SAVE_MODE_CHANGED").setPackage(getPowerSaveModeChangedListenerPackage().get()).addFlags(AudioFormat.EVRCB), UserHandle.ALL);
            }
            Intent intent2 = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED_INTERNAL");
            intent2.addFlags(1073741824);
            this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, "android.permission.DEVICE_POWER");
            for (PowerManagerInternal.LowPowerModeListener listener : listeners) {
                PowerSaveState result = this.mBatterySaverPolicy.getBatterySaverPolicy(listener.getServiceType());
                listener.onLowPowerModeChanged(result);
            }
        }
    }

    private Optional<String> getPowerSaveModeChangedListenerPackage() {
        Optional<String> empty;
        if (this.mPowerSaveModeChangedListenerPackage == null) {
            String configPowerSaveModeChangedListenerPackage = this.mContext.getString(17040017);
            if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).isSystemPackage(configPowerSaveModeChangedListenerPackage)) {
                empty = Optional.of(configPowerSaveModeChangedListenerPackage);
            } else {
                empty = Optional.empty();
            }
            this.mPowerSaveModeChangedListenerPackage = empty;
        }
        return this.mPowerSaveModeChangedListenerPackage;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBatterySavingStats() {
        int dozeMode;
        PowerManager pm = getPowerManager();
        if (pm == null) {
            Slog.wtf(TAG, "PowerManager not initialized");
            return;
        }
        boolean isInteractive = pm.isInteractive();
        int i = 2;
        int i2 = 1;
        if (pm.isDeviceIdleMode()) {
            dozeMode = 2;
        } else {
            dozeMode = pm.isLightDeviceIdleMode() ? 1 : 0;
        }
        synchronized (this.mLock) {
            BatterySavingStats batterySavingStats = this.mBatterySavingStats;
            if (getFullEnabledLocked()) {
                i = 1;
            } else if (!getAdaptiveEnabledLocked()) {
                i = 0;
            }
            int i3 = isInteractive ? 1 : 0;
            if (!this.mIsPluggedIn) {
                i2 = 0;
            }
            batterySavingStats.transitionState(i, i3, dozeMode, i2);
        }
    }

    private void setFullEnabledLocked(boolean value) {
        if (this.mFullEnabledRaw == value) {
            return;
        }
        PowerManager.invalidatePowerSaveModeCaches();
        this.mFullEnabledRaw = value;
    }

    private boolean getFullEnabledLocked() {
        return this.mFullEnabledRaw;
    }

    private void setAdaptiveEnabledLocked(boolean value) {
        if (this.mAdaptiveEnabledRaw == value) {
            return;
        }
        PowerManager.invalidatePowerSaveModeCaches();
        this.mAdaptiveEnabledRaw = value;
    }

    private boolean getAdaptiveEnabledLocked() {
        return this.mAdaptiveEnabledRaw;
    }
}
