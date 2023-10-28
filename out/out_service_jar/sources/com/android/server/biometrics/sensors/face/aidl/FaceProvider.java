package com.android.server.biometrics.sensors.face.aidl;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.common.ComponentInfo;
import android.hardware.biometrics.face.IFace;
import android.hardware.biometrics.face.SensorProps;
import android.hardware.face.Face;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceServiceReceiver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.HostingRecord;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.InvalidationRequesterClient;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.PerformanceTracker;
import com.android.server.biometrics.sensors.face.FaceUtils;
import com.android.server.biometrics.sensors.face.ServiceProvider;
import com.android.server.biometrics.sensors.face.UsageStats;
import com.android.server.biometrics.sensors.face.aidl.FaceProvider;
import com.android.server.slice.SliceClientPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class FaceProvider implements IBinder.DeathRecipient, ServiceProvider {
    private static final int ENROLL_TIMEOUT_SEC = 75;
    private final BiometricContext mBiometricContext;
    private final Context mContext;
    private IFace mDaemon;
    private final String mHalInstanceName;
    private final LockoutResetDispatcher mLockoutResetDispatcher;
    private boolean mTestHalEnabled;
    private final UsageStats mUsageStats;
    private final AtomicLong mRequestCounter = new AtomicLong(0);
    final SparseArray<Sensor> mSensors = new SparseArray<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ActivityTaskManager mActivityTaskManager = ActivityTaskManager.getInstance();
    private final BiometricTaskStackListener mTaskStackListener = new BiometricTaskStackListener();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BiometricTaskStackListener extends TaskStackListener {
        private BiometricTaskStackListener() {
        }

        public void onTaskStackChanged() {
            FaceProvider.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$BiometricTaskStackListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    FaceProvider.BiometricTaskStackListener.this.m2356x49077757();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskStackChanged$0$com-android-server-biometrics-sensors-face-aidl-FaceProvider$BiometricTaskStackListener  reason: not valid java name */
        public /* synthetic */ void m2356x49077757() {
            for (int i = 0; i < FaceProvider.this.mSensors.size(); i++) {
                BaseClientMonitor client = FaceProvider.this.mSensors.valueAt(i).getScheduler().getCurrentClient();
                if (!(client instanceof AuthenticationClient)) {
                    Slog.e(FaceProvider.this.getTag(), "Task stack changed for client: " + client);
                } else if (!Utils.isKeyguard(FaceProvider.this.mContext, client.getOwnerString()) && !Utils.isSystem(FaceProvider.this.mContext, client.getOwnerString()) && Utils.isBackground(client.getOwnerString()) && !client.isAlreadyDone()) {
                    Slog.e(FaceProvider.this.getTag(), "Stopping background authentication, currentClient: " + client);
                    FaceProvider.this.mSensors.valueAt(i).getScheduler().cancelAuthenticationOrDetection(client.getToken(), client.getRequestId());
                }
            }
        }
    }

    public FaceProvider(Context context, SensorProps[] props, String halInstanceName, LockoutResetDispatcher lockoutResetDispatcher, BiometricContext biometricContext) {
        SensorProps[] sensorPropsArr = props;
        this.mContext = context;
        this.mHalInstanceName = halInstanceName;
        this.mUsageStats = new UsageStats(context);
        this.mLockoutResetDispatcher = lockoutResetDispatcher;
        this.mBiometricContext = biometricContext;
        int length = sensorPropsArr.length;
        int i = 0;
        while (i < length) {
            SensorProps prop = sensorPropsArr[i];
            int sensorId = prop.commonProps.sensorId;
            List<ComponentInfoInternal> componentInfo = new ArrayList<>();
            if (prop.commonProps.componentInfo != null) {
                ComponentInfo[] componentInfoArr = prop.commonProps.componentInfo;
                int length2 = componentInfoArr.length;
                int i2 = 0;
                while (i2 < length2) {
                    ComponentInfo info = componentInfoArr[i2];
                    componentInfo.add(new ComponentInfoInternal(info.componentId, info.hardwareVersion, info.firmwareVersion, info.serialNumber, info.softwareVersion));
                    i2++;
                    componentInfoArr = componentInfoArr;
                    length2 = length2;
                    i = i;
                }
            }
            FaceSensorPropertiesInternal internalProp = new FaceSensorPropertiesInternal(prop.commonProps.sensorId, prop.commonProps.sensorStrength, prop.commonProps.maxEnrollmentsPerUser, componentInfo, prop.sensorType, prop.supportsDetectInteraction, prop.halControlsPreview, false);
            Sensor sensor = new Sensor(getTag() + SliceClientPermissions.SliceAuthority.DELIMITER + sensorId, this, this.mContext, this.mHandler, internalProp, lockoutResetDispatcher, this.mBiometricContext);
            this.mSensors.put(sensorId, sensor);
            Slog.d(getTag(), "Added: " + internalProp);
            i++;
            sensorPropsArr = props;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getTag() {
        return "FaceProvider/" + this.mHalInstanceName;
    }

    boolean hasHalInstance() {
        return this.mTestHalEnabled || ServiceManager.checkService(new StringBuilder().append(IFace.DESCRIPTOR).append(SliceClientPermissions.SliceAuthority.DELIMITER).append(this.mHalInstanceName).toString()) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized IFace getHalInstance() {
        if (this.mTestHalEnabled) {
            return new TestHal();
        }
        IFace iFace = this.mDaemon;
        if (iFace != null) {
            return iFace;
        }
        Slog.d(getTag(), "Daemon was null, reconnecting");
        IFace asInterface = IFace.Stub.asInterface(Binder.allowBlocking(ServiceManager.waitForDeclaredService(IFace.DESCRIPTOR + SliceClientPermissions.SliceAuthority.DELIMITER + this.mHalInstanceName)));
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

    private void scheduleLoadAuthenticatorIds(int sensorId) {
        for (UserInfo user : UserManager.get(this.mContext).getAliveUsers()) {
            scheduleLoadAuthenticatorIdsForUser(sensorId, user.id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleLoadAuthenticatorIdsForUser(final int sensorId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2350xe12c4fa4(sensorId, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleLoadAuthenticatorIdsForUser$0$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2350xe12c4fa4(int sensorId, int userId) {
        FaceGetAuthenticatorIdClient client = new FaceGetAuthenticatorIdClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(0, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleInvalidationRequest(final int sensorId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2349x2c0f2d5b(userId, sensorId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInvalidationRequest$1$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2349x2c0f2d5b(int userId, int sensorId) {
        Context context = this.mContext;
        InvalidationRequesterClient<Face> client = new InvalidationRequesterClient<>(context, userId, sensorId, BiometricLogger.ofUnknown(context), this.mBiometricContext, FaceUtils.getInstance(sensorId));
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public boolean containsSensor(int sensorId) {
        return this.mSensors.contains(sensorId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public List<FaceSensorPropertiesInternal> getSensorProperties() {
        List<FaceSensorPropertiesInternal> props = new ArrayList<>();
        for (int i = 0; i < this.mSensors.size(); i++) {
            props.add(this.mSensors.valueAt(i).getSensorProperties());
        }
        return props;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public FaceSensorPropertiesInternal getSensorProperties(int sensorId) {
        return this.mSensors.get(sensorId).getSensorProperties();
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public List<Face> getEnrolledFaces(int sensorId, int userId) {
        return FaceUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleInvalidateAuthenticatorId(final int sensorId, final int userId, final IInvalidationCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2348xf5f823d8(sensorId, userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInvalidateAuthenticatorId$2$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2348xf5f823d8(int sensorId, int userId, IInvalidationCallback callback) {
        FaceInvalidationClient client = new FaceInvalidationClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, sensorId, createLogger(0, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds(), callback);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public int getLockoutModeForUser(int sensorId, int userId) {
        return this.mSensors.get(sensorId).getLockoutCache().getLockoutModeForUser(userId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long getAuthenticatorId(int sensorId, int userId) {
        return this.mSensors.get(sensorId).getAuthenticatorIds().getOrDefault(Integer.valueOf(userId), 0L).longValue();
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public boolean isHardwareDetected(int sensorId) {
        return hasHalInstance();
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleGenerateChallenge(final int sensorId, final int userId, final IBinder token, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2345x517928dc(sensorId, token, receiver, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGenerateChallenge$3$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2345x517928dc(int sensorId, IBinder token, IFaceServiceReceiver receiver, int userId, String opPackageName) {
        FaceGenerateChallengeClient client = new FaceGenerateChallengeClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), userId, opPackageName, sensorId, createLogger(0, 0), this.mBiometricContext);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRevokeChallenge(final int sensorId, final int userId, final IBinder token, final String opPackageName, final long challenge) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2353xad7d158c(sensorId, token, userId, opPackageName, challenge);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRevokeChallenge$4$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2353xad7d158c(int sensorId, IBinder token, int userId, String opPackageName, long challenge) {
        FaceRevokeChallengeClient client = new FaceRevokeChallengeClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, userId, opPackageName, sensorId, createLogger(0, 0), this.mBiometricContext, challenge);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleEnroll(final int sensorId, final IBinder token, final byte[] hardwareAuthToken, final int userId, final IFaceServiceReceiver receiver, final String opPackageName, final int[] disabledFeatures, final Surface previewSurface, final boolean debugConsent) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2343xd2a36d7a(sensorId, token, receiver, userId, hardwareAuthToken, opPackageName, id, disabledFeatures, previewSurface, debugConsent);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleEnroll$5$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2343xd2a36d7a(final int sensorId, IBinder token, IFaceServiceReceiver receiver, final int userId, byte[] hardwareAuthToken, String opPackageName, long id, int[] disabledFeatures, Surface previewSurface, boolean debugConsent) {
        int maxTemplatesPerUser = this.mSensors.get(sensorId).getSensorProperties().maxEnrollmentsPerUser;
        FaceEnrollClient client = new FaceEnrollClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), userId, hardwareAuthToken, opPackageName, id, FaceUtils.getInstance(sensorId), disabledFeatures, 75, previewSurface, sensorId, createLogger(1, 0), this.mBiometricContext, maxTemplatesPerUser, debugConsent);
        scheduleForSensor(sensorId, client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (success) {
                    FaceProvider.this.scheduleLoadAuthenticatorIdsForUser(sensorId, userId);
                    FaceProvider.this.scheduleInvalidationRequest(sensorId, userId);
                }
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelEnrollment(final int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2340xcd47938(sensorId, token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelEnrollment$6$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2340xcd47938(int sensorId, IBinder token, long requestId) {
        this.mSensors.get(sensorId).getScheduler().cancelEnrollment(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleFaceDetect(final int sensorId, final IBinder token, final int userId, final ClientMonitorCallbackConverter callback, final String opPackageName, final int statsClient) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2344xb024d8be(sensorId, token, id, callback, userId, opPackageName, statsClient);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleFaceDetect$7$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2344xb024d8be(int sensorId, IBinder token, long id, ClientMonitorCallbackConverter callback, int userId, String opPackageName, int statsClient) {
        boolean isStrongBiometric = Utils.isStrongBiometric(sensorId);
        FaceDetectClient client = new FaceDetectClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, id, callback, userId, opPackageName, sensorId, createLogger(2, statsClient), this.mBiometricContext, isStrongBiometric);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelFaceDetect(final int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2341x858eb09a(sensorId, token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelFaceDetect$8$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2341x858eb09a(int sensorId, IBinder token, long requestId) {
        this.mSensors.get(sensorId).getScheduler().cancelAuthenticationOrDetection(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleAuthenticate(final int sensorId, final IBinder token, final long operationId, final int userId, final int cookie, final ClientMonitorCallbackConverter callback, final String opPackageName, final long requestId, final boolean restricted, final int statsClient, final boolean allowBackgroundAuthentication, final boolean isKeyguardBypassEnabled) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2342x5dceb2e7(sensorId, token, requestId, callback, userId, operationId, restricted, opPackageName, cookie, statsClient, allowBackgroundAuthentication, isKeyguardBypassEnabled);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAuthenticate$9$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2342x5dceb2e7(int sensorId, IBinder token, long requestId, ClientMonitorCallbackConverter callback, int userId, long operationId, boolean restricted, String opPackageName, int cookie, int statsClient, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        boolean isStrongBiometric = Utils.isStrongBiometric(sensorId);
        FaceAuthenticationClient client = new FaceAuthenticationClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, requestId, callback, userId, operationId, restricted, opPackageName, cookie, false, sensorId, createLogger(2, statsClient), this.mBiometricContext, isStrongBiometric, this.mUsageStats, this.mSensors.get(sensorId).getLockoutCache(), allowBackgroundAuthentication, isKeyguardBypassEnabled);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleAuthenticate(int sensorId, IBinder token, long operationId, int userId, int cookie, ClientMonitorCallbackConverter callback, String opPackageName, boolean restricted, int statsClient, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        long id = this.mRequestCounter.incrementAndGet();
        scheduleAuthenticate(sensorId, token, operationId, userId, cookie, callback, opPackageName, id, restricted, statsClient, allowBackgroundAuthentication, isKeyguardBypassEnabled);
        return id;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelAuthentication(final int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2339xe0371231(sensorId, token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAuthentication$10$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2339xe0371231(int sensorId, IBinder token, long requestId) {
        this.mSensors.get(sensorId).getScheduler().cancelAuthenticationOrDetection(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRemove(int sensorId, IBinder token, int faceId, int userId, IFaceServiceReceiver receiver, String opPackageName) {
        scheduleRemoveSpecifiedIds(sensorId, token, new int[]{faceId}, userId, receiver, opPackageName);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRemoveAll(int sensorId, IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) {
        List<Face> faces = FaceUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
        int[] faceIds = new int[faces.size()];
        for (int i = 0; i < faces.size(); i++) {
            faceIds[i] = faces.get(i).getBiometricId();
        }
        scheduleRemoveSpecifiedIds(sensorId, token, faceIds, userId, receiver, opPackageName);
    }

    private void scheduleRemoveSpecifiedIds(final int sensorId, final IBinder token, final int[] faceIds, final int userId, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2351x9a4a1901(sensorId, token, receiver, faceIds, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemoveSpecifiedIds$11$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2351x9a4a1901(int sensorId, IBinder token, IFaceServiceReceiver receiver, int[] faceIds, int userId, String opPackageName) {
        FaceRemovalClient client = new FaceRemovalClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), faceIds, userId, opPackageName, FaceUtils.getInstance(sensorId), sensorId, createLogger(4, 0), this.mBiometricContext, this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleResetLockout(final int sensorId, final int userId, final byte[] hardwareAuthToken) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2352xe1567008(sensorId, userId, hardwareAuthToken);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleResetLockout$12$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2352xe1567008(int sensorId, int userId, byte[] hardwareAuthToken) {
        FaceResetLockoutClient client = new FaceResetLockoutClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(0, 0), this.mBiometricContext, hardwareAuthToken, this.mSensors.get(sensorId).getLockoutCache(), this.mLockoutResetDispatcher);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleSetFeature(final int sensorId, final IBinder token, final int userId, final int feature, final boolean enabled, final byte[] hardwareAuthToken, final IFaceServiceReceiver receiver, String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2354x984286e7(sensorId, userId, token, receiver, feature, enabled, hardwareAuthToken);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSetFeature$13$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2354x984286e7(int sensorId, int userId, IBinder token, IFaceServiceReceiver receiver, int feature, boolean enabled, byte[] hardwareAuthToken) {
        List<Face> faces = FaceUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
        if (faces.isEmpty()) {
            Slog.w(getTag(), "Ignoring setFeature, no templates enrolled for user: " + userId);
            return;
        }
        FaceSetFeatureClient client = new FaceSetFeatureClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, new ClientMonitorCallbackConverter(receiver), userId, this.mContext.getOpPackageName(), sensorId, BiometricLogger.ofUnknown(this.mContext), this.mBiometricContext, feature, enabled, hardwareAuthToken);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleGetFeature(final int sensorId, final IBinder token, final int userId, int feature, final ClientMonitorCallbackConverter callback, String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2346xef354912(sensorId, userId, token, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGetFeature$14$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2346xef354912(int sensorId, int userId, IBinder token, ClientMonitorCallbackConverter callback) {
        List<Face> faces = FaceUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId);
        if (faces.isEmpty()) {
            Slog.w(getTag(), "Ignoring getFeature, no templates enrolled for user: " + userId);
            return;
        }
        FaceGetFeatureClient client = new FaceGetFeatureClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), token, callback, userId, this.mContext.getOpPackageName(), sensorId, BiometricLogger.ofUnknown(this.mContext), this.mBiometricContext);
        scheduleForSensor(sensorId, client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void startPreparedClient(final int sensorId, final int cookie) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2355xe236cb0a(sensorId, cookie);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startPreparedClient$15$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2355xe236cb0a(int sensorId, int cookie) {
        this.mSensors.get(sensorId).getScheduler().startPreparedClient(cookie);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleInternalCleanup(final int sensorId, final int userId, final ClientMonitorCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2347x8899ff4f(sensorId, userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInternalCleanup$16$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2347x8899ff4f(int sensorId, int userId, ClientMonitorCallback callback) {
        List<Face> enrolledList = getEnrolledFaces(sensorId, userId);
        FaceInternalCleanupClient client = new FaceInternalCleanupClient(this.mContext, this.mSensors.get(sensorId).getLazySession(), userId, this.mContext.getOpPackageName(), sensorId, createLogger(3, 0), this.mBiometricContext, enrolledList, FaceUtils.getInstance(sensorId), this.mSensors.get(sensorId).getAuthenticatorIds());
        scheduleForSensor(sensorId, client, callback);
    }

    private BiometricLogger createLogger(int statsAction, int statsClient) {
        return new BiometricLogger(this.mContext, 4, statsAction, statsClient);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpProtoState(int sensorId, ProtoOutputStream proto, boolean clearSchedulerBuffer) {
        if (this.mSensors.contains(sensorId)) {
            this.mSensors.get(sensorId).dumpProtoState(sensorId, proto, clearSchedulerBuffer);
        }
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpProtoMetrics(int sensorId, FileDescriptor fd) {
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpInternal(int sensorId, PrintWriter pw) {
        PerformanceTracker performanceTracker = PerformanceTracker.getInstanceForSensorId(sensorId);
        JSONObject dump = new JSONObject();
        try {
            dump.put(HostingRecord.HOSTING_TYPE_SERVICE, getTag());
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int c = FaceUtils.getInstance(sensorId).getBiometricsForUser(this.mContext, userId).size();
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
        this.mUsageStats.print(pw);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
        return this.mSensors.get(sensorId).createTestSession(callback);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpHal(int sensorId, FileDescriptor fd, String[] args) {
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Slog.e(getTag(), "HAL died");
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceProvider$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                FaceProvider.this.m2338xb9a848e6();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$binderDied$17$com-android-server-biometrics-sensors-face-aidl-FaceProvider  reason: not valid java name */
    public /* synthetic */ void m2338xb9a848e6() {
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
}
