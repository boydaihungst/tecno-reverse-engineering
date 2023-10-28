package com.android.server.pm.permission;

import android.content.pm.PermissionInfo;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public final class LegacyPermission {
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String TAG_ITEM = "item";
    public static final int TYPE_CONFIG = 1;
    public static final int TYPE_DYNAMIC = 2;
    public static final int TYPE_MANIFEST = 0;
    private final int[] mGids;
    private final PermissionInfo mPermissionInfo;
    private final int mType;
    private final int mUid;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PermissionType {
    }

    public LegacyPermission(PermissionInfo permissionInfo, int type, int uid, int[] gids) {
        this.mPermissionInfo = permissionInfo;
        this.mType = type;
        this.mUid = uid;
        this.mGids = gids;
    }

    private LegacyPermission(String name, String packageName, int type) {
        PermissionInfo permissionInfo = new PermissionInfo();
        this.mPermissionInfo = permissionInfo;
        permissionInfo.name = name;
        permissionInfo.packageName = packageName;
        permissionInfo.protectionLevel = 2;
        this.mType = type;
        this.mUid = 0;
        this.mGids = EmptyArray.INT;
    }

    public PermissionInfo getPermissionInfo() {
        return this.mPermissionInfo;
    }

    public int getType() {
        return this.mType;
    }

    public static boolean read(Map<String, LegacyPermission> out, TypedXmlPullParser parser) {
        String tagName = parser.getName();
        if (tagName.equals("item")) {
            String name = parser.getAttributeValue((String) null, "name");
            String packageName = parser.getAttributeValue((String) null, "package");
            String ptype = parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE);
            if (name == null || packageName == null) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: permissions has no name at " + parser.getPositionDescription());
                return false;
            }
            boolean dynamic = "dynamic".equals(ptype);
            LegacyPermission bp = out.get(name);
            if (bp == null || bp.mType != 1) {
                bp = new LegacyPermission(name.intern(), packageName, dynamic ? 2 : 0);
            }
            bp.mPermissionInfo.protectionLevel = readInt(parser, null, "protection", 0);
            PermissionInfo permissionInfo = bp.mPermissionInfo;
            permissionInfo.protectionLevel = PermissionInfo.fixProtectionLevel(permissionInfo.protectionLevel);
            if (dynamic) {
                bp.mPermissionInfo.icon = readInt(parser, null, "icon", 0);
                bp.mPermissionInfo.nonLocalizedLabel = parser.getAttributeValue((String) null, "label");
            }
            out.put(bp.mPermissionInfo.name, bp);
            return true;
        }
        return false;
    }

    private static int readInt(TypedXmlPullParser parser, String namespace, String name, int defaultValue) {
        return parser.getAttributeInt(namespace, name, defaultValue);
    }

    public void write(TypedXmlSerializer serializer) throws IOException {
        if (this.mPermissionInfo.packageName == null) {
            return;
        }
        serializer.startTag((String) null, "item");
        serializer.attribute((String) null, "name", this.mPermissionInfo.name);
        serializer.attribute((String) null, "package", this.mPermissionInfo.packageName);
        if (this.mPermissionInfo.protectionLevel != 0) {
            serializer.attributeInt((String) null, "protection", this.mPermissionInfo.protectionLevel);
        }
        if (this.mType == 2) {
            serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, "dynamic");
            if (this.mPermissionInfo.icon != 0) {
                serializer.attributeInt((String) null, "icon", this.mPermissionInfo.icon);
            }
            if (this.mPermissionInfo.nonLocalizedLabel != null) {
                serializer.attribute((String) null, "label", this.mPermissionInfo.nonLocalizedLabel.toString());
            }
        }
        serializer.endTag((String) null, "item");
    }

    public boolean dump(PrintWriter pw, String packageName, Set<String> permissionNames, boolean readEnforced, boolean printedSomething, DumpState dumpState) {
        if (packageName != null && !packageName.equals(this.mPermissionInfo.packageName)) {
            return false;
        }
        if (permissionNames != null && !permissionNames.contains(this.mPermissionInfo.name)) {
            return false;
        }
        if (!printedSomething) {
            if (dumpState.onTitlePrinted()) {
                pw.println();
            }
            pw.println("Permissions:");
        }
        pw.print("  Permission [");
        pw.print(this.mPermissionInfo.name);
        pw.print("] (");
        pw.print(Integer.toHexString(System.identityHashCode(this)));
        pw.println("):");
        pw.print("    sourcePackage=");
        pw.println(this.mPermissionInfo.packageName);
        pw.print("    uid=");
        pw.print(this.mUid);
        pw.print(" gids=");
        pw.print(Arrays.toString(this.mGids));
        pw.print(" type=");
        pw.print(this.mType);
        pw.print(" prot=");
        pw.println(PermissionInfo.protectionToString(this.mPermissionInfo.protectionLevel));
        if (this.mPermissionInfo != null) {
            pw.print("    perm=");
            pw.println(this.mPermissionInfo);
            if ((this.mPermissionInfo.flags & 1073741824) == 0 || (this.mPermissionInfo.flags & 2) != 0) {
                pw.print("    flags=0x");
                pw.println(Integer.toHexString(this.mPermissionInfo.flags));
            }
        }
        if (Objects.equals(this.mPermissionInfo.name, "android.permission.READ_EXTERNAL_STORAGE")) {
            pw.print("    enforced=");
            pw.println(readEnforced);
            return true;
        }
        return true;
    }
}
