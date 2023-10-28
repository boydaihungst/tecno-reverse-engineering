package com.android.server.location.provider.proxy;

import android.content.Context;
import android.location.Location;
import android.location.LocationResult;
import android.location.provider.ILocationProvider;
import android.location.provider.ILocationProviderManager;
import android.location.provider.ProviderProperties;
import android.location.provider.ProviderRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.FgThread;
import com.android.server.location.LocationManagerService;
import com.android.server.location.provider.AbstractLocationProvider;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
/* loaded from: classes.dex */
public class ProxyLocationProvider extends AbstractLocationProvider implements ServiceWatcher.ServiceListener<CurrentUserServiceSupplier.BoundServiceInfo> {
    private static final String EXTRA_LOCATION_TAGS = "android:location_allow_listed_tags";
    private static final String LOCATION_TAGS_SEPARATOR = ";";
    private static final long RESET_DELAY_MS = 1000;
    CurrentUserServiceSupplier.BoundServiceInfo mBoundServiceInfo;
    final Context mContext;
    final ArrayList<Runnable> mFlushListeners;
    final Object mLock;
    final String mName;
    Proxy mProxy;
    private volatile ProviderRequest mRequest;
    Runnable mResetter;
    final ServiceWatcher mServiceWatcher;

    public static ProxyLocationProvider create(Context context, String provider, String action, int enableOverlayResId, int nonOverlayPackageResId) {
        ProxyLocationProvider proxy = new ProxyLocationProvider(context, provider, action, enableOverlayResId, nonOverlayPackageResId);
        if (proxy.checkServiceResolves()) {
            return proxy;
        }
        return null;
    }

    private ProxyLocationProvider(Context context, String provider, String action, int enableOverlayResId, int nonOverlayPackageResId) {
        super(ConcurrentUtils.DIRECT_EXECUTOR, null, null, Collections.emptySet());
        this.mLock = new Object();
        this.mFlushListeners = new ArrayList<>(0);
        this.mContext = context;
        this.mServiceWatcher = ServiceWatcher.create(context, provider, CurrentUserServiceSupplier.createFromConfig(context, action, enableOverlayResId, nonOverlayPackageResId), this);
        this.mName = provider;
        this.mProxy = null;
        this.mRequest = ProviderRequest.EMPTY_REQUEST;
    }

