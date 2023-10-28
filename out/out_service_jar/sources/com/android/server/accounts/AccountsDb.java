package com.android.server.accounts;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AccountsDb implements AutoCloseable {
    private static final String ACCOUNTS_ID = "_id";
    private static final String ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS = "last_password_entry_time_millis_epoch";
    private static final String ACCOUNTS_NAME = "name";
    private static final String ACCOUNTS_PASSWORD = "password";
    private static final String ACCOUNTS_PREVIOUS_NAME = "previous_name";
    private static final String ACCOUNTS_TYPE = "type";
    private static final String ACCOUNT_ACCESS_GRANTS = "SELECT name, uid FROM accounts, grants WHERE accounts_id=_id";
    private static final String AUTHTOKENS_ACCOUNTS_ID = "accounts_id";
    private static final String AUTHTOKENS_ID = "_id";
    private static final String AUTHTOKENS_TYPE = "type";
    static final String CE_DATABASE_NAME = "accounts_ce.db";
    private static final int CE_DATABASE_VERSION = 10;
    private static final String CE_DB_PREFIX = "ceDb.";
    private static final String CE_TABLE_ACCOUNTS = "ceDb.accounts";
    private static final String CE_TABLE_AUTHTOKENS = "ceDb.authtokens";
    private static final String CE_TABLE_EXTRAS = "ceDb.extras";
    private static final String COUNT_OF_MATCHING_GRANTS = "SELECT COUNT(*) FROM grants, accounts WHERE accounts_id=_id AND uid=? AND auth_token_type=? AND name=? AND type=?";
    private static final String COUNT_OF_MATCHING_GRANTS_ANY_TOKEN = "SELECT COUNT(*) FROM grants, accounts WHERE accounts_id=_id AND uid=? AND name=? AND type=?";
    private static final String DATABASE_NAME = "accounts.db";
    static final String DE_DATABASE_NAME = "accounts_de.db";
    private static final int DE_DATABASE_VERSION = 3;
    private static final String EXTRAS_ACCOUNTS_ID = "accounts_id";
    private static final String EXTRAS_ID = "_id";
    private static final String EXTRAS_KEY = "key";
    private static final String EXTRAS_VALUE = "value";
    private static final String GRANTS_ACCOUNTS_ID = "accounts_id";
    private static final String GRANTS_AUTH_TOKEN_TYPE = "auth_token_type";
    private static final String GRANTS_GRANTEE_UID = "uid";
    static final int MAX_DEBUG_DB_SIZE = 64;
    private static final String META_KEY = "key";
    private static final String META_KEY_DELIMITER = ":";
    private static final String META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX = "auth_uid_for_type:";
    private static final String META_VALUE = "value";
    private static final int PRE_N_DATABASE_VERSION = 9;
    private static final String SELECTION_ACCOUNTS_ID_BY_ACCOUNT = "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";
    private static final String SELECTION_META_BY_AUTHENTICATOR_TYPE = "key LIKE ?";
    private static final String SHARED_ACCOUNTS_ID = "_id";
    static final String TABLE_ACCOUNTS = "accounts";
    private static final String TABLE_AUTHTOKENS = "authtokens";
    private static final String TABLE_EXTRAS = "extras";
    private static final String TABLE_GRANTS = "grants";
    private static final String TABLE_META = "meta";
    static final String TABLE_SHARED_ACCOUNTS = "shared_accounts";
    private static final String TABLE_VISIBILITY = "visibility";
    private static final String TAG = "AccountsDb";
    private static final String VISIBILITY_ACCOUNTS_ID = "accounts_id";
    private static final String VISIBILITY_PACKAGE = "_package";
    private static final String VISIBILITY_VALUE = "value";
    private final Context mContext;
    private final DeDatabaseHelper mDeDatabase;
    private volatile SQLiteStatement mDebugStatementForLogging;
    private final File mPreNDatabaseFile;
    private static String TABLE_DEBUG = "debug_table";
    private static String DEBUG_TABLE_ACTION_TYPE = "action_type";
    private static String DEBUG_TABLE_TIMESTAMP = "time";
    private static String DEBUG_TABLE_CALLER_UID = "caller_uid";
    private static String DEBUG_TABLE_TABLE_NAME = "table_name";
    private static String DEBUG_TABLE_KEY = "primary_key";
    static String DEBUG_ACTION_SET_PASSWORD = "action_set_password";
    static String DEBUG_ACTION_CLEAR_PASSWORD = "action_clear_password";
    static String DEBUG_ACTION_ACCOUNT_ADD = "action_account_add";
    static String DEBUG_ACTION_ACCOUNT_REMOVE = "action_account_remove";
    static String DEBUG_ACTION_ACCOUNT_REMOVE_DE = "action_account_remove_de";
    static String DEBUG_ACTION_AUTHENTICATOR_REMOVE = "action_authenticator_remove";
    static String DEBUG_ACTION_ACCOUNT_RENAME = "action_account_rename";
    static String DEBUG_ACTION_CALLED_ACCOUNT_ADD = "action_called_account_add";
    static String DEBUG_ACTION_CALLED_ACCOUNT_REMOVE = "action_called_account_remove";
    static String DEBUG_ACTION_SYNC_DE_CE_ACCOUNTS = "action_sync_de_ce_accounts";
    static String DEBUG_ACTION_CALLED_START_ACCOUNT_ADD = "action_called_start_account_add";
    static String DEBUG_ACTION_CALLED_ACCOUNT_SESSION_FINISH = "action_called_account_session_finish";
    private static final String ACCOUNTS_TYPE_COUNT = "count(type)";
    private static final String[] ACCOUNT_TYPE_COUNT_PROJECTION = {DatabaseHelper.SoundModelContract.KEY_TYPE, ACCOUNTS_TYPE_COUNT};
    private static final String AUTHTOKENS_AUTHTOKEN = "authtoken";
    private static final String[] COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN = {DatabaseHelper.SoundModelContract.KEY_TYPE, AUTHTOKENS_AUTHTOKEN};
    private static final String[] COLUMNS_EXTRAS_KEY_AND_VALUE = {"key", "value"};
    final Object mDebugStatementLock = new Object();
    private volatile long mDebugDbInsertionPoint = -1;

    AccountsDb(DeDatabaseHelper deDatabase, Context context, File preNDatabaseFile) {
        this.mDeDatabase = deDatabase;
        this.mContext = context;
        this.mPreNDatabaseFile = preNDatabaseFile;
    }

    /* loaded from: classes.dex */
    private static class CeDatabaseHelper extends SQLiteOpenHelper {
        CeDatabaseHelper(Context context, String ceDatabaseName) {
            super(context, ceDatabaseName, (SQLiteDatabase.CursorFactory) null, 10);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            Log.i(AccountsDb.TAG, "Creating CE database " + getDatabaseName());
            db.execSQL("CREATE TABLE accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, password TEXT, UNIQUE(name,type))");
            db.execSQL("CREATE TABLE authtokens (  _id INTEGER PRIMARY KEY AUTOINCREMENT,  accounts_id INTEGER NOT NULL, type TEXT NOT NULL,  authtoken TEXT,  UNIQUE (accounts_id,type))");
            db.execSQL("CREATE TABLE extras ( _id INTEGER PRIMARY KEY AUTOINCREMENT, accounts_id INTEGER, key TEXT NOT NULL, value TEXT, UNIQUE(accounts_id,key))");
            createAccountsDeletionTrigger(db);
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM authtokens     WHERE accounts_id=OLD._id ;   DELETE FROM extras     WHERE accounts_id=OLD._id ; END");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(AccountsDb.TAG, "Upgrade CE from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 9) {
                if (Log.isLoggable(AccountsDb.TAG, 2)) {
                    Log.v(AccountsDb.TAG, "onUpgrade upgrading to v10");
                }
                db.execSQL("DROP TABLE IF EXISTS meta");
                db.execSQL("DROP TABLE IF EXISTS shared_accounts");
                db.execSQL("DROP TRIGGER IF EXISTS accountsDelete");
                createAccountsDeletionTrigger(db);
                db.execSQL("DROP TABLE IF EXISTS grants");
                db.execSQL("DROP TABLE IF EXISTS " + AccountsDb.TABLE_DEBUG);
                oldVersion++;
            }
            if (oldVersion != newVersion) {
                Log.e(AccountsDb.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(AccountsDb.TAG, "onDowngrade: recreate accounts CE table");
            AccountsDb.resetDatabase(db);
            onCreate(db);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountsDb.TAG, 2)) {
                Log.v(AccountsDb.TAG, "opened database accounts_ce.db");
            }
        }

        static CeDatabaseHelper create(Context context, File preNDatabaseFile, File ceDatabaseFile) {
            boolean newDbExists = ceDatabaseFile.exists();
            if (Log.isLoggable(AccountsDb.TAG, 2)) {
                Log.v(AccountsDb.TAG, "CeDatabaseHelper.create ceDatabaseFile=" + ceDatabaseFile + " oldDbExists=" + preNDatabaseFile.exists() + " newDbExists=" + newDbExists);
            }
            boolean removeOldDb = false;
            if (!newDbExists && preNDatabaseFile.exists()) {
                removeOldDb = migratePreNDbToCe(preNDatabaseFile, ceDatabaseFile);
            }
            CeDatabaseHelper ceHelper = new CeDatabaseHelper(context, ceDatabaseFile.getPath());
            ceHelper.getWritableDatabase();
            ceHelper.close();
            if (removeOldDb) {
                Slog.i(AccountsDb.TAG, "Migration complete - removing pre-N db " + preNDatabaseFile);
                if (!SQLiteDatabase.deleteDatabase(preNDatabaseFile)) {
                    Slog.e(AccountsDb.TAG, "Cannot remove pre-N db " + preNDatabaseFile);
                }
            }
            return ceHelper;
        }

        private static boolean migratePreNDbToCe(File oldDbFile, File ceDbFile) {
            Slog.i(AccountsDb.TAG, "Moving pre-N DB " + oldDbFile + " to CE " + ceDbFile);
            try {
                FileUtils.copyFileOrThrow(oldDbFile, ceDbFile);
                return true;
            } catch (IOException e) {
                Slog.e(AccountsDb.TAG, "Cannot copy file to " + ceDbFile + " from " + oldDbFile, e);
                AccountsDb.deleteDbFileWarnIfFailed(ceDbFile);
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Cursor findAuthtokenForAllAccounts(String accountType, String authToken) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        return db.rawQuery("SELECT ceDb.authtokens._id, ceDb.accounts.name, ceDb.authtokens.type FROM ceDb.accounts JOIN ceDb.authtokens ON ceDb.accounts._id = ceDb.authtokens.accounts_id WHERE ceDb.authtokens.authtoken = ? AND ceDb.accounts.type = ?", new String[]{authToken, accountType});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, String> findAuthTokensByAccount(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        HashMap<String, String> authTokensForAccount = new HashMap<>();
        Cursor cursor = db.query(CE_TABLE_AUTHTOKENS, COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN, SELECTION_ACCOUNTS_ID_BY_ACCOUNT, new String[]{account.name, account.type}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                String type = cursor.getString(0);
                String authToken = cursor.getString(1);
                authTokensForAccount.put(type, authToken);
            } finally {
                cursor.close();
            }
        }
        return authTokensForAccount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteAuthtokensByAccountIdAndType(long accountId, String authtokenType) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_AUTHTOKENS, "accounts_id=? AND type=?", new String[]{String.valueOf(accountId), authtokenType}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteAuthToken(String authTokenId) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_AUTHTOKENS, "_id= ?", new String[]{authTokenId}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertAuthToken(long accountId, String authTokenType, String authToken) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put("accounts_id", Long.valueOf(accountId));
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, authTokenType);
        values.put(AUTHTOKENS_AUTHTOKEN, authToken);
        return db.insert(CE_TABLE_AUTHTOKENS, AUTHTOKENS_AUTHTOKEN, values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int updateCeAccountPassword(long accountId, String password) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_PASSWORD, password);
        return db.update(CE_TABLE_ACCOUNTS, values, "_id=?", new String[]{String.valueOf(accountId)});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean renameCeAccount(long accountId, String newName) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        String[] argsAccountId = {String.valueOf(accountId)};
        return db.update(CE_TABLE_ACCOUNTS, values, "_id=?", argsAccountId) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteAuthTokensByAccountId(long accountId) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_AUTHTOKENS, "accounts_id=?", new String[]{String.valueOf(accountId)}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findExtrasIdByAccountId(long accountId, String key) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        Cursor cursor = db.query(CE_TABLE_EXTRAS, new String[]{"_id"}, "accounts_id=" + accountId + " AND key=?", new String[]{key}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1L;
        } finally {
            cursor.close();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateExtra(long extrasId, String value) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put("value", value);
        int rows = db.update(TABLE_EXTRAS, values, "_id=?", new String[]{String.valueOf(extrasId)});
        return rows == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertExtra(long accountId, String key, String value) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("accounts_id", Long.valueOf(accountId));
        values.put("value", value);
        return db.insert(CE_TABLE_EXTRAS, "key", values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, String> findUserExtrasForAccount(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        Map<String, String> userExtrasForAccount = new HashMap<>();
        String[] selectionArgs = {account.name, account.type};
        Cursor cursor = db.query(CE_TABLE_EXTRAS, COLUMNS_EXTRAS_KEY_AND_VALUE, SELECTION_ACCOUNTS_ID_BY_ACCOUNT, selectionArgs, null, null, null);
        while (cursor.moveToNext()) {
            try {
                String tmpkey = cursor.getString(0);
                String value = cursor.getString(1);
                userExtrasForAccount.put(tmpkey, value);
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return userExtrasForAccount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findCeAccountId(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        String[] columns = {"_id"};
        String[] selectionArgs = {account.name, account.type};
        Cursor cursor = db.query(CE_TABLE_ACCOUNTS, columns, "name=? AND type=?", selectionArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                long j = cursor.getLong(0);
                if (cursor != null) {
                    cursor.close();
                }
                return j;
            }
            if (cursor != null) {
                cursor.close();
            }
            return -1L;
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String findAccountPasswordByNameAndType(String name, String type) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        String[] selectionArgs = {name, type};
        String[] columns = {ACCOUNTS_PASSWORD};
        Cursor cursor = db.query(CE_TABLE_ACCOUNTS, columns, "name=? AND type=?", selectionArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                String string = cursor.getString(0);
                if (cursor != null) {
                    cursor.close();
                }
                return string;
            }
            if (cursor != null) {
                cursor.close();
            }
            return null;
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertCeAccount(Account account, String password) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put("name", account.name);
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
        values.put(ACCOUNTS_PASSWORD, password);
        return db.insert(CE_TABLE_ACCOUNTS, "name", values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DeDatabaseHelper extends SQLiteOpenHelper {
        private volatile boolean mCeAttached;
        private final int mUserId;

        private DeDatabaseHelper(Context context, int userId, String deDatabaseName) {
            super(context, deDatabaseName, (SQLiteDatabase.CursorFactory) null, 3);
            this.mUserId = userId;
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            Log.i(AccountsDb.TAG, "Creating DE database for user " + this.mUserId);
            db.execSQL("CREATE TABLE accounts ( _id INTEGER PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, previous_name TEXT, last_password_entry_time_millis_epoch INTEGER DEFAULT 0, UNIQUE(name,type))");
            db.execSQL("CREATE TABLE meta ( key TEXT PRIMARY KEY NOT NULL, value TEXT)");
            createGrantsTable(db);
            createSharedAccountsTable(db);
            createAccountsDeletionTrigger(db);
            createDebugTable(db);
            createAccountsVisibilityTable(db);
            createAccountsDeletionVisibilityCleanupTrigger(db);
        }

        private void createSharedAccountsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shared_accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, UNIQUE(name,type))");
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM grants     WHERE accounts_id=OLD._id ; END");
        }

        private void createGrantsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  accounts_id INTEGER NOT NULL, auth_token_type STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (accounts_id,auth_token_type,uid))");
        }

        private void createAccountsVisibilityTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE visibility ( accounts_id INTEGER NOT NULL, _package TEXT NOT NULL, value INTEGER, PRIMARY KEY(accounts_id,_package))");
        }

        static void createDebugTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + AccountsDb.TABLE_DEBUG + " ( _id INTEGER," + AccountsDb.DEBUG_TABLE_ACTION_TYPE + " TEXT NOT NULL, " + AccountsDb.DEBUG_TABLE_TIMESTAMP + " DATETIME," + AccountsDb.DEBUG_TABLE_CALLER_UID + " INTEGER NOT NULL," + AccountsDb.DEBUG_TABLE_TABLE_NAME + " TEXT NOT NULL," + AccountsDb.DEBUG_TABLE_KEY + " INTEGER PRIMARY KEY)");
            db.execSQL("CREATE INDEX timestamp_index ON " + AccountsDb.TABLE_DEBUG + " (" + AccountsDb.DEBUG_TABLE_TIMESTAMP + ")");
        }

        private void createAccountsDeletionVisibilityCleanupTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDeleteVisibility DELETE ON accounts BEGIN   DELETE FROM visibility     WHERE accounts_id=OLD._id ; END");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(AccountsDb.TAG, "upgrade from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 1) {
                createAccountsVisibilityTable(db);
                createAccountsDeletionVisibilityCleanupTrigger(db);
                oldVersion = 3;
            }
            if (oldVersion == 2) {
                db.execSQL("DROP TRIGGER IF EXISTS accountsDeleteVisibility");
                db.execSQL("DROP TABLE IF EXISTS visibility");
                createAccountsVisibilityTable(db);
                createAccountsDeletionVisibilityCleanupTrigger(db);
                oldVersion++;
            }
            if (oldVersion != newVersion) {
                Log.e(AccountsDb.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(AccountsDb.TAG, "onDowngrade: recreate accounts DE table");
            AccountsDb.resetDatabase(db);
            onCreate(db);
        }

        public SQLiteDatabase getReadableDatabaseUserIsUnlocked() {
            if (!this.mCeAttached) {
                Log.wtf(AccountsDb.TAG, "getReadableDatabaseUserIsUnlocked called while user " + this.mUserId + " is still locked. CE database is not yet available.", new Throwable());
            }
            return super.getReadableDatabase();
        }

        public SQLiteDatabase getWritableDatabaseUserIsUnlocked() {
            if (!this.mCeAttached) {
                Log.wtf(AccountsDb.TAG, "getWritableDatabaseUserIsUnlocked called while user " + this.mUserId + " is still locked. CE database is not yet available.", new Throwable());
            }
            return super.getWritableDatabase();
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountsDb.TAG, 2)) {
                Log.v(AccountsDb.TAG, "opened database accounts_de.db");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void migratePreNDbToDe(File preNDbFile) {
            Log.i(AccountsDb.TAG, "Migrate pre-N database to DE preNDbFile=" + preNDbFile);
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("ATTACH DATABASE '" + preNDbFile.getPath() + "' AS preNDb");
            db.beginTransaction();
            db.execSQL("INSERT INTO accounts(_id,name,type, previous_name, last_password_entry_time_millis_epoch) SELECT _id,name,type, previous_name, last_password_entry_time_millis_epoch FROM preNDb.accounts");
            db.execSQL("INSERT INTO shared_accounts(_id,name,type) SELECT _id,name,type FROM preNDb.shared_accounts");
            db.execSQL("INSERT INTO " + AccountsDb.TABLE_DEBUG + "(_id," + AccountsDb.DEBUG_TABLE_ACTION_TYPE + "," + AccountsDb.DEBUG_TABLE_TIMESTAMP + "," + AccountsDb.DEBUG_TABLE_CALLER_UID + "," + AccountsDb.DEBUG_TABLE_TABLE_NAME + "," + AccountsDb.DEBUG_TABLE_KEY + ") SELECT _id," + AccountsDb.DEBUG_TABLE_ACTION_TYPE + "," + AccountsDb.DEBUG_TABLE_TIMESTAMP + "," + AccountsDb.DEBUG_TABLE_CALLER_UID + "," + AccountsDb.DEBUG_TABLE_TABLE_NAME + "," + AccountsDb.DEBUG_TABLE_KEY + " FROM preNDb." + AccountsDb.TABLE_DEBUG);
            db.execSQL("INSERT INTO grants(accounts_id,auth_token_type,uid) SELECT accounts_id,auth_token_type,uid FROM preNDb.grants");
            db.execSQL("INSERT INTO meta(key,value) SELECT key,value FROM preNDb.meta");
            db.setTransactionSuccessful();
            db.endTransaction();
            db.execSQL("DETACH DATABASE preNDb");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteDeAccount(long accountId) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_ACCOUNTS, new StringBuilder().append("_id=").append(accountId).toString(), null) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertSharedAccount(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", account.name);
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
        return db.insert(TABLE_SHARED_ACCOUNTS, "name", values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteSharedAccount(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_SHARED_ACCOUNTS, "name=? AND type=?", new String[]{account.name, account.type}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int renameSharedAccount(Account account, String newName) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        return db.update(TABLE_SHARED_ACCOUNTS, values, "name=? AND type=?", new String[]{account.name, account.type});
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:11:0x004d A[DONT_GENERATE] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public List<Account> getSharedAccounts() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        ArrayList<Account> accountList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SHARED_ACCOUNTS, new String[]{"name", DatabaseHelper.SoundModelContract.KEY_TYPE}, null, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return accountList;
            }
            int nameIndex = cursor.getColumnIndex("name");
            int typeIndex = cursor.getColumnIndex(DatabaseHelper.SoundModelContract.KEY_TYPE);
            do {
                accountList.add(new Account(cursor.getString(nameIndex), cursor.getString(typeIndex)));
            } while (cursor.moveToNext());
            return accountList;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findSharedAccountId(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHARED_ACCOUNTS, new String[]{"_id"}, "name=? AND type=?", new String[]{account.name, account.type}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1L;
        } finally {
            cursor.close();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findAccountLastAuthenticatedTime(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        return DatabaseUtils.longForQuery(db, "SELECT last_password_entry_time_millis_epoch FROM accounts WHERE name=? AND type=?", new String[]{account.name, account.type});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateAccountLastAuthenticatedTime(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS, Long.valueOf(System.currentTimeMillis()));
        int rowCount = db.update(TABLE_ACCOUNTS, values, "name=? AND type=?", new String[]{account.name, account.type});
        return rowCount > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDeAccountsTable(PrintWriter pw) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACCOUNTS, ACCOUNT_TYPE_COUNT_PROJECTION, null, null, DatabaseHelper.SoundModelContract.KEY_TYPE, null, null);
        while (cursor.moveToNext()) {
            try {
                pw.println(cursor.getString(0) + "," + cursor.getString(1));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findDeAccountId(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        String[] columns = {"_id"};
        String[] selectionArgs = {account.name, account.type};
        Cursor cursor = db.query(TABLE_ACCOUNTS, columns, "name=? AND type=?", selectionArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                long j = cursor.getLong(0);
                if (cursor != null) {
                    cursor.close();
                }
                return j;
            }
            if (cursor != null) {
                cursor.close();
            }
            return -1L;
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Long, Account> findAllDeAccounts() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        LinkedHashMap<Long, Account> map = new LinkedHashMap<>();
        String[] columns = {"_id", DatabaseHelper.SoundModelContract.KEY_TYPE, "name"};
        Cursor cursor = db.query(TABLE_ACCOUNTS, columns, null, null, null, null, "_id");
        while (cursor.moveToNext()) {
            try {
                long accountId = cursor.getLong(0);
                String accountType = cursor.getString(1);
                String accountName = cursor.getString(2);
                Account account = new Account(accountName, accountType);
                map.put(Long.valueOf(accountId), account);
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return map;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String findDeAccountPreviousName(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        String[] columns = {ACCOUNTS_PREVIOUS_NAME};
        String[] selectionArgs = {account.name, account.type};
        Cursor cursor = db.query(TABLE_ACCOUNTS, columns, "name=? AND type=?", selectionArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                String string = cursor.getString(0);
                if (cursor != null) {
                    cursor.close();
                }
                return string;
            } else if (cursor != null) {
                cursor.close();
                return null;
            } else {
                return null;
            }
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertDeAccount(Account account, long accountId) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", Long.valueOf(accountId));
        values.put("name", account.name);
        values.put(DatabaseHelper.SoundModelContract.KEY_TYPE, account.type);
        values.put(ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS, Long.valueOf(System.currentTimeMillis()));
        return db.insert(TABLE_ACCOUNTS, "name", values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean renameDeAccount(long accountId, String newName, String previousName) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put(ACCOUNTS_PREVIOUS_NAME, previousName);
        String[] argsAccountId = {String.valueOf(accountId)};
        return db.update(TABLE_ACCOUNTS, values, "_id=?", argsAccountId) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteGrantsByAccountIdAuthTokenTypeAndUid(long accountId, String authTokenType, long uid) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_GRANTS, "accounts_id=? AND auth_token_type=? AND uid=?", new String[]{String.valueOf(accountId), authTokenType, String.valueOf(uid)}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Integer> findAllUidGrants() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        List<Integer> result = new ArrayList<>();
        Cursor cursor = db.query(TABLE_GRANTS, new String[]{"uid"}, null, null, "uid", null, null);
        while (cursor.moveToNext()) {
            try {
                int uid = cursor.getInt(0);
                result.add(Integer.valueOf(uid));
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findMatchingGrantsCount(int uid, String authTokenType, Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        String[] args = {String.valueOf(uid), authTokenType, account.name, account.type};
        return DatabaseUtils.longForQuery(db, COUNT_OF_MATCHING_GRANTS, args);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long findMatchingGrantsCountAnyToken(int uid, Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        String[] args = {String.valueOf(uid), account.name, account.type};
        return DatabaseUtils.longForQuery(db, COUNT_OF_MATCHING_GRANTS_ANY_TOKEN, args);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertGrant(long accountId, String authTokenType, int uid) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("accounts_id", Long.valueOf(accountId));
        values.put(GRANTS_AUTH_TOKEN_TYPE, authTokenType);
        values.put("uid", Integer.valueOf(uid));
        return db.insert(TABLE_GRANTS, "accounts_id", values);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteGrantsByUid(int uid) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_GRANTS, "uid=?", new String[]{Integer.toString(uid)}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setAccountVisibility(long accountId, String packageName, int visibility) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("accounts_id", String.valueOf(accountId));
        values.put(VISIBILITY_PACKAGE, packageName);
        values.put("value", String.valueOf(visibility));
        return db.replace(TABLE_VISIBILITY, "value", values) != -1;
    }

    Integer findAccountVisibility(Account account, String packageName) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VISIBILITY, new String[]{"value"}, "accounts_id=(select _id FROM accounts WHERE name=? AND type=?) AND _package=? ", new String[]{account.name, account.type, packageName}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return Integer.valueOf(cursor.getInt(0));
            }
            cursor.close();
            return null;
        } finally {
            cursor.close();
        }
    }

    Integer findAccountVisibility(long accountId, String packageName) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VISIBILITY, new String[]{"value"}, "accounts_id=? AND _package=? ", new String[]{String.valueOf(accountId), packageName}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return Integer.valueOf(cursor.getInt(0));
            }
            cursor.close();
            return null;
        } finally {
            cursor.close();
        }
    }

    Account findDeAccountByAccountId(long accountId) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{"name", DatabaseHelper.SoundModelContract.KEY_TYPE}, "_id=? ", new String[]{String.valueOf(accountId)}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return new Account(cursor.getString(0), cursor.getString(1));
            }
            cursor.close();
            return null;
        } finally {
            cursor.close();
        }
    }

    Map<String, Integer> findAllVisibilityValuesForAccount(Account account) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Map<String, Integer> result = new HashMap<>();
        Cursor cursor = db.query(TABLE_VISIBILITY, new String[]{VISIBILITY_PACKAGE, "value"}, SELECTION_ACCOUNTS_ID_BY_ACCOUNT, new String[]{account.name, account.type}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                result.put(cursor.getString(0), Integer.valueOf(cursor.getInt(1)));
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Account, Map<String, Integer>> findAllVisibilityValues() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Map<Account, Map<String, Integer>> result = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT visibility._package, visibility.value, accounts.name, accounts.type FROM visibility JOIN accounts ON accounts._id = visibility.accounts_id", null);
        while (cursor.moveToNext()) {
            try {
                String packageName = cursor.getString(0);
                Integer visibility = Integer.valueOf(cursor.getInt(1));
                String accountName = cursor.getString(2);
                String accountType = cursor.getString(3);
                Account account = new Account(accountName, accountType);
                Map<String, Integer> accountVisibility = result.get(account);
                if (accountVisibility == null) {
                    accountVisibility = new HashMap<>();
                    result.put(account, accountVisibility);
                }
                accountVisibility.put(packageName, visibility);
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteAccountVisibilityForPackage(String packageName) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_VISIBILITY, "_package=? ", new String[]{packageName}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long insertOrReplaceMetaAuthTypeAndUid(String authenticatorType, int uid) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX + authenticatorType);
        values.put("value", Integer.valueOf(uid));
        return db.insertWithOnConflict(TABLE_META, null, values, 5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, Integer> findMetaAuthUid() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor metaCursor = db.query(TABLE_META, new String[]{"key", "value"}, SELECTION_META_BY_AUTHENTICATOR_TYPE, new String[]{"auth_uid_for_type:%"}, null, null, "key");
        Map<String, Integer> map = new LinkedHashMap<>();
        while (metaCursor.moveToNext()) {
            try {
                String type = TextUtils.split(metaCursor.getString(0), META_KEY_DELIMITER)[1];
                String uidStr = metaCursor.getString(1);
                if (!TextUtils.isEmpty(type) && !TextUtils.isEmpty(uidStr)) {
                    int uid = Integer.parseInt(metaCursor.getString(1));
                    map.put(type, Integer.valueOf(uid));
                }
                Slog.e(TAG, "Auth type empty: " + TextUtils.isEmpty(type) + ", uid empty: " + TextUtils.isEmpty(uidStr));
            } finally {
                metaCursor.close();
            }
        }
        return map;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteMetaByAuthTypeAndUid(String type, int uid) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        return db.delete(TABLE_META, "key=? AND value=?", new String[]{new StringBuilder().append(META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX).append(type).toString(), String.valueOf(uid)}) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Pair<String, Integer>> findAllAccountGrants() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery(ACCOUNT_ACCESS_GRANTS, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    List<Pair<String, Integer>> results = new ArrayList<>();
                    do {
                        String accountName = cursor.getString(0);
                        int uid = cursor.getInt(1);
                        results.add(Pair.create(accountName, Integer.valueOf(uid)));
                    } while (cursor.moveToNext());
                    if (cursor != null) {
                        cursor.close();
                    }
                    return results;
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        List<Pair<String, Integer>> results2 = Collections.emptyList();
        if (cursor != null) {
            cursor.close();
        }
        return results2;
    }

    /* loaded from: classes.dex */
    private static class PreNDatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        private final int mUserId;

        PreNDatabaseHelper(Context context, int userId, String preNDatabaseName) {
            super(context, preNDatabaseName, (SQLiteDatabase.CursorFactory) null, 9);
            this.mContext = context;
            this.mUserId = userId;
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            throw new IllegalStateException("Legacy database cannot be created - only upgraded!");
        }

        private void createSharedAccountsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE shared_accounts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, UNIQUE(name,type))");
        }

        private void addLastSuccessfullAuthenticatedTimeColumn(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN last_password_entry_time_millis_epoch DEFAULT 0");
        }

        private void addOldAccountNameColumn(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN previous_name");
        }

        private void addDebugTable(SQLiteDatabase db) {
            DeDatabaseHelper.createDebugTable(db);
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(" CREATE TRIGGER accountsDelete DELETE ON accounts BEGIN   DELETE FROM authtokens     WHERE accounts_id=OLD._id ;   DELETE FROM extras     WHERE accounts_id=OLD._id ;   DELETE FROM grants     WHERE accounts_id=OLD._id ; END");
        }

        private void createGrantsTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE grants (  accounts_id INTEGER NOT NULL, auth_token_type STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (accounts_id,auth_token_type,uid))");
        }

        static long insertMetaAuthTypeAndUid(SQLiteDatabase db, String authenticatorType, int uid) {
            ContentValues values = new ContentValues();
            values.put("key", AccountsDb.META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX + authenticatorType);
            values.put("value", Integer.valueOf(uid));
            return db.insert(AccountsDb.TABLE_META, null, values);
        }

        private void populateMetaTableWithAuthTypeAndUID(SQLiteDatabase db, Map<String, Integer> authTypeAndUIDMap) {
            for (Map.Entry<String, Integer> entry : authTypeAndUIDMap.entrySet()) {
                insertMetaAuthTypeAndUid(db, entry.getKey(), entry.getValue().intValue());
            }
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(AccountsDb.TAG, "upgrade from version " + oldVersion + " to version " + newVersion);
            if (oldVersion == 1) {
                oldVersion++;
            }
            if (oldVersion == 2) {
                createGrantsTable(db);
                db.execSQL("DROP TRIGGER accountsDelete");
                createAccountsDeletionTrigger(db);
                oldVersion++;
            }
            if (oldVersion == 3) {
                db.execSQL("UPDATE accounts SET type = 'com.google' WHERE type == 'com.google.GAIA'");
                oldVersion++;
            }
            if (oldVersion == 4) {
                createSharedAccountsTable(db);
                oldVersion++;
            }
            if (oldVersion == 5) {
                addOldAccountNameColumn(db);
                oldVersion++;
            }
            if (oldVersion == 6) {
                addLastSuccessfullAuthenticatedTimeColumn(db);
                oldVersion++;
            }
            if (oldVersion == 7) {
                addDebugTable(db);
                oldVersion++;
            }
            if (oldVersion == 8) {
                populateMetaTableWithAuthTypeAndUID(db, AccountManagerService.getAuthenticatorTypeAndUIDForUser(this.mContext, this.mUserId));
                oldVersion++;
            }
            if (oldVersion != newVersion) {
                Log.e(AccountsDb.TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(AccountsDb.TAG, 2)) {
                Log.v(AccountsDb.TAG, "opened database accounts.db");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Account> findCeAccountsNotInDe() {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabaseUserIsUnlocked();
        Cursor cursor = db.rawQuery("SELECT name,type FROM ceDb.accounts WHERE NOT EXISTS  (SELECT _id FROM accounts WHERE _id=ceDb.accounts._id )", null);
        try {
            List<Account> accounts = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String accountName = cursor.getString(0);
                String accountType = cursor.getString(1);
                accounts.add(new Account(accountName, accountType));
            }
            return accounts;
        } finally {
            cursor.close();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteCeAccount(long accountId) {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_ACCOUNTS, new StringBuilder().append("_id=").append(accountId).toString(), null) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCeDatabaseAttached() {
        return this.mDeDatabase.mCeAttached;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void beginTransaction() {
        this.mDeDatabase.getWritableDatabase().beginTransaction();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTransactionSuccessful() {
        this.mDeDatabase.getWritableDatabase().setTransactionSuccessful();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endTransaction() {
        this.mDeDatabase.getWritableDatabase().endTransaction();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachCeDatabase(File ceDbFile) {
        CeDatabaseHelper.create(this.mContext, this.mPreNDatabaseFile, ceDbFile);
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        db.execSQL("ATTACH DATABASE '" + ceDbFile.getPath() + "' AS ceDb");
        this.mDeDatabase.mCeAttached = true;
    }

    long calculateDebugTableInsertionPoint() {
        try {
            SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
            String queryCountDebugDbRows = "SELECT COUNT(*) FROM " + TABLE_DEBUG;
            int size = (int) DatabaseUtils.longForQuery(db, queryCountDebugDbRows, null);
            if (size < 64) {
                return size;
            }
            String queryCountDebugDbRows2 = "SELECT " + DEBUG_TABLE_KEY + " FROM " + TABLE_DEBUG + " ORDER BY " + DEBUG_TABLE_TIMESTAMP + "," + DEBUG_TABLE_KEY + " LIMIT 1";
            return DatabaseUtils.longForQuery(db, queryCountDebugDbRows2, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to open debug table" + e);
            return -1L;
        }
    }

    SQLiteStatement compileSqlStatementForLogging() {
        SQLiteDatabase db = this.mDeDatabase.getWritableDatabase();
        String sql = "INSERT OR REPLACE INTO " + TABLE_DEBUG + " VALUES (?,?,?,?,?,?)";
        return db.compileStatement(sql);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SQLiteStatement getStatementForLogging() {
        if (this.mDebugStatementForLogging != null) {
            return this.mDebugStatementForLogging;
        }
        try {
            this.mDebugStatementForLogging = compileSqlStatementForLogging();
            return this.mDebugStatementForLogging;
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to open debug table" + e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeDebugStatement() {
        synchronized (this.mDebugStatementLock) {
            if (this.mDebugStatementForLogging != null) {
                this.mDebugStatementForLogging.close();
                this.mDebugStatementForLogging = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long reserveDebugDbInsertionPoint() {
        if (this.mDebugDbInsertionPoint == -1) {
            this.mDebugDbInsertionPoint = calculateDebugTableInsertionPoint();
            return this.mDebugDbInsertionPoint;
        }
        this.mDebugDbInsertionPoint = (this.mDebugDbInsertionPoint + 1) % 64;
        return this.mDebugDbInsertionPoint;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebugTable(PrintWriter pw) {
        SQLiteDatabase db = this.mDeDatabase.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DEBUG, null, null, null, null, null, DEBUG_TABLE_TIMESTAMP);
        pw.println("AccountId, Action_Type, timestamp, UID, TableName, Key");
        pw.println("Accounts History");
        while (cursor.moveToNext()) {
            try {
                pw.println(cursor.getString(0) + "," + cursor.getString(1) + "," + cursor.getString(2) + "," + cursor.getString(3) + "," + cursor.getString(4) + "," + cursor.getString(5));
            } finally {
                cursor.close();
            }
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        this.mDeDatabase.close();
    }

    static void deleteDbFileWarnIfFailed(File dbFile) {
        if (!SQLiteDatabase.deleteDatabase(dbFile)) {
            Log.w(TAG, "Database at " + dbFile + " was not deleted successfully");
        }
    }

    public static AccountsDb create(Context context, int userId, File preNDatabaseFile, File deDatabaseFile) {
        boolean newDbExists = deDatabaseFile.exists();
        DeDatabaseHelper deDatabaseHelper = new DeDatabaseHelper(context, userId, deDatabaseFile.getPath());
        if (!newDbExists && preNDatabaseFile.exists()) {
            PreNDatabaseHelper preNDatabaseHelper = new PreNDatabaseHelper(context, userId, preNDatabaseFile.getPath());
            preNDatabaseHelper.getWritableDatabase();
            preNDatabaseHelper.close();
            deDatabaseHelper.migratePreNDbToDe(preNDatabaseFile);
        }
        return new AccountsDb(deDatabaseHelper, context, preNDatabaseFile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void resetDatabase(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='table'", null);
        while (c.moveToNext()) {
            try {
                String name = c.getString(0);
                if (!"android_metadata".equals(name) && !"sqlite_sequence".equals(name)) {
                    db.execSQL("DROP TABLE IF EXISTS " + name);
                }
            } finally {
            }
        }
        if (c != null) {
            c.close();
        }
        c = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='trigger'", null);
        while (c.moveToNext()) {
            try {
                db.execSQL("DROP TRIGGER IF EXISTS " + c.getString(0));
            } finally {
            }
        }
        if (c != null) {
            c.close();
        }
    }
}
