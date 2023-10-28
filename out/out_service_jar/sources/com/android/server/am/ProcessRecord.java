package com.android.server.am;

import android.app.ActivityManager;
import android.app.IApplicationThread;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProcessInfo;
import android.content.pm.VersionedPackage;
import android.content.res.CompatibilityInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.Zygote;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessListener;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.am.ITranProcessRecord;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class ProcessRecord implements WindowProcessListener {
    public static final int PRELOAD_HIGH_LEVEL_STATE = 2;
    public static final int PRELOAD_LOW_LEVEL_STATE = 1;
    public static final int PRELOAD_NORMAL_STATE = 0;
    static final String TAG = "ActivityManager";
    final boolean appZygote;
    public volatile ApplicationInfo info;
    private boolean isMagicTouchWallpaper;
    public final boolean isSdkSandbox;
    final boolean isolated;
    public boolean launchingProvider;
    private volatile boolean mBindMountPending;
    private CompatibilityInfo mCompat;
    private IBinder.DeathRecipient mDeathRecipient;
    private boolean mDebugging;
    private long[] mDisabledCompatChanges;
    private int mDyingPid;
    final ProcessErrorStateRecord mErrorState;
    private int[] mGids;
    private volatile HostingRecord mHostingRecord;
    private boolean mInFullBackup;
    private ActiveInstrumentation mInstr;
    private String mInstructionSet;
    private String mIsolatedEntryPoint;
    private String[] mIsolatedEntryPointArgs;
    private long mKillTime;
    private boolean mKilled;
    private boolean mKilledByAm;
    private long mLastActivityTime;
    private int mLruSeq;
    private volatile int mMountMode;
    final ProcessCachedOptimizerRecord mOptRecord;
    private boolean mPendingStart;
    private volatile boolean mPersistent;
    public int mPid;
    private ArraySet<String> mPkgDeps;
    public final PackageList mPkgList;
    volatile ProcessRecord mPredecessor;
    private final ActivityManagerGlobalLock mProcLock;
    final ProcessProfileRecord mProfile;
    final ProcessProviderRecord mProviders;
    final ProcessReceiverRecord mReceivers;
    private volatile boolean mRemoved;
    private int mRenderThreadTid;
    private String mRequiredAbi;
    private volatile String mSeInfo;
    final ActivityManagerService mService;
    public final ProcessServiceRecord mServices;
    private String mShortStringName;
    private volatile long mStartElapsedTime;
    private long mStartSeq;
    private volatile int mStartUid;
    private volatile long mStartUptime;
    public final ProcessStateRecord mState;
    private String mStringName;
    volatile ProcessRecord mSuccessor;
    Runnable mSuccessorStartRunnable;
    public IApplicationThread mThread;
    private UidRecord mUidRecord;
    private boolean mUnlocked;
    private boolean mUsingWrapper;
    private boolean mWaitedForDebugger;
    private String mWaitingToKill;
    private final WindowProcessController mWindowProcessController;
    final String packageName;
    public int preloadState;
    final ProcessInfo processInfo;
    public final String processName;
    final MyProcessWrapper processWrapper;
    final String sdkSandboxClientAppPackage;
    final String sdkSandboxClientAppVolumeUuid;
    public final int uid;
    public final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStartParams(int startUid, HostingRecord hostingRecord, String seInfo, long startUptime, long startElapsedTime) {
        this.mStartUid = startUid;
        this.mHostingRecord = hostingRecord;
        this.mSeInfo = seInfo;
        this.mStartUptime = startUptime;
        this.mStartElapsedTime = startElapsedTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        long nowUptime = SystemClock.uptimeMillis();
        long nowElapsedTime = SystemClock.elapsedRealtime();
        pw.print(prefix);
        pw.print("user #");
        pw.print(this.userId);
        pw.print(" uid=");
        pw.print(this.info.uid);
        if (this.uid != this.info.uid) {
            pw.print(" ISOLATED uid=");
            pw.print(this.uid);
        }
        pw.print(" gids={");
        if (this.mGids != null) {
            for (int gi = 0; gi < this.mGids.length; gi++) {
                if (gi != 0) {
                    pw.print(", ");
                }
                pw.print(this.mGids[gi]);
            }
        }
        pw.println("}");
        if (this.processInfo != null) {
            pw.print(prefix);
            pw.println("processInfo:");
            if (this.processInfo.deniedPermissions != null) {
                for (int i = 0; i < this.processInfo.deniedPermissions.size(); i++) {
                    pw.print(prefix);
                    pw.print("  deny: ");
                    pw.println((String) this.processInfo.deniedPermissions.valueAt(i));
                }
            }
            if (this.processInfo.gwpAsanMode != -1) {
                pw.print(prefix);
                pw.println("  gwpAsanMode=" + this.processInfo.gwpAsanMode);
            }
            if (this.processInfo.memtagMode != -1) {
                pw.print(prefix);
                pw.println("  memtagMode=" + this.processInfo.memtagMode);
            }
        }
        pw.print(prefix);
        pw.print("mRequiredAbi=");
        pw.print(this.mRequiredAbi);
        pw.print(" instructionSet=");
        pw.println(this.mInstructionSet);
        if (this.info.className != null) {
            pw.print(prefix);
            pw.print("class=");
            pw.println(this.info.className);
        }
        if (this.info.manageSpaceActivityName != null) {
            pw.print(prefix);
            pw.print("manageSpaceActivityName=");
            pw.println(this.info.manageSpaceActivityName);
        }
        pw.print(prefix);
        pw.print("dir=");
        pw.print(this.info.sourceDir);
        pw.print(" publicDir=");
        pw.print(this.info.publicSourceDir);
        pw.print(" data=");
        pw.println(this.info.dataDir);
        this.mPkgList.dump(pw, prefix);
        if (this.mPkgDeps != null) {
            pw.print(prefix);
            pw.print("packageDependencies={");
            for (int i2 = 0; i2 < this.mPkgDeps.size(); i2++) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                pw.print(this.mPkgDeps.valueAt(i2));
            }
            pw.println("}");
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.println(this.mCompat);
        if (this.mInstr != null) {
            pw.print(prefix);
            pw.print("mInstr=");
            pw.println(this.mInstr);
        }
        pw.print(prefix);
        pw.print("thread=");
        pw.println(this.mThread);
        pw.print(prefix);
        pw.print("pid=");
        pw.println(this.mPid);
        pw.print(prefix);
        pw.print("lastActivityTime=");
        TimeUtils.formatDuration(this.mLastActivityTime, nowUptime, pw);
        pw.print(prefix);
        pw.print("startUptimeTime=");
        TimeUtils.formatDuration(this.mStartElapsedTime, nowUptime, pw);
        pw.print(prefix);
        pw.print("startElapsedTime=");
        TimeUtils.formatDuration(this.mStartElapsedTime, nowElapsedTime, pw);
        pw.println();
        if (this.mPersistent || this.mRemoved) {
            pw.print(prefix);
            pw.print("persistent=");
            pw.print(this.mPersistent);
            pw.print(" removed=");
            pw.println(this.mRemoved);
        }
        if (this.mDebugging) {
            pw.print(prefix);
            pw.print("mDebugging=");
            pw.println(this.mDebugging);
        }
        if (this.mPendingStart) {
            pw.print(prefix);
            pw.print("pendingStart=");
            pw.println(this.mPendingStart);
        }
        pw.print(prefix);
        pw.print("startSeq=");
        pw.println(this.mStartSeq);
        pw.print(prefix);
        pw.print("mountMode=");
        pw.println(DebugUtils.valueToString(Zygote.class, "MOUNT_EXTERNAL_", this.mMountMode));
        if (this.mKilled || this.mKilledByAm || this.mWaitingToKill != null) {
            pw.print(prefix);
            pw.print("killed=");
            pw.print(this.mKilled);
            pw.print(" killedByAm=");
            pw.print(this.mKilledByAm);
            pw.print(" waitingToKill=");
            pw.println(this.mWaitingToKill);
        }
        if (this.mIsolatedEntryPoint != null || this.mIsolatedEntryPointArgs != null) {
            pw.print(prefix);
            pw.print("isolatedEntryPoint=");
            pw.println(this.mIsolatedEntryPoint);
            pw.print(prefix);
            pw.print("isolatedEntryPointArgs=");
            pw.println(Arrays.toString(this.mIsolatedEntryPointArgs));
        }
        if (this.mState.getSetProcState() > 10) {
            this.mProfile.dumpCputime(pw, prefix);
        }
        this.mProfile.dumpPss(pw, prefix, nowUptime);
        this.mState.dump(pw, prefix, nowUptime);
        this.mErrorState.dump(pw, prefix, nowUptime);
        this.mServices.dump(pw, prefix, nowUptime);
        this.mProviders.dump(pw, prefix, nowUptime);
        this.mReceivers.dump(pw, prefix, nowUptime);
        this.mOptRecord.dump(pw, prefix, nowUptime);
        this.mWindowProcessController.dump(pw, prefix);
    }

    ProcessRecord(ActivityManagerService _service, ApplicationInfo _info, String _processName, int _uid) {
        this(_service, _info, _processName, _uid, null, -1, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord(ActivityManagerService _service, ApplicationInfo _info, String _processName, int _uid, String _sdkSandboxClientAppPackage, int _definingUid, String _definingProcessName) {
        ProcessInfo procInfo;
        this.isMagicTouchWallpaper = false;
        PackageList packageList = new PackageList(this);
        this.mPkgList = packageList;
        this.preloadState = 0;
        this.launchingProvider = false;
        this.mService = _service;
        this.mProcLock = _service.mProcLock;
        this.info = _info;
        ProcessInfo procInfo2 = null;
        if (_service.mPackageManagerInt == null) {
            procInfo = null;
        } else {
            if (_definingUid > 0) {
                ArrayMap<String, ProcessInfo> processes = _service.mPackageManagerInt.getProcessesForUid(_definingUid);
                if (processes != null) {
                    procInfo2 = processes.get(_definingProcessName);
                }
            } else {
                ArrayMap<String, ProcessInfo> processes2 = _service.mPackageManagerInt.getProcessesForUid(_uid);
                if (processes2 != null) {
                    procInfo2 = processes2.get(_processName);
                }
            }
            if (procInfo2 != null && procInfo2.deniedPermissions == null && procInfo2.gwpAsanMode == -1 && procInfo2.memtagMode == -1 && procInfo2.nativeHeapZeroInitialized == -1) {
                procInfo = null;
            } else {
                procInfo = procInfo2;
            }
        }
        this.processInfo = procInfo;
        this.isolated = Process.isIsolated(_uid);
        boolean isSdkSandboxUid = Process.isSdkSandboxUid(_uid);
        this.isSdkSandbox = isSdkSandboxUid;
        this.appZygote = UserHandle.getAppId(_uid) >= 90000 && UserHandle.getAppId(_uid) <= 98999;
        this.uid = _uid;
        int userId = UserHandle.getUserId(_uid);
        this.userId = userId;
        this.processName = _processName;
        this.sdkSandboxClientAppPackage = _sdkSandboxClientAppPackage;
        if (isSdkSandboxUid) {
            this.sdkSandboxClientAppVolumeUuid = getClientInfoForSdkSandbox().volumeUuid;
        } else {
            this.sdkSandboxClientAppVolumeUuid = null;
        }
        if ("com.transsion.livewallpaper.magictouch".equals(_processName)) {
            this.isMagicTouchWallpaper = true;
        }
        if (_processName != null && "com.android.systemui".equals(_processName)) {
            this.mPersistent = true;
        } else {
            this.mPersistent = false;
        }
        this.mRemoved = false;
        ProcessProfileRecord processProfileRecord = new ProcessProfileRecord(this);
        this.mProfile = processProfileRecord;
        this.mServices = new ProcessServiceRecord(this);
        this.mProviders = new ProcessProviderRecord(this);
        this.mReceivers = new ProcessReceiverRecord(this);
        this.mErrorState = new ProcessErrorStateRecord(this);
        ProcessStateRecord processStateRecord = new ProcessStateRecord(this);
        this.mState = processStateRecord;
        ProcessCachedOptimizerRecord processCachedOptimizerRecord = new ProcessCachedOptimizerRecord(this);
        this.mOptRecord = processCachedOptimizerRecord;
        long now = SystemClock.uptimeMillis();
        processProfileRecord.init(now);
        processCachedOptimizerRecord.init(now);
        processStateRecord.init(now);
        this.mWindowProcessController = new WindowProcessController(_service.mActivityTaskManager, this.info, _processName, _uid, userId, this, this);
        this.processWrapper = initProcessWrapperByGriffin();
        packageList.put(_info.packageName, new ProcessStats.ProcessStateHolder(_info.longVersionCode));
        this.packageName = _info.packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidRecord getUidRecord() {
        return this.mUidRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUidRecord(UidRecord uidRecord) {
        this.mUidRecord = uidRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageList getPkgList() {
        return this.mPkgList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<String> getPkgDeps() {
        return this.mPkgDeps;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPkgDeps(ArraySet<String> pkgDeps) {
        this.mPkgDeps = pkgDeps;
    }

    public int getPid() {
        return this.mPid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPid(int pid) {
        this.mPid = pid;
        this.mWindowProcessController.setPid(pid);
        this.mShortStringName = null;
        this.mStringName = null;
        synchronized (this.mProfile.mProfilerLock) {
            this.mProfile.setPid(pid);
            ITranProcessRecord.Instance().inMuteProcessList(this.mService, this.packageName, pid, this.uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IApplicationThread getThread() {
        return this.mThread;
    }

    public void makeActive(IApplicationThread thread, ProcessStatsService tracker) {
        this.mProfile.onProcessActive(thread, tracker);
        this.mThread = thread;
        this.mWindowProcessController.setThread(thread);
        ITranActivityManagerService.Instance().onMakeActive(this);
    }

    public void makeInactive(ProcessStatsService tracker) {
        ITranActivityManagerService.Instance().onMakeInactive(this);
        this.mThread = null;
        this.mWindowProcessController.setThread(null);
        this.mProfile.onProcessInactive(tracker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDyingPid() {
        return this.mDyingPid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDyingPid(int dyingPid) {
        this.mDyingPid = dyingPid;
    }

    int[] getGids() {
        return this.mGids;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setGids(int[] gids) {
        this.mGids = gids;
    }

    String getRequiredAbi() {
        return this.mRequiredAbi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequiredAbi(String requiredAbi) {
        this.mRequiredAbi = requiredAbi;
        this.mWindowProcessController.setRequiredAbi(requiredAbi);
    }

    String getInstructionSet() {
        return this.mInstructionSet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInstructionSet(String instructionSet) {
        this.mInstructionSet = instructionSet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPersistent(boolean persistent) {
        this.mPersistent = persistent;
        this.mWindowProcessController.setPersistent(persistent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPersistent() {
        return this.mPersistent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPendingStart() {
        return this.mPendingStart;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPendingStart(boolean pendingStart) {
        this.mPendingStart = pendingStart;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getStartSeq() {
        return this.mStartSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStartSeq(long startSeq) {
        this.mStartSeq = startSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HostingRecord getHostingRecord() {
        return this.mHostingRecord;
    }

    void setHostingRecord(HostingRecord hostingRecord) {
        this.mHostingRecord = hostingRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSeInfo() {
        return this.mSeInfo;
    }

    void setSeInfo(String seInfo) {
        this.mSeInfo = seInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getStartUptime() {
        return this.mStartUptime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public long getStartTime() {
        return this.mStartUptime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getStartElapsedTime() {
        return this.mStartElapsedTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getStartUid() {
        return this.mStartUid;
    }

    void setStartUid(int startUid) {
        this.mStartUid = startUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMountMode() {
        return this.mMountMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMountMode(int mountMode) {
        this.mMountMode = mountMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBindMountPending() {
        return this.mBindMountPending;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBindMountPending(boolean bindMountPending) {
        this.mBindMountPending = bindMountPending;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUnlocked() {
        return this.mUnlocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUnlocked(boolean unlocked) {
        this.mUnlocked = unlocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRenderThreadTid() {
        return this.mRenderThreadTid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRenderThreadTid(int renderThreadTid) {
        this.mRenderThreadTid = renderThreadTid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompatibilityInfo getCompat() {
        return this.mCompat;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCompat(CompatibilityInfo compat) {
        this.mCompat = compat;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long[] getDisabledCompatChanges() {
        return this.mDisabledCompatChanges;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisabledCompatChanges(long[] disabledCompatChanges) {
        this.mDisabledCompatChanges = disabledCompatChanges;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unlinkDeathRecipient() {
        IApplicationThread iApplicationThread;
        if (this.mDeathRecipient != null && (iApplicationThread = this.mThread) != null) {
            iApplicationThread.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        }
        this.mDeathRecipient = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeathRecipient(IBinder.DeathRecipient deathRecipient) {
        this.mDeathRecipient = deathRecipient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder.DeathRecipient getDeathRecipient() {
        return this.mDeathRecipient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveInstrumentation(ActiveInstrumentation instr) {
        this.mInstr = instr;
        boolean z = true;
        boolean isInstrumenting = instr != null;
        WindowProcessController windowProcessController = this.mWindowProcessController;
        int i = isInstrumenting ? instr.mSourceUid : -1;
        if (!isInstrumenting || !instr.mHasBackgroundActivityStartsPermission) {
            z = false;
        }
        windowProcessController.setInstrumenting(isInstrumenting, i, z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveInstrumentation getActiveInstrumentation() {
        return this.mInstr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKilledByAm() {
        return this.mKilledByAm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKilledByAm(boolean killedByAm) {
        this.mKilledByAm = killedByAm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKilled() {
        return this.mKilled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKilled(boolean killed) {
        this.mKilled = killed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getKillTime() {
        return this.mKillTime;
    }

    void setKillTime(long killTime) {
        this.mKillTime = killTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getWaitingToKill() {
        return this.mWaitingToKill;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWaitingToKill(String waitingToKill) {
        this.mWaitingToKill = waitingToKill;
    }

    @Override // com.android.server.wm.WindowProcessListener
    public boolean isRemoved() {
        return this.mRemoved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRemoved(boolean removed) {
        this.mRemoved = removed;
    }

    public boolean isDebugging() {
        return this.mDebugging;
    }

    public ApplicationInfo getClientInfoForSdkSandbox() {
        if (!this.isSdkSandbox || this.sdkSandboxClientAppPackage == null) {
            throw new IllegalStateException("getClientInfoForSdkSandbox called for non-sandbox process");
        }
        PackageManagerInternal pm = this.mService.getPackageManagerInternal();
        return pm.getApplicationInfo(this.sdkSandboxClientAppPackage, 0L, 1000, this.userId);
    }

    public boolean isDebuggable() {
        if ((this.info.flags & 2) != 0) {
            return true;
        }
        if (this.isSdkSandbox) {
            ApplicationInfo clientInfo = getClientInfoForSdkSandbox();
            return (clientInfo == null || (clientInfo.flags & 2) == 0) ? false : true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugging(boolean debugging) {
        this.mDebugging = debugging;
        this.mWindowProcessController.setDebugging(debugging);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWaitedForDebugger() {
        return this.mWaitedForDebugger;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWaitedForDebugger(boolean waitedForDebugger) {
        this.mWaitedForDebugger = waitedForDebugger;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastActivityTime() {
        return this.mLastActivityTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastActivityTime(long lastActivityTime) {
        this.mLastActivityTime = lastActivityTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUsingWrapper() {
        return this.mUsingWrapper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUsingWrapper(boolean usingWrapper) {
        this.mUsingWrapper = usingWrapper;
        this.mWindowProcessController.setUsingWrapper(usingWrapper);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLruSeq() {
        return this.mLruSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLruSeq(int lruSeq) {
        this.mLruSeq = lruSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getIsolatedEntryPoint() {
        return this.mIsolatedEntryPoint;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsolatedEntryPoint(String isolatedEntryPoint) {
        this.mIsolatedEntryPoint = isolatedEntryPoint;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getIsolatedEntryPointArgs() {
        return this.mIsolatedEntryPointArgs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsolatedEntryPointArgs(String[] isolatedEntryPointArgs) {
        this.mIsolatedEntryPointArgs = isolatedEntryPointArgs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInFullBackup() {
        return this.mInFullBackup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInFullBackup(boolean inFullBackup) {
        this.mInFullBackup = inFullBackup;
    }

    @Override // com.android.server.wm.WindowProcessListener
    public boolean isCached() {
        return this.mState.isCached();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActivities() {
        return this.mWindowProcessController.hasActivities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActivitiesOrRecentTasks() {
        return this.mWindowProcessController.hasActivitiesOrRecentTasks();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasRecentTasks() {
        return this.mWindowProcessController.hasRecentTasks();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onCleanupApplicationRecordLSP(ProcessStatsService processStats, boolean allowRestart, boolean unlinkDeath) {
        this.mErrorState.onCleanupApplicationRecordLSP();
        resetPackageList(processStats);
        if (unlinkDeath) {
            unlinkDeathRecipient();
        }
        makeInactive(processStats);
        setWaitingToKill(null);
        this.mState.onCleanupApplicationRecordLSP();
        this.mServices.onCleanupApplicationRecordLocked();
        this.mReceivers.onCleanupApplicationRecordLocked();
        return this.mProviders.onCleanupApplicationRecordLocked(allowRestart);
    }

    public boolean isInterestingToUserLocked() {
        if (this.mWindowProcessController.isInterestingToUser()) {
            return true;
        }
        return this.mServices.hasForegroundServices();
    }

    public boolean isForegroundToUserLocked() {
        if (this.mWindowProcessController.isForegroundToUser()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleCrashLocked(String message, int exceptionTypeId, Bundle extras) {
        if (!this.mKilledByAm && this.mThread != null) {
            if (this.mPid == Process.myPid()) {
                Slog.w(TAG, "scheduleCrash: trying to crash system process!");
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mThread.scheduleCrash(message, exceptionTypeId, extras);
                } catch (RemoteException e) {
                    killLocked("scheduleCrash for '" + message + "' failed", 4, true);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killLocked(String reason, int reasonCode, boolean noisy) {
        killLocked(reason, reasonCode, 0, noisy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killLocked(String reason, int reasonCode, int subReason, boolean noisy) {
        killLocked(reason, reason, reasonCode, subReason, noisy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0098  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x00f5  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x012e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void killLocked(String reason, String description, int reasonCode, int subReason, boolean noisy) {
        String str;
        String description2;
        boolean z;
        if (!this.mKilledByAm) {
            Trace.traceBegin(64L, "kill");
            if (reasonCode != 6) {
                str = description;
            } else if (this.mErrorState.getAnrAnnotation() != null) {
                description2 = description + ": " + this.mErrorState.getAnrAnnotation();
                if (this.mService != null && (noisy || this.info.uid == this.mService.mCurOomAdjUid)) {
                    this.mService.reportUidInfoMessageLocked(TAG, "Killing " + toShortString() + " (adj " + this.mState.getSetAdj() + "): " + reason, this.info.uid);
                }
                if (this.mPid <= 0) {
                    z = true;
                    this.mService.mAmsExt.onAppProcessDied(this.mService.mContext, this, this.info, this.userId, null, reason);
                    this.mService.mProcessList.noteAppKill(this, reasonCode, subReason, description2);
                    EventLog.writeEvent((int) EventLogTags.AM_KILL, Integer.valueOf(this.userId), Integer.valueOf(this.mPid), this.processName, Integer.valueOf(this.mState.getSetAdj()), reason);
                    Process.killProcessQuiet(this.mPid);
                    ProcessList.killProcessGroup(this.uid, this.mPid);
                    ITranActivityManagerService.Instance().onProcessRecordKill(this, reasonCode, subReason, reason);
                } else {
                    z = true;
                    if (!this.mPersistent || this.info == null || !"com.android.systemui".equals(this.info.packageName)) {
                        this.mPendingStart = false;
                    } else {
                        Slog.i(TAG, "Skipping set pendingStart = false for " + this.info.packageName);
                    }
                }
                if (!this.mPersistent) {
                    synchronized (this.mProcLock) {
                        try {
                            ActivityManagerService.boostPriorityForProcLockedSection();
                            this.mKilled = z;
                            this.mKilledByAm = z;
                            this.mKillTime = SystemClock.uptimeMillis();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                }
                Trace.traceEnd(64L);
            } else {
                str = description;
            }
            description2 = str;
            if (this.mService != null) {
                this.mService.reportUidInfoMessageLocked(TAG, "Killing " + toShortString() + " (adj " + this.mState.getSetAdj() + "): " + reason, this.info.uid);
            }
            if (this.mPid <= 0) {
            }
            if (!this.mPersistent) {
            }
            Trace.traceEnd(64L);
        }
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        dumpDebug(proto, fieldId, -1);
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId, int lruIndex) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.mPid);
        proto.write(1138166333442L, this.processName);
        proto.write(1120986464259L, this.info.uid);
        if (UserHandle.getAppId(this.info.uid) >= 10000) {
            proto.write(1120986464260L, this.userId);
            proto.write(1120986464261L, UserHandle.getAppId(this.info.uid));
        }
        if (this.uid != this.info.uid) {
            proto.write(1120986464262L, UserHandle.getAppId(this.uid));
        }
        proto.write(1133871366151L, this.mPersistent);
        if (lruIndex >= 0) {
            proto.write(1120986464264L, lruIndex);
        }
        proto.end(token);
    }

    public String toShortString() {
        String shortStringName = this.mShortStringName;
        if (shortStringName != null) {
            return shortStringName;
        }
        StringBuilder sb = new StringBuilder(128);
        toShortString(sb);
        String sb2 = sb.toString();
        this.mShortStringName = sb2;
        return sb2;
    }

    void toShortString(StringBuilder sb) {
        sb.append(this.mPid);
        sb.append(':');
        sb.append(this.processName);
        sb.append('/');
        if (this.info.uid < 10000) {
            sb.append(this.uid);
            return;
        }
        sb.append('u');
        sb.append(this.userId);
        int appId = UserHandle.getAppId(this.info.uid);
        if (appId >= 10000) {
            sb.append('a');
            sb.append(appId - 10000);
        } else {
            sb.append('s');
            sb.append(appId);
        }
        if (this.uid != this.info.uid) {
            sb.append('i');
            sb.append(UserHandle.getAppId(this.uid) - 99000);
        }
    }

    public String toString() {
        String stringName = this.mStringName;
        if (stringName != null) {
            return stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ProcessRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        toShortString(sb);
        sb.append('}');
        String sb2 = sb.toString();
        this.mStringName = sb2;
        return sb2;
    }

    public boolean addPackage(String pkg, long versionCode, ProcessStatsService tracker) {
        synchronized (tracker.mLock) {
            synchronized (this.mPkgList) {
                if (!this.mPkgList.containsKey(pkg)) {
                    ProcessStats.ProcessStateHolder holder = new ProcessStats.ProcessStateHolder(versionCode);
                    ProcessState baseProcessTracker = this.mProfile.getBaseProcessTracker();
                    if (baseProcessTracker != null) {
                        tracker.updateProcessStateHolderLocked(holder, pkg, this.info.uid, versionCode, this.processName);
                        this.mPkgList.put(pkg, holder);
                        if (holder.state != baseProcessTracker) {
                            holder.state.makeActive();
                        }
                    } else {
                        this.mPkgList.put(pkg, holder);
                    }
                    return true;
                }
                return false;
            }
        }
    }

    public void resetPackageList(ProcessStatsService tracker) {
        PackageList packageList;
        synchronized (tracker.mLock) {
            final ProcessState baseProcessTracker = this.mProfile.getBaseProcessTracker();
            PackageList packageList2 = this.mPkgList;
            try {
                synchronized (packageList2) {
                    try {
                        int numOfPkgs = this.mPkgList.size();
                        if (baseProcessTracker != null) {
                            long now = SystemClock.uptimeMillis();
                            baseProcessTracker.setState(-1, tracker.getMemFactorLocked(), now, this.mPkgList.getPackageListLocked());
                            this.mPkgList.forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessRecord$$ExternalSyntheticLambda0
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    ProcessRecord.this.m1491lambda$resetPackageList$0$comandroidserveramProcessRecord((String) obj, (ProcessStats.ProcessStateHolder) obj2);
                                }
                            });
                            if (numOfPkgs == 1) {
                                packageList = packageList2;
                            } else {
                                this.mPkgList.forEachPackageProcessStats(new Consumer() { // from class: com.android.server.am.ProcessRecord$$ExternalSyntheticLambda1
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ProcessRecord.lambda$resetPackageList$1(baseProcessTracker, (ProcessStats.ProcessStateHolder) obj);
                                    }
                                });
                                this.mPkgList.clear();
                                ProcessStats.ProcessStateHolder holder = new ProcessStats.ProcessStateHolder(this.info.longVersionCode);
                                packageList = packageList2;
                                tracker.updateProcessStateHolderLocked(holder, this.info.packageName, this.info.uid, this.info.longVersionCode, this.processName);
                                this.mPkgList.put(this.info.packageName, holder);
                                if (holder.state != baseProcessTracker) {
                                    holder.state.makeActive();
                                }
                            }
                        } else {
                            packageList = packageList2;
                            if (numOfPkgs != 1) {
                                this.mPkgList.clear();
                                this.mPkgList.put(this.info.packageName, new ProcessStats.ProcessStateHolder(this.info.longVersionCode));
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetPackageList$0$com-android-server-am-ProcessRecord  reason: not valid java name */
    public /* synthetic */ void m1491lambda$resetPackageList$0$comandroidserveramProcessRecord(String pkgName, ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(3, this.uid, this.processName, pkgName, ActivityManager.processStateAmToProto(-1), holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$resetPackageList$1(ProcessState baseProcessTracker, ProcessStats.ProcessStateHolder holder) {
        if (holder.state != null && holder.state != baseProcessTracker) {
            holder.state.makeInactive();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getPackageList() {
        return this.mPkgList.getPackageList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<VersionedPackage> getPackageListWithVersionCode() {
        return this.mPkgList.getPackageListWithVersionCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowProcessController getWindowProcessController() {
        return this.mWindowProcessController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addOrUpdateAllowBackgroundActivityStartsToken(Binder entity, IBinder originatingToken) {
        Objects.requireNonNull(entity);
        this.mWindowProcessController.addOrUpdateAllowBackgroundActivityStartsToken(entity, originatingToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllowBackgroundActivityStartsToken(Binder entity) {
        Objects.requireNonNull(entity);
        this.mWindowProcessController.removeAllowBackgroundActivityStartsToken(entity);
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void clearProfilerIfNeeded() {
        synchronized (this.mService.mAppProfiler.mProfilerLock) {
            this.mService.mAppProfiler.clearProfilerLPf();
        }
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void updateServiceConnectionActivities() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mServices.updateServiceConnectionActivitiesLocked(this.mServices);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void setPendingUiClean(boolean pendingUiClean) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                this.mProfile.setPendingUiClean(pendingUiClean);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void setPendingUiCleanAndForceProcessStateUpTo(int newState) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                setPendingUiClean(true);
                this.mState.forceProcessStateUpTo(newState);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void updateProcessInfo(boolean updateServiceConnectionActivities, boolean activityChange, boolean updateOomAdj) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (updateServiceConnectionActivities) {
                    this.mService.mServices.updateServiceConnectionActivitiesLocked(this.mServices);
                }
                if (this.mThread == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                this.mService.updateLruProcessLocked(this, activityChange, null);
                if (updateOomAdj) {
                    this.mService.updateOomAdjLocked(this, "updateOomAdj_activityChange");
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override // com.android.server.wm.WindowProcessListener
    public long getCpuTime() {
        return this.mService.mAppProfiler.getCpuTimeForPid(this.mPid);
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void onStartActivity(int topProcessState, boolean setProfileProc, String packageName, long versionCode) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mWaitingToKill = null;
                if (setProfileProc) {
                    synchronized (this.mService.mAppProfiler.mProfilerLock) {
                        this.mService.mAppProfiler.setProfileProcLPf(this);
                    }
                }
                if (packageName != null) {
                    addPackage(packageName, versionCode, this.mService.mProcessStats);
                }
                updateProcessInfo(false, true, true);
                setPendingUiClean(true);
                this.mState.setHasShownUi(true);
                this.mState.forceProcessStateUpTo(topProcessState);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void appDied(String reason) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.appDiedLocked(this, reason);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.wm.WindowProcessListener
    public void setRunningRemoteAnimation(boolean runningRemoteAnimation) {
        if (this.mPid == Process.myPid()) {
            Slog.wtf(TAG, "system can't run remote animation");
            return;
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mState.setRunningRemoteAnimation(runningRemoteAnimation);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public long getInputDispatchingTimeoutMillis() {
        return this.mWindowProcessController.getInputDispatchingTimeoutMillis();
    }

    public int getProcessClassEnum() {
        if (this.mPid == ActivityManagerService.MY_PID) {
            return 3;
        }
        if (this.info == null) {
            return 0;
        }
        return (this.info.flags & 1) != 0 ? 2 : 1;
    }

    List<ProcessRecord> getLruProcessList() {
        return this.mService.mProcessList.getLruProcessesLOSP();
    }

    private MyProcessWrapper createWrapper() {
        TranProcessInfo griffinProcessInfo = ITranProcessRecord.Instance().initMyProcessWrapper(this.processName, this.uid, this.mPkgList, this.info);
        return new MyProcessWrapper(griffinProcessInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MyProcessWrapper extends TranProcessWrapper {
        public MyProcessWrapper(TranProcessInfo griffinProcessInfo) {
            super(griffinProcessInfo);
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public String processName() {
            return ProcessRecord.this.processName;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int uid() {
            return ProcessRecord.this.uid;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int pid() {
            return ProcessRecord.this.mPid;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public ArrayList<String> pkgList() {
            ArrayList<String> result = new ArrayList<>();
            try {
                result.addAll(ProcessRecord.this.mPkgList.getPackageListLocked().keySet());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public ArraySet<String> pkgDeps() {
            return ProcessRecord.this.mPkgDeps;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean starting() {
            return false;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int maxAdj() {
            return ProcessRecord.this.mState.getMaxAdj();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int setAdj() {
            return ProcessRecord.this.mState.getSetRawAdj();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int setSchedGroup() {
            return ProcessRecord.this.mState.getSetSchedGroup();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int setProcState() {
            return ProcessRecord.this.mState.getSetProcState();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean persistent() {
            return ProcessRecord.this.mPersistent;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean serviceb() {
            return ProcessRecord.this.mState.isServiceB();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean serviceHighRam() {
            return ProcessRecord.this.mState.isServiceHighRam();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean notCachedSinceIdle() {
            return ProcessRecord.this.mState.isNotCachedSinceIdle();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasClientActivities() {
            return ProcessRecord.this.mServices.hasClientActivities();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasStartedServices() {
            return ProcessRecord.this.mState.hasStartedServices();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean foregroundServices() {
            return ProcessRecord.this.mServices.hasForegroundServices();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean foregroundActivities() {
            return ProcessRecord.this.mState.hasForegroundActivities();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean systemNoUi() {
            return ProcessRecord.this.mState.isSystemNoUi();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasShownUi() {
            return ProcessRecord.this.mState.hasShownUi();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasTopUi() {
            return ProcessRecord.this.mState.hasTopUi();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasOverlayUi() {
            return ProcessRecord.this.mState.hasOverlayUi();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasAboveClient() {
            return ProcessRecord.this.mServices.hasAboveClient();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean treatLikeActivity() {
            return ProcessRecord.this.mServices.isTreatedLikeActivity();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean bad() {
            return false;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean killed() {
            return ProcessRecord.this.mKilled;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean killedByAm() {
            return ProcessRecord.this.mKilledByAm;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean unlocked() {
            return ProcessRecord.this.mUnlocked;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long interactionEventTime() {
            return ProcessRecord.this.mState.getInteractionEventTime();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long fgInteractionTime() {
            return ProcessRecord.this.mState.getFgInteractionTime();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastCpuTime() {
            return ProcessRecord.this.mProfile.mLastCpuTime.get();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long curCpuTime() {
            return ProcessRecord.this.mProfile.mCurCpuTime.get();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastRequestedGc() {
            return ProcessRecord.this.mProfile.getLastRequestedGc();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastLowMemory() {
            return ProcessRecord.this.mProfile.getLastLowMemory();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastProviderTime() {
            return ProcessRecord.this.mProviders.getLastProviderTime();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean reportLowMemory() {
            return ProcessRecord.this.mProfile.getReportLowMemory();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean empty() {
            return ProcessRecord.this.mState.isEmpty();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean cached() {
            return ProcessRecord.this.mState.isCached();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean execServicesFg() {
            return ProcessRecord.this.mServices.shouldExecServicesFg();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public String hostingType() {
            return ProcessRecord.this.mHostingRecord == null ? "" : ProcessRecord.this.mHostingRecord.getType();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public String hostingNameStr() {
            return ProcessRecord.this.mHostingRecord == null ? "" : ProcessRecord.this.mHostingRecord.getName();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean isolated() {
            return ProcessRecord.this.isolated;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public ApplicationInfo info() {
            return ProcessRecord.this.info;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasActivities() {
            return ProcessRecord.this.mWindowProcessController.hasActivities();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasServices() {
            return ProcessRecord.this.mServices.numberOfRunningServices() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasExecutingServices() {
            return ProcessRecord.this.mServices.numberOfExecutingServices() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasConnections() {
            return ProcessRecord.this.mServices.numberOfConnections() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasReceivers() {
            return ProcessRecord.this.mReceivers.numberOfCurReceivers() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasPubProviders() {
            return ProcessRecord.this.mProviders.numberOfProviders() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean hasConProviders() {
            return ProcessRecord.this.mProviders.numberOfProviderConnections() > 0;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Activity> activities() {
            List<TranAppInfo.Activity> aaList = new ArrayList<>();
            ArrayList<ActivityRecord> activities = ProcessRecord.this.mWindowProcessController.getActivities();
            for (int i = 0; i < activities.size(); i++) {
                ActivityRecord ar = activities.get(i);
                if (ar != null) {
                    TranAppInfo.Activity aa = new TranAppInfo.Activity(ar.info, ar.grifAppInfo);
                    aaList.add(aa);
                }
            }
            return aaList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Service> services() {
            List<TranAppInfo.Service> asList = new ArrayList<>();
            for (int i = 0; i < ProcessRecord.this.mServices.numberOfRunningServices(); i++) {
                try {
                    ServiceRecord sr = ProcessRecord.this.mServices.getRunningServiceAt(i);
                    TranAppInfo.Service as = new TranAppInfo.Service(sr.serviceInfo, sr.grifAppInfo);
                    asList.add(as);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return asList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Service> executingServices() {
            List<TranAppInfo.Service> asList = new ArrayList<>();
            for (int i = 0; i < ProcessRecord.this.mServices.numberOfExecutingServices(); i++) {
                try {
                    ServiceRecord sr = ProcessRecord.this.mServices.getExecutingServiceAt(i);
                    TranAppInfo.Service as = new TranAppInfo.Service(sr.serviceInfo, sr.grifAppInfo);
                    asList.add(as);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return asList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo> connections() {
            List<TranAppInfo> aiList = new ArrayList<>();
            for (int i = 0; i < ProcessRecord.this.mServices.numberOfConnections(); i++) {
                try {
                    aiList.add(ProcessRecord.this.mServices.getConnectionAt(i).binding.service.grifAppInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return aiList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Receiver> receivers() {
            List<TranAppInfo.Receiver> arList = new ArrayList<>();
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (this.processInfo == null) {
                return arList;
            }
            for (int i = 0; i < ProcessRecord.this.mReceivers.numberOfCurReceivers(); i++) {
                BroadcastRecord br = ProcessRecord.this.mReceivers.getCurReceiverAt(i);
                TranAppInfo appInfo = ITranGriffinFeature.Instance().getAppInfo(br.curComponent.getPackageName());
                TranAppInfo.Receiver ar = new TranAppInfo.Receiver(br.curReceiver, appInfo);
                arList.add(ar);
            }
            return arList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Provider> pubProviders() {
            List<TranAppInfo.Provider> apList = new ArrayList<>();
            for (int i = 0; i < ProcessRecord.this.mProviders.numberOfProviders(); i++) {
                try {
                    ContentProviderRecord cpr = ProcessRecord.this.mProviders.getProviderAt(i);
                    TranAppInfo grifAppInfo = cpr.appInfo == null ? null : ITranGriffinFeature.Instance().getAppInfo(cpr.appInfo.packageName);
                    TranAppInfo.Provider ap = new TranAppInfo.Provider(cpr.info, grifAppInfo);
                    apList.add(ap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return apList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranAppInfo.Provider> conProviders() {
            List<TranAppInfo.Provider> apList = new ArrayList<>();
            for (int i = 0; i < ProcessRecord.this.mProviders.numberOfProviderConnections(); i++) {
                try {
                    ContentProviderRecord cpr = ProcessRecord.this.mProviders.getProviderConnectionAt(i).provider;
                    if (cpr != null && cpr.proc != null) {
                        int providerPid = cpr.proc.mPid;
                        String providerProcName = cpr.proc.processName;
                        String providerPackageName = cpr.proc.info.packageName;
                        TranAppInfo grifAppInfo = cpr.appInfo == null ? null : ITranGriffinFeature.Instance().getAppInfo(cpr.appInfo.packageName);
                        TranAppInfo.Provider ap = new TranAppInfo.Provider(cpr.info, grifAppInfo);
                        if (providerPid != ProcessRecord.this.mPid && !providerProcName.equals(ProcessRecord.this.processName) && !providerPackageName.equals(ProcessRecord.this.info.packageName) && !apList.contains(ap)) {
                            apList.add(ap);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return apList;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastPssTime() {
            return ProcessRecord.this.mProfile.getLastPssTime();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastPss() {
            return ProcessRecord.this.mProfile.getLastPss();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastSwapPss() {
            return ProcessRecord.this.mProfile.getLastSwapPss();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastCachedPss() {
            return ProcessRecord.this.mProfile.getLastCachedPss();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public long lastCachedSwapPss() {
            return ProcessRecord.this.mProfile.getLastCachedSwapPss();
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public boolean launchingProvider() {
            return ProcessRecord.this.launchingProvider;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public int preloadState() {
            return ProcessRecord.this.preloadState;
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranProcessWrapper> getServiceClients() {
            ProcessRecord client;
            Set<TranProcessWrapper> clients = new HashSet<>();
            int serviceRecrdCount = ProcessRecord.this.mServices.numberOfRunningServices();
            if (serviceRecrdCount > 0) {
                for (int si = 0; si < serviceRecrdCount; si++) {
                    ServiceRecord s = ProcessRecord.this.mServices.getRunningServiceAt(si);
                    if (s.getConnections().size() > 0) {
                        int N = s.getConnections().size();
                        for (int conni = 0; conni < N; conni++) {
                            ArrayList<ConnectionRecord> cr = s.getConnections().valueAt(conni);
                            for (int i = 0; i < cr.size(); i++) {
                                ConnectionRecord c = cr.get(i);
                                if (c != null && c.binding != null && c.binding.client != null && (client = c.binding.client) != null && client.mPid != ProcessRecord.this.mPid) {
                                    clients.add(client.processWrapper);
                                }
                            }
                        }
                    }
                }
            }
            return new ArrayList(clients);
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranProcessWrapper> getProviderClients() {
            Set<TranProcessWrapper> clients = new HashSet<>();
            int providerRecordCount = ProcessRecord.this.mProviders.numberOfProviders();
            if (providerRecordCount > 0) {
                for (int i = 0; i < providerRecordCount; i++) {
                    ContentProviderRecord cpr = ProcessRecord.this.mProviders.getProviderAt(i);
                    if (cpr != null && cpr.connections != null && cpr.connections.size() > 0) {
                        for (int conni = 0; conni < cpr.connections.size(); conni++) {
                            ContentProviderConnection conn = cpr.connections.get(conni);
                            if (conn != null && conn.client != null && conn.client.mPid != ProcessRecord.this.mPid) {
                                clients.add(conn.client.processWrapper);
                            }
                        }
                    }
                }
            }
            return new ArrayList(clients);
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranProcessWrapper> getConnectServices() {
            Set<TranProcessWrapper> services = new HashSet<>();
            int connectionRecordCount = ProcessRecord.this.mServices.numberOfConnections();
            if (connectionRecordCount > 0) {
                for (int index = 0; index < connectionRecordCount; index++) {
                    ConnectionRecord cr = ProcessRecord.this.mServices.getConnectionAt(index);
                    if (cr != null && cr.binding != null && cr.binding.client != null && ProcessRecord.this.mPid == cr.binding.client.mPid) {
                        ServiceRecord record = cr.binding.service;
                        TranProcessWrapper service = ITranGriffinFeature.Instance().getProcessWrapper(record.processName, record.appInfo.uid);
                        if (service != null) {
                            services.add(service);
                        }
                    }
                }
            }
            return new ArrayList(services);
        }

        @Override // com.transsion.hubcore.griffin.lib.app.TranProcessWrapper
        public List<TranProcessWrapper> getConnectProviders() {
            ContentProviderRecord cpr;
            Set<TranProcessWrapper> providers = new HashSet<>();
            int cpCount = ProcessRecord.this.mProviders.numberOfProviderConnections();
            if (cpCount > 0) {
                for (int index = 0; index < cpCount; index++) {
                    ContentProviderConnection cpc = ProcessRecord.this.mProviders.getProviderConnectionAt(index);
                    if (cpc.client != null && ProcessRecord.this.mPid == cpc.client.mPid && (cpr = cpc.provider) != null && cpr.proc != null) {
                        providers.add(cpr.proc.processWrapper);
                    }
                }
            }
            return new ArrayList(providers);
        }
    }

    public boolean hasMediaAction() {
        try {
            if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                Slog.i(TAG, "mServices.size: " + this.mServices.numberOfRunningServices());
            }
            if (this.mServices.numberOfRunningServices() > 0) {
                for (int i = 0; i < this.mServices.numberOfRunningServices(); i++) {
                    ServiceRecord sr = this.mServices.getRunningServiceAt(i);
                    Intent mediaIntent = sr.intent != null ? sr.intent.getIntent() : null;
                    if (mediaIntent != null && mediaIntent.getAction() != null && mediaIntent.getAction().equals("android.media.browse.MediaBrowserService")) {
                        if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                            Slog.i(TAG, "mediaIntent.getAction(): " + mediaIntent.getAction());
                        }
                        return true;
                    }
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "hasMediaAction Error", e);
            return false;
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

    private MyProcessWrapper initProcessWrapperByGriffin() {
        if (!ITranGriffinFeature.Instance().isGriffinSupport()) {
            return null;
        }
        MyProcessWrapper processWrapper = createWrapper();
        this.mWindowProcessController.setProcessWrapper(processWrapper);
        return processWrapper;
    }

    public boolean isMagicTouchWallpaper() {
        return this.isMagicTouchWallpaper;
    }
}
