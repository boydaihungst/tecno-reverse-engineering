package com.android.server.pm;

import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
final class PrepareResult {
    public final boolean mClearCodeCache;
    public final PackageSetting mDisabledPs;
    public final AndroidPackage mExistingPackage;
    public final PackageSetting mOriginalPs;
    public final ParsedPackage mPackageToScan;
    public final int mParseFlags;
    public final boolean mReplace;
    public final int mScanFlags;
    public final boolean mSystem;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrepareResult(boolean replace, int scanFlags, int parseFlags, AndroidPackage existingPackage, ParsedPackage packageToScan, boolean clearCodeCache, boolean system, PackageSetting originalPs, PackageSetting disabledPs) {
        this.mReplace = replace;
        this.mScanFlags = scanFlags;
        this.mParseFlags = parseFlags;
        this.mExistingPackage = existingPackage;
        this.mPackageToScan = packageToScan;
        this.mClearCodeCache = clearCodeCache;
        this.mSystem = system;
        this.mOriginalPs = originalPs;
        this.mDisabledPs = disabledPs;
    }
}
