package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IApplicationThread;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.IPermissionManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.SystemUtil;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.transsion.hubcore.server.am.ITranBroadcastQueue;
import defpackage.CompanionAppsPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
/* loaded from: classes.dex */
public final class BroadcastQueue {
    static final int BROADCAST_INTENT_MSG = 200;
    static final int BROADCAST_TIMEOUT_MSG = 201;
    public static final boolean IS_ROOT_ENABLE;
    static final int MAX_BROADCAST_HISTORY;
    static final int MAX_BROADCAST_SUMMARY_HISTORY;
    private static final String TAG = "BroadcastQueue";
    private static final String TAG_BROADCAST = TAG + ActivityManagerDebugConfig.POSTFIX_BROADCAST;
    private static final String TAG_MU = "BroadcastQueue_MU";
    final Intent[] mBroadcastSummaryHistory;
    boolean mBroadcastsScheduled;
    final BroadcastConstants mConstants;
    final boolean mDelayBehindServices;
    final BroadcastDispatcher mDispatcher;
    final BroadcastHandler mHandler;
    boolean mLogLatencyMetrics;
    BroadcastRecord mPendingBroadcast;
    int mPendingBroadcastRecvIndex;
    boolean mPendingBroadcastTimeoutMessage;
    final String mQueueName;
    final ActivityManagerService mService;
    SystemUtil mStl;
    final long[] mSummaryHistoryDispatchTime;
    final long[] mSummaryHistoryEnqueueTime;
    final long[] mSummaryHistoryFinishTime;
    int mSummaryHistoryNext;
    final ArrayList<BroadcastRecord> mParallelBroadcasts = new ArrayList<>();
    final SparseIntArray mSplitRefcounts = new SparseIntArray();
    private int mNextToken = 0;
    final BroadcastRecord[] mBroadcastHistory = new BroadcastRecord[MAX_BROADCAST_HISTORY];
    int mHistoryNext = 0;

