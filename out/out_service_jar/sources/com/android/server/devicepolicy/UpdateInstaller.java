package com.android.server.devicepolicy;

import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.StartInstallingUpdateCallback;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class UpdateInstaller {
    static final String TAG = "UpdateInstaller";
    private StartInstallingUpdateCallback mCallback;
    private DevicePolicyConstants mConstants;
    protected Context mContext;
    protected File mCopiedUpdateFile;
    private DevicePolicyManagerService.Injector mInjector;
    private ParcelFileDescriptor mUpdateFileDescriptor;

    public abstract void installUpdateInThread();

    /* JADX INFO: Access modifiers changed from: protected */
    public UpdateInstaller(Context context, ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback callback, DevicePolicyManagerService.Injector injector, DevicePolicyConstants constants) {
        this.mContext = context;
        this.mCallback = callback;
        this.mUpdateFileDescriptor = updateFileDescriptor;
        this.mInjector = injector;
        this.mConstants = constants;
    }

    public void startInstallUpdate() {
        this.mCopiedUpdateFile = null;
        if (!isBatteryLevelSufficient()) {
            notifyCallbackOnError(5, "The battery level must be above " + this.mConstants.BATTERY_THRESHOLD_NOT_CHARGING + " while not charging or above " + this.mConstants.BATTERY_THRESHOLD_CHARGING + " while charging");
            return;
        }
        Thread thread = new Thread(new Runnable() { // from class: com.android.server.devicepolicy.UpdateInstaller$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UpdateInstaller.this.m3163x2016bea1();
            }
        });
        thread.setPriority(10);
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startInstallUpdate$0$com-android-server-devicepolicy-UpdateInstaller  reason: not valid java name */
    public /* synthetic */ void m3163x2016bea1() {
        File copyUpdateFileToDataOtaPackageDir = copyUpdateFileToDataOtaPackageDir();
        this.mCopiedUpdateFile = copyUpdateFileToDataOtaPackageDir;
        if (copyUpdateFileToDataOtaPackageDir == null) {
            notifyCallbackOnError(1, "Error while copying file.");
        } else {
            installUpdateInThread();
        }
    }

    private boolean isBatteryLevelSufficient() {
        Intent batteryStatus = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        float batteryPercentage = calculateBatteryPercentage(batteryStatus);
        boolean isBatteryPluggedIn = batteryStatus.getIntExtra("plugged", -1) > 0;
        return isBatteryPluggedIn ? batteryPercentage >= ((float) this.mConstants.BATTERY_THRESHOLD_CHARGING) : batteryPercentage >= ((float) this.mConstants.BATTERY_THRESHOLD_NOT_CHARGING);
    }

    private float calculateBatteryPercentage(Intent batteryStatus) {
        int level = batteryStatus.getIntExtra("level", -1);
        int scale = batteryStatus.getIntExtra("scale", -1);
        return (level * 100) / scale;
    }

    private File copyUpdateFileToDataOtaPackageDir() {
        try {
            File destination = createNewFileWithPermissions();
            copyToFile(destination);
            return destination;
        } catch (IOException e) {
            Log.w(TAG, "Failed to copy update file to OTA directory", e);
            notifyCallbackOnError(1, Log.getStackTraceString(e));
            return null;
        }
    }

    private File createNewFileWithPermissions() throws IOException {
        File destination = File.createTempFile("update", ".zip", new File(Environment.getDataDirectory() + "/ota_package"));
        FileUtils.setPermissions(destination, 484, -1, -1);
        return destination;
    }

    private void copyToFile(File destination) throws IOException {
        OutputStream out = new FileOutputStream(destination);
        try {
            InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(this.mUpdateFileDescriptor);
            FileUtils.copy(in, out);
            in.close();
            out.close();
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    void cleanupUpdateFile() {
        File file = this.mCopiedUpdateFile;
        if (file != null && file.exists()) {
            this.mCopiedUpdateFile.delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyCallbackOnError(int errorCode, String errorMessage) {
        cleanupUpdateFile();
        DevicePolicyEventLogger.createEvent(74).setInt(errorCode).write();
        try {
            this.mCallback.onStartInstallingUpdateError(errorCode, errorMessage);
        } catch (RemoteException e) {
            Log.d(TAG, "Error while calling callback", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyCallbackOnSuccess() {
        cleanupUpdateFile();
        this.mInjector.powerManagerReboot("deviceowner");
    }
}
