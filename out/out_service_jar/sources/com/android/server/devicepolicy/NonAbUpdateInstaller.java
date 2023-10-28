package com.android.server.devicepolicy;

import android.app.admin.StartInstallingUpdateCallback;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RecoverySystem;
import android.util.Log;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import java.io.IOException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NonAbUpdateInstaller extends UpdateInstaller {
    /* JADX INFO: Access modifiers changed from: package-private */
    public NonAbUpdateInstaller(Context context, ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback callback, DevicePolicyManagerService.Injector injector, DevicePolicyConstants constants) {
        super(context, updateFileDescriptor, callback, injector, constants);
    }

    @Override // com.android.server.devicepolicy.UpdateInstaller
    public void installUpdateInThread() {
        try {
            RecoverySystem.installPackage(this.mContext, this.mCopiedUpdateFile);
            notifyCallbackOnSuccess();
        } catch (IOException e) {
            Log.w("UpdateInstaller", "IO error while trying to install non AB update.", e);
            notifyCallbackOnError(1, Log.getStackTraceString(e));
        }
    }
}
