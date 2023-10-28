package com.android.server.locksettings;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.utils.WatchableImpl;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class LockSettingsStorage extends WatchableImpl {
    private static final String CHILD_PROFILE_LOCK_FILE = "gatekeeper.profile.key";
    private static final String COLUMN_KEY = "name";
    private static final String COLUMN_USERID = "user";
    private static final boolean DEBUG = false;
    private static final String LOCK_PASSWORD_FILE = "gatekeeper.password.key";
    private static final String LOCK_PATTERN_FILE = "gatekeeper.pattern.key";
    private static final String REBOOT_ESCROW_FILE = "reboot.escrow.key";
    private static final String REBOOT_ESCROW_SERVER_BLOB = "reboot.escrow.server.blob.key";
    private static final boolean SYNTHETIC_PASSWORD_BACKUP_SUPPORT = true;
    private static final String SYNTHETIC_PASSWORD_DIRECTORY = "spblob/";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "LockSettingsStorage";
    private final Context mContext;
    private final DatabaseHelper mOpenHelper;
    private PersistentDataBlockManagerInternal mPersistentDataBlockManagerInternal;
    private static final String COLUMN_VALUE = "value";
    private static final String[] COLUMNS_FOR_QUERY = {COLUMN_VALUE};
    private static final String[] COLUMNS_FOR_PREFETCH = {"name", COLUMN_VALUE};
    private static final Object DEFAULT = new Object();
    private static final String[] SETTINGS_TO_BACKUP = {"lock_screen_owner_info_enabled", "lock_screen_owner_info", "lock_pattern_visible_pattern", "lockscreen.power_button_instantly_locks"};
    private final Cache mCache = new Cache();
    private final Object mFileWriteLock = new Object();

    /* loaded from: classes.dex */
    public interface Callback {
        void initialize(SQLiteDatabase sQLiteDatabase);
    }

    /* loaded from: classes.dex */
    public static class CredentialHash {
        byte[] hash;
        int type;

        private CredentialHash(byte[] hash, int type) {
            if (type != -1) {
                if (hash == null) {
                    throw new IllegalArgumentException("Empty hash for CredentialHash");
                }
            } else if (hash != null) {
                throw new IllegalArgumentException("None type CredentialHash should not have hash");
            }
            this.hash = hash;
            this.type = type;
        }

        static CredentialHash create(byte[] hash, int type) {
            if (type == -1) {
                throw new IllegalArgumentException("Bad type for CredentialHash");
            }
            return new CredentialHash(hash, type);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static CredentialHash createEmptyHash() {
            return new CredentialHash(null, -1);
        }
    }

    public LockSettingsStorage(Context context) {
        this.mContext = context;
        this.mOpenHelper = new DatabaseHelper(context);
    }

    public void setDatabaseOnCreateCallback(Callback callback) {
        this.mOpenHelper.setCallback(callback);
    }

    public void writeKeyValue(String key, String value, int userId) {
        writeKeyValue(this.mOpenHelper.getWritableDatabase(), key, value, userId);
    }

    public void writeKeyValue(SQLiteDatabase db, String key, String value, int userId) {
        ContentValues cv = new ContentValues();
        cv.put("name", key);
        cv.put(COLUMN_USERID, Integer.valueOf(userId));
        cv.put(COLUMN_VALUE, value);
        db.beginTransaction();
        try {
            db.delete(TABLE, "name=? AND user=?", new String[]{key, Integer.toString(userId)});
            db.insert(TABLE, null, cv);
            db.setTransactionSuccessful();
            this.mCache.putKeyValue(key, value, userId);
            db.endTransaction();
            dispatchChange(this);
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    public String readKeyValue(String key, String defaultValue, int userId) {
        synchronized (this.mCache) {
            if (this.mCache.hasKeyValue(key, userId)) {
                return this.mCache.peekKeyValue(key, defaultValue, userId);
            }
            int version = this.mCache.getVersion();
            Object result = DEFAULT;
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            Cursor cursor = db.query(TABLE, COLUMNS_FOR_QUERY, "user=? AND name=?", new String[]{Integer.toString(userId), key}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = cursor.getString(0);
                }
                cursor.close();
            }
            this.mCache.putKeyValueIfUnchanged(key, result, userId, version);
            return result == DEFAULT ? defaultValue : (String) result;
        }
    }

    public void removeKey(String key, int userId) {
        removeKey(this.mOpenHelper.getWritableDatabase(), key, userId);
    }

    private void removeKey(SQLiteDatabase db, String key, int userId) {
        ContentValues cv = new ContentValues();
        cv.put("name", key);
        cv.put(COLUMN_USERID, Integer.valueOf(userId));
        db.beginTransaction();
        try {
            db.delete(TABLE, "name=? AND user=?", new String[]{key, Integer.toString(userId)});
            db.setTransactionSuccessful();
            this.mCache.removeKey(key, userId);
            db.endTransaction();
            dispatchChange(this);
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    public void prefetchUser(int userId) {
        synchronized (this.mCache) {
            if (this.mCache.isFetched(userId)) {
                return;
            }
            this.mCache.setFetched(userId);
            int version = this.mCache.getVersion();
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            Cursor cursor = db.query(TABLE, COLUMNS_FOR_PREFETCH, "user=?", new String[]{Integer.toString(userId)}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    String value = cursor.getString(1);
                    this.mCache.putKeyValueIfUnchanged(key, value, userId, version);
                }
                cursor.close();
            }
            readCredentialHash(userId);
        }
    }

    private CredentialHash readPasswordHashIfExists(int userId) {
        byte[] stored = readFile(getLockPasswordFilename(userId));
        if (ArrayUtils.isEmpty(stored)) {
            return null;
        }
        return new CredentialHash(stored, 2);
    }

    private CredentialHash readPatternHashIfExists(int userId) {
        byte[] stored = readFile(getLockPatternFilename(userId));
        if (ArrayUtils.isEmpty(stored)) {
            return null;
        }
        return new CredentialHash(stored, 1);
    }

    public CredentialHash readCredentialHash(int userId) {
        CredentialHash passwordHash = readPasswordHashIfExists(userId);
        if (passwordHash != null) {
            return passwordHash;
        }
        CredentialHash patternHash = readPatternHashIfExists(userId);
        if (patternHash != null) {
            return patternHash;
        }
        return CredentialHash.createEmptyHash();
    }

    public void removeChildProfileLock(int userId) {
        try {
            deleteFile(getChildProfileLockFile(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeChildProfileLock(int userId, byte[] lock) {
        writeFile(getChildProfileLockFile(userId), lock);
    }

    public byte[] readChildProfileLock(int userId) {
        return readFile(getChildProfileLockFile(userId));
    }

    public boolean hasChildProfileLock(int userId) {
        return hasFile(getChildProfileLockFile(userId));
    }

    public void writeRebootEscrow(int userId, byte[] rebootEscrow) {
        writeFile(getRebootEscrowFile(userId), rebootEscrow);
    }

    public byte[] readRebootEscrow(int userId) {
        return readFile(getRebootEscrowFile(userId));
    }

    public boolean hasRebootEscrow(int userId) {
        return hasFile(getRebootEscrowFile(userId));
    }

    public void removeRebootEscrow(int userId) {
        deleteFile(getRebootEscrowFile(userId));
    }

    public void writeRebootEscrowServerBlob(byte[] serverBlob) {
        writeFile(getRebootEscrowServerBlob(), serverBlob);
    }

    public byte[] readRebootEscrowServerBlob() {
        return readFile(getRebootEscrowServerBlob());
    }

    public boolean hasRebootEscrowServerBlob() {
        return hasFile(getRebootEscrowServerBlob());
    }

    public void removeRebootEscrowServerBlob() {
        deleteFile(getRebootEscrowServerBlob());
    }

    public boolean hasPassword(int userId) {
        return hasFile(getLockPasswordFilename(userId));
    }

    public boolean hasPattern(int userId) {
        return hasFile(getLockPatternFilename(userId));
    }

    private boolean hasFile(String name) {
        byte[] contents = readFile(name);
        return contents != null && contents.length > 0;
    }

    private byte[] readFile(String name) {
        synchronized (this.mCache) {
            if (this.mCache.hasFile(name)) {
                return this.mCache.peekFile(name);
            }
            int version = this.mCache.getVersion();
            byte[] stored = null;
            try {
                RandomAccessFile raf = new RandomAccessFile(name, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                stored = new byte[(int) raf.length()];
                raf.readFully(stored, 0, stored.length);
                raf.close();
                raf.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
                Slog.e(TAG, "Cannot read file " + e2);
            }
            this.mCache.putFileIfUnchanged(name, stored, version);
            return stored;
        }
    }

    private void fsyncDirectory(File directory) {
        try {
            FileChannel file = FileChannel.open(directory.toPath(), StandardOpenOption.READ);
            file.force(true);
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error syncing directory: " + directory, e);
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(9:3|(2:4|5)|(6:10|11|12|13|14|15)|24|11|12|13|14|15) */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x004f, code lost:
        r2 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0050, code lost:
        r3 = com.android.server.locksettings.LockSettingsStorage.TAG;
        r4 = "Error closing file " + r2;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void writeFile(String name, byte[] hash) {
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(name, "rws");
            } catch (IOException e) {
                Slog.e(TAG, "Error writing to file " + e);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e2) {
                        String str = TAG;
                        String str2 = "Error closing file " + e2;
                        Slog.e(str, str2);
                        this.mCache.putFile(name, hash);
                        dispatchChange(this);
                    }
                }
            }
            if (hash != null && hash.length != 0) {
                raf.write(hash, 0, hash.length);
                raf.close();
                fsyncDirectory(new File(name).getAbsoluteFile().getParentFile());
                raf.close();
                this.mCache.putFile(name, hash);
                dispatchChange(this);
            }
            Slog.e(TAG, "write none to file " + name);
            raf.setLength(0L);
            raf.close();
            fsyncDirectory(new File(name).getAbsoluteFile().getParentFile());
            raf.close();
            this.mCache.putFile(name, hash);
            dispatchChange(this);
        }
    }

    private void deleteFile(String name) {
        synchronized (this.mFileWriteLock) {
            File file = new File(name);
            if (file.exists()) {
                file.delete();
                this.mCache.putFile(name, null);
            }
            dispatchChange(this);
        }
    }

    public void writeCredentialHash(CredentialHash hash, int userId) {
        byte[] patternHash = null;
        byte[] passwordHash = null;
        if (hash.type == 2 || hash.type == 4 || hash.type == 3) {
            passwordHash = hash.hash;
        } else {
            if (hash.type == 1) {
                patternHash = hash.hash;
            } else {
                Preconditions.checkArgument(hash.type == -1, "Unknown credential type");
            }
        }
        writeFile(getLockPasswordFilename(userId), passwordHash);
        writeFile(getLockPatternFilename(userId), patternHash);
    }

    String getLockPatternFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PATTERN_FILE);
    }

    String getLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PASSWORD_FILE);
    }

    String getChildProfileLockFile(int userId) {
        return getLockCredentialFilePathForUser(userId, CHILD_PROFILE_LOCK_FILE);
    }

    String getRebootEscrowFile(int userId) {
        return getLockCredentialFilePathForUser(userId, REBOOT_ESCROW_FILE);
    }

    String getRebootEscrowServerBlob() {
        return getLockCredentialFilePathForUser(0, REBOOT_ESCROW_SERVER_BLOB);
    }

    private String getLockCredentialFilePathForUser(int userId, String basename) {
        String dataSystemDirectory = Environment.getDataDirectory().getAbsolutePath() + SYSTEM_DIRECTORY;
        if (userId == 0) {
            return dataSystemDirectory + basename;
        }
        return new File(Environment.getUserSystemDirectory(userId), basename).getAbsolutePath();
    }

    public void writeSyntheticPasswordState(int userId, long handle, String name, byte[] data) {
        Slog.d(TAG, "writeSyntheticPasswordState file= " + getSynthenticPasswordStateFilePathForUser(userId, handle, name));
        ensureSyntheticPasswordDirectoryForUser(userId);
        writeFile(getSynthenticPasswordStateFilePathForUser(userId, handle, name), data);
        writeFile(getSynthenticPasswordBackUpStateFilePathForUser(userId, handle, name), data);
    }

    public byte[] readSyntheticPasswordState(int userId, long handle, String name) {
        String fileName = getSynthenticPasswordStateFilePathForUser(userId, handle, name);
        String backupFileName = getSynthenticPasswordBackUpStateFilePathForUser(userId, handle, name);
        if (hasFile(fileName) && !hasFile(backupFileName)) {
            Slog.i(TAG, "no " + backupFileName + " backup file,copy file");
            ensureSyntheticPasswordBackUpDirectoryForUser(userId);
            copyFile(fileName, backupFileName);
        } else if (!hasFile(fileName) && hasFile(backupFileName)) {
            Slog.i(TAG, "no " + fileName + " file,use backup file");
            copyFile(backupFileName, fileName);
        }
        return readFile(getSynthenticPasswordStateFilePathForUser(userId, handle, name));
    }

    public void deleteSyntheticPasswordState(int userId, long handle, String name) {
        deleteSyntheticPasswordState(getSynthenticPasswordStateFilePathForUser(userId, handle, name), userId, handle, name);
        deleteSyntheticPasswordState(getSynthenticPasswordBackUpStateFilePathForUser(userId, handle, name), userId, handle, name);
    }

    public void copySyntheticPasswordState(int userId, long handle, String name) {
        String fileName = getSynthenticPasswordStateFilePathForUser(userId, handle, name);
        String backupFileName = getSynthenticPasswordBackUpStateFilePathForUser(userId, handle, name);
        if (hasFile(backupFileName)) {
            Slog.i(TAG, "file: " + fileName + " is broken,use backup file");
            copyFile(backupFileName, fileName);
        }
    }

    private File getSyntheticPasswordBackUpDirectoryForUser(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), SYNTHETIC_PASSWORD_DIRECTORY);
    }

    private String getSynthenticPasswordBackUpStateFilePathForUser(int userId, long handle, String name) {
        File baseDir = getSyntheticPasswordBackUpDirectoryForUser(userId);
        String baseName = TextUtils.formatSimple("%016x.%s", new Object[]{Long.valueOf(handle), name});
        return new File(baseDir, baseName).getAbsolutePath();
    }

    private void ensureSyntheticPasswordBackUpDirectoryForUser(int userId) {
        File baseDir = getSyntheticPasswordBackUpDirectoryForUser(userId);
        if (!baseDir.exists()) {
            Slog.d(TAG, "ensureSyntheticPasswordBackUpDirectoryForUser  mkdir spblob");
            baseDir.mkdirs();
        }
    }

    private void deleteSyntheticPasswordState(String path, int userId, long handle, String name) {
        File file = new File(path);
        Slog.d(TAG, "deleteSyntheticPasswordState file= " + file);
        try {
            if (file.exists()) {
                try {
                    RandomAccessFile raf = new RandomAccessFile(path, "rws");
                    try {
                        int fileSize = (int) raf.length();
                        raf.write(new byte[fileSize]);
                        raf.close();
                    } catch (Throwable th) {
                        try {
                            raf.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to zeroize " + path, e);
                }
                this.mCache.putFile(path, null);
            }
        } finally {
            file.delete();
            dispatchChange(this);
        }
    }

    private void copyFile(String srcFile, String destFile) {
        byte[] in = readFile(srcFile);
        writeFile(destFile, in);
    }

    public Map<Integer, List<Long>> listSyntheticPasswordHandlesForAllUsers(String stateName) {
        Map<Integer, List<Long>> result = new ArrayMap<>();
        UserManager um = UserManager.get(this.mContext);
        for (UserInfo user : um.getUsers()) {
            result.put(Integer.valueOf(user.id), listSyntheticPasswordHandlesForUser(stateName, user.id));
        }
        return result;
    }

    public List<Long> listSyntheticPasswordHandlesForUser(String stateName, int userId) {
        File baseDir = getSyntheticPasswordDirectoryForUser(userId);
        List<Long> result = new ArrayList<>();
        File[] files = baseDir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            String[] parts = file.getName().split("\\.");
            if (parts.length == 2 && parts[1].equals(stateName)) {
                try {
                    result.add(Long.valueOf(Long.parseUnsignedLong(parts[0], 16)));
                } catch (NumberFormatException e) {
                    Slog.e(TAG, "Failed to parse handle " + parts[0]);
                }
            }
        }
        return result;
    }

    protected File getSyntheticPasswordDirectoryForUser(int userId) {
        return new File(Environment.getDataSystemDeDirectory(userId), SYNTHETIC_PASSWORD_DIRECTORY);
    }

    private void ensureSyntheticPasswordDirectoryForUser(int userId) {
        File baseDir = getSyntheticPasswordDirectoryForUser(userId);
        if (!baseDir.exists()) {
            baseDir.mkdir();
        }
        ensureSyntheticPasswordBackUpDirectoryForUser(userId);
    }

    protected String getSynthenticPasswordStateFilePathForUser(int userId, long handle, String name) {
        File baseDir = getSyntheticPasswordDirectoryForUser(userId);
        String baseName = TextUtils.formatSimple("%016x.%s", new Object[]{Long.valueOf(handle), name});
        return new File(baseDir, baseName).getAbsolutePath();
    }

    public void removeUser(int userId) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        UserManager um = (UserManager) this.mContext.getSystemService(COLUMN_USERID);
        UserInfo parentInfo = um.getProfileParent(userId);
        if (parentInfo == null) {
            synchronized (this.mFileWriteLock) {
                deleteFilesAndRemoveCache(getLockPasswordFilename(userId), getLockPatternFilename(userId), getRebootEscrowFile(userId));
            }
        } else {
            removeChildProfileLock(userId);
        }
        File spStateDir = getSyntheticPasswordDirectoryForUser(userId);
        try {
            db.beginTransaction();
            db.delete(TABLE, "user='" + userId + "'", null);
            db.setTransactionSuccessful();
            this.mCache.removeUser(userId);
            this.mCache.purgePath(spStateDir.getAbsolutePath());
            db.endTransaction();
            dispatchChange(this);
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    private void deleteFilesAndRemoveCache(String... names) {
        for (String name : names) {
            File file = new File(name);
            if (file.exists()) {
                file.delete();
                this.mCache.putFile(name, null);
                dispatchChange(this);
            }
        }
    }

    public void setBoolean(String key, boolean value, int userId) {
        setString(key, value ? "1" : "0", userId);
    }

    public void setLong(String key, long value, int userId) {
        setString(key, Long.toString(value), userId);
    }

    public void setInt(String key, int value, int userId) {
        setString(key, Integer.toString(value), userId);
    }

    public void setString(String key, String value, int userId) {
        Preconditions.checkArgument(userId != -9999, "cannot store lock settings for FRP user");
        writeKeyValue(key, value, userId);
        if (ArrayUtils.contains(SETTINGS_TO_BACKUP, key)) {
            BackupManager.dataChanged(UserBackupManagerService.SETTINGS_PACKAGE);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) {
        String value = getString(key, null, userId);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value.equals("1") || value.equals("true");
    }

    public long getLong(String key, long defaultValue, int userId) {
        String value = getString(key, null, userId);
        return TextUtils.isEmpty(value) ? defaultValue : Long.parseLong(value);
    }

    public int getInt(String key, int defaultValue, int userId) {
        String value = getString(key, null, userId);
        return TextUtils.isEmpty(value) ? defaultValue : Integer.parseInt(value);
    }

    public String getString(String key, String defaultValue, int userId) {
        if (userId == -9999) {
            return null;
        }
        if ("legacy_lock_pattern_enabled".equals(key)) {
            key = "lock_pattern_autolock";
        }
        return readKeyValue(key, defaultValue, userId);
    }

    void closeDatabase() {
        this.mOpenHelper.close();
    }

    void clearCache() {
        this.mCache.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentDataBlockManagerInternal getPersistentDataBlockManager() {
        if (this.mPersistentDataBlockManagerInternal == null) {
            this.mPersistentDataBlockManagerInternal = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        }
        return this.mPersistentDataBlockManagerInternal;
    }

    public void writePersistentDataBlock(int persistentType, int userId, int qualityForUi, byte[] payload) {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlockManager();
        if (persistentDataBlock == null) {
            return;
        }
        persistentDataBlock.setFrpCredentialHandle(PersistentData.toBytes(persistentType, userId, qualityForUi, payload));
        dispatchChange(this);
    }

    public PersistentData readPersistentDataBlock() {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlockManager();
        if (persistentDataBlock == null) {
            return PersistentData.NONE;
        }
        try {
            return PersistentData.fromBytes(persistentDataBlock.getFrpCredentialHandle());
        } catch (IllegalStateException e) {
            Slog.e(TAG, "Error reading persistent data block", e);
            return PersistentData.NONE;
        }
    }

    /* loaded from: classes.dex */
    public static class PersistentData {
        public static final PersistentData NONE = new PersistentData(0, -10000, 0, null);
        public static final int TYPE_NONE = 0;
        public static final int TYPE_SP = 1;
        public static final int TYPE_SP_WEAVER = 2;
        static final byte VERSION_1 = 1;
        static final int VERSION_1_HEADER_SIZE = 10;
        final byte[] payload;
        final int qualityForUi;
        final int type;
        final int userId;

        private PersistentData(int type, int userId, int qualityForUi, byte[] payload) {
            this.type = type;
            this.userId = userId;
            this.qualityForUi = qualityForUi;
            this.payload = payload;
        }

        public static PersistentData fromBytes(byte[] frpData) {
            if (frpData == null || frpData.length == 0) {
                return NONE;
            }
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(frpData));
            try {
                byte version = is.readByte();
                if (version != 1) {
                    Slog.wtf(LockSettingsStorage.TAG, "Unknown PersistentData version code: " + ((int) version));
                    return NONE;
                }
                int type = is.readByte() & 255;
                int userId = is.readInt();
                int qualityForUi = is.readInt();
                byte[] payload = new byte[frpData.length - 10];
                System.arraycopy(frpData, 10, payload, 0, payload.length);
                return new PersistentData(type, userId, qualityForUi, payload);
            } catch (IOException e) {
                Slog.wtf(LockSettingsStorage.TAG, "Could not parse PersistentData", e);
                return NONE;
            }
        }

        public static byte[] toBytes(int persistentType, int userId, int qualityForUi, byte[] payload) {
            if (persistentType == 0) {
                Preconditions.checkArgument(payload == null, "TYPE_NONE must have empty payload");
                return null;
            }
            if (payload != null && payload.length > 0) {
                r0 = true;
            }
            Preconditions.checkArgument(r0, "empty payload must only be used with TYPE_NONE");
            ByteArrayOutputStream os = new ByteArrayOutputStream(payload.length + 10);
            DataOutputStream dos = new DataOutputStream(os);
            try {
                dos.writeByte(1);
                dos.writeByte(persistentType);
                dos.writeInt(userId);
                dos.writeInt(qualityForUi);
                dos.write(payload);
                return os.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("ByteArrayOutputStream cannot throw IOException");
            }
        }
    }

    public void dump(IndentingPrintWriter pw) {
        UserManager um = UserManager.get(this.mContext);
        for (UserInfo user : um.getUsers()) {
            File userPath = getSyntheticPasswordDirectoryForUser(user.id);
            pw.println(String.format("User %d [%s]:", Integer.valueOf(user.id), userPath.getAbsolutePath()));
            pw.increaseIndent();
            File[] files = userPath.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    pw.println(String.format("%6d %s %s", Long.valueOf(file.length()), LockSettingsService.timestampToString(file.lastModified()), file.getName()));
                }
            } else {
                pw.println("[Not found]");
            }
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "locksettings.db";
        private static final int DATABASE_VERSION = 2;
        private static final int IDLE_CONNECTION_TIMEOUT_MS = 30000;
        private static final String TAG = "LockSettingsDB";
        private Callback mCallback;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 2);
            setWriteAheadLoggingEnabled(false);
            setIdleConnectionTimeout(30000L);
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        private void createTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE locksettings (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,user INTEGER,value TEXT);");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            createTable(db);
            Callback callback = this.mCallback;
            if (callback != null) {
                callback.initialize(db);
            }
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            int upgradeVersion = oldVersion;
            if (upgradeVersion == 1) {
                upgradeVersion = 2;
            }
            if (upgradeVersion != 2) {
                Slog.w(TAG, "Failed to upgrade database!");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Cache {
        private final ArrayMap<CacheKey, Object> mCache;
        private final CacheKey mCacheKey;
        private int mVersion;

        private Cache() {
            this.mCache = new ArrayMap<>();
            this.mCacheKey = new CacheKey();
            this.mVersion = 0;
        }

        String peekKeyValue(String key, String defaultValue, int userId) {
            Object cached = peek(0, key, userId);
            return cached == LockSettingsStorage.DEFAULT ? defaultValue : (String) cached;
        }

        boolean hasKeyValue(String key, int userId) {
            return contains(0, key, userId);
        }

        void putKeyValue(String key, String value, int userId) {
            put(0, key, value, userId);
        }

        void putKeyValueIfUnchanged(String key, Object value, int userId, int version) {
            putIfUnchanged(0, key, value, userId, version);
        }

        void removeKey(String key, int userId) {
            remove(0, key, userId);
        }

        byte[] peekFile(String fileName) {
            return copyOf((byte[]) peek(1, fileName, -1));
        }

        boolean hasFile(String fileName) {
            return contains(1, fileName, -1);
        }

        void putFile(String key, byte[] value) {
            put(1, key, copyOf(value), -1);
        }

        void putFileIfUnchanged(String key, byte[] value, int version) {
            putIfUnchanged(1, key, copyOf(value), -1, version);
        }

        void setFetched(int userId) {
            put(2, "isFetched", "true", userId);
        }

        boolean isFetched(int userId) {
            return contains(2, "", userId);
        }

        private synchronized void remove(int type, String key, int userId) {
            this.mCache.remove(this.mCacheKey.set(type, key, userId));
        }

        private synchronized void put(int type, String key, Object value, int userId) {
            this.mCache.put(new CacheKey().set(type, key, userId), value);
            this.mVersion++;
        }

        private synchronized void putIfUnchanged(int type, String key, Object value, int userId, int version) {
            if (!contains(type, key, userId) && this.mVersion == version) {
                put(type, key, value, userId);
            }
        }

        private synchronized boolean contains(int type, String key, int userId) {
            return this.mCache.containsKey(this.mCacheKey.set(type, key, userId));
        }

        private synchronized Object peek(int type, String key, int userId) {
            return this.mCache.get(this.mCacheKey.set(type, key, userId));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized int getVersion() {
            return this.mVersion;
        }

        synchronized void removeUser(int userId) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                if (this.mCache.keyAt(i).userId == userId) {
                    this.mCache.removeAt(i);
                }
            }
            int i2 = this.mVersion;
            this.mVersion = i2 + 1;
        }

        private byte[] copyOf(byte[] data) {
            if (data != null) {
                return Arrays.copyOf(data, data.length);
            }
            return null;
        }

        synchronized void purgePath(String path) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                CacheKey entry = this.mCache.keyAt(i);
                if (entry.type == 1 && entry.key.startsWith(path)) {
                    this.mCache.removeAt(i);
                }
            }
            int i2 = this.mVersion;
            this.mVersion = i2 + 1;
        }

        synchronized void clear() {
            this.mCache.clear();
            this.mVersion++;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static final class CacheKey {
            static final int TYPE_FETCHED = 2;
            static final int TYPE_FILE = 1;
            static final int TYPE_KEY_VALUE = 0;
            String key;
            int type;
            int userId;

            private CacheKey() {
            }

            public CacheKey set(int type, String key, int userId) {
                this.type = type;
                this.key = key;
                this.userId = userId;
                return this;
            }

            public boolean equals(Object obj) {
                if (obj instanceof CacheKey) {
                    CacheKey o = (CacheKey) obj;
                    return this.userId == o.userId && this.type == o.type && this.key.equals(o.key);
                }
                return false;
            }

            public int hashCode() {
                return (this.key.hashCode() ^ this.userId) ^ this.type;
            }
        }
    }
}
