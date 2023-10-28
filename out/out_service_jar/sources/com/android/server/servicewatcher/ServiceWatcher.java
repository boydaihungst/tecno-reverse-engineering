package com.android.server.servicewatcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.server.FgThread;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes2.dex */
public interface ServiceWatcher {

    /* loaded from: classes2.dex */
    public interface ServiceChangedListener {
        void onServiceChanged();
    }

    /* loaded from: classes2.dex */
    public interface ServiceListener<TBoundServiceInfo extends BoundServiceInfo> {
        void onBind(IBinder iBinder, TBoundServiceInfo tboundserviceinfo) throws RemoteException;

        void onUnbind();
    }

    /* loaded from: classes2.dex */
    public interface ServiceSupplier<TBoundServiceInfo extends BoundServiceInfo> {
        TBoundServiceInfo getServiceInfo();

        boolean hasMatchingService();

        void register(ServiceChangedListener serviceChangedListener);

        void unregister();
    }

    boolean checkServiceResolves();

    void dump(PrintWriter printWriter);

    void register();

    void runOnBinder(BinderOperation binderOperation);

    void unregister();

    /* loaded from: classes2.dex */
    public interface BinderOperation {
        void run(IBinder iBinder) throws RemoteException;

        default void onError(Throwable t) {
        }
    }

    /* loaded from: classes2.dex */
    public static class BoundServiceInfo {
        protected final String mAction;
        protected final ComponentName mComponentName;
        protected final int mUid;

        protected BoundServiceInfo(String action, ResolveInfo resolveInfo) {
            this(action, resolveInfo.serviceInfo.applicationInfo.uid, resolveInfo.serviceInfo.getComponentName());
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public BoundServiceInfo(String action, int uid, ComponentName componentName) {
            this.mAction = action;
            this.mUid = uid;
            this.mComponentName = (ComponentName) Objects.requireNonNull(componentName);
        }

        public String getAction() {
            return this.mAction;
        }

        public ComponentName getComponentName() {
            return this.mComponentName;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.mUid);
        }

        public final boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BoundServiceInfo) {
                BoundServiceInfo that = (BoundServiceInfo) o;
                return this.mUid == that.mUid && Objects.equals(this.mAction, that.mAction) && this.mComponentName.equals(that.mComponentName);
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(this.mAction, Integer.valueOf(this.mUid), this.mComponentName);
        }

        public String toString() {
            if (this.mComponentName == null) {
                return "none";
            }
            return this.mUid + SliceClientPermissions.SliceAuthority.DELIMITER + this.mComponentName.flattenToShortString();
        }
    }

    static <TBoundServiceInfo extends BoundServiceInfo> ServiceWatcher create(Context context, String tag, ServiceSupplier<TBoundServiceInfo> serviceSupplier, ServiceListener<? super TBoundServiceInfo> serviceListener) {
        return create(context, FgThread.getHandler(), tag, serviceSupplier, serviceListener);
    }

    static <TBoundServiceInfo extends BoundServiceInfo> ServiceWatcher create(Context context, Handler handler, String tag, ServiceSupplier<TBoundServiceInfo> serviceSupplier, ServiceListener<? super TBoundServiceInfo> serviceListener) {
        return new ServiceWatcherImpl(context, handler, tag, serviceSupplier, serviceListener);
    }
}
