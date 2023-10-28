package com.android.server.pm;

import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface FeatureConfig {
    void enableLogging(int i, boolean z);

    boolean isGloballyEnabled();

    boolean isLoggingEnabled(int i);

    void onSystemReady();

    boolean packageIsEnabled(AndroidPackage androidPackage);

    FeatureConfig snapshot();

    void updatePackageState(PackageStateInternal packageStateInternal, boolean z);
}
