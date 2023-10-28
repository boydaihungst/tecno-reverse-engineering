package com.android.server.pm;

import android.content.Intent;
import android.content.IntentFilter;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.utils.WatchedArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
/* loaded from: classes2.dex */
final class AppsFilterUtils {
    AppsFilterUtils() {
    }

    public static boolean requestsQueryAllPackages(AndroidPackage pkg) {
        return pkg.getRequestedPermissions().contains("android.permission.QUERY_ALL_PACKAGES");
    }

    public static boolean canQueryViaComponents(AndroidPackage querying, AndroidPackage potentialTarget, WatchedArrayList<String> protectedBroadcasts) {
        if (!querying.getQueriesIntents().isEmpty()) {
            for (Intent intent : querying.getQueriesIntents()) {
                if (matchesPackage(intent, potentialTarget, protectedBroadcasts)) {
                    return true;
                }
            }
        }
        return !querying.getQueriesProviders().isEmpty() && matchesProviders(querying.getQueriesProviders(), potentialTarget);
    }

    public static boolean canQueryViaPackage(AndroidPackage querying, AndroidPackage potentialTarget) {
        return !querying.getQueriesPackages().isEmpty() && querying.getQueriesPackages().contains(potentialTarget.getPackageName());
    }

    public static boolean canQueryAsInstaller(PackageStateInternal querying, AndroidPackage potentialTarget) {
        InstallSource installSource = querying.getInstallSource();
        if (potentialTarget.getPackageName().equals(installSource.installerPackageName)) {
            return true;
        }
        return !installSource.isInitiatingPackageUninstalled && potentialTarget.getPackageName().equals(installSource.initiatingPackageName);
    }

    public static boolean canQueryViaUsesLibrary(AndroidPackage querying, AndroidPackage potentialTarget) {
        if (potentialTarget.getLibraryNames().isEmpty()) {
            return false;
        }
        List<String> libNames = potentialTarget.getLibraryNames();
        int size = libNames.size();
        for (int i = 0; i < size; i++) {
            String libName = libNames.get(i);
            if (querying.getUsesLibraries().contains(libName) || querying.getUsesOptionalLibraries().contains(libName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesProviders(Set<String> queriesAuthorities, AndroidPackage potentialTarget) {
        for (int p = ArrayUtils.size(potentialTarget.getProviders()) - 1; p >= 0; p--) {
            ParsedProvider provider = potentialTarget.getProviders().get(p);
            if (provider.isExported() && provider.getAuthority() != null) {
                StringTokenizer authorities = new StringTokenizer(provider.getAuthority(), ";", false);
                while (authorities.hasMoreElements()) {
                    if (queriesAuthorities.contains(authorities.nextToken())) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private static boolean matchesPackage(Intent intent, AndroidPackage potentialTarget, WatchedArrayList<String> protectedBroadcasts) {
        return matchesAnyComponents(intent, potentialTarget.getServices(), null) || matchesAnyComponents(intent, potentialTarget.getActivities(), null) || matchesAnyComponents(intent, potentialTarget.getReceivers(), protectedBroadcasts) || matchesAnyComponents(intent, potentialTarget.getProviders(), null);
    }

    private static boolean matchesAnyComponents(Intent intent, List<? extends ParsedMainComponent> components, WatchedArrayList<String> protectedBroadcasts) {
        for (int i = ArrayUtils.size(components) - 1; i >= 0; i--) {
            ParsedMainComponent component = components.get(i);
            if (component.isExported() && matchesAnyFilter(intent, component, protectedBroadcasts)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyFilter(Intent intent, ParsedComponent component, WatchedArrayList<String> protectedBroadcasts) {
        List<ParsedIntentInfo> intents = component.getIntents();
        for (int i = ArrayUtils.size(intents) - 1; i >= 0; i--) {
            IntentFilter intentFilter = intents.get(i).getIntentFilter();
            if (matchesIntentFilter(intent, intentFilter, protectedBroadcasts)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesIntentFilter(Intent intent, IntentFilter intentFilter, WatchedArrayList<String> protectedBroadcasts) {
        return intentFilter.match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(), intent.getCategories(), "AppsFilter", true, protectedBroadcasts != null ? protectedBroadcasts.untrackedStorage() : null) > 0;
    }
}
