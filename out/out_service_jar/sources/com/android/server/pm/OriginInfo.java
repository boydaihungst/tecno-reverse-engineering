package com.android.server.pm;

import java.io.File;
/* loaded from: classes2.dex */
final class OriginInfo {
    final boolean mExisting;
    final File mFile;
    final File mResolvedFile;
    final String mResolvedPath;
    final boolean mStaged;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OriginInfo fromNothing() {
        return new OriginInfo(null, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OriginInfo fromExistingFile(File file) {
        return new OriginInfo(file, false, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OriginInfo fromStagedFile(File file) {
        return new OriginInfo(file, true, false);
    }

    private OriginInfo(File file, boolean staged, boolean existing) {
        this.mFile = file;
        this.mStaged = staged;
        this.mExisting = existing;
        if (file != null) {
            this.mResolvedPath = file.getAbsolutePath();
            this.mResolvedFile = file;
            return;
        }
        this.mResolvedPath = null;
        this.mResolvedFile = null;
    }
}
