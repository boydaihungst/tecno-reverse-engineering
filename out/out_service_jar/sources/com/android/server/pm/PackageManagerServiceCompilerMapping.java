package com.android.server.pm;

import android.os.SystemProperties;
import com.android.server.pm.dex.DexoptOptions;
import dalvik.system.DexFile;
/* loaded from: classes2.dex */
public class PackageManagerServiceCompilerMapping {
    static final int REASON_AGGRESSIVE = 1000;
    static final int REASON_SHARED_INDEX;
    public static final String[] REASON_STRINGS;

    static {
        String[] strArr = {"first-boot", "boot-after-ota", "post-boot", "install", "install-fast", "install-bulk", "install-bulk-secondary", "install-bulk-downgraded", "install-bulk-secondary-downgraded", "bg-dexopt", "ab-ota", "inactive", "cmdline", "shared"};
        REASON_STRINGS = strArr;
        int length = strArr.length - 1;
        REASON_SHARED_INDEX = length;
        if (14 != strArr.length) {
            throw new IllegalStateException("REASON_STRINGS not correct");
        }
        if (!"shared".equals(strArr[length])) {
            throw new IllegalStateException("REASON_STRINGS not correct because of shared index");
        }
    }

    private static String getSystemPropertyName(int reason) {
        if (reason >= 0) {
            String[] strArr = REASON_STRINGS;
            if (reason < strArr.length) {
                return "pm.dexopt." + strArr[reason];
            }
        }
        throw new IllegalArgumentException("reason " + reason + " invalid");
    }

    private static String getAndCheckValidity(int reason) {
        String sysPropValue = SystemProperties.get(getSystemPropertyName(reason));
        if (sysPropValue == null || sysPropValue.isEmpty() || (!sysPropValue.equals(DexoptOptions.COMPILER_FILTER_NOOP) && !DexFile.isValidCompilerFilter(sysPropValue))) {
            throw new IllegalStateException("Value \"" + sysPropValue + "\" not valid (reason " + REASON_STRINGS[reason] + ")");
        }
        if (!isFilterAllowedForReason(reason, sysPropValue)) {
            throw new IllegalStateException("Value \"" + sysPropValue + "\" not allowed (reason " + REASON_STRINGS[reason] + ")");
        }
        return sysPropValue;
    }

    private static boolean isFilterAllowedForReason(int reason, String filter) {
        return (reason == REASON_SHARED_INDEX && DexFile.isProfileGuidedCompilerFilter(filter)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void checkProperties() {
        String sysPropName;
        RuntimeException toThrow = null;
        for (int reason = 0; reason <= 13; reason++) {
            try {
                sysPropName = getSystemPropertyName(reason);
            } catch (Exception exc) {
                if (toThrow == null) {
                    toThrow = new IllegalStateException("PMS compiler filter settings are bad.");
                }
                toThrow.addSuppressed(exc);
            }
            if (sysPropName == null || sysPropName.isEmpty()) {
                throw new IllegalStateException("Reason system property name \"" + sysPropName + "\" for reason " + REASON_STRINGS[reason]);
                break;
            }
            getAndCheckValidity(reason);
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }

    public static String getCompilerFilterForReason(int reason) {
        return getAndCheckValidity(reason);
    }

    public static String getDefaultCompilerFilter() {
        String value = SystemProperties.get("dalvik.vm.dex2oat-filter");
        if (value == null || value.isEmpty() || !DexFile.isValidCompilerFilter(value) || DexFile.isProfileGuidedCompilerFilter(value)) {
            return "speed";
        }
        return value;
    }

    public static String getReasonName(int reason) {
        if (reason == 1000) {
            return "user-request";
        }
        if (reason >= 0) {
            String[] strArr = REASON_STRINGS;
            if (reason < strArr.length) {
                return strArr[reason];
            }
        }
        throw new IllegalArgumentException("reason " + reason + " invalid");
    }
}
