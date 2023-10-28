package com.android.server.am;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.server.AlarmManagerInternal;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
/* loaded from: classes.dex */
public class BroadcastDispatcher {
    private static final String TAG = "BroadcastDispatcher";
    private AlarmManagerInternal mAlarm;
    private final BroadcastConstants mConstants;
    private BroadcastRecord mCurrentBroadcast;
    private final Handler mHandler;
    private final Object mLock;
    private final BroadcastQueue mQueue;
    final SparseIntArray mAlarmUids = new SparseIntArray();
    final AlarmManagerInternal.InFlightListener mAlarmListener = new AlarmManagerInternal.InFlightListener() { // from class: com.android.server.am.BroadcastDispatcher.1
        @Override // com.android.server.AlarmManagerInternal.InFlightListener
        public void broadcastAlarmPending(int recipientUid) {
            synchronized (BroadcastDispatcher.this.mLock) {
                int newCount = BroadcastDispatcher.this.mAlarmUids.get(recipientUid, 0) + 1;
                BroadcastDispatcher.this.mAlarmUids.put(recipientUid, newCount);
                int numEntries = BroadcastDispatcher.this.mDeferredBroadcasts.size();
                int i = 0;
                while (true) {
                    if (i >= numEntries) {
                        break;
                    } else if (recipientUid != ((Deferrals) BroadcastDispatcher.this.mDeferredBroadcasts.get(i)).uid) {
                        i++;
                    } else {
                        Deferrals d = (Deferrals) BroadcastDispatcher.this.mDeferredBroadcasts.remove(i);
                        BroadcastDispatcher.this.mAlarmBroadcasts.add(d);
                        break;
                    }
                }
            }
        }

        @Override // com.android.server.AlarmManagerInternal.InFlightListener
        public void broadcastAlarmComplete(int recipientUid) {
            synchronized (BroadcastDispatcher.this.mLock) {
                int newCount = BroadcastDispatcher.this.mAlarmUids.get(recipientUid, 0) - 1;
                if (newCount >= 0) {
                    BroadcastDispatcher.this.mAlarmUids.put(recipientUid, newCount);
                } else {
                    Slog.wtf(BroadcastDispatcher.TAG, "Undercount of broadcast alarms in flight for " + recipientUid);
                    BroadcastDispatcher.this.mAlarmUids.put(recipientUid, 0);
                }
                if (newCount <= 0) {
                    int numEntries = BroadcastDispatcher.this.mAlarmBroadcasts.size();
                    int i = 0;
                    while (true) {
                        if (i >= numEntries) {
                            break;
                        } else if (recipientUid != ((Deferrals) BroadcastDispatcher.this.mAlarmBroadcasts.get(i)).uid) {
                            i++;
                        } else {
                            Deferrals d = (Deferrals) BroadcastDispatcher.this.mAlarmBroadcasts.remove(i);
                            BroadcastDispatcher.insertLocked(BroadcastDispatcher.this.mDeferredBroadcasts, d);
                            break;
                        }
                    }
                }
            }
        }
    };
    final Runnable mScheduleRunnable = new Runnable() { // from class: com.android.server.am.BroadcastDispatcher.2
        @Override // java.lang.Runnable
        public void run() {
            synchronized (BroadcastDispatcher.this.mLock) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    Slog.v(BroadcastDispatcher.TAG, "Deferral recheck of pending broadcasts");
                }
                BroadcastDispatcher.this.mQueue.scheduleBroadcastsLocked();
                BroadcastDispatcher.this.mRecheckScheduled = false;
            }
        }
    };
    private boolean mRecheckScheduled = false;
    private final ArrayList<BroadcastRecord> mOrderedBroadcasts = new ArrayList<>();
    private final ArrayList<Deferrals> mDeferredBroadcasts = new ArrayList<>();
    private final ArrayList<Deferrals> mAlarmBroadcasts = new ArrayList<>();
    private SparseArray<DeferredBootCompletedBroadcastPerUser> mUser2Deferred = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Deferrals {
        int alarmCount;
        final ArrayList<BroadcastRecord> broadcasts = new ArrayList<>();
        long deferUntil;
        long deferredAt;
        long deferredBy;
        final int uid;

        Deferrals(int uid, long now, long backoff, int count) {
            this.uid = uid;
            this.deferredAt = now;
            this.deferredBy = backoff;
            this.deferUntil = now + backoff;
            this.alarmCount = count;
        }

        void add(BroadcastRecord br) {
            this.broadcasts.add(br);
        }

        int size() {
            return this.broadcasts.size();
        }

        boolean isEmpty() {
            return this.broadcasts.isEmpty();
        }

        void dumpDebug(ProtoOutputStream proto, long fieldId) {
            Iterator<BroadcastRecord> it = this.broadcasts.iterator();
            while (it.hasNext()) {
                BroadcastRecord br = it.next();
                br.dumpDebug(proto, fieldId);
            }
        }

        void dumpLocked(Dumper d) {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d(BroadcastDispatcher.TAG, "dumpLocked start size() = " + this.broadcasts.size());
            }
            Iterator<BroadcastRecord> it = this.broadcasts.iterator();
            while (it.hasNext()) {
                BroadcastRecord br = it.next();
                d.dump(br);
            }
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d(BroadcastDispatcher.TAG, "dumpLocked end size() = " + this.broadcasts.size());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Deferrals{uid=");
            sb.append(this.uid);
            sb.append(", deferUntil=");
            sb.append(this.deferUntil);
            sb.append(", #broadcasts=");
            sb.append(this.broadcasts.size());
            sb.append("}");
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class Dumper {
        final String mDumpPackage;
        String mHeading;
        String mLabel;
        int mOrdinal;
        final PrintWriter mPw;
        final String mQueueName;
        final SimpleDateFormat mSdf;
        boolean mPrinted = false;
        boolean mNeedSep = true;

        Dumper(PrintWriter pw, String queueName, String dumpPackage, SimpleDateFormat sdf) {
            this.mPw = pw;
            this.mQueueName = queueName;
            this.mDumpPackage = dumpPackage;
            this.mSdf = sdf;
        }

        void setHeading(String heading) {
            this.mHeading = heading;
            this.mPrinted = false;
        }

        void setLabel(String label) {
            this.mLabel = "  " + label + " " + this.mQueueName + " #";
            this.mOrdinal = 0;
        }

        boolean didPrint() {
            return this.mPrinted;
        }

        void dump(BroadcastRecord br) {
            String str = this.mDumpPackage;
            if (str == null || str.equals(br.callerPackage)) {
                if (!this.mPrinted) {
                    if (this.mNeedSep) {
                        this.mPw.println();
                    }
                    this.mPrinted = true;
                    this.mNeedSep = true;
                    this.mPw.println("  " + this.mHeading + " [" + this.mQueueName + "]:");
                }
                this.mPw.println(this.mLabel + this.mOrdinal + ":");
                this.mOrdinal++;
                br.dump(this.mPw, "    ", this.mSdf);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DeferredBootCompletedBroadcastPerUser {
        boolean mBootCompletedBroadcastReceived;
        boolean mLockedBootCompletedBroadcastReceived;
        private int mUserId;
        SparseBooleanArray mUidReadyForLockedBootCompletedBroadcast = new SparseBooleanArray();
        SparseBooleanArray mUidReadyForBootCompletedBroadcast = new SparseBooleanArray();
        SparseArray<BroadcastRecord> mDeferredLockedBootCompletedBroadcasts = new SparseArray<>();
        SparseArray<BroadcastRecord> mDeferredBootCompletedBroadcasts = new SparseArray<>();

        DeferredBootCompletedBroadcastPerUser(int userId) {
            this.mUserId = userId;
        }

        public void updateUidReady(int uid) {
            if (!this.mLockedBootCompletedBroadcastReceived || this.mDeferredLockedBootCompletedBroadcasts.size() != 0) {
                this.mUidReadyForLockedBootCompletedBroadcast.put(uid, true);
            }
            if (!this.mBootCompletedBroadcastReceived || this.mDeferredBootCompletedBroadcasts.size() != 0) {
                this.mUidReadyForBootCompletedBroadcast.put(uid, true);
            }
        }

        public void enqueueBootCompletedBroadcasts(String action, SparseArray<BroadcastRecord> deferred) {
            if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
                enqueueBootCompletedBroadcasts(deferred, this.mDeferredLockedBootCompletedBroadcasts, this.mUidReadyForLockedBootCompletedBroadcast);
                this.mLockedBootCompletedBroadcastReceived = true;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    dumpBootCompletedBroadcastRecord(this.mDeferredLockedBootCompletedBroadcasts);
                }
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                enqueueBootCompletedBroadcasts(deferred, this.mDeferredBootCompletedBroadcasts, this.mUidReadyForBootCompletedBroadcast);
                this.mBootCompletedBroadcastReceived = true;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    dumpBootCompletedBroadcastRecord(this.mDeferredBootCompletedBroadcasts);
                }
            }
        }

        private void enqueueBootCompletedBroadcasts(SparseArray<BroadcastRecord> from, SparseArray<BroadcastRecord> into, SparseBooleanArray uidReadyForReceiver) {
            for (int i = uidReadyForReceiver.size() - 1; i >= 0; i--) {
                if (from.indexOfKey(uidReadyForReceiver.keyAt(i)) < 0) {
                    uidReadyForReceiver.removeAt(i);
                }
            }
            int size = from.size();
            for (int i2 = 0; i2 < size; i2++) {
                int uid = from.keyAt(i2);
                into.put(uid, from.valueAt(i2));
                if (uidReadyForReceiver.indexOfKey(uid) < 0) {
                    uidReadyForReceiver.put(uid, false);
                }
            }
        }

        public BroadcastRecord dequeueDeferredBootCompletedBroadcast(boolean isAllUidReady) {
            BroadcastRecord next = dequeueDeferredBootCompletedBroadcast(this.mDeferredLockedBootCompletedBroadcasts, this.mUidReadyForLockedBootCompletedBroadcast, isAllUidReady);
            if (next == null) {
                return dequeueDeferredBootCompletedBroadcast(this.mDeferredBootCompletedBroadcasts, this.mUidReadyForBootCompletedBroadcast, isAllUidReady);
            }
            return next;
        }

        private BroadcastRecord dequeueDeferredBootCompletedBroadcast(SparseArray<BroadcastRecord> uid2br, SparseBooleanArray uidReadyForReceiver, boolean isAllUidReady) {
            int size = uid2br.size();
            for (int i = 0; i < size; i++) {
                int uid = uid2br.keyAt(i);
                if (isAllUidReady || uidReadyForReceiver.get(uid)) {
                    BroadcastRecord br = uid2br.valueAt(i);
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                        Object receiver = br.receivers.get(0);
                        if (receiver instanceof BroadcastFilter) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                Slog.i(BroadcastDispatcher.TAG, "getDeferredBootCompletedBroadcast uid:" + uid + " BroadcastFilter:" + ((BroadcastFilter) receiver) + " broadcast:" + br.intent.getAction());
                            }
                        } else {
                            ResolveInfo info = (ResolveInfo) receiver;
                            String packageName = info.activityInfo.applicationInfo.packageName;
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                                Slog.i(BroadcastDispatcher.TAG, "getDeferredBootCompletedBroadcast uid:" + uid + " packageName:" + packageName + " broadcast:" + br.intent.getAction());
                            }
                        }
                    }
                    uid2br.removeAt(i);
                    if (uid2br.size() == 0) {
                        uidReadyForReceiver.clear();
                    }
                    return br;
                }
            }
            return null;
        }

        private SparseArray<BroadcastRecord> getDeferredList(String action) {
            if (action.equals("android.intent.action.LOCKED_BOOT_COMPLETED")) {
                SparseArray<BroadcastRecord> brs = this.mDeferredLockedBootCompletedBroadcasts;
                return brs;
            } else if (!action.equals("android.intent.action.BOOT_COMPLETED")) {
                return null;
            } else {
                SparseArray<BroadcastRecord> brs2 = this.mDeferredBootCompletedBroadcasts;
                return brs2;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getBootCompletedBroadcastsUidsSize(String action) {
            SparseArray<BroadcastRecord> brs = getDeferredList(action);
            if (brs != null) {
                return brs.size();
            }
            return 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getBootCompletedBroadcastsReceiversSize(String action) {
            SparseArray<BroadcastRecord> brs = getDeferredList(action);
            if (brs == null) {
                return 0;
            }
            int size = 0;
            int s = brs.size();
            for (int i = 0; i < s; i++) {
                size += brs.valueAt(i).receivers.size();
            }
            return size;
        }

        public void dump(Dumper dumper, String action) {
            SparseArray<BroadcastRecord> brs = getDeferredList(action);
            if (brs == null) {
                return;
            }
            int size = brs.size();
            for (int i = 0; i < size; i++) {
                dumper.dump(brs.valueAt(i));
            }
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            int size = this.mDeferredLockedBootCompletedBroadcasts.size();
            for (int i = 0; i < size; i++) {
                this.mDeferredLockedBootCompletedBroadcasts.valueAt(i).dumpDebug(proto, fieldId);
            }
            int size2 = this.mDeferredBootCompletedBroadcasts.size();
            for (int i2 = 0; i2 < size2; i2++) {
                this.mDeferredBootCompletedBroadcasts.valueAt(i2).dumpDebug(proto, fieldId);
            }
        }

        private void dumpBootCompletedBroadcastRecord(SparseArray<BroadcastRecord> brs) {
            String packageName;
            int size = brs.size();
            for (int i = 0; i < size; i++) {
                Object receiver = brs.valueAt(i).receivers.get(0);
                if (receiver instanceof BroadcastFilter) {
                    BroadcastFilter recv = (BroadcastFilter) receiver;
                    packageName = recv.receiverList.app.processName;
                } else {
                    ResolveInfo info = (ResolveInfo) receiver;
                    packageName = info.activityInfo.applicationInfo.packageName;
                }
                Slog.i(BroadcastDispatcher.TAG, "uid:" + brs.keyAt(i) + " packageName:" + packageName + " receivers:" + brs.valueAt(i).receivers.size());
            }
        }
    }

    private DeferredBootCompletedBroadcastPerUser getDeferredPerUser(int userId) {
        if (this.mUser2Deferred.contains(userId)) {
            return this.mUser2Deferred.get(userId);
        }
        DeferredBootCompletedBroadcastPerUser temp = new DeferredBootCompletedBroadcastPerUser(userId);
        this.mUser2Deferred.put(userId, temp);
        return temp;
    }

    public void updateUidReadyForBootCompletedBroadcastLocked(int uid) {
        getDeferredPerUser(UserHandle.getUserId(uid)).updateUidReady(uid);
    }

    private BroadcastRecord dequeueDeferredBootCompletedBroadcast() {
        boolean isAllUidReady = this.mQueue.mService.mConstants.mDeferBootCompletedBroadcast == 0;
        BroadcastRecord next = null;
        int size = this.mUser2Deferred.size();
        for (int i = 0; i < size; i++) {
            next = this.mUser2Deferred.valueAt(i).dequeueDeferredBootCompletedBroadcast(isAllUidReady);
            if (next != null) {
                break;
            }
        }
        return next;
    }

    public BroadcastDispatcher(BroadcastQueue queue, BroadcastConstants constants, Handler handler, Object lock) {
        this.mQueue = queue;
        this.mConstants = constants;
        this.mHandler = handler;
        this.mLock = lock;
    }

    public void start() {
        AlarmManagerInternal alarmManagerInternal = (AlarmManagerInternal) LocalServices.getService(AlarmManagerInternal.class);
        this.mAlarm = alarmManagerInternal;
        alarmManagerInternal.registerInFlightListener(this.mAlarmListener);
    }

    public boolean isEmpty() {
        boolean z;
        synchronized (this.mLock) {
            z = isIdle() && getBootCompletedBroadcastsUidsSize("android.intent.action.LOCKED_BOOT_COMPLETED") == 0 && getBootCompletedBroadcastsUidsSize("android.intent.action.BOOT_COMPLETED") == 0;
        }
        return z;
    }

    public boolean isIdle() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mCurrentBroadcast == null && this.mOrderedBroadcasts.isEmpty() && isDeferralsListEmpty(this.mDeferredBroadcasts) && isDeferralsListEmpty(this.mAlarmBroadcasts);
        }
        return z;
    }

    private static int pendingInDeferralsList(ArrayList<Deferrals> list) {
        int pending = 0;
        int numEntries = list.size();
        for (int i = 0; i < numEntries; i++) {
            pending += list.get(i).size();
        }
        return pending;
    }

    private static boolean isDeferralsListEmpty(ArrayList<Deferrals> list) {
        return pendingInDeferralsList(list) == 0;
    }

    public String describeStateLocked() {
        StringBuilder sb = new StringBuilder(128);
        if (this.mCurrentBroadcast != null) {
            sb.append("1 in flight, ");
        }
        sb.append(this.mOrderedBroadcasts.size());
        sb.append(" ordered");
        int n = pendingInDeferralsList(this.mAlarmBroadcasts);
        if (n > 0) {
            sb.append(", ");
            sb.append(n);
            sb.append(" deferrals in alarm recipients");
        }
        int n2 = pendingInDeferralsList(this.mDeferredBroadcasts);
        if (n2 > 0) {
            sb.append(", ");
            sb.append(n2);
            sb.append(" deferred");
        }
        int n3 = getBootCompletedBroadcastsUidsSize("android.intent.action.LOCKED_BOOT_COMPLETED");
        if (n3 > 0) {
            sb.append(", ");
            sb.append(n3);
            sb.append(" deferred LOCKED_BOOT_COMPLETED/");
            sb.append(getBootCompletedBroadcastsReceiversSize("android.intent.action.LOCKED_BOOT_COMPLETED"));
            sb.append(" receivers");
        }
        int n4 = getBootCompletedBroadcastsUidsSize("android.intent.action.BOOT_COMPLETED");
        if (n4 > 0) {
            sb.append(", ");
            sb.append(n4);
            sb.append(" deferred BOOT_COMPLETED/");
            sb.append(getBootCompletedBroadcastsReceiversSize("android.intent.action.BOOT_COMPLETED"));
            sb.append(" receivers");
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        if (r.receivers == null || r.receivers.isEmpty()) {
            this.mOrderedBroadcasts.add(r);
        } else if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(r.intent.getAction())) {
            SparseArray<BroadcastRecord> deferred = r.splitDeferredBootCompletedBroadcastLocked(this.mQueue.mService.mInternal, this.mQueue.mService.mConstants.mDeferBootCompletedBroadcast);
            getDeferredPerUser(r.userId).enqueueBootCompletedBroadcasts("android.intent.action.LOCKED_BOOT_COMPLETED", deferred);
            if (!r.receivers.isEmpty()) {
                this.mOrderedBroadcasts.add(r);
            }
        } else if ("android.intent.action.BOOT_COMPLETED".equals(r.intent.getAction())) {
            SparseArray<BroadcastRecord> deferred2 = r.splitDeferredBootCompletedBroadcastLocked(this.mQueue.mService.mInternal, this.mQueue.mService.mConstants.mDeferBootCompletedBroadcast);
            getDeferredPerUser(r.userId).enqueueBootCompletedBroadcasts("android.intent.action.BOOT_COMPLETED", deferred2);
            if (!r.receivers.isEmpty()) {
                this.mOrderedBroadcasts.add(r);
            }
        } else {
            this.mOrderedBroadcasts.add(r);
        }
    }

    private int getBootCompletedBroadcastsUidsSize(String action) {
        int size = 0;
        int s = this.mUser2Deferred.size();
        for (int i = 0; i < s; i++) {
            size += this.mUser2Deferred.valueAt(i).getBootCompletedBroadcastsUidsSize(action);
        }
        return size;
    }

    private int getBootCompletedBroadcastsReceiversSize(String action) {
        int size = 0;
        int s = this.mUser2Deferred.size();
        for (int i = 0; i < s; i++) {
            size += this.mUser2Deferred.valueAt(i).getBootCompletedBroadcastsReceiversSize(action);
        }
        return size;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastRecord replaceBroadcastLocked(BroadcastRecord r, String typeForLogging) {
        BroadcastRecord old = replaceBroadcastLocked(this.mOrderedBroadcasts, r, typeForLogging);
        if (old == null) {
            old = replaceDeferredBroadcastLocked(this.mAlarmBroadcasts, r, typeForLogging);
        }
        if (old == null) {
            return replaceDeferredBroadcastLocked(this.mDeferredBroadcasts, r, typeForLogging);
        }
        return old;
    }

    private BroadcastRecord replaceDeferredBroadcastLocked(ArrayList<Deferrals> list, BroadcastRecord r, String typeForLogging) {
        int numEntries = list.size();
        for (int i = 0; i < numEntries; i++) {
            Deferrals d = list.get(i);
            BroadcastRecord old = replaceBroadcastLocked(d.broadcasts, r, typeForLogging);
            if (old != null) {
                return old;
            }
        }
        return null;
    }

    private BroadcastRecord replaceBroadcastLocked(ArrayList<BroadcastRecord> list, BroadcastRecord r, String typeForLogging) {
        Intent intent = r.intent;
        for (int i = list.size() - 1; i >= 0; i--) {
            BroadcastRecord old = list.get(i);
            if (old.userId == r.userId && intent.filterEquals(old.intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG, "***** Replacing " + typeForLogging + " [" + this.mQueue.mQueueName + "]: " + intent);
                }
                r.deferred = old.deferred;
                list.set(i, r);
                return old;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        BroadcastRecord broadcastRecord;
        boolean didSomething = cleanupBroadcastListDisabledReceiversLocked(this.mOrderedBroadcasts, packageName, filterByClasses, userId, doit);
        if (doit || !didSomething) {
            ArrayList<BroadcastRecord> lockedBootCompletedBroadcasts = new ArrayList<>();
            int usize = this.mUser2Deferred.size();
            for (int u = 0; u < usize; u++) {
                SparseArray<BroadcastRecord> brs = this.mUser2Deferred.valueAt(u).mDeferredLockedBootCompletedBroadcasts;
                int size = brs.size();
                for (int i = 0; i < size; i++) {
                    lockedBootCompletedBroadcasts.add(brs.valueAt(i));
                }
            }
            didSomething = cleanupBroadcastListDisabledReceiversLocked(lockedBootCompletedBroadcasts, packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            ArrayList<BroadcastRecord> bootCompletedBroadcasts = new ArrayList<>();
            int usize2 = this.mUser2Deferred.size();
            for (int u2 = 0; u2 < usize2; u2++) {
                SparseArray<BroadcastRecord> brs2 = this.mUser2Deferred.valueAt(u2).mDeferredBootCompletedBroadcasts;
                int size2 = brs2.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    bootCompletedBroadcasts.add(brs2.valueAt(i2));
                }
            }
            didSomething = cleanupBroadcastListDisabledReceiversLocked(bootCompletedBroadcasts, packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            didSomething |= cleanupDeferralsListDisabledReceiversLocked(this.mAlarmBroadcasts, packageName, filterByClasses, userId, doit);
        }
        if (doit || !didSomething) {
            didSomething |= cleanupDeferralsListDisabledReceiversLocked(this.mDeferredBroadcasts, packageName, filterByClasses, userId, doit);
        }
        if ((doit || !didSomething) && (broadcastRecord = this.mCurrentBroadcast) != null) {
            return didSomething | broadcastRecord.cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
        }
        return didSomething;
    }

    private boolean cleanupDeferralsListDisabledReceiversLocked(ArrayList<Deferrals> list, String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        boolean didSomething = false;
        Iterator<Deferrals> it = list.iterator();
        while (it.hasNext()) {
            Deferrals d = it.next();
            didSomething = cleanupBroadcastListDisabledReceiversLocked(d.broadcasts, packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething;
    }

    private boolean cleanupBroadcastListDisabledReceiversLocked(ArrayList<BroadcastRecord> list, String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        boolean didSomething = false;
        Iterator<BroadcastRecord> it = list.iterator();
        while (it.hasNext()) {
            BroadcastRecord br = it.next();
            didSomething |= br.cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        BroadcastRecord broadcastRecord = this.mCurrentBroadcast;
        if (broadcastRecord != null) {
            broadcastRecord.dumpDebug(proto, fieldId);
        }
        Iterator<Deferrals> it = this.mAlarmBroadcasts.iterator();
        while (it.hasNext()) {
            Deferrals d = it.next();
            d.dumpDebug(proto, fieldId);
        }
        Iterator<BroadcastRecord> it2 = this.mOrderedBroadcasts.iterator();
        while (it2.hasNext()) {
            BroadcastRecord br = it2.next();
            br.dumpDebug(proto, fieldId);
        }
        Iterator<Deferrals> it3 = this.mDeferredBroadcasts.iterator();
        while (it3.hasNext()) {
            Deferrals d2 = it3.next();
            d2.dumpDebug(proto, fieldId);
        }
        int size = this.mUser2Deferred.size();
        for (int i = 0; i < size; i++) {
            this.mUser2Deferred.valueAt(i).dumpDebug(proto, fieldId);
        }
    }

    public BroadcastRecord getActiveBroadcastLocked() {
        return this.mCurrentBroadcast;
    }

    public BroadcastRecord getNextBroadcastLocked(long now) {
        BroadcastRecord broadcastRecord = this.mCurrentBroadcast;
        if (broadcastRecord != null) {
            return broadcastRecord;
        }
        boolean someQueued = !this.mOrderedBroadcasts.isEmpty();
        BroadcastRecord next = null;
        if (0 == 0) {
            next = dequeueDeferredBootCompletedBroadcast();
        }
        if (next == null && !this.mAlarmBroadcasts.isEmpty()) {
            next = popLocked(this.mAlarmBroadcasts);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL && next != null) {
                Slog.i(TAG, "Next broadcast from alarm targets: " + next);
            }
        }
        if (next == null && !this.mDeferredBroadcasts.isEmpty()) {
            int i = 0;
            while (true) {
                if (i >= this.mDeferredBroadcasts.size()) {
                    break;
                }
                Deferrals d = this.mDeferredBroadcasts.get(i);
                if (now < d.deferUntil && someQueued) {
                    break;
                } else if (d.broadcasts.size() > 0) {
                    BroadcastRecord next2 = d.broadcasts.remove(0);
                    next = next2;
                    this.mDeferredBroadcasts.remove(i);
                    d.deferredBy = calculateDeferral(d.deferredBy);
                    d.deferUntil += d.deferredBy;
                    insertLocked(this.mDeferredBroadcasts, d);
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                        Slog.i(TAG, "Next broadcast from deferrals " + next + ", deferUntil now " + d.deferUntil);
                    }
                } else {
                    i++;
                }
            }
        }
        if (next == null && someQueued) {
            BroadcastRecord next3 = this.mOrderedBroadcasts.remove(0);
            next = next3;
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                Slog.i(TAG, "Next broadcast from main queue: " + next);
            }
        }
        this.mCurrentBroadcast = next;
        return next;
    }

    public void retireBroadcastLocked(BroadcastRecord r) {
        if (r != this.mCurrentBroadcast) {
            Slog.wtf(TAG, "Retiring broadcast " + r + " doesn't match current outgoing " + this.mCurrentBroadcast);
        }
        this.mCurrentBroadcast = null;
    }

    public boolean isDeferringLocked(int uid) {
        Deferrals d = findUidLocked(uid);
        if (d == null || !d.broadcasts.isEmpty() || SystemClock.uptimeMillis() < d.deferUntil) {
            return d != null;
        }
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
            Slog.i(TAG, "No longer deferring broadcasts to uid " + d.uid);
        }
        removeDeferral(d);
        return false;
    }

    public void startDeferring(int uid) {
        synchronized (this.mLock) {
            Deferrals d = findUidLocked(uid);
            if (d == null) {
                long now = SystemClock.uptimeMillis();
                Deferrals d2 = new Deferrals(uid, now, this.mConstants.DEFERRAL, this.mAlarmUids.get(uid, 0));
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    Slog.i(TAG, "Now deferring broadcasts to " + uid + " until " + d2.deferUntil);
                }
                if (d2.alarmCount == 0) {
                    insertLocked(this.mDeferredBroadcasts, d2);
                    scheduleDeferralCheckLocked(true);
                } else {
                    this.mAlarmBroadcasts.add(d2);
                }
            } else {
                d.deferredBy = this.mConstants.DEFERRAL;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    Slog.i(TAG, "Uid " + uid + " slow again, deferral interval reset to " + d.deferredBy);
                }
            }
        }
    }

    public void addDeferredBroadcast(int uid, BroadcastRecord br) {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
            Slog.i(TAG, "Enqueuing deferred broadcast " + br);
        }
        synchronized (this.mLock) {
            Deferrals d = findUidLocked(uid);
            if (d == null) {
                Slog.wtf(TAG, "Adding deferred broadcast but not tracking " + uid);
            } else if (br == null) {
                Slog.wtf(TAG, "Deferring null broadcast to " + uid);
            } else {
                br.deferred = true;
                d.add(br);
            }
        }
    }

    public void scheduleDeferralCheckLocked(boolean force) {
        if ((force || !this.mRecheckScheduled) && !this.mDeferredBroadcasts.isEmpty()) {
            Deferrals d = this.mDeferredBroadcasts.get(0);
            if (!d.broadcasts.isEmpty()) {
                this.mHandler.removeCallbacks(this.mScheduleRunnable);
                this.mHandler.postAtTime(this.mScheduleRunnable, d.deferUntil);
                this.mRecheckScheduled = true;
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL) {
                    Slog.i(TAG, "Scheduling deferred broadcast recheck at " + d.deferUntil);
                }
            }
        }
    }

    public void cancelDeferralsLocked() {
        zeroDeferralTimes(this.mAlarmBroadcasts);
        zeroDeferralTimes(this.mDeferredBroadcasts);
    }

    private static void zeroDeferralTimes(ArrayList<Deferrals> list) {
        int num = list.size();
        for (int i = 0; i < num; i++) {
            Deferrals d = list.get(i);
            d.deferredBy = 0L;
            d.deferUntil = 0L;
        }
    }

    private Deferrals findUidLocked(int uid) {
        Deferrals d = findUidLocked(uid, this.mDeferredBroadcasts);
        if (d == null) {
            return findUidLocked(uid, this.mAlarmBroadcasts);
        }
        return d;
    }

    private boolean removeDeferral(Deferrals d) {
        boolean didRemove = this.mDeferredBroadcasts.remove(d);
        if (!didRemove) {
            return this.mAlarmBroadcasts.remove(d);
        }
        return didRemove;
    }

    private static Deferrals findUidLocked(int uid, ArrayList<Deferrals> list) {
        int numElements = list.size();
        for (int i = 0; i < numElements; i++) {
            Deferrals d = list.get(i);
            if (uid == d.uid) {
                return d;
            }
        }
        return null;
    }

    private static BroadcastRecord popLocked(ArrayList<Deferrals> list) {
        Deferrals d = list.get(0);
        if (d.broadcasts.isEmpty()) {
            return null;
        }
        return d.broadcasts.remove(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void insertLocked(ArrayList<Deferrals> list, Deferrals d) {
        int numElements = list.size();
        int i = 0;
        while (i < numElements && d.deferUntil >= list.get(i).deferUntil) {
            i++;
        }
        list.add(i, d);
    }

    private long calculateDeferral(long previous) {
        return Math.max(this.mConstants.DEFERRAL_FLOOR, ((float) previous) * this.mConstants.DEFERRAL_DECAY_FACTOR);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpLocked(PrintWriter pw, String dumpPackage, String queueName, SimpleDateFormat sdf) {
        Dumper dumper = new Dumper(pw, queueName, dumpPackage, sdf);
        dumper.setHeading("Currently in flight");
        dumper.setLabel("In-Flight Ordered Broadcast");
        if (this.mCurrentBroadcast != null) {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d(TAG, "dumpLocked mCurrentBroadcast start");
            }
            dumper.dump(this.mCurrentBroadcast);
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d(TAG, "dumpLocked mCurrentBroadcast end");
            }
        } else {
            pw.println("  (null)");
        }
        dumper.setHeading("Active ordered broadcasts");
        dumper.setLabel("Active Ordered Broadcast");
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mAlarmBroadcasts start mAlarmBroadcasts.size = " + this.mAlarmBroadcasts.size());
        }
        Iterator<Deferrals> it = this.mAlarmBroadcasts.iterator();
        while (it.hasNext()) {
            Deferrals d = it.next();
            d.dumpLocked(dumper);
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mAlarmBroadcasts end");
        }
        boolean printed = false | dumper.didPrint();
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mOrderedBroadcasts start mOrderedBroadcasts.size = " + this.mOrderedBroadcasts.size());
        }
        Iterator<BroadcastRecord> it2 = this.mOrderedBroadcasts.iterator();
        while (it2.hasNext()) {
            BroadcastRecord br = it2.next();
            dumper.dump(br);
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mOrderedBroadcasts end");
        }
        boolean printed2 = printed | dumper.didPrint();
        dumper.setHeading("Deferred ordered broadcasts");
        dumper.setLabel("Deferred Ordered Broadcast");
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mDeferredBroadcasts start mDeferredBroadcasts.size = " + this.mDeferredBroadcasts.size());
        }
        Iterator<Deferrals> it3 = this.mDeferredBroadcasts.iterator();
        while (it3.hasNext()) {
            Deferrals d2 = it3.next();
            d2.dumpLocked(dumper);
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d(TAG, "dumpLocked mDeferredBroadcasts end");
        }
        boolean printed3 = printed2 | dumper.didPrint();
        dumper.setHeading("Deferred LOCKED_BOOT_COMPLETED broadcasts");
        dumper.setLabel("Deferred LOCKED_BOOT_COMPLETED Broadcast");
        int size = this.mUser2Deferred.size();
        for (int i = 0; i < size; i++) {
            this.mUser2Deferred.valueAt(i).dump(dumper, "android.intent.action.LOCKED_BOOT_COMPLETED");
        }
        boolean printed4 = printed3 | dumper.didPrint();
        dumper.setHeading("Deferred BOOT_COMPLETED broadcasts");
        dumper.setLabel("Deferred BOOT_COMPLETED Broadcast");
        int size2 = this.mUser2Deferred.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mUser2Deferred.valueAt(i2).dump(dumper, "android.intent.action.BOOT_COMPLETED");
        }
        return printed4 | dumper.didPrint();
    }
}
