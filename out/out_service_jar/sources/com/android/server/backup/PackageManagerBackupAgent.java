package com.android.server.backup;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.backup.utils.BackupEligibilityRules;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public class PackageManagerBackupAgent extends BackupAgent {
    private static final String ANCESTRAL_RECORD_KEY = "@ancestral_record@";
    private static final int ANCESTRAL_RECORD_VERSION = 1;
    private static final boolean DEBUG = false;
    private static final String DEFAULT_HOME_KEY = "@home@";
    private static final String GLOBAL_METADATA_KEY = "@meta@";
    private static final String STATE_FILE_HEADER = "=state=";
    private static final int STATE_FILE_VERSION = 2;
    private static final String TAG = "PMBA";
    private static final int UNDEFINED_ANCESTRAL_RECORD_VERSION = -1;
    private List<PackageInfo> mAllPackages;
    private boolean mHasMetadata;
    private PackageManager mPackageManager;
    private ComponentName mRestoredHome;
    private String mRestoredHomeInstaller;
    private ArrayList<byte[]> mRestoredHomeSigHashes;
    private long mRestoredHomeVersion;
    private HashMap<String, Metadata> mRestoredSignatures;
    private ComponentName mStoredHomeComponent;
    private ArrayList<byte[]> mStoredHomeSigHashes;
    private long mStoredHomeVersion;
    private String mStoredIncrementalVersion;
    private int mStoredSdkVersion;
    private int mUserId;
    private HashMap<String, Metadata> mStateVersions = new HashMap<>();
    private final HashSet<String> mExisting = new HashSet<>();

    /* loaded from: classes.dex */
    interface RestoreDataConsumer {
        void consumeRestoreData(BackupDataInput backupDataInput) throws IOException;
    }

    /* loaded from: classes.dex */
    public class Metadata {
        public ArrayList<byte[]> sigHashes;
        public long versionCode;

        Metadata(long version, ArrayList<byte[]> hashes) {
            this.versionCode = version;
            this.sigHashes = hashes;
        }
    }

    public PackageManagerBackupAgent(PackageManager packageMgr, List<PackageInfo> packages, int userId) {
        init(packageMgr, packages, userId);
    }

    public PackageManagerBackupAgent(PackageManager packageMgr, int userId, BackupEligibilityRules backupEligibilityRules) {
        init(packageMgr, null, userId);
        evaluateStorablePackages(backupEligibilityRules);
    }

    private void init(PackageManager packageMgr, List<PackageInfo> packages, int userId) {
        this.mPackageManager = packageMgr;
        this.mAllPackages = packages;
        this.mRestoredSignatures = null;
        this.mHasMetadata = false;
        this.mStoredSdkVersion = Build.VERSION.SDK_INT;
        this.mStoredIncrementalVersion = Build.VERSION.INCREMENTAL;
        this.mUserId = userId;
    }

    public void evaluateStorablePackages(BackupEligibilityRules backupEligibilityRules) {
        this.mAllPackages = getStorableApplications(this.mPackageManager, this.mUserId, backupEligibilityRules);
    }

    public static List<PackageInfo> getStorableApplications(PackageManager pm, int userId, BackupEligibilityRules backupEligibilityRules) {
        List<PackageInfo> pkgs = pm.getInstalledPackagesAsUser(134217728, userId);
        int N = pkgs.size();
        for (int a = N - 1; a >= 0; a--) {
            PackageInfo pkg = pkgs.get(a);
            if (!backupEligibilityRules.appIsEligibleForBackup(pkg.applicationInfo)) {
                pkgs.remove(a);
            }
        }
        return pkgs;
    }

    public boolean hasMetadata() {
        return this.mHasMetadata;
    }

    public Metadata getRestoredMetadata(String packageName) {
        HashMap<String, Metadata> hashMap = this.mRestoredSignatures;
        if (hashMap == null) {
            Slog.w(TAG, "getRestoredMetadata() before metadata read!");
            return null;
        }
        return hashMap.get(packageName);
    }

    public Set<String> getRestoredPackages() {
        HashMap<String, Metadata> hashMap = this.mRestoredSignatures;
        if (hashMap == null) {
            Slog.w(TAG, "getRestoredPackages() before metadata read!");
            return null;
        }
        return hashMap.keySet();
    }

    /* JADX WARN: Removed duplicated region for block: B:103:0x00e1 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0103  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0138 A[Catch: IOException -> 0x00f8, TRY_ENTER, TRY_LEAVE, TryCatch #1 {IOException -> 0x00f8, blocks: (B:32:0x00e1, B:35:0x00eb, B:46:0x0107, B:50:0x011b, B:51:0x0129, B:54:0x0138, B:60:0x015b, B:64:0x017a, B:66:0x017e, B:68:0x0184, B:69:0x0189, B:71:0x0191, B:75:0x01b5, B:77:0x01b9, B:78:0x01df, B:80:0x01e6, B:82:0x01f8, B:81:0x01f3, B:90:0x0222), top: B:103:0x00e1 }] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x014a A[Catch: IOException -> 0x0245, TRY_ENTER, TryCatch #2 {IOException -> 0x0245, blocks: (B:29:0x00d2, B:52:0x012d, B:57:0x014f, B:58:0x0155, B:56:0x014a), top: B:105:0x00d2 }] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x015b A[Catch: IOException -> 0x00f8, TRY_ENTER, TRY_LEAVE, TryCatch #1 {IOException -> 0x00f8, blocks: (B:32:0x00e1, B:35:0x00eb, B:46:0x0107, B:50:0x011b, B:51:0x0129, B:54:0x0138, B:60:0x015b, B:64:0x017a, B:66:0x017e, B:68:0x0184, B:69:0x0189, B:71:0x0191, B:75:0x01b5, B:77:0x01b9, B:78:0x01df, B:80:0x01e6, B:82:0x01f8, B:81:0x01f3, B:90:0x0222), top: B:103:0x00e1 }] */
    @Override // android.app.backup.BackupAgent
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        PackageInfo homeInfo;
        String homeInstaller;
        long homeVersion;
        ArrayList<byte[]> homeSigHashes;
        ComponentName home;
        boolean z;
        boolean needHomeBackup;
        Iterator<PackageInfo> it;
        String str;
        boolean needHomeBackup2;
        Iterator<PackageInfo> it2;
        Iterator<PackageInfo> it3;
        String str2 = GLOBAL_METADATA_KEY;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        DataOutputStream outputBufferStream = new DataOutputStream(outputBuffer);
        parseStateFile(oldState);
        String str3 = this.mStoredIncrementalVersion;
        if (str3 == null || !str3.equals(Build.VERSION.INCREMENTAL)) {
            Slog.i(TAG, "Previous metadata " + this.mStoredIncrementalVersion + " mismatch vs " + Build.VERSION.INCREMENTAL + " - rewriting");
            this.mExisting.clear();
        }
        try {
            outputBufferStream.writeInt(1);
            writeEntity(data, ANCESTRAL_RECORD_KEY, outputBuffer.toByteArray());
            long homeVersion2 = 0;
            ArrayList<byte[]> homeSigHashes2 = null;
            PackageInfo homeInfo2 = null;
            String homeInstaller2 = null;
            ComponentName home2 = getPreferredHomeComponent();
            if (home2 != null) {
                try {
                    try {
                        homeInfo2 = this.mPackageManager.getPackageInfoAsUser(home2.getPackageName(), 134217728, this.mUserId);
                        homeInstaller2 = this.mPackageManager.getInstallerPackageName(home2.getPackageName());
                        homeVersion2 = homeInfo2.getLongVersionCode();
                    } catch (PackageManager.NameNotFoundException e) {
                        homeVersion2 = 0;
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                }
                try {
                    SigningInfo signingInfo = homeInfo2.signingInfo;
                    if (signingInfo == null) {
                        Slog.e(TAG, "Home app has no signing information");
                    } else {
                        Signature[] homeInfoSignatures = signingInfo.getApkContentsSigners();
                        homeSigHashes2 = BackupUtils.hashSignatureArray(homeInfoSignatures);
                    }
                    homeInfo = homeInfo2;
                    homeInstaller = homeInstaller2;
                    homeVersion = homeVersion2;
                    homeSigHashes = homeSigHashes2;
                    home = home2;
                } catch (PackageManager.NameNotFoundException e3) {
                    Slog.w(TAG, "Can't access preferred home info");
                    homeInfo = homeInfo2;
                    homeInstaller = homeInstaller2;
                    homeVersion = homeVersion2;
                    homeSigHashes = null;
                    home = null;
                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    if (homeVersion == this.mStoredHomeVersion) {
                    }
                    z = true;
                    needHomeBackup = z;
                    if (needHomeBackup) {
                    }
                    outputBuffer.reset();
                    if (!this.mExisting.contains(GLOBAL_METADATA_KEY)) {
                    }
                    it = this.mAllPackages.iterator();
                    while (it.hasNext()) {
                    }
                    writeStateFile(this.mAllPackages, home, homeVersion, homeSigHashes, newState);
                }
            } else {
                homeInfo = null;
                homeSigHashes = null;
                home = home2;
                homeInstaller = null;
                homeVersion = 0;
            }
            try {
                PackageManagerInternal pmi2 = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (homeVersion == this.mStoredHomeVersion) {
                    try {
                        if (Objects.equals(home, this.mStoredHomeComponent) && (home == null || BackupUtils.signaturesMatch(this.mStoredHomeSigHashes, homeInfo, pmi2))) {
                            z = false;
                            needHomeBackup = z;
                            if (needHomeBackup) {
                                if (home == null) {
                                    data.writeEntityHeader(DEFAULT_HOME_KEY, -1);
                                } else {
                                    outputBuffer.reset();
                                    outputBufferStream.writeUTF(home.flattenToString());
                                    outputBufferStream.writeLong(homeVersion);
                                    outputBufferStream.writeUTF(homeInstaller != null ? homeInstaller : "");
                                    writeSignatureHashArray(outputBufferStream, homeSigHashes);
                                    writeEntity(data, DEFAULT_HOME_KEY, outputBuffer.toByteArray());
                                }
                            }
                            outputBuffer.reset();
                            if (!this.mExisting.contains(GLOBAL_METADATA_KEY)) {
                                outputBufferStream.writeInt(Build.VERSION.SDK_INT);
                                outputBufferStream.writeUTF(Build.VERSION.INCREMENTAL);
                                writeEntity(data, GLOBAL_METADATA_KEY, outputBuffer.toByteArray());
                            } else {
                                this.mExisting.remove(GLOBAL_METADATA_KEY);
                            }
                            it = this.mAllPackages.iterator();
                            while (it.hasNext()) {
                                PackageInfo pkg = it.next();
                                PackageManagerInternal pmi3 = pmi2;
                                String packName = pkg.packageName;
                                if (packName.equals(str2)) {
                                    pmi2 = pmi3;
                                } else {
                                    try {
                                        str = str2;
                                        try {
                                            needHomeBackup2 = needHomeBackup;
                                        } catch (PackageManager.NameNotFoundException e4) {
                                            needHomeBackup2 = needHomeBackup;
                                            it2 = it;
                                            this.mExisting.add(packName);
                                            pmi2 = pmi3;
                                            str2 = str;
                                            needHomeBackup = needHomeBackup2;
                                            it = it2;
                                        }
                                        try {
                                            PackageInfo info = this.mPackageManager.getPackageInfoAsUser(packName, 134217728, this.mUserId);
                                            if (!this.mExisting.contains(packName)) {
                                                it3 = it;
                                            } else {
                                                this.mExisting.remove(packName);
                                                it3 = it;
                                                if (info.getLongVersionCode() == this.mStateVersions.get(packName).versionCode) {
                                                    pmi2 = pmi3;
                                                    str2 = str;
                                                    needHomeBackup = needHomeBackup2;
                                                    it = it3;
                                                }
                                            }
                                            SigningInfo signingInfo2 = info.signingInfo;
                                            if (signingInfo2 == null) {
                                                Slog.w(TAG, "Not backing up package " + packName + " since it appears to have no signatures.");
                                                pmi2 = pmi3;
                                                str2 = str;
                                                needHomeBackup = needHomeBackup2;
                                                it = it3;
                                            } else {
                                                outputBuffer.reset();
                                                if (info.versionCodeMajor != 0) {
                                                    outputBufferStream.writeInt(Integer.MIN_VALUE);
                                                    outputBufferStream.writeLong(info.getLongVersionCode());
                                                } else {
                                                    outputBufferStream.writeInt(info.versionCode);
                                                }
                                                Signature[] infoSignatures = signingInfo2.getApkContentsSigners();
                                                writeSignatureHashArray(outputBufferStream, BackupUtils.hashSignatureArray(infoSignatures));
                                                writeEntity(data, packName, outputBuffer.toByteArray());
                                                pmi2 = pmi3;
                                                str2 = str;
                                                needHomeBackup = needHomeBackup2;
                                                it = it3;
                                            }
                                        } catch (PackageManager.NameNotFoundException e5) {
                                            it2 = it;
                                            this.mExisting.add(packName);
                                            pmi2 = pmi3;
                                            str2 = str;
                                            needHomeBackup = needHomeBackup2;
                                            it = it2;
                                        }
                                    } catch (PackageManager.NameNotFoundException e6) {
                                        str = str2;
                                    }
                                }
                            }
                            writeStateFile(this.mAllPackages, home, homeVersion, homeSigHashes, newState);
                        }
                    } catch (IOException e7) {
                        Slog.e(TAG, "Unable to write package backup data file!");
                        return;
                    }
                }
                z = true;
                needHomeBackup = z;
                if (needHomeBackup) {
                }
                outputBuffer.reset();
                if (!this.mExisting.contains(GLOBAL_METADATA_KEY)) {
                }
                it = this.mAllPackages.iterator();
                while (it.hasNext()) {
                }
                writeStateFile(this.mAllPackages, home, homeVersion, homeSigHashes, newState);
            } catch (IOException e8) {
            }
        } catch (IOException e9) {
            Slog.e(TAG, "Unable to write package backup data file!");
        }
    }

    private static void writeEntity(BackupDataOutput data, String key, byte[] bytes) throws IOException {
        data.writeEntityHeader(key, bytes.length);
        data.writeEntityData(bytes, bytes.length);
    }

    @Override // android.app.backup.BackupAgent
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        int ancestralRecordVersion = getAncestralRecordVersionValue(data);
        RestoreDataConsumer consumer = getRestoreDataConsumer(ancestralRecordVersion);
        if (consumer == null) {
            Slog.w(TAG, "Ancestral restore set version is unknown to this Android version; not restoring");
        } else {
            consumer.consumeRestoreData(data);
        }
    }

    private int getAncestralRecordVersionValue(BackupDataInput data) throws IOException {
        if (!data.readNextHeader()) {
            return -1;
        }
        String key = data.getKey();
        int dataSize = data.getDataSize();
        if (!ANCESTRAL_RECORD_KEY.equals(key)) {
            return -1;
        }
        byte[] inputBytes = new byte[dataSize];
        data.readEntityData(inputBytes, 0, dataSize);
        ByteArrayInputStream inputBuffer = new ByteArrayInputStream(inputBytes);
        DataInputStream inputBufferStream = new DataInputStream(inputBuffer);
        int ancestralRecordVersionValue = inputBufferStream.readInt();
        return ancestralRecordVersionValue;
    }

    private RestoreDataConsumer getRestoreDataConsumer(int ancestralRecordVersion) {
        switch (ancestralRecordVersion) {
            case -1:
                return new LegacyRestoreDataConsumer();
            case 0:
            default:
                Slog.e(TAG, "Unrecognized ANCESTRAL_RECORD_VERSION: " + ancestralRecordVersion);
                return null;
            case 1:
                return new AncestralVersion1RestoreDataConsumer();
        }
    }

    private static void writeSignatureHashArray(DataOutputStream out, ArrayList<byte[]> hashes) throws IOException {
        out.writeInt(hashes.size());
        Iterator<byte[]> it = hashes.iterator();
        while (it.hasNext()) {
            byte[] buffer = it.next();
            out.writeInt(buffer.length);
            out.write(buffer);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ArrayList<byte[]> readSignatureHashArray(DataInputStream in) {
        try {
            try {
                int num = in.readInt();
                if (num > 20) {
                    Slog.e(TAG, "Suspiciously large sig count in restore data; aborting");
                    throw new IllegalStateException("Bad restore state");
                }
                boolean nonHashFound = false;
                ArrayList<byte[]> sigs = new ArrayList<>(num);
                for (int i = 0; i < num; i++) {
                    int len = in.readInt();
                    byte[] readHash = new byte[len];
                    in.read(readHash);
                    sigs.add(readHash);
                    if (len != 32) {
                        nonHashFound = true;
                    }
                }
                if (nonHashFound) {
                    return BackupUtils.hashSignatureArray(sigs);
                }
                return sigs;
            } catch (EOFException e) {
                Slog.w(TAG, "Read empty signature block");
                return null;
            }
        } catch (IOException e2) {
            Slog.e(TAG, "Unable to read signatures");
            return null;
        }
    }

    private void parseStateFile(ParcelFileDescriptor stateFile) {
        long versionCode;
        this.mExisting.clear();
        this.mStateVersions.clear();
        this.mStoredSdkVersion = 0;
        this.mStoredIncrementalVersion = null;
        this.mStoredHomeComponent = null;
        this.mStoredHomeVersion = 0L;
        this.mStoredHomeSigHashes = null;
        FileInputStream instream = new FileInputStream(stateFile.getFileDescriptor());
        BufferedInputStream inbuffer = new BufferedInputStream(instream);
        DataInputStream in = new DataInputStream(inbuffer);
        boolean ignoreExisting = false;
        try {
            String pkg = in.readUTF();
            if (pkg.equals(STATE_FILE_HEADER)) {
                int stateVersion = in.readInt();
                if (stateVersion > 2) {
                    Slog.w(TAG, "Unsupported state file version " + stateVersion + ", redoing from start");
                    return;
                }
                pkg = in.readUTF();
            } else {
                Slog.i(TAG, "Older version of saved state - rewriting");
                ignoreExisting = true;
            }
            if (pkg.equals(DEFAULT_HOME_KEY)) {
                this.mStoredHomeComponent = ComponentName.unflattenFromString(in.readUTF());
                this.mStoredHomeVersion = in.readLong();
                this.mStoredHomeSigHashes = readSignatureHashArray(in);
                pkg = in.readUTF();
            }
            if (pkg.equals(GLOBAL_METADATA_KEY)) {
                this.mStoredSdkVersion = in.readInt();
                this.mStoredIncrementalVersion = in.readUTF();
                if (!ignoreExisting) {
                    this.mExisting.add(GLOBAL_METADATA_KEY);
                }
                while (true) {
                    String pkg2 = in.readUTF();
                    int versionCodeInt = in.readInt();
                    if (versionCodeInt == Integer.MIN_VALUE) {
                        versionCode = in.readLong();
                    } else {
                        versionCode = versionCodeInt;
                    }
                    if (!ignoreExisting) {
                        this.mExisting.add(pkg2);
                    }
                    this.mStateVersions.put(pkg2, new Metadata(versionCode, null));
                }
            } else {
                Slog.e(TAG, "No global metadata in state file!");
            }
        } catch (EOFException e) {
        } catch (IOException e2) {
            Slog.e(TAG, "Unable to read Package Manager state file: " + e2);
        }
    }

    private ComponentName getPreferredHomeComponent() {
        return this.mPackageManager.getHomeActivities(new ArrayList());
    }

    private void writeStateFile(List<PackageInfo> pkgs, ComponentName preferredHome, long homeVersion, ArrayList<byte[]> homeSigHashes, ParcelFileDescriptor stateFile) {
        FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
        BufferedOutputStream outbuf = new BufferedOutputStream(outstream);
        DataOutputStream out = new DataOutputStream(outbuf);
        try {
            out.writeUTF(STATE_FILE_HEADER);
            out.writeInt(2);
            if (preferredHome != null) {
                out.writeUTF(DEFAULT_HOME_KEY);
                out.writeUTF(preferredHome.flattenToString());
                out.writeLong(homeVersion);
                writeSignatureHashArray(out, homeSigHashes);
            }
            out.writeUTF(GLOBAL_METADATA_KEY);
            out.writeInt(Build.VERSION.SDK_INT);
            out.writeUTF(Build.VERSION.INCREMENTAL);
            for (PackageInfo pkg : pkgs) {
                out.writeUTF(pkg.packageName);
                if (pkg.versionCodeMajor != 0) {
                    out.writeInt(Integer.MIN_VALUE);
                    out.writeLong(pkg.getLongVersionCode());
                } else {
                    out.writeInt(pkg.versionCode);
                }
            }
            out.flush();
        } catch (IOException e) {
            Slog.e(TAG, "Unable to write package manager state file!");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LegacyRestoreDataConsumer implements RestoreDataConsumer {
        private LegacyRestoreDataConsumer() {
        }

        @Override // com.android.server.backup.PackageManagerBackupAgent.RestoreDataConsumer
        public void consumeRestoreData(BackupDataInput data) throws IOException {
            List<ApplicationInfo> restoredApps;
            long versionCode;
            List<ApplicationInfo> restoredApps2 = new ArrayList<>();
            HashMap<String, Metadata> sigMap = new HashMap<>();
            while (true) {
                String key = data.getKey();
                int dataSize = data.getDataSize();
                byte[] inputBytes = new byte[dataSize];
                data.readEntityData(inputBytes, 0, dataSize);
                ByteArrayInputStream inputBuffer = new ByteArrayInputStream(inputBytes);
                DataInputStream inputBufferStream = new DataInputStream(inputBuffer);
                if (key.equals(PackageManagerBackupAgent.GLOBAL_METADATA_KEY)) {
                    int storedSdkVersion = inputBufferStream.readInt();
                    PackageManagerBackupAgent.this.mStoredSdkVersion = storedSdkVersion;
                    PackageManagerBackupAgent.this.mStoredIncrementalVersion = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mHasMetadata = true;
                    restoredApps = restoredApps2;
                } else if (key.equals(PackageManagerBackupAgent.DEFAULT_HOME_KEY)) {
                    String cn = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHome = ComponentName.unflattenFromString(cn);
                    PackageManagerBackupAgent.this.mRestoredHomeVersion = inputBufferStream.readLong();
                    PackageManagerBackupAgent.this.mRestoredHomeInstaller = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHomeSigHashes = PackageManagerBackupAgent.readSignatureHashArray(inputBufferStream);
                    restoredApps = restoredApps2;
                } else {
                    int versionCodeInt = inputBufferStream.readInt();
                    if (versionCodeInt == Integer.MIN_VALUE) {
                        versionCode = inputBufferStream.readLong();
                    } else {
                        versionCode = versionCodeInt;
                    }
                    ArrayList<byte[]> sigs = PackageManagerBackupAgent.readSignatureHashArray(inputBufferStream);
                    if (sigs == null || sigs.size() == 0) {
                        Slog.w(PackageManagerBackupAgent.TAG, "Not restoring package " + key + " since it appears to have no signatures.");
                        restoredApps2 = restoredApps2;
                    } else {
                        ApplicationInfo app = new ApplicationInfo();
                        app.packageName = key;
                        restoredApps2.add(app);
                        restoredApps = restoredApps2;
                        sigMap.put(key, new Metadata(versionCode, sigs));
                    }
                }
                boolean readNextHeader = data.readNextHeader();
                if (readNextHeader) {
                    restoredApps2 = restoredApps;
                } else {
                    PackageManagerBackupAgent.this.mRestoredSignatures = sigMap;
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AncestralVersion1RestoreDataConsumer implements RestoreDataConsumer {
        private AncestralVersion1RestoreDataConsumer() {
        }

        @Override // com.android.server.backup.PackageManagerBackupAgent.RestoreDataConsumer
        public void consumeRestoreData(BackupDataInput data) throws IOException {
            List<ApplicationInfo> restoredApps;
            long versionCode;
            List<ApplicationInfo> restoredApps2 = new ArrayList<>();
            HashMap<String, Metadata> sigMap = new HashMap<>();
            while (data.readNextHeader()) {
                String key = data.getKey();
                int dataSize = data.getDataSize();
                byte[] inputBytes = new byte[dataSize];
                data.readEntityData(inputBytes, 0, dataSize);
                ByteArrayInputStream inputBuffer = new ByteArrayInputStream(inputBytes);
                DataInputStream inputBufferStream = new DataInputStream(inputBuffer);
                if (key.equals(PackageManagerBackupAgent.GLOBAL_METADATA_KEY)) {
                    int storedSdkVersion = inputBufferStream.readInt();
                    PackageManagerBackupAgent.this.mStoredSdkVersion = storedSdkVersion;
                    PackageManagerBackupAgent.this.mStoredIncrementalVersion = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mHasMetadata = true;
                    restoredApps = restoredApps2;
                } else if (key.equals(PackageManagerBackupAgent.DEFAULT_HOME_KEY)) {
                    String cn = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHome = ComponentName.unflattenFromString(cn);
                    PackageManagerBackupAgent.this.mRestoredHomeVersion = inputBufferStream.readLong();
                    PackageManagerBackupAgent.this.mRestoredHomeInstaller = inputBufferStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHomeSigHashes = PackageManagerBackupAgent.readSignatureHashArray(inputBufferStream);
                    restoredApps = restoredApps2;
                } else {
                    int versionCodeInt = inputBufferStream.readInt();
                    if (versionCodeInt == Integer.MIN_VALUE) {
                        versionCode = inputBufferStream.readLong();
                    } else {
                        versionCode = versionCodeInt;
                    }
                    ArrayList<byte[]> sigs = PackageManagerBackupAgent.readSignatureHashArray(inputBufferStream);
                    if (sigs == null || sigs.size() == 0) {
                        Slog.w(PackageManagerBackupAgent.TAG, "Not restoring package " + key + " since it appears to have no signatures.");
                        restoredApps2 = restoredApps2;
                    } else {
                        ApplicationInfo app = new ApplicationInfo();
                        app.packageName = key;
                        restoredApps2.add(app);
                        restoredApps = restoredApps2;
                        sigMap.put(key, new Metadata(versionCode, sigs));
                    }
                }
                restoredApps2 = restoredApps;
            }
            PackageManagerBackupAgent.this.mRestoredSignatures = sigMap;
        }
    }
}
