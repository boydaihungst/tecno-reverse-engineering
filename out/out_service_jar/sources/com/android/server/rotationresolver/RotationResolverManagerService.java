package com.android.server.rotationresolver;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.rotationresolver.RotationResolverInternal;
import android.service.rotationresolver.RotationResolutionRequest;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class RotationResolverManagerService extends AbstractMasterSystemService<RotationResolverManagerService, RotationResolverManagerPerUserService> {
    private static final boolean DEFAULT_SERVICE_ENABLED = true;
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    static final int ORIENTATION_UNKNOWN = 0;
    static final int RESOLUTION_DISABLED = 6;
    static final int RESOLUTION_FAILURE = 8;
    static final int RESOLUTION_UNAVAILABLE = 7;
    private static final String TAG = RotationResolverManagerService.class.getSimpleName();
    private final Context mContext;
    boolean mIsServiceEnabled;
    private final SensorPrivacyManager mPrivacyManager;

    public RotationResolverManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039940), null, 68);
        this.mContext = context;
        this.mPrivacyManager = SensorPrivacyManager.getInstance(context);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService, com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            DeviceConfig.addOnPropertiesChangedListener("rotation_resolver", getContext().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.rotationresolver.RotationResolverManagerService$$ExternalSyntheticLambda0
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    RotationResolverManagerService.this.m6398x14cb8900(properties);
                }
            });
            this.mIsServiceEnabled = DeviceConfig.getBoolean("rotation_resolver", KEY_SERVICE_ENABLED, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-rotationresolver-RotationResolverManagerService  reason: not valid java name */
    public /* synthetic */ void m6398x14cb8900(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    private void onDeviceConfigChange(Set<String> keys) {
        if (keys.contains(KEY_SERVICE_ENABLED)) {
            this.mIsServiceEnabled = DeviceConfig.getBoolean("rotation_resolver", KEY_SERVICE_ENABLED, true);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("resolver", new BinderService());
        publishLocalService(RotationResolverInternal.class, new LocalService());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public RotationResolverManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new RotationResolverManagerPerUserService(this, this.mLock, resolvedUserId);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void onServiceRemoved(RotationResolverManagerPerUserService service, int userId) {
        synchronized (this.mLock) {
            service.destroyLocked();
        }
    }

    public static boolean isServiceConfigured(Context context) {
        return !TextUtils.isEmpty(getServiceConfigPackage(context));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getComponentNameShellCommand(int userId) {
        synchronized (this.mLock) {
            RotationResolverManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                return service.getComponentName();
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resolveRotationShellCommand(int userId, RotationResolverInternal.RotationResolverCallbackInternal callbackInternal, RotationResolutionRequest request) {
        synchronized (this.mLock) {
            RotationResolverManagerPerUserService service = getServiceForUserLocked(userId);
            if (service != null) {
                service.resolveRotationLocked(callbackInternal, request, new CancellationSignal());
            } else {
                Slog.i(TAG, "service not available for user_id: " + userId);
            }
        }
    }

    static String getServiceConfigPackage(Context context) {
        return context.getPackageManager().getRotationResolverPackageName();
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends RotationResolverInternal {
        private LocalService() {
        }

        public boolean isRotationResolverSupported() {
            boolean z;
            synchronized (RotationResolverManagerService.this.mLock) {
                z = RotationResolverManagerService.this.mIsServiceEnabled;
            }
            return z;
        }

        public void resolveRotation(RotationResolverInternal.RotationResolverCallbackInternal callbackInternal, String packageName, int proposedRotation, int currentRotation, long timeout, CancellationSignal cancellationSignalInternal) {
            RotationResolutionRequest request;
            Objects.requireNonNull(callbackInternal);
            Objects.requireNonNull(cancellationSignalInternal);
            synchronized (RotationResolverManagerService.this.mLock) {
                try {
                    try {
                        boolean isCameraAvailable = !RotationResolverManagerService.this.mPrivacyManager.isSensorPrivacyEnabled(2);
                        try {
                            if (RotationResolverManagerService.this.mIsServiceEnabled && isCameraAvailable) {
                                try {
                                    RotationResolverManagerPerUserService service = (RotationResolverManagerPerUserService) RotationResolverManagerService.this.getServiceForUserLocked(UserHandle.getCallingUserId());
                                    if (packageName == null) {
                                        request = new RotationResolutionRequest("", currentRotation, proposedRotation, true, timeout);
                                    } else {
                                        request = new RotationResolutionRequest(packageName, currentRotation, proposedRotation, true, timeout);
                                    }
                                    service.resolveRotationLocked(callbackInternal, request, cancellationSignalInternal);
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                if (isCameraAvailable) {
                                    Slog.w(RotationResolverManagerService.TAG, "Rotation Resolver service is disabled.");
                                } else {
                                    Slog.w(RotationResolverManagerService.TAG, "Camera is locked by a toggle.");
                                }
                                callbackInternal.onFailure(0);
                                RotationResolverManagerService.logRotationStats(proposedRotation, currentRotation, 6);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(RotationResolverManagerService.this.mContext, RotationResolverManagerService.TAG, pw)) {
                synchronized (RotationResolverManagerService.this.mLock) {
                    RotationResolverManagerService.this.dumpLocked("", pw);
                }
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            RotationResolverManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_ROTATION_RESOLVER", RotationResolverManagerService.TAG);
            new RotationResolverShellCommand(RotationResolverManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logRotationStatsWithTimeToCalculate(int proposedRotation, int currentRotation, int result, long timeToCalculate) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.AUTO_ROTATE_REPORTED, surfaceRotationToProto(currentRotation), surfaceRotationToProto(proposedRotation), result, timeToCalculate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logRotationStats(int proposedRotation, int currentRotation, int result) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.AUTO_ROTATE_REPORTED, surfaceRotationToProto(currentRotation), surfaceRotationToProto(proposedRotation), result);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int errorCodeToProto(int error) {
        switch (error) {
            case 0:
            case 1:
            case 2:
                return 0;
            case 3:
            default:
                return 8;
            case 4:
                return 7;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int surfaceRotationToProto(int rotationPoseResult) {
        switch (rotationPoseResult) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 5;
            default:
                return 8;
        }
    }
}
