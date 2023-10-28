package com.android.server.storage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageVolume;
import android.service.storage.IExternalStorageService;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.storage.StorageSessionController;
import com.android.server.storage.StorageUserConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes2.dex */
public final class StorageUserConnection {
    private static final int DEFAULT_REMOTE_TIMEOUT_SECONDS = 20;
    private static final String TAG = "StorageUserConnection";
    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private final StorageSessionController mSessionController;
    private final StorageManagerInternal mSmInternal;
    private final int mUserId;
    private final Object mSessionsLock = new Object();
    private final ActiveConnection mActiveConnection = new ActiveConnection();
    private final Map<String, Session> mSessions = new HashMap();
    private final SparseArray<Integer> mUidsBlockedOnIo = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface AsyncStorageServiceCall {
        void run(IExternalStorageService iExternalStorageService, RemoteCallback remoteCallback) throws RemoteException;
    }

    public StorageUserConnection(Context context, int userId, StorageSessionController controller) {
        this.mContext = (Context) Objects.requireNonNull(context);
        int checkArgumentNonnegative = Preconditions.checkArgumentNonnegative(userId);
        this.mUserId = checkArgumentNonnegative;
        this.mSessionController = controller;
        this.mSmInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
        HandlerThread handlerThread = new HandlerThread("StorageUserConnectionThread-" + checkArgumentNonnegative);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
    }

    public void startSession(String sessionId, ParcelFileDescriptor pfd, String upperPath, String lowerPath) throws StorageSessionController.ExternalStorageServiceException {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(pfd);
        Objects.requireNonNull(upperPath);
        Objects.requireNonNull(lowerPath);
        Session session = new Session(sessionId, upperPath, lowerPath);
        synchronized (this.mSessionsLock) {
            Preconditions.checkArgument(!this.mSessions.containsKey(sessionId));
            this.mSessions.put(sessionId, session);
        }
        this.mActiveConnection.startSession(session, pfd);
    }

    public void notifyVolumeStateChanged(String sessionId, StorageVolume vol) throws StorageSessionController.ExternalStorageServiceException {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(vol);
        synchronized (this.mSessionsLock) {
            if (!this.mSessions.containsKey(sessionId)) {
                Slog.i(TAG, "No session found for sessionId: " + sessionId);
            } else {
                this.mActiveConnection.notifyVolumeStateChanged(sessionId, vol);
            }
        }
    }

    public void freeCache(String volumeUuid, long bytes) throws StorageSessionController.ExternalStorageServiceException {
        synchronized (this.mSessionsLock) {
            for (String sessionId : this.mSessions.keySet()) {
                this.mActiveConnection.freeCache(sessionId, volumeUuid, bytes);
            }
        }
    }

    public void notifyAnrDelayStarted(String packageName, int uid, int tid, int reason) throws StorageSessionController.ExternalStorageServiceException {
        List<String> primarySessionIds = this.mSmInternal.getPrimaryVolumeIds();
        synchronized (this.mSessionsLock) {
            for (String sessionId : this.mSessions.keySet()) {
                if (primarySessionIds.contains(sessionId)) {
                    this.mActiveConnection.notifyAnrDelayStarted(packageName, uid, tid, reason);
                    return;
                }
            }
        }
    }

    public Session removeSession(String sessionId) {
        Session remove;
        synchronized (this.mSessionsLock) {
            this.mUidsBlockedOnIo.clear();
            remove = this.mSessions.remove(sessionId);
        }
        return remove;
    }

    public void removeSessionAndWait(String sessionId) throws StorageSessionController.ExternalStorageServiceException {
        Session session = removeSession(sessionId);
        if (session == null) {
            Slog.i(TAG, "No session found for id: " + sessionId);
            return;
        }
        Slog.i(TAG, "Waiting for session end " + session + " ...");
        this.mActiveConnection.endSession(session);
    }

    public void resetUserSessions() {
        synchronized (this.mSessionsLock) {
            if (this.mSessions.isEmpty()) {
                return;
            }
            this.mSmInternal.resetUser(this.mUserId);
        }
    }

    public void removeAllSessions() {
        synchronized (this.mSessionsLock) {
            Slog.i(TAG, "Removing  " + this.mSessions.size() + " sessions for user: " + this.mUserId + "...");
            this.mSessions.clear();
        }
    }

