package com.android.server.pm;

import android.content.pm.ResolveInfo;
/* loaded from: classes2.dex */
public final class CrossProfileDomainInfo {
    int mHighestApprovalLevel;
    ResolveInfo mResolveInfo;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileDomainInfo(ResolveInfo resolveInfo, int highestApprovalLevel) {
        this.mResolveInfo = resolveInfo;
        this.mHighestApprovalLevel = highestApprovalLevel;
    }

    public String toString() {
        return "CrossProfileDomainInfo{resolveInfo=" + this.mResolveInfo + ", highestApprovalLevel=" + this.mHighestApprovalLevel + '}';
    }
}
