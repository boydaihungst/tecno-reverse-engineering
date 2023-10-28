package com.android.server.backup.utils;

import android.app.backup.IBackupManagerMonitor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.restore.RestorePolicy;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
/* loaded from: classes.dex */
public class TarBackupReader {
    private static final int TAR_HEADER_LENGTH_FILESIZE = 12;
    private static final int TAR_HEADER_LENGTH_MODE = 8;
    private static final int TAR_HEADER_LENGTH_MODTIME = 12;
    private static final int TAR_HEADER_LENGTH_PATH = 100;
    private static final int TAR_HEADER_LENGTH_PATH_PREFIX = 155;
    private static final int TAR_HEADER_LONG_RADIX = 8;
    private static final int TAR_HEADER_OFFSET_FILESIZE = 124;
    private static final int TAR_HEADER_OFFSET_MODE = 100;
    private static final int TAR_HEADER_OFFSET_MODTIME = 136;
    private static final int TAR_HEADER_OFFSET_PATH = 0;
    private static final int TAR_HEADER_OFFSET_PATH_PREFIX = 345;
    private static final int TAR_HEADER_OFFSET_TYPE_CHAR = 156;
    private final BytesReadListener mBytesReadListener;
    private final InputStream mInputStream;
    private IBackupManagerMonitor mMonitor;
    private byte[] mWidgetData = null;

    public TarBackupReader(InputStream inputStream, BytesReadListener bytesReadListener, IBackupManagerMonitor monitor) {
        this.mInputStream = inputStream;
        this.mBytesReadListener = bytesReadListener;
        this.mMonitor = monitor;
    }

    public FileMetadata readTarHeaders() throws IOException {
        byte[] block = new byte[512];
        FileMetadata info = null;
        if (readTarHeader(block)) {
            try {
                info = new FileMetadata();
                info.size = extractRadix(block, 124, 12, 8);
                info.mtime = extractRadix(block, 136, 12, 8);
                info.mode = extractRadix(block, 100, 8, 8);
                info.path = extractString(block, 345, 155);
                String path = extractString(block, 0, 100);
                if (path.length() > 0) {
                    if (info.path.length() > 0) {
                        info.path += '/';
                    }
                    info.path += path;
                }
                int typeChar = block[156];
                if (typeChar == 120) {
                    boolean gotHeader = readPaxExtendedHeader(info);
                    if (gotHeader) {
                        gotHeader = readTarHeader(block);
                    }
                    if (!gotHeader) {
                        throw new IOException("Bad or missing pax header");
                    }
                    typeChar = block[156];
                }
                switch (typeChar) {
                    case 0:
                        return null;
                    case 48:
                        info.type = 1;
                        break;
                    case 53:
                        info.type = 2;
                        if (info.size != 0) {
                            Slog.w(BackupManagerService.TAG, "Directory entry with nonzero size in header");
                            info.size = 0L;
                            break;
                        }
                        break;
                    default:
                        Slog.e(BackupManagerService.TAG, "Unknown tar entity type: " + typeChar);
                        throw new IOException("Unknown entity type " + typeChar);
                }
                if ("shared/".regionMatches(0, info.path, 0, "shared/".length())) {
                    info.path = info.path.substring("shared/".length());
                    info.packageName = UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                    info.domain = "shared";
                    Slog.i(BackupManagerService.TAG, "File in shared storage: " + info.path);
                } else if ("apps/".regionMatches(0, info.path, 0, "apps/".length())) {
                    info.path = info.path.substring("apps/".length());
                    int slash = info.path.indexOf(47);
                    if (slash >= 0) {
                        info.packageName = info.path.substring(0, slash);
                        info.path = info.path.substring(slash + 1);
                        if (!info.path.equals(UserBackupManagerService.BACKUP_MANIFEST_FILENAME) && !info.path.equals(UserBackupManagerService.BACKUP_METADATA_FILENAME)) {
                            int slash2 = info.path.indexOf(47);
                            if (slash2 >= 0) {
                                info.domain = info.path.substring(0, slash2);
                                info.path = info.path.substring(slash2 + 1);
                            } else {
                                throw new IOException("Illegal semantic path in non-manifest " + info.path);
                            }
                        }
                    } else {
                        throw new IOException("Illegal semantic path in " + info.path);
                    }
                }
            } catch (IOException e) {
                Slog.e(BackupManagerService.TAG, "Parse error in header: " + e.getMessage());
                throw e;
            }
        }
        return info;
    }

    private static int readExactly(InputStream in, byte[] buffer, int offset, int size) throws IOException {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        int soFar = 0;
        while (soFar < size) {
            int nRead = in.read(buffer, offset + soFar, size - soFar);
            if (nRead <= 0) {
                break;
            }
            soFar += nRead;
        }
        return soFar;
    }

