package com.android.server.pm;

import android.content.pm.SigningDetails;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SELinux;
import android.os.Trace;
import android.os.incremental.IncrementalManager;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Slog;
import com.android.internal.content.NativeLibraryHelper;
import com.android.server.pm.Installer;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import libcore.io.IoUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class FileInstallArgs extends InstallArgs {
    private File mCodeFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FileInstallArgs(InstallParams params) {
        super(params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FileInstallArgs(String codePath, String[] instructionSets, PackageManagerService pm) {
        super(OriginInfo.fromNothing(), null, null, 0, InstallSource.EMPTY, null, null, instructionSets, null, null, null, 3, null, 0, SigningDetails.UNKNOWN, 0, 0, false, 0, 0, pm);
        this.mCodeFile = codePath != null ? new File(codePath) : null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int copyApk() {
        Trace.traceBegin(262144L, "copyApk");
        try {
            return doCopyApk();
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private int doCopyApk() {
        int ret;
        if (this.mOriginInfo.mStaged) {
            if (PackageManagerService.DEBUG_INSTALL) {
                Slog.d("PackageManager", this.mOriginInfo.mFile + " already staged; skipping copy");
            }
            this.mCodeFile = this.mOriginInfo.mFile;
            return 1;
        }
        try {
            boolean isEphemeral = (this.mInstallFlags & 2048) != 0;
            File tempDir = this.mPm.mInstallerService.allocateStageDirLegacy(this.mVolumeUuid, isEphemeral);
            this.mCodeFile = tempDir;
            int ret2 = PackageManagerServiceUtils.copyPackage(this.mOriginInfo.mFile.getAbsolutePath(), this.mCodeFile);
            if (ret2 != 1) {
                Slog.e("PackageManager", "Failed to copy package");
                return ret2;
            }
            boolean isIncremental = IncrementalManager.isIncrementalPath(this.mCodeFile.getAbsolutePath());
            File libraryRoot = new File(this.mCodeFile, "lib");
            NativeLibraryHelper.Handle handle = null;
            try {
                try {
                    handle = NativeLibraryHelper.Handle.create(this.mCodeFile);
                    ret = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libraryRoot, this.mAbiOverride, isIncremental);
                } catch (IOException e) {
                    Slog.e("PackageManager", "Copying native libraries failed", e);
                    ret = RequestStatus.SYS_ETIMEDOUT;
                }
                return ret;
            } finally {
                IoUtils.closeQuietly(handle);
            }
        } catch (IOException e2) {
            Slog.w("PackageManager", "Failed to create copy file: " + e2);
            return -4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int doPreInstall(int status) {
        if (status != 1) {
            cleanUp();
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public boolean doRename(int status, ParsedPackage parsedPackage) {
        if (status != 1) {
            cleanUp();
            return false;
        }
        File targetDir = resolveTargetDir();
        File beforeCodeFile = this.mCodeFile;
        File afterCodeFile = PackageManagerServiceUtils.getNextCodePath(targetDir, parsedPackage.getPackageName());
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "Renaming " + beforeCodeFile + " to " + afterCodeFile);
        }
        boolean onIncremental = this.mPm.mIncrementalManager != null && IncrementalManager.isIncrementalPath(beforeCodeFile.getAbsolutePath());
        try {
            PackageManagerServiceUtils.makeDirRecursive(afterCodeFile.getParentFile(), 509);
            if (onIncremental) {
                this.mPm.mIncrementalManager.linkCodePath(beforeCodeFile, afterCodeFile);
            } else {
                Os.rename(beforeCodeFile.getAbsolutePath(), afterCodeFile.getAbsolutePath());
            }
            if (!onIncremental && !SELinux.restoreconRecursive(afterCodeFile)) {
                Slog.w("PackageManager", "Failed to restorecon");
                return false;
            }
            this.mCodeFile = afterCodeFile;
            try {
                parsedPackage.setPath(afterCodeFile.getCanonicalPath());
                parsedPackage.setBaseApkPath(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, parsedPackage.getBaseApkPath()));
                parsedPackage.setSplitCodePaths(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, parsedPackage.getSplitCodePaths()));
                return true;
            } catch (IOException e) {
                Slog.e("PackageManager", "Failed to get path: " + afterCodeFile, e);
                return false;
            }
        } catch (ErrnoException | IOException e2) {
            Slog.w("PackageManager", "Failed to rename", e2);
            return false;
        }
    }

    private File resolveTargetDir() {
        boolean isStagedInstall = (this.mInstallFlags & 2097152) != 0;
        if (isStagedInstall) {
            return Environment.getDataAppDirectory(null);
        }
        return this.mCodeFile.getParentFile();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public int doPostInstall(int status, int uid) {
        if (status != 1) {
            cleanUp();
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

    private boolean cleanUp() {
        File file = this.mCodeFile;
        if (file == null || !file.exists()) {
            return false;
        }
        this.mRemovePackageHelper.removeCodePathLI(this.mCodeFile);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public void cleanUpResourcesLI() {
        List<String> allCodePaths = Collections.EMPTY_LIST;
        File file = this.mCodeFile;
        if (file != null && file.exists()) {
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input.reset(), this.mCodeFile, 0);
            if (result.isSuccess()) {
                allCodePaths = ((PackageLite) result.getResult()).getAllApkPaths();
            }
        }
        cleanUp();
        removeDexFiles(allCodePaths, this.mInstructionSets);
    }

    void removeDexFiles(List<String> allCodePaths, String[] instructionSets) {
        if (!allCodePaths.isEmpty()) {
            if (instructionSets == null) {
                throw new IllegalStateException("instructionSet == null");
            }
            String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
            for (String codePath : allCodePaths) {
                for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                    try {
                        this.mPm.mInstaller.rmdex(codePath, dexCodeInstructionSet);
                    } catch (Installer.InstallerException e) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.pm.InstallArgs
    public boolean doPostDeleteLI(boolean delete) {
        cleanUpResourcesLI();
        return true;
    }
}
