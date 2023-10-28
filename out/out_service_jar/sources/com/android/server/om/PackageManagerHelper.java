package com.android.server.om;

import android.content.om.OverlayableInfo;
import android.util.ArrayMap;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.IOException;
import java.util.Map;
/* loaded from: classes2.dex */
interface PackageManagerHelper {
    boolean doesTargetDefineOverlayable(String str, int i) throws IOException;

    void enforcePermission(String str, String str2) throws SecurityException;

    String getConfigSignaturePackage();

    Map<String, Map<String, String>> getNamedActors();

    OverlayableInfo getOverlayableForTarget(String str, String str2, int i) throws IOException;

    AndroidPackage getPackageForUser(String str, int i);

    String[] getPackagesForUid(int i);

    ArrayMap<String, AndroidPackage> initializeForUser(int i);

    boolean isInstantApp(String str, int i);

    boolean signaturesMatching(String str, String str2, int i);
}
