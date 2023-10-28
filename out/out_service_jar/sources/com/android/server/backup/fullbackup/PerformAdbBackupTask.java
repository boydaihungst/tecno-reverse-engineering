package com.android.server.backup.fullbackup;

import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.PasswordUtils;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
/* loaded from: classes.dex */
public class PerformAdbBackupTask extends FullBackupTask implements BackupRestoreTask {
    private final boolean mAllApps;
    private final BackupEligibilityRules mBackupEligibilityRules;
    private final boolean mCompress;
    private final int mCurrentOpToken;
    private final String mCurrentPassword;
    private PackageInfo mCurrentTarget;
    private final boolean mDoWidgets;
    private final String mEncryptPassword;
    private final boolean mIncludeApks;
    private final boolean mIncludeObbs;
    private final boolean mIncludeShared;
    private final boolean mIncludeSystem;
    private final boolean mKeyValue;
    private final AtomicBoolean mLatch;
    private final OperationStorage mOperationStorage;
    private final ParcelFileDescriptor mOutputFile;
    private final ArrayList<String> mPackages;
    private final UserBackupManagerService mUserBackupManagerService;

    public PerformAdbBackupTask(UserBackupManagerService backupManagerService, OperationStorage operationStorage, ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, String curPassword, String encryptPassword, boolean doAllApps, boolean doSystem, boolean doCompress, boolean doKeyValue, String[] packages, AtomicBoolean latch, BackupEligibilityRules backupEligibilityRules) {
        super(observer);
        ArrayList<String> arrayList;
        this.mUserBackupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mLatch = latch;
        this.mOutputFile = fd;
        this.mIncludeApks = includeApks;
        this.mIncludeObbs = includeObbs;
        this.mIncludeShared = includeShared;
        this.mDoWidgets = doWidgets;
        this.mAllApps = doAllApps;
        this.mIncludeSystem = doSystem;
        if (packages == null) {
            arrayList = new ArrayList<>();
        } else {
            arrayList = new ArrayList<>(Arrays.asList(packages));
        }
        this.mPackages = arrayList;
        this.mCurrentPassword = curPassword;
        if (encryptPassword == null || "".equals(encryptPassword)) {
            this.mEncryptPassword = curPassword;
        } else {
            this.mEncryptPassword = encryptPassword;
        }
        this.mCompress = doCompress;
        this.mKeyValue = doKeyValue;
        this.mBackupEligibilityRules = backupEligibilityRules;
    }

    private void addPackagesToSet(TreeMap<String, PackageInfo> set, List<String> pkgNames) {
        for (String pkgName : pkgNames) {
            if (!set.containsKey(pkgName)) {
                try {
                    PackageInfo info = this.mUserBackupManagerService.getPackageManager().getPackageInfo(pkgName, 134217728);
                    set.put(pkgName, info);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(BackupManagerService.TAG, "Unknown package " + pkgName + ", skipping");
                }
            }
        }
    }

    private OutputStream emitAesBackupHeader(StringBuilder headerbuf, OutputStream ofstream) throws Exception {
        byte[] newUserSalt = this.mUserBackupManagerService.randomBytes(512);
        SecretKey userKey = PasswordUtils.buildPasswordKey(BackupPasswordManager.PBKDF_CURRENT, this.mEncryptPassword, newUserSalt, 10000);
        byte[] encryptionKey = new byte[32];
        this.mUserBackupManagerService.getRng().nextBytes(encryptionKey);
        byte[] checksumSalt = this.mUserBackupManagerService.randomBytes(512);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec encryptionKeySpec = new SecretKeySpec(encryptionKey, "AES");
        c.init(1, encryptionKeySpec);
        OutputStream finalOutput = new CipherOutputStream(ofstream, c);
        headerbuf.append(PasswordUtils.ENCRYPTION_ALGORITHM_NAME);
        headerbuf.append('\n');
        headerbuf.append(PasswordUtils.byteArrayToHex(newUserSalt));
        headerbuf.append('\n');
        headerbuf.append(PasswordUtils.byteArrayToHex(checksumSalt));
        headerbuf.append('\n');
        headerbuf.append(10000);
        headerbuf.append('\n');
        Cipher mkC = Cipher.getInstance("AES/CBC/PKCS5Padding");
        mkC.init(1, userKey);
        headerbuf.append(PasswordUtils.byteArrayToHex(mkC.getIV()));
        headerbuf.append('\n');
        byte[] IV = c.getIV();
        byte[] mk = encryptionKeySpec.getEncoded();
        byte[] checksum = PasswordUtils.makeKeyChecksum(BackupPasswordManager.PBKDF_CURRENT, encryptionKeySpec.getEncoded(), checksumSalt, 10000);
        ByteArrayOutputStream blob = new ByteArrayOutputStream(IV.length + mk.length + checksum.length + 3);
        DataOutputStream mkOut = new DataOutputStream(blob);
        mkOut.writeByte(IV.length);
        mkOut.write(IV);
        mkOut.writeByte(mk.length);
        mkOut.write(mk);
        mkOut.writeByte(checksum.length);
        mkOut.write(checksum);
        mkOut.flush();
        byte[] encryptedMk = mkC.doFinal(blob.toByteArray());
        headerbuf.append(PasswordUtils.byteArrayToHex(encryptedMk));
        headerbuf.append('\n');
        return finalOutput;
    }

