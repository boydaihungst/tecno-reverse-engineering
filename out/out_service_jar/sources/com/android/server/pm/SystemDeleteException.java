package com.android.server.pm;
/* loaded from: classes2.dex */
final class SystemDeleteException extends Exception {
    final PackageManagerException mReason;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemDeleteException(PackageManagerException reason) {
        this.mReason = reason;
    }
}
