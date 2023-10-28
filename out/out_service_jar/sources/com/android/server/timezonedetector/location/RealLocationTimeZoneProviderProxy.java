package com.android.server.timezonedetector.location;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.timezone.ITimeZoneProvider;
import android.service.timezone.ITimeZoneProviderManager;
import android.service.timezone.TimeZoneProviderEvent;
import android.util.IndentingPrintWriter;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RealLocationTimeZoneProviderProxy extends LocationTimeZoneProviderProxy implements ServiceWatcher.ServiceListener<CurrentUserServiceSupplier.BoundServiceInfo> {
    private ManagerProxy mManagerProxy;
    private TimeZoneProviderRequest mRequest;
    private final ServiceWatcher mServiceWatcher;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RealLocationTimeZoneProviderProxy(Context context, Handler handler, ThreadingDomain threadingDomain, String action, String providerPackageName, boolean isTestProvider) {
        super(context, threadingDomain);
        CurrentUserServiceSupplier serviceSupplier;
        this.mManagerProxy = null;
        this.mRequest = TimeZoneProviderRequest.createStopUpdatesRequest();
        Objects.requireNonNull(providerPackageName);
        if (!isTestProvider) {
            serviceSupplier = CurrentUserServiceSupplier.create(context, action, providerPackageName, "android.permission.BIND_TIME_ZONE_PROVIDER_SERVICE", "android.permission.INSTALL_LOCATION_TIME_ZONE_PROVIDER_SERVICE");
        } else {
            serviceSupplier = CurrentUserServiceSupplier.createUnsafeForTestsOnly(context, action, providerPackageName, "android.permission.BIND_TIME_ZONE_PROVIDER_SERVICE", null);
        }
        this.mServiceWatcher = ServiceWatcher.create(context, handler, "RealLocationTimeZoneProviderProxy", serviceSupplier, this);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    void onInitialize() {
        if (!register()) {
            throw new IllegalStateException("Unable to register binder proxy");
        }
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    void onDestroy() {
        this.mServiceWatcher.unregister();
    }

    private boolean register() {
        boolean resolves = this.mServiceWatcher.checkServiceResolves();
        if (resolves) {
            this.mServiceWatcher.register();
        }
        return resolves;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onBind(IBinder binder, CurrentUserServiceSupplier.BoundServiceInfo boundService) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            this.mManagerProxy = new ManagerProxy();
            this.mListener.onProviderBound();
            trySendCurrentRequest();
        }
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onUnbind() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            this.mManagerProxy = null;
            this.mListener.onProviderUnbound();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    public final void setRequest(TimeZoneProviderRequest request) {
        this.mThreadingDomain.assertCurrentThread();
        Objects.requireNonNull(request);
        synchronized (this.mSharedLock) {
            this.mRequest = request;
            trySendCurrentRequest();
        }
    }

    private void trySendCurrentRequest() {
        final ManagerProxy managerProxy = this.mManagerProxy;
        final TimeZoneProviderRequest request = this.mRequest;
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.timezonedetector.location.RealLocationTimeZoneProviderProxy$$ExternalSyntheticLambda0
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public final void run(IBinder iBinder) {
                RealLocationTimeZoneProviderProxy.lambda$trySendCurrentRequest$0(TimeZoneProviderRequest.this, managerProxy, iBinder);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$trySendCurrentRequest$0(TimeZoneProviderRequest request, ManagerProxy managerProxy, IBinder binder) throws RemoteException {
        ITimeZoneProvider service = ITimeZoneProvider.Stub.asInterface(binder);
        if (request.sendUpdates()) {
            service.startUpdates(managerProxy, request.getInitializationTimeout().toMillis(), request.getEventFilteringAgeThreshold().toMillis());
        } else {
            service.stopUpdates();
        }
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public void dump(IndentingPrintWriter ipw, String[] args) {
        synchronized (this.mSharedLock) {
            ipw.println("{RealLocationTimeZoneProviderProxy}");
            ipw.println("mRequest=" + this.mRequest);
            this.mServiceWatcher.dump(ipw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ManagerProxy extends ITimeZoneProviderManager.Stub {
        private ManagerProxy() {
        }

        public void onTimeZoneProviderEvent(TimeZoneProviderEvent event) {
            synchronized (RealLocationTimeZoneProviderProxy.this.mSharedLock) {
                if (RealLocationTimeZoneProviderProxy.this.mManagerProxy != this) {
                    return;
                }
                RealLocationTimeZoneProviderProxy.this.handleTimeZoneProviderEvent(event);
            }
        }
    }
}