    public void close() {
        this.mActiveConnection.close();
        this.mHandlerThread.quit();
    }

    public Set<String> getAllSessionIds() {
        HashSet hashSet;
        synchronized (this.mSessionsLock) {
            hashSet = new HashSet(this.mSessions.keySet());
        }
        return hashSet;
    }

    public void notifyAppIoBlocked(String volumeUuid, int uid, int tid, int reason) {
        synchronized (this.mSessionsLock) {
            int ioBlockedCounter = this.mUidsBlockedOnIo.get(uid, 0).intValue();
            this.mUidsBlockedOnIo.put(uid, Integer.valueOf(ioBlockedCounter + 1));
        }
    }

    public void notifyAppIoResumed(String volumeUuid, int uid, int tid, int reason) {
        synchronized (this.mSessionsLock) {
            int ioBlockedCounter = this.mUidsBlockedOnIo.get(uid, 0).intValue();
            if (ioBlockedCounter == 0) {
                Slog.w(TAG, "Unexpected app IO resumption for uid: " + uid);
            }
            if (ioBlockedCounter <= 1) {
                this.mUidsBlockedOnIo.remove(uid);
            } else {
                this.mUidsBlockedOnIo.put(uid, Integer.valueOf(ioBlockedCounter - 1));
            }
        }
    }

    public boolean isAppIoBlocked(int uid) {
        boolean contains;
        synchronized (this.mSessionsLock) {
            contains = this.mUidsBlockedOnIo.contains(uid);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ActiveConnection implements AutoCloseable {
        private final Object mLock;
        private final ArrayList<CompletableFuture<Void>> mOutstandingOps;
        private CompletableFuture<IExternalStorageService> mRemoteFuture;
        private ServiceConnection mServiceConnection;

        private ActiveConnection() {
            this.mLock = new Object();
            this.mOutstandingOps = new ArrayList<>();
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            ServiceConnection oldConnection;
            synchronized (this.mLock) {
                Slog.i(StorageUserConnection.TAG, "Closing connection for user " + StorageUserConnection.this.mUserId);
                oldConnection = this.mServiceConnection;
                this.mServiceConnection = null;
                CompletableFuture<IExternalStorageService> completableFuture = this.mRemoteFuture;
                if (completableFuture != null) {
                    completableFuture.cancel(true);
                    this.mRemoteFuture = null;
                }
                Iterator<CompletableFuture<Void>> it = this.mOutstandingOps.iterator();
                while (it.hasNext()) {
                    CompletableFuture<Void> op = it.next();
                    op.cancel(true);
                }
                this.mOutstandingOps.clear();
            }
            if (oldConnection != null) {
                try {
                    StorageUserConnection.this.mContext.unbindService(oldConnection);
                } catch (Exception e) {
                    Slog.w(StorageUserConnection.TAG, "Failed to unbind service", e);
                }
            }
        }

        private void asyncBestEffort(Consumer<IExternalStorageService> consumer) {
            synchronized (this.mLock) {
                CompletableFuture<IExternalStorageService> completableFuture = this.mRemoteFuture;
                if (completableFuture == null) {
                    Slog.w(StorageUserConnection.TAG, "Dropping async request service is not bound");
                    return;
                }
                IExternalStorageService service = completableFuture.getNow(null);
                if (service == null) {
                    Slog.w(StorageUserConnection.TAG, "Dropping async request service is not connected");
                } else {
                    consumer.accept(service);
                }
            }
        }

        private void waitForAsyncVoid(AsyncStorageServiceCall asyncCall) throws Exception {
            final CompletableFuture<Void> opFuture = new CompletableFuture<>();
            RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda1
                public final void onResult(Bundle bundle) {
                    StorageUserConnection.ActiveConnection.this.m6717x7cb5457(opFuture, bundle);
                }
            });
            waitForAsync(asyncCall, callback, opFuture, this.mOutstandingOps, 20L);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [383=4] */
        /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
        private <T> T waitForAsync(final AsyncStorageServiceCall asyncCall, final RemoteCallback callback, final CompletableFuture<T> opFuture, ArrayList<CompletableFuture<T>> outstandingOps, long timeoutSeconds) throws Exception {
            CompletableFuture<IExternalStorageService> serviceFuture = connectIfNeeded();
            try {
                synchronized (this.mLock) {
                    outstandingOps.add(opFuture);
                }
                T t = (T) serviceFuture.thenCompose(new Function() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return StorageUserConnection.ActiveConnection.lambda$waitForAsync$1(StorageUserConnection.AsyncStorageServiceCall.this, callback, opFuture, (IExternalStorageService) obj);
                    }
                }).get(timeoutSeconds, TimeUnit.SECONDS);
                synchronized (this.mLock) {
                    outstandingOps.remove(opFuture);
                }
                return t;
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    outstandingOps.remove(opFuture);
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ CompletionStage lambda$waitForAsync$1(AsyncStorageServiceCall asyncCall, RemoteCallback callback, CompletableFuture opFuture, IExternalStorageService service) {
            try {
                asyncCall.run(service, callback);
            } catch (RemoteException e) {
                opFuture.completeExceptionally(e);
            }
            return opFuture;
        }

        public void startSession(final Session session, final ParcelFileDescriptor fd) throws StorageSessionController.ExternalStorageServiceException {
            try {
                try {
                    waitForAsyncVoid(new AsyncStorageServiceCall() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda5
                        @Override // com.android.server.storage.StorageUserConnection.AsyncStorageServiceCall
                        public final void run(IExternalStorageService iExternalStorageService, RemoteCallback remoteCallback) {
                            iExternalStorageService.startSession(r0.sessionId, 3, fd, r0.upperPath, StorageUserConnection.Session.this.lowerPath, remoteCallback);
                        }
                    });
                    try {
                        fd.close();
                    } catch (IOException e) {
                    }
                } catch (Exception e2) {
                    throw new StorageSessionController.ExternalStorageServiceException("Failed to start session: " + session, e2);
                }
            } catch (Throwable th) {
                try {
                    fd.close();
                } catch (IOException e3) {
                }
                throw th;
            }
        }

        public void endSession(final Session session) throws StorageSessionController.ExternalStorageServiceException {
            try {
                waitForAsyncVoid(new AsyncStorageServiceCall() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda6
                    @Override // com.android.server.storage.StorageUserConnection.AsyncStorageServiceCall
                    public final void run(IExternalStorageService iExternalStorageService, RemoteCallback remoteCallback) {
                        iExternalStorageService.endSession(StorageUserConnection.Session.this.sessionId, remoteCallback);
                    }
                });
            } catch (Exception e) {
                throw new StorageSessionController.ExternalStorageServiceException("Failed to end session: " + session, e);
            }
        }

