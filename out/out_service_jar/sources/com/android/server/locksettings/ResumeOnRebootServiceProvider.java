package com.android.server.locksettings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelableException;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.resumeonreboot.IResumeOnRebootService;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/* loaded from: classes.dex */
public class ResumeOnRebootServiceProvider {
    static final String PROP_ROR_PROVIDER_PACKAGE = "persist.sys.resume_on_reboot_provider_package";
    private static final String PROVIDER_PACKAGE = DeviceConfig.getString("ota", "resume_on_reboot_service_package", "");
    private static final String PROVIDER_REQUIRED_PERMISSION = "android.permission.BIND_RESUME_ON_REBOOT_SERVICE";
    private static final String TAG = "ResumeOnRebootServiceProvider";
    private final Context mContext;
    private final PackageManager mPackageManager;

    public ResumeOnRebootServiceProvider(Context context) {
        this(context, context.getPackageManager());
    }

    public ResumeOnRebootServiceProvider(Context context, PackageManager packageManager) {
        this.mContext = context;
        this.mPackageManager = packageManager;
    }

    private ServiceInfo resolveService() {
        Intent intent = new Intent();
        intent.setAction("android.service.resumeonreboot.ResumeOnRebootService");
        int queryFlag = 4;
        String testAppName = SystemProperties.get(PROP_ROR_PROVIDER_PACKAGE, "");
        if (testAppName.isEmpty()) {
            queryFlag = 4 | 1048576;
            String str = PROVIDER_PACKAGE;
            if (str != null && !str.equals("")) {
                intent.setPackage(str);
            }
        } else {
            Slog.i(TAG, "Using test app: " + testAppName);
            intent.setPackage(testAppName);
        }
        List<ResolveInfo> resolvedIntents = this.mPackageManager.queryIntentServices(intent, queryFlag);
        for (ResolveInfo resolvedInfo : resolvedIntents) {
            if (resolvedInfo.serviceInfo != null && PROVIDER_REQUIRED_PERMISSION.equals(resolvedInfo.serviceInfo.permission)) {
                return resolvedInfo.serviceInfo;
            }
        }
        return null;
    }

    public ResumeOnRebootServiceConnection getServiceConnection() {
        ServiceInfo serviceInfo = resolveService();
        if (serviceInfo == null) {
            return null;
        }
        return new ResumeOnRebootServiceConnection(this.mContext, serviceInfo.getComponentName());
    }

    /* loaded from: classes.dex */
    public static class ResumeOnRebootServiceConnection {
        private static final String TAG = "ResumeOnRebootServiceConnection";
        private IResumeOnRebootService mBinder;
        private final ComponentName mComponentName;
        private final Context mContext;
        ServiceConnection mServiceConnection;

        private ResumeOnRebootServiceConnection(Context context, ComponentName componentName) {
            this.mContext = context;
            this.mComponentName = componentName;
        }

        public void unbindService() {
            ServiceConnection serviceConnection = this.mServiceConnection;
            if (serviceConnection != null) {
                this.mContext.unbindService(serviceConnection);
            }
            this.mBinder = null;
        }

        public void bindToService(long timeOut) throws RemoteException, TimeoutException {
            IResumeOnRebootService iResumeOnRebootService = this.mBinder;
            if (iResumeOnRebootService == null || !iResumeOnRebootService.asBinder().isBinderAlive()) {
                final CountDownLatch connectionLatch = new CountDownLatch(1);
                Intent intent = new Intent();
                intent.setComponent(this.mComponentName);
                ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.locksettings.ResumeOnRebootServiceProvider.ResumeOnRebootServiceConnection.1
                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        ResumeOnRebootServiceConnection.this.mBinder = IResumeOnRebootService.Stub.asInterface(service);
                        connectionLatch.countDown();
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName name) {
                        ResumeOnRebootServiceConnection.this.mBinder = null;
                    }
                };
                this.mServiceConnection = serviceConnection;
                boolean success = this.mContext.bindServiceAsUser(intent, serviceConnection, AudioFormat.AAC_MAIN, BackgroundThread.getHandler(), UserHandle.SYSTEM);
                if (!success) {
                    Slog.e(TAG, "Binding: " + this.mComponentName + " u" + UserHandle.SYSTEM + " failed.");
                } else {
                    waitForLatch(connectionLatch, "serviceConnection", timeOut);
                }
            }
        }

        public byte[] wrapBlob(byte[] unwrappedBlob, long lifeTimeInMillis, long timeOutInMillis) throws RemoteException, TimeoutException, IOException {
            IResumeOnRebootService iResumeOnRebootService = this.mBinder;
            if (iResumeOnRebootService == null || !iResumeOnRebootService.asBinder().isBinderAlive()) {
                throw new RemoteException("Service not bound");
            }
            CountDownLatch binderLatch = new CountDownLatch(1);
            ResumeOnRebootServiceCallback resultCallback = new ResumeOnRebootServiceCallback(binderLatch);
            this.mBinder.wrapSecret(unwrappedBlob, lifeTimeInMillis, new RemoteCallback(resultCallback));
            waitForLatch(binderLatch, "wrapSecret", timeOutInMillis);
            if (resultCallback.getResult().containsKey("exception_key")) {
                throwTypedException((ParcelableException) resultCallback.getResult().getParcelable("exception_key"));
            }
            return resultCallback.mResult.getByteArray("wrapped_blob_key");
        }

        public byte[] unwrap(byte[] wrappedBlob, long timeOut) throws RemoteException, TimeoutException, IOException {
            IResumeOnRebootService iResumeOnRebootService = this.mBinder;
            if (iResumeOnRebootService == null || !iResumeOnRebootService.asBinder().isBinderAlive()) {
                throw new RemoteException("Service not bound");
            }
            CountDownLatch binderLatch = new CountDownLatch(1);
            ResumeOnRebootServiceCallback resultCallback = new ResumeOnRebootServiceCallback(binderLatch);
            this.mBinder.unwrap(wrappedBlob, new RemoteCallback(resultCallback));
            waitForLatch(binderLatch, "unWrapSecret", timeOut);
            if (resultCallback.getResult().containsKey("exception_key")) {
                throwTypedException((ParcelableException) resultCallback.getResult().getParcelable("exception_key"));
            }
            return resultCallback.getResult().getByteArray("unrwapped_blob_key");
        }

        private void throwTypedException(ParcelableException exception) throws IOException, RemoteException {
            if (exception != null && (exception.getCause() instanceof IOException)) {
                exception.maybeRethrow(IOException.class);
                return;
            }
            throw new RemoteException("ResumeOnRebootServiceConnection wrap/unwrap failed", exception, true, true);
        }

        private void waitForLatch(CountDownLatch latch, String reason, long timeOut) throws RemoteException, TimeoutException {
            try {
                if (!latch.await(timeOut, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Latch wait for " + reason + " elapsed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RemoteException("Latch wait for " + reason + " interrupted");
            }
        }
    }

    /* loaded from: classes.dex */
    private static class ResumeOnRebootServiceCallback implements RemoteCallback.OnResultListener {
        private Bundle mResult;
        private final CountDownLatch mResultLatch;

        private ResumeOnRebootServiceCallback(CountDownLatch resultLatch) {
            this.mResultLatch = resultLatch;
        }

        public void onResult(Bundle result) {
            this.mResult = result;
            this.mResultLatch.countDown();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Bundle getResult() {
            return this.mResult;
        }
    }
}
