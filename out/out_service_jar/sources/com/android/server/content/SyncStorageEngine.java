package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IntPair;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.content.SyncManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SyncStorageEngine {
    private static final int ACCOUNTS_VERSION = 3;
    private static final String ACCOUNT_INFO_FILE_NAME = "accounts.xml";
    private static final double DEFAULT_FLEX_PERCENT_SYNC = 0.04d;
    private static final long DEFAULT_MIN_FLEX_ALLOWED_SECS = 5;
    private static final long DEFAULT_POLL_FREQUENCY_SECONDS = 86400;
    private static final boolean DELETE_LEGACY_PARCEL_FILES = true;
    public static final int EVENT_START = 0;
    public static final int EVENT_STOP = 1;
    private static final String LEGACY_STATISTICS_FILE_NAME = "stats.bin";
    private static final String LEGACY_STATUS_FILE_NAME = "status.bin";
    public static final int MAX_HISTORY = 100;
    public static final String MESG_CANCELED = "canceled";
    public static final String MESG_SUCCESS = "success";
    static final long MILLIS_IN_4WEEKS = 2419200000L;
    private static final int MSG_WRITE_STATISTICS = 2;
    private static final int MSG_WRITE_STATUS = 1;
    public static final long NOT_IN_BACKOFF_MODE = -1;
    public static final String[] SOURCES = {"OTHER", "LOCAL", "POLL", "USER", "PERIODIC", "FEED"};
    public static final int SOURCE_FEED = 5;
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_OTHER = 0;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
    public static final int SOURCE_USER = 3;
    public static final int STATISTICS_FILE_END = 0;
    public static final int STATISTICS_FILE_ITEM = 101;
    public static final int STATISTICS_FILE_ITEM_OLD = 100;
    private static final String STATISTICS_FILE_NAME = "stats";
    public static final int STATUS_FILE_END = 0;
    public static final int STATUS_FILE_ITEM = 100;
    private static final String STATUS_FILE_NAME = "status";
    private static final String SYNC_DIR_NAME = "sync";
    private static final boolean SYNC_ENABLED_DEFAULT = false;
    private static final String TAG = "SyncManager";
    private static final String TAG_FILE = "SyncManagerFile";
    private static final long WRITE_STATISTICS_DELAY = 1800000;
    private static final long WRITE_STATUS_DELAY = 600000;
    private static final String XML_ATTR_ENABLED = "enabled";
    private static final String XML_ATTR_LISTEN_FOR_TICKLES = "listen-for-tickles";
    private static final String XML_ATTR_NEXT_AUTHORITY_ID = "nextAuthorityId";
    private static final String XML_ATTR_SYNC_RANDOM_OFFSET = "offsetInSeconds";
    private static final String XML_ATTR_USER = "user";
    private static final String XML_TAG_LISTEN_FOR_TICKLES = "listenForTickles";
    private static PeriodicSyncAddedListener mPeriodicSyncAddedListener;
    private static HashMap<String, String> sAuthorityRenames;
    private static volatile SyncStorageEngine sSyncStorageEngine;
    private final AtomicFile mAccountInfoFile;
    private final HashMap<AccountAndUser, AccountInfo> mAccounts;
    final SparseArray<AuthorityInfo> mAuthorities;
    private OnAuthorityRemovedListener mAuthorityRemovedListener;
    private final Calendar mCal;
    private final RemoteCallbackList<ISyncStatusObserver> mChangeListeners;
    private final Context mContext;
    private final SparseArray<ArrayList<SyncInfo>> mCurrentSyncs;
    final DayStats[] mDayStats;
    private boolean mDefaultMasterSyncAutomatically;
    private boolean mGrantSyncAdaptersAccountAccess;
    private final MyHandler mHandler;
    private volatile boolean mIsClockValid;
    private final SyncLogger mLogger;
    private SparseArray<Boolean> mMasterSyncAutomatically;
    private int mNextAuthorityId;
    private int mNextHistoryId;
    private final PackageManagerInternal mPackageManagerInternal;
    private final ArrayMap<ComponentName, SparseArray<AuthorityInfo>> mServices;
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private File mSyncDir;
    private final ArrayList<SyncHistoryItem> mSyncHistory;
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    final SparseArray<SyncStatusInfo> mSyncStatus;
    private int mYear;
    private int mYearInDays;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnAuthorityRemovedListener {
        void onAuthorityRemoved(EndPoint endPoint);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnSyncRequestListener {
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle, int i2, int i3, int i4);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface PeriodicSyncAddedListener {
        void onPeriodicSyncAdded(EndPoint endPoint, Bundle bundle, long j, long j2);
    }

    /* loaded from: classes.dex */
    public static class SyncHistoryItem {
        int authorityId;
        long downstreamActivity;
        long elapsedTime;
        int event;
        long eventTime;
        Bundle extras;
        int historyId;
        boolean initialization;
        String mesg;
        int reason;
        int source;
        int syncExemptionFlag;
        long upstreamActivity;
    }

    static {
        HashMap<String, String> hashMap = new HashMap<>();
        sAuthorityRenames = hashMap;
        hashMap.put("contacts", "com.android.contacts");
        sAuthorityRenames.put("calendar", "com.android.calendar");
        sSyncStorageEngine = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AccountInfo {
        final AccountAndUser accountAndUser;
        final HashMap<String, AuthorityInfo> authorities = new HashMap<>();

        AccountInfo(AccountAndUser accountAndUser) {
            this.accountAndUser = accountAndUser;
        }
    }

    /* loaded from: classes.dex */
    public static class EndPoint {
        public static final EndPoint USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL = new EndPoint(null, null, -1);
        final Account account;
        final String provider;
        final int userId;

        public EndPoint(Account account, String provider, int userId) {
            this.account = account;
            this.provider = provider;
            this.userId = userId;
        }

        public boolean matchesSpec(EndPoint spec) {
            boolean accountsMatch;
            boolean providersMatch;
            int i = this.userId;
            int i2 = spec.userId;
            if (i == i2 || i == -1 || i2 == -1) {
                Account account = spec.account;
                if (account == null) {
                    accountsMatch = true;
                } else {
                    accountsMatch = this.account.equals(account);
                }
                String str = spec.provider;
                if (str == null) {
                    providersMatch = true;
                } else {
                    providersMatch = this.provider.equals(str);
                }
                return accountsMatch && providersMatch;
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Account account = this.account;
            StringBuilder append = sb.append(account == null ? "ALL ACCS" : account.name).append(SliceClientPermissions.SliceAuthority.DELIMITER);
            String str = this.provider;
            if (str == null) {
                str = "ALL PDRS";
            }
            append.append(str);
            sb.append(":u" + this.userId);
            return sb.toString();
        }

        public String toSafeString() {
            StringBuilder sb = new StringBuilder();
            Account account = this.account;
            StringBuilder append = sb.append(account == null ? "ALL ACCS" : SyncLogger.logSafe(account)).append(SliceClientPermissions.SliceAuthority.DELIMITER);
            String str = this.provider;
            if (str == null) {
                str = "ALL PDRS";
            }
            append.append(str);
            sb.append(":u" + this.userId);
            return sb.toString();
        }
    }

    /* loaded from: classes.dex */
    public static class AuthorityInfo {
        public static final int NOT_INITIALIZED = -1;
        public static final int NOT_SYNCABLE = 0;
        public static final int SYNCABLE = 1;
        public static final int SYNCABLE_NOT_INITIALIZED = 2;
        public static final int SYNCABLE_NO_ACCOUNT_ACCESS = 3;
        public static final int UNDEFINED = -2;
        long backoffDelay;
        long backoffTime;
        long delayUntil;
        boolean enabled;
        final int ident;
        final ArrayList<PeriodicSync> periodicSyncs;
        int syncable;
        final EndPoint target;

        AuthorityInfo(AuthorityInfo toCopy) {
            this.target = toCopy.target;
            this.ident = toCopy.ident;
            this.enabled = toCopy.enabled;
            this.syncable = toCopy.syncable;
            this.backoffTime = toCopy.backoffTime;
            this.backoffDelay = toCopy.backoffDelay;
            this.delayUntil = toCopy.delayUntil;
            this.periodicSyncs = new ArrayList<>();
            Iterator<PeriodicSync> it = toCopy.periodicSyncs.iterator();
            while (it.hasNext()) {
                PeriodicSync sync = it.next();
                this.periodicSyncs.add(new PeriodicSync(sync));
            }
        }

        AuthorityInfo(EndPoint info, int id) {
            this.target = info;
            this.ident = id;
            this.enabled = false;
            this.periodicSyncs = new ArrayList<>();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1L;
            this.backoffDelay = -1L;
            if (SyncStorageEngine.mPeriodicSyncAddedListener != null) {
                SyncStorageEngine.mPeriodicSyncAddedListener.onPeriodicSyncAdded(this.target, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
            }
        }

        public String toString() {
            return this.target + ", enabled=" + this.enabled + ", syncable=" + this.syncable + ", backoff=" + this.backoffTime + ", delay=" + this.delayUntil;
        }
    }

    /* loaded from: classes.dex */
    public static class DayStats {
        public final int day;
        public int failureCount;
        public long failureTime;
        public int successCount;
        public long successTime;

        public DayStats(int day) {
            this.day = day;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AccountAuthorityValidator {
        private final AccountManager mAccountManager;
        private final PackageManager mPackageManager;
        private final SparseArray<Account[]> mAccountsCache = new SparseArray<>();
        private final SparseArray<ArrayMap<String, Boolean>> mProvidersPerUserCache = new SparseArray<>();

        AccountAuthorityValidator(Context context) {
            this.mAccountManager = (AccountManager) context.getSystemService(AccountManager.class);
            this.mPackageManager = context.getPackageManager();
        }

        boolean isAccountValid(Account account, int userId) {
            Account[] accountsForUser = this.mAccountsCache.get(userId);
            if (accountsForUser == null) {
                accountsForUser = this.mAccountManager.getAccountsAsUser(userId);
                this.mAccountsCache.put(userId, accountsForUser);
            }
            return ArrayUtils.contains(accountsForUser, account);
        }

        boolean isAuthorityValid(String authority, int userId) {
            ArrayMap<String, Boolean> authorityMap = this.mProvidersPerUserCache.get(userId);
            if (authorityMap == null) {
                authorityMap = new ArrayMap<>();
                this.mProvidersPerUserCache.put(userId, authorityMap);
            }
            if (!authorityMap.containsKey(authority)) {
                authorityMap.put(authority, Boolean.valueOf(this.mPackageManager.resolveContentProviderAsUser(authority, 786432, userId) != null));
            }
            return authorityMap.get(authority).booleanValue();
        }
    }

    private SyncStorageEngine(Context context, File dataDir, Looper looper) {
        SparseArray<AuthorityInfo> sparseArray = new SparseArray<>();
        this.mAuthorities = sparseArray;
        this.mAccounts = new HashMap<>();
        this.mCurrentSyncs = new SparseArray<>();
        this.mSyncStatus = new SparseArray<>();
        this.mSyncHistory = new ArrayList<>();
        this.mChangeListeners = new RemoteCallbackList<>();
        this.mServices = new ArrayMap<>();
        this.mNextAuthorityId = 0;
        this.mDayStats = new DayStats[28];
        this.mNextHistoryId = 0;
        this.mMasterSyncAutomatically = new SparseArray<>();
        this.mHandler = new MyHandler(looper);
        this.mContext = context;
        sSyncStorageEngine = this;
        SyncLogger syncLogger = SyncLogger.getInstance();
        this.mLogger = syncLogger;
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = context.getResources().getBoolean(17891794);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        File systemDir = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        File file = new File(systemDir, SYNC_DIR_NAME);
        this.mSyncDir = file;
        file.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(this.mSyncDir);
        this.mAccountInfoFile = new AtomicFile(new File(this.mSyncDir, ACCOUNT_INFO_FILE_NAME), "sync-accounts");
        this.mStatusFile = new AtomicFile(new File(this.mSyncDir, STATUS_FILE_NAME), "sync-status");
        this.mStatisticsFile = new AtomicFile(new File(this.mSyncDir, STATISTICS_FILE_NAME), "sync-stats");
        readAccountInfoLocked();
        readStatusLocked();
        readStatisticsLocked();
        if (syncLogger.enabled()) {
            int size = sparseArray.size();
            syncLogger.log("Loaded ", Integer.valueOf(size), " items");
            for (int i = 0; i < size; i++) {
                this.mLogger.log(this.mAuthorities.valueAt(i));
            }
        }
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir(), Looper.getMainLooper());
    }

    public static void init(Context context, Looper looper) {
        if (sSyncStorageEngine != null) {
            return;
        }
        File dataDir = Environment.getDataDirectory();
        sSyncStorageEngine = new SyncStorageEngine(context, dataDir, looper);
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine == null) {
            throw new IllegalStateException("not initialized");
        }
        return sSyncStorageEngine;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setOnSyncRequestListener(OnSyncRequestListener listener) {
        if (this.mSyncRequestListener == null) {
            this.mSyncRequestListener = listener;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setOnAuthorityRemovedListener(OnAuthorityRemovedListener listener) {
        if (this.mAuthorityRemovedListener == null) {
            this.mAuthorityRemovedListener = listener;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setPeriodicSyncAddedListener(PeriodicSyncAddedListener listener) {
        if (mPeriodicSyncAddedListener == null) {
            mPeriodicSyncAddedListener = listener;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatusLocked();
                }
            } else if (msg.what == 2) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatisticsLocked();
                }
            }
        }
    }

    public int getSyncRandomOffset() {
        return this.mSyncRandomOffset;
    }

    public void addStatusChangeListener(int mask, int callingUid, ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            long cookie = IntPair.of(callingUid, mask);
            this.mChangeListeners.register(callback, Long.valueOf(cookie));
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.unregister(callback);
        }
    }

    public static long calculateDefaultFlexTime(long syncTimeSeconds) {
        if (syncTimeSeconds < DEFAULT_MIN_FLEX_ALLOWED_SECS) {
            return 0L;
        }
        if (syncTimeSeconds < DEFAULT_POLL_FREQUENCY_SECONDS) {
            return (long) (syncTimeSeconds * DEFAULT_FLEX_PERCENT_SYNC);
        }
        return 3456L;
    }

    void reportChange(int which, EndPoint target) {
        String syncAdapterPackageName;
        if (target.account == null || target.provider == null) {
            syncAdapterPackageName = null;
        } else {
            syncAdapterPackageName = ContentResolver.getSyncAdapterPackageAsUser(target.account.type, target.provider, target.userId);
        }
        reportChange(which, syncAdapterPackageName, target.userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportChange(int which, String callingPackageName, int callingUserId) {
        ArrayList<ISyncStatusObserver> reports = null;
        synchronized (this.mAuthorities) {
            int i = this.mChangeListeners.beginBroadcast();
            while (i > 0) {
                i--;
                long cookie = ((Long) this.mChangeListeners.getBroadcastCookie(i)).longValue();
                int registerUid = IntPair.first(cookie);
                int registerUserId = UserHandle.getUserId(registerUid);
                int mask = IntPair.second(cookie);
                if ((which & mask) != 0 && callingUserId == registerUserId && (callingPackageName == null || !this.mPackageManagerInternal.filterAppAccess(callingPackageName, registerUid, callingUserId))) {
                    if (reports == null) {
                        reports = new ArrayList<>(i);
                    }
                    reports.add(this.mChangeListeners.getBroadcastItem(i));
                }
            }
            this.mChangeListeners.finishBroadcast();
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "reportChange " + which + " to: " + reports);
        }
        if (reports != null) {
            int i2 = reports.size();
            while (i2 > 0) {
                i2--;
                try {
                    reports.get(i2).onStatusChanged(which);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public boolean getSyncAutomatically(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            boolean z = true;
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "getSyncAutomatically");
                if (authority == null || !authority.enabled) {
                    z = false;
                }
                return z;
            }
            int i = this.mAuthorities.size();
            while (i > 0) {
                i--;
                AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i);
                if (authorityInfo.target.matchesSpec(new EndPoint(account, providerName, userId)) && authorityInfo.enabled) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setSyncAutomatically(Account account, int userId, String providerName, boolean sync, int syncExemptionFlag, int callingUid, int callingPid) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.d("SyncManager", "setSyncAutomatically:  provider " + providerName + ", user " + userId + " -> " + sync);
        }
        this.mLogger.log("Set sync auto account=", account, " user=", Integer.valueOf(userId), " authority=", providerName, " value=", Boolean.toString(sync), " cuid=", Integer.valueOf(callingUid), " cpid=", Integer.valueOf(callingPid));
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(account, providerName, userId), -1, false);
            if (authority.enabled == sync) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.d("SyncManager", "setSyncAutomatically: already set to " + sync + ", doing nothing");
                }
                return;
            }
            if (sync && authority.syncable == 2) {
                authority.syncable = -1;
            }
            authority.enabled = sync;
            writeAccountInfoLocked();
            if (sync) {
                requestSync(account, userId, -6, providerName, new Bundle(), syncExemptionFlag, callingUid, callingPid);
            }
            reportChange(1, authority.target);
            queueBackup();
        }
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "get authority syncable");
                if (authority == null) {
                    return -1;
                }
                return authority.syncable;
            }
            int i = this.mAuthorities.size();
            while (i > 0) {
                i--;
                AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i);
                if (authorityInfo.target != null && authorityInfo.target.provider.equals(providerName)) {
                    return authorityInfo.syncable;
                }
            }
            return -1;
        }
    }

    public void setIsSyncable(Account account, int userId, String providerName, int syncable, int callingUid, int callingPid) {
        setSyncableStateForEndPoint(new EndPoint(account, providerName, userId), syncable, callingUid, callingPid);
    }

    private void setSyncableStateForEndPoint(EndPoint target, int syncable, int callingUid, int callingPid) {
        int syncable2;
        this.mLogger.log("Set syncable ", target, " value=", Integer.toString(syncable), " cuid=", Integer.valueOf(callingUid), " cpid=", Integer.valueOf(callingPid));
        synchronized (this.mAuthorities) {
            try {
                try {
                    AuthorityInfo aInfo = getOrCreateAuthorityLocked(target, -1, false);
                    if (syncable >= -1) {
                        syncable2 = syncable;
                    } else {
                        syncable2 = -1;
                    }
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.d("SyncManager", "setIsSyncable: " + aInfo.toString() + " -> " + syncable2);
                    }
                    if (aInfo.syncable == syncable2) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.d("SyncManager", "setIsSyncable: already set to " + syncable2 + ", doing nothing");
                        }
                        return;
                    }
                    aInfo.syncable = syncable2;
                    writeAccountInfoLocked();
                    if (syncable2 == 1) {
                        requestSync(aInfo, -5, new Bundle(), 0, callingUid, callingPid);
                    }
                    reportChange(1, target);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getBackoff");
            if (authority != null) {
                return Pair.create(Long.valueOf(authority.backoffTime), Long.valueOf(authority.backoffDelay));
            }
            return null;
        }
    }

    public void setBackoff(EndPoint info, long nextSyncTime, long nextDelay) {
        int i;
        boolean changed;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setBackoff: " + info + " -> nextSyncTime " + nextSyncTime + ", nextDelay " + nextDelay);
        }
        synchronized (this.mAuthorities) {
            if (info.account != null && info.provider != null) {
                AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
                if (authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay) {
                    changed = false;
                    i = 1;
                } else {
                    authorityInfo.backoffTime = nextSyncTime;
                    authorityInfo.backoffDelay = nextDelay;
                    changed = true;
                    i = 1;
                }
            }
            i = 1;
            changed = setBackoffLocked(info.account, info.userId, info.provider, nextSyncTime, nextDelay);
        }
        if (changed) {
            reportChange(i, info);
        }
    }

    private boolean setBackoffLocked(Account account, int userId, String providerName, long nextSyncTime, long nextDelay) {
        boolean changed = false;
        for (AccountInfo accountInfo : this.mAccounts.values()) {
            if (account == null || account.equals(accountInfo.accountAndUser.account) || userId == accountInfo.accountAndUser.userId) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (providerName == null || providerName.equals(authorityInfo.target.provider)) {
                        if (authorityInfo.backoffTime != nextSyncTime || authorityInfo.backoffDelay != nextDelay) {
                            authorityInfo.backoffTime = nextSyncTime;
                            authorityInfo.backoffDelay = nextDelay;
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    public void clearAllBackoffsLocked() {
        ArraySet<Integer> changedUserIds = new ArraySet<>();
        synchronized (this.mAuthorities) {
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "clearAllBackoffsLocked: authority:" + authorityInfo.target + " account:" + accountInfo.accountAndUser.account.name + " user:" + accountInfo.accountAndUser.userId + " backoffTime was: " + authorityInfo.backoffTime + " backoffDelay was: " + authorityInfo.backoffDelay);
                        }
                        authorityInfo.backoffTime = -1L;
                        authorityInfo.backoffDelay = -1L;
                        changedUserIds.add(Integer.valueOf(accountInfo.accountAndUser.userId));
                    }
                }
            }
        }
        for (int i = changedUserIds.size() - 1; i > 0; i--) {
            reportChange(1, null, changedUserIds.valueAt(i).intValue());
        }
    }

    public long getDelayUntilTime(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getDelayUntil");
            if (authority == null) {
                return 0L;
            }
            return authority.delayUntil;
        }
    }

    public void setDelayUntilTime(EndPoint info, long delayUntil) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setDelayUntil: " + info + " -> delayUntil " + delayUntil);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority.delayUntil == delayUntil) {
                return;
            }
            authority.delayUntil = delayUntil;
            reportChange(1, info);
        }
    }

    boolean restoreAllPeriodicSyncs() {
        if (mPeriodicSyncAddedListener == null) {
            return false;
        }
        synchronized (this.mAuthorities) {
            for (int i = 0; i < this.mAuthorities.size(); i++) {
                AuthorityInfo authority = this.mAuthorities.valueAt(i);
                Iterator<PeriodicSync> it = authority.periodicSyncs.iterator();
                while (it.hasNext()) {
                    PeriodicSync periodicSync = it.next();
                    mPeriodicSyncAddedListener.onPeriodicSyncAdded(authority.target, periodicSync.extras, periodicSync.period, periodicSync.flexTime);
                }
                authority.periodicSyncs.clear();
            }
            writeAccountInfoLocked();
        }
        return true;
    }

    public void setMasterSyncAutomatically(boolean flag, int userId, int syncExemptionFlag, int callingUid, int callingPid) {
        this.mLogger.log("Set master enabled=", Boolean.valueOf(flag), " user=", Integer.valueOf(userId), " cuid=", Integer.valueOf(callingUid), " cpid=", Integer.valueOf(callingPid));
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            if (auto == null || !auto.equals(Boolean.valueOf(flag))) {
                this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(flag));
                writeAccountInfoLocked();
                if (flag) {
                    requestSync(null, userId, -7, null, new Bundle(), syncExemptionFlag, callingUid, callingPid);
                }
                reportChange(1, null, userId);
                this.mContext.sendBroadcast(ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED);
                queueBackup();
            }
        }
    }

    public boolean getMasterSyncAutomatically(int userId) {
        boolean booleanValue;
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            booleanValue = auto == null ? this.mDefaultMasterSyncAutomatically : auto.booleanValue();
        }
        return booleanValue;
    }

    public int getAuthorityCount() {
        int size;
        synchronized (this.mAuthorities) {
            size = this.mAuthorities.size();
        }
        return size;
    }

    public AuthorityInfo getAuthority(int authorityId) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            authorityInfo = this.mAuthorities.get(authorityId);
        }
        return authorityInfo;
    }

    public boolean isSyncActive(EndPoint info) {
        synchronized (this.mAuthorities) {
            for (SyncInfo syncInfo : getCurrentSyncs(info.userId)) {
                AuthorityInfo ainfo = getAuthority(syncInfo.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void markPending(EndPoint info, boolean pendingValue) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority == null) {
                return;
            }
            SyncStatusInfo status = getOrCreateSyncStatusLocked(authority.ident);
            status.pending = pendingValue;
            reportChange(2, info);
        }
    }

    public void removeStaleAccounts(Account[] currentAccounts, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray<AuthorityInfo> removing = new SparseArray<>();
            Iterator<AccountInfo> accIt = this.mAccounts.values().iterator();
            while (accIt.hasNext()) {
                AccountInfo acc = accIt.next();
                if (acc.accountAndUser.userId == userId) {
                    if (currentAccounts == null || !ArrayUtils.contains(currentAccounts, acc.accountAndUser.account)) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "Account removed: " + acc.accountAndUser);
                        }
                        for (AuthorityInfo auth : acc.authorities.values()) {
                            removing.put(auth.ident, auth);
                        }
                        accIt.remove();
                    }
                }
            }
            int i = removing.size();
            if (i > 0) {
                while (i > 0) {
                    i--;
                    int ident = removing.keyAt(i);
                    AuthorityInfo auth2 = removing.valueAt(i);
                    OnAuthorityRemovedListener onAuthorityRemovedListener = this.mAuthorityRemovedListener;
                    if (onAuthorityRemovedListener != null) {
                        onAuthorityRemovedListener.onAuthorityRemoved(auth2.target);
                    }
                    this.mAuthorities.remove(ident);
                    int j = this.mSyncStatus.size();
                    while (j > 0) {
                        j--;
                        if (this.mSyncStatus.keyAt(j) == ident) {
                            SparseArray<SyncStatusInfo> sparseArray = this.mSyncStatus;
                            sparseArray.remove(sparseArray.keyAt(j));
                        }
                    }
                    int j2 = this.mSyncHistory.size();
                    while (j2 > 0) {
                        j2--;
                        if (this.mSyncHistory.get(j2).authorityId == ident) {
                            this.mSyncHistory.remove(j2);
                        }
                    }
                }
                writeAccountInfoLocked();
                writeStatusLocked();
                writeStatisticsLocked();
            }
        }
    }

    public SyncInfo addActiveSync(SyncManager.ActiveSyncContext activeSyncContext) {
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "setActiveSync: account= auth=" + activeSyncContext.mSyncOperation.target + " src=" + activeSyncContext.mSyncOperation.syncSource + " extras=" + activeSyncContext.mSyncOperation.getExtrasAsString());
            }
            EndPoint info = activeSyncContext.mSyncOperation.target;
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
            syncInfo = new SyncInfo(authorityInfo.ident, authorityInfo.target.account, authorityInfo.target.provider, activeSyncContext.mStartTime);
            getCurrentSyncs(authorityInfo.target.userId).add(syncInfo);
        }
        reportActiveChange(activeSyncContext.mSyncOperation.target);
        return syncInfo;
    }

    public void removeActiveSync(SyncInfo syncInfo, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removeActiveSync: account=" + syncInfo.account + " user=" + userId + " auth=" + syncInfo.authority);
            }
            getCurrentSyncs(userId).remove(syncInfo);
        }
        reportActiveChange(new EndPoint(syncInfo.account, syncInfo.authority, userId));
    }

    public void reportActiveChange(EndPoint target) {
        reportChange(4, target);
    }

    public long insertStartSyncEvent(SyncOperation op, long now) {
        ArrayList<SyncHistoryItem> arrayList;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "insertStartSyncEvent: " + op);
            }
            AuthorityInfo authority = getAuthorityLocked(op.target, "insertStartSyncEvent");
            if (authority == null) {
                return -1L;
            }
            SyncHistoryItem item = new SyncHistoryItem();
            item.initialization = op.isInitialization();
            item.authorityId = authority.ident;
            int i = this.mNextHistoryId;
            this.mNextHistoryId = i + 1;
            item.historyId = i;
            if (this.mNextHistoryId < 0) {
                this.mNextHistoryId = 0;
            }
            item.eventTime = now;
            item.source = op.syncSource;
            item.reason = op.reason;
            item.extras = op.getClonedExtras();
            item.event = 0;
            item.syncExemptionFlag = op.syncExemptionFlag;
            this.mSyncHistory.add(0, item);
            while (this.mSyncHistory.size() > 100) {
                this.mSyncHistory.remove(arrayList.size() - 1);
            }
            long id = item.historyId;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "returning historyId " + id);
            }
            reportChange(8, op.owningPackage, op.target.userId);
            return id;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x0161 A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0180 A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:53:0x020a A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x023a A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:62:0x023e A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0255 A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x0259 A[Catch: all -> 0x0279, TryCatch #1 {all -> 0x0279, blocks: (B:19:0x0075, B:20:0x00a8, B:28:0x010c, B:30:0x011a, B:36:0x014b, B:38:0x0161, B:40:0x0167, B:43:0x016f, B:51:0x01c1, B:53:0x020a, B:54:0x0211, B:55:0x0214, B:58:0x0224, B:56:0x0217, B:57:0x021e, B:59:0x0227, B:61:0x023a, B:66:0x0255, B:70:0x026e, B:67:0x0259, B:69:0x0262, B:62:0x023e, B:64:0x0247, B:44:0x0180, B:46:0x0188, B:49:0x0190, B:50:0x01b0, B:31:0x0126, B:33:0x012a, B:34:0x0142, B:22:0x00ac, B:23:0x00bc, B:24:0x00cc, B:25:0x00dc, B:26:0x00ec, B:27:0x00fc), top: B:84:0x0075 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void stopSyncEvent(long historyId, long elapsedTime, String resultMessage, long downstreamActivity, long upstreamActivity, String opPackageName, int userId) {
        int i;
        boolean writeStatisticsNow;
        boolean writeStatusNow;
        synchronized (this.mAuthorities) {
            try {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "stopSyncEvent: historyId=" + historyId);
                }
                SyncHistoryItem item = null;
                int i2 = this.mSyncHistory.size();
                while (i2 > 0) {
                    i2--;
                    item = this.mSyncHistory.get(i2);
                    if (item.historyId == historyId) {
                        break;
                    }
                    item = null;
                }
                if (item == null) {
                    Slog.w("SyncManager", "stopSyncEvent: no history for id " + historyId);
                    return;
                }
                item.elapsedTime = elapsedTime;
                item.event = 1;
                item.mesg = resultMessage;
                item.downstreamActivity = downstreamActivity;
                try {
                    item.upstreamActivity = upstreamActivity;
                    SyncStatusInfo status = getOrCreateSyncStatusLocked(item.authorityId);
                    status.maybeResetTodayStats(isClockValid(), false);
                    status.totalStats.numSyncs++;
                    status.todayStats.numSyncs++;
                    status.totalStats.totalElapsedTime += elapsedTime;
                    status.todayStats.totalElapsedTime += elapsedTime;
                    switch (item.source) {
                        case 0:
                            status.totalStats.numSourceOther++;
                            status.todayStats.numSourceOther++;
                            break;
                        case 1:
                            status.totalStats.numSourceLocal++;
                            status.todayStats.numSourceLocal++;
                            break;
                        case 2:
                            status.totalStats.numSourcePoll++;
                            status.todayStats.numSourcePoll++;
                            break;
                        case 3:
                            status.totalStats.numSourceUser++;
                            status.todayStats.numSourceUser++;
                            break;
                        case 4:
                            status.totalStats.numSourcePeriodic++;
                            status.todayStats.numSourcePeriodic++;
                            break;
                        case 5:
                            status.totalStats.numSourceFeed++;
                            status.todayStats.numSourceFeed++;
                            break;
                    }
                    int day = getCurrentDayLocked();
                    DayStats[] dayStatsArr = this.mDayStats;
                    DayStats dayStats = dayStatsArr[0];
                    if (dayStats == null) {
                        dayStatsArr[0] = new DayStats(day);
                        i = 0;
                    } else if (day == dayStats.day) {
                        i = 0;
                        DayStats dayStats2 = this.mDayStats[0];
                    } else {
                        DayStats[] dayStatsArr2 = this.mDayStats;
                        System.arraycopy(dayStatsArr2, 0, dayStatsArr2, 1, dayStatsArr2.length - 1);
                        this.mDayStats[0] = new DayStats(day);
                        writeStatisticsNow = true;
                        i = 0;
                        DayStats ds = this.mDayStats[i];
                        long lastSyncTime = item.eventTime + elapsedTime;
                        writeStatusNow = false;
                        if (!MESG_SUCCESS.equals(resultMessage)) {
                            writeStatusNow = (status.lastSuccessTime == 0 || status.lastFailureTime != 0) ? true : true;
                            status.setLastSuccess(item.source, lastSyncTime);
                            ds.successCount++;
                            ds.successTime += elapsedTime;
                        } else if (!MESG_CANCELED.equals(resultMessage)) {
                            if (status.lastFailureTime == 0) {
                                writeStatusNow = true;
                            }
                            status.totalStats.numFailures++;
                            status.todayStats.numFailures++;
                            status.setLastFailure(item.source, lastSyncTime, resultMessage);
                            ds.failureCount++;
                            ds.failureTime += elapsedTime;
                        } else {
                            status.totalStats.numCancels++;
                            status.todayStats.numCancels++;
                            writeStatusNow = true;
                        }
                        StringBuilder event = new StringBuilder();
                        event.append("" + resultMessage + " Source=" + SOURCES[item.source] + " Elapsed=");
                        SyncManager.formatDurationHMS(event, elapsedTime);
                        event.append(" Reason=");
                        event.append(SyncOperation.reasonToString(null, item.reason));
                        if (item.syncExemptionFlag != 0) {
                            event.append(" Exemption=");
                            switch (item.syncExemptionFlag) {
                                case 1:
                                    event.append("fg");
                                    break;
                                case 2:
                                    event.append("top");
                                    break;
                                default:
                                    event.append(item.syncExemptionFlag);
                                    break;
                            }
                        }
                        event.append(" Extras=");
                        SyncOperation.extrasToStringBuilder(item.extras, event);
                        status.addEvent(event.toString());
                        if (!writeStatusNow) {
                            writeStatusLocked();
                        } else if (!this.mHandler.hasMessages(1)) {
                            MyHandler myHandler = this.mHandler;
                            myHandler.sendMessageDelayed(myHandler.obtainMessage(1), 600000L);
                        }
                        if (!writeStatisticsNow) {
                            writeStatisticsLocked();
                        } else if (!this.mHandler.hasMessages(2)) {
                            MyHandler myHandler2 = this.mHandler;
                            myHandler2.sendMessageDelayed(myHandler2.obtainMessage(2), 1800000L);
                        }
                        reportChange(8, opPackageName, userId);
                    }
                    writeStatisticsNow = false;
                    DayStats ds2 = this.mDayStats[i];
                    long lastSyncTime2 = item.eventTime + elapsedTime;
                    writeStatusNow = false;
                    if (!MESG_SUCCESS.equals(resultMessage)) {
                    }
                    StringBuilder event2 = new StringBuilder();
                    event2.append("" + resultMessage + " Source=" + SOURCES[item.source] + " Elapsed=");
                    SyncManager.formatDurationHMS(event2, elapsedTime);
                    event2.append(" Reason=");
                    event2.append(SyncOperation.reasonToString(null, item.reason));
                    if (item.syncExemptionFlag != 0) {
                    }
                    event2.append(" Extras=");
                    SyncOperation.extrasToStringBuilder(item.extras, event2);
                    status.addEvent(event2.toString());
                    if (!writeStatusNow) {
                    }
                    if (!writeStatisticsNow) {
                    }
                    reportChange(8, opPackageName, userId);
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private List<SyncInfo> getCurrentSyncs(int userId) {
        List<SyncInfo> currentSyncsLocked;
        synchronized (this.mAuthorities) {
            currentSyncsLocked = getCurrentSyncsLocked(userId);
        }
        return currentSyncsLocked;
    }

    public List<SyncInfo> getCurrentSyncsCopy(int userId, boolean canAccessAccounts) {
        List<SyncInfo> syncsCopy;
        SyncInfo copy;
        synchronized (this.mAuthorities) {
            List<SyncInfo> syncs = getCurrentSyncsLocked(userId);
            syncsCopy = new ArrayList<>();
            for (SyncInfo sync : syncs) {
                if (!canAccessAccounts) {
                    copy = SyncInfo.createAccountRedacted(sync.authorityId, sync.authority, sync.startTime);
                } else {
                    copy = new SyncInfo(sync);
                }
                syncsCopy.add(copy);
            }
        }
        return syncsCopy;
    }

    private List<SyncInfo> getCurrentSyncsLocked(int userId) {
        ArrayList<SyncInfo> syncs = this.mCurrentSyncs.get(userId);
        if (syncs == null) {
            ArrayList<SyncInfo> syncs2 = new ArrayList<>();
            this.mCurrentSyncs.put(userId, syncs2);
            return syncs2;
        }
        return syncs;
    }

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint info) {
        Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
            createCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(authorityInfo);
        }
        return createCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint info) {
        if (info.account == null || info.provider == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return cur;
                }
            }
            return null;
        }
    }

    public boolean isSyncPending(EndPoint info) {
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info) && cur.pending) {
                    return true;
                }
            }
            return false;
        }
    }

    public ArrayList<SyncHistoryItem> getSyncHistory() {
        ArrayList<SyncHistoryItem> items;
        synchronized (this.mAuthorities) {
            int N = this.mSyncHistory.size();
            items = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                items.add(this.mSyncHistory.get(i));
            }
        }
        return items;
    }

    public DayStats[] getDayStatistics() {
        DayStats[] ds;
        synchronized (this.mAuthorities) {
            DayStats[] dayStatsArr = this.mDayStats;
            ds = new DayStats[dayStatsArr.length];
            System.arraycopy(dayStatsArr, 0, ds, 0, ds.length);
        }
        return ds;
    }

    private Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked(AuthorityInfo authorityInfo) {
        SyncStatusInfo syncStatusInfo = getOrCreateSyncStatusLocked(authorityInfo.ident);
        return Pair.create(new AuthorityInfo(authorityInfo), new SyncStatusInfo(syncStatusInfo));
    }

    private int getCurrentDayLocked() {
        this.mCal.setTimeInMillis(System.currentTimeMillis());
        int dayOfYear = this.mCal.get(6);
        if (this.mYear != this.mCal.get(1)) {
            this.mYear = this.mCal.get(1);
            this.mCal.clear();
            this.mCal.set(1, this.mYear);
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / 86400000);
        }
        return this.mYearInDays + dayOfYear;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint info, String tag) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo accountInfo = this.mAccounts.get(au);
        if (accountInfo == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", tag + ": unknown account " + au);
            }
            return null;
        }
        AuthorityInfo authority = accountInfo.authorities.get(info.provider);
        if (authority == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", tag + ": unknown provider " + info.provider);
            }
            return null;
        }
        return authority;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo account = this.mAccounts.get(au);
        if (account == null) {
            account = new AccountInfo(au);
            this.mAccounts.put(au, account);
        }
        AuthorityInfo authority = account.authorities.get(info.provider);
        if (authority == null) {
            AuthorityInfo authority2 = createAuthorityLocked(info, ident, doWrite);
            account.authorities.put(info.provider, authority2);
            return authority2;
        }
        return authority;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (ident < 0) {
            ident = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            doWrite = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "created a new AuthorityInfo for " + info);
        }
        AuthorityInfo authority = new AuthorityInfo(info, ident);
        this.mAuthorities.put(ident, authority);
        if (doWrite) {
            writeAccountInfoLocked();
        }
        return authority;
    }

    public void removeAuthority(EndPoint info) {
        synchronized (this.mAuthorities) {
            removeAuthorityLocked(info.account, info.userId, info.provider, true);
        }
    }

    private void removeAuthorityLocked(Account account, int userId, String authorityName, boolean doWrite) {
        AuthorityInfo authorityInfo;
        AccountInfo accountInfo = this.mAccounts.get(new AccountAndUser(account, userId));
        if (accountInfo != null && (authorityInfo = accountInfo.authorities.remove(authorityName)) != null) {
            OnAuthorityRemovedListener onAuthorityRemovedListener = this.mAuthorityRemovedListener;
            if (onAuthorityRemovedListener != null) {
                onAuthorityRemovedListener.onAuthorityRemoved(authorityInfo.target);
            }
            this.mAuthorities.remove(authorityInfo.ident);
            if (doWrite) {
                writeAccountInfoLocked();
            }
        }
    }

    private SyncStatusInfo getOrCreateSyncStatusLocked(int authorityId) {
        SyncStatusInfo status = this.mSyncStatus.get(authorityId);
        if (status == null) {
            SyncStatusInfo status2 = new SyncStatusInfo(authorityId);
            this.mSyncStatus.put(authorityId, status2);
            return status2;
        }
        return status;
    }

    public void writeAllState() {
        synchronized (this.mAuthorities) {
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public boolean shouldGrantSyncAdaptersAccountAccess() {
        return this.mGrantSyncAdaptersAccountAccess;
    }

    public void clearAndReadState() {
        synchronized (this.mAuthorities) {
            this.mAuthorities.clear();
            this.mAccounts.clear();
            this.mServices.clear();
            this.mSyncStatus.clear();
            this.mSyncHistory.clear();
            readAccountInfoLocked();
            readStatusLocked();
            readStatisticsLocked();
            writeAccountInfoLocked();
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1735=6, 1736=5, 1738=5, 1739=5, 1740=5] */
    private void readAccountInfoLocked() {
        TypedXmlPullParser parser;
        int nextId;
        TypedXmlPullParser parser2;
        int highestAuthorityId = -1;
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mAccountInfoFile.openRead();
                    if (Log.isLoggable(TAG_FILE, 2)) {
                        Slog.v(TAG_FILE, "Reading " + this.mAccountInfoFile.getBaseFile());
                    }
                    TypedXmlPullParser parser3 = Xml.resolvePullParser(fis);
                    int eventType = parser3.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = parser3.next();
                    }
                    if (eventType == 1) {
                        Slog.i("SyncManager", "No initial accounts");
                        this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                        if (fis != null) {
                            try {
                                fis.close();
                                return;
                            } catch (IOException e) {
                                return;
                            }
                        }
                        return;
                    }
                    if ("accounts".equals(parser3.getName())) {
                        boolean listen = parser3.getAttributeBoolean((String) null, XML_ATTR_LISTEN_FOR_TICKLES, true);
                        int version = parser3.getAttributeInt((String) null, "version", 0);
                        if (version < 3) {
                            this.mGrantSyncAdaptersAccountAccess = true;
                        }
                        int nextId2 = parser3.getAttributeInt((String) null, XML_ATTR_NEXT_AUTHORITY_ID, 0);
                        this.mNextAuthorityId = Math.max(this.mNextAuthorityId, nextId2);
                        int attributeInt = parser3.getAttributeInt((String) null, XML_ATTR_SYNC_RANDOM_OFFSET, 0);
                        this.mSyncRandomOffset = attributeInt;
                        if (attributeInt == 0) {
                            parser = parser3;
                            Random random = new Random(System.currentTimeMillis());
                            this.mSyncRandomOffset = random.nextInt(86400);
                        } else {
                            parser = parser3;
                        }
                        this.mMasterSyncAutomatically.put(0, Boolean.valueOf(listen));
                        int eventType2 = parser.next();
                        AuthorityInfo authority = null;
                        PeriodicSync periodicSync = null;
                        AccountAuthorityValidator validator = new AccountAuthorityValidator(this.mContext);
                        while (true) {
                            if (eventType2 == 2) {
                                String tagName = parser.getName();
                                nextId = nextId2;
                                if (parser.getDepth() != 2) {
                                    parser2 = parser;
                                    if (parser2.getDepth() == 3) {
                                        if ("periodicSync".equals(tagName) && authority != null) {
                                            periodicSync = parsePeriodicSync(parser2, authority);
                                        }
                                    } else if (parser2.getDepth() == 4 && periodicSync != null && "extra".equals(tagName)) {
                                        parseExtra(parser2, periodicSync.extras);
                                    }
                                } else if ("authority".equals(tagName)) {
                                    parser2 = parser;
                                    AuthorityInfo authority2 = parseAuthority(parser2, version, validator);
                                    if (authority2 == null) {
                                        EventLog.writeEvent(1397638484, "26513719", -1, "Malformed authority");
                                    } else if (authority2.ident > highestAuthorityId) {
                                        highestAuthorityId = authority2.ident;
                                        periodicSync = null;
                                        authority = authority2;
                                    }
                                    periodicSync = null;
                                    authority = authority2;
                                } else {
                                    parser2 = parser;
                                    if (XML_TAG_LISTEN_FOR_TICKLES.equals(tagName)) {
                                        parseListenForTickles(parser2);
                                    }
                                }
                            } else {
                                nextId = nextId2;
                                parser2 = parser;
                            }
                            eventType2 = parser2.next();
                            if (eventType2 == 1) {
                                break;
                            }
                            parser = parser2;
                            nextId2 = nextId;
                        }
                    }
                    maybeMigrateSettingsForRenamedAuthorities();
                } catch (IOException e2) {
                    if (fis == null) {
                        Slog.i("SyncManager", "No initial accounts");
                    } else {
                        Slog.w("SyncManager", "Error reading accounts", e2);
                    }
                    this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e3) {
                        }
                    }
                }
            } catch (XmlPullParserException e4) {
                Slog.w("SyncManager", "Error reading accounts", e4);
                this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e5) {
                    }
                }
            }
        } finally {
            this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e6) {
                }
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File syncDir) {
        File file = new File(syncDir, "pending.bin");
        if (!file.exists()) {
            return;
        }
        file.delete();
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        boolean writeNeeded = false;
        ArrayList<AuthorityInfo> authoritiesToRemove = new ArrayList<>();
        int N = this.mAuthorities.size();
        for (int i = 0; i < N; i++) {
            AuthorityInfo authority = this.mAuthorities.valueAt(i);
            String newAuthorityName = sAuthorityRenames.get(authority.target.provider);
            if (newAuthorityName != null) {
                authoritiesToRemove.add(authority);
                if (authority.enabled) {
                    EndPoint newInfo = new EndPoint(authority.target.account, newAuthorityName, authority.target.userId);
                    if (getAuthorityLocked(newInfo, "cleanup") == null) {
                        AuthorityInfo newAuthority = getOrCreateAuthorityLocked(newInfo, -1, false);
                        newAuthority.enabled = true;
                        writeNeeded = true;
                    }
                }
            }
        }
        Iterator<AuthorityInfo> it = authoritiesToRemove.iterator();
        while (it.hasNext()) {
            AuthorityInfo authorityInfo = it.next();
            removeAuthorityLocked(authorityInfo.target.account, authorityInfo.target.userId, authorityInfo.target.provider, false);
            writeNeeded = true;
        }
        return writeNeeded;
    }

    private void parseListenForTickles(TypedXmlPullParser parser) {
        try {
            parser.getAttributeInt((String) null, XML_ATTR_USER);
        } catch (XmlPullParserException e) {
            Slog.e("SyncManager", "error parsing the user for listen-for-tickles", e);
        }
        boolean listen = parser.getAttributeBoolean((String) null, "enabled", true);
        this.mMasterSyncAutomatically.put(0, Boolean.valueOf(listen));
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x0174  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x019d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private AuthorityInfo parseAuthority(TypedXmlPullParser parser, int version, AccountAuthorityValidator validator) throws XmlPullParserException {
        String accountType;
        String accountType2;
        String str;
        AuthorityInfo authority;
        int i;
        int i2;
        int parseInt;
        AuthorityInfo authority2 = null;
        int id = -1;
        try {
            id = parser.getAttributeInt((String) null, "id");
        } catch (XmlPullParserException e) {
            Slog.e("SyncManager", "error parsing the id of the authority", e);
        }
        if (id >= 0) {
            String authorityName = parser.getAttributeValue((String) null, "authority");
            boolean enabled = parser.getAttributeBoolean((String) null, "enabled", true);
            String syncable = parser.getAttributeValue((String) null, "syncable");
            String accountName = parser.getAttributeValue((String) null, "account");
            String accountType3 = parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE);
            int userId = parser.getAttributeInt((String) null, XML_ATTR_USER, 0);
            String packageName = parser.getAttributeValue((String) null, "package");
            String className = parser.getAttributeValue((String) null, "class");
            if (accountType3 == null && packageName == null) {
                String syncable2 = String.valueOf(-1);
                accountType = "com.google";
                accountType2 = syncable2;
            } else {
                accountType = accountType3;
                accountType2 = syncable;
            }
            AuthorityInfo authority3 = this.mAuthorities.get(id);
            if (!Log.isLoggable(TAG_FILE, 2)) {
                str = "SyncManager";
            } else {
                str = "SyncManager";
                Slog.v(TAG_FILE, "Adding authority: account=" + accountName + " accountType=" + accountType + " auth=" + authorityName + " package=" + packageName + " class=" + className + " user=" + userId + " enabled=" + enabled + " syncable=" + accountType2);
            }
            if (authority3 == null) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Creating authority entry");
                }
                if (accountName == null || authorityName == null) {
                    authority = authority3;
                    i = 1;
                    i2 = 0;
                } else {
                    EndPoint info = new EndPoint(new Account(accountName, accountType), authorityName, userId);
                    if (validator.isAccountValid(info.account, userId) && validator.isAuthorityValid(authorityName, userId)) {
                        AuthorityInfo authority4 = getOrCreateAuthorityLocked(info, id, false);
                        if (version > 0) {
                            authority4.periodicSyncs.clear();
                        }
                        authority2 = authority4;
                        i = 1;
                        i2 = 0;
                        if (authority2 == null) {
                            authority2.enabled = enabled;
                            if (accountType2 == null) {
                                parseInt = -1;
                            } else {
                                try {
                                    parseInt = Integer.parseInt(accountType2);
                                } catch (NumberFormatException e2) {
                                    if (UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN.equals(accountType2)) {
                                        authority2.syncable = -1;
                                    } else {
                                        authority2.syncable = Boolean.parseBoolean(accountType2) ? i : i2;
                                    }
                                }
                            }
                            authority2.syncable = parseInt;
                        } else {
                            Slog.w(str, "Failure adding authority: auth=" + authorityName + " enabled=" + enabled + " syncable=" + accountType2);
                        }
                    }
                    i2 = 0;
                    i = 1;
                    authority = authority3;
                    EventLog.writeEvent(1397638484, "35028827", -1, "account:" + info.account + " provider:" + authorityName + " user:" + userId);
                }
            } else {
                authority = authority3;
                i = 1;
                i2 = 0;
            }
            authority2 = authority;
            if (authority2 == null) {
            }
        }
        return authority2;
    }

    private PeriodicSync parsePeriodicSync(TypedXmlPullParser parser, AuthorityInfo authorityInfo) {
        long flextime;
        Bundle extras = new Bundle();
        try {
            long period = parser.getAttributeLong((String) null, "period");
            try {
                flextime = parser.getAttributeLong((String) null, "flex");
            } catch (XmlPullParserException e) {
                long flextime2 = calculateDefaultFlexTime(period);
                Slog.e("SyncManager", "Error formatting value parsed for periodic sync flex, using default: " + flextime2, e);
                flextime = flextime2;
            }
            PeriodicSync periodicSync = new PeriodicSync(authorityInfo.target.account, authorityInfo.target.provider, extras, period, flextime);
            authorityInfo.periodicSyncs.add(periodicSync);
            return periodicSync;
        } catch (XmlPullParserException e2) {
            Slog.e("SyncManager", "error parsing the period of a periodic sync", e2);
            return null;
        }
    }

    private void parseExtra(TypedXmlPullParser parser, Bundle extras) {
        String name = parser.getAttributeValue((String) null, "name");
        String type = parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        try {
            if ("long".equals(type)) {
                extras.putLong(name, parser.getAttributeLong((String) null, "value1"));
            } else if ("integer".equals(type)) {
                extras.putInt(name, parser.getAttributeInt((String) null, "value1"));
            } else if ("double".equals(type)) {
                extras.putDouble(name, parser.getAttributeDouble((String) null, "value1"));
            } else if ("float".equals(type)) {
                extras.putFloat(name, parser.getAttributeFloat((String) null, "value1"));
            } else if ("boolean".equals(type)) {
                extras.putBoolean(name, parser.getAttributeBoolean((String) null, "value1"));
            } else if ("string".equals(type)) {
                extras.putString(name, parser.getAttributeValue((String) null, "value1"));
            } else if ("account".equals(type)) {
                String value1 = parser.getAttributeValue((String) null, "value1");
                String value2 = parser.getAttributeValue((String) null, "value2");
                extras.putParcelable(name, new Account(value1, value2));
            }
        } catch (XmlPullParserException e) {
            Slog.e("SyncManager", "error parsing bundle value", e);
        }
    }

    private void writeAccountInfoLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mAccountInfoFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            fos = this.mAccountInfoFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            out.startDocument((String) null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag((String) null, "accounts");
            out.attributeInt((String) null, "version", 3);
            out.attributeInt((String) null, XML_ATTR_NEXT_AUTHORITY_ID, this.mNextAuthorityId);
            out.attributeInt((String) null, XML_ATTR_SYNC_RANDOM_OFFSET, this.mSyncRandomOffset);
            int M = this.mMasterSyncAutomatically.size();
            for (int m = 0; m < M; m++) {
                int userId = this.mMasterSyncAutomatically.keyAt(m);
                Boolean listen = this.mMasterSyncAutomatically.valueAt(m);
                out.startTag((String) null, XML_TAG_LISTEN_FOR_TICKLES);
                out.attributeInt((String) null, XML_ATTR_USER, userId);
                out.attributeBoolean((String) null, "enabled", listen.booleanValue());
                out.endTag((String) null, XML_TAG_LISTEN_FOR_TICKLES);
            }
            int N = this.mAuthorities.size();
            for (int i = 0; i < N; i++) {
                AuthorityInfo authority = this.mAuthorities.valueAt(i);
                EndPoint info = authority.target;
                out.startTag((String) null, "authority");
                out.attributeInt((String) null, "id", authority.ident);
                out.attributeInt((String) null, XML_ATTR_USER, info.userId);
                out.attributeBoolean((String) null, "enabled", authority.enabled);
                out.attribute((String) null, "account", info.account.name);
                out.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, info.account.type);
                out.attribute((String) null, "authority", info.provider);
                out.attributeInt((String) null, "syncable", authority.syncable);
                out.endTag((String) null, "authority");
            }
            out.endTag((String) null, "accounts");
            out.endDocument();
            this.mAccountInfoFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing accounts", e1);
            if (fos != null) {
                this.mAccountInfoFile.failWrite(fos);
            }
        }
    }

    private void readStatusParcelLocked(File parcel) {
        Parcel in;
        try {
            AtomicFile parcelFile = new AtomicFile(parcel);
            byte[] data = parcelFile.readFully();
            in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial status");
            return;
        }
        while (true) {
            int token = in.readInt();
            if (token != 0) {
                if (token != 100) {
                    Slog.w("SyncManager", "Unknown status token: " + token);
                    return;
                }
                try {
                    SyncStatusInfo status = new SyncStatusInfo(in);
                    if (this.mAuthorities.indexOfKey(status.authorityId) >= 0) {
                        status.pending = false;
                        this.mSyncStatus.put(status.authorityId, status);
                    }
                } catch (Exception e2) {
                    Slog.e("SyncManager", "Unable to parse some sync status.", e2);
                }
                Slog.i("SyncManager", "No initial status");
                return;
            }
            return;
        }
    }

    private void upgradeStatusIfNeededLocked() {
        File parcelStatus = new File(this.mSyncDir, LEGACY_STATUS_FILE_NAME);
        if (parcelStatus.exists() && !this.mStatusFile.exists()) {
            readStatusParcelLocked(parcelStatus);
            writeStatusLocked();
        }
        if (parcelStatus.exists() && this.mStatusFile.exists()) {
            parcelStatus.delete();
        }
    }

    void readStatusLocked() {
        upgradeStatusIfNeededLocked();
        if (!this.mStatusFile.exists()) {
            return;
        }
        try {
            FileInputStream in = this.mStatusFile.openRead();
            readStatusInfoLocked(in);
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            Slog.e("SyncManager", "Unable to read status info file.", e);
        }
    }

    private void readStatusInfoLocked(InputStream in) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 1:
                    long token = proto.start((long) CompanionAppsPermissions.APP_PERMISSIONS);
                    SyncStatusInfo status = readSyncStatusInfoLocked(proto);
                    proto.end(token);
                    if (this.mAuthorities.indexOfKey(status.authorityId) < 0) {
                        break;
                    } else {
                        status.pending = false;
                        this.mSyncStatus.put(status.authorityId, status);
                        break;
                    }
            }
        }
    }

    private SyncStatusInfo readSyncStatusInfoLocked(ProtoInputStream proto) throws IOException {
        SyncStatusInfo status;
        if (proto.nextField(1120986464258L)) {
            status = new SyncStatusInfo(proto.readInt(1120986464258L));
        } else {
            status = new SyncStatusInfo(0);
        }
        int successTimesCount = 0;
        int failureTimesCount = 0;
        ArrayList<Pair<Long, String>> lastEventInformation = new ArrayList<>();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    status.populateLastEventsInformation(lastEventInformation);
                    return status;
                case 2:
                    Slog.w("SyncManager", "Failed to read the authority id via fast-path; some data might not have been read.");
                    status = new SyncStatusInfo(proto.readInt(1120986464258L), status);
                    break;
                case 3:
                    status.lastSuccessTime = proto.readLong(1112396529667L);
                    break;
                case 4:
                    status.lastSuccessSource = proto.readInt(1120986464260L);
                    break;
                case 5:
                    status.lastFailureTime = proto.readLong(1112396529669L);
                    break;
                case 6:
                    status.lastFailureSource = proto.readInt(1120986464262L);
                    break;
                case 7:
                    status.lastFailureMesg = proto.readString(1138166333447L);
                    break;
                case 8:
                    status.initialFailureTime = proto.readLong(1112396529672L);
                    break;
                case 9:
                    status.pending = proto.readBoolean(1133871366153L);
                    break;
                case 10:
                    status.initialize = proto.readBoolean(1133871366154L);
                    break;
                case 11:
                    status.addPeriodicSyncTime(proto.readLong(2211908157451L));
                    break;
                case 12:
                    long eventToken = proto.start(2246267895820L);
                    Pair<Long, String> lastEventInfo = parseLastEventInfoLocked(proto);
                    if (lastEventInfo != null) {
                        lastEventInformation.add(lastEventInfo);
                    }
                    proto.end(eventToken);
                    break;
                case 13:
                    status.lastTodayResetTime = proto.readLong(1112396529677L);
                    break;
                case 14:
                    long totalStatsToken = proto.start(1146756268046L);
                    readSyncStatusStatsLocked(proto, status.totalStats);
                    proto.end(totalStatsToken);
                    break;
                case 15:
                    long todayStatsToken = proto.start(1146756268047L);
                    readSyncStatusStatsLocked(proto, status.todayStats);
                    proto.end(todayStatsToken);
                    break;
                case 16:
                    long yesterdayStatsToken = proto.start(1146756268048L);
                    readSyncStatusStatsLocked(proto, status.yesterdayStats);
                    proto.end(yesterdayStatsToken);
                    break;
                case 17:
                    long successTime = proto.readLong(2211908157457L);
                    if (successTimesCount == status.perSourceLastSuccessTimes.length) {
                        Slog.w("SyncManager", "Attempted to read more per source last success times than expected; data might be corrupted.");
                        break;
                    } else {
                        status.perSourceLastSuccessTimes[successTimesCount] = successTime;
                        successTimesCount++;
                        break;
                    }
                case 18:
                    long failureTime = proto.readLong(2211908157458L);
                    if (failureTimesCount == status.perSourceLastFailureTimes.length) {
                        Slog.w("SyncManager", "Attempted to read more per source last failure times than expected; data might be corrupted.");
                        break;
                    } else {
                        status.perSourceLastFailureTimes[failureTimesCount] = failureTime;
                        failureTimesCount++;
                        break;
                    }
            }
        }
    }

    private Pair<Long, String> parseLastEventInfoLocked(ProtoInputStream proto) throws IOException {
        long time = 0;
        String message = null;
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (message == null) {
                        return null;
                    }
                    return new Pair<>(Long.valueOf(time), message);
                case 1:
                    time = proto.readLong(1112396529665L);
                    break;
                case 2:
                    message = proto.readString(1138166333442L);
                    break;
            }
        }
    }

    private void readSyncStatusStatsLocked(ProtoInputStream proto, SyncStatusInfo.Stats stats) throws IOException {
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 1:
                    stats.totalElapsedTime = proto.readLong(1112396529665L);
                    break;
                case 2:
                    stats.numSyncs = proto.readInt(1120986464258L);
                    break;
                case 3:
                    stats.numFailures = proto.readInt(1120986464259L);
                    break;
                case 4:
                    stats.numCancels = proto.readInt(1120986464260L);
                    break;
                case 5:
                    stats.numSourceOther = proto.readInt(1120986464261L);
                    break;
                case 6:
                    stats.numSourceLocal = proto.readInt(1120986464262L);
                    break;
                case 7:
                    stats.numSourcePoll = proto.readInt(1120986464263L);
                    break;
                case 8:
                    stats.numSourceUser = proto.readInt(1120986464264L);
                    break;
                case 9:
                    stats.numSourcePeriodic = proto.readInt(1120986464265L);
                    break;
                case 10:
                    stats.numSourceFeed = proto.readInt(1120986464266L);
                    break;
            }
        }
    }

    void writeStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mStatusFile.getBaseFile());
        }
        this.mHandler.removeMessages(1);
        FileOutputStream fos = null;
        try {
            try {
                fos = this.mStatusFile.startWrite();
                writeStatusInfoLocked(fos);
                this.mStatusFile.finishWrite(fos);
                fos = null;
            } catch (IOException | IllegalArgumentException e) {
                Slog.e("SyncManager", "Unable to write sync status to proto.", e);
            }
        } finally {
            this.mStatusFile.failWrite(fos);
        }
    }

    private void writeStatusInfoLocked(OutputStream out) {
        SyncStorageEngine syncStorageEngine = this;
        ProtoOutputStream proto = new ProtoOutputStream(out);
        int size = syncStorageEngine.mSyncStatus.size();
        int i = 0;
        while (i < size) {
            SyncStatusInfo info = syncStorageEngine.mSyncStatus.valueAt(i);
            long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
            proto.write(1120986464258L, info.authorityId);
            proto.write(1112396529667L, info.lastSuccessTime);
            proto.write(1120986464260L, info.lastSuccessSource);
            proto.write(1112396529669L, info.lastFailureTime);
            proto.write(1120986464262L, info.lastFailureSource);
            proto.write(1138166333447L, info.lastFailureMesg);
            proto.write(1112396529672L, info.initialFailureTime);
            proto.write(1133871366153L, info.pending);
            proto.write(1133871366154L, info.initialize);
            int periodicSyncTimesSize = info.getPeriodicSyncTimesSize();
            for (int j = 0; j < periodicSyncTimesSize; j++) {
                proto.write(2211908157451L, info.getPeriodicSyncTime(j));
            }
            int lastEventsSize = info.getEventCount();
            int j2 = 0;
            while (j2 < lastEventsSize) {
                long eventToken = proto.start(2246267895820L);
                proto.write(1112396529665L, info.getEventTime(j2));
                proto.write(1138166333442L, info.getEvent(j2));
                proto.end(eventToken);
                j2++;
                size = size;
            }
            int size2 = size;
            proto.write(1112396529677L, info.lastTodayResetTime);
            long totalStatsToken = proto.start(1146756268046L);
            syncStorageEngine.writeStatusStatsLocked(proto, info.totalStats);
            proto.end(totalStatsToken);
            long todayStatsToken = proto.start(1146756268047L);
            syncStorageEngine.writeStatusStatsLocked(proto, info.todayStats);
            proto.end(todayStatsToken);
            long yesterdayStatsToken = proto.start(1146756268048L);
            syncStorageEngine.writeStatusStatsLocked(proto, info.yesterdayStats);
            proto.end(yesterdayStatsToken);
            int lastSuccessTimesSize = info.perSourceLastSuccessTimes.length;
            int j3 = 0;
            while (j3 < lastSuccessTimesSize) {
                proto.write(2211908157457L, info.perSourceLastSuccessTimes[j3]);
                j3++;
                totalStatsToken = totalStatsToken;
                periodicSyncTimesSize = periodicSyncTimesSize;
                lastEventsSize = lastEventsSize;
            }
            int lastFailureTimesSize = info.perSourceLastFailureTimes.length;
            int j4 = 0;
            while (j4 < lastFailureTimesSize) {
                proto.write(2211908157458L, info.perSourceLastFailureTimes[j4]);
                j4++;
                todayStatsToken = todayStatsToken;
            }
            proto.end(token);
            i++;
            syncStorageEngine = this;
            size = size2;
        }
        proto.flush();
    }

    private void writeStatusStatsLocked(ProtoOutputStream proto, SyncStatusInfo.Stats stats) {
        proto.write(1112396529665L, stats.totalElapsedTime);
        proto.write(1120986464258L, stats.numSyncs);
        proto.write(1120986464259L, stats.numFailures);
        proto.write(1120986464260L, stats.numCancels);
        proto.write(1120986464261L, stats.numSourceOther);
        proto.write(1120986464262L, stats.numSourceLocal);
        proto.write(1120986464263L, stats.numSourcePoll);
        proto.write(1120986464264L, stats.numSourceUser);
        proto.write(1120986464265L, stats.numSourcePeriodic);
        proto.write(1120986464266L, stats.numSourceFeed);
    }

    private void requestSync(AuthorityInfo authorityInfo, int reason, Bundle extras, int syncExemptionFlag, int callingUid, int callingPid) {
        OnSyncRequestListener onSyncRequestListener;
        if (Process.myUid() == 1000 && (onSyncRequestListener = this.mSyncRequestListener) != null) {
            onSyncRequestListener.onSyncRequest(authorityInfo.target, reason, extras, syncExemptionFlag, callingUid, callingPid);
            return;
        }
        SyncRequest.Builder req = new SyncRequest.Builder().syncOnce().setExtras(extras);
        req.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
        ContentResolver.requestSync(req.build());
    }

    private void requestSync(Account account, int userId, int reason, String authority, Bundle extras, int syncExemptionFlag, int callingUid, int callingPid) {
        OnSyncRequestListener onSyncRequestListener;
        if (Process.myUid() == 1000 && (onSyncRequestListener = this.mSyncRequestListener) != null) {
            onSyncRequestListener.onSyncRequest(new EndPoint(account, authority, userId), reason, extras, syncExemptionFlag, callingUid, callingPid);
            return;
        }
        ContentResolver.requestSync(account, authority, extras);
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x0028, code lost:
        android.util.Slog.w("SyncManager", "Unknown stats token: " + r5);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void readStatsParcelLocked(File parcel) {
        Parcel in = Parcel.obtain();
        try {
            try {
                AtomicFile parcelFile = new AtomicFile(parcel);
                byte[] data = parcelFile.readFully();
                in.unmarshall(data, 0, data.length);
                in.setDataPosition(0);
                int index = 0;
                while (true) {
                    int token = in.readInt();
                    if (token == 0) {
                        break;
                    }
                    if (token != 101 && token != 100) {
                        break;
                    }
                    int day = in.readInt();
                    if (token == 100) {
                        day = (day - 2009) + 14245;
                    }
                    DayStats ds = new DayStats(day);
                    ds.successCount = in.readInt();
                    ds.successTime = in.readLong();
                    ds.failureCount = in.readInt();
                    ds.failureTime = in.readLong();
                    DayStats[] dayStatsArr = this.mDayStats;
                    if (index < dayStatsArr.length) {
                        dayStatsArr[index] = ds;
                        index++;
                    }
                }
            } catch (IOException e) {
                Slog.i("SyncManager", "No initial statistics");
            }
        } finally {
            in.recycle();
        }
    }

    private void upgradeStatisticsIfNeededLocked() {
        File parcelStats = new File(this.mSyncDir, LEGACY_STATISTICS_FILE_NAME);
        if (parcelStats.exists() && !this.mStatisticsFile.exists()) {
            readStatsParcelLocked(parcelStats);
            writeStatisticsLocked();
        }
        if (parcelStats.exists() && this.mStatisticsFile.exists()) {
            parcelStats.delete();
        }
    }

    private void readStatisticsLocked() {
        upgradeStatisticsIfNeededLocked();
        if (!this.mStatisticsFile.exists()) {
            return;
        }
        try {
            FileInputStream in = this.mStatisticsFile.openRead();
            readDayStatsLocked(in);
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            Slog.e("SyncManager", "Unable to read day stats file.", e);
        }
    }

    private void readDayStatsLocked(InputStream in) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        int statsCount = 0;
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 1:
                    long token = proto.start((long) CompanionAppsPermissions.APP_PERMISSIONS);
                    DayStats stats = readIndividualDayStatsLocked(proto);
                    proto.end(token);
                    DayStats[] dayStatsArr = this.mDayStats;
                    dayStatsArr[statsCount] = stats;
                    statsCount++;
                    if (statsCount != dayStatsArr.length) {
                        break;
                    } else {
                        return;
                    }
            }
        }
    }

    private DayStats readIndividualDayStatsLocked(ProtoInputStream proto) throws IOException {
        DayStats stats;
        if (proto.nextField((long) CompanionMessage.MESSAGE_ID)) {
            stats = new DayStats(proto.readInt((long) CompanionMessage.MESSAGE_ID));
        } else {
            stats = new DayStats(0);
        }
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return stats;
                case 1:
                    Slog.w("SyncManager", "Failed to read the day via fast-path; some data might not have been read.");
                    DayStats temp = new DayStats(proto.readInt((long) CompanionMessage.MESSAGE_ID));
                    temp.successCount = stats.successCount;
                    temp.successTime = stats.successTime;
                    temp.failureCount = stats.failureCount;
                    temp.failureTime = stats.failureTime;
                    stats = temp;
                    break;
                case 2:
                    stats.successCount = proto.readInt(1120986464258L);
                    break;
                case 3:
                    stats.successTime = proto.readLong(1112396529667L);
                    break;
                case 4:
                    stats.failureCount = proto.readInt(1120986464260L);
                    break;
                case 5:
                    stats.failureTime = proto.readLong(1112396529669L);
                    break;
            }
        }
    }

    void writeStatisticsLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v("SyncManager", "Writing new " + this.mStatisticsFile.getBaseFile());
        }
        this.mHandler.removeMessages(2);
        FileOutputStream fos = null;
        try {
            try {
                fos = this.mStatisticsFile.startWrite();
                writeDayStatsLocked(fos);
                this.mStatisticsFile.finishWrite(fos);
                fos = null;
            } catch (IOException | IllegalArgumentException e) {
                Slog.e("SyncManager", "Unable to write day stats to proto.", e);
            }
        } finally {
            this.mStatisticsFile.failWrite(fos);
        }
    }

    private void writeDayStatsLocked(OutputStream out) throws IOException, IllegalArgumentException {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        int size = this.mDayStats.length;
        for (int i = 0; i < size; i++) {
            DayStats stats = this.mDayStats[i];
            if (stats == null) {
                break;
            }
            long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
            proto.write(CompanionMessage.MESSAGE_ID, stats.day);
            proto.write(1120986464258L, stats.successCount);
            proto.write(1112396529667L, stats.successTime);
            proto.write(1120986464260L, stats.failureCount);
            proto.write(1112396529669L, stats.failureTime);
            proto.end(token);
        }
        proto.flush();
    }

    public void queueBackup() {
        BackupManager.dataChanged(PackageManagerService.PLATFORM_PACKAGE_NAME);
    }

    public void setClockValid() {
        if (!this.mIsClockValid) {
            this.mIsClockValid = true;
            Slog.w("SyncManager", "Clock is valid now.");
        }
    }

    public boolean isClockValid() {
        return this.mIsClockValid;
    }

    public void resetTodayStats(boolean force) {
        if (force) {
            Log.w("SyncManager", "Force resetting today stats.");
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                cur.maybeResetTodayStats(isClockValid(), force);
            }
            writeStatusLocked();
        }
    }
}
