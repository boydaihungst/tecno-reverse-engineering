package com.android.server.companion.virtual;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraInjectionSession;
import android.hardware.camera2.CameraManager;
import android.util.ArrayMap;
import android.util.Slog;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class CameraAccessController extends CameraManager.AvailabilityCallback implements AutoCloseable {
    private static final String TAG = "CameraAccessController";
    private final CameraAccessBlockedCallback mBlockedCallback;
    private final CameraManager mCameraManager;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final VirtualDeviceManagerInternal mVirtualDeviceManagerInternal;
    private final Object mLock = new Object();
    private int mObserverCount = 0;
    private ArrayMap<String, InjectionSessionData> mPackageToSessionData = new ArrayMap<>();
    private ArrayMap<String, OpenCameraInfo> mAppsToBlockOnVirtualDevice = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface CameraAccessBlockedCallback {
        void onCameraAccessBlocked(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class InjectionSessionData {
        public int appUid;
        public ArrayMap<String, CameraInjectionSession> cameraIdToSession = new ArrayMap<>();

        InjectionSessionData() {
        }
    }

    /* loaded from: classes.dex */
    static class OpenCameraInfo {
        public String packageName;
        public int packageUid;

        OpenCameraInfo() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CameraAccessController(Context context, VirtualDeviceManagerInternal virtualDeviceManagerInternal, CameraAccessBlockedCallback blockedCallback) {
        this.mContext = context;
        this.mVirtualDeviceManagerInternal = virtualDeviceManagerInternal;
        this.mBlockedCallback = blockedCallback;
        this.mCameraManager = (CameraManager) context.getSystemService(CameraManager.class);
        this.mPackageManager = context.getPackageManager();
    }

    public void startObservingIfNeeded() {
        synchronized (this.mLock) {
            if (this.mObserverCount == 0) {
                this.mCameraManager.registerAvailabilityCallback(this.mContext.getMainExecutor(), this);
            }
            this.mObserverCount++;
        }
    }

    public void stopObservingIfNeeded() {
        synchronized (this.mLock) {
            int i = this.mObserverCount - 1;
            this.mObserverCount = i;
            if (i <= 0) {
                close();
            }
        }
    }

    public void blockCameraAccessIfNeeded(Set<Integer> runningUids) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mAppsToBlockOnVirtualDevice.size(); i++) {
                String cameraId = this.mAppsToBlockOnVirtualDevice.keyAt(i);
                OpenCameraInfo openCameraInfo = this.mAppsToBlockOnVirtualDevice.get(cameraId);
                int packageUid = openCameraInfo.packageUid;
                if (runningUids.contains(Integer.valueOf(packageUid))) {
                    String packageName = openCameraInfo.packageName;
                    if (this.mPackageToSessionData.get(packageName) == null) {
                        InjectionSessionData data = new InjectionSessionData();
                        data.appUid = packageUid;
                        this.mPackageToSessionData.put(packageName, data);
                    }
                    startBlocking(packageName, cameraId);
                }
            }
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        synchronized (this.mLock) {
            int i = this.mObserverCount;
            if (i < 0) {
                Slog.wtf(TAG, "Unexpected negative mObserverCount: " + this.mObserverCount);
            } else if (i > 0) {
                Slog.w(TAG, "Unexpected close with observers remaining: " + this.mObserverCount);
            }
        }
        this.mCameraManager.unregisterAvailabilityCallback(this);
    }

    public void onCameraOpened(String cameraId, String packageName) {
        synchronized (this.mLock) {
            try {
                try {
                    ApplicationInfo ainfo = this.mPackageManager.getApplicationInfo(packageName, 0);
                    InjectionSessionData data = this.mPackageToSessionData.get(packageName);
                    if (!this.mVirtualDeviceManagerInternal.isAppRunningOnAnyVirtualDevice(ainfo.uid)) {
                        OpenCameraInfo openCameraInfo = new OpenCameraInfo();
                        openCameraInfo.packageName = packageName;
                        openCameraInfo.packageUid = ainfo.uid;
                        this.mAppsToBlockOnVirtualDevice.put(cameraId, openCameraInfo);
                        CameraInjectionSession existingSession = data != null ? data.cameraIdToSession.get(cameraId) : null;
                        if (existingSession != null) {
                            existingSession.close();
                            data.cameraIdToSession.remove(cameraId);
                            if (data.cameraIdToSession.isEmpty()) {
                                this.mPackageToSessionData.remove(packageName);
                            }
                        }
                        return;
                    }
                    if (data == null) {
                        data = new InjectionSessionData();
                        data.appUid = ainfo.uid;
                        this.mPackageToSessionData.put(packageName, data);
                    }
                    if (data.cameraIdToSession.containsKey(cameraId)) {
                        return;
                    }
                    startBlocking(packageName, cameraId);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "onCameraOpened - unknown package " + packageName, e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void onCameraClosed(String cameraId) {
        synchronized (this.mLock) {
            this.mAppsToBlockOnVirtualDevice.remove(cameraId);
            for (int i = this.mPackageToSessionData.size() - 1; i >= 0; i--) {
                InjectionSessionData data = this.mPackageToSessionData.valueAt(i);
                CameraInjectionSession session = data.cameraIdToSession.get(cameraId);
                if (session != null) {
                    session.close();
                    data.cameraIdToSession.remove(cameraId);
                    if (data.cameraIdToSession.isEmpty()) {
                        this.mPackageToSessionData.removeAt(i);
                    }
                }
            }
        }
    }

    private void startBlocking(final String packageName, final String cameraId) {
        try {
            Slog.d(TAG, "startBlocking() cameraId: " + cameraId + " packageName: " + packageName);
            this.mCameraManager.injectCamera(packageName, cameraId, "", this.mContext.getMainExecutor(), new CameraInjectionSession.InjectionStatusCallback() { // from class: com.android.server.companion.virtual.CameraAccessController.1
                public void onInjectionSucceeded(CameraInjectionSession session) {
                    CameraAccessController.this.onInjectionSucceeded(cameraId, packageName, session);
                }

                public void onInjectionError(int errorCode) {
                    CameraAccessController.this.onInjectionError(cameraId, packageName, errorCode);
                }
            });
        } catch (CameraAccessException e) {
            Slog.e(TAG, "Failed to injectCamera for cameraId:" + cameraId + " package:" + packageName, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInjectionSucceeded(String cameraId, String packageName, CameraInjectionSession session) {
        synchronized (this.mLock) {
            InjectionSessionData data = this.mPackageToSessionData.get(packageName);
            if (data == null) {
                Slog.e(TAG, "onInjectionSucceeded didn't find expected entry for package " + packageName);
                session.close();
                return;
            }
            CameraInjectionSession existingSession = data.cameraIdToSession.put(cameraId, session);
            if (existingSession != null) {
                Slog.e(TAG, "onInjectionSucceeded found unexpected existing session for camera " + cameraId);
                existingSession.close();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInjectionError(String cameraId, String packageName, int errorCode) {
        if (errorCode != 2) {
            Slog.e(TAG, "Unexpected injection error code:" + errorCode + " for camera:" + cameraId + " and package:" + packageName);
            return;
        }
        synchronized (this.mLock) {
            InjectionSessionData data = this.mPackageToSessionData.get(packageName);
            if (data != null) {
                this.mBlockedCallback.onCameraAccessBlocked(data.appUid);
            }
        }
    }
}
