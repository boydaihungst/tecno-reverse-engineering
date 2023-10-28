package com.android.server.profcollect;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.provider.DeviceConfig;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.profcollect.IProfCollectd;
import com.android.server.profcollect.IProviderStatusCallback;
import com.android.server.profcollect.ProfcollectForwardingService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.vibrator.VibratorManagerService;
import com.android.server.wm.ActivityMetricsLaunchObserver;
import com.android.server.wm.ActivityMetricsLaunchObserverRegistry;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public final class ProfcollectForwardingService extends SystemService {
    private static ProfcollectForwardingService sSelfService;
    private final AppLaunchObserver mAppLaunchObserver;
    private final Handler mHandler;
    private IProfCollectd mIProfcollect;
    private IProviderStatusCallback mProviderStatusCallback;
    public static final String LOG_TAG = "ProfcollectForwardingService";
    private static final boolean DEBUG = Log.isLoggable(LOG_TAG, 3);
    private static final long BG_PROCESS_PERIOD = TimeUnit.HOURS.toMillis(4);

    public ProfcollectForwardingService(Context context) {
        super(context);
        this.mHandler = new ProfcollectdHandler(IoThread.getHandler().getLooper());
        this.mProviderStatusCallback = new IProviderStatusCallback.Stub() { // from class: com.android.server.profcollect.ProfcollectForwardingService.1
            @Override // com.android.server.profcollect.IProviderStatusCallback
            public void onProviderReady() {
                ProfcollectForwardingService.this.mHandler.sendEmptyMessage(1);
            }
        };
        this.mAppLaunchObserver = new AppLaunchObserver();
        if (sSelfService != null) {
            throw new AssertionError("only one service instance allowed");
        }
        sSelfService = this;
    }

    public static boolean enabled() {
        return DeviceConfig.getBoolean("profcollect_native_boot", ServiceConfigAccessor.PROVIDER_MODE_ENABLED, false) || SystemProperties.getBoolean("persist.profcollectd.enabled_override", false);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        if (DEBUG) {
            Log.d(LOG_TAG, "Profcollect forwarding service start");
        }
        connectNativeService();
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase != 1000 || this.mIProfcollect == null) {
            return;
        }
        BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.profcollect.ProfcollectForwardingService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ProfcollectForwardingService.this.m6340xc1c651f8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-profcollect-ProfcollectForwardingService  reason: not valid java name */
    public /* synthetic */ void m6340xc1c651f8() {
        if (serviceHasSupportedTraceProvider()) {
            registerProviderStatusCallback();
        }
    }

    private void registerProviderStatusCallback() {
        IProfCollectd iProfCollectd = this.mIProfcollect;
        if (iProfCollectd == null) {
            return;
        }
        try {
            iProfCollectd.registerProviderStatusCallback(this.mProviderStatusCallback);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to register provider status callback: " + e.getMessage());
        }
    }

    private boolean serviceHasSupportedTraceProvider() {
        IProfCollectd iProfCollectd = this.mIProfcollect;
        if (iProfCollectd == null) {
            return false;
        }
        try {
            return !iProfCollectd.get_supported_provider().isEmpty();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to get supported provider: " + e.getMessage());
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean tryConnectNativeService() {
        if (connectNativeService()) {
            return true;
        }
        this.mHandler.sendEmptyMessageDelayed(0, 5000L);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean connectNativeService() {
        try {
            IProfCollectd profcollectd = IProfCollectd.Stub.asInterface(ServiceManager.getServiceOrThrow("profcollectd"));
            profcollectd.asBinder().linkToDeath(new ProfcollectdDeathRecipient(), 0);
            this.mIProfcollect = profcollectd;
            return true;
        } catch (ServiceManager.ServiceNotFoundException | RemoteException e) {
            Log.w(LOG_TAG, "Failed to connect profcollectd binder service.");
            return false;
        }
    }

    /* loaded from: classes2.dex */
    private class ProfcollectdHandler extends Handler {
        public static final int MESSAGE_BINDER_CONNECT = 0;
        public static final int MESSAGE_REGISTER_SCHEDULERS = 1;

        public ProfcollectdHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    ProfcollectForwardingService.this.connectNativeService();
                    return;
                case 1:
                    ProfcollectForwardingService.this.registerObservers();
                    ProfcollectBGJobService.schedule(ProfcollectForwardingService.this.getContext());
                    return;
                default:
                    throw new AssertionError("Unknown message: " + message);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ProfcollectdDeathRecipient implements IBinder.DeathRecipient {
        private ProfcollectdDeathRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.w(ProfcollectForwardingService.LOG_TAG, "profcollectd has died");
            ProfcollectForwardingService.this.mIProfcollect = null;
            ProfcollectForwardingService.this.tryConnectNativeService();
        }
    }

    /* loaded from: classes2.dex */
    public static class ProfcollectBGJobService extends JobService {
        private static final int JOB_IDLE_PROCESS = 260817;
        private static final ComponentName JOB_SERVICE_NAME = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, ProfcollectBGJobService.class.getName());

        public static void schedule(Context context) {
            JobScheduler js = (JobScheduler) context.getSystemService(JobScheduler.class);
            js.schedule(new JobInfo.Builder(JOB_IDLE_PROCESS, JOB_SERVICE_NAME).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(ProfcollectForwardingService.BG_PROCESS_PERIOD).build());
        }

        @Override // android.app.job.JobService
        public boolean onStartJob(JobParameters params) {
            if (ProfcollectForwardingService.DEBUG) {
                Log.d(ProfcollectForwardingService.LOG_TAG, "Starting background process job");
            }
            BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.profcollect.ProfcollectForwardingService$ProfcollectBGJobService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ProfcollectForwardingService.ProfcollectBGJobService.lambda$onStartJob$0();
                }
            });
            return true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onStartJob$0() {
            try {
                ProfcollectForwardingService.sSelfService.mIProfcollect.process();
            } catch (RemoteException e) {
                Log.e(ProfcollectForwardingService.LOG_TAG, "Failed to process profiles in background: " + e.getMessage());
            }
        }

        @Override // android.app.job.JobService
        public boolean onStopJob(JobParameters params) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerObservers() {
        BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.profcollect.ProfcollectForwardingService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ProfcollectForwardingService.this.m6342xa2a396a9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerObservers$1$com-android-server-profcollect-ProfcollectForwardingService  reason: not valid java name */
    public /* synthetic */ void m6342xa2a396a9() {
        registerAppLaunchObserver();
        registerOTAObserver();
    }

    private void registerAppLaunchObserver() {
        ActivityTaskManagerInternal atmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        ActivityMetricsLaunchObserverRegistry launchObserverRegistry = atmInternal.getLaunchObserverRegistry();
        launchObserverRegistry.registerLaunchObserver(this.mAppLaunchObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void traceOnAppStart(String packageName) {
        if (this.mIProfcollect == null) {
            return;
        }
        int traceFrequency = DeviceConfig.getInt("profcollect_native_boot", "applaunch_trace_freq", 2);
        int randomNum = ThreadLocalRandom.current().nextInt(100);
        if (randomNum < traceFrequency) {
            if (DEBUG) {
                Log.d(LOG_TAG, "Tracing on app launch event: " + packageName);
            }
            BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.profcollect.ProfcollectForwardingService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ProfcollectForwardingService.this.m6343x12c1ee95();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$traceOnAppStart$2$com-android-server-profcollect-ProfcollectForwardingService  reason: not valid java name */
    public /* synthetic */ void m6343x12c1ee95() {
        try {
            this.mIProfcollect.trace_once("applaunch");
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to initiate trace: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AppLaunchObserver extends ActivityMetricsLaunchObserver {
        private AppLaunchObserver() {
        }

        @Override // com.android.server.wm.ActivityMetricsLaunchObserver
        public void onIntentStarted(Intent intent, long timestampNanos) {
            ProfcollectForwardingService.this.traceOnAppStart(intent.getPackage());
        }
    }

    private void registerOTAObserver() {
        UpdateEngine updateEngine = new UpdateEngine();
        updateEngine.bind(new UpdateEngineCallback() { // from class: com.android.server.profcollect.ProfcollectForwardingService.2
            public void onStatusUpdate(int status, float percent) {
                if (ProfcollectForwardingService.DEBUG) {
                    Log.d(ProfcollectForwardingService.LOG_TAG, "Received OTA status update, status: " + status + ", percent: " + percent);
                }
                if (status == 6) {
                    ProfcollectForwardingService.this.packProfileReport();
                }
            }

            public void onPayloadApplicationComplete(int errorCode) {
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void packProfileReport() {
        if (this.mIProfcollect == null) {
            return;
        }
        final Context context = getContext();
        BackgroundThread.get().getThreadHandler().post(new Runnable() { // from class: com.android.server.profcollect.ProfcollectForwardingService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProfcollectForwardingService.this.m6341x5f1f0fd5(context);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$packProfileReport$3$com-android-server-profcollect-ProfcollectForwardingService  reason: not valid java name */
    public /* synthetic */ void m6341x5f1f0fd5(Context context) {
        try {
            String reportName = this.mIProfcollect.report() + ".zip";
            if (!context.getResources().getBoolean(17891728)) {
                Log.i(LOG_TAG, "Upload is not enabled.");
                return;
            }
            Intent intent = new Intent().setPackage(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME).setAction("com.android.shell.action.PROFCOLLECT_UPLOAD").putExtra("filename", reportName);
            context.sendBroadcast(intent);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to upload report: " + e.getMessage());
        }
    }
}
