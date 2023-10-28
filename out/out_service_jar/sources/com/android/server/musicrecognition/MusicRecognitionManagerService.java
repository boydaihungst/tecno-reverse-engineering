package com.android.server.musicrecognition;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.musicrecognition.IMusicRecognitionManager;
import android.media.musicrecognition.IMusicRecognitionManagerCallback;
import android.media.musicrecognition.RecognitionRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import java.io.FileDescriptor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes2.dex */
public class MusicRecognitionManagerService extends AbstractMasterSystemService<MusicRecognitionManagerService, MusicRecognitionManagerPerUserService> {
    private static final int MAX_TEMP_SERVICE_SUBSTITUTION_DURATION_MS = 60000;
    private static final String TAG = MusicRecognitionManagerService.class.getSimpleName();
    final ExecutorService mExecutorService;
    private MusicRecognitionManagerStub mMusicRecognitionManagerStub;

    public MusicRecognitionManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039930), null);
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public MusicRecognitionManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new MusicRecognitionManagerPerUserService(this, this.mLock, resolvedUserId);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.musicrecognition.MusicRecognitionManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [android.os.IBinder, com.android.server.musicrecognition.MusicRecognitionManagerService$MusicRecognitionManagerStub] */
    @Override // com.android.server.SystemService
    public void onStart() {
        ?? musicRecognitionManagerStub = new MusicRecognitionManagerStub();
        this.mMusicRecognitionManagerStub = musicRecognitionManagerStub;
        publishBinderService("music_recognition", musicRecognitionManagerStub);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceCaller(String func) {
        Context ctx = getContext();
        if (ctx.checkCallingPermission("android.permission.MANAGE_MUSIC_RECOGNITION") != 0) {
            String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " doesn't hold android.permission.MANAGE_MUSIC_RECOGNITION";
            throw new SecurityException(msg);
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_MUSIC_RECOGNITION", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return 60000;
    }

    /* loaded from: classes2.dex */
    final class MusicRecognitionManagerStub extends IMusicRecognitionManager.Stub {
        MusicRecognitionManagerStub() {
        }

        public void beginRecognition(RecognitionRequest recognitionRequest, IBinder callback) {
            MusicRecognitionManagerService.this.enforceCaller("beginRecognition");
            synchronized (MusicRecognitionManagerService.this.mLock) {
                int userId = UserHandle.getCallingUserId();
                MusicRecognitionManagerPerUserService service = (MusicRecognitionManagerPerUserService) MusicRecognitionManagerService.this.getServiceForUserLocked(userId);
                if (service != null && (isDefaultServiceLocked(userId) || isCalledByServiceAppLocked("beginRecognition"))) {
                    service.beginRecognitionLocked(recognitionRequest, callback);
                } else {
                    try {
                        IMusicRecognitionManagerCallback.Stub.asInterface(callback).onRecognitionFailed(3);
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.musicrecognition.MusicRecognitionManagerService$MusicRecognitionManagerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new MusicRecognitionManagerServiceShellCommand(MusicRecognitionManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private boolean isDefaultServiceLocked(int userId) {
            String defaultServiceName = MusicRecognitionManagerService.this.mServiceNameResolver.getDefaultServiceName(userId);
            if (defaultServiceName != null) {
                String currentServiceName = MusicRecognitionManagerService.this.mServiceNameResolver.getServiceName(userId);
                return defaultServiceName.equals(currentServiceName);
            }
            return false;
        }

        private boolean isCalledByServiceAppLocked(String methodName) {
            int userId = UserHandle.getCallingUserId();
            int callingUid = Binder.getCallingUid();
            String serviceName = MusicRecognitionManagerService.this.mServiceNameResolver.getServiceName(userId);
            if (serviceName == null) {
                Slog.e(MusicRecognitionManagerService.TAG, methodName + ": called by UID " + callingUid + ", but there's no service set for user " + userId);
                return false;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            if (serviceComponent == null) {
                Slog.w(MusicRecognitionManagerService.TAG, methodName + ": invalid service name: " + serviceName);
                return false;
            }
            String servicePackageName = serviceComponent.getPackageName();
            PackageManager pm = MusicRecognitionManagerService.this.getContext().getPackageManager();
            try {
                int serviceUid = pm.getPackageUidAsUser(servicePackageName, UserHandle.getCallingUserId());
                if (callingUid != serviceUid) {
                    Slog.e(MusicRecognitionManagerService.TAG, methodName + ": called by UID " + callingUid + ", but service UID is " + serviceUid);
                    return false;
                }
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(MusicRecognitionManagerService.TAG, methodName + ": could not verify UID for " + serviceName);
                return false;
            }
        }
    }
}
