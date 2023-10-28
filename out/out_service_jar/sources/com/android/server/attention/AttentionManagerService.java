package com.android.server.attention;

import android.app.ActivityThread;
import android.attention.AttentionManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.SensorPrivacyManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.attention.IAttentionCallback;
import android.service.attention.IAttentionService;
import android.service.attention.IProximityUpdateCallback;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class AttentionManagerService extends SystemService {
    protected static final int ATTENTION_CACHE_BUFFER_SIZE = 5;
    private static final long CONNECTION_TTL_MILLIS = 60000;
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT_SERVICE_ENABLED = true;
    static final long DEFAULT_STALE_AFTER_MILLIS = 1000;
    static final String KEY_SERVICE_ENABLED = "service_enabled";
    static final String KEY_STALE_AFTER_MILLIS = "stale_after_millis";
    private static final String LOG_TAG = "AttentionManagerService";
    private static final long SERVICE_BINDING_WAIT_MILLIS = 1000;
    private static String sTestAttentionServicePackage;
    private AttentionCheckCacheBuffer mAttentionCheckCacheBuffer;
    private AttentionHandler mAttentionHandler;
    private boolean mBinding;
    ComponentName mComponentName;
    private final AttentionServiceConnection mConnection;
    private final Context mContext;
    AttentionCheck mCurrentAttentionCheck;
    ProximityUpdate mCurrentProximityUpdate;
    boolean mIsServiceEnabled;
    private final Object mLock;
    private final PowerManager mPowerManager;
    private final SensorPrivacyManager mPrivacyManager;
    protected IAttentionService mService;
    private CountDownLatch mServiceBindingLatch;
    long mStaleAfterMillis;

    public AttentionManagerService(Context context) {
        this(context, (PowerManager) context.getSystemService("power"), new Object(), null);
        this.mAttentionHandler = new AttentionHandler();
    }

    AttentionManagerService(Context context, PowerManager powerManager, Object lock, AttentionHandler handler) {
        super(context);
        this.mConnection = new AttentionServiceConnection();
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mPowerManager = powerManager;
        this.mLock = lock;
        this.mAttentionHandler = handler;
        this.mPrivacyManager = SensorPrivacyManager.getInstance(context);
        this.mServiceBindingLatch = new CountDownLatch(1);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mContext.registerReceiver(new ScreenStateReceiver(), new IntentFilter("android.intent.action.SCREEN_OFF"));
            readValuesFromDeviceConfig();
            DeviceConfig.addOnPropertiesChangedListener("attention_manager_service", ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.attention.AttentionManagerService$$ExternalSyntheticLambda0
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    AttentionManagerService.this.m1751xc1887ce2(properties);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-attention-AttentionManagerService  reason: not valid java name */
    public /* synthetic */ void m1751xc1887ce2(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("attention", new BinderService());
        publishLocalService(AttentionManagerInternal.class, new LocalService());
    }

    public static boolean isServiceConfigured(Context context) {
        return !TextUtils.isEmpty(getServiceConfigPackage(context));
    }

    protected boolean isServiceAvailable() {
        if (this.mComponentName == null) {
            this.mComponentName = resolveAttentionService(this.mContext);
        }
        return this.mComponentName != null;
    }

    private boolean getIsServiceEnabled() {
        return DeviceConfig.getBoolean("attention_manager_service", KEY_SERVICE_ENABLED, true);
    }

    protected long getStaleAfterMillis() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_STALE_AFTER_MILLIS, 1000L);
        if (millis < 0 || millis > JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
            Slog.w(LOG_TAG, "Bad flag value supplied for: stale_after_millis");
            return 1000L;
        }
        return millis;
    }

    private void onDeviceConfigChange(Set<String> keys) {
        for (String key : keys) {
            char c = 65535;
            switch (key.hashCode()) {
                case -337803025:
                    if (key.equals(KEY_STALE_AFTER_MILLIS)) {
                        c = 1;
                        break;
                    }
                    break;
                case 1914663863:
                    if (key.equals(KEY_SERVICE_ENABLED)) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    readValuesFromDeviceConfig();
                    return;
                default:
                    Slog.i(LOG_TAG, "Ignoring change on " + key);
            }
        }
    }

    private void readValuesFromDeviceConfig() {
        this.mIsServiceEnabled = getIsServiceEnabled();
        this.mStaleAfterMillis = getStaleAfterMillis();
        Slog.i(LOG_TAG, "readValuesFromDeviceConfig():\nmIsServiceEnabled=" + this.mIsServiceEnabled + "\nmStaleAfterMillis=" + this.mStaleAfterMillis);
    }

    boolean checkAttention(long timeout, AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
        Objects.requireNonNull(callbackInternal);
        if (!this.mIsServiceEnabled) {
            Slog.w(LOG_TAG, "Trying to call checkAttention() on an unsupported device.");
            return false;
        } else if (!isServiceAvailable()) {
            Slog.w(LOG_TAG, "Service is not available at this moment.");
            return false;
        } else if (this.mPrivacyManager.isSensorPrivacyEnabled(2)) {
            Slog.w(LOG_TAG, "Camera is locked by a toggle.");
            return false;
        } else if (!this.mPowerManager.isInteractive() || this.mPowerManager.isPowerSaveMode()) {
            return false;
        } else {
            synchronized (this.mLock) {
                freeIfInactiveLocked();
                bindLocked();
            }
            long now = SystemClock.uptimeMillis();
            awaitServiceBinding(Math.min(1000L, timeout));
            synchronized (this.mLock) {
                AttentionCheckCacheBuffer attentionCheckCacheBuffer = this.mAttentionCheckCacheBuffer;
                AttentionCheckCache cache = attentionCheckCacheBuffer == null ? null : attentionCheckCacheBuffer.getLast();
                if (cache != null && now < cache.mLastComputed + this.mStaleAfterMillis) {
                    callbackInternal.onSuccess(cache.mResult, cache.mTimestamp);
                    return true;
                }
                AttentionCheck attentionCheck = this.mCurrentAttentionCheck;
                if (attentionCheck == null || (attentionCheck.mIsDispatched && this.mCurrentAttentionCheck.mIsFulfilled)) {
                    this.mCurrentAttentionCheck = new AttentionCheck(callbackInternal, this);
                    if (this.mService != null) {
                        try {
                            cancelAfterTimeoutLocked(timeout);
                            this.mService.checkAttention(this.mCurrentAttentionCheck.mIAttentionCallback);
                            this.mCurrentAttentionCheck.mIsDispatched = true;
                        } catch (RemoteException e) {
                            Slog.e(LOG_TAG, "Cannot call into the AttentionService");
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
    }

    void cancelAttentionCheck(AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
        synchronized (this.mLock) {
            if (!this.mCurrentAttentionCheck.mCallbackInternal.equals(callbackInternal)) {
                Slog.w(LOG_TAG, "Cannot cancel a non-current request");
            } else {
                cancel();
            }
        }
    }

    boolean onStartProximityUpdates(AttentionManagerInternal.ProximityUpdateCallbackInternal callbackInternal) {
        Objects.requireNonNull(callbackInternal);
        if (!this.mIsServiceEnabled) {
            Slog.w(LOG_TAG, "Trying to call onProximityUpdate() on an unsupported device.");
            return false;
        } else if (!isServiceAvailable()) {
            Slog.w(LOG_TAG, "Service is not available at this moment.");
            return false;
        } else if (!this.mPowerManager.isInteractive()) {
            Slog.w(LOG_TAG, "Proximity Service is unavailable during screen off at this moment.");
            return false;
        } else {
            synchronized (this.mLock) {
                freeIfInactiveLocked();
                bindLocked();
            }
            awaitServiceBinding(1000L);
            synchronized (this.mLock) {
                ProximityUpdate proximityUpdate = this.mCurrentProximityUpdate;
                if (proximityUpdate != null && proximityUpdate.mStartedUpdates) {
                    if (this.mCurrentProximityUpdate.mCallbackInternal == callbackInternal) {
                        Slog.w(LOG_TAG, "Provided callback is already registered. Skipping.");
                        return true;
                    }
                    Slog.w(LOG_TAG, "New proximity update cannot be processed because there is already an ongoing update");
                    return false;
                }
                ProximityUpdate proximityUpdate2 = new ProximityUpdate(callbackInternal);
                this.mCurrentProximityUpdate = proximityUpdate2;
                return proximityUpdate2.startUpdates();
            }
        }
    }

    void onStopProximityUpdates(AttentionManagerInternal.ProximityUpdateCallbackInternal callbackInternal) {
        synchronized (this.mLock) {
            ProximityUpdate proximityUpdate = this.mCurrentProximityUpdate;
            if (proximityUpdate != null && proximityUpdate.mCallbackInternal.equals(callbackInternal) && this.mCurrentProximityUpdate.mStartedUpdates) {
                this.mCurrentProximityUpdate.cancelUpdates();
                this.mCurrentProximityUpdate = null;
                return;
            }
            Slog.w(LOG_TAG, "Cannot stop a non-current callback");
        }
    }

    protected void freeIfInactiveLocked() {
        this.mAttentionHandler.removeMessages(1);
        this.mAttentionHandler.sendEmptyMessageDelayed(1, 60000L);
    }

    private void cancelAfterTimeoutLocked(long timeout) {
        this.mAttentionHandler.sendEmptyMessageDelayed(2, timeout);
    }

    private static String getServiceConfigPackage(Context context) {
        return context.getPackageManager().getAttentionServicePackageName();
    }

    private void awaitServiceBinding(long millis) {
        try {
            this.mServiceBindingLatch.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.e(LOG_TAG, "Interrupted while waiting to bind Attention Service.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ComponentName resolveAttentionService(Context context) {
        String resolvedPackage;
        String serviceConfigPackage = getServiceConfigPackage(context);
        int flags = 1048576;
        if (!TextUtils.isEmpty(sTestAttentionServicePackage)) {
            resolvedPackage = sTestAttentionServicePackage;
            flags = 128;
        } else if (TextUtils.isEmpty(serviceConfigPackage)) {
            return null;
        } else {
            resolvedPackage = serviceConfigPackage;
        }
        Intent intent = new Intent("android.service.attention.AttentionService").setPackage(resolvedPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, flags);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.wtf(LOG_TAG, String.format("Service %s not found in package %s", "android.service.attention.AttentionService", serviceConfigPackage));
            return null;
        }
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        String permission = serviceInfo.permission;
        if ("android.permission.BIND_ATTENTION_SERVICE".equals(permission)) {
            return serviceInfo.getComponentName();
        }
        Slog.e(LOG_TAG, String.format("Service %s should require %s permission. Found %s permission", serviceInfo.getComponentName(), "android.permission.BIND_ATTENTION_SERVICE", serviceInfo.permission));
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(IndentingPrintWriter ipw) {
        ipw.println("Attention Manager Service (dumpsys attention) state:\n");
        ipw.println("isServiceEnabled=" + this.mIsServiceEnabled);
        ipw.println("mStaleAfterMillis=" + this.mStaleAfterMillis);
        ipw.println("AttentionServicePackageName=" + getServiceConfigPackage(this.mContext));
        ipw.println("Resolved component:");
        if (this.mComponentName != null) {
            ipw.increaseIndent();
            ipw.println("Component=" + this.mComponentName.getPackageName());
            ipw.println("Class=" + this.mComponentName.getClassName());
            ipw.decreaseIndent();
        }
        synchronized (this.mLock) {
            ipw.println("binding=" + this.mBinding);
            ipw.println("current attention check:");
            AttentionCheck attentionCheck = this.mCurrentAttentionCheck;
            if (attentionCheck != null) {
                attentionCheck.dump(ipw);
            }
            AttentionCheckCacheBuffer attentionCheckCacheBuffer = this.mAttentionCheckCacheBuffer;
            if (attentionCheckCacheBuffer != null) {
                attentionCheckCacheBuffer.dump(ipw);
            }
            ProximityUpdate proximityUpdate = this.mCurrentProximityUpdate;
            if (proximityUpdate != null) {
                proximityUpdate.dump(ipw);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends AttentionManagerInternal {
        private LocalService() {
        }

        public boolean isAttentionServiceSupported() {
            return AttentionManagerService.this.mIsServiceEnabled;
        }

        public boolean checkAttention(long timeout, AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
            return AttentionManagerService.this.checkAttention(timeout, callbackInternal);
        }

        public void cancelAttentionCheck(AttentionManagerInternal.AttentionCallbackInternal callbackInternal) {
            AttentionManagerService.this.cancelAttentionCheck(callbackInternal);
        }

        public boolean onStartProximityUpdates(AttentionManagerInternal.ProximityUpdateCallbackInternal callback) {
            return AttentionManagerService.this.onStartProximityUpdates(callback);
        }

        public void onStopProximityUpdates(AttentionManagerInternal.ProximityUpdateCallbackInternal callback) {
            AttentionManagerService.this.onStopProximityUpdates(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class AttentionCheckCacheBuffer {
        private final AttentionCheckCache[] mQueue = new AttentionCheckCache[5];
        private int mStartIndex = 0;
        private int mSize = 0;

        AttentionCheckCacheBuffer() {
        }

        public AttentionCheckCache getLast() {
            int i = this.mStartIndex;
            int i2 = this.mSize;
            int lastIdx = ((i + i2) - 1) % 5;
            if (i2 == 0) {
                return null;
            }
            return this.mQueue[lastIdx];
        }

        public void add(AttentionCheckCache cache) {
            int i = this.mStartIndex;
            int i2 = this.mSize;
            int nextIndex = (i + i2) % 5;
            this.mQueue[nextIndex] = cache;
            if (i2 == 5) {
                this.mStartIndex = i + 1;
            } else {
                this.mSize = i2 + 1;
            }
        }

        public AttentionCheckCache get(int offset) {
            if (offset >= this.mSize) {
                return null;
            }
            return this.mQueue[(this.mStartIndex + offset) % 5];
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(IndentingPrintWriter ipw) {
            ipw.println("attention check cache:");
            for (int i = 0; i < this.mSize; i++) {
                AttentionCheckCache cache = get(i);
                if (cache != null) {
                    ipw.increaseIndent();
                    ipw.println("timestamp=" + cache.mTimestamp);
                    ipw.println("result=" + cache.mResult);
                    ipw.decreaseIndent();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class AttentionCheckCache {
        private final long mLastComputed;
        private final int mResult;
        private final long mTimestamp;

        AttentionCheckCache(long lastComputed, int result, long timestamp) {
            this.mLastComputed = lastComputed;
            this.mResult = result;
            this.mTimestamp = timestamp;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AttentionCheck {
        private final AttentionManagerInternal.AttentionCallbackInternal mCallbackInternal;
        private final IAttentionCallback mIAttentionCallback;
        private boolean mIsDispatched;
        private boolean mIsFulfilled;

        AttentionCheck(final AttentionManagerInternal.AttentionCallbackInternal callbackInternal, final AttentionManagerService service) {
            this.mCallbackInternal = callbackInternal;
            this.mIAttentionCallback = new IAttentionCallback.Stub() { // from class: com.android.server.attention.AttentionManagerService.AttentionCheck.1
                public void onSuccess(int result, long timestamp) {
                    if (AttentionCheck.this.mIsFulfilled) {
                        return;
                    }
                    AttentionCheck.this.mIsFulfilled = true;
                    callbackInternal.onSuccess(result, timestamp);
                    logStats(result);
                    service.appendResultToAttentionCacheBuffer(new AttentionCheckCache(SystemClock.uptimeMillis(), result, timestamp));
                }

                public void onFailure(int error) {
                    if (AttentionCheck.this.mIsFulfilled) {
                        return;
                    }
                    AttentionCheck.this.mIsFulfilled = true;
                    callbackInternal.onFailure(error);
                    logStats(error);
                }

                private void logStats(int result) {
                    FrameworkStatsLog.write(143, result);
                }
            };
        }

        void cancelInternal() {
            this.mIsFulfilled = true;
            this.mCallbackInternal.onFailure(3);
        }

        void dump(IndentingPrintWriter ipw) {
            ipw.increaseIndent();
            ipw.println("is dispatched=" + this.mIsDispatched);
            ipw.println("is fulfilled:=" + this.mIsFulfilled);
            ipw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ProximityUpdate {
        private final AttentionManagerInternal.ProximityUpdateCallbackInternal mCallbackInternal;
        private final IProximityUpdateCallback mIProximityUpdateCallback;
        private boolean mStartedUpdates;

        ProximityUpdate(AttentionManagerInternal.ProximityUpdateCallbackInternal callbackInternal) {
            this.mCallbackInternal = callbackInternal;
            this.mIProximityUpdateCallback = new IProximityUpdateCallback.Stub() { // from class: com.android.server.attention.AttentionManagerService.ProximityUpdate.1
                public void onProximityUpdate(double distance) {
                    synchronized (AttentionManagerService.this.mLock) {
                        ProximityUpdate.this.mCallbackInternal.onProximityUpdate(distance);
                        AttentionManagerService.this.freeIfInactiveLocked();
                    }
                }
            };
        }

        boolean startUpdates() {
            synchronized (AttentionManagerService.this.mLock) {
                if (this.mStartedUpdates) {
                    Slog.w(AttentionManagerService.LOG_TAG, "Already registered to a proximity service.");
                    return false;
                } else if (AttentionManagerService.this.mService == null) {
                    Slog.w(AttentionManagerService.LOG_TAG, "There is no service bound. Proximity update request rejected.");
                    return false;
                } else {
                    try {
                        AttentionManagerService.this.mService.onStartProximityUpdates(this.mIProximityUpdateCallback);
                        this.mStartedUpdates = true;
                        return true;
                    } catch (RemoteException e) {
                        Slog.e(AttentionManagerService.LOG_TAG, "Cannot call into the AttentionService", e);
                        return false;
                    }
                }
            }
        }

        void cancelUpdates() {
            synchronized (AttentionManagerService.this.mLock) {
                if (this.mStartedUpdates) {
                    if (AttentionManagerService.this.mService == null) {
                        this.mStartedUpdates = false;
                        return;
                    }
                    try {
                        AttentionManagerService.this.mService.onStopProximityUpdates();
                        this.mStartedUpdates = false;
                    } catch (RemoteException e) {
                        Slog.e(AttentionManagerService.LOG_TAG, "Cannot call into the AttentionService", e);
                    }
                }
            }
        }

        void dump(IndentingPrintWriter ipw) {
            ipw.increaseIndent();
            ipw.println("is StartedUpdates=" + this.mStartedUpdates);
            ipw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void appendResultToAttentionCacheBuffer(AttentionCheckCache cache) {
        synchronized (this.mLock) {
            if (this.mAttentionCheckCacheBuffer == null) {
                this.mAttentionCheckCacheBuffer = new AttentionCheckCacheBuffer();
            }
            this.mAttentionCheckCacheBuffer.add(cache);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AttentionServiceConnection implements ServiceConnection {
        private AttentionServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            init(IAttentionService.Stub.asInterface(service));
            AttentionManagerService.this.mServiceBindingLatch.countDown();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            cleanupService();
        }

        void cleanupService() {
            init(null);
            AttentionManagerService.this.mServiceBindingLatch = new CountDownLatch(1);
        }

        private void init(IAttentionService service) {
            synchronized (AttentionManagerService.this.mLock) {
                AttentionManagerService.this.mService = service;
                AttentionManagerService.this.mBinding = false;
                AttentionManagerService.this.handlePendingCallbackLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePendingCallbackLocked() {
        AttentionCheck attentionCheck = this.mCurrentAttentionCheck;
        if (attentionCheck != null && !attentionCheck.mIsDispatched) {
            IAttentionService iAttentionService = this.mService;
            if (iAttentionService != null) {
                try {
                    iAttentionService.checkAttention(this.mCurrentAttentionCheck.mIAttentionCallback);
                    this.mCurrentAttentionCheck.mIsDispatched = true;
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Cannot call into the AttentionService");
                }
            } else {
                this.mCurrentAttentionCheck.mCallbackInternal.onFailure(2);
            }
        }
        ProximityUpdate proximityUpdate = this.mCurrentProximityUpdate;
        if (proximityUpdate != null && proximityUpdate.mStartedUpdates) {
            IAttentionService iAttentionService2 = this.mService;
            if (iAttentionService2 != null) {
                try {
                    iAttentionService2.onStartProximityUpdates(this.mCurrentProximityUpdate.mIProximityUpdateCallback);
                    return;
                } catch (RemoteException e2) {
                    Slog.e(LOG_TAG, "Cannot call into the AttentionService", e2);
                    return;
                }
            }
            this.mCurrentProximityUpdate.cancelUpdates();
            this.mCurrentProximityUpdate = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class AttentionHandler extends Handler {
        private static final int ATTENTION_CHECK_TIMEOUT = 2;
        private static final int CHECK_CONNECTION_EXPIRATION = 1;

        AttentionHandler() {
            super(Looper.myLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (AttentionManagerService.this.mLock) {
                        AttentionManagerService.this.cancelAndUnbindLocked();
                    }
                    return;
                case 2:
                    synchronized (AttentionManagerService.this.mLock) {
                        AttentionManagerService.this.cancel();
                    }
                    return;
                default:
                    return;
            }
        }
    }

    void cancel() {
        if (this.mCurrentAttentionCheck.mIsFulfilled) {
            return;
        }
        IAttentionService iAttentionService = this.mService;
        if (iAttentionService == null) {
            this.mCurrentAttentionCheck.cancelInternal();
            return;
        }
        try {
            iAttentionService.cancelAttentionCheck(this.mCurrentAttentionCheck.mIAttentionCallback);
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Unable to cancel attention check");
            this.mCurrentAttentionCheck.cancelInternal();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAndUnbindLocked() {
        synchronized (this.mLock) {
            AttentionCheck attentionCheck = this.mCurrentAttentionCheck;
            if (attentionCheck == null && this.mCurrentProximityUpdate == null) {
                return;
            }
            if (attentionCheck != null) {
                cancel();
            }
            ProximityUpdate proximityUpdate = this.mCurrentProximityUpdate;
            if (proximityUpdate != null) {
                proximityUpdate.cancelUpdates();
            }
            if (this.mService == null) {
                return;
            }
            this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.attention.AttentionManagerService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AttentionManagerService.this.m1750x5cfb1dce();
                }
            });
            this.mConnection.cleanupService();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAndUnbindLocked$1$com-android-server-attention-AttentionManagerService  reason: not valid java name */
    public /* synthetic */ void m1750x5cfb1dce() {
        this.mContext.unbindService(this.mConnection);
    }

    private void bindLocked() {
        if (this.mBinding || this.mService != null) {
            return;
        }
        this.mBinding = true;
        this.mAttentionHandler.post(new Runnable() { // from class: com.android.server.attention.AttentionManagerService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AttentionManagerService.this.m1749x956746c9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$bindLocked$2$com-android-server-attention-AttentionManagerService  reason: not valid java name */
    public /* synthetic */ void m1749x956746c9() {
        Intent serviceIntent = new Intent("android.service.attention.AttentionService").setComponent(this.mComponentName);
        this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, 67112961, UserHandle.CURRENT);
    }

    /* loaded from: classes.dex */
    private final class ScreenStateReceiver extends BroadcastReceiver {
        private ScreenStateReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                synchronized (AttentionManagerService.this.mLock) {
                    AttentionManagerService.this.cancelAndUnbindLocked();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class AttentionManagerServiceShellCommand extends ShellCommand {
        final TestableAttentionCallbackInternal mTestableAttentionCallback;
        final TestableProximityUpdateCallbackInternal mTestableProximityUpdateCallback;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class TestableAttentionCallbackInternal extends AttentionManagerInternal.AttentionCallbackInternal {
            private int mLastCallbackCode = -1;

            TestableAttentionCallbackInternal() {
            }

            public void onSuccess(int result, long timestamp) {
                this.mLastCallbackCode = result;
            }

            public void onFailure(int error) {
                this.mLastCallbackCode = error;
            }

            public void reset() {
                this.mLastCallbackCode = -1;
            }

            public int getLastCallbackCode() {
                return this.mLastCallbackCode;
            }
        }

        private AttentionManagerServiceShellCommand() {
            this.mTestableAttentionCallback = new TestableAttentionCallbackInternal();
            this.mTestableProximityUpdateCallback = new TestableProximityUpdateCallbackInternal();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class TestableProximityUpdateCallbackInternal extends AttentionManagerInternal.ProximityUpdateCallbackInternal {
            private double mLastCallbackCode = -1.0d;

            TestableProximityUpdateCallbackInternal() {
            }

            public void onProximityUpdate(double distance) {
                this.mLastCallbackCode = distance;
            }

            public void reset() {
                this.mLastCallbackCode = -1.0d;
            }

            public double getLastCallbackCode() {
                return this.mLastCallbackCode;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter err = getErrPrintWriter();
            try {
                char c2 = 3;
                switch (cmd.hashCode()) {
                    case -1208709968:
                        if (cmd.equals("getLastTestCallbackCode")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1002424240:
                        if (cmd.equals("getAttentionServiceComponent")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case -415045819:
                        if (cmd.equals("setTestableAttentionService")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3045982:
                        if (cmd.equals("call")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1048378748:
                        if (cmd.equals("getLastTestProximityUpdateCallbackCode")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1193447472:
                        if (cmd.equals("clearTestableAttentionService")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        return cmdResolveAttentionServiceComponent();
                    case 1:
                        String nextArgRequired = getNextArgRequired();
                        switch (nextArgRequired.hashCode()) {
                            case -1571871954:
                                if (nextArgRequired.equals("onStartProximityUpdates")) {
                                    c2 = 2;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            case 685821932:
                                if (nextArgRequired.equals("onStopProximityUpdates")) {
                                    break;
                                }
                                c2 = 65535;
                                break;
                            case 763077136:
                                if (nextArgRequired.equals("cancelCheckAttention")) {
                                    c2 = 1;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            case 1485997302:
                                if (nextArgRequired.equals("checkAttention")) {
                                    c2 = 0;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            default:
                                c2 = 65535;
                                break;
                        }
                        switch (c2) {
                            case 0:
                                return cmdCallCheckAttention();
                            case 1:
                                return cmdCallCancelAttention();
                            case 2:
                                return cmdCallOnStartProximityUpdates();
                            case 3:
                                return cmdCallOnStopProximityUpdates();
                            default:
                                throw new IllegalArgumentException("Invalid argument");
                        }
                    case 2:
                        return cmdSetTestableAttentionService(getNextArgRequired());
                    case 3:
                        return cmdClearTestableAttentionService();
                    case 4:
                        return cmdGetLastTestCallbackCode();
                    case 5:
                        return cmdGetLastTestProximityUpdateCallbackCode();
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (IllegalArgumentException e) {
                err.println("Error: " + e.getMessage());
                return -1;
            }
        }

        private int cmdSetTestableAttentionService(String testingServicePackage) {
            PrintWriter out = getOutPrintWriter();
            if (TextUtils.isEmpty(testingServicePackage)) {
                out.println("false");
                return 0;
            }
            AttentionManagerService.sTestAttentionServicePackage = testingServicePackage;
            resetStates();
            out.println(AttentionManagerService.this.mComponentName != null ? "true" : "false");
            return 0;
        }

        private int cmdClearTestableAttentionService() {
            AttentionManagerService.sTestAttentionServicePackage = "";
            this.mTestableAttentionCallback.reset();
            this.mTestableProximityUpdateCallback.reset();
            resetStates();
            return 0;
        }

        private int cmdCallCheckAttention() {
            PrintWriter out = getOutPrintWriter();
            boolean calledSuccessfully = AttentionManagerService.this.checkAttention(2000L, this.mTestableAttentionCallback);
            out.println(calledSuccessfully ? "true" : "false");
            return 0;
        }

        private int cmdCallCancelAttention() {
            PrintWriter out = getOutPrintWriter();
            AttentionManagerService.this.cancelAttentionCheck(this.mTestableAttentionCallback);
            out.println("true");
            return 0;
        }

        private int cmdCallOnStartProximityUpdates() {
            PrintWriter out = getOutPrintWriter();
            boolean calledSuccessfully = AttentionManagerService.this.onStartProximityUpdates(this.mTestableProximityUpdateCallback);
            out.println(calledSuccessfully ? "true" : "false");
            return 0;
        }

        private int cmdCallOnStopProximityUpdates() {
            PrintWriter out = getOutPrintWriter();
            AttentionManagerService.this.onStopProximityUpdates(this.mTestableProximityUpdateCallback);
            out.println("true");
            return 0;
        }

        private int cmdResolveAttentionServiceComponent() {
            PrintWriter out = getOutPrintWriter();
            ComponentName resolvedComponent = AttentionManagerService.resolveAttentionService(AttentionManagerService.this.mContext);
            out.println(resolvedComponent != null ? resolvedComponent.flattenToShortString() : "");
            return 0;
        }

        private int cmdGetLastTestCallbackCode() {
            PrintWriter out = getOutPrintWriter();
            out.println(this.mTestableAttentionCallback.getLastCallbackCode());
            return 0;
        }

        private int cmdGetLastTestProximityUpdateCallbackCode() {
            PrintWriter out = getOutPrintWriter();
            out.println(this.mTestableProximityUpdateCallback.getLastCallbackCode());
            return 0;
        }

        private void resetStates() {
            synchronized (AttentionManagerService.this.mLock) {
                AttentionManagerService.this.mCurrentProximityUpdate = null;
            }
            AttentionManagerService attentionManagerService = AttentionManagerService.this;
            attentionManagerService.mComponentName = AttentionManagerService.resolveAttentionService(attentionManagerService.mContext);
        }

        public void onHelp() {
            PrintWriter out = getOutPrintWriter();
            out.println("Attention commands: ");
            out.println("  setTestableAttentionService <service_package>: Bind to a custom implementation of attention service");
            out.println("  ---<service_package>:");
            out.println("       := Package containing the Attention Service implementation to bind to");
            out.println("  ---returns:");
            out.println("       := true, if was bound successfully");
            out.println("       := false, if was not bound successfully");
            out.println("  clearTestableAttentionService: Undo custom bindings. Revert to previous behavior");
            out.println("  getAttentionServiceComponent: Get the current service component string");
            out.println("  ---returns:");
            out.println("       := If valid, the component string (in shorten form) for the currently bound service.");
            out.println("       := else, empty string");
            out.println("  call checkAttention: Calls check attention");
            out.println("  ---returns:");
            out.println("       := true, if the call was successfully dispatched to the service implementation. (to see the result, call getLastTestCallbackCode)");
            out.println("       := false, otherwise");
            out.println("  call cancelCheckAttention: Cancels check attention");
            out.println("  call onStartProximityUpdates: Calls onStartProximityUpdates");
            out.println("  ---returns:");
            out.println("       := true, if the request was successfully dispatched to the service implementation. (to see the result, call getLastTestProximityUpdateCallbackCode)");
            out.println("       := false, otherwise");
            out.println("  call onStopProximityUpdates: Cancels proximity updates");
            out.println("  getLastTestCallbackCode");
            out.println("  ---returns:");
            out.println("       := An integer, representing the last callback code received from the bounded implementation. If none, it will return -1");
            out.println("  getLastTestProximityUpdateCallbackCode");
            out.println("  ---returns:");
            out.println("       := A double, representing the last proximity value received from the bounded implementation. If none, it will return -1.0");
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        AttentionManagerServiceShellCommand mAttentionManagerServiceShellCommand;

        private BinderService() {
            this.mAttentionManagerServiceShellCommand = new AttentionManagerServiceShellCommand();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            this.mAttentionManagerServiceShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(AttentionManagerService.this.mContext, AttentionManagerService.LOG_TAG, pw)) {
                return;
            }
            AttentionManagerService.this.dumpInternal(new IndentingPrintWriter(pw, "  "));
        }
    }
}
