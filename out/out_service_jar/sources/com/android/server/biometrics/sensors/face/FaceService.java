package com.android.server.biometrics.sensors.face;

import android.content.Context;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.face.IFace;
import android.hardware.biometrics.face.SensorProps;
import android.hardware.face.Face;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.FaceServiceReceiver;
import android.hardware.face.IFaceService;
import android.hardware.face.IFaceServiceReceiver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.NativeHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.face.FaceService;
import com.android.server.biometrics.sensors.face.aidl.FaceProvider;
import com.android.server.biometrics.sensors.face.hidl.Face10;
import com.android.server.slice.SliceClientPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
public class FaceService extends SystemService {
    protected static final String TAG = "FaceService";
    private final LockPatternUtils mLockPatternUtils;
    private final LockoutResetDispatcher mLockoutResetDispatcher;
    private final List<ServiceProvider> mServiceProviders;
    private final FaceServiceWrapper mServiceWrapper;

    public static native NativeHandle acquireSurfaceHandle(Surface surface);

    public static native void releaseSurfaceHandle(NativeHandle nativeHandle);

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
        List<FaceSensorPropertiesInternal> properties = getSensorProperties();
        if (properties.size() != 1) {
            Slog.e(TAG, "Multiple sensors found: " + properties.size());
            return null;
        }
        int sensorId = properties.get(0).sensorId;
        for (ServiceProvider provider : this.mServiceProviders) {
            if (provider.containsSensor(sensorId)) {
                return new Pair<>(Integer.valueOf(sensorId), provider);
            }
        }
        Slog.e(TAG, "Single sensor, but provider not found");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<FaceSensorPropertiesInternal> getSensorProperties() {
        List<FaceSensorPropertiesInternal> properties = new ArrayList<>();
        for (ServiceProvider provider : this.mServiceProviders) {
            properties.addAll(provider.getSensorProperties());
        }
        return properties;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FaceServiceWrapper extends IFaceService.Stub {
        private FaceServiceWrapper() {
        }

        public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for createTestSession, sensorId: " + sensorId);
                return null;
            }
            return provider.createTestSession(sensorId, callback, opPackageName);
        }

