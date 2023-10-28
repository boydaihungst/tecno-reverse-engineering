package com.android.server.backup.transport;

import android.app.backup.RestoreDescription;
import android.app.backup.RestoreSet;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.infra.AndroidFuture;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/* loaded from: classes.dex */
public class BackupTransportClient {
    private static final String TAG = "BackupTransportClient";
    private final IBackupTransport mTransportBinder;
    private final TransportStatusCallbackPool mCallbackPool = new TransportStatusCallbackPool();
    private final TransportFutures mTransportFutures = new TransportFutures();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BackupTransportClient(IBackupTransport transportBinder) {
        this.mTransportBinder = transportBinder;
    }

    public String name() throws RemoteException {
        AndroidFuture<String> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.name(resultFuture);
        return (String) getFutureResult(resultFuture);
    }

    public Intent configurationIntent() throws RemoteException {
        AndroidFuture<Intent> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.configurationIntent(resultFuture);
        return (Intent) getFutureResult(resultFuture);
    }

    public String currentDestinationString() throws RemoteException {
        AndroidFuture<String> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.currentDestinationString(resultFuture);
        return (String) getFutureResult(resultFuture);
    }

    public Intent dataManagementIntent() throws RemoteException {
        AndroidFuture<Intent> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.dataManagementIntent(resultFuture);
        return (Intent) getFutureResult(resultFuture);
    }

    public CharSequence dataManagementIntentLabel() throws RemoteException {
        AndroidFuture<CharSequence> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.dataManagementIntentLabel(resultFuture);
        return (CharSequence) getFutureResult(resultFuture);
    }

    public String transportDirName() throws RemoteException {
        AndroidFuture<String> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.transportDirName(resultFuture);
        return (String) getFutureResult(resultFuture);
    }

