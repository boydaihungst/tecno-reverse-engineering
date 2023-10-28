package com.android.server.location.contexthub;

import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppState;
import android.os.RemoteException;
import android.util.Log;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ContextHubTransactionManager {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");
    private static final int MAX_PENDING_REQUESTS = 10000;
    private static final int NUM_TRANSACTION_RECORDS = 20;
    private static final String TAG = "ContextHubTransactionManager";
    private final ContextHubClientManager mClientManager;
    private final IContextHubWrapper mContextHubProxy;
    private final NanoAppStateManager mNanoAppStateManager;
    private final ArrayDeque<ContextHubServiceTransaction> mTransactionQueue = new ArrayDeque<>();
    private final AtomicInteger mNextAvailableId = new AtomicInteger();
    private final ScheduledThreadPoolExecutor mTimeoutExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> mTimeoutFuture = null;
    private final ConcurrentLinkedEvictingDeque<TransactionRecord> mTransactionRecordDeque = new ConcurrentLinkedEvictingDeque<>(20);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TransactionRecord {
        private final long mTimestamp = System.currentTimeMillis();
        private final String mTransaction;

        TransactionRecord(String transaction) {
            this.mTransaction = transaction;
        }

        public String toString() {
            return ContextHubTransactionManager.DATE_FORMAT.format(new Date(this.mTimestamp)) + " " + this.mTransaction;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubTransactionManager(IContextHubWrapper contextHubProxy, ContextHubClientManager clientManager, NanoAppStateManager nanoAppStateManager) {
        this.mContextHubProxy = contextHubProxy;
        this.mClientManager = clientManager;
        this.mNanoAppStateManager = nanoAppStateManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction createLoadTransaction(final int contextHubId, final NanoAppBinary nanoAppBinary, final IContextHubTransactionCallback onCompleteCallback, String packageName) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 0, nanoAppBinary.getNanoAppId(), packageName) { // from class: com.android.server.location.contexthub.ContextHubTransactionManager.1
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.loadNanoapp(contextHubId, nanoAppBinary, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to load nanoapp with ID 0x" + Long.toHexString(nanoAppBinary.getNanoAppId()), e);
                    return 1;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onTransactionComplete(int result) {
                ContextHubStatsLog.write(ContextHubStatsLog.CHRE_CODE_DOWNLOAD_TRANSACTED, nanoAppBinary.getNanoAppId(), nanoAppBinary.getNanoAppVersion(), 1, ContextHubTransactionManager.this.toStatsTransactionResult(result));
                if (result == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.addNanoAppInstance(contextHubId, nanoAppBinary.getNanoAppId(), nanoAppBinary.getNanoAppVersion());
                }
                try {
                    onCompleteCallback.onTransactionComplete(result);
                    if (result == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppLoaded(contextHubId, nanoAppBinary.getNanoAppId());
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction createUnloadTransaction(final int contextHubId, final long nanoAppId, final IContextHubTransactionCallback onCompleteCallback, String packageName) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 1, nanoAppId, packageName) { // from class: com.android.server.location.contexthub.ContextHubTransactionManager.2
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.unloadNanoapp(contextHubId, nanoAppId, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to unload nanoapp with ID 0x" + Long.toHexString(nanoAppId), e);
                    return 1;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onTransactionComplete(int result) {
                ContextHubStatsLog.write(ContextHubStatsLog.CHRE_CODE_DOWNLOAD_TRANSACTED, nanoAppId, 0, 2, ContextHubTransactionManager.this.toStatsTransactionResult(result));
                if (result == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.removeNanoAppInstance(contextHubId, nanoAppId);
                }
                try {
                    onCompleteCallback.onTransactionComplete(result);
                    if (result == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppUnloaded(contextHubId, nanoAppId);
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction createEnableTransaction(final int contextHubId, final long nanoAppId, final IContextHubTransactionCallback onCompleteCallback, String packageName) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 2, packageName) { // from class: com.android.server.location.contexthub.ContextHubTransactionManager.3
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.enableNanoapp(contextHubId, nanoAppId, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to enable nanoapp with ID 0x" + Long.toHexString(nanoAppId), e);
                    return 1;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onTransactionComplete(int result) {
                try {
                    onCompleteCallback.onTransactionComplete(result);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction createDisableTransaction(final int contextHubId, final long nanoAppId, final IContextHubTransactionCallback onCompleteCallback, String packageName) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 3, packageName) { // from class: com.android.server.location.contexthub.ContextHubTransactionManager.4
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.disableNanoapp(contextHubId, nanoAppId, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to disable nanoapp with ID 0x" + Long.toHexString(nanoAppId), e);
                    return 1;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onTransactionComplete(int result) {
                try {
                    onCompleteCallback.onTransactionComplete(result);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubServiceTransaction createQueryTransaction(final int contextHubId, final IContextHubTransactionCallback onCompleteCallback, String packageName) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 4, packageName) { // from class: com.android.server.location.contexthub.ContextHubTransactionManager.5
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.queryNanoapps(contextHubId);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to query for nanoapps", e);
                    return 1;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onTransactionComplete(int result) {
                onQueryResponse(result, Collections.emptyList());
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.location.contexthub.ContextHubServiceTransaction
            public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
                try {
                    onCompleteCallback.onQueryResponse(result, nanoAppStateList);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onQueryComplete", e);
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addTransaction(ContextHubServiceTransaction transaction) throws IllegalStateException {
        if (this.mTransactionQueue.size() == 10000) {
            throw new IllegalStateException("Transaction queue is full (capacity = 10000)");
        }
        this.mTransactionQueue.add(transaction);
        this.mTransactionRecordDeque.add(new TransactionRecord(transaction.toString()));
        if (this.mTransactionQueue.size() == 1) {
            startNextTransaction();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onTransactionResponse(int transactionId, boolean success) {
        ContextHubServiceTransaction transaction = this.mTransactionQueue.peek();
        if (transaction == null) {
            Log.w(TAG, "Received unexpected transaction response (no transaction pending)");
        } else if (transaction.getTransactionId() != transactionId) {
            Log.w(TAG, "Received unexpected transaction response (expected ID = " + transaction.getTransactionId() + ", received ID = " + transactionId + ")");
        } else {
            transaction.onTransactionComplete(success ? 0 : 5);
            removeTransactionAndStartNext();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onQueryResponse(List<NanoAppState> nanoAppStateList) {
        ContextHubServiceTransaction transaction = this.mTransactionQueue.peek();
        if (transaction == null) {
            Log.w(TAG, "Received unexpected query response (no transaction pending)");
        } else if (transaction.getTransactionType() != 4) {
            Log.w(TAG, "Received unexpected query response (expected " + transaction + ")");
        } else {
            transaction.onQueryResponse(0, nanoAppStateList);
            removeTransactionAndStartNext();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onHubReset() {
        ContextHubServiceTransaction transaction = this.mTransactionQueue.peek();
        if (transaction == null) {
            return;
        }
        removeTransactionAndStartNext();
    }

    private void removeTransactionAndStartNext() {
        this.mTimeoutFuture.cancel(false);
        ContextHubServiceTransaction transaction = this.mTransactionQueue.remove();
        transaction.setComplete();
        if (!this.mTransactionQueue.isEmpty()) {
            startNextTransaction();
        }
    }

    private void startNextTransaction() {
        int result = 1;
        while (result != 0 && !this.mTransactionQueue.isEmpty()) {
            final ContextHubServiceTransaction transaction = this.mTransactionQueue.peek();
            result = transaction.onTransact();
            if (result == 0) {
                Runnable onTimeoutFunc = new Runnable() { // from class: com.android.server.location.contexthub.ContextHubTransactionManager$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContextHubTransactionManager.this.m4315x274bb8d2(transaction);
                    }
                };
                long timeoutSeconds = transaction.getTimeout(TimeUnit.SECONDS);
                try {
                    this.mTimeoutFuture = this.mTimeoutExecutor.schedule(onTimeoutFunc, timeoutSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "Error when schedule a timer", e);
                }
            } else {
                transaction.onTransactionComplete(ContextHubServiceUtil.toTransactionResult(result));
                this.mTransactionQueue.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startNextTransaction$0$com-android-server-location-contexthub-ContextHubTransactionManager  reason: not valid java name */
    public /* synthetic */ void m4315x274bb8d2(ContextHubServiceTransaction transaction) {
        synchronized (this) {
            if (!transaction.isComplete()) {
                Log.d(TAG, transaction + " timed out");
                transaction.onTransactionComplete(6);
                removeTransactionAndStartNext();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int toStatsTransactionResult(int result) {
        switch (result) {
            case 0:
                return 0;
            case 1:
            default:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
        }
    }

    public String toString() {
        TransactionRecord[] arr;
        StringBuilder sb = new StringBuilder(100);
        synchronized (this) {
            arr = (TransactionRecord[]) this.mTransactionQueue.toArray(new TransactionRecord[0]);
        }
        for (int i = 0; i < arr.length; i++) {
            sb.append(i + ": " + arr[i] + "\n");
        }
        sb.append("Transaction History:\n");
        Iterator<TransactionRecord> iterator = this.mTransactionRecordDeque.descendingIterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next() + "\n");
        }
        return sb.toString();
    }
}