    private boolean checkServiceResolves() {
        return this.mServiceWatcher.checkServiceResolves();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onBind(IBinder binder, CurrentUserServiceSupplier.BoundServiceInfo boundServiceInfo) throws RemoteException {
        ILocationProvider provider = ILocationProvider.Stub.asInterface(binder);
        synchronized (this.mLock) {
            Proxy proxy = new Proxy();
            this.mProxy = proxy;
            this.mBoundServiceInfo = boundServiceInfo;
            provider.setLocationProviderManager(proxy);
            ProviderRequest request = this.mRequest;
            if (!request.equals(ProviderRequest.EMPTY_REQUEST)) {
                provider.setRequest(request);
            }
        }
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onUnbind() {
        Runnable[] flushListeners;
        synchronized (this.mLock) {
            this.mProxy = null;
            this.mBoundServiceInfo = null;
            if (this.mResetter == null) {
                this.mResetter = new AnonymousClass1();
                FgThread.getHandler().postDelayed(this.mResetter, 1000L);
            }
            flushListeners = (Runnable[]) this.mFlushListeners.toArray(new Runnable[0]);
            this.mFlushListeners.clear();
        }
        for (Runnable runnable : flushListeners) {
            runnable.run();
        }
    }

    /* renamed from: com.android.server.location.provider.proxy.ProxyLocationProvider$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mResetter == this) {
                    ProxyLocationProvider.this.setState(new UnaryOperator() { // from class: com.android.server.location.provider.proxy.ProxyLocationProvider$1$$ExternalSyntheticLambda0
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            AbstractLocationProvider.State state;
                            AbstractLocationProvider.State state2 = (AbstractLocationProvider.State) obj;
                            state = AbstractLocationProvider.State.EMPTY_STATE;
                            return state;
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStart() {
        this.mServiceWatcher.register();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStop() {
        this.mServiceWatcher.unregister();
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onSetRequest(final ProviderRequest request) {
        this.mRequest = request;
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.location.provider.proxy.ProxyLocationProvider$$ExternalSyntheticLambda0
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public final void run(IBinder iBinder) {
                ProxyLocationProvider.lambda$onSetRequest$0(request, iBinder);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onSetRequest$0(ProviderRequest request, IBinder binder) throws RemoteException {
        ILocationProvider provider = ILocationProvider.Stub.asInterface(binder);
        provider.setRequest(request);
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onFlush(final Runnable callback) {
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.location.provider.proxy.ProxyLocationProvider.2
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void run(IBinder binder) throws RemoteException {
                ILocationProvider provider = ILocationProvider.Stub.asInterface(binder);
                synchronized (ProxyLocationProvider.this.mLock) {
                    ProxyLocationProvider.this.mFlushListeners.add(callback);
                }
                provider.flush();
            }

            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void onError(Throwable t) {
                synchronized (ProxyLocationProvider.this.mLock) {
                    ProxyLocationProvider.this.mFlushListeners.remove(callback);
                }
                callback.run();
            }
        });
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onExtraCommand(int uid, int pid, final String command, final Bundle extras) {
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.location.provider.proxy.ProxyLocationProvider$$ExternalSyntheticLambda1
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public final void run(IBinder iBinder) {
                ProxyLocationProvider.lambda$onExtraCommand$1(command, extras, iBinder);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onExtraCommand$1(String command, Bundle extras, IBinder binder) throws RemoteException {
        ILocationProvider provider = ILocationProvider.Stub.asInterface(binder);
        provider.sendExtraCommand(command, extras);
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mServiceWatcher.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Proxy extends ILocationProviderManager.Stub {
        Proxy() {
        }

        public void onInitialize(final boolean allowed, final ProviderProperties properties, String attributionTag) {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                if (ProxyLocationProvider.this.mResetter != null) {
                    FgThread.getHandler().removeCallbacks(ProxyLocationProvider.this.mResetter);
                    ProxyLocationProvider.this.mResetter = null;
                }
                String[] attributionTags = new String[0];
                if (ProxyLocationProvider.this.mBoundServiceInfo.getMetadata() != null) {
                    String tagsStr = ProxyLocationProvider.this.mBoundServiceInfo.getMetadata().getString(ProxyLocationProvider.EXTRA_LOCATION_TAGS);
                    if (!TextUtils.isEmpty(tagsStr)) {
                        attributionTags = tagsStr.split(ProxyLocationProvider.LOCATION_TAGS_SEPARATOR);
                        Log.i(LocationManagerService.TAG, ProxyLocationProvider.this.mName + " provider loaded extra attribution tags: " + Arrays.toString(attributionTags));
                    }
                }
                final ArraySet<String> extraAttributionTags = new ArraySet<>(attributionTags);
                final CallerIdentity identity = CallerIdentity.fromBinderUnsafe(ProxyLocationProvider.this.mBoundServiceInfo.getComponentName().getPackageName(), attributionTag);
                ProxyLocationProvider.this.setState(new UnaryOperator() { // from class: com.android.server.location.provider.proxy.ProxyLocationProvider$Proxy$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        AbstractLocationProvider.State withExtraAttributionTags;
                        boolean z = allowed;
                        ProviderProperties providerProperties = properties;
                        AbstractLocationProvider.State state = (AbstractLocationProvider.State) obj;
                        withExtraAttributionTags = AbstractLocationProvider.State.EMPTY_STATE.withAllowed(z).withProperties(providerProperties).withIdentity(identity).withExtraAttributionTags(extraAttributionTags);
                        return withExtraAttributionTags;
                    }
                });
            }
        }

        public void onSetProperties(ProviderProperties properties) {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                ProxyLocationProvider.this.setProperties(properties);
            }
        }

        public void onSetAllowed(boolean allowed) {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                ProxyLocationProvider.this.setAllowed(allowed);
            }
        }

        public void onReportLocation(Location location) {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                ProxyLocationProvider.this.reportLocation(LocationResult.wrap(new Location[]{location}).validate());
            }
        }

        public void onReportLocations(List<Location> locations) {
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                ProxyLocationProvider.this.reportLocation(LocationResult.wrap(locations).validate());
            }
        }

        public void onFlushComplete() {
            Runnable callback = null;
            synchronized (ProxyLocationProvider.this.mLock) {
                if (ProxyLocationProvider.this.mProxy != this) {
                    return;
                }
                if (!ProxyLocationProvider.this.mFlushListeners.isEmpty()) {
                    callback = ProxyLocationProvider.this.mFlushListeners.remove(0);
                }
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }
}
