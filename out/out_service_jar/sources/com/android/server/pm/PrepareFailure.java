package com.android.server.pm;

import android.util.ExceptionUtils;
/* loaded from: classes2.dex */
final class PrepareFailure extends PackageManagerException {
    public String mConflictingPackage;
    public String mConflictingPermission;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrepareFailure(int error) {
        super(error, "Failed to prepare for install.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrepareFailure(int error, String detailMessage) {
        super(error, detailMessage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrepareFailure(String message, Exception e) {
        super(((PackageManagerException) e).error, ExceptionUtils.getCompleteMessage(message, e));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrepareFailure conflictsWithExistingPermission(String conflictingPermission, String conflictingPackage) {
        this.mConflictingPermission = conflictingPermission;
        this.mConflictingPackage = conflictingPackage;
        return this;
    }
}
