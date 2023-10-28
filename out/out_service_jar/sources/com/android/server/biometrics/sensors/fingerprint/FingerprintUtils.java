package com.android.server.biometrics.sensors.fingerprint;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintUtils;
import java.util.List;
/* loaded from: classes.dex */
public class FingerprintUtils implements BiometricUtils<Fingerprint> {
    private static final String GOOGLE_SETUP_PACKAGE_NAME = "com.google.android.setupwizard";
    private static final String KEY_FLAG_FROM_FINGERPRINT = "key_flag_from_fingerprint";
    private static final String LEGACY_FINGERPRINT_FILE = "settings_fingerprint.xml";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    static final String TAG = "FingerprintUtils";
    private static SparseArray<FingerprintUtils> sInstances;
    private String launcher_class;
    private String launcher_pkg;
    private ActivityManager mActivityManager;
    private Context mContext;
    private final String mFileName;
    private FingerprintManager mFingerprintManager;
    private boolean mMonitorFocusApp = false;
    private final SparseArray<FingerprintUserState> mUserStates = new SparseArray<>();
    public static final boolean OS_FINGERPRINT_QUICKAPP_SUPPORT = SystemProperties.get("ro.os_fingerprint_quickapp_support").equals("1");
    private static final Object sInstanceLock = new Object();

    public static FingerprintUtils getInstance(int sensorId) {
        return getInstance(sensorId, null);
    }

    private static FingerprintUtils getInstance(int sensorId, String fileName) {
        FingerprintUtils utils;
        synchronized (sInstanceLock) {
            if (sInstances == null) {
                sInstances = new SparseArray<>();
            }
            if (sInstances.get(sensorId) == null) {
                if (fileName == null) {
                    fileName = "settings_fingerprint_" + sensorId + ".xml";
                }
                sInstances.put(sensorId, new FingerprintUtils(fileName));
            }
            utils = sInstances.get(sensorId);
        }
        return utils;
    }

    public static FingerprintUtils getLegacyInstance(int sensorId) {
        return getInstance(sensorId, LEGACY_FINGERPRINT_FILE);
    }

    private FingerprintUtils(String fileName) {
        this.mFileName = fileName;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public List<Fingerprint> getBiometricsForUser(Context ctx, int userId) {
        return getStateForUser(ctx, userId).getBiometrics();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void addBiometricForUser(Context context, int userId, Fingerprint fingerprint) {
        getStateForUser(context, userId).addBiometric(fingerprint);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void removeBiometricForUser(Context context, int userId, int fingerId) {
        getStateForUser(context, userId).removeBiometric(fingerId);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void renameBiometricForUser(Context context, int userId, int fingerId, CharSequence name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        getStateForUser(context, userId).renameBiometric(fingerId, name);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public CharSequence getUniqueName(Context context, int userId) {
        return getStateForUser(context, userId).getUniqueName();
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void setInvalidationInProgress(Context context, int userId, boolean inProgress) {
        getStateForUser(context, userId).setInvalidationInProgress(inProgress);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public boolean isInvalidationInProgress(Context context, int userId) {
        return getStateForUser(context, userId).isInvalidationInProgress();
    }

    private FingerprintUserState getStateForUser(Context ctx, int userId) {
        FingerprintUserState state;
        synchronized (this) {
            state = this.mUserStates.get(userId);
            if (state == null) {
                state = new FingerprintUserState(ctx, userId, this.mFileName);
                this.mUserStates.put(userId, state);
            }
        }
        return state;
    }

    public static boolean isKnownErrorCode(int errorCode) {
        switch (errorCode) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    public static boolean isKnownAcquiredCode(int acquiredCode) {
        switch (acquiredCode) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
            case 10:
                return true;
            case 8:
            default:
                return false;
        }
    }

    public BiometricAuthenticator.Identifier getAddBiometricForUser(Context ctx, int userId) {
        return getStateForUser(ctx, userId).getAddBiometric();
    }

    public boolean checkName(Context ctx, int userId, String name) {
        return !getStateForUser(ctx, userId).isUnique(name);
    }

    public String getAppPackagenameForUser(Context ctx, int fingerId, int userId) {
        return getStateForUser(ctx, userId).getAppPackagename(fingerId);
    }

    public boolean hasAppPackagename(Context ctx, int userId) {
        return getStateForUser(ctx, userId).hasAppPackagename();
    }

    public void setAppBiometricsForUser(Context ctx, int fingerId, int userId, CharSequence packagename, int appUserId) {
        getStateForUser(ctx, userId).setAppBiometrics(fingerId, packagename, appUserId);
    }

    public int getAppUserIdForUser(Context ctx, int fingerId, int userId) {
        return getStateForUser(ctx, userId).getAppUserId(fingerId);
    }

    public static void vibrateFingerprintError(Context context) {
        ITranFingerprintUtils.Instance().vibrateFingerprintError(context);
    }

    public static void vibrateFingerprintSuccess(Context context) {
        ITranFingerprintUtils.Instance().vibrateFingerprintSuccess(context);
    }

    public void startAppForFp(Context ctx, int fingerId, int userId) {
        ITranFingerprintUtils.Instance().startAppForFp(ctx, fingerId, userId, this);
    }

    public void notifyAppResumeForFp(Context ctx, int userId, String packagename, String name) {
        ITranFingerprintUtils.Instance().notifyAppResumeForFp(ctx, userId, packagename, name, this);
    }

    public void notifyAppPauseForFp(Context ctx, int userId, String packagename, String name) {
        ITranFingerprintUtils.Instance().notifyAppPauseForFp(ctx, userId, packagename, name);
    }

    public void onRetrieveSettings(Context ctx) {
        ITranFingerprintUtils.Instance().onRetrieveSettings(ctx);
    }

    public void onUpdateFocusedApp(Context ctx, String oldPackageName, ComponentName oldActivityComponent, String newPackageName, ComponentName newActivityComponent) {
        ITranFingerprintUtils.Instance().onUpdateFocusedApp(ctx, oldPackageName, oldActivityComponent, newPackageName, newActivityComponent, this);
    }

    public void getHomeForFp(Context ctx) {
        ITranFingerprintUtils.Instance().getHomeForFp(ctx);
    }

    public void registerUltraPowerContentResolver(Context ctx) {
        ITranFingerprintUtils.Instance().registerUltraPowerContentResolver(ctx);
    }
}
