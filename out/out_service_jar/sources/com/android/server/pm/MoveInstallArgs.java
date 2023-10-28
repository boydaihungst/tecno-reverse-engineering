package com.android.server.pm;

import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Environment;
import android.util.Slog;
import com.android.server.pm.Installer;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.io.File;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class MoveInstallArgs extends InstallArgs {
    private File mCodeFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MoveInstallArgs(InstallParams params) {
        super(params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int copyApk() {
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "Moving " + this.mMoveInfo.mPackageName + " from " + this.mMoveInfo.mFromUuid + " to " + this.mMoveInfo.mToUuid);
        }
        synchronized (this.mPm.mInstaller) {
            try {
                this.mPm.mInstaller.moveCompleteApp(this.mMoveInfo.mFromUuid, this.mMoveInfo.mToUuid, this.mMoveInfo.mPackageName, this.mMoveInfo.mAppId, this.mMoveInfo.mSeInfo, this.mMoveInfo.mTargetSdkVersion, this.mMoveInfo.mFromCodePath);
            } catch (Installer.InstallerException e) {
                Slog.w("PackageManager", "Failed to move app", e);
                return RequestStatus.SYS_ETIMEDOUT;
            }
        }
        String toPathName = new File(this.mMoveInfo.mFromCodePath).getName();
        this.mCodeFile = new File(Environment.getDataAppDirectory(this.mMoveInfo.mToUuid), toPathName);
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "codeFile after move is " + this.mCodeFile);
            return 1;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int doPreInstall(int status) {
        if (status != 1) {
            cleanUp(this.mMoveInfo.mToUuid);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public boolean doRename(int status, ParsedPackage parsedPackage) {
        if (status == 1) {
            return true;
        }
        cleanUp(this.mMoveInfo.mToUuid);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int doPostInstall(int status, int uid) {
        if (status == 1) {
            cleanUp(this.mMoveInfo.mFromUuid);
        } else {
            cleanUp(this.mMoveInfo.mToUuid);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public String getCodePath() {
        File file = this.mCodeFile;
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }

    private void cleanUp(String volumeUuid) {
        String toPathName = new File(this.mMoveInfo.mFromCodePath).getName();
        File codeFile = new File(Environment.getDataAppDirectory(volumeUuid), toPathName);
        Slog.d("PackageManager", "Cleaning up " + this.mMoveInfo.mPackageName + " on " + volumeUuid);
        int[] userIds = this.mPm.mUserManager.getUserIds();
        synchronized (this.mPm.mInstallLock) {
            for (int userId : userIds) {
                try {
                    this.mPm.mInstaller.destroyAppData(volumeUuid, this.mMoveInfo.mPackageName, userId, 131075, 0L);
                } catch (Installer.InstallerException e) {
                    Slog.w("PackageManager", String.valueOf(e));
                }
            }
            this.mRemovePackageHelper.removeCodePathLI(codeFile);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public void cleanUpResourcesLI() {
        throw new UnsupportedOperationException();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public boolean doPostDeleteLI(boolean delete) {
        throw new UnsupportedOperationException();
    }
}