    public Signature[] readAppManifestAndReturnSignatures(FileMetadata info) throws IOException {
        if (info.size > 65536) {
            throw new IOException("Restore manifest too big; corrupt? size=" + info.size);
        }
        byte[] buffer = new byte[(int) info.size];
        if (readExactly(this.mInputStream, buffer, 0, (int) info.size) == info.size) {
            this.mBytesReadListener.onBytesRead(info.size);
            String[] str = new String[1];
            try {
                int offset = extractLine(buffer, 0, str);
                int version = Integer.parseInt(str[0]);
                if (version == 1) {
                    int offset2 = extractLine(buffer, offset, str);
                    String manifestPackage = str[0];
                    if (manifestPackage.equals(info.packageName)) {
                        int offset3 = extractLine(buffer, offset2, str);
                        info.version = Integer.parseInt(str[0]);
                        int offset4 = extractLine(buffer, offset3, str);
                        Integer.parseInt(str[0]);
                        int offset5 = extractLine(buffer, offset4, str);
                        info.installerPackageName = str[0].length() > 0 ? str[0] : null;
                        int offset6 = extractLine(buffer, offset5, str);
                        info.hasApk = str[0].equals("1");
                        int offset7 = extractLine(buffer, offset6, str);
                        int numSigs = Integer.parseInt(str[0]);
                        if (numSigs > 0) {
                            Signature[] sigs = new Signature[numSigs];
                            for (int i = 0; i < numSigs; i++) {
                                offset7 = extractLine(buffer, offset7, str);
                                sigs[i] = new Signature(str[0]);
                            }
                            return sigs;
                        }
                        Slog.i(BackupManagerService.TAG, "Missing signature on backed-up package " + info.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 42, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                    } else {
                        Slog.i(BackupManagerService.TAG, "Expected package " + info.packageName + " but restore manifest claims " + manifestPackage);
                        Bundle monitoringExtras = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 43, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras, "android.app.backup.extra.LOG_MANIFEST_PACKAGE_NAME", manifestPackage));
                    }
                } else {
                    Slog.i(BackupManagerService.TAG, "Unknown restore manifest version " + version + " for package " + info.packageName);
                    Bundle monitoringExtras2 = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 44, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras2, "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", version));
                }
            } catch (NumberFormatException e) {
                Slog.w(BackupManagerService.TAG, "Corrupt restore manifest for package " + info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 46, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
            } catch (IllegalArgumentException e2) {
                Slog.w(BackupManagerService.TAG, e2.getMessage());
            }
            return null;
        }
        throw new IOException("Unexpected EOF in manifest");
    }

    public RestorePolicy chooseRestorePolicy(PackageManager packageManager, boolean allowApks, FileMetadata info, Signature[] signatures, PackageManagerInternal pmi, int userId) {
        return chooseRestorePolicy(packageManager, allowApks, info, signatures, pmi, userId, BackupEligibilityRules.forBackup(packageManager, pmi, userId));
    }

    /* JADX WARN: Not initialized variable reg: 16, insn: 0x017e: MOVE  (r9 I:??[OBJECT, ARRAY]) = (r16 I:??[OBJECT, ARRAY] A[D('policy' com.android.server.backup.restore.RestorePolicy)]), block:B:40:0x017e */
    /* JADX WARN: Removed duplicated region for block: B:47:0x018c  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x01ac  */
    /* JADX WARN: Removed duplicated region for block: B:52:0x01cb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public RestorePolicy chooseRestorePolicy(PackageManager packageManager, boolean allowApks, FileMetadata info, Signature[] signatures, PackageManagerInternal pmi, int userId, BackupEligibilityRules eligibilityRules) {
        RestorePolicy policy;
        PackageInfo pkgInfo;
        RestorePolicy policy2;
        if (signatures == null) {
            return RestorePolicy.IGNORE;
        }
        RestorePolicy policy3 = RestorePolicy.IGNORE;
        try {
            pkgInfo = packageManager.getPackageInfoAsUser(info.packageName, 134217728, userId);
            int i = pkgInfo.applicationInfo.flags;
        } catch (PackageManager.NameNotFoundException e) {
            if (!allowApks) {
            }
            Bundle monitoringExtras = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 40, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras, "android.app.backup.extra.LOG_POLICY_ALLOW_APKS", allowApks));
            if (policy == RestorePolicy.ACCEPT_IF_APK) {
            }
            return policy;
        }
        if (eligibilityRules.isAppBackupAllowed(pkgInfo.applicationInfo)) {
            if (!UserHandle.isCore(pkgInfo.applicationInfo.uid)) {
                policy2 = policy3;
            } else if (pkgInfo.applicationInfo.backupAgentName != null) {
                policy2 = policy3;
            } else {
                Slog.w(BackupManagerService.TAG, "Package " + info.packageName + " is system level with no agent");
                policy2 = policy3;
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 38, pkgInfo, 2, null);
            }
            if (!eligibilityRules.signaturesMatch(signatures, pkgInfo)) {
                Slog.w(BackupManagerService.TAG, "Restore manifest signatures do not match installed application for " + info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 37, pkgInfo, 3, null);
            } else {
                if ((pkgInfo.applicationInfo.flags & 131072) != 0) {
                    Slog.i(BackupManagerService.TAG, "Package has restoreAnyVersion; taking data");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 34, pkgInfo, 3, null);
                    policy = RestorePolicy.ACCEPT;
                } else if (pkgInfo.getLongVersionCode() >= info.version) {
                    Slog.i(BackupManagerService.TAG, "Sig + version match; taking data");
                    policy = RestorePolicy.ACCEPT;
                    try {
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 35, pkgInfo, 3, null);
                    } catch (PackageManager.NameNotFoundException e2) {
                        if (!allowApks) {
                            Slog.i(BackupManagerService.TAG, "Package " + info.packageName + " not installed; requiring apk in dataset");
                            policy = RestorePolicy.ACCEPT_IF_APK;
                        } else {
                            policy = RestorePolicy.IGNORE;
                        }
                        Bundle monitoringExtras2 = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 40, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras2, "android.app.backup.extra.LOG_POLICY_ALLOW_APKS", allowApks));
                        if (policy == RestorePolicy.ACCEPT_IF_APK) {
                        }
                        return policy;
                    }
                } else if (allowApks) {
                    Slog.i(BackupManagerService.TAG, "Data version " + info.version + " is newer than installed version " + pkgInfo.getLongVersionCode() + " - requiring apk");
                    policy = RestorePolicy.ACCEPT_IF_APK;
                } else {
                    Slog.i(BackupManagerService.TAG, "Data requires newer version " + info.version + "; ignoring");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 36, pkgInfo, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_OLD_VERSION", info.version));
                    policy = RestorePolicy.IGNORE;
                }
                if (policy == RestorePolicy.ACCEPT_IF_APK && !info.hasApk) {
                    Slog.i(BackupManagerService.TAG, "Cannot restore package " + info.packageName + " without the matching .apk");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 41, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                }
                return policy;
            }
        } else {
            policy2 = policy3;
            Slog.i(BackupManagerService.TAG, "Restore manifest from " + info.packageName + " but allowBackup=false");
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 39, pkgInfo, 3, null);
        }
        policy = policy2;
        if (policy == RestorePolicy.ACCEPT_IF_APK) {
            Slog.i(BackupManagerService.TAG, "Cannot restore package " + info.packageName + " without the matching .apk");
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 41, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
        }
        return policy;
    }

    public void skipTarPadding(long size) throws IOException {
        long partial = (size + 512) % 512;
        if (partial > 0) {
            int needed = 512 - ((int) partial);
            byte[] buffer = new byte[needed];
            if (readExactly(this.mInputStream, buffer, 0, needed) == needed) {
                this.mBytesReadListener.onBytesRead(needed);
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }
    }

    public void readMetadata(FileMetadata info) throws IOException {
        if (info.size > 65536) {
            throw new IOException("Metadata too big; corrupt? size=" + info.size);
        }
        byte[] buffer = new byte[(int) info.size];
        if (readExactly(this.mInputStream, buffer, 0, (int) info.size) == info.size) {
            this.mBytesReadListener.onBytesRead(info.size);
            String[] str = new String[1];
            int offset = extractLine(buffer, 0, str);
            int version = Integer.parseInt(str[0]);
            if (version == 1) {
                int offset2 = extractLine(buffer, offset, str);
                String pkg = str[0];
                if (info.packageName.equals(pkg)) {
                    ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset2, buffer.length - offset2);
                    DataInputStream in = new DataInputStream(bin);
                    while (bin.available() > 0) {
                        int token = in.readInt();
                        int size = in.readInt();
                        if (size > 65536) {
                            throw new IOException("Datum " + Integer.toHexString(token) + " too big; corrupt? size=" + info.size);
                        }
                        switch (token) {
                            case UserBackupManagerService.BACKUP_WIDGET_METADATA_TOKEN /* 33549569 */:
                                byte[] bArr = new byte[size];
                                this.mWidgetData = bArr;
                                in.read(bArr);
                                break;
                            default:
                                Slog.i(BackupManagerService.TAG, "Ignoring metadata blob " + Integer.toHexString(token) + " for " + info.packageName);
                                in.skipBytes(size);
                                break;
                        }
                    }
                    return;
                }
                Slog.w(BackupManagerService.TAG, "Metadata mismatch: package " + info.packageName + " but widget data for " + pkg);
                Bundle monitoringExtras = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 47, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras, "android.app.backup.extra.LOG_WIDGET_PACKAGE_NAME", pkg));
                return;
            }
            Slog.w(BackupManagerService.TAG, "Unsupported metadata version " + version);
            Bundle monitoringExtras2 = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 48, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras2, "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", version));
            return;
        }
        throw new IOException("Unexpected EOF in widget data");
    }

    private static int extractLine(byte[] buffer, int offset, String[] outStr) throws IOException {
        int end = buffer.length;
        if (offset >= end) {
            throw new IOException("Incomplete data");
        }
        int pos = offset;
        while (pos < end) {
            byte c = buffer[pos];
            if (c == 10) {
                break;
            }
            pos++;
        }
        outStr[0] = new String(buffer, offset, pos - offset);
        return pos + 1;
    }

    private boolean readTarHeader(byte[] block) throws IOException {
        int got = readExactly(this.mInputStream, block, 0, 512);
        if (got == 0) {
            return false;
        }
        if (got < 512) {
            throw new IOException("Unable to read full block header");
        }
        this.mBytesReadListener.onBytesRead(512L);
        return true;
    }

    private boolean readPaxExtendedHeader(FileMetadata info) throws IOException {
        if (info.size > 32768) {
            Slog.w(BackupManagerService.TAG, "Suspiciously large pax header size " + info.size + " - aborting");
            throw new IOException("Sanity failure: pax header size " + info.size);
        }
        int numBlocks = (int) ((info.size + 511) >> 9);
        byte[] data = new byte[numBlocks * 512];
        if (readExactly(this.mInputStream, data, 0, data.length) < data.length) {
            throw new IOException("Unable to read full pax header");
        }
        this.mBytesReadListener.onBytesRead(data.length);
        int contentSize = (int) info.size;
        int offset = 0;
        while (true) {
            int eol = offset + 1;
            while (eol < contentSize && data[eol] != 32) {
                eol++;
            }
            if (eol >= contentSize) {
                throw new IOException("Invalid pax data");
            }
            int linelen = (int) extractRadix(data, offset, eol - offset, 10);
            int key = eol + 1;
            int eol2 = (offset + linelen) - 1;
            int value = key + 1;
            while (data[value] != 61 && value <= eol2) {
                value++;
            }
            if (value > eol2) {
                throw new IOException("Invalid pax declaration");
            }
            String keyStr = new String(data, key, value - key, "UTF-8");
            String valStr = new String(data, value + 1, (eol2 - value) - 1, "UTF-8");
            if ("path".equals(keyStr)) {
                info.path = valStr;
            } else if ("size".equals(keyStr)) {
                info.size = Long.parseLong(valStr);
            } else {
                Slog.i(BackupManagerService.TAG, "Unhandled pax key: " + key);
            }
            offset += linelen;
            if (offset >= contentSize) {
                return true;
            }
        }
    }

    private static long extractRadix(byte[] data, int offset, int maxChars, int radix) throws IOException {
        long value = 0;
        int end = offset + maxChars;
        for (int i = offset; i < end; i++) {
            byte b = data[i];
            if (b == 0 || b == 32) {
                break;
            } else if (b < 48 || b > (radix + 48) - 1) {
                throw new IOException("Invalid number in header: '" + ((char) b) + "' for radix " + radix);
            } else {
                value = (radix * value) + (b - 48);
            }
        }
        return value;
    }

    private static String extractString(byte[] data, int offset, int maxChars) throws IOException {
        int end = offset + maxChars;
        int eos = offset;
        while (eos < end && data[eos] != 0) {
            eos++;
        }
        return new String(data, offset, eos - offset, "US-ASCII");
    }

    private static void hexLog(byte[] block) {
        int offset = 0;
        int remaining = block.length;
        StringBuilder buf = new StringBuilder(64);
        while (remaining > 0) {
            buf.append(String.format("%04x   ", Integer.valueOf(offset)));
            int numThisLine = remaining <= 16 ? remaining : 16;
            for (int i = 0; i < numThisLine; i++) {
                buf.append(String.format("%02x ", Byte.valueOf(block[offset + i])));
            }
            Slog.i("hexdump", buf.toString());
            buf.setLength(0);
            remaining -= numThisLine;
            offset += numThisLine;
        }
    }

    public IBackupManagerMonitor getMonitor() {
        return this.mMonitor;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }
}
