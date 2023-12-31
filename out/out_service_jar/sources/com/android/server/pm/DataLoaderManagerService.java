package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.DataLoaderParamsParcel;
import android.content.pm.IDataLoader;
import android.content.pm.IDataLoaderManager;
import android.content.pm.IDataLoaderStatusListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.SystemService;
import com.android.server.pm.DataLoaderManagerService;
import java.util.List;
/* loaded from: classes2.dex */
public class DataLoaderManagerService extends SystemService {
    private static final String TAG = "DataLoaderManager";
    private final DataLoaderManagerBinderService mBinderService;
    private final Context mContext;
    private final Handler mHandler;
    private SparseArray<DataLoaderServiceConnection> mServiceConnections;
    private final HandlerThread mThread;

    public DataLoaderManagerService(Context context) {
        super(context);
        this.mServiceConnections = new SparseArray<>();
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        this.mBinderService = new DataLoaderManagerBinderService();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("dataloader_manager", this.mBinderService);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class DataLoaderManagerBinderService extends IDataLoaderManager.Stub {
        DataLoaderManagerBinderService() {
        }

        public boolean bindToDataLoader(final int dataLoaderId, DataLoaderParamsParcel params, long bindDelayMs, IDataLoaderStatusListener listener) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                try {
                    if (DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId) != null) {
                        return true;
                    }
                    ComponentName componentName = new ComponentName(params.packageName, params.className);
                    final ComponentName dataLoaderComponent = resolveDataLoaderComponentName(componentName);
                    if (dataLoaderComponent == null) {
                        Slog.e(DataLoaderManagerService.TAG, "Invalid component: " + componentName + " for ID=" + dataLoaderId);
                        return false;
                    }
                    final DataLoaderServiceConnection connection = new DataLoaderServiceConnection(dataLoaderId, listener);
                    final Intent intent = new Intent();
                    intent.setComponent(dataLoaderComponent);
                    return DataLoaderManagerService.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.DataLoaderManagerService$DataLoaderManagerBinderService$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            DataLoaderManagerService.DataLoaderManagerBinderService.this.m5406x955b9684(intent, connection, dataLoaderComponent, dataLoaderId);
                        }
                    }, bindDelayMs);
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$bindToDataLoader$0$com-android-server-pm-DataLoaderManagerService$DataLoaderManagerBinderService  reason: not valid java name */
        public /* synthetic */ void m5406x955b9684(Intent intent, DataLoaderServiceConnection connection, ComponentName dataLoaderComponent, int dataLoaderId) {
            if (!DataLoaderManagerService.this.mContext.bindServiceAsUser(intent, connection, 1, DataLoaderManagerService.this.mHandler, UserHandle.of(UserHandle.getCallingUserId()))) {
                Slog.e(DataLoaderManagerService.TAG, "Failed to bind to: " + dataLoaderComponent + " for ID=" + dataLoaderId);
                DataLoaderManagerService.this.mContext.unbindService(connection);
            }
        }

        private ComponentName resolveDataLoaderComponentName(ComponentName componentName) {
            PackageManager pm = DataLoaderManagerService.this.mContext.getPackageManager();
            if (pm == null) {
                Slog.e(DataLoaderManagerService.TAG, "PackageManager is not available.");
                return null;
            }
            Intent intent = new Intent("android.intent.action.LOAD_DATA");
            intent.setComponent(componentName);
            List<ResolveInfo> services = pm.queryIntentServicesAsUser(intent, 0, UserHandle.getCallingUserId());
            if (services == null || services.isEmpty()) {
                Slog.e(DataLoaderManagerService.TAG, "Failed to find data loader service provider in " + componentName);
                return null;
            }
            int numServices = services.size();
            if (0 < numServices) {
                ResolveInfo ri = services.get(0);
                ComponentName resolved = new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
                return resolved;
            }
            Slog.e(DataLoaderManagerService.TAG, "Didn't find any matching data loader service provider.");
            return null;
        }

        public IDataLoader getDataLoader(int dataLoaderId) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection serviceConnection = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId, null);
                if (serviceConnection == null) {
                    return null;
                }
                return serviceConnection.getDataLoader();
            }
        }

        public void unbindFromDataLoader(int dataLoaderId) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection serviceConnection = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId, null);
                if (serviceConnection == null) {
                    return;
                }
                serviceConnection.destroy();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DataLoaderServiceConnection implements ServiceConnection, IBinder.DeathRecipient {
        IDataLoader mDataLoader = null;
        final int mId;
        final IDataLoaderStatusListener mListener;

        DataLoaderServiceConnection(int id, IDataLoaderStatusListener listener) {
            this.mId = id;
            this.mListener = listener;
            callListener(1);
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName className, IBinder service) {
            IDataLoader asInterface = IDataLoader.Stub.asInterface(service);
            this.mDataLoader = asInterface;
            if (asInterface == null) {
                onNullBinding(className);
            } else if (!append()) {
                DataLoaderManagerService.this.mContext.unbindService(this);
            } else {
                try {
                    service.linkToDeath(this, 0);
                    callListener(2);
                } catch (RemoteException e) {
                    Slog.e(DataLoaderManagerService.TAG, "Failed to link to DataLoader's death: " + this.mId, e);
                    onBindingDied(className);
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " disconnected, but will try to recover");
            unbindAndReportDestroyed();
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " died");
            unbindAndReportDestroyed();
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " failed to start");
            unbindAndReportDestroyed();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " died");
            unbindAndReportDestroyed();
        }

        IDataLoader getDataLoader() {
            return this.mDataLoader;
        }

        private void unbindAndReportDestroyed() {
            if (unbind()) {
                callListener(0);
            }
        }

        void destroy() {
            IDataLoader iDataLoader = this.mDataLoader;
            if (iDataLoader != null) {
                try {
                    iDataLoader.destroy(this.mId);
                } catch (RemoteException e) {
                }
                this.mDataLoader = null;
            }
            unbind();
        }

        boolean unbind() {
            try {
                DataLoaderManagerService.this.mContext.unbindService(this);
            } catch (Exception e) {
            }
            return remove();
        }

        private boolean append() {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection bound = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(this.mId);
                if (bound == this) {
                    return true;
                }
                if (bound != null) {
                    return false;
                }
                DataLoaderManagerService.this.mServiceConnections.append(this.mId, this);
                return true;
            }
        }

        private boolean remove() {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                if (DataLoaderManagerService.this.mServiceConnections.get(this.mId) == this) {
                    DataLoaderManagerService.this.mServiceConnections.remove(this.mId);
                    return true;
                }
                return false;
            }
        }

        private void callListener(int status) {
            IDataLoaderStatusListener iDataLoaderStatusListener = this.mListener;
            if (iDataLoaderStatusListener != null) {
                try {
                    iDataLoaderStatusListener.onStatusChanged(this.mId, status);
                } catch (RemoteException e) {
                }
            }
        }
    }
}
