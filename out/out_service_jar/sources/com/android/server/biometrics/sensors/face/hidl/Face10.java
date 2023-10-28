package com.android.server.biometrics.sensors.face.hidl;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.UserSwitchObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback;
import android.hardware.face.Face;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceServiceReceiver;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.NativeHandle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.HostingRecord;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.EnumerateConsumer;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.PerformanceTracker;
import com.android.server.biometrics.sensors.RemovalConsumer;
import com.android.server.biometrics.sensors.face.FaceUtils;
import com.android.server.biometrics.sensors.face.LockoutHalImpl;
import com.android.server.biometrics.sensors.face.ServiceProvider;
import com.android.server.biometrics.sensors.face.UsageStats;
import com.android.server.biometrics.sensors.face.hidl.Face10;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class Face10 implements IHwBinder.DeathRecipient, ServiceProvider {
    private static final int ENROLL_TIMEOUT_SEC = 75;
    private static final int GENERATE_CHALLENGE_COUNTER_TTL_MILLIS = 600000;
    private static final int GENERATE_CHALLENGE_REUSE_INTERVAL_MILLIS = 60000;
    private static final String TAG = "Face10";
    public static Clock sSystemClock = Clock.systemUTC();
    private final Map<Integer, Long> mAuthenticatorIds;
    private final BiometricContext mBiometricContext;
    private final Context mContext;
    private IBiometricsFace mDaemon;
    private final HalResultController mHalResultController;
    private final Handler mHandler;
    private final Supplier<IBiometricsFace> mLazyDaemon;
    private final LockoutHalImpl mLockoutTracker;
    private final BiometricScheduler mScheduler;
    private final int mSensorId;
    private final FaceSensorPropertiesInternal mSensorProperties;
    private boolean mTestHalEnabled;
    private final UsageStats mUsageStats;
    private final UserSwitchObserver mUserSwitchObserver;
    private final AtomicLong mRequestCounter = new AtomicLong(0);
    private int mCurrentUserId = -10000;
    private final List<Long> mGeneratedChallengeCount = new ArrayList();
    private FaceGenerateChallengeClient mGeneratedChallengeCache = null;

    /* loaded from: classes.dex */
    public static class HalResultController extends IBiometricsFaceClientCallback.Stub {
        private Callback mCallback;
        private final Context mContext;
        private final Handler mHandler;
        private final LockoutResetDispatcher mLockoutResetDispatcher;
        private final LockoutHalImpl mLockoutTracker;
        private final BiometricScheduler mScheduler;
        private final int mSensorId;

        /* loaded from: classes.dex */
        public interface Callback {
            void onHardwareUnavailable();
        }

        HalResultController(int sensorId, Context context, Handler handler, BiometricScheduler scheduler, LockoutHalImpl lockoutTracker, LockoutResetDispatcher lockoutResetDispatcher) {
            this.mSensorId = sensorId;
            this.mContext = context;
            this.mHandler = handler;
            this.mScheduler = scheduler;
            this.mLockoutTracker = lockoutTracker;
            this.mLockoutResetDispatcher = lockoutResetDispatcher;
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onEnrollResult(final long deviceId, final int faceId, final int userId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2415x6d0eb17(userId, faceId, deviceId, remaining);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollResult$0$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2415x6d0eb17(int userId, int faceId, long deviceId, int remaining) {
            CharSequence name = FaceUtils.getLegacyInstance(this.mSensorId).getUniqueName(this.mContext, userId);
            BiometricAuthenticator.Identifier face = new Face(name, faceId, deviceId);
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceEnrollClient)) {
                Slog.e(Face10.TAG, "onEnrollResult for non-enroll client: " + Utils.getClientName(client));
                return;
            }
            FaceEnrollClient enrollClient = (FaceEnrollClient) client;
            enrollClient.onEnrollResult(face, remaining);
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onAuthenticated(final long deviceId, final int faceId, int userId, final ArrayList<Byte> token) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2414x7f9b01a2(faceId, deviceId, token);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticated$1$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2414x7f9b01a2(int faceId, long deviceId, ArrayList token) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(Face10.TAG, "onAuthenticated for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            boolean authenticated = faceId != 0;
            Face face = new Face("", faceId, deviceId);
            authenticationConsumer.onAuthenticated(face, authenticated, token);
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onAcquired(long deviceId, int userId, final int acquiredInfo, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2413x8fd1abca(acquiredInfo, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAcquired$2$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2413x8fd1abca(int acquiredInfo, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AcquisitionClient)) {
                Slog.e(Face10.TAG, "onAcquired for non-acquire client: " + Utils.getClientName(client));
                return;
            }
            AcquisitionClient<?> acquisitionClient = (AcquisitionClient) client;
            acquisitionClient.onAcquired(acquiredInfo, vendorCode);
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onError(long deviceId, int userId, final int error, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2417x6b274b27(error, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$3$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2417x6b274b27(int error, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            Slog.d(Face10.TAG, "handleError, client: " + (client != null ? client.getOwnerString() : null) + ", error: " + error + ", vendorCode: " + vendorCode);
            if (!(client instanceof ErrorConsumer)) {
                Slog.e(Face10.TAG, "onError for non-error consumer: " + Utils.getClientName(client));
                return;
            }
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(error, vendorCode);
            if (error == 1) {
                Slog.e(Face10.TAG, "Got ERROR_HW_UNAVAILABLE");
                Callback callback = this.mCallback;
                if (callback != null) {
                    callback.onHardwareUnavailable();
                }
            }
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onRemoved(final long deviceId, final ArrayList<Integer> removed, int userId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2419x722bb22e(removed, deviceId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRemoved$4$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2419x722bb22e(ArrayList removed, long deviceId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof RemovalConsumer)) {
                Slog.e(Face10.TAG, "onRemoved for non-removal consumer: " + Utils.getClientName(client));
                return;
            }
            RemovalConsumer removalConsumer = (RemovalConsumer) client;
            if (!removed.isEmpty()) {
                for (int i = 0; i < removed.size(); i++) {
                    int id = ((Integer) removed.get(i)).intValue();
                    Face face = new Face("", id, deviceId);
                    int remaining = (removed.size() - i) - 1;
                    Slog.d(Face10.TAG, "Removed, faceId: " + id + ", remaining: " + remaining);
                    removalConsumer.onRemoved(face, remaining);
                }
            } else {
                removalConsumer.onRemoved(null, 0);
            }
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "face_unlock_re_enroll", 0, -2);
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onEnumerate(final long deviceId, final ArrayList<Integer> faceIds, int userId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2416x53d47909(faceIds, deviceId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnumerate$5$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2416x53d47909(ArrayList faceIds, long deviceId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof EnumerateConsumer)) {
                Slog.e(Face10.TAG, "onEnumerate for non-enumerate consumer: " + Utils.getClientName(client));
                return;
            }
            EnumerateConsumer enumerateConsumer = (EnumerateConsumer) client;
            if (!faceIds.isEmpty()) {
                for (int i = 0; i < faceIds.size(); i++) {
                    Face face = new Face("", ((Integer) faceIds.get(i)).intValue(), deviceId);
                    enumerateConsumer.onEnumerationResult(face, (faceIds.size() - i) - 1);
                }
                return;
            }
            enumerateConsumer.onEnumerationResult(null, 0);
        }

        @Override // android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback
        public void onLockoutChanged(final long duration) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$HalResultController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Face10.HalResultController.this.m2418x95793e83(duration);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutChanged$6$com-android-server-biometrics-sensors-face-hidl-Face10$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2418x95793e83(long duration) {
            int lockoutMode;
            Slog.d(Face10.TAG, "onLockoutChanged: " + duration);
            if (duration == 0) {
                lockoutMode = 0;
            } else if (duration == -1 || duration == JobStatus.NO_LATEST_RUNTIME) {
                lockoutMode = 2;
            } else {
                lockoutMode = 1;
            }
            this.mLockoutTracker.setCurrentUserLockoutMode(lockoutMode);
            if (duration == 0) {
                this.mLockoutResetDispatcher.notifyLockoutResetCallbacks(this.mSensorId);
            }
        }
    }

    Face10(Context context, FaceSensorPropertiesInternal sensorProps, LockoutResetDispatcher lockoutResetDispatcher, Handler handler, BiometricScheduler scheduler, BiometricContext biometricContext) {
        SynchronousUserSwitchObserver synchronousUserSwitchObserver = new SynchronousUserSwitchObserver() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.1
            public void onUserSwitching(int newUserId) {
                Face10.this.scheduleInternalCleanup(newUserId, null);
                Face10 face10 = Face10.this;
                face10.scheduleGetFeature(face10.mSensorId, new Binder(), newUserId, 1, null, Face10.this.mContext.getOpPackageName());
            }
        };
        this.mUserSwitchObserver = synchronousUserSwitchObserver;
        this.mSensorProperties = sensorProps;
        this.mContext = context;
        this.mSensorId = sensorProps.sensorId;
        this.mScheduler = scheduler;
        this.mHandler = handler;
        this.mBiometricContext = biometricContext;
        this.mUsageStats = new UsageStats(context);
        this.mAuthenticatorIds = new HashMap();
        this.mLazyDaemon = new Supplier() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                IBiometricsFace daemon;
                daemon = Face10.this.getDaemon();
                return daemon;
            }
        };
        LockoutHalImpl lockoutHalImpl = new LockoutHalImpl();
        this.mLockoutTracker = lockoutHalImpl;
        HalResultController halResultController = new HalResultController(sensorProps.sensorId, context, handler, scheduler, lockoutHalImpl, lockoutResetDispatcher);
        this.mHalResultController = halResultController;
        halResultController.setCallback(new HalResultController.Callback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda5
            @Override // com.android.server.biometrics.sensors.face.hidl.Face10.HalResultController.Callback
            public final void onHardwareUnavailable() {
                Face10.this.m2399x632ceeab();
            }
        });
        try {
            ActivityManager.getService().registerUserSwitchObserver(synchronousUserSwitchObserver, TAG);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to register user switch observer");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2399x632ceeab() {
        this.mDaemon = null;
        this.mCurrentUserId = -10000;
    }

    public static Face10 newInstance(Context context, FaceSensorPropertiesInternal sensorProps, LockoutResetDispatcher lockoutResetDispatcher) {
        Handler handler = new Handler(Looper.getMainLooper());
        return new Face10(context, sensorProps, lockoutResetDispatcher, handler, new BiometricScheduler(TAG, 1, null), BiometricContext.getInstance(context));
    }

    public void serviceDied(long cookie) {
        Slog.e(TAG, "HAL died");
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2411xaa000c11();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$serviceDied$1$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2411xaa000c11() {
        PerformanceTracker.getInstanceForSensorId(this.mSensorId).incrementHALDeathCount();
        this.mDaemon = null;
        this.mCurrentUserId = -10000;
        BaseClientMonitor client = this.mScheduler.getCurrentClient();
        if (client instanceof ErrorConsumer) {
            Slog.e(TAG, "Sending ERROR_HW_UNAVAILABLE for client: " + client);
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(1, 0);
            FrameworkStatsLog.write(148, 4, 1, -1);
        }
        this.mScheduler.recordCrashState();
        this.mScheduler.reset();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized IBiometricsFace getDaemon() {
        if (this.mTestHalEnabled) {
            TestHal testHal = new TestHal(this.mContext, this.mSensorId);
            testHal.setCallback(this.mHalResultController);
            return testHal;
        }
        IBiometricsFace iBiometricsFace = this.mDaemon;
        if (iBiometricsFace != null) {
            return iBiometricsFace;
        }
        Slog.d(TAG, "Daemon was null, reconnecting, current operation: " + this.mScheduler.getCurrentClient());
        try {
            this.mDaemon = IBiometricsFace.getService();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get face HAL", e);
        } catch (NoSuchElementException e2) {
            Slog.w(TAG, "NoSuchElementException", e2);
        }
        IBiometricsFace iBiometricsFace2 = this.mDaemon;
        if (iBiometricsFace2 == null) {
            Slog.w(TAG, "Face HAL not available");
            return null;
        }
        iBiometricsFace2.asBinder().linkToDeath(this, 0L);
        long halId = 0;
        try {
            halId = this.mDaemon.setCallback(this.mHalResultController).value;
        } catch (RemoteException e3) {
            Slog.e(TAG, "Failed to set callback for face HAL", e3);
            this.mDaemon = null;
        }
        Slog.d(TAG, "Face HAL ready, HAL ID: " + halId);
        if (halId != 0) {
            scheduleLoadAuthenticatorIds();
            scheduleInternalCleanup(ActivityManager.getCurrentUser(), null);
            scheduleGetFeature(this.mSensorId, new Binder(), ActivityManager.getCurrentUser(), 1, null, this.mContext.getOpPackageName());
        } else {
            Slog.e(TAG, "Unable to set callback");
            this.mDaemon = null;
        }
        return this.mDaemon;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public boolean containsSensor(int sensorId) {
        return this.mSensorId == sensorId;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public List<FaceSensorPropertiesInternal> getSensorProperties() {
        List<FaceSensorPropertiesInternal> properties = new ArrayList<>();
        properties.add(this.mSensorProperties);
        return properties;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public FaceSensorPropertiesInternal getSensorProperties(int sensorId) {
        return this.mSensorProperties;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public List<Face> getEnrolledFaces(int sensorId, int userId) {
        return FaceUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public int getLockoutModeForUser(int sensorId, int userId) {
        return this.mLockoutTracker.getLockoutModeForUser(userId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long getAuthenticatorId(int sensorId, int userId) {
        return this.mAuthenticatorIds.getOrDefault(Integer.valueOf(userId), 0L).longValue();
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public boolean isHardwareDetected(int sensorId) {
        return getDaemon() != null;
    }

    private boolean isGeneratedChallengeCacheValid() {
        return this.mGeneratedChallengeCache != null && sSystemClock.millis() - this.mGeneratedChallengeCache.getCreatedAt() < 60000;
    }

    private void incrementChallengeCount() {
        this.mGeneratedChallengeCount.add(0, Long.valueOf(sSystemClock.millis()));
    }

    private int decrementChallengeCount() {
        final long now = sSystemClock.millis();
        this.mGeneratedChallengeCount.removeIf(new Predicate() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda14
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Face10.lambda$decrementChallengeCount$2(now, (Long) obj);
            }
        });
        if (!this.mGeneratedChallengeCount.isEmpty()) {
            this.mGeneratedChallengeCount.remove(0);
        }
        return this.mGeneratedChallengeCount.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$decrementChallengeCount$2(long now, Long x) {
        return now - x.longValue() > 600000;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleGenerateChallenge(int sensorId, final int userId, final IBinder token, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2402xc2395c71(receiver, userId, token, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGenerateChallenge$3$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2402xc2395c71(IFaceServiceReceiver receiver, int userId, IBinder token, String opPackageName) {
        incrementChallengeCount();
        if (!isGeneratedChallengeCacheValid()) {
            scheduleUpdateActiveUserWithoutHandler(userId);
            final FaceGenerateChallengeClient client = new FaceGenerateChallengeClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), userId, opPackageName, this.mSensorId, createLogger(0, 0), this.mBiometricContext, sSystemClock.millis());
            this.mGeneratedChallengeCache = client;
            this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.2
                @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
                public void onClientStarted(BaseClientMonitor clientMonitor) {
                    if (client != clientMonitor) {
                        Slog.e(Face10.TAG, "scheduleGenerateChallenge onClientStarted, mismatched client. Expecting: " + client + ", received: " + clientMonitor);
                    }
                }
            });
            return;
        }
        Slog.d(TAG, "Current challenge is cached and will be reused");
        this.mGeneratedChallengeCache.reuseResult(receiver);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRevokeChallenge(int sensorId, final int userId, final IBinder token, final String opPackageName, long challenge) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2409xb8188d21(token, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRevokeChallenge$4$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2409xb8188d21(IBinder token, int userId, String opPackageName) {
        boolean shouldRevoke = decrementChallengeCount() == 0;
        if (!shouldRevoke) {
            Slog.w(TAG, "scheduleRevokeChallenge skipped - challenge still in use: " + this.mGeneratedChallengeCount);
            return;
        }
        Slog.d(TAG, "scheduleRevokeChallenge executing - no active clients");
        this.mGeneratedChallengeCache = null;
        final FaceRevokeChallengeClient client = new FaceRevokeChallengeClient(this.mContext, this.mLazyDaemon, token, userId, opPackageName, this.mSensorId, createLogger(0, 0), this.mBiometricContext);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.3
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (client != clientMonitor) {
                    Slog.e(Face10.TAG, "scheduleRevokeChallenge, mismatched client.Expecting: " + client + ", received: " + clientMonitor);
                }
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleEnroll(int sensorId, final IBinder token, final byte[] hardwareAuthToken, final int userId, final IFaceServiceReceiver receiver, final String opPackageName, final int[] disabledFeatures, final Surface previewSurface, boolean debugConsent) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2401xef22ef8f(userId, token, receiver, hardwareAuthToken, opPackageName, id, disabledFeatures, previewSurface);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleEnroll$5$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2401xef22ef8f(int userId, IBinder token, IFaceServiceReceiver receiver, byte[] hardwareAuthToken, String opPackageName, long id, int[] disabledFeatures, Surface previewSurface) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        BiometricNotificationUtils.cancelReEnrollNotification(this.mContext);
        final FaceEnrollClient client = new FaceEnrollClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), userId, hardwareAuthToken, opPackageName, id, FaceUtils.getLegacyInstance(this.mSensorId), disabledFeatures, 75, previewSurface, this.mSensorId, createLogger(1, 0), this.mBiometricContext);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.4
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (success) {
                    Face10.this.scheduleUpdateActiveUserWithoutHandler(client.getTargetUserId());
                }
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelEnrollment(int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2398xb6ff21cd(token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelEnrollment$6$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2398xb6ff21cd(IBinder token, long requestId) {
        this.mScheduler.cancelEnrollment(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleFaceDetect(int sensorId, IBinder token, int userId, ClientMonitorCallbackConverter callback, String opPackageName, int statsClient) {
        throw new IllegalStateException("Face detect not supported by IBiometricsFace@1.0. Did youforget to check the supportsFaceDetection flag?");
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelFaceDetect(int sensorId, IBinder token, long requestId) {
        throw new IllegalStateException("Face detect not supported by IBiometricsFace@1.0. Did youforget to check the supportsFaceDetection flag?");
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleAuthenticate(int sensorId, final IBinder token, final long operationId, final int userId, final int cookie, final ClientMonitorCallbackConverter receiver, final String opPackageName, final long requestId, final boolean restricted, final int statsClient, final boolean allowBackgroundAuthentication, final boolean isKeyguardBypassEnabled) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2400x756964fe(userId, token, requestId, receiver, operationId, restricted, opPackageName, cookie, statsClient, allowBackgroundAuthentication, isKeyguardBypassEnabled);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAuthenticate$7$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2400x756964fe(int userId, IBinder token, long requestId, ClientMonitorCallbackConverter receiver, long operationId, boolean restricted, String opPackageName, int cookie, int statsClient, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        boolean isStrongBiometric = Utils.isStrongBiometric(this.mSensorId);
        FaceAuthenticationClient client = new FaceAuthenticationClient(this.mContext, this.mLazyDaemon, token, requestId, receiver, userId, operationId, restricted, opPackageName, cookie, false, this.mSensorId, createLogger(2, statsClient), this.mBiometricContext, isStrongBiometric, this.mLockoutTracker, this.mUsageStats, allowBackgroundAuthentication, isKeyguardBypassEnabled);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public long scheduleAuthenticate(int sensorId, IBinder token, long operationId, int userId, int cookie, ClientMonitorCallbackConverter receiver, String opPackageName, boolean restricted, int statsClient, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        long id = this.mRequestCounter.incrementAndGet();
        scheduleAuthenticate(sensorId, token, operationId, userId, cookie, receiver, opPackageName, id, restricted, statsClient, allowBackgroundAuthentication, isKeyguardBypassEnabled);
        return id;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void cancelAuthentication(int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2397xb7d63617(token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAuthentication$8$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2397xb7d63617(IBinder token, long requestId) {
        this.mScheduler.cancelAuthenticationOrDetection(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRemove(int sensorId, final IBinder token, final int faceId, final int userId, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2406x446640ed(userId, token, receiver, faceId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemove$9$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2406x446640ed(int userId, IBinder token, IFaceServiceReceiver receiver, int faceId, String opPackageName) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        FaceRemovalClient client = new FaceRemovalClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), faceId, userId, opPackageName, FaceUtils.getLegacyInstance(this.mSensorId), this.mSensorId, createLogger(4, 0), this.mBiometricContext, this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleRemoveAll(int sensorId, final IBinder token, final int userId, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2407xa9f476c0(userId, token, receiver, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemoveAll$10$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2407xa9f476c0(int userId, IBinder token, IFaceServiceReceiver receiver, String opPackageName) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        FaceRemovalClient client = new FaceRemovalClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), 0, userId, opPackageName, FaceUtils.getLegacyInstance(this.mSensorId), this.mSensorId, createLogger(4, 0), this.mBiometricContext, this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleResetLockout(final int sensorId, final int userId, final byte[] hardwareAuthToken) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2408xb58021be(sensorId, userId, hardwareAuthToken);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleResetLockout$11$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2408xb58021be(int sensorId, int userId, byte[] hardwareAuthToken) {
        if (getEnrolledFaces(sensorId, userId).isEmpty()) {
            Slog.w(TAG, "Ignoring lockout reset, no templates enrolled for user: " + userId);
            return;
        }
        scheduleUpdateActiveUserWithoutHandler(userId);
        Context context = this.mContext;
        FaceResetLockoutClient client = new FaceResetLockoutClient(context, this.mLazyDaemon, userId, context.getOpPackageName(), this.mSensorId, createLogger(0, 0), this.mBiometricContext, hardwareAuthToken);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleSetFeature(final int sensorId, final IBinder token, final int userId, final int feature, final boolean enabled, final byte[] hardwareAuthToken, final IFaceServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2410x39828bdd(sensorId, userId, token, receiver, opPackageName, feature, enabled, hardwareAuthToken);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSetFeature$12$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2410x39828bdd(int sensorId, int userId, IBinder token, IFaceServiceReceiver receiver, String opPackageName, int feature, boolean enabled, byte[] hardwareAuthToken) {
        List<Face> faces = getEnrolledFaces(sensorId, userId);
        if (faces.isEmpty()) {
            Slog.w(TAG, "Ignoring setFeature, no templates enrolled for user: " + userId);
            return;
        }
        scheduleUpdateActiveUserWithoutHandler(userId);
        int faceId = faces.get(0).getBiometricId();
        FaceSetFeatureClient client = new FaceSetFeatureClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), userId, opPackageName, this.mSensorId, BiometricLogger.ofUnknown(this.mContext), this.mBiometricContext, feature, enabled, hardwareAuthToken, faceId);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleGetFeature(final int sensorId, final IBinder token, final int userId, final int feature, final ClientMonitorCallbackConverter listener, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2403xe5910a48(sensorId, userId, token, listener, opPackageName, feature);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGetFeature$13$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2403xe5910a48(int sensorId, final int userId, IBinder token, ClientMonitorCallbackConverter listener, String opPackageName, final int feature) {
        List<Face> faces = getEnrolledFaces(sensorId, userId);
        if (faces.isEmpty()) {
            Slog.w(TAG, "Ignoring getFeature, no templates enrolled for user: " + userId);
            return;
        }
        scheduleUpdateActiveUserWithoutHandler(userId);
        int faceId = faces.get(0).getBiometricId();
        Context context = this.mContext;
        final FaceGetFeatureClient client = new FaceGetFeatureClient(context, this.mLazyDaemon, token, listener, userId, opPackageName, this.mSensorId, BiometricLogger.ofUnknown(context), this.mBiometricContext, feature, faceId);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.5
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (success && feature == 1) {
                    boolean value = client.getValue();
                    StringBuilder append = new StringBuilder().append("Updating attention value for user: ").append(userId).append(" to value: ");
                    int settingsValue = value ? 1 : 0;
                    Slog.d(Face10.TAG, append.append(settingsValue).toString());
                    ContentResolver contentResolver = Face10.this.mContext.getContentResolver();
                    int i = userId;
                    int settingsValue2 = value ? 1 : 0;
                    Settings.Secure.putIntForUser(contentResolver, "face_unlock_attention_required", settingsValue2, i);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleInternalCleanup(final int userId, final ClientMonitorCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2404x3c201f66(userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInternalCleanup$14$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2404x3c201f66(int userId, ClientMonitorCallback callback) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        List<Face> enrolledList = getEnrolledFaces(this.mSensorId, userId);
        Context context = this.mContext;
        FaceInternalCleanupClient client = new FaceInternalCleanupClient(context, this.mLazyDaemon, userId, context.getOpPackageName(), this.mSensorId, createLogger(3, 0), this.mBiometricContext, enrolledList, FaceUtils.getLegacyInstance(this.mSensorId), this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client, callback);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void scheduleInternalCleanup(int sensorId, int userId, ClientMonitorCallback callback) {
        scheduleInternalCleanup(userId, callback);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void startPreparedClient(int sensorId, final int cookie) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2412x91afb91f(cookie);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startPreparedClient$15$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2412x91afb91f(int cookie) {
        this.mScheduler.startPreparedClient(cookie);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpProtoState(int sensorId, ProtoOutputStream proto, boolean clearSchedulerBuffer) {
        long sensorToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        proto.write(CompanionMessage.MESSAGE_ID, this.mSensorProperties.sensorId);
        proto.write(CompanionMessage.TYPE, 2);
        proto.write(1120986464259L, Utils.getCurrentStrength(this.mSensorProperties.sensorId));
        proto.write(1146756268036L, this.mScheduler.dumpProtoState(clearSchedulerBuffer));
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(2246267895813L);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            proto.write(1120986464258L, FaceUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId).size());
            proto.end(userToken);
        }
        proto.write(1133871366150L, this.mSensorProperties.resetLockoutRequiresHardwareAuthToken);
        proto.write(1133871366151L, this.mSensorProperties.resetLockoutRequiresChallenge);
        proto.end(sensorToken);
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpProtoMetrics(int sensorId, FileDescriptor fd) {
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpInternal(int sensorId, PrintWriter pw) {
        PerformanceTracker performanceTracker = PerformanceTracker.getInstanceForSensorId(this.mSensorId);
        JSONObject dump = new JSONObject();
        try {
            dump.put(HostingRecord.HOSTING_TYPE_SERVICE, TAG);
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int c = FaceUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId).size();
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
            Slog.e(TAG, "dump formatting failure", e);
        }
        pw.println(dump);
        pw.println("HAL deaths since last reboot: " + performanceTracker.getHALDeathCount());
        this.mScheduler.dump(pw);
        this.mUsageStats.print(pw);
    }

    private void scheduleLoadAuthenticatorIds() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                Face10.this.m2405x50d764b0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleLoadAuthenticatorIds$16$com-android-server-biometrics-sensors-face-hidl-Face10  reason: not valid java name */
    public /* synthetic */ void m2405x50d764b0() {
        for (UserInfo user : UserManager.get(this.mContext).getAliveUsers()) {
            int targetUserId = user.id;
            if (!this.mAuthenticatorIds.containsKey(Integer.valueOf(targetUserId))) {
                scheduleUpdateActiveUserWithoutHandler(targetUserId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleUpdateActiveUserWithoutHandler(final int targetUserId) {
        boolean hasEnrolled = !getEnrolledFaces(this.mSensorId, targetUserId).isEmpty();
        Context context = this.mContext;
        FaceUpdateActiveUserClient client = new FaceUpdateActiveUserClient(context, this.mLazyDaemon, targetUserId, context.getOpPackageName(), this.mSensorId, createLogger(0, 0), this.mBiometricContext, hasEnrolled, this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.Face10.6
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (success) {
                    Face10.this.mCurrentUserId = targetUserId;
                    return;
                }
                Slog.w(Face10.TAG, "Failed to change user, still: " + Face10.this.mCurrentUserId);
            }
        });
    }

    private BiometricLogger createLogger(int statsAction, int statsClient) {
        return new BiometricLogger(this.mContext, 4, statsAction, statsClient);
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:25:0x0062 -> B:36:0x006c). Please submit an issue!!! */
    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public void dumpHal(int sensorId, FileDescriptor fd, String[] args) {
        IBiometricsFace daemon;
        if ((Build.IS_ENG || Build.IS_USERDEBUG) && !SystemProperties.getBoolean("ro.face.disable_debug_data", false) && !SystemProperties.getBoolean("persist.face.disable_debug_data", false) && (daemon = getDaemon()) != null) {
            FileOutputStream devnull = null;
            try {
                try {
                    try {
                        devnull = new FileOutputStream("/dev/null");
                        NativeHandle handle = new NativeHandle(new FileDescriptor[]{devnull.getFD(), fd}, new int[0], false);
                        daemon.debug(handle, new ArrayList<>(Arrays.asList(args)));
                        devnull.close();
                    } catch (RemoteException | IOException ex) {
                        Slog.d(TAG, "error while reading face debugging data", ex);
                        if (devnull != null) {
                            devnull.close();
                        }
                    }
                } catch (Throwable th) {
                    if (devnull != null) {
                        try {
                            devnull.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestHalEnabled(boolean enabled) {
        this.mTestHalEnabled = enabled;
    }

    @Override // com.android.server.biometrics.sensors.face.ServiceProvider
    public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
        return new BiometricTestSessionImpl(this.mContext, this.mSensorId, callback, this, this.mHalResultController);
    }
}