    public int initializeDevice() throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.initializeDevice(callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int clearBackupData(PackageInfo packageInfo) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.clearBackupData(packageInfo, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int finishBackup() throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.finishBackup(callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public long requestBackupTime() throws RemoteException {
        AndroidFuture<Long> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.requestBackupTime(resultFuture);
        Long result = (Long) getFutureResult(resultFuture);
        if (result == null) {
            return -1000L;
        }
        return result.longValue();
    }

    public int performBackup(PackageInfo packageInfo, ParcelFileDescriptor inFd, int flags) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.performBackup(packageInfo, inFd, flags, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public RestoreSet[] getAvailableRestoreSets() throws RemoteException {
        AndroidFuture<List<RestoreSet>> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.getAvailableRestoreSets(resultFuture);
        List<RestoreSet> result = (List) getFutureResult(resultFuture);
        if (result == null) {
            return null;
        }
        return (RestoreSet[]) result.toArray(new RestoreSet[0]);
    }

    public long getCurrentRestoreSet() throws RemoteException {
        AndroidFuture<Long> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.getCurrentRestoreSet(resultFuture);
        Long result = (Long) getFutureResult(resultFuture);
        if (result == null) {
            return -1000L;
        }
        return result.longValue();
    }

    public int startRestore(long token, PackageInfo[] packages) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.startRestore(token, packages, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public RestoreDescription nextRestorePackage() throws RemoteException {
        AndroidFuture<RestoreDescription> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.nextRestorePackage(resultFuture);
        return (RestoreDescription) getFutureResult(resultFuture);
    }

    public int getRestoreData(ParcelFileDescriptor outFd) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.getRestoreData(outFd, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public void finishRestore() throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.finishRestore(callback);
            callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public long requestFullBackupTime() throws RemoteException {
        AndroidFuture<Long> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.requestFullBackupTime(resultFuture);
        Long result = (Long) getFutureResult(resultFuture);
        if (result == null) {
            return -1000L;
        }
        return result.longValue();
    }

    public int performFullBackup(PackageInfo targetPackage, ParcelFileDescriptor socket, int flags) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.performFullBackup(targetPackage, socket, flags, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int checkFullBackupSize(long size) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.checkFullBackupSize(size, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int sendBackupData(int numBytes) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        this.mTransportBinder.sendBackupData(numBytes, callback);
        try {
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public void cancelFullBackup() throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.cancelFullBackup(callback);
            callback.onOperationComplete();
            callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public boolean isAppEligibleForBackup(PackageInfo targetPackage, boolean isFullBackup) throws RemoteException {
        AndroidFuture<Boolean> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.isAppEligibleForBackup(targetPackage, isFullBackup, resultFuture);
        Boolean result = (Boolean) getFutureResult(resultFuture);
        return result != null && result.booleanValue();
    }

    public long getBackupQuota(String packageName, boolean isFullBackup) throws RemoteException {
        AndroidFuture<Long> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.getBackupQuota(packageName, isFullBackup, resultFuture);
        Long result = (Long) getFutureResult(resultFuture);
        if (result == null) {
            return -1000L;
        }
        return result.longValue();
    }

    public int getNextFullRestoreDataChunk(ParcelFileDescriptor socket) throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.getNextFullRestoreDataChunk(socket, callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int abortFullRestore() throws RemoteException {
        TransportStatusCallback callback = this.mCallbackPool.acquire();
        try {
            this.mTransportBinder.abortFullRestore(callback);
            return callback.getOperationStatus();
        } finally {
            this.mCallbackPool.recycle(callback);
        }
    }

    public int getTransportFlags() throws RemoteException {
        AndroidFuture<Integer> resultFuture = this.mTransportFutures.newFuture();
        this.mTransportBinder.getTransportFlags(resultFuture);
        Integer result = (Integer) getFutureResult(resultFuture);
        if (result == null) {
            return -1000;
        }
        return result.intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBecomingUnusable() {
        this.mCallbackPool.cancelActiveCallbacks();
        this.mTransportFutures.cancelActiveFutures();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [385=4] */
    private <T> T getFutureResult(AndroidFuture<T> future) {
        try {
            return (T) future.get(600L, TimeUnit.SECONDS);
        } catch (InterruptedException | CancellationException | ExecutionException | TimeoutException e) {
            Slog.w(TAG, "Failed to get result from transport:", e);
            return null;
        } finally {
            this.mTransportFutures.remove(future);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TransportFutures {
        private final Set<AndroidFuture<?>> mActiveFutures;
        private final Object mActiveFuturesLock;

        private TransportFutures() {
            this.mActiveFuturesLock = new Object();
            this.mActiveFutures = new HashSet();
        }

        <T> AndroidFuture<T> newFuture() {
            AndroidFuture<T> future = new AndroidFuture<>();
            synchronized (this.mActiveFuturesLock) {
                this.mActiveFutures.add(future);
            }
            return future;
        }

        <T> void remove(AndroidFuture<T> future) {
            synchronized (this.mActiveFuturesLock) {
                this.mActiveFutures.remove(future);
            }
        }

        void cancelActiveFutures() {
            synchronized (this.mActiveFuturesLock) {
                for (AndroidFuture<?> future : this.mActiveFutures) {
                    try {
                        future.cancel(true);
                    } catch (CancellationException e) {
                    }
                }
                this.mActiveFutures.clear();
            }
        }
    }

    /* loaded from: classes.dex */
    private static class TransportStatusCallbackPool {
        private static final int MAX_POOL_SIZE = 100;
        private final Set<TransportStatusCallback> mActiveCallbacks;
        private final Queue<TransportStatusCallback> mCallbackPool;
        private final Object mPoolLock;

        private TransportStatusCallbackPool() {
            this.mPoolLock = new Object();
            this.mCallbackPool = new ArrayDeque();
            this.mActiveCallbacks = new HashSet();
        }

        TransportStatusCallback acquire() {
            TransportStatusCallback callback;
            synchronized (this.mPoolLock) {
                callback = this.mCallbackPool.poll();
                if (callback == null) {
                    callback = new TransportStatusCallback();
                }
                callback.reset();
                this.mActiveCallbacks.add(callback);
            }
            return callback;
        }

        void recycle(TransportStatusCallback callback) {
            synchronized (this.mPoolLock) {
                this.mActiveCallbacks.remove(callback);
                if (this.mCallbackPool.size() > 100) {
                    Slog.d(BackupTransportClient.TAG, "TransportStatusCallback pool size exceeded");
                } else {
                    this.mCallbackPool.add(callback);
                }
            }
        }

        void cancelActiveCallbacks() {
            synchronized (this.mPoolLock) {
                for (TransportStatusCallback callback : this.mActiveCallbacks) {
                    try {
                        callback.onOperationCompleteWithStatus(-1000);
                        callback.getOperationStatus();
                    } catch (RemoteException e) {
                    }
                    if (this.mCallbackPool.size() < 100) {
                        this.mCallbackPool.add(callback);
                    }
                }
                this.mActiveCallbacks.clear();
            }
        }
    }
}
