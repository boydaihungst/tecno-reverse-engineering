package com.android.server.pm;

import android.os.UserHandle;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
final class ScanRequest {
    public final String mCpuAbiOverride;
    public final PackageSetting mDisabledPkgSetting;
    public final boolean mIsPlatformPackage;
    public final AndroidPackage mOldPkg;
    public final PackageSetting mOldPkgSetting;
    public final SharedUserSetting mOldSharedUserSetting;
    public final PackageSetting mOriginalPkgSetting;
    public final int mParseFlags;
    public final ParsedPackage mParsedPackage;
    public final PackageSetting mPkgSetting;
    public final String mRealPkgName;
    public final int mScanFlags;
    public final SharedUserSetting mSharedUserSetting;
    public final UserHandle mUser;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ScanRequest(ParsedPackage parsedPackage, SharedUserSetting oldSharedUserSetting, AndroidPackage oldPkg, PackageSetting pkgSetting, SharedUserSetting sharedUserSetting, PackageSetting disabledPkgSetting, PackageSetting originalPkgSetting, String realPkgName, int parseFlags, int scanFlags, boolean isPlatformPackage, UserHandle user, String cpuAbiOverride) {
        this.mParsedPackage = parsedPackage;
        this.mOldPkg = oldPkg;
        this.mPkgSetting = pkgSetting;
        this.mOldSharedUserSetting = oldSharedUserSetting;
        this.mSharedUserSetting = sharedUserSetting;
        this.mOldPkgSetting = pkgSetting == null ? null : new PackageSetting(pkgSetting);
        this.mDisabledPkgSetting = disabledPkgSetting;
        this.mOriginalPkgSetting = originalPkgSetting;
        this.mRealPkgName = realPkgName;
        this.mParseFlags = parseFlags;
        this.mScanFlags = scanFlags;
        this.mIsPlatformPackage = isPlatformPackage;
        this.mUser = user;
        this.mCpuAbiOverride = cpuAbiOverride;
    }
}