        public byte[] dumpSensorServiceStateProto(int sensorId, boolean clearSchedulerBuffer) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ProtoOutputStream proto = new ProtoOutputStream();
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider != null) {
                provider.dumpProtoState(sensorId, proto, clearSchedulerBuffer);
            }
            proto.flush();
            return proto.getBytes();
        }

        public List<FaceSensorPropertiesInternal> getSensorPropertiesInternal(String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            return FaceService.this.getSensorProperties();
        }

        public FaceSensorPropertiesInternal getSensorProperties(int sensorId, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "No matching sensor for getSensorProperties, sensorId: " + sensorId + ", caller: " + opPackageName);
                return null;
            }
            return provider.getSensorProperties(sensorId);
        }

        public void generateChallenge(IBinder token, int sensorId, int userId, IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "No matching sensor for generateChallenge, sensorId: " + sensorId);
            } else {
                provider.scheduleGenerateChallenge(sensorId, userId, token, receiver, opPackageName);
            }
        }

        public void revokeChallenge(IBinder token, int sensorId, int userId, String opPackageName, long challenge) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "No matching sensor for revokeChallenge, sensorId: " + sensorId);
            } else {
                provider.scheduleRevokeChallenge(sensorId, userId, token, opPackageName, challenge);
            }
        }

        public long enroll(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures, Surface previewSurface, boolean debugConsent) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for enroll");
                return -1L;
            }
            return ((ServiceProvider) provider.second).scheduleEnroll(((Integer) provider.first).intValue(), token, hardwareAuthToken, userId, receiver, opPackageName, disabledFeatures, previewSurface, debugConsent);
        }

        public long enrollRemotely(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            return -1L;
        }

        public void cancelEnrollment(IBinder token, long requestId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for cancelEnrollment");
            } else {
                ((ServiceProvider) provider.second).cancelEnrollment(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public long authenticate(IBinder token, long operationId, int userId, IFaceServiceReceiver receiver, String opPackageName, boolean isKeyguardBypassEnabled) {
            int statsClient;
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (Utils.isKeyguard(FaceService.this.getContext(), opPackageName)) {
                statsClient = 1;
            } else {
                statsClient = 0;
            }
            boolean isKeyguard = Utils.isKeyguard(FaceService.this.getContext(), opPackageName);
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for authenticate");
                return -1L;
            }
            return ((ServiceProvider) provider.second).scheduleAuthenticate(((Integer) provider.first).intValue(), token, operationId, userId, 0, new ClientMonitorCallbackConverter(receiver), opPackageName, false, statsClient, isKeyguard, isKeyguardBypassEnabled);
        }

        public long detectFace(IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (!Utils.isKeyguard(FaceService.this.getContext(), opPackageName)) {
                Slog.w(FaceService.TAG, "detectFace called from non-sysui package: " + opPackageName);
                return -1L;
            } else if (!Utils.isUserEncryptedOrLockdown(FaceService.this.mLockPatternUtils, userId)) {
                Slog.e(FaceService.TAG, "detectFace invoked when user is not encrypted or lockdown");
                return -1L;
            } else {
                Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
                if (provider == null) {
                    Slog.w(FaceService.TAG, "Null provider for detectFace");
                    return -1L;
                }
                return ((ServiceProvider) provider.second).scheduleFaceDetect(((Integer) provider.first).intValue(), token, userId, new ClientMonitorCallbackConverter(receiver), opPackageName, 1);
            }
        }

        public void prepareForAuthentication(int sensorId, boolean requireConfirmation, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for prepareForAuthentication");
            } else {
                provider.scheduleAuthenticate(sensorId, token, operationId, userId, cookie, new ClientMonitorCallbackConverter(sensorReceiver), opPackageName, requestId, true, 2, allowBackgroundAuthentication, false);
            }
        }

        public void startPreparedClient(int sensorId, int cookie) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for startPreparedClient");
            } else {
                provider.startPreparedClient(sensorId, cookie);
            }
        }

        public void cancelAuthentication(IBinder token, String opPackageName, long requestId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for cancelAuthentication");
            } else {
                ((ServiceProvider) provider.second).cancelAuthentication(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public void cancelFaceDetect(IBinder token, String opPackageName, long requestId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (!Utils.isKeyguard(FaceService.this.getContext(), opPackageName)) {
                Slog.w(FaceService.TAG, "cancelFaceDetect called from non-sysui package: " + opPackageName);
                return;
            }
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for cancelFaceDetect");
            } else {
                ((ServiceProvider) provider.second).cancelFaceDetect(((Integer) provider.first).intValue(), token, requestId);
            }
        }

        public void cancelAuthenticationFromService(int sensorId, IBinder token, String opPackageName, long requestId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for cancelAuthenticationFromService");
            } else {
                provider.cancelAuthentication(sensorId, token, requestId);
            }
        }

        public void remove(IBinder token, int faceId, int userId, IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for remove");
            } else {
                ((ServiceProvider) provider.second).scheduleRemove(((Integer) provider.first).intValue(), token, faceId, userId, receiver, opPackageName);
            }
        }

        public void removeAll(IBinder token, int userId, final IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            IFaceServiceReceiver iFaceServiceReceiver = new FaceServiceReceiver() { // from class: com.android.server.biometrics.sensors.face.FaceService.FaceServiceWrapper.1
                final int numSensors;
                int sensorsFinishedRemoving = 0;

                {
                    this.numSensors = FaceServiceWrapper.this.getSensorPropertiesInternal(FaceService.this.getContext().getOpPackageName()).size();
                }

                public void onRemoved(Face face, int remaining) throws RemoteException {
                    if (remaining == 0) {
                        this.sensorsFinishedRemoving++;
                        Slog.d(FaceService.TAG, "sensorsFinishedRemoving: " + this.sensorsFinishedRemoving + ", numSensors: " + this.numSensors);
                        if (this.sensorsFinishedRemoving == this.numSensors) {
                            receiver.onRemoved((Face) null, 0);
                        }
                    }
                }
            };
            for (ServiceProvider provider : FaceService.this.mServiceProviders) {
                List<FaceSensorPropertiesInternal> props = provider.getSensorProperties();
                for (FaceSensorPropertiesInternal prop : props) {
                    provider.scheduleRemoveAll(prop.sensorId, token, userId, iFaceServiceReceiver, opPackageName);
                }
            }
        }

        public void addLockoutResetCallback(IBiometricServiceLockoutResetCallback callback, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            FaceService.this.mLockoutResetDispatcher.addCallback(callback, opPackageName);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(FaceService.this.getContext(), FaceService.TAG, pw)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                if (args.length > 1 && "--proto".equals(args[0]) && "--state".equals(args[1])) {
                    ProtoOutputStream proto = new ProtoOutputStream(fd);
                    for (ServiceProvider provider : FaceService.this.mServiceProviders) {
                        for (FaceSensorPropertiesInternal props : provider.getSensorProperties()) {
                            provider.dumpProtoState(props.sensorId, proto, false);
                        }
                    }
                    proto.flush();
                } else if (args.length > 0 && "--proto".equals(args[0])) {
                    for (ServiceProvider provider2 : FaceService.this.mServiceProviders) {
                        for (FaceSensorPropertiesInternal props2 : provider2.getSensorProperties()) {
                            provider2.dumpProtoMetrics(props2.sensorId, fd);
                        }
                    }
                } else if (args.length > 1 && "--hal".equals(args[0])) {
                    for (ServiceProvider provider3 : FaceService.this.mServiceProviders) {
                        for (FaceSensorPropertiesInternal props3 : provider3.getSensorProperties()) {
                            provider3.dumpHal(props3.sensorId, fd, (String[]) Arrays.copyOfRange(args, 1, args.length, args.getClass()));
                        }
                    }
                } else {
                    for (ServiceProvider provider4 : FaceService.this.mServiceProviders) {
                        for (FaceSensorPropertiesInternal props4 : provider4.getSensorProperties()) {
                            pw.println("Dumping for sensorId: " + props4.sensorId + ", provider: " + provider4.getClass().getSimpleName());
                            provider4.dumpInternal(props4.sensorId, pw);
                            pw.println();
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isHardwareDetected(int sensorId, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            long token = Binder.clearCallingIdentity();
            try {
                ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
                if (provider == null) {
                    Slog.w(FaceService.TAG, "Null provider for isHardwareDetected, caller: " + opPackageName);
                    return false;
                }
                return provider.isHardwareDetected(sensorId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<Face> getEnrolledFaces(int sensorId, int userId, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (userId != UserHandle.getCallingUserId()) {
                Utils.checkPermission(FaceService.this.getContext(), "android.permission.INTERACT_ACROSS_USERS");
            }
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for getEnrolledFaces, caller: " + opPackageName);
                return Collections.emptyList();
            }
            return provider.getEnrolledFaces(sensorId, userId);
        }

        public boolean hasEnrolledFaces(int sensorId, int userId, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            if (userId != UserHandle.getCallingUserId()) {
                Utils.checkPermission(FaceService.this.getContext(), "android.permission.INTERACT_ACROSS_USERS");
            }
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider != null) {
                return provider.getEnrolledFaces(sensorId, userId).size() > 0;
            }
            Slog.w(FaceService.TAG, "Null provider for hasEnrolledFaces, caller: " + opPackageName);
            return false;
        }

        public int getLockoutModeForUser(int sensorId, int userId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for getLockoutModeForUser");
                return 0;
            }
            return provider.getLockoutModeForUser(sensorId, userId);
        }

        public void invalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for invalidateAuthenticatorId");
            } else {
                provider.scheduleInvalidateAuthenticatorId(sensorId, userId, callback);
            }
        }

        public long getAuthenticatorId(int sensorId, int userId) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for getAuthenticatorId");
                return 0L;
            }
            return provider.getAuthenticatorId(sensorId, userId);
        }

        public void resetLockout(IBinder token, int sensorId, int userId, byte[] hardwareAuthToken, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceProvider provider = FaceService.this.getProviderForSensor(sensorId);
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for resetLockout, caller: " + opPackageName);
            } else {
                provider.scheduleResetLockout(sensorId, userId, hardwareAuthToken);
            }
        }

        public void setFeature(IBinder token, int userId, int feature, boolean enabled, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for setFeature");
            } else {
                ((ServiceProvider) provider.second).scheduleSetFeature(((Integer) provider.first).intValue(), token, userId, feature, enabled, hardwareAuthToken, receiver, opPackageName);
            }
        }

        public void getFeature(IBinder token, int userId, int feature, IFaceServiceReceiver receiver, String opPackageName) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.MANAGE_BIOMETRIC");
            Pair<Integer, ServiceProvider> provider = FaceService.this.getSingleProvider();
            if (provider == null) {
                Slog.w(FaceService.TAG, "Null provider for getFeature");
            } else {
                ((ServiceProvider) provider.second).scheduleGetFeature(((Integer) provider.first).intValue(), token, userId, feature, new ClientMonitorCallbackConverter(receiver), opPackageName);
            }
        }

        private void addHidlProviders(List<FaceSensorPropertiesInternal> hidlSensors) {
            for (FaceSensorPropertiesInternal hidlSensor : hidlSensors) {
                FaceService.this.mServiceProviders.add(Face10.newInstance(FaceService.this.getContext(), hidlSensor, FaceService.this.mLockoutResetDispatcher));
            }
        }

        private void addAidlProviders() {
            String[] instances = ServiceManager.getDeclaredInstances(IFace.DESCRIPTOR);
            if (instances == null || instances.length == 0) {
                return;
            }
            for (String instance : instances) {
                String fqName = IFace.DESCRIPTOR + SliceClientPermissions.SliceAuthority.DELIMITER + instance;
                IFace face = IFace.Stub.asInterface(Binder.allowBlocking(ServiceManager.waitForDeclaredService(fqName)));
                if (face == null) {
                    Slog.e(FaceService.TAG, "Unable to get declared service: " + fqName);
                } else {
                    try {
                        SensorProps[] props = face.getSensorProps();
                        FaceProvider provider = new FaceProvider(FaceService.this.getContext(), props, instance, FaceService.this.mLockoutResetDispatcher, BiometricContext.getInstance(FaceService.this.getContext()));
                        FaceService.this.mServiceProviders.add(provider);
                    } catch (RemoteException e) {
                        Slog.e(FaceService.TAG, "Remote exception in getSensorProps: " + fqName);
                    }
                }
            }
        }

        public void registerAuthenticators(final List<FaceSensorPropertiesInternal> hidlSensors) {
            Utils.checkPermission(FaceService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceThread thread = new ServiceThread(FaceService.TAG, 10, true);
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.FaceService$FaceServiceWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    FaceService.FaceServiceWrapper.this.m2328x68671e19(hidlSensors);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerAuthenticators$0$com-android-server-biometrics-sensors-face-FaceService$FaceServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2328x68671e19(List hidlSensors) {
            addHidlProviders(hidlSensors);
            addAidlProviders();
            IBiometricService biometricService = IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
            for (ServiceProvider provider : FaceService.this.mServiceProviders) {
                List<FaceSensorPropertiesInternal> props = provider.getSensorProperties();
                for (FaceSensorPropertiesInternal prop : props) {
                    int sensorId = prop.sensorId;
                    int strength = Utils.propertyStrengthToAuthenticatorStrength(prop.sensorStrength);
                    FaceAuthenticator authenticator = new FaceAuthenticator(FaceService.this.mServiceWrapper, sensorId);
                    try {
                        biometricService.registerAuthenticator(sensorId, 8, strength, authenticator);
                    } catch (RemoteException e) {
                        Slog.e(FaceService.TAG, "Remote exception when registering sensorId: " + sensorId);
                    }
                }
            }
        }
    }

    public FaceService(Context context) {
        super(context);
        this.mServiceWrapper = new FaceServiceWrapper();
        this.mLockoutResetDispatcher = new LockoutResetDispatcher(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mServiceProviders = new ArrayList();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("face", this.mServiceWrapper);
    }
}
