package com.android.server.compat.overrides;

import android.app.compat.PackageOverride;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Pair;
import android.util.Slog;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import libcore.util.HexEncoding;
/* loaded from: classes.dex */
final class AppCompatOverridesParser {
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("true|false", 2);
    static final String FLAG_OWNED_CHANGE_IDS = "owned_change_ids";
    static final String FLAG_REMOVE_OVERRIDES = "remove_overrides";
    private static final String TAG = "AppCompatOverridesParser";
    private static final String WILDCARD_NO_OWNED_CHANGE_IDS_WARNING = "Wildcard can't be used in 'remove_overrides' flag with an empty owned_change_ids' flag";
    private static final String WILDCARD_SYMBOL = "*";
    private final PackageManager mPackageManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppCompatOverridesParser(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, Set<Long>> parseRemoveOverrides(String configStr, Set<Long> ownedChangeIds) {
        String str;
        Set<Long> set = ownedChangeIds;
        if (configStr.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<Long>> result = new ArrayMap<>();
        String str2 = WILDCARD_SYMBOL;
        if (configStr.equals(WILDCARD_SYMBOL)) {
            if (ownedChangeIds.isEmpty()) {
                Slog.w(TAG, WILDCARD_NO_OWNED_CHANGE_IDS_WARNING);
                return Collections.emptyMap();
            }
            List<ApplicationInfo> installedApps = this.mPackageManager.getInstalledApplications(4194304);
            for (ApplicationInfo appInfo : installedApps) {
                result.put(appInfo.packageName, set);
            }
            return result;
        }
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(configStr);
            int i = 0;
            while (i < parser.size()) {
                String packageName = parser.keyAt(i);
                String changeIdsStr = parser.getString(packageName, "");
                if (changeIdsStr.equals(str2)) {
                    if (ownedChangeIds.isEmpty()) {
                        Slog.w(TAG, WILDCARD_NO_OWNED_CHANGE_IDS_WARNING);
                    } else {
                        result.put(packageName, set);
                    }
                } else {
                    String[] split = changeIdsStr.split(":");
                    int length = split.length;
                    int i2 = 0;
                    while (i2 < length) {
                        String changeIdStr = split[i2];
                        try {
                            long changeId = Long.parseLong(changeIdStr);
                            result.computeIfAbsent(packageName, new Function() { // from class: com.android.server.compat.overrides.AppCompatOverridesParser$$ExternalSyntheticLambda0
                                @Override // java.util.function.Function
                                public final Object apply(Object obj) {
                                    return AppCompatOverridesParser.lambda$parseRemoveOverrides$0((String) obj);
                                }
                            }).add(Long.valueOf(changeId));
                            str = str2;
                        } catch (NumberFormatException e) {
                            str = str2;
                            Slog.w(TAG, "Invalid change ID in 'remove_overrides' flag: " + changeIdStr, e);
                        }
                        i2++;
                        str2 = str;
                    }
                }
                i++;
                set = ownedChangeIds;
                str2 = str2;
            }
            return result;
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Invalid format in 'remove_overrides' flag: " + configStr, e2);
            return Collections.emptyMap();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$parseRemoveOverrides$0(String k) {
        return new ArraySet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Set<Long> parseOwnedChangeIds(String configStr) {
        String[] split;
        if (configStr.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new ArraySet<>();
        for (String changeIdStr : configStr.split(",")) {
            try {
                result.add(Long.valueOf(Long.parseLong(changeIdStr)));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "Invalid change ID in 'owned_change_ids' flag: " + changeIdStr, e);
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Long, PackageOverride> parsePackageOverrides(String configStr, String packageName, long versionCode, Set<Long> changeIdsToSkip) {
        Pair<String, String> signatureAndConfig;
        String signature;
        if (configStr.isEmpty()) {
            return Collections.emptyMap();
        }
        PackageOverrideComparator comparator = new PackageOverrideComparator(versionCode);
        Map<Long, PackageOverride> overridesToAdd = new ArrayMap<>();
        Pair<String, String> signatureAndConfig2 = extractSignatureFromConfig(configStr);
        if (signatureAndConfig2 == null) {
            return Collections.emptyMap();
        }
        String signature2 = (String) signatureAndConfig2.first;
        String overridesConfig = (String) signatureAndConfig2.second;
        if (!verifySignature(packageName, signature2)) {
            return Collections.emptyMap();
        }
        String[] split = overridesConfig.split(",");
        int length = split.length;
        int i = 0;
        while (i < length) {
            String overrideEntryString = split[i];
            List<String> changeIdAndVersions = Arrays.asList(overrideEntryString.split(":", 4));
            if (changeIdAndVersions.size() != 4) {
                Slog.w(TAG, "Invalid change override entry: " + overrideEntryString);
                signatureAndConfig = signatureAndConfig2;
                signature = signature2;
            } else {
                try {
                    long changeId = Long.parseLong(changeIdAndVersions.get(0));
                    if (changeIdsToSkip.contains(Long.valueOf(changeId))) {
                        signatureAndConfig = signatureAndConfig2;
                        signature = signature2;
                    } else {
                        String minVersionCodeStr = changeIdAndVersions.get(1);
                        String maxVersionCodeStr = changeIdAndVersions.get(2);
                        String enabledStr = changeIdAndVersions.get(3);
                        if (!BOOLEAN_PATTERN.matcher(enabledStr).matches()) {
                            signatureAndConfig = signatureAndConfig2;
                            Slog.w(TAG, "Invalid enabled string in override entry: " + overrideEntryString);
                            signature = signature2;
                        } else {
                            signatureAndConfig = signatureAndConfig2;
                            boolean enabled = Boolean.parseBoolean(enabledStr);
                            PackageOverride.Builder overrideBuilder = new PackageOverride.Builder().setEnabled(enabled);
                            try {
                                if (minVersionCodeStr.isEmpty()) {
                                    signature = signature2;
                                } else {
                                    signature = signature2;
                                    try {
                                        overrideBuilder.setMinVersionCode(Long.parseLong(minVersionCodeStr));
                                    } catch (NumberFormatException e) {
                                        e = e;
                                        Slog.w(TAG, "Invalid min/max version code in override entry: " + overrideEntryString, e);
                                        i++;
                                        signatureAndConfig2 = signatureAndConfig;
                                        signature2 = signature;
                                    }
                                }
                                if (!maxVersionCodeStr.isEmpty()) {
                                    overrideBuilder.setMaxVersionCode(Long.parseLong(maxVersionCodeStr));
                                }
                                try {
                                    PackageOverride override = overrideBuilder.build();
                                    if (!overridesToAdd.containsKey(Long.valueOf(changeId)) || comparator.compare(override, overridesToAdd.get(Long.valueOf(changeId))) < 0) {
                                        overridesToAdd.put(Long.valueOf(changeId), override);
                                    }
                                } catch (IllegalArgumentException e2) {
                                    Slog.w(TAG, "Failed to build PackageOverride", e2);
                                }
                            } catch (NumberFormatException e3) {
                                e = e3;
                                signature = signature2;
                            }
                        }
                    }
                } catch (NumberFormatException e4) {
                    signatureAndConfig = signatureAndConfig2;
                    signature = signature2;
                    Slog.w(TAG, "Invalid change ID in override entry: " + overrideEntryString, e4);
                }
            }
            i++;
            signatureAndConfig2 = signatureAndConfig;
            signature2 = signature;
        }
        return overridesToAdd;
    }

    private static Pair<String, String> extractSignatureFromConfig(String configStr) {
        List<String> signatureAndConfig = Arrays.asList(configStr.split("~"));
        if (signatureAndConfig.size() == 1) {
            return Pair.create("", configStr);
        }
        if (signatureAndConfig.size() > 2) {
            Slog.w(TAG, "Only one signature per config is supported. Config: " + configStr);
            return null;
        }
        return Pair.create(signatureAndConfig.get(0), signatureAndConfig.get(1));
    }

    private boolean verifySignature(String packageName, String signature) {
        try {
            boolean z = true;
            if (!signature.isEmpty() && !this.mPackageManager.hasSigningCertificate(packageName, HexEncoding.decode(signature), 1)) {
                z = false;
            }
            boolean signatureValid = z;
            if (!signatureValid) {
                Slog.w(TAG, packageName + " did not have expected signature: " + signature);
            }
            return signatureValid;
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Unable to verify signature " + signature + " for " + packageName, e);
            return false;
        }
    }

    /* loaded from: classes.dex */
    private static final class PackageOverrideComparator implements Comparator<PackageOverride> {
        private final long mVersionCode;

        PackageOverrideComparator(long versionCode) {
            this.mVersionCode = versionCode;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(PackageOverride o1, PackageOverride o2) {
            boolean isVersionInRange1 = isVersionInRange(o1, this.mVersionCode);
            boolean isVersionInRange2 = isVersionInRange(o2, this.mVersionCode);
            if (isVersionInRange1 != isVersionInRange2) {
                return isVersionInRange1 ? -1 : 1;
            }
            boolean isVersionAfterRange1 = isVersionAfterRange(o1, this.mVersionCode);
            boolean isVersionAfterRange2 = isVersionAfterRange(o2, this.mVersionCode);
            if (isVersionAfterRange1 != isVersionAfterRange2) {
                return isVersionAfterRange1 ? -1 : 1;
            }
            return Long.compare(getVersionProximity(o1, this.mVersionCode), getVersionProximity(o2, this.mVersionCode));
        }

        private static boolean isVersionInRange(PackageOverride override, long versionCode) {
            return override.getMinVersionCode() <= versionCode && versionCode <= override.getMaxVersionCode();
        }

        private static boolean isVersionAfterRange(PackageOverride override, long versionCode) {
            return override.getMaxVersionCode() < versionCode;
        }

        private static boolean isVersionBeforeRange(PackageOverride override, long versionCode) {
            return override.getMinVersionCode() > versionCode;
        }

        private static long getVersionProximity(PackageOverride override, long versionCode) {
            if (isVersionAfterRange(override, versionCode)) {
                return versionCode - override.getMaxVersionCode();
            }
            if (isVersionBeforeRange(override, versionCode)) {
                return override.getMinVersionCode() - versionCode;
            }
            return 0L;
        }
    }
}
