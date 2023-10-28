package com.android.server.biometrics.sensors.fingerprint;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.hardware.biometrics.IBiometricStateListener;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.fingerprint.IFingerprint;
import android.hardware.biometrics.fingerprint.SensorProps;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintServiceReceiver;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;
import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.Binder;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.biometrics.BiometricService;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.fingerprint.FingerprintService;
import com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21UdfpsMock;
import com.android.server.slice.SliceClientPermissions;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class FingerprintService extends SystemService {
    private static final boolean OS_FINGERPRINT_QUICKAPP_SUPPORT = SystemProperties.get("ro.os_fingerprint_quickapp_support").equals("1");
    protected static final String TAG = "FingerprintService";
    private final AppOpsManager mAppOps;
    private final RemoteCallbackList<IFingerprintAuthenticatorsRegisteredCallback> mAuthenticatorsRegisteredCallbacks;
    private final BiometricStateCallback mBiometricStateCallback;
    private final GestureAvailabilityDispatcher mGestureAvailabilityDispatcher;
    private final Handler mHandler;
    private final Object mLock;
    private final LockPatternUtils mLockPatternUtils;
    private final LockoutResetDispatcher mLockoutResetDispatcher;
    private final List<FingerprintSensorPropertiesInternal> mSensorProps;
    private final List<ServiceProvider> mServiceProviders;
    private final FingerprintServiceWrapper mServiceWrapper;

    public void registerBiometricStateListener(IBiometricStateListener listener) {
        this.mBiometricStateCallback.registerBiometricStateListener(listener);
        broadcastCurrentEnrollmentState(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastCurrentEnrollmentState(final IBiometricStateListener listener) {
        UserManager um = UserManager.get(getContext());
        synchronized (this.mLock) {
            for (final FingerprintSensorPropertiesInternal prop : this.mSensorProps) {
                ServiceProvider provider = getProviderForSensor(prop.sensorId);
                for (final UserInfo userInfo : um.getAliveUsers()) {
                    final boolean enrolled = !provider.getEnrolledFingerprints(prop.sensorId, userInfo.id).isEmpty();
                    this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            FingerprintService.this.m2438xc4c63880(listener, userInfo, prop, enrolled);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$broadcastCurrentEnrollmentState$0$com-android-server-biometrics-sensors-fingerprint-FingerprintService  reason: not valid java name */
    public /* synthetic */ void m2438xc4c63880(IBiometricStateListener listener, UserInfo userInfo, FingerprintSensorPropertiesInternal prop, boolean enrolled) {
        if (listener != null) {
            this.mBiometricStateCallback.notifyEnrollmentStateChanged(listener, userInfo.id, prop.sensorId, enrolled);
        } else {
            this.mBiometricStateCallback.notifyAllEnrollmentStateChanged(userInfo.id, prop.sensorId, enrolled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FingerprintServiceWrapper extends IFingerprintService.Stub {
        private FingerprintServiceWrapper() {
        }

        public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.TEST_BIOMETRIC");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for createTestSession, sensorId: " + sensorId);
                return null;
            }
            return provider.createTestSession(sensorId, callback, opPackageName);
        }

        public byte[] dumpSensorServiceStateProto(int sensorId, boolean clearSchedulerBuffer) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ProtoOutputStream proto = new ProtoOutputStream();
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider != null) {
                provider.dumpProtoState(sensorId, proto, clearSchedulerBuffer);
            }
            proto.flush();
            return proto.getBytes();
        }

        public List<FingerprintSensorPropertiesInternal> getSensorPropertiesInternal(String opPackageName) {
            if (FingerprintService.this.getContext().checkCallingOrSelfPermission("android.permission.USE_BIOMETRIC_INTERNAL") != 0) {
                Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.TEST_BIOMETRIC");
            }
            return FingerprintService.this.getSensorProperties();
        }

        public FingerprintSensorPropertiesInternal getSensorProperties(int sensorId, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching sensor for getSensorProperties, sensorId: " + sensorId + ", caller: " + opPackageName);
                return null;
            }
            return provider.getSensorProperties(sensorId);
        }

        public void generateChallenge(IBinder token, int sensorId, int userId, IFingerprintServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching sensor for generateChallenge, sensorId: " + sensorId);
            } else {
                provider.scheduleGenerateChallenge(sensorId, userId, token, receiver, opPackageName);
            }
        }

        public void revokeChallenge(IBinder token, int sensorId, int userId, String opPackageName, long challenge) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching sensor for revokeChallenge, sensorId: " + sensorId);
            } else {
                provider.scheduleRevokeChallenge(sensorId, userId, token, opPackageName, challenge);
            }
        }

        public long enroll(IBinder token, byte[] hardwareAuthToken, int userId, IFingerprintServiceReceiver receiver, String opPackageName, int enrollReason) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for enroll");
                return -1L;
            }
            return ((ServiceProvider) provider.second).scheduleEnroll(((Integer) provider.first).intValue(), token, hardwareAuthToken, userId, receiver, opPackageName, enrollReason);
        }

        public void cancelEnrollment(IBinder token, long requestId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for cancelEnrollment");
            } else {
                ((ServiceProvider) provider.second).cancelEnrollment(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public long authenticate(IBinder token, long operationId, int sensorId, int userId, IFingerprintServiceReceiver receiver, String opPackageName, String attributionTag, boolean ignoreEnrollmentState) {
            Pair<Integer, ServiceProvider> provider;
            Pair<Integer, ServiceProvider> provider2;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int callingUserId = UserHandle.getCallingUserId();
            if (!FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, true, callingUid, callingPid, callingUserId)) {
                Slog.w(FingerprintService.TAG, "Authenticate rejecting package: " + opPackageName);
                return -1L;
            }
            boolean isKeyguard = Utils.isKeyguard(FingerprintService.this.getContext(), opPackageName);
            long identity1 = Binder.clearCallingIdentity();
            if (isKeyguard) {
                try {
                    try {
                        if (Utils.isUserEncryptedOrLockdown(FingerprintService.this.mLockPatternUtils, userId)) {
                            EventLog.writeEvent(1397638484, "79776455");
                            Slog.e(FingerprintService.TAG, "Authenticate invoked when user is encrypted or lockdown");
                            Binder.restoreCallingIdentity(identity1);
                            return -1L;
                        }
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(identity1);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            Binder.restoreCallingIdentity(identity1);
            boolean restricted = FingerprintService.this.getContext().checkCallingPermission("android.permission.MANAGE_FINGERPRINT") != 0;
            int statsClient = isKeyguard ? 1 : 3;
            if (sensorId == -1) {
                provider = FingerprintService.this.getSingleProvider();
            } else {
                Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
                provider = new Pair<>(Integer.valueOf(sensorId), FingerprintService.this.getProviderForSensor(sensorId));
            }
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for authenticate");
                return -1L;
            }
            FingerprintSensorPropertiesInternal sensorProps = ((ServiceProvider) provider.second).getSensorProperties(sensorId);
            if (isKeyguard || Utils.isSettings(FingerprintService.this.getContext(), opPackageName) || sensorProps == null) {
                provider2 = provider;
            } else if (!sensorProps.isAnyUdfpsType()) {
                provider2 = provider;
            } else {
                try {
                    return authenticateWithPrompt(operationId, sensorProps, callingUid, callingUserId, receiver, opPackageName, ignoreEnrollmentState);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(FingerprintService.TAG, "Invalid package", e);
                    return -1L;
                }
            }
            Pair<Integer, ServiceProvider> provider3 = provider2;
            return ((ServiceProvider) provider3.second).scheduleAuthenticate(((Integer) provider3.first).intValue(), token, operationId, userId, 0, new ClientMonitorCallbackConverter(receiver), opPackageName, restricted, statsClient, isKeyguard);
        }

        private long authenticateWithPrompt(long operationId, final FingerprintSensorPropertiesInternal props, int uId, final int userId, final IFingerprintServiceReceiver receiver, String opPackageName, boolean ignoreEnrollmentState) throws PackageManager.NameNotFoundException {
            Context context = FingerprintService.this.getUiContext();
            Context promptContext = context.createPackageContextAsUser(opPackageName, 0, UserHandle.getUserHandleForUid(uId));
            Executor executor = context.getMainExecutor();
            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(promptContext).setTitle(context.getString(17039804)).setSubtitle(context.getString(17040344)).setNegativeButton(context.getString(17039360), executor, new DialogInterface.OnClickListener() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService$FingerprintServiceWrapper$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    FingerprintService.FingerprintServiceWrapper.lambda$authenticateWithPrompt$0(receiver, dialogInterface, i);
                }
            }).setIsForLegacyFingerprintManager(props.sensorId).setIgnoreEnrollmentState(ignoreEnrollmentState).build();
            BiometricPrompt.AuthenticationCallback promptCallback = new BiometricPrompt.AuthenticationCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService.FingerprintServiceWrapper.1
                @Override // android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    try {
                        if (FingerprintUtils.isKnownErrorCode(errorCode)) {
                            receiver.onError(errorCode, 0);
                        } else {
                            receiver.onError(8, errorCode);
                        }
                    } catch (RemoteException e) {
                        Slog.e(FingerprintService.TAG, "Remote exception in onAuthenticationError()", e);
                    }
                }

                @Override // android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    Fingerprint fingerprint = new Fingerprint("", 0, 0L);
                    boolean isStrong = props.sensorStrength == 2;
                    try {
                        receiver.onAuthenticationSucceeded(fingerprint, userId, isStrong);
                    } catch (RemoteException e) {
                        Slog.e(FingerprintService.TAG, "Remote exception in onAuthenticationSucceeded()", e);
                    }
                }

                @Override // android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
                public void onAuthenticationFailed() {
                    try {
                        receiver.onAuthenticationFailed();
                    } catch (RemoteException e) {
                        Slog.e(FingerprintService.TAG, "Remote exception in onAuthenticationFailed()", e);
                    }
                }

                public void onAuthenticationAcquired(int acquireInfo) {
                    try {
                        if (FingerprintUtils.isKnownAcquiredCode(acquireInfo)) {
                            receiver.onAcquired(acquireInfo, 0);
                        } else {
                            receiver.onAcquired(6, acquireInfo);
                        }
                    } catch (RemoteException e) {
                        Slog.e(FingerprintService.TAG, "Remote exception in onAuthenticationAcquired()", e);
                    }
                }
            };
            return biometricPrompt.authenticateForOperation(new CancellationSignal(), executor, promptCallback, operationId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$authenticateWithPrompt$0(IFingerprintServiceReceiver receiver, DialogInterface dialog, int which) {
            try {
                receiver.onError(10, 0);
            } catch (RemoteException e) {
                Slog.e(FingerprintService.TAG, "Remote exception in negative button onClick()", e);
            }
        }

        public long detectFingerprint(IBinder token, int userId, IFingerprintServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (!Utils.isKeyguard(FingerprintService.this.getContext(), opPackageName)) {
                Slog.w(FingerprintService.TAG, "detectFingerprint called from non-sysui package: " + opPackageName);
                return -1L;
            } else if (!Utils.isUserEncryptedOrLockdown(FingerprintService.this.mLockPatternUtils, userId)) {
                Slog.e(FingerprintService.TAG, "detectFingerprint invoked when user is not encrypted or lockdown");
                return -1L;
            } else {
                Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FingerprintService.TAG, "Null provider for detectFingerprint");
                    return -1L;
                }
                return ((ServiceProvider) provider.second).scheduleFingerDetect(((Integer) provider.first).intValue(), token, userId, new ClientMonitorCallbackConverter(receiver), opPackageName, 1);
            }
        }

        public void prepareForAuthentication(int sensorId, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for prepareForAuthentication");
            } else {
                provider.scheduleAuthenticate(sensorId, token, operationId, userId, cookie, new ClientMonitorCallbackConverter(sensorReceiver), opPackageName, requestId, true, 2, allowBackgroundAuthentication);
            }
        }

        public void startPreparedClient(int sensorId, int cookie) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for startPreparedClient");
            } else {
                provider.startPreparedClient(sensorId, cookie);
            }
        }

        public void cancelAuthentication(IBinder token, String opPackageName, String attributionTag, long requestId) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            int callingUserId = UserHandle.getCallingUserId();
            if (!FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, true, callingUid, callingPid, callingUserId)) {
                Slog.w(FingerprintService.TAG, "cancelAuthentication rejecting package: " + opPackageName);
                return;
            }
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for cancelAuthentication");
            } else {
                ((ServiceProvider) provider.second).cancelAuthentication(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public void cancelFingerprintDetect(IBinder token, String opPackageName, long requestId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (!Utils.isKeyguard(FingerprintService.this.getContext(), opPackageName)) {
                Slog.w(FingerprintService.TAG, "cancelFingerprintDetect called from non-sysui package: " + opPackageName);
                return;
            }
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for cancelFingerprintDetect");
            } else {
                ((ServiceProvider) provider.second).cancelAuthentication(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public void cancelAuthenticationFromService(int sensorId, IBinder token, String opPackageName, long requestId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            Slog.d(FingerprintService.TAG, "cancelAuthenticationFromService, sensorId: " + sensorId);
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for cancelAuthenticationFromService");
            } else {
                provider.cancelAuthentication(sensorId, token, requestId);
            }
        }

        public void remove(IBinder token, int fingerId, int userId, IFingerprintServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for remove");
            } else {
                ((ServiceProvider) provider.second).scheduleRemove(((Integer) provider.first).intValue(), token, receiver, fingerId, userId, opPackageName);
            }
        }

        public void removeAll(IBinder token, int userId, final IFingerprintServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            IFingerprintServiceReceiver iFingerprintServiceReceiver = new FingerprintServiceReceiver() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService.FingerprintServiceWrapper.2
                final int numSensors;
                int sensorsFinishedRemoving = 0;

                {
                    this.numSensors = FingerprintServiceWrapper.this.getSensorPropertiesInternal(FingerprintService.this.getContext().getOpPackageName()).size();
                }

                public void onRemoved(Fingerprint fp, int remaining) throws RemoteException {
                    if (remaining == 0) {
                        this.sensorsFinishedRemoving++;
                        Slog.d(FingerprintService.TAG, "sensorsFinishedRemoving: " + this.sensorsFinishedRemoving + ", numSensors: " + this.numSensors);
                        if (this.sensorsFinishedRemoving == this.numSensors) {
                            receiver.onRemoved((Fingerprint) null, 0);
                        }
                    }
                }
            };
            for (ServiceProvider provider : FingerprintService.this.mServiceProviders) {
                List<FingerprintSensorPropertiesInternal> props = provider.getSensorProperties();
                for (FingerprintSensorPropertiesInternal prop : props) {
                    provider.scheduleRemoveAll(prop.sensorId, token, iFingerprintServiceReceiver, userId, opPackageName);
                }
            }
        }

        public void addLockoutResetCallback(IBiometricServiceLockoutResetCallback callback, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            FingerprintService.this.mLockoutResetDispatcher.addCallback(callback, opPackageName);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(FingerprintService.this.getContext(), FingerprintService.TAG, pw)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                if (args.length > 1 && "--proto".equals(args[0]) && "--state".equals(args[1])) {
                    ProtoOutputStream proto = new ProtoOutputStream(fd);
                    for (ServiceProvider provider : FingerprintService.this.mServiceProviders) {
                        for (FingerprintSensorPropertiesInternal props : provider.getSensorProperties()) {
                            provider.dumpProtoState(props.sensorId, proto, false);
                        }
                    }
                    proto.flush();
                } else if (args.length > 0 && "--proto".equals(args[0])) {
                    for (ServiceProvider provider2 : FingerprintService.this.mServiceProviders) {
                        for (FingerprintSensorPropertiesInternal props2 : provider2.getSensorProperties()) {
                            provider2.dumpProtoMetrics(props2.sensorId, fd);
                        }
                    }
                } else {
                    for (ServiceProvider provider3 : FingerprintService.this.mServiceProviders) {
                        for (FingerprintSensorPropertiesInternal props3 : provider3.getSensorProperties()) {
                            pw.println("Dumping for sensorId: " + props3.sensorId + ", provider: " + provider3.getClass().getSimpleName());
                            pw.println("Fps state: " + FingerprintService.this.mBiometricStateCallback.getBiometricState());
                            provider3.dumpInternal(props3.sensorId, pw);
                            pw.println();
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isHardwareDetectedDeprecated(String opPackageName, String attributionTag) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                long token = Binder.clearCallingIdentity();
                try {
                    Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                    if (provider == null) {
                        Slog.w(FingerprintService.TAG, "Null provider for isHardwareDetectedDeprecated, caller: " + opPackageName);
                        return false;
                    }
                    return ((ServiceProvider) provider.second).isHardwareDetected(((Integer) provider.first).intValue());
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            return false;
        }

        public boolean isHardwareDetected(int sensorId, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for isHardwareDetected, caller: " + opPackageName);
                return false;
            }
            return provider.isHardwareDetected(sensorId);
        }

        public void rename(int fingerId, int userId, String name) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            if (!Utils.isCurrentUserOrProfile(FingerprintService.this.getContext(), userId)) {
                return;
            }
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for rename");
            } else {
                ((ServiceProvider) provider.second).rename(((Integer) provider.first).intValue(), fingerId, userId, name);
            }
        }

        public List<Fingerprint> getEnrolledFingerprints(int userId, String opPackageName, String attributionTag) {
            if (!FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return Collections.emptyList();
            }
            if (userId != UserHandle.getCallingUserId()) {
                Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.INTERACT_ACROSS_USERS");
            }
            return FingerprintService.this.getEnrolledFingerprintsDeprecated(userId, opPackageName);
        }

        public boolean hasEnrolledFingerprintsDeprecated(int userId, String opPackageName, String attributionTag) {
            if (!FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return false;
            }
            if (userId != UserHandle.getCallingUserId()) {
                Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.INTERACT_ACROSS_USERS");
            }
            return !FingerprintService.this.getEnrolledFingerprintsDeprecated(userId, opPackageName).isEmpty();
        }

        public void setAppBiometrics(int fingerId, int groupId, String packagename, int appUserId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            int effectiveGroupId = FingerprintService.this.getEffectiveUserId(groupId);
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for setAppBiometrics");
                return;
            }
            ((ServiceProvider) provider.second).setAppBiometricsForUser(((Integer) provider.first).intValue(), fingerId, effectiveGroupId, packagename, appUserId);
            ITranFingerprintService.Instance().setAppBiometrics(fingerId, groupId, packagename, appUserId);
        }

        public String getAppPackagename(int fingerId, int userId, String opPackageName, String attributionTag) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
                Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FingerprintService.TAG, "Null provider for getAppPackagename");
                    return null;
                }
                ITranFingerprintService.Instance().getAppPackagename(fingerId, userId, opPackageName, attributionTag);
                return ((ServiceProvider) provider.second).getAppPackagenameForUser(((Integer) provider.first).intValue(), fingerId, effectiveUserId);
            }
            return null;
        }

        public Fingerprint getAddFingerprint(int userId, String opPackageName, String attributionTag) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
                Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FingerprintService.TAG, "Null provider for getAddFingerprint");
                    return null;
                }
                ITranFingerprintService.Instance().getAddFingerprint(userId, opPackageName, attributionTag);
                return ((ServiceProvider) provider.second).getAddBiometricForUser(((Integer) provider.first).intValue(), effectiveUserId);
            }
            return null;
        }

        public boolean hasAppPackagename(int userId, String opPackageName, String attributionTag) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
                Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FingerprintService.TAG, "Null provider for hasAppPackagename");
                    return false;
                }
                ITranFingerprintService.Instance().hasAppPackagename(userId, opPackageName, attributionTag);
                return ((ServiceProvider) provider.second).hasAppPackagename(((Integer) provider.first).intValue(), effectiveUserId);
            }
            return false;
        }

        public boolean checkName(int userId, String opPackageName, String name, String attributionTag) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, attributionTag, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
                Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FingerprintService.TAG, "Null provider for checkName");
                    return false;
                }
                ITranFingerprintService.Instance().checkName(userId, opPackageName, name, attributionTag);
                return ((ServiceProvider) provider.second).checkName(((Integer) provider.first).intValue(), effectiveUserId, name);
            }
            return false;
        }

        public void startAppForFp(int userId, int fingerId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for startAppForFp");
                return;
            }
            ((ServiceProvider) provider.second).startAppForFp(((Integer) provider.first).intValue(), fingerId, effectiveUserId);
            ITranFingerprintService.Instance().startAppForFp(userId, fingerId);
        }

        public void notifyAppResumeForFp(int userId, String packagename, String name) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for notifyAppResumeForFp");
                return;
            }
            ((ServiceProvider) provider.second).notifyAppResumeForFp(((Integer) provider.first).intValue(), effectiveUserId, packagename, name);
            ITranFingerprintService.Instance().notifyAppResumeForFp(userId, packagename, name);
        }

        public void notifyAppPauseForFp(int userId, String packagename, String name) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            int effectiveUserId = FingerprintService.this.getEffectiveUserId(userId);
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for notifyAppPauseForFp");
                return;
            }
            ((ServiceProvider) provider.second).notifyAppPauseForFp(((Integer) provider.first).intValue(), effectiveUserId, packagename, name);
            ITranFingerprintService.Instance().notifyAppPauseForFp(userId, packagename, name);
        }

        public int getAppUserId(int fingerId, int groupId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            int effectiveGroupId = FingerprintService.this.getEffectiveUserId(groupId);
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for getAppUserId");
                return 0;
            }
            int userId = ((ServiceProvider) provider.second).getAppUserIdForUser(((Integer) provider.first).intValue(), fingerId, effectiveGroupId);
            ITranFingerprintService.Instance().getAppUserId(fingerId, groupId);
            return userId;
        }

        public void onUpdateFocusedApp(String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            Pair<Integer, ServiceProvider> provider = FingerprintService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for onUpdateFocusedApp");
                return;
            }
            ((ServiceProvider) provider.second).onUpdateFocusedApp(((Integer) provider.first).intValue(), oldPackageName, oldComponent, newPackageName, newComponent);
            ITranFingerprintService.Instance().onUpdateFocusedApp(oldPackageName, oldComponent, newPackageName, newComponent);
        }

        public boolean hasEnrolledFingerprints(int sensorId, int userId, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider != null) {
                return provider.getEnrolledFingerprints(sensorId, userId).size() > 0;
            }
            Slog.w(FingerprintService.TAG, "Null provider for hasEnrolledFingerprints, caller: " + opPackageName);
            return false;
        }

        public int getLockoutModeForUser(int sensorId, int userId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for getLockoutModeForUser");
                return 0;
            }
            return provider.getLockoutModeForUser(sensorId, userId);
        }

        public void invalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for invalidateAuthenticatorId");
            } else {
                provider.scheduleInvalidateAuthenticatorId(sensorId, userId, callback);
            }
        }

        public long getAuthenticatorId(int sensorId, int userId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for getAuthenticatorId");
                return 0L;
            }
            return provider.getAuthenticatorId(sensorId, userId);
        }

        public void resetLockout(IBinder token, int sensorId, int userId, byte[] hardwareAuthToken, String opPackageName) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.RESET_FINGERPRINT_LOCKOUT");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "Null provider for resetLockout, caller: " + opPackageName);
            } else {
                provider.scheduleResetLockout(sensorId, userId, hardwareAuthToken);
            }
        }

        public boolean isClientActive() {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.mGestureAvailabilityDispatcher.isAnySensorActive();
        }

        public void addClientActiveCallback(IFingerprintClientActiveCallback callback) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mGestureAvailabilityDispatcher.registerCallback(callback);
        }

        public void removeClientActiveCallback(IFingerprintClientActiveCallback callback) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mGestureAvailabilityDispatcher.removeCallback(callback);
        }

        private void addHidlProviders(List<FingerprintSensorPropertiesInternal> hidlSensors) {
            Fingerprint21 fingerprint21;
            for (FingerprintSensorPropertiesInternal hidlSensor : hidlSensors) {
                if ((Build.IS_USERDEBUG || Build.IS_ENG) && FingerprintService.this.getContext().getResources().getBoolean(17891340) && Settings.Secure.getIntForUser(FingerprintService.this.getContext().getContentResolver(), Fingerprint21UdfpsMock.CONFIG_ENABLE_TEST_UDFPS, 0, -2) != 0) {
                    fingerprint21 = Fingerprint21UdfpsMock.newInstance(FingerprintService.this.getContext(), FingerprintService.this.mBiometricStateCallback, hidlSensor, FingerprintService.this.mLockoutResetDispatcher, FingerprintService.this.mGestureAvailabilityDispatcher, BiometricContext.getInstance(FingerprintService.this.getContext()));
                } else {
                    fingerprint21 = Fingerprint21.newInstance(FingerprintService.this.getContext(), FingerprintService.this.mBiometricStateCallback, hidlSensor, FingerprintService.this.mHandler, FingerprintService.this.mLockoutResetDispatcher, FingerprintService.this.mGestureAvailabilityDispatcher);
                }
                FingerprintService.this.mServiceProviders.add(fingerprint21);
            }
        }

        private void addAidlProviders() {
            String[] instances;
            String str;
            String[] instances2 = ServiceManager.getDeclaredInstances(IFingerprint.DESCRIPTOR);
            if (instances2 != null && instances2.length != 0) {
                int length = instances2.length;
                int i = 0;
                while (i < length) {
                    String instance = instances2[i];
                    String fqName = IFingerprint.DESCRIPTOR + SliceClientPermissions.SliceAuthority.DELIMITER + instance;
                    IFingerprint fp = IFingerprint.Stub.asInterface(Binder.allowBlocking(ServiceManager.waitForDeclaredService(fqName)));
                    if (fp == null) {
                        Slog.e(FingerprintService.TAG, "Unable to get declared service: " + fqName);
                        instances = instances2;
                    } else {
                        try {
                            SensorProps[] props = fp.getSensorProps();
                            Context context = FingerprintService.this.getContext();
                            BiometricStateCallback biometricStateCallback = FingerprintService.this.mBiometricStateCallback;
                            LockoutResetDispatcher lockoutResetDispatcher = FingerprintService.this.mLockoutResetDispatcher;
                            GestureAvailabilityDispatcher gestureAvailabilityDispatcher = FingerprintService.this.mGestureAvailabilityDispatcher;
                            BiometricContext biometricContext = BiometricContext.getInstance(FingerprintService.this.getContext());
                            instances = instances2;
                            str = FingerprintService.TAG;
                            try {
                                FingerprintProvider provider = new FingerprintProvider(context, biometricStateCallback, props, instance, lockoutResetDispatcher, gestureAvailabilityDispatcher, biometricContext);
                                FingerprintService.this.mServiceProviders.add(provider);
                            } catch (RemoteException e) {
                                Slog.e(str, "Remote exception in getSensorProps: " + fqName);
                                i++;
                                instances2 = instances;
                            }
                        } catch (RemoteException e2) {
                            instances = instances2;
                            str = FingerprintService.TAG;
                        }
                    }
                    i++;
                    instances2 = instances;
                }
            }
        }

        public void registerAuthenticators(final List<FingerprintSensorPropertiesInternal> hidlSensors) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceThread thread = new ServiceThread(FingerprintService.TAG, 10, true);
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService$FingerprintServiceWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    FingerprintService.FingerprintServiceWrapper.this.m2439x208a2dd5(hidlSensors);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerAuthenticators$1$com-android-server-biometrics-sensors-fingerprint-FingerprintService$FingerprintServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2439x208a2dd5(List hidlSensors) {
            addHidlProviders(hidlSensors);
            addAidlProviders();
            IBiometricService biometricService = IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
            for (ServiceProvider provider : FingerprintService.this.mServiceProviders) {
                List<FingerprintSensorPropertiesInternal> props = provider.getSensorProperties();
                for (FingerprintSensorPropertiesInternal prop : props) {
                    int sensorId = prop.sensorId;
                    int strength = Utils.propertyStrengthToAuthenticatorStrength(prop.sensorStrength);
                    FingerprintAuthenticator authenticator = new FingerprintAuthenticator(FingerprintService.this.mServiceWrapper, sensorId);
                    try {
                        biometricService.registerAuthenticator(sensorId, 2, strength, authenticator);
                    } catch (RemoteException e) {
                        Slog.e(FingerprintService.TAG, "Remote exception when registering sensorId: " + sensorId);
                    }
                }
            }
            synchronized (FingerprintService.this.mLock) {
                for (ServiceProvider provider2 : FingerprintService.this.mServiceProviders) {
                    FingerprintService.this.mSensorProps.addAll(provider2.getSensorProperties());
                }
            }
            FingerprintService.this.broadcastCurrentEnrollmentState(null);
            FingerprintService.this.broadcastAllAuthenticatorsRegistered();
        }

        public void addAuthenticatorsRegisteredCallback(IFingerprintAuthenticatorsRegisteredCallback callback) {
            boolean registered;
            boolean hasSensorProps;
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (callback == null) {
                Slog.e(FingerprintService.TAG, "addAuthenticatorsRegisteredCallback, callback is null");
                return;
            }
            synchronized (FingerprintService.this.mLock) {
                registered = FingerprintService.this.mAuthenticatorsRegisteredCallbacks.register(callback);
                hasSensorProps = !FingerprintService.this.mSensorProps.isEmpty();
            }
            if (registered && hasSensorProps) {
                FingerprintService.this.broadcastAllAuthenticatorsRegistered();
            } else if (!registered) {
                Slog.e(FingerprintService.TAG, "addAuthenticatorsRegisteredCallback failed to register callback");
            }
        }

        public void onPointerDown(long requestId, int sensorId, int x, int y, float minor, float major) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching provider for onFingerDown, sensorId: " + sensorId);
            } else {
                provider.onPointerDown(requestId, sensorId, x, y, minor, major);
            }
        }

        public void onPointerUp(long requestId, int sensorId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching provider for onFingerUp, sensorId: " + sensorId);
            } else {
                provider.onPointerUp(requestId, sensorId);
            }
        }

        public void onUiReady(long requestId, int sensorId) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FingerprintService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FingerprintService.TAG, "No matching provider for onUiReady, sensorId: " + sensorId);
            } else {
                provider.onUiReady(requestId, sensorId);
            }
        }

        public void setUdfpsOverlayController(IUdfpsOverlayController controller) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            for (ServiceProvider provider : FingerprintService.this.mServiceProviders) {
                provider.setUdfpsOverlayController(controller);
            }
        }

        public void setSidefpsController(ISidefpsController controller) {
            Utils.checkPermission(FingerprintService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            for (ServiceProvider provider : FingerprintService.this.mServiceProviders) {
                provider.setSidefpsController(controller);
            }
        }

        public void registerBiometricStateListener(IBiometricStateListener listener) {
            FingerprintService.this.registerBiometricStateListener(listener);
        }

        public void setKeyguardClientVisible(String opPackageName, boolean visible) {
            ITranFingerprintService.Instance().setKeyguardClientVisible(opPackageName, visible);
        }

        public void setMyClientVisible(String opPackageName, boolean visible) {
            ITranFingerprintService.Instance().setMyClientVisible(opPackageName, visible);
        }

        public void notifyActivateFingerprint(boolean activate) {
            ITranFingerprintService.Instance().notifyActivateFingerprint(activate);
        }

        public boolean isAuthenticating() {
            return ITranFingerprintService.Instance().isAuthenticating();
        }
    }

    public FingerprintService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mServiceWrapper = new FingerprintServiceWrapper();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mGestureAvailabilityDispatcher = new GestureAvailabilityDispatcher();
        this.mLockoutResetDispatcher = new LockoutResetDispatcher(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mServiceProviders = new ArrayList();
        this.mBiometricStateCallback = new BiometricStateCallback();
        this.mAuthenticatorsRegisteredCallbacks = new RemoteCallbackList<>();
        this.mSensorProps = new ArrayList();
        this.mHandler = new Handler(BiometricService.getBiometricLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastAllAuthenticatorsRegistered() {
        List<IFingerprintAuthenticatorsRegisteredCallback> callbacks = new ArrayList<>();
        synchronized (this.mLock) {
            if (!this.mSensorProps.isEmpty()) {
                List<FingerprintSensorPropertiesInternal> props = new ArrayList<>(this.mSensorProps);
                int n = this.mAuthenticatorsRegisteredCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    IFingerprintAuthenticatorsRegisteredCallback cb = this.mAuthenticatorsRegisteredCallbacks.getBroadcastItem(i);
                    callbacks.add(cb);
                    this.mAuthenticatorsRegisteredCallbacks.unregister(cb);
                }
                this.mAuthenticatorsRegisteredCallbacks.finishBroadcast();
                for (IFingerprintAuthenticatorsRegisteredCallback cb2 : callbacks) {
                    try {
                        cb2.onAllAuthenticatorsRegistered(props);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Remote exception in onAllAuthenticatorsRegistered", e);
                    }
                }
                return;
            }
            Slog.e(TAG, "mSensorProps is empty");
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("fingerprint", this.mServiceWrapper);
        if (OS_FINGERPRINT_QUICKAPP_SUPPORT) {
            registerPackageReceiver(getContext());
        }
        ITranFingerprintService.Instance().registerPackageReceiver(getContext());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ServiceProvider getProviderForSensor(int sensorId) {
        for (ServiceProvider provider : this.mServiceProviders) {
            if (provider.containsSensor(sensorId)) {
                return provider;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Pair<Integer, ServiceProvider> getSingleProvider() {
        List<FingerprintSensorPropertiesInternal> properties = getSensorProperties();
        if (properties.isEmpty()) {
            Slog.e(TAG, "No providers found");
            return null;
        }
        int sensorId = properties.get(0).sensorId;
        for (ServiceProvider provider : this.mServiceProviders) {
            if (provider.containsSensor(sensorId)) {
                return new Pair<>(Integer.valueOf(sensorId), provider);
            }
        }
        Slog.e(TAG, "Provider not found");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<FingerprintSensorPropertiesInternal> getSensorProperties() {
        List<FingerprintSensorPropertiesInternal> list;
        synchronized (this.mLock) {
            list = this.mSensorProps;
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<Fingerprint> getEnrolledFingerprintsDeprecated(int userId, String opPackageName) {
        Pair<Integer, ServiceProvider> provider = getSingleProvider();
        if (provider == null) {
            Slog.w(TAG, "Null provider for getEnrolledFingerprintsDeprecated, caller: " + opPackageName);
            return Collections.emptyList();
        }
        return ((ServiceProvider) provider.second).getEnrolledFingerprints(((Integer) provider.first).intValue(), userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean canUseFingerprint(String opPackageName, String attributionTag, boolean requireForeground, int uid, int pid, int userId) {
        if (getContext().checkCallingPermission("android.permission.USE_FINGERPRINT") != 0) {
            Utils.checkPermission(getContext(), "android.permission.USE_BIOMETRIC");
        }
        if (Binder.getCallingUid() == 1000 || Utils.isKeyguard(getContext(), opPackageName)) {
            return true;
        }
        if (!Utils.isCurrentUserOrProfile(getContext(), userId)) {
            Slog.w(TAG, "Rejecting " + opPackageName + "; not a current user or profile");
            return false;
        } else if (!checkAppOps(uid, opPackageName, attributionTag)) {
            Slog.w(TAG, "Rejecting " + opPackageName + "; permission denied");
            return false;
        } else if (!requireForeground || Utils.isForeground(uid, pid)) {
            return true;
        } else {
            Slog.w(TAG, "Rejecting " + opPackageName + "; not in foreground");
            return false;
        }
    }

    private boolean checkAppOps(int uid, String opPackageName, String attributionTag) {
        if (this.mAppOps.noteOp(78, uid, opPackageName, attributionTag, (String) null) != 0 && this.mAppOps.noteOp(55, uid, opPackageName, attributionTag, (String) null) != 0) {
            return false;
        }
        return true;
    }

    private final void registerPackageReceiver(Context context) {
        Utils.checkPermission(context, "android.permission.USE_BIOMETRIC_INTERNAL");
        final Pair<Integer, ServiceProvider> provider = getSingleProvider();
        if (provider == null) {
            Slog.w(TAG, "Null provider for registerPackageReceiver");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                FingerprintUtils.getInstance(((Integer) provider.first).intValue()).getHomeForFp(context2);
            }
        }, filter);
        FingerprintUtils.getInstance(((Integer) provider.first).intValue()).registerUltraPowerContentResolver(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getEffectiveUserId(int userId) {
        UserManager um = UserManager.get(getContext());
        if (um != null) {
            long callingIdentity = Binder.clearCallingIdentity();
            int userId2 = um.getCredentialOwnerProfile(userId);
            Binder.restoreCallingIdentity(callingIdentity);
            return userId2;
        }
        Slog.e(TAG, "Unable to acquire UserManager");
        return userId;
    }
}
