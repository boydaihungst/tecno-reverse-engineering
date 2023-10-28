package com.android.server.pm.parsing.library;

import com.android.internal.util.ArrayUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class PackageSharedLibraryUpdater {
    public abstract void updatePackage(ParsedPackage parsedPackage, boolean z);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void removeLibrary(ParsedPackage parsedPackage, String libraryName) {
        parsedPackage.removeUsesLibrary(libraryName).removeUsesOptionalLibrary(libraryName);
    }

    static <T> ArrayList<T> prefix(ArrayList<T> cur, T val) {
        if (cur == null) {
            cur = new ArrayList<>();
        }
        cur.add(0, val);
        return cur;
    }

    private static boolean isLibraryPresent(List<String> usesLibraries, List<String> usesOptionalLibraries, String apacheHttpLegacy) {
        return ArrayUtils.contains(usesLibraries, apacheHttpLegacy) || ArrayUtils.contains(usesOptionalLibraries, apacheHttpLegacy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prefixImplicitDependency(ParsedPackage parsedPackage, String existingLibrary, String implicitDependency) {
        List<String> usesLibraries = parsedPackage.getUsesLibraries();
        List<String> usesOptionalLibraries = parsedPackage.getUsesOptionalLibraries();
        if (!isLibraryPresent(usesLibraries, usesOptionalLibraries, implicitDependency)) {
            if (ArrayUtils.contains(usesLibraries, existingLibrary)) {
                parsedPackage.addUsesLibrary(0, implicitDependency);
            } else if (ArrayUtils.contains(usesOptionalLibraries, existingLibrary)) {
                parsedPackage.addUsesOptionalLibrary(0, implicitDependency);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prefixRequiredLibrary(ParsedPackage parsedPackage, String libraryName) {
        List<String> usesLibraries = parsedPackage.getUsesLibraries();
        List<String> usesOptionalLibraries = parsedPackage.getUsesOptionalLibraries();
        boolean alreadyPresent = isLibraryPresent(usesLibraries, usesOptionalLibraries, libraryName);
        if (!alreadyPresent) {
            parsedPackage.addUsesLibrary(0, libraryName);
        }
    }
}