        public void notifyVolumeStateChanged(final String sessionId, final StorageVolume vol) throws StorageSessionController.ExternalStorageServiceException {
            try {
                waitForAsyncVoid(new AsyncStorageServiceCall() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda3
                    @Override // com.android.server.storage.StorageUserConnection.AsyncStorageServiceCall
                    public final void run(IExternalStorageService iExternalStorageService, RemoteCallback remoteCallback) {
                        iExternalStorageService.notifyVolumeStateChanged(sessionId, vol, remoteCallback);
                    }
                });
            } catch (Exception e) {
                throw new StorageSessionController.ExternalStorageServiceException("Failed to notify volume state changed for vol : " + vol, e);
            }
        }

        public void freeCache(final String sessionId, final String volumeUuid, final long bytes) throws StorageSessionController.ExternalStorageServiceException {
            try {
                waitForAsyncVoid(new AsyncStorageServiceCall() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda2
                    @Override // com.android.server.storage.StorageUserConnection.AsyncStorageServiceCall
                    public final void run(IExternalStorageService iExternalStorageService, RemoteCallback remoteCallback) {
                        iExternalStorageService.freeCache(sessionId, volumeUuid, bytes, remoteCallback);
                    }
                });
            } catch (Exception e) {
                throw new StorageSessionController.ExternalStorageServiceException("Failed to free " + bytes + " bytes for volumeUuid : " + volumeUuid, e);
            }
        }

        public void notifyAnrDelayStarted(final String packgeName, final int uid, final int tid, final int reason) throws StorageSessionController.ExternalStorageServiceException {
            asyncBestEffort(new Consumer() { // from class: com.android.server.storage.StorageUserConnection$ActiveConnection$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    StorageUserConnection.ActiveConnection.lambda$notifyAnrDelayStarted$6(packgeName, uid, tid, reason, (IExternalStorageService) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$notifyAnrDelayStarted$6(String packgeName, int uid, int tid, int reason, IExternalStorageService service) {
            try {
                service.notifyAnrDelayStarted(packgeName, uid, tid, reason);
            } catch (RemoteException e) {
                Slog.w(StorageUserConnection.TAG, "Failed to notify ANR delay started", e);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: setResult */
        public void m6717x7cb5457(Bundle result, CompletableFuture<Void> future) {
            ParcelableException ex = result.getParcelable("android.service.storage.extra.error");
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                future.complete(null);
            }
        }

        private CompletableFuture<IExternalStorageService> connectIfNeeded() throws StorageSessionController.ExternalStorageServiceException {
            ComponentName name = StorageUserConnection.this.mSessionController.getExternalStorageServiceComponentName();
            if (name == null) {
                throw new StorageSessionController.ExternalStorageServiceException("Not ready to bind to the ExternalStorageService for user " + StorageUserConnection.this.mUserId);
            }
            synchronized (this.mLock) {
                CompletableFuture<IExternalStorageService> completableFuture = this.mRemoteFuture;
                if (completableFuture != null) {
                    return completableFuture;
                }
                final CompletableFuture<IExternalStorageService> future = new CompletableFuture<>();
                this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.storage.StorageUserConnection.ActiveConnection.1
                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName name2, IBinder service) {
                        Slog.i(StorageUserConnection.TAG, "Service: [" + name2 + "] connected. User [" + StorageUserConnection.this.mUserId + "]");
                        handleConnection(service);
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName name2) {
                        Slog.i(StorageUserConnection.TAG, "Service: [" + name2 + "] disconnected. User [" + StorageUserConnection.this.mUserId + "]");
                        handleDisconnection();
                    }

                    @Override // android.content.ServiceConnection
                    public void onBindingDied(ComponentName name2) {
                        Slog.i(StorageUserConnection.TAG, "Service: [" + name2 + "] died. User [" + StorageUserConnection.this.mUserId + "]");
                        handleDisconnection();
                    }

                    @Override // android.content.ServiceConnection
                    public void onNullBinding(ComponentName name2) {
                        Slog.wtf(StorageUserConnection.TAG, "Service: [" + name2 + "] is null. User [" + StorageUserConnection.this.mUserId + "]");
                    }

                    private void handleConnection(IBinder service) {
                        synchronized (ActiveConnection.this.mLock) {
                            future.complete(IExternalStorageService.Stub.asInterface(service));
                        }
                    }

                    private void handleDisconnection() {
                        ActiveConnection.this.close();
                        StorageUserConnection.this.resetUserSessions();
                    }
                };
                Slog.i(StorageUserConnection.TAG, "Binding to the ExternalStorageService for user " + StorageUserConnection.this.mUserId);
                if (StorageUserConnection.this.mContext.bindServiceAsUser(new Intent().setComponent(name), this.mServiceConnection, 65, StorageUserConnection.this.mHandlerThread.getThreadHandler(), UserHandle.of(StorageUserConnection.this.mUserId))) {
                    Slog.i(StorageUserConnection.TAG, "Bound to the ExternalStorageService for user " + StorageUserConnection.this.mUserId);
                    this.mRemoteFuture = future;
                    return future;
                }
                throw new StorageSessionController.ExternalStorageServiceException("Failed to bind to the ExternalStorageService for user " + StorageUserConnection.this.mUserId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class Session {
        public final String lowerPath;
        public final String sessionId;
        public final String upperPath;

        Session(String sessionId, String upperPath, String lowerPath) {
            this.sessionId = sessionId;
            this.upperPath = upperPath;
            this.lowerPath = lowerPath;
        }

        public String toString() {
            return "[SessionId: " + this.sessionId + ". UpperPath: " + this.upperPath + ". LowerPath: " + this.lowerPath + "]";
        }
    }
}
