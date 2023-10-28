package com.android.server.logcat;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ILogd;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.logcat.ILogcatManagerService;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.policy.EventLogTags;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public final class LogcatManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final int MSG_APPROVE_LOG_ACCESS = 1;
    private static final int MSG_DECLINE_LOG_ACCESS = 2;
    private static final int MSG_LOG_ACCESS_FINISHED = 3;
    private static final int MSG_LOG_ACCESS_REQUESTED = 0;
    private static final int MSG_LOG_ACCESS_STATUS_EXPIRED = 5;
    private static final int MSG_PENDING_TIMEOUT = 4;
    static final int PENDING_CONFIRMATION_TIMEOUT_MILLIS;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_DECLINED = 3;
    static final int STATUS_EXPIRATION_TIMEOUT_MILLIS = 60000;
    private static final int STATUS_NEW_REQUEST = 0;
    private static final int STATUS_PENDING = 1;
    private static final String TAG = "LogcatManagerService";
    private final Map<LogAccessClient, Integer> mActiveLogAccessCount;
    private ActivityManagerInternal mActivityManagerInternal;
    private final BinderService mBinderService;
    private final Supplier<Long> mClock;
    private final Context mContext;
    private final Handler mHandler;
    private final Injector mInjector;
    private final LogcatManagerServiceInternal mLocalService;
    private final Map<LogAccessClient, LogAccessStatus> mLogAccessStatus;
    private ILogd mLogdService;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface LogAccessRequestStatus {
    }

    static {
        PENDING_CONFIRMATION_TIMEOUT_MILLIS = Build.IS_DEBUGGABLE ? EventLogTags.SCREEN_TOGGLED : 400000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LogAccessClient {
        final String mPackageName;
        final int mUid;

        LogAccessClient(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof LogAccessClient) {
                LogAccessClient that = (LogAccessClient) o;
                return this.mUid == that.mUid && Objects.equals(this.mPackageName, that.mPackageName);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mUid), this.mPackageName);
        }

        public String toString() {
            return "LogAccessClient{mUid=" + this.mUid + ", mPackageName=" + this.mPackageName + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LogAccessRequest {
        final int mFd;
        final int mGid;
        final int mPid;
        final int mUid;

        private LogAccessRequest(int uid, int gid, int pid, int fd) {
            this.mUid = uid;
            this.mGid = gid;
            this.mPid = pid;
            this.mFd = fd;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof LogAccessRequest) {
                LogAccessRequest that = (LogAccessRequest) o;
                return this.mUid == that.mUid && this.mGid == that.mGid && this.mPid == that.mPid && this.mFd == that.mFd;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mUid), Integer.valueOf(this.mGid), Integer.valueOf(this.mPid), Integer.valueOf(this.mFd));
        }

        public String toString() {
            return "LogAccessRequest{mUid=" + this.mUid + ", mGid=" + this.mGid + ", mPid=" + this.mPid + ", mFd=" + this.mFd + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LogAccessStatus {
        final List<LogAccessRequest> mPendingRequests;
        int mStatus;

        private LogAccessStatus() {
            this.mStatus = 0;
            this.mPendingRequests = new ArrayList();
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends ILogcatManagerService.Stub {
        private BinderService() {
        }

        public void startThread(int uid, int gid, int pid, int fd) {
            LogAccessRequest logAccessRequest = new LogAccessRequest(uid, gid, pid, fd);
            Message msg = LogcatManagerService.this.mHandler.obtainMessage(0, logAccessRequest);
            LogcatManagerService.this.mHandler.sendMessageAtTime(msg, ((Long) LogcatManagerService.this.mClock.get()).longValue());
        }

        public void finishThread(int uid, int gid, int pid, int fd) {
            LogAccessRequest logAccessRequest = new LogAccessRequest(uid, gid, pid, fd);
            Message msg = LogcatManagerService.this.mHandler.obtainMessage(3, logAccessRequest);
            LogcatManagerService.this.mHandler.sendMessageAtTime(msg, ((Long) LogcatManagerService.this.mClock.get()).longValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class LogcatManagerServiceInternal {
        LogcatManagerServiceInternal() {
        }

        public void approveAccessForClient(int uid, String packageName) {
            LogAccessClient client = new LogAccessClient(uid, packageName);
            Message msg = LogcatManagerService.this.mHandler.obtainMessage(1, client);
            LogcatManagerService.this.mHandler.sendMessageAtTime(msg, ((Long) LogcatManagerService.this.mClock.get()).longValue());
        }

        public void declineAccessForClient(int uid, String packageName) {
            LogAccessClient client = new LogAccessClient(uid, packageName);
            Message msg = LogcatManagerService.this.mHandler.obtainMessage(2, client);
            LogcatManagerService.this.mHandler.sendMessageAtTime(msg, ((Long) LogcatManagerService.this.mClock.get()).longValue());
        }
    }

    private ILogd getLogdService() {
        if (this.mLogdService == null) {
            this.mLogdService = this.mInjector.getLogdService();
        }
        return this.mLogdService;
    }

    /* loaded from: classes.dex */
    private static class LogAccessRequestHandler extends Handler {
        private final LogcatManagerService mService;

        LogAccessRequestHandler(Looper looper, LogcatManagerService service) {
            super(looper);
            this.mService = service;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    LogAccessRequest request = (LogAccessRequest) msg.obj;
                    this.mService.onLogAccessRequested(request);
                    return;
                case 1:
                    LogAccessClient client = (LogAccessClient) msg.obj;
                    this.mService.onAccessApprovedForClient(client);
                    return;
                case 2:
                    LogAccessClient client2 = (LogAccessClient) msg.obj;
                    this.mService.onAccessDeclinedForClient(client2);
                    return;
                case 3:
                    LogAccessRequest request2 = (LogAccessRequest) msg.obj;
                    this.mService.onLogAccessFinished(request2);
                    return;
                case 4:
                    LogAccessClient client3 = (LogAccessClient) msg.obj;
                    this.mService.onPendingTimeoutExpired(client3);
                    return;
                case 5:
                    LogAccessClient client4 = (LogAccessClient) msg.obj;
                    this.mService.onAccessStatusExpired(client4);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        protected Supplier<Long> createClock() {
            return new Supplier() { // from class: com.android.server.logcat.LogcatManagerService$Injector$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return Long.valueOf(SystemClock.uptimeMillis());
                }
            };
        }

        protected Looper getLooper() {
            return Looper.getMainLooper();
        }

        protected ILogd getLogdService() {
            return ILogd.Stub.asInterface(ServiceManager.getService("logd"));
        }
    }

    public LogcatManagerService(Context context) {
        this(context, new Injector());
    }

    public LogcatManagerService(Context context, Injector injector) {
        super(context);
        this.mLogAccessStatus = new ArrayMap();
        this.mActiveLogAccessCount = new ArrayMap();
        this.mContext = context;
        this.mInjector = injector;
        this.mClock = injector.createClock();
        this.mBinderService = new BinderService();
        this.mLocalService = new LogcatManagerServiceInternal();
        this.mHandler = new LogAccessRequestHandler(injector.getLooper(), this);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        try {
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            publishBinderService("logcat", this.mBinderService);
            publishLocalService(LogcatManagerServiceInternal.class, this.mLocalService);
        } catch (Throwable t) {
            Slog.e(TAG, "Could not start the LogcatManagerService.", t);
        }
    }

    LogcatManagerServiceInternal getLocalService() {
        return this.mLocalService;
    }

    ILogcatManagerService getBinderService() {
        return this.mBinderService;
    }

    private LogAccessClient getClientForRequest(LogAccessRequest request) {
        String packageName = getPackageName(request);
        if (packageName == null) {
            return null;
        }
        return new LogAccessClient(request.mUid, packageName);
    }

    private String getPackageName(LogAccessRequest request) {
        String packageName;
        ActivityManagerInternal activityManagerInternal = this.mActivityManagerInternal;
        if (activityManagerInternal != null && (packageName = activityManagerInternal.getPackageNameByPid(request.mPid)) != null) {
            return packageName;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Slog.e(TAG, "PackageManager is null, declining the logd access");
            return null;
        }
        String[] packageNames = pm.getPackagesForUid(request.mUid);
        if (ArrayUtils.isEmpty(packageNames)) {
            Slog.e(TAG, "Unknown calling package name, declining the logd access");
            return null;
        }
        String firstPackageName = packageNames[0];
        if (firstPackageName == null || firstPackageName.isEmpty()) {
            Slog.e(TAG, "Unknown calling package name, declining the logd access");
            return null;
        }
        return firstPackageName;
    }

    void onLogAccessRequested(LogAccessRequest request) {
        LogAccessClient client = getClientForRequest(request);
        if (client == null) {
            declineRequest(request);
            return;
        }
        LogAccessStatus logAccessStatus = this.mLogAccessStatus.get(client);
        if (logAccessStatus == null) {
            logAccessStatus = new LogAccessStatus();
            this.mLogAccessStatus.put(client, logAccessStatus);
        }
        switch (logAccessStatus.mStatus) {
            case 0:
                logAccessStatus.mPendingRequests.add(request);
                processNewLogAccessRequest(client);
                return;
            case 1:
                logAccessStatus.mPendingRequests.add(request);
                return;
            case 2:
                approveRequest(client, request);
                return;
            case 3:
                declineRequest(request);
                return;
            default:
                return;
        }
    }

    private boolean shouldShowConfirmationDialog(LogAccessClient client) {
        int procState = this.mActivityManagerInternal.getUidProcessState(client.mUid);
        return procState == 2;
    }

    private void processNewLogAccessRequest(LogAccessClient client) {
        boolean isInstrumented = this.mActivityManagerInternal.getInstrumentationSourceUid(client.mUid) != -1;
        if (isInstrumented) {
            onAccessApprovedForClient(client);
        } else if (!shouldShowConfirmationDialog(client)) {
            onAccessDeclinedForClient(client);
        } else {
            LogAccessStatus logAccessStatus = this.mLogAccessStatus.get(client);
            logAccessStatus.mStatus = 1;
            Handler handler = this.mHandler;
            handler.sendMessageAtTime(handler.obtainMessage(4, client), this.mClock.get().longValue() + PENDING_CONFIRMATION_TIMEOUT_MILLIS);
            Intent mIntent = createIntent(client);
            this.mContext.startActivityAsUser(mIntent, UserHandle.SYSTEM);
        }
    }

    void onAccessApprovedForClient(LogAccessClient client) {
        scheduleStatusExpiry(client);
        LogAccessStatus logAccessStatus = this.mLogAccessStatus.get(client);
        if (logAccessStatus != null) {
            for (LogAccessRequest request : logAccessStatus.mPendingRequests) {
                approveRequest(client, request);
            }
            logAccessStatus.mStatus = 2;
            logAccessStatus.mPendingRequests.clear();
        }
    }

    void onAccessDeclinedForClient(LogAccessClient client) {
        scheduleStatusExpiry(client);
        LogAccessStatus logAccessStatus = this.mLogAccessStatus.get(client);
        if (logAccessStatus != null) {
            for (LogAccessRequest request : logAccessStatus.mPendingRequests) {
                declineRequest(request);
            }
            logAccessStatus.mStatus = 3;
            logAccessStatus.mPendingRequests.clear();
        }
    }

    private void scheduleStatusExpiry(LogAccessClient client) {
        this.mHandler.removeMessages(4, client);
        this.mHandler.removeMessages(5, client);
        Handler handler = this.mHandler;
        handler.sendMessageAtTime(handler.obtainMessage(5, client), this.mClock.get().longValue() + 60000);
    }

    void onPendingTimeoutExpired(LogAccessClient client) {
        LogAccessStatus logAccessStatus = this.mLogAccessStatus.get(client);
        if (logAccessStatus != null && logAccessStatus.mStatus == 1) {
            onAccessDeclinedForClient(client);
        }
    }

    void onAccessStatusExpired(LogAccessClient client) {
        this.mLogAccessStatus.remove(client);
    }

    void onLogAccessFinished(LogAccessRequest request) {
        LogAccessClient client = getClientForRequest(request);
        int activeCount = this.mActiveLogAccessCount.getOrDefault(client, 1).intValue() - 1;
        if (activeCount == 0) {
            this.mActiveLogAccessCount.remove(client);
        } else {
            this.mActiveLogAccessCount.put(client, Integer.valueOf(activeCount));
        }
    }

    private void approveRequest(LogAccessClient client, LogAccessRequest request) {
        try {
            getLogdService().approve(request.mUid, request.mGid, request.mPid, request.mFd);
            Integer activeCount = this.mActiveLogAccessCount.getOrDefault(client, 0);
            this.mActiveLogAccessCount.put(client, Integer.valueOf(activeCount.intValue() + 1));
        } catch (RemoteException e) {
            Slog.e(TAG, "Fails to call remote functions", e);
        }
    }

    private void declineRequest(LogAccessRequest request) {
        try {
            getLogdService().decline(request.mUid, request.mGid, request.mPid, request.mFd);
        } catch (RemoteException e) {
            Slog.e(TAG, "Fails to call remote functions", e);
        }
    }

    public Intent createIntent(LogAccessClient client) {
        Intent intent = new Intent(this.mContext, LogAccessDialogActivity.class);
        intent.setFlags(268468224);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", client.mPackageName);
        intent.putExtra("android.intent.extra.UID", client.mUid);
        return intent;
    }
}
