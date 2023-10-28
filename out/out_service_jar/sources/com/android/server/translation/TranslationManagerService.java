package com.android.server.translation;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.translation.ITranslationManager;
import android.view.translation.TranslationContext;
import android.view.translation.TranslationSpec;
import android.view.translation.UiTranslationSpec;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.SyncResultReceiver;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
public final class TranslationManagerService extends AbstractMasterSystemService<TranslationManagerService, TranslationManagerServiceImpl> {
    private static final int MAX_TEMP_SERVICE_SUBSTITUTION_DURATION_MS = 120000;
    private static final String TAG = "TranslationManagerService";

    public TranslationManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039947), null, 4);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public TranslationManagerServiceImpl newServiceLocked(int resolvedUserId, boolean disabled) {
        return new TranslationManagerServiceImpl(this, this.mLock, resolvedUserId, disabled);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_UI_TRANSLATION", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_SUBSTITUTION_DURATION_MS;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void dumpLocked(String prefix, PrintWriter pw) {
        super.dumpLocked(prefix, pw);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceCallerHasPermission(String permission) {
        String msg = "Permission Denial from pid =" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " doesn't hold " + permission;
        getContext().enforceCallingPermission(permission, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDefaultServiceLocked(int userId) {
        String defaultServiceName = this.mServiceNameResolver.getDefaultServiceName(userId);
        if (defaultServiceName == null) {
            return false;
        }
        String currentServiceName = this.mServiceNameResolver.getServiceName(userId);
        return defaultServiceName.equals(currentServiceName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCalledByServiceAppLocked(int userId, String methodName) {
        int callingUid = Binder.getCallingUid();
        String serviceName = this.mServiceNameResolver.getServiceName(userId);
        if (serviceName == null) {
            Slog.e(TAG, methodName + ": called by UID " + callingUid + ", but there's no service set for user " + userId);
            return false;
        }
        ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
        if (serviceComponent == null) {
            Slog.w(TAG, methodName + ": invalid service name: " + serviceName);
            return false;
        }
        String servicePackageName = serviceComponent.getPackageName();
        PackageManager pm = getContext().getPackageManager();
        try {
            int serviceUid = pm.getPackageUidAsUser(servicePackageName, userId);
            if (callingUid != serviceUid) {
                Slog.e(TAG, methodName + ": called by UID " + callingUid + ", but service UID is " + serviceUid);
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, methodName + ": could not verify UID for " + serviceName);
            return false;
        }
    }

    /* loaded from: classes2.dex */
    final class TranslationManagerServiceStub extends ITranslationManager.Stub {
        TranslationManagerServiceStub() {
        }

        public void onTranslationCapabilitiesRequest(int sourceFormat, int targetFormat, ResultReceiver receiver, int userId) throws RemoteException {
            synchronized (TranslationManagerService.this.mLock) {
                TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                if (service != null && (TranslationManagerService.this.isDefaultServiceLocked(userId) || TranslationManagerService.this.isCalledByServiceAppLocked(userId, "getTranslationCapabilities"))) {
                    service.onTranslationCapabilitiesRequestLocked(sourceFormat, targetFormat, receiver);
                } else {
                    Slog.v(TranslationManagerService.TAG, "onGetTranslationCapabilitiesLocked(): no service for " + userId);
                    receiver.send(2, null);
                }
            }
        }

        public void registerTranslationCapabilityCallback(IRemoteCallback callback, int userId) {
            TranslationManagerServiceImpl service;
            synchronized (TranslationManagerService.this.mLock) {
                service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
            }
            if (service != null) {
                service.registerTranslationCapabilityCallback(callback, Binder.getCallingUid());
            }
        }

        public void unregisterTranslationCapabilityCallback(IRemoteCallback callback, int userId) {
            TranslationManagerServiceImpl service;
            synchronized (TranslationManagerService.this.mLock) {
                service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
            }
            if (service != null) {
                service.unregisterTranslationCapabilityCallback(callback);
            }
        }

        public void onSessionCreated(TranslationContext translationContext, int sessionId, IResultReceiver receiver, int userId) throws RemoteException {
            synchronized (TranslationManagerService.this.mLock) {
                TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                if (service != null && (TranslationManagerService.this.isDefaultServiceLocked(userId) || TranslationManagerService.this.isCalledByServiceAppLocked(userId, "onSessionCreated"))) {
                    service.onSessionCreatedLocked(translationContext, sessionId, receiver);
                } else {
                    Slog.v(TranslationManagerService.TAG, "onSessionCreated(): no service for " + userId);
                    receiver.send(2, (Bundle) null);
                }
            }
        }

        public void updateUiTranslationState(int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, List<AutofillId> viewIds, IBinder token, int taskId, UiTranslationSpec uiTranslationSpec, int userId) {
            TranslationManagerService.this.enforceCallerHasPermission("android.permission.MANAGE_UI_TRANSLATION");
            synchronized (TranslationManagerService.this.mLock) {
                TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                if (service != null && (TranslationManagerService.this.isDefaultServiceLocked(userId) || TranslationManagerService.this.isCalledByServiceAppLocked(userId, "updateUiTranslationState"))) {
                    service.updateUiTranslationStateLocked(state, sourceSpec, targetSpec, viewIds, token, taskId, uiTranslationSpec);
                }
            }
        }

        public void registerUiTranslationStateCallback(IRemoteCallback callback, int userId) {
            synchronized (TranslationManagerService.this.mLock) {
                TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                if (service != null) {
                    service.registerUiTranslationStateCallbackLocked(callback, Binder.getCallingUid());
                }
            }
        }

        public void unregisterUiTranslationStateCallback(IRemoteCallback callback, int userId) {
            TranslationManagerServiceImpl service;
            synchronized (TranslationManagerService.this.mLock) {
                service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
            }
            if (service != null) {
                service.unregisterUiTranslationStateCallback(callback);
            }
        }

        public void onTranslationFinished(boolean activityDestroyed, IBinder token, ComponentName componentName, int userId) {
            synchronized (TranslationManagerService.this.mLock) {
                TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                service.onTranslationFinishedLocked(activityDestroyed, token, componentName);
            }
        }

        public void getServiceSettingsActivity(IResultReceiver result, int userId) {
            TranslationManagerServiceImpl service;
            synchronized (TranslationManagerService.this.mLock) {
                service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
            }
            if (service == null) {
                try {
                    result.send(2, (Bundle) null);
                    return;
                } catch (RemoteException e) {
                    Slog.w(TranslationManagerService.TAG, "Unable to send getServiceSettingsActivity(): " + e);
                    return;
                }
            }
            ComponentName componentName = service.getServiceSettingsActivityLocked();
            if (componentName == null) {
                try {
                    result.send(1, (Bundle) null);
                } catch (RemoteException e2) {
                    Slog.w(TranslationManagerService.TAG, "Unable to send getServiceSettingsActivity(): " + e2);
                }
            }
            Intent intent = new Intent();
            intent.setComponent(componentName);
            long identity = Binder.clearCallingIdentity();
            try {
                PendingIntent pendingIntent = PendingIntent.getActivityAsUser(TranslationManagerService.this.getContext(), 0, intent, 67108864, null, new UserHandle(userId));
                try {
                    result.send(1, SyncResultReceiver.bundleFor(pendingIntent));
                } catch (RemoteException e3) {
                    Slog.w(TranslationManagerService.TAG, "Unable to send getServiceSettingsActivity(): " + e3);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(TranslationManagerService.this.getContext(), TranslationManagerService.TAG, pw)) {
                synchronized (TranslationManagerService.this.mLock) {
                    TranslationManagerService.this.dumpLocked("", pw);
                    int userId = UserHandle.getCallingUserId();
                    TranslationManagerServiceImpl service = (TranslationManagerServiceImpl) TranslationManagerService.this.getServiceForUserLocked(userId);
                    if (service != null) {
                        service.dumpLocked("  ", fd, pw);
                    }
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.translation.TranslationManagerService$TranslationManagerServiceStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new TranslationManagerServiceShellCommand(TranslationManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("translation", new TranslationManagerServiceStub());
    }
}
