package com.android.server.devicepolicy;

import android.app.admin.DeviceAdminService;
import android.app.admin.IDeviceAdminService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.PersistentConnection;
import com.android.server.appbinding.AppBindingUtils;
import com.android.server.devicepolicy.DevicePolicyManagerService;
/* loaded from: classes.dex */
public class DeviceAdminServiceController {
    static final boolean DEBUG = false;
    static final String TAG = "DevicePolicyManager";
    private final DevicePolicyConstants mConstants;
    final Context mContext;
    private final Handler mHandler;
    private final DevicePolicyManagerService.Injector mInjector;
    final Object mLock = new Object();
    private final SparseArray<DevicePolicyServiceConnection> mConnections = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DevicePolicyServiceConnection extends PersistentConnection<IDeviceAdminService> {
        public DevicePolicyServiceConnection(int userId, ComponentName componentName) {
            super(DeviceAdminServiceController.TAG, DeviceAdminServiceController.this.mContext, DeviceAdminServiceController.this.mHandler, userId, componentName, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_STABLE_CONNECTION_THRESHOLD_SEC);
        }

        @Override // com.android.server.am.PersistentConnection
        protected int getBindFlags() {
            return 67108864;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.am.PersistentConnection
        public IDeviceAdminService asInterface(IBinder binder) {
            return IDeviceAdminService.Stub.asInterface(binder);
        }
    }

    public DeviceAdminServiceController(DevicePolicyManagerService service, DevicePolicyConstants constants) {
        DevicePolicyManagerService.Injector injector = service.mInjector;
        this.mInjector = injector;
        this.mContext = injector.mContext;
        this.mHandler = new Handler(BackgroundThread.get().getLooper());
        this.mConstants = constants;
    }

    private ServiceInfo findService(String packageName, int userId) {
        return AppBindingUtils.findService(packageName, userId, "android.app.action.DEVICE_ADMIN_SERVICE", "android.permission.BIND_DEVICE_ADMIN", DeviceAdminService.class, this.mInjector.getIPackageManager(), new StringBuilder());
    }

    public void startServiceForOwner(String packageName, int userId, String actionForLog) {
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                ServiceInfo service = findService(packageName, userId);
                if (service == null) {
                    disconnectServiceOnUserLocked(userId, actionForLog);
                    return;
                }
                PersistentConnection<IDeviceAdminService> existing = this.mConnections.get(userId);
                if (existing != null) {
                    disconnectServiceOnUserLocked(userId, actionForLog);
                }
                DevicePolicyServiceConnection conn = new DevicePolicyServiceConnection(userId, service.getComponentName());
                this.mConnections.put(userId, conn);
                conn.bind();
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    public void stopServiceForOwner(int userId, String actionForLog) {
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                disconnectServiceOnUserLocked(userId, actionForLog);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private void disconnectServiceOnUserLocked(int userId, String actionForLog) {
        DevicePolicyServiceConnection conn = this.mConnections.get(userId);
        if (conn != null) {
            conn.unbind();
            this.mConnections.remove(userId);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mConnections.size() == 0) {
                return;
            }
            pw.println("Owner Services:");
            pw.increaseIndent();
            for (int i = 0; i < this.mConnections.size(); i++) {
                int userId = this.mConnections.keyAt(i);
                pw.print("User: ");
                pw.println(userId);
                DevicePolicyServiceConnection con = this.mConnections.valueAt(i);
                pw.increaseIndent();
                con.dump("", pw);
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
        }
    }
}
