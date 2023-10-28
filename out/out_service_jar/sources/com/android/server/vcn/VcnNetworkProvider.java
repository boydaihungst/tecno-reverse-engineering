package com.android.server.vcn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.util.ArraySet;
import com.android.internal.util.IndentingPrintWriter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public class VcnNetworkProvider extends NetworkProvider {
    private static final String TAG = VcnNetworkProvider.class.getSimpleName();
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private final Set<NetworkRequestListener> mListeners;
    private final Set<NetworkRequest> mRequests;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface NetworkRequestListener {
        void onNetworkRequested(NetworkRequest networkRequest);
    }

    public VcnNetworkProvider(Context context, Looper looper) {
        this(context, looper, new Dependencies());
    }

    public VcnNetworkProvider(Context context, Looper looper, Dependencies dependencies) {
        super((Context) Objects.requireNonNull(context, "Missing context"), (Looper) Objects.requireNonNull(looper, "Missing looper"), TAG);
        this.mListeners = new ArraySet();
        this.mRequests = new ArraySet();
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mDeps = (Dependencies) Objects.requireNonNull(dependencies, "Missing dependencies");
    }

    public void register() {
        ((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class)).registerNetworkProvider(this);
        this.mDeps.registerNetworkOffer(this, Vcn.getNetworkScore(), buildCapabilityFilter(), new HandlerExecutor(this.mHandler), new NetworkProvider.NetworkOfferCallback() { // from class: com.android.server.vcn.VcnNetworkProvider.1
            public void onNetworkNeeded(NetworkRequest request) {
                VcnNetworkProvider.this.handleNetworkRequested(request);
            }

            public void onNetworkUnneeded(NetworkRequest request) {
                VcnNetworkProvider.this.handleNetworkRequestWithdrawn(request);
            }
        });
    }

    private NetworkCapabilities buildCapabilityFilter() {
        NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder().addTransportType(0).addCapability(14).addCapability(13).addCapability(15).addCapability(28);
        for (Integer num : VcnGatewayConnectionConfig.ALLOWED_CAPABILITIES) {
            int cap = num.intValue();
            builder.addCapability(cap);
        }
        return builder.build();
    }

    public void registerListener(NetworkRequestListener listener) {
        this.mListeners.add(listener);
        resendAllRequests(listener);
    }

    public void unregisterListener(NetworkRequestListener listener) {
        this.mListeners.remove(listener);
    }

    public void resendAllRequests(NetworkRequestListener listener) {
        for (NetworkRequest request : this.mRequests) {
            notifyListenerForEvent(listener, request);
        }
    }

    private void notifyListenerForEvent(NetworkRequestListener listener, NetworkRequest request) {
        listener.onNetworkRequested(request);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkRequested(NetworkRequest request) {
        this.mRequests.add(request);
        for (NetworkRequestListener listener : this.mListeners) {
            notifyListenerForEvent(listener, request);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkRequestWithdrawn(NetworkRequest request) {
        this.mRequests.remove(request);
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("VcnNetworkProvider:");
        pw.increaseIndent();
        pw.println("mListeners:");
        pw.increaseIndent();
        for (NetworkRequestListener listener : this.mListeners) {
            pw.println(listener);
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("mRequests:");
        pw.increaseIndent();
        for (NetworkRequest request : this.mRequests) {
            pw.println(request);
        }
        pw.decreaseIndent();
        pw.println();
        pw.decreaseIndent();
    }

    /* loaded from: classes2.dex */
    public static class Dependencies {
        public void registerNetworkOffer(VcnNetworkProvider provider, NetworkScore score, NetworkCapabilities capabilitiesFilter, Executor executor, NetworkProvider.NetworkOfferCallback callback) {
            provider.registerNetworkOffer(score, capabilitiesFilter, executor, callback);
        }
    }
}
