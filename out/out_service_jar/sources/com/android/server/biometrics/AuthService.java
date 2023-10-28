package com.android.server.biometrics;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.IAuthService;
import android.hardware.biometrics.IBiometricEnabledOnKeyguardCallback;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.PromptInfo;
import android.hardware.biometrics.SensorLocationInternal;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceService;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.iris.IIrisService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemService;
import com.lucid.propertiesapi.PowerXtendDynamicTuningAPIImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public class AuthService extends SystemService {
    private static final int DEFAULT_HIDL_DISABLED = 0;
    private static final String SETTING_HIDL_DISABLED = "com.android.server.biometrics.AuthService.hidlDisabled";
    private static final String SYSPROP_API_LEVEL = "ro.board.api_level";
    private static final String SYSPROP_FIRST_API_LEVEL = "ro.board.first_api_level";
    private static final String TAG = "AuthService";
    private IBiometricService mBiometricService;
    final IAuthService.Stub mImpl;
    private final Injector mInjector;

    /* loaded from: classes.dex */
    public static class Injector {
        public IBiometricService getBiometricService() {
            return IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
        }

        public void publishBinderService(AuthService service, IAuthService.Stub impl) {
            service.publishBinderService("auth", impl);
        }

        public String[] getConfiguration(Context context) {
            return context.getResources().getStringArray(17236004);
        }

        public IFingerprintService getFingerprintService() {
            return IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
        }

        public IFaceService getFaceService() {
            return IFaceService.Stub.asInterface(ServiceManager.getService("face"));
        }

        public IIrisService getIrisService() {
            return IIrisService.Stub.asInterface(ServiceManager.getService(PowerXtendDynamicTuningAPIImpl.IRIS_FILES_EXTENTION));
        }

        public AppOpsManager getAppOps(Context context) {
            return (AppOpsManager) context.getSystemService(AppOpsManager.class);
        }

        public boolean isHidlDisabled(Context context) {
            return (Build.IS_ENG || Build.IS_USERDEBUG) && Settings.Secure.getIntForUser(context.getContentResolver(), AuthService.SETTING_HIDL_DISABLED, 0, -2) == 1;
        }
    }

    /* loaded from: classes.dex */
    private final class AuthServiceImpl extends IAuthService.Stub {
        private AuthServiceImpl() {
        }

        public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) throws RemoteException {
            Utils.checkPermission(AuthService.this.getContext(), "android.permission.TEST_BIOMETRIC");
            long identity = Binder.clearCallingIdentity();
            try {
                return AuthService.this.mInjector.getBiometricService().createTestSession(sensorId, callback, opPackageName);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<SensorPropertiesInternal> getSensorProperties(String opPackageName) throws RemoteException {
            Utils.checkPermission(AuthService.this.getContext(), "android.permission.TEST_BIOMETRIC");
            long identity = Binder.clearCallingIdentity();
            try {
                return AuthService.this.mInjector.getBiometricService().getSensorProperties(opPackageName);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public String getUiPackage() {
            Utils.checkPermission(AuthService.this.getContext(), "android.permission.TEST_BIOMETRIC");
            return AuthService.this.getContext().getResources().getString(17039893);
        }

        public long authenticate(IBinder token, long sessionId, int userId, IBiometricServiceReceiver receiver, String opPackageName, PromptInfo promptInfo) throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            if (userId == callingUserId) {
                AuthService.this.checkPermission();
            } else {
                Slog.w(AuthService.TAG, "User " + callingUserId + " is requesting authentication of userid: " + userId);
                AuthService.this.checkInternalPermission();
            }
            if (!AuthService.this.checkAppOps(callingUid, opPackageName, "authenticate()")) {
                authenticateFastFail("Denied by app ops: " + opPackageName, receiver);
                return -1L;
            } else if (token == null || receiver == null || opPackageName == null || promptInfo == null) {
                authenticateFastFail("Unable to authenticate, one or more null arguments", receiver);
                return -1L;
            } else if (!Utils.isForeground(callingUid, callingPid)) {
                authenticateFastFail("Caller is not foreground: " + opPackageName, receiver);
                return -1L;
            } else {
                if (promptInfo.containsTestConfigurations() && AuthService.this.getContext().checkCallingOrSelfPermission("android.permission.TEST_BIOMETRIC") != 0) {
                    AuthService.this.checkInternalPermission();
                }
                if (promptInfo.containsPrivateApiConfigurations()) {
                    AuthService.this.checkInternalPermission();
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    return AuthService.this.mBiometricService.authenticate(token, sessionId, userId, receiver, opPackageName, promptInfo);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private void authenticateFastFail(String message, IBiometricServiceReceiver receiver) {
            Slog.e(AuthService.TAG, "authenticateFastFail: " + message);
            try {
                receiver.onError(0, 5, 0);
            } catch (RemoteException e) {
                Slog.e(AuthService.TAG, "authenticateFastFail failed to notify caller", e);
            }
        }

        public void cancelAuthentication(IBinder token, String opPackageName, long requestId) throws RemoteException {
            AuthService.this.checkPermission();
            if (token == null || opPackageName == null) {
                Slog.e(AuthService.TAG, "Unable to cancel authentication, one or more null arguments");
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                AuthService.this.mBiometricService.cancelAuthentication(token, opPackageName, requestId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int canAuthenticate(String opPackageName, int userId, int authenticators) throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                AuthService.this.checkInternalPermission();
            } else {
                AuthService.this.checkPermission();
            }
            long identity = Binder.clearCallingIdentity();
            try {
                int result = AuthService.this.mBiometricService.canAuthenticate(opPackageName, userId, callingUserId, authenticators);
                Slog.d(AuthService.TAG, "canAuthenticate, userId: " + userId + ", callingUserId: " + callingUserId + ", authenticators: " + authenticators + ", result: " + result);
                return result;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean hasEnrolledBiometrics(int userId, String opPackageName) throws RemoteException {
            AuthService.this.checkInternalPermission();
            long identity = Binder.clearCallingIdentity();
            try {
                return AuthService.this.mBiometricService.hasEnrolledBiometrics(userId, opPackageName);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void registerEnabledOnKeyguardCallback(IBiometricEnabledOnKeyguardCallback callback) throws RemoteException {
            AuthService.this.checkInternalPermission();
            int callingUserId = UserHandle.getCallingUserId();
            long identity = Binder.clearCallingIdentity();
            try {
                AuthService.this.mBiometricService.registerEnabledOnKeyguardCallback(callback, callingUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void invalidateAuthenticatorIds(int userId, int fromSensorId, IInvalidationCallback callback) throws RemoteException {
            AuthService.this.checkInternalPermission();
            long identity = Binder.clearCallingIdentity();
            try {
                AuthService.this.mBiometricService.invalidateAuthenticatorIds(userId, fromSensorId, callback);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public long[] getAuthenticatorIds(int userId) throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                AuthService.this.getContext().enforceCallingOrSelfPermission("android.permission.USE_BIOMETRIC_INTERNAL", "Must have android.permission.USE_BIOMETRIC_INTERNAL permission.");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return AuthService.this.mBiometricService.getAuthenticatorIds(userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void resetLockoutTimeBound(IBinder token, String opPackageName, int fromSensorId, int userId, byte[] hardwareAuthToken) throws RemoteException {
            AuthService.this.checkInternalPermission();
            long identity = Binder.clearCallingIdentity();
            try {
                AuthService.this.mBiometricService.resetLockoutTimeBound(token, opPackageName, fromSensorId, userId, hardwareAuthToken);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public CharSequence getButtonLabel(int userId, String opPackageName, int authenticators) throws RemoteException {
            String result;
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                AuthService.this.checkInternalPermission();
            } else {
                AuthService.this.checkPermission();
            }
            long identity = Binder.clearCallingIdentity();
            try {
                int modality = AuthService.this.mBiometricService.getCurrentModality(opPackageName, userId, callingUserId, authenticators);
                switch (AuthService.getCredentialBackupModality(modality)) {
                    case 0:
                        result = null;
                        break;
                    case 1:
                        result = AuthService.this.getContext().getString(17041453);
                        break;
                    case 2:
                        result = AuthService.this.getContext().getString(17040342);
                        break;
                    case 8:
                        result = AuthService.this.getContext().getString(17040287);
                        break;
                    default:
                        result = AuthService.this.getContext().getString(17039802);
                        break;
                }
                return result;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public CharSequence getPromptMessage(int userId, String opPackageName, int authenticators) throws RemoteException {
            String result;
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                AuthService.this.checkInternalPermission();
            } else {
                AuthService.this.checkPermission();
            }
            long identity = Binder.clearCallingIdentity();
            try {
                int modality = AuthService.this.mBiometricService.getCurrentModality(opPackageName, userId, callingUserId, authenticators);
                boolean isCredentialAllowed = Utils.isCredentialRequested(authenticators);
                switch (AuthService.getCredentialBackupModality(modality)) {
                    case 0:
                        result = null;
                        break;
                    case 1:
                        result = AuthService.this.getContext().getString(17041454);
                        break;
                    case 2:
                        if (isCredentialAllowed) {
                            result = AuthService.this.getContext().getString(17040361);
                            break;
                        } else {
                            result = AuthService.this.getContext().getString(17040344);
                            break;
                        }
                    case 8:
                        if (isCredentialAllowed) {
                            result = AuthService.this.getContext().getString(17040307);
                            break;
                        } else {
                            result = AuthService.this.getContext().getString(17040290);
                            break;
                        }
                    default:
                        if (isCredentialAllowed) {
                            result = AuthService.this.getContext().getString(17039812);
                            break;
                        } else {
                            result = AuthService.this.getContext().getString(17039803);
                            break;
                        }
                }
                return result;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public CharSequence getSettingName(int userId, String opPackageName, int authenticators) throws RemoteException {
            String result;
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                AuthService.this.checkInternalPermission();
            } else {
                AuthService.this.checkPermission();
            }
            long identity = Binder.clearCallingIdentity();
            try {
                int modality = AuthService.this.mBiometricService.getSupportedModalities(authenticators);
                switch (modality) {
                    case 0:
                        result = null;
                        break;
                    case 1:
                        result = AuthService.this.getContext().getString(17041453);
                        break;
                    case 2:
                        result = AuthService.this.getContext().getString(17040342);
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                    default:
                        if ((modality & 1) == 0) {
                            result = AuthService.this.getContext().getString(17039802);
                            break;
                        } else {
                            int biometricModality = modality & (-2);
                            if (biometricModality == 2) {
                                result = AuthService.this.getContext().getString(17040360);
                                break;
                            } else if (biometricModality == 8) {
                                result = AuthService.this.getContext().getString(17040306);
                                break;
                            } else {
                                result = AuthService.this.getContext().getString(17039811);
                                break;
                            }
                        }
                    case 4:
                        result = AuthService.this.getContext().getString(17039802);
                        break;
                    case 8:
                        result = AuthService.this.getContext().getString(17040287);
                        break;
                }
                return result;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public AuthService(Context context) {
        this(context, new Injector());
    }

    public AuthService(Context context, Injector injector) {
        super(context);
        this.mInjector = injector;
        this.mImpl = new AuthServiceImpl();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        SensorConfig[] hidlConfigs;
        this.mBiometricService = this.mInjector.getBiometricService();
        if (!this.mInjector.isHidlDisabled(getContext())) {
            int firstApiLevel = SystemProperties.getInt(SYSPROP_FIRST_API_LEVEL, 0);
            int apiLevel = SystemProperties.getInt(SYSPROP_API_LEVEL, firstApiLevel);
            String[] configStrings = this.mInjector.getConfiguration(getContext());
            if (configStrings.length == 0 && apiLevel == 30) {
                Slog.w(TAG, "Found R vendor partition without config_biometric_sensors");
                configStrings = generateRSdkCompatibleConfiguration();
            }
            hidlConfigs = new SensorConfig[configStrings.length];
            for (int i = 0; i < configStrings.length; i++) {
                hidlConfigs[i] = new SensorConfig(configStrings[i]);
            }
        } else {
            hidlConfigs = null;
        }
        registerAuthenticators(hidlConfigs);
        this.mInjector.publishBinderService(this, this.mImpl);
    }

    private String[] generateRSdkCompatibleConfiguration() {
        PackageManager pm = getContext().getPackageManager();
        ArrayList<String> modalities = new ArrayList<>();
        if (pm.hasSystemFeature("android.hardware.fingerprint")) {
            modalities.add(String.valueOf(2));
        }
        if (pm.hasSystemFeature("android.hardware.biometrics.face")) {
            modalities.add(String.valueOf(8));
        }
        String strength = String.valueOf(4095);
        String[] configStrings = new String[modalities.size()];
        for (int i = 0; i < modalities.size(); i++) {
            String id = String.valueOf(i);
            String modality = modalities.get(i);
            configStrings[i] = String.join(":", id, modality, strength);
        }
        Slog.d(TAG, "Generated config_biometric_sensors: " + Arrays.toString(configStrings));
        return configStrings;
    }

    private void registerAuthenticators(SensorConfig[] hidlSensors) {
        List<FingerprintSensorPropertiesInternal> hidlFingerprintSensors = new ArrayList<>();
        List<FaceSensorPropertiesInternal> hidlFaceSensors = new ArrayList<>();
        List<SensorPropertiesInternal> hidlIrisSensors = new ArrayList<>();
        if (hidlSensors != null) {
            for (SensorConfig sensor : hidlSensors) {
                Slog.d(TAG, "Registering HIDL ID: " + sensor.id + " Modality: " + sensor.modality + " Strength: " + sensor.strength);
                switch (sensor.modality) {
                    case 2:
                        hidlFingerprintSensors.add(getHidlFingerprintSensorProps(sensor.id, sensor.strength));
                        break;
                    case 4:
                        hidlIrisSensors.add(getHidlIrisSensorProps(sensor.id, sensor.strength));
                        break;
                    case 8:
                        hidlFaceSensors.add(getHidlFaceSensorProps(sensor.id, sensor.strength));
                        break;
                    default:
                        Slog.e(TAG, "Unknown modality: " + sensor.modality);
                        break;
                }
            }
        }
        IFingerprintService fingerprintService = this.mInjector.getFingerprintService();
        if (fingerprintService != null) {
            try {
                fingerprintService.registerAuthenticators(hidlFingerprintSensors);
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when registering fingerprint authenticators", e);
            }
        } else if (hidlFingerprintSensors.size() > 0) {
            Slog.e(TAG, "HIDL fingerprint configuration exists, but FingerprintService is null.");
        }
        IFaceService faceService = this.mInjector.getFaceService();
        if (faceService != null) {
            try {
                faceService.registerAuthenticators(hidlFaceSensors);
            } catch (RemoteException e2) {
                Slog.e(TAG, "RemoteException when registering face authenticators", e2);
            }
        } else if (hidlFaceSensors.size() > 0) {
            Slog.e(TAG, "HIDL face configuration exists, but FaceService is null.");
        }
        IIrisService irisService = this.mInjector.getIrisService();
        if (irisService != null) {
            try {
                irisService.registerAuthenticators(hidlIrisSensors);
            } catch (RemoteException e3) {
                Slog.e(TAG, "RemoteException when registering iris authenticators", e3);
            }
        } else if (hidlIrisSensors.size() > 0) {
            Slog.e(TAG, "HIDL iris configuration exists, but IrisService is null.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkInternalPermission() {
        getContext().enforceCallingOrSelfPermission("android.permission.USE_BIOMETRIC_INTERNAL", "Must have USE_BIOMETRIC_INTERNAL permission");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPermission() {
        if (getContext().checkCallingOrSelfPermission("android.permission.USE_FINGERPRINT") != 0) {
            getContext().enforceCallingOrSelfPermission("android.permission.USE_BIOMETRIC", "Must have USE_BIOMETRIC permission");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkAppOps(int uid, String opPackageName, String reason) {
        return this.mInjector.getAppOps(getContext()).noteOp(78, uid, opPackageName, (String) null, reason) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getCredentialBackupModality(int modality) {
        return modality == 1 ? modality : modality & (-2);
    }

    private FingerprintSensorPropertiesInternal getHidlFingerprintSensorProps(int sensorId, int strength) {
        int sensorType;
        int[] udfpsProps = getContext().getResources().getIntArray(17236149);
        boolean isUdfps = !ArrayUtils.isEmpty(udfpsProps);
        boolean isPowerbuttonFps = getContext().getResources().getBoolean(17891686);
        if (isUdfps) {
            sensorType = 3;
        } else if (isPowerbuttonFps) {
            sensorType = 4;
        } else {
            sensorType = 1;
        }
        int maxEnrollmentsPerUser = getContext().getResources().getInteger(17694832);
        List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        if (isUdfps && udfpsProps.length == 3) {
            return new FingerprintSensorPropertiesInternal(sensorId, Utils.authenticatorStrengthToPropertyStrength(strength), maxEnrollmentsPerUser, componentInfo, sensorType, true, false, List.of(new SensorLocationInternal("", udfpsProps[0], udfpsProps[1], udfpsProps[2])));
        }
        return new FingerprintSensorPropertiesInternal(sensorId, Utils.authenticatorStrengthToPropertyStrength(strength), maxEnrollmentsPerUser, componentInfo, sensorType, false);
    }

    private FaceSensorPropertiesInternal getHidlFaceSensorProps(int sensorId, int strength) {
        boolean supportsSelfIllumination = getContext().getResources().getBoolean(17891661);
        int maxTemplatesAllowed = getContext().getResources().getInteger(17694831);
        List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        return new FaceSensorPropertiesInternal(sensorId, Utils.authenticatorStrengthToPropertyStrength(strength), maxTemplatesAllowed, componentInfo, 0, false, supportsSelfIllumination, true);
    }

    private SensorPropertiesInternal getHidlIrisSensorProps(int sensorId, int strength) {
        List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        return new SensorPropertiesInternal(sensorId, Utils.authenticatorStrengthToPropertyStrength(strength), 1, componentInfo, false, false);
    }
}
