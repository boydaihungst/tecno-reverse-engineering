package com.android.server.pm;

import java.util.Map;
/* loaded from: classes2.dex */
final class CommitRequest {
    final int[] mAllUsers;
    final Map<String, ReconciledPackage> mReconciledPackages;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CommitRequest(Map<String, ReconciledPackage> reconciledPackages, int[] allUsers) {
        this.mReconciledPackages = reconciledPackages;
        this.mAllUsers = allUsers;
    }
}
