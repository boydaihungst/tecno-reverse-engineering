package com.android.server.ambientcontext;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.BroadcastOptions;
import android.app.PendingIntent;
import android.app.ambientcontext.AmbientContextEventRequest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.ambientcontext.AmbientContextDetectionResult;
import android.service.ambientcontext.AmbientContextDetectionServiceStatus;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.server.infra.AbstractPerUserSystemService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AmbientContextManagerPerUserService extends AbstractPerUserSystemService<AmbientContextManagerPerUserService, AmbientContextManagerService> {
    private static final String TAG = AmbientContextManagerPerUserService.class.getSimpleName();
    private ComponentName mComponentName;
    RemoteAmbientContextDetectionService mRemoteService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AmbientContextManagerPerUserService(AmbientContextManagerService master, Object lock, int userId) {
        super(master, lock, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyLocked() {
        Slog.d(TAG, "Trying to cancel the remote request. Reason: Service destroyed.");
        if (this.mRemoteService != null) {
            synchronized (this.mLock) {
                this.mRemoteService.unbind();
                this.mRemoteService = null;
            }
        }
    }

    private void ensureRemoteServiceInitiated() {
        if (this.mRemoteService == null) {
            this.mRemoteService = new RemoteAmbientContextDetectionService(getContext(), this.mComponentName, getUserId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    boolean setUpServiceIfNeeded() {
        if (this.mComponentName == null) {
            this.mComponentName = updateServiceInfoLocked();
        }
        if (this.mComponentName == null) {
            return false;
        }
        try {
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(this.mComponentName, 0L, this.mUserId);
            return serviceInfo != null;
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while setting up service");
            return false;
        }
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 0L, this.mUserId);
            if (serviceInfo != null) {
                String permission = serviceInfo.permission;
                if (!"android.permission.BIND_AMBIENT_CONTEXT_DETECTION_SERVICE".equals(permission)) {
                    throw new SecurityException(String.format("Service %s requires %s permission. Found %s permission", serviceInfo.getComponentName(), "android.permission.BIND_AMBIENT_CONTEXT_DETECTION_SERVICE", serviceInfo.permission));
                }
            }
            return serviceInfo;
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public void dumpLocked(String prefix, PrintWriter pw) {
        synchronized (this.mLock) {
            super.dumpLocked(prefix, pw);
        }
        RemoteAmbientContextDetectionService remoteAmbientContextDetectionService = this.mRemoteService;
        if (remoteAmbientContextDetectionService != null) {
            remoteAmbientContextDetectionService.dump("", new IndentingPrintWriter(pw, "  "));
        }
    }

    public void onRegisterObserver(AmbientContextEventRequest request, PendingIntent pendingIntent, RemoteCallback clientStatusCallback) {
        synchronized (this.mLock) {
            if (!setUpServiceIfNeeded()) {
                Slog.w(TAG, "Detection service is not available at this moment.");
                sendStatusCallback(clientStatusCallback, 3);
                return;
            }
            startDetection(request, pendingIntent.getCreatorPackage(), createDetectionResultRemoteCallback(), clientStatusCallback);
            ((AmbientContextManagerService) this.mMaster).newClientAdded(this.mUserId, request, pendingIntent, clientStatusCallback);
        }
    }

    private RemoteCallback getServerStatusCallback(final RemoteCallback clientStatusCallback) {
        return new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ambientcontext.AmbientContextManagerPerUserService$$ExternalSyntheticLambda1
            public final void onResult(Bundle bundle) {
                AmbientContextManagerPerUserService.lambda$getServerStatusCallback$0(clientStatusCallback, bundle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getServerStatusCallback$0(RemoteCallback clientStatusCallback, Bundle result) {
        AmbientContextDetectionServiceStatus serviceStatus = (AmbientContextDetectionServiceStatus) result.get("android.app.ambientcontext.AmbientContextServiceStatusBundleKey");
        long token = Binder.clearCallingIdentity();
        try {
            String packageName = serviceStatus.getPackageName();
            Bundle bundle = new Bundle();
            bundle.putInt("android.app.ambientcontext.AmbientContextStatusBundleKey", serviceStatus.getStatusCode());
            clientStatusCallback.sendResult(bundle);
            int statusCode = serviceStatus.getStatusCode();
            Slog.i(TAG, "Got detection status of " + statusCode + " for " + packageName);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startDetection(AmbientContextEventRequest request, String callingPackage, RemoteCallback detectionResultCallback, RemoteCallback clientStatusCallback) {
        String str = TAG;
        Slog.d(str, "Requested detection of " + request.getEventTypes());
        synchronized (this.mLock) {
            if (setUpServiceIfNeeded()) {
                ensureRemoteServiceInitiated();
                this.mRemoteService.startDetection(request, callingPackage, detectionResultCallback, getServerStatusCallback(clientStatusCallback));
            } else {
                Slog.w(str, "No valid component found for AmbientContextDetectionService");
                sendStatusToCallback(clientStatusCallback, 2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendStatusCallback(RemoteCallback statusCallback, int statusCode) {
        Bundle bundle = new Bundle();
        bundle.putInt("android.app.ambientcontext.AmbientContextStatusBundleKey", statusCode);
        statusCallback.sendResult(bundle);
    }

    public void onUnregisterObserver(String callingPackage) {
        synchronized (this.mLock) {
            stopDetection(callingPackage);
            ((AmbientContextManagerService) this.mMaster).clientRemoved(this.mUserId, callingPackage);
        }
    }

    public void onQueryServiceStatus(int[] eventTypes, String callingPackage, RemoteCallback statusCallback) {
        String str = TAG;
        Slog.d(str, "Query event status of " + Arrays.toString(eventTypes) + " for " + callingPackage);
        synchronized (this.mLock) {
            if (!setUpServiceIfNeeded()) {
                Slog.w(str, "Detection service is not available at this moment.");
                sendStatusToCallback(statusCallback, 2);
                return;
            }
            ensureRemoteServiceInitiated();
            this.mRemoteService.queryServiceStatus(eventTypes, callingPackage, getServerStatusCallback(statusCallback));
        }
    }

    public void onStartConsentActivity(int[] eventTypes, String callingPackage) {
        String str = TAG;
        Slog.d(str, "Opening consent activity of " + Arrays.toString(eventTypes) + " for " + callingPackage);
        int userId = getUserId();
        try {
            ParceledListSlice<ActivityManager.RecentTaskInfo> recentTasks = ActivityTaskManager.getService().getRecentTasks(1, 0, userId);
            if (recentTasks == null || recentTasks.getList().isEmpty()) {
                Slog.e(str, "Recent task list is empty!");
                return;
            }
            ActivityManager.RecentTaskInfo task = (ActivityManager.RecentTaskInfo) recentTasks.getList().get(0);
            if (!callingPackage.equals(task.topActivityInfo.packageName)) {
                Slog.e(str, "Recent task package name: " + task.topActivityInfo.packageName + " doesn't match with client package name: " + callingPackage);
                return;
            }
            ComponentName consentComponent = getConsentComponent();
            if (consentComponent == null) {
                Slog.e(str, "Consent component not found!");
                return;
            }
            Slog.d(str, "Starting consent activity for " + callingPackage);
            Intent intent = new Intent();
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    Context context = getContext();
                    String packageNameExtraKey = context.getResources().getString(17039882);
                    String eventArrayExtraKey = context.getResources().getString(17039881);
                    intent.setComponent(consentComponent);
                    if (packageNameExtraKey != null) {
                        intent.putExtra(packageNameExtraKey, callingPackage);
                    } else {
                        Slog.d(str, "Missing packageNameExtraKey for consent activity");
                    }
                    if (eventArrayExtraKey != null) {
                        intent.putExtra(eventArrayExtraKey, eventTypes);
                    } else {
                        Slog.d(str, "Missing eventArrayExtraKey for consent activity");
                    }
                    ActivityOptions options = ActivityOptions.makeBasic();
                    options.setLaunchTaskId(task.taskId);
                    context.startActivityAsUser(intent, options.toBundle(), context.getUser());
                } catch (ActivityNotFoundException e) {
                    Slog.e(TAG, "unable to start consent activity");
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "Failed to query recent tasks!");
        }
    }

    private ComponentName getConsentComponent() {
        Context context = getContext();
        String consentComponent = context.getResources().getString(17039916);
        if (TextUtils.isEmpty(consentComponent)) {
            return null;
        }
        Slog.i(TAG, "Consent component name: " + consentComponent);
        return ComponentName.unflattenFromString(consentComponent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendStatusToCallback(RemoteCallback callback, int status) {
        Bundle bundle = new Bundle();
        bundle.putInt("android.app.ambientcontext.AmbientContextStatusBundleKey", status);
        callback.sendResult(bundle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopDetection(String packageName) {
        Slog.d(TAG, "Stop detection for " + packageName);
        synchronized (this.mLock) {
            if (this.mComponentName != null) {
                ensureRemoteServiceInitiated();
                this.mRemoteService.stopDetection(packageName);
            }
        }
    }

    private void sendDetectionResultIntent(PendingIntent pendingIntent, AmbientContextDetectionResult result) {
        Intent intent = new Intent();
        intent.putExtra("android.app.ambientcontext.extra.AMBIENT_CONTEXT_EVENTS", new ArrayList(result.getEvents()));
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setPendingIntentBackgroundActivityLaunchAllowed(false);
        try {
            pendingIntent.send(getContext(), 0, intent, null, null, null, options.toBundle());
            Slog.i(TAG, "Sending PendingIntent to " + pendingIntent.getCreatorPackage() + ": " + result);
        } catch (PendingIntent.CanceledException e) {
            Slog.w(TAG, "Couldn't deliver pendingIntent:" + pendingIntent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteCallback createDetectionResultRemoteCallback() {
        return new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ambientcontext.AmbientContextManagerPerUserService$$ExternalSyntheticLambda0
            public final void onResult(Bundle bundle) {
                AmbientContextManagerPerUserService.this.m1529x445772aa(bundle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createDetectionResultRemoteCallback$1$com-android-server-ambientcontext-AmbientContextManagerPerUserService  reason: not valid java name */
    public /* synthetic */ void m1529x445772aa(Bundle result) {
        AmbientContextDetectionResult detectionResult = (AmbientContextDetectionResult) result.get("android.app.ambientcontext.AmbientContextDetectionResultBundleKey");
        String packageName = detectionResult.getPackageName();
        PendingIntent pendingIntent = ((AmbientContextManagerService) this.mMaster).getPendingIntent(this.mUserId, packageName);
        if (pendingIntent == null) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            sendDetectionResultIntent(pendingIntent, detectionResult);
            Slog.i(TAG, "Got detection result of " + detectionResult.getEvents() + " for " + packageName);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
