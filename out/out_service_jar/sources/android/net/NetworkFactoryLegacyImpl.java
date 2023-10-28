package android.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NetworkFactoryLegacyImpl extends Handler implements NetworkFactoryShim {
    public static final int CMD_CANCEL_REQUEST = 2;
    public static final int CMD_REQUEST_NETWORK = 1;
    private static final int CMD_SET_FILTER = 4;
    private static final int CMD_SET_SCORE = 3;
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    NetworkCapabilities mCapabilityFilter;
    final Context mContext;
    private final Map<NetworkRequest, NetworkRequestInfo> mNetworkRequests;
    final NetworkFactory mParent;
    NetworkProvider mProvider;
    private int mScore;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkFactoryLegacyImpl(NetworkFactory parent, Looper looper, Context context, NetworkCapabilities filter) {
        super(looper);
        this.mNetworkRequests = new LinkedHashMap();
        this.mProvider = null;
        this.mParent = parent;
        this.mContext = context;
        this.mCapabilityFilter = filter;
    }

    public void register(String logTag) {
        if (this.mProvider != null) {
            throw new IllegalStateException("A NetworkFactory must only be registered once");
        }
        this.mParent.log("Registering NetworkFactory");
        this.mProvider = new NetworkProvider(this.mContext, getLooper(), logTag) { // from class: android.net.NetworkFactoryLegacyImpl.1
            public void onNetworkRequested(NetworkRequest request, int score, int servingProviderId) {
                NetworkFactoryLegacyImpl.this.handleAddRequest(request, score, servingProviderId);
            }

            public void onNetworkRequestWithdrawn(NetworkRequest request) {
                NetworkFactoryLegacyImpl.this.handleRemoveRequest(request);
            }
        };
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).registerNetworkProvider(this.mProvider);
    }

    @Override // android.net.NetworkFactoryShim
    public void terminate() {
        if (this.mProvider == null) {
            throw new IllegalStateException("This NetworkFactory was never registered");
        }
        this.mParent.log("Unregistering NetworkFactory");
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).unregisterNetworkProvider(this.mProvider);
        removeCallbacksAndMessages(null);
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                handleAddRequest((NetworkRequest) msg.obj, msg.arg1, msg.arg2);
                return;
            case 2:
                handleRemoveRequest((NetworkRequest) msg.obj);
                return;
            case 3:
                handleSetScore(msg.arg1);
                return;
            case 4:
                handleSetFilter((NetworkCapabilities) msg.obj);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NetworkRequestInfo {
        public int providerId;
        public final NetworkRequest request;
        public boolean requested = false;
        public int score;

        NetworkRequestInfo(NetworkRequest request, int score, int providerId) {
            this.request = request;
            this.score = score;
            this.providerId = providerId;
        }

        public String toString() {
            return "{" + this.request + ", score=" + this.score + ", requested=" + this.requested + "}";
        }
    }

    protected void handleAddRequest(NetworkRequest request, int score, int servingProviderId) {
        NetworkRequestInfo n = this.mNetworkRequests.get(request);
        if (n == null) {
            this.mParent.log("got request " + request + " with score " + score + " and providerId " + servingProviderId);
            n = new NetworkRequestInfo(request, score, servingProviderId);
            this.mNetworkRequests.put(n.request, n);
        } else {
            n.score = score;
            n.providerId = servingProviderId;
        }
        evalRequest(n);
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

    private void handleSetScore(int score) {
        this.mScore = score;
        evalRequests();
    }

    private void handleSetFilter(NetworkCapabilities netCap) {
        this.mCapabilityFilter = netCap;
        evalRequests();
    }

    public boolean acceptRequest(NetworkRequest request) {
        return this.mParent.acceptRequest(request);
    }

    private void evalRequest(NetworkRequestInfo n) {
        if (shouldNeedNetworkFor(n)) {
            this.mParent.needNetworkFor(n.request);
            n.requested = true;
        } else if (shouldReleaseNetworkFor(n)) {
            this.mParent.releaseNetworkFor(n.request);
            n.requested = false;
        }
    }

    private boolean shouldNeedNetworkFor(NetworkRequestInfo n) {
        return !n.requested && (n.score < this.mScore || n.providerId == this.mProvider.getProviderId()) && n.request.canBeSatisfiedBy(this.mCapabilityFilter) && acceptRequest(n.request);
    }

    private boolean shouldReleaseNetworkFor(NetworkRequestInfo n) {
        return n.requested && !((n.score <= this.mScore || n.providerId == this.mProvider.getProviderId()) && n.request.canBeSatisfiedBy(this.mCapabilityFilter) && acceptRequest(n.request));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void evalRequests() {
        for (NetworkRequestInfo n : this.mNetworkRequests.values()) {
            evalRequest(n);
        }
    }

    public void reevaluateAllRequests() {
        post(new Runnable() { // from class: android.net.NetworkFactoryLegacyImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                NetworkFactoryLegacyImpl.this.evalRequests();
            }
        });
    }

    @Override // android.net.NetworkFactoryShim
    public void releaseRequestAsUnfulfillableByAnyFactory(final NetworkRequest r) {
        post(new Runnable() { // from class: android.net.NetworkFactoryLegacyImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NetworkFactoryLegacyImpl.this.m19x5f2053ac(r);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$releaseRequestAsUnfulfillableByAnyFactory$0$android-net-NetworkFactoryLegacyImpl  reason: not valid java name */
    public /* synthetic */ void m19x5f2053ac(NetworkRequest r) {
        this.mParent.log("releaseRequestAsUnfulfillableByAnyFactory: " + r);
        NetworkProvider provider = this.mProvider;
        if (provider == null) {
            this.mParent.log("Ignoring attempt to release unregistered request as unfulfillable");
        } else {
            provider.declareNetworkRequestUnfulfillable(r);
        }
    }

    public void setScoreFilter(int score) {
        sendMessage(obtainMessage(3, score, 0));
    }

    public void setScoreFilter(NetworkScore score) {
        setScoreFilter(score.getLegacyInt());
    }

    public void setCapabilityFilter(NetworkCapabilities netCap) {
        sendMessage(obtainMessage(4, new NetworkCapabilities(netCap)));
    }

    public int getRequestCount() {
        return this.mNetworkRequests.size();
    }

    @Override // android.net.NetworkFactoryShim
    public int getSerialNumber() {
        return this.mProvider.getProviderId();
    }

    @Override // android.net.NetworkFactoryShim
    public NetworkProvider getProvider() {
        return this.mProvider;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(toString());
        for (NetworkRequestInfo n : this.mNetworkRequests.values()) {
            writer.println("  " + n);
        }
    }

    @Override // android.os.Handler
    public String toString() {
        StringBuilder append = new StringBuilder().append("providerId=");
        NetworkProvider networkProvider = this.mProvider;
        return append.append(networkProvider != null ? Integer.valueOf(networkProvider.getProviderId()) : "null").append(", ScoreFilter=").append(this.mScore).append(", Filter=").append(this.mCapabilityFilter).append(", requests=").append(this.mNetworkRequests.size()).toString();
    }
}
