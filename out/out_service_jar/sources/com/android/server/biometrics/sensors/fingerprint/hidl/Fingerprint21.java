package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.TaskStackListener;
import android.app.UserSwitchObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.HostingRecord;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnumerateConsumer;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.PerformanceTracker;
import com.android.server.biometrics.sensors.RemovalConsumer;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
import com.android.server.biometrics.sensors.fingerprint.ServiceProvider;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21;
import com.android.server.biometrics.sensors.fingerprint.hidl.LockoutFrameworkImpl;
import com.android.server.pm.PackageManagerService;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class Fingerprint21 implements IHwBinder.DeathRecipient, ServiceProvider {
    private static final int ENROLL_TIMEOUT_SEC = 60;
    private static final String TAG = "Fingerprint21";
    private final ActivityTaskManager mActivityTaskManager;
    private final Map<Integer, Long> mAuthenticatorIds;
    private final BiometricContext mBiometricContext;
    private final BiometricStateCallback mBiometricStateCallback;
    final Context mContext;
    private IBiometricsFingerprint mDaemon;
    private final HalResultController mHalResultController;
    private final Handler mHandler;
    private final boolean mIsPowerbuttonFps;
    private final boolean mIsUdfps;
    private final Supplier<IBiometricsFingerprint> mLazyDaemon;
    private final LockoutFrameworkImpl.LockoutResetCallback mLockoutResetCallback;
    private final LockoutResetDispatcher mLockoutResetDispatcher;
    private final LockoutFrameworkImpl mLockoutTracker;
    private final BiometricScheduler mScheduler;
    private final int mSensorId;
    private final FingerprintSensorPropertiesInternal mSensorProperties;
    private ISidefpsController mSidefpsController;
    private final BiometricTaskStackListener mTaskStackListener;
    private boolean mTestHalEnabled;
    private IUdfpsOverlayController mUdfpsOverlayController;
    private final UserSwitchObserver mUserSwitchObserver;
    private final AtomicLong mRequestCounter = new AtomicLong(0);
    private int mCurrentUserId = -10000;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BiometricTaskStackListener extends TaskStackListener {
        private BiometricTaskStackListener() {
        }

        public void onTaskStackChanged() {
            Fingerprint21.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$BiometricTaskStackListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.BiometricTaskStackListener.this.m2533x4d563c4a();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskStackChanged$0$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$BiometricTaskStackListener  reason: not valid java name */
        public /* synthetic */ void m2533x4d563c4a() {
            BaseClientMonitor client = Fingerprint21.this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationClient)) {
                Slog.e(Fingerprint21.TAG, "Task stack changed for client: " + client);
            } else if (!Utils.isKeyguard(Fingerprint21.this.mContext, client.getOwnerString()) && !Utils.isSystem(Fingerprint21.this.mContext, client.getOwnerString()) && Utils.isBackground(client.getOwnerString()) && !client.isAlreadyDone()) {
                Slog.e(Fingerprint21.TAG, "Stopping background authentication, currentClient: " + client);
                Fingerprint21.this.mScheduler.cancelAuthenticationOrDetection(client.getToken(), client.getRequestId());
            }
        }
    }

    private boolean isLauncher(String clientPackage, String packageName) {
        return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(clientPackage) && packageName != null && (packageName.contains("launcher") || packageName.contains("Launcher"));
    }

    /* loaded from: classes.dex */
    public static class HalResultController extends IBiometricsFingerprintClientCallback.Stub {
        private Callback mCallback;
        private final Context mContext;
        final Handler mHandler;
        final BiometricScheduler mScheduler;
        private final int mSensorId;

        /* loaded from: classes.dex */
        public interface Callback {
            void onHardwareUnavailable();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public HalResultController(int sensorId, Context context, Handler handler, BiometricScheduler scheduler) {
            this.mSensorId = sensorId;
            this.mContext = context;
            this.mHandler = handler;
            this.mScheduler = scheduler;
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onEnrollResult(final long deviceId, final int fingerId, final int groupId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2536x2896cdef(groupId, fingerId, deviceId, remaining);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollResult$0$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2536x2896cdef(int groupId, int fingerId, long deviceId, int remaining) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintEnrollClient)) {
                Slog.e(Fingerprint21.TAG, "onEnrollResult for non-enroll client: " + Utils.getClientName(client));
                return;
            }
            int currentUserId = client.getTargetUserId();
            CharSequence name = FingerprintUtils.getLegacyInstance(this.mSensorId).getUniqueName(this.mContext, currentUserId);
            FingerprintEnrollClient enrollClient = (FingerprintEnrollClient) client;
            enrollClient.onEnrollResult(new Fingerprint(name, groupId, fingerId, deviceId), remaining);
            ITranFingerprintService.Instance().onEnrollResult(remaining);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onAcquired(long deviceId, int acquiredInfo, int vendorCode) {
            onAcquired_2_2(deviceId, acquiredInfo, vendorCode);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback
        public void onAcquired_2_2(long deviceId, final int acquiredInfo, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2534xb3b894dd(acquiredInfo, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAcquired_2_2$1$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2534xb3b894dd(int acquiredInfo, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AcquisitionClient)) {
                Slog.e(Fingerprint21.TAG, "onAcquired for non-acquisition client: " + Utils.getClientName(client));
                return;
            }
            ITranFingerprintService.Instance().onAcquireHook(client.getOwnerString(), acquiredInfo, vendorCode);
            AcquisitionClient<?> acquisitionClient = (AcquisitionClient) client;
            acquisitionClient.onAcquired(acquiredInfo, vendorCode);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onAuthenticated(final long deviceId, final int fingerId, final int groupId, final ArrayList<Byte> token) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2535x5515f059(fingerId, groupId, deviceId, token);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticated$2$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2535x5515f059(int fingerId, int groupId, long deviceId, ArrayList token) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(Fingerprint21.TAG, "onAuthenticated for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            ITranFingerprintService.Instance().onAuthenticatedHook(client.getOwnerString(), fingerId != 0);
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            boolean authenticated = fingerId != 0;
            Fingerprint fp = new Fingerprint("", groupId, fingerId, deviceId);
            authenticationConsumer.onAuthenticated(fp, authenticated, token);
            ITranFingerprintService.Instance().onAuthenticated(authenticated);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onError(long deviceId, final int error, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2538x23fbd1ff(error, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$3$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2538x23fbd1ff(int error, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            Slog.d(Fingerprint21.TAG, "handleError, client: " + Utils.getClientName(client) + ", error: " + error + ", vendorCode: " + vendorCode);
            if (!(client instanceof ErrorConsumer)) {
                Slog.e(Fingerprint21.TAG, "onError for non-error consumer: " + Utils.getClientName(client));
                return;
            }
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(error, vendorCode);
            if (error == 1) {
                Slog.e(Fingerprint21.TAG, "Got ERROR_HW_UNAVAILABLE");
                Callback callback = this.mCallback;
                if (callback != null) {
                    callback.onHardwareUnavailable();
                }
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onRemoved(final long deviceId, final int fingerId, final int groupId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2539x50bfe0c6(fingerId, remaining, groupId, deviceId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRemoved$4$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2539x50bfe0c6(int fingerId, int remaining, int groupId, long deviceId) {
            Slog.d(Fingerprint21.TAG, "Removed, fingerId: " + fingerId + ", remaining: " + remaining);
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof RemovalConsumer)) {
                Slog.e(Fingerprint21.TAG, "onRemoved for non-removal consumer: " + Utils.getClientName(client));
                return;
            }
            RemovalConsumer removalConsumer = (RemovalConsumer) client;
            removalConsumer.onRemoved(new Fingerprint("", groupId, fingerId, deviceId), remaining);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onEnumerate(final long deviceId, final int fingerId, final int groupId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$HalResultController$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21.HalResultController.this.m2537xbf4a0c61(groupId, fingerId, deviceId, remaining);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnumerate$5$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21$HalResultController  reason: not valid java name */
        public /* synthetic */ void m2537xbf4a0c61(int groupId, int fingerId, long deviceId, int remaining) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof EnumerateConsumer)) {
                Slog.e(Fingerprint21.TAG, "onEnumerate for non-enumerate consumer: " + Utils.getClientName(client));
                return;
            }
            EnumerateConsumer enumerateConsumer = (EnumerateConsumer) client;
            enumerateConsumer.onEnumerationResult(new Fingerprint("", groupId, fingerId, deviceId), remaining);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Fingerprint21(Context context, BiometricStateCallback biometricStateCallback, FingerprintSensorPropertiesInternal sensorProps, BiometricScheduler scheduler, Handler handler, LockoutResetDispatcher lockoutResetDispatcher, HalResultController controller, BiometricContext biometricContext) {
        LockoutFrameworkImpl.LockoutResetCallback lockoutResetCallback = new LockoutFrameworkImpl.LockoutResetCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.1
            @Override // com.android.server.biometrics.sensors.fingerprint.hidl.LockoutFrameworkImpl.LockoutResetCallback
            public void onLockoutReset(int userId) {
                Fingerprint21.this.mLockoutResetDispatcher.notifyLockoutResetCallbacks(Fingerprint21.this.mSensorProperties.sensorId);
            }
        };
        this.mLockoutResetCallback = lockoutResetCallback;
        SynchronousUserSwitchObserver synchronousUserSwitchObserver = new SynchronousUserSwitchObserver() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.2
            public void onUserSwitching(int newUserId) {
                Fingerprint21.this.scheduleInternalCleanup(newUserId, null);
            }
        };
        this.mUserSwitchObserver = synchronousUserSwitchObserver;
        this.mContext = context;
        this.mBiometricStateCallback = biometricStateCallback;
        this.mBiometricContext = biometricContext;
        this.mSensorProperties = sensorProps;
        this.mSensorId = sensorProps.sensorId;
        this.mIsUdfps = sensorProps.sensorType == 3 || sensorProps.sensorType == 2;
        this.mIsPowerbuttonFps = sensorProps.sensorType == 4;
        this.mScheduler = scheduler;
        this.mHandler = handler;
        this.mActivityTaskManager = ActivityTaskManager.getInstance();
        this.mTaskStackListener = new BiometricTaskStackListener();
        this.mAuthenticatorIds = Collections.synchronizedMap(new HashMap());
        this.mLazyDaemon = new Supplier() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda21
            @Override // java.util.function.Supplier
            public final Object get() {
                return Fingerprint21.this.getDaemon();
            }
        };
        this.mLockoutResetDispatcher = lockoutResetDispatcher;
        this.mLockoutTracker = new LockoutFrameworkImpl(context, lockoutResetCallback);
        this.mHalResultController = controller;
        controller.setCallback(new HalResultController.Callback() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda22
            @Override // com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.HalResultController.Callback
            public final void onHardwareUnavailable() {
                Fingerprint21.this.m2515x998c2c83();
            }
        });
        try {
            ActivityManager.getService().registerUserSwitchObserver(synchronousUserSwitchObserver, TAG);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to register user switch observer");
        }
        ITranFingerprintService.Instance().setContext(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2515x998c2c83() {
        this.mDaemon = null;
        this.mCurrentUserId = -10000;
    }

    public static Fingerprint21 newInstance(Context context, BiometricStateCallback biometricStateCallback, FingerprintSensorPropertiesInternal sensorProps, Handler handler, LockoutResetDispatcher lockoutResetDispatcher, GestureAvailabilityDispatcher gestureAvailabilityDispatcher) {
        BiometricScheduler scheduler = new BiometricScheduler(TAG, BiometricScheduler.sensorTypeFromFingerprintProperties(sensorProps), gestureAvailabilityDispatcher);
        HalResultController controller = new HalResultController(sensorProps.sensorId, context, handler, scheduler);
        return new Fingerprint21(context, biometricStateCallback, sensorProps, scheduler, handler, lockoutResetDispatcher, controller, BiometricContext.getInstance(context));
    }

    public void serviceDied(long cookie) {
        Slog.e(TAG, "HAL died");
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2529x9b94df69();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$serviceDied$1$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2529x9b94df69() {
        PerformanceTracker.getInstanceForSensorId(this.mSensorProperties.sensorId).incrementHALDeathCount();
        this.mDaemon = null;
        this.mCurrentUserId = -10000;
        BaseClientMonitor client = this.mScheduler.getCurrentClient();
        if (client instanceof ErrorConsumer) {
            Slog.e(TAG, "Sending ERROR_HW_UNAVAILABLE for client: " + client);
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(1, 0);
            FrameworkStatsLog.write(148, 1, 1, -1);
        }
        this.mScheduler.recordCrashState();
        this.mScheduler.reset();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized IBiometricsFingerprint getDaemon() {
        if (this.mTestHalEnabled) {
            TestHal testHal = new TestHal(this.mContext, this.mSensorId);
            testHal.setNotify(this.mHalResultController);
            return testHal;
        }
        IBiometricsFingerprint iBiometricsFingerprint = this.mDaemon;
        if (iBiometricsFingerprint != null) {
            return iBiometricsFingerprint;
        }
        Slog.d(TAG, "Daemon was null, reconnecting, current operation: " + this.mScheduler.getCurrentClient());
        try {
            this.mDaemon = IBiometricsFingerprint.getService();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get fingerprint HAL", e);
        } catch (NoSuchElementException e2) {
            Slog.w(TAG, "NoSuchElementException", e2);
        }
        IBiometricsFingerprint iBiometricsFingerprint2 = this.mDaemon;
        if (iBiometricsFingerprint2 == null) {
            Slog.w(TAG, "Fingerprint HAL not available");
            return null;
        }
        iBiometricsFingerprint2.asBinder().linkToDeath(this, 0L);
        ITranFingerprintService.Instance().setDaemonCallback(this.mHalResultController);
        long halId = 0;
        try {
            halId = this.mDaemon.setNotify(ITranFingerprintService.Instance().getDaemonCallback() == null ? this.mHalResultController : ITranFingerprintService.Instance().getDaemonCallback());
        } catch (RemoteException e3) {
            Slog.e(TAG, "Failed to set callback for fingerprint HAL", e3);
            this.mDaemon = null;
        }
        Slog.d(TAG, "Fingerprint HAL ready, HAL ID: " + halId);
        if (halId != 0) {
            scheduleLoadAuthenticatorIds();
            scheduleInternalCleanup(ActivityManager.getCurrentUser(), null);
        } else {
            Slog.e(TAG, "Unable to set callback");
            this.mDaemon = null;
        }
        return this.mDaemon;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IUdfpsOverlayController getUdfpsOverlayController() {
        return this.mUdfpsOverlayController;
    }

    private void scheduleLoadAuthenticatorIds() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2524xbc181f45();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleLoadAuthenticatorIds$2$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2524xbc181f45() {
        for (UserInfo user : UserManager.get(this.mContext).getAliveUsers()) {
            int targetUserId = user.id;
            if (!this.mAuthenticatorIds.containsKey(Integer.valueOf(targetUserId))) {
                scheduleUpdateActiveUserWithoutHandler(targetUserId, true);
            }
        }
    }

    private void scheduleUpdateActiveUserWithoutHandler(int targetUserId) {
        scheduleUpdateActiveUserWithoutHandler(targetUserId, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleUpdateActiveUserWithoutHandler(final int targetUserId, boolean force) {
        boolean hasEnrolled = !getEnrolledFingerprints(this.mSensorProperties.sensorId, targetUserId).isEmpty();
        Context context = this.mContext;
        FingerprintUpdateActiveUserClient client = new FingerprintUpdateActiveUserClient(context, this.mLazyDaemon, targetUserId, context.getOpPackageName(), this.mSensorProperties.sensorId, createLogger(0, 0), this.mBiometricContext, new Supplier() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                int currentUser;
                currentUser = Fingerprint21.this.getCurrentUser();
                return Integer.valueOf(currentUser);
            }
        }, hasEnrolled, this.mAuthenticatorIds, force);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.3
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                if (success) {
                    Fingerprint21.this.mCurrentUserId = targetUserId;
                    return;
                }
                Slog.w(Fingerprint21.TAG, "Failed to change user, still: " + Fingerprint21.this.mCurrentUserId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurrentUser() {
        return this.mCurrentUserId;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean containsSensor(int sensorId) {
        return this.mSensorProperties.sensorId == sensorId;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public List<FingerprintSensorPropertiesInternal> getSensorProperties() {
        List<FingerprintSensorPropertiesInternal> properties = new ArrayList<>();
        properties.add(this.mSensorProperties);
        return properties;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public FingerprintSensorPropertiesInternal getSensorProperties(int sensorId) {
        return this.mSensorProperties;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleResetLockout(final int sensorId, final int userId, byte[] hardwareAuthToken) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2527x2fe3b71b(userId, sensorId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleResetLockout$3$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2527x2fe3b71b(int userId, int sensorId) {
        Context context = this.mContext;
        FingerprintResetLockoutClient client = new FingerprintResetLockoutClient(context, userId, context.getOpPackageName(), sensorId, createLogger(0, 0), this.mBiometricContext, this.mLockoutTracker);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleGenerateChallenge(int sensorId, final int userId, final IBinder token, final IFingerprintServiceReceiver receiver, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2522xd86a6868(token, receiver, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleGenerateChallenge$4$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2522xd86a6868(IBinder token, IFingerprintServiceReceiver receiver, int userId, String opPackageName) {
        FingerprintGenerateChallengeClient client = new FingerprintGenerateChallengeClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), userId, opPackageName, this.mSensorProperties.sensorId, createLogger(0, 0), this.mBiometricContext);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRevokeChallenge(int sensorId, final int userId, final IBinder token, final String opPackageName, long challenge) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2528x86cea518(token, userId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRevokeChallenge$5$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2528x86cea518(IBinder token, int userId, String opPackageName) {
        FingerprintRevokeChallengeClient client = new FingerprintRevokeChallengeClient(this.mContext, this.mLazyDaemon, token, userId, opPackageName, this.mSensorProperties.sensorId, createLogger(0, 0), this.mBiometricContext);
        this.mScheduler.scheduleClientMonitor(client);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleEnroll(int sensorId, final IBinder token, final byte[] hardwareAuthToken, final int userId, final IFingerprintServiceReceiver receiver, final String opPackageName, final int enrollReason) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2520x3f4d2f06(userId, token, id, receiver, hardwareAuthToken, opPackageName, enrollReason);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleEnroll$6$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2520x3f4d2f06(int userId, IBinder token, long id, IFingerprintServiceReceiver receiver, byte[] hardwareAuthToken, String opPackageName, int enrollReason) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        FingerprintEnrollClient client = new FingerprintEnrollClient(this.mContext, this.mLazyDaemon, token, id, new ClientMonitorCallbackConverter(receiver), userId, hardwareAuthToken, opPackageName, FingerprintUtils.getLegacyInstance(this.mSensorId), 60, this.mSensorProperties.sensorId, createLogger(1, 0), this.mBiometricContext, this.mUdfpsOverlayController, this.mSidefpsController, enrollReason);
        this.mScheduler.scheduleClientMonitor(client, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.4
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientStarted(BaseClientMonitor clientMonitor) {
                ITranFingerprintService.Instance().setEnrollState(true);
                Fingerprint21.this.mBiometricStateCallback.onClientStarted(clientMonitor);
            }

            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                ITranFingerprintService.Instance().setEnrollState(false);
                Fingerprint21.this.mBiometricStateCallback.onClientFinished(clientMonitor, success);
                if (success) {
                    Fingerprint21.this.scheduleUpdateActiveUserWithoutHandler(clientMonitor.getTargetUserId(), true);
                }
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void cancelEnrollment(int sensorId, final IBinder token, final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2514xb40d9cc4(token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelEnrollment$7$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2514xb40d9cc4(IBinder token, long requestId) {
        this.mScheduler.cancelEnrollment(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleFingerDetect(int sensorId, final IBinder token, final int userId, final ClientMonitorCallbackConverter listener, final String opPackageName, final int statsClient) {
        final long id = this.mRequestCounter.incrementAndGet();
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2521xfcf0a9be(userId, token, id, listener, opPackageName, statsClient);
            }
        });
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleFingerDetect$8$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2521xfcf0a9be(int userId, IBinder token, long id, ClientMonitorCallbackConverter listener, String opPackageName, int statsClient) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        boolean isStrongBiometric = Utils.isStrongBiometric(this.mSensorProperties.sensorId);
        FingerprintDetectClient client = new FingerprintDetectClient(this.mContext, this.mLazyDaemon, token, id, listener, userId, opPackageName, this.mSensorProperties.sensorId, createLogger(2, statsClient), this.mBiometricContext, this.mUdfpsOverlayController, isStrongBiometric);
        this.mScheduler.scheduleClientMonitor(client, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleAuthenticate(int sensorId, final IBinder token, final long operationId, final int userId, final int cookie, final ClientMonitorCallbackConverter listener, final String opPackageName, final long requestId, final boolean restricted, final int statsClient, final boolean allowBackgroundAuthentication) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2519xc9a656d4(userId, token, requestId, listener, operationId, restricted, opPackageName, cookie, statsClient, allowBackgroundAuthentication);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAuthenticate$9$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2519xc9a656d4(int userId, IBinder token, long requestId, ClientMonitorCallbackConverter listener, long operationId, boolean restricted, String opPackageName, int cookie, int statsClient, boolean allowBackgroundAuthentication) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        boolean isStrongBiometric = Utils.isStrongBiometric(this.mSensorProperties.sensorId);
        FingerprintAuthenticationClient client = new FingerprintAuthenticationClient(this.mContext, this.mLazyDaemon, token, requestId, listener, userId, operationId, restricted, opPackageName, cookie, false, this.mSensorProperties.sensorId, createLogger(2, statsClient), this.mBiometricContext, isStrongBiometric, this.mTaskStackListener, this.mLockoutTracker, this.mUdfpsOverlayController, this.mSidefpsController, allowBackgroundAuthentication, this.mSensorProperties);
        this.mScheduler.scheduleClientMonitor(client, this.mBiometricStateCallback);
        client.setVibrateState(!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(opPackageName));
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long scheduleAuthenticate(int sensorId, IBinder token, long operationId, int userId, int cookie, ClientMonitorCallbackConverter listener, String opPackageName, boolean restricted, int statsClient, boolean allowBackgroundAuthentication) {
        long id = this.mRequestCounter.incrementAndGet();
        scheduleAuthenticate(sensorId, token, operationId, userId, cookie, listener, opPackageName, id, restricted, statsClient, allowBackgroundAuthentication);
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startPreparedClient$10$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2532xf96fb8dc(int cookie) {
        this.mScheduler.startPreparedClient(cookie);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void startPreparedClient(int sensorId, final int cookie) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2532xf96fb8dc(cookie);
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void cancelAuthentication(int sensorId, final IBinder token, final long requestId) {
        Slog.d(TAG, "cancelAuthentication, sensorId: " + sensorId);
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2513xdde82cbd(token, requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAuthentication$11$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2513xdde82cbd(IBinder token, long requestId) {
        this.mScheduler.cancelAuthenticationOrDetection(token, requestId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRemove(int sensorId, final IBinder token, final IFingerprintServiceReceiver receiver, final int fingerId, final int userId, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2525x37a70885(userId, token, receiver, fingerId, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemove$12$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2525x37a70885(int userId, IBinder token, IFingerprintServiceReceiver receiver, int fingerId, String opPackageName) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        FingerprintRemovalClient client = new FingerprintRemovalClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), fingerId, userId, opPackageName, FingerprintUtils.getLegacyInstance(this.mSensorId), this.mSensorProperties.sensorId, createLogger(4, 0), this.mBiometricContext, this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client, this.mBiometricStateCallback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleRemoveAll(int sensorId, final IBinder token, final IFingerprintServiceReceiver receiver, final int userId, final String opPackageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2526x92268db5(userId, token, receiver, opPackageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRemoveAll$13$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2526x92268db5(int userId, IBinder token, IFingerprintServiceReceiver receiver, String opPackageName) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        FingerprintRemovalClient client = new FingerprintRemovalClient(this.mContext, this.mLazyDaemon, token, new ClientMonitorCallbackConverter(receiver), 0, userId, opPackageName, FingerprintUtils.getLegacyInstance(this.mSensorId), this.mSensorProperties.sensorId, createLogger(4, 0), this.mBiometricContext, this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client, this.mBiometricStateCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleInternalCleanup(final int userId, final ClientMonitorCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2523x1471f9fe(userId, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInternalCleanup$14$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2523x1471f9fe(int userId, ClientMonitorCallback callback) {
        scheduleUpdateActiveUserWithoutHandler(userId);
        List<Fingerprint> enrolledList = getEnrolledFingerprints(this.mSensorProperties.sensorId, userId);
        Context context = this.mContext;
        FingerprintInternalCleanupClient client = new FingerprintInternalCleanupClient(context, this.mLazyDaemon, userId, context.getOpPackageName(), this.mSensorProperties.sensorId, createLogger(3, 0), this.mBiometricContext, enrolledList, FingerprintUtils.getLegacyInstance(this.mSensorId), this.mAuthenticatorIds);
        this.mScheduler.scheduleClientMonitor(client, callback);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleInternalCleanup(int sensorId, int userId, ClientMonitorCallback callback) {
        scheduleInternalCleanup(userId, new ClientMonitorCompositeCallback(callback, this.mBiometricStateCallback));
    }

    private BiometricLogger createLogger(int statsAction, int statsClient) {
        return new BiometricLogger(this.mContext, 1, statsAction, statsClient);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean isHardwareDetected(int sensorId) {
        return getDaemon() != null;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void rename(int sensorId, final int fingerId, final int userId, final String name) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2518x23c1b9a5(userId, fingerId, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rename$15$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2518x23c1b9a5(int userId, int fingerId, String name) {
        FingerprintUtils.getLegacyInstance(this.mSensorId).renameBiometricForUser(this.mContext, userId, fingerId, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public List<Fingerprint> getEnrolledFingerprints(int sensorId, int userId) {
        return FingerprintUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public int getLockoutModeForUser(int sensorId, int userId) {
        return this.mLockoutTracker.getLockoutModeForUser(userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public long getAuthenticatorId(int sensorId, int userId) {
        return this.mAuthenticatorIds.getOrDefault(Integer.valueOf(userId), 0L).longValue();
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerDown(long requestId, int sensorId, final int x, final int y, final float minor, final float major) {
        this.mScheduler.getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Fingerprint21.lambda$onPointerDown$16(x, y, minor, major, (BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onPointerDown$16(int x, int y, float minor, float major, BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.w(TAG, "onFingerDown received during client: " + client);
        } else {
            ((Udfps) client).onPointerDown(x, y, minor, major);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerUp(long requestId, int sensorId) {
        this.mScheduler.getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Fingerprint21.lambda$onPointerUp$17((BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onPointerUp$17(BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.w(TAG, "onFingerDown received during client: " + client);
        } else {
            ((Udfps) client).onPointerUp();
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onUiReady(long requestId, int sensorId) {
        this.mScheduler.getCurrentClientIfMatches(requestId, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Fingerprint21.lambda$onUiReady$18((BaseClientMonitor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onUiReady$18(BaseClientMonitor client) {
        if (!(client instanceof Udfps)) {
            Slog.w(TAG, "onUiReady received during client: " + client);
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
        long sensorToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        proto.write(CompanionMessage.MESSAGE_ID, this.mSensorProperties.sensorId);
        proto.write(CompanionMessage.TYPE, 1);
        if (this.mSensorProperties.isAnyUdfpsType()) {
            proto.write(2259152797704L, 0);
        }
        proto.write(1120986464259L, Utils.getCurrentStrength(this.mSensorProperties.sensorId));
        proto.write(1146756268036L, this.mScheduler.dumpProtoState(clearSchedulerBuffer));
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(2246267895813L);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            proto.write(1120986464258L, FingerprintUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId).size());
            proto.end(userToken);
        }
        proto.write(1133871366150L, this.mSensorProperties.resetLockoutRequiresHardwareAuthToken);
        proto.write(1133871366151L, this.mSensorProperties.resetLockoutRequiresChallenge);
        proto.end(sensorToken);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void dumpProtoMetrics(int sensorId, FileDescriptor fd) {
        PerformanceTracker tracker = PerformanceTracker.getInstanceForSensorId(this.mSensorProperties.sensorId);
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            proto.write(1120986464258L, FingerprintUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId).size());
            long countsToken = proto.start(1146756268035L);
            proto.write(CompanionMessage.MESSAGE_ID, tracker.getAcceptForUser(userId));
            proto.write(1120986464258L, tracker.getRejectForUser(userId));
            proto.write(1120986464259L, tracker.getAcquireForUser(userId));
            proto.write(1120986464260L, tracker.getTimedLockoutForUser(userId));
            proto.write(1120986464261L, tracker.getPermanentLockoutForUser(userId));
            proto.end(countsToken);
            long countsToken2 = proto.start(1146756268036L);
            proto.write(CompanionMessage.MESSAGE_ID, tracker.getAcceptCryptoForUser(userId));
            proto.write(1120986464258L, tracker.getRejectCryptoForUser(userId));
            proto.write(1120986464259L, tracker.getAcquireCryptoForUser(userId));
            proto.write(1120986464260L, 0);
            proto.write(1120986464261L, 0);
            proto.end(countsToken2);
            proto.end(userToken);
        }
        proto.flush();
        tracker.clear();
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void scheduleInvalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) {
        try {
            callback.onCompleted();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to complete InvalidateAuthenticatorId");
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void dumpInternal(int sensorId, PrintWriter pw) {
        PerformanceTracker performanceTracker = PerformanceTracker.getInstanceForSensorId(this.mSensorProperties.sensorId);
        JSONObject dump = new JSONObject();
        try {
            dump.put(HostingRecord.HOSTING_TYPE_SERVICE, TAG);
            dump.put("isUdfps", this.mIsUdfps);
            dump.put("isPowerbuttonFps", this.mIsPowerbuttonFps);
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int N = FingerprintUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId).size();
                JSONObject set = new JSONObject();
                set.put("id", userId);
                set.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, N);
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
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestHalEnabled(boolean enabled) {
        this.mTestHalEnabled = enabled;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) {
        return new BiometricTestSessionImpl(this.mContext, this.mSensorProperties.sensorId, callback, this.mBiometricStateCallback, this, this.mHalResultController);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void setAppBiometricsForUser(final int sensorId, final int fingerId, final int groupId, final String packagename, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2530xeb5f61e3(sensorId, fingerId, groupId, packagename, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAppBiometricsForUser$19$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2530xeb5f61e3(int sensorId, int fingerId, int groupId, String packagename, int userId) {
        FingerprintUtils.getLegacyInstance(sensorId).setAppBiometricsForUser(this.mContext, fingerId, groupId, packagename, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public String getAppPackagenameForUser(int sensorId, int fingerId, int userId) {
        return FingerprintUtils.getLegacyInstance(sensorId).getAppPackagenameForUser(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public Fingerprint getAddBiometricForUser(int sensorId, int userId) {
        return FingerprintUtils.getLegacyInstance(sensorId).getAddBiometricForUser(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean hasAppPackagename(int sensorId, int userId) {
        return FingerprintUtils.getLegacyInstance(sensorId).hasAppPackagename(this.mContext, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public boolean checkName(int sensorId, int userId, String name) {
        return FingerprintUtils.getLegacyInstance(sensorId).checkName(this.mContext, userId, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void startAppForFp(final int sensorId, final int fingerId, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2531x88d4a947(sensorId, fingerId, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startAppForFp$20$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2531x88d4a947(int sensorId, int fingerId, int userId) {
        FingerprintUtils.getLegacyInstance(sensorId).startAppForFp(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void notifyAppResumeForFp(final int sensorId, final int userId, final String packagename, final String name) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2517x88b6567a(sensorId, userId, packagename, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAppResumeForFp$21$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2517x88b6567a(int sensorId, int userId, String packagename, String name) {
        FingerprintUtils.getLegacyInstance(sensorId).notifyAppResumeForFp(this.mContext, userId, packagename, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void notifyAppPauseForFp(final int sensorId, final int userId, final String packagename, final String name) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21.this.m2516x98337e46(sensorId, userId, packagename, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAppPauseForFp$22$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21  reason: not valid java name */
    public /* synthetic */ void m2516x98337e46(int sensorId, int userId, String packagename, String name) {
        FingerprintUtils.getLegacyInstance(sensorId).notifyAppPauseForFp(this.mContext, userId, packagename, name);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public int getAppUserIdForUser(int sensorId, int fingerId, int userId) {
        return FingerprintUtils.getLegacyInstance(sensorId).getAppUserIdForUser(this.mContext, fingerId, userId);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onUpdateFocusedApp(int sensorId, String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
        FingerprintUtils.getLegacyInstance(sensorId).onUpdateFocusedApp(this.mContext, oldPackageName, oldComponent, newPackageName, newComponent);
    }
}
