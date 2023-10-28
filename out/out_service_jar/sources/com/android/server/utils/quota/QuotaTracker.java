package com.android.server.utils.quota;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.BackgroundThread;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.utils.AlarmQueue;
import com.android.server.utils.quota.QuotaTracker;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class QuotaTracker {
    private static final String ALARM_TAG_QUOTA_CHECK;
    private static final boolean DEBUG = false;
    static final long MAX_WINDOW_SIZE_MS = 2592000000L;
    static final long MIN_WINDOW_SIZE_MS = 20000;
    private static final String TAG;
    private final AlarmManager mAlarmManager;
    private final BroadcastReceiver mBroadcastReceiver;
    final Categorizer mCategorizer;
    protected final Context mContext;
    private final InQuotaAlarmQueue mInQuotaAlarmQueue;
    protected final Injector mInjector;
    private boolean mIsQuotaFree;
    final Object mLock = new Object();
    private final ArraySet<QuotaChangeListener> mQuotaChangeListeners = new ArraySet<>();
    private final SparseArrayMap<String, Boolean> mFreeQuota = new SparseArrayMap<>();
    private boolean mIsEnabled = true;

    abstract void dropEverythingLocked();

    abstract Handler getHandler();

    abstract long getInQuotaTimeElapsedLocked(int i, String str, String str2);

    abstract void handleRemovedAppLocked(int i, String str);

    abstract void handleRemovedUserLocked(int i);

    abstract boolean isWithinQuotaLocked(int i, String str, String str2);

    abstract void maybeUpdateAllQuotaStatusLocked();

    abstract void maybeUpdateQuotaStatus(int i, String str, String str2);

    abstract void onQuotaFreeChangedLocked(int i, String str, boolean z);

    abstract void onQuotaFreeChangedLocked(boolean z);

    static {
        String simpleName = QuotaTracker.class.getSimpleName();
        TAG = simpleName;
        ALARM_TAG_QUOTA_CHECK = "*" + simpleName + ".quota_check*";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        /* JADX INFO: Access modifiers changed from: package-private */
        public long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        boolean isAlarmManagerReady() {
            return ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).isBootCompleted();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public QuotaTracker(Context context, Categorizer categorizer, Injector injector) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.utils.quota.QuotaTracker.1
            private String getPackageName(Intent intent) {
                Uri uri = intent.getData();
                if (uri != null) {
                    return uri.getSchemeSpecificPart();
                }
                return null;
            }

            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean z;
                if (intent == null || intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    return;
                }
                String action = intent.getAction();
                if (action == null) {
                    Slog.e(QuotaTracker.TAG, "Received intent with null action");
                    return;
                }
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1580442797:
                        if (action.equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                        synchronized (QuotaTracker.this.mLock) {
                            QuotaTracker.this.onAppRemovedLocked(UserHandle.getUserId(uid), getPackageName(intent));
                        }
                        return;
                    case true:
                        int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        synchronized (QuotaTracker.this.mLock) {
                            QuotaTracker.this.onUserRemovedLocked(userId);
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mCategorizer = categorizer;
        this.mContext = context;
        this.mInjector = injector;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mInQuotaAlarmQueue = new InQuotaAlarmQueue(context, FgThread.getHandler().getLooper());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, BackgroundThread.getHandler());
        IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, userFilter, null, BackgroundThread.getHandler());
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mInQuotaAlarmQueue.removeAllAlarms();
            this.mFreeQuota.clear();
            dropEverythingLocked();
        }
    }

    public boolean isWithinQuota(int userId, String packageName, String tag) {
        boolean isWithinQuotaLocked;
        synchronized (this.mLock) {
            isWithinQuotaLocked = isWithinQuotaLocked(userId, packageName, tag);
        }
        return isWithinQuotaLocked;
    }

    public void setQuotaFree(int userId, String packageName, boolean isFree) {
        synchronized (this.mLock) {
            boolean wasFree = ((Boolean) this.mFreeQuota.getOrDefault(userId, packageName, Boolean.FALSE)).booleanValue();
            if (wasFree != isFree) {
                this.mFreeQuota.add(userId, packageName, Boolean.valueOf(isFree));
                onQuotaFreeChangedLocked(userId, packageName, isFree);
            }
        }
    }

    public void setQuotaFree(boolean isFree) {
        synchronized (this.mLock) {
            if (this.mIsQuotaFree == isFree) {
                return;
            }
            this.mIsQuotaFree = isFree;
            if (this.mIsEnabled) {
                onQuotaFreeChangedLocked(isFree);
                scheduleQuotaCheck();
            }
        }
    }

    public void registerQuotaChangeListener(QuotaChangeListener listener) {
        synchronized (this.mLock) {
            if (this.mQuotaChangeListeners.add(listener) && this.mQuotaChangeListeners.size() == 1) {
                scheduleQuotaCheck();
            }
        }
    }

    public void unregisterQuotaChangeListener(QuotaChangeListener listener) {
        synchronized (this.mLock) {
            this.mQuotaChangeListeners.remove(listener);
        }
    }

    public void setEnabled(boolean enable) {
        synchronized (this.mLock) {
            if (this.mIsEnabled == enable) {
                return;
            }
            this.mIsEnabled = enable;
            if (!enable) {
                clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabledLocked() {
        return this.mIsEnabled;
    }

    boolean isQuotaFreeLocked() {
        return this.mIsQuotaFree;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isQuotaFreeLocked(int userId, String packageName) {
        return this.mIsQuotaFree || ((Boolean) this.mFreeQuota.getOrDefault(userId, packageName, Boolean.FALSE)).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIndividualQuotaFreeLocked(int userId, String packageName) {
        return ((Boolean) this.mFreeQuota.getOrDefault(userId, packageName, Boolean.FALSE)).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAlarm(final int type, final long triggerAtMillis, final String tag, final AlarmManager.OnAlarmListener listener) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.utils.quota.QuotaTracker$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                QuotaTracker.this.m7452x93cebe36(type, triggerAtMillis, tag, listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAlarm$0$com-android-server-utils-quota-QuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7452x93cebe36(int type, long triggerAtMillis, String tag, AlarmManager.OnAlarmListener listener) {
        if (this.mInjector.isAlarmManagerReady()) {
            this.mAlarmManager.set(type, triggerAtMillis, tag, listener, getHandler());
        } else {
            Slog.w(TAG, "Alarm not scheduled because boot isn't completed");
        }
    }

    void cancelAlarm(final AlarmManager.OnAlarmListener listener) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.utils.quota.QuotaTracker$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                QuotaTracker.this.m7450lambda$cancelAlarm$1$comandroidserverutilsquotaQuotaTracker(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelAlarm$1$com-android-server-utils-quota-QuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7450lambda$cancelAlarm$1$comandroidserverutilsquotaQuotaTracker(AlarmManager.OnAlarmListener listener) {
        if (this.mInjector.isAlarmManagerReady()) {
            this.mAlarmManager.cancel(listener);
        } else {
            Slog.w(TAG, "Alarm not cancelled because boot isn't completed");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleQuotaCheck() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.utils.quota.QuotaTracker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                QuotaTracker.this.m7453x6ccba7f();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleQuotaCheck$2$com-android-server-utils-quota-QuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7453x6ccba7f() {
        synchronized (this.mLock) {
            if (this.mQuotaChangeListeners.size() > 0) {
                maybeUpdateAllQuotaStatusLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppRemovedLocked(int userId, String packageName) {
        if (packageName == null) {
            Slog.wtf(TAG, "Told app removed but given null package name.");
            return;
        }
        this.mInQuotaAlarmQueue.removeAlarms(userId, packageName);
        this.mFreeQuota.delete(userId, packageName);
        handleRemovedAppLocked(userId, packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserRemovedLocked(int userId) {
        this.mInQuotaAlarmQueue.removeAlarmsForUserId(userId);
        this.mFreeQuota.delete(userId);
        handleRemovedUserLocked(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postQuotaStatusChanged(final int userId, final String packageName, final String tag) {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.utils.quota.QuotaTracker$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                QuotaTracker.this.m7451x621cc963(userId, packageName, tag);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postQuotaStatusChanged$3$com-android-server-utils-quota-QuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7451x621cc963(int userId, String packageName, String tag) {
        QuotaChangeListener[] listeners;
        synchronized (this.mLock) {
            ArraySet<QuotaChangeListener> arraySet = this.mQuotaChangeListeners;
            listeners = (QuotaChangeListener[]) arraySet.toArray(new QuotaChangeListener[arraySet.size()]);
        }
        for (QuotaChangeListener listener : listeners) {
            listener.onQuotaStateChanged(userId, packageName, tag);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeScheduleStartAlarmLocked(int userId, String packageName, String tag) {
        if (this.mQuotaChangeListeners.size() == 0) {
            return;
        }
        Uptc.string(userId, packageName, tag);
        if (isWithinQuota(userId, packageName, tag)) {
            this.mInQuotaAlarmQueue.removeAlarmForKey(new Uptc(userId, packageName, tag));
            maybeUpdateQuotaStatus(userId, packageName, tag);
            return;
        }
        this.mInQuotaAlarmQueue.addAlarm(new Uptc(userId, packageName, tag), getInQuotaTimeElapsedLocked(userId, packageName, tag));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelScheduledStartAlarmLocked(int userId, String packageName, String tag) {
        this.mInQuotaAlarmQueue.removeAlarmForKey(new Uptc(userId, packageName, tag));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class InQuotaAlarmQueue extends AlarmQueue<Uptc> {
        private InQuotaAlarmQueue(Context context, Looper looper) {
            super(context, looper, QuotaTracker.ALARM_TAG_QUOTA_CHECK, "In quota", false, 0L);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.utils.AlarmQueue
        public boolean isForUser(Uptc uptc, int userId) {
            return userId == uptc.userId;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$removeAlarms$0(int userId, String packageName, Uptc uptc) {
            return userId == uptc.userId && packageName.equals(uptc.packageName);
        }

        void removeAlarms(final int userId, final String packageName) {
            removeAlarmsIf(new Predicate() { // from class: com.android.server.utils.quota.QuotaTracker$InQuotaAlarmQueue$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return QuotaTracker.InQuotaAlarmQueue.lambda$removeAlarms$0(userId, packageName, (Uptc) obj);
                }
            });
        }

        @Override // com.android.server.utils.AlarmQueue
        protected void processExpiredAlarms(ArraySet<Uptc> expired) {
            for (int i = 0; i < expired.size(); i++) {
                final Uptc uptc = expired.valueAt(i);
                QuotaTracker.this.getHandler().post(new Runnable() { // from class: com.android.server.utils.quota.QuotaTracker$InQuotaAlarmQueue$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        QuotaTracker.InQuotaAlarmQueue.this.m7454xd506b99e(uptc);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$processExpiredAlarms$1$com-android-server-utils-quota-QuotaTracker$InQuotaAlarmQueue  reason: not valid java name */
        public /* synthetic */ void m7454xd506b99e(Uptc uptc) {
            QuotaTracker.this.maybeUpdateQuotaStatus(uptc.userId, uptc.packageName, uptc.tag);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("QuotaTracker:");
        pw.increaseIndent();
        synchronized (this.mLock) {
            pw.println("Is enabled: " + this.mIsEnabled);
            pw.println("Is global quota free: " + this.mIsQuotaFree);
            pw.println("Current elapsed time: " + this.mInjector.getElapsedRealtime());
            pw.println();
            pw.println();
            this.mInQuotaAlarmQueue.dump(pw);
            pw.println();
            pw.println("Per-app free quota:");
            pw.increaseIndent();
            for (int u = 0; u < this.mFreeQuota.numMaps(); u++) {
                int userId = this.mFreeQuota.keyAt(u);
                for (int p = 0; p < this.mFreeQuota.numElementsForKey(userId); p++) {
                    String pkgName = (String) this.mFreeQuota.keyAt(u, p);
                    pw.print(Uptc.string(userId, pkgName, null));
                    pw.print(": ");
                    pw.println(this.mFreeQuota.get(userId, pkgName));
                }
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    public void dump(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        synchronized (this.mLock) {
            proto.write(1133871366145L, this.mIsEnabled);
            proto.write(1133871366146L, this.mIsQuotaFree);
            proto.write(1112396529667L, this.mInjector.getElapsedRealtime());
        }
        proto.end(token);
    }
}
