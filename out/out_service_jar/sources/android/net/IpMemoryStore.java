package android.net;

import android.content.Context;
import android.net.IIpMemoryStoreCallbacks;
import android.net.networkstack.ModuleNetworkStackClient;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
/* loaded from: classes.dex */
public class IpMemoryStore extends IpMemoryStoreClient {
    private static final String TAG = IpMemoryStore.class.getSimpleName();
    private final CompletableFuture<IIpMemoryStore> mService;
    private final AtomicReference<CompletableFuture<IIpMemoryStore>> mTailNode;

    public IpMemoryStore(Context context) {
        super(context);
        CompletableFuture<IIpMemoryStore> completableFuture = new CompletableFuture<>();
        this.mService = completableFuture;
        this.mTailNode = new AtomicReference<>(completableFuture);
        getModuleNetworkStackClient(context).fetchIpMemoryStore(new IIpMemoryStoreCallbacks.Stub() { // from class: android.net.IpMemoryStore.1
            @Override // android.net.IIpMemoryStoreCallbacks
            public void onIpMemoryStoreFetched(IIpMemoryStore memoryStore) {
                IpMemoryStore.this.mService.complete(memoryStore);
            }

            @Override // android.net.IIpMemoryStoreCallbacks
            public int getInterfaceVersion() {
                return 10;
            }

            @Override // android.net.IIpMemoryStoreCallbacks
            public String getInterfaceHash() {
                return "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
            }
        });
    }

    @Override // android.net.IpMemoryStoreClient
    protected void runWhenServiceReady(final Consumer<IIpMemoryStore> cb) throws ExecutionException {
        this.mTailNode.getAndUpdate(new UnaryOperator() { // from class: android.net.IpMemoryStore$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                CompletableFuture handle;
                handle = ((CompletableFuture) obj).handle(new BiFunction() { // from class: android.net.IpMemoryStore$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj2, Object obj3) {
                        return IpMemoryStore.lambda$runWhenServiceReady$0(r1, (IIpMemoryStore) obj2, (Throwable) obj3);
                    }
                });
                return handle;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ IIpMemoryStore lambda$runWhenServiceReady$0(Consumer cb, IIpMemoryStore store, Throwable exception) {
        if (exception != null) {
            Log.wtf(TAG, "Error fetching IpMemoryStore", exception);
            return store;
        }
        try {
            cb.accept(store);
        } catch (Exception e) {
            Log.wtf(TAG, "Exception occurred: " + e.getMessage());
        }
        return store;
    }

    protected ModuleNetworkStackClient getModuleNetworkStackClient(Context context) {
        return ModuleNetworkStackClient.getInstance(context);
    }

    public static IpMemoryStore getMemoryStore(Context context) {
        return new IpMemoryStore(context);
    }
}
