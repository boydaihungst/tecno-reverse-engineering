package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ScanResult {
    public final List<String> mChangedAbiCodePath;
    public final List<SharedLibraryInfo> mDynamicSharedLibraryInfos;
    public final boolean mExistingSettingCopied;
    public final PackageSetting mPkgSetting;
    public final int mPreviousAppId = -1;
    public final ScanRequest mRequest;
    public final SharedLibraryInfo mSdkSharedLibraryInfo;
    public final SharedLibraryInfo mStaticSharedLibraryInfo;
    public final boolean mSuccess;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ScanResult(ScanRequest request, boolean success, PackageSetting pkgSetting, List<String> changedAbiCodePath, boolean existingSettingCopied, int previousAppId, SharedLibraryInfo sdkSharedLibraryInfo, SharedLibraryInfo staticSharedLibraryInfo, List<SharedLibraryInfo> dynamicSharedLibraryInfos) {
        this.mRequest = request;
        this.mSuccess = success;
        this.mPkgSetting = pkgSetting;
        this.mChangedAbiCodePath = changedAbiCodePath;
        this.mExistingSettingCopied = existingSettingCopied;
        this.mSdkSharedLibraryInfo = sdkSharedLibraryInfo;
        this.mStaticSharedLibraryInfo = staticSharedLibraryInfo;
        this.mDynamicSharedLibraryInfos = dynamicSharedLibraryInfos;
    }

    public boolean needsNewAppId() {
        return this.mPreviousAppId != -1;
    }
}
