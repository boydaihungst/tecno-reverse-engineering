package com.android.server.biometrics;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.PromptInfo;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.pm.PackageManagerService;
import java.util.List;
/* loaded from: classes.dex */
public class Utils {
    private static final String TAG = "BiometricUtils";

    public static boolean isDebugEnabled(Context context, int targetUserId) {
        if (targetUserId == -10000) {
            return false;
        }
        if (!Build.IS_ENG && !Build.IS_USERDEBUG) {
            return false;
        }
        if (targetUserId >= 0 && Settings.Secure.getIntForUser(context.getContentResolver(), "biometric_debug_enabled", 0, targetUserId) == 0) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void combineAuthenticatorBundles(PromptInfo promptInfo) {
        int authenticators;
        boolean deviceCredentialAllowed = promptInfo.isDeviceCredentialAllowed();
        promptInfo.setDeviceCredentialAllowed(false);
        if (promptInfo.getAuthenticators() != 0) {
            authenticators = promptInfo.getAuthenticators();
        } else if (deviceCredentialAllowed) {
            authenticators = 33023;
        } else {
            authenticators = 255;
        }
        promptInfo.setAuthenticators(authenticators);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isCredentialRequested(int authenticators) {
        return (32768 & authenticators) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isCredentialRequested(PromptInfo promptInfo) {
        return isCredentialRequested(promptInfo.getAuthenticators());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getPublicBiometricStrength(int authenticators) {
        return authenticators & 255;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getPublicBiometricStrength(PromptInfo promptInfo) {
        return getPublicBiometricStrength(promptInfo.getAuthenticators());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isBiometricRequested(int authenticators) {
        return getPublicBiometricStrength(authenticators) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isBiometricRequested(PromptInfo promptInfo) {
        return getPublicBiometricStrength(promptInfo) != 0;
    }

    public static boolean isAtLeastStrength(int sensorStrength, int requestedStrength) {
        int sensorStrength2 = sensorStrength & 32767;
        if (((~requestedStrength) & sensorStrength2) != 0) {
            return false;
        }
        for (int i = 1; i <= requestedStrength; i = (i << 1) | 1) {
            if (i == sensorStrength2) {
                return true;
            }
        }
        Slog.e("BiometricService", "Unknown sensorStrength: " + sensorStrength2 + ", requestedStrength: " + requestedStrength);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isValidAuthenticatorConfig(PromptInfo promptInfo) {
        int authenticators = promptInfo.getAuthenticators();
        return isValidAuthenticatorConfig(authenticators);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isValidAuthenticatorConfig(int authenticators) {
        if (authenticators == 0) {
            return true;
        }
        if (((-65536) & authenticators) != 0) {
            Slog.e("BiometricService", "Non-biometric, non-credential bits found. Authenticators: " + authenticators);
            return false;
        }
        int biometricBits = authenticators & 32767;
        if ((biometricBits == 0 && isCredentialRequested(authenticators)) || biometricBits == 15 || biometricBits == 255) {
            return true;
        }
        Slog.e("BiometricService", "Unsupported biometric flags. Authenticators: " + authenticators);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int biometricConstantsToBiometricManager(int biometricConstantsCode) {
        switch (biometricConstantsCode) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 7:
            case 9:
                return 0;
            case 11:
            case 14:
                return 11;
            case 12:
                return 12;
            case 15:
                return 15;
            case 18:
                return 1;
            default:
                Slog.e("BiometricService", "Unhandled result code: " + biometricConstantsCode);
                return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getAuthenticationTypeForResult(int reason) {
        switch (reason) {
            case 1:
            case 4:
                return 2;
            case 7:
                return 1;
            default:
                throw new IllegalArgumentException("Unsupported dismissal reason: " + reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int authenticatorStatusToBiometricConstant(int status) {
        switch (status) {
            case 1:
                return 0;
            case 2:
            case 4:
                return 12;
            case 3:
            case 6:
            case 8:
            default:
                return 1;
            case 5:
                return 15;
            case 7:
                return 11;
            case 9:
                return 14;
            case 10:
                return 7;
            case 11:
                return 9;
            case 12:
                return 18;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isConfirmationSupported(int modality) {
        switch (modality) {
            case 4:
            case 8:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int removeBiometricBits(int authenticators) {
        return authenticators & (-32768);
    }

    public static boolean listContains(int[] haystack, int needle) {
        for (int i : haystack) {
            if (i == needle) {
                return true;
            }
        }
        return false;
    }

    public static void checkPermission(Context context, String permission) {
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    public static boolean isCurrentUserOrProfile(Context context, int userId) {
        int[] enabledProfileIds;
        UserManager um = UserManager.get(context);
        if (um == null) {
            Slog.e(TAG, "Unable to get UserManager");
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (int profileId : um.getEnabledProfileIds(ActivityManager.getCurrentUser())) {
                if (profileId == userId) {
                    Binder.restoreCallingIdentity(token);
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean isStrongBiometric(int sensorId) {
        IBiometricService service = IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
        try {
            return isAtLeastStrength(service.getCurrentStrength(sensorId), 15);
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
            return false;
        }
    }

    public static int getCurrentStrength(int sensorId) {
        IBiometricService service = IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
        try {
            return service.getCurrentStrength(sensorId);
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
            return 0;
        }
    }

    public static boolean isKeyguard(Context context, String clientPackage) {
        boolean hasPermission = hasInternalPermission(context);
        ComponentName keyguardComponent = ComponentName.unflattenFromString(context.getResources().getString(17039991));
        String keyguardPackage = keyguardComponent != null ? keyguardComponent.getPackageName() : null;
        return hasPermission && keyguardPackage != null && keyguardPackage.equals(clientPackage);
    }

    public static boolean isSystem(Context context, String clientPackage) {
        return hasInternalPermission(context) && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(clientPackage);
    }

    public static boolean isSettings(Context context, String clientPackage) {
        return hasInternalPermission(context) && "com.android.settings".equals(clientPackage);
    }

    private static boolean hasInternalPermission(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.USE_BIOMETRIC_INTERNAL") == 0;
    }

    public static String getClientName(BaseClientMonitor client) {
        return client != null ? client.getClass().getSimpleName() : "null";
    }

    private static boolean containsFlag(int haystack, int needle) {
        return (haystack & needle) != 0;
    }

    public static boolean isUserEncryptedOrLockdown(LockPatternUtils lpu, int user) {
        int strongAuth = lpu.getStrongAuthForUser(user);
        boolean isEncrypted = containsFlag(strongAuth, 1);
        boolean isLockDown = containsFlag(strongAuth, 2) || containsFlag(strongAuth, 32);
        Slog.d(TAG, "isEncrypted: " + isEncrypted + " isLockdown: " + isLockDown);
        return isEncrypted || isLockDown;
    }

    public static boolean isForeground(int callingUid, int callingPid) {
        try {
            List<ActivityManager.RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            if (procs == null) {
                Slog.e(TAG, "No running app processes found, defaulting to true");
                return true;
            }
            for (int i = 0; i < procs.size(); i++) {
                ActivityManager.RunningAppProcessInfo proc = procs.get(i);
                if (proc.pid == callingPid && proc.uid == callingUid && proc.importance <= 125) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
            return false;
        }
    }

    public static boolean isLauncher(String clientPackage, String packageName) {
        return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(clientPackage) && packageName != null && (packageName.contains("launcher") || packageName.contains("Launcher"));
    }

    public static int authenticatorStrengthToPropertyStrength(int strength) {
        switch (strength) {
            case 15:
                return 2;
            case 255:
                return 1;
            case 4095:
                return 0;
            default:
                throw new IllegalArgumentException("Unknown strength: " + strength);
        }
    }

    public static int propertyStrengthToAuthenticatorStrength(int strength) {
        switch (strength) {
            case 0:
                return 4095;
            case 1:
                return 255;
            case 2:
                return 15;
            default:
                throw new IllegalArgumentException("Unknown strength: " + strength);
        }
    }

    public static boolean isBackground(String clientPackage) {
        Slog.v(TAG, "Checking if the authenticating is in background, clientPackage:" + clientPackage);
        List<ActivityManager.RunningTaskInfo> tasks = ActivityTaskManager.getInstance().getTasks(Integer.MAX_VALUE);
        if (tasks == null || tasks.isEmpty()) {
            Slog.d(TAG, "No running tasks reported");
            return true;
        }
        for (ActivityManager.RunningTaskInfo taskInfo : tasks) {
            ComponentName topActivity = taskInfo.topActivity;
            if (topActivity != null) {
                String topPackage = topActivity.getPackageName();
                if (!isLauncher(clientPackage, topPackage)) {
                    if (topPackage.contentEquals(clientPackage) && taskInfo.isVisible()) {
                        return false;
                    }
                    Slog.i(TAG, "Running task, top: " + topPackage + ", isVisible: " + taskInfo.isVisible());
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
