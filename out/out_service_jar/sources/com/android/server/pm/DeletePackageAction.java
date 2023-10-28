package com.android.server.pm;

import android.os.UserHandle;
/* loaded from: classes2.dex */
final class DeletePackageAction {
    public final PackageSetting mDeletingPs;
    public final PackageSetting mDisabledPs;
    public final int mFlags;
    public final PackageRemovedInfo mRemovedInfo;
    public final UserHandle mUser;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeletePackageAction(PackageSetting deletingPs, PackageSetting disabledPs, PackageRemovedInfo removedInfo, int flags, UserHandle user) {
        this.mDeletingPs = deletingPs;
        this.mDisabledPs = disabledPs;
        this.mRemovedInfo = removedInfo;
        this.mFlags = flags;
        this.mUser = user;
    }
}
