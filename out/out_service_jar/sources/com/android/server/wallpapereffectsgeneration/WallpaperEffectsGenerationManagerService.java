package com.android.server.wallpapereffectsgeneration;

import android.app.ActivityManagerInternal;
import android.app.wallpapereffectsgeneration.CinematicEffectRequest;
import android.app.wallpapereffectsgeneration.CinematicEffectResponse;
import android.app.wallpapereffectsgeneration.ICinematicEffectListener;
import android.app.wallpapereffectsgeneration.IWallpaperEffectsGenerationManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
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
public class WallpaperEffectsGenerationManagerService extends AbstractMasterSystemService<WallpaperEffectsGenerationManagerService, WallpaperEffectsGenerationPerUserService> {
    private static final boolean DEBUG = false;
    private static final int MAX_TEMP_SERVICE_DURATION_MS = 120000;
    private static final String TAG = WallpaperEffectsGenerationManagerService.class.getSimpleName();
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;

    public WallpaperEffectsGenerationManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039949), null, 17);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public WallpaperEffectsGenerationPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new WallpaperEffectsGenerationPerUserService(this, this.mLock, resolvedUserId);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("wallpaper_effects_generation", new WallpaperEffectsGenerationManagerStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_WALLPAPER_EFFECTS_GENERATION", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        WallpaperEffectsGenerationPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageUpdatedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        WallpaperEffectsGenerationPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageRestartedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_DURATION_MS;
    }

    /* loaded from: classes2.dex */
    private class WallpaperEffectsGenerationManagerStub extends IWallpaperEffectsGenerationManager.Stub {
        private WallpaperEffectsGenerationManagerStub() {
        }

        public void generateCinematicEffect(final CinematicEffectRequest request, final ICinematicEffectListener listener) {
            if (!runForUser("generateCinematicEffect", true, new Consumer() { // from class: com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationManagerService$WallpaperEffectsGenerationManagerStub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((WallpaperEffectsGenerationPerUserService) obj).onGenerateCinematicEffectLocked(request, listener);
                }
            })) {
                try {
                    listener.onCinematicEffectGenerated(new CinematicEffectResponse.Builder(0, request.getTaskId()).build());
                } catch (RemoteException e) {
                }
            }
        }

        public void returnCinematicEffectResponse(final CinematicEffectResponse response) {
            runForUser("returnCinematicResponse", false, new Consumer() { // from class: com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationManagerService$WallpaperEffectsGenerationManagerStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((WallpaperEffectsGenerationPerUserService) obj).onReturnCinematicEffectResponseLocked(response);
                }
            });
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationManagerService$WallpaperEffectsGenerationManagerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new WallpaperEffectsGenerationManagerServiceShellCommand(WallpaperEffectsGenerationManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private boolean runForUser(String func, boolean checkManageWallpaperEffectsPermission, Consumer<WallpaperEffectsGenerationPerUserService> c) {
            ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            int userId = am.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), Binder.getCallingUserHandle().getIdentifier(), false, 0, (String) null, (String) null);
            if (checkManageWallpaperEffectsPermission) {
                Context ctx = WallpaperEffectsGenerationManagerService.this.getContext();
                if (ctx.checkCallingPermission("android.permission.MANAGE_WALLPAPER_EFFECTS_GENERATION") != 0 && !WallpaperEffectsGenerationManagerService.this.mServiceNameResolver.isTemporary(userId) && !WallpaperEffectsGenerationManagerService.this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid())) {
                    String msg = "Permission Denial: Cannot call " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                    Slog.w(WallpaperEffectsGenerationManagerService.TAG, msg);
                    throw new SecurityException(msg);
                }
            }
            int origCallingUid = Binder.getCallingUid();
            long origId = Binder.clearCallingIdentity();
            boolean accepted = false;
            try {
                synchronized (WallpaperEffectsGenerationManagerService.this.mLock) {
                    WallpaperEffectsGenerationPerUserService service = (WallpaperEffectsGenerationPerUserService) WallpaperEffectsGenerationManagerService.this.getServiceForUserLocked(userId);
                    if (service != null) {
                        if (!checkManageWallpaperEffectsPermission && !service.isCallingUidAllowed(origCallingUid)) {
                            String msg2 = "Permission Denial: cannot call " + func + ", uid[" + origCallingUid + "] doesn't match service implementation";
                            Slog.w(WallpaperEffectsGenerationManagerService.TAG, msg2);
                            throw new SecurityException(msg2);
                        }
                        accepted = true;
                        c.accept(service);
                    }
                }
                return accepted;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }
}