    static {
        MAX_BROADCAST_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 10 : 50;
        MAX_BROADCAST_SUMMARY_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 25 : 300;
        IS_ROOT_ENABLE = "1".equals(SystemProperties.get("persist.user.root.support", "0"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BroadcastHandler extends Handler {
        public BroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 200:
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(BroadcastQueue.TAG_BROADCAST, "Received BROADCAST_INTENT_MSG [" + BroadcastQueue.this.mQueueName + "]");
                    }
                    BroadcastQueue.this.processNextBroadcast(true);
                    return;
                case 201:
                    synchronized (BroadcastQueue.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            BroadcastQueue.this.broadcastTimeoutLocked(true);
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastQueue(ActivityManagerService service, Handler handler, String name, BroadcastConstants constants, boolean allowDelayBehindServices) {
        int i = MAX_BROADCAST_SUMMARY_HISTORY;
        this.mBroadcastSummaryHistory = new Intent[i];
        this.mSummaryHistoryNext = 0;
        this.mSummaryHistoryEnqueueTime = new long[i];
        this.mSummaryHistoryDispatchTime = new long[i];
        this.mSummaryHistoryFinishTime = new long[i];
        this.mBroadcastsScheduled = false;
        this.mPendingBroadcast = null;
        this.mLogLatencyMetrics = true;
        this.mService = service;
        BroadcastHandler broadcastHandler = new BroadcastHandler(handler.getLooper());
        this.mHandler = broadcastHandler;
        this.mQueueName = name;
        this.mDelayBehindServices = allowDelayBehindServices;
        this.mConstants = constants;
        this.mDispatcher = new BroadcastDispatcher(this, constants, broadcastHandler, service);
        if (IS_ROOT_ENABLE) {
            HandlerThread handlerThread = new HandlerThread("KillProcessHandlerOomAdjuster");
            handlerThread.start();
            this.mStl = new SystemUtil(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start(ContentResolver resolver) {
        this.mDispatcher.start();
        this.mConstants.startObserving(this.mHandler, resolver);
    }

    public String toString() {
        return this.mQueueName;
    }

    public boolean isPendingBroadcastProcessLocked(int pid) {
        BroadcastRecord broadcastRecord = this.mPendingBroadcast;
        return broadcastRecord != null && broadcastRecord.curApp.getPid() == pid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPendingBroadcastProcessLocked(ProcessRecord app) {
        BroadcastRecord broadcastRecord = this.mPendingBroadcast;
        return broadcastRecord != null && broadcastRecord.curApp == app;
    }

    public void enqueueParallelBroadcastLocked(BroadcastRecord r) {
        r.enqueueClockTime = System.currentTimeMillis();
        r.enqueueTime = SystemClock.uptimeMillis();
        r.enqueueRealTime = SystemClock.elapsedRealtime();
        this.mParallelBroadcasts.add(r);
        enqueueBroadcastHelper(r);
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        r.enqueueClockTime = System.currentTimeMillis();
        r.enqueueTime = SystemClock.uptimeMillis();
        r.enqueueRealTime = SystemClock.elapsedRealtime();
        this.mDispatcher.enqueueOrderedBroadcastLocked(r);
        enqueueBroadcastHelper(r);
    }

    private void enqueueBroadcastHelper(BroadcastRecord r) {
        if (Trace.isTagEnabled(64L)) {
            Trace.asyncTraceBegin(64L, createBroadcastTraceTitle(r, 0), System.identityHashCode(r));
        }
    }

    public final BroadcastRecord replaceParallelBroadcastLocked(BroadcastRecord r) {
        return replaceBroadcastLocked(this.mParallelBroadcasts, r, "PARALLEL");
    }

    public final BroadcastRecord replaceOrderedBroadcastLocked(BroadcastRecord r) {
        return this.mDispatcher.replaceBroadcastLocked(r, "ORDERED");
    }

    private BroadcastRecord replaceBroadcastLocked(ArrayList<BroadcastRecord> queue, BroadcastRecord r, String typeForLogging) {
        Intent intent = r.intent;
        for (int i = queue.size() - 1; i > 0; i--) {
            BroadcastRecord old = queue.get(i);
            if (old.userId == r.userId && intent.filterEquals(old.intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "***** DROPPING " + typeForLogging + " [" + this.mQueueName + "]: " + intent);
                }
                queue.set(i, r);
                return old;
            }
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [396=5] */
    /* JADX WARN: Removed duplicated region for block: B:54:0x018d  */
    /* JADX WARN: Type inference failed for: r1v1 */
    /* JADX WARN: Type inference failed for: r1v2, types: [com.android.server.am.ProcessRecord, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r1v5 */
    /* JADX WARN: Type inference failed for: r1v6 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private final void processCurBroadcastLocked(BroadcastRecord r, ProcessRecord app, int receiverType, int processTemperature) throws RemoteException {
        ?? r1;
        ProcessReceiverRecord prr;
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Process cur broadcast " + r + " for app " + app);
        }
        IApplicationThread thread = app.getThread();
        if (thread == null) {
            throw new RemoteException();
        }
        if (app.isInFullBackup()) {
            skipReceiverLocked(r);
            return;
        }
        r.receiver = thread.asBinder();
        r.curApp = app;
        ProcessReceiverRecord prr2 = app.mReceivers;
        prr2.addCurReceiver(r);
        app.mState.forceProcessStateUpTo(11);
        if (this.mService.mInternal.getRestrictionLevel(app.info.packageName, app.userId) < 40) {
            this.mService.updateLruProcessLocked(app, false, null);
        }
        this.mService.enqueueOomAdjTargetLocked(app);
        this.mService.updateOomAdjPendingTargetsLocked("updateOomAdj_startReceiver");
        maybeReportBroadcastDispatchedEventLocked(r, r.curReceiver.applicationInfo.uid);
        r.intent.setComponent(r.curComponent);
        boolean started = false;
        try {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                try {
                    Slog.v(TAG_BROADCAST, "Delivering to component " + r.curComponent + ": " + r);
                } catch (Throwable th) {
                    th = th;
                    r1 = 0;
                    prr = prr2;
                    if (!started) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Process cur broadcast " + r + ": NOT STARTED!");
                        }
                        r.receiver = r1;
                        r.curApp = r1;
                        prr.removeCurReceiver(r);
                    }
                    throw th;
                }
            }
            this.mService.notifyPackageUse(r.intent.getComponent().getPackageName(), 3);
            try {
                r1 = 0;
                r1 = 0;
                try {
                    thread.scheduleReceiver(new Intent(r.intent), r.curReceiver, this.mService.compatibilityInfoForPackage(r.curReceiver.applicationInfo), r.resultCode, r.resultData, r.resultExtras, r.ordered, r.userId, app.mState.getReportedProcState());
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        try {
                            Slog.v(TAG_BROADCAST, "Process cur broadcast " + r + " DELIVERED for app " + app);
                        } catch (Throwable th2) {
                            th = th2;
                            prr = prr2;
                            if (!started) {
                            }
                            throw th;
                        }
                    }
                    started = true;
                    FrameworkStatsLog.write((int) FrameworkStatsLog.BROADCAST_DELIVERY_EVENT_REPORTED, app.uid, r.callingUid == -1 ? 1000 : r.callingUid, ActivityManagerService.getShortAction(r.intent.getAction()), receiverType, processTemperature);
                    if (1 == 0) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Process cur broadcast " + r + ": NOT STARTED!");
                        }
                        r.receiver = null;
                        r.curApp = null;
                        prr2.removeCurReceiver(r);
                    }
                    if (app.isKilled()) {
                        throw new RemoteException("app gets killed during broadcasting");
                    }
                } catch (Throwable th3) {
                    th = th3;
                    prr = prr2;
                }
            } catch (Throwable th4) {
                th = th4;
                prr = prr2;
                r1 = 0;
            }
        } catch (Throwable th5) {
            th = th5;
            r1 = 0;
            prr = prr2;
        }
    }

    public void updateUidReadyForBootCompletedBroadcastLocked(int uid) {
        this.mDispatcher.updateUidReadyForBootCompletedBroadcastLocked(uid);
    }

    public boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br == null || br.curApp.getPid() <= 0 || br.curApp.getPid() != app.getPid()) {
            return false;
        }
        if (br.curApp != app) {
            Slog.e(TAG, "App mismatch when sending pending broadcast to " + app.processName + ", intended target is " + br.curApp.processName);
            return false;
        }
        try {
            this.mPendingBroadcast = null;
            processCurBroadcastLocked(br, app, 2, 3);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Exception in new application when starting receiver " + br.curComponent.flattenToShortString(), e);
            logBroadcastReceiverDiscardLocked(br);
            finishReceiverLocked(br, br.resultCode, br.resultData, br.resultExtras, br.resultAbort, false);
            scheduleBroadcastsLocked();
            br.state = 0;
            throw new RuntimeException(e.getMessage());
        }
    }

    public void skipPendingBroadcastLocked(int pid) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br != null && br.curApp.getPid() == pid) {
            br.state = 0;
            br.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
            scheduleBroadcastsLocked();
        }
    }

    public void skipCurrentReceiverLocked(ProcessRecord app) {
        BroadcastRecord broadcastRecord;
        BroadcastRecord r = null;
        BroadcastRecord curActive = this.mDispatcher.getActiveBroadcastLocked();
        if (curActive != null && curActive.curApp == app) {
            r = curActive;
        }
        if (r == null && (broadcastRecord = this.mPendingBroadcast) != null && broadcastRecord.curApp == app) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "[" + this.mQueueName + "] skip & discard pending app " + r);
            }
            r = this.mPendingBroadcast;
        }
        if (r != null) {
            skipReceiverLocked(r);
        }
    }

    private void skipReceiverLocked(BroadcastRecord r) {
        logBroadcastReceiverDiscardLocked(r);
        finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
        scheduleBroadcastsLocked();
    }

    public void scheduleBroadcastsLocked() {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Schedule broadcasts [" + this.mQueueName + "]: current=" + this.mBroadcastsScheduled);
        }
        if (this.mBroadcastsScheduled) {
            return;
        }
        BroadcastHandler broadcastHandler = this.mHandler;
        broadcastHandler.sendMessage(broadcastHandler.obtainMessage(200, this));
        this.mBroadcastsScheduled = true;
    }

    public BroadcastRecord getMatchingOrderedReceiver(IBinder receiver) {
        BroadcastRecord br = this.mDispatcher.getActiveBroadcastLocked();
        if (br != null && br.receiver == receiver) {
            return br;
        }
        return null;
    }

    private int nextSplitTokenLocked() {
        int next = this.mNextToken + 1;
        if (next <= 0) {
            next = 1;
        }
        this.mNextToken = next;
        return next;
    }

    private void postActivityStartTokenRemoval(final ProcessRecord app, final BroadcastRecord r) {
        String msgToken = (app.toShortString() + r.toString()).intern();
        this.mHandler.removeCallbacksAndMessages(msgToken);
        this.mHandler.postAtTime(new Runnable() { // from class: com.android.server.am.BroadcastQueue$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BroadcastQueue.this.m1345x8f92a100(app, r);
            }
        }, msgToken, r.receiverTime + this.mConstants.ALLOW_BG_ACTIVITY_START_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postActivityStartTokenRemoval$0$com-android-server-am-BroadcastQueue  reason: not valid java name */
    public /* synthetic */ void m1345x8f92a100(ProcessRecord app, BroadcastRecord r) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                app.removeAllowBackgroundActivityStartsToken(r);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public boolean finishReceiverLocked(BroadcastRecord r, int resultCode, String resultData, Bundle resultExtras, boolean resultAbort, boolean waitForServices) {
        ActivityInfo nextReceiver;
        int state = r.state;
        ActivityInfo receiver = r.curReceiver;
        long finishTime = SystemClock.uptimeMillis();
        long elapsed = finishTime - r.receiverTime;
        r.state = 0;
        if (state == 0) {
            Slog.w(TAG_BROADCAST, "finishReceiver [" + this.mQueueName + "] called but state is IDLE");
        }
        if (r.allowBackgroundActivityStarts && r.curApp != null) {
            if (elapsed <= this.mConstants.ALLOW_BG_ACTIVITY_START_TIMEOUT) {
                postActivityStartTokenRemoval(r.curApp, r);
            } else {
                r.curApp.removeAllowBackgroundActivityStartsToken(r);
            }
        }
        if (r.nextReceiver > 0) {
            r.duration[r.nextReceiver - 1] = elapsed;
        }
        if (!r.timeoutExempt) {
            if (r.curApp != null && this.mConstants.SLOW_TIME > 0 && elapsed > this.mConstants.SLOW_TIME) {
                if (!UserHandle.isCore(r.curApp.uid)) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                        Slog.i(TAG_BROADCAST, "Broadcast receiver " + (r.nextReceiver - 1) + " was slow: " + receiver + " br=" + r);
                    }
                    this.mDispatcher.startDeferring(r.curApp.uid);
                } else if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    Slog.i(TAG_BROADCAST, "Core uid " + r.curApp.uid + " receiver was slow but not deferring: " + receiver + " br=" + r);
                }
            }
        } else if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
            Slog.i(TAG_BROADCAST, "Finished broadcast " + r.intent.getAction() + " is exempt from deferral policy");
        }
        r.receiver = null;
        r.intent.setComponent(null);
        if (r.curApp != null && r.curApp.mReceivers.hasCurReceiver(r)) {
            r.curApp.mReceivers.removeCurReceiver(r);
            this.mService.enqueueOomAdjTargetLocked(r.curApp);
        }
        if (r.curFilter != null) {
            r.curFilter.receiverList.curBroadcast = null;
        }
        r.curFilter = null;
        r.curReceiver = null;
        r.curApp = null;
        this.mPendingBroadcast = null;
        r.resultCode = resultCode;
        r.resultData = resultData;
        r.resultExtras = resultExtras;
        if (resultAbort && (r.intent.getFlags() & 134217728) == 0) {
            r.resultAbort = resultAbort;
        } else {
            r.resultAbort = false;
        }
        if (waitForServices && r.curComponent != null && r.queue.mDelayBehindServices && r.queue.mDispatcher.getActiveBroadcastLocked() == r) {
            if (r.nextReceiver < r.receivers.size()) {
                Object obj = r.receivers.get(r.nextReceiver);
                nextReceiver = obj instanceof ActivityInfo ? (ActivityInfo) obj : null;
            } else {
                nextReceiver = null;
            }
            if ((receiver == null || nextReceiver == null || receiver.applicationInfo.uid != nextReceiver.applicationInfo.uid || !receiver.processName.equals(nextReceiver.processName)) && this.mService.mServices.hasBackgroundServicesLocked(r.userId)) {
                Slog.i(TAG, "Delay finish: " + r.curComponent.flattenToShortString());
                r.state = 4;
                return false;
            }
        }
        r.curComponent = null;
        if (state != 1 && state != 3) {
            return false;
        }
        return true;
    }

    public void backgroundServicesFinishedLocked(int userId) {
        BroadcastRecord br = this.mDispatcher.getActiveBroadcastLocked();
        if (br != null && br.userId == userId && br.state == 4) {
            Slog.i(TAG, "Resuming delayed broadcast");
            br.curComponent = null;
            br.state = 0;
            processNextBroadcastLocked(false, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performReceiveLocked(ProcessRecord app, IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int receiverUid, int callingUid) throws RemoteException {
        int i;
        if (app != null) {
            IApplicationThread thread = app.getThread();
            if (thread != null) {
                try {
                    try {
                        boolean z = IS_ROOT_ENABLE;
                        if (z) {
                            this.mStl.sendNoBinderMessage(app.getPid(), app.processName, "scheduleRegisteredReceiver");
                        }
                        thread.scheduleRegisteredReceiver(receiver, intent, resultCode, data, extras, ordered, sticky, sendingUser, app.mState.getReportedProcState());
                        if (z) {
                            this.mStl.sendBinderMessage();
                        }
                    } catch (RemoteException ex) {
                        synchronized (this.mService) {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (skipExceptionOfPerformReceiveLocked(ex, app)) {
                                Slog.w(TAG, "no scheduleCrashLocked " + app.processName + " (pid " + app.getPid() + "). skip and ignore it.");
                            } else {
                                Slog.w(TAG, "Can't deliver broadcast to " + app.processName + " (pid " + app.getPid() + "). Crashing it.");
                                app.scheduleCrashLocked("can't deliver broadcast", 2, null);
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw ex;
                        }
                    }
                } catch (Throwable th) {
                    if (IS_ROOT_ENABLE) {
                        this.mStl.sendBinderMessage();
                    }
                    throw th;
                }
            } else {
                throw new RemoteException("app.thread must not be null");
            }
        } else {
            receiver.performReceive(intent, resultCode, data, extras, ordered, sticky, sendingUser);
        }
        int i2 = receiverUid == -1 ? 1000 : receiverUid;
        if (callingUid == -1) {
            i = 1000;
        } else {
            i = callingUid;
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.BROADCAST_DELIVERY_EVENT_REPORTED, i2, i, ActivityManagerService.getShortAction(intent.getAction()), 1, 1);
    }

    private boolean skipExceptionOfPerformReceiveLocked(Exception ex, ProcessRecord app) {
        boolean aospFreezerSupport = this.mService.isAppFreezerEnabled();
        if (aospFreezerSupport && ex != null && app != null && ex.getMessage() != null && ex.getMessage().startsWith("Transaction failed on small parcel") && DeadObjectException.class.isInstance(ex)) {
            return true;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:120:0x068c  */
    /* JADX WARN: Removed duplicated region for block: B:128:0x0706  */
    /* JADX WARN: Removed duplicated region for block: B:136:0x07b0  */
    /* JADX WARN: Removed duplicated region for block: B:144:0x0830  */
    /* JADX WARN: Removed duplicated region for block: B:146:0x0835  */
    /* JADX WARN: Removed duplicated region for block: B:191:0x0974  */
    /* JADX WARN: Removed duplicated region for block: B:195:0x098f  */
    /* JADX WARN: Removed duplicated region for block: B:214:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:58:0x0357  */
    /* JADX WARN: Removed duplicated region for block: B:80:0x04c6  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x04d3  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x054e  */
    /* JADX WARN: Removed duplicated region for block: B:96:0x0554  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered, int index) {
        boolean skip;
        boolean skip2;
        boolean skip3;
        String str;
        String str2;
        String str3;
        String str4;
        boolean skip4;
        boolean skip5;
        boolean skip6;
        boolean skip7;
        String str5;
        boolean skip8;
        boolean skip9;
        boolean skip10;
        boolean skip11;
        String str6;
        BroadcastQueue broadcastQueue;
        int i;
        String str7;
        boolean skip12 = false;
        if (r.options != null && !r.options.testRequireCompatChange(filter.owningUid)) {
            Slog.w(TAG, "Compat change filtered: broadcasting " + r.intent.toString() + " to uid " + filter.owningUid + " due to compat change " + r.options.getRequireCompatChangeId());
            skip12 = true;
        }
        if (!this.mService.validateAssociationAllowedLocked(r.callerPackage, r.callingUid, filter.packageName, filter.owningUid)) {
            Slog.w(TAG, "Association not allowed: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") to " + filter.packageName + " through " + filter);
            skip12 = true;
        }
        if (!skip12 && !this.mService.mIntentFirewall.checkBroadcast(r.intent, r.callingUid, r.callingPid, r.resolvedType, filter.receiverList.uid)) {
            Slog.w(TAG, "Firewall blocked: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") to " + filter.packageName + " through " + filter);
            skip12 = true;
        }
        String str8 = ") requires ";
        String str9 = ") requires appop ";
        if (filter.requiredPermission != null) {
            if (ActivityManagerService.checkComponentPermission(filter.requiredPermission, r.callingPid, r.callingUid, -1, true) != 0) {
                Slog.w(TAG, "Permission Denial: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") requires " + filter.requiredPermission + " due to registered receiver " + filter);
                skip12 = true;
            } else {
                int opCode = AppOpsManager.permissionToOpCode(filter.requiredPermission);
                if (opCode != -1 && this.mService.getAppOpsManager().noteOpNoThrow(opCode, r.callingUid, r.callerPackage, r.callerFeatureId, "Broadcast sent to protected receiver") != 0) {
                    Slog.w(TAG, "Appop Denial: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") requires appop " + AppOpsManager.permissionToOp(filter.requiredPermission) + " due to registered receiver " + filter);
                    skip12 = true;
                }
            }
        }
        if (!skip12 && (filter.receiverList.app == null || filter.receiverList.app.isKilled() || filter.receiverList.app.mErrorState.isCrashing())) {
            Slog.w(TAG, "Skipping deliver [" + this.mQueueName + "] " + r + " to " + filter.receiverList + ": process gone or crashing");
            skip12 = true;
        }
        boolean visibleToInstantApps = (r.intent.getFlags() & 2097152) != 0;
        if (!skip12 && !visibleToInstantApps && filter.instantApp && filter.receiverList.uid != r.callingUid) {
            Slog.w(TAG, "Instant App Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") due to sender " + r.callerPackage + " (uid " + r.callingUid + ") not specifying FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS");
            skip12 = true;
        }
        if (skip12 || filter.visibleToInstantApp || !r.callerInstantApp) {
            skip = skip12;
        } else {
            skip = skip12;
            if (filter.receiverList.uid != r.callingUid) {
                Slog.w(TAG, "Instant App Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") requires receiver be visible to instant apps due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                skip2 = true;
                String str10 = "Permission Denial: receiving ";
                String str11 = "Broadcast delivered to registered receiver ";
                String str12 = "Appop Denial: receiving ";
                if (skip2) {
                    skip3 = skip2;
                    if (r.requiredPermissions == null || r.requiredPermissions.length <= 0) {
                        str = "Appop Denial: receiving ";
                        str2 = "Permission Denial: receiving ";
                        str3 = "Broadcast delivered to registered receiver ";
                        str4 = ") requires appop ";
                    } else {
                        int perm = 0;
                        while (true) {
                            String str13 = str9;
                            if (perm >= r.requiredPermissions.length) {
                                str = str12;
                                str2 = str10;
                                str3 = str11;
                                str4 = str13;
                                break;
                            }
                            String requiredPermission = r.requiredPermissions[perm];
                            int i2 = perm;
                            String str14 = str12;
                            str3 = str11;
                            if (ActivityManagerService.checkComponentPermission(requiredPermission, filter.receiverList.pid, filter.receiverList.uid, -1, true) != 0) {
                                Slog.w(TAG, str10 + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + str8 + requiredPermission + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                skip4 = true;
                                str2 = str10;
                                str4 = str13;
                                str = str14;
                                break;
                            }
                            int appOp = AppOpsManager.permissionToOpCode(requiredPermission);
                            if (appOp == -1 || appOp == r.appOp) {
                                str7 = str8;
                                str2 = str10;
                            } else {
                                str7 = str8;
                                str2 = str10;
                                if (this.mService.getAppOpsManager().noteOpNoThrow(appOp, filter.receiverList.uid, filter.packageName, filter.featureId, str3 + filter.receiverId) != 0) {
                                    str = str14;
                                    str4 = str13;
                                    Slog.w(TAG, str + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + str4 + AppOpsManager.permissionToOp(requiredPermission) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                    skip4 = true;
                                    break;
                                }
                            }
                            perm = i2 + 1;
                            str12 = str14;
                            str9 = str13;
                            str11 = str3;
                            str8 = str7;
                            str10 = str2;
                        }
                        if (skip4) {
                            skip5 = skip4;
                        } else if (r.requiredPermissions == null || r.requiredPermissions.length == 0) {
                            skip5 = skip4;
                            if (ActivityManagerService.checkComponentPermission(null, filter.receiverList.pid, filter.receiverList.uid, -1, true) != 0) {
                                Slog.w(TAG, "Permission Denial: security check failed when receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                skip6 = true;
                                if (!skip6 || r.excludedPermissions == null || r.excludedPermissions.length <= 0) {
                                    skip7 = skip6;
                                    str5 = str4;
                                } else {
                                    int i3 = 0;
                                    while (i3 < r.excludedPermissions.length) {
                                        String excludedPermission = r.excludedPermissions[i3];
                                        boolean skip13 = skip6;
                                        str5 = str4;
                                        int perm2 = ActivityManagerService.checkComponentPermission(excludedPermission, filter.receiverList.pid, filter.receiverList.uid, -1, true);
                                        int appOp2 = AppOpsManager.permissionToOpCode(excludedPermission);
                                        if (appOp2 != -1) {
                                            if (perm2 != 0) {
                                                i = i3;
                                            } else {
                                                i = i3;
                                                if (this.mService.getAppOpsManager().checkOpNoThrow(appOp2, filter.receiverList.uid, filter.packageName) == 0) {
                                                    Slog.w(TAG, str + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") excludes appop " + AppOpsManager.permissionToOp(excludedPermission) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                                    skip8 = true;
                                                    break;
                                                }
                                            }
                                            i3 = i + 1;
                                            str2 = str2;
                                            skip6 = skip13;
                                            str4 = str5;
                                        } else {
                                            i = i3;
                                            if (perm2 != 0) {
                                                i3 = i + 1;
                                                str2 = str2;
                                                skip6 = skip13;
                                                str4 = str5;
                                            } else {
                                                Slog.w(TAG, str2 + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") excludes " + excludedPermission + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                                skip8 = true;
                                                break;
                                            }
                                        }
                                    }
                                    skip7 = skip6;
                                    str5 = str4;
                                }
                                skip8 = skip7;
                                if (!skip8 && r.excludedPackages != null && r.excludedPackages.length > 0 && ArrayUtils.contains(r.excludedPackages, filter.packageName)) {
                                    Slog.w(TAG, "Skipping delivery of excluded package " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") excludes package " + filter.packageName + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                    skip8 = true;
                                }
                                if (!skip8 || r.appOp == -1) {
                                    skip9 = skip8;
                                } else {
                                    skip9 = skip8;
                                    if (this.mService.getAppOpsManager().noteOpNoThrow(r.appOp, filter.receiverList.uid, filter.packageName, filter.featureId, str3 + filter.receiverId) != 0) {
                                        Slog.w(TAG, str + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + str5 + AppOpsManager.opToName(r.appOp) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                                        skip10 = true;
                                        if (skip10 && !filter.exported && ActivityManagerService.checkComponentPermission(null, r.callingPid, r.callingUid, filter.receiverList.uid, filter.exported) != 0) {
                                            Slog.w(TAG, "Exported Denial: sending " + r.intent.toString() + ", action: " + r.intent.getAction() + " from " + r.callerPackage + " (uid=" + r.callingUid + ") due to receiver " + filter.receiverList.app + " (uid " + filter.receiverList.uid + ") not specifying RECEIVER_EXPORTED");
                                            skip11 = true;
                                        } else {
                                            skip11 = skip10;
                                        }
                                        if (skip11) {
                                            r.delivery[index] = 2;
                                            return;
                                        } else if (!requestStartTargetPermissionsReviewIfNeededLocked(r, filter.packageName, filter.owningUserId)) {
                                            r.delivery[index] = 2;
                                            return;
                                        } else {
                                            r.delivery[index] = 1;
                                            if (ordered) {
                                                r.receiver = filter.receiverList.receiver.asBinder();
                                                r.curFilter = filter;
                                                filter.receiverList.curBroadcast = r;
                                                r.state = 2;
                                                if (filter.receiverList.app != null) {
                                                    r.curApp = filter.receiverList.app;
                                                    filter.receiverList.app.mReceivers.addCurReceiver(r);
                                                    this.mService.enqueueOomAdjTargetLocked(r.curApp);
                                                    this.mService.updateOomAdjPendingTargetsLocked("updateOomAdj_startReceiver");
                                                }
                                            } else if (filter.receiverList.app != null) {
                                                this.mService.mOomAdjuster.mCachedAppOptimizer.unfreezeTemporarily(filter.receiverList.app);
                                            }
                                            try {
                                                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                                                    try {
                                                        Slog.i(TAG_BROADCAST, "Delivering to " + filter + " : " + r);
                                                    } catch (RemoteException e) {
                                                        e = e;
                                                        str6 = TAG;
                                                        broadcastQueue = this;
                                                        Slog.w(str6, "Failure sending broadcast " + r.intent, e);
                                                        if (filter.receiverList.app != null) {
                                                            filter.receiverList.app.removeAllowBackgroundActivityStartsToken(r);
                                                            if (ordered) {
                                                                filter.receiverList.app.mReceivers.removeCurReceiver(r);
                                                                broadcastQueue.mService.enqueueOomAdjTargetLocked(r.curApp);
                                                            }
                                                        }
                                                        if (!ordered) {
                                                            r.receiver = null;
                                                            r.curFilter = null;
                                                            filter.receiverList.curBroadcast = null;
                                                            return;
                                                        }
                                                        return;
                                                    }
                                                }
                                                if (filter.receiverList.app != null && filter.receiverList.app.isInFullBackup()) {
                                                    if (!ordered) {
                                                        str6 = TAG;
                                                        broadcastQueue = this;
                                                    } else {
                                                        skipReceiverLocked(r);
                                                        str6 = TAG;
                                                        broadcastQueue = this;
                                                    }
                                                } else {
                                                    r.receiverTime = SystemClock.uptimeMillis();
                                                    maybeAddAllowBackgroundActivityStartsToken(filter.receiverList.app, r);
                                                    maybeScheduleTempAllowlistLocked(filter.owningUid, r, r.options);
                                                    maybeReportBroadcastDispatchedEventLocked(r, filter.owningUid);
                                                    ProcessRecord processRecord = filter.receiverList.app;
                                                    IIntentReceiver iIntentReceiver = filter.receiverList.receiver;
                                                    Intent intent = new Intent(r.intent);
                                                    int i4 = r.resultCode;
                                                    String str15 = r.resultData;
                                                    Bundle bundle = r.resultExtras;
                                                    boolean z = r.ordered;
                                                    boolean z2 = r.initialSticky;
                                                    int i5 = r.userId;
                                                    int i6 = filter.receiverList.uid;
                                                    int i7 = r.callingUid;
                                                    broadcastQueue = this;
                                                    str6 = TAG;
                                                    try {
                                                        performReceiveLocked(processRecord, iIntentReceiver, intent, i4, str15, bundle, z, z2, i5, i6, i7);
                                                        if (filter.receiverList.app != null && r.allowBackgroundActivityStarts && !r.ordered) {
                                                            broadcastQueue.postActivityStartTokenRemoval(filter.receiverList.app, r);
                                                        }
                                                    } catch (RemoteException e2) {
                                                        e = e2;
                                                        Slog.w(str6, "Failure sending broadcast " + r.intent, e);
                                                        if (filter.receiverList.app != null) {
                                                        }
                                                        if (!ordered) {
                                                        }
                                                    }
                                                }
                                                if (ordered) {
                                                    r.state = 3;
                                                    return;
                                                }
                                                return;
                                            } catch (RemoteException e3) {
                                                e = e3;
                                                str6 = TAG;
                                                broadcastQueue = this;
                                            }
                                        }
                                    }
                                }
                                skip10 = skip9;
                                if (skip10) {
                                }
                                skip11 = skip10;
                                if (skip11) {
                                }
                            }
                        } else {
                            skip5 = skip4;
                        }
                        skip6 = skip5;
                        if (!skip6) {
                        }
                        skip7 = skip6;
                        str5 = str4;
                        skip8 = skip7;
                        if (!skip8) {
                            Slog.w(TAG, "Skipping delivery of excluded package " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") excludes package " + filter.packageName + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                            skip8 = true;
                        }
                        if (!skip8) {
                        }
                        skip9 = skip8;
                        skip10 = skip9;
                        if (skip10) {
                        }
                        skip11 = skip10;
                        if (skip11) {
                        }
                    }
                } else {
                    skip3 = skip2;
                    str = "Appop Denial: receiving ";
                    str2 = "Permission Denial: receiving ";
                    str3 = "Broadcast delivered to registered receiver ";
                    str4 = ") requires appop ";
                }
                skip4 = skip3;
                if (skip4) {
                }
                skip6 = skip5;
                if (!skip6) {
                }
                skip7 = skip6;
                str5 = str4;
                skip8 = skip7;
                if (!skip8) {
                }
                if (!skip8) {
                }
                skip9 = skip8;
                skip10 = skip9;
                if (skip10) {
                }
                skip11 = skip10;
                if (skip11) {
                }
            }
        }
        skip2 = skip;
        String str102 = "Permission Denial: receiving ";
        String str112 = "Broadcast delivered to registered receiver ";
        String str122 = "Appop Denial: receiving ";
        if (skip2) {
        }
        skip4 = skip3;
        if (skip4) {
        }
        skip6 = skip5;
        if (!skip6) {
        }
        skip7 = skip6;
        str5 = str4;
        skip8 = skip7;
        if (!skip8) {
        }
        if (!skip8) {
        }
        skip9 = skip8;
        skip10 = skip9;
        if (skip10) {
        }
        skip11 = skip10;
        if (skip11) {
        }
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(BroadcastRecord receiverRecord, String receivingPackageName, final int receivingUserId) {
        boolean callerForeground;
        if (this.mService.getPackageManagerInternal().isPermissionsReviewRequired(receivingPackageName, receivingUserId)) {
            if (receiverRecord.callerApp != null) {
                callerForeground = receiverRecord.callerApp.mState.getSetSchedGroup() != 0;
            } else {
                callerForeground = true;
            }
            if (!callerForeground || receiverRecord.intent.getComponent() == null) {
                Slog.w(TAG, "u" + receivingUserId + " Receiving a broadcast in package" + receivingPackageName + " requires a permissions review");
                return false;
            }
            PendingIntentRecord intentSender = this.mService.mPendingIntentController.getIntentSender(1, receiverRecord.callerPackage, receiverRecord.callerFeatureId, receiverRecord.callingUid, receiverRecord.userId, null, null, 0, new Intent[]{receiverRecord.intent}, new String[]{receiverRecord.intent.resolveType(this.mService.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(411041792);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", receivingPackageName);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(intentSender));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                Slog.i(TAG, "u" + receivingUserId + " Launching permission review for package " + receivingPackageName);
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.BroadcastQueue.1
                @Override // java.lang.Runnable
                public void run() {
                    BroadcastQueue.this.mService.mContext.startActivityAsUser(intent, new UserHandle(receivingUserId));
                }
            });
            return false;
        }
        return true;
    }

    void maybeScheduleTempAllowlistLocked(int uid, BroadcastRecord r, BroadcastOptions brOptions) {
        if (brOptions != null && brOptions.getTemporaryAppAllowlistDuration() > 0) {
            long duration = brOptions.getTemporaryAppAllowlistDuration();
            int type = brOptions.getTemporaryAppAllowlistType();
            int reasonCode = brOptions.getTemporaryAppAllowlistReasonCode();
            String reason = brOptions.getTemporaryAppAllowlistReason();
            if (duration > 2147483647L) {
                duration = 2147483647L;
            }
            StringBuilder b = new StringBuilder();
            b.append("broadcast:");
            UserHandle.formatUid(b, r.callingUid);
            b.append(":");
            if (r.intent.getAction() != null) {
                b.append(r.intent.getAction());
            } else if (r.intent.getComponent() != null) {
                r.intent.getComponent().appendShortString(b);
            } else if (r.intent.getData() != null) {
                b.append(r.intent.getData());
            }
            b.append(",reason:");
            b.append(reason);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG, "Broadcast temp allowlist uid=" + uid + " duration=" + duration + " type=" + type + " : " + b.toString());
            }
            this.mService.tempAllowlistUidLocked(uid, duration, reasonCode, b.toString(), type, r.callingUid);
        }
    }

    final boolean isSignaturePerm(String[] perms) {
        if (perms == null) {
            return false;
        }
        IPermissionManager pm = AppGlobals.getPermissionManager();
        for (int i = perms.length - 1; i >= 0; i--) {
            try {
                PermissionInfo pi = pm.getPermissionInfo(perms[i], PackageManagerService.PLATFORM_PACKAGE_NAME, 0);
                if (pi == null || (pi.protectionLevel & 31) != 2) {
                    return false;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processNextBroadcast(boolean fromMsg) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                processNextBroadcastLocked(fromMsg, false);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    static String broadcastDescription(BroadcastRecord r, ComponentName component) {
        return r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") to " + component.flattenToShortString();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v7, resolved type: com.android.server.am.BroadcastDispatcher */
    /* JADX DEBUG: Multi-variable search result rejected for r41v0, resolved type: com.android.server.am.BroadcastQueue */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:162:0x054d  */
    /* JADX WARN: Removed duplicated region for block: B:180:0x060d  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x0622  */
    /* JADX WARN: Removed duplicated region for block: B:187:0x0630  */
    /* JADX WARN: Removed duplicated region for block: B:190:0x0653  */
    /* JADX WARN: Removed duplicated region for block: B:363:0x0dc6  */
    /* JADX WARN: Removed duplicated region for block: B:368:0x0de7  */
    /* JADX WARN: Removed duplicated region for block: B:408:0x0f0c  */
    /* JADX WARN: Removed duplicated region for block: B:426:0x0f4b  */
    /* JADX WARN: Removed duplicated region for block: B:431:0x0f90  */
    /* JADX WARN: Removed duplicated region for block: B:478:0x1111  */
    /* JADX WARN: Removed duplicated region for block: B:481:0x114d  */
    /* JADX WARN: Removed duplicated region for block: B:482:0x1154  */
    /* JADX WARN: Removed duplicated region for block: B:485:0x1172  */
    /* JADX WARN: Removed duplicated region for block: B:487:0x11c8  */
    /* JADX WARN: Removed duplicated region for block: B:501:0x126f A[LOOP:2: B:60:0x01fa->B:501:0x126f, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:544:0x0687 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:554:0x0eb8 A[SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r14v0 */
    /* JADX WARN: Type inference failed for: r14v1, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r14v18 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final void processNextBroadcastLocked(boolean fromMsg, boolean skipOomAdj) {
        int i;
        boolean forceReceive;
        BroadcastRecord r;
        IIntentReceiver iIntentReceiver;
        long j;
        BroadcastRecord r2;
        boolean sendResult;
        boolean skip;
        int opCode;
        boolean isSingleton;
        int perm;
        int i2;
        boolean skip2;
        int i3;
        String targetProcess;
        int receiverUid;
        boolean reseut;
        boolean skip3;
        int perm2;
        boolean skip4;
        int appOp;
        BroadcastRecord defer;
        ProcessRecord proc;
        ProcessRecord processRecord;
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: " + this.mParallelBroadcasts.size() + " parallel broadcasts; " + this.mDispatcher.describeStateLocked());
        }
        this.mService.updateCpuStats();
        ?? r14 = 0;
        if (fromMsg) {
            this.mBroadcastsScheduled = false;
        }
        while (true) {
            i = 1;
            if (this.mParallelBroadcasts.size() <= 0) {
                break;
            }
            BroadcastRecord r3 = this.mParallelBroadcasts.remove(0);
            r3.dispatchTime = SystemClock.uptimeMillis();
            r3.dispatchRealTime = SystemClock.elapsedRealtime();
            r3.dispatchClockTime = System.currentTimeMillis();
            if (Trace.isTagEnabled(64L)) {
                Trace.asyncTraceEnd(64L, createBroadcastTraceTitle(r3, 0), System.identityHashCode(r3));
                Trace.asyncTraceBegin(64L, createBroadcastTraceTitle(r3, 1), System.identityHashCode(r3));
            }
            int N = r3.receivers.size();
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Processing parallel broadcast [" + this.mQueueName + "] " + r3);
            }
            for (int i4 = 0; i4 < N; i4++) {
                Object target = r3.receivers.get(i4);
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Delivering non-ordered on [" + this.mQueueName + "] to registered " + target + ": " + r3);
                }
                deliverToRegisteredReceiverLocked(r3, (BroadcastFilter) target, false, i4);
            }
            addBroadcastToHistoryLocked(r3);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Done with parallel broadcast [" + this.mQueueName + "] " + r3);
            }
        }
        if (this.mPendingBroadcast != null) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: waiting for " + this.mPendingBroadcast.curApp);
            }
            if (this.mPendingBroadcast.curApp.getPid() <= 0) {
                ProcessRecord proc2 = (ProcessRecord) this.mService.mProcessList.getProcessNamesLOSP().get(this.mPendingBroadcast.curApp.processName, this.mPendingBroadcast.curApp.uid);
                proc = (proc2 == null || !proc2.isPendingStart()) ? 1 : null;
            } else {
                synchronized (this.mService.mPidsSelfLocked) {
                    ProcessRecord proc3 = this.mService.mPidsSelfLocked.get(this.mPendingBroadcast.curApp.getPid());
                    if (proc3 != null && !proc3.mErrorState.isCrashing()) {
                        processRecord = null;
                        proc = processRecord;
                    }
                    processRecord = 1;
                    proc = processRecord;
                }
            }
            if (proc == null) {
                return;
            }
            Slog.w(TAG, "pending app  [" + this.mQueueName + "]" + this.mPendingBroadcast.curApp + " died before responding to broadcast");
            this.mPendingBroadcast.state = 0;
            this.mPendingBroadcast.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
        }
        boolean looped = false;
        while (true) {
            long now = SystemClock.uptimeMillis();
            BroadcastRecord r4 = this.mDispatcher.getNextBroadcastLocked(now);
            if (r4 == null) {
                this.mDispatcher.scheduleDeferralCheckLocked(r14);
                synchronized (this.mService.mAppProfiler.mProfilerLock) {
                    this.mService.mAppProfiler.scheduleAppGcsLPf();
                }
                if (looped && !skipOomAdj) {
                    this.mService.updateOomAdjPendingTargetsLocked("updateOomAdj_startReceiver");
                }
                if (this.mService.mUserController.mBootCompleted && this.mLogLatencyMetrics) {
                    this.mLogLatencyMetrics = r14;
                    return;
                }
                return;
            }
            int numReceivers = r4.receivers != null ? r4.receivers.size() : r14;
            if (this.mService.mProcessesReady && !r4.timeoutExempt && r4.dispatchTime > 0 && numReceivers > 0 && now > r4.dispatchTime + (this.mConstants.TIMEOUT * 2 * numReceivers)) {
                Slog.w(TAG, "Hung broadcast [" + this.mQueueName + "] discarded after timeout failure: now=" + now + " dispatchTime=" + r4.dispatchTime + " startTime=" + r4.receiverTime + " intent=" + r4.intent + " numReceivers=" + numReceivers + " nextReceiver=" + r4.nextReceiver + " state=" + r4.state);
                broadcastTimeoutLocked(r14);
                r4.state = r14;
                forceReceive = true;
            } else {
                forceReceive = false;
            }
            if (r4.state != 0) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.d(TAG_BROADCAST, "processNextBroadcast(" + this.mQueueName + ") called when not idle (state=" + r4.state + ")");
                    return;
                }
                return;
            }
            if (r4.receivers == null || r4.nextReceiver >= numReceivers || r4.resultAbort || forceReceive) {
                if (r4.resultTo != null) {
                    if (r4.splitToken != 0) {
                        int newCount = this.mSplitRefcounts.get(r4.splitToken) - i;
                        if (newCount == 0) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                Slog.i(TAG_BROADCAST, "Sending broadcast completion for split token " + r4.splitToken + " : " + r4.intent.getAction());
                            }
                            this.mSplitRefcounts.delete(r4.splitToken);
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                Slog.i(TAG_BROADCAST, "Result refcount now " + newCount + " for split token " + r4.splitToken + " : " + r4.intent.getAction() + " - not sending completion yet");
                            }
                            this.mSplitRefcounts.put(r4.splitToken, newCount);
                            sendResult = false;
                            if (sendResult) {
                                r = r4;
                                iIntentReceiver = null;
                                j = 64;
                            } else {
                                if (r4.callerApp != null) {
                                    this.mService.mOomAdjuster.mCachedAppOptimizer.unfreezeTemporarily(r4.callerApp);
                                }
                                try {
                                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                        try {
                                            Slog.i(TAG_BROADCAST, "Finishing broadcast [" + this.mQueueName + "] " + r4.intent.getAction() + " app=" + r4.callerApp);
                                        } catch (RemoteException e) {
                                            e = e;
                                            r = r4;
                                            iIntentReceiver = null;
                                            j = 64;
                                            r.resultTo = iIntentReceiver;
                                            Slog.w(TAG, "Failure [" + this.mQueueName + "] sending broadcast result of " + r.intent, e);
                                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                            }
                                            cancelBroadcastTimeoutLocked();
                                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                                            }
                                            addBroadcastToHistoryLocked(r);
                                            if (r.intent.getComponent() == null) {
                                            }
                                            this.mDispatcher.retireBroadcastLocked(r);
                                            looped = true;
                                            r2 = null;
                                            if (r2 != null) {
                                            }
                                        }
                                    }
                                    r = r4;
                                    iIntentReceiver = null;
                                    j = 64;
                                    try {
                                        performReceiveLocked(r4.callerApp, r4.resultTo, new Intent(r4.intent), r4.resultCode, r4.resultData, r4.resultExtras, false, false, r4.userId, r4.callingUid, r4.callingUid);
                                        logBootCompletedBroadcastCompletionLatencyIfPossible(r);
                                        r.resultTo = null;
                                    } catch (RemoteException e2) {
                                        e = e2;
                                        r.resultTo = iIntentReceiver;
                                        Slog.w(TAG, "Failure [" + this.mQueueName + "] sending broadcast result of " + r.intent, e);
                                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                        }
                                        cancelBroadcastTimeoutLocked();
                                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                                        }
                                        addBroadcastToHistoryLocked(r);
                                        if (r.intent.getComponent() == null) {
                                            this.mService.addBroadcastStatLocked(r.intent.getAction(), r.callerPackage, r.manifestCount, r.manifestSkipCount, r.finishTime - r.dispatchTime);
                                        }
                                        this.mDispatcher.retireBroadcastLocked(r);
                                        looped = true;
                                        r2 = null;
                                        if (r2 != null) {
                                        }
                                    }
                                } catch (RemoteException e3) {
                                    e = e3;
                                    r = r4;
                                    iIntentReceiver = null;
                                    j = 64;
                                }
                            }
                        }
                    }
                    sendResult = true;
                    if (sendResult) {
                    }
                } else {
                    r = r4;
                    iIntentReceiver = null;
                    j = 64;
                }
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Cancelling BROADCAST_TIMEOUT_MSG");
                }
                cancelBroadcastTimeoutLocked();
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                    Slog.v(TAG_BROADCAST, "Finished with ordered broadcast " + r);
                }
                addBroadcastToHistoryLocked(r);
                if (r.intent.getComponent() == null && r.intent.getPackage() == null && (r.intent.getFlags() & 1073741824) == 0) {
                    this.mService.addBroadcastStatLocked(r.intent.getAction(), r.callerPackage, r.manifestCount, r.manifestSkipCount, r.finishTime - r.dispatchTime);
                }
                this.mDispatcher.retireBroadcastLocked(r);
                looped = true;
                r2 = null;
            } else {
                if (!r4.deferred) {
                    int receiverUid2 = r4.getReceiverUid(r4.receivers.get(r4.nextReceiver));
                    if (this.mDispatcher.isDeferringLocked(receiverUid2)) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                            Slog.i(TAG_BROADCAST, "Next receiver in " + r4 + " uid " + receiverUid2 + " at " + r4.nextReceiver + " is under deferral");
                        }
                        if (r4.nextReceiver + i == numReceivers) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                Slog.i(TAG_BROADCAST, "Sole receiver of " + r4 + " is under deferral; setting aside and proceeding");
                            }
                            defer = r4;
                            this.mDispatcher.retireBroadcastLocked(r4);
                        } else {
                            defer = r4.splitRecipientsLocked(receiverUid2, r4.nextReceiver);
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                String str = TAG_BROADCAST;
                                Slog.i(str, "Post split:");
                                Slog.i(str, "Original broadcast receivers:");
                                for (int i5 = 0; i5 < r4.receivers.size(); i5++) {
                                    Slog.i(TAG_BROADCAST, "  " + r4.receivers.get(i5));
                                }
                                Slog.i(TAG_BROADCAST, "Split receivers:");
                                for (int i6 = 0; i6 < defer.receivers.size(); i6++) {
                                    Slog.i(TAG_BROADCAST, "  " + defer.receivers.get(i6));
                                }
                            }
                            if (r4.resultTo != null) {
                                int token = r4.splitToken;
                                if (token == 0) {
                                    int nextSplitTokenLocked = nextSplitTokenLocked();
                                    defer.splitToken = nextSplitTokenLocked;
                                    r4.splitToken = nextSplitTokenLocked;
                                    this.mSplitRefcounts.put(r4.splitToken, 2);
                                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                        Slog.i(TAG_BROADCAST, "Broadcast needs split refcount; using new token " + r4.splitToken);
                                    }
                                } else {
                                    int curCount = this.mSplitRefcounts.get(token);
                                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL && curCount == 0) {
                                        Slog.wtf(TAG_BROADCAST, "Split refcount is zero with token for " + r4);
                                    }
                                    this.mSplitRefcounts.put(token, curCount + 1);
                                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                        Slog.i(TAG_BROADCAST, "New split count for token " + token + " is " + (curCount + 1));
                                    }
                                }
                            }
                        }
                        this.mDispatcher.addDeferredBroadcast(receiverUid2, defer);
                        looped = true;
                        r2 = null;
                        iIntentReceiver = null;
                        j = 64;
                    }
                }
                r2 = r4;
                iIntentReceiver = null;
                j = 64;
            }
            if (r2 != null) {
                int recIdx = r2.nextReceiver;
                r2.nextReceiver = recIdx + 1;
                r2.receiverTime = SystemClock.uptimeMillis();
                if (recIdx == 0) {
                    r2.dispatchTime = r2.receiverTime;
                    r2.dispatchRealTime = SystemClock.elapsedRealtime();
                    r2.dispatchClockTime = System.currentTimeMillis();
                    if (this.mLogLatencyMetrics) {
                        FrameworkStatsLog.write(142, r2.dispatchClockTime - r2.enqueueClockTime);
                    }
                    if (Trace.isTagEnabled(j)) {
                        long j2 = j;
                        Trace.asyncTraceEnd(j2, createBroadcastTraceTitle(r2, 0), System.identityHashCode(r2));
                        Trace.asyncTraceBegin(j2, createBroadcastTraceTitle(r2, 1), System.identityHashCode(r2));
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        Slog.v(TAG_BROADCAST, "Processing ordered broadcast [" + this.mQueueName + "] " + r2);
                    }
                }
                if (!this.mPendingBroadcastTimeoutMessage) {
                    long timeoutTime = r2.receiverTime + this.mConstants.TIMEOUT;
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Submitting BROADCAST_TIMEOUT_MSG [" + this.mQueueName + "] for " + r2 + " at " + timeoutTime);
                    }
                    setBroadcastTimeoutLocked(timeoutTime);
                }
                BroadcastOptions brOptions = r2.options;
                Object nextReceiver = r2.receivers.get(recIdx);
                if (nextReceiver instanceof BroadcastFilter) {
                    BroadcastFilter filter = (BroadcastFilter) nextReceiver;
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Delivering ordered [" + this.mQueueName + "] to registered " + filter + ": " + r2);
                    }
                    deliverToRegisteredReceiverLocked(r2, filter, r2.ordered, recIdx);
                    if (r2.receiver == null || !r2.ordered) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Quick finishing [" + this.mQueueName + "]: ordered=" + r2.ordered + " receiver=" + r2.receiver);
                        }
                        r2.state = 0;
                        scheduleBroadcastsLocked();
                        return;
                    } else if (filter.receiverList != null) {
                        maybeAddAllowBackgroundActivityStartsToken(filter.receiverList.app, r2);
                        return;
                    } else {
                        return;
                    }
                }
                ResolveInfo info = (ResolveInfo) nextReceiver;
                ComponentName component = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                boolean skip5 = false;
                if (brOptions != null && (info.activityInfo.applicationInfo.targetSdkVersion < brOptions.getMinManifestReceiverApiLevel() || info.activityInfo.applicationInfo.targetSdkVersion > brOptions.getMaxManifestReceiverApiLevel())) {
                    Slog.w(TAG, "Target SDK mismatch: receiver " + info.activityInfo + " targets " + info.activityInfo.applicationInfo.targetSdkVersion + " but delivery restricted to [" + brOptions.getMinManifestReceiverApiLevel() + ", " + brOptions.getMaxManifestReceiverApiLevel() + "] broadcasting " + broadcastDescription(r2, component));
                    skip5 = true;
                }
                if (brOptions != null && !brOptions.testRequireCompatChange(info.activityInfo.applicationInfo.uid)) {
                    Slog.w(TAG, "Compat change filtered: broadcasting " + broadcastDescription(r2, component) + " to uid " + info.activityInfo.applicationInfo.uid + " due to compat change " + r2.options.getRequireCompatChangeId());
                    skip5 = true;
                }
                if (!skip5 && !this.mService.validateAssociationAllowedLocked(r2.callerPackage, r2.callingUid, component.getPackageName(), info.activityInfo.applicationInfo.uid)) {
                    Slog.w(TAG, "Association not allowed: broadcasting " + broadcastDescription(r2, component));
                    skip5 = true;
                }
                if (!skip5 && (!this.mService.mIntentFirewall.checkBroadcast(r2.intent, r2.callingUid, r2.callingPid, r2.resolvedType, info.activityInfo.applicationInfo.uid))) {
                    Slog.w(TAG, "Firewall blocked: broadcasting " + broadcastDescription(r2, component));
                }
                int perm3 = ActivityManagerService.checkComponentPermission(info.activityInfo.permission, r2.callingPid, r2.callingUid, info.activityInfo.applicationInfo.uid, info.activityInfo.exported);
                if (!skip5 && perm3 != 0) {
                    if (!info.activityInfo.exported) {
                        Slog.w(TAG, "Permission Denial: broadcasting " + broadcastDescription(r2, component) + " is not exported from uid " + info.activityInfo.applicationInfo.uid);
                    } else {
                        Slog.w(TAG, "Permission Denial: broadcasting " + broadcastDescription(r2, component) + " requires " + info.activityInfo.permission);
                    }
                    skip = true;
                } else if (!skip5 && info.activityInfo.permission != null && (opCode = AppOpsManager.permissionToOpCode(info.activityInfo.permission)) != -1 && this.mService.getAppOpsManager().noteOpNoThrow(opCode, r2.callingUid, r2.callerPackage, r2.callerFeatureId, "Broadcast delivered to " + info.activityInfo.name) != 0) {
                    Slog.w(TAG, "Appop Denial: broadcasting " + broadcastDescription(r2, component) + " requires appop " + AppOpsManager.permissionToOp(info.activityInfo.permission));
                    skip = true;
                } else {
                    skip = skip5;
                }
                try {
                    boolean isSingleton2 = this.mService.isSingleton(info.activityInfo.processName, info.activityInfo.applicationInfo, info.activityInfo.name, info.activityInfo.flags);
                    isSingleton = isSingleton2;
                } catch (SecurityException e4) {
                    Slog.w(TAG, e4.getMessage());
                    skip = true;
                    isSingleton = false;
                }
                if ((info.activityInfo.flags & 1073741824) != 0 && ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS", info.activityInfo.applicationInfo.uid) != 0) {
                    Slog.w(TAG, "Permission Denial: Receiver " + component.flattenToShortString() + " requests FLAG_SINGLE_USER, but app does not hold android.permission.INTERACT_ACROSS_USERS");
                    skip = true;
                }
                if (!skip && info.activityInfo.applicationInfo.isInstantApp() && r2.callingUid != info.activityInfo.applicationInfo.uid) {
                    Slog.w(TAG, "Instant App Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ") Instant Apps do not support manifest receivers");
                    skip = true;
                }
                if (!skip && r2.callerInstantApp && (info.activityInfo.flags & 1048576) == 0 && r2.callingUid != info.activityInfo.applicationInfo.uid) {
                    Slog.w(TAG, "Instant App Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires receiver have visibleToInstantApps set due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                    skip = true;
                }
                if (r2.curApp != null && r2.curApp.mErrorState.isCrashing()) {
                    Slog.w(TAG, "Skipping deliver ordered [" + this.mQueueName + "] " + r2 + " to " + r2.curApp + ": process crashing");
                    skip = true;
                }
                if (!skip) {
                    boolean isAvailable = false;
                    try {
                        isAvailable = AppGlobals.getPackageManager().isPackageAvailable(info.activityInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                    } catch (Exception e5) {
                        Slog.w(TAG, "Exception getting recipient info for " + info.activityInfo.packageName, e5);
                    }
                    if (!isAvailable) {
                        Slog.w(TAG_BROADCAST, "Skipping delivery to " + info.activityInfo.packageName + " / " + info.activityInfo.applicationInfo.uid + " : package no longer available");
                        skip = true;
                    }
                }
                if (!skip && !requestStartTargetPermissionsReviewIfNeededLocked(r2, info.activityInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid))) {
                    Slog.w(TAG_BROADCAST, "Skipping delivery: permission review required for " + broadcastDescription(r2, component));
                    skip = true;
                }
                int receiverUid3 = info.activityInfo.applicationInfo.uid;
                if (r2.callingUid != 1000 && isSingleton && this.mService.isValidSingletonCall(r2.callingUid, receiverUid3)) {
                    info.activityInfo = this.mService.getActivityInfoForUser(info.activityInfo, 0);
                }
                String targetProcess2 = info.activityInfo.processName;
                ProcessRecord app = this.mService.getProcessRecordLocked(targetProcess2, info.activityInfo.applicationInfo.uid);
                if (skip) {
                    perm = perm3;
                } else {
                    perm = perm3;
                    int allowed = this.mService.getAppStartModeLOSP(info.activityInfo.applicationInfo.uid, info.activityInfo.packageName, info.activityInfo.applicationInfo.targetSdkVersion, -1, true, false, false);
                    if (allowed != 0) {
                        if (allowed == 3) {
                            Slog.w(TAG, "Background execution disabled: receiving " + r2.intent + " to " + component.flattenToShortString());
                            skip = true;
                        } else if ((r2.intent.getFlags() & 8388608) != 0 || (r2.intent.getComponent() == null && r2.intent.getPackage() == null && (r2.intent.getFlags() & 16777216) == 0 && !isSignaturePerm(r2.requiredPermissions))) {
                            this.mService.addBackgroundCheckViolationLocked(r2.intent.getAction(), component.getPackageName());
                            Slog.w(TAG, "Background execution not allowed: receiving " + r2.intent + " to " + component.flattenToShortString());
                            skip = true;
                        }
                    }
                }
                if (!skip && !"android.intent.action.ACTION_SHUTDOWN".equals(r2.intent.getAction()) && !this.mService.mUserController.isUserRunning(UserHandle.getUserId(info.activityInfo.applicationInfo.uid), 0)) {
                    skip = true;
                    Slog.w(TAG, "Skipping delivery to " + info.activityInfo.packageName + " / " + info.activityInfo.applicationInfo.uid + " : user is not running");
                }
                boolean isSuppress = this.mService.mAmsExt.onBeforeStartProcessForStaticReceiver(info.activityInfo.packageName);
                if (isSuppress) {
                    Slog.d(TAG, "processNextBroadcastLocked, suppress to start process of staticReceiver for package:" + info.activityInfo.packageName);
                    skip = true;
                }
                if (!skip && r2.excludedPermissions != null && r2.excludedPermissions.length > 0) {
                    int i7 = 0;
                    while (true) {
                        if (i7 >= r2.excludedPermissions.length) {
                            i2 = perm;
                            break;
                        }
                        String excludedPermission = r2.excludedPermissions[i7];
                        try {
                            skip4 = skip;
                        } catch (RemoteException e6) {
                            skip4 = skip;
                        }
                        try {
                            perm = AppGlobals.getPackageManager().checkPermission(excludedPermission, info.activityInfo.applicationInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                        } catch (RemoteException e7) {
                            perm = -1;
                            appOp = AppOpsManager.permissionToOpCode(excludedPermission);
                            if (appOp != -1) {
                            }
                        }
                        appOp = AppOpsManager.permissionToOpCode(excludedPermission);
                        if (appOp != -1) {
                            if (perm != 0) {
                                i7++;
                                skip = skip4;
                            } else {
                                skip = true;
                                i2 = perm;
                                break;
                            }
                        } else {
                            if (perm == 0 && this.mService.getAppOpsManager().checkOpNoThrow(appOp, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName) == 0) {
                                skip = true;
                                i2 = perm;
                                break;
                            }
                            i7++;
                            skip = skip4;
                        }
                    }
                } else {
                    i2 = perm;
                    skip = skip;
                }
                if (!skip && r2.excludedPackages != null && r2.excludedPackages.length > 0 && ArrayUtils.contains(r2.excludedPackages, component.getPackageName())) {
                    Slog.w(TAG, "Skipping delivery of excluded package " + r2.intent + " to " + component.flattenToShortString() + " excludes package " + component.getPackageName() + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                    skip = true;
                }
                if (skip || info.activityInfo.applicationInfo.uid == 1000 || r2.requiredPermissions == null || r2.requiredPermissions.length <= 0) {
                    skip = skip;
                } else {
                    int perm4 = i2;
                    int perm5 = 0;
                    while (true) {
                        if (perm5 >= r2.requiredPermissions.length) {
                            break;
                        }
                        String requiredPermission = r2.requiredPermissions[perm5];
                        try {
                            try {
                                skip3 = skip;
                                try {
                                    perm2 = AppGlobals.getPackageManager().checkPermission(requiredPermission, info.activityInfo.applicationInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                                } catch (RemoteException e8) {
                                    perm2 = -1;
                                    if (perm2 == 0) {
                                    }
                                    if (skip) {
                                    }
                                    skip2 = skip;
                                    if (!skip2) {
                                    }
                                }
                            } catch (RemoteException e9) {
                                skip3 = skip;
                            }
                        } catch (RemoteException e10) {
                            skip3 = skip;
                        }
                        if (perm2 == 0) {
                            Slog.w(TAG, "Permission Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires " + requiredPermission + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                            skip = true;
                            break;
                        }
                        int perm6 = perm2;
                        int appOp2 = AppOpsManager.permissionToOpCode(requiredPermission);
                        if (appOp2 == -1 || appOp2 == r2.appOp || noteOpForManifestReceiver(appOp2, r2, info, component)) {
                            perm5++;
                            perm4 = perm6;
                            skip = skip3;
                        } else {
                            skip = true;
                            break;
                        }
                    }
                }
                if (skip && r2.appOp != -1 && !noteOpForManifestReceiver(r2.appOp, r2, info, component)) {
                    skip2 = true;
                } else {
                    skip2 = skip;
                }
                if (!skip2) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Skipping delivery of ordered [" + this.mQueueName + "] " + r2 + " for reason described above");
                    }
                    r2.delivery[recIdx] = 2;
                    r2.receiver = null;
                    r2.curFilter = null;
                    r2.state = 0;
                    r2.manifestSkipCount++;
                    scheduleBroadcastsLocked();
                    return;
                }
                r2.manifestCount++;
                r2.delivery[recIdx] = 1;
                r2.state = 1;
                r2.curComponent = component;
                r2.curReceiver = info.activityInfo;
                if (ActivityManagerDebugConfig.DEBUG_MU && r2.callingUid > 100000) {
                    Slog.v(TAG_MU, "Updated broadcast record activity info for secondary user, " + info.activityInfo + ", callingUid = " + r2.callingUid + ", uid = " + receiverUid3);
                }
                boolean isActivityCapable = brOptions != null && brOptions.getTemporaryAppAllowlistDuration() > 0;
                maybeScheduleTempAllowlistLocked(receiverUid3, r2, brOptions);
                if (r2.intent.getComponent() != null && r2.curComponent != null && !TextUtils.equals(r2.curComponent.getPackageName(), r2.callerPackage)) {
                    this.mService.mUsageStatsService.reportEvent(r2.curComponent.getPackageName(), r2.userId, 31);
                }
                try {
                    AppGlobals.getPackageManager().setPackageStoppedState(r2.curComponent.getPackageName(), false, r2.userId);
                } catch (RemoteException e11) {
                } catch (IllegalArgumentException e12) {
                    Slog.w(TAG, "Failed trying to unstop package " + r2.curComponent.getPackageName() + ": " + e12);
                }
                if (app == null || app.getThread() == null || app.isKilled()) {
                    i3 = 1;
                    targetProcess = targetProcess2;
                    receiverUid = receiverUid3;
                } else {
                    try {
                        try {
                            try {
                                app.addPackage(info.activityInfo.packageName, info.activityInfo.applicationInfo.longVersionCode, this.mService.mProcessStats);
                                maybeAddAllowBackgroundActivityStartsToken(app, r2);
                                i3 = 1;
                            } catch (RemoteException e13) {
                                e = e13;
                                i3 = 1;
                            }
                            try {
                                processCurBroadcastLocked(r2, app, 2, 1);
                                return;
                            } catch (RemoteException e14) {
                                e = e14;
                                targetProcess = targetProcess2;
                                receiverUid = receiverUid3;
                                Slog.w(TAG, "Exception when sending broadcast to " + r2.curComponent, e);
                                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                }
                                reseut = ITranBroadcastQueue.Instance().hookPmLimitStartProcessForReceiver(r2.callingPid, r2.callingUid, r2.callerPackage, r2.callerApp == null ? r2.callerApp.processWrapper : null, r2.intent, r2.curReceiver, r2.curComponent, r2.ordered);
                                if (!reseut) {
                                }
                            }
                        } catch (RemoteException e15) {
                            e = e15;
                            i3 = 1;
                            targetProcess = targetProcess2;
                            receiverUid = receiverUid3;
                        }
                    } catch (RuntimeException e16) {
                        Slog.wtf(TAG, "Failed sending broadcast to " + r2.curComponent + " with " + r2.intent, e16);
                        logBroadcastReceiverDiscardLocked(r2);
                        finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                        scheduleBroadcastsLocked();
                        r2.state = 0;
                        return;
                    }
                }
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Need to start app [" + this.mQueueName + "] " + targetProcess + " for broadcast " + r2);
                }
                reseut = ITranBroadcastQueue.Instance().hookPmLimitStartProcessForReceiver(r2.callingPid, r2.callingUid, r2.callerPackage, r2.callerApp == null ? r2.callerApp.processWrapper : null, r2.intent, r2.curReceiver, r2.curComponent, r2.ordered);
                if (!reseut) {
                    r2.curApp = null;
                    Slog.w(TAG, "Unable to launch app " + info.activityInfo.applicationInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiverUid + " for broadcast " + r2.intent + ": AutoStart Limit");
                    logBroadcastReceiverDiscardLocked(r2);
                    finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                    scheduleBroadcastsLocked();
                    r2.state = 0;
                    return;
                }
                r2.curApp = this.mService.startProcessLocked(targetProcess, info.activityInfo.applicationInfo, true, r2.intent.getFlags() | 4, new HostingRecord("broadcast", r2.curComponent, r2.intent.getAction()), isActivityCapable ? i3 : 0, (r2.intent.getFlags() & 33554432) != 0 ? i3 : false, false);
                if (r2.curApp != null) {
                    maybeAddAllowBackgroundActivityStartsToken(r2.curApp, r2);
                    this.mPendingBroadcast = r2;
                    this.mPendingBroadcastRecvIndex = recIdx;
                    return;
                }
                Slog.w(TAG, "Unable to launch app " + info.activityInfo.applicationInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + receiverUid + " for broadcast " + r2.intent + ": process is bad");
                logBroadcastReceiverDiscardLocked(r2);
                finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                scheduleBroadcastsLocked();
                r2.state = 0;
                return;
            }
            i = 1;
            r14 = 0;
        }
    }

    private String getTargetPackage(BroadcastRecord r) {
        if (r.intent == null) {
            return null;
        }
        if (r.intent.getPackage() != null) {
            return r.intent.getPackage();
        }
        if (r.intent.getComponent() != null) {
            return r.intent.getComponent().getPackageName();
        }
        return null;
    }

    private void logBootCompletedBroadcastCompletionLatencyIfPossible(BroadcastRecord r) {
        int userType;
        int numReceivers = r.receivers != null ? r.receivers.size() : 0;
        if (r.nextReceiver < numReceivers) {
            return;
        }
        String action = r.intent.getAction();
        int event = 0;
        if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            event = 1;
        } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            event = 2;
        }
        if (event != 0) {
            int dispatchLatency = (int) (r.dispatchTime - r.enqueueTime);
            int completeLatency = (int) (SystemClock.uptimeMillis() - r.enqueueTime);
            int dispatchRealLatency = (int) (r.dispatchRealTime - r.enqueueRealTime);
            int completeRealLatency = (int) (SystemClock.elapsedRealtime() - r.enqueueRealTime);
            UserManagerInternal umInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            UserInfo userInfo = umInternal.getUserInfo(r.userId);
            if (userInfo == null) {
                userType = 0;
            } else {
                int userType2 = UserManager.getUserTypeForStatsd(userInfo.userType);
                userType = userType2;
            }
            Slog.i(TAG_BROADCAST, "BOOT_COMPLETED_BROADCAST_COMPLETION_LATENCY_REPORTED action:" + action + " dispatchLatency:" + dispatchLatency + " completeLatency:" + completeLatency + " dispatchRealLatency:" + dispatchRealLatency + " completeRealLatency:" + completeRealLatency + " receiversSize:" + numReceivers + " userId:" + r.userId + " userType:" + (userInfo != null ? userInfo.userType : null));
            FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_COMPLETED_BROADCAST_COMPLETION_LATENCY_REPORTED, event, dispatchLatency, completeLatency, dispatchRealLatency, completeRealLatency, r.userId, userType);
        }
    }

    private void maybeReportBroadcastDispatchedEventLocked(BroadcastRecord r, int targetUid) {
        String targetPackage;
        if (r.options == null || r.options.getIdForResponseEvent() <= 0 || (targetPackage = getTargetPackage(r)) == null) {
            return;
        }
        getUsageStatsManagerInternal().reportBroadcastDispatched(r.callingUid, targetPackage, UserHandle.of(r.userId), r.options.getIdForResponseEvent(), SystemClock.elapsedRealtime(), this.mService.getUidStateLocked(targetUid));
    }

    private UsageStatsManagerInternal getUsageStatsManagerInternal() {
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        return usageStatsManagerInternal;
    }

    private boolean noteOpForManifestReceiver(int appOp, BroadcastRecord r, ResolveInfo info, ComponentName component) {
        String[] strArr;
        if (ArrayUtils.isEmpty(info.activityInfo.attributionTags)) {
            return noteOpForManifestReceiverInner(appOp, r, info, component, null);
        }
        for (String tag : info.activityInfo.attributionTags) {
            if (!noteOpForManifestReceiverInner(appOp, r, info, component, tag)) {
                return false;
            }
        }
        return true;
    }

    private boolean noteOpForManifestReceiverInner(int appOp, BroadcastRecord r, ResolveInfo info, ComponentName component, String tag) {
        if (this.mService.getAppOpsManager().noteOpNoThrow(appOp, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName, tag, "Broadcast delivered to " + info.activityInfo.name) != 0) {
            Slog.w(TAG, "Appop Denial: receiving " + r.intent + " to " + component.flattenToShortString() + " requires appop " + AppOpsManager.opToName(appOp) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
            return false;
        }
        return true;
    }

    private void maybeAddAllowBackgroundActivityStartsToken(ProcessRecord proc, BroadcastRecord r) {
        if (r == null || proc == null || !r.allowBackgroundActivityStarts) {
            return;
        }
        String msgToken = (proc.toShortString() + r.toString()).intern();
        this.mHandler.removeCallbacksAndMessages(msgToken);
        proc.addOrUpdateAllowBackgroundActivityStartsToken(r, r.mBackgroundActivityStartsToken);
    }

    final void setBroadcastTimeoutLocked(long timeoutTime) {
        if (!this.mPendingBroadcastTimeoutMessage) {
            Message msg = this.mHandler.obtainMessage(201, this);
            this.mHandler.sendMessageAtTime(msg, timeoutTime);
            this.mPendingBroadcastTimeoutMessage = true;
        }
    }

    final void cancelBroadcastTimeoutLocked() {
        if (this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.removeMessages(201, this);
            this.mPendingBroadcastTimeoutMessage = false;
        }
    }

    final void broadcastTimeoutLocked(boolean fromMsg) {
        Object curReceiver;
        ProcessRecord app;
        String anrMessage;
        boolean debugging = false;
        if (fromMsg) {
            this.mPendingBroadcastTimeoutMessage = false;
        }
        if (this.mDispatcher.isEmpty() || this.mDispatcher.getActiveBroadcastLocked() == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        BroadcastRecord r = this.mDispatcher.getActiveBroadcastLocked();
        if (fromMsg) {
            if (!this.mService.mProcessesReady) {
                return;
            }
            if (r.timeoutExempt) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.i(TAG_BROADCAST, "Broadcast timeout but it's exempt: " + r.intent.getAction());
                    return;
                }
                return;
            }
            long timeoutTime = r.receiverTime + this.mConstants.TIMEOUT;
            if (timeoutTime > now) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Premature timeout [" + this.mQueueName + "] @ " + now + ": resetting BROADCAST_TIMEOUT_MSG for " + timeoutTime);
                }
                setBroadcastTimeoutLocked(timeoutTime);
                return;
            }
        }
        if (r.state == 4) {
            Slog.i(TAG, "Waited long enough for: " + (r.curComponent != null ? r.curComponent.flattenToShortString() : "(null)"));
            r.curComponent = null;
            r.state = 0;
            processNextBroadcastLocked(false, false);
            return;
        }
        if (r.curApp != null && r.curApp.isDebugging()) {
            debugging = true;
        }
        Slog.w(TAG, "Timeout of broadcast " + r + " - receiver=" + r.receiver + ", started " + (now - r.receiverTime) + "ms ago");
        r.receiverTime = now;
        if (!debugging) {
            r.anrCount++;
        }
        ProcessRecord app2 = null;
        if (r.nextReceiver > 0) {
            Object curReceiver2 = r.receivers.get(r.nextReceiver - 1);
            r.delivery[r.nextReceiver - 1] = 3;
            curReceiver = curReceiver2;
        } else {
            Object curReceiver3 = r.curReceiver;
            curReceiver = curReceiver3;
        }
        Slog.w(TAG, "Receiver during timeout of " + r + " : " + curReceiver);
        logBroadcastReceiverDiscardLocked(r);
        if (curReceiver != null && (curReceiver instanceof BroadcastFilter)) {
            BroadcastFilter bf = (BroadcastFilter) curReceiver;
            if (bf.receiverList.pid != 0 && bf.receiverList.pid != ActivityManagerService.MY_PID) {
                synchronized (this.mService.mPidsSelfLocked) {
                    app2 = this.mService.mPidsSelfLocked.get(bf.receiverList.pid);
                }
            }
            app = app2;
        } else {
            app = r.curApp;
        }
        if (app == null) {
            anrMessage = null;
        } else {
            String anrMessage2 = "Broadcast of " + r.intent.toString();
            anrMessage = anrMessage2;
        }
        if (this.mPendingBroadcast == r) {
            this.mPendingBroadcast = null;
        }
        finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
        scheduleBroadcastsLocked();
        if (!debugging && anrMessage != null) {
            this.mService.mAnrHelper.appNotResponding(app, anrMessage);
        }
    }

    private final int ringAdvance(int x, int increment, int ringSize) {
        int x2 = x + increment;
        if (x2 < 0) {
            return ringSize - 1;
        }
        if (x2 >= ringSize) {
            return 0;
        }
        return x2;
    }

    private final void addBroadcastToHistoryLocked(BroadcastRecord original) {
        if (original.callingUid < 0) {
            return;
        }
        original.finishTime = SystemClock.uptimeMillis();
        if (Trace.isTagEnabled(64L)) {
            Trace.asyncTraceEnd(64L, createBroadcastTraceTitle(original, 1), System.identityHashCode(original));
        }
        ApplicationInfo info = original.callerApp != null ? original.callerApp.info : null;
        String callerPackage = info != null ? info.packageName : original.callerPackage;
        if (callerPackage != null) {
            this.mService.mHandler.obtainMessage(74, original.callingUid, 0, callerPackage).sendToTarget();
        }
        BroadcastRecord historyRecord = original.maybeStripForHistory();
        BroadcastRecord[] broadcastRecordArr = this.mBroadcastHistory;
        int i = this.mHistoryNext;
        broadcastRecordArr[i] = historyRecord;
        this.mHistoryNext = ringAdvance(i, 1, MAX_BROADCAST_HISTORY);
        this.mBroadcastSummaryHistory[this.mSummaryHistoryNext] = historyRecord.intent;
        this.mSummaryHistoryEnqueueTime[this.mSummaryHistoryNext] = historyRecord.enqueueClockTime;
        this.mSummaryHistoryDispatchTime[this.mSummaryHistoryNext] = historyRecord.dispatchClockTime;
        this.mSummaryHistoryFinishTime[this.mSummaryHistoryNext] = System.currentTimeMillis();
        this.mSummaryHistoryNext = ringAdvance(this.mSummaryHistoryNext, 1, MAX_BROADCAST_SUMMARY_HISTORY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        boolean didSomething = false;
        for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            didSomething |= this.mParallelBroadcasts.get(i).cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething | this.mDispatcher.cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
    }

    final void logBroadcastReceiverDiscardLocked(BroadcastRecord r) {
        int logIndex = r.nextReceiver - 1;
        if (logIndex >= 0 && logIndex < r.receivers.size()) {
            Object curReceiver = r.receivers.get(logIndex);
            if (curReceiver instanceof BroadcastFilter) {
                BroadcastFilter bf = (BroadcastFilter) curReceiver;
                EventLog.writeEvent((int) EventLogTags.AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(bf.owningUserId), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), Integer.valueOf(System.identityHashCode(bf)));
                return;
            }
            ResolveInfo ri = (ResolveInfo) curReceiver;
            EventLog.writeEvent((int) EventLogTags.AM_BROADCAST_DISCARD_APP, Integer.valueOf(UserHandle.getUserId(ri.activityInfo.applicationInfo.uid)), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), ri.toString());
            return;
        }
        if (logIndex < 0) {
            Slog.w(TAG, "Discarding broadcast before first receiver is invoked: " + r);
        }
        EventLog.writeEvent((int) EventLogTags.AM_BROADCAST_DISCARD_APP, -1, Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver), "NONE");
    }

    private String createBroadcastTraceTitle(BroadcastRecord record, int state) {
        Object[] objArr = new Object[4];
        objArr[0] = state == 0 ? "in queue" : "dispatched";
        objArr[1] = record.callerPackage == null ? "" : record.callerPackage;
        objArr[2] = record.callerApp == null ? "process unknown" : record.callerApp.toShortString();
        objArr[3] = record.intent != null ? record.intent.getAction() : "";
        return TextUtils.formatSimple("Broadcast %s from %s (%s) %s", objArr);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIdle() {
        return this.mParallelBroadcasts.isEmpty() && this.mDispatcher.isIdle() && this.mPendingBroadcast == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelDeferrals() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mDispatcher.cancelDeferralsLocked();
                scheduleBroadcastsLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String describeState() {
        String str;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                str = this.mParallelBroadcasts.size() + " parallel; " + this.mDispatcher.describeStateLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        int i;
        int lastIndex;
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mQueueName);
        int N = this.mParallelBroadcasts.size();
        for (int i2 = N - 1; i2 >= 0; i2--) {
            this.mParallelBroadcasts.get(i2).dumpDebug(proto, 2246267895810L);
        }
        this.mDispatcher.dumpDebug(proto, 2246267895811L);
        BroadcastRecord broadcastRecord = this.mPendingBroadcast;
        if (broadcastRecord != null) {
            broadcastRecord.dumpDebug(proto, 1146756268036L);
        }
        int lastIndex2 = this.mHistoryNext;
        int ringIndex = lastIndex2;
        do {
            i = -1;
            ringIndex = ringAdvance(ringIndex, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord r = this.mBroadcastHistory[ringIndex];
            if (r != null) {
                r.dumpDebug(proto, 2246267895813L);
                continue;
            }
        } while (ringIndex != lastIndex2);
        int i3 = this.mSummaryHistoryNext;
        int ringIndex2 = i3;
        int lastIndex3 = i3;
        while (true) {
            int ringIndex3 = ringAdvance(ringIndex2, i, MAX_BROADCAST_SUMMARY_HISTORY);
            Intent intent = this.mBroadcastSummaryHistory[ringIndex3];
            if (intent == null) {
                lastIndex = lastIndex3;
            } else {
                long summaryToken = proto.start(2246267895814L);
                lastIndex = lastIndex3;
                intent.dumpDebug(proto, 1146756268033L, false, true, true, false);
                proto.write(1112396529666L, this.mSummaryHistoryEnqueueTime[ringIndex3]);
                proto.write(1112396529667L, this.mSummaryHistoryDispatchTime[ringIndex3]);
                proto.write(1112396529668L, this.mSummaryHistoryFinishTime[ringIndex3]);
                proto.end(summaryToken);
            }
            int lastIndex4 = lastIndex;
            if (ringIndex3 != lastIndex4) {
                lastIndex3 = lastIndex4;
                ringIndex2 = ringIndex3;
                i = -1;
            } else {
                proto.end(token);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean needSep) {
        boolean needSep2;
        BroadcastRecord broadcastRecord;
        boolean needSep3;
        String str;
        int lastIndex;
        String str2 = dumpPackage;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String str3 = ":";
        if (this.mParallelBroadcasts.isEmpty() && this.mDispatcher.isEmpty() && this.mPendingBroadcast == null) {
            needSep2 = needSep;
        } else {
            boolean printed = false;
            needSep2 = needSep;
            for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
                BroadcastRecord br = this.mParallelBroadcasts.get(i);
                if (str2 == null || str2.equals(br.callerPackage)) {
                    if (!printed) {
                        if (needSep2) {
                            pw.println();
                        }
                        needSep2 = true;
                        printed = true;
                        pw.println("  Active broadcasts [" + this.mQueueName + "]:");
                    }
                    pw.println("  Active Broadcast " + this.mQueueName + " #" + i + ":");
                    br.dump(pw, "    ", sdf);
                }
            }
            this.mDispatcher.dumpLocked(pw, str2, this.mQueueName, sdf);
            if (str2 == null || ((broadcastRecord = this.mPendingBroadcast) != null && str2.equals(broadcastRecord.callerPackage))) {
                pw.println();
                pw.println("  Pending broadcast [" + this.mQueueName + "]:");
                BroadcastRecord broadcastRecord2 = this.mPendingBroadcast;
                if (broadcastRecord2 != null) {
                    broadcastRecord2.dump(pw, "    ", sdf);
                } else {
                    pw.println("    (null)");
                }
                needSep2 = true;
            }
        }
        this.mConstants.dump(pw);
        boolean printed2 = false;
        int i2 = -1;
        int lastIndex2 = this.mHistoryNext;
        int ringIndex = lastIndex2;
        while (true) {
            int ringIndex2 = ringAdvance(ringIndex, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord r = this.mBroadcastHistory[ringIndex2];
            int lastIndex3 = lastIndex2;
            if (r == null) {
                str = str3;
            } else {
                i2++;
                if (str2 != null && !str2.equals(r.callerPackage)) {
                    str = str3;
                } else {
                    if (!printed2) {
                        if (needSep2) {
                            pw.println();
                        }
                        pw.println("  Historical broadcasts [" + this.mQueueName + "]:");
                        printed2 = true;
                        needSep2 = true;
                    }
                    if (dumpAll) {
                        needSep3 = needSep2;
                        pw.print("  Historical Broadcast " + this.mQueueName + " #");
                        pw.print(i2);
                        pw.println(str3);
                        r.dump(pw, "    ", sdf);
                        str = str3;
                    } else {
                        needSep3 = needSep2;
                        pw.print("  #");
                        pw.print(i2);
                        pw.print(": ");
                        pw.println(r);
                        pw.print("    ");
                        str = str3;
                        pw.println(r.intent.toShortString(false, true, true, false));
                        if (r.targetComp != null && r.targetComp != r.intent.getComponent()) {
                            pw.print("    targetComp: ");
                            pw.println(r.targetComp.toShortString());
                        }
                        Bundle bundle = r.intent.getExtras();
                        if (bundle != null) {
                            pw.print("    extras: ");
                            pw.println(bundle.toString());
                        }
                    }
                    needSep2 = needSep3;
                }
            }
            ringIndex = ringIndex2;
            if (ringIndex == lastIndex3) {
                break;
            }
            str2 = dumpPackage;
            lastIndex2 = lastIndex3;
            str3 = str;
        }
        if (str2 == null) {
            int lastIndex4 = this.mSummaryHistoryNext;
            int ringIndex3 = lastIndex4;
            if (dumpAll) {
                printed2 = false;
                i2 = -1;
            } else {
                int j = i2;
                while (j > 0 && ringIndex3 != lastIndex4) {
                    ringIndex3 = ringAdvance(ringIndex3, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                    if (this.mBroadcastHistory[ringIndex3] != null) {
                        j--;
                    }
                }
            }
            while (true) {
                ringIndex3 = ringAdvance(ringIndex3, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                Intent intent = this.mBroadcastSummaryHistory[ringIndex3];
                if (intent == null) {
                    lastIndex = lastIndex4;
                } else {
                    if (!printed2) {
                        if (needSep2) {
                            pw.println();
                        }
                        needSep2 = true;
                        pw.println("  Historical broadcasts summary [" + this.mQueueName + "]:");
                        printed2 = true;
                    }
                    if (!dumpAll && i2 >= 50) {
                        pw.println("  ...");
                        break;
                    }
                    i2++;
                    pw.print("  #");
                    pw.print(i2);
                    pw.print(": ");
                    boolean needSep4 = needSep2;
                    pw.println(intent.toShortString(false, true, true, false));
                    pw.print("    ");
                    lastIndex = lastIndex4;
                    TimeUtils.formatDuration(this.mSummaryHistoryDispatchTime[ringIndex3] - this.mSummaryHistoryEnqueueTime[ringIndex3], pw);
                    pw.print(" dispatch ");
                    TimeUtils.formatDuration(this.mSummaryHistoryFinishTime[ringIndex3] - this.mSummaryHistoryDispatchTime[ringIndex3], pw);
                    pw.println(" finish");
                    pw.print("    enq=");
                    pw.print(sdf.format(new Date(this.mSummaryHistoryEnqueueTime[ringIndex3])));
                    pw.print(" disp=");
                    pw.print(sdf.format(new Date(this.mSummaryHistoryDispatchTime[ringIndex3])));
                    pw.print(" fin=");
                    pw.println(sdf.format(new Date(this.mSummaryHistoryFinishTime[ringIndex3])));
                    Bundle bundle2 = intent.getExtras();
                    if (bundle2 != null) {
                        pw.print("    extras: ");
                        pw.println(bundle2.toString());
                    }
                    needSep2 = needSep4;
                }
                int lastIndex5 = lastIndex;
                if (ringIndex3 == lastIndex5) {
                    break;
                }
                lastIndex4 = lastIndex5;
            }
        }
        return needSep2;
    }
}
