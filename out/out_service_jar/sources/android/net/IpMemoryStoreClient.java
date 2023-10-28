package android.net;

import android.content.Context;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.NetworkAttributes;
import android.net.ipmemorystore.OnBlobRetrievedListener;
import android.net.ipmemorystore.OnDeleteStatusListener;
import android.net.ipmemorystore.OnL2KeyResponseListener;
import android.net.ipmemorystore.OnNetworkAttributesRetrievedListener;
import android.net.ipmemorystore.OnSameL3NetworkResponseListener;
import android.net.ipmemorystore.OnStatusListener;
import android.net.ipmemorystore.Status;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public abstract class IpMemoryStoreClient {
    private static final String TAG = IpMemoryStoreClient.class.getSimpleName();
    private final Context mContext;

    /* JADX INFO: Access modifiers changed from: private */
    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface ThrowingRunnable {
        void run() throws RemoteException;
    }

    protected abstract void runWhenServiceReady(Consumer<IIpMemoryStore> consumer) throws ExecutionException;

    public IpMemoryStoreClient(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("missing context");
        }
        this.mContext = context;
    }

    private void ignoringRemoteException(ThrowingRunnable r) {
        ignoringRemoteException("Failed to execute remote procedure call", r);
    }

    private void ignoringRemoteException(String message, ThrowingRunnable r) {
        try {
            r.run();
        } catch (RemoteException e) {
            Log.e(TAG, message, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$storeNetworkAttributes$1$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m12lambda$storeNetworkAttributes$1$androidnetIpMemoryStoreClient(final String l2Key, final NetworkAttributes attributes, final OnStatusListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda25
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.storeNetworkAttributes(l2Key, attributes.toParcelable(), OnStatusListener.toAIDL(listener));
            }
        });
    }

    public void storeNetworkAttributes(final String l2Key, final NetworkAttributes attributes, final OnStatusListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda21
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m12lambda$storeNetworkAttributes$1$androidnetIpMemoryStoreClient(l2Key, attributes, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            if (listener == null) {
                return;
            }
            ignoringRemoteException("Error storing network attributes", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda22
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnStatusListener.this.onComplete(new Status(-5));
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$storeBlob$4$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m11lambda$storeBlob$4$androidnetIpMemoryStoreClient(final String l2Key, final String clientId, final String name, final Blob data, final OnStatusListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda20
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.storeBlob(l2Key, clientId, name, data, OnStatusListener.toAIDL(listener));
            }
        });
    }

    public void storeBlob(final String l2Key, final String clientId, final String name, final Blob data, final OnStatusListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda17
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m11lambda$storeBlob$4$androidnetIpMemoryStoreClient(l2Key, clientId, name, data, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            if (listener == null) {
                return;
            }
            ignoringRemoteException("Error storing blob", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda18
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnStatusListener.this.onComplete(new Status(-5));
                }
            });
        }
    }

    public void findL2Key(final NetworkAttributes attributes, final OnL2KeyResponseListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m7lambda$findL2Key$7$androidnetIpMemoryStoreClient(attributes, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            ignoringRemoteException("Error finding L2 Key", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda5
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnL2KeyResponseListener.this.onL2KeyResponse(new Status(-5), null);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$findL2Key$7$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m7lambda$findL2Key$7$androidnetIpMemoryStoreClient(final NetworkAttributes attributes, final OnL2KeyResponseListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda15
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.findL2Key(attributes.toParcelable(), OnL2KeyResponseListener.toAIDL(listener));
            }
        });
    }

    public void isSameNetwork(final String l2Key1, final String l2Key2, final OnSameL3NetworkResponseListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda9
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m8lambda$isSameNetwork$10$androidnetIpMemoryStoreClient(l2Key1, l2Key2, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            ignoringRemoteException("Error checking for network sameness", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda10
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnSameL3NetworkResponseListener.this.onSameL3NetworkResponse(new Status(-5), null);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isSameNetwork$10$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m8lambda$isSameNetwork$10$androidnetIpMemoryStoreClient(final String l2Key1, final String l2Key2, final OnSameL3NetworkResponseListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda6
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.isSameNetwork(l2Key1, l2Key2, OnSameL3NetworkResponseListener.toAIDL(listener));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$retrieveNetworkAttributes$13$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m10x98d9d2ac(final String l2Key, final OnNetworkAttributesRetrievedListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda3
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.retrieveNetworkAttributes(l2Key, OnNetworkAttributesRetrievedListener.toAIDL(listener));
            }
        });
    }

    public void retrieveNetworkAttributes(final String l2Key, final OnNetworkAttributesRetrievedListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m10x98d9d2ac(l2Key, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            ignoringRemoteException("Error retrieving network attributes", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda8
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnNetworkAttributesRetrievedListener.this.onNetworkAttributesRetrieved(new Status(-5), null, null);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$retrieveBlob$16$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m9lambda$retrieveBlob$16$androidnetIpMemoryStoreClient(final String l2Key, final String clientId, final String name, final OnBlobRetrievedListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda11
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.retrieveBlob(l2Key, clientId, name, OnBlobRetrievedListener.toAIDL(listener));
            }
        });
    }

    public void retrieveBlob(final String l2Key, final String clientId, final String name, final OnBlobRetrievedListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda23
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m9lambda$retrieveBlob$16$androidnetIpMemoryStoreClient(l2Key, clientId, name, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            ignoringRemoteException("Error retrieving blob", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda24
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnBlobRetrievedListener.this.onBlobRetrieved(new Status(-5), null, null, null);
                }
            });
        }
    }

    public void delete(final String l2Key, final boolean needWipe, final OnDeleteStatusListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda12
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m4lambda$delete$19$androidnetIpMemoryStoreClient(l2Key, needWipe, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            if (listener == null) {
                return;
            }
            ignoringRemoteException("Error deleting from the memory store", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda13
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnDeleteStatusListener.this.onComplete(new Status(-5), 0);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$delete$19$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m4lambda$delete$19$androidnetIpMemoryStoreClient(final String l2Key, final boolean needWipe, final OnDeleteStatusListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda0
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.delete(l2Key, needWipe, OnDeleteStatusListener.toAIDL(listener));
            }
        });
    }

    public void deleteCluster(final String cluster, final boolean needWipe, final OnDeleteStatusListener listener) {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m5lambda$deleteCluster$22$androidnetIpMemoryStoreClient(cluster, needWipe, listener, (IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException e) {
            if (listener == null) {
                return;
            }
            ignoringRemoteException("Error deleting from the memory store", new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda2
                @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
                public final void run() {
                    OnDeleteStatusListener.this.onComplete(new Status(-5), 0);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteCluster$22$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m5lambda$deleteCluster$22$androidnetIpMemoryStoreClient(final String cluster, final boolean needWipe, final OnDeleteStatusListener listener, final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda16
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.deleteCluster(cluster, needWipe, OnDeleteStatusListener.toAIDL(listener));
            }
        });
    }

    public void factoryReset() {
        try {
            runWhenServiceReady(new Consumer() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda14
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IpMemoryStoreClient.this.m6lambda$factoryReset$25$androidnetIpMemoryStoreClient((IIpMemoryStore) obj);
                }
            });
        } catch (ExecutionException m) {
            Log.e(TAG, "Error executing factory reset", m);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$factoryReset$25$android-net-IpMemoryStoreClient  reason: not valid java name */
    public /* synthetic */ void m6lambda$factoryReset$25$androidnetIpMemoryStoreClient(final IIpMemoryStore service) {
        ignoringRemoteException(new ThrowingRunnable() { // from class: android.net.IpMemoryStoreClient$$ExternalSyntheticLambda19
            @Override // android.net.IpMemoryStoreClient.ThrowingRunnable
            public final void run() {
                IIpMemoryStore.this.factoryReset();
            }
        });
    }
}
