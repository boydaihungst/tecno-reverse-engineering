package com.android.server.pm.verify.domain;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Patterns;
import com.android.internal.util.CollectionUtils;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
/* loaded from: classes2.dex */
public final class DomainVerificationUtils {
    private static final ThreadLocal<Matcher> sCachedMatcher = ThreadLocal.withInitial(new Supplier() { // from class: com.android.server.pm.verify.domain.DomainVerificationUtils$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            Matcher matcher;
            matcher = Patterns.DOMAIN_NAME.matcher("");
            return matcher;
        }
    });

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PackageManager.NameNotFoundException throwPackageUnavailable(String packageName) throws PackageManager.NameNotFoundException {
        throw new PackageManager.NameNotFoundException("Package " + packageName + " unavailable");
    }

    public static boolean isDomainVerificationIntent(Intent intent, long resolveInfoFlags) {
        if (intent.isWebIntent()) {
            String host = intent.getData().getHost();
            if (!TextUtils.isEmpty(host) && sCachedMatcher.get().reset(host).matches()) {
                Set<String> categories = intent.getCategories();
                int categoriesSize = CollectionUtils.size(categories);
                if (categoriesSize > 2) {
                    return false;
                }
                if (categoriesSize == 2) {
                    return intent.hasCategory("android.intent.category.DEFAULT") && intent.hasCategory("android.intent.category.BROWSABLE");
                }
                boolean matchDefaultByFlags = (65536 & resolveInfoFlags) != 0;
                if (categoriesSize == 0) {
                    return matchDefaultByFlags;
                }
                if (intent.hasCategory("android.intent.category.BROWSABLE")) {
                    return matchDefaultByFlags;
                }
                return intent.hasCategory("android.intent.category.DEFAULT");
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isChangeEnabled(PlatformCompat platformCompat, AndroidPackage pkg, long changeId) {
        return platformCompat.isChangeEnabledInternalNoLogging(changeId, buildMockAppInfo(pkg));
    }

    private static ApplicationInfo buildMockAppInfo(AndroidPackage pkg) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = pkg.getPackageName();
        appInfo.targetSdkVersion = pkg.getTargetSdkVersion();
        return appInfo;
    }
}
