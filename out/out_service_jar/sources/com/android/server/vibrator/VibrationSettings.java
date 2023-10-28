package com.android.server.vibrator;

import android.app.ActivityManager;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.vibrator.VibrationConfig;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.utils.PriorityDump;
import com.android.server.vibrator.Vibration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VibrationSettings {
    private static final String TAG = "VibrationSettings";
    private static final int VIBRATE_ON_DISABLED_USAGE_ALLOWED = 66;
    private AudioManager mAudioManager;
    private boolean mBatterySaverMode;
    private final Context mContext;
    private SparseIntArray mCurrentVibrationIntensities;
    private final SparseArray<VibrationEffect> mFallbackEffects;
    private final List<OnVibratorSettingsChanged> mListeners;
    private final Object mLock;
    private PowerManagerInternal mPowerManagerInternal;
    private int mRingerMode;
    final SettingsBroadcastReceiver mSettingChangeReceiver;
    final SettingsContentObserver mSettingObserver;
    private final String mSystemUiPackage;
    final UidObserver mUidObserver;
    private boolean mVibrateInputDevices;
    private boolean mVibrateOn;
    private final VibrationConfig mVibrationConfig;
    private static final Set<Integer> BACKGROUND_PROCESS_USAGE_ALLOWLIST = new HashSet(Arrays.asList(33, 17, 49, 65, 50, 34));
    private static final Set<Integer> BATTERY_SAVER_USAGE_ALLOWLIST = new HashSet(Arrays.asList(33, 17, 65, 34, 50));
    private static final Set<Integer> SYSTEM_VIBRATION_SCREEN_OFF_USAGE_ALLOWLIST = new HashSet(Arrays.asList(18, 34, 50));
    private static final Set<Integer> POWER_MANAGER_SLEEP_REASON_ALLOWLIST = new HashSet(Arrays.asList(9, 2));
    private static final IntentFilter USER_SWITCHED_INTENT_FILTER = new IntentFilter("android.intent.action.USER_SWITCHED");
    private static final IntentFilter INTERNAL_RINGER_MODE_CHANGED_INTENT_FILTER = new IntentFilter("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface OnVibratorSettingsChanged {
        void onChange();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VibrationSettings(Context context, Handler handler) {
        this(context, handler, new VibrationConfig(context.getResources()));
    }

    VibrationSettings(Context context, Handler handler, VibrationConfig config) {
        this.mLock = new Object();
        this.mListeners = new ArrayList();
        this.mCurrentVibrationIntensities = new SparseIntArray();
        this.mContext = context;
        this.mVibrationConfig = config;
        this.mSettingObserver = new SettingsContentObserver(handler);
        this.mUidObserver = new UidObserver();
        this.mSettingChangeReceiver = new SettingsBroadcastReceiver();
        this.mSystemUiPackage = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getSystemUiServiceComponent().getPackageName();
        VibrationEffect clickEffect = createEffectFromResource(17236151);
        VibrationEffect doubleClickEffect = createEffectFromResource(17236055);
        VibrationEffect heavyClickEffect = createEffectFromResource(17236086);
        VibrationEffect tickEffect = createEffectFromResource(17236013);
        SparseArray<VibrationEffect> sparseArray = new SparseArray<>();
        this.mFallbackEffects = sparseArray;
        sparseArray.put(0, clickEffect);
        sparseArray.put(1, doubleClickEffect);
        sparseArray.put(2, tickEffect);
        sparseArray.put(5, heavyClickEffect);
        sparseArray.put(21, VibrationEffect.get(2, false));
        update();
    }

    public void onSystemReady() {
        PowerManagerInternal pm = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        AudioManager am = (AudioManager) this.mContext.getSystemService(AudioManager.class);
        int ringerMode = am.getRingerModeInternal();
        synchronized (this.mLock) {
            this.mPowerManagerInternal = pm;
            this.mAudioManager = am;
            this.mRingerMode = ringerMode;
        }
        try {
            ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, (String) null);
        } catch (RemoteException e) {
        }
        pm.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() { // from class: com.android.server.vibrator.VibrationSettings.1
            public int getServiceType() {
                return 2;
            }

            public void onLowPowerModeChanged(PowerSaveState result) {
                boolean shouldNotifyListeners;
                synchronized (VibrationSettings.this.mLock) {
                    shouldNotifyListeners = result.batterySaverEnabled != VibrationSettings.this.mBatterySaverMode;
                    VibrationSettings.this.mBatterySaverMode = result.batterySaverEnabled;
                }
                if (shouldNotifyListeners) {
                    VibrationSettings.this.notifyListeners();
                }
            }
        });
        registerSettingsChangeReceiver(USER_SWITCHED_INTENT_FILTER);
        registerSettingsChangeReceiver(INTERNAL_RINGER_MODE_CHANGED_INTENT_FILTER);
        registerSettingsObserver(Settings.System.getUriFor("vibrate_input_devices"));
        registerSettingsObserver(Settings.System.getUriFor("vibrate_on"));
        registerSettingsObserver(Settings.System.getUriFor("haptic_feedback_enabled"));
        registerSettingsObserver(Settings.System.getUriFor("alarm_vibration_intensity"));
        registerSettingsObserver(Settings.System.getUriFor("haptic_feedback_intensity"));
        registerSettingsObserver(Settings.System.getUriFor("hardware_haptic_feedback_intensity"));
        registerSettingsObserver(Settings.System.getUriFor("media_vibration_intensity"));
        registerSettingsObserver(Settings.System.getUriFor("notification_vibration_intensity"));
        registerSettingsObserver(Settings.System.getUriFor("ring_vibration_intensity"));
        update();
    }

    public void addListener(OnVibratorSettingsChanged listener) {
        synchronized (this.mLock) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            }
        }
    }

    public void removeListener(OnVibratorSettingsChanged listener) {
        synchronized (this.mLock) {
            this.mListeners.remove(listener);
        }
    }

    public int getRampStepDuration() {
        return this.mVibrationConfig.getRampStepDurationMs();
    }

    public int getRampDownDuration() {
        return this.mVibrationConfig.getRampDownDurationMs();
    }

    public int getDefaultIntensity(int usageHint) {
        return this.mVibrationConfig.getDefaultVibrationIntensity(usageHint);
    }

    public int getCurrentIntensity(int usageHint) {
        int i;
        int defaultIntensity = getDefaultIntensity(usageHint);
        synchronized (this.mLock) {
            i = this.mCurrentVibrationIntensities.get(usageHint, defaultIntensity);
        }
        return i;
    }

    public VibrationEffect getFallbackEffect(int effectId) {
        return this.mFallbackEffects.get(effectId);
    }

    public boolean shouldVibrateInputDevices() {
        return this.mVibrateInputDevices;
    }

    public Vibration.Status shouldIgnoreVibration(int uid, VibrationAttributes attrs) {
        int usage = attrs.getUsage();
        synchronized (this.mLock) {
            if (!this.mUidObserver.isUidForeground(uid) && !BACKGROUND_PROCESS_USAGE_ALLOWLIST.contains(Integer.valueOf(usage))) {
                return Vibration.Status.IGNORED_BACKGROUND;
            } else if (this.mBatterySaverMode && !BATTERY_SAVER_USAGE_ALLOWLIST.contains(Integer.valueOf(usage))) {
                return Vibration.Status.IGNORED_FOR_POWER;
            } else {
                if (!attrs.isFlagSet(2)) {
                    if (!this.mVibrateOn && 66 != usage) {
                        return Vibration.Status.IGNORED_FOR_SETTINGS;
                    } else if (getCurrentIntensity(usage) == 0) {
                        return Vibration.Status.IGNORED_FOR_SETTINGS;
                    }
                }
                if (!attrs.isFlagSet(1) && !shouldVibrateForRingerModeLocked(usage)) {
                    return Vibration.Status.IGNORED_FOR_RINGER_MODE;
                }
                return null;
            }
        }
    }

    public boolean shouldCancelVibrationOnScreenOff(int uid, String opPkg, int usage, long vibrationStartUptimeMillis) {
        PowerManagerInternal pm;
        synchronized (this.mLock) {
            pm = this.mPowerManagerInternal;
        }
        if (pm != null) {
            PowerManager.SleepData sleepData = pm.getLastGoToSleep();
            if (sleepData.goToSleepUptimeMillis < vibrationStartUptimeMillis || POWER_MANAGER_SLEEP_REASON_ALLOWLIST.contains(Integer.valueOf(sleepData.goToSleepReason))) {
                Slog.d(TAG, "Ignoring screen off event triggered at uptime " + sleepData.goToSleepUptimeMillis + " for reason " + PowerManager.sleepReasonToString(sleepData.goToSleepReason));
                return false;
            }
        }
        if (SYSTEM_VIBRATION_SCREEN_OFF_USAGE_ALLOWLIST.contains(Integer.valueOf(usage))) {
            return (uid == 1000 || uid == 0 || this.mSystemUiPackage.equals(opPkg)) ? false : true;
        }
        return true;
    }

    private boolean shouldVibrateForRingerModeLocked(int usageHint) {
        return ((usageHint == 33 || usageHint == 49) && this.mRingerMode == 0) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void update() {
        updateSettings();
        updateRingerMode();
        notifyListeners();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSettings() {
        synchronized (this.mLock) {
            boolean z = true;
            this.mVibrateInputDevices = loadSystemSetting("vibrate_input_devices", 0) > 0;
            if (loadSystemSetting("vibrate_on", 1) <= 0) {
                z = false;
            }
            this.mVibrateOn = z;
            int alarmIntensity = toIntensity(loadSystemSetting("alarm_vibration_intensity", -1), getDefaultIntensity(17));
            int defaultHapticFeedbackIntensity = getDefaultIntensity(18);
            int hapticFeedbackIntensity = toIntensity(loadSystemSetting("haptic_feedback_intensity", -1), defaultHapticFeedbackIntensity);
            int positiveHapticFeedbackIntensity = toPositiveIntensity(hapticFeedbackIntensity, defaultHapticFeedbackIntensity);
            int hardwareFeedbackIntensity = toIntensity(loadSystemSetting("hardware_haptic_feedback_intensity", -1), positiveHapticFeedbackIntensity);
            int mediaIntensity = toIntensity(loadSystemSetting("media_vibration_intensity", -1), getDefaultIntensity(19));
            int defaultNotificationIntensity = getDefaultIntensity(49);
            int notificationIntensity = toIntensity(loadSystemSetting("notification_vibration_intensity", -1), defaultNotificationIntensity);
            int positiveNotificationIntensity = toPositiveIntensity(notificationIntensity, defaultNotificationIntensity);
            int ringIntensity = toIntensity(loadSystemSetting("ring_vibration_intensity", -1), getDefaultIntensity(33));
            this.mCurrentVibrationIntensities.clear();
            this.mCurrentVibrationIntensities.put(17, alarmIntensity);
            this.mCurrentVibrationIntensities.put(49, notificationIntensity);
            this.mCurrentVibrationIntensities.put(19, mediaIntensity);
            this.mCurrentVibrationIntensities.put(0, mediaIntensity);
            this.mCurrentVibrationIntensities.put(33, ringIntensity);
            this.mCurrentVibrationIntensities.put(65, positiveNotificationIntensity);
            this.mCurrentVibrationIntensities.put(50, hardwareFeedbackIntensity);
            this.mCurrentVibrationIntensities.put(34, hardwareFeedbackIntensity);
            if (!loadBooleanSetting("haptic_feedback_enabled")) {
                this.mCurrentVibrationIntensities.put(18, 0);
            } else {
                this.mCurrentVibrationIntensities.put(18, hapticFeedbackIntensity);
            }
            this.mCurrentVibrationIntensities.put(66, positiveHapticFeedbackIntensity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRingerMode() {
        int ringerModeInternal;
        synchronized (this.mLock) {
            AudioManager audioManager = this.mAudioManager;
            if (audioManager == null) {
                ringerModeInternal = 0;
            } else {
                ringerModeInternal = audioManager.getRingerModeInternal();
            }
            this.mRingerMode = ringerModeInternal;
        }
    }

    public String toString() {
        String str;
        synchronized (this.mLock) {
            StringBuilder vibrationIntensitiesString = new StringBuilder("{");
            for (int i = 0; i < this.mCurrentVibrationIntensities.size(); i++) {
                int usage = this.mCurrentVibrationIntensities.keyAt(i);
                int intensity = this.mCurrentVibrationIntensities.valueAt(i);
                vibrationIntensitiesString.append(VibrationAttributes.usageToString(usage)).append("=(").append(intensityToString(intensity)).append(",default:").append(intensityToString(getDefaultIntensity(usage))).append("), ");
            }
            vibrationIntensitiesString.append('}');
            str = "VibrationSettings{mVibratorConfig=" + this.mVibrationConfig + ", mVibrateInputDevices=" + this.mVibrateInputDevices + ", mBatterySaverMode=" + this.mBatterySaverMode + ", mVibrateOn=" + this.mVibrateOn + ", mVibrationIntensities=" + ((Object) vibrationIntensitiesString) + ", mProcStatesCache=" + this.mUidObserver.mProcStatesCache + '}';
        }
        return str;
    }

    public void dumpProto(ProtoOutputStream proto) {
        synchronized (this.mLock) {
            proto.write(1133871366168L, this.mVibrateOn);
            proto.write(1133871366150L, this.mBatterySaverMode);
            proto.write(1120986464274L, getCurrentIntensity(17));
            proto.write(1120986464275L, getDefaultIntensity(17));
            proto.write(1120986464278L, getCurrentIntensity(50));
            proto.write(1120986464279L, getDefaultIntensity(50));
            proto.write(1120986464263L, getCurrentIntensity(18));
            proto.write(1120986464264L, getDefaultIntensity(18));
            proto.write(1120986464276L, getCurrentIntensity(19));
            proto.write(1120986464277L, getDefaultIntensity(19));
            proto.write(1120986464265L, getCurrentIntensity(49));
            proto.write(1120986464266L, getDefaultIntensity(49));
            proto.write(1120986464267L, getCurrentIntensity(33));
            proto.write(1120986464268L, getDefaultIntensity(33));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListeners() {
        List<OnVibratorSettingsChanged> currentListeners;
        synchronized (this.mLock) {
            currentListeners = new ArrayList<>(this.mListeners);
        }
        for (OnVibratorSettingsChanged listener : currentListeners) {
            listener.onChange();
        }
    }

    private static String intensityToString(int intensity) {
        switch (intensity) {
            case 0:
                return "OFF";
            case 1:
                return "LOW";
            case 2:
                return "MEDIUM";
            case 3:
                return PriorityDump.PRIORITY_ARG_HIGH;
            default:
                return "UNKNOWN INTENSITY " + intensity;
        }
    }

    private int toPositiveIntensity(int value, int defaultValue) {
        if (value == 0) {
            return defaultValue;
        }
        return toIntensity(value, defaultValue);
    }

    private int toIntensity(int value, int defaultValue) {
        if (value < 0 || value > 3) {
            return defaultValue;
        }
        return value;
    }

    private boolean loadBooleanSetting(String settingKey) {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), settingKey, 0, -2) != 0;
    }

    private int loadSystemSetting(String settingName, int defaultValue) {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), settingName, defaultValue, -2);
    }

    private void registerSettingsObserver(Uri settingUri) {
        this.mContext.getContentResolver().registerContentObserver(settingUri, true, this.mSettingObserver, -1);
    }

    private void registerSettingsChangeReceiver(IntentFilter intentFilter) {
        this.mContext.registerReceiver(this.mSettingChangeReceiver, intentFilter);
    }

    private VibrationEffect createEffectFromResource(int resId) {
        long[] timings = getLongIntArray(this.mContext.getResources(), resId);
        return createEffectFromTimings(timings);
    }

    private static VibrationEffect createEffectFromTimings(long[] timings) {
        if (timings == null || timings.length == 0) {
            return null;
        }
        if (timings.length == 1) {
            return VibrationEffect.createOneShot(timings[0], -1);
        }
        return VibrationEffect.createWaveform(timings, -1);
    }

    private static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SettingsContentObserver extends ContentObserver {
        SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VibrationSettings.this.updateSettings();
            VibrationSettings.this.notifyListeners();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SettingsBroadcastReceiver extends BroadcastReceiver {
        SettingsBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                VibrationSettings.this.update();
            } else if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                VibrationSettings.this.updateRingerMode();
                VibrationSettings.this.notifyListeners();
            }
        }
    }

    /* loaded from: classes2.dex */
    final class UidObserver extends IUidObserver.Stub {
        private final SparseArray<Integer> mProcStatesCache = new SparseArray<>();

        UidObserver() {
        }

        public boolean isUidForeground(int uid) {
            return this.mProcStatesCache.get(uid, 6).intValue() <= 6;
        }

        public void onUidGone(int uid, boolean disabled) {
            this.mProcStatesCache.delete(uid);
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            this.mProcStatesCache.put(uid, Integer.valueOf(procState));
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }
}
