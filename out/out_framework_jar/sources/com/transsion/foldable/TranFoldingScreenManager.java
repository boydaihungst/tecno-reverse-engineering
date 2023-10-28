package com.transsion.foldable;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.Logging.Session;
import android.util.Log;
import com.transsion.foldable.ITranFoldingScreen;
/* loaded from: classes4.dex */
public class TranFoldingScreenManager {
    public static final int COMPATIBLE_MODE_16_9 = 1;
    public static final int COMPATIBLE_MODE_4_3 = 2;
    public static final int COMPATIBLE_MODE_FULL_SCREEN = 0;
    public static final int COMPATIBLE_MODE_UNSET = -1;
    private static final String KEY_OS_ACTIVITY_EMBEDDING_PKGS = "key_os_activity_embedding_pkgs";
    private static final boolean OS_FOLDABLE_SCREEN_SUPPORT = SystemProperties.getBoolean("ro.os_foldable_screen_support", false);
    private static final String OS_SCREEN_REALAY_MODE = "os_screen_relay_mode";
    public static final int SCREEN_RELAY_MODE_INTELLIGENT = 0;
    public static final int SCREEN_RELAY_MODE_KEEP = 2;
    public static final int SCREEN_RELAY_MODE_OFF = 3;
    public static final int SCREEN_RELAY_MODE_SWIPE = 1;
    public static final String SERVICE_NAME = "tran_foldable";
    private static final String TAG = "TranFoldingScreenManager";
    private static TranFoldingScreenManager sInstance;
    private ActivityManager mActivityManager;
    private DeviceStateManager mDeviceStateManager;
    private ITranFoldingScreen mService = ITranFoldingScreen.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));

    public static TranFoldingScreenManager getInstance() {
        if (sInstance == null) {
            sInstance = new TranFoldingScreenManager();
        }
        return sInstance;
    }

    private ActivityManager getActivityManager(Context context) {
        if (this.mActivityManager == null) {
            this.mActivityManager = (ActivityManager) context.getSystemService("activity");
        }
        return this.mActivityManager;
    }

    private DeviceStateManager getDeviceStateManager(Context context) {
        if (this.mDeviceStateManager == null) {
            this.mDeviceStateManager = (DeviceStateManager) context.getSystemService(Context.DEVICE_STATE_SERVICE);
        }
        return this.mDeviceStateManager;
    }

    public static boolean isFoldableDevice() {
        return OS_FOLDABLE_SCREEN_SUPPORT;
    }

    public int getCurrentDeviceState(Context context) {
        DeviceStateManager deviceStateManager;
        if (OS_FOLDABLE_SCREEN_SUPPORT && (deviceStateManager = getDeviceStateManager(context)) != null) {
            return deviceStateManager.getCurrentState();
        }
        return -1;
    }

    public void setCompatibleMode(Context context, String packageName, int mode) {
        setCompatibleModeNoStopPackage(context, packageName, mode);
        ITranFoldingScreen iTranFoldingScreen = this.mService;
        if (iTranFoldingScreen != null) {
            try {
                iTranFoldingScreen.removeTaskWhileModeChanged(packageName, UserHandle.getUserId(Process.myUid()), "remove task due to compatible mode changed");
            } catch (RemoteException e) {
                Log.w(TAG, e.toString());
            }
        }
    }

    public int getScreenRelayMode(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), OS_SCREEN_REALAY_MODE, 0, -2);
    }

    public void setScreenRelayMode(Context context, int mode) {
        Settings.System.putInt(context.getContentResolver(), OS_SCREEN_REALAY_MODE, mode);
    }

    public void setCompatibleModeNoStopPackage(Context context, String packageName, int mode) {
        Settings.System.putInt(context.getContentResolver(), getKeyFromPackageName(packageName), mode);
    }

    public int getCompatibleMode(Context context, String packageName) {
        ITranFoldingScreen iTranFoldingScreen;
        int compatibleMode = Settings.System.getIntForUser(context.getContentResolver(), getKeyFromPackageName(packageName), -1, -2);
        if (compatibleMode == -1 && (iTranFoldingScreen = this.mService) != null) {
            try {
                compatibleMode = iTranFoldingScreen.getCompatibleModeDefaultValue(packageName);
            } catch (RemoteException e) {
                Log.w(TAG, e.toString());
            }
        }
        if (compatibleMode != -1) {
            return compatibleMode;
        }
        return 0;
    }

    private String getKeyFromPackageName(String packageName) {
        return "os_compatible_" + packageName.replaceAll("\\.", Session.SESSION_SEPARATION_CHAR_CHILD);
    }

    public boolean isPkgInActivityEmbedding(Context context, String packageName) {
        if (OS_FOLDABLE_SCREEN_SUPPORT) {
            String pkg = packageName + ",";
            String embeddingPkgs = Settings.System.getString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS);
            return embeddingPkgs != null && embeddingPkgs.contains(pkg);
        }
        return false;
    }

    public void addActivityEmbeddingPkg(Context context, String packageName) {
        if (!OS_FOLDABLE_SCREEN_SUPPORT) {
            return;
        }
        String embeddingPkgs = Settings.System.getString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS);
        if (embeddingPkgs == null) {
            embeddingPkgs = "";
        }
        String pkg = packageName + ",";
        if (!embeddingPkgs.contains(pkg)) {
            Settings.System.putString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS, embeddingPkgs + pkg);
        }
    }

    public void removeActivityEmbeddingPkg(Context context, String packageName) {
        if (!OS_FOLDABLE_SCREEN_SUPPORT) {
            return;
        }
        String pkg = packageName + ",";
        String embeddingPkgs = Settings.System.getString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS);
        if (embeddingPkgs != null && embeddingPkgs.contains(pkg)) {
            Settings.System.putString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS, embeddingPkgs.replace(pkg, ""));
        }
    }

    public String getActivityEmbeddingPkgs(Context context) {
        return Settings.System.getString(context.getContentResolver(), KEY_OS_ACTIVITY_EMBEDDING_PKGS);
    }

    public String getKeyOfActivityEmbeddingPkgs() {
        return KEY_OS_ACTIVITY_EMBEDDING_PKGS;
    }
}
