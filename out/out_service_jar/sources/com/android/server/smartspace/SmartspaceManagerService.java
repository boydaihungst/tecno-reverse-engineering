package com.android.server.smartspace;

import android.app.ActivityManagerInternal;
import android.app.smartspace.ISmartspaceCallback;
import android.app.smartspace.ISmartspaceManager;
import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceSessionId;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class SmartspaceManagerService extends AbstractMasterSystemService<SmartspaceManagerService, SmartspacePerUserService> {
    private static final boolean DEBUG = false;
    private static final int MAX_TEMP_SERVICE_DURATION_MS = 120000;
    private static final String TAG = SmartspaceManagerService.class.getSimpleName();
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;

    public SmartspaceManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039943), null, 17);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public SmartspacePerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new SmartspacePerUserService(this, this.mLock, resolvedUserId);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("smartspace", new SmartspaceManagerStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_SMARTSPACE", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        SmartspacePerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageUpdatedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        SmartspacePerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageRestartedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_DURATION_MS;
    }

    /* loaded from: classes2.dex */
    private class SmartspaceManagerStub extends ISmartspaceManager.Stub {
        private SmartspaceManagerStub() {
        }

        public void createSmartspaceSession(final SmartspaceConfig smartspaceConfig, final SmartspaceSessionId sessionId, final IBinder token) {
            runForUserLocked("createSmartspaceSession", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).onCreateSmartspaceSessionLocked(smartspaceConfig, sessionId, token);
                }
            });
        }

        public void notifySmartspaceEvent(final SmartspaceSessionId sessionId, final SmartspaceTargetEvent event) {
            runForUserLocked("notifySmartspaceEvent", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).notifySmartspaceEventLocked(sessionId, event);
                }
            });
        }

        public void requestSmartspaceUpdate(final SmartspaceSessionId sessionId) {
            runForUserLocked("requestSmartspaceUpdate", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).requestSmartspaceUpdateLocked(sessionId);
                }
            });
        }

        public void registerSmartspaceUpdates(final SmartspaceSessionId sessionId, final ISmartspaceCallback callback) {
            runForUserLocked("registerSmartspaceUpdates", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).registerSmartspaceUpdatesLocked(sessionId, callback);
                }
            });
        }

        public void unregisterSmartspaceUpdates(final SmartspaceSessionId sessionId, final ISmartspaceCallback callback) {
            runForUserLocked("unregisterSmartspaceUpdates", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).unregisterSmartspaceUpdatesLocked(sessionId, callback);
                }
            });
        }

        public void destroySmartspaceSession(final SmartspaceSessionId sessionId) {
            runForUserLocked("destroySmartspaceSession", sessionId, new Consumer() { // from class: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SmartspacePerUserService) obj).onDestroyLocked(sessionId);
                }
            });
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.smartspace.SmartspaceManagerService$SmartspaceManagerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new SmartspaceManagerServiceShellCommand(SmartspaceManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private void runForUserLocked(String func, SmartspaceSessionId sessionId, Consumer<SmartspacePerUserService> c) {
            ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            int userId = am.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), sessionId.getUserHandle().getIdentifier(), false, 0, (String) null, (String) null);
            Context ctx = SmartspaceManagerService.this.getContext();
            if (ctx.checkCallingPermission("android.permission.MANAGE_SMARTSPACE") != 0 && !SmartspaceManagerService.this.mServiceNameResolver.isTemporary(userId) && !SmartspaceManagerService.this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid())) {
                String msg = "Permission Denial: Cannot call " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w(SmartspaceManagerService.TAG, msg);
                throw new SecurityException(msg);
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (SmartspaceManagerService.this.mLock) {
                    SmartspacePerUserService service = (SmartspacePerUserService) SmartspaceManagerService.this.getServiceForUserLocked(userId);
                    c.accept(service);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }
}
