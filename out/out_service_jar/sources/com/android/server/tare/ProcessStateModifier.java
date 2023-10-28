package com.android.server.tare;

import android.app.ActivityManager;
import android.app.IUidObserver;
import android.os.RemoteException;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.SparseIntArray;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ProcessStateModifier extends Modifier {
    private static final int PROC_STATE_BUCKET_BFGS = 3;
    private static final int PROC_STATE_BUCKET_BG = 4;
    private static final int PROC_STATE_BUCKET_FGS = 2;
    private static final int PROC_STATE_BUCKET_NONE = 0;
    private static final int PROC_STATE_BUCKET_TOP = 1;
    private static final String TAG = "TARE-" + ProcessStateModifier.class.getSimpleName();
    private final InternalResourceService mIrs;
    private final Object mLock = new Object();
    private final SparseArrayMap<String, Integer> mPackageToUidCache = new SparseArrayMap<>();
    private final SparseIntArray mUidProcStateBucketCache = new SparseIntArray();
    private final IUidObserver mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.tare.ProcessStateModifier.1
        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            int newBucket = ProcessStateModifier.this.getProcStateBucket(procState);
            synchronized (ProcessStateModifier.this.mLock) {
                int curBucket = ProcessStateModifier.this.mUidProcStateBucketCache.get(uid);
                if (curBucket != newBucket) {
                    ProcessStateModifier.this.mUidProcStateBucketCache.put(uid, newBucket);
                }
                ProcessStateModifier.this.notifyStateChangedLocked(uid);
            }
        }

        public void onUidGone(int uid, boolean disabled) {
            synchronized (ProcessStateModifier.this.mLock) {
                if (ProcessStateModifier.this.mUidProcStateBucketCache.indexOfKey(uid) < 0) {
                    Slog.e(ProcessStateModifier.TAG, "UID " + uid + " marked gone but wasn't in cache.");
                    return;
                }
                ProcessStateModifier.this.mUidProcStateBucketCache.delete(uid);
                ProcessStateModifier.this.notifyStateChangedLocked(uid);
            }
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ProcStateBucket {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessStateModifier(InternalResourceService irs) {
        this.mIrs = irs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void setup() {
        try {
            ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void tearDown() {
        try {
            ActivityManager.getService().unregisterUidObserver(this.mUidObserver);
        } catch (RemoteException e) {
        }
        this.mPackageToUidCache.clear();
        this.mUidProcStateBucketCache.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getModifiedPrice(int userId, String pkgName, long ctp, long price) {
        int procState;
        synchronized (this.mLock) {
            procState = this.mUidProcStateBucketCache.get(this.mIrs.getUid(userId, pkgName), 0);
        }
        switch (procState) {
            case 1:
                return 0L;
            case 2:
                return Math.min(ctp, price);
            case 3:
                if (price <= ctp) {
                    return price;
                }
                return (long) (ctp + ((price - ctp) * 0.5d));
            default:
                return price;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.Modifier
    public void dump(IndentingPrintWriter pw) {
        pw.print("Proc state bucket cache = ");
        pw.println(this.mUidProcStateBucketCache);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getProcStateBucket(int procState) {
        if (procState <= 2) {
            return 1;
        }
        if (procState <= 4) {
            return 2;
        }
        if (procState > 5) {
            return 4;
        }
        return 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyStateChangedLocked(final int uid) {
        TareHandlerThread.getHandler().post(new Runnable() { // from class: com.android.server.tare.ProcessStateModifier$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProcessStateModifier.this.m6777x6105f8ab(uid);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyStateChangedLocked$0$com-android-server-tare-ProcessStateModifier  reason: not valid java name */
    public /* synthetic */ void m6777x6105f8ab(int uid) {
        this.mIrs.onUidStateChanged(uid);
    }
}
