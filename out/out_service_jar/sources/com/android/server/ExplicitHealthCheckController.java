package com.android.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.watchdog.ExplicitHealthCheckService;
import android.service.watchdog.IExplicitHealthCheckService;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ExplicitHealthCheckController {
    private static final String TAG = "ExplicitHealthCheckController";
    private ServiceConnection mConnection;
    private final Context mContext;
    private boolean mEnabled;
    private final Object mLock = new Object();
    private Runnable mNotifySyncRunnable;
    private Consumer<String> mPassedConsumer;
    private IExplicitHealthCheckService mRemoteService;
    private Consumer<List<ExplicitHealthCheckService.PackageConfig>> mSupportedConsumer;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ExplicitHealthCheckController(Context context) {
        this.mContext = context;
    }

    public void setEnabled(boolean enabled) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Explicit health checks " + (enabled ? "enabled." : "disabled."));
            this.mEnabled = enabled;
        }
    }

    public void setCallbacks(Consumer<String> passedConsumer, Consumer<List<ExplicitHealthCheckService.PackageConfig>> supportedConsumer, Runnable notifySyncRunnable) {
        synchronized (this.mLock) {
            if (this.mPassedConsumer != null || this.mSupportedConsumer != null || this.mNotifySyncRunnable != null) {
                Slog.wtf(TAG, "Resetting health check controller callbacks");
            }
            this.mPassedConsumer = (Consumer) Objects.requireNonNull(passedConsumer);
            this.mSupportedConsumer = (Consumer) Objects.requireNonNull(supportedConsumer);
            this.mNotifySyncRunnable = (Runnable) Objects.requireNonNull(notifySyncRunnable);
        }
    }

    public void syncRequests(final Set<String> newRequestedPackages) {
        boolean enabled;
        synchronized (this.mLock) {
            enabled = this.mEnabled;
        }
        if (!enabled) {
            Slog.i(TAG, "Health checks disabled, no supported packages");
            this.mSupportedConsumer.accept(Collections.emptyList());
            return;
        }
        getSupportedPackages(new Consumer() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ExplicitHealthCheckController.this.m191x60aa37ed(newRequestedPackages, (List) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$syncRequests$3$com-android-server-ExplicitHealthCheckController  reason: not valid java name */
    public /* synthetic */ void m191x60aa37ed(final Set newRequestedPackages, final List supportedPackageConfigs) {
        this.mSupportedConsumer.accept(supportedPackageConfigs);
        getRequestedPackages(new Consumer() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ExplicitHealthCheckController.this.m190x6f0091ce(supportedPackageConfigs, newRequestedPackages, (List) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$syncRequests$2$com-android-server-ExplicitHealthCheckController  reason: not valid java name */
    public /* synthetic */ void m190x6f0091ce(List supportedPackageConfigs, Set newRequestedPackages, List previousRequestedPackages) {
        synchronized (this.mLock) {
            Set<String> supportedPackages = new ArraySet<>();
            Iterator it = supportedPackageConfigs.iterator();
            while (it.hasNext()) {
                ExplicitHealthCheckService.PackageConfig config = (ExplicitHealthCheckService.PackageConfig) it.next();
                supportedPackages.add(config.getPackageName());
            }
            newRequestedPackages.retainAll(supportedPackages);
            actOnDifference(previousRequestedPackages, newRequestedPackages, new Consumer() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ExplicitHealthCheckController.this.m188x8bad4590((String) obj);
                }
            });
            actOnDifference(newRequestedPackages, previousRequestedPackages, new Consumer() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda6
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ExplicitHealthCheckController.this.m189x7d56ebaf((String) obj);
                }
            });
            if (newRequestedPackages.isEmpty()) {
                Slog.i(TAG, "No more health check requests, unbinding...");
                unbindService();
            }
        }
    }

    private void actOnDifference(Collection<String> collection1, Collection<String> collection2, Consumer<String> action) {
        for (String packageName : collection1) {
            if (!collection2.contains(packageName)) {
                action.accept(packageName);
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: request */
    public void m189x7d56ebaf(String packageName) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("request health check for " + packageName)) {
                Slog.i(TAG, "Requesting health check for package " + packageName);
                try {
                    this.mRemoteService.request(packageName);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to request health check for package " + packageName, e);
                }
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: cancel */
    public void m188x8bad4590(String packageName) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("cancel health check for " + packageName)) {
                Slog.i(TAG, "Cancelling health check for package " + packageName);
                try {
                    this.mRemoteService.cancel(packageName);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to cancel health check for package " + packageName, e);
                }
            }
        }
    }

    private void getSupportedPackages(final Consumer<List<ExplicitHealthCheckService.PackageConfig>> consumer) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("get health check supported packages")) {
                Slog.d(TAG, "Getting health check supported packages");
                try {
                    this.mRemoteService.getSupportedPackages(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda3
                        public final void onResult(Bundle bundle) {
                            ExplicitHealthCheckController.lambda$getSupportedPackages$4(consumer, bundle);
                        }
                    }));
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to get health check supported packages", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getSupportedPackages$4(Consumer consumer, Bundle result) {
        ArrayList parcelableArrayList = result.getParcelableArrayList("android.service.watchdog.extra.supported_packages");
        Slog.i(TAG, "Explicit health check supported packages " + parcelableArrayList);
        consumer.accept(parcelableArrayList);
    }

    private void getRequestedPackages(final Consumer<List<String>> consumer) {
        synchronized (this.mLock) {
            if (prepareServiceLocked("get health check requested packages")) {
                Slog.d(TAG, "Getting health check requested packages");
                try {
                    this.mRemoteService.getRequestedPackages(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda2
                        public final void onResult(Bundle bundle) {
                            ExplicitHealthCheckController.lambda$getRequestedPackages$5(consumer, bundle);
                        }
                    }));
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to get health check requested packages", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getRequestedPackages$5(Consumer consumer, Bundle result) {
        ArrayList<String> stringArrayList = result.getStringArrayList("android.service.watchdog.extra.requested_packages");
        Slog.i(TAG, "Explicit health check requested packages " + stringArrayList);
        consumer.accept(stringArrayList);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0045, code lost:
        android.util.Slog.i(com.android.server.ExplicitHealthCheckController.TAG, "Not binding to service, service disabled");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void bindService() {
        synchronized (this.mLock) {
            boolean z = this.mEnabled;
            if (z && this.mConnection == null && this.mRemoteService == null) {
                ComponentName component = getServiceComponentNameLocked();
                if (component == null) {
                    Slog.wtf(TAG, "Explicit health check service not found");
                    return;
                }
                Intent intent = new Intent();
                intent.setComponent(component);
                ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.ExplicitHealthCheckController.1
                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service is connected " + name);
                        ExplicitHealthCheckController.this.initState(service);
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName name) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service is disconnected " + name);
                        synchronized (ExplicitHealthCheckController.this.mLock) {
                            ExplicitHealthCheckController.this.mRemoteService = null;
                        }
                    }

                    @Override // android.content.ServiceConnection
                    public void onBindingDied(ComponentName name) {
                        Slog.i(ExplicitHealthCheckController.TAG, "Explicit health check service binding is dead. Rebind: " + name);
                        ExplicitHealthCheckController.this.unbindService();
                        ExplicitHealthCheckController.this.bindService();
                    }

                    @Override // android.content.ServiceConnection
                    public void onNullBinding(ComponentName name) {
                        Slog.wtf(ExplicitHealthCheckController.TAG, "Explicit health check service binding is null?? " + name);
                    }
                };
                this.mConnection = serviceConnection;
                this.mContext.bindServiceAsUser(intent, serviceConnection, 1, UserHandle.of(0));
                Slog.i(TAG, "Explicit health check service is bound");
                return;
            }
            if (this.mRemoteService != null) {
                Slog.i(TAG, "Not binding to service, service already connected");
            } else {
                Slog.i(TAG, "Not binding to service, service already connecting");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindService() {
        synchronized (this.mLock) {
            if (this.mRemoteService != null) {
                this.mContext.unbindService(this.mConnection);
                this.mRemoteService = null;
                this.mConnection = null;
            }
            Slog.i(TAG, "Explicit health check service is unbound");
        }
    }

    private ServiceInfo getServiceInfoLocked() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.watchdog.ExplicitHealthCheckService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, 132);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        return resolveInfo.serviceInfo;
    }

    private ComponentName getServiceComponentNameLocked() {
        ServiceInfo serviceInfo = getServiceInfoLocked();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (!"android.permission.BIND_EXPLICIT_HEALTH_CHECK_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, name.flattenToShortString() + " does not require permission android.permission.BIND_EXPLICIT_HEALTH_CHECK_SERVICE");
            return null;
        }
        return name;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initState(IBinder service) {
        synchronized (this.mLock) {
            if (!this.mEnabled) {
                Slog.w(TAG, "Attempting to connect disabled service?? Unbinding...");
                unbindService();
                return;
            }
            IExplicitHealthCheckService asInterface = IExplicitHealthCheckService.Stub.asInterface(service);
            this.mRemoteService = asInterface;
            try {
                asInterface.setCallback(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ExplicitHealthCheckController$$ExternalSyntheticLambda0
                    public final void onResult(Bundle bundle) {
                        ExplicitHealthCheckController.this.m187x39d409c2(bundle);
                    }
                }));
                Slog.i(TAG, "Service initialized, syncing requests");
            } catch (RemoteException e) {
                Slog.wtf(TAG, "Could not setCallback on explicit health check service");
            }
            this.mNotifySyncRunnable.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initState$6$com-android-server-ExplicitHealthCheckController  reason: not valid java name */
    public /* synthetic */ void m187x39d409c2(Bundle result) {
        String packageName = result.getString("android.service.watchdog.extra.health_check_passed_package");
        if (!TextUtils.isEmpty(packageName)) {
            Consumer<String> consumer = this.mPassedConsumer;
            if (consumer == null) {
                Slog.wtf(TAG, "Health check passed for package " + packageName + "but no consumer registered.");
                return;
            } else {
                consumer.accept(packageName);
                return;
            }
        }
        Slog.wtf(TAG, "Empty package passed explicit health check?");
    }

    private boolean prepareServiceLocked(String action) {
        if (this.mRemoteService != null && this.mEnabled) {
            return true;
        }
        Slog.i(TAG, "Service not ready to " + action + (this.mEnabled ? ". Binding..." : ". Disabled"));
        if (this.mEnabled) {
            bindService();
            return false;
        }
        return false;
    }
}
