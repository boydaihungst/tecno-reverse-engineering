package com.android.server.ambientcontext;

import android.app.PendingIntent;
import android.app.ambientcontext.AmbientContextEventRequest;
import android.app.ambientcontext.IAmbientContextManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.os.RemoteCallback;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public class AmbientContextManagerService extends AbstractMasterSystemService<AmbientContextManagerService, AmbientContextManagerPerUserService> {
    private static final boolean DEFAULT_SERVICE_ENABLED = true;
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    public static final int MAX_TEMPORARY_SERVICE_DURATION_MS = 30000;
    private static final String TAG = AmbientContextManagerService.class.getSimpleName();
    private final Context mContext;
    private Set<ClientRequest> mExistingClientRequests;
    boolean mIsServiceEnabled;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class ClientRequest {
        private final RemoteCallback mClientStatusCallback;
        private final PendingIntent mPendingIntent;
        private final AmbientContextEventRequest mRequest;
        private final int mUserId;

        ClientRequest(int userId, AmbientContextEventRequest request, PendingIntent pendingIntent, RemoteCallback clientStatusCallback) {
            this.mUserId = userId;
            this.mRequest = request;
            this.mPendingIntent = pendingIntent;
            this.mClientStatusCallback = clientStatusCallback;
        }

        String getPackageName() {
            return this.mPendingIntent.getCreatorPackage();
        }

        AmbientContextEventRequest getRequest() {
            return this.mRequest;
        }

        PendingIntent getPendingIntent() {
            return this.mPendingIntent;
        }

        RemoteCallback getClientStatusCallback() {
            return this.mClientStatusCallback;
        }

        boolean hasUserId(int userId) {
            return this.mUserId == userId;
        }

        boolean hasUserIdAndPackageName(int userId, String packageName) {
            return userId == this.mUserId && packageName.equals(getPackageName());
        }
    }

    public AmbientContextManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039917), null, 68);
        this.mContext = context;
        this.mExistingClientRequests = new ArraySet();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("ambient_context", new AmbientContextManagerInternal());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService, com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            DeviceConfig.addOnPropertiesChangedListener("ambient_context_manager_service", getContext().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.ambientcontext.AmbientContextManagerService$$ExternalSyntheticLambda0
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    AmbientContextManagerService.this.m1532x99343820(properties);
                }
            });
            this.mIsServiceEnabled = DeviceConfig.getBoolean("ambient_context_manager_service", KEY_SERVICE_ENABLED, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-ambientcontext-AmbientContextManagerService  reason: not valid java name */
    public /* synthetic */ void m1532x99343820(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void newClientAdded(int userId, AmbientContextEventRequest request, PendingIntent pendingIntent, RemoteCallback clientStatusCallback) {
        Slog.d(TAG, "New client added: " + pendingIntent.getCreatorPackage());
        this.mExistingClientRequests.removeAll(findExistingRequests(userId, pendingIntent.getCreatorPackage()));
        this.mExistingClientRequests.add(new ClientRequest(userId, request, pendingIntent, clientStatusCallback));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clientRemoved(int userId, String packageName) {
        Slog.d(TAG, "Remove client: " + packageName);
        this.mExistingClientRequests.removeAll(findExistingRequests(userId, packageName));
    }

    private Set<ClientRequest> findExistingRequests(int userId, String packageName) {
        Set<ClientRequest> existingRequests = new ArraySet<>();
        for (ClientRequest clientRequest : this.mExistingClientRequests) {
            if (clientRequest.hasUserIdAndPackageName(userId, packageName)) {
                existingRequests.add(clientRequest);
            }
        }
        return existingRequests;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingIntent getPendingIntent(int userId, String packageName) {
        for (ClientRequest clientRequest : this.mExistingClientRequests) {
            if (clientRequest.hasUserIdAndPackageName(userId, packageName)) {
                return clientRequest.getPendingIntent();
            }
        }
        return null;
    }

    private void onDeviceConfigChange(Set<String> keys) {
        if (keys.contains(KEY_SERVICE_ENABLED)) {
            this.mIsServiceEnabled = DeviceConfig.getBoolean("ambient_context_manager_service", KEY_SERVICE_ENABLED, true);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public AmbientContextManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new AmbientContextManagerPerUserService(this, this.mLock, resolvedUserId);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void onServiceRemoved(AmbientContextManagerPerUserService service, int userId) {
        Slog.d(TAG, "onServiceRemoved");
        service.destroyLocked();
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        Slog.d(TAG, "Restoring remote request. Reason: Service package restarted.");
        restorePreviouslyEnabledClients(userId);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        Slog.d(TAG, "Restoring remote request. Reason: Service package updated.");
        restorePreviouslyEnabledClients(userId);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return 30000;
    }

    public static boolean isDetectionServiceConfigured() {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        String[] packageNames = pmi.getKnownPackageNames(18, 0);
        boolean isServiceConfigured = packageNames.length != 0;
        Slog.i(TAG, "Detection service configured: " + isServiceConfigured);
        return isServiceConfigured;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startDetection(int userId, AmbientContextEventRequest request, String packageName, RemoteCallback detectionResultCallback, RemoteCallback statusCallback) {
        Context context = this.mContext;
        String str = TAG;
        context.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", str);
        synchronized (this.mLock) {
            AmbientContextManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                service.startDetection(request, packageName, detectionResultCallback, statusCallback);
            } else {
                Slog.i(str, "service not available for user_id: " + userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopAmbientContextEvent(int userId, String packageName) {
        Context context = this.mContext;
        String str = TAG;
        context.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", str);
        synchronized (this.mLock) {
            AmbientContextManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                service.stopDetection(packageName);
            } else {
                Slog.i(str, "service not available for user_id: " + userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void queryServiceStatus(int userId, String packageName, int[] eventTypes, RemoteCallback callback) {
        Context context = this.mContext;
        String str = TAG;
        context.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", str);
        synchronized (this.mLock) {
            AmbientContextManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                service.onQueryServiceStatus(eventTypes, packageName, callback);
            } else {
                Slog.i(str, "query service not available for user_id: " + userId);
            }
        }
    }

    private void restorePreviouslyEnabledClients(int userId) {
        synchronized (this.mLock) {
            AmbientContextManagerPerUserService service = getServiceForUserLocked(userId);
            for (ClientRequest clientRequest : this.mExistingClientRequests) {
                if (clientRequest.hasUserId(userId)) {
                    Slog.d(TAG, "Restoring detection for " + clientRequest.getPackageName());
                    service.startDetection(clientRequest.getRequest(), clientRequest.getPackageName(), service.createDetectionResultRemoteCallback(), clientRequest.getClientStatusCallback());
                }
            }
        }
    }

    public ComponentName getComponentName(int userId) {
        synchronized (this.mLock) {
            AmbientContextManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                return service.getComponentName();
            }
            return null;
        }
    }

    /* loaded from: classes.dex */
    private final class AmbientContextManagerInternal extends IAmbientContextManager.Stub {
        final AmbientContextManagerPerUserService mService;

        private AmbientContextManagerInternal() {
            this.mService = (AmbientContextManagerPerUserService) AmbientContextManagerService.this.getServiceForUserLocked(UserHandle.getCallingUserId());
        }

        public void registerObserver(AmbientContextEventRequest request, PendingIntent resultPendingIntent, RemoteCallback statusCallback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(resultPendingIntent);
            Objects.requireNonNull(statusCallback);
            AmbientContextManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", AmbientContextManagerService.TAG);
            AmbientContextManagerService.this.assertCalledByPackageOwner(resultPendingIntent.getCreatorPackage());
            if (!AmbientContextManagerService.this.mIsServiceEnabled) {
                Slog.w(AmbientContextManagerService.TAG, "Service not available.");
                this.mService.sendStatusCallback(statusCallback, 3);
                return;
            }
            this.mService.onRegisterObserver(request, resultPendingIntent, statusCallback);
        }

        public void unregisterObserver(String callingPackage) {
            AmbientContextManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", AmbientContextManagerService.TAG);
            AmbientContextManagerService.this.assertCalledByPackageOwner(callingPackage);
            this.mService.onUnregisterObserver(callingPackage);
        }

        public void queryServiceStatus(int[] eventTypes, String callingPackage, RemoteCallback statusCallback) {
            Objects.requireNonNull(eventTypes);
            Objects.requireNonNull(callingPackage);
            Objects.requireNonNull(statusCallback);
            AmbientContextManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", AmbientContextManagerService.TAG);
            AmbientContextManagerService.this.assertCalledByPackageOwner(callingPackage);
            if (!AmbientContextManagerService.this.mIsServiceEnabled) {
                Slog.w(AmbientContextManagerService.TAG, "Detection service not available.");
                this.mService.sendStatusToCallback(statusCallback, 3);
                return;
            }
            this.mService.onQueryServiceStatus(eventTypes, callingPackage, statusCallback);
        }

        public void startConsentActivity(int[] eventTypes, String callingPackage) {
            Objects.requireNonNull(eventTypes);
            Objects.requireNonNull(callingPackage);
            AmbientContextManagerService.this.assertCalledByPackageOwner(callingPackage);
            AmbientContextManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_CONTEXT_EVENT", AmbientContextManagerService.TAG);
            this.mService.onStartConsentActivity(eventTypes, callingPackage);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(AmbientContextManagerService.this.mContext, AmbientContextManagerService.TAG, pw)) {
                synchronized (AmbientContextManagerService.this.mLock) {
                    AmbientContextManagerService.this.dumpLocked("", pw);
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.ambientcontext.AmbientContextManagerService$AmbientContextManagerInternal */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new AmbientContextShellCommand(AmbientContextManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }
}
