package com.android.server.pm.pkg;
/* loaded from: classes2.dex */
public final class SELinuxUtil {
    public static final String COMPLETE_STR = ":complete";
    private static final String INSTANT_APP_STR = ":ephemeralapp";

    public static String getSeinfoUser(PackageUserState userState) {
        if (userState.isInstantApp()) {
            return ":ephemeralapp:complete";
        }
        return COMPLETE_STR;
    }
}