    private void finalizeBackup(OutputStream out) {
        try {
            byte[] eof = new byte[1024];
            out.write(eof);
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, "Error attempting to finalize backup stream");
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:117:0x0301
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [459=16, 461=16, 464=16, 465=6, 466=6, 467=6, 469=6, 470=6, 471=6, 472=6, 473=6, 474=6, 475=6, 476=11, 477=5, 478=5, 480=5, 482=5] */
    @Override // java.lang.Runnable
    public void run() {
        /*
            r35 = this;
            r13 = r35
            boolean r0 = r13.mKeyValue
            if (r0 == 0) goto L9
            java.lang.String r0 = ", including key-value backups"
            goto Lb
        L9:
            java.lang.String r0 = ""
        Lb:
            r14 = r0
            java.lang.String r0 = "BackupManagerService"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "--- Performing adb backup"
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r14)
            java.lang.String r2 = " ---"
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Slog.i(r0, r1)
            java.util.TreeMap r0 = new java.util.TreeMap
            r0.<init>()
            r15 = r0
            com.android.server.backup.fullbackup.FullBackupObbConnection r0 = new com.android.server.backup.fullbackup.FullBackupObbConnection
            com.android.server.backup.UserBackupManagerService r1 = r13.mUserBackupManagerService
            r0.<init>(r1)
            r12 = r0
            r12.establish()
            r35.sendStartBackup()
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            android.content.pm.PackageManager r11 = r0.getPackageManager()
            boolean r0 = r13.mAllApps
            r10 = 1
            if (r0 == 0) goto L6f
            r0 = 134217728(0x8000000, float:3.85186E-34)
            java.util.List r0 = r11.getInstalledPackages(r0)
            r1 = 0
        L50:
            int r2 = r0.size()
            if (r1 >= r2) goto L6f
            java.lang.Object r2 = r0.get(r1)
            android.content.pm.PackageInfo r2 = (android.content.pm.PackageInfo) r2
            boolean r3 = r13.mIncludeSystem
            if (r3 != 0) goto L67
            android.content.pm.ApplicationInfo r3 = r2.applicationInfo
            int r3 = r3.flags
            r3 = r3 & r10
            if (r3 != 0) goto L6c
        L67:
            java.lang.String r3 = r2.packageName
            r15.put(r3, r2)
        L6c:
            int r1 = r1 + 1
            goto L50
        L6f:
            boolean r0 = r13.mDoWidgets
            r1 = 0
            if (r0 == 0) goto L7e
        L75:
            java.util.List r0 = com.android.server.AppWidgetBackupBridge.getWidgetParticipants(r1)
            if (r0 == 0) goto L7e
            r13.addPackagesToSet(r15, r0)
        L7e:
            java.util.ArrayList<java.lang.String> r0 = r13.mPackages
            if (r0 == 0) goto L85
            r13.addPackagesToSet(r15, r0)
        L85:
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r8 = r0
            java.util.Set r0 = r15.entrySet()
            java.util.Iterator r16 = r0.iterator()
        L93:
            boolean r0 = r16.hasNext()
            if (r0 == 0) goto L10d
            java.lang.Object r0 = r16.next()
            java.util.Map$Entry r0 = (java.util.Map.Entry) r0
            java.lang.Object r0 = r0.getValue()
            android.content.pm.PackageInfo r0 = (android.content.pm.PackageInfo) r0
            com.android.server.backup.utils.BackupEligibilityRules r2 = r13.mBackupEligibilityRules
            android.content.pm.ApplicationInfo r3 = r0.applicationInfo
            boolean r2 = r2.appIsEligibleForBackup(r3)
            if (r2 == 0) goto Le9
            com.android.server.backup.utils.BackupEligibilityRules r2 = r13.mBackupEligibilityRules
            android.content.pm.ApplicationInfo r3 = r0.applicationInfo
            boolean r2 = r2.appIsStopped(r3)
            if (r2 == 0) goto Lba
            goto Le9
        Lba:
            com.android.server.backup.utils.BackupEligibilityRules r2 = r13.mBackupEligibilityRules
            boolean r2 = r2.appIsKeyValueOnly(r0)
            if (r2 == 0) goto L10c
            r16.remove()
            java.lang.String r2 = "BackupManagerService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Package "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = r0.packageName
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = " is key-value."
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Slog.i(r2, r3)
            r8.add(r0)
            goto L10c
        Le9:
            r16.remove()
            java.lang.String r2 = "BackupManagerService"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Package "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = r0.packageName
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = " is not eligible for backup, removing."
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Slog.i(r2, r3)
        L10c:
            goto L93
        L10d:
            java.util.ArrayList r0 = new java.util.ArrayList
            java.util.Collection r2 = r15.values()
            r0.<init>(r2)
            r9 = r0
            java.io.FileOutputStream r0 = new java.io.FileOutputStream
            android.os.ParcelFileDescriptor r2 = r13.mOutputFile
            java.io.FileDescriptor r2 = r2.getFileDescriptor()
            r0.<init>(r2)
            r7 = r0
            r2 = 0
            r3 = 0
            java.lang.String r0 = r13.mEncryptPassword     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            if (r0 == 0) goto L15f
            int r0 = r0.length()     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L141 android.os.RemoteException -> L150
            if (r0 <= 0) goto L15f
            r0 = r10
            goto L160
        L131:
            r0 = move-exception
            r1 = r0
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L710
        L141:
            r0 = move-exception
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L662
        L150:
            r0 = move-exception
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L6b3
        L15f:
            r0 = r1
        L160:
            r17 = r0
            r4 = r7
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            java.lang.String r5 = r13.mCurrentPassword     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            boolean r0 = r0.backupPasswordMatches(r5)     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            if (r0 != 0) goto L1c7
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r1 = "Backup password mismatch; aborting"
            android.util.Slog.w(r0, r1)     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L141 android.os.RemoteException -> L150
            if (r2 == 0) goto L17c
            r2.flush()     // Catch: java.io.IOException -> L182
            r2.close()     // Catch: java.io.IOException -> L182
        L17c:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L182
            r0.close()     // Catch: java.io.IOException -> L182
            goto L19f
        L182:
            r0 = move-exception
            java.lang.String r1 = "BackupManagerService"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "IO error closing adb backup file: "
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r6 = r0.getMessage()
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r5 = r5.toString()
            android.util.Slog.e(r1, r5)
        L19f:
            java.util.concurrent.atomic.AtomicBoolean r1 = r13.mLatch
            monitor-enter(r1)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L1c4
            r0.set(r10)     // Catch: java.lang.Throwable -> L1c4
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L1c4
            r0.notifyAll()     // Catch: java.lang.Throwable -> L1c4
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L1c4
            r35.sendEndBackup()
            r12.tearDown()
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r1 = "Full backup pass complete."
            android.util.Slog.d(r0, r1)
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            com.android.server.backup.UserBackupManagerService$BackupWakeLock r0 = r0.getWakelock()
            r0.release()
            return
        L1c4:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L1c4
            throw r0
        L1c7:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            r5 = 1024(0x400, float:1.435E-42)
            r0.<init>(r5)     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            r6 = r0
            java.lang.String r0 = "ANDROID BACKUP\n"
            r6.append(r0)     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            r0 = 5
            r6.append(r0)     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            boolean r0 = r13.mCompress     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            if (r0 == 0) goto L1df
            java.lang.String r0 = "\n1\n"
            goto L1e1
        L1df:
            java.lang.String r0 = "\n0\n"
        L1e1:
            r6.append(r0)     // Catch: java.lang.Throwable -> L645 java.lang.Exception -> L655 android.os.RemoteException -> L6a6
            if (r17 == 0) goto L1fd
            java.io.OutputStream r0 = r13.emitAesBackupHeader(r6, r4)     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L1ec
            r4 = r0
            goto L203
        L1ec:
            r0 = move-exception
            r30 = r6
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L5e5
        L1fd:
            java.lang.String r0 = "none\n"
            r6.append(r0)     // Catch: java.lang.Exception -> L5d6 java.lang.Throwable -> L645
        L203:
            java.lang.String r0 = r6.toString()     // Catch: java.lang.Exception -> L5d6 java.lang.Throwable -> L645
            java.lang.String r5 = "UTF-8"
            byte[] r0 = r0.getBytes(r5)     // Catch: java.lang.Exception -> L5d6 java.lang.Throwable -> L645
            r7.write(r0)     // Catch: java.lang.Exception -> L5d6 java.lang.Throwable -> L645
            boolean r5 = r13.mCompress     // Catch: java.lang.Exception -> L5d6 java.lang.Throwable -> L645
            if (r5 == 0) goto L225
            java.util.zip.Deflater r5 = new java.util.zip.Deflater     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L1ec
            r1 = 9
            r5.<init>(r1)     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L1ec
            r1 = r5
            java.util.zip.DeflaterOutputStream r5 = new java.util.zip.DeflaterOutputStream     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L1ec
            r5.<init>(r4, r1, r10)     // Catch: java.lang.Throwable -> L131 java.lang.Exception -> L1ec
            r4 = r5
            r19 = r4
            goto L227
        L225:
            r19 = r4
        L227:
            r5 = r19
            boolean r0 = r13.mIncludeShared     // Catch: java.lang.Throwable -> L5a2 java.lang.Exception -> L5b4 android.os.RemoteException -> L5c5
            if (r0 == 0) goto L279
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L240 java.lang.Exception -> L251 android.os.RemoteException -> L261 android.content.pm.PackageManager.NameNotFoundException -> L271
            android.content.pm.PackageManager r0 = r0.getPackageManager()     // Catch: java.lang.Throwable -> L240 java.lang.Exception -> L251 android.os.RemoteException -> L261 android.content.pm.PackageManager.NameNotFoundException -> L271
            java.lang.String r1 = "com.android.sharedstoragebackup"
            r2 = 0
            android.content.pm.PackageInfo r0 = r0.getPackageInfo(r1, r2)     // Catch: java.lang.Throwable -> L240 java.lang.Exception -> L251 android.os.RemoteException -> L261 android.content.pm.PackageManager.NameNotFoundException -> L271
            r3 = r0
            r9.add(r3)     // Catch: java.lang.Throwable -> L240 java.lang.Exception -> L251 android.os.RemoteException -> L261 android.content.pm.PackageManager.NameNotFoundException -> L271
            goto L279
        L240:
            r0 = move-exception
            r1 = r0
            r2 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L710
        L251:
            r0 = move-exception
            r2 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L662
        L261:
            r0 = move-exception
            r2 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            goto L6b3
        L271:
            r0 = move-exception
            java.lang.String r1 = "BackupManagerService"
            java.lang.String r2 = "Unable to find shared-storage backup handler"
            android.util.Slog.e(r1, r2)     // Catch: java.lang.Throwable -> L240 java.lang.Exception -> L251 android.os.RemoteException -> L261
        L279:
            int r0 = r9.size()     // Catch: java.lang.Throwable -> L5a2 java.lang.Exception -> L5b4 android.os.RemoteException -> L5c5
            r1 = 0
            r4 = r1
        L27f:
            if (r4 >= r0) goto L4bd
            java.lang.Object r1 = r9.get(r4)     // Catch: java.lang.Throwable -> L489 java.lang.Exception -> L49b android.os.RemoteException -> L4ac
            android.content.pm.PackageInfo r1 = (android.content.pm.PackageInfo) r1     // Catch: java.lang.Throwable -> L489 java.lang.Exception -> L49b android.os.RemoteException -> L4ac
            r3 = r1
            java.lang.String r1 = "BackupManagerService"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L452 java.lang.Exception -> L465 android.os.RemoteException -> L477
            r2.<init>()     // Catch: java.lang.Throwable -> L452 java.lang.Exception -> L465 android.os.RemoteException -> L477
            java.lang.String r10 = "--- Performing full backup for package "
            java.lang.StringBuilder r2 = r2.append(r10)     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.String r10 = r3.packageName     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.StringBuilder r2 = r2.append(r10)     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.String r10 = " ---"
            java.lang.StringBuilder r2 = r2.append(r10)     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            android.util.Slog.i(r1, r2)     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.String r1 = r3.packageName     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            java.lang.String r2 = "com.android.sharedstoragebackup"
            boolean r1 = r1.equals(r2)     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            r20 = r1
            com.android.server.backup.fullbackup.FullBackupEngine r21 = new com.android.server.backup.fullbackup.FullBackupEngine     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            com.android.server.backup.UserBackupManagerService r2 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            r10 = 0
            boolean r1 = r13.mIncludeApks     // Catch: java.lang.Throwable -> L41e java.lang.Exception -> L430 android.os.RemoteException -> L441
            r22 = 9223372036854775807(0x7fffffffffffffff, double:NaN)
            r24 = r8
            int r8 = r13.mCurrentOpToken     // Catch: java.lang.Throwable -> L3e7 java.lang.Exception -> L3fa android.os.RemoteException -> L40c
            r25 = 0
            r26 = r11
            com.android.server.backup.utils.BackupEligibilityRules r11 = r13.mBackupEligibilityRules     // Catch: java.lang.Throwable -> L3b0 java.lang.Exception -> L3c3 android.os.RemoteException -> L3d5
            r27 = r1
            r1 = r21
            r28 = r3
            r3 = r5
            r29 = r4
            r4 = r10
            r10 = r5
            r5 = r28
            r30 = r6
            r6 = r27
            r27 = r7
            r7 = r35
            r32 = r9
            r31 = r24
            r24 = r8
            r8 = r22
            r33 = r10
            r10 = r24
            r22 = r11
            r18 = r26
            r11 = r25
            r34 = r14
            r14 = r12
            r12 = r22
            r1.<init>(r2, r3, r4, r5, r6, r7, r8, r10, r11, r12)     // Catch: java.lang.Throwable -> L391 java.lang.Exception -> L39c android.os.RemoteException -> L3a6
            r1 = r21
            if (r20 == 0) goto L31a
            java.lang.String r2 = "Shared storage"
            r3 = r2
            r2 = r28
            goto L31e
        L301:
            r0 = move-exception
            r1 = r0
            r3 = r28
            r2 = r33
            r5 = 1
            goto L710
        L30a:
            r0 = move-exception
            r3 = r28
            r2 = r33
            r5 = 1
            goto L662
        L312:
            r0 = move-exception
            r3 = r28
            r2 = r33
            r5 = 1
            goto L6b3
        L31a:
            r2 = r28
            java.lang.String r3 = r2.packageName     // Catch: java.lang.Throwable -> L378 java.lang.Exception -> L381 android.os.RemoteException -> L389
        L31e:
            r13.sendOnBackupPackage(r3)     // Catch: java.lang.Throwable -> L378 java.lang.Exception -> L381 android.os.RemoteException -> L389
            r13.mCurrentTarget = r2     // Catch: java.lang.Throwable -> L378 java.lang.Exception -> L381 android.os.RemoteException -> L389
            r1.backupOnePackage()     // Catch: java.lang.Throwable -> L378 java.lang.Exception -> L381 android.os.RemoteException -> L389
            boolean r3 = r13.mIncludeObbs     // Catch: java.lang.Throwable -> L378 java.lang.Exception -> L381 android.os.RemoteException -> L389
            if (r3 == 0) goto L361
            if (r20 != 0) goto L361
            r4 = r33
            boolean r3 = r14.backupObbs(r2, r4)     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            if (r3 == 0) goto L335
            goto L363
        L335:
            java.lang.RuntimeException r5 = new java.lang.RuntimeException     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            r6.<init>()     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            java.lang.String r7 = "Failure writing OBB stack for "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            java.lang.StringBuilder r6 = r6.append(r2)     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            r5.<init>(r6)     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
            throw r5     // Catch: java.lang.Throwable -> L34e java.lang.Exception -> L355 android.os.RemoteException -> L35b
        L34e:
            r0 = move-exception
            r1 = r0
            r3 = r2
            r2 = r4
            r5 = 1
            goto L710
        L355:
            r0 = move-exception
            r3 = r2
            r2 = r4
            r5 = 1
            goto L662
        L35b:
            r0 = move-exception
            r3 = r2
            r2 = r4
            r5 = 1
            goto L6b3
        L361:
            r4 = r33
        L363:
            int r1 = r29 + 1
            r3 = r2
            r5 = r4
            r12 = r14
            r11 = r18
            r7 = r27
            r6 = r30
            r8 = r31
            r9 = r32
            r14 = r34
            r10 = 1
            r4 = r1
            goto L27f
        L378:
            r0 = move-exception
            r4 = r33
            r1 = r0
            r3 = r2
            r2 = r4
            r5 = 1
            goto L710
        L381:
            r0 = move-exception
            r4 = r33
            r3 = r2
            r2 = r4
            r5 = 1
            goto L662
        L389:
            r0 = move-exception
            r4 = r33
            r3 = r2
            r2 = r4
            r5 = 1
            goto L6b3
        L391:
            r0 = move-exception
            r2 = r28
            r4 = r33
            r1 = r0
            r3 = r2
            r2 = r4
            r5 = 1
            goto L710
        L39c:
            r0 = move-exception
            r2 = r28
            r4 = r33
            r3 = r2
            r2 = r4
            r5 = 1
            goto L662
        L3a6:
            r0 = move-exception
            r2 = r28
            r4 = r33
            r3 = r2
            r2 = r4
            r5 = 1
            goto L6b3
        L3b0:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r34 = r14
            r31 = r24
            r18 = r26
            r14 = r12
            r1 = r0
            r2 = r4
            r5 = 1
            goto L710
        L3c3:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r34 = r14
            r31 = r24
            r18 = r26
            r14 = r12
            r2 = r4
            r5 = 1
            goto L662
        L3d5:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r34 = r14
            r31 = r24
            r18 = r26
            r14 = r12
            r2 = r4
            r5 = 1
            goto L6b3
        L3e7:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r18 = r11
            r34 = r14
            r31 = r24
            r14 = r12
            r1 = r0
            r2 = r4
            r5 = 1
            goto L710
        L3fa:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r18 = r11
            r34 = r14
            r31 = r24
            r14 = r12
            r2 = r4
            r5 = 1
            goto L662
        L40c:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r32 = r9
            r18 = r11
            r34 = r14
            r31 = r24
            r14 = r12
            r2 = r4
            r5 = 1
            goto L6b3
        L41e:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r1 = r0
            r2 = r4
            r5 = 1
            goto L463
        L430:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = 1
            goto L475
        L441:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = 1
            goto L487
        L452:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r1 = r0
            r2 = r4
            r5 = r10
        L463:
            goto L710
        L465:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = r10
        L475:
            goto L662
        L477:
            r0 = move-exception
            r2 = r3
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = r10
        L487:
            goto L6b3
        L489:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r1 = r0
            r2 = r4
            r5 = r10
            goto L5b2
        L49b:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = r10
            goto L5c3
        L4ac:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
            r5 = r10
            goto L5d4
        L4bd:
            r29 = r4
            r4 = r5
            r30 = r6
            r27 = r7
            r31 = r8
            r32 = r9
            r18 = r11
            r34 = r14
            r14 = r12
            boolean r1 = r13.mKeyValue     // Catch: java.lang.Throwable -> L592 java.lang.Exception -> L598 android.os.RemoteException -> L59d
            if (r1 == 0) goto L539
            java.util.Iterator r1 = r31.iterator()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
        L4d5:
            boolean r2 = r1.hasNext()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            if (r2 == 0) goto L539
            java.lang.Object r2 = r1.next()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            android.content.pm.PackageInfo r2 = (android.content.pm.PackageInfo) r2     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r5 = "BackupManagerService"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            r6.<init>()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r7 = "--- Performing key-value backup for package "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r7 = r2.packageName     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r7 = " ---"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            android.util.Slog.i(r5, r6)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            com.android.server.backup.KeyValueAdbBackupEngine r5 = new com.android.server.backup.KeyValueAdbBackupEngine     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            com.android.server.backup.UserBackupManagerService r6 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            android.content.pm.PackageManager r24 = r6.getPackageManager()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            com.android.server.backup.UserBackupManagerService r7 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.io.File r25 = r7.getBaseStateDir()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            com.android.server.backup.UserBackupManagerService r7 = r13.mUserBackupManagerService     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.io.File r26 = r7.getDataDir()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            r20 = r5
            r21 = r4
            r22 = r2
            r23 = r6
            r20.<init>(r21, r22, r23, r24, r25, r26)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            java.lang.String r6 = r2.packageName     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            r13.sendOnBackupPackage(r6)     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            r5.backupOnePackage()     // Catch: java.lang.Throwable -> L529 java.lang.Exception -> L52f android.os.RemoteException -> L534
            goto L4d5
        L529:
            r0 = move-exception
            r1 = r0
            r2 = r4
            r5 = 1
            goto L710
        L52f:
            r0 = move-exception
            r2 = r4
            r5 = 1
            goto L662
        L534:
            r0 = move-exception
            r2 = r4
            r5 = 1
            goto L6b3
        L539:
            r13.finalizeBackup(r4)     // Catch: java.lang.Throwable -> L592 java.lang.Exception -> L598 android.os.RemoteException -> L59d
            if (r4 == 0) goto L544
            r4.flush()     // Catch: java.io.IOException -> L54a
            r4.close()     // Catch: java.io.IOException -> L54a
        L544:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L54a
            r0.close()     // Catch: java.io.IOException -> L54a
            goto L567
        L54a:
            r0 = move-exception
            java.lang.String r1 = "BackupManagerService"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r5 = "IO error closing adb backup file: "
            java.lang.StringBuilder r2 = r2.append(r5)
            java.lang.String r5 = r0.getMessage()
            java.lang.StringBuilder r2 = r2.append(r5)
            java.lang.String r2 = r2.toString()
            android.util.Slog.e(r1, r2)
        L567:
            java.util.concurrent.atomic.AtomicBoolean r1 = r13.mLatch
            monitor-enter(r1)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L58f
            r5 = 1
            r0.set(r5)     // Catch: java.lang.Throwable -> L58f
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L58f
            r0.notifyAll()     // Catch: java.lang.Throwable -> L58f
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L58f
            r35.sendEndBackup()
            r14.tearDown()
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r1 = "Full backup pass complete."
            android.util.Slog.d(r0, r1)
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            com.android.server.backup.UserBackupManagerService$BackupWakeLock r0 = r0.getWakelock()
            r0.release()
            r5 = r4
            goto L70a
        L58f:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L58f
            throw r0
        L592:
            r0 = move-exception
            r5 = 1
            r1 = r0
            r2 = r4
            goto L710
        L598:
            r0 = move-exception
            r5 = 1
            r2 = r4
            goto L662
        L59d:
            r0 = move-exception
            r5 = 1
            r2 = r4
            goto L6b3
        L5a2:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            r1 = r0
            r2 = r4
        L5b2:
            goto L710
        L5b4:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
        L5c3:
            goto L662
        L5c5:
            r0 = move-exception
            r4 = r5
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            r2 = r4
        L5d4:
            goto L6b3
        L5d6:
            r0 = move-exception
            r30 = r6
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
        L5e5:
            r1 = r0
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r6 = "Unable to emit archive header"
            android.util.Slog.e(r0, r6, r1)     // Catch: java.lang.Exception -> L640 android.os.RemoteException -> L642 java.lang.Throwable -> L70e
            if (r2 == 0) goto L5f5
            r2.flush()     // Catch: java.io.IOException -> L5fb
            r2.close()     // Catch: java.io.IOException -> L5fb
        L5f5:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L5fb
            r0.close()     // Catch: java.io.IOException -> L5fb
            goto L618
        L5fb:
            r0 = move-exception
            java.lang.String r6 = "BackupManagerService"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "IO error closing adb backup file: "
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.String r8 = r0.getMessage()
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Slog.e(r6, r7)
        L618:
            java.util.concurrent.atomic.AtomicBoolean r6 = r13.mLatch
            monitor-enter(r6)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L63d
            r0.set(r5)     // Catch: java.lang.Throwable -> L63d
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L63d
            r0.notifyAll()     // Catch: java.lang.Throwable -> L63d
            monitor-exit(r6)     // Catch: java.lang.Throwable -> L63d
            r35.sendEndBackup()
            r14.tearDown()
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r5 = "Full backup pass complete."
            android.util.Slog.d(r0, r5)
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            com.android.server.backup.UserBackupManagerService$BackupWakeLock r0 = r0.getWakelock()
            r0.release()
            return
        L63d:
            r0 = move-exception
            monitor-exit(r6)     // Catch: java.lang.Throwable -> L63d
            throw r0
        L640:
            r0 = move-exception
            goto L662
        L642:
            r0 = move-exception
            goto L6b3
        L645:
            r0 = move-exception
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
            r1 = r0
            goto L710
        L655:
            r0 = move-exception
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
        L662:
            java.lang.String r1 = "BackupManagerService"
            java.lang.String r4 = "Internal exception during full backup"
            android.util.Slog.e(r1, r4, r0)     // Catch: java.lang.Throwable -> L70e
            if (r2 == 0) goto L671
            r2.flush()     // Catch: java.io.IOException -> L677
            r2.close()     // Catch: java.io.IOException -> L677
        L671:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L677
            r0.close()     // Catch: java.io.IOException -> L677
            goto L694
        L677:
            r0 = move-exception
            java.lang.String r1 = "BackupManagerService"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "IO error closing adb backup file: "
            java.lang.StringBuilder r4 = r4.append(r6)
            java.lang.String r6 = r0.getMessage()
            java.lang.StringBuilder r4 = r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Slog.e(r1, r4)
        L694:
            java.util.concurrent.atomic.AtomicBoolean r1 = r13.mLatch
            monitor-enter(r1)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L6a3
            r0.set(r5)     // Catch: java.lang.Throwable -> L6a3
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L6a3
            r0.notifyAll()     // Catch: java.lang.Throwable -> L6a3
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L6a3
            goto L6f3
        L6a3:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L6a3
            throw r0
        L6a6:
            r0 = move-exception
            r27 = r7
            r31 = r8
            r32 = r9
            r5 = r10
            r18 = r11
            r34 = r14
            r14 = r12
        L6b3:
            java.lang.String r1 = "BackupManagerService"
            java.lang.String r4 = "App died during full backup"
            android.util.Slog.e(r1, r4)     // Catch: java.lang.Throwable -> L70e
            if (r2 == 0) goto L6c2
            r2.flush()     // Catch: java.io.IOException -> L6c8
            r2.close()     // Catch: java.io.IOException -> L6c8
        L6c2:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L6c8
            r0.close()     // Catch: java.io.IOException -> L6c8
            goto L6e5
        L6c8:
            r0 = move-exception
            java.lang.String r1 = "BackupManagerService"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "IO error closing adb backup file: "
            java.lang.StringBuilder r4 = r4.append(r6)
            java.lang.String r6 = r0.getMessage()
            java.lang.StringBuilder r4 = r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Slog.e(r1, r4)
        L6e5:
            java.util.concurrent.atomic.AtomicBoolean r1 = r13.mLatch
            monitor-enter(r1)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L70b
            r0.set(r5)     // Catch: java.lang.Throwable -> L70b
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L70b
            r0.notifyAll()     // Catch: java.lang.Throwable -> L70b
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L70b
        L6f3:
            r35.sendEndBackup()
            r14.tearDown()
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r1 = "Full backup pass complete."
            android.util.Slog.d(r0, r1)
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            com.android.server.backup.UserBackupManagerService$BackupWakeLock r0 = r0.getWakelock()
            r0.release()
            r5 = r2
        L70a:
            return
        L70b:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L70b
            throw r0
        L70e:
            r0 = move-exception
            r1 = r0
        L710:
            if (r2 == 0) goto L718
            r2.flush()     // Catch: java.io.IOException -> L71e
            r2.close()     // Catch: java.io.IOException -> L71e
        L718:
            android.os.ParcelFileDescriptor r0 = r13.mOutputFile     // Catch: java.io.IOException -> L71e
            r0.close()     // Catch: java.io.IOException -> L71e
            goto L73b
        L71e:
            r0 = move-exception
            java.lang.String r4 = "BackupManagerService"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "IO error closing adb backup file: "
            java.lang.StringBuilder r6 = r6.append(r7)
            java.lang.String r7 = r0.getMessage()
            java.lang.StringBuilder r6 = r6.append(r7)
            java.lang.String r6 = r6.toString()
            android.util.Slog.e(r4, r6)
        L73b:
            java.util.concurrent.atomic.AtomicBoolean r4 = r13.mLatch
            monitor-enter(r4)
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L760
            r0.set(r5)     // Catch: java.lang.Throwable -> L760
            java.util.concurrent.atomic.AtomicBoolean r0 = r13.mLatch     // Catch: java.lang.Throwable -> L760
            r0.notifyAll()     // Catch: java.lang.Throwable -> L760
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L760
            r35.sendEndBackup()
            r14.tearDown()
            java.lang.String r0 = "BackupManagerService"
            java.lang.String r4 = "Full backup pass complete."
            android.util.Slog.d(r0, r4)
            com.android.server.backup.UserBackupManagerService r0 = r13.mUserBackupManagerService
            com.android.server.backup.UserBackupManagerService$BackupWakeLock r0 = r0.getWakelock()
            r0.release()
            throw r1
        L760:
            r0 = move-exception
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L760
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.fullbackup.PerformAdbBackupTask.run():void");
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void execute() {
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void operationComplete(long result) {
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void handleCancel(boolean cancelAll) {
        PackageInfo target = this.mCurrentTarget;
        Slog.w(BackupManagerService.TAG, "adb backup cancel of " + target);
        if (target != null) {
            this.mUserBackupManagerService.tearDownAgentAndKill(this.mCurrentTarget.applicationInfo);
        }
        this.mOperationStorage.removeOperation(this.mCurrentOpToken);
    }
}
