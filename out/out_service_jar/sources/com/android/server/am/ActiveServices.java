package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.IApplicationThread;
import android.app.IForegroundServiceObserver;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteServiceException;
import android.app.ServiceStartArgs;
import android.app.admin.DevicePolicyEventLogger;
import android.app.compat.CompatChanges;
import android.appwidget.AppWidgetManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerExemptionManager;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.webkit.WebViewZygote;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import com.android.server.SystemUtil;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ComponentAliasResolver;
import com.android.server.am.ServiceRecord;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.uri.NeededUriGrants;
import com.android.server.usage.AppStandbyController;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.ActivityServiceConnectionsHolder;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranActiveServices;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class ActiveServices {
    private static final SimpleDateFormat DATE_FORMATTER;
    private static final boolean DEBUG_DELAYED_SERVICE;
    private static final boolean DEBUG_DELAYED_STARTS;
    static final long FGS_BG_START_RESTRICTION_CHANGE_ID = 170668199;
    static final int FGS_IMMEDIATE_DISPLAY_MASK = 54;
    static final long FGS_START_EXCEPTION_CHANGE_ID = 174041399;
    static final int FGS_STOP_REASON_STOP_FOREGROUND = 1;
    static final int FGS_STOP_REASON_STOP_SERVICE = 2;
    static final int FGS_STOP_REASON_UNKNOWN = 0;
    public static final boolean IS_ROOT_ENABLE;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    private static final boolean LOG_SERVICE_START_STOP = false;
    static final int SERVICE_BACKGROUND_TIMEOUT;
    static final int SERVICE_TIMEOUT;
    private static final boolean SHOW_DUNGEON_NOTIFICATION = false;
    private static final String TAG_MU = "ActivityManager_MU";
    final ActivityManagerService mAm;
    AppStateTracker mAppStateTracker;
    AppWidgetManagerInternal mAppWidgetManagerInternal;
    private TranProcessWrapper mCallerProc;
    String mLastAnrDump;
    final int mMaxStartingBackground;
    SystemUtil mStl;
    private static final String TAG = "ActivityManager";
    private static final String TAG_SERVICE = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE;
    private static final String TAG_SERVICE_EXECUTING = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE_EXECUTING;
    final SparseArray<ServiceMap> mServiceMap = new SparseArray<>();
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> mServiceConnections = new ArrayMap<>();
    final ArrayList<ServiceRecord> mPendingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mRestartingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mDestroyingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mPendingFgsNotifications = new ArrayList<>();
    private boolean mFgsDeferralRateLimited = true;
    final SparseLongArray mFgsDeferralEligible = new SparseLongArray();
    final RemoteCallbackList<IForegroundServiceObserver> mFgsObservers = new RemoteCallbackList<>();
    private ArrayMap<ServiceRecord, ArrayList<Runnable>> mPendingBringups = new ArrayMap<>();
    private ArrayList<ServiceRecord> mTmpCollectionResults = null;
    private final SparseArray<AppOpCallback> mFgsAppOpCallbacks = new SparseArray<>();
    private final ArraySet<String> mRestartBackoffDisabledPackages = new ArraySet<>();
    boolean mScreenOn = true;
    ArraySet<String> mAllowListWhileInUsePermissionInFgs = new ArraySet<>();
    final Runnable mLastAnrDumpClearer = new Runnable() { // from class: com.android.server.am.ActiveServices.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.mLastAnrDump = null;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    };
    private final Runnable mPostDeferredFGSNotifications = new Runnable() { // from class: com.android.server.am.ActiveServices.5
        @Override // java.lang.Runnable
        public void run() {
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(ActiveServices.TAG_SERVICE, "+++ evaluating deferred FGS notifications +++");
            }
            long now = SystemClock.uptimeMillis();
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (int i = ActiveServices.this.mPendingFgsNotifications.size() - 1; i >= 0; i--) {
                        ServiceRecord r = ActiveServices.this.mPendingFgsNotifications.get(i);
                        if (r.fgDisplayTime <= now) {
                            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                Slog.d(ActiveServices.TAG_SERVICE, "FGS " + r + " handling deferred notification now");
                            }
                            ActiveServices.this.mPendingFgsNotifications.remove(i);
                            if (r.isForeground && r.app != null) {
                                r.postNotification();
                                r.mFgsNotificationShown = true;
                            } else if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                Slog.d(ActiveServices.TAG_SERVICE, "  - service no longer running/fg, ignoring");
                            }
                        }
                    }
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(ActiveServices.TAG_SERVICE, "Done evaluating deferred FGS notifications; " + ActiveServices.this.mPendingFgsNotifications.size() + " remaining");
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface FgsStopReason {
    }

    static {
        boolean z = ActivityManagerDebugConfig.DEBUG_SERVICE;
        DEBUG_DELAYED_SERVICE = z;
        DEBUG_DELAYED_STARTS = z;
        int i = Build.HW_TIMEOUT_MULTIPLIER * 20000;
        SERVICE_TIMEOUT = i;
        SERVICE_BACKGROUND_TIMEOUT = i * 10;
        DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        IS_ROOT_ENABLE = "1".equals(SystemProperties.get("persist.user.root.support", "0"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class BackgroundRestrictedListener implements AppStateTracker.BackgroundRestrictedAppListener {
        BackgroundRestrictedListener() {
        }

        public void updateBackgroundRestrictedForUidPackage(int uid, String packageName, boolean restricted) {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (!ActiveServices.this.isForegroundServiceAllowedInBackgroundRestricted(uid, packageName)) {
                        ActiveServices.this.stopAllForegroundServicesLocked(uid, packageName);
                    }
                    ActiveServices.this.mAm.mProcessList.updateBackgroundRestrictedForUidPackageLocked(uid, packageName, restricted);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopAllForegroundServicesLocked(int uid, String packageName) {
        ServiceMap smap = getServiceMapLocked(UserHandle.getUserId(uid));
        int N = smap.mServicesByInstanceName.size();
        ArrayList<ServiceRecord> toStop = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            ServiceRecord r = smap.mServicesByInstanceName.valueAt(i);
            if ((uid == r.serviceInfo.applicationInfo.uid || packageName.equals(r.serviceInfo.packageName)) && r.isForeground) {
                toStop.add(r);
            }
        }
        int numToStop = toStop.size();
        if (numToStop > 0 && ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            Slog.i(TAG, "Package " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + " in FAS with foreground services");
        }
        for (int i2 = 0; i2 < numToStop; i2++) {
            ServiceRecord r2 = toStop.get(i2);
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.i(TAG, "  Stopping fg for service " + r2);
            }
            setServiceForegroundInnerLocked(r2, 0, null, 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ActiveForegroundApp {
        boolean mAppOnTop;
        long mEndTime;
        long mHideTime;
        CharSequence mLabel;
        int mNumActive;
        String mPackageName;
        boolean mShownWhileScreenOn;
        boolean mShownWhileTop;
        long mStartTime;
        long mStartVisibleTime;
        int mUid;

        ActiveForegroundApp() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ServiceMap extends Handler {
        static final int MSG_BG_START_TIMEOUT = 1;
        static final int MSG_ENSURE_NOT_START_BG = 3;
        static final int MSG_UPDATE_FOREGROUND_APPS = 2;
        final ArrayMap<String, ActiveForegroundApp> mActiveForegroundApps;
        boolean mActiveForegroundAppsChanged;
        final ArrayList<ServiceRecord> mDelayedStartList;
        final ArrayMap<ComponentName, ServiceRecord> mServicesByInstanceName;
        final ArrayMap<Intent.FilterComparison, ServiceRecord> mServicesByIntent;
        final ArrayList<ServiceRecord> mStartingBackground;
        final int mUserId;

        ServiceMap(Looper looper, int userId) {
            super(looper);
            this.mServicesByInstanceName = new ArrayMap<>();
            this.mServicesByIntent = new ArrayMap<>();
            this.mDelayedStartList = new ArrayList<>();
            this.mStartingBackground = new ArrayList<>();
            this.mActiveForegroundApps = new ArrayMap<>();
            this.mUserId = userId;
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            rescheduleDelayedStartsLocked();
                        } finally {
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 2:
                    ActiveServices.this.updateForegroundApps(this);
                    return;
                case 3:
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            rescheduleDelayedStartsLocked();
                        } finally {
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }

        void ensureNotStartingBackgroundLocked(ServiceRecord r) {
            if (this.mStartingBackground.remove(r)) {
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "No longer background starting: " + r);
                }
                removeMessages(3);
                Message msg = obtainMessage(3);
                sendMessage(msg);
            }
            if (!this.mDelayedStartList.remove(r) || !ActiveServices.DEBUG_DELAYED_STARTS) {
                return;
            }
            Slog.v(ActiveServices.TAG_SERVICE, "No longer delaying start: " + r);
        }

        void rescheduleDelayedStartsLocked() {
            removeMessages(1);
            long now = SystemClock.uptimeMillis();
            int i = 0;
            int N = this.mStartingBackground.size();
            while (i < N) {
                ServiceRecord r = this.mStartingBackground.get(i);
                if (r.startingBgTimeout <= now) {
                    Slog.i(ActiveServices.TAG, "Waited long enough for: " + r);
                    this.mStartingBackground.remove(i);
                    N--;
                    i--;
                }
                i++;
            }
            while (this.mDelayedStartList.size() > 0 && this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ServiceRecord r2 = this.mDelayedStartList.remove(0);
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "REM FR DELAY LIST (exec next): " + r2);
                }
                if (ActiveServices.DEBUG_DELAYED_SERVICE && this.mDelayedStartList.size() > 0) {
                    Slog.v(ActiveServices.TAG_SERVICE, "Remaining delayed list:");
                    for (int i2 = 0; i2 < this.mDelayedStartList.size(); i2++) {
                        Slog.v(ActiveServices.TAG_SERVICE, "  #" + i2 + ": " + this.mDelayedStartList.get(i2));
                    }
                }
                r2.delayed = false;
                if (r2.pendingStarts.size() <= 0) {
                    Slog.wtf(ActiveServices.TAG, "**** NO PENDING STARTS! " + r2 + " startReq=" + r2.startRequested + " delayedStop=" + r2.delayedStop);
                } else {
                    try {
                        ServiceRecord.StartItem si = r2.pendingStarts.get(0);
                        ActiveServices.this.startServiceInnerLocked(this, si.intent, r2, false, true, si.callingId, r2.startRequested);
                    } catch (TransactionTooLargeException e) {
                    }
                }
            }
            if (this.mStartingBackground.size() > 0) {
                ServiceRecord next = this.mStartingBackground.get(0);
                long when = next.startingBgTimeout > now ? next.startingBgTimeout : now;
                if (ActiveServices.DEBUG_DELAYED_SERVICE) {
                    Slog.v(ActiveServices.TAG_SERVICE, "Top bg start is " + next + ", can delay others up to " + when);
                }
                Message msg = obtainMessage(1);
                sendMessageAtTime(msg, when);
            }
            if (this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ActiveServices.this.mAm.backgroundServicesFinishedLocked(this.mUserId);
            }
        }
    }

    public ActiveServices(ActivityManagerService service) {
        int i = 1;
        this.mAm = service;
        int maxBg = 0;
        try {
            maxBg = Integer.parseInt(SystemProperties.get("ro.config.max_starting_bg", "0"));
        } catch (RuntimeException e) {
        }
        if (maxBg > 0) {
            i = maxBg;
        } else if (!ActivityManager.isLowRamDeviceStatic()) {
            i = 8;
        }
        this.mMaxStartingBackground = i;
        ServiceManager.getService("platform_compat");
        if (IS_ROOT_ENABLE) {
            HandlerThread handlerThread = new HandlerThread("KillProcessHandlerOomAdjuster");
            handlerThread.start();
            this.mStl = new SystemUtil(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemServicesReady() {
        getAppStateTracker().addBackgroundRestrictedAppListener(new BackgroundRestrictedListener());
        this.mAppWidgetManagerInternal = (AppWidgetManagerInternal) LocalServices.getService(AppWidgetManagerInternal.class);
        setAllowListWhileInUsePermissionInFgs();
    }

    private AppStateTracker getAppStateTracker() {
        if (this.mAppStateTracker == null) {
            this.mAppStateTracker = (AppStateTracker) LocalServices.getService(AppStateTracker.class);
        }
        return this.mAppStateTracker;
    }

    private void setAllowListWhileInUsePermissionInFgs() {
        String attentionServicePackageName = this.mAm.mContext.getPackageManager().getAttentionServicePackageName();
        if (!TextUtils.isEmpty(attentionServicePackageName)) {
            this.mAllowListWhileInUsePermissionInFgs.add(attentionServicePackageName);
        }
        String systemCaptionsServicePackageName = this.mAm.mContext.getPackageManager().getSystemCaptionsServicePackageName();
        if (!TextUtils.isEmpty(systemCaptionsServicePackageName)) {
            this.mAllowListWhileInUsePermissionInFgs.add(systemCaptionsServicePackageName);
        }
    }

    ServiceRecord getServiceByNameLocked(ComponentName name, int callingUser) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "getServiceByNameLocked(" + name + "), callingUser = " + callingUser);
        }
        return getServiceMapLocked(callingUser).mServicesByInstanceName.get(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasBackgroundServicesLocked(int callingUser) {
        ServiceMap smap = this.mServiceMap.get(callingUser);
        return smap != null && smap.mStartingBackground.size() >= this.mMaxStartingBackground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServiceNotificationLocked(String pkg, int userId, String channelId) {
        ServiceMap smap = this.mServiceMap.get(userId);
        if (smap != null) {
            for (int i = 0; i < smap.mServicesByInstanceName.size(); i++) {
                ServiceRecord sr = smap.mServicesByInstanceName.valueAt(i);
                if (sr.appInfo.packageName.equals(pkg) && sr.isForeground && Objects.equals(sr.foregroundNoti.getChannelId(), channelId)) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Channel u" + userId + "/pkg=" + pkg + "/channelId=" + channelId + " has fg service notification");
                        return true;
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopForegroundServicesForChannelLocked(String pkg, int userId, String channelId) {
        ServiceMap smap = this.mServiceMap.get(userId);
        if (smap != null) {
            for (int i = 0; i < smap.mServicesByInstanceName.size(); i++) {
                ServiceRecord sr = smap.mServicesByInstanceName.valueAt(i);
                if (sr.appInfo.packageName.equals(pkg) && sr.isForeground && Objects.equals(sr.foregroundNoti.getChannelId(), channelId)) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Stopping FGS u" + userId + "/pkg=" + pkg + "/channelId=" + channelId + " for conversation channel clear");
                    }
                    stopServiceLocked(sr, false);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ServiceMap getServiceMapLocked(int callingUser) {
        ServiceMap smap = this.mServiceMap.get(callingUser);
        if (smap == null) {
            ServiceMap smap2 = new ServiceMap(this.mAm.mHandler.getLooper(), callingUser);
            this.mServiceMap.put(callingUser, smap2);
            return smap2;
        }
        return smap;
    }

    ArrayMap<ComponentName, ServiceRecord> getServicesLocked(int callingUser) {
        return getServiceMapLocked(callingUser).mServicesByInstanceName;
    }

    private boolean appRestrictedAnyInBackground(int uid, String packageName) {
        AppStateTracker appStateTracker = getAppStateTracker();
        if (appStateTracker != null) {
            return appStateTracker.isAppBackgroundRestricted(uid, packageName);
        }
        return false;
    }

    void updateAppRestrictedAnyInBackgroundLocked(int uid, String packageName) {
        ProcessRecord app;
        boolean restricted = appRestrictedAnyInBackground(uid, packageName);
        UidRecord uidRec = this.mAm.mProcessList.getUidRecordLOSP(uid);
        if (uidRec != null && (app = uidRec.getProcessInPackage(packageName)) != null) {
            app.mState.setBackgroundRestricted(restricted);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, boolean fgRequired, String callingPackage, String callingFeatureId, int userId) throws TransactionTooLargeException {
        return startServiceLocked(caller, service, resolvedType, callingPid, callingUid, fgRequired, callingPackage, callingFeatureId, userId, false, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:72:0x0278  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x02ff  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, boolean fgRequired, String callingPackage, String callingFeatureId, int userId, boolean allowBackgroundActivityStarts, IBinder backgroundActivityStartsToken) throws TransactionTooLargeException {
        boolean callerFg;
        boolean callerFg2;
        boolean forcedStandby;
        String str;
        boolean forceSilentAbort;
        boolean callerFg3;
        boolean forceSilentAbort2;
        boolean callerFg4;
        ServiceLookupResult res;
        String str2;
        int allowed;
        String str3;
        boolean fgRequired2;
        boolean fgRequired3;
        if (DEBUG_DELAYED_STARTS) {
            Slog.v(TAG_SERVICE, "startService: " + service + " type=" + resolvedType + " args=" + service.getExtras());
        }
        if (caller != null) {
            ProcessRecord callerApp = this.mAm.getRecordForAppLOSP(caller);
            if (callerApp == null) {
                throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting service " + service);
            }
            boolean callerFg5 = callerApp.mState.getSetSchedGroup() != 0;
            this.mCallerProc = callerApp.processWrapper;
            callerFg = callerFg5;
        } else {
            this.mCallerProc = null;
            callerFg = true;
        }
        boolean callerFg6 = callerFg;
        ServiceLookupResult res2 = retrieveServiceLocked(service, null, resolvedType, callingPackage, callingPid, callingUid, userId, true, callerFg6, false, false);
        if (res2 == null) {
            return null;
        }
        if (res2.record == null) {
            return new ComponentName("!", res2.permission != null ? res2.permission : "private to package");
        }
        ServiceRecord r = res2.record;
        setFgsRestrictionLocked(callingPackage, callingPid, callingUid, service, r, userId, allowBackgroundActivityStarts);
        if (!this.mAm.mUserController.exists(r.userId)) {
            Slog.w(TAG, "Trying to start service with non-existent user! " + r.userId);
            return null;
        }
        boolean bgLaunch = !this.mAm.isUidActiveLOSP(r.appInfo.uid);
        if (!bgLaunch || !appRestrictedAnyInBackground(r.appInfo.uid, r.packageName)) {
            callerFg2 = callerFg6;
            forcedStandby = false;
        } else {
            if (!ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                callerFg2 = callerFg6;
            } else {
                callerFg2 = callerFg6;
                Slog.d(TAG, "Forcing bg-only service start only for " + r.shortInstanceName + " : bgLaunch=" + bgLaunch + " callerFg=" + callerFg2);
            }
            forcedStandby = true;
        }
        if (fgRequired) {
            logFgsBackgroundStart(r);
            if (r.mAllowStartForeground == -1 && isBgFgsRestrictionEnabled(r)) {
                String msg = "startForegroundService() not allowed due to mAllowStartForeground false: service " + r.shortInstanceName;
                Slog.w(TAG, msg);
                showFgsBgRestrictedNotificationLocked(r);
                logFGSStateChangeLocked(r, 3, 0, 0);
                if (CompatChanges.isChangeEnabled((long) FGS_START_EXCEPTION_CHANGE_ID, callingUid)) {
                    throw new ForegroundServiceStartNotAllowedException(msg);
                }
                return null;
            }
        }
        if (fgRequired) {
            forceSilentAbort = false;
            callerFg3 = callerFg2;
            int mode = this.mAm.getAppOpsManager().checkOpNoThrow(76, r.appInfo.uid, r.packageName);
            switch (mode) {
                case 0:
                case 3:
                    str = callingPackage;
                    break;
                case 1:
                    str = callingPackage;
                    Slog.w(TAG, "startForegroundService not allowed due to app op: service " + service + " to " + r.shortInstanceName + " from pid=" + callingPid + " uid=" + callingUid + " pkg=" + str);
                    forceSilentAbort = true;
                    forceSilentAbort2 = false;
                    break;
                case 2:
                default:
                    return new ComponentName("!!", "foreground not allowed as per app op");
            }
            if (forcedStandby && (r.startRequested || forceSilentAbort2)) {
                str2 = "!";
                callerFg4 = callerFg3;
                res = res2;
                str3 = "?";
                fgRequired2 = forceSilentAbort2;
            } else {
                callerFg4 = callerFg3;
                res = res2;
                boolean fgRequired4 = forceSilentAbort2;
                str2 = "!";
                String str4 = str;
                allowed = this.mAm.getAppStartModeLOSP(r.appInfo.uid, r.packageName, r.appInfo.targetSdkVersion, callingPid, false, false, forcedStandby);
                if (allowed == 0) {
                    Slog.w(TAG, "Background start not allowed: service " + service + " to " + r.shortInstanceName + " from pid=" + callingPid + " uid=" + callingUid + " pkg=" + str4 + " startFg?=" + fgRequired4);
                    if (allowed == 1 || forceSilentAbort) {
                        return null;
                    }
                    if (forcedStandby && fgRequired4) {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.v(TAG, "Silently dropping foreground service launch due to FAS");
                        }
                        return null;
                    }
                    UidRecord uidRec = this.mAm.mProcessList.getUidRecordLOSP(r.appInfo.uid);
                    return new ComponentName("?", "app is in background uid " + uidRec);
                }
                str3 = "?";
                fgRequired2 = fgRequired4;
            }
            if (r.appInfo.targetSdkVersion < 26 && fgRequired2) {
                if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK || ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.i(TAG, "startForegroundService() but host targets " + r.appInfo.targetSdkVersion + " - not requiring startForeground()");
                }
                fgRequired3 = false;
            } else {
                fgRequired3 = fgRequired2;
            }
            String str5 = str2;
            String str6 = str3;
            ServiceLookupResult res3 = res;
            if (!deferServiceBringupIfFrozenLocked(r, service, callingPackage, callingFeatureId, callingUid, callingPid, fgRequired3, callerFg4, userId, allowBackgroundActivityStarts, backgroundActivityStartsToken, false, null) && requestStartTargetPermissionsReviewIfNeededLocked(r, callingPackage, callingFeatureId, callingUid, service, callerFg4, userId, false, null)) {
                ComponentName realResult = startServiceInnerLocked(r, service, callingUid, callingPid, fgRequired3, callerFg4, allowBackgroundActivityStarts, backgroundActivityStartsToken);
                if (res3.aliasComponent != null && !realResult.getPackageName().startsWith(str5) && !realResult.getPackageName().startsWith(str6)) {
                    return res3.aliasComponent;
                }
                return realResult;
            }
            return null;
        }
        str = callingPackage;
        forceSilentAbort = false;
        callerFg3 = callerFg2;
        forceSilentAbort2 = fgRequired;
        if (forcedStandby) {
        }
        callerFg4 = callerFg3;
        res = res2;
        boolean fgRequired42 = forceSilentAbort2;
        str2 = "!";
        String str42 = str;
        allowed = this.mAm.getAppStartModeLOSP(r.appInfo.uid, r.packageName, r.appInfo.targetSdkVersion, callingPid, false, false, forcedStandby);
        if (allowed == 0) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ComponentName startServiceInnerLocked(ServiceRecord r, Intent service, int callingUid, int callingPid, boolean fgRequired, boolean callerFg, boolean allowBackgroundActivityStarts, IBinder backgroundActivityStartsToken) throws TransactionTooLargeException {
        boolean addToStarting;
        NeededUriGrants neededGrants = this.mAm.mUgmInternal.checkGrantUriPermissionFromIntent(service, callingUid, r.packageName, r.userId);
        if (unscheduleServiceRestartLocked(r, callingUid, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "START SERVICE WHILE RESTART PENDING: " + r);
        }
        boolean wasStartRequested = r.startRequested;
        r.lastActivity = SystemClock.uptimeMillis();
        r.startRequested = true;
        r.delayedStop = false;
        r.fgRequired = fgRequired;
        r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), service, neededGrants, callingUid));
        if (fgRequired) {
            synchronized (this.mAm.mProcessStats.mLock) {
                ServiceState stracker = r.getTracker();
                if (stracker != null) {
                    stracker.setForeground(true, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
            }
            this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null, true, false, null, false, 0, -1);
        }
        ServiceMap smap = getServiceMapLocked(r.userId);
        boolean addToStarting2 = false;
        if (!callerFg && !fgRequired && r.app == null && this.mAm.mUserController.hasStartedUserState(r.userId)) {
            ProcessRecord proc = this.mAm.getProcessRecordLocked(r.processName, r.appInfo.uid);
            if (proc == null || proc.mState.getCurProcState() > 11) {
                if (DEBUG_DELAYED_SERVICE) {
                    Slog.v(TAG_SERVICE, "Potential start delay of " + r + " in " + proc);
                }
                if (!r.delayed) {
                    if (smap.mStartingBackground.size() >= this.mMaxStartingBackground) {
                        Slog.i(TAG_SERVICE, "Delaying start of: " + r);
                        smap.mDelayedStartList.add(r);
                        r.delayed = true;
                        return r.name;
                    }
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Not delaying: " + r);
                    }
                    addToStarting2 = true;
                } else {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Continuing to delay: " + r);
                    }
                    return r.name;
                }
            } else if (proc.mState.getCurProcState() >= 10) {
                addToStarting2 = true;
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Not delaying, but counting as bg: " + r);
                }
            } else if (DEBUG_DELAYED_STARTS) {
                StringBuilder sb = new StringBuilder(128);
                sb.append("Not potential delay (state=").append(proc.mState.getCurProcState()).append(' ').append(proc.mState.getAdjType());
                String reason = proc.mState.makeAdjReason();
                if (reason != null) {
                    sb.append(' ');
                    sb.append(reason);
                }
                sb.append("): ");
                sb.append(r.toString());
                Slog.v(TAG_SERVICE, sb.toString());
            }
            addToStarting = addToStarting2;
        } else {
            if (DEBUG_DELAYED_STARTS) {
                if (callerFg || fgRequired) {
                    Slog.v(TAG_SERVICE, "Not potential delay (callerFg=" + callerFg + " uid=" + callingUid + " pid=" + callingPid + " fgRequired=" + fgRequired + "): " + r);
                } else if (r.app != null) {
                    Slog.v(TAG_SERVICE, "Not potential delay (cur app=" + r.app + "): " + r);
                } else {
                    Slog.v(TAG_SERVICE, "Not potential delay (user " + r.userId + " not started): " + r);
                }
            }
            addToStarting = false;
        }
        if (allowBackgroundActivityStarts) {
            r.allowBgActivityStartsOnServiceStart(backgroundActivityStartsToken);
        }
        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
            ComponentName cmp = startServiceInnerLocked(smap, service, r, callerFg, addToStarting, callingUid, wasStartRequested, this.mCallerProc, 1);
            this.mCallerProc = null;
            return cmp;
        }
        ComponentName cmp2 = startServiceInnerLocked(smap, service, r, callerFg, addToStarting, callingUid, wasStartRequested);
        return cmp2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean requestStartTargetPermissionsReviewIfNeededLocked(final ServiceRecord r, String callingPackage, String callingFeatureId, int callingUid, final Intent service, final boolean callerFg, final int userId, boolean isBinding, final IServiceConnection connection) {
        if (this.mAm.getPackageManagerInternal().isPermissionsReviewRequired(r.packageName, r.userId)) {
            if (!callerFg) {
                Slog.w(TAG, "u" + r.userId + (isBinding ? " Binding" : " Starting") + " a service in package" + r.packageName + " requires a permissions review");
                return false;
            }
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(411041792);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", r.packageName);
            if (isBinding) {
                RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.am.ActiveServices.2
                    public void onResult(Bundle result) {
                        ActivityManagerService activityManagerService;
                        String str;
                        synchronized (ActiveServices.this.mAm) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                long identity = Binder.clearCallingIdentity();
                                if (ActiveServices.this.mPendingServices.contains(r)) {
                                    if (!ActiveServices.this.mAm.getPackageManagerInternal().isPermissionsReviewRequired(r.packageName, r.userId)) {
                                        try {
                                            ActiveServices.this.bringUpServiceLocked(r, service.getFlags(), callerFg, false, false, false, true);
                                            activityManagerService = ActiveServices.this.mAm;
                                            str = "updateOomAdj_startService";
                                        } catch (RemoteException e) {
                                            activityManagerService = ActiveServices.this.mAm;
                                            str = "updateOomAdj_startService";
                                        } catch (Throwable th) {
                                            ActiveServices.this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
                                            throw th;
                                        }
                                        activityManagerService.updateOomAdjPendingTargetsLocked(str);
                                    } else {
                                        ActiveServices.this.unbindServiceLocked(connection);
                                    }
                                    Binder.restoreCallingIdentity(identity);
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                Binder.restoreCallingIdentity(identity);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } catch (Throwable th2) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th2;
                            }
                        }
                    }
                });
                intent.putExtra("android.intent.extra.REMOTE_CALLBACK", (Parcelable) callback);
            } else {
                intent.putExtra("android.intent.extra.INTENT", new IntentSender(this.mAm.mPendingIntentController.getIntentSender(4, callingPackage, callingFeatureId, callingUid, userId, null, null, 0, new Intent[]{service}, new String[]{service.resolveType(this.mAm.mContext.getContentResolver())}, 1409286144, null)));
            }
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                Slog.i(TAG, "u" + r.userId + " Launching permission review for package " + r.packageName);
            }
            this.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.ActiveServices.3
                @Override // java.lang.Runnable
                public void run() {
                    ActiveServices.this.mAm.mContext.startActivityAsUser(intent, new UserHandle(userId));
                }
            });
            return false;
        }
        return true;
    }

    private boolean deferServiceBringupIfFrozenLocked(final ServiceRecord s, final Intent serviceIntent, final String callingPackage, final String callingFeatureId, final int callingUid, final int callingPid, final boolean fgRequired, final boolean callerFg, final int userId, final boolean allowBackgroundActivityStarts, final IBinder backgroundActivityStartsToken, final boolean isBinding, final IServiceConnection connection) {
        ArrayList<Runnable> curPendingBringups;
        PackageManagerInternal pm = this.mAm.getPackageManagerInternal();
        boolean frozen = pm.isPackageFrozen(s.packageName, callingUid, s.userId);
        if (!frozen) {
            return false;
        }
        ArrayList<Runnable> curPendingBringups2 = this.mPendingBringups.get(s);
        if (curPendingBringups2 != null) {
            curPendingBringups = curPendingBringups2;
        } else {
            ArrayList<Runnable> curPendingBringups3 = new ArrayList<>();
            this.mPendingBringups.put(s, curPendingBringups3);
            curPendingBringups = curPendingBringups3;
        }
        curPendingBringups.add(new Runnable() { // from class: com.android.server.am.ActiveServices.4
            @Override // java.lang.Runnable
            public void run() {
                ActivityManagerService activityManagerService;
                String str;
                synchronized (ActiveServices.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (!ActiveServices.this.mPendingBringups.containsKey(s)) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        } else if (!ActiveServices.this.requestStartTargetPermissionsReviewIfNeededLocked(s, callingPackage, callingFeatureId, callingUid, serviceIntent, callerFg, userId, isBinding, connection)) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        } else {
                            if (isBinding) {
                                try {
                                    ActiveServices.this.bringUpServiceLocked(s, serviceIntent.getFlags(), callerFg, false, false, false, true);
                                    activityManagerService = ActiveServices.this.mAm;
                                    str = "updateOomAdj_startService";
                                } catch (TransactionTooLargeException e) {
                                    activityManagerService = ActiveServices.this.mAm;
                                    str = "updateOomAdj_startService";
                                } catch (Throwable th) {
                                    ActiveServices.this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
                                    throw th;
                                }
                                activityManagerService.updateOomAdjPendingTargetsLocked(str);
                            } else {
                                try {
                                    ActiveServices.this.startServiceInnerLocked(s, serviceIntent, callingUid, callingPid, fgRequired, callerFg, allowBackgroundActivityStarts, backgroundActivityStartsToken);
                                } catch (TransactionTooLargeException e2) {
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    } catch (Throwable th2) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th2;
                    }
                }
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void schedulePendingServiceStartLocked(String packageName, int userId) {
        int totalPendings = this.mPendingBringups.size();
        int i = totalPendings - 1;
        while (i >= 0 && totalPendings > 0) {
            ServiceRecord r = this.mPendingBringups.keyAt(i);
            if (r.userId == userId && TextUtils.equals(r.packageName, packageName)) {
                ArrayList<Runnable> curPendingBringups = this.mPendingBringups.valueAt(i);
                if (curPendingBringups != null) {
                    for (int j = curPendingBringups.size() - 1; j >= 0; j--) {
                        curPendingBringups.get(j).run();
                    }
                    curPendingBringups.clear();
                }
                int curTotalPendings = this.mPendingBringups.size();
                this.mPendingBringups.remove(r);
                if (totalPendings != curTotalPendings) {
                    totalPendings = this.mPendingBringups.size();
                    i = totalPendings - 1;
                } else {
                    totalPendings = this.mPendingBringups.size();
                    i--;
                }
            } else {
                i--;
            }
        }
    }

    ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting, int callingUid, boolean wasStartRequested) throws TransactionTooLargeException {
        return startServiceInnerLocked(smap, service, r, callerFg, addToStarting, callingUid, wasStartRequested, null, -1);
    }

    ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting, int callingUid, boolean wasStartRequested, TranProcessWrapper callerProc, int callerType) throws TransactionTooLargeException {
        boolean z;
        int i;
        synchronized (this.mAm.mProcessStats.mLock) {
            ServiceState stracker = r.getTracker();
            z = true;
            if (stracker != null) {
                stracker.setStarted(true, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
            }
        }
        r.callStart = false;
        int uid = r.appInfo.uid;
        String packageName = r.name.getPackageName();
        String serviceName = r.name.getClassName();
        FrameworkStatsLog.write(99, uid, packageName, serviceName, 1);
        this.mAm.mBatteryStatsService.noteServiceStartRunning(uid, packageName, serviceName);
        String error = ITranGriffinFeature.Instance().isGriffinSupport() ? bringUpServiceLocked(r, service.getFlags(), callerFg, false, false, false, true, callerProc, callerType) : bringUpServiceLocked(r, service.getFlags(), callerFg, false, false, false, true);
        this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
        if (error != null) {
            return new ComponentName("!!", error);
        }
        String shortAction = ActivityManagerService.getShortAction(service.getAction());
        if (r.app == null || r.app.getThread() == null) {
            i = 3;
        } else if (wasStartRequested || !r.getConnections().isEmpty()) {
            i = 2;
        } else {
            i = 1;
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.SERVICE_REQUEST_EVENT_REPORTED, uid, callingUid, shortAction, 1, false, i);
        if (r.startRequested && addToStarting) {
            if (smap.mStartingBackground.size() != 0) {
                z = false;
            }
            boolean first = z;
            smap.mStartingBackground.add(r);
            r.startingBgTimeout = SystemClock.uptimeMillis() + this.mAm.mConstants.BG_START_TIMEOUT;
            if (DEBUG_DELAYED_SERVICE) {
                RuntimeException here = new RuntimeException("here");
                here.fillInStackTrace();
                Slog.v(TAG_SERVICE, "Starting background (first=" + first + "): " + r, here);
            } else if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Starting background (first=" + first + "): " + r);
            }
            if (first) {
                smap.rescheduleDelayedStartsLocked();
            }
        } else if (callerFg || r.fgRequired) {
            smap.ensureNotStartingBackgroundLocked(r);
        }
        return r.name;
    }

    private void stopServiceLocked(ServiceRecord service, boolean enqueueOomAdj) {
        if (service.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Delaying stop of pending: " + service);
            }
            service.delayedStop = true;
            return;
        }
        int uid = service.appInfo.uid;
        String packageName = service.name.getPackageName();
        String serviceName = service.name.getClassName();
        FrameworkStatsLog.write(99, uid, packageName, serviceName, 2);
        this.mAm.mBatteryStatsService.noteServiceStopRunning(uid, packageName, serviceName);
        service.startRequested = false;
        if (service.tracker != null) {
            synchronized (this.mAm.mProcessStats.mLock) {
                service.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
            }
        }
        service.callStart = false;
        bringDownServiceIfNeededLocked(service, false, false, enqueueOomAdj);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int stopServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopService: " + service + " type=" + resolvedType);
        }
        ProcessRecord callerApp = this.mAm.getRecordForAppLOSP(caller);
        if (caller != null && callerApp == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when stopping service " + service);
        }
        ServiceLookupResult r = retrieveServiceLocked(service, null, resolvedType, null, Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, false, false);
        if (r != null) {
            if (r.record != null) {
                long origId = Binder.clearCallingIdentity();
                try {
                    stopServiceLocked(r.record, false);
                    Binder.restoreCallingIdentity(origId);
                    return 1;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            }
            return -1;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopInBackgroundLocked(int uid) {
        ServiceMap services = this.mServiceMap.get(UserHandle.getUserId(uid));
        ArrayList<ServiceRecord> stopping = null;
        if (services != null) {
            for (int i = services.mServicesByInstanceName.size() - 1; i >= 0; i--) {
                ServiceRecord service = services.mServicesByInstanceName.valueAt(i);
                if (service.appInfo.uid == uid && service.startRequested && this.mAm.getAppStartModeLOSP(service.appInfo.uid, service.packageName, service.appInfo.targetSdkVersion, -1, false, false, false) != 0) {
                    if (stopping == null) {
                        stopping = new ArrayList<>();
                    }
                    String compName = service.shortInstanceName;
                    EventLogTags.writeAmStopIdleService(service.appInfo.uid, compName);
                    StringBuilder sb = new StringBuilder(64);
                    sb.append("Stopping service due to app idle: ");
                    UserHandle.formatUid(sb, service.appInfo.uid);
                    sb.append(" ");
                    TimeUtils.formatDuration(service.createRealTime - SystemClock.elapsedRealtime(), sb);
                    sb.append(" ");
                    sb.append(compName);
                    Slog.w(TAG, sb.toString());
                    stopping.add(service);
                    if (appRestrictedAnyInBackground(service.appInfo.uid, service.packageName)) {
                        cancelForegroundNotificationLocked(service);
                    }
                }
            }
            if (stopping != null) {
                int size = stopping.size();
                for (int i2 = size - 1; i2 >= 0; i2--) {
                    ServiceRecord service2 = stopping.get(i2);
                    service2.delayed = false;
                    services.ensureNotStartingBackgroundLocked(service2);
                    stopServiceLocked(service2, true);
                }
                if (size > 0) {
                    this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killMisbehavingService(ServiceRecord r, int appUid, int appPid, String localPackageName, int exceptionTypeId) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (!r.destroying) {
                    stopServiceLocked(r, false);
                } else {
                    ServiceMap smap = getServiceMapLocked(r.userId);
                    ServiceRecord found = smap.mServicesByInstanceName.remove(r.instanceName);
                    if (found != null) {
                        stopServiceLocked(found, false);
                    }
                }
                this.mAm.crashApplicationWithType(appUid, appPid, localPackageName, -1, "Bad notification for startForeground", true, exceptionTypeId);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder peekServiceLocked(Intent service, String resolvedType, String callingPackage) {
        ServiceLookupResult r = retrieveServiceLocked(service, null, resolvedType, callingPackage, Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.getCallingUserId(), false, false, false, false);
        if (r == null) {
            return null;
        }
        if (r.record == null) {
            throw new SecurityException("Permission Denial: Accessing service from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + r.permission);
        }
        IntentBindRecord ib = r.record.bindings.get(r.record.intent);
        if (ib == null) {
            return null;
        }
        IBinder ret = ib.binder;
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopServiceTokenLocked(ComponentName className, IBinder token, int startId) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopServiceToken: " + className + " " + token + " startId=" + startId);
        }
        ServiceRecord r = findServiceLocked(className, token, UserHandle.getCallingUserId());
        if (r == null) {
            return false;
        }
        if (startId >= 0) {
            ServiceRecord.StartItem si = r.findDeliveredStart(startId, false, false);
            if (si != null) {
                while (r.deliveredStarts.size() > 0) {
                    ServiceRecord.StartItem cur = r.deliveredStarts.remove(0);
                    cur.removeUriPermissionsLocked();
                    if (cur == si) {
                        break;
                    }
                }
            }
            if (r.getLastStartId() != startId) {
                return false;
            }
            if (r.deliveredStarts.size() > 0) {
                Slog.w(TAG, "stopServiceToken startId " + startId + " is last, but have " + r.deliveredStarts.size() + " remaining args");
            }
        }
        int uid = r.appInfo.uid;
        String packageName = r.name.getPackageName();
        String serviceName = r.name.getClassName();
        FrameworkStatsLog.write(99, uid, packageName, serviceName, 2);
        this.mAm.mBatteryStatsService.noteServiceStopRunning(uid, packageName, serviceName);
        r.startRequested = false;
        if (r.tracker != null) {
            synchronized (this.mAm.mProcessStats.mLock) {
                r.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
            }
        }
        r.callStart = false;
        long origId = Binder.clearCallingIdentity();
        bringDownServiceIfNeededLocked(r, false, false, false);
        Binder.restoreCallingIdentity(origId);
        return true;
    }

    public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, int flags, int foregroundServiceType) {
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        try {
            ServiceRecord r = findServiceLocked(className, token, userId);
            if (r != null) {
                setServiceForegroundInnerLocked(r, id, notification, flags, foregroundServiceType);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getForegroundServiceTypeLocked(ComponentName className, IBinder token) {
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        int ret = 0;
        try {
            ServiceRecord r = findServiceLocked(className, token, userId);
            if (r != null) {
                ret = r.foregroundServiceType;
            }
            return ret;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    boolean foregroundAppShownEnoughLocked(ActiveForegroundApp aa, long nowElapsed) {
        long j;
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            Slog.d(TAG, "Shown enough: pkg=" + aa.mPackageName + ", uid=" + aa.mUid);
        }
        aa.mHideTime = JobStatus.NO_LATEST_RUNTIME;
        if (aa.mShownWhileTop) {
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "YES - shown while on top");
                return true;
            }
            return true;
        } else if (this.mScreenOn || aa.mShownWhileScreenOn) {
            long minTime = aa.mStartVisibleTime;
            if (aa.mStartTime != aa.mStartVisibleTime) {
                j = this.mAm.mConstants.FGSERVICE_SCREEN_ON_AFTER_TIME;
            } else {
                j = this.mAm.mConstants.FGSERVICE_MIN_SHOWN_TIME;
            }
            long minTime2 = minTime + j;
            if (nowElapsed >= minTime2) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "YES - shown long enough with screen on");
                }
                return true;
            }
            long reportTime = this.mAm.mConstants.FGSERVICE_MIN_REPORT_TIME + nowElapsed;
            aa.mHideTime = reportTime > minTime2 ? reportTime : minTime2;
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "NO -- wait " + (aa.mHideTime - nowElapsed) + " with screen on");
                return false;
            }
            return false;
        } else {
            long minTime3 = aa.mEndTime + this.mAm.mConstants.FGSERVICE_SCREEN_ON_BEFORE_TIME;
            if (nowElapsed >= minTime3) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "YES - gone long enough with screen off");
                }
                return true;
            }
            aa.mHideTime = minTime3;
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "NO -- wait " + (aa.mHideTime - nowElapsed) + " with screen off");
                return false;
            }
            return false;
        }
    }

    void updateForegroundApps(ServiceMap smap) {
        ArrayList<ActiveForegroundApp> active = null;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long now = SystemClock.elapsedRealtime();
                long nextUpdateTime = JobStatus.NO_LATEST_RUNTIME;
                if (smap != null) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG, "Updating foreground apps for user " + smap.mUserId);
                    }
                    for (int i = smap.mActiveForegroundApps.size() - 1; i >= 0; i--) {
                        ActiveForegroundApp aa = smap.mActiveForegroundApps.valueAt(i);
                        if (aa.mEndTime != 0) {
                            boolean canRemove = foregroundAppShownEnoughLocked(aa, now);
                            if (canRemove) {
                                smap.mActiveForegroundApps.removeAt(i);
                                smap.mActiveForegroundAppsChanged = true;
                            } else if (aa.mHideTime < nextUpdateTime) {
                                nextUpdateTime = aa.mHideTime;
                            }
                        }
                        boolean canRemove2 = aa.mAppOnTop;
                        if (!canRemove2) {
                            if (isForegroundServiceAllowedInBackgroundRestricted(aa.mUid, aa.mPackageName)) {
                                if (active == null) {
                                    active = new ArrayList<>();
                                }
                                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                    Slog.d(TAG, "Adding active: pkg=" + aa.mPackageName + ", uid=" + aa.mUid);
                                }
                                active.add(aa);
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                    Slog.d(TAG, "bg-restricted app " + aa.mPackageName + SliceClientPermissions.SliceAuthority.DELIMITER + aa.mUid + " exiting top; demoting fg services ");
                                }
                                stopAllForegroundServicesLocked(aa.mUid, aa.mPackageName);
                            }
                        }
                    }
                    smap.removeMessages(2);
                    if (nextUpdateTime < JobStatus.NO_LATEST_RUNTIME) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.d(TAG, "Next update time in: " + (nextUpdateTime - now));
                        }
                        Message msg = smap.obtainMessage(2);
                        smap.sendMessageAtTime(msg, (SystemClock.uptimeMillis() + nextUpdateTime) - SystemClock.elapsedRealtime());
                    }
                }
                if (!smap.mActiveForegroundAppsChanged) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                smap.mActiveForegroundAppsChanged = false;
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void requestUpdateActiveForegroundAppsLocked(ServiceMap smap, long timeElapsed) {
        Message msg = smap.obtainMessage(2);
        if (timeElapsed != 0) {
            smap.sendMessageAtTime(msg, (SystemClock.uptimeMillis() + timeElapsed) - SystemClock.elapsedRealtime());
            return;
        }
        smap.mActiveForegroundAppsChanged = true;
        smap.sendMessage(msg);
    }

    private void decActiveForegroundAppLocked(ServiceMap smap, ServiceRecord r) {
        ActiveForegroundApp active = smap.mActiveForegroundApps.get(r.packageName);
        if (active != null) {
            active.mNumActive--;
            if (active.mNumActive <= 0) {
                active.mEndTime = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "Ended running of service");
                }
                if (foregroundAppShownEnoughLocked(active, active.mEndTime)) {
                    smap.mActiveForegroundApps.remove(r.packageName);
                    smap.mActiveForegroundAppsChanged = true;
                    requestUpdateActiveForegroundAppsLocked(smap, 0L);
                } else if (active.mHideTime < JobStatus.NO_LATEST_RUNTIME) {
                    requestUpdateActiveForegroundAppsLocked(smap, active.mHideTime);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateScreenStateLocked(boolean screenOn) {
        if (this.mScreenOn != screenOn) {
            this.mScreenOn = screenOn;
            if (screenOn) {
                long nowElapsed = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "Screen turned on");
                }
                for (int i = this.mServiceMap.size() - 1; i >= 0; i--) {
                    ServiceMap smap = this.mServiceMap.valueAt(i);
                    long nextUpdateTime = JobStatus.NO_LATEST_RUNTIME;
                    boolean changed = false;
                    for (int j = smap.mActiveForegroundApps.size() - 1; j >= 0; j--) {
                        ActiveForegroundApp active = smap.mActiveForegroundApps.valueAt(j);
                        if (active.mEndTime == 0) {
                            if (!active.mShownWhileScreenOn) {
                                active.mShownWhileScreenOn = true;
                                active.mStartVisibleTime = nowElapsed;
                            }
                        } else {
                            if (!active.mShownWhileScreenOn && active.mStartVisibleTime == active.mStartTime) {
                                active.mStartVisibleTime = nowElapsed;
                                active.mEndTime = nowElapsed;
                            }
                            if (foregroundAppShownEnoughLocked(active, nowElapsed)) {
                                smap.mActiveForegroundApps.remove(active.mPackageName);
                                smap.mActiveForegroundAppsChanged = true;
                                changed = true;
                            } else if (active.mHideTime < nextUpdateTime) {
                                nextUpdateTime = active.mHideTime;
                            }
                        }
                    }
                    if (changed) {
                        requestUpdateActiveForegroundAppsLocked(smap, 0L);
                    } else if (nextUpdateTime < JobStatus.NO_LATEST_RUNTIME) {
                        requestUpdateActiveForegroundAppsLocked(smap, nextUpdateTime);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void foregroundServiceProcStateChangedLocked(UidRecord uidRec) {
        ServiceMap smap = this.mServiceMap.get(UserHandle.getUserId(uidRec.getUid()));
        if (smap != null) {
            boolean changed = false;
            for (int j = smap.mActiveForegroundApps.size() - 1; j >= 0; j--) {
                ActiveForegroundApp active = smap.mActiveForegroundApps.valueAt(j);
                if (active.mUid == uidRec.getUid()) {
                    if (uidRec.getCurProcState() <= 2) {
                        if (!active.mAppOnTop) {
                            active.mAppOnTop = true;
                            changed = true;
                        }
                        active.mShownWhileTop = true;
                    } else if (active.mAppOnTop) {
                        active.mAppOnTop = false;
                        changed = true;
                    }
                }
            }
            if (changed) {
                requestUpdateActiveForegroundAppsLocked(smap, 0L);
            }
        }
    }

    private boolean isForegroundServiceAllowedInBackgroundRestricted(ProcessRecord app) {
        ProcessStateRecord state = app.mState;
        if (!state.isBackgroundRestricted() || state.getSetProcState() <= 3) {
            return true;
        }
        return state.getSetProcState() == 4 && state.isSetBoundByNonBgRestrictedApp();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isForegroundServiceAllowedInBackgroundRestricted(int uid, String packageName) {
        ProcessRecord app;
        UidRecord uidRec = this.mAm.mProcessList.getUidRecordLOSP(uid);
        return (uidRec == null || (app = uidRec.getProcessInPackage(packageName)) == null || !isForegroundServiceAllowedInBackgroundRestricted(app)) ? false : true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2115=8, 2121=4, 2129=4] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:148:0x03cf A[Catch: all -> 0x043b, TryCatch #10 {all -> 0x043b, blocks: (B:39:0x0116, B:165:0x0435, B:166:0x043a, B:108:0x02c3, B:110:0x02c9, B:112:0x02d1, B:114:0x02dd, B:116:0x02f5, B:118:0x02fe, B:122:0x0307, B:123:0x030b, B:124:0x0320, B:125:0x0326, B:127:0x033f, B:128:0x0345, B:138:0x0363, B:139:0x039f, B:141:0x03ac, B:142:0x03af, B:144:0x03b3, B:145:0x03b7, B:148:0x03cf, B:150:0x03d5, B:129:0x0346, B:131:0x034c, B:132:0x035c), top: B:243:0x0111 }] */
    /* JADX WARN: Removed duplicated region for block: B:152:0x03ef  */
    /* JADX WARN: Removed duplicated region for block: B:163:0x0413  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x0442  */
    /* JADX WARN: Removed duplicated region for block: B:183:0x0466  */
    /* JADX WARN: Removed duplicated region for block: B:232:0x02aa A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:246:0x016b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:98:0x02a5  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void setServiceForegroundInnerLocked(ServiceRecord r, int id, Notification notification, int flags, int foregroundServiceType) {
        boolean z;
        int foregroundServiceType2;
        boolean alreadyStartedOp;
        boolean stopProcStatsOp;
        boolean ignoreForeground;
        ProcessServiceRecord psr;
        int foregroundServiceType3;
        ProcessServiceRecord psr2;
        int foregroundServiceType4;
        UidRecord uidRec;
        if (id == 0) {
            if (r.isForeground) {
                ServiceMap smap = getServiceMapLocked(r.userId);
                if (smap != null) {
                    decActiveForegroundAppLocked(smap, r);
                }
                if ((flags & 1) != 0) {
                    cancelForegroundNotificationLocked(r);
                    r.foregroundId = 0;
                    r.foregroundNoti = null;
                    z = false;
                } else if (r.appInfo.targetSdkVersion >= 21) {
                    if (!r.mFgsNotificationShown && !"com.android.fanstrace".equals(r.packageName)) {
                        r.postNotification();
                    }
                    dropFgsNotificationStateLocked(r);
                    if ((flags & 2) != 0) {
                        z = false;
                        r.foregroundId = 0;
                        r.foregroundNoti = null;
                    } else {
                        z = false;
                    }
                } else {
                    z = false;
                }
                r.isForeground = z;
                r.mFgsExitTime = SystemClock.uptimeMillis();
                synchronized (this.mAm.mProcessStats.mLock) {
                    ServiceState stracker = r.getTracker();
                    if (stracker != null) {
                        stracker.setForeground(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                }
                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null);
                unregisterAppOpCallbackLocked(r);
                logFGSStateChangeLocked(r, 2, r.mFgsExitTime > r.mFgsEnterTime ? (int) (r.mFgsExitTime - r.mFgsEnterTime) : 0, 1);
                r.mFgsNotificationWasDeferred = false;
                signalForegroundServiceObserversLocked(r);
                resetFgsRestrictionLocked(r);
                this.mAm.updateForegroundServiceUsageStats(r.name, r.userId, false);
                if (r.app != null) {
                    this.mAm.updateLruProcessLocked(r.app, false, null);
                    updateServiceForegroundLocked(r.app.mServices, true);
                }
            }
        } else if (notification == null) {
            throw new IllegalArgumentException("null notification");
        } else {
            if (r.appInfo.isInstantApp()) {
                int mode = this.mAm.getAppOpsManager().checkOpNoThrow(68, r.appInfo.uid, r.appInfo.packageName);
                switch (mode) {
                    case 0:
                        break;
                    case 1:
                        Slog.w(TAG, "Instant app " + r.appInfo.packageName + " does not have permission to create foreground services, ignoring.");
                        return;
                    case 2:
                        throw new SecurityException("Instant app " + r.appInfo.packageName + " does not have permission to create foreground services");
                    default:
                        this.mAm.enforcePermission("android.permission.INSTANT_APP_FOREGROUND_SERVICE", r.app.getPid(), r.appInfo.uid, "startForeground");
                        break;
                }
                foregroundServiceType2 = foregroundServiceType;
            } else {
                if (r.appInfo.targetSdkVersion >= 28) {
                    this.mAm.enforcePermission("android.permission.FOREGROUND_SERVICE", r.app.getPid(), r.appInfo.uid, "startForeground");
                }
                int manifestType = r.serviceInfo.getForegroundServiceType();
                int foregroundServiceType5 = foregroundServiceType;
                if (foregroundServiceType5 == -1) {
                    foregroundServiceType5 = manifestType;
                }
                if ((foregroundServiceType5 & manifestType) != foregroundServiceType5) {
                    throw new IllegalArgumentException("foregroundServiceType " + String.format("0x%08X", Integer.valueOf(foregroundServiceType5)) + " is not a subset of foregroundServiceType attribute " + String.format("0x%08X", Integer.valueOf(manifestType)) + " in service element of manifest file");
                }
                foregroundServiceType2 = foregroundServiceType5;
            }
            if (r.fgRequired) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                    Slog.i(TAG, "Service called startForeground() as required: " + r);
                }
                r.fgRequired = false;
                r.fgWaiting = false;
                this.mAm.mHandler.removeMessages(66, r);
                alreadyStartedOp = true;
                stopProcStatsOp = true;
            } else {
                alreadyStartedOp = false;
                stopProcStatsOp = false;
            }
            ProcessServiceRecord psr3 = r.app.mServices;
            boolean ignoreForeground2 = false;
            try {
                int mode2 = this.mAm.getAppOpsManager().checkOpNoThrow(76, r.appInfo.uid, r.packageName);
                try {
                    switch (mode2) {
                        case 0:
                        case 3:
                            if (!ignoreForeground2 || isForegroundServiceAllowedInBackgroundRestricted(r.app)) {
                                ignoreForeground = ignoreForeground2;
                            } else {
                                Slog.w(TAG, "Service.startForeground() not allowed due to bg restriction: service " + r.shortInstanceName);
                                updateServiceForegroundLocked(psr3, false);
                                ignoreForeground = true;
                            }
                            if (ignoreForeground) {
                                try {
                                    try {
                                        if (r.mStartForegroundCount == 0) {
                                            try {
                                                if (r.fgRequired) {
                                                    psr = psr3;
                                                    foregroundServiceType3 = foregroundServiceType2;
                                                } else {
                                                    long delayMs = SystemClock.elapsedRealtime() - r.createRealTime;
                                                    if (delayMs > this.mAm.mConstants.mFgsStartForegroundTimeoutMs) {
                                                        resetFgsRestrictionLocked(r);
                                                        psr = psr3;
                                                        foregroundServiceType3 = foregroundServiceType2;
                                                        setFgsRestrictionLocked(r.serviceInfo.packageName, r.app.getPid(), r.appInfo.uid, r.intent.getIntent(), r, r.userId, false);
                                                        String temp = "startForegroundDelayMs:" + delayMs;
                                                        if (r.mInfoAllowStartForeground != null) {
                                                            r.mInfoAllowStartForeground += "; " + temp;
                                                        } else {
                                                            r.mInfoAllowStartForeground = temp;
                                                        }
                                                        r.mLoggedInfoAllowStartForeground = false;
                                                    } else {
                                                        psr = psr3;
                                                        foregroundServiceType3 = foregroundServiceType2;
                                                    }
                                                }
                                            } catch (Throwable th) {
                                                th = th;
                                                if (stopProcStatsOp) {
                                                }
                                                if (alreadyStartedOp) {
                                                }
                                                throw th;
                                            }
                                        } else {
                                            psr = psr3;
                                            foregroundServiceType3 = foregroundServiceType2;
                                            try {
                                                if (r.mStartForegroundCount >= 1) {
                                                    setFgsRestrictionLocked(r.serviceInfo.packageName, r.app.getPid(), r.appInfo.uid, r.intent.getIntent(), r, r.userId, false);
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                                if (stopProcStatsOp) {
                                                }
                                                if (alreadyStartedOp) {
                                                }
                                                throw th;
                                            }
                                        }
                                        if (!r.mAllowWhileInUsePermissionInFgs) {
                                            Slog.w(TAG, "Foreground service started from background can not have location/camera/microphone access: service " + r.shortInstanceName);
                                        }
                                        logFgsBackgroundStart(r);
                                        if (r.mAllowStartForeground == -1 && isBgFgsRestrictionEnabled(r)) {
                                            String msg = "Service.startForeground() not allowed due to mAllowStartForeground false: service " + r.shortInstanceName;
                                            Slog.w(TAG, msg);
                                            showFgsBgRestrictedNotificationLocked(r);
                                            psr2 = psr;
                                            try {
                                                updateServiceForegroundLocked(psr2, true);
                                                ignoreForeground = true;
                                                logFGSStateChangeLocked(r, 3, 0, 0);
                                                if (CompatChanges.isChangeEnabled((long) FGS_START_EXCEPTION_CHANGE_ID, r.appInfo.uid)) {
                                                    throw new ForegroundServiceStartNotAllowedException(msg);
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                                if (stopProcStatsOp) {
                                                }
                                                if (alreadyStartedOp) {
                                                }
                                                throw th;
                                            }
                                        } else {
                                            psr2 = psr;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } else {
                                psr2 = psr3;
                                foregroundServiceType3 = foregroundServiceType2;
                            }
                            if (ignoreForeground) {
                                try {
                                    if (r.foregroundId != id) {
                                        cancelForegroundNotificationLocked(r);
                                        r.foregroundId = id;
                                    }
                                    notification.flags |= 64;
                                    r.foregroundNoti = notification;
                                    foregroundServiceType4 = foregroundServiceType3;
                                    r.foregroundServiceType = foregroundServiceType4;
                                    if (!r.isForeground) {
                                        ServiceMap smap2 = getServiceMapLocked(r.userId);
                                        if (smap2 != null) {
                                            ActiveForegroundApp active = smap2.mActiveForegroundApps.get(r.packageName);
                                            if (active == null) {
                                                active = new ActiveForegroundApp();
                                                active.mPackageName = r.packageName;
                                                active.mUid = r.appInfo.uid;
                                                active.mShownWhileScreenOn = this.mScreenOn;
                                                if (r.app != null && (uidRec = r.app.getUidRecord()) != null) {
                                                    boolean z2 = uidRec.getCurProcState() <= 2;
                                                    active.mShownWhileTop = z2;
                                                    active.mAppOnTop = z2;
                                                }
                                                long elapsedRealtime = SystemClock.elapsedRealtime();
                                                active.mStartVisibleTime = elapsedRealtime;
                                                active.mStartTime = elapsedRealtime;
                                                smap2.mActiveForegroundApps.put(r.packageName, active);
                                                requestUpdateActiveForegroundAppsLocked(smap2, 0L);
                                            }
                                            active.mNumActive++;
                                        }
                                        r.isForeground = true;
                                        r.mAllowStartForegroundAtEntering = r.mAllowStartForeground;
                                        r.mAllowWhileInUsePermissionInFgsAtEntering = r.mAllowWhileInUsePermissionInFgs;
                                        r.mStartForegroundCount++;
                                        r.mFgsEnterTime = SystemClock.uptimeMillis();
                                        if (stopProcStatsOp) {
                                            stopProcStatsOp = false;
                                        } else {
                                            synchronized (this.mAm.mProcessStats.mLock) {
                                                ServiceState stracker2 = r.getTracker();
                                                if (stracker2 != null) {
                                                    stracker2.setForeground(true, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                                                }
                                            }
                                        }
                                        this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null, true, false, "", false, 0, -1);
                                        registerAppOpCallbackLocked(r);
                                        this.mAm.updateForegroundServiceUsageStats(r.name, r.userId, true);
                                        logFGSStateChangeLocked(r, 1, 0, 0);
                                    }
                                    signalForegroundServiceObserversLocked(r);
                                    if (!"com.android.fanstrace".equals(r.packageName)) {
                                        r.postNotification();
                                    }
                                    if (r.app != null) {
                                        updateServiceForegroundLocked(psr2, true);
                                    }
                                    getServiceMapLocked(r.userId).ensureNotStartingBackgroundLocked(r);
                                    this.mAm.notifyPackageUse(r.serviceInfo.packageName, 2);
                                } catch (Throwable th6) {
                                    th = th6;
                                    if (stopProcStatsOp) {
                                        synchronized (this.mAm.mProcessStats.mLock) {
                                            ServiceState stracker3 = r.getTracker();
                                            if (stracker3 != null) {
                                                stracker3.setForeground(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                                            }
                                        }
                                    }
                                    if (alreadyStartedOp) {
                                        this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null);
                                    }
                                    throw th;
                                }
                            } else {
                                foregroundServiceType4 = foregroundServiceType3;
                                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                    Slog.d(TAG, "Suppressing startForeground() for FAS " + r);
                                }
                            }
                            if (stopProcStatsOp) {
                                synchronized (this.mAm.mProcessStats.mLock) {
                                    ServiceState stracker4 = r.getTracker();
                                    if (stracker4 != null) {
                                        stracker4.setForeground(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                                    }
                                }
                            }
                            if (alreadyStartedOp) {
                                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null);
                            }
                            return;
                        case 1:
                            try {
                                Slog.w(TAG, "Service.startForeground() not allowed due to app op: service " + r.shortInstanceName);
                                ignoreForeground2 = true;
                                if (ignoreForeground2) {
                                    break;
                                }
                                ignoreForeground = ignoreForeground2;
                                if (ignoreForeground) {
                                }
                                if (ignoreForeground) {
                                }
                                if (stopProcStatsOp) {
                                }
                                if (alreadyStartedOp) {
                                }
                                return;
                            } catch (Throwable th7) {
                                th = th7;
                                if (stopProcStatsOp) {
                                }
                                if (alreadyStartedOp) {
                                }
                                throw th;
                            }
                        case 2:
                        default:
                            throw new SecurityException("Foreground not allowed as per app op");
                    }
                } catch (Throwable th8) {
                    th = th8;
                }
            } catch (Throwable th9) {
                th = th9;
            }
        }
    }

    private boolean withinFgsDeferRateLimit(ServiceRecord sr, long now) {
        if (now < sr.fgDisplayTime) {
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG_SERVICE, "FGS transition for " + sr + " within deferral period, no rate limit applied");
            }
            return false;
        }
        int uid = sr.appInfo.uid;
        long eligible = this.mFgsDeferralEligible.get(uid, 0L);
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE && now < eligible) {
            Slog.d(TAG_SERVICE, "FGS transition for uid " + uid + " within rate limit, showing immediately");
        }
        return now < eligible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManagerInternal.ServiceNotificationPolicy applyForegroundServiceNotificationLocked(Notification notification, String tag, int id, String pkg, int userId) {
        if (tag != null) {
            return ActivityManagerInternal.ServiceNotificationPolicy.NOT_FOREGROUND_SERVICE;
        }
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            Slog.d(TAG_SERVICE, "Evaluating FGS policy for id=" + id + " pkg=" + pkg + " not=" + notification);
        }
        ServiceMap smap = this.mServiceMap.get(userId);
        if (smap == null) {
            return ActivityManagerInternal.ServiceNotificationPolicy.NOT_FOREGROUND_SERVICE;
        }
        for (int i = 0; i < smap.mServicesByInstanceName.size(); i++) {
            ServiceRecord sr = smap.mServicesByInstanceName.valueAt(i);
            if (sr.isForeground && id == sr.foregroundId && pkg.equals(sr.appInfo.packageName)) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "   FOUND: notification is for " + sr);
                }
                notification.flags |= 64;
                sr.foregroundNoti = notification;
                boolean showNow = shouldShowFgsNotificationLocked(sr);
                if (showNow) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "   Showing immediately due to policy");
                    }
                    sr.mFgsNotificationDeferred = false;
                    return ActivityManagerInternal.ServiceNotificationPolicy.SHOW_IMMEDIATELY;
                }
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "   Deferring / update-only");
                }
                startFgsDeferralTimerLocked(sr);
                return ActivityManagerInternal.ServiceNotificationPolicy.UPDATE_ONLY;
            }
        }
        return ActivityManagerInternal.ServiceNotificationPolicy.NOT_FOREGROUND_SERVICE;
    }

    private boolean shouldShowFgsNotificationLocked(ServiceRecord r) {
        long now = SystemClock.uptimeMillis();
        if (this.mAm.mConstants.mFlagFgsNotificationDeferralEnabled) {
            if (r.mFgsNotificationDeferred && now >= r.fgDisplayTime) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "FGS reached end of deferral period: " + r);
                }
                return true;
            } else if (withinFgsDeferRateLimit(r, now)) {
                return true;
            } else {
                if (this.mAm.mConstants.mFlagFgsNotificationDeferralApiGated) {
                    boolean isLegacyApp = r.appInfo.targetSdkVersion < 31;
                    if (isLegacyApp) {
                        return true;
                    }
                }
                boolean isLegacyApp2 = r.mFgsNotificationShown;
                if (isLegacyApp2) {
                    return true;
                }
                if (!r.foregroundNoti.isForegroundDisplayForceDeferred()) {
                    if (r.foregroundNoti.shouldShowForegroundImmediately()) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.d(TAG_SERVICE, "FGS " + r + " notification policy says show immediately");
                        }
                        return true;
                    } else if ((r.foregroundServiceType & 54) != 0) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.d(TAG_SERVICE, "FGS " + r + " type gets immediate display");
                        }
                        return true;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "FGS " + r + " notification is app deferred");
                }
                return false;
            }
        }
        return true;
    }

    private void startFgsDeferralTimerLocked(ServiceRecord r) {
        long now = SystemClock.uptimeMillis();
        int uid = r.appInfo.uid;
        long when = this.mAm.mConstants.mFgsNotificationDeferralInterval + now;
        for (int i = 0; i < this.mPendingFgsNotifications.size(); i++) {
            ServiceRecord pending = this.mPendingFgsNotifications.get(i);
            if (pending == r) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "FGS " + r + " already pending notification display");
                    return;
                }
                return;
            }
            if (uid == pending.appInfo.uid) {
                when = Math.min(when, pending.fgDisplayTime);
            }
        }
        if (this.mFgsDeferralRateLimited) {
            long nextEligible = this.mAm.mConstants.mFgsNotificationDeferralExclusionTime + when;
            this.mFgsDeferralEligible.put(uid, nextEligible);
        }
        r.fgDisplayTime = when;
        boolean isLegacyApp = true;
        r.mFgsNotificationDeferred = true;
        r.mFgsNotificationWasDeferred = true;
        r.mFgsNotificationShown = false;
        this.mPendingFgsNotifications.add(r);
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            Slog.d(TAG_SERVICE, "FGS " + r + " notification in " + (when - now) + " ms");
        }
        if (r.appInfo.targetSdkVersion >= 31) {
            isLegacyApp = false;
        }
        if (isLegacyApp) {
            Slog.i(TAG_SERVICE, "Deferring FGS notification in legacy app " + r.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + UserHandle.formatUid(r.appInfo.uid) + " : " + r.foregroundNoti);
        }
        this.mAm.mHandler.postAtTime(this.mPostDeferredFGSNotifications, when);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableFgsNotificationRateLimitLocked(boolean enable) {
        if (enable != this.mFgsDeferralRateLimited) {
            this.mFgsDeferralRateLimited = enable;
            if (!enable) {
                this.mFgsDeferralEligible.clear();
            }
        }
        return enable;
    }

    private void removeServiceNotificationDeferralsLocked(String packageName, int userId) {
        for (int i = this.mPendingFgsNotifications.size() - 1; i >= 0; i--) {
            ServiceRecord r = this.mPendingFgsNotifications.get(i);
            if (userId == r.userId && r.appInfo.packageName.equals(packageName)) {
                this.mPendingFgsNotifications.remove(i);
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "Removing notification deferral for " + r);
                }
            }
        }
    }

    public void onForegroundServiceNotificationUpdateLocked(boolean shown, Notification notification, int id, String pkg, int userId) {
        for (int i = this.mPendingFgsNotifications.size() - 1; i >= 0; i--) {
            ServiceRecord sr = this.mPendingFgsNotifications.get(i);
            if (userId == sr.userId && id == sr.foregroundId && sr.appInfo.packageName.equals(pkg)) {
                if (shown) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Notification shown; canceling deferral of " + sr);
                    }
                    sr.mFgsNotificationShown = true;
                    sr.mFgsNotificationDeferred = false;
                    this.mPendingFgsNotifications.remove(i);
                } else if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG_SERVICE, "FGS notification deferred for " + sr);
                }
            }
        }
        ServiceMap smap = this.mServiceMap.get(userId);
        if (smap != null) {
            for (int i2 = 0; i2 < smap.mServicesByInstanceName.size(); i2++) {
                ServiceRecord sr2 = smap.mServicesByInstanceName.valueAt(i2);
                if (sr2.isForeground && id == sr2.foregroundId && sr2.appInfo.packageName.equals(pkg)) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Recording shown notification for " + sr2);
                    }
                    sr2.foregroundNoti = notification;
                }
            }
        }
    }

    private void registerAppOpCallbackLocked(ServiceRecord r) {
        if (r.app == null) {
            return;
        }
        int uid = r.appInfo.uid;
        AppOpCallback callback = this.mFgsAppOpCallbacks.get(uid);
        if (callback == null) {
            callback = new AppOpCallback(r.app, this.mAm.getAppOpsManager());
            this.mFgsAppOpCallbacks.put(uid, callback);
        }
        callback.registerLocked();
    }

    private void unregisterAppOpCallbackLocked(ServiceRecord r) {
        int uid = r.appInfo.uid;
        AppOpCallback callback = this.mFgsAppOpCallbacks.get(uid);
        if (callback != null) {
            callback.unregisterLocked();
            if (callback.isObsoleteLocked()) {
                this.mFgsAppOpCallbacks.remove(uid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AppOpCallback {
        private static final int[] LOGGED_AP_OPS = {0, 1, 27, 26};
        private final AppOpsManager mAppOpsManager;
        private final ProcessRecord mProcessRecord;
        private final SparseIntArray mAcceptedOps = new SparseIntArray();
        private final SparseIntArray mRejectedOps = new SparseIntArray();
        private final Object mCounterLock = new Object();
        private final SparseIntArray mAppOpModes = new SparseIntArray();
        private int mNumFgs = 0;
        private boolean mDestroyed = false;
        private final AppOpsManager.OnOpNotedListener mOpNotedCallback = new AppOpsManager.OnOpNotedListener() { // from class: com.android.server.am.ActiveServices.AppOpCallback.1
            public void onOpNoted(int op, int uid, String pkgName, String attributionTag, int flags, int result) {
                AppOpCallback.this.incrementOpCountIfNeeded(op, uid, result);
            }
        };
        private final AppOpsManager.OnOpStartedListener mOpStartedCallback = new AppOpsManager.OnOpStartedListener() { // from class: com.android.server.am.ActiveServices.AppOpCallback.2
            public void onOpStarted(int op, int uid, String pkgName, String attributionTag, int flags, int result) {
                AppOpCallback.this.incrementOpCountIfNeeded(op, uid, result);
            }
        };

        AppOpCallback(ProcessRecord r, AppOpsManager appOpsManager) {
            int[] iArr;
            this.mProcessRecord = r;
            this.mAppOpsManager = appOpsManager;
            for (int op : LOGGED_AP_OPS) {
                int mode = appOpsManager.unsafeCheckOpRawNoThrow(op, r.uid, r.info.packageName);
                this.mAppOpModes.put(op, mode);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void incrementOpCountIfNeeded(int op, int uid, int result) {
            if (uid == this.mProcessRecord.uid && isNotTop()) {
                incrementOpCount(op, result == 0);
            }
        }

        private boolean isNotTop() {
            return this.mProcessRecord.mState.getCurProcState() != 2;
        }

        private void incrementOpCount(int op, boolean allowed) {
            synchronized (this.mCounterLock) {
                SparseIntArray counter = allowed ? this.mAcceptedOps : this.mRejectedOps;
                int index = counter.indexOfKey(op);
                if (index >= 0) {
                    counter.setValueAt(index, counter.valueAt(index) + 1);
                } else {
                    counter.put(op, 1);
                }
            }
        }

        void registerLocked() {
            if (isObsoleteLocked()) {
                Slog.wtf(ActiveServices.TAG, "Trying to register on a stale AppOpCallback.");
                return;
            }
            int i = this.mNumFgs + 1;
            this.mNumFgs = i;
            if (i == 1) {
                AppOpsManager appOpsManager = this.mAppOpsManager;
                int[] iArr = LOGGED_AP_OPS;
                appOpsManager.startWatchingNoted(iArr, this.mOpNotedCallback);
                this.mAppOpsManager.startWatchingStarted(iArr, this.mOpStartedCallback);
            }
        }

        void unregisterLocked() {
            int i = this.mNumFgs - 1;
            this.mNumFgs = i;
            if (i <= 0) {
                this.mDestroyed = true;
                logFinalValues();
                this.mAppOpsManager.stopWatchingNoted(this.mOpNotedCallback);
                this.mAppOpsManager.stopWatchingStarted(this.mOpStartedCallback);
            }
        }

        boolean isObsoleteLocked() {
            return this.mDestroyed;
        }

        private void logFinalValues() {
            int[] iArr;
            synchronized (this.mCounterLock) {
                for (int op : LOGGED_AP_OPS) {
                    int acceptances = this.mAcceptedOps.get(op);
                    int rejections = this.mRejectedOps.get(op);
                    if (acceptances > 0 || rejections > 0) {
                        FrameworkStatsLog.write(256, this.mProcessRecord.uid, op, modeToEnum(this.mAppOpModes.get(op)), acceptances, rejections);
                    }
                }
            }
        }

        private static int modeToEnum(int mode) {
            switch (mode) {
                case 0:
                    return 1;
                case 1:
                    return 2;
                case 2:
                case 3:
                default:
                    return 0;
                case 4:
                    return 3;
            }
        }
    }

    private void cancelForegroundNotificationLocked(ServiceRecord r) {
        if (r.foregroundNoti != null) {
            ServiceMap sm = getServiceMapLocked(r.userId);
            if (sm != null) {
                for (int i = sm.mServicesByInstanceName.size() - 1; i >= 0; i--) {
                    ServiceRecord other = sm.mServicesByInstanceName.valueAt(i);
                    if (other != r && other.isForeground && other.foregroundId == r.foregroundId && other.packageName.equals(r.packageName)) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.i(TAG_SERVICE, "FGS notification for " + r + " shared by " + other + " (isForeground=" + other.isForeground + ") - NOT cancelling");
                            return;
                        } else {
                            return;
                        }
                    }
                }
            }
            r.cancelNotification();
        }
    }

    private void updateServiceForegroundLocked(ProcessServiceRecord psr, boolean oomAdj) {
        boolean anyForeground = false;
        int fgServiceTypes = 0;
        for (int i = psr.numberOfRunningServices() - 1; i >= 0; i--) {
            ServiceRecord sr = psr.getRunningServiceAt(i);
            if (sr.isForeground || sr.fgRequired) {
                anyForeground = true;
                fgServiceTypes |= sr.foregroundServiceType;
            }
        }
        this.mAm.updateProcessForegroundLocked(psr.mApp, anyForeground, fgServiceTypes, oomAdj);
        psr.setHasReportedForegroundServices(anyForeground);
    }

    private void updateAllowlistManagerLocked(ProcessServiceRecord psr) {
        psr.mAllowlistManager = false;
        for (int i = psr.numberOfRunningServices() - 1; i >= 0; i--) {
            ServiceRecord sr = psr.getRunningServiceAt(i);
            if (sr.allowlistManager) {
                psr.mAllowlistManager = true;
                return;
            }
        }
    }

    private void stopServiceAndUpdateAllowlistManagerLocked(ServiceRecord service) {
        ProcessServiceRecord psr = service.app.mServices;
        psr.stopService(service);
        psr.updateBoundClientUids();
        if (service.allowlistManager) {
            updateAllowlistManagerLocked(psr);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateServiceConnectionActivitiesLocked(ProcessServiceRecord clientPsr) {
        ArraySet<ProcessRecord> updatedProcesses = null;
        for (int i = 0; i < clientPsr.numberOfConnections(); i++) {
            ConnectionRecord conn = clientPsr.getConnectionAt(i);
            ProcessRecord proc = conn.binding.service.app;
            if (proc != null && proc != clientPsr.mApp) {
                if (updatedProcesses == null) {
                    updatedProcesses = new ArraySet<>();
                } else if (updatedProcesses.contains(proc)) {
                }
                updatedProcesses.add(proc);
                updateServiceClientActivitiesLocked(proc.mServices, null, false);
            }
        }
    }

    private boolean updateServiceClientActivitiesLocked(ProcessServiceRecord psr, ConnectionRecord modCr, boolean updateLru) {
        if (modCr != null && modCr.binding.client != null && !modCr.binding.client.hasActivities()) {
            return false;
        }
        boolean anyClientActivities = false;
        for (int i = psr.numberOfRunningServices() - 1; i >= 0 && !anyClientActivities; i--) {
            ServiceRecord sr = psr.getRunningServiceAt(i);
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = sr.getConnections();
            for (int conni = connections.size() - 1; conni >= 0 && !anyClientActivities; conni--) {
                ArrayList<ConnectionRecord> clist = connections.valueAt(conni);
                int cri = clist.size() - 1;
                while (true) {
                    if (cri >= 0) {
                        ConnectionRecord cr = clist.get(cri);
                        if (cr.binding.client != null && cr.binding.client != psr.mApp && cr.binding.client.hasActivities()) {
                            anyClientActivities = true;
                            break;
                        }
                        cri--;
                    }
                }
            }
        }
        if (anyClientActivities == psr.hasClientActivities()) {
            return false;
        }
        psr.setHasClientActivities(anyClientActivities);
        if (updateLru) {
            this.mAm.updateLruProcessLocked(psr.mApp, anyClientActivities, null);
        }
        return true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3141=11] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:203:0x0536 */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't wrap try/catch for region: R(55:68|(2:70|(2:72|(2:74|75)(1:76))(1:317))(1:318)|77|(19:(2:79|(65:81|82|83|84|85|86|87|(3:306|307|(1:309))|89|(2:91|(3:93|334|99)(1:304))(1:305)|100|(1:102)|108|(1:110)(1:302)|111|112|113|114|115|(2:296|297)|117|118|119|(1:121)|122|123|(1:125)|126|(1:128)|129|(1:131)|132|133|(1:293)(2:137|(1:292)(1:141))|142|(1:144)|145|(2:147|148)(1:291)|149|150|(7:152|153|154|155|(5:157|158|159|160|(5:162|163|164|165|166)(1:168))(1:282)|169|(3:171|172|173))(1:286)|174|175|176|177|178|179|(14:241|242|243|(3:245|246|247)(1:269)|248|(1:250)|251|(2:253|(3:255|256|257))|259|260|261|(2:265|257)|256|257)(1:181)|(1:183)|184|185|186|(1:238)(1:(1:237)(1:192))|193|194|(1:196)|197|198|(2:232|(1:234))(7:202|(1:204)(1:231)|205|206|207|208|(1:225)(3:212|213|214))|215|216|217|218|219|220))(1:316)|185|186|(1:188)|238|193|194|(0)|197|198|(1:200)|232|(0)|215|216|217|218|219|220)|315|82|83|84|85|86|87|(0)|89|(0)(0)|100|(0)|108|(0)(0)|111|112|113|114|115|(0)|117|118|119|(0)|122|123|(0)|126|(0)|129|(0)|132|133|(1:135)|293|142|(0)|145|(0)(0)|149|150|(0)(0)|174|175|176|177|178|179|(0)(0)|(0)|184) */
    /* JADX WARN: Code restructure failed: missing block: B:300:0x074c, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:302:0x0757, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:304:0x0764, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:306:0x0775, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:308:0x0786, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:310:0x0799, code lost:
        r0 = th;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:113:0x0322 A[Catch: all -> 0x030a, TryCatch #5 {all -> 0x030a, blocks: (B:106:0x02ed, B:108:0x02f1, B:111:0x031e, B:113:0x0322, B:115:0x032e, B:116:0x0334, B:128:0x0355, B:130:0x035b, B:117:0x0335, B:119:0x033b, B:121:0x034d), top: B:324:0x02ed }] */
    /* JADX WARN: Removed duplicated region for block: B:127:0x0354  */
    /* JADX WARN: Removed duplicated region for block: B:130:0x035b A[Catch: all -> 0x030a, TRY_LEAVE, TryCatch #5 {all -> 0x030a, blocks: (B:106:0x02ed, B:108:0x02f1, B:111:0x031e, B:113:0x0322, B:115:0x032e, B:116:0x0334, B:128:0x0355, B:130:0x035b, B:117:0x0335, B:119:0x033b, B:121:0x034d), top: B:324:0x02ed }] */
    /* JADX WARN: Removed duplicated region for block: B:133:0x0372  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x0374  */
    /* JADX WARN: Removed duplicated region for block: B:147:0x0412 A[Catch: all -> 0x03ef, TRY_ENTER, TRY_LEAVE, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:151:0x041b A[Catch: all -> 0x03ef, TRY_ENTER, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:154:0x0421 A[Catch: all -> 0x03ef, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:157:0x042b A[Catch: all -> 0x03ef, TRY_LEAVE, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:173:0x0458 A[Catch: all -> 0x03ef, TRY_ENTER, TRY_LEAVE, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:176:0x0469 A[Catch: all -> 0x03ef, TRY_ENTER, TRY_LEAVE, TryCatch #7 {all -> 0x03ef, blocks: (B:140:0x03eb, B:147:0x0412, B:151:0x041b, B:152:0x041d, B:154:0x0421, B:155:0x0424, B:157:0x042b, B:161:0x0432, B:163:0x0438, B:165:0x0442, B:167:0x0446, B:173:0x0458, B:176:0x0469), top: B:328:0x03eb }] */
    /* JADX WARN: Removed duplicated region for block: B:178:0x0476  */
    /* JADX WARN: Removed duplicated region for block: B:182:0x047f  */
    /* JADX WARN: Removed duplicated region for block: B:207:0x0555  */
    /* JADX WARN: Removed duplicated region for block: B:241:0x05e9  */
    /* JADX WARN: Removed duplicated region for block: B:243:0x05f1 A[Catch: all -> 0x05fa, TRY_LEAVE, TryCatch #16 {all -> 0x05fa, blocks: (B:238:0x05cd, B:243:0x05f1, B:250:0x0619, B:262:0x063b, B:269:0x0699, B:273:0x06a1, B:232:0x05bd, B:234:0x05c6), top: B:343:0x05bd }] */
    /* JADX WARN: Removed duplicated region for block: B:262:0x063b A[Catch: all -> 0x05fa, TRY_ENTER, TRY_LEAVE, TryCatch #16 {all -> 0x05fa, blocks: (B:238:0x05cd, B:243:0x05f1, B:250:0x0619, B:262:0x063b, B:269:0x0699, B:273:0x06a1, B:232:0x05bd, B:234:0x05c6), top: B:343:0x05bd }] */
    /* JADX WARN: Removed duplicated region for block: B:287:0x0714 A[Catch: all -> 0x073c, TryCatch #17 {all -> 0x073c, blocks: (B:283:0x0703, B:289:0x071c, B:285:0x070a, B:287:0x0714), top: B:344:0x068d }] */
    /* JADX WARN: Removed duplicated region for block: B:324:0x02ed A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:328:0x03eb A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:346:0x0587 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r22v3 */
    /* JADX WARN: Type inference failed for: r22v4, types: [int] */
    /* JADX WARN: Type inference failed for: r22v5 */
    /* JADX WARN: Type inference failed for: r22v6 */
    /* JADX WARN: Type inference failed for: r36v0 */
    /* JADX WARN: Type inference failed for: r36v3 */
    /* JADX WARN: Type inference failed for: r36v6 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid, String sdkSandboxClientAppPackage, String callingPackage, int userId) throws TransactionTooLargeException {
        ActivityServiceConnectionsHolder<ConnectionRecord> activity;
        boolean z;
        Intent service2;
        Intent originalService;
        int clientLabel;
        PendingIntent clientIntent;
        ActiveServices activeServices;
        Intent service3;
        int callingUid;
        ServiceRecord s;
        Intent service4;
        int callingUid2;
        boolean permissionsReviewRequired;
        long origId;
        ServiceRecord s2;
        boolean z2;
        ConnectionRecord c;
        ProcessServiceRecord clientPsr;
        ProcessServiceRecord clientPsr2;
        ArrayList<ConnectionRecord> clist;
        ArrayList<ConnectionRecord> clist2;
        ConnectionRecord c2;
        AppBindRecord b;
        ProcessRecord callerApp;
        ActiveServices activeServices2;
        boolean needOomAdj;
        ServiceRecord s3;
        boolean z3;
        ProcessRecord callerApp2;
        boolean z4;
        AppBindRecord b2;
        boolean needOomAdj2;
        ConnectionRecord c3;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "bindService: " + service + " type=" + resolvedType + " conn=" + connection.asBinder() + " flags=0x" + Integer.toHexString(flags));
        }
        int callingPid = Binder.getCallingPid();
        int callingUid3 = Binder.getCallingUid();
        ProcessRecord callerApp3 = this.mAm.getRecordForAppLOSP(caller);
        if (callerApp3 == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + callingPid + ") when binding service " + service);
        }
        TranProcessWrapper callerProc = callerApp3.processWrapper;
        if (token != null) {
            ActivityServiceConnectionsHolder<ConnectionRecord> activity2 = this.mAm.mAtmInternal.getServiceConnectionsHolder(token);
            if (activity2 == null) {
                Slog.w(TAG, "Binding with unknown activity: " + token);
                return 0;
            }
            activity = activity2;
        } else {
            activity = null;
        }
        boolean isCallerSystem = callerApp3.info.uid == 1000;
        if (isCallerSystem) {
            service.setDefusable(true);
            PendingIntent clientIntent2 = (PendingIntent) service.getParcelableExtra("android.intent.extra.client_intent");
            if (clientIntent2 != null) {
                z = false;
                int clientLabel2 = service.getIntExtra("android.intent.extra.client_label", 0);
                if (clientLabel2 != 0) {
                    Intent originalService2 = new Intent(service);
                    service2 = service.cloneFilter();
                    originalService = originalService2;
                    clientLabel = clientLabel2;
                    clientIntent = clientIntent2;
                } else {
                    service2 = service;
                    originalService = null;
                    clientLabel = clientLabel2;
                    clientIntent = clientIntent2;
                }
            } else {
                z = false;
                service2 = service;
                originalService = null;
                clientLabel = 0;
                clientIntent = clientIntent2;
            }
        } else {
            z = false;
            service2 = service;
            originalService = null;
            clientLabel = 0;
            clientIntent = null;
        }
        if ((flags & 134217728) != 0) {
            this.mAm.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS", "BIND_TREAT_LIKE_ACTIVITY");
        }
        if ((flags & 524288) != 0 && !isCallerSystem) {
            throw new SecurityException("Non-system caller (pid=" + callingPid + ") set BIND_SCHEDULE_LIKE_TOP_APP when binding service " + service2);
        }
        if ((flags & 16777216) != 0 && !isCallerSystem) {
            throw new SecurityException("Non-system caller " + caller + " (pid=" + callingPid + ") set BIND_ALLOW_WHITELIST_MANAGEMENT when binding service " + service2);
        }
        if ((flags & 4194304) != 0 && !isCallerSystem) {
            throw new SecurityException("Non-system caller " + caller + " (pid=" + callingPid + ") set BIND_ALLOW_INSTANT when binding service " + service2);
        }
        ?? r36 = 65536;
        if ((flags & 65536) != 0 && !isCallerSystem) {
            throw new SecurityException("Non-system caller (pid=" + callingPid + ") set BIND_ALMOST_PERCEPTIBLE when binding service " + service2);
        }
        if ((flags & 1048576) != 0) {
            this.mAm.enforceCallingPermission("android.permission.START_ACTIVITIES_FROM_BACKGROUND", "BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS");
        }
        if ((flags & 262144) != 0) {
            this.mAm.enforceCallingPermission("android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND", "BIND_ALLOW_FOREGROUND_SERVICE_STARTS_FROM_BACKGROUND");
        }
        boolean callerFg = callerApp3.mState.getSetSchedGroup() != 0 ? true : z;
        boolean isBindExternal = (flags & Integer.MIN_VALUE) != 0 ? true : z;
        boolean allowInstant = (flags & 4194304) != 0 ? true : z;
        ActivityServiceConnectionsHolder<ConnectionRecord> activity3 = activity;
        Intent service5 = service2;
        ServiceLookupResult res = retrieveServiceLocked(service2, instanceName, isSdkSandboxService, sdkSandboxClientAppUid, sdkSandboxClientAppPackage, resolvedType, callingPackage, callingPid, callingUid3, userId, true, callerFg, isBindExternal, allowInstant);
        if (res == null) {
            return 0;
        }
        if (res.record == null) {
            return -1;
        }
        if (ITranActiveServices.Instance().isAppIdleEnable()) {
            activeServices = this;
            if (activeServices.mScreenOn) {
                service3 = service5;
                callingUid = callingUid3;
            } else {
                callingUid = callingUid3;
                if (ITranActiveServices.Instance().limitBindService(callerApp3, callingPackage, service5, !activeServices.mAm.isUidActiveLOSP(callingUid), false, callingUid)) {
                    Slog.d(TAG, "TranAppIdleMgr limit bindService=" + service5 + ",callingPackage=" + callingPackage);
                    return 0;
                }
                service3 = service5;
            }
        } else {
            activeServices = this;
            service3 = service5;
            callingUid = callingUid3;
        }
        ServiceRecord s4 = res.record;
        boolean packageFrozen = deferServiceBringupIfFrozenLocked(s4, service3, callingPackage, null, callingUid, callingPid, false, callerFg, userId, false, null, true, connection);
        try {
            try {
                try {
                    if (packageFrozen) {
                        s = s4;
                        service4 = service3;
                        callingUid2 = callingUid;
                    } else {
                        s = s4;
                        service4 = service3;
                        callingUid2 = callingUid;
                        if (!requestStartTargetPermissionsReviewIfNeededLocked(s4, callingPackage, null, callingUid, service3, callerFg, userId, true, connection)) {
                            permissionsReviewRequired = true;
                            origId = Binder.clearCallingIdentity();
                            s2 = s;
                            if (activeServices.unscheduleServiceRestartLocked(s2, callerApp3.info.uid, false)) {
                                try {
                                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                        Slog.v(TAG_SERVICE, "BIND SERVICE WHILE RESTART PENDING: " + s2);
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    Binder.restoreCallingIdentity(origId);
                                    throw th;
                                }
                            }
                            if ((flags & 1) == 0) {
                                s2.lastActivity = SystemClock.uptimeMillis();
                                if (s2.hasAutoCreateConnections()) {
                                    z2 = true;
                                } else {
                                    synchronized (activeServices.mAm.mProcessStats.mLock) {
                                        ServiceState stracker = s2.getTracker();
                                        if (stracker != null) {
                                            z2 = true;
                                            stracker.setBound(true, activeServices.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                                        } else {
                                            z2 = true;
                                        }
                                    }
                                }
                            } else {
                                z2 = true;
                            }
                            if ((flags & 2097152) != 0) {
                                activeServices.mAm.requireAllowedAssociationsLocked(s2.appInfo.packageName);
                            }
                            boolean wasStartRequested = s2.startRequested;
                            boolean hadConnections = s2.getConnections().isEmpty() ? z2 : false;
                            activeServices.mAm.startAssociationLocked(callerApp3.uid, callerApp3.processName, callerApp3.mState.getCurProcState(), s2.appInfo.uid, s2.appInfo.longVersionCode, s2.instanceName, s2.processName);
                            Intent service6 = service4;
                            activeServices.mAm.grantImplicitAccess(callerApp3.userId, service6, callerApp3.uid, UserHandle.getAppId(s2.appInfo.uid));
                            AppBindRecord b3 = s2.retrieveAppBindingLocked(service6, callerApp3);
                            c = new ConnectionRecord(b3, activity3, connection, flags, clientLabel, clientIntent, callerApp3.uid, callerApp3.processName, callingPackage, res.aliasComponent);
                            IBinder binder = connection.asBinder();
                            s2.addConnection(binder, c);
                            b3.connections.add(c);
                            if (activity3 != null) {
                                try {
                                    activity3.addConnection(c);
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(origId);
                                    throw th;
                                }
                            }
                            clientPsr = b3.client.mServices;
                            clientPsr.addConnection(c);
                            c.startAssociationIfNeeded();
                            if ((c.flags & 8) != 0) {
                                clientPsr.setHasAboveClient(z2);
                            }
                            if ((c.flags & 16777216) != 0) {
                                s2.allowlistManager = z2;
                            }
                            if ((flags & 1048576) != 0) {
                                s2.setAllowedBgActivityStartsByBinding(z2);
                            }
                            if ((flags & 32768) != 0) {
                                s2.isNotAppComponentUsage = z2;
                            }
                            if (s2.app != null || s2.app.mState == null) {
                                clientPsr2 = clientPsr;
                            } else if (s2.app.mState.getCurProcState() > 2 || (flags & 65536) == 0) {
                                clientPsr2 = clientPsr;
                            } else {
                                clientPsr2 = clientPsr;
                                s2.lastTopAlmostPerceptibleBindRequestUptimeMs = SystemClock.uptimeMillis();
                            }
                            if (s2.app != null) {
                                activeServices.updateServiceClientActivitiesLocked(s2.app.mServices, c, z2);
                            }
                            clist = activeServices.mServiceConnections.get(binder);
                            if (clist != null) {
                                ArrayList<ConnectionRecord> clist3 = new ArrayList<>();
                                activeServices.mServiceConnections.put(binder, clist3);
                                clist2 = clist3;
                            } else {
                                clist2 = clist;
                            }
                            clist2.add(c);
                            if ((flags & 1) == 0) {
                                try {
                                    s2.lastActivity = SystemClock.uptimeMillis();
                                    needOomAdj = true;
                                    try {
                                        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                                            c2 = c;
                                            b = b3;
                                            s = s2;
                                            r36 = callerApp3;
                                            activeServices2 = activeServices;
                                            try {
                                                if (bringUpServiceLocked(s2, service6.getFlags(), callerFg, false, permissionsReviewRequired, packageFrozen, true, callerProc, 2, originalService) != null) {
                                                    activeServices2.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_bindService");
                                                    Slog.d(TAG, "isGriffinSupport  bringUpServiceLocked and unbindServiceLocked" + service6);
                                                    activeServices2.unbindServiceLocked(connection);
                                                    Binder.restoreCallingIdentity(origId);
                                                    return 0;
                                                }
                                                callerApp = r36;
                                            } catch (Throwable th3) {
                                                th = th3;
                                                Binder.restoreCallingIdentity(origId);
                                                throw th;
                                            }
                                        } else {
                                            c2 = c;
                                            b = b3;
                                            s = s2;
                                            callerApp = callerApp3;
                                            activeServices2 = activeServices;
                                        }
                                        if (bringUpServiceLocked(s, service6.getFlags(), callerFg, false, permissionsReviewRequired, packageFrozen, true) != null) {
                                            activeServices2.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_bindService");
                                            Binder.restoreCallingIdentity(origId);
                                            return 0;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } else {
                                c2 = c;
                                b = b3;
                                s = s2;
                                callerApp = callerApp3;
                                activeServices2 = activeServices;
                                needOomAdj = false;
                            }
                            setFgsRestrictionLocked(callingPackage, callingPid, callingUid2, service6, s, userId, false);
                            s3 = s;
                            if (s3.app == null) {
                                try {
                                    ProcessServiceRecord servicePsr = s3.app.mServices;
                                    if ((flags & 134217728) != 0) {
                                        z3 = true;
                                        try {
                                            servicePsr.setTreatLikeActivity(true);
                                        } catch (Throwable th6) {
                                            th = th6;
                                            Binder.restoreCallingIdentity(origId);
                                            throw th;
                                        }
                                    } else {
                                        z3 = true;
                                    }
                                    if (s3.allowlistManager) {
                                        servicePsr.mAllowlistManager = z3;
                                    }
                                    ActivityManagerService activityManagerService = activeServices2.mAm;
                                    ProcessRecord processRecord = s3.app;
                                    try {
                                        if (callerApp.hasActivitiesOrRecentTasks()) {
                                            if (servicePsr.hasClientActivities()) {
                                                callerApp2 = callerApp;
                                                z4 = z3;
                                                b2 = b;
                                                activityManagerService.updateLruProcessLocked(processRecord, z4, b2.client);
                                                activeServices2.mAm.enqueueOomAdjTargetLocked(s3.app);
                                                needOomAdj2 = true;
                                            }
                                        }
                                        if (callerApp2.mState.getCurProcState() > 2 || (flags & 134217728) == 0) {
                                            z4 = false;
                                            b2 = b;
                                            activityManagerService.updateLruProcessLocked(processRecord, z4, b2.client);
                                            activeServices2.mAm.enqueueOomAdjTargetLocked(s3.app);
                                            needOomAdj2 = true;
                                        }
                                        z4 = z3;
                                        b2 = b;
                                        activityManagerService.updateLruProcessLocked(processRecord, z4, b2.client);
                                        activeServices2.mAm.enqueueOomAdjTargetLocked(s3.app);
                                        needOomAdj2 = true;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        Binder.restoreCallingIdentity(origId);
                                        throw th;
                                    }
                                    callerApp2 = callerApp;
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } else {
                                callerApp2 = callerApp;
                                b2 = b;
                                z3 = true;
                                needOomAdj2 = needOomAdj;
                            }
                            if (needOomAdj2) {
                                activeServices2.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_bindService");
                            }
                            FrameworkStatsLog.write((int) FrameworkStatsLog.SERVICE_REQUEST_EVENT_REPORTED, s3.appInfo.uid, callingUid2, ActivityManagerService.getShortAction(service6.getAction()), 2, false, (int) ((s3.app != null || s3.app.getThread() == null) ? 3 : (wasStartRequested || hadConnections) ? 2 : z3));
                            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                Slog.v(TAG_SERVICE, "Bind " + s3 + " with " + b2 + ": received=" + b2.intent.received + " apps=" + b2.intent.apps.size() + " doRebind=" + b2.intent.doRebind);
                            }
                            if (s3.app == null && b2.intent.received) {
                                ComponentName clientSideComponentName = res.aliasComponent != null ? res.aliasComponent : s3.name;
                                try {
                                    c2.conn.connected(clientSideComponentName, b2.intent.binder, false);
                                } catch (Exception e) {
                                    Slog.w(TAG, "Failure sending service " + s3.shortInstanceName + " to connection " + c3.conn.asBinder() + " (in " + c3.binding.client.processName + ")", e);
                                }
                                if (b2.intent.apps.size() == 1 && b2.intent.doRebind) {
                                    activeServices2.requestServiceBindingLocked(s3, b2.intent, callerFg, true);
                                }
                            } else if (!b2.intent.requested) {
                                activeServices2.requestServiceBindingLocked(s3, b2.intent, callerFg, false);
                            }
                            activeServices2.maybeLogBindCrossProfileService(userId, callingPackage, callerApp2.info.uid);
                            activeServices2.getServiceMapLocked(s3.userId).ensureNotStartingBackgroundLocked(s3);
                            Binder.restoreCallingIdentity(origId);
                            activeServices2.notifyBindingServiceEventLocked(callerApp2, callingPackage);
                            return 1;
                        }
                    }
                    activeServices2.maybeLogBindCrossProfileService(userId, callingPackage, callerApp2.info.uid);
                    activeServices2.getServiceMapLocked(s3.userId).ensureNotStartingBackgroundLocked(s3);
                    Binder.restoreCallingIdentity(origId);
                    activeServices2.notifyBindingServiceEventLocked(callerApp2, callingPackage);
                    return 1;
                } catch (Throwable th9) {
                    th = th9;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
                if (s3.app == null) {
                }
                if (!b2.intent.requested) {
                }
            } catch (Throwable th10) {
                th = th10;
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.SERVICE_REQUEST_EVENT_REPORTED, s3.appInfo.uid, callingUid2, ActivityManagerService.getShortAction(service6.getAction()), 2, false, (int) ((s3.app != null || s3.app.getThread() == null) ? 3 : (wasStartRequested || hadConnections) ? 2 : z3));
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            }
        } catch (Throwable th11) {
            th = th11;
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
        permissionsReviewRequired = false;
        origId = Binder.clearCallingIdentity();
        s2 = s;
        if (activeServices.unscheduleServiceRestartLocked(s2, callerApp3.info.uid, false)) {
        }
        if ((flags & 1) == 0) {
        }
        if ((flags & 2097152) != 0) {
        }
        boolean wasStartRequested2 = s2.startRequested;
        boolean hadConnections2 = s2.getConnections().isEmpty() ? z2 : false;
        activeServices.mAm.startAssociationLocked(callerApp3.uid, callerApp3.processName, callerApp3.mState.getCurProcState(), s2.appInfo.uid, s2.appInfo.longVersionCode, s2.instanceName, s2.processName);
        Intent service62 = service4;
        activeServices.mAm.grantImplicitAccess(callerApp3.userId, service62, callerApp3.uid, UserHandle.getAppId(s2.appInfo.uid));
        AppBindRecord b32 = s2.retrieveAppBindingLocked(service62, callerApp3);
        c = new ConnectionRecord(b32, activity3, connection, flags, clientLabel, clientIntent, callerApp3.uid, callerApp3.processName, callingPackage, res.aliasComponent);
        IBinder binder2 = connection.asBinder();
        s2.addConnection(binder2, c);
        b32.connections.add(c);
        if (activity3 != null) {
        }
        clientPsr = b32.client.mServices;
        clientPsr.addConnection(c);
        c.startAssociationIfNeeded();
        if ((c.flags & 8) != 0) {
        }
        if ((c.flags & 16777216) != 0) {
        }
        if ((flags & 1048576) != 0) {
        }
        if ((flags & 32768) != 0) {
        }
        if (s2.app != null) {
        }
        clientPsr2 = clientPsr;
        if (s2.app != null) {
        }
        clist = activeServices.mServiceConnections.get(binder2);
        if (clist != null) {
        }
        clist2.add(c);
        if ((flags & 1) == 0) {
        }
        setFgsRestrictionLocked(callingPackage, callingPid, callingUid2, service62, s, userId, false);
        s3 = s;
        if (s3.app == null) {
        }
        if (needOomAdj2) {
        }
    }

    private void notifyBindingServiceEventLocked(ProcessRecord callerApp, String callingPackage) {
        ApplicationInfo ai = callerApp.info;
        String callerPackage = ai != null ? ai.packageName : callingPackage;
        if (callerPackage != null) {
            this.mAm.mHandler.obtainMessage(75, callerApp.uid, 0, callerPackage).sendToTarget();
        }
    }

    private void maybeLogBindCrossProfileService(int userId, String callingPackage, int callingUid) {
        int callingUserId;
        if (UserHandle.isCore(callingUid) || (callingUserId = UserHandle.getCallingUserId()) == userId || !this.mAm.mUserController.isSameProfileGroup(callingUserId, userId)) {
            return;
        }
        DevicePolicyEventLogger.createEvent(151).setStrings(new String[]{callingPackage}).write();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3220=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0193 A[Catch: all -> 0x01ce, TryCatch #6 {all -> 0x01ce, blocks: (B:3:0x000c, B:5:0x0010, B:7:0x003e, B:9:0x004f, B:11:0x0053, B:13:0x0065, B:14:0x006e, B:16:0x0074, B:18:0x0087, B:20:0x008b, B:21:0x00a3, B:23:0x00a7, B:24:0x00c5, B:26:0x00c9, B:63:0x0196, B:28:0x00e7, B:30:0x00eb, B:31:0x0103, B:33:0x0107, B:50:0x013c, B:51:0x013e, B:60:0x018f, B:62:0x0193, B:65:0x01a1, B:67:0x01a5, B:69:0x01ab, B:34:0x010a, B:70:0x01ac, B:73:0x01bf), top: B:81:0x000c }] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x01a5 A[Catch: all -> 0x01ce, TryCatch #6 {all -> 0x01ce, blocks: (B:3:0x000c, B:5:0x0010, B:7:0x003e, B:9:0x004f, B:11:0x0053, B:13:0x0065, B:14:0x006e, B:16:0x0074, B:18:0x0087, B:20:0x008b, B:21:0x00a3, B:23:0x00a7, B:24:0x00c5, B:26:0x00c9, B:63:0x0196, B:28:0x00e7, B:30:0x00eb, B:31:0x0103, B:33:0x0107, B:50:0x013c, B:51:0x013e, B:60:0x018f, B:62:0x0193, B:65:0x01a1, B:67:0x01a5, B:69:0x01ab, B:34:0x010a, B:70:0x01ac, B:73:0x01bf), top: B:81:0x000c }] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0196 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
        Intent.FilterComparison filter;
        IntentBindRecord b;
        SystemUtil systemUtil;
        boolean z;
        Intent intent2 = intent;
        long origId = Binder.clearCallingIdentity();
        try {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "PUBLISHING " + r + " " + intent2 + ": " + service);
            }
            if (r != null) {
                Intent.FilterComparison filter2 = new Intent.FilterComparison(intent2);
                IntentBindRecord b2 = r.bindings.get(filter2);
                if (b2 != null && !b2.received) {
                    b2.binder = service;
                    b2.requested = true;
                    b2.received = true;
                    ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
                    int conni = connections.size() - 1;
                    while (conni >= 0) {
                        ArrayList<ConnectionRecord> clist = connections.valueAt(conni);
                        int i = 0;
                        while (i < clist.size()) {
                            ConnectionRecord c = clist.get(i);
                            if (filter2.equals(c.binding.intent.intent)) {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Publishing to: " + c);
                                }
                                ComponentName clientSideComponentName = c.aliasComponent != null ? c.aliasComponent : r.name;
                                try {
                                    z = IS_ROOT_ENABLE;
                                    if (z) {
                                        filter = filter2;
                                        try {
                                            b = b2;
                                            try {
                                                try {
                                                    this.mStl.sendNoBinderMessage(r.app.getPid(), r.app.processName, "connected");
                                                } catch (Throwable th) {
                                                    th = th;
                                                    if (IS_ROOT_ENABLE) {
                                                        this.mStl.sendBinderMessage();
                                                    }
                                                    throw th;
                                                }
                                            } catch (Exception e) {
                                                e = e;
                                                Slog.w(TAG, "Failure sending service " + r.shortInstanceName + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                                                if (IS_ROOT_ENABLE) {
                                                    i++;
                                                    intent2 = intent;
                                                    filter2 = filter;
                                                    b2 = b;
                                                } else {
                                                    systemUtil = this.mStl;
                                                    systemUtil.sendBinderMessage();
                                                    i++;
                                                    intent2 = intent;
                                                    filter2 = filter;
                                                    b2 = b;
                                                }
                                            }
                                        } catch (Exception e2) {
                                            e = e2;
                                            b = b2;
                                            Slog.w(TAG, "Failure sending service " + r.shortInstanceName + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                                            if (IS_ROOT_ENABLE) {
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (IS_ROOT_ENABLE) {
                                            }
                                            throw th;
                                        }
                                    } else {
                                        filter = filter2;
                                        b = b2;
                                    }
                                    c.conn.connected(clientSideComponentName, service, false);
                                } catch (Exception e3) {
                                    e = e3;
                                    filter = filter2;
                                    b = b2;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                                if (z) {
                                    systemUtil = this.mStl;
                                    systemUtil.sendBinderMessage();
                                }
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Not publishing to: " + c);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Bound intent: " + c.binding.intent.intent);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Published intent: " + intent2);
                                }
                                filter = filter2;
                                b = b2;
                            }
                            i++;
                            intent2 = intent;
                            filter2 = filter;
                            b2 = b;
                        }
                        conni--;
                        intent2 = intent;
                    }
                }
                serviceDoneExecutingLocked(r, this.mDestroyingServices.contains(r), false, false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateServiceGroupLocked(IServiceConnection connection, int group, int importance) {
        IBinder binder = connection.asBinder();
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "updateServiceGroup: conn=" + binder);
        }
        ArrayList<ConnectionRecord> clist = this.mServiceConnections.get(binder);
        if (clist == null) {
            throw new IllegalArgumentException("Could not find connection for " + connection.asBinder());
        }
        for (int i = clist.size() - 1; i >= 0; i--) {
            ConnectionRecord crec = clist.get(i);
            ServiceRecord srec = crec.binding.service;
            if (srec != null && (srec.serviceInfo.flags & 2) != 0) {
                if (srec.app != null) {
                    ProcessServiceRecord psr = srec.app.mServices;
                    if (group > 0) {
                        psr.setConnectionService(srec);
                        psr.setConnectionGroup(group);
                        psr.setConnectionImportance(importance);
                    } else {
                        psr.setConnectionService(null);
                        psr.setConnectionGroup(0);
                        psr.setConnectionImportance(0);
                    }
                } else if (group > 0) {
                    srec.pendingConnectionGroup = group;
                    srec.pendingConnectionImportance = importance;
                } else {
                    srec.pendingConnectionGroup = 0;
                    srec.pendingConnectionImportance = 0;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean unbindServiceLocked(IServiceConnection connection) {
        IBinder binder = connection.asBinder();
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "unbindService: conn=" + binder);
        }
        ArrayList<ConnectionRecord> clist = this.mServiceConnections.get(binder);
        if (clist == null) {
            Slog.w(TAG, "Unbind failed: could not find connection for " + connection.asBinder());
            return false;
        }
        long origId = Binder.clearCallingIdentity();
        while (clist.size() > 0) {
            try {
                ConnectionRecord r = clist.get(0);
                removeConnectionLocked(r, null, null, true);
                if (clist.size() > 0 && clist.get(0) == r) {
                    Slog.wtf(TAG, "Connection " + r + " not removed for binder " + binder);
                    clist.remove(0);
                }
                ProcessRecord app = r.binding.service.app;
                if (app != null) {
                    ProcessServiceRecord psr = app.mServices;
                    if (psr.mAllowlistManager) {
                        updateAllowlistManagerLocked(psr);
                    }
                    if ((r.flags & 134217728) != 0) {
                        psr.setTreatLikeActivity(true);
                        this.mAm.updateLruProcessLocked(app, true, null);
                    }
                    this.mAm.enqueueOomAdjTargetLocked(app);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
        this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unbindFinishedLocked(ServiceRecord r, Intent intent, boolean doRebind) {
        long origId = Binder.clearCallingIdentity();
        if (r != null) {
            try {
                Intent.FilterComparison filter = new Intent.FilterComparison(intent);
                IntentBindRecord b = r.bindings.get(filter);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "unbindFinished in " + r + " at " + b + ": apps=" + (b != null ? b.apps.size() : 0));
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                if (b != null) {
                    if (b.apps.size() > 0 && !inDestroying) {
                        boolean inFg = false;
                        for (int i = b.apps.size() - 1; i >= 0; i--) {
                            ProcessRecord client = b.apps.valueAt(i).client;
                            if (client != null && client.mState.getSetSchedGroup() != 0) {
                                inFg = true;
                                break;
                            }
                        }
                        try {
                            requestServiceBindingLocked(r, b, inFg, true);
                        } catch (TransactionTooLargeException e) {
                        }
                    } else {
                        b.doRebind = true;
                    }
                }
                serviceDoneExecutingLocked(r, inDestroying, false, false);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    private final ServiceRecord findServiceLocked(ComponentName name, IBinder token, int userId) {
        ServiceRecord r = getServiceByNameLocked(name, userId);
        if (r == token) {
            return r;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ServiceLookupResult {
        final ComponentName aliasComponent;
        final String permission;
        final ServiceRecord record;

        ServiceLookupResult(ServiceRecord _record, ComponentName _aliasComponent) {
            this.record = _record;
            this.permission = null;
            this.aliasComponent = _aliasComponent;
        }

        ServiceLookupResult(String _permission) {
            this.record = null;
            this.permission = _permission;
            this.aliasComponent = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ServiceRestarter implements Runnable {
        private ServiceRecord mService;

        private ServiceRestarter() {
        }

        void setService(ServiceRecord service) {
            this.mService = service;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (this.mService != null && ITranActiveServices.Instance().hookLimitRestartService(new HookActiveServiceInfo(this.mService))) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ActiveServices.this.performServiceRestartLocked(this.mService);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
    }

    private ServiceLookupResult retrieveServiceLocked(Intent service, String instanceName, String resolvedType, String callingPackage, int callingPid, int callingUid, int userId, boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal, boolean allowInstant) {
        return retrieveServiceLocked(service, instanceName, false, 0, null, resolvedType, callingPackage, callingPid, callingUid, userId, createIfNeeded, callingFromFg, isBindExternal, allowInstant);
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:208:0x060e */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:271:0x0242 */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x0217, code lost:
        if ((r2.flags & 2) != 0) goto L64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x0221, code lost:
        throw new java.lang.IllegalArgumentException("Service cannot be both sdk sandbox and isolated");
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:219:0x063a  */
    /* JADX WARN: Removed duplicated region for block: B:256:0x080f  */
    /* JADX WARN: Type inference failed for: r4v9, types: [com.android.server.am.ActivityManagerService] */
    /* JADX WARN: Type inference failed for: r7v10 */
    /* JADX WARN: Type inference failed for: r7v11 */
    /* JADX WARN: Type inference failed for: r7v14, types: [android.content.pm.ServiceInfo] */
    /* JADX WARN: Type inference failed for: r7v30 */
    /* JADX WARN: Type inference failed for: r7v31 */
    /* JADX WARN: Type inference failed for: r7v7, types: [int] */
    /* JADX WARN: Type inference failed for: r7v8 */
    /* JADX WARN: Type inference failed for: r7v9, types: [java.lang.String] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ServiceLookupResult retrieveServiceLocked(Intent service, String instanceName, boolean isSdkSandboxService, int sdkSandboxClientAppUid, String sdkSandboxClientAppPackage, String resolvedType, String callingPackage, int callingPid, int callingUid, int userId, boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal, boolean allowInstant) {
        ComponentName comp;
        ComponentName comp2;
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        ServiceRecord r;
        int opCode;
        int flags;
        int flags2;
        int flags3;
        ServiceInfo sInfo;
        String str6;
        ServiceInfo sInfo2;
        ComponentName className;
        ComponentName className2;
        ComponentName name;
        ServiceMap smap;
        Intent.FilterComparison filter;
        int flags4;
        ServiceInfo sInfo3;
        ServiceInfo sInfo4;
        if (isSdkSandboxService && instanceName == null) {
            throw new IllegalArgumentException("No instanceName provided for sdk sandbox process");
        }
        ServiceRecord r2 = null;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "retrieveServiceLocked: " + service + " type=" + resolvedType + " callingUid=" + callingUid);
        }
        int userId2 = this.mAm.mUserController.handleIncomingUser(callingPid, callingUid, userId, false, getAllowMode(service, callingPackage), HostingRecord.HOSTING_TYPE_SERVICE, callingPackage);
        ServiceMap smap2 = getServiceMapLocked(userId2);
        ComponentAliasResolver.Resolution<ComponentName> resolution = this.mAm.mComponentAliasResolver.resolveService(service, resolvedType, 0, userId2, callingUid);
        if (instanceName == null) {
            comp = service.getComponent();
        } else {
            ComponentName realComp = service.getComponent();
            if (realComp == null) {
                throw new IllegalArgumentException("Can't use custom instance name '" + instanceName + "' without expicit component in Intent");
            }
            comp = new ComponentName(realComp.getPackageName(), realComp.getClassName() + ":" + instanceName);
        }
        if (comp != null) {
            ServiceRecord r3 = smap2.mServicesByInstanceName.get(comp);
            r2 = r3;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE && r2 != null) {
                Slog.v(TAG_SERVICE, "Retrieved by component: " + r2);
            }
        }
        if (r2 == null && !isBindExternal && instanceName == null) {
            ServiceRecord r4 = smap2.mServicesByIntent.get(new Intent.FilterComparison(service));
            r2 = r4;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE && r2 != null) {
                Slog.v(TAG_SERVICE, "Retrieved by intent: " + r2);
            }
        }
        if (r2 == null) {
            comp2 = comp;
        } else {
            comp2 = comp;
            if (this.mAm.getPackageManagerInternal().filterAppAccess(r2.packageName, callingUid, userId2)) {
                Slog.w(TAG_SERVICE, "Unable to start service " + service + " U=" + userId2 + ": not found");
                return null;
            } else if ((r2.serviceInfo.flags & 4) != 0 && !callingPackage.equals(r2.packageName)) {
                r2 = null;
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Whoops, can't use existing external service");
                }
            }
        }
        ServiceRecord r5 = r2;
        ServiceMap smap3 = smap2;
        if (r5 != null) {
            str = " and ";
            str2 = "Service lookup failed: ";
            str3 = TAG;
            str4 = "association not allowed between packages ";
            str5 = callingPackage;
            r = r5;
        } else {
            if (allowInstant) {
                int flags5 = 268436480 | 8388608;
                flags = flags5;
            } else {
                flags = 268436480;
            }
            try {
                PackageManagerInternal packageManagerInternal = this.mAm.getPackageManagerInternal();
                long j = flags;
                flags2 = flags;
                flags3 = TAG;
                str4 = "Unable to start service ";
                str5 = callingPackage;
                try {
                    ResolveInfo rInfo = packageManagerInternal.resolveService(service, resolvedType, j, userId2, callingUid);
                    sInfo = rInfo != null ? rInfo.serviceInfo : null;
                } catch (RemoteException e) {
                    str3 = flags3;
                    str2 = "Service lookup failed: ";
                    str = " and ";
                    str4 = "association not allowed between packages ";
                }
            } catch (RemoteException e2) {
                str = " and ";
                str2 = "Service lookup failed: ";
                str3 = TAG;
                str4 = "association not allowed between packages ";
                str5 = callingPackage;
            }
            if (sInfo == null) {
                Slog.w(TAG_SERVICE, str4 + service + " U=" + userId2 + ": not found");
                return null;
            }
            if (instanceName != null && (sInfo.flags & 2) == 0 && !isSdkSandboxService) {
                throw new IllegalArgumentException("Can't use instance name '" + instanceName + "' with non-isolated non-sdk sandbox service '" + sInfo.name + "'");
            }
            ComponentName className3 = new ComponentName(sInfo.applicationInfo.packageName, sInfo.name);
            ComponentName name2 = comp2 != null ? comp2 : className3;
            ?? r42 = this.mAm;
            String packageName = name2.getPackageName();
            ?? r7 = sInfo.applicationInfo.uid;
            try {
                try {
                    try {
                        if (!r42.validateAssociationAllowedLocked(str5, callingUid, packageName, r7)) {
                            try {
                                str4 = "association not allowed between packages ";
                                r7 = " and ";
                                String msg = str4 + str5 + ((String) r7) + name2.getPackageName();
                                packageName = "Service lookup failed: ";
                                try {
                                    Slog.w(flags3, packageName + msg);
                                    return new ServiceLookupResult(msg);
                                } catch (RemoteException e3) {
                                    str2 = packageName;
                                    str = r7;
                                    str3 = flags3;
                                }
                            } catch (RemoteException e4) {
                                str4 = "association not allowed between packages ";
                                str3 = flags3;
                                str2 = "Service lookup failed: ";
                                str = " and ";
                                r = r5;
                                if (r == null) {
                                }
                            }
                        } else {
                            r7 = " and ";
                            str4 = "association not allowed between packages ";
                            String definingPackageName = sInfo.applicationInfo.packageName;
                            int definingUid = sInfo.applicationInfo.uid;
                            packageName = "BIND_EXTERNAL_SERVICE failed, ";
                            try {
                                if ((sInfo.flags & 4) != 0) {
                                    if (!isBindExternal) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE required for " + name2);
                                    }
                                    if (!sInfo.exported) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + className3 + " is not exported");
                                    }
                                    if ((sInfo.flags & 2) == 0) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + className3 + " is not an isolatedProcess");
                                    }
                                    str6 = r7;
                                    if (AppGlobals.getPackageManager().getPackageUid(str5, 0L, userId2) != callingUid) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, calling package not owned by calling UID ");
                                    }
                                    ApplicationInfo aInfo = AppGlobals.getPackageManager().getApplicationInfo(str5, (long) GadgetFunction.NCM, userId2);
                                    if (aInfo == null) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, could not resolve client package " + str5);
                                    }
                                    ServiceInfo sInfo5 = new ServiceInfo(sInfo);
                                    sInfo5.applicationInfo = new ApplicationInfo(sInfo5.applicationInfo);
                                    sInfo5.applicationInfo.packageName = aInfo.packageName;
                                    sInfo5.applicationInfo.uid = aInfo.uid;
                                    ComponentName name3 = new ComponentName(aInfo.packageName, name2.getClassName());
                                    packageName = aInfo.packageName;
                                    ComponentName className4 = new ComponentName(packageName, instanceName == null ? className3.getClassName() : className3.getClassName() + ":" + instanceName);
                                    service.setComponent(name3);
                                    className = className4;
                                    sInfo2 = sInfo5;
                                    className2 = name3;
                                } else {
                                    str6 = r7;
                                    if (isBindExternal) {
                                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + name2 + " is not an externalService");
                                    }
                                    sInfo2 = sInfo;
                                    className = className3;
                                    className2 = name2;
                                }
                                if (userId2 <= 0) {
                                    name = className2;
                                    str3 = flags3;
                                    str2 = "Service lookup failed: ";
                                    flags3 = flags2;
                                    str = str6;
                                    smap = smap3;
                                    r7 = sInfo2;
                                } else {
                                    ActivityManagerService activityManagerService = this.mAm;
                                    String str7 = sInfo2.processName;
                                    ApplicationInfo applicationInfo = sInfo2.applicationInfo;
                                    packageName = sInfo2.name;
                                    ComponentName name4 = className2;
                                    if (!activityManagerService.isSingleton(str7, applicationInfo, packageName, sInfo2.flags)) {
                                        str3 = flags3;
                                        str2 = "Service lookup failed: ";
                                        flags4 = flags2;
                                        str = str6;
                                        name = name4;
                                        sInfo3 = sInfo2;
                                    } else if (!this.mAm.isValidSingletonCall(callingUid, sInfo2.applicationInfo.uid)) {
                                        str3 = flags3;
                                        str2 = "Service lookup failed: ";
                                        flags4 = flags2;
                                        str = str6;
                                        name = name4;
                                        sInfo3 = sInfo2;
                                    } else {
                                        userId2 = 0;
                                        smap3 = getServiceMapLocked(0);
                                        long token = Binder.clearCallingIdentity();
                                        try {
                                            packageName = flags3;
                                            flags4 = flags2;
                                            str3 = packageName;
                                            str2 = "Service lookup failed: ";
                                            name = name4;
                                            str = str6;
                                            try {
                                                ResolveInfo rInfoForUserId0 = this.mAm.getPackageManagerInternal().resolveService(service, resolvedType, flags2, 0, callingUid);
                                                if (rInfoForUserId0 == null) {
                                                    Slog.w(TAG_SERVICE, "Unable to resolve service " + service + " U=0: not found");
                                                    Binder.restoreCallingIdentity(token);
                                                    return null;
                                                }
                                                sInfo4 = rInfoForUserId0.serviceInfo;
                                                Binder.restoreCallingIdentity(token);
                                                ServiceInfo sInfo6 = new ServiceInfo(sInfo4);
                                                sInfo6.applicationInfo = this.mAm.getAppInfoForUser(sInfo6.applicationInfo, userId2);
                                                smap = smap3;
                                                r7 = sInfo6;
                                                flags3 = flags4;
                                            } catch (Throwable th) {
                                                th = th;
                                                Binder.restoreCallingIdentity(token);
                                                throw th;
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    sInfo4 = sInfo3;
                                    ServiceInfo sInfo62 = new ServiceInfo(sInfo4);
                                    sInfo62.applicationInfo = this.mAm.getAppInfoForUser(sInfo62.applicationInfo, userId2);
                                    smap = smap3;
                                    r7 = sInfo62;
                                    flags3 = flags4;
                                }
                                try {
                                    ServiceRecord r6 = smap.mServicesByInstanceName.get(name);
                                    try {
                                        if (ActivityManagerDebugConfig.DEBUG_SERVICE && r6 != null) {
                                            Slog.v(TAG_SERVICE, "Retrieved via pm by intent: " + r6);
                                        }
                                        if (r6 == null && createIfNeeded) {
                                            Intent.FilterComparison filter2 = new Intent.FilterComparison(service.cloneFilter());
                                            ServiceRestarter res = new ServiceRestarter();
                                            String sdkSandboxProcessName = isSdkSandboxService ? instanceName : null;
                                            r6 = new ServiceRecord(this.mAm, className, name, definingPackageName, definingUid, filter2, r7, callingFromFg, res, sdkSandboxProcessName, sdkSandboxClientAppUid, sdkSandboxClientAppPackage);
                                            res.setService(r6);
                                            smap.mServicesByInstanceName.put(name, r6);
                                            smap.mServicesByIntent.put(filter2, r6);
                                            int i = this.mPendingServices.size() - 1;
                                            while (i >= 0) {
                                                ServiceRecord pr = this.mPendingServices.get(i);
                                                if (pr.serviceInfo.applicationInfo.uid != ((ServiceInfo) r7).applicationInfo.uid) {
                                                    filter = filter2;
                                                } else if (!pr.instanceName.equals(name)) {
                                                    filter = filter2;
                                                } else {
                                                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                                        filter = filter2;
                                                        Slog.v(TAG_SERVICE, "Remove pending: " + pr);
                                                    } else {
                                                        filter = filter2;
                                                    }
                                                    this.mPendingServices.remove(i);
                                                }
                                                i--;
                                                filter2 = filter;
                                            }
                                            for (int i2 = this.mPendingBringups.size() - 1; i2 >= 0; i2--) {
                                                ServiceRecord pr2 = this.mPendingBringups.keyAt(i2);
                                                if (pr2.serviceInfo.applicationInfo.uid == ((ServiceInfo) r7).applicationInfo.uid && pr2.instanceName.equals(name)) {
                                                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                                        Slog.v(TAG_SERVICE, "Remove pending bringup: " + pr2);
                                                    }
                                                    this.mPendingBringups.removeAt(i2);
                                                }
                                            }
                                            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                                packageName = "Retrieve created new service: ";
                                                Slog.v(TAG_SERVICE, "Retrieve created new service: " + r6);
                                            }
                                        }
                                        r = r6;
                                    } catch (RemoteException e5) {
                                        r5 = r6;
                                    }
                                } catch (RemoteException e6) {
                                }
                            } catch (RemoteException e7) {
                            }
                        }
                    } catch (RemoteException e8) {
                        str2 = packageName;
                        str = r7;
                        str3 = flags3;
                    }
                } catch (RemoteException e9) {
                    str = r7;
                    str3 = flags3;
                    str2 = "Service lookup failed: ";
                }
            } catch (RemoteException e10) {
            }
            r = r5;
        }
        if (r == null) {
            r.mRecentCallingPackage = str5;
            r.mRecentCallingUid = callingUid;
            try {
                r.mRecentCallerApplicationInfo = this.mAm.mContext.getPackageManager().getApplicationInfoAsUser(str5, 0, UserHandle.getUserId(callingUid));
            } catch (PackageManager.NameNotFoundException e11) {
            }
            if (!this.mAm.validateAssociationAllowedLocked(str5, callingUid, r.packageName, r.appInfo.uid)) {
                String msg2 = str4 + str5 + str + r.packageName;
                Slog.w(str3, str2 + msg2);
                return new ServiceLookupResult(msg2);
            }
            String str8 = str3;
            if (!this.mAm.mIntentFirewall.checkService(r.name, service, callingUid, callingPid, resolvedType, r.appInfo)) {
                return new ServiceLookupResult("blocked by firewall");
            }
            if (ActivityManagerService.checkComponentPermission(r.permission, callingPid, callingUid, r.appInfo.uid, r.exported) != 0) {
                if (!r.exported) {
                    Slog.w(str8, "Permission Denial: Accessing service " + r.shortInstanceName + " from pid=" + callingPid + ", uid=" + callingUid + " that is not exported from uid " + r.appInfo.uid);
                    return new ServiceLookupResult("not exported from uid " + r.appInfo.uid);
                }
                Slog.w(str8, "Permission Denial: Accessing service " + r.shortInstanceName + " from pid=" + callingPid + ", uid=" + callingUid + " requires " + r.permission);
                return new ServiceLookupResult(r.permission);
            } else if ("android.permission.BIND_HOTWORD_DETECTION_SERVICE".equals(r.permission) && callingUid != 1000) {
                Slog.w(str8, "Permission Denial: Accessing service " + r.shortInstanceName + " from pid=" + callingPid + ", uid=" + callingUid + " requiring permission " + r.permission + " can only be bound to from the system.");
                return new ServiceLookupResult("can only be bound to by the system.");
            } else if (r.permission == null || str5 == null || (opCode = AppOpsManager.permissionToOpCode(r.permission)) == -1 || this.mAm.getAppOpsManager().checkOpNoThrow(opCode, callingUid, str5) == 0) {
                return new ServiceLookupResult(r, resolution.getAlias());
            } else {
                Slog.w(str8, "Appop Denial: Accessing service " + r.shortInstanceName + " from pid=" + callingPid + ", uid=" + callingUid + " requires appop " + AppOpsManager.opToName(opCode));
                return null;
            }
        }
        return null;
    }

    private int getAllowMode(Intent service, String callingPackage) {
        if (callingPackage != null && service.getComponent() != null && callingPackage.equals(service.getComponent().getPackageName())) {
            return 3;
        }
        return 1;
    }

    private boolean bumpServiceExecutingLocked(ServiceRecord r, boolean fg, String why, String oomAdjReason) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, ">>> EXECUTING " + why + " of " + r + " in app " + r.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, ">>> EXECUTING " + why + " of " + r.shortInstanceName);
        }
        boolean timeoutNeeded = true;
        if (this.mAm.mBootPhase < 600 && r.app != null && r.app.getPid() == ActivityManagerService.MY_PID) {
            Slog.w(TAG, "Too early to start/bind service in system_server: Phase=" + this.mAm.mBootPhase + " " + r.getComponentName());
            timeoutNeeded = false;
        }
        if (r.executeNesting == 0) {
            r.executeFg = fg;
            synchronized (this.mAm.mProcessStats.mLock) {
                ServiceState stracker = r.getTracker();
                if (stracker != null) {
                    stracker.setExecuting(true, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
            }
            if (r.app != null) {
                ProcessServiceRecord psr = r.app.mServices;
                psr.startExecutingService(r);
                psr.setExecServicesFg(psr.shouldExecServicesFg() || fg);
                if (timeoutNeeded && psr.numberOfExecutingServices() == 1) {
                    scheduleServiceTimeoutLocked(r.app);
                }
            }
        } else if (r.app != null && fg) {
            ProcessServiceRecord psr2 = r.app.mServices;
            if (!psr2.shouldExecServicesFg()) {
                psr2.setExecServicesFg(true);
                if (timeoutNeeded) {
                    scheduleServiceTimeoutLocked(r.app);
                }
            }
        }
        boolean oomAdjusted = false;
        if (oomAdjReason != null && r.app != null && r.app.mState.getCurProcState() > 10) {
            this.mAm.enqueueOomAdjTargetLocked(r.app);
            this.mAm.updateOomAdjPendingTargetsLocked(oomAdjReason);
            oomAdjusted = true;
        }
        r.executeFg |= fg;
        r.executeNesting++;
        r.executingStart = SystemClock.uptimeMillis();
        return oomAdjusted;
    }

    private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i, boolean execInFg, boolean rebind) throws TransactionTooLargeException {
        if (r.app == null || r.app.getThread() == null) {
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG_SERVICE, "requestBind " + i + ": requested=" + i.requested + " rebind=" + rebind);
        }
        if ((!i.requested || rebind) && i.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind", "updateOomAdj_bindService");
                r.app.getThread().scheduleBindService(r, i.intent.getIntent(), rebind, r.app.mState.getReportedProcState());
                if (!rebind) {
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + r, e);
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying, false);
                throw e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + r);
                }
                boolean inDestroying2 = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying2, inDestroying2, false);
                return false;
            }
        }
        return true;
    }

    private final boolean scheduleServiceRestartLocked(ServiceRecord r, boolean allowCancel) {
        String reason;
        boolean inRestarting;
        boolean N;
        String reason2;
        String reason3;
        boolean inRestarting2;
        boolean inRestarting3;
        boolean inRestarting4;
        boolean repeat;
        long minDuration;
        boolean repeat2;
        boolean canceled;
        int N2;
        boolean N3;
        if (!this.mAm.mAtmInternal.isShuttingDown()) {
            ServiceMap smap = getServiceMapLocked(r.userId);
            if (smap.mServicesByInstanceName.get(r.instanceName) != r) {
                ServiceRecord cur = smap.mServicesByInstanceName.get(r.instanceName);
                Slog.wtf(TAG, "Attempting to schedule restart of " + r + " when found in map: " + cur);
                return false;
            }
            long now = SystemClock.uptimeMillis();
            int oldPosInRestarting = this.mRestartingServices.indexOf(r);
            boolean inRestarting5 = oldPosInRestarting != -1;
            if ((r.serviceInfo.applicationInfo.flags & 8) == 0) {
                long minDuration2 = this.mAm.mConstants.SERVICE_RESTART_DURATION;
                long resetTime = this.mAm.mConstants.SERVICE_RESET_RUN_DURATION;
                boolean canceled2 = false;
                int N4 = r.deliveredStarts.size();
                if (N4 > 0) {
                    int i = N4 - 1;
                    while (i >= 0) {
                        ServiceRecord.StartItem si = r.deliveredStarts.get(i);
                        si.removeUriPermissionsLocked();
                        if (si.intent == null) {
                            N2 = N4;
                            N3 = inRestarting5;
                        } else {
                            if (allowCancel) {
                                canceled = canceled2;
                                if (si.deliveryCount >= 3 || si.doneExecutingCount >= 6) {
                                    N2 = N4;
                                    Slog.w(TAG, "Canceling start item " + si.intent + " in service " + r.shortInstanceName);
                                    canceled2 = true;
                                    N3 = inRestarting5;
                                } else {
                                    N2 = N4;
                                }
                            } else {
                                canceled = canceled2;
                                N2 = N4;
                            }
                            r.pendingStarts.add(0, si);
                            N3 = inRestarting5;
                            long dur = (SystemClock.uptimeMillis() - si.deliveredTime) * 2;
                            if (minDuration2 < dur) {
                                minDuration2 = dur;
                            }
                            if (resetTime < dur) {
                                resetTime = dur;
                            }
                            canceled2 = canceled;
                        }
                        i--;
                        inRestarting5 = N3;
                        N4 = N2;
                    }
                    N = inRestarting5;
                    r.deliveredStarts.clear();
                } else {
                    N = inRestarting5;
                }
                if (allowCancel) {
                    boolean shouldStop = r.canStopIfKilled(canceled2);
                    if (shouldStop && !r.hasAutoCreateConnections()) {
                        return false;
                    }
                    reason2 = (!r.startRequested || shouldStop) ? "connection" : "start-requested";
                } else {
                    reason2 = "always";
                }
                r.totalRestartCount++;
                if (r.restartDelay == 0) {
                    r.restartCount++;
                    r.restartDelay = minDuration2;
                    reason3 = reason2;
                } else if (r.crashCount > 1) {
                    reason3 = reason2;
                    r.restartDelay = this.mAm.mConstants.BOUND_SERVICE_CRASH_RESTART_DURATION * (r.crashCount - 1);
                } else {
                    reason3 = reason2;
                    if (now <= r.restartTime + resetTime) {
                        r.restartDelay *= this.mAm.mConstants.SERVICE_RESTART_DURATION_FACTOR;
                        if (r.restartDelay < minDuration2) {
                            r.restartDelay = minDuration2;
                        }
                    } else {
                        r.restartCount = 1;
                        r.restartDelay = minDuration2;
                    }
                }
                if (!isServiceRestartBackoffEnabledLocked(r.packageName)) {
                    reason = reason3;
                    r.restartDelay = this.mAm.mConstants.SERVICE_RESTART_DURATION;
                    r.nextRestartTime = r.restartDelay + now;
                    inRestarting2 = N;
                } else {
                    long j = r.restartDelay + now;
                    r.mEarliestRestartTime = j;
                    r.nextRestartTime = j;
                    if (!N) {
                        inRestarting3 = N;
                    } else {
                        this.mRestartingServices.remove(oldPosInRestarting);
                        inRestarting3 = false;
                    }
                    if (!this.mRestartingServices.isEmpty()) {
                        long restartTimeBetween = getExtraRestartTimeInBetweenLocked() + this.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN;
                        while (true) {
                            boolean repeat3 = false;
                            long nextRestartTime = r.nextRestartTime;
                            int i2 = this.mRestartingServices.size() - 1;
                            while (true) {
                                if (i2 < 0) {
                                    inRestarting4 = inRestarting3;
                                    repeat = repeat3;
                                    minDuration = minDuration2;
                                    reason = reason3;
                                    break;
                                }
                                inRestarting4 = inRestarting3;
                                ServiceRecord r2 = this.mRestartingServices.get(i2);
                                minDuration = minDuration2;
                                reason = reason3;
                                long nextRestartTime2 = r2.nextRestartTime;
                                if (nextRestartTime >= nextRestartTime2 - restartTimeBetween && nextRestartTime < nextRestartTime2 + restartTimeBetween) {
                                    r.nextRestartTime = nextRestartTime2 + restartTimeBetween;
                                    r.restartDelay = r.nextRestartTime - now;
                                    repeat2 = true;
                                    break;
                                }
                                repeat = repeat3;
                                if (nextRestartTime >= nextRestartTime2 + restartTimeBetween) {
                                    break;
                                }
                                i2--;
                                reason3 = reason;
                                inRestarting3 = inRestarting4;
                                repeat3 = repeat;
                                minDuration2 = minDuration;
                            }
                            repeat2 = repeat;
                            if (!repeat2) {
                                break;
                            }
                            reason3 = reason;
                            inRestarting3 = inRestarting4;
                            minDuration2 = minDuration;
                        }
                    } else {
                        long extraDelay = getExtraRestartTimeInBetweenLocked();
                        r.nextRestartTime = Math.max(now + extraDelay, r.nextRestartTime);
                        r.restartDelay = r.nextRestartTime - now;
                        inRestarting4 = inRestarting3;
                        reason = reason3;
                    }
                    inRestarting2 = inRestarting4;
                }
                inRestarting = inRestarting2;
            } else {
                r.totalRestartCount++;
                r.restartCount = 0;
                r.restartDelay = 0L;
                r.mEarliestRestartTime = 0L;
                r.nextRestartTime = now;
                reason = "persistent";
                inRestarting = inRestarting5;
            }
            r.mRestartSchedulingTime = now;
            if (!inRestarting) {
                if (oldPosInRestarting == -1) {
                    r.createdFromFg = false;
                    synchronized (this.mAm.mProcessStats.mLock) {
                        r.makeRestarting(this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                }
                boolean added = false;
                int i3 = 0;
                int size = this.mRestartingServices.size();
                while (true) {
                    if (i3 >= size) {
                        break;
                    }
                    ServiceRecord r22 = this.mRestartingServices.get(i3);
                    int size2 = size;
                    if (r22.nextRestartTime <= r.nextRestartTime) {
                        i3++;
                        size = size2;
                    } else {
                        this.mRestartingServices.add(i3, r);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    this.mRestartingServices.add(r);
                }
            }
            cancelForegroundNotificationLocked(r);
            performScheduleRestartLocked(r, "Scheduling", reason, now);
            return true;
        }
        Slog.w(TAG, "Not scheduling restart of crashed service " + r.shortInstanceName + " - system is shutting down");
        return false;
    }

    void performScheduleRestartLocked(ServiceRecord r, String scheduling, String reason, long now) {
        if (r.fgRequired && r.fgWaiting) {
            this.mAm.mHandler.removeMessages(66, r);
            r.fgWaiting = false;
        }
        this.mAm.mHandler.removeCallbacks(r.restarter);
        this.mAm.mHandler.postAtTime(r.restarter, r.nextRestartTime);
        r.nextRestartTime = r.restartDelay + now;
        Slog.w(TAG, scheduling + " restart of crashed service " + r.shortInstanceName + " in " + r.restartDelay + "ms for " + reason);
        EventLog.writeEvent((int) EventLogTags.AM_SCHEDULE_SERVICE_RESTART, Integer.valueOf(r.userId), r.shortInstanceName, Long.valueOf(r.restartDelay));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void rescheduleServiceRestartOnMemoryPressureIfNeededLocked(int prevMemFactor, int curMemFactor, String reason, long now) {
        boolean enabled = this.mAm.mConstants.mEnableExtraServiceRestartDelayOnMemPressure;
        if (!enabled) {
            return;
        }
        performRescheduleServiceRestartOnMemoryPressureLocked(this.mAm.mConstants.mExtraServiceRestartDelayOnMemPressure[prevMemFactor], this.mAm.mConstants.mExtraServiceRestartDelayOnMemPressure[curMemFactor], reason, now);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void rescheduleServiceRestartOnMemoryPressureIfNeededLocked(boolean prevEnabled, boolean curEnabled, long now) {
        if (prevEnabled == curEnabled) {
            return;
        }
        int memFactor = this.mAm.mAppProfiler.getLastMemoryLevelLocked();
        long delay = this.mAm.mConstants.mExtraServiceRestartDelayOnMemPressure[memFactor];
        performRescheduleServiceRestartOnMemoryPressureLocked(prevEnabled ? delay : 0L, curEnabled ? delay : 0L, "config", now);
    }

    void rescheduleServiceRestartIfPossibleLocked(long extraRestartTimeBetween, long minRestartTimeBetween, String reason, long now) {
        long spanForInsertOne;
        long j;
        long restartTimeBetween = extraRestartTimeBetween + minRestartTimeBetween;
        long spanForInsertOne2 = restartTimeBetween * 2;
        long lastRestartTime = now;
        int lastRestartTimePos = -1;
        int size = this.mRestartingServices.size();
        int i = 0;
        while (i < size) {
            ServiceRecord r = this.mRestartingServices.get(i);
            if ((r.serviceInfo.applicationInfo.flags & 8) != 0) {
                spanForInsertOne = spanForInsertOne2;
            } else if (isServiceRestartBackoffEnabledLocked(r.packageName)) {
                spanForInsertOne = spanForInsertOne2;
                long spanForInsertOne3 = r.mEarliestRestartTime;
                if (lastRestartTime + restartTimeBetween <= spanForInsertOne3) {
                    long j2 = r.mEarliestRestartTime;
                    if (i > 0) {
                        j = this.mRestartingServices.get(i - 1).nextRestartTime + restartTimeBetween;
                    } else {
                        j = 0;
                    }
                    r.nextRestartTime = Math.max(now, Math.max(j2, j));
                } else {
                    if (lastRestartTime <= now) {
                        r.nextRestartTime = Math.max(now, Math.max(r.mEarliestRestartTime, r.mRestartSchedulingTime + extraRestartTimeBetween));
                    } else {
                        r.nextRestartTime = Math.max(now, lastRestartTime + restartTimeBetween);
                    }
                    if (i > lastRestartTimePos + 1) {
                        this.mRestartingServices.remove(i);
                        this.mRestartingServices.add(lastRestartTimePos + 1, r);
                    }
                }
                int j3 = lastRestartTimePos + 1;
                long lastRestartTime2 = lastRestartTime;
                int lastRestartTimePos2 = lastRestartTimePos;
                while (j3 <= i) {
                    ServiceRecord r2 = this.mRestartingServices.get(j3);
                    long timeInBetween = r2.nextRestartTime - (j3 == 0 ? lastRestartTime2 : this.mRestartingServices.get(j3 - 1).nextRestartTime);
                    if (timeInBetween >= spanForInsertOne) {
                        break;
                    }
                    lastRestartTime2 = r2.nextRestartTime;
                    lastRestartTimePos2 = j3;
                    j3++;
                }
                r.restartDelay = r.nextRestartTime - now;
                performScheduleRestartLocked(r, "Rescheduling", reason, now);
                lastRestartTime = lastRestartTime2;
                lastRestartTimePos = lastRestartTimePos2;
                i++;
                spanForInsertOne2 = spanForInsertOne;
            } else {
                spanForInsertOne = spanForInsertOne2;
            }
            lastRestartTime = r.nextRestartTime;
            lastRestartTimePos = i;
            i++;
            spanForInsertOne2 = spanForInsertOne;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0092  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void performRescheduleServiceRestartOnMemoryPressureLocked(long oldExtraDelay, long newExtraDelay, String reason, long now) {
        long delta;
        int size;
        int i;
        boolean reschedule;
        long delta2 = newExtraDelay - oldExtraDelay;
        if (delta2 == 0) {
            return;
        }
        if (delta2 > 0) {
            long restartTimeBetween = this.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN + newExtraDelay;
            long lastRestartTime = now;
            int size2 = this.mRestartingServices.size();
            int i2 = 0;
            while (i2 < size2) {
                ServiceRecord r = this.mRestartingServices.get(i2);
                if ((r.serviceInfo.applicationInfo.flags & 8) != 0) {
                    delta = delta2;
                    size = size2;
                    i = i2;
                } else if (isServiceRestartBackoffEnabledLocked(r.packageName)) {
                    boolean reschedule2 = false;
                    if (lastRestartTime <= now) {
                        long oldVal = r.nextRestartTime;
                        delta = delta2;
                        size = size2;
                        i = i2;
                        r.nextRestartTime = Math.max(now, Math.max(r.mEarliestRestartTime, r.mRestartSchedulingTime + newExtraDelay));
                        reschedule2 = r.nextRestartTime != oldVal;
                    } else {
                        delta = delta2;
                        size = size2;
                        i = i2;
                        if (r.nextRestartTime - lastRestartTime < restartTimeBetween) {
                            r.nextRestartTime = Math.max(lastRestartTime + restartTimeBetween, now);
                            reschedule = true;
                            r.restartDelay = r.nextRestartTime - now;
                            long lastRestartTime2 = r.nextRestartTime;
                            if (reschedule) {
                                performScheduleRestartLocked(r, "Rescheduling", reason, now);
                            }
                            lastRestartTime = lastRestartTime2;
                            i2 = i + 1;
                            delta2 = delta;
                            size2 = size;
                        }
                    }
                    reschedule = reschedule2;
                    r.restartDelay = r.nextRestartTime - now;
                    long lastRestartTime22 = r.nextRestartTime;
                    if (reschedule) {
                    }
                    lastRestartTime = lastRestartTime22;
                    i2 = i + 1;
                    delta2 = delta;
                    size2 = size;
                } else {
                    delta = delta2;
                    size = size2;
                    i = i2;
                }
                lastRestartTime = r.nextRestartTime;
                i2 = i + 1;
                delta2 = delta;
                size2 = size;
            }
        } else if (delta2 < 0) {
            rescheduleServiceRestartIfPossibleLocked(newExtraDelay, this.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN, reason, now);
        }
    }

    long getExtraRestartTimeInBetweenLocked() {
        if (!this.mAm.mConstants.mEnableExtraServiceRestartDelayOnMemPressure) {
            return 0L;
        }
        int memFactor = this.mAm.mAppProfiler.getLastMemoryLevelLocked();
        return this.mAm.mConstants.mExtraServiceRestartDelayOnMemPressure[memFactor];
    }

    final void performServiceRestartLocked(ServiceRecord r) {
        if (!this.mRestartingServices.contains(r)) {
            return;
        }
        if (!isServiceNeededLocked(r, false, false)) {
            Slog.wtf(TAG, "Restarting service that is not needed: " + r);
            return;
        }
        try {
            bringUpServiceLocked(r, r.intent.getIntent().getFlags(), r.createdFromFg, true, false, false, true);
        } catch (TransactionTooLargeException e) {
        } catch (Throwable th) {
            this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
            throw th;
        }
        this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
    }

    private final boolean unscheduleServiceRestartLocked(ServiceRecord r, int callingUid, boolean force) {
        if (!force && r.restartDelay == 0) {
            return false;
        }
        boolean removed = this.mRestartingServices.remove(r);
        if (removed || callingUid != r.appInfo.uid) {
            r.resetRestartCounter();
        }
        if (removed) {
            clearRestartingIfNeededLocked(r);
        }
        this.mAm.mHandler.removeCallbacks(r.restarter);
        return true;
    }

    private void clearRestartingIfNeededLocked(ServiceRecord r) {
        if (r.restartTracker != null) {
            boolean stillTracking = false;
            int i = this.mRestartingServices.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                } else if (this.mRestartingServices.get(i).restartTracker != r.restartTracker) {
                    i--;
                } else {
                    stillTracking = true;
                    break;
                }
            }
            if (!stillTracking) {
                synchronized (this.mAm.mProcessStats.mLock) {
                    r.restartTracker.setRestarting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
                r.restartTracker = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setServiceRestartBackoffEnabledLocked(String packageName, boolean enable, String reason) {
        if (!enable) {
            if (this.mRestartBackoffDisabledPackages.contains(packageName)) {
                return;
            }
            this.mRestartBackoffDisabledPackages.add(packageName);
            long now = SystemClock.uptimeMillis();
            int size = this.mRestartingServices.size();
            for (int i = 0; i < size; i++) {
                ServiceRecord r = this.mRestartingServices.get(i);
                if (TextUtils.equals(r.packageName, packageName)) {
                    long remaining = r.nextRestartTime - now;
                    if (remaining > this.mAm.mConstants.SERVICE_RESTART_DURATION) {
                        r.restartDelay = this.mAm.mConstants.SERVICE_RESTART_DURATION;
                        r.nextRestartTime = r.restartDelay + now;
                        performScheduleRestartLocked(r, "Rescheduling", reason, now);
                    }
                }
                Collections.sort(this.mRestartingServices, new Comparator() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda0
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        return ActiveServices.lambda$setServiceRestartBackoffEnabledLocked$0((ServiceRecord) obj, (ServiceRecord) obj2);
                    }
                });
            }
            return;
        }
        removeServiceRestartBackoffEnabledLocked(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$setServiceRestartBackoffEnabledLocked$0(ServiceRecord a, ServiceRecord b) {
        return (int) (a.nextRestartTime - b.nextRestartTime);
    }

    private void removeServiceRestartBackoffEnabledLocked(String packageName) {
        this.mRestartBackoffDisabledPackages.remove(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isServiceRestartBackoffEnabledLocked(String packageName) {
        return !this.mRestartBackoffDisabledPackages.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired, boolean packageFrozen, boolean enqueueOomAdj) throws TransactionTooLargeException {
        return bringUpServiceLocked(r, intentFlags, execInFg, whileRestarting, permissionsReviewRequired, packageFrozen, enqueueOomAdj, null, -1);
    }

    private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired, boolean packageFrozen, boolean enqueueOomAdj, TranProcessWrapper callerProc, int callerType) throws TransactionTooLargeException {
        return bringUpServiceLocked(r, intentFlags, execInFg, whileRestarting, permissionsReviewRequired, packageFrozen, enqueueOomAdj, callerProc, callerType, null);
    }

    /* JADX WARN: Removed duplicated region for block: B:125:0x03f3  */
    /* JADX WARN: Removed duplicated region for block: B:131:0x0441  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x044a  */
    /* JADX WARN: Removed duplicated region for block: B:151:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired, boolean packageFrozen, boolean enqueueOomAdj, TranProcessWrapper callerProc, int callerType, Intent originalIntent) throws TransactionTooLargeException {
        String procName;
        String str;
        String str2;
        String str3;
        ProcessRecord app;
        HostingRecord hostingRecord;
        String suppressAction;
        ProcessRecord app2;
        if (r.app != null && r.app.getThread() != null) {
            sendServiceArgsLocked(r, execInFg, false);
            return null;
        } else if (whileRestarting || !this.mRestartingServices.contains(r)) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Bringing up " + r + " " + r.intent + " fg=" + r.fgRequired);
            }
            if (this.mRestartingServices.remove(r)) {
                clearRestartingIfNeededLocked(r);
            }
            if (r.delayed) {
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "REM FR DELAY LIST (bring up): " + r);
                }
                getServiceMapLocked(r.userId).mDelayedStartList.remove(r);
                r.delayed = false;
            }
            if (!this.mAm.mUserController.hasStartedUserState(r.userId)) {
                String msg = "Unable to launch app " + r.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + r.appInfo.uid + " for service " + r.intent.getIntent() + ": user " + r.userId + " is stopped";
                Slog.w(TAG, msg);
                bringDownServiceLocked(r, enqueueOomAdj);
                return msg;
            }
            if (!r.appInfo.packageName.equals(r.mRecentCallingPackage) && !r.isNotAppComponentUsage) {
                this.mAm.mUsageStatsService.reportEvent(r.packageName, r.userId, 31);
            }
            try {
                AppGlobals.getPackageManager().setPackageStoppedState(r.packageName, false, r.userId);
            } catch (RemoteException e) {
            } catch (IllegalArgumentException e2) {
                Slog.w(TAG, "Failed trying to unstop package " + r.packageName + ": " + e2);
            }
            boolean isolated = (r.serviceInfo.flags & 2) != 0;
            String procName2 = r.processName;
            HostingRecord hostingRecord2 = new HostingRecord(HostingRecord.HOSTING_TYPE_SERVICE, r.instanceName, r.definingPackageName, r.definingUid, r.serviceInfo.processName);
            if (!isolated) {
                ProcessRecord app3 = this.mAm.getProcessRecordLocked(procName2, r.appInfo.uid);
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slog.v(TAG_MU, "bringUpServiceLocked: appInfo.uid=" + r.appInfo.uid + " app=" + app3);
                }
                if (app3 == null) {
                    procName = procName2;
                    app2 = app3;
                    str = TAG;
                    str2 = "Unable to launch app ";
                    str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
                } else {
                    IApplicationThread thread = app3.getThread();
                    int pid = app3.getPid();
                    UidRecord uidRecord = app3.getUidRecord();
                    if (thread != null) {
                        try {
                            app3.addPackage(r.appInfo.packageName, r.appInfo.longVersionCode, this.mAm.mProcessStats);
                            procName = procName2;
                            app2 = app3;
                            str = TAG;
                            str2 = "Unable to launch app ";
                            str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
                        } catch (TransactionTooLargeException e3) {
                            throw e3;
                        } catch (RemoteException e4) {
                            e = e4;
                            procName = procName2;
                            app2 = app3;
                            str = TAG;
                            str2 = "Unable to launch app ";
                            str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
                        }
                        try {
                            realStartServiceLocked(r, app3, thread, pid, uidRecord, execInFg, enqueueOomAdj);
                            return null;
                        } catch (TransactionTooLargeException e5) {
                            throw e5;
                        } catch (RemoteException e6) {
                            e = e6;
                            Slog.w(str, "Exception when starting service " + r.shortInstanceName, e);
                            hostingRecord = hostingRecord2;
                            app = app2;
                            if (app != null) {
                            }
                            if (r.fgRequired) {
                            }
                            if (!this.mPendingServices.contains(r)) {
                            }
                            if (!r.delayedStop) {
                            }
                        }
                    } else {
                        procName = procName2;
                        app2 = app3;
                        str = TAG;
                        str2 = "Unable to launch app ";
                        str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
                    }
                }
                hostingRecord = hostingRecord2;
                app = app2;
            } else {
                procName = procName2;
                str = TAG;
                str2 = "Unable to launch app ";
                str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
                app = r.isolationHostProc;
                if (WebViewZygote.isMultiprocessEnabled() && r.serviceInfo.packageName.equals(WebViewZygote.getPackageName())) {
                    hostingRecord2 = HostingRecord.byWebviewZygote(r.instanceName, r.definingPackageName, r.definingUid, r.serviceInfo.processName);
                }
                if ((r.serviceInfo.flags & 8) == 0) {
                    hostingRecord = hostingRecord2;
                } else {
                    hostingRecord = HostingRecord.byAppZygote(r.instanceName, r.definingPackageName, r.definingUid, r.serviceInfo.processName);
                }
            }
            if (app != null && !permissionsReviewRequired && !packageFrozen) {
                boolean limited = ITranActiveServices.Instance().hookBringUpServiceLocked(callerType, callerProc, new HookActiveServiceInfo(r), originalIntent != null ? originalIntent : r.intent.getIntent());
                if (limited) {
                    String msg2 = str2 + r.appInfo.packageName + str3 + r.appInfo.uid + " for service " + (originalIntent != null ? originalIntent : r.intent.getIntent()) + ": AutoStart Limit";
                    Slog.w(str, msg2);
                    bringDownServiceLocked(r, enqueueOomAdj);
                    return msg2;
                }
                String str4 = str2;
                String str5 = str3;
                if (!"1".equals(SystemProperties.get("persist.vendor.duraspeed.support"))) {
                    suppressAction = "allowed";
                } else {
                    String action = null;
                    if (r.intent.getIntent() != null) {
                        action = r.intent.getIntent().getAction();
                    }
                    suppressAction = this.mAm.mAmsExt.onReadyToStartComponent(r.appInfo.packageName, r.appInfo.uid, HostingRecord.HOSTING_TYPE_SERVICE, action);
                }
                if (suppressAction != null && suppressAction.equals("skipped")) {
                    Slog.d(str, "bringUpServiceLocked, suppress to start service!");
                    try {
                        AppGlobals.getPackageManager().setPackageStoppedState(r.packageName, true, r.userId);
                    } catch (Exception e7) {
                        Slog.w(str, "Exception: " + e7);
                    }
                } else if (!r.isSdkSandbox) {
                    String procName3 = procName;
                    if (ITranActiveServices.Instance().canStartAppInLowStorage(this.mAm.mContext, procName3)) {
                        app = this.mAm.startProcessLocked(procName3, r.appInfo, true, intentFlags, hostingRecord, 0, false, isolated);
                    } else {
                        app = null;
                    }
                } else {
                    int uid = Process.toSdkSandboxUid(r.sdkSandboxClientAppUid);
                    ProcessRecord app4 = this.mAm.startSdkSandboxProcessLocked(procName, r.appInfo, true, intentFlags, hostingRecord, 0, uid, r.sdkSandboxClientAppPackage);
                    r.isolationHostProc = app4;
                    app = app4;
                }
                if (app == null) {
                    String msg3 = str4 + r.appInfo.packageName + str5 + r.appInfo.uid + " for service " + r.intent.getIntent() + ": process is bad";
                    Slog.w(str, msg3);
                    bringDownServiceLocked(r, enqueueOomAdj);
                    return msg3;
                } else if (isolated) {
                    r.isolationHostProc = app;
                }
            }
            if (r.fgRequired) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.v(str, "Allowlisting " + UserHandle.formatUid(r.appInfo.uid) + " for fg-service launch");
                }
                this.mAm.tempAllowlistUidLocked(r.appInfo.uid, this.mAm.mConstants.mServiceStartForegroundTimeoutMs, FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_SERVICE_LAUNCH, "fg-service-launch", 0, r.mRecentCallingUid);
            }
            if (!this.mPendingServices.contains(r)) {
                this.mPendingServices.add(r);
            }
            if (!r.delayedStop) {
                r.delayedStop = false;
                if (r.startRequested) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Applying delayed stop (in bring up): " + r);
                    }
                    stopServiceLocked(r, enqueueOomAdj);
                    return null;
                }
                return null;
            }
            return null;
        } else {
            return null;
        }
    }

    private final void requestServiceBindingsLocked(ServiceRecord r, boolean execInFg) throws TransactionTooLargeException {
        for (int i = r.bindings.size() - 1; i >= 0; i--) {
            IntentBindRecord ibr = r.bindings.valueAt(i);
            if (!requestServiceBindingLocked(r, ibr, execInFg, false)) {
                return;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:58:0x019e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void realStartServiceLocked(ServiceRecord r, ProcessRecord app, IApplicationThread thread, int pid, UidRecord uidRecord, boolean execInFg, boolean enqueueOomAdj) throws RemoteException {
        boolean z;
        boolean created;
        boolean z2;
        if (thread == null) {
            throw new RemoteException();
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "realStartServiceLocked, ServiceRecord.uid = " + r.appInfo.uid + ", ProcessRecord.uid = " + app.uid);
        }
        r.setProcess(app, thread, pid, uidRecord);
        long uptimeMillis = SystemClock.uptimeMillis();
        r.lastActivity = uptimeMillis;
        r.restartTime = uptimeMillis;
        ProcessServiceRecord psr = app.mServices;
        boolean newService = psr.startService(r);
        bumpServiceExecutingLocked(r, execInFg, "create", null);
        this.mAm.updateLruProcessLocked(app, false, null);
        updateServiceForegroundLocked(psr, false);
        this.mAm.enqueueOomAdjTargetLocked(app);
        this.mAm.updateOomAdjLocked(app, "updateOomAdj_startService");
        try {
            int uid = r.appInfo.uid;
            String packageName = r.name.getPackageName();
            String serviceName = r.name.getClassName();
            FrameworkStatsLog.write(100, uid, packageName, serviceName);
            this.mAm.mBatteryStatsService.noteServiceStartLaunch(uid, packageName, serviceName);
            this.mAm.notifyPackageUse(r.serviceInfo.packageName, 1);
            thread.scheduleCreateService(r, r.serviceInfo, this.mAm.compatibilityInfoForPackage(r.serviceInfo.applicationInfo), app.mState.getReportedProcState());
            r.postNotification();
            if (1 == 0) {
                boolean inDestroying = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying, false);
                if (newService) {
                    psr.stopService(r);
                    r.setProcess(null, null, 0, null);
                }
                if (!inDestroying) {
                    scheduleServiceRestartLocked(r, false);
                }
            }
            if (!r.allowlistManager) {
                z = true;
            } else {
                z = true;
                psr.mAllowlistManager = true;
            }
            requestServiceBindingsLocked(r, execInFg);
            updateServiceClientActivitiesLocked(psr, null, z);
            if (newService && 1 != 0) {
                psr.addBoundClientUidsOfNewService(r);
            }
            if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
                created = z;
                r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), null, null, 0));
            } else {
                created = z;
            }
            sendServiceArgsLocked(r, execInFg, created);
            if (!r.delayed) {
                z2 = false;
            } else {
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "REM FR DELAY LIST (new proc): " + r);
                }
                getServiceMapLocked(r.userId).mDelayedStartList.remove(r);
                z2 = false;
                r.delayed = false;
            }
            if (r.delayedStop) {
                r.delayedStop = z2;
                if (r.startRequested) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Applying delayed stop (from start): " + r);
                    }
                    stopServiceLocked(r, enqueueOomAdj);
                }
            }
        } catch (DeadObjectException e) {
            try {
                Slog.w(TAG, "Application dead when creating service " + r);
                this.mAm.appDiedLocked(app, "Died when creating service");
                throw e;
            } catch (Throwable th) {
                e = th;
                if (0 == 0) {
                    boolean inDestroying2 = this.mDestroyingServices.contains(r);
                    serviceDoneExecutingLocked(r, inDestroying2, inDestroying2, false);
                    if (newService) {
                        psr.stopService(r);
                        r.setProcess(null, null, 0, null);
                    }
                    if (!inDestroying2) {
                        scheduleServiceRestartLocked(r, false);
                    }
                }
                throw e;
            }
        } catch (Throwable th2) {
            e = th2;
            if (0 == 0) {
            }
            throw e;
        }
    }

    private final void sendServiceArgsLocked(ServiceRecord r, boolean execInFg, boolean oomAdjusted) throws TransactionTooLargeException {
        int N = r.pendingStarts.size();
        if (N == 0) {
            return;
        }
        ArrayList<ServiceStartArgs> args = new ArrayList<>();
        while (r.pendingStarts.size() > 0) {
            ServiceRecord.StartItem si = r.pendingStarts.remove(0);
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Sending arguments to: " + r + " " + r.intent + " args=" + si.intent);
            }
            if (si.intent != null || N <= 1) {
                si.deliveredTime = SystemClock.uptimeMillis();
                r.deliveredStarts.add(si);
                si.deliveryCount++;
                if (si.neededGrants != null) {
                    this.mAm.mUgmInternal.grantUriPermissionUncheckedFromIntent(si.neededGrants, si.getUriPermissionsLocked());
                }
                this.mAm.grantImplicitAccess(r.userId, si.intent, si.callingId, UserHandle.getAppId(r.appInfo.uid));
                bumpServiceExecutingLocked(r, execInFg, "start", null);
                if (r.fgRequired && !r.fgWaiting) {
                    if (!r.isForeground) {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.i(TAG, "Launched service must call startForeground() within timeout: " + r);
                        }
                        scheduleServiceForegroundTransitionTimeoutLocked(r);
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.i(TAG, "Service already foreground; no new timeout: " + r);
                        }
                        r.fgRequired = false;
                    }
                }
                int flags = 0;
                if (si.deliveryCount > 1) {
                    flags = 0 | 2;
                }
                if (si.doneExecutingCount > 0) {
                    flags |= 1;
                }
                args.add(new ServiceStartArgs(si.taskRemoved, si.id, flags, si.intent));
            }
        }
        if (!oomAdjusted) {
            this.mAm.enqueueOomAdjTargetLocked(r.app);
            this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
        }
        ParceledListSlice<ServiceStartArgs> slice = new ParceledListSlice<>(args);
        slice.setInlineCountLimit(4);
        Exception caughtException = null;
        try {
            r.app.getThread().scheduleServiceArgs(r, slice);
        } catch (TransactionTooLargeException e) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Transaction too large for " + args.size() + " args, first: " + args.get(0).args);
            }
            Slog.w(TAG, "Failed delivering service starts", e);
            caughtException = e;
        } catch (RemoteException e2) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Crashed while sending args: " + r);
            }
            Slog.w(TAG, "Failed delivering service starts", e2);
            caughtException = e2;
        } catch (Exception e3) {
            Slog.w(TAG, "Unexpected exception", e3);
            caughtException = e3;
        }
        if (caughtException != null) {
            boolean inDestroying = this.mDestroyingServices.contains(r);
            int size = args.size();
            for (int i = 0; i < size; i++) {
                serviceDoneExecutingLocked(r, inDestroying, inDestroying, true);
            }
            this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
            if (caughtException instanceof TransactionTooLargeException) {
                throw ((TransactionTooLargeException) caughtException);
            }
        }
    }

    private final boolean isServiceNeededLocked(ServiceRecord r, boolean knowConn, boolean hasConn) {
        if (r.startRequested) {
            return true;
        }
        if (!knowConn) {
            hasConn = r.hasAutoCreateConnections();
        }
        return hasConn;
    }

    private final void bringDownServiceIfNeededLocked(ServiceRecord r, boolean knowConn, boolean hasConn, boolean enqueueOomAdj) {
        if (isServiceNeededLocked(r, knowConn, hasConn) || this.mPendingServices.contains(r)) {
            return;
        }
        bringDownServiceLocked(r, enqueueOomAdj);
    }

    private void bringDownServiceLocked(ServiceRecord r, boolean enqueueOomAdj) {
        boolean oomAdjusted;
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
        for (int conni = connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> c = connections.valueAt(conni);
            for (int i = 0; i < c.size(); i++) {
                ConnectionRecord cr = c.get(i);
                cr.serviceDead = true;
                cr.stopAssociation();
                ComponentName componentName = cr.aliasComponent != null ? cr.aliasComponent : r.name;
                try {
                    cr.conn.connected(r.name, (IBinder) null, true);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure disconnecting service " + r.shortInstanceName + " to connection " + c.get(i).conn.asBinder() + " (in " + c.get(i).binding.client.processName + ")", e);
                }
            }
        }
        if (r.app != null && r.app.getThread() != null) {
            oomAdjusted = false;
            for (int i2 = r.bindings.size() - 1; i2 >= 0; i2--) {
                IntentBindRecord ibr = r.bindings.valueAt(i2);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Bringing down binding " + ibr + ": hasBound=" + ibr.hasBound);
                }
                if (ibr.hasBound) {
                    try {
                        oomAdjusted |= bumpServiceExecutingLocked(r, false, "bring down unbind", "updateOomAdj_unbindService");
                        ibr.hasBound = false;
                        ibr.requested = false;
                        r.app.getThread().scheduleUnbindService(r, ibr.intent.getIntent());
                    } catch (Exception e2) {
                        Slog.w(TAG, "Exception when unbinding service " + r.shortInstanceName, e2);
                        serviceProcessGoneLocked(r, enqueueOomAdj);
                        oomAdjusted = oomAdjusted;
                    }
                }
            }
        } else {
            oomAdjusted = false;
        }
        boolean oomAdjusted2 = r.fgRequired;
        if (oomAdjusted2) {
            Slog.w(TAG_SERVICE, "Bringing down service while still waiting for start foreground: " + r);
            r.fgRequired = false;
            r.fgWaiting = false;
            synchronized (this.mAm.mProcessStats.mLock) {
                ServiceState stracker = r.getTracker();
                if (stracker != null) {
                    stracker.setForeground(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
            }
            this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null);
            this.mAm.mHandler.removeMessages(66, r);
            if (r.app != null) {
                Message msg = this.mAm.mHandler.obtainMessage(69);
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = r.app;
                args.arg2 = r.toString();
                args.arg3 = r.getComponentName();
                msg.obj = args;
                this.mAm.mHandler.sendMessage(msg);
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            RuntimeException here = new RuntimeException();
            here.fillInStackTrace();
            Slog.v(TAG_SERVICE, "Bringing down " + r + " " + r.intent, here);
        }
        r.destroyTime = SystemClock.uptimeMillis();
        ServiceMap smap = getServiceMapLocked(r.userId);
        ServiceRecord found = smap.mServicesByInstanceName.remove(r.instanceName);
        if (found != null && found != r) {
            smap.mServicesByInstanceName.put(r.instanceName, found);
            throw new IllegalStateException("Bringing down " + r + " but actually running " + found);
        }
        smap.mServicesByIntent.remove(r.intent);
        r.totalRestartCount = 0;
        unscheduleServiceRestartLocked(r, 0, true);
        for (int i3 = this.mPendingServices.size() - 1; i3 >= 0; i3--) {
            if (this.mPendingServices.get(i3) == r) {
                this.mPendingServices.remove(i3);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Removed pending: " + r);
                }
            }
        }
        if (this.mPendingBringups.remove(r) != null && ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Removed pending bringup: " + r);
        }
        cancelForegroundNotificationLocked(r);
        boolean exitingFg = r.isForeground;
        if (exitingFg) {
            decActiveForegroundAppLocked(smap, r);
            synchronized (this.mAm.mProcessStats.mLock) {
                ServiceState stracker2 = r.getTracker();
                if (stracker2 != null) {
                    stracker2.setForeground(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
            }
            this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r.appInfo.uid, r.packageName, null);
            unregisterAppOpCallbackLocked(r);
            r.mFgsExitTime = SystemClock.uptimeMillis();
            logFGSStateChangeLocked(r, 2, r.mFgsExitTime > r.mFgsEnterTime ? (int) (r.mFgsExitTime - r.mFgsEnterTime) : 0, 2);
            this.mAm.updateForegroundServiceUsageStats(r.name, r.userId, false);
        }
        r.isForeground = false;
        r.mFgsNotificationWasDeferred = false;
        dropFgsNotificationStateLocked(r);
        r.foregroundId = 0;
        r.foregroundNoti = null;
        resetFgsRestrictionLocked(r);
        if (exitingFg) {
            signalForegroundServiceObserversLocked(r);
        }
        r.clearDeliveredStartsLocked();
        r.pendingStarts.clear();
        smap.mDelayedStartList.remove(r);
        if (r.app != null) {
            this.mAm.mBatteryStatsService.noteServiceStopLaunch(r.appInfo.uid, r.name.getPackageName(), r.name.getClassName());
            stopServiceAndUpdateAllowlistManagerLocked(r);
            if (r.app.getThread() != null) {
                this.mAm.updateLruProcessLocked(r.app, false, null);
                updateServiceForegroundLocked(r.app.mServices, false);
                try {
                    oomAdjusted |= bumpServiceExecutingLocked(r, false, "destroy", oomAdjusted ? null : "updateOomAdj_unbindService");
                    this.mDestroyingServices.add(r);
                    r.destroying = true;
                    r.app.getThread().scheduleStopService(r);
                } catch (Exception e3) {
                    Slog.w(TAG, "Exception when destroying service " + r.shortInstanceName, e3);
                    serviceProcessGoneLocked(r, enqueueOomAdj);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Removed service that has no process: " + r);
            }
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Removed service that is not running: " + r);
        }
        if (!oomAdjusted) {
            this.mAm.enqueueOomAdjTargetLocked(r.app);
            if (!enqueueOomAdj) {
                this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
            }
        }
        if (r.bindings.size() > 0) {
            r.bindings.clear();
        }
        if (r.restarter instanceof ServiceRestarter) {
            ((ServiceRestarter) r.restarter).setService(null);
        }
        synchronized (this.mAm.mProcessStats.mLock) {
            int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
            if (r.tracker != null) {
                long now = SystemClock.uptimeMillis();
                r.tracker.setStarted(false, memFactor, now);
                r.tracker.setBound(false, memFactor, now);
                if (r.executeNesting == 0) {
                    r.tracker.clearCurrentOwner(r, false);
                    r.tracker = null;
                }
            }
        }
        smap.ensureNotStartingBackgroundLocked(r);
    }

    private void dropFgsNotificationStateLocked(ServiceRecord r) {
        if (r.foregroundNoti == null) {
            return;
        }
        boolean shared = false;
        ServiceMap smap = this.mServiceMap.get(r.userId);
        if (smap != null) {
            int numServices = smap.mServicesByInstanceName.size();
            int i = 0;
            while (true) {
                if (i >= numServices) {
                    break;
                }
                ServiceRecord sr = smap.mServicesByInstanceName.valueAt(i);
                if (sr == r || !sr.isForeground || r.foregroundId != sr.foregroundId || !r.appInfo.packageName.equals(sr.appInfo.packageName)) {
                    i++;
                } else {
                    shared = true;
                    break;
                }
            }
        } else {
            Slog.wtf(TAG, "FGS " + r + " not found!");
        }
        if (!shared) {
            r.stripForegroundServiceFlagFromNotification();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeConnectionLocked(ConnectionRecord c, ProcessRecord skipApp, ActivityServiceConnectionsHolder skipAct, boolean enqueueOomAdj) {
        IBinder binder = c.conn.asBinder();
        AppBindRecord b = c.binding;
        ServiceRecord s = b.service;
        ArrayList<ConnectionRecord> clist = s.getConnections().get(binder);
        if (clist != null) {
            clist.remove(c);
            if (clist.size() == 0) {
                s.removeConnection(binder);
            }
        }
        b.connections.remove(c);
        c.stopAssociation();
        if (c.activity != null && c.activity != skipAct) {
            c.activity.removeConnection(c);
        }
        if (b.client != skipApp) {
            ProcessServiceRecord psr = b.client.mServices;
            psr.removeConnection(c);
            if ((c.flags & 8) != 0) {
                psr.updateHasAboveClientLocked();
            }
            if ((c.flags & 16777216) != 0) {
                s.updateAllowlistManager();
                if (!s.allowlistManager && s.app != null) {
                    updateAllowlistManagerLocked(s.app.mServices);
                }
            }
            if ((c.flags & 1048576) != 0) {
                s.updateIsAllowedBgActivityStartsByBinding();
            }
            if ((c.flags & 65536) != 0) {
                psr.updateHasTopStartedAlmostPerceptibleServices();
            }
            if (s.app != null) {
                updateServiceClientActivitiesLocked(s.app.mServices, c, true);
            }
        }
        ArrayList<ConnectionRecord> clist2 = this.mServiceConnections.get(binder);
        if (clist2 != null) {
            clist2.remove(c);
            if (clist2.size() == 0) {
                this.mServiceConnections.remove(binder);
            }
        }
        this.mAm.stopAssociationLocked(b.client.uid, b.client.processName, s.appInfo.uid, s.appInfo.longVersionCode, s.instanceName, s.processName);
        if (b.connections.size() == 0) {
            b.intent.apps.remove(b.client);
        }
        if (!c.serviceDead) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Disconnecting binding " + b.intent + ": shouldUnbind=" + b.intent.hasBound);
            }
            if (s.app != null && s.app.getThread() != null && b.intent.apps.size() == 0 && b.intent.hasBound) {
                try {
                    bumpServiceExecutingLocked(s, false, "unbind", "updateOomAdj_unbindService");
                    if (b.client != s.app && (c.flags & 32) == 0 && s.app.mState.getSetProcState() <= 13) {
                        this.mAm.updateLruProcessLocked(s.app, false, null);
                    }
                    b.intent.hasBound = false;
                    b.intent.doRebind = false;
                    s.app.getThread().scheduleUnbindService(s, b.intent.intent.getIntent());
                } catch (Exception e) {
                    Slog.w(TAG, "Exception when unbinding service " + s.shortInstanceName, e);
                    serviceProcessGoneLocked(s, enqueueOomAdj);
                }
            }
            if (s.getConnections().isEmpty()) {
                this.mPendingServices.remove(s);
                this.mPendingBringups.remove(s);
            }
            if ((c.flags & 1) != 0) {
                boolean hasAutoCreate = s.hasAutoCreateConnections();
                if (!hasAutoCreate && s.tracker != null) {
                    synchronized (this.mAm.mProcessStats.mLock) {
                        s.tracker.setBound(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                }
                bringDownServiceIfNeededLocked(s, true, hasAutoCreate, enqueueOomAdj);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceDoneExecutingLocked(ServiceRecord r, int type, int startId, int res, boolean enqueueOomAdj) {
        boolean inDestroying = this.mDestroyingServices.contains(r);
        if (r != null) {
            if (type == 1) {
                r.callStart = true;
                switch (res) {
                    case 0:
                    case 1:
                        r.findDeliveredStart(startId, false, true);
                        r.stopIfKilled = false;
                        break;
                    case 2:
                        r.findDeliveredStart(startId, false, true);
                        if (r.getLastStartId() == startId) {
                            r.stopIfKilled = true;
                            break;
                        }
                        break;
                    case 3:
                        ServiceRecord.StartItem si = r.findDeliveredStart(startId, false, false);
                        if (si != null) {
                            si.deliveryCount = 0;
                            si.doneExecutingCount++;
                            r.stopIfKilled = true;
                            break;
                        }
                        break;
                    case 1000:
                        r.findDeliveredStart(startId, true, true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown service start result: " + res);
                }
                if (res == 0) {
                    r.callStart = false;
                }
            } else if (type == 2) {
                if (inDestroying) {
                    if (r.executeNesting != 1) {
                        Slog.w(TAG, "Service done with onDestroy, but executeNesting=" + r.executeNesting + ": " + r);
                        r.executeNesting = 1;
                    }
                } else if (r.app != null) {
                    Slog.w(TAG, "Service done with onDestroy, but not inDestroying: " + r + ", app=" + r.app);
                }
            }
            long origId = Binder.clearCallingIdentity();
            serviceDoneExecutingLocked(r, inDestroying, inDestroying, enqueueOomAdj);
            Binder.restoreCallingIdentity(origId);
            return;
        }
        Slog.w(TAG, "Done executing unknown service from pid " + Binder.getCallingPid());
    }

    private void serviceProcessGoneLocked(ServiceRecord r, boolean enqueueOomAdj) {
        if (r.tracker != null) {
            synchronized (this.mAm.mProcessStats.mLock) {
                int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
                long now = SystemClock.uptimeMillis();
                r.tracker.setExecuting(false, memFactor, now);
                r.tracker.setForeground(false, memFactor, now);
                r.tracker.setBound(false, memFactor, now);
                r.tracker.setStarted(false, memFactor, now);
            }
        }
        serviceDoneExecutingLocked(r, true, true, enqueueOomAdj);
    }

    private void serviceDoneExecutingLocked(ServiceRecord r, boolean inDestroying, boolean finishing, boolean enqueueOomAdj) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "<<< DONE EXECUTING " + r + ": nesting=" + r.executeNesting + ", inDestroying=" + inDestroying + ", app=" + r.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, "<<< DONE EXECUTING " + r.shortInstanceName);
        }
        r.executeNesting--;
        if (r.executeNesting <= 0) {
            if (r.app != null) {
                ProcessServiceRecord psr = r.app.mServices;
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Nesting at 0 of " + r.shortInstanceName);
                }
                psr.setExecServicesFg(false);
                psr.stopExecutingService(r);
                if (psr.numberOfExecutingServices() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
                        Slog.v(TAG_SERVICE_EXECUTING, "No more executingServices of " + r.shortInstanceName);
                    }
                    this.mAm.mHandler.removeMessages(12, r.app);
                } else if (r.executeFg) {
                    int i = psr.numberOfExecutingServices() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        } else if (!psr.getExecutingServiceAt(i).executeFg) {
                            i--;
                        } else {
                            psr.setExecServicesFg(true);
                            break;
                        }
                    }
                }
                if (inDestroying) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.v(TAG_SERVICE, "doneExecuting remove destroying " + r);
                    }
                    this.mDestroyingServices.remove(r);
                    r.bindings.clear();
                }
                if (enqueueOomAdj) {
                    this.mAm.enqueueOomAdjTargetLocked(r.app);
                } else {
                    this.mAm.updateOomAdjLocked(r.app, "updateOomAdj_unbindService");
                }
            }
            r.executeFg = false;
            if (r.tracker != null) {
                synchronized (this.mAm.mProcessStats.mLock) {
                    int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
                    long now = SystemClock.uptimeMillis();
                    r.tracker.setExecuting(false, memFactor, now);
                    r.tracker.setForeground(false, memFactor, now);
                    if (finishing) {
                        r.tracker.clearCurrentOwner(r, false);
                        r.tracker = null;
                    }
                }
            }
            if (finishing) {
                if (r.app != null && !r.app.isPersistent()) {
                    stopServiceAndUpdateAllowlistManagerLocked(r);
                }
                r.setProcess(null, null, 0, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean attachApplicationLocked(ProcessRecord proc, String processName) throws RemoteException {
        boolean didSomething = false;
        proc.mState.setBackgroundRestricted(appRestrictedAnyInBackground(proc.uid, proc.info.packageName));
        if (this.mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            int i = 0;
            while (i < this.mPendingServices.size()) {
                try {
                    sr = this.mPendingServices.get(i);
                    if (proc == sr.isolationHostProc || (proc.uid == sr.appInfo.uid && processName.equals(sr.processName))) {
                        IApplicationThread thread = proc.getThread();
                        int pid = proc.getPid();
                        UidRecord uidRecord = proc.getUidRecord();
                        this.mPendingServices.remove(i);
                        i--;
                        proc.addPackage(sr.appInfo.packageName, sr.appInfo.longVersionCode, this.mAm.mProcessStats);
                        realStartServiceLocked(sr, proc, thread, pid, uidRecord, sr.createdFromFg, true);
                        didSomething = true;
                        if (!isServiceNeededLocked(sr, false, false)) {
                            bringDownServiceLocked(sr, true);
                        }
                        this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_startService");
                    }
                    i++;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception in new application when starting service " + sr.shortInstanceName, e);
                    throw e;
                }
            }
        }
        if (this.mRestartingServices.size() > 0) {
            boolean didImmediateRestart = false;
            for (int i2 = 0; i2 < this.mRestartingServices.size(); i2++) {
                ServiceRecord sr2 = this.mRestartingServices.get(i2);
                if (proc == sr2.isolationHostProc || (proc.uid == sr2.appInfo.uid && processName.equals(sr2.processName))) {
                    this.mAm.mHandler.removeCallbacks(sr2.restarter);
                    this.mAm.mHandler.post(sr2.restarter);
                    didImmediateRestart = true;
                }
            }
            if (didImmediateRestart) {
                this.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActiveServices.this.m971x26a645a4();
                    }
                });
            }
        }
        return didSomething;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$attachApplicationLocked$1$com-android-server-am-ActiveServices  reason: not valid java name */
    public /* synthetic */ void m971x26a645a4() {
        long now = SystemClock.uptimeMillis();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                rescheduleServiceRestartIfPossibleLocked(getExtraRestartTimeInBetweenLocked(), this.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN, "other", now);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processStartTimedOutLocked(ProcessRecord proc) {
        ArrayList<ServiceRecord> restartSystemUIServices = new ArrayList<>();
        boolean needOomAdj = false;
        int i = 0;
        int size = this.mPendingServices.size();
        while (i < size) {
            ServiceRecord sr = this.mPendingServices.get(i);
            if ((proc.uid == sr.appInfo.uid && proc.processName.equals(sr.processName)) || sr.isolationHostProc == proc) {
                if (proc.isPersistent() && "com.android.systemui".equals(this.mPendingServices.get(i).processName)) {
                    restartSystemUIServices.add(sr);
                } else {
                    Slog.w(TAG, "Forcing bringing down service: " + sr);
                    sr.isolationHostProc = null;
                    this.mPendingServices.remove(i);
                    size = this.mPendingServices.size();
                    i--;
                    needOomAdj = true;
                    bringDownServiceLocked(sr, true);
                }
            }
            i++;
        }
        for (int i2 = 0; i2 < restartSystemUIServices.size(); i2++) {
            ServiceRecord systemuiSR = restartSystemUIServices.get(i2);
            if (systemuiSR.restartCount < this.mAm.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY) {
                scheduleServiceRestartLocked(systemuiSR, true);
                SystemProperties.set("persist.sys.systemui.restart", String.valueOf(systemuiSR.restartTime));
                Slog.i(TAG, "processStartTimedOutLocked restart again  " + systemuiSR + " count=" + systemuiSR.restartCount);
            }
        }
        if (needOomAdj) {
            this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
        }
    }

    private boolean collectPackageServicesLocked(String packageName, Set<String> filterByClasses, boolean evenPersistent, boolean doit, ArrayMap<ComponentName, ServiceRecord> services) {
        boolean didSomething = false;
        for (int i = services.size() - 1; i >= 0; i--) {
            ServiceRecord service = services.valueAt(i);
            boolean sameComponent = packageName == null || (service.packageName.equals(packageName) && (filterByClasses == null || filterByClasses.contains(service.name.getClassName())));
            if (sameComponent && (service.app == null || evenPersistent || !service.app.isPersistent())) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                Slog.i(TAG, "  Force stopping service " + service);
                if (service.app != null && !service.app.isPersistent()) {
                    stopServiceAndUpdateAllowlistManagerLocked(service);
                }
                service.setProcess(null, null, 0, null);
                service.isolationHostProc = null;
                if (this.mTmpCollectionResults == null) {
                    this.mTmpCollectionResults = new ArrayList<>();
                }
                this.mTmpCollectionResults.add(service);
            }
        }
        return didSomething;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean bringDownDisabledPackageServicesLocked(String packageName, Set<String> filterByClasses, int userId, boolean evenPersistent, boolean fullStop, boolean doit) {
        boolean didSomething;
        boolean didSomething2 = false;
        ArrayList<ServiceRecord> arrayList = this.mTmpCollectionResults;
        if (arrayList != null) {
            arrayList.clear();
        }
        if (userId == -1) {
            didSomething = false;
            for (int i = this.mServiceMap.size() - 1; i >= 0; i--) {
                didSomething |= collectPackageServicesLocked(packageName, filterByClasses, evenPersistent, doit, this.mServiceMap.valueAt(i).mServicesByInstanceName);
                if (!doit && didSomething) {
                    return true;
                }
                if (doit && filterByClasses == null) {
                    forceStopPackageLocked(packageName, this.mServiceMap.valueAt(i).mUserId);
                }
            }
        } else {
            ServiceMap smap = this.mServiceMap.get(userId);
            if (smap != null) {
                ArrayMap<ComponentName, ServiceRecord> items = smap.mServicesByInstanceName;
                didSomething2 = collectPackageServicesLocked(packageName, filterByClasses, evenPersistent, doit, items);
            }
            if (doit && filterByClasses == null) {
                forceStopPackageLocked(packageName, userId);
            }
            didSomething = didSomething2;
        }
        ArrayList<ServiceRecord> arrayList2 = this.mTmpCollectionResults;
        if (arrayList2 != null) {
            int size = arrayList2.size();
            for (int i2 = size - 1; i2 >= 0; i2--) {
                bringDownServiceLocked(this.mTmpCollectionResults.get(i2), true);
            }
            if (size > 0) {
                this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
            }
            if (fullStop && !this.mTmpCollectionResults.isEmpty()) {
                final ArrayList<ServiceRecord> allServices = (ArrayList) this.mTmpCollectionResults.clone();
                this.mAm.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActiveServices.lambda$bringDownDisabledPackageServicesLocked$2(allServices);
                    }
                }, 250L);
            }
            ArrayList<ServiceRecord> allServices2 = this.mTmpCollectionResults;
            allServices2.clear();
        }
        return didSomething;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$bringDownDisabledPackageServicesLocked$2(ArrayList allServices) {
        for (int i = 0; i < allServices.size(); i++) {
            ((ServiceRecord) allServices.get(i)).cancelNotification();
        }
    }

    private void signalForegroundServiceObserversLocked(ServiceRecord r) {
        int num = this.mFgsObservers.beginBroadcast();
        for (int i = 0; i < num; i++) {
            try {
                this.mFgsObservers.getBroadcastItem(i).onForegroundStateChanged(r, r.appInfo.packageName, r.userId, r.isForeground);
            } catch (RemoteException e) {
            }
        }
        this.mFgsObservers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean registerForegroundServiceObserverLocked(int callingUid, IForegroundServiceObserver callback) {
        try {
            int mapSize = this.mServiceMap.size();
            for (int mapIndex = 0; mapIndex < mapSize; mapIndex++) {
                ServiceMap smap = this.mServiceMap.valueAt(mapIndex);
                if (smap != null) {
                    int numServices = smap.mServicesByInstanceName.size();
                    for (int i = 0; i < numServices; i++) {
                        ServiceRecord sr = smap.mServicesByInstanceName.valueAt(i);
                        if (sr.isForeground && callingUid == sr.appInfo.uid) {
                            callback.onForegroundStateChanged(sr, sr.appInfo.packageName, sr.userId, true);
                        }
                    }
                }
            }
            this.mFgsObservers.register(callback);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG_SERVICE, "Bad FGS observer from uid " + callingUid);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceStopPackageLocked(String packageName, int userId) {
        ServiceMap smap = this.mServiceMap.get(userId);
        if (smap != null && smap.mActiveForegroundApps.size() > 0) {
            for (int i = smap.mActiveForegroundApps.size() - 1; i >= 0; i--) {
                ActiveForegroundApp aa = smap.mActiveForegroundApps.valueAt(i);
                if (aa.mPackageName.equals(packageName)) {
                    smap.mActiveForegroundApps.removeAt(i);
                    smap.mActiveForegroundAppsChanged = true;
                }
            }
            if (smap.mActiveForegroundAppsChanged) {
                requestUpdateActiveForegroundAppsLocked(smap, 0L);
            }
        }
        for (int i2 = this.mPendingBringups.size() - 1; i2 >= 0; i2--) {
            ServiceRecord r = this.mPendingBringups.keyAt(i2);
            if (TextUtils.equals(r.packageName, packageName) && r.userId == userId) {
                this.mPendingBringups.removeAt(i2);
            }
        }
        removeServiceRestartBackoffEnabledLocked(packageName);
        removeServiceNotificationDeferralsLocked(packageName, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUpServices(int userId, ComponentName component, Intent baseIntent) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        ArrayMap<ComponentName, ServiceRecord> alls = getServicesLocked(userId);
        for (int i = alls.size() - 1; i >= 0; i--) {
            ServiceRecord sr = alls.valueAt(i);
            if (sr.packageName.equals(component.getPackageName())) {
                services.add(sr);
            }
        }
        boolean needOomAdj = false;
        for (int i2 = services.size() - 1; i2 >= 0; i2--) {
            ServiceRecord sr2 = services.get(i2);
            if (sr2.startRequested) {
                if ((sr2.serviceInfo.flags & 1) != 0) {
                    Slog.i(TAG, "Stopping service " + sr2.shortInstanceName + ": remove task");
                    stopServiceLocked(sr2, true);
                    needOomAdj = true;
                } else {
                    sr2.pendingStarts.add(new ServiceRecord.StartItem(sr2, true, sr2.getLastStartId(), baseIntent, null, 0));
                    if (sr2.app != null && sr2.app.getThread() != null) {
                        try {
                            sendServiceArgsLocked(sr2, true, false);
                        } catch (TransactionTooLargeException e) {
                        }
                    }
                }
            }
        }
        if (needOomAdj) {
            this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void killServicesLocked(ProcessRecord app, boolean allowRestart) {
        IApplicationThread iApplicationThread;
        ProcessServiceRecord psr = app.mServices;
        int i = psr.numberOfConnections() - 1;
        while (true) {
            iApplicationThread = null;
            if (i < 0) {
                break;
            }
            removeConnectionLocked(psr.getConnectionAt(i), app, null, true);
            i--;
        }
        updateServiceConnectionActivitiesLocked(psr);
        psr.removeAllConnections();
        boolean z = false;
        psr.mAllowlistManager = false;
        int i2 = psr.numberOfRunningServices() - 1;
        while (i2 >= 0) {
            ServiceRecord sr = psr.getRunningServiceAt(i2);
            this.mAm.mBatteryStatsService.noteServiceStopLaunch(sr.appInfo.uid, sr.name.getPackageName(), sr.name.getClassName());
            if (sr.app != app && sr.app != null && !sr.app.isPersistent()) {
                sr.app.mServices.stopService(sr);
                sr.app.mServices.updateBoundClientUids();
            }
            sr.setProcess(iApplicationThread, iApplicationThread, z ? 1 : 0, iApplicationThread);
            sr.isolationHostProc = iApplicationThread;
            sr.executeNesting = z ? 1 : 0;
            synchronized (this.mAm.mProcessStats.mLock) {
                sr.forceClearTracker();
            }
            if (this.mDestroyingServices.remove(sr) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "killServices remove destroying " + sr);
            }
            int numClients = sr.bindings.size();
            int bindingi = numClients - 1;
            IApplicationThread iApplicationThread2 = iApplicationThread;
            while (bindingi >= 0) {
                IntentBindRecord b = sr.bindings.valueAt(bindingi);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Killing binding " + b + ": shouldUnbind=" + b.hasBound);
                }
                b.binder = iApplicationThread2;
                b.hasBound = z;
                b.received = z;
                b.requested = z;
                for (int appi = b.apps.size() - 1; appi >= 0; appi--) {
                    ProcessRecord proc = b.apps.keyAt(appi);
                    if (!proc.isKilledByAm() && proc.getThread() != null) {
                        AppBindRecord abind = b.apps.valueAt(appi);
                        for (int conni = abind.connections.size() - 1; conni >= 0; conni--) {
                            ConnectionRecord conn = abind.connections.valueAt(conni);
                            if ((conn.flags & 49) == 1) {
                                break;
                            }
                        }
                    }
                }
                bindingi--;
                z = false;
                iApplicationThread2 = null;
            }
            i2--;
            z = false;
            iApplicationThread = null;
        }
        ServiceMap smap = getServiceMapLocked(app.userId);
        for (int i3 = psr.numberOfRunningServices() - 1; i3 >= 0; i3--) {
            ServiceRecord sr2 = psr.getRunningServiceAt(i3);
            if (!app.isPersistent()) {
                psr.stopService(sr2);
                psr.updateBoundClientUids();
            }
            ServiceRecord curRec = smap.mServicesByInstanceName.get(sr2.instanceName);
            if (curRec != sr2) {
                if (curRec != null) {
                    Slog.wtf(TAG, "Service " + sr2 + " in process " + app + " not same as in map: " + curRec);
                }
            } else if (allowRestart && sr2.crashCount >= this.mAm.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (sr2.serviceInfo.applicationInfo.flags & 8) == 0) {
                Slog.w(TAG, "Service crashed " + sr2.crashCount + " times, stopping: " + sr2);
                Object[] objArr = new Object[4];
                objArr[0] = Integer.valueOf(sr2.userId);
                objArr[1] = Integer.valueOf(sr2.crashCount);
                objArr[2] = sr2.shortInstanceName;
                objArr[3] = Integer.valueOf(sr2.app != null ? sr2.app.getPid() : -1);
                EventLog.writeEvent((int) EventLogTags.AM_SERVICE_CRASHED_TOO_MUCH, objArr);
                bringDownServiceLocked(sr2, true);
            } else {
                if (allowRestart && this.mAm.mUserController.isUserRunning(sr2.userId, 0)) {
                    boolean scheduled = scheduleServiceRestartLocked(sr2, true);
                    if (!scheduled) {
                        bringDownServiceLocked(sr2, true);
                    } else if (sr2.canStopIfKilled(false)) {
                        sr2.startRequested = false;
                        if (sr2.tracker != null) {
                            synchronized (this.mAm.mProcessStats.mLock) {
                                sr2.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                            }
                        }
                    }
                }
                bringDownServiceLocked(sr2, true);
            }
        }
        this.mAm.updateOomAdjPendingTargetsLocked("updateOomAdj_unbindService");
        if (!allowRestart) {
            psr.stopAllServices();
            psr.clearBoundClientUids();
            for (int i4 = this.mRestartingServices.size() - 1; i4 >= 0; i4--) {
                ServiceRecord r = this.mRestartingServices.get(i4);
                if (r.processName.equals(app.processName) && r.serviceInfo.applicationInfo.uid == app.info.uid) {
                    this.mRestartingServices.remove(i4);
                    clearRestartingIfNeededLocked(r);
                }
            }
            for (int i5 = this.mPendingServices.size() - 1; i5 >= 0; i5--) {
                ServiceRecord r2 = this.mPendingServices.get(i5);
                if (r2.processName.equals(app.processName) && r2.serviceInfo.applicationInfo.uid == app.info.uid) {
                    this.mPendingServices.remove(i5);
                }
            }
            for (int i6 = this.mPendingBringups.size() - 1; i6 >= 0; i6--) {
                ServiceRecord r3 = this.mPendingBringups.keyAt(i6);
                if (r3.processName.equals(app.processName) && r3.serviceInfo.applicationInfo.uid == app.info.uid) {
                    this.mPendingBringups.removeAt(i6);
                }
            }
        }
        int i7 = this.mDestroyingServices.size();
        while (i7 > 0) {
            int i8 = i7 - 1;
            ServiceRecord sr3 = this.mDestroyingServices.get(i8);
            if (sr3.app == app) {
                synchronized (this.mAm.mProcessStats.mLock) {
                    sr3.forceClearTracker();
                }
                this.mDestroyingServices.remove(i8);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "killServices remove destroying " + sr3);
                }
            }
            i7 = i8;
        }
        psr.stopAllExecutingServices();
    }

    ActivityManager.RunningServiceInfo makeRunningServiceInfoLocked(ServiceRecord r) {
        ActivityManager.RunningServiceInfo info = new ActivityManager.RunningServiceInfo();
        info.service = r.name;
        if (r.app != null) {
            info.pid = r.app.getPid();
        }
        info.uid = r.appInfo.uid;
        info.process = r.processName;
        info.foreground = r.isForeground;
        info.activeSince = r.createRealTime;
        info.started = r.startRequested;
        info.clientCount = r.getConnections().size();
        info.crashCount = r.crashCount;
        info.lastActivityTime = r.lastActivity;
        if (r.isForeground) {
            info.flags |= 2;
        }
        if (r.startRequested) {
            info.flags |= 1;
        }
        if (r.app != null && r.app.getPid() == ActivityManagerService.MY_PID) {
            info.flags |= 4;
        }
        if (r.app != null && r.app.isPersistent()) {
            info.flags |= 8;
        }
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
        for (int conni = connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> connl = connections.valueAt(conni);
            for (int i = 0; i < connl.size(); i++) {
                ConnectionRecord conn = connl.get(i);
                if (conn.clientLabel != 0) {
                    info.clientPackage = conn.binding.client.info.packageName;
                    info.clientLabel = conn.clientLabel;
                    return info;
                }
            }
        }
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ActivityManager.RunningServiceInfo> getRunningServiceInfoLocked(int maxNum, int flags, int callingUid, boolean allowed, boolean canInteractAcrossUsers) {
        ArrayList<ActivityManager.RunningServiceInfo> res = new ArrayList<>();
        long ident = Binder.clearCallingIdentity();
        try {
            if (canInteractAcrossUsers) {
                int[] users = this.mAm.mUserController.getUsers();
                for (int ui = 0; ui < users.length && res.size() < maxNum; ui++) {
                    ArrayMap<ComponentName, ServiceRecord> alls = getServicesLocked(users[ui]);
                    for (int i = 0; i < alls.size() && res.size() < maxNum; i++) {
                        res.add(makeRunningServiceInfoLocked(alls.valueAt(i)));
                    }
                }
                for (int i2 = 0; i2 < this.mRestartingServices.size() && res.size() < maxNum; i2++) {
                    ServiceRecord r = this.mRestartingServices.get(i2);
                    ActivityManager.RunningServiceInfo info = makeRunningServiceInfoLocked(r);
                    info.restarting = r.nextRestartTime;
                    res.add(info);
                }
            } else {
                int userId = UserHandle.getUserId(callingUid);
                ArrayMap<ComponentName, ServiceRecord> alls2 = getServicesLocked(userId);
                for (int i3 = 0; i3 < alls2.size() && res.size() < maxNum; i3++) {
                    ServiceRecord sr = alls2.valueAt(i3);
                    if (allowed || (sr.app != null && sr.app.uid == callingUid)) {
                        res.add(makeRunningServiceInfoLocked(sr));
                    }
                }
                for (int i4 = 0; i4 < this.mRestartingServices.size() && res.size() < maxNum; i4++) {
                    ServiceRecord r2 = this.mRestartingServices.get(i4);
                    if (r2.userId == userId && (allowed || (r2.app != null && r2.app.uid == callingUid))) {
                        ActivityManager.RunningServiceInfo info2 = makeRunningServiceInfoLocked(r2);
                        info2.restarting = r2.nextRestartTime;
                        res.add(info2);
                    }
                }
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public PendingIntent getRunningServiceControlPanelLocked(ComponentName name) {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        ServiceRecord r = getServiceByNameLocked(name, userId);
        if (r != null) {
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
            for (int conni = connections.size() - 1; conni >= 0; conni--) {
                ArrayList<ConnectionRecord> conn = connections.valueAt(conni);
                for (int i = 0; i < conn.size(); i++) {
                    if (conn.get(i).clientIntent != null) {
                        return conn.get(i).clientIntent;
                    }
                }
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceTimeout(ProcessRecord proc) {
        String anrMessage = null;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (proc.isDebugging()) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ProcessServiceRecord psr = proc.mServices;
                if (psr.numberOfExecutingServices() != 0 && proc.getThread() != null) {
                    long now = SystemClock.uptimeMillis();
                    long maxTime = now - (psr.shouldExecServicesFg() ? SERVICE_TIMEOUT : SERVICE_BACKGROUND_TIMEOUT);
                    ServiceRecord timeout = null;
                    long nextTime = 0;
                    int i = psr.numberOfExecutingServices() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        ServiceRecord sr = psr.getExecutingServiceAt(i);
                        if (sr.executingStart < maxTime) {
                            timeout = sr;
                            break;
                        }
                        if (sr.executingStart > nextTime) {
                            nextTime = sr.executingStart;
                        }
                        i--;
                    }
                    if (timeout != null && this.mAm.mProcessList.isInLruListLOSP(proc)) {
                        Slog.w(TAG, "Timeout executing service: " + timeout);
                        StringWriter sw = new StringWriter();
                        FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 1024);
                        fastPrintWriter.println(timeout);
                        timeout.dump((PrintWriter) fastPrintWriter, "    ");
                        fastPrintWriter.close();
                        this.mLastAnrDump = sw.toString();
                        this.mAm.mHandler.removeCallbacks(this.mLastAnrDumpClearer);
                        this.mAm.mHandler.postDelayed(this.mLastAnrDumpClearer, AppStandbyController.ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                        anrMessage = "executing service " + timeout.shortInstanceName;
                    } else {
                        Message msg = this.mAm.mHandler.obtainMessage(12);
                        msg.obj = proc;
                        this.mAm.mHandler.sendMessageAtTime(msg, psr.shouldExecServicesFg() ? SERVICE_TIMEOUT + nextTime : SERVICE_BACKGROUND_TIMEOUT + nextTime);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (anrMessage != null) {
                        this.mAm.mAnrHelper.appNotResponding(proc, anrMessage);
                        return;
                    }
                    return;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceForegroundTimeout(ServiceRecord r) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (r.fgRequired && r.fgWaiting && !r.destroying) {
                    ProcessRecord app = r.app;
                    if (app != null && app.isDebugging()) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                        Slog.i(TAG, "Service foreground-required timeout for " + r);
                    }
                    r.fgWaiting = false;
                    stopServiceLocked(r, false);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (app != null) {
                        String annotation = "Context.startForegroundService() did not then call Service.startForeground(): " + r;
                        Message msg = this.mAm.mHandler.obtainMessage(67);
                        SomeArgs args = SomeArgs.obtain();
                        args.arg1 = app;
                        args.arg2 = annotation;
                        msg.obj = args;
                        this.mAm.mHandler.sendMessageDelayed(msg, this.mAm.mConstants.mServiceStartForegroundAnrDelayMs);
                        return;
                    }
                    return;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceForegroundTimeoutANR(ProcessRecord app, String annotation) {
        this.mAm.mAnrHelper.appNotResponding(app, annotation);
    }

    public void updateServiceApplicationInfoLocked(ApplicationInfo applicationInfo) {
        int userId = UserHandle.getUserId(applicationInfo.uid);
        ServiceMap serviceMap = this.mServiceMap.get(userId);
        if (serviceMap != null) {
            ArrayMap<ComponentName, ServiceRecord> servicesByName = serviceMap.mServicesByInstanceName;
            for (int j = servicesByName.size() - 1; j >= 0; j--) {
                ServiceRecord serviceRecord = servicesByName.valueAt(j);
                if (applicationInfo.packageName.equals(serviceRecord.appInfo.packageName)) {
                    serviceRecord.appInfo = applicationInfo;
                    serviceRecord.serviceInfo.applicationInfo = applicationInfo;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceForegroundCrash(ProcessRecord app, String serviceRecord, ComponentName service) {
        this.mAm.crashApplicationWithTypeWithExtras(app.uid, app.getPid(), app.info.packageName, app.userId, "Context.startForegroundService() did not then call Service.startForeground(): " + serviceRecord, false, 1, RemoteServiceException.ForegroundServiceDidNotStartInTimeException.createExtrasForService(service));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleServiceTimeoutLocked(ProcessRecord proc) {
        if (proc.mServices.numberOfExecutingServices() == 0 || proc.getThread() == null) {
            return;
        }
        Message msg = this.mAm.mHandler.obtainMessage(12);
        msg.obj = proc;
        this.mAm.mHandler.sendMessageDelayed(msg, proc.mServices.shouldExecServicesFg() ? SERVICE_TIMEOUT : SERVICE_BACKGROUND_TIMEOUT);
    }

    void scheduleServiceForegroundTransitionTimeoutLocked(ServiceRecord r) {
        if (r.app.mServices.numberOfExecutingServices() == 0 || r.app.getThread() == null) {
            return;
        }
        Message msg = this.mAm.mHandler.obtainMessage(66);
        msg.obj = r;
        r.fgWaiting = true;
        this.mAm.mHandler.sendMessageDelayed(msg, this.mAm.mConstants.mServiceStartForegroundTimeoutMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ServiceDumper {
        private final String[] args;
        private final boolean dumpAll;
        private final String dumpPackage;
        private final FileDescriptor fd;
        private final ActivityManagerService.ItemMatcher matcher;
        private final PrintWriter pw;
        final /* synthetic */ ActiveServices this$0;
        private final ArrayList<ServiceRecord> services = new ArrayList<>();
        private final long nowReal = SystemClock.elapsedRealtime();
        private boolean needSep = false;
        private boolean printedAnything = false;
        private boolean printed = false;

        ServiceDumper(ActiveServices this$0, FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
            ActiveServices activeServices = this$0;
            this.this$0 = activeServices;
            int i = 0;
            this.fd = fd;
            this.pw = pw;
            this.args = args;
            this.dumpAll = dumpAll;
            this.dumpPackage = dumpPackage;
            ActivityManagerService.ItemMatcher itemMatcher = new ActivityManagerService.ItemMatcher();
            this.matcher = itemMatcher;
            itemMatcher.build(args, opti);
            int[] users = activeServices.mAm.mUserController.getUsers();
            int length = users.length;
            while (i < length) {
                int user = users[i];
                ServiceMap smap = activeServices.getServiceMapLocked(user);
                if (smap.mServicesByInstanceName.size() > 0) {
                    for (int si = 0; si < smap.mServicesByInstanceName.size(); si++) {
                        ServiceRecord r = smap.mServicesByInstanceName.valueAt(si);
                        if (this.matcher.match(r, r.name) && (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName))) {
                            this.services.add(r);
                        }
                    }
                }
                i++;
                activeServices = this$0;
            }
        }

        private void dumpHeaderLocked() {
            this.pw.println("ACTIVITY MANAGER SERVICES (dumpsys activity services)");
            if (this.this$0.mLastAnrDump != null) {
                this.pw.println("  Last ANR service:");
                this.pw.print(this.this$0.mLastAnrDump);
                this.pw.println();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void dumpLocked() {
            dumpHeaderLocked();
            try {
                int[] users = this.this$0.mAm.mUserController.getUsers();
                for (int user : users) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && this.services.get(serviceIdx).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            }
                            dumpServiceLocalLocked(r);
                        }
                        this.needSep |= this.printed;
                    }
                    dumpUserRemainsLocked(user);
                }
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            dumpRemainsLocked();
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        /* JADX INFO: Access modifiers changed from: package-private */
        public void dumpWithClient() {
            synchronized (this.this$0.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpHeaderLocked();
                } finally {
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            try {
                int[] users = this.this$0.mAm.mUserController.getUsers();
                for (int user : users) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && this.services.get(serviceIdx).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            }
                            synchronized (this.this$0.mAm) {
                                ActivityManagerService.boostPriorityForLockedSection();
                                dumpServiceLocalLocked(r);
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            dumpServiceClient(r);
                        }
                        this.needSep |= this.printed;
                    }
                    synchronized (this.this$0.mAm) {
                        ActivityManagerService.boostPriorityForLockedSection();
                        dumpUserRemainsLocked(user);
                    }
                }
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            synchronized (this.this$0.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpRemainsLocked();
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        private void dumpUserHeaderLocked(int user) {
            if (!this.printed) {
                if (this.printedAnything) {
                    this.pw.println();
                }
                this.pw.println("  User " + user + " active services:");
                this.printed = true;
            }
            this.printedAnything = true;
            if (this.needSep) {
                this.pw.println();
            }
        }

        private void dumpServiceLocalLocked(ServiceRecord r) {
            dumpUserHeaderLocked(r.userId);
            this.pw.print("  * ");
            this.pw.println(r);
            if (this.dumpAll) {
                r.dump(this.pw, "    ");
                this.needSep = true;
                return;
            }
            this.pw.print("    app=");
            this.pw.println(r.app);
            this.pw.print("    created=");
            TimeUtils.formatDuration(r.createRealTime, this.nowReal, this.pw);
            this.pw.print(" started=");
            this.pw.print(r.startRequested);
            this.pw.print(" connections=");
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = r.getConnections();
            this.pw.println(connections.size());
            if (connections.size() > 0) {
                this.pw.println("    Connections:");
                for (int conni = 0; conni < connections.size(); conni++) {
                    ArrayList<ConnectionRecord> clist = connections.valueAt(conni);
                    for (int i = 0; i < clist.size(); i++) {
                        ConnectionRecord conn = clist.get(i);
                        this.pw.print("      ");
                        this.pw.print(conn.binding.intent.intent.getIntent().toShortString(false, false, false, false));
                        this.pw.print(" -> ");
                        ProcessRecord proc = conn.binding.client;
                        this.pw.println(proc != null ? proc.toShortString() : "null");
                    }
                }
            }
        }

        private void dumpServiceClient(ServiceRecord r) {
            IApplicationThread thread;
            ProcessRecord proc = r.app;
            if (proc == null || (thread = proc.getThread()) == null) {
                return;
            }
            this.pw.println("    Client:");
            this.pw.flush();
            try {
                TransferPipe tp = new TransferPipe();
                try {
                    thread.dumpService(tp.getWriteFd(), r, this.args);
                    tp.setBufferPrefix("      ");
                    tp.go(this.fd, 2000L);
                    tp.kill();
                } catch (Throwable th) {
                    tp.kill();
                    throw th;
                }
            } catch (RemoteException e) {
                this.pw.println("      Got a RemoteException while dumping the service");
            } catch (IOException e2) {
                this.pw.println("      Failure while dumping the service: " + e2);
            }
            this.needSep = true;
        }

        private void dumpUserRemainsLocked(int user) {
            String str;
            String str2;
            ServiceMap smap = this.this$0.getServiceMapLocked(user);
            this.printed = false;
            int SN = smap.mDelayedStartList.size();
            for (int si = 0; si < SN; si++) {
                ServiceRecord r = smap.mDelayedStartList.get(si);
                if (this.matcher.match(r, r.name) && ((str2 = this.dumpPackage) == null || str2.equals(r.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + user + " delayed start services:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Delayed start ");
                    this.pw.println(r);
                }
            }
            this.printed = false;
            int SN2 = smap.mStartingBackground.size();
            for (int si2 = 0; si2 < SN2; si2++) {
                ServiceRecord r2 = smap.mStartingBackground.get(si2);
                if (this.matcher.match(r2, r2.name) && ((str = this.dumpPackage) == null || str.equals(r2.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + user + " starting in background:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Starting bg ");
                    this.pw.println(r2);
                }
            }
        }

        private void dumpRemainsLocked() {
            String str;
            String str2;
            String str3;
            if (this.this$0.mPendingServices.size() > 0) {
                this.printed = false;
                for (int i = 0; i < this.this$0.mPendingServices.size(); i++) {
                    ServiceRecord r = this.this$0.mPendingServices.get(i);
                    if (this.matcher.match(r, r.name) && ((str3 = this.dumpPackage) == null || str3.equals(r.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Pending services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Pending ");
                        this.pw.println(r);
                        r.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.this$0.mRestartingServices.size() > 0) {
                this.printed = false;
                for (int i2 = 0; i2 < this.this$0.mRestartingServices.size(); i2++) {
                    ServiceRecord r2 = this.this$0.mRestartingServices.get(i2);
                    if (this.matcher.match(r2, r2.name) && ((str2 = this.dumpPackage) == null || str2.equals(r2.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Restarting services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Restarting ");
                        this.pw.println(r2);
                        r2.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.this$0.mDestroyingServices.size() > 0) {
                this.printed = false;
                for (int i3 = 0; i3 < this.this$0.mDestroyingServices.size(); i3++) {
                    ServiceRecord r3 = this.this$0.mDestroyingServices.get(i3);
                    if (this.matcher.match(r3, r3.name) && ((str = this.dumpPackage) == null || str.equals(r3.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Destroying services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Destroy ");
                        this.pw.println(r3);
                        r3.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.dumpAll) {
                this.printed = false;
                for (int ic = 0; ic < this.this$0.mServiceConnections.size(); ic++) {
                    ArrayList<ConnectionRecord> r4 = this.this$0.mServiceConnections.valueAt(ic);
                    for (int i4 = 0; i4 < r4.size(); i4++) {
                        ConnectionRecord cr = r4.get(i4);
                        if (this.matcher.match(cr.binding.service, cr.binding.service.name) && (this.dumpPackage == null || (cr.binding.client != null && this.dumpPackage.equals(cr.binding.client.info.packageName)))) {
                            this.printedAnything = true;
                            if (!this.printed) {
                                if (this.needSep) {
                                    this.pw.println();
                                }
                                this.needSep = true;
                                this.pw.println("  Connection bindings to services:");
                                this.printed = true;
                            }
                            this.pw.print("  * ");
                            this.pw.println(cr);
                            cr.dump(this.pw, "    ");
                        }
                    }
                }
            }
            if (this.matcher.all) {
                long nowElapsed = SystemClock.elapsedRealtime();
                int[] users = this.this$0.mAm.mUserController.getUsers();
                for (int user : users) {
                    boolean printedUser = false;
                    ServiceMap smap = this.this$0.mServiceMap.get(user);
                    if (smap != null) {
                        for (int i5 = smap.mActiveForegroundApps.size() - 1; i5 >= 0; i5--) {
                            ActiveForegroundApp aa = smap.mActiveForegroundApps.valueAt(i5);
                            String str4 = this.dumpPackage;
                            if (str4 == null || str4.equals(aa.mPackageName)) {
                                if (!printedUser) {
                                    printedUser = true;
                                    this.printedAnything = true;
                                    if (this.needSep) {
                                        this.pw.println();
                                    }
                                    this.needSep = true;
                                    this.pw.print("Active foreground apps - user ");
                                    this.pw.print(user);
                                    this.pw.println(":");
                                }
                                this.pw.print("  #");
                                this.pw.print(i5);
                                this.pw.print(": ");
                                this.pw.println(aa.mPackageName);
                                if (aa.mLabel != null) {
                                    this.pw.print("    mLabel=");
                                    this.pw.println(aa.mLabel);
                                }
                                this.pw.print("    mNumActive=");
                                this.pw.print(aa.mNumActive);
                                this.pw.print(" mAppOnTop=");
                                this.pw.print(aa.mAppOnTop);
                                this.pw.print(" mShownWhileTop=");
                                this.pw.print(aa.mShownWhileTop);
                                this.pw.print(" mShownWhileScreenOn=");
                                this.pw.println(aa.mShownWhileScreenOn);
                                this.pw.print("    mStartTime=");
                                TimeUtils.formatDuration(aa.mStartTime - nowElapsed, this.pw);
                                this.pw.print(" mStartVisibleTime=");
                                TimeUtils.formatDuration(aa.mStartVisibleTime - nowElapsed, this.pw);
                                this.pw.println();
                                if (aa.mEndTime != 0) {
                                    this.pw.print("    mEndTime=");
                                    TimeUtils.formatDuration(aa.mEndTime - nowElapsed, this.pw);
                                    this.pw.println();
                                }
                            }
                        }
                        if (smap.hasMessagesOrCallbacks()) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.printedAnything = true;
                            this.needSep = true;
                            this.pw.print("  Handler - user ");
                            this.pw.print(user);
                            this.pw.println(":");
                            smap.dumpMine(new PrintWriterPrinter(this.pw), "    ");
                        }
                    }
                }
            }
            if (!this.printedAnything) {
                this.pw.println("  (nothing)");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceDumper newServiceDumperLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        return new ServiceDumper(this, fd, pw, args, opti, dumpAll, dumpPackage);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        int[] users;
        ActiveServices activeServices = this;
        synchronized (activeServices.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long outterToken = proto.start(fieldId);
                int[] users2 = activeServices.mAm.mUserController.getUsers();
                int length = users2.length;
                int i = 0;
                while (i < length) {
                    int user = users2[i];
                    ServiceMap smap = activeServices.mServiceMap.get(user);
                    if (smap == null) {
                        users = users2;
                    } else {
                        long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                        proto.write(CompanionMessage.MESSAGE_ID, user);
                        ArrayMap<ComponentName, ServiceRecord> alls = smap.mServicesByInstanceName;
                        int i2 = 0;
                        while (i2 < alls.size()) {
                            alls.valueAt(i2).dumpDebug(proto, 2246267895810L);
                            i2++;
                            users2 = users2;
                        }
                        users = users2;
                        proto.end(token);
                    }
                    i++;
                    activeServices = this;
                    users2 = users;
                }
                proto.end(outterToken);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean dumpService(FileDescriptor fd, PrintWriter pw, String name, int[] users, String[] args, int opti, boolean dumpAll) {
        int[] users2;
        ArrayList<ServiceRecord> services = new ArrayList<>();
        Predicate<ServiceRecord> filter = DumpUtils.filterRecord(name);
        synchronized (this.mAm) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (users != null) {
                        users2 = users;
                    } else {
                        users2 = this.mAm.mUserController.getUsers();
                    }
                    for (int user : users2) {
                        ServiceMap smap = this.mServiceMap.get(user);
                        if (smap != null) {
                            ArrayMap<ComponentName, ServiceRecord> alls = smap.mServicesByInstanceName;
                            for (int i = 0; i < alls.size(); i++) {
                                ServiceRecord r1 = alls.valueAt(i);
                                if (filter.test(r1)) {
                                    services.add(r1);
                                }
                            }
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (services.size() <= 0) {
                        return false;
                    }
                    services.sort(Comparator.comparing(new Function() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda2
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            return ((ServiceRecord) obj).getComponentName();
                        }
                    }));
                    boolean needSep = false;
                    for (int i2 = 0; i2 < services.size(); i2++) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        dumpService("", fd, pw, services.get(i2), args, dumpAll);
                    }
                    return true;
                } catch (Throwable th) {
                    th = th;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void dumpService(String prefix, FileDescriptor fd, PrintWriter pw, ServiceRecord r, String[] args, boolean dumpAll) {
        IApplicationThread thread;
        String innerPrefix = prefix + "  ";
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("SERVICE ");
                pw.print(r.shortInstanceName);
                pw.print(" ");
                pw.print(Integer.toHexString(System.identityHashCode(r)));
                pw.print(" pid=");
                if (r.app != null) {
                    pw.print(r.app.getPid());
                    pw.print(" user=");
                    pw.println(r.userId);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (r.app != null && (thread = r.app.getThread()) != null) {
            pw.print(prefix);
            pw.println("  Client:");
            pw.flush();
            try {
                TransferPipe tp = new TransferPipe();
                thread.dumpService(tp.getWriteFd(), r, args);
                tp.setBufferPrefix(prefix + "    ");
                tp.go(fd);
                tp.kill();
            } catch (RemoteException e) {
                pw.println(prefix + "    Got a RemoteException while dumping the service");
            } catch (IOException e2) {
                pw.println(prefix + "    Failure while dumping the service: " + e2);
            }
        }
    }

    private void setFgsRestrictionLocked(String callingPackage, int callingPid, int callingUid, Intent intent, ServiceRecord r, int userId, boolean allowBackgroundActivityStarts) {
        r.mLastSetFgsRestrictionTime = SystemClock.elapsedRealtime();
        if (!this.mAm.mConstants.mFlagBackgroundFgsStartRestrictionEnabled) {
            r.mAllowWhileInUsePermissionInFgs = true;
        }
        if (!r.mAllowWhileInUsePermissionInFgs || r.mAllowStartForeground == -1) {
            int allowWhileInUse = shouldAllowFgsWhileInUsePermissionLocked(callingPackage, callingPid, callingUid, r, allowBackgroundActivityStarts);
            if (!r.mAllowWhileInUsePermissionInFgs) {
                r.mAllowWhileInUsePermissionInFgs = allowWhileInUse != -1;
            }
            if (r.mAllowStartForeground == -1) {
                r.mAllowStartForeground = shouldAllowFgsStartForegroundWithBindingCheckLocked(allowWhileInUse, callingPackage, callingPid, callingUid, intent, r, userId);
            }
        }
    }

    void resetFgsRestrictionLocked(ServiceRecord r) {
        r.mAllowWhileInUsePermissionInFgs = false;
        r.mAllowStartForeground = -1;
        r.mInfoAllowStartForeground = null;
        r.mInfoTempFgsAllowListReason = null;
        r.mLoggedInfoAllowStartForeground = false;
        r.mLastSetFgsRestrictionTime = 0L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canStartForegroundServiceLocked(int callingPid, int callingUid, String callingPackage) {
        if (this.mAm.mConstants.mFlagBackgroundFgsStartRestrictionEnabled) {
            int allowWhileInUse = shouldAllowFgsWhileInUsePermissionLocked(callingPackage, callingPid, callingUid, null, false);
            int allowStartFgs = shouldAllowFgsStartForegroundNoBindingCheckLocked(allowWhileInUse, callingPid, callingUid, callingPackage, null);
            if (allowStartFgs == -1 && canBindingClientStartFgsLocked(callingUid) != null) {
                allowStartFgs = 54;
            }
            return allowStartFgs != -1;
        }
        return true;
    }

    private int shouldAllowFgsWhileInUsePermissionLocked(String callingPackage, int callingPid, final int callingUid, ServiceRecord targetService, boolean allowBackgroundActivityStarts) {
        ActiveInstrumentation instr;
        Integer allowedType;
        boolean isCallerSystem;
        int ret = -1;
        int uidState = this.mAm.getUidStateLocked(callingUid);
        if (-1 == -1 && uidState <= 2) {
            ret = PowerExemptionManager.getReasonCodeFromProcState(uidState);
        }
        if (ret == -1) {
            boolean isCallingUidVisible = this.mAm.mAtmInternal.isUidForeground(callingUid);
            if (isCallingUidVisible) {
                ret = 50;
            }
        }
        if (ret == -1 && allowBackgroundActivityStarts) {
            ret = 53;
        }
        if (ret == -1) {
            int callingAppId = UserHandle.getAppId(callingUid);
            switch (callingAppId) {
                case 0:
                case 1000:
                case UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE /* 1027 */:
                case 2000:
                    isCallerSystem = true;
                    break;
                default:
                    isCallerSystem = false;
                    break;
            }
            if (isCallerSystem) {
                ret = 51;
            }
        }
        if (ret == -1 && (allowedType = (Integer) this.mAm.mProcessList.searchEachLruProcessesLOSP(false, new Function() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ActiveServices.lambda$shouldAllowFgsWhileInUsePermissionLocked$3(callingUid, (ProcessRecord) obj);
            }
        })) != null) {
            ret = allowedType.intValue();
        }
        if (ret == -1 && this.mAm.mInternal.isTempAllowlistedForFgsWhileInUse(callingUid)) {
            return 70;
        }
        if (ret == -1 && targetService != null && targetService.app != null && (instr = targetService.app.getActiveInstrumentation()) != null && instr.mHasBackgroundActivityStartsPermission) {
            ret = 60;
        }
        if (ret == -1 && this.mAm.checkPermission("android.permission.START_ACTIVITIES_FROM_BACKGROUND", callingPid, callingUid) == 0) {
            ret = 58;
        }
        if (ret == -1) {
            if (verifyPackage(callingPackage, callingUid)) {
                boolean isAllowedPackage = this.mAllowListWhileInUsePermissionInFgs.contains(callingPackage);
                if (isAllowedPackage) {
                    ret = 65;
                }
            } else {
                EventLog.writeEvent(1397638484, "215003903", Integer.valueOf(callingUid), "callingPackage:" + callingPackage + " does not belong to callingUid:" + callingUid);
            }
        }
        if (ret == -1) {
            boolean isDeviceOwner = this.mAm.mInternal.isDeviceOwner(callingUid);
            if (isDeviceOwner) {
                return 55;
            }
            return ret;
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$shouldAllowFgsWhileInUsePermissionLocked$3(int callingUid, ProcessRecord pr) {
        if (pr.uid == callingUid && pr.getWindowProcessController().areBackgroundFgsStartsAllowed()) {
            return 52;
        }
        return null;
    }

    private String canBindingClientStartFgsLocked(final int uid) {
        final ArraySet<Integer> checkedClientUids = new ArraySet<>();
        Pair<Integer, String> isAllowed = (Pair) this.mAm.mProcessList.searchEachLruProcessesLOSP(false, new Function() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ActiveServices.this.m972x366be4e2(uid, checkedClientUids, (ProcessRecord) obj);
            }
        });
        if (isAllowed == null) {
            return null;
        }
        String bindFromPackage = (String) isAllowed.second;
        return bindFromPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$canBindingClientStartFgsLocked$4$com-android-server-am-ActiveServices  reason: not valid java name */
    public /* synthetic */ Pair m972x366be4e2(int uid, ArraySet checkedClientUids, ProcessRecord pr) {
        int i = uid;
        if (pr.uid == i) {
            ProcessServiceRecord psr = pr.mServices;
            int serviceCount = psr.mServices.size();
            int svc = 0;
            while (svc < serviceCount) {
                ArrayMap<IBinder, ArrayList<ConnectionRecord>> conns = psr.mServices.valueAt(svc).getConnections();
                int size = conns.size();
                int conni = 0;
                while (conni < size) {
                    ArrayList<ConnectionRecord> crs = conns.valueAt(conni);
                    int con = 0;
                    while (con < crs.size()) {
                        ConnectionRecord cr = crs.get(con);
                        ProcessRecord clientPr = cr.binding.client;
                        if (!clientPr.isPersistent()) {
                            int clientPid = clientPr.mPid;
                            int clientUid = clientPr.uid;
                            if (clientUid != i && !checkedClientUids.contains(Integer.valueOf(clientUid))) {
                                String clientPackageName = cr.clientPackageName;
                                int allowWhileInUse2 = shouldAllowFgsWhileInUsePermissionLocked(clientPackageName, clientPid, clientUid, null, false);
                                int allowStartFgs = shouldAllowFgsStartForegroundNoBindingCheckLocked(allowWhileInUse2, clientPid, clientUid, clientPackageName, null);
                                if (allowStartFgs != -1) {
                                    return new Pair(Integer.valueOf(allowStartFgs), clientPackageName);
                                }
                                checkedClientUids.add(Integer.valueOf(clientUid));
                            }
                        }
                        con++;
                        i = uid;
                    }
                    conni++;
                    i = uid;
                }
                svc++;
                i = uid;
            }
            return null;
        }
        return null;
    }

    private int shouldAllowFgsStartForegroundWithBindingCheckLocked(int allowWhileInUse, String callingPackage, int callingPid, int callingUid, Intent intent, ServiceRecord r, int userId) {
        String bindFromPackage;
        int ret;
        ActivityManagerService.FgsTempAllowListItem tempAllowListReason = this.mAm.isAllowlistedForFgsStartLOSP(callingUid);
        r.mInfoTempFgsAllowListReason = tempAllowListReason;
        int ret2 = shouldAllowFgsStartForegroundNoBindingCheckLocked(allowWhileInUse, callingPid, callingUid, callingPackage, r);
        if (ret2 != -1) {
            bindFromPackage = null;
            ret = ret2;
        } else {
            String bindFromPackage2 = canBindingClientStartFgsLocked(callingUid);
            if (bindFromPackage2 == null) {
                bindFromPackage = bindFromPackage2;
                ret = ret2;
            } else {
                bindFromPackage = bindFromPackage2;
                ret = 54;
            }
        }
        int uidState = this.mAm.getUidStateLocked(callingUid);
        int callerTargetSdkVersion = -1;
        try {
            callerTargetSdkVersion = this.mAm.mContext.getPackageManager().getTargetSdkVersion(callingPackage);
        } catch (PackageManager.NameNotFoundException e) {
        }
        String debugInfo = "[callingPackage: " + callingPackage + "; callingUid: " + callingUid + "; uidState: " + ProcessList.makeProcStateString(uidState) + "; intent: " + intent + "; code:" + PowerExemptionManager.reasonCodeToString(ret) + "; tempAllowListReason:<" + (tempAllowListReason == null ? null : tempAllowListReason.mReason + ",reasonCode:" + PowerExemptionManager.reasonCodeToString(tempAllowListReason.mReasonCode) + ",duration:" + tempAllowListReason.mDuration + ",callingUid:" + tempAllowListReason.mCallingUid) + ">; targetSdkVersion:" + r.appInfo.targetSdkVersion + "; callerTargetSdkVersion:" + callerTargetSdkVersion + "; startForegroundCount:" + r.mStartForegroundCount + "; bindFromPackage:" + bindFromPackage + "]";
        if (!debugInfo.equals(r.mInfoAllowStartForeground)) {
            r.mLoggedInfoAllowStartForeground = false;
            r.mInfoAllowStartForeground = debugInfo;
        }
        return ret;
    }

    private int shouldAllowFgsStartForegroundNoBindingCheckLocked(int allowWhileInUse, int callingPid, final int callingUid, String callingPackage, ServiceRecord targetService) {
        String inputMethod;
        ComponentName cn;
        ActivityManagerService.FgsTempAllowListItem item;
        Integer allowedType;
        int uidState;
        int ret = allowWhileInUse;
        if (ret == -1 && (uidState = this.mAm.getUidStateLocked(callingUid)) <= 2) {
            ret = PowerExemptionManager.getReasonCodeFromProcState(uidState);
        }
        if (ret == -1 && (allowedType = (Integer) this.mAm.mProcessList.searchEachLruProcessesLOSP(false, new Function() { // from class: com.android.server.am.ActiveServices$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ActiveServices.this.m973xf9dc939e(callingUid, (ProcessRecord) obj);
            }
        })) != null) {
            ret = allowedType.intValue();
        }
        if (ret == -1 && this.mAm.checkPermission("android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND", callingPid, callingUid) == 0) {
            ret = 59;
        }
        if (ret == -1 && this.mAm.mAtmInternal.hasSystemAlertWindowPermission(callingUid, callingPid, callingPackage)) {
            ret = 62;
        }
        if (ret == -1) {
            boolean isCompanionApp = this.mAm.mInternal.isAssociatedCompanionApp(UserHandle.getUserId(callingUid), callingUid);
            if (isCompanionApp && (isPermissionGranted("android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND", callingPid, callingUid) || isPermissionGranted("android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND", callingPid, callingUid))) {
                ret = 57;
            }
        }
        if (ret == -1 && (item = this.mAm.isAllowlistedForFgsStartLOSP(callingUid)) != null) {
            if (item == ActivityManagerService.FAKE_TEMP_ALLOW_LIST_ITEM) {
                ret = 300;
            } else {
                ret = item.mReasonCode;
            }
        }
        if (ret == -1 && UserManager.isDeviceInDemoMode(this.mAm.mContext)) {
            ret = 63;
        }
        if (ret == -1) {
            boolean isProfileOwner = this.mAm.mInternal.isProfileOwner(callingUid);
            if (isProfileOwner) {
                ret = 56;
            }
        }
        if (ret == -1) {
            AppOpsManager appOpsManager = this.mAm.getAppOpsManager();
            if (appOpsManager.checkOpNoThrow(47, callingUid, callingPackage) == 0) {
                ret = 68;
            } else if (appOpsManager.checkOpNoThrow(94, callingUid, callingPackage) == 0) {
                ret = 69;
            }
        }
        if (ret == -1 && (inputMethod = Settings.Secure.getStringForUser(this.mAm.mContext.getContentResolver(), "default_input_method", UserHandle.getUserId(callingUid))) != null && (cn = ComponentName.unflattenFromString(inputMethod)) != null && cn.getPackageName().equals(callingPackage)) {
            ret = 71;
        }
        if (ret == -1 && this.mAm.mConstants.mFgsAllowOptOut && targetService != null && targetService.appInfo.hasRequestForegroundServiceExemption()) {
            return 1000;
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$shouldAllowFgsStartForegroundNoBindingCheckLocked$5$com-android-server-am-ActiveServices  reason: not valid java name */
    public /* synthetic */ Integer m973xf9dc939e(int callingUid, ProcessRecord app) {
        if (app.uid == callingUid) {
            ProcessStateRecord state = app.mState;
            if (state.isAllowedStartFgs()) {
                return Integer.valueOf(PowerExemptionManager.getReasonCodeFromProcState(state.getCurProcState()));
            }
            ActiveInstrumentation instr = app.getActiveInstrumentation();
            if (instr != null && instr.mHasBackgroundForegroundServiceStartsPermission) {
                return 61;
            }
            long lastInvisibleTime = app.mState.getLastInvisibleTime();
            if (lastInvisibleTime > 0 && lastInvisibleTime < JobStatus.NO_LATEST_RUNTIME) {
                long sinceLastInvisible = SystemClock.elapsedRealtime() - lastInvisibleTime;
                if (sinceLastInvisible < this.mAm.mConstants.mFgToBgFgsGraceDuration) {
                    return 67;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private boolean isPermissionGranted(String permission, int callingPid, int callingUid) {
        return this.mAm.checkPermission(permission, callingPid, callingUid) == 0;
    }

    private static boolean isFgsBgStart(int code) {
        return (code == 10 || code == 11 || code == 12 || code == 50) ? false : true;
    }

    private void showFgsBgRestrictedNotificationLocked(ServiceRecord r) {
        if (!this.mAm.mConstants.mFgsStartRestrictionNotificationEnabled) {
            return;
        }
        Context context = this.mAm.mContext;
        String content = "App restricted: " + r.mRecentCallingPackage;
        long now = System.currentTimeMillis();
        String bigText = DATE_FORMATTER.format(Long.valueOf(now)) + " " + r.mInfoAllowStartForeground;
        Notification.Builder n = new Notification.Builder(context, SystemNotificationChannels.ALERTS).setGroup("com.android.fgs-bg-restricted").setSmallIcon(17303684).setWhen(0L).setColor(context.getColor(17170460)).setTicker("Foreground Service BG-Launch Restricted").setContentTitle("Foreground Service BG-Launch Restricted").setContentText(content).setStyle(new Notification.BigTextStyle().bigText(bigText));
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notifyAsUser(Long.toString(now), 61, n.build(), UserHandle.ALL);
    }

    private boolean isBgFgsRestrictionEnabled(ServiceRecord r) {
        return this.mAm.mConstants.mFlagFgsStartRestrictionEnabled && CompatChanges.isChangeEnabled((long) FGS_BG_START_RESTRICTION_CHANGE_ID, r.appInfo.uid) && (!this.mAm.mConstants.mFgsStartRestrictionCheckCallerTargetSdk || CompatChanges.isChangeEnabled((long) FGS_BG_START_RESTRICTION_CHANGE_ID, r.mRecentCallingUid));
    }

    private void logFgsBackgroundStart(ServiceRecord r) {
        if (isFgsBgStart(r.mAllowStartForeground) && !r.mLoggedInfoAllowStartForeground) {
            String msg = "Background started FGS: " + (r.mAllowStartForeground != -1 ? "Allowed " : "Disallowed ") + r.mInfoAllowStartForeground;
            if (r.mAllowStartForeground != -1) {
                if (ActivityManagerUtils.shouldSamplePackageForAtom(r.packageName, this.mAm.mConstants.mFgsStartAllowedLogSampleRate)) {
                    Slog.wtfQuiet(TAG, msg);
                }
                Slog.i(TAG, msg);
            } else {
                if (ActivityManagerUtils.shouldSamplePackageForAtom(r.packageName, this.mAm.mConstants.mFgsStartDeniedLogSampleRate)) {
                    Slog.wtfQuiet(TAG, msg);
                }
                Slog.w(TAG, msg);
            }
            r.mLoggedInfoAllowStartForeground = true;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r18v0 */
    /* JADX WARN: Type inference failed for: r18v1, types: [int] */
    /* JADX WARN: Type inference failed for: r18v2 */
    private void logFGSStateChangeLocked(ServiceRecord r, int state, int durationMs, int fgsStopReason) {
        ?? r18;
        int fgsStartReasonCode;
        char c;
        int event;
        if (!ActivityManagerUtils.shouldSamplePackageForAtom(r.packageName, this.mAm.mConstants.mFgsAtomSampleRate)) {
            return;
        }
        if (state == 1 || state == 2) {
            boolean allowWhileInUsePermissionInFgs = r.mAllowWhileInUsePermissionInFgsAtEntering;
            r18 = allowWhileInUsePermissionInFgs;
            fgsStartReasonCode = r.mAllowStartForegroundAtEntering;
        } else {
            boolean allowWhileInUsePermissionInFgs2 = r.mAllowWhileInUsePermissionInFgs;
            r18 = allowWhileInUsePermissionInFgs2;
            fgsStartReasonCode = r.mAllowStartForeground;
        }
        int callerTargetSdkVersion = r.mRecentCallerApplicationInfo != null ? r.mRecentCallerApplicationInfo.targetSdkVersion : 0;
        FrameworkStatsLog.write(60, r.appInfo.uid, r.shortInstanceName, state, r18, fgsStartReasonCode, r.appInfo.targetSdkVersion, r.mRecentCallingUid, callerTargetSdkVersion, r.mInfoTempFgsAllowListReason != null ? r.mInfoTempFgsAllowListReason.mCallingUid : -1, r.mFgsNotificationWasDeferred, r.mFgsNotificationShown, durationMs, r.mStartForegroundCount, ActivityManagerUtils.hashComponentNameForAtom(r.shortInstanceName), r.mFgsHasNotificationPermission, r.foregroundServiceType);
        if (state == 1) {
            event = EventLogTags.AM_FOREGROUND_SERVICE_START;
            c = 2;
        } else {
            c = 2;
            if (state == 2) {
                event = EventLogTags.AM_FOREGROUND_SERVICE_STOP;
            } else if (state != 3) {
                return;
            } else {
                event = EventLogTags.AM_FOREGROUND_SERVICE_DENIED;
            }
        }
        Object[] objArr = new Object[11];
        objArr[0] = Integer.valueOf(r.userId);
        objArr[1] = r.shortInstanceName;
        objArr[c] = Integer.valueOf((int) r18);
        objArr[3] = PowerExemptionManager.reasonCodeToString(fgsStartReasonCode);
        objArr[4] = Integer.valueOf(r.appInfo.targetSdkVersion);
        objArr[5] = Integer.valueOf(callerTargetSdkVersion);
        objArr[6] = Integer.valueOf(r.mFgsNotificationWasDeferred ? 1 : 0);
        objArr[7] = Integer.valueOf(r.mFgsNotificationShown ? 1 : 0);
        objArr[8] = Integer.valueOf(durationMs);
        objArr[9] = Integer.valueOf(r.mStartForegroundCount);
        objArr[10] = fgsStopReasonToString(fgsStopReason);
        EventLog.writeEvent(event, objArr);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canAllowWhileInUsePermissionInFgsLocked(int callingPid, int callingUid, String callingPackage) {
        return shouldAllowFgsWhileInUsePermissionLocked(callingPackage, callingPid, callingUid, null, false) != -1;
    }

    private boolean verifyPackage(String packageName, int uid) {
        if (uid == 0 || uid == 1000) {
            return true;
        }
        return this.mAm.getPackageManagerInternal().isSameApp(packageName, uid, UserHandle.getUserId(uid));
    }

    private static String fgsStopReasonToString(int stopReason) {
        switch (stopReason) {
            case 1:
                return "STOP_FOREGROUND";
            case 2:
                return "STOP_SERVICE";
            default:
                return "UNKNOWN";
        }
    }

    /* loaded from: classes.dex */
    public static class HookActiveServiceInfo {
        private final ServiceRecord serviceRecord;

        public HookActiveServiceInfo(ServiceRecord serviceRecord) {
            this.serviceRecord = serviceRecord;
        }

        public ServiceInfo getServiceInfo() {
            ServiceRecord serviceRecord = this.serviceRecord;
            if (serviceRecord != null) {
                return serviceRecord.serviceInfo;
            }
            return null;
        }

        public Intent getIntent() {
            ServiceRecord serviceRecord = this.serviceRecord;
            if (serviceRecord != null && serviceRecord.intent != null) {
                return this.serviceRecord.intent.getIntent();
            }
            return null;
        }

        public boolean getCreatedFromFg() {
            ServiceRecord serviceRecord = this.serviceRecord;
            if (serviceRecord != null) {
                return serviceRecord.createdFromFg;
            }
            return false;
        }

        public String getShortInstanceName() {
            ServiceRecord serviceRecord = this.serviceRecord;
            if (serviceRecord != null) {
                return serviceRecord.shortInstanceName;
            }
            return null;
        }

        public ComponentName getName() {
            ServiceRecord serviceRecord = this.serviceRecord;
            if (serviceRecord != null) {
                return serviceRecord.name;
            }
            return null;
        }
    }
}
