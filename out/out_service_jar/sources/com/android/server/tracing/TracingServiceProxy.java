package com.android.server.tracing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IMessenger;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.tracing.ITracingServiceProxy;
import android.tracing.TraceReportParams;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.SystemService;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class TracingServiceProxy extends SystemService {
    private static final String INTENT_ACTION_NOTIFY_SESSION_STOLEN = "com.android.traceur.NOTIFY_SESSION_STOLEN";
    private static final String INTENT_ACTION_NOTIFY_SESSION_STOPPED = "com.android.traceur.NOTIFY_SESSION_STOPPED";
    private static final int MAX_CACHED_REPORTER_SERVICES = 8;
    private static final int MAX_FILE_SIZE_BYTES_TO_PIPE = 1024;
    private static final int REPORT_BEGIN = 1;
    private static final int REPORT_BIND_PERM_INCORRECT = 3;
    private static final int REPORT_SVC_COMM_ERROR = 5;
    private static final int REPORT_SVC_HANDOFF = 2;
    private static final int REPORT_SVC_PERM_MISSING = 4;
    private static final String TAG = "TracingServiceProxy";
    private static final String TRACING_APP_ACTIVITY = "com.android.traceur.StopTraceService";
    private static final String TRACING_APP_PACKAGE_NAME = "com.android.traceur";
    public static final String TRACING_SERVICE_PROXY_BINDER_NAME = "tracing.proxy";
    private final LruCache<ComponentName, ServiceConnector<IMessenger>> mCachedReporterServices;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final ITracingServiceProxy.Stub mTracingServiceProxy;

    public TracingServiceProxy(Context context) {
        super(context);
        this.mTracingServiceProxy = new ITracingServiceProxy.Stub() { // from class: com.android.server.tracing.TracingServiceProxy.1
            public void notifyTraceSessionEnded(boolean sessionStolen) {
                TracingServiceProxy.this.notifyTraceur(sessionStolen);
            }

            public void reportTrace(TraceReportParams params) {
                TracingServiceProxy.this.reportTrace(params);
            }
        };
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mCachedReporterServices = new LruCache<>(8);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(TRACING_SERVICE_PROXY_BINDER_NAME, this.mTracingServiceProxy);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyTraceur(boolean sessionStolen) {
        Intent intent = new Intent();
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(TRACING_APP_PACKAGE_NAME, 1048576);
            intent.setClassName(info.packageName, TRACING_APP_ACTIVITY);
            if (sessionStolen) {
                intent.setAction(INTENT_ACTION_NOTIFY_SESSION_STOLEN);
            } else {
                intent.setAction(INTENT_ACTION_NOTIFY_SESSION_STOPPED);
            }
            long identity = Binder.clearCallingIdentity();
            try {
                this.mContext.startForegroundServiceAsUser(intent, UserHandle.SYSTEM);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to notifyTraceSessionEnded", e);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(TAG, "Failed to locate Traceur", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportTrace(TraceReportParams params) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.TRACING_SERVICE_REPORT_EVENT, 1, params.uuidLsb, params.uuidMsb);
        ComponentName component = new ComponentName(params.reporterPackageName, params.reporterClassName);
        if (!hasBindServicePermission(component)) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.TRACING_SERVICE_REPORT_EVENT, 3, params.uuidLsb, params.uuidMsb);
            return;
        }
        boolean hasDumpPermission = hasPermission(component, "android.permission.DUMP");
        boolean hasUsageStatsPermission = hasPermission(component, "android.permission.PACKAGE_USAGE_STATS");
        if (!hasDumpPermission || !hasUsageStatsPermission) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.TRACING_SERVICE_REPORT_EVENT, 4, params.uuidLsb, params.uuidMsb);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            reportTrace(getOrCreateReporterService(component), params);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void reportTrace(ServiceConnector<IMessenger> reporterService, final TraceReportParams params) {
        reporterService.post(new ServiceConnector.VoidJob() { // from class: com.android.server.tracing.TracingServiceProxy$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                TracingServiceProxy.lambda$reportTrace$0(params, (IMessenger) obj);
            }
        }).whenComplete(new BiConsumer() { // from class: com.android.server.tracing.TracingServiceProxy$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                TracingServiceProxy.lambda$reportTrace$1(params, (Void) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reportTrace$0(TraceReportParams params, IMessenger messenger) throws Exception {
        if (params.usePipeForTesting) {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor.AutoCloseInputStream i = new ParcelFileDescriptor.AutoCloseInputStream(params.fd);
            try {
                ParcelFileDescriptor.AutoCloseOutputStream o = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
                byte[] array = i.readNBytes(1024);
                if (array.length == 1024) {
                    throw new IllegalArgumentException("Trace file too large when |usePipeForTesting| is set.");
                }
                o.write(array);
                o.close();
                i.close();
                params.fd = pipe[0];
            } catch (Throwable th) {
                try {
                    i.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        Message message = Message.obtain();
        message.what = 1;
        message.obj = params;
        messenger.send(message);
        FrameworkStatsLog.write((int) FrameworkStatsLog.TRACING_SERVICE_REPORT_EVENT, 2, params.uuidLsb, params.uuidMsb);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reportTrace$1(TraceReportParams params, Void res, Throwable err) {
        if (err != null) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.TRACING_SERVICE_REPORT_EVENT, 5, params.uuidLsb, params.uuidMsb);
            Slog.e(TAG, "Failed to report trace", err);
        }
        try {
            params.fd.close();
        } catch (IOException e) {
        }
    }

    private ServiceConnector<IMessenger> getOrCreateReporterService(ComponentName component) {
        ServiceConnector<IMessenger> connector = this.mCachedReporterServices.get(component);
        if (connector == null) {
            Intent intent = new Intent();
            intent.setComponent(component);
            Context context = this.mContext;
            ServiceConnector<IMessenger> connector2 = new ServiceConnector.Impl<IMessenger>(context, intent, 33, context.getUser().getIdentifier(), new Function() { // from class: com.android.server.tracing.TracingServiceProxy$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return IMessenger.Stub.asInterface((IBinder) obj);
                }
            }) { // from class: com.android.server.tracing.TracingServiceProxy.2
                private static final long DISCONNECT_TIMEOUT_MS = 15000;
                private static final long REQUEST_TIMEOUT_MS = 10000;

                protected long getAutoDisconnectTimeoutMs() {
                    return DISCONNECT_TIMEOUT_MS;
                }

                protected long getRequestTimeoutMs() {
                    return 10000L;
                }
            };
            this.mCachedReporterServices.put(intent.getComponent(), connector2);
            return connector2;
        }
        return connector;
    }

    private boolean hasPermission(ComponentName componentName, String permission) throws SecurityException {
        if (this.mPackageManager.checkPermission(permission, componentName.getPackageName()) != 0) {
            Slog.e(TAG, "Trace reporting service " + componentName.toShortString() + " does not have " + permission + " permission");
            return false;
        }
        return true;
    }

    private boolean hasBindServicePermission(ComponentName componentName) {
        try {
            ServiceInfo info = this.mPackageManager.getServiceInfo(componentName, 0);
            if (!"android.permission.BIND_TRACE_REPORT_SERVICE".equals(info.permission)) {
                Slog.e(TAG, "Trace reporting service " + componentName.toShortString() + " does not request android.permission.BIND_TRACE_REPORT_SERVICE permission; instead requests " + info.permission);
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Trace reporting service " + componentName.toShortString() + " does not exist");
            return false;
        }
    }
}
