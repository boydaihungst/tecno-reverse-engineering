package com.android.server.wm;

import android.os.Trace;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.BLASTSyncEngine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executor;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BLASTSyncEngine {
    private static final String TAG = "BLASTSyncEngine";
    private final WindowManagerService mWm;
    private int mNextSyncId = 0;
    private final SparseArray<SyncGroup> mActiveSyncs = new SparseArray<>();
    private final ArrayList<PendingSyncSet> mPendingSyncSets = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface TransactionReadyListener {
        void onTransactionReady(int i, SurfaceControl.Transaction transaction);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PendingSyncSet {
        private Runnable mApplySync;
        private Runnable mStartSync;

        private PendingSyncSet() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class SyncGroup {
        final TransactionReadyListener mListener;
        final Runnable mOnTimeout;
        private SurfaceControl.Transaction mOrphanTransaction;
        boolean mReady;
        final ArraySet<WindowContainer> mRootMembers;
        final int mSyncId;
        private String mTraceName;

        private SyncGroup(TransactionReadyListener listener, int id, String name) {
            this.mReady = false;
            this.mRootMembers = new ArraySet<>();
            this.mOrphanTransaction = null;
            this.mSyncId = id;
            this.mListener = listener;
            this.mOnTimeout = new Runnable() { // from class: com.android.server.wm.BLASTSyncEngine$SyncGroup$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    BLASTSyncEngine.SyncGroup.this.m7876lambda$new$0$comandroidserverwmBLASTSyncEngine$SyncGroup();
                }
            };
            if (Trace.isTagEnabled(32L)) {
                String str = name + "SyncGroupReady";
                this.mTraceName = str;
                Trace.asyncTraceBegin(32L, str, id);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-wm-BLASTSyncEngine$SyncGroup  reason: not valid java name */
        public /* synthetic */ void m7876lambda$new$0$comandroidserverwmBLASTSyncEngine$SyncGroup() {
            Slog.w(BLASTSyncEngine.TAG, "Sync group " + this.mSyncId + " timeout");
            synchronized (BLASTSyncEngine.this.mWm.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    onTimeout();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public SurfaceControl.Transaction getOrphanTransaction() {
            if (this.mOrphanTransaction == null) {
                this.mOrphanTransaction = BLASTSyncEngine.this.mWm.mTransactionFactory.get();
            }
            return this.mOrphanTransaction;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onSurfacePlacement() {
            if (this.mReady) {
                if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                    long protoLogParam0 = this.mSyncId;
                    String protoLogParam1 = String.valueOf(this.mRootMembers);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 966569777, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
                }
                for (int i = this.mRootMembers.size() - 1; i >= 0; i--) {
                    WindowContainer wc = this.mRootMembers.valueAt(i);
                    if (!wc.isSyncFinished()) {
                        if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                            long protoLogParam02 = this.mSyncId;
                            String protoLogParam12 = String.valueOf(wc);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, -230587670, 1, (String) null, new Object[]{Long.valueOf(protoLogParam02), protoLogParam12});
                            return;
                        } else {
                            return;
                        }
                    }
                }
                finishNow();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void finishNow() {
            String str = this.mTraceName;
            if (str != null) {
                Trace.asyncTraceEnd(32L, str, this.mSyncId);
            }
            if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                long protoLogParam0 = this.mSyncId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, -1905191109, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            SurfaceControl.Transaction merged = BLASTSyncEngine.this.mWm.mTransactionFactory.get();
            SurfaceControl.Transaction transaction = this.mOrphanTransaction;
            if (transaction != null) {
                merged.merge(transaction);
            }
            Iterator<WindowContainer> it = this.mRootMembers.iterator();
            while (it.hasNext()) {
                WindowContainer wc = it.next();
                wc.finishSync(merged, false);
            }
            ArraySet<WindowContainer> wcAwaitingCommit = new ArraySet<>();
            Iterator<WindowContainer> it2 = this.mRootMembers.iterator();
            while (it2.hasNext()) {
                WindowContainer wc2 = it2.next();
                wc2.waitForSyncTransactionCommit(wcAwaitingCommit);
            }
            final C1CommitCallback callback = new C1CommitCallback(wcAwaitingCommit);
            Executor executor = new Executor() { // from class: com.android.server.wm.BLASTSyncEngine$SyncGroup$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Executor
                public final void execute(Runnable runnable) {
                    runnable.run();
                }
            };
            Objects.requireNonNull(callback);
            merged.addTransactionCommittedListener(executor, new SurfaceControl.TransactionCommittedListener() { // from class: com.android.server.wm.BLASTSyncEngine$SyncGroup$$ExternalSyntheticLambda1
                public final void onTransactionCommitted() {
                    BLASTSyncEngine.SyncGroup.C1CommitCallback.this.onCommitted();
                }
            });
            BLASTSyncEngine.this.mWm.mH.postDelayed(callback, 5000L);
            Trace.traceBegin(32L, "onTransactionReady");
            this.mListener.onTransactionReady(this.mSyncId, merged);
            Trace.traceEnd(32L);
            BLASTSyncEngine.this.mActiveSyncs.remove(this.mSyncId);
            BLASTSyncEngine.this.mWm.mH.removeCallbacks(this.mOnTimeout);
            if (BLASTSyncEngine.this.mActiveSyncs.size() == 0 && !BLASTSyncEngine.this.mPendingSyncSets.isEmpty()) {
                if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 1730300180, 0, (String) null, (Object[]) null);
                }
                final PendingSyncSet pt = (PendingSyncSet) BLASTSyncEngine.this.mPendingSyncSets.remove(0);
                pt.mStartSync.run();
                if (BLASTSyncEngine.this.mActiveSyncs.size() == 0) {
                    throw new IllegalStateException("Pending Sync Set didn't start a sync.");
                }
                BLASTSyncEngine.this.mWm.mH.post(new Runnable() { // from class: com.android.server.wm.BLASTSyncEngine$SyncGroup$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        BLASTSyncEngine.SyncGroup.this.m7875x5d40ddce(pt);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: com.android.server.wm.BLASTSyncEngine$SyncGroup$1CommitCallback  reason: invalid class name */
        /* loaded from: classes2.dex */
        public class C1CommitCallback implements Runnable {
            boolean ran = false;
            final /* synthetic */ ArraySet val$wcAwaitingCommit;

            /* JADX DEBUG: Incorrect args count in method signature: ()V */
            C1CommitCallback(ArraySet arraySet) {
                this.val$wcAwaitingCommit = arraySet;
            }

            public void onCommitted() {
                synchronized (BLASTSyncEngine.this.mWm.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (this.ran) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        BLASTSyncEngine.this.mWm.mH.removeCallbacks(this);
                        this.ran = true;
                        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                        Iterator it = this.val$wcAwaitingCommit.iterator();
                        while (it.hasNext()) {
                            WindowContainer wc = (WindowContainer) it.next();
                            wc.onSyncTransactionCommitted(t);
                        }
                        t.apply();
                        this.val$wcAwaitingCommit.clear();
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }

            @Override // java.lang.Runnable
            public void run() {
                Trace.traceBegin(32L, "onTransactionCommitTimeout");
                Slog.e(BLASTSyncEngine.TAG, "WM sent Transaction to organized, but never received commit callback. Application ANR likely to follow.");
                Trace.traceEnd(32L);
                onCommitted();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$finishNow$2$com-android-server-wm-BLASTSyncEngine$SyncGroup  reason: not valid java name */
        public /* synthetic */ void m7875x5d40ddce(PendingSyncSet pt) {
            synchronized (BLASTSyncEngine.this.mWm.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    pt.mApplySync.run();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setReady(boolean ready) {
            if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                long protoLogParam0 = this.mSyncId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 1689989893, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            this.mReady = ready;
            if (ready) {
                BLASTSyncEngine.this.mWm.mWindowPlacerLocked.requestTraversal();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addToSync(WindowContainer wc) {
            if (!this.mRootMembers.add(wc)) {
                return;
            }
            if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
                long protoLogParam0 = this.mSyncId;
                String protoLogParam1 = String.valueOf(wc);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, -1973119651, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
            }
            wc.setSyncGroup(this);
            wc.prepareSync();
            BLASTSyncEngine.this.mWm.mWindowPlacerLocked.requestTraversal();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void onCancelSync(WindowContainer wc) {
            this.mRootMembers.remove(wc);
        }

        private void onTimeout() {
            if (BLASTSyncEngine.this.mActiveSyncs.contains(this.mSyncId)) {
                for (int i = this.mRootMembers.size() - 1; i >= 0; i--) {
                    WindowContainer<?> wc = this.mRootMembers.valueAt(i);
                    if (!wc.isSyncFinished()) {
                        Slog.i(BLASTSyncEngine.TAG, "Unfinished container: " + wc);
                    }
                }
                finishNow();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BLASTSyncEngine(WindowManagerService wms) {
        this.mWm = wms;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SyncGroup prepareSyncSet(TransactionReadyListener listener, String name) {
        int i = this.mNextSyncId;
        this.mNextSyncId = i + 1;
        return new SyncGroup(listener, i, name);
    }

    int startSyncSet(TransactionReadyListener listener) {
        return startSyncSet(listener, 5000L, "");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int startSyncSet(TransactionReadyListener listener, long timeoutMs, String name) {
        SyncGroup s = prepareSyncSet(listener, name);
        startSyncSet(s, timeoutMs);
        return s.mSyncId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSyncSet(SyncGroup s) {
        startSyncSet(s, 5000L);
    }

    void startSyncSet(SyncGroup s, long timeoutMs) {
        if (this.mActiveSyncs.size() != 0 && ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
            long protoLogParam0 = s.mSyncId;
            ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 800698875, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        this.mActiveSyncs.put(s.mSyncId, s);
        if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
            long protoLogParam02 = s.mSyncId;
            String protoLogParam1 = String.valueOf(s.mListener);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 550717438, 1, (String) null, new Object[]{Long.valueOf(protoLogParam02), protoLogParam1});
        }
        scheduleTimeout(s, timeoutMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActiveSync() {
        return this.mActiveSyncs.size() != 0;
    }

    void scheduleTimeout(SyncGroup s, long timeoutMs) {
        this.mWm.mH.postDelayed(s.mOnTimeout, timeoutMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addToSyncSet(int id, WindowContainer wc) {
        getSyncGroup(id).addToSync(wc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady(int id, boolean ready) {
        getSyncGroup(id).setReady(ready);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReady(int id) {
        setReady(id, true);
    }

    boolean isReady(int id) {
        return getSyncGroup(id).mReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abort(int id) {
        getSyncGroup(id).finishNow();
    }

    private SyncGroup getSyncGroup(int id) {
        SyncGroup syncGroup = this.mActiveSyncs.get(id);
        if (syncGroup == null) {
            throw new IllegalStateException("SyncGroup is not started yet id=" + id);
        }
        return syncGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSurfacePlacement() {
        for (int i = this.mActiveSyncs.size() - 1; i >= 0; i--) {
            this.mActiveSyncs.valueAt(i).onSurfacePlacement();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void queueSyncSet(Runnable startSync, Runnable applySync) {
        PendingSyncSet pt = new PendingSyncSet();
        pt.mStartSync = startSync;
        pt.mApplySync = applySync;
        this.mPendingSyncSets.add(pt);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingSyncSets() {
        return !this.mPendingSyncSets.isEmpty();
    }
}
