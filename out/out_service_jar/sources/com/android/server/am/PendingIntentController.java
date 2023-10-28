package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.PendingIntent;
import android.app.PendingIntentStats;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.AlarmManagerInternal;
import com.android.server.LocalServices;
import com.android.server.am.PendingIntentRecord;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.SafeActivityOptions;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class PendingIntentController {
    private static final int RECENT_N = 10;
    private static final String TAG = "ActivityManager";
    private static final String TAG_MU = "ActivityManager_MU";
    ActivityManagerInternal mAmInternal;
    private final ActivityManagerConstants mConstants;
    final Handler mH;
    final UserController mUserController;
    final Object mLock = new Object();
    final HashMap<PendingIntentRecord.Key, WeakReference<PendingIntentRecord>> mIntentSenderRecords = new HashMap<>();
    private final SparseIntArray mIntentsPerUid = new SparseIntArray();
    private final SparseArray<RingBuffer<String>> mRecentIntentsPerUid = new SparseArray<>();
    final ActivityTaskManagerInternal mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);

    public PendingIntentController(Looper looper, UserController userController, ActivityManagerConstants constants) {
        this.mH = new Handler(looper);
        this.mUserController = userController;
        this.mConstants = constants;
    }

    public void onActivityManagerInternalAdded() {
        synchronized (this.mLock) {
            this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
    }

    public PendingIntentRecord getIntentSender(int type, String packageName, String featureId, int callingUid, int userId, IBinder token, String resultWho, int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bOptions) {
        Object obj;
        Intent intent;
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    if (ActivityManagerDebugConfig.DEBUG_MU) {
                        try {
                            Slog.v(TAG_MU, "getIntentSender(): uid=" + callingUid);
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                            throw th;
                        }
                    }
                    if (intents != null) {
                        for (Intent intent2 : intents) {
                            intent2.setDefusable(true);
                        }
                    }
                    Bundle.setDefusable(bOptions, true);
                    boolean noCreate = (flags & 536870912) != 0;
                    boolean cancelCurrent = (flags & 268435456) != 0;
                    boolean updateCurrent = (flags & 134217728) != 0;
                    try {
                        obj = obj2;
                        try {
                            PendingIntentRecord.Key key = new PendingIntentRecord.Key(type, packageName, featureId, token, resultWho, requestCode, intents, resolvedTypes, flags & (-939524097), SafeActivityOptions.fromBundle(bOptions), userId);
                            WeakReference<PendingIntentRecord> ref = this.mIntentSenderRecords.get(key);
                            PendingIntentRecord rec = ref != null ? ref.get() : null;
                            if (rec != null) {
                                if (cancelCurrent) {
                                    makeIntentSenderCanceled(rec);
                                    this.mIntentSenderRecords.remove(key);
                                    decrementUidStatLocked(rec);
                                } else {
                                    if (updateCurrent) {
                                        if (rec.key.requestIntent != null) {
                                            Intent intent3 = rec.key.requestIntent;
                                            if (intents != null) {
                                                intent = intents[intents.length - 1];
                                            } else {
                                                intent = null;
                                            }
                                            intent3.replaceExtras(intent);
                                        }
                                        if (intents != null) {
                                            intents[intents.length - 1] = rec.key.requestIntent;
                                            rec.key.allIntents = intents;
                                            rec.key.allResolvedTypes = resolvedTypes;
                                        } else {
                                            rec.key.allIntents = null;
                                            rec.key.allResolvedTypes = null;
                                        }
                                    }
                                    return rec;
                                }
                            }
                            if (!noCreate) {
                                PendingIntentRecord rec2 = new PendingIntentRecord(this, key, callingUid);
                                this.mIntentSenderRecords.put(key, rec2.ref);
                                incrementUidStatLocked(rec2);
                                return rec2;
                            }
                            return rec;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        obj = obj2;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
                obj = obj2;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:109:0x0063 A[Catch: all -> 0x008a, TryCatch #0 {, blocks: (B:76:0x0004, B:79:0x000d, B:81:0x000f, B:82:0x0019, B:84:0x001f, B:86:0x0027, B:87:0x002b, B:89:0x0033, B:91:0x0039, B:107:0x0061, B:109:0x0063, B:111:0x0073, B:94:0x0040, B:99:0x004c, B:102:0x0053, B:113:0x0088), top: B:118:0x0004 }] */
    /* JADX WARN: Removed duplicated region for block: B:129:0x0060 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean removePendingIntentsForPackage(String packageName, int userId, int appId, boolean doIt) {
        boolean didSomething = false;
        synchronized (this.mLock) {
            if (this.mIntentSenderRecords.size() <= 0) {
                return false;
            }
            Iterator<WeakReference<PendingIntentRecord>> it = this.mIntentSenderRecords.values().iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> wpir = it.next();
                if (wpir == null) {
                    it.remove();
                } else {
                    PendingIntentRecord pir = wpir.get();
                    if (pir == null) {
                        it.remove();
                    } else if (packageName == null) {
                        if (pir.key.userId == userId) {
                            if (doIt) {
                                return true;
                            }
                            didSomething = true;
                            it.remove();
                            makeIntentSenderCanceled(pir);
                            decrementUidStatLocked(pir);
                            if (pir.key.activity != null) {
                                Message m = PooledLambda.obtainMessage(new PendingIntentController$$ExternalSyntheticLambda0(), this, pir.key.activity, pir.ref);
                                this.mH.sendMessage(m);
                            }
                        }
                    } else if (UserHandle.getAppId(pir.uid) == appId && (userId == -1 || pir.key.userId == userId)) {
                        if (pir.key.packageName.equals(packageName)) {
                            if (doIt) {
                            }
                        }
                    }
                }
            }
            return didSomething;
        }
    }

    public void cancelIntentSender(IIntentSender sender) {
        if (!(sender instanceof PendingIntentRecord)) {
            return;
        }
        synchronized (this.mLock) {
            PendingIntentRecord rec = (PendingIntentRecord) sender;
            try {
                int uid = AppGlobals.getPackageManager().getPackageUid(rec.key.packageName, 268435456L, UserHandle.getCallingUserId());
                if (!UserHandle.isSameApp(uid, Binder.getCallingUid())) {
                    String msg = "Permission Denial: cancelIntentSender() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " is not allowed to cancel package " + rec.key.packageName;
                    Slog.w(TAG, msg);
                    throw new SecurityException(msg);
                }
                cancelIntentSender(rec, true);
            } catch (RemoteException e) {
                throw new SecurityException(e);
            }
        }
    }

    public void cancelIntentSender(PendingIntentRecord rec, boolean cleanActivity) {
        synchronized (this.mLock) {
            makeIntentSenderCanceled(rec);
            this.mIntentSenderRecords.remove(rec.key);
            decrementUidStatLocked(rec);
            if (cleanActivity && rec.key.activity != null) {
                Message m = PooledLambda.obtainMessage(new PendingIntentController$$ExternalSyntheticLambda0(), this, rec.key.activity, rec.ref);
                this.mH.sendMessage(m);
            }
        }
    }

    public boolean registerIntentSenderCancelListener(IIntentSender sender, IResultReceiver receiver) {
        if (!(sender instanceof PendingIntentRecord)) {
            Slog.w(TAG, "registerIntentSenderCancelListener called on non-PendingIntentRecord");
            return true;
        }
        synchronized (this.mLock) {
            PendingIntentRecord pendingIntent = (PendingIntentRecord) sender;
            boolean isCancelled = pendingIntent.canceled;
            if (!isCancelled) {
                pendingIntent.registerCancelListenerLocked(receiver);
                return true;
            }
            return false;
        }
    }

    public void unregisterIntentSenderCancelListener(IIntentSender sender, IResultReceiver receiver) {
        if (!(sender instanceof PendingIntentRecord)) {
            return;
        }
        synchronized (this.mLock) {
            ((PendingIntentRecord) sender).unregisterCancelListenerLocked(receiver);
        }
    }

    public void setPendingIntentAllowlistDuration(IIntentSender target, IBinder allowlistToken, long duration, int type, int reasonCode, String reason) {
        if (!(target instanceof PendingIntentRecord)) {
            Slog.w(TAG, "markAsSentFromNotification(): not a PendingIntentRecord: " + target);
            return;
        }
        synchronized (this.mLock) {
            ((PendingIntentRecord) target).setAllowlistDurationLocked(allowlistToken, duration, type, reasonCode, reason);
        }
    }

    public int getPendingIntentFlags(IIntentSender target) {
        int i;
        if (!(target instanceof PendingIntentRecord)) {
            Slog.w(TAG, "markAsSentFromNotification(): not a PendingIntentRecord: " + target);
            return 0;
        }
        synchronized (this.mLock) {
            i = ((PendingIntentRecord) target).key.flags;
        }
        return i;
    }

    private void makeIntentSenderCanceled(PendingIntentRecord rec) {
        rec.canceled = true;
        RemoteCallbackList<IResultReceiver> callbacks = rec.detachCancelListenersLocked();
        if (callbacks != null) {
            Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.am.PendingIntentController$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((PendingIntentController) obj).handlePendingIntentCancelled((RemoteCallbackList) obj2);
                }
            }, this, callbacks);
            this.mH.sendMessage(m);
        }
        AlarmManagerInternal ami = (AlarmManagerInternal) LocalServices.getService(AlarmManagerInternal.class);
        ami.remove(new PendingIntent(rec));
    }

    public void handlePendingIntentCancelled(RemoteCallbackList<IResultReceiver> callbacks) {
        int N = callbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                callbacks.getBroadcastItem(i).send(0, (Bundle) null);
            } catch (RemoteException e) {
            }
        }
        callbacks.finishBroadcast();
        callbacks.kill();
    }

    public void clearPendingResultForActivity(IBinder activityToken, WeakReference<PendingIntentRecord> pir) {
        this.mAtmInternal.clearPendingResultForActivity(activityToken, pir);
    }

    public void dumpPendingIntents(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        synchronized (this.mLock) {
            boolean printed = false;
            pw.println("ACTIVITY MANAGER PENDING INTENTS (dumpsys activity intents)");
            if (this.mIntentSenderRecords.size() > 0) {
                ArrayMap<String, ArrayList<PendingIntentRecord>> byPackage = new ArrayMap<>();
                ArrayList<WeakReference<PendingIntentRecord>> weakRefs = new ArrayList<>();
                Iterator<WeakReference<PendingIntentRecord>> it = this.mIntentSenderRecords.values().iterator();
                while (it.hasNext()) {
                    WeakReference<PendingIntentRecord> ref = it.next();
                    PendingIntentRecord rec = ref != null ? ref.get() : null;
                    if (rec == null) {
                        weakRefs.add(ref);
                    } else if (dumpPackage == null || dumpPackage.equals(rec.key.packageName)) {
                        ArrayList<PendingIntentRecord> list = byPackage.get(rec.key.packageName);
                        if (list == null) {
                            list = new ArrayList<>();
                            byPackage.put(rec.key.packageName, list);
                        }
                        list.add(rec);
                    }
                }
                for (int i = 0; i < byPackage.size(); i++) {
                    ArrayList<PendingIntentRecord> intents = byPackage.valueAt(i);
                    printed = true;
                    pw.print("  * ");
                    pw.print(byPackage.keyAt(i));
                    pw.print(": ");
                    pw.print(intents.size());
                    pw.println(" items");
                    for (int j = 0; j < intents.size(); j++) {
                        pw.print("    #");
                        pw.print(j);
                        pw.print(": ");
                        pw.println(intents.get(j));
                        if (dumpAll) {
                            intents.get(j).dump(pw, "      ");
                        }
                    }
                }
                int i2 = weakRefs.size();
                if (i2 > 0) {
                    printed = true;
                    pw.println("  * WEAK REFS:");
                    for (int i3 = 0; i3 < weakRefs.size(); i3++) {
                        pw.print("    #");
                        pw.print(i3);
                        pw.print(": ");
                        pw.println(weakRefs.get(i3));
                    }
                }
            }
            int sizeOfIntentsPerUid = this.mIntentsPerUid.size();
            if (sizeOfIntentsPerUid > 0) {
                for (int i4 = 0; i4 < sizeOfIntentsPerUid; i4++) {
                    pw.print("  * UID: ");
                    pw.print(this.mIntentsPerUid.keyAt(i4));
                    pw.print(" total: ");
                    pw.println(this.mIntentsPerUid.valueAt(i4));
                }
            }
            if (!printed) {
                pw.println("  (nothing)");
            }
        }
    }

    public List<PendingIntentStats> dumpPendingIntentStatsForStatsd() {
        List<PendingIntentStats> pendingIntentStats = new ArrayList<>();
        synchronized (this.mLock) {
            if (this.mIntentSenderRecords.size() > 0) {
                SparseIntArray countsByUid = new SparseIntArray();
                SparseIntArray bundleSizesByUid = new SparseIntArray();
                for (WeakReference<PendingIntentRecord> reference : this.mIntentSenderRecords.values()) {
                    if (reference != null && reference.get() != null) {
                        PendingIntentRecord record = reference.get();
                        int index = countsByUid.indexOfKey(record.uid);
                        if (index < 0) {
                            countsByUid.put(record.uid, 1);
                            bundleSizesByUid.put(record.uid, record.key.requestIntent.getExtrasTotalSize());
                        } else {
                            countsByUid.put(record.uid, countsByUid.valueAt(index) + 1);
                            bundleSizesByUid.put(record.uid, bundleSizesByUid.valueAt(index) + record.key.requestIntent.getExtrasTotalSize());
                        }
                    }
                }
                int size = countsByUid.size();
                for (int i = 0; i < size; i++) {
                    pendingIntentStats.add(new PendingIntentStats(countsByUid.keyAt(i), countsByUid.valueAt(i), bundleSizesByUid.valueAt(i) / 1024));
                }
            }
        }
        return pendingIntentStats;
    }

    void incrementUidStatLocked(PendingIntentRecord pir) {
        int uid = pir.uid;
        int idx = this.mIntentsPerUid.indexOfKey(uid);
        int newCount = 1;
        if (idx < 0) {
            this.mIntentsPerUid.put(uid, 1);
        } else {
            newCount = this.mIntentsPerUid.valueAt(idx) + 1;
            this.mIntentsPerUid.setValueAt(idx, newCount);
        }
        int lowBound = (this.mConstants.PENDINGINTENT_WARNING_THRESHOLD - 10) + 1;
        RingBuffer<String> recentHistory = null;
        if (newCount == lowBound) {
            recentHistory = new RingBuffer<>(String.class, 10);
            this.mRecentIntentsPerUid.put(uid, recentHistory);
        } else if (newCount > lowBound && newCount <= this.mConstants.PENDINGINTENT_WARNING_THRESHOLD) {
            recentHistory = this.mRecentIntentsPerUid.get(uid);
        }
        if (recentHistory == null) {
            return;
        }
        recentHistory.append(pir.key.toString());
        if (newCount == this.mConstants.PENDINGINTENT_WARNING_THRESHOLD) {
            Slog.wtf(TAG, "Too many PendingIntent created for uid " + uid + ", recent 10: " + Arrays.toString(recentHistory.toArray()));
            this.mRecentIntentsPerUid.remove(uid);
        }
    }

    public void decrementUidStatLocked(PendingIntentRecord pir) {
        int uid = pir.uid;
        int idx = this.mIntentsPerUid.indexOfKey(uid);
        if (idx >= 0) {
            int newCount = this.mIntentsPerUid.valueAt(idx) - 1;
            if (newCount == this.mConstants.PENDINGINTENT_WARNING_THRESHOLD - 10) {
                this.mRecentIntentsPerUid.delete(uid);
            }
            if (newCount == 0) {
                this.mIntentsPerUid.removeAt(idx);
            } else {
                this.mIntentsPerUid.setValueAt(idx, newCount);
            }
        }
    }
}
