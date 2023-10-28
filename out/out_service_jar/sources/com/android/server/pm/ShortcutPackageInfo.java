package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.backup.BackupUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ShortcutPackageInfo {
    private static final String ATTR_BACKUP_ALLOWED = "allow-backup";
    private static final String ATTR_BACKUP_ALLOWED_INITIALIZED = "allow-backup-initialized";
    private static final String ATTR_BACKUP_SOURCE_BACKUP_ALLOWED = "bk_src_backup-allowed";
    private static final String ATTR_BACKUP_SOURCE_VERSION = "bk_src_version";
    private static final String ATTR_LAST_UPDATE_TIME = "last_udpate_time";
    private static final String ATTR_SHADOW = "shadow";
    private static final String ATTR_SIGNATURE_HASH = "hash";
    private static final String ATTR_VERSION = "version";
    private static final String TAG = "ShortcutService";
    static final String TAG_ROOT = "package-info";
    private static final String TAG_SIGNATURE = "signature";
    private boolean mBackupAllowedInitialized;
    private boolean mIsShadow;
    private long mLastUpdateTime;
    private ArrayList<byte[]> mSigHashes;
    private long mVersionCode;
    private long mBackupSourceVersionCode = -1;
    private boolean mBackupAllowed = false;
    private boolean mBackupSourceBackupAllowed = false;

    private ShortcutPackageInfo(long versionCode, long lastUpdateTime, ArrayList<byte[]> sigHashes, boolean isShadow) {
        this.mVersionCode = -1L;
        this.mVersionCode = versionCode;
        this.mLastUpdateTime = lastUpdateTime;
        this.mIsShadow = isShadow;
        this.mSigHashes = sigHashes;
    }

    public static ShortcutPackageInfo newEmpty() {
        return new ShortcutPackageInfo(-1L, 0L, new ArrayList(0), false);
    }

    public boolean isShadow() {
        return this.mIsShadow;
    }

    public void setShadow(boolean shadow) {
        this.mIsShadow = shadow;
    }

    public long getVersionCode() {
        return this.mVersionCode;
    }

    public long getBackupSourceVersionCode() {
        return this.mBackupSourceVersionCode;
    }

    public boolean isBackupSourceBackupAllowed() {
        return this.mBackupSourceBackupAllowed;
    }

    public long getLastUpdateTime() {
        return this.mLastUpdateTime;
    }

    public boolean isBackupAllowed() {
        return this.mBackupAllowed;
    }

    public void updateFromPackageInfo(PackageInfo pi) {
        if (pi != null) {
            this.mVersionCode = pi.getLongVersionCode();
            this.mLastUpdateTime = pi.lastUpdateTime;
            this.mBackupAllowed = ShortcutService.shouldBackupApp(pi);
            this.mBackupAllowedInitialized = true;
        }
    }

    public boolean hasSignatures() {
        return this.mSigHashes.size() > 0;
    }

    public int canRestoreTo(ShortcutService s, PackageInfo currentPackage, boolean anyVersionOkay) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        if (!BackupUtils.signaturesMatch(this.mSigHashes, currentPackage, pmi)) {
            Slog.w(TAG, "Can't restore: Package signature mismatch");
            return 102;
        } else if (!ShortcutService.shouldBackupApp(currentPackage) || !this.mBackupSourceBackupAllowed) {
            Slog.w(TAG, "Can't restore: package didn't or doesn't allow backup");
            return 101;
        } else if (anyVersionOkay || currentPackage.getLongVersionCode() >= this.mBackupSourceVersionCode) {
            return 0;
        } else {
            Slog.w(TAG, String.format("Can't restore: package current version %d < backed up version %d", Long.valueOf(currentPackage.getLongVersionCode()), Long.valueOf(this.mBackupSourceVersionCode)));
            return 100;
        }
    }

    public static ShortcutPackageInfo generateForInstalledPackageForTest(ShortcutService s, String packageName, int packageUserId) {
        PackageInfo pi = s.getPackageInfoWithSignatures(packageName, packageUserId);
        SigningInfo signingInfo = pi.signingInfo;
        if (signingInfo == null) {
            Slog.e(TAG, "Can't get signatures: package=" + packageName);
            return null;
        }
        Signature[] signatures = signingInfo.getApkContentsSigners();
        ShortcutPackageInfo ret = new ShortcutPackageInfo(pi.getLongVersionCode(), pi.lastUpdateTime, BackupUtils.hashSignatureArray(signatures), false);
        ret.mBackupSourceBackupAllowed = ShortcutService.shouldBackupApp(pi);
        ret.mBackupSourceVersionCode = pi.getLongVersionCode();
        return ret;
    }

    public void refreshSignature(ShortcutService s, ShortcutPackageItem pkg) {
        if (this.mIsShadow) {
            s.wtf("Attempted to refresh package info for shadow package " + pkg.getPackageName() + ", user=" + pkg.getOwnerUserId());
            return;
        }
        PackageInfo pi = s.getPackageInfoWithSignatures(pkg.getPackageName(), pkg.getPackageUserId());
        if (pi == null) {
            Slog.w(TAG, "Package not found: " + pkg.getPackageName());
            return;
        }
        SigningInfo signingInfo = pi.signingInfo;
        if (signingInfo == null) {
            Slog.w(TAG, "Not refreshing signature for " + pkg.getPackageName() + " since it appears to have no signing info.");
            return;
        }
        Signature[] signatures = signingInfo.getApkContentsSigners();
        this.mSigHashes = BackupUtils.hashSignatureArray(signatures);
    }

    public void saveToXml(ShortcutService s, TypedXmlSerializer out, boolean forBackup) throws IOException {
        if (forBackup && !this.mBackupAllowedInitialized) {
            s.wtf("Backup happened before mBackupAllowed is initialized.");
        }
        out.startTag((String) null, TAG_ROOT);
        ShortcutService.writeAttr(out, ATTR_VERSION, this.mVersionCode);
        ShortcutService.writeAttr(out, ATTR_LAST_UPDATE_TIME, this.mLastUpdateTime);
        ShortcutService.writeAttr(out, ATTR_SHADOW, this.mIsShadow);
        ShortcutService.writeAttr(out, ATTR_BACKUP_ALLOWED, this.mBackupAllowed);
        ShortcutService.writeAttr(out, ATTR_BACKUP_ALLOWED_INITIALIZED, this.mBackupAllowedInitialized);
        ShortcutService.writeAttr(out, ATTR_BACKUP_SOURCE_VERSION, this.mBackupSourceVersionCode);
        ShortcutService.writeAttr(out, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, this.mBackupSourceBackupAllowed);
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            out.startTag((String) null, TAG_SIGNATURE);
            String encoded = Base64.getEncoder().encodeToString(this.mSigHashes.get(i));
            ShortcutService.writeAttr(out, ATTR_SIGNATURE_HASH, encoded);
            out.endTag((String) null, TAG_SIGNATURE);
        }
        out.endTag((String) null, TAG_ROOT);
    }

    public void loadFromXml(TypedXmlPullParser parser, boolean fromBackup) throws IOException, XmlPullParserException {
        char c;
        TypedXmlPullParser typedXmlPullParser = parser;
        long versionCode = ShortcutService.parseLongAttribute(typedXmlPullParser, ATTR_VERSION, -1L);
        long lastUpdateTime = ShortcutService.parseLongAttribute(typedXmlPullParser, ATTR_LAST_UPDATE_TIME);
        int i = 1;
        boolean shadow = fromBackup || ShortcutService.parseBooleanAttribute(typedXmlPullParser, ATTR_SHADOW);
        long backupSourceVersion = ShortcutService.parseLongAttribute(typedXmlPullParser, ATTR_BACKUP_SOURCE_VERSION, -1L);
        boolean backupAllowed = ShortcutService.parseBooleanAttribute(typedXmlPullParser, ATTR_BACKUP_ALLOWED, true);
        boolean backupSourceBackupAllowed = ShortcutService.parseBooleanAttribute(typedXmlPullParser, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, true);
        ArrayList<byte[]> hashes = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != i && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    if (depth == outerDepth + 1) {
                        switch (tag.hashCode()) {
                            case 1073584312:
                                if (tag.equals(TAG_SIGNATURE)) {
                                    c = 0;
                                    break;
                                }
                            default:
                                c = 65535;
                                break;
                        }
                        switch (c) {
                            case 0:
                                String hash = ShortcutService.parseStringAttribute(typedXmlPullParser, ATTR_SIGNATURE_HASH);
                                byte[] decoded = Base64.getDecoder().decode(hash);
                                hashes.add(decoded);
                                typedXmlPullParser = parser;
                                i = 1;
                                break;
                        }
                    }
                    ShortcutService.warnForInvalidTag(depth, tag);
                    typedXmlPullParser = parser;
                    i = 1;
                }
            }
        }
        if (fromBackup) {
            this.mVersionCode = -1L;
            this.mBackupSourceVersionCode = versionCode;
            this.mBackupSourceBackupAllowed = backupAllowed;
        } else {
            this.mVersionCode = versionCode;
            this.mBackupSourceVersionCode = backupSourceVersion;
            this.mBackupSourceBackupAllowed = backupSourceBackupAllowed;
        }
        this.mLastUpdateTime = lastUpdateTime;
        this.mIsShadow = shadow;
        this.mSigHashes = hashes;
        this.mBackupAllowed = false;
        this.mBackupAllowedInitialized = false;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println();
        pw.print(prefix);
        pw.println("PackageInfo:");
        pw.print(prefix);
        pw.print("  IsShadow: ");
        pw.print(this.mIsShadow);
        pw.print(this.mIsShadow ? " (not installed)" : " (installed)");
        pw.println();
        pw.print(prefix);
        pw.print("  Version: ");
        pw.print(this.mVersionCode);
        pw.println();
        if (this.mBackupAllowedInitialized) {
            pw.print(prefix);
            pw.print("  Backup Allowed: ");
            pw.print(this.mBackupAllowed);
            pw.println();
        }
        if (this.mBackupSourceVersionCode != -1) {
            pw.print(prefix);
            pw.print("  Backup source version: ");
            pw.print(this.mBackupSourceVersionCode);
            pw.println();
            pw.print(prefix);
            pw.print("  Backup source backup allowed: ");
            pw.print(this.mBackupSourceBackupAllowed);
            pw.println();
        }
        pw.print(prefix);
        pw.print("  Last package update time: ");
        pw.print(this.mLastUpdateTime);
        pw.println();
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            pw.print(prefix);
            pw.print("    ");
            pw.print("SigHash: ");
            pw.println(HexEncoding.encode(this.mSigHashes.get(i)));
        }
    }
}
