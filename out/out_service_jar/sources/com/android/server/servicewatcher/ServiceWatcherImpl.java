package com.android.server.servicewatcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.Preconditions;
import com.android.server.servicewatcher.ServiceWatcher;
import com.android.server.servicewatcher.ServiceWatcher.BoundServiceInfo;
import com.android.server.servicewatcher.ServiceWatcherImpl;
import com.android.server.tare.AlarmManagerEconomicPolicy;
import java.io.PrintWriter;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ServiceWatcherImpl<TBoundServiceInfo extends ServiceWatcher.BoundServiceInfo> implements ServiceWatcher, ServiceWatcher.ServiceChangedListener {
    static final long RETRY_DELAY_MS = 15000;
    final Context mContext;
    final Handler mHandler;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() { // from class: com.android.server.servicewatcher.ServiceWatcherImpl.1
        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            return true;
        }

        public void onSomePackagesChanged() {
            ServiceWatcherImpl.this.onServiceChanged(false);
        }
    };
    private boolean mRegistered = false;
    private ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection mServiceConnection = new MyServiceConnection(null);
    final ServiceWatcher.ServiceListener<? super TBoundServiceInfo> mServiceListener;
    final ServiceWatcher.ServiceSupplier<TBoundServiceInfo> mServiceSupplier;
    final String mTag;
    static final String TAG = "ServiceWatcher";
    static final boolean D = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceWatcherImpl(Context context, Handler handler, String tag, ServiceWatcher.ServiceSupplier<TBoundServiceInfo> serviceSupplier, ServiceWatcher.ServiceListener<? super TBoundServiceInfo> serviceListener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mTag = tag;
        this.mServiceSupplier = serviceSupplier;
        this.mServiceListener = serviceListener;
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher
    public boolean checkServiceResolves() {
        return this.mServiceSupplier.hasMatchingService();
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher
    public synchronized void register() {
        Preconditions.checkState(!this.mRegistered);
        this.mRegistered = true;
        this.mPackageMonitor.register(this.mContext, UserHandle.ALL, true, this.mHandler);
        this.mServiceSupplier.register(this);
        onServiceChanged(false);
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher
    public synchronized void unregister() {
        Preconditions.checkState(this.mRegistered);
        this.mServiceSupplier.unregister();
        this.mPackageMonitor.unregister();
        this.mRegistered = false;
        onServiceChanged(false);
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceChangedListener
    public synchronized void onServiceChanged() {
        onServiceChanged(false);
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher
    public synchronized void runOnBinder(final ServiceWatcher.BinderOperation operation) {
        final ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection serviceConnection = this.mServiceConnection;
        this.mHandler.post(new Runnable() { // from class: com.android.server.servicewatcher.ServiceWatcherImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ServiceWatcherImpl.MyServiceConnection.this.runOnBinder(operation);
            }
        });
    }

    synchronized void onServiceChanged(boolean forceRebind) {
        TBoundServiceInfo newBoundServiceInfo;
        if (this.mRegistered) {
            newBoundServiceInfo = this.mServiceSupplier.getServiceInfo();
        } else {
            newBoundServiceInfo = null;
        }
        if (forceRebind || !Objects.equals(this.mServiceConnection.getBoundServiceInfo(), newBoundServiceInfo)) {
            Log.i(TAG, "[" + this.mTag + "] chose new implementation " + newBoundServiceInfo);
            final ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection oldServiceConnection = this.mServiceConnection;
            final ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection newServiceConnection = new MyServiceConnection(newBoundServiceInfo);
            this.mServiceConnection = newServiceConnection;
            this.mHandler.post(new Runnable() { // from class: com.android.server.servicewatcher.ServiceWatcherImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ServiceWatcherImpl.lambda$onServiceChanged$1(ServiceWatcherImpl.MyServiceConnection.this, newServiceConnection);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onServiceChanged$1(MyServiceConnection oldServiceConnection, MyServiceConnection newServiceConnection) {
        oldServiceConnection.unbind();
        newServiceConnection.bind();
    }

    /* JADX WARN: Type inference failed for: r1v0, types: [com.android.server.servicewatcher.ServiceWatcher$BoundServiceInfo] */
    public String toString() {
        ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection serviceConnection;
        synchronized (this) {
            serviceConnection = this.mServiceConnection;
        }
        return serviceConnection.getBoundServiceInfo().toString();
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher
    public void dump(PrintWriter pw) {
        ServiceWatcherImpl<TBoundServiceInfo>.MyServiceConnection serviceConnection;
        synchronized (this) {
            serviceConnection = this.mServiceConnection;
        }
        pw.println("target service=" + serviceConnection.getBoundServiceInfo());
        pw.println("connected=" + serviceConnection.isConnected());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class MyServiceConnection implements ServiceConnection {
        private volatile IBinder mBinder;
        private final TBoundServiceInfo mBoundServiceInfo;
        private Runnable mRebinder;

        MyServiceConnection(TBoundServiceInfo boundServiceInfo) {
            this.mBoundServiceInfo = boundServiceInfo;
        }

        TBoundServiceInfo getBoundServiceInfo() {
            return this.mBoundServiceInfo;
        }

        boolean isConnected() {
            return this.mBinder != null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void bind() {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            if (this.mBoundServiceInfo == null) {
                return;
            }
            if (ServiceWatcherImpl.D) {
                Log.d(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] binding to " + this.mBoundServiceInfo);
            }
            Intent bindIntent = new Intent(this.mBoundServiceInfo.getAction()).setComponent(this.mBoundServiceInfo.getComponentName());
            if (!ServiceWatcherImpl.this.mContext.bindServiceAsUser(bindIntent, this, AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT, ServiceWatcherImpl.this.mHandler, UserHandle.of(this.mBoundServiceInfo.getUserId()))) {
                Log.e(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] unexpected bind failure - retrying later");
                this.mRebinder = new Runnable() { // from class: com.android.server.servicewatcher.ServiceWatcherImpl$MyServiceConnection$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        ServiceWatcherImpl.MyServiceConnection.this.bind();
                    }
                };
                ServiceWatcherImpl.this.mHandler.postDelayed(this.mRebinder, ServiceWatcherImpl.RETRY_DELAY_MS);
                return;
            }
            this.mRebinder = null;
        }

        void unbind() {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            if (this.mBoundServiceInfo == null) {
                return;
            }
            if (ServiceWatcherImpl.D) {
                Log.d(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] unbinding from " + this.mBoundServiceInfo);
            }
            if (this.mRebinder != null) {
                ServiceWatcherImpl.this.mHandler.removeCallbacks(this.mRebinder);
                this.mRebinder = null;
            } else {
                ServiceWatcherImpl.this.mContext.unbindService(this);
            }
            onServiceDisconnected(this.mBoundServiceInfo.getComponentName());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void runOnBinder(ServiceWatcher.BinderOperation operation) {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            if (this.mBinder == null) {
                operation.onError(new DeadObjectException());
                return;
            }
            try {
                operation.run(this.mBinder);
            } catch (RemoteException | RuntimeException e) {
                Log.e(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] error running operation on " + this.mBoundServiceInfo, e);
                operation.onError(e);
            }
        }

        @Override // android.content.ServiceConnection
        public final void onServiceConnected(ComponentName component, IBinder binder) {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            Preconditions.checkState(this.mBinder == null);
            Log.i(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] connected to " + component.toShortString());
            this.mBinder = binder;
            if (ServiceWatcherImpl.this.mServiceListener != null) {
                try {
                    ServiceWatcherImpl.this.mServiceListener.onBind(binder, this.mBoundServiceInfo);
                } catch (RemoteException | RuntimeException e) {
                    Log.e(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] error running operation on " + this.mBoundServiceInfo, e);
                }
            }
        }

        @Override // android.content.ServiceConnection
        public final void onServiceDisconnected(ComponentName component) {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            if (this.mBinder == null) {
                return;
            }
            Log.i(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] disconnected from " + this.mBoundServiceInfo);
            this.mBinder = null;
            if (ServiceWatcherImpl.this.mServiceListener != null) {
                ServiceWatcherImpl.this.mServiceListener.onUnbind();
            }
        }

        @Override // android.content.ServiceConnection
        public final void onBindingDied(ComponentName component) {
            Preconditions.checkState(Looper.myLooper() == ServiceWatcherImpl.this.mHandler.getLooper());
            Log.w(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] " + this.mBoundServiceInfo + " died");
            ServiceWatcherImpl.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.servicewatcher.ServiceWatcherImpl$MyServiceConnection$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ServiceWatcherImpl.MyServiceConnection.this.m6476x663ab6fb();
                }
            }, 500L);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBindingDied$0$com-android-server-servicewatcher-ServiceWatcherImpl$MyServiceConnection  reason: not valid java name */
        public /* synthetic */ void m6476x663ab6fb() {
            ServiceWatcherImpl.this.onServiceChanged(true);
        }

        @Override // android.content.ServiceConnection
        public final void onNullBinding(ComponentName component) {
            Log.e(ServiceWatcherImpl.TAG, "[" + ServiceWatcherImpl.this.mTag + "] " + this.mBoundServiceInfo + " has null binding");
        }
    }
}
