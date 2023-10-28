package android.net;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.IPacProxyInstalledListener;
import android.net.PacProxyManager;
import android.os.Binder;
import android.os.RemoteException;
import com.android.internal.util.FunctionalUtils;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executor;
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
/* loaded from: classes2.dex */
public class PacProxyManager {
    private final Context mContext;
    private final HashMap<PacProxyInstalledListener, PacProxyInstalledListenerProxy> mListenerMap = new HashMap<>();
    private final IPacProxyManager mService;

    /* loaded from: classes2.dex */
    public interface PacProxyInstalledListener {
        void onPacProxyInstalled(Network network, ProxyInfo proxyInfo);
    }

    public PacProxyManager(Context context, IPacProxyManager service) {
        Objects.requireNonNull(service, "missing IPacProxyManager");
        this.mContext = context;
        this.mService = service;
    }

    public void addPacProxyInstalledListener(Executor executor, PacProxyInstalledListener listener) {
        try {
            synchronized (this.mListenerMap) {
                PacProxyInstalledListenerProxy listenerProxy = new PacProxyInstalledListenerProxy(executor, listener);
                if (this.mListenerMap.putIfAbsent(listener, listenerProxy) != null) {
                    throw new IllegalStateException("Listener is already added.");
                }
                this.mService.addListener(listenerProxy);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removePacProxyInstalledListener(PacProxyInstalledListener listener) {
        try {
            synchronized (this.mListenerMap) {
                PacProxyInstalledListenerProxy listenerProxy = this.mListenerMap.remove(listener);
                if (listenerProxy == null) {
                    return;
                }
                this.mService.removeListener(listenerProxy);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCurrentProxyScriptUrl(ProxyInfo proxy) {
        try {
            this.mService.setCurrentProxyScriptUrl(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes2.dex */
    public class PacProxyInstalledListenerProxy extends IPacProxyInstalledListener.Stub {
        private final Executor mExecutor;
        private final PacProxyInstalledListener mListener;

        PacProxyInstalledListenerProxy(Executor executor, PacProxyInstalledListener listener) {
            this.mExecutor = executor;
            this.mListener = listener;
        }

        @Override // android.net.IPacProxyInstalledListener
        public void onPacProxyInstalled(final Network network, final ProxyInfo proxy) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: android.net.PacProxyManager$PacProxyInstalledListenerProxy$$ExternalSyntheticLambda1
                @Override // com.android.internal.util.FunctionalUtils.ThrowingRunnable
                public final void runOrThrow() {
                    PacProxyManager.PacProxyInstalledListenerProxy.this.m2771x182047b2(network, proxy);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPacProxyInstalled$1$android-net-PacProxyManager$PacProxyInstalledListenerProxy  reason: not valid java name */
        public /* synthetic */ void m2771x182047b2(final Network network, final ProxyInfo proxy) throws Exception {
            this.mExecutor.execute(new Runnable() { // from class: android.net.PacProxyManager$PacProxyInstalledListenerProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PacProxyManager.PacProxyInstalledListenerProxy.this.m2770x121c7c53(network, proxy);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPacProxyInstalled$0$android-net-PacProxyManager$PacProxyInstalledListenerProxy  reason: not valid java name */
        public /* synthetic */ void m2770x121c7c53(Network network, ProxyInfo proxy) {
            this.mListener.onPacProxyInstalled(network, proxy);
        }
    }
}
