package com.android.server.appprediction;

import android.app.ActivityManagerInternal;
import android.app.prediction.AppPredictionContext;
import android.app.prediction.AppPredictionSessionId;
import android.app.prediction.AppTargetEvent;
import android.app.prediction.IPredictionCallback;
import android.app.prediction.IPredictionManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
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
/* loaded from: classes.dex */
public class AppPredictionManagerService extends AbstractMasterSystemService<AppPredictionManagerService, AppPredictionPerUserService> {
    private static final int MAX_TEMP_SERVICE_DURATION_MS = 120000;
    private static final String TAG = AppPredictionManagerService.class.getSimpleName();
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;

    public AppPredictionManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039918), null, 17);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public AppPredictionPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new AppPredictionPerUserService(this, this.mLock, resolvedUserId);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("app_prediction", new PredictionManagerServiceStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_APP_PREDICTIONS", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        AppPredictionPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageUpdatedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        AppPredictionPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageRestartedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_DURATION_MS;
    }

    /* loaded from: classes.dex */
    private class PredictionManagerServiceStub extends IPredictionManager.Stub {
        private PredictionManagerServiceStub() {
        }

        public void createPredictionSession(final AppPredictionContext context, final AppPredictionSessionId sessionId, final IBinder token) {
            runForUserLocked("createPredictionSession", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).onCreatePredictionSessionLocked(context, sessionId, token);
                }
            });
        }

        public void notifyAppTargetEvent(final AppPredictionSessionId sessionId, final AppTargetEvent event) {
            runForUserLocked("notifyAppTargetEvent", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).notifyAppTargetEventLocked(sessionId, event);
                }
            });
        }

        public void notifyLaunchLocationShown(final AppPredictionSessionId sessionId, final String launchLocation, final ParceledListSlice targetIds) {
            runForUserLocked("notifyLaunchLocationShown", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).notifyLaunchLocationShownLocked(sessionId, launchLocation, targetIds);
                }
            });
        }

        public void sortAppTargets(final AppPredictionSessionId sessionId, final ParceledListSlice targets, final IPredictionCallback callback) {
            runForUserLocked("sortAppTargets", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).sortAppTargetsLocked(sessionId, targets, callback);
                }
            });
        }

        public void registerPredictionUpdates(final AppPredictionSessionId sessionId, final IPredictionCallback callback) {
            runForUserLocked("registerPredictionUpdates", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).registerPredictionUpdatesLocked(sessionId, callback);
                }
            });
        }

        public void unregisterPredictionUpdates(final AppPredictionSessionId sessionId, final IPredictionCallback callback) {
            runForUserLocked("unregisterPredictionUpdates", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).unregisterPredictionUpdatesLocked(sessionId, callback);
                }
            });
        }

        public void requestPredictionUpdate(final AppPredictionSessionId sessionId) {
            runForUserLocked("requestPredictionUpdate", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).requestPredictionUpdateLocked(sessionId);
                }
            });
        }

        public void onDestroyPredictionSession(final AppPredictionSessionId sessionId) {
            runForUserLocked("onDestroyPredictionSession", sessionId, new Consumer() { // from class: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub$$ExternalSyntheticLambda6
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppPredictionPerUserService) obj).onDestroyPredictionSessionLocked(sessionId);
                }
            });
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.appprediction.AppPredictionManagerService$PredictionManagerServiceStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new AppPredictionManagerServiceShellCommand(AppPredictionManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private void runForUserLocked(String func, AppPredictionSessionId sessionId, Consumer<AppPredictionPerUserService> c) {
            ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            int userId = am.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), sessionId.getUserId(), false, 0, (String) null, (String) null);
            Context ctx = AppPredictionManagerService.this.getContext();
            if (ctx.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") != 0 && !AppPredictionManagerService.this.mServiceNameResolver.isTemporary(userId) && !AppPredictionManagerService.this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid()) && Binder.getCallingUid() != 1000) {
                String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " expected caller to hold PACKAGE_USAGE_STATS permission";
                Slog.w(AppPredictionManagerService.TAG, msg);
                throw new SecurityException(msg);
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (AppPredictionManagerService.this.mLock) {
                    AppPredictionPerUserService service = (AppPredictionPerUserService) AppPredictionManagerService.this.getServiceForUserLocked(userId);
                    c.accept(service);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }
}
