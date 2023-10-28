package android.net;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkScore;
import android.os.Looper;
import android.os.Message;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NetworkFactoryImpl extends NetworkFactoryLegacyImpl {
    private static final int CMD_CANCEL_REQUEST = 2;
    private static final int CMD_LISTEN_TO_ALL_REQUESTS = 6;
    private static final int CMD_OFFER_NETWORK = 5;
    private static final int CMD_REQUEST_NETWORK = 1;
    private static final int CMD_SET_FILTER = 4;
    private static final int CMD_SET_SCORE = 3;
    private static final boolean DBG = true;
    private static final NetworkScore INVINCIBLE_SCORE = new NetworkScore.Builder().setLegacyInt(1000).build();
    private static final boolean VDBG = false;
    private final Executor mExecutor;
    private final Map<NetworkRequest, NetworkRequestInfo> mNetworkRequests;
    private final NetworkProvider.NetworkOfferCallback mRequestCallback;
    private NetworkScore mScore;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-net-NetworkFactoryImpl  reason: not valid java name */
    public /* synthetic */ void m16lambda$new$0$androidnetNetworkFactoryImpl(Runnable command) {
        post(command);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkFactoryImpl(NetworkFactory parent, Looper looper, Context context, NetworkCapabilities filter) {
        super(parent, looper, context, filter != null ? filter : NetworkCapabilities.Builder.withoutDefaultCapabilities().build());
        this.mNetworkRequests = new LinkedHashMap();
        this.mScore = new NetworkScore.Builder().setLegacyInt(0).build();
        this.mRequestCallback = new NetworkProvider.NetworkOfferCallback() { // from class: android.net.NetworkFactoryImpl.1
            public void onNetworkNeeded(NetworkRequest request) {
                NetworkFactoryImpl.this.handleAddRequest(request);
            }

            public void onNetworkUnneeded(NetworkRequest request) {
                NetworkFactoryImpl.this.handleRemoveRequest(request);
            }
        };
        this.mExecutor = new Executor() { // from class: android.net.NetworkFactoryImpl$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                NetworkFactoryImpl.this.m16lambda$new$0$androidnetNetworkFactoryImpl(runnable);
            }
        };
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public void register(String logTag) {
        register(logTag, false);
    }

    @Override // android.net.NetworkFactoryShim
    public void registerIgnoringScore(String logTag) {
        register(logTag, true);
    }

    private void register(String logTag, boolean listenToAllRequests) {
        if (this.mProvider != null) {
            throw new IllegalStateException("A NetworkFactory must only be registered once");
        }
        this.mParent.log("Registering NetworkFactory");
        this.mProvider = new NetworkProvider(this.mContext, getLooper(), logTag) { // from class: android.net.NetworkFactoryImpl.2
            public void onNetworkRequested(NetworkRequest request, int score, int servingProviderId) {
                NetworkFactoryImpl.this.handleAddRequest(request);
            }

            public void onNetworkRequestWithdrawn(NetworkRequest request) {
                NetworkFactoryImpl.this.handleRemoveRequest(request);
            }
        };
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).registerNetworkProvider(this.mProvider);
        if (listenToAllRequests) {
            sendMessage(obtainMessage(6));
        } else {
            sendMessage(obtainMessage(5));
        }
    }

    private void handleOfferNetwork(NetworkScore score) {
        this.mProvider.registerNetworkOffer(score, this.mCapabilityFilter, this.mExecutor, this.mRequestCallback);
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                handleAddRequest((NetworkRequest) msg.obj);
                return;
            case 2:
                handleRemoveRequest((NetworkRequest) msg.obj);
                return;
            case 3:
                handleSetScore((NetworkScore) msg.obj);
                return;
            case 4:
                handleSetFilter((NetworkCapabilities) msg.obj);
                return;
            case 5:
                handleOfferNetwork(this.mScore);
                return;
            case 6:
                handleOfferNetwork(INVINCIBLE_SCORE);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NetworkRequestInfo {
        public final NetworkRequest request;
        public boolean requested = false;

        NetworkRequestInfo(NetworkRequest request) {
            this.request = request;
        }

        public String toString() {
            return "{" + this.request + ", requested=" + this.requested + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAddRequest(NetworkRequest request) {
        NetworkRequestInfo n = this.mNetworkRequests.get(request);
        if (n == null) {
            this.mParent.log("got request " + request);
            n = new NetworkRequestInfo(request);
            this.mNetworkRequests.put(n.request, n);
        }
        if (this.mParent.acceptRequest(request)) {
            n.requested = true;
            this.mParent.needNetworkFor(request);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemoveRequest(NetworkRequest request) {
        NetworkRequestInfo n = this.mNetworkRequests.get(request);
        if (n != null) {
            this.mNetworkRequests.remove(request);
            if (n.requested) {
                this.mParent.releaseNetworkFor(n.request);
            }
        }
    }

    private void handleSetScore(NetworkScore score) {
        if (this.mScore.equals(score)) {
            return;
        }
        this.mScore = score;
        this.mParent.reevaluateAllRequests();
    }

    private void handleSetFilter(NetworkCapabilities netCap) {
        if (netCap.equals(this.mCapabilityFilter)) {
            return;
        }
        this.mCapabilityFilter = netCap;
        this.mParent.reevaluateAllRequests();
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public final void reevaluateAllRequests() {
        if (this.mProvider == null) {
            return;
        }
        this.mProvider.registerNetworkOffer(this.mScore, this.mCapabilityFilter, this.mExecutor, this.mRequestCallback);
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    @Deprecated
    public void setScoreFilter(int score) {
        setScoreFilter(new NetworkScore.Builder().setLegacyInt(score).build());
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public void setScoreFilter(NetworkScore score) {
        sendMessage(obtainMessage(3, score));
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public void setCapabilityFilter(NetworkCapabilities netCap) {
        sendMessage(obtainMessage(4, new NetworkCapabilities(netCap)));
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public int getRequestCount() {
        return this.mNetworkRequests.size();
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.net.NetworkFactoryShim
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(toString());
        for (NetworkRequestInfo n : this.mNetworkRequests.values()) {
            writer.println("  " + n);
        }
    }

    @Override // android.net.NetworkFactoryLegacyImpl, android.os.Handler
    public String toString() {
        return "providerId=" + (this.mProvider != null ? Integer.valueOf(this.mProvider.getProviderId()) : "null") + ", ScoreFilter=" + this.mScore + ", Filter=" + this.mCapabilityFilter + ", requests=" + this.mNetworkRequests.size();
    }
}
