package android.window;

import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.InsetsController$$ExternalSyntheticLambda4;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.ViewRootImpl$8$$ExternalSyntheticLambda0;
import android.window.SurfaceSyncer;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class SurfaceSyncer {
    private static final boolean DEBUG = false;
    private static final String TAG = "SurfaceSyncer";
    private static Supplier<SurfaceControl.Transaction> sTransactionFactory = new InsetsController$$ExternalSyntheticLambda4();
    private final Object mSyncSetLock = new Object();
    private final SparseArray<SyncSet> mSyncSets = new SparseArray<>();
    private int mIdCounter = 0;

    /* loaded from: classes4.dex */
    public interface SurfaceViewFrameCallback {
        void onFrameStarted();
    }

    /* loaded from: classes4.dex */
    public interface SyncBufferCallback {
        void onBufferReady(SurfaceControl.Transaction transaction);
    }

    public static void setTransactionFactory(Supplier<SurfaceControl.Transaction> transactionFactory) {
        sTransactionFactory = transactionFactory;
    }

    public int setupSync(final Runnable onComplete) {
        final Handler handler = new Handler(Looper.myLooper());
        return setupSync(new Consumer() { // from class: android.window.SurfaceSyncer$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                SurfaceSyncer.lambda$setupSync$0(onComplete, handler, (SurfaceControl.Transaction) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setupSync$0(Runnable onComplete, Handler handler, SurfaceControl.Transaction transaction) {
        transaction.apply();
        if (onComplete != null) {
            handler.post(onComplete);
        }
    }

    public int setupSync(final Consumer<SurfaceControl.Transaction> syncRequestComplete) {
        final int syncId;
        synchronized (this.mSyncSetLock) {
            syncId = this.mIdCounter;
            this.mIdCounter = syncId + 1;
            SyncSet syncSet = new SyncSet(syncId, new Consumer() { // from class: android.window.SurfaceSyncer$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SurfaceSyncer.this.m6255lambda$setupSync$1$androidwindowSurfaceSyncer(syncId, syncRequestComplete, (SurfaceControl.Transaction) obj);
                }
            });
            this.mSyncSets.put(syncId, syncSet);
        }
        return syncId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupSync$1$android-window-SurfaceSyncer  reason: not valid java name */
    public /* synthetic */ void m6255lambda$setupSync$1$androidwindowSurfaceSyncer(int syncId, Consumer syncRequestComplete, SurfaceControl.Transaction transaction) {
        synchronized (this.mSyncSetLock) {
            this.mSyncSets.remove(syncId);
        }
        syncRequestComplete.accept(transaction);
    }

    public void markSyncReady(int syncId) {
        SyncSet syncSet;
        synchronized (this.mSyncSetLock) {
            syncSet = this.mSyncSets.get(syncId);
        }
        if (syncSet == null) {
            Log.e(TAG, "Failed to find syncSet for syncId=" + syncId);
        } else {
            syncSet.markSyncReady();
        }
    }

    public void merge(int syncId, int otherSyncId, SurfaceSyncer otherSurfaceSyncer) {
        SyncSet syncSet;
        synchronized (this.mSyncSetLock) {
            syncSet = this.mSyncSets.get(syncId);
        }
        SyncSet otherSyncSet = otherSurfaceSyncer.getAndValidateSyncSet(otherSyncId);
        if (otherSyncSet == null) {
            return;
        }
        syncSet.merge(otherSyncSet);
    }

    public boolean addToSync(int syncId, SurfaceView surfaceView, Consumer<SurfaceViewFrameCallback> frameCallbackConsumer) {
        return addToSync(syncId, new SurfaceViewSyncTarget(surfaceView, frameCallbackConsumer));
    }

    public boolean addToSync(int syncId, View view) {
        ViewRootImpl viewRoot = view.getViewRootImpl();
        if (viewRoot == null) {
            return false;
        }
        return addToSync(syncId, viewRoot.mSyncTarget);
    }

    public boolean addToSync(int syncId, SyncTarget syncTarget) {
        SyncSet syncSet = getAndValidateSyncSet(syncId);
        if (syncSet == null) {
            return false;
        }
        return syncSet.addSyncableSurface(syncTarget);
    }

    public void addTransactionToSync(int syncId, SurfaceControl.Transaction t) {
        SyncSet syncSet = getAndValidateSyncSet(syncId);
        if (syncSet != null) {
            syncSet.addTransactionToSync(t);
        }
    }

    private SyncSet getAndValidateSyncSet(int syncId) {
        SyncSet syncSet;
        synchronized (this.mSyncSetLock) {
            syncSet = this.mSyncSets.get(syncId);
        }
        if (syncSet == null) {
            Log.e(TAG, "Failed to find sync for id=" + syncId);
            return null;
        }
        return syncSet;
    }

    /* loaded from: classes4.dex */
    public interface SyncTarget {
        void onReadyToSync(SyncBufferCallback syncBufferCallback);

        default void onSyncComplete() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class SyncSet {
        private boolean mFinished;
        private final Object mLock;
        private final Set<SyncSet> mMergedSyncSets;
        private final Set<Integer> mPendingSyncs;
        private final int mSyncId;
        private boolean mSyncReady;
        private Consumer<SurfaceControl.Transaction> mSyncRequestCompleteCallback;
        private final Set<SyncTarget> mSyncTargets;
        private final SurfaceControl.Transaction mTransaction;

        private SyncSet(int syncId, Consumer<SurfaceControl.Transaction> syncRequestComplete) {
            this.mLock = new Object();
            this.mPendingSyncs = new ArraySet();
            this.mTransaction = (SurfaceControl.Transaction) SurfaceSyncer.sTransactionFactory.get();
            this.mSyncTargets = new ArraySet();
            this.mMergedSyncSets = new ArraySet();
            this.mSyncId = syncId;
            this.mSyncRequestCompleteCallback = syncRequestComplete;
        }

        boolean addSyncableSurface(SyncTarget syncTarget) {
            SyncBufferCallback syncBufferCallback = new SyncBufferCallback() { // from class: android.window.SurfaceSyncer.SyncSet.1
                @Override // android.window.SurfaceSyncer.SyncBufferCallback
                public void onBufferReady(SurfaceControl.Transaction t) {
                    synchronized (SyncSet.this.mLock) {
                        if (t != null) {
                            SyncSet.this.mTransaction.merge(t);
                        }
                        SyncSet.this.mPendingSyncs.remove(Integer.valueOf(hashCode()));
                        SyncSet.this.checkIfSyncIsComplete();
                    }
                }
            };
            synchronized (this.mLock) {
                if (this.mSyncReady) {
                    Log.e(SurfaceSyncer.TAG, "Sync " + this.mSyncId + " was already marked as ready. No more SyncTargets can be added.");
                    return false;
                }
                this.mPendingSyncs.add(Integer.valueOf(syncBufferCallback.hashCode()));
                this.mSyncTargets.add(syncTarget);
                syncTarget.onReadyToSync(syncBufferCallback);
                return true;
            }
        }

        void markSyncReady() {
            synchronized (this.mLock) {
                this.mSyncReady = true;
                checkIfSyncIsComplete();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void checkIfSyncIsComplete() {
            if (!this.mSyncReady || !this.mPendingSyncs.isEmpty() || !this.mMergedSyncSets.isEmpty()) {
                return;
            }
            for (SyncTarget syncTarget : this.mSyncTargets) {
                syncTarget.onSyncComplete();
            }
            this.mSyncTargets.clear();
            this.mSyncRequestCompleteCallback.accept(this.mTransaction);
            this.mFinished = true;
        }

        void addTransactionToSync(SurfaceControl.Transaction t) {
            synchronized (this.mLock) {
                this.mTransaction.merge(t);
            }
        }

        public void updateCallback(final Consumer<SurfaceControl.Transaction> transactionConsumer) {
            synchronized (this.mLock) {
                if (this.mFinished) {
                    Log.e(SurfaceSyncer.TAG, "Attempting to merge SyncSet " + this.mSyncId + " when sync is already complete");
                    transactionConsumer.accept(new SurfaceControl.Transaction());
                }
                final Consumer<SurfaceControl.Transaction> oldCallback = this.mSyncRequestCompleteCallback;
                this.mSyncRequestCompleteCallback = new Consumer() { // from class: android.window.SurfaceSyncer$SyncSet$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        SurfaceSyncer.SyncSet.lambda$updateCallback$0(oldCallback, transactionConsumer, (SurfaceControl.Transaction) obj);
                    }
                };
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$updateCallback$0(Consumer oldCallback, Consumer transactionConsumer, SurfaceControl.Transaction transaction) {
            oldCallback.accept(new SurfaceControl.Transaction());
            transactionConsumer.accept(transaction);
        }

        public void merge(final SyncSet otherSyncSet) {
            synchronized (this.mLock) {
                this.mMergedSyncSets.add(otherSyncSet);
            }
            otherSyncSet.updateCallback(new Consumer() { // from class: android.window.SurfaceSyncer$SyncSet$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SurfaceSyncer.SyncSet.this.m6261lambda$merge$1$androidwindowSurfaceSyncer$SyncSet(otherSyncSet, (SurfaceControl.Transaction) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$merge$1$android-window-SurfaceSyncer$SyncSet  reason: not valid java name */
        public /* synthetic */ void m6261lambda$merge$1$androidwindowSurfaceSyncer$SyncSet(SyncSet otherSyncSet, SurfaceControl.Transaction transaction) {
            synchronized (this.mLock) {
                this.mMergedSyncSets.remove(otherSyncSet);
                this.mTransaction.merge(transaction);
                checkIfSyncIsComplete();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class SurfaceViewSyncTarget implements SyncTarget {
        private final Consumer<SurfaceViewFrameCallback> mFrameCallbackConsumer;
        private final SurfaceView mSurfaceView;

        SurfaceViewSyncTarget(SurfaceView surfaceView, Consumer<SurfaceViewFrameCallback> frameCallbackConsumer) {
            this.mSurfaceView = surfaceView;
            this.mFrameCallbackConsumer = frameCallbackConsumer;
        }

        @Override // android.window.SurfaceSyncer.SyncTarget
        public void onReadyToSync(final SyncBufferCallback syncBufferCallback) {
            this.mFrameCallbackConsumer.accept(new SurfaceViewFrameCallback() { // from class: android.window.SurfaceSyncer$SurfaceViewSyncTarget$$ExternalSyntheticLambda0
                @Override // android.window.SurfaceSyncer.SurfaceViewFrameCallback
                public final void onFrameStarted() {
                    SurfaceSyncer.SurfaceViewSyncTarget.this.m6256x3717becb(syncBufferCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReadyToSync$0$android-window-SurfaceSyncer$SurfaceViewSyncTarget  reason: not valid java name */
        public /* synthetic */ void m6256x3717becb(SyncBufferCallback syncBufferCallback) {
            SurfaceView surfaceView = this.mSurfaceView;
            Objects.requireNonNull(syncBufferCallback);
            surfaceView.syncNextFrame(new ViewRootImpl$8$$ExternalSyntheticLambda0(syncBufferCallback));
        }
    }
}
