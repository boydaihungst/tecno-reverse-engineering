package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.utils.WatchedLongSparseArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class SharedLibraryUtils {
    SharedLibraryUtils() {
    }

    public static boolean addSharedLibraryToPackageVersionMap(Map<String, WatchedLongSparseArray<SharedLibraryInfo>> target, SharedLibraryInfo library) {
        String name = library.getName();
        if (target.containsKey(name)) {
            if (library.getType() != 2 || target.get(name).indexOfKey(library.getLongVersion()) >= 0) {
                return false;
            }
        } else {
            target.put(name, new WatchedLongSparseArray<>());
        }
        target.get(name).put(library.getLongVersion(), library);
        return true;
    }

    public static SharedLibraryInfo getSharedLibraryInfo(String name, long version, Map<String, WatchedLongSparseArray<SharedLibraryInfo>> existingLibraries, Map<String, WatchedLongSparseArray<SharedLibraryInfo>> newLibraries) {
        if (newLibraries != null) {
            WatchedLongSparseArray<SharedLibraryInfo> versionedLib = newLibraries.get(name);
            SharedLibraryInfo info = null;
            if (versionedLib != null) {
                SharedLibraryInfo info2 = versionedLib.get(version);
                info = info2;
            }
            if (info != null) {
                return info;
            }
        }
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib2 = existingLibraries.get(name);
        if (versionedLib2 == null) {
            return null;
        }
        return versionedLib2.get(version);
    }

    public static List<SharedLibraryInfo> findSharedLibraries(PackageStateInternal pkgSetting) {
        if (!pkgSetting.getTransientState().getUsesLibraryInfos().isEmpty()) {
            ArrayList<SharedLibraryInfo> retValue = new ArrayList<>();
            Set<String> collectedNames = new HashSet<>();
            for (SharedLibraryInfo info : pkgSetting.getTransientState().getUsesLibraryInfos()) {
                findSharedLibrariesRecursive(info, retValue, collectedNames);
            }
            return retValue;
        }
        return Collections.emptyList();
    }

    private static void findSharedLibrariesRecursive(SharedLibraryInfo info, ArrayList<SharedLibraryInfo> collected, Set<String> collectedNames) {
        if (!collectedNames.contains(info.getName())) {
            collectedNames.add(info.getName());
            collected.add(info);
            if (info.getDependencies() != null) {
                for (SharedLibraryInfo dep : info.getDependencies()) {
                    findSharedLibrariesRecursive(dep, collected, collectedNames);
                }
            }
        }
    }
}
