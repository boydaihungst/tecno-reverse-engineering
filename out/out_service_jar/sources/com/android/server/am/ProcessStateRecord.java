package com.android.server.am;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.OomAdjuster;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public final class ProcessStateRecord {
    private static final int VALUE_FALSE = 0;
    private static final int VALUE_INVALID = -1;
    private static final int VALUE_TRUE = 1;
    private int mAdjSeq;
    private Object mAdjSource;
    private int mAdjSourceProcState;
    private Object mAdjTarget;
    private String mAdjType;
    private int mAdjTypeCode;
    private final ProcessRecord mApp;
    private long mCacheOomRankerRss;
    private long mCacheOomRankerRssTimeMs;
    private int mCacheOomRankerUseCount;
    private boolean mCached;
    private int mCompletedAdjSeq;
    private boolean mContainsCycle;
    private boolean mEmpty;
    private long mFgInteractionTime;
    private Object mForcingToImportant;
    private boolean mHasForegroundActivities;
    private boolean mHasOverlayUi;
    private boolean mHasShownUi;
    private boolean mHasStartedServices;
    private boolean mHasTopUi;
    private long mInteractionEventTime;
    private long mLastCanKillOnBgRestrictedAndIdleTime;
    private long mLastInvisibleTime;
    private long mLastStateTime;
    private long mLastTopTime;
    private boolean mNoKillOnBgRestrictedAndIdle;
    private boolean mNotCachedSinceIdle;
    private final ActivityManagerGlobalLock mProcLock;
    private boolean mProcStateChanged;
    private boolean mReachable;
    private boolean mRepForegroundActivities;
    private boolean mReportedInteraction;
    private boolean mRunningRemoteAnimation;
    private int mSavedPriority;
    private final ActivityManagerService mService;
    private boolean mServiceB;
    private boolean mServiceHighRam;
    private boolean mSetCached;
    private boolean mSetNoKillOnBgRestrictedAndIdle;
    private boolean mSystemNoUi;
    private long mWhenUnimportant;
    private int mMaxAdj = 1001;
    private int mCurRawAdj = -10000;
    private int mSetRawAdj = -10000;
    public int mCurAdj = -10000;
    private int mSetAdj = -10000;
    private int mVerifiedAdj = -10000;
    private int mCurCapability = 0;
    private int mSetCapability = 0;
    private int mCurSchedGroup = 0;
    private int mSetSchedGroup = 0;
    private int mCurProcState = 20;
    private int mRepProcState = 20;
    private int mCurRawProcState = 20;
    private int mSetProcState = 20;
    private boolean mBackgroundRestricted = false;
    private boolean mCurBoundByNonBgRestrictedApp = false;
    private boolean mSetBoundByNonBgRestrictedApp = false;
    private int mCachedHasActivities = -1;
    private int mCachedIsHeavyWeight = -1;
    private int mCachedHasVisibleActivities = -1;
    private int mCachedIsHomeProcess = -1;
    private int mCachedIsPreviousProcess = -1;
    private int mCachedHasRecentTasks = -1;
    private int mCachedIsReceivingBroadcast = -1;
    private int[] mCachedCompatChanges = {-1, -1, -1};
    private int mCachedAdj = -10000;
    private boolean mCachedForegroundActivities = false;
    private int mCachedProcState = 19;
    private int mCachedSchedGroup = 0;
    private boolean mAgaresImproveMaxAdj = false;
    private boolean mAgaresComputeOomAdj = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessStateRecord(ProcessRecord app) {
        this.mApp = app;
        ActivityManagerService activityManagerService = app.mService;
        this.mService = activityManagerService;
        this.mProcLock = activityManagerService.mProcLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(long now) {
        this.mLastStateTime = now;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMaxAdj(int maxAdj) {
        this.mMaxAdj = maxAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxAdj() {
        return this.mMaxAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAgaresImprovedMaxAdj() {
        return this.mAgaresImproveMaxAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAgaresImproMaxAdj(boolean improved) {
        this.mAgaresImproveMaxAdj = improved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAgaresComputeOomAdj() {
        return this.mAgaresComputeOomAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAgaresComputeOomAdj(boolean computed) {
        this.mAgaresComputeOomAdj = computed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurRawAdj(int curRawAdj) {
        this.mCurRawAdj = curRawAdj;
        this.mApp.getWindowProcessController().setPerceptible(curRawAdj <= 200);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurRawAdj() {
        return this.mCurRawAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetRawAdj(int setRawAdj) {
        this.mSetRawAdj = setRawAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetRawAdj() {
        return this.mSetRawAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurAdj(int curAdj) {
        this.mCurAdj = curAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurAdj() {
        return this.mCurAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetAdj(int setAdj) {
        this.mSetAdj = setAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetAdj() {
        return this.mSetAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetAdjWithServices() {
        int i = this.mSetAdj;
        if (i >= 900 && this.mHasStartedServices) {
            return 800;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setVerifiedAdj(int verifiedAdj) {
        this.mVerifiedAdj = verifiedAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getVerifiedAdj() {
        return this.mVerifiedAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurCapability(int curCapability) {
        this.mCurCapability = curCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurCapability() {
        return this.mCurCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetCapability(int setCapability) {
        this.mSetCapability = setCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetCapability() {
        return this.mSetCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurrentSchedulingGroup(int curSchedGroup) {
        this.mCurSchedGroup = curSchedGroup;
        this.mApp.getWindowProcessController().setCurrentSchedulingGroup(curSchedGroup);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentSchedulingGroup() {
        return this.mCurSchedGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetSchedGroup(int setSchedGroup) {
        this.mSetSchedGroup = setSchedGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetSchedGroup() {
        return this.mSetSchedGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurProcState(int curProcState) {
        this.mCurProcState = curProcState;
        this.mApp.getWindowProcessController().setCurrentProcState(this.mCurProcState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurProcState() {
        return this.mCurProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurRawProcState(int curRawProcState) {
        this.mCurRawProcState = curRawProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurRawProcState() {
        return this.mCurRawProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReportedProcState(int repProcState) {
        this.mRepProcState = repProcState;
        this.mApp.getPkgList().forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessStateRecord$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ProcessStateRecord.this.m1493x9bb18389((String) obj, (ProcessStats.ProcessStateHolder) obj2);
            }
        });
        this.mApp.getWindowProcessController().setReportedProcState(repProcState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setReportedProcState$0$com-android-server-am-ProcessStateRecord  reason: not valid java name */
    public /* synthetic */ void m1493x9bb18389(String pkgName, ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(3, this.mApp.uid, this.mApp.processName, pkgName, ActivityManager.processStateAmToProto(this.mRepProcState), holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getReportedProcState() {
        return this.mRepProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceProcessStateUpTo(int newState) {
        if (this.mRepProcState > newState) {
            synchronized (this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    this.mRepProcState = newState;
                    setCurProcState(newState);
                    setCurRawProcState(newState);
                    this.mApp.getPkgList().forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessStateRecord$$ExternalSyntheticLambda0
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ProcessStateRecord.this.m1492xaf90c7a5((String) obj, (ProcessStats.ProcessStateHolder) obj2);
                        }
                    });
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterProcLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$forceProcessStateUpTo$1$com-android-server-am-ProcessStateRecord  reason: not valid java name */
    public /* synthetic */ void m1492xaf90c7a5(String pkgName, ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(3, this.mApp.uid, this.mApp.processName, pkgName, ActivityManager.processStateAmToProto(this.mRepProcState), holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetProcState(int setProcState) {
        if (ActivityManager.isProcStateCached(this.mSetProcState) && !ActivityManager.isProcStateCached(setProcState)) {
            this.mCacheOomRankerUseCount++;
        }
        this.mSetProcState = setProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetProcState() {
        return this.mSetProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastStateTime(long lastStateTime) {
        this.mLastStateTime = lastStateTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastStateTime() {
        return this.mLastStateTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSavedPriority(int savedPriority) {
        this.mSavedPriority = savedPriority;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSavedPriority() {
        return this.mSavedPriority;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setServiceB(boolean serviceb) {
        this.mServiceB = serviceb;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isServiceB() {
        return this.mServiceB;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setServiceHighRam(boolean serviceHighRam) {
        this.mServiceHighRam = serviceHighRam;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isServiceHighRam() {
        return this.mServiceHighRam;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNotCachedSinceIdle(boolean notCachedSinceIdle) {
        this.mNotCachedSinceIdle = notCachedSinceIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNotCachedSinceIdle() {
        return this.mNotCachedSinceIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasStartedServices(boolean hasStartedServices) {
        this.mHasStartedServices = hasStartedServices;
        if (hasStartedServices) {
            this.mApp.mProfile.addHostingComponentType(128);
        } else {
            this.mApp.mProfile.clearHostingComponentType(128);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasStartedServices() {
        return this.mHasStartedServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasForegroundActivities(boolean hasForegroundActivities) {
        this.mHasForegroundActivities = hasForegroundActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundActivities() {
        return this.mHasForegroundActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRepForegroundActivities(boolean repForegroundActivities) {
        this.mRepForegroundActivities = repForegroundActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasRepForegroundActivities() {
        return this.mRepForegroundActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasShownUi(boolean hasShownUi) {
        this.mHasShownUi = hasShownUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasShownUi() {
        return this.mHasShownUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasTopUi(boolean hasTopUi) {
        this.mHasTopUi = hasTopUi;
        this.mApp.getWindowProcessController().setHasTopUi(hasTopUi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasTopUi() {
        return this.mHasTopUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasOverlayUi(boolean hasOverlayUi) {
        this.mHasOverlayUi = hasOverlayUi;
        this.mApp.getWindowProcessController().setHasOverlayUi(hasOverlayUi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasOverlayUi() {
        return this.mHasOverlayUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRunningRemoteAnimation() {
        return this.mRunningRemoteAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRunningRemoteAnimation(boolean runningRemoteAnimation) {
        if (this.mRunningRemoteAnimation == runningRemoteAnimation) {
            return;
        }
        this.mRunningRemoteAnimation = runningRemoteAnimation;
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.i("ActivityManager", "Setting runningRemoteAnimation=" + runningRemoteAnimation + " for pid=" + this.mApp.getPid());
        }
        this.mService.updateOomAdjLocked(this.mApp, "updateOomAdj_uiVisibility");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProcStateChanged(boolean procStateChanged) {
        this.mProcStateChanged = procStateChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasProcStateChanged() {
        return this.mProcStateChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReportedInteraction(boolean reportedInteraction) {
        this.mReportedInteraction = reportedInteraction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasReportedInteraction() {
        return this.mReportedInteraction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInteractionEventTime(long interactionEventTime) {
        this.mInteractionEventTime = interactionEventTime;
        this.mApp.getWindowProcessController().setInteractionEventTime(interactionEventTime);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getInteractionEventTime() {
        return this.mInteractionEventTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFgInteractionTime(long fgInteractionTime) {
        this.mFgInteractionTime = fgInteractionTime;
        this.mApp.getWindowProcessController().setFgInteractionTime(fgInteractionTime);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getFgInteractionTime() {
        return this.mFgInteractionTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcingToImportant(Object forcingToImportant) {
        this.mForcingToImportant = forcingToImportant;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getForcingToImportant() {
        return this.mForcingToImportant;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjSeq(int adjSeq) {
        this.mAdjSeq = adjSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void decAdjSeq() {
        this.mAdjSeq--;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAdjSeq() {
        return this.mAdjSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCompletedAdjSeq(int completedAdjSeq) {
        this.mCompletedAdjSeq = completedAdjSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void decCompletedAdjSeq() {
        this.mCompletedAdjSeq--;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCompletedAdjSeq() {
        return this.mCompletedAdjSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setContainsCycle(boolean containsCycle) {
        this.mContainsCycle = containsCycle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsCycle() {
        return this.mContainsCycle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWhenUnimportant(long whenUnimportant) {
        this.mWhenUnimportant = whenUnimportant;
        this.mApp.getWindowProcessController().setWhenUnimportant(whenUnimportant);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getWhenUnimportant() {
        return this.mWhenUnimportant;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastTopTime(long lastTopTime) {
        this.mLastTopTime = lastTopTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastTopTime() {
        return this.mLastTopTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEmpty(boolean empty) {
        this.mEmpty = empty;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmpty() {
        return this.mEmpty;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCached(boolean cached) {
        this.mCached = cached;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCached() {
        return this.mCached;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCacheOomRankerUseCount() {
        return this.mCacheOomRankerUseCount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSystemNoUi(boolean systemNoUi) {
        this.mSystemNoUi = systemNoUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemNoUi() {
        return this.mSystemNoUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjType(String adjType) {
        this.mAdjType = adjType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getAdjType() {
        return this.mAdjType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjTypeCode(int adjTypeCode) {
        this.mAdjTypeCode = adjTypeCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAdjTypeCode() {
        return this.mAdjTypeCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjSource(Object adjSource) {
        this.mAdjSource = adjSource;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getAdjSource() {
        return this.mAdjSource;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjSourceProcState(int adjSourceProcState) {
        this.mAdjSourceProcState = adjSourceProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAdjSourceProcState() {
        return this.mAdjSourceProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjTarget(Object adjTarget) {
        this.mAdjTarget = adjTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getAdjTarget() {
        return this.mAdjTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReachable() {
        return this.mReachable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReachable(boolean reachable) {
        this.mReachable = reachable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetCachedInfo() {
        this.mCachedHasActivities = -1;
        this.mCachedIsHeavyWeight = -1;
        this.mCachedHasVisibleActivities = -1;
        this.mCachedIsHomeProcess = -1;
        this.mCachedIsPreviousProcess = -1;
        this.mCachedHasRecentTasks = -1;
        this.mCachedIsReceivingBroadcast = -1;
        this.mCachedAdj = -10000;
        this.mCachedForegroundActivities = false;
        this.mCachedProcState = 19;
        this.mCachedSchedGroup = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedHasActivities() {
        if (this.mCachedHasActivities == -1) {
            int i = this.mApp.getWindowProcessController().hasActivities() ? 1 : 0;
            this.mCachedHasActivities = i;
            if (i == 1) {
                this.mApp.mProfile.addHostingComponentType(16);
            } else {
                this.mApp.mProfile.clearHostingComponentType(16);
            }
        }
        return this.mCachedHasActivities == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedIsHeavyWeight() {
        if (this.mCachedIsHeavyWeight == -1) {
            this.mCachedIsHeavyWeight = this.mApp.getWindowProcessController().isHeavyWeightProcess() ? 1 : 0;
        }
        return this.mCachedIsHeavyWeight == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedHasVisibleActivities() {
        if (this.mCachedHasVisibleActivities == -1) {
            this.mCachedHasVisibleActivities = this.mApp.getWindowProcessController().hasVisibleActivities() ? 1 : 0;
        }
        return this.mCachedHasVisibleActivities == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedIsHomeProcess() {
        if (this.mCachedIsHomeProcess == -1) {
            if (this.mApp.getWindowProcessController().isHomeProcess()) {
                this.mCachedIsHomeProcess = 1;
                this.mService.mAppProfiler.mHasHomeProcess = true;
            } else {
                this.mCachedIsHomeProcess = 0;
            }
        }
        return this.mCachedIsHomeProcess == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedIsPreviousProcess() {
        if (this.mCachedIsPreviousProcess == -1) {
            if (this.mApp.getWindowProcessController().isPreviousProcess()) {
                this.mCachedIsPreviousProcess = 1;
                this.mService.mAppProfiler.mHasPreviousProcess = true;
            } else {
                this.mCachedIsPreviousProcess = 0;
            }
        }
        return this.mCachedIsPreviousProcess == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedHasRecentTasks() {
        if (this.mCachedHasRecentTasks == -1) {
            this.mCachedHasRecentTasks = this.mApp.getWindowProcessController().hasRecentTasks() ? 1 : 0;
        }
        return this.mCachedHasRecentTasks == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedIsReceivingBroadcast(ArraySet<BroadcastQueue> tmpQueue) {
        if (this.mCachedIsReceivingBroadcast == -1) {
            tmpQueue.clear();
            int i = this.mService.isReceivingBroadcastLocked(this.mApp, tmpQueue) ? 1 : 0;
            this.mCachedIsReceivingBroadcast = i;
            if (i != 1) {
                this.mApp.mProfile.clearHostingComponentType(32);
            } else {
                this.mCachedSchedGroup = tmpQueue.contains(this.mService.mFgBroadcastQueue) ? 2 : 0;
                this.mApp.mProfile.addHostingComponentType(32);
            }
        }
        return this.mCachedIsReceivingBroadcast == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedCompatChange(int cachedCompatChangeId) {
        int[] iArr = this.mCachedCompatChanges;
        if (iArr[cachedCompatChangeId] == -1) {
            iArr[cachedCompatChangeId] = this.mService.mOomAdjuster.isChangeEnabled(cachedCompatChangeId, this.mApp.info, false) ? 1 : 0;
        }
        return this.mCachedCompatChanges[cachedCompatChangeId] == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeOomAdjFromActivitiesIfNecessary(OomAdjuster.ComputeOomAdjWindowCallback callback, int adj, boolean foregroundActivities, boolean hasVisibleActivities, int procState, int schedGroup, int appUid, int logUid, int processCurTop) {
        if (this.mCachedAdj != -10000) {
            return;
        }
        callback.initialize(this.mApp, adj, foregroundActivities, hasVisibleActivities, procState, schedGroup, appUid, logUid, processCurTop);
        int minLayer = Math.min(99, this.mApp.getWindowProcessController().computeOomAdjFromActivities(callback));
        this.mCachedAdj = callback.adj;
        this.mCachedForegroundActivities = callback.foregroundActivities;
        this.mCachedHasVisibleActivities = callback.mHasVisibleActivities ? 1 : 0;
        this.mCachedProcState = callback.procState;
        this.mCachedSchedGroup = callback.schedGroup;
        int i = this.mCachedAdj;
        if (i == 100) {
            this.mCachedAdj = i + minLayer;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCachedAdj() {
        return this.mCachedAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getCachedForegroundActivities() {
        return this.mCachedForegroundActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCachedProcState() {
        return this.mCachedProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCachedSchedGroup() {
        return this.mCachedSchedGroup;
    }

    public String makeAdjReason() {
        if (this.mAdjSource != null || this.mAdjTarget != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(' ');
            Object obj = this.mAdjTarget;
            if (obj instanceof ComponentName) {
                sb.append(((ComponentName) obj).flattenToShortString());
            } else if (obj != null) {
                sb.append(obj.toString());
            } else {
                sb.append("{null}");
            }
            sb.append("<=");
            Object obj2 = this.mAdjSource;
            if (obj2 instanceof ProcessRecord) {
                sb.append("Proc{");
                sb.append(((ProcessRecord) this.mAdjSource).toShortString());
                sb.append("}");
            } else if (obj2 != null) {
                sb.append(obj2.toString());
            } else {
                sb.append("{null}");
            }
            return sb.toString();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLSP() {
        setHasForegroundActivities(false);
        this.mHasShownUi = false;
        this.mForcingToImportant = null;
        this.mVerifiedAdj = -10000;
        this.mSetAdj = -10000;
        this.mCurAdj = -10000;
        this.mSetRawAdj = -10000;
        this.mCurRawAdj = -10000;
        this.mSetCapability = 0;
        this.mCurCapability = 0;
        this.mSetSchedGroup = 0;
        this.mCurSchedGroup = 0;
        this.mSetProcState = 20;
        this.mCurRawProcState = 20;
        this.mCurProcState = 20;
        int i = 0;
        while (true) {
            int[] iArr = this.mCachedCompatChanges;
            if (i < iArr.length) {
                iArr[i] = -1;
                i++;
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAllowedStartFgs() {
        return this.mCurProcState <= 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBackgroundRestricted() {
        return this.mBackgroundRestricted;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBackgroundRestricted(boolean restricted) {
        this.mBackgroundRestricted = restricted;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurBoundByNonBgRestrictedApp() {
        return this.mCurBoundByNonBgRestrictedApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurBoundByNonBgRestrictedApp(boolean bound) {
        this.mCurBoundByNonBgRestrictedApp = bound;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSetBoundByNonBgRestrictedApp() {
        return this.mSetBoundByNonBgRestrictedApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetBoundByNonBgRestrictedApp(boolean bound) {
        this.mSetBoundByNonBgRestrictedApp = bound;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLastInvisibleTime(boolean hasVisibleActivities) {
        if (!hasVisibleActivities) {
            if (this.mLastInvisibleTime == JobStatus.NO_LATEST_RUNTIME) {
                this.mLastInvisibleTime = SystemClock.elapsedRealtime();
                return;
            }
            return;
        }
        this.mLastInvisibleTime = JobStatus.NO_LATEST_RUNTIME;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastInvisibleTime() {
        return this.mLastInvisibleTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNoKillOnBgRestrictedAndIdle(boolean shouldNotKill) {
        this.mNoKillOnBgRestrictedAndIdle = shouldNotKill;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldNotKillOnBgRestrictedAndIdle() {
        return this.mNoKillOnBgRestrictedAndIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetCached(boolean cached) {
        this.mSetCached = cached;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSetCached() {
        return this.mSetCached;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetNoKillOnBgRestrictedAndIdle(boolean shouldNotKill) {
        this.mSetNoKillOnBgRestrictedAndIdle = shouldNotKill;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSetNoKillOnBgRestrictedAndIdle() {
        return this.mSetNoKillOnBgRestrictedAndIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastCanKillOnBgRestrictedAndIdleTime(long now) {
        this.mLastCanKillOnBgRestrictedAndIdleTime = now;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCanKillOnBgRestrictedAndIdleTime() {
        return this.mLastCanKillOnBgRestrictedAndIdleTime;
    }

    public void setCacheOomRankerRss(long rss, long rssTimeMs) {
        this.mCacheOomRankerRss = rss;
        this.mCacheOomRankerRssTimeMs = rssTimeMs;
    }

    public long getCacheOomRankerRss() {
        return this.mCacheOomRankerRss;
    }

    public long getCacheOomRankerRssTimeMs() {
        return this.mCacheOomRankerRssTimeMs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        if (this.mReportedInteraction || this.mFgInteractionTime != 0) {
            pw.print(prefix);
            pw.print("reportedInteraction=");
            pw.print(this.mReportedInteraction);
            if (this.mInteractionEventTime != 0) {
                pw.print(" time=");
                TimeUtils.formatDuration(this.mInteractionEventTime, SystemClock.elapsedRealtime(), pw);
            }
            if (this.mFgInteractionTime != 0) {
                pw.print(" fgInteractionTime=");
                TimeUtils.formatDuration(this.mFgInteractionTime, SystemClock.elapsedRealtime(), pw);
            }
            pw.println();
        }
        pw.print(prefix);
        pw.print("adjSeq=");
        pw.print(this.mAdjSeq);
        pw.print(" lruSeq=");
        pw.println(this.mApp.getLruSeq());
        pw.print(prefix);
        pw.print("oom adj: max=");
        pw.print(this.mMaxAdj);
        pw.print(" curRaw=");
        pw.print(this.mCurRawAdj);
        pw.print(" setRaw=");
        pw.print(this.mSetRawAdj);
        pw.print(" cur=");
        pw.print(this.mCurAdj);
        pw.print(" set=");
        pw.println(this.mSetAdj);
        pw.print(prefix);
        pw.print("mCurSchedGroup=");
        pw.print(this.mCurSchedGroup);
        pw.print(" setSchedGroup=");
        pw.print(this.mSetSchedGroup);
        pw.print(" systemNoUi=");
        pw.println(this.mSystemNoUi);
        pw.print(prefix);
        pw.print("curProcState=");
        pw.print(getCurProcState());
        pw.print(" mRepProcState=");
        pw.print(this.mRepProcState);
        pw.print(" setProcState=");
        pw.print(this.mSetProcState);
        pw.print(" lastStateTime=");
        TimeUtils.formatDuration(getLastStateTime(), nowUptime, pw);
        pw.println();
        pw.print(prefix);
        pw.print("curCapability=");
        ActivityManager.printCapabilitiesFull(pw, this.mCurCapability);
        pw.print(" setCapability=");
        ActivityManager.printCapabilitiesFull(pw, this.mSetCapability);
        pw.println();
        if (this.mBackgroundRestricted) {
            pw.print(" backgroundRestricted=");
            pw.print(this.mBackgroundRestricted);
            pw.print(" boundByNonBgRestrictedApp=");
            pw.print(this.mSetBoundByNonBgRestrictedApp);
        }
        pw.println();
        if (this.mHasShownUi || this.mApp.mProfile.hasPendingUiClean()) {
            pw.print(prefix);
            pw.print("hasShownUi=");
            pw.print(this.mHasShownUi);
            pw.print(" pendingUiClean=");
            pw.println(this.mApp.mProfile.hasPendingUiClean());
        }
        pw.print(prefix);
        pw.print("cached=");
        pw.print(this.mCached);
        pw.print(" empty=");
        pw.println(this.mEmpty);
        if (this.mServiceB) {
            pw.print(prefix);
            pw.print("serviceb=");
            pw.print(this.mServiceB);
            pw.print(" serviceHighRam=");
            pw.println(this.mServiceHighRam);
        }
        if (this.mNotCachedSinceIdle) {
            pw.print(prefix);
            pw.print("notCachedSinceIdle=");
            pw.print(this.mNotCachedSinceIdle);
            pw.print(" initialIdlePss=");
            pw.println(this.mApp.mProfile.getInitialIdlePss());
        }
        if (hasTopUi() || hasOverlayUi() || this.mRunningRemoteAnimation) {
            pw.print(prefix);
            pw.print("hasTopUi=");
            pw.print(hasTopUi());
            pw.print(" hasOverlayUi=");
            pw.print(hasOverlayUi());
            pw.print(" runningRemoteAnimation=");
            pw.println(this.mRunningRemoteAnimation);
        }
        if (this.mHasForegroundActivities || this.mRepForegroundActivities) {
            pw.print(prefix);
            pw.print("foregroundActivities=");
            pw.print(this.mHasForegroundActivities);
            pw.print(" (rep=");
            pw.print(this.mRepForegroundActivities);
            pw.println(")");
        }
        if (this.mSetProcState > 10) {
            pw.print(prefix);
            pw.print("whenUnimportant=");
            TimeUtils.formatDuration(this.mWhenUnimportant - nowUptime, pw);
            pw.println();
        }
        if (this.mLastTopTime > 0) {
            pw.print(prefix);
            pw.print("lastTopTime=");
            TimeUtils.formatDuration(this.mLastTopTime, nowUptime, pw);
            pw.println();
        }
        long j = this.mLastInvisibleTime;
        if (j > 0 && j < JobStatus.NO_LATEST_RUNTIME) {
            pw.print(prefix);
            pw.print("lastInvisibleTime=");
            long elapsedRealtimeNow = SystemClock.elapsedRealtime();
            long currentTimeNow = System.currentTimeMillis();
            long lastInvisibleCurrentTime = (currentTimeNow - elapsedRealtimeNow) + this.mLastInvisibleTime;
            TimeUtils.dumpTimeWithDelta(pw, lastInvisibleCurrentTime, currentTimeNow);
            pw.println();
        }
        if (this.mHasStartedServices) {
            pw.print(prefix);
            pw.print("hasStartedServices=");
            pw.println(this.mHasStartedServices);
        }
        pw.print("isAgaresImprovedMaxAdj");
        pw.print(this.mAgaresImproveMaxAdj);
    }
}
