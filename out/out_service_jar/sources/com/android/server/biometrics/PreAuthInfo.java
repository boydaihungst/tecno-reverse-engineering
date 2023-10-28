package com.android.server.biometrics;

import android.app.admin.DevicePolicyManager;
import android.app.trust.ITrustManager;
import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.biometrics.PromptInfo;
import android.os.RemoteException;
import android.util.Pair;
import android.util.Slog;
import com.android.server.biometrics.BiometricService;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PreAuthInfo {
    static final int AUTHENTICATOR_OK = 1;
    static final int BIOMETRIC_DISABLED_BY_DEVICE_POLICY = 3;
    static final int BIOMETRIC_HARDWARE_NOT_DETECTED = 6;
    static final int BIOMETRIC_INSUFFICIENT_STRENGTH = 4;
    static final int BIOMETRIC_INSUFFICIENT_STRENGTH_AFTER_DOWNGRADE = 5;
    static final int BIOMETRIC_LOCKOUT_PERMANENT = 11;
    static final int BIOMETRIC_LOCKOUT_TIMED = 10;
    static final int BIOMETRIC_NOT_ENABLED_FOR_APPS = 8;
    static final int BIOMETRIC_NOT_ENROLLED = 7;
    static final int BIOMETRIC_NO_HARDWARE = 2;
    static final int BIOMETRIC_SENSOR_PRIVACY_ENABLED = 12;
    static final int CREDENTIAL_NOT_ENROLLED = 9;
    private static final String TAG = "BiometricService/PreAuthInfo";
    final boolean confirmationRequested;
    final Context context;
    final boolean credentialAvailable;
    final boolean credentialRequested;
    final List<BiometricSensor> eligibleSensors;
    final boolean ignoreEnrollmentState;
    final List<Pair<BiometricSensor, Integer>> ineligibleSensors;
    private final boolean mBiometricRequested;
    private final int mBiometricStrengthRequested;
    final int userId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface AuthenticatorStatus {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PreAuthInfo create(ITrustManager trustManager, DevicePolicyManager devicePolicyManager, BiometricService.SettingObserver settingObserver, List<BiometricSensor> sensors, int userId, PromptInfo promptInfo, String opPackageName, boolean checkDevicePolicyManager, Context context) throws RemoteException {
        List<Pair<BiometricSensor, Integer>> ineligibleSensors;
        boolean confirmationRequested;
        List<BiometricSensor> eligibleSensors;
        boolean confirmationRequested2 = promptInfo.isConfirmationRequested();
        boolean biometricRequested = Utils.isBiometricRequested(promptInfo);
        int requestedStrength = Utils.getPublicBiometricStrength(promptInfo);
        boolean credentialRequested = Utils.isCredentialRequested(promptInfo);
        boolean credentialAvailable = trustManager.isDeviceSecure(userId, context.getAssociatedDisplayId());
        List<BiometricSensor> eligibleSensors2 = new ArrayList<>();
        List<Pair<BiometricSensor, Integer>> ineligibleSensors2 = new ArrayList<>();
        if (!biometricRequested) {
            ineligibleSensors = ineligibleSensors2;
            confirmationRequested = confirmationRequested2;
            eligibleSensors = eligibleSensors2;
        } else {
            for (BiometricSensor sensor : sensors) {
                List<Pair<BiometricSensor, Integer>> ineligibleSensors3 = ineligibleSensors2;
                boolean confirmationRequested3 = confirmationRequested2;
                List<BiometricSensor> eligibleSensors3 = eligibleSensors2;
                int status = getStatusForBiometricAuthenticator(devicePolicyManager, settingObserver, sensor, userId, opPackageName, checkDevicePolicyManager, requestedStrength, promptInfo.getAllowedSensorIds(), promptInfo.isIgnoreEnrollmentState(), context);
                Slog.d(TAG, "Package: " + opPackageName + " Sensor ID: " + sensor.id + " Modality: " + sensor.modality + " Status: " + status);
                if (status == 1 || status == 12) {
                    eligibleSensors3.add(sensor);
                } else {
                    ineligibleSensors3.add(new Pair<>(sensor, Integer.valueOf(status)));
                }
                eligibleSensors2 = eligibleSensors3;
                ineligibleSensors2 = ineligibleSensors3;
                confirmationRequested2 = confirmationRequested3;
            }
            ineligibleSensors = ineligibleSensors2;
            confirmationRequested = confirmationRequested2;
            eligibleSensors = eligibleSensors2;
        }
        return new PreAuthInfo(biometricRequested, requestedStrength, credentialRequested, eligibleSensors, ineligibleSensors, credentialAvailable, confirmationRequested, promptInfo.isIgnoreEnrollmentState(), userId, context);
    }

    private static int getStatusForBiometricAuthenticator(DevicePolicyManager devicePolicyManager, BiometricService.SettingObserver settingObserver, BiometricSensor sensor, int userId, String opPackageName, boolean checkDevicePolicyManager, int requestedStrength, List<Integer> requestedSensorIds, boolean ignoreEnrollmentState, Context context) {
        if (!requestedSensorIds.isEmpty() && !requestedSensorIds.contains(Integer.valueOf(sensor.id))) {
            return 2;
        }
        boolean wasStrongEnough = Utils.isAtLeastStrength(sensor.oemStrength, requestedStrength);
        boolean isStrongEnough = Utils.isAtLeastStrength(sensor.getCurrentStrength(), requestedStrength);
        if (wasStrongEnough && !isStrongEnough) {
            return 5;
        }
        if (wasStrongEnough) {
            try {
                if (!sensor.impl.isHardwareDetected(opPackageName)) {
                    return 6;
                }
                if (!sensor.impl.hasEnrolledTemplates(userId, opPackageName) && !ignoreEnrollmentState) {
                    return 7;
                }
                try {
                    SensorPrivacyManager sensorPrivacyManager = (SensorPrivacyManager) context.getSystemService(SensorPrivacyManager.class);
                    if (sensorPrivacyManager != null && sensor.modality == 8 && sensorPrivacyManager.isSensorPrivacyEnabled(2, userId)) {
                        return 12;
                    }
                    int lockoutMode = sensor.impl.getLockoutModeForUser(userId);
                    if (lockoutMode == 1) {
                        return 10;
                    }
                    if (lockoutMode == 2) {
                        return 11;
                    }
                    if (!isEnabledForApp(settingObserver, sensor.modality, userId)) {
                        return 8;
                    }
                    if (!checkDevicePolicyManager || !isBiometricDisabledByDevicePolicy(devicePolicyManager, sensor.modality, userId)) {
                        return 1;
                    }
                    return 3;
                } catch (RemoteException e) {
                    return 6;
                }
            } catch (RemoteException e2) {
            }
        } else {
            return 4;
        }
    }

    private static boolean isEnabledForApp(BiometricService.SettingObserver settingObserver, int modality, int userId) {
        return settingObserver.getEnabledForApps(userId);
    }

    private static boolean isBiometricDisabledByDevicePolicy(DevicePolicyManager devicePolicyManager, int modality, int effectiveUserId) {
        int biometricToCheck = mapModalityToDevicePolicyType(modality);
        if (biometricToCheck == 0) {
            throw new IllegalStateException("Modality unknown to devicePolicyManager: " + modality);
        }
        int devicePolicyDisabledFeatures = devicePolicyManager.getKeyguardDisabledFeatures(null, effectiveUserId);
        boolean isBiometricDisabled = (biometricToCheck & devicePolicyDisabledFeatures) != 0;
        Slog.w(TAG, "isBiometricDisabledByDevicePolicy(" + modality + "," + effectiveUserId + ")=" + isBiometricDisabled);
        return isBiometricDisabled;
    }

    private static int mapModalityToDevicePolicyType(int modality) {
        switch (modality) {
            case 2:
                return 32;
            case 4:
                return 256;
            case 8:
                return 128;
            default:
                Slog.e(TAG, "Error modality=" + modality);
                return 0;
        }
    }

    private PreAuthInfo(boolean biometricRequested, int biometricStrengthRequested, boolean credentialRequested, List<BiometricSensor> eligibleSensors, List<Pair<BiometricSensor, Integer>> ineligibleSensors, boolean credentialAvailable, boolean confirmationRequested, boolean ignoreEnrollmentState, int userId, Context context) {
        this.mBiometricRequested = biometricRequested;
        this.mBiometricStrengthRequested = biometricStrengthRequested;
        this.credentialRequested = credentialRequested;
        this.eligibleSensors = eligibleSensors;
        this.ineligibleSensors = ineligibleSensors;
        this.credentialAvailable = credentialAvailable;
        this.confirmationRequested = confirmationRequested;
        this.ignoreEnrollmentState = ignoreEnrollmentState;
        this.userId = userId;
        this.context = context;
    }

    private Pair<BiometricSensor, Integer> calculateErrorByPriority() {
        for (Pair<BiometricSensor, Integer> pair : this.ineligibleSensors) {
            if (((Integer) pair.second).intValue() == 7) {
                return pair;
            }
        }
        return this.ineligibleSensors.get(0);
    }

    private Pair<Integer, Integer> getInternalStatus() {
        int status;
        int modality = 0;
        SensorPrivacyManager sensorPrivacyManager = (SensorPrivacyManager) this.context.getSystemService(SensorPrivacyManager.class);
        boolean cameraPrivacyEnabled = false;
        if (sensorPrivacyManager != null) {
            cameraPrivacyEnabled = sensorPrivacyManager.isSensorPrivacyEnabled(2, this.userId);
        }
        boolean z = this.mBiometricRequested;
        if (z && this.credentialRequested) {
            if (this.credentialAvailable || !this.eligibleSensors.isEmpty()) {
                for (BiometricSensor sensor : this.eligibleSensors) {
                    modality |= sensor.modality;
                }
                if (this.credentialAvailable) {
                    modality |= 1;
                    status = 1;
                } else if (modality == 8 && cameraPrivacyEnabled) {
                    status = 12;
                } else {
                    status = 1;
                }
            } else if (!this.ineligibleSensors.isEmpty()) {
                Pair<BiometricSensor, Integer> pair = calculateErrorByPriority();
                modality = 0 | ((BiometricSensor) pair.first).modality;
                status = ((Integer) pair.second).intValue();
            } else {
                modality = 0 | 1;
                status = 9;
            }
        } else if (z) {
            if (!this.eligibleSensors.isEmpty()) {
                for (BiometricSensor sensor2 : this.eligibleSensors) {
                    modality |= sensor2.modality;
                }
                if (modality == 8 && cameraPrivacyEnabled) {
                    status = 12;
                } else {
                    status = 1;
                }
            } else if (!this.ineligibleSensors.isEmpty()) {
                Pair<BiometricSensor, Integer> pair2 = calculateErrorByPriority();
                modality = 0 | ((BiometricSensor) pair2.first).modality;
                status = ((Integer) pair2.second).intValue();
            } else {
                modality = 0 | 0;
                status = 2;
            }
        } else if (this.credentialRequested) {
            modality = 0 | 1;
            status = this.credentialAvailable ? 1 : 9;
        } else {
            Slog.e(TAG, "No authenticators requested");
            status = 2;
        }
        Slog.d(TAG, "getCanAuthenticateInternal Modality: " + modality + " AuthenticatorStatus: " + status);
        return new Pair<>(Integer.valueOf(modality), Integer.valueOf(status));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCanAuthenticateResult() {
        return Utils.biometricConstantsToBiometricManager(Utils.authenticatorStatusToBiometricConstant(((Integer) getInternalStatus().second).intValue()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<Integer, Integer> getPreAuthenticateStatus() {
        Pair<Integer, Integer> internalStatus = getInternalStatus();
        int publicError = Utils.authenticatorStatusToBiometricConstant(((Integer) internalStatus.second).intValue());
        int modality = ((Integer) internalStatus.first).intValue();
        switch (((Integer) internalStatus.second).intValue()) {
            case 1:
            case 2:
            case 5:
            case 6:
            case 7:
            case 9:
            case 10:
            case 11:
            case 12:
                break;
            case 3:
            case 4:
            case 8:
            default:
                modality = 0;
                break;
        }
        return new Pair<>(Integer.valueOf(modality), Integer.valueOf(publicError));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldShowCredential() {
        return this.credentialRequested && this.credentialAvailable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getEligibleModalities() {
        int modalities = 0;
        for (BiometricSensor sensor : this.eligibleSensors) {
            modalities |= sensor.modality;
        }
        if (this.credentialRequested && this.credentialAvailable) {
            return modalities | 1;
        }
        return modalities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numSensorsWaitingForCookie() {
        int numWaiting = 0;
        for (BiometricSensor sensor : this.eligibleSensors) {
            if (sensor.getSensorState() == 1) {
                Slog.d(TAG, "Sensor ID: " + sensor.id + " Waiting for cookie: " + sensor.getCookie());
                numWaiting++;
            }
        }
        return numWaiting;
    }

    public String toString() {
        StringBuilder string = new StringBuilder("BiometricRequested: " + this.mBiometricRequested + ", StrengthRequested: " + this.mBiometricStrengthRequested + ", CredentialRequested: " + this.credentialRequested);
        string.append(", Eligible:{");
        for (BiometricSensor sensor : this.eligibleSensors) {
            string.append(sensor.id).append(" ");
        }
        string.append("}");
        string.append(", Ineligible:{");
        for (Pair<BiometricSensor, Integer> ineligible : this.ineligibleSensors) {
            string.append(ineligible.first).append(":").append(ineligible.second).append(" ");
        }
        string.append("}");
        string.append(", CredentialAvailable: ").append(this.credentialAvailable);
        string.append(", ");
        return string.toString();
    }
}
