package com.android.server.tare;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.TimeUtils;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.UserManagerInternal;
import com.android.server.tare.EconomicPolicy;
import com.android.server.tare.EconomyManagerInternal;
import com.android.server.tare.Ledger;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.utils.AlarmQueue;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Agent {
    private static final String ALARM_TAG_AFFORDABILITY_CHECK = "*tare.affordability_check*";
    private static final boolean DEBUG;
    private static final int MSG_CHECK_ALL_AFFORDABILITY = 0;
    private static final int MSG_CHECK_INDIVIDUAL_AFFORDABILITY = 1;
    private static final String TAG;
    private final Analyst mAnalyst;
    private final BalanceThresholdAlarmQueue mBalanceThresholdAlarmQueue;
    private final InternalResourceService mIrs;
    private final Object mLock;
    private final Scribe mScribe;
    private final SparseArrayMap<String, SparseArrayMap<String, OngoingEvent>> mCurrentOngoingEvents = new SparseArrayMap<>();
    private final SparseArrayMap<String, ArraySet<ActionAffordabilityNote>> mActionAffordabilityNotes = new SparseArrayMap<>();
    private final TotalDeltaCalculator mTotalDeltaCalculator = new TotalDeltaCalculator();
    private final TrendCalculator mTrendCalculator = new TrendCalculator();
    private final OngoingEventUpdater mOngoingEventUpdater = new OngoingEventUpdater();
    private final Handler mHandler = new AgentHandler(TareHandlerThread.get().getLooper());
    private final AppStandbyInternal mAppStandbyInternal = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);

    static {
        String str = "TARE-" + Agent.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Agent(InternalResourceService irs, Scribe scribe, Analyst analyst) {
        this.mLock = irs.getLock();
        this.mIrs = irs;
        this.mScribe = scribe;
        this.mAnalyst = analyst;
        this.mBalanceThresholdAlarmQueue = new BalanceThresholdAlarmQueue(irs.getContext(), TareHandlerThread.get().getLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TotalDeltaCalculator implements Consumer<OngoingEvent> {
        private Ledger mLedger;
        private long mNow;
        private long mNowElapsed;
        private long mTotal;

        private TotalDeltaCalculator() {
        }

        void reset(Ledger ledger, long nowElapsed, long now) {
            this.mLedger = ledger;
            this.mNowElapsed = nowElapsed;
            this.mNow = now;
            this.mTotal = 0L;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(OngoingEvent ongoingEvent) {
            this.mTotal += Agent.this.getActualDeltaLocked(ongoingEvent, this.mLedger, this.mNowElapsed, this.mNow).price;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getBalanceLocked(int userId, String pkgName) {
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        long balance = ledger.getCurrentBalance();
        SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) this.mCurrentOngoingEvents.get(userId, pkgName);
        if (ongoingEvents != null) {
            long nowElapsed = SystemClock.elapsedRealtime();
            long now = TareUtils.getCurrentTimeMillis();
            this.mTotalDeltaCalculator.reset(ledger, nowElapsed, now);
            ongoingEvents.forEach(this.mTotalDeltaCalculator);
            return balance + this.mTotalDeltaCalculator.mTotal;
        }
        return balance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAffordableLocked(long balance, long price, long ctp) {
        return balance >= price && this.mScribe.getRemainingConsumableCakesLocked() >= ctp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteInstantaneousEventLocked(int userId, String pkgName, int eventId, String tag) {
        if (this.mIrs.isSystem(userId, pkgName)) {
            return;
        }
        long now = TareUtils.getCurrentTimeMillis();
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        int eventType = EconomicPolicy.getEventType(eventId);
        switch (eventType) {
            case Integer.MIN_VALUE:
                EconomicPolicy.Reward reward = economicPolicy.getReward(eventId);
                if (reward == null) {
                    break;
                } else {
                    long rewardSum = ledger.get24HourSum(eventId, now);
                    long rewardVal = Math.max(0L, Math.min(reward.maxDailyReward - rewardSum, reward.instantReward));
                    recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, eventId, tag, rewardVal, 0L), true);
                    break;
                }
            case 1073741824:
                EconomicPolicy.Cost actionCost = economicPolicy.getCostOfAction(eventId, userId, pkgName);
                recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, eventId, tag, -actionCost.price, actionCost.costToProduce), true);
                break;
            default:
                Slog.w(TAG, "Unsupported event type: " + eventType);
                break;
        }
        scheduleBalanceCheckLocked(userId, pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteOngoingEventLocked(int userId, String pkgName, int eventId, String tag, long startElapsed) {
        noteOngoingEventLocked(userId, pkgName, eventId, tag, startElapsed, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void noteOngoingEventLocked(int userId, String pkgName, int eventId, String tag, long startElapsed, boolean updateBalanceCheck) {
        SparseArrayMap<String, OngoingEvent> ongoingEvents;
        if (this.mIrs.isSystem(userId, pkgName)) {
            return;
        }
        SparseArrayMap<String, OngoingEvent> ongoingEvents2 = (SparseArrayMap) this.mCurrentOngoingEvents.get(userId, pkgName);
        if (ongoingEvents2 != null) {
            ongoingEvents = ongoingEvents2;
        } else {
            SparseArrayMap<String, OngoingEvent> ongoingEvents3 = new SparseArrayMap<>();
            this.mCurrentOngoingEvents.add(userId, pkgName, ongoingEvents3);
            ongoingEvents = ongoingEvents3;
        }
        OngoingEvent ongoingEvent = (OngoingEvent) ongoingEvents.get(eventId, tag);
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        int eventType = EconomicPolicy.getEventType(eventId);
        switch (eventType) {
            case Integer.MIN_VALUE:
                EconomicPolicy.Reward reward = economicPolicy.getReward(eventId);
                if (reward != null) {
                    if (ongoingEvent == null) {
                        ongoingEvents.add(eventId, tag, new OngoingEvent(eventId, tag, startElapsed, reward));
                        break;
                    } else {
                        ongoingEvent.refCount++;
                        break;
                    }
                }
                break;
            case 1073741824:
                EconomicPolicy.Cost actionCost = economicPolicy.getCostOfAction(eventId, userId, pkgName);
                if (ongoingEvent == null) {
                    ongoingEvents.add(eventId, tag, new OngoingEvent(eventId, tag, startElapsed, actionCost));
                    break;
                } else {
                    ongoingEvent.refCount++;
                    break;
                }
            default:
                Slog.w(TAG, "Unsupported event type: " + eventType);
                break;
        }
        if (updateBalanceCheck) {
            scheduleBalanceCheckLocked(userId, pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDeviceStateChangedLocked() {
        onPricingChangedLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPricingChangedLocked() {
        onAnythingChangedLocked(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppStatesChangedLocked(int userId, ArraySet<String> pkgNames) {
        long now;
        long now2 = TareUtils.getCurrentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        int i = 0;
        while (i < pkgNames.size()) {
            String pkgName = pkgNames.valueAt(i);
            SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) this.mCurrentOngoingEvents.get(userId, pkgName);
            if (ongoingEvents == null) {
                now = now2;
            } else {
                now = now2;
                this.mOngoingEventUpdater.reset(userId, pkgName, now2, nowElapsed);
                ongoingEvents.forEach(this.mOngoingEventUpdater);
                ArraySet<ActionAffordabilityNote> actionAffordabilityNotes = (ArraySet) this.mActionAffordabilityNotes.get(userId, pkgName);
                if (actionAffordabilityNotes != null) {
                    int size = actionAffordabilityNotes.size();
                    long newBalance = this.mScribe.getLedgerLocked(userId, pkgName).getCurrentBalance();
                    int n = 0;
                    while (n < size) {
                        ActionAffordabilityNote note = actionAffordabilityNotes.valueAt(n);
                        note.recalculateCosts(economicPolicy, userId, pkgName);
                        int n2 = n;
                        ArraySet<ActionAffordabilityNote> actionAffordabilityNotes2 = actionAffordabilityNotes;
                        int size2 = size;
                        boolean isAffordable = isAffordableLocked(newBalance, note.getCachedModifiedPrice(), note.getCtp());
                        if (note.isCurrentlyAffordable() != isAffordable) {
                            note.setNewAffordability(isAffordable);
                            this.mIrs.postAffordabilityChanged(userId, pkgName, note);
                        }
                        n = n2 + 1;
                        actionAffordabilityNotes = actionAffordabilityNotes2;
                        size = size2;
                    }
                }
                scheduleBalanceCheckLocked(userId, pkgName);
            }
            i++;
            now2 = now;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAnythingChangedLocked(boolean updateOngoingEvents) {
        long now;
        String pkgName;
        long now2 = TareUtils.getCurrentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        for (int uIdx = this.mCurrentOngoingEvents.numMaps() - 1; uIdx >= 0; uIdx--) {
            int userId = this.mCurrentOngoingEvents.keyAt(uIdx);
            int pIdx = this.mCurrentOngoingEvents.numElementsForKey(userId) - 1;
            while (pIdx >= 0) {
                String pkgName2 = (String) this.mCurrentOngoingEvents.keyAt(uIdx, pIdx);
                SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) this.mCurrentOngoingEvents.valueAt(uIdx, pIdx);
                if (ongoingEvents == null) {
                    now = now2;
                } else {
                    if (!updateOngoingEvents) {
                        now = now2;
                        pkgName = pkgName2;
                    } else {
                        long j = now2;
                        now = now2;
                        pkgName = pkgName2;
                        this.mOngoingEventUpdater.reset(userId, pkgName2, j, nowElapsed);
                        ongoingEvents.forEach(this.mOngoingEventUpdater);
                    }
                    scheduleBalanceCheckLocked(userId, pkgName);
                }
                pIdx--;
                now2 = now;
            }
        }
        for (int uIdx2 = this.mActionAffordabilityNotes.numMaps() - 1; uIdx2 >= 0; uIdx2--) {
            int userId2 = this.mActionAffordabilityNotes.keyAt(uIdx2);
            for (int pIdx2 = this.mActionAffordabilityNotes.numElementsForKey(userId2) - 1; pIdx2 >= 0; pIdx2--) {
                String pkgName3 = (String) this.mActionAffordabilityNotes.keyAt(uIdx2, pIdx2);
                ArraySet<ActionAffordabilityNote> actionAffordabilityNotes = (ArraySet) this.mActionAffordabilityNotes.valueAt(uIdx2, pIdx2);
                if (actionAffordabilityNotes != null) {
                    int size = actionAffordabilityNotes.size();
                    long newBalance = getBalanceLocked(userId2, pkgName3);
                    int n = 0;
                    while (n < size) {
                        ActionAffordabilityNote note = actionAffordabilityNotes.valueAt(n);
                        note.recalculateCosts(economicPolicy, userId2, pkgName3);
                        int size2 = size;
                        int n2 = n;
                        boolean isAffordable = isAffordableLocked(newBalance, note.getCachedModifiedPrice(), note.getCtp());
                        if (note.isCurrentlyAffordable() != isAffordable) {
                            note.setNewAffordability(isAffordable);
                            this.mIrs.postAffordabilityChanged(userId2, pkgName3, note);
                        }
                        n = n2 + 1;
                        size = size2;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopOngoingActionLocked(int userId, String pkgName, int eventId, String tag, long nowElapsed, long now) {
        stopOngoingActionLocked(userId, pkgName, eventId, tag, nowElapsed, now, true, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopOngoingActionLocked(int userId, String pkgName, int eventId, String tag, long nowElapsed, long now, boolean updateBalanceCheck, boolean notifyOnAffordabilityChange) {
        if (this.mIrs.isSystem(userId, pkgName)) {
            return;
        }
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) this.mCurrentOngoingEvents.get(userId, pkgName);
        if (ongoingEvents == null) {
            Slog.w(TAG, "No ongoing transactions for " + TareUtils.appToString(userId, pkgName));
            return;
        }
        OngoingEvent ongoingEvent = (OngoingEvent) ongoingEvents.get(eventId, tag);
        if (ongoingEvent == null) {
            Slog.w(TAG, "Nonexistent ongoing transaction " + EconomicPolicy.eventToString(eventId) + (tag == null ? "" : ":" + tag) + " for " + TareUtils.appToString(userId, pkgName) + " ended");
            return;
        }
        ongoingEvent.refCount--;
        if (ongoingEvent.refCount <= 0) {
            long startElapsed = ongoingEvent.startTimeElapsed;
            long startTime = now - (nowElapsed - startElapsed);
            EconomicPolicy.Cost actualDelta = getActualDeltaLocked(ongoingEvent, ledger, nowElapsed, now);
            recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(startTime, now, eventId, tag, actualDelta.price, actualDelta.costToProduce), notifyOnAffordabilityChange);
            ongoingEvents.delete(eventId, tag);
        }
        if (updateBalanceCheck) {
            scheduleBalanceCheckLocked(userId, pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public EconomicPolicy.Cost getActualDeltaLocked(OngoingEvent ongoingEvent, Ledger ledger, long nowElapsed, long now) {
        long startElapsed = ongoingEvent.startTimeElapsed;
        long durationSecs = (nowElapsed - startElapsed) / 1000;
        long computedDelta = ongoingEvent.getDeltaPerSec() * durationSecs;
        if (ongoingEvent.reward == null) {
            return new EconomicPolicy.Cost(ongoingEvent.getCtpPerSec() * durationSecs, computedDelta);
        }
        long rewardSum = ledger.get24HourSum(ongoingEvent.eventId, now);
        return new EconomicPolicy.Cost(0L, Math.max(0L, Math.min(ongoingEvent.reward.maxDailyReward - rewardSum, computedDelta)));
    }

    /* JADX WARN: Removed duplicated region for block: B:31:0x011d  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0156  */
    /* JADX WARN: Removed duplicated region for block: B:43:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void recordTransactionLocked(int userId, String pkgName, Ledger ledger, Ledger.Transaction transaction, boolean notifyOnAffordabilityChange) {
        Ledger.Transaction transaction2;
        ArraySet<ActionAffordabilityNote> actionAffordabilityNotes;
        int i;
        if (!DEBUG && transaction.delta == 0) {
            return;
        }
        if (this.mIrs.isSystem(userId, pkgName)) {
            Slog.wtfStack(TAG, "Tried to adjust system balance for " + TareUtils.appToString(userId, pkgName));
            return;
        }
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        long originalBalance = ledger.getCurrentBalance();
        if (transaction.delta > 0 && transaction.delta + originalBalance > economicPolicy.getMaxSatiatedBalance()) {
            long newDelta = Math.max(0L, economicPolicy.getMaxSatiatedBalance() - originalBalance);
            Slog.i(TAG, "Would result in becoming too rich. Decreasing transaction " + EconomicPolicy.eventToString(transaction.eventId) + (transaction.tag == null ? "" : ":" + transaction.tag) + " for " + TareUtils.appToString(userId, pkgName) + " by " + TareUtils.cakeToString(transaction.delta - newDelta));
            transaction2 = new Ledger.Transaction(transaction.startTimeMs, transaction.endTimeMs, transaction.eventId, transaction.tag, newDelta, transaction.ctp);
            ledger.recordTransaction(transaction2);
            this.mScribe.adjustRemainingConsumableCakesLocked(-transaction2.ctp);
            this.mAnalyst.noteTransaction(transaction2);
            if (transaction2.delta != 0 && notifyOnAffordabilityChange && (actionAffordabilityNotes = (ArraySet) this.mActionAffordabilityNotes.get(userId, pkgName)) != null) {
                long newBalance = ledger.getCurrentBalance();
                for (i = 0; i < actionAffordabilityNotes.size(); i++) {
                    ActionAffordabilityNote note = actionAffordabilityNotes.valueAt(i);
                    boolean isAffordable = isAffordableLocked(newBalance, note.getCachedModifiedPrice(), note.getCtp());
                    if (note.isCurrentlyAffordable() != isAffordable) {
                        note.setNewAffordability(isAffordable);
                        this.mIrs.postAffordabilityChanged(userId, pkgName, note);
                    }
                }
            }
            if (transaction2.ctp == 0) {
                this.mHandler.sendEmptyMessage(0);
                this.mIrs.maybePerformQuantitativeEasingLocked();
                return;
            }
            return;
        }
        transaction2 = transaction;
        ledger.recordTransaction(transaction2);
        this.mScribe.adjustRemainingConsumableCakesLocked(-transaction2.ctp);
        this.mAnalyst.noteTransaction(transaction2);
        if (transaction2.delta != 0) {
            long newBalance2 = ledger.getCurrentBalance();
            while (i < actionAffordabilityNotes.size()) {
            }
        }
        if (transaction2.ctp == 0) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reclaimUnusedAssetsLocked(double percentage, long minUnusedTimeMs, boolean scaleMinBalance) {
        int p;
        int userId;
        int u;
        long minBalance;
        long toReclaim;
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        SparseArrayMap<String, Ledger> ledgers = this.mScribe.getLedgersLocked();
        long now = TareUtils.getCurrentTimeMillis();
        int u2 = 0;
        while (u2 < ledgers.numMaps()) {
            int userId2 = ledgers.keyAt(u2);
            int p2 = 0;
            while (p2 < ledgers.numElementsForKey(userId2)) {
                Ledger ledger = (Ledger) ledgers.valueAt(u2, p2);
                long curBalance = ledger.getCurrentBalance();
                if (curBalance <= 0) {
                    p = p2;
                    userId = userId2;
                    u = u2;
                } else {
                    String pkgName = (String) ledgers.keyAt(u2, p2);
                    long timeSinceLastUsedMs = this.mAppStandbyInternal.getTimeSinceLastUsedByUser(pkgName, userId2);
                    if (timeSinceLastUsedMs >= minUnusedTimeMs) {
                        if (!scaleMinBalance) {
                            minBalance = economicPolicy.getMinSatiatedBalance(userId2, pkgName);
                        } else {
                            minBalance = this.mIrs.getMinBalanceLocked(userId2, pkgName);
                        }
                        long toReclaim2 = (long) (curBalance * percentage);
                        if (curBalance - toReclaim2 >= minBalance) {
                            toReclaim = toReclaim2;
                        } else {
                            toReclaim = curBalance - minBalance;
                        }
                        if (toReclaim > 0) {
                            if (DEBUG) {
                                Slog.i(TAG, "Reclaiming unused wealth! Taking " + toReclaim + " from " + TareUtils.appToString(userId2, pkgName));
                            }
                            p = p2;
                            userId = userId2;
                            u = u2;
                            recordTransactionLocked(userId2, pkgName, ledger, new Ledger.Transaction(now, now, 2, null, -toReclaim, 0L), true);
                        } else {
                            p = p2;
                            userId = userId2;
                            u = u2;
                        }
                    } else {
                        p = p2;
                        userId = userId2;
                        u = u2;
                    }
                }
                p2 = p + 1;
                userId2 = userId;
                u2 = u;
            }
            u2++;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppUnexemptedLocked(int userId, String pkgName) {
        double percentageToReclaim;
        long curBalance = getBalanceLocked(userId, pkgName);
        long minBalance = this.mIrs.getMinBalanceLocked(userId, pkgName);
        if (curBalance <= minBalance) {
            return;
        }
        long timeSinceLastUsedMs = this.mAppStandbyInternal.getTimeSinceLastUsedByUser(pkgName, userId);
        if (timeSinceLastUsedMs < 86400000) {
            percentageToReclaim = 0.25d;
        } else if (timeSinceLastUsedMs < 172800000) {
            percentageToReclaim = 0.5d;
        } else if (timeSinceLastUsedMs < 259200000) {
            percentageToReclaim = 0.75d;
        } else {
            percentageToReclaim = 1.0d;
        }
        long overage = curBalance - minBalance;
        long toReclaim = (long) (overage * percentageToReclaim);
        if (toReclaim > 0) {
            if (DEBUG) {
                Slog.i(TAG, "Reclaiming bonus wealth! Taking " + toReclaim + " from " + TareUtils.appToString(userId, pkgName));
            }
            long now = TareUtils.getCurrentTimeMillis();
            Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
            recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, 4, null, -toReclaim, 0L), true);
        }
    }

    private boolean shouldGiveCredits(PackageInfo packageInfo) {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo == null || !applicationInfo.hasCode()) {
            return false;
        }
        int userId = UserHandle.getUserId(packageInfo.applicationInfo.uid);
        return !this.mIrs.isSystem(userId, packageInfo.packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCreditSupplyChanged() {
        this.mHandler.sendEmptyMessage(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void distributeBasicIncomeLocked(int batteryLevel) {
        int i;
        List<PackageInfo> pkgs = this.mIrs.getInstalledPackages();
        long now = TareUtils.getCurrentTimeMillis();
        int i2 = 0;
        while (i2 < pkgs.size()) {
            PackageInfo pkgInfo = pkgs.get(i2);
            if (!shouldGiveCredits(pkgInfo)) {
                i = i2;
            } else {
                int userId = UserHandle.getUserId(pkgInfo.applicationInfo.uid);
                String pkgName = pkgInfo.packageName;
                Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
                long minBalance = this.mIrs.getMinBalanceLocked(userId, pkgName);
                double perc = batteryLevel / 100.0d;
                long shortfall = minBalance - ledger.getCurrentBalance();
                if (shortfall > 0) {
                    i = i2;
                    recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, 0, null, (long) (shortfall * perc), 0L), true);
                } else {
                    i = i2;
                }
            }
            i2 = i + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantBirthrightsLocked() {
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        int[] userIds = userManagerInternal.getUserIds();
        for (int userId : userIds) {
            grantBirthrightsLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantBirthrightsLocked(int userId) {
        int i;
        List<PackageInfo> pkgs = this.mIrs.getInstalledPackages(userId);
        long now = TareUtils.getCurrentTimeMillis();
        int i2 = 0;
        while (i2 < pkgs.size()) {
            PackageInfo packageInfo = pkgs.get(i2);
            if (!shouldGiveCredits(packageInfo)) {
                i = i2;
            } else {
                String pkgName = packageInfo.packageName;
                Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
                if (ledger.getCurrentBalance() > 0) {
                    Slog.wtf(TAG, "App " + pkgName + " had credits before economy was set up");
                    i = i2;
                } else {
                    i = i2;
                    recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, 1, null, this.mIrs.getMinBalanceLocked(userId, pkgName), 0L), true);
                }
            }
            i2 = i + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantBirthrightLocked(int userId, String pkgName) {
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        if (ledger.getCurrentBalance() > 0) {
            Slog.wtf(TAG, "App " + pkgName + " had credits as soon as it was installed");
            return;
        }
        long now = TareUtils.getCurrentTimeMillis();
        recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, 1, null, this.mIrs.getMinBalanceLocked(userId, pkgName), 0L), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppExemptedLocked(int userId, String pkgName) {
        long minBalance = this.mIrs.getMinBalanceLocked(userId, pkgName);
        long missing = minBalance - getBalanceLocked(userId, pkgName);
        if (missing <= 0) {
            return;
        }
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        long now = TareUtils.getCurrentTimeMillis();
        recordTransactionLocked(userId, pkgName, ledger, new Ledger.Transaction(now, now, 3, null, missing, 0L), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemovedLocked(int userId, String pkgName) {
        reclaimAssetsLocked(userId, pkgName);
        this.mBalanceThresholdAlarmQueue.removeAlarmForKey(new Package(userId, pkgName));
    }

    private void reclaimAssetsLocked(int userId, String pkgName) {
        Ledger ledger = this.mScribe.getLedgerLocked(userId, pkgName);
        if (ledger.getCurrentBalance() != 0) {
            this.mScribe.adjustRemainingConsumableCakesLocked(-ledger.getCurrentBalance());
        }
        this.mScribe.discardLedgerLocked(userId, pkgName);
        this.mCurrentOngoingEvents.delete(userId, pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemovedLocked(int userId, List<String> pkgNames) {
        reclaimAssetsLocked(userId, pkgNames);
        this.mBalanceThresholdAlarmQueue.removeAlarmsForUserId(userId);
    }

    private void reclaimAssetsLocked(int userId, List<String> pkgNames) {
        for (int i = 0; i < pkgNames.size(); i++) {
            reclaimAssetsLocked(userId, pkgNames.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TrendCalculator implements Consumer<OngoingEvent> {
        static final long WILL_NOT_CROSS_THRESHOLD = -1;
        private long mCtpThreshold;
        private long mCurBalance;
        private long mLowerThreshold;
        private long mMaxDeltaPerSecToCtpThreshold;
        private long mMaxDeltaPerSecToLowerThreshold;
        private long mMaxDeltaPerSecToUpperThreshold;
        private long mRemainingConsumableCredits;
        private long mUpperThreshold;

        TrendCalculator() {
        }

        void reset(long curBalance, long remainingConsumableCredits, ArraySet<ActionAffordabilityNote> actionAffordabilityNotes) {
            this.mCurBalance = curBalance;
            this.mRemainingConsumableCredits = remainingConsumableCredits;
            this.mMaxDeltaPerSecToLowerThreshold = 0L;
            this.mMaxDeltaPerSecToUpperThreshold = 0L;
            this.mMaxDeltaPerSecToCtpThreshold = 0L;
            long j = Long.MIN_VALUE;
            this.mUpperThreshold = Long.MIN_VALUE;
            this.mLowerThreshold = JobStatus.NO_LATEST_RUNTIME;
            this.mCtpThreshold = 0L;
            if (actionAffordabilityNotes != null) {
                int i = 0;
                while (i < actionAffordabilityNotes.size()) {
                    ActionAffordabilityNote note = actionAffordabilityNotes.valueAt(i);
                    long price = note.getCachedModifiedPrice();
                    if (price <= this.mCurBalance) {
                        long j2 = this.mLowerThreshold;
                        this.mLowerThreshold = j2 == JobStatus.NO_LATEST_RUNTIME ? price : Math.max(j2, price);
                    } else {
                        long j3 = this.mUpperThreshold;
                        this.mUpperThreshold = j3 == j ? price : Math.min(j3, price);
                    }
                    long ctp = note.getCtp();
                    if (ctp <= this.mRemainingConsumableCredits) {
                        this.mCtpThreshold = Math.max(this.mCtpThreshold, ctp);
                    }
                    i++;
                    j = Long.MIN_VALUE;
                }
            }
        }

        long getTimeToCrossLowerThresholdMs() {
            long j = this.mMaxDeltaPerSecToLowerThreshold;
            if (j == 0 && this.mMaxDeltaPerSecToCtpThreshold == 0) {
                return -1L;
            }
            long minSeconds = JobStatus.NO_LATEST_RUNTIME;
            if (j != 0) {
                minSeconds = (this.mLowerThreshold - this.mCurBalance) / j;
            }
            long j2 = this.mMaxDeltaPerSecToCtpThreshold;
            if (j2 != 0) {
                minSeconds = Math.min(minSeconds, (this.mCtpThreshold - this.mRemainingConsumableCredits) / j2);
            }
            return 1000 * minSeconds;
        }

        long getTimeToCrossUpperThresholdMs() {
            long j = this.mMaxDeltaPerSecToUpperThreshold;
            if (j == 0) {
                return -1L;
            }
            long minSeconds = (this.mUpperThreshold - this.mCurBalance) / j;
            return 1000 * minSeconds;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(OngoingEvent ongoingEvent) {
            long deltaPerSec = ongoingEvent.getDeltaPerSec();
            long j = this.mCurBalance;
            if (j >= this.mLowerThreshold && deltaPerSec < 0) {
                this.mMaxDeltaPerSecToLowerThreshold += deltaPerSec;
            } else if (j < this.mUpperThreshold && deltaPerSec > 0) {
                this.mMaxDeltaPerSecToUpperThreshold += deltaPerSec;
            }
            long ctpPerSec = ongoingEvent.getCtpPerSec();
            if (this.mRemainingConsumableCredits >= this.mCtpThreshold && deltaPerSec < 0) {
                this.mMaxDeltaPerSecToCtpThreshold -= ctpPerSec;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleBalanceCheckLocked(int userId, String pkgName) {
        long timeToThresholdMs;
        SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) this.mCurrentOngoingEvents.get(userId, pkgName);
        if (ongoingEvents == null) {
            this.mBalanceThresholdAlarmQueue.removeAlarmForKey(new Package(userId, pkgName));
            return;
        }
        this.mTrendCalculator.reset(getBalanceLocked(userId, pkgName), this.mScribe.getRemainingConsumableCakesLocked(), (ArraySet) this.mActionAffordabilityNotes.get(userId, pkgName));
        ongoingEvents.forEach(this.mTrendCalculator);
        long lowerTimeMs = this.mTrendCalculator.getTimeToCrossLowerThresholdMs();
        long upperTimeMs = this.mTrendCalculator.getTimeToCrossUpperThresholdMs();
        if (lowerTimeMs == -1) {
            if (upperTimeMs == -1) {
                this.mBalanceThresholdAlarmQueue.removeAlarmForKey(new Package(userId, pkgName));
                return;
            }
            timeToThresholdMs = upperTimeMs;
        } else {
            timeToThresholdMs = upperTimeMs == -1 ? lowerTimeMs : Math.min(lowerTimeMs, upperTimeMs);
        }
        this.mBalanceThresholdAlarmQueue.addAlarm(new Package(userId, pkgName), SystemClock.elapsedRealtime() + timeToThresholdMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tearDownLocked() {
        this.mCurrentOngoingEvents.clear();
        this.mBalanceThresholdAlarmQueue.removeAllAlarms();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class OngoingEvent {
        public final EconomicPolicy.Cost actionCost;
        public final int eventId;
        public int refCount;
        public final EconomicPolicy.Reward reward;
        public final long startTimeElapsed;
        public final String tag;

        OngoingEvent(int eventId, String tag, long startTimeElapsed, EconomicPolicy.Reward reward) {
            this.startTimeElapsed = startTimeElapsed;
            this.eventId = eventId;
            this.tag = tag;
            this.reward = reward;
            this.actionCost = null;
            this.refCount = 1;
        }

        OngoingEvent(int eventId, String tag, long startTimeElapsed, EconomicPolicy.Cost actionCost) {
            this.startTimeElapsed = startTimeElapsed;
            this.eventId = eventId;
            this.tag = tag;
            this.reward = null;
            this.actionCost = actionCost;
            this.refCount = 1;
        }

        long getDeltaPerSec() {
            EconomicPolicy.Cost cost = this.actionCost;
            if (cost != null) {
                return -cost.price;
            }
            EconomicPolicy.Reward reward = this.reward;
            if (reward != null) {
                return reward.ongoingRewardPerSecond;
            }
            Slog.wtfStack(Agent.TAG, "No action or reward in ongoing event?!??!");
            return 0L;
        }

        long getCtpPerSec() {
            EconomicPolicy.Cost cost = this.actionCost;
            if (cost != null) {
                return cost.costToProduce;
            }
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class OngoingEventUpdater implements Consumer<OngoingEvent> {
        private long mNow;
        private long mNowElapsed;
        private String mPkgName;
        private int mUserId;

        private OngoingEventUpdater() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reset(int userId, String pkgName, long now, long nowElapsed) {
            this.mUserId = userId;
            this.mPkgName = pkgName;
            this.mNow = now;
            this.mNowElapsed = nowElapsed;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(OngoingEvent ongoingEvent) {
            Agent.this.stopOngoingActionLocked(this.mUserId, this.mPkgName, ongoingEvent.eventId, ongoingEvent.tag, this.mNowElapsed, this.mNow, false, false);
            Agent.this.noteOngoingEventLocked(this.mUserId, this.mPkgName, ongoingEvent.eventId, ongoingEvent.tag, this.mNowElapsed, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class Package {
        public final String packageName;
        public final int userId;

        Package(int userId, String packageName) {
            this.userId = userId;
            this.packageName = packageName;
        }

        public String toString() {
            return TareUtils.appToString(this.userId, this.packageName);
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Package)) {
                return false;
            }
            Package other = (Package) obj;
            if (this.userId != other.userId || !Objects.equals(this.packageName, other.packageName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return this.packageName.hashCode() + this.userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class BalanceThresholdAlarmQueue extends AlarmQueue<Package> {
        private BalanceThresholdAlarmQueue(Context context, Looper looper) {
            super(context, looper, Agent.ALARM_TAG_AFFORDABILITY_CHECK, "Affordability check", true, 15000L);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.utils.AlarmQueue
        public boolean isForUser(Package key, int userId) {
            return key.userId == userId;
        }

        @Override // com.android.server.utils.AlarmQueue
        protected void processExpiredAlarms(ArraySet<Package> expired) {
            for (int i = 0; i < expired.size(); i++) {
                Package p = expired.valueAt(i);
                Agent.this.mHandler.obtainMessage(1, p.userId, 0, p.packageName).sendToTarget();
            }
        }
    }

    public void registerAffordabilityChangeListenerLocked(int userId, String pkgName, EconomyManagerInternal.AffordabilityChangeListener listener, EconomyManagerInternal.ActionBill bill) {
        ArraySet<ActionAffordabilityNote> actionAffordabilityNotes = (ArraySet) this.mActionAffordabilityNotes.get(userId, pkgName);
        if (actionAffordabilityNotes == null) {
            actionAffordabilityNotes = new ArraySet<>();
            this.mActionAffordabilityNotes.add(userId, pkgName, actionAffordabilityNotes);
        }
        CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
        ActionAffordabilityNote note = new ActionAffordabilityNote(bill, listener, economicPolicy);
        if (actionAffordabilityNotes.add(note)) {
            if (!this.mIrs.isEnabled()) {
                note.setNewAffordability(true);
                return;
            }
            note.recalculateCosts(economicPolicy, userId, pkgName);
            note.setNewAffordability(isAffordableLocked(getBalanceLocked(userId, pkgName), note.getCachedModifiedPrice(), note.getCtp()));
            this.mIrs.postAffordabilityChanged(userId, pkgName, note);
            scheduleBalanceCheckLocked(userId, pkgName);
        }
    }

    public void unregisterAffordabilityChangeListenerLocked(int userId, String pkgName, EconomyManagerInternal.AffordabilityChangeListener listener, EconomyManagerInternal.ActionBill bill) {
        ArraySet<ActionAffordabilityNote> actionAffordabilityNotes = (ArraySet) this.mActionAffordabilityNotes.get(userId, pkgName);
        if (actionAffordabilityNotes != null) {
            CompleteEconomicPolicy economicPolicy = this.mIrs.getCompleteEconomicPolicyLocked();
            ActionAffordabilityNote note = new ActionAffordabilityNote(bill, listener, economicPolicy);
            if (actionAffordabilityNotes.remove(note)) {
                scheduleBalanceCheckLocked(userId, pkgName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class ActionAffordabilityNote {
        private final EconomyManagerInternal.ActionBill mActionBill;
        private long mCtp;
        private boolean mIsAffordable;
        private final EconomyManagerInternal.AffordabilityChangeListener mListener;
        private long mModifiedPrice;

        ActionAffordabilityNote(EconomyManagerInternal.ActionBill bill, EconomyManagerInternal.AffordabilityChangeListener listener, EconomicPolicy economicPolicy) {
            this.mActionBill = bill;
            List<EconomyManagerInternal.AnticipatedAction> anticipatedActions = bill.getAnticipatedActions();
            for (int i = 0; i < anticipatedActions.size(); i++) {
                EconomyManagerInternal.AnticipatedAction aa = anticipatedActions.get(i);
                EconomicPolicy.Action action = economicPolicy.getAction(aa.actionId);
                if (action == null) {
                    throw new IllegalArgumentException("Invalid action id: " + aa.actionId);
                }
            }
            this.mListener = listener;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public EconomyManagerInternal.ActionBill getActionBill() {
            return this.mActionBill;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public EconomyManagerInternal.AffordabilityChangeListener getListener() {
            return this.mListener;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getCachedModifiedPrice() {
            return this.mModifiedPrice;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getCtp() {
            return this.mCtp;
        }

        void recalculateCosts(EconomicPolicy economicPolicy, int userId, String pkgName) {
            long modifiedPrice = 0;
            long ctp = 0;
            List<EconomyManagerInternal.AnticipatedAction> anticipatedActions = this.mActionBill.getAnticipatedActions();
            for (int i = 0; i < anticipatedActions.size(); i++) {
                EconomyManagerInternal.AnticipatedAction aa = anticipatedActions.get(i);
                EconomicPolicy.Cost actionCost = economicPolicy.getCostOfAction(aa.actionId, userId, pkgName);
                modifiedPrice += (actionCost.price * aa.numInstantaneousCalls) + (actionCost.price * (aa.ongoingDurationMs / 1000));
                ctp += (actionCost.costToProduce * aa.numInstantaneousCalls) + (actionCost.costToProduce * (aa.ongoingDurationMs / 1000));
            }
            this.mModifiedPrice = modifiedPrice;
            this.mCtp = ctp;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isCurrentlyAffordable() {
            return this.mIsAffordable;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setNewAffordability(boolean isAffordable) {
            this.mIsAffordable = isAffordable;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof ActionAffordabilityNote) {
                ActionAffordabilityNote other = (ActionAffordabilityNote) o;
                return this.mActionBill.equals(other.mActionBill) && this.mListener.equals(other.mListener);
            }
            return false;
        }

        public int hashCode() {
            int hash = (0 * 31) + Objects.hash(this.mListener);
            return (hash * 31) + this.mActionBill.hashCode();
        }
    }

    /* loaded from: classes2.dex */
    private final class AgentHandler extends Handler {
        AgentHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (Agent.this.mLock) {
                        removeMessages(0);
                        Agent.this.onAnythingChangedLocked(false);
                    }
                    return;
                case 1:
                    int userId = msg.arg1;
                    String pkgName = (String) msg.obj;
                    synchronized (Agent.this.mLock) {
                        ArraySet<ActionAffordabilityNote> actionAffordabilityNotes = (ArraySet) Agent.this.mActionAffordabilityNotes.get(userId, pkgName);
                        if (actionAffordabilityNotes != null && actionAffordabilityNotes.size() > 0) {
                            long newBalance = Agent.this.getBalanceLocked(userId, pkgName);
                            for (int i = 0; i < actionAffordabilityNotes.size(); i++) {
                                ActionAffordabilityNote note = actionAffordabilityNotes.valueAt(i);
                                boolean isAffordable = Agent.this.isAffordableLocked(newBalance, note.getCachedModifiedPrice(), note.getCtp());
                                if (note.isCurrentlyAffordable() != isAffordable) {
                                    note.setNewAffordability(isAffordable);
                                    Agent.this.mIrs.postAffordabilityChanged(userId, pkgName, note);
                                }
                            }
                        }
                        Agent.this.scheduleBalanceCheckLocked(userId, pkgName);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLocked(IndentingPrintWriter pw) {
        Agent agent = this;
        pw.println();
        agent.mBalanceThresholdAlarmQueue.dump(pw);
        pw.println();
        pw.println("Ongoing events:");
        pw.increaseIndent();
        boolean printedEvents = false;
        long nowElapsed = SystemClock.elapsedRealtime();
        int u = agent.mCurrentOngoingEvents.numMaps() - 1;
        while (u >= 0) {
            int userId = agent.mCurrentOngoingEvents.keyAt(u);
            int p = agent.mCurrentOngoingEvents.numElementsForKey(userId) - 1;
            while (p >= 0) {
                String pkgName = (String) agent.mCurrentOngoingEvents.keyAt(u, p);
                SparseArrayMap<String, OngoingEvent> ongoingEvents = (SparseArrayMap) agent.mCurrentOngoingEvents.get(userId, pkgName);
                boolean printedApp = false;
                for (int e = ongoingEvents.numMaps() - 1; e >= 0; e--) {
                    int eventId = ongoingEvents.keyAt(e);
                    int t = ongoingEvents.numElementsForKey(eventId) - 1;
                    while (t >= 0) {
                        if (!printedApp) {
                            printedApp = true;
                            pw.println(TareUtils.appToString(userId, pkgName));
                            pw.increaseIndent();
                        }
                        printedEvents = true;
                        OngoingEvent ongoingEvent = (OngoingEvent) ongoingEvents.valueAt(e, t);
                        pw.print(EconomicPolicy.eventToString(ongoingEvent.eventId));
                        if (ongoingEvent.tag != null) {
                            pw.print("(");
                            pw.print(ongoingEvent.tag);
                            pw.print(")");
                        }
                        pw.print(" runtime=");
                        String pkgName2 = pkgName;
                        SparseArrayMap<String, OngoingEvent> ongoingEvents2 = ongoingEvents;
                        TimeUtils.formatDuration(nowElapsed - ongoingEvent.startTimeElapsed, pw);
                        pw.print(" delta/sec=");
                        pw.print(TareUtils.cakeToString(ongoingEvent.getDeltaPerSec()));
                        long ctp = ongoingEvent.getCtpPerSec();
                        if (ctp != 0) {
                            pw.print(" ctp/sec=");
                            pw.print(TareUtils.cakeToString(ongoingEvent.getCtpPerSec()));
                        }
                        pw.print(" refCount=");
                        pw.print(ongoingEvent.refCount);
                        pw.println();
                        t--;
                        pkgName = pkgName2;
                        ongoingEvents = ongoingEvents2;
                    }
                }
                if (printedApp) {
                    pw.decreaseIndent();
                }
                p--;
                agent = this;
            }
            u--;
            agent = this;
        }
        if (!printedEvents) {
            pw.print("N/A");
        }
        pw.decreaseIndent();
    }
}
