package com.android.server.pm.pkg;

import android.content.pm.ComponentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.parsing.ParsingPackageRead;
/* loaded from: classes2.dex */
public class PackageUserStateUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "PackageUserStateUtils";

    public static boolean isMatch(PackageUserState state, ComponentInfo componentInfo, long flags) {
        return isMatch(state, componentInfo.applicationInfo.isSystemApp(), componentInfo.applicationInfo.enabled, componentInfo.enabled, componentInfo.directBootAware, componentInfo.name, flags);
    }

    public static boolean isMatch(PackageUserState state, boolean isSystem, boolean isPackageEnabled, ParsedMainComponent component, long flags) {
        return isMatch(state, isSystem, isPackageEnabled, component.isEnabled(), component.isDirectBootAware(), component.getName(), flags);
    }

    public static boolean isMatch(PackageUserState state, boolean isSystem, boolean isPackageEnabled, boolean isComponentEnabled, boolean isComponentDirectBootAware, String componentName, long flags) {
        boolean z = true;
        boolean matchUninstalled = (4202496 & flags) != 0;
        if (!isAvailable(state, flags) && (!isSystem || !matchUninstalled)) {
            return reportIfDebug(false, flags);
        }
        if (isEnabled(state, isPackageEnabled, isComponentEnabled, componentName, flags)) {
            if ((1048576 & flags) == 0 || isSystem) {
                boolean matchesUnaware = ((262144 & flags) == 0 || isComponentDirectBootAware) ? false : true;
                boolean matchesAware = (524288 & flags) != 0 && isComponentDirectBootAware;
                if (!matchesUnaware && !matchesAware) {
                    z = false;
                }
                return reportIfDebug(z, flags);
            }
            return reportIfDebug(false, flags);
        }
        return reportIfDebug(false, flags);
    }

    public static boolean isAvailable(PackageUserState state, long flags) {
        boolean matchAnyUser = (4194304 & flags) != 0;
        boolean matchUninstalled = (8192 & flags) != 0;
        boolean matchHidden = (537001984 & flags) != 0;
        if (matchAnyUser) {
            return true;
        }
        if (state.isInstalled()) {
            if (!state.isHidden() || matchUninstalled) {
                return true;
            }
            if (matchHidden && state.isHidden()) {
                return true;
            }
        }
        return false;
    }

    public static boolean reportIfDebug(boolean result, long flags) {
        return result;
    }

    public static boolean isEnabled(PackageUserState state, ComponentInfo componentInfo, long flags) {
        return isEnabled(state, componentInfo.applicationInfo.enabled, componentInfo.enabled, componentInfo.name, flags);
    }

    public static boolean isEnabled(PackageUserState state, boolean isPackageEnabled, ParsedMainComponent parsedComponent, long flags) {
        return isEnabled(state, isPackageEnabled, parsedComponent.isEnabled(), parsedComponent.getName(), flags);
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0020 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0027 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0028  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static boolean isEnabled(PackageUserState state, boolean isPackageEnabled, boolean isComponentEnabled, String componentName, long flags) {
        if ((512 & flags) == 0) {
            switch (state.getEnabledState()) {
                case 0:
                    if (!isPackageEnabled) {
                        return false;
                    }
                    if (state.isComponentEnabled(componentName)) {
                        return true;
                    }
                    if (state.isComponentDisabled(componentName)) {
                        return false;
                    }
                    return isComponentEnabled;
                case 1:
                default:
                    if (state.isComponentEnabled(componentName)) {
                    }
                    break;
                case 2:
                case 3:
                    return false;
                case 4:
                    if ((32768 & flags) == 0) {
                        return false;
                    }
                    if (!isPackageEnabled) {
                    }
                    if (state.isComponentEnabled(componentName)) {
                    }
                    break;
            }
        } else {
            return true;
        }
    }

    public static boolean isPackageEnabled(PackageUserState state, ParsingPackageRead pkg) {
        switch (state.getEnabledState()) {
            case 1:
                return true;
            case 2:
            case 3:
            case 4:
                return false;
            default:
                return pkg.isEnabled();
        }
    }
}
