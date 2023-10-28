package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import android.permission.PermissionControllerManager;
import android.provider.DeviceConfig;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.pm.permission.OneTimePermissionUserManager;
/* loaded from: classes2.dex */
public class OneTimePermissionUserManager {
    private static final boolean DEBUG = false;
    private static final long DEFAULT_KILLED_DELAY_MILLIS = 5000;
    private static final String LOG_TAG = OneTimePermissionUserManager.class.getSimpleName();
    public static final String PROPERTY_KILLED_DELAY_CONFIG_KEY = "one_time_permissions_killed_delay_millis";
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private final Handler mHandler;
    private final PermissionControllerManager mPermissionControllerManager;
    private final Object mLock = new Object();
    private final BroadcastReceiver mUninstallListener = new BroadcastReceiver() { // from class: com.android.server.pm.permission.OneTimePermissionUserManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.UID_REMOVED".equals(intent.getAction())) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                PackageInactivityListener listener = (PackageInactivityListener) OneTimePermissionUserManager.this.mListeners.get(uid);
                if (listener != null) {
                    listener.cancel();
                    OneTimePermissionUserManager.this.mListeners.remove(uid);
                }
            }
        }
    };
    private final SparseArray<PackageInactivityListener> mListeners = new SparseArray<>();
    private final IActivityManager mIActivityManager = ActivityManager.getService();
    private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);

    /* JADX INFO: Access modifiers changed from: package-private */
    public OneTimePermissionUserManager(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mPermissionControllerManager = (PermissionControllerManager) context.getSystemService(PermissionControllerManager.class);
        this.mHandler = context.getMainThreadHandler();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startPackageOneTimeSession(String packageName, long timeoutMillis, long revokeAfterKilledDelayMillis) {
        try {
            int uid = this.mContext.getPackageManager().getPackageUid(packageName, 0);
            synchronized (this.mLock) {
                try {
                    try {
                        PackageInactivityListener listener = this.mListeners.get(uid);
                        if (listener != null) {
                            listener.updateSessionParameters(timeoutMillis, revokeAfterKilledDelayMillis);
                            return;
                        }
                        this.mListeners.put(uid, new PackageInactivityListener(uid, packageName, timeoutMillis, revokeAfterKilledDelayMillis));
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Unknown package name " + packageName, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopPackageOneTimeSession(String packageName) {
        try {
            int uid = this.mContext.getPackageManager().getPackageUid(packageName, 0);
            synchronized (this.mLock) {
                PackageInactivityListener listener = this.mListeners.get(uid);
                if (listener != null) {
                    this.mListeners.remove(uid);
                    listener.cancel();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Unknown package name " + packageName, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerUninstallListener() {
        this.mContext.registerReceiver(this.mUninstallListener, new IntentFilter("android.intent.action.UID_REMOVED"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PackageInactivityListener implements AlarmManager.OnAlarmListener {
        private static final int STATE_ACTIVE = 2;
        private static final int STATE_GONE = 0;
        private static final int STATE_TIMER = 1;
        private static final long TIMER_INACTIVE = -1;
        private final Object mInnerLock;
        private boolean mIsAlarmSet;
        private boolean mIsFinished;
        private final IUidObserver.Stub mObserver;
        private final String mPackageName;
        private long mRevokeAfterKilledDelay;
        private long mTimeout;
        private long mTimerStart;
        private final Object mToken;
        private final int mUid;

        private PackageInactivityListener(int uid, String packageName, long timeout, long revokeAfterkilledDelay) {
            long j;
            this.mTimerStart = -1L;
            this.mInnerLock = new Object();
            this.mToken = new Object();
            IUidObserver.Stub stub = new IUidObserver.Stub() { // from class: com.android.server.pm.permission.OneTimePermissionUserManager.PackageInactivityListener.1
                public void onUidGone(int uid2, boolean disabled) {
                    if (uid2 == PackageInactivityListener.this.mUid) {
                        PackageInactivityListener.this.updateUidState(0);
                    }
                }

                public void onUidStateChanged(int uid2, int procState, long procStateSeq, int capability) {
                    if (uid2 == PackageInactivityListener.this.mUid) {
                        if (procState > 4 && procState != 20) {
                            PackageInactivityListener.this.updateUidState(1);
                        } else {
                            PackageInactivityListener.this.updateUidState(2);
                        }
                    }
                }

                public void onUidActive(int uid2) {
                }

                public void onUidIdle(int uid2, boolean disabled) {
                }

                public void onUidProcAdjChanged(int uid2) {
                }

                public void onUidCachedChanged(int uid2, boolean cached) {
                }
            };
            this.mObserver = stub;
            Log.i(OneTimePermissionUserManager.LOG_TAG, "Start tracking " + packageName + ". uid=" + uid + " timeout=" + timeout + " killedDelay=" + revokeAfterkilledDelay);
            this.mUid = uid;
            this.mPackageName = packageName;
            this.mTimeout = timeout;
            if (revokeAfterkilledDelay == -1) {
                j = DeviceConfig.getLong("permissions", OneTimePermissionUserManager.PROPERTY_KILLED_DELAY_CONFIG_KEY, (long) OneTimePermissionUserManager.DEFAULT_KILLED_DELAY_MILLIS);
            } else {
                j = revokeAfterkilledDelay;
            }
            this.mRevokeAfterKilledDelay = j;
            try {
                OneTimePermissionUserManager.this.mIActivityManager.registerUidObserver(stub, 3, 4, (String) null);
            } catch (RemoteException e) {
                Log.e(OneTimePermissionUserManager.LOG_TAG, "Couldn't check uid proc state", e);
                synchronized (this.mInnerLock) {
                    onPackageInactiveLocked();
                }
            }
            updateUidState();
        }

        public void updateSessionParameters(long timeoutMillis, long revokeAfterKilledDelayMillis) {
            long j;
            synchronized (this.mInnerLock) {
                this.mTimeout = Math.min(this.mTimeout, timeoutMillis);
                long j2 = this.mRevokeAfterKilledDelay;
                if (revokeAfterKilledDelayMillis == -1) {
                    j = DeviceConfig.getLong("permissions", OneTimePermissionUserManager.PROPERTY_KILLED_DELAY_CONFIG_KEY, (long) OneTimePermissionUserManager.DEFAULT_KILLED_DELAY_MILLIS);
                } else {
                    j = revokeAfterKilledDelayMillis;
                }
                this.mRevokeAfterKilledDelay = Math.min(j2, j);
                Log.v(OneTimePermissionUserManager.LOG_TAG, "Updated params for " + this.mPackageName + ". timeout=" + this.mTimeout + " killedDelay=" + this.mRevokeAfterKilledDelay);
                updateUidState();
            }
        }

        private int getCurrentState() {
            return getStateFromProcState(OneTimePermissionUserManager.this.mActivityManagerInternal.getUidProcessState(this.mUid));
        }

        private int getStateFromProcState(int procState) {
            if (procState == 20) {
                return 0;
            }
            if (procState > 4) {
                return 1;
            }
            return 2;
        }

        private void updateUidState() {
            updateUidState(getCurrentState());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateUidState(int state) {
            Log.v(OneTimePermissionUserManager.LOG_TAG, "Updating state for " + this.mPackageName + " (" + this.mUid + "). state=" + state);
            synchronized (this.mInnerLock) {
                OneTimePermissionUserManager.this.mHandler.removeCallbacksAndMessages(this.mToken);
                if (state == 0) {
                    if (this.mRevokeAfterKilledDelay == 0) {
                        onPackageInactiveLocked();
                        return;
                    } else {
                        OneTimePermissionUserManager.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.permission.OneTimePermissionUserManager$PackageInactivityListener$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                OneTimePermissionUserManager.PackageInactivityListener.this.m5815xa831ea07();
                            }
                        }, this.mToken, this.mRevokeAfterKilledDelay);
                        return;
                    }
                }
                if (state == 1) {
                    if (this.mTimerStart == -1) {
                        this.mTimerStart = System.currentTimeMillis();
                        setAlarmLocked();
                    }
                } else if (state == 2) {
                    this.mTimerStart = -1L;
                    cancelAlarmLocked();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateUidState$0$com-android-server-pm-permission-OneTimePermissionUserManager$PackageInactivityListener  reason: not valid java name */
        public /* synthetic */ void m5815xa831ea07() {
            synchronized (this.mInnerLock) {
                int currentState = getCurrentState();
                if (currentState == 0) {
                    onPackageInactiveLocked();
                } else {
                    updateUidState(currentState);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void cancel() {
            synchronized (this.mInnerLock) {
                this.mIsFinished = true;
                cancelAlarmLocked();
                try {
                    OneTimePermissionUserManager.this.mIActivityManager.unregisterUidObserver(this.mObserver);
                } catch (RemoteException e) {
                    Log.e(OneTimePermissionUserManager.LOG_TAG, "Unable to unregister uid observer.", e);
                }
            }
        }

        private void setAlarmLocked() {
            if (this.mIsAlarmSet) {
                return;
            }
            long revokeTime = this.mTimerStart + this.mTimeout;
            if (revokeTime > System.currentTimeMillis()) {
                OneTimePermissionUserManager.this.mAlarmManager.setExact(0, revokeTime, OneTimePermissionUserManager.LOG_TAG, this, OneTimePermissionUserManager.this.mHandler);
                this.mIsAlarmSet = true;
                return;
            }
            this.mIsAlarmSet = true;
            onAlarm();
        }

        private void cancelAlarmLocked() {
            if (this.mIsAlarmSet) {
                OneTimePermissionUserManager.this.mAlarmManager.cancel(this);
                this.mIsAlarmSet = false;
            }
        }

        private void onPackageInactiveLocked() {
            if (this.mIsFinished) {
                return;
            }
            this.mIsFinished = true;
            cancelAlarmLocked();
            OneTimePermissionUserManager.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.permission.OneTimePermissionUserManager$PackageInactivityListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    OneTimePermissionUserManager.PackageInactivityListener.this.m5814x9fb6e340();
                }
            });
            try {
                OneTimePermissionUserManager.this.mIActivityManager.unregisterUidObserver(this.mObserver);
            } catch (RemoteException e) {
                Log.e(OneTimePermissionUserManager.LOG_TAG, "Unable to unregister uid observer.", e);
            }
            synchronized (OneTimePermissionUserManager.this.mLock) {
                OneTimePermissionUserManager.this.mListeners.remove(this.mUid);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPackageInactiveLocked$1$com-android-server-pm-permission-OneTimePermissionUserManager$PackageInactivityListener  reason: not valid java name */
        public /* synthetic */ void m5814x9fb6e340() {
            Log.i(OneTimePermissionUserManager.LOG_TAG, "One time session expired for " + this.mPackageName + " (" + this.mUid + ").");
            OneTimePermissionUserManager.this.mPermissionControllerManager.notifyOneTimePermissionSessionTimeout(this.mPackageName);
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            synchronized (this.mInnerLock) {
                if (this.mIsAlarmSet) {
                    this.mIsAlarmSet = false;
                    onPackageInactiveLocked();
                }
            }
        }
    }
}
