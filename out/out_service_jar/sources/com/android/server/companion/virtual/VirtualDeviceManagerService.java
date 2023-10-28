package com.android.server.companion.virtual;

import android.app.ActivityOptions;
import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.virtual.IVirtualDevice;
import android.companion.virtual.IVirtualDeviceActivityListener;
import android.companion.virtual.IVirtualDeviceManager;
import android.companion.virtual.VirtualDeviceParams;
import android.content.Context;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplayConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda4;
import com.android.server.companion.virtual.CameraAccessController;
import com.android.server.companion.virtual.VirtualDeviceImpl;
import com.android.server.companion.virtual.VirtualDeviceManagerService;
import com.android.server.wm.ActivityInterceptorCallback;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class VirtualDeviceManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "VirtualDeviceManagerService";
    private final ActivityInterceptorCallback mActivityInterceptorCallback;
    private final ConcurrentHashMap<Integer, List<AssociationInfo>> mAllAssociations;
    private final SparseArray<CameraAccessController> mCameraAccessControllers;
    private final Handler mHandler;
    private final VirtualDeviceManagerImpl mImpl;
    private final VirtualDeviceManagerInternal mLocalService;
    private final SparseArray<CompanionDeviceManager.OnAssociationsChangedListener> mOnAssociationsChangedListeners;
    private final PendingTrampolineMap mPendingTrampolines;
    private final Object mVirtualDeviceManagerLock;
    private final SparseArray<VirtualDeviceImpl> mVirtualDevices;

    public VirtualDeviceManagerService(Context context) {
        super(context);
        this.mVirtualDeviceManagerLock = new Object();
        Handler handler = new Handler(Looper.getMainLooper());
        this.mHandler = handler;
        this.mPendingTrampolines = new PendingTrampolineMap(handler);
        this.mCameraAccessControllers = new SparseArray<>();
        this.mVirtualDevices = new SparseArray<>();
        this.mAllAssociations = new ConcurrentHashMap<>();
        this.mOnAssociationsChangedListeners = new SparseArray<>();
        this.mActivityInterceptorCallback = new ActivityInterceptorCallback() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService.1
            @Override // com.android.server.wm.ActivityInterceptorCallback
            public ActivityInterceptorCallback.ActivityInterceptResult intercept(ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                VirtualDeviceImpl.PendingTrampoline pt;
                if (info.callingPackage == null || (pt = VirtualDeviceManagerService.this.mPendingTrampolines.remove(info.callingPackage)) == null) {
                    return null;
                }
                pt.mResultReceiver.send(0, null);
                ActivityOptions options = info.checkedOptions;
                if (options == null) {
                    options = ActivityOptions.makeBasic();
                }
                return new ActivityInterceptorCallback.ActivityInterceptResult(info.intent, options.setLaunchDisplayId(pt.mDisplayId));
            }
        };
        this.mImpl = new VirtualDeviceManagerImpl();
        this.mLocalService = new LocalService();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("virtualdevice", this.mImpl);
        publishLocalService(VirtualDeviceManagerInternal.class, this.mLocalService);
        ActivityTaskManagerInternal activityTaskManagerInternal = (ActivityTaskManagerInternal) getLocalService(ActivityTaskManagerInternal.class);
        activityTaskManagerInternal.registerActivityStartInterceptor(3, this.mActivityInterceptorCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isValidVirtualDeviceLocked(IVirtualDevice virtualDevice) {
        try {
            return this.mVirtualDevices.contains(virtualDevice.getAssociationId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        super.onUserStarting(user);
        Context userContext = getContext().createContextAsUser(user.getUserHandle(), 0);
        synchronized (this.mVirtualDeviceManagerLock) {
            CompanionDeviceManager cdm = (CompanionDeviceManager) userContext.getSystemService(CompanionDeviceManager.class);
            final int userId = user.getUserIdentifier();
            this.mAllAssociations.put(Integer.valueOf(userId), cdm.getAllAssociations());
            CompanionDeviceManager.OnAssociationsChangedListener listener = new CompanionDeviceManager.OnAssociationsChangedListener() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService$$ExternalSyntheticLambda0
                public final void onAssociationsChanged(List list) {
                    VirtualDeviceManagerService.this.m2739x5e2090b7(userId, list);
                }
            };
            this.mOnAssociationsChangedListeners.put(userId, listener);
            cdm.addOnAssociationsChangedListener(new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), listener);
            CameraAccessController cameraAccessController = new CameraAccessController(userContext, this.mLocalService, new CameraAccessController.CameraAccessBlockedCallback() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService$$ExternalSyntheticLambda1
                @Override // com.android.server.companion.virtual.CameraAccessController.CameraAccessBlockedCallback
                public final void onCameraAccessBlocked(int i) {
                    VirtualDeviceManagerService.this.onCameraAccessBlocked(i);
                }
            });
            this.mCameraAccessControllers.put(user.getUserIdentifier(), cameraAccessController);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserStarting$0$com-android-server-companion-virtual-VirtualDeviceManagerService  reason: not valid java name */
    public /* synthetic */ void m2739x5e2090b7(int userId, List associations) {
        this.mAllAssociations.put(Integer.valueOf(userId), associations);
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        super.onUserStopping(user);
        synchronized (this.mVirtualDeviceManagerLock) {
            int userId = user.getUserIdentifier();
            this.mAllAssociations.remove(Integer.valueOf(userId));
            CompanionDeviceManager cdm = (CompanionDeviceManager) getContext().createContextAsUser(user.getUserHandle(), 0).getSystemService(CompanionDeviceManager.class);
            CompanionDeviceManager.OnAssociationsChangedListener listener = this.mOnAssociationsChangedListeners.get(userId);
            if (listener != null) {
                cdm.removeOnAssociationsChangedListener(listener);
                this.mOnAssociationsChangedListeners.remove(userId);
            }
            CameraAccessController cameraAccessController = this.mCameraAccessControllers.get(user.getUserIdentifier());
            if (cameraAccessController != null) {
                cameraAccessController.close();
                this.mCameraAccessControllers.remove(user.getUserIdentifier());
            } else {
                Slog.w(TAG, "Cannot unregister cameraAccessController for user " + user);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCameraAccessBlocked(int appUid) {
        synchronized (this.mVirtualDeviceManagerLock) {
            int size = this.mVirtualDevices.size();
            for (int i = 0; i < size; i++) {
                CharSequence deviceName = this.mVirtualDevices.valueAt(i).getDisplayName();
                this.mVirtualDevices.valueAt(i).showToastWhereUidIsRunning(appUid, getContext().getString(17041691, deviceName), 1);
            }
        }
    }

    /* loaded from: classes.dex */
    class VirtualDeviceManagerImpl extends IVirtualDeviceManager.Stub implements VirtualDeviceImpl.PendingTrampolineCallback {
        VirtualDeviceManagerImpl() {
        }

        public IVirtualDevice createVirtualDevice(IBinder token, String packageName, int associationId, VirtualDeviceParams params, IVirtualDeviceActivityListener activityListener) {
            VirtualDeviceImpl virtualDevice;
            VirtualDeviceManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.CREATE_VIRTUAL_DEVICE", "createVirtualDevice");
            int callingUid = getCallingUid();
            if (!PermissionUtils.validateCallingPackageName(VirtualDeviceManagerService.this.getContext(), packageName)) {
                throw new SecurityException("Package name " + packageName + " does not belong to calling uid " + callingUid);
            }
            AssociationInfo associationInfo = getAssociationInfo(packageName, associationId);
            if (associationInfo == null) {
                throw new IllegalArgumentException("No association with ID " + associationId);
            }
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                if (VirtualDeviceManagerService.this.mVirtualDevices.contains(associationId)) {
                    throw new IllegalStateException("Virtual device for association ID " + associationId + " already exists");
                }
                final int userId = UserHandle.getUserId(callingUid);
                final CameraAccessController cameraAccessController = (CameraAccessController) VirtualDeviceManagerService.this.mCameraAccessControllers.get(userId);
                virtualDevice = new VirtualDeviceImpl(VirtualDeviceManagerService.this.getContext(), associationInfo, token, callingUid, new VirtualDeviceImpl.OnDeviceCloseListener() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService.VirtualDeviceManagerImpl.1
                    @Override // com.android.server.companion.virtual.VirtualDeviceImpl.OnDeviceCloseListener
                    public void onClose(int associationId2) {
                        synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                            VirtualDeviceManagerService.this.mVirtualDevices.remove(associationId2);
                            CameraAccessController cameraAccessController2 = cameraAccessController;
                            if (cameraAccessController2 != null) {
                                cameraAccessController2.stopObservingIfNeeded();
                            } else {
                                Slog.w(VirtualDeviceManagerService.TAG, "cameraAccessController not found for user " + userId);
                            }
                        }
                    }
                }, this, activityListener, new Consumer() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService$VirtualDeviceManagerImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        CameraAccessController.this.blockCameraAccessIfNeeded((ArraySet) obj);
                    }
                }, params);
                if (cameraAccessController != null) {
                    cameraAccessController.startObservingIfNeeded();
                } else {
                    Slog.w(VirtualDeviceManagerService.TAG, "cameraAccessController not found for user " + userId);
                }
                VirtualDeviceManagerService.this.mVirtualDevices.put(associationInfo.getId(), virtualDevice);
            }
            return virtualDevice;
        }

        public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IVirtualDevice virtualDevice, String packageName) throws RemoteException {
            VirtualDeviceImpl virtualDeviceImpl;
            int callingUid = getCallingUid();
            if (!PermissionUtils.validateCallingPackageName(VirtualDeviceManagerService.this.getContext(), packageName)) {
                throw new SecurityException("Package name " + packageName + " does not belong to calling uid " + callingUid);
            }
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                virtualDeviceImpl = (VirtualDeviceImpl) VirtualDeviceManagerService.this.mVirtualDevices.get(virtualDevice.getAssociationId());
                if (virtualDeviceImpl == null) {
                    throw new SecurityException("Invalid VirtualDevice");
                }
            }
            if (virtualDeviceImpl.getOwnerUid() != callingUid) {
                throw new SecurityException("uid " + callingUid + " is not the owner of the supplied VirtualDevice");
            }
            long tokenTwo = Binder.clearCallingIdentity();
            try {
                GenericWindowPolicyController gwpc = virtualDeviceImpl.createWindowPolicyController();
                Binder.restoreCallingIdentity(tokenTwo);
                DisplayManagerInternal displayManager = (DisplayManagerInternal) VirtualDeviceManagerService.this.getLocalService(DisplayManagerInternal.class);
                int displayId = displayManager.createVirtualDisplay(virtualDisplayConfig, callback, virtualDevice, gwpc, packageName);
                tokenTwo = Binder.clearCallingIdentity();
                try {
                    virtualDeviceImpl.onVirtualDisplayCreatedLocked(gwpc, displayId);
                    return displayId;
                } finally {
                }
            } finally {
            }
        }

        private AssociationInfo getAssociationInfo(String packageName, int associationId) {
            int callingUserId = getCallingUserHandle().getIdentifier();
            List<AssociationInfo> associations = (List) VirtualDeviceManagerService.this.mAllAssociations.get(Integer.valueOf(callingUserId));
            if (associations != null) {
                int associationSize = associations.size();
                for (int i = 0; i < associationSize; i++) {
                    AssociationInfo associationInfo = associations.get(i);
                    if (associationInfo.belongsToPackage(callingUserId, packageName) && associationId == associationInfo.getId()) {
                        return associationInfo;
                    }
                }
                return null;
            }
            Slog.w(VirtualDeviceManagerService.TAG, "No associations for user " + callingUserId);
            return null;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Throwable e) {
                Slog.e(VirtualDeviceManagerService.TAG, "Error during IPC", e);
                throw ExceptionUtils.propagate(e, RemoteException.class);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
            if (!DumpUtils.checkDumpAndUsageStatsPermission(VirtualDeviceManagerService.this.getContext(), VirtualDeviceManagerService.TAG, fout)) {
                return;
            }
            fout.println("Created virtual devices: ");
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                for (int i = 0; i < VirtualDeviceManagerService.this.mVirtualDevices.size(); i++) {
                    ((VirtualDeviceImpl) VirtualDeviceManagerService.this.mVirtualDevices.valueAt(i)).dump(fd, fout, args);
                }
            }
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceImpl.PendingTrampolineCallback
        public void startWaitingForPendingTrampoline(VirtualDeviceImpl.PendingTrampoline pendingTrampoline) {
            VirtualDeviceImpl.PendingTrampoline existing = VirtualDeviceManagerService.this.mPendingTrampolines.put(pendingTrampoline.mPendingIntent.getCreatorPackage(), pendingTrampoline);
            if (existing != null) {
                existing.mResultReceiver.send(2, null);
            }
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceImpl.PendingTrampolineCallback
        public void stopWaitingForPendingTrampoline(VirtualDeviceImpl.PendingTrampoline pendingTrampoline) {
            VirtualDeviceManagerService.this.mPendingTrampolines.remove(pendingTrampoline.mPendingIntent.getCreatorPackage());
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends VirtualDeviceManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public boolean isValidVirtualDevice(IVirtualDevice virtualDevice) {
            boolean isValidVirtualDeviceLocked;
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                isValidVirtualDeviceLocked = VirtualDeviceManagerService.this.isValidVirtualDeviceLocked(virtualDevice);
            }
            return isValidVirtualDeviceLocked;
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public void onVirtualDisplayRemoved(IVirtualDevice virtualDevice, int displayId) {
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                ((VirtualDeviceImpl) virtualDevice).onVirtualDisplayRemovedLocked(displayId);
            }
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public int getBaseVirtualDisplayFlags(IVirtualDevice virtualDevice) {
            return ((VirtualDeviceImpl) virtualDevice).getBaseVirtualDisplayFlags();
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public boolean isAppOwnerOfAnyVirtualDevice(int uid) {
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                int size = VirtualDeviceManagerService.this.mVirtualDevices.size();
                for (int i = 0; i < size; i++) {
                    if (((VirtualDeviceImpl) VirtualDeviceManagerService.this.mVirtualDevices.valueAt(i)).getOwnerUid() == uid) {
                        return true;
                    }
                }
                return false;
            }
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public boolean isAppRunningOnAnyVirtualDevice(int uid) {
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                int size = VirtualDeviceManagerService.this.mVirtualDevices.size();
                for (int i = 0; i < size; i++) {
                    if (((VirtualDeviceImpl) VirtualDeviceManagerService.this.mVirtualDevices.valueAt(i)).isAppRunningOnVirtualDevice(uid)) {
                        return true;
                    }
                }
                return false;
            }
        }

        @Override // com.android.server.companion.virtual.VirtualDeviceManagerInternal
        public boolean isDisplayOwnedByAnyVirtualDevice(int displayId) {
            synchronized (VirtualDeviceManagerService.this.mVirtualDeviceManagerLock) {
                int size = VirtualDeviceManagerService.this.mVirtualDevices.size();
                for (int i = 0; i < size; i++) {
                    if (((VirtualDeviceImpl) VirtualDeviceManagerService.this.mVirtualDevices.valueAt(i)).isDisplayOwnedByVirtualDevice(displayId)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PendingTrampolineMap {
        private static final int TRAMPOLINE_WAIT_MS = 5000;
        private final Handler mHandler;
        private final ConcurrentHashMap<String, VirtualDeviceImpl.PendingTrampoline> mMap = new ConcurrentHashMap<>();

        PendingTrampolineMap(Handler handler) {
            this.mHandler = handler;
        }

        VirtualDeviceImpl.PendingTrampoline put(String packageName, final VirtualDeviceImpl.PendingTrampoline pendingTrampoline) {
            VirtualDeviceImpl.PendingTrampoline existing = this.mMap.put(packageName, pendingTrampoline);
            this.mHandler.removeCallbacksAndMessages(existing);
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.companion.virtual.VirtualDeviceManagerService$PendingTrampolineMap$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    VirtualDeviceManagerService.PendingTrampolineMap.this.m2740x47faf820(pendingTrampoline);
                }
            }, pendingTrampoline, 5000L);
            return existing;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$put$0$com-android-server-companion-virtual-VirtualDeviceManagerService$PendingTrampolineMap  reason: not valid java name */
        public /* synthetic */ void m2740x47faf820(VirtualDeviceImpl.PendingTrampoline pendingTrampoline) {
            String creatorPackage = pendingTrampoline.mPendingIntent.getCreatorPackage();
            if (creatorPackage != null) {
                remove(creatorPackage);
            }
        }

        VirtualDeviceImpl.PendingTrampoline remove(String packageName) {
            VirtualDeviceImpl.PendingTrampoline pendingTrampoline = this.mMap.remove(packageName);
            this.mHandler.removeCallbacksAndMessages(pendingTrampoline);
            return pendingTrampoline;
        }
    }
}
