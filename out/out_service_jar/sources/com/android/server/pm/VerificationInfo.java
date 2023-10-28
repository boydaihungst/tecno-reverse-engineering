package com.android.server.pm;

import android.net.Uri;
/* loaded from: classes2.dex */
final class VerificationInfo {
    final int mInstallerUid;
    final int mOriginatingUid;
    final Uri mOriginatingUri;
    final Uri mReferrer;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VerificationInfo(Uri originatingUri, Uri referrer, int originatingUid, int installerUid) {
        this.mOriginatingUri = originatingUri;
        this.mReferrer = referrer;
        this.mOriginatingUid = originatingUid;
        this.mInstallerUid = installerUid;
    }
}
