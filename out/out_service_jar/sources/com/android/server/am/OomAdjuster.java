package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemUtil;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.wm.ActivityServiceConnectionsHolder;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.WindowProcessController;
import com.transsion.hubcore.cpubooster.ITranAnimationBoost;
import com.transsion.hubcore.server.am.ITranOomAdjuster;
import com.transsion.hubcore.server.am.ITranProcessList;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class OomAdjuster {
    static final long CAMERA_MICROPHONE_CAPABILITY_CHANGE_ID = 136219221;
    public static final boolean IS_ROOT_ENABLE = "1".equals(SystemProperties.get("persist.user.root.support", "0"));
    static final String OOM_ADJ_REASON_ACTIVITY = "updateOomAdj_activityChange";
    static final String OOM_ADJ_REASON_ALLOWLIST = "updateOomAdj_allowlistChange";
    static final String OOM_ADJ_REASON_BIND_SERVICE = "updateOomAdj_bindService";
    static final String OOM_ADJ_REASON_FINISH_RECEIVER = "updateOomAdj_finishReceiver";
    static final String OOM_ADJ_REASON_GET_PROVIDER = "updateOomAdj_getProvider";
    static final String OOM_ADJ_REASON_METHOD = "updateOomAdj";
    static final String OOM_ADJ_REASON_NONE = "updateOomAdj_meh";
    static final String OOM_ADJ_REASON_PROCESS_BEGIN = "updateOomAdj_processBegin";
    static final String OOM_ADJ_REASON_PROCESS_END = "updateOomAdj_processEnd";
    static final String OOM_ADJ_REASON_REMOVE_PROVIDER = "updateOomAdj_removeProvider";
    static final String OOM_ADJ_REASON_START_RECEIVER = "updateOomAdj_startReceiver";
    static final String OOM_ADJ_REASON_START_SERVICE = "updateOomAdj_startService";
    static final String OOM_ADJ_REASON_UI_VISIBILITY = "updateOomAdj_uiVisibility";
    static final String OOM_ADJ_REASON_UNBIND_SERVICE = "updateOomAdj_unbindService";
    static final long PROCESS_CAPABILITY_CHANGE_ID = 136274596;
    static final String TAG = "OomAdjuster";
    static final long USE_SHORT_FGS_USAGE_INTERACTION_TIME = 183972877;
    ActiveUids mActiveUids;
    int mAdjSeq;
    CacheOomRanker mCacheOomRanker;
    CachedAppOptimizer mCachedAppOptimizer;
    ActivityManagerConstants mConstants;
    PowerManagerInternal mLocalPowerManager;
    int mNewNumAServiceProcs;
    int mNewNumServiceProcs;
    private long mNextNoKillDebugMessageTime;
    int mNumCachedHiddenProcs;
    int mNumNonCachedProcs;
    int mNumServiceProcs;
    private final int mNumSlots;
    private boolean mOomAdjUpdateOngoing;
    private boolean mPendingFullOomAdjUpdate;
    private final ArraySet<ProcessRecord> mPendingProcessSet;
    private final ActivityManagerGlobalLock mProcLock;
    private final Handler mProcessGroupHandler;
    private final ProcessList mProcessList;
    private final ArraySet<ProcessRecord> mProcessesInCycle;
    private final ActivityManagerService mService;
    SystemUtil mStl;
    private final ArrayList<UidRecord> mTmpBecameIdle;
    private final ArraySet<BroadcastQueue> mTmpBroadcastQueue;
    private final ComputeOomAdjWindowCallback mTmpComputeOomAdjWindowCallback;
    final long[] mTmpLong;
    private final ArrayList<ProcessRecord> mTmpProcessList;
    private final ArrayDeque<ProcessRecord> mTmpQueue;
    private final ActiveUids mTmpUidRecords;

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isChangeEnabled(int cachedCompatChangeId, ApplicationInfo app, boolean defaultValue) {
        PlatformCompatCache.getInstance();
        return PlatformCompatCache.isChangeEnabled(cachedCompatChangeId, app, defaultValue);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OomAdjuster(ActivityManagerService service, ProcessList processList, ActiveUids activeUids) {
        this(service, processList, activeUids, createAdjusterThread());
    }

    private static ServiceThread createAdjusterThread() {
        ServiceThread adjusterThread = new ServiceThread(TAG, -10, false);
        adjusterThread.start();
        return adjusterThread;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OomAdjuster(ActivityManagerService service, ProcessList processList, ActiveUids activeUids, ServiceThread adjusterThread) {
        this.mTmpLong = new long[3];
        this.mAdjSeq = 0;
        this.mNumServiceProcs = 0;
        this.mNewNumAServiceProcs = 0;
        this.mNewNumServiceProcs = 0;
        this.mNumNonCachedProcs = 0;
        this.mNumCachedHiddenProcs = 0;
        this.mTmpBroadcastQueue = new ArraySet<>();
        this.mTmpProcessList = new ArrayList<>();
        this.mTmpBecameIdle = new ArrayList<>();
        this.mPendingProcessSet = new ArraySet<>();
        this.mProcessesInCycle = new ArraySet<>();
        this.mOomAdjUpdateOngoing = false;
        this.mPendingFullOomAdjUpdate = false;
        this.mTmpComputeOomAdjWindowCallback = new ComputeOomAdjWindowCallback();
        this.mService = service;
        this.mProcessList = processList;
        this.mProcLock = service.mProcLock;
        this.mActiveUids = activeUids;
        this.mLocalPowerManager = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mConstants = service.mConstants;
        this.mCachedAppOptimizer = new CachedAppOptimizer(service);
        this.mCacheOomRanker = new CacheOomRanker(service);
        this.mProcessGroupHandler = new Handler(adjusterThread.getLooper(), new Handler.Callback() { // from class: com.android.server.am.OomAdjuster$$ExternalSyntheticLambda1
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return OomAdjuster.lambda$new$0(message);
            }
        });
        this.mTmpUidRecords = new ActiveUids(service, false);
        this.mTmpQueue = new ArrayDeque<>(this.mConstants.CUR_MAX_CACHED_PROCESSES << 1);
        this.mNumSlots = 10;
        if (IS_ROOT_ENABLE) {
            HandlerThread handlerThread = new HandlerThread("KillProcessHandlerOomAdjuster");
            handlerThread.start();
            this.mStl = new SystemUtil(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$0(Message msg) {
        int pid = msg.arg1;
        int group = msg.arg2;
        if (pid == ActivityManagerService.MY_PID) {
            return true;
        }
        if (Trace.isTagEnabled(64L)) {
            Trace.traceBegin(64L, "setProcessGroup " + msg.obj + " to " + group);
        }
        try {
            try {
                Process.setProcessGroup(pid, group);
                if (ITranAnimationBoost.Instance().isLauncherApp(String.valueOf(msg.obj)) && ITranAnimationBoost.Instance().getAnimBoostStatu()) {
                    ITranAnimationBoost.Instance().setLaunchAnimGroup();
                }
            } catch (Exception e) {
                if (ActivityManagerDebugConfig.DEBUG_ALL) {
                    Slog.w(TAG, "Failed setting process group of " + pid + " to " + group, e);
                }
            }
            return true;
        } finally {
            Trace.traceEnd(64L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initSettings() {
        this.mCachedAppOptimizer.init();
        this.mCacheOomRanker.init(ActivityThread.currentApplication().getMainExecutor());
        if (this.mService.mConstants.KEEP_WARMING_SERVICES.size() > 0) {
            IntentFilter filter = new IntentFilter("android.intent.action.USER_SWITCHED");
            this.mService.mContext.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.am.OomAdjuster.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    synchronized (OomAdjuster.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            OomAdjuster.this.handleUserSwitchedLocked();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }, filter, null, this.mService.mHandler);
        }
    }

    void handleUserSwitchedLocked() {
        this.mProcessList.forEachLruProcessesLOSP(false, new Consumer() { // from class: com.android.server.am.OomAdjuster$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                OomAdjuster.this.updateKeepWarmIfNecessaryForProcessLocked((ProcessRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateKeepWarmIfNecessaryForProcessLocked(ProcessRecord app) {
        ArraySet<ComponentName> warmServices = this.mService.mConstants.KEEP_WARMING_SERVICES;
        boolean includeWarmPkg = false;
        PackageList pkgList = app.getPkgList();
        int j = warmServices.size() - 1;
        while (true) {
            if (j >= 0) {
                if (pkgList.containsKey(warmServices.valueAt(j).getPackageName())) {
                    includeWarmPkg = true;
                    break;
                } else {
                    j--;
                }
            } else {
                break;
            }
        }
        if (!includeWarmPkg) {
            return;
        }
        ProcessServiceRecord psr = app.mServices;
        for (int j2 = psr.numberOfRunningServices() - 1; j2 >= 0; j2--) {
            psr.getRunningServiceAt(j2).updateKeepWarmLocked();
        }
    }

    private boolean performUpdateOomAdjLSP(ProcessRecord app, int cachedAdj, ProcessRecord topApp, long now) {
        if (app.getThread() == null) {
            return false;
        }
        app.mState.resetCachedInfo();
        app.mState.setCurBoundByNonBgRestrictedApp(false);
        UidRecord uidRec = app.getUidRecord();
        if (uidRec != null) {
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Starting update of " + uidRec);
            }
            uidRec.reset();
        }
        this.mPendingProcessSet.remove(app);
        this.mProcessesInCycle.clear();
        computeOomAdjLSP(app, cachedAdj, topApp, false, now, false, true);
        if (!this.mProcessesInCycle.isEmpty()) {
            for (int i = this.mProcessesInCycle.size() - 1; i >= 0; i--) {
                this.mProcessesInCycle.valueAt(i).mState.setCompletedAdjSeq(this.mAdjSeq - 1);
            }
            return true;
        }
        if (uidRec != null) {
            uidRec.forEachProcess(new Consumer() { // from class: com.android.server.am.OomAdjuster$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    OomAdjuster.this.updateAppUidRecIfNecessaryLSP((ProcessRecord) obj);
                }
            });
            if (uidRec.getCurProcState() != 20 && (uidRec.getSetProcState() != uidRec.getCurProcState() || uidRec.getSetCapability() != uidRec.getCurCapability() || uidRec.isSetAllowListed() != uidRec.isCurAllowListed())) {
                ActiveUids uids = this.mTmpUidRecords;
                uids.clear();
                uids.put(uidRec.getUid(), uidRec);
                updateUidsLSP(uids, SystemClock.elapsedRealtime());
            }
        }
        return applyOomAdjLSP(app, false, now, SystemClock.elapsedRealtime());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateOomAdjLocked(String oomAdjReason) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                updateOomAdjLSP(oomAdjReason);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    private void updateOomAdjLSP(String oomAdjReason) {
        if (checkAndEnqueueOomAdjTargetLocked(null)) {
            return;
        }
        try {
            this.mOomAdjUpdateOngoing = true;
            performUpdateOomAdjLSP(oomAdjReason);
        } finally {
            this.mOomAdjUpdateOngoing = false;
            updateOomAdjPendingTargetsLocked(oomAdjReason);
        }
    }

    private void performUpdateOomAdjLSP(String oomAdjReason) {
        ProcessRecord topApp = this.mService.getTopApp();
        this.mPendingProcessSet.clear();
        AppProfiler appProfiler = this.mService.mAppProfiler;
        this.mService.mAppProfiler.mHasHomeProcess = false;
        appProfiler.mHasPreviousProcess = false;
        updateOomAdjInnerLSP(oomAdjReason, topApp, null, null, true, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateOomAdjLocked(ProcessRecord app, String oomAdjReason) {
        boolean updateOomAdjLSP;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                updateOomAdjLSP = updateOomAdjLSP(app, oomAdjReason);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        return updateOomAdjLSP;
    }

    private boolean updateOomAdjLSP(ProcessRecord app, String oomAdjReason) {
        if (app == null || !this.mConstants.OOMADJ_UPDATE_QUICK) {
            updateOomAdjLSP(oomAdjReason);
            return true;
        } else if (checkAndEnqueueOomAdjTargetLocked(app)) {
            return true;
        } else {
            try {
                this.mOomAdjUpdateOngoing = true;
                return performUpdateOomAdjLSP(app, oomAdjReason);
            } finally {
                this.mOomAdjUpdateOngoing = false;
                updateOomAdjPendingTargetsLocked(oomAdjReason);
            }
        }
    }

    private boolean performUpdateOomAdjLSP(ProcessRecord app, String oomAdjReason) {
        ProcessRecord topApp = this.mService.getTopApp();
        Trace.traceBegin(64L, oomAdjReason);
        this.mService.mOomAdjProfiler.oomAdjStarted();
        this.mAdjSeq++;
        ProcessStateRecord state = app.mState;
        boolean wasCached = state.isCached();
        int oldAdj = state.getCurRawAdj();
        int cachedAdj = oldAdj >= 900 ? oldAdj : 1001;
        boolean wasBackground = ActivityManager.isProcStateBackground(state.getSetProcState());
        int oldCap = state.getSetCapability();
        state.setContainsCycle(false);
        state.setProcStateChanged(false);
        state.resetCachedInfo();
        state.setCurBoundByNonBgRestrictedApp(false);
        this.mPendingProcessSet.remove(app);
        boolean success = performUpdateOomAdjLSP(app, cachedAdj, topApp, SystemClock.uptimeMillis());
        if (success && (wasCached != state.isCached() || oldAdj == -10000 || !this.mProcessesInCycle.isEmpty() || oldCap != state.getCurCapability() || wasBackground != ActivityManager.isProcStateBackground(state.getSetProcState()))) {
            ArrayList<ProcessRecord> processes = this.mTmpProcessList;
            ActiveUids uids = this.mTmpUidRecords;
            this.mPendingProcessSet.add(app);
            for (int i = this.mProcessesInCycle.size() - 1; i >= 0; i--) {
                this.mPendingProcessSet.add(this.mProcessesInCycle.valueAt(i));
            }
            this.mProcessesInCycle.clear();
            boolean containsCycle = collectReachableProcessesLocked(this.mPendingProcessSet, processes, uids);
            this.mPendingProcessSet.clear();
            if (!containsCycle) {
                state.setReachable(false);
                processes.remove(app);
            }
            int size = processes.size();
            if (size > 0) {
                this.mAdjSeq--;
                updateOomAdjInnerLSP(oomAdjReason, topApp, processes, uids, containsCycle, false);
            } else if (state.getCurRawAdj() == 1001) {
                processes.add(app);
                assignCachedAdjIfNecessary(processes);
                applyOomAdjLSP(app, false, SystemClock.uptimeMillis(), SystemClock.elapsedRealtime());
            }
            this.mTmpProcessList.clear();
            this.mService.mOomAdjProfiler.oomAdjEnded();
            Trace.traceEnd(64L);
            return true;
        }
        this.mProcessesInCycle.clear();
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.i(ActivityManagerService.TAG_OOM_ADJ, "No oomadj changes for " + app);
        }
        this.mService.mOomAdjProfiler.oomAdjEnded();
        Trace.traceEnd(64L);
        return success;
    }

    private boolean collectReachableProcessesLocked(ArraySet<ProcessRecord> apps, ArrayList<ProcessRecord> processes, ActiveUids uids) {
        ArrayDeque<ProcessRecord> queue = this.mTmpQueue;
        queue.clear();
        processes.clear();
        int size = apps.size();
        for (int i = 0; i < size; i++) {
            ProcessRecord app = apps.valueAt(i);
            app.mState.setReachable(true);
            queue.offer(app);
        }
        uids.clear();
        boolean containsCycle = false;
        ProcessRecord pr = queue.poll();
        while (pr != null) {
            processes.add(pr);
            UidRecord uidRec = pr.getUidRecord();
            if (uidRec != null) {
                uids.put(uidRec.getUid(), uidRec);
            }
            ProcessServiceRecord psr = pr.mServices;
            for (int i2 = psr.numberOfConnections() - 1; i2 >= 0; i2--) {
                ConnectionRecord cr = psr.getConnectionAt(i2);
                ProcessRecord service = (cr.flags & 2) != 0 ? cr.binding.service.isolationHostProc : cr.binding.service.app;
                if (service != null && service != pr) {
                    containsCycle |= service.mState.isReachable();
                    if (!service.mState.isReachable() && (cr.flags & 134217888) != 32) {
                        queue.offer(service);
                        service.mState.setReachable(true);
                    }
                }
            }
            ProcessProviderRecord ppr = pr.mProviders;
            for (int i3 = ppr.numberOfProviderConnections() - 1; i3 >= 0; i3--) {
                ContentProviderConnection cpc = ppr.getProviderConnectionAt(i3);
                ProcessRecord provider = cpc.provider.proc;
                if (provider != null && provider != pr) {
                    containsCycle |= provider.mState.isReachable();
                    if (!provider.mState.isReachable()) {
                        queue.offer(provider);
                        provider.mState.setReachable(true);
                    }
                }
            }
            ProcessRecord pr2 = queue.poll();
            pr = pr2;
        }
        int size2 = processes.size();
        if (size2 > 0) {
            int l = 0;
            for (int r = size2 - 1; l < r; r--) {
                ProcessRecord t = processes.get(l);
                processes.set(l, processes.get(r));
                processes.set(r, t);
                l++;
            }
        }
        return containsCycle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enqueueOomAdjTargetLocked(ProcessRecord app) {
        if (app != null) {
            this.mPendingProcessSet.add(app);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeOomAdjTargetLocked(ProcessRecord app, boolean procDied) {
        if (app != null) {
            this.mPendingProcessSet.remove(app);
            if (procDied) {
                PlatformCompatCache.getInstance().invalidate(app.info);
            }
        }
    }

    private boolean checkAndEnqueueOomAdjTargetLocked(ProcessRecord app) {
        if (!this.mOomAdjUpdateOngoing) {
            return false;
        }
        if (app != null) {
            this.mPendingProcessSet.add(app);
        } else {
            this.mPendingFullOomAdjUpdate = true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateOomAdjPendingTargetsLocked(String oomAdjReason) {
        if (this.mPendingFullOomAdjUpdate) {
            this.mPendingFullOomAdjUpdate = false;
            this.mPendingProcessSet.clear();
            updateOomAdjLocked(oomAdjReason);
        } else if (this.mPendingProcessSet.isEmpty() || this.mOomAdjUpdateOngoing) {
        } else {
            try {
                this.mOomAdjUpdateOngoing = true;
                performUpdateOomAdjPendingTargetsLocked(oomAdjReason);
            } finally {
                this.mOomAdjUpdateOngoing = false;
                updateOomAdjPendingTargetsLocked(oomAdjReason);
            }
        }
    }

    private void performUpdateOomAdjPendingTargetsLocked(String oomAdjReason) {
        ProcessRecord topApp = this.mService.getTopApp();
        Trace.traceBegin(64L, oomAdjReason);
        this.mService.mOomAdjProfiler.oomAdjStarted();
        ArrayList<ProcessRecord> processes = this.mTmpProcessList;
        ActiveUids uids = this.mTmpUidRecords;
        collectReachableProcessesLocked(this.mPendingProcessSet, processes, uids);
        this.mPendingProcessSet.clear();
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                updateOomAdjInnerLSP(oomAdjReason, topApp, processes, uids, true, false);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        processes.clear();
        this.mService.mOomAdjProfiler.oomAdjEnded();
        Trace.traceEnd(64L);
    }

    private void updateOomAdjInnerLSP(String oomAdjReason, ProcessRecord topApp, ArrayList<ProcessRecord> processes, ActiveUids uids, boolean potentialCycles, boolean startProfiling) {
        ArrayList<ProcessRecord> arrayList;
        ActiveUids activeUids;
        int numProc;
        int i;
        ArrayList<ProcessRecord> activeProcesses;
        int numProc2;
        int numProc3;
        ArrayList<ProcessRecord> activeProcesses2;
        long nowElapsed;
        if (startProfiling) {
            Trace.traceBegin(64L, oomAdjReason);
            this.mService.mOomAdjProfiler.oomAdjStarted();
        }
        long now = SystemClock.uptimeMillis();
        long nowElapsed2 = SystemClock.elapsedRealtime();
        long oldTime = now - this.mConstants.mMaxEmptyTimeMillis;
        boolean z = true;
        boolean z2 = false;
        boolean fullUpdate = processes == null;
        if (fullUpdate) {
            arrayList = this.mProcessList.getLruProcessesLOSP();
        } else {
            arrayList = processes;
        }
        ArrayList<ProcessRecord> activeProcesses3 = arrayList;
        int numProc4 = activeProcesses3.size();
        if (uids != null) {
            activeUids = uids;
        } else {
            int numUids = this.mActiveUids.size();
            ActiveUids activeUids2 = this.mTmpUidRecords;
            activeUids2.clear();
            for (int i2 = 0; i2 < numUids; i2++) {
                UidRecord uidRec = this.mActiveUids.valueAt(i2);
                activeUids2.put(uidRec.getUid(), uidRec);
            }
            activeUids = activeUids2;
        }
        for (int i3 = activeUids.size() - 1; i3 >= 0; i3--) {
            UidRecord uidRec2 = activeUids.valueAt(i3);
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Starting update of " + uidRec2);
            }
            uidRec2.reset();
        }
        int i4 = this.mAdjSeq;
        this.mAdjSeq = i4 + 1;
        if (fullUpdate) {
            this.mNewNumServiceProcs = 0;
            this.mNewNumAServiceProcs = 0;
        }
        if (!fullUpdate && !potentialCycles) {
            z = false;
        }
        boolean computeClients = z;
        int i5 = numProc4 - 1;
        while (i5 >= 0) {
            ProcessStateRecord state = activeProcesses3.get(i5).mState;
            state.setReachable(z2);
            if (state.getAdjSeq() != this.mAdjSeq) {
                state.setContainsCycle(false);
                state.setCurRawProcState(19);
                state.setCurRawAdj(1001);
                state.setSetCapability(0);
                state.resetCachedInfo();
                state.setCurBoundByNonBgRestrictedApp(false);
            }
            i5--;
            z2 = false;
        }
        this.mProcessesInCycle.clear();
        int i6 = numProc4 - 1;
        boolean retryCycles = false;
        while (i6 >= 0) {
            ProcessRecord app = activeProcesses3.get(i6);
            ProcessStateRecord state2 = app.mState;
            if (app.isKilledByAm() || app.getThread() == null) {
                numProc3 = numProc4;
                activeProcesses2 = activeProcesses3;
                nowElapsed = nowElapsed2;
            } else {
                state2.setProcStateChanged(false);
                numProc3 = numProc4;
                activeProcesses2 = activeProcesses3;
                nowElapsed = nowElapsed2;
                computeOomAdjLSP(app, 1001, topApp, fullUpdate, now, false, computeClients);
                state2.setCompletedAdjSeq(this.mAdjSeq);
                retryCycles |= state2.containsCycle();
            }
            i6--;
            numProc4 = numProc3;
            activeProcesses3 = activeProcesses2;
            nowElapsed2 = nowElapsed;
        }
        int numProc5 = numProc4;
        ArrayList<ProcessRecord> activeProcesses4 = activeProcesses3;
        long nowElapsed3 = nowElapsed2;
        if (this.mCacheOomRanker.useOomReranking()) {
            this.mCacheOomRanker.reRankLruCachedAppsLSP(this.mProcessList.getLruProcessesLSP(), this.mProcessList.getLruProcessServiceStartLOSP());
        }
        assignCachedAdjIfNecessary(this.mProcessList.getLruProcessesLOSP());
        if (computeClients) {
            int cycleCount = 0;
            while (retryCycles && cycleCount < 10) {
                cycleCount++;
                int i7 = 0;
                while (true) {
                    numProc = numProc5;
                    if (i7 >= numProc) {
                        break;
                    }
                    ArrayList<ProcessRecord> activeProcesses5 = activeProcesses4;
                    ProcessRecord app2 = activeProcesses5.get(i7);
                    ProcessStateRecord state3 = app2.mState;
                    if (!app2.isKilledByAm() && app2.getThread() != null && state3.containsCycle()) {
                        state3.decAdjSeq();
                        state3.decCompletedAdjSeq();
                    }
                    i7++;
                    activeProcesses4 = activeProcesses5;
                    numProc5 = numProc;
                }
                ArrayList<ProcessRecord> activeProcesses6 = activeProcesses4;
                retryCycles = false;
                int i8 = 0;
                while (i8 < numProc) {
                    ProcessRecord app3 = activeProcesses6.get(i8);
                    ProcessStateRecord state4 = app3.mState;
                    if (app3.isKilledByAm() || app3.getThread() == null || !state4.containsCycle()) {
                        i = i8;
                        activeProcesses = activeProcesses6;
                        numProc2 = numProc;
                    } else {
                        i = i8;
                        activeProcesses = activeProcesses6;
                        numProc2 = numProc;
                        if (computeOomAdjLSP(app3, state4.getCurRawAdj(), topApp, true, now, true, true)) {
                            retryCycles = true;
                        }
                    }
                    i8 = i + 1;
                    numProc = numProc2;
                    activeProcesses6 = activeProcesses;
                }
                numProc5 = numProc;
                activeProcesses4 = activeProcesses6;
            }
        }
        this.mProcessesInCycle.clear();
        this.mNumNonCachedProcs = 0;
        this.mNumCachedHiddenProcs = 0;
        boolean allChanged = updateAndTrimProcessLSP(now, nowElapsed3, oldTime, activeUids);
        this.mNumServiceProcs = this.mNewNumServiceProcs;
        if (this.mService.mAlwaysFinishActivities) {
            this.mService.mAtmInternal.scheduleDestroyAllActivities("always-finish");
        }
        if (allChanged) {
            this.mService.mAppProfiler.requestPssAllProcsLPr(now, false, this.mService.mProcessStats.isMemFactorLowered());
        }
        updateUidsLSP(activeUids, nowElapsed3);
        synchronized (this.mService.mProcessStats.mLock) {
            long nowUptime = SystemClock.uptimeMillis();
            if (this.mService.mProcessStats.shouldWriteNowLocked(nowUptime)) {
                ActivityManagerService.MainHandler mainHandler = this.mService.mHandler;
                ActivityManagerService activityManagerService = this.mService;
                mainHandler.post(new ActivityManagerService.ProcStatsRunnable(activityManagerService, activityManagerService.mProcessStats));
            }
            this.mService.mProcessStats.updateTrackingAssociationsLocked(this.mAdjSeq, nowUptime);
        }
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            long duration = SystemClock.uptimeMillis() - now;
            Slog.d(ActivityManagerService.TAG_OOM_ADJ, "Did OOM ADJ in " + duration + "ms");
        }
        if (startProfiling) {
            this.mService.mOomAdjProfiler.oomAdjEnded();
            Trace.traceEnd(64L);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [979=4] */
    private void assignCachedAdjIfNecessary(ArrayList<ProcessRecord> lruList) {
        int lastCachedGroupUid;
        int curCachedAdj;
        int nextCachedAdj;
        int numEmptyProcs;
        int stepEmpty;
        int emptyFactor;
        int stepEmpty2;
        int emptyFactor2;
        String str;
        int lastCachedGroupUid2;
        int nextCachedAdj2;
        int lastCachedGroupUid3;
        int curCachedAdj2;
        int curCachedAdj3;
        int stepEmpty3;
        int nextCachedAdj3;
        int numLru = lruList.size();
        int curCachedAdj4 = 900;
        int nextCachedAdj4 = 900 + 10;
        int curCachedImpAdj = 0;
        int curEmptyAdj = 905;
        int nextEmptyAdj = 905 + 10;
        int emptyProcessLimit = this.mConstants.CUR_MAX_EMPTY_PROCESSES;
        int cachedProcessLimit = this.mConstants.CUR_MAX_CACHED_PROCESSES - emptyProcessLimit;
        int i = this.mNumCachedHiddenProcs;
        int numEmptyProcs2 = (numLru - this.mNumNonCachedProcs) - i;
        if (numEmptyProcs2 > cachedProcessLimit) {
            numEmptyProcs2 = cachedProcessLimit;
        }
        int i2 = i > 0 ? (i + this.mNumSlots) - 1 : 1;
        int i3 = this.mNumSlots;
        int cachedFactor = i2 / i3;
        if (cachedFactor < 1) {
            cachedFactor = 1;
        }
        int emptyFactor3 = ((numEmptyProcs2 + i3) - 1) / i3;
        if (emptyFactor3 < 1) {
            emptyFactor3 = 1;
        }
        int stepCached = -1;
        int curEmptyAdj2 = -1;
        int lastCachedGroup = 0;
        int lastCachedGroupImportance = 0;
        int lastCachedGroupUid4 = 0;
        int numLru2 = numLru - 1;
        while (numLru2 >= 0) {
            int emptyProcessLimit2 = emptyProcessLimit;
            ProcessRecord app = lruList.get(numLru2);
            int cachedProcessLimit2 = cachedProcessLimit;
            ProcessStateRecord state = app.mState;
            if (app.isKilledByAm() || app.getThread() == null) {
                lastCachedGroupUid = lastCachedGroupUid4;
                curCachedAdj = curCachedAdj4;
                nextCachedAdj = nextCachedAdj4;
                numEmptyProcs = numEmptyProcs2;
                stepEmpty = curEmptyAdj2;
                emptyFactor = emptyFactor3;
                stepEmpty2 = curEmptyAdj;
                emptyFactor2 = nextEmptyAdj;
            } else {
                numEmptyProcs = numEmptyProcs2;
                int emptyFactor4 = emptyFactor3;
                if (state.getCurAdj() >= 1001) {
                    ProcessServiceRecord psr = app.mServices;
                    int stepEmpty4 = curEmptyAdj2;
                    int curEmptyAdj3 = curEmptyAdj;
                    int nextEmptyAdj2 = nextEmptyAdj;
                    switch (state.getCurProcState()) {
                        case 16:
                        case 17:
                        case 18:
                            int connectionImportance = 0;
                            int connectionGroup = psr.getConnectionGroup();
                            if (connectionGroup != 0) {
                                int connectionImportance2 = psr.getConnectionImportance();
                                str = ")";
                                if (lastCachedGroupUid4 == app.uid && lastCachedGroup == connectionGroup) {
                                    if (connectionImportance2 > lastCachedGroupImportance) {
                                        if (curCachedAdj4 >= nextCachedAdj4 || curCachedAdj4 >= 999) {
                                            lastCachedGroupImportance = connectionImportance2;
                                        } else {
                                            curCachedImpAdj++;
                                            lastCachedGroupImportance = connectionImportance2;
                                        }
                                    }
                                    connectionImportance = 1;
                                } else {
                                    lastCachedGroupUid4 = app.uid;
                                    lastCachedGroup = connectionGroup;
                                    lastCachedGroupImportance = connectionImportance2;
                                    connectionImportance = 0;
                                }
                            } else {
                                str = ")";
                            }
                            if (connectionImportance == 0 && curCachedAdj4 != nextCachedAdj4) {
                                stepCached++;
                                curCachedImpAdj = 0;
                                if (stepCached >= cachedFactor) {
                                    stepCached = 0;
                                    curCachedAdj4 = nextCachedAdj4;
                                    nextCachedAdj4 += 10;
                                    if (nextCachedAdj4 > 999) {
                                        nextCachedAdj4 = 999;
                                    }
                                }
                            }
                            state.setCurRawAdj(curCachedAdj4 + curCachedImpAdj);
                            state.setCurAdj(psr.modifyRawOomAdj(curCachedAdj4 + curCachedImpAdj));
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                lastCachedGroupUid2 = lastCachedGroupUid4;
                                nextCachedAdj2 = nextCachedAdj4;
                                Slog.d(ActivityManagerService.TAG_LRU, "Assigning activity LRU #" + numLru2 + " adj: " + state.getCurAdj() + " (curCachedAdj=" + curCachedAdj4 + " curCachedImpAdj=" + curCachedImpAdj + str);
                            } else {
                                lastCachedGroupUid2 = lastCachedGroupUid4;
                                nextCachedAdj2 = nextCachedAdj4;
                            }
                            curEmptyAdj2 = stepEmpty4;
                            curEmptyAdj = curEmptyAdj3;
                            nextEmptyAdj = nextEmptyAdj2;
                            lastCachedGroupUid4 = lastCachedGroupUid2;
                            nextCachedAdj4 = nextCachedAdj2;
                            emptyFactor = emptyFactor4;
                            continue;
                        default:
                            int curEmptyAdj4 = curEmptyAdj3;
                            int nextEmptyAdj3 = nextEmptyAdj2;
                            if (curEmptyAdj4 != nextEmptyAdj3) {
                                lastCachedGroupUid3 = lastCachedGroupUid4;
                                int lastCachedGroupUid5 = stepEmpty4 + 1;
                                curCachedAdj2 = curCachedAdj4;
                                curCachedAdj3 = emptyFactor4;
                                if (lastCachedGroupUid5 >= curCachedAdj3) {
                                    curEmptyAdj4 = nextEmptyAdj3;
                                    nextEmptyAdj3 += 10;
                                    stepEmpty3 = 0;
                                    if (nextEmptyAdj3 > 999) {
                                        nextEmptyAdj3 = 999;
                                    }
                                } else {
                                    stepEmpty3 = lastCachedGroupUid5;
                                }
                            } else {
                                lastCachedGroupUid3 = lastCachedGroupUid4;
                                curCachedAdj2 = curCachedAdj4;
                                curCachedAdj3 = emptyFactor4;
                                stepEmpty3 = stepEmpty4;
                            }
                            state.setCurRawAdj(curEmptyAdj4);
                            state.setCurAdj(psr.modifyRawOomAdj(curEmptyAdj4));
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                emptyFactor = curCachedAdj3;
                                nextCachedAdj3 = nextCachedAdj4;
                                Slog.d(ActivityManagerService.TAG_LRU, "Assigning empty LRU #" + numLru2 + " adj: " + state.getCurAdj() + " (curEmptyAdj=" + curEmptyAdj4 + ")");
                            } else {
                                emptyFactor = curCachedAdj3;
                                nextCachedAdj3 = nextCachedAdj4;
                            }
                            curEmptyAdj = curEmptyAdj4;
                            nextEmptyAdj = nextEmptyAdj3;
                            curEmptyAdj2 = stepEmpty3;
                            lastCachedGroupUid4 = lastCachedGroupUid3;
                            curCachedAdj4 = curCachedAdj2;
                            nextCachedAdj4 = nextCachedAdj3;
                            continue;
                    }
                    numLru2--;
                    emptyProcessLimit = emptyProcessLimit2;
                    cachedProcessLimit = cachedProcessLimit2;
                    numEmptyProcs2 = numEmptyProcs;
                    emptyFactor3 = emptyFactor;
                } else {
                    lastCachedGroupUid = lastCachedGroupUid4;
                    curCachedAdj = curCachedAdj4;
                    nextCachedAdj = nextCachedAdj4;
                    emptyFactor2 = nextEmptyAdj;
                    stepEmpty = curEmptyAdj2;
                    emptyFactor = emptyFactor4;
                    stepEmpty2 = curEmptyAdj;
                }
            }
            curEmptyAdj = stepEmpty2;
            nextEmptyAdj = emptyFactor2;
            curEmptyAdj2 = stepEmpty;
            lastCachedGroupUid4 = lastCachedGroupUid;
            curCachedAdj4 = curCachedAdj;
            nextCachedAdj4 = nextCachedAdj;
            numLru2--;
            emptyProcessLimit = emptyProcessLimit2;
            cachedProcessLimit = cachedProcessLimit2;
            numEmptyProcs2 = numEmptyProcs;
            emptyFactor3 = emptyFactor;
        }
    }

    private boolean updateAndTrimProcessLSP(long now, long nowElapsed, long oldTime, ActiveUids activeUids) {
        int i;
        int i2;
        ArrayList<ProcessRecord> lruList;
        int numLru;
        ProcessStateRecord state;
        ProcessRecord app;
        int numEmpty;
        int numLru2;
        ArrayList<ProcessRecord> lruList2 = this.mProcessList.getLruProcessesLOSP();
        int numLru3 = lruList2.size();
        boolean doKillExcessiveProcesses = shouldKillExcessiveProcesses(now);
        if (!doKillExcessiveProcesses && this.mNextNoKillDebugMessageTime < now) {
            Slog.d(TAG, "Not killing cached processes");
            this.mNextNoKillDebugMessageTime = now + 5000;
        }
        int i3 = Integer.MAX_VALUE;
        if (!doKillExcessiveProcesses) {
            i = Integer.MAX_VALUE;
        } else {
            i = this.mConstants.CUR_MAX_EMPTY_PROCESSES;
        }
        int emptyProcessLimit = i;
        if (doKillExcessiveProcesses) {
            i3 = this.mConstants.CUR_MAX_CACHED_PROCESSES - emptyProcessLimit;
        }
        int cachedProcessLimit = i3;
        int lastCachedGroup = 0;
        int lastCachedGroupUid = 0;
        int numCached = 0;
        int numCachedExtraGroup = 0;
        int numCachedExtraGroup2 = numLru3 - 1;
        int numTrimming = 0;
        int numEmpty2 = 0;
        while (numCachedExtraGroup2 >= 0) {
            ProcessRecord app2 = lruList2.get(numCachedExtraGroup2);
            ProcessStateRecord state2 = app2.mState;
            if (!app2.isKilledByAm() && app2.getThread() != null) {
                if (state2.getCompletedAdjSeq() != this.mAdjSeq) {
                    state = state2;
                    i2 = numCachedExtraGroup2;
                    app = app2;
                    lruList = lruList2;
                    numLru = numLru3;
                    numEmpty = numEmpty2;
                    numLru2 = numTrimming;
                } else {
                    state = state2;
                    i2 = numCachedExtraGroup2;
                    app = app2;
                    lruList = lruList2;
                    numLru = numLru3;
                    numEmpty = numEmpty2;
                    numLru2 = numTrimming;
                    applyOomAdjLSP(app2, true, now, nowElapsed);
                }
                ProcessRecord app3 = app;
                ProcessServiceRecord psr = app3.mServices;
                switch (state.getCurProcState()) {
                    case 16:
                    case 17:
                        this.mNumCachedHiddenProcs++;
                        numCached++;
                        int connectionGroup = psr.getConnectionGroup();
                        if (connectionGroup != 0) {
                            if (lastCachedGroupUid == app3.info.uid && lastCachedGroup == connectionGroup) {
                                numCachedExtraGroup++;
                            } else {
                                lastCachedGroupUid = app3.info.uid;
                                lastCachedGroup = connectionGroup;
                            }
                        } else {
                            lastCachedGroup = 0;
                            lastCachedGroupUid = 0;
                        }
                        if (numCached - numCachedExtraGroup > cachedProcessLimit) {
                            app3.killLocked("cached #" + numCached, "too many cached", 13, 2, true);
                        }
                        numEmpty2 = numEmpty;
                        break;
                    case 18:
                    default:
                        this.mNumNonCachedProcs++;
                        numEmpty2 = numEmpty;
                        break;
                    case 19:
                        if (numEmpty > this.mConstants.CUR_TRIM_EMPTY_PROCESSES && app3.getLastActivityTime() < oldTime) {
                            app3.killLocked("empty for " + ((now - app3.getLastActivityTime()) / 1000) + "s", "empty for too long", 13, 4, true);
                            numEmpty2 = numEmpty;
                            break;
                        } else {
                            numEmpty2 = numEmpty + 1;
                            if (numEmpty2 > emptyProcessLimit) {
                                app3.killLocked("empty #" + numEmpty2, "too many empty", 13, 3, true);
                                break;
                            }
                        }
                        break;
                }
                if (app3.isolated && psr.numberOfRunningServices() <= 0 && app3.getIsolatedEntryPoint() == null) {
                    app3.killLocked("isolated not needed", 13, 17, true);
                } else {
                    updateAppUidRecLSP(app3);
                }
                if (state.getCurProcState() < 14 || app3.isKilledByAm()) {
                    numTrimming = numLru2;
                } else {
                    numTrimming = numLru2 + 1;
                }
            } else {
                i2 = numCachedExtraGroup2;
                lruList = lruList2;
                numLru = numLru3;
                numEmpty2 = numEmpty2;
                numTrimming = numTrimming;
            }
            numCachedExtraGroup2 = i2 - 1;
            lruList2 = lruList;
            numLru3 = numLru;
        }
        return this.mService.mAppProfiler.updateLowMemStateLSP(numCached, numEmpty2, numTrimming);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAppUidRecIfNecessaryLSP(ProcessRecord app) {
        if (!app.isKilledByAm() && app.getThread() != null) {
            if (!app.isolated || app.mServices.numberOfRunningServices() > 0 || app.getIsolatedEntryPoint() != null) {
                updateAppUidRecLSP(app);
            }
        }
    }

    private void updateAppUidRecLSP(ProcessRecord app) {
        UidRecord uidRec = app.getUidRecord();
        if (uidRec != null) {
            ProcessStateRecord state = app.mState;
            uidRec.setEphemeral(app.info.isInstantApp());
            if (uidRec.getCurProcState() > state.getCurProcState()) {
                uidRec.setCurProcState(state.getCurProcState());
            }
            if (app.mServices.hasForegroundServices()) {
                uidRec.setForegroundServices(true);
            }
            uidRec.setCurCapability(uidRec.getCurCapability() | state.getCurCapability());
        }
    }

    private void updateUidsLSP(ActiveUids activeUids, long nowElapsed) {
        this.mProcessList.incrementProcStateSeqAndNotifyAppsLOSP(activeUids);
        ArrayList<UidRecord> becameIdle = this.mTmpBecameIdle;
        becameIdle.clear();
        PowerManagerInternal powerManagerInternal = this.mLocalPowerManager;
        if (powerManagerInternal != null) {
            powerManagerInternal.startUidChanges();
        }
        for (int i = activeUids.size() - 1; i >= 0; i--) {
            UidRecord uidRec = activeUids.valueAt(i);
            if (uidRec.getCurProcState() != 20 && (uidRec.getSetProcState() != uidRec.getCurProcState() || uidRec.getSetCapability() != uidRec.getCurCapability() || uidRec.isSetAllowListed() != uidRec.isCurAllowListed() || uidRec.getProcAdjChanged())) {
                int uidChange = 0;
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Changes in " + uidRec + ": proc state from " + uidRec.getSetProcState() + " to " + uidRec.getCurProcState() + ", capability from " + uidRec.getSetCapability() + " to " + uidRec.getCurCapability() + ", allowlist from " + uidRec.isSetAllowListed() + " to " + uidRec.isCurAllowListed() + ", procAdjChanged: " + uidRec.getProcAdjChanged());
                }
                if (ActivityManager.isProcStateBackground(uidRec.getCurProcState()) && !uidRec.isCurAllowListed()) {
                    if (!ActivityManager.isProcStateBackground(uidRec.getSetProcState()) || uidRec.isSetAllowListed()) {
                        uidRec.setLastBackgroundTime(nowElapsed);
                        if (!this.mService.mHandler.hasMessages(58)) {
                            this.mService.mHandler.sendEmptyMessageDelayed(58, this.mConstants.BACKGROUND_SETTLE_TIME);
                        }
                    }
                    if (uidRec.isIdle() && !uidRec.isSetIdle()) {
                        uidChange = 0 | 2;
                        becameIdle.add(uidRec);
                    }
                } else {
                    if (uidRec.isIdle()) {
                        uidChange = 0 | 4;
                        EventLogTags.writeAmUidActive(uidRec.getUid());
                        uidRec.setIdle(false);
                        ITranOomAdjuster.Instance().hookUidChangedActive(uidRec.getUid());
                    }
                    uidRec.setLastBackgroundTime(0L);
                }
                boolean wasCached = uidRec.getSetProcState() > 11;
                boolean isCached = uidRec.getCurProcState() > 11;
                if (wasCached != isCached || uidRec.getSetProcState() == 20) {
                    uidChange |= isCached ? 8 : 16;
                }
                if (uidRec.getSetCapability() != uidRec.getCurCapability()) {
                    uidChange |= 32;
                }
                if (uidRec.getSetProcState() != uidRec.getCurProcState()) {
                    uidChange |= Integer.MIN_VALUE;
                }
                if (uidRec.getProcAdjChanged()) {
                    uidChange |= 64;
                }
                uidRec.setSetProcState(uidRec.getCurProcState());
                uidRec.setSetCapability(uidRec.getCurCapability());
                uidRec.setSetAllowListed(uidRec.isCurAllowListed());
                uidRec.setSetIdle(uidRec.isIdle());
                uidRec.clearProcAdjChanged();
                if ((uidChange & Integer.MIN_VALUE) != 0 || (uidChange & 32) != 0) {
                    this.mService.mAtmInternal.onUidProcStateChanged(uidRec.getUid(), uidRec.getSetProcState());
                }
                if (uidChange != 0) {
                    this.mService.enqueueUidChangeLocked(uidRec, -1, uidChange);
                }
                if ((uidChange & Integer.MIN_VALUE) != 0 || (uidChange & 32) != 0) {
                    this.mService.noteUidProcessState(uidRec.getUid(), uidRec.getCurProcState(), uidRec.getCurCapability());
                }
                if (uidRec.hasForegroundServices()) {
                    this.mService.mServices.foregroundServiceProcStateChangedLocked(uidRec);
                }
            }
            this.mService.mInternal.deletePendingTopUid(uidRec.getUid(), nowElapsed);
        }
        PowerManagerInternal powerManagerInternal2 = this.mLocalPowerManager;
        if (powerManagerInternal2 != null) {
            powerManagerInternal2.finishUidChanges();
        }
        int size = becameIdle.size();
        if (size > 0) {
            for (int i2 = size - 1; i2 >= 0; i2--) {
                this.mService.mServices.stopInBackgroundLocked(becameIdle.get(i2).getUid());
            }
        }
    }

    private boolean shouldKillExcessiveProcesses(long nowUptime) {
        long lastUserUnlockingUptime = this.mService.mUserController.getLastUserUnlockingUptime();
        if (lastUserUnlockingUptime == 0) {
            return !this.mConstants.mNoKillCachedProcessesUntilBootCompleted;
        }
        long noKillCachedProcessesPostBootCompletedDurationMillis = this.mConstants.mNoKillCachedProcessesPostBootCompletedDurationMillis;
        return lastUserUnlockingUptime + noKillCachedProcessesPostBootCompletedDurationMillis <= nowUptime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ComputeOomAdjWindowCallback implements WindowProcessController.ComputeOomAdjCallback {
        int adj;
        ProcessRecord app;
        int appUid;
        boolean foregroundActivities;
        int logUid;
        boolean mHasVisibleActivities;
        ProcessStateRecord mState;
        int procState;
        int processStateCurTop;
        int schedGroup;

        ComputeOomAdjWindowCallback() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void initialize(ProcessRecord app, int adj, boolean foregroundActivities, boolean hasVisibleActivities, int procState, int schedGroup, int appUid, int logUid, int processStateCurTop) {
            this.app = app;
            this.adj = adj;
            this.foregroundActivities = foregroundActivities;
            this.mHasVisibleActivities = hasVisibleActivities;
            this.procState = procState;
            this.schedGroup = schedGroup;
            this.appUid = appUid;
            this.logUid = logUid;
            this.processStateCurTop = processStateCurTop;
            this.mState = app.mState;
        }

        @Override // com.android.server.wm.WindowProcessController.ComputeOomAdjCallback
        public void onVisibleActivity() {
            if (this.adj > 100) {
                this.adj = 100;
                this.mState.setAdjType("vis-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to vis-activity: " + this.app);
                }
            }
            int i = this.procState;
            int i2 = this.processStateCurTop;
            if (i > i2) {
                this.procState = i2;
                this.mState.setAdjType("vis-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to vis-activity (top): " + this.app);
                }
            }
            if (this.schedGroup < 2) {
                this.schedGroup = 2;
            }
            this.mState.setCached(false);
            this.mState.setEmpty(false);
            this.foregroundActivities = true;
            this.mHasVisibleActivities = true;
        }

        @Override // com.android.server.wm.WindowProcessController.ComputeOomAdjCallback
        public void onPausedActivity() {
            if (this.adj > 200) {
                this.adj = 200;
                this.mState.setAdjType("pause-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to pause-activity: " + this.app);
                }
            }
            int i = this.procState;
            int i2 = this.processStateCurTop;
            if (i > i2) {
                this.procState = i2;
                this.mState.setAdjType("pause-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to pause-activity (top): " + this.app);
                }
            }
            if (this.schedGroup < 2) {
                this.schedGroup = 2;
            }
            this.mState.setCached(false);
            this.mState.setEmpty(false);
            this.foregroundActivities = true;
            this.mHasVisibleActivities = false;
        }

        @Override // com.android.server.wm.WindowProcessController.ComputeOomAdjCallback
        public void onStoppingActivity(boolean finishing) {
            if (this.adj > 200) {
                this.adj = 200;
                this.mState.setAdjType("stop-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to stop-activity: " + this.app);
                }
            }
            if (!finishing && this.procState > 15) {
                this.procState = 15;
                this.mState.setAdjType("stop-activity");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to stop-activity: " + this.app);
                }
            }
            this.mState.setCached(false);
            this.mState.setEmpty(false);
            this.foregroundActivities = true;
            this.mHasVisibleActivities = false;
        }

        @Override // com.android.server.wm.WindowProcessController.ComputeOomAdjCallback
        public void onOtherActivity() {
            if (this.procState > 16) {
                this.procState = 16;
                this.mState.setAdjType("cch-act");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || this.logUid == this.appUid) {
                    OomAdjuster.this.reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to cached activity: " + this.app);
                }
            }
            this.mHasVisibleActivities = false;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:198:0x053f  */
    /* JADX WARN: Removed duplicated region for block: B:212:0x059c  */
    /* JADX WARN: Removed duplicated region for block: B:230:0x0606  */
    /* JADX WARN: Removed duplicated region for block: B:236:0x0636  */
    /* JADX WARN: Removed duplicated region for block: B:240:0x065d  */
    /* JADX WARN: Removed duplicated region for block: B:243:0x0664  */
    /* JADX WARN: Removed duplicated region for block: B:246:0x069a  */
    /* JADX WARN: Removed duplicated region for block: B:250:0x06a2  */
    /* JADX WARN: Removed duplicated region for block: B:263:0x06f3  */
    /* JADX WARN: Removed duplicated region for block: B:270:0x0737  */
    /* JADX WARN: Removed duplicated region for block: B:315:0x0819  */
    /* JADX WARN: Removed duplicated region for block: B:381:0x09a7  */
    /* JADX WARN: Removed duplicated region for block: B:384:0x09b6  */
    /* JADX WARN: Removed duplicated region for block: B:476:0x0b19  */
    /* JADX WARN: Removed duplicated region for block: B:505:0x0b78  */
    /* JADX WARN: Removed duplicated region for block: B:515:0x0b91  */
    /* JADX WARN: Removed duplicated region for block: B:521:0x0ba5  */
    /* JADX WARN: Removed duplicated region for block: B:523:0x0bac  */
    /* JADX WARN: Removed duplicated region for block: B:528:0x0bb8  */
    /* JADX WARN: Removed duplicated region for block: B:532:0x0bc5  */
    /* JADX WARN: Removed duplicated region for block: B:540:0x0c40  */
    /* JADX WARN: Removed duplicated region for block: B:542:0x0c52  */
    /* JADX WARN: Removed duplicated region for block: B:549:0x0c7b  */
    /* JADX WARN: Removed duplicated region for block: B:550:0x0c81  */
    /* JADX WARN: Removed duplicated region for block: B:553:0x0c8d  */
    /* JADX WARN: Removed duplicated region for block: B:572:0x0cfa  */
    /* JADX WARN: Removed duplicated region for block: B:582:0x0dc3  */
    /* JADX WARN: Removed duplicated region for block: B:687:0x1081  */
    /* JADX WARN: Removed duplicated region for block: B:691:0x1092  */
    /* JADX WARN: Removed duplicated region for block: B:698:0x10c2  */
    /* JADX WARN: Removed duplicated region for block: B:704:0x10ec  */
    /* JADX WARN: Removed duplicated region for block: B:712:0x110b  */
    /* JADX WARN: Removed duplicated region for block: B:734:0x1168  */
    /* JADX WARN: Removed duplicated region for block: B:738:0x1178  */
    /* JADX WARN: Removed duplicated region for block: B:741:0x117d  */
    /* JADX WARN: Removed duplicated region for block: B:746:0x118e A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:749:0x1195  */
    /* JADX WARN: Removed duplicated region for block: B:752:0x11cc  */
    /* JADX WARN: Removed duplicated region for block: B:759:0x11e7 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:762:0x0d9a A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:766:0x106a A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean computeOomAdjLSP(ProcessRecord app, int cachedAdj, ProcessRecord topApp, boolean doingAll, long now, boolean cycleReEval, boolean computeClients) {
        int schedGroup;
        int adj;
        int procState;
        ProcessServiceRecord psr;
        boolean foregroundActivities;
        boolean hasVisibleActivities;
        int adj2;
        int prevCapability;
        int prevProcState;
        int prevAppAdj;
        int PROCESS_STATE_CUR_TOP;
        int capability;
        int schedGroup2;
        BackupRecord backupTarget;
        int capabilityFromFGS;
        int adj3;
        int is;
        boolean foregroundActivities2;
        int schedGroup3;
        boolean boundByNonBgRestricted;
        int prevCapability2;
        int prevProcState2;
        int prevAppAdj2;
        ProcessServiceRecord psr2;
        boolean z;
        boolean hasVisibleActivities2;
        int appUid;
        ProcessStateRecord state;
        String str;
        int logUid;
        ProcessProviderRecord ppr;
        int i;
        boolean boundByNonBgRestricted2;
        int provi;
        int logUid2;
        int schedGroup4;
        ProcessProviderRecord ppr2;
        boolean z2;
        int schedGroup5;
        ProcessServiceRecord psr3;
        int capabilityFromFGS2;
        ProcessStateRecord state2;
        int schedGroup6;
        int i2;
        int provi2;
        ProcessProviderRecord ppr3;
        ProcessServiceRecord psr4;
        int capabilityFromFGS3;
        String str2;
        int schedGroup7;
        ContentProviderRecord cpr;
        int procState2;
        int schedGroup8;
        ProcessStateRecord state3;
        String str3;
        boolean z3;
        int adj4;
        int adj5;
        int procState3;
        int schedGroup9;
        ProcessStateRecord state4;
        int provi3;
        ProcessProviderRecord ppr4;
        int logUid3;
        ProcessServiceRecord psr5;
        String str4;
        int adj6;
        String str5;
        ContentProviderConnection conn;
        int adj7;
        ContentProviderRecord cpr2;
        ProcessRecord client;
        ProcessStateRecord state5;
        int schedGroup10;
        int schedGroup11;
        int logUid4;
        String str6;
        boolean boundByNonBgRestricted3;
        BackupRecord backupTarget2;
        String str7;
        ProcessServiceRecord psr6;
        int i3;
        int is2;
        int prevCapability3;
        int prevProcState3;
        int prevAppAdj3;
        int prevAppAdj4;
        BackupRecord backupTarget3;
        ProcessServiceRecord psr7;
        int conni;
        int appUid2;
        ProcessStateRecord state6;
        String str8;
        int procState4;
        int conni2;
        int adj8;
        int is3;
        int prevCapability4;
        int prevProcState4;
        int prevAppAdj5;
        int prevAppAdj6;
        BackupRecord backupTarget4;
        int conni3;
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> serviceConnections;
        ServiceRecord s;
        ProcessServiceRecord psr8;
        ConnectionRecord cr;
        ProcessRecord client2;
        int procState5;
        int logUid5;
        int appUid3;
        int conni4;
        int prevCapability5;
        int prevProcState5;
        int prevAppAdj7;
        int prevAppAdj8;
        int prevCapability6;
        BackupRecord backupTarget5;
        ProcessStateRecord state7;
        int adj9;
        ArrayMap<IBinder, ArrayList<ConnectionRecord>> serviceConnections2;
        boolean z4;
        int is4;
        ProcessStateRecord cstate;
        char c;
        char c2;
        boolean z5;
        ConnectionRecord cr2;
        ConnectionRecord cr3;
        int clientProcState;
        ArrayList<ConnectionRecord> clist;
        ServiceRecord s2;
        int adj10;
        ProcessStateRecord state8;
        String str9;
        int conni5;
        int appUid4;
        boolean z6;
        int procState6;
        ProcessServiceRecord psr9;
        int procState7;
        int adj11;
        int procState8;
        ServiceRecord s3;
        int schedGroup12;
        int clientProcState2;
        int i4;
        int newAdj;
        int procState9;
        int procState10;
        ProcessStateRecord state9 = app.mState;
        if (this.mAdjSeq == state9.getAdjSeq()) {
            if (state9.getAdjSeq() == state9.getCompletedAdjSeq()) {
                return false;
            }
            state9.setContainsCycle(true);
            this.mProcessesInCycle.add(app);
            return false;
        } else if (app.getThread() == null) {
            state9.setAdjSeq(this.mAdjSeq);
            state9.setCurrentSchedulingGroup(0);
            state9.setCurProcState(19);
            state9.setCurAdj(999);
            state9.setCurRawAdj(999);
            state9.setCompletedAdjSeq(state9.getAdjSeq());
            state9.setCurCapability(0);
            return false;
        } else {
            state9.setAdjTypeCode(0);
            state9.setAdjSource(null);
            state9.setAdjTarget(null);
            state9.setEmpty(false);
            state9.setCached(false);
            if (!cycleReEval) {
                state9.setNoKillOnBgRestrictedAndIdle(false);
                UidRecord uidRec = app.getUidRecord();
                app.mOptRecord.setShouldNotFreeze((uidRec != null && uidRec.isCurAllowListed()) || ITranOomAdjuster.Instance().hookSetShouldNotFreeze(app.info.packageName));
            }
            int appUid5 = app.info.uid;
            int logUid6 = this.mService.mCurOomAdjUid;
            int prevAppAdj9 = state9.getCurAdj();
            int prevProcState6 = state9.getCurProcState();
            int prevCapability7 = state9.getCurCapability();
            ProcessServiceRecord psr10 = app.mServices;
            if (state9.getMaxAdj() <= 0) {
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making fixed: " + app);
                }
                state9.setAdjType("fixed");
                state9.setAdjSeq(this.mAdjSeq);
                state9.setCurRawAdj(state9.getMaxAdj());
                state9.setHasForegroundActivities(false);
                state9.setCurrentSchedulingGroup(2);
                state9.setCurCapability(15);
                state9.setCurProcState(0);
                state9.setSystemNoUi(true);
                if (app == topApp) {
                    state9.setSystemNoUi(false);
                    state9.setCurrentSchedulingGroup(3);
                    state9.setAdjType("pers-top-activity");
                } else if (state9.hasTopUi()) {
                    state9.setSystemNoUi(false);
                    state9.setAdjType("pers-top-ui");
                } else if (state9.getCachedHasVisibleActivities()) {
                    state9.setSystemNoUi(false);
                }
                if (!state9.isSystemNoUi()) {
                    if (this.mService.mWakefulness.get() == 1 || state9.isRunningRemoteAnimation()) {
                        state9.setCurProcState(1);
                        state9.setCurrentSchedulingGroup(3);
                    } else {
                        state9.setCurProcState(5);
                        state9.setCurrentSchedulingGroup(1);
                    }
                }
                state9.setCurRawProcState(state9.getCurProcState());
                state9.setCurAdj(state9.getMaxAdj());
                state9.setCompletedAdjSeq(state9.getAdjSeq());
                return state9.getCurAdj() < prevAppAdj9 || state9.getCurProcState() < prevProcState6;
            }
            state9.setSystemNoUi(false);
            int PROCESS_STATE_CUR_TOP2 = this.mService.mAtmInternal.getTopProcessState();
            int capability2 = cycleReEval ? app.mState.getCurCapability() : 0;
            boolean foregroundActivities3 = false;
            boolean hasVisibleActivities3 = false;
            if (PROCESS_STATE_CUR_TOP2 == 2 && app == topApp) {
                adj = 0;
                schedGroup = 3;
                state9.setAdjType(HostingRecord.HOSTING_TYPE_TOP_ACTIVITY);
                foregroundActivities3 = true;
                hasVisibleActivities3 = true;
                procState = PROCESS_STATE_CUR_TOP2;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making top: " + app);
                }
            } else if (state9.isRunningRemoteAnimation()) {
                adj = 100;
                schedGroup = 3;
                state9.setAdjType("running-remote-anim");
                procState = PROCESS_STATE_CUR_TOP2;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making running remote anim: " + app);
                }
            } else if (app.getActiveInstrumentation() != null) {
                adj = 0;
                schedGroup = 2;
                state9.setAdjType(ParsingPackageUtils.TAG_INSTRUMENTATION);
                procState = 4;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making instrumentation: " + app);
                }
            } else if (state9.getCachedIsReceivingBroadcast(this.mTmpBroadcastQueue)) {
                adj = 0;
                schedGroup = this.mTmpBroadcastQueue.contains(this.mService.mFgBroadcastQueue) ? 2 : 0;
                state9.setAdjType("broadcast");
                procState = 11;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making broadcast: " + app);
                }
            } else if (psr10.numberOfExecutingServices() > 0) {
                adj = 0;
                schedGroup = psr10.shouldExecServicesFg() ? 2 : 0;
                state9.setAdjType("exec-service");
                procState = 10;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making exec-service: " + app);
                }
            } else if (app == topApp) {
                adj = 0;
                schedGroup = 0;
                state9.setAdjType("top-sleeping");
                foregroundActivities3 = true;
                procState = PROCESS_STATE_CUR_TOP2;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making top (sleeping): " + app);
                }
            } else if (app.preloadState == 2) {
                adj = 800;
                schedGroup = 0;
                state9.setAdjType("agares-preload");
                state9.setAgaresComputeOomAdj(true);
                procState = 7;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making agares-preload: " + app);
                }
            } else {
                schedGroup = 0;
                adj = cachedAdj;
                procState = 19;
                if (!state9.containsCycle()) {
                    state9.setCached(true);
                    state9.setEmpty(true);
                    state9.setAdjType("cch-empty");
                }
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Making empty: " + app);
                }
            }
            int procState11 = procState;
            boolean foregroundActivities4 = foregroundActivities3;
            boolean hasVisibleActivities4 = hasVisibleActivities3;
            int adj12 = adj;
            int schedGroup13 = schedGroup;
            if (app.preloadState == 0 && state9.isAgaresComputeOomAdj()) {
                state9.setAgaresComputeOomAdj(false);
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    Slog.i(ActivityManagerService.TAG_OOM_ADJ, "Cancel agares-preload: " + app);
                }
            }
            if (foregroundActivities4 || !state9.getCachedHasActivities()) {
                psr = psr10;
                foregroundActivities = foregroundActivities4;
                hasVisibleActivities = hasVisibleActivities4;
                adj2 = adj12;
            } else {
                psr = psr10;
                state9.computeOomAdjFromActivitiesIfNecessary(this.mTmpComputeOomAdjWindowCallback, adj12, foregroundActivities4, hasVisibleActivities4, procState11, schedGroup13, appUid5, logUid6, PROCESS_STATE_CUR_TOP2);
                int adj13 = state9.getCachedAdj();
                boolean foregroundActivities5 = state9.getCachedForegroundActivities();
                boolean hasVisibleActivities5 = state9.getCachedHasVisibleActivities();
                procState11 = state9.getCachedProcState();
                schedGroup13 = state9.getCachedSchedGroup();
                foregroundActivities = foregroundActivities5;
                hasVisibleActivities = hasVisibleActivities5;
                adj2 = adj13;
            }
            if (procState11 <= 18 || !state9.getCachedHasRecentTasks()) {
                prevCapability = prevCapability7;
            } else {
                procState11 = 18;
                state9.setAdjType("cch-rec");
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    prevCapability = prevCapability7;
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to cached recent: " + app);
                } else {
                    prevCapability = prevCapability7;
                }
            }
            String str10 = ": ";
            String str11 = "Raise to ";
            if (adj2 <= 200 && procState11 <= 4) {
                prevProcState = prevProcState6;
                prevAppAdj = prevAppAdj9;
                if (psr.hasForegroundServices() && adj2 > 50 && (state9.getLastTopTime() + this.mConstants.TOP_TO_FGS_GRACE_DURATION > now || state9.getSetProcState() <= 2)) {
                    adj2 = 50;
                    state9.setAdjType("fg-service-act");
                    if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                        reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to recent fg: " + app);
                    }
                }
                if (!psr.hasTopStartedAlmostPerceptibleServices() && adj2 > 50) {
                    PROCESS_STATE_CUR_TOP = PROCESS_STATE_CUR_TOP2;
                    capability = capability2;
                    if (state9.getLastTopTime() + this.mConstants.TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION > now || state9.getSetProcState() <= 2) {
                        adj2 = 50;
                        state9.setAdjType("top-ej-act");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to recent fg for EJ: " + app);
                        }
                    }
                } else {
                    PROCESS_STATE_CUR_TOP = PROCESS_STATE_CUR_TOP2;
                    capability = capability2;
                }
                if ((adj2 <= 200 || procState11 > 8) && state9.getForcingToImportant() != null) {
                    adj2 = 200;
                    procState11 = 8;
                    state9.setCached(false);
                    state9.setAdjType("force-imp");
                    state9.setAdjSource(state9.getForcingToImportant());
                    schedGroup13 = 2;
                    if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                        reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to force imp: " + app);
                    }
                }
                if (state9.getCachedIsHeavyWeight()) {
                    if (adj2 > 400) {
                        adj2 = 400;
                        schedGroup13 = 0;
                        state9.setCached(false);
                        state9.setAdjType("heavy");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to heavy: " + app);
                        }
                    }
                    if (procState11 > 13) {
                        procState11 = 13;
                        state9.setAdjType("heavy");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to heavy: " + app);
                        }
                    }
                }
                if (state9.getCachedIsHomeProcess()) {
                    if (adj2 > 600) {
                        adj2 = 600;
                        schedGroup13 = 0;
                        state9.setCached(false);
                        state9.setAdjType("home");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to home: " + app);
                        }
                    }
                    if (procState11 > 14) {
                        procState11 = 14;
                        state9.setAdjType("home");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to home: " + app);
                        }
                    }
                }
                if (state9.getCachedIsPreviousProcess() && state9.getCachedHasActivities()) {
                    if (adj2 > 700) {
                        adj2 = 700;
                        schedGroup13 = 0;
                        state9.setCached(false);
                        state9.setAdjType("previous");
                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to prev: " + app);
                        }
                    }
                    if (procState11 > 15) {
                        schedGroup2 = schedGroup13;
                        if (cycleReEval) {
                            procState11 = Math.min(procState11, state9.getCurRawProcState());
                            adj2 = Math.min(adj2, state9.getCurRawAdj());
                            schedGroup2 = Math.max(schedGroup2, state9.getCurrentSchedulingGroup());
                        }
                        state9.setCurRawAdj(adj2);
                        state9.setCurRawProcState(procState11);
                        state9.setHasStartedServices(false);
                        state9.setAdjSeq(this.mAdjSeq);
                        backupTarget = this.mService.mBackupTargets.get(app.userId);
                        if (backupTarget != null && app == backupTarget.app) {
                            if (adj2 > 300) {
                                if (ActivityManagerDebugConfig.DEBUG_BACKUP) {
                                    Slog.v(ActivityManagerService.TAG_BACKUP, "oom BACKUP_APP_ADJ for " + app);
                                }
                                adj2 = 300;
                                if (procState11 > 8) {
                                    procState11 = 8;
                                }
                                state9.setAdjType(HostingRecord.HOSTING_TYPE_BACKUP);
                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to backup: " + app);
                                }
                                state9.setCached(false);
                            }
                            if (procState11 > 9) {
                                procState11 = 9;
                                state9.setAdjType(HostingRecord.HOSTING_TYPE_BACKUP);
                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to backup: " + app);
                                }
                            }
                        }
                        boolean boundByNonBgRestricted4 = state9.isCurBoundByNonBgRestrictedApp();
                        int i5 = adj2;
                        capabilityFromFGS = 0;
                        adj3 = i5;
                        int i6 = capability;
                        boolean schedGroup14 = false;
                        is = psr.numberOfRunningServices() - 1;
                        int capability3 = i6;
                        while (true) {
                            foregroundActivities2 = foregroundActivities;
                            if (is < 0) {
                                schedGroup3 = schedGroup2;
                                boundByNonBgRestricted = boundByNonBgRestricted4;
                                prevCapability2 = prevCapability;
                                prevProcState2 = prevProcState;
                                prevAppAdj2 = prevAppAdj;
                                psr2 = psr;
                                z = false;
                                hasVisibleActivities2 = hasVisibleActivities;
                                appUid = appUid5;
                                state = state9;
                                str = str10;
                                logUid = logUid6;
                                break;
                            } else if (adj3 <= 0 && schedGroup2 != 0 && procState11 <= 2) {
                                schedGroup3 = schedGroup2;
                                boundByNonBgRestricted = boundByNonBgRestricted4;
                                prevCapability2 = prevCapability;
                                prevProcState2 = prevProcState;
                                prevAppAdj2 = prevAppAdj;
                                psr2 = psr;
                                z = false;
                                hasVisibleActivities2 = hasVisibleActivities;
                                appUid = appUid5;
                                state = state9;
                                str = str10;
                                logUid = logUid6;
                                break;
                            } else {
                                ProcessServiceRecord psr11 = psr;
                                boolean hasVisibleActivities6 = hasVisibleActivities;
                                ServiceRecord s4 = psr11.getRunningServiceAt(is);
                                int schedGroup15 = schedGroup2;
                                if (!s4.startRequested) {
                                    boundByNonBgRestricted3 = boundByNonBgRestricted4;
                                    backupTarget2 = backupTarget;
                                    str7 = str10;
                                    psr6 = psr11;
                                    i3 = 0;
                                } else {
                                    state9.setHasStartedServices(true);
                                    if (procState11 <= 10) {
                                        boundByNonBgRestricted3 = boundByNonBgRestricted4;
                                    } else {
                                        state9.setAdjType("started-services");
                                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                                            boundByNonBgRestricted3 = boundByNonBgRestricted4;
                                            procState10 = 10;
                                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to started service: " + app);
                                        } else {
                                            boundByNonBgRestricted3 = boundByNonBgRestricted4;
                                            procState10 = 10;
                                        }
                                        procState11 = procState10;
                                    }
                                    if (!s4.mKeepWarming && state9.hasShownUi() && !state9.getCachedIsHomeProcess()) {
                                        if (adj3 > 500) {
                                            state9.setAdjType("cch-started-ui-services");
                                        }
                                        backupTarget2 = backupTarget;
                                        str7 = str10;
                                        psr6 = psr11;
                                        i3 = 0;
                                    } else {
                                        if (s4.mKeepWarming) {
                                            procState9 = procState11;
                                            backupTarget2 = backupTarget;
                                            str7 = str10;
                                            psr6 = psr11;
                                        } else {
                                            procState9 = procState11;
                                            backupTarget2 = backupTarget;
                                            str7 = str10;
                                            psr6 = psr11;
                                            if (now >= s4.lastActivity + this.mConstants.MAX_SERVICE_INACTIVITY) {
                                                i3 = 0;
                                                if (adj3 > 500) {
                                                    state9.setAdjType("cch-started-services");
                                                }
                                                procState11 = procState9;
                                            }
                                        }
                                        if (adj3 <= 500) {
                                            i3 = 0;
                                        } else {
                                            adj3 = 500;
                                            state9.setAdjType("started-services");
                                            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                                                reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to started service: " + app);
                                            }
                                            i3 = 0;
                                            state9.setCached(false);
                                        }
                                        if (adj3 > 500) {
                                        }
                                        procState11 = procState9;
                                    }
                                }
                                if (s4.isForeground) {
                                    int fgsType = s4.foregroundServiceType;
                                    if (s4.mAllowWhileInUsePermissionInFgs) {
                                        int capabilityFromFGS4 = ((fgsType & 8) != 0 ? 1 : i3) | capabilityFromFGS;
                                        boolean enabled = state9.getCachedCompatChange(1);
                                        if (enabled) {
                                            capabilityFromFGS = capabilityFromFGS4 | ((fgsType & 64) != 0 ? 2 : i3) | ((fgsType & 128) != 0 ? 4 : i3);
                                        } else {
                                            capabilityFromFGS = capabilityFromFGS4 | 6;
                                        }
                                    }
                                }
                                ArrayMap<IBinder, ArrayList<ConnectionRecord>> serviceConnections3 = s4.getConnections();
                                boolean z7 = true;
                                int procState12 = procState11;
                                boundByNonBgRestricted4 = boundByNonBgRestricted3;
                                int logUid7 = serviceConnections3.size() - 1;
                                schedGroup2 = schedGroup15;
                                while (true) {
                                    if (logUid7 < 0) {
                                        is2 = is;
                                        prevCapability3 = prevCapability;
                                        prevProcState3 = prevProcState;
                                        prevAppAdj3 = prevAppAdj;
                                        prevAppAdj4 = PROCESS_STATE_CUR_TOP;
                                        backupTarget3 = backupTarget2;
                                        psr7 = psr6;
                                        conni = logUid6;
                                        appUid2 = appUid5;
                                        state6 = state9;
                                        str8 = str7;
                                        break;
                                    } else if (adj3 <= 0 && schedGroup2 != 0 && procState12 <= 2) {
                                        conni = logUid6;
                                        appUid2 = appUid5;
                                        is2 = is;
                                        state6 = state9;
                                        prevCapability3 = prevCapability;
                                        prevProcState3 = prevProcState;
                                        prevAppAdj3 = prevAppAdj;
                                        prevAppAdj4 = PROCESS_STATE_CUR_TOP;
                                        backupTarget3 = backupTarget2;
                                        str8 = str7;
                                        psr7 = psr6;
                                        break;
                                    } else {
                                        ArrayList<ConnectionRecord> clist2 = serviceConnections3.valueAt(logUid7);
                                        int capability4 = capability3;
                                        boolean boundByNonBgRestricted5 = boundByNonBgRestricted4;
                                        int procState13 = procState12;
                                        int procState14 = adj3;
                                        int adj14 = 0;
                                        boolean scheduleLikeTopApp = schedGroup14;
                                        int schedGroup16 = schedGroup2;
                                        while (true) {
                                            if (adj14 >= clist2.size()) {
                                                procState4 = procState13;
                                                conni2 = logUid7;
                                                adj8 = procState14;
                                                is3 = is;
                                                prevCapability4 = prevCapability;
                                                prevProcState4 = prevProcState;
                                                prevAppAdj5 = prevAppAdj;
                                                prevAppAdj6 = PROCESS_STATE_CUR_TOP;
                                                backupTarget4 = backupTarget2;
                                                conni3 = logUid6;
                                                serviceConnections = serviceConnections3;
                                                s = s4;
                                                psr8 = psr6;
                                                break;
                                            } else if (procState14 <= 0 && schedGroup16 != 0 && procState13 <= 2) {
                                                procState4 = procState13;
                                                conni2 = logUid7;
                                                adj8 = procState14;
                                                is3 = is;
                                                prevCapability4 = prevCapability;
                                                prevProcState4 = prevProcState;
                                                prevAppAdj5 = prevAppAdj;
                                                prevAppAdj6 = PROCESS_STATE_CUR_TOP;
                                                backupTarget4 = backupTarget2;
                                                psr8 = psr6;
                                                conni3 = logUid6;
                                                serviceConnections = serviceConnections3;
                                                s = s4;
                                                break;
                                            } else {
                                                ConnectionRecord cr4 = clist2.get(adj14);
                                                int i7 = adj14;
                                                if (cr4.binding.client == app) {
                                                    conni4 = logUid7;
                                                    is4 = is;
                                                    prevCapability5 = prevCapability;
                                                    prevProcState5 = prevProcState;
                                                    prevAppAdj7 = prevAppAdj;
                                                    prevAppAdj8 = PROCESS_STATE_CUR_TOP;
                                                    prevCapability6 = i7;
                                                    backupTarget5 = backupTarget2;
                                                    conni5 = logUid6;
                                                    serviceConnections2 = serviceConnections3;
                                                    clist = clist2;
                                                    s2 = s4;
                                                    psr9 = psr6;
                                                    appUid4 = appUid5;
                                                    state8 = state9;
                                                    str9 = str7;
                                                } else {
                                                    boolean trackedProcState = false;
                                                    ProcessRecord client3 = cr4.binding.client;
                                                    ProcessStateRecord state10 = state9;
                                                    ProcessStateRecord state11 = client3.mState;
                                                    if (computeClients) {
                                                        cr = cr4;
                                                        prevCapability5 = prevCapability;
                                                        client2 = client3;
                                                        prevProcState5 = prevProcState;
                                                        prevCapability6 = i7;
                                                        procState5 = procState13;
                                                        prevAppAdj7 = prevAppAdj;
                                                        logUid5 = logUid6;
                                                        appUid3 = appUid5;
                                                        conni4 = logUid7;
                                                        adj9 = procState14;
                                                        prevAppAdj8 = PROCESS_STATE_CUR_TOP;
                                                        backupTarget5 = backupTarget2;
                                                        serviceConnections2 = serviceConnections3;
                                                        z4 = z7;
                                                        is4 = is;
                                                        state7 = state10;
                                                        computeOomAdjLSP(client3, cachedAdj, topApp, doingAll, now, cycleReEval, true);
                                                        cstate = state11;
                                                    } else {
                                                        cr = cr4;
                                                        client2 = client3;
                                                        procState5 = procState13;
                                                        logUid5 = logUid6;
                                                        appUid3 = appUid5;
                                                        conni4 = logUid7;
                                                        prevCapability5 = prevCapability;
                                                        prevProcState5 = prevProcState;
                                                        prevAppAdj7 = prevAppAdj;
                                                        prevAppAdj8 = PROCESS_STATE_CUR_TOP;
                                                        prevCapability6 = i7;
                                                        backupTarget5 = backupTarget2;
                                                        state7 = state10;
                                                        adj9 = procState14;
                                                        serviceConnections2 = serviceConnections3;
                                                        z4 = z7;
                                                        is4 = is;
                                                        cstate = state11;
                                                        cstate.setCurRawAdj(state11.getCurAdj());
                                                        cstate.setCurRawProcState(cstate.getCurProcState());
                                                    }
                                                    int clientAdj = cstate.getCurRawAdj();
                                                    int clientProcState3 = cstate.getCurRawProcState();
                                                    boolean clientIsSystem = clientProcState3 < 2 ? z4 : false;
                                                    if (cstate.isCurBoundByNonBgRestrictedApp()) {
                                                        c = 4;
                                                        c2 = 3;
                                                    } else {
                                                        c2 = 3;
                                                        if (clientProcState3 <= 3) {
                                                            c = 4;
                                                        } else {
                                                            c = 4;
                                                            if (clientProcState3 != 4 || cstate.isBackgroundRestricted()) {
                                                                z5 = false;
                                                                boundByNonBgRestricted5 |= z5;
                                                                if (client2.mOptRecord.shouldNotFreeze()) {
                                                                    app.mOptRecord.setShouldNotFreeze(z4);
                                                                }
                                                                cr2 = cr;
                                                                if ((cr2.flags & 32) != 0) {
                                                                    if (cr2.hasFlag(4096)) {
                                                                        capability4 |= cstate.getCurCapability();
                                                                    }
                                                                    if ((cstate.getCurCapability() & 8) != 0) {
                                                                        if (clientProcState3 <= 5) {
                                                                            if ((cr2.flags & 131072) != 0) {
                                                                                capability4 |= 8;
                                                                            }
                                                                        } else {
                                                                            capability4 |= 8;
                                                                        }
                                                                    }
                                                                    cr3 = cr2;
                                                                    clist = clist2;
                                                                    if (shouldSkipDueToCycle(app, cstate, procState5, adj9, cycleReEval)) {
                                                                        s2 = s4;
                                                                        procState13 = procState5;
                                                                        procState14 = adj9;
                                                                        state8 = state7;
                                                                        str9 = str7;
                                                                        psr9 = psr6;
                                                                        conni5 = logUid5;
                                                                        appUid4 = appUid3;
                                                                    } else {
                                                                        if (clientProcState3 >= 16) {
                                                                            clientProcState3 = 19;
                                                                        }
                                                                        String adjType = null;
                                                                        if ((cr3.flags & 16) == 0) {
                                                                            adj10 = adj9;
                                                                            s3 = s4;
                                                                        } else {
                                                                            if (clientAdj < 900) {
                                                                                app.mOptRecord.setShouldNotFreeze(true);
                                                                            }
                                                                            if (!state7.hasShownUi() || state7.getCachedIsHomeProcess()) {
                                                                                adj10 = adj9;
                                                                                state7 = state7;
                                                                                s3 = s4;
                                                                                if (now >= s4.lastActivity + this.mConstants.MAX_SERVICE_INACTIVITY) {
                                                                                    if (adj10 > clientAdj) {
                                                                                        adjType = "cch-bound-services";
                                                                                    }
                                                                                    clientAdj = adj10;
                                                                                }
                                                                            } else {
                                                                                adj10 = adj9;
                                                                                if (adj10 > clientAdj) {
                                                                                    adjType = "cch-bound-ui-services";
                                                                                }
                                                                                state7.setCached(false);
                                                                                clientAdj = adj10;
                                                                                clientProcState3 = procState5;
                                                                                s3 = s4;
                                                                            }
                                                                        }
                                                                        if (adj10 <= clientAdj) {
                                                                            state8 = state7;
                                                                        } else {
                                                                            if (!state7.hasShownUi() || state7.getCachedIsHomeProcess()) {
                                                                                i4 = 200;
                                                                            } else {
                                                                                i4 = 200;
                                                                                if (clientAdj > 200) {
                                                                                    if (adj10 < 900) {
                                                                                        state8 = state7;
                                                                                    } else {
                                                                                        adjType = "cch-bound-ui-services";
                                                                                        procState6 = procState5;
                                                                                        schedGroup12 = schedGroup16;
                                                                                        state8 = state7;
                                                                                        if ((cr3.flags & 8388612) == 0) {
                                                                                            int curSchedGroup = cstate.getCurrentSchedulingGroup();
                                                                                            if (curSchedGroup > schedGroup12) {
                                                                                                if ((cr3.flags & 64) != 0) {
                                                                                                    schedGroup12 = curSchedGroup;
                                                                                                } else {
                                                                                                    schedGroup12 = 2;
                                                                                                }
                                                                                            }
                                                                                            if (clientProcState3 < 2) {
                                                                                                clientProcState3 = cr3.hasFlag(268435456) ? 4 : cr3.hasFlag(67108864) ? 5 : (this.mService.mWakefulness.get() != 1 || (cr3.flags & 33554432) == 0) ? 6 : 5;
                                                                                            } else if (clientProcState3 == 2) {
                                                                                                clientProcState3 = 3;
                                                                                                boolean enabled2 = cstate.getCachedCompatChange(0);
                                                                                                if (enabled2) {
                                                                                                    if (cr3.hasFlag(4096)) {
                                                                                                        capability4 |= cstate.getCurCapability();
                                                                                                    }
                                                                                                } else {
                                                                                                    capability4 |= cstate.getCurCapability();
                                                                                                }
                                                                                            }
                                                                                        } else if ((cr3.flags & 8388608) == 0) {
                                                                                            if (clientProcState3 < 8) {
                                                                                                clientProcState3 = 8;
                                                                                            }
                                                                                        } else if (clientProcState3 < 7) {
                                                                                            clientProcState3 = 7;
                                                                                        }
                                                                                        if (schedGroup12 >= 3 && (cr3.flags & 524288) != 0 && clientIsSystem) {
                                                                                            schedGroup16 = 3;
                                                                                            scheduleLikeTopApp = true;
                                                                                        } else {
                                                                                            schedGroup16 = schedGroup12;
                                                                                        }
                                                                                        if (!trackedProcState) {
                                                                                            cr3.trackProcState(clientProcState3, this.mAdjSeq);
                                                                                        }
                                                                                        if (procState6 > clientProcState3) {
                                                                                            procState6 = clientProcState3;
                                                                                            state8.setCurRawProcState(procState6);
                                                                                            if (adjType == null) {
                                                                                                adjType = HostingRecord.HOSTING_TYPE_SERVICE;
                                                                                            }
                                                                                        }
                                                                                        if (procState6 < 7 && (cr3.flags & 536870912) != 0) {
                                                                                            app.setPendingUiClean(true);
                                                                                        }
                                                                                        if (adjType != null) {
                                                                                            state8.setAdjType(adjType);
                                                                                            state8.setAdjTypeCode(2);
                                                                                            state8.setAdjSource(cr3.binding.client);
                                                                                            state8.setAdjSourceProcState(clientProcState3);
                                                                                            s2 = s3;
                                                                                            state8.setAdjTarget(s2.instanceName);
                                                                                            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON) {
                                                                                                conni5 = logUid5;
                                                                                                appUid4 = appUid3;
                                                                                            } else {
                                                                                                conni5 = logUid5;
                                                                                                appUid4 = appUid3;
                                                                                                if (conni5 != appUid4) {
                                                                                                    clientProcState2 = clientProcState3;
                                                                                                    str9 = str7;
                                                                                                }
                                                                                            }
                                                                                            clientProcState2 = clientProcState3;
                                                                                            str9 = str7;
                                                                                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to " + adjType + str9 + app + ", due to " + cr3.binding.client + " adj=" + adj10 + " procState=" + ProcessList.makeProcStateString(procState6));
                                                                                        } else {
                                                                                            clientProcState2 = clientProcState3;
                                                                                            s2 = s3;
                                                                                            str9 = str7;
                                                                                            conni5 = logUid5;
                                                                                            appUid4 = appUid3;
                                                                                        }
                                                                                        clientProcState = clientProcState2;
                                                                                        z6 = true;
                                                                                    }
                                                                                }
                                                                            }
                                                                            if ((cr3.flags & 72) != 0) {
                                                                                if (clientAdj >= -700) {
                                                                                    newAdj = clientAdj;
                                                                                } else {
                                                                                    newAdj = -700;
                                                                                    cr3.trackProcState(0, this.mAdjSeq);
                                                                                    schedGroup16 = 2;
                                                                                    procState5 = 0;
                                                                                    trackedProcState = true;
                                                                                }
                                                                            } else {
                                                                                int newAdj2 = cr3.flags;
                                                                                if ((newAdj2 & 256) != 0 && clientAdj <= i4 && adj10 >= 250) {
                                                                                    newAdj = 250;
                                                                                } else {
                                                                                    int newAdj3 = cr3.flags;
                                                                                    if ((newAdj3 & 65536) != 0 && clientAdj < i4 && adj10 >= 225) {
                                                                                        newAdj = ProcessList.PERCEPTIBLE_MEDIUM_APP_ADJ;
                                                                                    } else {
                                                                                        int newAdj4 = cr3.flags;
                                                                                        if ((newAdj4 & 1073741824) != 0 && clientAdj < i4 && adj10 >= i4) {
                                                                                            newAdj = 200;
                                                                                        } else if (clientAdj >= i4) {
                                                                                            newAdj = clientAdj;
                                                                                        } else if (cr3.hasFlag(268435456) && clientAdj <= 100 && adj10 > 100) {
                                                                                            newAdj = 100;
                                                                                        } else if (adj10 > 100) {
                                                                                            newAdj = Math.max(clientAdj, 100);
                                                                                        } else {
                                                                                            newAdj = adj10;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            if (cstate.isCached()) {
                                                                                state8 = state7;
                                                                            } else {
                                                                                state8 = state7;
                                                                                state8.setCached(false);
                                                                            }
                                                                            if (adj10 <= newAdj) {
                                                                                procState6 = procState5;
                                                                                schedGroup12 = schedGroup16;
                                                                            } else {
                                                                                adj10 = newAdj;
                                                                                state8.setCurRawAdj(adj10);
                                                                                adjType = HostingRecord.HOSTING_TYPE_SERVICE;
                                                                                procState6 = procState5;
                                                                                schedGroup12 = schedGroup16;
                                                                            }
                                                                            if ((cr3.flags & 8388612) == 0) {
                                                                            }
                                                                            if (schedGroup12 >= 3) {
                                                                            }
                                                                            schedGroup16 = schedGroup12;
                                                                            if (!trackedProcState) {
                                                                            }
                                                                            if (procState6 > clientProcState3) {
                                                                            }
                                                                            if (procState6 < 7) {
                                                                                app.setPendingUiClean(true);
                                                                            }
                                                                            if (adjType != null) {
                                                                            }
                                                                            clientProcState = clientProcState2;
                                                                            z6 = true;
                                                                        }
                                                                        procState6 = procState5;
                                                                        schedGroup12 = schedGroup16;
                                                                        if ((cr3.flags & 8388612) == 0) {
                                                                        }
                                                                        if (schedGroup12 >= 3) {
                                                                        }
                                                                        schedGroup16 = schedGroup12;
                                                                        if (!trackedProcState) {
                                                                        }
                                                                        if (procState6 > clientProcState3) {
                                                                        }
                                                                        if (procState6 < 7) {
                                                                        }
                                                                        if (adjType != null) {
                                                                        }
                                                                        clientProcState = clientProcState2;
                                                                        z6 = true;
                                                                    }
                                                                } else {
                                                                    cr3 = cr2;
                                                                    clientProcState = clientProcState3;
                                                                    clist = clist2;
                                                                    s2 = s4;
                                                                    adj10 = adj9;
                                                                    state8 = state7;
                                                                    str9 = str7;
                                                                    conni5 = logUid5;
                                                                    appUid4 = appUid3;
                                                                    if (clientAdj >= 900) {
                                                                        z6 = true;
                                                                    } else {
                                                                        z6 = true;
                                                                        app.mOptRecord.setShouldNotFreeze(true);
                                                                    }
                                                                    procState6 = procState5;
                                                                }
                                                                if ((cr3.flags & 134217728) != 0) {
                                                                    psr9 = psr6;
                                                                } else {
                                                                    psr9 = psr6;
                                                                    psr9.setTreatLikeActivity(z6);
                                                                }
                                                                ActivityServiceConnectionsHolder a = cr3.activity;
                                                                if ((cr3.flags & 128) != 0) {
                                                                    procState7 = procState6;
                                                                } else if (a == null || adj10 <= 0) {
                                                                    procState7 = procState6;
                                                                } else if (!a.isActivityVisible()) {
                                                                    procState7 = procState6;
                                                                } else {
                                                                    state8.setCurRawAdj(0);
                                                                    if ((cr3.flags & 4) == 0) {
                                                                        if ((cr3.flags & 64) != 0) {
                                                                            schedGroup16 = 4;
                                                                        } else {
                                                                            schedGroup16 = 2;
                                                                        }
                                                                    }
                                                                    state8.setCached(false);
                                                                    state8.setAdjType(HostingRecord.HOSTING_TYPE_SERVICE);
                                                                    state8.setAdjTypeCode(2);
                                                                    state8.setAdjSource(a);
                                                                    state8.setAdjSourceProcState(procState6);
                                                                    state8.setAdjTarget(s2.instanceName);
                                                                    if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || conni5 == appUid4) {
                                                                        adj11 = 0;
                                                                        procState8 = procState6;
                                                                        reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to service w/activity: " + app);
                                                                    } else {
                                                                        adj11 = 0;
                                                                        procState8 = procState6;
                                                                    }
                                                                    procState14 = adj11;
                                                                    procState13 = procState8;
                                                                }
                                                                procState14 = adj10;
                                                                procState13 = procState7;
                                                            }
                                                        }
                                                    }
                                                    z5 = z4;
                                                    boundByNonBgRestricted5 |= z5;
                                                    if (client2.mOptRecord.shouldNotFreeze()) {
                                                    }
                                                    cr2 = cr;
                                                    if ((cr2.flags & 32) != 0) {
                                                    }
                                                    if ((cr3.flags & 134217728) != 0) {
                                                    }
                                                    ActivityServiceConnectionsHolder a2 = cr3.activity;
                                                    if ((cr3.flags & 128) != 0) {
                                                    }
                                                    procState14 = adj10;
                                                    procState13 = procState7;
                                                }
                                                adj14 = prevCapability6 + 1;
                                                str7 = str9;
                                                psr6 = psr9;
                                                is = is4;
                                                serviceConnections3 = serviceConnections2;
                                                clist2 = clist;
                                                prevCapability = prevCapability5;
                                                prevProcState = prevProcState5;
                                                z7 = true;
                                                state9 = state8;
                                                appUid5 = appUid4;
                                                PROCESS_STATE_CUR_TOP = prevAppAdj8;
                                                backupTarget2 = backupTarget5;
                                                prevAppAdj = prevAppAdj7;
                                                s4 = s2;
                                                logUid6 = conni5;
                                                logUid7 = conni4;
                                            }
                                        }
                                        int i8 = conni2 - 1;
                                        str7 = str7;
                                        psr6 = psr8;
                                        procState12 = procState4;
                                        is = is3;
                                        backupTarget2 = backupTarget4;
                                        serviceConnections3 = serviceConnections;
                                        boundByNonBgRestricted4 = boundByNonBgRestricted5;
                                        capability3 = capability4;
                                        prevCapability = prevCapability4;
                                        prevProcState = prevProcState4;
                                        i3 = 0;
                                        z7 = true;
                                        state9 = state9;
                                        appUid5 = appUid5;
                                        PROCESS_STATE_CUR_TOP = prevAppAdj6;
                                        prevAppAdj = prevAppAdj5;
                                        s4 = s;
                                        logUid6 = conni3;
                                        logUid7 = i8;
                                        adj3 = adj8;
                                        schedGroup2 = schedGroup16;
                                        schedGroup14 = scheduleLikeTopApp;
                                    }
                                }
                                is = is2 - 1;
                                logUid6 = conni;
                                procState11 = procState12;
                                str10 = str8;
                                PROCESS_STATE_CUR_TOP = prevAppAdj4;
                                backupTarget = backupTarget3;
                                prevCapability = prevCapability3;
                                prevProcState = prevProcState3;
                                prevAppAdj = prevAppAdj3;
                                state9 = state6;
                                appUid5 = appUid2;
                                hasVisibleActivities = hasVisibleActivities6;
                                psr = psr7;
                                foregroundActivities = foregroundActivities2;
                            }
                        }
                        ppr = app.mProviders;
                        i = procState11;
                        boundByNonBgRestricted2 = boundByNonBgRestricted;
                        provi = ppr.numberOfProviders() - 1;
                        logUid2 = schedGroup3;
                        while (true) {
                            if (provi < 0) {
                                schedGroup4 = logUid2;
                                ppr2 = ppr;
                                z2 = z;
                                schedGroup5 = logUid;
                                psr3 = psr2;
                                capabilityFromFGS2 = capabilityFromFGS;
                                state2 = state;
                                break;
                            } else if (adj3 <= 0 && logUid2 != 0 && i <= 2) {
                                schedGroup4 = logUid2;
                                state2 = state;
                                ppr2 = ppr;
                                schedGroup5 = logUid;
                                psr3 = psr2;
                                capabilityFromFGS2 = capabilityFromFGS;
                                z2 = false;
                                break;
                            } else {
                                ContentProviderRecord cpr3 = ppr.getProviderAt(provi);
                                int schedGroup17 = logUid2;
                                int i9 = i;
                                int procState15 = cpr3.connections.size() - 1;
                                int procState16 = i9;
                                boolean boundByNonBgRestricted6 = boundByNonBgRestricted2;
                                int schedGroup18 = schedGroup17;
                                while (true) {
                                    if (procState15 < 0) {
                                        provi2 = provi;
                                        ppr3 = ppr;
                                        psr4 = psr2;
                                        capabilityFromFGS3 = capabilityFromFGS;
                                        str2 = str11;
                                        i = procState16;
                                        schedGroup7 = schedGroup18;
                                        cpr = cpr3;
                                        procState2 = logUid;
                                        schedGroup8 = adj3;
                                        state3 = state;
                                        str3 = str;
                                        break;
                                    }
                                    if (adj3 <= 0 && schedGroup18 != 0) {
                                        adj5 = adj3;
                                        if (procState16 <= 2) {
                                            i = procState16;
                                            provi2 = provi;
                                            ppr3 = ppr;
                                            procState2 = logUid;
                                            psr4 = psr2;
                                            capabilityFromFGS3 = capabilityFromFGS;
                                            str2 = str11;
                                            schedGroup7 = schedGroup18;
                                            cpr = cpr3;
                                            schedGroup8 = adj5;
                                            state3 = state;
                                            str3 = str;
                                            break;
                                        }
                                    } else {
                                        adj5 = adj3;
                                    }
                                    ContentProviderConnection conn2 = cpr3.connections.get(procState15);
                                    String str12 = str;
                                    ProcessRecord client4 = conn2.client;
                                    int capabilityFromFGS5 = capabilityFromFGS;
                                    ProcessStateRecord cstate2 = client4.mState;
                                    if (client4 == app) {
                                        procState3 = procState16;
                                        schedGroup9 = schedGroup18;
                                        state4 = state;
                                        provi3 = provi;
                                        ppr4 = ppr;
                                        logUid3 = logUid;
                                        psr5 = psr2;
                                        str4 = str11;
                                        adj6 = adj5;
                                        str5 = str12;
                                        adj7 = procState15;
                                        cpr2 = cpr3;
                                    } else {
                                        if (computeClients) {
                                            procState3 = procState16;
                                            psr5 = psr2;
                                            adj6 = adj5;
                                            conn = conn2;
                                            schedGroup9 = schedGroup18;
                                            adj7 = procState15;
                                            state4 = state;
                                            provi3 = provi;
                                            ppr4 = ppr;
                                            str4 = str11;
                                            cpr2 = cpr3;
                                            logUid3 = logUid;
                                            str5 = str12;
                                            client = client4;
                                            computeOomAdjLSP(client4, cachedAdj, topApp, doingAll, now, cycleReEval, true);
                                        } else {
                                            procState3 = procState16;
                                            schedGroup9 = schedGroup18;
                                            state4 = state;
                                            provi3 = provi;
                                            ppr4 = ppr;
                                            logUid3 = logUid;
                                            psr5 = psr2;
                                            str4 = str11;
                                            adj6 = adj5;
                                            str5 = str12;
                                            conn = conn2;
                                            adj7 = procState15;
                                            cpr2 = cpr3;
                                            client = client4;
                                            cstate2.setCurRawAdj(cstate2.getCurAdj());
                                            cstate2.setCurRawProcState(cstate2.getCurProcState());
                                        }
                                        if (!shouldSkipDueToCycle(app, cstate2, procState3, adj6, cycleReEval)) {
                                            int clientAdj2 = cstate2.getCurRawAdj();
                                            int clientProcState4 = cstate2.getCurRawProcState();
                                            if (clientProcState4 >= 16) {
                                                clientProcState4 = 19;
                                            }
                                            if (client.mOptRecord.shouldNotFreeze()) {
                                                app.mOptRecord.setShouldNotFreeze(true);
                                            }
                                            boundByNonBgRestricted6 |= cstate2.isCurBoundByNonBgRestrictedApp() || clientProcState4 <= 3 || (clientProcState4 == 4 && !cstate2.isBackgroundRestricted());
                                            String adjType2 = null;
                                            int adj15 = adj6;
                                            if (adj15 <= clientAdj2) {
                                                state5 = state4;
                                            } else {
                                                if (state4.hasShownUi() && !state4.getCachedIsHomeProcess()) {
                                                    if (clientAdj2 > 200) {
                                                        adjType2 = "cch-ui-provider";
                                                        state5 = state4;
                                                        state5.setCached(state5.isCached() & cstate2.isCached());
                                                    }
                                                }
                                                adj15 = clientAdj2 > 0 ? clientAdj2 : 0;
                                                state5 = state4;
                                                state5.setCurRawAdj(adj15);
                                                adjType2 = "provider";
                                                state5.setCached(state5.isCached() & cstate2.isCached());
                                            }
                                            if (clientProcState4 <= 4) {
                                                if (adjType2 == null) {
                                                    adjType2 = "provider";
                                                }
                                                if (clientProcState4 == 2) {
                                                    clientProcState4 = 3;
                                                } else {
                                                    clientProcState4 = 5;
                                                }
                                            }
                                            conn.trackProcState(clientProcState4, this.mAdjSeq);
                                            int procState17 = procState3;
                                            if (procState17 > clientProcState4) {
                                                procState17 = clientProcState4;
                                                state5.setCurRawProcState(procState17);
                                            }
                                            int schedGroup19 = schedGroup9;
                                            if (cstate2.getCurrentSchedulingGroup() <= schedGroup19) {
                                                schedGroup10 = schedGroup19;
                                            } else {
                                                schedGroup10 = 2;
                                            }
                                            if (adjType2 != null) {
                                                state5.setAdjType(adjType2);
                                                state5.setAdjTypeCode(1);
                                                state5.setAdjSource(client);
                                                state5.setAdjSourceProcState(clientProcState4);
                                                state5.setAdjTarget(cpr2.name);
                                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON) {
                                                    logUid4 = logUid3;
                                                } else {
                                                    logUid4 = logUid3;
                                                    if (logUid4 != appUid) {
                                                        schedGroup11 = schedGroup10;
                                                        str6 = str5;
                                                    }
                                                }
                                                schedGroup11 = schedGroup10;
                                                str6 = str5;
                                                reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, str4 + adjType2 + str6 + app + ", due to " + client + " adj=" + adj15 + " procState=" + ProcessList.makeProcStateString(procState17));
                                            } else {
                                                schedGroup11 = schedGroup10;
                                                logUid4 = logUid3;
                                                str6 = str5;
                                            }
                                            adj3 = adj15;
                                            procState16 = procState17;
                                            schedGroup18 = schedGroup11;
                                            procState15 = adj7 - 1;
                                            str = str6;
                                            logUid = logUid4;
                                            state = state5;
                                            cpr3 = cpr2;
                                            capabilityFromFGS = capabilityFromFGS5;
                                            ppr = ppr4;
                                            provi = provi3;
                                            str11 = str4;
                                            psr2 = psr5;
                                        }
                                    }
                                    procState16 = procState3;
                                    adj3 = adj6;
                                    state5 = state4;
                                    logUid4 = logUid3;
                                    schedGroup18 = schedGroup9;
                                    str6 = str5;
                                    procState15 = adj7 - 1;
                                    str = str6;
                                    logUid = logUid4;
                                    state = state5;
                                    cpr3 = cpr2;
                                    capabilityFromFGS = capabilityFromFGS5;
                                    ppr = ppr4;
                                    provi = provi3;
                                    str11 = str4;
                                    psr2 = psr5;
                                }
                                if (!cpr.hasExternalProcessHandles()) {
                                    z3 = false;
                                    adj3 = schedGroup8;
                                    adj4 = schedGroup7;
                                } else {
                                    if (schedGroup8 <= 0) {
                                        z3 = false;
                                        adj3 = schedGroup8;
                                        adj4 = schedGroup7;
                                    } else {
                                        adj3 = 0;
                                        state3.setCurRawAdj(0);
                                        adj4 = 2;
                                        z3 = false;
                                        state3.setCached(false);
                                        state3.setAdjType("ext-provider");
                                        state3.setAdjTarget(cpr.name);
                                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || procState2 == appUid) {
                                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to external provider: " + app);
                                        }
                                    }
                                    if (i > 6) {
                                        i = 6;
                                        state3.setCurRawProcState(6);
                                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || procState2 == appUid) {
                                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to external provider: " + app);
                                        }
                                    }
                                }
                                logUid = procState2;
                                logUid2 = adj4;
                                str = str3;
                                state = state3;
                                boundByNonBgRestricted2 = boundByNonBgRestricted6;
                                capabilityFromFGS = capabilityFromFGS3;
                                str11 = str2;
                                psr2 = psr4;
                                z = z3;
                                provi = provi2 - 1;
                                ppr = ppr3;
                            }
                        }
                        if (ppr2.getLastProviderTime() > 0 && ppr2.getLastProviderTime() + this.mConstants.CONTENT_PROVIDER_RETAIN_TIME > now) {
                            if (adj3 > 700) {
                                adj3 = 700;
                                state2.setCached(z2);
                                state2.setAdjType("recent-provider");
                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || schedGroup5 == appUid) {
                                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise adj to recent provider: " + app);
                                }
                                schedGroup4 = 0;
                            }
                            if (i > 15) {
                                i = 15;
                                state2.setAdjType("recent-provider");
                                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || schedGroup5 == appUid) {
                                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to recent provider: " + app);
                                }
                            }
                        }
                        if (i >= 19) {
                            if (psr3.hasClientActivities()) {
                                i = 17;
                                state2.setAdjType("cch-client-act");
                            } else if (psr3.isTreatedLikeActivity()) {
                                i = 16;
                                state2.setAdjType("cch-as-act");
                            }
                        }
                        if (adj3 == 500) {
                            if (doingAll && !cycleReEval) {
                                state2.setServiceB(this.mNewNumAServiceProcs > this.mNumServiceProcs / 3 ? true : z2);
                                this.mNewNumServiceProcs++;
                                if (!state2.isServiceB()) {
                                    if (this.mService.mAppProfiler.isLastMemoryLevelNormal()) {
                                        i2 = 1;
                                    } else if (app.mProfile.getLastPss() < this.mProcessList.getCachedRestoreThresholdKb()) {
                                        i2 = 1;
                                    } else {
                                        state2.setServiceHighRam(true);
                                        state2.setServiceB(true);
                                    }
                                    this.mNewNumAServiceProcs += i2;
                                } else {
                                    state2.setServiceHighRam(z2);
                                }
                            }
                            if (state2.isServiceB()) {
                                adj3 = 800;
                            }
                        }
                        state2.setCurRawAdj(adj3);
                        if (adj3 <= state2.getMaxAdj()) {
                            schedGroup6 = schedGroup4;
                        } else {
                            adj3 = state2.getMaxAdj();
                            if (adj3 > 250) {
                                schedGroup6 = schedGroup4;
                            } else {
                                schedGroup6 = 2;
                            }
                        }
                        if (i < 5 && this.mService.mWakefulness.get() != 1 && !schedGroup14 && schedGroup6 > 1) {
                            schedGroup6 = 1;
                        }
                        if (psr3.hasForegroundServices()) {
                            capability3 |= capabilityFromFGS2;
                        }
                        ProcessServiceRecord psr12 = psr3;
                        state2.setCurAdj(psr12.modifyRawOomAdj(adj3));
                        state2.setCurCapability(capability3 | getDefaultCapability(psr12, i));
                        state2.setCurrentSchedulingGroup(schedGroup6);
                        state2.setCurProcState(i);
                        state2.setCurRawProcState(i);
                        state2.updateLastInvisibleTime(hasVisibleActivities2);
                        state2.setHasForegroundActivities(foregroundActivities2);
                        state2.setCompletedAdjSeq(this.mAdjSeq);
                        state2.setCurBoundByNonBgRestrictedApp(boundByNonBgRestricted2);
                        return state2.getCurAdj() < prevAppAdj2 || state2.getCurProcState() < prevProcState2 || state2.getCurCapability() != prevCapability2;
                    }
                    procState11 = 15;
                    state9.setAdjType("previous");
                    if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                        reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise procstate to prev: " + app);
                    }
                }
                schedGroup2 = schedGroup13;
                if (cycleReEval) {
                }
                state9.setCurRawAdj(adj2);
                state9.setCurRawProcState(procState11);
                state9.setHasStartedServices(false);
                state9.setAdjSeq(this.mAdjSeq);
                backupTarget = this.mService.mBackupTargets.get(app.userId);
                if (backupTarget != null) {
                    if (adj2 > 300) {
                    }
                    if (procState11 > 9) {
                    }
                }
                boolean boundByNonBgRestricted42 = state9.isCurBoundByNonBgRestrictedApp();
                int i52 = adj2;
                capabilityFromFGS = 0;
                adj3 = i52;
                int i62 = capability;
                boolean schedGroup142 = false;
                is = psr.numberOfRunningServices() - 1;
                int capability32 = i62;
                while (true) {
                    foregroundActivities2 = foregroundActivities;
                    if (is < 0) {
                    }
                    is = is2 - 1;
                    logUid6 = conni;
                    procState11 = procState12;
                    str10 = str8;
                    PROCESS_STATE_CUR_TOP = prevAppAdj4;
                    backupTarget = backupTarget3;
                    prevCapability = prevCapability3;
                    prevProcState = prevProcState3;
                    prevAppAdj = prevAppAdj3;
                    state9 = state6;
                    appUid5 = appUid2;
                    hasVisibleActivities = hasVisibleActivities6;
                    psr = psr7;
                    foregroundActivities = foregroundActivities2;
                }
                ppr = app.mProviders;
                i = procState11;
                boundByNonBgRestricted2 = boundByNonBgRestricted;
                provi = ppr.numberOfProviders() - 1;
                logUid2 = schedGroup3;
                while (true) {
                    if (provi < 0) {
                    }
                    logUid = procState2;
                    logUid2 = adj4;
                    str = str3;
                    state = state3;
                    boundByNonBgRestricted2 = boundByNonBgRestricted6;
                    capabilityFromFGS = capabilityFromFGS3;
                    str11 = str2;
                    psr2 = psr4;
                    z = z3;
                    provi = provi2 - 1;
                    ppr = ppr3;
                }
                if (ppr2.getLastProviderTime() > 0) {
                    if (adj3 > 700) {
                    }
                    if (i > 15) {
                    }
                }
                if (i >= 19) {
                }
                if (adj3 == 500) {
                }
                state2.setCurRawAdj(adj3);
                if (adj3 <= state2.getMaxAdj()) {
                }
                if (i < 5) {
                    schedGroup6 = 1;
                }
                if (psr3.hasForegroundServices()) {
                }
                ProcessServiceRecord psr122 = psr3;
                state2.setCurAdj(psr122.modifyRawOomAdj(adj3));
                state2.setCurCapability(capability32 | getDefaultCapability(psr122, i));
                state2.setCurrentSchedulingGroup(schedGroup6);
                state2.setCurProcState(i);
                state2.setCurRawProcState(i);
                state2.updateLastInvisibleTime(hasVisibleActivities2);
                state2.setHasForegroundActivities(foregroundActivities2);
                state2.setCompletedAdjSeq(this.mAdjSeq);
                state2.setCurBoundByNonBgRestrictedApp(boundByNonBgRestricted2);
                if (state2.getCurAdj() < prevAppAdj2) {
                }
            }
            if (psr.hasForegroundServices()) {
                adj2 = 200;
                procState11 = 4;
                state9.setAdjType("fg-service");
                state9.setCached(false);
                schedGroup13 = 2;
                if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                    prevProcState = prevProcState6;
                    prevAppAdj = prevAppAdj9;
                    reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to " + state9.getAdjType() + ": " + app + " ");
                } else {
                    prevProcState = prevProcState6;
                    prevAppAdj = prevAppAdj9;
                }
            } else {
                prevProcState = prevProcState6;
                prevAppAdj = prevAppAdj9;
                if (state9.hasOverlayUi()) {
                    adj2 = 200;
                    procState11 = 6;
                    state9.setCached(false);
                    state9.setAdjType("has-overlay-ui");
                    schedGroup13 = 2;
                    if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON || logUid6 == appUid5) {
                        reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to overlay ui: " + app);
                    }
                }
            }
            if (psr.hasForegroundServices()) {
                adj2 = 50;
                state9.setAdjType("fg-service-act");
                if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON) {
                }
                reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to recent fg: " + app);
            }
            if (!psr.hasTopStartedAlmostPerceptibleServices()) {
            }
            PROCESS_STATE_CUR_TOP = PROCESS_STATE_CUR_TOP2;
            capability = capability2;
            if (adj2 <= 200) {
            }
            adj2 = 200;
            procState11 = 8;
            state9.setCached(false);
            state9.setAdjType("force-imp");
            state9.setAdjSource(state9.getForcingToImportant());
            schedGroup13 = 2;
            if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON) {
            }
            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, "Raise to force imp: " + app);
            if (state9.getCachedIsHeavyWeight()) {
            }
            if (state9.getCachedIsHomeProcess()) {
            }
            if (state9.getCachedIsPreviousProcess()) {
                if (adj2 > 700) {
                }
                if (procState11 > 15) {
                }
            }
            schedGroup2 = schedGroup13;
            if (cycleReEval) {
            }
            state9.setCurRawAdj(adj2);
            state9.setCurRawProcState(procState11);
            state9.setHasStartedServices(false);
            state9.setAdjSeq(this.mAdjSeq);
            backupTarget = this.mService.mBackupTargets.get(app.userId);
            if (backupTarget != null) {
            }
            boolean boundByNonBgRestricted422 = state9.isCurBoundByNonBgRestrictedApp();
            int i522 = adj2;
            capabilityFromFGS = 0;
            adj3 = i522;
            int i622 = capability;
            boolean schedGroup1422 = false;
            is = psr.numberOfRunningServices() - 1;
            int capability322 = i622;
            while (true) {
                foregroundActivities2 = foregroundActivities;
                if (is < 0) {
                }
                is = is2 - 1;
                logUid6 = conni;
                procState11 = procState12;
                str10 = str8;
                PROCESS_STATE_CUR_TOP = prevAppAdj4;
                backupTarget = backupTarget3;
                prevCapability = prevCapability3;
                prevProcState = prevProcState3;
                prevAppAdj = prevAppAdj3;
                state9 = state6;
                appUid5 = appUid2;
                hasVisibleActivities = hasVisibleActivities6;
                psr = psr7;
                foregroundActivities = foregroundActivities2;
            }
            ppr = app.mProviders;
            i = procState11;
            boundByNonBgRestricted2 = boundByNonBgRestricted;
            provi = ppr.numberOfProviders() - 1;
            logUid2 = schedGroup3;
            while (true) {
                if (provi < 0) {
                }
                logUid = procState2;
                logUid2 = adj4;
                str = str3;
                state = state3;
                boundByNonBgRestricted2 = boundByNonBgRestricted6;
                capabilityFromFGS = capabilityFromFGS3;
                str11 = str2;
                psr2 = psr4;
                z = z3;
                provi = provi2 - 1;
                ppr = ppr3;
            }
            if (ppr2.getLastProviderTime() > 0) {
            }
            if (i >= 19) {
            }
            if (adj3 == 500) {
            }
            state2.setCurRawAdj(adj3);
            if (adj3 <= state2.getMaxAdj()) {
            }
            if (i < 5) {
            }
            if (psr3.hasForegroundServices()) {
            }
            ProcessServiceRecord psr1222 = psr3;
            state2.setCurAdj(psr1222.modifyRawOomAdj(adj3));
            state2.setCurCapability(capability322 | getDefaultCapability(psr1222, i));
            state2.setCurrentSchedulingGroup(schedGroup6);
            state2.setCurProcState(i);
            state2.setCurRawProcState(i);
            state2.updateLastInvisibleTime(hasVisibleActivities2);
            state2.setHasForegroundActivities(foregroundActivities2);
            state2.setCompletedAdjSeq(this.mAdjSeq);
            state2.setCurBoundByNonBgRestrictedApp(boundByNonBgRestricted2);
            if (state2.getCurAdj() < prevAppAdj2) {
            }
        }
    }

    private int getDefaultCapability(ProcessServiceRecord psr, int procState) {
        if ("com.sh.smart.caller".equals(psr.mApp.processName)) {
            return 15;
        }
        switch (procState) {
            case 0:
            case 1:
            case 2:
                return 15;
            case 3:
                return 8;
            case 4:
                if (psr.hasForegroundServices()) {
                    return 8;
                }
                return 14;
            case 5:
                return 8;
            default:
                return 0;
        }
    }

    private boolean shouldSkipDueToCycle(ProcessRecord app, ProcessStateRecord client, int procState, int adj, boolean cycleReEval) {
        if (client.containsCycle()) {
            app.mState.setContainsCycle(true);
            this.mProcessesInCycle.add(app);
            if (client.getCompletedAdjSeq() < this.mAdjSeq) {
                if (cycleReEval) {
                    return client.getCurRawProcState() >= procState && client.getCurRawAdj() >= adj;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportOomAdjMessageLocked(String tag, String msg) {
        Slog.d(tag, msg);
        synchronized (this.mService.mOomAdjObserverLock) {
            if (this.mService.mCurOomAdjObserver != null) {
                this.mService.mUiHandler.obtainMessage(70, msg).sendToTarget();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWakefulnessChanged(int wakefulness) {
        this.mCachedAppOptimizer.onWakefulnessChanged(wakefulness);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2804=4] */
    /* JADX WARN: Code restructure failed: missing block: B:144:0x034e, code lost:
        if (r0 != false) goto L66;
     */
    /* JADX WARN: Code restructure failed: missing block: B:153:0x035f, code lost:
        if (com.android.server.am.OomAdjuster.IS_ROOT_ENABLE == false) goto L77;
     */
    /* JADX WARN: Code restructure failed: missing block: B:154:0x0361, code lost:
        r21.mStl.sendBinderMessage();
     */
    /* JADX WARN: Removed duplicated region for block: B:117:0x02ba A[Catch: Exception -> 0x02bf, TRY_LEAVE, TryCatch #6 {Exception -> 0x02bf, blocks: (B:117:0x02ba, B:115:0x02b5), top: B:265:0x02b5 }] */
    /* JADX WARN: Removed duplicated region for block: B:129:0x02d0  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x030b  */
    /* JADX WARN: Removed duplicated region for block: B:135:0x0316  */
    /* JADX WARN: Removed duplicated region for block: B:138:0x0324  */
    /* JADX WARN: Removed duplicated region for block: B:157:0x036f  */
    /* JADX WARN: Removed duplicated region for block: B:163:0x0389  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x03e0 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean applyOomAdjLSP(final ProcessRecord app, boolean doingAll, long now, long nowElapsed) {
        boolean success;
        int changes;
        boolean forceUpdatePssTime;
        int processGroup;
        ProcessStateRecord state = app.mState;
        UidRecord uidRec = app.getUidRecord();
        if (state.getCurRawAdj() != state.getSetRawAdj()) {
            state.setSetRawAdj(state.getCurRawAdj());
        }
        if (this.mCachedAppOptimizer.useCompaction() && this.mService.mBooted) {
            if (state.getCurAdj() != state.getSetAdj()) {
                this.mCachedAppOptimizer.onOomAdjustChanged(state.getSetAdj(), state.getCurAdj(), app);
            } else if (this.mService.mWakefulness.get() != 1) {
                if (state.getSetAdj() < 0 && !state.isRunningRemoteAnimation() && this.mCachedAppOptimizer.shouldCompactPersistent(app, now)) {
                    this.mCachedAppOptimizer.compactAppPersistent(app);
                } else if (state.getCurProcState() == 5 && this.mCachedAppOptimizer.shouldCompactBFGS(app, now)) {
                    this.mCachedAppOptimizer.compactAppBfgs(app);
                }
            }
        }
        if (state.getCurAdj() != state.getSetAdj()) {
            ProcessList.setOomAdj(app.getPid(), app.uid, state.getCurAdj());
            if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ || this.mService.mCurOomAdjUid == app.info.uid) {
                String msg = "Set " + app.getPid() + " " + app.processName + " adj " + state.getCurAdj() + ": " + state.getAdjType();
                reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, msg);
            }
            state.setSetAdj(state.getCurAdj());
            if (uidRec != null) {
                uidRec.noteProcAdjChanged();
            }
            state.setVerifiedAdj(-10000);
        }
        int curSchedGroup = state.getCurrentSchedulingGroup();
        if (state.getSetSchedGroup() != curSchedGroup) {
            int oldSchedGroup = state.getSetSchedGroup();
            state.setSetSchedGroup(curSchedGroup);
            if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ || this.mService.mCurOomAdjUid == app.uid) {
                String msg2 = "Setting sched group of " + app.processName + " to " + curSchedGroup + ": " + state.getAdjType();
                reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, msg2);
            }
            String msg3 = app.getWaitingToKill();
            if (msg3 != null && app.mReceivers.numberOfCurReceivers() == 0 && state.getSetSchedGroup() == 0) {
                app.killLocked(app.getWaitingToKill(), 10, 22, true);
                success = false;
            } else {
                switch (curSchedGroup) {
                    case 0:
                        processGroup = 0;
                        break;
                    case 1:
                        processGroup = 7;
                        break;
                    case 2:
                    default:
                        processGroup = -1;
                        break;
                    case 3:
                    case 4:
                        processGroup = 5;
                        break;
                }
                Handler handler = this.mProcessGroupHandler;
                success = true;
                handler.sendMessage(handler.obtainMessage(0, app.getPid(), processGroup, app.processName));
                try {
                    int renderThreadTid = app.getRenderThreadTid();
                    try {
                        if (curSchedGroup == 3) {
                            if (oldSchedGroup != 3) {
                                app.getWindowProcessController().onTopProcChanged();
                                if (this.mService.mUseFifoUiScheduling) {
                                    state.setSavedPriority(Process.getThreadPriority(app.getPid()));
                                    ActivityManagerService.scheduleAsFifoPriority(app.getPid(), true);
                                    if (renderThreadTid != 0) {
                                        ActivityManagerService.scheduleAsFifoPriority(renderThreadTid, true);
                                        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                            Slog.d("UI_FIFO", "Set RenderThread (TID " + renderThreadTid + ") to FIFO");
                                        }
                                    } else if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                                        Slog.d("UI_FIFO", "Not setting RenderThread TID");
                                    }
                                } else {
                                    if (ITranOomAdjuster.Instance().isMultiKillEnabled(app.processName)) {
                                        ITranProcessList.Instance().do_AOT_multikill();
                                    }
                                    Process.setThreadPriority(app.getPid(), -10);
                                    if (renderThreadTid != 0) {
                                        try {
                                            Process.setThreadPriority(renderThreadTid, -10);
                                        } catch (IllegalArgumentException e) {
                                        }
                                    }
                                }
                            }
                        } else if (oldSchedGroup == 3 && curSchedGroup != 3) {
                            app.getWindowProcessController().onTopProcChanged();
                            try {
                                if (this.mService.mUseFifoUiScheduling) {
                                    try {
                                        try {
                                            try {
                                                try {
                                                    Process.setThreadScheduler(app.getPid(), 0, 0);
                                                    Process.setThreadPriority(app.getPid(), state.getSavedPriority());
                                                    if (renderThreadTid != 0) {
                                                        Process.setThreadScheduler(renderThreadTid, 0, 0);
                                                    }
                                                } catch (Exception e2) {
                                                    e = e2;
                                                    if (ActivityManagerDebugConfig.DEBUG_ALL) {
                                                        Slog.w(TAG, "Failed setting thread priority of " + app.getPid(), e);
                                                    }
                                                    ITranWindowManagerService.Instance().wmsSetProcessGroup(app.uid, app.getPid(), processGroup, app.info.packageName);
                                                    if (state.hasRepForegroundActivities() == state.hasForegroundActivities()) {
                                                    }
                                                    updateAppFreezeStateLSP(app);
                                                    if (state.getReportedProcState() != state.getCurProcState()) {
                                                    }
                                                    if (state.getSetProcState() != 20) {
                                                    }
                                                    state.setLastStateTime(now);
                                                    if (ActivityManagerDebugConfig.DEBUG_PSS) {
                                                    }
                                                    forceUpdatePssTime = true;
                                                    synchronized (this.mService.mAppProfiler.mProfilerLock) {
                                                    }
                                                }
                                            } catch (IllegalArgumentException e3) {
                                                e = e3;
                                                Slog.w(TAG, "Failed to set scheduling policy, thread does not exist:\n" + e);
                                                if (renderThreadTid != 0) {
                                                }
                                                ITranWindowManagerService.Instance().wmsSetProcessGroup(app.uid, app.getPid(), processGroup, app.info.packageName);
                                                if (state.hasRepForegroundActivities() == state.hasForegroundActivities()) {
                                                }
                                                updateAppFreezeStateLSP(app);
                                                if (state.getReportedProcState() != state.getCurProcState()) {
                                                }
                                                if (state.getSetProcState() != 20) {
                                                }
                                                state.setLastStateTime(now);
                                                if (ActivityManagerDebugConfig.DEBUG_PSS) {
                                                }
                                                forceUpdatePssTime = true;
                                                synchronized (this.mService.mAppProfiler.mProfilerLock) {
                                                }
                                            }
                                        } catch (IllegalArgumentException e4) {
                                            e = e4;
                                        }
                                    } catch (SecurityException e5) {
                                        Slog.w(TAG, "Failed to set scheduling policy, not allowed:\n" + e5);
                                    }
                                    if (renderThreadTid != 0) {
                                        Process.setThreadPriority(renderThreadTid, -4);
                                    }
                                } else {
                                    try {
                                        Process.setThreadPriority(app.getPid(), 0);
                                    } catch (Exception e6) {
                                        e = e6;
                                        if (ActivityManagerDebugConfig.DEBUG_ALL) {
                                        }
                                        ITranWindowManagerService.Instance().wmsSetProcessGroup(app.uid, app.getPid(), processGroup, app.info.packageName);
                                        if (state.hasRepForegroundActivities() == state.hasForegroundActivities()) {
                                        }
                                        updateAppFreezeStateLSP(app);
                                        if (state.getReportedProcState() != state.getCurProcState()) {
                                        }
                                        if (state.getSetProcState() != 20) {
                                        }
                                        state.setLastStateTime(now);
                                        if (ActivityManagerDebugConfig.DEBUG_PSS) {
                                        }
                                        forceUpdatePssTime = true;
                                        synchronized (this.mService.mAppProfiler.mProfilerLock) {
                                        }
                                    }
                                }
                                if (renderThreadTid != 0) {
                                }
                            } catch (Exception e7) {
                                e = e7;
                            }
                        }
                    } catch (Exception e8) {
                        e = e8;
                    }
                } catch (Exception e9) {
                    e = e9;
                }
                ITranWindowManagerService.Instance().wmsSetProcessGroup(app.uid, app.getPid(), processGroup, app.info.packageName);
            }
        } else {
            success = true;
        }
        if (state.hasRepForegroundActivities() == state.hasForegroundActivities()) {
            state.setRepForegroundActivities(state.hasForegroundActivities());
            int changes2 = 0 | 1;
            changes = changes2;
        } else {
            changes = 0;
        }
        updateAppFreezeStateLSP(app);
        if (state.getReportedProcState() != state.getCurProcState()) {
            state.setReportedProcState(state.getCurProcState());
            if (app.getThread() != null) {
                try {
                    boolean z = IS_ROOT_ENABLE;
                    if (z) {
                        this.mStl.sendNoBinderMessage(app.getPid(), app.processName, "setProcessState");
                    }
                    app.getThread().setProcessState(state.getReportedProcState());
                } catch (RemoteException e10) {
                } catch (Throwable th) {
                    if (IS_ROOT_ENABLE) {
                        this.mStl.sendBinderMessage();
                    }
                    throw th;
                }
            }
        }
        if (state.getSetProcState() != 20 || ProcessList.procStatesDifferForMem(state.getCurProcState(), state.getSetProcState())) {
            state.setLastStateTime(now);
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(AppProfiler.TAG_PSS, "Process state change from " + ProcessList.makeProcStateString(state.getSetProcState()) + " to " + ProcessList.makeProcStateString(state.getCurProcState()) + " next pss in " + (app.mProfile.getNextPssTime() - now) + ": " + app);
            }
            forceUpdatePssTime = true;
        } else {
            forceUpdatePssTime = false;
        }
        synchronized (this.mService.mAppProfiler.mProfilerLock) {
            try {
                try {
                    app.mProfile.updateProcState(app.mState);
                    this.mService.mAppProfiler.updateNextPssTimeLPf(state.getCurProcState(), app.mProfile, now, forceUpdatePssTime);
                    if (state.getSetProcState() != state.getCurProcState()) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ || this.mService.mCurOomAdjUid == app.uid) {
                            String msg4 = "Proc state change of " + app.processName + " to " + ProcessList.makeProcStateString(state.getCurProcState()) + " (" + state.getCurProcState() + "): " + state.getAdjType();
                            reportOomAdjMessageLocked(ActivityManagerService.TAG_OOM_ADJ, msg4);
                        }
                        boolean setImportant = state.getSetProcState() < 10;
                        boolean curImportant = state.getCurProcState() < 10;
                        if (setImportant && !curImportant) {
                            state.setWhenUnimportant(now);
                            app.mProfile.mLastCpuTime.set(0L);
                        }
                        maybeUpdateUsageStatsLSP(app, nowElapsed);
                        maybeUpdateLastTopTime(state, now);
                        state.setSetProcState(state.getCurProcState());
                        if (state.getSetProcState() >= 14) {
                            state.setNotCachedSinceIdle(false);
                        }
                        if (doingAll) {
                            state.setProcStateChanged(true);
                        } else {
                            synchronized (this.mService.mProcessStats.mLock) {
                                ActivityManagerService activityManagerService = this.mService;
                                activityManagerService.setProcessTrackerStateLOSP(app, activityManagerService.mProcessStats.getMemFactorLocked());
                            }
                        }
                    } else if (state.hasReportedInteraction()) {
                        boolean fgsInteractionChangeEnabled = state.getCachedCompatChange(2);
                        long interactionThreshold = fgsInteractionChangeEnabled ? this.mConstants.USAGE_STATS_INTERACTION_INTERVAL_POST_S : this.mConstants.USAGE_STATS_INTERACTION_INTERVAL_PRE_S;
                        if (nowElapsed - state.getInteractionEventTime() > interactionThreshold) {
                            maybeUpdateUsageStatsLSP(app, nowElapsed);
                        }
                    } else {
                        boolean fgsInteractionChangeEnabled2 = state.getCachedCompatChange(2);
                        long interactionThreshold2 = fgsInteractionChangeEnabled2 ? this.mConstants.SERVICE_USAGE_INTERACTION_TIME_POST_S : this.mConstants.SERVICE_USAGE_INTERACTION_TIME_PRE_S;
                        if (nowElapsed - state.getFgInteractionTime() > interactionThreshold2) {
                            maybeUpdateUsageStatsLSP(app, nowElapsed);
                        }
                    }
                    if (state.getCurCapability() != state.getSetCapability()) {
                        changes |= 4;
                        state.setSetCapability(state.getCurCapability());
                    }
                    boolean curBoundByNonBgRestrictedApp = state.isCurBoundByNonBgRestrictedApp();
                    if (curBoundByNonBgRestrictedApp != state.isSetBoundByNonBgRestrictedApp()) {
                        state.setSetBoundByNonBgRestrictedApp(curBoundByNonBgRestrictedApp);
                        if (!curBoundByNonBgRestrictedApp && state.isBackgroundRestricted()) {
                            this.mService.mHandler.post(new Runnable() { // from class: com.android.server.am.OomAdjuster$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    OomAdjuster.this.m1442lambda$applyOomAdjLSP$1$comandroidserveramOomAdjuster(app);
                                }
                            });
                        }
                    }
                    if (changes != 0) {
                        if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                            Slog.i(ProcessList.TAG_PROCESS_OBSERVERS, "Changes in " + app + ": " + changes);
                        }
                        ActivityManagerService.ProcessChangeItem item = this.mProcessList.enqueueProcessChangeItemLocked(app.getPid(), app.info.uid);
                        item.changes |= changes;
                        item.foregroundActivities = state.hasRepForegroundActivities();
                        item.capability = state.getSetCapability();
                        if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                            Slog.i(ProcessList.TAG_PROCESS_OBSERVERS, "Item " + Integer.toHexString(System.identityHashCode(item)) + " " + app.toShortString() + ": changes=" + item.changes + " foreground=" + item.foregroundActivities + " type=" + state.getAdjType() + " source=" + state.getAdjSource() + " target=" + state.getAdjTarget() + " capability=" + item.capability);
                        }
                    }
                    if (state.isCached() && !state.shouldNotKillOnBgRestrictedAndIdle() && (!state.isSetCached() || state.isSetNoKillOnBgRestrictedAndIdle())) {
                        state.setLastCanKillOnBgRestrictedAndIdleTime(nowElapsed);
                        if (!this.mService.mHandler.hasMessages(58)) {
                            this.mService.mHandler.sendEmptyMessageDelayed(58, this.mConstants.mKillBgRestrictedAndCachedIdleSettleTimeMs);
                        }
                    }
                    state.setSetCached(state.isCached());
                    state.setSetNoKillOnBgRestrictedAndIdle(state.shouldNotKillOnBgRestrictedAndIdle());
                    return success;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyOomAdjLSP$1$com-android-server-am-OomAdjuster  reason: not valid java name */
    public /* synthetic */ void m1442lambda$applyOomAdjLSP$1$comandroidserveramOomAdjuster(ProcessRecord app) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mServices.stopAllForegroundServicesLocked(app.uid, app.info.packageName);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAttachingSchedGroupLSP(ProcessRecord app) {
        int initialSchedGroup = 2;
        ProcessStateRecord state = app.mState;
        if (state.hasForegroundActivities()) {
            try {
                app.getWindowProcessController().onTopProcChanged();
                Process.setThreadPriority(app.getPid(), -10);
                initialSchedGroup = 3;
            } catch (Exception e) {
                Slog.w(TAG, "Failed to pre-set top priority to " + app + " " + e);
            }
        }
        state.setSetSchedGroup(initialSchedGroup);
        state.setCurrentSchedulingGroup(initialSchedGroup);
    }

    void maybeUpdateUsageStats(ProcessRecord app, long nowElapsed) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                synchronized (this.mProcLock) {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    maybeUpdateUsageStatsLSP(app, nowElapsed);
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void maybeUpdateUsageStatsLSP(ProcessRecord app, long nowElapsed) {
        boolean isInteraction;
        long interactionTime;
        long interactionThreshold;
        ProcessStateRecord state = app.mState;
        if (ActivityManagerDebugConfig.DEBUG_USAGE_STATS) {
            Slog.d(TAG, "Checking proc [" + Arrays.toString(app.getPackageList()) + "] state changes: old = " + state.getSetProcState() + ", new = " + state.getCurProcState());
        }
        if (this.mService.mUsageStatsService == null) {
            return;
        }
        boolean fgsInteractionChangeEnabled = state.getCachedCompatChange(2);
        if (ActivityManager.isProcStateConsideredInteraction(state.getCurProcState())) {
            isInteraction = true;
            state.setFgInteractionTime(0L);
        } else if (state.getCurProcState() <= 4) {
            if (state.getFgInteractionTime() == 0) {
                state.setFgInteractionTime(nowElapsed);
                isInteraction = false;
            } else {
                if (fgsInteractionChangeEnabled) {
                    interactionTime = this.mConstants.SERVICE_USAGE_INTERACTION_TIME_POST_S;
                } else {
                    interactionTime = this.mConstants.SERVICE_USAGE_INTERACTION_TIME_PRE_S;
                }
                isInteraction = nowElapsed > state.getFgInteractionTime() + interactionTime;
            }
        } else {
            isInteraction = state.getCurProcState() <= 6;
            state.setFgInteractionTime(0L);
        }
        if (fgsInteractionChangeEnabled) {
            interactionThreshold = this.mConstants.USAGE_STATS_INTERACTION_INTERVAL_POST_S;
        } else {
            interactionThreshold = this.mConstants.USAGE_STATS_INTERACTION_INTERVAL_PRE_S;
        }
        if (isInteraction && (!state.hasReportedInteraction() || nowElapsed - state.getInteractionEventTime() > interactionThreshold)) {
            state.setInteractionEventTime(nowElapsed);
            String[] packages = app.getPackageList();
            if (packages != null) {
                for (String str : packages) {
                    this.mService.mUsageStatsService.reportEvent(str, app.userId, 6);
                }
            }
        }
        state.setReportedInteraction(isInteraction);
        if (!isInteraction) {
            state.setInteractionEventTime(0L);
        }
    }

    private void maybeUpdateLastTopTime(ProcessStateRecord state, long nowUptime) {
        if (state.getSetProcState() <= 2 && state.getCurProcState() > 2) {
            state.setLastTopTime(nowUptime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void idleUidsLocked() {
        int N = this.mActiveUids.size();
        if (N <= 0) {
            return;
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        long maxBgTime = nowElapsed - this.mConstants.BACKGROUND_SETTLE_TIME;
        PowerManagerInternal powerManagerInternal = this.mLocalPowerManager;
        if (powerManagerInternal != null) {
            powerManagerInternal.startUidChanges();
        }
        long nextTime = 0;
        for (int i = N - 1; i >= 0; i--) {
            UidRecord uidRec = this.mActiveUids.valueAt(i);
            long bgTime = uidRec.getLastBackgroundTime();
            if (bgTime > 0 && !uidRec.isIdle()) {
                if (bgTime <= maxBgTime) {
                    EventLogTags.writeAmUidIdle(uidRec.getUid());
                    synchronized (this.mProcLock) {
                        try {
                            ActivityManagerService.boostPriorityForProcLockedSection();
                            uidRec.setIdle(true);
                            uidRec.setSetIdle(true);
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    ITranOomAdjuster.Instance().hookUidChangedIdle(uidRec.getUid());
                    this.mService.doStopUidLocked(uidRec.getUid(), uidRec);
                } else if (nextTime == 0 || nextTime > bgTime) {
                    nextTime = bgTime;
                }
            }
        }
        PowerManagerInternal powerManagerInternal2 = this.mLocalPowerManager;
        if (powerManagerInternal2 != null) {
            powerManagerInternal2.finishUidChanges();
        }
        if (this.mService.mConstants.mKillBgRestrictedAndCachedIdle) {
            ArraySet<ProcessRecord> apps = this.mProcessList.mAppsInBackgroundRestricted;
            int size = apps.size();
            for (int i2 = 0; i2 < size; i2++) {
                long bgTime2 = this.mProcessList.m1472x5c5ad3cc(apps.valueAt(i2), nowElapsed) - this.mConstants.BACKGROUND_SETTLE_TIME;
                if (bgTime2 > 0 && (nextTime == 0 || nextTime > bgTime2)) {
                    nextTime = bgTime2;
                }
            }
        }
        if (nextTime > 0) {
            this.mService.mHandler.removeMessages(58);
            this.mService.mHandler.sendEmptyMessageDelayed(58, (this.mConstants.BACKGROUND_SETTLE_TIME + nextTime) - nowElapsed);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAppIdTempAllowlistStateLSP(int uid, boolean onAllowlist) {
        boolean changed = false;
        for (int i = this.mActiveUids.size() - 1; i >= 0; i--) {
            UidRecord uidRec = this.mActiveUids.valueAt(i);
            if (uidRec.getUid() == uid && uidRec.isCurAllowListed() != onAllowlist) {
                uidRec.setCurAllowListed(onAllowlist);
                changed = true;
            }
        }
        if (changed) {
            updateOomAdjLSP(OOM_ADJ_REASON_ALLOWLIST);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUidTempAllowlistStateLSP(int uid, boolean onAllowlist) {
        UidRecord uidRec = this.mActiveUids.get(uid);
        if (uidRec != null && uidRec.isCurAllowListed() != onAllowlist) {
            uidRec.setCurAllowListed(onAllowlist);
            updateOomAdjLSP(OOM_ADJ_REASON_ALLOWLIST);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProcessListVariablesLocked(ProtoOutputStream proto) {
        proto.write(1120986464305L, this.mAdjSeq);
        proto.write(1120986464306L, this.mProcessList.getLruSeqLOSP());
        proto.write(1120986464307L, this.mNumNonCachedProcs);
        proto.write(1120986464309L, this.mNumServiceProcs);
        proto.write(1120986464310L, this.mNewNumServiceProcs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpSequenceNumbersLocked(PrintWriter pw) {
        pw.println("  mAdjSeq=" + this.mAdjSeq + " mLruSeq=" + this.mProcessList.getLruSeqLOSP());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProcCountsLocked(PrintWriter pw) {
        pw.println("  mNumNonCachedProcs=" + this.mNumNonCachedProcs + " (" + this.mProcessList.getLruSizeLOSP() + " total) mNumCachedHiddenProcs=" + this.mNumCachedHiddenProcs + " mNumServiceProcs=" + this.mNumServiceProcs + " mNewNumServiceProcs=" + this.mNewNumServiceProcs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpCachedAppOptimizerSettings(PrintWriter pw) {
        this.mCachedAppOptimizer.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpCacheOomRankerSettings(PrintWriter pw) {
        this.mCacheOomRanker.dump(pw);
    }

    private void updateAppFreezeStateLSP(ProcessRecord app) {
        if (!this.mCachedAppOptimizer.useFreezer() || app.mOptRecord.isFreezeExempt()) {
            return;
        }
        ProcessCachedOptimizerRecord opt = app.mOptRecord;
        if (opt.isFrozen() && opt.shouldNotFreeze()) {
            this.mCachedAppOptimizer.unfreezeAppLSP(app);
            return;
        }
        ProcessStateRecord state = app.mState;
        if (state.getCurAdj() >= 900 && !opt.isFrozen() && !opt.shouldNotFreeze()) {
            this.mCachedAppOptimizer.freezeAppAsyncLSP(app);
        } else if (state.getSetAdj() < 900) {
            this.mCachedAppOptimizer.unfreezeAppLSP(app);
        }
    }

    public void updateWallpaperSchedGroup(boolean isAnimationRunning) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ProcessRecord wallpaperApp = getMagicTouchWallpaper();
                if (wallpaperApp != null) {
                    ProcessStateRecord state = wallpaperApp.mState;
                    if (state.getCurrentSchedulingGroup() == 1) {
                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                        return;
                    } else if (!isAnimationRunning) {
                        Slog.d(TAG, "updateWallpaperSchedGroup force background");
                        Handler handler = this.mProcessGroupHandler;
                        handler.sendMessage(handler.obtainMessage(0, wallpaperApp.getPid(), 0, wallpaperApp.processName));
                    } else {
                        Slog.d(TAG, "updateWallpaperSchedGroup to default");
                        Handler handler2 = this.mProcessGroupHandler;
                        handler2.sendMessage(handler2.obtainMessage(0, wallpaperApp.getPid(), -1, wallpaperApp.processName));
                    }
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
    }

    private ProcessRecord getMagicTouchWallpaper() {
        ArrayList<ProcessRecord> activeProcesses = this.mProcessList.getLruProcessesLOSP();
        int numProc = activeProcesses.size();
        for (int i = numProc - 1; i >= 0; i--) {
            ProcessRecord app = activeProcesses.get(i);
            if (app.isMagicTouchWallpaper()) {
                return app;
            }
        }
        return null;
    }
}
