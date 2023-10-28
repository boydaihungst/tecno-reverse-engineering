package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.ShortcutUser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ShortcutLauncher extends ShortcutPackageItem {
    private static final String ATTR_LAUNCHER_USER_ID = "launcher-user";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_PACKAGE_USER_ID = "package-user";
    private static final String ATTR_VALUE = "value";
    private static final String TAG = "ShortcutService";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PIN = "pin";
    static final String TAG_ROOT = "launcher-pins";
    private final int mOwnerUserId;
    private final ArrayMap<ShortcutUser.PackageWithUser, ArraySet<String>> mPinnedShortcuts;

    private ShortcutLauncher(ShortcutUser shortcutUser, int ownerUserId, String packageName, int launcherUserId, ShortcutPackageInfo spi) {
        super(shortcutUser, launcherUserId, packageName, spi != null ? spi : ShortcutPackageInfo.newEmpty());
        this.mPinnedShortcuts = new ArrayMap<>();
        this.mOwnerUserId = ownerUserId;
    }

    public ShortcutLauncher(ShortcutUser shortcutUser, int ownerUserId, String packageName, int launcherUserId) {
        this(shortcutUser, ownerUserId, packageName, launcherUserId, null);
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public int getOwnerUserId() {
        return this.mOwnerUserId;
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected boolean canRestoreAnyVersion() {
        return true;
    }

    private void onRestoreBlocked() {
        ArrayList<ShortcutUser.PackageWithUser> pinnedPackages = new ArrayList<>(this.mPinnedShortcuts.keySet());
        this.mPinnedShortcuts.clear();
        for (int i = pinnedPackages.size() - 1; i >= 0; i--) {
            ShortcutUser.PackageWithUser pu = pinnedPackages.get(i);
            ShortcutPackage p = this.mShortcutUser.getPackageShortcutsIfExists(pu.packageName);
            if (p != null) {
                p.refreshPinnedFlags();
            }
        }
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected void onRestored(int restoreBlockReason) {
        if (restoreBlockReason != 0) {
            onRestoreBlocked();
        }
    }

    public void pinShortcuts(int packageUserId, String packageName, List<String> ids, boolean forPinRequest) {
        ShortcutPackage packageShortcuts = this.mShortcutUser.getPackageShortcutsIfExists(packageName);
        if (packageShortcuts == null) {
            return;
        }
        ShortcutUser.PackageWithUser pu = ShortcutUser.PackageWithUser.of(packageUserId, packageName);
        int idSize = ids.size();
        if (idSize == 0) {
            this.mPinnedShortcuts.remove(pu);
        } else {
            ArraySet<String> prevSet = this.mPinnedShortcuts.get(pu);
            ArraySet<String> newSet = new ArraySet<>();
            for (int i = 0; i < idSize; i++) {
                String id = ids.get(i);
                ShortcutInfo si = packageShortcuts.findShortcutById(id);
                if (si != null && (si.isDynamic() || si.isManifestShortcut() || ((prevSet != null && prevSet.contains(id)) || forPinRequest))) {
                    newSet.add(id);
                }
            }
            this.mPinnedShortcuts.put(pu, newSet);
        }
        packageShortcuts.refreshPinnedFlags();
    }

    public ArraySet<String> getPinnedShortcutIds(String packageName, int packageUserId) {
        return this.mPinnedShortcuts.get(ShortcutUser.PackageWithUser.of(packageUserId, packageName));
    }

    public boolean hasPinned(ShortcutInfo shortcut) {
        ArraySet<String> pinned = getPinnedShortcutIds(shortcut.getPackage(), shortcut.getUserId());
        return pinned != null && pinned.contains(shortcut.getId());
    }

    public void addPinnedShortcut(String packageName, int packageUserId, String id, boolean forPinRequest) {
        ArrayList<String> pinnedList;
        ArraySet<String> pinnedSet = getPinnedShortcutIds(packageName, packageUserId);
        if (pinnedSet != null) {
            pinnedList = new ArrayList<>(pinnedSet.size() + 1);
            pinnedList.addAll(pinnedSet);
        } else {
            pinnedList = new ArrayList<>(1);
        }
        pinnedList.add(id);
        pinShortcuts(packageUserId, packageName, pinnedList, forPinRequest);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cleanUpPackage(String packageName, int packageUserId) {
        return this.mPinnedShortcuts.remove(ShortcutUser.PackageWithUser.of(packageUserId, packageName)) != null;
    }

    public void ensurePackageInfo() {
        PackageInfo pi = this.mShortcutUser.mService.getPackageInfoWithSignatures(getPackageName(), getPackageUserId());
        if (pi == null) {
            Slog.w(TAG, "Package not found: " + getPackageName());
        } else {
            getPackageInfo().updateFromPackageInfo(pi);
        }
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public void saveToXml(TypedXmlSerializer out, boolean forBackup) throws IOException {
        int size;
        if ((forBackup && !getPackageInfo().isBackupAllowed()) || (size = this.mPinnedShortcuts.size()) == 0) {
            return;
        }
        out.startTag((String) null, TAG_ROOT);
        ShortcutService.writeAttr(out, ATTR_PACKAGE_NAME, getPackageName());
        ShortcutService.writeAttr(out, ATTR_LAUNCHER_USER_ID, getPackageUserId());
        getPackageInfo().saveToXml(this.mShortcutUser.mService, out, forBackup);
        for (int i = 0; i < size; i++) {
            ShortcutUser.PackageWithUser pu = this.mPinnedShortcuts.keyAt(i);
            if (!forBackup || pu.userId == getOwnerUserId()) {
                out.startTag((String) null, "package");
                ShortcutService.writeAttr(out, ATTR_PACKAGE_NAME, pu.packageName);
                ShortcutService.writeAttr(out, ATTR_PACKAGE_USER_ID, pu.userId);
                ArraySet<String> ids = this.mPinnedShortcuts.valueAt(i);
                int idSize = ids.size();
                for (int j = 0; j < idSize; j++) {
                    ShortcutService.writeTagValue(out, TAG_PIN, ids.valueAt(j));
                }
                out.endTag((String) null, "package");
            }
        }
        out.endTag((String) null, TAG_ROOT);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [305=4] */
    public static ShortcutLauncher loadFromFile(File path, ShortcutUser shortcutUser, int ownerUserId, boolean fromBackup) {
        AtomicFile file = new AtomicFile(path);
        try {
            FileInputStream in = file.openRead();
            ShortcutLauncher ret = null;
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(in);
                while (true) {
                    int type = parser.next();
                    if (type == 1) {
                        return ret;
                    }
                    if (type == 2) {
                        int depth = parser.getDepth();
                        String tag = parser.getName();
                        if (depth == 1 && TAG_ROOT.equals(tag)) {
                            ret = loadFromXml(parser, shortcutUser, ownerUserId, fromBackup);
                        } else {
                            ShortcutService.throwForInvalidTag(depth, tag);
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e);
                return null;
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0058, code lost:
        if (r13.equals("package") != false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00a9, code lost:
        if (r13.equals(com.android.server.pm.ShortcutLauncher.TAG_PIN) != false) goto L35;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ShortcutLauncher loadFromXml(TypedXmlPullParser parser, ShortcutUser shortcutUser, int ownerUserId, boolean fromBackup) throws IOException, XmlPullParserException {
        int i = ownerUserId;
        String launcherPackageName = ShortcutService.parseStringAttribute(parser, ATTR_PACKAGE_NAME);
        int launcherUserId = fromBackup ? i : ShortcutService.parseIntAttribute(parser, ATTR_LAUNCHER_USER_ID, i);
        ShortcutLauncher ret = new ShortcutLauncher(shortcutUser, i, launcherPackageName, launcherUserId);
        ArraySet<String> ids = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            boolean z = true;
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    boolean z2 = false;
                    if (depth == outerDepth + 1) {
                        switch (tag.hashCode()) {
                            case -1923478059:
                                if (tag.equals("package-info")) {
                                    z = false;
                                    break;
                                }
                                z = true;
                                break;
                            case -807062458:
                                break;
                            default:
                                z = true;
                                break;
                        }
                        switch (z) {
                            case false:
                                ret.getPackageInfo().loadFromXml(parser, fromBackup);
                                break;
                            case true:
                                String packageName = ShortcutService.parseStringAttribute(parser, ATTR_PACKAGE_NAME);
                                int packageUserId = fromBackup ? i : ShortcutService.parseIntAttribute(parser, ATTR_PACKAGE_USER_ID, i);
                                ids = new ArraySet<>();
                                ret.mPinnedShortcuts.put(ShortcutUser.PackageWithUser.of(packageUserId, packageName), ids);
                                i = ownerUserId;
                                continue;
                        }
                    }
                    if (depth == outerDepth + 2) {
                        switch (tag.hashCode()) {
                            case 110997:
                                break;
                            default:
                                z2 = true;
                                break;
                        }
                        switch (z2) {
                            case false:
                                if (ids != null) {
                                    ids.add(ShortcutService.parseStringAttribute(parser, ATTR_VALUE));
                                    break;
                                } else {
                                    Slog.w(TAG, "pin in invalid place");
                                    break;
                                }
                        }
                    }
                    ShortcutService.warnForInvalidTag(depth, tag);
                }
                i = ownerUserId;
            }
        }
        return ret;
    }

    public void dump(PrintWriter pw, String prefix, ShortcutService.DumpFilter filter) {
        pw.println();
        pw.print(prefix);
        pw.print("Launcher: ");
        pw.print(getPackageName());
        pw.print("  Package user: ");
        pw.print(getPackageUserId());
        pw.print("  Owner user: ");
        pw.print(getOwnerUserId());
        pw.println();
        getPackageInfo().dump(pw, prefix + "  ");
        pw.println();
        int size = this.mPinnedShortcuts.size();
        for (int i = 0; i < size; i++) {
            pw.println();
            ShortcutUser.PackageWithUser pu = this.mPinnedShortcuts.keyAt(i);
            pw.print(prefix);
            pw.print("  ");
            pw.print("Package: ");
            pw.print(pu.packageName);
            pw.print("  User: ");
            pw.println(pu.userId);
            ArraySet<String> ids = this.mPinnedShortcuts.valueAt(i);
            int idSize = ids.size();
            for (int j = 0; j < idSize; j++) {
                pw.print(prefix);
                pw.print("    Pinned: ");
                pw.print(ids.valueAt(j));
                pw.println();
            }
        }
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = super.dumpCheckin(clear);
        return result;
    }

    ArraySet<String> getAllPinnedShortcutsForTest(String packageName, int packageUserId) {
        return new ArraySet<>(this.mPinnedShortcuts.get(ShortcutUser.PackageWithUser.of(packageUserId, packageName)));
    }

    @Override // com.android.server.pm.ShortcutPackageItem
    protected File getShortcutPackageItemFile() {
        File path = new File(this.mShortcutUser.mService.injectUserDataPath(this.mShortcutUser.getUserId()), "launchers");
        String fileName = getPackageName() + getPackageUserId() + ".xml";
        return new File(path, fileName);
    }
}
