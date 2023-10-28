package com.android.server;

import android.content.Context;
import android.gsi.AvbPublicKey;
import android.gsi.GsiProgress;
import android.gsi.IGsiService;
import android.gsi.IGsiServiceCallback;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.image.IDynamicSystemService;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Slog;
import java.io.File;
/* loaded from: classes.dex */
public class DynamicSystemService extends IDynamicSystemService.Stub {
    private static final int GSID_ROUGH_TIMEOUT_MS = 8192;
    private static final long MINIMUM_SD_MB = 30720;
    private static final String PATH_DEFAULT = "/data/gsi/";
    private static final String TAG = "DynamicSystemService";
    private Context mContext;
    private String mDsuSlot;
    private volatile IGsiService mGsiService;
    private String mInstallPath;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DynamicSystemService(Context context) {
        this.mContext = context;
    }

    private IGsiService getGsiService() {
        if (this.mGsiService != null) {
            return this.mGsiService;
        }
        return IGsiService.Stub.asInterface(ServiceManager.waitForService("gsiservice"));
    }

    /* loaded from: classes.dex */
    class GsiServiceCallback extends IGsiServiceCallback.Stub {
        private int mResult = -1;

        GsiServiceCallback() {
        }

        @Override // android.gsi.IGsiServiceCallback
        public synchronized void onResult(int result) {
            this.mResult = result;
            notify();
        }

        public int getResult() {
            return this.mResult;
        }
    }

    public boolean startInstallation(String dsuSlot) throws RemoteException {
        IGsiService service = getGsiService();
        this.mGsiService = service;
        String path = SystemProperties.get("os.aot.path");
        if (path.isEmpty()) {
            int userId = UserHandle.myUserId();
            StorageManager sm = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            for (VolumeInfo volume : sm.getVolumes()) {
                if (volume.getType() == 0 && volume.isMountedWritable()) {
                    DiskInfo disk = volume.getDisk();
                    long mega = disk.size >> 20;
                    Slog.i(TAG, volume.getPath() + ": " + mega + " MB");
                    if (mega < MINIMUM_SD_MB) {
                        Slog.i(TAG, volume.getPath() + ": insufficient storage");
                    } else {
                        File sd_internal = volume.getInternalPathForUser(userId);
                        if (sd_internal != null) {
                            path = new File(sd_internal, dsuSlot).getPath();
                        }
                    }
                }
            }
            if (path.isEmpty()) {
                path = PATH_DEFAULT + dsuSlot;
            }
            Slog.i(TAG, "startInstallation -> " + path);
        }
        this.mInstallPath = path;
        this.mDsuSlot = dsuSlot;
        if (service.openInstall(path) != 0) {
            Slog.i(TAG, "Failed to open " + path);
            return false;
        }
        return true;
    }

    public int createPartition(String name, long size, boolean readOnly) throws RemoteException {
        IGsiService service = getGsiService();
        int status = service.createPartition(name, size, readOnly);
        if (status != 0) {
            Slog.i(TAG, "Failed to create partition: " + name);
        }
        return status;
    }

    public boolean closePartition() throws RemoteException {
        IGsiService service = getGsiService();
        if (service.closePartition() != 0) {
            Slog.i(TAG, "Partition installation completes with error");
            return false;
        }
        return true;
    }

    public boolean finishInstallation() throws RemoteException {
        IGsiService service = getGsiService();
        if (service.closeInstall() != 0) {
            Slog.i(TAG, "Failed to finish installation");
            return false;
        }
        return true;
    }

    public GsiProgress getInstallationProgress() throws RemoteException {
        return getGsiService().getInstallProgress();
    }

    public boolean abort() throws RemoteException {
        return getGsiService().cancelGsiInstall();
    }

    public boolean isInUse() {
        return SystemProperties.getBoolean("ro.gsid.image_running", false);
    }

    public boolean isInstalled() {
        boolean installed = SystemProperties.getBoolean("gsid.image_installed", false);
        Slog.i(TAG, "isInstalled(): " + installed);
        return installed;
    }

    public boolean isEnabled() throws RemoteException {
        return getGsiService().isGsiEnabled();
    }

    public boolean remove() throws RemoteException {
        try {
            GsiServiceCallback callback = new GsiServiceCallback();
            synchronized (callback) {
                getGsiService().removeGsiAsync(callback);
                callback.wait(8192L);
            }
            return callback.getResult() == 0;
        } catch (InterruptedException e) {
            Slog.e(TAG, "remove() was interrupted");
            return false;
        }
    }

    public boolean setEnable(boolean enable, boolean oneShot) throws RemoteException {
        IGsiService gsiService = getGsiService();
        if (enable) {
            try {
                if (this.mDsuSlot == null) {
                    this.mDsuSlot = gsiService.getActiveDsuSlot();
                }
                GsiServiceCallback callback = new GsiServiceCallback();
                synchronized (callback) {
                    gsiService.enableGsiAsync(oneShot, this.mDsuSlot, callback);
                    callback.wait(8192L);
                }
                return callback.getResult() == 0;
            } catch (InterruptedException e) {
                Slog.e(TAG, "setEnable() was interrupted");
                return false;
            }
        }
        return gsiService.disableGsi();
    }

    public boolean setAshmem(ParcelFileDescriptor ashmem, long size) {
        try {
            return getGsiService().setGsiAshmem(ashmem, size);
        } catch (RemoteException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public boolean submitFromAshmem(long size) {
        try {
            return getGsiService().commitGsiChunkFromAshmem(size);
        } catch (RemoteException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public boolean getAvbPublicKey(AvbPublicKey dst) {
        try {
            return getGsiService().getAvbPublicKey(dst) == 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public long suggestScratchSize() throws RemoteException {
        return getGsiService().suggestScratchSize();
    }
}
