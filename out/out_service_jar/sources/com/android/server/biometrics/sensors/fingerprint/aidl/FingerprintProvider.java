package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.SensorLocationInternal;
import android.hardware.biometrics.common.ComponentInfo;
import android.hardware.biometrics.fingerprint.IFingerprint;
import android.hardware.biometrics.fingerprint.SensorLocation;
import android.hardware.biometrics.fingerprint.SensorProps;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.HostingRecord;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.InvalidationRequesterClient;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.PerformanceTracker;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
import com.android.server.biometrics.sensors.fingerprint.ServiceProvider;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider;
import com.android.server.slice.SliceClientPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class FingerprintProvider implements IBinder.DeathRecipient, ServiceProvider {
    private final BiometricContext mBiometricContext;
    private final BiometricStateCallback mBiometricStateCallback;
    private final Context mContext;
    private IFingerprint mDaemon;
    private final String mHalInstanceName;
    private final LockoutResetDispatcher mLockoutResetDispatcher;
    private ISidefpsController mSidefpsController;
    private boolean mTestHalEnabled;
    private IUdfpsOverlayController mUdfpsOverlayController;
    private final AtomicLong mRequestCounter = new AtomicLong(0);
    final SparseArray<Sensor> mSensors = new SparseArray<>();
    private final Handler mHandler = new Handler(BiometricScheduler.getBiometricLooper());
    private final ActivityTaskManager mActivityTaskManager = ActivityTaskManager.getInstance();
    private final BiometricTaskStackListener mTaskStackListener = new BiometricTaskStackListener();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BiometricTaskStackListener extends TaskStackListener {
        private BiometricTaskStackListener() {
        }

        public void onTaskStackChanged() {
            FingerprintProvider.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$BiometricTaskStackListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    FingerprintProvider.BiometricTaskStackListener.this.m2472xb44d2071();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskStackChanged$0$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider$BiometricTaskStackListener  reason: not valid java name */
        public /* synthetic */ void m2472xb44d2071() {
            for (int i = 0; i < FingerprintProvider.this.mSensors.size(); i++) {
                BaseClientMonitor client = FingerprintProvider.this.mSensors.valueAt(i).getScheduler().getCurrentClient();
                if (!(client instanceof AuthenticationClient)) {
                    Slog.e(FingerprintProvider.this.getTag(), "Task stack changed for client: " + client);
                } else if (!Utils.isKeyguard(FingerprintProvider.this.mContext, client.getOwnerString()) && !Utils.isSystem(FingerprintProvider.this.mContext, client.getOwnerString()) && Utils.isBackground(client.getOwnerString()) && !client.isAlreadyDone()) {
                    Slog.e(FingerprintProvider.this.getTag(), "Stopping background authentication, currentClient: " + client);
                    FingerprintProvider.this.mSensors.valueAt(i).getScheduler().cancelAuthenticationOrDetection(client.getToken(), client.getRequestId());
                }
            }
        }
    }

    public FingerprintProvider(Context context, BiometricStateCallback biometricStateCallback, SensorProps[] props, String halInstanceName, LockoutResetDispatcher lockoutResetDispatcher, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, BiometricContext biometricContext) {
        int i;
        int i2;
        SensorProps[] sensorPropsArr = props;
        this.mContext = context;
        this.mBiometricStateCallback = biometricStateCallback;
        this.mHalInstanceName = halInstanceName;
        this.mLockoutResetDispatcher = lockoutResetDispatcher;
        this.mBiometricContext = biometricContext;
        List<SensorLocationInternal> workaroundLocations = getWorkaroundSensorProps(context);
        int length = sensorPropsArr.length;
        int i3 = 0;
        while (i3 < length) {
            SensorProps prop = sensorPropsArr[i3];
            int sensorId = prop.commonProps.sensorId;
            List<ComponentInfoInternal> componentInfo = new ArrayList<>();
            if (prop.commonProps.componentInfo == null) {
                i = i3;
                i2 = length;
            } else {
                ComponentInfo[] componentInfoArr = prop.commonProps.componentInfo;
                int length2 = componentInfoArr.length;
                int i4 = 0;
                while (i4 < length2) {
                    ComponentInfo info = componentInfoArr[i4];
                    componentInfo.add(new ComponentInfoInternal(info.componentId, info.hardwareVersion, info.firmwareVersion, info.serialNumber, info.softwareVersion));
                    i4++;
                    componentInfoArr = componentInfoArr;
                    length2 = length2;
                    i3 = i3;
                    length = length;
                }
                i = i3;
                i2 = length;
            }
            FingerprintSensorPropertiesInternal internalProp = new FingerprintSensorPropertiesInternal(prop.commonProps.sensorId, prop.commonProps.sensorStrength, prop.commonProps.maxEnrollmentsPerUser, componentInfo, prop.sensorType, prop.halControlsIllumination, true, !workaroundLocations.isEmpty() ? workaroundLocations : (List) Arrays.stream(prop.sensorLocations).map(new Function() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda4
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return FingerprintProvider.lambda$new$0((SensorLocation) obj);
                }
            }).collect(Collectors.toList()));
            Sensor sensor = new Sensor(getTag() + SliceClientPermissions.SliceAuthority.DELIMITER + sensorId, this, this.mContext, this.mHandler, internalProp, lockoutResetDispatcher, gestureAvailabilityDispatcher, this.mBiometricContext);
            this.mSensors.put(sensorId, sensor);
            Slog.d(getTag(), "Added: " + internalProp);
            i3 = i + 1;
            sensorPropsArr = props;
            length = i2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ SensorLocationInternal lambda$new$0(SensorLocation location) {
        return new SensorLocationInternal(location.display, location.sensorLocationX, location.sensorLocationY, location.sensorRadius);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getTag() {
        return "FingerprintProvider/" + this.mHalInstanceName;
    }

    boolean hasHalInstance() {
        return this.mTestHalEnabled || ServiceManager.checkService(new StringBuilder().append(IFingerprint.DESCRIPTOR).append(SliceClientPermissions.SliceAuthority.DELIMITER).append(this.mHalInstanceName).toString()) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized IFingerprint getHalInstance() {
        if (this.mTestHalEnabled) {
            return new TestHal();
        }
        IFingerprint iFingerprint = this.mDaemon;
        if (iFingerprint != null) {
            return iFingerprint;
        }
        Slog.d(getTag(), "Daemon was null, reconnecting");
        IFingerprint asInterface = IFingerprint.Stub.asInterface(Binder.allowBlocking(ServiceManager.waitForDeclaredService(IFingerprint.DESCRIPTOR + SliceClientPermissions.SliceAuthority.DELIMITER + this.mHalInstanceName)));
        this.mDaemon = asInterface;
        if (asInterface == null) {
            Slog.e(getTag(), "Unable to get daemon");
            return null;
        }
        try {
            asInterface.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
            Slog.e(getTag(), "Unable to linkToDeath", e);
        }
        for (int i = 0; i < this.mSensors.size(); i++) {
            int sensorId = this.mSensors.keyAt(i);
            scheduleLoadAuthenticatorIds(sensorId);
            scheduleInternalCleanup(sensorId, ActivityManager.getCurrentUser(), null);
        }
        return this.mDaemon;
    }

    private void scheduleForSensor(int sensorId, BaseClientMonitor client) {
        if (!this.mSensors.contains(sensorId)) {
            throw new IllegalStateException("Unable to schedule client: " + client + " for sensor: " + sensorId);
        }
        this.mSensors.get(sensorId).getScheduler().scheduleClientMonitor(client);
    }

    private void scheduleForSensor(int sensorId, BaseClientMonitor client, ClientMonitorCallback callback) {
        if (!this.mSensors.contains(sensorId)) {
            throw new IllegalStateException("Unable to schedule client: " + client + " for sensor: " + sensorId);
        }
        this.mSensors.get(sensorId).getScheduler().scheduleClientMonitor(client, callback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean containsSensor(int sensorId) {
        return this.mSensors.contains(sensorId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public List<FingerprintSensorPropertiesInternal> getSensorProperties() {
        List<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        for (int i = 0; i < this.mSensors.size(); i++) {
            props.add(this.mSensors.valueAt(i).getSensorProperties());
        }
        return props;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public FingerprintSensorPropertiesInternal getSensorProperties(int sensorId) {
        if (this.mSensors.size() == 0) {
            return null;
        }
        if (sensorId == -1) {
            return this.mSensors.valueAt(0).getSensorProperties();
        }
        Sensor sensor = this.mSensors.get(sensorId);
        if (sensor != null) {
            return sensor.getSensorProperties();
        }
        return null;
    }

    private void scheduleLoadAuthenticatorIds(int sensorId) {
        for (UserInfo user : UserManager.get(this.mContext).getAliveUsers()) {
            scheduleLoadAuthenticatorIdsForUser(sensorId, user.id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleLoadAuthenticatorIdsForUser(final int sensorId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2465x410f8a29(sensorId, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleLoadAuthenticatorIdsForUser$1$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2465x410f8a29(int sensorId, int userId) {
        FingerprintGetAuthenticatorIdClient client = new FingerprintGetAuthenticatorIdClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(0, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleInvalidationRequest(final int sensorId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2464x7f049ba0(userId, sensorId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInvalidationRequest$2$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2464x7f049ba0(int userId, int sensorId) {
        Context context = this.mContext;
        InvalidationRequesterClient<Fingerprint> client = new InvalidationRequesterClient<>(context, userId, sensorId, BiometricLogger.ofUnknown(context), this.mBiometricContext, FingerprintUtils.getInstance(sensorId));
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleResetLockout(final int sensorId, final int userId, final byte[] hardwareAuthToken) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2467x1d2e1dd4(sensorId, userId, hardwareAuthToken);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleResetLockout$3$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2467x1d2e1dd4(int sensorId, int userId, byte[] hardwareAuthToken) {
        FingerprintResetLockoutClient client = new FingerprintResetLockoutClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(0, 0), this.mBiometricContext, hardwareAuthToken, this.mSensors.get(sensorId).getLockoutCache(), this.mLockoutResetDispatcher);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleGenerateChallenge(final int sensorId, final int userId, final IBinder token, final IFingerprintServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2461xe16be161(sensorId, token, receiver, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGenerateChallenge$4$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2461xe16be161(int sensorId, IBinder token, IFingerprintServiceReceiver receiver, int userId, String opPackageName) {
        FingerprintGenerateChallengeClient client = new FingerprintGenerateChallengeClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), userId, opPackageName, sensorId, createLogger(0, 0), this.mBiometricContext);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRevokeChallenge(final int sensorId, final int userId, final IBinder token, final String opPackageName, final long challenge) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2468x88fbda11(sensorId, token, userId, opPackageName, challenge);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRevokeChallenge$5$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2468x88fbda11(int sensorId, IBinder token, int userId, String opPackageName, long challenge) {
        FingerprintRevokeChallengeClient client = new FingerprintRevokeChallengeClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, userId, opPackageName, sensorId, createLogger(0, 0), this.mBiometricContext, challenge);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleEnroll(final int sensorId, final IBinder token, final byte[] hardwareAuthToken, final int userId, final IFingerprintServiceReceiver receiver, final String opPackageName, final int enrollReason) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2459xcebab97f(sensorId, token, id, receiver, userId, hardwareAuthToken, opPackageName, enrollReason);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleEnroll$6$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2459xcebab97f(final int sensorId, IBinder token, long id, IFingerprintServiceReceiver receiver, final int userId, byte[] hardwareAuthToken, String opPackageName, int enrollReason) {
        int maxTemplatesPerUser = this.mSensors.get(sensorId).getSensorProperties().maxEnrollmentsPerUser;
        FingerprintEnrollClient client = new FingerprintEnrollClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, id, new ClientMonitorCallbackConverter(receiver), userId, hardwareAuthToken, opPackageName, FingerprintUtils.getInstance(sensorId), sensorId, createLogger(1, 0), this.mBiometricContext, this.mSensors.get(sensorId).getSensorProperties(), this.mUdfpsOverlayController, this.mSidefpsController, maxTemplatesPerUser, enrollReason);
        scheduleForSensor(sensorId, client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientStarted(BaseClientMonitor clientMonitor) {
                FingerprintProvider.this.mBiometricStateCallback.onClientStarted(clientMonitor);
            }

            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                FingerprintProvider.this.mBiometricStateCallback.onClientFinished(clientMonitor, success);
                if (success) {
                    FingerprintProvider.this.scheduleLoadAuthenticatorIdsForUser(sensorId, userId);
                    FingerprintProvider.this.scheduleInvalidationRequest(sensorId, userId);
                }
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void cancelEnrollment(final int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2452xfd9560bd(sensorId, token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelEnrollment$7$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2452xfd9560bd(int sensorId, IBinder token, long requestId) {
        this.mSensors.get(sensorId).getScheduler().cancelEnrollment(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleFingerDetect(final int sensorId, final IBinder token, final int userId, final ClientMonitorCallbackConverter callback, final String opPackageName, final int statsClient) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2460x43d54a37(sensorId, token, id, callback, userId, opPackageName, statsClient);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleFingerDetect$8$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2460x43d54a37(int sensorId, IBinder token, long id, ClientMonitorCallbackConverter callback, int userId, String opPackageName, int statsClient) {
        boolean isStrongBiometric = Utils.isStrongBiometric(sensorId);
        FingerprintDetectClient client = new FingerprintDetectClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, id, callback, userId, opPackageName, sensorId, createLogger(2, statsClient), this.mBiometricContext, this.mUdfpsOverlayController, isStrongBiometric);
        scheduleForSensor(sensorId, client, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleAuthenticate(final int sensorId, final IBinder token, final long operationId, final int userId, final int cookie, final ClientMonitorCallbackConverter callback, final String opPackageName, final long requestId, final boolean restricted, final int statsClient, final boolean allowBackgroundAuthentication) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2458x169e4ecd(sensorId, token, requestId, callback, userId, operationId, restricted, opPackageName, cookie, statsClient, allowBackgroundAuthentication);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAuthenticate$9$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2458x169e4ecd(int sensorId, IBinder token, long requestId, ClientMonitorCallbackConverter callback, int userId, long operationId, boolean restricted, String opPackageName, int cookie, int statsClient, boolean allowBackgroundAuthentication) {
        boolean isStrongBiometric = Utils.isStrongBiometric(sensorId);
        FingerprintAuthenticationClient client = new FingerprintAuthenticationClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, requestId, callback, userId, operationId, restricted, opPackageName, cookie, false, sensorId, createLogger(2, statsClient), this.mBiometricContext, isStrongBiometric, this.mTaskStackListener, this.mSensors.get(sensorId).getLockoutCache(), this.mUdfpsOverlayController, this.mSidefpsController, allowBackgroundAuthentication, this.mSensors.get(sensorId).getSensorProperties());
        scheduleForSensor(sensorId, client, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleAuthenticate(int sensorId, IBinder token, long operationId, int userId, int cookie, ClientMonitorCallbackConverter callback, String opPackageName, boolean restricted, int statsClient, boolean allowBackgroundAuthentication) {
        long id = this.mRequestCounter.incrementAndGet();
        scheduleAuthenticate(sensorId, token, operationId, userId, cookie, callback, opPackageName, id, restricted, statsClient, allowBackgroundAuthentication);
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startPreparedClient$10$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2471x55880ad5(int sensorId, int cookie) {
        this.mSensors.get(sensorId).getScheduler().startPreparedClient(cookie);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void startPreparedClient(final int sensorId, final int cookie) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2471x55880ad5(sensorId, cookie);
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void cancelAuthentication(final int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2451xeaf851f6(sensorId, token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAuthentication$11$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2451xeaf851f6(int sensorId, IBinder token, long requestId) {
        this.mSensors.get(sensorId).getScheduler().cancelAuthenticationOrDetection(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRemove(int sensorId, IBinder token, IFingerprintServiceReceiver receiver, int fingerId, int userId, String opPackageName) {
        scheduleRemoveSpecifiedIds(sensorId, token, new int[]{fingerId}, userId, receiver, opPackageName);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRemoveAll(int sensorId, IBinder token, IFingerprintServiceReceiver receiver, int userId, String opPackageName) {
        List<Fingerprint> fingers = FingerprintUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
        int[] fingerIds = new int[fingers.size()];
        for (int i = 0; i < fingers.size(); i++) {
            fingerIds[i] = fingers.get(i).getBiometricId();
        }
        scheduleRemoveSpecifiedIds(sensorId, token, fingerIds, userId, receiver, opPackageName);
    }

    private void scheduleRemoveSpecifiedIds(final int sensorId, final IBinder token, final int[] fingerprintIds, final int userId, final IFingerprintServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2466x2e8f2cc6(sensorId, token, receiver, fingerprintIds, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemoveSpecifiedIds$12$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2466x2e8f2cc6(int sensorId, IBinder token, IFingerprintServiceReceiver receiver, int[] fingerprintIds, int userId, String opPackageName) {
        FingerprintRemovalClient client = new FingerprintRemovalClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), fingerprintIds, userId, opPackageName, FingerprintUtils.getInstance(sensorId), sensorId, createLogger(4, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleInternalCleanup(final int sensorId, final int userId, final ClientMonitorCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2462xe0923d18(sensorId, userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInternalCleanup$13$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2462xe0923d18(int sensorId, int userId, ClientMonitorCallback callback) {
        List<Fingerprint> enrolledList = getEnrolledFingerprints(sensorId, userId);
        FingerprintInternalCleanupClient client = new FingerprintInternalCleanupClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(3, 0), this.mBiometricContext, enrolledList, FingerprintUtils.getInstance(sensorId), this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client, new ClientMonitorCompositeCallback(callback, this.mBiometricStateCallback));
    }

    private BiometricLogger createLogger(int statsAction, int statsClient) {
        return new BiometricLogger(this.mContext, 1, statsAction, statsClient);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean isHardwareDetected(int sensorId) {
        return hasHalInstance();
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void rename(int sensorId, int fingerId, int userId, String name) {
        FingerprintUtils.getInstance(sensorId).renameBiometricForUser(this.mContext, userId, fingerId, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public List<Fingerprint> getEnrolledFingerprints(int sensorId, int userId) {
        return FingerprintUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleInvalidateAuthenticatorId(final int sensorId, final int userId, final IInvalidationCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2463xca4d3483(sensorId, userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInvalidateAuthenticatorId$14$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2463xca4d3483(int sensorId, int userId, IInvalidationCallback callback) {
        FingerprintInvalidationClient client = new FingerprintInvalidationClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, sensorId, createLogger(0, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds(), callback);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public int getLockoutModeForUser(int sensorId, int userId) {
        return this.mSensors.get(sensorId).getLockoutCache().getLockoutModeForUser(userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long getAuthenticatorId(int sensorId, int userId) {
        return this.mSensors.get(sensorId).getAuthenticatorIds().getOrDefault(Integer.valueOf(userId), 0L).longValue();
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerDown(long requestId, int sensorId, final int x, final int y, final float minor, final float major) {
        this.mSensors.get(sensorId).getScheduler().getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                FingerprintProvider.this.m2455x10d7726(x, y, minor, major, (BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPointerDown$15$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2455x10d7726(int x, int y, float minor, float major, BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.e(getTag(), "onPointerDown received during client: " + client);
        } else {
            ((Udfps) client).onPointerDown(x, y, minor, major);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerUp(long requestId, int sensorId) {
        this.mSensors.get(sensorId).getScheduler().getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                FingerprintProvider.this.m2456x933492fe((BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPointerUp$16$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2456x933492fe(BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.e(getTag(), "onPointerUp received during client: " + client);
        } else {
            ((Udfps) client).onPointerUp();
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onUiReady(long requestId, int sensorId) {
        this.mSensors.get(sensorId).getScheduler().getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                FingerprintProvider.this.m2457x25f236d4((BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUiReady$17$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2457x25f236d4(BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.e(getTag(), "onUiReady received during client: " + client);
        } else {
            ((Udfps) client).onUiReady();
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void setUdfpsOverlayController(IUdfpsOverlayController controller) {
        this.mUdfpsOverlayController = controller;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void setSidefpsController(ISidefpsController controller) {
        this.mSidefpsController = controller;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void dumpProtoState(int sensorId, ProtoOutputStream proto, boolean clearSchedulerBuffer) {
        if (this.mSensors.contains(sensorId)) {
            this.mSensors.get(sensorId).dumpProtoState(sensorId, proto, clearSchedulerBuffer);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void dumpProtoMetrics(int sensorId, FileDescriptor fd) {
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void dumpInternal(int sensorId, PrintWriter pw) {
        PerformanceTracker performanceTracker = PerformanceTracker.getInstanceForSensorId(sensorId);
        JSONObject dump = new JSONObject();
        try {
            dump.put(HostingRecord.HOSTING_TYPE_SERVICE, getTag());
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int c = FingerprintUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId).size();
                JSONObject set = new JSONObject();
                set.put("id", userId);
                set.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, c);
                set.put("accept", performanceTracker.getAcceptForUser(userId));
                set.put("reject", performanceTracker.getRejectForUser(userId));
                set.put("acquire", performanceTracker.getAcquireForUser(userId));
                set.put("lockout", performanceTracker.getTimedLockoutForUser(userId));
                set.put("permanentLockout", performanceTracker.getPermanentLockoutForUser(userId));
                set.put("acceptCrypto", performanceTracker.getAcceptCryptoForUser(userId));
                set.put("rejectCrypto", performanceTracker.getRejectCryptoForUser(userId));
                set.put("acquireCrypto", performanceTracker.getAcquireCryptoForUser(userId));
                sets.put(set);
            }
            dump.put("prints", sets);
        } catch (JSONException e) {
            Slog.e(getTag(), "dump formatting failure", e);
        }
        pw.println(dump);
        pw.println("HAL deaths since last reboot: " + performanceTracker.getHALDeathCount());
        this.mSensors.get(sensorId).getScheduler().dump(pw);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
        return this.mSensors.get(sensorId).createTestSession(callback, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void setAppBiometricsForUser(final int sensorId, final int fingerId, final int groupId, final String packagename, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2469x589653d(sensorId, fingerId, groupId, packagename, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAppBiometricsForUser$18$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2469x589653d(int sensorId, int fingerId, int groupId, String packagename, int userId) {
        FingerprintUtils.getInstance(sensorId).setAppBiometricsForUser(this.mContext, fingerId, groupId, packagename, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public String getAppPackagenameForUser(int sensorId, int fingerId, int userId) {
        return FingerprintUtils.getInstance(sensorId).getAppPackagenameForUser(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public Fingerprint getAddBiometricForUser(int sensorId, int userId) {
        return FingerprintUtils.getInstance(sensorId).getAddBiometricForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean hasAppPackagename(int sensorId, int userId) {
        return FingerprintUtils.getInstance(sensorId).hasAppPackagename(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean checkName(int sensorId, int userId, String name) {
        return FingerprintUtils.getInstance(sensorId).checkName(this.mContext, userId, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void startAppForFp(final int sensorId, final int fingerId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2470xfb5d8d6(sensorId, fingerId, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startAppForFp$19$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2470xfb5d8d6(int sensorId, int fingerId, int userId) {
        FingerprintUtils.getInstance(sensorId).startAppForFp(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void notifyAppResumeForFp(final int sensorId, final int userId, final String packagename, final String name) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2454x6ea64c94(sensorId, userId, packagename, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAppResumeForFp$20$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2454x6ea64c94(int sensorId, int userId, String packagename, String name) {
        FingerprintUtils.getInstance(sensorId).notifyAppResumeForFp(this.mContext, userId, packagename, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void notifyAppPauseForFp(final int sensorId, final int userId, final String packagename, final String name) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2453x66e16b60(sensorId, userId, packagename, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAppPauseForFp$21$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2453x66e16b60(int sensorId, int userId, String packagename, String name) {
        FingerprintUtils.getInstance(sensorId).notifyAppPauseForFp(this.mContext, userId, packagename, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public int getAppUserIdForUser(int sensorId, int fingerId, int userId) {
        return FingerprintUtils.getInstance(sensorId).getAppUserIdForUser(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onUpdateFocusedApp(int sensorId, String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
        FingerprintUtils.getInstance(sensorId).onUpdateFocusedApp(this.mContext, oldPackageName, oldComponent, newPackageName, newComponent);
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Slog.e(getTag(), "HAL died");
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintProvider$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintProvider.this.m2450x2f789632();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$binderDied$22$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintProvider  reason: not valid java name */
    public /* synthetic */ void m2450x2f789632() {
        this.mDaemon = null;
        for (int i = 0; i < this.mSensors.size(); i++) {
            Sensor sensor = this.mSensors.valueAt(i);
            int sensorId = this.mSensors.keyAt(i);
            PerformanceTracker.getInstanceForSensorId(sensorId).incrementHALDeathCount();
            sensor.onBinderDied();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestHalEnabled(boolean enabled) {
        this.mTestHalEnabled = enabled;
    }

    private List<SensorLocationInternal> getWorkaroundSensorProps(Context context) {
        SensorLocationInternal location;
        List<SensorLocationInternal> sensorLocations = new ArrayList<>();
        TypedArray sfpsProps = context.getResources().obtainTypedArray(17236130);
        for (int i = 0; i < sfpsProps.length(); i++) {
            int id = sfpsProps.getResourceId(i, -1);
            if (id > 0 && (location = parseSensorLocation(context.getResources().obtainTypedArray(id))) != null) {
                sensorLocations.add(location);
            }
        }
        sfpsProps.recycle();
        return sensorLocations;
    }

    private SensorLocationInternal parseSensorLocation(TypedArray array) {
        if (array == null) {
            return null;
        }
        try {
            return new SensorLocationInternal(array.getString(0), array.getInt(1, 0), array.getInt(2, 0), array.getInt(3, 0));
        } catch (Exception e) {
            Slog.w(getTag(), "malformed sensor location", e);
            return null;
        }
    }
}
