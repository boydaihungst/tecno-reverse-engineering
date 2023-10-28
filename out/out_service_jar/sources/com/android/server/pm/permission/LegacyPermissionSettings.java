package com.android.server.pm.permission;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class LegacyPermissionSettings {
    private final Object mLock;
    private final ArrayMap<String, LegacyPermission> mPermissions = new ArrayMap<>();
    private final ArrayMap<String, LegacyPermission> mPermissionTrees = new ArrayMap<>();

    public LegacyPermissionSettings(Object lock) {
        this.mLock = lock;
    }

    public List<LegacyPermission> getPermissions() {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mPermissions.values());
        }
        return arrayList;
    }

    public List<LegacyPermission> getPermissionTrees() {
        ArrayList arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mPermissionTrees.values());
        }
        return arrayList;
    }

    public void replacePermissions(List<LegacyPermission> permissions) {
        synchronized (this.mLock) {
            this.mPermissions.clear();
            int permissionsSize = permissions.size();
            for (int i = 0; i < permissionsSize; i++) {
                LegacyPermission permission = permissions.get(i);
                this.mPermissions.put(permission.getPermissionInfo().name, permission);
            }
        }
    }

    public void replacePermissionTrees(List<LegacyPermission> permissionTrees) {
        synchronized (this.mLock) {
            this.mPermissionTrees.clear();
            int permissionsSize = permissionTrees.size();
            for (int i = 0; i < permissionsSize; i++) {
                LegacyPermission permissionTree = permissionTrees.get(i);
                this.mPermissionTrees.put(permissionTree.getPermissionInfo().name, permissionTree);
            }
        }
    }

    public void readPermissions(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissions, parser);
        }
    }

    public void readPermissionTrees(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissionTrees, parser);
        }
    }

    public static void readPermissions(ArrayMap<String, LegacyPermission> out, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (!LegacyPermission.read(out, parser)) {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element reading permissions: " + parser.getName() + " at " + parser.getPositionDescription());
                        }
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public void writePermissions(TypedXmlSerializer serializer) throws IOException {
        synchronized (this.mLock) {
            for (LegacyPermission bp : this.mPermissions.values()) {
                bp.write(serializer);
            }
        }
    }

    public void writePermissionTrees(TypedXmlSerializer serializer) throws IOException {
        synchronized (this.mLock) {
            for (LegacyPermission bp : this.mPermissionTrees.values()) {
                bp.write(serializer);
            }
        }
    }

    public static void dumpPermissions(PrintWriter pw, String packageName, ArraySet<String> permissionNames, List<LegacyPermission> permissions, Map<String, Set<String>> appOpPermissionPackages, boolean externalStorageEnforced, DumpState dumpState) {
        int permissionsSize = permissions.size();
        boolean printedSomething = false;
        for (int i = 0; i < permissionsSize; i++) {
            LegacyPermission permission = permissions.get(i);
            printedSomething = permission.dump(pw, packageName, permissionNames, externalStorageEnforced, printedSomething, dumpState);
        }
        if (packageName == null && permissionNames == null) {
            boolean firstEntry = true;
            for (Map.Entry<String, Set<String>> entry : appOpPermissionPackages.entrySet()) {
                if (firstEntry) {
                    firstEntry = false;
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("AppOp Permissions:");
                }
                pw.print("  AppOp Permission ");
                pw.print(entry.getKey());
                pw.println(":");
                for (String appOpPackageName : entry.getValue()) {
                    pw.print("    ");
                    pw.println(appOpPackageName);
                }
            }
        }
    }
}
